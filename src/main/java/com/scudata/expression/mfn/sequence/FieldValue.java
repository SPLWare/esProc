package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.expression.IParam;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

/**
 * 取排列指定字段的值或设置指定字段的值
 * A.field(F) A.field(F, v)
 * @author RunQian
 *
 */
public class FieldValue extends SequenceFunction {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("field" + mm.getMessage("function.missingParam"));
		}
	}

	public Object calculate(Context ctx) {
		if (param.isLeaf()) {
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
		if (src instanceof Table) {
			if (src.dataStruct().getFieldCount() > field) {
				return src.fieldValues(field);
			} else {
				int size = src.length();
				Sequence result = new Sequence(size);
				for (int i = 1; i <= size; ++i) {
					result.add(null);
				}
				
				return result;
			}
		}
		
		int size = src.length();
		Sequence result = new Sequence(size);

		for (int i = 1; i <= size; ++i) {
			BaseRecord cur = (BaseRecord)src.getMem(i);
			if (cur == null) {
				result.add(null);
			} else {
				result.add(cur.getFieldValue2(field));
			}
		}

		return result;
	}
	
	public static Sequence getFieldValues(Sequence src, String fieldName) {
		if (src instanceof Table) {
			int field = src.dataStruct().getFieldIndex(fieldName);
			if (field != -1) {
				return src.fieldValues(field);
			} else {
				int size = src.length();
				Sequence result = new Sequence(size);
				for (int i = 1; i <= size; ++i) {
					result.add(null);
				}
				
				return result;
			}
		}
		
		int size = src.length();
		Sequence result = new Sequence(size);

		int col = -1; // 字段在上一条记录的索引
		BaseRecord prevRecord = null; // 上一条记录

		int i = 1;
		while (i <= size) {
			Object obj = src.getMem(i);
			if (obj != null) {
				if (!(obj instanceof BaseRecord)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("engine.needPmt"));
				}

				prevRecord = (BaseRecord)obj;
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
			Object obj = src.getMem(i);
			if (obj != null) {
				if (!(obj instanceof BaseRecord)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("engine.needPmt"));
				}

				BaseRecord cur = (BaseRecord)obj;
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
		int size = src.length();

		int col = -1; // 字段在上一条记录的索引
		BaseRecord prevRecord = null; // 上一条记录

		int i = 1;
		while (i <= size) {
			Object obj = src.getMem(i++);
			if (obj != null) {
				if (!(obj instanceof BaseRecord)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("engine.needPmt"));
				}

				prevRecord = (BaseRecord)obj;
				col = prevRecord.getFieldIndex(fieldName);
				if (col >= 0) {
					prevRecord.setNormalFieldValue(col, val);
				}
				break;
			}
		}

		for (; i <= size; ++i) {
			Object obj = src.getMem(i);
			if (obj != null) {
				if (!(obj instanceof BaseRecord)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("engine.needPmt"));
				}

				BaseRecord cur = (BaseRecord)obj;
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
		int len = values.length();
		if (len > src.length()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("field: " + mm.getMessage("engine.memCountNotMatch"));
		}

		int col = -1; // 字段在上一条记录的索引
		BaseRecord prevRecord = null; // 上一条记录

		int i = 1;
		while (i <= len) {
			Object obj = src.getMem(i);
			if (obj != null) {
				if (!(obj instanceof BaseRecord)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("engine.needPmt"));
				}

				prevRecord = (BaseRecord)obj;
				col = prevRecord.getFieldIndex(fieldName);
				if (col >= 0) {
					prevRecord.setNormalFieldValue(col, values.getMem(i));
				}
				
				i++;
				break;
			} else {
				i++;
			}
		}

		for (; i <= len; ++i) {
			Object obj = src.getMem(i);
			if (obj != null) {
				if (!(obj instanceof BaseRecord)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("engine.needPmt"));
				}

				BaseRecord cur = (BaseRecord)obj;
				if (!prevRecord.isSameDataStruct(cur)) {
					col = cur.getFieldIndex(fieldName);
					prevRecord = cur;
				}

				if (col >= 0) {
					cur.setNormalFieldValue(col, values.getMem(i));
				}
			}
		}
	}

	private static void setFieldValues(Sequence src, int field, Object value) {
		for (int i = 1, len = src.length(); i <= len; ++i) {
			BaseRecord cur = (BaseRecord)src.getMem(i);
			if (cur != null) {
				cur.set2(field, value);
			}
		}
	}

	private static void setFieldValues(Sequence src, int field, Sequence values) {
		int len = values.length();
		if (len > src.length()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("field: " + mm.getMessage("engine.memCountNotMatch"));
		}

		for (int i = 1; i <= len; ++i) {
			BaseRecord cur = (BaseRecord)src.getMem(i);
			if (cur != null) {
				cur.set2(field, values.getMem(i));
			}
		}
	}
}
