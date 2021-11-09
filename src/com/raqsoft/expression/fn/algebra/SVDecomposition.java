package com.raqsoft.expression.fn.algebra;

/**
 * 矩阵的奇异值分解处理
 * @author bd
 */
public class SVDecomposition {
	//对于m*n矩阵，A=UΣV*，U为m*m酉矩阵，V*为V的共轭转置，n*n酉矩阵
	private double[][] U, V;
	//半正定m*n矩阵Σ中的奇异值
	private double[] s;

	// 矩阵的行列数
	private int rows, cols;

	/**
	 * 初始化
	 * @param A		矩阵
	 */
	public SVDecomposition(Matrix matrix) {
		// Initialize.
		double[][] A = matrix.getArrayCopy();
		this.rows = matrix.getRows();
		this.cols = matrix.getCols();
		
		int nu = Math.min(this.rows, this.cols);
		s = new double[Math.min(this.rows + 1, this.cols)];
		U = new double[this.rows][nu];
		V = new double[this.cols][this.cols];
		double[] e = new double[this.cols ];
		double[] work = new double[this.rows];
		boolean wantu = true;
		boolean wantv = true;

		int nct = Math.min(this.rows - 1, this.cols);
		int nrt = Math.max(0, Math.min(this.cols - 2, this.rows));
		for (int k = 0; k < Math.max(nct, nrt); k++) {
			if (k < nct) {
				s[k] = 0;
				for (int r = k; r < this.rows; r++) {
					s[k] = Math.hypot(s[k], A[r][k]);
				}
				if (s[k] != 0.0) {
					if (A[k][k] < 0.0) {
						s[k] = -s[k];
					}
					for (int r = k; r < this.rows; r++) {
						A[r][k] /= s[k];
					}
					A[k][k] += 1.0;
				}
				s[k] = -s[k];
			}
			for (int j = k + 1; j < this.cols; j++) {
				if ((k < nct) & (s[k] != 0.0)) {
					double t = 0;
					for (int r = k; r < this.rows; r++) {
						t += A[r][k] * A[r][j];
					}
					t = -t / A[k][k];
					for (int r = k; r < this.rows; r++) {
						A[r][j] += t * A[r][k];
					}
				}

				e[j] = A[k][j];
			}
			if (wantu & (k < nct)) {
				for (int r = k; r < this.rows; r++) {
					U[r][k] = A[r][k];
				}
			}
			if (k < nrt) {
				e[k] = 0;
				for (int r = k + 1; r < this.cols; r++) {
					e[k] = Math.hypot(e[k], e[r]);
				}
				if (e[k] != 0.0) {
					if (e[k + 1] < 0.0) {
						e[k] = -e[k];
					}
					for (int r = k + 1; r < this.cols; r++) {
						e[r] /= e[k];
					}
					e[k + 1] += 1.0;
				}
				e[k] = -e[k];
				if ((k + 1 < this.rows) & (e[k] != 0.0)) {
					for (int r = k + 1; r < this.rows; r++) {
						work[r] = 0.0;
					}
					for (int j = k + 1; j < this.cols; j++) {
						for (int r = k + 1; r < this.rows; r++) {
							work[r] += e[j] * A[r][j];
						}
					}
					for (int j = k + 1; j < this.cols; j++) {
						double t = -e[j] / e[k + 1];
						for (int r = k + 1; r < this.rows; r++) {
							A[r][j] += t * work[r];
						}
					}
				}
				if (wantv) {
					for (int r = k + 1; r < this.cols; r++) {
						V[r][k] = e[r];
					}
				}
			}
		}
		
		int p = Math.min(this.cols, this.rows + 1);
		if (nct < this.cols) {
			s[nct] = A[nct][nct];
		}
		if (this.rows < p) {
			s[p - 1] = 0.0;
		}
		if (nrt + 1 < p) {
			e[nrt] = A[nrt][p - 1];
		}
		e[p - 1] = 0.0;
		if (wantu) {
			for (int j = nct; j < nu; j++) {
				for (int r = 0; r < this.rows; r++) {
					U[r][j] = 0.0;
				}
				U[j][j] = 1.0;
			}
			for (int k = nct - 1; k >= 0; k--) {
				if (s[k] != 0.0) {
					for (int j = k + 1; j < nu; j++) {
						double t = 0;
						for (int r = k; r < this.rows; r++) {
							t += U[r][k] * U[r][j];
						}
						t = -t / U[k][k];
						for (int r = k; r < this.rows; r++) {
							U[r][j] += t * U[r][k];
						}
					}
					for (int r = k; r < this.rows; r++) {
						U[r][k] = -U[r][k];
					}
					U[k][k] = 1.0 + U[k][k];
					for (int r = 0; r < k - 1; r++) {
						U[r][k] = 0.0;
					}
				} else {
					for (int r = 0; r < this.rows; r++) {
						U[r][k] = 0.0;
					}
					U[k][k] = 1.0;
				}
			}
		}
		if (wantv) {
			for (int k = this.cols - 1; k >= 0; k--) {
				if ((k < nrt) & (e[k] != 0.0)) {
					for (int j = k + 1; j < nu; j++) {
						double t = 0;
						for (int r = k + 1; r < this.cols; r++) {
							t += V[r][k] * V[r][j];
						}
						t = -t / V[k + 1][k];
						for (int r = k + 1; r < this.cols; r++) {
							V[r][j] += t * V[r][k];
						}
					}
				}
				for (int r = 0; r < this.cols; r++) {
					V[r][k] = 0.0;
				}
				V[k][k] = 1.0;
			}
		}
		int pp = p - 1;
		int iter = 0;
		double eps = Math.pow(2.0, -52.0);
		double tiny = Math.pow(2.0, -966.0);
		while (p > 0) {
			int k, kase;
			for (k = p - 2; k >= -1; k--) {
				if (k == -1) {
					break;
				}
				if (Math.abs(e[k]) <= tiny + eps * (Math.abs(s[k]) + Math.abs(s[k + 1]))) {
					e[k] = 0.0;
					break;
				}
			}
			if (k == p - 2) {
				kase = 4;
			} else {
				int ks;
				for (ks = p - 1; ks >= k; ks--) {
					if (ks == k) {
						break;
					}
					double t = (ks != p ? Math.abs(e[ks]) : 0.) + (ks != k + 1 ? Math.abs(e[ks - 1]) : 0.);
					if (Math.abs(s[ks]) <= tiny + eps * t) {
						s[ks] = 0.0;
						break;
					}
				}
				if (ks == k) {
					kase = 3;
				} else if (ks == p - 1) {
					kase = 1;
				} else {
					kase = 2;
					k = ks;
				}
			}
			k++;

			switch (kase) {

			case 1: {
				double f = e[p - 2];
				e[p - 2] = 0.0;
				for (int j = p - 2; j >= k; j--) {
					double t = Math.hypot(s[j], f);
					double cs = s[j] / t;
					double sn = f / t;
					s[j] = t;
					if (j != k) {
						f = -sn * e[j - 1];
						e[j - 1] = cs * e[j - 1];
					}
					if (wantv) {
						for (int r = 0; r < this.cols; r++) {
							t = cs * V[r][j] + sn * V[r][p - 1];
							V[r][p - 1] = -sn * V[r][j] + cs * V[r][p - 1];
							V[r][j] = t;
						}
					}
				}
			}
			break;

			case 2: {
				double f = e[k - 1];
				e[k - 1] = 0.0;
				for (int j = k; j < p; j++) {
					double t = Math.hypot(s[j], f);
					double cs = s[j] / t;
					double sn = f / t;
					s[j] = t;
					f = -sn * e[j];
					e[j] = cs * e[j];
					if (wantu) {
						for (int r = 0; r < this.rows; r++) {
							t = cs * U[r][j] + sn * U[r][k - 1];
							U[r][k - 1] = -sn * U[r][j] + cs * U[r][k - 1];
							U[r][j] = t;
						}
					}
				}
			}
			break;

			case 3: {
				double scale = Math
						.max(Math.max(Math.max(Math.max(Math.abs(s[p - 1]), Math.abs(s[p - 2])), Math.abs(e[p - 2])),
								Math.abs(s[k])), Math.abs(e[k]));
				double sp = s[p - 1] / scale;
				double spm1 = s[p - 2] / scale;
				double epm1 = e[p - 2] / scale;
				double sk = s[k] / scale;
				double ek = e[k] / scale;
				double b = ((spm1 + sp) * (spm1 - sp) + epm1 * epm1) / 2.0;
				double c = (sp * epm1) * (sp * epm1);
				double shift = 0.0;
				if ((b != 0.0) | (c != 0.0)) {
					shift = Math.sqrt(b * b + c);
					if (b < 0.0) {
						shift = -shift;
					}
					shift = c / (b + shift);
				}
				double f = (sk + sp) * (sk - sp) + shift;
				double g = sk * ek;
				for (int j = k; j < p - 1; j++) {
					double t = Math.hypot(f, g);
					double cs = f / t;
					double sn = g / t;
					if (j != k) {
						e[j - 1] = t;
					}
					f = cs * s[j] + sn * e[j];
					e[j] = cs * e[j] - sn * s[j];
					g = sn * s[j + 1];
					s[j + 1] = cs * s[j + 1];
					if (wantv) {
						for (int r = 0; r < this.cols; r++) {
							t = cs * V[r][j] + sn * V[r][j + 1];
							V[r][j + 1] = -sn * V[r][j] + cs * V[r][j + 1];
							V[r][j] = t;
						}
					}
					t = Math.hypot(f, g);
					cs = f / t;
					sn = g / t;
					s[j] = t;
					f = cs * e[j] + sn * s[j + 1];
					s[j + 1] = -sn * e[j] + cs * s[j + 1];
					g = sn * e[j + 1];
					e[j + 1] = cs * e[j + 1];
					if (wantu && (j < this.rows - 1)) {
						for (int r = 0; r < this.rows; r++) {
							t = cs * U[r][j] + sn * U[r][j + 1];
							U[r][j + 1] = -sn * U[r][j] + cs * U[r][j + 1];
							U[r][j] = t;
						}
					}
				}
				e[p - 2] = f;
				iter = iter + 1;
			}
			break;
			case 4: {
				if (s[k] <= 0.0) {
					s[k] = (s[k] < 0.0 ? -s[k] : 0.0);
					if (wantv) {
						for (int r = 0; r <= pp; r++) {
							V[r][k] = -V[r][k];
						}
					}
				}
				while (k < pp) {
					if (s[k] >= s[k + 1]) {
						break;
					}
					double t = s[k];
					s[k] = s[k + 1];
					s[k + 1] = t;
					if (wantv && (k < this.cols - 1)) {
						for (int r = 0; r < this.cols; r++) {
							t = V[r][k + 1];
							V[r][k + 1] = V[r][k];
							V[r][k] = t;
						}
					}
					if (wantu && (k < this.rows - 1)) {
						for (int r = 0; r < this.rows; r++) {
							t = U[r][k + 1];
							U[r][k + 1] = U[r][k];
							U[r][k] = t;
						}
					}
					k++;
				}
				iter = 0;
				p--;
			}
				break;
			}
		}
	}
	
	/**
	 * Return the left singular vectors
	 * @return U
	 */
	public Matrix getU() {
		return new Matrix(U, this.rows, Math.min(this.rows + 1, this.cols));
	}

	/**
	 * Return the right singular vectors
	 * @return V
	 */
	public Matrix getV() {
		return new Matrix(V, this.cols, this.cols);
	}

	/**
	 * Return the one-dimensional array of singular values
	 * @return diagonal of S.
	 */
	public double[] getSingularValues() {
		return s;
	}

	/**
	 * 返回奇异值对角矩阵
	 * @return S
	 */
	public Matrix getS() {
		Matrix X = new Matrix(this.cols, this.cols);
		double[][] S = X.getArray();
		for (int r = 0; r < this.cols; r++) {
			for (int j = 0; j < this.cols; j++) {
				S[r][j] = 0.0;
			}
			S[r][r] = this.s[r];
		}
		return X;
	}

	/**
	 * Two norm
	 * @return max(S)
	 */
	public double norm2() {
		return s[0];
	}

	/**
	 * Two norm condition number
	 * @return max(S)/min(S)
	 */
	public double cond() {
		return s[0] / s[Math.min(this.rows, this.cols) - 1];
	}

	/**
	 * Effective numerical matrix rank
	 * @return Number of nonnegligible singular values.
	 */
	public int rank() {
		double eps = Math.pow(2.0, -52.0);
		double tol = Math.max(this.rows, this.cols) * s[0] * eps;
		int rank = 0;
		for (int r = 0; r < s.length; r++) {
			if (s[r] > tol) {
				rank++;
			}
		}
		return rank;
	}
}
