/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package net.sf.genjar.zip;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.zip.ZipException;

/**
 * Extends {@link java.util.zip.ZipFile}, making use of {@link ZipEntry} flag for already-compressed entries. Intended
 * to avoid the expense of decompression and recompression when copying a file from one archive to another.
 * 
 * Warning, this involves accessing private fields from {@link org.apache.tools.zip.ZipOutputStream}, so it's somewhat
 * fragile. But it does improve build performance considerably.
 */
public class ZipFile extends java.util.zip.ZipFile {
    private final static Object[] EMPTY_ARGS = new Object[0];

    private Constructor<?> zipFileInputStreamConstructor;
    private Field jzfileField;
    private Method ensureOpenMethod;
    private Method getEntryMethod16;
    private Method getEntryMethod17;

    public ZipFile(final File file) throws ZipException, IOException {
        super(file);
        init();
    }

    private void init() {
        if (zipFileInputStreamConstructor != null) {
            return;
        }

        try {
            final Class<?> cl = Class.forName("java.util.zip.ZipFile$ZipFileInputStream");
            zipFileInputStreamConstructor = cl.getDeclaredConstructor(java.util.zip.ZipFile.class, long.class);
            zipFileInputStreamConstructor.setAccessible(true);

            jzfileField = java.util.zip.ZipFile.class.getDeclaredField("jzfile");
            jzfileField.setAccessible(true);

            ensureOpenMethod = java.util.zip.ZipFile.class.getDeclaredMethod("ensureOpen", new Class<?>[0]);
            ensureOpenMethod.setAccessible(true);

            try {
                getEntryMethod16 = java.util.zip.ZipFile.class.getDeclaredMethod("getEntry", new Class<?>[] {
                        long.class, String.class, boolean.class });
                getEntryMethod16.setAccessible(true);
            } catch (final NoSuchMethodException e) {
                getEntryMethod17 = java.util.zip.ZipFile.class.getDeclaredMethod("getEntry", new Class<?>[] {
                        long.class, byte[].class, boolean.class });
                getEntryMethod17.setAccessible(true);
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Returns an InputStream for reading the contents of the given entry.
     * 
     * @param ze the entry to get the stream for.
     * @return a stream to read the entry from.
     * @throws IOException if unable to create an input stream from the zipentry
     * @throws ZipException if the zipentry has an unsupported compression method
     */
    @Override
    public InputStream getInputStream(final java.util.zip.ZipEntry ze) throws IOException, ZipException {
        if (ze instanceof ZipEntry) {
            final ZipEntry ze2 = (ZipEntry) ze;
            if (ze2.isAlreadyCompressed()) {
                final InputStream is = getCompressedInputStream(ze.getName());
                return is;
            }

            return super.getInputStream(ze);
        }

        return super.getInputStream(ze);
    }

    /**
     * Returns an input stream for reading the contents of the specified entry, or null if the entry was not found.
     */
    private InputStream getCompressedInputStream(final String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        long jzentry = 0;
        synchronized (this) {
            try {
                ensureOpenMethod.invoke(this, EMPTY_ARGS);
                final long l = jzfileField.getLong(this);
                if (getEntryMethod16 != null) {
                    jzentry = (Long) getEntryMethod16.invoke(this, new Object[] { l, name, false });
                } else {
                    jzentry = (Long) getEntryMethod17.invoke(this, new Object[] { l, name.getBytes(), false });
                }
                if (jzentry == 0) {
                    return null;
                }
                return (InputStream) zipFileInputStreamConstructor.newInstance(new Object[] { this, jzentry });
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
