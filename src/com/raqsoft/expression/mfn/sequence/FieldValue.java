package com.raqsoft.expression.mfn.sequence;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.ListBase1;
import com.raqsoft.dm.Record;
import com.raqsoft.dm.Sequence;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.SequenceFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 取排列指定字段的值或设置指定字段的值
 * A.field(F) A.field(F, v)
 * @author RunQian
 *
 */
public class FieldValue extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("field" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (obj instanceof Number) {
				int findex = ((Number)obj).intValue();
				if (findex > 0) {
					// 字段从0开始计数
					findex--;
				} else if (findex == 0) {
					return null;
				} // 小于0从后数
				
				if (option == null || option.indexOf('r') == -1) {
					return getFieldValues(srcSequence, findex);
				} else {
					return srcSequence.fieldValues_r(findex);
				}
			} else if (obj instanceof String) {
				if (option == null || option.indexOf('r') == -1) {
					return getFieldValues(srcSequence, (String)obj);
				} else {
					return srcSequence.fieldValues_r((String)obj);
				}
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("field" + mm.getMessage("function.paramTypeError"));
			}
		} else {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("field" + mm.getMessage("function.invalidParam"));
			}

			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);
			if (sub0 == null || sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("field" + mm.getMessage("function.invalidParam"));
			}

			Object obj = sub0.getLeafExpression().calculate(ctx);
			Object value = sub1.getLeafExpression().calculate(ctx);
			if (obj instanceof Number) {
				int findex = ((Number)obj).intValue();
				if (findex > 0) {
					// 字段从0开始计数
					findex--;
				} else if (findex == 0) {
					return null;
				} // 小于0从后数
				
				if (value instanceof Sequence) {
					setFieldValues(srcSequence, findex, (Sequence)value);
				} else {
					setFieldValues(srcSequence, findex, value);
				}
			} else if (obj instanceof String) {				
				if (value instanceof Sequence) {
					setFieldValues(srcSequence, (String)obj, (Sequence)value);
				} else {
					setFieldValues(srcSequence, (String)obj, value);
				}
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("field" + mm.getMessage("function.paramTypeError"));
			}

			return null;
		}
	}
	
	private static Sequence getFieldValues(Sequence src, int field) {
		ListBase1 mems = src.getMems();
		int size = mems.size();
		Sequence result = new Sequence(size);

		for (int i = 1; i <= size; ++i) {
			Record cur = (Record)mems.get(i);
			if (cur == null) {
				result.add(null);
			} else {
				result.add(cur.getFieldValue2(field));
			}
		}

		return result;
	}
	
	public static Sequence getFieldValues(Sequence src, String fieldName) {
		ListBase1 mems = src.getMems();
		int size = mems.size();
		Sequence result = new Sequence(size);

		int col = -1; // 字段在上一条记录的索引
		Record prevRecord = null; // 上一条记录

		int i = 1;
		while (i <= size) {
			Object obj = mems.get(i++);
			if (obj != null) {
				if (!(obj instanceof Record)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("engine.needPmt"));
				}

				prevRecord = (Record)obj;
				col = prevRecord.getFieldIndex(fieldName);
				if (col >= 0) {
					result.add(prevRecord.getFieldValue(col));
				} else {
					result.add(null);
				}

				break;
			} else {
				result.add(null);
			}
		}

		for (; i <= size; ++i) {
			Object obj = mems.get(i);
			if (obj != null) {
				if (!(obj instanceof Record)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("engine.needPmt"));
				}

				Record cur = (Record)obj;
				if (!prevRecord.isSameDataStruct(cur)) {
					col = cur.getFieldIndex(fieldName);
					prevRecord = cur;
				}

				if (col >= 0) {
					result.add(cur.getFieldValue(col));
				} else {
					result.add(null);
				}
			} else {
				result.add(null);
			}
		}

		return result;
	}
	
	public static void setFieldValues(Sequence src, String fieldName, Object val) {
		ListBase1 mems = src.getMems();
		int size = mems.size();

		int col = -1; // 字段在上一条记录的索引
		Record prevRecord = null; // 上一条记录

		int i = 1;
		while (i <= size) {
			Object obj = mems.get(i++);
			if (obj != null) {
				if (!(obj instanceof Record)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("engine.needPmt"));
				}

				prevRecord = (Record)obj;
				col = prevRecord.getFieldIndex(fieldName);
				if (col >= 0) {
					prevRecord.setNormalFieldValue(col, val);
				}
				break;
			}
		}

		for (; i <= size; ++i) {
			Object obj = mems.get(i);
			if (obj != null) {
				if (!(obj instanceof Record)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("engine.needPmt"));
				}

				Record cur = (Record)obj;
				if (!prevRecord.isSameDataStruct(cur)) {
					col = cur.getFieldIndex(fieldName);
					prevRecord = cur;
				}

				if (col >= 0) {
					cur.setNormalFieldValue(col, val);
				}
			}
		}
	}

	public static void setFieldValues(Sequence src, String fieldName, Sequence values) {
		ListBase1 mems1 = src.getMems();
		ListBase1 mems2 = values.getMems();
		int len = mems2.size();
		if (len > mems1.size()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("field: " + mm.getMessage("engine.memCountNotMatch"));
		}

		int col = -1; // 字段在上一条记录的索引
		Record prevRecord = null; // 上一条记录

		int i = 1;
		while (i <= len) {
			Object obj = mems1.get(i);
			if (obj != null) {
				if (!(obj instanceof Record)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("engine.needPmt"));
				}

				prevRecord = (Record)obj;
				col = prevRecord.getFieldIndex(fieldName);
				if (col >= 0) {
					prevRecord.setNormalFieldValue(col, mems2.get(i));
				}
				
				i++;
				break;
			} else {
				i++;
			}
		}

		for (; i <= len; ++i) {
			Object obj = mems1.get(i);
			if (obj != null) {
				if (!(obj instanceof Record)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("engine.needPmt"));
				}

				Record cur = (Record)obj;
				if (!prevRecord.isSameDataStruct(cur)) {
					col = cur.getFieldIndex(fieldName);
					prevRecord = cur;
				}

				if (col >= 0) {
					cur.setNormalFieldValue(col, mems2.get(i));
				}
			}
		}
	}

	private static void setFieldValues(Sequence src, int field, Object value) {
		ListBase1 mems1 = src.getMems();
		for (int i = 1, len = mems1.size(); i <= len; ++i) {
			Record cur = (Record)mems1.get(i);
			if (cur != null) {
				cur.set2(field, value);
			}
		}
	}

	private static void setFieldValues(Sequence src, int field, Sequence values) {
		ListBase1 mems1 = src.getMems();
		ListBase1 mems2 = values.getMems();
		int len = mems2.size();
		if (len > mems1.size()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("field: " + mm.getMessage("engine.memCountNotMatch"));
		}

		for (int i = 1; i <= len; ++i) {
			Record cur = (Record)mems1.get(i);
			if (cur != null) {
				cur.set2(field, mems2.get(i));
			}
		}
	}
}
