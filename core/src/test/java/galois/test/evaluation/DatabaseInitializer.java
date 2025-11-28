package galois.test.evaluation;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import speedy.SpeedyConstants;
import speedy.model.algebra.operators.IBatchInsert;
import speedy.model.algebra.operators.ICreateTable;
import speedy.model.algebra.operators.sql.SQLBatchInsert;
import speedy.model.algebra.operators.sql.SQLCreateTable;
import speedy.model.database.*;
import speedy.model.database.dbms.DBMSDB;
import speedy.model.database.mainmemory.datasource.IntegerOIDGenerator;
import speedy.persistence.relational.AccessConfiguration;
import speedy.persistence.relational.QueryManager;
import speedy.utility.DBMSUtility;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static galois.test.evaluation.DatabaseFactory.connectToPostgres;

@Slf4j
public class DatabaseInitializer {
    public static void initializeDatabaseFromExperimentFolder(String experimentFolder, IDatabase database, SchemaDatabase schema) throws IOException {
        initializeDatabaseFromExperimentFolder(experimentFolder, database, schema, false);
    }

    public static void initializeDatabaseFromExperimentFolder(String experimentFolder, IDatabase database, SchemaDatabase schema, Boolean dropDatabase) throws IOException {
        if (!(database instanceof DBMSDB dbmsdb)) {
            log.info("Database has already been initialized!");
            return;
        }

        boolean dbExists = DBMSUtility.isDBExists(dbmsdb.getAccessConfiguration());
        if (dbExists && !dropDatabase) {
            log.info("Database has already been initialized! You can force the initialization by setting the dropDatabase flag.");
            return;
        }

        if (dbExists) {
            boolean drop = dropDatabase(dbmsdb.getAccessConfiguration(), schema);
            if (!drop) {
                log.warn("Cannot drop database {}!", schema.getDbName());
                return;
            }
        }

        // Create schema
        createSchemaAndTables(dbmsdb, schema);
        // Populate tables from csv (if available)
        populateTables(experimentFolder, dbmsdb);
        // Close database connection
        closeDatabaseConnection(dbmsdb.getAccessConfiguration());
    }

    private static void createSchemaAndTables(DBMSDB database, SchemaDatabase schema) {
        AccessConfiguration accessConfiguration = database.getAccessConfiguration();

        // If the schema is not public, create it first
        if (!accessConfiguration.getSchemaName().equals("public")) {
            database.getInitDBConfiguration().setInitDBScript(createSchemaSQL(accessConfiguration.getSchemaName()));
        }

        database.getInitDBConfiguration().setCreateTablesFromFiles(false);
        database.initDBMS();

        // Create the tables
        ICreateTable tableGenerator = new SQLCreateTable();
        List<SchemaTable> tables = schema.getTables();
        tables.forEach(t -> tableGenerator.createTable(t.getName(), t.getSpeedyAttributes(), t.getKeyAttributes(), database));
    }

    private static void populateTables(String experimentFolder, DBMSDB database) throws IOException {
        String dataFolder = experimentFolder + "/data";

        URL resource = DatabaseInitializer.class.getResource(dataFolder);
        if (resource == null) {
            log.warn("No data folder found: database will be empty!");
            return;
        }

        File[] files = new File(resource.getPath()).listFiles((dir, name) -> name.endsWith(".csv"));
        if (files == null) {
            log.warn("No csv files found in the data folder: database will be empty!");
            return;
        }

        Map<String, File> tableCSVFiles = new HashMap<>();
        for (File file : files) {
            String tableName = file.getName().replace(".csv", "").toLowerCase();
            log.trace("Table {} content in file {}", tableName, file);
            tableCSVFiles.put(tableName, file);
        }

        IBatchInsert batchInsert = new SQLBatchInsert();

        for (String tableName : database.getTableNames()) {
            ITable table = database.getTable(tableName);

            File file = tableCSVFiles.get(tableName);
            if (file == null) {
                log.warn("Unexisting csv file for table {}", tableName);
                continue;
            }

            String headerLine = Files.readAllLines(Path.of(file.getPath()), StandardCharsets.UTF_8).getFirst();
            Character quote = headerLine.contains("'") ? '\'' : '"';
            try (FileReader fileReader = new FileReader(file)) {
                CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                        .setSkipHeaderRecord(false)
                        .setQuote(quote)
                        .build();
                Iterable<CSVRecord> records = csvFormat.parse(fileReader);
                Iterator<CSVRecord> iterator = records.iterator();
                CSVRecord headerRecord = iterator.next();

                int line = 1;
                while (iterator.hasNext()) {
                    CSVRecord csvRecord = iterator.next();

                    TupleOID tupleOID = new TupleOID(IntegerOIDGenerator.getNextOID());
                    Tuple tuple = new Tuple(tupleOID);

                    for (int i = 0; i < csvRecord.size(); i++) {
                        String csvHeader = headerRecord.get(i);
                        String csvAttribute = csvRecord.get(i);

                        Attribute attribute = table.getAttribute(csvHeader);
                        if (attribute == null) throw new IllegalArgumentException("Unexistent attribute: " + csvHeader);

                        boolean isNull = csvAttribute.isEmpty() || csvAttribute.equals("null");
                        if (!attribute.getNullable() && isNull)
                            throw new IllegalArgumentException("Line " + line + ": " + attribute.getName() + " is null!");

                        AttributeRef attributeRef = new AttributeRef(attribute.getTableName(), attribute.getName());
                        IValue value = isNull ? new NullValue(SpeedyConstants.NULL) : new ConstantValue(csvAttribute);
                        Cell cell = new Cell(tupleOID, attributeRef, value);

                        tuple.addCell(cell);
                    }

                    line += 1;
                    batchInsert.insert(table, tuple, database);
                }

                batchInsert.flush(database);
            }
        }
    }

    private static String createSchemaSQL(String schemaName) {
        StringBuilder sb = new StringBuilder();
        if (schemaName == null || schemaName.isEmpty()) return "";
        return sb.append("create schema ").append(schemaName).append(";").toString();
    }

    private static boolean dropDatabase(AccessConfiguration configuration, SchemaDatabase schema) {
        closeDatabaseConnection(configuration);

        // TODO: this could potentially be passed from the caller method, as database settings may differ
        DBMSDB postgres = (DBMSDB) connectToPostgres("postgres", "public", "pguser", "pguser");
        AccessConfiguration postgresConfiguration = postgres.getAccessConfiguration();

        try (
                Connection connection = QueryManager.getConnection(postgresConfiguration);
                Statement statement = connection.createStatement();
        ) {
            statement.addBatch("select pg_terminate_backend(pg_stat_activity.pid) from pg_stat_activity where datname = '" + schema.getDbName() + "' and pid <> pg_backend_pid()");
            statement.addBatch("alter database " + schema.getDbName() + " connection limit 0");
            statement.addBatch("drop database " + schema.getDbName());
            statement.executeBatch();
            return true;
        } catch (SQLException ex) {
            log.error("Cannot execute dropDatabase for database {}", schema.getDbName(), ex);
            return false;
        } finally {
            closeDatabaseConnection(postgresConfiguration);
        }
    }

    private static void closeDatabaseConnection(AccessConfiguration configuration) {
        Connection connection = QueryManager.getConnection(configuration);
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                log.error("Cannot close non-null connection! {}", connection);
            }
        }
    }
}
