package cltool;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

/**
 * Provides commonly-used functionality for testing command-line tools.
 *
 * @author aarond
 * @since Oct 21, 2009
 *
 * @version $Revision$
 */
public abstract class ToolTestCase
{
    protected final static String UNIT_TEST_DIR = "unit-test-data/";

    /**
     * Executes the tool with the given arguments, returning the tool output as a String. Output
     * combines STDOUT and STDERR into a single String.
     *
     * @param tool Tool to be tested
     * @param args Command-line
     * @param input Standard Input
     * @return Tool output (STDOUT and STDERR)
     * @throws Exception if something bad happens
     */
    protected String executeTool(BaseCommandlineTool tool, String args, String input) throws Exception
    {
        return executeTool(tool, args, new ByteArrayInputStream(input.getBytes()));
    }

    /**
     * Executes the tool with the given arguments, returning the tool output as a String. Output
     * combines STDOUT and STDERR into a single String.
     *
     * @param tool Tool to be tested
     * @param args Command-line
     * @param inputFilename File from unit-test-data directory to use as tool input.
     * @return Tool output (STDOUT and STDERR)
     * @throws Exception if something bad happens
     */
    protected String executeToolFromFile(BaseCommandlineTool tool, String args, String inputFilename) throws Exception
    {
        return executeTool(tool, args, BaseCommandlineTool.fileAsInputStream(UNIT_TEST_DIR + inputFilename));
    }

    /**
     * Executes the tool with the given arguments, using the specified InputStream as input. Output
     * combines STDOUT and STDERR into a single String.
     *
     * @param tool Tool to be tested
     * @param args Command-line
     * @param inputFilename File from unit-test-data directory to use as tool input.
     * @return Tool output (STDOUT and STDERR)
     * @throws Exception if something bad happens
     */
    protected String executeTool(BaseCommandlineTool tool, String args, InputStream input) throws Exception
    {
        // Store STDIN, STDOUT, and STDERR so we can restore them after the test run
        InputStream systemIn = System.in;
        PrintStream systemOut = System.out;
        PrintStream systemErr = System.err;

        ByteArrayOutputStream bos = new ByteArrayOutputStream(8192);
        try
        {
            // Redirect STDIN, STDOUT, and STDERR for testing
            System.setIn(input);
            PrintStream ps = new PrintStream(bos);
            System.setOut(ps);
            System.setErr(ps);

            // Execute the tool
            String[] argArray = args.length() == 0 ? new String[0] : args.split(" ");
            BaseCommandlineTool.run(tool, argArray);
        }
        finally
        {
            // Restore STDIN, STDOUT, STDERR
            System.setIn(systemIn);
            System.setOut(systemOut);
            System.setErr(systemErr);
        }
        String output = new String(bos.toByteArray());

        // Just to avoid cross-platform issues, we'll replace all forms of newline with '\n'
        return output.replaceAll("\r\n|\r", "\n");
    }

    /**
     * Returns the contents of the specified file
     *
     * @param filename
     * @return Contents of the specified file
     * @throws IOException
     */
    protected static String unitTestFileAsString(String filename) throws IOException
    {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(UNIT_TEST_DIR + filename)));
        StringBuilder sb = new StringBuilder(1024);
        for (String line = br.readLine(); line != null; line = br.readLine())
        {
            sb.append(line);
            sb.append('\n');
        }
        return sb.toString();
    }
}
