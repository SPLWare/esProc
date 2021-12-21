package com.scudata.lib.mongo.function;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.bson.Document;
import com.mongodb.client.MongoDatabase;
import com.scudata.common.*;
import com.scudata.dm.*;
import com.scudata.dm.cursor.ICursor;
import com.scudata.util.JSONUtil;

public class ImCursor extends ICursor {
	private String m_cmd;
	private ImMongo m_mongo;
	private String[] m_cols;
	private Table m_bufTable;
	private long cursorId = 1; //若为0，则无数据了。
	
	public ImCursor(ImMongo mongo, String cmd, Table buf, Context ctx) {
		m_cmd = cmd;
		m_mongo = mongo;
		this.ctx = ctx;
		m_bufTable = buf;
		if (m_bufTable!=null){
			m_cols = m_bufTable.dataStruct().getFieldNames();
		}
		if (ctx != null) {
			ctx.addResource(this);
		}		
	}

	protected long skipOver(long n) {
		if (m_mongo == null || n == 0){
			return 0;
		}
		long fetchSize = n;
		long count = 0;
		//1。有缓存情况
		if (m_bufTable!=null){
			if (m_bufTable.length()>fetchSize){ //1.1。有足够缓存
				for(int i=(int)fetchSize; i>0; i--){
					m_bufTable.delete(i);
				}
				return fetchSize;
			}else{			//1.2. 缓存数不足	
				fetchSize -= m_bufTable.length();
				count=m_bufTable.length();
				m_bufTable.clear();		
				m_bufTable = null;				
			}
		}
		//2.查询新数据
		while (fetchSize>0 && cursorId>0) {
			Table buf = runCommand(m_mongo.m_db, m_cmd);
			if (buf==null){
				break;
			}
			
			if (buf.length()>fetchSize){ //2.1 有足够缓存
				for(int i=(int)fetchSize; i>0; i--){
					buf.delete(i);
				}
				//2.2剩余存入缓冲.
				m_bufTable = new Table(buf.dataStruct());
				m_bufTable.addAll(buf);
				return n;
			}else{			//2.3  缓存数不足	
				count+=buf.length();
				fetchSize -= buf.length();
				buf.clear();
				buf = null;
			}			
		}
		
		return count;
	}
	
	//还不清楚getMore关闭cursor方法，而不是断开连接.
	public synchronized void close() {
//		super.close();
//		
//		try {
//			if (ctx != null) ctx.removeResource(this);			
//			cursorId = 0;
//			if (m_mongo != null) {
//				m_mongo.close();
//				m_mongo = null;
//			}
//		} catch (Exception e) {
//			throw new RQException(e.getMessage(), e);
//		}
	}

	protected Sequence get(int n) {		
		Table vTbl = null;
		if (m_mongo == null || n < 1) {
			if (m_cols!=null){
				return new Table(m_cols);
			}else{
				return null;
			}
		}
		
		//1。有缓存情况
		if (m_bufTable!=null){
			vTbl = new Table(m_bufTable.dataStruct());
			if (m_bufTable.length()>n){ //1.1。有足够缓存
				for(int i=0; i<n; i++){
					vTbl.add(m_bufTable.get(i+1));					
				}
				for(int i=n; i>0; i--){
					m_bufTable.delete(i);
				}
				return vTbl;
			}else{			//1.2. 缓存数不足	
				n -= m_bufTable.length();
				vTbl.addAll(m_bufTable);
				m_bufTable.clear();		
				m_bufTable = null;
			}
		}
		
		//2. 查询新数据。
		while (n > 0 && cursorId>0) {
			Table buf = runCommand(m_mongo.m_db, m_cmd);
			if (buf==null){
				break;
			}
			if (vTbl==null){
				vTbl = new Table(buf.dataStruct());
			}
			
			if (buf.length()>n){ //2.1 有足够缓存
				for(int i=0; i<n; i++){
					Object o = buf.get(i+1);
					if (o instanceof Record){
						vTbl.newLast(((Record)o).getFieldValues());
					}else if(o instanceof Sequence){
						vTbl.addAll((Sequence)o);
					}
				}
				for(int i=n; i>0; i--){
					buf.delete(i);
				}
				//2.2 剩余存入缓冲.
				m_bufTable = new Table(buf.dataStruct());
				m_bufTable.addAll(buf);
				return vTbl;
			}else{			//2.3  缓存数不足	
				vTbl.addAll(buf);
				n -= buf.length();
				buf.clear();				
			}			
		}
		
		if (n > 0) {
			close();
		}
		if (vTbl==null && m_cols!=null){
			vTbl = new Table(m_cols);
		}

		return vTbl;
	}

	protected void finalize() throws Throwable {
		close();
	}
	
	public Table runCommand(MongoDatabase db, String cmd) {
		Table tbl = null;
		try{		
			if (cmd==null) {
				return null;
			}
			
			Object obj = null;
			Document command = null;
			command = Document.parse(cmd);
			Document docs = db.runCommand(command);
			double dVal = docs.getDouble("ok");
			if (dVal==0){
				return tbl;
			}
			Document cur = (Document)docs.get("cursor");
			cursorId = cur.getLong("id");
			Record rcd = parse(cur);

			if (rcd.dataStruct().getFieldIndex("firstBatch")>-1){
				obj = rcd.getFieldValue("firstBatch");
			}else if (rcd.dataStruct().getFieldIndex("nextBatch")>-1){
				obj = rcd.getFieldValue("nextBatch");
			}
			if (obj instanceof Table){
				tbl = (Table)obj;
				if (m_cols==null){
					m_cols = tbl.dataStruct().getFieldNames();
				}
			}
		}catch(Exception e){
			Logger.error(e.getMessage());
		}
		
		return tbl;
	}
	
	public static Record parse(Document doc){
		int idx = 0;
		Set<String>set = doc.keySet();
		DataStruct ds1 = new DataStruct(set.toArray(new String[set.size()]));
		
		Object[] line = new Object[set.size()];;
		for(String k:set){
			Object val = doc.get(k);			
			if (val instanceof Document){				
				Object subObj = parse((Document)val);
				line[idx++] = subObj;
			}else if(val instanceof List){
				List<?> list = (List<?>)val;
				if (list.size()==0){
					idx++;
					continue;
				}
				Object o = ((List<?>)val).get(0);
				// List<Document>结构
				if (o instanceof Document){
					Table subNode = null;
					Record subRec = null;
					List<Document> dlist = (List<Document>)val;
					
					for(Document sub:dlist){
						if (subNode == null){
							subRec = parse(sub);
							subNode = new Table(subRec.dataStruct());
							subNode.newLast(subRec.getFieldValues());
						}else{
							subRec = parse(sub);	
							subNode = doRecord(subNode, subRec);
						}
					}
					line[idx++] = subNode;
				}else{ // List<Object>结构
					Object[] objs = list.toArray(new Object[list.size()]);
					Sequence seq = new Sequence(objs);					
					line[idx++] = seq;
				}
			}else{
				line[idx++] = val;				
			}
		}

		Record rcd = new Record(ds1,line);
		return rcd;
	}
	
	//按数组原顺序去重合并数组
	private static String[] merge(String[] oldArr, String[] newArr){
        Map<String, Integer> map = new LinkedHashMap<String, Integer>();
       
        for (String anOldArr : oldArr) {
           map.put(anOldArr, 1);
        }
 
        for (String aNewArr : newArr) {
        	map.put(aNewArr, 1);
        }
        
        Set<String> set = map.keySet();
        String[] ss = set.toArray(new String[set.size()]);
        return ss;
    }
	
	//参数oldDs是否包含newDs
	private static boolean isDataStructContain(DataStruct oldDs, DataStruct newDs){
		boolean bRet = true;
		String[] oldArr = oldDs.getFieldNames();
		String[] newArr = newDs.getFieldNames();
		if (oldArr.length<newArr.length){
			bRet = false;
		}else{
			for(int i=0; i<newArr.length; i++){
				if (!ArrayUtils.contains(oldArr, newArr[i])){
					bRet = false;
					break;
				}
			}
		}
		
		return bRet;
	}
	
	//字段不一致时字段数据对齐
	private static Table doRecord(Table subNode, Record subRec) {
		Table ret = null;
		// 1.结构相同。
		if (subNode.dataStruct().isCompatible(subRec.dataStruct())){
			subNode.newLast(subRec.getFieldValues());	
			ret = subNode;			
		//2. 结构包括关系, 直接追加数据
		}else if(isDataStructContain(subNode.dataStruct(), subRec.dataStruct())){
			// newData
			Object[] subLine = null;
			String[] fullCols = subNode.dataStruct().getFieldNames();

			String[] cols = subRec.getFieldNames();
			subLine = new Object[fullCols.length];
			for(int j=0; j<fullCols.length; j++){	
				 if (ArrayUtils.contains(cols, fullCols[j])) {
					 subLine[j] = subRec.getFieldValue(fullCols[j]);
				 }
			}
			subNode.newLast(subLine);
			ret = subNode;
		//3. 结构不同时，重构序表
		}else{
			String[] newCols = merge(subNode.dataStruct().getFieldNames(), 
									 subRec.dataStruct().getFieldNames());
			Table newTable = new Table(newCols);
			ListBase1 mems = subNode.getMems();
			
			// oldData
			Record r = null;
			for(int i=0; i<mems.size(); i++){
				r = (Record)mems.get(i+1);
				newTable.newLast(r.getFieldValues());
			}
			// newData
			Object[] subLine = null;
			String[] cols = subRec.getFieldNames();
			subLine = new Object[newCols.length];
			for(int j=0; j<newCols.length; j++){	
				 if (ArrayUtils.contains(cols, newCols[j])) {
					 subLine[j] = subRec.getFieldValue(newCols[j]);
				 }
			}
			newTable.newLast(subLine);

			ret = newTable;
			newTable = null;
		}
		subNode = null;
		return ret;
	}
}
