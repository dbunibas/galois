package engine.model.database.operators.dbms;

import engine.EngineConstants;
import engine.model.database.Attribute;
import engine.model.database.EmptyDB;
import engine.model.database.IDatabase;
import engine.model.database.dbms.DBMSDB;
import engine.model.database.dbms.DBMSVirtualDB;
import engine.model.database.operators.IDatabaseManager;
import engine.persistence.relational.AccessConfiguration;
import engine.utility.DBMSUtility;
import engine.persistence.relational.QueryManager;
import engine.utility.EngineUtility;

public class SQLDatabaseManager implements IDatabaseManager {

    public IDatabase createDatabase(IDatabase target, String suffix) {
        AccessConfiguration targetConfiguration = ((DBMSDB) target).getAccessConfiguration();
        AccessConfiguration newAccessConfiguration = targetConfiguration.clone();
        newAccessConfiguration.setSchemaSuffix(suffix);
        DBMSUtility.createSchema(newAccessConfiguration);
        DBMSDB database = new DBMSDB(newAccessConfiguration);
        return database;
    }

    public IDatabase cloneTarget(IDatabase target, String suffix) {
        AccessConfiguration targetConfiguration = ((DBMSDB) target).getAccessConfiguration();
        AccessConfiguration cloneConfiguration = targetConfiguration.clone();
        cloneConfiguration.setSchemaSuffix(suffix);
        cloneSchema(targetConfiguration.getSchemaAndSuffix(), cloneConfiguration.getSchemaAndSuffix(), targetConfiguration);
//        DBMSDB clone = new DBMSDB(target, cloneAccessConfiguration); //shallow copy
        DBMSDB clone = new DBMSDB(cloneConfiguration);
        return clone;
    }

    public void removeClone(IDatabase target, String suffix) {
        AccessConfiguration targetConfiguration = ((DBMSDB) target).getAccessConfiguration();
        AccessConfiguration cloneConfiguration = targetConfiguration.clone();
        cloneConfiguration.setSchemaSuffix(suffix);
        DBMSUtility.removeSchema(cloneConfiguration.getSchemaAndSuffix(), targetConfiguration);
    }

    public void removeTable(String tableName, IDatabase db) {
        AccessConfiguration ac = ((DBMSDB) db).getAccessConfiguration();
        StringBuilder script = new StringBuilder();
        script.append("DROP TABLE ").append(ac.getSchemaAndSuffix()).append(".").append(tableName).append("\n");
        QueryManager.executeScript(script.toString(), ac, true, true, true, false);
    }

    private void cloneSchema(String src, String dest, AccessConfiguration ac) {
        StringBuilder script = new StringBuilder();
        script.append(getCloneFunction()).append("\n");
        script.append("SELECT clone_schema('").append(src).append("','").append(dest).append("');");
        QueryManager.executeScript(script.toString(), ac, true, true, true, false);
    }

    private String getCloneFunction() {
        StringBuilder function = new StringBuilder();
        function.append("CREATE OR REPLACE FUNCTION clone_schema(source_schema text, dest_schema text) RETURNS void AS").append("\n");
        function.append("$BODY$").append("\n");
        function.append("DECLARE ").append("\n");
        function.append("  objeto text;").append("\n");
        function.append("  buffer text;").append("\n");
        function.append("BEGIN").append("\n");
        function.append("    EXECUTE 'CREATE SCHEMA ' || dest_schema ;").append("\n");
        function.append("    FOR objeto IN").append("\n");
        function.append("        SELECT table_name::text FROM information_schema.TABLES WHERE table_schema = source_schema").append("\n");
        function.append("    LOOP").append("\n");
        function.append("        buffer := dest_schema || '.' || objeto;").append("\n");
        function.append("        EXECUTE 'CREATE TABLE ' || buffer || ' (LIKE ' || source_schema || '.' || objeto || ' INCLUDING CONSTRAINTS INCLUDING INDEXES INCLUDING DEFAULTS)';").append("\n");
//        function.append("        EXECUTE 'CREATE TABLE ' || buffer || ' (LIKE ' || source_schema || '.' || objeto || ' INCLUDING CONSTRAINTS INCLUDING INDEXES INCLUDING DEFAULTS) WITH OIDS';").append("\n");
        function.append("        EXECUTE 'INSERT INTO ' || buffer || '(SELECT * FROM ' || source_schema || '.' || objeto || ')';").append("\n");
        function.append("    END LOOP;").append("\n");
        function.append("END;").append("\n");
        function.append("$BODY$").append("\n");
        function.append("LANGUAGE plpgsql VOLATILE;").append("\n");
        return function.toString();
    }

    public void addUniqueConstraints(IDatabase db) {
        StringBuilder sb = new StringBuilder();
        AccessConfiguration ac;
        if (db instanceof DBMSDB) {
            ac = ((DBMSDB) db).getAccessConfiguration();
        } else if (db instanceof DBMSVirtualDB) {
            ac = ((DBMSVirtualDB) db).getAccessConfiguration();
        } else {
            throw new IllegalArgumentException();
        }
        for (String tableName : db.getTableNames()) {
            sb.append("ALTER TABLE ").append(DBMSUtility.getSchemaNameAndDot(ac)).append(tableName).append(" ADD UNIQUE (");
            for (Attribute attribute : db.getTable(tableName).getAttributes()) {
                if (attribute.getName().equalsIgnoreCase(EngineConstants.OID)) {
                    continue;
                }
                sb.append(attribute.getName()).append(", ");
            }
            EngineUtility.removeChars(", ".length(), sb);
            sb.append(") NOT DEFERRABLE INITIALLY IMMEDIATE;\n");
        }
        QueryManager.executeScript(sb.toString(), ac, true, true, true, false);
    }

    public void initDatabase(IDatabase source, IDatabase target, boolean cleanTarget, boolean preventInsertDuplicates) {
        AccessConfiguration accessConfiguration = ((DBMSDB) target).getAccessConfiguration();
        createWorkSchema(accessConfiguration);
        createFunctionsForNumericalSkolems(source, target);
        if (preventInsertDuplicates) {
            addUniqueConstraints(target);
        }
        if (cleanTarget) {
            QueryManager.executeScript(cleanTargetScript(target), accessConfiguration, true, true, true, true);
        }
    }

    private void createWorkSchema(AccessConfiguration accessConfiguration) {
        StringBuilder result = new StringBuilder();
        result.append("DROP SCHEMA IF EXISTS ").append(EngineConstants.WORK_SCHEMA).append(" CASCADE;\n");
        result.append("CREATE SCHEMA ").append(EngineConstants.WORK_SCHEMA).append(";\n\n");
        QueryManager.executeScript(result.toString(), accessConfiguration, true, true, false, false);
    }

    private void createFunctionsForNumericalSkolems(IDatabase source, IDatabase target) {
        if (!(source instanceof EmptyDB)) {
            AccessConfiguration sourceAccessConfiguration = ((DBMSDB) source).getAccessConfiguration();
            DBMSUtility.createFunctionsForNumericalSkolem(sourceAccessConfiguration);
        }
        AccessConfiguration targetAccessConfiguration = ((DBMSDB) target).getAccessConfiguration();
        DBMSUtility.createFunctionsForNumericalSkolem(targetAccessConfiguration);
        AccessConfiguration workAccessConfiguration = targetAccessConfiguration.clone();
        workAccessConfiguration.setSchemaName(EngineConstants.WORK_SCHEMA);
        DBMSUtility.createFunctionsForNumericalSkolem(workAccessConfiguration);
    }

    private String cleanTargetScript(IDatabase target) {
        StringBuilder result = new StringBuilder();
        result.append("----- Cleaning Target -----\n");
        DBMSDB targetDB = (DBMSDB) target;
        for (String tableName : target.getTableNames()) {
            result.append("DELETE FROM ").append(DBMSUtility.getSchemaNameAndDot(targetDB.getAccessConfiguration())).append(tableName).append(";\n");
        }
        result.append("\n");
        return result.toString();
    }
}
