/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000, 2001, 2002, 2003 Jesse Stockall.  All rights reserved.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.PatternSet;

/**
 * Determines whether a class is to be included in the jar.
 * <p>
 *
 * Buildfile example:
 *
 * <pre>
 * &lt;classfilter&gt;
 *   &lt;include name=&quot;com.foo.&quot; &gt;&lt;br/&gt;
 * &lt;exclude name=&quot;org&quot; &gt;&lt;br/&gt;
 * &lt;/classfilter&gt;
 * </pre>
 * <p>
 *
 * As a class' dependency lists are generated, each class is checked against a ClassFilter by
 * calling the <code>include</code> method. This method checks the class name against its list of
 * <i>includes</i> and <i>excludes</i> .
 * </p>
 * <p>
 *
 * If the class name starts with any of the strings in the <i>includes</i> list, the class is
 * included in the jar and its dependency lists are checked. If the class name starts with any of
 * the strings in the <i>excludes</i> list, the class is <b>not</b> included in the jar and the
 * dependency analysis halts (for that class).
 * </p>
 * <p>
 *
 * Note that the following packages are excluded by default:
 * <code>java javax sun sunw com.sun org.omg</code>
 * </p>
 *
 * @author Original Code: <a href="mailto:jake@riggshill.com">John W. Kohler </a>
 * @author Jesse Stockall
 * @created February 23, 2003
 * @version $Revision: 1.3 $ $Date: 2003/02/23 18:25:23 $
 */
public class ClassFilter extends DataType
{
    private final PatternSet patternSet = new PatternSet();
    private final Project project;

    /**
     * Constructs a filter object with the default exclusions
     *
     * @param log Logger for messages.
     * @param proj The ant project.
     */
    ClassFilter(Project proj)
    {
        project = proj;

        // you really do NOT want these included in your
        // jar - if you do, then spcify 'em as includes
        addExcludePattern("java.");
        addExcludePattern("javax.");
        addExcludePattern("sun.");
        addExcludePattern("sunw.");
        addExcludePattern("com.sun.");
        addExcludePattern("org.omg.");
        addExcludePattern("com.ibm.jvm.");

        // in some circumstances, these names will be included
        // in a class' constant pool as classes, so they're
        // excluded just to make sure
        addExcludePattern("boolean");
        addExcludePattern("byte");
        addExcludePattern("char");
        addExcludePattern("short");
        addExcludePattern("int");
        addExcludePattern("long");
        addExcludePattern("float");
        addExcludePattern("double");

        // Exclude all arrays as well
        addExcludePattern("[");
    }

    /**
     * Gets the list of exclude patterns.
     *
     * @return The exclude list.
     */
    public List<String> getExcludeList()
    {
        String[] patterns = patternSet.getExcludePatterns(project);

        if (patterns != null)
        {
            return Arrays.asList(patterns);
        }

        return new ArrayList<String>();
    }

    /**
     * Gets the list of include patterns.
     *
     * @return The include list.
     */
    public List<String> getIncludeList()
    {
        String[] patterns = patternSet.getIncludePatterns(project);

        if (patterns != null)
        {
            return Arrays.asList(patterns);
        }

        return new ArrayList<String>();
    }

    /**
     * Adds a name entry on the include list.
     *
     * @return An include pattern set entry.
     */
    public PatternSet.NameEntry createInclude()
    {
        if (isReference())
        {
            throw noChildrenAllowed();
        }
        return patternSet.createInclude();
    }

    /**
     * Adds a name entry on the include files list.
     *
     * @return An exclude pattern set entry.
     */
    public PatternSet.NameEntry createExclude()
    {
        if (isReference())
        {
            throw noChildrenAllowed();
        }
        return patternSet.createExclude();
    }

    /**
     * Adds an exclusion pattern.
     *
     * @param patt The exclusion pattern to add.
     */
    private void addExcludePattern(final String patt)
    {
        String pattern = patt.replace('/', '.');

        patternSet.setExcludes(pattern);
        project.log("Exclude:" + pattern, Project.MSG_DEBUG);
    }
}
// vi:set ts=4 sw=4:
