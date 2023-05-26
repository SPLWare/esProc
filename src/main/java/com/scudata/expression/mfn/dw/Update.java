package com.scudata.expression.mfn.dw;

import java.io.IOException;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dw.ColPhyTable;
import com.scudata.expression.IParam;
import com.scudata.expression.PhyTableFunction;
import com.scudata.resources.EngineMessage;

/**
 * 更新组表数据，新键值则插入，保持键有序，组表需有主键
 * T.update(P:D)
 * @author RunQian
 *
 */
public class Update extends PhyTableFunction {
	public Object calculate(Context ctx) {
		if (table instanceof ColPhyTable) {
			ColPhyTable colTable = (ColPhyTable) table;
			if (!colTable.getGroupTable().isPureFormat()) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("dw.oldVersion2"));
			}
		}
		
		String opt = option;
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("update" + mm.getMessage("function.missingParam"));
		}
		
		boolean hasN = opt != null && opt.indexOf('n') != -1;
		boolean hasW = opt != null && opt.indexOf('w') != -1;
		Object result = null;
		Object obj, obj2 = null;
		
		if (param.getType() == IParam.Colon) {
			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);
			obj = sub0.getLeafExpression().calculate(ctx);
			obj2 = sub1 == null ? null : sub1.getLeafExpression().calculate(ctx);
			
		} else {
			obj = param.getLeafExpression().calculate(ctx);
		}
		
		if (obj instanceof Sequence) {
			try {
				if (!hasN) {
					table.update((Sequence)obj, opt);
					result = table;
				} else {
					result = table.update((Sequence)obj, opt);
				}
			} catch (IOException e) {
				throw new RQException(e);
			}
		} else if (hasW && obj instanceof ICursor && table instanceof ColPhyTable) {
			try {
				ICursor cs = (ICursor)obj;
				/**
				 * 用游标更新时不能增加记录,
				 * 所以不能出现@i
				 */
				if (opt != null && opt.indexOf('i') != -1) {
					opt = opt.replace("i", "");
				}
				((ColPhyTable)table).update(cs, opt);
				result = table;
			} catch (IOException e) {
				throw new RQException(e);
			}
		} else if (obj != null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("update" + mm.getMessage("function.paramTypeError"));
		} else {
			if (!hasN) {
				result = table;
			} else {
				result = null;
			}
		}
		
		//处理删除
		if (obj2 != null) {
			try {
				Sequence result2 = table.delete((Sequence)obj2, option);
				if (hasN) {
					((Sequence)result).addAll(result2);
				}
			} catch (IOException e) {
				throw new RQException(e);
			}
		}
		return result;
	}
}
