package com.scudata.expression.mfn.pseudo;

import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.op.Select;
import com.scudata.dw.MemoryTable;
import com.scudata.dw.pseudo.IPseudo;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.PseudoFunction;
import com.scudata.parallel.ClusterCursor;
import com.scudata.parallel.ClusterPseudo;

/**
 * 把虚表读成内表
 * T.memory(C,…;w)
 * @author RunQian
 *
 */
public class Memory extends PseudoFunction {
	public Object calculate(Context ctx) {
		return createMemory(pseudo, param, option, ctx);
	}
	
	public static Object createMemory(IPseudo pseudo, IParam param, String option, Context ctx) {
		if (pseudo instanceof ClusterPseudo) {
			return ((ClusterPseudo)pseudo).memory(option, ctx);
		}
		return memory(option, pseudo, param, ctx);
	}
	
	private static Object memory(String option, IPseudo pseudo, IParam param, Context ctx) {
		Expression filter = null;
		if (param != null && param.getType() == IParam.Semicolon) {
			IParam expParam = param.getSub(1);
			if (expParam != null && expParam.isLeaf()) {
				filter = expParam.getLeafExpression();
			}
			param = param.getSub(0);
		}
		if (filter != null) {
			pseudo = (IPseudo) pseudo.addOperation(new Select(filter, null), ctx);
		}
		
		ICursor cursor = CreateCursor.createCursor("memory", pseudo, param, null, ctx);
		if (cursor instanceof ClusterCursor) {
			return ((ClusterCursor)cursor).memory(null, ctx);
		}
		Sequence seq = cursor.fetch();
		
		Table table;
		if (seq instanceof Table) {
			table = (Table)seq;
		} else if (seq == null) {
			return null;
		} else {
			table = seq.derive("o");
		}
		
		MemoryTable result = new MemoryTable(table);		
		return result;
	}
	
	public static Object createMemory(IPseudo pseudo, Expression []exps, String []names, Expression filter, 
			String option, Context ctx) {
		if (filter != null) {
			pseudo = (IPseudo) pseudo.addOperation(new Select(filter, null), ctx);
		}
		if (pseudo instanceof ClusterPseudo) {
			return ((ClusterPseudo)pseudo).memory(option, ctx);
		}
		
		ICursor cursor = pseudo.cursor(exps, names);
		Sequence seq = cursor.fetch();
		
		Table table;
		if (seq instanceof Table) {
			table = (Table)seq;
		} else if (seq == null) {
			return null;
		} else {
			table = seq.derive("o");
		}
		
		MemoryTable result = new MemoryTable(table);		
		return result;
	}
}
