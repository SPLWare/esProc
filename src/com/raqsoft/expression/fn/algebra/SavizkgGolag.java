package com.raqsoft.expression.fn.algebra;

import com.raqsoft.common.Logger;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.resources.EngineMessage;

public class SavizkgGolag extends Function {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("sg" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("sg" + mm.getMessage("function.invalidParam"));
		} else {
			if (param.getSubSize() < 3) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("sg" + mm.getMessage("function.invalidParam"));
			}

			IParam sub1 = param.getSub(0);
			IParam sub2 = param.getSub(1);
			IParam sub3 = param.getSub(2);
			if (sub2 == null || sub3 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("sg" + mm.getMessage("function.invalidParam"));
			}
			
			Object o1 = sub1 == null ? null : sub1.getLeafExpression().calculate(ctx);
			Object o2 = sub2.getLeafExpression().calculate(ctx);
			Object o3 = sub3.getLeafExpression().calculate(ctx);
			int p = 0;
			if (param.getSubSize()>=4) {
				IParam sub4 = param.getSub(3);
				Object o4 = sub4.getLeafExpression().calculate(ctx);
				if (o4 instanceof Number) {
					p = ((Number) o4).intValue();
				}
			}
			if ((o1 == null || o1 instanceof Sequence) && o2 instanceof Number && o3 instanceof Number) {
				int k = ((Number) o2).intValue();
				int n = ((Number) o3).intValue();
				if (n < 1) {
					n = 3;
					Logger.info("n is not less than 3, set it to "+n);
				}
				if (k < 1) {
					k = 1;
				}
				Matrix B = filterCoeff(n, k, p);
				if (o1 == null) {
					// 返回平滑系数矩阵
					return B.toSequence(option, true);
				}
				n = B.getRows();
				double[][] bvs = B.getArray();
				int step = (n-1)/2; 
				Matrix A = new Matrix((Sequence) o1);
				int cols = A.getCols();
				if (cols == 1) {
					A = A.transpose();
					cols = A.getCols();
				}
				// 如果只有一列，视为向量，转为单行
				double[][] vs = A.getArray();
				int rows = vs.length;
				if (cols < n) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("sg" + mm.getMessage("function.invalidParam") + " m is too large than data.");
				}
				double[] window = new double[n];
				for(int row =0; row < rows; row++){
					int col = 0;
			        //Smooth start data, size is step
					double[] filter = new double[cols];
		        	System.arraycopy(vs[row], 0, window, 0, n);
			        for(; col < step; col++)
			        {
			            filter[col] = mul(bvs[col], window);
			        }
			        //Smooth middle data
			        for(; col < cols - step; col++){
			        	System.arraycopy(vs[row], col-step, window, 0, n);
			            filter[col] = mul(bvs[step], window);
			        }
			        //Smooth end data, size is step
		        	System.arraycopy(vs[row], cols-n, window, 0, n);
			        for(; col < cols; col++)
			        {
			            filter[col] = mul(bvs[n+col-cols], window);
			        }
			        vs[row] = filter;
			    }
				return A.toSequence(option, true);
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("sg" + mm.getMessage("function.paramTypeError"));
			}
		}
	}
	
	public static double mul(double[] row, double[] col) {
		double s = 0;
		int len = row.length;
		if (len != col.length) {
			throw new RQException("Sizes are unmatched, can't calculate multiple。");
		}
		for (int i = 0; i < len; i++ ) {
			s += row[i]*col[i];
		}
		return s;
	}
	
	public Matrix createMatrix(int n,int k) {
		int step = (n - 1) / 2;
		Matrix matrix = new Matrix(n, k );
		double[][] vs = matrix.getArray();
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < k ; j++) {
				vs[i][j] = Math.pow(i - step, j);
			}
		}
		return matrix;
	}
	
	/**
	 * n个取样点k次多项式p阶导数
	 * @param k
	 * @param n
	 * @return
	 */
	public Matrix filterCoeff(int n,int k, int p) {
		//n是奇数
		if (n%2 == 0) {
			n = n+1;
			Logger.info("m is an odd number, set it to "+n);
		}
		//k不大于n, 此时改为可操作的n
		if (k > n) {
			k = n - 1;
			Logger.info("n is less than m, set it to "+k);
		}
		//p不大于k, 此时改为k
		if (p > k) {
			p = k;
			Logger.info("n can't be greater than m, set it to "+k);
		}
		Matrix matrix = createMatrix(n,k+1);
		Matrix mt = matrix.transpose();
		Matrix first = matrix;
		if (p > 0) {
			first = getDerivativeMatrix(n, k+1, p);
		}
		Matrix A = first.times(mt.times(matrix).inverse()).times(mt);
		return A;
	}
	
	/**
	 * 获取n点k次p阶求导矩阵
	 * @return
	 */
	public Matrix getDerivativeMatrix(int n,int k, int p) {
		int step = (n - 1) / 2;
		Matrix matrix = new Matrix(n, k);
		double[][] vs = matrix.getArray();
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < k ; j++) {
				if (j < p) {
					vs[i][j] = 0;
				}
				else {
					double v = Math.pow(i - step, j - p);
					for (int l = 0; l < p; l++) {
						v *= j - l;
					}
					vs[i][j] = v;
				}
			}
		}
		return matrix;
	}

}
