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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.Project;

/**
 * Description of the Class
 *
 * @author <a href="mailto:jesse_dev@yahoo.com">Jesse</a>
 */
public class GenJarTest extends BuildFileTest
{

    private Project project;

    private final static String JAR_FILE = "test/build/genjar2-test-tmp.jar";
    private final static String STATIC_LIST_DIR = "test/etc";

    /**
     * Constructor for the SOSTest object
     *
     * @param s Test name
     */
    public GenJarTest(String s)
    {
        super(s);
    }

    /**
     * The JUnit setup method
     *
     * @throws Exception
     */
    @Override
    protected void setUp() throws Exception
    {
        project = new Project();
        project.setBasedir(".");
        configureProject("test/build.xml");
    }

    /**
     * The teardown method for JUnit
     *
     * @throws Exception
     */
    @Override
    protected void tearDown() throws Exception
    {
        executeTarget("clean");
    }

    /** Tests */
    public void testBuildExceptions()
    {
        expectSpecificBuildException("test.ex.1", "some cause", "GenJar: destfile attribute is required");
        expectSpecificBuildException("test.ex.2", "some cause",
            "fileset doesn't support the nested \"fileset\" element.");
        expectSpecificBuildException("test.ex.3", "some cause", "Cannot set both dir and src attributes");
    }

    /**
     * Tests
     *
     * @throws IOException
     */
    public void testClass1() throws IOException
    {
        executeTarget("test.class.1");
        checkJarfileContents("CL1");
    }

    /**
     * Tests
     *
     * @throws IOException
     */
    public void testClass2() throws IOException
    {
        executeTarget("test.class.2");
        checkJarfileContents("CL2");
    }

    /**
     * Tests class dependency building with a fileset in <class>
     *
     * @throws IOException
     */
    public void testClass3() throws IOException
    {
        executeTarget("test.class.3");
        checkJarfileContents("CL3");
    }

    /**
     * Tests class dependency building with inner classes
     *
     * @throws IOException
     */
    public void testClass4() throws IOException
    {
        executeTarget("test.class.4");
        checkJarfileContents("CL4");
    }

    /**
     * Tests
     *
     * @throws IOException
     */
    public void testClassFilter1() throws IOException
    {
        executeTarget("test.classfilter.1");
        checkJarfileContents("CF1");
    }

    /**
     * Tests
     *
     * @throws IOException
     */
    public void testClassFilter2() throws IOException
    {
        executeTarget("test.classfilter.2");
        checkJarfileContents("CF2");
    }

    public void testFileSet1() throws IOException
    {
        executeTarget("test.fileset.1");
        checkJarfileContents("FS1");
    }

    public void testFileSet2() throws IOException
    {
        executeTarget("test.fileset.2");
        checkJarfileContents("FS2");
    }

    /**
     * Tests including multiple zipfilesets with duplicate entries
     *
     * @throws IOException
     */
    public void testZipfileset1() throws IOException
    {
        executeTarget("test.zipfileset.1");
        checkJarfileContents("Z1");
    }

    /**
     * Tests the zipfileset 'includes' attribute
     *
     * @throws IOException
     */
    public void testZipfileset2() throws IOException
    {
        executeTarget("test.zipfileset.2");
        checkJarfileContents("Z2");
    }

    /**
     * Tests the zipfileset 'excludes' attribute
     *
     * @throws IOException
     */
    public void testZipfileset3() throws IOException
    {
        executeTarget("test.zipfileset.3");
        checkJarfileContents("Z3");
    }

    /**
     * Tests including a required jar using zipfileset. If we're including the entire jar, the build
     * should succeed even if the jar isn't listed explicitly in the classpath
     *
     * @throws IOException
     */
    public void testZipfileset4() throws IOException
    {
        executeTarget("test.zipfileset.4");
        checkJarfileContents("Z4");
    }

    /**
     * Tests excluding a required class (even though it's in a jar included via zipfileset). We
     * expect this build to fail.
     *
     * @throws IOException
     */
    public void testZipfileset5() throws IOException
    {
        expectSpecificBuildException("test.zipfileset.5", "Required file excluded",
            "Jar component not found (package4/jar/Class8_1.class)");
    }

    public void testZipgroupfileset1() throws IOException
    {
        executeTarget("test.zipgroupfileset.1");
        checkJarfileContents("ZG1");
    }

    public void testZipgroupfileset2() throws IOException
    {
        executeTarget("test.zipgroupfileset.2");
        checkJarfileContents("ZG2");
    }

    public void testManifest1() throws IOException
    {
        executeTarget("test.manifest.1");
        checkJarManifest("test/etc/MANIFEST.MF1");
    }

    public void testManifest2() throws IOException
    {
        executeTarget("test.manifest.2");
        checkJarManifest("test/etc/MANIFEST.MF2");
    }

    private void checkJarManifest(String staticManifestFile) throws IOException, FileNotFoundException
    {
        JarFile jarFile = new JarFile(JAR_FILE);
        String jarManifest = readInputStream(jarFile.getInputStream(jarFile.getJarEntry("META-INF/MANIFEST.MF")));
        jarFile.close();
        String staticManifest = readInputStream(new FileInputStream(staticManifestFile));

        assertEquals(staticManifest, jarManifest);
    }

    // We don't have the appropriate key to sign jars, and don't really care...
    // /** Tests */
    // public void testSignJar()
    // {
    // expectLogContaining("test.sign.jar", "jar verified.");
    // }

    /**
     * Tests the classpath resolvers, including specifying a jar file with an upper-case suffix
     *
     * @throws IOException
     */
    public void testUpperCaseClasspathEntry() throws IOException
    {
        executeTarget("test.classpath.1");
        checkJarfileContents("CP1");
    }

    /** Tests file locking */
    public void testFileLocking1()
    {
        executeTarget("test.locking.1");
    }

    /** Profiles execution speed */
    public void testProfile()
    {
        long startTime = System.currentTimeMillis();
        executeTarget("test.profile");
        System.out.println("Profiled in " + (System.currentTimeMillis() - startTime) + " ms");
    }

    // Utility methods

    private String readInputStream(InputStream is) throws IOException
    {
        StringBuilder sb = new StringBuilder(1024);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        for (String line = br.readLine(); line != null; line = br.readLine())
        {
            // Convert to the path separators, so the tests will run on Windows as well
            sb.append(line);
            sb.append('\n');
        }
        br.close();
        return sb.toString();
    }

    private void checkJarfileContents(String prefix) throws IOException
    {
        // Check the jar file contents
        Set<String> staticFileList = readFilenames(new FileInputStream(STATIC_LIST_DIR + "/" + prefix + ".filelist"));

        HashSet<String> jarFileList = new HashSet<String>();
        Set<String> jarIndexList = null;
        JarFile jarFile = new JarFile(JAR_FILE);
        for (Enumeration<JarEntry> e = jarFile.entries(); e.hasMoreElements();)
        {
            JarEntry jarEntry = e.nextElement();
            final String name = jarEntry.getName();
            jarFileList.add(name);

            if (name.equals("META-INF/INDEX.LIST"))
            {
                jarIndexList = readFilenames(jarFile.getInputStream(jarEntry));
            }
        }

        // Check the jar file contents
        staticFileList.removeAll(jarFileList);
        if (staticFileList.size() > 0)
        {
            fail("Jar file missing files: " + staticFileList.toString());
        }

        // Check the index, if present
        if (jarIndexList != null)
        {
            File staticIndexFile = new File(STATIC_LIST_DIR + "/" + prefix + ".index");
            if (staticIndexFile.exists())
            {
                Set<String> staticIndexList = readFilenames(new FileInputStream(staticIndexFile));
                assertEquals("Different number of entries in manifest", staticIndexList.size(), jarIndexList.size());
                assertTrue("Incorrect entry in manifest", jarIndexList.containsAll(staticIndexList));
            }
        }
    }

    private Set<String> readFilenames(InputStream is) throws IOException
    {
        Set<String> lines = new HashSet<String>();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        for (String line = br.readLine(); line != null; line = br.readLine())
        {
            // Convert to the path separators, so the tests will run on Windows as well
            lines.add(line.replace('\\', '/'));
        }
        br.close();
        return lines;
    }
}
