package com.scudata.lib.ftp;

import java.util.ArrayList;

import com.scudata.common.Logger;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

public class FtpMkdir  extends Function {

	public Node optimize(Context ctx) {
		if (param != null) {
			param.optimize(ctx);
		}
		return this;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("ftp_mkdir" + mm.getMessage("function.missingParam"));
		}

		int size = param.getSubSize();
		if (size != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("ftp_mkdir" + mm.getMessage("function.invalidParam"));
		}
		
		FtpClientImpl client = null;
		SFtpClientImpl sclient = null;
		String path = null;
		for(int i=0; i<size; i++){
			if (param.getSub(i) == null ) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("ftp_mkdir" + mm.getMessage("function.invalidParam"));
			}
			
			if (i==0) {
				Object o = param.getSub(i).getLeafExpression().calculate(ctx);
				if (o instanceof FtpClientImpl) client = (FtpClientImpl)o;
				else if (o instanceof SFtpClientImpl) sclient = (SFtpClientImpl)o;
				else throw new RQException("first parameter is not ftp_client");
			} else if (i == 1) {
				path = (String)param.getSub(i).getLeafExpression().calculate(ctx);
			}
		}
		
		Sequence s = null;
		ArrayList<String> dirs = new ArrayList<String>();
		dirs.add(path);
		try {
			if (client != null) s = client.mkdir(dirs);
			//else sclient.changeRemoteDir(path);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RQException("ftp_mkdir : " + e.getMessage());
		}
		return s;
	}

}
