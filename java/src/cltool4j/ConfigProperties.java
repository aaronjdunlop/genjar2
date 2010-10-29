package cltool4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;

/**
 * Represents configuration properties for a command-line tool. Generally accessed at runtime via the
 * singleton subclass {@link GlobalConfigProperties}. This implementation allows programmatic configuration as
 * well (e.g., for unit testing).
 * 
 * 
 * @author Aaron Dunlop
 * @since Oct 2010
 * 
 *        $Id$
 */
public class ConfigProperties extends Properties {

    public ConfigProperties() {
    }

    /**
     * Returns the value of the specified property
     * 
     * @param key
     * @return the value of the specified property
     * @throws InvalidConfigurationException if the property is not set or cannot be parsed as an integer
     */
    @Override
    public String getProperty(final String key) {
        final String value = super.getProperty(key);
        if (value == null) {
            throw new InvalidConfigurationException(key);
        }
        return value;
    }

    /**
     * Parses the specified property as an integer.
     * 
     * @param key
     * @return the specified property as an integer
     * @throws InvalidConfigurationException if the property is not set or cannot be parsed as an integer
     */
    public int getIntProperty(final String key) {
        try {
            return Integer.parseInt(getProperty(key));
        } catch (NumberFormatException e) {
            throw new InvalidConfigurationException(key, e.getMessage());
        }
    }

    /**
     * Parses the specified property as an float.
     * 
     * @param key
     * @return the specified property as an float
     * @throws InvalidConfigurationException if the property is not set or cannot be parsed as an float
     */
    public float getFloatProperty(final String key) {
        try {
            return Float.parseFloat(getProperty(key));
        } catch (NumberFormatException e) {
            throw new InvalidConfigurationException(key, e.getMessage());
        }
    }

    /**
     * Parses the specified property as an boolean.
     * 
     * @param key
     * @return the specified property as an boolean
     * @throws InvalidConfigurationException if the property is not set or cannot be parsed as an boolean
     */
    public boolean getBooleanProperty(final String key) {
        try {
            return Boolean.parseBoolean(getProperty(key));
        } catch (NumberFormatException e) {
            throw new InvalidConfigurationException(key, e.getMessage());
        }
    }

    /**
     * Merges the provided properties into global property storage, overwriting any conflicting keys (that is,
     * properties set in the provided {@link Properties} instance override those in the current global
     * storage).
     * 
     * @param newProperties
     */
    public void mergeOver(Properties newProperties) {
        for (final Object key : newProperties.keySet()) {
            setProperty((String) key, newProperties.getProperty((String) key));
        }
    }

    /**
     * Merges the provided properties into global property storage, skipping any conflicting keys (that is,
     * existing properties override properties set in the provided {@link Properties} instance).
     * 
     * @param newProperties
     */
    public void mergeUnder(Properties newProperties) {
        for (final Object key : newProperties.keySet()) {
            if (!containsKey(key)) {
                setProperty((String) key, newProperties.getProperty((String) key));
            }
        }
    }

    @Override
    public String toString() {
        ArrayList<String> stringProps = new ArrayList<String>();
        for (Object key : keySet()) {
            stringProps.add((String) key + "=" + getProperty((String) key) + '\n');
        }
        Collections.sort(stringProps);

        StringBuilder sb = new StringBuilder(128);
        for (String property : stringProps) {
            sb.append(property);
        }
        // Remove final line feed
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    /**
     * Thrown if an unknown property is requested from {@link ConfigProperties}. Clients of
     * {@link ConfigProperties} generally expect any required configuration parameters to be specified, and
     * {@link BaseCommandlineTool} handles the exception case if they are not.
     */
    public static class InvalidConfigurationException extends RuntimeException {

        public InvalidConfigurationException(final String key) {
            super("No value found for configuration option " + key);
        }

        public InvalidConfigurationException(final String key, final String message) {
            super("Invalid configuration option " + key + " : " + message);
        }
    }

}
