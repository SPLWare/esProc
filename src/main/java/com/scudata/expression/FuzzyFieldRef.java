package com.scudata.expression;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.ComputeStack;
import com.scudata.dm.Context;
import com.scudata.dm.Current;
import com.scudata.dm.Sequence;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 字段引用
 * A.#F r.#F ~.#F 取字段F的值，找不到返回null
 * @author WangXiaoJun
 *
 */
public class FuzzyFieldRef extends FieldRef {
	public FuzzyFieldRef(String fieldName) {
		super(fieldName);
	}

	public Object calculate(Context ctx) {
		if (s2r instanceof Sequence) {
			ComputeStack stack = ctx.getComputeStack();
			Object obj = stack.getCurrentValue((Sequence)s2r);

			// 如果当前元素是序列则取其第一个元素
			if (obj instanceof Sequence) {
				if (((Sequence)obj).length() == 0) {
					return null;
				} else {
					obj = ((Sequence)obj).get(1);
				}
			}

			if (obj instanceof BaseRecord) {
				BaseRecord cur = (BaseRecord)obj;
				if (prevDs != cur.dataStruct()) {
					prevDs = cur.dataStruct();
					col = prevDs.getFieldIndex(name);
				}

				if (col != -1) {
					return cur.getNormalFieldValue(col);
				} else {
					return null;
				}
			} else {
				return null;
			}
		} else if (s2r instanceof BaseRecord) {
			BaseRecord cur = (BaseRecord)s2r;
			if (prevDs != cur.dataStruct()) {
				prevDs = cur.dataStruct();
				col = prevDs.getFieldIndex(name);
			}

			if (col != -1) {
				return cur.getNormalFieldValue(col);
			} else {
				return null;
			}
		} else {
			return null;
		}
	}
	
	// '=' 对字段进行赋值
	public Object assign(Object value, Context ctx) {
		if (s2r instanceof Sequence) {
			ComputeStack stack = ctx.getComputeStack();
			Object obj = stack.getCurrentValue((Sequence)s2r);
			if (obj instanceof BaseRecord) {
				BaseRecord cur = (BaseRecord)obj;
				if (prevDs != cur.dataStruct()) {
					prevDs = cur.dataStruct();
					col = prevDs.getFieldIndex(name);
				}
	
				if (col != -1) {
					cur.setNormalFieldValue(col, value);
				}
			}
		} else if (s2r instanceof BaseRecord) {
			BaseRecord cur = (BaseRecord)s2r;
			if (prevDs != cur.dataStruct()) {
				prevDs = cur.dataStruct();
				col = prevDs.getFieldIndex(name);
			}
			
			if (col != -1) {
				cur.setNormalFieldValue(col, value);
			}
		}

		return value;
	}

	// '+=' 对字段进行赋值
	public Object addAssign(Object value, Context ctx) {
		if (s2r instanceof Sequence) {
			ComputeStack stack = ctx.getComputeStack();
			Object obj = stack.getCurrentValue((Sequence)s2r);

			if (obj instanceof BaseRecord) {
				BaseRecord cur = (BaseRecord)obj;
				if (prevDs != cur.dataStruct()) {
					prevDs = cur.dataStruct();
					col = prevDs.getFieldIndex(name);
				}
	
				if (col != -1) {
					Object result = Variant.add(cur.getNormalFieldValue(col), value);
					cur.setNormalFieldValue(col, result);
					return result;
				}
			}
		} else if (s2r instanceof BaseRecord) {
			BaseRecord cur = (BaseRecord)s2r;
			if (prevDs != cur.dataStruct()) {
				prevDs = cur.dataStruct();
				col = prevDs.getFieldIndex(name);
			}
			
			if (col != -1) {
				Object result = Variant.add(cur.getNormalFieldValue(col), value);
				cur.setNormalFieldValue(col, result);
				return result;
			}
		}
		
		return value;
	}
	
	public Object move(Move node, Context ctx) {
		if (!(s2r instanceof Sequence)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"[]\"" + mm.getMessage("dot.seriesLeft"));
		}

		ComputeStack stack = ctx.getComputeStack();
		Current current = stack.getSequenceCurrent((Sequence)s2r);
		if (current == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"[]\"" + mm.getMessage("engine.seriesNotInStack"));
		}

		int pos = node.calculateIndex(current, ctx);
		if (pos < 1) {
			return null;
		}

		Object mem = current.get(pos);
		if (mem instanceof BaseRecord) {
			BaseRecord r = (BaseRecord)mem;
			if (prevDs != r.dataStruct()) {
				prevDs = r.dataStruct();
				col = prevDs.getFieldIndex(name);
			}

			if (col != -1) {
				return r.getNormalFieldValue(col);
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	public Object moveAssign(Move node, Object value, Context ctx) {
		if (!(s2r instanceof Sequence)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"[]\"" + mm.getMessage("dot.seriesLeft"));
		}
		
		ComputeStack stack = ctx.getComputeStack();
		Current current = stack.getSequenceCurrent((Sequence)s2r);
		if (current == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"[]\"" + mm.getMessage("engine.seriesNotInStack"));
		}
		
		int pos = node.calculateIndex(current, ctx);
		if (pos < 1) {
			return value;
		}
		
		Object mem = current.get(pos);
		if (mem instanceof BaseRecord) {
			BaseRecord r = (BaseRecord)mem;
			if (prevDs != r.dataStruct()) {
				prevDs = r.dataStruct();
				col = prevDs.getFieldIndex(name);
			}

			if (col != -1) {
				r.setNormalFieldValue(col, value);
			}
		}
		
		return value;
	}

	public Object moves(Move node, Context ctx) {
		if (!(s2r instanceof Sequence)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"[]\"" + mm.getMessage("dot.seriesLeft"));
		}

		ComputeStack stack = ctx.getComputeStack();
		Current current = stack.getSequenceCurrent((Sequence)s2r);
		if (current == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"[]\"" + mm.getMessage("engine.seriesNotInStack"));
		}

		int []range = node.calculateIndexRange(current, ctx);
		if (range == null) {
			return new Sequence(0);
		}
		
		return Move.getFieldValues(current, name, range[0], range[1]);
	}
}
