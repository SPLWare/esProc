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
		if (size == 0) {
			Expression exp = param.getLeafExpression();
			if (exp == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("ws_client" + mm.getMessage("function.invalidParam"));
			}
			//Logger.debug("size " + size);
			client = (FtpClientImpl)exp.calculate(ctx);
		} else {
			client = (FtpClientImpl)param.getSub(0).getLeafExpression().calculate(ctx);
		}
				
		try {
			client.close();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RQException("ftp_close : " + e.getMessage());
		}
		return "close success!";
	}

}
