package com.raqsoft.expression.mfn.pseudo;

import com.raqsoft.dm.Context;
import com.raqsoft.dm.DataStruct;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.Table;
import com.raqsoft.dm.cursor.ICursor;
import com.raqsoft.expression.PseudoFunction;

/**
 * 从虚表导入数据
 * pseudo.import()
 * @author RunQian
 *
 */
public class Import extends PseudoFunction {
	public Object calculate(Context ctx) {
		ICursor cursor = CreateCursor.createCursor("import", pseudo, param, ctx);
		Sequence seq = cursor.fetch();
		
		if (seq != null) {
			return seq;
		} else {
			DataStruct ds = cursor.getDataStruct();
			if (ds != null) {
				return new Table(ds);
			} else {
				return null;
			}
		}
	}
}
