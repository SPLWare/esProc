package com.scudata.dw.pseudo;

import java.util.ArrayList;
import java.util.List;

import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.cursor.MemoryCursor;
import com.scudata.dm.op.Operable;
import com.scudata.dm.op.Operation;
import com.scudata.dw.ColumnTableMetaData;
import com.scudata.dw.ITableMetaData;
import com.scudata.dw.JoinCursor;
import com.scudata.expression.Expression;
import com.scudata.expression.mfn.dw.News;
import com.scudata.parallel.ClusterTableMetaData;

//T.news(cs,...)
public class PseudoNews extends Pseudo implements Operable, IPseudo {
	private Object ptable;//参数cs/A，也可能是一个虚表
	private String option;
	
	public PseudoNews() {
	}
	
	public PseudoNews(PseudoDefination pd, Object ptable, String option) {
		this.pd = pd;
		this.ptable = ptable;
		this.option = option;
		init();
		addPKeyNames();
	}
	
	public PseudoNews(PseudoDefination pd, Object ptable, Expression[] exps, String[] names, Expression filter,
			String[] fkNames, Sequence[] codes, String option) {
		this.pd = pd;
		this.ptable = ptable;
		this.exps = exps;
		this.names = names;
		this.filter = filter;
		if (fkNames != null) {
			for (String fkname : fkNames) {
				fkNameList.add(fkname);
			}
		}
		if (codes != null) {
			for (Sequence code : codes) {
				codeList.add(code);
			}
		}
		this.option = option;
		init();
		addPKeyNames();
	}

	private void init() {
		extraNameList = new ArrayList<String>();
		allNameList = new ArrayList<String>();
		String []names = getPd().getAllColNames();
		for (String name : names) {
			allNameList.add(name);
		}
	}
	
	public void addPKeyNames() {
		//addColNames(table.getAllSortedColNames());
		//ptable.addPKeyNames();
	}
	
	public void addColNames(String[] nameArray) {
		for (String name : nameArray) {
			addColName(name);
		}
	}

	public void addColName(String name) {
		if (ptable instanceof IPseudo) {
			((IPseudo)ptable).addColName(name);
		}
		if (name == null) return; 
		if (allNameList.contains(name) && !extraNameList.contains(name)) {
			extraNameList.add(name);
		}
	}

	/**
	 * 得到table.news(cursor)的游标
	 * @param table
	 * @param cursor
	 * @param fkNames
	 * @param codes
	 * @return
	 */
	private ICursor getCursor(ITableMetaData table, ICursor cursor, String []fkNames, Sequence []codes) {
		ICursor result;
		if (table instanceof ClusterTableMetaData) {
			result = ((ClusterTableMetaData)table).news(exps, names, cursor, 2, option, filter, fkNames, codes);
		} else if (JoinCursor.isColTable(table)) {
			result = (ICursor) News.news((ColumnTableMetaData)table, cursor, cursor, filter, exps,	names, fkNames, codes, option, ctx);
		} else {
			result = (ICursor) News.news((ITableMetaData)table, cursor, cursor, filter, exps,	names, fkNames, codes, ctx);
		}
		ArrayList<Operation> opList = this.opList;
		if (opList != null) {
			for (Operation op : opList) {
				result.addOperation(op, ctx);
			}
		}
		return result;
	}
	
	/**
	 * 返回T.news(cs)的游标
	 */
	public ICursor cursor(Expression[] exps, String[] names) {
		//取得ctx
		if (ctx == null) {
			if (ptable instanceof IPseudo) {
				ctx = ((IPseudo)ptable).getContext();
			} else if (ptable instanceof ICursor) {
				ctx = ((ICursor)ptable).getContext();
			}
		}
		
		//把可能的取出字段添加到虚表T里
		if (exps != null) {
			for (Expression exp : exps) {
				addColName(exp.getIdentifierName());
			}
		}
		
		//得到虚表T的实体表
		List<ITableMetaData> tables = getPd().getTables();
		int tsize = tables.size();
		
		//根据ptable得到cs（可能对应多个）
		ICursor cursors[] = new ICursor[tsize];
		if (ptable instanceof PseudoBFile) {
			cursors = ((PseudoBFile)ptable).getCursors();
		} else if (ptable instanceof PseudoTable) {
			cursors = ((PseudoTable)ptable).getCursors();
		} else if (ptable instanceof ICursor) {
			cursors[0] = (ICursor)ptable;
		} else {
			cursors[0] = new MemoryCursor((Sequence) ptable);
		}
		
		//设置取出字段
		setFetchInfo(cursors[0], exps, names);
		exps = this.exps;
		names = this.names;
		
		//组织F:K参数
		String []fkNames = null;
		Sequence []codes = null;
		if (fkNameList != null) {
			int size = fkNameList.size();
			fkNames = new String[size];
			fkNameList.toArray(fkNames);
			
			codes = new Sequence[size];
			codeList.toArray(codes);
		}
		
		if (tsize == 1) {
			return getCursor(tables.get(0), cursors[0], fkNames, codes);
		} else {
			for (int i = 0; i < tsize; i++) {
				cursors[i] = getCursor(tables.get(i), cursors[i], fkNames, codes);
			}
			return PseudoTable.mergeCursor(cursors, null, ctx);
		}
	}
	
	public Object clone(Context ctx) throws CloneNotSupportedException {
		PseudoNews obj = new PseudoNews();
		cloneField(obj);
		obj.ptable = ptable;
		obj.option = option;
		obj.ctx = ctx;
		return obj;
	}
}
