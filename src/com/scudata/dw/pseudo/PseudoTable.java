package com.scudata.dw.pseudo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ConjxCursor;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.cursor.MemoryCursor;
import com.scudata.dm.cursor.MergeCursor;
import com.scudata.dm.cursor.MultipathCursors;
import com.scudata.dm.op.Conj;
import com.scudata.dm.op.Group;
import com.scudata.dm.op.Join;
import com.scudata.dm.op.New;
import com.scudata.dm.op.Operable;
import com.scudata.dm.op.Operation;
import com.scudata.dm.op.Switch;
import com.scudata.dw.ITableMetaData;
import com.scudata.expression.Constant;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.expression.ParamParser;
import com.scudata.expression.UnknownSymbol;
import com.scudata.expression.mfn.sequence.Contain;
import com.scudata.expression.operator.And;
import com.scudata.expression.operator.DotOperator;
import com.scudata.expression.operator.Equals;
import com.scudata.expression.operator.NotEquals;
import com.scudata.expression.operator.Or;
import com.scudata.util.CursorUtil;

public class PseudoTable extends Pseudo {	
	//创建游标需要的参数
	private String []fkNames;
	private Sequence []codes;
	private int pathCount;
	
	private ArrayList<Operation> extraOpList = new ArrayList<Operation>();//其它情况产生的延迟计算（不是主动调用select添加）

	private PseudoTable mcsTable;
	
	protected boolean hasPseudoColumns = false;//是否需要根据伪字段转换（枚举、二值）表达式
	
	public PseudoTable() {
	}
	
	/**
	 * 产生虚表对象
	 * @param rec 定义记录
	 * @param hs 分机序列
	 * @param n 并行数
	 * @param ctx
	 */
	public PseudoTable(Record rec, int n, Context ctx) {
		pd = new PseudoDefination(rec, ctx);
		pathCount = n;
		this.ctx = ctx;
		extraNameList = new ArrayList<String>();
		init();
	}

	public PseudoTable(Record rec, PseudoTable mcs, Context ctx) {
		this(rec, 0, ctx);
		mcsTable = mcs;
	}
	
	private void init() {
		if (getPd() != null) {
			allNameList = new ArrayList<String>();
			String []names = getPd().getAllColNames();
			for (String name : names) {
				allNameList.add(name);
			}
			
			if (getPd().getColumns() != null) {
				List<PseudoColumn> columns = getPd().getColumns();
				for (PseudoColumn column : columns) {
					//如果存在枚举伪字段和二值伪字段，要记录下来，在接下来的处理中会用到
					if (column.getPseudo() != null && 
							(column.getBits() != null || column.get_enum() != null)) {
						hasPseudoColumns = true;
					}
					if (column.getDim() != null) {
						addColName(column.getName());
					}
				}
			}
		}
	}

	public void addPKeyNames() {
		addColNames(getPd().getAllSortedColNames());
	}
	
	public void addColNames(String []nameArray) {
		for (String name : nameArray) {
			addColName(name);
		}
	}
	
	public void addColName(String name) {
		if (name == null) return; 
		if (allNameList.contains(name) && !extraNameList.contains(name)) {
			extraNameList.add(name);
		}
	}
	
	/**
	 * 设置取出字段
	 * @param exps 取出表达式
	 * @param fields 取出别名
	 */
	private void setFetchInfo(Expression []exps, String []fields) {
		this.exps = null;
		this.names = null;
		boolean needNew = extraNameList.size() > 0;
		Expression newExps[] = null;
		
		extraOpList.clear();
		
		//set FK codes info
		if (fkNameList != null) {
			int size = fkNameList.size();
			fkNames = new String[size];
			fkNameList.toArray(fkNames);
			
			codes = new Sequence[size];
			codeList.toArray(codes);
		}
		
		if (exps == null) {
			if (fields == null) {
				return;
			} else {
				this.names = getFetchColNames(fields);//有取出字段
			}
		} else {
			
			newExps = exps.clone();//备份一下
			
			/**
			 * 有取出表达式也有取出字段,则检查extraNameList里是否包含exps里的字段
			 * 如果包含就去掉
			 */
			ArrayList<String> tempList = new ArrayList<String>();
			for (String name : extraNameList) {
				if (!tempList.contains(name)) {
					tempList.add(name);
				}
			}
			for (Expression exp : exps) {
				String expName = exp.getIdentifierName();
				if (tempList.contains(expName)) {
					tempList.remove(expName);
				}
			}
			
			ArrayList<String> tempNameList = new ArrayList<String>();
			ArrayList<Expression> tempExpList = new ArrayList<Expression>();
			int size = exps.length;
			for (int i = 0; i < size; i++) {
				Expression exp = exps[i];
				String name = fields[i];
				Node node = exp.getHome();
				if (node instanceof UnknownSymbol) {
					/**
					 * 先检查一下取出表达式是否枚举字段，如果是则做个转换
					 */
					String expName = exp.getIdentifierName();
					if (!allNameList.contains(expName)) {
						PseudoColumn col = pd.findColumnByPseudoName(expName);
						if (col.get_enum() != null) {
							String var = "pseudo_enum_value_" + i;
							ctx.setParamValue(var, col.get_enum());
							name = col.getName();
							newExps[i] = new Expression(var + "(" + name + "+1)");
							exp = new Expression(name);
							needNew = true;
						}
					}
					
					tempExpList.add(exp);
					tempNameList.add(name);
					
//				} else if (node instanceof DotOperator) {
//					Node left = node.getLeft();
//					if (left != null && left instanceof UnknownSymbol) {
//						PseudoColumn col = getPd().findColumnByName( ((UnknownSymbol)left).getName());
//						if (col != null) {
//							Derive derive = new Derive(new Expression[] {exp}, new String[] {name}, null);
//							extraOpList.add(derive);
//						}
//					}
				} else {
				}
			}
			
			for (String name : tempList) {
				tempExpList.add(new Expression(name));
				tempNameList.add(name);
			}
			
			size = tempExpList.size();
			this.exps = new Expression[size];
			tempExpList.toArray(this.exps);
			
			this.names = new String[size];
			tempNameList.toArray(this.names);
		}
		
		if (needNew) {
			New _new = new New(newExps, fields, null);
			extraOpList.add(_new);
		}
		return;
	}
	
	private String[] getFetchColNames(String []fields) {
		ArrayList<String> tempList = new ArrayList<String>();
		if (fields != null) {
			for (String name : fields) {
				tempList.add(name);
			}
		}
		for (String name : extraNameList) {
			if (!tempList.contains(name)) {
				tempList.add(name);
			}
		}
		
		int size = tempList.size();
		if (size == 0) {
			return null;
		}
		String []newFields = new String[size];
		tempList.toArray(newFields);
		return newFields;
	}
	
	/**
	 * 得到虚表的每个实体表的游标构成的数组
	 * @return
	 */
	public ICursor[] getCursors() {
		List<ITableMetaData> tables = getPd().getTables();
		int size = tables.size();
		ICursor cursors[] = new ICursor[size];
		
		for (int i = 0; i < size; i++) {
			cursors[i] = getCursor(tables.get(i), null, true);
		}
		return cursors;
	}
	
	/**
	 * 得到table的游标
	 * @param table
	 * @param mcs
	 * @param addOpt 是否把附加计算添加
	 * @return
	 */
	private ICursor getCursor(ITableMetaData table, ICursor mcs, boolean addOpt) {
		ICursor cursor = null;
		if (fkNames != null) {
			if (mcs != null ) {
				if (mcs instanceof MultipathCursors) {
					cursor = table.cursor(null, this.names, filter, fkNames, codes, null, (MultipathCursors)mcs, null, ctx);
				} else {
					if (exps == null) {
						cursor = table.cursor(null, this.names, filter, fkNames, codes, null, ctx);
					} else {
						cursor = table.cursor(this.exps, this.names, filter, fkNames, codes, null, ctx);
					}
				}
			} else if (pathCount > 1) {
				if (exps == null) {
					cursor = table.cursor(null, this.names, filter, fkNames, codes, null, pathCount, ctx);
				} else {
					cursor = table.cursor(this.exps, this.names, filter, fkNames, codes, null, pathCount, ctx);
				}
			} else {
				if (exps == null) {
					cursor = table.cursor(null, this.names, filter, fkNames, codes, null, ctx);
				} else {
					cursor = table.cursor(this.exps, this.names, filter, fkNames, codes, null, ctx);
				}
			}
		} else {
			if (mcs != null ) {
				if (mcs instanceof MultipathCursors) {
					cursor = table.cursor(null, this.names, filter, null, null, null, (MultipathCursors)mcs, null, ctx);
				} else {
					if (exps == null) {
						cursor = table.cursor(this.names, filter, ctx);
					} else {
						cursor = table.cursor(this.exps, this.names, filter, null, null, null, ctx);
					}
				}
			} else if (pathCount > 1) {
				if (exps == null) {
					cursor = table.cursor(null, this.names, filter, null, null, null, pathCount, ctx);
				} else {
					cursor = table.cursor(this.exps, this.names, filter, null, null, null, pathCount, ctx);
				}
			} else {
				if (exps == null) {
					cursor = table.cursor(this.names, filter, ctx);
				} else {
					cursor = table.cursor(this.exps, this.names, filter, null, null, null, ctx);
				}
			}
		}
		
		if (getPd() != null && getPd().getColumns() != null) {
			for (PseudoColumn column : getPd().getColumns()) {
				if (column.getDim() != null) {//如果存在外键，则添加一个switch的延迟计算
					Sequence dim;
					if (column.getDim() instanceof Sequence) {
						dim = (Sequence) column.getDim();
					} else {
						dim = ((IPseudo) column.getDim()).cursor(null, null).fetch();
					}
					
					String fkey[] = column.getFkey();
					if (fkey == null) {
						String[] fkNames = new String[] {column.getName()};//此时name就是外键字段
						Sequence[] codes = new Sequence[] {dim};
						Switch s = new Switch(fkNames, codes, null, null);
						cursor.addOperation(s, ctx);
//					} else if (fkey.length == 1) {
//						Sequence[] codes = new Sequence[] {dim};
//						Switch s = new Switch(fkey, codes, null, null);
//						cursor.addOperation(s, ctx);
					} else {
						int size = fkey.length;
						Expression[][] exps = new Expression[1][];
						exps[0] = new Expression[size];
						for (int i = 0; i < size; i++) {
							exps[0][i] = new Expression(fkey[i]);
						}
						Expression[][] newExps = new Expression[1][];
						newExps[0] = new Expression[] {new Expression("~")};
						String[][] newNames = new String[1][];
						newNames[0] = new String[] {column.getName()};
						Join join = new Join(null, null, exps, new Sequence[] {dim}, new Expression[1][], newExps, newNames, null);
						cursor.addOperation(join, ctx);
					}
				}
			}
		}
	
		if (addOpt) {
			if (opList != null) {
				for (Operation op : opList) {
					cursor.addOperation(op, ctx);
				}
			}
			if (extraOpList != null) {
				for (Operation op : extraOpList) {
					cursor.addOperation(op, ctx);
				}
			}
		}

		return cursor;
	
	}
	
	/**
	 * 归并或者连接游标
	 * @param cursors
	 * @return
	 */
	static ICursor mergeCursor(ICursor cursors[],String user, Context ctx) {
		DataStruct ds = cursors[0].getDataStruct();
		int[] sortFields = null;
		
		/**
		 * 如果存在user（账户属性）就使用user字段归并,否则使用主键归并。
		 * 没有主键就不归并。
		 */
		if (user != null) {
			int idx = ds.getFieldIndex(user);
			if (idx >= 0) {
				sortFields = new int[] {idx};
			}
		} else {
			sortFields = ds.getPKIndex();
		}
		if (sortFields != null) {
			return new MergeCursor(cursors, sortFields, null, ctx);//有序则归并
		} else {
			return new ConjxCursor(cursors);//无序则连接
		}
	}
	
	private ICursor addOptionToCursor(ICursor cursor) {
		if (opList != null) {
			for (Operation op : opList) {
				cursor.addOperation(op, ctx);
			}
		}
		if (extraOpList != null) {
			for (Operation op : extraOpList) {
				cursor.addOperation(op, ctx);
			}
		}
		return cursor;
	}
	
	//返回虚表的游标
	public ICursor cursor(Expression []exps, String []names) {
		setFetchInfo(exps, names);//把取出字段添加进去，里面可能会对extraOpList赋值
		
		//每个实体文件生成一个游标
		List<ITableMetaData> tables = getPd().getTables();
		int size = tables.size();
		ICursor cursors[] = new ICursor[size];
		
		/**
		 * 对得到游标进行归并，分为情况
		 * 1 只有一个游标则返回；
		 * 2 有多个游标且不并行时，进行归并
		 * 3 有多个游标且并行时，先对第一个游标分段，然后其它游标按第一个同步分段，最后把每个游标的每个段进行归并
		 */
		if (size == 1) {//只有一个游标直接返回
			return getCursor(tables.get(0), null, true);
		} else {
			if (pathCount > 1) {//指定了并行数，此时忽略mcsTable
				cursors[0] = getCursor(tables.get(0), null, false);
				for (int i = 1; i < size; i++) {
					cursors[i] = getCursor(tables.get(i), cursors[0], false);
				}
			} else {//没有指定并行数
				if (mcsTable == null) {//没有指定分段参考虚表mcsTable
					for (int i = 0; i < size; i++) {
						cursors[i] = getCursor(tables.get(i), null, false);
					}
					return addOptionToCursor(mergeCursor(cursors, pd.getUser(), ctx));
				} else {//指定了分段参考虚表mcsTable
					ICursor mcs = null;
					if (mcsTable != null) {
						mcs = mcsTable.cursor();
					}
					for (int i = 0; i < size; i++) {
						cursors[i] = getCursor(tables.get(i), mcs, false);
					}
					mcs.close();
				}
			}
			
			//对cursors按段归并或连接:把所有游标的第N路归并,得到N个游标,再把这N个游标做成多路游标返回
			int mcount = ((MultipathCursors)cursors[0]).getPathCount();//分段数
			ICursor mcursors[] = new ICursor[mcount];//结果游标
			for (int m = 0; m < mcount; m++) {
				ICursor cursorArray[] = new ICursor[size];
				for (int i = 0; i < size; i++) {
					cursorArray[i] = ((MultipathCursors)cursors[i]).getCursors()[m];
				}
				mcursors[m] = mergeCursor(cursorArray, pd.getUser(), ctx);
			}
			return addOptionToCursor(new MultipathCursors(mcursors, ctx));
		}
	}
	
	//用于获取多路游标
	private ICursor cursor() {
		List<ITableMetaData> tables = getPd().getTables();
		return tables.get(0).cursor(null, null, null, null, null, null, pathCount, ctx);
	}

	public Object clone(Context ctx) throws CloneNotSupportedException {
		PseudoTable obj = new PseudoTable();
		obj.hasPseudoColumns = hasPseudoColumns;
		obj.pathCount = pathCount;
		obj.mcsTable = mcsTable;
		obj.fkNames = fkNames == null ? null : fkNames.clone();
		obj.codes = codes == null ? null : codes.clone();
		cloneField(obj);
		obj.ctx = ctx;
		return obj;
	}

	public PseudoTable setPathCount(int pathCount) {
		PseudoTable table = null;
		try {
			table = (PseudoTable) clone(ctx);
			table.pathCount = pathCount;
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return table;
	}

	public PseudoTable setMcsTable(PseudoTable mcsTable) {
		PseudoTable table = null;
		try {
			table = (PseudoTable) clone(ctx);
			table.mcsTable = mcsTable;
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return table;
	}
	
	/**
	 * 把表达式里涉及伪字段的枚举、二值运算进行转换
	 * @param node
	 */
	private void parseFilter(Node node) {
		if (node instanceof And || node instanceof Or) {
			parseFilter(node.getLeft());
			parseFilter(node.getRight());
		} else if (node instanceof Equals || node instanceof NotEquals) {
			//对伪字段的==、!=进行处理
			if (node.getLeft() instanceof UnknownSymbol) {
				//判断是否是伪字段
				String pname = ((UnknownSymbol) node.getLeft()).getName();
				PseudoColumn col = getPd().findColumnByPseudoName(pname);
				if (col != null) {
					Sequence seq;
					//判断是否是对枚举伪字段进行运算
					seq = col.get_enum();
					if (seq != null) {
						node.setLeft(new UnknownSymbol(col.getName()));//改为真字段
						Integer obj = seq.firstIndexOf(node.getRight().calculate(ctx));
						obj--;
						node.setRight(new Constant(obj));//把枚举值改为对应的真的值
					}
					
					//判断是否是对二值伪字段进行运算
					seq = col.getBits();
					if (seq != null) {
						int idx = seq.firstIndexOf(pname) - 1;
						int bit = 1 << idx;
						String str = "and(" + col.getName() + "," + bit + ")";
						node.setLeft(new Expression(str).getHome());//改为真字段的位运算
						if ((Boolean) node.getRight().calculate(ctx)) {
							node.setRight(new Constant(bit));
						} else {
							node.setRight(new Constant(0));
						}
					}
				}
			} else if (node.getRight() instanceof UnknownSymbol) {
				//处理字段名在右边的情况，左右交换一下再处理，逻辑跟上面一样
				Node right = node.getRight();
				node.setRight(node.getLeft());
				node.setLeft(right);
				parseFilter(node);
			}
		} else if (node instanceof DotOperator) {
			//对有枚举列表的伪字段的contain进行处理
			if (node.getRight() instanceof Contain) {
				Contain contain = (Contain)node.getRight();
				IParam param = contain.getParam();
				if (param == null || !param.isLeaf()) {
					return;
				}
				
				//判断是否是对伪字段进行contain运算
				UnknownSymbol un = (UnknownSymbol) param.getLeafExpression().getHome();
				PseudoColumn col = getPd().findColumnByPseudoName(un.getName());
				if (col != null && col.get_enum() != null) {
					Object val = node.getLeft().calculate(ctx);
					if (val instanceof Sequence) {
						//把contain右边的字段名改为真字段
						IParam newParam = ParamParser.parse(col.getName(), null, ctx);
						contain.setParam(newParam);
						
						//把contain左边的枚举值序列改为对应的真的值的序列
						Sequence value = (Sequence) val;
						Sequence newValue = new Sequence();
						int size = value.length();
						for (int i = 1; i <= size; i++) {
							Integer obj = col.get_enum().firstIndexOf(value.get(i));
							obj--;
							newValue.add(obj);
						}
						node.setLeft(new Constant(newValue));
					}
				}
			}
		}
	}
	
	public Operable addOperation(Operation op, Context ctx) {
		if (op == null) {
			return this;
		}
		if (hasPseudoColumns) {
			/**
			 * 处理伪字段，二值字段，枚举字段
			 */
			Expression exp = op.getFunction().getParam().getLeafExpression();
			Node node = exp.getHome();
			parseFilter(node);
		}
		
		/**
		 * 当虚表的user存在，并且是做group@u(user)，并且user不是首字段时
		 * 把group@u(user)转换为.group(首字段).conj(~.group(user))
		 */
		if (pd.getUser() != null && op instanceof Group) {
			String ugrp = pd.getAllColNames()[0];//首字段
			Group group = (Group) op;
			if (!(pd.getUser().equals(ugrp)) && group.getOpt() != null && group.getOpt().indexOf("u") >= 0) {
				Group newGroup = new Group(new Expression[] {new Expression(ugrp)}, null);
				Conj conj = new Conj(new Expression("~.group(" + pd.getUser() + ")"));
				return super.addOperation(newGroup, ctx).addOperation(conj, ctx);
			}
		}
		
		return super.addOperation(op, ctx);
	}
	
	/**
	 * 把游标的伪列转换为真字段，用于update和append
	 * @param cursor
	 * @param columns
	 * @param fields
	 */
	private void convertPseudoColumn(ICursor cursor, List<PseudoColumn> columns, String fields[]) {
		//先把不是伪字段的赋值过来
		DataStruct ds = new DataStruct(fields);
		int size = ds.getFieldCount();
		Expression []exps = new Expression[size];
		String []names = new String[size];
		for (int c = 0; c < size; c++) {
			exps[c] = new Expression(fields[c]);
			names[c] = fields[c];
		}
		
		//转换游标里的伪字段
		size = columns.size();
		for (int c = 0; c < size; c++) {
			PseudoColumn column = columns.get(c);
			String pseudoName = column.getPseudo();
			Sequence bitNames = column.getBits();
			int idx = ds.getFieldIndex(column.getName());
			
			if (column.getExp() != null) {
				//有表达式的伪列
				exps[idx] = new Expression(column.getExp());
				names[idx] = column.getName();
			} else if (pseudoName != null && column.get_enum() != null) {
				//枚举伪列
				String var = "pseudo_enum_value_" + c;
				Context context = cursor.getContext();
				if (context == null) {
					context = new Context();
					cursor.setContext(context);
					context.setParamValue(var, column.get_enum());
				} else {
					context.setParamValue(var, column.get_enum());
				}
				exps[idx] = new Expression(var + ".pos(" + pseudoName + ")");
				names[idx] = column.getName();
			} else if (bitNames != null) {
				//处理二值伪字段(多个伪字段按位转换为一个真字段)
				String exp = "0";
				int len = bitNames.length();
				for (int i = 1; i <= len; i++) {
					String field = (String) bitNames.get(i);
					//转换为bit值,并累加
					int bit = 1 << (i - 1);
					exp += "+ if(" + field + "," + bit + ",0)";
				}
				exps[idx] = new Expression(exp);
				names[idx] = column.getName();
			}
		}
		
		New _new = new New(exps, names, null);
		cursor.addOperation(_new, null);
	}
	
	public void append(ICursor cursor, String option) {
		//把数据追加到file，如果有多个file则取最后一个
		List<ITableMetaData> tables = getPd().getTables();
		int size = tables.size();
		if (size == 0) {
			return;
		}
		ITableMetaData table = tables.get(size - 1);

		List<PseudoColumn> columns = pd.getColumns();
		if (columns != null) {
			String fields[] = table.getAllColNames();
			convertPseudoColumn(cursor, columns, fields);
		}
		
		try {
			table.append(cursor, option);
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		}
	}
	
	public Sequence update(Sequence data, String opt) {
		//更新到最后一个file
		List<ITableMetaData> tables = getPd().getTables();
		int size = tables.size();
		if (size == 0) {
			return null;
		}
		ITableMetaData table = tables.get(size - 1);

		List<PseudoColumn> columns = pd.getColumns();
		if (columns != null) {
			String fields[] = table.getAllColNames();
			ICursor cursor = new MemoryCursor(data);
			convertPseudoColumn(cursor, columns, fields);
			data = cursor.fetch();
		}
		
		try {
			return table.update(data, opt);
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		}
	}
	
	public Sequence delete(Sequence data, String opt) {
		List<ITableMetaData> tables = getPd().getTables();
		int size = tables.size();
		if (size == 0) {
			return null;
		}
		
		Sequence result = null;
		for (ITableMetaData table : tables) {
			List<PseudoColumn> columns = pd.getColumns();
			if (columns != null) {
				String fields[] = table.getAllColNames();
				ICursor cursor = new MemoryCursor(data);
				convertPseudoColumn(cursor, columns, fields);
				data = cursor.fetch();
			}
			
			try {
				result = table.delete(data, opt);
			} catch (IOException e) {
				throw new RQException(e.getMessage(), e);
			}
		}
		return result;
	}
	
	// 判断是否配置了指定名称的外键
	public boolean hasForeignKey(String fkName) {
		PseudoColumn column = pd.findColumnByName(fkName);
		if (column.getDim() != null)
			return true;
		else
			return false;
	}
	
	/**
	 * 添加外键
	 * @param fkName	外键名
	 * @param fieldNames 外键字段
	 * @param code	外表
	 * @return
	 */
	public PseudoTable addForeignKeys(String fkName, String []fieldNames, PseudoTable code) {
		PseudoTable table = null;
		try {
			table = (PseudoTable) clone(ctx);
			table.getPd().addPseudoColumn(new PseudoColumn(fkName, fieldNames, code));
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return table;
	}
	
	// 主子表按主表主键做有序连接
	public static ICursor join(PseudoTable masterTable, PseudoTable subTable) {
		String[] keys = masterTable.getPrimaryKey();
		if (keys != null) {
			int size = keys.length;
			Expression[] exps = new Expression[size];
			for (int i = 0; i < size; i++) {
				exps[i] = new Expression(keys[i]);
			}
			Expression [][]joinExps = new Expression [][] {exps, exps};//使用主表的主键做join
			
			ICursor cursors[] = new ICursor[]{masterTable.cursor(null, null), subTable.cursor(null, null)};
			ICursor cursor = CursorUtil.joinx(cursors, null, joinExps, null, masterTable.getContext());
			return cursor;
		}
		return null;
	}
	
	// 取虚表主键
	public String[] getPrimaryKey() {
		return getPd().getAllSortedColNames();
	}
	
	// 取字段做switch指向的虚表，如果没做则返回空
	public PseudoTable getFieldSwitchTable(String fieldName) {
		List<PseudoColumn> columns = pd.getColumns();
		if (columns == null) {
			return null;
		}
		
		for (PseudoColumn column : columns) {
			if (column.getDim() != null) {
				if (column.getFkey() == null && column.getName().equals(fieldName)) {
					return (PseudoTable) column.getDim();
				} 
//				else if (column.getFkey() != null 
//						&& column.getFkey().length == 1
//						&& column.getFkey()[0].equals(fieldName)) {
//					return (PseudoTable) column.getDim();
//				}
			}
		}
		return null;
	}
}


