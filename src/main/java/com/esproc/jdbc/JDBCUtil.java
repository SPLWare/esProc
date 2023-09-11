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
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.scudata.app.common.AppConsts;
import com.scudata.app.common.AppUtil;
import com.scudata.cellset.datamodel.Command;
import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.common.ArgumentTokenizer;
import com.scudata.common.Escape;
import com.scudata.common.Logger;
import com.scudata.common.StringUtils;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Env;
import com.scudata.dm.KeyWord;
import com.scudata.dm.LocalFile;
import com.scudata.dm.Param;
import com.scudata.dm.ParamList;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.query.SimpleSelect;
import com.scudata.expression.fn.Eval;

/**
 * JDBC tool class
 */
public class JDBCUtil {

	/**
	 * Get the type of SQL statement
	 * 
	 * @param sql
	 * @return Constants defined above
	 */
	public static byte getJdbcSqlType(String sql) {
		if (!StringUtils.isValidString(sql)) {
			return JDBCConsts.TYPE_NONE;
		}
		sql = trimSql(sql);
		if (sql.startsWith(">")) {
			return JDBCConsts.TYPE_EXE;
		} else if (sql.startsWith("=")) {
			return JDBCConsts.TYPE_EXP;
		} else if (sql.toLowerCase().startsWith(JDBCConsts.KEY_CALLS)) {
			return JDBCConsts.TYPE_CALLS;
		} else if (sql.toLowerCase().startsWith(JDBCConsts.KEY_CALL)) {
			return JDBCConsts.TYPE_CALL;
		} else if (sql.startsWith("$")) {
			String s = sql;
			s = s.substring(1).trim();
			if (s.startsWith("(")) {
				s = s.substring(1).trim();
				if (s.startsWith(")")) {
					return JDBCConsts.TYPE_SIMPLE_SQL;
				}
				Command command = Command.parse(sql);
				if (command.getType() == Command.SQL) {
					return JDBCConsts.TYPE_SQL;
				}
			} else if (AppUtil.isSQL(sql)) {
				return JDBCConsts.TYPE_SIMPLE_SQL;
			}
		} else if (AppUtil.isSQL(sql) && JDBCUtil.isCompatiblesql) {
			return JDBCConsts.TYPE_SIMPLE_SQL;
		}
		return JDBCConsts.TYPE_NONE;
	}

	/**
	 * Execute JDBC statement
	 * 
	 * @param sql
	 * @param parameters
	 * @param ctx
	 * @return Object
	 * @throws Exception
	 */
	public static Object execute(String sql, ArrayList<Object> parameters,
			Context ctx) throws Exception {
		return execute(sql, parameters, ctx, true);
	}

	/**
	 * Execute JDBC statement
	 * 
	 * @param sql        语句
	 * @param parameters 参数列表
	 * @param ctx        上下文
	 * @param logInfo    是否输出日志
	 * @return Object
	 * @throws Exception
	 */
	public static Object execute(String sql, ArrayList<?> parameters,
			Context ctx, boolean logInfo) throws Exception {
		if (logInfo)
			Logger.debug("SQL:[" + sql + "]");
		if (!StringUtils.isValidString(sql)) {
			return null;
		}
		sql = trimSql(sql);
		if (logInfo)
			Logger.debug("param size="
					+ (parameters == null ? "0" : ("" + parameters.size())));
		boolean hasReturn = true;
		boolean isGrid = false;
		String splName = null;
		boolean isCalls = false;
		if (sql.startsWith(">")) {
			hasReturn = false;
		} else if (sql.startsWith("=")) {
			sql = sql.substring(1);
			isGrid = AppUtil.isGrid(sql);
		} else if (sql.toLowerCase().startsWith(JDBCConsts.KEY_CALLS)) {
			String[] nameParam = getCallsNameParam(sql, parameters);
			splName = nameParam[0];
			String params = nameParam[1];
			isCalls = true;
			sql = JDBCUtil.getCallExp(splName, params);
		} else if (sql.toLowerCase().startsWith(JDBCConsts.KEY_CALL)) {
			String[] nameParam = getCallNameParam(sql);
			splName = nameParam[0];
			String params = nameParam[1];
			sql = JDBCUtil.getCallExp(splName, params);
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

				Command command = Command.parse(sql);
				if (command.getType() == Command.SQL) {
					Sequence arg = prepareArg((ArrayList<Object>) parameters);
					sql = AppUtil.prepareSql(sql, arg);
					return AppUtil.execute(sql, arg, ctx);
				}
			} else if (AppUtil.isSQL(sql)) {
				return AppUtil.executeSql(s, (ArrayList<Object>) parameters,
						ctx);
			}
		} else if (AppUtil.isSQL(sql) && JDBCUtil.isCompatiblesql) {
			return AppUtil.executeSql(sql, (ArrayList<Object>) parameters, ctx);
		} else {
			sql = parseSpl(sql);
		}

		Sequence arg;
		if (isCalls) {
			arg = JDBCUtil.prepareCallsArg((ArrayList<Sequence>) parameters);
		} else {
			arg = prepareArg((ArrayList<Object>) parameters);
		}

		if (isGrid) { // 网格表达式
			Object val = AppUtil.execute(sql, arg, ctx);
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
			o = AppUtil.execute1(sql, arg, ctx);
		}
		if (!hasReturn)
			return null;
		return o;
	}

	/**
	 * 去除sql外的空白和大括号
	 * @param sql
	 * @return
	 */
	public static String trimSql(String sql) {
		if (!StringUtils.isValidString(sql))
			return null;
		sql = sql.trim();
		if (sql.startsWith("{") && sql.endsWith("}")) {
			sql = sql.substring(1, sql.length() - 1);
			sql = sql.trim();
		}
		return sql;
	}

	/**
	 * Get call expression
	 * 
	 * @param splName
	 * @param params
	 * @return String
	 * @throws SQLException
	 */
	public static String getCallExp(String splName, String params)
			throws SQLException {
		if (params == null)
			params = "";
		else
			params = params.trim();
		String sql = "jdbccall(\"" + splName + "\""
				+ (params.length() > 0 ? ("," + params) : "") + ")";
		return sql;
	}

	/**
	 * Get call expression
	 * 
	 * @param splName
	 * @param params
	 * @param isOnlyServer
	 * @return String
	 * @throws SQLException
	 */
	public static String getCallExp(String splName, String params,
			boolean isOnlyServer, List<String> hostNames) throws SQLException {
		if (params == null)
			params = "";
		else
			params = params.trim();
		String hosts = JDBCUtil
				.getNodesString(splName, isOnlyServer, hostNames);

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
			if (!splName.startsWith("\"") || !splName.endsWith("\"")) {
				splName = Escape.addEscAndQuote(splName, true);
			}
			sql = "callx@j(" + splName
					+ (params.length() > 0 ? ("," + params) : "") + hosts
					+ ")(1)";
		} else {
			if (isOnlyServer) {
				throw new SQLException(JDBCMessage.get().getMessage(
						"jdbcutil.noserverconfig"));
			}
			sql = "call(\"" + splName + "\""
					+ (params.length() > 0 ? ("," + params) : "") + ")";
		}
		return sql;
	}

	/**
	 * 是否calls spl语句
	 * 
	 * @param sql
	 * @return boolean
	 */
	public static boolean isCallsStatement(String sql) {
		if (!StringUtils.isValidString(sql)) {
			return false;
		}
		byte sqlType = JDBCUtil.getJdbcSqlType(sql);
		return sqlType == JDBCConsts.TYPE_CALLS;
	}

	/**
	 * 取calls语句的spl名称和参数
	 * @param sql
	 * @param parameters
	 * @return
	 * @throws SQLException
	 */
	public static String[] getCallsNameParam(String sql, ArrayList<?> parameters)
			throws SQLException {
		return getCallsNameParam(sql, parameters, true);
	}

	/**
	 * 取calls语句的spl名称和参数
	 * @param sql
	 * @param parameters
	 * @param isExecute 是否执行时调用的，不是执行时不处理参数
	 * @return
	 * @throws SQLException
	 */
	public static String[] getCallsNameParam(String sql,
			ArrayList<?> parameters, boolean isExecute) throws SQLException {
		sql = sql.substring(JDBCConsts.KEY_CALLS.length());
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
			if (isExecute) {
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
						if (param != null) {
							param = param.trim();
							if (KeyWord.isArg(param)) {
								totalParams.append(param);
								continue;
							}
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
							if (param != null) {
								param = param.trim();
								if (KeyWord.isArg(param)) {
									totalParams.append(param);
									continue;
								}
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
			}
		} else {
			// Stored procedure formatting error. For example: calls spl(...)
			throw new SQLException(JDBCMessage.get().getMessage(
					"jdbcutil.errorcallsformat"));
		}
		String splName = name;
		if (splName != null) {
			splName = splName.trim();
			// 脚本
			if (splName.startsWith("\"") && splName.endsWith("\"")) {
				splName = splName.substring(1, splName.length() - 2);
				splName = splName.trim();
			}
		}
		return new String[] { splName, params };
	}

	/**
	 * 取call语句的splx名称和参数
	 * @param sql
	 * @return
	 * @throws SQLException
	 */
	public static String[] getCallNameParam(String sql) throws SQLException {
		sql = sql.substring(JDBCConsts.KEY_CALL.length());
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
			throw new SQLException(JDBCMessage.get().getMessage(
					"jdbcutil.errorcallformat"));
		}
		String splName = name;
		if (splName != null) {
			splName = splName.trim();
			// 脚本
			if (splName.startsWith("\"") && splName.endsWith("\"")) {
				splName = splName.substring(1, splName.length() - 2);
				splName = splName.trim();
			}
		}
		return new String[] { splName, params };
	}

	/**
	 * 取SPL语句的splx名称和参数
	 * @param sql
	 * @return
	 */
	public static String[] getSplNameParam(String sql) {
		sql = sql.trim();
		String spl = sql;
		String params = null;
		if (sql.endsWith(")") && sql.indexOf("(") > 0) {
			ArgumentTokenizer at = new ArgumentTokenizer(sql, '(');
			if (at.hasNext()) {
				spl = at.next();
			} else {
				int paramStart = sql.indexOf("(");
				spl = sql.substring(0, paramStart);
			}
			if (spl.length() < sql.length()) {
				params = sql.substring(spl.length() + 1, sql.length() - 1);
			}
		} else {
			ArgumentTokenizer at = new ArgumentTokenizer(sql, ' ');
			if (at.hasNext()) {
				spl = at.next();
				if (spl.length() < sql.length()) {
					params = sql.substring(spl.length(), sql.length());
				}
			}
		}
		if (params != null) {
			params = params.trim();
		}
		return new String[] { spl, params };
	}

	/**
	 * 从call,calls,spl三种语句中获取SPL文件名
	 * 
	 * @param sql
	 * @return String[]
	 * @throws SQLException
	 */
	public static String getSplName(String sql) throws SQLException {
		sql = trimSql(sql);
		byte sqlType = JDBCUtil.getJdbcSqlType(sql);
		String splName;
		if (sqlType == JDBCConsts.TYPE_CALLS) {
			String[] nameParam = JDBCUtil.getCallsNameParam(sql, null, false);
			splName = nameParam[0];
		} else if (sqlType == JDBCConsts.TYPE_CALL) {
			String[] nameParam = JDBCUtil.getCallNameParam(sql);
			splName = nameParam[0];
		} else if (sqlType == JDBCConsts.TYPE_SPL) {
			String[] nameParam = JDBCUtil.getSplNameParam(sql);
			splName = nameParam[0];
		} else {
			throw new SQLException(JDBCMessage.get().getMessage(
					"error.onlycallsetparam"));
		}
		return splName;
	}

	/**
	 * Preparation parameters
	 * 
	 * @param parameters
	 * @return Sequence
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
	 * @return Sequence
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
			int rowCount = oss.length;
			if (rowCount < 1) {
				return null;
			}
			Object[] firstRow = oss[0];
			int colCount = firstRow.length;
			if (colCount < 1) {
				return null;
			}

			String[] fields = new String[colCount];
			DataStruct ds = new DataStruct();
			for (int c = 0; c < colCount; c++) {
				Object o = firstRow[c];
				if (o != null && (o instanceof String)) {
					fields[c] = o.toString();
				} else {
					fields = null;
					break;
				}
			}
			int dataStart = 1;
			if (fields == null) {
				fields = new String[colCount]; // 数据结构会自动生成字段名
				dataStart = 0;
			}
			ds = new DataStruct(fields);
			Table t = new Table(ds);
			for (int r = dataStart; r < rowCount; r++) {
				t.newLast(oss[r]);
			}
			return t;
		} else if (obj instanceof Object[]) {
			Object[] os = (Object[]) obj;
			for (int j = 0; j < os.length; j++)
				s.add(os[j]);
			return s;
		} else if (obj instanceof List<?>) {
			List<?> list = (List<?>) obj;
			for (int j = 0; j < list.size(); j++)
				s.add(list.get(j));
			return s;
		} else if (obj instanceof Vector<?>) {
			Vector<?> vec = (Vector<?>) obj;
			for (int j = 0; j < vec.size(); j++)
				s.add(vec.get(j));
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
	 * Parse the execute spl statement
	 * 
	 * @param sql
	 * @return String
	 * @throws SQLException
	 */
	private static String parseSpl(String sql) throws SQLException {
		if (sql == null || sql.length() == 0)
			throw new SQLException("The spl name is empty.");
		String[] nameParam = getSplNameParam(sql);
		String spl = nameParam[0];
		String params = nameParam[1];
		sql = JDBCUtil.getCallExp(spl, params);
		return sql;
	}

	/**
	 * 取SPL文件的参数，根据参数名设置call(s)的参数时调用
	 * 
	 * @param procedureNamePattern
	 * @return Table
	 * @throws SQLException
	 */
	public static Table getSplParams(String splPath) throws SQLException {
		Table t = new Table(new String[] { JDBCConsts.PROCEDURE_NAME,
				JDBCConsts.PARAM_LIST });
		try {
			PgmCellSet cs = AppUtil.readCellSet(splPath);
			if (cs != null) {
				ParamList pl = cs.getParamList();
				if (pl != null) {
					String splName = new File(splPath).getName();
					t.newLast(new Object[] { splName, pl });
				}
			}
		} catch (Exception e) {
			throw new SQLException(e.getMessage(), e);
		}
		return t;
	}

	/**
	 * 取SPL文件名
	 * 
	 * @param procedureNamePattern
	 * @return Table
	 * @throws SQLException
	 */
	public static Table getProcedures(String procedureNamePattern)
			throws SQLException {
		Map<String, String> map = getSplList(procedureNamePattern);
		Iterator<String> iter = map.keySet().iterator();
		Table t = new Table(new String[] { JDBCConsts.PROCEDURE_NAME,
				JDBCConsts.PROCEDURE_FILE });
		while (iter.hasNext()) {
			Object key = iter.next();
			Object value = map.get(key);
			t.newLast(new Object[] { value, key });
		}
		return t;
	}

	/**
	 * 取SPL文件和参数，用于元数据
	 * 
	 * @param procedureNamePattern
	 * @return Table
	 * @throws SQLException
	 */
	public static Table getProcedureColumns(String procedureNamePattern,
			String columnNamePattern) throws SQLException {
		Map<String, ParamList> map = getProcedureColumnMap(
				procedureNamePattern, columnNamePattern);
		Iterator<String> iter = map.keySet().iterator();
		Table t = new Table(new String[] { JDBCConsts.PROCEDURE_NAME,
				JDBCConsts.PARAM_LIST });
		while (iter.hasNext()) {
			Object key = iter.next();
			Object value = map.get(key);
			t.newLast(new Object[] { key, value });
		}
		return t;
	}

	/**
	 * 取SPL文件名和参数映射
	 * @param procedureNamePattern
	 * @param columnNamePattern
	 * @return
	 * @throws SQLException
	 */
	public static Map<String, ParamList> getProcedureColumnMap(
			String procedureNamePattern, String columnNamePattern)
			throws SQLException {
		Map<String, String> map = getSplList(procedureNamePattern);
		Map<String, ParamList> paramMap = new HashMap<String, ParamList>();
		Iterator<String> iter = map.keySet().iterator();
		while (iter.hasNext()) {
			String key = iter.next().toString();
			String splPath = key;
			try {
				PgmCellSet cs = AppUtil.readCellSet(splPath);
				if (cs == null)
					continue;
				ParamList pl = cs.getParamList();
				if (pl == null) {
					continue;
				}
				Pattern columnPattern = JDBCUtil.getPattern(columnNamePattern,
						null);
				if (columnPattern != null) {
					ParamList filterParams = new ParamList();
					Param param;
					for (int i = 0; i < pl.count(); i++) {
						param = pl.get(i);
						if (param != null
								&& StringUtils.isValidString(param.getName())) {
							Matcher m = columnPattern.matcher(param.getName());
							if (m.matches()) {
								filterParams.add(param);
							}
						}
					}
					pl = filterParams;
					cs.setParamList(pl);
				}
				String splName = new File(splPath).getName();
				paramMap.put(splName, pl);
			} catch (Exception e) {
				throw new SQLException(e.getMessage(), e);
			}
		}
		return paramMap;
	}

	/**
	 * Get the table names in the specified pattern
	 * 
	 * @param tableNamePattern
	 * @return Table
	 * @throws SQLException
	 */
	public static Table getTables(String tableNamePattern) throws SQLException {
		Map<String, String> map = getTableMap(tableNamePattern);
		Iterator<String> iter = map.keySet().iterator();
		Table t = new Table(new String[] { JDBCConsts.TABLE_NAME });
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
	 * @return Pattern
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
	 * @return Table
	 * @throws SQLException
	 */
	public static Table getColumns(String tableNamePattern,
			String columnNamePattern, Context ctx) throws SQLException {
		Map<String, String> map = getTableMap(tableNamePattern);
		Iterator<String> iter = map.keySet().iterator();
		Table t = new Table(new String[] { JDBCConsts.TABLE_NAME,
				JDBCConsts.COLUMN_NAME, JDBCConsts.DATA_TYPE });

		Pattern columnPattern = JDBCUtil.getPattern(columnNamePattern, null);

		DataStruct ds;
		Sequence data = null;
		while (iter.hasNext()) {
			String key = iter.next().toString();
			String name = map.get(key).toString();
			SimpleSelect ss = new SimpleSelect(null, ctx);
			ICursor cursor = null;
			try {
				cursor = ss.query("select * from "
						+ Escape.addEscAndQuote(name));
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
					colType = com.scudata.common.Types
							.getTypeBySQLType(colType);
					t.newLast(new Object[] { name, fname, colType });
				}
			}
		}
		return t;
	}

	/**
	 * Retrieve files that match the filter.
	 * 
	 * @param filter SQL rules used. "%" means one or more characters, and "_" means
	 *               one character.
	 * @return files map
	 */
	public static Map<String, String> getSplList(String filter) {
		List<String> fileExts = new ArrayList<String>();
		String[] exts = AppConsts.SPL_FILE_EXTS.split(",");
		for (String ext : exts)
			fileExts.add("." + ext);
		return getFiles(filter, fileExts, false);
	}

	/**
	 * Get files
	 * 
	 * @param filter   File name filter
	 * @param fileExts File extensions
	 * @param matchAll 是否需要匹配全路径。FALSE时可以仅匹配文件名。
	 * @return files map
	 */
	public static Map<String, String> getFiles(String filter,
			List<String> fileExts, boolean matchAll) {
		Map<String, String> map = new HashMap<String, String>();
		Pattern pattern = JDBCUtil.getPattern(filter, fileExts);
		String mainPath = Env.getMainPath();
		if (StringUtils.isValidString(mainPath)) {
			File mainDir = new File(mainPath);
			getDirFiles(mainDir.getAbsolutePath().length(), mainDir, map,
					pattern, fileExts, matchAll);
		}
		return map;
	}

	/**
	 * Get the files in the specified path
	 * 
	 * @param rootLen  The length of the parent file path
	 * @param pfile    The parent directory
	 * @param map      Storage file name and title mapping
	 * @param pattern  The Pattern object
	 * @param fileExts File extensions
	 * @param matchAll 是否需要匹配全路径。FALSE时可以仅匹配文件名。
	 */
	private static void getDirFiles(int rootLen, File pfile,
			Map<String, String> map, Pattern pattern, List<String> fileExts,
			boolean matchAll) {
		if (pfile == null)
			return;
		if (pfile.isDirectory()) {
			File[] subFiles = pfile.listFiles();
			if (subFiles == null)
				return;
			for (File sf : subFiles) {
				getDirFiles(rootLen, sf, map, pattern, fileExts, matchAll);
			}
		} else {
			String fileName = pfile.getName();
			if (pfile.isFile()) {
				for (String fileExt : fileExts) {
					if (fileName.toLowerCase().endsWith(fileExt)) {
						if (pattern != null) {
							boolean find;
							if (matchAll) { // 匹配全路径
								fileName = getSubPath(rootLen, pfile);
								find = matchPattern(pattern, pfile, fileName,
										fileExt);
							} else {
								// 匹配文件名
								find = matchPattern(pattern, pfile, fileName,
										fileExt);
								// 匹配全路径
								if (!find) {
									fileName = getSubPath(rootLen, pfile);
									find = matchPattern(pattern, pfile,
											fileName, fileExt);
								}
							}
							if (!find) {
								break;
							}
						}
						if (matchAll) {
							fileName = getSubPath(rootLen, pfile);
						} else {
							fileName = pfile.getName();
							fileName = fileName.substring(0, fileName.length()
									- fileExt.length());
						}
						map.put(pfile.getPath(), fileName);
						break;
					}
				}
			}
		}
	}

	/**
	 * 正则匹配
	 * @param pattern Pattern
	 * @param file 文件
	 * @param fileName 文件名
	 * @param fileExt 文件后缀
	 * @return 是否匹配
	 */
	private static boolean matchPattern(Pattern pattern, File file,
			String fileName, String fileExt) {
		Matcher m;
		boolean find = false;
		// 直接匹配正则表达式
		m = pattern.matcher(fileName);
		find = m.matches();
		if (!find) {
			// pattern可能没加后缀，fileName去掉后缀后正则匹配
			fileName = fileName.substring(0,
					fileName.length() - fileExt.length());
			m = pattern.matcher(fileName);
			find = m.matches();
		}
		if (!find) { // 用文件路径匹配
			String sPattern = pattern.toString();
			if (!sPattern.toLowerCase().endsWith(fileExt)) {
				sPattern += fileExt;
			}
			find = sameFileName(sPattern, file.getAbsolutePath());
		}
		return find;
	}

	/**
	 * Compare whether the two file paths are consistent
	 * 
	 * @param file1 Relative path
	 * @param file2 Absolute path
	 * @return whether the two file paths are consistent
	 */
	private static boolean sameFileName(String file1, String file2) {
		if (file1 == null || file2 == null)
			return false;
		file1 = new File(Env.getMainPath(), file1).getAbsolutePath();
		file2 = new File(file2).getAbsolutePath();
		return file1.equals(file2);
	}

	/**
	 * Get the child path relative to the parent path
	 * 
	 * @param rootLen
	 * @param f
	 * @return the child path relative to the parent path
	 */
	private static final String getSubPath(int rootLen, File f) {
		String path = f.getPath();
		path = path.substring(rootLen);
		while (path.startsWith("\\") || path.startsWith("/")) {
			path = path.substring(1);
		}
		return path;
	}

	/**
	 * Get table names
	 * 
	 * @param filter Table name filter
	 * @return table names map
	 */
	public static Map<String, String> getTableMap(String filter) {
		List<String> fileExts = new ArrayList<String>();
		String[] exts = JDBCConsts.DATA_FILE_EXTS.split(",");
		for (String ext : exts) {
			fileExts.add("." + ext);
		}
		return getFiles(filter, fileExts, true);
	}

	/**
	 * Get the string of the unit names
	 * 
	 * @param spl
	 * @param isOnlyServer
	 * @return String
	 */
	public static String getNodesString(String spl, boolean isOnlyServer,
			List<String> hosts) {
		if (!StringUtils.isValidString(spl)) {
			return null;
		}
		if (!isOnlyServer) {
			LocalFile f = new LocalFile(spl, "s");
			if (f.exists())
				return null;
		}
		// List<String> hosts = Server.getInstance().getHostNames();
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
	 * @return ResultSet
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
	 * @return ResultSet
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
				BaseRecord r = t.getRecord(i);
				if (r != null) {
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
				if (first == null || !(first instanceof BaseRecord)) {
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
					fields = ((BaseRecord) first).dataStruct().getFieldNames();
					types = new int[fields.length];
					initColumnTypes(types);
					dealTypes = new int[fields.length];
					datas = new ArrayList<ArrayList<Object>>(seq.length());
					for (int i = 1; i <= seq.length(); i++) {
						ArrayList<Object> row = new ArrayList<Object>(
								fields.length);
						Object seqi = seq.get(i);
						if (seqi != null && seqi instanceof BaseRecord) {
							BaseRecord r = (BaseRecord) seq.get(i);
							for (int j = 0; j < fields.length; j++) {
								Object o = null;
								try { // 异构数据
									o = r.getFieldValue(fields[j]);
								} catch (Exception ex) {
								}
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
		} else if (obj instanceof BaseRecord) {
			BaseRecord r = (BaseRecord) obj;
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
	 * Get an empty result set.
	 * 
	 * @return ResultSet
	 * @throws SQLException
	 */
	public static ResultSet getEmptyResultSet() throws SQLException {
		return new com.esproc.jdbc.ResultSet(
				(byte) com.esproc.jdbc.ResultSet.GET_EMPTY_RESULT);
	}

	/**
	 * Initialize column data type
	 * 
	 * @param types
	 */
	private static void initColumnTypes(int[] types) {
		if (types == null || types.length == 0)
			return;
		for (int i = 0; i < types.length; i++) {
			types[i] = JDBCConsts.DEFAULT_TYPE;
		}
	}

	/**
	 * Get the SQL type of data object
	 * 
	 * @param o
	 * @param type
	 * @return type
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
	 * @return byte
	 */
	public static byte getProperDataType(Object o) {
		if (o instanceof Integer) {
			return com.scudata.common.Types.DT_INT;
		} else {
			return com.scudata.common.Types.getProperDataType(o);
		}
	}

	/**
	 * Whether integer
	 * 
	 * @param type
	 * @return boolean
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
	 * @return boolean
	 */
	private static boolean isNumberType(int type) {
		return type == java.sql.Types.FLOAT || type == java.sql.Types.DOUBLE
				|| isIntType(type);
	}

	/**
	 * Whether date
	 * 
	 * @param type
	 * @return boolean
	 */
	private static boolean isDateType(int type) {
		return type == java.sql.Types.DATE || type == java.sql.Types.TIME
				|| type == java.sql.Types.TIMESTAMP;
	}

	/**
	 * Convert to SQL type according to raqsoft type
	 * 
	 * @param type
	 * @return int
	 */
	public static int getSQLTypeByType(byte type) {
		switch (type) {
		case com.scudata.common.Types.DT_INT:
			return java.sql.Types.INTEGER;
		case com.scudata.common.Types.DT_SHORT:
			return java.sql.Types.SMALLINT;
		case com.scudata.common.Types.DT_LONG:
		case com.scudata.common.Types.DT_BIGINT:
			return java.sql.Types.BIGINT;
		case com.scudata.common.Types.DT_FLOAT:
			return java.sql.Types.FLOAT;
		case com.scudata.common.Types.DT_DOUBLE:
			return java.sql.Types.DOUBLE;
		case com.scudata.common.Types.DT_DECIMAL:
			return java.sql.Types.DECIMAL;
		case com.scudata.common.Types.DT_DATE:
			return java.sql.Types.DATE;
		case com.scudata.common.Types.DT_TIME:
			return java.sql.Types.TIME;
		case com.scudata.common.Types.DT_DATETIME:
			return java.sql.Types.TIMESTAMP;
		case com.scudata.common.Types.DT_STRING:
			return java.sql.Types.VARCHAR;
		case com.scudata.common.Types.DT_BOOLEAN:
			return java.sql.Types.BOOLEAN;
		case com.scudata.common.Types.DT_BYTE_SERIES:
			return java.sql.Types.BINARY;
		}
		return java.sql.Types.JAVA_OBJECT;
	}

	/**
	 * Get the display name according to the data type
	 * 
	 * @param type
	 * @return String
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
	 * @return ArrayList<Object>
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
	 * @return ArrayList<ArrayList<Object>>
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
	 * @return String
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

	public static boolean isCompatiblesql = false;

	/**
	 * Output debugging information.
	 * 
	 * @param info
	 */
	public static void log(String info) {
		if (isDebugMode)
			Logger.debug(info);
	}

	private static final int MAX_SPL_LENGTH = 65536;

	public static void checkSqlLength(String sql) throws SQLException {
		if (sql == null)
			return;
		if (sql.length() > MAX_SPL_LENGTH) {
			// 语句超出长度限制（{0}）。
			throw new SQLException(JDBCMessage.get().getMessage("error.maxlen",
					MAX_SPL_LENGTH));
		}
	}
}
