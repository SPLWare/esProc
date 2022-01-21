package com.scudata.lib.matrix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.scudata.common.Logger;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.fn.algebra.Matrix;
import com.scudata.expression.fn.algebra.SVDecomposition;
import com.scudata.expression.fn.algebra.Vector;
import com.scudata.resources.EngineMessage;

public class PLS extends Function {
	/**
	 * pls(A,F)和pls(A,T,n)
	 * @param ctx	上下文
	 * @return
	 */
	public Object calculate (Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pls" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pls" + mm.getMessage("function.invalidParam"));
		} else {
			if (param.getSubSize() == 3) {
				// 返回fit结果coef序列，结果处理为序列，参数由3个成员组成，前2个为二维序列，第3个为主成分数
				IParam sub1 = param.getSub(0);
				IParam sub2 = param.getSub(1);
				IParam sub3 = param.getSub(2);
				if (sub1 == null || sub2 == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("pls" + mm.getMessage("function.invalidParam"));
				}
				Object o1 = sub1.getLeafExpression().calculate(ctx);
				Object o2 = sub2.getLeafExpression().calculate(ctx);
				int components = 0;
				if (sub3 != null) {
					Object o3 = sub3.getLeafExpression().calculate(ctx);
					if (o3 instanceof Number) {
						components = ((Number) o3).intValue();
					}
				}
				if (o1 instanceof Sequence && o2 instanceof Sequence ) {
					Matrix A = new Matrix((Sequence)o1);
					Matrix T = new Matrix((Sequence)o2);
					Object v1 = ((Sequence)o2).get(1);
					boolean ifv = v1 instanceof Number;
					if (T.getRows() == 1) {
						T = T.transpose();
						ifv = true;
					}
					if (A.getRows() != T.getRows()) {
						throw new RQException("In the pls(A, Y, n) function, A and Y must have the same number of rows");
					}
					int ncomp = 0;
					if ( A.getRows() < A.getCols()) {
						ncomp = A.getRows() - 1;
						if (components > ncomp) {
							Logger.warn("In the pls(A, Y, n) function, compnents n must less than the rows number of A, set n to "+ ncomp +".");
							components = ncomp;
						}
					}
					else {
						ncomp = A.getCols();
						if (components > ncomp) {
							Logger.warn("In the pls(A, Y, n) function, compnents n can't greater than the cols number of A, set n to "+ ncomp +".");
							components = ncomp;
						}
					}
					if (components <= 0) {
						Logger.warn("In the pls(A, Y, n) function, compnents n must greater than 0, set n to "+ ncomp +".");
						components = ncomp;
					}
					String v = ifv? "v" : null;
			    	// 支持Y多列，返回值改为Matrix
					Matrix coef = fit(A, T, components);
					return coef.toSequence(v, true);
				}
				else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("pls" + mm.getMessage("function.invalidParam"));
				}
			}
			else if (param.getSubSize() == 2) {
				// 返回拟合结果，结果为矩阵转换为的二维序列，参数由2个成员组成，第1个为二维序列，第2个为fit获得coef序列
				IParam sub1 = param.getSub(0);
				IParam sub2 = param.getSub(1);
				if (sub1 == null || sub2 == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("pls" + mm.getMessage("function.invalidParam"));
				}
				Object o1 = sub1.getLeafExpression().calculate(ctx);
				Object o2 = sub2.getLeafExpression().calculate(ctx);
				if (o1 instanceof Sequence && o2 instanceof Sequence) {
					Sequence s1 = (Sequence) o1;
					Sequence s2 = (Sequence) o2;
					if (s1.length() > 0 && s2.length() > 0) {
						Object v1 = s1.get(1);
						Matrix A = new Matrix(s1);
						Object v2 = s2.get(1);
						// 如果A为单列向量时，自动转置
						if (A.getCols() == 1) {
							A = A.transpose();
						}
						
						if (v2 instanceof Sequence) {
							// 支持多列，如果建模时Y为多列，返回结果就是矩阵
							Matrix coef = new Matrix(s2);
							Matrix result = predictY(A, coef);
							return result.toSequence(option, true);
						}
						else {
							// 建模时Y为向量，返回值根据A为向量或单值
							Vector coef = new Vector(s2);
							Matrix result = predictY(A, coef);
							option = option == null ? "v" : option+"v";
							Object res = result.toSequence(option, true);
							if (v1 instanceof Sequence) {
								// pls(A,T)中A是矩阵
								if (! (res instanceof Sequence)) {
									Sequence seq = new Sequence(1);
									seq.add(res);
									return seq;
								}
								return res;
							}
							else {
								// pls(A,T)中A是向量，单列时返回单值
								if (res instanceof Sequence) {
									return ((Sequence) res).get(1);
								}
								return res;
							}
						}
					}
					else {
						Logger.warn("The data in pls() can't be empty.");
						MessageManager mm = EngineMessage.get();
						throw new RQException("pls" + mm.getMessage("function.invalidParam"));
					}
				}
				else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("pls" + mm.getMessage("function.invalidParam"));
				}
			}
			else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pls" + mm.getMessage("function.invalidParam"));
			}
		}
	}
	
	/**
	 * PLS训练
	 * @param X_input
	 * @param Y_input
	 * @param components
	 * @return
	 */
    public Matrix fit(Matrix X_input, Matrix Y_input, int components){
    	// 支持Y多列，返回值改为Matrix, 先做X_input和Y_input的中心化，得到X和Y，每列均值为0
    	Matrix X = X_input.changeAverageToZero();
    	Matrix Y = Y_input.changeAverageToZero();
        Matrix x_weights_store = new Matrix(new double[X_input.getCols()][components]);
        Matrix S = X.transpose().times(Y);//At*Y, C*Yc矩阵
        Matrix Si = S;
        Matrix T = new Matrix(new double[X_input.getRows()][components]);
        Matrix P = new Matrix(new double[X_input.getCols()][components]);
        for (int k=0; k<components; k++) {
            List<double[][]> svd_result = svd_cross_product(Si);
            double[][] U = svd_result.get(0);
            // X的投影轴
            Matrix w = new Matrix(U);
            // X的得分
            Matrix t = X.times(w);

            // X的回归系数，载荷
            Matrix p = ((X.transpose()).times(t)).divide(t.transpose().times(t).get(0, 0));

            //存储weights，scores，loadings
            ravel(x_weights_store, w, k);
            ravel(T, t, k);
            ravel(P, p, k);
            Matrix P2 = new Matrix(new double[X_input.getCols()][k+1]);
            ravel2(P2, P, k);
        	Si = S.minus(P2.times((P2.transpose().times(P2).inverse())).times(P2.transpose()).times(S));
        	// 如果得到的Si全为0，说明拟合无误差了，无法执行更高维的拟合
        	boolean zero = true;
        	for (int r = 0, rlen = Si.getRows(); r < rlen; r++) {
        		for (int c = 0, clen = Si.getCols(); c < clen; c++) {
        			if ( Si.get(r, c) != 0 ) {
        				zero = false;
        				r = rlen;
        				break;
        			}
        		}
        	}
        	if (zero) {
            	// 使用当前主成分数
            	components = k+1;
            	Matrix x_weights_store0 = x_weights_store;
                x_weights_store = new Matrix(new double[X_input.getCols()][components]);
                ravel2(x_weights_store, x_weights_store0, k);
            	Matrix T0 = T;
                T = new Matrix(new double[X_input.getRows()][components]);
                ravel2(T, T0, k);
                P = P2;
            	break;
        	}
        }
        Matrix pinv2_matrix = pinv2(T);
        Matrix x_rotations_matrix =x_weights_store.times(pinv2_matrix);
        Matrix coef_ = x_rotations_matrix.times(Y);
        
    	// 支持Y多列，返回值改为Matrix
        double[][] coef_array = coef_.getArray();
        double[] b = compute_b(X_input, coef_, Y_input);
        double[][] coef = new double[coef_array.length+1][b.length];
        for (int c = 0; c < b.length; c++ ) {
            coef[0][c] = b[c];
            for (int i=1;i< coef_array.length+1;i++) {
                coef[i][c] = coef_array[i-1][c];
            }
        }
        return new Matrix(coef);
    }

    /**
     * 预测Y, 拟合时Y为向量
     * @param X_input
     * @param coef
     * @return
     */
    public Matrix predictY(Matrix X_input, Vector coef){
        double b0 = coef.get(0);
        double[][] b = new double[X_input.getRows()][1];
        for (int i= 0, iSize = X_input.getRows();i < iSize;i++){
            b[i][0] = b0;
        }

        double[][] coef_other = new double[coef.len()-1][1];
        for (int i =0;i<coef.len()-1;i++){
            coef_other[i][0] = coef.get(i+1);
        }

        Matrix b_matrix = new Matrix(b);
        Matrix coef_other_matrix = new Matrix(coef_other);
        Matrix Ypred = X_input.times(coef_other_matrix).plus(b_matrix);
        return Ypred;
    }

    /**
     * 预测Y，拟合时Y为矩阵
     * @param X_input
     * @param coef
     * @return
     */
    public Matrix predictY(Matrix X_input, Matrix coef){
    	// 支持Y多列，coef不一定是向量，和上面的方法区分开
        double[][] b = new double[X_input.getRows()][coef.getCols()];
        double[][] coef_other = new double[coef.getRows()-1][coef.getCols()];
        for (int j= 0, jSize = coef.getCols();j < jSize;j++){
            double b0 = coef.get(0, j);
            for (int i= 0, iSize = X_input.getRows();i < iSize;i++){
                b[i][j] = b0;
            }
            for (int i = 0;i<coef.getRows()-1;i++){
                coef_other[i][j] = coef.get(i+1, j);
            }
        }

        Matrix b_matrix = new Matrix(b);
        Matrix coef_other_matrix = new Matrix(coef_other);
        Matrix Ypred = X_input.times(coef_other_matrix).plus(b_matrix);
        return Ypred;
    }

    /**
     * 
     * @param X_input
     * @param coef_matrix
     * @param Y_input
     * @return
     */
    private double[] compute_b(Matrix X_input, Matrix coef_matrix, Matrix Y_input){
        double[][] X1 = X_input.getArray().clone();//复制
        double[][] X2 = X_input.changeAverageToZero().getArray();//减均值

        Matrix X1_matrix = new Matrix(X1);
        Matrix X2_matrix = new Matrix(X2);
        
        double[] sum = new double[Y_input.getCols()];
        double[] y_mean = new double[Y_input.getCols()];
        for (int j = 0, jSize = Y_input.getCols(); j < jSize; j++) {
        	for (int i = 0, iSize = Y_input.getRows(); i < iSize; i++) {
                sum[j] += Y_input.get(i, j);
            }
            y_mean[j] = sum[j] / Y_input.getRows();
        }
        
        double[] b = new double[Y_input.getCols()]; 
        for (int j = 0; j < b.length; j++) {
        	b[j] = X2_matrix.times(coef_matrix).get(0,j) + y_mean[j] -X1_matrix.times(coef_matrix).get(0,j);
        }
        return b;
    }

    private Matrix pinv2(Matrix input) {
        List<double[][]> uvh = svd_cross_product(input);
        double[][] u = uvh.get(0);
        double[][] vh = uvh.get(1);
        double[] s_tmp = get_singularValues(input);
        Arrays.sort(s_tmp);
        double[] s = new double[s_tmp.length];
        for (int i=0;i<s_tmp.length;i++) s[i]= s_tmp[s_tmp.length-1-i];
        int rank = s.length;
        u = slice_cols(u,0,rank);
        u = two_array_divide(u,s);

        Matrix u_matrix = new Matrix(u);
        Matrix vh_matrix = new Matrix(vh).transpose();
        Matrix result = u_matrix.times(vh_matrix).transpose();

        return result;

    }
    
    private double[][] two_array_divide(double[][] input, double[] divied) {
        double[][] result = new double[input.length][divied.length];
        for (int i = 0;i<input.length;i++){
            for (int j = 0; j < divied.length; j++) {
                result[i][j] = input[i][j]/divied[j];
            }
        }
        return result;
    }


    private double[][] slice_cols(double[][] input,int s,int e){
        double[][] result = new double[input.length][e-s];
        for (int i = 0;i<input.length;i++){
            for (int j = s; j < e; j++) {
                result[i][j-s] = input[i][j];
            }
        }
        return result;
    }

    private void ravel(Matrix x_store, Matrix x_before, int k){
        for (int i=0 ;i < x_store.getRows();i++){
            x_store.set(i, k, x_before.get(i,0));
        }
    }

    private void ravel2(Matrix x_store, Matrix x_before, int k){
    	for (int j = 0; j <= k; j++) {
            for (int i=0 ;i < x_store.getRows();i++){
                x_store.set(i, j, x_before.get(i, j));
            }
    	}
    }

    private double[] get_singularValues(Matrix x){
        SVDecomposition svd = new SVDecomposition(x);
        double[] singularValues = svd.getSingularValues();
        Arrays.sort(singularValues);
        return singularValues;
    }

    private List<double[][]> svd_cross_product(Matrix x){
        SVDecomposition svd = new SVDecomposition(x);//奇异值分解
        Matrix u = svd.getU();
        double[][] U = u.getArray();
        //double[][] U = chooseU(u.getArray(), x.getRows(), x.getCols());


        Matrix v = svd.getV();
        double[][] V = v.getArray();
        List<double[][]> result = new ArrayList<double[][]>(2);
        result.add(U);
        result.add(V);
        return result;
    }
    
    public static void main(String[] args) {
    	double[][] A = new double[5][4];
    	A[0][0] = 1;
    	A[0][1] = 2;
    	A[0][2] = 3;
    	A[0][3] = 4;
    	A[1][0] = 11;
    	A[1][1] = 12;
    	A[1][2] = 13;
    	A[1][3] = 14;
    	A[2][0] = 21;
    	A[2][1] = 22;
    	A[2][2] = 23;
    	A[2][3] = 24;
    	A[3][0] = 31;
    	A[3][1] = 32;
    	A[3][2] = 33;
    	A[3][3] = 34;
    	A[4][0] = 41;
    	A[4][1] = 42;
    	A[4][2] = 43;
    	A[4][3] = 44;
    	double[][] Y1 = new double[5][1];
    	Y1[0][0] = 10;
    	Y1[1][0] = 50;
    	Y1[2][0] = 90;
    	Y1[3][0] = 130;
    	Y1[4][0] = 170;
    	double[][] Y2 = new double[5][2];
    	Y2[0][0] = 1;
    	Y2[1][0] = 0;
    	Y2[2][0] = -4;
    	Y2[3][0] = 3;
    	Y2[4][0] = 9;
    	Y2[0][1] = -2;
    	Y2[1][1] = 2;
    	Y2[2][1] = 5;
    	Y2[3][1] = 7;
    	Y2[4][1] = 3;
    	double[][] Y3 = new double[5][3];
    	Y3[0][0] = 1;
    	Y3[1][0] = 0;
    	Y3[2][0] = -4;
    	Y3[3][0] = 3;
    	Y3[4][0] = 9;
    	Y3[0][1] = -2;
    	Y3[1][1] = 2;
    	Y3[2][1] = 5;
    	Y3[3][1] = 7;
    	Y3[4][1] = 3;
    	Y3[0][2] = 1;
    	Y3[1][2] = -8;
    	Y3[2][2] = 9;
    	Y3[3][2] = 2;
    	Y3[4][2] = 0;
    	Matrix X = new Matrix(A);
    	Matrix Y11 = new Matrix(Y1);
    	Matrix Y22 = new Matrix(Y2);
    	Matrix Y33 = new Matrix(Y3);
    	PLS p = new PLS();
    	Matrix res = p.fit(X, Y11, 2);
    	System.out.println();
    	for (int i = 0; i < res.getRows(); i ++) {
    		System.out.print(res.get(i, 0)+", ");
    	}
    	Matrix res2 = p.predictY(X, res);
    	System.out.println();
    	for (int i = 0; i < res2.getRows(); i ++) {
    		System.out.print(res2.get(i, 0)+", ");
    	}
    	System.out.println("test");
    	res = p.fit(X, Y22, 2);
    	System.out.println();
    	for (int i = 0; i < res.getRows(); i ++) {
    		System.out.print(res.get(i, 0)+", ");
    	}
    	System.out.println();
    	for (int i = 0; i < res.getRows(); i ++) {
    		System.out.print(res.get(i, 1)+", ");
    	}
    	res = p.fit(X, Y33, 2);
    	System.out.println();
    	for (int i = 0; i < res.getRows(); i ++) {
    		System.out.print(res.get(i, 0)+", ");
    	}
    	System.out.println();
    	for (int i = 0; i < res.getRows(); i ++) {
    		System.out.print(res.get(i, 1)+", ");
    	}
    	System.out.println();
    	for (int i = 0; i < res.getRows(); i ++) {
    		System.out.print(res.get(i, 2)+", ");
    	}
    }
}
