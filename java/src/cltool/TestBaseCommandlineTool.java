package cltool;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Date;

import org.junit.Test;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import static org.junit.Assert.*;

public class TestBaseCommandlineTool extends ToolTestCase
{
    /**
     * Verifies that the {@link BaseCommandlineTool#setup(CmdLineParser)} method is executed
     * properly
     *
     * @throws Exception
     */
    @Test
    public void testSetup() throws Exception
    {
        final Cat tool = new Cat();
        executeTool(tool, "", "");
        assertTrue("Expected setupFlag = 'true'", tool.setupFlag);
        assertEquals(2, tool.option);
    }

    @Test
    public void testCat() throws Exception
    {
        final String input = "This is a\nthree-line\ntest.\n";
        final String output = executeTool(new Cat(), "", input);
        assertEquals(input, output);
    }

    @Test
    public void testDebugOutput() throws Exception
    {
        final String input = "This is a\nthree-line\ntest.\n";

        StringBuilder sb = new StringBuilder();
        sb.append("MaxThreads: 1\n");
        sb.append(input);
        sb.append("Execution Time: 0ms\n");

        String output = executeTool(new Cat(), "-v debug -time", input);
        // Just in case the actual run took a measurable amount of time
        output = output.replaceFirst("(: [0-9]+ms)", ": 0ms");

        assertEquals(sb.toString(), output);

        // Try a couple other ways of specifying verbosity
        sb = new StringBuilder();
        sb.append("MaxThreads: 1\n");
        sb.append(input);
        assertEquals(sb.toString(), executeTool(new Cat(), "-v 2", input));
        assertEquals(sb.toString(), executeTool(new Cat(), "-v +2", input));
        assertEquals(input, executeTool(new Cat(), "-v -1", input));
    }

    /**
     * Tests argument parsing. Pass three arguments to a class which declares one required and one
     * optional argument. We expect the declared arguments to be populated and the third to be
     * considerd as an input file.
     *
     * @throws Exception
     */
    @Test
    public void testArguments() throws Exception
    {
        final WithRequiredArguments tool = new WithRequiredArguments();
        executeTool(tool, "-option foo arg0 arg1 arg2", "");
        assertEquals("foo", tool.option);
        assertEquals("arg0", tool.arg0);
        assertEquals("arg1", tool.arg1);
        assertArrayEquals(new String[] {"arg2"}, tool.otherArgs);
    }

    /**
     * Tests parsing required multi-value arguments. Tests a class which declares two arguments and
     * required multi-value args.
     *
     * @throws Exception
     */
    @Test
    public void testRequiredMultivaluedArgument() throws Exception
    {
        final WithRequiredMultivaluedArgument tool = new WithRequiredMultivaluedArgument();

        // Verify that multi-valued arg is parsed
        executeTool(tool, "arg1 arg2 arg3", "");
        assertArrayEquals(new String[] {"arg1", "arg2", "arg3"}, tool.requiredArgs);

        // Verify usage output if multi-valued arg is not supplied
        final StringBuilder sb = new StringBuilder();
        sb.append("Argument <args> is required\n");
        sb.append("\n");
        sb
            .append("Usage: TestBaseCommandlineTool$WithRequiredMultivaluedArgument [-out filename] [-time] [-v level] <args>\n");
        sb.append(" args          : [args]\n");
        sb.append(" -out filename : Output file\n");
        sb.append(" -time         : Output execution times\n");
        sb.append(" -v level      : Verbosity  (all,+5,5; finest,+4,4; finer,+3,3; fine,+2,2,debug;\n");
        sb.append("                 config,+1,1; info,0; warning,-1; severe,-2; off,-3)   Default = info\n");

        assertEquals(sb.toString(), executeTool(tool, "", ""));
    }

    /**
     * Tests parsing required multi-value arguments. Tests a class which declares two arguments and
     * required multi-value args.
     *
     * @throws Exception
     */
    @Test
    public void testRequiredMultiValuedArgumentWithOtherArguments() throws Exception
    {
        final WithRequiredArgumentsAndMultivaluedArgument tool = new WithRequiredArgumentsAndMultivaluedArgument();

        // Verify that multi-valued parameters are parsed
        executeTool(tool, "arg0 arg1 arg2 arg3 arg4", "");
        assertEquals("arg0", tool.arg0);
        assertEquals("arg1", tool.arg1);
        assertArrayEquals(new String[] {"arg2", "arg3", "arg4"}, tool.otherArgs);

        // Verify usage output if multi-valued params are not supplied
        StringBuilder sb = new StringBuilder();
        sb.append("Argument <arg0> is required\n");
        sb.append("\n");
        sb
            .append("Usage: TestBaseCommandlineTool$WithRequiredArgumentsAndMultivaluedArgument [-out filename] [-time] [-v level] <arg0> <arg1> <values>\n");
        sb.append(" arg0          : arg0\n");
        sb.append(" arg1          : arg1\n");
        sb.append(" values        : Other required arguments\n");
        sb.append(" -out filename : Output file\n");
        sb.append(" -time         : Output execution times\n");
        sb.append(" -v level      : Verbosity  (all,+5,5; finest,+4,4; finer,+3,3; fine,+2,2,debug;\n");
        sb.append("                 config,+1,1; info,0; warning,-1; severe,-2; off,-3)   Default = info\n");
        assertEquals(sb.toString(), executeTool(tool, "", ""));

        sb = new StringBuilder();
        sb.append("Argument <values> is required\n");
        sb.append("\n");
        sb
            .append("Usage: TestBaseCommandlineTool$WithRequiredArgumentsAndMultivaluedArgument [-out filename] [-time] [-v level] <arg0> <arg1> <values>\n");
        sb.append(" arg0          : arg0\n");
        sb.append(" arg1          : arg1\n");
        sb.append(" values        : Other required arguments\n");
        sb.append(" -out filename : Output file\n");
        sb.append(" -time         : Output execution times\n");
        sb.append(" -v level      : Verbosity  (all,+5,5; finest,+4,4; finer,+3,3; fine,+2,2,debug;\n");
        sb.append("                 config,+1,1; info,0; warning,-1; severe,-2; off,-3)   Default = info\n");

        assertEquals(sb.toString(), executeTool(tool, "arg1 arg2", ""));
    }

    /**
     * Tests parsing required multi-value arguments. Tests a class which declares optional single-
     * and multi-valued arguments.
     *
     * @throws Exception
     */
    @Test
    public void testOptionalMultivaluedArgument() throws Exception
    {
        final WithOptionalMultivaluedArgument tool = new WithOptionalMultivaluedArgument();

        // Verify that both single- and multi-valued args is parsed
        executeTool(tool, "arg1", "");
        assertEquals("arg1", tool.optionalArg);
        assertNull(tool.optionalArgs);

        executeTool(tool, "arg1 arg2 arg3", "");
        assertEquals("arg1", tool.optionalArg);
        assertArrayEquals(new String[] {"arg2", "arg3"}, tool.optionalArgs);
    }

    @Test
    public void testUsageOutput() throws Exception
    {
        // Test with an invalid option
        StringBuilder sb = new StringBuilder();
        sb.append("\"-badarg\" is not a valid option\n");
        sb.append("\n");
        sb.append("Usage: TestBaseCommandlineTool$Cat [-option opt] [-out filename] [-time] [-v level] [files]\n");
        sb.append(" -option opt   : Integer option;   Default = 2\n");
        sb.append(" -out filename : Output file\n");
        sb.append(" -time         : Output execution times\n");
        sb.append(" -v level      : Verbosity  (all,+5,5; finest,+4,4; finer,+3,3; fine,+2,2,debug;\n");
        sb.append("                 config,+1,1; info,0; warning,-1; severe,-2; off,-3)   Default = info\n");

        assertEquals(sb.toString(), executeTool(new Cat(), "-badarg", ""));

        // With a missing option
        sb = new StringBuilder();
        sb.append("Option \"-option\" is required\n");
        sb.append("\n");
        sb
            .append("Usage: TestBaseCommandlineTool$WithRequiredArguments <-option value> [-out filename] [-time] [-v level] [-xt threads] <arg0> [arg1] [values]\n");
        sb.append(" arg0          : arg0\n");
        sb.append(" arg1          : arg1\n");
        sb.append(" values        : [other args]\n");
        sb.append(" -option value : o\n");
        sb.append(" -out filename : Output file\n");
        sb.append(" -time         : Output execution times\n");
        sb.append(" -v level      : Verbosity  (all,+5,5; finest,+4,4; finer,+3,3; fine,+2,2,debug;\n");
        sb.append("                 config,+1,1; info,0; warning,-1; severe,-2; off,-3)   Default = info\n");
        sb.append(" -xt threads   : Maximum threads;   Default = 2\n");

        WithRequiredArguments tool = new WithRequiredArguments();
        assertEquals(sb.toString(), executeTool(tool, "argument", ""));
        // assertEquals("argument", tool.arg);

        // With a missing argument
        sb = new StringBuilder();
        sb.append("Argument <arg0> is required\n");
        sb.append("\n");
        sb
            .append("Usage: TestBaseCommandlineTool$WithRequiredArguments <-option value> [-out filename] [-time] [-v level] [-xt threads] <arg0> [arg1] [values]\n");
        sb.append(" arg0          : arg0\n");
        sb.append(" arg1          : arg1\n");
        sb.append(" values        : [other args]\n");
        sb.append(" -option value : o;   Default = foo\n");
        sb.append(" -out filename : Output file\n");
        sb.append(" -time         : Output execution times\n");
        sb.append(" -v level      : Verbosity  (all,+5,5; finest,+4,4; finer,+3,3; fine,+2,2,debug;\n");
        sb.append("                 config,+1,1; info,0; warning,-1; severe,-2; off,-3)   Default = info\n");
        sb.append(" -xt threads   : Maximum threads;   Default = 2\n");

        tool = new WithRequiredArguments();
        assertEquals(sb.toString(), executeTool(tool, "-option foo", ""));
        assertEquals("foo", tool.option);
    }

    @Test
    public void testMultivaluedOption() throws Exception
    {
        final WithMultivaluedOption tool = new WithMultivaluedOption();
        executeTool(tool, "", "");
        assertArrayEquals(new int[] {1}, tool.intOpts);

        executeTool(tool, "-i 2,3", "");
        assertArrayEquals(new int[] {2, 3}, tool.intOpts);
    }

    @Test
    public void testDateOptionHandler() throws Exception
    {
        final WithDateField tool = new WithDateField();
        executeTool(tool, "2008.12.01", "");
        assertEquals(1228118400000L, tool.date.getTime());

        executeTool(tool, "12.01.2008", "");
        assertEquals(1228118400000L, tool.date.getTime());

        executeTool(tool, "2008/12/01", "");
        assertEquals(1228118400000L, tool.date.getTime());

        executeTool(tool, "12/01/2008", "");
        assertEquals(1228118400000L, tool.date.getTime());

        // And test a parse failure
        final String output = executeTool(tool, "123456", "");
        assertTrue(output.startsWith("Error parsing date: 123456"));
    }

    @Test
    public void testCalendarOptionHandler() throws Exception
    {
        final WithCalendarField tool = new WithCalendarField();
        executeTool(tool, "2008.12.01", "");
        assertEquals(2008, tool.cal.get(Calendar.YEAR));
        assertEquals(11, tool.cal.get(Calendar.MONTH));
        assertEquals(1, tool.cal.get(Calendar.DATE));

        // And test a parse failure
        final String output = executeTool(tool, "123456", "");
        assertTrue(output.startsWith("Error parsing date: 123456"));
    }

    @Test
    public void testMemoryOptionHandler() throws Exception
    {
        final WithMemoryField tool = new WithMemoryField();
        executeTool(tool, "1500", "");
        assertEquals(1500, tool.memory);
        executeTool(tool, "1500m", "");
        assertEquals(1500 * 1024 * 1024, tool.memory);
        executeTool(tool, "2k", "");
        assertEquals(2048, tool.memory);
        executeTool(tool, "1g", "");
        assertEquals(1024 * 1024 * 1024, tool.memory);

        // And test a parse failure
        final String output = executeTool(tool, "2z", "");
        assertTrue(output.startsWith("Error parsing memory argument: 2z"));
    }

    /**
     * Tests enumeration handling - both through case-insensitive lexical matching and through the
     * forString() method.
     *
     * @throws Exception
     */
    @Test
    public void testEnumOptionHandler() throws Exception
    {
        final WithEnumField tool = new WithEnumField();
        executeTool(tool, "OptionB", "");
        assertEquals(Enum.OptionB, tool.e);
        executeTool(tool, "optiona", "");
        assertEquals(Enum.OptionA, tool.e);
        executeTool(tool, "b", "");
        assertEquals(Enum.OptionB, tool.e);

        // And test a parse failure
        final String output = executeTool(tool, "2z", "");
        assertTrue("Wrong error output", output.startsWith("\"2z\" is not a valid value for <enum>"));
    }

    private static class Cat extends BaseCommandlineTool
    {
        private boolean setupFlag = false;

        @Option(name = "-option", metaVar = "opt", usage = "Integer option")
        public int option = 2;

        @Override
        public void setup(final CmdLineParser parser) throws Exception
        {
            setupFlag = true;
        }

        @Override
        public void run() throws Exception
        {
            logger.fine("MaxThreads: " + maxThreads);
            final StringBuffer sb = new StringBuffer();
            for (final String arg : inputFiles)
            {
                sb.append(arg);
                sb.append(' ');
            }
            if (sb.length() > 0)
            {
                sb.deleteCharAt(sb.length() - 1);
            }

            final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            for (String s = br.readLine(); s != null; s = br.readLine())
            {
                System.out.println(s);
            }
        }
    }

    private static class WithMultivaluedOption extends BaseCommandlineTool
    {
        @Option(name = "-i", multiValued = true, separator = ",", usage = "[args]", metaVar = "args")
        private final int[] intOpts = {1};

        @Override
        protected void run() throws Exception
        {}
    }

    @Threadable
    private static class WithRequiredArguments extends BaseCommandlineTool
    {
        @Argument(index = 0, required = true, usage = "arg0", metaVar = "arg0")
        private String arg0;

        @Argument(index = 1, usage = "arg1", metaVar = "arg1")
        private String arg1;

        @Argument(usage = "[other args]", metaVar = "values")
        private String[] otherArgs;

        @Option(name = "-option", usage = "o", required = true, metaVar = "value")
        private String option;

        @Override
        protected void run() throws Exception
        {}
    }

    private static class WithRequiredMultivaluedArgument extends BaseCommandlineTool
    {
        @Argument(required = true, multiValued = true, usage = "[args]", metaVar = "args")
        private String[] requiredArgs;

        @Override
        protected void run() throws Exception
        {}
    }

    private static class WithOptionalMultivaluedArgument extends BaseCommandlineTool
    {
        @Argument(required = false, usage = "[arg]", metaVar = "arg")
        private String optionalArg;

        @Argument(required = false, multiValued = true, usage = "[args]", metaVar = "args")
        private String[] optionalArgs;

        @Override
        protected void run() throws Exception
        {}
    }

    private static class WithRequiredArgumentsAndMultivaluedArgument extends BaseCommandlineTool
    {
        // arg0 and arg1 are declared in reverse order, just to confirm that 'index' is used and not
        // declaration ordering.
        @Argument(index = 1, required = true, usage = "arg1", metaVar = "arg1")
        private String arg1;

        @Argument(index = 0, required = true, usage = "arg0", metaVar = "arg0")
        private String arg0;

        @Argument(required = true, usage = "Other required arguments", metaVar = "values")
        private String[] otherArgs;

        @Override
        protected void run() throws Exception
        {
            final StringBuilder sb = new StringBuilder();
            for (final String arg : otherArgs)
            {
                sb.append(arg);
                sb.append(' ');
            }
            System.out.println(sb.substring(0, sb.length() - 2));
        }
    }

    private static class WithCalendarField extends BaseCommandlineTool
    {
        @Argument(index = 0, required = true, usage = "date", metaVar = "date")
        protected Calendar cal;

        @Override
        protected void run() throws Exception
        {}
    }

    private static class WithDateField extends BaseCommandlineTool
    {
        @Argument(index = 0, required = true, usage = "date", metaVar = "date")
        protected Date date;

        @Override
        protected void run() throws Exception
        {}
    }

    private static class WithMemoryField extends BaseCommandlineTool
    {
        @Argument(index = 0, required = true, usage = "Memory value", metaVar = "value")
        protected int memory;

        @Override
        protected void run() throws Exception
        {}
    }

    private static class WithEnumField extends BaseCommandlineTool
    {
        @Argument(index = 0, required = true, usage = "Enum value", metaVar = "enum")
        protected Enum e;

        @Override
        protected void run() throws Exception
        {}
    }

    public static enum Enum
    {
        OptionA, OptionB;

        public static Enum forString(final String s)
        {
            if (s.equalsIgnoreCase("a"))
            {
                return OptionA;
            }
            else if (s.equalsIgnoreCase("b"))
            {
                return OptionB;
            }

            return null;
        }
    }
}
