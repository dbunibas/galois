package galois.utils;


import galois.exception.GaloisRuntimeException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Configuration {
    private static final Configuration C = new Configuration();

    private final Properties props;

    private Configuration() {
        try (InputStream s = getClass().getClassLoader().getResourceAsStream("configuration.properties")) {
            props = new Properties();
            props.load(s);
        } catch (NullPointerException | IOException ex) {
            throw new GaloisRuntimeException("Cannot load configuration file!");
        }
    }

    public static Configuration getInstance() {
        return C;
    }

    public String getTogetheraiApiKey() {
        return props.getProperty("togetherai.api-key");
    }

    public String getTogetheraiModel() {
        return props.getProperty("togetherai.model");
    }

    public Boolean getTogetheraiReasoningEnabled() {
        String prop = props.getProperty("togetherai.reasoning-enabled");
        if (prop == null || prop.isBlank()) return null;
        return Boolean.parseBoolean(prop);
    }

    public int getTogetheraiWaitTimeMs() {
        String waitTime = props.getProperty("togetherai.wait-time-ms");
        try {
            int parsedValue = Integer.parseInt(waitTime);
            if (parsedValue <= 0) {
                throw new GaloisRuntimeException("Wait time ms must be greater than 0.");
            }
            return parsedValue;
        } catch (NumberFormatException ex) {
            throw new GaloisRuntimeException(ex);
        }
    }

    public String getOpenaiApiKey() {
        return props.getProperty("openai.api-key");
    }

    public String getOpenaiModelName() {
        return props.getProperty("openai.model-name");
    }

    public String getLocalApiKey() {
        return props.getProperty("local.api-key");
    }

    public String getLocalModelName() {
        return props.getProperty("local.model-name");
    }

    public String getLocalBaseUrl() {
        return props.getProperty("local.base-url");
    }

    public Double getLocalTemperature() {
        String prop = props.getProperty("local.temperature");
        if (prop == null || prop.isBlank()) return null;
        try {
            return Double.parseDouble(prop.trim());
        } catch (NumberFormatException ex) {
            throw new GaloisRuntimeException("Local temperature is not a number: " + prop);
        }
    }

    public Integer getLocalMaxTokens() {
        String prop = props.getProperty("local.max-tokens");
        if (prop == null || prop.isBlank()) return null;
        try {
            return Integer.parseInt(prop.trim());
        } catch (NumberFormatException ex) {
            throw new GaloisRuntimeException("Local max tokens is not a number: " + prop);
        }
    }

    public Boolean getLocalReasoningEnabled() {
        String prop = props.getProperty("local.reasoning-enabled");
        if (prop == null || prop.isBlank()) return null;
        return Boolean.parseBoolean(prop);
    }

    public Boolean getLocalStream() {
        String prop = props.getProperty("local.stream");
        if (prop == null || prop.isBlank()) return null;
        return Boolean.parseBoolean(prop);
    }

    public String getLocalReasoningEffort() {
        String prop = props.getProperty("local.reasoning-effort");
        if (prop == null || prop.isBlank()) return null;
        return prop.trim();
    }

    public String getOllamaUrl() {
        return props.getProperty("ollama.url");
    }

    public String getOllamaModel() {
        return props.getProperty("ollama.model");
    }

    public String getChromaUrl() {
        return props.getProperty("chroma.url");
    }

    public String getLLMProvider() {
        return props.getProperty("llm-provider");
    }

    public boolean isCacheEnabled() {
        String enabled = props.getProperty("cache.enabled");
        return Boolean.parseBoolean(enabled);
    }

    public String getCacheAbsolutePath() {
        String path = props.getProperty("cache.path");
        if (isCacheEnabled() && path.isBlank()) {
            throw new GaloisRuntimeException("Cache path is undefined!");
        }
        return path;
    }

    public String getCacheDbDriver() {
        String driver = props.getProperty("cache.dbDriver");
        if (isCacheEnabled() && driver.isBlank()) {
            throw new GaloisRuntimeException("Cache driver is undefined!");
        }
        return driver;
    }

    public String getCacheDbUri() {
        String uri = props.getProperty("cache.dbUri");
        if (isCacheEnabled() && uri.isBlank()) {
            throw new GaloisRuntimeException("Cache uri is undefined!");
        }
        return uri;
    }

    public String getCacheDbUser() {
        String user = props.getProperty("cache.dbUser");
        if (isCacheEnabled() && user.isBlank()) {
            throw new GaloisRuntimeException("Cache user is undefined!");
        }
        return user;
    }

    public String getCacheDbPassword() {
        String password = props.getProperty("cache.dbPassword");
        if (isCacheEnabled() && password.isBlank()) {
            throw new GaloisRuntimeException("Cache password is undefined!");
        }
        return password;
    }

    public boolean exportConfidence() {
        return false;
    }

    public String getResultsAbsolutePath() {
        return props.getProperty("export.results-path");
    }

    public String getExportExcelAbsolutePath() {
        return props.getProperty("export.excel-path");
    }
}
