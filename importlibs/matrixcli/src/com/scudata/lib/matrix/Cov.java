package com.scudata.lib.matrix;

import com.scudata.common.Logger;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.fn.algebra.Matrix;
import com.scudata.resources.EngineMessage;

/**
 * 两个向量的协方差cov(A, B)
 * @author bd
 */
public class Cov extends Function {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("cov" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("cov" + mm.getMessage("function.invalidParam"));
		} else {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("cov" + mm.getMessage("function.invalidParam"));
			}

			IParam sub1 = param.getSub(0);
			IParam sub2 = param.getSub(1);
			if (sub1 == null || sub2 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("cov" + mm.getMessage("function.invalidParam"));
			}
			Object o1 = sub1.getLeafExpression().calculate(ctx);
			Object o2 = sub2.getLeafExpression().calculate(ctx);
			if (o1 instanceof Sequence && o2 instanceof Sequence) {
				// edited by bd, 2021.11.17, 在dis函数中，单层序列认为是横向量
				Sequence s1 = (Sequence) o1;
				Sequence s2 = (Sequence) o2;
				Matrix A = new Matrix(s1);
				Matrix B = new Matrix(s2);
				
				Object o11 = s1.length() > 0 ? s1.get(1) : null;
				Object o21 = s2.length() > 0 ? s2.get(1) : null;
				if (!(o11 instanceof Sequence)) {
					// A为单序列定义的向量，转成横向量
					A = A.transpose();
				}
				if (!(o21 instanceof Sequence)) {
					// A为单序列定义的向量，转成横向量
					B = B.transpose();
				}
				double[] as = A.getArray()[0];
				double[] bs = B.getArray()[0];
				int rs = as.length;
				if (rs != bs.length) {
					// 不同维
					MessageManager mm = EngineMessage.get();
					//edited by bd, 2020.3.10, 矩阵运算中的问题只输出错误信息，不中断，返回null
					Logger.warn("cov" + mm.getMessage("function.paramTypeError"));
					return null;
				}
				// 协方差
				double avga = 0;
				for (int i = 0; i < rs; i++) {
					avga += as[i];
				}
				avga /= rs;
				double avgb = 0;
				for (int i = 0; i < rs; i++) {
					avgb += bs[i];
				}
				avgb /= rs;
				double cov = 0;
				for (int i = 0; i < rs; i++) {
					cov += (as[i] - avga) * (bs[i] - avgb);
				}
				cov = cov/(rs - 1);
				return cov;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("cov" + mm.getMessage("function.paramTypeError"));
			}
		}
	}
}
