package com.raqsoft.lib.mongo.function;

import org.bson.Document;
import com.mongodb.client.MongoDatabase;
import com.raqsoft.common.*;
import com.raqsoft.util.JSONUtil;
import com.raqsoft.dm.*;

// mdb.shell@x(s)
public class ImShell extends ImFunction {
	
	protected Object doQuery(Object[] objs) {
		ImMongo mongo = null;
		try {
			if (objs==null || objs.length!=2){
				throw new Exception("Shell paramSize error!");
			}
			if (objs[0] instanceof ImMongo){
				mongo = (ImMongo)objs[0];
			}else{
				throw new Exception("Shell paramType error");
			}
			
			return runCommand(mongo, objs[1].toString());
		} catch (Exception e) {
			Logger.error(e.getMessage());
		}
		
		return null;
	}
	
	public Object runCommand(ImMongo mongo, String cmd) {
		Object obj = null;
		try{
			long pid = 0;
			Document command = null;
			MongoDatabase db = mongo.m_db;
			command = Document.parse(cmd);
			Document docs = db.runCommand(command);
			double dVal = docs.getDouble("ok");
			if (dVal==0){
				return obj;
			}
			Document cur = (Document)docs.get("cursor");
			String s = null;
			
			if (cur==null){
				s = docs.toJson();
				char[] chars = s.toCharArray();
				Object r = JSONUtil.parseJSON(chars, 0, chars.length - 1);
				return r;
			}
			
			s = cur.toJson();
			char[] chars = s.toCharArray();
			Object r = JSONUtil.parseJSON(chars, 0, chars.length - 1);

			if (r instanceof Record){
				Record rcd = (Record)r;
				if (rcd.dataStruct().getFieldIndex("firstBatch")>-1){
					obj = rcd.getFieldValue("firstBatch");
					//System.out.println(obj);
					if (obj instanceof Sequence){
						if ( ((Sequence)obj).length()==0){
							obj = r;
						}
					}
				}
			}	
			
			pid = cur.getLong("id");
			if (pid>0){ //for cursor
				String collectName = cur.getString("ns");
				collectName= collectName.replace(db.getName()+".", "");
				cmd = String.format("{'getMore':NumberLong('%d'), 'collection':'%s'}", pid, collectName);
				Table bufTbl = null;
				if (obj instanceof Table){
					bufTbl = (Table)obj;
				}
				return new ImCursor(mongo, cmd, bufTbl, m_ctx);
			}
		}catch(Exception e){
			String info = e.getMessage();
			if (info.indexOf("getLastError")>0){
				info = info.substring(info.indexOf("{"));
				char[] chars = info.toCharArray();
				obj = JSONUtil.parseJSON(chars, 0, chars.length - 1);
			}else{			
				Logger.error(e.getMessage());
			}
		}
		
		return obj;
	}
}
