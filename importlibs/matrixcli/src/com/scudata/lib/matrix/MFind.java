package com.scudata.lib.matrix;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.fn.algebra.Matrix;
import com.scudata.resources.EngineMessage;


/**
 * 在矩阵中查找元素位置
 * mfind(A)，查找A中非空数值的位置，从1开始，先列后行
 * mfind(A, n)，查找A中前n个非空数值的的位置，从1开始，先列后行
 * @author bd
 *
 */
public class MFind extends Function {
	public Object calculate (Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("mfind" + mm.getMessage("function.missingParam"));
		} else {
			Object oa = null;
			Object o2 = null;
			if (param.isLeaf()) {
				// 只有一个参数，mfind(A), 查找非零成员
				oa = param.getLeafExpression().calculate(ctx);
			}
			else if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("mfind" + mm.getMessage("function.invalidParam"));
			}
			else {
				IParam sub1 = param.getSub(0);
				IParam sub2 = param.getSub(1);
				if (sub1 == null || sub2 == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mfind" + mm.getMessage("function.invalidParam"));
				}
				oa = sub1.getLeafExpression().calculate(ctx);
				o2 = sub2.getLeafExpression().calculate(ctx);
			}
			if (oa instanceof Sequence) {
				Matrix A = new Matrix((Sequence)oa);
				if (o2 != null && !(o2 instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mfind" + mm.getMessage("function.paramTypeError"));
				}
				else {
					int b = 1;
					if (o2 instanceof Number) {
						b = ((Number) o2).intValue();
					}
					return find(A, b);
				}
			}
			MessageManager mm = EngineMessage.get();
			throw new RQException("mfind" + mm.getMessage("function.paramTypeError"));
		}
	}
	
	protected static Sequence find(Matrix A, int n) {
        int rows = A.getRows();
        int cols = A.getCols();
        double[][] vs = A.getArray();
        Sequence result = new Sequence(n);
        int len = 0;
    	for (int c = 0; c < cols; c++) {
    		for (int r = 0; r < rows; r++) {
        		if (vs[r][c] != 0) {
        			int loc = c*rows + r + 1;
        			result.add(loc);
        			len++;
        			if (len >= n) {
        				return result;
        			}
        		}
        	}
        }
        return result;
	}
}
