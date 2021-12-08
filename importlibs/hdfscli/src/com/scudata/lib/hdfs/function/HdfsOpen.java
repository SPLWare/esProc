package com.scudata.lib.hdfs.function;

import com.scudata.dm.Context;
import com.scudata.expression.Node;

/****
 * 
 * @author 
 * hdfs_open(uri,[username])	·µ»ØHdfsOpen
 */
public class HdfsOpen extends HdfsFunction {
	public Node optimize(Context ctx) {
		if (param != null) {
			param.optimize(ctx);
		}
		
		return this;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			HdfsClient client = new HdfsClient(ctx, null, null, "root");
			return client.getFileSystem();
		}
		
		String userName = "root";
		String sUrl = null;
		String objs[] = null;	
		
		if (param.isLeaf()){
			sUrl = (String)param.getLeafExpression().calculate(ctx);
		}else if(param.getSubSize()>=2){
			sUrl = (String)param.getSub(0).getLeafExpression().calculate(ctx);
			userName = (String)param.getSub(1).getLeafExpression().calculate(ctx);
		}

		HdfsClient client = new HdfsClient(ctx, objs, sUrl, userName);
		return client.getFileSystem();
	}
}