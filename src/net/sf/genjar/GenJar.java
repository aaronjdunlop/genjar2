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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipFile;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Jar;
import org.apache.tools.ant.taskdefs.Manifest;
import org.apache.tools.ant.taskdefs.ManifestException;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.ZipFileSet;
import org.apache.tools.ant.types.resources.FileResource;

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
    private final List<ContainedObject> containedObjects = new ArrayList<ContainedObject>(32);
    private final List<ZipFileSet> zipFileSets = new ArrayList<ZipFileSet>(8);
    private final List<FileSet> zipGroupFileSets = new ArrayList<FileSet>(8);

    private Path classpath = null;
    private ClassFilter classFilter = null;
    // TODO: Remove destDir functionality? It's not really standard to a jar task.
    private File destDir = null;
    private final List<PathResolver> pathResolvers = new LinkedList<PathResolver>();
    private final Set<String> resolved = new HashSet<String>();

    /** merged manifests added through addConfiguredManifest */
    private Manifest configuredManifest;

    /** merged manifests added through filesets */
    private Manifest filesetManifest;

    /**
     * whether to merge the main section of fileset manifests; value is true if filesetmanifest is
     * 'merge'
     */
    private final boolean mergeManifestsMain = true;

    /** the manifest specified by the 'manifest' attribute * */
    private Manifest manifest;

    /**
     * The file found from the 'manifest' attribute. This can be either the location of a manifest,
     * or the name of a jar added through a fileset. If its the name of an added jar, the manifest
     * is looked for in META-INF/MANIFEST.MF
     */
    private File manifestFile;

    /** jar index is JDK 1.3+ only */
    private boolean index = false;

    /** Constructor for the GenJar object */
    public GenJar()
    {
        setTaskName("GenJar");
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
     * Sets the name of the directory where the classes will be copied.
     *
     * @param path The directory name.
     */
    public void setDestDir(Path path)
    {
        destDir = getProject().resolveFile(path.toString());
    }

    /**
     * The manifest file to use. This can be either the location of a manifest, or the name of a jar
     * added through a fileset. If its the name of an added jar, the task expects the manifest to be
     * in the jar at META-INF/MANIFEST.MF.
     *
     * TODO: Eliminate local manifest handling?
     *
     * @param manifestFile
     */
    @Override
    public void setManifest(File manifestFile)
    {
        if (!manifestFile.exists())
        {
            throw new BuildException("Manifest file: " + manifestFile + " does not exist.", getLocation());
        }

        this.manifestFile = manifestFile;
        super.setManifest(manifestFile);
    }

    /**
     * Allows the manifest for the archive file to be provided inline in the build file rather than
     * in an external file.
     *
     * @param newManifest
     * @throws ManifestException
     */
    @Override
    public void addConfiguredManifest(Manifest newManifest) throws ManifestException
    {
        if (configuredManifest == null)
        {
            configuredManifest = newManifest;
        }
        else
        {
            configuredManifest.merge(newManifest);
        }
    }

    /**
     * Builds a <class> element.
     *
     * @return A <class> element.
     */
    public RootClass createClass()
    {
        RootClass cs = new RootClass(getProject());

        containedObjects.add(cs);
        return cs;
    }

    /**
     * Adds a root class.
     */
    public void addClass(RootClass rootClass)
    {
        containedObjects.add(rootClass);
    }

    /**
     * Adds a contained FileSet
     */
    @Override
    public void addFileset(FileSet fs)
    {
        containedObjects.add(new ContainedFileSet(getProject(), fs));
    }

    /**
     * Adds a contained ZipFileSet
     */
    @Override
    public void addZipfileset(ZipFileSet fs)
    {
        addFileset(fs);
        zipFileSets.add(fs);
    }

    /**
     * Adds a contained ZipGroupFileSet
     */
    public void addZipgroupfileset(FileSet fs)
    {
//        addFileset(fs);
        zipGroupFileSets.add(fs);
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
    @SuppressWarnings("unchecked")
    public void execute() throws BuildException
    {
        long start = System.currentTimeMillis();

        if (classFilter == null)
        {
            classFilter = new ClassFilter(getProject());
        }

        getProject().log("GenJar Ver: 0.4.0", Project.MSG_VERBOSE);
        if ((getDestFile() == null) && (destDir == null))
        {
            throw new BuildException("GenJar: Either a destfile or destdir attribute is required", getLocation());
        }

        //
        // set up the classpath & resolvers - file/zip
        //
        try
        {
            if (classpath == null)
            {
                classpath = new Path(getProject());
            }
            if (!classpath.isReference())
            {
                //
                // add the system path now - AFTER all other paths are
                // specified
                //
                classpath.addExisting(Path.systemClasspath);
            }
            getProject().log("Initializing Path Resolvers", Project.MSG_VERBOSE);
            getProject().log("Classpath:" + classpath, Project.MSG_VERBOSE);
            initPathResolvers();
        }
        catch (IOException ioe)
        {
            throw new BuildException("Unable to process classpath: " + ioe, getLocation());
        }

        try
        {
            for (FileSet zipGroupFileSet : zipGroupFileSets)
            {
                for (Iterator i = zipGroupFileSet.iterator(); i.hasNext();)
                {
                    FileResource fileResource = (FileResource) i.next();
                    ContainedFileSet cfs = new ContainedFileSet(getProject(), new ZipFile(fileResource.getFile()));
                    containedObjects.add(cfs);
                }
            }
        }
        catch (IOException ioe)
        {
            throw new BuildException("Unable to process zipgroupfileset(s): " + ioe, getLocation());
        }

        //
        // run over all the resource and class specifications
        // given in the project file
        // resources are resolved to full path names while
        // class specifications are exploded to dependency
        // graphs - when done, getJarEntries() returns a list
        // of all entries generated by this JarSpec
        //
        Set<GenJarEntry> jarEntrySpecs = new LinkedHashSet<GenJarEntry>();

        for (Iterator<ContainedObject> it = containedObjects.iterator(); it.hasNext();)
        {
            ContainedObject js = it.next();

            try
            {
                js.resolve(this);
            }
            catch (IOException ioe)
            {
                throw new BuildException("Unable to resolve: ", ioe, getLocation());
            }

            //
            // before adding a new jarspec - see if it already exists
            // first entry added to jar always wins
            //
            List<GenJarEntry> jarEntries = js.getJarEntries();

            for (Iterator<GenJarEntry> iter = jarEntries.iterator(); iter.hasNext();)
            {
                GenJarEntry spec = iter.next();

                if (!jarEntrySpecs.contains(spec))
                {
                    jarEntrySpecs.add(spec);
                    getProject().log("Adding " + spec.getJarName(), Project.MSG_VERBOSE);
                }
                else
                {
                    getProject().log("Duplicate (ignored): " + spec.getJarName(), Project.MSG_VERBOSE);
                }
            }
        }

        if (getDestFile() != null)
        {
            log("Generating jar: " + getDestFile());

            // prep the manifest
            java.util.jar.Manifest mf;

            try
            {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintWriter writer = new PrintWriter(baos);

                if (manifest == null)
                {
                    // manifest = Manifest.getDefaultManifest();
                    manifest = createManifest();
                }
                else
                {
                    for (Enumeration<String> e = manifest.getWarnings(); e.hasMoreElements();)
                    {
                        log("Manifest warning: " + e.nextElement(), Project.MSG_WARN);
                    }
                }
                manifest.write(writer);
                writer.flush();

                ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

                mf = new java.util.jar.Manifest(bais);
            }
            catch (IOException ioe)
            {
                throw new BuildException("Error creating the manifest: ", ioe, getLocation());
            }

            JarOutputStream jout = null;
            InputStream is = null;

            try
            {
                jout = new JarOutputStream(new FileOutputStream(getDestFile()), mf);

                for (Iterator<GenJarEntry> it = jarEntrySpecs.iterator(); it.hasNext();)
                {
                    GenJarEntry jes = it.next();
                    JarEntry entry = new JarEntry(jes.getJarName());

                    is = resolveEntry(jes);

                    if (is == null)
                    {
                        getProject().log("Unable to locate previously resolved resource", Project.MSG_ERR);
                        getProject().log("       Jar Name:" + jes.getJarName(), Project.MSG_ERR);
                        getProject().log(" Resoved Source:" + jes.getSourceFile(), Project.MSG_ERR);
                        try
                        {
                            jout.close();
                        }
                        catch (IOException ioe)
                        {}
                        throw new BuildException("Jar component not found (" + jes.getJarName() + ')', getLocation());
                    }
                    jout.putNextEntry(entry);

                    byte[] buff = new byte[4096];
                    int len;

                    while ((len = is.read(buff, 0, buff.length)) != -1)
                    {
                        jout.write(buff, 0, len);
                    }
                    jout.closeEntry();
                    is.close();

                    getProject().log("Added: " + jes.getJarName(), Project.MSG_VERBOSE);
                }

                if (index)
                {
                    addIndexList(jout, jarEntrySpecs);
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
                    if (jout != null)
                    {
                        jout.close();
                    }
                }
                catch (IOException ioe)
                {}
            }
            log("Jar Generated (" + (System.currentTimeMillis() - start) + " ms)");
        }

        // Destdir has been specified, so try to generate the dependencies on disk
        if (destDir != null)
        {
            log("Generating class structure in " + destDir);
            if (destDir != null && !destDir.isDirectory())
            {
                throw new BuildException("Destination directory \"" + destDir + "\" does not exist "
                    + "or is not a directory", getLocation());
            }

            FileOutputStream fileout = null;
            InputStream is = null;

            try
            {
                for (GenJarEntry jes : jarEntrySpecs)
                {
                    String classname = jes.getJarName();
                    int i = classname.lastIndexOf("/");
                    String path = "";

                    if (i > 0)
                    {
                        path = classname.substring(0, i);
                    }
                    classname = classname.substring(i + 1);

                    File filepath = new File(destDir, path);

                    if (!filepath.exists())
                    {
                        if (!filepath.mkdirs())
                        {
                            throw new BuildException("Unable to create directory " + filepath.getAbsolutePath(),
                                getLocation());
                        }
                    }

                    File classfile = new File(filepath, classname);

                    getProject().log("Writing: " + classfile.getAbsolutePath(), Project.MSG_DEBUG);
                    fileout = new FileOutputStream(classfile);
                    is = resolveEntry(jes);

                    if (is == null)
                    {
                        getProject().log("Unable to locate previously resolved resource", Project.MSG_ERR);
                        getProject().log("       Jar Name:" + jes.getJarName(), Project.MSG_ERR);
                        getProject().log(" Resoved Source:" + jes.getSourceFile(), Project.MSG_ERR);
                        try
                        {
                            fileout.close();
                        }
                        catch (IOException ioe)
                        {}
                        throw new BuildException("File not found (" + jes.getJarName() + ')', getLocation());
                    }

                    byte[] buff = new byte[4096];
                    int len;

                    while ((len = is.read(buff, 0, buff.length)) != -1)
                    {
                        fileout.write(buff, 0, len);
                    }
                    fileout.close();
                    is.close();

                    getProject().log("Wrote: " + classfile.getName(), Project.MSG_VERBOSE);
                }
            }
            catch (IOException ioe)
            {
                throw new BuildException("Unable to write classes ", ioe, getLocation());
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
                    if (fileout != null)
                    {
                        fileout.close();
                    }
                }
                catch (IOException ioe)
                {}
            }
            log("Class Structure Generated (" + (System.currentTimeMillis() - start) + " ms)");
        }

        // Close all the resolvers
        for (PathResolver resolver : pathResolvers)
        {
            resolver.close();
        }
    }

    /**
     * Description of the Method
     *
     * @param spec Description of the Parameter
     * @return Description of the Return Value
     * @throws IOException Description of the Exception
     */
    InputStream resolveEntry(GenJarEntry spec) throws IOException
    {
        InputStream is = null;

        for (PathResolver resolver : pathResolvers)
        {
            is = resolver.resolve(spec);
            if (is != null)
            {
                return is;
            }
        }
        return null;
    }

    // =====================================================================
    // TODO: class dependency determination needs to move to either its own
    // class or to ClassSpec
    // =====================================================================

    /**
     * Generates a list of all classes upon which the list of classes depend.
     *
     * @param jarEntrySpecs List of <code>JarEntrySpec</code>s used as a list of class names from
     *            which to start.
     */
    void generateDependancies(List<GenJarEntry> jarEntrySpecs)
    {
        List<String> dependants = generateClassDependancies(jarEntrySpecs.iterator());

        for (Iterator<String> it = dependants.iterator(); it.hasNext();)
        {
            jarEntrySpecs.add(new GenJarEntry(it.next(), null));
        }
    }

    private Manifest getManifest()
    {

        Manifest newManifest = null;
        Reader r = null;

        try
        {
            r = new FileReader(manifestFile);
            newManifest = getManifest(r);
        }
        catch (IOException e)
        {
            throw new BuildException("Unable to read manifest file: " + manifestFile + " (" + e.getMessage() + ")", e);
        }
        finally
        {
            if (r != null)
            {
                try
                {
                    r.close();
                }
                catch (IOException e)
                {
                    // do nothing
                }
            }
        }
        return newManifest;
    }

    private Manifest getManifest(Reader r)
    {

        Manifest newManifest = null;

        try
        {
            newManifest = new Manifest(r);
        }
        catch (ManifestException e)
        {
            log("Manifest is invalid: " + e.getMessage(), Project.MSG_ERR);
            throw new BuildException("Invalid Manifest: " + manifestFile, e, getLocation());
        }
        catch (IOException e)
        {
            throw new BuildException("Unable to read manifest file" + " (" + e.getMessage() + ")", e);
        }
        return newManifest;
    }

    //
    // TODO: path resolution needs to move to its own class
    //

    /**
     * Iterate through the classpath and create an array of all the <code>PathResolver</code> s
     *
     * @throws IOException Description of the Exception
     */
    private void initPathResolvers() throws IOException
    {
        String[] classpathEntries = classpath.list();

        for (String entry : classpathEntries)
        {
            final File f = new File(entry);

            if (!f.exists())
            {
                continue;
            }

            final String suffix = entry.substring(entry.length() - 4);
            if (suffix.equalsIgnoreCase(".jar") || suffix.equalsIgnoreCase(".zip"))
            {
                pathResolvers.add(new ZipResolver(f, getProject()));
            }
            else if (f.isDirectory())
            {
                pathResolvers.add(new FileResolver(f, getProject()));
            }
            else
            {
                throw new BuildException(f.getName() + " is not a valid classpath component", getLocation());
            }
        }

        for (ZipFileSet zipFileSet : zipFileSets)
        {
            pathResolvers.add(new ZipResolver(zipFileSet.getSrc(), getProject()));
        }

        for (FileSet fileSet : zipGroupFileSets)
        {
            for (String filename : fileSet.getDirectoryScanner().getIncludedFiles())
            {
                pathResolvers.add(new ZipResolver(new File(fileSet.getDir(), filename), getProject()));
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
    private List<String> generateClassDependancies(Iterator<GenJarEntry> it)
    {
        Analyzer ga = new Analyzer(getProject(), classFilter.getIncludeList(), classFilter.getExcludeList());

        ga.addClassPath(classpath);

        while (it.hasNext())
        {
            GenJarEntry js = it.next();
            String classname = js.getJarName();

            if (!resolved.contains(classname))
            {
                resolved.add(classname);

                // Ant's analyzer framework adds the .class, so strip it here
                if (classname.endsWith(".class"))
                {
                    classname = classname.substring(0, classname.length() - 6);
                }
                ga.addRootClass(classname);
            }
        }

        LinkedList<String> deps = new LinkedList<String>();
        Enumeration<String> e = ga.getClassDependencies();

        while (e.hasMoreElements())
        {
            String dep = e.nextElement();

            // Now convert back to / and add .class
            deps.add(dep.replace('.', '/') + ".class");
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
     * @param jout An opened JarOutPutStream to write to index file to.
     * @param jarEntrySpecs A list of all the entried in the jar.
     * @throws IOException thrown if there is an error while creating the index and adding it to the
     *             zip stream.
     */
    private void addIndexList(JarOutputStream jout, Set<GenJarEntry> jarEntrySpecs) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // encoding must be UTF8 as specified in the specs.
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, "UTF8"));

        // version-info blankline
        writer.println("JarIndex-Version: 1.0");
        writer.println();

        // header newline
        writer.println(getDestFile().getName());

        LinkedList<String> entryDirs = new LinkedList<String>();

        for (Iterator<GenJarEntry> it = jarEntrySpecs.iterator(); it.hasNext();)
        {
            GenJarEntry spec = it.next();
            String entry = spec.getJarName();

            entry = entry.replace('\\', '/');

            int pos = entry.lastIndexOf('/');

            if (pos != -1)
            {
                entry = entry.substring(0, pos);
            }
            else
            {
                // Class is in root and can be ignored? maybe?
                continue;
            }
            // looks like nothing from META-INF should be added
            // and the check is not case insensitive.
            // see sun.misc.JarIndex
            if (entry.startsWith("META-INF"))
            {
                continue;
            }
            // Only add the path once
            if (!entryDirs.contains(entry))
            {
                entryDirs.add(entry);
            }
        }

        for (Iterator<String> it = entryDirs.iterator(); it.hasNext();)
        {
            writer.println(it.next());
        }

        writer.flush();

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        JarEntry entry = new JarEntry("META-INF/INDEX.LIST");

        jout.putNextEntry(entry);

        byte[] buff = new byte[4096];
        int len;

        while ((len = bais.read(buff, 0, buff.length)) != -1)
        {
            jout.write(buff, 0, len);
        }
        jout.closeEntry();
    }

    // Some Manifest code borrowed from Ant's jar task

    private Manifest createManifest() throws BuildException
    {
        try
        {
            Manifest finalManifest = Manifest.getDefaultManifest();

            if (manifest == null)
            {
                if (manifestFile != null)
                {
                    // if we haven't got the manifest yet, attempt to
                    // get it now and have manifest be the final merge
                    manifest = getManifest();
                    finalManifest.merge(filesetManifest);
                    finalManifest.merge(configuredManifest);
                    finalManifest.merge(manifest, !mergeManifestsMain);
                }
                else if (configuredManifest != null)
                {
                    // configuredManifest is the final merge
                    finalManifest.merge(filesetManifest);
                    finalManifest.merge(configuredManifest, !mergeManifestsMain);
                }
                else if (filesetManifest != null)
                {
                    // filesetManifest is the final (and only) merge
                    finalManifest.merge(filesetManifest, !mergeManifestsMain);
                }
            }
            else
            {
                // manifest is the final merge
                finalManifest.merge(filesetManifest);
                finalManifest.merge(configuredManifest);
                finalManifest.merge(manifest, !mergeManifestsMain);
            }

            return finalManifest;
        }
        catch (ManifestException e)
        {
            log("Manifest is invalid: " + e.getMessage(), Project.MSG_ERR);
            throw new BuildException("Invalid Manifest", e, getLocation());
        }
    }
}
// vi:set ts=4 sw=4:
