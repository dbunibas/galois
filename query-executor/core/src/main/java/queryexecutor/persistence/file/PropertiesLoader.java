package queryexecutor.persistence.file;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import queryexecutor.exceptions.DAOException;

public class PropertiesLoader {

    private static final Logger logger = LoggerFactory.getLogger(PropertiesLoader.class);

    public static Properties loadProperties(String propertiesFile) throws DAOException {
        InputStream inputStream = null;
        Properties properties = null;
        try {
            inputStream = new FileInputStream(propertiesFile);
            properties = new Properties();
            properties.load(inputStream);
        } catch (IOException iOException) {
            logger.error("Error: " + iOException.getMessage());
            throw new DAOException(iOException.getMessage());
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ex) {
                }
            }
        }
        return properties;
    }

}
