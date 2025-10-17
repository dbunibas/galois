package bsf.test.experiments;

import bsf.parser.IQueryPlanParser;
import bsf.parser.postgresql.PostgresXMLParser;
import bsf.planner.IQueryPlanner;
import bsf.planner.postgresql.xml.PostgresXMLPlanner;
import queryexecutor.persistence.relational.AccessConfiguration;

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
