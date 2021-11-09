package com.raqsoft.expression.operator;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.expression.Move;
import com.raqsoft.expression.Node;
import com.raqsoft.expression.Operator;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.util.Variant;

/**
 * 点运算符：.
 * A.f()
 * @author WangXiaoJun
 *
 */
public class DotOperator extends Operator {
	public DotOperator() {
		priority = PRI_SUF;
	}
	
	public Node optimize(Context ctx) {
		if (left == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\".\"" + mm.getMessage("operator.missingleftOperation"));
		}
		
		if (right == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\".\"" + mm.getMessage("operator.missingRightOperation"));
		}
		
		// 如果右侧函数不会修改左侧对象的值并且左侧对象是序列常量则可以先产生序列
		// 比如[1,2,3].contain(n)
		if (!right.ifModifySequence()) {
			left = left.optimize(ctx, true);
			right = right.optimize(ctx);
			return this;
		} else {
			return super.optimize(ctx);
		}
	}

	/**
	 * 计算左侧对象的值
	 * @param ctx 计算上下文
	 * @return 左侧对象
	 */
	private Object getLeftObject(Context ctx) {
		Object obj = left.calculate(ctx);
		
		// n.f()当f是序列函数时把n解释成to(n)
		if (obj instanceof Number && right.isSequenceFunction()) {
			int n = ((Number)obj).intValue();
			if (n > 0) {
				return new Sequence(1, n);
			} else {
				return new Sequence(0);
			}
		} else {
			return obj;
		}
	}

	public Object calculate(Context ctx) {
		Object leftValue = getLeftObject(ctx);
		if (leftValue == null) {
			return null;
		}

		Node right = this.right;
		while (right != null) {
			if (right.isLeftTypeMatch(leftValue)) {
				right.setDotLeftObject(leftValue);
				return right.calculate(ctx);
			} else {
				right = right.getNextFunction();
			}
		}
		
		MessageManager mm = EngineMessage.get();
		throw new RQException(mm.getMessage("dot.leftTypeError", Variant.getDataType(leftValue)));
	}

	public Object assign(Object value, Context ctx) {
		Object result1 = getLeftObject(ctx);
		if (result1 == null) {
			return value;
		}
		
		right.setDotLeftObject(result1);
		return right.assign(value, ctx);
	}

	public Object addAssign(Object value, Context ctx) {
		Object result1 = getLeftObject(ctx);
		if (result1 == null) {
			return null;
		}
		
		right.setDotLeftObject(result1);
		return right.addAssign(value, ctx);
	}

	public Object move(Move node, Context ctx) {
		Object result1 = getLeftObject(ctx);
		if (result1 == null) {
			return null;
		}
		
		right.setDotLeftObject(result1);
		return right.move(node, ctx);
	}

	public Object moveAssign(Move node, Object value, Context ctx) {
		Object result1 = getLeftObject(ctx);
		if (result1 == null) {
			return value;
		}
		
		right.setDotLeftObject(result1);
		return right.moveAssign(node, value, ctx);
	}
	
	public Object moves(Move node, Context ctx) {
		Object result1 = getLeftObject(ctx);
		if (result1 == null) {
			return null;
		}
		
		right.setDotLeftObject(result1);
		return right.moves(node, ctx);
	}
}
