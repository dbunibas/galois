package galois.test.experiments;

import galois.parser.IQueryPlanParser;
import galois.parser.postgresql.PostgresXMLParser;
import galois.planner.IQueryPlanner;
import galois.planner.postgresql.xml.PostgresXMLPlanner;
import speedy.persistence.relational.AccessConfiguration;

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
