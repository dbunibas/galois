package galois.parser.postgresql.nodeparsers;

import galois.llm.algebra.config.OperatorsConfiguration;
import org.jdom2.Element;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.algebra.Project;
import speedy.model.algebra.ProjectionAttribute;
import speedy.model.database.AttributeRef;
import speedy.model.database.IDatabase;
import speedy.model.database.ITable;
import speedy.model.database.TableAlias;

import java.util.List;

public class ProjectParser extends AbstractNodeParser {
    public boolean shouldParseNode(Element node, ITable table) {
        if (node == null) return false;
        return node.getChildren("Item", node.getNamespace()).size() != table.getAttributes().size();
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

    private List<ProjectionAttribute> getProjectionAttributes(Element output) {
        return output.getChildren("Item", output.getNamespace()).stream()
                .map(i -> new ProjectionAttribute(new AttributeRef(getTableAlias(), i.getText())))
                .toList();
    }
}
