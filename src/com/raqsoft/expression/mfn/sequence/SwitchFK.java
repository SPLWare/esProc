package com.raqsoft.expression.mfn.sequence;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.op.Operation;
import com.raqsoft.dm.op.Switch;
import com.raqsoft.dm.op.SwitchRemote;
import com.raqsoft.dw.compress.ColumnList;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.SequenceFunction;
import com.raqsoft.parallel.ClusterMemoryTable;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.dw.MemoryTable;

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
		Object []codes;
		Expression []exps;

		if (param.getType() == IParam.Semicolon) { // ;
			int count = param.getSubSize();
			fkNames = new String[count];
			codes = new Object[count];
			exps = new Expression[count];

			for (int i = 0; i < count; ++i) {
				IParam sub = param.getSub(i);
				parseSwitchParam(sub, i, fkNames, codes, exps, ctx);
			}
		} else {
			fkNames = new String[1];
			codes = new Object[1];
			exps = new Expression[1];
			parseSwitchParam(param, 0, fkNames, codes, exps, ctx);
		}
		
		int count = codes.length;
		Sequence []seqs = new Sequence[count];
		boolean hasClusterTable = false;
		for (int i = 0; i < count; ++i) {
			if (codes[i] instanceof Sequence) {
				seqs[i] = (Sequence)codes[i];
			} else if (codes[i] instanceof ClusterMemoryTable) {
				hasClusterTable = true;
			} else if (codes[i] == null) {
				//seqs[i] = new Sequence(0);
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("switch" + mm.getMessage("function.paramTypeError"));
			}
		}
		
		Operation op;
		if (hasClusterTable) {
			op = new SwitchRemote(this, fkNames, codes, exps, option);
		} else {
			op = new Switch(this, fkNames, seqs, exps, option);
		}
		
		return op.process(srcSequence, ctx);
	}
}
