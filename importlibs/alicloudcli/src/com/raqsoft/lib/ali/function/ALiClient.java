package com.raqsoft.lib.ali.function;

import java.util.Iterator;
import java.util.List;

import com.alicloud.openservices.tablestore.SyncClient;
import com.alicloud.openservices.tablestore.model.BatchGetRowRequest;
import com.alicloud.openservices.tablestore.model.BatchGetRowResponse;
import com.alicloud.openservices.tablestore.model.Column;
import com.alicloud.openservices.tablestore.model.ColumnType;
import com.alicloud.openservices.tablestore.model.ColumnValue;
import com.alicloud.openservices.tablestore.model.Direction;
import com.alicloud.openservices.tablestore.model.GetRowRequest;
import com.alicloud.openservices.tablestore.model.GetRowResponse;
import com.alicloud.openservices.tablestore.model.MultiRowQueryCriteria;
import com.alicloud.openservices.tablestore.model.PrimaryKey;
import com.alicloud.openservices.tablestore.model.PrimaryKeyColumn;
import com.alicloud.openservices.tablestore.model.PrimaryKeyValue;
import com.alicloud.openservices.tablestore.model.RangeIteratorParameter;
import com.alicloud.openservices.tablestore.model.Row;
import com.alicloud.openservices.tablestore.model.SingleRowQueryCriteria;
import com.alicloud.openservices.tablestore.model.filter.ColumnValueFilter;
import com.alicloud.openservices.tablestore.model.filter.CompositeColumnValueFilter;
import com.alicloud.openservices.tablestore.model.filter.SingleColumnValueFilter;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.DataStruct;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.Table;
import com.raqsoft.dm.cursor.ICursor;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.Node;
import com.raqsoft.expression.UnknownSymbol;
import com.raqsoft.expression.operator.And;
import com.raqsoft.expression.operator.Equals;
import com.raqsoft.expression.operator.Greater;
import com.raqsoft.expression.operator.Not;
import com.raqsoft.expression.operator.NotEquals;
import com.raqsoft.expression.operator.NotGreater;
import com.raqsoft.expression.operator.NotSmaller;
import com.raqsoft.expression.operator.Or;
import com.raqsoft.expression.operator.Smaller;

public class ALiClient {
	private final SyncClient client;
	
	public ALiClient(String endpoint, String accessKeyId, String accessKeySecret, String instanceName) {
		client = new SyncClient(endpoint, accessKeyId, accessKeySecret, instanceName);
	}
	
	// 关闭连接释放资源
	public void close() {
		client.shutdown();
	}
	
	/**
	 * 阿里云Row转为润乾序表
	 * @param row Row 阿里云Row
	 * @param colNames String [] 字段名，如果是null则用从row里顺序取
	 * @return
	 */
	static Table toTable(Row row, String []colNames) {
		if (row == null) return null;
		
		if (colNames == null) {
			PrimaryKey pk = row.getPrimaryKey();
			int pkCount = pk.size();
			
			Column []columns = row.getColumns();
			int count = columns.length;
			
			colNames = new String[pkCount + count];
			Object []values = new Object[pkCount + count];
			
			for (int i = 0; i < pkCount; ++i) {
				PrimaryKeyColumn column = pk.getPrimaryKeyColumn(i);
				colNames[i] = column.getName();
				values[i] = toDMValue(column.getValue());
			}
			
			for (int i = 0, c = pkCount; i < count; ++i, ++c) {
				colNames[c] = columns[i].getName();
				values[c] = toDMValue(columns[i].getValue());
			}
			
			Table table = new Table(colNames);
			table.newLast(values);
			return table;
		} else {
			int count = colNames.length;
			Object []values = new Object[count];
			PrimaryKey pk = row.getPrimaryKey();
			
			for (int i = 0; i < count; ++i) {
				Column column = row.getLatestColumn(colNames[i]);
				if (column != null) {
					values[i] = toDMValue(column.getValue());
				} else {
					PrimaryKeyColumn pkColumn = pk.getPrimaryKeyColumn(colNames[i]);
					if (pkColumn != null) {
						values[i] = toDMValue(pkColumn.getValue());
					}
				}
			}
			
			Table table = new Table(colNames);
			table.newLast(values);
			return table;
		}
	}
	
	/**
	 * 阿里云Row数组转为润乾序表
	 * @param rows Row[] 阿里云Row数组
	 * @param colNames String[] 字段名，如果是null则以第一行为准
	 * @return
	 */
	static Table toTable(Row []rows, String []colNames) {
		DataStruct ds = colNames == null ? null : new DataStruct(colNames);
		return toTable(rows, ds);
	}
	
	static Table toTable(Row []rows, DataStruct ds) {
		PrimaryKey pk = rows[0].getPrimaryKey();
		int pkCount = pk.size();
		String []pkNames = new String[pkCount];
		for (int i = 0; i < pkCount; ++i) {
			pkNames[i] = pk.getPrimaryKeyColumn(i).getName();
		}
		
		int len = rows.length;
		if (ds == null) {
			Column []columns = rows[0].getColumns();
			int count = columns.length;
			String []names = new String[count];
			for (int i = 0; i < count; ++i) {
				names[i] = columns[i].getName();
			}
			
			int totalCount = pkCount + count;
			String []colNames = new String[totalCount];
			System.arraycopy(pkNames, 0, colNames, 0, pkCount);
			System.arraycopy(names, 0, colNames, pkCount, count);
			
			Table table = new Table(colNames, len);
			Object []values = new Object[totalCount];
			
			for (Row row : rows) {
				pk = row.getPrimaryKey();
				for (int i = 0; i < pkCount; ++i) {
					values[i] = toDMValue(pk.getPrimaryKeyColumn(i).getValue());
				}
				
				for (int i = 0; i < count; ++i) {
					Column column = row.getLatestColumn(names[i]);
					if (column != null) {
						values[pkCount + i] = toDMValue(column.getValue());
					} else {
						values[pkCount + i] = null;
					}
				}
				
				table.newLast(values);
			}
			
			return table;
		}

		Table table = new Table(ds, len);
		String []colNames = ds.getFieldNames();
		int count = colNames.length;
		Object []values = new Object[count];
		
		int []pkIndex = new int[count]; // 字段对应第几个主键，不是主键则为-1
		for (int i = 0; i < count; ++i) {
			pkIndex[i] = -1;
		}
		
		for (int i = 0; i < pkCount; ++i) {
			int index = ds.getFieldIndex(pkNames[i]);
			if (index != -1) {
				pkIndex[index] = i;
			}
		}
		
		for (Row row : rows) {
			for (int i = 0; i < count; ++i) {
				if (pkIndex[i] == -1) {
					Column column = row.getLatestColumn(colNames[i]);
					if (column != null) {
						values[i] = toDMValue(column.getValue());
					} else {
						values[i] = null;
					}
				} else {
					PrimaryKeyColumn pkColumn = row.getPrimaryKey().getPrimaryKeyColumn(pkIndex[i]);
					values[i] = toDMValue(pkColumn.getValue());
				}
			}
			
			table.newLast(values);
		}
		
		return table;
	}
	
	private static PrimaryKey toPrimaryKey(String keyName, Object keyValue, PrimaryKeyValue nullObj) {
		PrimaryKeyValue pkValue = toPrimaryKeyValue(keyValue, nullObj);
		PrimaryKeyColumn pkColumn = new PrimaryKeyColumn(keyName, pkValue);
		return new PrimaryKey(new PrimaryKeyColumn[]{pkColumn});
	}
	
	private static PrimaryKey toPrimaryKey(String []keyNames, Object []keyValues, PrimaryKeyValue nullObj) {
		int count = keyNames.length;
		PrimaryKeyColumn []pkColumns = new PrimaryKeyColumn[count];
		for (int i = 0; i < count; ++i) {
			PrimaryKeyValue pkValue = toPrimaryKeyValue(keyValues[i], nullObj);
			pkColumns[i] = new PrimaryKeyColumn(keyNames[i], pkValue);
		}
		
		return new PrimaryKey(pkColumns);
	}
	
	private static PrimaryKeyValue toPrimaryKeyValue(Object value, PrimaryKeyValue nullObj) {
		if (value instanceof String) {
			return PrimaryKeyValue.fromString((String)value);
		} else if (value instanceof Number) {
			return PrimaryKeyValue.fromLong(((Number)value).longValue());
		} else if (value instanceof byte[]) {
			return PrimaryKeyValue.fromBinary((byte[])value);
		} else if (value == null && nullObj != null) {
			return nullObj;
		} else {
			throw new RQException("非法的主键值");
		}
	}
	
	private static ColumnValue toColumnValue(Object value) {
		if (value instanceof String) {
			return new ColumnValue(value, ColumnType.STRING);
		} else if (value instanceof Double) {
			return new ColumnValue(value, ColumnType.DOUBLE);
		} else if (value instanceof Long) {
			return new ColumnValue(value, ColumnType.INTEGER);
		} else if (value instanceof Number) {
			return ColumnValue.fromLong(((Number)value).longValue());
		} else if (value instanceof Boolean) {
			return new ColumnValue(value, ColumnType.BOOLEAN);
		} else if (value instanceof byte[]) {
			return new ColumnValue(value, ColumnType.BINARY);
		} else {
			throw new RQException("非法的列值");
		}
	}
	
	public static Object toDMValue(ColumnValue value) {
		switch (value.getType()) {
		case STRING:
			return value.asString();
		case DOUBLE:
			return value.asDouble();
		case INTEGER:
			return value.asLong();
		case BOOLEAN:
			return value.asBoolean();
		case BINARY:
			return value.asBinary();
        default:
        	throw new RQException("不可识别的数据类型");
		}
	}
	
	public static Object toDMValue(PrimaryKeyValue value) {
		switch (value.getType()) {
		case STRING:
			return value.asString();
		case INTEGER:
			return value.asLong();
		case BINARY:
			return value.asBinary();
        default:
        	throw new RQException("不可识别的数据类型");
		}
	}
	
	private static String getFieldName(Node node) {
		if (node instanceof UnknownSymbol) {
			return ((UnknownSymbol)node).getName();
		} else {
			throw new RQException("非法的过滤表达式");
		}
	}
	
	private static ColumnValueFilter toFilter(Node node, Context ctx) {
		if (node instanceof And) {
			CompositeColumnValueFilter filter = new CompositeColumnValueFilter(CompositeColumnValueFilter.LogicOperator.AND);
			ColumnValueFilter left = toFilter(node.getLeft(), ctx);
			ColumnValueFilter right = toFilter(node.getRight(), ctx);
			filter.addFilter(left);
			filter.addFilter(right);
			return filter;
		} else if (node instanceof Or) {
			CompositeColumnValueFilter filter = new CompositeColumnValueFilter(CompositeColumnValueFilter.LogicOperator.OR);
			ColumnValueFilter left = toFilter(node.getLeft(), ctx);
			ColumnValueFilter right = toFilter(node.getRight(), ctx);
			filter.addFilter(left);
			filter.addFilter(right);
			return filter;
		} else if (node instanceof Not) {
			CompositeColumnValueFilter filter = new CompositeColumnValueFilter(CompositeColumnValueFilter.LogicOperator.NOT);
			ColumnValueFilter right = toFilter(node.getRight(), ctx);
			filter.addFilter(right);
			return filter;
		}
		
		SingleColumnValueFilter.CompareOperator operator;
		if (node instanceof Equals) {
			operator = SingleColumnValueFilter.CompareOperator.EQUAL;
		} else if (node instanceof NotSmaller) {
			operator = SingleColumnValueFilter.CompareOperator.GREATER_EQUAL;
		} else if (node instanceof Greater) {
			operator = SingleColumnValueFilter.CompareOperator.GREATER_THAN;
		} else if (node instanceof NotGreater) {
			operator = SingleColumnValueFilter.CompareOperator.LESS_EQUAL;
		} else if (node instanceof Smaller) {
			operator = SingleColumnValueFilter.CompareOperator.LESS_THAN;
		} else if (node instanceof NotEquals) {
			operator = SingleColumnValueFilter.CompareOperator.NOT_EQUAL;
		} else {
			throw new RQException("非法的过滤表达式");
		}
		
		String field = getFieldName(node.getLeft());
		Object value = node.getRight().calculate(ctx);
		ColumnValue columnValue = toColumnValue(value);
		return new SingleColumnValueFilter(field, operator, columnValue);
	}
	
	private static ColumnValueFilter toFilter(Expression exp, Context ctx) {
		if (exp == null) return null;
		
		Node node = exp.getHome();
		return node == null ? null : toFilter(node, ctx);
	}
	
	/**
	 * 查询单条记录
	 * @param tableName 表名
	 * @param keyName 主键名，如果多主键则是主键名构成的序列(Sequence)
	 * @param keyValue 主键值，如果多主键则是主键值构成的序列(Sequence)，如果查多行则是多组主键构成的序列(Sequence)
	 * @param selectCol 选出字段名，null表示选出所有字段，多字段用序列(Sequence)
	 * @param exp 字段过滤表达式，可用 &&、||、!、>、>=、==、<、<=、!=
	 * @param ctx
	 * @param opt x：查询结束后关闭连接
	 * @return
	 */
	public Table query(String tableName, Object keyName, Object keyValue, 
			Object selectCol, Expression filter, Context ctx, String opt) {
		String []selectCols = null;
		if (selectCol instanceof String) {
			selectCols = new String[] {(String)selectCol};
		} else if (selectCol instanceof Sequence) {
			Sequence seq = (Sequence)selectCol;
			selectCols = new String[seq.length()];
			seq.toArray(selectCols);
		} else if (selectCol != null) {
			throw new RQException("选出字段名称错误");
		}
		
		if (keyName instanceof String) { // 单字段主键
			if (keyValue instanceof Sequence) { // 批量取
				String name = (String)keyName;
				Object []values = ((Sequence)keyValue).toArray();
				int valCount = values.length;
				
				PrimaryKey []pks = new PrimaryKey[valCount];
				for (int i = 0; i < valCount; ++i) {
					pks[i] = toPrimaryKey(name, values[i], null);
				}
				
				return batchGetRow(tableName, pks, selectCols, filter, ctx, opt);
			} else {
				PrimaryKey pk = toPrimaryKey((String)keyName, keyValue, null);
				return getRow(tableName, pk, selectCols, filter, ctx, opt);
			}
		} else if (keyName instanceof Sequence) { // 多字段主键
			if (!(keyValue instanceof Sequence)) {
				throw new RQException("主键值和主键名不匹配");
			}
			
			Sequence valueSeq = (Sequence)keyValue;
			int valCount = valueSeq.length();
			if (valCount == 0) {
				throw new RQException("主键值和主键名不匹配");
			}
			
			Sequence nameSeq = (Sequence)keyName;
			int pkCount = nameSeq.length();
			String []keyNames = new String[pkCount];
			nameSeq.toArray(keyNames);
			
			if (valueSeq.get(1) instanceof Sequence) { // 批量取
				Sequence []seqs = new Sequence[valCount];
				valueSeq.toArray(seqs);
				
				PrimaryKey []pks = new PrimaryKey[valCount];
				for (int i = 0; i < valCount; ++i) {
					Object []values = seqs[i].toArray();
					if (values.length != pkCount) {
						throw new RQException("主键值和主键名不匹配");
					}
					
					pks[i] = toPrimaryKey(keyNames, values, null);
				}
				
				return batchGetRow(tableName, pks, selectCols, filter, ctx, opt);
			} else {
				if (valCount != pkCount) {
					throw new RQException("主键值和主键名不匹配");
				}
				
				Object []keyValues = valueSeq.toArray();
				PrimaryKey pk = toPrimaryKey(keyNames, keyValues, null);
				return getRow(tableName, pk, selectCols, filter, ctx, opt);
			}
		} else {
			throw new RQException("非法的主键名称");
		}
	}
	
	public ICursor queryRange(String tableName, Object keyName, Object startValue, Object endValue, 
			Object selectCol, Expression filter, Context ctx, String opt) {
		String []selectCols = null;
		if (selectCol instanceof String) {
			selectCols = new String[] {(String)selectCol};
		} else if (selectCol instanceof Sequence) {
			Sequence seq = (Sequence)selectCol;
			selectCols = new String[seq.length()];
			seq.toArray(selectCols);
		} else if (selectCol != null) {
			throw new RQException("选出字段名称错误");
		}

		boolean isForward = opt == null || opt.indexOf('z') == -1;
		PrimaryKey startPK, endPK;
		if (keyName instanceof String) { // 单字段主键
			String name = (String)keyName;
			if (isForward) {
				startPK = toPrimaryKey(name, startValue, PrimaryKeyValue.INF_MIN);
				endPK = toPrimaryKey(name, endValue, PrimaryKeyValue.INF_MAX);
			} else {
				startPK = toPrimaryKey(name, startValue, PrimaryKeyValue.INF_MAX);
				endPK = toPrimaryKey(name, endValue, PrimaryKeyValue.INF_MIN);
			}
		} else if (keyName instanceof Sequence) { // 多字段主键
			Sequence nameSeq = (Sequence)keyName;
			int pkCount = nameSeq.length();
			String []keyNames = new String[pkCount];
			nameSeq.toArray(keyNames);
			
			Object []startValues = new Object[pkCount];
			if (startValue instanceof Sequence) {
				Sequence seq = (Sequence)startValue;
				if (seq.length() != pkCount) {
					throw new RQException("主键值和主键名不匹配");
				}
				
				seq.toArray(startValues);
			} else if (startValue != null) {
				throw new RQException("主键值和主键名不匹配");
			}
			
			Object []endValues = new Object[pkCount];
			if (endValue instanceof Sequence) {
				Sequence seq = (Sequence)endValue;
				if (seq.length() != pkCount) {
					throw new RQException("主键值和主键名不匹配");
				}
				
				seq.toArray(endValues);
			} else if (endValue != null) {
				throw new RQException("主键值和主键名不匹配");
			}
			
			if (isForward) {
				startPK = toPrimaryKey(keyNames, startValues, PrimaryKeyValue.INF_MIN);
				endPK = toPrimaryKey(keyNames, endValues, PrimaryKeyValue.INF_MAX);
			} else {
				startPK = toPrimaryKey(keyNames, startValues, PrimaryKeyValue.INF_MAX);
				endPK = toPrimaryKey(keyNames, endValues, PrimaryKeyValue.INF_MIN);
			}
		} else {
			throw new RQException("非法的主键名称");
		}
		
		return getRange(tableName, startPK, endPK, selectCols, filter, ctx, opt);
	}

	private Table getRow(String tableName, PrimaryKey pk, 
			String []colNames, Expression exp, Context ctx, String opt) {
		SingleRowQueryCriteria criteria = new SingleRowQueryCriteria(tableName, pk);
		criteria.setMaxVersions(1);
		
		ColumnValueFilter filter = toFilter(exp, ctx);
		if (filter != null) {
			criteria.setFilter(filter);
		}
		
		if (colNames != null) {
			criteria.addColumnsToGet(colNames);
		}
		
		GetRowRequest request = new GetRowRequest(criteria);
		GetRowResponse response = client.getRow(request);
		Row row = response.getRow();
		
		if (opt != null && opt.indexOf('x') != -1) {
			close();
		}

		return toTable(row, colNames);
	}
	
	private Table batchGetRow(String tableName, PrimaryKey []pks, 
			String []colNames, Expression exp, Context ctx, String opt) {
		MultiRowQueryCriteria criteria = new MultiRowQueryCriteria(tableName);
		criteria.setMaxVersions(1);
		
		for (PrimaryKey pk : pks) {
			criteria.addRow(pk);
		}
		
		ColumnValueFilter filter = toFilter(exp, ctx);
		if (filter != null) {
			criteria.setFilter(filter);
		}
		
		if (colNames != null) {
			criteria.addColumnsToGet(colNames);
		}
		
		BatchGetRowRequest request = new BatchGetRowRequest();
		request.addMultiRowQueryCriteria(criteria);
		BatchGetRowResponse reponse = client.batchGetRow(request);
		
		List<BatchGetRowResponse.RowResult> rowList = reponse.getSucceedRows();
		if (rowList == null || rowList.size() == 0) {
			return null;
		}
		
		int size = rowList.size();
		Row []rows = new Row[size];
		for (int i = 0; i < size; ++i) {
			rows[i] = rowList.get(i).getRow();
		}
		
		if (opt != null && opt.indexOf('x') != -1) {
			close();
		}
		
		return toTable(rows, colNames);
	}
	
	private ICursor getRange(String tableName, PrimaryKey startPK, PrimaryKey endPK, 
			String []colNames, Expression exp, Context ctx, String opt) {
		RangeIteratorParameter rip = new RangeIteratorParameter(tableName);
		rip.setMaxVersions(1);
		
		rip.setInclusiveStartPrimaryKey(startPK);
		rip.setExclusiveEndPrimaryKey(endPK);
		if (opt != null && opt.indexOf('z') != -1) {
			rip.setDirection(Direction.BACKWARD);
		}
		
		ColumnValueFilter filter = toFilter(exp, ctx);
		if (filter != null) {
			rip.setFilter(filter);
		}
		
		if (colNames != null) {
			rip.addColumnsToGet(colNames);
		}
		
		Iterator<Row> iterator = client.createRangeIterator(rip);
		return new ALiCursor(this, iterator, colNames, opt, ctx);
	}
}