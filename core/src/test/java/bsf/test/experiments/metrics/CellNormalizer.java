package bsf.test.experiments.metrics;

import java.text.Normalizer;
import java.util.regex.*;
public class CellNormalizer {

    public String normalize(Object cell) {
        if (cell == null) {
            return "None";
        } else if (cell instanceof Boolean) {
            return cell.toString();
        } else if (cell instanceof Number) {
            double value = ((Number) cell).doubleValue();
            if (Double.isNaN(value)) {
                return "None";
            } else {
                return String.format("%.2f", value);
            }
        } else if (cell instanceof String) {
            String strCell = (String) cell;
            strCell = strCell.replaceAll(",", "");
            //Strip accent letters
            strCell = java.text.Normalizer.normalize(strCell, Normalizer.Form.NFD);
            strCell = strCell.replaceAll("[^\\p{ASCII}]", "");
            strCell = strCell.replaceAll("\\p{M}", "");
            strCell = strCell.replaceAll("'", "");
            //------
            // Patterns for million, billion, and thousand
            Pattern millionPattern = Pattern.compile("(\\d+(\\.\\d+)?)\\s*(million|m)", Pattern.CASE_INSENSITIVE);
            Pattern billionPattern = Pattern.compile("(\\d+(\\.\\d+)?)\\s*(billion|b)", Pattern.CASE_INSENSITIVE);
            Pattern thousandPattern = Pattern.compile("(\\d+(\\.\\d+)?)\\s*(thousand|k)", Pattern.CASE_INSENSITIVE);


            Matcher millionMatcher = millionPattern.matcher(strCell);
            Matcher billionMatcher = billionPattern.matcher(strCell);
            Matcher thousandMatcher = thousandPattern.matcher(strCell);

            if (millionMatcher.find()) {
                double value = Double.parseDouble(millionMatcher.group(1)) * 1_000_000;
                return String.format("%.0f", value);
            } else if (billionMatcher.find()) {
                double value = Double.parseDouble(billionMatcher.group(1)) * 1_000_000_000;
                return String.format("%.0f", value);
            } else if (thousandMatcher.find()) {
                double value = Double.parseDouble(thousandMatcher.group(1)) * 1_000;
                return String.format("%.0f", value);
            }

            if (strCell.matches("^-?\\d+(\\.\\d+)?$")) {
                double value = Double.parseDouble(strCell);
                if (strCell.contains(".")) {
                    return String.format("%.2f", value);
                } else {
                    return String.format("%.0f", value);
                }
            } else {
                return strCell.replaceAll("\n", "").trim().toLowerCase();
            }
        } else {
            // Handle other types if necessary
            return cell.toString().toLowerCase();
        }
    }

}

