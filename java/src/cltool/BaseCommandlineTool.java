package cltool;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.zip.GZIPInputStream;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OneArgumentOptionHandler;
import org.kohsuke.args4j.spi.Setter;

/**
 * Base class for any tools which should be executable from the command-line. This class implements
 * the majority of the functionality needed to execute java code as a 'standard' command-line tool,
 * including parsing command-line options and reading input from either STDIN or from multiple files
 * specified on the command-line.
 *
 * Unfortunately, it doesn't appear possible to determine the actual class being executed within
 * <code>main(String[])</code>, so each subclass must implement a <code>main(String[])</code> method
 * and call {@link BaseCommandlineTool#run(String[])} from within it.
 *
 * In addition, subclasses should include a no-argument constructor and the abstract methods
 * declared here in the superclass.
 *
 *
 * @author Aaron Dunlop
 * @since Aug 14, 2008
 *
 *        $Id$
 */
public abstract class BaseCommandlineTool
{
    /** Non-threadable tools use a single thread */
    @Option(name = "-xt", metaVar = "threads", usage = "Maximum threads")
    protected int maxThreads = Runtime.getRuntime().availableProcessors();

    @Option(name = "-out", metaVar = "filename", usage = "Output file")
    protected File outputFile = null;

    protected static Logger logger = Logger.getLogger("default");

    @Option(name = "-v", handler = LogLevelOptionHandler.class, metaVar = "level", usage = "Verbosity  (3,all; 2,trace, 1,debug; 0,info; -1,warn; -2,error; -3,fatal; -4,off)")
    protected LogLevel verbosityLevel = LogLevel.info;

    @Option(name = "-time", usage = "Output execution times")
    protected boolean time = false;

    protected String[] args = new String[0];

    @Argument(multiValued = true, metaVar = "[files]")
    protected String[] inputFiles = new String[0];

    protected Exception exception;

    protected final static SimpleDateFormat COMMANDLINE_DATE_FORMATS[] = new SimpleDateFormat[] {
        // Dot-separated, with time
        new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS.ZZZ"),
        new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS"),
        new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss"),
        new SimpleDateFormat("yyyy.MM.dd.HH.mm"),
        new SimpleDateFormat("yyyy.MM.dd.HH.mm"),

        // Dot- and colon-separated, with time
        new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS ZZZ"), new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS"),
        new SimpleDateFormat("yyyy.MM.dd HH:mm:ss"), new SimpleDateFormat("yyyy.MM.dd HH:mm"),

        new SimpleDateFormat("MM.dd.yyyy HH:mm:ss"),
        new SimpleDateFormat("MM.dd.yy HH:mm:ss"),
        new SimpleDateFormat("MM.dd HH:mm:ss"),
        new SimpleDateFormat("MM.dd.yyyy HH:mm"),
        new SimpleDateFormat("MM.dd.yy HH:mm"),
        new SimpleDateFormat("MM.dd HH:mm"),

        // Dot-separated, without time
        new SimpleDateFormat("yyyy.MM.dd"),
        new SimpleDateFormat("MM.dd.yyyy"),
        new SimpleDateFormat("MM.dd.yy"),
        new SimpleDateFormat("MM.dd"),

        // Slash-separated, with time
        new SimpleDateFormat("MM/dd/yyyy HH:mm:ss"), new SimpleDateFormat("MM/dd/yy HH:mm:ss"),
        new SimpleDateFormat("MM/dd HH:mm:ss"), new SimpleDateFormat("MM/dd/yyyy HH:mm"),
        new SimpleDateFormat("MM/dd/yy HH:mm"), new SimpleDateFormat("MM/dd HH:mm"),

        // Slash-separated, without time
        new SimpleDateFormat("yyyy/MM/dd"), new SimpleDateFormat("MM/dd/yyyy"), new SimpleDateFormat("MM/dd/yy"),
        new SimpleDateFormat("MM/dd")};

    /**
     * Perform any tool-specific setup. This method will only be called once, even if the tool is
     * threadable and {@link #run()} is called by multiple threads.
     *
     * @param parser The command-line parser which parsed the options. Use this parser instance if
     *            an overriding implementation needs to throw a {@link CmdLineException}.
     */
    protected void setup(final CmdLineParser parser) throws Exception
    {}

    /**
     * Execute the tool's core functionality. If the tool is threadable, this method should be
     * thread-safe and reentrant.
     *
     * @throws Exception
     */
    protected abstract void run() throws Exception;

    /**
     * Parses command-line arguments and executes the tool. This method should be called from within
     * the main() methods of all subclasses.
     *
     * @param args
     */
    public final static void run(final String[] args)
    {
        try
        {
            final BaseCommandlineTool tool = (BaseCommandlineTool) Class.forName(
                Thread.currentThread().getStackTrace()[2].getClassName()).getConstructor(new Class[] {}).newInstance(
                new Object[] {});
            run(tool, args);
        }
        catch (final Exception e)
        {
            e.printStackTrace();
        }
    }

    final static void run(final BaseCommandlineTool tool, final String[] args) throws Exception
    {
        final long startTime = System.currentTimeMillis();

        CmdLineParser.registerHandler(Date.class, DateOptionHandler.class);
        CmdLineParser.registerHandler(Calendar.class, CalendarOptionHandler.class);

        final CmdLineParser parser = new CmdLineParser(tool);

        try
        {
            parser.parseArgument(args);

            // Configure Log4J to log to the console, and only the message actually logged,
            // without any header or formatting.
            BasicConfigurator.configure(new ConsoleAppender(new PatternLayout("%m%n")));

            BaseCommandlineTool.logger.setLevel(tool.verbosityLevel.toLog4JLevel());

            if (tool.outputFile != null)
            {
                try
                {
                    System.setOut(new PrintStream(tool.outputFile));
                }
                catch (final FileNotFoundException e)
                {
                    System.err.println("Unable to open " + tool.outputFile + " : " + e.getMessage());
                    return;
                }
            }

            tool.setup(null);
        }
        catch (final CmdLineException e)
        {
            System.err.println(e.getMessage());
            System.err.println("\nUsage:");
            parser.printUsage(System.err);
            return;
        }

        // Handle arguments
        if (tool.inputFiles.length > 0 && tool.inputFiles[0].length() > 0)
        {
            // Handle one or more input files from the command-line, translating gzipped
            // files as appropriate.
            // TODO: Re-route multiple files into a single InputStream so we can execute the tool a
            // single time.
            for (final Object filename : tool.inputFiles)
            {
                final InputStream is = fileAsInputStream((String) filename);
                System.setIn(is);
                tool.run();
                is.close();
            }
        }
        else
        {
            // Handle input on STDIN
            tool.run();
        }

        if (tool.exception != null)
        {
            throw tool.exception;
        }

        if (tool.time)
        {
            System.out.format("Execution Time: %dms\n", System.currentTimeMillis() - startTime);
        }

        System.out.close();
    }

    /**
     * Open the specified file, uncompressing GZIP'd files as appropriate
     *
     * @param filename
     * @return InputStream
     * @throws IOException
     */
    protected static InputStream fileAsInputStream(final String filename) throws IOException
    {
        final File f = new File(filename);
        if (!f.exists())
        {
            System.err.println("Unable to find file: " + filename);
            System.exit(-1);
        }

        InputStream is = new FileInputStream(filename);
        if (filename.endsWith(".gz"))
        {
            is = new GZIPInputStream(is);
        }
        return is;
    }

    protected static Calendar parseDate(final CmdLineParser parser, final String date) throws CmdLineException
    {
        final Calendar c = Calendar.getInstance();
        final int year = c.get(Calendar.YEAR);

        for (final SimpleDateFormat dateFormat : COMMANDLINE_DATE_FORMATS)
        {
            try
            {
                final Date d = dateFormat.parse(date);
                c.setTime(d);
                if (c.get(Calendar.YEAR) == 1970)
                {
                    c.set(Calendar.YEAR, year);
                }
                return c;
            }
            catch (final java.text.ParseException ignore)
            {}
        }

        throw new CmdLineException(parser, "Error parsing date: " + date);
    }

    /**
     * @return Logger instance
     */
    public static Logger getLogger()
    {
        return logger;
    }

    /**
     * Parses a date into a {@link java.util.Calendar}
     */
    public static class CalendarOptionHandler extends OneArgumentOptionHandler<Calendar>
    {
        public CalendarOptionHandler(final CmdLineParser parser, final OptionDef option, final Setter<? super Calendar> setter)
        {
            super(parser, option, setter);
        }

        @Override
        public Calendar parse(final String s) throws CmdLineException
        {
            return BaseCommandlineTool.parseDate(owner, s.toLowerCase());
        }

        @Override
        public String getDefaultMetaVariable()
        {
            return "date";
        }
    }

    /**
     * Parses a date into a {@link java.util.Date}
     */
    public static class DateOptionHandler extends OneArgumentOptionHandler<Date>
    {
        public DateOptionHandler(final CmdLineParser parser, final OptionDef option, final Setter<? super Date> setter)
        {
            super(parser, option, setter);
        }

        @Override
        public Date parse(final String s) throws CmdLineException
        {
            return BaseCommandlineTool.parseDate(owner, s.toLowerCase()).getTime();
        }

        @Override
        public String getDefaultMetaVariable()
        {
            return "date";
        }
    }

    /**
     * Parses a date into a long (seconds since the epoch)
     */
    public static class TimestampOptionHandler extends OneArgumentOptionHandler<Long>
    {
        public TimestampOptionHandler(final CmdLineParser parser, final OptionDef option, final Setter<? super Long> setter)
        {
            super(parser, option, setter);
        }

        @Override
        public Long parse(final String s) throws CmdLineException
        {
            return BaseCommandlineTool.parseDate(owner, s.toLowerCase()).getTime().getTime();
        }

        @Override
        public String getDefaultMetaVariable()
        {
            return "date";
        }
    }

    public static enum LogLevel
    {
        all, trace, debug, info, warn, error, fatal, off;

        public Level toLog4JLevel()
        {
            switch (this)
            {
                case all : return Level.ALL;
                case trace : return Level.TRACE;
                case debug : return Level.DEBUG;
                case info : return Level.INFO;
                case warn : return Level.WARN;
                case error : return Level.ERROR;
                case fatal : return Level.FATAL;
                case off : return Level.OFF;
                default : return null;
            }
        }
    }

    public static class LogLevelOptionHandler extends OneArgumentOptionHandler<LogLevel>
    {
        public LogLevelOptionHandler(final CmdLineParser parser, final OptionDef option, final Setter<? super LogLevel> setter)
        {
            super(parser, option, setter);
        }

        /**
         * @return "level"
         */
        @Override
        public String getDefaultMetaVariable()
        {
            return "level";
        }

        @Override
        public LogLevel parse(String s) throws CmdLineException
        {
            if (Character.isDigit(s.charAt(s.length() - 1)))
            {
                s = s.replaceFirst("\\+", "");
                final int level = Integer.parseInt(s);
                switch (level)
                {
                    case 3 : return LogLevel.all;
                    case 2 : return LogLevel.trace;
                    case 1 : return LogLevel.debug;
                    case 0 : return LogLevel.info;
                    case -1 : return LogLevel.warn;
                    case -2 : return LogLevel.error;
                    case -3 : return LogLevel.fatal;
                    case -4 : return LogLevel.off;
                }
            }
            else
            {
                if (s.equals("all"))
                {
                    return LogLevel.all;
                }
                else if (s.equals("trace"))
                {
                    return LogLevel.trace;
                }
                else if (s.equals("debug"))
                {
                    return LogLevel.debug;
                }
                else if (s.equals("info"))
                {
                    return LogLevel.info;
                }
                else if (s.equals("warn"))
                {
                    return LogLevel.warn;
                }
                else if (s.equals("error"))
                {
                    return LogLevel.error;
                }
                else if (s.equals("fatal"))
                {
                    return LogLevel.fatal;
                }
                else if (s.equals("off"))
                {
                    return LogLevel.off;
                }
            }

            throw new CmdLineException(owner, "Unknown verbosity level: " + s);
        }
    }

    public static class MemoryOptionHandler extends OneArgumentOptionHandler<Integer>
    {
        public MemoryOptionHandler(final CmdLineParser parser, final OptionDef option, final Setter<? super Integer> setter)
        {
            super(parser, option, setter);
        }

        @Override
        public Integer parse(String s) throws CmdLineException
        {
            int multiplier = 1;
            if (s.endsWith("m"))
            {
                multiplier = 1024 * 1024;
                s = s.substring(0, s.length() - 1);
            }
            else if (s.endsWith("k"))
            {
                multiplier = 1024;
                s = s.substring(0, s.length() - 1);
            }
            else if (s.endsWith("g"))
            {
                multiplier = 1024 * 1024 * 1024;
                s = s.substring(0, s.length() - 1);
            }
            try
            {
                return new Integer(Integer.parseInt(s) * multiplier);
            }
            catch (final NumberFormatException e)
            {
                throw new CmdLineException(owner, "Error parsing memory argument: " + s);
            }
        }

        @Override
        public String getDefaultMetaVariable()
        {
            return "amount";
        }
    }
}
