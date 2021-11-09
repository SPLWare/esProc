package com.raqsoft.expression.fn;

import java.io.File;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.FileObject;
import com.raqsoft.dm.JobSpace;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.Node;
import com.raqsoft.expression.ParamInfo2;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.vdb.Library;
import com.raqsoft.vdb.VDB;

public class VDBase extends Function {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("vdbase" + mm.getMessage("function.missingParam"));
		}
		
		if (option != null && option.indexOf('r') != -1) {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("vdbase" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);
			if (sub0 == null || sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("vdbase" + mm.getMessage("function.invalidParam"));
			}
			
			Object p0 = sub0.getLeafExpression().calculate(ctx);
			String pathName0;
			if (p0 instanceof String) {
				pathName0 = (String)p0;
			} else if (p0 instanceof FileObject) {
				pathName0 = ((FileObject)p0).getFileName();
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("vdbase" + mm.getMessage("function.paramTypeError"));
			}
			
			Object p1 = sub1.getLeafExpression().calculate(ctx);
			String pathName1;
			if (p1 instanceof String) {
				pathName1 = (String)p1;
			} else if (p1 instanceof FileObject) {
				pathName1 = ((FileObject)p1).getFileName();
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("vdbase" + mm.getMessage("function.paramTypeError"));
			}
			
			JobSpace js = ctx.getJobSpace();
			if (js != null && js.getAppHome() != null) {
				File file = new File(js.getAppHome(), pathName0);
				pathName0 = file.getAbsolutePath();
				file = new File(js.getAppHome(), pathName1);
				pathName1 = file.getAbsolutePath();
			}
			
			Library library = new Library(pathName0);
			library.start();
			boolean result = library.reset(pathName1);
			library.stop();
			return result;
		}
		
		String pathName;
		Object []keys = null;
		int []lens = null;
		if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (obj instanceof String) {
				pathName = (String)obj;
			} else if (obj instanceof FileObject) {
				pathName = ((FileObject)obj).getFileName();
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("vdbase" + mm.getMessage("function.paramTypeError"));
			}
		} else if (param.getType() == IParam.Semicolon) {
			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);
			if (sub0 == null || sub1 == null || param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("vdbase" + mm.getMessage("function.invalidParam"));
			}
			
			Object obj = sub0.getLeafExpression().calculate(ctx);
			if (obj instanceof String) {
				pathName = (String)obj;
			} else if (obj instanceof FileObject) {
				pathName = ((FileObject)obj).getFileName();
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("vdbase" + mm.getMessage("function.paramTypeError"));
			}

			ParamInfo2 pi = ParamInfo2.parse(sub1, "vdbase", true, true);
			keys = pi.getValues1(ctx);
			Object []nums = pi.getValues2(ctx);
			int len = nums.length;
			lens = new int[len];
			for (int i = 0; i < len; ++i) {
				if (!(nums[i] instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("vdbase" + mm.getMessage("function.paramTypeError"));
				}
				
				lens[i] = ((Number)nums[i]).intValue();
				if (lens[i] < 2) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("vdbase" + mm.getMessage("function.invalidParam"));
				}
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("vdbase" + mm.getMessage("function.invalidParam"));
		}
		
		JobSpace js = ctx.getJobSpace();
		if (js != null && js.getAppHome() != null) {
			File file = new File(js.getAppHome(), pathName);
			pathName = file.getAbsolutePath();
		}
		
		Library library = Library.instance(pathName);		
		if (keys != null) {
			library.createKeyLibrary(keys, lens);
		}
		
		VDB vdb = library.createVDB();
		ctx.addResource(vdb);
		return vdb;
	}
	
	public byte calcExpValueType(Context ctx) {
		return Expression.TYPE_DB;
	}

	public Node optimize(Context ctx) {
		return this;
	}
}
