package queryexecutor.persistence.file.operators;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import queryexecutor.exceptions.DAOException;
import queryexecutor.persistence.DAOUtility;

public class DiffCSV {

    private static Logger logger = LoggerFactory.getLogger(DiffCSV.class);
    private String separator = ",";
    private DAOUtility utility = new DAOUtility();

    public void compare(String sourceFile, String destFile, String resultFile) {
        compare(sourceFile, destFile, true, resultFile, false);
    }

    public void compare(String sourceFile, String destFile, String resultFile, boolean useSecondHeader) {
        compare(sourceFile, destFile, true, resultFile, useSecondHeader);
    }

    public void compare(String sourceFile, String destFile, boolean destHasHeader, String resultFile, boolean useSecondHeader) {
        assert (sourceFile != null && destFile != null && resultFile != null);
        BufferedReader source = null;
        BufferedReader dest = null;
        FileWriter result = null;
        try {
            source = utility.getBufferedReader(sourceFile);
            dest = utility.getBufferedReader(destFile);
            List<String> srcHeader = readLine(source);
            List<String> destHeader = null;
            if (destHasHeader) {
                destHeader = readLine(dest);
//                if (!equals(srcHeader, destHeader)) {
                if (srcHeader.size() != destHeader.size()) {
                    throw new DAOException("Source and Target headers must be equal.\n\tSource header: " + srcHeader + "\n\tTarget header: " + destHeader);
                }
            }
            result = new FileWriter(resultFile);
            List<String> header = srcHeader;
            if (destHasHeader && useSecondHeader) {
                header = destHeader;
            }
            compareReaders(header, source, dest, result);
        } catch (Exception e) {
            throw new DAOException(e);
        } finally {
            try {
                if (source != null) source.close();
                if (dest != null) dest.close();
                if (result != null) result.close();
            } catch (IOException ex) {
            }
        }
    }

    private void compareReaders(List<String> header, BufferedReader source, BufferedReader dest, FileWriter result) throws IOException {
        int row = 0;
        boolean areEquals = true;
        while (true) {
            row++;
            List<String> srcRow = readLine(source);
            List<String> destRow = readLine(dest);
            if (srcRow == null && destRow == null) {
                if (areEquals) {
                    logger.warn("No differences");
                }
                return;
            }
            if (srcRow == null || destRow == null) {
                throw new DAOException("Source and Target files must have the same number of rows");
            }
            if (srcRow.size() != header.size() || destRow.size() != header.size()) {
                throw new DAOException("Source and Target files must have the same number of columns\n\tSource: " + srcRow + "\n\tTarget: " + destRow);
            }
            for (int col = 0; col < header.size(); col++) {
                String attribute = header.get(col);
                String srcValue = srcRow.get(col);
                String destValue = destRow.get(col);
                if (srcValue.equals(destValue)) {
                    continue;
                }
                areEquals = false;
                result.write(row + "." + attribute);
                result.write(separator);
                result.write(destValue);
                result.write(separator);
                result.write(srcValue);
                result.write("\n");
            }
        }
    }

    private List<String> readLine(BufferedReader reader) throws IOException {
        String line = reader.readLine();
        if (line == null || line.trim().isEmpty()) return null;
        List<String> result = new ArrayList<String>();
        for (String token : line.split(separator, -1)) {
            String attributeWithType = token.trim();
            String attribute = (attributeWithType.contains("(") ? attributeWithType.substring(0, attributeWithType.indexOf("(")) : attributeWithType);
            result.add(attribute.trim());
        }
        return result;
    }

    public String getSeparator() {
        return separator;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }

}
