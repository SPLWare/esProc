package com.raqsoft.expression.mfn.dw;

import java.io.IOException;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dw.ITableMetaData;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.TableMetaDataFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 取组表附表或者为组表增加附表
 * T.attach(T’,C…)
 * @author RunQian
 *
 */
public class Attach extends TableMetaDataFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("attach" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			String tableName = param.getLeafExpression().getIdentifierName();
			ITableMetaData table = this.table;
			table = table.getAnnexTable(tableName);
			if (table == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(tableName + mm.getMessage("dw.tableNotExist"));
			}

			return table;
		}
		
		IParam sub0 = param.getSub(0);
		if (sub0 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("attach" + mm.getMessage("function.invalidParam"));
		}
		
		String tableName = sub0.getLeafExpression().getIdentifierName();
		int size = param.getSubSize();
		String []fields = new String[size - 1];
		int []serialBytesLen = new int[size - 1];
		
		for (int i = 1; i < size; ++i) {
			IParam sub = param.getSub(i);
			if (sub == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("attach" + mm.getMessage("function.invalidParam"));
			} else if (sub.isLeaf()) {
				fields[i - 1] = sub.getLeafExpression().getIdentifierName();
			} else {
				IParam p0 = sub.getSub(0);
				IParam p1 = sub.getSub(1);
				if (p0 == null || p1 == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("attach" + mm.getMessage("function.invalidParam"));
				}
				
				fields[i - 1] = p0.getLeafExpression().getIdentifierName();
				Object obj = p1.getLeafExpression().calculate(ctx);
				if (!(obj instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("attach" + mm.getMessage("function.paramTypeError"));
				}
				
				serialBytesLen[i - 1] = ((Number)obj).intValue();
			}
		}
		
		try {
			return table.createAnnexTable(fields, serialBytesLen, tableName);
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		}
	}

}
