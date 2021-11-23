package com.scudata.lib.hdfs.function;

import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.FileObject;
import com.scudata.expression.Node;

public class HdfsFile extends HdfsFunction {
	boolean bCursor = false;
	public Node optimize(Context ctx) {
		super.optimize(ctx);
		
		return this;
	}

	public Object doQuery( Object[] objs){
		if(objs!=null ){
			String folder =  objs[0].toString();
			String cs = null;
			if (objs.length==2){
				cs = objs[1].toString();
			}

			if (m_hdfs==null) {
				throw new RQException("Error: FileSystem is null.");
			}
			String url = m_hdfs.getUri().toString();
			String name = url+folder;
			HdfsFileImpl hdfs = new HdfsFileImpl(m_hdfs, folder);
			return new FileObject(hdfs, name, cs, option);
		}
		
		return null;
	}
	
}
