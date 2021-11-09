package com.raqsoft.lib.ftp;

import java.io.InputStream;
import java.util.ArrayList;

import com.raqsoft.common.Logger;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.FileObject;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;

public class FtpPut  extends Function {

	public Node optimize(Context ctx) {
		if (param != null) {
			param.optimize(ctx);
		}
		return this;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("ftp_put" + mm.getMessage("function.missingParam"));
		}

		int size = param.getSubSize();
		if (size != 3) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("ftp_put" + mm.getMessage("function.invalidParam"));
		}
		
		FtpClientImpl client = null;
		SFtpClientImpl sclient = null;
		String remote = null;
		FileObject fo = null;
		boolean overwrite = option!=null&&option.indexOf("f")>=0;
		for(int i=0; i<size; i++){
			if (param.getSub(i) == null ) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("ftp_put" + mm.getMessage("function.invalidParam"));
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
				if (o instanceof String) fo = new FileObject(o.toString());
				else if (o instanceof FileObject) fo = ((FileObject)o);
				else throw new RQException("The third parameter must be a string file path or a file object");
			}
		}
		
		boolean r = false;
		try {
			if (client != null) r = client.put(remote,fo.getInputStream(),overwrite);
			else r = sclient.put(remote, fo.getLocalFile().file().getAbsolutePath());
			Logger.debug("put local ["+fo.getLocalFile().file().getAbsolutePath()+"] to remote ["+remote+"] " + (r?"success":"failed"));
		} catch (Exception e) {
			e.printStackTrace();
			throw new RQException("ftp_put : " + e.getMessage());
		}
		return "upload file to remote ["+remote+"] "+(r?"success":"failed");
	}

}
