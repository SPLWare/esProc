package com.raqsoft.lib.sap.function;

import com.raqsoft.common.Logger;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.FileObject;
import com.sap.conn.jco.*;
import com.sap.conn.jco.ext.Environment;
import java.io.InputStreamReader;
import java.util.Properties;

public final class RfcManager {
    private static final String ABAP_AS_POOLED = "ABAP_AS_POOL";
    private JCOProvider provider;
    private JCoDestination destination;
    static {
    	// Properties properties = loadProperties(); 
		// catch IllegalStateException if an instance is already registered
        try {
        	String jcoPath = System.getProperty("JAVA_SAPJCO");
    		if (jcoPath == null){
				String path = System.getProperty("user.dir");	
				path = path.replace("\\bin", "\\lib");
				System.setProperty("JAVA_SAPJCO=",path);
    		}
        } catch (IllegalStateException e) {
            Logger.error(e.getMessage());
        }
    }
    
    public RfcManager(){
    		
    }
    
    public RfcManager(String fileName, String charset){
    	FileObject fo = new FileObject(fileName);
    	if (charset !=null){
    		fo.setCharset(charset);
    	}
    	
    	init(fo);
    }

	public void init(FileObject fo){
    	try {			
			if (fo == null){
				throw new RQException("fileName is not existed");
			}

			Properties props = new Properties();
			if (fo.getCharset() != null){
				InputStreamReader ist = new InputStreamReader(fo.getInputStream(), fo.getCharset());
				props.load(ist);
			}else{
				props.load(fo.getInputStream());
			}
			provider = new JCOProvider();

			if (!Environment.isDestinationDataProviderRegistered()){
				try
				{
					Environment.registerDestinationDataProvider(provider);
				}
				catch(IllegalStateException providerAlreadyRegisteredException)
				{
					throw new Error(providerAlreadyRegisteredException);
				}
			}
            provider.changePropertiesForABAP_AS(ABAP_AS_POOLED, props);
		} catch (Exception e) {
			Logger.error(e.getStackTrace());
		}
    }
  
    public RfcManager(String user, String passwd, String ashost, String sysnr,String client, String lang, String route) {
        try {
        	Properties props=new Properties();
            props.setProperty("jco.client.user",user);
            props.setProperty("jco.client.passwd",passwd);
            props.setProperty("jco.client.ashost",ashost);            
            props.setProperty("jco.client.client",client);
            props.setProperty("jco.client.sysnr",sysnr);
            props.setProperty("jco.client.lang", lang);
            if (route!=null && !route.isEmpty()){
            	props.setProperty("jco.client.saprouter", route);
            }
            
            props.setProperty("jco.destination.peak_limit","3");
            props.setProperty("jco.destination.pool_capacity","11");
            
            provider = new JCOProvider();
            if (!Environment.isDestinationDataProviderRegistered()){
				Environment.registerDestinationDataProvider(provider);
			}
            provider.changePropertiesForABAP_AS(ABAP_AS_POOLED, props);
        } catch (IllegalStateException e) {
            Logger.info(e.getMessage());
        }
    }

    public JCoDestination getDestination() throws JCoException {
        if (destination == null) {
            destination = JCoDestinationManager.getDestination(ABAP_AS_POOLED);
        }
        return destination;
    }

    public void execute(JCoFunction function) {
        try {
            function.execute(getDestination());
        } catch (JCoException e) {
            e.printStackTrace();
        }
    }

	public JCoFunction getFunction(String functionName) {
        JCoFunction function = null;
        try {
            function = getDestination().getRepository().getFunctionTemplate(functionName).getFunction();
        } catch (JCoException e) {
        	e.printStackTrace();
        } catch (NullPointerException e) {
        	e.printStackTrace();
        }
        return function;
    }
	
	public void close(){
		if (provider!=null){
			provider.changePropertiesForABAP_AS(ABAP_AS_POOLED, null);
			Environment.unregisterDestinationDataProvider(provider);
			provider = null;
		}
	}
}