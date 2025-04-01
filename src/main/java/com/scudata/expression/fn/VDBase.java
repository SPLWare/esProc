package com.scudata.expression.fn;

import java.io.File;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.FileObject;
import com.scudata.dm.JobSpace;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.expression.ParamInfo2;
import com.scudata.resources.EngineMessage;
import com.scudata.vdb.Library;
import com.scudata.vdb.VDB;

public class VDBase extends Function {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("vdbase" + mm.getMessage("function.missingParam"));
		}
	}

	public Object calculate(Context ctx) {
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
