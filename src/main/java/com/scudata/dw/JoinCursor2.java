package com.scudata.dw;

import java.util.ArrayList;

import com.scudata.array.IArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.cursor.MultipathCursors;
import com.scudata.dm.op.Switch;
import com.scudata.expression.Expression;
import com.scudata.expression.FieldRef;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.Moves;
import com.scudata.expression.Node;
import com.scudata.expression.ParamInfo2;
import com.scudata.expression.UnknownSymbol;
import com.scudata.expression.mfn.sequence.Avg;
import com.scudata.expression.mfn.sequence.Count;
import com.scudata.expression.mfn.sequence.Max;
import com.scudata.expression.mfn.sequence.Min;
import com.scudata.expression.mfn.sequence.New;
import com.scudata.expression.mfn.sequence.Sum;
import com.scudata.parallel.ClusterPhyTable;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 用于T.new T.derive T.news的结果的游标 (T是行存、有补区、附表时)
 * @author runqian
 *
 */
public class JoinCursor2 extends ICursor {
	private boolean isClosed;
	private boolean isNew;
	private boolean isNews;
	private DataStruct ds;
	
	private ICursor cursor1;//T的游标
	private Sequence cache1;
	private ICursor cursor2;//A/cs
	//private String[] csNames;//A/cs:K的K，用于指定A/cs参与连接的字段
	private Sequence cache2;

	private int cur1 = -1;
	private int cur2 = -1;
	
	private int keyCount;
	private int csFieldsCount;//A/cs的字段个数
	private int []keyIndex2;//A/cs的主键下标
	private int []fieldIndex2;//A/cs取出字段的下标
	
	private int []fieldIndex1;//T取出字段的下标
	
	private boolean hasExps;//
	private Node nodes[];
	
	private boolean hasR;
	
	/**
	 * 
	 * @param table
	 * @param exps
	 * @param fields
	 * @param cursor2
	 * @param opt	0:derive; 1:new; 2:news
	 * @param ctx
	 */
	public JoinCursor2(Object table, Expression []exps, String []fields, ICursor cursor2, String[] csNames, Expression filter,
			String []fkNames, Sequence []codes, String[] opts, int opt, String option, Context ctx) {
		this.isNew = opt == 1;
		this.isNews = opt == 2;
		this.ctx = ctx;
		
		if (ctx != null) {
			ctx.addResource(this);
		}
		
		hasR = option != null && option.indexOf("r") != -1;
		
		String []keyNames;
		if (table instanceof IPhyTable) {
			keyNames = ((IPhyTable) table).getAllSortedColNames();
		} else {
			keyNames = ((ClusterPhyTable) table).getAllSortedColNames();
		}
		
		this.cursor2 = cursor2;
		//this.csNames = csNames;
		Sequence seq = cursor2.peek(1);
		if (seq == null) {
			isClosed = true;
			return;
		}
		DataStruct ds2 = ((Record) seq.get(1)).dataStruct();
		
		String []joinNames = keyNames;//join字段，默认取T的主键
		if (isNew) {
			//new时就是T的主键
			if (joinNames == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("ds.lessKey"));
			}
			keyCount = joinNames.length;
			keyIndex2 = new int[keyCount];
			if (csNames == null) {
				for (int i = 0; i < keyCount; i++) {
					keyIndex2[i] = i;
				}
			} else {
				if (csNames.length > keyCount) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("ds.lessKey"));
				}
				for (int i = 0; i < keyCount; i++) {
					keyIndex2[i] = ds2.getFieldIndex(csNames[i]);
				}
			}
		} else {
			//news时取cs的主键
			
			//1.得到cs主键的下标
			if (csNames == null) {
				keyIndex2 = ds2.getPKIndex();
			} else {
				int csNamesLen = csNames.length;
				keyIndex2 = new int[csNamesLen];
				for (int i = 0; i < csNamesLen; i++) {
					keyIndex2[i] = ds2.getFieldIndex(csNames[i]);
				}
			}
			if (keyIndex2 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("ds.lessKey"));
			}
			keyCount = keyIndex2.length;
			
			//2.取T前面的字段
			joinNames = new String[keyCount];//此时不是T的主键
			String[] allNames;
			if (table instanceof IPhyTable) {
				allNames = ((IPhyTable) table).getAllColNames();
			} else {
				allNames = ((ClusterPhyTable) table).getAllColNames();
			}
			for (int i = 0; i < keyCount; i++) {
				joinNames[i] = allNames[i];
			}
		}
		
		ArrayList<String> keyList = new ArrayList<String>();//临时使用
		for (int i = 0; i < keyCount; i++) {
			keyList.add(keyNames[i]);
		}
		
		//处理表达式，exps里可能有{……}
		ArrayList<Expression> fetchExps = new ArrayList<Expression>();
		for (int i = 0, len = exps.length; i < len; i++) {
			Expression exp = exps[i];
			if (exp == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("Expression.missingParam"));
			}
			
			if (fields[i] == null) {
				fields[i] = exps[i].getFieldName();
			}
			
			Node home = exp.getHome();
			if (home instanceof UnknownSymbol) {
				String f = exp.getFieldName();
				if (!keyList.contains(f) && ds2.getFieldIndex(f) < 0)
						fetchExps.add(exp);
			} else {
				hasExps = true;
				if (home instanceof Moves) {
					IParam fieldParam = ((Moves) exp.getHome()).getParam();
					ParamInfo2 pi = ParamInfo2.parse(fieldParam, "cursor", false, false);
					String []subFields = pi.getExpressionStrs1();
					for (String f : subFields) {
						if (!keyList.contains(f))
								fetchExps.add(new Expression(f));
					}
				} else if (home instanceof com.scudata.expression.fn.gather.Top) {
					IParam fieldParam = ((com.scudata.expression.fn.gather.Top) exp.getHome()).getParam();
					if (fieldParam != null) {
						if (!fieldParam.isLeaf()) {
							IParam sub1 = fieldParam.getSub(1);
							if (!keyList.contains(sub1.getLeafExpression().getFieldName()))
								fetchExps.add(sub1.getLeafExpression());
						}
					}
				} else {
					String field = ((Function)home).getParamString();
					if (!keyList.contains(field) && ds2.getFieldIndex(field) < 0)
						fetchExps.add(new Expression(field));
				}
			}
		}
		
		Expression []allExps;
		allExps = new Expression[keyCount + fetchExps.size()];
		int i = 0;
		for (; i < keyCount; i++) {
			allExps[i] = new Expression(keyNames[i]);
		}
		for (Expression exp : fetchExps) {
			allExps[i++] = exp;
		}
		
		if (hasExps) {
			int len = exps.length;
			nodes = new Node[len];
			for (i = 0; i < len; i++) {
				nodes[i] = parseNode(exps[i], ctx);
			}
		}
		
		if (table instanceof IPhyTable) {
			this.cursor1 = ((IPhyTable) table).cursor(allExps, null, filter, null, null, null, null, ctx);
		}
		
		if (isNew || isNews) {
			ds = new DataStruct(fields);
		} else {
			csFieldsCount = ds2.getFieldCount();
			String[] fieldNames = new String[csFieldsCount + fields.length];
			System.arraycopy(ds2.getFieldNames(), 0, fieldNames, 0, csFieldsCount);
			System.arraycopy(fields, 0, fieldNames, csFieldsCount, fields.length);
			ds = new DataStruct(fieldNames);
		}
		
		int len = exps.length;
		fieldIndex1 = new int[len];
		fieldIndex2 = new int[len];
		DataStruct ds1 = cursor1.getDataStruct();
		for (i = 0; i < len; i++) {
			fieldIndex1[i] = ds1.getFieldIndex(exps[i].getIdentifierName());
			fieldIndex2[i] = ds2.getFieldIndex(exps[i].getIdentifierName());
		}
		
		init();
		
		if (fkNames != null) {
			int fkCount = fkNames.length;
			for (int j = 0; j < fkCount; j++) {
				String[] fkn = new String[] {fkNames[j]};
				Sequence[] code = new Sequence[] {codes[j]};
				String _opt = null;
				Expression _exps[] = null;
				if (opts[j] != null && opts[j].indexOf("#") != -1) {
					_exps = new Expression[] {new Expression("#")};
				} else if (opts[j] != null && opts[j].indexOf("null") != -1) {
					_opt = "d";
				} else {
					_opt = "i";
				}
				Switch op = new Switch(fkn, code, _exps, _opt);
				addOperation(op, ctx);
			}
		}
	}
	
	public JoinCursor2(ICursor cursor1, ICursor cursor2, Context ctx) {
		this.cursor1 = cursor1;
		this.cursor2 = cursor2;
		this.ctx = ctx;
	}
	
	void init() {
		cache1 = cursor1.fetch(FETCHCOUNT);
		if (cache1 == null || cache1.length() == 0) {
			isClosed = true;
			return;
		}
		cur1 = 1;
	}
	
	public static MultipathCursors makeMultiJoinCursor(Object table, Expression []exps, String []fields, MultipathCursors cursor2, 
			String[] csNames, Expression filter, String []fkNames, Sequence []codes, String[] opts, int opt, String option, Context ctx) {
		boolean isNew = opt == 1;
		boolean isNews = opt == 2;
		boolean hasExps = false;
		int csFieldsCount = 0;
		DataStruct ds;
		int []fieldIndex1 = null;
		
		String []keyNames;
		if (table instanceof IPhyTable) {
			keyNames = ((IPhyTable) table).getAllSortedColNames();
		} else {
			keyNames = ((ClusterPhyTable) table).getAllSortedColNames();
		}
		
		DataStruct ds2 = (cursor2.getCursors()[0]).getDataStruct();
		int []keyIndex2;
		int keyCount;
		String []joinNames = keyNames;//join字段，默认取T的主键
		if (isNew) {
			//new时就是T的主键
			if (joinNames == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("ds.lessKey"));
			}
			keyCount = joinNames.length;
			keyIndex2 = new int[keyCount];
			if (csNames == null) {
				for (int i = 0; i < keyCount; i++) {
					keyIndex2[i] = i;
				}
			} else {
				if (csNames.length > keyCount) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("ds.lessKey"));
				}
				for (int i = 0; i < keyCount; i++) {
					keyIndex2[i] = ds2.getFieldIndex(csNames[i]);
				}
			}
		} else {
			//news时取cs的主键
			
			//1.得到cs主键的下标
			if (csNames == null) {
				keyIndex2 = ds2.getPKIndex();
			} else {
				int csNamesLen = csNames.length;
				keyIndex2 = new int[csNamesLen];
				for (int i = 0; i < csNamesLen; i++) {
					keyIndex2[i] = ds2.getFieldIndex(csNames[i]);
				}
			}
			if (keyIndex2 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("ds.lessKey"));
			}
			keyCount = keyIndex2.length;
			
			//2.取T前面的字段
			joinNames = new String[keyCount];//此时不是T的主键
			String[] allNames;
			if (table instanceof IPhyTable) {
				allNames = ((IPhyTable) table).getAllColNames();
			} else {
				allNames = ((ClusterPhyTable) table).getAllColNames();
			}
			for (int i = 0; i < keyCount; i++) {
				joinNames[i] = allNames[i];
			}
		}
		
		ArrayList<String> keyList = new ArrayList<String>();//临时使用
		for (int i = 0; i < keyCount; i++) {
			keyList.add(keyNames[i]);
		}
		
		//处理表达式，exps里可能有{……}
		ArrayList<Expression> fetchExps = new ArrayList<Expression>();
		for (int i = 0, len = exps.length; i < len; i++) {
			Expression exp = exps[i];
			if (exp == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("Expression.missingParam"));
			}
			
			if (fields[i] == null) {
				fields[i] = exps[i].getFieldName();
			}
			
			Node home = exp.getHome();
			if (home instanceof UnknownSymbol) {
				if (!keyList.contains(exp.getFieldName()))
						fetchExps.add(exp);
			} else {
				hasExps = true;
				isNews = false;//有表达式时不能用news
				if (home instanceof Moves) {
					IParam fieldParam = ((Moves) exp.getHome()).getParam();
					ParamInfo2 pi = ParamInfo2.parse(fieldParam, "cursor", false, false);
					String []subFields = pi.getExpressionStrs1();

					for (String f : subFields) {
						if (!keyList.contains(f))
								fetchExps.add(new Expression(f));
					}
				} else if (home instanceof com.scudata.expression.fn.gather.Top) {
					IParam fieldParam = ((com.scudata.expression.fn.gather.Top) exp.getHome()).getParam();
					if (fieldParam != null) {
						if (!fieldParam.isLeaf()) {
							IParam sub1 = fieldParam.getSub(1);
							if (!keyList.contains(sub1.getLeafExpression().getFieldName()))
								fetchExps.add(sub1.getLeafExpression());
						}
					}
				} else {
					String field = ((Function)home).getParamString();
					if (!keyList.contains(field))
						fetchExps.add(new Expression(field));
				}
			}
		}
		
		Expression []allExps;
		allExps = new Expression[keyCount + fetchExps.size()];
		int i = 0;
		for (; i < keyCount; i++) {
			allExps[i] = new Expression(keyNames[i]);
		}
		for (Expression exp : fetchExps) {
			allExps[i++] = exp;
		}
		
		Node nodes[] = null; 
		if (hasExps) {
			int len = exps.length;
			nodes = new Node[len];
			for (i = 0; i < len; i++) {
				nodes[i] = parseNode(exps[i], ctx);
			}
		}
		
		String[] allExpNames = new String[allExps.length];
		for (i = 0; i < allExpNames.length; i++) {
			allExpNames[i] = allExps[i].toString();
		}
		
		ICursor cursor1 = null;
		if (table instanceof IPhyTable) {
			Expression w = null;
			if (filter != null) {
				w = filter.newExpression(ctx); // 分段并行读取时需要复制表达式，同一个表达式不支持并行运算
			}
			cursor1 = ((IPhyTable) table).cursor(null, allExpNames, w, null, null, null, cursor2, null, ctx);
		}

		if (isNew || isNews) {
			ds = new DataStruct(fields);
		} else {
			csFieldsCount = ds2.getFieldCount();
			String[] fieldNames = new String[csFieldsCount + fields.length];
			System.arraycopy(ds2.getFieldNames(), 0, fieldNames, 0, csFieldsCount);
			System.arraycopy(fields, 0, fieldNames, csFieldsCount, fields.length);
			ds = new DataStruct(fieldNames);
		}
		
		if (!hasExps) {
			int len = exps.length;
			fieldIndex1 = new int[len];
			for (i = 0; i < len; i++) {
				fieldIndex1[i] = cursor1.getDataStruct().getFieldIndex(fields[i]);
			}
		}
		
		int len = cursor2.getPathCount();
		ICursor cursors1[] = ((MultipathCursors) cursor1).getParallelCursors();
		ICursor cursors2[] = cursor2.getParallelCursors();
		ICursor cursors[] = new ICursor[len];
		boolean hasR = option != null && option.indexOf("r") != -1;
		for (i = 0; i < len; i++) {
			JoinCursor2 cs = new JoinCursor2(cursors1[i], cursors2[i], ctx);
			cs.ds = ds;
			cs.isNew = opt == 1;
			cs.isNews = opt == 2;
			cs.hasExps = hasExps;
			cs.csFieldsCount = csFieldsCount;
			cs.fieldIndex1 = fieldIndex1;
			cs.keyIndex2 = keyIndex2;
			cs.keyCount = keyCount;
			cs.nodes = nodes;
			cs.hasR = hasR;
			cs.init();
			if (fkNames != null) {
				int fkCount = fkNames.length;
				for (int j = 0; j < fkCount; j++) {
					String[] fkn = new String[] {fkNames[j]};
					Sequence[] code = new Sequence[] {codes[j]};
					String _opt = null;
					Expression _exps[] = null;
					if (opts[j] != null && opts[j].indexOf("#") != -1) {
						_exps = new Expression[] {new Expression("#")};
					} else if (opts[j] != null && opts[j].indexOf("null") != -1) {
						_opt = "d";
					} else {
						_opt = "i";
					}
					Switch op = new Switch(fkn, code, _exps, _opt);
					cs.addOperation(op, ctx);
				}
				
			}
			cursors[i] = cs;
		}
		return new MultipathCursors(cursors, ctx);
	}
	
	protected Sequence get(int n) {
		if (isClosed || n < 1) {
			return null;
		}

		if (isNews) {
			return getDataForNews(n);
		}
		
		if (hasExps) {
			return getDataForNew(n);
		}
		
		int keyCount = this.keyCount;
		int csFieldsCount = this.csFieldsCount;
		int len = isNew ? ds.getFieldCount() : ds.getFieldCount() - csFieldsCount;
		
		if (cache2 == null || cache2.length() == 0) {
			cache2 = cursor2.fetch(n);
			cur2 = 1;
		}
		
		int cur1 = this.cur1;
		int cur2 = this.cur2;
		Sequence cache1 = this.cache1;
		Sequence cache2 = this.cache2;
		IArray mems1 = cache1.getMems();
		IArray mems2 = cache2.getMems();
		int len1 = cache1.length();
		int len2 = cache2.length();
		int []fieldIndex1 = this.fieldIndex1;
		int []fieldIndex2 = this.fieldIndex2;
		int []keyIndex2 = this.keyIndex2;
		boolean isNew = this.isNew;
		boolean isNews = this.isNews;
		ICursor cursor1 = this.cursor1;
		ICursor cursor2 = this.cursor2;
		
		Table newTable;
		if (n > INITSIZE) {
			newTable = new Table(ds, INITSIZE);
		} else {
			newTable = new Table(ds, n);
		}
		
		Object []keys2 = new Object[keyCount];
		while (true) {
			Record record1 = (Record) mems1.get(cur1);
			Record record2 = (Record) mems2.get(cur2);
			
			for (int i = 0; i < keyCount; i++) {
				keys2[i] = record2.getFieldValue(keyIndex2[i]);
			}
			Object []keys1 = record1.getFieldValues();
			
			int cmp = Variant.compareArrays(keys2, keys1);
			if (cmp == 0) {
				cur1++;
				if (!isNews) {
					cur2++;
				}
				BaseRecord record = newTable.newLast();
				Object [] objs2 = record2.getFieldValues();
				if (isNew || isNews) {
					for (int i = 0; i < len; i++) {
						int idx = fieldIndex1[i];
						if (idx >= 0) {
							record.setNormalFieldValue(i, keys1[idx]);
						}
						else {
							idx = fieldIndex2[i];
							record.setNormalFieldValue(i, objs2[idx]);
						}
					}
				} else {
					Object []vals = record2.getFieldValues();
					System.arraycopy(vals, 0, record.getFieldValues(), 0, csFieldsCount);
					for (int i = 0; i < len; i++) {
						record.setNormalFieldValue(i + csFieldsCount, keys1[fieldIndex1[i]]);
					}
				}
			} else if (cmp > 0) {
				cur1++;
			} else if (cmp < 0) {
				cur2++;
			}
			
			if (cur1 > len1) {
				cur1 = 1;
				cache1 = cursor1.fetch(FETCHCOUNT);
				if (cache1 == null || cache1.length() == 0) {
					isClosed = true;
					break;
				}
				mems1 = cache1.getMems();
				len1 = cache1.length();
			}
			if (cur2 > len2) {
				cur2 = 1;
				cache2 = cursor2.fetch(n - newTable.length());
				if (cache2 == null || cache2.length() == 0) {
					isClosed = true;
					break;
				}
				mems2 = cache2.getMems();
				len2 = cache2.length();
			}
			
			if (newTable.length() == n) {
				break;
			}
		}
		
		this.cache1 = cache1;
		this.cache2 = cache2;
		this.cur1 = cur1;
		this.cur2 = cur2;
		
		if (newTable.length() > 0) {
			return newTable;
		} else {
			return null;
		}
	}

	protected long skipOver(long n) {
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
		return count;
	}
	
	
	public void close() {
		super.close();
		isClosed = true;
		cache1 = null;
		cache2 = null;
		cursor1.close();
		cursor2.close();
	}
	
	public boolean reset() {
		close();
		if (!cursor1.reset() || !cursor2.reset()) {
			return false;
		} else {
			isClosed = false;
			cur1 = -1;
			cur2 = -1;
			return true;
		}
	}
	
	/**
	 * 有表达式时取数据
	 * @param n
	 * @return
	 */
	protected Sequence getData(int n) {
		if (isClosed || n < 1) {
			return null;
		}

		Node nodes[] = this.nodes;
		int keyCount = this.keyCount;
		int csFieldsCount = this.csFieldsCount;
		int len = isNew ? ds.getFieldCount() : ds.getFieldCount() - csFieldsCount;
		
		if (cache2 == null || cache2.length() == 0) {
			cache2 = cursor2.fetch(n);
			cur2 = 1;
		}

		int cur2 = this.cur2;
		Sequence cache2 = this.cache2;
		
		Table newTable;
		if (n > INITSIZE) {
			newTable = new Table(ds, INITSIZE);
		} else {
			newTable = new Table(ds, n);
		}
		
		int keysIndex[] = new int[keyCount];
		for (int i = 0; i < keyCount; i++) {
			keysIndex[i] = i;
		}
		
		while (true) {
			Sequence seq1 = cursor1.fetchGroup(keysIndex);
			if (seq1 == null || seq1.length() == 0) {
				isClosed = true;
				break;
			}
			
			Record record1 = (Record) seq1.get(1);
			Record record2 = (Record) cache2.get(cur2);
			Object []keys2 = new Object[keyCount];
			for (int i = 0; i < keyCount; i++) {
				keys2[i] = record2.getFieldValue(keyIndex2[i]);
			}
			Object []keys1 = record1.getFieldValues();
			
			int cmp = Variant.compareArrays(keys2, keys1);
			if (cmp == 0) {
				cur2++;
				BaseRecord record = newTable.newLast();
				if (isNew) {
					for (int i = 0; i < len; i++) {
						//record.setNormalFieldValue(i, keys1[i + keyCount]);
						Node node = nodes[i];
						if (node instanceof FieldRef) {
							node.setDotLeftObject(seq1.get(1));
						} else {
							node.setDotLeftObject(seq1);
						}
						record.setNormalFieldValue(i, node.calculate(ctx));
					}
				} else {
					Object []vals = record2.getFieldValues();
					System.arraycopy(vals, 0, record.getFieldValues(), 0, csFieldsCount);
					for (int i = 0; i < len; i++) {
						Node node = nodes[i];
						if (node instanceof FieldRef) {
							node.setDotLeftObject(seq1.get(1));
						} else {
							node.setDotLeftObject(seq1);
						}
						record.setNormalFieldValue(i + csFieldsCount, node.calculate(ctx));
					}
				}
			} else if (cmp > 0) {
			} else if (cmp < 0) {
				cur2++;
			}
			
			if (cur2 > cache2.length()) {
				cur2 = 1;
				cache2 = cursor2.fetch(n - newTable.length());
				if (cache2 == null || cache2.length() == 0) {
					isClosed = true;
					break;
				}
			}
			
			if (newTable.length() == n) {
				break;
			}
		}
		
		this.cache2 = cache2;
		this.cur2 = cur2;
		
		if (newTable.length() > 0) {
			return newTable;
		} else {
			return null;
		}
	}
	
	//有表达式
	private Sequence getDataForNew(int n) {
		if (isClosed || n < 1) {
			return null;
		}

		int keyCount = this.keyCount;
		int len = ds.getFieldCount();
		
		Object []keys2 = new Object[keyCount];
		
		//取出来一段cs的数据
		if (cache2 == null || cache2.length() == 0) {
			cache2 = cursor2.fetch(ICursor.FETCHCOUNT);
			cur2 = 1;
			BaseRecord record2 = (BaseRecord) cache2.get(1);
			for (int i = 0; i < keyCount; i++) {
				keys2[i] = record2.getFieldValue(keyIndex2[i]);
			}
		}
		
		int cur1 = this.cur1;
		Sequence cache1 = this.cache1;
		int len1 = this.cache1.length();
		IArray mems1 = cache1.getMems();
		
		int cur2 = this.cur2;
		Sequence cache2 = this.cache2;
		IArray mems2 = cache2.getMems();
		int len2 = cache2.length();
		int []keyIndex2 = this.keyIndex2;
		ICursor cursor2 = this.cursor2;
		
		Table newTable;
		if (n > INITSIZE) {
			newTable = new Table(ds, INITSIZE);
		} else {
			newTable = new Table(ds, n);
		}

		Table tempTable = new Table(cache2.dataStruct());//用于汇总
		
		while (true) {
			BaseRecord record1 = (BaseRecord) mems1.get(cur1);
			BaseRecord record2 = (BaseRecord) mems2.get(cur2);
			for (int i = 0; i < keyCount; i++) {
				keys2[i] = record2.getFieldValue(keyIndex2[i]);
			}
			Object []keys1 = record1.getFieldValues();
			
			int cmp = Variant.compareArrays(keys2, keys1);
			if (cmp == 0) {
				//把这一条加入临时汇总table
				tempTable.add(record2);
				
				cur2++;
				if (cur2 > len2) {
					cur2 = 1;
					cache2 = cursor2.fetch(ICursor.FETCHCOUNT);
					if (cache2 == null || cache2.length() == 0) {
						isClosed = true;
						break;
					}
					mems2 = cache2.getMems();
					len2 = cache2.length();
				}
				record2 = (BaseRecord) mems2.get(cur2);
				for (int i = 0; i < keyCount; i++) {
					keys2[i] = record2.getFieldValue(keyIndex2[i]);
				}
				
				if (0 != Variant.compareArrays(keys2, keys1)) {
					//如果不相等，表示这一组取完了，计算临时汇总数据
					BaseRecord record = newTable.newLast();
					calcExpsForNew(record, tempTable, record1, len);
					
					//取出下一条符合条件的
					cur1++;
					if (cur1 > len1) {
						cur1 = 1;
						cache1 = cursor1.fetch(FETCHCOUNT);
						if (cache1 == null || cache1.length() == 0) {
							isClosed = true;
							break;
						}
						mems1 = cache1.getMems();
						len1 = cache1.length();
					}
				}
			} else if (cmp > 0) {
				//取出下一条符合条件的
				cur1++;
				if (cur1 > len1) {
					cur1 = 1;
					cache1 = cursor1.fetch(FETCHCOUNT);
					if (cache1 == null || cache1.length() == 0) {
						isClosed = true;
						break;
					}
					mems1 = cache1.getMems();
					len1 = cache1.length();
				}
			} else if (cmp < 0) {
				cur2++;
				if (cur2 > len2) {
					cur2 = 1;
					cache2 = cursor2.fetch(ICursor.FETCHCOUNT);
					if (cache2 == null || cache2.length() == 0) {
						isClosed = true;
						break;
					}
					mems2 = cache2.getMems();
					len2 = cache2.length();
				}
				record2 = (BaseRecord) mems2.get(cur2);
				for (int i = 0; i < keyCount; i++) {
					keys2[i] = record2.getFieldValue(keyIndex2[i]);
				}
			}
			
			if (newTable.length() == n) {
				break;
			}
		}
		
		BaseRecord record1 = (BaseRecord) mems1.get(cur1);
		if (isClosed && tempTable != null && tempTable.length() != 0) {
			//如果不相等，表示这一组取完了，计算临时汇总数据
			BaseRecord record = newTable.newLast();
			calcExpsForNew(record, tempTable, record1, len);
		}
		
		this.cache1 = cache1;
		this.cache2 = cache2;
		this.cur1 = cur1;
		this.cur2 = cur2;
		
		if (newTable.length() > 0) {
			return newTable;
		} else {
			return null;
		}
	}

	/**
	 * T.news的取数
	 * @param n
	 * @return
	 */
	private Sequence getDataForNews(int n) {
		if (isClosed || n < 1) {
			return null;
		}

		int keyCount = this.keyCount;
		int len = ds.getFieldCount();
		
		
		Object []keys2 = new Object[keyCount];
		//Object []keys1 = this.keys1;
		
		if (cache2 == null || cache2.length() == 0) {
			cache2 = cursor2.fetch(ICursor.FETCHCOUNT);
			cur2 = 1;
			BaseRecord record2 = (BaseRecord) cache2.get(1);
			for (int i = 0; i < keyCount; i++) {
				keys2[i] = record2.getFieldValue(keyIndex2[i]);
			}
		}
		
		int cur1 = this.cur1;
		Sequence cache1 = this.cache1;
		int len1 = this.cache1.length();
		IArray mems1 = cache1.getMems();
		
		int cur2 = this.cur2;
		Sequence cache2 = this.cache2;
		IArray mems2 = cache2.getMems();
		int len2 = cache2.length();
		int []fieldIndex1 = this.fieldIndex1;
		int []fieldIndex2 = this.fieldIndex2;
		int []keyIndex2 = this.keyIndex2;
		boolean hasR = this.hasR;
		boolean hasExps = this.hasExps;
		ICursor cursor2 = this.cursor2;
		
		Table newTable;
		if (n > INITSIZE) {
			newTable = new Table(ds, INITSIZE);
		} else {
			newTable = new Table(ds, n);
		}
		Table tempTable = new Table(cache1.dataStruct());//用于汇总
		BaseRecord record1 = (BaseRecord) mems1.get(cur1);
		BaseRecord record2 = (BaseRecord) mems2.get(cur2);
		for (int i = 0; i < keyCount; i++) {
			keys2[i] = record2.getFieldValue(keyIndex2[i]);
		}
		Object []keys1 = record1.getFieldValues();
		
		while (true) {
			int cmp = Variant.compareArrays(keys2, keys1);
			if (cmp == 0) {
				BaseRecord record = newTable.newLast();
				if (hasExps) {
					tempTable.newLast(keys1);//添加到临时汇总
				} else {
					for (int i = 0; i < len; i++) {
						int idx = fieldIndex1[i];
						if (idx < 0)
							record.setNormalFieldValue(i, record2.getFieldValue(fieldIndex2[i]));
						else 
							record.setNormalFieldValue(i, keys1[idx]);
					}
				}
				
				cur1++;
				if (cur1 > len1) {
					cur1 = 1;
					cache1 = cursor1.fetch(FETCHCOUNT);
					if (cache1 == null || cache1.length() == 0) {
						isClosed = true;
						break;
					}
					mems1 = cache1.getMems();
					len1 = cache1.length();
				}
				record1 = (BaseRecord) mems1.get(cur1);
				keys1 = record1.getFieldValues();
				
				if (hasR) {
					if (hasExps) {
						//把这一组取完
						while(Variant.compareArrays(keys2, keys1) == 0) {
							record1 = (BaseRecord) mems1.get(cur1);
							keys1 = record1.getFieldValues();
							tempTable.newLast(keys1);//添加到临时汇总
							cur1++;
							if (cur1 > len1) {
								cur1 = 1;
								cache1 = cursor1.fetch(FETCHCOUNT);
								if (cache1 == null || cache1.length() == 0) {
									isClosed = true;
									break;
								}
								mems1 = cache1.getMems();
								len1 = cache1.length();
							}
							record1 = (BaseRecord) mems1.get(cur1);
							keys1 = record1.getFieldValues();
						}
						
						calcExpsForNews(record, tempTable, record2, len);
					}
					
					//按照cs对齐
					cur2++;
					if (cur2 > len2) {
						cur2 = 1;
						cache2 = cursor2.fetch(ICursor.FETCHCOUNT);
						if (cache2 == null || cache2.length() == 0) {
							isClosed = true;
							break;
						}
						mems2 = cache2.getMems();
						len2 = cache2.length();
					}
					record2 = (BaseRecord) mems2.get(cur2);
					for (int i = 0; i < keyCount; i++) {
						keys2[i] = record2.getFieldValue(keyIndex2[i]);
					}
				}
			} else if (cmp > 0) {
				cur1++;
				if (cur1 > len1) {
					cur1 = 1;
					cache1 = cursor1.fetch(FETCHCOUNT);
					if (cache1 == null || cache1.length() == 0) {
						isClosed = true;
						break;
					}
					mems1 = cache1.getMems();
					len1 = cache1.length();
				}
				record1 = (BaseRecord) mems1.get(cur1);
				keys1 = record1.getFieldValues();
			} else if (cmp < 0) {
				cur2++;
				if (cur2 > len2) {
					cur2 = 1;
					cache2 = cursor2.fetch(ICursor.FETCHCOUNT);
					if (cache2 == null || cache2.length() == 0) {
						isClosed = true;
						break;
					}
					mems2 = cache2.getMems();
					len2 = cache2.length();
				}
				record2 = (BaseRecord) mems2.get(cur2);
				for (int i = 0; i < keyCount; i++) {
					keys2[i] = record2.getFieldValue(keyIndex2[i]);
				}
			}
			
			if (newTable.length() >= n) {
				break;
			}
		}
		
		this.cache1 = cache1;
		this.cache2 = cache2;
		this.cur1 = cur1;
		this.cur2 = cur2;
		
		if (newTable.length() > 0) {
			return newTable;
		} else {
			return null;
		}
	}
	
	/**
	 * 基于两个记录（以及同组数据）计算表达式
	 * @param record
	 * @param tempTable 主键相同的一组数据
	 * @param r
	 * @param len
	 */
	private void calcExpsForNew(BaseRecord record, Table tempTable, BaseRecord r, int len) {
		Node nodes[] = this.nodes;
		int fieldIndex2[] = this.fieldIndex2;
		for (int i = 0; i < len; i++) {
			Node node = nodes[i];
			if (node instanceof FieldRef) {
				if (fieldIndex2[i] < 0) {
					node.setDotLeftObject(r);
				} else {
					node.setDotLeftObject(tempTable.get(1));
				}
			} else {
				node.setDotLeftObject(tempTable);
			}
			record.setNormalFieldValue(i, node.calculate(ctx));
		}
	
		tempTable.clear();
	}
	
	private void calcExpsForNews(BaseRecord record, Table tempTable, BaseRecord r, int len) {
		Node nodes[] = this.nodes;
		for (int i = 0; i < len; i++) {
			Node node = nodes[i];
			if (node instanceof FieldRef) {
				node.setDotLeftObject(r);
			} else {
				node.setDotLeftObject(tempTable);
			}
			record.setNormalFieldValue(i, node.calculate(ctx));
		}
	
		tempTable.clear();
	}
	
	private static Node parseNode(Expression exp, Context ctx) {
		Node home = exp.getHome();
		Node node = null;
		
		if (home instanceof Moves) {
			node = new New();
			((Function) node).setParameter(null, ctx, ((Function)exp.getHome()).getParamString());
		} else if (home instanceof UnknownSymbol) {
			node = new FieldRef(exp.getFieldName());
		} else if (home instanceof Function) {
			String fname = ((Function)home).getFunctionName();
			if (fname.equals("sum")) {
				node = new Sum();
				((Function) node).setParameter(null, ctx, ((Function)exp.getHome()).getParamString());
			} else if (fname.equals("count")) {
				node = new Count();
				((Function) node).setParameter(null, ctx, ((Function)exp.getHome()).getParamString());
			} else if (fname.equals("min")) {
				node = new Min();
				((Function) node).setParameter(null, ctx, ((Function)exp.getHome()).getParamString());
			} else if (fname.equals("max")) {
				node = new Max();
				((Function) node).setParameter(null, ctx, ((Function)exp.getHome()).getParamString());
			} else if (fname.equals("avg")) {
				node = new Avg();
				((Function) node).setParameter(null, ctx, ((Function)exp.getHome()).getParamString());
			} else if (fname.equals("top")) {
				node = new com.scudata.expression.mfn.sequence.Top();
				((Function) node).setParameter(null, ctx, ((Function)exp.getHome()).getParamString());
			}
		}
		return node;
	}
}