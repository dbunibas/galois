package queryexecutor.test.utility;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import queryexecutor.persistence.relational.QueryStatManager;
import queryexecutor.utility.Size;
import queryexecutor.utility.QueryExecutorUtility;

public class TestResults {

    private static Logger logger = LoggerFactory.getLogger(TestResults.class);
    private static Map<TestId, Long> timeResults = new HashMap<TestId, Long>();

    public static void appendRow(StringBuilder sb, Object... args) {
        for (Object arg : args) {
            sb.append(arg).append("\t");
        }
        QueryExecutorUtility.removeChars("\t".length(), sb);
        sb.append("\n");
    }

    private static String getResultDir() {
        return System.getProperty("user.home") + "/Dropbox/Temp/QueryExecutorExp/";
    }

    public static void addTimeResult(Size size, String group, long time) {
        timeResults.put(new TestId(size, group), time);
    }

    public static void addTimeResult(String size, String group, long time) {
        timeResults.put(new TestId(size, group), time);
    }

    public static void printResults(String testName) {
        String result = getResultString(testName);
        if (logger.isDebugEnabled()) logger.debug("\n" + result);
        writeResults(testName, result);
    }

    private static String getResultString(String testName) {
        StringBuilder result = new StringBuilder();
        appendRow(result, "------------------------------------------------");
        appendRow(result, " Execution Times ", testName);
        appendRow(result, "------------------------------------------------");
        appendRow(result, " Exp", "Times (ms)");
        appendRow(result, "------------------------------------------------");
        for (TestId testId : sortTestIds(timeResults.keySet())) {
            appendRow(result, testId.toString(), timeResults.get(testId));
        }
        return result.toString();
    }

    private static void writeResults(String testName, String result) {
        Writer out = null;
        try {
            String path = getResultDir() + testName + "_" + new Date().getTime() + ".txt";
            if (logger.isDebugEnabled()) logger.debug("Writing results in " + path);
            File outFile = new File(path);
            outFile.getParentFile().mkdirs();
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8"));
            out.write(DateFormat.getDateTimeInstance().format(new Date()) + "\n");
            out.write(result);
        } catch (Exception ex) {
            logger.error("Unable to write results to string. " + ex);
            ex.printStackTrace();
        } finally {
            try {
                if (out != null) out.close();
            } catch (IOException ex) {
            }
        }
    }

    public static void resetResults() {
        timeResults.clear();
    }

    public static void resetStats() {
        QueryStatManager.getInstance().resetStatistics();
    }

    public static void printStats(String prefix) {
        QueryStatManager.getInstance().printStatistics(prefix);
    }

    private static List<TestId> sortTestIds(Set<TestId> keySet) {
        List<TestId> sortedList = new ArrayList<TestId>(keySet);
        Collections.sort(sortedList);
        return sortedList;
    }
}
