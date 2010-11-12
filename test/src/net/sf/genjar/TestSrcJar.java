package net.sf.genjar;

import java.io.IOException;

import org.apache.tools.ant.Project;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link SrcJar}
 * 
 * @author Aaron Dunlop
 * 
 */
public class TestSrcJar extends GenJarTestCase {

    @Before
    @Override
    protected void setUp() throws Exception {
        project = new Project();
        project.setBasedir(".");
        configureProject("test/build.xml");
    }

    @After
    @Override
    protected void tearDown() throws Exception {
        executeTarget("clean");
    }

    @Test
    public void testClass1() throws IOException {
        executeTarget("test.srcjar.1");
        checkJarfileContents("SL1");
    }
}
