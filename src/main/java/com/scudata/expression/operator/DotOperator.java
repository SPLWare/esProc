package com.scudata.expression.operator;

import com.scudata.array.BoolArray;
import com.scudata.array.IArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.expression.Move;
import com.scudata.expression.Node;
import com.scudata.expression.Operator;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

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
	
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (left == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\".\"" + mm.getMessage("operator.missingLeftOperation"));
		} else if (right == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\".\"" + mm.getMessage("operator.missingRightOperation"));
		}
		
		left.checkValidity();
		right.checkValidity();
	}
	
	public Node optimize(Context ctx) {
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
				Object result = right.calculate(ctx);
				right.releaseDotLeftObject();
				return result;
			} else {
				right = right.getNextFunction();
			}
		}
		
		String fnName;
		if (this.right instanceof Function) {
			fnName = ((Function)this.right).getFunctionName();
		} else {
			fnName = "";
		}
		
		MessageManager mm = EngineMessage.get();
		throw new RQException(mm.getMessage("dot.leftTypeError", Variant.getDataType(leftValue), fnName));
	}
	
	public Object assign(Object value, Context ctx) {
		Object leftValue = getLeftObject(ctx);
		if (leftValue == null) {
			return null;
		}

		Node right = this.right;
		while (right != null) {
			if (right.isLeftTypeMatch(leftValue)) {
				right.setDotLeftObject(leftValue);
				return right.assign(value, ctx);
			} else {
				right = right.getNextFunction();
			}
		}
		
		String fnName;
		if (this.right instanceof Function) {
			fnName = ((Function)this.right).getFunctionName();
		} else {
			fnName = "";
		}
		
		MessageManager mm = EngineMessage.get();
		throw new RQException(mm.getMessage("dot.leftTypeError", Variant.getDataType(leftValue), fnName));
	}

	public Object addAssign(Object value, Context ctx) {
		Object leftValue = getLeftObject(ctx);
		if (leftValue == null) {
			return null;
		}

		Node right = this.right;
		while (right != null) {
			if (right.isLeftTypeMatch(leftValue)) {
				right.setDotLeftObject(leftValue);
				return right.addAssign(value, ctx);
			} else {
				right = right.getNextFunction();
			}
		}
		
		String fnName;
		if (this.right instanceof Function) {
			fnName = ((Function)this.right).getFunctionName();
		} else {
			fnName = "";
		}
		
		MessageManager mm = EngineMessage.get();
		throw new RQException(mm.getMessage("dot.leftTypeError", Variant.getDataType(leftValue), fnName));
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
	
	/**
	 * 计算出所有行的结果
	 * @param ctx 计算上行文
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx) {
		right.setLeft(left);
		return right.calculateAll(ctx);
		//return right.calculateAll(left, ctx);
	}
	
	/**
	 * 计算signArray中取值为sign的行
	 * @param ctx
	 * @param signArray 行标识数组
	 * @param sign 标识
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx, IArray signArray, boolean sign) {
		right.setLeft(left);
		return right.calculateAll(ctx, signArray, sign);
	}
	
	/**
	 * 计算逻辑与运算符&&的右侧表达式
	 * @param ctx 计算上行文
	 * @param leftResult &&左侧表达式的计算结果
	 * @return BoolArray
	 */
	public BoolArray calculateAnd(Context ctx, IArray leftResult) {
		right.setLeft(left);
		return right.calculateAnd(ctx, leftResult);

		/*BoolArray result = leftResult.isTrue();
		int size = result.size();
		Current current = ctx.getComputeStack().getTopCurrent();
		
		for (int i = 1; i <= size; ++i) {
			if (result.isTrue(i)) {
				current.setCurrent(i);
				Object value = calculate(ctx);
				if (Variant.isFalse(value)) {
					result.set(i, false);
				}
			}
		}
		
		return result;*/
	}
	
	/**
	 * 判断给定的值域范围是否满足当前条件表达式
	 * @param ctx 计算上行文
	 * @return 取值参照Relation. -1：值域范围内没有满足条件的值，0：值域范围内有满足条件的值，1：值域范围的值都满足条件
	 */
	public int isValueRangeMatch(Context ctx) {
		right.setLeft(left);
		return right.isValueRangeMatch(ctx);
		/*if (right instanceof MemberFunction) {
			IArray array = left.calculateRange(ctx);
			if (array instanceof ConstArray) {
				Object obj = array.get(1);
				return ((MemberFunction)right).isValueRangeMatch(obj, ctx);
			} else {
				return Relation.PARTICALMATCH;
			}
		} else {
			return Relation.PARTICALMATCH;
		}*/
	}
	
	/**
	 * 计算表达式的取值范围
	 * @param ctx 计算上行文
	 * @return
	 */
	public IArray calculateRange(Context ctx) {
		right.setLeft(left);
		return right.calculateRange(ctx);
	}
	
	/**
	 * 返回节点是否单调递增的
	 * @return true：是单调递增的，false：不是
	 */
	public boolean isMonotone() {
		return left.isMonotone() && right.isMonotone();
	}
}
