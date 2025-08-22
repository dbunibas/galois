package galois.test.experiments.run;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import galois.Constants;
import galois.llm.models.TogetherAIModel;
import galois.llm.models.togetherai.TogetherAIConstants;
import galois.utils.Configuration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.List;

@Slf4j
public class TestRAGFortune {

    private static final String EXP_NAME = "2024Fortune1000Companies";
    public static final String EXPORT_PATH = "/Users/donatello/Projects/research/galois/rag/" + EXP_NAME + "/";

    @Test
    public void testGenerateDocuments() throws IOException {
        new File(EXPORT_PATH).getAbsoluteFile().mkdirs();
        TogetherAIModel toghetherAiModel = new TogetherAIModel(Configuration.getInstance().getTogetheraiApiKey(), TogetherAIConstants.MODEL_LLAMA3_1_70B, true);
        toghetherAiModel.setTemperature(0.7);
        InputStream csvDataset = TestRAG.class.getResourceAsStream("/rag/" + EXP_NAME + "/fortune1000_2024.csv");
        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = CsvSchema.emptySchema().
                withColumnSeparator(',')
                .withQuoteChar('"');
        MappingIterator<List<String>> it = mapper
                .readerForListOf(String.class)
                .with(schema)
                .with(CsvParser.Feature.WRAP_AS_ARRAY)
                .readValues(csvDataset);
        List<String> headers = it.next();
        log.info("Headers: {}", headers);
        List<String> description = it.next();
        log.info("Description: {}", description);
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMinimumIntegerDigits(4);
        nf.setGroupingUsed(false);
        int index = 0;
        while (it.hasNext()) {
            index++;
            if (index > 500) continue;
            StringBuilder prompt = new StringBuilder();
            prompt.append("""
                    Generate a document for a financial journal that incorporates all the following information about a company.
                    The document should be written in a detailed, professional, narrative style, avoiding the use of tables and bullet points.
                    The data is sourced from the Fortune 2024 ranking.
                    """);
            List<String> companyData = it.next();
            for (int i = 0; i < headers.size(); i++) {
                prompt.append("# ").append(headers.get(i)).append(" (").append(description.get(i)).append("): ").append(companyData.get(i)).append("\n");
            }
            String response = toghetherAiModel.generate(prompt.toString());
//            String response = "TEST";
            String reportFile = EXPORT_PATH + nf.format(Integer.parseInt(companyData.get(0))) + "-" + companyData.get(1).replace("/", "") + ".MD";
            IOUtils.write(response, new FileOutputStream(reportFile));
            log.info(prompt.toString());
        }
    }
}
