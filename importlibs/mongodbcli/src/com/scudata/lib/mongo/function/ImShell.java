package com.scudata.lib.mongo.function;

import org.bson.Document;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.scudata.common.*;
import com.scudata.util.JSONUtil;
import com.scudata.dm.*;

// mdb.shell(s)
public class ImShell extends ImFunction {
	
	protected Object doQuery(Object[] objs) {
		try {
			if (objs==null || objs.length!=2){
				throw new Exception("Shell paramSize error!");
			}
			
			MongoDatabase db=null;
			if (objs[0] instanceof ImMongo){
				ImMongo mongo = (ImMongo)objs[0];
				if (db==null)
				{
					if (option!=null && option.contains("m")){							
						m_ctx.setParamValue("client", mongo.m_client);
						return new ImThreadCursor(mongo.m_client, mongo.m_dbName, objs[1].toString(), m_ctx);
					}else{
						db = mongo.m_client.getDatabase(mongo.m_dbName);
					}
				}
			}else{
				throw new Exception("Shell paramType error");
			}
			//System.out.println("thread:"+Thread.currentThread().getId()+"; ctx="+m_ctx+"; ImShell="+this+"; db="+m_db);
			if (db==null){
				throw new Exception("db is null.");
			}
			return runCommand(db, objs[1].toString());			
		} catch (Exception e) {
			Logger.error("error : "+e.getMessage());
		}
		
		return null;
	}
	
	public Object runCommand(MongoDatabase db, String cmd) {
		Object obj = null;
		try{
			Document command = null;
			command = Document.parse(cmd);
			Document docs = db.runCommand(command);
			double dVal = docs.getDouble("ok");
			if (dVal==0){
				System.out.println("no data");
				return obj;
			}
			
			//游标的实现
			BaseRecord rootNode = null;
			if (option!=null && option.contains("d")){
				if (option.contains("c")){
					return doCursorData(db, docs);
				}
				//非游标的实现
				Document cur = (Document)docs.get("cursor");				
				if (cur==null){
					rootNode = ImCursor.parse(docs);
					return rootNode;
				}
				obj=doNormalData(db, cur);
			}else{
				return ImCursor.parse(docs);
			}
		}catch(Exception e){
			String info = e.getMessage();
			if (info.indexOf("{")>-1 && info.indexOf("}")>0){
				info = info.substring(info.indexOf("{"));
				char[] chars = info.toCharArray();
				obj = JSONUtil.parseJSON(chars, 0, chars.length - 1);
			}else{			
				Logger.error(info);
			}
		}
		
		return obj;
	}
	
	//将当前的数据缓存后放入序表中
	private Object doNormalData(MongoDatabase db, Document cur) {
		try{
			Object obj = null;		
			BaseRecord rcd = ImCursor.parse(cur);
			
			if (rcd.dataStruct().getFieldIndex("firstBatch")>-1){
				obj = rcd.getFieldValue("firstBatch");
				if (obj instanceof Sequence){
					if ( ((Sequence)obj).length()==0){
						obj = rcd;
					}
				}
			}
			
			String cmd[] = new String[2];
			long pid = cur.getLong("id");
			if (pid>0){
				String collectName = cur.getString("ns");
				collectName= collectName.replace(db.getName()+".", "");
				cmd[0] = String.format("{'getMore':NumberLong('%d'), 'collection':'%s', batchSize:101 }", pid, collectName);
			}
			
			Table bufTbl = null;
			if (obj instanceof Table){
				bufTbl = (Table)obj;
			}
			
			ImCursor cursor = new ImCursor(db, cmd, bufTbl, m_ctx);
			return cursor.fetch();
		}catch(Exception e){
			Logger.error(e.getMessage());
		}
		
		return null;
	}
	
	//将当前的数据缓存后放入游标中
	private Object doCursorData(MongoDatabase db, Document docs) {
		BaseRecord r = null;
		Object obj = null;
		Document cur = (Document)docs.get("cursor");
				
		if (cur==null){
			r = ImCursor.parse(docs);
		}else{
			r = ImCursor.parse(cur);
		}
		
		if (r.dataStruct().getFieldIndex("firstBatch")>-1){
			obj = r.getFieldValue("firstBatch");
			if (obj instanceof Sequence){
				if ( ((Sequence)obj).length()==0){
					obj = r;
				}
			}
		}
		
		String cmd[] = new String[2];
		long pid = cur.getLong("id");
		if (obj!=null && pid>0){
			String collectName = cur.getString("ns");
			collectName= collectName.replace(db.getName()+".", "");
			cmd[0] = String.format("{'getMore':NumberLong('%d'), 'collection':'%s', batchSize:101 }", pid, collectName);
			cmd[1] = String.format("{'killCursors':'%s', 'cursors':[ NumberLong('%d')]}", collectName, pid);
		}
		
		Table bufTbl = null;
		if (obj instanceof Table){
			bufTbl = (Table)obj;
		}
		return new ImCursor(db, cmd, bufTbl, m_ctx);		
	}
	
	
}
