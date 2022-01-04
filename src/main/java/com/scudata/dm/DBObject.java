package com.scudata.dm;

import java.sql.Connection;

import com.scudata.common.DBSession;
import com.scudata.common.ISessionFactory;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.cursor.ICursor;
import com.scudata.expression.Expression;
import com.scudata.resources.EngineMessage;
import com.scudata.util.DatabaseUtil;

/**
 * 数据库函数处理对象
 */
public class DBObject implements IResource {
	private DBSession dbSession; // 数据库连接
	private Context ctx; // 上下文
	private boolean canClose; // 连接是否可以被关闭
	private boolean isLower = false; // 字段名是否转小写

	/**
	 * 构建数据库对象
	 * @param dbSession DBSession 数据库连接
	 */
	public DBObject(DBSession dbSession) {
		this.dbSession = dbSession;
	}

	/**
	 * 创建一数据库连接,使用完后需要调用close关闭
	 * @param dbsf ISessionFactory
	 * @param opt String e：当有异常发生时记录异常信息而不是抛出异常
	 * @throws Exception
	 */
	public DBObject(ISessionFactory dbsf, String opt, Context ctx) throws Exception {
		dbSession = dbsf.getSession();
		this.canClose = true;
		this.ctx = ctx;
		if (ctx != null) ctx.addResource(this);
		
		if (opt != null) {
			if (opt.indexOf('e') != -1) dbSession.setErrorMode(true);
			if (opt.indexOf('l') != -1) isLower = true;
			dbSession.isolate(opt);
		}
	}

	// 只有用工厂创建的DBObject才能调用close
	public boolean canClose() {
		return canClose;
	}
	
	/**
	 * 关闭由connect创建的连接
	 */
	public void close() {
		if (!canClose) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.dbCloseError"));
		}
		
		DBSession dbSession = getDbSession();
		if (!dbSession.isClosed()) {
			if (ctx != null) ctx.removeResource(this);
			if (!dbSession.getAutoCommit()) {
				// 有的数据库调用rollback时会抛出异常
				try {
					rollback(null); // 不调此函数close会抛出异常
				} catch (Exception e) {
				}
			}
			
			dbSession.close();
		}
	}
	
	/**
	 * 返回最近的错误信息，并重设错误
	 * @param opt String m：返回错误字符串，默认返回errorCode
	 * @return Object
	 */
	public Object error(String opt) {
		DBSession session = getDbSession();
		java.sql.SQLException e = session.error();
		session.setError(null); // 取出错误后重设错误

		if (e == null) {
			if (opt == null || opt.indexOf('m') == -1) {
				return new Integer(0);
			} else {
				return null;
			}
		} else {
			if (opt == null || opt.indexOf('m') == -1) {
				return new Integer(e.getErrorCode());
			} else {
				String str = e.getMessage();
				if (str == null) {
					str = "SQLException error code：" + e.getErrorCode();
				}

				return str;
			}
		}
	}

	/**
	 * 更新提交到数据库
	 */
	public void commit() {
		try {
			Connection con = (Connection)dbSession.getSession();
			con.commit();
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		}
	}

	/**
	 * 取消上次提交后所作的更新
	 * @param name String 空回滚所有
	 */
	public boolean rollback(String name) {
		try {
			return dbSession.rollback(name);
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		}
	}

	public String isolate(String opt) {
		try {
			return dbSession.isolate(opt);
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		}
	}
	
	// 字段名是否使用小写
	public boolean isLower() {
		return isLower;
	}
	
	public boolean savepoint(String name) {
		try {
			return dbSession.savepoint(name);
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		}
	}
	
	/**
	 * 返回数据库链接，使用完后调用releaseDBSession释放
	 * @return DBSession
	 */
	public DBSession getDbSession() {
		return dbSession;
	}

	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof DBObject)) return false;

		DBObject other = (DBObject)obj;
		return dbSession == other.dbSession;
	}

	/**
	 * 执行查询语句
	 * @param sql String 查询语句
	 * @param params Object[] 参数值
	 * @param types byte[] 参数类型
	 * @param opt String i：结果集只有1列时返回成序列
	 * @param ctx Context
	 * @return Sequence
	 */
	public Sequence query(String sql, Object []params, byte []types, String opt, Context ctx) {
		if (isLower) {
			if (opt == null) {
				opt = "l";
			} else {
				opt += "l";
			}
		}
		
		Sequence result = DatabaseUtil.query(sql,params,types,opt,ctx,getDbSession());
		
		if (opt != null && opt.indexOf('x') != -1 && canClose()) {
			close();
		}
		
		return result;
	}
	
	/**
	 * 针对序列的每一个元素执行查询语句，返回结果集合并的序表
	 * @param srcSeries Sequence 源序列
	 * @param sql String 查询语句
	 * @param params Expression[] 参数表达式
	 * @param types byte[] 参数类型
	 * @param opt String
	 * @param ctx Context
	 * @return Sequence
	 */
	public Sequence query(Sequence srcSeries, String sql, Expression[] params,
			   byte[] types, String opt, Context ctx) {
		if (isLower) {
			if (opt == null) {
				opt = "l";
			} else {
				opt += "l";
			}
		}
		
		Sequence result = DatabaseUtil.query(srcSeries,sql,params,types,opt,ctx,getDbSession());
		
		if (opt != null && opt.indexOf('x') != -1 && canClose()) {
			close();
		}
		
		return result;
	}


	/**
	 * 执行查询语句，返回满足条件的第一条记录的字段或多字段构成的序列。
	 * @param sql String 查询语句
	 * @param params Object[] 参数
	 * @param types byte[] 参数类型
	 * @param opt String
	 * @return Object
	 */
	public Object query1(String sql, Object []params, byte []types, String opt) {
		if (isLower) {
			if (opt == null) {
				opt = "l";
			} else {
				opt += "l";
			}
		}

		DBSession dbs = getDbSession();
		Sequence sequence = DatabaseUtil.query(sql, params, types, dbs, opt);
		
		if (opt != null && opt.indexOf('x') != -1 && canClose()) {
			close();
		}

		if (sequence == null || sequence.length() == 0) return null;

		Object obj = sequence.get(1);
		if (obj instanceof Record) {
			Record r = (Record)obj;
			Object []vals = r.getFieldValues();
			if (vals.length == 1) {
				return vals[0];
			} else {
				return new Sequence(vals);
			}
		} else {
			return obj;
		}
	}

	/**
	 * 执行存储过程返回结果序列，如果返回多个数据集，则返回序列的序列
	 * @param sql String sql语句
	 * @param params Object[] 参数值
	 * @param types byte[] 参数类型
	 * @param modes byte[] 输入输出模式
	 * @param outParams String[] 输出参数所到变量的名称
	 * @param ctx Context
	 * @return Sequence
	 */
	public Sequence proc(String sql, Object[] params, byte[] types, byte[] modes,
					   String[] outParams, Context ctx) {
		DBSession dbs = getDbSession();
		Sequence series = DatabaseUtil.proc(sql, params, modes, types, outParams, dbs, ctx);
		return series == null ? new Sequence(0) : series;
	}

	/**
	 * 针对数据库执行sql语句
	 * @param sql String
	 * @param params Object[] 参数值
	 * @param types byte[] 参数类型
	 * @param opt String k: 不提交事务，缺省将提交
	 * @return Object
	 */
	public Object execute(String sql, Object []params, byte []types, String opt) {
		if (isLower) {
			if (opt == null) {
				opt = "l";
			} else {
				opt += "l";
			}
		}

		DBSession dbs = getDbSession();
		Object ret = DatabaseUtil.execute(sql, params, types, dbs, opt);
		if (opt == null || opt.indexOf('k') == -1) commit();
		return ret;
	}

	/**
	 * 针对序列的每一个元素执行sql语句
	 * @param srcSeries Sequence 源序列
	 * @param sql String
	 * @param params Expression[] 参数表达式
	 * @param types byte[] 参数类型
	 * @param opt String k: 不提交事务，缺省将提交
	 * @param ctx Context
	 */
	public void execute(Sequence srcSeries, String sql, Expression[] params,
			byte[] types, String opt, Context ctx) {
		DBSession dbs = getDbSession();
		DatabaseUtil.execute(srcSeries,sql,params,types,ctx,dbs);//opt,
		if (opt == null || opt.indexOf('k') == -1) commit();
	}

	public void execute(ICursor cursor, String sql, Expression[] params,
			byte[] types, String opt, Context ctx) {
		DBSession dbs = getDbSession();
		DatabaseUtil.execute(cursor,sql,params,types,ctx,dbs);//opt,
		if (opt == null || opt.indexOf('k') == -1) commit();
	}

	/**
	 * 根据srcSeries更新表table中的字段fields
	 * @param srcSeries Sequence 源排列
	 * @param table String 表名
	 * @param fields String[] 字段名
	 * @param fopts String[] p：字段是主键，a：字段是自增字段
	 * @param exps Expression[] 值表达式
	 * @param opt String
	 * @param ctx Context
	 * @return int 更新记录数
	 */
	public int update(Sequence srcSeries, String table, String[] fields,
					   String[] fopts, Expression[] exps, String opt, Context ctx) {
		if (srcSeries == null || srcSeries.length() == 0) return 0;

		DBSession dbs = getDbSession();
		int count = DatabaseUtil.update(srcSeries, table, fields, fopts, exps, opt, dbs, ctx);

		if (opt == null || opt.indexOf('k') == -1) {
			commit();
		}

		return count;
	}

	public int update(ICursor cursor, String table, String[] fields,
					   String[] fopts, Expression[] exps, String opt, Context ctx) {
		DBSession dbs = getDbSession();
		int count = DatabaseUtil.update(cursor, table, fields, fopts, exps, opt, dbs, ctx);

		if (opt == null || opt.indexOf('k') == -1) {
			commit();
		}

		return count;
	}
	
	public int update(Sequence seq1, Sequence seq2, String table, String[] fields,
					   String[] fopts, Expression[] exps, String opt, Context ctx) {
		DBSession dbs = getDbSession();
		int count = DatabaseUtil.update(seq1, seq2, table, fields, fopts, exps, opt, dbs, ctx);

		if (opt == null || opt.indexOf('k') == -1) {
			commit();
		}

		return count;
	}
}
