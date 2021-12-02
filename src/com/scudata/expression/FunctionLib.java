package com.scudata.expression;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.scudata.common.Logger;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.operator.DotOperator;
import com.scudata.resources.EngineMessage;

/**
 * 函数库，用于系统函数的加载和查找
 * @author RunQian
 *
 */
public final class FunctionLib {
	// 用于链接同名成员函数的类对象
	private static class ClassLink {
		Class<? extends MemberFunction> fnClass; // 成员函数类对象
		ClassLink next; // 下一个同名的成员函数类对象
		
		ClassLink(Class<? extends MemberFunction> fnClass) {
			this.fnClass = fnClass;
		}
		
		MemberFunction newInstance(String fnName) throws InstantiationException, IllegalAccessException{
			MemberFunction fn = fnClass.newInstance();
			fn.setFunctionName(fnName);
			if (next != null) {
				MemberFunction nextFn = next.newInstance(fnName);
				fn.setNextFunction(nextFn);
			}
			
			return fn;
		}
	}
	
	// 全局函数映射表
	private static HashMap<String, Class<? extends Function>> fnMap = 
		new HashMap<String, Class<? extends Function>>(256);
	
	// 成员函数映射表
	private static HashMap<String, ClassLink> mfnMap = new HashMap<String, ClassLink>(256);
	
	// 程序网格函数映射表，[函数名,程序网路径名]
	private static HashMap<String, String> dfxFnMap = new HashMap<String, String>(256);
	
	private FunctionLib() {
	}

	static {
		// 自定义函数不再自动加载，提供函数让上层设置
		loadSystemFunctions();
	}

	/**
	 * 添加程序网函数
	 * @param fnName 函数名
	 * @param dfxPathName 程序网路径名
	 */
	public static void addDFXFunction(String fnName, String dfxPathName) {
		// 不能与全局函数重名
		if (fnMap.containsKey(fnName)) {// || dfxFnMap.containsKey(fnName)
			MessageManager mm = EngineMessage.get();
			throw new RuntimeException(mm.getMessage("FunctionLib.repeatedFunction") + fnName);
		}
		
		// 用新函数替换旧的
		dfxFnMap.put(fnName, dfxPathName);
	}
	
	/**
	 * 删除程序网函数
	 * @param fnName 函数名
	 */
	public static void removeDFXFunction(String fnName) {
		dfxFnMap.remove(fnName);
	}
	
	/**
	 * 根据函数名取程序网
	 * @param fnName 函数名
	 * @return 程序网路径名
	 */
	public static String getDFXFunction(String fnName) {
		return dfxFnMap.get(fnName);
	}
	
	/**
	 * 添加全局函数
	 * @param fnName 函数名
	 * @param className 类名（包含包名）
	 */
	public static void addFunction(String fnName, String className) {
		try {
			Class<? extends Function> funClass = (Class<? extends Function>)Class.forName(className);
			if (fnMap.containsKey(fnName)) {
				MessageManager mm = EngineMessage.get();
				throw new RuntimeException(mm.getMessage("FunctionLib.repeatedFunction") + fnName);
			}
			
			fnMap.put(fnName, funClass);
		} catch (Throwable e) {
			throw new RQException(className, e);
		}
	}
	
	/**
	 * 添加全局函数，重名抛异常
	 * @param fnName 函数名
	 * @param funClass 类对象
	 */
	public static void addFunction(String fnName, Class<? extends Function> funClass) {
		if (fnMap.containsKey(fnName)) {
			MessageManager mm = EngineMessage.get();
			throw new RuntimeException(mm.getMessage("FunctionLib.repeatedFunction") + fnName);
		}
		
		fnMap.put(fnName, funClass);
	}
	
	/**
	 * 添加全局函数，重名替换或不做改动
	 * @param fnName 函数名
	 * @param funClass 类对象
	 * @param replace true：重名替换，false：重名不做改动
	 */
	public static void addFunction(String fnName, String className, boolean replace) {
		if (replace || !fnMap.containsKey(fnName)) {
			try {
				Class<? extends Function> funClass = (Class<? extends Function>)Class.forName(className);
				fnMap.put(fnName, funClass);
			} catch (Throwable e) {
				throw new RQException(className, e);
			}
		}
	}
	
	/**
	 * 判断标识符是否是全局函数名
	 * @param id 标识符
	 * @return true：是全局函数名，false：不是全局函数名
	 */
	public static boolean isFnName(String id) {
		return fnMap.containsKey(id);
	}

	/**
	 * 由函数名创建全局函数
	 * @param fnName 函数名
	 * @return 函数名对应的全局函数
	 */
	public static Function newFunction(String fnName) {
		try {
			Class<? extends Function> funClass = fnMap.get(fnName);
			Function function = funClass.newInstance();
			function.setFunctionName(fnName);
			return function;
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e.getMessage());
		} catch (InstantiationException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	/**
	 * 添加成员函数
	 * @param fnName 成员函数名
	 * @param className 类名（包含包名）
	 */
	public static void addMemberFunction(String fnName, String className) {
		try {
			Class<? extends MemberFunction> fnClass = (Class<? extends MemberFunction>)Class.forName(className);
			addMemberFunction(fnName, fnClass);
		} catch (Throwable e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	/**
	 * 添加成员函数
	 * @param fnName 成员函数名
	 * @param fnClass 类对象
	 */
	public static void addMemberFunction(String fnName, Class<? extends MemberFunction> fnClass) {
		ClassLink prev = mfnMap.get(fnName);
		ClassLink fnLink = new ClassLink(fnClass);
		if (prev == null) {
			mfnMap.put(fnName, fnLink);
		} else {
			while (prev.next != null) {
				prev = prev.next;
			}
			
			prev.next = fnLink;
		}
	}
	
	/**
	 * 判断标识符是否是成员函数名
	 * @param id 标识符
	 * @return true：是成员函数名，false：不是成员函数名
	 */
	public static boolean isMemberFnName(String id) {
		return mfnMap.containsKey(id);
	}

	/**
	 * 由函数名创建成员函数
	 * @param fnName 函数名
	 * @return 函数名对应的成员函数
	 */
	public static MemberFunction newMemberFunction(String fnName) {
		try {
			ClassLink fnLink = mfnMap.get(fnName);
			return fnLink.newInstance(fnName);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e.getMessage());
		} catch (InstantiationException e) {
			throw new RuntimeException(e.getMessage());
		}
	}
	
	// 加载系统函数
	private static void loadSystemFunctions() {
		// 全局函数
		addFunction("between", "com.scudata.expression.fn.Between");
		addFunction("case", "com.scudata.expression.fn.Case");
		addFunction("cmp", "com.scudata.expression.fn.Compare");
		addFunction("clipboard", "com.scudata.expression.fn.Clipboard");
		addFunction("eval", "com.scudata.expression.fn.Eval");
		addFunction("if", "com.scudata.expression.fn.If");
		addFunction("to", "com.scudata.expression.fn.To");
		addFunction("join", "com.scudata.expression.fn.Join");
		addFunction("xjoin", "com.scudata.expression.fn.XJoin");
		addFunction("create", "com.scudata.expression.fn.Create");
		addFunction("new", "com.scudata.expression.fn.New");
		addFunction("sum", "com.scudata.expression.fn.gather.Sum");
		addFunction("avg", "com.scudata.expression.fn.gather.Average");
		addFunction("min", "com.scudata.expression.fn.gather.Min");
		addFunction("max", "com.scudata.expression.fn.gather.Max");
		addFunction("cand", "com.scudata.expression.fn.Cand");
		addFunction("cor", "com.scudata.expression.fn.Cor");
		addFunction("top", "com.scudata.expression.fn.gather.Top");
		addFunction("ifn", "com.scudata.expression.fn.Ifn");
		addFunction("nvl", "com.scudata.expression.fn.Nvl");
		addFunction("count", "com.scudata.expression.fn.gather.Count");
		addFunction("func", "com.scudata.expression.fn.Func");
		addFunction("call", "com.scudata.expression.fn.Call");
		addFunction("register", "com.scudata.expression.fn.Register");
		addFunction("arguments", "com.scudata.expression.fn.Arguments");
		addFunction("env", "com.scudata.expression.fn.EnvSet");
		addFunction("system", "com.scudata.expression.fn.SystemExec");
		addFunction("output", "com.scudata.expression.fn.Output");
		addFunction("sleep", "com.scudata.expression.fn.Sleep");
		addFunction("lock", "com.scudata.expression.fn.Lock");
		addFunction("sizeof", "com.scudata.expression.fn.Sizeof");
		addFunction("invoke", "com.scudata.expression.fn.Invoke");
		addFunction("icount", "com.scudata.expression.fn.gather.ICount");
		addFunction("mode", "com.scudata.expression.fn.Mode");
		addFunction("median", "com.scudata.expression.fn.gather.Median");
		addFunction("z", "com.scudata.expression.fn.ZSeq");
		addFunction("iterate", "com.scudata.expression.fn.gather.Iterate");
		addFunction("seq", "com.scudata.expression.fn.Seq");
		addFunction("rank", "com.scudata.expression.fn.Rank");
		addFunction("ranki", "com.scudata.expression.fn.Ranki");
		addFunction("cum", "com.scudata.expression.fn.Cum");
		
		addFunction("k", "com.scudata.expression.fn.CreateSerialBytes");
		addFunction("range", "com.scudata.expression.fn.Range");
		addFunction("blob", "com.scudata.expression.fn.Blob");
		addFunction("jdbccall", "com.scudata.expression.fn.JDBCCall");
		
		addFunction("pseudo", "com.scudata.expression.fn.CreatePseudo");
		
		// 并行
		addFunction("callx", "com.scudata.expression.fn.parallel.Callx");
		addFunction("hosts", "com.scudata.expression.fn.parallel.Hosts");
		addFunction("syncfile", "com.scudata.expression.fn.parallel.SyncFile");
		//addFunction("zone", "com.scudata.expression.fn.parallel.Zone");

		// series function
		addMemberFunction("step", "com.scudata.expression.mfn.sequence.Step");
		addMemberFunction("inv", "com.scudata.expression.mfn.sequence.Inv");
		addMemberFunction("p", "com.scudata.expression.mfn.sequence.PosConvert");
		addMemberFunction("m", "com.scudata.expression.mfn.sequence.MGet");
		addMemberFunction("eq", "com.scudata.expression.mfn.sequence.Eq");
		addMemberFunction("count", "com.scudata.expression.mfn.sequence.Count");
		addMemberFunction("len", "com.scudata.expression.mfn.sequence.Len");
		addMemberFunction("len", "com.scudata.expression.mfn.Len");
		addMemberFunction("ifn", "com.scudata.expression.mfn.sequence.Ifn");
		addMemberFunction("nvl", "com.scudata.expression.mfn.sequence.Nvl");
		addMemberFunction("id", "com.scudata.expression.mfn.sequence.Id");
		addMemberFunction("id", "com.scudata.expression.mfn.cursor.Id");
		addMemberFunction("id", "com.scudata.expression.mfn.channel.Id");
		addMemberFunction("sum", "com.scudata.expression.mfn.sequence.Sum");
		addMemberFunction("icount", "com.scudata.expression.mfn.sequence.ICount");
		addMemberFunction("mode", "com.scudata.expression.mfn.sequence.Mode");
		addMemberFunction("avg", "com.scudata.expression.mfn.sequence.Avg");
		addMemberFunction("min", "com.scudata.expression.mfn.sequence.Min");
		addMemberFunction("max", "com.scudata.expression.mfn.sequence.Max");
		addMemberFunction("minp", "com.scudata.expression.mfn.sequence.Minp");
		addMemberFunction("maxp", "com.scudata.expression.mfn.sequence.Maxp");
		addMemberFunction("cand", "com.scudata.expression.mfn.sequence.Cand");
		addMemberFunction("cor", "com.scudata.expression.mfn.sequence.Cor");
		addMemberFunction("rank", "com.scudata.expression.mfn.sequence.Rank");
		addMemberFunction("ranks", "com.scudata.expression.mfn.sequence.Ranks");
		addMemberFunction("conj", "com.scudata.expression.mfn.sequence.Conj");
		addMemberFunction("conj", "com.scudata.expression.mfn.op.AttachConj");
		addMemberFunction("union", "com.scudata.expression.mfn.sequence.Union");
		addMemberFunction("diff", "com.scudata.expression.mfn.sequence.Diff");
		addMemberFunction("isect", "com.scudata.expression.mfn.sequence.Isect");
		addMemberFunction("xunion", "com.scudata.expression.mfn.sequence.Xunion");
		addMemberFunction("merge", "com.scudata.expression.mfn.sequence.Merge");
		addMemberFunction("run", "com.scudata.expression.mfn.sequence.Run");
		addMemberFunction("run", "com.scudata.expression.mfn.op.AttachRun");
		addMemberFunction("run", "com.scudata.expression.mfn.record.Run");
		addMemberFunction("calc", "com.scudata.expression.mfn.sequence.Calc");
		addMemberFunction("pos", "com.scudata.expression.mfn.sequence.Pos");
		addMemberFunction("contain", "com.scudata.expression.mfn.sequence.Contain");
		addMemberFunction("pseg", "com.scudata.expression.mfn.sequence.PSeg");
		addMemberFunction("segp", "com.scudata.expression.mfn.sequence.Segp");
		addMemberFunction("pmin", "com.scudata.expression.mfn.sequence.PMin");
		addMemberFunction("pmax", "com.scudata.expression.mfn.sequence.PMax");
		addMemberFunction("ptop", "com.scudata.expression.mfn.sequence.PTop");
		addMemberFunction("top", "com.scudata.expression.mfn.sequence.Top");
		addMemberFunction("pselect", "com.scudata.expression.mfn.sequence.PSelect");
		addMemberFunction("psort", "com.scudata.expression.mfn.sequence.PSort");
		addMemberFunction("select", "com.scudata.expression.mfn.sequence.Select");
		addMemberFunction("select", "com.scudata.expression.mfn.op.AttachSelect");
		addMemberFunction("sort", "com.scudata.expression.mfn.sequence.Sort");
		addMemberFunction("rvs", "com.scudata.expression.mfn.sequence.Rvs");
		addMemberFunction("swap", "com.scudata.expression.mfn.sequence.Swap");
		addMemberFunction("shift", "com.scudata.expression.mfn.sequence.Shift");
		addMemberFunction("pad", "com.scudata.expression.mfn.sequence.Pad");
		addMemberFunction("lookup", "com.scudata.expression.mfn.sequence.Lookup");
		addMemberFunction("sumif", "com.scudata.expression.mfn.sequence.Sumif");
		addMemberFunction("countif", "com.scudata.expression.mfn.sequence.Countif");
		addMemberFunction("avgif", "com.scudata.expression.mfn.sequence.Avgif");
		addMemberFunction("minif", "com.scudata.expression.mfn.sequence.Minif");
		addMemberFunction("maxif", "com.scudata.expression.mfn.sequence.Maxif");
		addMemberFunction("iterate", "com.scudata.expression.mfn.sequence.Iterate");
		addMemberFunction("iterate", "com.scudata.expression.mfn.cursor.Iterate");
		addMemberFunction("iterate", "com.scudata.expression.mfn.channel.Iterate");
		addMemberFunction("fno", "com.scudata.expression.mfn.record.FieldNo");
		addMemberFunction("fno", "com.scudata.expression.mfn.sequence.FieldNo");
		addMemberFunction("field", "com.scudata.expression.mfn.record.FieldValue");
		addMemberFunction("field", "com.scudata.expression.mfn.sequence.FieldValue");
		addMemberFunction("fname", "com.scudata.expression.mfn.record.FieldName");
		addMemberFunction("fname", "com.scudata.expression.mfn.sequence.FieldName");
		addMemberFunction("to", "com.scudata.expression.mfn.sequence.To");
		addMemberFunction("pivot", "com.scudata.expression.mfn.sequence.Pivot");
		
		addMemberFunction("r", "com.scudata.expression.mfn.RowField");
		addMemberFunction("median", "com.scudata.expression.mfn.sequence.Median");
		
		// 修改
		addMemberFunction("modify", "com.scudata.expression.mfn.sequence.Modify");
		addMemberFunction("modify", "com.scudata.expression.mfn.record.Modify");
		addMemberFunction("reset", "com.scudata.expression.mfn.sequence.Reset");
		addMemberFunction("reset", "com.scudata.expression.mfn.cursor.Reset");
		addMemberFunction("insert", "com.scudata.expression.mfn.sequence.Insert");
		addMemberFunction("delete", "com.scudata.expression.mfn.sequence.Delete");
		addMemberFunction("paste", "com.scudata.expression.mfn.table.Paste");
		addMemberFunction("record", "com.scudata.expression.mfn.sequence.RecordValue");
		addMemberFunction("record", "com.scudata.expression.mfn.record.RecordValue");
		addMemberFunction("rename", "com.scudata.expression.mfn.table.Rename");
		addMemberFunction("rename", "com.scudata.expression.mfn.op.AttachRename");
		addMemberFunction("rename", "com.scudata.expression.mfn.vdb.Rename");
		addMemberFunction("alter", "com.scudata.expression.mfn.table.Alter");

		// 产生
		addMemberFunction("create", "com.scudata.expression.mfn.sequence.Create");
		addMemberFunction("create", "com.scudata.expression.mfn.record.Create");
		addMemberFunction("new", "com.scudata.expression.mfn.sequence.New");
		addMemberFunction("new", "com.scudata.expression.mfn.op.AttachNew");
		addMemberFunction("derive", "com.scudata.expression.mfn.sequence.Derive");
		addMemberFunction("derive", "com.scudata.expression.mfn.op.AttachDerive");
		addMemberFunction("penum", "com.scudata.expression.mfn.sequence.PEnum");
		addMemberFunction("align", "com.scudata.expression.mfn.sequence.Align");
		addMemberFunction("enum", "com.scudata.expression.mfn.sequence.Enum");
		addMemberFunction("group", "com.scudata.expression.mfn.sequence.Group");
		addMemberFunction("group", "com.scudata.expression.mfn.op.AttachGroup");
		addMemberFunction("groups", "com.scudata.expression.mfn.sequence.Groups");
		addMemberFunction("groups", "com.scudata.expression.mfn.cursor.Groups");
		addMemberFunction("groups", "com.scudata.expression.mfn.channel.Groups");
		addMemberFunction("groupi", "com.scudata.expression.mfn.sequence.Groupi");
		addMemberFunction("news", "com.scudata.expression.mfn.sequence.News");
		addMemberFunction("news", "com.scudata.expression.mfn.op.AttachNews");

		addMemberFunction("pfind", "com.scudata.expression.mfn.sequence.PFind");
		addMemberFunction("find", "com.scudata.expression.mfn.sequence.Find");
		addMemberFunction("v", "com.scudata.expression.mfn.Value");

		// 映射
		addMemberFunction("key", "com.scudata.expression.mfn.record.Key");
		addMemberFunction("keys", "com.scudata.expression.mfn.table.Keys");
		addMemberFunction("switch", "com.scudata.expression.mfn.sequence.SwitchFK");
		addMemberFunction("switch", "com.scudata.expression.mfn.op.AttachSwitch");
		addMemberFunction("index", "com.scudata.expression.mfn.table.Index");
		addMemberFunction("prior", "com.scudata.expression.mfn.record.Prior");
		addMemberFunction("nodes", "com.scudata.expression.mfn.sequence.Nodes");

		addMemberFunction("array", "com.scudata.expression.mfn.record.Array");
		addMemberFunction("array", "com.scudata.expression.mfn.sequence.Array");
		addMemberFunction("regex", "com.scudata.expression.mfn.string.Regex");
		addMemberFunction("regex", "com.scudata.expression.mfn.sequence.Regex");
		addMemberFunction("regex", "com.scudata.expression.mfn.op.AttachRegex");
		addMemberFunction("concat", "com.scudata.expression.mfn.sequence.Concat");

		// 简单存储
		addFunction("filename", "com.scudata.expression.fn.FileName");
		addFunction("directory", "com.scudata.expression.fn.Directory");
		addFunction("file", "com.scudata.expression.fn.CreateFile");
		addFunction("movefile", "com.scudata.expression.fn.MoveFile");
		addFunction("httpfile", "com.scudata.expression.fn.CreateHttpFile");
		addFunction("httpupload", "com.scudata.expression.fn.Http_Upload");
		addMemberFunction("read", "com.scudata.expression.mfn.file.Read");
		addMemberFunction("write", "com.scudata.expression.mfn.file.Write");
		addMemberFunction("export", "com.scudata.expression.mfn.file.Export");
		addMemberFunction("export", "com.scudata.expression.mfn.sequence.Export");
		addMemberFunction("import", "com.scudata.expression.mfn.file.Import");
		addMemberFunction("import", "com.scudata.expression.mfn.string.Import");
		addMemberFunction("name", "com.scudata.expression.mfn.file.Name");
		addMemberFunction("exists", "com.scudata.expression.mfn.file.Exists");
		addMemberFunction("size", "com.scudata.expression.mfn.file.Size");
		addMemberFunction("date", "com.scudata.expression.mfn.file.Date");
		addMemberFunction("property", "com.scudata.expression.mfn.file.Property");
		addMemberFunction("property", "com.scudata.expression.mfn.string.Property");
		addMemberFunction("iselect", "com.scudata.expression.mfn.file.ISelect");

		// excel
		addMemberFunction("xlsexport", "com.scudata.expression.mfn.file.XlsExport");
		addMemberFunction("xlsexport", "com.scudata.expression.mfn.xo.XlsExport");
		addMemberFunction("xlsimport", "com.scudata.expression.mfn.file.XlsImport");
		addMemberFunction("xlsimport", "com.scudata.expression.mfn.xo.XlsImport");
		addMemberFunction("xlsopen", "com.scudata.expression.mfn.file.XlsOpen");
		addMemberFunction("xlswrite", "com.scudata.expression.mfn.file.XlsWrite");
		addMemberFunction("xlsclose", "com.scudata.expression.mfn.xo.XlsClose");
		addMemberFunction("xlscell", "com.scudata.expression.mfn.xo.XlsCell");
		
		addFunction("T", "com.scudata.expression.fn.T");
		addFunction("cellname", "com.scudata.expression.fn.CellName");

		addMemberFunction("htmlparse", "com.scudata.expression.mfn.string.HTMLParse");
		addMemberFunction("close", "com.scudata.expression.mfn.Close");
		
		// 数据库
		addFunction("connect", "com.scudata.expression.fn.Connect");
		addMemberFunction("commit", "com.scudata.expression.mfn.db.Commit");
		addMemberFunction("rollback", "com.scudata.expression.mfn.db.Rollback");
		addMemberFunction("rollback", "com.scudata.expression.mfn.file.Rollback");
		addMemberFunction("rollback", "com.scudata.expression.mfn.file.FileGroupRollback");
		addMemberFunction("query", "com.scudata.expression.mfn.db.Query");
		addMemberFunction("query", "com.scudata.expression.mfn.file.Query");
		addMemberFunction("execute", "com.scudata.expression.mfn.db.Execute");
		addMemberFunction("proc", "com.scudata.expression.mfn.db.Proc");
		addMemberFunction("error", "com.scudata.expression.mfn.db.Error");
		addMemberFunction("update", "com.scudata.expression.mfn.db.Update");
		addMemberFunction("isolate", "com.scudata.expression.mfn.db.Isolate");
		addMemberFunction("savepoint", "com.scudata.expression.mfn.db.SavePoint");
		
		// 数据仓库
		addFunction("vdbase", "com.scudata.expression.fn.VDBase");
		addMemberFunction("begin", "com.scudata.expression.mfn.vdb.Begin");
		addMemberFunction("commit", "com.scudata.expression.mfn.vdb.Commit");
		addMemberFunction("rollback", "com.scudata.expression.mfn.vdb.Rollback");
		addMemberFunction("home", "com.scudata.expression.mfn.vdb.Home");
		addMemberFunction("path", "com.scudata.expression.mfn.vdb.Path");
		addMemberFunction("lock", "com.scudata.expression.mfn.vdb.Lock");
		addMemberFunction("list", "com.scudata.expression.mfn.vdb.List");
		addMemberFunction("load", "com.scudata.expression.mfn.vdb.Load");
		addMemberFunction("date", "com.scudata.expression.mfn.vdb.Date");
		addMemberFunction("save", "com.scudata.expression.mfn.vdb.Save");
		addMemberFunction("move", "com.scudata.expression.mfn.vdb.Move");
		addMemberFunction("read", "com.scudata.expression.mfn.vdb.Read");
		addMemberFunction("write", "com.scudata.expression.mfn.vdb.Write");
		addMemberFunction("update", "com.scudata.expression.mfn.vdb.Update");
		addMemberFunction("saveblob", "com.scudata.expression.mfn.vdb.SaveBlob");
		addMemberFunction("retrieve", "com.scudata.expression.mfn.vdb.Retrive");
		addMemberFunction("archive", "com.scudata.expression.mfn.vdb.Archive");
		addMemberFunction("purge", "com.scudata.expression.mfn.vdb.Purge");
		addMemberFunction("copy", "com.scudata.expression.mfn.vdb.Copy");
		
		// 游标
		addMemberFunction("cursor", "com.scudata.expression.mfn.db.CreateCursor");
		addMemberFunction("cursor", "com.scudata.expression.mfn.file.CreateCursor");
		addMemberFunction("cursor", "com.scudata.expression.mfn.sequence.CreateCursor");
		addMemberFunction("cursor", "com.scudata.expression.mfn.cursor.CreateCursor");
		addMemberFunction("mcursor", "com.scudata.expression.mfn.sequence.MCursor");
		addMemberFunction("mcursor", "com.scudata.expression.mfn.cursor.MCursor");
		addMemberFunction("fetch", "com.scudata.expression.mfn.cursor.Fetch");
		addMemberFunction("fetch", "com.scudata.expression.mfn.channel.Fetch");
		addMemberFunction("skip", "com.scudata.expression.mfn.cursor.Skip");
		addMemberFunction("groupx", "com.scudata.expression.mfn.cursor.Groupx");
		addMemberFunction("groupx", "com.scudata.expression.mfn.channel.Groupx");
		addMemberFunction("groupn", "com.scudata.expression.mfn.op.AttachGroupn");
		addMemberFunction("sortx", "com.scudata.expression.mfn.cursor.Sortx");
		addMemberFunction("sortx", "com.scudata.expression.mfn.channel.Sortx");
		addMemberFunction("join", "com.scudata.expression.mfn.op.AttachJoin");
		addMemberFunction("join", "com.scudata.expression.mfn.sequence.JoinFK");
		addMemberFunction("joinx", "com.scudata.expression.mfn.cursor.Joinx");
		addMemberFunction("joinx", "com.scudata.expression.mfn.sequence.Joinx");
		addMemberFunction("joinx", "com.scudata.expression.mfn.channel.Joinx");
		addMemberFunction("mergex", "com.scudata.expression.mfn.cursor.Mergex");
		addMemberFunction("mergex", "com.scudata.expression.mfn.sequence.Mergex");
		addMemberFunction("conjx", "com.scudata.expression.mfn.sequence.Conjx");
		addMemberFunction("total", "com.scudata.expression.mfn.cursor.Total");
		addMemberFunction("total", "com.scudata.expression.mfn.channel.Total");


		addFunction("xjoinx", "com.scudata.expression.fn.XJoinx");
		addFunction("joinx", "com.scudata.expression.fn.Joinx");
		addFunction("cursor", "com.scudata.expression.fn.CreateCursor");
		addFunction("channel", "com.scudata.expression.fn.CreateChannel");
		addMemberFunction("push", "com.scudata.expression.mfn.op.AttachPush");
		addMemberFunction("result", "com.scudata.expression.mfn.channel.Result");
		
		// 仓库
		addFunction("memory", "com.scudata.expression.fn.parallel.Memory");
		addMemberFunction("row", "com.scudata.expression.mfn.TableRow");
		addMemberFunction("dup", "com.scudata.expression.mfn.table.Dup");
		addMemberFunction("dup", "com.scudata.expression.mfn.cluster.Dup");
		addMemberFunction("attach", "com.scudata.expression.mfn.dw.Attach");
		addMemberFunction("attach", "com.scudata.expression.mfn.cluster.Attach");
		addMemberFunction("append", "com.scudata.expression.mfn.dw.Append");
		addMemberFunction("update", "com.scudata.expression.mfn.dw.Update");
		addMemberFunction("update", "com.scudata.expression.mfn.dw.UpdateMemoryTable");
		addMemberFunction("delete", "com.scudata.expression.mfn.dw.Delete");
		addMemberFunction("index", "com.scudata.expression.mfn.dw.Index");
		addMemberFunction("memory", "com.scudata.expression.mfn.dw.Memory");
		addMemberFunction("cursor", "com.scudata.expression.mfn.dw.CreateCursor");
		addMemberFunction("import", "com.scudata.expression.mfn.dw.Import");
		addMemberFunction("new", "com.scudata.expression.mfn.dw.New");
		addMemberFunction("news", "com.scudata.expression.mfn.dw.News");
		addMemberFunction("derive", "com.scudata.expression.mfn.dw.Derive");
		addMemberFunction("icursor", "com.scudata.expression.mfn.dw.Icursor");
		addMemberFunction("cgroups", "com.scudata.expression.mfn.dw.Cgroups");
		addMemberFunction("find", "com.scudata.expression.mfn.dw.Find");
		addMemberFunction("create", "com.scudata.expression.mfn.file.Create");
		addMemberFunction("create", "com.scudata.expression.mfn.dw.Create");
		addMemberFunction("open", "com.scudata.expression.mfn.file.Open");
		addMemberFunction("reset", "com.scudata.expression.mfn.file.Reset");
		addMemberFunction("create", "com.scudata.expression.mfn.file.FileGroupCreate");
		addMemberFunction("reset", "com.scudata.expression.mfn.file.FileGroupReset");
		addMemberFunction("open", "com.scudata.expression.mfn.file.FileGroupOpen");
		addMemberFunction("cuboid", "com.scudata.expression.mfn.dw.CreateCuboid");
		addMemberFunction("rename", "com.scudata.expression.mfn.dw.Rename");
		addMemberFunction("alter", "com.scudata.expression.mfn.dw.Alter");
		
		//虚表
		addMemberFunction("import", "com.scudata.expression.mfn.pseudo.Import");
		addMemberFunction("cursor", "com.scudata.expression.mfn.pseudo.CreateCursor");
		addMemberFunction("append", "com.scudata.expression.mfn.pseudo.Append");
		addMemberFunction("update", "com.scudata.expression.mfn.pseudo.Update");
		addMemberFunction("delete", "com.scudata.expression.mfn.pseudo.Delete");
		addMemberFunction("groups", "com.scudata.expression.mfn.pseudo.Groups");
		
		//集群
		addMemberFunction("create", "com.scudata.expression.mfn.cluster.Create");
		addMemberFunction("open", "com.scudata.expression.mfn.cluster.Open");
		addMemberFunction("append", "com.scudata.expression.mfn.cluster.Append");
		addMemberFunction("update", "com.scudata.expression.mfn.cluster.Update");
		addMemberFunction("delete", "com.scudata.expression.mfn.cluster.Delete");
		addMemberFunction("index", "com.scudata.expression.mfn.cluster.Index");
		addMemberFunction("index", "com.scudata.expression.mfn.cluster.MemoryIndex");
		addMemberFunction("cuboid", "com.scudata.expression.mfn.cluster.CreateCuboid");
		addMemberFunction("reset", "com.scudata.expression.mfn.cluster.Reset");
		addMemberFunction("memory", "com.scudata.expression.mfn.cursor.Memory");
		addMemberFunction("memory", "com.scudata.expression.mfn.cluster.Memory");
		addMemberFunction("cursor", "com.scudata.expression.mfn.cluster.CreateCursor");
		addMemberFunction("cursor", "com.scudata.expression.mfn.cluster.CreateMemoryCursor");
		addMemberFunction("new", "com.scudata.expression.mfn.cluster.New");
		addMemberFunction("news", "com.scudata.expression.mfn.cluster.News");
		addMemberFunction("derive", "com.scudata.expression.mfn.cluster.Derive");
		addMemberFunction("icursor", "com.scudata.expression.mfn.cluster.Icursor");
		addMemberFunction("cgroups", "com.scudata.expression.mfn.cluster.Cgroups");
		
		// 统计图
		addFunction("canvas", "com.scudata.expression.fn.CreateCanvas");
		addMemberFunction("plot", "com.scudata.expression.mfn.canvas.Plot");
		addMemberFunction("draw", "com.scudata.expression.mfn.canvas.Draw");
		addMemberFunction("hlink", "com.scudata.expression.mfn.canvas.HLink");

		// 时间日期函数
		addFunction("age", "com.scudata.expression.fn.datetime.Age");
		addFunction("datetime", "com.scudata.expression.fn.datetime.DateTime");
		addFunction("day", "com.scudata.expression.fn.datetime.Day");
		addFunction("hour", "com.scudata.expression.fn.datetime.Hour");
		addFunction("minute", "com.scudata.expression.fn.datetime.Minute");
		addFunction("month", "com.scudata.expression.fn.datetime.Month");
		addFunction("now", "com.scudata.expression.fn.datetime.Now");
		addFunction("second", "com.scudata.expression.fn.datetime.Second");
		addFunction("millisecond", "com.scudata.expression.fn.datetime.Millisecond");
		addFunction("date", "com.scudata.expression.fn.datetime.ToDate");
		addFunction("time", "com.scudata.expression.fn.datetime.ToTime");
		addFunction("year", "com.scudata.expression.fn.datetime.Year");
		addFunction("periods", "com.scudata.expression.fn.datetime.Period");
		addFunction("interval", "com.scudata.expression.fn.datetime.Interval");
		addFunction("elapse", "com.scudata.expression.fn.datetime.Elapse");
		addFunction("days", "com.scudata.expression.fn.datetime.Days");
		addFunction("pdate", "com.scudata.expression.fn.datetime.PDate");
		addFunction("deq", "com.scudata.expression.fn.datetime.DateEqual");
		addFunction("workday", "com.scudata.expression.fn.datetime.WorkDay");
		addFunction("workdays", "com.scudata.expression.fn.datetime.WorkDays");

		// 数学函数
		addFunction("abs", "com.scudata.expression.fn.math.Abs");
		addFunction("and","com.scudata.expression.fn.math.And");
		addFunction("acos", "com.scudata.expression.fn.math.Arccos");
		addFunction("acosh", "com.scudata.expression.fn.math.Arccosh");
		addFunction("asin", "com.scudata.expression.fn.math.Arcsin");
		addFunction("asinh", "com.scudata.expression.fn.math.Arcsinh");
		addFunction("atan", "com.scudata.expression.fn.math.Arctan");
		addFunction("atanh", "com.scudata.expression.fn.math.Arctanh");
		addFunction("bin","com.scudata.expression.fn.math.Bin");
		addFunction("bits","com.scudata.expression.fn.math.Bits");
		addFunction("ceil", "com.scudata.expression.fn.math.Ceiling");
		addFunction("combin","com.scudata.expression.fn.math.Combin");
		addFunction("cos", "com.scudata.expression.fn.math.Cos");
		addFunction("cosh", "com.scudata.expression.fn.math.Cosh");
		addFunction("digits","com.scudata.expression.fn.math.Digits");
		addFunction("exp", "com.scudata.expression.fn.math.Exp");
		addFunction("fact", "com.scudata.expression.fn.math.Fact");
		addFunction("floor", "com.scudata.expression.fn.math.Floor");
		addFunction("gcd","com.scudata.expression.fn.math.Gcd");
		addFunction("hash","com.scudata.expression.fn.math.Hash");
		addFunction("hex","com.scudata.expression.fn.math.Hex");
		addFunction("inf","com.scudata.expression.fn.math.Inf");
		addFunction("lcm","com.scudata.expression.fn.math.Lcm");
		addFunction("lg", "com.scudata.expression.fn.math.Loga");
		addFunction("ln", "com.scudata.expression.fn.math.Log");
		addFunction("not","com.scudata.expression.fn.math.Not");
		addFunction("or","com.scudata.expression.fn.math.Or");
		addFunction("permut","com.scudata.expression.fn.math.Permut");
		addFunction("pi", "com.scudata.expression.fn.math.Pi");
		addFunction("power", "com.scudata.expression.fn.math.Pow");
		addFunction("product","com.scudata.expression.fn.math.Product");
		addFunction("rand", "com.scudata.expression.fn.math.Rand");
		addFunction("round", "com.scudata.expression.fn.math.Round");
		addFunction("shift","com.scudata.expression.fn.math.Shift");
		addFunction("sign", "com.scudata.expression.fn.math.Sign");
		addFunction("sin", "com.scudata.expression.fn.math.Sin");
		addFunction("sinh", "com.scudata.expression.fn.math.Sinh");
		addFunction("sqrt", "com.scudata.expression.fn.math.Sqrt");
		addFunction("tan", "com.scudata.expression.fn.math.Tan");
		addFunction("tanh", "com.scudata.expression.fn.math.Tanh");
		addFunction("xor","com.scudata.expression.fn.math.Xor");
		
		// 字符串函数
		addFunction("fill", "com.scudata.expression.fn.string.Fill");
		addFunction("left", "com.scudata.expression.fn.string.Left");
		addFunction("len", "com.scudata.expression.fn.string.Len");
		addFunction("like", "com.scudata.expression.fn.string.Like");
		addFunction("lower", "com.scudata.expression.fn.string.Lower");
		addFunction("mid", "com.scudata.expression.fn.string.Mid");
		addFunction("pos", "com.scudata.expression.fn.string.Pos");
		addFunction("replace", "com.scudata.expression.fn.string.Replace");
		addFunction("right", "com.scudata.expression.fn.string.Right");
		addFunction("trim", "com.scudata.expression.fn.string.Trim");
		addFunction("upper", "com.scudata.expression.fn.string.Upper");
		addFunction("pad", "com.scudata.expression.fn.string.Pad");
		addFunction("rands", "com.scudata.expression.fn.string.Rands");
		addFunction("concat", "com.scudata.expression.fn.string.Concat");
		addFunction("urlencode", "com.scudata.expression.fn.string.URLEncode");
		addFunction("base64", "com.scudata.expression.fn.string.Base64");
		addFunction("md5", "com.scudata.expression.fn.string.MD5Encrypt");
		addFunction("substr", "com.scudata.expression.fn.string.SubString");
		
		addMemberFunction("words", "com.scudata.expression.mfn.string.Words");
		addMemberFunction("split", "com.scudata.expression.mfn.string.Split");
		addMemberFunction("sqlparse", "com.scudata.expression.mfn.string.SQLParse");
		addMemberFunction("sqltranslate", "com.scudata.expression.mfn.string.SQLTranslate");

		// 类型转换函数
		addFunction("ifv", "com.scudata.expression.fn.convert.IfVariable");
		addFunction("ifa", "com.scudata.expression.fn.convert.IfSequence");
		addFunction("ifr", "com.scudata.expression.fn.convert.IfRecord");
		addFunction("ift", "com.scudata.expression.fn.convert.IfTable");
		addFunction("ifdate", "com.scudata.expression.fn.convert.IfDate");
		addFunction("iftime", "com.scudata.expression.fn.convert.IfTime");
		addFunction("ifnumber", "com.scudata.expression.fn.convert.IfNumber");
		addFunction("ifstring", "com.scudata.expression.fn.convert.IfString");
		addFunction("isalpha", "com.scudata.expression.fn.convert.IsAlpha");
		addFunction("isdigit", "com.scudata.expression.fn.convert.IsDigit");
		addFunction("islower", "com.scudata.expression.fn.convert.IsLower");
		addFunction("isupper", "com.scudata.expression.fn.convert.IsUpper");

		addFunction("bool", "com.scudata.expression.fn.convert.ToBool");
		addFunction("int", "com.scudata.expression.fn.convert.ToInteger");
		addFunction("long", "com.scudata.expression.fn.convert.ToLong");
		addFunction("float", "com.scudata.expression.fn.convert.ToDouble");
		addFunction("number", "com.scudata.expression.fn.convert.ToNumber");
		addFunction("string", "com.scudata.expression.fn.convert.ToString");
		addFunction("decimal", "com.scudata.expression.fn.convert.ToBigDecimal");
		addFunction("asc", "com.scudata.expression.fn.convert.ToAsc");
		addFunction("char", "com.scudata.expression.fn.convert.ToChar");
		addFunction("rgb", "com.scudata.expression.fn.convert.RGB");
		addFunction("chn", "com.scudata.expression.fn.convert.ToChinese");
		addFunction("parse", "com.scudata.expression.fn.convert.Parse");
		addFunction("format", "com.scudata.expression.fn.convert.Format");
		addFunction("json", "com.scudata.expression.fn.convert.Json");
		addFunction("xml", "com.scudata.expression.fn.convert.Xml");
		
		//financial function
		addFunction("Fsln","com.scudata.expression.fn.financial.Sln");
		addFunction("Fsyd","com.scudata.expression.fn.financial.Syd");
		addFunction("Fdb","com.scudata.expression.fn.financial.Db");
		addFunction("Fddb","com.scudata.expression.fn.financial.Ddb");
		addFunction("Fvdb","com.scudata.expression.fn.financial.Vdb");
		addFunction("Fnper","com.scudata.expression.fn.financial.Nper");
		addFunction("Fpmt","com.scudata.expression.fn.financial.Pmt");
		addFunction("Fv","com.scudata.expression.fn.financial.Fv");
		addFunction("Frate","com.scudata.expression.fn.financial.Rate");
		addFunction("Fnpv","com.scudata.expression.fn.financial.Npv");
		addFunction("Firr","com.scudata.expression.fn.financial.Irr");
		addFunction("Fmirr","com.scudata.expression.fn.financial.Mirr");
		addFunction("Faccrint","com.scudata.expression.fn.financial.Accrint");
		addFunction("Faccrintm","com.scudata.expression.fn.financial.Accrintm");
		addFunction("Fintrate","com.scudata.expression.fn.financial.Intrate");
		addFunction("Freceived","com.scudata.expression.fn.financial.Received");
		addFunction("Fprice","com.scudata.expression.fn.financial.Price");
		addFunction("Fdisc","com.scudata.expression.fn.financial.Disc");
		addFunction("Fyield","com.scudata.expression.fn.financial.Yield");
		addFunction("Fcoups","com.scudata.expression.fn.financial.Coups");
		addFunction("Fcoupcd","com.scudata.expression.fn.financial.Coupcd");
		addFunction("Fduration","com.scudata.expression.fn.financial.Duration");
		
		//algebra function
		addFunction("var","com.scudata.expression.fn.algebra.Var");
		addFunction("mse","com.scudata.expression.fn.algebra.Mse");
		addFunction("mae","com.scudata.expression.fn.algebra.Mae");
		addFunction("cov","com.scudata.expression.fn.algebra.Cov");
		addFunction("covm","com.scudata.expression.fn.algebra.Covm");
		addFunction("dis","com.scudata.expression.fn.algebra.Distance");
		addFunction("dism","com.scudata.expression.fn.algebra.Dism");
		addFunction("I","com.scudata.expression.fn.algebra.Identity");
		addFunction("mul","com.scudata.expression.fn.algebra.Mul");
		addFunction("transpose","com.scudata.expression.fn.algebra.Transpose");
		addFunction("inverse","com.scudata.expression.fn.algebra.Inverse");
		addFunction("det","com.scudata.expression.fn.algebra.Det");
		addFunction("rankm","com.scudata.expression.fn.algebra.Rankm");
		addFunction("linefit","com.scudata.expression.fn.algebra.Linefit");
		addFunction("polyfit","com.scudata.expression.fn.algebra.Polyfit");
		addFunction("norm","com.scudata.expression.fn.algebra.Normalize");
		addFunction("pearson","com.scudata.expression.fn.algebra.Pearson");
		addFunction("spearman","com.scudata.expression.fn.algebra.Spearman");
		addFunction("pca","com.scudata.expression.fn.algebra.PCA");
		addFunction("pls","com.scudata.expression.fn.algebra.PLS");
		addFunction("sg","com.scudata.expression.fn.algebra.SavizkgGolag");
		addFunction("ones","com.scudata.expression.fn.algebra.Ones");
		addFunction("zeros","com.scudata.expression.fn.algebra.Zeros");
		addFunction("eye","com.scudata.expression.fn.algebra.Eye");
		addFunction("mfind","com.scudata.expression.fn.algebra.MFind");
		addFunction("msum","com.scudata.expression.fn.algebra.MSum");
		addFunction("mcumsum","com.scudata.expression.fn.algebra.MCumsum");
		addFunction("mmean","com.scudata.expression.fn.algebra.MMean");
		addFunction("mnorm","com.scudata.expression.fn.algebra.MNormalize");
		addFunction("mstd","com.scudata.expression.fn.algebra.MStd");
		addFunction("norminv","com.scudata.expression.fn.algebra.Norminv");
		addFunction("lasso","com.scudata.expression.fn.algebra.Lasso");
		addFunction("ridge","com.scudata.expression.fn.algebra.Ridge");
		addFunction("elasticnet","com.scudata.expression.fn.algebra.ElasticNet");
	}

	/**
	 * 加载自定义函数
	 * @param fileName String
	 */
	public static void loadCustomFunctions(String fileName) {
		try {
			InputStream is = null;
			File file;
			file = new File(fileName);
			if (file.exists()) {
				is = new FileInputStream(file);
			} else {
				is = FunctionLib.class.getResourceAsStream(fileName);
			}
			
			if (is == null) {
				throw new Exception("load customFunction file inputstream failed.");
			} else {
				loadCustomFunctions(is);
			}
		} catch (Exception x) {
		}
	}

	/**
	 * 加载用户定义函数
	 * @param is 输入流
	 */
	public static void loadCustomFunctions(InputStream is) {
		try {
			Properties pt = new Properties();
			pt.load(is);
			for (Enumeration<?> e = pt.propertyNames(); e.hasMoreElements(); ) {
				Object key = e.nextElement();
				String value = (String) pt.get(key);
				int pos = value.indexOf(',');
				String type = value.substring(0, pos).trim();
				String cls = value.substring(pos + 1, value.length()).trim();

				if (type.equals("1")) {
					addMemberFunction( (String) key, cls);
				} else if (type.equals("0")) {
					addFunction( (String) key, cls);
				}

			} //for
		}
		catch (Throwable e) {
			Logger.error(e.getMessage());
		}
	}
	
	private static void loadExt(InputStream is, ClassLoader loader) throws Exception {
		Properties pt = new Properties();
		pt.load(is);
		HashMap<String, Class<? extends Function>> m0 = new HashMap<String, Class<? extends Function>>();
		ArrayList<String> mfnNames = new ArrayList<String>();
		ArrayList<Class<? extends MemberFunction>> mfns = new ArrayList<Class<? extends MemberFunction>>();
		
		for (Enumeration<?> e = pt.propertyNames(); e.hasMoreElements(); ) {
			String key = (String)e.nextElement();
			String value = (String) pt.get(key);
			int pos = value.indexOf(',');
			String type = value.substring(0, pos).trim();
			String clsName = value.substring(pos + 1, value.length()).trim();

			if (type.equals("0")) {
				// 全局函数不允许重名
				if (fnMap.containsKey(key)) {
					MessageManager mm = EngineMessage.get();
					throw new RuntimeException(mm.getMessage("FunctionLib.repeatedFunction") + key);
				}
				
				Class<? extends Function> cls = (Class<? extends Function>)loader.loadClass(clsName);
				m0.put(key, cls);
			} else if (type.equals("1")) {
				// 成员函数可以重名
				Class<? extends MemberFunction> cls = (Class<? extends MemberFunction>)loader.loadClass(clsName);
				mfnNames.add(key);
				mfns.add(cls);
			}
		}
		
		fnMap.putAll(m0);
		for (int i = 0, size = mfnNames.size(); i < size; ++i) {
			addMemberFunction(mfnNames.get(i), mfns.get(i));
		}
	}
	
	/**
	 * 装载外部库
	 * @param path 外部库配置文件
	 */
	public static void loadExtLibrary(File path){
		File[] fs = path.listFiles();
		ArrayList<URL> list = new ArrayList<URL>();
		ArrayList<File> jars = new ArrayList<File>();
		for (File f : fs) {
			if (f.getName().endsWith(".jar")) {
				jars.add(f);
				try{
					list.add(new URL("file", null, 0, f.getCanonicalPath()));
				} catch (Exception e) {
				}
			}
		}
		
		//loader没指定父
		URLClassLoader loader = new URLClassLoader(list.toArray(new URL[]{}), FunctionLib.class.getClassLoader());
		Pattern p = Pattern.compile("com/scudata/lib/(\\w+)/functions.properties");
		
		for (File f : jars) {
			JarFile jf = null;
			try {
				jf = new JarFile(f);
				Enumeration<JarEntry> jee = jf.entries();
				while (jee.hasMoreElements()) {
					JarEntry je = jee.nextElement();
					Matcher m = p.matcher(je.getName());
					if(!m.matches()) continue;
					String libName = m.group(1);
					try{
						InputStream in = jf.getInputStream(je);
						loadExt(in, loader);
						Logger.info("load library [" + libName + "] from " + path.getName());
					} catch(Throwable e) {
						Logger.error("failed to load library [" + libName + "] from " + path.getName(), e);
					}
				}
				
				jf.close();
			} catch(Exception e) {
				Logger.error("failed to open jar file" + path.getName() + "/" + f.getName(), e);
				continue;
			}
		}
	}
	
	/**
	 * 计算指定成员函数
	 * @param leftValue 左侧对象
	 * @param fnName 函数名
	 * @param param 函数参数
	 * @param option 函数选项
	 * @param ctx 计算上下文
	 * @return 计算结果
	 */
	public static Object executeMemberFunction(Object leftValue, String fnName, String param, String option, Context ctx) {
		MemberFunction fn = FunctionLib.newMemberFunction(fnName);
		fn.setParameter(null, ctx, param);
		fn.setOption(option);
		
		Constant leftNode = new Constant(leftValue);
		DotOperator dot = new DotOperator();
		dot.setLeft(leftNode);
		dot.setRight(fn);
		return dot.calculate(ctx);
	}
}
