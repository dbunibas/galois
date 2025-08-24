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

    public String getOllamaUrl() {
        return props.getProperty("ollama.url");
    }

    public String getOllamaModel() {
        return props.getProperty("ollama.model");
    }

    public String getChromaUrl() {
        return props.getProperty("chroma.url");
    }

    public String getLLMModel() {
        return props.getProperty("llm-model");
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

    public String getResultsAbsolutePath() {
        return props.getProperty("export.results-path");
    }

    public String getExportExcelAbsolutePath() {
        return props.getProperty("export.excel-path");
    }
}
