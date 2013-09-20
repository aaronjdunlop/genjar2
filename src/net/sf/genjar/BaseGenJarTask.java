package net.sf.genjar;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipException;

import net.sf.genjar.zip.ZipEntry;
import net.sf.genjar.zip.ZipFile;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Jar;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.ZipFileSet;

public abstract class BaseGenJarTask extends Jar {

    protected final List<BaseResolver> resolvers = new LinkedList<BaseResolver>();

    /**
     * Finds a file in one of the tasks resolvers.
     * 
     * 
     * @param filename File to resolve
     * @return ZipEntry of resolved class location, or null if not found
     * @throws IOException if an I/O error occurs
     */
    protected ZipEntry resolveEntry(final String filename) throws IOException {
        for (final BaseResolver resolver : resolvers) {
            final ZipEntry ze = resolver.resolve(filename);
            if (ze != null) {
                return ze;
            }
        }
        return null;
    }

    protected List<BaseResolver> initResolvers(final Path path) throws IOException {
        final List<BaseResolver> tempResolvers = new ArrayList<BaseResolver>();

        if (path == null) {
            return tempResolvers;
        }

        for (final String classpathEntry : path.list()) {
            final File f = new File(classpathEntry);

            if (!f.exists()) {
                continue;
            }

            final String suffix = classpathEntry.substring(classpathEntry.length() - 4);
            if (suffix.equalsIgnoreCase(".jar") || suffix.equalsIgnoreCase(".zip")) {
                final ZipFileSet zfs = new ZipFileSet();
                zfs.setSrc(f);
                zfs.setProject(getProject());
                tempResolvers.add(new ArchiveResolver(zfs));
            } else if (f.isDirectory()) {
                final FileSet fs = new FileSet();
                fs.setDir(f);
                fs.setProject(getProject());
                tempResolvers.add(new FileResolver(fs));
            } else {
                throw new BuildException(f.getName() + " is not a valid classpath component", getLocation());
            }
        }
        return tempResolvers;
    }

    /**
     * Searches a path for specified files. Subclasses search specific types of paths (e.g. filesystem directory,
     * Zip/Jar file, etc.)
     */
    public static abstract class BaseResolver {

        /** Patterns to match include patterns are defined or null if no patterns are set */
        protected HashSet<String> includedFiles;

        protected BaseResolver(final FileSet fileset) {
            if (fileset != null && fileset.hasPatterns()) {
                includedFiles = new HashSet<String>();
                for (final String filename : fileset.getDirectoryScanner(fileset.getProject()).getIncludedFiles()) {
                    includedFiles.add(filename.replaceAll("\\\\", "/"));
                }
            }
        }

        /**
         * Searches the path for the specified file.
         * 
         * @param entry Filename to resolve
         * @return ZipEntry opened to the specified file, or null if not found
         * @exception IOException if any errors are encountered
         */
        protected abstract ZipEntry resolve(String entry) throws IOException;

        /**
         * Closes any resources opened by the resolver.
         */
        protected abstract void close();
    }

    /**
     * Searches a filesystem directory structure
     */
    public static class FileResolver extends BaseResolver {

        private final File baseDir;

        FileResolver(final FileSet fileset) {
            super(fileset);
            this.baseDir = fileset.getDir();
        }

        @Override
        public void close() {
        }

        @Override
        public ZipEntry resolve(final String jarEntry) throws IOException {
            if (includedFiles != null && !includedFiles.contains(jarEntry)) {
                return null;
            }
            final File f = new File(baseDir, jarEntry);
            if (!f.exists() || f.isDirectory()) {
                return null;
            }
            return new ZipEntry(jarEntry, new FileInputStream(f));
        }

        @Override
        public String toString() {
            return "FileResolver: " + baseDir.getAbsolutePath();
        }
    }

    /**
     * Searches a Zip or Jar archive
     */
    class ArchiveResolver extends BaseResolver {

        private final ZipFile zip;

        ArchiveResolver(final ZipFileSet fileset) throws IOException {
            super(fileset);
            zip = new ZipFile(fileset.getSrc());
        }

        ArchiveResolver(final String zipfileName) throws ZipException, IOException {
            super(null);
            zip = new ZipFile(new File(zipfileName));
        }

        @Override
        public void close() {
            // Close the archive (avoiding unnecessary file locks)
            if (zip != null) {
                try {
                    zip.close();
                } catch (final IOException ignore) {
                }
            }
        }

        @Override
        public ZipEntry resolve(final String jarEntry) throws IOException {
            if (includedFiles != null && !includedFiles.contains(jarEntry)) {
                return null;
            }
            final java.util.zip.ZipEntry ze = zip.getEntry(jarEntry);
            if (ze == null) {
                return null;
            }

            final ZipEntry compressedZe = new ZipEntry(ze, true);
            compressedZe.setInputStream(zip.getInputStream(compressedZe));
            return compressedZe;
        }

        @Override
        public String toString() {
            return "ArchiveResolver: " + zip.getName();
        }
    }
}
