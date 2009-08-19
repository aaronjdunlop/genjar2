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

import java.io.IOException;
import java.util.HashSet;

import net.sf.genjar.zip.ZipEntry;

import org.apache.tools.ant.types.FileSet;

/**
 * A PathResolver is used for each component of the classpath.</p>
 * <p>
 *
 * Each type of component is a specialization of this base class. For example, a jar file
 * encountered in the classpath causes a JarResolver to be instantiated. A JarResolver knows how to
 * search within jar files for specific files.
 * </p>
 * <p>
 *
 * This approach is taken for two reasons:
 * <ol>
 * <li>To encapsulate the specifics of fetching streams and what attributes are set on a jar entry's
 * manifest entry.</li>
 * <li>To provide an association between the <i>source</i> of a class (or resource) and the
 * repository from which it came. This info is priceless when trying to figure out why the wrong
 * classes are being included in your jar.</li>
 *
 * </ol>
 * </p>
 *
 * @author Original Code: <a href="mailto:jake@riggshill.com">John W. Kohler </a>
 * @author Jesse Stockall
 * @created February 23, 2003
 * @version $Revision: 1.3 $ $Date: 2003/02/23 18:25:23 $
 */
abstract class BaseResolver
{
    /** Null if no patterns are set on the fileset */
    protected HashSet<String> includedFiles;

    /**
     * Constructor for the PathResolver object
     *
     * @param log Description of the Parameter
     */
    protected BaseResolver(FileSet fileset)
    {
        if (fileset.hasPatterns())
        {
            includedFiles = new HashSet<String>();
            for (final String filename : fileset.getDirectoryScanner(fileset.getProject()).getIncludedFiles())
            {
                includedFiles.add(filename.replaceAll("\\\\", "/"));
            }
        }
    }

    /**
     * Given a JarEntrySpec, a path to the actual resource is generated and an input stream is
     * returned on the path.
     *
     * @param jarEntry entry (class or resource) to be resolved
     * @return IOStream opened on the file referenced
     * @exception IOException if any errors are encountered
     */
    protected abstract ZipEntry resolve(String jarEntry) throws IOException;

    /**
     * Closes any resources opened by the resolver.
     */
    protected abstract void close();
}
