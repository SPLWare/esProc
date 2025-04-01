package com.scudata.expression;

import java.util.List;

import com.scudata.array.ConstArray;
import com.scudata.array.IArray;
import com.scudata.cellset.INormalCell;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.Context;
import com.scudata.dm.Current;
import com.scudata.dm.ParamList;
import com.scudata.dm.Sequence;
import com.scudata.resources.EngineMessage;

/**
 * 执行偏移运算
 * A[2]; f[2] A.f[2]
 * @author RunQian
 *
 */
public class Move extends Function {
	private Node left;

	public Move() {
		priority = PRI_SUF;
	}

	public void setLeft(Node node) {
		left = node;
	}

	public Node getLeft() {
		return left;
	}

	protected boolean containParam(String name) {
		if (getLeft().containParam(name)) return true;
		return super.containParam(name);
	}

	protected void getUsedParams(Context ctx, ParamList resultList) {
		getLeft().getUsedParams(ctx, resultList);
		super.getUsedParams(ctx, resultList);
	}
	
	public void getUsedFields(Context ctx, List<String> resultList) {
		getLeft().getUsedFields(ctx, resultList);
		super.getUsedFields(ctx, resultList);
	}
	
	protected void getUsedCells(List<INormalCell> resultList) {
		getLeft().getUsedCells(resultList);
		super.getUsedCells(resultList);
	}
	
	/**
	 * 重置表达式，用于表达式缓存，多次执行使用不同的上下文，清除跟上下文有关的缓存信息
	 */
	public void reset() {
		getLeft().reset();
		super.reset();
	}

	public Node optimize(Context ctx) {
		param.optimize(ctx);
		left = getLeft().optimize(ctx);
		return this;
	}
	
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("[]" + mm.getMessage("function.missingParam"));
		}
		
		if (left == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"[]\"" + mm.getMessage("operator.missingRightOperation"));
		}
		
		left.checkValidity();
	}
	
	/**
	 * 判断是否可以计算全部的值，有赋值运算时只能一行行计算
	 * @return
	 */
	public boolean canCalculateAll() {
		if (!left.canCalculateAll()) {
			return false;
		}
		
		return param.canCalculateAll();
	}

	public Object calculate(Context ctx) {
		if (param.isLeaf()) {
			return getLeft().move(this, ctx);
		} else {
			return getLeft().moves(this, ctx);
		}
	}

	public Object assign(Object value, Context ctx) {
		if (cs instanceof com.scudata.cellset.datamodel.CellSet) {
			return getLeft().moveAssign(this, value, ctx);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("assign.needVar"));
		}
	}

	/**
	 * 计算偏移后的索引
	 * @param current 序列的循环当前
	 * @param ctx 计算上下文
	 * @return 偏移后的索引
	 */
	public int calculateIndex(Current current, Context ctx) {
		return calculateIndex(current, param, ctx);
	}
	
	/**
	 * 计算偏移后的索引
	 * @param current 序列的循环当前
	 * @param param 偏移参数
	 * @param ctx 计算上下文
	 * @return 偏移后的索引
	 */
	public static int calculateIndex(Current current, IParam param, Context ctx) {
		Object posObj = param.getLeafExpression().calculate(ctx);
		if (!(posObj instanceof Number)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("[]" + mm.getMessage("function.paramTypeError"));
		}

		int pos = ((Number)posObj).intValue() + current.getCurrentIndex();
		if (pos > 0 && pos <= current.length()) {
			return pos;
		} else {
			return 0;
		}
	}

	/**
	 * 计算偏移后的范围
	 * @param current 序列的循环当前
	 * @param ctx 计算上下文
	 * @return [起始位置, 结束位置]，起始位置（包含）和结束位置（包含）组成的数组
	 */
	public int[] calculateIndexRange(Current current, Context ctx) {
		return calculateIndexRange(current, param, ctx);
	}
	
	/**
	 * 计算偏移后的范围
	 * @param current 序列的循环当前
	 * @param param 左右偏移参数
	 * @param ctx 计算上下文
	 * @return [起始位置, 结束位置]，起始位置（包含）和结束位置（包含）组成的数组
	 */
	public static int[] calculateIndexRange(Current current, IParam param, Context ctx) {
		Number start = null, end = null;
		if (param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("[]" + mm.getMessage("function.invalidParam"));
		}

		IParam startParam = param.getSub(0);
		IParam endParam = param.getSub(1);
		if (startParam != null) {
			Object obj = startParam.getLeafExpression().calculate(ctx);
			if (!(obj instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("[]" + mm.getMessage("function.paramTypeError"));
			}

			start = (Number)obj;
		}

		if (endParam != null) {
			Object obj = endParam.getLeafExpression().calculate(ctx);
			if (!(obj instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("[]" + mm.getMessage("function.paramTypeError"));
			}

			end = (Number)obj;
		}
		
		return moves(current, start, end);
	}
	
	private static int[] moves(Current current, Number start, Number end) {
		int curIndex = current.getCurrentIndex();
		if (curIndex < 1) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.seriesNotInStack"));
		}

		int len = current.length();
		int startSeq, endSeq;
		if (start == null) {
			startSeq = 1;
		} else {
			startSeq = curIndex + start.intValue();
			if (startSeq < 1)startSeq = 1;
		}

		if (end == null) {
			endSeq = len;
		} else {
			endSeq = curIndex + end.intValue();
			if (endSeq > len)endSeq = len;
		}

		if (startSeq <= endSeq) {
			return new int[] {startSeq, endSeq};
		} else {
			return null;
		}
	}
	
	/**
	 * 取排列指定范围内记录的字段值
	 * @param current 排列的循环当前
	 * @param fieldName 字段名
	 * @param start 起始序号，包含
	 * @param end 结束序号，包含
	 * @return 结果集序列
	 */
	public static Sequence getFieldValues(Current current, String fieldName, int start, int end) {
		Sequence result = new Sequence(end - start + 1);
		int col = -1; // 字段在上一条记录的索引
		BaseRecord prevRecord = null; // 上一条记录

		while (start <= end) {
			Object obj = current.get(start++);
			if (obj != null) {
				if (!(obj instanceof BaseRecord)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("engine.needPmt"));
				}

				prevRecord = (BaseRecord)obj;
				col = prevRecord.getFieldIndex(fieldName);
				if (col < 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(fieldName + mm.getMessage("ds.fieldNotExist"));
				}

				result.add(prevRecord.getFieldValue(col));
				break;
			} else {
				result.add(null);
			}
		}

		for (; start <= end; ++start) {
			Object obj = current.get(start);
			if (obj != null) {
				if (!(obj instanceof BaseRecord)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("engine.needPmt"));
				}

				// 先跟上一条记录的结构做比较，如果同结构则直接用字段号取
				BaseRecord cur = (BaseRecord)obj;
				if (!prevRecord.isSameDataStruct(cur)) {
					col = cur.getFieldIndex(fieldName);
					if (col < 0) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(fieldName + mm.getMessage("ds.fieldNotExist"));
					}

					prevRecord = cur;
				}

				result.add(cur.getFieldValue(col));
			} else {
				result.add(null);
			}
		}

		return result;
	}

	/**
	 * 取排列指定范围内记录的字段值
	 * @param current 排列的循环当前
	 * @param field 字段号
	 * @param start 起始序号，包含
	 * @param end 结束序号，包含
	 * @return 结果集序列
	 */
	public static Sequence getFieldValues(Current current, int field, int start, int end) {
		Sequence result = new Sequence(end - start + 1);
		for (; start <= end; ++start) {
			Object obj = current.get(start);
			if (obj != null) {
				if (!(obj instanceof BaseRecord)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("engine.needPmt"));
				}

				BaseRecord cur = (BaseRecord)obj;
				result.add(cur.getFieldValue2(field));
			} else {
				result.add(null);
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
		if (param.isLeaf()) {
			IArray posArray = param.getLeafExpression().calculateAll(ctx);
			if (posArray instanceof ConstArray) {
				int move = posArray.getInt(1);
				IArray array = left.calculateAll(ctx);
				int len = array.size();
				
				if (move > 0) {
					if (move < len) {
						IArray result = array.newInstance(len);
						result.addAll(array, move + 1, len - move);
						for (int i = result.size(); i <= len; ++i) {
							result.pushNull();
						}
						
						return result;
					} else {
						return new ConstArray(null, len);
					}
				} else if (move < 0) {
					int end = len + move;
					if (end > 0) {
						IArray result = array.newInstance(len);
						for (int i = end + 1; i <= len; ++i) {
							result.pushNull();
						}
						
						result.addAll(array, end);
						return result;
					} else {
						return new ConstArray(null, len);
					}
				} else {
					return array;
				}
			}
		}

		return super.calculateAll(ctx);
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
