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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import xeus.jcl.JarClassLoader;

/**
 * JUnit tests for {@link GenJar}
 */
public class TestGenJar extends GenJarTestCase {

    @Before
    @Override
    protected void setUp() throws Exception {
        configureProject("test/build.xml");
    }

    @After
    @Override
    protected void tearDown() throws Exception {
        executeTarget("clean");
    }

    @Test
    public void testBuildExceptions() {
        expectSpecificBuildException("test.ex.1", "some cause", "GenJar: destfile attribute is required");
        expectSpecificBuildException("test.ex.2", "some cause",
                "fileset doesn't support the nested \"fileset\" element.");
        expectSpecificBuildException("test.ex.3", "some cause", "Cannot set both dir and src attributes");
    }

    @Test
    public void testClass1() throws IOException {
        executeTarget("test.class.1");
        checkJarfileContents("CL1");
    }

    @Test
    public void testClass2() throws IOException {
        executeTarget("test.class.2");
        checkJarfileContents("CL2");
    }

    @Test
    public void testClass3() throws IOException {
        executeTarget("test.class.3");
        checkJarfileContents("CL3");
    }

    @Test
    public void testClass4() throws IOException {
        executeTarget("test.class.4");
        checkJarfileContents("CL4");
    }

    @Test
    public void testClassFilter1() throws IOException {
        executeTarget("test.classfilter.1");
        checkJarfileContents("CF1");
    }

    @Test
    public void testClassFilter2() throws IOException {
        executeTarget("test.classfilter.2");
        checkJarfileContents("CF2");
    }

    @Test
    public void testFileSet1() throws IOException {
        executeTarget("test.fileset.1");
        checkJarfileContents("FS1");
    }

    @Test
    public void testFileSet2() throws IOException {
        executeTarget("test.fileset.2");
        checkJarfileContents("FS2");
    }

    @Test
    public void testZipfileset1() throws IOException {
        executeTarget("test.zipfileset.1");
        checkJarfileContents("Z1");
    }

    @Test
    public void testZipfileset2() throws IOException {
        executeTarget("test.zipfileset.2");
        checkJarfileContents("Z2");
    }

    @Test
    public void testZipfileset3() throws IOException {
        executeTarget("test.zipfileset.3");
        checkJarfileContents("Z3");
    }

    @Test
    public void testZipfileset4() throws Exception {
        executeTarget("test.zipfileset.4");
        checkJarfileContents("Z4");

        // Verify that we can load a class that should be in the packaged jar
        final JarClassLoader jcl = new JarClassLoader();

        jcl.add(JAR_DIR);
        try {
            jcl.loadClass("package4.jar.Class8_1");
        } catch (final Throwable t) {
            fail("Unable to load class package4.jar.Class8_1: " + t.getMessage());
        }
    }

    @Test
    public void testZipgroupfileset1() throws IOException {
        executeTarget("test.zipgroupfileset.1");
        checkJarfileContents("ZG1");
    }

    @Test
    public void testZipgroupfileset2() throws IOException {
        executeTarget("test.zipgroupfileset.2");
        checkJarfileContents("ZG2");
    }

    @Test
    public void testManifest1() throws IOException {
        executeTarget("test.manifest.1");
        checkJarManifest("test/etc/MANIFEST.MF1");
    }

    @Test
    public void testManifest2() throws IOException {
        executeTarget("test.manifest.2");
        checkJarManifest("test/etc/MANIFEST.MF2");
    }

    private void checkJarManifest(final String staticManifestFile) throws IOException, FileNotFoundException {
        final JarFile jarFile = new JarFile(JAR_FILE);
        final String jarManifest = readInputStream(jarFile.getInputStream(jarFile.getJarEntry("META-INF/MANIFEST.MF")));
        jarFile.close();
        final String staticManifest = readInputStream(new FileInputStream(staticManifestFile));
        assertEquals(staticManifest, cleanJarManifest(jarManifest));
    }

    private String cleanJarManifest(String manifest) {
        final Pattern p1 = Pattern.compile("^Created-By: 1.*$", Pattern.MULTILINE);
        manifest = p1.matcher(manifest).replaceFirst("Created-By: JVM");

        final Pattern p2 = Pattern.compile("^Ant-Version: .*$", Pattern.MULTILINE);
        manifest = p2.matcher(manifest).replaceFirst("Ant-Version: ANT");

        return manifest;
    }

    @Test
    public void testUpperCaseClasspathEntry() throws IOException {
        executeTarget("test.classpath.1");
        checkJarfileContents("CP1");
    }

    @Test
    public void testFileLocking1() {
        executeTarget("test.locking.1");
    }

    @Test
    public void testGenjarJar() {
        executeTarget("test.genjar.jar");
    }

    @Test
    public void testProfile() {
        final long startTime = System.currentTimeMillis();
        executeTarget("test.profile");
        System.out.println("Profiled in " + (System.currentTimeMillis() - startTime) + " ms");
    }

    // Utility methods

    private String readInputStream(final InputStream is) throws IOException {
        final StringBuilder sb = new StringBuilder(1024);
        final BufferedReader br = new BufferedReader(new InputStreamReader(is));
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            // Convert to the path separators, so the tests will run on Windows as well
            sb.append(line);
            sb.append('\n');
        }
        br.close();
        return sb.toString();
    }
}
