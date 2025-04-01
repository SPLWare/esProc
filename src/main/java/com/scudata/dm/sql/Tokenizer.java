package com.scudata.dm.sql;

import java.util.ArrayList;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.resources.EngineMessage;

/**
 * 对SQL语句做分词处理，得到词数组
 * @author RunQian
 *
 */
public final class Tokenizer {
	public static final char UNKNOWN = 0;
	public static final char KEYWORD = 1; // 保留字
	public static final char IDENT = 2; // 表、字段、别名、函数
	public static final char NUMBER = 3; // 数字常量
	public static final char STRING = 4; // 字符串常量
	public static final char OPERATOR = 5; // 运算符+-*/=<>&|^%!

	public static final char LPAREN = '(';
	public static final char RPAREN = ')';
	public static final char COMMA = ',';
	public static final char DOT = '.';
	public static final char PARAMMARK = '?';
	

	private static final String OPSTRING = "+-*/=<>&|^%!~"; // ~按位取反
	//private final static String[] GATHERS = {"AVG", "COUNT", "MAX", "MIN", "SUM", "COUNTIF", "COUNTD", "AVGD", "SUMD"};
	//private final static String[] 窗口函数 = {"AVG", "COUNT", "MAX", "MIN", "SUM", "RANK", "DENSE_RANK", "ROW_NUMBER"};

	//private final static String[] OPERATORS = {
	//	"+","-","*","/","=","<","<=","<>","!=",">",">=","%","^","||"
	//};

	public static final String COL_AS = " "; // " AS "
	public static final String TABLE_AS = " "; // oracle表的别名不能加as

	// 选出列表达式中可以用的关键字，用于判断选出列最后的标识符是否是别名
	private final static String[] OPKEYWORDS = {"AND", "OR", "LIKE", "NOT"};

	// "BETWEEN","ROWS","CASE","INNER","END","ONLY", "OUTER",
	private final static String[] KEYWORDS = {
		"ALL","AND","AS","ASC", "AT",
		"BETWEEN", "BOTTOM","BY",
		"CALL","CREATE",//"CASE",
		"DESC","DISTINCT","DROP",
		"END", "EXCEPT","EXISTS","ELSE",
		"FETCH","FIRST","FROM","FULL",
		"GROUP",
		"HAVING",
		"IN","INTERSECT","IS","INTO",
		"JOIN",
		"LIKE","LEFT","LIMIT",
		"MINUS",
		"NOT","NULL",
		"ON","ONLY","OR","ORDER",
		"ROWS", "RIGHT",
		"SELECT",
		"THEN","TO","TOP","TABLE","TEMPORARY","TEMP",
		"UNION",
		"WHEN","WHERE","WITH"
	};

	/**
	 * 搜索括号的结束位置
	 * @param tokens 对SQL语句做分词得到的结果
	 * @param start 左括号的位置
	 * @param next 搜索的结束位置，不包含
	 * @return 右括号的位置，找不到抛出异常
	 */
	public static int scanParen(Token []tokens, int start, final int next) {
		int deep = 0;
		for (int i = start + 1; i < next; ++i) {
			if (tokens[i].getType() == LPAREN) {
				deep++;
			} else if (tokens[i].getType() == RPAREN) {
				if (deep == 0) return i;
				deep--;
			}
		}

		MessageManager mm = EngineMessage.get();
		throw new RQException("(,)" + mm.getMessage("Expression.illMatched"));
	}
	
	/**
	 * 在指定范围内搜索逗号
	 * @param tokens 对SQL语句做分词得到的结果
	 * @param start 搜索起始位置，包括
	 * @param next 搜索的结束位置，不包含
	 * @return 逗号的位置，找不到返回-1
	 */
	public static int scanComma(Token []tokens, int start, final int next) {
		for(int i = start; i < next; ++i) {
			char type = tokens[i].getType();
			if(type == COMMA) {
				return i;
			} else if(type == LPAREN) { // 跳过()
				i = scanParen(tokens, i, next);
			}
		}

		return -1;
	}

	public static String []getStrings(Token []tokens, int start, final int next) {
		String []ts = new String[next - start];
		for (int i = start; i < next; ++i) {
			ts[i - start] = tokens[i].getString();
		}

		return ts;
	}

	public static boolean isOperatorKeyWord(String name) {
		return isKeyWord(name, OPKEYWORDS);
	}

	public static boolean isKeyWord(String id, String []keyWords) {
		if (id == null)return false;
		id = id.toUpperCase();
		for (int i = 0, len = keyWords.length; i < len; ++i) {
			if (id.equals(keyWords[i]))return true;
		}

		return false;
	}

	public static boolean isKeyWord(String id) {
		return isKeyWord(id, KEYWORDS);
	}

	public static boolean isIdentifierStart(char ch) {
		// sqlserver临时表不会出现#？
		return Character.isJavaIdentifierStart(ch); //  || ch == '#'
	}

	public static boolean isIdentifierPart(char ch) {
		// sqlserver临时表不会出现#？
		return Character.isJavaIdentifierPart(ch); //  || ch == '#'
	}

	/**
	 * 对SQL语句做分词，双引号内的为标识符
	 * @param sql SQL语句
	 * @return Token数组
	 */
	public static Token[] parse(String sql) {
		int curIndex = 0;
		int cmdLen = sql.length();
		ArrayList<Token> tokenList = new ArrayList<Token>();

		while (curIndex < cmdLen) {
			char ch = sql.charAt(curIndex);
			if (Character.isWhitespace(ch)) {
				curIndex++;
			} else if (isIdentifierStart(ch)) { // 标识符、字段、表
				int next = scanId(sql, curIndex + 1);
				String id = sql.substring(curIndex, next);
				String upId = id.toUpperCase();
				if (isKeyWord(upId)) {
					Token token = new Token(KEYWORD, upId, curIndex);
					tokenList.add(token);
				} else {
					Token token = new Token(IDENT, id, curIndex);
					tokenList.add(token);
				}

				curIndex = next;
			} else if (Character.isDigit(ch)) { // 数字
				int next = scanNumber(sql, curIndex + 1);
				String id = sql.substring(curIndex, next);
				Token token = new Token(NUMBER, id, curIndex);
				tokenList.add(token);

				curIndex = next;
			} else if (ch == DOT) { // .操作符或数字
				int next = scanNumber(sql, curIndex);
				String id = sql.substring(curIndex, next);
				if (next > curIndex + 1) {
					Token token = new Token(NUMBER, id, curIndex);
					tokenList.add(token);
				} else {
					Token token = new Token(DOT, id, curIndex);
					tokenList.add(token);
				}

				curIndex = next;
			} else if (ch == '\'') { // 字符串
				int next = scanString(sql, curIndex + 1);
				if (next < 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("'" + mm.getMessage("Expression.illMatched"));
				}

				String id = sql.substring(curIndex, next);
				Token token = new Token(STRING, id, curIndex);
				tokenList.add(token);

				curIndex = next;
			} else if (ch == '"') { // 表名或字段
				int next = scanId(sql, curIndex);
				String id = sql.substring(curIndex, next);
				Token token = new Token(IDENT, id, curIndex);
				tokenList.add(token);

				curIndex = next;
			} else if (ch == PARAMMARK) { // ? ?1 ?2 ...参数
				Token token = new Token(PARAMMARK, "?", curIndex);
				tokenList.add(token);
				
				curIndex++;
			} else if (OPSTRING.indexOf(ch) != -1) { // 运算符
				String id = sql.substring(curIndex, curIndex + 1);
				Token token = new Token(OPERATOR, id, curIndex);
				tokenList.add(token);

				curIndex++;
			} else if (ch == LPAREN || ch == RPAREN || ch == COMMA) { // (),
				String id = sql.substring(curIndex, curIndex + 1);
				Token token = new Token(ch, id, curIndex);
				tokenList.add(token);

				curIndex++;
			} else {
				int next = scanId(sql, curIndex + 1);
				Token token = new Token(UNKNOWN, sql.substring(curIndex, next), curIndex);
				tokenList.add(token);

				curIndex = next;
			}
		}

		int size = tokenList.size();
		Token []tokens = new Token[size];
		tokenList.toArray(tokens);
		return tokens;
	}

	private static int scanId(String command, int start) {
		int len = command.length();
		if (start == len) return start;

		if (command.charAt(start) == '"') {
			start = scanIdString(command, start + 1);
			if (start < 0) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("\"" + mm.getMessage("Expression.illMatched"));
			}

			return start;
		} else {
			for (; start < len; ++start) {
				char ch = command.charAt(start);
				if (!isIdentifierPart(ch)) {
					break;
				}
			}

			return start;
		}
	}

	public static int scanNumber(String command, int start) {
		int len = command.length();
		for (; start < len; ++start) {
			if (!Character.isDigit(command.charAt(start))) break;
		}


		if (start < len && command.charAt(start) == '.') {
			for (++start; start < len; ++start) {
				if (!Character.isDigit(command.charAt(start))) break;
			}
		}

		return start;
	}

	// 查找'ss'，如果字符串里含有单引号则用两个连一块的单引号表示 'dd''ff'
	public static int scanString(String command, int start) {
		int len = command.length();
		for (; start < len; ++start) {
			if (command.charAt(start) == '\'') {
				start++;
				if (start == len || command.charAt(start) != '\'') {
					return start;
				}
			}
		}

		return -1;
	}

	private static int scanIdString(String command, int start) {
		int len = command.length();
		for (; start < len; ++start) {
			if (command.charAt(start) == '"') {
				start++;
				if (start == len || command.charAt(start) != '"') {
					return start;
				}
			}
		}

		return -1;
	}
	
	public static boolean isOperator(String ch) {
		return (ch.length() == 1 && OPSTRING.indexOf(ch) != -1);
	}
}
