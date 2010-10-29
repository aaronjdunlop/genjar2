package cltool4j.args4j;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks a field or setter that receives a command line switch value.
 * 
 * <p>
 * This annotation can be placed on a field of type T or the method of the form
 * <tt>void <i>methodName</i>(T value)</tt>.
 * 
 * <p>
 * The behavior of the annotation differs depending on the type of the field or the parameter of the method.
 * 
 * <h2>Boolean Option</h2>
 * <p>
 * When T is boolean , it represents a boolean option that takes the form of "-OPT". When this option is set,
 * the property will be set to true.
 * 
 * <h2>String Option</h2>
 * <p>
 * When T is {@link String}, it represents an option that takes one operand. The value of the operand is set
 * to the property.
 * 
 * <h2>Enum Option</h2>
 * <p>
 * When T is derived from {@link Enum}, it represents an option that takes an operand, which must be one of
 * the enum constants. The comparion between the operand and the enum constant name is case insensitive. See
 * also {@link EnumAliasMap}.
 * <p>
 * 
 * 
 * <h2>File Switch</h2>
 * <p>
 * When T is a {@link File}, it represents an option that takes a file/directory name as an operand.
 * 
 * @author Kohsuke Kawaguchi
 * @author Aaron Dunlop
 */
@Retention(RUNTIME)
@Target({ FIELD, METHOD, PARAMETER })
public @interface Option {

    /**
     * @return The name of the option; e.g. "-foo" or "-bar".
     */
    String name();

    /**
     * @return Aliases for the options, such as "--long-option-name".
     */
    String[] aliases() default {};

    /**
     * Help string used to display the usage screen.
     * @return Human-readable usage description
     */
    String usage() default "";

    /**
     * When the option takes an operand, the usage screen will show something like this:
     * 
     * <pre>
     * -x FOO  : blah blah blah
     * </pre>
     * 
     * You can replace the 'FOO' token by using this parameter.
     * 
     * @return A human-readable label for the option's parameter
     */
    String metaVar() default "";

    /**
     * Indicates that the option is mandatory. {@link CmdLineParser#parseArguments(String...)} will throw a
     * {@link CmdLineException} if a required option is not present.
     * 
     * @see {@link #ignoreRequired()}
     * @return True if the option is required
     */
    boolean required() default false;

    /**
     * Specify the {@link ArgumentParser} that processes the command line arguments. By default, the
     * {@link ArgumentParser} class will be inferred from the type of field or method annotated; if this
     * annotation element is included, it overrides the default parser inference.
     * 
     * @return {@link ArgumentParser} class
     */
    @SuppressWarnings("rawtypes")
    Class<? extends ArgumentParser> parser() default ArgumentParser.class;

    /**
     * Whether the option is multi-valued. For mappings to List<...>, this defaults to true, otherwise false
     */
    boolean multiValued() default false;

    /**
     * @return Separator to use for multi-valued parameters (e.g. -f 1,2,3). Use of {@link #separator()}
     *         implies {@link #multiValued()} as well.
     */
    String separator() default "";

    /**
     * @return Annotations which must be present on the class to 'activate' this option. e.g. an option
     *         declared in a superclass which only applies to subclasses with a particular annotation.
     */
    Class<? extends Annotation>[] requiredAnnotations() default {};

    /**
     * @return True if this option should be hidden in standard usage display
     */
    boolean hidden() default false;

    /**
     * @return True if this option indicates a help request from the user. If true, required argument checks
     *         are suppressed, to avoid printing out an error instead of the desired usage information.
     */
    boolean ignoreRequired() default false;

    /**
     * @return The name of a resource which must be present in classpath for this option to be valid. If not
     *         present, the option will be ignored and its description will be omitted from help information.
     */
    String requiredResource() default "";
}
