package com.raqsoft.lib.ftp;

import java.util.ArrayList;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;

public class FtpMGet  extends Function {

	public Node optimize(Context ctx) {
		if (param != null) {
			param.optimize(ctx);
		}
		return this;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("ftp_mget" + mm.getMessage("function.missingParam"));
		}

		int size = param.getSubSize();
		if (size < 4) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("ftp_mget" + mm.getMessage("function.invalidParam"));
		}
		
		FtpClientImpl client = null;
		SFtpClientImpl sclient = null;
		String localFolder = null;
		String remoteFolder = null;
		ArrayList<String> remoteFiles = new ArrayList<String>();
		boolean overwrite = option!=null&&option.indexOf("f")>=0;
		boolean ignore = option!=null&&option.indexOf("t")>=0;
		if (ignore) overwrite = false;
		for(int i=0; i<size; i++){
			
			if (i==0) {
				if (param.getSub(i) == null ) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("ftp_mget" + mm.getMessage("function.invalidParam"));
				}

				Object o = param.getSub(i).getLeafExpression().calculate(ctx);
				if (o instanceof FtpClientImpl) client = (FtpClientImpl)o;
				else if (o instanceof SFtpClientImpl) sclient = (SFtpClientImpl)o;
				else throw new RQException("first parameter is not ftp_client");
			} else if (i == 1) {
				if (param.getSub(i) == null ) {
					remoteFolder = null;
				} else remoteFolder = (String)param.getSub(i).getLeafExpression().calculate(ctx);
			} else if (i == 2) {
				if (param.getSub(i) == null ) {
					localFolder = null;
				} else localFolder = (String)param.getSub(i).getLeafExpression().calculate(ctx);
			} else {
				remoteFiles.add((String)param.getSub(i).getLeafExpression().calculate(ctx));
			}
		}
		
		Sequence r = null;
		try {
			if (client != null) r = client.mget(localFolder,remoteFolder,remoteFiles,overwrite,ignore);
			//else r = sclient.mget(remote,localFile.getLocalFile().file().getAbsolutePath());
		} catch (Exception e) {
			e.printStackTrace();
			throw new RQException("ftp_mget : " + e.getMessage());
		}
		return r;
	}

}
