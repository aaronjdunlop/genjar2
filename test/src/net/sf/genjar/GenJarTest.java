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
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.Project;

/**
 * Description of the Class
 *
 * @author   <a href="mailto:jesse_dev@yahoo.com">Jesse</a>
 */
public class GenJarTest extends BuildFileTest
{

    private Project project;

    /**
     * Constructor for the SOSTest object
     *
     * @param s  Test name
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
    protected void setUp()
        throws Exception
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
    protected void tearDown()
        throws Exception
    {
        executeTarget("clean");
    }

    /** Tests */
    public void testBuildExceptions()
    {
        expectSpecificBuildException("test.ex.1", "some cause", "GenJar: Either a destfile or destdir attribute is required");
        expectSpecificBuildException("test.ex.2", "some cause", "fileset doesn't support the nested \"fileset\" element.");
        expectBuildException("test.ex.3", "can't add Fileset - file already set");
        expectSpecificBuildException("test.ex.4", "some cause", "Cannot set both dir and src attributes");
    }

    /** Tests */
    public void testResources1()
    {
        executeTarget("test.resource.1");
        ArrayList<String> genList = getFileList(new File("test/build/R1"));
        ArrayList<String> staticList = readStaticTestFile(new File("test/etc/R1.filelist"));

        assertEquals("Different number of files", staticList.size(), genList.size());

        assertTrue("Incorrect file in jar", genList.containsAll(staticList));
    }

    /** Tests */
    public void testResources2()
    {
        executeTarget("test.resource.2");
        ArrayList<String> genList = getFileList(new File("test/build/R2"));
        ArrayList<String> staticList = readStaticTestFile(new File("test/etc/R2.filelist"));

        assertEquals("Different number of files", staticList.size(), genList.size());

        assertTrue("Incorrect file in jar", genList.containsAll(staticList));
    }

    public void testFileSet1() {
        executeTarget("test.fileset.1");
        ArrayList<String> genList = getFileList(new File("test/build/F1"));
        ArrayList<String> staticList = readStaticTestFile(new File("test/etc/F1.filelist"));

        assertEquals("Different number of files", staticList.size(), genList.size());

        assertTrue("Incorrect file in jar", genList.containsAll(staticList));
    }

    /** Tests */
    public void testClass1()
    {
        executeTarget("test.class.1");
        ArrayList<String> genList = getFileList(new File("test/build/CL1"));
        ArrayList<String> staticList = readStaticTestFile(new File("test/etc/CL1.filelist"));

        assertEquals("Different number of files", staticList.size(), genList.size());

        assertTrue("Incorrect file in jar", genList.containsAll(staticList));
    }

    /** Tests */
    public void testClass2()
    {
        executeTarget("test.class.2");
        File dir = new File("test/build/CL2");
        ArrayList<String> genList = getFileList(dir);
        ArrayList<String> staticList = readStaticTestFile(new File("test/etc/CL2.filelist"));

        assertEquals("Different number of files", staticList.size(), genList.size());

        assertTrue("Incorrect file in jar", genList.containsAll(staticList));
    }

    /** Tests class dependency building with a fileset in <class> */
    public void testClass3()
    {
        executeTarget("test.class.3");
        File dir = new File("test/build/CL3");
        ArrayList<String> genList = getFileList(dir);
        ArrayList<String> staticList = readStaticTestFile(new File("test/etc/CL3.filelist"));

//        for (Iterator it = genList.iterator(); it.hasNext();) {
//            System.out.println("gen listing: " + it.next());
//        }
//        for (Iterator it = staticList.iterator(); it.hasNext();) {
//            System.out.println("static listing: " + it.next());
//        }

        assertEquals("Different number of files", staticList.size(), genList.size());

        assertTrue("Incorrect file in jar", genList.containsAll(staticList));
    }

    /** Tests class dependency building with inner classes */
    public void testClass4()
    {
        executeTarget("test.class.4");
        File dir = new File("test/build/CL4");
        ArrayList<String> genList = getFileList(dir);
        ArrayList<String> staticList = readStaticTestFile(new File("test/etc/CL4.filelist"));
//
//        for (Iterator it = genList.iterator(); it.hasNext();) {
//            System.out.println("gen listing: " + it.next());
//        }
//        for (Iterator it = staticList.iterator(); it.hasNext();) {
//            System.out.println("static listing: " + it.next());
//        }

        assertEquals("Different number of files", staticList.size(), genList.size());

        assertTrue("Incorrect file in jar", genList.containsAll(staticList));
    }

    /** Tests class dependency building with inner classes in a jar */
    public void testClass5()
    {
        executeTarget("test.class.5");
        File dir = new File("test/build/CL5");
        ArrayList<String> genList = getFileList(dir);
        ArrayList<String> staticList = readStaticTestFile(new File("test/etc/CL5.filelist"));
//
//        for (Iterator it = genList.iterator(); it.hasNext();) {
//            System.out.println("gen listing: " + it.next());
//        }
//        for (Iterator it = staticList.iterator(); it.hasNext();) {
//            System.out.println("static listing: " + it.next());
//        }

        assertEquals("Different number of files", staticList.size(), genList.size());

        assertTrue("Incorrect file in jar", genList.containsAll(staticList));
    }

    /** Tests */
    public void testClassFilter1()
    {
        executeTarget("test.classfilter.1");
        ArrayList<String> genList = getFileList(new File("test/build/CF1"));
        ArrayList<String> staticList = readStaticTestFile(new File("test/etc/CF1.filelist"));

        assertEquals("Different number of files", staticList.size(), genList.size());

        assertTrue("Incorrect file in jar", genList.containsAll(staticList));
    }

    /** Tests */
    public void testClassFilter2()
    {
        executeTarget("test.classfilter.2");
        ArrayList<String> genList = getFileList(new File("test/build/CF2"));

        ArrayList<String> staticList = readStaticTestFile(new File("test/etc/CF2.filelist"));

        assertEquals("Different number of files", staticList.size(), genList.size());

        assertTrue("Incorrect file in jar", genList.containsAll(staticList));
    }

    /** Tests */
    public void testLibrary1()
    {
        executeTarget("test.library.1");
        ArrayList<String> genList = getFileList(new File("test/build/L1"));
        ArrayList<String> staticList = readStaticTestFile(new File("test/etc/L1.filelist"));

        assertEquals("Different number of files", staticList.size(), genList.size());

        assertTrue("Incorrect file in jar", genList.containsAll(staticList));
    }

    /** Tests */
    public void testLibrary2()
    {
        executeTarget("test.library.2");
        ArrayList<String> genList = getFileList(new File("test/build/L2"));
        ArrayList<String> staticList = readStaticTestFile(new File("test/etc/L2.filelist"));

//        for (Iterator it = genList.iterator(); it.hasNext();) {
//            System.out.println("gen listing: " + it.next());
//        }
//        for (Iterator it = staticList.iterator(); it.hasNext();) {
//            System.out.println("static listing: " + it.next());
//        }

        assertEquals("Different number of files", staticList.size(), genList.size());

        assertTrue("Incorrect file in jar", genList.containsAll(staticList));

        ArrayList<String> staticIndexList = readStaticTestFile(new File("test/etc/INDEX.LIST"));
        ArrayList<String> jarIndexList = readStaticTestFile(new File("test/build/L2/META-INF/INDEX.LIST"));

        assertEquals("Different number of entries in manifest", staticIndexList.size(), jarIndexList.size());

        assertTrue("Incorrect entry in manifest", jarIndexList.containsAll(staticIndexList));
    }

    /** Tests */
    public void testLibrary3()
    {
        executeTarget("test.library.3");
        ArrayList<String> genList = getFileList(new File("test/build/L3"));
        ArrayList<String> staticList = readStaticTestFile(new File("test/etc/L3.filelist"));

//        for (Iterator it = genList.iterator(); it.hasNext();) {
//            System.out.println("gen listing: " + it.next());
//        }
//        for (Iterator it = staticList.iterator(); it.hasNext();) {
//            System.out.println("static listing: " + it.next());
//        }

        assertEquals("Different number of files", staticList.size(), genList.size());
        assertTrue("Incorrect file in jar", genList.containsAll(staticList));
    }

    public void testZipgroupfilesetToDir()
    {
        executeTarget("test.zipfileset.4");
        ArrayList<String> genList = getFileList(new File("test/build/Z4"));
        ArrayList<String> staticList = readStaticTestFile(new File("test/etc/Z4.filelist"));

        assertEquals("Different number of files", staticList.size(), genList.size());
        assertTrue("Incorrect file in jar", genList.containsAll(staticList));
    }

    public void testZipgroupfilesetToJar()
    {
        executeTarget("test.zipfileset.5");
        ArrayList<String> genList = getFileList(new File("test/build/Z5"));
        ArrayList<String> staticList = readStaticTestFile(new File("test/etc/Z5.filelist"));

        assertEquals("Different number of files", staticList.size(), genList.size());
        assertTrue("Incorrect file in jar", genList.containsAll(staticList));
    }

//    public void testManifest1() {
//        executeTarget("test.manifest.1");
//        ArrayList manifestList = readStaticTestFile(new File("test/etc/MANIFEST.MF"));
//        ArrayList jarManifest = readStaticTestFile(new File("test/build/M1/META-INF/MANIFEST.MF"));
//
//        for (Iterator it = manifestList.iterator(); it.hasNext();) {
//            System.out.println("static: " + it.next());
//        }
//
//        for (Iterator it = jarManifest.iterator(); it.hasNext();) {
//            System.out.println("jar: " + it.next());
//        }
//
//        assertEquals("Different number of entries in manifest", manifestList.size(), jarManifest.size());
//
//        assertTrue("Incorrect entry in manifest", jarManifest.containsAll(manifestList));
//    }
//
//    public void testManifest2() {
//        executeTarget("test.manifest.2");
//        ArrayList manifestList = readStaticTestFile(new File("test/etc/MANIFEST.MF2"));
//        ArrayList jarManifest = readStaticTestFile(new File("test/build/M2/META-INF/MANIFEST.MF"));
//
//        assertEquals("Different number of entries in manifest", manifestList.size(), jarManifest.size());
//
//        assertTrue("Incorrect entry in manifest", jarManifest.containsAll(manifestList));
//    }

    // We don't have the appropriate key to sign jars, and don't really care...
//    /** Tests */
//    public void testSignJar()
//    {
//        expectLogContaining("test.sign.jar", "jar verified.");
//    }

    /** Tests */
    public void testUpperCaseClasspathEntry()
    {
        executeTarget("test.classpath.1");
        File dir = new File("test/build/CP1");
        ArrayList<String> genList = getFileList(dir);
        ArrayList<String> staticList = readStaticTestFile(new File("test/etc/CP1.filelist"));

        assertEquals("Different number of files", staticList.size(), genList.size());

        assertTrue("Incorrect file in jar", genList.containsAll(staticList));
    }

    /** Tests */
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

    private ArrayList<String> readStaticTestFile(File file)
    {
        ArrayList<String> al = new ArrayList<String>();
        if (! file.exists())
        {
            fail(file.getPath() + "does not exist");
        }
        if (! file.isFile())
        {
            fail(file.getPath() + "is not a file");
        }
        try
        {
            FileInputStream fin = new FileInputStream(file);

            BufferedReader myInput = new BufferedReader(new InputStreamReader(fin));
            String line;
            while ((line = myInput.readLine()) != null)
            {
                // Convert to the path seperators, so the tests will run n Windows as well
                al.add(line.replace('/', File.separatorChar));
            }
        }
        catch (IOException ioe)
        {
            fail("Error reading file list: " + file.getPath());
        }
        return al;
    }

    private ArrayList<String> getFileList(File dir)
    {
        ArrayList<String> al = new ArrayList<String>();
        if (! dir.exists())
        {
            fail(dir.getPath() + "does not exist");
        }
        if (! dir.isDirectory())
        {
            fail(dir.getPath() + "is not a directory");
        }

        File[] files = dir.listFiles();
        for (int i = 0, length = files.length; i < length; i++)
        {
            if (files[i].isFile())
            {
                al.add(files[i].getPath());
                continue;
            }
            if (files[i].isDirectory())
            {
                al.addAll(getFileList(files[i]));
            }
        }
        return al;
    }
}
// vi:set ts=4 sw=4:
