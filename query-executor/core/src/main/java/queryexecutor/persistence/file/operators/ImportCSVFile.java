package queryexecutor.persistence.file.operators;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import queryexecutor.QueryExecutorConstants;
import queryexecutor.exceptions.DAOException;
import queryexecutor.model.algebra.operators.IBatchInsert;
import queryexecutor.model.algebra.operators.ICreateTable;
import queryexecutor.model.database.Attribute;
import queryexecutor.model.database.AttributeRef;
import queryexecutor.model.database.Cell;
import queryexecutor.model.database.ConstantValue;
import queryexecutor.model.database.IValue;
import queryexecutor.model.database.NullValue;
import queryexecutor.model.database.Tuple;
import queryexecutor.model.database.TupleOID;
import queryexecutor.model.database.dbms.DBMSDB;
import queryexecutor.model.database.dbms.InitDBConfiguration;
import queryexecutor.model.database.mainmemory.datasource.IntegerOIDGenerator;
import queryexecutor.model.database.operators.dbms.IValueEncoder;
import queryexecutor.persistence.file.CSVFile;
import queryexecutor.utility.QueryExecutorUtility;

public class ImportCSVFile {

    private final static Logger logger = LoggerFactory.getLogger(ImportCSVFile.class);
    private ICreateTable tableCreator;
    private IBatchInsert batchInsertOperator;
    private IValueEncoder valueEncoder;

    public ImportCSVFile(ICreateTable tableCreator, IBatchInsert batchInsertOperator, IValueEncoder valueEncoder) {
        this.tableCreator = tableCreator;
        this.batchInsertOperator = batchInsertOperator;
        this.valueEncoder = valueEncoder;
    }

    public void importCSVFile(String tableName, CSVFile fileToImport, Map<String, List<Attribute>> tablesAdded, DBMSDB database) {
        String csvFile = fileToImport.getFileName();
        InitDBConfiguration configuration = database.getInitDBConfiguration();
        Reader in = null;
        try {
            in = new FileReader(csvFile);
            List<Attribute> attributes;
            CsvMapper mapper = new CsvMapper();
            mapper.enable(CsvParser.Feature.WRAP_AS_ARRAY);
            CsvSchema schema = CsvSchema.emptySchema().
                    withColumnSeparator(fileToImport.getSeparator());
            if (fileToImport.getQuoteCharacter() != null) {
                schema = schema.withQuoteChar(fileToImport.getQuoteCharacter());
            }
            MappingIterator<String[]> it = mapper.readerFor(String[].class).with(schema).readValues(in);
            if (!it.hasNext()) {
                throw new DAOException("Empty file " + csvFile);
            }
            if (fileToImport.isHasHeader()) {
                String[] headers = it.next();
                attributes = CSVUtility.readCSVAttributes(tableName, headers);
                System.out.println("Importing file " + csvFile + " into table " + tableName + "...");
                if (!tablesAdded.containsKey(tableName)) {
                    tablesAdded.put(tableName, attributes);
                    if (configuration.isCreateTablesFromFiles()) {
                        tableCreator.createTable(tableName, attributes, database);
                    }
                }
            } else {
                database.loadTables();
                attributes = QueryExecutorUtility.extractAttributesFromDB(tableName, database);
            }
            insertCSVTuples(tableName, attributes, it, database, csvFile, fileToImport.getRecordsToImport(), fileToImport.isRandomizeInput());
        } catch (Exception ex) {
            logger.error(ex.getLocalizedMessage());
            ex.printStackTrace();
            String message = "Unable to load CSV file " + csvFile;
            if (ex.getMessage() != null && !ex.getMessage().equals("NULL")) {
                message += "\n" + ex.getMessage();
            }
            throw new DAOException(message);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                }
            }
        }
    }

    private void insertCSVTuples(String tableName, List<Attribute> attributes, Iterator<String[]> it, DBMSDB target, String csvFile, Integer recordsToImport, boolean randomizeInput) {
        int importedRecords = 0;
        if (randomizeInput) {
            it = randomizeCSVRecords(it, recordsToImport);
        }
        while (it.hasNext()) {
            String[] record = it.next();
            TupleOID tupleOID = new TupleOID(IntegerOIDGenerator.getNextOID());
            Tuple tuple = new Tuple(tupleOID);
            for (int i = 0; i < attributes.size(); i++) {
                Attribute attribute = attributes.get(i);
                String stringValue = record[i];
                if (stringValue == null) {
                    throw new DAOException("Error importing " + csvFile + ". Attribute " + attribute.getName() + " in table " + tableName + " is missing");
                }
                AttributeRef attributeRef = new AttributeRef(attribute.getTableName(), attribute.getName());
                IValue value;
                if (!QueryExecutorUtility.isNull(stringValue)) {
                    if (valueEncoder != null) {
                        stringValue = valueEncoder.encode(stringValue);
                    }
                    value = new ConstantValue(stringValue);
                } else {
                    value = new NullValue(QueryExecutorConstants.NULL);
                }
                Cell cell = new Cell(tupleOID, attributeRef, value);
                tuple.addCell(cell);
            }
            batchInsertOperator.insert(target.getTable(tableName), tuple, target);
            importedRecords++;
            if (recordsToImport != null && importedRecords >= recordsToImport) {
                break;
            }
        }
        batchInsertOperator.flush(target);
    }

    private Iterator<String[]> randomizeCSVRecords(Iterator<String[]> it, Integer recordsToImport) {
        List<String[]> result = new ArrayList<String[]>();
        int importedRecords = 0;
        while (it.hasNext()) {
            String[] record = it.next();
            result.add(record);
            if (recordsToImport != null && importedRecords >= recordsToImport) {
                break;
            }
        }
        Collections.shuffle(result);
        return result.iterator();
    }
}
