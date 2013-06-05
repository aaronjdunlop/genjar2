package net.sf.genjar;

import java.util.HashMap;
import java.util.HashSet;

public class ReferenceChain {

    private final HashMap<String, String> referenceMap;
    private final String classname;

    public ReferenceChain(final String classname, final HashMap<String, String> referenceMap) {
        this.classname = classname;
        this.referenceMap = referenceMap;
    }

    @Override
    public String toString() {
        // Just in case, protect against infinite loops
        final HashSet<String> observedClasses = new HashSet<String>();

        final StringBuilder sb = new StringBuilder();
        for (String currentClass = normalizeClassname(classname); currentClass != null; currentClass = normalizeClassname(referenceMap
                .get(currentClass))) {
            if (!observedClasses.add(currentClass)) {
                break;
            }

            sb.append(currentClass);
            sb.append(" <- ");
        }
        sb.delete(sb.length() - 4, sb.length() - 1);
        return sb.toString();
    }

    private String normalizeClassname(final String s) {
        return s == null ? null : s.replace('/', '.').replace(".class", "");
    }
}
