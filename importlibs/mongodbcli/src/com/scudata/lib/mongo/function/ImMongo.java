package com.scudata.lib.mongo.function;

import java.io.File;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoDatabase;
import com.scudata.common.Logger;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.IResource;
import com.scudata.dm.Sequence;

public class ImMongo implements IResource {
	public MongoClient m_client;
	public MongoDatabase m_db;
	private Context ctx;
	
	// mongodb://[user:pwd@]ip:port/db?arg=v
	public ImMongo(Object[] objs, Context ctx) {
		this.ctx = ctx;

		try {
			String str = objs[0].toString();
			MongoClientOptions.Builder optionsBuilder = MongoClientOptions.builder();
//			if (objs.length>=2 && objs[1] instanceof Sequence){
//				Sequence seq = (Sequence)objs[1];
//				for(int i=0; i<seq.length(); i++){
//					Object item = seq.get(i+1);
//					if (i==0){
//						if (item instanceof Integer){						
//							optionsBuilder.connectTimeout(Integer.parseInt(item.toString()));
//						}else{
//							optionsBuilder.connectTimeout(3000);
//						}
//					}else if (i==1){
//						if (item instanceof Integer){						
//							optionsBuilder.socketTimeout(Integer.parseInt(item.toString()));
//						}else{
//							optionsBuilder.socketTimeout(3000);
//						}
//					}else if (i==2){
//						if (item instanceof Integer){						
//							optionsBuilder.serverSelectionTimeout(Integer.parseInt(item.toString()));
//						}else{
//							optionsBuilder.serverSelectionTimeout(3000);
//						}
//					}
//				}				
//			}
			
			char SEP = File.separatorChar;
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
			
			m_client = new MongoClient(conn);		
			m_client.startSession();
		
			m_db = m_client.getDatabase(conn.getDatabase());
			if (ctx != null) ctx.addResource(this);
		} catch (Exception e) {
			throw new RQException(e);
		}
	}

	@Override
	public void close() {
		if (ctx != null){
			ctx.removeResource(this);
		}
		m_client.close();		
	}
}
