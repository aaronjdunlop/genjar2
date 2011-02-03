package cltool4j.args4j;

import java.io.StringWriter;

/**
 * Signals an error in the user input.
 * 
 * TODO Remove {@link #parser} field. It's not used except in unit tests
 * 
 * @author Kohsuke Kawaguchi
 */
public class CmdLineException extends Exception {
    private static final long serialVersionUID = -8574071211991372980L;

    private final CmdLineParser parser;

    public CmdLineException(CmdLineParser parser, String message) {
        super(message);
        this.parser = parser;
    }

    public CmdLineException(CmdLineParser parser, String message, Throwable cause) {
        super(message, cause);
        this.parser = parser;
    }

    public CmdLineException(CmdLineParser parser, Throwable cause) {
        super(cause);
        this.parser = parser;
    }

    public String getFullUsageMessage() {
        StringWriter sw = new StringWriter();
        parser.printUsage(sw, true);
        return sw.toString();
    }

    /**
     * @return the {@link CmdLineParser} that triggered the exception.
     */
    public CmdLineParser getParser() {
        return parser;
    }
}
