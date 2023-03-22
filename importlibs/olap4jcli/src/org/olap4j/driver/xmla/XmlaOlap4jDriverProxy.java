package org.olap4j.driver.xmla;

import java.sql.*;
import java.util.*;
import org.olap4j.driver.xmla.proxy.XmlaOlap4jProxy;
import com.scudata.common.Logger;

public class XmlaOlap4jDriverProxy extends XmlaOlap4jDriver {

    private final Factory factory;

    static {
        try {
            register();
        } catch (SQLException e) {
            Logger.error(e.getMessage());
        } catch (RuntimeException e) {
            Logger.error(e.getMessage());
            throw e;
        }
    }

    /**
     * Creates an XmlaOlap4jDriver.
     */
    public XmlaOlap4jDriverProxy() {
        factory = createFactory();
    }
    
    private static Factory createFactory() {
        final String factoryClassName = getFactoryClassName();
        try {
            final Class<?> clazz = Class.forName(factoryClassName);
            return (Factory) clazz.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String getFactoryClassName() {
        try {
            // If java.sql.PseudoColumnUsage is present, we are running JDBC 4.1 or later.
            Class.forName("java.sql.PseudoColumnUsage");
            return "org.olap4j.driver.xmla.FactoryJdbc41ImplProxy";
        } catch (ClassNotFoundException e) {
            // java.sql.PseudoColumnUsage is not present. This means we are
            // running JDBC 4.0 or earlier.
            try {
                Class.forName("java.sql.Wrapper");
                return "org.olap4j.driver.xmla.FactoryJdbc4ImplProxy";
            } catch (ClassNotFoundException e2) {
                // java.sql.Wrapper is not present. This means we are running
                // JDBC 3.0 or earlier (probably JDK 1.5). Load the JDBC 3.0
                // factory.
                return "org.olap4j.driver.xmla.FactoryJdbc3Impl";
            }
        }
    }

    /**
     * first deregister XmlaOlap4jDriver 
     * Registers an instance of XmlaOlap4jDriverProx.
     *
     * <p>Called implicitly on class load, and implements the traditional
     * 'Class.forName' way of registering JDBC drivers.
     *
     * @throws SQLException on error
     */
    private static void register() throws SQLException {
    	Enumeration<Driver> ds = DriverManager.getDrivers();
    	Driver d = null;
    	while(ds.hasMoreElements()){
    		d = ds.nextElement();
    		if (d.toString().indexOf("org.olap4j.driver.xmla")>-1){
    			DriverManager.deregisterDriver(d);
    	    	d = null;
    		}    		
    	}
    	
        DriverManager.registerDriver(new XmlaOlap4jDriverProxy());
    }

    public Connection connect(String url, Properties info) throws SQLException {
        if (!XmlaOlap4jConnection.acceptsURL(url)) {
            return null;
        }

        // Parses the connection string
        Map<String, String> map =
            XmlaOlap4jConnection.parseConnectString(url, info);

        // Creates a connection proxy
        XmlaOlap4jProxy proxy = createProxy(map);
        // returns a connection object to the java API
        return factory.newConnection(this, proxy, url, info);
    }
 
}

// End XmlaOlap4jDriver.java
