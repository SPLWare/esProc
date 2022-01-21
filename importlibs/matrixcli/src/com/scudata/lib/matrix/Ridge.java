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

public class Ridge extends Function {

	/**
	 * 回归函数ridge(A,F)和ridge(A, Y, learning_rate, iterations)
	 * @param ctx
	 * @return
	 */
	public Object calculate (Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("ridge" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("ridge" + mm.getMessage("function.invalidParam"));
		} else {
			if (param.getSubSize() == 4) {
				// 返回fit结果序列，参数由4个成员组成，第1个为二维序列，第2个为b
				IParam sub1 = param.getSub(0);
				IParam sub2 = param.getSub(1);
				IParam sub3 = param.getSub(2);
				IParam sub4 = param.getSub(3);
				if (sub1 == null || sub2 == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("ridge" + mm.getMessage("function.invalidParam"));
				}
				Object o1 = sub1.getLeafExpression().calculate(ctx);
				Object o2 = sub2.getLeafExpression().calculate(ctx);
				double ld = 0.01;
				int iter = 1000;
				if (sub3 != null) {
					Object o3 = sub3.getLeafExpression().calculate(ctx);
					if (o3 instanceof Number) {
						ld = ((Number) o3).doubleValue();
					}
				}
				if (sub4 != null) {
					Object o4 = sub4.getLeafExpression().calculate(ctx);
					if (o4 instanceof Number) {
						iter = ((Number) o4).intValue();
					}
				}
				if (o1 instanceof Sequence && o2 instanceof Sequence ) {
					Matrix A = new Matrix((Sequence)o1);
					Matrix T = new Matrix((Sequence)o2);
					Object v1 = ((Sequence)o2).get(1);
					// edited by bd, 2021.2.25, 如果T为单行向量时，自动转置
					boolean ifv = v1 instanceof Number;
					if (T.getRows() == 1) {
						T = T.transpose();
						ifv = true;
					}
					if (A.getRows() != T.getRows()) {
						throw new RQException("In the ridge(A, Y, learning_rate, iterations) function, A and Y must have the same number of rows");
					}
					String v = ifv? "v" : null;
					Sequence coef = fit(A, T, ld, iter);
					if (v == null) {
					}
					// return coef.toSequence(v, true);
					return coef;
				}
				else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("ridge" + mm.getMessage("function.invalidParam"));
				}
			}
			else if (param.getSubSize() == 2) {
				// 返回拟合结果，结果为矩阵转换为的二维序列，参数由2个成员组成，第1个为二维序列，第2个为fit获得coef序列
				IParam sub1 = param.getSub(0);
				IParam sub2 = param.getSub(1);
				if (sub1 == null || sub2 == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("ridge" + mm.getMessage("function.invalidParam"));
				}
				Object o1 = sub1.getLeafExpression().calculate(ctx);
				Object o2 = sub2.getLeafExpression().calculate(ctx);
				// option = option == null ? "v" : option+"v";
				if (o1 instanceof Sequence && o2 instanceof Sequence) {
					Sequence s1 = (Sequence) o1;
					Sequence s2 = (Sequence) o2;
					if (s1.length() > 0 && s2.length() > 0) {
						//Object v1 = s1.get(1);
						Matrix A = new Matrix(s1);
						Object v2 = s2.get(1);
						// edited by bd, 2021.3.5, 如果A为单列向量时，自动转置
						if (A.getCols() == 1) {
							A = A.transpose();
						}
						
						if (v2 instanceof Sequence) {
							// edited by bd, 2021.3.17, 支持多列，如果建模时Y为多列，返回结果就是矩阵
							Sequence coef = (Sequence) s2;
							if (coef.length() != 2) {
								MessageManager mm = EngineMessage.get();
								throw new RQException("ridge" + mm.getMessage("function.invalidParam"));
							}
							Sequence wSeq = (Sequence) coef.get(1);
							Matrix W = new Matrix(wSeq);
							Object bObj = coef.get(2);
							double b = 0;
							if (bObj instanceof Number) {
								b = ((Number) bObj).doubleValue();
							}
							Matrix result = predict(A, W, b);
							return result.toSequence(option, true);
						}
					}
					else {
						Logger.warn("The data in ridge() can't be empty.");
						MessageManager mm = EngineMessage.get();
						throw new RQException("ridge" + mm.getMessage("function.invalidParam"));
					}
				}
				else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("ridge" + mm.getMessage("function.invalidParam"));
				}
			}
			else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("ridge" + mm.getMessage("function.invalidParam"));
			}
		}
		MessageManager mm = EngineMessage.get();
		throw new RQException("ridge" + mm.getMessage("function.invalidParam"));
	}
	
    protected static Sequence fit(Matrix X, Matrix Y, double learning_rate, int iterrations) {
        int n = X.getCols();
        int m = X.getRows();

        double[][] W = new double[n][1];
        double b = 0;
        Matrix WMatrix = new Matrix(W);


        for (int i = 0; i < iterrations; i += 1) {

            Matrix YPred = predict(X, WMatrix, b);
            // calculate gradients
            Matrix a1 = Y.minus(YPred);
            Matrix a2 = X.transpose().times(a1);

            Matrix dw = ((a2.divide(-1 / 2.0)).plus(WMatrix.divide(1 / 2.0))).divide(m);
            double db = -2 * a1.elementSum() / m;

            // update weights
            WMatrix = WMatrix.minus(dw.divide(1 / learning_rate));
            b = b - db * learning_rate;
        }
        Sequence result = new Sequence(2);
        result.add(WMatrix.toSequence(null, true));
        result.add(b);
        return result;
    }
    
    protected static Matrix predict(Matrix X, Matrix W, double b) {
        Matrix YpredMatrix = X.times(W).plus(b);
        return YpredMatrix;
    }
}