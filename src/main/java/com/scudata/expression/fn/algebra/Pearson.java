package com.scudata.expression.fn.algebra;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * 计算皮尔逊系数pearson(A,B)，B省略时用to(A.len())，支持@r和@a选项
 * @author bd, 2021.1.19
 *
 */
public class Pearson extends Function {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pearson" + mm.getMessage("function.missingParam"));
		}
	}
	
	public Object calculate(Context ctx) {
		//先判断有没有分号后的参数
		int setn = 0;
		IParam param = this.param;
		if (param.getType() == IParam.Semicolon) {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pearson" + mm.getMessage("function.invalidParam"));
			}

			IParam locParam = param.getSub(1);
			if (locParam != null) {
				Object obj = locParam.getLeafExpression().calculate(ctx);
				if (obj != null && !(obj instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("pearson" + mm.getMessage("function.paramTypeError"));
				}

				setn = ((Number) obj).intValue();
			}
			
			param = param.getSub(0);
			if (param == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pearson" + mm.getMessage("function.missingParam"));
			}
		}
		
		Object o1 = null, o2 = null;
		if (param.isLeaf()) {
			o1 = param.getLeafExpression().calculate(ctx);
			if (o1 instanceof Sequence) {
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pearson" + mm.getMessage("function.paramTypeError"));
			}
		} else {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pearson" + mm.getMessage("function.invalidParam"));
			}

			IParam sub1 = param.getSub(0);
			IParam sub2 = param.getSub(1);
			if (sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pearson" + mm.getMessage("function.invalidParam"));
			}
			o1 = sub1.getLeafExpression().calculate(ctx);
			o2 = sub2 == null ? null : sub2.getLeafExpression().calculate(ctx);
		}
		if (o2 == null) {
			int len = o1 instanceof Sequence ? ((Sequence) o1).length() : 1;
			o2 = new Sequence(1, len);
		}
		if (o1 instanceof Sequence && o2 instanceof Sequence) {
			boolean ifR2 = false;
			boolean adjust = false;
			if (option != null) {
				if (option.indexOf('r') > -1) {
					ifR2 = true;
				}
				if (option.indexOf('a') > -1) {
					adjust = true;
				}
			}
			Vector v1 = new Vector((Sequence) o1);
			Vector v2 = new Vector((Sequence) o2);
			int n = ((Sequence) o1).length();
			if (adjust && setn > 0) {
				n = setn;
			}
			if (ifR2) {
				//@r选项
				v1 = Normalize.normalize(v1);
				v2 = Normalize.normalize(v2);
			}
			return Double.valueOf(pearson(v1, v2, (double) n));
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pearson" + mm.getMessage("function.paramTypeError"));
		}
	}
	
	protected static double pearson(Vector vx, Vector vy, double n) {
		double[] x = vx.getValue();
		double[] y = vy.getValue();
		double X = 0;
		double Y = 0;
		double XX = 0;
		double YY = 0;
		double XY = 0;
		for (int i = 0; i < n; i++)  {
			double xcur = x[i];
			double ycur = y[i];
			X += xcur;
			Y += ycur;
			XX += xcur * xcur;
			YY += ycur * ycur;
			XY += xcur * ycur;
		}
		return (n*XY-X*Y)/Math.sqrt(n*XX-X*X)/Math.sqrt(n*YY-Y*Y);
	}
	
	public static double pearson(Sequence x, Sequence y, double n) {
		double X = 0;
		double Y = 0;
		double XX = 0;
		double YY = 0;
		double XY = 0;
		for (int i = 1; i <= n; i++)  {
			Object xo = x.get(i);
			Object yo = y.get(i);
			double xcur = xo instanceof Number ? ((Number) xo).doubleValue() : 0;
			double ycur = yo instanceof Number ? ((Number) yo).doubleValue() : 0;
			X += xcur;
			Y += ycur;
			XX += xcur * xcur;
			YY += ycur * ycur;
			XY += xcur * ycur;
		}
		return (n*XY-X*Y)/Math.sqrt(n*XX-X*X)/Math.sqrt(n*YY-Y*Y);
	}
}
