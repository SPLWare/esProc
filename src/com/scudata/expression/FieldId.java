package com.scudata.expression;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.ComputeStack;
import com.scudata.dm.Context;
import com.scudata.dm.IComputeItem;
import com.scudata.dm.KeyWord;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 用序号的方式引用记录的字段
 * #1 r.#1 A.#1
 * @author WangXiaoJun
 *
 */
public class FieldId extends Node {
	private Object src;
	private int index;

	public FieldId(String id) {
		index = KeyWord.getFiledId(id) - 1;
		if (index < 0) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.indexOutofBound"));
		}
	}

	public int getFieldIndex() {
		return index;
	}
	
	public void setDotLeftObject(Object obj) {
		src = obj;
	}

	public Object calculate(Context ctx) {
		if (src == null) { // #1
			ComputeStack stack = ctx.getComputeStack();
			Object obj = stack.getTopObject().getCurrent();
			if (obj instanceof Record) {
				return ((Record)obj).getNormalFieldValue(index);
			} else if (obj == null) {
				return null;
			} else if (obj instanceof Sequence) {
				// 如果当前元素是序列则取其第一个元素
				if (((Sequence)obj).length() == 0) {
					return null;
				}

				obj = ((Sequence)obj).get(1);
				if (obj instanceof Record) {
					return ((Record)obj).getNormalFieldValue(index);
				} else if (obj == null) {
					return null;
				}
			}

			MessageManager mm = EngineMessage.get();
			throw new RQException("#" + (index + 1) + mm.getMessage("ds.fieldNotExist"));
		} else { // series.#1 record.#1
			if (src instanceof Sequence) {
				ComputeStack stack = ctx.getComputeStack();
				Object obj = stack.getCurrentValue((Sequence)src);

				if (obj instanceof Record) {
					return ((Record)obj).getNormalFieldValue(index);
				} else if (obj == null) {
					return null;
				} else if (obj instanceof Sequence) {
					// 如果当前元素是序列则取其第一个元素
					if (((Sequence)obj).length() == 0) {
						return null;
					}

					obj = ((Sequence)obj).get(1);
					if (obj instanceof Record) {
						return ((Record)obj).getNormalFieldValue(index);
					} else if (obj == null) {
						return null;
					}
				}

				MessageManager mm = EngineMessage.get();
				throw new RQException("#" + (index + 1) + mm.getMessage("ds.fieldNotExist"));
			} else if (src instanceof Record) {
				return ((Record)src).getNormalFieldValue(index);
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("#" + (index + 1) + mm.getMessage("ds.fieldNotExist"));
			}
		}
	}

	// '=' 对字段进行赋值
	public Object assign(Object value, Context ctx) {
		if (src == null) { // #1
			ComputeStack stack = ctx.getComputeStack();
			Object obj = stack.getTopObject().getCurrent();
			if (obj instanceof Record) {
				((Record)obj).setNormalFieldValue(index, value);
				return value;
			} else if (obj == null) {
				return value;
			} else if (obj instanceof Sequence) {
				// 如果当前元素是序列则取其第一个元素
				if (((Sequence)obj).length() == 0) {
					return value;
				}

				obj = ((Sequence)obj).get(1);
				if (obj instanceof Record) {
					((Record)obj).setNormalFieldValue(index, value);
					return value;
				} else if (obj == null) {
					return value;
				}
			}

			MessageManager mm = EngineMessage.get();
			throw new RQException("#" + (index + 1) + mm.getMessage("ds.fieldNotExist"));
		} else { // series.#1 record.#1
			if (src instanceof Sequence) {
				ComputeStack stack = ctx.getComputeStack();
				Object obj = stack.getCurrentValue((Sequence)src);
				if (obj instanceof Record) {
					((Record)obj).setNormalFieldValue(index, value);
					return value;
				} else if (obj == null) {
					return value;
				} else if (obj instanceof Sequence) {
					// 如果当前元素是序列则取其第一个元素
					if (((Sequence)obj).length() == 0) {
						return value;
					}

					obj = ((Sequence)obj).get(1);
					if (obj instanceof Record) {
						((Record)obj).setNormalFieldValue(index, value);
						return value;
					} else if (obj == null) {
						return value;
					}
				}

				MessageManager mm = EngineMessage.get();
				throw new RQException("#" + (index + 1) + mm.getMessage("ds.fieldNotExist"));
			} else if (src instanceof Record) {
				((Record)src).setNormalFieldValue(index, value);
				return value;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("#" + (index = 1) +
									  mm.getMessage("ds.fieldNotExist"));
			}
		}
	}
	
	// '+=' 对字段进行赋值
	public Object addAssign(Object value, Context ctx) {
		if (src == null) { // #1
			ComputeStack stack = ctx.getComputeStack();
			Object obj = stack.getTopObject().getCurrent();
			if (obj instanceof Record) {
				Record r = (Record)obj;
				Object result = Variant.add(r.getNormalFieldValue(index), value);
				r.setNormalFieldValue(index, result);
				return result;
			} else if (obj == null) {
				return value;
			} else if (obj instanceof Sequence) {
				// 如果当前元素是序列则取其第一个元素
				if (((Sequence)obj).length() == 0) {
					return value;
				}

				obj = ((Sequence)obj).get(1);
				if (obj instanceof Record) {
					Record r = (Record)obj;
					Object result = Variant.add(r.getNormalFieldValue(index), value);
					r.setNormalFieldValue(index, result);
					return result;
				} else if (obj == null) {
					return value;
				}
			}

			MessageManager mm = EngineMessage.get();
			throw new RQException("#" + (index + 1) + mm.getMessage("ds.fieldNotExist"));
		} else { // series.#1 record.#1
			if (src instanceof Sequence) {
				ComputeStack stack = ctx.getComputeStack();
				Object obj = stack.getCurrentValue((Sequence)src);
				if (obj instanceof Record) {
					Record r = (Record)obj;
					Object result = Variant.add(r.getNormalFieldValue(index), value);
					r.setNormalFieldValue(index, result);
					return result;
				} else if (obj == null) {
					return value;
				} else if (obj instanceof Sequence) {
					// 如果当前元素是序列则取其第一个元素
					if (((Sequence)obj).length() == 0) {
						return value;
					}

					obj = ((Sequence)obj).get(1);
					if (obj instanceof Record) {
						Record r = (Record)obj;
						Object result = Variant.add(r.getNormalFieldValue(index), value);
						r.setNormalFieldValue(index, result);
						return result;
					} else if (obj == null) {
						return value;
					}
				}

				MessageManager mm = EngineMessage.get();
				throw new RQException("#" + (index + 1) + mm.getMessage("ds.fieldNotExist"));
			} else if (src instanceof Record) {
				Record r = (Record)src;
				Object result = Variant.add(r.getNormalFieldValue(index), value);
				r.setNormalFieldValue(index, result);
				return result;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("#" + (index = 1) + mm.getMessage("ds.fieldNotExist"));
			}
		}
	}

	public Object move(Move node, Context ctx) {
		if (src == null) { // #1
			ComputeStack stack = ctx.getComputeStack();
			IComputeItem temp = stack.getTopObject();
			if (temp instanceof Sequence.Current) {
				Sequence.Current current = (Sequence.Current)temp;
				int pos = node.calculateIndex(current, ctx);
				if (pos < 1) {
					return null;
				}

				Object obj = current.get(pos);
				if (obj instanceof Record) {
					return ((Record)obj).getNormalFieldValue(index);
				} else if (obj == null) {
					return null;
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("#" + (index + 1) + mm.getMessage("ds.fieldNotExist"));
				}
			}
			
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Expression.unknownExpression") + "#" + (index + 1));
		} else { // series.#1 record.#1
			if (!(src instanceof Sequence)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("\"[]\"" + mm.getMessage("dot.seriesLeft"));
			}

			ComputeStack stack = ctx.getComputeStack();
			Sequence.Current current = stack.getSequenceCurrent((Sequence)src);
			if (current == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("\"[]\"" + mm.getMessage("engine.seriesNotInStack"));
			}

			int pos = node.calculateIndex(current, ctx);
			if (pos < 1) {
				return null;
			}

			Object obj = current.get(pos);
			if (obj instanceof Record) {
				return ((Record)obj).getNormalFieldValue(index);
			} else if (obj == null) {
				return null;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("#" + (index + 1) + mm.getMessage("ds.fieldNotExist"));
			}
		}
	}

	public Object moveAssign(Move node, Object value, Context ctx) {
		if (src == null) { // #1
			ComputeStack stack = ctx.getComputeStack();
			IComputeItem temp = stack.getTopObject();
			if (temp instanceof Sequence.Current) {
				Sequence.Current current = (Sequence.Current)temp;
				int pos = node.calculateIndex(current, ctx);
				if (pos < 1) {
					return value;
				}

				Object obj = current.get(pos);
				if (obj instanceof Record) {
					((Record)obj).setNormalFieldValue(index, value);
					return value;
				} else if (obj == null) {
					return value;
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("#" + (index + 1) + mm.getMessage("ds.fieldNotExist"));
				}
			}

			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Expression.unknownExpression") + "#" + (index + 1));
		} else { // series.#1 record.#1
			if (!(src instanceof Sequence)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("\"[]\"" + mm.getMessage("dot.seriesLeft"));
			}

			ComputeStack stack = ctx.getComputeStack();
			Sequence.Current current = stack.getSequenceCurrent((Sequence)src);
			if (current == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("\"[]\"" + mm.getMessage("engine.seriesNotInStack"));
			}

			int pos = node.calculateIndex(current, ctx);
			if (pos < 1) return value;

			Object obj = current.get(pos);
			if (obj instanceof Record) {
				((Record)obj).setNormalFieldValue(index, value);
				return value;
			} else if (obj == null) {
				return value;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("#" + (index + 1) + mm.getMessage("ds.fieldNotExist"));
			}
		}
	}

	public Object moves(Move node, Context ctx) {
		if (src == null) { // #1
			ComputeStack stack = ctx.getComputeStack();
			IComputeItem temp = stack.getTopObject();
			if (temp instanceof Sequence.Current) {
				Sequence.Current current = (Sequence.Current)temp;
				int []range = node.calculateIndexRange(current, ctx);
				if (range != null) {
					return Move.getFieldValues(current, index, range[0], range[1]);
				} else {
					return new Sequence(0);
				}
			}

			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Expression.unknownExpression") + "#" + (index + 1));
		} else { // series.#1 record.#1
			if (!(src instanceof Sequence)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("\"[]\"" + mm.getMessage("dot.seriesLeft"));
			}

			ComputeStack stack = ctx.getComputeStack();
			Sequence.Current current = stack.getSequenceCurrent((Sequence)src);
			if (current == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("\"[]\"" + mm.getMessage("engine.seriesNotInStack"));
			}

			int []range = node.calculateIndexRange(current, ctx);
			if (range != null) {
				return Move.getFieldValues(current, index, range[0], range[1]);
			} else {
				return new Sequence(0);
			}
		}
	}
}
