package engine.persistence.relational;

import engine.exceptions.DAOException;
import java.sql.Connection;

public interface IConnectionFactory {
            
    public Connection getConnection(AccessConfiguration accessConfiguration) throws DAOException;

    public void close();

}