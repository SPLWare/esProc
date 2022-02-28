package com.scudata.lib.ftp;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

public class FtpClose  extends Function {

	public Node optimize(Context ctx) {
		if (param != null) {
			param.optimize(ctx);
		}
		return this;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("ftp_close" + mm.getMessage("function.missingParam"));
		}

		int size = param.getSubSize();
		FtpClientImpl client = null;
		SFtpClientImpl sclient = null;
		if (size == 0) {
			Expression exp = param.getLeafExpression();
			if (exp == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("ws_client" + mm.getMessage("function.invalidParam"));
			}
			//Logger.debug("size " + size);
			Object o = exp.calculate(ctx);
			if (o instanceof FtpClientImpl) client = (FtpClientImpl)o;
			else sclient = (SFtpClientImpl)o;
		} else {
			Object o = param.getSub(0).getLeafExpression().calculate(ctx);
			if (o instanceof FtpClientImpl) client = (FtpClientImpl)o;
			else sclient = (SFtpClientImpl)o;
		}
				
		try {
			if (client != null) client.close();
			else sclient.close();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RQException("ftp_close : " + e.getMessage());
		}
		return "close success!";
	}

}
