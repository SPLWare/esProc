package com.scudata.expression;

import java.util.List;

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
import com.scudata.dm.DataStruct;
import com.scudata.dm.IComputeItem;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 字段引用
 * A.f r.f ~.f
 * @author WangXiaoJun
 *
 */
public class FieldRef extends Node {
	protected String name;
	protected Object s2r; // 序列或记录
	protected int col; // 字段索引
	protected DataStruct prevDs;

	private Node left; // 点操作符的左侧节点	

	public FieldRef(String fieldName) {
		name = fieldName;
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
	
	public String getName() {
		return name;
	}
	
	public void getUsedFields(Context ctx, List<String> resultList) {
		resultList.add(name);
	}
	
	public boolean isLeftTypeMatch(Object obj) {
		return true;
	}

	public void setDotLeftObject(Object obj) {
		s2r = obj;
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
					if (col < 0) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(name + mm.getMessage("ds.fieldNotExist"));
					}
				}

				return cur.getNormalFieldValue(col);
			} else if (obj == null) {
				// 检查一下是否是序表的字段，防止T.f(...)写错函数名解释成取f的成员了
				if (s2r instanceof Table) {
					col = ((Table)s2r).dataStruct().getFieldIndex(name);
					if (col < 0) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(name + mm.getMessage("ds.fieldNotExist"));
					}					
				}
				
				return null;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException(name + mm.getMessage("ds.fieldNotExist"));
			}
		} else if (s2r instanceof BaseRecord) {
			BaseRecord cur = (BaseRecord)s2r;
			if (prevDs != cur.dataStruct()) {
				prevDs = cur.dataStruct();
				col = prevDs.getFieldIndex(name);
				if (col < 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(name + mm.getMessage("ds.fieldNotExist"));
				}
			}

			return cur.getNormalFieldValue(col);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Expression.unknownExpression") + name);
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

	private IArray getField(Object leftObj, Context ctx) {
		ComputeStack stack = ctx.getComputeStack();
		Sequence top = stack.getTopSequence();
		
		if (leftObj == top) {
			// T.fn(T.field)
			return top.getFieldValueArray(name);
		} else if (leftObj instanceof Sequence) {
			Sequence sequence = (Sequence)leftObj;
			int n = stack.getCurrentIndex(sequence);
			
			if (n > 0) {
				leftObj = sequence.getMem(n);
				if (leftObj instanceof Sequence) {
					// 如果当前元素是序列则取其第一个元素
					if (((Sequence)leftObj).length() > 0) {
						leftObj = ((Sequence)leftObj).getMem(1);
					} else {
						leftObj = null;
					}
				}
			} else if (sequence.length() > 0) {
				leftObj = sequence.getMem(1);
			} else {
				leftObj = null;
			}
			
			if (leftObj instanceof BaseRecord) {
				BaseRecord r = (BaseRecord)leftObj;
				if (prevDs != r.dataStruct()) {
					prevDs = r.dataStruct();
					col = prevDs.getFieldIndex(name);
					if (col < 0) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(name + mm.getMessage("ds.fieldNotExist"));
					}
				}
				
				leftObj = r.getNormalFieldValue(col);
			} else if (leftObj != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(name + mm.getMessage("ds.fieldNotExist"));
			}
			
			return new ConstArray(leftObj, top.length());
		} else if (leftObj instanceof BaseRecord) {
			BaseRecord r = (BaseRecord)leftObj;
			if (prevDs != r.dataStruct()) {
				prevDs = r.dataStruct();
				col = prevDs.getFieldIndex(name);
				if (col < 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(name + mm.getMessage("ds.fieldNotExist"));
				}
			}
			
			return new ConstArray(r.getNormalFieldValue(col), top.length());
		} else if (leftObj == null) {
			return new ConstArray(null, top.length());
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(name + mm.getMessage("ds.fieldNotExist"));
		}
	}
	
	/**
	 * 计算出所有行的结果
	 * @param ctx 计算上行文
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx) {
		if (left instanceof CurrentElement) {
			// ~.f
			ComputeStack stack = ctx.getComputeStack();
			IComputeItem item = stack.getTopObject();
			return item.getCurrentSequence().getFieldValueArray(name);
		} else if (left instanceof ElementRef) {
			return ((ElementRef)left).getFieldArray(ctx, this);
		}
		
		// A.f
		IArray leftArray = left.calculateAll(ctx);
		if (leftArray instanceof ConstArray) {
			Object leftObj = leftArray.get(1);
			return getField(leftObj, ctx);
		} else {
			return getFieldArray(leftArray);
		}
	}
	
	public IArray getFieldArray(IArray leftArray) {
		// 可能是外键式引用fk.f或A.f
		int len = leftArray.size();
		Object src = leftArray.get(1);
		IArray result;
		
		if (src instanceof BaseRecord) {
			BaseRecord r = (BaseRecord)src;
			if (prevDs != r.dataStruct()) {
				prevDs = r.dataStruct();
				col = prevDs.getFieldIndex(name);
				if (col < 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(name + mm.getMessage("ds.fieldNotExist"));
				}
			}
			
			result = r.createFieldValueArray(col, len);
		} else {
			result = new ObjectArray(len);
		}
		
		if (result instanceof ObjectArray) {
			for (int i = 1; i <= len; ++i) {
				src = leftArray.get(i);
				if (src instanceof Sequence) {
					Sequence seq = (Sequence)src;
					if (seq.length() > 0) {
						src = seq.getMem(1);
					} else {
						src = null;
					}
				}
	
				if (src instanceof BaseRecord) {
					BaseRecord r = (BaseRecord)src;
					if (prevDs != r.dataStruct()) {
						prevDs = r.dataStruct();
						col = prevDs.getFieldIndex(name);
						if (col < 0) {
							MessageManager mm = EngineMessage.get();
							throw new RQException(name + mm.getMessage("ds.fieldNotExist"));
						}
					}
					
					r.getNormalFieldValue(col, result);
				} else if (src == null) {
					result.push(null);
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException(name + mm.getMessage("ds.fieldNotExist"));
				}
			}
		} else {
			// 纯序表
			for (int i = 1; i <= len; ++i) {
				src = leftArray.get(i);
				if (src instanceof BaseRecord) {
					BaseRecord r = (BaseRecord)src;
					r.getNormalFieldValue(col, result);
				} else if (src == null) {
					result.push(null);
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException(name + mm.getMessage("ds.fieldNotExist"));
				}
			}
		}
		
		result.setTemporary(true);
		return result;
	}
	
	// '=' 对字段进行赋值
	public Object assign(Object value, Context ctx) {
		if (s2r instanceof Sequence) {
			ComputeStack stack = ctx.getComputeStack();
			Object obj = stack.getCurrentValue((Sequence)s2r);
			if (obj == null) return value;

			if (!(obj instanceof BaseRecord)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(name + mm.getMessage("ds.fieldNotExist"));
			}

			BaseRecord cur = (BaseRecord)obj;
			if (prevDs != cur.dataStruct()) {
				prevDs = cur.dataStruct();
				col = prevDs.getFieldIndex(name);
				if (col < 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(name + mm.getMessage("ds.fieldNotExist"));
				}
			}

			cur.setNormalFieldValue(col, value);
		} else if (s2r instanceof BaseRecord) {
			BaseRecord cur = (BaseRecord)s2r;
			if (prevDs != cur.dataStruct()) {
				prevDs = cur.dataStruct();
				col = prevDs.getFieldIndex(name);
				if (col < 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(name + mm.getMessage("ds.fieldNotExist"));
				}
			}
			
			cur.setNormalFieldValue(col, value);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Expression.unknownExpression") + name);
		}

		return value;
	}

	// '+=' 对字段进行赋值
	public Object addAssign(Object value, Context ctx) {
		if (s2r instanceof Sequence) {
			ComputeStack stack = ctx.getComputeStack();
			Object obj = stack.getCurrentValue((Sequence)s2r);
			if (obj == null) return value;

			if (!(obj instanceof BaseRecord)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(name + mm.getMessage("ds.fieldNotExist"));
			}

			BaseRecord cur = (BaseRecord)obj;
			if (prevDs != cur.dataStruct()) {
				prevDs = cur.dataStruct();
				col = prevDs.getFieldIndex(name);
				if (col < 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(name + mm.getMessage("ds.fieldNotExist"));
				}
			}

			Object result = Variant.add(cur.getNormalFieldValue(col), value);
			cur.setNormalFieldValue(col, result);
			return result;
		} else if (s2r instanceof BaseRecord) {
			BaseRecord cur = (BaseRecord)s2r;
			if (prevDs != cur.dataStruct()) {
				prevDs = cur.dataStruct();
				col = prevDs.getFieldIndex(name);
				if (col < 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(name + mm.getMessage("ds.fieldNotExist"));
				}
			}
			
			Object result = Variant.add(cur.getNormalFieldValue(col), value);
			cur.setNormalFieldValue(col, result);
			return result;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Expression.unknownExpression") + name);
		}
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
		if (pos < 1) return null;

		Object mem = current.get(pos);
		if (mem == null) return null;
		if (mem instanceof BaseRecord) {
			BaseRecord r = (BaseRecord)mem;
			if (prevDs != r.dataStruct()) {
				prevDs = r.dataStruct();
				col = prevDs.getFieldIndex(name);
				if (col < 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(name + mm.getMessage("ds.fieldNotExist"));
				}
			}

			return r.getNormalFieldValue(col);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(name + mm.getMessage("ds.fieldNotExist"));
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
		if (pos < 1) return value;
		
		Object mem = current.get(pos);
		if (mem == null) return value;
		if (!(mem instanceof BaseRecord)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(name + mm.getMessage("ds.fieldNotExist"));
		}
		
		BaseRecord r = (BaseRecord)mem;
		if (prevDs != r.dataStruct()) {
			prevDs = r.dataStruct();
			col = prevDs.getFieldIndex(name);
			if (col < 0) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(name + mm.getMessage("ds.fieldNotExist"));
			}
		}

		r.setNormalFieldValue(col, value);
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
		if (range == null) return new Sequence(0);
		return Move.getFieldValues(current, name, range[0], range[1]);
	}
	
	public int getCol() {
		return col;
	}
	
	/**
	 * 返回节点是否单调递增的
	 * @return true：是单调递增的，false：不是
	 */
	public boolean isMonotone() {
		return true;
	}
}
