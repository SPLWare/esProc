package com.scudata.expression.mfn.cursor;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dw.IColumnCursorUtil;
import com.scudata.expression.CursorFunction;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * 从游标中读取数据
 * cs.fetch(n) cs.fetch(;x)
 * @author RunQian
 *
 */
public class Fetch extends CursorFunction {
	public Object calculate(Context ctx) {
		Sequence result;
		if (param == null) {
			if (option == null || option.indexOf('0') == -1) {
				result = cursor.fetch();
			} else {
				result = cursor.peek(ICursor.MAXSIZE);
			}
		} else if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (!(obj instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("fetch" + mm.getMessage("function.paramTypeError"));
			}

			int n = ((Number)obj).intValue();
			if (option == null || option.indexOf('0') == -1) {
				result = cursor.fetch(n);
			} else {
				result = cursor.peek(n);
			}
		} else {
			if (param.getType() != IParam.Semicolon || param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("fetch" + mm.getMessage("function.invalidParam"));
			}

			IParam sub = param.getSub(1);
			if (sub == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("fetch" + mm.getMessage("function.invalidParam"));
			} else if (sub.isLeaf()) {
				result = cursor.fetchGroup(sub.getLeafExpression(), ctx);
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("fetch" + mm.getMessage("function.invalidParam"));
			}
		}
		
		if (cursor.isColumnCursor()) {
			result = IColumnCursorUtil.util.convert(result);
		}
		
		if (option != null) {
			if (option.indexOf('x') != -1) {
				cursor.close();
			}
			
			if (result != null && option.indexOf('o') != -1) {
				return result.derive(option);
			}
		}
		
		return result;
	}
}

