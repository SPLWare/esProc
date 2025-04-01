package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dw.MemoryTable;
import com.scudata.dw.compress.ColumnList;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

// 
/**
 * 把排列的外键字段值变成对应的维表的记录的引用，或者把记录的引用变成记录的主键值
 * A.switch(Fi,Ai:x;…) A.switch(Fi;...)
 * @author RunQian
 *
 */
public class SwitchFK extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (srcSequence instanceof MemoryTable && ((MemoryTable)srcSequence).isCompressTable()) {
			MemoryTable table = (MemoryTable)srcSequence;
			ColumnList mems = (ColumnList) table.getMems();
			if (param == null) {
				mems.switchFk(null, null, null, null, ctx);
				return table;
			} else if (param.isLeaf()) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("switch" + mm.getMessage("function.missingParam"));
			}
			
			String []fkNames;
			Sequence []codes;
			Expression []exps;
	
			if (param.getType() == IParam.Semicolon) { // ;
				int count = param.getSubSize();
				fkNames = new String[count];
				codes = new Sequence[count];
				exps = new Expression[count];
	
				for (int i = 0; i < count; ++i) {
					IParam sub = param.getSub(i);
					parseSwitchParam(sub, i, fkNames, codes, exps, ctx);
				}
			} else {
				fkNames = new String[1];
				codes = new Sequence[1];
				exps = new Expression[1];
				parseSwitchParam(param, 0, fkNames, codes, exps, ctx);
			}
			
			mems.switchFk(fkNames, codes, exps, option, ctx);
			return table;
		}
		
		String []fkNames;
		String []timeFkNames;
		Object []codes;
		Expression []exps;
		Expression []timeExps;

		if (param.getType() == IParam.Semicolon) { // ;
			int count = param.getSubSize();
			fkNames = new String[count];
			timeFkNames = new String[count];
			codes = new Object[count];
			exps = new Expression[count];
			timeExps = new Expression[count];

			for (int i = 0; i < count; ++i) {
				IParam sub = param.getSub(i);
				parseSwitchParam(sub, i, fkNames, timeFkNames, codes, exps, timeExps, ctx);
			}
		} else {
			fkNames = new String[1];
			timeFkNames = new String[1];
			codes = new Object[1];
			exps = new Expression[1];
			timeExps = new Expression[1];
			parseSwitchParam(param, 0, fkNames, timeFkNames, codes, exps, timeExps, ctx);
		}
		
		return srcSequence.switchFk(this, fkNames, timeFkNames, codes, exps, timeExps, option, ctx);
	}
}
