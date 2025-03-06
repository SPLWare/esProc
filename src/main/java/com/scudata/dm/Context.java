package com.scudata.dm;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.common.DBSession;
import com.scudata.common.ISessionFactory;
import com.scudata.expression.DfxFunction;

/**
 * 计算用到的上下文
 * @author WangXiaoJun
 *
 */
public class Context {
	private Context parent; // 父上下文
	private JobSpace js; // 作业空间
	private Map<String, DBSession> dbSessions = new HashMap<String, DBSession>();// DBSession映射
	private Map<String, ISessionFactory> dbsfs = new HashMap<String, ISessionFactory>(); // ISessionFactory映射

	private ParamList paramList = new ParamList(); // 变量列表
	private ComputeStack computeStack = new ComputeStack(); // 计算堆栈
	private String defDsName; // 缺省数据库链接名称

	private Random random; // 用于生成随机值
	private Param iterateParam = new Param(KeyWord.ITERATEPARAM, Param.VAR, null); // 迭代变量
	
	/**
	 * 创建上下文对象
	 */
	public Context() {
	}

	/**
	 * 创建上下文对象, 和父上下文共享语义层
	 * @param parent 父上下文
	 */
	public Context(Context parent) {
		this.parent = parent;
	}

	/**
	 * 设置父上下文
	 * @param parent Context
	 */
	public void setParent(Context parent) {
		this.parent = parent;
	}

	/**
	 * 取得父对象上下文
	 * @return  父上下文
	 */
	public Context getParent() {
		return parent;
	}

	/**
	 * 按名称取数据库或OLAP配置
	 * @param dbName 数据库或OLAP名
	 * @return DataSourceConfig
	 */
	public DBSession getDBSession(String dbName) {
		DBSession ds = dbSessions.get(dbName);
		if (ds != null) return ds;

		if (parent != null) {
			return parent.getDBSession(dbName);
		} else {
			return null;
		}
	}

	/**
	 * 返回所有数据库连接
	 * @return Map name：DBSession
	 */
	public Map<String, DBSession> getDBSessionMap() {
		return dbSessions;
	}

	/**
	 * 按名称设数据库或OLAP配置
	 * @param dbName 数据源名
	 * @param dbSession 数据源配置
	 */
	public void setDBSession(String dbName, DBSession dbSession) {
		dbSessions.put(dbName, dbSession);
	}

	/**
	 * 删除数据库连接
	 * @param dbName String 数据库连接名
	 * @return DBSession
	 */
	public DBSession removeDBSession(String dbName) {
		DBSession ds = dbSessions.remove(dbName);
		if (ds != null) return ds;
		return parent == null ? null : parent.removeDBSession(dbName);
	}

	/**
	 * 获取数据库连接工厂
	 * @param name String 数据库名称
	 * @return ISessionFactory 数据库连接工厂
	 */
	public ISessionFactory getDBSessionFactory(String name) {
		ISessionFactory sf = dbsfs.get(name);
		if (sf != null) return sf;
		return parent == null ? null : parent.getDBSessionFactory(name);
	 }

	 /**
	  * 返回所有数据库连接工厂
	  * @return Map name：ISessionFactory
	  */
	 public Map<String, ISessionFactory> getDBSessionFactoryMap() {
		 return dbsfs;
	 }

	 /**
	  * 设置数据库连接工厂
	  * @param name String 数据库名称
	  * @param sf ISessionFactory 数据库连接工厂
	  */
	 public void setDBSessionFactory(String name, ISessionFactory sf) {
		 dbsfs.put(name, sf);
	 }

	 /**
	  * 删除数据库连接工厂
	  * @param name String
	  * @return ISessionFactory
	  */
	 public ISessionFactory removeDBSessionFactory(String name) {
		 ISessionFactory sf = dbsfs.remove(name);
		 if (sf != null) return sf;
		 return parent == null ? null : parent.removeDBSessionFactory(name);
	 }

	/**
	 * 按名称取变量
	 * @param name 变量名
	 * @return DataStruct
	 */
	public Param getParam(String name) {
		// 不再从父上下文取
		// 多路游标有自己的上下文，创建多路游标后主dfx再创建变量多路游标里访问不到了
		Param param = paramList.get(name);
		if (param != null) return param;
		return parent == null ? null : parent.getParam(name);
	}

	/**
	 * 返回参数列表
	 * @return ParamList
	 */
	public ParamList getParamList() {
		return paramList;
	}

	/**
	 * 设置参数列表
	 * @param list ParamList
	 */
	public void setParamList(ParamList list) {
		if (list == null) {
			this.paramList = new ParamList();
		} else {
			this.paramList = list;
		}
	}

	/**
	 * 添加变量
	 * @param param 变量
	 */
	public void addParam(Param param) {
		paramList.add(param);
	}

	/**
	 * 按名称删除变量
	 * @param name String
	 * @return Param
	 */
	public Param removeParam(String name) {
		return paramList.remove(name);
		// 不再从父上下文删
		//if (param != null) return param;
		//return parent == null ? null : parent.removeParam(name);
	}

	/**
	 * 设置参数的值，如果变量不存在则产生一个VAR类型的参数
	 * @param name String
	 * @param value Object
	 */
	public void setParamValue(String name, Object value) {
		Param p = getParam(name);
		if (p == null) {
			addParam(new Param(name, Param.VAR, value));
		} else {
			p.setValue(value);
		}
	}

	/**
	 * 设置参数的值，如果变量不存在则产生一个paramType类型的参数
	 * @param name String
	 * @param value Object
	 * @param paramType byte Param.VAR  ARG
	 */
	public void setParamValue(String name, Object value, byte paramType) {
		Param p = getParam(name);
		if (p == null) {
			addParam(new Param(name, paramType, value));
		} else {
			p.setValue(value);
		}
	}

	/**
	 * 取计算堆栈，用于成员函数左侧对象的压栈
	 * @return
	 */
	public ComputeStack getComputeStack() {
		return computeStack;
	}

	/**
	 * 返回缺省数据库链接名称
	 * @return String
	 */
	public String getDefDBsessionName() {
		return this.defDsName;
	}

	/**
	 * 设置缺省数据库链接名称
	 * @param dsn String
	 */
	public void setDefDBsessionName(String dsn) {
		this.defDsName = dsn;
	}

	/**
	 * 返回缺省数据库链接
	 * @return DBSession
	 */
	public DBSession getDefDBsession() {
		return getDBSession(defDsName);
	}

	// 返回一个已连接的数据库
	public DBSession getDBSession() {
		if (dbSessions.size() != 0) {
			return dbSessions.values().iterator().next();
		}

		if (parent != null) {
			return parent.getDBSession();
		} else {
			return null;
		}
	}

	/**
	 * 返回random对象
	 * @return Random
	 */
	public Random getRandom() {
		if (random == null) random = new Random();
		return random;
	}

	/**
	 * 返回根为seed的random对象
	 * @param seed long 根
	 * @return Random
	 */
	public Random getRandom(long seed) {
		if (random == null) {
			random = new Random(seed);
		} else {
			random.setSeed(seed);
		}

		return random;
	}

	/**
	 * 设置工作空间
	 * @param space 工作空间
	 */
	public void setJobSpace(JobSpace space) {
		this.js = space;
	}

	/**
	 * 取工作空间
	 * @return JobSpace
	 */
	public JobSpace getJobSpace() {
		return js;
	}

	/**
	 * 取资源管理器
	 * @return ResourceManager
	 */
	public ResourceManager getResourceManager() {
		if (js != null) {
			return js.getResourceManager();
		} else {
			return null;
		}
	}
	
	/**
	 * 添加指定资源到资源管理器
	 * @param resource 资源
	 */
	public void addResource(IResource resource) {
		ResourceManager rm = getResourceManager();
		if (rm != null) {
			rm.add(resource);
		}
	}
	
	/**
	 * 把指定资源从资源管理器中删除
	 * @param resource 资源
	 */
	public void removeResource(IResource resource) {
		ResourceManager rm = getResourceManager();
		if (rm != null) {
			rm.remove(resource);
		}
	}
	
	/**
	 * 创建新的计算环境
	 * @return Context
	 */
	public Context newComputeContext() {
		// 不再指定this为父，func调用层数多了容易导致StackOverflowError异常
		Context ctx = new Context();
		ctx.js = js;
		ctx.dbSessions = dbSessions;
		ctx.dbsfs = dbsfs;
		ctx.defDsName = defDsName;

		ParamList paramList = this.paramList;
		ParamList paramList2 = ctx.paramList;
		for (int i = 0, count = paramList.count(); i < count; ++i) {
			Param p = paramList.get(i);
			paramList2.add(new Param(p));
		}

		return ctx;
	}
	
	/**
	 * 复制指定上线文的计算环境，不复制变量
	 * @param ctx 计算上下文
	 */
	public void setEnv(Context ctx) {
		js = ctx.js;
		dbSessions = ctx.dbSessions;
		dbsfs = ctx.dbsfs;
		defDsName = ctx.defDsName;
	}

	/**
	 * 取迭代变量
	 * @return Param
	 */
	public Param getIterateParam() {
		return iterateParam;
	}
	
	/**
	 * 添加程序网函数
	 * @param fnName 函数名
	 * @param dfxPathName 程序网路径名
	 */
	public void addDFXFunction(String fnName, String dfxPathName, String opt) {
		js.addDFXFunction(fnName, dfxPathName, opt);
	}
	
	/**
	 * 添加程序网函数
	 * @param fnName 函数名
	 * @param funcInfo 函数体信息
	 */
	public void addDFXFunction(String fnName, PgmCellSet.FuncInfo funcInfo) {
		js.addDFXFunction(fnName, funcInfo);
	}
	
	/**
	 * 删除程序网函数
	 * @param fnName 函数名
	 */
	public void removeDFXFunction(String fnName) {
		js.removeDFXFunction(fnName);
	}
	
	/**
	 * 根据函数名取程序网
	 * @param fnName 函数名
	 * @return 程序网函数
	 */
	public DfxFunction getDFXFunction(String fnName) {
		if (js != null) {
			return js.getDFXFunction(fnName);
		} else {
			return null;
		}
	}
}
