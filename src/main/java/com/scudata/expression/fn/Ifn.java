package com.scudata.expression.fn;

import com.scudata.array.IArray;
import com.scudata.array.ObjectArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

// ifn(n1,…)
public class Ifn extends Function {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("ifn" + mm.getMessage("function.missingParam"));
		}
	}

	public Object calculate(Context ctx) {
		IParam param = this.param;
		if (param.isLeaf()) {
			return param.getLeafExpression().calculate(ctx);
		} else {
			int size = param.getSubSize();
			for (int i = 0; i < size; ++i) {
				IParam sub = param.getSub(i);
				if (sub == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("ifn" + mm.getMessage("function.invalidParam"));
				}
				
				Object obj = sub.getLeafExpression().calculate(ctx);
				if (obj != null) return obj;
			}

			return null;
		}
	}

	/**
	 * 计算出所有行的结果
	 * @param ctx 计算上行文
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx) {
		IParam param = this.param;
		if (param.isLeaf()) {
			return param.getLeafExpression().calculateAll(ctx);
		} else {
			int psize = param.getSubSize();
			IArray []arrays = new IArray[psize];
			boolean isSameType = true;
			
			for (int i = 0; i < psize; ++i) {
				IParam sub = param.getSub(i);
				if (sub == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("ifn" + mm.getMessage("function.invalidParam"));
				}
				
				arrays[i] = sub.getLeafExpression().calculateAll(ctx);
				if (isSameType && i > 0 && arrays[i].getClass() != arrays[i - 1].getClass()) {
					isSameType = false;
				}
			}
			
			IArray result;
			int len = arrays[0].size();
			
			if (isSameType) {
				result = arrays[0].dup();
			} else {
				result = new ObjectArray(len);
				result.addAll(arrays[0]);
			}
			
			Next:
			for (int i = 1; i <= len; ++i) {
				if (result.isNull(i)) {
					for (int p = 1; p < psize; ++p) {
						if (!arrays[p].isNull(i)) {
							result.set(i, arrays[p].get(i));
							continue Next;
						}
					}
				}
			}
			
			result.setTemporary(true);
			return result;
		}
	}
	
	/**
	 * 计算signArray中取值为sign的行
	 * @param ctx
	 * @param signArray 行标识数组
	 * @param sign 标识
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx, IArray signArray, boolean sign) {
		IParam param = this.param;
		if (param.isLeaf()) {
			return param.getLeafExpression().calculateAll(ctx, signArray, sign);
		} else {
			int psize = param.getSubSize();
			IArray []arrays = new IArray[psize];
			boolean isSameType = true;
			
			for (int i = 0; i < psize; ++i) {
				IParam sub = param.getSub(i);
				if (sub == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("ifn" + mm.getMessage("function.invalidParam"));
				}
				
				arrays[i] = sub.getLeafExpression().calculateAll(ctx, signArray, sign);
				if (isSameType && i > 0 && arrays[i].getClass() != arrays[i - 1].getClass()) {
					isSameType = false;
				}
			}
			
			IArray result;
			int len = arrays[0].size();
			
			if (isSameType) {
				result = arrays[0].dup();
			} else {
				result = new ObjectArray(len);
				result.addAll(arrays[0]);
			}
			
			Next:
			for (int i = 1; i <= len; ++i) {
				if (result.isNull(i)) {
					for (int p = 1; p < psize; ++p) {
						if (!arrays[p].isNull(i)) {
							result.set(i, arrays[p].get(i));
							continue Next;
						}
					}
				}
			}
			
			result.setTemporary(true);
			return result;
		}
	}
}
