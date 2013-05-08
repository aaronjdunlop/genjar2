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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.sf.genjar.zip.ZipEntry;
import net.sf.genjar.zip.ZipOutputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.FileScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.ZipFileSet;
import org.apache.tools.ant.util.FileUtils;

/**
 * Driver class for the GenJar task.
 * <p>
 * 
 * This class is instantiated when Ant encounters the &lt;genjar&gt; element.
 * 
 * @author John W. Kohler
 * @author Jesse Stockall
 * @author Aaron Dunlop
 * @created February 23, 2003
 * @version $Revision: 1.4 $ $Date: 2003/02/23 18:25:23 $
 */
public class GenJar extends BaseGenJarTask {

    private final List<RootClass> rootClasses = new ArrayList<RootClass>(32);
    private final List<RootClassList> rootClassLists = new ArrayList<RootClassList>(32);

    private final List<FileSet> filesets = new ArrayList<FileSet>();
    private final List<FileSet> groupfilesets = new ArrayList<FileSet>();

    private Path classpath = null;
    private Path runtimeClasspath = null;
    private ClassFilter classFilter = null;
    private final List<BaseResolver> runtimeClasspathResolvers = new LinkedList<BaseResolver>();

    /** Constructor for the GenJar object */
    public GenJar() {
        setTaskName("GenJar");
        setUpdate(false);
    }

    /**
     * Adds a classpath
     * 
     * @param path
     */
    public void addClasspath(final Path path) {
        this.classpath = path;
    }

    /**
     * Sets the <classpathref> attribute.
     * 
     * @param r The new classpath ref.
     */
    public void setClasspathRef(final Reference r) {
        final Path cp = new Path(getProject());
        cp.setRefid(r);
        addClasspath(cp);
    }

    /**
     * Adds a runtime classpath (<runtimeclasspath>)
     * 
     * @param path
     */
    public void addRuntimeClasspath(final Path path) {
        this.runtimeClasspath = path;
    }

    /**
     * Sets the <runtimeclasspathref> attribute.
     * 
     * @param r The new classpath ref.
     */
    public void setRuntimeClasspathRef(final Reference r) {
        final Path cp = new Path(getProject());
        cp.setRefid(r);
        addRuntimeClasspath(cp);
    }

    /**
     * Builds a <class> element.
     * 
     * @return A <class> element.
     */
    public RootClass createClass() {
        final RootClass rc = new RootClass(getProject());
        rootClasses.add(rc);
        return rc;
    }

    /**
     * Builds a <classes> element.
     * 
     * @return A <classes> element.
     */
    public RootClassList createClasses() {
        final RootClassList rc = new RootClassList();
        rootClassLists.add(rc);
        return rc;
    }

    /**
     * Adds a root class (<class>).
     */
    public void addClass(final RootClass rootClass) {
        rootClasses.add(rootClass);
    }

    /**
     * Adds a contained FileSet (<fileset>)
     */
    @Override
    public void addFileset(final FileSet fs) {
        filesets.add(fs);
    }

    /**
     * Adds a contained ZipFileSet (<zipfileset>)
     */
    @Override
    public void addZipfileset(final ZipFileSet fs) {
        filesets.add(fs);
    }

    /**
     * Adds a contained ZipGroupFileSet (<zipgroupfileset>)
     */
    public void addZipgroupfileset(final FileSet fs) {
        groupfilesets.add(fs);
    }

    /**
     * Builds a classfilter element.
     * 
     * @return A <classfilter> element.
     */
    public ClassFilter createClassfilter() {
        if (classFilter == null) {
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
     * <li>resolve resource file paths resolve class file paths generate dependancy graphs for class files and resolve
     * those paths check for duplicates
     * <li>generate manifest entries for all candidate files
     * <li>build jar
     * </ol>
     * 
     * 
     * @throws BuildException Description of the Exception
     */
    @Override
    public void execute() throws BuildException {
        final long start = System.currentTimeMillis();

        if (classFilter == null) {
            classFilter = new ClassFilter(getProject());
        }

        processGroupFilesets();

        getProject().log("GenJar Ver: 2.0.0", Project.MSG_VERBOSE);
        if (getDestFile() == null) {
            throw new BuildException("GenJar: destfile attribute is required", getLocation());
        }

        //
        // Set up the classpath & resolvers - file/zip
        //
        try {
            if (classpath == null) {
                classpath = new Path(getProject());
            }
            if (!classpath.isReference()) {
                // Add the system path now - AFTER all other paths are specified
                classpath.addExisting(Path.systemClasspath);
            }

            getProject().log("Initializing Path Resolvers", Project.MSG_VERBOSE);
            getProject().log("Classpath:" + classpath, Project.MSG_VERBOSE);
            initResolvers();
        } catch (final IOException ioe) {
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
        final Set<String> archiveEntries = new HashSet<String>();

        for (final RootClassList rc : rootClassLists) {
            for (final String classname : rc.getClassNames()) {
                rootClasses.add(new RootClass(getProject(), classname));
            }
        }

        for (final RootClass rc : rootClasses) {
            rc.resolve(this);

            //
            // before adding a new jarspec - see if it already exists
            // first entry added to jar always wins
            //
            for (final String entry : rc.getJarEntries()) {
                if (!archiveEntries.contains(entry)) {
                    archiveEntries.add(entry);
                    getProject().log("Adding " + entry, Project.MSG_VERBOSE);
                } else {
                    getProject().log("Duplicate (ignored): " + entry, Project.MSG_VERBOSE);
                }
            }
        }

        if (zipFile.exists()) {
            zipFile.delete();
        }

        log("Generating jar: " + getDestFile());

        ZipEntry ze = null;
        ZipOutputStream zOut = null;
        try {
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
            for (final String jarEntry : archiveEntries) {
                ze = resolveEntry(jarEntry);
                if (ze == null) {
                    // Ignore classes in the runtime classpath (they don't need to be included in the jar)
                    if (inRuntimeClasspath(jarEntry)) {
                        continue;
                    }

                    getProject().log("Unable to locate required resource: " + jarEntry, Project.MSG_ERR);
                    throw new BuildException("Unable to locate required resource [" + jarEntry + ']', getLocation());
                }
                final InputStream is = ze.getInputStream();
                getProject().log("Archiving: " + ze.getName(), Project.MSG_VERBOSE);

                zOut.putNextEntry(ze);
                for (int read = is.read(buf); read != -1; read = is.read(buf)) {
                    zOut.write(buf, 0, read);
                }
                zOut.closeEntry();
                is.close();
            }

            // Add the explicit resource collections to the archive.
            for (int i = 0; i < fss.length; i++) {
                if (addThem[i].length != 0) {
                    addResourcesToArchive(fss[i], addThem[i], zOut, archiveEntries);
                }
            }

        } catch (final FileNotFoundException fnfe) {
            throw new BuildException("Unable to access jar file (" + getDestFile() + ") msg:", fnfe, getLocation());
        } catch (final IOException ioe) {
            throw new BuildException("Unable to create jar: " + ioe.getMessage(), ioe, getLocation());
        } finally {
            try {
                if (ze != null) {
                    ze.getInputStream().close();
                }
            } catch (final IOException ioe) {
            }
            try {
                if (zOut != null) {
                    zOut.close();
                }
            } catch (final IOException ioe) {
            }
        }
        log("Jar Generated (" + (System.currentTimeMillis() - start) + " ms)");

        // Close all the resolvers
        for (final BaseResolver resolver : resolvers) {
            resolver.close();
        }
    }

    /**
     * Add the given resources to the archive.
     * 
     * @param rc may give additional information like fullpath or permissions.
     * @param resources the resources to add
     * @param zOut the stream to write to
     * @throws IOException on error
     * 
     * @since Ant 1.7
     */
    private void addResourcesToArchive(final ResourceCollection rc, final Resource[] resources,
            final ZipOutputStream zOut, final Set<String> archiveEntries) throws IOException {
        if (rc instanceof ZipFileSet) {
            addZipfilesetToArchive((ZipFileSet) rc, resources, zOut, archiveEntries);
        } else {
            // TODO Add entries from this ResourceCollection to archiveEntries so we won't re-add
            // them later.
            super.addResources(rc, resources, zOut);
        }
    }

    /**
     * Add zipfileset to the archive.
     * 
     * @param fileset may give additional information like fullpath or permissions.
     * @param resources the resources to add
     * @param zOut the stream to write to
     * @throws IOException on error
     * 
     * @since Ant 1.5.2
     */
    protected final void addZipfilesetToArchive(final ZipFileSet zfs, final Resource[] resources,
            final ZipOutputStream zOut, final Set<String> archiveEntries) throws IOException {
        final String prefix = zfs.getPrefix(getProject());
        final String fullpath = zfs.getFullpath(getProject());
        final int dirMode = zfs.getDirMode(getProject());

        if (prefix.length() > 0 && fullpath.length() > 0) {
            throw new BuildException("Both prefix and fullpath attributes must" + " not be set on the same fileset.");
        }

        if (resources.length != 1 && fullpath.length() > 0) {
            throw new BuildException("fullpath attribute may only be specified" + " for filesets that specify a single"
                    + " file.");
        }

        final net.sf.genjar.zip.ZipFile zf = new net.sf.genjar.zip.ZipFile(zfs.getSrc(getProject()));
        try {
            final byte[] buf = new byte[8192];
            for (int i = 0; i < resources.length; i++) {
                final String name = resources[i].getName().replace(File.separatorChar, '/');
                // Don't re-add entries already in the archive or manifests from an included jar
                if (archiveEntries.contains(name) || name.equals("META-INF/MANIFEST.MF")) {
                    continue;
                }

                archiveEntries.add(name);

                final int nextToLastSlash = name.lastIndexOf("/", name.length() - 2);
                if (nextToLastSlash != -1) {
                    addParentDirs(null, name.substring(0, nextToLastSlash + 1), zOut, prefix, dirMode);
                }

                final ZipEntry ze = new ZipEntry(zf.getEntry(resources[i].getName()), true);
                addParentDirs(null, name, zOut, prefix, dirMode);

                final InputStream is = zf.getInputStream(ze);
                try {
                    zOut.putNextEntry(ze);
                    for (int read = is.read(buf); read != -1; read = is.read(buf)) {
                        zOut.write(buf, 0, read);
                    }
                    zOut.closeEntry();
                } finally {
                    FileUtils.close(is);
                }
            }
        } finally {
            zf.close();
        }
    }

    /**
     * Returns true if the specified class file is found in a library specified as runtime classpath. Jars in the
     * runtime classpath are included in the generated META-INF/MANIFEST.MF.
     * 
     * @param classfileName
     * @return true if the specified class file is found in a runtime library
     * @throws IOException
     */
    private boolean inRuntimeClasspath(final String classfileName) throws IOException {
        for (final BaseResolver resolver : runtimeClasspathResolvers) {
            if (resolver.resolve(classfileName) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Generates a list of all classes upon which the list of classes depend.
     * 
     * @param jarEntrySpecs List of <code>JarEntrySpec</code>s used as a list of class names from which to start.
     */
    void generateDependancies(final Collection<String> jarEntries) {
        jarEntries.addAll(generateClassDependencies(jarEntries));
    }

    /**
     * Iterate through the classpath and create an array of all the {@link BaseResolver}s
     * 
     * @throws IOException Description of the Exception
     */
    private void initResolvers() throws IOException {
        resolvers.addAll(initResolvers(classpath));

        for (final FileSet fs : filesets) {
            if (fs instanceof ZipFileSet) {
                resolvers.add(new ArchiveResolver((ZipFileSet) fs));
            } else {
                resolvers.add(new FileResolver(fs));
            }
        }

        runtimeClasspathResolvers.addAll(initResolvers(runtimeClasspath));
    }

    /**
     * Generates a list of classes upon which the named class depends.
     * 
     * @param it Iterator of all the classes to use when building the dependencies.
     * @return A List of all the class dependencies.
     */
    @SuppressWarnings("unchecked")
    private Set<String> generateClassDependencies(final Collection<String> jarEntries) {
        final Analyzer ga = new Analyzer(getProject(), classFilter.getIncludeList(), classFilter.getExcludeList());
        ga.addClassPath(classpath);

        final Set<String> resolvedClasses = new HashSet<String>();

        for (String classname : jarEntries) {
            if (!resolvedClasses.contains(classname)) {
                resolvedClasses.add(classname);

                // Ant's analyzer framework adds the .class, so strip it here
                if (classname.endsWith(".class")) {
                    classname = classname.substring(0, classname.length() - 6);
                }
                ga.addRootClass(classname);
            }
        }

        final Set<String> deps = new HashSet<String>();
        final Enumeration<String> e = ga.getClassDependencies();

        while (e.hasMoreElements()) {
            // Now convert back to / and add .class
            deps.add(e.nextElement().replace('.', '/') + ".class");
        }
        return deps;
    }

    /**
     * Stolen verbatim from Ant's Jar task, since this method is private and can't be called from genjar
     */
    private void processGroupFilesets() {
        // Add the files found in the groupfileset to filesets
        for (final FileSet fs : groupfilesets) {
            log("Processing groupfileset ", Project.MSG_VERBOSE);
            final FileScanner scanner = fs.getDirectoryScanner(getProject());
            final File basedir = scanner.getBasedir();

            for (final String filename : scanner.getIncludedFiles()) {
                log("Adding file " + filename + " to fileset", Project.MSG_VERBOSE);
                final ZipFileSet zf = new ZipFileSet();
                zf.setProject(getProject());
                zf.setSrc(new File(basedir, filename));
                filesets.add(zf);
            }
        }
    }
}
