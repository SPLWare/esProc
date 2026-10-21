package com.scudata.cellset.datamodel;

import java.lang.reflect.Method;

import com.scudata.cellset.ICellSet;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.DBObject;
import com.scudata.dm.sql.Tokenizer;
import com.scudata.expression.Expression;
import com.scudata.resources.EngineMessage;

public class SqlCommand extends Command {
	private String sql;
	private String db;
	private String opt;
	private Expression dbExp;
	private boolean isCursor = false;
	private boolean isInit = false;

	public SqlCommand(String sql, String db, String opt, String expStr) {
		super(SQL, null, expStr);
		this.db = db;
		this.opt = opt;
		
		// 删除注释
		sql = stripComments(sql);
		sql = stripCursorFor(sql);
		this.sql = sql;
	}
	
	public void init(PgmCellSet cs, Object db) {
		if (isInit) {
			return;
		}
		
		isInit = true;
		if (db instanceof DBObject) {
			try {
				Class<? extends Object> classObj = Class.forName("com.scudata.nlc.parser.Compiler");
				Method method = classObj.getMethod("compileSQL", PgmCellSet.class, DBObject.class, String.class);
				Object []args = new Object[] {cs, (DBObject)db, sql};
				sql = (String)method.invoke(classObj, args);
			} catch (Exception e) {
				//throw new RQException(e.getMessage(), e);
			}
		}
		
		sql = parseParam(sql);
	}
	
	public boolean isLogicSql() {
		return db != null && db.length() == 0;
	}
	
	public boolean isQuery() {
		if (sql == null || sql.length() < 6) {
			return false;
		} else if (sql.charAt(0) == '$') { // 逻辑SQL
			return true;
		}

		String mainKw = findMainStatementKeyword(sql);
		if (mainKw == null) {
			return false;
		} else {
			return mainKw.equalsIgnoreCase("select");
		}
	}

	public String getSql() {
		return sql;
	}

	public String getDb() {
		return db;
	}

	public String getOption() {
		return opt;
	}

	public Expression getDbExpression(ICellSet cs, Context ctx) {
		if (dbExp == null && db != null) {
			dbExp = new Expression(cs, ctx, db);
		}

		return dbExp;
	}

	private static String findMainStatementKeyword(String sql) {
		int parenDepth = 0;
		int i = 0;
		int len = sql.length();

		boolean withSeen = false;
		boolean cteNameExpected = false;
		boolean parenExpectedForCte = false;
		boolean inSingleQuote = false;
		boolean inDoubleQuote = false;

		while (i < len) {
			char c = sql.charAt(i);

			if (c == '\'' && !inDoubleQuote) {
				inSingleQuote = !inSingleQuote;
				i++;
				continue;
			} else if (c == '"' && !inSingleQuote) {
				inDoubleQuote = !inDoubleQuote;
				i++;
				continue;
			} else if (inSingleQuote || inDoubleQuote) {
				i++;
				continue;
			} else if (Character.isWhitespace(c)) {
				i++;
				continue;
			} else if (c == ';') {
				i++;
				withSeen = false;
				continue;
			} else if (c == ',' && parenDepth == 0) {
				i++;
				cteNameExpected = withSeen;
				continue;
			} else if (c == '(') {
				parenDepth++;
				i++;
				parenExpectedForCte = false;
				continue;
			} else if (c == ')') {
				parenDepth--;
				i++;
				continue;
			}

			if (parenDepth == 0) {
				int kwEnd = i;
				while (kwEnd < len && !isSqlDelimiter(sql.charAt(kwEnd))) {
					char kc = sql.charAt(kwEnd);
					if (kc == '\'' || kc == '"') {
						break;
					}
					kwEnd++;
				}
				String kw = sql.substring(i, kwEnd);
				String upperKw = kw.toUpperCase();

				if (!withSeen) {
					if ("WITH".equals(upperKw)) {
						withSeen = true;
						cteNameExpected = true;
						i = kwEnd;
						continue;
					}
					return upperKw;
				}

				if (cteNameExpected) {
					cteNameExpected = false;
					i = kwEnd;
					continue;
				}

				if ("AS".equals(upperKw)) {
					parenExpectedForCte = true;
					i = kwEnd;
					continue;
				}

				if (parenExpectedForCte) {
					return upperKw;
				}

				if (!upperKw.isEmpty() && Character.isLetter(upperKw.charAt(0))) {
					return upperKw;
				}
			}

			i++;
		}

		return null;
	}

	private static boolean isSqlDelimiter(char c) {
		return c == '(' || c == ')' || c == ',' || c == ';' || Character.isWhitespace(c);
	}

	private static String stripComments(String sql) {
		if (sql == null) {
			return null;
		}

		StringBuilder result = new StringBuilder();
		int i = 0;
		int len = sql.length();
		boolean inSingleQuote = false;
		boolean inDoubleQuote = false;

		while (i < len) {
			char c = sql.charAt(i);

			if (c == '\'' && !inDoubleQuote) {
				inSingleQuote = !inSingleQuote;
				result.append(c);
				i++;
				continue;
			}

			if (c == '"' && !inSingleQuote) {
				inDoubleQuote = !inDoubleQuote;
				result.append(c);
				i++;
				continue;
			}

			if (inSingleQuote || inDoubleQuote) {
				result.append(c);
				i++;
				continue;
			}

			if (c == '-' && i + 1 < len && sql.charAt(i + 1) == '-') {
				while (i < len && sql.charAt(i) != '\n') {
					i++;
				}
				if (i < len) {
					result.append('\n');
					i++;
				}
				continue;
			}

			if (c == '/' && i + 1 < len && sql.charAt(i + 1) == '*') {
				i += 2;
				while (i < len - 1 && !(sql.charAt(i) == '*' && sql.charAt(i + 1) == '/')) {
					i++;
				}
				if (i < len - 1) {
					i += 2;
				} else {
					i = len;
				}
				continue;
			}

			result.append(c);
			i++;
		}

		return result.toString();
	}
	
	//删除cursor for
	private String stripCursorFor(String sql) {
		if (sql == null || sql.length() < 10) {
			return sql;
		}
		
		int i = 0;
		int len = sql.length();
		
		while (i < len) {
			char ch = sql.charAt(i);
			if (Character.isWhitespace(ch)) {
				i++;
			} else {
				break;
			}
		}
		
		if (!sql.regionMatches(true, i, "cursor", 0, 6)) {
			return sql;
		}
		
		i += 6;
		while (i < len) {
			char ch = sql.charAt(i);
			if (Character.isWhitespace(ch)) {
				i++;
			} else {
				break;
			}
		}
		
		if (sql.regionMatches(true, i, "for", 0, 3)) {
			isCursor = true;
			return sql.substring(i + 3);
		} else {
			return sql;
		}
	}

	public boolean isCursor() {
		return isCursor;
	}
	
	// 解析SQL语句中用到的参数，?x表示引用外部单元格或变量x
	private String parseParam(String sql) {
		if (sql == null) {
			return null;
		}
		
		int len = sql.length();
		StringBuffer sb = new StringBuffer(len);
		int curIndex = 0;
		String param = null;
		
		while (curIndex < len) {
			char ch = sql.charAt(curIndex);
			if (ch == '\'') { // 字符串
				int next = Tokenizer.scanString(sql, curIndex + 1);
				if (next < 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("'" + mm.getMessage("Expression.illMatched"));
				}

				String id = sql.substring(curIndex, next);
				sb.append(id);
				curIndex = next;
			} else if (ch == '"') { // 表名或字段
				int next = Tokenizer.scanId(sql, curIndex);
				String id = sql.substring(curIndex, next);
				sb.append(id);
				curIndex = next;
			} else if (ch == Tokenizer.PARAMMARK) { // ?x参数
				sb.append(Tokenizer.PARAMMARK);
				int start = curIndex + 1;
				
				if (start < len && Tokenizer.isIdentifierStart(sql.charAt(start))) {
					for (curIndex = start + 1; curIndex < len; ++curIndex) {
						if (!Tokenizer.isIdentifierPart(sql.charAt(curIndex))) {
							break;
						}
					}
					
					if (param == null) {
						param = sql.substring(start, curIndex);
					} else {
						param += "," + sql.substring(start, curIndex);
					}
				} else {
					curIndex++;
				}
			} else {
				sb.append(ch);
				curIndex++;
			}
		}
		
		if (param == null) {
			return sql;
		} else {
			setParamString(param);
			return sb.toString();
		}
	}
}
