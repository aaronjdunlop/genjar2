package cltool4j.args4j;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * {@link Setter} that sets multiple values to a collection {@link Field}.
 * 
 * @author Kohsuke Kawaguchi
 */
public final class MultiValueFieldSetter<T> extends Setter<T> {
    private final Object bean;
    private final Field f;
    private final String separator;

    private T value;

    private final static String UNKNOWN_COLLECTION = " is not a known collection type";

    @SuppressWarnings("unchecked")
    public MultiValueFieldSetter(final Object bean, final CmdLineParser parser, final Field f) {
        super(f.getAnnotations(), (ArgumentParser<T>) parser.argumentParser(getType(f)));
        this.bean = bean;
        this.f = f;

        if (!(List.class.isAssignableFrom(f.getType()) || Set.class.isAssignableFrom(f.getType()) || f
                .getType().isArray())) {
            throw new IllegalAnnotationError("Field of type " + f.getType() + " is not supported");
        }

        if (option != null) {
            separator = option.separator().length() > 0 ? option.separator() : null;
        } else {
            separator = argument.separator().length() > 0 ? argument.separator() : null;
        }
    }

    @Override
    public void parseNextOperand(Parameters parameters) throws IllegalArgumentException {
        if (separator != null) {
            for (String operand : parameters.next().split(separator)) {
                addValue(argumentParser.parse(operand));
            }
        } else {
            addValue(argumentParser.parseNextOperand(parameters));
        }
    }

    @Override
    public void parseNextArgument(Parameters parameters) throws IllegalArgumentException {
        // For multi-valued setters, there's no difference between parsing operands and arguments
        parseNextOperand(parameters);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void addValue(final T v) {
        if (f.getType().isArray()) {
            // Array handling is ugly and requires copying as each argument is added,
            // but it's rare to create really large arrays on the command-line
            // (And note that ArrayList expansion often involves reference copying as well)
            T newArray = null;
            final Class<?> componentType = f.getType().getComponentType();
            if (value == null) {
                newArray = (T) Array.newInstance(componentType, 1);
                Array.set(newArray, 0, v);
            } else {
                final int currentLength = Array.getLength(value);
                newArray = (T) Array.newInstance(componentType, currentLength + 1);
                for (int i = 0; i < currentLength; i++) {
                    Array.set(newArray, i, Array.get(value, i));
                }
                Array.set(newArray, currentLength, v);
            }
            value = newArray;

        } else {
            if (value == null) {
                // We support LinkedList, ArrayList, HashSet, and TreeSet
                if (f.getType().isAssignableFrom(LinkedList.class)) {
                    value = (T) new LinkedList();
                } else if (f.getType().isAssignableFrom(ArrayList.class)) {
                    value = (T) new ArrayList();
                } else if (f.getType().isAssignableFrom(HashSet.class)) {
                    value = (T) new HashSet();
                } else if (f.getType().isAssignableFrom(TreeSet.class)) {
                    value = (T) new TreeSet();
                } else {
                    throw new IllegalAnnotationError(f.getType() + UNKNOWN_COLLECTION);
                }
            }
            ((Collection<T>) value).add(v);
        }
    }

    @Override
    protected void setValues() {
        if (value != null) {
            if (Modifier.isFinal(f.getModifiers())) {
                throw new IllegalAccessError("Cannot set final field");
            }

            try {
                f.set(bean, value);
            } catch (final IllegalAccessException e) {
                // try again
                f.setAccessible(true);
                try {
                    f.set(bean, value);
                } catch (final IllegalAccessException e2) {
                    throw new IllegalAccessError(e2.getMessage());
                }
            }
        }
    }

    @Override
    public boolean isMultiValued() {
        return true;
    }

    @Override
    public boolean isEnum() {
        return getType(f).isEnum();
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> getType(final Field f) {
        if (f.getType().isArray()) {
            return (Class<T>) f.getType().getComponentType();
        }
        // TODO: compute this correctly
        Type t = f.getGenericType();
        if (t instanceof ParameterizedType) {
            final ParameterizedType pt = (ParameterizedType) t;
            t = pt.getActualTypeArguments()[0];
            if (t instanceof Class)
                return (Class<T>) t;
        }
        return (Class<T>) Object.class;
    }

    public void clear() {
        // Null out default array values when we encounter the option or argument (so we won't end up
        // appending command-line parameters to default array values)
        if (f.getType().isArray()) {
            try {
                f.set(bean, null);
            } catch (final IllegalAccessException e) {
                // try again
                f.setAccessible(true);
                try {
                    f.set(bean, null);
                } catch (final IllegalAccessException e2) {
                    throw new IllegalAccessError(e2.getMessage());
                }
            }
        }
    }
}
