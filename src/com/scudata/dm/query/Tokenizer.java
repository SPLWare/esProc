package com.scudata.dm.query;

import java.util.ArrayList;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.common.Sentence;
import com.scudata.resources.ParseMessage;

public final class Tokenizer {
	public static final char KEYWORD = 0; // 保留字
	public static final char IDENT = 1; // 表、字段、别名、函数
	public static final char NUMBER = 2; // 数字常量
	public static final char STRING = 3; // 字符串常量
	public static final char OPERATOR = 4; // 运算符+-*/=<>&|^%!

	public static final char LPAREN = '(';
	public static final char RPAREN = ')';
	public static final char COMMA = ',';
	public static final char DOT = '.';
	public static final char PARAMMARK = '?';

	public static final char LEVELMARK = '#';
	public static final char TABLEMARK = '@'; // F@S、@var
	//public static final char LOCATORMARK = '~'; // F~W

	public static final char OUTERFUNCTION = '$'; // $func(...) 类型对应IDENT？

	private static final String OPSTRING = "+-*/=<>&|^%!~"; // ~按位取反
	private final static String[] GATHERS = {"AVG", "COUNT", "MAX", "MIN", "SUM", "COUNTIF", "FIRST", "LAST"};
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
		"ALL","AND","AS","ASC","AT",
		"BETWEEN","BOTTOM","BY",
		"CALL","CREATE","CASE","COMMIT",
		"DELETE","DESC","DISTINCT","DROP",
		"ELSE","END","EXCEPT","EXISTS","EXTERNAL",
		"FETCH","FIRST","FOREIGN","FROM","FULL",
		"GROUP",
		"HAVING",
		"IN","INNER","INSERT","INTERSECT","IS","INTO",
		"JOIN",
		"LIKE","LEFT","LIMIT",
		"MINUS",
		"NOT","NULL",
		"OFFSET","OLAP","ON","ONLY","OR","ORDER","OUTER",
		"PARALLEL","PRIMARY",
		"RIGHT","ROWS","ROLLBACK",
		"SET","SELECT",
		"THEN","TO","TOP","TABLE","TEMPORARY","TEMP",
		"UPDATE","UNION",
		"VALUES",
		"WHEN","WHERE","WITH"
	};

	// 查找合并关键字
	public static int scanMergeKeyWord(Token []tokens, int start, final int next) {
		for(int i = start; i < next; ++i) {
			if(tokens[i].isMergeKeyWord()) {
				return i;
			} else if (tokens[i].getType() == LPAREN) { // 跳过()
				i = scanParen(tokens, i, next);
			}
		}

		return -1;
	}

	// 在指定范围内搜索指定的关键字，需要大写
	public static int scanByDataKeyWord(Token []tokens, int start, final int next) {
		for(int i = start; i < next; ++i) {
			Token token = tokens[i];
			if (token.getType() == Tokenizer.KEYWORD) {
				if (token.equals("BY")) {
					return i;
				} else if (token.equals("FROM") || token.equals("HAVING") || token.equals("ORDER")) {
					return -1;
				}
			} else if (tokens[i].getType() == LPAREN) { // 跳过()
				i = scanParen(tokens, i, next);
			}
		}

		return -1;
	}

	// 在指定范围内搜索指定的关键字，需要大写
	public static int scanKeyWord(String key, Token []tokens, int start, final int next) {
		for(int i = start; i < next; ++i) {
			if(tokens[i].isKeyWord(key)) {
				return i;
			} else if (tokens[i].getType() == LPAREN) { // 跳过()
				i = scanParen(tokens, i, next);
			}
		}

		return -1;
	}

	public static int lastScanKeyWord(String key, Token []tokens, int start, final int next) {
		for(int i = next - 1; i >= start; --i) {
			if(tokens[i].isKeyWord(key)) {
				return i;
			} else if (tokens[i].getType() == RPAREN) { // 跳过()
				i = lastScanParen(tokens, start, i);
			}
		}

		return -1;
	}

	public static int scanKeyWords(String []keys, Token []tokens, int start, final int next) {
		int keyCount = keys.length;
		for(int i = start; i < next; ++i) {
			Token token = tokens[i];
			if (token.getType() == LPAREN) { // 跳过()
				i = scanParen(tokens, i, next);
			} else if (token.getType() == KEYWORD) {
				for (int k = 0; k < keyCount; ++k) {
					if (token.equals(keys[k])) return i;
				}
			}
		}

		return -1;
	}

	// 在指定范围内搜索逗号
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

	// 搜索()，起始位置是左括号
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

		MessageManager mm = ParseMessage.get();
		throw new RQException("(,)" + mm.getMessage("mark.notMatch"));
	}

	// end为右括号
	public static int lastScanParen(Token []tokens, int start, int end) {
		int deep = 0;
		for(int i = end - 1; i >= start; --i) {
			if (tokens[i].getType() == LPAREN) {
				if (deep == 0) return i;
				deep--;
			} else if (tokens[i].getType() == RPAREN) {
				deep++;
			}
		}

		MessageManager mm = ParseMessage.get();
		throw new RQException("(,)" + mm.getMessage("mark.notMatch"));
	}

	public static String []getStrings(Token []tokens, int start, final int next) {
		String []ts = new String[next - start];
		for (int i = start; i < next; ++i) {
			ts[i - start] = tokens[i].getString();
		}

		return ts;
	}

	public static boolean isGatherFunction(String name) {
		return isKeyWord(name, GATHERS);
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

	// 解析sql，双引号内的为标识符
	public static Token[] parse(String command) {
		int curIndex = 0;
		//command = command.replaceAll("\"", " ").replaceAll("\r\n", " ").replaceAll("\r", " ").replaceAll("\n", " ");
		command = command.trim();
		
		//xingjl 20201211, 以 where 1=2结束时，去掉where，用top 1返回1条，针对永洪、帆软集成集算器
		String upp = command.toUpperCase();
		int selectPos = upp.indexOf("SELECT ");
		int topPos = upp.indexOf(" TOP ");
		int wherePos = upp.indexOf(" WHERE ");
		//System.out.println("SIMPLE SQL INIT : "+command);
		if (selectPos == 0 && topPos == -1 && wherePos>0) {
			String w = upp.substring(wherePos);
			w = w.replaceFirst("WHERE", "").replaceFirst("1", "").replaceFirst("=", "").replaceFirst("2", "");
			if (w.trim().length() == 0) {
				command = command.substring(0,7) + "TOP 1 " + command.substring(7,wherePos);
				//System.out.println("SIMPLE SQL : "+command);
			}
		}
		
		int cmdLen = command.length();
		ArrayList<Token> tokenList = new ArrayList<Token>();
		int paramSeq = 0;
		while (curIndex < cmdLen) {
			char ch = command.charAt(curIndex);
			if (Character.isWhitespace(ch)) {
				Token token = tokenList.get(tokenList.size()-1);
				token.addSpace();
				curIndex++;
			} else if (ch == '[' || ch == ']' || ch == '{' || ch == '}' || ch == ':' || ch == '\\' || ch == '~' || ch == ';') {
				Token token = new Token(ch, ""+ch, curIndex, ""+ch);
				tokenList.add(token);
				curIndex++;
			} else if (ch == LEVELMARK || ch == TABLEMARK) { // #@
				int next = scanId(command, curIndex + 1);
				String id = command.substring(curIndex, next);
				Token token = new Token(ch, id, curIndex, id);
				tokenList.add(token);
				curIndex = next;
			} else if (ch == OUTERFUNCTION) { // $，类型用IDENT？
				int next = scanId(command, curIndex + 1);
				String id = command.substring(curIndex, next);
				Token token = new Token(IDENT, id, curIndex, id);
				tokenList.add(token);
				curIndex = next;
			} else if (isIdentifierStart(ch)) { // 标识符、字段、表
				int next = scanId(command, curIndex + 1);
				String id = command.substring(curIndex, next);
				String upId = id.toUpperCase();
				if (isKeyWord(upId)) {
					Token token = new Token(KEYWORD, upId, curIndex, id);
					tokenList.add(token);
				} else {
					Token token = new Token(IDENT, id, curIndex, id);
					tokenList.add(token);
				}

				curIndex = next;
			} else if (Character.isDigit(ch)) { // 数字
				int next = scanNumber(command, curIndex + 1);
				String id = command.substring(curIndex, next);
				Token token = new Token(NUMBER, id, curIndex, id);
				tokenList.add(token);

				curIndex = next;
			} else if (ch == DOT) { // .操作符或数字
				int next = scanNumber(command, curIndex);
				String id = command.substring(curIndex, next);
				if (next > curIndex + 1) {
					Token token = new Token(NUMBER, id, curIndex, id);
					tokenList.add(token);
				} else {
					Token token = new Token(ch, id, curIndex, id);
					tokenList.add(token);
				}

				curIndex = next;
			} else if (ch == '\'') { // 字符串
				int next = scanString(command, curIndex + 1);
				if (next < 0) {
					MessageManager mm = ParseMessage.get();
					throw new RQException("','" + mm.getMessage("mark.notMatch"));
				}

				String id = command.substring(curIndex, next);
				Token token = new Token(STRING, id, curIndex, id);
				tokenList.add(token);

				curIndex = next;
			} else if (ch == '"') { // 表名或字段
				int next = scanId(command, curIndex);
				String id = command.substring(curIndex, next);
				Token token = new Token(IDENT, id, curIndex, id);
				tokenList.add(token);

				curIndex = next;
			} else if (ch == PARAMMARK) { // ? ?1 ?2 ...参数
				int numIndex = curIndex + 1;
				int next = scanNumber(command, numIndex);
				if (next > numIndex) { // ?n
					try {
						String strNum = command.substring(numIndex, next);
						paramSeq = Integer.parseInt(strNum);
						if (paramSeq < 1) {
							MessageManager mm = ParseMessage.get();
							throw new RQException(mm.getMessage("syntax.error") + numIndex);
						}
					} catch (NumberFormatException e) {
						MessageManager mm = ParseMessage.get();
						throw new RQException(mm.getMessage("syntax.error") + numIndex);
					}
				} else { // ?
					paramSeq++;
				}

				Token token = new Token(ch, "?" + paramSeq, curIndex, "?" + paramSeq);
				tokenList.add(token);
				curIndex = next;
			} else if (OPSTRING.indexOf(ch) != -1) { // 运算符
				String id = command.substring(curIndex, curIndex + 1);
				Token token = new Token(OPERATOR, id, curIndex, id);
				tokenList.add(token);

				curIndex++;
			} else if (ch == LPAREN || ch == RPAREN || ch == COMMA) { // (),
				String id = command.substring(curIndex, curIndex + 1);
				Token token = new Token(ch, id, curIndex, id);
				tokenList.add(token);

				curIndex++;
			} else {
				MessageManager mm = ParseMessage.get();
				
				throw new RQException(mm.getMessage("syntax.error") + curIndex+",["+ch+"]");
			}
		}

		int size = tokenList.size();
		Token[] tokens = new Token[size];
		tokenList.toArray(tokens);
		if(tokens.length > 0 && tokens[tokens.length - 1].getSpaces().isEmpty())
		{
			tokens[tokens.length - 1].addSpace();
		}
				
		return tokens;
	}

	public static int getLogicSqlStart(String command) {
		int curIndex = 0;
		int cmdLen = command.length();

		while (curIndex < cmdLen) {
			char ch = command.charAt(curIndex);
			if (Character.isWhitespace(ch)) {
				curIndex++;
			} else if (Character.isJavaIdentifierStart(ch)) { // 标识符、字段、表
				int next = scanId(command, curIndex + 1);
				String id = command.substring(curIndex, next).toUpperCase();
				if (id.equals("SELECT") || id.equals("WITH") || id.equals("CREATE") || id.equals("DROP")) {
					return curIndex;
				}

				curIndex = next;
			} else if (Character.isDigit(ch)) { // 数字
				curIndex = scanNumber(command, curIndex + 1);
			} else if (ch == '\'') { // 字符串
				int next = scanString(command, curIndex + 1);
				if (next < 0) {
					MessageManager mm = ParseMessage.get();
					throw new RQException("','" + mm.getMessage("mark.notMatch"));
				}

				curIndex = next;
			} else if (ch == '"') { // 表名或字段
				int next = scanIdString(command, curIndex + 1);
				if (next < 0) {
					MessageManager mm = ParseMessage.get();
					throw new RQException("','" + mm.getMessage("mark.notMatch"));
				}

				curIndex = next;
			} else {
				curIndex++;
			}
		}

		return -1;
	}

	private static int scanId(String command, int start) {
		int len = command.length();
		if (start == len) return start;

		if (command.charAt(start) == '"') {
			start = scanIdString(command, start + 1);
			if (start < 0) {
				MessageManager mm = ParseMessage.get();
				throw new RQException("','" + mm.getMessage("mark.notMatch"));
			} else if (start == len) {
				return start;
			}

			char ch = command.charAt(start);
			if (isIdentifierPart(ch)) {
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + start);
			} else {
				return start;
			}
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

	/**
	 * 把字符s替换成字符串r，忽略单引号和双引号括起来的内容
	 * @param str String 源串
	 * @param s char 字符 ?
	 * @param r String 替换成的串
	 * @param ignoreQuote boolean true：忽略引号，替换引号内的字串
	 * @return String
	 */
	public static String replace(String str, char s, String r, boolean ignoreQuote) {
		int len = str.length();
		StringBuffer sb = new StringBuffer(128 + len);

		for (int i = 0; i < len; ++i) {
			char ch = str.charAt(i);
			if (!ignoreQuote && (ch == '\'' || ch == '\"')) { // 字符串
				int pos = Sentence.scanQuotation(str, i);
				if (pos == -1) {
					sb.append(str.substring(i));
					break;
				} else {
					sb.append(str.substring(i, pos + 1));
					i = pos;
				}
			} else if (ch == s) {
				sb.append(r);
			} else {
				sb.append(ch);
			}
		}

		return sb.toString();
	}

	/**
	 * 替换字符串中的表名tableName成exp，忽略大小写，忽略单引号括起来的内容，s可能被双引号括起来
	 * 例如 tableNam.field1替换成t2.fk.field1，tableName后跟着.操作符
	 * @param str String 源串
	 * @param tableName String 需要替换的字串
	 * @param exp String 替换成的串
	 * @return String 返回替换后的串
	 */
	public static String replaceTable(String str, String tableName, String exp) {
		int len = str.length();
		StringBuffer sb = new StringBuffer(128 + len);
		int subLen = tableName.length();
		if (tableName == null || exp == null) {
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("config.error"));
		}

		// tableName后跟着操作符.的才替换？

		for (int i = 0; i < len; ) {
			if (i + subLen >= len) {
				sb.append(str.substring(i));
				break;
			}

			char ch = str.charAt(i);
			if (isIdentifierStart(ch) || ch == '"') {
				int next = scanId(str, i);
				if (next - i == subLen && str.charAt(next) == DOT &&
					str.regionMatches(true, i, tableName, 0, subLen)) {
					sb.append(exp);
				} else {
					sb.append(str.substring(i, next));
				}

				i = next;
			} else if (ch == '\'') { // 字符串
				int pos = Sentence.scanQuotation(str, i);
				if (pos == -1) {
					sb.append(str.substring(i));
					break;
				} else {
					sb.append(str.substring(i, pos + 1));
					i = pos + 1;
				}
			} else if (ch == DOT) {
				sb.append(ch);
				i++;

				// 跳过后面的字段，字段名可能和表名重了？
				ch = str.charAt(i);
				if (isIdentifierStart(ch) || ch == '"') {
					int next = scanId(str, i);
					sb.append(str.substring(i, next));
					i = next;
				}
			} else {
				sb.append(ch);
				i++;
			}
		}

		return sb.toString();
	}

	public static String replace(String str, String sub, String exp, boolean ignoreQuote) {
		int len = str.length();
		StringBuffer sb = new StringBuffer(128 + len);
		int subLen = sub.length();

		for (int i = 0; i < len; ) {
			if (i + subLen - 1 > len) {
				sb.append(str.substring(i));
				break;
			}

			char ch = str.charAt(i);
			if (!ignoreQuote && (ch == '\'' || ch == '\"')) { // 字符串
				int pos = Sentence.scanQuotation(str, i);
				if (pos == -1) {
					sb.append(str.substring(i));
					break;
				} else {
					sb.append(str.substring(i, pos + 1));
					i = pos + 1;
				}
			} else if (str.regionMatches(true, i, sub, 0, subLen)) {
				sb.append(exp);
				i += subLen;
			} else {
				sb.append(ch);
				i++;
			}
		}

		return sb.toString();
	}
	
	public static boolean isOperator(String ch) {
		return (ch.length() == 1 && OPSTRING.indexOf(ch) != -1);
	}
	
	// 搜索{}，起始位置是左大括号
	public static int scanBrace(Token []tokens, int start, final int next) {
		int deep = 0;
		for (int i = start + 1; i < next; ++i) {
			if (tokens[i].getString().equals("{")) {
				deep++;
			} else if (tokens[i].getString().equals("}")) {
				if (deep == 0) return i;
				deep--;
			}
		}

		MessageManager mm = ParseMessage.get();
		throw new RQException("{,}" + mm.getMessage("mark.notMatch"));
	}	
	
	// 搜索case的end，起始位置是case
	public static int scanCaseEnd(Token []tokens, int start, final int next) {
		int deep = 0;
		for (int i = start + 1; i < next; ++i) {
			if (tokens[i].isKeyWord("CASE")) {
				deep++;
			} else if (tokens[i].isKeyWord("END")) {
				if (deep == 0) return i;
				deep--;
			}
		}

		throw new RQException("CASE语句缺少END关键字");
	}	
		
	// 搜索case的when，起始位置when
	public static int scanCaseWhen(Token []tokens, int start, final int next) {
		int deep = 0;
		for (int i = start + 1; i < next; ++i) {
			if (tokens[i].isKeyWord("CASE")) {
				deep++;
			} else if (tokens[i].isKeyWord("END")) {
				deep--;
			} else if (tokens[i].isKeyWord("WHEN")) {
				if (deep == 0) return i;
			} else if (deep < 0) {
				throw new RQException("CASE语句中有多余的END关键字");
			}
		}		
			
		return -1;
	}
			
	// 搜索case的then，起始位置是when
	public static int scanCaseThen(Token []tokens, int start, final int next) {
		int deep = 0;
		for (int i = start + 1; i < next; ++i) {
			if (tokens[i].isKeyWord("CASE")) {
				deep++;
			} else if (tokens[i].isKeyWord("END")) {
				deep--;
			} else if (tokens[i].isKeyWord("THEN")) {
				if (deep == 0) return i;
			} else if (deep < 0) {
				throw new RQException("CASE语句中有多余的END关键字");
			}
		}

		throw new RQException("CASE语句缺少THEN关键字");
	}		
			
	// 搜索case的else，起始位置是case
	public static int scanCaseElse(Token []tokens, int start, final int next) {
		int deep = 0;
		for (int i = start + 1; i < next; ++i) {
			if (tokens[i].isKeyWord("CASE")) {
				deep++;
			} else if (tokens[i].isKeyWord("END")) {
				deep--;
			} else if (tokens[i].isKeyWord("ELSE")) {
				if (deep == 0) return i;
			} else if (deep < 0) {
				throw new RQException("CASE语句中有多余的END关键字");
			}
		}
		
		return -1;
	}		
}
