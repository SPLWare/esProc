package com.scudata.dw.pseudo;

import java.util.ArrayList;

import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.op.Operable;
import com.scudata.dm.op.Operation;
import com.scudata.expression.Expression;
import com.scudata.util.CursorUtil;

public class PseudoJoinx extends Pseudo {
	private IPseudo []joinxTables;
	private Expression [][]joinExps;
	private String []joinNames;
	private String option;
	//创建游标需要的参数
	private Context ctx;
	private ArrayList<Operation> opList;
	
	public PseudoJoinx() {
	}
	
	public PseudoJoinx(IPseudo []joinxTables, Expression [][]joinExps, String []joinNames, String option) {
		this.joinxTables = joinxTables;
		this.joinExps = joinExps;
		this.joinNames = joinNames;
		this.option = option;
	}
	
	public void addPKeyNames() {
		throw new RQException("never run to here");
	}
	
	public void addColNames(String[] nameArray) {
		for (IPseudo table : joinxTables) {
			table.addColNames(nameArray);
		}
	}

	public void addColName(String name) {
		for (IPseudo table : joinxTables) {
			table.addColName(name);
		}
	}

	public Operable addOperation(Operation op, Context ctx) {
		PseudoJoinx newObj = null;
		try {
			newObj = (PseudoJoinx) this.clone(ctx);
			newObj.addOpt(op, ctx);
		} catch (CloneNotSupportedException e) {
			throw new RQException(e);
		}
		return (Operable) newObj;
	}
	
	public void addOpt(Operation op, Context ctx) {
		if (opList == null) {
			opList = new ArrayList<Operation>();
		}
		
		if (op != null) {
			opList.add(op);
			ArrayList<String> tempList = new ArrayList<String>();
			new Expression(op.getFunction()).getUsedFields(ctx, tempList);
			for (String name : tempList) {
				addColName(name);
			}
		}
		
		if (this.ctx == null) {
			this.ctx = ctx;
		}
	}
	
	//exps 取出字段表达式
	//namess 取出字段新名字
	//TODO 对于joinx得到的虚表，exps，names是什么含义呢？
	public ICursor cursor(Expression []exps, String []names) {
		if (ctx == null) {
			ctx = joinxTables[0].getContext();
		}
		
		int count = joinxTables.length;
		ICursor cursors[] = new ICursor[count];
		for (int i = 0; i < count; i++) {
			cursors[i] = joinxTables[i].cursor(exps, names);
		}
		
		ICursor cursor = CursorUtil.joinx(cursors, joinNames, joinExps, option, ctx);
		ArrayList<Operation> opList = this.opList;
		if (opList != null) {
			for (Operation op : opList) {
				cursor.addOperation(op, ctx);
			}
		}
		return cursor;
	}

	public ICursor cursor(Expression []exps, String []names, boolean isColumn) {
		return cursor(exps, names);
	}
	
	public boolean isColumn(String col) {
		throw new RQException("never run to here");
	}
	
	public Context getContext() {
		return ctx;
	}
	
	public Object clone(Context ctx) throws CloneNotSupportedException {
		PseudoJoinx obj = new PseudoJoinx();
		obj.option = option;
		int size = joinxTables.length;
		obj.joinxTables = new IPseudo[size];
		for (int i = 0; i < size; i++) {
			obj.joinxTables[i] = (IPseudo) joinxTables[i].clone(ctx);
		}
		
		obj.joinExps = joinExps.clone();
		obj.joinNames = joinNames == null ? null : joinNames.clone();
		obj.ctx = ctx;
		if (opList != null) {
			obj.opList = new ArrayList<Operation>();
			for (Operation op : opList) {
				obj.opList.add(op.duplicate(ctx));
			}
		}
		return obj;
	}
	
	public void append(ICursor cursor, String option) {
		throw new RQException("never run to here");
	}

	public Sequence update(Sequence data, String opt) {
		throw new RQException("never run to here");
	}
	
	public Sequence delete(Sequence data, String opt) {
		throw new RQException("never run to here");
	}
}
