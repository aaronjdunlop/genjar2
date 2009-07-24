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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.ZipFileSet;

/**
 * Represents a &lt;resource&gt; element within the project file.</p>
 * <p>
 *
 * TODO: Test includes and excludes on Ant filesets and zipfilesets
 *
 * In addition to holding the final <i>jar name</i> of the resource, it performs the actual
 * resolution of file names along with expansion of <i>filesets</i> .
 *
 * @author Original Code: <a href="mailto:jake@riggshill.com">John W. Kohler </a>
 * @author Jesse Stockall
 * @created February 23, 2003
 * @version $Revision: 1.2 $ $Date: 2003/02/23 18:25:23 $
 */
public class ContainedFileSet extends ContainedObject
{
    private final Project project;
    private final List<GenJarEntry> jarEntries = new ArrayList<GenJarEntry>();

    private FileSet fileset;
    private ZipFile zipFile;

    /**
     * @param proj the owning project
     * @param fileset the Ant FileSet represented
     */
    ContainedFileSet(Project proj, FileSet fileset)
    {
        project = proj;
        this.fileset = fileset;
    }

    /**
     * @param proj the owning project
     * @param fileset the Ant FileSet represented
     */
    ContainedFileSet(Project proj, ZipFile zipFile)
    {
        project = proj;
        this.zipFile = zipFile;
    }

    /**
     * Returns a List of all JarEntry objects collected by this Resource
     *
     * @return List all collected JarEntry objects
     */
    @Override
    public List<GenJarEntry> getJarEntries()
    {
        return jarEntries;
    }

    /**
     * creates a FileSet - in response to the ant parse phase
     *
     * @return Description of the Return Value
     */
    public FileSet createFileset()
    {
        FileSet set = new FileSet();
        fileset = set;
        return set;
    }

    /**
     * resolves the file attribute or fileset children into a collection of JarEntrySpec objects
     *
     * @param gj Description of the Parameter
     * @throws IOException Description of the Exception
     */
    @Override
    public void resolve(GenJar gj) throws IOException
    {
        if (zipFile != null)
        {
            resolveZip(zipFile);
        }
        else
        {
            if (fileset instanceof ZipFileSet)
            {
                resolveZip(new ZipFile(((ZipFileSet) fileset).getSrc()));
            }
            else
            {
                final File dir = fileset.getDir(project);

                for (final String f : fileset.getDirectoryScanner(project).getIncludedFiles())
                {
                    jarEntries.add(new GenJarEntry(f, new File(dir, f)));
                }
            }
        }
    }

    private void resolveZip(ZipFile zf)
    {
        try
        {
            Enumeration<? extends ZipEntry> entries = zf.entries();

            while (entries.hasMoreElements())
            {
                GenJarEntry je = new GenJarEntry();
                ZipEntry zentry = entries.nextElement();

                //
                // zip directories are not allowed - they screw
                // up the file resolvers BIG TIME
                //
                if (zentry.isDirectory())
                {
                    continue;
                }

                // disallow the contents of the META-INF directory,
                // this means Manifests, Index lists and signing information
                String name = zentry.getName();

                if (name.startsWith("META-INF"))
                {
                    continue;
                }
                //
                // setup the JarEntry object and copy any existing
                // attributes - attributes from library jar override
                // ours
                //
                je.setJarName(name);

                long size = zentry.getSize();

                if (size != -1L)
                {
                    je.setAttribute("Content-Length", size);
                }

                if (zf instanceof JarFile && ((JarFile) zf).getManifest() != null)
                {
                    je.addAttributes(((JarFile) zf).getManifest().getAttributes(name));
                }

                jarEntries.add(je);
            }
            zf.close();
        }
        catch (IOException ioe)
        {
            throw new BuildException("genjar:IOException while reading library jar");
        }
    }

    /**
     * return a string representation of this resource set
     *
     * @return All the toString() methods form the jar entires.
     */
    @Override
    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        for (Iterator<GenJarEntry> it = jarEntries.iterator(); it.hasNext();)
        {
            sb.append("\n");
            sb.append(it.next());
        }
        return sb.toString();
    }
}
// vi:set ts=4 sw=4:
