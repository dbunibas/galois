package floq.parser;

import java.util.ArrayList;
import java.util.List;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.StatementVisitorAdapter;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.FromItemVisitorAdapter;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectVisitorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParserFrom {

    private static final Logger logger = LoggerFactory.getLogger(ParserFrom.class);

    private SelectVisitorAdapter selectVisitorAdapter;
    private StatementVisitorAdapter statementVisitor;
    private FromItemVisitorAdapter fromVisitorAdapter;
    private List<String> tables = new ArrayList<>();

    public ParserFrom() {
        initParsers();
    }

    public void parseFrom(String sql) throws ParserException {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            statement.accept(statementVisitor);
        } catch (Exception e) {
            throw new ParserException("Error with parsing: " + sql + "\nMessage: " + e.getMessage());
        }
    }

    public List<String> getTables() {
        return tables;
    }

    private void initParsers() {

        this.fromVisitorAdapter = new FromItemVisitorAdapter<>() {
            @Override
            public Object visit(Table tableName, Object context) {
                String name = tableName.getName();
                tables.add(name);
                return super.visit(tableName, context);
            }

        };

        this.selectVisitorAdapter = new SelectVisitorAdapter() {

            @Override
            public Object visit(PlainSelect plainSelect, Object context) {
                if (plainSelect.getFromItem() != null) {
                    FromItem fromItem = plainSelect.getFromItem();
                    fromItem.accept(fromVisitorAdapter, null);
                }
                if (plainSelect.getJoins() != null && !plainSelect.getJoins().isEmpty()) {
                    for (Join join : plainSelect.getJoins()) {
                        join.getFromItem().accept(fromVisitorAdapter, null);
                    }
                }
                return super.visit(plainSelect, context);
            }
        };

        statementVisitor = new StatementVisitorAdapter() {
            @Override
            public Object visit(Select select, Object context) {
                return select.getPlainSelect().accept(selectVisitorAdapter, null);
            }
        };
    }

}
