package queryexecutor.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import queryexecutor.exceptions.DAOException;
import queryexecutor.model.database.mainmemory.datasource.NullValueFactory;

public class Types {

    private static Logger logger = LoggerFactory.getLogger(Types.class);

    public final static String BOOLEAN = "boolean";
    public final static String STRING = "string";
    public final static String INTEGER = "integer";
    public final static String LONG = "long";
    public final static String REAL = "real";
    public final static String DOUBLE_PRECISION = "double precision";
    public final static String DATE = "date";
    public final static String DATETIME = "datetime";
    public final static String ANY = "any";

    public static Object getTypedValue(String type, Object value) throws DAOException {
        if (value == null || value.toString().equalsIgnoreCase("NULL")) {
            return NullValueFactory.getNullValue();
        }
        String stringValue = value.toString();
        if (type.equals(BOOLEAN)) {
            if(stringValue.equalsIgnoreCase("YES")) return Boolean.TRUE;
            if(stringValue.equalsIgnoreCase("NO")) return Boolean.FALSE;
            return Boolean.parseBoolean(stringValue);
        }
        if (type.equals(STRING)) {
            return stringValue;
        }
        if (type.equals(INTEGER)) {
            try {
                String valueS = stringValue;
                logger.debug("Value to parse: " + valueS);
                double d = Double.parseDouble(stringValue);
                if (d == Math.floor(d)) {
                    //integer value
                    return (int)d;
                } else {
                    throw new DAOException("Not Integer Value: " + valueS);
                }
            } catch (NumberFormatException ex) {
                logger.error(ex.getLocalizedMessage());
                throw new DAOException(ex.getMessage());
            }
        }
        if (type.equals(LONG)) {
            try {
                String valueS = stringValue;
                logger.debug("Value to parse: " + valueS);
                double d = Double.parseDouble(stringValue);
                if (d == Math.floor(d)) {
                    //integer value
                    return (long)d;
                } else {
                    throw new DAOException("Not Long Value: " + valueS);
                }
            } catch (NumberFormatException ex) {
                logger.error(ex.getLocalizedMessage());
                throw new DAOException(ex.getMessage());
            }
        }
        if (type.equals(REAL)) {
            try {
                return Double.parseDouble(stringValue);
            } catch (NumberFormatException ex) {
                logger.error(ex.getLocalizedMessage());
                throw new DAOException(ex.getMessage());
            }
        }
        if (type.equals(DOUBLE_PRECISION)) {
            try {
                return Double.parseDouble(stringValue);
            } catch (NumberFormatException ex) {
                logger.error(ex.getLocalizedMessage());
                throw new DAOException(ex.getMessage());
            }
        }
        if (type.equals(DATE) || type.equals(DATETIME)) {
            return stringValue;
//            try {
//                return DateFormat.getDateInstance().parse(value.toString());
//            } catch (ParseException ex) {
//                logger.error(ex);
//                throw new DAOException(ex.getMessage());
//            }
        }
        return stringValue;
    }
    
    public static boolean isNumerical(String type) {
        String[] numericalTypes = {INTEGER, LONG, REAL, DOUBLE_PRECISION};
        for (String numericalType : numericalTypes) {
            if (type.equalsIgnoreCase(numericalType)) return true;
        }
        return false;
    }
    
    public static boolean checkType(String type, String value) {
        try {
            Object typedValue = getTypedValue(type, value);
            if (typedValue != null) return true;
        } catch (DAOException daoException) {
            return false;
        }
        return false;
    }

}
