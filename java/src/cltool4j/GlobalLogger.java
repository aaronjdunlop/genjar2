package cltool4j;

import java.util.logging.Logger;

/**
 * Globally-accessible {@link Logger} instance for a command-line tool.
 * 
 * @author aarond
 */
public class GlobalLogger extends Logger {

    private final static GlobalLogger singletonInstance = new GlobalLogger();

    private GlobalLogger() {
        super("GlobalLogger", null);
    }

    public static GlobalLogger singleton() {
        return singletonInstance;
    }
}
