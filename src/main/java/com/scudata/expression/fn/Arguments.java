package com.scudata.expression.fn;

import com.scudata.array.ConstArray;
import com.scudata.array.IArray;
import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.dm.Context;
import com.scudata.dm.ParamList;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;

/**
 * 返回当前网格的参数名列表
 * arguments()
 * @author RunQian
 *
 */
public class Arguments extends Function {
	public Object calculate(Context ctx) {
		if (cs instanceof PgmCellSet) {
			ParamList paramList = ((PgmCellSet)cs).getParamList();
			if (paramList != null && paramList.count() > 0) {
				int count = paramList.count();
				Sequence seq = new Sequence(count);
				
				for (int i = 0; i < count; ++i) {
					String name = paramList.get(i).getName();
					seq.add(name);
				}
				
				return seq;
			}
		}
		
		return new Sequence(0);
	}

	/**
	 * 计算出所有行的结果
	 * @param ctx 计算上行文
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx) {
		Sequence sequence = ctx.getComputeStack().getTopSequence();
		Object value = calculate(ctx);
		return new ConstArray(value, sequence.length());
	}
	
	/**
	 * 计算signArray中取值为sign的行
	 * @param ctx
	 * @param signArray 行标识数组
	 * @param sign 标识
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx, IArray signArray, boolean sign) {
		Sequence sequence = ctx.getComputeStack().getTopSequence();
		Object value = calculate(ctx);
		return new ConstArray(value, sequence.length());
	}
}
