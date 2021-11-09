package com.esproc.jdbc;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Reader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.raqsoft.app.common.AppUtil;
import com.raqsoft.cellset.datamodel.Command;
import com.raqsoft.common.ArgumentTokenizer;
import com.raqsoft.common.Escape;
import com.raqsoft.common.Logger;
import com.raqsoft.common.StringUtils;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.DataStruct;
import com.raqsoft.dm.LocalFile;
import com.raqsoft.dm.Record;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.Table;
import com.raqsoft.dm.cursor.ICursor;
import com.raqsoft.dm.query.SimpleSelect;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.fn.Eval;
import com.raqsoft.util.CellSetUtil;

/**
 * JDBC tool class
 */
public class JDBCUtil {
	/**
	 * Execute JDBC commands. Currently supports: $(db)sql, simple sql, cellSet
	 * expression (separated by \t and \n). Does not support call dfx and the
	 * beginning of dfx.
	 * 
	 * @param cmd
	 * @param ctx
	 * @throws SQLException
	 */
	public static Object executeCmd(String cmd, Context ctx)
			throws SQLException {
		return executeCmd(cmd, null, ctx);
	}

	/**
	 * Execute JDBC commands.
	 * 
	 * @param cmd
	 * @param args
	 * @param ctx
	 * @return
	 * @throws SQLException
	 */
	public static Object executeCmd(String cmd, Sequence args, Context ctx)
			throws SQLException {
		if (!StringUtils.isValidString(cmd)) {
			return null;
		}
		cmd = cmd.trim();
		cmd = Escape.removeEscAndQuote(cmd);
		boolean isGrid = false;
		if (AppUtil.isSQL(cmd)) {
			/* Simple SQL without the beginning of $() */
			cmd = "$()" + cmd;
		} else if (cmd.startsWith("$")) {
			/* $(db)sql or $()sql */
		} else {
			/* Cellset expression */
			isGrid = AppUtil.isGrid(cmd);
		}
		if (isGrid) {
			return CellSetUtil.execute(cmd, args, ctx);
		} else {
			return CellSetUtil.execute1(cmd, args, ctx);
		}
	}

	/**
	 * JDBC keywords
	 */

	/** select */
	public static final String KEY_SELECT = "select";
	/** with */
	public static final String KEY_WITH = "with";
	/** call */
	public static final String KEY_CALL = "call";
	/** calls */
	public static final String KEY_CALLS = "calls";
	/** set */
	public static final String KEY_SET = "set";
	/** sqlfirst */
	public static final String KEY_SQLFIRST = "sqlfirst";
	/** simple */
	public static final String KEY_SIMPLE = "simple";

	/**
	 * Get call expression
	 * 
	 * @param dfxName
	 * @param params
	 * @return
	 * @throws SQLException
	 */
	public static String getCallExp(String dfxName, String params)
			throws SQLException {
		if (params == null)
			params = "";
		else
			params = params.trim();
		String sql = "jdbccall(\"" + dfxName + "\""
				+ (params.length() > 0 ? ("," + params) : "") + ")";
		return sql;
	}

	/**
	 * Get call expression
	 * 
	 * @param dfxName
	 * @param params
	 * @param isOnlyServer
	 * @return
	 * @throws SQLException
	 */
	public static String getCallExp(String dfxName, String params,
			boolean isOnlyServer) throws SQLException {
		if (params == null)
			params = "";
		else
			params = params.trim();
		String hosts = JDBCUtil.getNodesString(dfxName, isOnlyServer);

		String sql;
		if (hosts != null) {
			if (params.length() > 0) {
				if (params.indexOf(",") > -1) {
					String[] ps = params.split(",");
					if (ps != null && ps.length > 0) {
						StringBuffer callxParams = new StringBuffer();
						for (int i = 0; i < ps.length; i++) {
							if (i > 0)
								callxParams.append(",");
							callxParams.append("[");
							callxParams.append(ps[i]);
							callxParams.append("]");
						}
						params = callxParams.toString();
					}
				} else {
					params = "[" + params + "]";
				}
			} else
				params = ""; // [null]
			if (!dfxName.startsWith("\"") || !dfxName.endsWith("\"")) {
				dfxName = Escape.addEscAndQuote(dfxName, true);
			}
			sql = "callx@j(" + dfxName
					+ (params.length() > 0 ? ("," + params) : "") + hosts
					+ ")(1)";
		} else {
			if (isOnlyServer) {
				throw new SQLException(JDBCMessage.get().getMessage(
						"jdbcutil.noserverconfig"));
			}
			sql = "call(\"" + dfxName + "\""
					+ (params.length() > 0 ? ("," + params) : "") + ")";
		}
		return sql;
	}

	/**
	 * JDBC statement types
	 */
	/** unknown */
	public static final byte TYPE_NONE = 0;
	/** Execute statement */
	public static final byte TYPE_EXE = 1;
	/** Expression statement */
	public static final byte TYPE_EXP = 2;
	/** Call statement */
	public static final byte TYPE_CALL = 3;
	/** Call statements */
	public static final byte TYPE_CALLS = 4;
	/** Execute dfx statement */
	public static final byte TYPE_DFX = 5;
	/** Execute SQL statement */
	public static final byte TYPE_SQL = 6;
	/** Execute simple SQL statement */
	public static final byte TYPE_SIMPLE_SQL = 7;

	/**
	 * Get the type of SQL statement
	 * 
	 * @param sql
	 * @return Constants defined above
	 */
	public static byte getJdbcSqlType(String sql) {
		if (!StringUtils.isValidString(sql)) {
			return TYPE_NONE;
		}
		sql = sql.trim();
		/* Procedure may be enclosed in braces */
		if (sql.startsWith("{") && sql.endsWith("}")) {
			sql = sql.substring(1, sql.length() - 1);
			sql = sql.trim();
		}
		if (sql.startsWith(">")) {
			return TYPE_EXE;
		} else if (sql.startsWith("=")) {
			return TYPE_EXP;
		} else if (sql.toLowerCase().startsWith(JDBCUtil.KEY_CALLS)) {
			return TYPE_CALLS;
		} else if (sql.toLowerCase().startsWith(JDBCUtil.KEY_CALL)) {
			return TYPE_CALL;
		} else if (sql.startsWith("$")) {
			String s = sql;
			s = s.substring(1).trim();
			if (s.startsWith("(")) {
				s = s.substring(1).trim();
				if (s.startsWith(")")) {
					return TYPE_SIMPLE_SQL;
				}
			}
			return TYPE_SQL;
		} else if (AppUtil.isSQL(sql)) {
			return TYPE_SIMPLE_SQL;
		}
		return TYPE_NONE;
	}

	/**
	 * 是否calls dfx语句
	 * 
	 * @param sql
	 * @return
	 */
	public static boolean isCallsStatement(String sql) {
		if (!StringUtils.isValidString(sql)) {
			return false;
		}
		sql = sql.trim();
		if (sql.startsWith("{") && sql.endsWith("}")) {
			sql = sql.substring(1, sql.length() - 1);
			sql = sql.trim();
		}
		byte sqlType = JDBCUtil.getJdbcSqlType(sql);
		return sqlType == JDBCUtil.TYPE_CALLS;
	}

	/**
	 * Execute JDBC statement
	 * 
	 * @param sql
	 * @param parameters
	 * @param ctx
	 * @return
	 * @throws Exception
	 */
	public static Object execute(String sql, ArrayList<Object> parameters,
			Context ctx) throws Exception {
		return execute(sql, parameters, ctx, true);
	}

	/**
	 * Execute JDBC statement
	 * 
	 * @param sql
	 *            语句
	 * @param parameters
	 *            参数列表
	 * @param ctx
	 *            上下文
	 * @param logInfo
	 *            是否输出日志
	 * @return
	 * @throws Exception
	 */
	public static Object execute(String sql, ArrayList<?> parameters,
			Context ctx, boolean logInfo) throws Exception {
		if (logInfo)
			Logger.debug("SQL:[" + sql + "]");
		if (!StringUtils.isValidString(sql)) {
			return null;
		}
		sql = sql.trim();
		if (sql.startsWith("{") && sql.endsWith("}")) {
			sql = sql.substring(1, sql.length() - 1);
			sql = sql.trim();
		}
		if (logInfo)
			Logger.debug("param size="
					+ (parameters == null ? "0" : ("" + parameters.size())));
		boolean hasReturn = true;
		boolean isGrid = false;
		String dfxName = null;
		boolean isCalls = false;
		if (sql.startsWith(">")) {
			hasReturn = false;
		} else if (sql.startsWith("=")) {
			isGrid = AppUtil.isGrid(sql);
			sql = sql.substring(1);
		} else if (sql.toLowerCase().startsWith(JDBCUtil.KEY_CALLS)) {
			sql = sql.substring(JDBCUtil.KEY_CALLS.length());
			sql = sql.trim();
			int left = sql.indexOf('(');
			String name, params;
			if (left == -1 && sql.lastIndexOf(')') == -1) {
				name = sql.replaceAll("'", "");
				params = null;
			} else if (left > 0 && sql.endsWith(")")) {
				name = sql.substring(0, left);
				name = name.replaceAll("'", "");
				params = sql.substring(left + 1, sql.length() - 1).trim();

				// 处理非?的参数
				if (parameters.isEmpty()) {
					// 没有传递参数时，拼成一个成员的序列
					StringBuffer totalParams = new StringBuffer();
					ArgumentTokenizer at = new ArgumentTokenizer(params, ',');
					while (at.hasNext()) {
						if (totalParams.length() > 0) {
							totalParams.append(",");
						}
						String param = at.next();
						if (param != null && param.trim().equals("?")) {
							totalParams.append("?");
							continue;
						}
						totalParams.append("["
								+ (param == null ? "" : param.trim()) + "]");
					}
					params = totalParams.toString();
				} else {
					Sequence seq = (Sequence) parameters.get(0);
					int len = seq.length();
					if (len >= 1) {
						boolean needTransParam = false;
						StringBuffer totalParams = new StringBuffer();
						ArgumentTokenizer at = new ArgumentTokenizer(params,
								',');
						while (at.hasNext()) {
							if (totalParams.length() > 0) {
								totalParams.append(",");
							}
							String param = at.next();
							if (param != null && param.trim().equals("?")) {
								totalParams.append("?");
								continue;
							}
							needTransParam = true;
							StringBuffer repParams = new StringBuffer();
							for (int i = 0; i < len; i++) {
								if (repParams.length() > 0)
									repParams.append(",");
								// 非?的参数应该重复N遍
								if (param == null) {
									repParams.append("");
								} else {
									repParams.append(param.trim());
								}
							}
							totalParams
									.append("[" + repParams.toString() + "]");
						}
						if (needTransParam) {
							params = totalParams.toString();
						}
					}
				}
			} else {
				throw new SQLException(
						"Stored procedure formatting error. For example: calls dfx(...)");
			}
			isCalls = true;
			dfxName = name;
			if (dfxName != null) {
				dfxName = dfxName.trim();
				// 脚本
				if (dfxName.startsWith("\"") && dfxName.endsWith("\"")) {
					dfxName = dfxName.substring(1, dfxName.length() - 2);
					dfxName = dfxName.trim();
				} else if (!dfxName.toLowerCase().endsWith(".dfx")) {
					dfxName += ".dfx";
				}
			}
			sql = JDBCUtil.getCallExp(dfxName, params);
		} else if (sql.toLowerCase().startsWith(JDBCUtil.KEY_CALL)) {
			sql = sql.substring(JDBCUtil.KEY_CALL.length());
			sql = sql.trim();
			int left = sql.indexOf('(');
			String name, params;
			if (left == -1 && sql.lastIndexOf(')') == -1) {
				name = sql.replaceAll("'", "");
				params = null;
			} else if (left > 0 && sql.endsWith(")")) {
				name = sql.substring(0, left);
				name = name.replaceAll("'", "");
				params = sql.substring(left + 1, sql.length() - 1).trim();
			} else {
				throw new SQLException(
						"Stored procedure formatting error. For example: call dfx(...)");
			}
			dfxName = name;
			if (dfxName != null) {
				dfxName = dfxName.trim();
				// 脚本
				if (dfxName.startsWith("\"") && dfxName.endsWith("\"")) {
					dfxName = dfxName.substring(1, dfxName.length() - 2);
					dfxName = dfxName.trim();
				} else if (!dfxName.toLowerCase().endsWith(".dfx")) {
					dfxName += ".dfx";
				}
			}
			sql = JDBCUtil.getCallExp(dfxName, params);
		} else if (sql.startsWith("$")) {
			String s = sql;
			s = s.substring(1).trim();
			if (s.startsWith("(")) {
				s = s.substring(1).trim();
				if (s.startsWith(")")) {
					sql = s.substring(1).trim();
					return AppUtil.executeSql(sql,
							(ArrayList<Object>) parameters, ctx);
				}
			}
			Command command = Command.parse(sql);
			if (command.getType() == Command.SQL) {
				Sequence arg = prepareArg((ArrayList<Object>) parameters);
				sql = AppUtil.prepareSql(sql, arg);
				return CellSetUtil.execute(sql, arg, ctx);
			}
		} else if (AppUtil.isSQL(sql)) {
			return AppUtil.executeSql(sql, (ArrayList<Object>) parameters, ctx);
		} else {
			sql = parseDfxSql(sql);
		}

		Sequence arg;
		if (isCalls) {
			arg = JDBCUtil.prepareCallsArg((ArrayList<Sequence>) parameters);
		} else {
			arg = prepareArg((ArrayList<Object>) parameters);
		}

		if (isGrid) { // 网格表达式
			Object val = CellSetUtil.execute(sql, arg, ctx);
			return val;
		}
		boolean isOldCall = false; // ?参数的call语句
		if (sql.startsWith("jdbccall")) {
			int i1 = sql.indexOf("(");
			int i2 = sql.indexOf(")");
			if (i2 > i1) {
				String params = sql.substring(i1 + 1, i2);
				ArgumentTokenizer at = new ArgumentTokenizer(params, ',');
				while (at.hasNext()) {
					String param = at.next();
					if (param != null && param.trim().equals("?")) {
						isOldCall = true;
						break;
					}
				}
			}
		}
		if (sql.startsWith("=") || sql.startsWith(">")) {
			sql = sql.substring(1);
		}
		Object o;
		if (isOldCall) {
			o = Eval.calc(sql, arg, null, ctx);
		} else {
			o = CellSetUtil.execute1(sql, arg, ctx);
		}
		if (!hasReturn)
			return null;
		return o;
	}

	/**
	 * Preparation parameters
	 * 
	 * @param parameters
	 * @return
	 * @throws SQLException
	 */
	public static Sequence prepareArg(List<Object> parameters)
			throws SQLException {
		Sequence arg = new Sequence();
		if (parameters != null && parameters.size() > 0) {
			for (int i = 0; i < parameters.size(); i++) {
				Object obj = parameters.get(i);
				obj = transParamValue(obj);
				arg.add(obj);
				Logger.debug("param" + (i + 1) + "=[" + obj + "]");
			}
		}
		return arg;
	}

	/**
	 * 准备calls参数
	 * 
	 * @param parameters
	 * @return
	 * @throws SQLException
	 */
	public static Sequence prepareCallsArg(List<Sequence> parameters)
			throws SQLException {
		Sequence arg = new Sequence();
		if (parameters != null) {
			Object obj;
			for (int i = 0; i < parameters.size(); i++) {
				Sequence seq = parameters.get(i);
				Sequence sub = new Sequence();
				for (int j = 1; j <= seq.length(); j++) {
					obj = seq.get(j);
					obj = transParamValue(obj);
					sub.add(obj);
				}
				Logger.debug("param" + (i + 1) + "=" + seq.toString());
				arg.add(sub);
			}
		}
		return arg;
	}

	private static Object transParamValue(Object obj) {
		Sequence s = new Sequence();
		if (obj instanceof Object[][]) {
			Object[][] oss = (Object[][]) obj;
			Table t = null;
			if (oss.length >= 1) {
				String[] fields = new String[oss[0].length];
				String[] descs = new String[oss[0].length];
				DataStruct ds = new DataStruct();
				for (int j = 0; j < oss[0].length; j++) {
					Object o = oss[0][j];
					if (o != null) {
						fields[j] = o.toString();
						descs[j] = o.toString();
					} else {
						fields[j] = "";
						descs[j] = "";
					}
				}
				ds = new DataStruct(fields);
				t = new Table(ds);
			} else {
				t = new Table();
			}
			for (int j = 1; j < oss.length; j++) {
				Record r = t.newLast();
				for (int k = 0; k < oss[j].length; k++) {
					r.set(k, oss[j][k]);
				}
			}
			return t;
		} else if (obj instanceof Object[]) {
			Object[] os = (Object[]) obj;
			for (int j = 0; j < os.length; j++)
				s.add(os[j]);
			return s;
		} else if (obj instanceof short[]) {
			short[] os = (short[]) obj;
			for (int j = 0; j < os.length; j++)
				s.add(new Short(os[j]));
			return s;
		} else if (obj instanceof int[]) {
			int[] os = (int[]) obj;
			for (int j = 0; j < os.length; j++)
				s.add(new Integer(os[j]));
			return s;
		} else if (obj instanceof long[]) {
			long[] os = (long[]) obj;
			for (int j = 0; j < os.length; j++)
				s.add(new Long(os[j]));
			return s;
		} else if (obj instanceof double[]) {
			double[] os = (double[]) obj;
			for (int j = 0; j < os.length; j++)
				s.add(new Double(os[j]));
			return s;
		} else if (obj instanceof float[]) {
			float[] os = (float[]) obj;
			for (int j = 0; j < os.length; j++)
				s.add(new Float(os[j]));
			return s;
		} else if (obj instanceof boolean[]) {
			boolean[] os = (boolean[]) obj;
			for (int j = 0; j < os.length; j++)
				s.add(new Boolean(os[j]));
			return s;
		} else {
			return obj;
		}
	}

	/**
	 * Parse the execute dfx statement
	 * 
	 * @param sql
	 * @return
	 * @throws SQLException
	 */
	private static String parseDfxSql(String sql) throws SQLException {
		if (sql == null || sql.length() == 0)
			throw new SQLException("The dfx name is empty.");
		sql = sql.trim();
		String dfx = sql;
		String params = null;
		if (sql.endsWith(")") && sql.indexOf("(") > 0) {
			ArgumentTokenizer at = new ArgumentTokenizer(sql, '(');
			if (at.hasNext()) {
				dfx = at.next();
			} else {
				int paramStart = sql.indexOf("(");
				dfx = sql.substring(0, paramStart);
			}
			if (dfx.length() < sql.length()) {
				params = sql.substring(dfx.length() + 1, sql.length() - 1);
			}
		} else {
			ArgumentTokenizer at = new ArgumentTokenizer(sql, ' ');
			if (at.hasNext()) {
				dfx = at.next();
				if (dfx.length() < sql.length()) {
					params = sql.substring(dfx.length(), sql.length());
				}
			}
		}
		if (params != null) {
			params = params.trim();
		}
		dfx = dfx.trim();
		if (!dfx.toLowerCase().endsWith(".dfx"))
			dfx += ".dfx";
		sql = JDBCUtil.getCallExp(dfx, params);
		return sql;
	}

	/**
	 * Field names in the result set
	 */
	public static final String TABLE_NAME = "TABLE_NAME";
	public static final String COLUMN_NAME = "COLUMN_NAME";
	public static final String DATA_TYPE = "DATA_TYPE";

	/**
	 * Get the table names in the specified pattern
	 * 
	 * @param tableNamePattern
	 * @return
	 * @throws SQLException
	 */
	public static Table getTables(String tableNamePattern) throws SQLException {
		Map<String, String> map = Server.getTables(tableNamePattern);
		Iterator<String> iter = map.keySet().iterator();
		Table t = new Table(new String[] { TABLE_NAME });
		while (iter.hasNext()) {
			String key = iter.next().toString();
			String value = map.get(key).toString();
			t.newLast(new Object[] { value });
		}
		return t;
	}

	/**
	 * Generate Pattern based on filter
	 * 
	 * @param filter
	 * @param fileExts
	 * @return
	 */
	public static Pattern getPattern(String filter, List<String> fileExts) {
		Pattern pattern = null;
		if (StringUtils.isValidString(filter)) {
			if ("%".equals(filter)) {
				filter = null;
			} else {
				filter = filter.replaceAll("%", ".*");
				filter = filter.replaceAll("_", ".?");
				final String PS = File.separator;
				if (AppUtil.isWindowsOS()) {
					filter = filter.replaceAll("/", "\\" + PS);
					if (fileExts != null && fileExts.size() > 1)
						filter = filter.replaceAll("\\\\", "\\\\\\\\");
				}
			}
			if (StringUtils.isValidString(filter))
				pattern = Pattern.compile(filter);
		}
		return pattern;
	}

	/**
	 * Get column names based on pattern
	 * 
	 * @param tableNamePattern
	 * @param columnNamePattern
	 * @param ctx
	 * @return
	 * @throws SQLException
	 */
	public static Table getColumns(String tableNamePattern,
			String columnNamePattern, Context ctx) throws SQLException {
		Table meta = getMeta();
		Map<String, String> tableFileMap = getTableFileMap(meta, false);
		Map<String, String> map = Server.getTables(tableNamePattern);
		Iterator<String> iter = map.keySet().iterator();
		Table t = new Table(new String[] { "TABLE_NAME", "COLUMN_NAME",
				"DATA_TYPE" });

		Pattern columnPattern = JDBCUtil.getPattern(columnNamePattern, null);

		DataStruct ds;
		Sequence data = null;
		while (iter.hasNext()) {
			String key = iter.next().toString();
			String name = map.get(key).toString();
			SimpleSelect ss = new SimpleSelect(null, ctx);
			ICursor cursor = null;
			try {
				String fileName = null;
				if (tableFileMap != null) {
					fileName = tableFileMap.get(name);
				}
				if (!StringUtils.isValidString(fileName)) {
					fileName = name;
				}
				cursor = ss.query("select * from "
						+ Escape.addEscAndQuote(fileName));
				ds = ss.getDataStruct();
				data = cursor.fetch(10);
			} catch (Exception e) {
				throw new SQLException(JDBCMessage.get().getMessage(
						"jdbcutil.dserror", name)
						+ e.getMessage(), e);
			} finally {
				if (cursor != null)
					cursor.close();
			}
			if (ds != null) {
				Map<String, Integer> colTypes = new HashMap<String, Integer>();
				try {
					if (meta != null && meta.length() > 0) {
						Sequence tableMeta = (Sequence) meta.select(
								new Expression("#" + (COL_TABLE + 1) + "=="
										+ Escape.addEscAndQuote(name)), null,
								ctx);
						if (tableMeta != null && tableMeta.length() > 0) {
							for (int c = 0; c < ds.getFieldCount(); c++) {
								String colName = ds.getFieldName(c);
								Sequence colMeta = (Sequence) tableMeta
										.select(new Expression(
												"#"
														+ (COL_COLUMN + 1)
														+ "=="
														+ Escape.addEscAndQuote(colName)),
												null, ctx);
								if (colMeta != null && colMeta.length() > 0) {
									Record record = (Record) colMeta.get(1);
									Object colType = record
											.getFieldValue(COL_TYPE);
									if (colType != null
											&& colType instanceof Number) {
										byte esprocType = ((Number) colType)
												.byteValue();
										int sqlType = getSQLTypeByType(esprocType);
										colTypes.put(colName, sqlType);
									}
								}
							}
						}
					}
				} catch (Exception e) {
					Logger.error(e.getMessage());
				}
				for (int i = 0; i < ds.getFieldCount(); i++) {
					String fname = ds.getFieldName(i);
					if (columnPattern != null) {
						Matcher m = columnPattern.matcher(fname);
						if (!m.matches()) {
							continue;
						}
					}

					int colType = Types.NULL;
					if (colTypes != null) {
						Integer tmp = colTypes.get(fname);
						if (tmp != null)
							colType = tmp;
					}
					if (colType == Types.NULL) {
						if (data != null) {
							Sequence colData = data.fieldValues(fname);
							for (int j = 2, len = colData.length(); j <= len; j++) {
								Object o = colData.get(j);
								if (o != null) {
									colType = getType(o, colType);
									break;
								}
							}
						}
					}
					colType = com.raqsoft.common.Types
							.getTypeBySQLType(colType);
					t.newLast(new Object[] { name, fname, colType });
				}
			}
		}
		return t;
	}

	/**
	 * Fields in the file meta.txt
	 */
	private static final int COL_TABLE = 0;
	private static final int COL_FILE = 1;
	private static final int COL_COLUMN = 2;
	private static final int COL_TYPE = 3;

	/**
	 * Read the meta.txt file
	 * 
	 * @return
	 */
	public static Table getMeta() {
		// 简单SQL不再支持元数据
		// String mainPath = Env.getMainPath();
		// if (!StringUtils.isValidString(mainPath))
		// return null;
		// FileObject fo = new FileObject("meta.txt", "UTF-8", null, null);
		// if (fo.isExists())
		// try {
		// return (Table) fo.importSeries("t");
		// } catch (Exception e) {
		// }
		return null;
	}

	/**
	 * Get the mapping of file name and title
	 * 
	 * @return
	 */
	public static Map<String, String> getTableFileMap() {
		return getTableFileMap(getMeta(), false);
	}

	/**
	 * Get the mapping of file name and title
	 * 
	 * @param fileTableMap
	 * @return
	 */
	public static Map<String, String> getTableFileMap(boolean fileTableMap) {
		return getTableFileMap(getMeta(), fileTableMap);
	}

	/**
	 * Get the mapping of file name and title
	 * 
	 * @param t
	 * @param fileTableMap
	 * @return
	 */
	public static Map<String, String> getTableFileMap(Table t,
			boolean fileTableMap) {
		if (t == null)
			return null;
		Map<String, String> map = new HashMap<String, String>();
		StringBuffer buf = new StringBuffer();
		for (int i = 1, len = t.length(); i <= len; i++) {
			Record r = t.getRecord(i);
			Object table = r.getFieldValue(COL_TABLE);
			Object file = r.getFieldValue(COL_FILE);
			if (StringUtils.isValidString(table)
					&& StringUtils.isValidString(file)) {
				if (fileTableMap) {
					map.put((String) file, (String) table);
				} else {
					map.put((String) table, (String) file);
				}
				if (buf.length() > 0)
					buf.append(";");
				buf.append((String) table + "," + file);
			}
		}
		if (buf.length() > 0) {
			log("meta tables:" + buf.toString());
		} else {
			log("meta tables: null");
		}
		return map;
	}

	/**
	 * Parse call dfx statement
	 * 
	 * @param sql
	 * @return
	 * @throws SQLException
	 */
	public static String[] parseCallDfxSql(String sql) throws SQLException {
		String dfx;
		String params = null;
		sql = sql.trim();
		if (sql.toLowerCase().startsWith(JDBCUtil.KEY_CALL)
				|| sql.toLowerCase().startsWith(JDBCUtil.KEY_CALLS)) {
			if (sql.toLowerCase().startsWith(JDBCUtil.KEY_CALLS)) {
				sql = sql.substring(JDBCUtil.KEY_CALLS.length());
			} else {
				sql = sql.substring(JDBCUtil.KEY_CALL.length());
			}
			sql = sql.trim();
			int left = sql.indexOf('(');
			if (left == -1 && sql.lastIndexOf(')') == -1) {
				dfx = sql.replaceAll("'", "");
				params = null;
			} else if (left > 0 && sql.endsWith(")")) {
				dfx = sql.substring(0, left);
				dfx = dfx.replaceAll("'", "");
				params = sql.substring(left + 1, sql.length() - 1).trim();
			} else {
				throw new SQLException(
						"Stored procedure formatting error. For example: call dfx(...)");
			}
			if (dfx != null) {
				dfx = dfx.trim();
				if (dfx.startsWith("\"") && dfx.endsWith("\"")) {
					dfx = dfx.substring(1, dfx.length() - 2);
					dfx = dfx.trim();
				} else if (!dfx.toLowerCase().endsWith(".dfx")) {
					dfx += ".dfx";
				}
			}
		} else {
			dfx = sql;
			if (sql.endsWith(")") && sql.indexOf("(") > 0) {
				ArgumentTokenizer at = new ArgumentTokenizer(sql, '(');
				if (at.hasNext()) {
					dfx = at.next();
				} else {
					int paramStart = sql.indexOf("(");
					dfx = sql.substring(0, paramStart);
				}
				if (dfx.length() < sql.length()) {
					params = sql.substring(dfx.length() + 1, sql.length() - 1);
				}
			} else {
				ArgumentTokenizer at = new ArgumentTokenizer(sql, ' ');
				if (at.hasNext()) {
					dfx = at.next();
					if (dfx.length() < sql.length()) {
						params = sql.substring(dfx.length(), sql.length());
					}
				}
			}
			if (params != null) {
				params = params.trim();
			}
			dfx = dfx.trim();
			if (!dfx.toLowerCase().endsWith(".dfx"))
				dfx += ".dfx";
		}
		return new String[] { dfx, params };
	}

	/**
	 * Get the string of the unit names
	 * 
	 * @param dfx
	 * @param isOnlyServer
	 * @return
	 */
	public static String getNodesString(String dfx, boolean isOnlyServer) {
		if (!StringUtils.isValidString(dfx)) {
			return null;
		}
		if (!isOnlyServer) {
			LocalFile f = new LocalFile(dfx, "s");
			if (f.exists())
				return null;
		}
		List<String> hosts = Server.getInstance().getHostNames();
		if (hosts.isEmpty())
			return null;
		StringBuffer sb = new StringBuffer();
		for (int i = 0, size = hosts.size(); i < size; i++) {
			String host = (String) hosts.get(i);
			if (i > 0)
				sb.append(",");
			sb.append(Escape.addEscAndQuote(host, '\"'));
		}
		return ";[" + sb.toString() + "]";
	}

	/**
	 * Generate result set
	 * 
	 * @param obj
	 * @param fetchSize
	 * @return
	 * @throws SQLException
	 */
	public static ResultSet generateResultSet(Object obj, int fetchSize)
			throws SQLException {
		return generateResultSet(obj, "Field", fetchSize);
	}

	/**
	 * Generate result set
	 * 
	 * @param obj
	 * @param colName
	 * @param fetchSize
	 * @return
	 * @throws SQLException
	 */
	public static ResultSet generateResultSet(Object obj, String colName,
			int fetchSize) throws SQLException {
		if (obj == null)
			return null;
		String fields[] = null;
		int types[] = null;
		int dealTypes[] = null;
		ArrayList<ArrayList<Object>> datas = null;
		if (obj instanceof Table) {
			Table t = (Table) obj;
			fields = t.dataStruct().getFieldNames();
			types = new int[fields.length];
			initColumnTypes(types);
			dealTypes = new int[fields.length];
			datas = new ArrayList<ArrayList<Object>>(t.length());
			for (int i = 1; i <= t.length(); i++) {
				ArrayList<Object> row = new ArrayList<Object>(fields.length);
				Object seqi = t.get(i);
				if (seqi != null && seqi instanceof Record) {
					Record r = (Record) t.get(i);
					for (int j = 0; j < fields.length; j++) {
						Object o = r.getFieldValue(fields[j]);
						row.add(o);
						if (o == null)
							continue;
						if (dealTypes[j] != 1) {
							types[j] = getType(o, types[j]);
							dealTypes[j] = 1;
						}
					}
				} else {
					for (int j = 0; j < fields.length; j++) {
						row.add(null);
					}
				}
				datas.add(row);
			}
		} else if (obj instanceof Sequence) {
			Sequence seq = (Sequence) obj;
			if (seq.length() == 0) {
				fields = new String[] { colName };
				types = new int[fields.length];
				initColumnTypes(types);
				datas = new ArrayList<ArrayList<Object>>(seq.length());
			} else {
				Object first = seq.get(1);
				if (first == null || !(first instanceof Record)) {
					fields = new String[] { colName };
					types = new int[fields.length];
					initColumnTypes(types);
					dealTypes = new int[fields.length];
					datas = new ArrayList<ArrayList<Object>>(seq.length());
					for (int i = 1; i <= seq.length(); i++) {
						Object o = seq.get(i);
						ArrayList<Object> row = new ArrayList<Object>(1);
						row.add(o);
						datas.add(row);
						if (o == null)
							continue;
						types[0] = getType(o, types[0]);
						if (dealTypes[0] != 1) {
							types[0] = getType(o, types[0]);
							dealTypes[0] = 1;
						}
					}
				} else {
					fields = ((Record) first).dataStruct().getFieldNames();
					types = new int[fields.length];
					initColumnTypes(types);
					dealTypes = new int[fields.length];
					datas = new ArrayList<ArrayList<Object>>(seq.length());
					for (int i = 1; i <= seq.length(); i++) {
						ArrayList<Object> row = new ArrayList<Object>(
								fields.length);
						Object seqi = seq.get(i);
						if (seqi != null && seqi instanceof Record) {
							Record r = (Record) seq.get(i);
							for (int j = 0; j < fields.length; j++) {
								Object o = r.getFieldValue(fields[j]);
								row.add(o);
								if (o == null)
									continue;
								if (dealTypes[j] != 1) {
									types[j] = getType(o, types[j]);
									dealTypes[j] = 1;
								}
							}
						} else {
							for (int j = 0; j < fields.length; j++) {
								row.add(null);
							}
						}
						datas.add(row);
					}
				}
			}
		} else if (obj instanceof Record) {
			Record r = (Record) obj;
			fields = r.dataStruct().getFieldNames();
			types = new int[fields.length];
			initColumnTypes(types);
			datas = new ArrayList<ArrayList<Object>>(1);
			ArrayList<Object> row = new ArrayList<Object>(fields.length);
			for (int i = 0; i < fields.length; i++) {
				Object o = r.getFieldValue(i);
				row.add(o);
				if (o == null)
					continue;
				types[i] = getType(o, types[i]);
			}
			datas.add(row);
		} else if (obj instanceof ICursor) {
			ICursor c = (ICursor) obj;
			com.esproc.jdbc.ResultSet set = new com.esproc.jdbc.ResultSet(c);
			return set;
		} else {
			fields = new String[] { colName };
			types = new int[1];
			initColumnTypes(types);
			datas = new ArrayList<ArrayList<Object>>(1);
			ArrayList<Object> row = new ArrayList<Object>(1);
			row.add(obj);
			if (obj != null) {
				types[0] = getType(obj, types[0]);
			}
			datas.add(row);
		}
		ResultSetMetaData metaData = new ResultSetMetaData(fields, types);
		com.esproc.jdbc.ResultSet set = new com.esproc.jdbc.ResultSet(datas,
				metaData);
		set.setFetchSize(fetchSize);
		return set;
	}

	/**
	 * Default data type
	 */
	private static final int DEFAULT_TYPE = java.sql.Types.NULL;

	/**
	 * Initialize column data type
	 * 
	 * @param types
	 */
	private static void initColumnTypes(int[] types) {
		if (types == null || types.length == 0)
			return;
		for (int i = 0; i < types.length; i++) {
			types[i] = DEFAULT_TYPE;
		}
	}

	/**
	 * Get the SQL type of data object
	 * 
	 * @param o
	 * @param type
	 * @return
	 */
	public static int getType(Object o, int type) {
		if (o == null) {
			return type;
		}
		if (type == java.sql.Types.JAVA_OBJECT) {
			return type;
		}
		byte newType = getProperDataType(o);
		int newSQLType = getSQLTypeByType(newType);
		if (type == java.sql.Types.NULL) {
			return newSQLType;
		}
		if (type != newSQLType) {
			if (isIntType(type) && isIntType(newSQLType)) {
				return java.sql.Types.INTEGER;
			}
			if (isNumberType(type) && isNumberType(newSQLType)) {
				return java.sql.Types.DOUBLE;
			}
			if (isDateType(type) && isDateType(newSQLType)) {
				return java.sql.Types.TIMESTAMP;
			}
			return java.sql.Types.JAVA_OBJECT;
		}
		return newSQLType;
	}

	/**
	 * Get the raqsoft type of data object
	 * 
	 * @param o
	 * @return
	 */
	public static byte getProperDataType(Object o) {
		if (o instanceof Integer) {
			return com.raqsoft.common.Types.DT_INT;
		} else {
			return com.raqsoft.common.Types.getProperDataType(o);
		}
	}

	/**
	 * Whether integer
	 * 
	 * @param type
	 * @return
	 */
	private static boolean isIntType(int type) {
		return type == java.sql.Types.TINYINT
				|| type == java.sql.Types.SMALLINT
				|| type == java.sql.Types.INTEGER;
	}

	/**
	 * Whether numerical
	 * 
	 * @param type
	 * @return
	 */
	private static boolean isNumberType(int type) {
		return type == java.sql.Types.FLOAT || type == java.sql.Types.DOUBLE
				|| isIntType(type);
	}

	/**
	 * Whether date
	 * 
	 * @param type
	 * @return
	 */
	private static boolean isDateType(int type) {
		return type == java.sql.Types.DATE || type == java.sql.Types.TIME
				|| type == java.sql.Types.TIMESTAMP;
	}

	/**
	 * Convert to SQL type according to raqsoft type
	 * 
	 * @param type
	 * @return
	 */
	public static int getSQLTypeByType(byte type) {
		switch (type) {
		case com.raqsoft.common.Types.DT_INT:
			return java.sql.Types.INTEGER;
		case com.raqsoft.common.Types.DT_SHORT:
			return java.sql.Types.SMALLINT;
		case com.raqsoft.common.Types.DT_LONG:
		case com.raqsoft.common.Types.DT_BIGINT:
			return java.sql.Types.BIGINT;
		case com.raqsoft.common.Types.DT_FLOAT:
			return java.sql.Types.FLOAT;
		case com.raqsoft.common.Types.DT_DOUBLE:
			return java.sql.Types.DOUBLE;
		case com.raqsoft.common.Types.DT_DECIMAL:
			return java.sql.Types.DECIMAL;
		case com.raqsoft.common.Types.DT_DATE:
			return java.sql.Types.DATE;
		case com.raqsoft.common.Types.DT_TIME:
			return java.sql.Types.TIME;
		case com.raqsoft.common.Types.DT_DATETIME:
			return java.sql.Types.TIMESTAMP;
		case com.raqsoft.common.Types.DT_STRING:
			return java.sql.Types.VARCHAR;
		case com.raqsoft.common.Types.DT_BOOLEAN:
			return java.sql.Types.BOOLEAN;
		case com.raqsoft.common.Types.DT_BYTE_SERIES:
			return java.sql.Types.BINARY;
		}
		return java.sql.Types.JAVA_OBJECT;
	}

	/**
	 * Get the display name according to the data type
	 * 
	 * @param type
	 * @return
	 */
	public static String getTypeDisp(int type) {
		switch (type) {
		case java.sql.Types.INTEGER:
			return "INTEGER";
		case java.sql.Types.SMALLINT:
			return "SMALLINT";
		case java.sql.Types.BIGINT:
			return "BIGINT";
		case java.sql.Types.FLOAT:
			return "FLOAT";
		case java.sql.Types.DOUBLE:
			return "DOUBLE";
		case java.sql.Types.DECIMAL:
			return "DECIMAL";
		case java.sql.Types.DATE:
			return "DATE";
		case java.sql.Types.TIME:
			return "TIME";
		case java.sql.Types.TIMESTAMP:
			return "TIMESTAMP";
		case java.sql.Types.VARCHAR:
			return "VARCHAR";
		case java.sql.Types.BOOLEAN:
			return "BOOLEAN";
		case java.sql.Types.BINARY:
			return "BINARY";
		}
		return "JAVA_OBJECT";
	}

	/**
	 * Write data to the output stream.
	 * 
	 * @param out
	 * @param list
	 * @throws IOException
	 */
	public static void writeArrayList(ObjectOutput out, ArrayList<Object> list)
			throws IOException {
		if (list == null) {
			out.writeShort((short) 0);
		} else {
			int size = list.size();
			out.writeShort((short) size);
			for (int i = 0; i < size; i++) {
				out.writeObject(list.get(i));
			}
		}
	}

	/**
	 * Read data from the input stream
	 * 
	 * @param in
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static ArrayList<Object> readArrayList(ObjectInput in)
			throws IOException, ClassNotFoundException {
		int count = in.readShort();
		ArrayList<Object> list = null;
		if (count > 0) {
			list = new ArrayList<Object>();
			for (int i = 0; i < count; i++) {
				list.add(in.readObject());
			}
		}
		return list;
	}

	/**
	 * Write data to the output stream.
	 * 
	 * @param out
	 * @param list
	 * @throws IOException
	 */
	public static void writeArrayList2(ObjectOutput out,
			ArrayList<ArrayList<Object>> list) throws IOException {
		if (list == null) {
			out.writeShort((short) 0);
		} else {
			int size = list.size();
			out.writeShort((short) size);
			for (int i = 0; i < size; i++) {
				writeArrayList(out, list.get(i));
			}
		}
	}

	/**
	 * Read data from the input stream
	 * 
	 * @param in
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static ArrayList<ArrayList<Object>> readArrayList2(ObjectInput in)
			throws IOException, ClassNotFoundException {
		int count = in.readShort();
		ArrayList<ArrayList<Object>> list = null;
		if (count > 0) {
			list = new ArrayList<ArrayList<Object>>();
			for (int i = 0; i < count; i++) {
				list.add(readArrayList(in));
			}
		}
		return list;
	}

	/**
	 * Convert the Clob object to a string.
	 * 
	 * @param x
	 * @return
	 * @throws SQLException
	 */
	public static String clobToString(java.sql.Clob x) throws SQLException {
		StringBuffer buf = new StringBuffer();
		Reader is = null;
		try {
			is = x.getCharacterStream();// 得到流
			BufferedReader br = new BufferedReader(is);
			String s = br.readLine();
			while (s != null) {
				buf.append(s);
				s = br.readLine();
			}
		} catch (IOException e) {
			throw new SQLException(e);
		} finally {
			if (is != null)
				try {
					is.close();
				} catch (IOException e) {
				}
		}
		return buf.toString();
	}

	/**
	 * The debugmode parameter has been added. When true, output debugging
	 * information.
	 */
	public static boolean isDebugMode = false;

	/**
	 * Output debugging information.
	 * 
	 * @param info
	 */
	public static void log(String info) {
		if (isDebugMode)
			Logger.debug(info);
	}

	/**
	 * Get an empty result set.
	 * 
	 * @return
	 * @throws SQLException
	 */
	public static ResultSet getEmptyResultSet() throws SQLException {
		return new com.esproc.jdbc.ResultSet(
				(byte) com.esproc.jdbc.ResultSet.GET_EMPTY_RESULT);
	}
}
