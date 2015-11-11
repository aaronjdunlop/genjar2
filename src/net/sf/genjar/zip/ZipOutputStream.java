package net.sf.genjar.zip;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;

import org.apache.tools.ant.BuildException;
import org.apache.tools.zip.ZipLong;

/**
 * Extends {@link org.apache.tools.zip.ZipOutputStream}, making use of {@link ZipEntry}'s flag for already-compressed
 * files.
 * 
 * Warning, this involves accessing private fields from {@link org.apache.tools.zip.ZipOutputStream}, so it's somewhat
 * fragile. But it does improve build performance considerably.
 */
public class ZipOutputStream extends org.apache.tools.zip.ZipOutputStream {
    private final Field entryField;
    private final Field rafField;
    private Field localDataStartField;
    private final Field writtenField;

    private final RandomAccessFile raf;

    public ZipOutputStream(final File file) throws IOException {
        super(file);
        try {
            // Here be dragons... Accessing private fields of the superclass

            entryField = org.apache.tools.zip.ZipOutputStream.class.getDeclaredField("entry");
            entryField.setAccessible(true);

            // Get a handle to the RandomAccessFile
            rafField = org.apache.tools.zip.ZipOutputStream.class.getDeclaredField("raf");
            rafField.setAccessible(true);
            raf = (RandomAccessFile) rafField.get(this);

            try {
                localDataStartField = org.apache.tools.zip.ZipOutputStream.class.getDeclaredField("localDataStart");
                localDataStartField.setAccessible(true);
            } catch (final NoSuchFieldException e) {
                localDataStartField = null;
                // As of Ant 1.9, localDataStart was moved into the 'CurrentEntry' inner class - if this field reference
                // is null, we'll handle it in closeEntry()
            }

            writtenField = org.apache.tools.zip.ZipOutputStream.class.getDeclaredField("written");
            writtenField.setAccessible(true);

        } catch (final Exception e) {
            throw new BuildException(e);
        }
    }

    /**
     * Writes bytes to ZIP entry.
     * 
     * @param b the byte array to write
     * @param offset the start position to write from
     * @param length the number of bytes to write
     * @throws IOException on error
     */
    @Override
    public void write(final byte[] b, final int offset, final int length) throws IOException {
        try {
            final org.apache.tools.zip.ZipEntry entry = zipEntry();

            if (entry instanceof ZipEntry && entry.getMethod() == DEFLATED && ((ZipEntry) entry).isAlreadyCompressed()) {
                // First, write the compressed content
                writeOut(b, offset, length);

                // Update the 'written' field
                writtenField.setLong(this, writtenField.getLong(this) + length);
            } else {
                super.write(b, offset, length);
            }
        } catch (final IOException e) {
            throw e;
        } catch (final Exception e) {
            throw new BuildException(e);
        }
    }

    @Override
    public void closeEntry() throws IOException {
        try {
            final org.apache.tools.zip.ZipEntry entry = zipEntry();

            if (entry instanceof ZipEntry && entry.getMethod() == DEFLATED && ((ZipEntry) entry).isAlreadyCompressed()) {

                long localDataStart;

                // Handle either Ant 1.8 or Ant 1.9 inner structure
                final Object tmpEntry = entryField.get(this);
                if (localDataStartField != null) {
                    // Ant 1.8
                    localDataStart = localDataStartField.getLong(this);
                } else {
                    // Ant 1.9
                    final Field currentEntryLocalDataStartField = tmpEntry.getClass()
                            .getDeclaredField("localDataStart");
                    currentEntryLocalDataStartField.setAccessible(true);
                    localDataStart = currentEntryLocalDataStartField.getLong(tmpEntry);
                }

                // Save the current file position and seek to the entry header
                final long save = raf.getFilePointer();
                raf.seek(localDataStart);

                // Write out the CRC, etc.
                writeOut(ZipLong.getBytes(entry.getCrc()));
                writeOut(ZipLong.getBytes(entry.getCompressedSize()));
                writeOut(ZipLong.getBytes(entry.getSize()));

                // Restore the current position
                raf.seek(save);

                // Null out the entry field so we won't attempt to re-write it later in
                // closeEntry...
                entryField.set(this, null);
            } else {
                super.closeEntry();
            }
        } catch (final IOException e) {
            throw e;
        } catch (final Exception e) {
            throw new BuildException(e);
        }
    }

    private org.apache.tools.zip.ZipEntry zipEntry() throws NoSuchFieldException, SecurityException,
            IllegalArgumentException, IllegalAccessException {
        // Handle either Ant 1.8 or Ant 1.9 inner structure
        final Object tmpEntry = entryField.get(this);
        if (localDataStartField != null) {
            // Ant 1.8
            return (org.apache.tools.zip.ZipEntry) tmpEntry;
        }

        // Ant 1.9
        final Field currentEntryEntryField = tmpEntry.getClass().getDeclaredField("entry");
        currentEntryEntryField.setAccessible(true);
        return (org.apache.tools.zip.ZipEntry) currentEntryEntryField.get(tmpEntry);
    }
}