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

import java.io.InputStream;
import java.util.zip.ZipException;

/**
 * Extends {@link org.apache.tools.zip.ZipEntry}, adding a flag for entries which are already
 * compressed (generally in another ZIP file).
 *
 */
public class ZipEntry extends org.apache.tools.zip.ZipEntry
{
    private boolean alreadyCompressed = false;
    private java.util.zip.ZipEntry createdFrom;
    private InputStream inputStream;

    public ZipEntry(String name, InputStream is, boolean alreadyCompressed)
    {
        super(name);
        this.inputStream = is;
        this.alreadyCompressed = alreadyCompressed;
    }

    public ZipEntry(java.util.zip.ZipEntry entry, boolean alreadyCompressed) throws ZipException
    {
        super(entry);
        this.createdFrom = entry;
        this.alreadyCompressed = alreadyCompressed;
//
//        setSize(entry.getSize());
//        setCrc(entry.getCrc());
    }

    public boolean isAlreadyCompressed()
    {
        return alreadyCompressed;
    }

    public void setAlreadyCompressed(boolean alreadyCompressed)
    {
        this.alreadyCompressed = alreadyCompressed;
    }

    public java.util.zip.ZipEntry getCreatedFrom()
    {
        return createdFrom;
    }

    public InputStream getInputStream()
    {
        return inputStream;
    }

    public void setInputStream(InputStream is)
    {
        this.inputStream = is;
    }

    @Override
    public boolean equals(Object o)
    {
        return (o == this || o == createdFrom);
    }
}
