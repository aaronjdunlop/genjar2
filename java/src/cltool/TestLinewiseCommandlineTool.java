package cltool;

import java.util.concurrent.Callable;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class TestLinewiseCommandlineTool extends ToolTestCase
{
    /**
     * Tests ordering of multithreaded output, verifying that the lines are returned in the order
     * read even when 2 concurrent threads are processing the file.
     *
     * @throws Exception
     */
    @Test
    public void testLinewiseCat() throws Exception
    {
        String filename = "simple.txt";
        String expectedOutput = ToolTestCase.unitTestFileAsString(filename);
        // First, single-threaded
        assertEquals(expectedOutput, executeToolFromFile(new LinewiseCat(), "-xt 1", filename));

        // And now a 2-thread run
        assertEquals(expectedOutput, executeToolFromFile(new LinewiseCat(), "-xt 2", filename));
    }

    /**
     * Outputs each line as-is.
     */
    private static class LinewiseCat extends LinewiseCommandlineTool
    {
        @Override
        protected Callable<String> lineTask(final String line)
        {
            return new Callable<String>()
            {
                @Override
                public String call() throws Exception
                {
                    return line;
                }
            };
        }
    }
}
