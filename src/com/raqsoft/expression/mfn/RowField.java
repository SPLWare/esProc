package com.raqsoft.expression.mfn;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.DataStruct;
import com.raqsoft.dm.Record;
import com.raqsoft.dm.Sequence;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.MemberFunction;
import com.raqsoft.resources.EngineMessage;


/**
 * 根据行号取排列的记录或字段
 * k.r(T), k.r(T, F)
 * @author RunQian
 *
 */
public class RowField extends MemberFunction {
	private int r;
	private String field;
	private DataStruct ds;
	private int col;
	
	public boolean isLeftTypeMatch(Object obj) {
		return obj instanceof Number;
	}
	
	public void setDotLeftObject(Object obj) {
		r = ((Number)obj).intValue();
	}
	
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("r" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (obj instanceof Sequence) {
				return ((Sequence)obj).getMem(r);
			} else if (obj == null) {
				return null;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("r" + mm.getMessage("function.paramTypeError"));
			}
		} else {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("r" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);
			if (sub0 == null || sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("r" + mm.getMessage("function.invalidParam"));
			}
			
			Object obj = sub0.getLeafExpression().calculate(ctx);
			if (obj instanceof Sequence) {
				obj = ((Sequence)obj).getMem(r);
				if (obj instanceof Record) {
					Record r = (Record)obj;
					if (field == null) {
						field = sub1.getLeafExpression().getIdentifierName();
						ds = r.dataStruct();
						col = ds.getFieldIndex(field);
						if (col == -1) {
							MessageManager mm = EngineMessage.get();
							throw new RQException(field + mm.getMessage("ds.fieldNotExist"));
						}
						
						return r.getNormalFieldValue(col);
					} else {
						DataStruct curDs = r.dataStruct();
						if (curDs != ds) {
							ds = curDs;
							col = curDs.getFieldIndex(field);
							if (col == -1) {
								MessageManager mm = EngineMessage.get();
								throw new RQException(field + mm.getMessage("ds.fieldNotExist"));
							}
						}
						
						return r.getNormalFieldValue(col);
					}
				} else if (obj == null) {
					return null;
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("r" + mm.getMessage("function.paramTypeError"));
				}
			} else if (obj == null) {
				return null;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("r" + mm.getMessage("function.paramTypeError"));
			}
		}
	}
}
