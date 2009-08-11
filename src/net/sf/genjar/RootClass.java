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

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;

/**
 * <p>
 *
 * Represents a &lt;class&gt; element.
 * <p>
 *
 * Buildfile example:
 *
 * <pre>
 *   &lt;class name=&quot;com.riggshill.ant.genjar.GenJar&quot;/&gt;
 *   &lt;class name=&quot;com.riggshill.xml.Editor&quot; bean=&quot;yes&quot;/&gt;
 * </pre>
 * <p>
 *
 * When the &lt;class&gt; element is encountered, a new ClassSpec is instantiated to represent that
 * element. The class is used hold the class' name and manifest information.
 * </p>
 * <p>
 *
 * The <code>resolve()</code> method is implemented to determine which classes <i>this</i> class is
 * dependant upon. A list of these classes is held for later inclusion into the jar.
 * </p>
 *
 * @author Original Code: <a href="mailto:jake@riggshill.com">John W. Kohler </a>
 * @author Jesse Stockall
 * @created February 23, 2003
 * @version $Revision: 1.3 $ $Date: 2003/02/23 18:25:23 $
 */
public class RootClass
{
    private boolean singleNameSet = false;
    private final List<String> jarEntries = new LinkedList<String>();
    private final List<FileSet> filesets = new LinkedList<FileSet>();
    private final Project project;

    /**
     * Constructor
     *
     * @param project The current ant project.
     */
    public RootClass(Project proj)
    {
        project = proj;
    }

    /**
     * Constructor
     *
     * @param project The current ant project.
     * @param name Classname
     */
    public RootClass(Project project, String name)
    {
        this.project = project;
        this.jarEntries.add(name);
        this.singleNameSet = true;
    }

    /**
     * Returns the list of classes upon which this class is dependent.
     *
     * @return the list of all dependent classes
     */
    public List<String> getJarEntries()
    {
        return jarEntries;
    }

    /**
     * Invoked by Ant when the <code>name</code> attribute is encountered.
     *
     * @param name The new name value
     */
    public void setName(String name)
    {
        String classfileName = name.replace('.', '/') + ".class";
        this.singleNameSet = true;
        jarEntries.add(classfileName);
    }

    /**
     * Generates a list of all classes upon which this/these class is dependent.
     *
     * @param gj Description of the Parameter
     * @throws IOException Description of the Exception
     */
    public void resolve(GenJar gj) throws IOException
    {
        // TODO: We really want to do this sometime before resolve...
        for (FileSet fileset : filesets)
        {
            DirectoryScanner ds = fileset.getDirectoryScanner(project);
            String[] includedFiles = ds.getIncludedFiles();

            for (String filename : includedFiles)
            {
                if (filename.endsWith(".class"))
                {
                    jarEntries.add(filename);
                }
            }
        }

        // get dependencies for all class files
        gj.generateDependancies(jarEntries);
    }

    /**
     * Add a fileset to be resolved later.
     *
     * @return A fileset of classes.
     */
    public void addFileset(FileSet fileset)
    {
        if (singleNameSet)
        {
            throw new BuildException("can't add Fileset - class name already set");
        }

        filesets.add(fileset);
    }

    @Override
    public String toString()
    {
        return jarEntries.toString();
    }
}
