package com.raqsoft.expression.fn.parallel;

import java.util.ArrayList;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.JobSpace;
import com.raqsoft.dm.Machines;
import com.raqsoft.dm.ParallelCaller;
import com.raqsoft.dm.Sequence;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;


// callx(dfx,…;;x)callx@la(dfx,…;hs;x)
public class Callx extends Function {
	public Node optimize(Context ctx) {
		if (param != null)
			param.optimize(ctx);
		return this;
	}

	
	public Object calculate(Context ctx) {
		IParam leftParam = null;
		String[] hosts = null;
		int[] ports = null;
		String reduce = null;

		if (param == null) {
		} else if (param.getType() == IParam.Semicolon) {
			int size = param.getSubSize();
			if (size > 3) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("callx" + mm.getMessage("function.invalidParam"));
			}

			leftParam = param.getSub(0);

			IParam rightParam = param.getSub(1);
			IParam hubParam = null;
			if (rightParam != null && rightParam.getType() == IParam.Comma) {
				hubParam = rightParam.getSub(0);
				IParam sParam = rightParam.getSub(1);

				if(sParam.getSubSize()==0){//s or m
					Object indexObj = sParam.getLeafExpression().calculate(
							ctx);
					if (!(indexObj instanceof Sequence)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("callx" + mm.getMessage("function.invalidParam"));
					} else {
//						taskIndexes = (Sequence) indexObj;
					}
				}else{
					MessageManager mm = EngineMessage.get();
					throw new RQException("callx" + mm.getMessage("function.invalidParam"));
				}
				
			} else {
				hubParam = rightParam;
			}
			
			if (hubParam != null) {
				Object hostObj = hubParam.getLeafExpression().calculate(ctx);
				Machines mc = new Machines();
				if (!mc.set(hostObj)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("callx"
							+ mm.getMessage("function.invalidParam"));
				}

				hosts = mc.getHosts();
				ports = mc.getPorts();
			}

			if (size > 2) {
				IParam reduceParam = param.getSub(2);
				if (reduceParam != null) {
//					reduce = reduceParam.getLeafExpression().toString();
					reduce = (String)reduceParam.getLeafExpression().calculate(ctx);
				}
			}
		} else {
			leftParam = param;
		}

		String dfx;
		IParam dfxParam;
		int mcount = -1; // 并行数
		Object[] args = null; // 参数

		if (leftParam != null && leftParam.getType() == IParam.Comma) {
			dfxParam = leftParam.getSub(0);
			int pcount = leftParam.getSubSize() - 1;
			args = new Object[pcount];
			for (int p = 0; p < pcount; ++p) {
				IParam sub = leftParam.getSub(p + 1);
				if (sub != null) {
					args[p] = sub.getLeafExpression().calculate(ctx);
					if (args[p] instanceof Sequence) {
						int len = ((Sequence) args[p]).length();
						if (len == 0) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("callx"
									+ mm.getMessage("function.invalidParam"));
						}

						if (mcount == -1) {
							mcount = len;
						} else if (mcount != len) {
							MessageManager mm = EngineMessage.get();
							throw new RQException(
									"callx" + mm.getMessage("function.paramCountNotMatch"));
						}
					}
				}
			}
		} else {
			dfxParam = leftParam;
		}

		if (dfxParam == null) {
			dfx = null;
		} else if (dfxParam.isLeaf()) {
			Object obj = dfxParam.getLeafExpression().calculate(ctx);
			if (!(obj instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("callx" + mm.getMessage("function.paramTypeError"));
			}

			dfx = (String) obj;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("callx" + mm.getMessage("function.invalidParam"));
		}

		ParallelCaller caller = new ParallelCaller(dfx, hosts, ports);
		caller.setContext(ctx);
		caller.setOptions(option);
		caller.setReduce(reduce);

		if (args != null) {
			if (mcount == -1) {
				mcount = 1;
			}

			int pcount = args.length;
			for (int i = 1; i <= mcount; ++i) {
				ArrayList<Object> list = new ArrayList<Object>(pcount);
				for (int p = 0; p < pcount; ++p) {
					if (args[p] instanceof Sequence) {
						Sequence sequence = (Sequence) args[p];
						list.add(sequence.get(i));
					} else {
						list.add(args[p]);
					}
				}

				caller.addCall(list);
			}
		}

		JobSpace js = ctx.getJobSpace();
		if (js != null)
			caller.setJobSpaceId(js.getID());

		return caller.execute();
	}
}
