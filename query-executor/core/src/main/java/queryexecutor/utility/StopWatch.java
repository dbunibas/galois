package queryexecutor.utility;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StopWatch {

    private static final StopWatch singleton = new StopWatch();

    public static StopWatch getSharedInstance() {
        return singleton;
    }

    public StopWatch() {
        this(true);
    }

    public StopWatch(boolean start) {
        if (start) {
            start();
        }
    }

    private final static Logger logger = LoggerFactory.getLogger(StopWatch.class);
    private Map<String, Long> startTimes = new HashMap<String, Long>();
    private Map<String, Long> totalTimes = new HashMap<String, Long>();
    private Long singleStopWatchTimer = null;

    public void start(String key) {
        if (startTimes.containsKey(key)) {
            logger.error("Timer " + key + " is running");
            return;
        }
        startTimes.put(key, new Date().getTime());
    }

    public long stop(String key) {
        if (!startTimes.containsKey(key)) {
            logger.error("Timer " + key + " not started");
            return 0;
        }
        long totalTime = new Date().getTime() - startTimes.get(key);
        totalTimes.put(key, totalTime);
        startTimes.remove(key);
        return totalTime;
    }

    public long intermediate(String key) {
        if (!startTimes.containsKey(key)) {
            logger.error("Timer " + key + " not started");
            return 0;
        }
        return new Date().getTime() - startTimes.get(key);
    }

    public long getTotalTime(String key) {
        if (!totalTimes.containsKey(key)) {
            logger.error("Timer " + key + " not completed");
            return 0;
        }
        return totalTimes.get(key);
    }

    public void start() {
        if (singleStopWatchTimer != null) {
            logger.error("Timer is running");
            return;
        }
        singleStopWatchTimer = new Date().getTime();
    }

    public long stop() {
        if (singleStopWatchTimer == null) {
            logger.error("Timer not started");
            return 0;
        }
        long totalTime = new Date().getTime() - singleStopWatchTimer;
        singleStopWatchTimer = null;
        return totalTime;
    }

    public long intermediate() {
        if (singleStopWatchTimer == null) {
            logger.error("Timer not started");
            return 0;
        }
        return new Date().getTime() - singleStopWatchTimer;
    }
}
