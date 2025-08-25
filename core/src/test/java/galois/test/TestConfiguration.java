package galois.test;

import galois.utils.Configuration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestConfiguration {
    @Test
    public void testNoMissingPropertiesInConfiguration() {
        Assertions.assertNotNull(Configuration.getInstance().getTogetheraiApiKey(), "Missing TogetheraiApiKey!");
        Assertions.assertNotNull(Configuration.getInstance().getTogetheraiModel(), "Missing TogetheraiModel!");
        Assertions.assertTrue(Configuration.getInstance().getTogetheraiWaitTimeMs() > 0, "Missing TogetheraiWaitTimeMs!");
        Assertions.assertNotNull(Configuration.getInstance().getOpenaiApiKey(), "Missing OpenaiApiKey!");
        Assertions.assertNotNull(Configuration.getInstance().getOpenaiModelName(), "Missing OpenaiModelName!");
        Assertions.assertNotNull(Configuration.getInstance().getOllamaModel(), "Missing OllamaModel!");
        Assertions.assertNotNull(Configuration.getInstance().getLLMProvider(), "Missing LLMProvider!");
        Assertions.assertNotNull(Configuration.getInstance().getExportExcelAbsolutePath(), "Missing ExportExcelPath!");
    }
}
