package com.scudata.dm.cursor;

import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;

import com.scudata.common.DBConfig;
import com.scudata.common.DBInfo;
import com.scudata.common.DBSession;
import com.scudata.common.DBTypes;
import com.scudata.common.Logger;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.common.SQLTool;
import com.scudata.common.Sentence;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.Context;
import com.scudata.dm.DBObject;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.resources.DataSetMessage;
import com.scudata.util.DatabaseUtil;

/**
 * 用于从数据库取数的游标
 * @author RunQian
 *
 */
public class DBCursor extends ICursor {
	private String sql; // SQL语句
	private Object[] params; // 参数
	private byte[] types; // 参数类型
	
	private DBObject db; // 含有数据库连接的对象
	private ResultSet rs = null;
	private PreparedStatement pst = null;
	private Connection con = null;
	private ResultSetMetaData rsmd;

	private String dbCharset = null;
	private String toCharset = null;
	private boolean tranSQL = false;
	private boolean tranContent = true;
	private int dbType = DBTypes.UNKNOWN;
	private boolean bb = true;

	private boolean isAccessBug = false;
	private boolean isSingleField; // 是否返回单列组成的序列

	//private Table cursorTable; 改用ICursor.dataStruct表示

	private String opt;//增加查询数据，选项@d
	public DBCursor(String sql, Object[] params, byte[] types, DBObject db, String opt, Context ctx) {
		this.sql = sql;
		this.params = params;
		this.types = types;
		this.db = db;
		this.ctx = ctx;
		
		if (db.isLower()) {
			if (opt == null) {
				this.opt = "l";
			} else {
				this.opt = opt + "l";
			}
		} else {
			this.opt = opt;
		}
		
		init(sql, params, types, db.getDbSession(), opt);
		if (ctx != null) {
			ctx.addResource(this);
		}
	}

	/**
	 * 跳过指定条数的数据
	 * @param n 数量
	 * @return long 实际跳过的条数
	 */
	protected long skipOver(long n) {
		if (rs == null || n == 0)
			return 0;

		long count = n;
		try {
			while (n > 0 && (isAccessBug || !rs.isLast()) && rs.next()) {
				n--;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		if (n > 0) {
			return count - n;
		} else {
			return count;
		}
	}

	/**
	 * 关闭游标
	 */
	public synchronized void close() {
		super.close();
		
		if (db != null) {
			try {
				if (ctx != null) {
					ctx.removeResource(this);
				}
				
				if (rs != null) {
					rs.close();
				}
				
				if (pst != null) {
					pst.close();
				}
				
				if (opt != null && opt.indexOf('x') != -1 && db.canClose()) {
					db.close();
					db = null;
				}
			} catch (Exception e) {
				throw new RQException(e.getMessage(), e);
			} finally {
				rs = null;
				pst = null;
				con = null;
				rsmd = null;
				dataStruct = null;
			}
		}
	}

	/**
	 * 读取指定条数的记录返回
	 * @param n 数量
	 * @return Sequence
	 */
	protected Sequence get(int n) {
		if (rs == null || n < 1)
			return null;

		Table table;
		if (n > INITSIZE) {
			table = new Table(dataStruct, INITSIZE);
		} else {
			table = new Table(dataStruct, n);
		}

		try {
			int colCount = rsmd.getColumnCount();
			while (n > 0 && (isAccessBug || !rs.isLast()) && rs.next()
					&& (isAccessBug || !rs.isAfterLast())) {
				n--;
				this.get(table, colCount);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (table.length() > 0) {
			if (isSingleField) {
				return table.fieldValues(0);
			} else {
				//table.trimToSize();
				return table;
			}
		} else {
			return null;
		}
	}

	/**
	 * 对指定的数据库连接执行sql语句，返回结果构成的序列
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
	 */
	private void init(String sql, Object[] params, byte[] types, DBSession dbs, String opt) {
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
				if (info != null)
					name = info.getName();
				throw new RQException(mm.getMessage("error.conClosed", name));
			}

			if (dsConfig != null) {
				dbCharset = dsConfig.getDBCharset();
				tranSQL = dsConfig.getNeedTranSentence();
				tranContent = dsConfig.getNeedTranContent();
				if ((tranContent || tranSQL) && dbCharset == null) {
					String name = "";
					DBInfo info = dbs.getInfo();
					if (info != null)
						name = info.getName();
					throw new RQException(mm.getMessage("error.fromCharset",
							name));
				}

				toCharset = dsConfig.getClientCharset();
				if ((tranContent || tranSQL) && toCharset == null) {
					String name = "";
					DBInfo info = dbs.getInfo();
					if (info != null)
						name = info.getName();
					throw new RQException(
							mm.getMessage("error.toCharset", name));
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
					pos = Sentence.indexOf(sql, "?", pos + 1,
							Sentence.IGNORE_PARS + Sentence.IGNORE_QUOTE);
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
								o = new String(((String) o).getBytes(),
										dbCharset);
								l.set(i, o);
							}
						}
					} else if (args[paramIndex] instanceof String && tranSQL) {
						args[paramIndex] = new String(
								((String) args[paramIndex]).getBytes(),
								dbCharset);
					}
					if (args[paramIndex] instanceof Sequence) {
						Object[] objs = ((Sequence) args[paramIndex]).toArray();
						int objCount = objs.length;
						StringBuffer sb = new StringBuffer(2 * objCount);
						for (int iObj = 0; iObj < objCount; iObj++) {
							sb.append("?,");
						}
						if (sb.length() > 0
								&& sb.charAt(sb.length() - 1) == ',') {
							sb.deleteCharAt(sb.length() - 1);
						}
						if (sb.length() > 1) {
							sql = sql.substring(0, pos) + sb.toString()
									+ sql.substring(pos + 1);
						}
						pos = pos + sb.length();
					}
				}
			}

			try {
				// pst = con.prepareStatement(sql);
				// edited by bdl, 2010.7.28, 处理sqlserver中数据太多的情况
				// 另外在opt中添加z选项，如果出现z表示支持反向，否则不支持
				int rsType = ResultSet.TYPE_FORWARD_ONLY;
				if (opt != null && opt.indexOf("z") > -1)
					rsType = ResultSet.TYPE_SCROLL_INSENSITIVE;
				// added by bdl, 2010.11.22, 对于access数据库，把rsType设定为insensitive
				// else if
				// (con.getClass().getName().equals("sun.jdbc.odbc.JdbcOdbcConnection"))
				// {
				// rsType = ResultSet.TYPE_SCROLL_INSENSITIVE;
				// }
				// eidtd by bdl, 2010.12.7, 对于forwordonly类型的指针，均不判断islast
				if (rsType == ResultSet.TYPE_FORWARD_ONLY) // && dbType ==
															// DBTypes.ACCESS )
					this.isAccessBug = true;
				if (con.getClass()
						.getName()
						.equals("com.microsoft.sqlserver.jdbc.SQLServerConnection")) {
					Class clss = Class
							.forName("com.microsoft.sqlserver.jdbc.SQLServerResultSet");
					java.lang.reflect.Field fld = clss
							.getField("TYPE_SS_SERVER_CURSOR_FORWARD_ONLY");
					if (opt != null && opt.indexOf("z") > -1)
						fld = clss.getField("TYPE_SS_SCROLL_KEYSET");
					rsType = fld.getInt(null);
				}
				//added by bd, 2016.9.28, 当遇到POSTGRESQL时，将autocommit设为false
				//以防止这个JDBC将结果集全部返回
				DBInfo info = dbs.getInfo();
				if (info != null && info.getDBType() == DBTypes.POSTGRES) {
					Logger.info("Setting POSTGRESQL...");
					con.setAutoCommit(false);
					pst = con.prepareStatement(sql, rsType,
							ResultSet.TYPE_FORWARD_ONLY);
					pst.setFetchSize(1000);
				}
				else if (info != null && info.getDBType() == DBTypes.DBONE) {
					Logger.info("Setting DBONE...");
					con.setAutoCommit(false);
					pst = con.prepareStatement(sql, rsType,
							ResultSet.TYPE_FORWARD_ONLY);
					pst.setFetchSize(1000);
				}
				//edited by bd, 2016.12.15, 对于sqlserver，也设置fetchsize，以防其返回所有结果
				else if (info != null && info.getDBType() == DBTypes.SQLSVR) {
					con.setAutoCommit(false);
					pst = con.prepareStatement(sql, rsType,
							ResultSet.CONCUR_READ_ONLY);
					//edited by bd, 2016.12.19, 防止出现“不支持并发”错误
							//ResultSet.TYPE_FORWARD_ONLY);
					pst.setFetchSize(1000);
				}
				//edited by bd, 2017.9.7, Mysql执行大数据查询时，URL应该这样写：
				// jdbc:mysql://127.0.0.1:3306/mysql?useCursorFetch=true
				// 注意其中的useCursorFetch=true，可以使用流式检索
				else if (info != null && info.getDBType() == DBTypes.MYSQL) {
					con.setAutoCommit(false);
					//added by bd, 2019.1.9, mysql数据库中，使用游标时，整数类型的数据莫名其妙地读不出来
					//专门针对这个特殊处理
//					if (dbType == DBTypes.MYSQL) {
						pst = con.prepareStatement(sql);						
//					}
//					else {
//						pst = con.prepareStatement(sql, rsType,
//								ResultSet.TYPE_FORWARD_ONLY);
//					}
					pst.setFetchSize(1000);
					//在这里设定fetchSize，而不是用Integer.MIN_VALUE,这种是row by row的
				}
				else {
					pst = con.prepareStatement(sql, rsType,
						ResultSet.CONCUR_READ_ONLY);
				}
				// added by bdl, 2015.4.14
				// for mysql, the statement will return all the records default,
				// which will cause the oom error when deal with big data.
				// comment added by bd, 2016.10.21:
				// Mysql执行大数据查询时，URL应该这样写：
				// jdbc:mysql://127.0.0.1:3306/mysql?useCursorFetch=true
				// 注意其中的useCursorFetch=true，可以使得查询正常
				//if (con.getClass().getName().indexOf("com.mysql")>=0) {
					//pst.setFetchSize(Integer.MIN_VALUE);
					// pst.setFetchDirection(ResultSet.FETCH_REVERSE);
				//}
			} catch (SQLException e) {
				Logger.debug(e.getMessage());
				String name = "";
				DBInfo info = dbs.getInfo();
				if (info != null)
					name = info.getName();
				throw new RQException(mm.getMessage("error.sqlException", name,
						sql), e);
			}

			if (args != null && args.length > 0) {
				int pos = 0;
				for (int iArg = 0; iArg < args.length; iArg++) {
					pos++;
					try {
						byte type = argTypes[iArg];
						if (args[iArg] != null
								&& args[iArg] instanceof Sequence) {
							//String info = "args:" + iArg + ":";
							Object[] objs = ((Sequence) args[iArg]).toArray();
							for (int iObj = 0; iObj < objs.length; iObj++) {
								//info += objs[iObj];
								//info += ",";
								SQLTool.setObject(dbType, pst, pos, objs[iObj],
										type);
								pos++;
							}
							// Logger.debug(info);
							pos--;
						} else {
							// Logger.debug("arg" + iArg + ": " + args[iArg]);
							SQLTool.setObject(dbType, pst, pos, args[iArg],
									type);
						}
					} catch (Exception e) {
						String name = "";
						DBInfo info = dbs.getInfo();
						if (info != null)
							name = info.getName();
						throw new RQException(mm.getMessage("error.argIndex",
								name, Integer.toString(iArg + 1)));
					}
				}
			}

			try {
				rs = pst.executeQuery();
			} catch (SQLException e) {
				Logger.debug(e.getMessage());
				String name = "";
				DBInfo info = dbs.getInfo();
				if (info != null)
					name = info.getName();
				throw new RQException(mm.getMessage("error.sqlException", name,
						sql));
			}
			// edited by bdl, 2010.4.29, 如果有@t操作，返回库序表
			DataStruct ds = crtateDataStruct(rs, db.isLower());
			if (opt != null && opt.indexOf("t") > -1) {
				String[] fields = null;
				int colCount = rsmd.getColumnCount();
				String tableName = rsmd.getTableName(1);
				if (sql.indexOf(" as ") < 0) {
					fields = new String[colCount];
					String[][] tableCols = new String[colCount][];
					for (int c = 1; c <= colCount; ++c) {
						//edited by bd, 2016.8.29, 不应该用Name，而是Lable，否则别名设不进去
						//fields[c - 1] = rsmd.getColumnName(c);
						//String[] tCol = { rsmd.getColumnName(c) };
						fields[c - 1] = rsmd.getColumnLabel(c);
						String[] tCol = { rsmd.getColumnLabel(c) };
						tableCols[c - 1] = tCol;
					}
					// removed by bdl, 2010.12.29, tableInfo removed
					// table.setTableInfo(tableName, fields, tableCols, null);
				} else {
					String selCols = sql.substring(sql.indexOf("select") + 6,
							sql.indexOf("from")).trim();
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
						// removed by bdl, 2010.12.29, tableInfo removed
						// table.setTableInfo(tableName, fields, tableCols,
						// null);
					}
				}
				// added by bdl, 2010.8.16, 遇到t选项时，设定表信息的同时，
				// 自动设定主键，以备更新调用
				if (opt != null && opt.indexOf("u") > -1) {
					DatabaseMetaData dmd = con.getMetaData();
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
							pks[i] = nameList.get(i);
						}
						
						ds.setPrimary(pks);
					}
				}
			}

			if (opt != null && opt.indexOf('i') != -1
					&& ds.getFieldCount() == 1) {
				isSingleField = true;
			}

			//this.cursorTable = table;
			// edited end 2010.4.29
		} catch (RQException re) {
			throw re;
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		}
	}

	/**
	 * 取出结果数据集rs的每一行数据生成记录，构成Table返回
	 * 
	 * @param rs
	 *            ResultSet 结果数据集
	 * @return DataStruct
	 */
	private DataStruct crtateDataStruct(ResultSet rs, boolean lowercase) 
			throws SQLException, UnsupportedEncodingException {
		if (rs == null) {
			return null;
		}

		this.rsmd = rs.getMetaData();
		int colCount = rsmd.getColumnCount();
		int[] colTypes = new int[colCount];
		String[] colNames = new String[colCount];

		for (int c = 1; c <= colCount; ++c) {
			//edited by bd, 2016.8.29, 不应该用Name，而是Lable，否则别名设不进去
			//colNames[c - 1] = rsmd.getColumnName(c);
			//edited by bd, 2019.7.19, 添加了游标支持connect@l，允许返回字段名为小写
			if (lowercase) {
				colNames[c - 1] = rsmd.getColumnLabel(c).toLowerCase();
			}
			else {
				colNames[c - 1] = rsmd.getColumnLabel(c);
			}
			
			colTypes[c - 1] = rsmd.getColumnType(c);
		}

		if (tranContent
				&& (toCharset == null || toCharset.trim().length() == 0)) {
			MessageManager mm = DataSetMessage.get();
			throw new RQException(mm.getMessage("error.toCharset"));
		}

		if (toCharset != null) {
			bb = toCharset.equalsIgnoreCase(dbCharset) || dbCharset == null;
		}

		DataStruct ds = new DataStruct(colNames);
		setDataStruct(ds);
		return ds;
	}

	private void get(Table table, int colCount) throws SQLException,
			UnsupportedEncodingException {
		BaseRecord record = table.newLast();
		for (int n = 1; n <= colCount; ++n) {
			int type = 0;
			if (dbType == DBTypes.ORACLE) {
				type = rsmd.getColumnType(n);
			}
			try {
				Object obj = DatabaseUtil.tranData(type, dbType, rs, n,
						tranContent, dbCharset, toCharset, bb,opt);//调用增加opt选项，实现@d  xq 2015.4.22
				record.set(n - 1, obj);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	protected void finalize() throws Throwable {
		close();
	}
	
	/**
	 * 重置游标
	 * @return 返回是否成功，true：游标可以从头重新取数，false：不可以从头重新取数
	 */
	public boolean reset() {
		close();
		
		if (db != null) {
			init(sql, params, types, db.getDbSession(), opt);
			if (ctx != null) {
				ctx.addResource(this);
			}
			
			return true;
		} else {
			return false;
		}
	}
}
