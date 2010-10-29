package cltool4j.args4j;

public class IntParser extends ArgumentParser<Integer> {

    @Override
    public Integer parse(String arg) throws NumberFormatException {
        int multiplier = 1;
        if (arg.endsWith("m")) {
            multiplier = 1024 * 1024;
            arg = arg.substring(0, arg.length() - 1);
        } else if (arg.endsWith("k")) {
            multiplier = 1024;
            arg = arg.substring(0, arg.length() - 1);
        } else if (arg.endsWith("g")) {
            multiplier = 1024 * 1024 * 1024;
            arg = arg.substring(0, arg.length() - 1);
        }
        return new Integer(Integer.parseInt(arg) * multiplier);
    }
}
