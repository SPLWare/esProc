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

public class Lasso extends Function {

	/**
	 * 回归函数lasso(X,Y,r,p, alpha)和lasso(X,F), 返回的主成分系数矩阵无法降维，但可以用来计算主成分得分
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
			if (param.getSubSize() >= 4) {
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
				// added by bd, 2022.11.8, 添加alpha参数
				double alpha = 0.1;
				if (param.getSubSize() > 4) {
					IParam sub5 = param.getSub(4);
					if (sub5 != null) {
						Object o5 = sub5.getLeafExpression().calculate(ctx);
						if (o5 instanceof Number) {
							alpha = ((Number) o5).doubleValue();
						}
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
						throw new RQException("In the lasso(A, Y, r, p, alpha) function, A and Y must have the same number of rows");
					}
					String v = ifv? "v" : null;
					Sequence coef = null;
					if (option == null || option.contains("1")) {
						coef = fit0(A, T, r, p, alpha);
					}
					else if (option.contains("2")) {
						coef = fit2(A, T, r, p, alpha, 0.001);
					}
					else if (option.contains("3")) {
						coef = fit3(A, T, p, alpha, 0.001);
					}
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
	
    protected static Sequence fit0(Matrix X, Matrix Y, double learning_rate, double epochs, double alpha) {
        double b = 0;
        double[][] w = new double[X.getCols()][1];
        Matrix wMatrix = new Matrix(w);
        Matrix dw;

        for (int iter = 0; iter < epochs; iter++){
            w = wMatrix.getArray();
            //double alpha = 0.1;
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
    
    /**
     * edited by bd, 2022.11.11, 重新修改fit的方法，由于与原流程差异比较大，因此另起个名字
     * @param X
     * @param Y
     * @param learning_rate
     * @param epochs
     * @param alpha
     * @return
     */
    protected static Sequence fit2(Matrix X, Matrix Y, double learning_rate, int epochs, double alpha, double threshould) {
        double b = 0;
        int m = X.getRows();
        int cols = X.getCols();
        double[][] data = X.getArray();
        double[][] data1 = new double[m][cols+1];
        for (int i = 0; i < m; i++) {
        	data[i][0] = 1;
        	for (int j = 0; j < cols; j++) {
        		data1[i][j+1] = data[i][j];
        	}
        }
        Matrix A = new Matrix(data1, m, cols+1);
        
        double[][] w = new double[cols+1][1];
        for (int i = 0; i <= cols; i++) {
        	w[i][0] = 1;
        }
        Matrix wMat = new Matrix(w, cols + 1, 1);
        Matrix dw;

        for (int iter = 1; iter <= epochs; iter++){
        	double[][] ow = wMat.getArrayCopy();
        	Matrix owMat = new Matrix(ow, cols+1, 1);
        	boolean success = true;
        	for (int ci = 0; ci <= cols; ci++) {
        		for (int iter2 = 1; iter2 <= epochs; iter2++){
        			Matrix h = A.times(wMat);
        			Matrix h2 = h.minus(Y).divide(m);
        			double gradient = times(A, ci, h2) + sign(w[ci][0], alpha);
        			w[ci][0] = w[ci][0] - gradient * learning_rate;
        			if (Math.abs(gradient) < threshould) {
        				break;
        			}
        			if (iter2 >= epochs) {
        				success = false;
        			}
        		}
        	}
    		if (success) {
        		return (Sequence) wMat.toSequence(null, true);
    		}
        }
        return (Sequence) wMat.toSequence(null, true);
    }
    
    /**
     * 由github.com/haifengl/smile修改的lasso算法
     * Fits the LASSO model.
     * @param x the design matrix.
     * @param y the responsible variable Matrix.
     * @param lambda the shrinkage/regularization parameter.
     * @param tol the tolerance for stopping iterations (relative target duality gap).
     * @param maxIter the maximum number of IPM (Newton) iterations.
     * @return the model.
     */
    private static Sequence fit3(Matrix x, Matrix y, int maxIter, double lambda, double tol) {
        if (lambda < 0.0) {
            throw new IllegalArgumentException("Invalid shrinkage/regularization parameter lambda = " + lambda);
        }

        if (tol <= 0.0) {
            throw new IllegalArgumentException("Invalid tolerance: " + tol);
        }

        if (maxIter <= 0) {
            throw new IllegalArgumentException("Invalid maximum number of iterations: " + maxIter);
        }

        // INITIALIZE
        // IPM PARAMETERS
        final int MU = 2;             // updating parameter of t

        // LINE SEARCH PARAMETERS
        final double ALPHA = 0.01;    // minimum fraction of decrease in the objective
        final double BETA = 0.5;      // stepsize decrease factor
        final int MAX_LS_ITER = 100;  // maximum backtracking line search iteration
        final int pcgmaxi = 5000; // maximum number of maximum PCG iterations
        final double eta = 1E-3;  // tolerance for PCG termination

        int pitr = 0;
        ///int n = x.nrow();
        int n = x.getRows();
        ///int p = x.ncol();
        int p = x.getCols();

        double[] Y = new double[n];
        ///double ym = MathEx.mean(y);
        double ym = mean(y.getArray());
        for (int i = 0; i < n; i++) {
            Y[i] = y.get(i, 0) - ym;
        }

        double t = Math.min(Math.max(1.0, 1.0 / lambda), 2 * p / 1e-3);
        double dobj = Double.NEGATIVE_INFINITY; // dual objective function value
        double s = Double.POSITIVE_INFINITY;

        double[] w = new double[p];
        double[] u = new double[p];
        double[] z = new double[n];
        double[][] f = new double[2][p];
        ///Arrays.fill(u, 1.0);
        fill(u, 1.0);
        for (int i = 0; i < p; i++) {
            f[0][i] = w[i] - u[i];
            f[1][i] = -w[i] - u[i];
        }

        double[] neww = new double[p];
        double[] newu = new double[p];
        double[] newz = new double[n];
        double[][] newf = new double[2][p];

        double[] dx = new double[p];
        double[] du = new double[p];
        double[] dxu = new double[2 * p];
        double[] grad = new double[2 * p];

        // diagxtx = diag(X'X)
        // X has been standardized so that diag(X'X) is just 1.0.
        // Here we initialize it to 2.0 because we actually need 2 * diag(X'X)
        // during optimization.
        double[] diagxtx = new double[p];
        ///Arrays.fill(diagxtx, 2.0);
        fill(diagxtx, 2.0);

        double[] nu = new double[n];
        double[] xnu = new double[p];

        double[] q1 = new double[p];
        double[] q2 = new double[p];
        double[] d1 = new double[p];
        double[] d2 = new double[p];

        double[][] gradphi = new double[2][p];
        double[] prb = new double[p];
        double[] prs = new double[p];

        PCG pcg = new PCG(x, d1, d2, prb, prs);

        // MAIN LOOP
        int ntiter = 0;
        for (; ntiter <= maxIter; ntiter++) {
            ///x.mv(w, z);
        	mv(x, w, z);
            for (int i = 0; i < n; i++) {
                z[i] -= Y[i];
                nu[i] = 2 * z[i];
            }

            // CALCULATE DUALITY GAP
            ///x.tv(nu, xnu);
            tv(x, nu, xnu);
            ///double maxXnu = MathEx.normInf(xnu);
            double maxXnu = normInf(xnu);
            if (maxXnu > lambda) {
                double lnu = lambda / maxXnu;
                for (int i = 0; i < n; i++) {
                    nu[i] *= lnu;
                }
            }

            // primal objective function value
            double pobj = dot(z, z) + lambda * norm1(w);
            dobj = Math.max(-0.25 * dot(nu, nu) - dot(nu, Y), dobj);
            if (ntiter % 10 == 0) {
                Logger.info(String.format("LASSO: primal and dual objective function value after %3d iterations: %.5g\t%.5g%n", ntiter, pobj, dobj));
            }

            double gap = pobj - dobj;
            // STOPPING CRITERION
            if (gap / dobj < tol) {
                Logger.info(String.format("LASSO: primal and dual objective function value after %3d iterations: %.5g\t%.5g%n", ntiter, pobj, dobj));
                break;
            }

            // UPDATE t
            if (s >= 0.5) {
                t = Math.max(Math.min(2 * p * MU / gap, MU * t), t);
            }

            // CALCULATE NEWTON STEP    
            for (int i = 0; i < p; i++) {
                double q1i = 1.0 / (u[i] + w[i]);
                double q2i = 1.0 / (u[i] - w[i]);
                q1[i] = q1i;
                q2[i] = q2i;
                d1[i] = (q1i * q1i + q2i * q2i) / t;
                d2[i] = (q1i * q1i - q2i * q2i) / t;
            }

            // calculate gradient
            ///x.tv(z, gradphi[0]);
            tv(x, z, gradphi[0]);
            for (int i = 0; i < p; i++) {
                gradphi[0][i] = 2 * gradphi[0][i] - (q1[i] - q2[i]) / t;
                gradphi[1][i] = lambda - (q1[i] + q2[i]) / t;
                grad[i] = -gradphi[0][i];
                grad[i + p] = -gradphi[1][i];
            }

            // calculate vectors to be used in the preconditioner
            for (int i = 0; i < p; i++) {
                prb[i] = diagxtx[i] + d1[i];
                prs[i] = prb[i] * d1[i] - d2[i] * d2[i];
            }

            // set pcg tolerance (relative)
            double normg = norm2(grad);
            double pcgtol = Math.min(0.1, eta * gap / Math.min(1.0, normg));
            if (ntiter != 0 && pitr == 0) {
                pcgtol = pcgtol * 0.1;
            }

            // preconditioned conjugate gradient
            double error = pcg.solve(grad, dxu, pcg, pcgtol, 1, pcgmaxi);
            if (error > pcgtol) {
                pitr = pcgmaxi;
            }

            for (int i = 0; i < p; i++) {
                dx[i] = dxu[i];
                du[i] = dxu[i + p];
            }

            // BACKTRACKING LINE SEARCH
            double phi = dot(z, z) + lambda * sum(u) - sumlogneg(f) / t;
            s = 1.0;
            double gdx = dot(grad, dxu);

            int lsiter = 0;
            for (; lsiter < MAX_LS_ITER; lsiter++) {
                for (int i = 0; i < p; i++) {
                    neww[i] = w[i] + s * dx[i];
                    newu[i] = u[i] + s * du[i];
                    newf[0][i] = neww[i] - newu[i];
                    newf[1][i] = -neww[i] - newu[i];
                }

                if (max(newf) < 0.0) {
                    ///x.mv(neww, newz);
                	mv(x, neww, newz);
                    for (int i = 0; i < n; i++) {
                        newz[i] -= Y[i];
                    }

                    double newphi = dot(newz, newz) + lambda * sum(newu) - sumlogneg(newf) / t;
                    if (newphi - phi <= ALPHA * s * gdx) {
                        break;
                    }
                }
                s = BETA * s;
            }

            if (lsiter == MAX_LS_ITER) {
                Logger.error("LASSO: Too many iterations of line search.");
                break;
            }

            System.arraycopy(neww, 0, w, 0, p);
            System.arraycopy(newu, 0, u, 0, p);
            System.arraycopy(newf[0], 0, f[0], 0, p);
            System.arraycopy(newf[1], 0, f[1], 0, p);
        }

        if (ntiter == maxIter) {
            Logger.error("LASSO: Too many iterations.");
        }

        return toSeq(w);
    }
    
    private static Sequence toSeq(double[] x) {
    	Sequence seq = new Sequence(x.length);
    	for (double v : x) {
    		seq.add(v);
    	}
    	return seq;
    }
    
    static class PCG {
        /** The design matrix. */
        Matrix A;
        /** A' * A */
        Matrix AtA;
        /** The number of columns of A. */
        int p;
        /** The right bottom of Hessian matrix. */
        double[] d1;
        /** The last row/column of Hessian matrix. */
        double[] d2;
        /** The vector used in preconditioner. */
        double[] prb;
        /** The vector used in preconditioner. */
        double[] prs;
        /** A * x */
        double[] ax;
        /** A' * A * x. */
        double[] atax;
        
        public static double EPSILON = Math.pow(2.0, -52.0);

        /**
         * Constructor.
         */
        PCG(Matrix A, double[] d1, double[] d2, double[] prb, double[] prs) {
            this.A = A;
            this.d1 = d1;
            this.d2 = d2;
            this.prb = prb;
            this.prs = prs;

            int n = A.getRows();
            p = A.getCols();
            ax = new double[n];
            atax = new double[p];

            if (p < 10000) {
                AtA = A.transpose().times(A);
            }
        }

        protected int nrow() {
            return 2 * p;
        }

        protected int ncol() {
            return 2 * p;
        }

        protected int size() {
            return A.getRows();
        }

        protected void mv(double[] x, double[] y) {
            // COMPUTE AX (PCG)
            // 
            // y = hessphi * x,
            // 
            // where hessphi = [A'*A*2+D1 , D2;
            //                  D2        , D1];
            if (AtA != null) {
                Lasso.mv(AtA, x, atax);
            } else {
            	Lasso.mv(A, x, ax);
            	Lasso.tv(A, ax, atax);
            }

            for (int i = 0; i < p; i++) {
                y[i]     = 2 * atax[i] + d1[i] * x[i] + d2[i] * x[i + p];
                y[i + p] =               d2[i] * x[i] + d1[i] * x[i + p];
            }
        }

        protected void asolve(double[] b, double[] x) {
            // COMPUTE P^{-1}X (PCG)
            // y = P^{-1} * x
            for (int i = 0; i < p; i++) {
                x[i]   = ( d1[i] * b[i] -  d2[i] * b[i+p]) / prs[i];
                x[i+p] = (-d2[i] * b[i] + prb[i] * b[i+p]) / prs[i];
            }
        }
        
        public double solve(double[] b, double[] x, PCG P, double tol, int itol, int maxIter) {
            double err = 0.0;
            double ak, akden, bk, bkden = 1.0, bknum, bnrm, dxnrm, xnrm, zm1nrm, znrm = 0.0;
            int j, n = b.length;

            double[] p = new double[n];
            double[] pp = new double[n];
            double[] r = new double[n];
            double[] rr = new double[n];
            double[] z = new double[n];
            double[] zz = new double[n];

            mv(x, r);
            for (j = 0; j < n; j++) {
                r[j] = b[j] - r[j];
                rr[j] = r[j];
            }

            if (itol == 1) {
                bnrm = norm(b, itol);
                P.asolve(r, z);
            } else if (itol == 2) {
                P.asolve(b, z);
                bnrm = norm(z, itol);
                P.asolve(r, z);
            } else if (itol == 3 || itol == 4) {
                P.asolve(b, z);
                bnrm = norm(z, itol);
                P.asolve(r, z);
                znrm = norm(z, itol);
            } else {
                throw new IllegalArgumentException(String.format("Illegal itol: %d", itol));
            }

            for (int iter = 1; iter <= maxIter; iter++) {
                P.asolve(rr, zz);
                for (bknum = 0.0, j = 0; j < n; j++) {
                    bknum += z[j] * rr[j];
                }
                if (iter == 1) {
                    for (j = 0; j < n; j++) {
                        p[j] = z[j];
                        pp[j] = zz[j];
                    }
                } else {
                    bk = bknum / bkden;
                    for (j = 0; j < n; j++) {
                        p[j] = bk * p[j] + z[j];
                        pp[j] = bk * pp[j] + zz[j];
                    }
                }
                bkden = bknum;
                mv(p, z);
                for (akden = 0.0, j = 0; j < n; j++) {
                    akden += z[j] * pp[j];
                }
                ak = bknum / akden;
                mv(pp, zz);
                for (j = 0; j < n; j++) {
                    x[j] += ak * p[j];
                    r[j] -= ak * z[j];
                    rr[j] -= ak * zz[j];
                }
                P.asolve(r, z);
                if (itol == 1) {
                    err = norm(r, itol) / bnrm;
                } else if (itol == 2) {
                    err = norm(z, itol) / bnrm;
                } else if (itol == 3 || itol == 4) {
                    zm1nrm = znrm;
                    znrm = norm(z, itol);
                    if (Math.abs(zm1nrm - znrm) > EPSILON * znrm) {
                        dxnrm = Math.abs(ak) * norm(p, itol);
                        err = znrm / Math.abs(zm1nrm - znrm) * dxnrm;
                    } else {
                        err = znrm / bnrm;
                        continue;
                    }
                    xnrm = norm(x, itol);
                    if (err <= 0.5 * xnrm) {
                        err /= xnrm;
                    } else {
                        err = znrm / bnrm;
                        continue;
                    }
                }

                if (iter % 10 == 0) {
                    Logger.info(String.format("BCG: the error after %3d iterations: %.5g", iter, err));
                }

                if (err <= tol) {
                    Logger.info(String.format("BCG: the error after %3d iterations: %.5g", iter, err));
                    break;
                }
            }

            return err;
        }
        
        private static double norm(double[] x, int itol) {
            int len = x.length;
            if (itol <= 3) {
                double res = 0d;
                for (double v : x) {
                    res += v * v;
                }
                return Math.sqrt(res);
            } else {
                int isamax = 0;
                for (int i = 0; i < len; i++) {
                    if (Math.abs(x[i]) > Math.abs(x[isamax])) {
                        isamax = i;
                    }
                }
                return Math.abs(x[isamax]);
            }
        }
    }
    
    private static void fill(double[] vs, double v) {
    	for (int i = 0, len = vs.length; i < len; i++) {
    		vs[i] = v;
    	}
    }
    
    private static double mean(double[][] vs) {
    	int m = vs.length;
    	if (m < 1) return 0;
    	int n = vs[0].length;
    	double sum = sum(vs);
    	return sum/m/n;
    }
    
    private static double sum(double[][] vs) {
    	double sum = 0;
    	for (double[] row : vs) {
    		for (double v : row) {
    			sum += v;
    		}
    	}
    	return sum;
    }
    
	protected static void times(Matrix X, double d) {
		double[][] A = X.getArray();
		for (int r = 0, rows = A.length; r < rows; r++) {
			for (int c = 0, cols = X.getCols(); c < cols; c++) {
				A[r][c] = A[r][c] * d;
			}
		}
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
            	result[i][j] = sign(w[i][j], alpha);
            }
        }
        return  result;
    }
    
    private static double sign(double w, double alpha) {
    	if (w > 0) {
    		return 1*alpha;
    	}
    	else if (w < 0) {
    		return -1*alpha;
    	}
    	else {
    		return 0;
    	}
    }
    
    private static double times(Matrix A, int ci, Matrix B) {
    	double result = 0;
    	int rows = A.getRows();
    	for (int ri = 0; ri < rows; ri++) {
    		result += A.get(ri, ci)*B.get(ri, 0);
    	}
    	return result;
    }

    /// y = A' * x
    private static void tv(Matrix A, double[] x, double[] y) {
    	int rows = A.getRows();
    	int cols = A.getCols();
    	double[][] vs = A.getArray();
    	for (int c = 0; c < cols; c++) {
    		double res = 0;
    		for (int r = 0; r < rows; r++) {
    			res += vs[r][c] * x[r]; 
    		}
    		y[c] = res;
    	}
    }

    /// y = A*x+y
    private static void mv(Matrix A, double[] x, double[] y) {
    	mv(A, 1d, x, 1d, y);
    }
    
    /// y = alpha*A*x+beta*y
    private static void mv(Matrix A, double alpha, double[] x, double beta, double[] y) {
    	int rows = A.getRows();
    	int cols = A.getCols();
    	double[][] vs = A.getArray();
    	for (int r = 0; r < rows; r++) {
    		double[] row = vs[r];
    		double res = dot(row, x);
    		y[r] = beta * y[r] + res * alpha;
    	}
    }

    private static double normInf(double[] x) {
    	double max = 0d;
    	for (double v : x) {
    		double abs = Math.abs(v);
    		if ( abs > max) {
    			max = abs;
    		}
    	}
    	return max;
    }

    private static double dot(double[] x, double[] y, int size) {
    	double res = 0d;
    	for (int i = 0; i < size; i++) {
    		res += x[i] * y[i];
    	}
    	return res;
    }
    
    private static double dot(double[] x, double[] y) {
    	if (x == null || y == null || x.length != y.length) {
    		return 0;
    	}
    	return dot(x, y, x.length);
    }
    
    private static double norm1(double[] x) {
        double norm = 0d;
        for (double v : x) {
            norm += Math.abs(v);
        }
        return norm;
    }
    
    private static double norm2(double[] x) {
        double norm = 0d;

        for (double v : x) {
            norm += v * v;
        }

        norm = Math.sqrt(norm);

        return norm;
    }
    
    private static double max(double[] x) {
        double m = Double.NEGATIVE_INFINITY;
        for (double v : x) {
            if (v > m) {
                m = v;
            }
        }
        return m;
    }
    
    public static double max(double[][] vs) {
    	double m = vs[0][0];
        for (double[] row : vs) {
        	double v = max(row);
            if (m < v) {
                m = v;
            }
        }
        return m;
    }
    
    public static double sum(double[] x) {
        double sum = 0d;
        for (double v : x) {
            sum += v;
        }
        return sum;
    }
    
    private static double sumlogneg(double[][] vs) {
        double sum = 0d;
        for (double[] row : vs) {
            for (double x : row) {
                sum += Math.log(-x);
            }
        }
        return sum;
    }
}
