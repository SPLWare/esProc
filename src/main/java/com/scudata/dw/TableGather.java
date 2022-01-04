package com.scudata.dw;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.ICursor;
import com.scudata.expression.Expression;
import com.scudata.expression.FieldRef;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.Moves;
import com.scudata.expression.Node;
import com.scudata.expression.ParamInfo2;
import com.scudata.expression.UnknownSymbol;
import com.scudata.expression.operator.DotOperator;
import com.scudata.resources.EngineMessage;

/**
 * 用于实现字段表达式的f()
 * @author runqian
 *
 */
class TableGather {
	private ICursor cs;//源游标
	private int calcType;//计算类型
	private Sequence data;
	private TableMetaData table;
	private GroupTableRecord curRecord;//当前记录
	private int cur;//序号
	private int len;//当前个数
	private long recSeq;//当前伪号
	private Sequence temp;
	private static String []funName = {"field","sum","count","max","min","avg","top","iterate"};
	private String []subNames;//子字段名
	
	private boolean isRow;
	
	public TableGather(TableMetaData baseTable,Expression exp, Context ctx) {
		Node home = exp.getHome();
		if (!(home instanceof DotOperator) && !(home instanceof Moves)) {
			return;//目前只解析T.C / T.f(C) / T{}
		}
		
		Object obj = home.getLeft();
		String tableName = ((UnknownSymbol)obj).getName();
		table = baseTable.getAnnexTable(tableName);
		if (table == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(tableName + mm.getMessage("dw.tableNotExist"));
		}
		isRow = table instanceof RowTableMetaData;

		obj = exp.getHome().getRight();
		String field = null;
		IFilter[] filters = null;
		if (home instanceof Moves) {
			IParam fieldParam = ((Moves) home).getParam();
			ParamInfo2 pi = ParamInfo2.parse(fieldParam, "cursor", false, false);
			String []subFields = pi.getExpressionStrs1();
			String []subNames = pi.getExpressionStrs2();
			if (subFields == null) {
				subFields = table.getColNames();
				subNames = subFields;
			} else {
				int colCount = subNames.length;
				for (int i = 0; i < colCount; ++i) {
					if (subNames[i] == null || subNames[i].length() == 0) {
						if (subFields[i] == null) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("cursor" + mm.getMessage("function.invalidParam"));
						}

						subNames[i] = subFields[i];
					}
				}
			}
			this.subNames = subNames;
			if (isRow) {
				cs = table.cursor(subFields);
				((RowCursor)cs).setFetchByBlock(true);
			} else {
				cs = new TableCursor((ColumnTableMetaData) table, subFields, filters, ctx);
			}
			temp = new Sequence(10);
			calcType = 9;
			return;
		}
		if (obj instanceof FieldRef) {
			field = ((FieldRef)obj).getName();
			calcType = 0;
		} else if (obj instanceof Function) {
			field = ((Function)obj).getParamString();
			String fname = ((Function)obj).getFunctionName();
			for (int i = 0, len = funName.length; i < len; i++) {
				if (funName[i].equals(fname)) {
					calcType = i;
					break;
				}
			}
		}
		if (isRow) {
			cs = table.cursor(new String[]{field});
		} else {
			cs = new TableCursor((ColumnTableMetaData) table, new String[]{field}, filters, ctx);
		}
		temp = new Sequence(10);
	}
	
	void setSegment(int startBlock, int endBlock) {
		((IDWCursor) cs).setSegment(startBlock, endBlock);
	}
	
	void loadData() {
		if (cs instanceof TableCursor) {
			data = ((TableCursor)cs).get(Integer.MAX_VALUE - 1);
		} else {
			data = cs.fetch();
		}
		if (data == null)
			return;
		if (data.hasRecord()) {
			curRecord = (GroupTableRecord) data.getMem(1);
			len = data.length();
			cur = 1;
			recSeq = curRecord.getRecordSeq();
		}
	}
	
	void skip() {
		cs.skip();
	}
	
	Object getNextBySeq(long seq) {
		long recSeq = this.recSeq;
		int cur = this.cur;
		int len = this.len;
		Sequence data = this.data;
		if (data == null) {
			if (calcType == 2) return 0;
			return null;
		}
		GroupTableRecord r = (GroupTableRecord) data.getMem(cur);
		
		//找到第一个相同的
		while (seq != recSeq) {
			cur++;
			if (cur > len) {
				if (calcType == 2) return 0;
				return null;
			}
			r = (GroupTableRecord) data.getMem(cur);
			recSeq = r.getRecordSeq();
		}
		
		//取出所有相同的
		Sequence temp = this.temp;
		temp.clear();
		while (seq == recSeq) {
			if (calcType == 9) {
				temp.add(r);
			} else {
				temp.add(r.getFieldValue(0));
			}
			cur++;
			if (cur > len) {
				break;
			}
			r = (GroupTableRecord) data.getMem(cur);
			recSeq = r.getRecordSeq();
		}
		
		//指向
		this.cur = cur;
		this.recSeq = recSeq;
		
		//计算
		Object result = null;
		switch (calcType) {
		case 0 : 
			result = temp.getMem(1);
			break;
		case 1 : 
			//sum
			result = temp.sum();
			break;
		case 2 : 
			//count
			result = temp.length();
			break;
		case 3 : 
			//max
			result = temp.max();
			break;
		case 4 : 
			//min
			result = temp.min();
			break;
		case 5 : 
			//avg
			result = temp.average();
			break;
		case 9 : 
			//{}
			DataStruct ds = new DataStruct(subNames);
			Table t = new Table(ds);
			t.addAll(temp);
			result = t;
			break;
		default:
			break;
		}
		return result;
		
	}
}