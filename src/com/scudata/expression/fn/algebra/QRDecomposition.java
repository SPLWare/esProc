package com.scudata.expression.fn.algebra;

/**
 * 矩阵的正交三角分解处理
 * @author bd
 */
public class QRDecomposition {
	// QR分解矩阵，rows*cols的矩阵可以分解为正交矩阵Q和上三角矩阵R相乘，rows>=cols
	// 		其中Q为cols*cols的正交矩阵，即Q^T*Q=Q*Q^T=I
	//		R为rows*cols的上三角矩阵，右上有值，左下及下方都是0
	private double[][] QR;
	private int rows, cols;
	//
	private double[] Rdiag;
	
	protected QRDecomposition(Matrix A) {
		// QR分解初始化.
		this.QR = A.getArrayCopy();
		this.rows = A.getRows();
		this.cols = A.getCols();
		this.Rdiag = new double[this.cols];

		// 循环各列
		for (int k = 0; k < this.cols; k++) {
			double nrm = 0d;
			// 计算本列
			for (int r = k; r < this.rows; r++) {
				nrm = Math.hypot(nrm, QR[r][k]);
			}
			if (nrm != 0d) {
				// Form k-th Householder vector.
				if (QR[k][k] < 0) {
					nrm = -nrm;
				}
				for (int r = k; r < this.rows; r++) {
					QR[r][k] /= nrm;
				}
				QR[k][k] += 1.0;

				// Apply transformation to remaining columns.
				for (int c = k + 1; c < this.cols; c++) {
					double s = 0d;
					for (int r = k; r < this.rows; r++) {
						s += QR[r][k] * QR[r][c];
					}
					s = -s / QR[k][k];
					for (int r = k; r < this.rows; r++) {
						QR[r][c] += s * QR[r][k];
					}
				}
			}
			Rdiag[k] = -nrm;
		}
	}
	
	/**
	 * 使用QR分解判断矩阵是否满秩
	 * @return	是否满秩
	 */
	protected boolean isFullRank() {
		for (int c = 0; c < this.cols; c++) {
			if (Rdiag[c] == 0)
				return false;
		}
		return true;
	}

	/**
	 * 最小二乘法解 A*X = B
	 * @param B		矩阵参数B
	 * @return		X that minimizes the two norm of Q*R*X-B.
	 * @exception	异常
	 */
	protected Matrix solve(Matrix B) {
		if (B.getRows() != this.rows) {
			throw new IllegalArgumentException("Matrix row dimensions must agree.");
		}
		if (!this.isFullRank()) {
			throw new RuntimeException("Matrix is rank deficient.");
		}

		int nx = B.getCols();
		double[][] X = B.getArrayCopy();
		for (int k = 0; k < this.cols; k++) {
			for (int c = 0; c < nx; c++) {
				double s = 0d;
				for (int r = k; r < this.rows; r++) {
					s += QR[r][k] * X[r][c];
				}
				s = -s / QR[k][k];
				for (int r = k; r < this.rows; r++) {
					X[r][c] += s * QR[r][k];
				}
			}
		}
		for (int k = this.cols - 1; k >= 0; k--) {
			for (int c = 0; c < nx; c++) {
				X[k][c] /= Rdiag[k];
			}
			for (int r = 0; r < k; r++) {
				for (int c = 0; c < nx; c++) {
					X[r][c] -= X[k][c] * QR[r][k];
				}
			}
		}
		return (new Matrix(X, this.cols, nx).getMatrix(0, this.cols - 1, 0, nx - 1));
	}
}
