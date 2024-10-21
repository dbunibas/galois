package engine.persistence.relational;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryStatManager {

    private static Logger logger = LoggerFactory.getLogger(QueryStatManager.class);

    private static QueryStatManager singleton = new QueryStatManager();
    private final DecimalFormat df = new DecimalFormat("#.##");
    private List<QueryStat> statistics = new ArrayList<QueryStat>();
    private long readTuples = 0;
    private int TOP_K_QUERIES = 0; //0 Disabled
    private int QUERY_PREVIEW_LENGHT = -1; //-1 to print the whole query
    private Date lastPrint;
    private int SEC = -1; //-1 disabled

    private QueryStatManager() {
    }

    public static QueryStatManager getInstance() {
        return singleton;
    }

    public void addQuery(String query, long executionTime) {
        if (!logger.isDebugEnabled()) return;
        statistics.add(new QueryStat(query, executionTime));
        printStatisticsAfterSeconds();
    }

    private void printStatisticsAfterSeconds() {
        if (!logger.isDebugEnabled()) return;
        if (SEC < 0) return;
        Date nowDate = new Date();
        if (lastPrint == null || (nowDate.getTime() - lastPrint.getTime() > SEC * 1000)) {
            lastPrint = nowDate;
            printStatistics("* RUNNING *\n");
        }
    }

    public void printStatistics() {
        printStatistics("");
    }

    public void addReadTuple() {
        if (!logger.isDebugEnabled()) return;
        readTuples++;
    }

    public void printStatistics(String prefix) {
        if (!logger.isDebugEnabled()) return;
        Collections.sort(statistics);
        StringBuilder sb = new StringBuilder();
        sb.append(prefix);
        sb.append("--------------------------").append("\n");
        sb.append("Number of queries:\t").append(statistics.size()).append("\n");
        sb.append("Total execution time:\t").append(getTotalExecutionTime()).append("\n");
        int select = 0;
        int insert = 0;
        int update = 0;
        int delete = 0;
        int create = 0;
        long selectTime = 0;
        long insertTime = 0;
        long updateTime = 0;
        long deleteTime = 0;
        long createTime = 0;
        for (QueryStat queryStat : statistics) {
            String query = queryStat.getQuery();
            if (query.startsWith("SELECT")) {
                select++;
                selectTime += queryStat.getExecutionTime();
            }
            if (query.startsWith("INSERT")) {
                insert++;
                insertTime += queryStat.getExecutionTime();
            }
            if (query.startsWith("UPDATE")) {
                update++;
                updateTime += queryStat.getExecutionTime();
            }
            if (query.startsWith("DELETE")) {
                delete++;
                deleteTime += queryStat.getExecutionTime();
            }
            if (query.startsWith("CREATE")) {
                create++;
                createTime += queryStat.getExecutionTime();
            }
        }
        sb.append("Number of queries:").append("\n");
        sb.append("\t").append("Create").append("\t").append(create).append("\n");
        sb.append("\t").append("Select").append("\t").append(select).append("\n");
        sb.append("\t").append("Insert").append("\t").append(insert).append("\n");
        sb.append("\t").append("Update").append("\t").append(update).append("\n");
        sb.append("\t").append("Delete").append("\t").append(delete).append("\n");
        sb.append("Time for queries:").append("\n");
        sb.append("\t").append("Create").append("\t").append(createTime);
        if (create > 0) {
            sb.append("\t(").append(df.format((double) createTime / create)).append(" avg)");
        }
        sb.append("\n");
        sb.append("\t").append("Select").append("\t").append(selectTime);
        if (select > 0) {
            sb.append("\t(").append(df.format((double) selectTime / select)).append(" avg)");
        }
        sb.append("\n");
        sb.append("\t").append("Insert").append("\t").append(insertTime);
        if (insert > 0) {
            sb.append("\t(").append(df.format((double) insertTime / insert)).append(" avg)");
        }
        sb.append("\n");
        sb.append("\t").append("Update").append("\t").append(updateTime);
        if (update > 0) {
            sb.append("\t(").append(df.format((double) updateTime / update)).append(" avg)");
        }
        sb.append("\n");
        sb.append("\t").append("Delete").append("\t").append(deleteTime);
        if (delete > 0) {
            sb.append("\t(").append(df.format((double) deleteTime / delete)).append(" avg)");
        }
        sb.append("\n");
        sb.append("Read tuples:\t").append(readTuples).append("\n");
        if (TOP_K_QUERIES > 0) sb.append("Most expensive queries:").append("\n");
        for (int i = 0; i < Math.min(TOP_K_QUERIES, statistics.size()); i++) {
            QueryStat query = statistics.get(i);
            sb.append("\t").append(query.getExecutionTime()).append("\t").append(printQuery(query.getQuery())).append("\n");
        }
        sb.append("--------------------------").append("\n");
        logger.debug(sb.toString());
    }

    private long getTotalExecutionTime() {
        long total = 0;
        for (QueryStat queryStat : statistics) {
            total += queryStat.getExecutionTime();
        }
        return total;
    }

    private String printQuery(String query) {
        String singleLine = query.replaceAll("\n", " ");
        if (QUERY_PREVIEW_LENGHT != -1) {
            singleLine = singleLine.substring(0, Math.min(QUERY_PREVIEW_LENGHT, singleLine.length()));
        }
        return singleLine;
    }

    public void resetStatistics() {
        statistics.clear();
    }
}

class QueryStat implements Comparable<QueryStat> {

    private String query;
    private long executionTime;

    public QueryStat(String query, long executionTime) {
        this.query = query;
        this.executionTime = executionTime;
    }

    public String getQuery() {
        return query;
    }

    public long getExecutionTime() {
        return executionTime;
    }

    public int compareTo(QueryStat o) {
        if (this.executionTime > o.executionTime) {
            return -1;
        }
        if (this.executionTime < o.executionTime) {
            return 1;
        } else {
            return 0;
        }
    }
}
