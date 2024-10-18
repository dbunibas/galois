package floq.test.experiments;

import floq.parser.IQueryPlanParser;
import floq.parser.postgresql.PostgresXMLParser;
import floq.planner.IQueryPlanner;
import floq.planner.postgresql.xml.PostgresXMLPlanner;
import engine.persistence.relational.AccessConfiguration;

public class PlannerParserFactory {
    public static IQueryPlanner<?> getPlannerFor(String dbms, AccessConfiguration accessConfiguration) {
        switch (dbms) {
            case "postgres":
                return new PostgresXMLPlanner(accessConfiguration);
            default:
                throw new UnsupportedOperationException("Cannot create planner with parser for dbms: " + dbms);
        }
    }

    public static IQueryPlanParser<?> getParserFor(String dbms) {
        switch (dbms) {
            case "postgres":
                return new PostgresXMLParser();
            default:
                throw new UnsupportedOperationException("Cannot create planner with parser for dbms: " + dbms);
        }
    }
}
