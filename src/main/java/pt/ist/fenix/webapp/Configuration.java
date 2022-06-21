package pt.ist.fenix.webapp;

import org.fenixedu.bennu.core.domain.Bennu;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public interface Configuration {

    default Properties loadProperties() {
        try (final InputStream input = Bennu.class.getClassLoader().getResourceAsStream("configuration.properties")) {
            final Properties properties = new Properties();
            if (input == null) {
                return null;
            }
            properties.load(input);
            return properties;
        } catch (final IOException ex) {
            throw new Error(ex);
        }
    }

}
