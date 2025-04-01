package com.scudata.lib.math;

import com.scudata.common.Logger;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.fn.algebra.Matrix;
import com.scudata.resources.EngineMessage;

public class ElasticNet extends Function {
	/**
	 * 回归函数elasticnet(A,F)和elasticnet(A, Y, learning_rate, iterations, l1, l2)
	 * @param ctx
	 * @return
	 */
	public Object calculate (Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("elasticnet" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("elasticnet" + mm.getMessage("function.invalidParam"));
		} else {
			if (param.getSubSize() == 6) {
				// 返回fit结果序列，参数由4个成员组成，第1个为二维序列，第2个为b
				IParam sub1 = param.getSub(0);
				IParam sub2 = param.getSub(1);
				IParam sub3 = param.getSub(2);
				IParam sub4 = param.getSub(3);
				IParam sub5 = param.getSub(4);
				IParam sub6 = param.getSub(5);
				if (sub1 == null || sub2 == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("elasticnet" + mm.getMessage("function.invalidParam"));
				}
				Object o1 = sub1.getLeafExpression().calculate(ctx);
				Object o2 = sub2.getLeafExpression().calculate(ctx);
				double r = 0.01;
				int p = 1000;
				double l1 = 0.9;
				double l2 = 0.1;
				if (sub3 != null) {
					Object o3 = sub3.getLeafExpression().calculate(ctx);
					if (o3 instanceof Number) {
						r = ((Number) o3).doubleValue();
					}
				}
				if (sub4 != null) {
					Object o4 = sub4.getLeafExpression().calculate(ctx);
					if (o4 instanceof Number) {
						p = ((Number) o4).intValue();
					}
				}
				if (sub5 != null) {
					Object o5 = sub5.getLeafExpression().calculate(ctx);
					if (o5 instanceof Number) {
						l1 = ((Number) o5).doubleValue();
					}
				}
				if (sub6 != null) {
					Object o6 = sub6.getLeafExpression().calculate(ctx);
					if (o6 instanceof Number) {
						l2 = ((Number) o6).doubleValue();
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
						throw new RQException("In the elasticnet(A, Y, r, p, l1, l2) function, A and Y must have the same number of rows");
					}
					String v = ifv? "v" : null;
					Sequence coef = fit(A, T, r, p, l1, l2);
					if (v == null) {
					}
					// return coef.toSequence(v, true);
					return coef;
				}
				else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("elasticnet" + mm.getMessage("function.invalidParam"));
				}
			}
			else if (param.getSubSize() == 2) {
				// 返回拟合结果，结果为矩阵转换为的二维序列，参数由2个成员组成，第1个为二维序列，第2个为fit获得coef序列
				IParam sub1 = param.getSub(0);
				IParam sub2 = param.getSub(1);
				if (sub1 == null || sub2 == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("elasticnet" + mm.getMessage("function.invalidParam"));
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
						Logger.warn("The data in elasticnet() can't be empty.");
						MessageManager mm = EngineMessage.get();
						throw new RQException("elasticnet" + mm.getMessage("function.invalidParam"));
					}
				}
				else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("elasticnet" + mm.getMessage("function.invalidParam"));
				}
			}
			else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("elasticnet" + mm.getMessage("function.invalidParam"));
			}
		}
		MessageManager mm = EngineMessage.get();
		throw new RQException("elasticnet" + mm.getMessage("function.invalidParam"));
	}
	
    protected static Sequence fit(Matrix X, Matrix Y, double learning_rate, int iterations, double l1_penality, double l2_penality) {
        int m = X.getRows();
        int n = X.getCols();
        double[][] w = new double[n][1];
        
        double b = 0;
        Matrix W = new Matrix(w);

        for (int i = 0; i < iterations; i++) {
            Matrix YPred = predict(X, W, b);
            double[][] dw = new double[n][1];
            Matrix dwMatirx = new Matrix(dw);
            for (int j = 0; j < n; j++) {
                Matrix XSliceMatirx= new Matrix(twoArraySlice(X,j));
                Matrix tmp11 = XSliceMatirx.divide(1.0/2);
                Matrix tmp12 = Y.minus(YPred);
                double tmp13 = tmp11.dot(tmp12);
                double tmp1 = -1 * tmp13;
                double tmp2 = l1_penality+2*l2_penality*W.get(j, 0);
                if (W.get(j, 0) >0) {
                    dwMatirx.set(j, 0,  (tmp1+tmp2)/m);
                }
                else {
                    dwMatirx.set(j, 0,  (tmp1-tmp2)/m);
                }
            }
            double db = -2* Y.minus(YPred).elementSum()/m;
            W = W.minus(dwMatirx.divide(1/learning_rate));
            b = b-db*learning_rate;
            // int a = 1;
        }
        Sequence result = new Sequence(2);
        result.add(W.toSequence(null, true));
        result.add(b);
        return result;
    }

    private static double[][] twoArraySlice(Matrix input,int c) {
        double[][] oneArray = new double[input.getRows()][1];
        for (int r=0; r < input.getRows(); r++){
            oneArray[r][0] = input.get(r, c);
        }
        return oneArray;

    }
    
    protected static Matrix predict(Matrix X, Matrix W, double b) {
        return X.times(W).plus(b);
    }
}
