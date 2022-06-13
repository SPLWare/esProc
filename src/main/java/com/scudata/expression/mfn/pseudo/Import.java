package com.scudata.expression.mfn.pseudo;

import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.ICursor;
import com.scudata.expression.PseudoFunction;

/**
 * 从虚表导入数据
 * pseudo.import()
 * @author RunQian
 *
 */
public class Import extends PseudoFunction {
	public Object calculate(Context ctx) {
//		if (pseudo.getCache() != null) {
//			return pseudo.getCache();
//		}
		
		ICursor cursor = CreateCursor.createCursor("import", pseudo, param, option, ctx);
		Sequence seq = cursor.fetch();
		//pseudo.setCache(seq);
		
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
