package com.raqsoft.lib.mongo.function;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.IResource;

public class ImMongo implements IResource {
	public MongoClient m_client;
	public MongoDatabase m_db;
	private Context ctx;
	
	// mongodb://[user:pwd@]ip:port/db?arg=v
	public ImMongo(String str, Context ctx) {
		this.ctx = ctx;

		try {			
			MongoClientURI conn = new MongoClientURI(str);
			m_client = new MongoClient(conn);
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
