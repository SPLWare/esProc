package org.olap4j.driver.xmla;

import java.sql.SQLException;
import java.util.Properties;
import org.olap4j.OlapException;
import org.olap4j.driver.xmla.proxy.XmlaOlap4jProxy;

class FactoryJdbc4PlusProxy {
    public FactoryJdbc4PlusProxy() {

    }

    static abstract class AbstractConnection
        extends XmlaOlap4jConnectionProxy
    {
        /**
         * Creates an AbstractConnection.
         *
         * @param factory Factory
         * @param driver Driver
         * @param proxy Proxy
         * @param url URL
         * @param info Extra properties
         * @throws SQLException on error
         */
        AbstractConnection(
            Factory factory,
            XmlaOlap4jDriver driver,
            XmlaOlap4jProxy proxy,
            String url,
            Properties info) throws SQLException
        {
            super(factory, driver, proxy, url, info);
        }
    }


    static abstract class AbstractCellSet extends XmlaOlap4jCellSetProxy {
        /**
         * Creates an AbstractCellSet.
         *
         * @param olap4jStatement Statement
         * @throws OlapException on error
         */
        AbstractCellSet(
            XmlaOlap4jStatement olap4jStatement)
            throws OlapException
        {
            super(olap4jStatement);
        }       
    }

}

// End FactoryJdbc4PlusProxy.java
