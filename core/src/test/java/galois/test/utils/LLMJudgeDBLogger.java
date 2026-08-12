package galois.test.utils;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTable;
import com.j256.ormlite.table.TableUtils;
import galois.utils.Configuration;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import speedy.model.database.dbms.DBMSDB;
import speedy.persistence.relational.AccessConfiguration;

import java.sql.SQLException;
import java.util.Date;

@Slf4j
public class LLMJudgeDBLogger {

    private static final String JDBC_URI = "jdbc:postgresql:llm_judge";
    private static final LLMJudgeDBLogger INSTANCE = new LLMJudgeDBLogger();

    public static LLMJudgeDBLogger getInstance() {
        return INSTANCE;
    }

    @Setter
    private Boolean enabled = false;
    @Setter
    private String currentDB;
    @Setter
    private String currentDataset;
    @Setter
    private String currentQuery;

    private LLMJudgeDBLogger() {
        AccessConfiguration accessConfiguration = new AccessConfiguration();
        accessConfiguration.setDriver(Configuration.getInstance().getCacheDbDriver());
        accessConfiguration.setUri(JDBC_URI);
        accessConfiguration.setSchemaName("public");
        accessConfiguration.setLogin(Configuration.getInstance().getCacheDbUser());
        accessConfiguration.setPassword(Configuration.getInstance().getCacheDbPassword());

        DBMSDB dbmsdb = new DBMSDB(accessConfiguration);
        dbmsdb.initDBMS();
    }

    // A logging failure must not abort the experiment: errors are logged and swallowed.
    public void logComparison(String metric, String stage, String actualTuple, String expectedTuple, boolean matched, Double score) {
        if (!enabled) return;

        try (DAOLLMJudge dao = new DAOLLMJudge()) {
            dao.connect();
            LLMJudgeEntry entry = new LLMJudgeEntry(
                    new Date(),
                    currentDB,
                    currentDataset,
                    currentQuery,
                    metric,
                    stage,
                    actualTuple,
                    expectedTuple,
                    matched,
                    score
            );
            dao.saveEntry(entry);
        } catch (Exception e) {
            log.error("Cannot log comparison!", e);
        }
    }

    private static final class DAOLLMJudge implements AutoCloseable {
        private ConnectionSource connectionSource;
        private Dao<LLMJudgeEntry, Long> dao;

        public void connect() throws SQLException {
            connectionSource = new JdbcConnectionSource(
                    JDBC_URI,
                    Configuration.getInstance().getCacheDbUser(),
                    Configuration.getInstance().getCacheDbPassword()
            );
            dao = DaoManager.createDao(connectionSource, LLMJudgeEntry.class);
            // On Postgres, createTableIfNotExists guards the table but not the sequence
            // generated for the id column, so it must be skipped when the table exists
            if (!dao.isTableExists()) {
                TableUtils.createTableIfNotExists(connectionSource, LLMJudgeEntry.class);
            }
        }

        public void saveEntry(LLMJudgeEntry entry) throws SQLException {
            dao.create(entry);
        }

        @Override
        public void close() throws Exception {
            if (connectionSource != null) {
                connectionSource.close();
            }
        }
    }

    @DatabaseTable(tableName = "comparison")
    @Data
    @NoArgsConstructor
    private static final class LLMJudgeEntry {
        @DatabaseField(generatedId = true)
        private long id;

        @DatabaseField(canBeNull = false, columnName = "created_at", columnDefinition = "TIMESTAMP")
        private Date createdAt;

        // The shared indexName creates a composite index: llm_judge_run_idx (db, dataset, sql_query)
        @DatabaseField(indexName = "llm_judge_run_idx", columnDefinition = "TEXT")
        private String db;

        @DatabaseField(indexName = "llm_judge_run_idx", columnDefinition = "TEXT")
        private String dataset;

        @DatabaseField(columnName = "sql_query", indexName = "llm_judge_run_idx", columnDefinition = "TEXT")
        private String query;

        @DatabaseField(canBeNull = false, columnDefinition = "TEXT")
        private String metric;

        @DatabaseField(canBeNull = false, columnDefinition = "TEXT")
        private String stage;

        @DatabaseField(canBeNull = false, columnName = "actual_tuple", columnDefinition = "TEXT")
        private String actualTuple;

        @DatabaseField(canBeNull = false, columnName = "expected_tuple", columnDefinition = "TEXT")
        private String expectedTuple;

        @DatabaseField(canBeNull = false)
        private boolean matched;

        @DatabaseField(columnDefinition = "REAL")
        private Double score;

        LLMJudgeEntry(Date createdAt, String db, String dataset, String query, String metric,
                      String stage, String actualTuple, String expectedTuple, boolean matched, Double score) {
            this.createdAt = createdAt;
            this.db = db;
            this.dataset = dataset;
            this.query = query;
            this.metric = metric;
            this.stage = stage;
            this.actualTuple = actualTuple;
            this.expectedTuple = expectedTuple;
            this.matched = matched;
            this.score = score;
        }
    }
}
