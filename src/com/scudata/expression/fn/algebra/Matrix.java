package com.scudata.expression.fn.algebra;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
//import org.ejml.simple.SimpleMatrix;

import com.scudata.common.Logger;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;

/**
 * 矩阵基本类，提供矩阵的各类计算
 * 集算器中的矩阵使用数列的序列表示，一般不检查
 * added by bd, 2021.1.15, 用这个类兼容向量
 * @author bidalong
 *
 */
public class Matrix {

	private double[][] A;
	private int rows, cols;
	//added by bd, 2021.1.15, 是否是向量，向量会存储为单行或单列矩阵
	private boolean ifVector = false;
	
	protected Matrix(int rs, int cs) {
		this.A = new double[rs][cs];
		this.rows = rs;
		this.cols = cs;
	}
	
	/**
	 * 初始化矩阵
	 * @param value	二维数组表示的矩阵值
	 */
	/*
	protected Matrix(SimpleMatrix smatrix) {
		this.rows = smatrix.numRows();
		this.cols = smatrix.numCols();
		this.A = new double[this.rows][this.cols];
        for (int r = 0; r < this.rows ; r++) {
            for (int c = 0; c < this.cols; c++) {
            	this.A[r][c] = smatrix.get(r, c);
            }
        }
	}
	*/
	
	/**
	 * 初始化一个矩阵，如果matrix中序列长度不等，将按最大长度设置列数，其它补0
	 * 如果matrix中不为序列，按单列处理
	 * @param matrix
	 */
	protected Matrix(Sequence matrix) {
		int rows = matrix == null ? 0 : matrix.length();
		if (rows > 0) {
			if (matrix instanceof Table) {
				//added by bd, 2021.1.22, 添加对序表的支持
				// 二维数组, 序表
				Table tab = (Table) matrix;
				this.cols = tab.dataStruct().getFieldCount();
				this.rows = rows;
				this.A = new double[rows][this.cols];
				for (int i = 1; i <= rows; i++) {
					double[] row = getRow(tab.getRecord(i), this.cols);
					this.A[i-1] = row;
				}
			}
			else {
				int cols = 0;
				for (int r = 0; r < rows; r++ ) {
					Object row = matrix.get(r+1);
					if (row instanceof Sequence) {
						int cols2 = ((Sequence) row).length();
						if (cols < cols2) {
							cols = cols2;
						}
					}
				}
				if (cols == 0) {
					// 单一序列，视为一行数据，
					// edited by bd, 2021.1.15, 只有这种情况会被视为向量，纵向量不会自动认知
					// edited by bd, 2021.2.25, 向量改为默认纵向量，这样定义简单一些，横向量用[[1,2,3]]这样
					cols = 1;
					this.ifVector = false;
					this.A = new double[rows][1];
					for (int r = 0; r < rows; r++) {
						Object obj = ((Sequence) matrix).get(r+1);
						if (obj instanceof Number) {
							this.A[r][0] = ((Number) obj).doubleValue();
						}
					}
				}
				else {
					this.A = new double[rows][cols];
					for (int r = 0; r < rows; r++) {
						Object row = matrix.get(r+1);
						if (row instanceof Sequence) {
							int cols2 = ((Sequence) row).length();
							for (int c = 0; c < cols2; c++) {
								Object obj = ((Sequence) row).get(c+1);
								if (obj instanceof Number) {
									this.A[r][c] = ((Number) obj).doubleValue();
								}
							}
						}
						else if (row instanceof Number) {
							this.A[r][0] = ((Number) row).doubleValue();
						}
					}
				}
				this.cols = cols;
				this.rows = rows;
			}
		}
	}
	
	/**
	 * 读取一个一维序列为double数组，非数值型全按0计
	 * @param seq
	 * @return
	 */
	protected static double[] getRow(Sequence seq, int n) {
		if (n < 1) {
			n = seq == null ? 0 : seq.length();
		}
		if (n < 1) {
			return null;
		}
		double[] row = new double[n];
		for (int i = 1; i <=n; i++) {
			Object obj = seq.get(i);
			row[i-1] = getNumber(obj);
		}
		return row;
	}
	
	/**
	 * 读取一个一维序列为double数组，非数值型全按0计
	 * @param seq
	 * @return
	 */
	protected static double[] getRow(Record rec, int n) {
		double[] row = new double[n];
		Object[] vs = rec.getFieldValues();
		for (int i = 0; i <n; i++) {
			Object obj = vs[i];
			row[i] = getNumber(obj);
		}
		return row;
	}
	
	/*
	 * 将一个Object转换为double返回
	 */
	private static double getNumber(Object obj) {
		double d = 0;
		if (obj instanceof Sequence ) {
			if (((Sequence) obj).length() == 0) {
				return d;
			}
			else {
				obj = ((Sequence) obj).get(1);
			}
		}
		if (obj instanceof Number) {
			d = ((Number) obj).doubleValue();
		}
		else if (obj instanceof String) {
			d = Double.valueOf(obj.toString());
		}
		return d;
	}
	
	/**
	 * 对于向量，获取向量数据, added by bd, 2021.1.15
	 * @return
	 */
	protected double[] getVector() {
		if (this.ifVector) {
			if (this.rows == 1) {
				return this.A[0];
			}
			else if(this.cols == 1) {
				double[] vector = new double[this.rows];
				for (int i = 0; i < this.rows; i++) {
					vector[i] = this.A[i][0];
				}
				return vector;
			}
		}
		return null;
	}
	
	/**
	 * 初始化一个向量矩阵
	 * @param vector		生成向量的序列
	 * @param vertical	是否纵向量，false时为横向量
	 */
	protected Matrix(double[] vector, boolean vertical) {
		int size = vector == null ? 0 : vector.length;
		this.ifVector = true;
		if (vertical) {
			this.rows = size;
			this.cols = 1;
			this.A = new double[this.rows][this.cols];
			for (int r = 0; r < size; r++) {
				this.A[r][0] = vector[r];
			}
		}
		else {
			this.rows = 1;
			this.cols = size;
			this.A = new double[this.rows][this.cols];
			this.A[0] = vector;
		}
	}
	
	/**
	 * 初始化一个向量矩阵
	 * @param vector		生成向量的序列
	 * @param vertical	是否纵向量，false时为横向量
	 */
	protected Matrix(Sequence vector, boolean vertical) {
		int size = vector == null ? 0 : vector.length();
		this.ifVector = true;
		if (vertical) {
			this.rows = size;
			this.cols = 1;
			this.A = new double[this.rows][this.cols];
			for (int r = 0; r < size; r++) {
				Object obj = vector.get(r+1);
				if (obj instanceof Number) {
					this.A[r][0] = ((Number) obj).doubleValue();
				}
			}
		}
		else {
			this.rows = 1;
			this.cols = size;
			this.A = new double[this.rows][this.cols];
			for (int i = 0; i < size; i++) {
				Object obj = vector.get(i+1);
				if (obj instanceof Number) {
					this.A[0][i] = ((Number) obj).doubleValue();
				}
			}
		}
	}

	/**
	 * 初始化一个向量矩阵
	 * @param A		二维数组
	 * @param rows	行数，应该对应A
	 * @param cols		列数，应该对应A
	 */
	public Matrix(double[][] A) {
		this.A = A;
		this.rows = A.length;
		int cols = 0;
		for (int i = 0; i < this.rows; i++) {
			double[] row = this.A[i];
			int thisCols = row == null ? 0 : row.length;
			if (cols < thisCols) {
				cols = thisCols;
			}
		}
		this.cols = cols;
	}

	/**
	 * 初始化一个向量矩阵
	 * @param A		二维数组
	 * @param rows	行数，应该对应A
	 * @param cols		列数，应该对应A
	 */
	public Matrix(double[][] A, int rows, int cols) {
		this.A = A;
		this.rows = rows;
		this.cols = cols;
	}
	
	/**
	 * 将矩阵转为序列，特别的，只有一个成员时直接返回数据
	 * 为避免double运算时造成的误差，做四舍五入处理
	 * added by bd 2021.1.22, 添加参数允许返回为无列名序表
	 * @return
	 */
	public Object toSequence(String option, boolean real) {
		boolean ift = option != null && option.indexOf('t')>-1;
		boolean ifv = option != null && option.indexOf('v')>-1;
		if (ift) {
	        String[] cols = new String[]{"_1"};
	        if (this.cols > 1){
	        	cols = new String[this.cols];
		        for(int i=0; i<this.cols; i++){
		        	cols[i] = "_"+(i+1);
		        }
	        }
	        Table tbl = new Table(cols);
	        for(int i=0; i<this.rows; i++){
	        	Double[] r = new Double[this.cols];
	        	for(int j=0; j<this.cols; j++){
		        	r[j] = getValue(i, j, real);
	        	}
	        	tbl.newLast(r);
	        }
	        
	        return tbl;
		}
		if (this.rows == 1 && this.cols == 1) {
			// 如果只有一个成员，直接返回
			return this.A[0][0];
		}
		/*
		double min = 1;
		for (int r = 0; r < this.rows; r++) {
			for (int c = 0; c < this.cols; c++) {
				double d = Math.abs(this.A[r][c]);
				if (d > 0 && min > d) {
					min = d;
				}
			}
		}
		double pow = Math.ceil(Math.log(min)) - 5;
		double scale = Math.pow(10, pow);
		*/
		if (ifv && this.rows == 1) {
			//edited by bd, 2021.2.25, 当结果只有一列，且计算数中包含向量时，返回数列
			Sequence sub = new Sequence(this.cols);
			for (int c = 0; c < this.cols; c++) {
				double d = getValue(0, c, real);
				sub.add(d);
			}
			return sub;
		}
		else if (ifv && this.cols == 1) {
			//added by bd, 2021.2.25, 当结果只有一列，且计算数中包含向量时，返回数列
			Sequence sub = new Sequence(this.rows);
			for (int r = 0; r < this.rows; r++) {
				double d = getValue(r, 0, real);
				sub.add(d);
			}
			return sub;
		}
		Sequence seq = new Sequence(this.rows);
		for (int r = 0; r < this.rows; r++) {
			Sequence sub = new Sequence(this.cols);
			for (int c = 0; c < this.cols; c++) {
				double d = getValue(r, c, real);
				sub.add(d);
			}
			seq.add(sub);
		}
		return seq;
	}
	
	/*
	 * 修改矩阵中某个值，added by bd, 2021.4.8
	 */
	protected void set(int r, int c, double v) {
		this.A[r][c] = v;
	}

	private final static double scale = 1000000d;
	private double getValue(int r, int c, boolean real) {
		double d = this.A[r][c];
		if (!real) {
			d *= scale;
			if (d > Long.MIN_VALUE && d < Long.MAX_VALUE) {
				d = Math.round(d)/scale;
			} else {
				d = d / scale;
			}
		}
		return d;
	}

	/**
	 * 获取行数
	 * @return	矩阵行数
	 */
	public int getRows() {
		return this.rows;
	}

	/**
	 * 获取列数
	 * @return	矩阵列数
	 */
	public int getCols() {
		return this.cols;
	}

	/**
	 * 获取二维数组
	 * @return	矩阵的二维数组
	 */
	public double[][] getArray() {
		return this.A;
	}
	
	/**
	 * 求当前矩阵的协方差矩阵
	 * @return
	 */
	protected Matrix covm() {
		Matrix X = new Matrix(this.cols, this.cols);
		double[][] xs = X.getArray();
		// 各个维度数组，用A转置
		double[][] dims = this.transpose().getArray();
		// 各个维度的均值
		double[] dimv = new double[this.cols];
		for (int i = 0; i < this.cols; i++) {
			double[] dim = dims[i];
			double res = 0;
			for (int j = 0; j < this.rows; j++) {
				res += dim[j];
			}
			dimv[i] = res/this.rows;
		}
		for (int i = 0; i < this.cols; i++) {
			for (int j = 0; j < this.cols; j++) {
				if (i > j) {
					// 已经计算过的
					xs[i][j] = xs[j][i];
				}
				else if (i == j) {
					// 对角线上的协方差
					double cov = 0;
					for (int k = 0; k < this.rows; k++) {
						cov += Math.pow(this.A[k][i] - dimv[i], 2);
					}
					xs[i][j] = cov/(this.rows - 1);
				}
				else {
					// 右上三角的协方差
					double cov = 0;
					for (int k = 0; k < this.rows; k++) {
						cov += (this.A[k][i] - dimv[i]) * (this.A[k][j] - dimv[j]);
					}
					xs[i][j] = cov/(this.rows - 1);
				}
			}
		}
		return X;
	}
	
	/**
	 * 用矩阵数据来生成一个序表
	 * @return
	 */
	protected Table toTable() {
		String[] cns = new String[this.cols];
		for (int c = 0; c < this.cols; c++) {
			cns[c] = "Col"+(c+1);
		}
		Table t = new Table(cns);
		for (int r = 0; r < this.rows; r++) {
			Record rec = t.newLast();
			for (int c = 0; c < this.cols; c++) {
				rec.set(c, this.A[r][c]);
			}
		}
		return t;
	}

	/**
	 * 获取子矩阵
	 * @param r		需取出的各行号
	 * @param j0	起始列好
	 * @param j1	结束列号
	 * @return		指定位置的子矩阵
	 * @exception	异常
	 */
	protected Matrix getMatrix(int[] r, int j0, int j1) {
		Matrix X = new Matrix(r.length, j1 - j0 + 1);
		double[][] B = X.getArray();
		try {
			for (int i = 0; i < r.length; i++) {
				for (int j = j0; j <= j1; j++) {
					B[i][j - j0] = A[r[i]][j];
				}
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new ArrayIndexOutOfBoundsException("Submatrix indices");
		}
		return X;
	}

	/**
	 * 获取子矩阵
	 * @param r0	起始行号，从0开始
	 * @param r1	结束行号，包括在子矩阵中
	 * @param c0	起始列号，从0开始
	 * @param c1	结束列号，包括在子矩阵中
	 * @return		从当前矩阵中截取子矩阵
	 * @exception	行列超限错误
	 */
	public Matrix getMatrix(int r0, int r1, int c0, int c1) {
		Matrix X = new Matrix(r1 - r0 + 1, c1 - c0 + 1);
		double[][] B = X.getArray();
		try {
			for (int r = r0; r <= r1; r++) {
				for (int c = c0; c <= c1; c++) {
					B[r - r0][c - c0] = A[r][c];
				}
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new ArrayIndexOutOfBoundsException("Submatrix indices");
		}
		return X;
	}
	
	/**
	 * 从矩阵中获取指定行列的数，返回double
	 * @param matrix	矩阵，使用数列的序列表示
	 * @param r			行号，从0开始
	 * @param c			列号，从0开始
	 * @return
	 */
	protected static double get(Sequence matrix, int r, int c) {
		Object row = matrix.get(r+1);
		if (row instanceof Sequence) {
			int len = ((Sequence) row).length();
			if (len >= c) {
				Object obj = ((Sequence) row).get(c+1);
				if (obj instanceof Number) {
					return ((Number) obj).doubleValue();
				}
			}
		}
		return 0d;
	}
	
	/**
	 * 判断是否矩阵，不是矩阵返回null，是矩阵返回行列数构成的数组
	 * @param matrix	矩阵
	 * @param ifNumerical	是否判断成员为数值
	 * @return	是否矩阵
	 */
	protected static int[] ifMatrix(Sequence matrix, boolean ifNumerical) {
		int rows = matrix == null ? 0 : matrix.length();
		if (rows > 0) {
			int cols = 0;
			for (int r = 0; r < rows; r++ ) {
				Object row = matrix.get(r+1);
				if (row instanceof Sequence) {
					int cols2 = ((Sequence) row).length();
					if (cols == 0) {
						cols = cols2;
					}
					else if (cols != cols2) {
						return null;
					}
				}
				else {
					return null;
				}
				if (ifNumerical) {
					for (int c = 0; c < cols; c++) {
						Object obj = ((Sequence) row).get(c+1);
						if (!(obj instanceof Number)) {
							return null;
						}
					}
				}
			}
			int[] bak = {rows, cols};
			return bak;
		}
		return null;
	}
	
	/**
	 * 判断是否矩阵，不是矩阵返回null，是矩阵返回行列数构成的数组
	 * @return	本实例是否矩阵
	 */
	protected boolean ifMatrix() {
		return this.A != null;
	}
	
	/**
	 * 判断是否方阵，是方阵返回方阵的行列数(相等)
	 * @param matrix	矩阵
	 * @param ifNumerical	是否判断成员为数值
	 * @return	是否方阵
	 */
	protected static int ifSquare(Sequence matrix, boolean ifNumerical) {
		int rows = matrix == null ? 0 : matrix.length();
		if (rows > 0) {
			for (int r = 0; r < rows; r++ ) {
				Object row = matrix.get(r+1);
				if (row instanceof Sequence) {
					int cols = ((Sequence) row).length();
					if (cols != rows) {
						return 0;
					}
					if (ifNumerical) {
						for (int c = 0; c < cols; c++) {
							Object obj = ((Sequence) row).get(c+1);
							if (!(obj instanceof Number)) {
								return 0;
							}
						}
					}
				}
				else {
					return 0;
				}
			}
			return rows;
		}
		return 0;
	}
	
	/**
	 * 判断是否方阵
	 * @return
	 */
	protected boolean ifSquare() {
		return this.rows > 0 && this.rows == this.cols;
	}

	/**
	 * 转置
	 * @return
	 */
	public Matrix transpose(){
		Matrix X = new Matrix(this.cols, this.rows);
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				X.A[c][r] = A[r][c];
			}
		}
		return X;
	}

	/**
	 * 转置，如果matrix不满，则在返回的序列中用null补齐
	 * @param matrix
	 * @return
	 */
	public static Sequence transpose(Sequence matrix){
		int rows = matrix == null ? 0 : matrix.length();
		if (rows > 0) {
			int cols = 0;
			for (int r = 0; r < rows; r++ ) {
				Object row = matrix.get(r+1);
				if (row instanceof Sequence) {
					int cols2 = ((Sequence) row).length();
					if (cols < cols2) {
						cols = cols2;
					}
				}
			}
			Sequence trans = new Sequence(cols);
			for (int c = 0; c < cols; c++) {
				Sequence row0 = new Sequence(rows);
				trans.add(row0);
				for (int r = 0; r < rows; r++) {
					Object row = matrix.get(r+1);
					if (row instanceof Sequence) {
						int cols2 = ((Sequence) row).length();
						if (cols2 > c) {
							row0.add(((Sequence) row).get(c+1));
						}
						else {
							row0.add(null);
						}
					}
					else if (c == 0) {
						row0.add(row);
					}
					else {
						row0.add(null);
					}
				}
			}
			return trans;
		}
		return null;
	}

	/**
	 * 获取矩阵中指定位置的数，不做超限判断
	 * @param i		行号
	 * @param j		列号
	 * @return
	 */
	protected double get(int r, int c) {
		return this.A[r][c];
	}

	/**
	 * 检查两个矩阵的行数列数是否相等，很多矩阵运算都需要对应才能执行
	 * @param B
	 */
	private void checkMatrixSize(Matrix B) {
		if (B.rows != this.rows || B.cols != this.cols) {
			throw new IllegalArgumentException("Matrix dimensions must agree.");
		}
	}

	/**
	 * 矩阵相加，C=A+B
	 * @param B		用来执行加法的矩阵
	 * @return		矩阵相加的结果
	 */
	protected Matrix plus(Matrix B) {
		checkMatrixSize(B);
		Matrix X = new Matrix(this.rows, this.cols);
		for (int r = 0; r < this.rows; r++) {
			for (int c = 0; c < this.cols; c++) {
				X.A[r][c] = A[r][c] + B.A[r][c];
			}
		}
		return X;
	}


	/**
	 * 矩阵中每个成员计算平方
	 * @return		
	 */
	protected Matrix elementSquare() {
		Matrix X = new Matrix(this.rows, this.cols);
		for (int r = 0; r < this.rows; r++) {
			for (int c = 0; c < this.cols; c++) {
				X.A[r][c] = A[r][c] * A[r][c];
			}
		}
		return X;
	}

	/**
	 * 矩阵中每个成员的总和
	 * @return		
	 */
	protected double elementSum() {
		double sumup = 0d;
		for (int r = 0; r < this.rows; r++) {
			for (int c = 0; c < this.cols; c++) {
				sumup += A[r][c];
			}
		}
		return sumup;
	}

	/**
	 * 用来计算向量的内积
	 * @return		
	 */
	protected double dot(Matrix B) {
		double innerProduct = 0d;
		for (int r = 0; r < this.rows; r++) {
			for (int c = 0; c < this.cols; c++) {
				innerProduct += A[r][c]*B.A[r][c];
			}
		}
		return innerProduct;
	}
	
	/**
	 * 矩阵相加，结果记录在本矩阵中，A = A + B
	 * @param B		用来执行加法的矩阵
	 * @return		矩阵相加的结果
	 */

	protected Matrix plusUp(Matrix B) {
		checkMatrixSize(B);
		for (int r = 0; r < this.rows; r++) {
			for (int c = 0; c < this.cols; c++) {
				A[r][c] = A[r][c] + B.A[r][c];
			}
		}
		return this;
	}

	/**
	 * 矩阵相加，C=A-B
	 * @param B		用来执行减法的矩阵
	 * @return		矩阵相减的结果
	 */
	protected Matrix minus(Matrix B) {
		checkMatrixSize(B);
		Matrix X = new Matrix(this.rows, this.cols);
		for (int r = 0; r < this.rows; r++) {
			for (int c = 0; c < this.cols; c++) {
				X.A[r][c] = A[r][c] - B.A[r][c];
			}
		}
		return X;
	}

	/**
	 * 矩阵加实数，C=A+d
	 * @param d		用来执行加法的实数
	 * @return		
	 */
	protected Matrix plus(double d) {
		Matrix X = new Matrix(this.rows, this.cols);
		for (int r = 0; r < this.rows; r++) {
			for (int c = 0; c < this.cols; c++) {
				X.A[r][c] = A[r][c] + d;
			}
		}
		return X;
	}

	/**
	 * 矩阵减实数，C=A-d
	 * @param d		用来执行减法的实数
	 * @return		
	 */
	protected Matrix minus(double d) {
		Matrix X = new Matrix(this.rows, this.cols);
		for (int r = 0; r < this.rows; r++) {
			for (int c = 0; c < this.cols; c++) {
				X.A[r][c] = A[r][c] - d;
			}
		}
		return X;
	}

	/**
	 * 矩阵乘实数，C=A*d
	 * @param d		用来执行加法的实数
	 * @return		
	 */
	protected Matrix times(double d) {
		Matrix X = new Matrix(this.rows, this.cols);
		for (int r = 0; r < this.rows; r++) {
			for (int c = 0; c < this.cols; c++) {
				X.A[r][c] = A[r][c] * d;
			}
		}
		return X;
	}

	/**
	 * 矩阵相减，结果记录在本矩阵中，A=A-B
	 * @param B		用来执行减法的矩阵
	 * @return		矩阵相减的结果
	 */
	protected Matrix minusEquals(Matrix B) {
		checkMatrixSize(B);
		for (int r = 0; r < this.rows; r++) {
			for (int c = 0; c < this.cols; c++) {
				A[r][c] = A[r][c] - B.A[r][c];
			}
		}
		return this;
	}

	/**
	 * 矩阵相乘
	 * @param B		用来相乘的矩阵
	 * @return		矩阵相乘的结果矩阵
	 * @exception	异常
	 */
	public Matrix times(Matrix B) {
		if (B.rows != this.cols) {
			throw new IllegalArgumentException("Matrix inner dimensions must agree.");
		}
		Matrix X = new Matrix(this.rows, B.cols);
		double[] BVectorCol = new double[this.cols];
		for (int c = 0; c < B.cols; c++) {
			for (int k = 0; k < this.cols; k++) {
				BVectorCol[k] = B.A[k][c];
			}
			for (int r = 0; r < this.rows; r++) {
				double[] AVectorRow = A[r];
				double s = 0;
				for (int k = 0; k < this.cols; k++) {
					s += AVectorRow[k] * BVectorCol[k];
				}
				X.A[r][c] = s;
			}
		}
		return X;
	}

	/**
	 * 矩阵除以常数
	 * @param d		除数
	 * @return		结果矩阵
	 * @exception	异常
	 */
	public Matrix divide(double d) {
		Matrix X = new Matrix(this.rows, this.cols);
		for (int c = 0; c < this.cols; c++) {
			for (int r = 0; r < this.rows; r++) {
				X.A[r][c] = this.A[r][c] / d;
			}
		}
		return X;
	}

	/**
	 * 生成单位矩阵
	 * @param rows		行数
	 * @param cols		列数
	 * @return			返回rows*cols矩阵，对角线上为1，其它为0
	 */
	protected static Matrix identity(int rows, int cols) {
		Matrix X = new Matrix(rows, cols);
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				X.A[r][c] = (r == c ? 1d : 0d);
			}
		}
		return X;
	}

	/**
	 * 生成单位方阵
	 * @param size		行数
	 * @return			返回size*size的矩阵，对角线上为1，其余为0
	 */
	protected static Matrix identity(int size) {
		return identity(size, size);
	}

	/**
	 * 求解A*X = B
	 * @param		矩阵参数B
	 * @return		方阵直接求解，其它情况返回最小二乘法解
	 */

	public Matrix solve(Matrix B) {
		//edited by bd, 2020.3.10, 矩阵运算中的问题只输出错误信息，不中断，返回null
		Matrix result = null;
		try {
			result = (this.rows == this.cols ?
					(new LUDecomposition(this)).solve(B)
					:(new QRDecomposition(this)).solve(B));
		}
		catch (Exception e) {
			Logger.warn(e.getMessage());
		}
		return result;
	}

	/**
	 * 复制二维数组A
	 * @return	复制的二维数组
	 */
	public double[][] getArrayCopy() {
		double[][] X = new double[this.rows][this.cols];
		for (int r = 0; r < this.rows; r++) {
			for (int c = 0; c < this.cols; c++) {
				X[r][c] = A[r][c];
			}
		}
		return X;
	}
	
	/**
	 * 求逆矩阵或者伪逆矩阵
	 * @return
	 */
	public Matrix inverse() {
		return solve(identity(this.rows));
	}
	
	/**
	 * 求伪逆矩阵
	 * @return
	 */
	public Matrix pseudoinverse() {
        //RealMatrix m = this.realMatrix();
        //SimpleMatrix m = new SimpleMatrix(this.A);
    	//SimpleMatrix sm = this.realMatrix().pseudoInverse();
    	//Matrix pinv = new Matrix(sm);
    	//return pinv;
		
		// 如果列满秩，直接用QR分解，否则转置后求伪逆矩阵再转置回来
		if (this.rows >= this.cols) {
			return (new QRDecomposition(this)).solve(identity(this.rows));
		}
		else {
			Matrix X = (new QRDecomposition(this.transpose())).solve(identity(this.cols));
			return X.transpose();
		}
	}
    
    /**
     * 创建SimpleMatrix
     * @return
     */
    public RealMatrix realMatrix() {
    	return new Array2DRowRealMatrix(this.A);
    }
	   
	/**
	 * 方阵求行列式值
	 * @return	行列式值
	 */
	public double det() {
		//edited by bd, 2020.3.10, 矩阵运算中的问题只输出错误信息，不中断，返回null
		if (this.rows != this.cols) {
			//throw new IllegalArgumentException("Matrix must be square.");
			Logger.warn("Matrix must be square.");
			return 0;
		}
		double det = 0;
		try {
			det = new LUDecomposition(this).det();
		}
		catch (Exception e) {
			Logger.warn(e.getMessage());
		}
		return det;
	}
	
	public static void main(String[] args) {
		Sequence seq = new Sequence(3);
		Sequence sub1 = new Sequence(3);
		sub1.add(1);
		sub1.add(1);
		sub1.add(1);
		seq.add(sub1);
		Sequence sub2 = new Sequence(3);
		sub2.add(0);
		sub2.add(4);
		sub2.add(-1);
		seq.add(sub2);
		Sequence sub3 = new Sequence(3);
		sub3.add(2);
		sub3.add(-2);
		sub3.add(1);
		seq.add(sub3);
		//seq.add(4);
		Matrix m = new Matrix(seq);
		Sequence seq2 = new Sequence(3);
		seq2.add(6);
		seq2.add(5);
		seq2.add(1);
		m.solve(new Matrix(seq2, true)).output();
		System.out.println("done");
	}
	
	protected void output() {
		for (int i = 0; i < rows; i++) {
			String s = "";
			for (int j = 0; j < cols; j++) {
				s += A[i][j]+"\t";
			}
			System.out.println(s);
		}
	}

	/**
	 * 返回矩阵的秩，用奇异值分解处理
	 * @return	矩阵的秩
	 */
	public int rank() {
	      return new SVDecomposition(this).rank();
	}
	
	/**
	 * 计算和另一个矩阵的标准差，A与B必须同维
	 * @param B		另一矩阵
	 * @return		标准方差
	 */
	public double mse(Matrix B) {
		//edited by bd, 2020.3.10, 矩阵运算中的问题只输出错误信息，不中断，返回null
		try {
			Matrix X = this.minus(B);
			double result = 0;
			for (int r = 0; r < this.rows; r++) {
				for (int c = 0; c < this.cols; c++) {
					result += Math.pow(X.get(r, c), 2);
				}
			}
			return result / this.cols / this.rows;
		}
		catch (Exception e) {
			Logger.warn(e.getMessage());
		}
		return 0d;
	}
	
	/**
	 * 计算和另一个矩阵的绝对误差，A与B必须同维
	 * @param B		另一矩阵
	 * @return		绝对误差
	 */
	public double mae(Matrix B) {
		//edited by bd, 2020.3.10, 矩阵运算中的问题只输出错误信息，不中断，返回null
		try {
			Matrix X = this.minus(B);
			double result = 0;
			for (int r = 0; r < this.rows; r++) {
				for (int c = 0; c < this.cols; c++) {
					result += Math.abs(X.get(r, c));
				}
			}
			return result / this.cols / this.rows;
		}
		catch (Exception e) {
			Logger.warn(e.getMessage());
		}
		return 0d;
	}
	
	/**
	 * 是否是向量，向量存储为单行或单列矩阵
	 * @return
	 */
	public boolean ifVector() {
		return this.ifVector;
	}

	/**
	 * 生成新矩阵，使得新矩阵每一列的均值为0
	 * @param matrix	源矩阵
	 * @return
	 */
    public Matrix changeAverageToZero() {
        double[] sum = new double[this.cols];
        double[] average = new double[this.cols];
        double[][] averageArray = new double[this.rows][this.cols];
        for (int c = 0; c < this.cols; c++) {
            for (int r = 0; r < this.rows; r++) {
                sum[c] += this.get(r, c);
            }
            average[c] = sum[c] / this.rows;
        }
        for (int c = 0; c < this.cols; c++) {
            for (int r = 0; r < this.rows; r++) {
                averageArray[r][c] = this.get(r, c) - average[c];
            }
        }
        return new Matrix(averageArray);
    }

	/**
	 * 生成新矩阵，使得新矩阵每一列的均值为0
	 * @param matrix	源矩阵
	 * @return
	 */
    public Matrix changeAverageToZero(Vector averageV) {
        double[] average = averageV.getValue();
        double[][] averageArray = new double[this.rows][this.cols];
        for (int c = 0; c < this.cols; c++) {
            for (int r = 0; r < this.rows; r++) {
                averageArray[r][c] = this.get(r, c) - average[c];
            }
        }
        return new Matrix(averageArray);
    }
    
    /**
     * 获取每列的均值向量
     * @param primary	
     * @return
     */
    public Vector getAverage() {
        // 均值中心化后的矩阵
        double[] sum = new double[this.cols];
        double[] average = new double[this.cols];
        for (int c = 0; c < this.cols; c++) {
            for (int r = 0; r < this.rows; r++) {
                sum[c] += this.get(r, c);
            }
            average[c] = sum[c] / this.rows;
        }

        return new Vector(average);
    }
}
