package com.scudata.lib.hdfs.function;

import org.apache.hadoop.fs.FileSystem;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Param;
import com.scudata.expression.Function;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

// hdfs_close(hdfs_client)
public class HdfsClose extends Function {
	public Node optimize(Context ctx) {
		return this;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("hdfs_close" + mm.getMessage("function.missingParam"));
		}

		Object client = param.getLeafExpression().calculate(ctx);
		if (!(client instanceof FileSystem)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("hdfs_close" + mm.getMessage("function.paramTypeError"));
		}
		
		try {
			Param pm = ctx.getParam("classLoader");
			if (pm!=null ){
				ClassLoader loader = (ClassLoader)pm.getValue();
				Thread.currentThread().setContextClassLoader(loader);
				ctx.setParamValue("classLoader", null);
			}
			FileSystem fs = (FileSystem)client;
			fs.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
}
