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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

import net.sf.genjar.antutil.DependencyVisitor;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.DescendingVisitor;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.depend.AbstractAnalyzer;

/**
 * An analyzer capable of traversing all class - class relationships with configurable include / exclude lists.
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
public class Analyzer extends AbstractAnalyzer {

    private final List<String> excludes;
    private final List<String> includes;
    private final Project project;

    private final HashMap<String, String> referenceMap;

    final Pattern innerClassPattern = Pattern.compile("[\\.\\$][A-Z][A-Za-z0-9]*\\.[A-Z]");

    private String[] classpathList;

    /**
     * @param project The current Ant build project
     * @param includes List of patterns to explicitly include.
     * @param excludes List of patterns to explicitly exclude.
     */
    public Analyzer(final Project project, final List<String> includes, final List<String> excludes,
            final HashMap<String, String> referenceMap) {
        this.includes = includes;
        this.excludes = excludes;
        this.project = project;
        this.referenceMap = referenceMap;
    }

    /**
     * Determine the dependencies of the configured root classes.
     * 
     * @param files a vector to be populated with the files which contain the dependency classes
     * @param classes a vector to be populated with the names of the dependency classes.
     */
    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void determineDependencies(final Vector files, final Vector classes) {

        // we get the root classes and build up a set of
        // classes upon which they depend
        final HashSet<String> dependencies = new HashSet<String>();
        final HashSet<File> containers = new HashSet<File>();
        final HashSet<String> toAnalyze = new HashSet<String>();

        for (final Enumeration e = getRootClasses(); e.hasMoreElements();) {
            toAnalyze.add((String) e.nextElement());
        }

        int count = 0;
        final int maxCount = isClosureRequired() ? MAX_LOOPS : 2;

        while (toAnalyze.size() != 0 && count++ < maxCount) {

            final DependencyVisitor dependencyVisitor = new DependencyVisitor(referenceMap);

            for (final String classname : toAnalyze) {

                if (!isClassIncluded(classname)) {
                    continue;
                }
                if (dependencies.contains(classname)) {
                    continue;
                }

                dependencies.add(classname);

                try {
                    final File container = getClassContainer(classname);
                    if (container == null) {
                        continue;
                    }
                    containers.add(container);

                    final ClassParser parser = container.getName().endsWith(".class") ? new ClassParser(
                            container.getPath()) : new ClassParser(container.getPath(), classname.replace('.', '/')
                            + ".class");

                    final DescendingVisitor descendingVisitor = new DescendingVisitor(parser.parse(), dependencyVisitor);
                    dependencyVisitor.setDescendingVistor(descendingVisitor);
                    descendingVisitor.visit();

                } catch (final IOException ioe) {
                    // ignore
                }
            }

            toAnalyze.clear();

            // now recover all the dependencies collected and add to the list.
            for (final String className : dependencyVisitor.getDependencies()) {

                String newClassName = className;
                // DependencyVisitor.getDependencies() returns duplicate inner classes with naming
                // variations (e.g. Foo.Bar and Foo$Bar). This can cause problems downstream, so
                // eliminate them here.
                if (className.indexOf('$') >= 0) {
                    final StringBuilder sb = new StringBuilder(className);
                    while (innerClassPattern.matcher(sb).find() && sb.indexOf("$") < 0) {
                        sb.setCharAt(sb.lastIndexOf("."), '$');
                    }
                    newClassName = sb.toString();
                }

                if (!dependencies.contains(newClassName)) {
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
    protected boolean supportsFileDependencies() {
        return true;
    }

    /**
     * Add a classpath to the classpath being used by the analyzer. The classpath contains the binary classfiles for the
     * classes being analyzed The elements may either be the directories or jar files.Not all analyzers will use this
     * information.
     * 
     * Overrides the superclass implementation to populate {@link #classpathList}
     * 
     * @param classPath the Path instance specifying the classpath elements
     */
    @Override
    public void addClassPath(final Path classPath) {
        super.addClassPath(classPath);
        this.classpathList = classPath.list();
    }

    /**
     * Get the file that contains the class definition. Overrides the superclass implementation to only list the
     * classpath once
     * 
     * @param classname the name of the required class
     * @return the file instance, zip or class, containing the class or null if the class could not be found.
     * @exception IOException if the files in the classpath cannot be read.
     */
    @Override
    public File getClassContainer(final String classname) throws IOException {
        final String classLocation = classname.replace('.', '/') + ".class";
        // we look through the classpath elements. If the element is a dir
        // we look for the file. IF it is a zip, we look for the zip entry
        // TODO ??? Iterate over the classpath backwards, mapping from directory contents and jar contents to the jars
        // they're present in
        return getResourceContainer(classLocation, classpathList);
    }

    /**
     * Get the file that contains the resource
     * 
     * Copied from AbstractAnalyzer.java
     * 
     * @param resourceLocation the name of the required resource.
     * @param paths the paths which will be searched for the resource.
     * @return the file instance, zip or class, containing the class or null if the class could not be found.
     * @exception IOException if the files in the given paths cannot be read.
     */
    private File getResourceContainer(final String resourceLocation, final String[] paths) throws IOException {
        for (int i = 0; i < paths.length; ++i) {
            final File element = new File(paths[i]);
            if (!element.exists()) {
                continue;
            }
            if (element.isDirectory()) {
                final File resource = new File(element, resourceLocation);
                if (resource.exists()) {
                    return resource;
                }
            } else {
                // must be a zip of some sort
                ZipFile zipFile = null;
                try {
                    zipFile = new ZipFile(element);
                    if (zipFile.getEntry(resourceLocation) != null) {
                        return element;
                    }
                } finally {
                    if (zipFile != null) {
                        zipFile.close();
                    }
                }
            }
        }
        return null;
    }

    private boolean isClassIncluded(final String classname) {

        // normalize class name to dotted notation for logging
        final String normalizedClassname = classname.replace('/', '.');

        // if the class is explicitly included, then say ok....
        for (final String ip : includes) {
            if (normalizedClassname.startsWith(ip)) {
                project.log("Explicit Include (" + ip + "):" + classname, Project.MSG_DEBUG);
                return true;
            }
        }

        // no explicit inclusion - check for an exclusion
        for (final String ip : excludes) {
            if (normalizedClassname.startsWith(ip)) {
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
