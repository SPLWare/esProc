package com.scudata.lib.mongo.function;

import java.io.File;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.scudata.common.Logger;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.IResource;

public class ImMongo implements IResource {
	private Context m_ctx;
	public MongoClient m_client;
	public String m_dbName;
	
	// mongodb://[user:pwd@]ip:port/db?arg=v
	public ImMongo(Object[] objs, Context ctx) {
		this.m_ctx = ctx;

		try {
			String str = objs[0].toString();
			char SEP = File.separatorChar;
			MongoClientOptions.Builder optionsBuilder = MongoClientOptions.builder();
			
			if (objs.length>=2 ){
				String skey = null;
				String sval = null;
				
				if (objs[1] instanceof Object[]){
					Object[] vs = (Object[])objs[1];					
					//Logger.info("java = "+ System.getProperty("java.home") );
					if (vs.length>=1 && vs[0] instanceof String){
						skey = System.getProperty("java.home") + SEP + "lib" + SEP + "security"+SEP+ vs[0].toString();
					}
					if (vs.length>=2 && vs[1] instanceof String){
						sval = vs[1].toString();
					}
				}else if(objs[2] instanceof String){
					skey = System.getProperty("java.home") + SEP + "lib" + SEP + "security"+SEP+ objs[2].toString();
				}
				
				File f = new File(skey);
				if (f.exists()){
					System.setProperty("javax.net.ssl.trustStore", skey);
				}else{
					String trust = System.getProperty("java.home") + SEP + "lib" + SEP + "security"+SEP+"cacerts";
					System.setProperty("javax.net.ssl.trustStore", trust);
				}
				
		        if (sval!=null){
		        	System.setProperty("javax.net.ssl.trustStorePassword", sval);
		        }else{
		        	System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
		        }
		        optionsBuilder.sslEnabled(true).sslInvalidHostNameAllowed(true);
			}else{
				String skey = System.getProperty("java.home") + SEP + "lib" + SEP + "security"+SEP+ "cacerts";
				File file = new File(skey);
				if(file.exists()){
					System.setProperty("javax.net.ssl.trustStore", skey);
			        System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
				}
			}
			
			if (str.toLowerCase().contains("ssl=true")){
				optionsBuilder.sslEnabled(true).sslInvalidHostNameAllowed(true);
			}
			
			MongoClientURI conn = new MongoClientURI(str, optionsBuilder);
			m_dbName = conn.getDatabase();
			m_client = new MongoClient(conn);	
			MongoClientOptions.builder().connectionsPerHost(50).build();//每个主机的连接数
			
			if (ctx != null) ctx.addResource(this);
		} catch (Exception e) {
			throw new RQException(e);
		}
	}

	public void close() {
		try {
			if (m_ctx != null){
				m_ctx.removeResource(this);
				m_ctx = null;
			}
		} catch (Exception e) {
			Logger.error(e.getStackTrace());
		}
	}
	
	public void dbClose() {
		try {
			if(m_client!=null){
				m_client.close();
				m_client = null;
			}
		} catch (Exception e) {
			Logger.error(e.getStackTrace());
		}
	}
}
