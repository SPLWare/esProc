package com.scudata.lib.ftp;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.FileObject;
import com.scudata.expression.Function;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

public class FtpGet extends Function {

	public Node optimize(Context ctx) {
		if (param != null) {
			param.optimize(ctx);
		}
		return this;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("ftp_get" + mm.getMessage("function.missingParam"));
		}

		int size = param.getSubSize();
		if (size != 3) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("ftp_get" + mm.getMessage("function.invalidParam"));
		}
		
		FtpClientImpl client = null;
		SFtpClientImpl sclient = null;
		String remote = null;
		FileObject localFile = null;
		boolean overwrite = option!=null&&option.indexOf("f")>=0;
		for(int i=0; i<size; i++){
			if (param.getSub(i) == null ) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("ftp_get" + mm.getMessage("function.invalidParam"));
			}
			
			if (i==0) {
				Object o = param.getSub(i).getLeafExpression().calculate(ctx);
				if (o instanceof FtpClientImpl) client = (FtpClientImpl)o;
				else if (o instanceof SFtpClientImpl) sclient = (SFtpClientImpl)o;
				else throw new RQException("first parameter is not ftp_client");
			} else if (i == 1) {
				remote = (String)param.getSub(i).getLeafExpression().calculate(ctx);
			} else if (i == 2) {
				Object o = param.getSub(i).getLeafExpression().calculate(ctx);
				if (o instanceof String) localFile = new FileObject(o.toString());
				else if (o instanceof FileObject) localFile = (FileObject)o;
				else throw new RQException("The third parameter must be a string file path or a file object");
			}
		}
		
		boolean r = false;
		try {
			if (client != null) r = client.get(remote,localFile,overwrite);
			else r = sclient.get(remote,localFile.getLocalFile().file().getAbsolutePath());
		} catch (Exception e) {
			throw new RQException("ftp_get : " + e.getMessage());
		}
		return "get remote file ["+remote+"] "+(r?"success":"failed");
	}

}
