package galois.test;

import galois.test.utils.LLMJudgeDBLogger;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
public class TestLLMJudgeDBLogger {
    @Test
    public void testEnabledFalse() {
        LLMJudgeDBLogger.getInstance().setEnabled(false);
        LLMJudgeDBLogger.getInstance().logComparison("TEST", "TEST", "actual", "expected", false, null);
    }

    @Test
    public void testSimpleLogging() {
        LLMJudgeDBLogger.getInstance().setEnabled(true);
        LLMJudgeDBLogger.getInstance().logComparison("TEST", "TEST", "actual", "expected", false, null);

        LLMJudgeDBLogger.getInstance().setCurrentDataset("dbID");
        LLMJudgeDBLogger.getInstance().setCurrentDB("dataset");
        LLMJudgeDBLogger.getInstance().setCurrentQuery("query");
        LLMJudgeDBLogger.getInstance().logComparison("TEST", "TEST", "actual", "expected", true, null);

        LLMJudgeDBLogger.getInstance().setEnabled(false);
        LLMJudgeDBLogger.getInstance().logComparison("TEST", "TEST", "actual", "expected", true, null);
    }
}
