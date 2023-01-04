package com.scudata.lib.joinquant.function;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.FileObject;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.BFileCursor;
import com.scudata.dm.cursor.FileCursor;
import com.scudata.dm.cursor.ICursor;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;
import com.scudata.common.Logger;

/**
 *  1、在原文件中找要是否存在符合条件的记�?
	2、将存在的记录从现有的Table的移�?
	3、将清洗后的Table追加到文件中，完成当前条件后下载的数据后再汇�?
	4、记录下载位，比较还剩余下载量，支持断点下载
 * 
 * **********************************/
public class ImDownloadPostFq extends ImFunction {
	private String m_storagePath; 
	private String m_storageFile; 
	
	public Node optimize(Context ctx) {
		super.optimize(ctx);
		return this;
	}
	
	//jq_download(token,path, start_date,end_date)
	public Object doQuery(Object[] objs){
		try {
			if (objs.length<2){
				MessageManager mm = EngineMessage.get();
				throw new RQException("joinquant " + mm.getMessage("add param error"));
			}
			String tocken = null;
			if (objs[0] instanceof String){
				tocken = objs[0].toString();
			}
			
			//如果后后�?为btx或csv则为文件名，否则为目
			if (objs[1] instanceof String){
				String path = null;
				String tmp = objs[1].toString().toLowerCase();
				if (tmp.endsWith(".btx") ||tmp.endsWith(".csv")) {
					m_storageFile = tmp;
					File fp=new File(m_storageFile);
					path = fp.getParent();
				}else {
					path = tmp;
					m_storageFile = String.format("%s/download_stock_fq_post_2005.btx", path);
				}
				
				File f=new File(path);
				if (!f.exists()) {
					f.mkdirs();
				}
				m_storagePath = path;
			}
			
			if (tocken==null || m_storagePath == null  ) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("jq_download " + mm.getMessage("function.missingParam"));
			}
	
			int leftSize = getDownloadLeftSize(objs);

			if (leftSize>4000) {
				//1. 获取标的列表
				Table stockTbl = getStockList(m_storagePath, objs, "stock");
				//2。获取标的数
				Map<String, Object> paramMap = new HashMap<>();
				paramMap.put("method", "get_fq_factor");
				paramMap.put("token", tocken);
						
				paramMap.put("fq", "post");
				paramMap.put("date", "2005-01-01");
				paramMap.put("end_date", "2005-12-31");
				String info = doStockInfo(stockTbl, paramMap);
				return info;
			}else {
				return "Insufficient downloads";
			}
			//return ret;
		}catch(Exception e) {
			System.out.println(e.getMessage());
		}
		return null;
	}
	
	//下载数据存储.
	private String doStockInfo(Table stockTbl, Map<String, Object> paramMap) {
		String ret = "";
		try {
			File fp = new File(m_storageFile);
			
			if (!fp.exists()) {
				fp.createNewFile();
			}
			
			FileObject curFile = new FileObject(m_storageFile,"gbk","s", null);
			
			String code=null, name = null; 
			String info=null;
			Table allTable=new Table(new String[] {"code", "name", "date", "fq"});
			//遍历数据
			for(int n=0; n<stockTbl.length(); n++) {
				Record r = stockTbl.getRecord(n+1);
				code = r.getFieldValue("code").toString();
				name = r.getFieldValue("display_name").toString();
				paramMap.put("code", code);	
				
				String[] body = JQNetWork.GetNetArrayData(paramMap);
				Object objs[] = new Object[4];
				objs[0] = code;
				objs[1] = name;
				if (body== null) {
					objs[2] = "2005-01-04";
					objs[3] = 1.0;
				}else {
					String[] sourceStrArray = body[1].split(",", -1);
					if (sourceStrArray.length==2) {
						objs[2] = sourceStrArray[0];
						objs[3] = Float.parseFloat(sourceStrArray[1]);
					}
				}
				allTable.newLast(objs);
				info=String.format("index=%d; code=%s; name=%s; len=%d", n, code, name, body.length);
				//System.out.println(info);
				Logger.info(info);
				if (n>5) break; // for test
			}
			if (allTable.length()>0) {
				SaveTableToFile(allTable, curFile);
			}
		}catch(Exception e) {
			System.out.println(e.getMessage());
		}
		
		return ret;
	}
	
	 
	//存储数据
	public static void SaveTableToFile(Sequence seq, FileObject f) {
		String fName = f.getFileName();
		String suffix = fName.substring(fName.lastIndexOf(".")+1);
		FileObject fo = new FileObject(fName,"gbk","s", null);
		if (suffix.compareToIgnoreCase("btx")==0) {
			fo.exportSeries(seq, "ab", null);
		}else {
			if (fo.size()==0) { //read datas
				fo.exportSeries(seq, "tc", null);
			}else {
				fo.exportSeries(seq, "ac", null);
			}
		}
	}
	
	//获取下载的记录数
	private int getDownloadLeftSize( Object[] params) {
		try {
			Object[] objs = new Object[1];
			objs[0] = params[0];
			
			ImQueryCount c = new ImQueryCount();
			Object ret = c.doQuery(objs);
			if (ret!=null) {
				return Integer.parseInt(ret.toString());
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return 0;
	}
	//获取股票列表
	private Table getStockList(String path, Object[] params, String typeName) {
		Table ret = null;
		try {
			Object[] objs = new Object[3];
			objs[0] = params[0];
			objs[1] = typeName;
			objs[2] = new Date( System.currentTimeMillis());
			File fo = new File(path);
			if (!fo.exists()) {
				fo.mkdirs();
			}
			String stockFile = String.format("%s/%s_list.btx", path, typeName);
			FileObject f = new FileObject(stockFile, "gbk","s", null);
			if (f.isExists()) { //read data
				ret = getSeqFromFile(f);
			} else { //down load
				ImAllSecurities sc = new ImAllSecurities();
				ret = (Table)sc.doQuery(objs);
				f.exportSeries(ret, "ab", null);
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return ret;
	}
	
	//从文件中获取数据
	private  Table getSeqFromFile(FileObject fo) {
		if (!fo.isExists()) return null;
		
		ICursor cur = null;
		int type = fo.getFileType();
		if (type==FileObject.FILETYPE_BINARY) {
			cur = new BFileCursor(fo, null, null, null);
		} else {
			cur = new FileCursor(fo, 1, 1, null, null, null, "tc", new Context());
		}
		Table temp = (Table) cur.fetch(1000);
		if (temp==null) return null;
		
		Table seq = new Table(temp.dataStruct().getFieldNames());
		while (null != temp) {
			seq.addAll(temp);
			if (temp.length()<1000) break;
			temp = (Table)cur.fetch(1000);
		}
		
		return seq;
	}
	
	public static void main(String[] args){
		ImDownloadPostFq cls = new ImDownloadPostFq();
		String path = "d:/tmp/data/jq/download";
		//Table tbl = cls.getStockList(path, new Object[] {"5b6a9ba1b5f073bb20667f2f06ca0ab9adea132c"}, "stock");
		Object[] os = new Object[] {"5b6a9ba1b5f073bb20667f2f06ca0ab9adea132c", path};
		cls.doQuery(os);
		System.out.println("end...");
		
	}
}
