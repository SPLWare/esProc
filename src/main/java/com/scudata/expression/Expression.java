package com.scudata.expression;

import java.util.ArrayList;
import java.util.List;

import com.scudata.array.IArray;
import com.scudata.array.ObjectArray;
import com.scudata.cellset.ICellSet;
import com.scudata.cellset.INormalCell;
import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.common.CellLocation;
import com.scudata.common.DBSession;
import com.scudata.common.Escape;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.common.Sentence;
import com.scudata.common.StringUtils;
import com.scudata.dm.Context;
import com.scudata.dm.Current;
import com.scudata.dm.DBObject;
import com.scudata.dm.DataStruct;
import com.scudata.dm.KeyWord;
import com.scudata.dm.Param;
import com.scudata.dm.ParamList;
import com.scudata.expression.fn.Call;
import com.scudata.expression.operator.*;
import com.scudata.resources.EngineMessage;
import com.scudata.util.EnvUtil;
import com.scudata.util.Variant;

/**
 * 表达式对象
 * @author WangXiaoJun
 *
 */
public class Expression {
	public static final Expression NULL = new Expression(new Constant(null));

	public static final byte TYPE_DB = 1; // 数据库连接
	public static final byte TYPE_FILE = 2; // 文件对象
	public static final byte TYPE_SEQUENCE = 3; // 序列
	public static final byte TYPE_TABLE = 4; // 序表
	public static final byte TYPE_CURSOR = 5; // 游标
	
	public static final byte TYPE_OTHER = 101; // 非以上返回值类型
	public static final byte TYPE_UNKNOWN = 102; // 无法确定返回值类型

	public static final boolean DoOptimize = true;

	private String expStr;
	private int location;
	private Node home;
	private ICellSet cs;

	// 是否可以计算全部的值，有赋值运算时只能一行行计算
	private boolean canCalculateAll;
	
	/**
	 * 构建表达式
	 * @param str 待解析的表达式
	 */
	public Expression(String str) {
		this(null, null, str);
	}
	
	/**
	 * 构建表达式
	 * @param ctx 计算上下文
	 * @param str 待解析的表达式
	 */
	public Expression(Context ctx, String str) {
		this(null, ctx, str);
	}

	/**
	 * 构建表达式
	 * @param cs 当前网格
	 * @param ctx 计算上下文
	 * @param str 待解析的表达式
	 */
	public Expression(ICellSet cs, Context ctx, String str) {
		this(cs, ctx, str, DoOptimize, true);
	}

	/**
	 * 构建表达式
	 * @param cs 当前网格
	 * @param ctx 计算上下文
	 * @param str 待解析的表达式
	 * @param opt 是否做优化
	 */
	public Expression(ICellSet cs, Context ctx, String str, boolean opt) {
		this(cs, ctx, str, opt, true);
	}

	/**
	 * 构建表达式
	 * @param cs 当前网格
	 * @param ctx 计算上下文
	 * @param str 待解析的表达式
	 * @param opt 是否做优化
	 * @param doMacro 是否做宏替换
	 */
	public Expression(ICellSet cs, Context ctx, String str, boolean opt, boolean doMacro) {
		this.cs = cs;
		expStr = doMacro ? replaceMacros(str, cs, ctx) : str;
		
		if (expStr != null) {
			try {
				create(cs, ctx);
			} catch (RQException re) {
				MessageManager mm = EngineMessage.get();
				re.setMessage(mm.getMessage("Expression.inExp", expStr) + re.getMessage());
				throw re;
			}
		}
		
		if (home == null) {
			home = new Constant(null);
		} else {
			home.checkValidity();
			if (opt) {
				home = home.optimize(ctx);
			}
		}
		
		canCalculateAll = home.canCalculateAll();
	}

	/**
	 * 构建表达式
	 * @param node 表达式根节点
	 */
	public Expression(Node node) {
		home = node;
		canCalculateAll = node.canCalculateAll();
	}
	
	/**
	 * 取得节点
	 * @return Node
	 */
	public Node getHome() {
		return home;
	}

	/**
	 * 返回表达式是否是常数表达式
	 * @return true：是，false：不是
	 */
	public boolean isConstExpression() {
		return home instanceof Constant;
	}
	
	/**
	 * 运算表达式
	 * @param ctx 运算报表时的上下文环境变量
	 * @return 运算结果
	 */
	public Object calculate(Context ctx) {
		return home.calculate(ctx);
	}

	/**
	 * 计算出引用的单元格，不是取单元格的值，如果表达式不是单元格引用则返回空
	 * @param ctx 计算上下文
	 * @return INormalCell
	 */
	public INormalCell calculateCell(Context ctx) {
		return home.calculateCell(ctx);
	}

	/**
	 * 对表达式执行赋值操作
	 * @param value Object
	 * @param ctx Context
	 */
	public void assign(Object value, Context ctx) {
		home.assign(value, ctx);
	}

	/**
	 * 取表达式串
	 * @return String
	 */
	public String toString() {
		return expStr;
	}

	/**
	 * 取标识符名字，用单引号括起来的去掉单引号
	 * @return String
	 */
	public String getIdentifierName() {
		if (expStr != null) {
			int end = expStr.length() - 1;
			if (end > 0 && expStr.charAt(0) == '\'' && expStr.charAt(end) == '\'') {
				return expStr.substring(1, end); // Escape.remove()
			}
		}

		return expStr;
	}
	
	/**
	 * 取表达式对应的字段名，用于根据表达式自动生成字段名
	 * @return
	 */
	public String getFieldName() {
		return getFieldName(null);
	}
	
	/**
	 * 取表达式对应的字段名，用于new之类的函数省略字段名的时候
	 * A.f变成f，#1变成f
	 * @param ds 源表的数据结构，可空
	 */
	public String getFieldName(DataStruct ds) {
		if (home instanceof DotOperator) {
			Node right = home.getRight();
			if (right instanceof FieldRef) {
				String name = ((FieldRef)right).getName();
				if (name.charAt(0) == '\'' && name.charAt(name.length() - 1) == '\'') {
					return name.substring(1, name.length() - 1);
				} else {
					return name;
				}
			} else {
				return expStr;
			}
		} else if (ds != null && home instanceof FieldId) {
			int c = ((FieldId)home).getFieldIndex();
			if (c < ds.getFieldCount()) {
				return ds.getFieldName(c);
			} else {
				return expStr;
			}
		} else {
			return getIdentifierName();
		}
	}
	
	/**
	 * 取表达式对应的字段索引
	 * @param ds 数据结构
	 * @return 字段索引，如果不是字段表达式则返回-1
	 */
	public int getFieldIndex(DataStruct ds) {
		int index = ds.getFieldIndex(expStr);
		if (index != -1) {
			return index;
		} else {
			return getFieldIndex(home, ds);
		}
	}
	
	private static int getFieldIndex(Node home, DataStruct ds) {
		if (home instanceof DotOperator) {
			Node left = home.getLeft();
			if (left instanceof DotOperator || getFieldIndex(left, ds) != -1) {
				return -1;
			}
			
			Node right = home.getRight();
			if (right instanceof FieldRef) {
				String fieldName = ((FieldRef)right).getName();
				return ds.getFieldIndex(fieldName);
			}
		} else if (home instanceof UnknownSymbol) {
			String fieldName = ((UnknownSymbol)home).getName();
			return ds.getFieldIndex(fieldName);
		} else if (home instanceof FieldId) {
			int c = ((FieldId)home).getFieldIndex();
			if (c < ds.getFieldCount()) {
				return c;
			}
		}
		
		return -1;
	}

	/**
	 * 表达式优化
	 * @param ctx 运算报表时的上下文环境变量
	 */
	public void optimize(Context ctx) {
		home = home.optimize(ctx);
	}
	
	/**
	 * 表达式深度优化（包括单元格和参数引用）
	 * @param ctx 运算报表时的上下文环境变量
	 */
	public void deepOptimize(Context ctx) {
		home = home.deepOptimize(ctx);
	}

	/**
	 * 返回是否包含指定参数
	 * @param name String
	 * @return boolean
	 */
	public boolean containParam(String name) {
		if (name == null || name.length() == 0) {
			return false;
		}

		return home.containParam(name);
	}

	/**
	 * 查找表达式中用到的参数
	 * @param resultList ParamList 生成新的参数对象
	 */
	public void getUsedParams(Context ctx, ParamList resultList) {
		home.getUsedParams(ctx, resultList);
	}
	
	/**
	 * 查找表达式中可能用到的字段，可能取得不准确或者包含多个表的
	 * @param ctx
	 * @param resultList
	 */
	public void getUsedFields(Context ctx, List<String> resultList) {
		home.getUsedFields(ctx, resultList);
	}
	
	/**
	 * 如果表达式是字段或字段序列则返回字段名数组，否则返回null
	 * @param exp 表达式
	 * @return 字段名数组
	 */
	public String[] toFields() {
		Node node = getHome();
		
		// 单字段
		if (node instanceof UnknownSymbol) {
			String [] res = new String[1];
			res[0] = ((UnknownSymbol) node).getName();
			return res;
		}
		
		if (!(node instanceof ValueList))
			return null;
		
		ValueList firList = (ValueList)node;

		//SymbolParam 
		IParam param = firList.getParam();
		if (null == param)
			return null;
		
		int count = param.getSubSize();
		if (0 >= count)
			return null;
		
		String [] res = new String[count];
		for (int i = 0; i < count; i++) {
			res[i] = param.getSub(i).getLeafExpression().getIdentifierName();
		}
		
		return res;		
	}
	
	/**
	 * 取表达式用到的单元格
	 * @param List<INormalCell> resultList
	 */
	public void getUsedCells(List<INormalCell> resultList) {
		home.getUsedCells(resultList);
	}

	/**
	 * 返回表达式最后一个优先级比'.'高的节点的返回值类型
	 * @param ctx Context
	 * @return byte
	 */
	public byte getExpValueType(Context ctx) {
		Node right = home;
		while (right != null && right.getPriority() < Node.PRI_SUF) { // .的优先级
			right = right.getRight();
		}

		return right == null ? TYPE_OTHER : right.calcExpValueType(ctx);
	}

	private void create(ICellSet cs, Context ctx) {
		int len = expStr.length();
		int inBrackets = 0;
		Node preNode = null;

		while (location < len) {
			char c = expStr.charAt(location);
			if (Character.isWhitespace(c)) {
				location++;
				continue;
			}

			Node node = null;
			switch (c) {
			case '(':
				if (preNode != null && !(preNode instanceof Operator)) { //A1(2)
					node = new ElementRef();
					((Function)node).setParameter(cs, ctx, scanParameter());
					break;
				} else if (preNode instanceof DotOperator) { // A1.(exp)
					node = new Calc();
					((Function)node).setParameter(cs, ctx, scanParameter());
					break;
				} else { // 1 * (2 + 3)
					inBrackets++;
					location++;
					continue;
				}
			case ')':
				inBrackets--;
				if (inBrackets < 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("(,)" + mm.getMessage("Expression.illMatched"));
				}
				location++;
				continue;
			case '+':
				location++;
				if (location < len && expStr.charAt(location) == '+') { // ++
					node = new MemAdd();
					location++;
				} else if (location < len && expStr.charAt(location) == '=') { // +=
					node = new AddAssign();
					location++;
				} else {
					if (preNode != null && !(preNode instanceof Operator)) {
						node = new Add();
					} else {
						node = new Plus();
					}
				}
				break;
			case '-':
				location++;
				if (location < len && expStr.charAt(location) == '-') { // --
					node = new MemSubtract();
					location++;
				} else if (location < len && expStr.charAt(location) == '=') { // -=
					node = new SubtractAssign();
					location++;
				} else {
					if (preNode != null && !(preNode instanceof Operator)) {
						node = new Subtract();
					} else {
						node = new Negative();
					}
				}
				break;
			case '*':
				if (preNode == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("\"*\"" + mm.getMessage("operator.missingLeftOperation"));
				}
				
				location++;
				if (location < len && expStr.charAt(location) == '*') { // **
					node = new MemMultiply();
					location++;
				} else if (location < len && expStr.charAt(location) == '=') { // *=
					node = new MultiplyAssign();
					location++;
				} else {
					node = new Multiply();
				}
				break;
			case '/':
				if (preNode == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("\"*\"" + mm.getMessage("operator.missingLeftOperation"));
				}

				location++;
				if (location < len && expStr.charAt(location) == '/') { // //
					node = new MemDivide();
					location++;
				} else if (location < len && expStr.charAt(location) == '=') { // /=
					node = new DivideAssign();
					location++;
				} else {
					node = new Divide();
				}
				break;
			case '%':
				location++;
				if (location < len && expStr.charAt(location) == '%') { // %%
					node = new MemMod();
					location++;
				} else if (location < len && expStr.charAt(location) == '=') { // %=
					node = new ModAssign();
					location++;
				} else {
					node = new Mod();
				}
				break;
			case '=':
				location++;
				if (location < len && expStr.charAt(location) == '=') {
					node = new Equals();
					location++;
				} else {
					node = new Assign();
				}
				break;
			case '!':
				location++;
				if (location < len && expStr.charAt(location) == '=') {
					node = new NotEquals();
					location++;
				} else {
					node = new Not();
				}
				break;
			case '>':
				location++;
				if (location < len && expStr.charAt(location) == '=') {
					node = new NotSmaller();
					location++;
				} else {
					node = new Greater();
				}
				break;
			case '<':
				location++;
				if (location < len && expStr.charAt(location) == '=') {
					node = new NotGreater();
					location++;
				} else {
					node = new Smaller();
				}
				break;
			case '&':
				location++;
				if (location < len && expStr.charAt(location) == '&') {
					node = new And();
					location++;
				} else if (location < len && expStr.charAt(location) == '=') { // &=
					node = new UnionAssign();
					location++;
				} else {
					node = new Union(); // 并列
				}
				break;
			case '^':
				location++;
				if (location < len && expStr.charAt(location) == '=') { // ^=
					node = new ISectAssign();
					location++;
				} else {
					node = new ISect();
				}
				break;
			case '|':
				location++;
				if (location < len && expStr.charAt(location) == '|') {
					node = new Or();
					location++;
				} else if (location < len && expStr.charAt(location) == '=') { // |=
					node = new ConjAssign();
					location++;
				} else {
					node = new Conj();
				}
				break;
			case '\\':
				location++;
				if (location < len && expStr.charAt(location) == '\\') {
					node = new MemIntDivide();
					location++;
				} else if (location < len && expStr.charAt(location) == '=') { // \=
					node = new IntDivideAssign();
					location++;
				} else {
					node = new Diff();
				}
				break;
			case ',':
				node = new Comma();
				location++;
				break;
			case '.':
				if (preNode == null || preNode instanceof Operator) {
					location++;
					String id = '.' + scanId();
					Object obj = Variant.parse(id);
					if (obj instanceof String) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(mm.getMessage("Expression.unknownExpression") + id);
					}
					node = new Constant(obj);
				} else {
					node = new DotOperator(); // series.fun  record.field
					location++;
				}
				break;
			case '"':
				int dqmatch = Sentence.scanQuotation(expStr, location);
				if (dqmatch == -1) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("\"" + mm.getMessage("Expression.illMatched"));
				}
				String str = expStr.substring(location + 1, dqmatch);
				location = dqmatch + 1;
				node = new Constant(Escape.remove(str));
				break;
			case '\'':
				int qmatch = Sentence.scanQuotation(expStr, location);
				if (qmatch == -1) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("'" + mm.getMessage("Expression.illMatched"));
				}
				String strID = expStr.substring(location + 1, qmatch);
				location = qmatch + 1;

				if (preNode instanceof DotOperator) {
					node = new FieldRef(strID);
				} else {
					node = new UnknownSymbol(strID);
				}
				break;
			case '[':
				int match = Sentence.scanBracket(expStr, location);
				if (match == -1) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("[,]" + mm.getMessage("Expression.illMatched"));
				}

				if (preNode == null || preNode instanceof Operator) {
					node = new ValueList(); // 序列[elm1, elm2, ...]
				} else { // series[2] field[2] series.field[2] series[2].field H[hc] F[hc]
					node = new Move();
				}

				((Function)node).setParameter(cs, ctx, expStr.substring(location + 1, match));
				location = match + 1;
				break;
			case '{':
				match = Sentence.scanBrace(expStr, location);
				if (match == -1) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("{,}" + mm.getMessage("Expression.illMatched"));
				}

				if (preNode == null || preNode instanceof Operator) {
					//{} 记录表达式
					node = new CreateRecord();
				} else {
					// 排号运算
					node = new Moves();
				}
				
				((Function)node).setParameter(cs, ctx, expStr.substring(location + 1, match));
				location = match + 1;
				break;
			default:
				node = createNode(cs, ctx, preNode);
			}

			// x y	将字串常量x与y合并
			if (preNode instanceof Constant && node instanceof Constant &&
				((Constant)preNode).append((Constant)node)) {
				continue;
			}

			node.setInBrackets(inBrackets);
			preNode = node;
			if (home == null) {
				home = node;
			} else {
				Node right = home;
				Node parent = null;

				while (right != null && right.getPriority() < node.getPriority()) {
					parent = right;
					right = right.getRight();
				}
				node.setLeft(right);
				if (parent != null) {
					parent.setRight(node);
				} else {
					home = node;
				}
			}
		}

		if (inBrackets > 0) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("(,)" + mm.getMessage("Expression.illMatched"));
		}
	}

	private Node createNode(ICellSet cs, Context ctx, Node preNode) {
		String id = scanId();
		int idLen = id.length();
		if (idLen < 1) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Expression.unknownExpression") + expStr.charAt(location));
		}

		if (KeyWord.isCurrentElement(id)) {
			return new CurrentElement(); // ~, A.~
		} else if (KeyWord.isIterateParam(id)) {
			return new VarParam(ctx.getIterateParam()); // ~~
		} else if (KeyWord.isCurrentSeq(id)) {
			return new CurrentSeq(); // #, A.#
		} else if (KeyWord.isFieldId(id)) {
			return new FieldId(id); // #n, r.#n
		} else if (KeyWord.isCurrentCellSeq(id)) {
			return new CurrentCellSeq(); // #@
		}

		//形如series.select(...),series.field
		if (preNode instanceof DotOperator) {
			return createMemberNode(cs, id, ctx);
		}

		if (id.startsWith(KeyWord.CURRENTSEQ)) { // #A1 #F
			if (cs instanceof PgmCellSet) {
				INormalCell cell = cs.getCell(id.substring(1));
				if (cell != null) {
					return new ForCellCurSeq((PgmCellSet)cs, cell.getRow(), cell.getCol());
				}
			}
			
			return new FieldFuzzyRef(id.substring(1));
		}

		if (id.equals("$") && isNextChar('[')) { // 字符串
			int index = expStr.indexOf('[', location);
			int match = Sentence.scanBracket(expStr, index);
			if (match != -1) {
				location = match + 1;
				return new Constant(Escape.remove(expStr.substring(index + 1, match).trim()));
			}
		}

		if (cs != null) {
			INormalCell cell = cs.getCell(id);
			if (cell != null) return new CSVariable(cell);

			if (KeyWord.isCurrentCell(id)) return new CurrentCell(cs);

			if (KeyWord.isSubCodeBlock(id) && cs instanceof PgmCellSet) {
				return new SubVal((PgmCellSet)cs);
			}
		}

		if (KeyWord.isArg(id)) {
			return new ArgNode(id);
		}

		//参数，变量
		Param var = EnvUtil.getParam(id, ctx);
		if (var != null) {
			// connection由参数传入
			Object val = var.getValue();
			if (val instanceof DBSession) {
				return new Constant(new DBObject((DBSession)val));
			}
			
			byte kind = var.getKind();
			switch (kind) {
			case Param.VAR:
				return new VarParam(var);
			case Param.ARG:
				return new ArgParam(var);
			default:
				return new ConstParam(id, var.getValue());
			}
		}
		
		// 优先解析成变量，如果和函数重名则用function@引用函数
		// function() or function@opt()
		if (isNextChar('(')) {
			int atIdx = id.indexOf(KeyWord.OPTION);
			String fnName = id;
			String fnOpt = null;
			
			if (atIdx != -1) {
				fnName = id.substring(0, atIdx);
				fnOpt = id.substring(atIdx + 1);
			}

			if (FunctionLib.isFnName(fnName)) {
				Function fn = FunctionLib.newFunction(fnName);
				fn.setOption(fnOpt);
				fn.setParameter(cs, ctx, scanParameter());
				return fn;
			}
			
			String dfx = FunctionLib.getDFXFunction(fnName);
			if (dfx != null) {
				Function fun = new Call();
				if (fnOpt == null) {
					fun.setOption("r");
				} else {
					fun.setOption("r" + fnOpt);
				}
				
				String param = scanParameter();
				if (param == null || param.length() == 0) {
					fun.setParameter(cs, ctx, '"' + dfx + '"');
				} else {
					fun.setParameter(cs, ctx, '"' + dfx + '"' + ',' + param);
				}
				
				return fun;
			}
		}
		
		if (ctx != null) {
			DBSession dbs = ctx.getDBSession(id);
			if (dbs != null) {
				return new Constant(new DBObject(dbs));
			}
		}

		// 浮点数的判断
		if (isNextChar('.') && isNumber(id)) {
			int prevPos = location++;
			if (isNextChar('(')) { // n.()
				location = prevPos;
			} else {
				Object obj = Variant.parse(id + '.' + scanId());
				if (obj instanceof String) {
					location = prevPos;
				} else {
					return new Constant(obj);
				}
			}
		}

		Object value = Variant.parse(id);
		if (value instanceof String) { // 字段名;
			return new UnknownSymbol( (String) value);
		} else {
			return new Constant(value);
		}
	}

	//解析序列函数及列,运算列
	private Node createMemberNode(ICellSet cs, String id, Context ctx) {
		if (isNextChar('(')) {
			int atIdx = id.indexOf(KeyWord.OPTION);
			String fnName = id;
			String fnOpt = null;
			
			// A.@m(x)
			if (atIdx == 0) {
				fnOpt = id.substring(1);
				Calc calc = new Calc();
				calc.setOption(fnOpt);
				calc.setParameter(cs, ctx, scanParameter());
				return calc;
			}
			
			if (atIdx != -1) {
				fnName = id.substring(0, atIdx);
				fnOpt = id.substring(atIdx + 1);
			}

			if (FunctionLib.isMemberFnName(fnName)) {
				MemberFunction mfn = FunctionLib.newMemberFunction(fnName);
				mfn.setOption(fnOpt);
				mfn.setParameter(cs, ctx, scanParameter());
				return mfn;
			}
		}

		if (id.startsWith(KeyWord.CURRENTSEQ)) { // ~.#F
			return new FuzzyFieldRef(id.substring(1));
		} else {
			return new FieldRef(id);
		}
	}

	// 返回下一个字符是否是c，空字符跳过
	private boolean isNextChar(char c) {
		int len = expStr.length();
		for (int i = location; i < len; ++i) {
			if (expStr.charAt(i) == c) {
				return true;
			} else if (!Character.isWhitespace(expStr.charAt(i))) {
				return false;
			}
		}
		return false;
	}

	private boolean isNumber(String num) {
		int length = num.length();
		for (int i = 0; i < length; ++i) {
			char c = num.charAt(i);
			if (c < '0' || c > '9') return false;
		}
		return true;
	}

	private String scanId() {
		int len = expStr.length();
		int begin = location;
		
		while (location < len) {
			char c = expStr.charAt(location);
			if (KeyWord.isSymbol(c)) {
				break;
			} else {
				location++;
			}
		}
		
		return expStr.substring(begin, location);
	}

	private String scanParameter() {
		int len = expStr.length();
		while (location < len) {
			char c = expStr.charAt(location);
			if (Character.isWhitespace(c)) {
				location++;
			} else {
				break;
			}
		}
		
		if (location == len) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Expression.missingParam"));
		}
		
		char c = expStr.charAt(location);
		if (c != '(') {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Expression.missingParam"));
		}
		
		int match = scanParenthesis(expStr, location);
		if (match == -1) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("(,)" + mm.getMessage("Expression.illMatched"));
		}
		
		String param = expStr.substring(location + 1, match);
		location = match + 1;
		return param;
	}

	/**
	 * 进行宏替换
	 * @param text String ${macroName}, $(cellName), $(), $C$1, ~$r
	 * @param cs ICellSet
	 * @param ctx Context
	 * @return String
	 */
	public static String replaceMacros(String text, ICellSet cs, Context ctx) {
		if (text == null) return null;

		int len = text.length();
		StringBuffer newStr = null;

		PgmCellSet pcs = null;
		if (cs instanceof PgmCellSet) pcs = (PgmCellSet)cs;

		int idx = 0;
		while(idx < len) {
			char c = text.charAt(idx);
			// 字符串中的$不做替换
			if (c == '\'' || c == '\"') {
				int match = Sentence.scanQuotation(text, idx);
				if (match < 0) {
					if (newStr != null) newStr.append(c);
					idx += 1;
				} else {
					if (newStr != null) newStr.append(text.substring(idx, match + 1));
					idx = match + 1;
				}
			} else if (KeyWord.isSymbol(c)) {
				if (newStr != null) newStr.append(c);
				idx += 1;
			} else {
				int last = KeyWord.scanId(text, idx + 1);
				char lc = text.charAt(last - 1);
				if (last < len && lc == '$') { // str${}, str$()
					char nc = text.charAt(last);
					if (nc == '{') {
						int match = Sentence.scanBrace(text, last);
						if (match == -1) { // 括号不匹配
							MessageManager mm = EngineMessage.get();
							throw new RQException("{,}" + mm.getMessage("Expression.illMatched"));
						}

						if (newStr == null) {
							newStr = new StringBuffer(len + 80);
							newStr.append(text.substring(0, idx));
						}

						newStr.append(text.substring(idx, last - 1));
						newStr.append(getMacroValue(text.substring(last + 1, match), cs, ctx));
						idx = match + 1;
					} else if (nc == '(') {
						// $(c)  $()
						int match = scanParenthesis(text, last);
						if (match == -1) { // 括号不匹配
							MessageManager mm = EngineMessage.get();
							throw new RQException("(,)" + mm.getMessage("Expression.illMatched"));
						}

						if (pcs == null) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("$()" + mm.getMessage("Expression.missingCs"));
						}

						String strCell = text.substring(last + 1, match).trim();
						String retStr = pcs.getMacroReplaceString(strCell);

						if (newStr == null) {
							newStr = new StringBuffer(len + 80);
							newStr.append(text.substring(0, idx));
						}

						newStr.append(text.substring(idx, last - 1));
						newStr.append(retStr);
						idx = match + 1;
					} else {
						if (newStr != null) newStr.append(text.substring(idx, last));
						idx = last;
					}
				} else {
					// id, $A$1, ~$r, #$A$1, #$A
					String subStr = text.substring(idx, last);
					String cellId = removeAbsoluteSymbol(subStr);
					if (cellId == null) {
						if (newStr != null) newStr.append(subStr);
					} else {
						if (newStr == null) {
							newStr = new StringBuffer(len + 80);
							newStr.append(text.substring(0, idx));
						}

						newStr.append(cellId);
					}

					idx = last;
				}
			}
		}

		return newStr == null ? text : newStr.toString();
	}
	
	/**
	 * 返回文本是否包含宏替换
	 * @param text
	 * @return
	 */
	public static boolean containMacro(String text) {
		if (text == null) {
			return false;
		}

		int len = text.length();
		int idx = 0;
		while(idx < len) {
			char c = text.charAt(idx);
			// 字符串中的$不做替换
			if (c == '\'' || c == '\"') {
				int match = Sentence.scanQuotation(text, idx);
				if (match < 0) {
					return false;
				} else {
					idx = match + 1;
				}
			} else if (KeyWord.isSymbol(c)) {
				idx += 1;
			} else {
				int last = KeyWord.scanId(text, idx + 1);
				char lc = text.charAt(last - 1);
				if (last < len && lc == '$') { // str${}, str$()
					char nc = text.charAt(last);
					if (nc == '{') {
						int match = Sentence.scanBrace(text, last);
						return match != -1;
					} else if (nc == '(') {
						// $(c)  $()
						int match = scanParenthesis(text, last);
						if (match == -1) { // 括号不匹配
							return false;
						} else {
							idx = match + 1;
						}
					} else {
						idx = last;
					}
				} else {
					idx = last;
				}
			}
		}

		return false;
	}

	// $A$1, #$A$1去掉$, 如果不是这种形式返回null
	private static String removeAbsoluteSymbol(String text) {
		if (text.length() < 3) return null;

		char ch = text.charAt(0);
		if (ch == KeyWord.CELLPREFIX) { // #
			ch = text.charAt(1);
			if (ch == '$') {
				int idx2 = text.indexOf('$', 3);
				if (idx2 == -1) { // #$A1
					String strId = text.substring(2);
					if (CellLocation.parse(strId) != null) {
						return KeyWord.CELLPREFIX + text.substring(2);
					} else {
						return null;
					}
				} else {
					String strCell = text.substring(2, idx2) + text.substring(idx2 + 1);
					if (CellLocation.parse(strCell) != null) {
						return KeyWord.CELLPREFIX + strCell;
					} else {
						return null;
					}
				}
			} else {
				int idx1 = text.indexOf('$', 2);
				if (idx1 == -1) {
					return null;
				} else {
					// #A$1
					String strCell = text.substring(1, idx1) + text.substring(idx1 + 1);
					if (CellLocation.parse(strCell) != null) {
						return KeyWord.CELLPREFIX + strCell;
					} else {
						return null;
					}
				}
			}
		} else if (ch == '$') {
			int idx2 = text.indexOf('$', 2);
			if (idx2 == -1) { // $A1
				String strId = text.substring(1);
				if (CellLocation.parse(strId) != null) {
					return text.substring(1);
				} else {
					return null;
				}
			} else {
				// $A$1
				String strCell = text.substring(1, idx2) + text.substring(idx2 + 1);
				return CellLocation.parse(strCell) == null ? null : strCell;
			}
		} else {
			int idx1 = text.indexOf('$');
			if (idx1 == -1) return null;

			// A$1
			String strCell = text.substring(0, idx1) + text.substring(idx1 + 1);
			return CellLocation.parse(strCell) == null ? null : strCell;
		}
	}

	private static String getMacroValue(String str, ICellSet cs, Context ctx) {
		Expression exp = new Expression(cs, ctx, str);
		Object obj = exp.calculate(ctx);
		if (obj instanceof String) {
			return (String)obj;
		} else if (obj == null) {
			return "";
		} else {
			// 自动转成字符串
			return Variant.toString(obj);
			//MessageManager mm = EngineMessage.get();
			//throw new RQException(mm.getMessage("Variant2.macroTypeError"));
		}
	}
	
	/**复制表达式，用于多线程计算
	 * @param ctx Context
	 */
	public Expression newExpression(Context ctx) {
		if (expStr != null) {
			return new Expression(cs, ctx, expStr, true, false);
		} else {
			return new Expression(home);
		}
	}

	/**
	 * 搜索下一个匹配的圆括号，引号内的圆括号被跳过，不处理转义符
	 * @param str 需要搜索括号的原串
	 * @param start  起始位置,即左圆括号在原串中的位置
	 * @return 若找到,则返回匹配的右圆括号在原串中的位置,否则返回-1
	 */
	public static int scanParenthesis(String str, int start) {
		//if (str.charAt(start) != '(') return -1;

		int len = str.length();
		for (int i = start + 1; i < len;) {
			char ch = str.charAt(i);
			switch (ch) {
			case '(':
				i = scanParenthesis(str, i);
				if (i < 0)
					return -1;
				i++;
				break;
			case '\"':
			case '\'':
				int q = Sentence.scanQuotation(str, i, '\\');
				if (q < 0) {
					i++;
				} else {
					i = q + 1;
				}
				break;
			case '[': // $[str]
				if (i > start && str.charAt(i - 1) == '$') {
					q = Sentence.scanBracket(str, i, '\\');
					if (q < 0) {
						i++;
					} else {
						i = q + 1;
					}
				} else {
					i++;
				}
				break;
			case ')':
				return i;
			default:
				i++;
				break;
			}
		}
		return -1;
	}
	
	/**
	 * 判断一个类的类型是否和另外一个类类型一致，或其子类
	 * @param obj	被判断的类
	 * @param base	参考类型
	 * @return	true	obj的类型为base或
	 */
	public static boolean ifIs(Object obj, Class<?> base) {
		if (null == obj)
			return false;
		if (obj.getClass() == base
				|| (null != obj.getClass() && obj.getClass().getSuperclass() == base))
			return true;
		return false;
	}
	
	/**
	 * 把一个字符串中的某个函数替换为新的字符串
	 * 
	 * @param src	源字符串
	 * @param func	要被删除的函数名
	 * @param newStr	替换的新字符串
	 * 					为null，为空，表示删除
	 * @return
	 */
	public static String replaceFunc(String src, String func, String newStr) {
		/*int start = src.indexOf(func);
		if (start != -1) {
			return src.substring(0, start) + newStr + src.substring(start + func.length());
		} else {
			return src;
		}*/
		return StringUtils.replace(src, func, newStr);
	}

	/**
	 * 取得操作符中所有类为参考类，或以参考类为基类的类
	 * 
	 * @param op	对应操作符
	 * @param base	要取得的函数的类型
	 * @return	返回对应类数组
	 */
	public static ArrayList<Object> getSpecFunc(Operator op, Class<?> base) {
		
		ArrayList<Object> classes = new ArrayList<Object>();
		
		Node left = op.getLeft();
		if (ifIs(left, base)) {
			classes.add(left);
		} else if (left instanceof Function){
			ArrayList<Object> temp = getSpecFunc((Function)left, base);
			temp.addAll(classes);
			classes = temp;
		} else if (left instanceof Operator) {
			ArrayList<Object> temp = getSpecFunc((Operator)left, base);
			temp.addAll(classes);
			classes = temp;
		}
		
		Node right = op.getRight();
		if (ifIs(right, base)) {
			classes.add(right);
		} else if (right instanceof Function){
			classes.addAll(getSpecFunc((Function)right, base));
		} else if (right instanceof Operator) {
			classes.addAll(getSpecFunc((Operator)right, base));
		}
		
		return classes;
	}
	
	/**
	 * 取得函数中所有类为参考类，或以参考类为基类的类
	 * 
	 * @param op	对应的函数
	 * @param base	要取得的函数的类型
	 * @return	返回对应类数组
	 */
	public static ArrayList<Object> getSpecFunc(Function fun, Class<?> base) {
		
		ArrayList<Object> funcs = new ArrayList<Object>();
		
		IParam par = fun.getParam();
		if (null == par)
			return funcs;
		int subCount = par.getSubSize();
		if (0 == subCount) {
			if (ifIs(fun, base)) {
				//String str = ((Gather)par).getFunctionString();
				funcs.add(fun);
			} else if (par instanceof Function){
				funcs.addAll(getSpecFunc((Function)par, base));
			} else if (par instanceof Operator) {
				funcs.addAll(getSpecFunc((Operator)par, base));
			} else if (par instanceof Expression) {
				funcs.addAll(getSpecFunc((Expression)par, base));
			} else if (par.isLeaf()) {
				funcs.addAll(getSpecFunc(par.getLeafExpression(), base));
			} else {
				funcs.addAll(getSpecFunc(par, base));
			}
		}
		
		IParam sub = null;
		for (int i = 0; i < subCount; i++) {
			sub = par.getSub(i);
			if (null == sub) {
				continue;
			} else if (ifIs(sub, base)) {
				funcs.add(sub);
			} else if (sub instanceof Function){
				funcs.addAll(getSpecFunc((Function)sub, base));
			} else if (sub instanceof Operator) {
				funcs.addAll(getSpecFunc((Operator)sub, base));
			} else if (sub instanceof Expression) {
				funcs.addAll(getSpecFunc((Expression)sub, base));
			} else if (sub.isLeaf()) {
				funcs.addAll(getSpecFunc(sub.getLeafExpression(), base));
			} else {
				funcs.addAll(getSpecFunc(sub, base));
			}
		}

		return funcs;
	}
	
	/**
	 * 取得参数中所有类为参考类，或以参考类为基类的类
	 * 
	 * @param param	对应的参数
	 * @param base	要取得的函数的类型
	 * @return	返回对应类数组
	 */
	public static ArrayList<Object> getSpecFunc(IParam param, Class<?> base) {
		ArrayList<Object> funcs = new ArrayList<Object>();

		int subCount = param.getSubSize();
		IParam sub = null;
		for (int i = 0; i < subCount; i++) {
			sub = param.getSub(i);
			if (null == sub) {
				continue;
			} else if (ifIs(sub, base)) {
				funcs.add(sub);
			} else if (sub instanceof Function){
				funcs.addAll(getSpecFunc((Function)sub, base));
			} else if (sub instanceof Operator) {
				funcs.addAll(getSpecFunc((Operator)sub, base));
			} else if (sub instanceof Expression) {
				funcs.addAll(getSpecFunc((Expression)sub, base));
			} else if (sub.isLeaf()) {
				funcs.addAll(getSpecFunc(sub.getLeafExpression(), base));
			} else {
				funcs.addAll(getSpecFunc(sub, base));
			}
		}

		return funcs;
	}
	
	/**
	 * 取得表达式中所有类为参考类，或以参考类为基类的类
	 * 
	 * @param op	对应操作符
	 * @param type	要取得的函数的类型
	 * 		1-- 基类为Gather的类
	 * 		2-- 基类为GatherEx的类
	 * @return	返回对应类数组
	 */
	public static ArrayList<Object> getSpecFunc(Expression exp, Class<?> base) {
		
		ArrayList<Object> funcs = new ArrayList<Object>();
		Node home = exp.getHome();
		
		if (null == home) {
			return funcs;
		} else if (ifIs(home, base)) {
			funcs.add(home);
		} else if (home instanceof Function){
			funcs.addAll(getSpecFunc((Function)home, base));
		} else if (home instanceof Operator) {
			funcs.addAll(getSpecFunc((Operator)home, base));
		} 
		
		return funcs;
	}
	

	/**
	 * 判断两个表达式字符串是否等价
	 * 		若字符串完全相同，则一定等价。
	 * 		除了双引号和单引号内的空格可忽略
	 * 	该函数没有考虑转义符等特殊的情况，只用于粗略估算的场合，目标是解决90%的问题。
	 * @param exp1	第一个表达式
	 * @param exp2      第二个表达式
	 * @return	true	两个表达式等价
	 * 			false	两个表达式不等价
	 */
	public static boolean sameExpression(String exp1, String exp2) {
	    /** 字符串1长度**/
		int len = exp1.length();
		/** 对比到得字符串2得位置 **/
	    int set = 0;
	    /** 是否在进行引号内得对比 **/
	    boolean bstr = false;
	    for (int i = 0; i < len; i++) {
	    	char ch1 = exp1.charAt(i);
	    	char ch2 = exp2.charAt(set);
	    	// 跳过exp1空格
	    	if (!bstr && ' ' == ch1)
	    		continue;
	    	
	    	// 跳过exp2空格
	    	if (!bstr && ' ' == ch2) {
	    		while (' ' == ch2) {
	    			set++;
	    			ch2 = exp2.charAt(set);
	    		}
	    	}
	    	
	    	// 其它比较
	    	if (ch1 != ch2)
	    		return false;
	    	
	    	// 设置引号标记
	    	if ('\"' == ch1 || '\'' == ch1) {
	    		bstr = !bstr;
	    	}
	    }
		return true;
	}
	/**
	 * 取得操作符及其下的聚合函数
	 * 
	 * @param op	对应操作符
	 * @return
	 */
	public static ArrayList<String> gatherParam(Operator op) {
		
		ArrayList<String> gathers = new ArrayList<String>();
		
		Node left = op.getLeft();
		if (left instanceof Gather) {
			String str = ((Gather)left).getFunctionString();
			gathers.add(0, str);
		} else if (left instanceof Function){
			ArrayList<String> temp = gatherParam((Function)left);
			temp.addAll(gathers);
			gathers = temp;
		} else if (left instanceof Operator) {
			ArrayList<String> temp = gatherParam((Operator)left);
			temp.addAll(gathers);
			gathers = temp;
		}
		
		Node right = op.getRight();
		if (right instanceof Gather) {
			String str = ((Gather)right).getFunctionString();
			gathers.add(str);
		} else if (right instanceof Function){
			gathers.addAll(gatherParam((Function)right));
		} else if (right instanceof Operator) {
			gathers.addAll(gatherParam((Operator)right));
		}
		
		return gathers;
	}
	
	/**
	 * 取得函数中所有基类为Gather的函数，包括参数中的。
	 * @param fun
	 * @return
	 */
	public static ArrayList<String> gatherParam(Function fun) {
		ArrayList<String> gathers = new ArrayList<String>();
		
		IParam par = fun.getParam();
		if (null == par) {
			return gathers;
		}
		
		int subCount = par.getSubSize();
		if (0 == subCount) {
			if (par instanceof Gather) {
				String str = ((Gather)par).getFunctionString();
				gathers.add(str);
			} else if (par instanceof Function){
				gathers.addAll(gatherParam((Function)par));
			} else if (par instanceof Operator) {
				gathers.addAll(gatherParam((Operator)par));
			} else if (par instanceof Expression) {
				gathers.addAll(gatherParam((Expression)par));
			} else if (par.isLeaf()) {
				gathers.addAll(gatherParam(par.getLeafExpression()));
			} else {
				gathers.addAll(gatherParam(par));
			}
		}
		
		IParam sub = null;
		for (int i = 0; i < subCount; i++) {
			sub = par.getSub(i);
			if (null == sub) {
				continue;
			} else if (sub instanceof Gather) {
				String str = ((Gather)sub).getFunctionString();
				gathers.add(str);
			} else if (sub instanceof Function){
				gathers.addAll(gatherParam((Function)sub));
			} else if (sub instanceof Operator) {
				gathers.addAll(gatherParam((Operator)sub));
			} else if (sub instanceof Expression) {
				gathers.addAll(gatherParam((Expression)sub));
			} else if (sub.isLeaf()) {
				gathers.addAll(gatherParam(sub.getLeafExpression()));
			} else {
				gathers.addAll(gatherParam(sub));
			}
		}

		return gathers;
	}
	
	/**
	 * 取得参数下的聚合函数
	 * @param par	
	 * @return
	 */
	public static ArrayList<String> gatherParam(IParam param) {
		ArrayList<String> gathers = new ArrayList<String>();

		int subCount = param.getSubSize();
		IParam sub = null;
		for (int i = 0; i < subCount; i++) {
			sub = param.getSub(i);
			if (null == sub) {
				continue;
			} else if (sub instanceof Gather) {
				String str = ((Gather)sub).getFunctionString();
				gathers.add(str);
			} else if (sub instanceof Function){
				gathers.addAll(gatherParam((Function)sub));
			} else if (sub instanceof Operator) {
				gathers.addAll(gatherParam((Operator)sub));
			} else if (sub instanceof Expression) {
				gathers.addAll(gatherParam((Expression)sub));
			} else if (sub.isLeaf()) {
				gathers.addAll(gatherParam(sub.getLeafExpression()));
			} else {
				gathers.addAll(gatherParam(sub));
			}
		}
		
		
		return gathers;
	}
	
	/**
	 * 取得表达式的所有基类为Gather的函数，包括函数中的。
	 * @param exp
	 * @return
	 */
	public static ArrayList<String> gatherParam(Expression exp) {
		ArrayList<String> gathers = new ArrayList<String>();
		Node home = exp.getHome();
		
		if (null == home) {
			return gathers;
		} else if (home instanceof Gather) {
			String str = ((Gather)home).getFunctionString();
			gathers.add(str);
		} else if (home instanceof Function){
			gathers.addAll(gatherParam((Function)home));
		} else if (home instanceof Operator) {
			gathers.addAll(gatherParam((Operator)home));
		} 
		
		return gathers;
	}
	
	/**
	 * 计算出所有行的结果
	 * @param ctx 计算上行文
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx) {
		if (canCalculateAll) {
			return home.calculateAll(ctx);
		} else {
			Current current = ctx.getComputeStack().getTopCurrent();
			int len = current.length();
			ObjectArray array = new ObjectArray(len);
			array.setTemporary(true);
			
			for (int i = 1; i <= len; ++i) {
				current.setCurrent(i);
				Object value = home.calculate(ctx);
				array.push(value);
			}
			
			return array;
		}
	}
	/**
	 * 判断是否可以计算全部的值，有赋值运算时只能一行行计算
	 * @return
	 */
	public boolean canCalculateAll() {
		return canCalculateAll;
	}
	
	/**
	 * 计算表达式的取值范围
	 * @param ctx 计算上行文
	 * @return
	 */
	public IArray calculateRange(Context ctx) {
		return home.calculateRange(ctx);
	}
	
	/**
	 * 判断给定的值域范围是否满足当前条件表达式
	 * @param ctx 计算上行文
	 * @return 取值参照Relation. -1：值域范围内没有满足条件的值，0：值域范围内有满足条件的值，1：值域范围的值都满足条件
	 */
	public int isValueRangeMatch(Context ctx) {
		return home.isValueRangeMatch(ctx);
	}
	
	/**
	 * 计算逻辑与运算符&&的右侧表达式
	 * @param ctx 计算上行文
	 * @param leftResult &&左侧表达式的计算结果
	 * @return
	 */
	public IArray calculateAnd(Context ctx, IArray leftResult) {
		if (leftResult == null) {
			return home.calculateAll(ctx);
		} else {
			return home.calculateAnd(ctx, leftResult);
		}
	}
	
	/**
	 * 计算signArray中取值为sign的行
	 * @param ctx
	 * @param signArray 行标识数组
	 * @param sign 标识
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx, IArray signArray, boolean sign) {
		return home.calculateAll(ctx, signArray, sign);
	}

	/**
	 * 返回节点是否单调递增的
	 * @return true：是单调递增的，false：不是
	 */
	public boolean isMonotone() {
		return home.isMonotone();
	}
}
