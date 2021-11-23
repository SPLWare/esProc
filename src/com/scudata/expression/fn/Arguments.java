package com.scudata.expression.fn;

import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.dm.Context;
import com.scudata.dm.ParamList;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;

/**
 * 返回当前网格的参数名列表
 * arguments()
 * @author RunQian
 *
 */
public class Arguments extends Function {
	public Object calculate(Context ctx) {
		if (cs instanceof PgmCellSet) {
			ParamList paramList = ((PgmCellSet)cs).getParamList();
			if (paramList != null && paramList.count() > 0) {
				int count = paramList.count();
				Sequence seq = new Sequence(count);
				
				for (int i = 0; i < count; ++i) {
					String name = paramList.get(i).getName();
					seq.add(name);
				}
				
				return seq;
			}
		}
		
		return new Sequence(0);
	}
}
