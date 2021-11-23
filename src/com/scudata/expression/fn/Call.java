package com.scudata.expression.fn;

import java.io.File;

import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.DfxManager;
import com.scudata.dm.FileObject;
import com.scudata.dm.Param;
import com.scudata.dm.ParamList;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

/**
 * 调用指定网格，返回网格的返回值，多返回值拼成序列
 * call(dfx,arg1,…) 传入参数arg1,…调用网格文件dfx，返回其第一个return值并关闭。
 * @author RunQian
 *
 */
public class Call extends Function {
	public Node optimize(Context ctx) {
		if (param != null) param.optimize(ctx);
		return this;
	}

	public Object calculate(Context ctx) {
		PgmCellSet pcs = getCallPgmCellSet(ctx);
		Object val = pcs.execute();
		
		if (option == null || option.indexOf('r') == -1) {
			pcs.reset();
			DfxManager.getInstance().putDfx(pcs);
		}
		
		return val;
	}
	
	// 在本地找dfx，找不到返回null
	public String getDfxPathName(Context ctx) {
		IParam param = this.param;
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("call" + mm.getMessage("function.missingParam"));
		} else if (!param.isLeaf()) {
			param = param.getSub(0);
			if (param == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("call" + mm.getMessage("function.invalidParam"));
			}
		}
		
		FileObject fo;
		Object obj = param.getLeafExpression().calculate(ctx);
		if (obj instanceof String) {
			fo = new FileObject((String)obj, null, "s", ctx);
		} else if (obj instanceof FileObject) {
			fo = (FileObject)obj;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("call" + mm.getMessage("function.paramTypeError"));
		}
		
		File file = fo.getLocalFile().getFile();
		if (file != null) {
			return file.getAbsolutePath();
		} else {
			return null;
		}
	}
	
	// ide用来取被调用网格进行单步跟踪
	public PgmCellSet getCallPgmCellSet(Context ctx) {
		IParam param = this.param;
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("call" + mm.getMessage("function.missingParam"));
		}

		boolean useCache = option == null || option.indexOf('r') == -1;
		DfxManager dfxManager = DfxManager.getInstance();
		PgmCellSet pcs;
		if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (obj instanceof String) {
				if (useCache) {
					pcs = dfxManager.removeDfx((String)obj, ctx);
				} else {
					pcs = dfxManager.readDfx((String)obj, ctx);
				}
			} else if (obj instanceof FileObject) {
				if (useCache) {
					pcs = dfxManager.removeDfx((FileObject)obj, ctx);
				} else {
					pcs = dfxManager.readDfx((FileObject)obj, ctx);
				}
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("call" + mm.getMessage("function.paramTypeError"));
			}
			
			pcs.setParamToContext();
		} else {
			IParam sub0 = param.getSub(0);
			if (sub0 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("call" + mm.getMessage("function.invalidParam"));
			}

			Object obj = sub0.getLeafExpression().calculate(ctx);
			if (obj instanceof String) {
				if (useCache) {
					pcs = dfxManager.removeDfx((String)obj, ctx);
				} else {
					pcs = dfxManager.readDfx((String)obj, ctx);
				}
			} else if (obj instanceof FileObject) {
				if (useCache) {
					pcs = dfxManager.removeDfx((FileObject)obj, ctx);
				} else {
					pcs = dfxManager.readDfx((FileObject)obj, ctx);
				}
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("call" + mm.getMessage("function.paramTypeError"));
			}

			pcs.setParamToContext();
			ParamList list = pcs.getParamList();
			if (list != null) {
				int size = param.getSubSize();
				if (size - 1 > list.count()) size = list.count() + 1;

				Context curCtx = pcs.getContext();
				for (int i = 1; i < size; ++i) {
					IParam sub = param.getSub(i);
					Param p = list.get(i - 1);
					if (sub != null) {
						Object val = sub.getLeafExpression().calculate(ctx);
						curCtx.setParamValue(p.getName(), val);
					} else {
						curCtx.setParamValue(p.getName(), null);
					}
				}
			}
		}
		
		return pcs;
	}
	
	// ide结束调用后调用次函数
	public void finish(PgmCellSet pcs) {
		if (option == null || option.indexOf('r') == -1) {
			pcs.reset();
			DfxManager.getInstance().putDfx(pcs);
		}
	}
}
