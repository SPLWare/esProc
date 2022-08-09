package com.scudata.dw;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.ListBase1;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.op.New;
import com.scudata.dm.op.Select;
import com.scudata.dm.op.Switch;
import com.scudata.expression.Expression;
import com.scudata.expression.Moves;
import com.scudata.expression.Node;
import com.scudata.expression.UnknownSymbol;
import com.scudata.expression.operator.And;
import com.scudata.expression.operator.DotOperator;
import com.scudata.resources.EngineMessage;

/**
 * 附表游标类
 * 附表取数时可能会用到基表的字段
 * @author runqian
 *
 */
public class JoinTableCursor extends IDWCursor{
	ColumnTableMetaData table;//本表
	private int seqs[][];//每个表里取出字段的位置
	
	private TableCursor tableCursor[];//取数游标（包含基表，附表的）

	private RecordValSearcher []recSearchers;//查找基表里匹配的记录
	private DataStruct ds;
	private String []sortedFields;//有序字段
	
	private int startBlock; // 包含
	private int endBlock; // 不包含
	private int curBlock = 0;
	private Sequence cache;
	
	// 同步分段时需要补上下一段第一块里属于本段的部分
	private Sequence appendData;
	private int appendIndex = 1;
	
	private boolean isClosed = false;
	private boolean isSegment = false;
	private boolean isFirstSkip = true;//是否是第一次Skip
	private boolean hasFilter = false;
	
	private Expression []exps;//表达式字段
	private Record calcRec; 
	private int seqs2[][];//需要取出来进行表达式计算的字段
	
	public JoinTableCursor(DataStruct ds) {
		this.ds = ds;
		setDataStruct(ds);
	}

	public void setTableCursor(TableCursor[] tableCursor) {
		this.tableCursor = tableCursor;
		RecordValSearcher []recSearchers = new RecordValSearcher[tableCursor.length];
		this.recSearchers = recSearchers;
		for (int i = 0, len = recSearchers.length; i < len; ++i) {
			recSearchers[i] = new RecordValSearcher();
			recSearchers[i].setIndex(seqs[i]);
			recSearchers[i].setModifyRecords(tableCursor[i].getModifyRecords());
		}
	}
	
	public void setSeqs(int[][] seqs) {
		this.seqs = seqs;
	}

	/**
	 * 返回一个附表游标
	 * @param table
	 * @return
	 */
	public static ICursor createAnnexCursor(ColumnTableMetaData table) {
		return parseFilterAndFields(null, table, null, null, null, null, null, null);
	}
	
	/**
	 * 返回附表游标
	 * @param table 附表
	 * @param fields 取出字段
	 * @return
	 */
	public static ICursor createAnnexCursor(ColumnTableMetaData table, String []fields) {
		return parseFilterAndFields(fields, table, null, null, null, null, null, null);
	}
	
	/**
	 * 附表游标
	 * @param table 附表
	 * @param fields 取出字段
	 * @param exp	过滤表达式
	 * @param fkNames switch过滤字段名
	 * @param codes switch过滤的序列
	 * @param ctx 上下文
	 * @return
	 */
	public static ICursor createAnnexCursor(ColumnTableMetaData table, String []fields, 
			Expression exp, String []fkNames, Sequence []codes, Context ctx) {
		return parseFilterAndFields(null, table, exp, fkNames, codes, null, null, ctx);
	}
	
	/**
	 * 
	 * @param table 附表
	 * @param exps 取出表达式
	 * @param names 取出别名
	 * @param exp	过滤表达式
	 * @param fkNames switch过滤字段名
	 * @param codes switch过滤的序列
	 * @param ctx 上下文
	 * @return
	 */
	public static ICursor createAnnexCursor(ColumnTableMetaData table, Expression []exps, String []names, 
			Expression exp, String []fkNames, Sequence []codes, Context ctx) {
		return parseFilterAndFields(null, table, exp, fkNames, codes, exps, names, ctx);
	}
	
	private static ICursor parseFilterAndFields(String []fields, ColumnTableMetaData table, Expression exp, 
			String []fkNames, Sequence []codes, Expression []exps, String []names, Context ctx) {
		ColumnTableMetaData ptable = (ColumnTableMetaData) table.parent;
		if (ptable == null) {
			return null;
		}
		
		try {
			ptable.appendCache();
			table.appendCache();
		} catch (IOException e) {
			throw new RQException(e);
		}
		
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
			if (cnt == colCount) {
				exps = null;
			}
		}
		
		ArrayList<String> fieldsList = new ArrayList<String>();
		ArrayList<IFilter> filterList = new ArrayList<IFilter>();
		String tableFields[];
		IFilter tableFilter[];
		
		ArrayList<ColumnTableMetaData> tableList = new ArrayList<ColumnTableMetaData>(2);
		tableList.add(ptable);
		tableList.add(table);
		int count = 2;
		String [][]fieldArrays = new String[count][];
		IFilter [][]filterArrays = new IFilter[count][];
		IFilter [][]findFilterArrays = new IFilter[count][];
		New newOp = null;
		
		//提取表达式的filter
		if (exp != null) {
			for (ColumnTableMetaData t : tableList) {
				Object obj = Cursor.parseFilter(t, exp, ctx);
				
				if (obj instanceof IFilter) {
					filterList.add((IFilter)obj);
					fieldsList.add(((IFilter)obj).getColumn().getColName());
				} else if (obj instanceof ArrayList) {
					ArrayList<Object> list = (ArrayList<Object>)obj;
					Node node = null;
					
					for (Object f : list) {
						if (f instanceof IFilter) {
							filterList.add((IFilter)f);
							fieldsList.add(((IFilter)f).getColumn().getColName());
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
				}
			}
			Collections.sort(filterList);
		}
		
		//处理K:T
		ArrayList<FindFilter> findFilterList = null;
		boolean hasModify = table.getModifyRecords() != null || ptable.getModifyRecords() != null;
		if (!hasModify && fkNames != null) {
			int fcount = fkNames.length;
			findFilterList = new ArrayList<FindFilter>();
			for (int i = 0, len = filterList.size(); i < len; i++) {
				findFilterList.add(null);
			}
			
			int fltCount = filterList.size();
			
			Next:
			for (int f = 0; f < fcount; ++f) {
				ColumnTableMetaData tempTable = table;
				ColumnMetaData column = tempTable.getColumn(fkNames[f]);
				if (column == null) {
					tempTable = ptable;
					column = tempTable.getColumn(fkNames[f]);
				}
				
				int pri = tempTable.getColumnFilterPriority(column);
				FindFilter find = new FindFilter(column, pri, codes[f]);
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
		}
	
		// 检查表达式里是否引用了没有选出的字段，如果引用了则加入到选出字段里
		if (exp != null) {
			ArrayList<String> nameList = new ArrayList<String>();
			exp.getUsedFields(ctx, nameList);
			if (nameList.size() > 0 && fields != null) {
				ArrayList<String> selectList = new ArrayList<String>();
				for (String name : fields) {
					selectList.add(name);
				}
				
				for (String name : nameList) {
					if (!selectList.contains(name) && 
							(ptable.getColumn(name) != null || table.getColumn(name) != null)) {
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
					
					newOp = new New(newExps, newNames, null);
					fields = newFields;
				}
			}
		}
		
		//如果选出字段为null时，选出默认
		if (fields == null) {
			String []pColNames = ptable.getSortedColNames();
			String []colNames = table.getColNames();
			//所有字段  + 主表所有字段
			ArrayList<String> list = new ArrayList<String>();
			for (String name : pColNames) {
				list.add(name);
			}
			for (String name : colNames) {
				if (!list.contains(name)) {
					list.add(name);
				}
			}
			int size = list.size();
			fields = new String[size];
			list.toArray(fields);	
		}
		
		//处理字段表达式
		DataStruct expsDs = null;
		ArrayList<String> expsList = null;
		if (exps != null) {
			expsList = table.getExpFields(exps);
			if (expsList != null) {
				String []expsFieldNames = new String[expsList.size()];
				expsList.toArray(expsFieldNames);
				expsDs = new DataStruct(expsFieldNames);
				
				for (String f : expsFieldNames) {
					if (!fieldsList.contains(f)) {
						fieldsList.add(f);
					}
				}
				
				for (int i = 0, size = exps.length; i < size; i++) {
					if (exps[i] != null) {
						if (exps[i].getHome() instanceof DotOperator) {
							exps[i] = null;
						} else if (exps[i].getHome() instanceof UnknownSymbol) {
							exps[i] = null;
						} else if (exps[i].getHome() instanceof Moves) {
							Node left = exps[i].getHome().getLeft();
							if (!(left instanceof UnknownSymbol)) {
								exps[i] = null;
							}
						}
					}
				}
			}
		}
		
		//检查选出字段是否都存在
		for (String f : fields) {
			if (f == null) continue;
			if (-1 == table.getDataStruct().getFieldIndex(f))
			{
				if (-1 == ptable.getDataStruct().getFieldIndex(f)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(f + mm.getMessage("ds.fieldNotExist"));
				}
			}
			if (!fieldsList.contains(f)) {
				fieldsList.add(f);
			}
		}
		
		//接下来把fieldsList和filterList拆分到各个表
		int tableCount = 0;
		int []tableId = new int[count];
		boolean hasFilter = false;
		
		for (int i = 0; i < count; ++i) {
			TableMetaData tb = tableList.get(i);
			String []colNames = tb.getColNames();//这里只能要子表自己的字段
			ArrayList<String> tempFieldsList = new ArrayList<String>();
			ArrayList<IFilter> tempFilterList = new ArrayList<IFilter>();
			ArrayList<IFilter> tempFindFilterList = new ArrayList<IFilter>();
			
			//check fieldsList
			for (String name : colNames) {
				if (fieldsList.contains(name)) {
					tempFieldsList.add(name);
					fieldsList.remove(name);
				}
				for (int j = 0, len = filterList.size(); j < len; j++) {
					IFilter f = filterList.get(j);
					if (f != null && f.getColumn().getColName().equals(name)) {
						tempFilterList.add(f);
						if (findFilterList != null) {
							tempFindFilterList.add(findFilterList.get(j));
						}
						filterList.set(j, null);
						break;
					}
				}
			}
			
			int size = tempFieldsList.size();
			if (size > 0) {
				//如果这个表里有字段要取
				tableFields = new String[size];
				tempFieldsList.toArray(tableFields);
				fieldArrays[tableCount] = tableFields;
				tableId[tableCount++] = i;
			}
			
			size = tempFilterList.size();
			if (size > 0) {
				//如果这个表里有filter要过滤
				tableFilter = new IFilter[size];
				tempFilterList.toArray(tableFilter);
				filterArrays[i] = tableFilter;
				hasFilter = true;
			}
			
			size = tempFindFilterList.size();
			if (size > 0) {
				//如果这个表里有filter要过滤
				tableFilter = new IFilter[size];
				tempFindFilterList.toArray(tableFilter);
				findFilterArrays[i] = tableFilter;
			}
			
			tempFieldsList.clear();
			tempFilterList.clear();
			tempFindFilterList.clear();
		}
		
		if (tableCount == 1) {
			if (tableId[0] != 0) {
				//要取的数据在且仅在子表，返回单表Cursor
				return null;
			} else {
				//要取的数据都在基表，仍要跟子表同步
				tableCount++;
			}
		}
		
		TableCursor tableCursor[] = new TableCursor[tableCount];
		DataStruct ds = new DataStruct(fields);
		JoinTableCursor cs = new JoinTableCursor(ds);
		
		int seqs[][] = new int[tableCount][];
		int seqs2[][] = new int[tableCount][];
		int c = 0;
		
		//如果要改为多表继承，需要修改这里!!
		for (int i = 0; i < tableCount; ++i) {
			String []fieldArray = fieldArrays[i];
			if (fieldArray != null) {
				tableCursor[c] = new TableCursor((ColumnTableMetaData) tableList.get(tableId[i]), fieldArray, filterArrays[i], ctx);
				int len = fieldArray.length;
				seqs[c] = new int[len];
				
				for (int j = 0; j < len; ++j) {
					seqs[c][j] = ds.getFieldIndex(fieldArray[j]);
				}
				if (exps != null) {
					seqs2[c] = new int[len];
					for (int j = 0; j < len; ++j) {
						seqs2[c][j] = expsDs.getFieldIndex(fieldArray[j]);
					}
				}
				c++;
			} else {
				//如果对子表取数时，字段都在主表，则为子表添加一个取数字段，便于取数
				fieldArray = table.getColNames();
				int size = fieldArray.length;
				fieldArray = new String[]{fieldArray[size - 1]};
				tableCursor[c] = new TableCursor(table, fieldArray, filterArrays[i], ctx);
				seqs[c] = null;
				seqs2[c] = null;
			}
		}
		
		//设置seqs和tableCursor[]给jtc
		//node不等于null,也要给jtc
		cs.endBlock = tableList.get(0).getDataBlockCount();
		cs.setSeqs(seqs);
		cs.setTableCursor(tableCursor);
		if (exp != null) {
			Select select = new Select(exp, null);
			cs.addOperation(select, ctx);
			
			//当条件里的字段被改名时
			if (names != null) {
				int nameLen = names.length;
				Expression []newExps = new Expression[nameLen];
				for (int i = 1; i <= nameLen; ++i) {
					newExps[i - 1] = new Expression("#" + i);
				}
				newOp = new New(newExps, names, null);
				names = null;
			}
		}
		
		//有补区时不在游标里处理K:T
		if (!hasModify && fkNames != null) {
			Switch op = new Switch(fkNames, codes, null, "i");
			cs.addOperation(op, ctx);
		}
		
		cs.hasFilter = hasFilter;
		cs.table = table;
		cs.ctx = ctx;
		if (exps != null) {
			cs.exps = exps;
			cs.seqs2 = seqs2;
			cs.calcRec = new Record(expsDs);
		}
		
		if (names != null) {
			int len = names.length;
			for (int i = 0; i < len; i++) {
				if (names[i] == null) {
					names[i] = ds.getFieldName(i);
				}
			}
			cs.ds = new DataStruct(names);
			cs.setDataStruct(cs.ds);
		}
		
		ds = cs.ds;
		if (table.hasPrimaryKey()) {
			// 如果附表有主键并且主键被选出则给数据结构设上主键
			String []keys = table.getAllSortedColNames();
			ArrayList<String> pkeyList = new ArrayList<String>();
			ArrayList<String> sortedFieldList = new ArrayList<String>();
			DataStruct temp = new DataStruct(fields);
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
				cs.sortedFields = new String[size];
				sortedFieldList.toArray(cs.sortedFields);
			}
		}
		
		if (newOp != null) 
			cs.addOperation(newOp, ctx);
		return cs;
	}
	
	public ColumnTableMetaData getColumnTableMetaData() {
		return table;
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
	 * 设置分段信息
	 */
	public void setSegment(int startBlock, int endBlock) {
		isSegment = true;
		this.startBlock = startBlock;
		this.curBlock = startBlock;
		this.endBlock = endBlock;
		
		if (startBlock == 0 || startBlock >= endBlock) {
			return;
		}
		
		for(TableCursor cursor : tableCursor) {
			cursor.setSegment(startBlock, endBlock);;
		}
	}

	/**
	 * 同步分段时需要补上下一段第一块里属于本段的部分
	 */
	public void setAppendData(Sequence seq) {
		this.appendData = seq;
	}
	
	protected Sequence get(int n) {
		// 修改了同步分段，同步分段时如果分段点是在块中某条记录，则使用appendData存放最后一块需要添加的记录
		isFirstSkip = false;
		Sequence seq = getData(n);
		if (appendData == null) {
			return seq;
		} else {
			if (seq == null) {
				if (appendIndex < 1) {
					return seq;
				}
				
				DataStruct ds = this.ds;
				Sequence appendData = this.appendData;
				int len = appendData.length();
				if (n > len - appendIndex + 1) {
					Table table = new Table(ds, len - appendIndex + 1);
					ListBase1 mems = table.getMems();
					for (int i = appendIndex; i <= len; ++i) {
						Record r = (Record)appendData.getMem(i);
						r.setDataStruct(ds);
						mems.add(r);
					}
					
					appendIndex = 0;
					return table;
				} else {
					Table table = new Table(ds, n);
					ListBase1 mems = table.getMems();
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
				if (diff > rest) {
					ListBase1 mems = seq.getMems();
					for (int i = appendIndex; i <= len; ++i) {
						Record r = (Record)appendData.getMem(i);
						r.setDataStruct(ds);
						mems.add(r);
					}
					
					appendIndex = 0;
					return seq;
				} else {
					ListBase1 mems = seq.getMems();
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
		
		DataStruct ds = this.ds;
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
		ListBase1 mems = cache.getMems();
		this.cache = null;
		TableCursor tableCursor[] = this.tableCursor;
		int tableCount = tableCursor.length;
		RecordValSearcher []recSearchers = this.recSearchers;
		
		Expression []exps = this.exps;
		Context ctx = this.ctx;
		int expCount = 0;
		Record calcRec = this.calcRec;
		if (exps != null) {
			expCount = exps.length;
		}
		
		while (curBlock < endBlock) {
			curBlock++;

			//遍历所有表TableCursor,每次取一块
			boolean skip = false;
			for (int c = 0; c < tableCount; ++c) {
				if (skip) {
					tableCursor[c].skipOver(MAXSIZE);
				} else {
					Sequence data = tableCursor[c].get(MAXSIZE);
					if (data == null) {
						skip = true;
					} else {
						recSearchers[c].setData(data);
					}
				}
			}
			if (skip) {
				continue;
			}
			//recordCount是该块能够取出的记录数,也就是最后一个表里读出来的记录数
			RecordValSearcher primarySearcher = recSearchers[0];
			RecordValSearcher subSearcher = recSearchers[tableCount - 1];
			int recordCount = subSearcher.getRecordCount();
			boolean hasFind;

			if (!hasFilter) {
				for (int i = 0; i < recordCount; ++i) {
					Record r = new Record(ds);
					long recNum = subSearcher.getRecNum();
					if (recNum > 0) {
						primarySearcher.getKeyValues(recNum, r);
					} else {
						primarySearcher.getMKeyValues(recNum, r, 0);
					}
					subSearcher.getRecordValue(r);
					if (exps != null) {
						if (recNum > 0) {
							for (int c = 0; c < tableCount; c++) {
								if(seqs2[c] != null) {
									for (int j = 0, len = seqs2[c].length; j < len; j++) {
										int index = seqs2[c][j];
										if (index < 0) continue;
										calcRec.setNormalFieldValue(index, recSearchers[c].getRecordValue(j));
									}
								}
							}
							for (int c = 0; c < expCount; c++) {
								if (exps[c] != null) {
									r.setNormalFieldValue(c, calcRec.calc(exps[c], ctx));
								}
							}
						} else {
							if(seqs2[0] != null) {
								for (int j = 0, len = seqs2[0].length; j < len; j++) {
									int index = seqs2[0][j];
									if (index < 0) continue;
									calcRec.setNormalFieldValue(index, primarySearcher.getModifyRecordValue(j));
								}
							}
							if(seqs2[1] != null) {
								for (int j = 0, len = seqs2[1].length; j < len; j++) {
									int index = seqs2[1][j];
									if (index < 0) continue;
									calcRec.setNormalFieldValue(index, subSearcher.getRecordValue(j));
								}
							}
							for (int c = 0; c < expCount; c++) {
								if (exps[c] != null) {
									r.setNormalFieldValue(c, calcRec.calc(exps[c], ctx));
								}
							}
						}
					}
					subSearcher.next();
					mems.add(r);
				}
			} else {
				//有Filter时，有可能会在主表里没有对应的
				for (int i = 0; i < recordCount; ++i) {
					Record r = new Record(ds);
					long recNum = subSearcher.getRecNum();
					hasFind = true;
					
					if (recNum > 0) {
						hasFind = primarySearcher.getKeyVals(recNum, r);
					} else {
						hasFind = true;
						primarySearcher.getMKeyValues(recNum, r, 0);
					}
					if (hasFind) {
						subSearcher.getRecordValue(r);
					}
					
					if (exps != null) {
						if (recNum > 0) {
							for (int c = 0; c < tableCount; c++) {
								if(seqs2[c] != null) {
									for (int j = 0, len = seqs2[c].length; j < len; j++) {
										int index = seqs2[c][j];
										if (index < 0) continue;
										calcRec.setNormalFieldValue(index, recSearchers[c].getRecordValue(j));
									}
								}
							}
							for (int c = 0; c < expCount; c++) {
								if (exps[c] != null) {
									r.setNormalFieldValue(c, calcRec.calc(exps[c], ctx));
								}
							}
						} else {
							if(seqs2[0] != null) {
								for (int j = 0, len = seqs2[0].length; j < len; j++) {
									int index = seqs2[0][j];
									if (index < 0) continue;
									calcRec.setNormalFieldValue(index, primarySearcher.getModifyRecordValue(j));
								}
							}
							if(seqs2[1] != null) {
								for (int j = 0, len = seqs2[1].length; j < len; j++) {
									int index = seqs2[1][j];
									if (index < 0) continue;
									calcRec.setNormalFieldValue(index, subSearcher.getRecordValue(j));
								}
							}
							for (int c = 0; c < expCount; c++) {
								if (exps[c] != null) {
									r.setNormalFieldValue(c, calcRec.calc(exps[c], ctx));
								}
							}
						}
					}
					
					subSearcher.next();
					if (hasFind) {
						mems.add(r);
					}
				}
			}
			
			int diff = n - cache.length();
			if (diff < 0) {
				this.cache = cache.split(n + 1);
				break;
			} else if (diff == 0) {
				break;
			}

		}

		this.curBlock = curBlock;
		if (cache.length() > 0) {
			return cache;
		} else {
			return null;
		}
		
	}

	protected long skipOver(long n) {
		if (isClosed) {
			return 0;
		} else if (isFirstSkip && n == MAXSKIPSIZE && !hasFilter && !isSegment) {
			return table.getActualRecordCount();
		}
		
		Sequence data;
		long rest = n;
		long count = 0;
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

		isFirstSkip = false;
		return count;
	}
	
	public void close() {
		super.close();
		isClosed = true;
		cache = null;
		
		try {
			if (tableCursor != null) {
				for (TableCursor tcs : tableCursor) {
					tcs.close();
				}
			}
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		}
	}
	
	public boolean reset() {
		super.close();
		cache = null;
		
		isClosed = false;
		curBlock = 0;
		int endBlock = this.endBlock;
		appendIndex = 1;
		isFirstSkip = true;
		
		for (TableCursor tcs : tableCursor) {
			tcs.reset();
		}
		
		if (isSegment) {
			setSegment(startBlock, endBlock);
		}
		return true;
	}
	
	public TableMetaData getTableMetaData() {
		return table;
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
}