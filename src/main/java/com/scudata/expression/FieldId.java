package com.scudata.expression;

import com.scudata.array.BoolArray;
import com.scudata.array.ConstArray;
import com.scudata.array.IArray;
import com.scudata.array.ObjectArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.ComputeStack;
import com.scudata.dm.Context;
import com.scudata.dm.Current;
import com.scudata.dm.IComputeItem;
import com.scudata.dm.KeyWord;
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

	private Node left; // 点操作符的左侧节点	

	public FieldId(String id) {
		index = KeyWord.getFiledId(id) - 1;
		if (index < 0) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.indexOutofBound"));
		}
	}
	
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

	public int getFieldIndex() {
		return index;
	}
	
	public void setDotLeftObject(Object obj) {
		src = obj;
	}

	public Object calculate(Context ctx) {
		if (src == null) { // #1
			ComputeStack stack = ctx.getComputeStack();
			return stack.getTopObject().getFieldValue(index);
		} else { // series.#1 record.#1
			if (src instanceof Sequence) {
				Sequence seq = (Sequence)src;
				Current current = ctx.getComputeStack().getSequenceCurrent(seq);
				if (current == null) {
					if (seq.length() > 0) {
						return seq.getFieldValue(1, index);
					} else {
						return null;
					}
				} else {
					return seq.getFieldValue(current.getCurrentIndex(), index);
				}
			} else if (src instanceof BaseRecord) {
				return ((BaseRecord)src).getNormalFieldValue(index);
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("#" + (index + 1) + mm.getMessage("ds.fieldNotExist"));
			}
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
				return sequence.getFieldValueArray(index);
			} else {
				BaseRecord r = (BaseRecord)item.getCurrent();
				sequence = stack.getTopSequence();
				return new ConstArray(r.getFieldValue(index), sequence.length());
			}
		} else if (left instanceof CurrentElement) {
			// ~.#f
			ComputeStack stack = ctx.getComputeStack();
			IComputeItem item = stack.getTopObject();
			Sequence sequence = item.getCurrentSequence();
			if (sequence != null) {
				return sequence.getFieldValueArray(index);
			} else {
				BaseRecord r = (BaseRecord)item.getCurrent();
				sequence = stack.getTopSequence();
				return new ConstArray(r.getFieldValue(index), sequence.length());
			}
		} else if (left instanceof ElementRef) {
			return ((ElementRef)left).getFieldArray(ctx, this);
		} else {
			// A.#f
			IArray leftValues = left.calculateAll(ctx);
			return getFieldArray(ctx, leftValues);
		}
	}

	public IArray getFieldArray(Context ctx, IArray leftValues) {
		ComputeStack stack = ctx.getComputeStack();
		if (leftValues instanceof ConstArray) {
			Sequence top = stack.getTopSequence();
			Object leftObj = leftValues.get(1);
			if (leftObj instanceof Sequence) {
				if (leftObj == top) {
					return top.getFieldValueArray(index);
				} else {
					Sequence sequence = (Sequence)leftObj;
					int n = stack.getCurrentIndex(sequence);
					
					Object cur = null;
					if (n > 0) {
						cur = sequence.getMem(n);
						if (cur instanceof Sequence) {
							// 如果当前元素是序列则取其第一个元素
							if (((Sequence)cur).length() > 0) {
								cur = ((Sequence)cur).getMem(1);
							} else {
								cur = null;
							}
						}
					} else if (sequence.length() > 0) {
						cur = sequence.getMem(1);
					}
					
					if (cur instanceof BaseRecord) {
						cur = ((BaseRecord)cur).getNormalFieldValue(index);
					} else if (cur != null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("#" + (index + 1) + mm.getMessage("ds.fieldNotExist"));
					}
					
					return new ConstArray(cur, top.length());
				}
			} else if (leftObj instanceof BaseRecord) {
				Object value = ((BaseRecord)leftObj).getNormalFieldValue(index);
				return new ConstArray(value, top.length());
			} else if (leftObj == null) {
				return new ConstArray(null, top.length());
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("#" + (index + 1) + mm.getMessage("ds.fieldNotExist"));
			}
		} else {
			int len = leftValues.size();
			IArray result = new ObjectArray(len);
			result.setTemporary(true);
			
			for (int i = 1; i <= len; ++i) {
				Object src = leftValues.get(i);
				if (src instanceof Sequence) {
					Sequence sequence = (Sequence)src;
					int n = stack.getCurrentIndex(sequence);
					
					Object cur = null;
					if (n > 0) {
						cur = sequence.getMem(n);
						if (cur instanceof Sequence) {
							// 如果当前元素是序列则取其第一个元素
							if (((Sequence)cur).length() > 0) {
								cur = ((Sequence)cur).getMem(1);
							} else {
								cur = null;
							}
						}
					} else if (sequence.length() > 0) {
						cur = sequence.getMem(1);
					}
					
					if (cur instanceof BaseRecord) {
						cur = ((BaseRecord)cur).getNormalFieldValue(index);
					} else if (cur != null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("#" + (index + 1) + mm.getMessage("ds.fieldNotExist"));
					}
					
					result.push(cur);
				} else if (src instanceof BaseRecord) {
					Object value = ((BaseRecord)src).getNormalFieldValue(index);
					result.push(value);
				} else if (src == null) {
					result.push(null);
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("#" + (index + 1) + mm.getMessage("ds.fieldNotExist"));
				}
			}
			
			return result;
		}
	}
	
	// '=' 对字段进行赋值
	public Object assign(Object value, Context ctx) {
		if (src == null) { // #1
			ComputeStack stack = ctx.getComputeStack();
			Object obj = stack.getTopObject().getCurrent();
			if (obj instanceof BaseRecord) {
				((BaseRecord)obj).setNormalFieldValue(index, value);
				return value;
			} else if (obj == null) {
				return value;
			} else if (obj instanceof Sequence) {
				// 如果当前元素是序列则取其第一个元素
				if (((Sequence)obj).length() == 0) {
					return value;
				}

				obj = ((Sequence)obj).get(1);
				if (obj instanceof BaseRecord) {
					((BaseRecord)obj).setNormalFieldValue(index, value);
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
				if (obj instanceof BaseRecord) {
					((BaseRecord)obj).setNormalFieldValue(index, value);
					return value;
				} else if (obj == null) {
					return value;
				} else if (obj instanceof Sequence) {
					// 如果当前元素是序列则取其第一个元素
					if (((Sequence)obj).length() == 0) {
						return value;
					}

					obj = ((Sequence)obj).get(1);
					if (obj instanceof BaseRecord) {
						((BaseRecord)obj).setNormalFieldValue(index, value);
						return value;
					} else if (obj == null) {
						return value;
					}
				}

				MessageManager mm = EngineMessage.get();
				throw new RQException("#" + (index + 1) + mm.getMessage("ds.fieldNotExist"));
			} else if (src instanceof BaseRecord) {
				((BaseRecord)src).setNormalFieldValue(index, value);
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
			if (obj instanceof BaseRecord) {
				BaseRecord r = (BaseRecord)obj;
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
				if (obj instanceof BaseRecord) {
					BaseRecord r = (BaseRecord)obj;
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
				if (obj instanceof BaseRecord) {
					BaseRecord r = (BaseRecord)obj;
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
					if (obj instanceof BaseRecord) {
						BaseRecord r = (BaseRecord)obj;
						Object result = Variant.add(r.getNormalFieldValue(index), value);
						r.setNormalFieldValue(index, result);
						return result;
					} else if (obj == null) {
						return value;
					}
				}

				MessageManager mm = EngineMessage.get();
				throw new RQException("#" + (index + 1) + mm.getMessage("ds.fieldNotExist"));
			} else if (src instanceof BaseRecord) {
				BaseRecord r = (BaseRecord)src;
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
			if (temp instanceof Current) {
				Current current = (Current)temp;
				int pos = node.calculateIndex(current, ctx);
				if (pos < 1) {
					return null;
				}

				Object obj = current.get(pos);
				if (obj instanceof BaseRecord) {
					return ((BaseRecord)obj).getNormalFieldValue(index);
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
			Current current = stack.getSequenceCurrent((Sequence)src);
			if (current == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("\"[]\"" + mm.getMessage("engine.seriesNotInStack"));
			}

			int pos = node.calculateIndex(current, ctx);
			if (pos < 1) {
				return null;
			}

			Object obj = current.get(pos);
			if (obj instanceof BaseRecord) {
				return ((BaseRecord)obj).getNormalFieldValue(index);
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
			if (temp instanceof Current) {
				Current current = (Current)temp;
				int pos = node.calculateIndex(current, ctx);
				if (pos < 1) {
					return value;
				}

				Object obj = current.get(pos);
				if (obj instanceof BaseRecord) {
					((BaseRecord)obj).setNormalFieldValue(index, value);
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
			Current current = stack.getSequenceCurrent((Sequence)src);
			if (current == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("\"[]\"" + mm.getMessage("engine.seriesNotInStack"));
			}

			int pos = node.calculateIndex(current, ctx);
			if (pos < 1) return value;

			Object obj = current.get(pos);
			if (obj instanceof BaseRecord) {
				((BaseRecord)obj).setNormalFieldValue(index, value);
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
			if (temp instanceof Current) {
				Current current = (Current)temp;
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
			Current current = stack.getSequenceCurrent((Sequence)src);
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
	
	/**
	 * 返回节点是否单调递增的
	 * @return true：是单调递增的，false：不是
	 */
	public boolean isMonotone() {
		return true;
	}
}
