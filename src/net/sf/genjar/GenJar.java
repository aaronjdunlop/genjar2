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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.FileScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Jar;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.ZipFileSet;
import org.apache.tools.zip.ZipOutputStream;

/**
 * Driver class for the GenJar task.
 * <p>
 *
 * This class is instantiated when Ant encounters the &lt;genjar&gt; element.
 *
 * @author Original Code: <a href="mailto:jake@riggshill.com">John W. Kohler </a>
 * @author Jesse Stockall
 * @created February 23, 2003
 * @version $Revision: 1.4 $ $Date: 2003/02/23 18:25:23 $
 */
public class GenJar extends Jar
{
    private final List<RootClass> rootClasses = new ArrayList<RootClass>(32);

    private final List<FileSet> filesets = new ArrayList<FileSet>();
    private final List<FileSet> groupfilesets = new ArrayList<FileSet>();

    private Path classpath = null;
    private ClassFilter classFilter = null;
    private final List<BaseResolver> resolvers = new LinkedList<BaseResolver>();

    /** jar index is JDK 1.3+ only */
    private boolean index = true;

    /** Constructor for the GenJar object */
    public GenJar()
    {
        setTaskName("GenJar");
        setUpdate(false);
    }

    /**
     * Set whether or not to create an index list for classes. This may speed up classloading in
     * some cases.
     *
     * @param flag The new index value
     */
    @Override
    public void setIndex(boolean flag)
    {
        index = flag;
    }

    /**
     * Adds a classpath
     *
     * @param path
     */
    public void addClasspath(Path path)
    {
        this.classpath = path;
    }

    /**
     * Sets the Classpathref attribute.
     *
     * @param r The new classpathRef.
     */
    public void setClasspathRef(Reference r)
    {
        Path cp = new Path(getProject());
        cp.setRefid(r);
        addClasspath(cp);
    }

    /**
     * Builds a <class> element.
     *
     * @return A <class> element.
     */
    public RootClass createClass()
    {
        RootClass rc = new RootClass(getProject());
        rootClasses.add(rc);
        return rc;
    }

    /**
     * Adds a root class.
     */
    public void addClass(RootClass rootClass)
    {
        rootClasses.add(rootClass);
    }

    /**
     * Adds a contained FileSet
     */
    @Override
    public void addFileset(FileSet fs)
    {
        filesets.add(fs);
    }

    /**
     * Adds a contained ZipFileSet
     */
    @Override
    public void addZipfileset(ZipFileSet fs)
    {
        filesets.add(fs);
    }

    /**
     * Adds a contained ZipGroupFileSet
     */
    public void addZipgroupfileset(FileSet fs)
    {
        groupfilesets.add(fs);
    }

    /**
     * Builds a classfilter element.
     *
     * @return A <classfilter> element.
     */
    public ClassFilter createClassfilter()
    {
        if (classFilter == null)
        {
            classFilter = new ClassFilter(getProject());
        }
        return classFilter;
    }

    /**
     * main execute for genjar
     * <ol>
     * <li>setup logger
     * <li>ensure classpath is setup (with any additions from sub-elements
     * <li>initialize file resolvers
     * <li>initialize the manifest
     * <li>resolve resource file paths resolve class file paths generate dependancy graphs for class
     * files and resolve those paths check for duplicates
     * <li>generate manifest entries for all candidate files
     * <li>build jar
     * </ol>
     *
     *
     * @throws BuildException Description of the Exception
     */
    @Override
    public void execute() throws BuildException
    {
        long start = System.currentTimeMillis();

        if (classFilter == null)
        {
            classFilter = new ClassFilter(getProject());
        }

        processGroupFilesets();

        getProject().log("GenJar Ver: 2.0.0", Project.MSG_VERBOSE);
        if (getDestFile() == null)
        {
            throw new BuildException("GenJar: destfile attribute is required", getLocation());
        }

        //
        // Set up the classpath & resolvers - file/zip
        //
        try
        {
            if (classpath == null)
            {
                classpath = new Path(getProject());
            }
            if (!classpath.isReference())
            {
                // Add the system path now - AFTER all other paths are specified
                classpath.addExisting(Path.systemClasspath);
            }

            getProject().log("Initializing Path Resolvers", Project.MSG_VERBOSE);
            getProject().log("Classpath:" + classpath, Project.MSG_VERBOSE);
            initResolvers();
        }
        catch (IOException ioe)
        {
            throw new BuildException("Unable to process classpath: " + ioe, getLocation());
        }

        //
        // run over all the resource and class specifications
        // given in the project file
        // resources are resolved to full path names while
        // class specifications are exploded to dependency
        // graphs - when done, getJarEntries() returns a list
        // of all entries generated by this JarSpec
        //
        final Set<String> jarEntries = new HashSet<String>();

        for (final RootClass rc : rootClasses)
        {
            try
            {
                rc.resolve(this);
            }
            catch (IOException ioe)
            {
                throw new BuildException("Unable to resolve: ", ioe, getLocation());
            }

            //
            // before adding a new jarspec - see if it already exists
            // first entry added to jar always wins
            //
            for (final String jarEntry : rc.getJarEntries())
            {
                if (!jarEntries.contains(jarEntry))
                {
                    jarEntries.add(jarEntry);
                    getProject().log("Adding " + jarEntry, Project.MSG_VERBOSE);
                }
                else
                {
                    getProject().log("Duplicate (ignored): " + jarEntry, Project.MSG_VERBOSE);
                }
            }
        }

        if (zipFile.exists())
        {
            zipFile.delete();
        }

        log("Generating jar: " + getDestFile());

        InputStream is = null;
        ZipOutputStream zOut = null;
        try
        {
            // Find the resources (fileset, zipfileset, zipgroupfileset, etc) that we're going to
            // add. Code stolen from Ant's Zip task
            final ResourceCollection[] fss = filesets.toArray(new ResourceCollection[filesets.size()]);
            final ArchiveState state = getResourcesToAdd(fss, zipFile, false);
            final Resource[][] addThem = state.getResourcesToAdd();


            // Create the jar file
            zOut = new ZipOutputStream(zipFile);
            zOut.setEncoding(getEncoding());
            zOut.setMethod(ZipOutputStream.DEFLATED);
            zOut.setLevel(getLevel());
            initZipOutputStream(zOut);

            final byte[] buf = new byte[16384];
            for (final String jarEntry : jarEntries)
            {
                is = resolveEntry(jarEntry);
                if (is == null)
                {
                    getProject().log("Unable to locate previously resolved resource", Project.MSG_ERR);
                    getProject().log("       Jar Name:" + jarEntry, Project.MSG_ERR);
                    throw new BuildException("Jar component not found (" + jarEntry + ')', getLocation());
                }
                zOut.putNextEntry(new org.apache.tools.zip.ZipEntry(jarEntry));

                for (int read = is.read(buf); read != -1; read = is.read(buf))
                {
                    zOut.write(buf, 0, read);
                }
                zOut.closeEntry();
                is.close();

                getProject().log("Added: " + jarEntry, Project.MSG_VERBOSE);
            }

            // Add the explicit resource collections to the archive.
            for (int i = 0; i < fss.length; i++)
            {
                if (addThem[i].length != 0)
                {
                    addResources(fss[i], addThem[i], zOut);
                }
            }

            // Add an index list to the jar if it was requested
            if (index)
            {
                addIndexList(zOut, jarEntries);
            }
        }
        catch (FileNotFoundException fnfe)
        {
            throw new BuildException("Unable to access jar file (" + getDestFile() + ") msg:", fnfe, getLocation());
        }
        catch (IOException ioe)
        {
            throw new BuildException("Unable to create jar: " + ioe.getMessage(), ioe, getLocation());
        }
        finally
        {
            try
            {
                if (is != null)
                {
                    is.close();
                }
            }
            catch (IOException ioe)
            {}
            try
            {
                if (zOut != null)
                {
                    zOut.close();
                }
            }
            catch (IOException ioe)
            {}
        }
        log("Jar Generated (" + (System.currentTimeMillis() - start) + " ms)");

        // Close all the resolvers
        for (BaseResolver resolver : resolvers)
        {
            resolver.close();
        }
    }

    /**
     * Description of the Method
     *
     * @param jarEntry Description of the Parameter
     * @return Description of the Return Value
     * @throws IOException Description of the Exception
     */
    private InputStream resolveEntry(String jarEntry) throws IOException
    {
        for (BaseResolver resolver : resolvers)
        {
            final InputStream is = resolver.resolve(jarEntry);
            if (is != null)
            {
                return is;
            }
        }
        return null;
    }

    /**
     * Generates a list of all classes upon which the list of classes depend.
     *
     * @param jarEntrySpecs List of <code>JarEntrySpec</code>s used as a list of class names from
     *            which to start.
     */
    void generateDependancies(Collection<String> jarEntries)
    {
        jarEntries.addAll(generateClassDependencies(jarEntries));
    }

    /**
     * Iterate through the classpath and create an array of all the {@link BaseResolver}s
     *
     * @throws IOException Description of the Exception
     */
    private void initResolvers() throws IOException
    {
        String[] classpathEntries = classpath.list();

        for (final String entry : classpathEntries)
        {
            final File f = new File(entry);

            if (!f.exists())
            {
                continue;
            }

            final String suffix = entry.substring(entry.length() - 4);
            if (suffix.equalsIgnoreCase(".jar") || suffix.equalsIgnoreCase(".zip"))
            {
                ZipFileSet zfs = new ZipFileSet();
                zfs.setSrc(f);
                zfs.setProject(getProject());
                resolvers.add(new ArchiveResolver(zfs));
            }
            else if (f.isDirectory())
            {
                FileSet fs = new FileSet();
                fs.setDir(f);
                fs.setProject(getProject());
                resolvers.add(new FileResolver(fs));
            }
            else
            {
                throw new BuildException(f.getName() + " is not a valid classpath component", getLocation());
            }
        }

        for (final FileSet fs : filesets)
        {
            if (fs instanceof ZipFileSet)
            {
                resolvers.add(new ArchiveResolver((ZipFileSet) fs));
            }
            else
            {
                resolvers.add(new FileResolver(fs));
            }
        }
    }

    /**
     * Generates a list of classes upon which the named class is dependent.
     *
     * @param it Iterator of all the classes to use when building the dependencies.
     * @return A List of all the class dependencies.
     */
    @SuppressWarnings("unchecked")
    private Set<String> generateClassDependencies(Collection<String> jarEntries)
    {
        final Analyzer ga = new Analyzer(getProject(), classFilter.getIncludeList(), classFilter.getExcludeList());
        ga.addClassPath(classpath);

        final Set<String> resolvedClasses = new HashSet<String>();

        for (String classname : jarEntries)
        {
            if (!resolvedClasses.contains(classname))
            {
                resolvedClasses.add(classname);

                // Ant's analyzer framework adds the .class, so strip it here
                if (classname.endsWith(".class"))
                {
                    classname = classname.substring(0, classname.length() - 6);
                }
                ga.addRootClass(classname);
            }
        }

        final Set<String> deps = new HashSet<String>();
        final Enumeration<String> e = ga.getClassDependencies();

        while (e.hasMoreElements())
        {
            // Now convert back to / and add .class
            deps.add(e.nextElement().replace('.', '/') + ".class");
        }
        return deps;
    }

    /**
     * Create the index list to speed up classloading. This is a JDK 1.3+ specific feature and is
     * enabled by default. See <a
     * href="http://java.sun.com/j2se/1.3/docs/guide/jar/jar.html#JAR+Index"> the JAR index
     * specification</a> for more details.
     * <p>
     *
     * Based on code from ant's Jar task.
     *
     * @param zOut An opened JarOutPutStream to write to index file to.
     * @param jarEntries A list of all the entried in the jar.
     * @throws IOException thrown if there is an error while creating the index and adding it to the
     *             zip stream.
     */
    private void addIndexList(final ZipOutputStream zOut, final Set<String> jarEntries) throws IOException
    {
        zOut.putNextEntry(new org.apache.tools.zip.ZipEntry("META-INF/INDEX.LIST"));

        // encoding must be UTF8 as specified in the specs.
        final PrintWriter writer = new PrintWriter(new OutputStreamWriter(zOut, "UTF8"));

        // version-info blankline
        writer.println("JarIndex-Version: 1.0\n");
        writer.println();

        // header newline
        writer.println(getDestFile().getName());

        final Set<String> directoriesAlreadyAdded = new HashSet<String>();

        for (final String jarEntry : jarEntries)
        {
            String path = jarEntry.replace('\\', '/');

            final int pos = path.lastIndexOf('/');
            if (pos == -1)
            {
                // Class is in root and can be ignored? maybe?
                continue;
            }

            // looks like nothing from META-INF should be added
            // and the check is not case insensitive.
            // see sun.misc.JarIndex
            if (path.startsWith("META-INF"))
            {
                continue;
            }

            path = path.substring(0, pos);

            // Only add the path once
            if (!directoriesAlreadyAdded.contains(path))
            {
                writer.println(path);
                directoriesAlreadyAdded.add(path);
            }
        }

        writer.flush();
        zOut.closeEntry();
    }

    /**
     * Stolen verbatim from Ant's Jar task, since this method is private and can't be called from
     * genjar
     */
    private void processGroupFilesets()
    {
        // Add the files found in the groupfileset to filesets
        for (final FileSet fs : groupfilesets)
        {
            log("Processing groupfileset ", Project.MSG_VERBOSE);
            final FileScanner scanner = fs.getDirectoryScanner(getProject());
            final File basedir = scanner.getBasedir();

            for (final String filename : scanner.getIncludedFiles())
            {
                log("Adding file " + filename + " to fileset", Project.MSG_VERBOSE);
                final ZipFileSet zf = new ZipFileSet();
                zf.setProject(getProject());
                zf.setSrc(new File(basedir, filename));
                filesets.add(zf);
            }
        }
    }
}
