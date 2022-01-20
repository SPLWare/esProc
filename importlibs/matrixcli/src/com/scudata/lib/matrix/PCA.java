package com.scudata.lib.matrix;

import java.util.*;

import org.apache.commons.math3.linear.*;
import org.ejml.data.Complex_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.simple.SimpleEVD;
import org.ejml.simple.SimpleMatrix;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.fn.algebra.Matrix;
import com.scudata.expression.fn.algebra.Vector;
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
    /*
    private Matrix getPrincipalComponent(double[] eigenvalueArray, Matrix eigenVectors, int n_components) {
        //edited by bd, 2021.4.9, 去除ejml的使用，这个包必须用jdk1.8比较麻烦
        Matrix X = eigenVectors.transpose();
        double[][] tEigenVectors = X.getArray();
        Map<Integer, double[]> principalMap = new HashMap<Integer, double[]>();// key=主成分特征值，value=该特征值对应的特征向量
        TreeMap<Double, double[]> eigenMap = new TreeMap<Double, double[]>(
                Collections.reverseOrder());// key=特征值，value=对应的特征向量；初始化为翻转排序，使map按key值降序排列

        for (int i = 0; i < tEigenVectors.length; i++) {
            double[] value = new double[tEigenVectors[0].length];
            value = tEigenVectors[i];
            eigenMap.put(eigenvalueArray[i], value);
        }

        // 选出前几个主成分
        List<Double> plist = new ArrayList<Double>();// 主成分特征值
        int now_component = 0;
        for (double key : eigenMap.keySet()) {
            if (now_component < n_components) {
                now_component += 1;
                plist.add(key);
                //principalComponentNum++;
            }
            else {
                break;
            }
        }

        // 往主成分map里输入数据
        for (int i = 0; i < plist.size(); i++) {
            if (eigenMap.containsKey(plist.get(i))) {
                principalMap.put(i, eigenMap.get(plist.get(i)));
            }
        }

        // 把map里的值存到二维数组里
        double[][] principalArray = new double[principalMap.size()][];
        Iterator<Map.Entry<Integer, double[]>> it = principalMap.entrySet()
                .iterator();
        for (int i = 0; it.hasNext(); i++) {
            principalArray[i] = it.next().getValue();
        }

        return new Matrix(principalArray);
    }
    */
    
    // 训练方法
    protected FitResult fit(Matrix inputData, int n_components) {
    	// 取每列均值，做中心化处理，使得每列均值为0
        Vector averageVector = inputData.getAverage();
        Matrix averageArray = inputData.changeAverageToZero(averageVector);
        System.out.println(averageArray.toSequence(option, true).toString());
        // 计算协方差矩阵X(XT)
        Matrix varMatrix = averageArray.covm();
        System.out.println(varMatrix.toSequence(option, true).toString());
        // 协方差矩阵的特征值分解
        RealMatrix m = new org.apache.commons.math3.linear.Array2DRowRealMatrix(varMatrix.getArray());
        EigenDecomposition evd = new org.apache.commons.math3.linear.EigenDecomposition(m);
        double[] ev = evd.getRealEigenvalues();
        double[][] V = evd.getV().getData();
        
        //edited by bd, 2021.12.29, 测试发现math3的特征值分解并不能保证ev有序，所以还是应该排个序
        Sequence evSeq = (new Vector(ev)).toSequence();
        Sequence sortP = evSeq.psort("z");
        Sequence vSeq = (Sequence) new Matrix(V).toSequence(option, true);
        System.out.println("****************");
        System.out.println(n_components);
        System.out.println(vSeq.toString());
        vSeq = vSeq.get(sortP);
        System.out.println(vSeq.toString());
        evSeq = evSeq.get(sortP);
        Matrix principalArray = getPrincipalComponent(new Matrix(vSeq), n_components);
        System.out.println(principalArray.toSequence(option, true).toString());
        
        //edited by bd, 2021.12.29, 将特征值分解时，得到的特征向量矩阵符号与matlab统一
        dealV(principalArray); 
        System.out.println(principalArray.toSequence(option, true).toString());

        //Matrix principalArray = getPrincipalComponent(ev, new Matrix(V), n_components);
        return new FitResult(averageVector, new Vector(evSeq), principalArray.transpose());
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
        testinput.changeAverageToZero(averageObject);
        Matrix resultMatrix = testinput.times(principalMatrix);
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
    	testinput.changeAverageToZero(fr.mu);
        Matrix resultMatrix = testinput.times(fr.coeff);
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
    
    public static void main(String[] args) {
    	double[][] data = new double[][]{{1,2,3,4},{2,3,1,2},{1,1,1,-1},{1,0,-2,-6}};
    	//double[][] data = new double[][] {{17,24,1,8,15},{23,5,7,14,16},{4,6,13,20,22},{10,12,19,21,3},{11,18,25,2,9}};
    	Sequence seq = new Sequence();
    	for (int i=0, ilen=data.length; i<ilen;i++) {
    		double[] data1 = data[i];
        	Sequence sub = new Sequence();
    		for (int j = 0, jlen = data1.length; j < jlen; j++) {
    			sub.add(data1[j]);
    		}
    		seq.add(sub);
    	}
    	Matrix A = new Matrix(data);
    	PCA pca = new PCA();
    	//pca.fitj(A, 3);
    	FitResult fr = pca.fit(A, 3);
    	System.out.println(fr.mu.toSequence().toString());
    	System.out.println(fr.latent.toSequence().toString());
    	System.out.println(fr.coeff.toSequence(pca.option, true).toString());
    	
        RealMatrix m = new org.apache.commons.math3.linear.Array2DRowRealMatrix(data);
        EigenDecomposition evd = new org.apache.commons.math3.linear.EigenDecomposition(m);
        double[] ev = evd.getRealEigenvalues();
        double[][] V = evd.getV().getData();
        double[] iev = evd.getImagEigenvalues();
    	System.out.println((new Vector(ev)).toSequence().toString());
    	System.out.println((new Vector(iev)).toSequence().toString());
    	System.out.println((new Matrix(V)).toSequence(pca.option, true).toString());
    	System.out.println("--done--");
    }
    
    

 // 训练方法
     /**
      * 训练方法
      * @param inputData	训练数据
      * @param n_components
      * @return
      */
    protected void fitj(Matrix inputData, int n_components) {
        Vector averageVector = inputData.getAverage();
        Matrix averageArray = inputData.changeAverageToZero();
        Matrix varMatrix = getVarianceMatrix(averageArray);
        Vector eigValueR = getEigenvalueMatrix2(varMatrix);

        Matrix eigVectorR = getEigenVectorMatrix2(varMatrix);
        List<Matrix>  resultList =  merge(eigValueR,eigVectorR);
        Matrix eigenvalueMatrix = resultList.get(0);
        Matrix eigenVectorMatrix = resultList.get(1);
        Matrix principalArray = getPrincipalComponent2(inputData, eigenvalueMatrix, eigenVectorMatrix, n_components);
        //Object[] dimenRedut = PCA.ArrToOb(principalArray);
        //Object[] result = mergeObject(averageVector,dimenRedut);
        //return principalArray;
        //return new FitResult(averageVector, eigValueR, principalArray);
		System.out.println(averageVector.toSequence().toString());
		System.out.println(eigValueR.toSequence().toString());
		System.out.println(principalArray.toSequence(option, true).toString());
    }
     

    /**
     * 获取主成分分析矩阵
     * @param primaryArray
     * @param eigenvalue
     * @param eigenVectors
     * @param n_components
     * @return
     */
    private Matrix getPrincipalComponent2(Matrix primaryArray,
                                                   Matrix eigenvalue, Matrix eigenVectors, int n_components) {
        SimpleMatrix X = new SimpleMatrix(eigenVectors.getArray()).transpose();
        double[][] tEigenVectors = getArray(X);
//        Matrix A = new Matrix(eigenVectors);// 定义一个特征向量矩阵
//        double[][] tEigenVectors = A.transpose().getArray();// 特征向量转置
        Map<Integer, double[]> principalMap = new HashMap<Integer, double[]>();// key=主成分特征值，value=该特征值对应的特征向量
        TreeMap<Double, double[]> eigenMap = new TreeMap<Double, double[]>(
                Collections.reverseOrder());// key=特征值，value=对应的特征向量；初始化为翻转排序，使map按key值降序排列
        //double total = 0;// 存储特征值总和
        int index = 0, n = eigenvalue.getRows();
        double[] eigenvalueArray = new double[n];// 把特征值矩阵对角线上的元素放到数组eigenvalueArray里
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j)
                    eigenvalueArray[index] = eigenvalue.get(i, j);
            }
            index++;
        }

        for (int i = 0; i < tEigenVectors.length; i++) {
            double[] value = new double[tEigenVectors[0].length];
            value = tEigenVectors[i];
            eigenMap.put(eigenvalueArray[i], value);
        }

        // 求特征总和
        //for (int i = 0; i < n; i++) {
        //    total += eigenvalueArray[i];
        //}
        // 选出前几个主成分
        //double temp = 0;
        //int principalComponentNum = 0;// 主成分数
        List<Double> plist = new ArrayList<Double>();// 主成分特征值
        int now_component = 0;
        for (double key : eigenMap.keySet()) {
            if (now_component < n_components) {
                now_component += 1;
                plist.add(key);
                //principalComponentNum++;
            }
            else {
                break;
            }
        }

        // 往主成分map里输入数据
        for (int i = 0; i < plist.size(); i++) {
            if (eigenMap.containsKey(plist.get(i))) {
                principalMap.put(i, eigenMap.get(plist.get(i)));
            }
        }

        // 把map里的值存到二维数组里
        double[][] principalArray = new double[principalMap.size()][];
        Iterator<Map.Entry<Integer, double[]>> it = principalMap.entrySet()
                .iterator();
        for (int i = 0; it.hasNext(); i++) {
            principalArray[i] = it.next().getValue();
        }

        return new Matrix(principalArray);
    }

    /**
     * 获取特征向量矩阵
     * @param matrix	源矩阵
     * @return
     */
    private Matrix getEigenVectorMatrix2(Matrix matrix) {
        SimpleMatrix X = new SimpleMatrix(matrix.getArray());
        SimpleEVD<SimpleMatrix> U = X.eig();
        org.ejml.interfaces.decomposition.EigenDecomposition<?> aa = U.getEVD();
        double[][] result = new double[matrix.getRows()][matrix.getRows()];
        for (int i =0; i<result.length;i++){
            org.ejml.data.Matrix cc = aa.getEigenVector(i);
            DMatrixRMaj dd =  (DMatrixRMaj ) cc;
            double[] singleMatrix = dd.data;
            result[i] = singleMatrix;
        }
        double[][] result1 = new double[matrix.getRows()][matrix.getRows()];
        for (int i =0; i<result.length;i++){
            for (int j =0; j<result.length;j++){
                result1[i][result.length-1-j] = result[j][i];
            }
        }

        return new Matrix(result1);
    }
    
    /**
     * 获取特征值向量
     * @param matrix	源矩阵
     * @return
     */
    private Vector getEigenvalueMatrix2(Matrix matrix) {
        SimpleMatrix X = new SimpleMatrix(matrix.getArray());
        SimpleEVD<SimpleMatrix> U = X.eig();
        Object[] bb =  U.getEigenvalues().toArray();
        Vector cc = ejmloneObToVector(bb);
//        Arrays.sort(cc);
//        // 由特征值组成的对角矩阵,eig()获取特征值
        double[] result = new double[cc.len()];
        for (int i =0, iSize = cc.len(); i<iSize;i++){
            result[i] = cc.get(result.length-1-i);
        }
        return new Vector(result);
    }
    
    private Vector ejmloneObToVector(Object []pracdata) {

        Object[] toss = (Object[]) pracdata;
        List<Object> seconds = Arrays.asList(toss);

        double[]testData = new double[toss.length];

        for (int i =0; i< toss.length; i++) {
            Object bb = seconds.get(i);
            Complex_F64 cc = (Complex_F64) bb;
            testData[i] = cc.real;
        }
        return new Vector(testData);
    }
	
	/**
	 * 初始化矩阵
	 * @param value	二维数组表示的矩阵值
	 */
	protected double[][] getArray(SimpleMatrix smatrix) {
		int rows = smatrix.numRows();
		int cols = smatrix.numCols();
		double[][] A = new double[rows][cols];
        for (int r = 0; r < rows ; r++) {
            for (int c = 0; c < cols; c++) {
            	A[r][c] = smatrix.get(r, c);
            }
        }
        return A;
	}
    /**
     * 
     * @param eigenvalueMatrix	
     * @param eigenVectorMatrix	特征向量矩阵
     * @return
     */
    private List<Matrix> merge(Vector eigenvalueMatrix, Matrix eigenVectorMatrix) {
        double[][] eigVectorT = eigenVectorMatrix.transpose().getArray();
        TreeMap<Double, double[]> eigenMap = new TreeMap<Double, double[]>(
        );//
        for (int i = 0; i < eigVectorT.length; i++) {
            double[] value = eigVectorT[i];
            eigenMap.put(eigenvalueMatrix.get(i), value);
        }
        int len = eigenvalueMatrix.len();
        double[] eigValue = new double[len];
        double[][] eigValueR = new double[len][len];



        double[][] eigVector = new double[eigVectorT.length][eigVectorT[0].length];
        int i = 0;
        for (double key : eigenMap.keySet()) {
            eigValue[i] = key;
            eigVector[i] = eigenMap.get(key);
            i++;
        }
        for (int j =0; j<len; j++){
            eigValueR[j][j] = eigValue[j];
        }
        Matrix eigVectorR = new Matrix(eigVector).transpose();

        List<Matrix> result = new ArrayList<Matrix>();
        result.add(new Matrix(eigValueR));
        result.add(eigVectorR);
        return result;
    }
}
