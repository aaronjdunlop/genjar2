package net.sf.genjar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.tools.ant.BuildFileTest;

public class GenJarTestCase extends BuildFileTest {

    protected final static String JAR_DIR = "test/build";
    protected final static String JAR_FILE = JAR_DIR + "/genjar2-test-tmp.jar";
    private final static String STATIC_LIST_DIR = "test/etc";


    protected void checkJarfileContents(String prefix) throws IOException
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
        jarFile.close();

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
