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
				return obj;
			}
			
			//游标的实现
			if (option!=null && option.contains("c")){
				return doCursorData(mongo, docs);
			}
			
			//非游标的实现
			Document cur = (Document)docs.get("cursor");
			Record rootNode = null;
			if (cur==null){
				rootNode = ImCursor.parse(cur);
				return rootNode;
			}
			
			rootNode = ImCursor.parse(cur);
			//获取表数据			
			if (rootNode.dataStruct().getFieldIndex("firstBatch")>-1){
				obj = rootNode.getFieldValue("firstBatch");
				if (obj instanceof Sequence){
					if ( ((Sequence)obj).length()==0){
						obj = rootNode;
					}
				}
			}
				
		}catch(Exception e){
			String info = e.getMessage();
			if (info.indexOf("getLastError")>0){
				info = info.substring(info.indexOf("{"));
				char[] chars = info.toCharArray();
				obj = JSONUtil.parseJSON(chars, 0, chars.length - 1);
				//游标选项的实现
				if (option!=null && option.contains("c")){
					Table bufTbl = null;
					if (obj instanceof Record){
						Record r = (Record)obj;
						bufTbl = new Table(r.dataStruct());
						bufTbl.newLast(r.getFieldValues());
					}
					return new ImCursor(mongo, null, bufTbl, m_ctx);		
				}
			}else{			
				Logger.error(e.getMessage());
			}
		}
		
		return obj;
	}
	
	//将当前的数据缓存后放入游标中
	private Object doCursorData(ImMongo mongo, Document docs) {
		Object r = null;
		String s = null;
		Document cur = (Document)docs.get("cursor");
				
		if (cur==null){
			s = docs.toJson();
			char[] chars = s.toCharArray();
			r = JSONUtil.parseJSON(chars, 0, chars.length - 1);
		}else{		
			s = cur.toJson();
			char[] chars = s.toCharArray();
			r = JSONUtil.parseJSON(chars, 0, chars.length - 1);
		}
		
		Object obj = null;
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
