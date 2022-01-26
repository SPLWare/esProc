package com.scudata.expression.mfn.dw;

import java.io.IOException;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dw.ColumnTableMetaData;
import com.scudata.expression.TableMetaDataFunction;
import com.scudata.resources.EngineMessage;

/**
 * 更新组表数据，新键值则插入，保持键有序，组表需有主键
 * T.update(P)
 * @author RunQian
 *
 */
public class Update extends TableMetaDataFunction {
	public Object calculate(Context ctx) {
		String opt = option;
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("update" + mm.getMessage("function.missingParam"));
		}
		
		boolean hasN = opt != null && opt.indexOf('n') != -1;
		boolean hasW = opt != null && opt.indexOf('w') != -1;
		
		Object obj = param.getLeafExpression().calculate(ctx);
		if (obj instanceof Sequence) {
			try {
				if (!hasN) {
					table.update((Sequence)obj, opt);
					return table;
				}
				return table.update((Sequence)obj, opt);
			} catch (IOException e) {
				throw new RQException(e);
			}
		} else if (hasW && obj instanceof ICursor && table instanceof ColumnTableMetaData) {
			try {
				ICursor cs = (ICursor)obj;
				/**
				 * 用游标更新时不能增加记录,
				 * 所以不能出现@i
				 */
				if (opt != null && opt.indexOf('i') != -1) {
					opt = opt.replace("i", "");
				}
				
				/**
				 * 增加@u
				 */
				if (opt == null) {
					opt = "u";
				} else {
					opt += "u";
				}
				
				if (!hasN) {
					Sequence seq = cs.fetch(ICursor.FETCHCOUNT);
					while(seq != null && seq.length() > 0) {
						table.update(seq, opt);
						seq = cs.fetch(ICursor.FETCHCOUNT);
					}
					return table;
				} else {
					Sequence result = new Sequence();
					Sequence seq = cs.fetch(ICursor.FETCHCOUNT);
					while(seq != null && seq.length() > 0) {
						Sequence temp = table.update(seq, opt);
						result.addAll(temp);
						seq = cs.fetch(ICursor.FETCHCOUNT);
					}
					return result;
				}
			} catch (IOException e) {
				throw new RQException(e);
			}
		} else if (obj != null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("update" + mm.getMessage("function.paramTypeError"));
		} else {
			if (!hasN) {
				return table;
			} else {
				return null;
			}
		}
	}
}
