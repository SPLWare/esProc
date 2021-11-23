package com.scudata.expression.fn.algebra;

import com.scudata.common.Logger;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

public class Lasso extends Function {

	/**
	 * 回归函数lasso(X,Y,r,p)和lasso(X,F), 返回的主成分系数矩阵无法降维，但可以用来计算主成分得分
	 * @param ctx
	 * @return
	 */
	public Object calculate (Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("lasso" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("lasso" + mm.getMessage("function.invalidParam"));
		} else {
			if (param.getSubSize() == 4) {
				// 返回fit结果序列，参数由4个成员组成，第1个为二维序列，第2个为b
				IParam sub1 = param.getSub(0);
				IParam sub2 = param.getSub(1);
				IParam sub3 = param.getSub(2);
				IParam sub4 = param.getSub(3);
				if (sub1 == null || sub2 == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("lasso" + mm.getMessage("function.invalidParam"));
				}
				Object o1 = sub1.getLeafExpression().calculate(ctx);
				Object o2 = sub2.getLeafExpression().calculate(ctx);
				double r = 0.01;
				int p = 1000;
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
						throw new RQException("In the lasso(A, Y, r, p) function, A and Y must have the same number of rows");
					}
					String v = ifv? "v" : null;
					Sequence coef = fit(A, T, r, p);
					if (v == null) {
					}
					// return coef.toSequence(v, true);
					return coef;
				}
				else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("lasso" + mm.getMessage("function.invalidParam"));
				}
			}
			else if (param.getSubSize() == 2) {
				// 返回拟合结果，结果为矩阵转换为的二维序列，参数由2个成员组成，第1个为二维序列，第2个为fit获得coef序列
				IParam sub1 = param.getSub(0);
				IParam sub2 = param.getSub(1);
				if (sub1 == null || sub2 == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("lasso" + mm.getMessage("function.invalidParam"));
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
							Matrix result = predict(A, coef);
							return result.toSequence(option, true);
						}
					}
					else {
						Logger.warn("The data in lasso() can't be empty.");
						MessageManager mm = EngineMessage.get();
						throw new RQException("lasso" + mm.getMessage("function.invalidParam"));
					}
				}
				else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("lasso" + mm.getMessage("function.invalidParam"));
				}
			}
			else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("lasso" + mm.getMessage("function.invalidParam"));
			}
		}
		MessageManager mm = EngineMessage.get();
		throw new RQException("lasso" + mm.getMessage("function.invalidParam"));
	}
	
    protected static Sequence fit(Matrix X, Matrix Y, double learning_rate, double epochs) {
        double b = 0;
        double[][] w = new double[X.getCols()][1];
        Matrix wMatrix = new Matrix(w);
        Matrix dw;

        for (int iter = 1; iter < epochs; iter++){
            w = wMatrix.getArray();
            double alpha = 0.1;
            int num_train = X.getRows();

            Matrix y_hat = X.times(wMatrix).plus(b);
            Matrix allLoss= y_hat.minus(Y).elementSquare().divide(num_train + sumDoubleAlpha(w,alpha));
            double loss = sumDouble(allLoss.getArray());
            if (loss < 0) {
            	// 避免警告
            }
            double[][] sign = signW(w,alpha);
            Matrix tttt= X.transpose().times(y_hat.minus(Y)).divide(num_train);
            Matrix tpppp = new Matrix(sign);
            dw = tttt.plus(tpppp);
            double db = y_hat.minus(Y).elementSum()/num_train;
            wMatrix = wMatrix.plus(dw.divide(-1/learning_rate));
            b+= -learning_rate*db;
            //int bb=1;
        }
        Sequence result = new Sequence(2);
        result.add(wMatrix.toSequence(null, true));
        result.add(b);
        return result;
    }
    
    protected static double rmse(Matrix A, Matrix B) {
    	double mse = A.mse(B);
    	return Math.sqrt(mse);
    }
    
    protected static Matrix predict(Matrix X, Sequence result) {
    	Sequence wSeq = (Sequence) result.get(1);
    	Matrix wMatrix = new Matrix(wSeq); 
    	double b = ((Number) result.get(2)).doubleValue();
        return X.times(wMatrix).plus(b);
    }
    
    private static double sumDoubleAlpha(double[][] X, double alpha) {
        double sum=0;
        for (int i=0;i<X.length;i++){
            sum+= alpha*Math.abs(X[i][0]);
        }
        return sum;
    }
    
    private static double sumDouble(double[][] X) {
        double sum=0;
        for (int i=0;i<X.length;i++){
            sum+= X[i][0];
        }
        return sum;
    }

    private static double[][] signW(double[][] w,double alpha) {
        double[][] result = new double[w.length][w[0].length];
        for (int i=0;i<w.length;i++){
            for (int j=0;j<w[0].length;j++){
                if (w[i][j] >0){
                    result[i][j] = 1*alpha;

                }
                else if (w[i][j] <0) {
                    result[i][j]=-1*alpha;
                }
                else {
                    result[i][j]=0*alpha;
                }
            }
        }
        return  result;
    }
}
