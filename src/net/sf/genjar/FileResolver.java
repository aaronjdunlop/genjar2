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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.tools.ant.types.FileSet;

/**
 * Represents a directory in the classpath.
 * <p>
 *
 * When a directory is located in the classpath, a FileResolver is instantiated that encapsulates
 * the path and performs searches in that directory. This class is used primarily to allow easy
 * association of the <i>source directory </i> with the jar entry's attributes.
 * <p>
 *
 * When a file is resolved from a JarEntrySpec, Attributes are added for the source file's path and
 * last modification time.
 * <p>
 *
 *
 *
 * @author Original Code: <a href="mailto:jake@riggshill.com">John W. Kohler </a>
 * @author Jesse Stockall
 * @created February 23, 2003
 * @version $Revision: 1.3 $ $Date: 2003/02/23 18:25:23 $
 */
class FileResolver extends BaseResolver
{
    private final File baseDir;

    /**
     * constructs a new FileResolver using the given <i>base directory</i>
     *
     * @param base a directory at which file searches begin
     * @param log an ant logging mechanism
     */
    FileResolver(FileSet fileset)
    {
        super(fileset);
        this.baseDir = fileset.getDir();
    }

    /**
     * Nothing to close
     */
    @Override
    public void close()
    {}

    /**
     * Resolve the file specified in a JarEntrySpec to a stream.
     *
     * @param jarEntry the JarEntrySpec to resolve
     * @return an InputStream open on the resolved file or null
     * @exception IOException if opening the stream fails
     */
    @Override
    public InputStream resolve(final String jarEntry) throws IOException
    {
        if (includedFiles != null && !includedFiles.contains(jarEntry))
        {
            return null;
        }
        final File f = new File(baseDir, jarEntry);
        if (!f.exists())
        {
            return null;
        }
        return new FileInputStream(f);
    }
}
