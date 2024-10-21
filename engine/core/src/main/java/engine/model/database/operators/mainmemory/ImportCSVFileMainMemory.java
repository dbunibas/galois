package engine.model.database.operators.mainmemory;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import engine.EngineConstants;
import static engine.EngineConstants.OID;
import engine.exceptions.DAOException;
import engine.model.database.IValue;
import engine.model.database.NullValue;
import engine.model.database.mainmemory.datasource.DataSource;
import engine.model.database.mainmemory.datasource.INode;
import engine.model.database.mainmemory.datasource.IntegerOIDGenerator;
import engine.model.database.mainmemory.datasource.OID;
import engine.model.database.mainmemory.datasource.nodes.AttributeNode;
import engine.model.database.mainmemory.datasource.nodes.LeafNode;
import engine.model.database.mainmemory.datasource.nodes.SetNode;
import engine.model.database.mainmemory.datasource.nodes.TupleNode;
import engine.persistence.PersistenceConstants;
import engine.persistence.Types;
import engine.utility.DBMSUtility;

public class ImportCSVFileMainMemory {

    private static final String CSV_EXTENSION = ".csv";
    private static final Logger logger = LoggerFactory.getLogger(ImportCSVFileMainMemory.class);

    public DataSource loadSchema(String instancePath, char separator, Character quoteCharacter) {
        return loadSchema(instancePath, separator, quoteCharacter, true);
    }

    public DataSource loadSchema(String instancePath, char separator, Character quoteCharacter, boolean header) {
        List<File> filesTable = getFileInFolder(instancePath, CSV_EXTENSION);
        Map<File, CSVTable> mapTable = loadTable(filesTable, separator, quoteCharacter, header);
        INode schemaNode = new TupleNode(PersistenceConstants.DATASOURCE_ROOT_LABEL, IntegerOIDGenerator.getNextOID());
        schemaNode.setRoot(true);
        generateSchema(schemaNode, mapTable);
        DataSource dataSource = new DataSource(PersistenceConstants.TYPE_CSV, schemaNode);
        if (logger.isDebugEnabled()) logger.debug(dataSource.getSchema().toString());
        return dataSource;
    }

    public void loadInstance(DataSource dataSource, String instancePath, char separator, Character quoteCharacter, boolean convertSkolemInHash) {
        loadInstance(dataSource, instancePath, separator, quoteCharacter, convertSkolemInHash, true);
    }

    public void loadInstance(DataSource dataSource, String instancePath, char separator, Character quoteCharacter, boolean convertSkolemInHash, boolean header) {
        List<File> filesTable = getFileInFolder(instancePath, CSV_EXTENSION);
        Map<File, CSVTable> mapTable = loadTable(filesTable, separator, quoteCharacter, header);
        INode instanceNode = new TupleNode(PersistenceConstants.DATASOURCE_ROOT_LABEL, IntegerOIDGenerator.getNextOID());
        instanceNode.setRoot(true);
        insertData(instanceNode, mapTable, separator, quoteCharacter, convertSkolemInHash, header);
        dataSource.addInstanceWithCheck(instanceNode);
    }

    private Map<File, CSVTable> loadTable(List<File> filesTable, char separator, Character quoteCharacter, boolean header) throws DAOException {
        Map<File, CSVTable> mapFileToTable = new HashMap<File, CSVTable>();
        for (File file : filesTable) {
            try {
                CsvMapper mapper = new CsvMapper();
                mapper.enable(CsvParser.Feature.WRAP_AS_ARRAY);
                CsvSchema schema = CsvSchema.emptySchema().
                        withColumnSeparator(separator);
                if (quoteCharacter != null) {
                    schema = schema.withQuoteChar(quoteCharacter);
                }
                String[] headers = null;
                if (header) {
                    MappingIterator<String[]> it = mapper.readerFor(String[].class).with(schema).readValues(file);
                    if (!it.hasNext()) {
                        throw new DAOException("Empty file " + file);
                    }
                    headers = it.next();
                } else {
                    MappingIterator<String[]> it = mapper.readerFor(String[].class).with(schema).readValues(file);
                    if (!it.hasNext()) {
                        throw new DAOException("Empty file " + file);
                    }
                    String[] values = it.next();
                    headers = new String[values.length];
                    for (int i = 0; i < values.length; i++) {
                        headers[i] = "a_" + i;
                    }
                }
                String tableName = extractTableName(file, CSV_EXTENSION);
                CSVTable table = new CSVTable(tableName);
                for (String attributeName : headers) {
                    table.addAttribute(attributeName);
                }
                mapFileToTable.put(file, table);
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new DAOException(ex.getMessage());
            }
        }
        return mapFileToTable;
    }

    private String extractTableName(File file, String CSV_EXTENSION) {
        return file.getName().replaceAll(CSV_EXTENSION, "");
    }

    private void generateSchema(INode schemaNode, Map<File, CSVTable> mapTable) {
        Collection<CSVTable> csvTables = mapTable.values();
        for (CSVTable csvTable : csvTables) {
            INode setNodeSchema = new SetNode(csvTable.getName());
            schemaNode.addChild(setNodeSchema);
            TupleNode tupleNodeSchema = new TupleNode(csvTable.getName() + "Tuple");
            setNodeSchema.addChild(tupleNodeSchema);
            for (String attribute : csvTable.getAttributes()) {
                tupleNodeSchema.addChild(createAttributeSchema(attribute));
            }
        }
    }

    private AttributeNode createAttributeSchema(String attributeName) {
        AttributeNode attributeNodeInstance = new AttributeNode(attributeName);
        LeafNode leafNodeInstance = new LeafNode(Types.STRING, EngineConstants.NULL_VALUE);
        attributeNodeInstance.addChild(leafNodeInstance);
        return attributeNodeInstance;
    }

    private void insertData(INode setNodeDB, Map<File, CSVTable> mapTable, char separator, Character quoteCharacter, boolean convertSkolemInHash, boolean header) {
        Set<File> fileSet = mapTable.keySet();
        for (File file : fileSet) {
            CSVTable csvTable = mapTable.get(file);
            addTable(setNodeDB, csvTable, file, separator, quoteCharacter, convertSkolemInHash, header);
        }
    }

    private void addTable(INode setNodeDB, CSVTable csvTable, File file, char separator, Character quoteCharacter, boolean convertSkolemInHash, boolean header) {
        INode setNodeTable = new SetNode(csvTable.getName(), IntegerOIDGenerator.getNextOID());
        setNodeDB.addChild(setNodeTable);
        try {
            CsvMapper mapper = new CsvMapper();
            mapper.enable(CsvParser.Feature.WRAP_AS_ARRAY);
            CsvSchema schema = CsvSchema.emptySchema().
                    withColumnSeparator(separator);
            if (quoteCharacter != null) {
                schema = schema.withQuoteChar(quoteCharacter);
            }
            MappingIterator<String[]> it = mapper.readerFor(String[].class).with(schema).readValues(file);
            if (!it.hasNext()) {
                throw new DAOException("Empty file " + file);
            }
            if (header) {
                it.next(); //Skipping header
            }
            insertDataInTable(setNodeTable, csvTable, it, convertSkolemInHash);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new DAOException("Unable to load csv file " + file.toString() + "\n" + ex.getLocalizedMessage());
        }
    }

    private AttributeNode createAttributeInstance(String attributeName, Object objValue, boolean convertSkolemInHash) {
        AttributeNode attributeNodeInstance = new AttributeNode(attributeName, IntegerOIDGenerator.getNextOID());
        IValue value = DBMSUtility.convertDBMSValue(objValue);
        if (convertSkolemInHash && (value instanceof NullValue) && ((NullValue) value).isLabeledNull()) {
            value = new NullValue(EngineConstants.SKOLEM_PREFIX + value.hashCode());
        }
        LeafNode leafNodeInstance = new LeafNode(Types.STRING, value);
        attributeNodeInstance.addChild(leafNodeInstance);
        return attributeNodeInstance;
    }

    private void insertDataInTable(INode setNodeTable, CSVTable csvTable, MappingIterator<String[]> it, boolean convertSkolemInHash) {
        while (it.hasNext()) {
            String[] record = it.next();
            if (record.length == 0 || areAllEmpty(record)) {
                continue;
            }
            if (record.length != csvTable.getAttributes().size()) {
                throw new DAOException("Line " + Arrays.toString(record) + " doesn't contains " + csvTable.getAttributes().size() + " values. but " + record.length);
            }
            OID tupleOID = getTupleOID(csvTable, record);
            TupleNode tupleNodeInstance = new TupleNode(csvTable.getName() + "Tuple", tupleOID);
            setNodeTable.addChild(tupleNodeInstance);
            for (int i = 0; i < csvTable.getAttributes().size(); i++) {
                String attributeName = csvTable.getAttributes().get(i);
                if(OID.equals(attributeName)){
                    continue;
                }
                String value = record[i];
                if (value != null) {
                    value = value.trim();
                }
                AttributeNode attributeNode = createAttributeInstance(attributeName, value, convertSkolemInHash);
                tupleNodeInstance.addChild(attributeNode);
            }
        }
    }

    private OID getTupleOID(CSVTable csvTable, String[] record) {
        int oidIndex = csvTable.getAttributes().indexOf(OID);
        if (oidIndex != -1) {
            try{
                return new OID(Integer.valueOf(record[oidIndex]));
            }catch(Exception e){
                logger.warn("Unable to load OID value from attribute {} from record {}", oidIndex, record, e);
            }
        }
        return IntegerOIDGenerator.getNextOID();
    }

    private List<File> getFileInFolder(String folderPath, String extension) {
        File folder = new File(folderPath);
        if (!folder.exists()) {
            throw new DAOException("Folder " + folder + " doesn't exist");
        }
        List<File> files = new ArrayList<File>();
        File[] listFiles = folder.listFiles();
        if (listFiles == null) {
            throw new DAOException("No files in folder " + folder.toString());
        }
        for (File file : listFiles) {
            if (file.isFile() && (extension == null || file.getName().endsWith(extension))) {
                files.add(file);
            }
        }
        return files;

    }

    private boolean areAllEmpty(String[] record) {
        for (int i = 0; i < record.length; i++) {
            if (!record[i].isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private class CSVTable {

        private String name;
        private List<String> attributes = new ArrayList<String>();

        public CSVTable(String name) {
            this.name = name;
        }

        public void addAttribute(String attributeName) {
            this.attributes.add(attributeName);
        }

        public String getName() {
            return name;
        }

        public List<String> getAttributes() {
            return attributes;
        }

    }

}
