package com.raqsoft.lib.mongo.function;

import org.bson.Document;
import com.mongodb.client.MongoDatabase;
import com.raqsoft.common.*;
import com.raqsoft.dm.*;
import com.raqsoft.dm.cursor.ICursor;
import com.raqsoft.util.JSONUtil;

public class ImCursor extends ICursor {
	private String m_cmd;
	private ImMongo m_mongo;
	private String[] m_cols;
	private int m_nFetchSize = 10000; //fetch() 缺省记录数量
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
		
		if (fetchSize>0) {
			close();
		}
		
		return count;
	}

	public synchronized void close() {
		super.close();
		
		try {
			if (ctx != null) ctx.removeResource(this);			
			cursorId = 0;
			if (m_mongo != null) {
				m_mongo.close();
				m_mongo = null;
			}
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		}
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

//		if (n == 2147483646){
//			n = m_nFetchSize;
//		}
		
		
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
			String s = cur.toJson();

			char[] chars = s.toCharArray();
			Object r = JSONUtil.parseJSON(chars, 0, chars.length - 1);

			if (r instanceof Record){
				Record rcd = (Record)r;
				if (rcd.dataStruct().getFieldIndex("firstBatch")>-1){
					obj = rcd.getFieldValue("firstBatch");
					//System.out.println(obj);
				}else if (rcd.dataStruct().getFieldIndex("nextBatch")>-1){
					obj = rcd.getFieldValue("nextBatch");
					//System.out.println(obj);
				}
				if (obj instanceof Table){
					tbl = (Table)obj;
					if (m_cols==null){
						m_cols = tbl.dataStruct().getFieldNames();
					}
				}
			}
		}catch(Exception e){
			Logger.error(e.getMessage());
		}
		
		return tbl;
	}
}
