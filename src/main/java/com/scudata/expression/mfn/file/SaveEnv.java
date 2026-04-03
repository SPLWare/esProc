package com.scudata.expression.mfn.file;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.Context;
import com.scudata.dm.Env;
import com.scudata.dm.JobSpace;
import com.scudata.dm.Table;
import com.scudata.expression.Expression;
import com.scudata.expression.FileFunction;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * 将环境变量保存到BTX
 * f.env(vi,….)
 * @author RunQian
 *
 */
public class SaveEnv extends FileFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			Table table;
			try {
				table = (Table)file.importSeries("b");
			} catch (java.io.IOException e) {
				throw new RQException(e.getMessage(), e);
			}
			
			if (table == null || table.length() != 1) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("license.fileFormatError"));
			}
			
			int fcount = table.getFieldCount();
			BaseRecord r = table.getRecord(1);
			String []names = r.getFieldNames();
			
			if (option == null) {
				for (int f = 0; f < fcount; ++f) {
					ctx.setParamValue(names[f], r.getNormalFieldValue(f));
				}
			} else if (option.indexOf('j') != -1) {
				JobSpace js = ctx.getJobSpace();
				for (int f = 0; f < fcount; ++f) {
					js.setParamValue(names[f], r.getNormalFieldValue(f));
				}
			} else if (option.indexOf('g') != -1) {
				for (int f = 0; f < fcount; ++f) {
					Env.setParamValue(names[f], r.getNormalFieldValue(f));
				}
			} else {
				for (int f = 0; f < fcount; ++f) {
					ctx.setParamValue(names[f], r.getNormalFieldValue(f));
				}
			}
			
			return Boolean.TRUE;
		}

		String []names;
		Object []values;
		if (param.isLeaf()) {
			names = new String[1];
			values = new Object[1];
			Expression exp = param.getLeafExpression();
			names[0] = exp.toString();
			values[0] = exp.calculate(ctx);
		} else {
			int size = param.getSubSize();
			names = new String[size];
			values = new Object[size];
			
			for (int i = 0; i < size; ++i) {
				IParam sub = param.getSub(i);
				if (sub == null || !sub.isLeaf()) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("env" + mm.getMessage("function.invalidParam"));
				}
				
				Expression exp = sub.getLeafExpression();
				names[i] = exp.toString();
				values[i] = exp.calculate(ctx);
			}
		}

		Table table = new Table(names, 1);
		table.newLast(values);
		file.exportSeries(table, "b", null);
		return Boolean.TRUE;
	}
}
