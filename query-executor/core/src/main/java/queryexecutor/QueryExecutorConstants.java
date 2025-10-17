package queryexecutor;

import queryexecutor.model.database.ConstantValue;
import queryexecutor.model.database.IValue;

public class QueryExecutorConstants {

    public static String PRINT_SEPARATOR = "________________________________________________________________________________________________________________\n";

    public static double TRUE = 1.0;
    public static double FALSE = 0.0;

    public static String INDENT = "    ";
    public static String SECONDARY_INDENT = "  ";
    public static String FINGERPRINT_SEPARATOR = "|-|";

    public static String CONST = "constant";
    public static String NULL = "null";
    public static String LLUN = "llun";
    public static String NULL_VALUE = "NULL";

    public static final String SKOLEM_PREFIX = "_SK";
    public static final String BIGINT_SKOLEM_PREFIX = "888";
    public static final String DOUBLE_SKOLEM_PREFIX = "8.88";
    public static final String SKOLEM_SEPARATOR = "-";
    public static final Long MIN_BIGINT_SKOLEM_VALUE = 8880000000L;

    public static final String LLUN_PREFIX = "_L";
    public static final String BIGINT_LLUN_PREFIX = "887";
    public static final String REAL_LLUN_PREFIX = "7.77";
    public static final String LLUN_SEPARATOR = "|";
    public static final Long MIN_BIGINT_LLUN_VALUE = 8870000000L;

    public static final Long MIN_BIGINT_SAFETY_SKIP_VALUE = 20000000L;

    public static final int MIN_LENGTH_FOR_NUMERIC_PLACEHOLDERS = 10;

    private static String[] stringSkolemPrefixes = {QueryExecutorConstants.SKOLEM_PREFIX, "_N"};

    private static String[] numericSkolemPrefixes = {
        QueryExecutorConstants.BIGINT_SKOLEM_PREFIX,
        QueryExecutorConstants.DOUBLE_SKOLEM_PREFIX};

    private static String[] stringLlunPrefixes = {QueryExecutorConstants.LLUN_PREFIX, "_V"};

    private static String[] numericLlunPrefixes = {
        QueryExecutorConstants.BIGINT_LLUN_PREFIX,
        QueryExecutorConstants.REAL_LLUN_PREFIX};

    public static final String SUFFIX_SEPARATOR = "_";

    public static String STANDARD_QUERY_TYPE = "Standard Query";
    public static String SYMMETRIC_QUERY_TYPE = "Symmetric Query";
    public static String INEQUALITY_QUERY_TYPE = "Inequality Query";
    public static String SINGLE_TUPLE_QUERY_TYPE = "Single Tuple Query";

    public static String SAMPLE_STRATEGY_DBMS_STATS = "DBMS_STATS";
    public static String SAMPLE_STRATEGY_TABLE_SIZE = "TABLE_SIZE";

    // VALUE CONSTRAINTS
//    public static String NON_NUMERIC = "NON NUMERIC";
//    public static String NUMERIC = "NUMERIC";
    public static String STAR_VALUE = "*";
    public static IValue POSITIVE_INFINITY = new ConstantValue(Double.MAX_VALUE);
    public static IValue NEGATIVE_INFINITY = new ConstantValue(-Double.MAX_VALUE);
    public static IValue ZERO = new ConstantValue(0);

    public static String OID = "oid";
    public static String TID = "tid";
    public static String STEP = "step";
    public static String VERSION = "version";

    public static String DC = "DC";

    // CELL CHANGE TYPES
    public static final String VIOGEN_CHANGE = "Viogen change";
    public static final String OUTLIER_CHANGE = "Outlier change";
    public static final String RANDOM_CHANGE = "Random change";

    public static String CHASE_FORWARD = "f";
    public static String CHASE_BACKWARD = "b";
    public static String CHASE_USER = "u";
    public static String CHASE_STEP_ROOT = "r";
    public static String CHASE_STEP_TGD = "t";

    public static String DELTA_TABLE_SEPARATOR = "__";
    public static String NA_TABLE_SUFFIX = DELTA_TABLE_SEPARATOR + "NA";

    public static final String WORK_SCHEMA = "work";

    public static String VALUE_LABEL = "_@=";

    public static String GEN_GROUP_ID = "GEN";

    public static String AGGR = "aggr";
    public static String COUNT = "count";

    /////////////////////// OPERATORS
    public static String EQUAL = "==";
    public static String NOT_EQUAL = "!=";
    public static String GREATER = ">";
    public static String LOWER = "<";
    public static String GREATER_EQ = ">=";
    public static String LOWER_EQ = "<=";

    public static String DELTA_TMP_TABLES = "tmp_";
    public static String CLONE_SUFFIX = "_clone";
    public static String DIRTY_SUFFIX = "_dirty";

    public static long TEST_TIMEOUT = 300000;

    public static String CSV = "CSV";
    public static String XML = "XML";
    public static String WORK_DIR = ".temp";

    public static final String SINGLE_FILE = "singleFile";
    public static final String MULTIPLE_FILES = "multipleFiles";

    /////////////////////// INSTANCE COMPARISON
    public static IValue WILDCARD = new ConstantValue("*");

    public enum ValueMatchResult {
        EQUAL_CONSTANTS, BOTH_PLACEHOLDER, PLACEHOLDER_TO_CONSTANT, CONSTANT_TO_PLACEHOLDER, NOT_MATCHING
    }
    ///////////////    DEBUG MODE     ///////////////////
//    public static final boolean DBMS_DEBUG = true;
    public static final boolean DBMS_DEBUG = false;

    ///////////////    GETTER AND SETTER     ///////////////////
    public static String[] getStringSkolemPrefixes() {
        return stringSkolemPrefixes;
    }

    public static void setStringSkolemPrefixes(String[] s) {
        stringSkolemPrefixes = s;
    }

    public static String[] getStringLlunPrefixes() {
        return stringLlunPrefixes;
    }

    public static void setStringLlunPrefixes(String[] s) {
        stringLlunPrefixes = s;
    }

    public static String[] getNumericSkolemPrefixes() {
        return numericSkolemPrefixes;
    }

    public static void setNumericSkolemPrefixes(String[] s) {
        numericSkolemPrefixes = s;
    }

    public static String[] getNumericLlunPrefixes() {
        return numericLlunPrefixes;
    }

    public static void setNumericLlunPrefixes(String[] numericLlunPrefixes) {
        numericLlunPrefixes = numericLlunPrefixes;
    }
}
