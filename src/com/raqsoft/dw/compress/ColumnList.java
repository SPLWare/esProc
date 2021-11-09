package com.raqsoft.dw.compress;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.DataStruct;
import com.raqsoft.dm.IndexTable;
import com.raqsoft.dm.HashIndexTable;
import com.raqsoft.dm.ListBase1;
import com.raqsoft.dm.Record;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.cursor.ICursor;
import com.raqsoft.dw.MemoryTable;
import com.raqsoft.expression.CurrentSeq;
import com.raqsoft.expression.Expression;
import com.raqsoft.resources.EngineMessage;

import java.math.*;

/**
 * 多个列的List
 * 列类型只支持int、long、double、BigDecimal、String、Data、TimeStamp、Time
 * @author runqian
 *
 */
public class ColumnList extends ListBase1 {
	private Column []columns;
	private DataStruct ds;

	public ColumnList() {
	}
	
	public ColumnList(DataStruct ds, Column []columns, int size) {
		this.ds = ds;
		this.columns = columns;
		this.size = size;
	}
	
	/**
	 * 把游标cs读成内存压缩表
	 * @param cs
	 */
	public ColumnList(ICursor cs) {
		Sequence data = cs.peek(1);	
		if (data == null || data.length() <= 0) {
			return;
		}
		
		Record rec = (Record) data.get(1);
		int count = rec.getFieldCount();
		ds = rec.dataStruct();
		Column []columns = new Column[count];
		this.columns = columns;
		
		int total = 0;
		data = cs.fetch(ICursor.FETCHCOUNT);
		while (data != null && data.length() > 0) {
			size  = data.length();
			for (int i = 1; i <= size; i++) {
				rec = (Record) data.get(i);
				Object []objs = rec.getFieldValues();
				for (int c = 0; c < count; c++) {
					if (columns[c] == null) {
						if (objs[c] != null) {
							Object obj = objs[c];
							if (obj instanceof Integer) {
								columns[c] = new IntColumn();
							} else if (obj instanceof Long) {
								columns[c] = new LongColumn();
							} else if (obj instanceof Double) {
								columns[c] = new DoubleColumn();
							} else if (obj instanceof BigDecimal) {
								columns[c] = new BigDecimalColumn();
							} else if (obj instanceof String) {
								columns[c] = new StringColumn();
							} else if (obj instanceof java.sql.Date) {
								columns[c] = new DateColumn();
							} else if (obj instanceof java.sql.Timestamp) {
								columns[c] = new DateTimeColumn();
							} else if (obj instanceof java.sql.Time) {
								columns[c] = new TimeColumn();
							}else if (obj instanceof Record) {
								columns[c] = new RefColumn();
							}
							for (int j = 0; j < total + i - 1; j++) {
								columns[c].addData(null);
							}
							columns[c].addData(obj);
						}
					} else {
						columns[c].addData(objs[c]);
					}
				}
			}
			
			total += size;
			data = cs.fetch(ICursor.FETCHCOUNT);
		}
		
		for (int c = 0; c < count; c++) {
			if (columns[c] == null) {
				columns[c] = new NullColumn();
			}
		}
		this.size = total;
	}
	
	/**
	 * 从游标cs读n条，存入压缩表
	 * @param cs
	 * @param n
	 */
	public ColumnList(ICursor cs, int n) {
		Sequence data = cs.peek(1);	
		if (data == null || data.length() <= 0) {
			return;
		}
		
		Record rec = (Record) data.get(1);
		int count = rec.getFieldCount();
		ds = rec.dataStruct();
		Column []columns = new Column[count];
		this.columns = columns;
		
		int total = 0;
		int rest = n;
		int fetchCount = rest >= ICursor.FETCHCOUNT ? ICursor.FETCHCOUNT : rest;
		data = cs.fetch(fetchCount);
		while (data != null && data.length() > 0) {
			size  = data.length();
			for (int i = 1; i <= size; i++) {
				rec = (Record) data.get(i);
				Object []objs = rec.getFieldValues();
				for (int c = 0; c < count; c++) {
					if (columns[c] == null) {
						if (objs[c] != null) {
							Object obj = objs[c];
							if (obj instanceof Integer) {
								columns[c] = new IntColumn();
							} else if (obj instanceof Long) {
								columns[c] = new LongColumn();
							} else if (obj instanceof Double) {
								columns[c] = new DoubleColumn();
							} else if (obj instanceof BigDecimal) {
								columns[c] = new BigDecimalColumn();
							} else if (obj instanceof String) {
								columns[c] = new StringColumn();
							} else if (obj instanceof java.sql.Date) {
								columns[c] = new DateColumn();
							} else if (obj instanceof java.sql.Timestamp) {
								columns[c] = new DateTimeColumn();
							} else if (obj instanceof java.sql.Time) {
								columns[c] = new TimeColumn();
							}else if (obj instanceof Record) {
								columns[c] = new RefColumn();
							}
							for (int j = 0; j < total + i - 1; j++) {
								columns[c].addData(null);
							}
							columns[c].addData(obj);
						}
					} else {
						columns[c].addData(objs[c]);
					}
				}
			}
			
			total += size;
			rest -= size;
			if (total >= n) {
				break;
			}
			fetchCount = rest >= ICursor.FETCHCOUNT ? ICursor.FETCHCOUNT : rest;
			data = cs.fetch(fetchCount);
		}
		
		for (int c = 0; c < count; c++) {
			if (columns[c] == null) {
				columns[c] = new NullColumn();
			}
		}
		this.size = total;
	}
	
	public DataStruct dataStruct() {
		return ds;
	}
	
	/**
	 * 读记录
	 */
	public Object get(int index) {
		Record record = new Record(ds);
		Column []columns = this.columns;
		for (int c = 0, count = columns.length; c < count; c++) {
			record.set(c, columns[c].getData(index));
		}
		return record;
	}
	
	public Object[] toArray() {
		Object[] result = new Object[size];
		int size = this.size;
		for (int i = 1; i <= size; i++) {
			result[i - 1] = get(i);
		}
		return result;
	}

	public Object[] toArray(Object a[]) {
		for (int i = 1; i <= size; i++) {
			a[i - 1] = get(i);
		}
		return a;
	}
	
	public void switchFk(String[] fkNames, Sequence[] codes, Expression[] exps, String opt, Context ctx) {
		boolean isIsect = false, isDiff = false;
		if (opt != null) {
			if (opt.indexOf('i') != -1) {
				isIsect = true;
			} else if (opt.indexOf('d') != -1) {
				isDiff = true;
			}
		}
		
		if (isIsect || isDiff) {
			switch2(fkNames, codes, exps, isDiff, ctx);
		} else {
			switch1(fkNames, codes, exps, ctx);
		}
	}

	/**
	 * 建立内存索引
	 * @param codes
	 * @param exps
	 * @param ctx
	 * @return
	 */
	private IndexTable[] getIndexTable(Sequence[] codes, Expression[] exps, Context ctx) {
		IndexTable []indexTables;
		int count = codes.length;
		indexTables = new IndexTable[count];
		for (int i = 0; i < count; ++i) {
			Sequence code = codes[i];
			Expression exp = null;
			if (exps != null && exps.length > i) {
				exp = exps[i];
			}

			if (exp == null || !(exp.getHome() instanceof CurrentSeq)) { // #
				indexTables[i] = code.getIndexTable(exp, ctx);
				if (indexTables[i] == null) {
					indexTables[i] = IndexTable.instance(code, exp, ctx);
				}
			}
		}
		
		return indexTables;
	}
	
	private void switch1(String[] fkNames, Sequence[] codes, Expression[] exps, Context ctx) {
		DataStruct ds = this.ds;
		if (ds == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.needPurePmt"));
		}
		
		int fkCount = fkNames.length;
		int []fkIndex = new int[fkCount];
		for (int f = 0; f < fkCount; ++f) {
			fkIndex[f] = ds.getFieldIndex(fkNames[f]);
			if (fkIndex[f] == -1) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(fkNames[f] + mm.getMessage("ds.fieldNotExist"));
			}
		}

		int len = size;
		IndexTable []indexTables = getIndexTable(codes, exps, ctx);
		for (int f = 0; f < fkCount; ++f) {
			int fk = fkIndex[f];
			IndexTable indexTable = indexTables[f];
			Column oldColumn = columns[fk];
			Sequence code = codes[f];
			
			if (code instanceof MemoryTable && ((MemoryTable)code).isCompressTable()) {
				SeqRefColumn column = new SeqRefColumn(code);
				if (indexTable != null) {
					for (int i = 1; i <= len; ++i) {
						Object key = oldColumn.getData(i);
						int seq = ((HashIndexTable)indexTable).findSeq(key);
						column.addData(seq);
					}
				} else { // #
					int codeLen = code.length();
					for (int i = 1; i <= len; ++i) {
						Object val = oldColumn.getData(i);
						if (val instanceof Number) {
							int seq = ((Number)val).intValue();
							if (seq > 0 && seq <= codeLen) {
								column.addData(seq);
							} else {
								column.addData(-1);
							}
						}
					}
				}
				columns[fk] = column;
			} else {
				RefColumn column = new RefColumn();
				if (indexTable != null) {
					for (int i = 1; i <= len; ++i) {
						Object key = oldColumn.getData(i);
						Object obj = indexTable.find(key);
						column.addData(obj);
					}
				} else { // #
					int codeLen = code.length();
					for (int i = 1; i <= len; ++i) {
						Object val = oldColumn.getData(i);
						if (val instanceof Number) {
							int seq = ((Number)val).intValue();
							if (seq > 0 && seq <= codeLen) {
								column.addData(code.getMem(seq));
							} else {
								column.addData(null);
							}
						}
					}
				}
				columns[fk] = column;
			}
		}
	}
	
	private void deleteNullFieldRecord(int fk) {
		Column []newColumns = columns.clone();
		for (int i = 0; i < newColumns.length; i++) {
			newColumns[i] = columns[i].clone();
		}
		int size = this.size;
		int total = 0;
		int count = columns.length;
		
		Column []columns = this.columns;
		Column col = columns[fk];
		for (int i = 1; i <= size; i++) {
			if (col.getData(i) != null) {
				for (int j = 0; j < count; j++) {
					if (newColumns[j] instanceof NullColumn) {
						continue;
					}
					if (newColumns[j] instanceof SeqRefColumn) {
						int seq = ((SeqRefColumn)columns[j]).getSeq(i);
						((SeqRefColumn)newColumns[j]).addData(seq);
					} else {
						newColumns[j].addData(columns[j].getData(i));
					}
				}
				total++;
			}
		}
		this.columns = newColumns;
		this.size = total;
	}
	
	private void switch2(String[] fkNames, Sequence[] codes, Expression[] exps, boolean isDiff, Context ctx) {
		DataStruct ds = this.ds;
		if (ds == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.needPurePmt"));
		}

		int fkCount = fkNames.length;
		int []fkIndex = new int[fkCount];
		for (int f = 0; f < fkCount; ++f) {
			fkIndex[f] = ds.getFieldIndex(fkNames[f]);
			if (fkIndex[f] == -1) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(fkNames[f] + mm.getMessage("ds.fieldNotExist"));
			}
		}
		
		
		IndexTable []indexTables = getIndexTable(codes, exps, ctx);
		
		for (int f = 0; f < fkCount; ++f) {
			int fk = fkIndex[f];
			IndexTable indexTable = indexTables[f];
			Column oldColumn = columns[fk];
			Sequence code = codes[f];
			int len = size;
			
			if (code instanceof MemoryTable && ((MemoryTable)code).isCompressTable()) {
				SeqRefColumn column = new SeqRefColumn(code);
				if (indexTable != null) {
					for (int i = 1; i <= len; ++i) {
						Object key = oldColumn.getData(i);
						int seq = ((HashIndexTable)indexTable).findSeq(key);
						if (isDiff) {
							// 找不到时保留源值
							if (seq > 0) {
								column.addData(-1);
							}
						} else {
							column.addData(seq);
						}
					}
				} else { // #
					int codeLen = code.length();
					for (int i = 1; i <= len; ++i) {
						Object val = oldColumn.getData(i);
						if (val instanceof Number) {
							int seq = ((Number)val).intValue();
							if (isDiff) {
								// 找不到时保留源值
								if (seq > 0 && seq <= codeLen) {
									column.addData(-1);
								}
							} else {
								if (seq > 0 && seq <= codeLen) {
									column.addData(seq);
								} else {
									column.addData(-1);
								}
							}
						}
					}
				}
				columns[fk] = column;
			} else {
				RefColumn column = new RefColumn();
				if (indexTable != null) {
					for (int i = 1; i <= len; ++i) {
						Object key = oldColumn.getData(i);
						Object obj = indexTable.find(key);
						if (isDiff) {
							// 找不到时保留源值
							if (obj != null) {
								column.addData(null);
							}
						} else {
							column.addData(obj);
						}
					}
				} else { // #
					int codeLen = code.length();
					for (int i = 1; i <= len; ++i) {
						Object val = oldColumn.getData(i);
						if (val instanceof Number) {
							int seq = ((Number)val).intValue();
							if (isDiff) {
								// 找不到时保留源值
								if (seq > 0 && seq <= codeLen) {
									column.addData(-1);
								}
							} else {
								if (seq > 0 && seq <= codeLen) {
									column.addData(seq);
								} else {
									column.addData(-1);
								}
							}
						}
					}
				}
				columns[fk] = column;
			}
			deleteNullFieldRecord(fk);
		}
	}

	public Column[] getColumns() {
		return columns;
	}
}
