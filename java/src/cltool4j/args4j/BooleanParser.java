package cltool4j.args4j;

import java.util.HashMap;
import java.util.Map;


/**
 * Boolean {@link ArgumentParser}.
 * 
 * TODO Eliminate parsing of booleans? Or limit it to a multivalued handler
 * 
 * @author Kohsuke Kawaguchi
 */
public class BooleanParser extends ArgumentParser<Boolean> {
    private static final Map<String, Boolean> ACCEPTABLE_VALUES = new HashMap<String, Boolean>();

    static {
        ACCEPTABLE_VALUES.put("true", Boolean.TRUE);
        ACCEPTABLE_VALUES.put("false", Boolean.FALSE);
        ACCEPTABLE_VALUES.put("t", Boolean.TRUE);
        ACCEPTABLE_VALUES.put("f", Boolean.FALSE);
        ACCEPTABLE_VALUES.put("on", Boolean.TRUE);
        ACCEPTABLE_VALUES.put("off", Boolean.FALSE);
        ACCEPTABLE_VALUES.put("yes", Boolean.TRUE);
        ACCEPTABLE_VALUES.put("no", Boolean.FALSE);
        ACCEPTABLE_VALUES.put("1", Boolean.TRUE);
        ACCEPTABLE_VALUES.put("0", Boolean.FALSE);
    }

    public BooleanParser(CmdLineParser parser) {
        super();
    }

    @Override
    public Boolean parseNextOperand(Parameters parameters) throws IllegalArgumentException {
        // If we're 'parsing' a boolean option, don't consume another argument
        return true;
    }

    @Override
    public Boolean parse(String arg) throws IllegalArgumentException {

        if (!ACCEPTABLE_VALUES.containsKey(arg)) {
            throw new IllegalArgumentException();
        }
        return ACCEPTABLE_VALUES.get(arg);
    }

    @Override
    public String defaultMetaVar() {
        return null;
    }

}
