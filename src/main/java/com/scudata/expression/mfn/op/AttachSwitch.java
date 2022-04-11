package com.scudata.expression.mfn.op;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dm.op.Operation;
import com.scudata.dm.op.Switch;
import com.scudata.dm.op.SwitchRemote;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.OperableFunction;
import com.scudata.parallel.ClusterMemoryTable;
import com.scudata.resources.EngineMessage;

/**
 * 对游标或管道附加外键切换运算
 * op.switch(Fi,Ai:x;…) op.switch(Fi;...) op是游标或管道
 * @author RunQian
 *
 */
public class AttachSwitch extends OperableFunction {
	public Object calculate(Context ctx) {
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
			op = new Switch(this, fkNames, timeFkNames, seqs, exps, timeExps, option);
		}
		
		if (cs != null) {
			op.setCurrentCell(cs.getCurrent());
		}
		
		return operable.addOperation(op, ctx);
	}
}
