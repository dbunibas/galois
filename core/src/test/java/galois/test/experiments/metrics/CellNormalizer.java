package galois.test.experiments.metrics;

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
            return cell.toString();
        }
    }

}

