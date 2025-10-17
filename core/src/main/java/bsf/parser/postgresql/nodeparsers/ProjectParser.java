package bsf.parser.postgresql.nodeparsers;

import bsf.llm.algebra.config.OperatorsConfiguration;
import org.jdom2.Element;
import queryexecutor.model.algebra.IAlgebraOperator;
import queryexecutor.model.algebra.Project;
import queryexecutor.model.algebra.ProjectionAttribute;
import queryexecutor.model.database.AttributeRef;
import queryexecutor.model.database.IDatabase;
import queryexecutor.model.database.ITable;
import queryexecutor.model.database.TableAlias;

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
