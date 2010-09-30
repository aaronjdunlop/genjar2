package cltool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.jar.Manifest;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.EnumAliasMap;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OneArgumentOptionHandler;
import org.kohsuke.args4j.spi.Setter;

/**
 * Base class for any tools which should be executable from the command-line. This class implements the
 * majority of the functionality needed to execute java code as a 'standard' command-line tool, including
 * parsing command-line options and reading input from either STDIN or from multiple files specified on the
 * command-line.
 *
 * Unfortunately, it doesn't appear possible to determine the actual class being executed within
 * <code>main(String[])</code>, so each subclass must implement a <code>main(String[])</code> method and call
 * {@link BaseCommandlineTool#run(String[])} from within it.
 *
 * In addition, subclasses should include a no-argument constructor and the abstract methods declared here in
 * the superclass.
 *
 *
 * @author Aaron Dunlop
 * @since Aug 14, 2008
 *
 *        $Id$
 */
public abstract class BaseCommandlineTool {

    @Option(name = "-help", help = true, usage = "Print detailed usage information")
    protected boolean printHelp = false;

    @Option(name = "-out", hidden = true, metaVar = "filename", usage = "Output file")
    protected File outputFile = null;

    @Option(name = "-time", hidden = true, usage = "Output execution times")
    protected boolean time = false;

    @Option(name = "-v", metaVar = "level", usage = "Verbosity")
    protected LogLevel verbosityLevel = LogLevel.info;

    @Option(name = "-version", hidden = true, help = true, usage = "Print version information")
    protected boolean printVersion = false;

    /**
     * Non-threadable tools use a single thread; {@link Threadable} tools default to either the optional
     * 'defaultThreads' parameter or the number of CPUs
     */
    @Option(name = "-xt", metaVar = "threads", usage = "Maximum threads", requiredAnnotations = { Threadable.class })
    protected int maxThreads = getClass().getAnnotation(Threadable.class) != null ? (getClass()
            .getAnnotation(Threadable.class).defaultThreads() != 0 ? getClass().getAnnotation(
            Threadable.class).defaultThreads() : Runtime.getRuntime().availableProcessors()) : 1;

    protected static Logger logger = Logger.getLogger("default");

    @Argument(multiValued = true, metaVar = "files")
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
            new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS ZZZ"),
            new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS"),
            new SimpleDateFormat("yyyy.MM.dd HH:mm:ss"),
            new SimpleDateFormat("yyyy.MM.dd HH:mm"),

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
            new SimpleDateFormat("MM/dd HH:mm:ss"),
            new SimpleDateFormat("MM/dd/yyyy HH:mm"),
            new SimpleDateFormat("MM/dd/yy HH:mm"),
            new SimpleDateFormat("MM/dd HH:mm"),

            // Slash-separated, without time
            new SimpleDateFormat("yyyy/MM/dd"), new SimpleDateFormat("MM/dd/yyyy"),
            new SimpleDateFormat("MM/dd/yy"), new SimpleDateFormat("MM/dd") };

    static {
        for (final SimpleDateFormat formatter : COMMANDLINE_DATE_FORMATS) {
            formatter.setLenient(false);
        }
    }

    /**
     * Default constructor
     */
    protected BaseCommandlineTool() {
    }

    /**
     * Perform any tool-specific setup. This method will only be called once, even if the tool is threadable
     * and {@link #run()} is called by multiple threads.
     *
     * @param parser The command-line parser which parsed the options. Use this parser instance if an
     *            overriding implementation needs to throw a {@link CmdLineException}.
     */
    protected void setup(final CmdLineParser parser) throws Exception {
    }

    /**
     * Perform any tool-specific setup. This method will only be called once, even if the tool is threadable
     * and {@link #run()} is called by multiple threads.
     */
    protected void cleanup() {
    }

    /**
     * Execute the tool's core functionality. If the tool is threadable, this method should be thread-safe and
     * reentrant.
     *
     * @throws Exception
     */
    protected abstract void run() throws Exception;

    /**
     * Parses command-line arguments and executes the tool. This method should be called from within the
     * main() methods of all subclasses.
     *
     * @param args
     */
    @SuppressWarnings("unchecked")
    public final static void run(final String[] args) {
        try {
            final Class<? extends BaseCommandlineTool> c = (Class<? extends BaseCommandlineTool>) Class
                    .forName(Thread.currentThread().getStackTrace()[2].getClassName());

            // For Scala objects
            try {
                final BaseCommandlineTool tool = (BaseCommandlineTool) c.getField("MODULE$").get(null);
                tool.runInternal(args);
            } catch (final Exception e) {
                // For Java
                final BaseCommandlineTool tool = c.getConstructor(new Class[] {})
                        .newInstance(new Object[] {});
                tool.runInternal(args);
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    protected final void runInternal(final String[] args) throws Exception {
        final long startTime = System.currentTimeMillis();

        CmdLineParser.registerHandler(Date.class, DateOptionHandler.class);
        CmdLineParser.registerHandler(Calendar.class, CalendarOptionHandler.class);

        final CmdLineParser parser = new CmdLineParser(this);
        parser.setUsageWidth(120);

        try {
            parser.parseArgument(args);

            // If the user specified -help, print extended usage information and exit
            if (printHelp) {
                // Don't print out default value for help flag
                printHelp = false;

                printUsage(parser, true);
                return;
            } else if (printVersion) {
                try {
                    final Class<? extends BaseCommandlineTool> c = getClass();
                    final String classFileName = c.getSimpleName() + ".class";
                    final String pathToThisClass = c.getResource(classFileName).toString();

                    final String pathToManifest = pathToThisClass.toString().substring(0,
                            pathToThisClass.indexOf("!") + 1)
                            + "/META-INF/MANIFEST.MF";

                    final Manifest manifest = new Manifest(new URL(pathToManifest).openStream());

                    if (manifest.getMainAttributes().getValue("Version") != null
                            && manifest.getMainAttributes().getValue("Version").length() > 0) {
                        System.out.println("Version: " + manifest.getMainAttributes().getValue("Version"));
                    }
                    System.out.println("Built at: " + manifest.getMainAttributes().getValue("Build-Time")
                            + " from source revision "
                            + manifest.getMainAttributes().getValue("Source-Revision"));
                } catch (final Exception e) {
                    System.out.println("Version information unavailable");
                }
                return;
            }

            // Configure java.util.logging to log to the console, and only the message actually
            // logged, without any header or formatting.
            logger = Logger.getLogger("cltool");
            for (final Handler h : logger.getHandlers()) {
                logger.removeHandler(h);
            }
            logger.setUseParentHandlers(false);
            final Level l = verbosityLevel.toLevel();
            logger.addHandler(new SystemOutHandler(l));
            logger.setLevel(l);

            if (outputFile != null) {
                try {
                    System.setOut(new PrintStream(outputFile));
                } catch (final FileNotFoundException e) {
                    System.err.println("Unable to open " + outputFile + " : " + e.getMessage());
                    return;
                }
            }

            setup(null);
        } catch (final CmdLineException e) {
            System.err.println(e.getMessage() + '\n');
            printUsage(parser, false);
            return;
        }

        // Handle arguments
        if (inputFiles.length > 0 && inputFiles[0].length() > 0) {
            // Handle one or more input files from the command-line, translating gzipped
            // files as appropriate.
            // TODO: Re-route multiple files into a single InputStream so we can execute the tool a
            // single time.
            for (final Object filename : inputFiles) {
                final InputStream is = fileAsInputStream((String) filename);
                System.setIn(is);
                run();
                is.close();
            }
        } else {
            // Handle input on STDIN
            run();
        }

        if (exception != null) {
            throw exception;
        }

        if (time) {
            System.out.format("Execution Time: %dms\n", System.currentTimeMillis() - startTime);
        }

        cleanup();
        System.out.flush();
        System.out.close();
    }

    private void printUsage(final CmdLineParser parser, final boolean includeHiddenOptions) {
        String classname = getClass().getName();
        classname = classname.substring(classname.lastIndexOf('.') + 1);
        if (classname.endsWith("$")) {
            classname = classname.substring(0, classname.length() - 1);
        }
        System.err.print("Usage: " + classname);
        parser.printOneLineUsage(System.err, includeHiddenOptions);
        parser.printUsage(System.err, includeHiddenOptions);
    }

    /**
     * Open the specified file, uncompressing GZIP'd files as appropriate
     *
     * @param filename
     * @return InputStream
     * @throws IOException
     */
    protected InputStream fileAsInputStream(final String filename) throws IOException {
        final File f = new File(filename);
        if (!f.exists()) {
            System.err.println("Unable to find file: " + filename);
            System.err.flush();
            System.exit(-1);
        }

        InputStream is = new FileInputStream(filename);
        if (filename.endsWith(".gz")) {
            is = new GZIPInputStream(is);
        }
        return is;
    }

    /**
     * Read the specified file, uncompressing GZIP'd files as appropriate
     *
     * @param filename
     * @return InputStream
     * @throws IOException
     */
    protected String fileAsString(final String filename) throws IOException {
        final File f = new File(filename);
        if (!f.exists()) {
            System.err.println("Unable to find file: " + filename);
            System.exit(-1);
        }

        final StringBuilder sb = new StringBuilder(10240);
        InputStream is = new FileInputStream(filename);
        if (filename.endsWith(".gz")) {
            is = new GZIPInputStream(is);
        }
        final BufferedReader r = new BufferedReader(new InputStreamReader(is));
        for (int c = r.read(); c != 0; c = r.read()) {
            sb.append((char) c);
        }
        return sb.toString();
    }

    /**
     * @return This morning at 00:00:00 local time as a {@link Date}
     */
    protected Date todayMidnight() {
        final Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        return cal.getTime();
    }

    /**
     * @return Yesterday at 00:00:00 local time as a {@link Date}
     */
    protected Date yesterdayMidnight() {
        final Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DATE, cal.get(Calendar.DATE) - 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        return cal.getTime();
    }

    /**
     * @return Yesterday at 23:59:59 local time as a {@link Date}
     */
    protected Date yesterday235959() {
        final Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DATE, cal.get(Calendar.DATE) - 1);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        return cal.getTime();
    }

    protected static Calendar parseDate(final CmdLineParser parser, final String date)
            throws CmdLineException {
        final Calendar c = Calendar.getInstance();
        final int year = c.get(Calendar.YEAR);

        for (final SimpleDateFormat dateFormat : COMMANDLINE_DATE_FORMATS) {
            try {
                final Date d = dateFormat.parse(date);
                c.setTime(d);
                if (c.get(Calendar.YEAR) == 1970) {
                    c.set(Calendar.YEAR, year);
                }
                return c;
            } catch (final java.text.ParseException ignore) {
            }
        }

        throw new CmdLineException(parser, "Error parsing date: " + date);
    }

    /**
     * @return Logger instance
     */
    public static Logger getLogger() {
        return logger;
    }

    /**
     * Parses a date into a {@link java.util.Calendar}
     */
    public static class CalendarOptionHandler extends OneArgumentOptionHandler<Calendar> {

        public CalendarOptionHandler(final CmdLineParser parser, final OptionDef option,
                final Setter<? super Calendar> setter) {
            super(parser, option, setter);
        }

        @Override
        public Calendar parse(final String s) throws CmdLineException {
            return BaseCommandlineTool.parseDate(owner, s.toLowerCase());
        }

        @Override
        public String getDefaultMetaVariable() {
            return "date";
        }
    }

    /**
     * Parses a date into a {@link java.util.Date}
     */
    public static class DateOptionHandler extends OneArgumentOptionHandler<Date> {

        public DateOptionHandler(final CmdLineParser parser, final OptionDef option,
                final Setter<? super Date> setter) {
            super(parser, option, setter);
        }

        @Override
        public Date parse(final String s) throws CmdLineException {
            return BaseCommandlineTool.parseDate(owner, s.toLowerCase()).getTime();
        }

        @Override
        public String getDefaultMetaVariable() {
            return "date";
        }
    }

    /**
     * Parses a date into a long (seconds since the epoch)
     */
    public static class TimestampOptionHandler extends OneArgumentOptionHandler<Long> {

        public TimestampOptionHandler(final CmdLineParser parser, final OptionDef option,
                final Setter<? super Long> setter) {
            super(parser, option, setter);
        }

        @Override
        public Long parse(final String s) throws CmdLineException {
            return BaseCommandlineTool.parseDate(owner, s.toLowerCase()).getTime().getTime();
        }

        @Override
        public String getDefaultMetaVariable() {
            return "date";
        }
    }

    public static enum LogLevel {
        all("+5", "5"), finest("+4", "4"), finer("+3", "3"), fine("+2", "2", "debug"), config("+1", "1"), info(
                "0"), warning("-1"), severe("-2"), off("-3");

        private LogLevel(final String... aliases) {
            EnumAliasMap.singleton().addAliases(this, aliases);
        }

        public Level toLevel() {
            switch (this) {
            case all:
                return Level.ALL;
            case finest:
                return Level.FINEST;
            case finer:
                return Level.FINER;
            case fine:
                return Level.FINE;
            case config:
                return Level.CONFIG;
            case info:
                return Level.INFO;
            case warning:
                return Level.WARNING;
            case severe:
                return Level.SEVERE;
            case off:
                return Level.OFF;
            default:
                return null;
            }
        }
    }

    private static class SystemOutHandler extends Handler {

        public SystemOutHandler(final Level level) {
            setLevel(level);
        }

        @Override
        public void close() throws SecurityException {
            flush();
        }

        @Override
        public void flush() {
            System.out.flush();
        }

        @Override
        public void publish(final LogRecord record) {
            System.out.println(record.getMessage());
        }
    }
}
