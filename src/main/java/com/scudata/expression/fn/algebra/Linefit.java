package com.scudata.expression.fn.algebra;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.ParamParser;
import com.scudata.resources.EngineMessage;

/**
 * 矩阵线性拟合，这个函数其实相当于解方程AX=Y，其中A为系数矩阵，Y为常数矩阵
 * 当A为m*m矩阵且满秩时，方程有唯一解；当A为m*n矩阵，m>n且列满秩时，用最小二乘法做线性拟合解
 * @author bd
 */
public class Linefit extends Function {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("linefit" + mm.getMessage("function.missingParam"));
		} else if (param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("linefit" + mm.getMessage("function.invalidParam"));
		}
	}
	
	public Object calculate(Context ctx) {
		IParam sub1 = param.getSub(0);
		IParam sub2 = param.getSub(1);
		if (sub1 == null || sub2 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("linefit" + mm.getMessage("function.invalidParam"));
		}
		
		Object o1 = sub1.getLeafExpression().calculate(ctx);
		Object o2 = sub2.getLeafExpression().calculate(ctx);
		boolean ifVector  = option != null && option.indexOf('1') > -1;
		if (o1 instanceof Sequence && o2 instanceof Sequence) {
			Matrix A = new Matrix((Sequence)o1);
			Matrix B = new Matrix((Sequence)o2);
			boolean oneline = B.getRows() == 1;
			if (oneline) {
				B = B.transpose();
			}
			else if (ifVector){
				if (!(((Sequence) o2).get(1) instanceof Sequence)) {
					oneline = true;
				}
			}
			
			if (A.getCols() == 0 || A.getRows() == 0) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("linefit" + mm.getMessage("function.paramTypeError"));
			}
			else if (B.getCols() == 0 || B.getRows() == 0) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("linefit" + mm.getMessage("function.paramTypeError"));
			}
			Matrix X = A.solve(B);
			if (X == null) {
				return null;
			}
			if (oneline) {
				double[][] vs = X.getArray();
				int rows = vs.length;
				if (rows > 0) {
					int cols = vs[0].length;
					if (cols == 1) {
						Sequence result = new Sequence(rows);
						for (int i = 0; i < rows; i++) {
							result.add(getValue(vs[i][0]));
						}
						return result;
					}
					else if (rows == 1) {
						Sequence result = new Sequence(cols);
						for (int i = 0; i < cols; i++) {
							result.add(getValue(vs[0][i]));
						}
						return result;
					}
				}
			}
			return X.toSequence(option, false);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("linefit" + mm.getMessage("function.paramTypeError"));
		}
	}
	
    public static void main(String[] args) {
    	Linefit func = new Linefit();
        double[][] h_A = {{1,2,3,4},{2,3,1,2},{1,1,1,-1},{1,0,-2,-6}};
        double[] h_B = {4,6,2,4};
        Sequence A = toSeq(h_A);
        Sequence B = toSeq(h_B, h_B.length);
		Context ctx = new Context();
        ctx.setParamValue("A", A);
        ctx.setParamValue("B", B);
        func.option = "1";

    	IParam params = ParamParser.parse("A,B", null, ctx);
        func.param = params;
        Object res = func.calculate(ctx);
        print(((Sequence)res), 10);
    }
	
	protected static Sequence toSeq(double[][] C) {
		int rows = C.length;
		Sequence seq = new Sequence(rows);
		for (int i = 0; i < rows; i++) {
			double[] sub = C[i];
			int cols = sub.length;
			seq.add(toSeq(sub, cols));
		}
		return seq;
	}
	
	protected static Sequence toSeq(double[] C, int size) {
		Sequence seq = new Sequence(size);
		for (int i = 0; i < size; i++) {
			seq.add(C[i]);
		}
		return seq;
	}
	
    protected static void print(Sequence seq, int n) {
    	int len = seq.length();
    	if (len > n) len = n;
		for (int c = 1; c<=len; c++) {
			Object o = seq.get(c);
			if (o instanceof Sequence) {
				print((Sequence) o);
			}
			else if (o instanceof Number) {
				System.out.printf("%2.6f ", ((Number)o).doubleValue() );
				System.out.print("  ");
			}
			else if (o != null) {
				System.out.print(o.toString());
			}
			else {
				System.out.printf("%2.6f ", "");
				System.out.print("  ");
			}
		}
		System.out.println();
    }
    
    protected static void print(Sequence seq) {
    	int len = seq.length();
		for (int c = 1; c<=len; c++) {
			Object o = seq.get(c);
			if (o instanceof Sequence) {
				print((Sequence) o);
			}
			else if (o instanceof Number) {
				System.out.printf("%2.6f ", ((Number)o).doubleValue() );
				System.out.print("  ");
			}
			else if (o != null) {
				System.out.print(o.toString());
			}
			else {
				System.out.printf("%2.6f ", "");
				System.out.print("  ");
			}
		}
		System.out.println();
    }
	private final static double scale = 1000000d;
	private final static double range = 1e-14;
	protected static double getValue(double d) {
		// added by bd, 2022.5.1, 如果绝对值太小，则保留原值
		double abs = Math.abs(d);
		double scale1 = scale;
		if (abs < range) {
			return d;
		}
		else if (abs < 1) {
			scale1 *= Math.pow(10, (int) Math.round((Math.log10(1/abs))));
		}
		double d1 = d*scale1;
		if (d1 > Long.MIN_VALUE && d1 < Long.MAX_VALUE) {
			d = Math.round(d1)/scale1;
		}
		return d; 
	}
}
