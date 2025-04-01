package com.scudata.dw.compress;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.math.*;
import java.util.Comparator;

import com.scudata.array.ArrayUtil;
import com.scudata.array.BoolArray;
import com.scudata.array.IArray;
import com.scudata.array.IntArray;
import com.scudata.array.ObjectArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.HashIndexTable;
import com.scudata.dm.IndexTable;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dw.MemoryTable;
import com.scudata.expression.CurrentSeq;
import com.scudata.expression.Expression;
import com.scudata.resources.EngineMessage;

/**
 * 多个列的List
 * 列类型只支持int、long、double、BigDecimal、String、Data、TimeStamp、Time
 * @author runqian
 *
 */
public class ColumnList implements IArray {
	private Column []columns;
	private DataStruct ds;
	private int size;
	
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
		
		BaseRecord rec = (BaseRecord) data.get(1);
		int count = rec.getFieldCount();
		ds = rec.dataStruct();
		Column []columns = new Column[count];
		this.columns = columns;
		
		int total = 0;
		data = cs.fetch(ICursor.FETCHCOUNT);
		while (data != null && data.length() > 0) {
			size  = data.length();
			for (int i = 1; i <= size; i++) {
				rec = (BaseRecord) data.get(i);
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
							}else if (obj instanceof BaseRecord) {
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
		
		BaseRecord rec = (BaseRecord) data.get(1);
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
				rec = (BaseRecord) data.get(i);
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
							}else if (obj instanceof BaseRecord) {
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

	public void toArray(Object a[]) {
		for (int i = 1; i <= size; i++) {
			a[i - 1] = get(i);
		}
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
					indexTables[i] = code.newIndexTable(exp, ctx);
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
						int seq = ((HashIndexTable)indexTable).findPos(key);
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
						int seq = ((HashIndexTable)indexTable).findPos(key);
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
	
	public void writeExternal(ObjectOutput out) throws IOException {
		// TODO Auto-generated method stub
		
	}
	
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		// TODO Auto-generated method stub
		
	}
	
	public byte[] serialize() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void fillRecord(byte[] bytes) throws IOException, ClassNotFoundException {
		// TODO Auto-generated method stub
		
	}
	
	public String getDataType() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void add(Object o) {
		// TODO Auto-generated method stub
	}
	
	public void addAll(Object[] array) {
		// TODO Auto-generated method stub
	}
	
	public void addAll(IArray array) {
		// TODO Auto-generated method stub
	}
	
	public void addAll(IArray array, int count) {
		// TODO Auto-generated method stub
	}

	/**
	 * 追加一组元素，如果类型不兼容则抛出异常
	 * @param array 元素数组
	 * @param index 要加入的数据的起始位置
	 * @param count 数量
	 */
	public void addAll(IArray array, int index, int count) {
		// TODO Auto-generated method stub
	}
	
	public void insert(int index, Object o) {
		// TODO Auto-generated method stub
	}
	
	public void insertAll(int pos, IArray array) {
		// TODO Auto-generated method stub
	}
	
	public void insertAll(int pos, Object[] array) {
		// TODO Auto-generated method stub
	}
	
	public void push(Object o) {
		// TODO Auto-generated method stub
	}

	/**
	 * 追加一个空成员（不检查容量，认为有足够空间存放元素）
	 */
	public void pushNull() {
	}
	
	public void push(IArray array, int index) {
		// TODO Auto-generated method stub
	}
	
	public void add(IArray array, int index) {
		// TODO Auto-generated method stub
	}

	public void set(int curIndex, IArray array, int index) {
		// TODO Auto-generated method stub
	}
	
	public IArray get(int[] indexArray) {
		// TODO Auto-generated method stub
		return null;
	}

	public IArray get(int start, int end) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public IArray get(IArray indexArray) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public int getInt(int index) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public long getLong(int index) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public void ensureCapacity(int minCapacity) {
		// TODO Auto-generated method stub
		
	}
	
	public boolean isNull(int index) {
		// TODO Auto-generated method stub
		return false;
	}

	
	public BoolArray isTrue() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public BoolArray isFalse() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public boolean isTrue(int index) {
		// TODO Auto-generated method stub
		return false;
	}
	
	public boolean isFalse(int index) {
		// TODO Auto-generated method stub
		return false;
	}
	
	public boolean isTemporary() {
		// TODO Auto-generated method stub
		return false;
	}
	
	public void setTemporary(boolean ifTemporary) {
		// TODO Auto-generated method stub
		
	}
	
	public void remove(int index) {
		// TODO Auto-generated method stub
		
	}
	
	public void remove(int[] seqs) {
		// TODO Auto-generated method stub
		
	}
	
	public void removeRange(int fromIndex, int toIndex) {
		// TODO Auto-generated method stub
		
	}
	
	public int size() {
		return size;
	}
	
	public int count() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public Object ifn() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void set(int index, Object obj) {
		// TODO Auto-generated method stub
	}
	
	public void clear() {
		// TODO Auto-generated method stub
		
	}
	
	public int binarySearch(Object elem) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public int binarySearch(Object elem, int start, int end) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public boolean contains(Object elem) {
		// TODO Auto-generated method stub
		return false;
	}
	
	public void contains(boolean isSorted, IArray array, BoolArray result) {
		// TODO Auto-generated method stub
		
	}
	
	public boolean objectContains(Object elem) {
		// TODO Auto-generated method stub
		return false;
	}
	
	public int firstIndexOf(Object elem, int start) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public int lastIndexOf(Object elem, int start) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public IntArray indexOfAll(Object elem, int start, boolean isSorted, boolean isFromHead) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public IArray dup() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public IArray newInstance(int count) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public IArray abs() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public IArray negate() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public IArray not() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public boolean isNumberArray() {
		// TODO Auto-generated method stub
		return false;
	}
	
	public IArray memberAdd(IArray array) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public IArray memberAdd(Object value) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public IArray memberSubtract(IArray array) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public IArray memberMultiply(IArray array) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public IArray memberMultiply(Object value) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public IArray memberDivide(IArray array) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public IArray memberMod(IArray array) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public IArray memberIntDivide(IArray array) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public BoolArray calcRelation(IArray array, int relation) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public BoolArray calcRelation(Object value, int relation) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void calcRelations(IArray array, int relation, BoolArray result, boolean isAnd) {
		// TODO Auto-generated method stub
		
	}
	
	public void calcRelations(Object value, int relation, BoolArray result, boolean isAnd) {
		// TODO Auto-generated method stub
		
	}
	
	public IArray bitwiseAnd(IArray array) {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * 计算两个数组的相对应的成员的按位或
	 * @param array 右侧数组
	 * @return 按位或结果数组
	 */
	public IArray bitwiseOr(IArray array) {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * 计算两个数组的相对应的成员的按位异或
	 * @param array 右侧数组
	 * @return 按位异或结果数组
	 */
	public IArray bitwiseXOr(IArray array) {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * 计算数组成员的按位取反
	 * @return 成员按位取反结果数组
	 */
	public IArray bitwiseNot() {
		// TODO Auto-generated method stub
		return null;
	}

	public int memberCompare(int index1, int index2) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	/**
	 * 判断数组的两个成员是否相等
	 * @param index1 成员1
	 * @param index2 成员2
	 * @return
	 */
	public boolean isMemberEquals(int index1, int index2) {
		throw new RuntimeException();
	}
	
	public int compareTo(IArray array) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public int hashCode(int index) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public Object sum() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public Object average() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public Object max() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public Object min() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void reserve(int start, int end) {
		// TODO Auto-generated method stub
		
	}
	
	public IArray split(int pos) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public IArray split(int from, int to) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void trimToSize() {
		// TODO Auto-generated method stub
		
	}
	
	public IArray select(IArray signArray) {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * 取某一区段标识数组取值为真的行组成新数组
	 * @param start 起始位置（包括）
	 * @param end 结束位置（不包括）
	 * @param signArray 标识数组
	 * @return IArray
	 */
	public IArray select(int start, int end, IArray signArray) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public boolean isEquals(int curIndex, IArray array, int index) {
		// TODO Auto-generated method stub
		return false;
	}
	
	public int compareTo(int curIndex, IArray array, int index) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public IArray memberAdd(int curIndex, IArray array, int index) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void sort() {
		// TODO Auto-generated method stub
		
	}
	
	public void sort(Comparator<Object> comparator) {
		// TODO Auto-generated method stub
		
	}
	
	public boolean hasRecord() {
		// TODO Auto-generated method stub
		return false;
	}
	
	public boolean isPmt(boolean isPure) {
		// TODO Auto-generated method stub
		return false;
	}
	
	public IArray rvs() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void removeLast() {
		// TODO Auto-generated method stub
		
	}
	
	public boolean isEquals(int curIndex, Object value) {
		// TODO Auto-generated method stub
		return false;
	}
	
	public int compareTo(int curIndex, Object value) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public IntArray ptop(int count, boolean isAll, boolean isLast, boolean ignoreNull) {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * 对数组元素从小到大做排名，取前count名的位置
	 * @param count 如果count小于0则从大到小做排名
	 * @param ignoreNull 是否忽略空元素
	 * @param iopt 是否按去重方式做排名
	 * @return IntArray
	 */
	public IntArray ptopRank(int count, boolean ignoreNull, boolean iopt) {
		return null;
	}
	
	public void setSize(int size) {
		// TODO Auto-generated method stub
		
	}
	
	public IArray get(int[] indexArray, int start, int end, boolean doCheck) {
		// TODO Auto-generated method stub
		return null;
	}

	public ObjectArray toObjectArray() {
		// TODO Auto-generated method stub
		return null;
	}

	public IArray toPureArray() {
		// TODO Auto-generated method stub
		return null;
	}

	public IArray reserve() {
		// TODO Auto-generated method stub
		return this;
	}
	
	/**
	 * 保留数组数据用于生成序列或序表
	 * @param refOrigin 引用源列，不复制数据
	 * @return
	 */

	public IArray reserve(boolean refOrigin) {
		// TODO Auto-generated method stub
		return this;
	}

	public boolean containTrue() {
		// TODO Auto-generated method stub
		return false;
	}

	public IArray combine(IArray signArray, IArray other) {
		// TODO Auto-generated method stub
		return null;
	}

	public IArray combine(IArray signArray, Object value) {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * 返回指定数组的成员在当前数组中的位置
	 * @param array 待查找的数组
	 * @param opt 选项，b：同序归并法查找，i：返回单递增数列，c：连续出现
	 * @return 位置或者位置序列
	 */
	public Object pos(IArray array, String opt) {
		return ArrayUtil.pos(this, array, opt);
	}

	/**
	 * 返回数组成员的二进制表示时1的个数和
	 * @return
	 */
	public int bit1() {
		MessageManager mm = EngineMessage.get();
		throw new RQException("bit1" + mm.getMessage("function.paramTypeError"));
	}

	/**
	 * 返回数组成员按位异或值的二进制表示时1的个数和
	 * @param array 异或数组
	 * @return 1的个数和
	 */
	public int bit1(IArray array) {
		MessageManager mm = EngineMessage.get();
		throw new RQException("bit1" + mm.getMessage("function.paramTypeError"));
	}
	
	/**
	 * 取指定位置连续相同的元素数量
	 * @param index 位置
	 * @return 连续相同的元素数量
	 */
	public int getNextEqualCount(int index) {
		throw new RuntimeException();
	}
}
