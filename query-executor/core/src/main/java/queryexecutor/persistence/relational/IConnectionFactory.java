package queryexecutor.persistence.relational;

import queryexecutor.exceptions.DAOException;
import java.sql.Connection;

public interface IConnectionFactory {
            
    public Connection getConnection(AccessConfiguration accessConfiguration) throws DAOException;

    public void close();

}