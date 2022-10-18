package com.scudata.lib.mongo.function;

import org.bson.Document;
import com.scudata.common.*;
import com.scudata.util.JSONUtil;
import com.scudata.dm.*;

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
			Document command = null;
			command = Document.parse(cmd);
			Document docs = mongo.m_db.runCommand(command);
			double dVal = docs.getDouble("ok");
			if (dVal==0){
				System.out.println("no data");
				return obj;
			}
			
			//游标的实现
			Record rootNode = null;
			if (option!=null && option.contains("d")){
				if (option.contains("c")){
					return doCursorData(mongo, docs);
				}
				//非游标的实现
				Document cur = (Document)docs.get("cursor");				
				if (cur==null){
					rootNode = ImCursor.parse(docs);
					return rootNode;
				}
				obj=doNormalData(mongo, cur);
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
	private Object doNormalData(ImMongo mongo, Document cur) {
		Object obj = null;		
		Record rcd = ImCursor.parse(cur);
		
		if (rcd.dataStruct().getFieldIndex("firstBatch")>-1){
			obj = rcd.getFieldValue("firstBatch");
			if (obj instanceof Sequence){
				if ( ((Sequence)obj).length()==0){
					obj = rcd;
				}
			}
		}
		
		String cmd = null;
		long pid = cur.getLong("id");
		if (pid>0){
			String collectName = cur.getString("ns");
			collectName= collectName.replace(mongo.m_db.getName()+".", "");
			cmd = String.format("{'getMore':NumberLong('%d'), 'collection':'%s', batchSize:101 }", pid, collectName);
		}
		
		Table bufTbl = null;
		if (obj instanceof Table){
			bufTbl = (Table)obj;
		}
		
		ImCursor cursor = new ImCursor(mongo, cmd, bufTbl, m_ctx);
		return cursor.fetch();
	}
	
	//将当前的数据缓存后放入游标中
	private Object doCursorData(ImMongo mongo, Document docs) {
		Record r = null;
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
		
		String cmd = null;
		long pid = cur.getLong("id");
		if (pid>0){
			String collectName = cur.getString("ns");
			collectName= collectName.replace(mongo.m_db.getName()+".", "");
			cmd = String.format("{'getMore':NumberLong('%d'), 'collection':'%s', batchSize:101 }", pid, collectName);
		}
		
		Table bufTbl = null;
		if (obj instanceof Table){
			bufTbl = (Table)obj;
		}
		return new ImCursor(mongo, cmd, bufTbl, m_ctx);		
	}
	
	
}
