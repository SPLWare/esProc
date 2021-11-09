package com.raqsoft.expression;

import java.util.List;

import com.raqsoft.common.DBSession;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.ComputeStack;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.DBObject;
import com.raqsoft.dm.DataStruct;
import com.raqsoft.dm.IComputeItem;
import com.raqsoft.dm.LinkEntry;
import com.raqsoft.dm.Param;
import com.raqsoft.dm.ParamList;
import com.raqsoft.dm.Record;
import com.raqsoft.dm.Sequence;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.util.EnvUtil;
import com.raqsoft.util.Variant;

/**
 * 产生表达式的时候没发分析出标识符是什么，可能是变量也可能是字段。
 * 运行过程中再根据计算上下文来确定是字段还是变量
 * @author WangXiaoJun
 *
 */
public class UnknownSymbol extends Node {
	private String name;
	
	private IComputeItem computeItem; // 上次计算时对应的计算对象
	private int col; // 上次计算对应的字段索引，用于性能优化
	private DataStruct prevDs; // 上次计算对应的数据结构，用于性能优化
	
	private Param param; // 上次计算对应的变量
	
	public UnknownSymbol(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	protected boolean containParam(String name) {
		return name.equals(this.name);
	}

	protected void getUsedParams(Context ctx, ParamList resultList) {
		Param param = EnvUtil.getParam(name, ctx);
		if (param != null && resultList.get(name) == null) {
			resultList.addVariable(name, param.getValue());
		}
	}
	
	public void getUsedFields(Context ctx, List<String> resultList) {
		if (EnvUtil.getParam(name, ctx) == null && ctx.getDBSession(name) == null) {
			resultList.add(name);
		}
	}

	public byte calcExpValueType(Context ctx) {
		return Expression.TYPE_UNKNOWN;
	}

	public Object calculate(Context ctx) {
		// 如果上次计算表达式时对应的纪录或序列还在堆栈中则使用上次的
		ComputeStack stack = ctx.getComputeStack();
		if (computeItem != null && computeItem.isInStack(stack)) {
			Object obj = computeItem.getCurrent();
			if (obj instanceof Record) {
				Record cur = (Record)obj;
				if (prevDs != cur.dataStruct()) {
					prevDs = cur.dataStruct();
					col = prevDs.getFieldIndex(name);
					if (col < 0) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(name + mm.getMessage("ds.fieldNotExist"));
					}
				}

				return cur.getNormalFieldValue(col);
			} else if (obj instanceof Sequence) {
				// 如果当前元素是序列则取其第一个元素
				if (((Sequence)obj).length() == 0) {
					return null;
				}
				
				obj = ((Sequence)obj).get(1);
				if (obj instanceof Record) {
					Record cur = (Record)obj;
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
					return null;
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("Expression.unknownExpression") + name);
				}
			} else if (obj == null) {
				return null;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("Expression.unknownExpression") + name);
			}
		} else if (param != null) {
			return param.getValue();
		} else {
			// 如果曾经被当做字段则不再找变量
			if (computeItem == null) {
				param = EnvUtil.getParam(name, ctx);
				if (param != null) { // 变量
					return param.getValue();
				}

				if (ctx != null) { // 数据库连接
					DBSession dbs = ctx.getDBSession(name);
					if (dbs != null) return new DBObject(dbs);
				}
			}

			// 字段
			// 第一次运算或运算环境已改变
			boolean hasNull = false; // 是否有序列第一个成员为空
			for (LinkEntry<IComputeItem> entry = stack.getStackHeadEntry(); entry != null; entry = entry.getNext()) {
				IComputeItem item = entry.getElement();
				Object cur = item.getCurrent();
				if (cur instanceof Record) {
					Record r = (Record) cur;
					col = r.getFieldIndex(name);

					if (col >= 0) {
						computeItem = item;
						prevDs = r.dataStruct();
						return r.getNormalFieldValue(col);
					}
				} else if (cur instanceof Sequence) {
					// 如果当前元素是序列则取其第一个元素
					if (((Sequence)cur).length() == 0) {
						computeItem = item;
						return null;
					}
					
					cur = ((Sequence)cur).get(1);
					if (cur instanceof Record) {
						Record r = (Record) cur;
						col = r.getFieldIndex(name);

						if (col >= 0) {
							computeItem = item;
							prevDs = r.dataStruct();
							return r.getNormalFieldValue(col);
						}
					} else if (cur == null) {
						hasNull = true;
					}
				} else if (cur == null) {
					hasNull = true;
				}
			}

			if (hasNull) {
				return null;
			}
			
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Expression.unknownExpression") + name);
		}
	}

	// '=' 赋值运算
	public Object assign(Object value, Context ctx) {
		// 如果上次计算表达式时对应的纪录或序列还在堆栈中则使用上次的
		ComputeStack stack = ctx.getComputeStack();
		if (computeItem != null && computeItem.isInStack(stack)) {
			Object obj = computeItem.getCurrent();
			if (obj instanceof Record) {
				Record cur = (Record)obj;
				if (prevDs != cur.dataStruct()) {
					prevDs = cur.dataStruct();
					col = prevDs.getFieldIndex(name);
					if (col < 0) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(name + mm.getMessage("ds.fieldNotExist"));
					}
				}

				cur.setNormalFieldValue(col, value);
			} else if (obj instanceof Sequence) {
				// 如果当前元素是序列则取其第一个元素
				if (((Sequence)obj).length() == 0) {
					return value;
				}
				
				obj = ((Sequence)obj).get(1);
				if (obj instanceof Record) {
					Record cur = (Record)obj;
					if (prevDs != cur.dataStruct()) {
						prevDs = cur.dataStruct();
						col = prevDs.getFieldIndex(name);
						if (col < 0) {
							MessageManager mm = EngineMessage.get();
							throw new RQException(name + mm.getMessage("ds.fieldNotExist"));
						}
					}

					cur.setNormalFieldValue(col, value);
				} else if (obj != null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("Expression.unknownExpression") + name);
				}
			} else if (obj != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("Expression.unknownExpression") + name);
			}
			
			return value;
		} else if (param != null) {
			param.setValue(value);
			return value;
		} else {
			if (computeItem == null) {
				param = EnvUtil.getParam(name, ctx); // 变量
				if (param != null) {
					param.setValue(value);
					return value;
				}
			}

			// 第一次运算或运算环境已改变
			for (LinkEntry<IComputeItem> entry = stack.getStackHeadEntry(); entry != null; entry = entry.getNext()) {
				IComputeItem item = entry.getElement();
				Object cur = item.getCurrent();
				
				// 赋值的情况不允许序列首元素为null？
				if (cur instanceof Record) {
					Record r = (Record) cur;
					col = r.getFieldIndex(name);

					if (col >= 0) {
						computeItem = item;
						prevDs = r.dataStruct();
						r.setNormalFieldValue(col, value);
						return value;
					}
				} else if (cur instanceof Sequence) {
					// 如果当前元素是序列则取其第一个元素
					if (((Sequence)cur).length() == 0) {
						computeItem = item;
						return value;
					}

					cur = ((Sequence)cur).get(1);
					if (cur instanceof Record) {
						Record r = (Record) cur;
						col = r.getFieldIndex(name);

						if (col >= 0) {
							computeItem = item;
							prevDs = r.dataStruct();
							r.setNormalFieldValue(col, value);
							return value;
						}
					}
				}
			}

			// 没有找到字段则产生变量放入上下文中
			Param newParam = new Param(name, Param.VAR, value);
			ctx.addParam(newParam);
			return value;
		}
	}

	// '+=' 赋值运算
	public Object addAssign(Object value, Context ctx) {
		// 如果上次计算表达式时对应的纪录或序列还在堆栈中则使用上次的
		ComputeStack stack = ctx.getComputeStack();
		if (computeItem != null && computeItem.isInStack(stack)) {
			Object obj = computeItem.getCurrent();
			if (obj instanceof Record) {
				Record cur = (Record)obj;
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
			} else if (obj instanceof Sequence) {
				// 如果当前元素是序列则取其第一个元素
				if (((Sequence)obj).length() == 0) {
					return value;
				}
				
				obj = ((Sequence)obj).get(1);
				if (obj instanceof Record) {
					Record cur = (Record)obj;
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
				} else if (obj != null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("Expression.unknownExpression") + name);
				}
			} else if (obj != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("Expression.unknownExpression") + name);
			}
			
			return value;
		} else if (param != null) {
			Object result = Variant.add(param.getValue(), value);
			param.setValue(result);
			return result;
		} else {
			if (computeItem == null) {
				param = EnvUtil.getParam(name, ctx); // 变量
				if (param != null) {
					Object result = Variant.add(param.getValue(), value);
					param.setValue(result);
					return result;
				}
			}

			// 第一次运算或运算环境已改变
			for (LinkEntry<IComputeItem> entry = stack.getStackHeadEntry(); entry != null; entry = entry.getNext()) {
				IComputeItem item = entry.getElement();
				Object cur = item.getCurrent();
				
				// 赋值的情况不允许序列首元素为null？
				if (cur instanceof Record) {
					Record r = (Record) cur;
					col = r.getFieldIndex(name);

					if (col >= 0) {
						computeItem = item;
						prevDs = r.dataStruct();

						Object result = Variant.add(r.getNormalFieldValue(col), value);
						r.setNormalFieldValue(col, result);
						return result;
					}
				} else if (cur instanceof Sequence) {
					// 如果当前元素是序列则取其第一个元素
					if (((Sequence)cur).length() == 0) {
						computeItem = item;
						return value;
					}

					cur = ((Sequence)cur).get(1);
					if (cur instanceof Record) {
						Record r = (Record) cur;
						col = r.getFieldIndex(name);

						if (col >= 0) {
							computeItem = item;
							prevDs = r.dataStruct();

							Object result = Variant.add(r.getNormalFieldValue(col), value);
							r.setNormalFieldValue(col, result);
							return result;
						}
					}
				}
			}

			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Expression.unknownExpression") + name);
		}
	}

	public Object move(Move node, Context ctx) {
		// 如果上次计算表达式时对应的序列还在堆栈中则使用上次的
		ComputeStack stack = ctx.getComputeStack();
		if (computeItem != null && computeItem.isInStack(stack)) {
			Sequence.Current current = (Sequence.Current)computeItem;
			int pos = node.calculateIndex(current, ctx);
			if (pos < 1) {
				return null;
			}

			// 如果当前元素是序列则取其第一个元素
			Object obj = current.get(pos);
			if (obj instanceof Sequence) {
				if (((Sequence)obj).length() == 0) {
					return null;
				} else {
					obj = ((Sequence)obj).get(1);
				}
			}

			if (obj instanceof Record) {
				Record r = (Record)obj;
				if (prevDs != r.dataStruct()) {
					prevDs = r.dataStruct();
					col = prevDs.getFieldIndex(name);
					if (col < 0) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(name + mm.getMessage("ds.fieldNotExist"));
					}
				}

				return r.getNormalFieldValue(col);
			} else if (obj == null) {
				return null;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("Expression.unknownExpression") + name);
			}
		}

		if (param != null || (computeItem == null && (param = EnvUtil.getParam(name, ctx)) != null)) {
			Object value = param.getValue();
			if (!(value instanceof Sequence)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("[]" + mm.getMessage("dot.seriesLeft"));
			}

			Sequence.Current current = stack.getSequenceCurrent((Sequence)value);
			if (current == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("[]" + mm.getMessage("engine.seriesNotInStack"));
			}

			int pos = node.calculateIndex(current, ctx);
			return pos > 0 ? current.get(pos) : null;
		}

		// 字段
		// 第一次运算或运算环境已改变
		for (LinkEntry<IComputeItem> entry = stack.getStackHeadEntry(); entry != null; entry = entry.getNext()) {
			IComputeItem item = entry.getElement();
			if (item instanceof Sequence.Current) { // series.(...)
				Sequence.Current current = (Sequence.Current) item;
				Object curObj = current.getCurrent();

				// 如果当前元素是序列则取其第一个元素
				if (curObj instanceof Sequence) {
					if (((Sequence)curObj).length() > 0) {
						curObj = ((Sequence)curObj).get(1);
					} else {
						curObj = null;
					}
				}

				if (curObj instanceof Record) {
					Record cur = (Record) curObj;
					col = cur.getFieldIndex(name);

					if (col >= 0) {
						computeItem = item;
						prevDs = cur.dataStruct();

						int pos = node.calculateIndex(current, ctx);
						if (pos < 1) return null;

						Object obj = current.get(pos);
						if (obj == null) return null;
						if (!(obj instanceof Record)) {
							MessageManager mm = EngineMessage.get();
							throw new RQException(mm.getMessage("Expression.unknownExpression") + name);
						}

						Record r = (Record)obj;
						if (prevDs != r.dataStruct()) {
							prevDs = r.dataStruct();
							col = prevDs.getFieldIndex(name);
							if (col < 0) {
								MessageManager mm = EngineMessage.get();
								throw new RQException(name + mm.getMessage("ds.fieldNotExist"));
							}
						}

						return r.getNormalFieldValue(col);
					}
				} else if (curObj == null) {
					computeItem = item;
					int pos = node.calculateIndex(current, ctx);
					if (pos < 1) return null;

					Object obj = current.get(pos);
					if (obj instanceof Sequence) {
						if (((Sequence)obj).length() == 0) {
							return null;
						} else {
							obj = ((Sequence)obj).get(1);
						}
					}

					if (obj == null) return null;
					if (!(obj instanceof Record)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(mm.getMessage("Expression.unknownExpression") + name);
					}

					Record r = ((Record)obj);
					prevDs = r.dataStruct();
					col = prevDs.getFieldIndex(name);
					if (col < 0) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(name + mm.getMessage("ds.fieldNotExist"));
					}

					return r.getNormalFieldValue(col);
				}
			}
		}

		MessageManager mm = EngineMessage.get();
		throw new RQException(mm.getMessage("Expression.unknownExpression") + name);
	}

	public Object moveAssign(Move node, Object value, Context ctx) {
		 // 如果上次计算表达式时对应的序列还在堆栈中则使用上次的
		ComputeStack stack = ctx.getComputeStack();
		if (computeItem != null && computeItem.isInStack(stack)) {
			Sequence.Current current = (Sequence.Current)computeItem;
			int pos = node.calculateIndex(current, ctx);
			if (pos < 1) return value;

			 // 如果当前元素是序列则取其第一个元素
			Object obj = current.get(pos);
			if (obj instanceof Sequence) {
				if (((Sequence)obj).length() == 0) {
					return value;
				} else {
					obj = ((Sequence)obj).get(1);
				}
			}

			if (obj instanceof Record) {
				Record r = (Record)obj;
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
			} else if (obj == null) {
				return value;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("Expression.unknownExpression") + name);
			}
		 }

		if (param != null || (computeItem == null && (param = EnvUtil.getParam(name, ctx)) != null)) {
			 Object obj = param.getValue();
			 if (!(obj instanceof Sequence)) {
				 MessageManager mm = EngineMessage.get();
				 throw new RQException("[]" + mm.getMessage("dot.seriesLeft"));
			 }

			 Sequence.Current current = stack.getSequenceCurrent((Sequence)obj);
			 if (current == null) {
				 MessageManager mm = EngineMessage.get();
				 throw new RQException("[]" + mm.getMessage("engine.seriesNotInStack"));
			 }

			 int pos = node.calculateIndex(current, ctx);
			 if (pos > 0) current.assign(pos, value);
			 return value;
		 }

		 // 字段
		 // 第一次运算或运算环境已改变
		for (LinkEntry<IComputeItem> entry = stack.getStackHeadEntry(); entry != null; entry = entry.getNext()) {
			IComputeItem item = entry.getElement();
			if (item instanceof Sequence.Current) { // series.(...)
				Sequence.Current current = (Sequence.Current) item;
				Object curObj = current.getCurrent();

				// 如果当前元素是序列则取其第一个元素
				if (curObj instanceof Sequence) {
					if (((Sequence)curObj).length() > 0) {
						curObj = ((Sequence)curObj).get(1);
					} else {
						curObj = null;
					}
				}

				if (curObj instanceof Record) {
					Record cur = (Record) curObj;
					col = cur.getFieldIndex(name);

					if (col >= 0) {
						computeItem = item;
						prevDs = cur.dataStruct();

						int pos = node.calculateIndex(current, ctx);
						if (pos < 1) return value;

						Object obj = current.get(pos);
						if (obj == null) return value;
						if (!(obj instanceof Record)) {
							MessageManager mm = EngineMessage.get();
							throw new RQException(mm.getMessage("Expression.unknownExpression") + name);
						}

						Record r = (Record)obj;
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
				} else if (curObj == null) {
					computeItem = item;
					int pos = node.calculateIndex(current, ctx);
					if (pos < 1) return value;

					Object obj = current.get(pos);
					if (obj instanceof Sequence) {
						if (((Sequence)obj).length() == 0) {
							return value;
						} else {
							obj = ((Sequence)obj).get(1);
						}
					}

					if (obj == null) return value;
					if (!(obj instanceof Record)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(mm.getMessage("Expression.unknownExpression") + name);
					}

					Record r = (Record)obj;
					prevDs = r.dataStruct();
					col = prevDs.getFieldIndex(name);
					if (col < 0) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(name + mm.getMessage("ds.fieldNotExist"));
					}

					r.setNormalFieldValue(col, value);
					return value;
				}
			}
		}

		MessageManager mm = EngineMessage.get();
		throw new RQException(mm.getMessage("Expression.unknownExpression") + name);
	}
	
	public Object moves(Move node, Context ctx) {
		// 如果上次计算表达式时对应的序列还在堆栈中则使用上次的
		ComputeStack stack = ctx.getComputeStack();
		if (computeItem != null && computeItem.isInStack(stack)) {
			Sequence.Current current = (Sequence.Current)computeItem;
			int []range = node.calculateIndexRange(current, ctx);
			if (range == null) return new Sequence(0);
			return Move.getFieldValues(current, name, range[0], range[1]);
		}

		if (param != null || (computeItem == null && (param = EnvUtil.getParam(name, ctx)) != null)) {
			Object value = param.getValue();
			if (!(value instanceof Sequence)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("[]" + mm.getMessage("dot.seriesLeft"));
			}

			Sequence.Current current = stack.getSequenceCurrent((Sequence)value);
			if (current == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("[]" + mm.getMessage("engine.seriesNotInStack"));
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

		// 字段
		// 第一次运算或运算环境已改变
		for (LinkEntry<IComputeItem> entry = stack.getStackHeadEntry(); entry != null; entry = entry.getNext()) {
			IComputeItem item = entry.getElement();
			if (item instanceof Sequence.Current) { // series.(...)
				Sequence.Current current = (Sequence.Current) item;
				Object curObj = current.getCurrent();

				if (curObj instanceof Sequence) {
					if (((Sequence)curObj).length() == 0) {
						curObj = null;
					} else {
						curObj = ((Sequence)curObj).get(1);
					}
				}

				if (curObj instanceof Record) {
					Record cur = (Record) curObj;
					col = cur.getFieldIndex(name);

					if (col >= 0) {
						computeItem = item;
						//prevRecord = cur;

						int []range = node.calculateIndexRange(current, ctx);
						if (range == null) return new Sequence(0);
						return Move.getFieldValues(current, name, range[0], range[1]);
					}
				} else if (curObj == null) {
					computeItem = item;
					int []range = node.calculateIndexRange(current, ctx);
					if (range == null) return new Sequence(0);
					return Move.getFieldValues(current, name, range[0], range[1]);
				}
			}
		}

		MessageManager mm = EngineMessage.get();
		throw new RQException(mm.getMessage("Expression.unknownExpression") + name);
	}

	public int getCol() {
		return col;
	}

}
