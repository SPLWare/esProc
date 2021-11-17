package com.raqsoft.expression;

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

import com.raqsoft.common.Logger;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.operator.DotOperator;
import com.raqsoft.resources.EngineMessage;

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
		addFunction("between", "com.raqsoft.expression.fn.Between");
		addFunction("case", "com.raqsoft.expression.fn.Case");
		addFunction("cmp", "com.raqsoft.expression.fn.Compare");
		addFunction("clipboard", "com.raqsoft.expression.fn.Clipboard");
		addFunction("eval", "com.raqsoft.expression.fn.Eval");
		addFunction("if", "com.raqsoft.expression.fn.If");
		addFunction("to", "com.raqsoft.expression.fn.To");
		addFunction("join", "com.raqsoft.expression.fn.Join");
		addFunction("xjoin", "com.raqsoft.expression.fn.XJoin");
		addFunction("create", "com.raqsoft.expression.fn.Create");
		addFunction("new", "com.raqsoft.expression.fn.New");
		addFunction("sum", "com.raqsoft.expression.fn.gather.Sum");
		addFunction("avg", "com.raqsoft.expression.fn.gather.Average");
		addFunction("min", "com.raqsoft.expression.fn.gather.Min");
		addFunction("max", "com.raqsoft.expression.fn.gather.Max");
		addFunction("cand", "com.raqsoft.expression.fn.Cand");
		addFunction("cor", "com.raqsoft.expression.fn.Cor");
		addFunction("top", "com.raqsoft.expression.fn.gather.Top");
		addFunction("ifn", "com.raqsoft.expression.fn.Ifn");
		addFunction("nvl", "com.raqsoft.expression.fn.Nvl");
		addFunction("count", "com.raqsoft.expression.fn.gather.Count");
		addFunction("func", "com.raqsoft.expression.fn.Func");
		addFunction("call", "com.raqsoft.expression.fn.Call");
		addFunction("register", "com.raqsoft.expression.fn.Register");
		addFunction("arguments", "com.raqsoft.expression.fn.Arguments");
		addFunction("env", "com.raqsoft.expression.fn.EnvSet");
		addFunction("system", "com.raqsoft.expression.fn.SystemExec");
		addFunction("output", "com.raqsoft.expression.fn.Output");
		addFunction("sleep", "com.raqsoft.expression.fn.Sleep");
		addFunction("lock", "com.raqsoft.expression.fn.Lock");
		addFunction("sizeof", "com.raqsoft.expression.fn.Sizeof");
		addFunction("invoke", "com.raqsoft.expression.fn.Invoke");
		addFunction("icount", "com.raqsoft.expression.fn.gather.ICount");
		addFunction("mode", "com.raqsoft.expression.fn.Mode");
		addFunction("median", "com.raqsoft.expression.fn.gather.Median");
		addFunction("z", "com.raqsoft.expression.fn.ZSeq");
		addFunction("iterate", "com.raqsoft.expression.fn.gather.Iterate");
		addFunction("seq", "com.raqsoft.expression.fn.Seq");
		addFunction("rank", "com.raqsoft.expression.fn.Rank");
		addFunction("ranki", "com.raqsoft.expression.fn.Ranki");
		addFunction("cum", "com.raqsoft.expression.fn.Cum");
		
		addFunction("k", "com.raqsoft.expression.fn.CreateSerialBytes");
		addFunction("range", "com.raqsoft.expression.fn.Range");
		addFunction("blob", "com.raqsoft.expression.fn.Blob");
		addFunction("jdbccall", "com.raqsoft.expression.fn.JDBCCall");
		
		addFunction("pseudo", "com.raqsoft.expression.fn.CreatePseudo");
		
		// 并行
		addFunction("callx", "com.raqsoft.expression.fn.parallel.Callx");
		addFunction("hosts", "com.raqsoft.expression.fn.parallel.Hosts");
		addFunction("syncfile", "com.raqsoft.expression.fn.parallel.SyncFile");
		//addFunction("zone", "com.raqsoft.expression.fn.parallel.Zone");

		// series function
		addMemberFunction("step", "com.raqsoft.expression.mfn.sequence.Step");
		addMemberFunction("inv", "com.raqsoft.expression.mfn.sequence.Inv");
		addMemberFunction("p", "com.raqsoft.expression.mfn.sequence.PosConvert");
		addMemberFunction("m", "com.raqsoft.expression.mfn.sequence.MGet");
		addMemberFunction("eq", "com.raqsoft.expression.mfn.sequence.Eq");
		addMemberFunction("count", "com.raqsoft.expression.mfn.sequence.Count");
		addMemberFunction("len", "com.raqsoft.expression.mfn.sequence.Len");
		addMemberFunction("len", "com.raqsoft.expression.mfn.Len");
		addMemberFunction("ifn", "com.raqsoft.expression.mfn.sequence.Ifn");
		addMemberFunction("nvl", "com.raqsoft.expression.mfn.sequence.Nvl");
		addMemberFunction("id", "com.raqsoft.expression.mfn.sequence.Id");
		addMemberFunction("id", "com.raqsoft.expression.mfn.cursor.Id");
		addMemberFunction("id", "com.raqsoft.expression.mfn.channel.Id");
		addMemberFunction("sum", "com.raqsoft.expression.mfn.sequence.Sum");
		addMemberFunction("icount", "com.raqsoft.expression.mfn.sequence.ICount");
		addMemberFunction("mode", "com.raqsoft.expression.mfn.sequence.Mode");
		addMemberFunction("avg", "com.raqsoft.expression.mfn.sequence.Avg");
		addMemberFunction("min", "com.raqsoft.expression.mfn.sequence.Min");
		addMemberFunction("max", "com.raqsoft.expression.mfn.sequence.Max");
		addMemberFunction("minp", "com.raqsoft.expression.mfn.sequence.Minp");
		addMemberFunction("maxp", "com.raqsoft.expression.mfn.sequence.Maxp");
		addMemberFunction("cand", "com.raqsoft.expression.mfn.sequence.Cand");
		addMemberFunction("cor", "com.raqsoft.expression.mfn.sequence.Cor");
		addMemberFunction("rank", "com.raqsoft.expression.mfn.sequence.Rank");
		addMemberFunction("ranks", "com.raqsoft.expression.mfn.sequence.Ranks");
		addMemberFunction("conj", "com.raqsoft.expression.mfn.sequence.Conj");
		addMemberFunction("conj", "com.raqsoft.expression.mfn.op.AttachConj");
		addMemberFunction("union", "com.raqsoft.expression.mfn.sequence.Union");
		addMemberFunction("diff", "com.raqsoft.expression.mfn.sequence.Diff");
		addMemberFunction("isect", "com.raqsoft.expression.mfn.sequence.Isect");
		addMemberFunction("xunion", "com.raqsoft.expression.mfn.sequence.Xunion");
		addMemberFunction("merge", "com.raqsoft.expression.mfn.sequence.Merge");
		addMemberFunction("run", "com.raqsoft.expression.mfn.sequence.Run");
		addMemberFunction("run", "com.raqsoft.expression.mfn.op.AttachRun");
		addMemberFunction("run", "com.raqsoft.expression.mfn.record.Run");
		addMemberFunction("calc", "com.raqsoft.expression.mfn.sequence.Calc");
		addMemberFunction("pos", "com.raqsoft.expression.mfn.sequence.Pos");
		addMemberFunction("contain", "com.raqsoft.expression.mfn.sequence.Contain");
		addMemberFunction("pseg", "com.raqsoft.expression.mfn.sequence.PSeg");
		addMemberFunction("segp", "com.raqsoft.expression.mfn.sequence.Segp");
		addMemberFunction("pmin", "com.raqsoft.expression.mfn.sequence.PMin");
		addMemberFunction("pmax", "com.raqsoft.expression.mfn.sequence.PMax");
		addMemberFunction("ptop", "com.raqsoft.expression.mfn.sequence.PTop");
		addMemberFunction("top", "com.raqsoft.expression.mfn.sequence.Top");
		addMemberFunction("pselect", "com.raqsoft.expression.mfn.sequence.PSelect");
		addMemberFunction("psort", "com.raqsoft.expression.mfn.sequence.PSort");
		addMemberFunction("select", "com.raqsoft.expression.mfn.sequence.Select");
		addMemberFunction("select", "com.raqsoft.expression.mfn.op.AttachSelect");
		addMemberFunction("sort", "com.raqsoft.expression.mfn.sequence.Sort");
		addMemberFunction("rvs", "com.raqsoft.expression.mfn.sequence.Rvs");
		addMemberFunction("swap", "com.raqsoft.expression.mfn.sequence.Swap");
		addMemberFunction("shift", "com.raqsoft.expression.mfn.sequence.Shift");
		addMemberFunction("pad", "com.raqsoft.expression.mfn.sequence.Pad");
		addMemberFunction("lookup", "com.raqsoft.expression.mfn.sequence.Lookup");
		addMemberFunction("sumif", "com.raqsoft.expression.mfn.sequence.Sumif");
		addMemberFunction("countif", "com.raqsoft.expression.mfn.sequence.Countif");
		addMemberFunction("avgif", "com.raqsoft.expression.mfn.sequence.Avgif");
		addMemberFunction("minif", "com.raqsoft.expression.mfn.sequence.Minif");
		addMemberFunction("maxif", "com.raqsoft.expression.mfn.sequence.Maxif");
		addMemberFunction("iterate", "com.raqsoft.expression.mfn.sequence.Iterate");
		addMemberFunction("iterate", "com.raqsoft.expression.mfn.cursor.Iterate");
		addMemberFunction("iterate", "com.raqsoft.expression.mfn.channel.Iterate");
		addMemberFunction("fno", "com.raqsoft.expression.mfn.record.FieldNo");
		addMemberFunction("fno", "com.raqsoft.expression.mfn.sequence.FieldNo");
		addMemberFunction("field", "com.raqsoft.expression.mfn.record.FieldValue");
		addMemberFunction("field", "com.raqsoft.expression.mfn.sequence.FieldValue");
		addMemberFunction("fname", "com.raqsoft.expression.mfn.record.FieldName");
		addMemberFunction("fname", "com.raqsoft.expression.mfn.sequence.FieldName");
		addMemberFunction("to", "com.raqsoft.expression.mfn.sequence.To");
		addMemberFunction("pivot", "com.raqsoft.expression.mfn.sequence.Pivot");
		
		addMemberFunction("r", "com.raqsoft.expression.mfn.RowField");
		addMemberFunction("median", "com.raqsoft.expression.mfn.sequence.Median");
		
		// 修改
		addMemberFunction("modify", "com.raqsoft.expression.mfn.sequence.Modify");
		addMemberFunction("modify", "com.raqsoft.expression.mfn.record.Modify");
		addMemberFunction("reset", "com.raqsoft.expression.mfn.sequence.Reset");
		addMemberFunction("reset", "com.raqsoft.expression.mfn.cursor.Reset");
		addMemberFunction("insert", "com.raqsoft.expression.mfn.sequence.Insert");
		addMemberFunction("delete", "com.raqsoft.expression.mfn.sequence.Delete");
		addMemberFunction("paste", "com.raqsoft.expression.mfn.table.Paste");
		addMemberFunction("record", "com.raqsoft.expression.mfn.sequence.RecordValue");
		addMemberFunction("record", "com.raqsoft.expression.mfn.record.RecordValue");
		addMemberFunction("rename", "com.raqsoft.expression.mfn.table.Rename");
		addMemberFunction("rename", "com.raqsoft.expression.mfn.op.AttachRename");
		addMemberFunction("rename", "com.raqsoft.expression.mfn.vdb.Rename");
		addMemberFunction("alter", "com.raqsoft.expression.mfn.table.Alter");

		// 产生
		addMemberFunction("create", "com.raqsoft.expression.mfn.sequence.Create");
		addMemberFunction("create", "com.raqsoft.expression.mfn.record.Create");
		addMemberFunction("new", "com.raqsoft.expression.mfn.sequence.New");
		addMemberFunction("new", "com.raqsoft.expression.mfn.op.AttachNew");
		addMemberFunction("derive", "com.raqsoft.expression.mfn.sequence.Derive");
		addMemberFunction("derive", "com.raqsoft.expression.mfn.op.AttachDerive");
		addMemberFunction("penum", "com.raqsoft.expression.mfn.sequence.PEnum");
		addMemberFunction("align", "com.raqsoft.expression.mfn.sequence.Align");
		addMemberFunction("enum", "com.raqsoft.expression.mfn.sequence.Enum");
		addMemberFunction("group", "com.raqsoft.expression.mfn.sequence.Group");
		addMemberFunction("group", "com.raqsoft.expression.mfn.op.AttachGroup");
		addMemberFunction("groups", "com.raqsoft.expression.mfn.sequence.Groups");
		addMemberFunction("groups", "com.raqsoft.expression.mfn.cursor.Groups");
		addMemberFunction("groups", "com.raqsoft.expression.mfn.channel.Groups");
		addMemberFunction("groupi", "com.raqsoft.expression.mfn.sequence.Groupi");
		addMemberFunction("news", "com.raqsoft.expression.mfn.sequence.News");
		addMemberFunction("news", "com.raqsoft.expression.mfn.op.AttachNews");

		addMemberFunction("pfind", "com.raqsoft.expression.mfn.sequence.PFind");
		addMemberFunction("find", "com.raqsoft.expression.mfn.sequence.Find");
		addMemberFunction("v", "com.raqsoft.expression.mfn.Value");

		// 映射
		addMemberFunction("key", "com.raqsoft.expression.mfn.record.Key");
		addMemberFunction("keys", "com.raqsoft.expression.mfn.table.Keys");
		addMemberFunction("switch", "com.raqsoft.expression.mfn.sequence.SwitchFK");
		addMemberFunction("switch", "com.raqsoft.expression.mfn.op.AttachSwitch");
		addMemberFunction("index", "com.raqsoft.expression.mfn.table.Index");
		addMemberFunction("prior", "com.raqsoft.expression.mfn.record.Prior");
		addMemberFunction("nodes", "com.raqsoft.expression.mfn.sequence.Nodes");

		addMemberFunction("array", "com.raqsoft.expression.mfn.record.Array");
		addMemberFunction("array", "com.raqsoft.expression.mfn.sequence.Array");
		addMemberFunction("regex", "com.raqsoft.expression.mfn.string.Regex");
		addMemberFunction("regex", "com.raqsoft.expression.mfn.sequence.Regex");
		addMemberFunction("regex", "com.raqsoft.expression.mfn.op.AttachRegex");
		addMemberFunction("concat", "com.raqsoft.expression.mfn.sequence.Concat");

		// 简单存储
		addFunction("filename", "com.raqsoft.expression.fn.FileName");
		addFunction("directory", "com.raqsoft.expression.fn.Directory");
		addFunction("file", "com.raqsoft.expression.fn.CreateFile");
		addFunction("movefile", "com.raqsoft.expression.fn.MoveFile");
		addFunction("httpfile", "com.raqsoft.expression.fn.CreateHttpFile");
		addFunction("httpupload", "com.raqsoft.expression.fn.Http_Upload");
		addMemberFunction("read", "com.raqsoft.expression.mfn.file.Read");
		addMemberFunction("write", "com.raqsoft.expression.mfn.file.Write");
		addMemberFunction("export", "com.raqsoft.expression.mfn.file.Export");
		addMemberFunction("export", "com.raqsoft.expression.mfn.sequence.Export");
		addMemberFunction("import", "com.raqsoft.expression.mfn.file.Import");
		addMemberFunction("import", "com.raqsoft.expression.mfn.string.Import");
		addMemberFunction("name", "com.raqsoft.expression.mfn.file.Name");
		addMemberFunction("exists", "com.raqsoft.expression.mfn.file.Exists");
		addMemberFunction("size", "com.raqsoft.expression.mfn.file.Size");
		addMemberFunction("date", "com.raqsoft.expression.mfn.file.Date");
		addMemberFunction("property", "com.raqsoft.expression.mfn.file.Property");
		addMemberFunction("property", "com.raqsoft.expression.mfn.string.Property");
		addMemberFunction("iselect", "com.raqsoft.expression.mfn.file.ISelect");

		// excel
		addMemberFunction("xlsexport", "com.raqsoft.expression.mfn.file.XlsExport");
		addMemberFunction("xlsexport", "com.raqsoft.expression.mfn.xo.XlsExport");
		addMemberFunction("xlsimport", "com.raqsoft.expression.mfn.file.XlsImport");
		addMemberFunction("xlsimport", "com.raqsoft.expression.mfn.xo.XlsImport");
		addMemberFunction("xlsopen", "com.raqsoft.expression.mfn.file.XlsOpen");
		addMemberFunction("xlswrite", "com.raqsoft.expression.mfn.file.XlsWrite");
		addMemberFunction("xlsclose", "com.raqsoft.expression.mfn.xo.XlsClose");
		addMemberFunction("xlscell", "com.raqsoft.expression.mfn.xo.XlsCell");
		
		addFunction("T", "com.raqsoft.expression.fn.T");
		addFunction("cellname", "com.raqsoft.expression.fn.CellName");

		addMemberFunction("htmlparse", "com.raqsoft.expression.mfn.string.HTMLParse");
		addMemberFunction("close", "com.raqsoft.expression.mfn.Close");
		
		// 数据库
		addFunction("connect", "com.raqsoft.expression.fn.Connect");
		addMemberFunction("commit", "com.raqsoft.expression.mfn.db.Commit");
		addMemberFunction("rollback", "com.raqsoft.expression.mfn.db.Rollback");
		addMemberFunction("rollback", "com.raqsoft.expression.mfn.file.Rollback");
		addMemberFunction("rollback", "com.raqsoft.expression.mfn.file.FileGroupRollback");
		addMemberFunction("query", "com.raqsoft.expression.mfn.db.Query");
		addMemberFunction("query", "com.raqsoft.expression.mfn.file.Query");
		addMemberFunction("execute", "com.raqsoft.expression.mfn.db.Execute");
		addMemberFunction("proc", "com.raqsoft.expression.mfn.db.Proc");
		addMemberFunction("error", "com.raqsoft.expression.mfn.db.Error");
		addMemberFunction("update", "com.raqsoft.expression.mfn.db.Update");
		addMemberFunction("isolate", "com.raqsoft.expression.mfn.db.Isolate");
		addMemberFunction("savepoint", "com.raqsoft.expression.mfn.db.SavePoint");
		
		// 数据仓库
		addFunction("vdbase", "com.raqsoft.expression.fn.VDBase");
		addMemberFunction("begin", "com.raqsoft.expression.mfn.vdb.Begin");
		addMemberFunction("commit", "com.raqsoft.expression.mfn.vdb.Commit");
		addMemberFunction("rollback", "com.raqsoft.expression.mfn.vdb.Rollback");
		addMemberFunction("home", "com.raqsoft.expression.mfn.vdb.Home");
		addMemberFunction("path", "com.raqsoft.expression.mfn.vdb.Path");
		addMemberFunction("lock", "com.raqsoft.expression.mfn.vdb.Lock");
		addMemberFunction("list", "com.raqsoft.expression.mfn.vdb.List");
		addMemberFunction("load", "com.raqsoft.expression.mfn.vdb.Load");
		addMemberFunction("date", "com.raqsoft.expression.mfn.vdb.Date");
		addMemberFunction("save", "com.raqsoft.expression.mfn.vdb.Save");
		addMemberFunction("move", "com.raqsoft.expression.mfn.vdb.Move");
		addMemberFunction("read", "com.raqsoft.expression.mfn.vdb.Read");
		addMemberFunction("write", "com.raqsoft.expression.mfn.vdb.Write");
		addMemberFunction("update", "com.raqsoft.expression.mfn.vdb.Update");
		addMemberFunction("saveblob", "com.raqsoft.expression.mfn.vdb.SaveBlob");
		addMemberFunction("retrieve", "com.raqsoft.expression.mfn.vdb.Retrive");
		addMemberFunction("archive", "com.raqsoft.expression.mfn.vdb.Archive");
		addMemberFunction("purge", "com.raqsoft.expression.mfn.vdb.Purge");
		addMemberFunction("copy", "com.raqsoft.expression.mfn.vdb.Copy");
		
		// 游标
		addMemberFunction("cursor", "com.raqsoft.expression.mfn.db.CreateCursor");
		addMemberFunction("cursor", "com.raqsoft.expression.mfn.file.CreateCursor");
		addMemberFunction("cursor", "com.raqsoft.expression.mfn.sequence.CreateCursor");
		addMemberFunction("cursor", "com.raqsoft.expression.mfn.cursor.CreateCursor");
		addMemberFunction("mcursor", "com.raqsoft.expression.mfn.sequence.MCursor");
		addMemberFunction("mcursor", "com.raqsoft.expression.mfn.cursor.MCursor");
		addMemberFunction("fetch", "com.raqsoft.expression.mfn.cursor.Fetch");
		addMemberFunction("fetch", "com.raqsoft.expression.mfn.channel.Fetch");
		addMemberFunction("skip", "com.raqsoft.expression.mfn.cursor.Skip");
		addMemberFunction("groupx", "com.raqsoft.expression.mfn.cursor.Groupx");
		addMemberFunction("groupx", "com.raqsoft.expression.mfn.channel.Groupx");
		addMemberFunction("groupn", "com.raqsoft.expression.mfn.op.AttachGroupn");
		addMemberFunction("sortx", "com.raqsoft.expression.mfn.cursor.Sortx");
		addMemberFunction("sortx", "com.raqsoft.expression.mfn.channel.Sortx");
		addMemberFunction("join", "com.raqsoft.expression.mfn.op.AttachJoin");
		addMemberFunction("join", "com.raqsoft.expression.mfn.sequence.JoinFK");
		addMemberFunction("joinx", "com.raqsoft.expression.mfn.cursor.Joinx");
		addMemberFunction("joinx", "com.raqsoft.expression.mfn.sequence.Joinx");
		addMemberFunction("joinx", "com.raqsoft.expression.mfn.channel.Joinx");
		addMemberFunction("mergex", "com.raqsoft.expression.mfn.cursor.Mergex");
		addMemberFunction("mergex", "com.raqsoft.expression.mfn.sequence.Mergex");
		addMemberFunction("conjx", "com.raqsoft.expression.mfn.sequence.Conjx");
		addMemberFunction("total", "com.raqsoft.expression.mfn.cursor.Total");
		addMemberFunction("total", "com.raqsoft.expression.mfn.channel.Total");


		addFunction("xjoinx", "com.raqsoft.expression.fn.XJoinx");
		addFunction("joinx", "com.raqsoft.expression.fn.Joinx");
		addFunction("cursor", "com.raqsoft.expression.fn.CreateCursor");
		addFunction("channel", "com.raqsoft.expression.fn.CreateChannel");
		addMemberFunction("push", "com.raqsoft.expression.mfn.op.AttachPush");
		addMemberFunction("result", "com.raqsoft.expression.mfn.channel.Result");
		
		// 仓库
		addFunction("memory", "com.raqsoft.expression.fn.parallel.Memory");
		addMemberFunction("row", "com.raqsoft.expression.mfn.TableRow");
		addMemberFunction("dup", "com.raqsoft.expression.mfn.table.Dup");
		addMemberFunction("dup", "com.raqsoft.expression.mfn.cluster.Dup");
		addMemberFunction("attach", "com.raqsoft.expression.mfn.dw.Attach");
		addMemberFunction("attach", "com.raqsoft.expression.mfn.cluster.Attach");
		addMemberFunction("append", "com.raqsoft.expression.mfn.dw.Append");
		addMemberFunction("update", "com.raqsoft.expression.mfn.dw.Update");
		addMemberFunction("update", "com.raqsoft.expression.mfn.dw.UpdateMemoryTable");
		addMemberFunction("delete", "com.raqsoft.expression.mfn.dw.Delete");
		addMemberFunction("index", "com.raqsoft.expression.mfn.dw.Index");
		addMemberFunction("memory", "com.raqsoft.expression.mfn.dw.Memory");
		addMemberFunction("cursor", "com.raqsoft.expression.mfn.dw.CreateCursor");
		addMemberFunction("import", "com.raqsoft.expression.mfn.dw.Import");
		addMemberFunction("new", "com.raqsoft.expression.mfn.dw.New");
		addMemberFunction("news", "com.raqsoft.expression.mfn.dw.News");
		addMemberFunction("derive", "com.raqsoft.expression.mfn.dw.Derive");
		addMemberFunction("icursor", "com.raqsoft.expression.mfn.dw.Icursor");
		addMemberFunction("cgroups", "com.raqsoft.expression.mfn.dw.Cgroups");
		addMemberFunction("find", "com.raqsoft.expression.mfn.dw.Find");
		addMemberFunction("create", "com.raqsoft.expression.mfn.file.Create");
		addMemberFunction("create", "com.raqsoft.expression.mfn.dw.Create");
		addMemberFunction("open", "com.raqsoft.expression.mfn.file.Open");
		addMemberFunction("reset", "com.raqsoft.expression.mfn.file.Reset");
		addMemberFunction("create", "com.raqsoft.expression.mfn.file.FileGroupCreate");
		addMemberFunction("reset", "com.raqsoft.expression.mfn.file.FileGroupReset");
		addMemberFunction("open", "com.raqsoft.expression.mfn.file.FileGroupOpen");
		addMemberFunction("cuboid", "com.raqsoft.expression.mfn.dw.CreateCuboid");
		addMemberFunction("rename", "com.raqsoft.expression.mfn.dw.Rename");
		addMemberFunction("alter", "com.raqsoft.expression.mfn.dw.Alter");
		
		//虚表
		addMemberFunction("import", "com.raqsoft.expression.mfn.pseudo.Import");
		addMemberFunction("cursor", "com.raqsoft.expression.mfn.pseudo.CreateCursor");
		//addMemberFunction("append", "com.raqsoft.expression.mfn.pseudo.Append");
		//addMemberFunction("groups", "com.raqsoft.expression.mfn.pseudo.Groups");
		
		//集群
		addMemberFunction("create", "com.raqsoft.expression.mfn.cluster.Create");
		addMemberFunction("open", "com.raqsoft.expression.mfn.cluster.Open");
		addMemberFunction("append", "com.raqsoft.expression.mfn.cluster.Append");
		addMemberFunction("update", "com.raqsoft.expression.mfn.cluster.Update");
		addMemberFunction("delete", "com.raqsoft.expression.mfn.cluster.Delete");
		addMemberFunction("index", "com.raqsoft.expression.mfn.cluster.Index");
		addMemberFunction("index", "com.raqsoft.expression.mfn.cluster.MemoryIndex");
		addMemberFunction("cuboid", "com.raqsoft.expression.mfn.cluster.CreateCuboid");
		addMemberFunction("reset", "com.raqsoft.expression.mfn.cluster.Reset");
		addMemberFunction("memory", "com.raqsoft.expression.mfn.cursor.Memory");
		addMemberFunction("memory", "com.raqsoft.expression.mfn.cluster.Memory");
		addMemberFunction("cursor", "com.raqsoft.expression.mfn.cluster.CreateCursor");
		addMemberFunction("cursor", "com.raqsoft.expression.mfn.cluster.CreateMemoryCursor");
		addMemberFunction("new", "com.raqsoft.expression.mfn.cluster.New");
		addMemberFunction("news", "com.raqsoft.expression.mfn.cluster.News");
		addMemberFunction("derive", "com.raqsoft.expression.mfn.cluster.Derive");
		addMemberFunction("icursor", "com.raqsoft.expression.mfn.cluster.Icursor");
		addMemberFunction("cgroups", "com.raqsoft.expression.mfn.cluster.Cgroups");
		
		// 统计图
		addFunction("canvas", "com.raqsoft.expression.fn.CreateCanvas");
		addMemberFunction("plot", "com.raqsoft.expression.mfn.canvas.Plot");
		addMemberFunction("draw", "com.raqsoft.expression.mfn.canvas.Draw");
		addMemberFunction("hlink", "com.raqsoft.expression.mfn.canvas.HLink");

		// 时间日期函数
		addFunction("age", "com.raqsoft.expression.fn.datetime.Age");
		addFunction("datetime", "com.raqsoft.expression.fn.datetime.DateTime");
		addFunction("day", "com.raqsoft.expression.fn.datetime.Day");
		addFunction("hour", "com.raqsoft.expression.fn.datetime.Hour");
		addFunction("minute", "com.raqsoft.expression.fn.datetime.Minute");
		addFunction("month", "com.raqsoft.expression.fn.datetime.Month");
		addFunction("now", "com.raqsoft.expression.fn.datetime.Now");
		addFunction("second", "com.raqsoft.expression.fn.datetime.Second");
		addFunction("millisecond", "com.raqsoft.expression.fn.datetime.Millisecond");
		addFunction("date", "com.raqsoft.expression.fn.datetime.ToDate");
		addFunction("time", "com.raqsoft.expression.fn.datetime.ToTime");
		addFunction("year", "com.raqsoft.expression.fn.datetime.Year");
		addFunction("periods", "com.raqsoft.expression.fn.datetime.Period");
		addFunction("interval", "com.raqsoft.expression.fn.datetime.Interval");
		addFunction("elapse", "com.raqsoft.expression.fn.datetime.Elapse");
		addFunction("days", "com.raqsoft.expression.fn.datetime.Days");
		addFunction("pdate", "com.raqsoft.expression.fn.datetime.PDate");
		addFunction("deq", "com.raqsoft.expression.fn.datetime.DateEqual");
		addFunction("workday", "com.raqsoft.expression.fn.datetime.WorkDay");
		addFunction("workdays", "com.raqsoft.expression.fn.datetime.WorkDays");

		// 数学函数
		addFunction("abs", "com.raqsoft.expression.fn.math.Abs");
		addFunction("and","com.raqsoft.expression.fn.math.And");
		addFunction("acos", "com.raqsoft.expression.fn.math.Arccos");
		addFunction("acosh", "com.raqsoft.expression.fn.math.Arccosh");
		addFunction("asin", "com.raqsoft.expression.fn.math.Arcsin");
		addFunction("asinh", "com.raqsoft.expression.fn.math.Arcsinh");
		addFunction("atan", "com.raqsoft.expression.fn.math.Arctan");
		addFunction("atanh", "com.raqsoft.expression.fn.math.Arctanh");
		addFunction("bin","com.raqsoft.expression.fn.math.Bin");
		addFunction("bits","com.raqsoft.expression.fn.math.Bits");
		addFunction("ceil", "com.raqsoft.expression.fn.math.Ceiling");
		addFunction("combin","com.raqsoft.expression.fn.math.Combin");
		addFunction("cos", "com.raqsoft.expression.fn.math.Cos");
		addFunction("cosh", "com.raqsoft.expression.fn.math.Cosh");
		addFunction("digits","com.raqsoft.expression.fn.math.Digits");
		addFunction("exp", "com.raqsoft.expression.fn.math.Exp");
		addFunction("fact", "com.raqsoft.expression.fn.math.Fact");
		addFunction("floor", "com.raqsoft.expression.fn.math.Floor");
		addFunction("gcd","com.raqsoft.expression.fn.math.Gcd");
		addFunction("hash","com.raqsoft.expression.fn.math.Hash");
		addFunction("hex","com.raqsoft.expression.fn.math.Hex");
		addFunction("inf","com.raqsoft.expression.fn.math.Inf");
		addFunction("lcm","com.raqsoft.expression.fn.math.Lcm");
		addFunction("lg", "com.raqsoft.expression.fn.math.Loga");
		addFunction("ln", "com.raqsoft.expression.fn.math.Log");
		addFunction("not","com.raqsoft.expression.fn.math.Not");
		addFunction("or","com.raqsoft.expression.fn.math.Or");
		addFunction("permut","com.raqsoft.expression.fn.math.Permut");
		addFunction("pi", "com.raqsoft.expression.fn.math.Pi");
		addFunction("power", "com.raqsoft.expression.fn.math.Pow");
		addFunction("product","com.raqsoft.expression.fn.math.Product");
		addFunction("rand", "com.raqsoft.expression.fn.math.Rand");
		addFunction("round", "com.raqsoft.expression.fn.math.Round");
		addFunction("shift","com.raqsoft.expression.fn.math.Shift");
		addFunction("sign", "com.raqsoft.expression.fn.math.Sign");
		addFunction("sin", "com.raqsoft.expression.fn.math.Sin");
		addFunction("sinh", "com.raqsoft.expression.fn.math.Sinh");
		addFunction("sqrt", "com.raqsoft.expression.fn.math.Sqrt");
		addFunction("tan", "com.raqsoft.expression.fn.math.Tan");
		addFunction("tanh", "com.raqsoft.expression.fn.math.Tanh");
		addFunction("xor","com.raqsoft.expression.fn.math.Xor");
		
		// 字符串函数
		addFunction("fill", "com.raqsoft.expression.fn.string.Fill");
		addFunction("left", "com.raqsoft.expression.fn.string.Left");
		addFunction("len", "com.raqsoft.expression.fn.string.Len");
		addFunction("like", "com.raqsoft.expression.fn.string.Like");
		addFunction("lower", "com.raqsoft.expression.fn.string.Lower");
		addFunction("mid", "com.raqsoft.expression.fn.string.Mid");
		addFunction("pos", "com.raqsoft.expression.fn.string.Pos");
		addFunction("replace", "com.raqsoft.expression.fn.string.Replace");
		addFunction("right", "com.raqsoft.expression.fn.string.Right");
		addFunction("trim", "com.raqsoft.expression.fn.string.Trim");
		addFunction("upper", "com.raqsoft.expression.fn.string.Upper");
		addFunction("pad", "com.raqsoft.expression.fn.string.Pad");
		addFunction("rands", "com.raqsoft.expression.fn.string.Rands");
		addFunction("concat", "com.raqsoft.expression.fn.string.Concat");
		addFunction("urlencode", "com.raqsoft.expression.fn.string.URLEncode");
		addFunction("base64", "com.raqsoft.expression.fn.string.Base64");
		addFunction("md5", "com.raqsoft.expression.fn.string.MD5Encrypt");
		addFunction("substr", "com.raqsoft.expression.fn.string.SubString");
		
		addMemberFunction("words", "com.raqsoft.expression.mfn.string.Words");
		addMemberFunction("split", "com.raqsoft.expression.mfn.string.Split");
		addMemberFunction("sqlparse", "com.raqsoft.expression.mfn.string.SQLParse");
		addMemberFunction("sqltranslate", "com.raqsoft.expression.mfn.string.SQLTranslate");

		// 类型转换函数
		addFunction("ifv", "com.raqsoft.expression.fn.convert.IfVariable");
		addFunction("ifa", "com.raqsoft.expression.fn.convert.IfSequence");
		addFunction("ifr", "com.raqsoft.expression.fn.convert.IfRecord");
		addFunction("ift", "com.raqsoft.expression.fn.convert.IfTable");
		addFunction("ifdate", "com.raqsoft.expression.fn.convert.IfDate");
		addFunction("iftime", "com.raqsoft.expression.fn.convert.IfTime");
		addFunction("ifnumber", "com.raqsoft.expression.fn.convert.IfNumber");
		addFunction("ifstring", "com.raqsoft.expression.fn.convert.IfString");
		addFunction("isalpha", "com.raqsoft.expression.fn.convert.IsAlpha");
		addFunction("isdigit", "com.raqsoft.expression.fn.convert.IsDigit");
		addFunction("islower", "com.raqsoft.expression.fn.convert.IsLower");
		addFunction("isupper", "com.raqsoft.expression.fn.convert.IsUpper");

		addFunction("bool", "com.raqsoft.expression.fn.convert.ToBool");
		addFunction("int", "com.raqsoft.expression.fn.convert.ToInteger");
		addFunction("long", "com.raqsoft.expression.fn.convert.ToLong");
		addFunction("float", "com.raqsoft.expression.fn.convert.ToDouble");
		addFunction("number", "com.raqsoft.expression.fn.convert.ToNumber");
		addFunction("string", "com.raqsoft.expression.fn.convert.ToString");
		addFunction("decimal", "com.raqsoft.expression.fn.convert.ToBigDecimal");
		addFunction("asc", "com.raqsoft.expression.fn.convert.ToAsc");
		addFunction("char", "com.raqsoft.expression.fn.convert.ToChar");
		addFunction("rgb", "com.raqsoft.expression.fn.convert.RGB");
		addFunction("chn", "com.raqsoft.expression.fn.convert.ToChinese");
		addFunction("parse", "com.raqsoft.expression.fn.convert.Parse");
		addFunction("format", "com.raqsoft.expression.fn.convert.Format");
		addFunction("json", "com.raqsoft.expression.fn.convert.Json");
		addFunction("xml", "com.raqsoft.expression.fn.convert.Xml");
		
		//financial function
		addFunction("Fsln","com.raqsoft.expression.fn.financial.Sln");
		addFunction("Fsyd","com.raqsoft.expression.fn.financial.Syd");
		addFunction("Fdb","com.raqsoft.expression.fn.financial.Db");
		addFunction("Fddb","com.raqsoft.expression.fn.financial.Ddb");
		addFunction("Fvdb","com.raqsoft.expression.fn.financial.Vdb");
		addFunction("Fnper","com.raqsoft.expression.fn.financial.Nper");
		addFunction("Fpmt","com.raqsoft.expression.fn.financial.Pmt");
		addFunction("Fv","com.raqsoft.expression.fn.financial.Fv");
		addFunction("Frate","com.raqsoft.expression.fn.financial.Rate");
		addFunction("Fnpv","com.raqsoft.expression.fn.financial.Npv");
		addFunction("Firr","com.raqsoft.expression.fn.financial.Irr");
		addFunction("Fmirr","com.raqsoft.expression.fn.financial.Mirr");
		addFunction("Faccrint","com.raqsoft.expression.fn.financial.Accrint");
		addFunction("Faccrintm","com.raqsoft.expression.fn.financial.Accrintm");
		addFunction("Fintrate","com.raqsoft.expression.fn.financial.Intrate");
		addFunction("Freceived","com.raqsoft.expression.fn.financial.Received");
		addFunction("Fprice","com.raqsoft.expression.fn.financial.Price");
		addFunction("Fdisc","com.raqsoft.expression.fn.financial.Disc");
		addFunction("Fyield","com.raqsoft.expression.fn.financial.Yield");
		addFunction("Fcoups","com.raqsoft.expression.fn.financial.Coups");
		addFunction("Fcoupcd","com.raqsoft.expression.fn.financial.Coupcd");
		addFunction("Fduration","com.raqsoft.expression.fn.financial.Duration");
		
		//algebra function
		addFunction("var","com.raqsoft.expression.fn.algebra.Var");
		addFunction("mse","com.raqsoft.expression.fn.algebra.Mse");
		addFunction("mae","com.raqsoft.expression.fn.algebra.Mae");
		addFunction("cov","com.raqsoft.expression.fn.algebra.Cov");
		addFunction("covm","com.raqsoft.expression.fn.algebra.Covm");
		addFunction("dis","com.raqsoft.expression.fn.algebra.Distance");
		addFunction("dism","com.raqsoft.expression.fn.algebra.Dism");
		addFunction("I","com.raqsoft.expression.fn.algebra.Identity");
		addFunction("mul","com.raqsoft.expression.fn.algebra.Mul");
		addFunction("transpose","com.raqsoft.expression.fn.algebra.Transpose");
		addFunction("inverse","com.raqsoft.expression.fn.algebra.Inverse");
		addFunction("det","com.raqsoft.expression.fn.algebra.Det");
		addFunction("rankm","com.raqsoft.expression.fn.algebra.Rankm");
		addFunction("linefit","com.raqsoft.expression.fn.algebra.Linefit");
		addFunction("polyfit","com.raqsoft.expression.fn.algebra.Polyfit");
		addFunction("norm","com.raqsoft.expression.fn.algebra.Normalize");
		addFunction("pearson","com.raqsoft.expression.fn.algebra.Pearson");
		addFunction("spearman","com.raqsoft.expression.fn.algebra.Spearman");
		addFunction("pca","com.raqsoft.expression.fn.algebra.PCA");
		addFunction("pls","com.raqsoft.expression.fn.algebra.PLS");
		addFunction("sg","com.raqsoft.expression.fn.algebra.SavizkgGolag");
		addFunction("ones","com.raqsoft.expression.fn.algebra.Ones");
		addFunction("zeros","com.raqsoft.expression.fn.algebra.Zeros");
		addFunction("eye","com.raqsoft.expression.fn.algebra.Eye");
		addFunction("mfind","com.raqsoft.expression.fn.algebra.MFind");
		addFunction("msum","com.raqsoft.expression.fn.algebra.MSum");
		addFunction("mcumsum","com.raqsoft.expression.fn.algebra.MCumsum");
		addFunction("mmean","com.raqsoft.expression.fn.algebra.MMean");
		addFunction("mnorm","com.raqsoft.expression.fn.algebra.MNormalize");
		addFunction("mstd","com.raqsoft.expression.fn.algebra.MStd");
		addFunction("norminv","com.raqsoft.expression.fn.algebra.Norminv");
		addFunction("lasso","com.raqsoft.expression.fn.algebra.Lasso");
		addFunction("ridge","com.raqsoft.expression.fn.algebra.Ridge");
		addFunction("elasticnet","com.raqsoft.expression.fn.algebra.ElasticNet");
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
		Pattern p = Pattern.compile("com/raqsoft/lib/(\\w+)/functions.properties");
		
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
