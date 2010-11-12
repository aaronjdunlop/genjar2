package net.sf.genjar;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import net.sf.genjar.zip.ZipEntry;
import net.sf.genjar.zip.ZipOutputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;

/**
 * Creates a source jar (generally to accompany a class-file jar created with {@link GenJar}). This class is
 * instantiated when Ant encounters the &lt;srcjar&gt; element.
 * 
 * @author Aaron Dunlop
 * @created Nov 5, 2010
 * @version $Revision: 1.4 $ $Date: 2003/02/23 18:25:23 $
 */
public class SrcJar extends BaseGenJarTask {

    private JarFile classJar;
    private Path sourcepath;

    /** Constructor for the GenJar object */
    public SrcJar() {
        setTaskName("SrcJar");
        setUpdate(false);
    }

    /**
     * Adds a source path - a location to search for matching source files
     * 
     * @param path
     */
    public void addSourcepath(final Path path) {
        this.sourcepath = path;
    }

    /**
     * Specifies the jar file to search for class files
     * 
     * @param path
     */
    public void setClassjar(final String classjar) {
        try {
            this.classJar = new JarFile(classjar);
        } catch (final Exception e) {
            throw new BuildException(e);
        }
    }

    /**
     * Constructs a source-file jar based on the contents of a class-file jar.
     * 
     * @throws BuildException If a required attribute is missing
     */
    @Override
    public void execute() throws BuildException {
        final long start = System.currentTimeMillis();

        getProject().log("SrcJar Ver: 2.0.0", Project.MSG_VERBOSE);

        if (getDestFile() == null) {
            throw new BuildException("SrcJar: destfile attribute is required", getLocation());
        }
        if (classJar == null) {
            throw new BuildException("SrcJar: classjar is required", getLocation());
        }
        if (sourcepath == null) {
            throw new BuildException("SrcJar: sourcepath is required", getLocation());
        }

        //
        // Set up the classpath & resolvers - file/zip
        //
        try {
            // if (sourcepath == null) {
            // sourcepath = new Path(getProject());
            // }
            // if (!sourcepath.isReference()) {
            // // Add the system path now - AFTER all other paths are specified
            // sourcepath.addExisting(Path.systemClasspath);
            // }

            getProject().log("Initializing Path Resolvers", Project.MSG_VERBOSE);
            getProject().log("Sourcepath:" + sourcepath, Project.MSG_VERBOSE);

            resolvers.addAll(initResolvers(sourcepath));

            // Add a resolver to search the class jar itself for resources
            resolvers.add(new ArchiveResolver(classJar.getName()));

        } catch (final IOException ioe) {
            throw new BuildException("Unable to process classpath: " + ioe, getLocation());
        }

        // Iterate over all entries in the source jar and look for matching source files in source path
        final Set<String> archiveEntries = new HashSet<String>();
        for (final Enumeration<JarEntry> e = classJar.entries(); e.hasMoreElements();) {
            final JarEntry je = e.nextElement();
            final String javaFile = je.getName().replaceFirst("(\\$\\w+)*\\.class", ".java");

            if (!archiveEntries.contains(javaFile)) {
                archiveEntries.add(javaFile);
                getProject().log("Adding " + javaFile, Project.MSG_VERBOSE);
            } else {
                getProject().log("Duplicate (ignored): " + javaFile, Project.MSG_VERBOSE);
            }
        }

        if (zipFile.exists()) {
            zipFile.delete();
        }

        log("Generating jar: " + getDestFile());

        ZipEntry ze = null;
        ZipOutputStream zOut = null;
        try {
            // Create the jar file
            zOut = new ZipOutputStream(zipFile);
            zOut.setEncoding(getEncoding());
            zOut.setMethod(ZipOutputStream.DEFLATED);
            zOut.setLevel(getLevel());
            initZipOutputStream(zOut);

            final byte[] buf = new byte[16384];
            for (final String entry : archiveEntries) {
                ze = resolveEntry(entry);
                if (ze == null) {
                    getProject().log("Unable to locate java file " + entry, Project.MSG_ERR);
                    continue;
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
}
