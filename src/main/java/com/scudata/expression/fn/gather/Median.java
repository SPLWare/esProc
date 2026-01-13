package com.scudata.expression.fn.gather;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Expression;
import com.scudata.expression.Gather;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * 聚合函数Median类
 * 
 * @author 于志华
 *
 */
public class Median extends Gather  {
	int	parK = 0;	// 选择哪个分段。若为0表达式该值为空
	int	parN = 0;	// 分多少段，若为0表示该值为空。
	private Expression exp;	// 计算表达式
	
	/**
	 * 根据median(k:n, exp)分别解析k、n、exp
	 */
	public void prepare(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("median" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			parK	= 1;
			parN	= 2;
			exp = param.getLeafExpression();
			return;
		} else if (param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("median" + mm.getMessage("function.invalidParam"));
		}
		
		IParam sub0 = param.getSub(0);
		IParam sub1 = param.getSub(1);
		exp = sub1.getLeafExpression();
		
		// median k、n参数允许均为空，此时取中值。
		if (null == sub0) {
			parK	= 1;
			parN	= 2;
			return;
		}
		
		sub1 = sub0.getSub(1);
		sub0 = sub0.getSub(0);
		
		try {
			Expression data;
			Object obj;
			if (null == sub0)
				parK = 0;
			else {
				data = sub0.getLeafExpression();
				obj = data.calculate(null);
				parK = (Integer)obj;
			}
			
			if (null == sub1)
				parN = 0;
			else {
				data = sub1.getLeafExpression();
				obj = data.calculate(null);
				parN = (Integer)obj;
			}
			
			if (parN < 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("median" + mm.getMessage("function.invalidParam"));
			}
			
			if (parK > parN) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("median" + mm.getMessage("function.invalidParam"));
			}
			
		} catch(Exception e) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("median" + mm.getMessage("function.invalidParam"));
		}
	}

	/**
	 * 把一条记录添加到meidan中间数据或把其它临时数据整合到median。
	 * 		被整合数据包括序列数据和非序列数据。
	 * 		序列数据需要把序列拆分后，合并。（若拆分后的数据还包含序列，不做进一步拆分）
	 * 		非序列数据直接合并。
	 * @param	oldValue	以前的中间数据
	 * @param	ctx	上下文变量，提供计算中间数据的上下文
	 */
	public Object gather(Object oldValue, Context ctx) {
		Object val = exp.calculate(ctx);
		if (val == null)
			return oldValue;
		
		if(oldValue == null) {
			if (val instanceof Sequence) {
				return val;
			} else {
				Sequence seq = new Sequence();
				seq.add(val);
				return seq;
			}
		} else {
			if (val instanceof Sequence) {
				((Sequence)oldValue).addAll((Sequence)val);
			} else {
				((Sequence)oldValue).add(val);
			}
			
			return oldValue;
		}

	}

	/**
	 * 把计算后的数据合并为内存的中间数据
	 * 		
	 */
	public Object gather(Context ctx) {
		Object val = exp.calculate(ctx);

		if (val instanceof Sequence) {
			return val;
		} else {
			Sequence seq = new Sequence();
			seq.add(val);
			return seq;
		}
	}

	/**
	 * 根据median内的参数，还原表达式字符串。
	 * @param	q	还原时，对应的排序列。
	 */
	public Expression getRegatherExpression(int q) {
		String str = "median("+parK+":"+parN+",#"+ + q + ")";
		return new Expression(str);

	}

	public Object calculate(Context ctx) {
		MessageManager mm = EngineMessage.get();
		throw new RQException(mm.getMessage("Expression.unknownFunction") + "median");
	}

	/**
	 * 是否需要最后的统计操作
	 */
	public boolean needFinish() {
		return true;
	}
	
	/**
	 * 统计临时中间数据，生成最终结果。
	 */
	public Object finish(Object val) {
		if (val == null || !(val instanceof Sequence)) {
			return val;
		}
	
		Sequence seq = ((Sequence)val).sort(null);
		return seq.median(1, seq.length(), parK, parN);	
	}
	
	public int getParK() {
		return parK;
	}

	public int getParN() {
		return parN;
	}

	public Expression getExp() {
		return exp;
	}
}
