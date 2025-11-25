package galois.test.evaluation;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import speedy.persistence.Types;
import speedy.persistence.file.CSVFile;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

@Slf4j
public class CSVFileHandler {
    public static Map<String, CSVFile> createDataFilesInExperimentFolder(String experimentFolder, SchemaDatabase schema) {
        Map<String, CSVFile> filesToAdd = new HashMap<>();

        String dataFolder = experimentFolder + "/data";
        URL resource = CSVFileHandler.class.getResource(dataFolder);
        if (resource == null) throw new IllegalArgumentException("Unable to locate folder " + dataFolder);

        File[] files = new File(resource.getPath()).listFiles((dir, name) -> name.endsWith(".csv") && !name.contains("_speedy"));
        if (files == null) throw new IllegalArgumentException("Incorrect folder " + dataFolder);

        Map<String, File> tableCSVFiles = new HashMap<>();
        for (File file : files) {
            String tableName = file.getName().replace(".csv", "").toLowerCase();
            log.debug("Table: {} in file {}", tableName, file);
            tableCSVFiles.put(tableName, file);
        }

        for (SchemaTable table : schema.getTables()) {
            File file = tableCSVFiles.get(table.getName());
            log.debug("Search for table: {} - Found file: {}", table, file);
            if (file == null)
                throw new IllegalArgumentException("Unknown csv for table " + table.getName() + " (existing files: " + tableCSVFiles.values() + ")");

            String speedyFileName = file.getName().replace(".csv", "") + "_speedy.csv";
            File speedyFile = new File(resource.getPath() + File.separator + speedyFileName);

            String textDelim;
            try {
                FileUtils.copyFile(file, speedyFile);
                textDelim = replaceHeadersWithTypes(speedyFile, table.getAttributes());
                if (textDelim != null) log.debug("Text Delim: {}", textDelim);
            } catch (IOException ioe) {
                log.error("Unable to duplicate file: {} to {}", file, speedyFile, ioe);
                throw new IllegalStateException(ioe);
            }

            log.debug("File to import: {}", speedyFile.getAbsolutePath());
            CSVFile fileToImport = new CSVFile(speedyFile.getAbsolutePath());
            fileToImport.setHasHeader(true);
            fileToImport.setSeparator(',');
            if (textDelim != null) {
                fileToImport.setQuoteCharacter(textDelim.charAt(0));
            }

            filesToAdd.put(table.getName(), fileToImport);
        }

        return filesToAdd;
    }

    private static String replaceHeadersWithTypes(File speedyFile, List<SchemaAttribute> attributes) throws IOException {
        List<String> lines = FileUtils.readLines(speedyFile, "utf-8");

        String headers = lines.getFirst();
        StringTokenizer tokenizer = new StringTokenizer(headers, ",");

        String textDelim = getTextDelim(headers);
        String updatedHeaders = updateHeaders(tokenizer, attributes, textDelim);
        log.warn("New headers: {}", updatedHeaders);
        lines.removeFirst();
        lines.addFirst(updatedHeaders);

        FileUtils.writeLines(speedyFile, lines);
        return textDelim; // TODO refactor
    }

    private static String updateHeaders(StringTokenizer tokenizer, List<SchemaAttribute> attributes, String textDelim) {
        Map<String, String> attributesWithType = new HashMap<>();
        for (SchemaAttribute attribute : attributes) {
            attributesWithType.put(attribute.getName(), attribute.getSpeedyAttributeType());
        }

        StringBuilder headersWithType = new StringBuilder();
        while (tokenizer.hasMoreTokens()) {
            String attributeName = tokenizer.nextToken().trim();
            if (textDelim != null) attributeName = attributeName.replace(textDelim, "");

            String type = attributesWithType.get(attributeName);
            if (type == null || type.equals(Types.STRING)) {
                headersWithType.append(attributeName).append(",");
            } else {
                if (textDelim == null) {
                    headersWithType.append(attributeName).append("(").append(type).append(")").append(",");
                } else {
                    headersWithType.append(textDelim).append(attributeName).append("(").append(type).append(")").append(textDelim).append(",");
                }
            }
        }

        return headersWithType.substring(0, headersWithType.length() - 1);
    }

    private static String getTextDelim(String headers) {
        if (headers.contains("\"")) return "\"";
        if (headers.contains("'")) return "'";
        return null;
    }
}
