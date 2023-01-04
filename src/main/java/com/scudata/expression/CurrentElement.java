package com.scudata.expression;

import com.scudata.array.BoolArray;
import com.scudata.array.ConstArray;
import com.scudata.array.IArray;
import com.scudata.array.ObjectArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.ComputeStack;
import com.scudata.dm.Context;
import com.scudata.dm.Current;
import com.scudata.dm.IComputeItem;
import com.scudata.dm.Sequence;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 用于取序列的当前元素
 * r.(~) A.(~)  A.(A.~)
 * @author WangXiaoJun
 *
 */
public class CurrentElement extends Node {
	private Sequence sequence;
	private Node left; // 点操作符的左侧节点	

	/**
	 * 取节点的左侧节点，没有返回空
	 * @return Node
	 */
	public Node getLeft() {
		return left;
	}

	/**
	 * 设置节点的左侧节点
	 * @param node 节点
	 */
	public void setLeft(Node node) {
		left = node;
	}

	public void setDotLeftObject(Object obj) {
		if (!(obj instanceof Sequence)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\".\"" + mm.getMessage("dot.seriesLeft"));
		}

		sequence = (Sequence)obj;
	}

	public Object calculate(Context ctx) {
		ComputeStack stack = ctx.getComputeStack();
		if (sequence == null) { // ~
			return stack.getTopObject().getCurrent();
		} else { // A.~
			return stack.getCurrentValue(sequence);
		}
	}
	
	/**
	 * 计算signArray中取值为sign的行
	 * @param ctx
	 * @param signArray 行标识数组
	 * @param sign 标识
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx, IArray signArray, boolean sign) {
		return calculateAll(ctx);
	}
	
	/**
	 * 计算逻辑与运算符&&的右侧表达式
	 * @param ctx 计算上行文
	 * @param leftResult &&左侧表达式的计算结果
	 * @return BoolArray
	 */
	public BoolArray calculateAnd(Context ctx, IArray leftResult) {
		BoolArray result = leftResult.isTrue();
		IArray array = calculateAll(ctx);
		
		for (int i = 1, size = result.size(); i <= size; ++i) {
			if (result.isTrue(i) && array.isFalse(i)) {
				result.set(i, false);
			}
		}
		
		return result;
	}
	
	/**
	 * 计算出所有行的结果
	 * @param ctx 计算上行文
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx) {
		if (left == null) {
			ComputeStack stack = ctx.getComputeStack();
			IComputeItem item = stack.getTopObject();
			Sequence sequence = item.getCurrentSequence();
			
			if (sequence != null) {
				return sequence.getMems();
			} else {
				sequence = stack.getTopSequence();
				Object value = item.getCurrent();
				return new ConstArray(value, sequence.length());
			}
		}

		ComputeStack stack = ctx.getComputeStack();
		Sequence topSequence = stack.getTopSequence();
		IArray leftArray = left.calculateAll(ctx);
		
		if (leftArray instanceof ConstArray) {
			Object leftValue = ((ConstArray)leftArray).getData();
			if (!(leftValue instanceof Sequence)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("\".\"" + mm.getMessage("dot.seriesLeft"));
			}
			
			Sequence sequence = (Sequence)leftValue;			
			if (topSequence == sequence) {
				return sequence.getMems();
			} else {
				// A.(B.(A.~))
				Object value = stack.getCurrentValue(sequence);
				return new ConstArray(value, topSequence.length());
			}
		} else {
			int len = topSequence.length();
			ObjectArray result = new ObjectArray(len);
			result.setTemporary(true);
			for (int i = 1; i <= len; ++i) {
				Object leftValue = leftArray.get(i);
				if (!(leftValue instanceof Sequence)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("\".\"" + mm.getMessage("dot.seriesLeft"));
				}
				
				Object cur = stack.getCurrentValue((Sequence)leftValue);
				result.push(cur);
			}
			
			return result;
		}
	}
	
	public Object assign(Object value, Context ctx) {
		ComputeStack stack = ctx.getComputeStack();
		if (sequence == null) { // ~
			IComputeItem temp = stack.getTopObject();
			if (temp instanceof Current) {
				((Current)temp).assign(value);
				return value;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("Expression.unknownExpression") + "~");
			}
		} else { // A.~
			Current current = stack.getSequenceCurrent(sequence);
			if (current == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("~" + mm.getMessage("engine.seriesNotInStack"));
			}

			current.assign(value);
			return value;
		}
	}
	
	public Object addAssign(Object value, Context ctx) {
		ComputeStack stack = ctx.getComputeStack();
		if (sequence == null) { // ~
			IComputeItem temp = stack.getTopObject();
			if (temp instanceof Current) {
				Object result = Variant.add(((Current)temp).getCurrent(), value);
				((Current)temp).assign(result);
				return result;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("Expression.unknownExpression") + "~");
			}
		} else { // A.~
			Current current = stack.getSequenceCurrent(sequence);
			if (current == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("~" + mm.getMessage("engine.seriesNotInStack"));
			}

			Object result = Variant.add(current.getCurrent(), value);
			current.assign(result);
			return result;
		}
	}

	public Object move(Move node, Context ctx) {
		ComputeStack stack = ctx.getComputeStack();
		if (sequence == null) { // ~
			IComputeItem temp = stack.getTopObject();
			if (temp instanceof Current) {
				Current current = (Current)temp;
				int pos = node.calculateIndex(current, ctx);
				return pos > 0 ? current.get(pos) : null;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("Expression.unknownExpression") + "~");
			}
		} else { // A.~
			Current current = stack.getSequenceCurrent(sequence);
			if (current == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("\"[]\"" + mm.getMessage("engine.seriesNotInStack"));
			}

			int pos = node.calculateIndex(current, ctx);
			return pos > 0 ? current.get(pos) : null;
		}
	}

	public Object moveAssign(Move node, Object value, Context ctx) {
		ComputeStack stack = ctx.getComputeStack();
		if (sequence == null) { // ~
			IComputeItem temp = stack.getTopObject();
			if (temp instanceof Current) {
				Current current = (Current)temp;
				int pos = node.calculateIndex(current, ctx);
				if (pos > 0) current.assign(pos, value);
				return value;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("Expression.unknownExpression") + "~");
			}
		} else { // A.~
			Current current = stack.getSequenceCurrent(sequence);
			if (current == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("\"[]\"" + mm.getMessage("engine.seriesNotInStack"));
			}

			int pos = node.calculateIndex(current, ctx);
			if (pos > 0) current.assign(pos, value);
			return value;
		}
	}
	
	public Object moves(Move node, Context ctx) {
		ComputeStack stack = ctx.getComputeStack();
		if (sequence == null) { // ~
			IComputeItem temp = stack.getTopObject();
			if (temp instanceof Current) {
				Current current = (Current)temp;
				int []range = node.calculateIndexRange(current, ctx);
				if (range == null) {
					return new Sequence(0);
				}

				int startSeq = range[0];
				int endSeq = range[1];
				Sequence result = new Sequence(endSeq - startSeq + 1);
				for (; startSeq <= endSeq; ++startSeq) {
					result.add(current.get(startSeq));
				}

				return result;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("Expression.unknownExpression") + "~");
			}
		} else { // A.~
			Current current = stack.getSequenceCurrent(sequence);
			if (current == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("\"{}\"" + mm.getMessage("engine.seriesNotInStack"));
			}

			int []range = node.calculateIndexRange(current, ctx);
			if (range == null) return new Sequence(0);

			int startSeq = range[0];
			int endSeq = range[1];
			Sequence result = new Sequence(endSeq - startSeq + 1);
			for (; startSeq <= endSeq; ++startSeq) {
				result.add(current.get(startSeq));
			}

			return result;
		}
	}
	
	/**
	 * 返回节点是否单调递增的
	 * @return true：是单调递增的，false：不是
	 */
	public boolean isMonotone() {
		return true;
	}
}
