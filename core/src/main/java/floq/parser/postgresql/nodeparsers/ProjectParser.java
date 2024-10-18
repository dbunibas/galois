package floq.parser.postgresql.nodeparsers;

import floq.llm.algebra.config.OperatorsConfiguration;
import org.jdom2.Element;
import engine.model.algebra.IAlgebraOperator;
import engine.model.algebra.Project;
import engine.model.algebra.ProjectionAttribute;
import engine.model.database.AttributeRef;
import engine.model.database.IDatabase;
import engine.model.database.ITable;
import engine.model.database.TableAlias;

import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProjectParser extends AbstractNodeParser {
    public boolean shouldParseNode(Element node, ITable table) {
        if (node == null) return false;
        List<Element> items = node.getChildren("Item", node.getNamespace());
        return items.size() != table.getAttributes().size();
    }

    @Override
    public IAlgebraOperator parse(Element node, IDatabase database, OperatorsConfiguration configuration) {
        String tableName = node.getChild("Relation-Name", node.getNamespace()).getText();
        String alias = node.getChild("Alias", node.getNamespace()).getText();
        TableAlias tableAlias = new TableAlias(tableName, alias);
        setTableAlias(tableAlias);
        Element output = node.getChild("Output", node.getNamespace());
        return new Project(getProjectionAttributes(output));
    }

    public List<ProjectionAttribute> getProjectionAttributes(Element output) {
        return output.getChildren("Item", output.getNamespace()).stream()
                .map(i -> new ProjectionAttribute(new AttributeRef(getTableAlias(), i.getText())))
                .toList();
    }
}
