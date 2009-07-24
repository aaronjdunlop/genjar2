/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002, 2003 Jesse Stockall.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in
 * the documentation and/or other materials provided with the
 * distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 * any, must include the following acknowlegement:
 * "This product includes software developed by the
 * Apache Software Foundation (http://www.apache.org/)."
 * Alternately, this acknowlegement may appear in the software itself,
 * if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
 * Foundation" must not be used to endorse or promote products derived
 * from this software without prior written permission. For written
 * permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 * nor may "Apache" appear in their names without prior written
 * permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 */
package net.sf.genjar;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;
import java.util.regex.Pattern;

import net.sf.genjar.antutil.DependencyVisitor;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.DescendingVisitor;
import org.apache.bcel.classfile.JavaClass;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.depend.AbstractAnalyzer;

/**
 * An analyzer capable fo traversing all class - class relationships with configurable include /
 * exclude lists.
 * <p>
 *
 * This Analyzer is a modified version of <code>org.apache.tools.ant.util.depend.FullAnalyzer</code>
 *
 * @author Conor MacNeill
 * @author <a href="mailto:hengels@innovidata.com">Holger Engels</a>
 * @author Jesse Stockall
 * @created February 23, 2003
 * @version $Revision: 1.2 $ $Date: 2003/02/23 18:25:23 $
 */
public class Analyzer extends AbstractAnalyzer
{
    private final List<String> excludes;
    private final List<String> includes;
    private final Project project;

    /**
     * GenJarAnalyzer c'tor
     *
     * @param project The current Ant build project
     * @param includes List of patterns to explicitly include.
     * @param excludes List of patterns to explicitly exclude.
     */
    public Analyzer(Project project, List<String> includes, List<String> excludes)
    {
        this.includes = includes;
        this.excludes = excludes;
        this.project = project;
    }

    /**
     * Determine the dependencies of the configured root classes.
     *
     * @param files a vector to be populated with the files which contain the dependency classes
     * @param classes a vector to be populated with the names of the dependency classes.
     */
    @Override
    @SuppressWarnings("unchecked")
    protected void determineDependencies(Vector files, Vector classes)
    {
        // we get the root classes and build up a set of
        // classes upon which they depend
        HashSet<String> dependencies = new HashSet<String>();
        HashSet<File> containers = new HashSet<File>();
        HashSet<String> toAnalyze = new HashSet<String>();

        for (Enumeration e = getRootClasses(); e.hasMoreElements();)
        {
            String classname = (String) e.nextElement();

            toAnalyze.add(classname);
        }

        int count = 0;
        int maxCount = isClosureRequired() ? MAX_LOOPS : 2;

        while (toAnalyze.size() != 0 && count++ < maxCount)
        {
            DependencyVisitor dependencyVisitor = new DependencyVisitor();

            for (final String classname : toAnalyze)
            {
                if (!isClassIncluded(classname))
                {
                    continue;
                }
                dependencies.add(classname);
                try
                {
                    File container = getClassContainer(classname);

                    if (container == null)
                    {
                        continue;
                    }
                    containers.add(container);

                    ClassParser parser = null;

                    if (container.getName().endsWith(".class"))
                    {
                        parser = new ClassParser(container.getPath());
                    }
                    else
                    {
                        parser = new ClassParser(container.getPath(), classname.replace('.', '/') + ".class");
                    }

                    JavaClass javaClass = parser.parse();
                    DescendingVisitor traverser = new DescendingVisitor(javaClass, dependencyVisitor);

                    traverser.visit();
                }
                catch (IOException ioe)
                {
                    // ignore
                }
            }

            toAnalyze.clear();

            // now recover all the dependencies collected and add to the list.
            Enumeration<String> depsEnum = dependencyVisitor.getDependencies();

            Pattern p = Pattern.compile("[\\.\\$][A-Z][A-Za-z0-9]*\\.[A-Z]");
            while (depsEnum.hasMoreElements())
            {
                String className = depsEnum.nextElement();

                String newClassName = className;
                // DependencyVisitor.getDependencies() returns duplicate inner classes with naming
                // variations (e.g. Foo.Bar and Foo$Bar). This can cause problems downstream, so
                // eliminate them here.
                if (className.indexOf('$') >= 0)
                {
                    StringBuilder sb = new StringBuilder(className);
                    while (p.matcher(sb).find())
                    {
                        sb.setCharAt(sb.lastIndexOf("."), '$');
                    }
                    newClassName = sb.toString();
                }

                if (!dependencies.contains(newClassName))
                {
                    toAnalyze.add(newClassName);
                }
            }
        }

        files.removeAllElements();
        files.addAll(containers);

        classes.removeAllElements();
        classes.addAll(dependencies);
    }

    /**
     * Indicate if this analyzer can determine dependent files.
     *
     * @return true if the analyzer provides dependency file information.
     */
    @Override
    protected boolean supportsFileDependencies()
    {
        return true;
    }

    private boolean isClassIncluded(final String classname)
    {
        // normalize class name to dotted notation for logging
        String normalizedClassname = classname.replace('/', '.');

        // if the class is explicitly included, then say ok....
        for (String ip : includes)
        {
            if (normalizedClassname.startsWith(ip))
            {
                project.log("Explicit Include (" + ip + "):" + classname, Project.MSG_DEBUG);
                return true;
            }
        }

        // no explicit inclusion - check for an exclusion
        for (String ip : excludes)
        {
            if (normalizedClassname.startsWith(ip))
            {
                project.log("Explicit Exclude (" + ip + "):" + classname, Project.MSG_DEBUG);
                return false;
            }
        }
        // nothing explicit - include by default
        project.log("Implicit Include:" + normalizedClassname, Project.MSG_DEBUG);
        return true;
    }
}
// vi:set ts=4 sw=4:
