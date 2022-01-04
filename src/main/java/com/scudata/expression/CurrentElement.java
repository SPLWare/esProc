package com.scudata.expression;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.ComputeStack;
import com.scudata.dm.Context;
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
	
	public Object assign(Object value, Context ctx) {
		ComputeStack stack = ctx.getComputeStack();
		if (sequence == null) { // ~
			IComputeItem temp = stack.getTopObject();
			if (temp instanceof Sequence.Current) {
				((Sequence.Current)temp).assign(value);
				return value;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("Expression.unknownExpression") + "~");
			}
		} else { // A.~
			Sequence.Current current = stack.getSequenceCurrent(sequence);
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
			if (temp instanceof Sequence.Current) {
				Object result = Variant.add(((Sequence.Current)temp).getCurrent(), value);
				((Sequence.Current)temp).assign(result);
				return result;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("Expression.unknownExpression") + "~");
			}
		} else { // A.~
			Sequence.Current current = stack.getSequenceCurrent(sequence);
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
			if (temp instanceof Sequence.Current) {
				Sequence.Current current = (Sequence.Current)temp;
				int pos = node.calculateIndex(current, ctx);
				return pos > 0 ? current.get(pos) : null;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("Expression.unknownExpression") + "~");
			}
		} else { // A.~
			Sequence.Current current = stack.getSequenceCurrent(sequence);
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
			if (temp instanceof Sequence.Current) {
				Sequence.Current current = (Sequence.Current)temp;
				int pos = node.calculateIndex(current, ctx);
				if (pos > 0) current.assign(pos, value);
				return value;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("Expression.unknownExpression") + "~");
			}
		} else { // A.~
			Sequence.Current current = stack.getSequenceCurrent(sequence);
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
			if (temp instanceof Sequence.Current) {
				Sequence.Current current = (Sequence.Current)temp;
				int []range = node.calculateIndexRange(current, ctx);
				if (range == null) return new Sequence(0);

				int startSeq = range[0];
				int endSeq = range[1];
				Sequence retSeries = new Sequence(endSeq - startSeq + 1);
				for (; startSeq <= endSeq; ++startSeq) {
					retSeries.add(current.get(startSeq));
				}

				return retSeries;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("Expression.unknownExpression") + "~");
			}
		} else { // A.~
			Sequence.Current current = stack.getSequenceCurrent(sequence);
			if (current == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("\"{}\"" + mm.getMessage("engine.seriesNotInStack"));
			}

			int []range = node.calculateIndexRange(current, ctx);
			if (range == null) return new Sequence(0);

			int startSeq = range[0];
			int endSeq = range[1];
			Sequence retSeries = new Sequence(endSeq - startSeq + 1);
			for (; startSeq <= endSeq; ++startSeq) {
				retSeries.add(current.get(startSeq));
			}

			return retSeries;
		}
	}
}
