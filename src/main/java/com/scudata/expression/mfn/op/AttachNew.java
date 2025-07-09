package com.scudata.expression.mfn.op;

import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.OperableFunction;
import com.scudata.expression.ParamInfo2;

/**
 * 对游标或管道附加创建新序表运算;
 * op.new(xi:Fi,…) op是游标或管道
 * @author RunQian
 *
 */
public class AttachNew extends OperableFunction {
	public Object calculate(Context ctx) {
		ParamInfo2 pi = ParamInfo2.parse(param, "new", false, false);
		Expression []exps = pi.getExpressions1();
		String []names = pi.getExpressionStrs2();

		//New op = new New(this, exps, names, option);
		//if (cs != null) {
		//	op.setCurrentCell(cs.getCurrent());
		//}
		
		//return operable.addOperation(op, ctx);
		return operable.newTable(this, exps, names, option, ctx);
	}
	
	/**
	 * 提取new、news的第一个参数 (A/cs:K)
	 * @param param
	 * @param ctx
	 * @return Object[] {A/cs对象, K数组}
	 */
	public static Object[] parse1stParam(IParam param, Context ctx) {
		Object[] objs = new Object[3];
		Object obj = null;
		String[] csNames = null;
		IParam csParam = param.getSub(0);
		if (csParam.isLeaf()) {
			obj = csParam.getLeafExpression().calculate(ctx);
		} else {
			obj = csParam.getSub(0).getLeafExpression().calculate(ctx);
			IParam newParam = csParam.create(1, csParam.getSubSize());
			ParamInfo2 pi = ParamInfo2.parse(newParam, "new", false, false);
			csNames = pi.getExpressionStrs1();
		}
		
		objs[0] = obj;
		objs[1] = csNames;
		return objs;
	}
}
