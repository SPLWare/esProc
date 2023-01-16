package com.scudata.expression.fn;

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
import com.scudata.dm.KeyWord;
import com.scudata.dm.LinkEntry;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.Move;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

/**
 * 循环函数中迭代运算，对有相同字段值的成员累积
 * get(level,F;a:b) 在多层循环函数中取出上层的基成员信息。比如A.fn(B.fn(get(1))，表示取A的当前循环成员
 * 					level为向上数的层数，本层为0；
 * 					F为字段名，#表示序号，省略取成员；
 * 					a:b解释与符号[…]相同，可省略；在循环函数外无定义
 * @author runqian
 *
 */
public class Get extends Function {
	private int level = -1;
	private String fieldName; // 所要取的字段，如果没有F参数则为空
	private boolean isSeq; // 是否取当前循环序号，如果fieldName为空并且isSeq为false则取当前循环成员
	private IParam moveParam;
	
	private DataStruct prevDs; // 上次计算对应的数据结构，用于性能优化
	private int col = -1; // 上次计算对应的字段索引，用于性能优化
	
	public Node optimize(Context ctx) {
		if (param != null) param.optimize(ctx);
		return this;
	}
	
	private void prepare(IParam param, Context ctx) {
		if (param != null && param.getType() == IParam.Semicolon) {
			moveParam = param.getSub(1);
			param = param.getSub(0);
		}
		
		if (param == null) {
			level = 0;
		} else if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (obj instanceof Number) {
				level = ((Number)obj).intValue();
				if (level < 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("get" + mm.getMessage("function.missingParam"));
				}
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("get" + mm.getMessage("function.paramTypeError"));
			}
		} else if (param.getSubSize() == 2) {
			IParam levelParam = param.getSub(0);
			if (levelParam == null) {
				level = 0;
			} else {
				Object obj = levelParam.getLeafExpression().calculate(ctx);
				if (obj instanceof Number) {
					level = ((Number)obj).intValue();
					if (level < 0) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("get" + mm.getMessage("function.missingParam"));
					}
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("get" + mm.getMessage("function.paramTypeError"));
				}
			}
			
			IParam fieldParam = param.getSub(1);
			if (fieldParam == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("get" + mm.getMessage("function.invalidParam"));
			}
			
			fieldName = fieldParam.getLeafExpression().getIdentifierName();
			isSeq = fieldName.equals(KeyWord.CURRENTSEQ);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("get" + mm.getMessage("function.invalidParam"));
		}
	}
	
	public Object calculate(Context ctx) {
		if (level == -1) {
			prepare(param, ctx);
		}
		
		ComputeStack stack = ctx.getComputeStack();
		LinkEntry<IComputeItem> entry = stack.getStackHeadEntry();
		if (entry == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("function.notInCyclicalFunction", "'get'"));
		}
		
		// 根据层取出相应的循环对象
		int level = this.level;
		while (level != 0) {
			level--;
			entry = entry.getNext();
			if (entry == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(this.level + mm.getMessage("engine.indexOutofBound"));
			}
		}
		
		IComputeItem item = entry.getElement();
		if (moveParam == null) {
			// get(level,F)
			if (fieldName == null) {
				return item.getCurrent();
			} else if (isSeq) {
				return item.getCurrentIndex();
			} else {
				Object obj = item.getCurrent();
				return getFieldValue(obj);
			}
		} else {
			// get(level,F;a:b)
			if (!(item instanceof Current)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("get" + mm.getMessage("function.invalidParam"));
			}
			
			Current current = (Current)item;
			if (moveParam.isLeaf()) {
				int pos = Move.calculateIndex(current, moveParam, ctx);
				if (fieldName == null) {
					return pos > 0 ? current.get(pos) : null;
				} else if (isSeq) {
					return pos;
				} else {
					if (pos < 1) {
						return null;
					}
					
					Object obj = current.get(pos);
					return getFieldValue(obj);
				}
			} else {
				int []range = Move.calculateIndexRange(current, moveParam, ctx);
				if (range == null) {
					return new Sequence(0);
				}
				
				if (fieldName == null) {
					int startSeq = range[0];
					int endSeq = range[1];
					Sequence result = new Sequence(endSeq - startSeq + 1);
					for (; startSeq <= endSeq; ++startSeq) {
						result.add(current.get(startSeq));
					}

					return result;
				} else if (isSeq) {
					return new Sequence(range[0], range[1]);
				} else {
					return Move.getFieldValues(current, fieldName, range[0], range[1]);
				}
			}
		}
	}
	
	// 取obj的fieldName字段值
	private Object getFieldValue(Object obj) {
		if (obj instanceof BaseRecord) {
			BaseRecord cur = (BaseRecord)obj;
			if (prevDs != cur.dataStruct()) {
				prevDs = cur.dataStruct();
				col = prevDs.getFieldIndex(fieldName);
				if (col < 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(fieldName + mm.getMessage("ds.fieldNotExist"));
				}
			}

			return cur.getNormalFieldValue(col);
		} else if (obj instanceof Sequence) {
			// 如果当前元素是序列则取其第一个元素
			if (((Sequence)obj).length() == 0) {
				return null;
			}
			
			obj = ((Sequence)obj).get(1);
			if (obj instanceof BaseRecord) {
				BaseRecord cur = (BaseRecord)obj;
				if (prevDs != cur.dataStruct()) {
					prevDs = cur.dataStruct();
					col = prevDs.getFieldIndex(fieldName);
					if (col < 0) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(fieldName + mm.getMessage("ds.fieldNotExist"));
					}
				}

				return cur.getNormalFieldValue(col);
			} else if (obj == null) {
				return null;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("Expression.unknownExpression") + fieldName);
			}
		} else if (obj == null) {
			return null;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Expression.unknownExpression") + fieldName);
		}
	}
	
	/**
	 * 计算出所有行的结果
	 * @param ctx 计算上行文
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx) {
		if (level == -1) {
			prepare(param, ctx);
		}
		
		Current current = ctx.getComputeStack().getTopCurrent();
		int len = current.length();

		if (level != 0 && moveParam == null) {
			Object value = calculate(ctx);
			return new ConstArray(value, len);
		} else {
			ObjectArray array = new ObjectArray(len);
			array.setTemporary(true);
			
			for (int i = 1; i <= len; ++i) {
				current.setCurrent(i);
				Object value = calculate(ctx);
				array.push(value);
			}
			
			return array;
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
}
