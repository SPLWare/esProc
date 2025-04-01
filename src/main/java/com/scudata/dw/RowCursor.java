package com.scudata.dw;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import com.scudata.array.IArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.ObjectReader;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.op.New;
import com.scudata.dm.op.Select;
import com.scudata.expression.CurrentElement;
import com.scudata.expression.Expression;
import com.scudata.expression.FieldRef;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.Moves;
import com.scudata.expression.Node;
import com.scudata.expression.UnknownSymbol;
import com.scudata.expression.mfn.sequence.Contain;
import com.scudata.expression.mfn.sequence.Find;
import com.scudata.expression.mfn.serial.Sbs;
import com.scudata.expression.operator.And;
import com.scudata.expression.operator.DotOperator;
import com.scudata.expression.operator.Equals;
import com.scudata.expression.operator.Greater;
import com.scudata.expression.operator.Not;
import com.scudata.expression.operator.NotEquals;
import com.scudata.expression.operator.NotGreater;
import com.scudata.expression.operator.NotSmaller;
import com.scudata.expression.operator.Or;
import com.scudata.expression.operator.Smaller;
import com.scudata.resources.EngineMessage;

/**
 * 行存基表的游标类
 * @author runqian
 *
 */
public class RowCursor extends IDWCursor {
	private RowPhyTable table;//原表
	private String []fields;//取出字段
	private DataStruct ds;//数据结构
	private String []sortedFields;//有序字段
	private Expression filter;//过滤表达式
	
	private BlockLinkReader rowReader;
	private ObjectReader segmentReader;
	
	private int startBlock; // 包含
	private int endBlock; // 不包含
	private int curBlock = 0;//当前块号
	private Sequence cache;//缓存区
	
	private long prevRecordSeq = 0; // 前一条记录的序号
	private int []findex; // 选出字段对应的字段号
	boolean needRead[];//需要读出来的列
	private boolean []isMyCol;//是否是继承字段
	ArrayList<ModifyRecord> modifyRecords;//补区记录
	private int mindex = 0;
	
	private boolean isClosed = false;
	private boolean isFirstSkip = true;
	private boolean isSegment = false;
	private Expression []exps;//表达式字段
	private TableGather []gathers;//从其它附表取f()
	private String []names;//取出名
	private boolean isField[];
	private DataStruct tempDs;
	
	//附表使用
	private RowPhyTable baseTable;
	private BlockLinkReader baseRowReader;
	private ObjectReader baseSegmentReader;
	private ArrayList<ModifyRecord> baseModifyRecords;
	private boolean isPrimaryTable; // 是否主表
	private boolean fetchByBlock = false;//按块读取
	
	private IFilter []filters;
	
	public RowCursor(RowPhyTable table) {
		this(table, null);
	}
	
	public RowCursor(RowPhyTable table, String []fields) {
		this(table, fields, null, null);
	}
	
	public RowCursor(RowPhyTable table, String []fields, Expression filter, Context ctx) {
		this.table = table;
		this.fields = fields;
		this.filter = filter;
		this.ctx = ctx;
		
		if (filter != null) {
			parseFilter(table, filter, ctx);
			Select select = new Select(filter, null);
			addOperation(select, ctx);
		}
		
		init();
	}
	
	public RowCursor(RowPhyTable table, String []fields, Expression filter, Expression []exps, String []names, Context ctx) {
		this.table = table;
		this.fields = fields;
		this.filter = filter;
		
		if (fields == null && exps != null) {
			//如果fields不存在，且表达式exps存在，则认为exps是取出
			int colCount = exps.length;
			fields = new String[colCount];
			int cnt = 0;
			for (int i = 0; i < colCount; ++i) {
				if (exps[i] == null) {
					exps[i] = Expression.NULL;
				}

				if (exps[i].getHome() instanceof UnknownSymbol) {
					fields[i] = exps[i].getIdentifierName();
					cnt++;
				}
			}
			this.fields = fields;
			if (cnt == colCount) {
				exps = null;
			}
		}
		
		if (exps != null) {
			this.exps = exps.clone();
			System.arraycopy(exps, 0, this.exps, 0, exps.length);
		}
		this.names = names;
		this.ctx = ctx;
		
		if (filter != null) {
			parseFilter(table, filter, ctx);
			Select select = new Select(filter, null);
			addOperation(select, ctx);
		}
		
		init();
	}

	public int getStartBlock() {
		return startBlock;
	}

	public int getEndBlock() {
		return endBlock;
	}

	public void setEndBlock(int endBlock) {
		this.endBlock = endBlock;
	}
	
	/**
	 * 设置分段startBlock包含，endBlock不包含
	 */
	public void setSegment(int startBlock, int endBlock) {
		isSegment = true;
		this.startBlock = startBlock;
		this.curBlock = startBlock;
		this.endBlock = endBlock;
		
		if (startBlock == 0 || startBlock >= endBlock) {
			return;
		}

		BlockLinkReader rowReader = this.rowReader;
		ObjectReader segmentReader = this.segmentReader;
		int keyCount = table.getAllSortedColNamesLength();
		long prevRecordSeq = 0;
		
		BlockLinkReader baseRowReader;
		ObjectReader baseSegmentReader;
		int baseKeyCount;
		
		try {
			
			for (int i = 0; i < startBlock; ++i) {
				prevRecordSeq += segmentReader.readInt32();
				segmentReader.readLong40();
				for (int f = 0; f < keyCount; ++f) {
					segmentReader.skipObject();
					segmentReader.skipObject();
				}
			}
	
			long segPos = segmentReader.position();
			segmentReader.readInt32();
			long pos = segmentReader.readLong40();
			for (int f = 0; f < keyCount; ++f) {
				segmentReader.skipObject();
				segmentReader.skipObject();
			}
			rowReader.seek(pos);
			
			this.segmentReader.close();
			this.segmentReader = table.getSegmentObjectReader();
			this.segmentReader.seek(segPos);
			this.prevRecordSeq = prevRecordSeq;
			
			if (!isPrimaryTable) {
				baseRowReader = this.baseRowReader;
				baseSegmentReader = this.baseSegmentReader;
				baseKeyCount = baseTable.getAllSortedColNamesLength();
				
				for (int i = 0; i < startBlock; ++i) {
					baseSegmentReader.readInt32();
					baseSegmentReader.readLong40();
					for (int f = 0; f < baseKeyCount; ++f) {
						baseSegmentReader.skipObject();
						baseSegmentReader.skipObject();
					}
				}
				
				segPos = baseSegmentReader.position();
				baseSegmentReader.readInt32();
				pos = baseSegmentReader.readLong40();
				for (int f = 0; f < baseKeyCount; ++f) {
					baseSegmentReader.skipObject();
					baseSegmentReader.skipObject();
				}
				baseRowReader.seek(pos);
				
				this.baseSegmentReader.close();
				this.baseSegmentReader = baseTable.getSegmentObjectReader();
				this.baseSegmentReader.seek(segPos);
			}
		
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		}
	}

	/**
	 * 根据计算节点得到相关列
	 * @param table
	 * @param node
	 * @return
	 */
	private static String getColumn(RowPhyTable table, Node node) {
		if (node instanceof UnknownSymbol) {
			String keyName = ((UnknownSymbol)node).getName();
			if (table.isDim(keyName)) {
				return keyName;
			} else {
				return null;
			}
		} else if (node instanceof DotOperator && node.getLeft() instanceof CurrentElement && 
				node.getRight() instanceof FieldRef) { // ~.key
			FieldRef fieldNode = (FieldRef)node.getRight();
			String keyName = fieldNode.getName();
			if (table.isDim(keyName)) {
				return keyName;
			} else {
				return null;
			}
		} else if (node instanceof Moves) {
			Node left = node.getLeft();
			if (left instanceof Moves) {
				return null;
			} else {
				return getColumn(table, left);
			}
		} else if (node instanceof com.scudata.expression.fn.math.And) {
			String col = getColumn(table, ((Function) node).getParam().getSub(0).getLeafExpression().getHome());
			if (col == null) {
				col = getColumn(table, ((Function) node).getParam().getSub(1).getLeafExpression().getHome());
			}
			return col;
		} else {
			return null;
		}
	}

	/**
	 * 把两个计算合并为一个 与 运算
	 * @param node
	 * @param left
	 * @param right
	 * @return
	 */
	private static Object combineAnd(Node node, Object left, Object right) {
		if (left instanceof ColumnsOr && right instanceof ColumnsOr) {
			return node;
		}
		if (left instanceof ColumnsOr) {
			left = ((ColumnsOr) left).getNode();
		}
		if (right instanceof ColumnsOr) {
			right = ((ColumnsOr) right).getNode();
		}
		
		if (left instanceof IFilter) {
			if (right instanceof IFilter) {
				IFilter f1 = (IFilter)left;
				IFilter f2 = (IFilter)right;
				if (f1.isSameColumn(f2)) {
					return new LogicAnd(f1, f2, f1.columnName);
				} else {
					ArrayList<Object> filterList = new ArrayList<Object>();
					filterList.add(f1);
					filterList.add(f2);
					return filterList;
				}
			} else if (right instanceof Node) {
				ArrayList<Object> filterList = new ArrayList<Object>();
				filterList.add(left);
				filterList.add(right);
				return filterList;
			} else {
				IFilter filter = (IFilter)left;
				@SuppressWarnings("unchecked")
				ArrayList<Object> filterList = (ArrayList<Object>)right;
				for (int i = 0, size = filterList.size(); i < size; ++i) {
					Object obj = filterList.get(i);
					if (obj instanceof IFilter && filter.isSameColumn((IFilter)obj)) {
						LogicAnd and = new LogicAnd(filter, (IFilter)obj, filter.columnName);
						filterList.set(i, and);
						return filterList;
					}
				}
				
				filterList.add(filter);
				return filterList;
			}
		} else if (left instanceof Node) {
			if (right instanceof IFilter) {
				ArrayList<Object> filterList = new ArrayList<Object>();
				filterList.add(left);
				filterList.add(right);
				return filterList;
			} else if (right instanceof Node) {
				return node;
			} else {
				@SuppressWarnings("unchecked")
				ArrayList<Object> filterList = (ArrayList<Object>)right;
				filterList.add(left);
				return filterList;
			}
		} else { // ArrayList<IFilter>
			@SuppressWarnings("unchecked")
			ArrayList<Object> filterList = (ArrayList<Object>)left;
			if (right instanceof IFilter) {
				IFilter filter = (IFilter)right;
				for (int i = 0, size = filterList.size(); i < size; ++i) {
					Object obj = filterList.get(i);
					if (obj instanceof IFilter && filter.isSameColumn((IFilter)obj)) {
						LogicAnd and = new LogicAnd(filter, (IFilter)obj, filter.columnName);
						filterList.set(i, and);
						return filterList;
					}
				}
				
				filterList.add(filter);
				return filterList;
			} else if (right instanceof Node) {
				filterList.add(right);
				return filterList;
			} else {
				@SuppressWarnings("unchecked")
				ArrayList<Object> filterList2 = (ArrayList<Object>)right;
				int size = filterList.size();
				
				Next:
				for (int i = 0, size2 = filterList2.size(); i < size2; ++i) {
					Object obj = filterList2.get(i);
					if (obj instanceof IFilter) {
						IFilter filter = (IFilter)obj;
						for (int j = 0; j < size; ++j) {
							obj = filterList.get(j);
							if (obj instanceof IFilter && filter.isSameColumn((IFilter)obj)) {
								LogicAnd and = new LogicAnd((IFilter)obj, filter, filter.columnName);
								filterList.set(j, and);
								continue Next;
							}
						}
						
						filterList.add(filter);
					} else {
						filterList.add(obj);
					}
				}
				
				return filterList;
			}
		}
	}
	
	/**
	 * 把两个计算合并为一个 或 运算
	 * @param node
	 * @param left
	 * @param right
	 * @return
	 */
	private static Object combineOr(Node node, Object left, Object right) {
		if (left instanceof IFilter) {
			if (right instanceof IFilter) {
				IFilter f1 = (IFilter)left;
				IFilter f2 = (IFilter)right;
				if (f1.isSameColumn(f2)) {
					return new LogicOr(f1, f2, f1.columnName);
				} else {
					ColumnsOr colsOr = new ColumnsOr();
					colsOr.addFilter(f1);
					colsOr.addFilter(f2);
					colsOr.setNode(node);
					return colsOr;
				}
			} else if (right instanceof ColumnsOr) {
				IFilter f1 = (IFilter)left;
				ColumnsOr colsOr = (ColumnsOr)right;
				colsOr.addFilter(f1);
				colsOr.setNode(node);
				return colsOr;
			} else {
				return node;
			}
		} else if (left instanceof ColumnsOr) {
			if (right instanceof IFilter) {
				ColumnsOr colsOr = (ColumnsOr)left;
				IFilter f2 = (IFilter)right;
				colsOr.addFilter(f2);
				colsOr.setNode(node);
				return colsOr;
			} else if (right instanceof ColumnsOr) {
				ColumnsOr colsOr1 = (ColumnsOr)left;
				ColumnsOr colsOr2 = (ColumnsOr)right;
				colsOr1.combineColumnsOr(colsOr2);
				colsOr1.setNode(node);
				return colsOr1;
			} else {
				return node;
			}
		} else {
			return node;
		}
	}

	/**
	 * 解析过滤表达式，提取可能的列过滤器
	 * @param table
	 * @param exp
	 * @param ctx
	 */
	private void parseFilter(RowPhyTable table, Expression exp, Context ctx) {
		Object obj = parseFilter(table, exp.getHome(), ctx);
		
		if (obj instanceof IFilter) {
			filters = new IFilter[] {(IFilter)obj};
		} else if (obj instanceof ArrayList) {
			@SuppressWarnings("unchecked")
			ArrayList<Object> list = (ArrayList<Object>)obj;
			ArrayList<IFilter> filterList = new ArrayList<IFilter>();
			Node node = null;
			for (Object f : list) {
				if (f instanceof IFilter) {
					filterList.add((IFilter)f);
				} else {
					if (node == null) {
						node = (Node)f;
					} else {
						And and = new And();
						and.setLeft(node);
						and.setRight((Node)f);
						node = and;
					}
				}
			}
			
			int size = filterList.size();
			if (size > 0) {
				filters = new IFilter[size];
				filterList.toArray(filters);
				Arrays.sort(filters);

			}
		} else if (obj instanceof ColumnsOr) {
			ArrayList<ModifyRecord> modifyRecords = table.getModifyRecords();
			if (modifyRecords != null || exps != null) {
			} else {
				//目前只优化没有补区和没有表达式的情况
				filters = ((ColumnsOr)obj).toArray();
			}
		}
	}
	
	/**
	 * 提取contain和 Not contain表达式的列过滤器
	 * @param table
	 * @param node
	 * @param ctx
	 * @return
	 */
	private static Object parseContain(RowPhyTable table, Node node, Context ctx) {
		if (node instanceof DotOperator) {
			if (node.getRight() instanceof Contain) {
				Contain contain = (Contain)node.getRight();
				IParam param = contain.getParam();
				if (param == null || !param.isLeaf()) {
					return node;
				}
				
				String column = getColumn(table, param.getLeafExpression().getHome());
				if (column == null) {
					return node;
				}
				
				try {
					Object val = node.getLeft().calculate(ctx);
					if (val instanceof Sequence) {
						int pri = table.getColumnFilterPriority(column);
						return new ContainFilter(column, pri, (Sequence)val, contain.getOption());
					} else {
						return node;
					}
				} catch (Exception e) {
					return node;
				}
			} else if (node.getRight() instanceof Find) {
				Find find = (Find)node.getRight();
				IParam param = find.getParam();
				if (param == null || !param.isLeaf()) {
					return node;
				}
				
				String column = getColumn(table, param.getLeafExpression().getHome());
				if (column == null) {
					return node;
				}
				
				try {
					Object val = node.getLeft().calculate(ctx);
					if (val instanceof Sequence) {
						int pri = table.getColumnFilterPriority(column);
						return new FindFilter(column, pri, (Sequence)val);
					} else {
						return node;
					}
				} catch (Exception e) {
					return node;
				}
			}
		} else if (node instanceof Not && node.getRight() instanceof DotOperator) {
			DotOperator dotNode = (DotOperator)node.getRight();
			if (dotNode.getRight() instanceof Contain) {
				Contain contain = (Contain)dotNode.getRight();
				IParam param = contain.getParam();
				if (param == null || !param.isLeaf()) {
					return node;
				}
				
				String column = getColumn(table, param.getLeafExpression().getHome());
				if (column == null) {
					return node;
				}
				
				try {
					Object val = dotNode.getLeft().calculate(ctx);
					if (val instanceof Sequence) {
						int pri = table.getColumnFilterPriority(column);
						return new NotContainFilter(column, pri, (Sequence)val, contain.getOption());
					} else {
						return node;
					}
				} catch (Exception e) {
					return node;
				}
			} else if (dotNode.getRight() instanceof Find) {
				Find find = (Find)dotNode.getRight();
				IParam param = find.getParam();
				if (param == null || !param.isLeaf()) {
					return node;
				}
				
				String column = getColumn(table, param.getLeafExpression().getHome());
				if (column == null) {
					return node;
				}
				
				try {
					Object val = dotNode.getLeft().calculate(ctx);
					if (val instanceof Sequence) {
						int pri = table.getColumnFilterPriority(column);
						return new NotFindFilter(column, pri, (Sequence)val);
					} else {
						return node;
					}
				} catch (Exception e) {
					return node;
				}
			}
		}
		
		return node;
	}
	
	/**
	 * 提取列过滤器
	 * @param table
	 * @param node
	 * @param ctx
	 * @return
	 */
	public static Object parseFilter(RowPhyTable table, Node node, Context ctx) {
		if (node instanceof And) {
			Object left = parseFilter(table, node.getLeft(), ctx);
			Object right = parseFilter(table, node.getRight(), ctx);
			return combineAnd(node, left, right);
		} else if (node instanceof Or) {
			Object left = parseFilter(table, node.getLeft(), ctx);
			Object right = parseFilter(table, node.getRight(), ctx);
			return combineOr(node, left, right);
		} else {
			int operator;
			if (node instanceof Equals) {
				operator = IFilter.EQUAL;
			} else if (node instanceof Greater) {
				operator = IFilter.GREATER;
			} else if (node instanceof NotSmaller) {
				operator = IFilter.GREATER_EQUAL;
			} else if (node instanceof Smaller) {
				operator = IFilter.LESS;
			} else if (node instanceof NotGreater) {
				operator = IFilter.LESS_EQUAL;
			} else if (node instanceof NotEquals) {
				operator = IFilter.NOT_EQUAL;
			} else {
				return parseContain(table, node, ctx);
			}
			
			Node left = node.getLeft();
			String column = getColumn(table, left);
			if (column != null) {
				try {
					Object value = node.getRight().calculate(ctx);
					int pri = table.getColumnFilterPriority(column);
					
					if (left instanceof com.scudata.expression.fn.math.And) {
						Expression expr = ((Function) left).getParam().getSub(0).getLeafExpression();
						if (expr.getHome() instanceof UnknownSymbol) {
							expr = ((Function) left).getParam().getSub(1).getLeafExpression();
						}
						return new AndFilter(column, pri, operator, expr.calculate(ctx), value);
					} else {
						return new ColumnFilter(column, pri, operator, value);
					}
				} catch(Exception e) {
					return node;
				}
			}
			
			Node right = node.getRight();
			column = getColumn(table, right);
			if (column != null) {
				try {
					Object value = left.calculate(ctx);
					int pri = table.getColumnFilterPriority(column);
					operator = IFilter.getInverseOP(operator);
					return new ColumnFilter(column, pri, operator, value);
				} catch(Exception e) {
					return node;
				}
			}

			return node;
		}
	}
	
	/**
	 * 初始化
	 */
	private void init() {
		try {
			table.appendCache();
		} catch (IOException e) {
			throw new RQException(e);
		}
		
		// 检查不可识别的表达式里是否引用了没有选出的字段，如果引用了则加入到选出字段里
		Expression filter = this.filter;
		if (filter != null) {
			ArrayList<String> colList = new ArrayList<String>();
			String []cols = table.getAllColNames();
			for (String col : cols) {
				colList.add(col);
			}
			ArrayList<String> nameList = new ArrayList<String>();
			filter.getUsedFields(ctx, nameList);
			if (nameList.size() > 0 && fields != null) {
				ArrayList<String> selectList = new ArrayList<String>();
				for (String name : fields) {
					selectList.add(name);
				}
				
				for (String name : nameList) {
					if (!selectList.contains(name) && colList.contains(name)) {
						selectList.add(name);
					}
				}
				
				int oldLen = fields.length;
				if (selectList.size() > oldLen) {
					String []newFields = new String[selectList.size()];
					selectList.toArray(newFields);
					Expression []newExps = new Expression[oldLen];
					for (int i = 1; i <= oldLen; ++i) {
						newExps[i - 1] = new Expression("#" + i);
					}
					
					int newLen = selectList.size();
					if (exps != null) {
						exps = Arrays.copyOf(exps, newLen);
						for (int i = oldLen; i < newLen; ++i) {
							exps[i] = new Expression(newFields[i]);
						}
					}
					String []newNames = null;
					if (names != null) {
						newNames = names;
						names = null;
					} else {
						newNames = fields;
					}
					
					New newOp = new New(newExps, newNames, null);
					addOperation(newOp, ctx);
					fields = newFields;
				}
			}
		}
		
		if (filters != null) {
			String []cols = table.getAllSortedColNames();
			int keyCount = cols.length;
			IFilter []filters = new IFilter[keyCount];
			for (int i = 0; i < keyCount; i++) {
				for (IFilter f : this.filters)
				if (cols[i].equals(f.columnName)) {
					filters[i] = f;
					break;
				}
			}
			this.filters = filters;
		}
		
		endBlock = table.getDataBlockCount();
		if (fields == null) {
			fields = table.getAllColNames();
		}
		ds = new DataStruct(fields);
		setDataStruct(ds);
		
		int colCount = fields.length;
		rowReader = table.getRowReader(true);
		segmentReader = table.getSegmentObjectReader();
		
		modifyRecords = table.getModifyRecords();
		DataStruct srcDs = table.getDataStruct();
		int allCount = table.getAllColNames().length;
		needRead = new boolean[allCount];//需要读的列
		findex = new int[colCount];
		for (int i = 0; i < colCount; ++i) {
			 int id = srcDs.getFieldIndex(fields[i]);
			 findex[i] = id;
			 if (id >= 0) needRead[id] = true;
		}
		
		
		isMyCol = new boolean[colCount];
		
		isPrimaryTable = table.parent == null;
		if (!isPrimaryTable) {
			baseTable = (RowPhyTable) table.parent;
			baseRowReader = baseTable.getRowReader(true);
			baseSegmentReader = baseTable.getSegmentObjectReader();
			baseModifyRecords = baseTable.getModifyRecords();
			
			//获得在主表里的index
			srcDs = baseTable.getDataStruct();
			for (int i = 0; i < colCount; ++i) {
				 int id = srcDs.getFieldIndex(fields[i]);
				 if (id != -1) {
					 findex[i] = id;
				 }
			}
			
			String []primaryTableKeys = table.parent.getColNames();
			ArrayList<String> primaryTableKeyList = new ArrayList<String>();
			for (String name : primaryTableKeys) {
				primaryTableKeyList.add(name);
			}
			
			for (int i = 0; i < colCount; i++) {
				if (primaryTableKeyList.contains(fields[i])) {
					isMyCol[i] = false;
				} else {
					isMyCol[i] = true;
				}
			}
		} else {
			for (int i = 0; i < colCount; i++) {
				isMyCol[i] = true;
			}
		}

		if (exps != null) {
			int size  = exps.length;
			gathers = new TableGather[size];
			isField = new boolean[size];
			for (int i = 0; i < size; i++) {
				if (exps[i] != null) {
					if (exps[i].getHome() instanceof DotOperator) {
						Node right = exps[i].getHome().getRight();
						if (!(right instanceof Sbs)) {
							gathers[i] = new TableGather(table, exps[i], ctx);
							exps[i] = null;
						}
					} else if (exps[i].getHome() instanceof UnknownSymbol) {
						exps[i] = null;
						isField[i] = true;
					} else if (exps[i].getHome() instanceof Moves) {
						Node left = exps[i].getHome().getLeft();
						if (left instanceof UnknownSymbol) {
							if (table.isSubTable(((UnknownSymbol) left).getName())) {
								gathers[i] = new TableGather(table, exps[i], ctx);
								exps[i] = null;
							}
						}
					}
				}
			}
			tempDs = new DataStruct(table.getAllColNames());
		}

		for (int i = 0; i < colCount; i++) {
			if (-1 == findex[i] && isField[i])
			{
				MessageManager mm = EngineMessage.get();
				throw new RQException(fields[i] + mm.getMessage("ds.fieldNotExist"));
			}
		}
		
		if (names != null) {
			int len = names.length;
			for (int i = 0; i < len; i++) {
				if (names[i] == null) {
					names[i] = ds.getFieldName(i);
				}
			}
			ds = new DataStruct(names);
			setDataStruct(ds);
		}
		
		if (table.hasPrimaryKey()) {
			// 如果附表有主键并且主键被选出则给数据结构设上主键
			String []keys = table.getAllSortedColNames();
			ArrayList<String> pkeyList = new ArrayList<String>();
			ArrayList<String> sortedFieldList = new ArrayList<String>();
			DataStruct temp;
			if (fields != null) {
				temp = new DataStruct(fields);
			} else {
				temp = ds;
			}
			boolean sign = true;
			for (String key : keys) {
				int idx = temp.getFieldIndex(key);
				if (idx == -1) {
					sign = false;
					break;
				} else {
					pkeyList.add(ds.getFieldName(idx));
					sortedFieldList.add(ds.getFieldName(idx));
				}
			}
			
			if (sign) {
				//有主键
				int size = pkeyList.size();
				String[] pkeys = new String[size];
				pkeyList.toArray(pkeys);
				ds.setPrimary(pkeys);
			}
			int size = sortedFieldList.size();
			if (size > 0) {
				//有序字段
				sortedFields = new String[size];
				sortedFieldList.toArray(sortedFields);
			}
		} else if (table.isSorted) {
			// 如果附表有序则组织有序字段
			String []keys = table.getAllSortedColNames();
			ArrayList<String> sortedFieldList = new ArrayList<String>();
			DataStruct temp;
			if (fields != null) {
				temp = new DataStruct(fields);
			} else {
				temp = ds;
			}
			for (String key : keys) {
				int idx = temp.getFieldIndex(key);
				if (idx == -1) {
					break;
				} else {
					sortedFieldList.add(ds.getFieldName(idx));
				}
			}
			int size = sortedFieldList.size();
			if (size > 0) {
				//有序字段
				sortedFields = new String[size];
				sortedFieldList.toArray(sortedFields);
			}
		}
	}
	
	protected Sequence get(int n) {
		if (isClosed || n < 1) {
			return null;
		}
		
		isFirstSkip = false;
		
		if (modifyRecords != null && mindex < modifyRecords.size()) {
			if (exps != null) {
				return getModify2(n);
			} else {
				return getModify(n);
			}
		}
		
		Sequence cache = this.cache;
		if (cache != null) {
			int len = cache.length();
			if (len > n) {
				this.cache = cache.split(n + 1);
				return cache;
			} else if (len == n) {
				this.cache = null;
				return cache;
			}
		} else {
			cache = new Table(ds, ICursor.FETCHCOUNT);
		}
		
		int curBlock = this.curBlock;
		int endBlock = this.endBlock;
		int colCount = this.fields.length;
		int allCount = table.getAllColNames().length;
		int keyCount = table.getAllSortedColNamesLength();
		Object []values = new Object[allCount];
		int []findex = this.findex;
		boolean []needRead = this.needRead;
		BlockLinkReader rowReader = this.rowReader;
		ObjectReader segmentReader = this.segmentReader;
		RowBufferReader bufReader;
		long seq = 0;
		
		BlockLinkReader baseRowReader = null;
		ObjectReader baseSegmentReader = null;
		RowBufferReader baseBufReader = null;
		int baseAllCount = 0;
		int baseKeyCount = 0;
		long baseSeq = 0;
		Object []baseValues = null;
		ArrayList<ModifyRecord> baseModifyRecords = null;
		boolean isPrimaryTable = this.isPrimaryTable;
		if (!isPrimaryTable) {
			baseRowReader = this.baseRowReader;
			baseSegmentReader = this.baseSegmentReader;
			baseAllCount = baseTable.getColNames().length;
			baseKeyCount = baseTable.getSortedColNames().length;
			baseValues = new Object[baseAllCount];
			baseModifyRecords = this.baseModifyRecords;
		}
		boolean []isMyCol = this.isMyCol;
		
		DataStruct ds = this.ds;		
		IArray mems = cache.getMems();
		this.cache = null;
		
		TableGather []gathers = null;
		Record temp = null;
		Context ctx = null;
		Expression []exps = this.exps;
		if (exps != null) {
			gathers = this.gathers;
			temp = new Record(this.tempDs);
			ctx = this.ctx;
		}
		
		try {
			while (curBlock < endBlock) {
				curBlock++;
				int recordCount = segmentReader.readInt32();
				if (isPrimaryTable && filters != null) {
					IFilter []filters = this.filters;
					boolean sign = true;
					long pos = segmentReader.readLong40();
					for (int i = 0; i < keyCount - baseKeyCount; ++i) {
						Object minValue = segmentReader.readObject();
						Object maxValue = segmentReader.readObject();
						if (filters[i] != null && !filters[i].match(minValue, maxValue)) {
							sign = false;
						}
					}
					if (!sign) {
						continue;
					}
					rowReader.seek(pos);
				} else {
					segmentReader.readLong40();
					for (int i = 0; i < keyCount - baseKeyCount; ++i) {
						segmentReader.skipObject();
						segmentReader.skipObject();
					}
				}
				
				bufReader = rowReader.readBlockBuffer();
				
				if (gathers != null) {
					for (TableGather gather : gathers) {
						if (gather != null) {
							gather.loadData();
						}
					}
				}
				
				if (!isPrimaryTable) {
					baseSegmentReader.readInt32();
					baseSegmentReader.readLong40();
					for (int i = 0; i < baseKeyCount; ++i) {
						baseSegmentReader.skipObject();
						baseSegmentReader.skipObject();
					}
					baseBufReader = baseRowReader.readBlockBuffer();
					baseSeq = (Long) baseBufReader.readObject();
					for (int k = 0; k < baseAllCount; ++k) {
						baseValues[k] = baseBufReader.readObject();
					}
				}

				int diff = n - cache.length();
				if (recordCount > diff) {
					int i = 0;
					for (; i < diff; ++i) {
						//读出来一整行
						long recNum = (Long) bufReader.readObject();//跳过伪号
						if (!isPrimaryTable) {
							seq = (Long) bufReader.readObject();//导列伪号
						}
						for (int f = 0; f < allCount; ++f) {
							if (f >= baseKeyCount) {
								if (needRead[f]) {
									values[f] = bufReader.readObject();
								} else {
									bufReader.skipObject();
								}
							}
						}

						if (!isPrimaryTable) {
							//主表里找对应的
							while (seq != baseSeq) {
								baseSeq = (Long) baseBufReader.readObject();
								//找到了读出来
								for (int k = 0; k < baseAllCount; ++k) {
									baseValues[k] = baseBufReader.readObject();
								}
								filterByModifyRecord(baseSeq, baseModifyRecords, baseValues); 
							}
						}
						
						//用取出字段组成记录
						ComTableRecord r = new ComTableRecord(ds);
						if (exps != null) {
							temp.setStart(0, values);
							if (!isPrimaryTable) {
								for (int f = 0; f < colCount; ++f) {
									if (exps[f] != null) {
										r.setNormalFieldValue(f, temp.calc(exps[f], ctx));
									} else if (isMyCol[f]) {
										r.setNormalFieldValue(f, values[findex[f]]);
									} else {
										r.setNormalFieldValue(f, baseValues[findex[f]]);
									}
								}
							} else {
								for (int f = 0; f < colCount; ++f) {
									if (exps[f] != null) {
										r.setNormalFieldValue(f, temp.calc(exps[f], ctx));
									} else if (gathers[f] != null) {
										r.setNormalFieldValue(f, gathers[f].getNextBySeq(recNum));
									} else {
										r.setNormalFieldValue(f, values[findex[f]]);
									}
								}
							}
						} else {
							if (!isPrimaryTable) {
								for (int f = 0; f < colCount; ++f) {
									if (isMyCol[f]) {
										r.setNormalFieldValue(f, values[findex[f]]);
									} else {
										r.setNormalFieldValue(f, baseValues[findex[f]]);
									}
								}
							} else {
								for (int f = 0; f < colCount; ++f) {
									r.setNormalFieldValue(f, values[findex[f]]);
								}
							}
						}
						if (isPrimaryTable) {
							r.setRecordSeq(recNum);
						} else {
							r.setRecordSeq(seq);
						}
						mems.add(r);
					}
					
					Table tmp = new Table(ds, ICursor.FETCHCOUNT);
					this.cache = tmp;
					mems = tmp.getMems();
					
					for (; i < recordCount; ++i) {
						//读出来一整行
						long recNum = (Long) bufReader.readObject();//跳过伪号
						if (!isPrimaryTable) {
							seq = (Long) bufReader.readObject();//导列伪号
						}
						for (int f = 0; f < allCount; ++f) {
							if (f >= baseKeyCount) {
								if (needRead[f]) {
									values[f] = bufReader.readObject();
								} else {
									bufReader.skipObject();
								}
							}
						}

						if (!isPrimaryTable) {
							//主表里找对应的
							while (seq != baseSeq) {
								baseSeq = (Long) baseBufReader.readObject();
								//找到了读出来
								for (int k = 0; k < baseAllCount; ++k) {
									baseValues[k] = baseBufReader.readObject();
								}
								filterByModifyRecord(baseSeq, baseModifyRecords, baseValues); 
							}
						}
						
						//用取出字段组成记录
						ComTableRecord r = new ComTableRecord(ds);
						if (exps != null) {
							temp.setStart(0, values);
							if (!isPrimaryTable) {
								for (int f = 0; f < colCount; ++f) {
									if (exps[f] != null) {
										r.setNormalFieldValue(f, temp.calc(exps[f], ctx));
									} else if (isMyCol[f]) {
										r.setNormalFieldValue(f, values[findex[f]]);
									} else {
										r.setNormalFieldValue(f, baseValues[findex[f]]);
									}
								}
							} else {
								for (int f = 0; f < colCount; ++f) {
									if (exps[f] != null) {
										r.setNormalFieldValue(f, temp.calc(exps[f], ctx));
									} else if (gathers[f] != null) {
										r.setNormalFieldValue(f, gathers[f].getNextBySeq(recNum));
									} else {
										r.setNormalFieldValue(f, values[findex[f]]);
									}
								}
							}
						} else {
							if (!isPrimaryTable) {
								for (int f = 0; f < colCount; ++f) {
									if (isMyCol[f]) {
										r.setNormalFieldValue(f, values[findex[f]]);
									} else {
										r.setNormalFieldValue(f, baseValues[findex[f]]);
									}
								}
							} else {
								for (int f = 0; f < colCount; ++f) {
									r.setNormalFieldValue(f, values[findex[f]]);
								}
							}
						}
						if (isPrimaryTable) {
							r.setRecordSeq(recNum);
						} else {
							r.setRecordSeq(seq);
						}
						mems.add(r);
					}
					
					break;
				} else {
					for (int i = 0; i < recordCount; ++i) {
						//读出来一整行
						long recNum = (Long) bufReader.readObject();//跳过伪号
						if (!isPrimaryTable) {
							seq = (Long) bufReader.readObject();//导列伪号
						}
						for (int f = 0; f < allCount; ++f) {
								if (f >= baseKeyCount) {
									if (needRead[f]) {
										values[f] = bufReader.readObject();
									} else {
										bufReader.skipObject();
									}
								}
						}

						if (!isPrimaryTable) {
							//主表里找对应的
							while (seq != baseSeq) {
								baseSeq = (Long) baseBufReader.readObject();
								//找到了读出来
								for (int k = 0; k < baseAllCount; ++k) {
									baseValues[k] = baseBufReader.readObject();
								}
								filterByModifyRecord(baseSeq, baseModifyRecords, baseValues); 
							}
						}
						
						//用取出字段组成记录
						ComTableRecord r = new ComTableRecord(ds);
						if (exps != null) {
							temp.setStart(0, values);
							if (!isPrimaryTable) {
								for (int f = 0; f < colCount; ++f) {
									if (exps[f] != null) {
										r.setNormalFieldValue(f, temp.calc(exps[f], ctx));
									} else if (isMyCol[f]) {
										r.setNormalFieldValue(f, values[findex[f]]);
									} else {
										r.setNormalFieldValue(f, baseValues[findex[f]]);
									}
								}
							} else {
								for (int f = 0; f < colCount; ++f) {
									if (exps[f] != null) {
										r.setNormalFieldValue(f, temp.calc(exps[f], ctx));
									} else if (gathers[f] != null) {
										r.setNormalFieldValue(f, gathers[f].getNextBySeq(recNum));
									} else {
										r.setNormalFieldValue(f, values[findex[f]]);
									}
								}
							}
						} else {
							if (!isPrimaryTable) {
								for (int f = 0; f < colCount; ++f) {
									if (isMyCol[f]) {
										r.setNormalFieldValue(f, values[findex[f]]);
									} else {
										r.setNormalFieldValue(f, baseValues[findex[f]]);
									}
								}
							} else {
								for (int f = 0; f < colCount; ++f) {
									r.setNormalFieldValue(f, values[findex[f]]);
								}
							}
						}
						if (isPrimaryTable) {
							r.setRecordSeq(recNum);
						} else {
							r.setRecordSeq(seq);
						}
						mems.add(r);
					}
					
					if (diff == recordCount || fetchByBlock) {
						break;
					}
				}
			}

		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		}
		
		this.curBlock = curBlock;
		if (cache.length() > 0) {
			return cache;
		} else {
			return null;
		}
	}
	
	/**
	 * 有补区时的取数
	 * @param n
	 * @return
	 */
	private Sequence getModify(int n) {
		Sequence cache = this.cache;
		if (cache != null) {
			int len = cache.length();
			if (len > n) {
				this.cache = cache.split(n + 1);
				return cache;
			} else if (len == n) {
				this.cache = null;
				return cache;
			}
		} else {
			cache = new Table(ds, ICursor.FETCHCOUNT);
		}
		
		int curBlock = this.curBlock;
		int endBlock = this.endBlock;
		ObjectReader segmentReader = this.segmentReader;
		BlockLinkReader rowReader = this.rowReader;
		int keyCount = table.getAllSortedColNamesLength();
		int colCount = fields.length;
		int allCount = table.getAllColNames().length;
		Object []values = new Object[allCount];
		
		RowBufferReader bufReader;
		DataStruct ds = this.ds;
		IArray mems = cache.getMems();
		this.cache = null;
		long prevRecordSeq = this.prevRecordSeq;
		ArrayList<ModifyRecord> modifyRecords = this.modifyRecords;
		int []findex = this.findex;
		int mindex = this.mindex;
		int mcount = modifyRecords.size();
		
		ModifyRecord mr = modifyRecords.get(mindex);
		long mseq = mr.getRecordSeq();

		BlockLinkReader baseRowReader = null;
		ObjectReader baseSegmentReader = null;
		RowBufferReader baseBufReader = null;
		int baseAllCount = 0;
		int baseKeyCount = 0;
		long baseSeq = 0;
		Object []baseValues = null;
		ArrayList<ModifyRecord> baseModifyRecords = null;
		long guideSeq = 0;
		
		boolean isPrimaryTable = this.isPrimaryTable;
		if (!isPrimaryTable) {
			baseRowReader = this.baseRowReader;
			baseSegmentReader = this.baseSegmentReader;
			baseAllCount = baseTable.getColNames().length;
			baseKeyCount = baseTable.getSortedColNames().length;
			baseValues = new Object[baseAllCount];
			baseModifyRecords = this.baseModifyRecords;
		}
		boolean []isMyCol = this.isMyCol;
		
		try {
			while (curBlock < endBlock) {
				curBlock++;
				int recordCount = segmentReader.readInt32();
				segmentReader.readLong40();
				for (int i = 0; i < keyCount - baseKeyCount; ++i) {
					segmentReader.skipObject();
					segmentReader.skipObject();
				}
				bufReader = rowReader.readBlockBuffer();
				
				if (!isPrimaryTable) {
					baseSegmentReader.readInt32();
					baseSegmentReader.readLong40();
					for (int i = 0; i < baseKeyCount; ++i) {
						baseSegmentReader.skipObject();
						baseSegmentReader.skipObject();
					}
					baseBufReader = baseRowReader.readBlockBuffer();
					baseSeq = (Long) baseBufReader.readObject();
					for (int k = 0; k < baseAllCount; ++k) {
						baseValues[k] = baseBufReader.readObject();
					}
				}
				
				for (int i = 0; i < recordCount; ++i) {
					prevRecordSeq++;
					if (prevRecordSeq != mseq) {
						//读出来一整行
						bufReader.skipObject();//跳过伪号
						if (!isPrimaryTable) {
							guideSeq = (Long) bufReader.readObject();//导列伪号
						}
						for (int f = 0; f < allCount; ++f) {
							if (f >= baseKeyCount) {
								values[f] = bufReader.readObject();
							}
						}
						
						if (!isPrimaryTable) {
							//主表里找对应的
							while (guideSeq != baseSeq) {
								baseSeq = (Long) baseBufReader.readObject();
								//找到了读出来
								for (int k = 0; k < baseAllCount; ++k) {
									baseValues[k] = baseBufReader.readObject();
								}
								filterByModifyRecord(baseSeq, baseModifyRecords, baseValues); 
							}
						}
						
						//用取出字段组成记录
						ComTableRecord r = new ComTableRecord(ds);
						if (!isPrimaryTable) {
							for (int f = 0; f < colCount; ++f) {
								if (isMyCol[f]) {
									r.setNormalFieldValue(f, values[findex[f]]);
								} else {
									r.setNormalFieldValue(f, baseValues[findex[f]]);
								}
							}
						} else {
							for (int f = 0; f < colCount; ++f) {
								r.setNormalFieldValue(f, values[findex[f]]);
							}
						}

						if (isPrimaryTable) {
							r.setRecordSeq(prevRecordSeq);
						} else {
							r.setRecordSeq(guideSeq);
						}
						mems.add(r);
					} else {
						bufReader.skipObject();//跳过伪号
						if (!isPrimaryTable) {
							guideSeq = (Long) bufReader.readObject();//导列伪号
						}
						
						// 可能插入多条
						boolean isInsert = true;
						while (true) {
							if (mr.isDelete()) {
								isInsert = false;
							} else {
								if (mr.isUpdate()) {
									isInsert = false;
								}
								
								Record sr = mr.getRecord();
								ComTableRecord r = new ComTableRecord(ds);
								if (isPrimaryTable) {
									for (int f = 0; f < colCount; ++f) {
										r.setNormalFieldValue(f, sr.getNormalFieldValue(findex[f]));
									}
								} else {
									long seq;
									if (isInsert) {
										seq = mr.getParentRecordSeq();
									} else {
										seq = guideSeq;
									}
									if (seq > 0) {
										//主表里找对应的
										while (seq != baseSeq) {
											baseSeq = (Long) baseBufReader.readObject();
											//找到了读出来
											for (int k = 0; k < baseAllCount; ++k) {
												baseValues[k] = baseBufReader.readObject();
											}
											filterByModifyRecord(baseSeq, baseModifyRecords, baseValues); 
										}
										
										for (int f = 0; f < colCount; ++f) {
											if (isMyCol[f]) {
												r.setNormalFieldValue(f, sr.getNormalFieldValue(findex[f]));
											} else {
												r.setNormalFieldValue(f, baseValues[findex[f]]);
											}
										}
										
									} else {
										//主表补区里找对应的
										seq = -seq - 1;
										Object []vals = baseModifyRecords.get((int) seq).getRecord().getFieldValues();
										for (int f = 0; f < colCount; ++f) {
											if (isMyCol[f]) {
												r.setNormalFieldValue(f, sr.getNormalFieldValue(findex[f]));
											} else {
												r.setNormalFieldValue(f, vals[findex[f]]);
											}
										}
									}
								}

								if (isInsert) {
									if (isPrimaryTable) {
										r.setRecordSeq(-mindex);//主表就是返回补区的序号
									} else {
										//根据key值找
										r.setRecordSeq(mr.getParentRecordSeq());//这里也可能是个负值，表示在主表的补区
									}
								} else {
									if (isPrimaryTable) {
										r.setRecordSeq(prevRecordSeq);
									} else {
										r.setRecordSeq(guideSeq);
									}
								}
								mems.add(r);
							}
							
							mindex++;
							if (mindex < mcount) {
								mr = modifyRecords.get(mindex);
								mseq = mr.getRecordSeq();
								if (prevRecordSeq != mseq) {
									break;
								}
							} else {
								mseq = -1;
								break;
							}
						}
						
						if (isInsert) {
							//读出来一整行
							for (int f = 0; f < allCount; ++f) {
								if (f >= baseKeyCount) {
									values[f] = bufReader.readObject();
								}
							}
							
							if (!isPrimaryTable) {
								//主表里找对应的
								while (guideSeq != baseSeq) {
									baseSeq = (Long) baseBufReader.readObject();
									//找到了读出来
									for (int k = 0; k < baseAllCount; ++k) {
										baseValues[k] = baseBufReader.readObject();
									}
								}
							}
							
							//用取出字段组成记录
							ComTableRecord r = new ComTableRecord(ds);
							if (!isPrimaryTable) {
								for (int f = 0; f < colCount; ++f) {
									if (isMyCol[f]) {
										r.setNormalFieldValue(f, values[findex[f]]);
									} else {
										r.setNormalFieldValue(f, baseValues[findex[f]]);
									}
								}
							} else {
								for (int f = 0; f < colCount; ++f) {
									r.setNormalFieldValue(f, values[findex[f]]);
								}
							}
							
							if (isPrimaryTable) {
								r.setRecordSeq(prevRecordSeq);
							} else {
								r.setRecordSeq(guideSeq);
							}
							mems.add(r);
						} else {
							for (int f = 0; f < allCount; ++f) {
								if (f >= baseKeyCount) {
									bufReader.skipObject();
								}
							}
						}
					}
				}
				
				if (fetchByBlock) {
					break;
				}
				int diff = n - cache.length();
				if (diff < 0) {
					this.cache = cache.split(n + 1);
					break;
				} else if (diff == 0) {
					break;
				}
			}
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		}
		
		this.curBlock = curBlock;
		this.prevRecordSeq = prevRecordSeq;
		this.mindex = mindex;
		
		if (cache.length() > 0) {
			return cache;
		} else {
			return null;
		}
	}

	/**
	 * 有字段表达式时且有补区时的取数
	 * @param n
	 * @return
	 */
	private Sequence getModify2(int n) {
		Sequence cache = this.cache;
		if (cache != null) {
			int len = cache.length();
			if (len > n) {
				this.cache = cache.split(n + 1);
				return cache;
			} else if (len == n) {
				this.cache = null;
				return cache;
			}
		} else {
			cache = new Table(ds, ICursor.FETCHCOUNT);
		}
		
		int curBlock = this.curBlock;
		int endBlock = this.endBlock;
		ObjectReader segmentReader = this.segmentReader;
		BlockLinkReader rowReader = this.rowReader;
		int keyCount = table.getAllSortedColNamesLength();
		int colCount = fields.length;
		int allCount = table.getAllColNames().length;
		Object []values = new Object[allCount];
		
		RowBufferReader bufReader;
		DataStruct ds = this.ds;
		IArray mems = cache.getMems();
		this.cache = null;
		long prevRecordSeq = this.prevRecordSeq;
		ArrayList<ModifyRecord> modifyRecords = this.modifyRecords;
		int []findex = this.findex;
		int mindex = this.mindex;
		int mcount = modifyRecords.size();
		
		ModifyRecord mr = modifyRecords.get(mindex);
		long mseq = mr.getRecordSeq();

		BlockLinkReader baseRowReader = null;
		ObjectReader baseSegmentReader = null;
		RowBufferReader baseBufReader = null;
		int baseAllCount = 0;
		int baseKeyCount = 0;
		long baseSeq = 0;
		Object []baseValues = null;
		ArrayList<ModifyRecord> baseModifyRecords = null;
		long guideSeq = 0;
		
		boolean isPrimaryTable = this.isPrimaryTable;
		if (!isPrimaryTable) {
			baseRowReader = this.baseRowReader;
			baseSegmentReader = this.baseSegmentReader;
			baseAllCount = baseTable.getColNames().length;
			baseKeyCount = baseTable.getSortedColNames().length;
			baseValues = new Object[baseAllCount];
			baseModifyRecords = this.baseModifyRecords;
		}
		boolean []isMyCol = this.isMyCol;
		
		TableGather []gathers = this.gathers;
		Record temp = new Record(this.tempDs);
		Context ctx = this.ctx;
		Expression []exps = this.exps;
		
		try {
			while (curBlock < endBlock) {
				curBlock++;
				int recordCount = segmentReader.readInt32();
				segmentReader.readLong40();
				for (int i = 0; i < keyCount - baseKeyCount; ++i) {
					segmentReader.skipObject();
					segmentReader.skipObject();
				}
				bufReader = rowReader.readBlockBuffer();
				
				for (TableGather gather : gathers) {
					if (gather != null) {
						gather.loadData();
					}
				}
				
				if (!isPrimaryTable) {
					baseSegmentReader.readInt32();
					baseSegmentReader.readLong40();
					for (int i = 0; i < baseKeyCount; ++i) {
						baseSegmentReader.skipObject();
						baseSegmentReader.skipObject();
					}
					baseBufReader = baseRowReader.readBlockBuffer();
					baseSeq = (Long) baseBufReader.readObject();
					for (int k = 0; k < baseAllCount; ++k) {
						baseValues[k] = baseBufReader.readObject();
					}
				}
				
				for (int i = 0; i < recordCount; ++i) {
					prevRecordSeq++;
					if (prevRecordSeq != mseq) {
						//读出来一整行
						bufReader.skipObject();//跳过伪号
						if (!isPrimaryTable) {
							guideSeq = (Long) bufReader.readObject();//导列伪号
						}
						for (int f = 0; f < allCount; ++f) {
							if (f >= baseKeyCount) {
								values[f] = bufReader.readObject();
							}
						}
						
						if (!isPrimaryTable) {
							//主表里找对应的
							while (guideSeq != baseSeq) {
								baseSeq = (Long) baseBufReader.readObject();
								//找到了读出来
								for (int k = 0; k < baseAllCount; ++k) {
									baseValues[k] = baseBufReader.readObject();
								}
								filterByModifyRecord(baseSeq, baseModifyRecords, baseValues); 
							}
						}
						
						//用取出字段组成记录
						ComTableRecord r = new ComTableRecord(ds);
						temp.setStart(0, values);
						if (!isPrimaryTable) {
							for (int f = 0; f < colCount; ++f) {
								if (exps[f] != null) {
									r.setNormalFieldValue(f, temp.calc(exps[f], ctx));
								} else if (isMyCol[f]) {
									r.setNormalFieldValue(f, values[findex[f]]);
								} else {
									r.setNormalFieldValue(f, baseValues[findex[f]]);
								}
							}
						} else {
							for (int f = 0; f < colCount; ++f) {
								if (exps[f] != null) {
									r.setNormalFieldValue(f, temp.calc(exps[f], ctx));
								} else if (gathers[f] != null) {
									r.setNormalFieldValue(f, gathers[f].getNextBySeq(prevRecordSeq));
								} else {
									r.setNormalFieldValue(f, values[findex[f]]);
								}
							}
						}

						if (isPrimaryTable) {
							r.setRecordSeq(prevRecordSeq);
						} else {
							r.setRecordSeq(guideSeq);
						}
						mems.add(r);
					} else {
						// 可能插入多条
						boolean isInsert = true;
						while (true) {
							if (mr.isDelete()) {
								isInsert = false;
							} else {
								if (mr.isUpdate()) {
									isInsert = false;
								}
								
								Record sr = mr.getRecord();
								ComTableRecord r = new ComTableRecord(ds);
								if (isPrimaryTable) {
									for (int f = 0; f < colCount; ++f) {
										if (exps[f] != null) {
											r.setNormalFieldValue(f, sr.calc(exps[f], ctx));
										} else if (gathers[f] != null) {
											if (isInsert) {
												r.setNormalFieldValue(f, gathers[f].getNextBySeq(-(mindex + 1)));
											} else {
												r.setNormalFieldValue(f, gathers[f].getNextBySeq(prevRecordSeq));
											}
										} else {
											r.setNormalFieldValue(f, sr.getNormalFieldValue(findex[f]));
										}
									}
								} else {
									long seq = mr.getParentRecordSeq();
									if (seq > 0) {
										//主表里找对应的
										while (seq != baseSeq) {
											baseSeq = (Long) baseBufReader.readObject();
											//找到了读出来
											for (int k = 0; k < baseAllCount; ++k) {
												baseValues[k] = baseBufReader.readObject();
											}
											filterByModifyRecord(baseSeq, baseModifyRecords, baseValues); 
										}
										
										for (int f = 0; f < colCount; ++f) {
											if (exps[f] != null) {
												r.setNormalFieldValue(f, sr.calc(exps[f], ctx));
											} else if (isMyCol[f]) {
												r.setNormalFieldValue(f, sr.getNormalFieldValue(findex[f]));
											} else {
												r.setNormalFieldValue(f, baseValues[findex[f]]);
											}
										}
										
									} else {
										//主表补区里找对应的
										seq = -seq - 1;
										Object []vals = baseModifyRecords.get((int) seq).getRecord().getFieldValues();
										for (int f = 0; f < colCount; ++f) {
											if (exps[f] != null) {
												r.setNormalFieldValue(f, sr.calc(exps[f], ctx));
											} else if (isMyCol[f]) {
												r.setNormalFieldValue(f, sr.getNormalFieldValue(findex[f]));
											} else {
												r.setNormalFieldValue(f, vals[findex[f]]);
											}
										}
									}
								}

								if (isInsert) {
									if (isPrimaryTable) {
										r.setRecordSeq(-mindex);//主表就是返回补区的序号
									} else {
										//根据key值找
										r.setRecordSeq(mr.getParentRecordSeq());//这里也可能是个负值，表示在主表的补区
									}
								} else {
									if (isPrimaryTable) {
										r.setRecordSeq(prevRecordSeq);
									} else {
										r.setRecordSeq(guideSeq);
									}
								}
								mems.add(r);
							}
							
							mindex++;
							if (mindex < mcount) {
								mr = modifyRecords.get(mindex);
								mseq = mr.getRecordSeq();
								if (prevRecordSeq != mseq) {
									break;
								}
							} else {
								mseq = -1;
								break;
							}
						}
						
						if (isInsert) {
							//读出来一整行
							long recNum = bufReader.readLong();//跳过伪号
							if (!isPrimaryTable) {
								guideSeq = (Long) bufReader.readObject();//导列伪号
							}
							for (int f = 0; f < allCount; ++f) {
								if (f >= baseKeyCount) {
									values[f] = bufReader.readObject();
								}
							}
							
							if (!isPrimaryTable) {
								//主表里找对应的
								while (guideSeq != baseSeq) {
									baseSeq = (Long) baseBufReader.readObject();
									//找到了读出来
									for (int k = 0; k < baseAllCount; ++k) {
										baseValues[k] = baseBufReader.readObject();
									}
								}
							}
							
							//用取出字段组成记录
							ComTableRecord r = new ComTableRecord(ds);
							temp.setStart(0, values);
							if (!isPrimaryTable) {
								for (int f = 0; f < colCount; ++f) {
									if (exps[f] != null) {
										r.setNormalFieldValue(f, temp.calc(exps[f], ctx));
									} else if (isMyCol[f]) {
										r.setNormalFieldValue(f, values[findex[f]]);
									} else {
										r.setNormalFieldValue(f, baseValues[findex[f]]);
									}
								}
							} else {
								for (int f = 0; f < colCount; ++f) {
									if (exps[f] != null) {
										r.setNormalFieldValue(f, temp.calc(exps[f], ctx));
									} else if (gathers[f] != null) {
										r.setNormalFieldValue(f, gathers[f].getNextBySeq(recNum));
									} else {
										r.setNormalFieldValue(f, values[findex[f]]);
									}
								}
							}
							
							if (isPrimaryTable) {
								r.setRecordSeq(prevRecordSeq);
							} else {
								r.setRecordSeq(guideSeq);
							}
							mems.add(r);
						} else {
							bufReader.skipObject();//跳过伪号
							if (!isPrimaryTable) {
								bufReader.skipObject();//跳过导列伪号
							}
							for (int f = 0; f < allCount; ++f) {
								if (f >= baseKeyCount) {
									bufReader.skipObject();
								}
							}
						}
					}
				}
				
				if (fetchByBlock) {
					break;
				}
				int diff = n - cache.length();
				if (diff < 0) {
					this.cache = cache.split(n + 1);
					break;
				} else if (diff == 0) {
					break;
				}
			}
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		}
		
		this.curBlock = curBlock;
		this.prevRecordSeq = prevRecordSeq;
		this.mindex = mindex;
		
		if (cache.length() > 0) {
			return cache;
		} else {
			return null;
		}
	}
	
	protected long skipOver(long n) {
		if (isClosed) {
			return 0;
		} else if (isFirstSkip && n == MAXSKIPSIZE && filter == null && !isSegment) {
			return table.getActualRecordCount();
		}
		
		boolean isFirstSkip = this.isFirstSkip;
		this.isFirstSkip = false;
		long count = 0;
		
		//对没有过滤的情况优化
		if (filters == null 
				&& !hasModify() 
				&& isFirstSkip  
				&& !isSegment 
				&& gathers == null 
				&& exps == null
				&& isPrimaryTable) {
			
			//处理cache
			Sequence cache = this.cache;
			if (cache != null) {
				int len = cache.length();
				if (n <= len) {
					get((int) n);
					return n;
				} else {
					get(len);
					count += len;
				}
			}
			
			//跳段
			int curBlock = this.curBlock;
			int endBlock = this.endBlock;
			BlockLinkReader rowReader = this.rowReader;
			ObjectReader segmentReader = this.segmentReader;
			int colCount = this.fields.length;
			int allCount = table.getAllColNames().length;
			int keyCount = table.getAllSortedColNamesLength();
			RowBufferReader bufReader;
			Object []values = new Object[allCount];
			
			try {
				while (curBlock < endBlock) {
					curBlock++;
					int recordCount = segmentReader.readInt32();
					long pos = segmentReader.readLong40();
					
					for (int i = 0; i < keyCount; i++) {
						segmentReader.skipObject();
						segmentReader.skipObject();
					}
					
					if (count + recordCount == n) {
						rowReader.seek(pos);
						rowReader.readBlockBuffer();//这里要读取一下才能确保下次读取时的位置
						this.curBlock = curBlock;
						return n;
					} else if (count + recordCount > n) {
						rowReader.seek(pos);
						bufReader = rowReader.readBlockBuffer();
						
						long diff = n - count;
						int i = 0;
						for (; i < diff; ++i) {
							bufReader.skipObject();//跳过伪号
							for (int f = 0; f < allCount; ++f) {
								bufReader.skipObject();
							}
						}
						
						Table tmp = new Table(ds, ICursor.FETCHCOUNT);
						this.cache = tmp;
						IArray mems = tmp.getMems();
						
						for (; i < recordCount; ++i) {
							ComTableRecord r = new ComTableRecord(ds);
							bufReader.readObject();//跳过伪号
							for (int f = 0; f < allCount; ++f) {
								if (needRead[f]) {
									values[f] = bufReader.readObject();
								} else {
									bufReader.skipObject();
								}
							
							}
							
							for (int f = 0; f < colCount; ++f) {
								r.setNormalFieldValue(f, values[findex[f]]);
							}
							mems.add(r);
						}
						this.curBlock = curBlock;
						return n;
					}
					count += recordCount;
				}
				this.curBlock = curBlock;
				return count;
			} catch (IOException e) {
				throw new RQException(e.getMessage(), e);
			}
			
		} else {
			Sequence data;
			long rest = n;
			while (rest != 0) {
				if (rest > FETCHCOUNT) {
					data = get(FETCHCOUNT);
				} else {
					data = get((int)rest);
				}
				
				if (data == null) {
					break;
				} else {
					count += data.length();
				}
				
				rest -= data.length();
			}
			return count;
		}
	}
	
	public void close() {
		super.close();
		if (isClosed) return;
		isClosed = true;
		cache = null;
		
		try {
			if (segmentReader != null) {
				segmentReader.close();
			}
			rowReader.close();
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			rowReader = null;
			segmentReader = null;
		}
	}
	
	public boolean reset() {
		close();
		
		isClosed = false;
		curBlock = 0;
		int endBlock = this.endBlock;
		prevRecordSeq = 0;
		mindex = 0;
		isFirstSkip = true;
		
		init();
		if (isSegment) {
			setSegment(startBlock, endBlock);
		}
		return true;
	}
	
	/**
	 * 对补区的记录过滤
	 * @param seq
	 * @param modifyRecords
	 * @param vals
	 */
	private static void filterByModifyRecord(long seq, ArrayList<ModifyRecord> modifyRecords, Object []vals) {
		if (modifyRecords == null) return;
		for (ModifyRecord mr : modifyRecords) {
			if (seq == mr.getRecordSeq()) {
				if (mr.getState() == ModifyRecord.STATE_UPDATE) {
					Record r = mr.getRecord();
					int size = vals.length;
					for (int i = 0; i < size; i++) {
						vals[i] = r.getNormalFieldValue(i);
					}
					return;
				}
			}
		}
	}

	public void setAppendData(Sequence seq) {
	}

	public PhyTable getTableMetaData() {
		return table;
	}
	
	/**
	 * 设置为按块取出（一次必定取完一块里的所有记录）
	 * @param fetchByBlock
	 */
	public void setFetchByBlock(boolean fetchByBlock) {
		this.fetchByBlock = fetchByBlock;
	}

	public String[] getSortFields() {
		return sortedFields;
	}

	public void setCache(Sequence cache) {
		if (this.cache != null) {
			cache.addAll(this.cache);
			this.cache = cache;
		} else {
			this.cache = cache;	
		}
	}
	
	protected Sequence getStartBlockData(int n) {
		// 只取第一块的记录，如果第一块没有满足条件的就返回
		int startBlock = this.startBlock;
		int endBlock = this.endBlock;
		try {
			setEndBlock(startBlock + 1);
			return get(n);
		} finally {
			setEndBlock(endBlock);
		}
	}
	
	private boolean hasModify() {
		return modifyRecords != null && mindex < modifyRecords.size();
	}
}