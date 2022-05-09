package com.scudata.expression.fn.algebra;

import com.scudata.common.RQException;

/**
 * 矩阵的三角分解处理
 * @author bd
 */
public class LUDecomposition {
	// 紧凑三角矩阵
	private double[][] LU;
	// 置换向量
	private int[] piv;
	
	private int rows, cols, pivsign; 

	/**
	 * 将矩阵执行三角分解法
	 */
	protected LUDecomposition(Matrix A) {
		this.LU = A.getArrayCopy();
		this.rows = A.getRows();
		this.cols = A.getCols();
		this.piv = new int[rows];
		for (int r = 0; r < rows; r++) {
			piv[r] = r;
		}
		this.pivsign = 1;
		double[] LUrow;
		double[] LUcol = new double[rows];

		// 外层循环，从首列开始计算LU矩阵
		for (int c = 0; c < cols; c++) {
			// 记录本列的原值
			for (int r = 0; r < rows; r++) {
				LUcol[r] = LU[r][c];
			}
			// 准备求解当前位置的值，
			// 左下，r>c, a(r,c)=l(r,1)*u(1,c)+l(r,2)*u(2,c)...+l(r,c)*u(c,c)
			// 其它，r<c, a(r,c)=l(r,1)*u(1,c)+...+l(r,r-1)*u(r-1,c)...+u(r,c)
			for (int r = 0; r < rows; r++) {
				LUrow = LU[r];
				// 前面部分的参数，涉及之前列和之前行，LU矩阵中的对应值应该已经计算过了
				int kmax = Math.min(r, c);
				double s = 0d;
				for (int k = 0; k < kmax; k++) {
					s += LUrow[k] * LUcol[k];
				}
				// 把当前位置的前置参数都减去
				// 剩下的，左下=l(r,c)*u(c,c)，其它部分就是u(r,c)了
				LUrow[c] = LUcol[r] -= s;
			}
			// 查找是否需要pivot转置矩阵
			int p = c;
			for (int r = c + 1; r < rows; r++) {
				if (Math.abs(LUcol[r]) > Math.abs(LUcol[p])) {
					p = r;
				}
			}
			if (p != c) {
				for (int k = 0; k < cols; k++) {
					double t = LU[p][k];
					LU[p][k] = LU[c][k];
					LU[c][k] = t;
				}
				int k = piv[p];
				piv[p] = piv[c];
				piv[c] = k;
				pivsign = -pivsign;
			}
			// 处理左下三角计算，此时本位置计算已经完成了，只需修改LU矩阵就行，LURow和LUCol不用管了
			if (c < rows & LU[c][c] != 0d) {
				for (int r = c + 1; r < rows; r++) {
					LU[r][c] /= LU[c][c];
				}
			}
		}
	}

	/**
	 * 用矩阵的三角分解法求解A*X = B
	 * @param B		矩阵参数B
	 * @return
	 */
	protected Matrix solve(Matrix B) {
		if (B.getRows() != this.rows) {
			throw new RQException("Matrix row dimensions must agree.");
		}
		if (!isNonsingular()) {
			//奇异矩阵，方程有无穷多解或无解
			throw new RQException("Matrix is singular.");
		}

		//执行三角分解后，方程变为L*U*X = B，即L*(U*X) = B
		//L*Y=B, 先计算B即U*X
	    int nx = B.getCols();
		Matrix Xmat = B.getMatrix(piv, 0, nx - 1);
		double[][] X = Xmat.getArray();

		// 求解 L*Y = B(piv,:)
		for (int k = 0; k < this.cols; k++) {
			for (int i = k + 1; i < this.cols; i++) {
				for (int j = 0; j < nx; j++) {
					X[i][j] -= X[k][j] * LU[i][k];
				}
			}
		}
		// 求解 U*X = Y;
		for (int k = this.cols - 1; k >= 0; k--) {
			for (int j = 0; j < nx; j++) {
				X[k][j] /= LU[k][k];
			}
			for (int i = 0; i < k; i++) {
				for (int j = 0; j < nx; j++) {
					X[i][j] -= X[k][j] * LU[i][k];
				}
			}
		}
		return Xmat;
	}

	/**
	 * 求行列式，非方阵抛异常
	 * @return		A的行列式det(A)
	 * @exception	异常
	 */
	protected double det() {
		if (this.rows != this.cols) {
			throw new IllegalArgumentException("Matrix must be square.");
		}
		double d = (double) pivsign;
		for (int j = 0; j < this.cols; j++) {
			d *= LU[j][j];
		}
		double scale = 1000000;
		d = d * scale;
		if (d > Long.MIN_VALUE && d < Long.MAX_VALUE) {
			d = Math.round(d)/scale;
		} else {
			d = d / scale;
		}
		return d;
	}
	
	/**
	 * 根据紧凑三角分解矩阵判断是否非奇异矩阵，如果其中无0，则一定是非奇异矩阵
	 * @param LU	紧凑三角分解矩阵
	 * @return		是否非奇异矩阵，奇异矩阵返回false
	 */
	private boolean isNonsingular() {
		for (int c = 0; c < this.cols; c++) {
			//if (this.LU[c][c] == 0)
			if (Matrix.ifZero(this.LU[c][c]))
				return false;
		}
		return true;
	}
}
