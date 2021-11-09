package com.raqsoft.lib.ftp;

import java.util.ArrayList;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;

public class FtpDir  extends Function {

	public Node optimize(Context ctx) {
		if (param != null) {
			param.optimize(ctx);
		}
		return this;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("ftp_dir" + mm.getMessage("function.missingParam"));
		}

		int size = 0;
		FtpClientImpl client = null;
		SFtpClientImpl sclient = null;
		if (param.isLeaf()){
			Object o = param.getLeafExpression().calculate(ctx);
			if (o instanceof FtpClientImpl) client = (FtpClientImpl)o;
			else if (o instanceof SFtpClientImpl) sclient = (SFtpClientImpl)o;
			else throw new RQException("first parameter is not ftp_client");
		}else{
			size = param.getSubSize();
			if (size < 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("ftp_dir" + mm.getMessage("function.invalidParam"));
			}
		}
		
		String path = null;
		boolean onlyDir = option!=null&&option.indexOf("d")>=0;
		boolean fullPath = option!=null&&option.indexOf("p")>=0;
		boolean mkdir = option!=null&&option.indexOf("m")>=0;
		boolean deldir = option!=null&&option.indexOf("r")>=0;
		ArrayList<String> patterns = new ArrayList<String>();
		for (int i=0; i<size; i++) {
			if (param.getSub(i) == null ) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("ftp_get" + mm.getMessage("function.invalidParam"));
			}
			
			if (i==0) {
				Object o = param.getSub(i).getLeafExpression().calculate(ctx);
				if (o instanceof FtpClientImpl) client = (FtpClientImpl)o;
				else if (o instanceof SFtpClientImpl) sclient = (SFtpClientImpl)o;
				else throw new RQException("first parameter is not ftp_client");
			} else {
				patterns.add((String)param.getSub(i).getLeafExpression().calculate(ctx));
			}
		}
		
		if (path == null){
			path = client.getCurrentDir();
		}
		
		Sequence s = null;
		try {
			if (client != null) {				
				if (mkdir) s = client.mkdir(patterns);
				else if (deldir) s = client.deldir(patterns);
				else s = client.dirList(path, patterns,onlyDir,fullPath);
			} else {
				s = sclient.dirList(path, patterns,onlyDir,fullPath);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RQException("ftp_dir : " + e.getMessage());
		}
		return s;
	}

}
