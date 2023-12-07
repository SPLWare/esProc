package com.scudata.dw;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.scudata.array.IArray;
import com.scudata.array.ObjectArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.ObjectReader;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.op.New;
import com.scudata.dm.op.Operation;
import com.scudata.dm.op.Select;
import com.scudata.dm.op.Switch;
import com.scudata.expression.Constant;
import com.scudata.expression.CurrentElement;
import com.scudata.expression.CurrentSeq;
import com.scudata.expression.ElementRef;
import com.scudata.expression.Expression;
import com.scudata.expression.FieldRef;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.Moves;
import com.scudata.expression.Node;
import com.scudata.expression.UnknownSymbol;
import com.scudata.expression.fn.Between;
import com.scudata.expression.fn.gather.Top;
import com.scudata.expression.mfn.sequence.Contain;
import com.scudata.expression.mfn.sequence.Find;
import com.scudata.expression.mfn.sequence.PFind;
import com.scudata.expression.mfn.serial.Sbs;
import com.scudata.expression.operator.And;
import com.scudata.expression.operator.Assign;
import com.scudata.expression.operator.Comma;
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
import com.scudata.util.Variant;

/**
 * 列存基表的游标类
 * @author runqian
 *
 */
public class Cursor extends IDWCursor {
	private ColPhyTable table;//原表
	private String []fields;//取出字段
	protected DataStruct ds;//数据结构
	private String []sortedFields;//有序字段
	private Expression filter;//过滤表达式
	
	// K:T	在K:T条件上基础上再令K=T.find(K)，K必须是选出字段
	private String []fkNames;
	private Sequence []codes;
	private String []opts;
	
	private IFilter []filters;//过滤器
	protected FindFilter []findFilters; // 用来做switch的字段
	private int []seqs; // colReaders对应的字段号，过滤字段可能不选出
	
	private ColumnMetaData []columns;//用到的列
	private BlockLinkReader rowCountReader;
	private BlockLinkReader []colReaders;
	private ObjectReader []segmentReaders;
	
	private int startBlock; // 包含
	private int endBlock = -1; // 不包含
	private int curBlock = 0;
	protected Sequence cache;
	
	private long prevRecordSeq = 0; // 前一条记录的序号
	private int []findex; // 选出字段对应的字段号
	private ArrayList<ModifyRecord> modifyRecords;
	private int mindex = 0;
	private int mcount = 0;
	
	// 同步分段时需要补上下一段第一块里属于本段的部分
	private Sequence appendData;
	private int appendIndex = 0;
	
	private boolean isClosed = false;
	private boolean isFirstSkip = true;
	private boolean isSegment = false;
	private Expression []exps;//表达式字段
	private Expression []expsBakup;
	private String []names;//取出名
	private TableGather []gathers;//从其它附表取
	private boolean isField[];//true,是字段；false，是表达式
	private DataStruct tempDs;
	
	public Cursor() {
	}
	
	public Cursor(ColPhyTable table) {
		this(table, null);
	}
	
	public Cursor(ColPhyTable table, String []fields) {
		this(table, fields, null, null);
	}
	
	public Cursor(ColPhyTable table, String []fields, Expression filter, Context ctx) {
		this.table = table;
		this.fields = fields;
		this.filter = filter;
		this.ctx = ctx;
		
		init();
	}
	
	public Cursor(ColPhyTable table, String []fields, Expression filter, String []fkNames, 
			Sequence []codes, String []opts, Context ctx) {
		this.table = table;
		this.fields = fields;
		this.filter = filter;
		this.fkNames = fkNames;
		this.codes = codes;
		this.opts = opts;
		this.ctx = ctx;
		
		init();
	}
	
	public Cursor(ColPhyTable table, Expression []exps, String []names, Expression filter, Context ctx) {
		this.table = table;
		this.filter = filter;
		this.names = names;
		this.ctx = ctx;
		
		if (exps == null && names != null) {
			this.fields = names;
			this.names = null;
		}
		
		initExps(exps);
		init();
	}
	
	public Cursor(ColPhyTable table, Expression []exps, String []names, Expression filter, 
			String []fkNames, Sequence []codes, String []opts, Context ctx) {
		this.table = table;
		this.filter = filter;
		this.fkNames = fkNames;
		this.codes = codes;
		this.opts = opts;
		this.names = names;
		this.ctx = ctx;
		
		if (exps == null && names != null) {
			this.fields = names;
			this.names = null;
		}
		
		initExps(exps);		
		init();
	}
	
	public PhyTable getTableMetaData() {
		return table;
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
		
		ColumnMetaData []columns = this.columns;
		BlockLinkReader rowCountReader = this.rowCountReader;
		int colCount = columns.length;
		long prevRecordSeq = 0;
		
		try {
			if (filters == null) {
				BlockLinkReader []colReaders = this.colReaders;
				ObjectReader []segmentReaders = new ObjectReader[colCount];
				for (int i = 0; i < colCount; ++i) {
					if (columns[i] != null) {
						segmentReaders[i] = columns[i].getSegmentReader();
					}
				}
				
				for (int i = 0; i < startBlock; ++i) {
					prevRecordSeq += rowCountReader.readInt32();
					for (int f = 0; f < colCount; ++f) {
						if (segmentReaders[f] != null) {
							segmentReaders[f].readLong40();
							if (columns[f].hasMaxMinValues()) {
								segmentReaders[f].skipObject();
								segmentReaders[f].skipObject();
								segmentReaders[f].skipObject();
							}
						}
					}
				}
				
				for (int f = 0; f < colCount; ++f) {
					if (segmentReaders[f] != null) {
						long pos = segmentReaders[f].readLong40();
						colReaders[f].seek(pos);
					}
				}
			} else {
				ObjectReader []segmentReaders = this.segmentReaders;
				for (int i = 0; i < startBlock; ++i) {
					prevRecordSeq += rowCountReader.readInt32();
					for (int f = 0; f < colCount; ++f) {
						if (segmentReaders[f] != null) {
							segmentReaders[f].readLong40();
							if (columns[f].hasMaxMinValues()) {
								segmentReaders[f].skipObject();
								segmentReaders[f].skipObject();
								segmentReaders[f].skipObject();
							}
						}
					}
				}
			}
			
			this.prevRecordSeq = prevRecordSeq;
			if (prevRecordSeq > 0 && mcount > 0) {
				// 补区也要相应地做分段
				ArrayList<ModifyRecord> modifyRecords = this.modifyRecords;
				int mindex = 0;
				for (ModifyRecord r : modifyRecords) {
					if (r.getRecordSeq() <= prevRecordSeq) {
						mindex++;
					} else {
						break;
					}
				}
				
				this.mindex = mindex;
			}
			
			if (gathers != null) {
				for (TableGather gather : gathers) {
					if (gather != null) {
						gather.setSegment(startBlock, endBlock);
					}
				}
			}
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		}
	}
	
	/**
	 *  同步分段时需要补上下一段第一块里属于本段的部分
	 */
	public void setAppendData(Sequence seq) {
		this.appendData = (Sequence) seq;
		if (seq != null && seq.length() > 0) {
			appendIndex = 1;
		}
	}
	
	public Sequence getAppendData() {
		return appendData;
	}
	
	/**
	 * 从表达式里提取涉及到的列
	 * @param table
	 * @param node
	 * @return
	 */
	private static ColumnMetaData getColumn(ColPhyTable table, Node node) {
		if (node instanceof UnknownSymbol) {
			/**
			 * 直接就是字段
			 */
			String keyName = ((UnknownSymbol)node).getName();
			return table.getColumn(keyName);
		} else if (node instanceof DotOperator && node.getLeft() instanceof CurrentElement && 
				node.getRight() instanceof FieldRef) {
			/**
			 *  ~.key格式的
			 */
			FieldRef fieldNode = (FieldRef)node.getRight();
			String keyName = fieldNode.getName();
			return table.getColumn(keyName);
		} else if (node instanceof DotOperator && node.getRight() instanceof Sbs) {
			/**
			 * 排号k.sbs()
			 */
			Node left = node.getLeft();
			return getColumn(table, left);
		} else if (node instanceof com.scudata.expression.fn.math.And) {
			ColumnMetaData col = getColumn(table, ((Function) node).getParam().getSub(0).getLeafExpression().getHome());
			if (col == null) {
				col = getColumn(table, ((Function) node).getParam().getSub(1).getLeafExpression().getHome());
			}
			return col;
		} else {
			return null;
		}
	}
	
	private static Object combineAnd(Node node, Object left, Object right) {
		/*if (left instanceof ColumnsOr && right instanceof ColumnsOr) {
			return node;
		}
		if (left instanceof ColumnsOr) {
			left = ((ColumnsOr) left).getNode();
		}
		if (right instanceof ColumnsOr) {
			right = ((ColumnsOr) right).getNode();
		}*/
		
		if (left instanceof IFilter) {
			if (right instanceof IFilter) {
				IFilter f1 = (IFilter)left;
				IFilter f2 = (IFilter)right;
				if (f1.isSameColumn(f2)) {
					return new LogicAnd(f1,f2);
				} else {
					ArrayList<Object> filterList = new ArrayList<Object>();
					filterList.add(f1);
					filterList.add(f2);
					return filterList;
				}
			} else if (right instanceof ArrayList) {
				IFilter filter = (IFilter)left;
				ArrayList<Object> filterList = (ArrayList<Object>)right;
				for (int i = 0, size = filterList.size(); i < size; ++i) {
					Object obj = filterList.get(i);
					if (obj instanceof IFilter && filter.isSameColumn((IFilter)obj)) {
						LogicAnd and = new LogicAnd(filter, (IFilter)obj);
						filterList.set(i, and);
						return filterList;
					}
				}
				
				filterList.add(filter);
				return filterList;
			} else {
				ArrayList<Object> filterList = new ArrayList<Object>();
				filterList.add(left);
				filterList.add(right);
				return filterList;
			}
		} else if (left instanceof ArrayList) {
			ArrayList<Object> filterList = (ArrayList<Object>)left;
			if (right instanceof IFilter) {
				IFilter filter = (IFilter)right;
				for (int i = 0, size = filterList.size(); i < size; ++i) {
					Object obj = filterList.get(i);
					if (obj instanceof IFilter && filter.isSameColumn((IFilter)obj)) {
						LogicAnd and = new LogicAnd(filter, (IFilter)obj);
						filterList.set(i, and);
						return filterList;
					}
				}
				
				filterList.add(filter);
				return filterList;
			} else if (right instanceof ArrayList) {
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
								LogicAnd and = new LogicAnd((IFilter)obj, filter);
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
			} else {
				filterList.add(right);
				return filterList;
			}
		} else {
			if (right instanceof IFilter) {
				ArrayList<Object> filterList = new ArrayList<Object>();
				filterList.add(left);
				filterList.add(right);
				return filterList;
			} else if (right instanceof ArrayList) {
				ArrayList<Object> filterList = (ArrayList<Object>)right;
				filterList.add(left);
				return filterList;
			} else {
				return node;
			}
		}
	}
	
	private static Object combineOr(Node node, Object left, Object right) {
		if (left instanceof IFilter) {
			if (right instanceof IFilter) {
				IFilter f1 = (IFilter)left;
				IFilter f2 = (IFilter)right;
				if (f1.isSameColumn(f2)) {
					return new LogicOr(f1,f2);
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

	private void parseSwitch(ColPhyTable table, Context ctx) {
		if (hasModify()) {
			int fkCount = fkNames.length;
			for (int i = 0; i < fkCount; i ++) {
				String[] fkn = new String[] {fkNames[i]};
				Sequence[] code = new Sequence[] {codes[i]};
				String opt = null;
				Expression exps[] = null;
				if (opts[i] != null && opts[i].indexOf("#") != -1) {
					exps = new Expression[] {new Expression("#")};
				} else if (opts[i] != null && opts[i].indexOf("null") != -1) {
					opt = "d";
				} else {
					opt = "i";
				}
				Switch op = new Switch(fkn, code, exps, opt);
				addOperation(op, ctx);
			}
		} else {
			int fcount = fkNames.length;
			ArrayList<IFilter> filterList = new ArrayList<IFilter>();
			ArrayList<FindFilter> findFilterList = new ArrayList<FindFilter>();
			if (filters != null) {
				for (IFilter filter : filters) {
					filterList.add(filter);
					findFilterList.add(null);
				}
			}
			
			int fltCount = filterList.size();
			
			Next:
			for (int f = 0; f < fcount; ++f) {
				ColumnMetaData column = table.getColumn(fkNames[f]);
				if (column == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(fkNames[f] + mm.getMessage("ds.fieldNotExist"));
				}
				
				int pri = table.getColumnFilterPriority(column);
				FindFilter find;
				if (opts[f] != null && opts[f].indexOf("#") != -1) {
					find = new MemberFilter(column, pri, codes[f], null);
				} else if (opts[f] != null && opts[f].indexOf("null") != -1) {
					find = new NotFindFilter(column, pri, codes[f], null);
				} else {
					find = new FindFilter(column, pri, codes[f], null);
				}
				for (int i = 0; i < fltCount; ++i) {
					IFilter filter = filterList.get(i);
					if (filter.isSameColumn(find)) {
						LogicAnd and = new LogicAnd(filter, find);
						filterList.set(i, and);
						findFilterList.set(i, find);
						continue Next;
					}
				}
				
				filterList.add(find);
				findFilterList.add(find);
			}
			
			int total = filterList.size();
			filters = new IFilter[total];
			findFilters = new FindFilter[total];
			filterList.toArray(filters);
			findFilterList.toArray(findFilters);
		}
	}
	
	private void parseFilter() {
		Object obj = parseFilter(table, filter, ctx);
		Expression unknownFilter = null;
		
		if (obj instanceof IFilter) {
			filters = new IFilter[] {(IFilter)obj};
		} else if (obj instanceof ArrayList) {
			ArrayList<Object> list = (ArrayList<Object>)obj;
			ArrayList<IFilter> filterList = new ArrayList<IFilter>();
			Node node = null;
			boolean hasOr = false;
			
			for (Object f : list) {
				if (f instanceof IFilter) {
					filterList.add((IFilter)f);
				} else if (f instanceof ColumnsOr) {
					hasOr = true;
					IFilter []orFilters = ((ColumnsOr)f).toArray();
					for (IFilter filter : orFilters) {
						filterList.add(filter);
					}
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
				
				if (!hasOr) {
					Arrays.sort(filters);
				}
				
				if (node != null) {
					unknownFilter = new Expression(node);
				}
			} else {
				unknownFilter = filter;
			}
		} else if (obj instanceof ColumnsOr) {
			ArrayList<ModifyRecord> modifyRecords = table.getModifyRecords();
			if (modifyRecords != null || exps != null) {
				unknownFilter = filter;
			} else {
				//目前只优化没有补区和没有表达式的情况
				filters = ((ColumnsOr)obj).toArray();
			}
		} else if (obj instanceof Top) {
			//do nothing
		} else {
			unknownFilter = filter;
		}
		
		if (unknownFilter != null) {
			Select select = new Select(unknownFilter, "o");
			addOperation(select, ctx);
			
			// 检查不可识别的表达式里是否引用了没有选出的字段，如果引用了则加入到选出字段里
			ArrayList<String> nameList = new ArrayList<String>();
			unknownFilter.getUsedFields(ctx, nameList);
			if (nameList.size() > 0 && fields != null) {
				ArrayList<String> selectList = new ArrayList<String>();
				for (String name : fields) {
					selectList.add(name);
				}
				
				for (String name : nameList) {
					if (!selectList.contains(name) && table.getColumn(name) != null) {
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
	}
	
	private static Object parseTop(ColPhyTable table, Top top, Context ctx) {
		top.prepare(ctx);
		String col = top.getExp().getIdentifierName();
		ColumnMetaData column = table.getColumn(col);
		if (column == null) {
			return top;
		}
		
		int pri = table.getColumnFilterPriority(column);
		return new TopFilter(column, pri, top);
	}
	
	private static Object parseBetween(ColPhyTable table, Between bt, Context ctx) {
		IParam sub0 = bt.getParam().getSub(0);
		IParam sub1 = bt.getParam().getSub(1);
		if (sub0 == null || sub1 == null) {
			return bt;
		}
		
		Expression field = sub0.getLeafExpression();
		ColumnMetaData column = table.getColumn(field.getIdentifierName());
		if (column == null) {
			return bt;
		}
		
		int pri = table.getColumnFilterPriority(column);
		IParam startParam = sub1.getSub(0);
		IParam endParam = sub1.getSub(1);
		IFilter s = null, e = null;
		if (startParam != null) {
			Object value = startParam.getLeafExpression().calculate(ctx);
			s = new ColumnFilter(column, pri, IFilter.GREATER_EQUAL, value);
		}
		if (endParam != null) {
			Object value = endParam.getLeafExpression().calculate(ctx);
			e = new ColumnFilter(column, pri, IFilter.LESS_EQUAL, value);
		}
		
		if (s == null && e != null)
			return e;
		if (e == null && s != null)
			return s;
		if (s != null && e != null) 
			return combineAnd(bt, s, e);

		return bt;
	}
	
	private static Object parseSeriesMember(ColPhyTable table, ElementRef sm, Context ctx) {
		ColumnMetaData column = table.getColumn(sm.getParamString());
		if (column == null) {
			return sm;
		}
		
		try {
			Object val = sm.getLeft().calculate(ctx);
			if (val instanceof Sequence) {
				int pri = table.getColumnFilterPriority(column);
				return new MemberFilter(column, pri, (Sequence)val, null);
			} else {
				return sm;
			}
		} catch (Exception e) {
			return sm;
		}
	}
	
	private static Object parseAssign(ColPhyTable table, Node node, Context ctx, boolean isFilter) {
		Node unknown = node.getLeft();
		Node dotoperator = node.getRight();
		if (unknown instanceof UnknownSymbol && dotoperator instanceof DotOperator) {
			Node left = dotoperator.getLeft();
			Node right = dotoperator.getRight();
			if (right instanceof Find) {
				Find find = (Find)right;
				IParam param = find.getParam();
				if (param == null || !param.isLeaf()) {
					return node;
				}
				
				ColumnMetaData column = getColumn(table, param.getLeafExpression().getHome());
				if (column == null) {
					return node;
				}
				
				try {
					Object val = left.calculate(ctx);
					if (val instanceof Sequence) {
						int pri = table.getColumnFilterPriority(column);
						return new FindsFilter(column, pri, (Sequence)val, dotoperator, false, isFilter);
					} else {
						return node;
					}
				} catch (Exception e) {
					return node;
				}
			} else if (right instanceof PFind) {
				PFind find = (PFind)right;
				IParam param = find.getParam();
				if (param == null || !param.isLeaf()) {
					return node;
				}
				
				ColumnMetaData column = getColumn(table, param.getLeafExpression().getHome());
				if (column == null) {
					return node;
				}
				
				try {
					Object val = left.calculate(ctx);
					if (val instanceof Sequence) {
						int pri = table.getColumnFilterPriority(column);
						return new FindsFilter(column, pri, (Sequence)val, dotoperator, true, isFilter);
					} else {
						return node;
					}
				} catch (Exception e) {
					return node;
				}
			}
		}
		return null;
	}
	
	private static void parseComma(ColPhyTable table, Node node, Context ctx, List<Object> out) {
		Node left = node.getLeft();
		if (left instanceof Comma) {
			parseComma(table, left, ctx, out);
		} else {
			if (left instanceof Assign) {
				Object obj = parseAssign(table, left, ctx, false);
				if (obj == null)
					throw new RuntimeException();
				else 
					out.add(obj);
			} else {
				throw new RuntimeException();
			}
		}
		
		Node right = node.getRight();
		if (right instanceof Assign) {
			Object obj = parseAssign(table, right, ctx, false);
			if (obj == null)
				throw new RuntimeException();
			else 
				out.add(obj);
			return;
		}

		if (right instanceof Constant) {
			if (Variant.isTrue(right.calculate(ctx))) {
				return;
			}
		} else {
			out.add(right);
		}
	}
	
	private static IFilter createFindFilter(ColumnMetaData column, int pri, Sequence data, boolean isNot) {
		if (data.length() > ContainFilter.BINARYSEARCH_COUNT) {
			if (isNot) {
				return new NotFindFilter(column, pri, data, null);
			} else {
				return new FindFilter(column, pri, data, null);
			}
		} else {
			// 元素数量较少时采用遍历发查找，因为字符串哈希较慢
			data = data.getPKeyValues();
			if (isNot) {
				return new NotContainFilter(column, pri, data, null, null);
			} else {
				return new ContainFilter(column, pri, data, null, null);
			}
		}
	}
	
	private static Object parseContain(ColPhyTable table, Node node, Context ctx, Context filterCtx) {
		if (node instanceof DotOperator) {
			if (node.getRight() instanceof Contain) {
				Contain contain = (Contain)node.getRight();
				IParam param = contain.getParam();
				if (param == null || !param.isLeaf()) {
					return node;
				}
				
				ColumnMetaData column = getColumn(table, param.getLeafExpression().getHome());
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
				
				ColumnMetaData column = getColumn(table, param.getLeafExpression().getHome());
				if (column == null) {
					return node;
				}
				
				try {
					Object val = node.getLeft().calculate(ctx);
					if (val instanceof Sequence) {
						int pri = table.getColumnFilterPriority(column);
						return createFindFilter(column, pri, (Sequence)val, false);
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
				
				ColumnMetaData column = getColumn(table, param.getLeafExpression().getHome());
				if (column == null) {
					return node;
				}
				
				try {
					Object val = dotNode.getLeft().calculate(ctx);
					if (val instanceof Sequence) {
						int pri = table.getColumnFilterPriority(column);
						return new NotContainFilter(column, pri, (Sequence)val, contain.getOption(), node);
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
				
				ColumnMetaData column = getColumn(table, param.getLeafExpression().getHome());
				if (column == null) {
					return node;
				}
				
				try {
					Object val = dotNode.getLeft().calculate(ctx);
					if (val instanceof Sequence) {
						int pri = table.getColumnFilterPriority(column);
						return createFindFilter(column, pri, (Sequence)val, true);
					} else {
						return node;
					}
				} catch (Exception e) {
					return node;
				}
			}
		}else if (node instanceof Assign) {
			//ki=wi
			Object obj = parseAssign(table, node, ctx, true);
			if (obj != null) 
				return obj;
		} else if (node instanceof Comma) {
			//(ki=wi,……,w)
			ArrayList<Object> list = new ArrayList<Object>();
			try {
				parseComma(table, node, ctx, list);
				return list;
			} catch (Exception e) {
				return node;
			}
		}
		
		return parseFieldExp(table, node, ctx, filterCtx);
	}
	
	
	// 如果node是单字段表达式则转成NodeFilter
	private static Object parseFieldExp(ColPhyTable table, Node node, Context ctx, Context filterCtx) {
		ArrayList<String> fieldList = new ArrayList<String>();
		node.getUsedFields(ctx, fieldList);
		ColumnMetaData column = null;
		
		for (String field : fieldList) {
			if (column == null) {
				column = table.getColumn(field);
			} else if (table.getColumn(field) != null) {
				return node;
			}
		}
		
		if (column != null) {
			int pri = table.getColumnFilterPriority(column);
			return new NodeFilter(column, pri, node, filterCtx);
		} else {
			return node;
		}
	}
	
	public static Object parseFilter(ColPhyTable table, Expression filter, Context ctx) {
		// 为IFilter新建上下文，有的IFilter需要为上下文增加变量用来引用字段值
		// 多线程时也需要用自己的上下文
		Context filterCtx = new Context(ctx);
		
		// 复制表达式，有的IFilter需要为上下文增加变量用来引用字段值
		// UnknownSymbol计算时会缓存变量，如果两个游标共用一个表达式则会受影响
		filter = filter.newExpression(ctx);
		
		Node node = filter.getHome();
		return parseFilter(table, node, ctx, filterCtx);
	}
	
	private static Object parseFilter(ColPhyTable table, Node node, Context ctx, Context filterCtx) {
		if (node instanceof And) {
			Object left = parseFilter(table, node.getLeft(), ctx, filterCtx);
			Object right = parseFilter(table, node.getRight(), ctx, filterCtx);
			return combineAnd(node, left, right);
		} else if (node instanceof Or) {
			Object left = parseFilter(table, node.getLeft(), ctx, filterCtx);
			Object right = parseFilter(table, node.getRight(), ctx, filterCtx);
			return combineOr(node, left, right);
		} else if (node instanceof Top) {
			return parseTop(table, (Top)node, ctx);
		} else if (node instanceof Between) {
			return parseBetween(table, (Between)node, ctx);
		} else if (node instanceof ElementRef) {
			return parseSeriesMember(table, (ElementRef)node, ctx);
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
				return parseContain(table, node, ctx, filterCtx);
			}
			
			Node left = node.getLeft();
			ColumnMetaData column = getColumn(table, left);
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

			return parseFieldExp(table, node, ctx, filterCtx);
		}
	}
	
	private void init() {
		try {
			table.appendCache();
		} catch (IOException e) {
			throw new RQException(e);
		}
		
		// 分析过滤表达式
		if (filter != null) {
			parseFilter();
		}
		if (fkNames != null) {
			parseSwitch(table, ctx);
		}
		
		//把filters里的ki=wi放到FindFilters里
		if (filters != null) {
			int len = filters.length;
			for (int i = 0; i < len; i++) {
				if (filters[i] instanceof FindsFilter) {
					if (findFilters == null) {
						findFilters = new FindFilter[len];
					}
					findFilters[i] = (FindsFilter) filters[i];
				}
			}
		}
		
		endBlock = table.getDataBlockCount();
		ColumnMetaData []columns;
		ArrayList<ColumnMetaData> expColumns = null;
		
		//处理子表对主表的共同和继承
		if (fields == null) {
			columns = table.getColumns();
			fields = table.getColNames();
		} else {
			if (exps != null) {
				columns = table.getColumns(exps);
				expColumns = table.getExpColumns(exps);
			} else {
				columns = table.getColumns(fields);
			}
		}
		
		ds = new DataStruct(fields);
		setDataStruct(ds);
		
		rowCountReader = table.getSegmentReader();
		int colCount = columns.length;
		
		if (expColumns != null) {
			ArrayList<ColumnMetaData> list = new ArrayList<ColumnMetaData>();
			for (ColumnMetaData col : columns) {
				list.add(col);
			}
			for (ColumnMetaData col : expColumns) {
				if (!list.contains(col)) {
					list.add(col);
				}
			}
			colCount = list.size();
			columns = new ColumnMetaData[colCount];
			list.toArray(columns);
		}
		
		if (filters == null) {
			colReaders = new BlockLinkReader[colCount];
			this.columns = columns;
			
			for (int i = 0; i < colCount; ++i) {
				if (columns[i] != null) {
					colReaders[i] = columns[i].getColReader(true);
				}
			}
		} else {
			ArrayList<ColumnMetaData> list = new ArrayList<ColumnMetaData>();
			for (IFilter filter : filters) {
				list.add(filter.getColumn());
			}
			
			for (ColumnMetaData col : columns) {
				if (col == null) {
					list.add(col);
				} else if (!list.contains(col)) {
					list.add(col);
				}
			}
			
			colCount = list.size();
			colReaders = new BlockLinkReader[colCount];
			segmentReaders = new ObjectReader[colCount];
			seqs = new int [colCount];
			this.columns = new ColumnMetaData[colCount];
			list.toArray(this.columns);
			
			for (int i = 0; i < colCount; ++i) {
				ColumnMetaData col = list.get(i);
				if (col != null) {
					colReaders[i] = col.getColReader(true);
					segmentReaders[i] = col.getSegmentReader();
					seqs[i] = ds.getFieldIndex(col.getColName());
				} else {
					seqs[i] = -1;
				}
			}
		}

		modifyRecords = table.getModifyRecords();
		if (filter != null && modifyRecords != null) {
			ArrayList<ModifyRecord> list = new ArrayList<ModifyRecord>();
			for (ModifyRecord mr : modifyRecords) {
				if (mr.isDelete()) {
					list.add(mr);
					continue;
				}
				Record sr = mr.getRecord();
				if (Variant.isTrue(sr.calc(filter, ctx))) {
					list.add(mr);
				} else {
					if (mr.isUpdate()) {
						list.add(new ModifyRecord(mr.getRecordSeq()));
					}
				}
			}
			if (list.size() == 0) {
				modifyRecords = null;
			} else {
				modifyRecords = list;
			}
		}
		
		if (modifyRecords != null) {
			mcount = modifyRecords.size();
			DataStruct srcDs = table.getDataStruct();
			findex = new int[fields.length];
			for (int i = 0; i < fields.length; ++i) {
				findex[i] = srcDs.getFieldIndex(fields[i]);
			}
		}
		
		/**
		 * 把exps里的附表表达式初始化
		 * T.f(x),T{}
		 */
		if (exps != null) {
			int size  = exps.length;
			gathers = new TableGather[size];
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
					} else if (exps[i].getHome() instanceof Moves) {
						Node left = exps[i].getHome().getLeft();
						if (left instanceof UnknownSymbol) {
							if (table.isSubTable(((UnknownSymbol) left).getName())) {
								gathers[i] = new TableGather(table, exps[i], ctx);
								exps[i] = null;
							}
						}
					} else if (exps[i].getHome() instanceof CurrentSeq) {
						gathers[i] = new TableSeqGather();
						exps[i] = null;
						if (names == null) {
							names = new String[size];
							names[i] = "#";
						} else if (names[i] == null) {
							names[i] = "#";
						}
					}
				}
			}
			
			isField = new boolean[colCount];
			String dsName[] = new String[colCount];
			for (int f = 0; f < colCount; ++f) {
				if (colReaders[f] != null) {
					isField[f] = true;
					dsName[f] = this.columns[f].getColName();
				}
			}
			tempDs = new DataStruct(dsName);
			for (int i = 0; i < colCount; ++i) {
				ColumnMetaData col = this.columns[i];
				if (col != null && colReaders[i] == null) {
					colReaders[i] = col.getColReader(true);
					segmentReaders[i] = col.getSegmentReader();
					seqs[i] = ds.getFieldIndex(col.getColName());
				}
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
			String []keys = table.getAllKeyColNames();
			String []sortedCols = table.getAllSortedColNames();
			ArrayList<String> pkeyList = new ArrayList<String>();
			ArrayList<String> sortedFieldList = new ArrayList<String>();
			DataStruct temp;
			if (fields != null) {
				temp = new DataStruct(fields);
			} else {
				temp = ds;
			}
			
			boolean signKey = true;
			for (String key : keys) {
				int idx = temp.getFieldIndex(key);
				if (idx == -1) {
					signKey = false;
					break;
				} else {
					pkeyList.add(ds.getFieldName(idx));
				}
			}
			for (String col : sortedCols) {
				int idx = temp.getFieldIndex(col);
				if (idx == -1) {
					break;
				} else {
					sortedFieldList.add(ds.getFieldName(idx));
				}
			}
			
			if (signKey) {
				//有主键
				int size = pkeyList.size();
				String[] pkeys = new String[size];
				pkeyList.toArray(pkeys);
				if (table.getGroupTable().hasTimeKey())
					ds.setPrimary(pkeys, "t");
				else
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
	
	protected int getInitSize(int n) {
		if (n != MAXSIZE) {
			return n;
		}
		
		long total = table.getTotalRecordCount() + mcount;
		int count;
		if (total > MAXSIZE) {
			count = MAXSIZE;
		} else {
			count = (int)total;
		}
		
		if (curBlock == 0 && endBlock == table.getDataBlockCount() && filters == null && codes == null) {
			return count;
		} else if (count > INITSIZE){
			return INITSIZE;
		} else {
			return count;
		}
	}
	
	protected Sequence get(int n) {
		// 修改了同步分段，同步分段时如果分段点是在块中某条记录，则使用appendData存放最后一块需要添加的记录
		isFirstSkip = false;
		Sequence seq;
		if (gathers == null) {
			seq = getData(n);
		} else {
			seq = getData2(n);
		}
		
		if (seq != null && seq.length() > n) {
			Sequence result = seq.split(1, n);
			cache = seq;
			return result;
		}
		
		if (appendIndex < 1) {
			return seq;
		} else {
			if (seq == null) {
				DataStruct ds = this.ds;
				Sequence appendData = this.appendData;
				int len = appendData.length();
				if (n >= len - appendIndex + 1) {
					Table table = new Table(ds, len - appendIndex + 1);
					IArray mems = table.getMems();
					for (int i = appendIndex; i <= len; ++i) {
						Record r = (Record)appendData.getMem(i);
						r.setDataStruct(ds);
						mems.add(r);
					}
					
					appendIndex = 0;
					return table;
				} else {
					Table table = new Table(ds, n);
					IArray mems = table.getMems();
					int appendIndex = this.appendIndex;
					for (int i = 0; i < n; ++i, ++appendIndex) {
						Record r = (Record)appendData.getMem(appendIndex);
						r.setDataStruct(ds);
						mems.add(r);
					}
					
					this.appendIndex = appendIndex;
					return table;
				}
			} else if (seq.length() == n) {
				return seq;
			} else {
				int diff = n - seq.length();
				DataStruct ds = this.ds;
				Sequence appendData = this.appendData;
				int len = appendData.length();
				int rest = len - appendIndex + 1;
				if (diff >= rest) {
					IArray mems = seq.getMems();
					for (int i = appendIndex; i <= len; ++i) {
						Record r = (Record)appendData.getMem(i);
						r.setDataStruct(ds);
						mems.add(r);
					}
					
					appendIndex = 0;
					return seq;
				} else {
					IArray mems = seq.getMems();
					int appendIndex = this.appendIndex;
					for (int i = 0; i < diff; ++i, ++appendIndex) {
						Record r = (Record)appendData.getMem(appendIndex);
						r.setDataStruct(ds);
						mems.add(r);
					}
					
					this.appendIndex = appendIndex;
					return seq;
				}
			}
		}
	}
	
	protected Sequence getData(int n) {
		if (isClosed || n < 1) {
			return null;
		}
		
		if (hasModify()) {
			return getModify(n);
		}
		
		Sequence cache = this.cache;
		if (cache != null) {
			int len = cache.length();
			if (len > n) {
				this.cache = (Sequence) cache.split(n + 1);
				return cache;
			} else if (len == n) {
				this.cache = null;
				return cache;
			}
		} else {
			cache = new Table(ds, getInitSize(n));
		}
		
		int curBlock = this.curBlock;
		int endBlock = this.endBlock;
		BlockLinkReader rowCountReader = this.rowCountReader;
		BlockLinkReader []colReaders = this.colReaders;
		int colCount = colReaders.length;
		BufferReader []bufReaders = new BufferReader[colCount];
		DataStruct ds = this.ds;
		IFilter []filters = this.filters;
		long prevRecordSeq = this.prevRecordSeq;
		
		IArray mems = cache.getMems();
		this.cache = null;

		try {
			if (filters == null) {
				while (curBlock < endBlock) {
					curBlock++;
					int recordCount = rowCountReader.readInt32();

					for (int f = 0; f < colCount; ++f) {
						bufReaders[f] = colReaders[f].readBlockData(recordCount);
					}
					
					int diff = n - cache.length();
					if (recordCount > diff) {
						int i = 0;
						for (; i < diff; ++i) {
							ComTableRecord r = new ComTableRecord(ds);
							for (int f = 0; f < colCount; ++f) {
								r.setNormalFieldValue(f, bufReaders[f].readObject());
							}
							r.setRecordSeq(++prevRecordSeq);
							mems.add(r);
						}
						
						Table tmp = new Table(ds, ICursor.FETCHCOUNT);
						this.cache = tmp;
						mems = tmp.getMems();
						
						for (; i < recordCount; ++i) {
							ComTableRecord r = new ComTableRecord(ds);
							for (int f = 0; f < colCount; ++f) {
								r.setNormalFieldValue(f, bufReaders[f].readObject());
							}
							r.setRecordSeq(++prevRecordSeq);
							mems.add(r);
						}
						
						break;
					} else {
						for (int i = 0; i < recordCount; ++i) {
							ComTableRecord r = new ComTableRecord(ds);
							for (int f = 0; f < colCount; ++f) {
								r.setNormalFieldValue(f, bufReaders[f].readObject());
							}
							r.setRecordSeq(++prevRecordSeq);
							mems.add(r);
						}
						
						if (diff == recordCount) {
							break;
						}
					}
				}
			} else if (filters.length == 1) {
				FindFilter findFilter = null;
				if (findFilters != null) {
					findFilter = findFilters[0];
				}
				
				ColumnMetaData []columns = this.columns;
				int []seqs = this.seqs;
				ObjectReader []segmentReaders = this.segmentReaders;
				long []positions = new long[colCount];
				IFilter filter = filters[0];
				
				while (curBlock < endBlock) {
					curBlock++;
					int recordCount = rowCountReader.readInt32();
					
					for (int f = 1; f < colCount; ++f) {
						positions[f] = segmentReaders[f].readLong40();
						if (columns[f].hasMaxMinValues()) {
							segmentReaders[f].skipObject();
							segmentReaders[f].skipObject();
							segmentReaders[f].skipObject();
						}
					}
					
					positions[0] = segmentReaders[0].readLong40();
					if (columns[0].hasMaxMinValues()) {
						Object minValue = segmentReaders[0].readObject();
						Object maxValue = segmentReaders[0].readObject();
						segmentReaders[0].skipObject();
						if (!filter.match(minValue, maxValue)) {
							continue;
						}
					}
					
					int nextRow = 0; // 普通列下一个要读的行，如果还没到当前要读的行则跳到
					BufferReader filterReader = colReaders[0].readBlockData(positions[0], recordCount);
					for (int f = 1; f < colCount; ++f) {
						bufReaders[f] = null;
					}
					
					for (int i = 0; i < recordCount; ++i) {
						// 按记录数循环，如果列的BufferReader没有产生则产生并跳到当前要读的行
						Object val = filterReader.readObject();
						if (!filter.match(val)) {
							continue;
						}
						
						Record r = new Record(ds);
						mems.add(r);
						if (seqs[0] != -1) {
							if (findFilter == null) {
								r.setNormalFieldValue(seqs[0], val);
							} else {
								r.setNormalFieldValue(seqs[0], findFilter.getFindResult());
							}
						}
						
						for (int f = 1; f < colCount; ++f) {
							if (bufReaders[f] == null) {
								bufReaders[f] = colReaders[f].readBlockData(positions[f], recordCount);
							}
							
							for (int j = nextRow; j < i; ++j) {
								bufReaders[f].skipObject();
							}
							
							if (seqs[f] != -1) {
								r.setNormalFieldValue(seqs[f], bufReaders[f].readObject());
							} else {
								bufReaders[f].skipObject();
							}
						}
						
						nextRow = i + 1;
					}
					
					int diff = n - cache.length();
					if (diff < 0) {
						this.cache = (Sequence) cache.split(n + 1);
						break;
					} else if (diff == 0) {
						break;
					}
				}
			} else {
				FindFilter []findFilters = this.findFilters;
				ColumnMetaData []columns = this.columns;
				int []seqs = this.seqs;
				ObjectReader []segmentReaders = this.segmentReaders;
				int filterCount = filters.length;
				long []positions = new long[colCount];
				
				while (curBlock < endBlock) {
					curBlock++;
					int recordCount = rowCountReader.readInt32();
					boolean sign = true;
					int f = 0;
					for (; f < filterCount; ++f) {
						positions[f] = segmentReaders[f].readLong40();
						if (columns[f].hasMaxMinValues()) {
							Object minValue = segmentReaders[f].readObject();
							Object maxValue = segmentReaders[f].readObject();
							segmentReaders[f].skipObject();
							if (!filters[f].match(minValue, maxValue)) {
								++f;
								sign = false;
								break;
							}
						}
					}
					
					for (; f < colCount; ++f) {
						positions[f] = segmentReaders[f].readLong40();
						if (columns[f].hasMaxMinValues()) {
							segmentReaders[f].skipObject();
							segmentReaders[f].skipObject();
							segmentReaders[f].skipObject();
						}
					}
					
					if (!sign) {
						continue;
					}
					
					int []nextRows = new int[colCount]; // 每个bufReaders下一条要读到的行，如果还没到当前要读的行则跳到
					Object []fvalues = new Object[colCount]; // 当前行条件字段的值

					for (f = 0; f < colCount; ++f) {
						bufReaders[f] = null;
					}
					
					Next:
					for (int i = 0; i < recordCount; ++i) {
						// 按记录数循环，如果列的BufferReader没有产生则产生并跳到当前要读的行
						for (f = 0; f < filterCount; ++f) {
							if (bufReaders[f] == null) 
							
							{
								bufReaders[f] = colReaders[f].readBlockData(positions[f], recordCount);
							}
							
							for (int j = nextRows[f]; j < i; ++j) {
								bufReaders[f].skipObject();
							}
							
							nextRows[f] = i + 1;
							fvalues[f] = bufReaders[f].readObject();
							if (!filters[f].match(fvalues[f])) {
								continue Next;
							}
						}
						
						Record r = new Record(ds);
						mems.add(r);
						for (f = 0; f < filterCount; ++f) {
							if (seqs[f] != -1) {
								if (findFilters == null || findFilters[f] == null) {
									r.setNormalFieldValue(seqs[f], fvalues[f]);
								} else {
									r.setNormalFieldValue(seqs[f], findFilters[f].getFindResult());
								}
							}
						}
						
						for (; f < colCount; ++f) {
							if (bufReaders[f] == null) {
								bufReaders[f] = colReaders[f].readBlockData(positions[f], recordCount);
							}
							
							for (int j = nextRows[f]; j < i; ++j) {
								bufReaders[f].skipObject();
							}
							
							nextRows[f] = i + 1;
							if (seqs[f] != -1) {
								r.setNormalFieldValue(seqs[f], bufReaders[f].readObject());
							} else {
								bufReaders[f].skipObject();
							}
						}
					}
					
					int diff = n - cache.length();
					if (diff < 0) {
						this.cache = (Sequence) cache.split(n + 1);
						break;
					} else if (diff == 0) {
						break;
					}
				}
			}
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		}
		
		this.curBlock = curBlock;
		this.prevRecordSeq = prevRecordSeq;
		if (cache.length() > 0) {
			return cache;
		} else {
			return null;
		}
	}
	
	//有字段表达式时用这个
	private Sequence getData2(int n) {
		if (isClosed || n < 1) {
			return null;
		}
		
		if (hasModify()) {
			return getModify2(n);
		}
		
		Sequence cache = this.cache;
		if (cache != null) {
			int len = cache.length();
			if (len > n) {
				this.cache = (Sequence) cache.split(n + 1);
				return cache;
			} else if (len == n) {
				this.cache = null;
				return cache;
			}
		} else {
			cache = new Table(ds, getInitSize(n));
		}
		
		int curBlock = this.curBlock;
		int endBlock = this.endBlock;
		BlockLinkReader rowCountReader = this.rowCountReader;
		BlockLinkReader []colReaders = this.colReaders;
		int colCount = colReaders.length;
		BufferReader []bufReaders = new BufferReader[colCount];
		DataStruct ds = this.ds;
		IFilter []filters = this.filters;
		long prevRecordSeq = this.prevRecordSeq;
		
		IArray mems = cache.getMems();
		this.cache = null;

		TableGather []gathers = this.gathers;
		boolean isField[] = this.isField;
		int retColCount = exps.length;
		Record temp = new Record(this.tempDs);
		Expression []exps = this.exps;
		Context ctx = this.ctx;
		
		try {
			if (filters == null) {
				while (curBlock < endBlock) {
					curBlock++;
					int recordCount = rowCountReader.readInt32();

					for (int f = 0; f < colCount; ++f) {
						if (isField[f]) {
							bufReaders[f] = colReaders[f].readBlockData(recordCount);
						}
					}
					
					for (TableGather gather : gathers) {
						if (gather != null) {
							gather.loadData();
						}
					}
					
					int diff = n - cache.length();
					if (recordCount > diff) {
						int i = 0;
						for (; i < diff; ++i) {
							for (int f = 0; f < colCount; ++f) {
								if (isField[f]) {
									temp.setNormalFieldValue(f, bufReaders[f].readObject());
								}
							}
							ComTableRecord r = new ComTableRecord(ds);
							for (int f = 0; f < retColCount; ++f) {
								if (exps[f] != null) {
									r.setNormalFieldValue(f, temp.calc(exps[f], ctx));
								} else if (isField[f]) {
									r.setNormalFieldValue(f, temp.getFieldValue(f));
								} else {
									if (gathers[f] != null)
										r.setNormalFieldValue(f, gathers[f].getNextBySeq(prevRecordSeq + 1));
								}
							}
							r.setRecordSeq(++prevRecordSeq);
							mems.add(r);
						}
						
						Table tmp = new Table(ds, ICursor.FETCHCOUNT);
						this.cache = tmp;
						mems = tmp.getMems();
						
						for (; i < recordCount; ++i) {
							for (int f = 0; f < colCount; ++f) {
								if (isField[f]) {
									temp.setNormalFieldValue(f, bufReaders[f].readObject());
								}
							}
							ComTableRecord r = new ComTableRecord(ds);
							for (int f = 0; f < retColCount; ++f) {
								if (exps[f] != null) {
									r.setNormalFieldValue(f, temp.calc(exps[f], ctx));
								} else if (isField[f]) {
									r.setNormalFieldValue(f, temp.getFieldValue(f));
								} else {
									if (gathers[f] != null)
										r.setNormalFieldValue(f, gathers[f].getNextBySeq(prevRecordSeq + 1));
								}
							}
							r.setRecordSeq(++prevRecordSeq);
							mems.add(r);
						}
						
						break;
					} else {
						for (int i = 0; i < recordCount; ++i) {
							for (int f = 0; f < colCount; ++f) {
								if (isField[f]) {
									temp.setNormalFieldValue(f, bufReaders[f].readObject());
								}
							}
							ComTableRecord r = new ComTableRecord(ds);
							for (int f = 0; f < retColCount; ++f) {
								if (exps[f] != null) {
									r.setNormalFieldValue(f, temp.calc(exps[f], ctx));
								} else if (isField[f]) {
									r.setNormalFieldValue(f, temp.getFieldValue(f));
								} else {
									if (gathers[f] != null)
										r.setNormalFieldValue(f, gathers[f].getNextBySeq(prevRecordSeq + 1));
								}
							}
							r.setRecordSeq(++prevRecordSeq);
							mems.add(r);
						}
						
						if (diff == recordCount) {
							break;
						}
					}
				}
			} else {
				FindFilter []findFilters = this.findFilters;
				ColumnMetaData []columns = this.columns;
				int []seqs = this.seqs;
				ObjectReader []segmentReaders = this.segmentReaders;
				int filterCount = filters.length;
				long []positions = new long[colCount];
				Object [][]filterValues = new Object[filterCount][];
				
				while (curBlock < endBlock) {
					curBlock++;
					int recordCount = rowCountReader.readInt32();
					boolean sign = true;
					int f = 0;
					for (; f < filterCount; ++f) {
						positions[f] = segmentReaders[f].readLong40();
						if (columns[f].hasMaxMinValues()) {
							Object minValue = segmentReaders[f].readObject();
							Object maxValue = segmentReaders[f].readObject();
							segmentReaders[f].skipObject();
							if (!filters[f].match(minValue, maxValue)) {
								++f;
								sign = false;
								break;
							}
						}
					}
					
					for (; f < colCount; ++f) {
						if (!isField[f]) continue;
						positions[f] = segmentReaders[f].readLong40();
						if (columns[f].hasMaxMinValues()) {
							segmentReaders[f].skipObject();
							segmentReaders[f].skipObject();
							segmentReaders[f].skipObject();
						}
					}
					
					if (!sign) {
						prevRecordSeq += recordCount;
						for (TableGather gather : gathers) {
							if (gather != null) {
								gather.skip();
							}
						}
						continue;
					}
					
					boolean []matchs = new boolean[recordCount];
					int matchCount = recordCount;
					for (int i = 0; i < recordCount; ++i) {
						matchs[i] = true;
					}

					for (f = 0; f < filterCount && matchCount > 0; ++f) {
						FindFilter findFilter = null;
						if (findFilters != null) {
							findFilter = findFilters[f];
						}
						
						Object []curValues = new Object[recordCount];
						filterValues[f] = curValues;
						IFilter filter = filters[f];
						BufferReader reader = colReaders[f].readBlockData(positions[f], recordCount);
						for (int i = 0; i < recordCount; ++i) {
							if (matchs[i]) {
								curValues[i] = reader.readObject();
								if (!filter.match(curValues[i])) {
									matchs[i] = false;
									matchCount--;
									if (matchCount == 0) {
										break;
									}
								} else if (findFilter != null) {
									curValues[i] = findFilter.getFindResult();
								}
							} else {
								reader.skipObject();
							}
						}
					}
					
					if (matchCount < 1) {
						prevRecordSeq += recordCount;
						for (TableGather gather : gathers) {
							if (gather != null) {
								gather.skip();
							}
						}
						continue;
					}
					for (TableGather gather : gathers) {
						if (gather != null) {
							gather.loadData();
						}
					}
					
					for (; f < colCount; ++f) {
						if (isField[f])
							bufReaders[f] = colReaders[f].readBlockData(positions[f], recordCount);
					}
					
					for (int i = 0; i < recordCount && matchCount > 0; ++i) {
						if (matchs[i]) {
							matchCount--;
							
							Record r = new Record(ds);
							for (f = 0; f < filterCount; ++f) {
								temp.setNormalFieldValue(f, filterValues[f][i]);
								if (seqs[f] >= 0) {
									r.setNormalFieldValue(seqs[f], filterValues[f][i]);
								}
							}
							for (; f < colCount; ++f) {
								if (isField[f]) {
									Object obj = bufReaders[f].readObject();
									if (seqs[f] >= 0) {
										r.setNormalFieldValue(seqs[f], obj);
									}
									temp.setNormalFieldValue(f, obj);
								}
							}
							
							for (f = 0; f < retColCount; ++f) {
								if (exps[f] != null) {
									r.setNormalFieldValue(f, temp.calc(exps[f], ctx));
								} else {
									if (gathers[f] != null)
										r.setNormalFieldValue(f, gathers[f].getNextBySeq(prevRecordSeq + i + 1));
								}
							}
							
							
							mems.add(r);
						} else {
							for (f = filterCount; f < colCount; ++f) {
								if (isField[f]) {
									bufReaders[f].skipObject();
								}
							}
						}
					}
					prevRecordSeq += recordCount;
					
					int diff = n - cache.length();
					if (diff < 0) {
						this.cache = (Sequence) cache.split(n + 1);
						break;
					} else if (diff == 0) {
						break;
					}
				}
			}
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		}
		
		this.curBlock = curBlock;
		this.prevRecordSeq = prevRecordSeq;
		if (cache.length() > 0) {
			return cache;
		} else {
			return null;
		}
	}
	
	private int getModifyRecord(int mindex, long endRecordSeq, Sequence result) {
		ArrayList<ModifyRecord> modifyRecords = this.modifyRecords;
		int []findex = this.findex;
		DataStruct ds = this.ds;
		int colCount = findex.length;
		int mcount = this.mcount;
		
		for (; mindex < mcount; ++mindex) {
			ModifyRecord mr = modifyRecords.get(mindex);
			if (mr.getRecordSeq() <= endRecordSeq) {
				if (!mr.isDelete()) {
					Record sr = mr.getRecord();
					if (Variant.isTrue(sr.calc(filter, ctx))) {
						Record r = new Record(ds);
						for (int f = 0; f < colCount; ++f) {
							r.setNormalFieldValue(f, sr.getNormalFieldValue(findex[f]));
						}
						
						result.add(r);
					}
				}
			} else {
				break;
			}
		}
		
		return mindex;
	}
	
	protected Sequence getModify(int n) {
		Sequence cache = this.cache;
		if (cache != null) {
			int len = cache.length();
			if (len > n) {
				this.cache = (Sequence) cache.split(n + 1);
				return cache;
			} else if (len == n) {
				this.cache = null;
				return cache;
			}
		} else {
			cache = new Table(ds, getInitSize(n));
		}
		
		int curBlock = this.curBlock;
		int endBlock = this.endBlock;
		BlockLinkReader rowCountReader = this.rowCountReader;
		BlockLinkReader []colReaders = this.colReaders;
		int colCount = colReaders.length;
		BufferReader []bufReaders = new BufferReader[colCount];
		DataStruct ds = this.ds;
		IFilter []filters = this.filters;
		
		IArray mems = cache.getMems();
		this.cache = null;
		long prevRecordSeq = this.prevRecordSeq;
		ArrayList<ModifyRecord> modifyRecords = this.modifyRecords;
		int []findex = this.findex;
		int findexLen = findex.length;
		int mindex = this.mindex;
		int mcount = this.mcount;
		
		ModifyRecord mr = modifyRecords.get(mindex);
		long mseq = mr.getRecordSeq();
				
		try {
			if (filters == null) {
				while (curBlock < endBlock) {
					curBlock++;
					int recordCount = rowCountReader.readInt32();
					for (int f = 0; f < colCount; ++f) {
						bufReaders[f] = colReaders[f].readBlockData(recordCount);
					}
					
					for (int i = 0; i < recordCount; ++i) {
						prevRecordSeq++;
						if (prevRecordSeq != mseq) {
							ComTableRecord r = new ComTableRecord(ds);
							for (int f = 0; f < colCount; ++f) {
								r.setNormalFieldValue(f, bufReaders[f].readObject());
							}
							
							r.setRecordSeq(prevRecordSeq);
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
									for (int f = 0; f < findexLen; ++f) {
										r.setNormalFieldValue(f, sr.getNormalFieldValue(findex[f]));
									}
									
									r.setRecordSeq(-mseq);
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
								ComTableRecord r = new ComTableRecord(ds);
								for (int f = 0; f < colCount; ++f) {
									r.setNormalFieldValue(f, bufReaders[f].readObject());
								}
								
								r.setRecordSeq(prevRecordSeq);
								mems.add(r);
							} else {
								for (int f = 0; f < colCount; ++f) {
									bufReaders[f].skipObject();
								}
							}
						}
					}
					
					if (curBlock == endBlock && endBlock == table.getDataBlockCount()) {
						for (; mindex < mcount; ++mindex) {
							// 可能存在内存追加的记录在补区
							mr = modifyRecords.get(mindex);
							mseq = mr.getRecordSeq();
							Record sr = mr.getRecord();
							ComTableRecord r = new ComTableRecord(ds);
							for (int f = 0; f < findexLen; ++f) {
								r.setNormalFieldValue(f, sr.getNormalFieldValue(findex[f]));
							}
							
							r.setRecordSeq(-mseq);
							mems.add(r);
						}
					}
					
					int diff = n - cache.length();
					if (diff < 0) {
						this.cache = (Sequence) cache.split(n + 1);
						break;
					} else if (diff == 0) {
						break;
					}
				}
				
				if (table.getDataBlockCount() == 0 && startBlock == 0 && endBlock == 0) {
					// 表中还没有添加数据时有可能在补区有数据
					for (; mindex < mcount; ++mindex) {
						// 可能存在内存追加的记录在补区
						mr = modifyRecords.get(mindex);
						mseq = mr.getRecordSeq();
						Record sr = mr.getRecord();
						ComTableRecord r = new ComTableRecord(ds);
						for (int f = 0; f < findexLen; ++f) {
							r.setNormalFieldValue(f, sr.getNormalFieldValue(findex[f]));
						}
						
						r.setRecordSeq(-mseq);
						mems.add(r);
					}
					
					int diff = n - cache.length();
					if (diff < 0) {
						this.cache = (Sequence) cache.split(n + 1);
					}
				}
			} else {
				ColumnMetaData []columns = this.columns;
				int []seqs = this.seqs;
				FindFilter[] findFilters = this.findFilters;
				ObjectReader []segmentReaders = this.segmentReaders;
				int filterCount = filters.length;
				long []positions = new long[colCount];
				Object [][]filterValues = new Object[filterCount][];
				
				while (curBlock < endBlock) {
					curBlock++;
					int recordCount = rowCountReader.readInt32();
					boolean sign = true;
					int f = 0;
					for (; f < filterCount; ++f) {
						positions[f] = segmentReaders[f].readLong40();
						if (columns[f].hasMaxMinValues()) {
							Object minValue = segmentReaders[f].readObject();
							Object maxValue = segmentReaders[f].readObject();
							segmentReaders[f].skipObject();
							if (!filters[f].match(minValue, maxValue)) {
								++f;
								sign = false;
								break;
							}
						}
					}
					
					for (; f < colCount; ++f) {
						positions[f] = segmentReaders[f].readLong40();
						if (columns[f].hasMaxMinValues()) {
							segmentReaders[f].skipObject();
							segmentReaders[f].skipObject();
							segmentReaders[f].skipObject();
						}
					}
					
					if (!sign) {
						prevRecordSeq += recordCount;
						mindex = getModifyRecord(mindex, prevRecordSeq, cache);
						if (mindex < mcount) {
							mr = modifyRecords.get(mindex);
							mseq = mr.getRecordSeq();
						} else {
							mr = null;
							mseq = -1;
						}
						continue;
					}
					
					boolean []matchs = new boolean[recordCount];
					int matchCount = recordCount;
					for (int i = 0; i < recordCount; ++i) {
						matchs[i] = true;
					}
					
					for (f = 0; f < filterCount && matchCount > 0; ++f) {
						Object []curValues = new Object[recordCount];
						filterValues[f] = curValues;
						IFilter filter = filters[f];
						BufferReader reader = colReaders[f].readBlockData(positions[f], recordCount);
						for (int i = 0; i < recordCount; ++i) {
							if (matchs[i]) {
								curValues[i] = reader.readObject();
								if (!filter.match(curValues[i])) {
									matchs[i] = false;
									matchCount--;
									if (matchCount == 0) {
										break;
									}
								} else {
									if (seqs[f] != -1 && findFilters != null && findFilters[f] != null) {
										curValues[i] = findFilters[f].getFindResult();
									}
								}
							} else {
								reader.skipObject();
							}
						}
					}
					
					if (matchCount < 1) {
						prevRecordSeq += recordCount;
						mindex = getModifyRecord(mindex, prevRecordSeq, cache);
						if (mindex < mcount) {
							mr = modifyRecords.get(mindex);
							mseq = mr.getRecordSeq();
						} else {
							mr = null;
							mseq = -1;
						}
						continue;
					}
					
					for (; f < colCount; ++f) {
						bufReaders[f] = colReaders[f].readBlockData(positions[f], recordCount);
					}
					
					for (int i = 0; i < recordCount; ++i) {
						prevRecordSeq++;
						boolean isInsert = true;
						
						if (prevRecordSeq == mseq) {
							while (true) {
								if (mr.isDelete()) {
									isInsert = false;
								} else {
									if (mr.isUpdate()) {
										isInsert = false;
									}
									
									Record sr = mr.getRecord();
									if (Variant.isTrue(sr.calc(filter, ctx))) {
										Record r = new Record(ds);
										for (f = 0; f < findexLen; ++f) {
											r.setNormalFieldValue(f, sr.getNormalFieldValue(findex[f]));
										}
										
										mems.add(r);
									}
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
						}
						
						if (isInsert && matchs[i]) {
							matchCount--;
							Record r = new Record(ds);
							for (f = 0; f < filterCount; ++f) {								
								if (seqs[f] != -1) {
									r.setNormalFieldValue(seqs[f], filterValues[f][i]);
								}
							}
							
							for (; f < colCount; ++f) {
								r.setNormalFieldValue(seqs[f], bufReaders[f].readObject());
							}
							
							mems.add(r);
						} else if (matchCount > 0) {
							for (f = filterCount; f < colCount; ++f) {
								bufReaders[f].skipObject();
							}
						}
					}
					
					if (curBlock == endBlock && endBlock == table.getDataBlockCount()) {
						for (; mindex < mcount; ++mindex) {
							// 可能存在内存追加的记录在补区
							mr = modifyRecords.get(mindex);
							Record sr = mr.getRecord();
							if (Variant.isTrue(sr.calc(filter, ctx))) {
								Record r = new Record(ds);
								for (f = 0; f < findexLen; ++f) {
									r.setNormalFieldValue(f, sr.getNormalFieldValue(findex[f]));
								}
								
								mems.add(r);
							}
						}
					}
					
					int diff = n - cache.length();
					if (diff < 0) {
						this.cache = (Sequence) cache.split(n + 1);
						break;
					} else if (diff == 0) {
						break;
					}
				}
				
				if (table.getDataBlockCount() == 0 && startBlock == 0 && endBlock == 0) {
					// 表中还没有添加数据时有可能在补区有数据
					for (; mindex < mcount; ++mindex) {
						// 可能存在内存追加的记录在补区
						mr = modifyRecords.get(mindex);
						Record sr = mr.getRecord();
						if (Variant.isTrue(sr.calc(filter, ctx))) {
							Record r = new Record(ds);
							for (int f = 0; f < findexLen; ++f) {
								r.setNormalFieldValue(f, sr.getNormalFieldValue(findex[f]));
							}
							
							mems.add(r);
						}
					}
					
					int diff = n - cache.length();
					if (diff < 0) {
						this.cache = (Sequence) cache.split(n + 1);
					}
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
	
	private Sequence getModify2(int n) {
		Sequence cache = this.cache;
		if (cache != null) {
			int len = cache.length();
			if (len > n) {
				this.cache = (Sequence) cache.split(n + 1);
				return cache;
			} else if (len == n) {
				this.cache = null;
				return cache;
			}
		} else {
			cache = new Table(ds, getInitSize(n));
		}
		
		int curBlock = this.curBlock;
		int endBlock = this.endBlock;
		BlockLinkReader rowCountReader = this.rowCountReader;
		BlockLinkReader []colReaders = this.colReaders;
		int colCount = colReaders.length;
		BufferReader []bufReaders = new BufferReader[colCount];
		DataStruct ds = this.ds;
		IFilter []filters = this.filters;
		
		IArray mems = cache.getMems();
		this.cache = null;
		long prevRecordSeq = this.prevRecordSeq;
		ArrayList<ModifyRecord> modifyRecords = this.modifyRecords;
		int []findex = this.findex;
		int mindex = this.mindex;
		int mcount = this.mcount;
		
		ModifyRecord mr = modifyRecords.get(mindex);
		long mseq = mr.getRecordSeq();
		
		TableGather []gathers = this.gathers;
		boolean isField[] = this.isField;
		int retColCount = exps.length;
		Record temp = new Record(this.tempDs);
		Expression []exps = this.exps;
		Context ctx = this.ctx;
		
		try {
			if (filters == null) {
				while (curBlock < endBlock) {
					curBlock++;
					int recordCount = rowCountReader.readInt32();
					for (int f = 0; f < colCount; ++f) {
						if (isField[f]) {
							bufReaders[f] = colReaders[f].readBlockData(recordCount);
						}
					}
					
					for (TableGather gather : gathers) {
						if (gather != null) {
							gather.loadData();
						}
					}

					for (int i = 0; i < recordCount; ++i) {
						prevRecordSeq++;
						if (prevRecordSeq != mseq) {
							ComTableRecord r = new ComTableRecord(ds);
							for (int f = 0; f < colCount; ++f) {
								if (isField[f]) {
									temp.setNormalFieldValue(f, bufReaders[f].readObject());
								}
							}
							for (int f = 0; f < retColCount; ++f) {
								if (exps[f] != null) {
									r.setNormalFieldValue(f, temp.calc(exps[f], ctx));
								} else if (isField[f]) {
									r.setNormalFieldValue(f, temp.getFieldValue(f));
								} else {
									if (gathers[f] != null)
										r.setNormalFieldValue(f, gathers[f].getNextBySeq(prevRecordSeq));
								}
							}

							r.setRecordSeq(prevRecordSeq);
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
									for (int f = 0; f < colCount; ++f) {
										if (exps[f] != null) {
											r.setNormalFieldValue(f, sr.calc(exps[f], ctx));
										} else if (isField[f]) {
											r.setNormalFieldValue(f, sr.getNormalFieldValue(findex[f]));
										} else {
											if (gathers[f] != null) {
												if (isInsert) {
													r.setNormalFieldValue(f, gathers[f].getNextBySeq(-(mindex + 1)));
												} else {
													r.setNormalFieldValue(f, gathers[f].getNextBySeq(prevRecordSeq));
												}
											}
										}
									}
									
									r.setRecordSeq(-mseq);
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
								ComTableRecord r = new ComTableRecord(ds);
								for (int f = 0; f < colCount; ++f) {
									if (isField[f]) {
										temp.setNormalFieldValue(f, bufReaders[f].readObject());
									}
								}
								for (int f = 0; f < retColCount; ++f) {
									if (exps[f] != null) {
										r.setNormalFieldValue(f, temp.calc(exps[f], ctx));
									} else if (isField[f]) {
										r.setNormalFieldValue(f, temp.getFieldValue(f));
									} else {
										if (gathers[f] != null)
											r.setNormalFieldValue(f, gathers[f].getNextBySeq(prevRecordSeq));
									}
								}
								r.setRecordSeq(prevRecordSeq);
								mems.add(r);
							} else {
								for (int f = 0; f < colCount; ++f) {
									if (isField[f]) {
										bufReaders[f].skipObject();
									}
								}
							}
						}
					}
					
					if (curBlock == endBlock && endBlock == table.getDataBlockCount()) {
						for (; mindex < mcount; ++mindex) {
							// 可能存在内存追加的记录在补区
							mr = modifyRecords.get(mindex);
							Record sr = mr.getRecord();
							ComTableRecord r = new ComTableRecord(ds);
							for (int f = 0; f < colCount; ++f) {
								if (exps[f] != null) {
									r.setNormalFieldValue(f, sr.calc(exps[f], ctx));
								} else if (isField[f]) {
									r.setNormalFieldValue(f, sr.getNormalFieldValue(findex[f]));
								} else {
									if (gathers[f] != null) {
										r.setNormalFieldValue(f, gathers[f].getNextBySeq(-(mindex + 1)));
									}
								}
							}
							
							r.setRecordSeq(-mseq);
							mems.add(r);
						}
					}
					
					int diff = n - cache.length();
					if (diff < 0) {
						this.cache = (Sequence) cache.split(n + 1);
						break;
					} else if (diff == 0) {
						break;
					}
				}
				
				if (table.getDataBlockCount() == 0 && startBlock == 0 && endBlock == 0) {
					// 表中还没有添加数据时有可能在补区有数据
					for (; mindex < mcount; ++mindex) {
						// 可能存在内存追加的记录在补区
						mr = modifyRecords.get(mindex);
						Record sr = mr.getRecord();
						ComTableRecord r = new ComTableRecord(ds);
						for (int f = 0; f < colCount; ++f) {
							if (exps[f] != null) {
								r.setNormalFieldValue(f, sr.calc(exps[f], ctx));
							} else if (isField[f]) {
								r.setNormalFieldValue(f, sr.getNormalFieldValue(findex[f]));
							} else {
								if (gathers[f] != null) {
									r.setNormalFieldValue(f, gathers[f].getNextBySeq(-(mindex + 1)));
								}
							}
						}
						
						r.setRecordSeq(-mseq);
						mems.add(r);
					}
				}
			} else {
				ColumnMetaData []columns = this.columns;
				int []seqs = this.seqs;
				ObjectReader []segmentReaders = this.segmentReaders;
				int filterCount = filters.length;
				long []positions = new long[colCount];
				Object [][]filterValues = new Object[filterCount][];
				
				while (curBlock < endBlock) {
					curBlock++;
					int recordCount = rowCountReader.readInt32();
					boolean sign = true;
					int f = 0;
					for (; f < filterCount; ++f) {
						positions[f] = segmentReaders[f].readLong40();
						if (columns[f].hasMaxMinValues()) {
							Object minValue = segmentReaders[f].readObject();
							Object maxValue = segmentReaders[f].readObject();
							segmentReaders[f].skipObject();
							if (!filters[f].match(minValue, maxValue)) {
								++f;
								sign = false;
								break;
							}
						}
					}
					
					for (; f < colCount; ++f) {
						if (!isField[f]) continue;
						positions[f] = segmentReaders[f].readLong40();
						if (columns[f].hasMaxMinValues()) {
							segmentReaders[f].skipObject();
							segmentReaders[f].skipObject();
							segmentReaders[f].skipObject();
						}
					}
					
					if (!sign) {
						prevRecordSeq += recordCount;
						mindex = getModifyRecord(mindex, prevRecordSeq, cache);
						if (mindex < mcount) {
							mr = modifyRecords.get(mindex);
							mseq = mr.getRecordSeq();
						} else {
							mr = null;
							mseq = -1;
						}
						for (TableGather gather : gathers) {
							if (gather != null) {
								gather.skip();
							}
						}
						continue;
					}
					
					boolean []matchs = new boolean[recordCount];
					int matchCount = recordCount;
					for (int i = 0; i < recordCount; ++i) {
						matchs[i] = true;
					}
					
					for (f = 0; f < filterCount && matchCount > 0; ++f) {
						Object []curValues = new Object[recordCount];
						filterValues[f] = curValues;
						IFilter filter = filters[f];
						BufferReader reader = colReaders[f].readBlockData(positions[f], recordCount);
						for (int i = 0; i < recordCount; ++i) {
							if (matchs[i]) {
								curValues[i] = reader.readObject();
								if (!filter.match(curValues[i])) {
									matchs[i] = false;
									matchCount--;
									if (matchCount == 0) {
										break;
									}
								}
							} else {
								reader.skipObject();
							}
						}
					}
					
					if (matchCount < 1) {
						prevRecordSeq += recordCount;
						mindex = getModifyRecord(mindex, prevRecordSeq, cache);
						if (mindex < mcount) {
							mr = modifyRecords.get(mindex);
							mseq = mr.getRecordSeq();
						} else {
							mr = null;
							mseq = -1;
						}
						for (TableGather gather : gathers) {
							if (gather != null) {
								gather.skip();
							}
						}
						continue;
					}
					for (TableGather gather : gathers) {
						if (gather != null) {
							gather.loadData();
						}
					}

					for (; f < colCount; ++f) {
						if (isField[f])
							bufReaders[f] = colReaders[f].readBlockData(positions[f], recordCount);
					}
					
					for (int i = 0; i < recordCount; ++i) {
						prevRecordSeq++;
						boolean isInsert = true;
						
						if (prevRecordSeq == mseq) {
							while (true) {
								if (mr.isDelete()) {
									isInsert = false;
								} else {
									if (mr.isUpdate()) {
										isInsert = false;
									}
									
									Record sr = mr.getRecord();
									if (Variant.isTrue(sr.calc(filter, ctx))) {
										Record r = new Record(ds);
//										for (f = 0; f < colCount; ++f) {
//											if (isField[f]) {
//												r.setNormalFieldValue(f, sr.getNormalFieldValue(findex[f]));
//											}
//										}
										for (f = 0; f < retColCount; ++f) {
											if (findex[f] >= 0) {
												r.setNormalFieldValue(f, sr.getNormalFieldValue(findex[f]));
											} else if (exps[f] != null) {
												r.setNormalFieldValue(f, sr.calc(exps[f], ctx));
											} else {
												if (gathers[f] != null) {
													if (isInsert) {
														r.setNormalFieldValue(f, gathers[f].getNextBySeq(-(mindex + 1)));
													} else {
														r.setNormalFieldValue(f, gathers[f].getNextBySeq(prevRecordSeq));
													}
												}
											}
										}
										mems.add(r);
									}
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
						}
						
						if (isInsert && matchs[i]) {
							matchCount--;
							Record r = new Record(ds);
							for (f = 0; f < filterCount; ++f) {
								temp.setNormalFieldValue(f, filterValues[f][i]);
								if (seqs[f] >= 0) {
									r.setNormalFieldValue(seqs[f], filterValues[f][i]);
								}
							}
							for (; f < colCount; ++f) {
								if (isField[f]) {
									Object obj = bufReaders[f].readObject();
									if (seqs[f] >= 0) {
										r.setNormalFieldValue(seqs[f], obj);
									}
									temp.setNormalFieldValue(f, obj);
								}
							}
							
							for (f = 0; f < retColCount; ++f) {
								if (exps[f] != null) {
									r.setNormalFieldValue(f, temp.calc(exps[f], ctx));
								} else {
									if (gathers[f] != null)
										r.setNormalFieldValue(f, gathers[f].getNextBySeq(prevRecordSeq));
								}
							}

							mems.add(r);
						} else if (matchCount > 0) {
							for (f = filterCount; f < colCount; ++f) {
								if (isField[f]) {
									bufReaders[f].skipObject();
								}
							}
						}
					}
					
					if (curBlock == endBlock && endBlock == table.getDataBlockCount()) {
						for (; mindex < mcount; ++mindex) {
							// 可能存在内存追加的记录在补区
							mr = modifyRecords.get(mindex);
							Record sr = mr.getRecord();
							if (Variant.isTrue(sr.calc(filter, ctx))) {
								Record r = new Record(ds);
								for (f = 0; f < retColCount; ++f) {
									if (findex[f] >= 0) {
										r.setNormalFieldValue(f, sr.getNormalFieldValue(findex[f]));
									} else if (exps[f] != null) {
										r.setNormalFieldValue(f, sr.calc(exps[f], ctx));
									} else {
										if (gathers[f] != null) {
											r.setNormalFieldValue(f, gathers[f].getNextBySeq(-(mindex + 1)));
										}
									}
								}
								
								mems.add(r);
							}
						}
					}
					
					int diff = n - cache.length();
					if (diff < 0) {
						this.cache = (Sequence) cache.split(n + 1);
						break;
					} else if (diff == 0) {
						break;
					}
				}
				
				if (table.getDataBlockCount() == 0 && startBlock == 0 && endBlock == 0) {
					// 表中还没有添加数据时有可能在补区有数据
					for (; mindex < mcount; ++mindex) {
						// 可能存在内存追加的记录在补区
						mr = modifyRecords.get(mindex);
						Record sr = mr.getRecord();
						if (Variant.isTrue(sr.calc(filter, ctx))) {
							Record r = new Record(ds);
							for (int f = 0; f < retColCount; ++f) {
								if (findex[f] >= 0) {
									r.setNormalFieldValue(f, sr.getNormalFieldValue(findex[f]));
								} else if (exps[f] != null) {
									r.setNormalFieldValue(f, sr.calc(exps[f], ctx));
								} else {
									if (gathers[f] != null) {
										r.setNormalFieldValue(f, gathers[f].getNextBySeq(-(mindex + 1)));
									}
								}
							}
							
							mems.add(r);
						}
					}
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
		} else if (isFirstSkip && n == MAXSKIPSIZE && filter == null && fkNames == null && !isSegment) {
			return table.getActualRecordCount();
		}
		
		boolean isFirstSkip = this.isFirstSkip;
		this.isFirstSkip = false;
		long count = 0;
		
		// 对单表跳过所有进行优化，不生成记录对象
		if (gathers == null && mcount < 1 && n == MAXSKIPSIZE) {
			if (cache != null) {
				count = cache.length();
			}
			
			int curBlock = this.curBlock;
			int endBlock = this.endBlock;
			BlockLinkReader rowCountReader = this.rowCountReader;
			IFilter []filters = this.filters;
			int colCount = colReaders.length;
			
			try {
				if (filters == null) {
					while (curBlock < endBlock) {
						curBlock++;
						count += rowCountReader.readInt32();
					}
				} else if (filters.length == 1) {
					ColumnMetaData column = columns[0];
					ObjectReader segmentReader = segmentReaders[0];
					IFilter filter = filters[0];
					
					while (curBlock < endBlock) {
						curBlock++;
						int recordCount = rowCountReader.readInt32();
						
						long position = segmentReader.readLong40();
						if (column.hasMaxMinValues()) {
							Object minValue = segmentReader.readObject();
							Object maxValue = segmentReader.readObject();
							segmentReader.skipObject();
							if (!filter.match(minValue, maxValue)) {
								continue;
							}
						}
						
						BufferReader filterReader = colReaders[0].readBlockData(position, recordCount);
						for (int i = 0; i < recordCount; ++i) {
							// 按记录数循环，如果列的BufferReader没有产生则产生并跳到当前要读的行
							Object val = filterReader.readObject();
							if (filter.match(val)) {
								count++;
							}
						}
					}
				} else {
					int filterCount = filters.length;
					BlockLinkReader []colReaders = this.colReaders;
					BufferReader []bufReaders = new BufferReader[filterCount];
					ColumnMetaData []columns = this.columns;
					ObjectReader []segmentReaders = this.segmentReaders;
					long []positions = new long[colCount];
					
					while (curBlock < endBlock) {
						curBlock++;
						int recordCount = rowCountReader.readInt32();
						boolean sign = true;
						int f = 0;
						for (; f < filterCount; ++f) {
							positions[f] = segmentReaders[f].readLong40();
							if (columns[f].hasMaxMinValues()) {
								Object minValue = segmentReaders[f].readObject();
								Object maxValue = segmentReaders[f].readObject();
								segmentReaders[f].skipObject();
								if (!filters[f].match(minValue, maxValue)) {
									++f;
									sign = false;
									break;
								}
							}
						}
						
						for (; f < colCount; ++f) {
							positions[f] = segmentReaders[f].readLong40();
							if (columns[f].hasMaxMinValues()) {
								segmentReaders[f].skipObject();
								segmentReaders[f].skipObject();
								segmentReaders[f].skipObject();
							}
						}
						
						if (!sign) {
							continue;
						}
						
						int []nextRows = new int[filterCount]; // 每个bufReaders下一条要读到的行，如果还没到当前要读的行则跳到
						for (f = 0; f < filterCount; ++f) {
							bufReaders[f] = null;
						}
						
						Next:
						for (int i = 0; i < recordCount; ++i) {
							// 按记录数循环，如果列的BufferReader没有产生则产生并跳到当前要读的行
							for (f = 0; f < filterCount; ++f) {
								if (bufReaders[f] == null) {
									bufReaders[f] = colReaders[f].readBlockData(positions[f], recordCount);
								}
								
								for (int j = nextRows[f]; j < i; ++j) {
									bufReaders[f].skipObject();
								}
								
								nextRows[f] = i + 1;
								if (!filters[f].match(bufReaders[f].readObject())) {
									continue Next;
								}
							}
							
							count++;
						}
					}
				}
			} catch (IOException e) {
				throw new RQException(e.getMessage(), e);
			}
			
			if (appendData != null) {
				count += appendData.length();
			}
			
			this.curBlock = curBlock;
			return count;
		} else if (filters == null && !hasModify() && isFirstSkip  && !isSegment && gathers == null && appendData == null) {
			//对没有过滤的情况优化
			
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
			BlockLinkReader rowCountReader = this.rowCountReader;
			BlockLinkReader []colReaders = this.colReaders;
			int colCount = colReaders.length;
			BufferReader []bufReaders = new BufferReader[colCount];
			ColumnMetaData []columns = this.columns;
			ObjectReader []segmentReaders = new ObjectReader[colCount];
			for (int i = 0; i < colCount; ++i) {
				segmentReaders[i] = columns[i].getSegmentReader();
			}
			
			long[] positions = new long[colCount];
			try {
				while (curBlock < endBlock) {
					curBlock++;
					int recordCount = rowCountReader.readInt32();
					for (int i = 0; i < colCount; i++) {
						positions[i] = segmentReaders[i].readLong40();
						if (columns[i].hasMaxMinValues()) {
							segmentReaders[i].skipObject();
							segmentReaders[i].skipObject();
							segmentReaders[i].skipObject();
						}
					}
					
					if (count + recordCount == n) {
						for (int f = 0; f < colCount; ++f) {
							colReaders[f].readBlockData(positions[f], recordCount);
						}
						this.curBlock = curBlock;
						return n;
					} else if (count + recordCount > n) {
						//cache = new Table(ds, recordCount);
						//IArray mems = cache.getMems();
						
						long diff = n - count;
						for (int f = 0; f < colCount; ++f) {
							bufReaders[f] = colReaders[f].readBlockData(positions[f], recordCount);
						}

						int i = 0;
						for (; i < diff; ++i) {
							for (int f = 0; f < colCount; ++f) {
								bufReaders[f].skipObject();
							}
							++prevRecordSeq;
						}
						
						Table tmp = new Table(ds, ICursor.FETCHCOUNT);
						this.cache = tmp;
						IArray mems = tmp.getMems();
						
						for (; i < recordCount; ++i) {
							ComTableRecord r = new ComTableRecord(ds);
							for (int f = 0; f < colCount; ++f) {
								r.setNormalFieldValue(f, bufReaders[f].readObject());
							}
							r.setRecordSeq(++prevRecordSeq);
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
		isClosed = true;
		cache = null;
		
		try {
			if (segmentReaders != null) {
				for (ObjectReader reader : segmentReaders) {
					if (reader != null) {
						reader.close();
					}
				}
			}
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			rowCountReader = null;
			colReaders = null;
			segmentReaders = null;
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
		exps = Operation.dupExpressions(expsBakup, ctx);
		if (appendData != null && appendData.length() > 0) {
			appendIndex = 1;
		}
		
		init();
		if (isSegment) {
			setSegment(startBlock, endBlock);
		}
		return true;
	}
	
	public void setSegment(boolean isSegment) {
		this.isSegment = isSegment;
	}

	public boolean isSegment() {
		return isSegment;
	}
	
	public boolean hasModify() {
		return mindex < mcount;
	}
	
	public void setCache(Sequence cache) {
		if (this.cache != null) {
			cache.addAll(this.cache);
			this.cache = (Sequence) cache;
		} else {
			this.cache = (Sequence) cache;	
		}
	}
	
	public Sequence getCache() {
		return cache;
	}

	public String[] getSortFields() {
		return sortedFields;
	}
	
	private void initExps(Expression exps[]) {
		if (exps != null) {
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
			if (cnt == colCount) {
				exps = null;
			}
		}
		
		if (exps != null) {
			this.exps = Operation.dupExpressions(exps, ctx);
			expsBakup = Operation.dupExpressions(exps, ctx);
		}
	}

	public int getCurBlock() {
		return curBlock;
	}

	public void setCurBlock(int curBlock) {
		this.curBlock = curBlock;
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

	public BlockLinkReader getRowCountReader() {
		return rowCountReader;
	}

	public void setRowCountReader(BlockLinkReader rowCountReader) {
		this.rowCountReader = rowCountReader;
	}

	public BlockLinkReader [] getColReaders() {
		return colReaders;
	}

	public void setColReaders(BlockLinkReader [] colReaders) {
		this.colReaders = colReaders;
	}

	public IFilter [] getFilters() {
		return filters;
	}

	public void setFilters(IFilter [] filters) {
		this.filters = filters;
	}

	public FindFilter [] getFindFilters() {
		return findFilters;
	}

	public void setFindFilters(FindFilter [] findFilters) {
		this.findFilters = findFilters;
	}

	public ColumnMetaData [] getColumns() {
		return columns;
	}

	public void setColumns(ColumnMetaData [] columns) {
		this.columns = columns;
	}

	public int [] getSeqs() {
		return seqs;
	}

	public void setSeqs(int [] seqs) {
		this.seqs = seqs;
	}

	public ObjectReader [] getSegmentReaders() {
		return segmentReaders;
	}

	public void setSegmentReaders(ObjectReader [] segmentReaders) {
		this.segmentReaders = segmentReaders;
	}

	public boolean isClosed() {
		return isClosed;
	}
	
	public int hashCode() {
		ArrayList<String> vals = new ArrayList<String>(10);
		File file = table.groupTable.getFile();
		if (file != null) {
			vals.add(file.getName());
		}
		
		for (ColumnMetaData col : columns) {
			vals.add(col.getColName());
		}
		String[] names = ds.getFieldNames();
		for (String name : names) {
			vals.add(name);
		}
		if (filter != null) {
			vals.add(filter.toString());
		}
		int count = vals.size();
		int hash = vals.get(0) != null ? vals.get(0).hashCode() : 0;
		for (int i = 1; i < count; ++i) {
			if (vals.get(i) != null) {
				hash = 31 * hash + vals.get(i).hashCode();
			} else {
				hash = 31 * hash;
			}
		}
		if (hash > 0) {
			return hash;
		} else {
			return -hash;
		}
	}
	
	public boolean canSkipBlock() {
		if (filters != null)
			return true;
		else
			return false;
	}
	
	public IArray[] getSkipBlockInfo(String key) {
		int curBlock = this.curBlock;
		int endBlock = this.endBlock;
		int initSize = endBlock - curBlock + 1;
		
		IFilter []filters = this.filters;
		int filterCount = filters.length;
		ColumnMetaData []columns = this.columns;
		int colCount = columns.length;
		
		int keyIndex = -1;
		for (int i = 0; i < colCount; i++) {
			if (columns[i].getColName().equals(key)) {
				keyIndex = i;
			}
		}
		
		ObjectArray minArray = new ObjectArray(initSize);
		ObjectArray maxArray = new ObjectArray(initSize);
		if (cache != null) {
			DataStruct tempDs = new DataStruct(fields);
			int idx = tempDs.getFieldIndex(key);
			Sequence cacheData = this.cache;
			int len = cacheData.length();
			minArray.add(((BaseRecord)cacheData.get(1)).getNormalFieldValue(idx));
			maxArray.add(((BaseRecord)cacheData.get(len)).getNormalFieldValue(idx));
		}
		
		//克隆分段reader
		ObjectReader[] segmentReaders = new ObjectReader[colCount];
		for (int i = 0; i < colCount; i++) {
			ObjectReader segmentReader = new ObjectReader(this.segmentReaders[i]);
			BlockLinkReader reader = (BlockLinkReader) segmentReader.getInputStream();
			segmentReader.setInputStream(new BlockLinkReader(reader));
			segmentReaders[i] = segmentReader;
		}
		
		boolean hasSkip = false;
		try {
			Object keyMinValue = null;
			Object keyMaxValue = null;
			while (curBlock < endBlock) {
				curBlock++;
				boolean sign = true;
				int f = 0;
				for (; f < filterCount; ++f) {
					segmentReaders[f].readLong40();
					if (columns[f].hasMaxMinValues()) {
						Object minValue = segmentReaders[f].readObject();
						Object maxValue = segmentReaders[f].readObject();
						segmentReaders[f].skipObject();
						if (f == keyIndex) {
							keyMinValue = minValue;
							keyMaxValue = maxValue;
						}
						if (!filters[f].match(minValue, maxValue)) {
							++f;
							sign = false;
							break;
						}
					}
				}
				
				for (; f < colCount; ++f) {
					if (f == keyIndex) {
						segmentReaders[f].readLong40();
						if (columns[f].hasMaxMinValues()) {
							keyMinValue = segmentReaders[f].readObject();
							keyMaxValue = segmentReaders[f].readObject();
							segmentReaders[f].skipObject();
						}
					} else {
						segmentReaders[f].readLong40();
						if (columns[f].hasMaxMinValues()) {
							segmentReaders[f].skipObject();
							segmentReaders[f].skipObject();
							segmentReaders[f].skipObject();
						}
					}
				}
				
				if (sign) {
					//没跳段
					minArray.add(keyMinValue);
					maxArray.add(keyMaxValue);
				} else {
					hasSkip = true;
				}
			}
			
			for (int i = 0; i < colCount; i++) {
				segmentReaders[i].close();
			}
			
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		}
		
		if (!hasSkip) {
			//如果没有跳段，则没有意义
			return null;
		}
		
		if (appendData != null) {
			DataStruct tempDs = new DataStruct(fields);
			int idx = tempDs.getFieldIndex(key);
			Sequence appendData = this.appendData;
			int len = appendData.length();
			minArray.add(((BaseRecord)appendData.get(1)).getNormalFieldValue(idx));
			maxArray.add(((BaseRecord)appendData.get(len)).getNormalFieldValue(idx));
		}
		
		if (minArray.size() == 0) {
			//如果跳过了所有段，则没有意义
			return null;
		}
		
		IArray[] result = new IArray[] {minArray, maxArray};
		return result;
	}
	
	/**
	 * 将游标设置为按照key条块 （pjoin时使用）
	 * 设置后，游标会按照values里的值进行跳块。
	 * @param key 维字段名
	 * @param values [minValue, maxValue] 
	 */
	public void setSkipBlockInfo(String key, IArray[] values) {
		if (values == null || key == null) {
			return;
		}
		int idx = ds.getFieldIndex(key);
		if (idx < 0) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(key + mm.getMessage("ds.fieldNotExist"));
		}
		
		ColumnMetaData column = table.getColumn(key); 
		IFilter filter = new BlockFilter(column, values);

		if (filters == null) {
			filters = new IFilter[] { filter };
			
			//把维列提到最前面
			ColumnMetaData []columns = this.columns;
			ColumnMetaData tempCol = columns[0];
			columns[0] = columns[idx];
			columns[idx] = tempCol;
			
			BlockLinkReader[] columnReaders = this.colReaders;
			BlockLinkReader temp = columnReaders[0];
			columnReaders[0] = columnReaders[idx];
			columnReaders[idx] = temp;

			int colCount = columnReaders.length;
			ObjectReader []segmentReaders = new ObjectReader[colCount];
			for (int i = 0; i < colCount; ++i) {
				if (columns[i] != null) {
					segmentReaders[i] = columns[i].getSegmentReader();
				}
			}
			seqs = new int [colCount];
			DataStruct ds = new DataStruct(fields);
			for (int i = 0; i < colCount; ++i) {
				ColumnMetaData col = columns[i];
				if (col != null) {
					seqs[i] = ds.getFieldIndex(col.getColName());
				} else {
					seqs[i] = -1;
				}
			}
			
			int startBlock = this.startBlock;
			long prevRecordSeq = 0;
			BlockLinkReader rowCountReader = this.rowCountReader;
			
			try {
				for (int i = 0; i < startBlock; ++i) {
					prevRecordSeq += rowCountReader.readInt32();
					for (int f = 0; f < colCount; ++f) {
						if (segmentReaders[f] != null) {
							segmentReaders[f].readLong40();
							if (columns[f].hasMaxMinValues()) {
								segmentReaders[f].skipObject();
								segmentReaders[f].skipObject();
								segmentReaders[f].skipObject();
							}
						}
					}
				}
				this.segmentReaders = segmentReaders;
				this.prevRecordSeq = prevRecordSeq;
			} catch (IOException e) {
				throw new RQException(e.getMessage(), e);
			}
		} else {
			int size = filters.length;
			
			//如果可以合并到已有filter
			for (int i = 0; i < size; i++) {
				IFilter f = filters[i];
				if (f.isSameColumn(filter)) {
					filters[i] = new LogicAnd(f, filter);
					return;
				}
			}
			
			//找到目前的位置
			ColumnMetaData []columns = this.columns;
			BlockLinkReader []columnReaders = this.colReaders;
			int len = columnReaders.length;
			String colName = key;
			for (int i = 0; i < len; i++) {
				if (columns[i].getColName().equals(colName)) {
					idx = i;
					break;
				}
			}
			
			//插入到filters中
			IFilter[] filters = this.filters;
			FindFilter[] findFilters = this.findFilters;
			IFilter[] newFilters = new IFilter[size + 1];
			FindFilter[] newFindFilters = null;
			if (findFilters != null) {
				newFindFilters = new FindFilter[size + 1];
			}
			
			for (int i = 0; i < size; i++) {
				if (newFindFilters != null) {
					newFindFilters[i + 1] = findFilters[i];
				}
				newFilters[i + 1] = filters[i];				
			}
			newFilters[0] = filter;
			
			this.filters = newFilters;
			this.findFilters = newFindFilters;
			
			//调整列的位置
			BlockLinkReader temp = columnReaders[idx];
			System.arraycopy(columnReaders, 0, columnReaders, 1, idx);
			columnReaders[0] = temp;
			
			ColumnMetaData tempCol = columns[idx];
			System.arraycopy(columns, 0, columns, 1, idx);
			columns[0] = tempCol;
			
			ObjectReader []segmentReaders = this.segmentReaders;
			ObjectReader tempReader = segmentReaders[idx];
			System.arraycopy(segmentReaders, 0, segmentReaders, 1, idx);
			segmentReaders[0] = tempReader;
			
			DataStruct ds = new DataStruct(fields);
			int colCount = columns.length;
			int[] seqs = new int [colCount];
			for (int i = 0; i < colCount; ++i) {
				seqs[i] = ds.getFieldIndex(columns[i].getColName());
			}
			this.seqs = seqs;
		}
	}
	
	protected Sequence getStartBlockData(int n) {
		// 只取第一块的记录，如果第一块没有满足条件的就返回
		if (startBlock >= endBlock) {
			Sequence result = appendData;
			appendData = null;
			appendIndex = 0;
			return result;
		}
		
		int startBlock = this.startBlock;
		int endBlock = this.endBlock;
		try {
			setEndBlock(startBlock + 1);
			
			Sequence seq;
			if (gathers == null) {
				seq = getData(n);
			} else {
				seq = getData2(n);
			}
			isFirstSkip = false;
			
			return seq;
		} finally {
			setEndBlock(endBlock);
		}
	}
	
	public TableGather[] getGathers() {
		return gathers;
	}
}