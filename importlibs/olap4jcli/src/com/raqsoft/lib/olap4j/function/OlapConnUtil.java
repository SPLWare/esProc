package com.raqsoft.lib.olap4j.function;
import java.sql.Connection;
//https://sourceforge.net/p/olap4j/discussion/577988/thread/93729c12/
import java.sql.DriverManager;  
import java.sql.SQLException;  
  
import org.olap4j.OlapConnection;  
import org.olap4j.OlapWrapper;
import com.raqsoft.common.Logger;  
  
public class OlapConnUtil {  
  
    private static final String DRIVER_CLASS_NAME = "org.olap4j.driver.xmla.XmlaOlap4jDriverProxy";  
    
    static {  
        try {  
            Class.forName(DRIVER_CLASS_NAME);  
        } catch (ClassNotFoundException e) {  
        	Logger.error(e.getStackTrace());  
        }  
    }  
  
	public static OlapConnection getOlapConn(String server, String catalog,  
            String user, String password) throws SQLException {  
        String url = "jdbc:xmla:Server=" + server;  
  
        OlapConnection conn = null;  
        try {
        	Connection connection = DriverManager.getConnection(url, user, password);  
            if (connection != null) {  
            	conn = connection.unwrap(OlapConnection.class);
            	if (catalog!=null){
            		conn.setCatalog(catalog);  
            	}
            }
        } catch (SQLException e) {  
            throw e;  
        }
        
        return conn;  
    }  
  
}  