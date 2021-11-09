package com.raqsoft.lib.ftp;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;

public class FtpClient  extends Function {

	public Node optimize(Context ctx) {
		if (param != null) {
			param.optimize(ctx);
		}
		return this;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("ftp_client" + mm.getMessage("function.missingParam"));
		}

		int size = param.getSubSize();
		
		String url = null;
		int port = 0;
		String user = "anonymous";
		String pwd = null;
		String option = this.option;
		if (option == null) option = "";
		int mode = option.indexOf("d")>=0?1:0;
		boolean sftp = option.indexOf("s")>=0;
		if (size == 0) {
			Expression exp = param.getLeafExpression();
			if (exp == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("ws_client" + mm.getMessage("function.invalidParam"));
			}
				url = param.getLeafExpression().calculate(ctx).toString();
		} else {
			char type = param.getType();
			if (type == IParam.Comma) {
				for(int i=0; i<size; i++){
					if (param.getSub(i) == null ) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("ftp_client" + mm.getMessage("function.invalidParam"));
					}
					if (i==0) {
						char typei = param.getSub(i).getType();
						if (typei == IParam.Colon) {
							url = param.getSub(i).getSub(0).getLeafExpression().calculate(ctx).toString();
							port = Integer.parseInt(param.getSub(i).getSub(1).getLeafExpression().calculate(ctx).toString());
						} else {
							url = param.getSub(i).getLeafExpression().calculate(ctx).toString();
						}
					} else if (i == 1) {
						user = (String)param.getSub(i).getLeafExpression().calculate(ctx);
					} else if (i == 2) {
						pwd = (String)param.getSub(i).getLeafExpression().calculate(ctx);
					}
				}
			} else if (type == IParam.Colon) {
				for(int i=0; i<size; i++){
					if (i==0) {
						url = param.getSub(i).getLeafExpression().calculate(ctx).toString();
					} else if (i == 1) {
						port = Integer.parseInt(param.getSub(i).getLeafExpression().calculate(ctx).toString());
					}
				}
			}
		}
		
		try {
			if (sftp) {
				SFtpClientImpl client = new SFtpClientImpl(ctx,url,port,mode);
				boolean succ = client.login(user, pwd);
				if (succ) {
					ctx.addResource(client);
				}
				return client;
			} else {
				FtpClientImpl client = new FtpClientImpl(ctx,url,port,mode);
				boolean succ = client.login(user, pwd);
				if (succ) {
					ctx.addResource(client);
				}
				return client;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
	}

}
