package engine.persistence.file.operators;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jdom.Document;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import engine.EngineConstants;
import engine.exceptions.DAOException;
import engine.model.algebra.operators.IBatchInsert;
import engine.model.algebra.operators.ICreateTable;
import engine.model.database.Attribute;
import engine.model.database.AttributeRef;
import engine.model.database.Cell;
import engine.model.database.ConstantValue;
import engine.model.database.IDatabase;
import engine.model.database.IValue;
import engine.model.database.NullValue;
import engine.model.database.Tuple;
import engine.model.database.TupleOID;
import engine.model.database.dbms.DBMSDB;
import engine.model.database.dbms.InitDBConfiguration;
import engine.model.database.mainmemory.datasource.IntegerOIDGenerator;
import engine.model.database.operators.dbms.IValueEncoder;
import engine.persistence.Types;
import engine.persistence.file.XMLFile;
import engine.persistence.xml.DAOXmlUtility;
import engine.utility.EngineUtility;

public class ImportXMLFile {

    private final static Logger logger = LoggerFactory.getLogger(ImportXMLFile.class);
    private DAOXmlUtility daoUtility = new DAOXmlUtility();
    private ICreateTable tableCreator;
    private IBatchInsert batchInsertOperator;
    private IValueEncoder valueEncoder;

    public ImportXMLFile(ICreateTable tableCreator, IBatchInsert batchInsertOperator, IValueEncoder valueEncoder) {
        this.tableCreator = tableCreator;
        this.batchInsertOperator = batchInsertOperator;
        this.valueEncoder = valueEncoder;
    }

    @SuppressWarnings("unchecked")
    public void importXMLFile(String tableName, XMLFile fileToImport, Map<String, List<Attribute>> tablesAdded, DBMSDB db) {
        String xmlFile = fileToImport.getFileName();
        InitDBConfiguration configuration = db.getInitDBConfiguration();
        try {
            Document document = daoUtility.buildDOM(xmlFile);
            Element tableElement = document.getRootElement();
            if (tableElement.getChildren().isEmpty()) {
                throw new DAOException("Unable to import file from empty file " + xmlFile);
            }
            System.out.println("Importing file " + xmlFile + " into table " + tableName + "...");
            if (!tablesAdded.containsKey(tableName)) {
                List<Attribute> attributes = createXMLTable(tableName, tableElement, db, configuration.isCreateTablesFromFiles());
                tablesAdded.put(tableName, attributes);
            }
            List<Attribute> attributes = tablesAdded.get(tableName);
            insertXMLTuples(tableName, attributes, tableElement, db, xmlFile);
        } catch (DAOException ex) {
            logger.error(ex.getLocalizedMessage());
            ex.printStackTrace();
            String message = "Unable to load XML file " + xmlFile;
            if (ex.getMessage() != null && !ex.getMessage().equals("NULL")) {
                message += "\n" + ex.getMessage();
            }
            throw new DAOException(message);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Attribute> createXMLTable(String tableName, Element tableElement, DBMSDB database, boolean createTable) {
        if (logger.isDebugEnabled()) logger.debug("Starting to create xml tables...");
        List<Attribute> attributes = new ArrayList<Attribute>();
        Element firstChild = (Element) tableElement.getChildren().get(0);
        for (Element attributeElement : (List<Element>) firstChild.getChildren()) {
            String attributeName = attributeElement.getName();
            String attributeType = Types.STRING;
            if (attributeElement.getAttribute("type") != null) {
                attributeType = attributeElement.getAttribute("type").getValue();
            }
            Attribute attribute = new Attribute(tableName, attributeName, attributeType);
            attributes.add(attribute);
        }
        if (createTable) {
            tableCreator.createTable(tableName, attributes, database);
        }
        return attributes;
    }

    @SuppressWarnings("unchecked")
    private void insertXMLTuples(String tableName, List<Attribute> attributes, Element tableElement, IDatabase target, String xmlFile) {
        if (logger.isDebugEnabled()) logger.debug("Starting to import xml tuples...");
        for (Element tupleElement : (List<Element>) tableElement.getChildren()) {
            TupleOID tupleOID = new TupleOID(IntegerOIDGenerator.getNextOID());
            Tuple tuple = new Tuple(tupleOID);
            if (logger.isDebugEnabled()) logger.debug("Importing tuple OID " + tupleOID);
            for (Attribute attribute : attributes) {
                if (logger.isDebugEnabled()) logger.debug("Attribute: " + attribute.getName());
                Element attributeElement = tupleElement.getChild(attribute.getName());
                if (attributeElement == null) {
                    throw new DAOException("Error importing " + xmlFile + ". Attribute " + attribute.getName() + " in table " + tableName + " is missing");
                }
                String stringValue = attributeElement.getText();
                AttributeRef attributeRef = new AttributeRef(attribute.getTableName(), attribute.getName());
                IValue value;
                if (!EngineUtility.isNull(stringValue)) {
                    if (valueEncoder != null) {
                        stringValue = valueEncoder.encode(stringValue);
                    }
                    value = new ConstantValue(stringValue);
                } else {
                    value = new NullValue(EngineConstants.NULL);
                }
                Cell cell = new Cell(tupleOID, attributeRef, value);
                tuple.addCell(cell);
            }
            if (logger.isDebugEnabled()) logger.debug("Preparing insert with operator " + batchInsertOperator.getClass().getName());
            batchInsertOperator.insert(target.getTable(tableName), tuple, target);
        }
        batchInsertOperator.flush(target);
    }
}
