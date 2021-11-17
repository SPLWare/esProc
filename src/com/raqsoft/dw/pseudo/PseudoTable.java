package com.raqsoft.dw.pseudo;

import java.util.ArrayList;
import java.util.List;

import com.raqsoft.dm.Context;
import com.raqsoft.dm.Machines;
import com.raqsoft.dm.Record;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.cursor.ConjxCursor;
import com.raqsoft.dm.cursor.ICursor;
import com.raqsoft.dm.cursor.MergeCursor;
import com.raqsoft.dm.cursor.MultipathCursors;
import com.raqsoft.dm.op.Derive;
import com.raqsoft.dm.op.Operable;
import com.raqsoft.dm.op.Operation;
import com.raqsoft.dm.op.Switch;
import com.raqsoft.dw.ITableMetaData;
import com.raqsoft.expression.Constant;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.Node;
import com.raqsoft.expression.ParamParser;
import com.raqsoft.expression.UnknownSymbol;
import com.raqsoft.expression.mfn.sequence.Contain;
import com.raqsoft.expression.operator.And;
import com.raqsoft.expression.operator.DotOperator;
import com.raqsoft.expression.operator.Equals;
import com.raqsoft.expression.operator.NotEquals;
import com.raqsoft.expression.operator.Or;

public class PseudoTable extends Pseudo implements Operable, IPseudo {	
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
	public PseudoTable(Record rec, Machines hs, int n, Context ctx) {
		pd = new PseudoDefination(rec, ctx);
		pathCount = n;
		this.ctx = ctx;
		extraNameList = new ArrayList<String>();
		init();
	}
	
//	public PseudoTable(ITableMetaData table, Context ctx) {
//		this.table = table;
//		this.ctx = ctx;
//		extraNameList = new ArrayList<String>();
//		init();
//	}
//	
//	public PseudoTable(ITableMetaData table, int n, Context ctx) {
//		this.table = table;
//		this.ctx = ctx;
//		extraNameList = new ArrayList<String>();
//		pathCount = n;
//		init();
//	}
//	
//	public PseudoTable(ITableMetaData table, PseudoTable ptable, Context ctx) {
//		this.table = table;
//		this.ctx = ctx;
//		extraNameList = new ArrayList<String>();
//		mcsTable = ptable;
//		init();
//	}
	
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
			//有取出表达式也有取出字段
			//检查extraNameList里是否包含exps里的字段
			//如果有，就去掉
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
					tempExpList.add(exp);
					tempNameList.add(name);
				} else if (node instanceof DotOperator) {
					Node left = node.getLeft();
					if (left != null && left instanceof UnknownSymbol) {
						PseudoColumn col = getPd().findColumnByName( ((UnknownSymbol)left).getName());
						if (col != null) {
							Derive derive = new Derive(new Expression[] {exp}, new String[] {name}, null);
							extraOpList.add(derive);
						}
					}
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
	}
	
	private String[] getFetchColNames(String []fields) {
		//if (fields == null) return null;
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
			cursors[i] = getCursor(tables.get(i), null);
		}
		return cursors;
	}
	
	/**
	 * 得到table的游标
	 * @param table
	 * @param mcs
	 * @return
	 */
	private ICursor getCursor(ITableMetaData table, ICursor mcs) {
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
				//如果存在外键，则添加一个switch的延迟计算
				if (column.getDim() != null) {
					String[] fkNames = new String[] {column.getName()};
					Sequence[] codes = new Sequence[] {(Sequence) column.getDim()};
					Expression[] expressions = new Expression[1];
					if (column.getFkey() != null) {
						expressions[0] = new Expression(column.getFkey());
					} else {
						expressions[0] = new Expression(column.getName());
					}
					Switch s = new Switch(fkNames, codes, expressions, null);
					cursor.addOperation(s, ctx);
				}
			}
		}
	
		if (extraOpList != null) {
			for (Operation op : extraOpList) {
				cursor.addOperation(op, ctx);
			}
		}
		if (opList != null) {
			for (Operation op : opList) {
				cursor.addOperation(op, ctx);
			}
		}
		
		return cursor;
	
	}
	
	/**
	 * 归并或者连接游标
	 * @param cursors
	 * @return
	 */
	static ICursor mergeCursor(ICursor cursors[], Context ctx) {
		int[] sortFields = cursors[0].getDataStruct().getPKIndex();
		if (sortFields != null) {
			return new MergeCursor(cursors, sortFields, null, ctx);//有序则归并
		} else {
			return new ConjxCursor(cursors);//无序则连接
		}
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
			return getCursor(tables.get(0), null);
		} else {
			if (pathCount > 1) {//指定了并行数，此时忽略mcsTable
				cursors[0] = getCursor(tables.get(0), null);
				for (int i = 1; i < size; i++) {
					cursors[i] = getCursor(tables.get(i), cursors[0]);
				}
			} else {//没有指定并行数
				if (mcsTable == null) {//没有指定分段参考虚表mcsTable
					for (int i = 0; i < size; i++) {
						cursors[i] = getCursor(tables.get(i), null);
					}
					return mergeCursor(cursors, ctx);
				} else {//指定了分段参考虚表mcsTable
					ICursor mcs = null;
					if (mcsTable != null) {
						mcs = mcsTable.cursor();
					}
					for (int i = 0; i < size; i++) {
						cursors[i] = getCursor(tables.get(i), mcs);
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
				mcursors[m] = mergeCursor(cursorArray, ctx);
			}
			return new MultipathCursors(mcursors, ctx);
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

	public void setPathCount(int pathCount) {
		this.pathCount = pathCount;
	}

	public void setMcsTable(PseudoTable mcsTable) {
		this.mcsTable = mcsTable;
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
							newValue.add(obj);
						}
						node.setLeft(new Constant(newValue));
					}
				}
			}
		}
	}
	
	public Operable addOperation(Operation op, Context ctx) {
		if (hasPseudoColumns) {
			Expression exp = op.getFunction().getParam().getLeafExpression();
			Node node = exp.getHome();
			parseFilter(node);
		}
		return super.addOperation(op, ctx);
	}
}

