package com.scudata.util;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.io.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.scudata.array.IArray;
import com.scudata.common.DBConfig;
import com.scudata.common.DBInfo;
import com.scudata.common.DBSession;
import com.scudata.common.DBTypes;
import com.scudata.common.ISessionFactory;
import com.scudata.common.Logger;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.common.SQLTool;
import com.scudata.common.Sentence;
import com.scudata.dm.*;
import com.scudata.dm.cursor.ICursor;
import com.scudata.expression.Expression;
import com.scudata.resources.DataSetMessage;

public class DatabaseUtil {
	public static int TYPE_ORACLE_TIMESTAMP = 1;
	public static int TYPE_ORACLE_DATE = 2;
	public static int TYPE_SYBASE_TIMESTAMP = 3;

	// added by bdl, 存储过程中，参数的“模式”，分别为输入参数，输出参数和输入输出参数
	// 对于游标，需要设置“输出参数”，而对于非游标的输出参数或者输入输出参数，将把值输出到指定名称的参数
	public static byte PROC_MODE_IN = (byte) 1;
	public static byte PROC_MODE_OUT = (byte) 2;
	public static byte PROC_MODE_INOUT = (byte) 3;

	private static Class<?> oracleTIMESTAMP = null;
	private static Class<?> oracleDATE = null;
	private static Class<?> sybaseTIMESTAMP = null;
	
	private static final byte Col_AutoIncrement = 0x01; // 列自动增长属性, moved from DataStruct by bd, 2017.1.13

	/**
	 * 对指定的数据库连接执行sql语句，返回结果构成的序列
	 * @param sql	String sql语句
	 * @param params	Object[] 参数值列表
	 * @param types	byte[] 参数类型列表，可空，参数类型参见com.scudata.common.Types，可用数组如字符串组等类型
	 * 						当参数类型空时，认为等同于“默认”类型，此时注意参数值不能为null
	 * @param dbs	DBSession
	 * @return Sequence
	 */
	private static Table retrieve(String sql, Object[] params, byte[] types, DBSession dbs, String opt,
			int recordLimit) {
		ResultSet rs = null;
		Statement st = null;
		PreparedStatement pst = null;
		Connection con = null;
		String dbCharset = null;
		String toCharset = null;
		boolean tranSQL = false;
		boolean tranContent = true;
		int dbType = DBTypes.UNKNOWN;
		boolean isSolid = opt != null && opt.indexOf("s") > -1;
		try {
			DBConfig dsConfig = null;
			MessageManager mm = DataSetMessage.get();
			if (dbs != null && dbs.getInfo() instanceof DBConfig) {
				dsConfig = (DBConfig) dbs.getInfo();
			}
			if (dbs != null) {
				Object session = dbs.getSession();
				if (session instanceof Connection) {
					con = (Connection) session;
				}
			}

			if (con == null || con.isClosed()) {
				String name = "";
				DBInfo info = dbs.getInfo();
				if (info != null) {
					name = info.getName();
				}
				throw new RQException(mm.getMessage("error.conClosed", name));
			}

			if (dsConfig != null) {
				dbCharset = dsConfig.getDBCharset();
				tranSQL = dsConfig.getNeedTranSentence();
				tranContent = dsConfig.getNeedTranContent();
				if ((tranContent || tranSQL) && dbCharset == null) {
					String name = "";
					DBInfo info = dbs.getInfo();
					if (info != null) {
						name = info.getName();
					}
					throw new RQException(mm.getMessage("error.fromCharset", name));
				}

				toCharset = dsConfig.getClientCharset();
				if ((tranContent || tranSQL) && toCharset == null) {
					String name = "";
					DBInfo info = dbs.getInfo();
					if (info != null) {
						name = info.getName();
					}
					throw new RQException(mm.getMessage("error.toCharset", name));
				}

				dbType = dsConfig.getDBType();
			} else {
				tranContent = false;
			}
			boolean bb = true;
			if (toCharset != null) {
				bb = toCharset.equalsIgnoreCase(dbCharset) || dbCharset == null;
			}

			if (tranSQL) {
				sql = new String(sql.getBytes(), dbCharset);
			}

			int paramCount = (params == null ? 0 : params.length);
			Object[] args = null;
			byte[] argTypes = null;
			if (paramCount > 0) {
				args = new Object[paramCount];
				argTypes = new byte[paramCount];
				int pos = 0;
				for (int paramIndex = 0; paramIndex < paramCount; paramIndex++) {
					pos = Sentence.indexOf(sql, "?", pos + 1, Sentence.IGNORE_PARS + Sentence.IGNORE_QUOTE);
					args[paramIndex] = params[paramIndex];
					if (types == null || types.length <= paramIndex) {
						argTypes[paramIndex] = com.scudata.common.Types.DT_DEFAULT;
					} else {
						argTypes[paramIndex] = types[paramIndex];
					}
					if (args[paramIndex] == null) {
						continue;
					}
					if (args[paramIndex] instanceof Sequence && tranContent) {
						Sequence l = (Sequence) args[paramIndex];
						for (int i = 1, size = l.length(); i <= size; i++) {
							Object o = l.get(i);
							if (o instanceof String && tranSQL) {
								o = new String(((String) o).getBytes(), dbCharset);
								l.set(i, o);
							}
						}
					} else if (args[paramIndex] instanceof String && tranSQL) {
						args[paramIndex] = new String(((String) args[paramIndex]).getBytes(), dbCharset);
					}
					if (args[paramIndex] instanceof Sequence) {
						Object[] objs = ((Sequence) args[paramIndex]).toArray();
						int objCount = objs.length;
						StringBuffer sb = new StringBuffer(2 * objCount);
						for (int iObj = 0; iObj < objCount; iObj++) {
							sb.append("?,");
						}
						if (sb.length() > 0 && sb.charAt(sb.length() - 1) == ',') {
							sb.deleteCharAt(sb.length() - 1);
						}
						if (sb.length() > 1) {
							sql = sql.substring(0, pos) + sb.toString() + sql.substring(pos + 1);
						}
						pos = pos + sb.length();
					}
				}
			}

			if (isSolid) {
				if (args != null && args.length > 0) {
					isSolid = false;
				}
			}

			try {
				if (isSolid) {
					st = con.createStatement();
				} else {
					pst = con.prepareStatement(sql);
				}
			} catch (SQLException e) {
				if (dbs.getErrorMode()) {
					dbs.setError(e);
				} else {
					String name = "";
					DBInfo info = dbs.getInfo();
					if (info != null) {
						name = info.getName();
					}
					throw new RQException(mm.getMessage("error.sqlException", name, sql) + " : " + e.getMessage(), e);
				}
			}

			if (args != null && args.length > 0) {
				int pos = 0;
				for (int iArg = 0; iArg < args.length; iArg++) {
					pos++;
					try {
						byte type = argTypes[iArg];
						if (args[iArg] != null && args[iArg] instanceof Sequence) {
							Object[] objs = ((Sequence) args[iArg]).toArray();
							for (int iObj = 0; iObj < objs.length; iObj++) {
								SQLTool.setObject(dbType, pst, pos, objs[iObj], type);
								pos++;
							}
							pos--;
						} else {
							SQLTool.setObject(dbType, pst, pos, args[iArg], type);
						}
					} catch (Exception e) {
						String name = "";
						DBInfo info = dbs.getInfo();
						if (info != null) {
							name = info.getName();
						}
						throw new RQException(mm.getMessage("error.argIndex", name, Integer.toString(iArg + 1)));
					}
				}
			}

			try {
				if (isSolid) {
					rs = st.executeQuery(sql);
				} else {
					rs = pst.executeQuery();
				}
			} catch (SQLException e) {
				if (dbs.getErrorMode()) {
					dbs.setError(e);
				} else {
					String name = "";
					DBInfo info = dbs.getInfo();
					if (info != null) {
						name = info.getName();
					}
					throw new RQException(mm.getMessage("error.sqlException", name, sql) + " : " + e.getMessage(), e);
				}
			}
			boolean addTable = false;
			if (opt != null && opt.indexOf("f") > -1) {
				addTable = true;
			}
			Table table = populate(rs, dbCharset, tranContent, toCharset, dbType, addTable, null, false, recordLimit,
					opt);
			if (opt != null && opt.indexOf("t") > -1) {
				String[] fields = null;
				ResultSetMetaData rsmd = rs.getMetaData();
				int colCount = rsmd.getColumnCount();
				String tableName = rsmd.getTableName(1);
				tableName = tranName(tableName, tranContent, dbCharset, toCharset, bb, opt);
				if (tableName == null || tableName.trim().length() < 1) {
					tableName = com.scudata.common.SQLParser.getClause(sql, com.scudata.common.SQLParser.KEY_FROM);
					tableName = removeTilde(tableName, dbs);
				}
				if (sql.indexOf(" as ") < 0) {
					fields = new String[colCount];
					String[][] tableCols = new String[colCount][];
					for (int c = 1; c <= colCount; ++c) {
						String colName = tranName(rsmd.getColumnLabel(c), tranContent, dbCharset, toCharset, bb, opt);
						fields[c - 1] = colName;
						String[] tCol = { colName };
						tableCols[c - 1] = tCol;
					}
				} else {
					String selCols = sql.substring(sql.indexOf("select") + 6, sql.indexOf("from")).trim();
					String[] cols = selCols.split(",");
					if (cols != null && cols.length > 0) {
						int length = cols.length;
						String[][] tableCols = new String[length][];
						fields = new String[length];
						for (int i = 0; i < length; i++) {
							String col = cols[i];
							if (col.indexOf(" as ") < 0) {
								fields[i] = col;
								String[] tCol = { col };
								tableCols[i] = tCol;
							} else {
								String[] sets = col.split(" ");
								fields[i] = sets[sets.length - 1];
								String[] tCol = { sets[0] };
								tableCols[i] = tCol;
							}

						}
					}
				}
				if (opt != null && opt.indexOf("u") > -1) {
					DatabaseMetaData dmd = con.getMetaData();
					try {
						rs = dmd.getPrimaryKeys(con.getCatalog(), null, tableName);
						int count = 0;
						ArrayList<String> nameList = new ArrayList<String>();
						while (rs.next()) {
							String keyName = rs.getString("COLUMN_NAME");
							if (keyName != null && keyName.trim().length() > 0) {
								nameList.add(keyName);
								count++;
							}
						}
						if (count > 0) {
							String[] pks = new String[count];
							for (int i = 0; i < count; i++) {
								pks[i] = (String) nameList.get(i);
							}
							table.setPrimary(pks);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			return table;
		} catch (RQException re) {
			throw re;
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (pst != null) {
					pst.close();
				}
				if (st != null) {
					st.close();
				}
			} catch (Exception e) {
				throw new RQException(e.getMessage(), e);
			}
		}
	}

	/**
	 * 对指定的数据库连接执行sql语句，返回结果集中的第一条记录
	 * 
	 * @param sql
	 *            String sql语句
	 * @param params
	 *            Object[] 参数值列表
	 * @param types
	 *            byte[] 参数类型列表，可空，参数类型参见com.scudata.common.Types，可用数组如字符串组等类型
	 *            当参数类型空时，认为等同于“默认”类型，此时注意参数值不能为null
	 * @param dbs
	 *            DBSession
	 * @return Sequence
	 */
	private static Table retrieveOne(String sql, Object[] params, byte[] types, DBSession dbs, Context ctx,
			String opt) {
		ResultSet rs = null;
		PreparedStatement pst = null;
		Connection con = null;
		String dbCharset = null;
		String toCharset = null;
		boolean tranSQL = false;
		boolean tranContent = true;
		int dbType = DBTypes.UNKNOWN;
		try {
			DBConfig dsConfig = null;
			MessageManager mm = DataSetMessage.get();
			if (dbs != null && dbs.getInfo() instanceof DBConfig) {
				dsConfig = (DBConfig) dbs.getInfo();
			}
			if (dbs != null) {
				Object session = dbs.getSession();
				if (session instanceof Connection) {
					con = (Connection) session;
				}
			}

			if (con == null || con.isClosed()) {
				String name = "";
				DBInfo info = dbs.getInfo();
				if (info != null) {
					name = info.getName();
				}
				throw new RQException(mm.getMessage("error.conClosed", name));
			}

			if (dsConfig != null) {
				dbCharset = dsConfig.getDBCharset();
				tranSQL = dsConfig.getNeedTranSentence();
				tranContent = dsConfig.getNeedTranContent();
				if ((tranContent || tranSQL) && dbCharset == null) {
					String name = "";
					DBInfo info = dbs.getInfo();
					if (info != null) {
						name = info.getName();
					}
					throw new RQException(mm.getMessage("error.fromCharset", name));
				}

				toCharset = dsConfig.getClientCharset();
				if ((tranContent || tranSQL) && toCharset == null) {
					String name = "";
					DBInfo info = dbs.getInfo();
					if (info != null) {
						name = info.getName();
					}
					throw new RQException(mm.getMessage("error.toCharset", name));
				}

				dbType = dsConfig.getDBType();
			} else {
				tranContent = false;
			}

			if (tranSQL) {
				sql = new String(sql.getBytes(), dbCharset);
			}

			int paramCount = (params == null ? 0 : params.length);
			Object[] args = null;
			byte[] argTypes = null;
			if (paramCount > 0) {
				args = new Object[paramCount];
				argTypes = new byte[paramCount];
				int pos = 0;
				for (int paramIndex = 0; paramIndex < paramCount; paramIndex++) {
					pos = Sentence.indexOf(sql, "?", pos + 1, Sentence.IGNORE_PARS + Sentence.IGNORE_QUOTE);
					args[paramIndex] = params[paramIndex];
					if (types == null || types.length <= paramIndex) {
						argTypes[paramIndex] = com.scudata.common.Types.DT_DEFAULT;
					} else {
						argTypes[paramIndex] = types[paramIndex];
					}
					if (args[paramIndex] == null) {
						continue;
					}
					if (args[paramIndex] instanceof Sequence && tranContent) {
						Sequence l = (Sequence) args[paramIndex];
						for (int i = 1, size = l.length(); i <= size; i++) {
							Object o = l.get(i);
							if (o instanceof String && tranSQL) {
								o = new String(((String) o).getBytes(), dbCharset);
								l.set(i, o);
							}
						}
					} else if (args[paramIndex] instanceof String && tranSQL) {
						args[paramIndex] = new String(((String) args[paramIndex]).getBytes(), dbCharset);
					}
					if (args[paramIndex] instanceof Sequence) {
						Object[] objs = ((Sequence) args[paramIndex]).toArray();
						int objCount = objs.length;
						StringBuffer sb = new StringBuffer(2 * objCount);
						for (int iObj = 0; iObj < objCount; iObj++) {
							sb.append("?,");
						}
						if (sb.length() > 0 && sb.charAt(sb.length() - 1) == ',') {
							sb.deleteCharAt(sb.length() - 1);
						}
						if (sb.length() > 1) {
							sql = sql.substring(0, pos) + sb.toString() + sql.substring(pos + 1);
						}
						pos = pos + sb.length();
					}
				}
			}

			try {
				pst = con.prepareStatement(sql);
			} catch (SQLException e) {
				if (dbs.getErrorMode()) {
					dbs.setError(e);
				} else {
					String name = "";
					DBInfo info = dbs.getInfo();
					if (info != null) {
						name = info.getName();
					}
					throw new RQException(mm.getMessage("error.sqlException", name, sql) + " : " + e.getMessage(), e);
				}
			}

			if (args != null && args.length > 0) {
				int pos = 0;
				for (int iArg = 0; iArg < args.length; iArg++) {
					pos++;
					try {
						byte type = argTypes[iArg];
						if (args[iArg] != null && args[iArg] instanceof Sequence) {
							Object[] objs = ((Sequence) args[iArg]).toArray();
							for (int iObj = 0; iObj < objs.length; iObj++) {
								SQLTool.setObject(dbType, pst, pos, objs[iObj], type);
								pos++;
							}
							pos--;
						} else {
							SQLTool.setObject(dbType, pst, pos, args[iArg], type);
						}
					} catch (Exception e) {
						String name = "";
						DBInfo info = dbs.getInfo();
						if (info != null) {
							name = info.getName();
						}
						throw new RQException(mm.getMessage("error.argIndex", name, Integer.toString(iArg + 1)));
					}
				}
			}

			try {
				rs = pst.executeQuery();
			} catch (SQLException e) {
				if (dbs.getErrorMode()) {
					dbs.setError(e);
				} else {
					String name = "";
					DBInfo info = dbs.getInfo();
					if (info != null) {
						name = info.getName();
					}
					throw new RQException(mm.getMessage("error.sqlException", name, sql) + " : " + e.getMessage(), e);
				}
			}

			return populateOne(rs, dbCharset, tranContent, toCharset, dbType, opt);
		} catch (RQException re) {
			throw re;
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (pst != null) {
					pst.close();
				}
			} catch (Exception e) {
				throw new RQException(e.getMessage(), e);
			}
		}
	}

	/**
	 * 对指定的数据库连接执行sql语句，不返回结果集
	 * @param sql	String sql语句
	 * @param params	Object[] 参数值列表
	 * @param types	byte[]
	 * @param dbs	DBSession 上下文 edited by bdl, 2012.9.21, 添加@s选项，设定之后使用固化sql
	 * @param opt	String 选项 edited by bdl, 2012.9.18, 增加返回值
	 * @return 运行结果 更新语句返回Integer，普通sql返回Boolean
	 */
	private static Object runSQL(String sql, Object[] params, byte[] types, DBSession dbs, boolean isupdate,
			String opt) {
		PreparedStatement pst = null;
		Statement st = null;
		Connection con = null;
		String dbCharset = null;
		String toCharset = null;
		boolean tranSQL = false;
		boolean tranContent = true;
		boolean isSolid = opt != null && opt.indexOf("s") > -1;
		int dbType = DBTypes.UNKNOWN;
		try {
			DBConfig dsConfig = null;
			MessageManager mm = DataSetMessage.get();
			if (dbs != null && dbs.getInfo() instanceof DBConfig) {
				dsConfig = (DBConfig) dbs.getInfo();
			}
			if (dbs != null) {
				Object session = dbs.getSession();
				if (session instanceof Connection) {
					con = (Connection) session;
				}
			}

			if (con == null || con.isClosed()) {
				String name = "";
				DBInfo info = dbs.getInfo();
				if (info != null) {
					name = info.getName();
				}
				throw new RQException(mm.getMessage("error.conClosed", name));
			}

			if (dsConfig != null) {
				dbCharset = dsConfig.getDBCharset();
				tranSQL = dsConfig.getNeedTranSentence();
				tranContent = dsConfig.getNeedTranContent();
				if ((tranContent || tranSQL) && dbCharset == null) {
					String name = "";
					DBInfo info = dbs.getInfo();
					if (info != null) {
						name = info.getName();
					}
					throw new RQException(mm.getMessage("error.fromCharset", name));
				}

				toCharset = dsConfig.getClientCharset();
				if ((tranContent || tranSQL) && toCharset == null) {
					String name = "";
					DBInfo info = dbs.getInfo();
					if (info != null) {
						name = info.getName();
					}
					throw new RQException(mm.getMessage("error.toCharset", name));
				}

				dbType = dsConfig.getDBType();
			} else {
				tranContent = false;
			}

			if (tranSQL) {
				sql = new String(sql.getBytes(), dbCharset);
			}

			int paramCount = (params == null ? 0 : params.length);
			Object[] args = null;
			byte[] argTypes = null;
			if (paramCount > 0) {
				args = new Object[paramCount];
				argTypes = new byte[paramCount];
				int pos = 0;
				for (int paramIndex = 0; paramIndex < paramCount; paramIndex++) {
					pos = Sentence.indexOf(sql, "?", pos + 1, Sentence.IGNORE_PARS + Sentence.IGNORE_QUOTE);
					args[paramIndex] = params[paramIndex];
					if (types == null || types.length <= paramIndex) {
						argTypes[paramIndex] = com.scudata.common.Types.DT_DEFAULT;
					} else {
						argTypes[paramIndex] = types[paramIndex];
					}
					if (args[paramIndex] == null) {
						continue;
					}
					if (args[paramIndex] instanceof Sequence && tranContent) {
						Sequence l = (Sequence) args[paramIndex];
						for (int i = 1, size = l.length(); i <= size; i++) {
							Object o = l.get(i);
							if (o instanceof String && tranSQL) {
								o = new String(((String) o).getBytes(), dbCharset);
								l.set(i, o);
							}
						}
					} else if (args[paramIndex] instanceof String && tranSQL) {
						args[paramIndex] = new String(((String) args[paramIndex]).getBytes(), dbCharset);
					}
					if (args[paramIndex] instanceof Sequence) {
						Object[] objs = ((Sequence) args[paramIndex]).toArray();
						int objCount = objs.length;
						StringBuffer sb = new StringBuffer(2 * objCount);
						for (int iObj = 0; iObj < objCount; iObj++) {
							sb.append("?,");
						}
						if (sb.length() > 0 && sb.charAt(sb.length() - 1) == ',') {
							sb.deleteCharAt(sb.length() - 1);
						}
						if (sb.length() > 1) {
							sql = sql.substring(0, pos) + sb.toString() + sql.substring(pos + 1);
						}
						pos = pos + sb.length();
					}
				}
			}

			if (isSolid) {
				if (args != null && args.length > 0) {
					isSolid = false;
				}
			}

			try {
				if (isSolid) {
					st = con.createStatement();
				} else {
					pst = con.prepareStatement(sql);
				}
			} catch (SQLException e) {
				//e.printStackTrace();
				if (dbs.getErrorMode()) {
					dbs.setError(e);
				} else {
					String name = "";
					DBInfo info = dbs.getInfo();
					if (info != null) {
						name = info.getName();
					}
					throw new RQException(mm.getMessage("error.sqlException", name, sql) + " : " + e.getMessage(), e);
				}
			}

			if (args != null && args.length > 0) {
				int pos = 0;
				for (int iArg = 0; iArg < args.length; iArg++) {
					pos++;
					try {
						byte type = argTypes[iArg];
						if (args[iArg] != null && args[iArg] instanceof Sequence) {
							Object[] objs = ((Sequence) args[iArg]).toArray();
							for (int iObj = 0; iObj < objs.length; iObj++) {
								SQLTool.setObject(dbType, pst, pos, objs[iObj], type);
								pos++;
							}
							pos--;
						} else {
							SQLTool.setObject(dbType, pst, pos, args[iArg], type);
						}
					} catch (SQLException e) {
						if (dbs.getErrorMode()) {
							dbs.setError(e);
						} else {
							String name = "";
							DBInfo info = dbs.getInfo();
							if (info != null) {
								name = info.getName();
							}
							throw new RQException(mm.getMessage("error.argIndex", name, Integer.toString(iArg + 1)), e);
						}
					} catch (Exception e) {
						String name = "";
						DBInfo info = dbs.getInfo();
						if (info != null) {
							name = info.getName();
						}
						throw new RQException(mm.getMessage("error.argIndex", name, Integer.toString(iArg + 1)), e);
					}
				}
			}
			Object result = null;

			try {
				if (isupdate) {
					int number = 0;
					if (isSolid) {
						number = st.executeUpdate(sql);
					} else {
						number = pst.executeUpdate();
					}
					result = new Integer(number);
				} else {
					String begin = (sql == null || sql.length() < 6) ? "" : sql.substring(0, 6);
					if (begin.equalsIgnoreCase("insert") || begin.equalsIgnoreCase("update")
							|| begin.equalsIgnoreCase("delete")) {
						int number = 0;
						if (isSolid) {
							number = st.executeUpdate(sql);
						} else {
							number = pst.executeUpdate();
						}
						result = new Integer(number);
					} else {
						boolean success = false;
						if (isSolid) {
							success = st.execute(sql);
						} else {
							success = pst.execute();
						}
						result = Boolean.valueOf(success);
					}
				}
			} catch (SQLException e) {
				if (dbs.getErrorMode()) {
					dbs.setError(e);
				} else {
					String name = "";
					DBInfo info = dbs.getInfo();
					if (info != null) {
						name = info.getName();
					}
					throw new RQException(mm.getMessage("error.sqlException", name, sql) + " : " + e.getMessage(), e);
				}
			}
			return result;
		} catch (RQException re) {
			if (dbs.getErrorMode() && dbs.error() != null) {
				return null;
			}
			else {
				throw re;
			}
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				if (pst != null) {
					pst.close();
				}
				if (st != null) {
					st.close();
				}
			} catch (Exception e) {
				throw new RQException(e.getMessage(), e);
			}
		}
	}

	/**
	 * added by bdl, 2008.11.17，不处理参数中的数组 以指定的参数执行指定的PreparedStatement，不返回结果集
	 * @param pst	PreparedStatement
	 * @param params	Object[]	参数
	 * @param types	byte[]	参数类型
	 * @param dbCharset	String	数据库字符集
	 * @param toCharset	String	终端字符集
	 * @param tranSQL	boolean	SQL是否需转码
	 * @param tranContent	boolean	内容字符是否需转码
	 * @param dbType	int 返回值
	 * @return 是否成功 Boolean
	 */
	private static Boolean runSQL2(PreparedStatement pst, Object[] params, byte[] types, String dbCharset,
			boolean tranSQL, int dbType, String dsName, DBSession dbs) {
		try {
			int paramCount = (params == null ? 0 : params.length);
			Object[] args = null;
			byte[] argTypes = null;
			if (paramCount > 0) {
				args = new Object[paramCount];
				argTypes = new byte[paramCount];
				for (int paramIndex = 0; paramIndex < paramCount; paramIndex++) {
					args[paramIndex] = params[paramIndex];
					if (types == null || types.length <= paramIndex) {
						argTypes[paramIndex] = com.scudata.common.Types.DT_DEFAULT;
					} else {
						argTypes[paramIndex] = types[paramIndex];
					}
					if (args[paramIndex] == null) {
						continue;
					}
					if (args[paramIndex] instanceof String && tranSQL) {
						args[paramIndex] = new String(((String) args[paramIndex]).getBytes(), dbCharset);
					}
				}
			}
			if (args != null && args.length > 0) {
				int pos = 0;
				for (int iArg = 0; iArg < args.length; iArg++) {
					pos++;
					try {
						byte type = argTypes[iArg];
						SQLTool.setObject(dbType, pst, pos, args[iArg], type);
					} catch (SQLException e) {
						if (dbs.getErrorMode()) {
							dbs.setError(e);
						} else {
							MessageManager mm = DataSetMessage.get();
							throw new RQException(mm.getMessage("error.argIndex", dsName, Integer.toString(iArg + 1)), e);
						}
					} catch (Exception e) {
						MessageManager mm = DataSetMessage.get();
						throw new RQException(mm.getMessage("error.argIndex", dsName, Integer.toString(iArg + 1)), e);
					}
				}
			}
			Boolean result = Boolean.FALSE;

			try {
				boolean success = pst.execute();
				result = Boolean.valueOf(success);
			} catch (SQLException e) {
				if (dbs.getErrorMode()) {
					dbs.setError(e);
				} else {
					MessageManager mm = DataSetMessage.get();
					throw new RQException(mm.getMessage("error.sqlException", dsName, "") + " : " + e.getMessage(), e);
				}
			}
			return result;
		} catch (RQException re) {
			throw re;
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		}
	}

	/**
	 * 不处理参数中的数组 以指定的参数执行指定的PreparedStatement，不返回结果集
	 * 
	 * @param pst
	 *            PreparedStatement
	 * @param params
	 *            Object[]
	 * @param types
	 *            byte[]
	 * @param dbCharset
	 *            String
	 * @param toCharset
	 *            String
	 * @param tranSQL
	 *            boolean
	 * @param tranContent
	 *            boolean
	 * @param dbType
	 *            int
	 */
	private static void addBatch(PreparedStatement pst, Object[] params, byte[] types, String dbCharset,
			boolean tranSQL, int dbType, String dsName, DBSession dbs) {
		try {
			int paramCount = (params == null ? 0 : params.length);
			Object[] args = null;
			byte[] argTypes = null;
			if (paramCount > 0) {
				args = new Object[paramCount];
				argTypes = new byte[paramCount];
				for (int paramIndex = 0; paramIndex < paramCount; paramIndex++) {
					args[paramIndex] = params[paramIndex];
					if (types == null || types.length <= paramIndex) {
						argTypes[paramIndex] = com.scudata.common.Types.DT_DEFAULT;
					} else {
						argTypes[paramIndex] = types[paramIndex];
					}
					if (args[paramIndex] == null) {
						continue;
					}
					if (args[paramIndex] instanceof String && tranSQL) {
						args[paramIndex] = new String(((String) args[paramIndex]).getBytes(), dbCharset);
					}
				}
			}
			if (args != null && args.length > 0) {
				int pos = 0;
				for (int iArg = 0; iArg < args.length; iArg++) {
					pos++;
					try {
						byte type = argTypes[iArg];
						SQLTool.setObject(dbType, pst, pos, args[iArg], type);
					} catch (SQLException e) {
						if (dbs.getErrorMode()) {
							dbs.setError(e);
						} else {
							MessageManager mm = DataSetMessage.get();
							e.printStackTrace();
							throw new RQException(mm.getMessage("error.argIndex", dsName, Integer.toString(iArg + 1)), e);
						}
					} catch (Exception e) {
						MessageManager mm = DataSetMessage.get();
						e.printStackTrace();
						throw new RQException(mm.getMessage("error.argIndex", dsName, Integer.toString(iArg + 1)), e);
					}
				}
			}
			pst.addBatch();
		} catch (RQException re) {
			throw re;
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		}
	}

	/**
	 * 取出结果数据集rs的每一行数据生成记录，构成排列返回
	 * 
	 * @param rs
	 *            ResultSet 结果数据集
	 * @param dbCharset
	 *            String
	 * @param needTranContent
	 *            boolean
	 * @param toCharset
	 *            String
	 * @param dbType
	 *            int DBTypes中定义的类型
	 * @param addTable
	 *            boolean 是否在字段名中添加表名，added by bdl, 2010.9.9
	 * @param recordLimit
	 *            added by bdl, 2012.2.27, 返回最大记录数
	 * @throws SQLException
	 * @throws UnsupportedEncodingException
	 * @return Sequence
	 */
	private static Table populate(ResultSet rs, String dbCharset, boolean needTranContent, String toCharset, int dbType,
			boolean addTable, Table table, boolean oneRecord, int recordLimit, String opt)
			throws SQLException, UnsupportedEncodingException {
		if (rs == null) {
			return null;
		}

		ResultSetMetaData rsmd = null;
		try {
			rsmd = rs.getMetaData();
		} catch (Exception e) {
		}
		if (rsmd == null) {
			return null;
		}

		int colCount = rsmd.getColumnCount();

		if (needTranContent && (toCharset == null || toCharset.trim().length() == 0)) {
			MessageManager mm = DataSetMessage.get();
			throw new RQException(mm.getMessage("error.toCharset"));
		}

		boolean bb = true;
		if (toCharset != null) {
			bb = toCharset.equalsIgnoreCase(dbCharset) || dbCharset == null;
		}

		if (table == null) {
			int[] colTypes = new int[colCount];
			String[] colNames = new String[colCount];

			for (int c = 1; c <= colCount; ++c) {
				try {
					if (addTable) {
						String tn = rsmd.getTableName(c);
						tn = tranName(tn, needTranContent, dbCharset, toCharset, bb, opt);
						if (tn == null) {
							tn = "";
						} else {
							tn += "_";
						}
						colNames[c - 1] = tn
								+ tranName(rsmd.getColumnLabel(c), needTranContent, dbCharset, toCharset, bb, opt);
					} else {
						colNames[c - 1] = tranName(rsmd.getColumnLabel(c), needTranContent, dbCharset, toCharset, bb, opt);
					}
					colTypes[c - 1] = rsmd.getColumnType(c);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			table = new Table(colNames);
		}
		if (recordLimit == 0) {
			return table;
		}
		boolean nolimit = recordLimit < 0;
		while (rs.next()) {
			BaseRecord record = table.newLast();
			for (int n = 1; n <= colCount; ++n) {
				int type = 0;
				if (dbType == DBTypes.ORACLE) {
					type = rsmd.getColumnType(n);
				}
				try {
					Object obj = tranData(type, dbType, rs, n, needTranContent, dbCharset, toCharset, bb, opt);
					record.set(n - 1, obj);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (!nolimit) {
				recordLimit--;
				if (recordLimit == 0) {
					return table;
				}
			}

			if (oneRecord) {
				return table;
			}
		}
		return table;
	}

	/**
	 * 取出结果数据集rs的每一行数据生成记录，构成排列返回，本方法用于queryGroup中，多次执行sql返回的数据结构相同
	 * 
	 * @param rs
	 *            ResultSet 结果数据集
	 * @param dbCharset
	 *            String
	 * @param needTranContent
	 *            boolean
	 * @param toCharset
	 *            String
	 * @param dbType
	 *            int DBTypes中定义的类型
	 * @param ctx
	 *            Context 上下文
	 * @param ds
	 *            DataStruct 数据结构，第一次执行时计算
	 * @throws SQLException
	 * @throws UnsupportedEncodingException
	 * @return Sequence
	 */
	private static Sequence populateGroup(ResultSet rs, String dbCharset, boolean needTranContent, String toCharset,
			int dbType, Table table, String opt) throws SQLException, UnsupportedEncodingException {
		if (rs == null) {
			return null;
		}

		if (needTranContent && (toCharset == null || toCharset.trim().length() == 0)) {
			MessageManager mm = DataSetMessage.get();
			throw new RQException(mm.getMessage("error.toCharset"));
		}
		boolean bb = true;
		if (toCharset != null) {
			bb = toCharset.equalsIgnoreCase(dbCharset) || dbCharset == null;
		}

		ResultSetMetaData rsmd = rs.getMetaData();
		int colCount = rsmd.getColumnCount();
		if (table == null) {
			int[] colTypes = new int[colCount];
			String[] colNames = new String[colCount];
			for (int c = 1; c <= colCount; ++c) {
				try {
					colNames[c - 1] = tranName(rsmd.getColumnLabel(c), needTranContent, dbCharset, toCharset, bb, opt);
				} catch (Exception e) {
					e.printStackTrace();
				}
				colTypes[c - 1] = rsmd.getColumnType(c);
			}
			table = new Table(colNames);
		}
		Sequence series = new Sequence();
		while (rs.next()) {
			BaseRecord record = table.newLast();
			series.add(record);
			for (int n = 1; n <= colCount; ++n) {
				int type = 0;
				if (dbType == DBTypes.ORACLE) {
					type = rsmd.getColumnType(n);
				}
				try {
					Object obj = tranData(type, dbType, rs, n, needTranContent, dbCharset, toCharset, bb, opt);
					record.set(n - 1, obj);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return series;
	}

	/**
	 * 取出结果数据集rs的第一条数据生成记录返回
	 * 
	 * @param rs
	 *            ResultSet 结果数据集
	 * @param dbCharset
	 *            String
	 * @param needTranContent
	 *            boolean
	 * @param toCharset
	 *            String
	 * @param dbType
	 *            int DBTypes中定义的类型
	 * @throws SQLException
	 * @throws UnsupportedEncodingException
	 * @return Sequence
	 */
	private static Table populateOne(ResultSet rs, String dbCharset, boolean needTranContent, String toCharset,
			int dbType, String opt) throws SQLException, UnsupportedEncodingException {
		if (rs == null) {
			return null;
		}
		if (needTranContent && (toCharset == null || toCharset.trim().length() == 0)) {
			MessageManager mm = DataSetMessage.get();
			throw new RQException(mm.getMessage("error.toCharset"));
		}

		boolean bb = true;
		if (toCharset != null) {
			bb = toCharset.equalsIgnoreCase(dbCharset) || dbCharset == null;
		}

		ResultSetMetaData rsmd = rs.getMetaData();

		int colCount = rsmd.getColumnCount();
		int[] colTypes = new int[colCount];
		String[] colNames = new String[colCount];

		try {
			for (int c = 1; c <= colCount; ++c) {
				colNames[c - 1] = tranName(rsmd.getColumnLabel(c), needTranContent, dbCharset, toCharset, bb, opt);
				colTypes[c - 1] = rsmd.getColumnType(c);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		Table table = new Table(colNames);
		if (rs.next()) {
			BaseRecord record = table.newLast();
			for (int n = 1; n <= colCount; ++n) {
				int type = 0;
				if (dbType == DBTypes.ORACLE) {
					type = rsmd.getColumnType(n);
				}
				try {
					Object obj = tranData(type, dbType, rs, n, needTranContent, dbCharset, toCharset, bb, opt);
					record.set(n - 1, obj);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return table;
		}
		return null;
	}

	/**
	 * 在dbName中执行sql语句
	 * @param sql	String sql语句
	 * @param params	Object[] 参数列表，可空
	 * @param types	byte[] 参数类型列表，可空，参数类型参见com.scudata.common.Types，可用数组如字符串组等类型
	 *            			当参数类型空时，认为等同于“默认”类型，此时注意参数值不能为null
	 * @param dbs	DBSession edited by bdl, 2012.9.18, 增加返回值
	 * @return	运行结果 更新语句返回Integer，普通sql返回Boolean
	 */
	public static Object execute(String sql, Object[] params, byte[] types, DBSession dbs, String opt) {
		return runSQL(sql, params, types, dbs, false, opt);
	}

	/**
	 * 在dbName中执行sql语句，用多组参数执行同一SQL语句，为提高效率，在执行时不去置换语句中的?，这样就要求参数中不能有数组
	 * @param sql	String sql语句
	 * @param params	Object[][] 参数列表的列表，可空
	 * @param types	byte[] 参数类型列表，可空，参数类型参见com.scudata.common.Types，可用数组如字符串组等类型
	 *            			当参数类型空时，认为等同于“默认”类型，此时注意参数值不能为null
	 * @param dbs	DBSession
	 * @param interrupt	boolean 是否中断
	 */
	public static void execute2old(String sql, Object[][] paramsGroup, byte[] types, DBSession dbs, boolean interrupt) {
		PreparedStatement pst = null;
		Connection con = null;
		String dbCharset = null;
		String toCharset = null;
		boolean tranSQL = false;
		boolean tranContent = true;
		int dbType = DBTypes.UNKNOWN;
		String name = "";
		DBInfo info = dbs.getInfo();
		if (info != null) {
			name = info.getName();
		}
		try {
			DBConfig dsConfig = null;
			MessageManager mm = DataSetMessage.get();
			if (dbs != null && dbs.getInfo() instanceof DBConfig) {
				dsConfig = (DBConfig) dbs.getInfo();
			}
			if (dbs != null) {
				Object session = dbs.getSession();
				if (session instanceof Connection) {
					con = (Connection) session;
				}
			}

			if (con == null || con.isClosed()) {
				throw new RQException(mm.getMessage("error.conClosed", name));
			}

			if (dsConfig != null) {
				dbCharset = dsConfig.getDBCharset();
				tranSQL = dsConfig.getNeedTranSentence();
				tranContent = dsConfig.getNeedTranContent();
				if ((tranContent || tranSQL) && dbCharset == null) {
					throw new RQException(mm.getMessage("error.fromCharset", name));
				}

				toCharset = dsConfig.getClientCharset();
				if ((tranContent || tranSQL) && toCharset == null) {
					throw new RQException(mm.getMessage("error.toCharset", name));
				}

				dbType = dsConfig.getDBType();
			} else {
				tranContent = false;
			}

			if (tranSQL) {
				sql = new String(sql.getBytes(), dbCharset);
			}

			try {
				pst = con.prepareStatement(sql);
			} catch (SQLException e) {
				if (dbs.getErrorMode()) {
					dbs.setError(e);
				} else {
					throw new RQException(mm.getMessage("error.sqlException", name, sql) + " : " + e.getMessage(), e);
				}
			}
			int count = paramsGroup.length;
			for (int i = 0; i < count; i++) {
				Object[] params = paramsGroup[i];
				try {
					runSQL2(pst, params, types, dbCharset, tranSQL, dbType, name, dbs);
				} catch (Exception e) {
					if (interrupt) {
						throw e;
					}
					e.printStackTrace();
				}
			}
		} catch (RQException re) {
			throw re;
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				if (pst != null) {
					pst.close();
				}
			} catch (Exception e) {
				throw new RQException(e.getMessage(), e);
			}
		}
	}

	/**
	 * 在dbName中执行sql语句，用多组参数执行同一SQL语句，为提高效率，在执行时不去置换语句中的?，这样就要求参数中不能有数组
	 * @param sql	String sql语句
	 * @param params	Object[][] 参数列表的列表，可空
	 * @param types	byte[] 参数类型列表，可空，参数类型参见com.scudata.common.Types，可用数组如字符串组等类型
	 *            			当参数类型空时，认为等同于“默认”类型，此时注意参数值不能为null
	 * @param dbs	DBSession
	 * @param interrupt	boolean 是否中断
	 */
	public static void execute2(String sql, Object[][] paramsGroup, byte[] types, DBSession dbs, boolean interrupt) {
		PreparedStatement pst = null;
		Connection con = null;
		String dbCharset = null;
		String toCharset = null;
		boolean tranSQL = false;
		boolean tranContent = true;
		int dbType = DBTypes.UNKNOWN;
		String name = "";
		DBInfo info = dbs.getInfo();
		if (info != null) {
			name = info.getName();
		}
		int batchSize = 1000;

		try {
			DBConfig dsConfig = null;
			MessageManager mm = DataSetMessage.get();
			if (dbs != null && dbs.getInfo() instanceof DBConfig) {
				dsConfig = (DBConfig) dbs.getInfo();
			}
			if (dbs != null) {
				Object session = dbs.getSession();
				if (session instanceof Connection) {
					con = (Connection) session;
				}
			}

			if (con == null || con.isClosed()) {
				throw new RQException(mm.getMessage("error.conClosed", name));
			}

			if (dsConfig != null) {
				batchSize = dsConfig.getBatchSize();
				if (batchSize < 1) {
					batchSize = 1;
				}
				dbCharset = dsConfig.getDBCharset();
				tranSQL = dsConfig.getNeedTranSentence();
				tranContent = dsConfig.getNeedTranContent();
				if ((tranContent || tranSQL) && dbCharset == null) {
					throw new RQException(mm.getMessage("error.fromCharset", name));
				}

				toCharset = dsConfig.getClientCharset();
				if ((tranContent || tranSQL) && toCharset == null) {
					throw new RQException(mm.getMessage("error.toCharset", name));
				}

				dbType = dsConfig.getDBType();
			} else {
				tranContent = false;
			}

			if (tranSQL) {
				sql = new String(sql.getBytes(), dbCharset);
			}

			try {
				pst = con.prepareStatement(sql);
			} catch (SQLException e) {
				if (dbs.getErrorMode()) {
					dbs.setError(e);
				} else {
					throw new RQException(mm.getMessage("error.sqlException", name, sql) + " : " + e.getMessage(), e);
				}
			}
			int count = paramsGroup.length;
			int batch = 1;
			for (int i = 0; i < count; i++) {
				Object[] params = paramsGroup[i];
				if (batchSize <= 1) {
					try {
						runSQL2(pst, params, types, dbCharset, tranSQL, dbType, name, dbs);
					} catch (Exception e) {
						if (interrupt) {
							throw e;
						}
						e.printStackTrace();
					}
				} else if (batch >= batchSize || i == count - 1) {
					try {
						addBatch(pst, params, types, dbCharset, tranSQL, dbType, name, dbs);
						pst.executeBatch();
						pst.clearBatch();
					} catch (SQLException e) {
						if (dbs.getErrorMode()) {
							dbs.setError(e);
						} else {
							if (interrupt) {
								throw e;
							}
							e.printStackTrace();
						}
					} catch (Exception e) {
						if (interrupt) {
							throw e;
						}
						e.printStackTrace();
					}
					batch = 1;
				} else {
					try {
						addBatch(pst, params, types, dbCharset, tranSQL, dbType, name, dbs);
					} catch (Exception e) {
						if (interrupt) {
							throw e;
						}
						e.printStackTrace();
					}
					batch++;
				}
			}
		} catch (RQException re) {
			throw re;
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				if (pst != null) {
					pst.close();
				}
			} catch (Exception e) {
				throw new RQException(e.getMessage(), e);
			}
		}
	}

	/**
	 * 在dbName中执行sql语句，用多组参数执行同一SQL语句，为提高效率， 在执行时不去置换语句中的?，这样就要求参数中不能有数组
	 * 这里一些数据库信息由上层获得，同时sql要求已经处理过所需的字段名转码
	 * 
	 * @param sql
	 *            String sql语句
	 * @param paramsGroup
	 *            Object[][] 参数列表的列表，可空
	 * @param types
	 *            byte[] 参数类型列表，可空，参数类型参见com.scudata.common.Types，可用数组
	 *            如字符串组等类型。当参数类型空时，认为等同于“默认”类型，此时注意参数值不能为null
	 * @param dbs
	 *            DBSession 数据库信息，记录错误状态用
	 * @param con
	 *            Connection 数据库连接对象
	 * @param dbCharset
	 *            数据库编码，用于将字符串参数转码
	 * @param tranSQL
	 *            是否需要转码，只有为true时处理
	 * @param dbType
	 *            数据库类型，对于某些数据库在设定参数时可能需要特殊调整
	 * @param dbn
	 *            数据库名称，用于错误提示
	 * @param batchSize
	 *            int 批处理阈值，从上层获得
	 * @param interrupt
	 *            是否在出错时中断
	 */
	private static void executeBatch(String sql, Object[][] paramsGroup, byte[] types, DBSession dbs, Connection con,
			String dbCharset, boolean tranSQL, int dbType, String dbn, int batchSize, boolean interrupt) {
		PreparedStatement pst = null;
		MessageManager mm = DataSetMessage.get();
		try {
			try {
				pst = con.prepareStatement(sql);
			} catch (SQLException e) {
				if (dbs.getErrorMode()) {
					dbs.setError(e);
				} else {
					throw new RQException(mm.getMessage("error.sqlException", dbn, sql) + " : " + e.getMessage(), e);
				}
			}
			int count = paramsGroup.length;
			int batch = 1;
			for (int i = 0; i < count; i++) {
				Object[] params = paramsGroup[i];
				if (batchSize <= 1) {
					try {
						runSQL2(pst, params, types, dbCharset, tranSQL, dbType, dbn, dbs);
					} catch (Exception e) {
						if (interrupt) {
							throw e;
						}
						e.printStackTrace();
					}
				} else if (batch >= batchSize || i == count - 1) {
					try {
						addBatch(pst, params, types, dbCharset, tranSQL, dbType, dbn, dbs);
						pst.executeBatch();
						pst.clearBatch();
					} catch (SQLException e) {
						e.printStackTrace();
						if (dbs.getErrorMode()) {
							dbs.setError(e);
						} else {
							if (interrupt) {
								throw e;
							}
							e.printStackTrace();
						}
					} catch (Exception e) {
						if (interrupt) {
							throw e;
						}
						e.printStackTrace();
					}
					batch = 1;
				} else {
					try {
						addBatch(pst, params, types, dbCharset, tranSQL, dbType, dbn, dbs);
					} catch (Exception e) {
						if (interrupt) {
							throw e;
						}
						e.printStackTrace();
					}
					batch++;
				}
			}
		} catch (RQException re) {
			throw re;
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				if (pst != null) {
					pst.close();
				}
			} catch (Exception e) {
				throw new RQException(e.getMessage(), e);
			}
		}
	}

	/**
	 * 用多组参数执行同一pst，为提高效率，在执行时不去置换语句中的?，这样就要求参数中不能有数组 完成后执行一次executeBatch，pst不关闭
	 * 
	 * @param pst
	 *            PreparedStatement pst
	 * @param params
	 *            Object[][] 参数列表的列表，可空
	 * @param types
	 *            byte[] 参数类型列表，可空，参数类型参见com.scudata.common.Types，可用数组如字符串组等类型
	 *            当参数类型空时，认为等同于“默认”类型，此时注意参数值不能为null
	 * @param dbs
	 *            DBSession
	 * @param interrupt
	 *            boolean 是否中断
	 */
	private static void executeBatch(PreparedStatement pst, Object[][] paramsGroup, byte[] types, DBSession dbs,
			String dbCharset, boolean tranSQL, int dbType, String name, boolean interrupt) {
		try {
			int count = paramsGroup.length;
			for (int i = 0; i < count; i++) {
				Object[] params = paramsGroup[i];
				try {
					addBatch(pst, params, types, dbCharset, tranSQL, dbType, name, dbs);
				} catch (Exception e) {
					if (interrupt) {
						throw e;
					}
					e.printStackTrace();
				}
			}
			try {
				// edited by bdl, 2014.7.8 异常根据DBSession的设定，@k时可能不抛出
				pst.executeBatch();
				pst.clearBatch();
			} catch (SQLException e) {
				if (dbs.getErrorMode()) {
					dbs.setError(e);
				} else {
					if (interrupt) {
						throw e;
					}
					e.printStackTrace();
				}
			} catch (Exception e) {
				if (interrupt) {
					throw e;
				}
				e.printStackTrace();
			}
		} catch (RQException re) {
			throw re;
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		}
	}

	/**
	 * 在dbName中执行sql语句
	 * @param sql	String sql语句
	 * @param params	Object[] 参数列表，可空
	 * @param types	byte[] 参数类型列表，可空，参数类型参见com.scudata.common.Types，可用数组如字符串组等类型
	 *           			 当参数类型空时，认为等同于“默认”类型，此时注意参数值不能为null
	 * @param dbs	DBSession
	 * @return Sequence
	 */
	public static Sequence query(String sql, Object[] params, byte[] types, DBSession dbs, String opt) {
		// edited by bdl, 2015.7.28, 支持@i选项，单列时返回序列
		Table tbl = retrieve(sql, params, types, dbs, opt, -1);
		if (tbl != null && tbl.dataStruct().getFieldCount() == 1 && opt != null && opt.indexOf('i') != -1) {
			return tbl.fieldValues(0);
		} else {
			return tbl;
		}
	}

	/**
	 * 在dbName中执行sql语句，返回序表
	 * @param sql	String sql语句
	 * @param params	Object[] 参数列表 * @param types byte[] 参数类型，当指定参数为空时有意义
	 * @param dbs	DBSession
	 * @param opt	String 选项
	 * @param ctx	Context 上下文环境
	 * @return Table 返回用结果集生成的序表
	 */
	public static Sequence query(String sql, Object[] params, byte[] types, DBSession dbs, String opt, Context ctx) {
		return query(sql, params, types, dbs, opt, -1, ctx);
	}

	/**
	 * added by bdl, 2012.2.27 在dbName中执行sql语句，返回序表
	 * @param sql	String sql语句
	 * @param params	Object[] 参数列表
	 * @param types	byte[] 参数类型，当指定参数为空时有意义
	 * @param dbs	DBSession
	 * @param opt	String 选项
	 * @param recordLimit	int
	 * @param ctx	Context 上下文环境
	 * @return Table	返回用结果集生成的序表
	 */
	public static Sequence query(String sql, Object[] params, byte[] types, DBSession dbs, String opt, int recordLimit,
			Context ctx) {
		// edited by bdl, 2015.7.28, 支持@i选项，单列时返回序列
		Table tbl = retrieve(sql, params, types, dbs, opt, recordLimit);
		if (tbl != null && tbl.dataStruct().getFieldCount() == 1 && opt != null && opt.indexOf('i') != -1) {
			return tbl.fieldValues(0);
		} else {
			return tbl;
		}
	}

	/**
	 * 以多组参数来执行同一sql语句，返回结果序列的序列，added by bdl, 2009.1.8
	 * @param sql	String sql语句
	 * @param params	Object[][] 参数列表，不可空，若为空则返回null，参数列表中的参数不允许是数组，否则有可能会出错
	 * @param types	byte[] 参数类型列表，可空，参数类型参见com.scudata.common.Types，可用数组如字符串组等类型
	 *            			当参数类型空时，认为等同于“默认”类型，此时注意参数值不能为null
	 * @param dbs	DBSession
	 * @return Sequence
	 */
	public static Sequence queryGroup(String sql, Object[][] params, byte[] types, DBSession dbs, Context ctx,
			String opt) throws RQException {
		if (params == null || params.length < 1) {
			return query(sql, null, types, dbs, null);
		}
		PreparedStatement pst = null;
		Connection con = null;
		String dbCharset = null;
		String toCharset = null;
		boolean tranSQL = false;
		boolean tranContent = true;
		int dbType = DBTypes.UNKNOWN;
		try {
			DBConfig dsConfig = null;
			MessageManager mm = DataSetMessage.get();
			if (dbs != null && dbs.getInfo() instanceof DBConfig) {
				dsConfig = (DBConfig) dbs.getInfo();
			}
			if (dbs != null) {
				Object session = dbs.getSession();
				if (session instanceof Connection) {
					con = (Connection) session;
				}
			}

			if (con == null || con.isClosed()) {
				String name = "";
				DBInfo info = dbs.getInfo();
				if (info != null) {
					name = info.getName();
				}
				throw new RQException(mm.getMessage("error.conClosed", name));
			}

			if (dsConfig != null) {
				dbCharset = dsConfig.getDBCharset();
				tranSQL = dsConfig.getNeedTranSentence();
				tranContent = dsConfig.getNeedTranContent();
				if ((tranContent || tranSQL) && dbCharset == null) {
					String name = "";
					DBInfo info = dbs.getInfo();
					if (info != null) {
						name = info.getName();
					}
					throw new RQException(mm.getMessage("error.fromCharset", name));
				}

				toCharset = dsConfig.getClientCharset();
				if ((tranContent || tranSQL) && toCharset == null) {
					String name = "";
					DBInfo info = dbs.getInfo();
					if (info != null) {
						name = info.getName();
					}
					throw new RQException(mm.getMessage("error.toCharset", name));
				}

				dbType = dsConfig.getDBType();
			} else {
				tranContent = false;
			}

			if (tranSQL) {
				sql = new String(sql.getBytes(), dbCharset);
			}

			Object[] modeParams = params[0];

			int paramCount = (modeParams == null ? 0 : modeParams.length);
			byte[] argTypes = null;
			if (paramCount > 0) {
				argTypes = new byte[paramCount];
				for (int paramIndex = 0; paramIndex < paramCount; paramIndex++) {
					if (types == null || types.length <= paramIndex) {
						argTypes[paramIndex] = com.scudata.common.Types.DT_DEFAULT;
					} else {
						argTypes[paramIndex] = types[paramIndex];
					}
				}
			}

			try {
				pst = con.prepareStatement(sql);
			} catch (SQLException e) {
				if (dbs.getErrorMode()) {
					dbs.setError(e);
				} else {
					String name = "";
					DBInfo info = dbs.getInfo();
					if (info != null) {
						name = info.getName();
					}
					throw new RQException(mm.getMessage("error.sqlException", name, sql) + " : " + e.getMessage(), e);
				}
			}

			Sequence resultSeries = new Sequence();

			Object[] args = new Object[paramCount];
			Table table = null;
			for (int i = 0, iCount = params.length; i < iCount; i++) {
				Object[] thisParams = params[i];
				if (paramCount > 0) {
					for (int paramIndex = 0; paramIndex < paramCount; paramIndex++) {
						args[paramIndex] = thisParams[paramIndex];
						if (args[paramIndex] == null) {
							continue;
						}
						if (args[paramIndex] instanceof String && tranSQL) {
							args[paramIndex] = new String(((String) args[paramIndex]).getBytes(), dbCharset);
						}
					}
				}
				if (args != null && args.length > 0) {
					for (int iArg = 0; iArg < args.length; iArg++) {
						try {
							byte type = argTypes[iArg];
							SQLTool.setObject(dbType, pst, iArg + 1, args[iArg], type);
						} catch (Exception e) {
							String name = "";
							DBInfo info = dbs.getInfo();
							if (info != null) {
								name = info.getName();
							}
							throw new RQException(mm.getMessage("error.argIndex", name, Integer.toString(iArg + 1)));
						}
					}
				}
				ResultSet rs = null;
				if (tranContent && (toCharset == null || toCharset.trim().length() == 0)) {
					throw new RQException(mm.getMessage("error.toCharset"));
				}
				boolean bb = true;
				if (toCharset != null) {
					bb = toCharset.equalsIgnoreCase(dbCharset) || dbCharset == null;
				}

				try {
					rs = pst.executeQuery();
					if (table == null) {
						ResultSetMetaData rsmd = rs.getMetaData();
						int colCount = rsmd.getColumnCount();
						int[] colTypes = new int[colCount];
						String[] colNames = new String[colCount];
						// added by bdl, 2012.10.18, 表名和字段名的转码
						for (int c = 1; c <= colCount; ++c) {
							colNames[c - 1] = tranName(rsmd.getColumnLabel(c), tranContent, dbCharset, toCharset, bb, opt);
							colTypes[c - 1] = rsmd.getColumnType(c);
						}
						table = new Table(colNames);
					}
					resultSeries.add(populateGroup(rs, dbCharset, tranContent, toCharset, dbType, table, opt));
				} catch (SQLException e) {
					if (dbs.getErrorMode()) {
						dbs.setError(e);
					} else {
						String name = "";
						DBInfo info = dbs.getInfo();
						if (info != null) {
							name = info.getName();
						}
						throw new RQException(mm.getMessage("error.sqlException", name, sql) + " : " + e.getMessage(), e);
					}
				} finally {
					if (rs != null) {
						rs.close();
					}
				}
			}
			return resultSeries;
		} catch (RQException re) {
			throw re;
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				if (pst != null) {
					pst.close();
				}
			} catch (Exception e) {
				throw new RQException(e.getMessage(), e);
			}
		}
	}

	/**
	 * 根据多主键查找记录构成序列返回
	 * @param tableName	String 表/视图名
	 * @param fields	String[] 待查询的字段
	 * @param keyNames	String[] 主键名
	 * @param keyValues	Object[][] 多记录的主键值构成的数组
	 * @param dbs	DBSession
	 * @return BaseRecord
	 */
	public static Table query(String tableName, String[] fields, String[] keyNames, Object[][] keyValues, DBSession dbs,
			Context ctx) {
		String sql = "select ";

		int size = fields.length;
		String fieldAll = null;
		String field = null;
		for (int i = 0; i < size; i++) {
			field = addTilde(fields[i], dbs);
			if (field != null && field.trim().length() > 0) {
				if (fieldAll == null) {
					fieldAll = field;
				} else {
					fieldAll += ", " + field;
				}
			}
		}
		if (fieldAll == null || fieldAll.trim().length() < 1) {
			// throw new RQException("Field names is Invalid!");
			MessageManager mm = DataSetMessage.get();
			throw new RQException(mm.getMessage("error.invalidField"));
		}

		size = keyNames.length;
		int colCount = keyValues.length;
		String conditions = null;
		String key = "";
		Object v = null;

		ArrayList<Object> params = new ArrayList<Object>();

		for (int n = 0; n < colCount; n++) {
			Object[] rValues = keyValues[n];
			String condition = null;
			for (int i = 0; i < size; i++) {
				key = keyNames[i];
				if (key != null && key.trim().length() > 0) {
					v = null;
					if (i < rValues.length) {
						v = rValues[i];
					}
					if (condition == null) {
						condition = "(" + key + " = ?)";
						params.add(v);
					} else {
						condition += " and (" + key + " = ?)";
						params.add(v);
					}
				}
			}
			if (condition != null && condition.trim().length() > 0) {
				if (conditions == null) {
					conditions = "(" + condition + ")";
				} else {
					conditions += " or (" + condition + ")";
				}
			}
		}
		if (conditions == null || conditions.trim().length() < 1) {
			MessageManager mm = DataSetMessage.get();
			throw new RQException(mm.getMessage("error.invalidCondition"));
			//throw new RQException("Conditions is Invalid!");
		}
		sql += fieldAll + " from " + addTilde(tableName, dbs) + " where " + conditions;
		return retrieve(sql, params.toArray(), null, dbs, null, -1);
	}

	/**
	 * 根据主键查找记录返回指定字段
	 * @param tableName	String
	 * @param fields	String[] 待查询的字段
	 * @param keyNames	String[] 主键名
	 * @param keyValues	Object[]主键值
	 * @param dbs	DBSession
	 * @return BaseRecord
	 */
	public static Sequence query(String tableName, String[] fields, String[] keyNames, Object[] keyValues,
			DBSession dbs, Context ctx, String opt) {
		String sql = "select ";

		int size = fields.length;
		String fieldAll = null;
		String field = null;
		for (int i = 0; i < size; i++) {
			field = addTilde(fields[i], dbs);
			if (field != null && field.trim().length() > 0) {
				if (fieldAll == null) {
					fieldAll = field;
				} else {
					fieldAll += ", " + field;
				}
			}
		}
		if (fieldAll == null || fieldAll.trim().length() < 1) {
			//throw new RQException("Field names is Invalid!");
			MessageManager mm = DataSetMessage.get();
			throw new RQException(mm.getMessage("error.invalidField"));
		}

		size = keyNames.length;
		String key = "";
		Object v = null;
		String condition = null;
		ArrayList<Object> params = new ArrayList<Object>();
		for (int i = 0; i < size; i++) {
			key = keyNames[i];
			if (key != null && key.trim().length() > 0) {
				v = null;
				if (i < keyValues.length) {
					v = keyValues[i];
				}
				if (condition == null) {
					condition = "(" + key + " = ?)";
					params.add(v);
				} else {
					condition += " and (" + key + " = ?)";
					params.add(v);
				}
			}
		}
		if (condition == null || condition.trim().length() < 1) {
			//throw new RQException("Condition is Invalid!");
			MessageManager mm = DataSetMessage.get();
			throw new RQException(mm.getMessage("error.invalidCondition"));
		}
		sql += fieldAll + " from " + addTilde(tableName, dbs) + " where " + condition;
		Table tbl = retrieveOne(sql, params.toArray(), null, dbs, ctx, opt);
		if (tbl != null && tbl.dataStruct().getFieldCount() == 1 && opt != null && opt.indexOf('i') != -1) {
			return tbl.fieldValues(0);
		} else {
			return tbl;
		}
	}

	/**
	 * 根据主键查找记录返回所有字段
	 * @param tableName	String 表名
	 * @param keyNames	String[] 主键名
	 * @param keyValues	Object[]主键值
	 * @param dbs	DBSession
	 * @return BaseRecord
	 */
	public static Sequence query(String tableName, String[] keyNames, Object[] keyValues, DBSession dbs, Context ctx,
			String opt) {
		String[] fields = { "*" };
		return query(tableName, fields, keyNames, keyValues, dbs, ctx, opt);
	}

	/**
	 * 根据多组主键查找多记录返回所有字段
	 * @param tableName	String 表名
	 * @param keyNames	String[] 主键名
	 * @param keyValues	Object[][] 多记录的主键值构成的数组
	 * @param dbs	DBSession
	 * @return Sequence
	 */
	public static Table query(String tableName, String[] keyNames, Object[][] keyValues, DBSession dbs, Context ctx) {
		String[] fields = { "*" };
		return query(tableName, fields, keyNames, keyValues, dbs, ctx);
	}

	/**
	 * 根据主键生成update或insert语句更新数据
	 * @param tableName	String 表/视图名
	 * @param keyValues	Object[] 主键值
	 * @param fields	String[] 要更新的字段名
	 * @param values	Object[] 要更新的字段值
	 * @param dbs	DBSession
	 */
	public static void update(String tableName, Object[] keyValues, String[] fields, Object[] values, DBSession dbs) {
		update(tableName, keyValues, fields, values, null, dbs);
	}

	/**
	 * 根据主键生成update或insert语句更新数据
	 * @param tableName	String 表/视图名
	 * @param keyValues	Object[] 主键值
	 * @param fields	String[] 要更新的字段名
	 * @param values	Object[] 要更新的字段值
	 * @param opt	String u: 只生成UPDATE, i: 只生成INSERT
	 * @param dbs	DBSession edited by bdl, 2012.9.18, 增加返回值
	 * @return	运行结果 更新语句返回Integer，普通sql返回Boolean
	 */
	public static Object update(String tableName, Object[] keyValues, String[] fields, Object[] values, String opt,
			DBSession dbs) {
		if (tableName == null || tableName.trim().length() < 1) {
			//throw new RQException("Table Name is Invalid!");
			MessageManager mm = DataSetMessage.get();
			throw new RQException(mm.getMessage("error.invalidTable"));
		}
		if (fields == null || fields.length < 1) {
			//throw new RQException("Field Names is Invalid!");
			MessageManager mm = DataSetMessage.get();
			throw new RQException(mm.getMessage("error.invalidField"));
		}
		if (values == null || values.length < 1) {
			//throw new RQException("Field Values is Invalid!");
			MessageManager mm = DataSetMessage.get();
			throw new RQException(mm.getMessage("error.invalidFieldValue"));
		}

		Connection con = null;
		String[] keyNames = null;
		try {
			MessageManager mm = DataSetMessage.get();
			if (dbs != null) {
				Object session = dbs.getSession();
				if (session instanceof Connection) {
					con = (Connection) session;
				}
			}
			if (con == null || con.isClosed()) {
				String dbName = "";
				DBInfo info = dbs.getInfo();
				if (info != null) {
					dbName = info.getName();
				}
				throw new RQException(mm.getMessage("error.conClosed", dbName));
			}
			keyNames = getKeyNames(con, tableName);
		} catch (RQException re) {
			throw re;
		} catch (Exception e) {
			throw new RQException(e);
		}
		if (keyNames == null) {
			return null;
		}

		if (keyNames == null || keyNames.length < 1) {
			//throw new RQException("Key Names is Invalid!");
			MessageManager mm = DataSetMessage.get();
			throw new RQException(mm.getMessage("error.invalidKey"));
		}
		if (keyValues == null || keyValues.length < 1) {
			//throw new RQException("Key Values is Invalid!");
			MessageManager mm = DataSetMessage.get();
			throw new RQException(mm.getMessage("error.invalidKeyValue"));
		}

		byte type = 0;
		if (opt != null) {
			if (opt.toLowerCase().indexOf('u') > -1) {
				type = 1;
			} else if (opt.toLowerCase().indexOf('i') > -1) {
				type = 2;
			}
		}

		int size = keyNames.length;
		String condition = null;
		String key = "";
		Object v = null;
		ArrayList<Object> params = new ArrayList<Object>();
		for (int i = 0; i < size; i++) {
			key = keyNames[i];
			if (key != null && key.trim().length() > 0) {
				v = null;
				if (i < keyValues.length) {
					v = keyValues[i];
				}
				if (condition == null) {
					condition = "(" + key + " = ?)";
					params.add(v);
				} else {
					condition += " and (" + key + " = ?)";
					params.add(v);
				}
			}
		}
		if (condition == null || condition.trim().length() < 1) {
			//throw new RQException("Condition is Invalid!");
			MessageManager mm = DataSetMessage.get();
			throw new RQException(mm.getMessage("error.invalidCondition"));
		}

		String sql = "";
		int n = 0;
		if (type == 0) {
			sql = "select count(*) from " + addTilde(tableName, dbs) + " where " + condition;
			Sequence se = retrieve(sql, params.toArray(), null, dbs, null, -1);
			n = ((Number) ((BaseRecord) se.get(1)).getFieldValue(0)).intValue();
			if (n > 0) {
				type = 1;
			} else {
				type = 2;
			}
		}
		ArrayList<Object> allParams = new ArrayList<Object>();
		if (type == 1) {
			size = fields.length;
			String sets = null;
			String field = "";
			Object fv = null;
			for (int i = 0; i < size; i++) {
				field = fields[i];
				if (field != null && field.trim().length() > 0) {
					fv = null;
					if (i < values.length) {
						fv = values[i];
					}
					if (sets == null) {
						sets = "" + field + " = ?";
						allParams.add(fv);
					} else {
						sets += ", " + field + " = ?";
						allParams.add(fv);
					}
				}
			}
			allParams.addAll(params);
			if (sets == null || sets.trim().length() < 1) {
				//throw new RQException("Field Names of Values is Invalid!");
				MessageManager mm = DataSetMessage.get();
				throw new RQException(mm.getMessage("error.invalidFieldValue"));
			}

			sql = "update " + addTilde(tableName, dbs) + " set " + sets + " where " + condition;
		} else {
			String fieldAll = null;
			String sets = null;
			String field = "";
			Object fv = null;
			size = keyNames.length;
			for (int i = 0; i < size; i++) {
				field = keyNames[i];
				if (field != null && field.trim().length() > 0) {
					fv = null;
					if (i < keyValues.length) {
						fv = keyValues[i];
					}
					if (sets == null) {
						fieldAll = "(" + field;
						sets = "( ?";
						allParams.add(fv);
					} else {
						fieldAll += "," + field;
						sets += ", ?";
						allParams.add(fv);
					}
				}
			}
			size = fields.length;
			for (int i = 0; i < size; i++) {
				field = fields[i];
				if (field == null) {
					continue;
				}
				boolean exist = false;
				for (int j = 0; j < keyNames.length; j++) {
					if (field.equalsIgnoreCase(keyNames[j])) {
						exist = true;
						break;
					}
				}
				if (exist) {
					continue;
				}
				if (field.trim().length() > 0) {
					fv = null;
					if (i < values.length) {
						fv = values[i];
					}
					if (sets == null) {
						fieldAll = "(" + field;
						sets = "( ?";
						allParams.add(fv);
					} else {
						fieldAll += "," + field;
						sets += ", ?";
						allParams.add(fv);
					}
				}
			}
			if (sets == null || sets.trim().length() < 1) {
				//throw new RQException("Field Values of Values is Invalid!");
				MessageManager mm = DataSetMessage.get();
				throw new RQException(mm.getMessage("error.invalidFieldValue"));
			} else {
				sets += " )";
			}
			if (fieldAll == null || fieldAll.trim().length() < 1) {
				//throw new RQException("Field Names of Values is Invalid!");
				MessageManager mm = DataSetMessage.get();
				throw new RQException(mm.getMessage("error.invalidFieldValue"));
			} else {
				fieldAll += " )";
			}
			sql = "insert into " + addTilde(tableName, dbs) + " " + fieldAll + " values " + sets;
		}
		if (dbs != null) {
			return runSQL(sql, allParams.toArray(), null, dbs, true, opt);
		} else {
			String dbName = "";
			MessageManager mm = DataSetMessage.get();
			throw new RQException(mm.getMessage("error.conClosed", dbName));
		}
	}

	/**
	 * 结果集数据转换
	 * @param type	数据类型
	 * @param dbType	数据库类型
	 * @param rs	结果集
	 * @param index	数据序号
	 * @param needTranContent	是否需要转换字符
	 * @param dbCharset	数据库字符集
	 * @param toCharset	终端字符集
	 * @param bb	字符集是否一致
	 * @return
	 * @throws Exception
	 */
	public static Object tranData(int type, int dbType, ResultSet rs, int index, boolean needTranContent,
			String dbCharset, String toCharset, boolean bb) throws Exception {
		return tranData(type, dbType, rs, index, needTranContent, dbCharset, toCharset, bb, null);
	}

	/**
	 * 结果集数据转换
	 * @param type	数据类型
	 * @param dbType	数据库类型
	 * @param rs	结果集
	 * @param index	数据序号
	 * @param needTranContent	是否需要转换字符
	 * @param dbCharset	数据库字符集
	 * @param toCharset	终端字符集
	 * @param bb	字符集是否一致
	 * @param opt	函数选项
	 * @return
	 * @throws Exception
	 */
	public static Object tranData(int type, int dbType, ResultSet rs, int index, boolean needTranContent,
			String dbCharset, String toCharset, boolean bb, String opt) throws Exception {
		if (dbType == DBTypes.ORACLE && oracleTIMESTAMP == null) {
			try {
				oracleTIMESTAMP = Class.forName("oracle.sql.TIMESTAMP");
				oracleDATE = Class.forName("oracle.sql.DATE");
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (dbType == DBTypes.SYBASE && sybaseTIMESTAMP == null) {
			try {
				sybaseTIMESTAMP = Class.forName("com.sybase.jdbc2.tds.SybTimestamp");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		Object obj = null;
		if (type == -1) {
			Reader rd = rs.getCharacterStream(index);
			obj = "";
			int c;
			StringBuffer sb = new StringBuffer();
			try {
				while ((c = rd.read()) != -1) {
					sb.append((char) c);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			obj = new String(sb.toString());
		} else if (type == java.sql.Types.DATE) {
			obj = rs.getTimestamp(index);
		} else {
			obj = rs.getObject(index);
		}

		if (obj == null)
			return null;
		
		if (obj instanceof Number) {
			if(obj instanceof java.math.BigDecimal){
				if (opt != null && opt.indexOf('d') > -1)
					return ((Number)obj).doubleValue();
			} else if (obj instanceof Integer | obj instanceof Long || obj instanceof Double){
				return obj;
			} else if (obj instanceof java.math.BigInteger) {
				java.math.BigInteger bi = (java.math.BigInteger) obj;
				return new java.math.BigDecimal(bi);
			} else if (obj instanceof Byte || obj instanceof Short) {
				return ((Number)obj).intValue();
			} else if (obj instanceof Float) {
				return ((Number)obj).doubleValue();
			}
		} else if (obj instanceof String && !bb) {
			try {
				if (needTranContent) {
					return new String(((String) obj).getBytes(dbCharset), toCharset);
				}
			} catch (Exception e) {
				MessageManager mm = DataSetMessage.get();
				throw new RQException(mm.getMessage("error.charset", dbCharset, toCharset));
			}
		} else if (obj instanceof Blob) {
			Blob blob = (Blob) obj;
			InputStream is = new BufferedInputStream(blob.getBinaryStream());
			try {
				ByteArrayOutputStream baos = new ByteArrayOutputStream(8096);
				byte[] buf = new byte[8096];
				while (true) {
					int c1 = is.read(buf);
					if (c1 < 0) {
						break;
					}
					baos.write(buf, 0, c1);
				}
				return baos.toByteArray();
			} catch (Exception e) {
				if (is != null) {
					try {
						is.close();
					} catch (Exception e1) {
					}
				}
			}
		} else if (obj instanceof Clob) {
			Clob clob = (Clob) obj;
			StringBuffer sb = new StringBuffer((int) clob.length());
			BufferedReader br = new BufferedReader(clob.getCharacterStream());
			try {
				char[] buf = new char[8096];
				while (true) {
					int c1 = br.read(buf);
					if (c1 < 0) {
						break;
					}
					sb.append(buf, 0, c1);
				}
				return sb.toString();
			} catch (Exception e) {
				if (br != null) {
					try {
						br.close();
					} catch (Exception el) {
					}
				}
			}
		} else if (obj instanceof LocalDateTime) {
			// added by bd, 2023.10.17, 添加对java.time.LocalDateTime的处理，这是一个不含时区信息的日期时间数据
			LocalDateTime ldt = (LocalDateTime) obj;
			ZonedDateTime zdt = ldt.atZone(ZoneId.systemDefault());
			return Timestamp.from(zdt.toInstant());
		} else if (obj instanceof LocalDate) {
			// added by bd, 2023.2.3, 添加对java.time.LocalDate的处理，这是一个不含时区信息的日期数据
			LocalDate ldt = (LocalDate) obj;
			ZonedDateTime zdt = ldt.atStartOfDay(ZoneId.systemDefault());
			return Timestamp.from(zdt.toInstant());
		} else if (dbType == DBTypes.ORACLE && oracleTIMESTAMP != null && oracleTIMESTAMP.isInstance(obj)) {
			return TranOracle.tran(TYPE_ORACLE_TIMESTAMP, obj);
		} else if (dbType == DBTypes.ORACLE && oracleDATE != null && oracleDATE.isInstance(obj)) {
			return TranOracle.tran(TYPE_ORACLE_DATE, obj);
		} else if (dbType == DBTypes.SYBASE && sybaseTIMESTAMP != null && sybaseTIMESTAMP.isInstance(obj)) {
			return TranSybase.tran(TYPE_SYBASE_TIMESTAMP, obj);
		}

		return obj;
	}

	private static String tranName(String name, boolean needTranContent, String dbCharset, String toCharset,
			boolean bb, String opt)
			throws Exception {
		String result = name;
		if (name != null && !bb) {
			try {
				if (needTranContent) {
					result = new String(name.getBytes(dbCharset), toCharset);
				}
			} catch (Exception e) {
				MessageManager mm = DataSetMessage.get();
				throw new RQException(mm.getMessage("error.charset", dbCharset, toCharset));
			}
		}
		if (result != null && opt != null && opt.indexOf("l") > -1) {
			result = result.toLowerCase();
		}
		return result;
	}

	private static String[] getKeyNames(Connection conn, String tableName) {
		if (conn == null || tableName == null || tableName.trim().length() < 1) {
			return null;
		}
		ResultSet rs = null;
		try {
			DatabaseMetaData dmd = conn.getMetaData();
			rs = dmd.getPrimaryKeys(conn.getCatalog(), null, tableName);
			int count = 0;
			ArrayList<String> nameList = new ArrayList<String>();
			ArrayList<Object> seqList = new ArrayList<Object>();
			while (rs.next()) {
				String keyName = rs.getString("COLUMN_NAME");
				Object seqObj = rs.getObject("KEY_SEQ");
				if (keyName != null && keyName.trim().length() > 0) {
					nameList.add(keyName);
					seqList.add(seqObj);
					count++;
				}
			}
			if (count > 0) {
				Object[] names0 = nameList.toArray();
				if (names0 == null || names0.length < 1) {
					return null;
				}
				String[] names = new String[count];
				int[] seqs = new int[count];
				for (int i = 0; i < count; i++) {
					seqs[i] = Integer.parseInt(seqList.get(i).toString());
					names[i] = names0[i].toString();
				}
				for (int i = 0; i < count - 1; i++) {
					for (int j = 0; j < count - 1 - i; j++) {
						if (seqs[j] > seqs[j + 1]) {
							int tmp = seqs[j];
							String tmps = names[j];
							seqs[j] = seqs[j + 1];
							names[j] = names[j + 1];
							seqs[j + 1] = tmp;
							names[j + 1] = tmps;
						}
					}
				}
				return names;
			}
			return null;
		} catch (RQException re) {
			throw re;
		} catch (Exception e) {
			throw new RQException(e);
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
			} catch (Exception e) {
				throw new RQException(e.getMessage(), e);
			}
		}
	}

	/**
	 * 用SQL在数据库中根据主键值组查询str，并返回结果
	 * @param tableName	String 表名
	 * @param str	String sql表达式
	 * @param keyValues	Object[] 主键字段对应值
	 * @param dbs	DBSession 数据库Session
	 * @return Object 查询结果
	 */
	public static Sequence select(String tableName, String str, Object[] keyValues, DBSession dbs, String opt) {
		return select(tableName, str, null, keyValues, dbs, opt);
	}

	/**
	 * 用SQL在数据库中根据主键值组查询str，并返回结果
	 * @param tableName	String 表名
	 * @param str	String sql表达式
	 * @param keyNames	主键名称
	 * @param keyValues	Object[] 主键字段对应值
	 * @param dbs	DBSession 数据库Session
	 * @return Object 查询结果
	 */
	public static Sequence select(String tableName, String str, String[] keyNames, Object[] keyValues, DBSession dbs,
			String opt) {
		DBInfo info = dbs.getInfo();
		String dbName = "";
		if (info != null) {
			dbName = info.getName();
		}
		String sql = "select " + str;
		Connection con = null;
		try {
			MessageManager mm = DataSetMessage.get();
			if (dbs != null) {
				Object session = dbs.getSession();
				if (session instanceof Connection) {
					con = (Connection) session;
				}
			}
			if (con == null || con.isClosed()) {
				throw new RQException(mm.getMessage("error.conClosed", dbName));
			}
		} catch (RQException re) {
			throw re;
		} catch (Exception e) {
			throw new RQException(e);
		}
		if (keyNames == null) {
			keyNames = getKeyNames(con, tableName);
		}
		if (keyNames == null) {
			return null;
		}

		int size = keyNames.length;
		String key = "";
		Object v = null;
		String condition = null;
		ArrayList<Object> params = new ArrayList<Object>();
		for (int i = 0; i < size; i++) {
			key = keyNames[i];
			if (key != null && key.trim().length() > 0) {
				v = null;
				if (i < keyValues.length) {
					v = keyValues[i];
				}
				if (condition == null) {
					condition = "(" + key + " = ?)";
					params.add(v);
				} else {
					condition += " and (" + key + " = ?)";
					params.add(v);
				}
			}
		}
		if (condition == null || condition.trim().length() < 1) {
			//throw new RQException("Condition is Invalid!");
			MessageManager mm = DataSetMessage.get();
			throw new RQException(mm.getMessage("error.invalidCondition"));
		}
		sql += " from " + addTilde(tableName, dbs) + " where " + condition;
		return retrieve2(sql, params.toArray(), null, dbs, opt);
	}

	/**
	 * 用SQL在数据库中根据主键值组查询str构成数组，并返回结果构成序列
	 * @param tableName	String 表名
	 * @param str	String sql表达式
	 * @param keyValues	Object[] 主键字段对应值
	 * @param dbs	DBSession 数据库Session
	 * @return Object	查询结果
	 */
	public static Sequence select(String tableName, String[] strs, String[] keyNames, Object[] keyValues, DBSession dbs,
			String opt) {
		DBInfo info = dbs.getInfo();
		String dbName = "";
		if (info != null) {
			dbName = info.getName();
		}
		String sql = "select ";

		int size = strs.length;
		String fieldAll = null;
		String field = null;
		for (int i = 0; i < size; i++) {
			field = strs[i];
			if (field != null && field.trim().length() > 0) {
				if (fieldAll == null) {
					fieldAll = field;
				} else {
					fieldAll += ", " + field;
				}
			}
		}
		if (fieldAll == null || fieldAll.trim().length() < 1) {
			//throw new RQException("SQL strings are Invalid!");
			MessageManager mm = DataSetMessage.get();
			throw new RQException(mm.getMessage("error.invalidFieldValue"));
		}

		Connection con = null;
		try {
			MessageManager mm = DataSetMessage.get();
			if (dbs != null) {
				Object session = dbs.getSession();
				if (session instanceof Connection) {
					con = (Connection) session;
				}
			}
			if (con == null || con.isClosed()) {
				throw new RQException(mm.getMessage("error.conClosed", dbName));
			}
		} catch (RQException re) {
			throw re;
		} catch (Exception e) {
			throw new RQException(e);
		}
		if (keyNames == null) {
			keyNames = getKeyNames(con, tableName);
		}
		if (keyNames == null) {
			return null;
		}

		size = keyNames.length;
		String key = "";
		Object v = null;
		String condition = null;
		ArrayList<Object> params = new ArrayList<Object>();
		for (int i = 0; i < size; i++) {
			key = keyNames[i];
			if (key != null && key.trim().length() > 0) {
				v = null;
				if (i < keyValues.length) {
					v = keyValues[i];
				}
				if (condition == null) {
					condition = "(" + key + " = ?)";
					params.add(v);
				} else {
					condition += " and (" + key + " = ?)";
					params.add(v);
				}
			}
		}
		if (condition == null || condition.trim().length() < 1) {
			//throw new RQException("Condition is Invalid!");
			MessageManager mm = DataSetMessage.get();
			throw new RQException(mm.getMessage("error.invalidCondition"));
		}
		sql += fieldAll + " from " + addTilde(tableName, dbs) + " where " + condition;
		return retrieve2(sql, params.toArray(), null, dbs, opt);
	}

	/**
	 * 用SQL在数据库中根据主键值组查询str构成数组，并返回结果构成序列
	 * @param tableName	String 表名
	 * @param str	String sql表达式
	 * @param keyValues	Object[] 主键字段对应值
	 * @param dbs	DBSession 数据库Session
	 * @return 	Object 查询结果
	 */
	public static Sequence select(String tableName, String[] strs, Object[] keyValues, DBSession dbs, String opt) {
		return select(tableName, strs, null, keyValues, dbs, opt);
	}

	/**
	 * 对指定的数据库连接执行sql语句，返回结果集中的记录序列，如果只有一个Field一条记录，直接返回值，否则返回值的序列，
	 * 遇到多条Field多条记录，则返回值序列的序列
	 * @param sql
	 *            String sql语句
	 * @param params
	 *            Object[] 参数值列表
	 * @param types
	 *            byte[] 参数类型列表，可空，参数类型参见com.scudata.common.Types，可用数组如字符串组等类型
	 *            当参数类型空时，认为等同于“默认”类型，此时注意参数值不能为null
	 * @param dbs
	 *            DBSession
	 * @return Sequence
	 */
	private static Sequence retrieve2(String sql, Object[] params, byte[] types, DBSession dbs, String opt) {
		ResultSet rs = null;
		PreparedStatement pst = null;
		Connection con = null;
		String dbCharset = null;
		String toCharset = null;
		boolean tranSQL = false;
		boolean tranContent = true;
		int dbType = DBTypes.UNKNOWN;
		try {
			DBConfig dsConfig = null;
			MessageManager mm = DataSetMessage.get();
			if (dbs != null && dbs.getInfo() instanceof DBConfig) {
				dsConfig = (DBConfig) dbs.getInfo();
			}
			if (dbs != null) {
				Object session = dbs.getSession();
				if (session instanceof Connection) {
					con = (Connection) session;
				}
			}

			if (con == null || con.isClosed()) {
				String name = "";
				DBInfo info = dbs.getInfo();
				if (info != null) {
					name = info.getName();
				}
				throw new RQException(mm.getMessage("error.conClosed", name));
			}

			if (dsConfig != null) {
				dbCharset = dsConfig.getDBCharset();
				tranSQL = dsConfig.getNeedTranSentence();
				tranContent = dsConfig.getNeedTranContent();
				if ((tranContent || tranSQL) && dbCharset == null) {
					String name = "";
					DBInfo info = dbs.getInfo();
					if (info != null) {
						name = info.getName();
					}
					throw new RQException(mm.getMessage("error.fromCharset", name));
				}

				toCharset = dsConfig.getClientCharset();
				if ((tranContent || tranSQL) && toCharset == null) {
					String name = "";
					DBInfo info = dbs.getInfo();
					if (info != null) {
						name = info.getName();
					}
					throw new RQException(mm.getMessage("error.toCharset", name));
				}

				dbType = dsConfig.getDBType();
			} else {
				tranContent = false;
			}

			if (tranSQL) {
				sql = new String(sql.getBytes(), dbCharset);
			}

			int paramCount = (params == null ? 0 : params.length);
			Object[] args = null;
			byte[] argTypes = null;
			if (paramCount > 0) {
				args = new Object[paramCount];
				argTypes = new byte[paramCount];
				int pos = 0;
				for (int paramIndex = 0; paramIndex < paramCount; paramIndex++) {
					pos = Sentence.indexOf(sql, "?", pos + 1, Sentence.IGNORE_PARS + Sentence.IGNORE_QUOTE);
					args[paramIndex] = params[paramIndex];
					if (types == null || types.length <= paramIndex) {
						argTypes[paramIndex] = com.scudata.common.Types.DT_DEFAULT;
					} else {
						argTypes[paramIndex] = types[paramIndex];
					}
					if (args[paramIndex] == null) {
						continue;
					}
					if (args[paramIndex] instanceof Sequence && tranContent) {
						Sequence l = (Sequence) args[paramIndex];
						for (int i = 1, size = l.length(); i <= size; i++) {
							Object o = l.get(i);
							if (o instanceof String && tranSQL) {
								o = new String(((String) o).getBytes(), dbCharset);
								l.set(i, o);
							}
						}
					} else if (args[paramIndex] instanceof String && tranSQL) {
						args[paramIndex] = new String(((String) args[paramIndex]).getBytes(), dbCharset);
					}
					if (args[paramIndex] instanceof Sequence) {
						Object[] objs = ((Sequence) args[paramIndex]).toArray();
						int objCount = objs.length;
						StringBuffer sb = new StringBuffer(2 * objCount);
						for (int iObj = 0; iObj < objCount; iObj++) {
							sb.append("?,");
						}
						if (sb.length() > 0 && sb.charAt(sb.length() - 1) == ',') {
							sb.deleteCharAt(sb.length() - 1);
						}
						if (sb.length() > 1) {
							sql = sql.substring(0, pos) + sb.toString() + sql.substring(pos + 1);
						}
						pos = pos + sb.length();
					}
				}
			}

			try {
				pst = con.prepareStatement(sql);
			} catch (SQLException e) {
				if (dbs.getErrorMode()) {
					dbs.setError(e);
				} else {
					String name = "";
					DBInfo info = dbs.getInfo();
					if (info != null) {
						name = info.getName();
					}
					throw new RQException(mm.getMessage("error.sqlException", name, sql) + " : " + e.getMessage(), e);

				}
			}

			if (args != null && args.length > 0) {
				int pos = 0;
				for (int iArg = 0; iArg < args.length; iArg++) {
					pos++;
					try {
						byte type = argTypes[iArg];
						if (args[iArg] != null && args[iArg] instanceof Sequence) {
							Object[] objs = ((Sequence) args[iArg]).toArray();
							for (int iObj = 0; iObj < objs.length; iObj++) {
								SQLTool.setObject(dbType, pst, pos, objs[iObj], type);
								pos++;
							}
							pos--;
						} else {
							SQLTool.setObject(dbType, pst, pos, args[iArg], type);
						}
					} catch (Exception e) {
						String name = "";
						DBInfo info = dbs.getInfo();
						if (info != null) {
							name = info.getName();
						}
						throw new RQException(mm.getMessage("error.argIndex", name, Integer.toString(iArg + 1)));
					}
				}
			}

			try {
				rs = pst.executeQuery();
			} catch (SQLException e) {
				if (dbs.getErrorMode()) {
					dbs.setError(e);
				} else {
					String name = "";
					DBInfo info = dbs.getInfo();
					if (info != null) {
						name = info.getName();
					}
					throw new RQException(mm.getMessage("error.sqlException", name, sql) + " : " + e.getMessage(), e);
				}
			}
			return populate2(rs, dbCharset, tranContent, toCharset, dbType, opt);
		} catch (RQException re) {
			throw re;
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (pst != null) {
					pst.close();
				}
			} catch (Exception e) {
				throw new RQException(e.getMessage(), e);
			}
		}
	}

	/**
	 * 对指定的数据库连接执行sql语句，返回结果集中的记录序列，如果只有一个Field一条记录，直接返回值，否则返回值的序列，
	 * 遇到多条Field多条记录，则返回值序列的序列
	 * @param rs
	 *            ResultSet 结果数据集
	 * @param dbCharset
	 *            String
	 * @param needTranContent
	 *            boolean
	 * @param toCharset
	 *            String
	 * @param dbType
	 *            int DBTypes中定义的类型
	 * @throws SQLException
	 * @throws UnsupportedEncodingException
	 * @return Sequence
	 */
	private static Sequence populate2(ResultSet rs, String dbCharset, boolean needTranContent, String toCharset,
			int dbType, String opt) throws SQLException, UnsupportedEncodingException {
		if (rs == null) {
			return null;
		}

		ResultSetMetaData rsmd = rs.getMetaData();

		int colCount = rsmd.getColumnCount();

		if (needTranContent && (toCharset == null || toCharset.trim().length() == 0)) {
			MessageManager mm = DataSetMessage.get();
			throw new RQException(mm.getMessage("error.toCharset"));
		}

		boolean bb = true;
		if (toCharset != null) {
			bb = toCharset.equalsIgnoreCase(dbCharset) || dbCharset == null;
		}
		Sequence series = new Sequence();
		int size = 0;
		Object value = null;
		while (rs.next()) {
			if (colCount == 1) {
				int type = 0;
				if (dbType == DBTypes.ORACLE) {
					type = rsmd.getColumnType(1);
				}
				try {
					value = tranData(type, dbType, rs, 1, needTranContent, dbCharset, toCharset, bb, opt);
				} catch (Exception e) {
					e.printStackTrace();
				}
				series.add(value);
			} else {
				Sequence sub = new Sequence();
				for (int n = 1; n <= colCount; ++n) {
					int type = 0;
					if (dbType == DBTypes.ORACLE) {
						type = rsmd.getColumnType(n);
					}
					try {
						Object obj = tranData(type, dbType, rs, n, needTranContent, dbCharset, toCharset, bb, opt);
						sub.add(obj);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				value = sub;
				series.add(value);
			}
			size++;
		}
		if (size < 2) {
			Sequence ser = new Sequence();
			ser.add(value);
			return ser;
		} else {
			return series;
		}
	}

	/**
	 * 根据主键生成update或insert语句更新数据
	 * @param tableName	String 表/视图名
	 * @param str	String 要更新的SQL语句
	 * @param keyNames	String[] 主键名
	 * @param keyValues	Object[] 主键值
	 * @param dbs	DBSession edited by bdl, 2012.9.18, 增加返回值
	 * @return 	运行结果 更新语句返回Integer，普通sql返回Boolean
	 */
	public static Object update(String tableName, String str, Object[] params, byte[] types, Object[] keyValues,
			DBSession dbs) {
		if (tableName == null || tableName.trim().length() < 1) {
			//throw new RQException("Table Name is Invalid!");
			MessageManager mm = DataSetMessage.get();
			throw new RQException(mm.getMessage("error.invalidTable"));
		}
		Connection con = null;
		String[] keyNames = null;
		try {
			MessageManager mm = DataSetMessage.get();
			if (dbs != null) {
				Object session = dbs.getSession();
				if (session instanceof Connection) {
					con = (Connection) session;
				}
			}
			if (con == null || con.isClosed()) {
				String dbName = "";
				DBInfo info = dbs.getInfo();
				if (info != null) {
					dbName = info.getName();
				}
				throw new RQException(mm.getMessage("error.conClosed", dbName));
			}
			keyNames = getKeyNames(con, tableName);
		} catch (RQException re) {
			throw re;
		} catch (Exception e) {
			throw new RQException(e);
		}
		if (keyNames == null) {
			return null;
		}

		if (keyNames == null || keyNames.length < 1) {
			//throw new RQException("Key Names is Invalid!");
			MessageManager mm = DataSetMessage.get();
			throw new RQException(mm.getMessage("error.invalidKey"));
		}
		if (keyValues == null || keyValues.length < 1) {
			//throw new RQException("Key Values is Invalid!");
			MessageManager mm = DataSetMessage.get();
			throw new RQException(mm.getMessage("error.invalidKeyValue"));
		}

		int size = keyNames.length;
		String condition = null;
		String key = "";
		Object v = null;
		ArrayList<Object> conParams = new ArrayList<Object>();
		for (int i = 0; i < size; i++) {
			key = keyNames[i];
			if (key != null && key.trim().length() > 0) {
				v = null;
				if (i < keyValues.length) {
					v = keyValues[i];
				}
				if (condition == null) {
					condition = "(" + key + " = ?)";
					conParams.add(v);
				} else {
					condition += " and (" + key + " = ?)";
					conParams.add(v);
				}
			}
		}
		if (condition == null || condition.trim().length() < 1) {
			//throw new RQException("Condition is Invalid!");
			MessageManager mm = DataSetMessage.get();
			throw new RQException(mm.getMessage("error.invalidCondition"));
		}

		String sql = "select count(*) from " + addTilde(tableName, dbs) + " where " + condition;
		Sequence se = retrieve(sql, conParams.toArray(), null, dbs, null, -1);
		int n = ((Number) ((BaseRecord) se.get(1)).getFieldValue(0)).intValue();
		ArrayList<Object> allParams = new ArrayList<Object>();
		if (n > 0) {
			if (str == null || str.trim().length() < 1) {
				//throw new RQException("Update SQL String is Invalid!");
				MessageManager mm = DataSetMessage.get();
				throw new RQException(mm.getMessage("error.invalidUpdateSQL"));
			}

			sql = "update " + addTilde(tableName, dbs) + " set " + str + " where " + condition;
			addParams(allParams, params);
			allParams.addAll(conParams);
		} else {
			String fieldAll = "";
			if (str == null || str.trim().length() < 1) {
				//throw new RQException("Update SQL String is Invalid!");
				MessageManager mm = DataSetMessage.get();
				throw new RQException(mm.getMessage("error.invalidUpdateSQL"));
			}
			/*
			if (fieldAll == null || fieldAll.trim().length() < 1) {
				//throw new RQException("Field Names of Values is Invalid!");
				MessageManager mm = DataSetMessage.get();
				throw new RQException(mm.getMessage("error.invalidField"));
			} else {
				fieldAll += " )";
			}
			*/
			sql = "insert into " + addTilde(tableName, dbs) + " " + fieldAll + " values (" + str + " )";
			addParams(allParams, params);
		}
		if (dbs != null) {
			return runSQL(sql, allParams.toArray(), types, dbs, true, null);
		} else {
			String dbName = "";
			MessageManager mm = DataSetMessage.get();
			throw new RQException(mm.getMessage("error.conClosed", dbName));
		}
	}

	private static void addParams(ArrayList<Object> allParams, Object[] params) {
		if (params == null || params.length < 1) {
			return;
		}
		for (int i = 0, iCount = params.length; i < iCount; i++) {
			allParams.add(params[i]);
		}
	}

	/**
	 * 根据主键生成update或insert语句更新数据
	 * @param tableName	String 表/视图名
	 * @param strs	String[] 要更新的SQL语句组
	 * @param keyNames	String[] 主键名
	 * @param keyValues	Object[] 主键值
	 * @param dbs	DBSession edited by bdl, 2012.9.18, 增加返回值
	 * @return 运行结果 更新语句返回Integer，普通sql返回Boolean
	 */
	public static Object update(String tableName, String[] strs, String[] keyNames, Object[] keyValues, DBSession dbs) {
		if (tableName == null || tableName.trim().length() < 1) {
			//throw new RQException("Table Name is Invalid!");
			MessageManager mm = DataSetMessage.get();
			throw new RQException(mm.getMessage("error.invalidTable"));
		}
		if (keyNames == null || keyNames.length < 1) {
			//throw new RQException("Key Names is Invalid!");
			MessageManager mm = DataSetMessage.get();
			throw new RQException(mm.getMessage("error.invalidKey"));
		}
		if (keyValues == null || keyValues.length < 1) {
			//throw new RQException("Key Values is Invalid!");
			MessageManager mm = DataSetMessage.get();
			throw new RQException(mm.getMessage("error.invalidKeyValue"));
		}
		int size = strs.length;
		String str = null;
		for (int i = 0; i < size; i++) {
			if (str == null) {
				str = strs[i];
			} else {
				str += strs[i];
			}
		}

		size = keyNames.length;
		String condition = null;
		String key = "";
		Object v = null;
		ArrayList<Object> conParams = new ArrayList<Object>();
		for (int i = 0; i < size; i++) {
			key = keyNames[i];
			if (key != null && key.trim().length() > 0) {
				v = null;
				if (i < keyValues.length) {
					v = keyValues[i];
				}
				if (condition == null) {
					condition = "(" + key + " = ?)";
					conParams.add(v);
				} else {
					condition += " and (" + key + " = ?)";
					conParams.add(v);
				}
			}
		}
		if (condition == null || condition.trim().length() < 1) {
			//throw new RQException("Condition is Invalid!");
			MessageManager mm = DataSetMessage.get();
			throw new RQException(mm.getMessage("error.invalidCondition"));
		}

		String sql = "select count(*) from " + addTilde(tableName, dbs) + " where " + condition;
		Sequence se = retrieve(sql, null, null, dbs, null, -1);
		int n = ((Number) ((BaseRecord) se.get(1)).getFieldValue(0)).intValue();
		if (n > 0) {
			if (str == null || str.trim().length() < 1) {
				//throw new RQException("Update SQL String is Invalid!");
				MessageManager mm = DataSetMessage.get();
				throw new RQException(mm.getMessage("error.invalidUpdateSQL"));
			}
			sql = "update " + addTilde(tableName, dbs) + " set " + str + " where " + condition;
		} else {
			String fieldAll = "";
			if (str == null || str.trim().length() < 1) {
				//throw new RQException("Update SQL String is Invalid!");
				MessageManager mm = DataSetMessage.get();
				throw new RQException(mm.getMessage("error.invalidUpdateSQL"));
			}
			/*
			if (fieldAll == null || fieldAll.trim().length() < 1) {
				throw new RQException("Field Names of Values is Invalid!");
			} else {
				fieldAll += " )";
			}
			*/
			sql = "insert into " + addTilde(tableName, dbs) + " " + fieldAll + " values (" + str + " )";
		}
		if (dbs != null) {
			return runSQL(sql, conParams.toArray(), null, dbs, true, null);
		} else {
			String dbName = "";
			MessageManager mm = DataSetMessage.get();
			throw new RQException(mm.getMessage("error.conClosed", dbName));
		}

	}

	/**
	 * 执行存储过程语句，返回结果序列，如果返回多个数据集，则返回序列的序列，如果返回数值，则设入将值设入指定参数
	 * @param proc	String 存储过程语句
	 * @param params	Object[] 参数值
	 * @param modes	byte[] 参数模式，可以选择DatabaseUtil.PROC_MODE_IN, PROC_MODE_OUT, PROC_MODE_INOUT
	 * @param types	byte[] 参数类型
	 * @param outVariables	String[] 输出值时，被赋值的参数名
	 * @param dbs	DBSession
	 * @param ctx	Context 上下文环境
	 */
	public static Sequence proc(String sql, Object[] params, byte[] modes, byte[] types, String[] outVariables,
			DBSession dbs, Context ctx) {
		PreparedStatement pst = null;
		ResultSet rs = null;
		Connection con = null;
		String dbCharset = null;
		String toCharset = null;
		boolean tranSQL = false;
		boolean tranContent = true;
		int dbType = DBTypes.UNKNOWN;
		boolean hasOutParam = false;
		try {
			DBInfo dsInfo = null;
			MessageManager mm = DataSetMessage.get();
			if (dbs != null && dbs.getInfo() instanceof DBInfo) {
				dsInfo = dbs.getInfo();
			}
			if (dbs != null) {
				Object session = dbs.getSession();
				if (session instanceof Connection) {
					con = (Connection) session;
				}
			}

			String name = "";
			if (dsInfo != null) {
				name = dsInfo.getName();
			}
			if (con == null || con.isClosed()) {
				throw new RQException(mm.getMessage("error.conClosed", name));
			}

			if (dsInfo != null) {
				dbCharset = dsInfo.getDBCharset();
				tranSQL = dsInfo.getNeedTranSentence();
				tranContent = dsInfo.getNeedTranContent();
				if ((tranContent || tranSQL) && dbCharset == null) {
					throw new RQException(mm.getMessage("error.fromCharset", name));
				}

				toCharset = dsInfo.getClientCharset();
				if ((tranContent || tranSQL) && toCharset == null) {
					throw new RQException(mm.getMessage("error.toCharset", name));
				}

				dbType = dsInfo.getDBType();
			} else {
				tranContent = false;
			}

			if (tranSQL) {
				sql = new String(sql.getBytes(), dbCharset);
			}
			if (sql.trim().startsWith("call")) {
				sql = "{" + sql + "}";
			}

			if (tranSQL) {
				sql = new String(sql.getBytes(), dbCharset);
			}

			int paramCount = params == null ? 0 : params.length;
			Object[] args = null;
			int outCount = 0;
			int outCursor = 0;
			if (paramCount > 0) {
				args = new Object[paramCount];
				int pos = 0;
				for (int i = 0; i < paramCount; i++) {
					pos = Sentence.indexOf(sql, "?", pos + 1, Sentence.IGNORE_PARS + Sentence.IGNORE_QUOTE);
					byte mode = modes[i];
					if (mode == DatabaseUtil.PROC_MODE_OUT || mode == DatabaseUtil.PROC_MODE_INOUT) {
						hasOutParam = true;
						if (types[i] != com.scudata.common.Types.DT_CURSOR) {
							outCount++;
						} else {
							outCursor++;
						}
					}
					if (mode == DatabaseUtil.PROC_MODE_IN || mode == DatabaseUtil.PROC_MODE_INOUT) {
						args[i] = params[i];
						if (args[i] instanceof Sequence) {
							Sequence l = (Sequence) args[i];
							int count = l.length();
							StringBuffer sb = new StringBuffer(2 * count);
							for (int n = 0; n < count; n++) {
								sb.append("?,");
							}
							if (sb.length() > 0 && sb.charAt(sb.length() - 1) == ',') {
								sb.deleteCharAt(sb.length() - 1);
							}
							if (sb.length() > 1) {
								sql = sql.substring(0, pos) + sb.toString() + sql.substring(pos + 1);
							}
							pos = pos + sb.length();
						}
					}
				}
			}
			int dsPos = -1; 
			Sequence dsPosGroup = new Sequence(); 
			String[] outParams = new String[outCount];
			int[] outParamsIndex = new int[outCount];
			int outIndex = -1;
			String[] outTables = new String[outCursor];
			int cursorIndex = -1;

			if (hasOutParam) {
				try {
					pst = con.prepareCall(sql);
				} catch (SQLException e) {
					if (dbs.getErrorMode()) {
						dbs.setError(e);
					} else {
						throw new RQException(mm.getMessage("error.sqlException", name, sql) + " : " + e.getMessage(), e);
					}
				}
			} else {
				try {
					pst = con.prepareStatement(sql);
				} catch (SQLException e) {
					if (dbs.getErrorMode()) {
						dbs.setError(e);
					} else {
						throw new RQException(mm.getMessage("error.sqlException", name, sql) + " : " + e.getMessage(), e);
					}
				}
			}

			int paramIndex = 0;
			for (int iArg = 0; iArg < paramCount; iArg++) {
				byte mode = modes[iArg];
				if (mode == DatabaseUtil.PROC_MODE_IN) {
					if (args[iArg] instanceof Sequence) {
						Sequence l = (Sequence) args[iArg];
						if (tranSQL) {
							for (int i = 1, size = l.length(); i <= size; i++) {
								Object o = l.get(i);
								if (o instanceof String) {
									o = new String(((String) o).getBytes(), dbCharset);
								}
								paramIndex++;
								SQLTool.setObject(dbType, pst, paramIndex, o, types[iArg]);
							}
						} else {
							for (int i = 1, size = l.length(); i <= size; i++) {
								Object o = l.get(i);
								paramIndex++;
								SQLTool.setObject(dbType, pst, paramIndex, o, types[iArg]);
							}
						}
					} else {
						Object o = args[iArg];
						if (tranSQL && o instanceof String) {
							o = new String(((String) o).getBytes(), dbCharset);
						}
						paramIndex++;
						SQLTool.setObject(dbType, pst, paramIndex, o, types[iArg]);
					}
				} else if (mode == DatabaseUtil.PROC_MODE_OUT) {
					paramIndex++;
					if (com.scudata.common.Types.DT_CURSOR == types[iArg]) {
						if (dbType == DBTypes.ORACLE) {
							try {
								Class<?> c = Class.forName("oracle.jdbc.driver.OracleTypes");
								Field f = c.getField("CURSOR");
								((CallableStatement) pst).registerOutParameter(paramIndex, f.getInt(null));
							} catch (Exception e) {
								throw new RQException(mm.getMessage("error.cursorException"));
							}
							if (dsPos < 0) {
								dsPos = paramIndex;
							} else {
								dsPosGroup.add(new Integer(paramIndex));
							}
							if (dsPos < 0) {
								throw new RQException(mm.getMessage("error.noResultSet"));
							}
							cursorIndex++;
							outTables[cursorIndex] = outVariables[iArg];
						}
						else if (dbType == DBTypes.POSTGRES || dbType == DBTypes.DBONE ) {
							((CallableStatement) pst).registerOutParameter(paramIndex, java.sql.Types.OTHER);
							if (dsPos < 0) {
								dsPos = paramIndex;
							}
							else {
								dsPosGroup.add(new Integer(paramIndex));
							}
							if (dsPos < 0) {
								throw new RQException(mm.getMessage("error.noResultSet"));
							}
						}
					} else {
						outIndex++;
						outParams[outIndex] = outVariables[iArg];
						outParamsIndex[outIndex] = paramIndex;
						registerOtherParameter((CallableStatement) pst, paramIndex, types[iArg], mm);
					}
				} else {
					Object o = args[iArg];
					if (tranContent && o instanceof String) {
						o = new String(((String) o).getBytes(), dbCharset);
					}
					paramIndex++;
					SQLTool.setObject(dbType, pst, paramIndex, o, types[iArg]);
					outIndex++;
					outParams[outIndex] = outVariables[iArg];
					outParamsIndex[outIndex] = paramIndex;
					registerOtherParameter((CallableStatement) pst, paramIndex, types[iArg], mm);
				}
			}
			if (hasOutParam) {
				if ((dbType == DBTypes.POSTGRES || dbType == DBTypes.DBONE) && dsPos > 0) {
					((CallableStatement) pst).execute(); 
				}
				else {
					rs = ((CallableStatement) pst).executeQuery(); 
				}
				try {
					for (int i = 0, count = outParams.length; i < count; i++) {
						Object value = ((CallableStatement) pst).getObject(outParamsIndex[i]);
						ctx.setParamValue(outParams[i], value);
					}
				} catch (Exception e) {
					throw new RQException(mm.getMessage("error.outParam"));
				}
			}
			try {
				if (dbType == DBTypes.ORACLE && dsPos > 0) {
					rs = (ResultSet) ((CallableStatement) pst).getObject(dsPos);
				}
				else if ((dbType == DBTypes.POSTGRES || dbType == DBTypes.DBONE) && dsPos > 0) {
					rs = (ResultSet) ((CallableStatement) pst).getObject(dsPos);
				}
				// edited by bd, 2022.12.11, 如果有输出参数，pst已经执行过了
				else if (hasOutParam) {
				}
				else {
					rs = pst.executeQuery();
				}
			} catch (SQLException e) {
				if (dbs.getErrorMode()) {
					dbs.setError(e);
				} else {
					throw new RQException(mm.getMessage("error.sqlException", name, sql) + " : " + e.getMessage(), e);
				}
			}

			if (dsInfo == null) {
				tranContent = false;
			}
			Sequence se = populate(rs, dbCharset, tranContent, toCharset, dbType, false, null, false, -1, null);
			if (outCursor > 0) {
				String outName = outTables[0];
				if (outName != null && outName.trim().length() > 0) {
					ctx.setParamValue(outName, se);
				}
			}

			if (dsPosGroup.length() > 0) {
				Sequence mul_dataset = new Sequence();
				mul_dataset.add(se);
				int size = dsPosGroup.length();
				for (int i = 0; i < size; i++) {
					int loc = ((Integer) dsPosGroup.get(i + 1)).intValue();
					if (rs != null) {
						try {
							rs.close();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					try {
						rs = (ResultSet) ((CallableStatement) pst).getObject(loc);
					} catch (SQLException e) {
						if (dbs.getErrorMode()) {
							dbs.setError(e);
						} else {
							throw new RQException(mm.getMessage("error.sqlException", name, sql) + " : " + e.getMessage(), e);
						}
					}
					Sequence se_add = populate(rs, dbCharset, tranContent, toCharset, dbType, false, null, false, -1,
							null);
					mul_dataset.add(se_add);
					if (outCursor > 0) {
						String outName = null;
						if (i < outTables.length - 1) {
							outName = outTables[i + 1];
						}
						if (outName != null && outName.trim().length() > 0) {
							ctx.setParamValue(outName, se_add);
						}
					}
				}
				if (mul_dataset.length() > 1) {
					return mul_dataset;
				}
			} else if (pst.getMoreResults()) {
				Sequence mul_dataset = new Sequence();
				mul_dataset.add(se);
				if (rs != null) {
					try {
						rs.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				rs = pst.getResultSet();
				Sequence se_add = populate(rs, dbCharset, tranContent, toCharset, dbType, false, null, false, -1, null);
				mul_dataset.add(se_add);

				// edited by bd, 2023.3.16, 返回数据集多于2个时无法正常返回到结果中，怀疑可能是判断有问题
				boolean more = pst.getMoreResults();
				while (more || pst.getUpdateCount() != -1) {
					if (!more) {
						// edited by bd, 2023.3.16, 如果more为false，说明当前返回结果并非结果集
						more = pst.getMoreResults();
						continue;
					}
				/*while (pst.getMoreResults() || pst.getUpdateCount() != -1) {
					if (pst.getUpdateCount() == -1) {
						pst.getMoreResults();
						continue;
					}*/
					if (rs != null) {
						try {
							rs.close();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					rs = pst.getResultSet();
					se_add = populate(rs, dbCharset, tranContent, toCharset, dbType, false, null, false, -1, null);
					mul_dataset.add(se_add);
					// edited by bd, 2023.3.16, 获取当前结果集后，判断下个结果
					more = pst.getMoreResults();
				}
				if (mul_dataset.length() > 1) {
					return mul_dataset;
				}
			}
			return se;
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (pst != null) {
					pst.close();
				}
			} catch (Exception e) {
				throw new RQException(e.getMessage(), e);
			}
		}
	}

	/**
	 * 注册参数
	 * @param cst	CallableStatement
	 * @param paramIndex	参数序号
	 * @param type	参数类型
	 * @param mm	信息管理器
	 */
	public static void registerOtherParameter(CallableStatement cst, int paramIndex, int type, MessageManager mm) {
		try {
			switch (type) {
			case com.scudata.common.Types.DT_STRING_SERIES:
			case com.scudata.common.Types.DT_STRING:
				cst.registerOutParameter(paramIndex, java.sql.Types.VARCHAR);
				break;
			case com.scudata.common.Types.DT_DOUBLE_SERIES:
			case com.scudata.common.Types.DT_DOUBLE:
				cst.registerOutParameter(paramIndex, java.sql.Types.DOUBLE);
				break;
			case com.scudata.common.Types.DT_INT_SERIES:
			case com.scudata.common.Types.DT_INT:
				cst.registerOutParameter(paramIndex, java.sql.Types.INTEGER);
				break;
			case com.scudata.common.Types.DT_DATE_SERIES:
			case com.scudata.common.Types.DT_DATE:
				cst.registerOutParameter(paramIndex, java.sql.Types.DATE);
				break;
			case com.scudata.common.Types.DT_TIME_SERIES:
			case com.scudata.common.Types.DT_TIME:
				cst.registerOutParameter(paramIndex, java.sql.Types.TIME);
				break;
			case com.scudata.common.Types.DT_DATETIME_SERIES:
			case com.scudata.common.Types.DT_DATETIME:
				cst.registerOutParameter(paramIndex, java.sql.Types.TIMESTAMP);
				break;
			case com.scudata.common.Types.DT_LONG_SERIES:
			case com.scudata.common.Types.DT_LONG:
			case com.scudata.common.Types.DT_BIGINT_SERIES:
			case com.scudata.common.Types.DT_BIGINT:
				cst.registerOutParameter(paramIndex, java.sql.Types.BIGINT);
				break;
			case com.scudata.common.Types.DT_SHORT_SERIES:
			case com.scudata.common.Types.DT_SHORT:
				cst.registerOutParameter(paramIndex, java.sql.Types.SMALLINT);
				break;
			case com.scudata.common.Types.DT_FLOAT_SERIES:
			case com.scudata.common.Types.DT_FLOAT:
				cst.registerOutParameter(paramIndex, java.sql.Types.FLOAT);
				break;
			case com.scudata.common.Types.DT_DECIMAL_SERIES:
			case com.scudata.common.Types.DT_DECIMAL:
				cst.registerOutParameter(paramIndex, java.sql.Types.DECIMAL);
				break;
			default:
				cst.registerOutParameter(paramIndex, java.sql.Types.VARCHAR);
				break;
			}
		} catch (Exception e) {
			throw new RQException(mm.getMessage("error.regParam", Integer.toString(paramIndex)));
		}
	}

	private static String addTilde(String field, DBSession dbs) {
		if (dbs != null && dbs.getInfo() instanceof DBConfig) {
			DBConfig dbc = (DBConfig) dbs.getInfo();
			if (dbc.isAddTilde()) {
				int dbType = dbc.getDBType();
				field = DBTypes.getLeftTilde(dbType) + field + DBTypes.getRightTilde(dbType);
			}
		}
		return field;
	}

	private static String removeTilde(String field, DBSession dbs) {
		if (field == null || field.trim().length() < 1) {
			return field;
		}
		if (dbs != null && dbs.getInfo() instanceof DBConfig) {
			DBConfig dbc = (DBConfig) dbs.getInfo();
			if (dbc.isAddTilde()) {
				int dbType = dbc.getDBType();
				if (field.substring(0, 1).equals(DBTypes.getLeftTilde(dbType))) {
					field = field.substring(1);
				}
				if (field.substring(field.length() - 1).equals(DBTypes.getRightTilde(dbType))) {
					field = field.substring(0, field.length() - 1);
				}
			}
		}
		return field;
	}

	/**
	 * DBObject调用，针对序列的每一个元素执行查询语句，返回结果集合并的序表 
	 * @param sql	String
	 * @param valueGroup	Object[][]
	 * @param types	int[]
	 * @param dbs	DBSession
	 * @param opt	String
	 * @param ctx	Context
	 * @return Table
	 */
	public static Table queryGroup(String inisql, Object[][] valueGroup, byte[] types, DBSession dbs, String opt,
			Context ctx) {
		ResultSet rs = null;
		PreparedStatement pst = null;
		Connection con = null;
		String dbCharset = null;
		String toCharset = null;
		boolean tranSQL = false;
		boolean tranContent = true;
		int dbType = DBTypes.UNKNOWN;
		try {
			DBConfig dsConfig = null;
			MessageManager mm = DataSetMessage.get();
			if (dbs != null && dbs.getInfo() instanceof DBConfig) {
				dsConfig = (DBConfig) dbs.getInfo();
			}
			if (dbs != null) {
				Object session = dbs.getSession();
				if (session instanceof Connection) {
					con = (Connection) session;
				}
			}

			if (con == null || con.isClosed()) {
				String name = "";
				DBInfo info = dbs.getInfo();
				if (info != null) {
					name = info.getName();
				}
				throw new RQException(mm.getMessage("error.conClosed", name));
			}

			if (dsConfig != null) {
				dbCharset = dsConfig.getDBCharset();
				tranSQL = dsConfig.getNeedTranSentence();
				tranContent = dsConfig.getNeedTranContent();
				if ((tranContent || tranSQL) && dbCharset == null) {
					String name = "";
					DBInfo info = dbs.getInfo();
					if (info != null) {
						name = info.getName();
					}
					throw new RQException(mm.getMessage("error.fromCharset", name));
				}

				toCharset = dsConfig.getClientCharset();
				if ((tranContent || tranSQL) && toCharset == null) {
					String name = "";
					DBInfo info = dbs.getInfo();
					if (info != null) {
						name = info.getName();
					}
					throw new RQException(mm.getMessage("error.toCharset", name));
				}

				dbType = dsConfig.getDBType();
			} else {
				tranContent = false;
			}

			if (tranSQL) {
				inisql = new String(inisql.getBytes(), dbCharset);
			}
			Table table = null;
			boolean addTable = false;
			if (opt != null && opt.indexOf("f") > -1) {
				addTable = true;

			}
			boolean oneRecord = false;
			if (opt != null && opt.indexOf("1") > -1) {
				oneRecord = true;

			}
			int queryCount = valueGroup == null ? 0 : valueGroup.length;

			for (int qi = 0; qi < queryCount; qi++) {
				try {
					if (rs != null) {
						try {
							rs.close();
						} catch (Exception e) {
							e.printStackTrace();
						}
						rs = null;
					}
					if (pst != null) {
						pst.close();
						pst = null;
					}
				} catch (Exception e) {
					throw new RQException(e.getMessage(), e);
				}
				String sql = inisql;
				Object[] params = valueGroup[qi];
				int paramCount = (params == null ? 0 : params.length);
				Object[] args = null;
				byte[] argTypes = null;
				if (paramCount > 0) {
					args = new Object[paramCount];
					argTypes = new byte[paramCount];
					int pos = 0;
					for (int paramIndex = 0; paramIndex < paramCount; paramIndex++) {
						pos = Sentence.indexOf(sql, "?", pos + 1, Sentence.IGNORE_PARS + Sentence.IGNORE_QUOTE);
						args[paramIndex] = params[paramIndex];
						if (types == null || types.length <= paramIndex) {
							argTypes[paramIndex] = com.scudata.common.Types.DT_DEFAULT;
						} else {
							argTypes[paramIndex] = types[paramIndex];
						}
						if (args[paramIndex] == null) {
							continue;
						}
						if (args[paramIndex] instanceof Sequence && tranContent) {
							Sequence l = (Sequence) args[paramIndex];
							for (int i = 1, size = l.length(); i <= size; i++) {
								Object o = l.get(i);
								if (o instanceof String && tranSQL) {
									o = new String(((String) o).getBytes(), dbCharset);
									l.set(i, o);
								}
							}
						} else if (args[paramIndex] instanceof String && tranSQL) {
							args[paramIndex] = new String(((String) args[paramIndex]).getBytes(), dbCharset);
						}
						if (args[paramIndex] instanceof Sequence) {
							Object[] objs = ((Sequence) args[paramIndex]).toArray();
							int objCount = objs.length;
							StringBuffer sb = new StringBuffer(2 * objCount);
							for (int iObj = 0; iObj < objCount; iObj++) {
								sb.append("?,");
							}
							if (sb.length() > 0 && sb.charAt(sb.length() - 1) == ',') {
								sb.deleteCharAt(sb.length() - 1);
							}
							if (sb.length() > 1) {
								sql = sql.substring(0, pos) + sb.toString() + sql.substring(pos + 1);
							}
							pos = pos + sb.length();
						}
					}
				}

				try {
					pst = con.prepareStatement(sql);
				} catch (SQLException e) {
					if (dbs.getErrorMode()) {
						dbs.setError(e);
					} else {
						String name = "";
						DBInfo info = dbs.getInfo();
						if (info != null) {
							name = info.getName();
						}
						throw new RQException(mm.getMessage("error.sqlException", name, sql) + " : " + e.getMessage(), e);
					}
				}

				if (args != null && args.length > 0) {
					int pos = 0;
					for (int iArg = 0; iArg < args.length; iArg++) {
						pos++;
						try {
							byte type = argTypes[iArg];
							if (args[iArg] != null && args[iArg] instanceof Sequence) {
								Object[] objs = ((Sequence) args[iArg]).toArray();
								for (int iObj = 0; iObj < objs.length; iObj++) {
									SQLTool.setObject(dbType, pst, pos, objs[iObj], type);
									pos++;
								}
								pos--;
							} else {
								SQLTool.setObject(dbType, pst, pos, args[iArg], type);
							}
						} catch (Exception e) {
							String name = "";
							DBInfo info = dbs.getInfo();
							if (info != null) {
								name = info.getName();
							}
							throw new RQException(mm.getMessage("error.argIndex", name, Integer.toString(iArg + 1)));
						}
					}
				}

				try {
					rs = pst.executeQuery();
				} catch (SQLException e) {
					if (dbs.getErrorMode()) {
						dbs.setError(e);
					} else {
						String name = "";
						DBInfo info = dbs.getInfo();
						if (info != null) {
							name = info.getName();
						}
						throw new RQException(mm.getMessage("error.sqlException", name, sql) + " : " + e.getMessage(), e);
					}
				}
				if (oneRecord) {
					return populate(rs, dbCharset, tranContent, toCharset, dbType, addTable, table, oneRecord, -1, opt);
				}
				table = populate(rs, dbCharset, tranContent, toCharset, dbType, addTable, table, oneRecord, -1, opt);
			}
			return table;
		} catch (RQException re) {
			throw re;
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (pst != null) {
					pst.close();
				}
			} catch (Exception e) {
				throw new RQException(e.getMessage(), e);
			}
		}
	}

	private static byte[] toByteArray(ArrayList<Byte> bytes) {
		byte[] rs = new byte[bytes.size()];
		int i = 0;
		for (byte obj : bytes) {
			rs[i++] = obj;
		}
		return rs;
	}

	/**
	 * 根据游标更新表table中的字段fields， 使用batch执行，要求一次执行更新整个游标数据，不然无法控制executeBatch
	 * @param cs	ICursor 源游标
	 * @param table	String 表名
	 * @param fields	String[] 字段名
	 * @param fopts	String[] p：字段是主键，a：字段是自增字段
	 * @param exps	Expression[] 值表达式
	 * @param opt	String t：作为非更新序表处理，k：完成后不清理状态
	 * @param dbs	DBSession
	 * @param ctx	Context
	 * @return int	增加返回值，成功更新条数
	 */
	public static int update(ICursor cs, String table, String[] fields, String[] fopts, Expression[] exps, String opt,
			DBSession dbs, Context ctx) {
		Statement st = null;
		Connection con = null;
		String dbCharset = null;
		String toCharset = null;
		boolean tranSQL = false;
		boolean tranContent = true;
		int dbType = DBTypes.UNKNOWN;

		int fsize = fields.length;
		String field = "";
		String fieldAll = null;
		String dbName = "";
		int batchSize = 1000;
		PreparedStatement pst = null;

		try {
			DBConfig dsConfig = null;
			if (dbs != null && dbs.getInfo() instanceof DBConfig) {
				dsConfig = (DBConfig) dbs.getInfo();
			}
			MessageManager mm = DataSetMessage.get();
			if (dbs != null) {
				Object session = dbs.getSession();
				if (session instanceof Connection) {
					con = (Connection) session;
				}
				batchSize = dsConfig.getBatchSize();
				if (batchSize < 1) {
					batchSize = 1;
				}
				dbCharset = dsConfig.getDBCharset();
				tranSQL = dsConfig.getNeedTranSentence();
				tranContent = dsConfig.getNeedTranContent();
				if ((tranContent || tranSQL) && dbCharset == null) {
					throw new RQException(mm.getMessage("error.fromCharset", dbName));
				}

				toCharset = dsConfig.getClientCharset();
				if ((tranContent || tranSQL) && toCharset == null) {
					throw new RQException(mm.getMessage("error.toCharset", dbName));
				}
				dbType = dsConfig.getDBType();
			} else {
				tranContent = false;
			}

			DBInfo info = dbs.getInfo();
			if (info != null) {
				dbName = info.getName();
			}
			if (con == null || con.isClosed()) {
				throw new RQException(mm.getMessage("error.conClosed", dbName));
			}
			boolean bb = true;
			if (toCharset != null) {
				bb = toCharset.equalsIgnoreCase(dbCharset) || dbCharset == null;
			}

			ResultSet rs = null;
			byte[] tColTypes = null;
			ArrayList<Integer> keyCols = new ArrayList<Integer>();
			ArrayList<String> autoKeys = new ArrayList<String>();
			boolean initial = true;
			byte[] ais = new byte[fsize];
			if (fopts != null) {
				int osize = fopts.length;
				if (osize > fsize) {
					osize = fsize;
				}
				for (int i = 0; i < osize; i++) {
					String fopt = fopts[i];
					if (fopt != null) {
						if (fopt.indexOf("p") > -1) {
							keyCols.add(new Integer(i));
						}
						if (fopt.indexOf("a") > -1) {
							ais[i] = Col_AutoIncrement;
							autoKeys.add(fields[i]);
						} else {
							ais[i] = 0;
						}
					}
				}
			}
			int[] kis = null;
			int keysize = keyCols.size();
			String check_sql = "";
			String update_sql = "";
			String insert_sql = "";
			while (true) {
				Sequence fetchSeq = cs.fetch(batchSize);
				if (fetchSeq == null || fetchSeq.length() == 0)
					break;
				if (initial) {
					if (keysize < 1) {
						Object o1 = fetchSeq.get(1);
						if (o1 instanceof BaseRecord) {
							DataStruct ds1 = ((BaseRecord) o1).dataStruct();
							String[] keys = ds1.getPrimary();
							int kc = keys == null ? 0 : keys.length;
							for (int i = 0; i < kc; i++) {
								String key = keys[i];
								if (key == null) {
									continue;
								}
								int ki = -1;
								for (int j = 0; j < fsize; j++) {
									if (key.equalsIgnoreCase(fields[j])) {
										ki = j;
										break;
									}
								}
								if (ki > -1) {
									keyCols.add(new Integer(ki));
								}
							}
						}
						keysize = keyCols.size();
					}
					if (tColTypes == null || tColTypes.length < 1 || keysize < 1) {
						String sql = "select";
						for (int i = 0, iSize = fields.length; i < iSize; i++) {
							sql += " " + addTilde(fields[i], dbs);
							if (i < iSize - 1) {
								sql += ",";
							}
						}
						sql += " from " + addTilde(table, dbs) + " where 1 = 0";
						try {
							pst = con.prepareStatement(sql);
							rs = pst.executeQuery();
							ResultSetMetaData rsmd = rs.getMetaData();
							int colSize = rsmd.getColumnCount();
							tColTypes = new byte[fsize];

							for (int ci = 0; ci < colSize; ci++) {
								String colname = rsmd.getColumnLabel(ci + 1);
								colname = tranName(colname, tranContent, dbCharset, toCharset, bb, opt);
								if (colname == null || colname.trim().length() < 1) {
									continue;
								}
								for (int fi = 0; fi < fsize; fi++) {
									if (colname.equalsIgnoreCase(fields[fi])) {
										int sqlType = rsmd.getColumnType(ci + 1);
										byte rqType = com.scudata.common.Types.getTypeBySQLType(sqlType);
										tColTypes[fi] = rqType;
										break;
									}
								}
							}

							if (keysize < 1) {
								if (table.indexOf(".") > 0) {
									String[] tns = table.split("\\.");
									if (tns == null || tns.length < 2) {
										int loc = table.indexOf(".");
										tns = new String[2];
										tns[0] = table.substring(0, loc);
										tns[1] = table.substring(loc + 1);
									}
								} else {
								}
								try {
									while (rs.next()) {
										String columnName = rs.getString("COLUMN_NAME");
										if (columnName == null) {
											continue;
										}
										int ki = -1;
										for (int i = 0; i < fsize; i++) {
											if (columnName.equalsIgnoreCase(fields[i])) {
												ki = i;
												break;
											}
										}
										if (ki > -1) {
											keyCols.add(new Integer(ki));
										}
									}
									keysize = keyCols.size();
								}
								catch (Exception e) {
								}
							}
						} catch (Exception e) {
							throw new RQException(e.getMessage(), e);
						} finally {
							try {
								if (rs != null) {
									rs.close();
								}
								if (pst != null) {
									pst.close();
								}
							} catch (Exception e) {
								throw new RQException(e.getMessage(), e);
							}
							rs = null;
						}
					}
					if (keysize < 1) {
						keyCols.add(new Integer(0));
						keysize = keyCols.size();
					}

					Expression[] keyExps = null;
					kis = new int[keysize];
					keyExps = new Expression[keysize];
					for (int i = 0; i < keysize; i++) {
						kis[i] = ((Integer) keyCols.get(i)).intValue();
						keyExps[i] = exps[kis[i]];
					}

					for (int iField = 0; iField < fsize; iField++) {
						if (ais[iField] == Col_AutoIncrement) {
							continue;
						}

						field = fields[iField];
						if (field != null && field.trim().length() > 0) {
							if (fieldAll == null) {
								fieldAll = "(" + field;
							} else {
								fieldAll += ", " + field;
							}
						}
					}
					if (fieldAll == null || fieldAll.trim().length() < 1) {
						//throw new RQException("Field Names is Invalid!");
						throw new RQException(mm.getMessage("error.invalidField"));
					}
					fieldAll += ")";
				}
				try {
					if (initial) {
						if (opt != null && opt.indexOf("a") > -1) {
							//Logger.debug("Clear all the records from table +"+table);
							Logger.debug(mm.getMessage("info.clearTable", table));
							String sql = "delete from " + addTilde(table, dbs);
							st = con.createStatement();
							st.execute(sql);
							st.close();
						}

						String condition = null;
						String key = "";
						for (int j = 0; j < keysize; j++) {
							key = fields[kis[j]];
							if (key != null && key.trim().length() > 0) {
								if (condition == null) {
									condition = "(" + key + " = ?)";
								} else {
									condition += " and (" + key + " = ?)";
								}
							}
						}
						check_sql = "select count(*) from " + addTilde(table, dbs) + " where " + condition;
						String sets = null;
						for (int iField = 0; iField < fsize; iField++) {
							if (ais[iField] == Col_AutoIncrement) {
								continue;
							}

							field = fields[iField];
							if (field != null && field.trim().length() > 0) {//此处的field没有过滤是否为主键，造成主键会重设一遍，xq 2015.4.21
								if (sets == null) {
									sets = field + " = ?";
								} else {
									sets += ", " + field + " = ?";
								}
							}
						}
						if (sets == null || sets.trim().length() < 1) {
							//throw new RQException("Field Names of Values is Invalid!");
							throw new RQException(mm.getMessage("error.invalidField"));
						}
						update_sql = "update " + addTilde(table, dbs) + " set " + sets + " where " + condition;
						sets = null;
						for (int iField = 0; iField < fsize; iField++) {
							if (ais[iField] == DataStruct.Col_AutoIncrement) {
								continue;
							}
							field = fields[iField];
							if (field != null && field.trim().length() > 0) {
								if (sets == null) {
									fieldAll = "(" + field;
									sets = "( ?";
								} else {
									fieldAll += ", " + field;
									sets += ", ?";
								}
							}
						}
						if (sets == null || sets.trim().length() < 1) {
							//throw new RQException("Field Values of Values is Invalid!");
							throw new RQException(mm.getMessage("error.invalidFieldValue"));
						} else {
							sets += " )";
						}
						if (fieldAll == null || fieldAll.trim().length() < 1) {
							//throw new RQException("Field Names of Values is Invalid!");
							throw new RQException(mm.getMessage("error.invalidField"));
						} else {
							fieldAll += " )";
						}
						insert_sql = "insert into " + addTilde(table, dbs) + " " + fieldAll + " values " + sets;

						if (tranSQL) {
							check_sql = new String(check_sql.getBytes(), dbCharset);
							update_sql = new String(update_sql.getBytes(), dbCharset);
							insert_sql = new String(insert_sql.getBytes(), dbCharset);
						}
					}

					ArrayList<Expression> updateParams = new ArrayList<Expression>();
					ArrayList<Expression> primaryParams = new ArrayList<Expression>();
					ArrayList<Expression> insertParams = new ArrayList<Expression>();
					ArrayList<Byte> updateTypes = new ArrayList<Byte>();
					ArrayList<Byte> primaryTypes = new ArrayList<Byte>();
					ArrayList<Byte> insertTypes = new ArrayList<Byte>();

					for (int iField = 0; iField < fsize; iField++) {
						if (ais[iField] == DataStruct.Col_AutoIncrement) {
							continue;
						}
						insertParams.add(exps[iField]);
						insertTypes.add(tColTypes[iField]);
					}

					for (int ki = 0; ki < keysize; ki++) {
						primaryParams.add(exps[kis[ki]]);
						primaryTypes.add(tColTypes[kis[ki]]);
					}

					updateParams.addAll(insertParams);
					updateTypes.addAll(insertTypes);
					updateParams.addAll(primaryParams);
					updateTypes.addAll(primaryTypes);

					boolean isAutoDetect = true;
					if (opt != null) {
						if (opt.indexOf('i') > -1) {
							if (initial) {
								try {
									//Logger.debug("Insert-only, preparing insert records: "+insert_sql);
									Logger.debug(mm.getMessage("info.insertOnly", insert_sql));
									pst = con.prepareStatement(insert_sql);
								} catch (SQLException e) {
									if (dbs.getErrorMode()) {
										dbs.setError(e);
									} else {
										throw new RQException(mm.getMessage("error.sqlException", dbName, insert_sql)
												+ " : " + e.getMessage(), e);
									}
								}
							}
							executeBatchPst(fetchSeq, pst, insertParams, insertTypes, ctx, dbs, dbCharset, tranSQL,
									dbType, dbName);
							isAutoDetect = false;
						} else if (opt.indexOf('u') > -1) {
							if (initial) {
								try {
									//Logger.debug("Update-only, preparing update records: "+update_sql);
									Logger.debug(mm.getMessage("info.updateOnly", update_sql));
									pst = con.prepareStatement(update_sql);
								} catch (SQLException e) {
									if (dbs.getErrorMode()) {
										dbs.setError(e);
									} else {
										throw new RQException(mm.getMessage("error.sqlException", dbName, update_sql)
												+ " : " + e.getMessage(), e);
									}
								}
							}
							executeBatchPst(fetchSeq, pst, updateParams, updateTypes, ctx, dbs, dbCharset, tranSQL,
									dbType, dbName);
							isAutoDetect = false;
						}
					}
					if (initial)
						initial = false;
					if (isAutoDetect) {
						Expression[] expParams = new Expression[primaryParams.size()];
						primaryParams.toArray(expParams);
						// edited by bd, 2022.4.15, 游标更新时，由于通常数据量比较大，暂时不支持对空值主键的处理
						Sequence recordCount = query(fetchSeq, check_sql, expParams, toByteArray(primaryTypes), opt,
								ctx, dbs, null, null, keysize);
						Sequence updateRecords = new Sequence();
						Sequence insertRecords = new Sequence();
						int rsize = recordCount == null ? 0 : recordCount.length();
						for (int i = 1; i <= rsize; i++) {
							BaseRecord r = (BaseRecord) recordCount.get(i);
							int c = ((Number) r.getFieldValue(0)).intValue();
							if (c > 0) {
								updateRecords.add(fetchSeq.get(i));
							} else {
								insertRecords.add(fetchSeq.get(i));
							}
						}
						if (updateRecords.length() > 0) {
							//Logger.debug("Auto update, preparing update records: "+update_sql);
							Logger.debug(mm.getMessage("info.autoUpdate", update_sql));
							// edited by bd, 2023.3.24, 更新时不应该用全数据
							//executeBatchSql(fetchSeq, update_sql, updateParams, updateTypes, ctx, dbs);
							executeBatchSql(updateRecords, update_sql, updateParams, updateTypes, ctx, dbs);
						}
						if (insertRecords.length() > 0) {
							//Logger.debug("Auto insert, preparing insert records: "+insert_sql);
							Logger.debug(mm.getMessage("info.autoInsert", insert_sql));
							// edited by bd, 2023.3.24, 更新时不应该用全数据
							//executeBatchSql(fetchSeq, insert_sql, insertParams, insertTypes, ctx, dbs);
							executeBatchSql(insertRecords, insert_sql, insertParams, insertTypes, ctx, dbs);
						}
					}

					/* 改造如下逐行执行为批量执行 xq 2015.4.21 end */
				} catch (RQException e) {
					//com.scudata.common.Logger.debug("update error:", e);
					Logger.debug(mm.getMessage("error.update", e.getMessage()));
					if (dbs.getErrorMode()) {
						dbs.setError(new SQLException(e.getMessage(), "Error: 5001 Update error: ", 5001));
					}
				}
			}
		} catch (RQException re) {
			throw re;
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				if (opt == null || opt.indexOf('k') < 0) {
					con.commit();
				}
				if (st != null) {
					st.close();
				}
				if (pst != null) {
					pst.close();
				}
			} catch (Exception e) {
				throw new RQException(e.getMessage(), e);
			}
		}
		return 0;// 批量处理，不知道在哪出错，返回值无意义，始终返回0 xq 2015.4.21
	}
	
	private static byte Col_NormalKey = 0x02;

	/**
	 * db.update(A:A',tbl,F:x,…;P,…)根据源排列和对比排列，更新数据库中的tbl表
	 * @param srcSeq	Sequence 源排列，用于在tbl中添加或更新记录
	 * @param compSeq	Sequence 对比排列，用于在tbl中，删除主键在compSeq而不在srcSeq中的记录
	 * @param table	String 数据库中表名，tbl
	 * @param fields	String[] 数据库表中的字段名，留空时由上层解析出A中的字段
	 * @param fopts	String[] 针对每个字段的选项，由上层分析表达式时设置，a自增p主键
	 * @param exps	Expression[] 针对每个字段更新所用的表达式，留空时由上层解析
	 * @param opt	String 选项，支持：i只添加;u只更新;d只删除;a执行前删除原表中所有记录;
					l第一个字段是自更新字段(上层处理);k完成后不提交事务(上层处理)
	 * @param dbs	DBSession
	 * @param ctx	Context
	 * @return
	 */
	public static int update(Sequence srcSeq, Sequence compSeq, String table, String[] fields, String[] fopts,
			Expression[] exps, String opt, DBSession dbs, Context ctx) {
		boolean oClear = false;
		if (opt != null && opt.indexOf("a") > -1) {
			oClear = true;
		}
		if (oClear || compSeq == null || compSeq.length() == 0) {
			//return update(srcSeq, table, fields, fopts, exps, opt, dbs, ctx);
		}
		
		Statement st = null;
		Connection con = null;
		String dbCharset = null;
		String toCharset = null;
		boolean tranSQL = false;
		boolean tranContent = true;
		int dbType = DBTypes.UNKNOWN;

		int fsize = fields.length;
		String field = "";
		String fieldAll = null;
		String dbn = "";
		int batchSize = 1000;

		try {
			DBConfig dsConfig = null;
			if (dbs != null && dbs.getInfo() instanceof DBConfig) {
				dsConfig = (DBConfig) dbs.getInfo();
			}
			if (dbs != null) {
				Object session = dbs.getSession();
				if (session instanceof Connection) {
					con = (Connection) session;
				}
			}
			if (con == null || con.isClosed()) {
				DBInfo info = dbs.getInfo();
				if (info != null) {
					dbn = info.getName();
				}
				MessageManager mm = DataSetMessage.get();
				throw new RQException(mm.getMessage("error.conClosed", dbn));
			}
			boolean bb = true;

			ResultSet rs = null;
			byte[] tColTypes = null;
			ArrayList<Integer> keyCols = new ArrayList<Integer>();
			ArrayList<String> autoKeys = new ArrayList<String>();
			byte[] ais = new byte[fsize];
			if (fopts != null) {
				int osize = fopts.length;
				if (osize > fsize) {
					osize = fsize;
				}
				for (int i = 0; i < osize; i++) {
					String fopt = fopts[i];
					if (fopt != null) {
						if (fopt.indexOf("p") > -1) {
							keyCols.add(new Integer(i));
						}
						if (fopt.indexOf("a") > -1) {
							ais[i] = DataStruct.Col_AutoIncrement;
							autoKeys.add(fields[i]);
						}
						else if (fopt.indexOf("p") > -1) {
							// edited by bd, 2022.4.14, 更新时不再设置主键值，这里预判断
							ais[i] = Col_NormalKey;
						}
						else {
							ais[i] = 0;
						}
					}
				}
			}
			int[] kis = null;
			int keysize = keyCols.size();

			if (tColTypes == null || tColTypes.length < 1 || keysize < 1 ) {
				String sql = "select";
				for (int i = 0, iSize = fields.length; i < iSize; i++) {
					sql += " " + addTilde(fields[i], dbs);
					if (i < iSize - 1) {
						sql += ",";
					}
				}
				sql += " from " + addTilde(table, dbs) + " where 1 = 0";
				PreparedStatement pst = null;
				try {
					pst = con.prepareStatement(sql);
					rs = pst.executeQuery();
					ResultSetMetaData rsmd = rs.getMetaData();
					int colSize = rsmd.getColumnCount();
					tColTypes = new byte[fsize];

					for (int ci = 0; ci < colSize; ci++) {
						String colname = rsmd.getColumnLabel(ci + 1);
						colname = tranName(colname, tranContent, dbCharset, toCharset, bb, opt);
						if (colname == null || colname.trim().length() < 1) {
							continue;
						}
						for (int fi = 0; fi < fsize; fi++) {
							if (colname.equalsIgnoreCase(fields[fi])) {
								int sqlType = rsmd.getColumnType(ci + 1);
								byte rqType = com.scudata.common.Types.getTypeBySQLType(sqlType);
								tColTypes[fi] = rqType;
								break;
							}
						}
					}
					
					if (keysize < 1) {
						DatabaseMetaData dbmd = con.getMetaData();
						String schema = "", tn = "";
						if (table.indexOf(".") > 0) {
							String[] tns = table.split("\\.");
							if (tns == null || tns.length < 2) {
								int loc = table.indexOf(".");
								tns = new String[2];
								tns[0] = table.substring(0, loc);
								tns[1] = table.substring(loc + 1);
							}
							schema = tns[0];
							tn = tns[1];
						} else {
							tn = table;
						}
						try {
							rs.close();
							rs = dbmd.getPrimaryKeys("", schema, tn);
							while (rs.next()) {
								String columnName = rs.getString("COLUMN_NAME");
								if (columnName == null) {
									continue;
								}
								int ki = -1;
								for (int i = 0; i < fsize; i++) {
									if (columnName.equalsIgnoreCase(fields[i])) {
										ki = i;
										break;
									}
								}
								if (ki > -1) {
									keyCols.add(new Integer(ki));
								}
							}
							keysize = keyCols.size();
						}
						catch (Exception e) {
						}
					}
				} catch (Exception e) {
					throw new RQException(e.getMessage(), e);
				} finally {
					try {
						if (rs != null) {
							rs.close();
						}
						if (pst != null) {
							pst.close();
						}
					} catch (Exception e) {
						throw new RQException(e.getMessage(), e);
					}
					rs = null;
				}
			}
			keysize = keyCols.size();
			if (keysize < 1) {
				DataStruct ds1 = null;
				if (srcSeq instanceof Table) {
					ds1 = ((Table) srcSeq).dataStruct();
				}
				if (ds1 == null&& srcSeq != null && srcSeq.length() > 0 ) {
					Object o1 = srcSeq.get(1);
					if (o1 instanceof BaseRecord) {
						ds1 = ((BaseRecord) o1).dataStruct();
					}
				}
				if (ds1 != null) {
					String[] keys = ds1.getPrimary();
					int kc = keys == null ? 0 : keys.length;
					for (int i = 0; i < kc; i++) {
						String key = keys[i];
						if (key == null) {
							continue;
						}
						int ki = -1;
						for (int j = 0; j < fsize; j++) {
							if (key.equalsIgnoreCase(fields[j])) {
								ki = j;
								break;
							}
						}
						if (ki > -1) {
							keyCols.add(new Integer(ki));
						}
					}
				}
				keysize = keyCols.size();
			}
			if (keysize < 1) {
				//throw new RQException("update function can't find Key Columns.");
				MessageManager mm = DataSetMessage.get();
				throw new RQException(mm.getMessage("error.invalidKey"));
			}

			Expression[] keyExps = null;
			kis = new int[keysize];
			keyExps = new Expression[keysize];
			for (int i = 0; i < keysize; i++) {
				kis[i] = ((Integer) keyCols.get(i)).intValue();
				keyExps[i] = exps[kis[i]];
			}

			for (int iField = 0; iField < fsize; iField++) {
				if (ais[iField] == DataStruct.Col_AutoIncrement) {
					continue;
				}

				field = fields[iField];
				if (field != null && field.trim().length() > 0) {
					if (fieldAll == null) {
						fieldAll = "(" + field;
					} else {
						fieldAll += ", " + field;
					}
				}
			}
			if (fieldAll == null || fieldAll.trim().length() < 1) {
				//throw new RQException("Field Names is Invalid!");
				MessageManager mm = DataSetMessage.get();
				throw new RQException(mm.getMessage("error.invalidField"));
			}
			fieldAll += ")";

			if (dsConfig != null) {
				batchSize = dsConfig.getBatchSize();
				if (batchSize < 1) {
					batchSize = 1;
				}
				dbCharset = dsConfig.getDBCharset();
				tranSQL = dsConfig.getNeedTranSentence();
				tranContent = dsConfig.getNeedTranContent();
				if ((tranContent || tranSQL) && dbCharset == null) {
					String dbName = "";
					DBInfo info = dbs.getInfo();
					if (info != null) {
						dbName = info.getName();
					}
					MessageManager mm = DataSetMessage.get();
					throw new RQException(mm.getMessage("error.fromCharset", dbName));
				}

				toCharset = dsConfig.getClientCharset();
				if ((tranContent || tranSQL) && toCharset == null) {
					String dbName = "";
					DBInfo info = dbs.getInfo();
					if (info != null) {
						dbName = info.getName();
					}
					MessageManager mm = DataSetMessage.get();
					throw new RQException(mm.getMessage("error.toCharset", dbName));
				}

				dbType = dsConfig.getDBType();
			} else {
				tranContent = false;
			}
			String dbName = "";
			try {
				boolean clearAll = false;
				if (opt != null && opt.indexOf("a") > -1) {
					//Logger.debug("Clear all the records from "+table);
					MessageManager mm = DataSetMessage.get();
					Logger.debug(mm.getMessage("info.clearTable", table));
					String sql = "delete from " + addTilde(table, dbs);
					st = con.createStatement();
					st.execute(sql);
					st.close();
					clearAll = true;
				}

				// edited by bd, 2022.4.14, 把这部分赋值提前，到生成语句之前设置，这是为了用这个表达式去计算主键中可能包含的空值
				/* 改造如下逐行执行为批量执行 xq 2015.4.21 begin */
				ArrayList<Expression> updateParams = new ArrayList<Expression>();
				ArrayList<Expression> updateFields = new ArrayList<Expression>();
				ArrayList<Expression> primaryParams = new ArrayList<Expression>();
				ArrayList<Expression> insertParams = new ArrayList<Expression>();
				ArrayList<Byte> updateTypes = new ArrayList<Byte>();
				ArrayList<Byte> primaryTypes = new ArrayList<Byte>();
				ArrayList<Byte> insertTypes = new ArrayList<Byte>();

				for (int iField = 0; iField < fsize; iField++) {
					if (ais[iField] == DataStruct.Col_AutoIncrement) {
						continue;
					}
					insertParams.add(exps[iField]);
					insertTypes.add(tColTypes[iField]);
					// edited by bd, 2022.4.14, update时不再更新主键值
					if (ais[iField] != DatabaseUtil.Col_NormalKey) {
						updateParams.add(exps[iField]);
						updateFields.add(new Expression(fields[iField]));
						updateTypes.add(tColTypes[iField]);
					}
				}
				ArrayList<Expression> primaryFields = new ArrayList<Expression>();
				for (int ki = 0; ki < keysize; ki++) {
					primaryFields.add(new Expression(fields[kis[ki]]));
					primaryParams.add(exps[kis[ki]]);
					primaryTypes.add(tColTypes[kis[ki]]);
					updateFields.add(new Expression(fields[kis[ki]]));
				}

				//updateParams.addAll(insertParams);
				//updateTypes.addAll(insertTypes);
				updateParams.addAll(primaryParams);
				updateTypes.addAll(primaryTypes);

				// added by bd, 2022.4.14, 计算每个主键是否是空值
				Expression[] params = new Expression[primaryParams.size()];
				ArrayList<Integer> nullKeys = new ArrayList<Integer>();
				primaryParams.toArray(params);
				int paramCount = params.length;
				int len = srcSeq.length();

				ComputeStack stack = ctx.getComputeStack();
				Current current = new Current(srcSeq);
				stack.push(current);

				try {
					for (int p = 0; p < paramCount; ++p) {
						if (params[p] == null)
							continue;
						for (int i = 1; i <= len; ++i) {
							current.setCurrent(i);
							Object pv = params[p].calculate(ctx);
							if (pv == null) {
								nullKeys.add(p);
								break;
							}
						}
					}
				} finally {
					stack.pop();
				}
				int nkeys = nullKeys.size();
				int nulls = (int) Math.round(Math.pow(2, nkeys)) - 1;
				String[] ucons = nulls > 0 ? new String[nulls] : null;
				
				String condition = null;
				String key = "";
				String check_sql = "";
				String update_sql = "";
				String insert_sql = "";
				String delete_sql = "";
				String[] ch_sqls = nulls > 0 ? new String[nulls] : null;
				String[] up_sqls = nulls > 0 ? new String[nulls] : null;
				String[] de_sqls = nulls > 0 ? new String[nulls] : null;
				for (int j = 0; j < keysize; j++) {
					key = fields[kis[j]];
					if (key != null && key.trim().length() > 0) {
						if (condition == null) {
							condition = "(" + key + " = ?)";
							// added by bd, 2022.4.14, 如果当前列可能有空值，则在对应的几个位置的条件都设上is null条件
							if (ucons != null) {
								int kloc = nullKeys.indexOf(j);
								if (kloc>-1) {
									// 当前主键可能存在空值
									for (int k = 0; k < nulls; k++) {
										if ((k & (1<<kloc)) == 0) {
											ucons[k] = key + " is null";
										}
										else {
											ucons[k] = "(" + key + " = ?)";
										}
									}
								}
							}
						} else {
							condition += " and (" + key + " = ?)";
							// added by bd, 2022.4.14, 如果当前列可能有空值，则在对应的几个位置的条件都设上is null条件
							if (ucons != null) {
								int kloc = nullKeys.indexOf(j);
								if (kloc>-1) {
									// 当前主键可能存在空值
									for (int k = 0; k < nulls; k++) {
										if ((k & (1<<kloc)) == 0) {
											ucons[k] += " and " + key + " is null";
										}
										else {
											ucons[k] += " and (" + key + " = ?)";
										}
									}
								}
							}
						}
					}
				}
				check_sql = "select count(*) from " + addTilde(table, dbs) + " where " + condition;
				delete_sql = "delete from " + addTilde(table, dbs) + " where " + condition;
				if (ucons != null) {
					for (int i = 0; i < nulls; i++) {
						ch_sqls[i] = "select count(*) from " + addTilde(table, dbs) + " where " + ucons[i];
						de_sqls[i] = "delete from " + addTilde(table, dbs) + " where " + ucons[i];
					}
				}
				String usets = null;
				for (int iField = 0; iField < fsize; iField++) {
					// edited by bd, 2022.4.14, 更新时不再将主键设一遍了
					if (ais[iField] == DataStruct.Col_AutoIncrement || 
							ais[iField] == DatabaseUtil.Col_NormalKey) {
						continue;
					}

					field = fields[iField];
					if (field != null && field.trim().length() > 0) {// ？？此处的field没有过滤是否为主键，造成主键会重设一遍，xq
																		// 2015.4.21
						if (usets == null) {
							usets = field + " = ?";
						} else {
							usets += ", " + field + " = ?";
						}
					}
				}
				if (usets == null || usets.trim().length() < 1) {
					if (fields == null || fields.length == 0) {
						//throw new RQException("Field Names of Values is Invalid!");
						MessageManager mm = DataSetMessage.get();
						throw new RQException(mm.getMessage("error.invalidField"));
					}
					else {
						// edited by bd, 2022.4.14, 如果用户只更新了主键值（其实是错误的，但是就给他更新一个主键就是了）
						for (int iField = 0; iField < fsize; iField++) {
							if (ais[iField] == DataStruct.Col_AutoIncrement) {
								continue;
							}

							field = fields[iField];
							if (field != null && field.trim().length() > 0) {
								if (usets == null) {
									usets = field + " = ?";
									break;
								}
							}
						}
						if (usets == null || usets.trim().length() < 1) {
							//throw new RQException("Field Names of Values is Invalid!");
							MessageManager mm = DataSetMessage.get();
							throw new RQException(mm.getMessage("error.invalidFieldValue"));
						}
						else {
							//Logger.warn("No field value will be updated!");
							MessageManager mm = DataSetMessage.get();
							Logger.warn(mm.getMessage("info.noUpdateField"));
						}
					}
				}
				update_sql = "update " + addTilde(table, dbs) + " set " + usets + " where " + condition;
				if (ucons != null) {
					for (int i = 0; i < nulls; i++) {
						up_sqls[i] = "update " + addTilde(table, dbs) + " set " + usets + " where " + ucons[i];
					}
				}
				String sets = null;
				for (int iField = 0; iField < fsize; iField++) {
					if (ais[iField] == DataStruct.Col_AutoIncrement) {
						continue;
					}
					field = fields[iField];
					if (field != null && field.trim().length() > 0) {
						if (sets == null) {
							fieldAll = "(" + field;
							sets = "( ?";
						} else {
							fieldAll += ", " + field;
							sets += ", ?";
						}
					}
				}
				if (sets == null || sets.trim().length() < 1) {
					//throw new RQException("Field Values of Values is Invalid!");
					MessageManager mm = DataSetMessage.get();
					throw new RQException(mm.getMessage("error.invalidFieldValue"));
				} else {
					sets += " )";
				}
				if (fieldAll == null || fieldAll.trim().length() < 1) {
					//throw new RQException("Field Names of Values is Invalid!");
					MessageManager mm = DataSetMessage.get();
					throw new RQException(mm.getMessage("error.invalidField"));
				} else {
					fieldAll += " )";
				}
				insert_sql = "insert into " + addTilde(table, dbs) + " " + fieldAll + " values " + sets;

				if (tranSQL) {
					check_sql = new String(check_sql.getBytes(), dbCharset);
					update_sql = new String(update_sql.getBytes(), dbCharset);
					insert_sql = new String(insert_sql.getBytes(), dbCharset);
					delete_sql = new String(delete_sql.getBytes(), dbCharset);
					if (ucons != null) {
						for (int i = 0; i < nulls; i++) {
							ch_sqls[i] = new String(ch_sqls[i].getBytes(), dbCharset);
							de_sqls[i] = new String(de_sqls[i].getBytes(), dbCharset);
							up_sqls[i] = new String(up_sqls[i].getBytes(), dbCharset);
						}
					}
				}


				boolean isAutoDetect = true;
				if (opt != null) {// 没有选项时，使用自动判断记录的插入或更新
					if (opt.indexOf('d') > -1 && !clearAll) {
						//Logger.debug("Delete-only, preparing delete records: "+delete_sql);
						MessageManager mm = DataSetMessage.get();
						Logger.debug(mm.getMessage("info.deleteOnly"));
						Expression[] keysFields = new Expression[primaryFields.size()];
						primaryFields.toArray(keysFields);
						// edited by bd, 2022.4.15, 删除时，主键如果为null，那判断时无法使用F=?，需要使用F is null，为此，需要把remainSeq中，主键为null的记录拆出来单独执行
						executeDifferBatch(srcSeq, compSeq, delete_sql, primaryFields, primaryParams, primaryTypes, ctx, dbs, con,
								dbCharset, tranSQL, dbType, dbName, batchSize, de_sqls, nullKeys, keysize);
						isAutoDetect = false;
					} else if (opt.indexOf('i') > -1) {
						if (compSeq != null && compSeq.length() > 0) {
							//Logger.debug("Insert only, preparing insert new-records: "+insert_sql);
							MessageManager mm = DataSetMessage.get();
							Logger.debug(mm.getMessage("info.insertOnly"));
							Expression[] keysParam = new Expression[primaryParams.size()];
							primaryParams.toArray(keysParam);
							Expression[] keysFields = new Expression[primaryFields.size()];
							primaryFields.toArray(keysFields);
							Sequence insertSeq = diffSequence(srcSeq, compSeq, keysParam, keysFields, ctx);
							IArray oldMems = srcSeq.getMems();
							srcSeq.setMems(insertSeq.getMems());
							executeBatchSql(srcSeq, insert_sql, insertParams,
									insertTypes, ctx, dbs, con, dbCharset, tranSQL, dbType, dbName, batchSize);
							srcSeq.setMems(oldMems);
						} else {
							//Logger.debug("Insert-Only, preparing insert records: "+insert_sql);
							MessageManager mm = DataSetMessage.get();
							Logger.debug(mm.getMessage("info.insertOnly"));
							executeBatchSql(srcSeq, insert_sql, insertParams, insertTypes, ctx, dbs, con, dbCharset,
									tranSQL, dbType, dbName, batchSize);
						}
						isAutoDetect = false;
					} else if (opt.indexOf('u') > -1) {
						if (compSeq != null && compSeq.length() > 0) {
							//Logger.debug("Update-only, update changed-records: "+update_sql);
							MessageManager mm = DataSetMessage.get();
							Logger.debug(mm.getMessage("info.updateOnly"));
							Expression[] keysParam = new Expression[primaryParams.size()];
							primaryParams.toArray(keysParam);
							Expression[] keysFields = new Expression[primaryFields.size()];
							primaryFields.toArray(keysFields);
							Sequence remainSeq = isectSequence(srcSeq, compSeq, keysParam, keysFields, ctx);
							IArray oldMems = srcSeq.getMems();
							srcSeq.setMems(remainSeq.getMems());
							//edited by bd, 2022.4.15, @u选项时，根据最后keysize个位置的空值设置更新语句
							executeDifferBatch(compSeq, srcSeq, update_sql, updateParams, updateFields, updateTypes, ctx, dbs, con,
									dbCharset, tranSQL, dbType, dbName, batchSize, up_sqls, nullKeys, keysize);
							srcSeq.setMems(oldMems);
						} else {
							executeBatchSql(srcSeq, update_sql, updateParams, updateTypes, ctx, dbs, con, dbCharset,
									tranSQL, dbType, dbName, batchSize);
						}
						isAutoDetect = false;
					}
				}
				if (isAutoDetect) {
					if (compSeq != null && compSeq.length() > 0) {
						if (!clearAll) {
							Expression[] keysFields = new Expression[primaryFields.size()];
							primaryFields.toArray(keysFields);
							//Logger.debug("Auto delete, preparing delete lost-records: "+delete_sql);
							MessageManager mm = DataSetMessage.get();
							Logger.debug(mm.getMessage("info.autoDelete"));
							//edited by bd, 2022.4.15, @u选项时，根据最后keysize个位置的空值设置更新语句
							executeDifferBatch(srcSeq, compSeq, delete_sql, primaryFields, primaryParams, primaryTypes, ctx, dbs, con,
									dbCharset, tranSQL, dbType, dbName, batchSize, de_sqls, nullKeys, keysize);
						}
						Expression[] keysParam = new Expression[primaryParams.size()];
						primaryParams.toArray(keysParam);
						//Logger.debug("Auto insert, preparing insert new-records: "+insert_sql);
						MessageManager mm = DataSetMessage.get();
						Logger.debug(mm.getMessage("info.autoInsert"));
						Expression[] keysFields = new Expression[primaryFields.size()];
						primaryFields.toArray(keysFields);
						Sequence insertSeq = diffSequence(srcSeq, compSeq, keysParam, keysFields, ctx);
						IArray oldMems = srcSeq.getMems();
						srcSeq.setMems(insertSeq.getMems());
						executeBatchSql(srcSeq, insert_sql, insertParams,
								insertTypes, ctx, dbs, con, dbCharset, tranSQL, dbType, dbName, batchSize);
						srcSeq.setMems(oldMems);
						Logger.debug("Auto update, preparing update changed-records: "+update_sql);
						Logger.debug(mm.getMessage("info.autoUpdate"));
						Sequence remainSeq = mergeDiffSequence(srcSeq, insertSeq, null, ctx);
						// edited by bd, 2022.4.14, 更新时，主键如果为null，那判断时无法使用F=?，需要使用F is null，为此，需要把remainSeq中，主键为null的记录拆出来单独执行
						srcSeq.setMems(remainSeq.getMems());
						//edited by bd, 2022.4.15, 根据最后keysize个位置的空值设置更新语句
						executeDifferBatch(compSeq, srcSeq, update_sql, updateParams, updateFields, updateTypes, ctx, dbs, con,
								dbCharset, tranSQL, dbType, dbName, batchSize, up_sqls, nullKeys, keysize);
						srcSeq.setMems(oldMems);
					} else {
						Expression[] expParams = new Expression[primaryParams.size()];
						primaryParams.toArray(expParams);
						//edited by bd, 2022.4.15, 根据最后keysize个位置的空值设置查询语句
						Sequence recordCount = query(srcSeq, check_sql, expParams, toByteArray(primaryTypes), opt, ctx,
								dbs, ch_sqls, nullKeys, keysize);
						Sequence updateRecords = new Sequence();
						Sequence insertRecords = new Sequence();
						int rsize = recordCount == null ? 0 : recordCount.length();
						for (int i = 1; i <= rsize; i++) {
							BaseRecord r = (BaseRecord) recordCount.get(i);
							// edited by bd, 2022.12.9, 当查询对应数据为空时，防止错误
							int c = 0;
							if (r != null)
								c = ((Number) r.getFieldValue(0)).intValue();
							//int c = ((Number) r.getFieldValue(0)).intValue();
							if (c > 0) {
								updateRecords.add(srcSeq.get(i));
							} else {
								insertRecords.add(srcSeq.get(i));
							}
						}
						if (updateRecords.length() > 0) {
							//Logger.debug("Auto update, preparing update records: "+update_sql);
							MessageManager mm = DataSetMessage.get();
							Logger.debug(mm.getMessage("info.autoUpdate"));
							//executeBatchSql(updateRecords, update_sql, updateParams, updateTypes, ctx, dbs, con,
							//		dbCharset, tranSQL, dbType, dbName, batchSize);
							executeDifferBatch(null, updateRecords, update_sql, updateParams, updateFields, updateTypes, ctx, dbs, con,
									dbCharset, tranSQL, dbType, dbName, batchSize, up_sqls, nullKeys, keysize);
						}
						if (insertRecords.length() > 0) {
							//Logger.debug("Auto insert, preparing insert records: "+update_sql);
							MessageManager mm = DataSetMessage.get();
							Logger.debug(mm.getMessage("info.autoInsert"));
							executeBatchSql(insertRecords, insert_sql, insertParams, insertTypes, ctx, dbs, con,
									dbCharset, tranSQL, dbType, dbName, batchSize);
						}
					}
				}

				/* 改造如下逐行执行为批量执行 xq 2015.4.21 end */
			} catch (RQException e) {
				//com.scudata.common.Logger.debug("update error:", e);
				MessageManager mm = DataSetMessage.get();
				Logger.debug(mm.getMessage("error.update", e.getMessage()));
				if (dbs.getErrorMode()) {
					dbs.setError(new SQLException(e.getMessage(), "Error: 5001 Update error: ", 5001));
				}
			}
		} catch (RQException re) {
			throw re;
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				if (opt == null || opt.indexOf('k') < 0) {
					con.commit();
				}
				if (st != null) {
					st.close();
				}
			} catch (Exception e) {
				throw new RQException(e.getMessage(), e);
			}
		}
		return 0;// 批量处理，不知道在哪出错，返回值无意义，始终返回0 xq 2015.4.21
	}

	/**
	 * DBObject调用，根据srcSeries更新表table中的字段fields
	 * @param srcSeries	Sequence 源排列
	 * @param table	String 表名
	 * @param fields	String[] 字段名
	 * @param fopts	String[] p：字段是主键，a：字段是自增字段
	 * @param exps	Expression[] 值表达式
	 * @param opt	String t：作为非更新序表处理，k：完成后不清理状态
	 * @param dbs	DBSession
	 * @param ctx	Context
	 * @return int 	增加返回值，成功更新条数
	 */
	public static int update(Sequence srcSeries, String table, String[] fields, String[] fopts, Expression[] exps,
			String opt, DBSession dbs, Context ctx) {
		if (1 > 0)
			return update(srcSeries, null, table, fields, fopts, exps, opt, dbs, ctx);
		boolean oClear = false;
		if (opt != null && opt.indexOf("a") > -1) {
			oClear = true;
		}
		boolean oInsert = false;
		if (opt != null && opt.indexOf("i") > -1) {
			oInsert = true;
		}
		if ( srcSeries == null || srcSeries.length() == 0) {
			return 0;
		}
		Statement st = null;
		Connection con = null;
		String dbCharset = null;
		String toCharset = null;
		boolean tranSQL = false;
		boolean tranContent = true;

		int fsize = fields.length;
		String field = "";
		String fieldAll = null;

		try {
			DBConfig dsConfig = null;
			if (dbs != null && dbs.getInfo() instanceof DBConfig) {
				dsConfig = (DBConfig) dbs.getInfo();
			}
			if (dbs != null) {
				Object session = dbs.getSession();
				if (session instanceof Connection) {
					con = (Connection) session;
				}
			}
			if (con == null || con.isClosed()) {
				String dbName = "";
				DBInfo info = dbs.getInfo();
				if (info != null) {
					dbName = info.getName();
				}
				MessageManager mm = DataSetMessage.get();
				throw new RQException(mm.getMessage("error.conClosed", dbName));
			}
			boolean bb = true;

			ResultSet rs = null;
			byte[] tColTypes = null;
			ArrayList<Integer> keyCols = new ArrayList<Integer>();
			ArrayList<String> autoKeys = new ArrayList<String>();
			byte[] ais = new byte[fsize];
			if (fopts != null) {
				int osize = fopts.length;
				if (osize > fsize) {
					osize = fsize;
				}
				for (int i = 0; i < osize; i++) {
					String fopt = fopts[i];
					if (fopt != null) {
						if (fopt.indexOf("p") > -1) {
							keyCols.add(new Integer(i));
						}
						if (fopt.indexOf("a") > -1) {
							ais[i] = DataStruct.Col_AutoIncrement;
							autoKeys.add(fields[i]);
						}
						else if (fopt.indexOf("p") > -1) {
							// edited by bd, 2022.4.14, 更新时不再设置主键值，这里预判断
							ais[i] = Col_NormalKey;
						}
						else {
							ais[i] = 0;
						}
					}
				}
			}
			int[] kis = null;
			int keysize = keyCols.size();
			if (tColTypes == null || tColTypes.length < 1 || keysize < 1) {
				String sql = "select";
				for (int i = 0, iSize = fields.length; i < iSize; i++) {
					sql += " " + addTilde(fields[i], dbs);
					if (i < iSize - 1) {
						sql += ",";
					}
				}
				sql += " from " + addTilde(table, dbs) + " where 1 = 0";
				PreparedStatement pst = null;
				try {
					pst = con.prepareStatement(sql);
					rs = pst.executeQuery();
					ResultSetMetaData rsmd = rs.getMetaData();
					int colSize = rsmd.getColumnCount();
					tColTypes = new byte[fsize];

					for (int ci = 0; ci < colSize; ci++) {
						String colname = rsmd.getColumnLabel(ci + 1);
						colname = tranName(colname, tranContent, dbCharset, toCharset, bb, opt);
						if (colname == null || colname.trim().length() < 1) {
							continue;
						}
						for (int fi = 0; fi < fsize; fi++) {
							if (colname.equalsIgnoreCase(fields[fi])) {
								int sqlType = rsmd.getColumnType(ci + 1);
								byte rqType = com.scudata.common.Types.getTypeBySQLType(sqlType);
								tColTypes[fi] = rqType;
								break;
							}
						}
					}

					if (!oClear && !oInsert && keysize < 1) {
						DatabaseMetaData dbmd = con.getMetaData();
						String schema = "", tn = "";
						if (table.indexOf(".") > 0) {
							String[] tns = table.split("\\.");
							if (tns == null || tns.length < 2) {
								int loc = table.indexOf(".");
								tns = new String[2];
								tns[0] = table.substring(0, loc);
								tns[1] = table.substring(loc + 1);
							}
							schema = tns[0];
							tn = tns[1];
						} else {
							tn = table;
						}
						try {
							rs.close();
							rs = dbmd.getPrimaryKeys("", schema, tn);
							while (rs.next()) {
								String columnName = rs.getString("COLUMN_NAME");
								if (columnName == null) {
									continue;
								}
								int ki = -1;
								for (int i = 0; i < fsize; i++) {
									if (columnName.equalsIgnoreCase(fields[i])) {
										ki = i;
										break;
									}
								}
								if (ki > -1) {
									keyCols.add(new Integer(ki));
								}
							}
							keysize = keyCols.size();
						}
						catch (Exception e) {
						}
					}
				} catch (Exception e) {
					throw new RQException(e.getMessage(), e);
				} finally {
					try {
						if (rs != null) {
							rs.close();
						}
						if (pst != null) {
							pst.close();
						}
					} catch (Exception e) {
						throw new RQException(e.getMessage(), e);
					}
					rs = null;
				}
			}
			if (!oClear && !oInsert && keysize < 1) {
				DataStruct ds1 = null;
				if (srcSeries instanceof Table) {
					ds1 = ((Table) srcSeries).dataStruct();
				}
				if (ds1 == null ) {
					Object o1 = srcSeries.get(1);
					if (o1 instanceof BaseRecord) {
						ds1 = ((BaseRecord) o1).dataStruct();
					}
				}
				if (ds1 != null) {
					String[] keys = ds1.getPrimary();
					int kc = keys == null ? 0 : keys.length;
					for (int i = 0; i < kc; i++) {
						String key = keys[i];
						if (key == null) {
							continue;
						}
						int ki = -1;
						for (int j = 0; j < fsize; j++) {
							if (key.equalsIgnoreCase(fields[j])) {
								ki = j;
								break;
							}
						}
						if (ki > -1) {
							keyCols.add(new Integer(ki));
						}
					}
				}
				keysize = keyCols.size();
			}
			if (!oClear && !oInsert && keysize < 1) {
				keyCols.add(new Integer(0));
				keysize = keyCols.size();
			}

			Expression[] keyExps = null;
			kis = new int[keysize];
			keyExps = new Expression[keysize];
			for (int i = 0; i < keysize; i++) {
				kis[i] = ((Integer) keyCols.get(i)).intValue();
				keyExps[i] = exps[kis[i]];
			}

			for (int iField = 0; iField < fsize; iField++) {
				if (ais[iField] == DataStruct.Col_AutoIncrement) {
					continue;
				}

				field = fields[iField];
				if (field != null && field.trim().length() > 0) {
					if (fieldAll == null) {
						fieldAll = "(" + field;
					} else {
						fieldAll += ", " + field;
					}
				}
			}
			if (fieldAll == null || fieldAll.trim().length() < 1) {
				//throw new RQException("Field Names is Invalid!");
				MessageManager mm = DataSetMessage.get();
				throw new RQException(mm.getMessage("error.invalidField"));
			}
			fieldAll += ")";
			if (dsConfig != null) {
				dbCharset = dsConfig.getDBCharset();
				tranSQL = dsConfig.getNeedTranSentence();
				tranContent = dsConfig.getNeedTranContent();
				if ((tranContent || tranSQL) && dbCharset == null) {
					String dbName = "";
					DBInfo info = dbs.getInfo();
					if (info != null) {
						dbName = info.getName();
					}
					MessageManager mm = DataSetMessage.get();
					throw new RQException(mm.getMessage("error.fromCharset", dbName));
				}

				toCharset = dsConfig.getClientCharset();
				if ((tranContent || tranSQL) && toCharset == null) {
					String dbName = "";
					DBInfo info = dbs.getInfo();
					if (info != null) {
						dbName = info.getName();
					}
					MessageManager mm = DataSetMessage.get();
					throw new RQException(mm.getMessage("error.toCharset", dbName));
				}

			} else {
				tranContent = false;
			}

			DataStruct tds = null;
			if (srcSeries instanceof Table) {
				tds = ((Table) srcSeries).dataStruct();
			}
			if (tds == null && srcSeries.length()>0) {
				Object o1 = srcSeries.get(1);
				if (o1 instanceof BaseRecord) {
					tds = ((BaseRecord) o1).dataStruct();
				}
			}
			try {
				if (oClear) {
					//Logger.debug("Clear all the records from table +"+table);
					MessageManager mm = DataSetMessage.get();
					Logger.debug(mm.getMessage("info.clearTable", table));
					String sql = "delete from " + addTilde(table, dbs);
					st = con.createStatement();
					st.execute(sql);
					st.close();
				}
				
				// edited by bd, 2022.4.15, 把这部分赋值提前，到生成语句之前设置，这是为了用这个表达式去计算主键中可能包含的空值
				// 改造如下逐行执行为批量执行 xq 2015.4.21 begin
				ArrayList<Expression> updateParams = new ArrayList<Expression>();
				ArrayList<Expression> primaryParams = new ArrayList<Expression>();
				ArrayList<Expression> insertParams = new ArrayList<Expression>();
				ArrayList<Byte> updateTypes = new ArrayList<Byte>();
				ArrayList<Byte> primaryTypes = new ArrayList<Byte>();
				ArrayList<Byte> insertTypes = new ArrayList<Byte>();

				for (int iField = 0; iField < fsize; iField++) {
					if (ais[iField] == DataStruct.Col_AutoIncrement) {
						continue;
					}
					insertParams.add(exps[iField]);
					insertTypes.add(tColTypes[iField]);
					// edited by bd, 2022.4.14, update时不再更新主键值
					if (ais[iField] != DatabaseUtil.Col_NormalKey) {
						updateParams.add(exps[iField]);
						updateTypes.add(tColTypes[iField]);
					}
				}

				for (int ki = 0; ki < keysize; ki++) {
					primaryParams.add(exps[kis[ki]]);
					primaryTypes.add(tColTypes[kis[ki]]);
				}

				//updateParams.addAll(insertParams);
				//updateTypes.addAll(insertTypes);
				updateParams.addAll(primaryParams);
				updateTypes.addAll(primaryTypes);

				String condition = null;
				String key = "";
				String check_sql = "";
				String update_sql = "";
				String insert_sql = "";
				for (int j = 0; j < keysize; j++) {
					key = fields[kis[j]];
					if (key != null && key.trim().length() > 0) {
						if (condition == null) {
							condition = "(" + key + " = ?)";
						} else {
							condition += " and (" + key + " = ?)";
						}
					}
				}
				check_sql = "select count(*) from " + addTilde(table, dbs) + " where " + condition;
				String sets = null;
				for (int iField = 0; iField < fsize; iField++) {
					if (ais[iField] == DataStruct.Col_AutoIncrement) {
						continue;
					}

					field = fields[iField];
					if (field != null && field.trim().length() > 0) {// ？？此处的field没有过滤是否为主键，造成主键会重设一遍，xq 2015.4.21
						if (sets == null) {
							sets = field + " = ?";
						} else {
							sets += ", " + field + " = ?";
						}
					}
				}
				if (sets == null || sets.trim().length() < 1) {
					//throw new RQException("Field Names of Values is Invalid!");
					MessageManager mm = DataSetMessage.get();
					throw new RQException(mm.getMessage("error.invalidField", table));
				}
				update_sql = "update " + addTilde(table, dbs) + " set " + sets + " where " + condition;
				sets = null;
				for (int iField = 0; iField < fsize; iField++) {
					if (ais[iField] == DataStruct.Col_AutoIncrement) {
						continue;
					}
					field = fields[iField];
					if (field != null && field.trim().length() > 0) {
						if (sets == null) {
							fieldAll = "(" + field;
							sets = "( ?";
						} else {
							fieldAll += ", " + field;
							sets += ", ?";
						}
					}
				}
				if (sets == null || sets.trim().length() < 1) {
					//throw new RQException("Field Values of Values is Invalid!");
					MessageManager mm = DataSetMessage.get();
					throw new RQException(mm.getMessage("error.invalidFieldValue", table));
				} else {
					sets += " )";
				}
				if (fieldAll == null || fieldAll.trim().length() < 1) {
					//throw new RQException("Field Names of Values is Invalid!");
					MessageManager mm = DataSetMessage.get();
					throw new RQException(mm.getMessage("error.invalidField", table));
				} else {
					fieldAll += " )";
				}
				insert_sql = "insert into " + addTilde(table, dbs) + " " + fieldAll + " values " + sets;

				if (tranSQL) {
					check_sql = new String(check_sql.getBytes(), dbCharset);
					update_sql = new String(update_sql.getBytes(), dbCharset);
					insert_sql = new String(insert_sql.getBytes(), dbCharset);
				}


				boolean isAutoDetect = true;
				if (opt != null) {// 没有选项时，使用自动判断记录的插入或更新
					if (oClear || oInsert) {// 强制对每条记录执行insert，合法性由程序员保证，通常会配上a，先执行删除
						//Logger.debug("Insert-only, preparing insert records: "+insert_sql);
						MessageManager mm = DataSetMessage.get();
						Logger.debug(mm.getMessage("info.insertOnly", table));
						executeBatchSql(srcSeries, insert_sql, insertParams, insertTypes, ctx, dbs);
						isAutoDetect = false;
					} else if (opt.indexOf('u') > -1) {// 强制对每条记录执行update，合法性由程序员保证
						//Logger.debug("Update-only, preparing update records: "+update_sql);
						MessageManager mm = DataSetMessage.get();
						Logger.debug(mm.getMessage("info.updateOnly", table));
						executeBatchSql(srcSeries, update_sql, updateParams, updateTypes, ctx, dbs);
						isAutoDetect = false;
					}
				}
				if (isAutoDetect) {// 使用自动探测记录的插入和更新状态
					Expression[] expParams = new Expression[primaryParams.size()];
					primaryParams.toArray(expParams);
					
					Sequence recordCount = query(srcSeries, check_sql, expParams, toByteArray(primaryTypes), opt, ctx,
							dbs, null, null, 0);
					Sequence updateRecords = new Sequence();
					Sequence insertRecords = new Sequence();
					int rsize = recordCount == null ? 0 : recordCount.length();
					for (int i = 1; i <= rsize; i++) {
						BaseRecord r = (BaseRecord) recordCount.get(i);
						int c = ((Number) r.getFieldValue(0)).intValue();
						if (c > 0) {
							updateRecords.add(srcSeries.get(i));
						} else {
							insertRecords.add(srcSeries.get(i));
						}
					}
					if (updateRecords.length() > 0) {
						//Logger.debug("Auto update, preparing update records: "+update_sql);
						MessageManager mm = DataSetMessage.get();
						Logger.debug(mm.getMessage("info.updateOnly", table));
						executeBatchSql(updateRecords, update_sql, updateParams, updateTypes, ctx, dbs);
					}
					if (insertRecords.length() > 0) {
						//Logger.debug("Auto insert, preparing insert records: "+insert_sql);
						MessageManager mm = DataSetMessage.get();
						Logger.debug(mm.getMessage("info.insertOnly", table));
						executeBatchSql(insertRecords, insert_sql, insertParams, insertTypes, ctx, dbs);
					}
				}

				// 改造如下逐行执行为批量执行 xq 2015.4.21 end 
			} catch (RQException e) {
				//com.scudata.common.Logger.debug("update error:", e);
				MessageManager mm = DataSetMessage.get();
				Logger.debug(mm.getMessage("error.update", e.getMessage()));
				if (dbs.getErrorMode()) {
					dbs.setError(new SQLException(e.getMessage(), "Error: 5001 Update error: ", 5001));
				}
			}
		} catch (RQException re) {
			throw re;
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				if (opt == null || opt.indexOf('k') < 0) {
					con.commit();
				}
				if (st != null) {
					st.close();
				}
			} catch (Exception e) {
				throw new RQException(e.getMessage(), e);
			}
		}
		return 0;// 批量处理，不知道在哪出错，返回值无意义，始终返回0 xq 2015.4.21
	}

	private static void executeBatchSql(Sequence srcSeries, String sql, ArrayList<Expression> exps, ArrayList<Byte> expTypes,
			Context ctx, DBSession dbs) {
		Expression[] expParams = new Expression[exps.size()];
		exps.toArray(expParams);
		execute(srcSeries, sql, expParams, toByteArray(expTypes), ctx, dbs);
	}

	// edited by bd, 2022.4.15, 添加参数de_sqls和nullKeys，这是考虑在执行删除sql时，如果键值中有空值需要用is null的判断，对应语句从de_sqls中获取，
	// nullKeys中记录的是各个可能空值的位置，根据空值产生的位置获取de_sqls中语句位置，如可能有3个空值，全是空值对应位置0，第1个空值对应110……等
	// 添加keysize记录共几个键，用于update时的比较
	private static void executeDifferBatch(Sequence srcSeq, Sequence compSeq, String sql,
			ArrayList<Expression> paramExps, ArrayList<Expression> oldFieldExps, ArrayList<Byte> paramTypes, Context ctx, DBSession dbs, Connection con,
			String dbCharset, boolean tranSQL, int dbType, String dbn, int batchSize, String[] nullSqls, ArrayList<Integer> nullKeys, int keysize) {
		Expression[] params = new Expression[paramExps.size()];
		paramExps.toArray(params);
		Expression[] fieldParams = new Expression[oldFieldExps.size()];
		oldFieldExps.toArray(fieldParams);
		byte[] types = toByteArray(paramTypes);

		if (compSeq == null || compSeq.length() == 0) {
			return;
		}
		int pCount = params == null ? 0 : params.length;
		// edited by bd, 2022.4.15, keyStart记录从第几个位置起已经是键值了。
		int keyStart = pCount - keysize;
		int len = compSeq.length();
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(compSeq);
		stack.push(current);
		Sequence usingParams = new Sequence();
		int nulls = nullSqls == null ? 0 : nullSqls.length;
		boolean nkeys = nullKeys != null && nullKeys.size() > 0 && nulls > 0;
		ArrayList<Sequence> nullParams = null;
		if (nkeys) {
			nullParams = new ArrayList<Sequence>(nulls);
			for (int i = 0; i < nulls; i++)
				nullParams.add(new Sequence());
		}
		try {
			for (int i = 1; i <= len; ++i) {
				current.setCurrent(i);
				Sequence pValues = new Sequence();
				int nloc = 0;
				for (int p = 0; p < pCount; ++p) {
					if (params[p] != null) {
						Object pv = params[p].calculate(ctx);
						if (nkeys && p >= keyStart && pv == null) {
							int kloc = nullKeys.indexOf(p-keyStart);
							nloc = nloc + (1<<kloc);
							// 如果改成了is null判断，对应的参数不必设了
							// 还是得先设定，不然没法执行比较了
						}
						pValues.add(pv);
					}
				}
				if (nloc > 0) {
					nloc = nulls - nloc;
					nullParams.get(nloc).add(pValues);
				}
				else {
					usingParams.add(pValues);
				}
			}
		} finally {
			stack.pop();
		}

		Sequence initParams = new Sequence();
		if (srcSeq != null && srcSeq.length() > 0) {
			len = srcSeq.length();
			stack = ctx.getComputeStack();
			current = new Current(srcSeq);
			stack.push(current);
			try {
				for (int i = 1; i <= len; ++i) {
					current.setCurrent(i);
					Sequence pValues = new Sequence();
					for (int p = 0; p < pCount; ++p) {
						if (fieldParams[p] != null)
							pValues.add(fieldParams[p].calculate(ctx));
					}
					initParams.add(pValues);
				}
			} finally {
				stack.pop();
			}
		}
		usingParams = usingParams.sort(null);
		initParams = initParams.sort(null);
		Sequence diffParamSeq = usingParams.diff(initParams, true);
		len = diffParamSeq.length();
		if (len < 1 && !nkeys)
			return;
		Object[][] valueGroup = new Object[len][pCount];
		for (int i = 1; i <= len; ++i) {
			Sequence seq = (Sequence) diffParamSeq.get(i);
			valueGroup[i - 1] = seq.toArray();
		}
		executeBatch(sql, valueGroup, types, dbs, con, dbCharset, tranSQL, dbType, dbn, batchSize, true);

		// 如果compSeq中包含空值键
		if (nkeys) {
			for (int i = 0; i < nulls; i++) {
				Sequence nullParam = nullParams.get(i);
				nullParam = nullParam.sort(null);
				diffParamSeq = nullParam.diff(initParams, true);
				len = diffParamSeq.length();
				if (len < 1)
					continue;
				valueGroup = new Object[len][pCount];
				sql = nullSqls[i];
				for (int j = 1; j <= len; ++j) {
					Sequence seq = (Sequence) diffParamSeq.get(j);
					// 比较后，再在这里去掉参数中的空值
					int psize = seq.length();
					ArrayList<Object> ps = new ArrayList<Object>();
					for (int p = 0; p < psize; p++) {
						Object pv = seq.get(p+1);
						if (p < keyStart || pv != null ) {
							ps.add(pv);
						}
					}
					valueGroup[j - 1] = ps.toArray();
				}
				executeBatch(sql, valueGroup, types, dbs, con, dbCharset, tranSQL, dbType, dbn, batchSize, true);
			}
		}
	}
	
	private static Sequence mergeDiffSequence(Sequence seq1, Sequence seq2, Expression[] exps, Context ctx) {
		Sequence diffAll = new Sequence();
		diffAll.add(seq1);
		diffAll.add(seq2);
		Sequence diffSeq = diffAll.merge(exps, "od", ctx);
		return diffSeq;
	}
	
	private static Sequence diffSequence(Sequence seq1, Sequence seq2, Expression[] exps1, Expression[] exps2, Context ctx) {
		if (exps1 == null || exps1.length < 1 || exps1[0] == null) {
			return mergeDiffSequence(seq1, seq2, exps2, ctx);
		}
		if (exps2 == null || exps2.length < 1 || exps2[0] == null) {
			return mergeDiffSequence(seq1, seq2, exps1, ctx);
		}
		int keyCount = exps1.length;
		IArray mems2 = seq2.getMems();
		int len2 = mems2.size();
		
		final int INIT_GROUPSIZE = HashUtil.getInitGroupSize();
		HashUtil hashUtil = new HashUtil((int)(len2 * 1.2));
		ListBase1 []groups = new ListBase1[hashUtil.getCapacity()];

		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(seq2);
		stack.push(current);

		try {
			for (int i = 1; i <= len2; ++i) {
				Object []keys = new Object[keyCount];
				current.setCurrent(i);
				for (int c = 0; c < keyCount; ++c) {
					keys[c] = exps2[c].calculate(ctx);
				}

				int hash = hashUtil.hashCode(keys, keyCount);
				if (groups[hash] == null) {
					groups[hash] = new ListBase1(INIT_GROUPSIZE);
					groups[hash].add(keys);
				} else {
					int index = HashUtil.bsearch_a(groups[hash], keys, keyCount);
					if (index < 1) {
						groups[hash].add(-index, keys);
					} else {
						groups[hash].add(index, keys);
					}
				}
			}
		} finally {
			stack.pop();
		}
		
		IArray mems1 = seq1.getMems();
		int len1 = mems1.size();
		Sequence result = new Sequence(len1);
		
		current = new Current(seq1);
		stack.push(current);

		try {
			for (int i = 1; i <= len1; ++i) {
				Object []keys = new Object[keyCount];
				current.setCurrent(i);
				for (int c = 0; c < keyCount; ++c) {
					keys[c] = exps1[c].calculate(ctx);
				}

				int hash = hashUtil.hashCode(keys, keyCount);
				if (groups[hash] == null) {
					result.add(mems1.get(i));
				} else {
					int index = HashUtil.bsearch_a(groups[hash], keys, keyCount);
					if (index < 1) {
						result.add(mems1.get(i));
					} else {
						groups[hash].remove(index);
					}
				}
			}
		} finally {
			stack.pop();
		}
		
		result.trimToSize();
		return result;
	}
	
	private static Sequence mergeIntersection(Sequence seq1, Sequence seq2, Expression[] exps, Context ctx) {
		Sequence diffAll = new Sequence();
		diffAll.add(seq1);
		diffAll.add(seq2);
		Sequence diffSeq = diffAll.merge(exps, "oi", ctx);
		return diffSeq;
	}

	private static Sequence isectSequence(Sequence seq1, Sequence seq2, Expression[] exps1, Expression[] exps2, Context ctx) {
		if (exps1 == null || exps1.length < 1 || exps1[0] == null) {
			return mergeIntersection(seq1, seq2, exps2, ctx);
		}
		if (exps2 == null || exps2.length < 1 || exps2[0] == null) {
			return mergeIntersection(seq1, seq2, exps1, ctx);
		}
		int keyCount = exps1.length;
		IArray mems2 = seq2.getMems();
		int len2 = mems2.size();
		
		final int INIT_GROUPSIZE = HashUtil.getInitGroupSize();
		HashUtil hashUtil = new HashUtil((int)(len2 * 1.2));
		ListBase1 []groups = new ListBase1[hashUtil.getCapacity()];

		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(seq2);
		stack.push(current);

		try {
			for (int i = 1; i <= len2; ++i) {
				Object []keys = new Object[keyCount];
				current.setCurrent(i);
				for (int c = 0; c < keyCount; ++c) {
					keys[c] = exps2[c].calculate(ctx);
				}

				int hash = hashUtil.hashCode(keys, keyCount);
				if (groups[hash] == null) {
					groups[hash] = new ListBase1(INIT_GROUPSIZE);
					groups[hash].add(keys);
				} else {
					int index = HashUtil.bsearch_a(groups[hash], keys, keyCount);
					if (index < 1) {
						groups[hash].add(-index, keys);
					} else {
						groups[hash].add(index, keys);
					}
				}
			}
		} finally {
			stack.pop();
		}
		
		IArray mems1 = seq1.getMems();
		int len1 = mems1.size();
		Sequence result = new Sequence(len1);
		
		current = new Current(seq1);
		stack.push(current);

		try {
			for (int i = 1; i <= len1; ++i) {
				Object []keys = new Object[keyCount];
				current.setCurrent(i);
				for (int c = 0; c < keyCount; ++c) {
					keys[c] = exps1[c].calculate(ctx);
				}
	
				int hash = hashUtil.hashCode(keys, keyCount);
				if (groups[hash] != null) {
					int index = HashUtil.bsearch_a(groups[hash], keys, keyCount);
					if (index > 0) {
						result.add(mems1.get(i));
						groups[hash].remove(index);
					}
				}
			}
		} finally {
			stack.pop();
		}
		
		result.trimToSize();
		return result;
	}

	private static void executeBatchPst(Sequence srcSeries, PreparedStatement pst, ArrayList<Expression> exps,
			ArrayList<Byte> expTypes, Context ctx, DBSession dbs, String dbCharset, boolean tranSQL, int dbType,
			String name) {
		Expression[] expParams = new Expression[exps.size()];
		exps.toArray(expParams);
		executePst(srcSeries, pst, expParams, toByteArray(expTypes), ctx, dbs, dbCharset, tranSQL, dbType, name);
	}

	private static void executeBatchSql(Sequence srcSeries, String sql, ArrayList<Expression> exps,
			ArrayList<Byte> expTypes, Context ctx, DBSession dbs, Connection con, String dbCharset, boolean tranSQL,
			int dbType, String dbn, int batchSize) {
		Expression[] expParams = new Expression[exps.size()];
		exps.toArray(expParams);
		execute(srcSeries, sql, expParams, toByteArray(expTypes), ctx, dbs, con, dbCharset, tranSQL, dbType, dbn,
				batchSize);
	}
	
	public static Sequence query(Sequence srcSeries, String sql, Expression[] params, byte[] types, String opt,
			Context ctx, DBSession dbs) {
		return query(srcSeries, sql, params, types, opt, ctx, dbs, null, null, 0);
	}

	/* 共享出该方法，从DBObject挪过来的， xq 2015.4.21 */
	private static Sequence query(Sequence srcSeries, String sql, Expression[] params, byte[] types, String opt,
			Context ctx, DBSession dbs, String[] nullSqls, ArrayList<Integer> nullKeys, int keysize) {
		if (srcSeries == null || srcSeries.length() == 0 || params == null || params.length == 0) {
			return query(sql, null, null, opt, ctx, dbs);
		}

		int pCount = params.length;
		// edited by bd, 2022.4.15, keyStart记录从第几个位置起已经是键值了。
		int keyStart = pCount - keysize;
		int len = srcSeries.length();
		//Object[][] valueGroup = new Object[len][pCount];
		// edited by bd, 2022.4.15, 如果需要应对空值，则参数的个数是不定的
		ArrayList<ArrayList<Object>> vgs = new ArrayList<ArrayList<Object>>(len);

		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(srcSeries);
		stack.push(current);
		
		int nulls = nullSqls == null ? 0 : nullSqls.length;
		boolean nkeys = nullKeys != null && nullKeys.size() > 0 && nulls > 0;
		ArrayList<ArrayList<ArrayList<Object>>> nullParams = null;
		// res 用来依次记录的查询结果来源，0是来自无null值的查询，其它对应nullParams中的成员位置+1
		int[] res = new int[len];
		if (nkeys) {
			nullParams = new ArrayList<ArrayList<ArrayList<Object>>>(nulls);
			for (int i = 0; i < nulls; i++)
				nullParams.add(new ArrayList<ArrayList<Object>>());
		}

		try {
			for (int i = 1; i <= len; ++i) {
				current.setCurrent(i);
				/*
				Object[] paramValues = new Object[pCount];
				valueGroup[i - 1] = paramValues;
				for (int p = 0; p < pCount; ++p) {
					if (params[p] != null)
						paramValues[p] = params[p].calculate(ctx);
				}
				*/
				ArrayList<Object> pValues = new ArrayList<Object>();
				int nloc = 0;
				for (int p = 0; p < pCount; ++p) {
					if (params[p] != null) {
						Object pv = params[p].calculate(ctx);
						if (nkeys && p >= keyStart && pv == null) {
							int kloc = nullKeys.indexOf(p-keyStart);
							nloc = nloc + (1<<kloc);
							// 如果改成了is null判断，对应的参数不必设了
							// 还是得设，不然直接缺失的话，长度有变化，无法与原值比较
						}
						pValues.add(pv);
					}
				}
				if (nloc > 0) {
					nloc = nulls - nloc;
					nullParams.get(nloc).add(pValues);
					res[i-1] = nloc+1;
				}
				else {
					vgs.add(pValues);
				}
			}
		} finally {
			stack.pop();
		}

		Object[][] valueGroup = toGroup(vgs);
		Table tbl = DatabaseUtil.queryGroup(sql, valueGroup, types, dbs, opt, ctx);

		// edited by bd, 2022.4.15, 这个方法在这里是为了更新db.update的，这里@i选项是插入用的，不存在query@i这种返回成一列的情况
		/*
		if (tbl != null && tbl.dataStruct().getFieldCount() == 1 && opt != null && opt.indexOf('i') != -1) {
			return tbl.fieldValues(0);
		} else {
			return tbl;
		}
		*/

		// 如果compSeq中包含空值键
		if (nkeys) {
			ArrayList<Table> nTbls = new ArrayList<Table>(nulls);
			Table tbli = null;
			for (int i = 0; i < nulls; i++) {
				ArrayList<ArrayList<Object>> nullParam = nullParams.get(i);
				valueGroup = toGroup(nullParam, keyStart);
				if (valueGroup != null) {
					//sql = nullSqls[i];
					// 查询时，由于空值不定，因此不设置参数类型了
					//tbli = DatabaseUtil.queryGroup(sql, valueGroup, null, dbs, opt, ctx);
					tbli = DatabaseUtil.queryGroup(nullSqls[i], valueGroup, null, dbs, opt, ctx);
				}
				// edited by bd, 2022.12.9, 自动判断空值时，is null对应的sql有不需要参数的可能性
				else if (nullSqls[i].indexOf("?")<0) {
					valueGroup = new Object[1][];
					valueGroup[0] = null;
					tbli = DatabaseUtil.queryGroup(nullSqls[i], valueGroup, null, dbs, opt, ctx);
				}
				nTbls.add(tbli);
			}
			Sequence result = new Sequence(len);
			int[] locs = new int[nulls+1];
			Object o = null;
			for (int i = 0; i < len; i++) {
				int ri = res[i];
				locs[ri] = locs[ri]+1;
				if (ri <= 0) {
					o = tbl.get(locs[ri]);
				}
				else {
					//o = nTbls.get(ri-1).get(locs[ri]);
					// edited by bd, 2022.12.9, 自动判断空值时，防止nTbls中有可能未记录数据
					Table ot = nTbls.get(ri-1);
					//o = ot == null ? null : ot.get(locs[ri]);
					//edited by bd, 2023.8.11, 当结果序表中存在重复主键的情况时，暂时不知道会出现什么复杂的状况，先处理超出总数量的错误。
					if (ot == null) o = null; 
					else  {
						if(locs[ri]>ot.length()) {
							locs[ri] = ot.length();
						}
						o = ot.get(locs[ri]);
					}	
				}
				result.add(o);
			}
			return result;
		}
		else {
			// 没有空值键，直接返回就是了
			return tbl;
		}
	}
	
	private static Object[][] toGroup(ArrayList<ArrayList<Object>> lvalues) {
		int len = lvalues.size();
		if (len < 1) return null;
		ArrayList<Object> o1 = lvalues.get(0);
		int len2 = o1.size();
		if (len2 < 1) return null;
		Object[][] result = new Object[len][len2];
		for (int i = 0; i < len; i++) {
			o1 = lvalues.get(i);
			for (int j = 0; j < len2; j++) {
				result[i][j] = o1.get(j);
			}
		}
		return result;
	}
	
	private static Object[][] toGroup(ArrayList<ArrayList<Object>> lvalues, int keyStart) {
		int len = lvalues.size();
		if (len < 1) return null;
		ArrayList<Object> o1 = lvalues.get(0);
		int len2 = o1.size();
		int len22 = len2;
		for (int i = keyStart; i < len2; i++) {
			Object o = o1.get(i);
			if (o == null) len22--;
		}
		if (len22 < 1) return null;
		Object[][] result = new Object[len][len22];
		for (int i = 0; i < len; i++) {
			o1 = lvalues.get(i);
			int col = 0;
			for (int j = 0; j < len2; j++) {
				Object o = o1.get(j);
				if (o!= null) {
					result[i][col] = o;
					col ++;
				}
			}
		}
		return result;
	}

	/* 共享该方法， xq 2015.4.21 */
	public static Sequence query(String sql, Object[] params, byte[] types, String opt, Context ctx, DBSession dbs) {
		// DBSession dbs = getDbSession();
		// edited by bdl, 2015.7.28, 支持@i选项，单列时返回序列
		return DatabaseUtil.query(sql, params, types, dbs, opt, ctx);
	}

	/* 将针对一个序列execute一个批处理的sql方法共享， xq 2015.4.21 */
	public static void execute(Sequence srcSeries, String sql, Expression[] params, byte[] types, Context ctx,
			DBSession dbs) {// String opt,
		if (srcSeries == null)
			return;
		int paramCount = params == null ? 0 : params.length;
		int len = srcSeries.length();
		Object[][] valueGroup = new Object[len][paramCount];

		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(srcSeries);
		stack.push(current);

		try {
			for (int i = 1; i <= len; ++i) {
				current.setCurrent(i);
				Object[] paramValues = new Object[paramCount];
				valueGroup[i - 1] = paramValues;

				for (int p = 0; p < paramCount; ++p) {
					if (params[p] != null)
						paramValues[p] = params[p].calculate(ctx);
				}
			}
		} finally {
			stack.pop();
		}

		DatabaseUtil.execute2(sql, valueGroup, types, dbs, true);
	}

	/**
	 * 批量执行，根据某个序列，计算参数后，执行某个sql
	 * @param sql	String sql语句
	 * @param params	Expression[] 各字段参数的表达式
	 * @param types	byte[] 参数类型列表，可空，参数类型参见com.scudata.common.Types，可用数组
	 *            	如字符串组等类型。当参数类型空时，认为等同于“默认”类型，此时注意参数值不能为null
	 * @param ctx	Context 上下文，计算上面的参数用
	 * @param dbs	DBSession 数据库信息，记录错误状态用
	 * @param con	Connection 数据库连接对象
	 * @param dbCharset	数据库编码，用于将字符串参数转码
	 * @param tranSQL	是否需要转码，只有为true时处理
	 * @param dbType	数据库类型，对于某些数据库在设定参数时可能需要特殊调整
	 * @param dbn	数据库名称，用于错误提示
	 * @param batchSize	int 批处理阈值，从上层获得
	 */
	private static void execute(Sequence srcSeries, String sql, Expression[] params, byte[] types, Context ctx,
			DBSession dbs, Connection con, String dbCharset, boolean tranSQL, int dbType, String dbn, int batchSize) {// String
																														// opt,
		if (srcSeries == null)
			return;
		int paramCount = params == null ? 0 : params.length;
		int len = srcSeries.length();
		Object[][] valueGroup = new Object[len][paramCount];

		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(srcSeries);
		stack.push(current);

		try {
			for (int i = 1; i <= len; ++i) {
				current.setCurrent(i);
				Object[] paramValues = new Object[paramCount];
				valueGroup[i - 1] = paramValues;

				for (int p = 0; p < paramCount; ++p) {
					if (params[p] != null)
						paramValues[p] = params[p].calculate(ctx);
				}
			}
		} finally {
			stack.pop();
		}

		executeBatch(sql, valueGroup, types, dbs, con, dbCharset, tranSQL, dbType, dbn, batchSize, true);
	}

	/**
	 * 针对一个cursor，批量执行sql，暂时要求一次全部执行，而不是指定fetch 2016.4.19
	 * @param cs	游标
	 * @param sql	sql语句
	 * @param params	使用参数，关于游标记录的表达式
	 * @param types	参数类型，当参数为null时会使用
	 * @param ctx	上下文
	 * @param dbs	数据源设定
	 */
	public static void execute(ICursor cs, String sql, Expression[] params, byte[] types, Context ctx, DBSession dbs) {
		PreparedStatement pst = null;
		Connection con = null;
		String dbCharset = null;
		String toCharset = null;
		boolean tranSQL = false;
		boolean tranContent = true;
		int dbType = DBTypes.UNKNOWN;
		String name = "";
		DBInfo info = dbs.getInfo();
		if (info != null) {
			name = info.getName();
		}
		int batchSize = 1000;

		try {
			DBConfig dsConfig = null;
			MessageManager mm = DataSetMessage.get();
			if (dbs != null && dbs.getInfo() instanceof DBConfig) {
				dsConfig = (DBConfig) dbs.getInfo();
			}
			if (dbs != null) {
				Object session = dbs.getSession();
				if (session instanceof Connection) {
					con = (Connection) session;
				}
			}

			if (con == null || con.isClosed()) {
				throw new RQException(mm.getMessage("error.conClosed", name));
			}

			if (dsConfig != null) {
				batchSize = dsConfig.getBatchSize();
				if (batchSize < 1) {
					batchSize = 1;
				}
				dbCharset = dsConfig.getDBCharset();
				tranSQL = dsConfig.getNeedTranSentence();
				tranContent = dsConfig.getNeedTranContent();
				if ((tranContent || tranSQL) && dbCharset == null) {
					throw new RQException(mm.getMessage("error.fromCharset", name));
				}

				toCharset = dsConfig.getClientCharset();
				if ((tranContent || tranSQL) && toCharset == null) {
					throw new RQException(mm.getMessage("error.toCharset", name));
				}

				dbType = dsConfig.getDBType();
			} else {
				tranContent = false;
			}

			if (tranSQL) {
				sql = new String(sql.getBytes(), dbCharset);
			}

			try {
				pst = con.prepareStatement(sql);
			} catch (SQLException e) {
				if (dbs.getErrorMode()) {
					dbs.setError(e);
				} else {
					throw new RQException(mm.getMessage("error.sqlException", name, sql) + " : " + e.getMessage(), e);
				}
			}
			while (true) {
				Sequence fetchSeq = cs.fetch(batchSize);
				if (fetchSeq == null || fetchSeq.length() == 0)
					break;
				DatabaseUtil.executePst(fetchSeq, pst, params, types, ctx, dbs, dbCharset, tranSQL, dbType, name);
			}
		} catch (RQException re) {
			throw re;
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				if (pst != null) {
					pst.close();
				}
			} catch (Exception e) {
				throw new RQException(e.getMessage(), e);
			}
		}
	}

	private static void executePst(Sequence srcSeries, PreparedStatement pst, Expression[] params, byte[] types,
			Context ctx, DBSession dbs, String dbCharset, boolean tranSQL, int dbType, String name) {
		if (srcSeries == null)
			return;
		int paramCount = params == null ? 0 : params.length;
		int len = srcSeries.length();
		Object[][] valueGroup = new Object[len][paramCount];

		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(srcSeries);
		stack.push(current);

		try {
			for (int i = 1; i <= len; ++i) {
				current.setCurrent(i);
				Object[] paramValues = new Object[paramCount];
				valueGroup[i - 1] = paramValues;

				for (int p = 0; p < paramCount; ++p) {
					if (params[p] != null)
						paramValues[p] = params[p].calculate(ctx);
				}
			}
		} finally {
			stack.pop();
		}

		executeBatch(pst, valueGroup, types, dbs, dbCharset, tranSQL, dbType, name, true);
	}
	
	/**
	 * 下述两个autoDB，用于在各处计算dfx时，连接和释放定义好的自动连接数据源
	 * @param ctx
	 * @param startDsNames
	 */
	public static void connectAutoDBs(Context ctx, List<String> startDsNames) {
		try {
			if (startDsNames != null) {
				for (int i = 0; i < startDsNames.size(); i++) {
					String dsName = (String) startDsNames.get(i);
					ISessionFactory isf = Env.getDBSessionFactory(dsName);
					if (isf != null){
						ctx.setDBSession(dsName, isf.getSession());
						Logger.debug(dsName+ " is auto connected.");
					}
				}
			}
		} catch (Throwable x) {
		}
	}

	/**
	 * 自动关闭连接
	 * @param ctx
	 */
	public static void closeAutoDBs(Context ctx) {
		if(ctx==null){
			return;
		}
		Map<String, DBSession> map = ctx.getDBSessionMap();
		if (map != null) {
			Iterator<String> iter = map.keySet().iterator();
			while (iter.hasNext()) {
				String name = iter.next().toString();
				DBSession sess = ctx.getDBSession(name);
				if (sess == null || sess.isClosed())
					continue;
				Object o = ctx.getDBSession(name).getSession();
				if (o != null && o instanceof java.sql.Connection) {
					try {
						((java.sql.Connection) o).close();
						Logger.debug(name+ " is auto closed.");
					} catch (Exception e) {
						Logger.warn(e.getMessage(), e);
					}
				}
			}
		}
	}
	
}
