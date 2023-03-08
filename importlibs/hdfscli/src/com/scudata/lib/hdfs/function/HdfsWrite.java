package com.scudata.lib.hdfs.function;

import org.apache.hadoop.fs.FileSystem;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.common.Logger;
import com.scudata.dm.Context;
import com.scudata.dm.FileObject;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.ICursor;
import com.scudata.expression.Expression;
import com.scudata.expression.FileFunction;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

//write@a(fp, A, fileName)
public class HdfsWrite extends FileFunction {
	protected FileSystem m_hdfs = null;
	protected Context m_ctx;
	
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("hdfs_client" + mm.getMessage("function.missingParam"));
		}
		m_ctx = ctx;
		IParam param0;
		Expression sexp = null;
		if (param.getType() == IParam.Semicolon) { // ;
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("export" + mm.getMessage("function.invalidParam"));
			}

			param0 = param.getSub(0);
			if (param0 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("export" + mm.getMessage("function.invalidParam"));
			}
			
			IParam param1 = param.getSub(1);
			if (param1 != null) {
				sexp = param1.getLeafExpression();
			}
		} else {
			param0 = param;
		}
		
		Expression []exps = null;
		String []names = null;
		int nSize = param0.getSubSize();
		Object[] objs = new  Object[6];
		if (nSize<3){ // series,xi:fi...
			throw new RQException("export function.invalidParam");
		}
		
		IParam sub = null;
		for (int i = 0; i < 3; ++i) {
			sub = param0.getSub(i);
			if (sub == null || !sub.isLeaf()) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("export" + mm.getMessage("function.invalidParam"));
			}			
			objs[i] = sub.getLeafExpression().calculate(ctx);
		}
		
		exps = new Expression[nSize - 3];
		names = new String[nSize - 3];
		for (int i = 3; i < nSize; ++i) {
			sub = param0.getSub(i);
			if (sub == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("export" + mm.getMessage("function.invalidParam"));
			} else if (sub.isLeaf()) {
				exps[i - 3] = sub.getLeafExpression();
			} else {
				if (sub.getSubSize() != 2) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("export" + mm.getMessage("function.invalidParam"));
				}

				IParam p1 = sub.getSub(0);
				if (p1 == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("export" + mm.getMessage("function.invalidParam"));
				}

				exps[i - 3] = p1.getLeafExpression();
				IParam p2 = sub.getSub(1);
				if (p2 != null) {
					names[i - 3] = p2.getLeafExpression().getIdentifierName();
				}
			}
		}
		if (exps.length==0){
			objs[3]=null;
		}else{
			objs[3] = exps;
		}
		if (names.length==0){
			objs[4]=null;
		}else{
			objs[4] = names;
		}

		if (sexp!=null){
			objs[5] = sexp.calculate(ctx);
		}
		return doQuery(objs);
	}
	
	public Object doQuery( Object[] objs){
		try {
			m_hdfs = (FileSystem)objs[0];
			String filePath = objs[1].toString();
        	String url = m_hdfs.getUri().toString();
			String name = url + filePath;
			HdfsFileImpl hdfs = new HdfsFileImpl(m_hdfs, filePath);
			FileObject fo = new FileObject(hdfs, name, null, option);

			if (objs[2] instanceof Sequence){				
				fo.exportSeries((Sequence)objs[2],(Expression[])objs[3], (String[])objs[4], option, objs[5], m_ctx); 
			}else if (objs[2] instanceof ICursor){
				fo.exportCursor((ICursor)objs[2], (Expression[])objs[3], (String[])objs[4], option, objs[5], m_ctx); 
			}
			
			return true;
		} catch (Exception e) {
			Logger.error(e.getMessage());
		}
		return false;
	}
	
//	public void saveTable(Table table, String hdfsFile, boolean bAppend) {
//		if (table == null)
//			return;
//		FSDataOutputStream fsDataOutputStream = null;
//		try {
//        	 FileSystem fileSystem = m_hdfs;
//        	 Path path = new Path(hdfsFile);
//             if(fileSystem.exists(new Path(hdfsFile)))
//             {
//            	 long size = fileSystem.getFileStatus(path).getLen();
//            	 fsDataOutputStream = fileSystem.append(path, (int)size);
//             }else{
//	            //输出流对象，将数据输出到HDFS文件系统
//	            fsDataOutputStream = fileSystem.create(new Path(hdfsFile));
//             }
//             
//            DataStruct ds = table.dataStruct();
// 			String[] fields = ds.getFieldNames();
// 			StringBuilder sb = new StringBuilder();
// 			
// 			int i = 0;
// 			// print colNames;
// 			for (i = 0; i < fields.length; i++) {
// 				//System.out.print(fields[i] + "\t");
// 				sb.append(fields[i]+";");
// 			}
// 			sb.append("\r\n");
// 			byte[] buf = null;
// 			//FileWriter fw = new FileWriter(file);
// 			for (i = 0; i < table.length(); i++) {
// 				Record rc = table.getRecord(i + 1);
// 				Object[] objs = rc.getFieldValues();
// 				for (Object o : objs) {
// 					sb.append(o+";");
// 				}
// 				sb.append("\r\n");
// 				if (i>0 && i%10000==0){
// 					buf = sb.toString().getBytes();
// 					fsDataOutputStream.write(buf, 0, buf.length);
// 					sb.setLength(0);
// 				}
// 			}
// 			buf = sb.toString().getBytes();
// 			fsDataOutputStream.write(buf, 0, buf.length);
//        } catch (Exception e) {
//        	Logger.error(e.getMessage());
//        }finally {
//        	try{
//        		if (fsDataOutputStream!=null){
//        			fsDataOutputStream.close();
//        		}
//        	}catch(Exception e){
//        		
//        	}
//        }
//	}
	
}
