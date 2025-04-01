package com.scudata.expression;

import com.scudata.cellset.ICellSet;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dm.op.Calculate;
import com.scudata.dm.op.Operable;
import com.scudata.resources.EngineMessage;

/**
 * 循环序列执行表达式，返回运算结果
 * A.(x)
 * @author WangXiaoJun
 *
 */
public class Calc extends MemberFunction {
	private Object srcObj;
	
	/**
	 * 设置函数参数
	 * @param cs 网格对象
	 * @param ctx 计算上下文
	 * @param param 函数参数字符串
	 */
	public void setParameter(ICellSet cs, Context ctx, String param) {
		strParam = param;
		this.cs = cs;
		
		// A.(x,…)把参数当成一个整体创建成逗号表达式
		this.param = ParamParser.newLeafParam(param, cs, ctx);
		//this.param = ParamParser.parse(param, cs, ctx);
	}
	
	public boolean isLeftTypeMatch(Object obj) {
		return true;
	}

	public void setDotLeftObject(Object obj) {
		srcObj = obj;
	}
	
	/**
	 * 释放节点引用的点操作符左侧的对象
	 */
	public void releaseDotLeftObject() {
		srcObj = null;
	}
	
	/**
	 * 判断当前节点是否是序列函数
	 * 如果点操作符的右侧节点是序列函数，左侧节点计算出数，则需要把数转成数列
	 * @return
	 */
	public boolean isSequenceFunction() {
		return true;
	}
	
	public Object calculate(Context ctx) {
		if (param == null) {
			return srcObj;
		} else if (param.isLeaf()) {
			Expression exp = param.getLeafExpression();
			if (srcObj instanceof Sequence) {
				return ((Sequence)srcObj).calc(exp, option, ctx);
			} else if (srcObj instanceof BaseRecord) {
				return ((BaseRecord)srcObj).calc(exp, ctx);
			} else if (srcObj instanceof Operable) {
				Calculate calculate = new Calculate(this, exp);
				((Operable)srcObj).addOperation(calculate, ctx);
				return srcObj;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("\".\"" + mm.getMessage("dot.s2rLeft"));
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("()" + mm.getMessage("function.invalidParam"));
		}
	}

	public Object getSrcObj() {
		return srcObj;
	}
}
