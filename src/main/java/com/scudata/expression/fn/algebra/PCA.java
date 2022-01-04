package com.scudata.expression.fn.algebra;

import org.apache.commons.math3.linear.*;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

public class PCA extends Function {
	/**
	 * pca(A,F)和pca(A,n), n省略为A的列数，返回的主成分系数矩阵无法降维，但可以用来计算主成分得分
	 * @param ctx	上下文
	 * @return
	 */
	public Object calculate (Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pca" + mm.getMessage("function.missingParam"));
		}
		Object o1 = null;
		Object o2 = null;
		if (param.isLeaf()) {
			// 只有一个参数，pca(A), n会自动设置为A的列数
			o1 = param.getLeafExpression().calculate(ctx);
		} else if (param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pca" + mm.getMessage("function.invalidParam"));
		} else {
			IParam sub1 = param.getSub(0);
			IParam sub2 = param.getSub(1);
			if (sub1 == null || sub2 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pca" + mm.getMessage("function.invalidParam"));
			}
			o1 = sub1.getLeafExpression().calculate(ctx);
			o2 = sub2.getLeafExpression().calculate(ctx);
		}
		if (o1 instanceof Sequence) {
			Matrix A = new Matrix((Sequence)o1);
			if (o2==null || o2 instanceof Number) {
				int b = A.getCols();
				if (o2 != null) {
					b = ((Number) o2).intValue();
				}
				if (option != null && option.contains("r")) {
					// 直接对A降维，先保留，一般用不到
					FitResult fr = fit(A, b);
					Matrix result = transformj(fr, A);
					return result.toSequence(option, true);
				}
				else {
					// 返回fit结果，结果处理为序列，由3个成员组成，第1个为均值序列mu，第2个为潜伏中的主成分方差latent，第3个为主成分系数矩阵coeff。可以用来作为第二个参数执行pca(A,X)
					FitResult fr = fit(A, b);
					Sequence result = new Sequence(3);
					result.add(fr.mu.toSequence());
					result.add(fr.latent.toSequence());
					result.add(fr.coeff.toSequence(option, true));
					return result;
				}
			}
			else if (o2 instanceof Sequence) {
				Sequence seq = (Sequence) o2;
				if (seq.length() >= 3) {
					o1 = seq.get(1);
					o2 = seq.get(2);
					Object o3 = seq.get(3);
					if (o1 instanceof Sequence && o2 instanceof Sequence && o3 instanceof Sequence) {
						Vector dv = new Vector((Sequence) o1);
						Vector dv2 = new Vector((Sequence) o2);
						Matrix dm = new Matrix((Sequence) o3);
						FitResult fr = new FitResult(dv, dv2, dm);
						Matrix result = transformj(fr, A);
						return result.toSequence(option, true);
					}
				}
			}
			MessageManager mm = EngineMessage.get();
			throw new RQException("pca" + mm.getMessage("function.paramTypeError"));
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pca" + mm.getMessage("function.paramTypeError"));
		}
	}
    
	/**
	 * 获取协方差矩阵
	 * @param matrix
	 * @return
	 */
    private Matrix getVarianceMatrix(Matrix matrix) {
        int rows = matrix.getRows();
        int cols = matrix.getCols();
        double[][] result = new double[cols][cols];// 协方差矩阵
        for (int c = 0; c < cols; c++) {
            for (int c2 = 0; c2 < cols; c2++) {
                double temp = 0;
                for (int r = 0; r < rows; r++) {
                    temp += matrix.get(r, c) * matrix.get(r, c2);
                }
                result[c][c2] = temp / (rows - 1);
            }
        }
        return new Matrix(result);
    }

    /**
     * 获取主成分分析矩阵
     * @param primaryArray
     * @param eigenvalue
     * @param eigenVectors
     * @param n_components
     * @return
     */
    private Matrix getPrincipalComponent(Matrix eigenVectors, int n_components) {
        Matrix X = eigenVectors.transpose();
        double[][] tEigenVectors = X.getArray();
        double[][] principalArray = new double[n_components][];
        for (int i = 0; i < n_components; i++) {
            principalArray[i] = tEigenVectors[i];
        }

        return new Matrix(principalArray);
    }
    
    // 训练方法
    protected FitResult fit(Matrix inputData, int n_components) {
    	// 取每列均值，做中心化处理，使得每列均值为0
        Vector averageVector = inputData.getAverage();
        Matrix averageArray = inputData.changeAverageToZero(averageVector);
        // 计算协方差矩阵X(XT)
        Matrix varMatrix = getVarianceMatrix(averageArray);
        // 协方差矩阵的特征值分解
        RealMatrix m = new org.apache.commons.math3.linear.Array2DRowRealMatrix(varMatrix.getArray());
        EigenDecomposition evd = new org.apache.commons.math3.linear.EigenDecomposition(m);
        double[] ev = evd.getRealEigenvalues();
        double[][] V = evd.getV().getData();
        //math3的特征值分解处理中，ev已经做完了排序，在下面的选取主成分时不必再排序，原方法暂时保留

        //edited by bd, 2021.12.29, 测试发现math3的特征值分解并不能保证ev有序，所以还是应该排个序
        Sequence evSeq = (new Vector(ev)).toSequence();
        Sequence sortP = evSeq.psort("z");
        Sequence vSeq = (Sequence) new Matrix(V).toSequence(option, true);
        vSeq = vSeq.get(sortP);
        evSeq = evSeq.get(sortP);
        Matrix principalArray = getPrincipalComponent(new Matrix(vSeq), n_components);
        
        //edited by bd, 2021.12.29, 将特征值分解时，得到的特征向量矩阵符号与matlab统一
        dealV(principalArray); 

        //Matrix principalArray = getPrincipalComponent(ev, new Matrix(V), n_components);
        return new FitResult(averageVector, new Vector(evSeq), principalArray.transpose());
        //return new FitResult(averageVector, new Vector(ev), principalArray.transpose());
    }
    
    private void dealV(Matrix V) {
    	//目前发现matlab中特征值向量的规律是使向量中绝对值最大的为正数，没找到文档陈述，测试了十几个矩阵的eig(vpa(A))的结果是这样
    	double[][] vs = V.getArray();
    	int cols = V.getCols();
    	for (int i = 0, len = vs.length; i < len; i++) {
			int loc = 0;
			double max = Math.abs(vs[i][0]);
    		for (int j = 1; j < cols; j++) {
    			double tmp = Math.abs(vs[i][j]); 
    			if (tmp>max) {
    				max = tmp;
    				loc = j;
    			}
    		} 
    		if (vs[i][loc]<0) {
        		for (int j = 0; j < cols; j++) {
        			vs[i][j] = -vs[i][j];
        		} 
    		}
    	}
    }
    
//    转换方法
    /**
     * 转换方法
     * @param principalDouble
     * @param averageObject
     * @param testinput
     * @return
     */
    public Matrix transform(Matrix principalMatrix, Vector averageObject, Matrix testinput){
    	Matrix averageArray = testinput.changeAverageToZero(averageObject);
        Matrix resultMatrix = averageArray.times(principalMatrix);
        return resultMatrix;
    }

    //    集算器使用转换方法
    /**
     * 根据训练结果转换
     * @param fr
     * @param testinput
     * @return
     */
    protected Matrix transformj(FitResult fr, Matrix testinput) {
    	Matrix averageArray = testinput.changeAverageToZero(fr.mu);
        Matrix resultMatrix = averageArray.times(fr.coeff);
        return resultMatrix;
    }
    
    protected class FitResult {
    	// mu
    	protected Vector mu;
    	// coeff
    	protected Matrix coeff;
    	// latent
    	protected Vector latent;
    	
    	protected FitResult(Vector mu, Vector latent, Matrix coeff) {
    		this.coeff = coeff;
    		this.mu = mu;
    		this.latent = latent;
    	}
    }

}
