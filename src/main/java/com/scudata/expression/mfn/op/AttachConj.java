package com.scudata.expression.mfn.op;

import com.scudata.cellset.ICellSet;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.op.Conj;
import com.scudata.expression.Expression;
import com.scudata.expression.OperableFunction;
import com.scudata.expression.ParamParser;
import com.scudata.resources.EngineMessage;

// 成员的和列
/**
 * 对游标或管道附加序列和列运算
 * op.conj() op.conj(x)，op是序列的游标或管道
 * @author RunQian
 *
 */
public class AttachConj extends OperableFunction {
	/**
	 * 设置函数参数
	 * @param cs 网格对象
	 * @param ctx 计算上下文
	 * @param param 函数参数字符串
	 */
	public void setParameter(ICellSet cs, Context ctx, String param) {
		strParam = param;
		this.cs = cs;
		
		// A.conj(x,…)把参数当成一个整体创建成逗号表达式
		this.param = ParamParser.newLeafParam(param, cs, ctx);
		if (next != null) {
			next.setParameter(cs, ctx, param);
		}
	}
	
	public Object calculate(Context ctx) {
		if (param == null) {
			Conj op = new Conj(this, null);
			if (cs != null) {
				op.setCurrentCell(cs.getCurrent());
			}
			
			return operable.addOperation(op, ctx);
		} else if (param.isLeaf()) {
			Expression exp = param.getLeafExpression();
			Conj op = new Conj(this, exp);
			if (cs != null) {
				op.setCurrentCell(cs.getCurrent());
			}
			
			return operable.addOperation(op, ctx);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("conj" + mm.getMessage("function.invalidParam"));
		}
	}
}
