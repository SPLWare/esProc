package org.olap4j.driver.xmla;

import org.olap4j.*;
import org.olap4j.driver.xmla.proxy.*;
import org.olap4j.impl.Named;
import org.w3c.dom.*;
import org.xml.sax.SAXException;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import static org.olap4j.driver.xmla.XmlaOlap4jUtil.*;

abstract class XmlaOlap4jConnectionProxy extends FactoryJdbc4Plus.AbstractConnection {
    public static DatabaseType dbType=DatabaseType.SSAS; //
    private static final boolean DEBUG = false;

    XmlaOlap4jConnectionProxy(
        Factory factory,
        XmlaOlap4jDriver driver,
        XmlaOlap4jProxy proxy,
        String url,
        Properties info)
        throws SQLException
    {
    	super(factory,driver,proxy,url,info);       
    }

    /**
     * Returns the error-handler
     * @return Error-handler
     */
    private XmlaHelper getHelper() {
        return helper;
    }
    
    /**
     * Executes an XMLA metadata request and returns the root element of the
     * response.
     *
     * @param request XMLA request string
     * @return Root element of the response
     * @throws OlapException on error
     */
    static int testCount = 0;
    Element executeMetadataRequest(String request) throws OlapException {
    	//System.out.println("TestCount = "+testCount++);
        byte[] bytes;
        if (DEBUG) {
            System.out.println("********************************************");
            System.out.println("** SENDING REQUEST :");
            System.out.println(request);
        }

        try {
            bytes = proxy.get(serverInfos, request);
        } catch (XmlaOlap4jProxyException e) {
            throw getHelper().createException(
                "This connection encountered an exception while executing a query.",
                e);
        }
        Document doc;
        try {
            doc = parse(bytes);
        } catch (IOException e) {
            throw getHelper().createException(
                "error discovering metadata", e);
        } catch (SAXException e) {
            throw getHelper().createException(
                "error discovering metadata", e);
        }

        final Element envelope = doc.getDocumentElement();
        if (DEBUG) {
            System.out.println("** SERVER RESPONSE :");
            System.out.println(XmlaOlap4jUtil.toString(doc, true));
        }
        assert envelope.getLocalName().equals("Envelope");
        assert envelope.getNamespaceURI().equals(SOAP_NS);
        Element header =
            findChild(envelope, SOAP_NS, "Header");
        Element body =
            findChild(envelope, SOAP_NS, "Body");
        Element fault =
            findChild(body, SOAP_NS, "Fault");
        if (fault != null) {
           return fault;
        }
        if (header != null) {
            Element session =
                findChild(header, XMLA_NS, "Session");
            if (session != null) {
                String sessionId =
                    session.getAttribute("SessionId");
                if ("".equals(sessionId)) {
                    sessionId = null;
                }
                serverInfos.setSessionId(sessionId);
            }
        }
        Element discoverResponse =
            findChild(body, XMLA_NS, "DiscoverResponse");
        Element returnElement =
            findChild(discoverResponse, XMLA_NS, "return");
        return findChild(returnElement, ROWSET_NS, "root");
    }
    
    <T extends Named> void populateList(
            List<T> list,
            Context context,
            MetadataRequest metadataRequest,
            Handler<T> handler,
            Object[] restrictions) throws OlapException
        {
            String request = generateRequest(context, metadataRequest, restrictions);
//            String vals = "";            
//            for(Object o:restrictions){
//            	vals+= o.toString()+"; ";
//            }
//            System.out.println("TestCount = "+testCount+";metadataRequest="+ metadataRequest+"; restrictions="+vals);
            Element root = executeMetadataRequest(request);
            for (Element o : childElements(root)) {
                if (o.getLocalName().equals("row")) {
                	if (!handler.toString().contains("MeasureHandler")){
                		 handler.handle(o, context, list);
                	}                   
                }
            }
            handler.sortList(list);
        }
    
    public enum DatabaseType {
        SSAS,
        SAP,
        ORACLE,
        ESS
    }
}

// End XmlaOlap4jConnectionProxy.java
