package com.scudata.dm.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.scudata.cellset.ICellSet;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.FileObject;
import com.scudata.dm.Param;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;
import com.scudata.resources.ParseMessage;
import com.scudata.util.JSONUtil;
import com.scudata.util.Variant;
import com.scudata.dm.sql.FunInfoManager;
import com.scudata.excel.ExcelTool;
import com.scudata.expression.Expression;
import com.scudata.resources.EngineMessage;

// select语句
public class Select extends QueryBody {
	// 表达式所在的位置
	enum Part {
		Column, Join, Where, Group, Having, Order
	}
	
	private static final String []FROMKEYS = new String[] {"JOIN", "ON", "CROSS", "INNER", "LEFT", 
			"RIGHT", "FULL", "WHERE", "GROUP", "HAVING", "ORDER", "LIMIT", "OFFSET", "INTO"};

	private SimpleSQL query; // select所属的查询语句
	private Token []tokens;
	//private int start;
	//private int next;
	
	private boolean distinct;
	private List<Column> columnList = new ArrayList<Column>(); // 选出字段
	private QueryBody from;
	private Exp where;
	private List<Exp> groupBy;
	private Exp having;
	private List<SortItem> orderBy;
	private int limit;
	private int offset;
	private String intoFile;
	
	private DataStruct dataStruct; // 结果集数据结构
	private ArrayList<GatherNode> gatherList = new ArrayList<GatherNode>();
	private int level = 0; // 查询的层次
	
	abstract public class Exp {
		protected int start; // 起始位置，包含
		protected int end; // 结束位置，不包含
		
		public Exp(int start, int end) {
			this.start = start;
			this.end = end;
		}
		
		public int getStart() {
			return start;
		}

		public void setStart(int start) {
			this.start = start;
		}

		public int getEnd() {
			return end;
		}

		public void setEnd(int end) {
			this.end = end;
		}
		
		// 是否是逻辑运算符and、or
		public boolean isLogicalOperator() {
			return false;
		}
		
		public int getPos() {
			return tokens[start].getPos();
		}
		
		public String getFieldName() {
			return null;
		}
		
		abstract public boolean isEquals(Exp node);
		
		public void getFields(List<FieldNode> resultList) {
		}
		
		public List<And> splitAnd() {
			List<FieldNode> fieldList = new ArrayList<FieldNode>();
			getFields(fieldList);
			And and = new And(this, fieldList);
			List<And> result = new ArrayList<And>();
			result.add(and);
			return result;
		}
		
		// 是否是与运算
		public boolean isAnd() {
			return false;
		}
		
		// 是否是或运算
		public boolean isOr() {
			return false;
		}
		
		// 是否是等号
		public boolean isEqualOperator() {
			return false;
		}
		
		abstract public String toSPL();
		
		public String toSPL(int groupByCount) {
			return toSPL();
		}
		
		// 拆分关连表达式
		public boolean splitJionExp(QueryBody leftTable, QueryBody rightTable, 
				List<Expression> leftExpList, List<Expression> rightExpList) {
			return false;
		}
		
		// 拆分关连表达式
		public boolean splitJionExp(List<QueryBody> tableList, int lastTable, 
				List<Expression> leftExpList, List<Expression> rightExpList) {
			return false;
		}
	}

	class Between extends Exp {
		private Exp left;
		private Exp from;
		private Exp to;
		private boolean isNot;

		public Between(Exp left, Exp from, Exp to, Exp not) {
			super(left.getStart(), to.getEnd());
			this.left = left;
			this.from = from;
			this.to = to;
			this.isNot = not != null;
		}

		public boolean isEquals(Exp node) {
			if (!(node instanceof Between)) {
				return false;
			}
			
			Between other = (Between)node;
			return isNot == other.isNot && Select.isEquals(left, other.left) 
					&& Select.isEquals(from, other.from) && Select.isEquals(to, other.to);
		}
		
		public void getFields(List<FieldNode> resultList) {
			left.getFields(resultList);
			from.getFields(resultList);
			to.getFields(resultList);
		}
		
		public String toSPL() {
			String x = left.toSPL();
			String a = from.toSPL();
			String b = to.toSPL();
			
			if(isNot) {
				return "!between(" + x + "," + a + ":" + b + ")";
			} else {
				return "between(" + x + "," + a + ":" + b + ")";
			}
		}
	}
	
	// case [exp] when ... then ...
	//            when ... then ...
	//            else ... 
	//end
	class Case extends Exp {
		private Exp field;
		private List<Exp> whenList;
		private List<Exp> thenList;
		private Exp defaultExp;
		
		public Case(int start, int end, Exp field, List<Exp> whenList, List<Exp> thenList, Exp defaultExp) {
			super(start, end);
			
			this.field = field;
			this.whenList = whenList;
			this.thenList = thenList;
			this.defaultExp = defaultExp;
		}

		public boolean isEquals(Exp node) {
			if (!(node instanceof Case)) {
				return false;
			}
			
			Case other = (Case)node;
			if (!Select.isEquals(field, other.field) || !Select.isEquals(defaultExp, other.defaultExp)) {
				return false;
			}
			
			int count = whenList.size();
			if (count != other.whenList.size()) {
				return false;
			}
			
			for (int i = 0; i < count; ++i) {
				if (!Select.isEquals(whenList.get(i), other.whenList.get(i)) ||
						!Select.isEquals(thenList.get(i), other.thenList.get(i))) {
					return false;
				}
			}
			
			return true;
		}
		
		public void getFields(List<FieldNode> resultList) {
			if (field != null) {
				field.getFields(resultList);
			}
			
			for (Exp exp : whenList) {
				exp.getFields(resultList);
			}
			
			for (Exp exp : thenList) {
				exp.getFields(resultList);
			}
			
			if (defaultExp != null) {
				defaultExp.getFields(resultList);
			}
		}
		
		public String toSPL() {
			String spl;
			if (field == null) {
				spl = "if(";
				int size = whenList.size();
				
				for (int i = 0; i < size; ++i) {
					if (i > 0) {
						spl += ",";
					}
					
					spl += whenList.get(i).toSPL();
					spl += ":";
					spl += thenList.get(i).toSPL();
				}
			} else {
				spl = "case(";
				spl += field.toSPL();
				int size = whenList.size();
				
				for (int i = 0; i < size; ++i) {
					spl += ",";
					spl += whenList.get(i).toSPL();
					spl += ":";
					spl += thenList.get(i).toSPL();
				}
			}
			
			if (defaultExp != null) {
				spl += ";";
				spl += defaultExp.toSPL();
			}

			return spl + ")";
		}
	}
	
	// SQL中和集算器规则相同的表达式
	class CommonNode extends Exp {
		private String exp;
		
		public CommonNode(int start, int end, String exp) {
			super(start, end);
			this.exp = exp;
		}
				
		public boolean isLogicalOperator() {
			return exp.equals("&&") || exp.equals("||");
		}
		
		public boolean isAnd() {
			return exp.equals("&&");
		}
		
		public boolean isOr() {
			return exp.equals("||");
		}
		
		public boolean isEqualOperator() {
			return exp.equals("==");
		}
		
		public boolean isEquals(Exp node) {
			if (!(node instanceof CommonNode)) {
				return false;
			}
			
			CommonNode other = (CommonNode)node;
			return Select.isEquals(other.exp, exp);
		}
		
		public String toSPL() {
			return exp;
		}
	}

	class Exists extends Exp {
		private QueryBody query;
		
		public Exists(int start, int end, QueryBody query) {
			super(start, end);
			this.query = query;
		}

		public boolean isEquals(Exp node) {
			return node == this;
		}
		
		public String toSPL() {
			return query.toSPL() + ".len()>0";
		}
	}

	class FieldNode extends Exp {
		private String tableName;
		private String fieldName;
		private Part part;
		private QueryBody table; // 字段对应的表

		public FieldNode(int start, int end, String tableName, String fieldName, Part part) {
			super(start, end);
			this.tableName = tableName;
			this.fieldName = fieldName;
			this.part = part;
			
			checkFileAttribute(tableName, fieldName);
		}
		
		public String getFieldName() {
			return fieldName;
		}

		public QueryBody getTable() {
			if (table == null) {
				table = getFromTable(tableName, fieldName);
				if (table == null) {
					MessageManager mm = ParseMessage.get();
					throw new RQException(fieldName + mm.getMessage("field.notExist"));
				}
			}
			
			return table;
		}
		
		public boolean isEquals(Exp node) {
			if (!(node instanceof FieldNode)) {
				return false;
			}
			
			FieldNode other = (FieldNode)node;
			return getTable() == other.getTable() && Select.isEquals(fieldName, other.fieldName);
		}
		
		public void getFields(List<FieldNode> resultList) {
			resultList.add(this);
		}
		
		// 是否可以用别名引用选出列，order by和having可以引用选出列别名
		private boolean canRefSelectedCol() {
			return part == Part.Order || part == Part.Having;
		}
		
		public String toSPL() {
			if (canRefSelectedCol() && tableName == null) {
				Column column = getColumn(fieldName);
				if (column != null) {
					return column.toSPL();
				}
			}
			
			QueryBody table = getTable();
			DataStruct ds = table.getDataStruct();
			int findex = getFieldIndex(ds, fieldName);
			
			if (findex == -1) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(fieldName + mm.getMessage("ds.fieldNotExist"));
			}
			
			findex++;
			String joinFieldName = table.getJoinFieldName();
			if (joinFieldName != null) {
				return joinFieldName + ".#" + findex;
			}
			
			int level = getLevel() - table.getLevel();
			if (level == 0) {
				return "#" + findex;
			} else {
				return "get(" + level + ",#" + findex + ")";
			}
		}
	}

	class Function extends Exp {
		private String fnName; // 函数名
		private List<Exp> params; // 参数
		
		public Function(int start, int end, String fnName, List<Exp> params) {
			super(start, end);
			this.fnName = fnName;
			this.params = params;
		}
		
		public boolean isEquals(Exp node) {
			if (!(node instanceof Function)) {
				return false;
			}
			
			Function other = (Function)node;
			if (!Select.isEquals(other.fnName, fnName)) {
				return false;
			}
			
			if (params == null) {
				return other.params == null;
			} else if (other.params == null) {
				return false;
			} else {
				int pcount = params.size();
				if (pcount != other.params.size()) {
					return false;
				}
				
				for (int i = 0; i < pcount; ++i) {
					if (!Select.isEquals(params.get(i), other.params.get(i))) {
						return false;
					}
				}
				
				return true;
			}
		}
		
		public void getFields(List<FieldNode> resultList) {
			if (params != null) {
				for (Exp exp : params) {
					exp.getFields(resultList);
				}
			}
		}
		
		public String toSPL() {
			int size = params.size();
			String []args = new String[size];
			for (int i = 1; i < size; ++i) {
				args[i] = params.get(i).toSPL();
			}

			String spl = FunInfoManager.getFunctionExp("ESPROC", fnName, args);
			if (spl == null) {
				spl = fnName + "(";
				for (int i = 0; i < size; ++i) {
					if (i > 0) {
						spl += ",";
					}
					
					spl += args[i];
				}
			}
			
			return spl;
		}
	}

	class GatherNode extends Exp {
		private String fnName; // 函数名
		private Exp param; // 参数
		private int fieldSeq; // 在分组结果集汇总字段中的序号
		
		public GatherNode(int start, int end, String fnName, Exp param) {
			super(start, end);
			this.fnName = fnName.toLowerCase();
			this.param = param;
		}

		public int getFieldSeq() {
			return fieldSeq;
		}

		public void setFieldSeq(int fieldSeq) {
			this.fieldSeq = fieldSeq;
		}

		public boolean isEquals(Exp node) {
			if (!(node instanceof GatherNode)) {
				return false;
			}
			
			GatherNode other = (GatherNode)node;
			if (!Select.isEquals(other.fnName, fnName)) {
				return false;
			}
			
			return Select.isEquals(param, other.param);
		}
		
		public void getFields(List<FieldNode> resultList) {
			param.getFields(resultList);
		}
		
		public String toSPL() {
			return fnName + "(" + param.toSPL() + ")";
		}
		
		public String toSPL(int groupByCount) {
			return "#" + (groupByCount + fieldSeq);
		}
	}

	class In extends Exp {
		private Exp left;
		private List<Exp> exps;
		private QueryBody subQuery;
		private boolean isNot;
		
		public In(int start, int end) {
			super(start, end);
		}
		
		public void setLeft(Exp left) {
			this.left = left;
			setStart(left.getStart());
		}

		public void setRight(List<Exp> right) {
			this.exps = right;
		}
		
		public void setRight(QueryBody right) {
			this.subQuery = right;
		}

		public void setNot(boolean isNot) {
			this.isNot = isNot;
		}
		
		public boolean isEquals(Exp node) {
			if (!(node instanceof In)) {
				return false;
			}
			
			In other = (In)node;
			if (isNot != other.isNot || !Select.isEquals(left, other.left)) {
				return false;
			}
			
			if (exps == null || other.exps == null) {
				return false;
			}
			
			int count = exps.size();
			if (count != other.exps.size()) {
				return false;
			}
			
			for (int i = 0; i < count; ++i) {
				if (!Select.isEquals(exps.get(i), other.exps.get(i))) {
					return false;
				}
			}
			
			return true;
		}
		
		public void getFields(List<FieldNode> resultList) {
			left.getFields(resultList);
			
			if (exps != null) {
				for (Exp exp : exps) {
					exp.getFields(resultList);
				}
			}
		}
		
		public String toSPL() {
			String x = left.toSPL();
			String spl;
			
			if (exps == null) {
				String query = subQuery.toSPL();
				spl = query + ".(#1).contain(" + x + ")";
			} else {
				// in只有一个值时判断是否是参数序列
				int size = exps.size();
				if (size == 1 && exps.get(0) instanceof CommonNode) {
					String v = exps.get(0).toSPL();
					Param p = getContext().getParam(v);
					if (p != null && p.getValue() instanceof Sequence) {
						spl = v + ".contain(" + x + ")";
					} else {
						spl = "[" + v + "]" + ".contain(" + x + ")";
					}
				} else {
					spl = "[";
					for (int i = 0; i < size; ++i) {
						if (i > 0) {
							spl += ",";
						}
						
						Exp exp = exps.get(i);
						spl += exp.toSPL();
					}
					
					spl += "]" + ".contain(" + x + ")";
				}
			}
			
			if(isNot) {
				return "!" + spl;
			} else {
				return spl;
			}
		}
	}

	class IsNull extends Exp {
		private Exp exp;
		private boolean isNot;
		
		public IsNull(int end, Exp exp, boolean isNot) {
			super(exp.getStart(), end);
			this.exp = exp;
			this.isNot = isNot;
		}
		
		public boolean isEquals(Exp node) {
			if (!(node instanceof IsNull)) {
				return false;
			}
			
			IsNull other = (IsNull)node;
			return isNot == other.isNot && Select.isEquals(exp, other.exp);
		}
		
		public void getFields(List<FieldNode> resultList) {
			exp.getFields(resultList);
		}
		
		public String toSPL() {
			if(isNot) {
				return exp.toSPL() + "!=null";
			} else {
				return exp.toSPL() + "==null";
			}
		}
	}

	class Like extends Exp {
		private Exp left;
		private Exp right;
		private boolean isNot;
		
		public Like(int start, int end) {
			super(start, end);
		}

		public void setLeft(Exp left) {
			this.left = left;
			setStart(left.getStart());
		}

		public void setRight(Exp right) {
			this.right = right;
			setEnd(right.getEnd());
		}

		public void setNot(boolean isNot) {
			this.isNot = isNot;
		}
		
		public boolean isEquals(Exp node) {
			if (!(node instanceof Like)) {
				return false;
			}
			
			Like other = (Like)node;
			return isNot == other.isNot && Select.isEquals(left, other.left) 
					&& Select.isEquals(right, other.right);
		}
		
		public void getFields(List<FieldNode> resultList) {
			left.getFields(resultList);
			right.getFields(resultList);
		}
		
		public String toSPL() {
			String pattern = right.toSPL();
			if (right instanceof CommonNode) {
				Param p = getContext().getParam(pattern);
				if (p != null) {
					Object val = p.getValue();
					if (val instanceof String) {
						pattern = (String)val;
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("like" + mm.getMessage("function.paramTypeError"));
					}
				}
			}
			
			pattern = pattern.replace("\\", "\\\\");
			pattern = pattern.replace("*", "\\\\*");
			pattern = pattern.replace("?", "\\\\?");
			
			pattern = pattern.replace("[_]", "" + (char)18 + (char)19);
			pattern = pattern.replace("_", "?");
			pattern = pattern.replace("" + (char)18 + (char)19, "_");
			
			pattern = pattern.replace("[%]", "" + (char)18 + (char)19);
			pattern = pattern.replace("%", "*");
			pattern = pattern.replace("" + (char)18 + (char)19, "%");		
			pattern = pattern.replace("[[]", "[");
			
			if (isNot) {
				return "!like@c(" + left.toSPL() +"," + pattern + ")";
			} else {
				return "like@c(" + left.toSPL() +"," + pattern + ")";
			}
		}
	}
	
	class LogicExp extends Exp {
		private List<Exp> exps;

		public LogicExp(List<Exp> exps) {
			super(exps.get(0).getStart(), exps.get(exps.size() - 1).getEnd());
			this.exps = exps;
		}

		public boolean isEquals(Exp node) {
			if (!(node instanceof LogicExp)) {
				return false;
			}
			
			LogicExp other = (LogicExp)node;
			int count = exps.size();
			if (count != other.exps.size()) {
				return false;
			}
			
			for (int i = 0; i < count; ++i) {
				if (!Select.isEquals(exps.get(i), other.exps.get(i))) {
					return false;
				}
			}
			
			return true;
		}
		
		public List<And> splitAnd() {
			List<Exp> exps = this.exps;
			int start = 0;
			int count = exps.size();
			List<And> result = new ArrayList<And>();
			
			for (int i = 0; i < count; ++i) {
				Exp exp = exps.get(i);
				if (exp.isOr()) {
					start = 0;
					result.clear();
					break;
				} else if (exp.isAnd()) {
					int subCount = i - start;
					if (subCount == 0) {
						MessageManager mm = ParseMessage.get();
						throw new RQException(mm.getMessage("syntax.error") + exp.getPos());
					}
					
					List<Exp> list = new ArrayList<Exp>(subCount);
					List<FieldNode> fieldList = new ArrayList<FieldNode>();
					for (; start < i; ++start) {
						exp = exps.get(start);
						exp.getFields(fieldList);
						list.add(exp);
					}
					
					And and = new And(new LogicExp(list), fieldList);
					result.add(and);
					++start;
				}
			}
			
			if (start == 0) {
				List<FieldNode> fieldList = new ArrayList<FieldNode>();
				for (; start < count; ++start) {
					Exp exp = exps.get(start);
					exp.getFields(fieldList);
				}
				
				And and = new And(this, fieldList);
				result.add(and);
			} else {
				int subCount = count - start;
				if (subCount == 0) {
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + exps.get(count - 1).getPos());
				}
				
				List<Exp> list = new ArrayList<Exp>(subCount);
				List<FieldNode> fieldList = new ArrayList<FieldNode>();
				for (; start < count; ++start) {
					Exp exp = exps.get(start);
					exp.getFields(fieldList);
					list.add(exp);
				}
				
				And and = new And(new LogicExp(list), fieldList);
				result.add(and);
			}
			
			return result;
		}
		
		public void getFields(List<FieldNode> resultList) {
			for (Exp exp : exps) {
				exp.getFields(resultList);
			}
		}
		
		public String toSPL() {
			int size = exps.size();
			String spl = exps.get(0).toSPL();
			
			for (int i = 1; i < size; ++i) {
				spl += " ";
				spl += exps.get(i).toSPL();
			}

			return spl;
		}
		
		public String toSPL(int start, int next) {
			String spl = exps.get(start).toSPL();
			
			for (++start; start < next; ++start) {
				spl += " ";
				spl += exps.get(start).toSPL();
			}

			return spl;
		}
		
		public boolean splitJionExp(QueryBody leftTable, QueryBody rightTable, 
				List<Expression> leftExpList, List<Expression> rightExpList) {
			int size = exps.size();
			int equalIndex = -1;
			List<FieldNode> fieldList1 = new ArrayList<FieldNode>();
			List<FieldNode> fieldList2 = new ArrayList<FieldNode>();
			
			for (int i = 0; i < size; ++i) {
				Exp exp = exps.get(i);
				if (exp.isEqualOperator()) {
					equalIndex = i;
				} else if (equalIndex == -1) {
					exp.getFields(fieldList1);
				} else {
					exp.getFields(fieldList2);
				}
			}
			
			if (equalIndex == -1 || fieldList1.size() == 0 || fieldList2.size() == 0) {
				return false;
			}
			
			QueryBody table1 = fieldList1.get(0).getTable();
			QueryBody table2 = fieldList2.get(0).getTable();
			if (table1 == table2) {
				return false;
			}
			
			for (int i = 1; i < fieldList1.size(); ++i) {
				if (fieldList1.get(i).getTable() != table1) {
					return false;
				}
			}
			
			for (int i = 1; i < fieldList2.size(); ++i) {
				if (fieldList2.get(i).getTable() != table2) {
					return false;
				}
			}
			
			Context ctx = getContext();
			ICellSet cellSet = getCellSet();
			String spl = toSPL(0, equalIndex);
			Expression exp1 = new Expression(cellSet, ctx, spl);
			spl = toSPL(equalIndex + 1, size);
			Expression exp2 = new Expression(cellSet, ctx, spl);
			
			if (table1 == leftTable) {
				leftExpList.add(exp1);
				rightExpList.add(exp2);
			} else {
				leftExpList.add(exp2);
				rightExpList.add(exp1);
			}
			
			return true;
		}
		
		public boolean splitJionExp(List<QueryBody> tableList, int lastTable, 
				List<Expression> leftExpList, List<Expression> rightExpList) {
			int size = exps.size();
			int equalIndex = -1;
			List<FieldNode> fieldList1 = new ArrayList<FieldNode>();
			List<FieldNode> fieldList2 = new ArrayList<FieldNode>();
			
			for (int i = 0; i < size; ++i) {
				Exp exp = exps.get(i);
				if (exp.isEqualOperator()) {
					equalIndex = i;
				} else if (equalIndex == -1) {
					exp.getFields(fieldList1);
				} else {
					exp.getFields(fieldList2);
				}
			}
			
			if (equalIndex == -1 || fieldList1.size() == 0 || fieldList2.size() == 0) {
				return false;
			}
			
			Context ctx = getContext();
			ICellSet cellSet = getCellSet();
			QueryBody rightTable = tableList.get(lastTable);
			
			if (fieldList1.get(0).getTable() == rightTable) {
				for (int i = 1; i < fieldList1.size(); ++i) {
					if (fieldList1.get(i).getTable() != rightTable) {
						return false;
					}
				}
				
				for (int i = 0; i < fieldList2.size(); ++i) {
					if (fieldList2.get(i).getTable() == rightTable) {
						return false;
					}
				}
				
				String spl = toSPL(0, equalIndex);
				Expression exp1 = new Expression(cellSet, ctx, spl);
				spl = toSPL(equalIndex + 1, size);
				Expression exp2 = new Expression(cellSet, ctx, spl);
				leftExpList.add(exp2);
				rightExpList.add(exp1);
				return true;
			} else if (fieldList2.get(0).getTable() == rightTable) {
				for (int i = 1; i < fieldList2.size(); ++i) {
					if (fieldList2.get(i).getTable() != rightTable) {
						return false;
					}
				}
				
				for (int i = 0; i < fieldList1.size(); ++i) {
					if (fieldList1.get(i).getTable() == rightTable) {
						return false;
					}
				}
				
				String spl = toSPL(0, equalIndex);
				Expression exp1 = new Expression(cellSet, ctx, spl);
				spl = toSPL(equalIndex + 1, size);
				Expression exp2 = new Expression(cellSet, ctx, spl);
				leftExpList.add(exp1);
				rightExpList.add(exp2);
				return true;
			} else {
				return false;
			}
		}
	}

	class Not extends Exp {
		private Exp exp;

		public Not(int start, int end) {
			super(start, end);
		}

		public void setExp(Exp exp) {
			this.exp = exp;
			setEnd(exp.getEnd());
		}
		
		public boolean isLogicalOperator() {
			return true;
		}
		
		public boolean isEquals(Exp node) {
			if (!(node instanceof Not)) {
				return false;
			}
			
			Not other = (Not)node;
			return Select.isEquals(exp, other.exp);
		}
		
		public void getFields(List<FieldNode> resultList) {
			exp.getFields(resultList);
		}
		
		public String toSPL() {
			return "!(" + exp.toSPL() + ")";
		}
	}

	class Paren extends Exp {
		private Exp exp;
		
		public Paren(int start, int end, Exp exp) {
			super(start, end);
			this.exp = exp;
		}

		public boolean isEquals(Exp node) {
			if (!(node instanceof Paren)) {
				return false;
			}
			
			Paren other = (Paren)node;
			return Select.isEquals(exp, other.exp);
		}
		
		public void getFields(List<FieldNode> resultList) {
			exp.getFields(resultList);
		}
		
		public String toSPL() {
			return "(" + exp.toSPL() + ")";
		}
		
		public boolean splitJionExp(QueryBody leftTable, QueryBody rightTable, 
				List<Expression> leftExpList, List<Expression> rightExpList) {
			return exp.splitJionExp(leftTable, rightTable, leftExpList, rightExpList);
		}
		
		public boolean splitJionExp(List<QueryBody> tableList, int lastTable, 
				List<Expression> leftExpList, List<Expression> rightExpList) {
			return exp.splitJionExp(tableList, lastTable, leftExpList, rightExpList);
		}
	}

	class SubQuery extends Exp {
		private QueryBody query;

		public SubQuery(int start, int end, QueryBody query) {
			super(start, end);
			this.query = query;
		}
		
		public boolean isEquals(Exp node) {
			return node == this;
		}
		
		public String toSPL() {
			return query.toSPL();
		}
	}
	
	/**
	 * 构建Select对象
	 * @param tokens
	 * @param start 起始索引，包含
	 * @param next 结束索引，不包含
	 */
	public Select(SimpleSQL query, Token []tokens, int start, int next) {
		this.query = query;
		this.tokens = tokens;
		//this.start = start;
		//this.next = next;
		
		// 扫描select语句
		scanSelect(tokens, start, next);
	}
	
	public Select(Select parent, Token []tokens, int start, int next) {
		this.query = parent.query;
		this.select = parent;
		this.tokens = tokens;
		//this.start = start;
		//this.next = next;
		this.level = parent.getLevel() + 1;
		
		// 扫描select语句
		scanSelect(tokens, start, next);
	}
	
	public static boolean isEquals(Exp src, Exp dest) {
		if (src == dest) {
			return true;
		} else if (src == null || dest == null) {
			return false;
		} else {
			return src.isEquals(dest);
		}
	}
	
	public static boolean isEquals(String src, String dest) {
		return src.equalsIgnoreCase(dest);
	}
	
	int getLevel() {
		return level;
	}

	Context getContext() {
		return query.getContext();
	}
	
	ICellSet getCellSet() {
		return query.getCellSet();
	}
	
	public List<Exp> getGroupBy() {
		return groupBy;
	}

	public int getGatherCount() {
		return gatherList.size();
	}
	
	public DataStruct getDataStruct() {
		if (dataStruct == null) {
			ArrayList<String> nameList = new ArrayList<String>();
			for (Column column : columnList) {
				column.getResultField(nameList);
			}
		}
		
		return dataStruct;
	}
	
	public static int getFieldIndex(DataStruct ds, String fieldName) {
		String []names = ds.getFieldNames();
		for (int i = 0, fcount = names.length; i < fcount; ++i) {
			if (isEquals(names[i], fieldName)) {
				return i;
			}
		}
		
		return -1;
	}
	
	// 当前select为子查询，有别名
	public QueryBody getQueryBody(String tableName, String fieldName) {
		if (tableName != null) {
			if (Select.isEquals(aliasName, tableName)) {
				return this;
			} else {
				return null;
			}
		} else {
			DataStruct ds = getDataStruct();
			String []names = ds.getFieldNames();
			for (int i = 0, fcount = names.length; i < fcount; ++i) {
				if (Select.isEquals(names[i], fieldName)) {
					return this;
				}
			}
			
			return null;
		}
	}
	
	public QueryBody getQueryBody(String tableName) {
		if (Select.isEquals(aliasName, tableName)) {
			return this;
		} else {
			return null;
		}
	}
	
	QueryBody getFromTable(String tableName) {
		return from.getQueryBody(tableName);
	}
	
	QueryBody getFrom() {
		return from;
	}
	
	// 取字段表达式对应的表
	QueryBody getFromTable(String tableName, String fieldName) {
		QueryBody table = from.getQueryBody(tableName, fieldName);
		if (table != null) {
			return table;
		} else if (select != null) {
			return select.getFromTable(tableName, fieldName);
		} else {
			return null;
		}
	}
	
	WithItem getWithItem(String tableName) {
		return query.getWithItem(tableName);
	}
	
	private List<Exp> scanParam(Token []tokens, int start, int next, Part part) {
		List<Exp> exps = new ArrayList<Exp>();
		while (start < next) {
			int comma = Tokenizer.scanComma(tokens, start, next);
			if (comma < 0) {
				Exp param = scanExp(tokens, start, next, part);
				exps.add(param);
				break;
			} else {
				Exp param = scanExp(tokens, start, comma, part);
				exps.add(param);

				start = comma + 1;
				if (start == next) {
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("function.paramError") + tokens[comma].getPos());
				}
			}
		}

		return exps;
	}
		
	/**
	 * 扫描词数组，生成表达式
	 * @param tokens 词数组
	 * @param start 起始位置，包含
	 * @param next 结束位置，不包含
	 * @param part 表达式所在的位置
	 * @return Exp
	 */
	private Exp scanExp(Token []tokens, int start, int next, Part part) {
		List<Exp> exps = new ArrayList<Exp>();
		Exp exp = null;
		boolean hasNot = false;
		
		for (int i = start; i < next; ++i) {
			Token token = tokens[i];
			if (token.getType() == Tokenizer.IDENT) {
				int pos = i + 1;
				if (pos < next && tokens[pos].getType() == Tokenizer.LPAREN) {
					int end = Tokenizer.scanParen(tokens, pos, next);
					String fnName = token.getString();
					if (Tokenizer.isGatherFunction(fnName)) {
						if(fnName.equalsIgnoreCase("COUNT")) {
							if (tokens[pos + 1].isKeyWord("DISTINCT")) {
								fnName = "icount";
								pos++;
							} else if (tokens[pos + 1].equals("*")) {
								if (pos + 2 != end) {
									MessageManager mm = ParseMessage.get();
									throw new RQException(mm.getMessage("syntax.error") + tokens[pos + 1].getPos());
								}
								
								tokens[pos + 1].setString("1");
								//tokens[pos + 1].setOriginString("1");
							}
						}
						
						List<Exp> list = scanParam(tokens, pos + 1, end, part);
						if (list.size() != 1) {
							MessageManager mm = ParseMessage.get();
							throw new RQException(mm.getMessage("syntax.error") + tokens[pos].getPos());
						}
						
						Exp param = list.get(0);
						GatherNode gather = new GatherNode(i, end + 1, fnName, param);
						exp = addGatherNode(gather);
					} else {
						List<Exp> list = scanParam(tokens, pos + 1, end, part);
						exp = new Function(i, end + 1, fnName, list);
					}
					
					i = end;
				} else {
					if (pos < next && tokens[pos].getType() == Tokenizer.DOT) {
						pos++;
						if (pos == next) {
							MessageManager mm = ParseMessage.get();
							throw new RQException(mm.getMessage("syntax.error") + tokens[next - 1].getPos());
						}

						exp = new FieldNode(i, pos + 1, token.getString(), tokens[pos].getString(), part);
						i = pos;
					} else {
						exp = new FieldNode(i, pos, null, token.getString(), part);
					}
				}
			} else if (token.getType() == Tokenizer.LPAREN) {
				int end = Tokenizer.scanParen(tokens, i, next);
				if (tokens[i + 1].isKeyWord("SELECT")) {
					QueryBody query = scanQuery(tokens, i + 1, end);
					exp = new SubQuery(i, end + 1, query);
				} else {
					exp = scanExp(tokens, i + 1, end, part);
					exp = new Paren(i, end + 1, exp);
				}
				
				i = end;
			} else if (token.isKeyWord("AND")) {
				exp = new CommonNode(i, i + 1, "&&");
			} else if (token.isKeyWord("OR")) {
				exp = new CommonNode(i, i + 1, "||");
			} else if (tokens[i].isKeyWord("NOT")) {
				if (i + 1 == next) {
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + tokens[i].getPos());
				}
				
				exp = new Not(i, i + 1);
				hasNot = true;
			} else if (token.isKeyWord("LIKE")) {
				i++;
				if (i == next) {
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + tokens[i - 1].getPos());
				}

				int end = Tokenizer.scanLogicalOperator(tokens, i, next);
				if (end == -1) {
					end = next;
				}
				
				Like like = new Like(i, i + 1);
				exp = scanExp(tokens, i, end, part);
				like.setRight(exp);
				
				Exp last = removeLastNot(exps);
				if (last != null) {
					like.setNot(true);
				}
				
				last = removeLastLogic(exps, "LIKE");
				like.setLeft(last);
				exp = like;
				i = end - 1;
			} else if (token.isKeyWord("IN")) {
				i++;
				if (i == next) {
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + tokens[i - 1].getPos());
				}
				
				if (tokens[i].getType() != Tokenizer.LPAREN) {
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + tokens[i].getPos());
				}
				
				// f in(v1,v2,...)
				int end = Tokenizer.scanParen(tokens, i, next);
				In in = new In(i, end + 1);
				
				if (tokens[i + 1].isKeyWord("SELECT")) {
					QueryBody query = scanQuery(tokens, i + 1, end);
					in.setRight(query);
				} else {
					List<Exp> params = scanParam(tokens, i + 1, end, part);
					in.setRight(params);
				}
				
				Exp last = removeLastNot(exps);
				if (last != null) {
					in.setNot(true);
				}
				
				last = removeLastLogic(exps, "IN");
				in.setLeft(last);
				exp = in;
				i = end;
			} else if (token.isKeyWord("EXISTS")) {
				i++;
				if (i == next) {
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + tokens[i - 1].getPos());
				}
				
				if (tokens[i].getType() != Tokenizer.LPAREN) {
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + tokens[i].getPos());
				}
				
				// f in(v1,v2,...)
				int end = Tokenizer.scanParen(tokens, i, next);
				QueryBody query = scanQuery(tokens, i + 1, end);
				exp = new Exists(i - 1, end + 1, query);
				i = end;
			} else if (token.isKeyWord("BETWEEN")) {
				// xx between ... and ...
				i++;
				if (i == next) {
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + tokens[i - 1].getPos());
				}
				
				int end = Tokenizer.scanKeyWord("AND", tokens, i, next);
				if (end == -1) {
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + tokens[i].getPos());
				}

				Exp from = scanExp(tokens, i, end, part);
				
				i = end + 1;
				if (i == next) {
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + tokens[i - 1].getPos());
				}
				
				end = Tokenizer.scanLogicalOperator(tokens, i, next);
				if (end == -1) {
					end = next;
				}
				
				Exp to = scanExp(tokens, i, end, part);
				Exp not = removeLastNot(exps);
				Exp left = removeLastLogic(exps, "BETWEEN");
				exp = new Between(left, from, to, not);
				i = end - 1;
			} else if (token.isKeyWord("CASE")) {
				exp = scanCase(tokens, i, next, part);
				i = exp.getEnd() - 1;
			} else if (token.isKeyWord("IS")) {
				i++;
				if (i == next) {
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + tokens[i - 1].getPos());
				}
				
				Exp left = removeLastLogic(exps, "IS");
				if (tokens[i].isKeyWord("NULL")) {
					exp = new IsNull(i + 1, left, false);
				} else if (tokens[i].isKeyWord("NOT")) {
					i++;
					if (i == next || !tokens[i].isKeyWord("NULL")) {
						MessageManager mm = ParseMessage.get();
						throw new RQException(mm.getMessage("syntax.error") + tokens[i - 1].getPos());
					}

					exp = new IsNull(i + 1, left, true);
				}
			} else if (token.equals("=")) {
				exp = new CommonNode(i, i + 1, "==");
			} else if (token.equals("<")) {
				if (i + 1 < next && tokens[i + 1].equals(">")) {
					exp = new CommonNode(i, i + 2, "!=");
					i++;
				} else if (i + 1 < next && tokens[i + 1].equals("=")) {
					exp = new CommonNode(i, i + 2, "<=");
					i++;
				} else {
					exp = new CommonNode(i, i + 1, "<");
				}
			} else if (token.equals(">")) {
				if (i + 1 < next && tokens[i + 1].equals("=")) {
					exp = new CommonNode(i, i + 2, ">=");
					i++;
				} else {
					exp = new CommonNode(i, i + 1, ">");
				}
			} else {
				exp = new CommonNode(i, i + 1, token.getString());
			}
			
			exps.add(exp);
		}
		
		if (hasNot) {
			combineNot(exps);
		}
		
		if (exps.size() == 0) {
			return null;
		} else if (exps.size() == 1) {
			return exps.get(0);
		} else {
			return new LogicExp(exps);
		}
	}
	
	// 合并not运算符
	private void combineNot(List<Exp> exps) {
		for (int i = 0, size = exps.size(); i < size; ++i) {
			Exp exp = exps.get(i);
			if (!(exp instanceof Not)) {
				continue;
			}
			
			Not not = (Not)exp;
			int start = i + 1;
			int next = size;
			
			for (int j = i + 1; j < size; ++j) {
				// 找到后面and、or、not的位置
				exp = exps.get(j);
				if (exp.isLogicalOperator()) {
					next = j;
					break;
				}
			}
			
			int count = next - start;
			if (count == 1) {
				exp = exps.remove(start);
				not.setExp(exp);
			} else if (count > 1) {
				List<Exp> list = new ArrayList<Exp>(count);
				for (int j = 0; j < count; ++j) {
					list.add(exps.remove(start));
				}
				
				exp = new LogicExp(list);
				not.setExp(exp);
			} else {
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + not.getPos());
			}
			
			size -= count;
		}
	}
	
	private Exp removeLastNot(List<Exp> exps) {
		int last = exps.size() - 1;
		if (last >= 0 && exps.get(last) instanceof Not) {
			return exps.remove(last);
		} else {
			return null;
		}
	}
	
	// 删除逻辑运算的最右面的部分，用于和is、like、between、in组合成逻辑运算
	private Exp removeLastLogic(List<Exp> exps, String errorInfo) {
		int size = exps.size();
		int start = size - 1;
		
		for (; start >= 0; --start) {
			if (exps.get(start).isLogicalOperator()) {
				break;
			}
		}
		
		start++;
		int count = size - start;
		if (count == 1) {
			return exps.remove(start);
		} else if (count > 1) {
			List<Exp> list = new ArrayList<Exp>(count);
			for (int i = start; i < size; ++i) {
				list.add(exps.remove(start));
			}
			
			return new LogicExp(list);
		} else {
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + errorInfo);
		}
	}
	
	//找下一个WHEN 或 ELSE 或 END 的位置
	private int scanNextCaseKeyWord(Token[] tokens, int start, int next) {
		int pos = Tokenizer.scanKeyWord("WHEN", tokens, start, next);
		if (pos != -1) {
			return pos;
		}
		
		pos = Tokenizer.scanKeyWord("ELSE", tokens, start, next);
		if (pos != -1) {
			return pos;
		}
		
		return Tokenizer.scanKeyWord("END", tokens, start, next);
	}
	
	// case [xx] when v1 then x1 when v2 then x2 ... else x end
	// 转成集算器函数case(x,x1:y1,…,xk:yk;y)，返回end的位置
	private Case scanCase(Token []tokens, int start, int next, Part part) {
		// 跳过case
		int casePos = start++;
		if (start == next) {
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + tokens[start - 1].getPos());
		}
		
		Exp field = null;
		List<Exp> whenList = new ArrayList<Exp>();
		List<Exp> thenList = new ArrayList<Exp>();
		Exp defaultExp = null;
		
		if (!tokens[start].isKeyWord("WHEN")) {
			int pos = Tokenizer.scanKeyWord("WHEN", tokens, start + 1, next);
			if (pos == -1) {
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + tokens[start].getPos());
			}
			
			field = scanExp(tokens, start, pos, part);
			start = pos;
		}
		
		while (true) {
			if (start == next) {
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + tokens[start - 1].getPos());
			} else if (tokens[start].isKeyWord("WHEN")) {
				//先得到when
				start++;
				int pos = Tokenizer.scanKeyWord("THEN", tokens, start, next);
				if (pos == -1) {
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + tokens[start].getPos());
				}
				
				Exp when = scanExp(tokens, start, pos, part);
				start = pos + 1;
				
				// 再查找then
				pos = scanNextCaseKeyWord(tokens, start, next);
				if (pos == -1) {
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + tokens[start].getPos());
				}
				
				Exp then = scanExp(tokens, start, pos, part);
				whenList.add(when);
				thenList.add(then);
				start = pos;
			} else if (tokens[start].isKeyWord("ELSE")) {
				int pos = Tokenizer.scanKeyWord("END", tokens, start, next);
				if (pos == -1) {
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + tokens[start].getPos());
				}
				
				start++;
				defaultExp = scanExp(tokens, start, pos, part);
				start = pos;
			} else if (tokens[start].isKeyWord("END")) {
				return new Case(casePos, start + 1, field, whenList, thenList, defaultExp);
			} else {
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + tokens[start].getPos());
			}
		}
	}
	
	private QueryBody scanQuery(Token []tokens, int start, int next) {
		SetOperation operation = null;
		for (int i = start; i < next;) {
			Token token = tokens[i];
			if (token.getType() == Tokenizer.LPAREN) {
				i = Tokenizer.scanParen(tokens, i, next) + 1;
			} else if (token.isKeyWord("UNION")) {
				Select select = new Select(this, tokens, start, i);
				
				i++;
				if (i == next) {
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + tokens[i - 1].getPos());
				}
				
				SetOperation.Type type = SetOperation.Type.UNION;
				if (tokens[i].isKeyWord("ALL")) {
					type = SetOperation.Type.UNIONALL;
					i++;
				}
				
				if (operation == null) {
					operation = new SetOperation(type);
					operation.setLeft(select);
				} else {
					operation.setRight(select);
					SetOperation newOperation = new SetOperation(type);
					newOperation.setLeft(operation);
					operation = newOperation;
				}
				
				start = i;
			} else if (token.isKeyWord("INTERSECT")) {
				Select select = new Select(this, tokens, start, i);
				
				i++;
				if (i == next) {
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + tokens[i - 1].getPos());
				}
								
				if (operation == null) {
					operation = new SetOperation(SetOperation.Type.INTERSECT);
					operation.setLeft(select);
				} else {
					operation.setRight(select);
					SetOperation newOperation = new SetOperation(SetOperation.Type.INTERSECT);
					newOperation.setLeft(operation);
					operation = newOperation;
				}
				
				start = i;
			} else if (token.isKeyWord("MINUS")) {
				Select select = new Select(this, tokens, start, i);
				
				i++;
				if (i == next) {
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + tokens[i - 1].getPos());
				}
								
				if (operation == null) {
					operation = new SetOperation(SetOperation.Type.MINUS);
					operation.setLeft(select);
				} else {
					operation.setRight(select);
					SetOperation newOperation = new SetOperation(SetOperation.Type.MINUS);
					newOperation.setLeft(operation);
					operation = newOperation;
				}
				
				start = i;
			} else {
				i++;
			}
		}
		
		if (operation == null) {
			return new Select(this, tokens, start, next);
		} else {
			Select select = new Select(this, tokens, start, next);
			operation.setRight(select);
			return operation;
		}
	}
	
	/**
	 * 扫描SELECT语句
	 * @param tokens
	 * @param start 起始索引，包含
	 * @param next 结束索引，不包含
	 */
	private void scanSelect(Token[] tokens, int start, int next) {
		if (!tokens[start].isKeyWord("SELECT")) {
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + tokens[start - 1].getPos());
		}
		
		start++; // 跳过select
		if (start == next) {
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + tokens[start - 1].getPos());
		}
		
		// 扫描DISTINCT、TOP
		start = scanQuantifies(tokens, start, next);
		int colStart = start;
		int colEnd = -1;

		for (; start < next; ++start) {
			Token token = tokens[start];
			if (token.isKeyWord("FROM")) {
				colEnd = start;
				break;
			} else if (token.getType() == Tokenizer.LPAREN) {
				// 跳过()
				start = Tokenizer.scanParen(tokens, start, next);
			}
		}
		
		if (colEnd == -1) {
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + tokens[next - 1].getPos());
		}
		
		start = scanFrom(tokens, start, next);
		scanColumns(tokens, colStart, colEnd);
		
		if (start == next) {
			return;
		}
		
		String []keyWords = new String[] {"GROUP", "HAVING", "ORDER", "LIMIT", "OFFSET", "INTO"};
		if (tokens[start].isKeyWord("WHERE")) {
			start++;
			int end = Tokenizer.scanKeyWords(tokens, start, next, keyWords, 0);
			if (start == end) {
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + tokens[start - 1].getPos());
			}
			
			where = scanExp(tokens, start, end, Part.Where);
			start = end;
			
			if (end == next) {
				return;
			}
		}
		
		if (tokens[start].isKeyWord("GROUP")) {
			// 跳过group
			start++;
			if (start == next || !tokens[start].isKeyWord("BY")) {
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + tokens[start - 1].getPos());
			}
			
			// 跳过by
			start++;
			int end = Tokenizer.scanKeyWords(tokens, start, next, keyWords, 1);
			if (start == end) {
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + tokens[start - 1].getPos());
			}
			
			scanGroupBy(tokens, start, end);
			start = end;
			
			if (end == next) {
				return;
			}
		}
		
		if (tokens[start].isKeyWord("HAVING")) {
			start++;
			int end = Tokenizer.scanKeyWords(tokens, start, next, keyWords, 2);
			if (start == end) {
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + tokens[start - 1].getPos());
			}
			
			having = scanExp(tokens, start, end, Part.Having);
			start = end;
			
			if (end == next) {
				return;
			}
		}
		
		if (tokens[start].isKeyWord("ORDER")) {
			// 跳过ORDER BY
			start++;
			if (start == next || !tokens[start].equals("BY")) {
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + tokens[start - 1].getPos());
			}
			
			start++;
			int end = Tokenizer.scanKeyWords(tokens, start, next, keyWords, 3);
			scanOrderBy(tokens, start, end);
			start = end;
			
			if (end == next) {
				return;
			}
		}
		
		if (tokens[start].isKeyWord("LIMIT")) {
			start++;
			if (start == next) {
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + tokens[start - 1].getPos());
			}
			
			try {
				limit = Integer.parseInt(tokens[start].getString());
			} catch (NumberFormatException nfe) {
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + tokens[start].getString());
			}
			
			start++;
			if (start == next) {
				return;
			}
		}
		
		if (tokens[start].isKeyWord("OFFSET")) {
			start++;
			if (start == next) {
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + tokens[start - 1].getPos());
			}
			
			try {
				offset = Integer.parseInt(tokens[start].getString());
			} catch (NumberFormatException nfe) {
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + tokens[start].getString());
			}
			
			start++;
			if (start == next) {
				return;
			}
		}
		
		if (tokens[start].isKeyWord("INTO")) {
			String file = "";
			
			for (++start; start < next; ++start) {
				file += tokens[start].getOriginString();
				file += tokens[start].getSpaces();
			}

			file = file.trim();
			if(file.startsWith("\"") && file.endsWith("\"")) {
				file = file.substring(1, file.length() - 1);
			}
			
			intoFile = file;
		} else {
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + tokens[start].getPos());
		}
	}
		
	// top n
	private int scanQuantifies(Token[] tokens, int start, int next) {
		Token token = tokens[start];
		if (token.isKeyWord("DISTINCT")) {
			start++;
			if (start == next) {
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + token.getString());
			}
			
			distinct = true;
			token = tokens[start];
		}

		if (token.isKeyWord("TOP")) {
			start++;
			if (start == next) {
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + token.getString());
			}

			token = tokens[start];

			start++;
			if (start == next) {
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + token.getString());
			}

			try {
				limit = Integer.parseInt(token.getString());
			} catch (NumberFormatException nfe) {
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + token.getString());
			}
		}

		return start;
	}
	
	private void scanColumns(Token []tokens, int start, int next) {
		while (true) {
			int comma = Tokenizer.scanComma(tokens, start, next);
			if (comma < 0) {
				scanColumnItem(tokens, start, next);
				break;
			} else if (start < comma) {
				scanColumnItem(tokens, start, comma);
				start = comma + 1;
				
				if (start == next) {
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + tokens[start - 1].getPos());
				}
			} else {
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + tokens[start - 1].getPos());
			}
		}
	}
	
	private void scanColumnItem(Token[] tokens, int start, int next) {
		if (start + 1 == next && tokens[start].equals("*")) { // select * from ...
			Column column = new Column(this, null);
			columnList.add(column);
		} else if (start + 3 == next &&
				   tokens[start + 1].getType() == Tokenizer.DOT &&
				   tokens[start + 2].equals("*")) { // T.*
			Column column = new Column(this, tokens[start].getString());
			columnList.add(column);
		} else {
			// t.fk as f:...多字段外键可以分别起别名
			String aliasName = null;
			int expNext = next;
			int index = next - 1;
			
			if(index > start && tokens[index].getType() == Tokenizer.IDENT) {
				int prev = index - 1;
				if (tokens[prev].isKeyWord("AS")) {
					if (start == prev) {
						MessageManager mm = ParseMessage.get();
						throw new RQException(mm.getMessage("syntax.error") + tokens[prev].getPos());
					}
					
					expNext = prev;
					aliasName = tokens[index].getString();
				} else if (!tokens[prev].canHaveRightExp()) {
					expNext = index;
					aliasName = tokens[index].getString();
				} // 否则没有别名
			}
			
			Exp exp = scanExp(tokens, start, expNext, Part.Column);
			Column column = new Column(this, exp, aliasName);
			columnList.add(column);
		}
	}
	
	// 返回表的结束位置，不包含
	private int scanTableEnd(Token []tokens, int start, int next) {
		int keyCount = FROMKEYS.length;
		for(; start < next; ++start) {
			Token token = tokens[start];
			if (token.isComma()) {
				return start;
			} else if (token.isKeyWord()) {
				String id = token.getString();
				for (int k = 0; k < keyCount; ++k) {
					if (id.equals(FROMKEYS[k])) {
						return start;
					}
				}
			}
		}
		
		return next;
	}	

	private int scanQueryBody(Token []tokens, int start, int next) {
		if(tokens[start].getString().equals("{")) {
			int end = Tokenizer.scanBrace(tokens, start, next);
			StringBuffer expBuf = new StringBuffer();
			for(int i = start + 1; i < end; ++i) {
				expBuf.append(tokens[i].getOriginString());
				expBuf.append(tokens[i].getSpaces());
			}
			
			String expStr = expBuf.toString();
			Object obj = null;
			
			try {
				Context ctx = query.getContext();
				obj = new Expression(query.getCellSet(), ctx, expStr).calculate(ctx);
			} catch(Exception ex) {
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":scanFrom, 异常的表名(注意表名不能为关键字或以数字开头):"+expStr);
			}
			
			end++;
			String aliasName = null;
			
			if(end < next && tokens[end].isKeyWord("AS")) {
				end++;
				if (end == next || tokens[end].getType() != Tokenizer.IDENT) {
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + tokens[end - 1].getPos());
				}
				
				aliasName = tokens[end].getString();
				end++;
			} else if(end < next && tokens[end].getType() == Tokenizer.IDENT) {
				aliasName = tokens[end].getString();
				end++;
			}

			if(obj instanceof ICursor) {
				from = new TableNode(this, (ICursor)obj, expStr, aliasName);
			} else if(obj instanceof Sequence) {
				from = new TableNode(this, (Sequence)obj, expStr, aliasName);
			} else {
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":scanFrom, 不支持的表变量类型");
			}
			
			return end;
		} else if(tokens[start].getType() == Tokenizer.LPAREN) {
			int end = Tokenizer.scanParen(tokens, start, next);
			start++;
			
			if (tokens[start].isKeyWord("SELECT")) {
				from = scanQuery(tokens, start, end);
			} else {
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + tokens[start].getPos());
			}
			
			end++;
			if(end < next && tokens[end].isKeyWord("AS")) {
				end++;
				if (end == next || tokens[end].getType() != Tokenizer.IDENT) {
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + tokens[end - 1].getPos());
				}
				
				String aliasName = tokens[end].getString();
				from.setAliasName(aliasName);
				end++;
			} else if(end < next && tokens[end].getType() == Tokenizer.IDENT) {
				String aliasName = tokens[end].getString();
				from.setAliasName(aliasName);
				end++;
			}
			
			return end;
		} else {
			String tableName = "";
			int tableEnd = scanTableEnd(tokens, start, next);
			
			for (; start < tableEnd; ++start) {
				tableName += tokens[start].getOriginString();
				tableName += tokens[start].getSpaces();
			}

			tableName = tableName.trim();
			String aliasName = null;
			
			if (start < next && tokens[start].isKeyWord("AS")) {
				start++;
				if (start == next || tokens[start].getType() != Tokenizer.IDENT) {
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + tokens[start - 1].getPos());
				}
				
				aliasName = tokens[start].getString();
				start++;
			} else {
				int splitPos = tableName.lastIndexOf(' ');
				if(splitPos != -1 && tableName.indexOf('.', splitPos) == -1) {
					aliasName = tableName.substring(splitPos + 1);
					if(aliasName.equals(tokens[start - 1].getOriginString())) {
						tableName = tableName.substring(0, splitPos).trim();
					}
				}
			}
			
			if(tableName.startsWith("\"") && tableName.endsWith("\"")) {
				tableName = tableName.substring(1, tableName.length() - 1);
			}
			
			from = new TableNode(this, tableName, aliasName);
			return start;
		}
	}
	
	private int scanFrom(Token []tokens, int start, int next) {
		start++; // 跳过FROM
		if (start == next) {
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + tokens[start - 1].getPos());
		}
		
		start = scanQueryBody(tokens, start, next);
		QueryBody queryBody = from;
		
		while (start < next) {
			if (tokens[start].getType() == Tokenizer.COMMA) {
				// from t1, t2, ... where ...
				start++;
				if (start == next) {
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + tokens[start - 1].getPos());
				}
				
				Join join = new Join(this);
				join.setLeft(queryBody);
				
				start = scanQueryBody(tokens, start, next);
				join.setRight(from);
				queryBody = join;
			} else if (tokens[start].isKeyWord("JOIN")) {
				// from t1 join t2 on ...
				start++;
				if (start == next) {
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + tokens[start - 1].getPos());
				}
				
				Join join = new Join(this);
				join.setLeft(queryBody);
				
				start = scanQueryBody(tokens, start, next);
				join.setRight(from);
				queryBody = join;
				
				if (start < next && tokens[start].isKeyWord("ON")) {
					start = scanOn(tokens, start, next, join);
				}
			} else {
				String option;
				if (tokens[start].isKeyWord("INNER")) {
					option = null;
				} else if (tokens[start].isKeyWord("LEFT")) {
					option = "1";
				} else if (tokens[start].isKeyWord("FULL")) {
					option = "f";
				} else if (tokens[start].isKeyWord("RIGHT")) {
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + tokens[start - 1].getPos());
				} else if (tokens[start].isKeyWord("CROSS")) {
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + tokens[start - 1].getPos());
				} else {
					break;
				}
				
				start++;
				if (start == next || !tokens[start].isKeyWord("JOIN")) {
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + tokens[start - 1].getPos());
				}
				
				start++;
				if (start == next) {
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + tokens[start - 1].getPos());
				}
				
				Join join = new Join(this, option);
				join.setLeft(queryBody);
				
				start = scanQueryBody(tokens, start, next);
				join.setRight(from);
				queryBody = join;
				
				if (start < next && tokens[start].isKeyWord("ON")) {
					start = scanOn(tokens, start, next, join);
				}
			}
		}
		
		from = queryBody;
		return start;
	}
	
	private int scanOn(Token []tokens, int start, int next, Join join) {
		// 跳过on
		start++;
		if (start == next) {
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + tokens[start - 1].getPos());
		}
		
		int keyCount = FROMKEYS.length;
		int end = next;
		
		for(int i = start; i < next; ++i) {
			Token token = tokens[i];
			if (token.getType() == Tokenizer.LPAREN) { // 跳过()
				i = Tokenizer.scanParen(tokens, i, next);
			} else if (token.isKeyWord()) {
				String id = token.getString();
				for (int k = 0; k < keyCount; ++k) {
					if (id.equals(FROMKEYS[k])) {
						end = i;
						break;
					}
				}
			}
		}
		
		Exp exp = scanExp(tokens, start, end, Part.Join);
		join.setOn(exp);
		return end;
	}
	
	private void scanGroupBy(Token []tokens, int start, int next) {
		if (start == next) {
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + tokens[start - 1].getPos());
		}
		
		groupBy = new ArrayList<Exp>();
		while (true) {
			int comma = Tokenizer.scanComma(tokens, start, next);
			if (comma < 0) {
				Exp exp = scanExp(tokens, start, next, Part.Group);
				groupBy.add(exp);
				break;
			} else if (start < comma) {
				Exp exp = scanExp(tokens, start, comma, Part.Group);
				groupBy.add(exp);
				start = comma + 1;
				
				if (start == next) {
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + tokens[start - 1].getPos());
				}
			} else {
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + tokens[start - 1].getPos());
			}
		}
	}
	
	// ORDER BY ... ASC/DESC, ... 
	private void scanOrderBy(Token[] tokens, int start, int next) {
		if (start == next) {
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + tokens[start - 1].getPos());
		}
		
		orderBy = new ArrayList<SortItem>();
		while (true) {
			int comma = Tokenizer.scanComma(tokens, start, next);
			int end;
			if (comma < 0) {
				end = next - 1;
			} else {
				end = comma - 1;
			}
			
			String order = null;
			if (tokens[end].isKeyWord("ASC") || tokens[end].isKeyWord("DESC")) {
				order = tokens[end].getString();
			} else {
				end++;
			}
			
			Exp exp = scanExp(tokens, start, end, Part.Order);
			SortItem sortItem = new SortItem(exp, order);
			sortItem.setOrder(order);
			orderBy.add(sortItem);
			
			if (comma < 0) {
				break;
			} else if (start < comma) {
				start = comma + 1;
				if (start == next) {
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + tokens[start - 1].getPos());
				}
			} else {
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + tokens[start - 1].getPos());
			}
		}
	}
	
	private GatherNode addGatherNode(GatherNode gather) {
		for (GatherNode node : gatherList) {
			if (node.isEquals(gather)) {
				return node;
			}
		}
		
		gatherList.add(gather);
		gather.setFieldSeq(gatherList.size());
		return gather;
	}
	
	String getNextTableParamName() {
		return query.getNextTableParamName();
	}
	
	Column getColumn(String name) {
		for (Column column : columnList) {
			if (column.isEquals(name)) {
				return column;
			}
		}
		
		return null;
	}
	
	private Object doGroup(Object data) {
		int byCount = groupBy == null ? 0 : groupBy.size();
		int gatherCount = gatherList == null ? 0 : gatherList.size();
		if (byCount == 0 && gatherCount == 0) {
			return data;
		}
		
		Expression []byExps = null;
		String []byNames = null;
		Expression []gatherExps = null;
		String []gatherNames = null;
		Context ctx = getContext();
		ICellSet cellSet = getCellSet();
		
		if (byCount > 0) {
			byExps = new Expression[byCount];
			byNames = new String[byCount];
			
			for (int i = 0; i < byCount; ++i) {
				String expStr = groupBy.get(i).toSPL();
				byExps[i] = new Expression(cellSet, ctx, expStr);
				byNames[i] = "_" + (i + 1);
			}
		}
		
		if (gatherCount > 0) {
			gatherExps = new Expression[gatherCount];
			gatherNames = new String[gatherCount];
			
			for (int i = 0, q = byCount + 1; i < gatherCount; ++i, ++q) {
				String expStr = gatherList.get(i).toSPL();
				gatherExps[i] = new Expression(cellSet, ctx, expStr);
				gatherNames[i] = "_" + q;
			}
		}
		
		Sequence result;
		if (data instanceof Sequence) {
			Sequence sequence = (Sequence)data;
			result = sequence.groups(byExps, byNames, gatherExps, gatherNames, null, ctx);
		} else {
			ICursor cs = (ICursor)data;
			result = cs.groups(byExps, byNames, gatherExps, gatherNames, null, ctx);
		}
		
		if (having != null) {
			String expStr = having.toSPL(byCount);
			Expression exp = new Expression(cellSet, ctx, expStr);
			result = (Sequence)result.select(exp, null, ctx);
		}
		
		return result;
	}
	
	private String attachGroup(String spl) {
		int byCount = groupBy == null ? 0 : groupBy.size();
		int gatherCount = gatherList == null ? 0 : gatherList.size();
		if (byCount == 0 && gatherCount == 0) {
			return spl;
		}
		
		spl += ".groups(";
		if (byCount > 0) {
			for (int i = 0; i < byCount; ++i) {
				if (i > 0) {
					spl += ",";
				}
				
				spl += groupBy.get(i).toSPL();
				spl += ":_" + (i + 1);
			}
		}
		
		if (gatherCount > 0) {
			spl += ";";
			for (int i = 0, q = byCount + 1; i < gatherCount; ++i, ++q) {
				if (i > 0) {
					spl += ",";
				}
				
				spl += gatherList.get(i).toSPL();
				spl += ":_" + q;
			}
		}
		
		spl += ")";
		if (having != null) {
			spl += ".select(";
			spl += having.toSPL(byCount);
			spl += ")";
		}
		
		return spl;
	}

	private Object doSort(Object data) {
		if (orderBy == null) {
			return data;
		}
		
		Sequence sequence;
		if (data instanceof Sequence) {
			sequence = (Sequence)data;
		} else {
			sequence = ((ICursor)data).fetch();
			if (sequence == null) {
				return null;
			}
		}
		
		int byCount = groupBy == null ? 0 : groupBy.size();
		Context ctx = getContext();
		ICellSet cellSet = getCellSet();
		int count = orderBy.size();
		Expression []exps = new Expression[count];
		int []orders = new int[count];
		
		for (int i = 0; i < count; ++i) {
			SortItem sortItem = orderBy.get(i);
			String expStr = sortItem.getSortExp().toSPL(byCount);
			exps[i] = new Expression(cellSet, ctx, expStr);
			orders[i] = sortItem.getOrder();
		}
		
		return sequence.sort(exps, orders, null, null, ctx);
	}
	
	private String attachSort(String spl) {
		if (orderBy == null) {
			return spl;
		}
		
		spl += ".sort(";
		int byCount = groupBy == null ? 0 : groupBy.size();
		
		for (int i = 0; i < orderBy.size(); ++i) {
			if (i > 0) {
				spl += ",";
			}
			
			SortItem sortItem = orderBy.get(i);
			spl += sortItem.getSortExp().toSPL(byCount);
			spl += ":";
			spl += sortItem.getOrder();
		}
		
		spl += ")";
		return spl;
	}
	
	// 计算选出列
	private Object doNew(Object data) {
		Context ctx = getContext();
		ICellSet cellSet = getCellSet();
		int count = columnList.size();
		ArrayList<Expression> expList = new ArrayList<Expression>();
		ArrayList<String> nameList = new ArrayList<String>();
		
		for (int i = 0; i < count; ++i) {
			Column column = columnList.get(i);
			if (column.isAllFields()) {
				ArrayList<QueryBody> tableList = column.getAllTables();
				for (QueryBody table : tableList) {
					DataStruct ds = table.getDataStruct();
					int fcount = ds.getFieldCount() - table.getFileAttributeCount();
					String joinFieldName = table.getJoinFieldName();
					
					if (joinFieldName == null) {
						for (int f = 0; f < fcount; ++f) {
							Expression exp = new Expression(cellSet, ctx, "#" + (f + 1));
							expList.add(exp);
							nameList.add(ds.getFieldName(f));
						}
					} else {
						for (int f = 0; f < fcount; ++f) {
							Expression exp = new Expression(cellSet, ctx, joinFieldName + ".#" + (f + 1));
							expList.add(exp);
							nameList.add(ds.getFieldName(f));
						}
					}
				}
			} else {
				Expression exp = new Expression(cellSet, ctx, column.toSPL());
				expList.add(exp);
				nameList.add(column.getAliasName());
			}
		}
		
		int fcount = expList.size();
		String []names = new String[fcount];
		Expression []exps = new Expression[fcount];
		nameList.toArray(names);
		expList.toArray(exps);
		
		if (data instanceof Sequence) {
			Sequence sequence = (Sequence)data;
			sequence = sequence.newTable(names, exps, ctx);
			if (distinct) {
				for (int f = 0; f < fcount; ++f) {
					exps[f] = new Expression(cellSet, ctx, "#" + (f + 1));
				}
				
				return sequence.group(exps, "1", ctx);
			} else {
				return sequence;
			}
		} else { // ICursor
			ICursor cs = ((ICursor)data);
			cs.newTable(null, exps, names, null, ctx);
			
			if (distinct) {
				for (int f = 0; f < fcount; ++f) {
					exps[f] = new Expression(cellSet, ctx, "#" + (f + 1));
				}
				
				return cs.groups(exps, names, null, null, null, ctx);
			} else {
				return cs;
			}
		}
	}
	
	// 计算选出列
	private String attachNew(String spl) {
		spl += ".new(";
		int count = columnList.size();
		int resultFieldCount = 0;
		
		for (int i = 0; i < count; ++i) {
			Column column = columnList.get(i);
			if (column.isAllFields()) {
				ArrayList<QueryBody> tableList = column.getAllTables();
				for (QueryBody table : tableList) {
					DataStruct ds = table.getDataStruct();
					int fcount = ds.getFieldCount() - table.getFileAttributeCount();
					String joinFieldName = table.getJoinFieldName();
					
					if (joinFieldName == null) {
						for (int f = 0; f < fcount; ++f) {
							if (resultFieldCount > 0) {
								spl += ",";
							}
							
							resultFieldCount++;
							spl += "#" + (f + 1);
							spl += ":";
							spl += ds.getFieldName(f);
						}
					} else {
						for (int f = 0; f < fcount; ++f) {
							if (resultFieldCount > 0) {
								spl += ",";
							}
							
							resultFieldCount++;
							spl += joinFieldName + ".#" + (f + 1);
							spl += ":";
							spl += ds.getFieldName(f);
						}
					}
				}
			} else {
				if (resultFieldCount > 0) {
					spl += ",";
				}
				
				resultFieldCount++;
				spl += column.toSPL();
				spl += ":";
				spl += column.getAliasName();
			}
		}
		
		spl += ")";
		if (distinct) {
			if (isSingleValue()) {
				spl += ".id(#1)";
			} else {
				spl += ".group@1(#1";
				for (int f = 2; f <= resultFieldCount; ++f) {
					spl += ",#" + f;
				}
				
				spl += ")";
			}
		} else if (resultFieldCount == 1) {
			if (isSingleValue()) {
				spl += ".#1";
			} else {
				spl += ".(#1)";
			}
		}

		return spl;
	}

	private Object doInto(Object data) {
		if (intoFile == null || data == null) {
			return data;
		}
		
		String pathName = intoFile;
		if (pathName.startsWith("{")) {
			int end = pathName.lastIndexOf('}');
			if (end > 0) {
				String expStr = pathName.substring(1, end);
				Context ctx = getContext();
				ICellSet cellSet = getCellSet();
				Expression exp = new Expression(cellSet, ctx, expStr);
				Object obj = exp.calculate(ctx);
				pathName = Variant.toString(obj) + pathName.substring(end + 1);
			}
		}
		
		FileObject fo = new FileObject(pathName);
		int dotIndex = pathName.lastIndexOf('.');
		if (dotIndex != -1) {
			String fileType = pathName.substring(dotIndex).toLowerCase();
			if(fileType.equals(".btx")) {
				if (data instanceof Sequence) {
					fo.exportSeries((Sequence)data, "ab", null);
				} else {
					fo.exportCursor((ICursor)data, null, null, "ab", null, getContext());
				}
			} else if(fileType.equals(".txt")) {
				if (data instanceof Sequence) {
					fo.exportSeries((Sequence)data, "at", null);
				} else {
					fo.exportCursor((ICursor)data, null, null, "at", null, getContext());
				}
			} else if(fileType.equals(".csv")) {
				if (data instanceof Sequence) {
					fo.exportSeries((Sequence)data, "atc", null);
				} else {
					fo.exportCursor((ICursor)data, null, null, "atc", null, getContext());
				}
			} else if(fileType.equals(".xls")) {
				ExcelTool et = new ExcelTool(fo, true, false, false, null);
				try {
					if (data instanceof Sequence) {
						et.fileXlsExport((Sequence)data, null, null, "at", getContext());
					} else {
						et.fileXlsExport((ICursor)data, null, null, "at", getContext());
					}
				} catch (Exception e) {
					throw new RQException(e.getMessage(), e);
				} finally {
					try {
						et.close();
					} catch (IOException e) {
						throw new RQException(e.getMessage(), e);
					}
				}
			} else if(fileType.equals(".xlsx")) {
				ExcelTool et = new ExcelTool(fo, true, true, false, null);
				try {
					if (data instanceof Sequence) {
						et.fileXlsExport((Sequence)data, null, null, "at", getContext());
					} else {
						et.fileXlsExport((ICursor)data, null, null, "at", getContext());
					}
				} catch (Exception e) {
					throw new RQException(e.getMessage(), e);
				} finally {
					try {
						et.close();
					} catch (IOException e) {
						throw new RQException(e.getMessage(), e);
					}
				}
			} else if(fileType.equals(".json")) {
				Sequence sequence;
				if (data instanceof Sequence) {
					sequence = (Sequence)data;
				} else {
					sequence = ((ICursor)data).fetch();
				}
				
				String json = JSONUtil.toJSON(sequence);
				try {
					fo.write(json, "a");
				} catch (Exception e) {
					throw new RQException(e.getMessage(), e);
				}
			} else {
				if (data instanceof Sequence) {
					fo.exportSeries((Sequence)data, "at", null);
				} else {
					fo.exportCursor((ICursor)data, null, null, "at", null, getContext());
				}
			}
		} else {
			if (data instanceof Sequence) {
				fo.exportSeries((Sequence)data, "at", null);
			} else {
				fo.exportCursor((ICursor)data, null, null, "at", null, getContext());
			}
		}
		
		return null;
	}
	
	// 是否是全聚合并且只有一个选出列
	private boolean isSingleValue() {
		return groupBy == null && gatherList != null && gatherList.size() == 1;
	}
	
	void checkFileAttribute(String tableName, String fieldName) {
		if (fieldName.equals("_file") || fieldName.equals("_ext") || fieldName.equals("_date") || fieldName.equals("_size")) {
			QueryBody table = from;
			if (tableName != null) {
				table = from.getQueryBody(tableName);
			}
			
			if (table instanceof TableNode) {
				((TableNode)table).addFileAttribute(fieldName);
			}
		}
	}
	
	public Object getData() {
		Object data = from.getData(where);
		if (data == null) {
			return null;
		}

		data = doGroup(data);
		data = doSort(data);
		data = doNew(data);
		
		if (limit > 0) {
			if (offset > 0) {
				if (data instanceof Sequence) {
					Sequence sequence = (Sequence)data;
					sequence = sequence.get(offset + 1, offset + limit + 1);
					return doInto(sequence);
				} else { // ICursor
					ICursor cs = (ICursor)data;
					cs.skip(offset);
					Sequence sequence = cs.fetch(limit);
					return doInto(sequence);
				}
			} else {
				if (data instanceof Sequence) {
					Sequence sequence = (Sequence)data;
					sequence = sequence.get(1, limit + 1);
					return doInto(sequence);
				} else { // ICursor
					ICursor cs = (ICursor)data;
					Sequence sequence = cs.fetch(limit);
					return doInto(sequence);
				}
			}
		} else if (offset > 0) {
			if (data instanceof Sequence) {
				Sequence sequence = (Sequence)data;
				sequence = sequence.get(offset + 1, sequence.length() + 1);
				return doInto(sequence);
			} else { // ICursor
				ICursor cs = (ICursor)data;
				cs.skip(offset);
				return doInto(cs);
			}
		} else {
			return doInto(data);
		}
	}

	public Object getData(Exp where) {
		Object data = getData();
		
		if (where != null && data != null) {
			String expStr = where.toSPL();
			Context ctx = getContext();
			ICellSet cellSet = getCellSet();
			Expression exp = new Expression(cellSet, ctx, expStr);
			
			if (data instanceof Sequence) {
				Sequence sequence = (Sequence)data;
				return sequence.select(exp, null, ctx);
			} else { // ICursor
				ICursor cs = (ICursor)data;
				return cs.select(null, exp, null, ctx);
			}
		} else {
			return data;
		}
	}

	public String toSPL() {
		Object data = from.getData();
		if (data == null) {
			return "null";
		}
		
		String filterMain = null;
		Context ctx = getContext();
		
		if (where != null) {
			List<And> andList = where.splitAnd();
			String filter = null;
			ArrayList<QueryBody> tableList = new ArrayList<QueryBody>();
			from.getAllJoinTables(tableList);
			
			for (And and : andList) {
				String spl = and.getExp().toSPL();
				if (and.isTable(tableList, tableList.size() - 1)) {
					if (filter == null) {
						filter = spl;
					} else {
						filter += "&&" + spl;
					}
				} else {
					if (filterMain == null) {
						filterMain = spl;
					} else {
						filterMain += "&&" + spl;
					}
				}
			}
			
			if (filter != null) {
				ICellSet cellSet = select.getCellSet();
				Expression exp = new Expression(cellSet, ctx, filter);
				
				if (data instanceof ICursor) {
					ICursor cs = (ICursor)data;
					cs.select(null, exp, null, ctx);
					data = cs.fetch();
				} else {
					data = ((Sequence)data).select(exp, null, ctx);
				}
			}
		}
		
		Sequence sequence;
		if (data instanceof ICursor) {
			sequence = ((ICursor)data).fetch();
		} else {
			sequence = (Sequence)data;
		}

		String paramName = select.getNextTableParamName();
		ctx.setParamValue(paramName, sequence);
		String spl = paramName;
		if (filterMain != null) {
			spl += ".select(" + filterMain + ")";
		}

		spl = attachGroup(spl);
		spl = attachSort(spl);
		spl = attachNew(spl);
		
		if (limit == 1) {
			if (offset > 0) {
				spl += ".m(" + (offset + 1) + ")";
			} else {
				spl += ".m(1)";
			}
		} else if (limit > 0) {
			if (offset > 0) {
				spl += ".to(" + (offset + 1) + "," + (offset + limit) + ")";
			} else {
				spl += ".to(" + limit + ")";
			}
		} else if (offset > 0) {
			spl += ".to(" + (offset + 1) + ",)";
		}
		
		return spl;
	}
}
