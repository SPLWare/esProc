package com.scudata.cellset.datamodel;

import java.util.HashMap;
import java.util.List;

import com.scudata.cellset.ICellSet;
import com.scudata.cellset.INormalCell;
import com.scudata.common.CellLocation;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.common.Sentence;
import com.scudata.dm.Context;
import com.scudata.dm.KeyWord;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.ParamParser;
import com.scudata.resources.EngineMessage;

/**
 * 程序网格里的语句，if、for等
 * @author WangXiaoJun
 *
 */
public class Command {
	public static final byte IF = 1;
	public static final byte ELSE = 2;
	public static final byte ELSEIF = 3;

	public static final byte FOR = 4;
	public static final byte CONTINUE = 5;
	public static final byte BREAK = 6;

	//public static final byte FUNCCALL = 7;
	public static final byte FUNC = 8;
	public static final byte RETURN = 9;
	public static final byte END = 10;
	public static final byte RESULT = 11; // 网格返回值

	public static final byte SQL = 12;
	public static final byte CLEAR = 13;
	//public static final byte ERROR = 14;

	public static final byte FORK = 15;
	public static final byte REDUCE = 16;
	public static final byte GOTO = 17;
	
	public static final byte CHANNEL = 18; // 管道
	public static final byte TRY = 19; // 异常捕捉

	private static final HashMap<String, Byte> keyMap = new HashMap<String, Byte>(20);
	static {
		keyMap.put("if", new Byte(IF));
		keyMap.put("else", new Byte(ELSE));
		keyMap.put("elseif", new Byte(ELSEIF));

		keyMap.put("for", new Byte(FOR));
		keyMap.put("next", new Byte(CONTINUE));
		keyMap.put("break", new Byte(BREAK));

		//keyMap.put("call", new Byte(CALL));
		keyMap.put("func", new Byte(FUNC));
		keyMap.put("return", new Byte(RETURN));
		keyMap.put("end", new Byte(END));
		keyMap.put("result", new Byte(RESULT));
		keyMap.put("$", new Byte(SQL));
		keyMap.put("clear", new Byte(CLEAR));
		keyMap.put("fork", new Byte(FORK));
		keyMap.put("reduce", new Byte(REDUCE));
		
		keyMap.put("goto", new Byte(GOTO));
		keyMap.put("cursor", new Byte(CHANNEL));
		keyMap.put("try", new Byte(TRY));
	}

	private byte type; // 命令类型

	/**
	 * goto：  单元格位置
	 * func c:   子程序的位置
	 * next：  for所在格的位置
	 */
	private String lctStr;
	private CellLocation lct;

	/**
	 * if：    条件表达式
	 * elseif: 条件表达式
	 * for：   循环序列表达式
	 * func c：  参数表达式
	 * return：返回值表达式
	 */
	protected String expStr;
	private IParam param;

	/**
	 * 构建语句对象
	 * @param type 语句类型
	 * @param lctStr 单元格标识符
	 * @param expStr 表达式串
	 */
	public Command(byte type, String lctStr, String expStr) {
		this.type = type;
		this.expStr = expStr;

		if (lctStr != null && lctStr.length() != 0) {
			this.lctStr = lctStr;
		}
	}

	public byte getType() {
		return type;
	}

	public String getLocation() {
		return lctStr;
	}

	/**
	 * 取语句引用的单元格
	 * @param ctx 计算上下文
	 * @return 单元格位置
	 */
	public CellLocation getCellLocation(Context ctx) {
		if (lct != null) {
			return lct;
		}

		if (lctStr != null) {
			lct = CellLocation.parse(lctStr);
			if (lct == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(lctStr + mm.getMessage("cellset.cellNotExist"));
			}
		}

		return lct;
	}

	/**
	 * 取语句参数表达式字符串
	 * @return 参数表达式字符串
	 */
	public String getExpression() {
		return expStr;
	}

	/**
	 * 取语句的参数
	 * @param cs
	 * @param ctx
	 * @return
	 */
	public IParam getParam(ICellSet cs, Context ctx) {
		if (param == null && expStr != null) {
			param = ParamParser.parse(expStr, cs, ctx, true);
		}

		return param;
	}
	
	/**
	 * 取语句参数对应的表达式，此语句只有一个参数
	 * @param cs 网格
	 * @param ctx 计算上下文
	 * @return 表达式对象
	 */
	public Expression getExpression(ICellSet cs, Context ctx) {
		IParam param = getParam(cs, ctx);
		if (param == null) {
			return null;
		} else if (param.isLeaf()) {
			return param.getLeafExpression();
		} else {
			IParam sub = param.getSub(0);
			while (sub != null && !sub.isLeaf()) {
				sub = sub.getSub(0);
			}

			return sub == null ? Expression.NULL : sub.getLeafExpression();
		}
	}

	/**
	 * 取语句参数对应的表达式数组，此语句可以有多个参数
	 * @param cs 网格
	 * @param ctx 计算上下文
	 * @return 表达式对象数组
	 */
	public Expression[] getExpressions(ICellSet cs, Context ctx) {
		IParam param = getParam(cs, ctx);
		if (param == null) {
			return new Expression[0];
		} else if (param.isLeaf()) {
			return new Expression[] {param.getLeafExpression()};
		} else {
			int size = param.getSubSize();
			Expression []exps = new Expression[size];
			for (int i = 0; i < size; ++i) {
				IParam sub = param.getSub(i);
				if (sub != null) exps[i] = sub.getLeafExpression();
			}

			return exps;
		}
	}

	/**
	 * 判断表达是否是result语句
	 * @param cmdStr 表达式串
	 * @return true：是，false：不是
	 */
	public static boolean isResultCommand(String cmdStr) {
		return cmdStr.startsWith("result");
	}

	/**
	 * 判断表达是否是sql语句
	 * @param cmdStr 表达式串
	 * @return true：是，false：不是
	 */
	public static boolean isSqlCommand(String cmdStr) {
		return cmdStr != null && cmdStr.length() > 0 && cmdStr.charAt(0) == '$';
	}

	/**
	 * 判断表达式是否是语句
	 * @param cmdStr 表达式串
	 * @return
	 */
	public static boolean isCommand(String cmdStr) {
		if (cmdStr == null || cmdStr.length() == 0) {
			return false;
		} else if (cmdStr.charAt(0) == '$') {
			return true;
		}

		int pos = KeyWord.scanId(cmdStr, 0);
		int atIndex = cmdStr.lastIndexOf(KeyWord.OPTION, pos);
		if (atIndex != -1) {
			pos = atIndex;
		}

		String key = cmdStr.substring(0, pos);
		return (Byte)keyMap.get(key) != null;
	}

	/**
	 * 把串解析成相应的语句
	 * @param cmdStr表达式串
	 * @return 语句
	 */
	public static Command parse(String cmdStr) {
		if (cmdStr == null || cmdStr.length() == 0) {
			return null;
		} else if (cmdStr.charAt(0) == '$') {
			return parseSqlCommand(cmdStr);
		}

		int pos = KeyWord.scanId(cmdStr, 0);
		int atIndex = cmdStr.lastIndexOf(KeyWord.OPTION, pos);
		if (atIndex != -1) {
			pos = atIndex;
		}

		Byte value = (Byte)keyMap.get(cmdStr.substring(0, pos));
		if (value == null) {
			return null;
		} else {
			return parse(value.byteValue(), cmdStr.substring(pos));
		}
	}

	// 跳过引号内的字符
	private static int scanSemicolon(String str, int start) {
		int idx = 0, len = str.length();
		while(idx < len) {
			char ch = str.charAt(idx);
			if (ch == '\'' || ch == '\"') {
				idx = Sentence.scanQuotation(str, idx);
				if(idx < 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("\",\'" + mm.getMessage("Expression.illMatched"));
				}
				
				idx ++;
			} else if (ch == '{') {
				// 花括号内是集算器表达式可能含引号
				idx = Sentence.scanBrace(str, idx);
				if(idx < 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("\",\'" + mm.getMessage("Expression.illMatched"));
				}
				
				idx ++;
			} else if( ch == ';') {
				return idx;
			} else {
				idx ++;
			}
		}
		
		return -1;
	}

	// $@1(db)select .... ; param...
	//
	private static Command parseSqlCommand(String str) {
		String opt = null;
		String db = null;
		String sql;
		String param = null;

		int len = str.length();
		int sqlStart = 1;
		while (sqlStart < len) {
			char c = str.charAt(sqlStart);
			if (c == KeyWord.OPTION) { // @
				sqlStart++;
				int pos = KeyWord.scanId(str, sqlStart);
				opt = str.substring(sqlStart, pos);

				sqlStart = pos;
			} else if (c == '(') {
				int match = Sentence.scanParenthesis(str, sqlStart);
				if (match == -1) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("(,)" + mm.getMessage("Expression.illMatched"));
				}

				db = str.substring(sqlStart + 1, match).trim();

				sqlStart = match + 1;
				break;
			} else if (Character.isWhitespace(c)) {
				sqlStart++;
			} else {
				break;
			}
		}

		int paramPos = scanSemicolon(str, sqlStart);
		if (paramPos == -1) {
			sql = str.substring(sqlStart, len);
		} else {
			sql = str.substring(sqlStart, paramPos);
			param = str.substring(paramPos + 1);
		}

		sql = sql.trim();
		if (sql.length() == 0) sql = null;
		return new SqlCommand(sql, db, opt, param);
	}

	private static Command parse(byte type, String param) {
		param = param.trim();
		String location = null;
		String exp = null;

		switch (type) {
		case FOR:
		case IF:
		case ELSEIF:
		case RETURN:
		case RESULT:
		case CLEAR:
		case END:
		case FORK:
		case CHANNEL:
			exp = param;
			break;
		case CONTINUE:
		case BREAK:
		case GOTO:
			location = param;
			break;
		case ELSE: // else if exp
			if (param.length() > 0) {
				if (param.startsWith("if")) {
					type = ELSEIF;
					exp = param.substring(2).trim();
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("engine.unknownSentence"));
				}
			}
			break;
		case FUNC:
		case REDUCE:
		case TRY:
			if (param.length() > 0) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.unknownSentence"));
			}
			break;
		default:
			throw new RuntimeException();
		}

		Command command = new Command(type, location, exp);
		return command;
	}
	
	/**
	 * 取语句引用到的单元格
	 * @param cs 网格对象
	 * @param ctx 计算上下文
	 * @param resultList 输出参数，存放引用到的网格
	 */
	public void getUsedCells(ICellSet cs, Context ctx, List<INormalCell> resultList) {
		IParam param = getParam(cs, ctx);
		if (param != null) {
			param.getUsedCells(resultList);
		}
		
		CellLocation lct = getCellLocation(ctx);
		if (lct != null) {
			INormalCell cell = cs.getCell(lct.getRow(), lct.getCol());
			if (!resultList.contains(cell)) {
				resultList.add(cell);
			}
		}
	}
}
