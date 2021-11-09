package com.raqsoft.lib.hdfs.function;

import java.util.ArrayList;
import java.util.List;

import com.raqsoft.common.*;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.expression.*;
import com.raqsoft.dm.*;

// hdfsfile(url:cs,xml:xml...)
public class ReadFile extends Function {
	private String m_codes;
	private String m_pathName;	

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("hdfsfile" + mm.getMessage("function.missingParam"));
		}

		List<String> ls = null;
		if (param.isLeaf()) {
			Object pathObj = param.getLeafExpression().calculate(ctx);
			if (!(pathObj instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("hdfsfile" + mm.getMessage("function.invalidParam"));
			}

			m_pathName = (String)pathObj;
		} else {
			for(int i=0; i<param.getSubSize(); i++){
				if( param.getType()==IParam.Colon){ //only for url:cs
					ls =parseParam(ctx, param);
					m_pathName = ls.get(0).toString();
					if (ls.size()>1){
						m_codes = ls.get(1).toString();
					}
					ls.clear();
				}else{ //for url:cs,xml:xml,
					IParam subs = param.getSub(i);
					if (i==0){
						ls =parseParam(ctx, subs);
						m_pathName = ls.get(0).toString();
						if (ls.size()>1){
							m_codes = ls.get(1).toString();
						}
						ls.clear();
					}else{ //for xml			
						ls=parseParam(ctx, subs);
					}
				}
			}			
		}

		try {
			final String prefix = "hdfs://";
			int len = prefix.length();
			if ( m_pathName.length() < len || 
				!m_pathName.substring(0, len).toLowerCase().equals(prefix)) {
				m_pathName = prefix + m_pathName;
			}
			// for xml;
			String []xmlfiles = null;
			if (ls.size()>0){
				xmlfiles = new String[ls.size()];
				ls.toArray(xmlfiles);
			}
			
			int index = m_pathName.indexOf(":", prefix.length());
			int offset = m_pathName.indexOf("/", index);
			String url = m_pathName.substring(0, offset);
			String path = m_pathName.substring(offset);

			HdfsClient client = new HdfsClient(ctx, xmlfiles, url, "root");			
			HdfsFileImpl file = new HdfsFileImpl(client.getFileSystem(), path);
			
			return new FileObject(file, m_pathName, m_codes, option);
		} catch (Exception e) {
			throw new RQException(e);
		}
	}

	public byte calcExpValueType(Context ctx) {
		return Expression.TYPE_FILE;
	}
	
	private List<String> parseParam(Context ctx, IParam param){
		List<String> ls = new ArrayList<String>();
		Object o = null;
		for(int i=0; i<param.getSubSize(); i++){
			IParam subs = param.getSub(i);
			o = subs.getLeafExpression().calculate(ctx);
			if (o==null || !(o instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("hdfsfile" + mm.getMessage("function.invalidParam"));
			}
			ls.add(o.toString());
		}
		
		return ls;
	}
}
