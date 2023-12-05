package com.scudata.expression.mfn.sequence;

import com.scudata.array.IArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.IParam;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

/**
 * 递归查询在外键中引用到指定记录的数据
 * P.nodes(F,r,n)
 * @author RunQian
 *
 */
public class Nodes extends SequenceFunction {
	public static void getAllSubs(Sequence srcSequence, String []fields, Sequence result) {
		for (int i = 1, len = srcSequence.length(); i <= len; ++i) {
			Object obj = srcSequence.getMem(i);
			if (obj instanceof BaseRecord) {
				getAllSubs((BaseRecord)obj, fields, result);
			} else if (obj instanceof Sequence) {
				getAllSubs((Sequence)obj, fields, result);
			}
		}
	}
	
	public static void getAllSubs(BaseRecord srcRecord, String []fields, Sequence result) {
		boolean sign = true;
		if (fields == null) {
			for (int f = 0, fcount = srcRecord.getFieldCount(); f < fcount; ++f) {
				Object obj = srcRecord.getNormalFieldValue(f);
				if (obj instanceof BaseRecord) {
					getAllSubs((BaseRecord)obj, null, result);
					sign = false;
				} else if (obj instanceof Sequence) {
					getAllSubs((Sequence)obj, null, result);
					sign = false;
				}
			}
		} else {
			for (String field : fields) {
				int f = srcRecord.getFieldIndex(field);
				if (f != -1) {
					Object obj = srcRecord.getNormalFieldValue(f);
					if (obj instanceof BaseRecord) {
						getAllSubs((BaseRecord)obj, null, result);
						sign = false;
					} else if (obj instanceof Sequence) {
						getAllSubs((Sequence)obj, null, result);
						sign = false;
					}
				}
			}
		}
		
		if (sign) {
			result.add(srcRecord);
		}
	}
	
	public Object calculate(Context ctx) {
		boolean isLeaf = false, isPath = false;
		if (option != null) {
			if (option.indexOf('r') != -1) {
				Sequence result = new Sequence();
				if (param == null) {
					getAllSubs(srcSequence, null, result);
				} else {
					String []fields = param.toStringArray("nodes", false);
					getAllSubs(srcSequence, fields, result);
				}
				
				return result;
			}
			
			if (option.indexOf('d') != -1) isLeaf = true;
			if (option.indexOf('p') != -1) isPath = true;
		}
		
		String field;
		BaseRecord parent = null;
		int maxLevel = 1000;
		
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("nodes" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			field = param.getLeafExpression().getIdentifierName();
		} else {
			int size = param.getSubSize();
			if (size > 3) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("nodes" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub = param.getSub(0);
			if (sub == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("nodes" + mm.getMessage("function.invalidParam"));
			}
			
			field = sub.getLeafExpression().getIdentifierName();
			sub = param.getSub(1);
			if (sub != null) {
				Object obj = sub.getLeafExpression().calculate(ctx);
				if (obj instanceof BaseRecord) {
					parent = (BaseRecord)obj;
				} else if (obj != null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("nodes" + mm.getMessage("function.paramTypeError"));
				}
			}
			
			if (size > 2) {
				sub = param.getSub(2);
				if (sub != null) {
					Object obj = sub.getLeafExpression().calculate(ctx);
					if (obj instanceof Number) {
						maxLevel = ((Number)obj).intValue();
					} else if (obj != null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("nodes" + mm.getMessage("function.paramTypeError"));
					}
				}
			}
		}

		IArray mems = srcSequence.getMems();
		int len = mems.size();
		Sequence result = new Sequence(len);
		
		if (isLeaf) {
			IArray p = srcSequence.fieldValues(field).getMems();
			for (int i = 1; i <= len; ++i) {
				BaseRecord r = (BaseRecord)mems.get(i);
				Sequence seq = r.prior(field, parent, maxLevel);
				if (seq != null && seq.length() > 0) {
					if (seq.length() == maxLevel || !p.objectContains(seq.get(1))) {
						if (isPath) {
							result.add(seq.rvs());
						} else {
							result.add(seq.get(1));
						}
					}
				}
			}
		} else {
			for (int i = 1; i <= len; ++i) {
				Object obj = mems.get(i);
				if (obj instanceof BaseRecord) {
					BaseRecord r = (BaseRecord)obj;
					Sequence seq = r.prior(field, parent, maxLevel);
					if (seq != null && seq.length() > 0) {
						if (isPath) {
							result.add(seq.rvs());
						} else {
							result.add(seq.get(1));
						}
					}
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("\".\"" + mm.getMessage("dot.tableLeft"));
				}
			}
		}
		
		
		return result;
	}
}
