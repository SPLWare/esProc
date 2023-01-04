package com.scudata.lib.joinquant.function;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.FileObject;
import com.scudata.dm.ListBase1;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.BFileCursor;
import com.scudata.dm.cursor.FileCursor;
import com.scudata.dm.cursor.ICursor;
import com.scudata.expression.Expression;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;
import com.scudata.common.Logger;

/**
 *  1、在原文件中找要是否存在符合条件的记
	2、将存在的记录从现有的Table的移
	3、将清洗后的Table追加到文件中，完成当前条件后下载的数据后再汇
	4、记录下载位，比较还剩余下载量，支持断点下载
 * 
 * **********************************/
public class ImDownload extends ImFunction {
	private String m_storagePath; 
	private String m_storageFile; 
	private int m_leftSize = 0;
	private boolean m_bFilter = true;
	private boolean m_bCollect = false;
	private String m_lastFile = ""; 	//从日志中读取
	private String m_typeName = "stock"; //或futures
	
	public Node optimize(Context ctx) {
		super.optimize(ctx);
		return this;
	}
	
	private void reset() {
		m_bFilter = true;
		m_bCollect = false;
		m_typeName = "stock";
		m_lastFile = "";
	}
	
	//jq_download(token,path, start_date,end_date)
	public Object doQuery(Object[] objs){
		try {
			if (objs.length<3){
				MessageManager mm = EngineMessage.get();
				throw new RQException("joinquant " + mm.getMessage("add param error"));
			}
			String tocken = null;
			String startTime = null;
			String endTime = null;
			SimpleDateFormat formatter= new SimpleDateFormat("yyyyMMdd");
			Date date = new Date(System.currentTimeMillis());
			String curDate = formatter.format(date);
			
			//选项.
			if (this.option!=null) {
				if ( option.contains("q")) { m_typeName = "futures"; }
				if ( option.contains("c")) { m_bCollect = true; }
				
			}
			
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
					//System.out.println(formatter.format(date));
					m_storageFile = String.format("%s/download_%s_kday_%s.btx", path, m_typeName, curDate);
				}
				
				File f=new File(path);
				if (!f.exists()) {
					f.mkdirs();
				}
				m_storagePath = path;
			}
			
			Object result1=null,result2=null;
			if (objs[2] instanceof String){
				startTime = objs[2].toString();
				result1 = Variant.parseDate(startTime);
				if (!(result1 instanceof Date)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("jq_download" + mm.getMessage("function.paramTypeError"));
				}
			}
			
			if (objs.length>=4 && objs[3] instanceof String){
				endTime = objs[3].toString();
				result2 = Variant.parseDate(endTime);
				if (!(result1 instanceof Date)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("jq_download" + mm.getMessage("function.paramTypeError"));
				}
			}else {
				formatter= new SimpleDateFormat("yyyy-MM-dd");
				endTime= formatter.format(date);
				result2 = date;
			}
			
			if (tocken==null || startTime == null || endTime == null ) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("jq_download " + mm.getMessage("function.missingParam"));
			}
	
			if (!(result1 instanceof Date) || !(result2 instanceof Date)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("interval" + mm.getMessage("function.paramTypeError"));
			}
			Date date1 = (Date)result1;
			Date date2 = (Date)result2;
			
			//计算获取的数据大概条数（由于接口get_price不支持startDate
			long days = Variant.interval(date1, date2, "");
			if (days>6) {
				days = (long)((days+2)*5/7+1);
			}else {
				days+=1;
			}
			
			m_leftSize = getDownloadLeftSize(objs);
			if (m_leftSize>4000) {
				//1. 获取标的列表
				Table stockTbl = getStockList(m_storagePath, objs, m_typeName, startTime);
				//2。获取标的数
				Map<String, Object> paramMap = new HashMap<>();
				paramMap.put("method", "get_price");
				paramMap.put("token", tocken);
						
				paramMap.put("count", days);
				paramMap.put("unit", "1d");
				paramMap.put("end_date", endTime);
				String info = doStockInfo(stockTbl, paramMap, startTime);

				return info;
			}else {
				return "Insufficient downloads";
			}
			//return ret;
		}catch(Exception e) {
			System.out.println(e.getMessage());
		}finally {
			reset();
		}
		return null;
	}
	
	//下载数据存储.
	private String doStockInfo(Table stockTbl, Map<String, Object> paramMap, String startTime) {
		String ret = "";
		int nTotal = 0;
		int nDownloadSize = 0;
		try {
			File fp = new File(m_storageFile);
			
			if (!fp.exists()) {
				fp.createNewFile();
				m_bFilter = false;
			}
			
			FileObject curFile = new FileObject(m_storageFile,"gbk","s", null);
			String sAllFile = String.format("%s/storage_%s_kday_all.btx", m_storagePath, m_typeName);
			FileObject fo = new FileObject(sAllFile,"gbk","s", null);
			
			ICursor cursor = null;
			if (!fo.isExists()) {
				m_bFilter = false;
			}else {
				int type = fo.getFileType();
				if (type==FileObject.FILETYPE_BINARY) {
					cursor = new BFileCursor(fo, null, null, null);
				} else {
					cursor = new FileCursor(fo, 1, 1, null, null, null, "tc", new Context());
				}
			}
			//查找�?新下载位
			String endTime = paramMap.get("end_date").toString();
			int n = getLastStockPos(startTime, endTime);
			String code=null, name = null; 
			String info=null;
			//遍历数据
			for(; n<stockTbl.length(); n++) {
				Record r = stockTbl.getRecord(n+1);
				code = r.getFieldValue("code").toString();
				name = r.getFieldValue("display_name").toString();
				paramMap.put("code", code);	
				
				String[] body = JQNetWork.GetNetArrayData(paramMap);
				Table tbl = DataType.toTableWithCode(code, name, body);
				if (tbl== null) continue;
				nDownloadSize+=tbl.length();
				//用startTime过滤当前下载的数
				String reg=String.format("date>=date(\"%s\")", startTime);
				Expression fltExp = new Expression(reg);
				Sequence curSeq = (Sequence)tbl.select(fltExp, "", m_ctx);
				tbl = null;
				tbl = new Table(curSeq.dataStruct());
				tbl.addAll(curSeq);
				nTotal+=tbl.length();
				//
				if (m_bFilter) {
				    reg=String.format("code==\"%s\"&&date>=date(\"%s\")",code, startTime);
				    tbl = DeleteFromFile(tbl, cursor, reg);
				}
				if (tbl.length()>0) {
					SaveTableToFile(tbl, curFile);
				}
				info=String.format("index=%d; code=%s; name=%s; len=%d", n, code, name, tbl.length());
				//System.out.println(info);
				Logger.info(info);
				//下载数据量不足时�?
				if (m_leftSize-nDownloadSize<4000) break;
				//if (n>last+10) break; // for test
			}
			// 此片段经(某条件下)的数据下载完后再汇�?�汇总数, 对已经汇总过的数据不再汇�?.
			if (m_bCollect && n==stockTbl.length() && !m_storageFile.equals(m_lastFile)) {
				File file = new File(sAllFile);
				if (!file.exists()) {
					file.createNewFile();
				}
				int type = curFile.getFileType();
				if (type==FileObject.FILETYPE_BINARY) {
					cursor = new BFileCursor(curFile, null, null, null);
				} else {
					cursor = new FileCursor(curFile, 1, 1, null, null, null, "tc", new Context());
				}

				fo.exportCursor(cursor, null, null, "ab", null, null);

			}
			//filter 是否过滤，若断点下载数据上次不过滤，此次也不再过
			//日志：记录最后一只code
			String lastInfo=String.format("name=%s;code=%s;start_date=%s;end_date=%s;file=%s;filter=%d;pos=%d",
					name, code, startTime, endTime, m_storageFile, m_bFilter?1:0, n);
			String sFile = String.format("%s/download_%s_last_info.txt", m_storagePath, m_typeName);
			FileObject flog = new FileObject(sFile, "gbk", "s", null);
			flog.write(lastInfo, null);			
			
			//返回信息
			ret = String.format("DownSize=%d; RecordSize=%d", nDownloadSize, nTotal);
		}catch(Exception e) {
			System.out.println(e.getMessage());
		}
		
		return ret;
	}
	
	//从日志中获取�?新下载位
	private int getLastStockPos(String startDate, String endDate) {
		try {
			String sFile = String.format("%s/download_%s_last_info.txt", m_storagePath, m_typeName);
			FileObject flog = new FileObject(sFile, "gbk", "s", null);
			if (!flog.isExists()) return 0;
			
			Object o = flog.read(0, -1, "n");
			int cnt = 0;
			if (o!=null && o instanceof Sequence) {
				o = ((Sequence)o).get(1);
				if (o==null) return 0;
				String[] lines =o.toString().split(";");
				for(String s:lines) {
					if (s.endsWith(startDate) || s.endsWith(endDate) || s.endsWith(m_storageFile)) {
						cnt+=1;
					}
					if(s.startsWith("file=")) {
						m_lastFile = s.replace("file=", "");
					}
				}
				if (cnt == 3 && lines.length==7) {
					String s = lines[lines.length-2];
					s = s.replace("filter=", "");
					int nval = Integer.parseInt(s);
					m_bFilter =(nval==1)?true:false;
					
					s = lines[lines.length-1];
					s = s.replace("pos=", "");
					return Integer.parseInt(s)+1;
				}
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return 0;
	}
	//当前Table中从总数据文件中
	 public Table DeleteFromFile(Table curTable, ICursor cursor, String sRegex) {
		 	Table filterTable = getFilterData(cursor, sRegex);
		 	if (filterTable==null) return curTable;
		 	
			return delete(curTable, filterTable, "");
	  }
	 //根据date将series数据从A表删除，并返回A
	 public Table delete(Table tbl, Sequence series, String opt) {
			if (series == null || series.length() == 0) {
				return tbl;
			}

			int[] index = null;
			
			ListBase1 mems = tbl.getMems();
			int delCount = 0;

			ListBase1 delMems = series.getMems();
			int count = delMems.size();
			index = new int[count];

			for (int i = 1; i <= count; ++i) {
				Object obj = delMems.get(i);
				if (obj instanceof Record) {
					Record t = (Record)obj;
					for (int j = 1, size = mems.size(); j <= size; ++j) {
						Record r = (Record)mems.get(j);
						if (r.getFieldValue("date").toString().compareTo( t.getFieldValue("date").toString())==0) {
							index[delCount] = j;
							delCount++;
							break;
						}
					}
				}
			}

			if (delCount == 0) {
				return tbl;
			}

			Arrays.sort(index);
			if (opt == null || opt.indexOf('n') == -1) {
				mems.remove(index);
				
				return tbl;
			} 
			
			return null;
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
	private Table getStockList(String path, Object[] params, String typeName, String startDate) {
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
	
		//用startDate过滤当前下载的数
		String reg=String.format("end_date>=date(\"%s\")", startDate);
		Expression fltExp = new Expression(reg);
		Sequence curSeq = (Sequence)ret.select(fltExp, "", m_ctx);
		Table tbl = new Table(curSeq.dataStruct());
		tbl.addAll(curSeq);
		return tbl;
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
	
	//从�?�数据文件中查找记录(filter: code+date)
	private  Table getFilterData(ICursor cur, String sRegex) {
		if (cur==null) return null;
		
		cur.reset();
		Expression fltExp = new Expression(sRegex);
		Context ctx = new Context();
		
		Table temp = (Table) cur.fetch(1000);
		if (temp==null) return null;
		
		Table seq = new Table(temp.dataStruct().getFieldNames());
		
		while (null != temp) {
			Sequence s = (Sequence)temp.select(fltExp, "", ctx);
			if (s!=null) {
				seq.addAll(s);
			}
			if (temp.length()<1000) break;
			temp = (Table)cur.fetch(1000);
		}
		
		return seq;
	}
	
	
	public static void main2(String[] args){
		ImDownload cls = new ImDownload();
		String path = "d:/tmp/data/jq/download";
		float a = 5.8f;
//		System.out.println("不大于他的最大整数为+
//				(int)(a==(int)a?a-1:(int)(a))+
//				"\n不小于他的最小整数为:"+
//				(int)(a+1));
		//Table tbl = cls.getStockList(path, new Object[] {"5b6a9ba1b2f77fb222667f2f06ca09bd7b5b893a"}, "stock");
		for (int i=1+7; i<10+7; i++)
		{
			long days = (long)((i+2)*5/7+1);
			System.out.println(i+"==>"+days);
		}
		System.out.println("end...");
		
	}
}
