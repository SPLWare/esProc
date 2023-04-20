package com.scudata.dm;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import com.scudata.app.common.AppConsts;
import com.scudata.app.common.AppUtil;
import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.common.Escape;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.cursor.BFileCursor;
import com.scudata.dm.cursor.FileCursor;
import com.scudata.dm.cursor.ICursor;
import com.scudata.expression.Expression;
import com.scudata.resources.EngineMessage;
import com.scudata.util.CellSetUtil;
import com.scudata.util.Variant;

/**
 * 文件对象
 * file(...)函数的返回值
 * @author WangXiaoJun
 *
 */
public class FileObject implements Externalizable {
	public static final String TEMPFILE_PREFIX = "tmpdata"; // 长度必须大于3
	
	public static final int FILETYPE_TEXT = 0; // 文本
	public static final int FILETYPE_BINARY = 1; // 集文件
	public static final int FILETYPE_GROUPTABLE = 2; // 组表
	public static final int FILETYPE_GROUPTABLE_ROW = 3; // 行式组表
	public static final int LOCK_SLEEP_TIME = 100; // 锁休眠时间
	
	// {(byte)'\r', (byte)'\n'}; // 行结束标志
	// \r\n 或 \n 写的时候根据操作系统决定，读的时候需要两种都兼容
	public static final byte[] LINE_SEPARATOR = System.getProperty("line.separator").getBytes();
	public static final byte[] DM_LINE_SEPARATOR = new byte[] {'\r', '\n'};
	public static final byte[] COL_SEPARATOR = new byte[] {(byte)'\t'}; // 列间隔
	private static final int BLOCKCOUNT = 999; // 二进制文件块大小
	public static final String S_FIELDNAME = "_1"; // 导出序列时默认的字段名

	private String fileName; // 文件路径名
	private String charset; // 文本文件的字符集
	private String opt; // 选项

	private Integer partition; // 分区
	
	// 文件所在机器的ip和端口
	private String ip;
	private int port;
	
	private boolean isSimpleSQL; // 是否用于简单SQL
	
	transient private IFile file; // 对应的实际文件，可以是本地文件、远程文件、HDFS文件
	transient private Context ctx; // 计算上下文
	
	transient private boolean isRemoteFileWritable = false; // 远程文件是否可写
	
	// 仅供序列化
	public FileObject() {
	}

	/**
	 * 用另一个文件对象构建文件对象
	 * @param fo
	 */
	public FileObject(FileObject fo) {
		this.fileName = fo.fileName;
		this.charset = fo.charset;
		this.opt = fo.opt;
		this.partition = fo.partition;
		this.ip = fo.ip;
		this.port = fo.port;
		this.file = fo.file;
		this.ctx = fo.ctx;
	}

	/**
	 * 根据文件名构造一文件对象
	 * @param name 文件路径名
	 */
	public FileObject(String name) {
		this(name, null);
	}
	
	/**
	 * 根据文件名构造一文件对象
	 * @param name 文件路径名
	 * @param opt 选项
	 * @param ctx 计算上下文
	 */
	public FileObject(String name, String opt, Context ctx) {
		this(name, null, opt, ctx);
	}

	/**
	 * 构建远程文件对象
	 * @param name 文件路径名
	 * @param ip
	 * @param port
	 */
	public FileObject(String name, String ip, int port) {
		this(name, null);
		this.ip = ip;
		this.port = port;
	}

	/**
	 * 根据名字构造一文件对象
	 * @param name String
	 * @param opt String s：查找路径包含类路径并且只读，p：在path列表中找并且只读
	 */
	public FileObject(String name, String opt) {
		this(name, null, opt, null);
	}

	/**
	 * 根据文件构造一文件对象
	 * @param file IFile
	 * @param name 文件名
	 * @param cs 文本文件字符集
	 * @param opt 选项
	 */
	public FileObject(IFile file, String name, String cs, String opt) {
		this(name, cs, opt, null);
		this.file = file;
	}

	/**
	 * 根据名字构造一文件对象
	 * @param name String
	 * @param cs String 字符集
	 * @param opt String s：查找路径包含类路径并且只读，p：在path列表中找并且只读
	 * @param ctx Context 不需要时传null
	 */
	public FileObject(String name, String cs, String opt, Context ctx) {
		this.ctx = ctx;
		setOption(opt);
		setCharset(cs);

		// "remote://ip:port/…" 远程文件
		final String remotePrefix = "remote://";
		if (name.startsWith(remotePrefix)) {
			int start = remotePrefix.length();
			int index = name.indexOf(':', start);
			if (index == -1) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("file.fileNotExist", name));
			}

			this.ip = name.substring(start, index).trim();
			index++;
			int index2 = name.indexOf('/', index);
			if (index2 == -1) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("file.fileNotExist", name));
			}

			String port = name.substring(index, index2).trim();
			this.port = Integer.parseInt(port);
			this.fileName = name.substring(index2 + 1);
		} else {
			this.fileName = name;
			//this.ip = Env.getLocalHost();
			//this.port = Env.getLocalPort();
		}
	}

	/**
	 * 设置是否是简单SQL
	 * @param b true：是
	 */
	public void setIsSimpleSQL(boolean b) {
		isSimpleSQL = b;
	}
	
	/**
	 * 取是否是简单SQL
	 * @return true：是
	 */
	public boolean getIsSimpleSQL() {
		return isSimpleSQL;
	}
	
	/**
	 * 设置远程文件可写
	 */
	public void setRemoteFileWritable(){
		isRemoteFileWritable = true;
	}
	
	/**
	 * 设置计算上下文
	 * @param ctx
	 */
	public void setContext(Context ctx) {
		this.ctx = ctx;
	}
	
	/**
	 * 取创建文件时指定的选项
	 * @return
	 */
	public String getOption() {
		return opt;
	}

	/**
	 * 设置选项
	 * @param opt
	 */
	public void setOption(String opt) {
		this.opt = opt;
	}
	
	/**
	 * 设置文本文件字符集
	 * @param cs 字符集
	 */
	public void setCharset(String cs) {
		if (cs == null || cs.length() == 0) {
			charset = Env.getDefaultCharsetName();
		} else {
			charset = cs;
		}
	}
	
	/**
	 * 设置分区
	 * @param p
	 */
	public void setPartition(Integer p) {
		this.partition = p;
		this.file = null;
	}

	/**
	 * 取分区
	 * @return
	 */
	public Integer getPartition() {
		return partition;
	}

	/**
	 * 设置远程机器IP
	 * @param ip
	 */
	public void setIP(String ip) {
		this.ip = ip;
	}

	/**
	 * 取远程机器IP
	 * @return IP
	 */
	public String getIP() {
		return ip;
	}

	/**
	 * 设置远程机器端口
	 * @param port
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * 取远程机器端口
	 * @return 端口
	 */
	public int getPort() {
		return port;
	}

	/**
	 * 取文件名
	 * @return
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * 设置文件名
	 * @param name
	 */
	public void setFileName(String name) {
		this.fileName = name;
	}

	/**
	 * 取字符集
	 * @return
	 */
	public String getCharset() {
		return charset;
	}

	/**
	 * 返回文件名
	 * @return String
	 */
	public String toString() {
		return fileName;
	}

	/**
	 * 读入一个文件，形成排列返回
	 * @param opt String t：第一行为标题，b：二进制文件，c：写成逗号分隔的csv文件
	 * 	s：不拆分字段，读成单字段串构成的序表，i：结果集只有1列时返回成序列
	 * 	q：如果字段串外有引号则先剥离，包括标题部分，k：保留数据项两端的空白符，缺省将自动做trim
	 * @throws IOException
	 * @return Sequence
	 */
	public Sequence importSeries(String opt) throws IOException {
		ICursor cursor;
		if (opt != null && opt.indexOf('b') != -1) {
			cursor = new BFileCursor(this, null, opt, null);
		} else {
			cursor = new FileCursor(this, 0, -1, null, opt, null);
		}
		
		try {
			return cursor.fetch();
		} finally {
			cursor.close();
		}
	}

	/**
	 * 读入一个文件，形成排列返回
	 * @param segSeq int 段号，从1开始计数
	 * @param segCount int 分段数
	 * @param fields String[] 选出字段名
	 * @param types byte[] 选出字段类型，可空。参照com.scudata.common.Types
	 * @param s Object excel sheet名（序号）或列分隔符
	 * @param opt String t：第一行为标题，b：二进制文件，c：写成逗号分隔的csv文件
	 * 	s：不拆分字段，读成单字段串构成的序表，i：结果集只有1列时返回成序列
	 * 	q：如果字段串外有引号则先剥离，包括标题部分，k：保留数据项两端的空白符，缺省将自动做trim
	 * 	e：Fi在文件中不存在时将生成null，缺省将报错
	 * @param ctx Context
	 * @throws IOException
	 * @return Sequence
	 */
	public Sequence importSeries(int segSeq, int segCount, String []fields, byte[] types,
							  Object s, String opt) throws IOException {
		ICursor cursor;
		if (opt != null && opt.indexOf('b') != -1) {
			cursor = new BFileCursor(this, fields, segSeq, segCount, opt, null);
		} else {
			String sep;
			if (s instanceof String) {
				sep = (String)s;
			} else if (s == null) {
				sep = null;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("import" + mm.getMessage("function.paramTypeError"));
			}

			cursor = new FileCursor(this, segSeq, segCount, fields, types, sep, opt, null);
		}
		
		try {
			return cursor.fetch();
		} finally {
			cursor.close();
		}
	}

	/**
	 * 导入文本、xls、xlsx文件
	 * @param importer 可以是LineImporter、ExcelTool、ExcelXTool
	 * @param opt t：第一行为标题
	 * @return 序表
	 * @throws IOException
	 */
	public static Table import_x(ILineInput importer, String opt) throws IOException {
		Object []line = importer.readLine();
		if (line == null)return null;
		int fcount = line.length;
		if (fcount == 0)return null;

		Table table;
		if (opt != null && opt.indexOf('t') != -1) {
			String[] items = new String[fcount];
			for (int f = 0; f < fcount; ++f) {
				items[f] = Variant.toString(line[f]);
			}

			table = new Table(items);
		} else {
			String[] items = new String[fcount];
			table = new Table(items);

			BaseRecord r = table.newLast();
			for (int f = 0; f < fcount; ++f) {
				r.setNormalFieldValue(f, line[f]);
			}
		}

		while (true) {
			line = importer.readLine();
			if (line == null)break;

			int curLen = line.length;
			if (curLen > fcount) curLen = fcount;
			BaseRecord r = table.newLast();
			for (int f = 0; f < curLen; ++f) {
				r.setNormalFieldValue(f, line[f]);
			}
		}

		table.trimToSize();
		return table;
	}
	
	/**
	 * 取序列的序列成员的最大长度
	 * @param seq 序列组成的序列
	 * @return 最大长度
	 */
	public static int getMaxMemberCount(Sequence seq) {
		int len = seq.length();
		int maxCount = -1;
		for (int i = 1; i <= len; ++i) {
			Object obj = seq.getMem(i);
			if (obj instanceof Sequence) {
				int count = ((Sequence)obj).length();
				if (maxCount < count) {
					maxCount = count;
				}
			} else if (obj != null) {
				return -1;
			}
		}
		
		return maxCount;
	}

	/**
	 * 导出排列
	 * @param exporter 可以是LineExporter、ExcelTool、ExcelXTool
	 * @param series 排列
	 * @param exps 要导出的字段表达式，如果null则导出所有字段
	 * @param names 导出后的字段名
	 * @param bTitle 是否导出字段名
	 * @param ctx
	 * @throws IOException
	 */
	public static void export_x(ILineOutput exporter, Sequence series, Expression []exps,
							   String []names, boolean bTitle, Context ctx) throws IOException {
		if (exps == null) {
			int fcount = 1;
			DataStruct ds = series.dataStruct();
			if (ds == null) {
				int len = series.length();
				fcount = getMaxMemberCount(series);
				
				if (fcount < 1) {
					if (bTitle && len > 0) {
						exporter.writeLine(new String[]{S_FIELDNAME});
					}
					
					Object []lineObjs = new Object[1];
					for (int i = 1; i <= len; ++i) {
						lineObjs[0] = series.getMem(i);
						exporter.writeLine(lineObjs);
					}
				} else {
					// A是序列的序列时，生成无标题/字段名的多列文本
					Object []lineObjs = new Object[fcount];
					for (int i = 1; i <= len; ++i) {
						Sequence seq = (Sequence)series.getMem(i);
						if (seq == null) {
							for (int f = 0; f < fcount; ++f) {
								lineObjs[f] = null;
							}
						} else {
							seq.toArray(lineObjs);
							for (int f = seq.length(); f < fcount; ++f) {
								lineObjs[f] = null;
							}
						}

						exporter.writeLine(lineObjs);
					}
				}
			} else {
				fcount = ds.getFieldCount();
				if (bTitle) exporter.writeLine(ds.getFieldNames());
	
				Object []lineObjs = new Object[fcount];
				for (int i = 1, len = series.length(); i <= len; ++i) {
					BaseRecord r = (BaseRecord)series.getMem(i);
					Object []vals = r.getFieldValues();
					for (int f = 0; f < fcount; ++f) {
						if (vals[f] instanceof BaseRecord) {
							lineObjs[f] = ((BaseRecord)vals[f]).value();
						} else {
							lineObjs[f] = vals[f];
						}
					}
	
					exporter.writeLine(lineObjs);
				}
			}
		} else {
			ComputeStack stack = ctx.getComputeStack();
			Current current = new Current(series);
			stack.push(current);

			try {
				int fcount = exps.length;
				if (bTitle) {
					if (names == null) names = new String[fcount];
					series.getNewFieldNames(exps, names, "export");
					exporter.writeLine(names);
				}

				Object []lineObjs = new Object[fcount];
				for (int i = 1, len = series.length(); i <= len; ++i) {
					current.setCurrent(i);
					for (int f = 0; f < fcount; ++f) {
						lineObjs[f] = exps[f].calculate(ctx);
						if (lineObjs[f] instanceof BaseRecord) {
							lineObjs[f] = ((BaseRecord)lineObjs[f]).value();
						}
					}

					exporter.writeLine(lineObjs);
				}
			} finally {
				stack.pop();
			}
		}
	}

	/**
	 * 导出排列
	 * @param exporter 可以是LineExporter、ExcelTool、ExcelXTool
	 * @param cursor 游标
	 * @param exps 要导出的字段表达式，如果null则导出所有字段
	 * @param names 导出后的字段名
	 * @param bTitle 是否导出字段名
	 * @param ctx
	 * @throws IOException
	 */
	public static void export_x(ILineOutput exporter, ICursor cursor, Expression []exps,
							   String []names, boolean bTitle, Context ctx) throws IOException {
		Sequence table = cursor.fetch(BLOCKCOUNT);
		if (table == null || table.length() == 0) return;

		if (exps == null) {
			int fcount = 1;
			DataStruct ds = table.dataStruct();
			if (ds == null) {
				if (bTitle) {
					exporter.writeLine(new String[]{S_FIELDNAME});
				}
			} else {
				fcount = ds.getFieldCount();
				if (bTitle) {
					exporter.writeLine(ds.getFieldNames());
				}
			}
			
			Object []lineObjs = new Object[fcount];
			while (true) {
				if (ds == null) {
					for (int i = 1, len = table.length(); i <= len; ++i) {
						lineObjs[0] = table.getMem(i);
						exporter.writeLine(lineObjs);
					}
				} else {
					for (int i = 1, len = table.length(); i <= len; ++i) {
						BaseRecord r = (BaseRecord)table.getMem(i);
						Object []vals = r.getFieldValues();
						for (int f = 0; f < fcount; ++f) {
							if (vals[f] instanceof BaseRecord) {
								lineObjs[f] = ((BaseRecord)vals[f]).value();
							} else {
								lineObjs[f] = vals[f];
							}
						}

						exporter.writeLine(lineObjs);
					}
				}

				table = cursor.fetch(BLOCKCOUNT);
				if (table == null || table.length() == 0) {
					break;
				}
			}
		} else {
			int fcount = exps.length;
			Object []lineObjs = new Object[fcount];
			if (bTitle) {
				if (names == null) names = new String[fcount];
				table.getNewFieldNames(exps, names, "export");
				exporter.writeLine(names);
			}

			ComputeStack stack = ctx.getComputeStack();
			while (true) {
				Current current = new Current(table);
				stack.push(current);

				try {
					for (int i = 1, len = table.length(); i <= len; ++i) {
						current.setCurrent(i);
						for (int f = 0; f < fcount; ++f) {
							lineObjs[f] = exps[f].calculate(ctx);
							if (lineObjs[f] instanceof BaseRecord) {
								lineObjs[f] = ((BaseRecord)lineObjs[f]).value();
							}
						}

						exporter.writeLine(lineObjs);
					}
				} finally {
					stack.pop();
				}

				table = cursor.fetch(BLOCKCOUNT);
				if (table == null || table.length() == 0) {
					break;
				}
			}
		}
	}

	/**
	 * 将排列所有字段导出到文件中
	 * @param series Sequence
	 * @param opt String ｔ：导出标题，c：写成逗号分隔的csv文件，b：二进制文件，a：追加写
	 * @param s Object excel sheet名或列分隔符，空用默认的
	 */
	public void exportSeries(Sequence series, String opt, Object s) {
		exportSeries(series, null, null, opt, s, null);
	}

	/**
	 * 将排列的指定字段导出到文件中
	 * @param series Sequence
	 * @param exps Expression[] 要导出的值表达式，空表示导出所有字段
	 * @param names String[] 值表达式对应的名字，省略用值表达式串
	 * @param opt String t：导出标题，c：写成逗号分隔的csv文件，b：二进制文件，a：追加写
	 * @param s Object excel sheet名或列分隔符，空用默认的
	 * @param ctx Context
	 */
	public void exportSeries(Sequence series, Expression []exps,
							 String []names, String opt, Object s, Context ctx) {
		if (BFileWriter.isBtxOption(opt)) {
			if (opt.indexOf('w') != -1) {
				series = series.toTable();
			}
			
			BFileWriter writer = new BFileWriter(this, opt);
			writer.export(series, exps, names, ctx);
			return;
		}
		
		boolean isTitle = false, isCsv = false, isAppend = false, isQuote = false, isQuoteEscape = false;
		byte[] LINE_SEPARATOR = FileObject.LINE_SEPARATOR;

		if (opt != null) {
			if (opt.indexOf('t') != -1) isTitle = true;
			if (opt.indexOf('c') != -1) isCsv = true;
			if (opt.indexOf('a') != -1) isAppend = true;
			if (opt.indexOf('q') != -1) isQuote = true;
			if (opt.indexOf('o') != -1) {
				isQuote = true;
				isQuoteEscape = true;
			}

			if (opt.indexOf('w') != -1) {
				LINE_SEPARATOR = FileObject.DM_LINE_SEPARATOR;
			}
		}

		if (series == null) {
			if (!isAppend) delete();
			return;
		}

		OutputStream os;
		if (isAppend) {
			os = getBufferedOutputStream(true);
			if (size() > 0) {
				isTitle = false;
			} else {
				isAppend = false;
			}
		} else {
			isAppend = false;
			os = getBufferedOutputStream(false);
		}

		try {
			byte []colSeparator = COL_SEPARATOR;
			if (isCsv) colSeparator = new byte[]{(byte)','};

			if (s instanceof String) {
				String str = (String)s;
				if (str.length() > 0) {
					colSeparator = str.getBytes(charset);
				}
			} else if (s != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("import" + mm.getMessage("function.paramTypeError"));
			}

			LineExporter exporter = new LineExporter(os, charset, colSeparator, LINE_SEPARATOR, isAppend);
			exporter.setQuote(isQuote);
			if (isQuoteEscape) {
				exporter.setEscapeChar('"');
			}
			
			export_x(exporter, series, exps, names, isTitle, ctx);
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				os.close();
			} catch (IOException e) {
				throw new RQException(e.getMessage(), e);
			}
		}
	}
		
	/**
	 * 导出cursor到文本
	 * @param cursor ICursor
	 * @param exps Expression[] 要导出的值表达式，空表示导出所有字段
	 * @param names String[] 值表达式对应的名字，省略用值表达式串
	 * @param opt String ｔ：导出标题，c：写成逗号分隔的csv文件，b：二进制文件，a：追加写
	 * @param s Object 列分隔符
	 * @param ctx Context
	 */
	public void exportCursor(ICursor cursor, Expression []exps,
							 String []names, String opt, Object s, Context ctx) {
		if (BFileWriter.isBtxOption(opt)) {
			BFileWriter writer = new BFileWriter(this, opt);
			writer.export(cursor, exps, names, ctx);
			return;
		}
		
		boolean isTitle = false, isCsv = false, isAppend = false, isQuote = false, isQuoteEscape = false;
		byte[] LINE_SEPARATOR = FileObject.LINE_SEPARATOR;
		if (opt != null) {
			if (opt.indexOf('t') != -1) isTitle = true;
			if (opt.indexOf('c') != -1) isCsv = true;
			if (opt.indexOf('a') != -1) isAppend = true;
			if (opt.indexOf('q') != -1) isQuote = true;
			if (opt.indexOf('o') != -1) {
				isQuote = true;
				isQuoteEscape = true;
			}
			
			if (opt.indexOf('w') != -1) {
				LINE_SEPARATOR = FileObject.DM_LINE_SEPARATOR;
			}
		}
		
		if (cursor == null) {
			if (!isAppend) delete();
			return;
		}

		OutputStream os;
		if (isAppend && size() > 0) {
			os = getBufferedOutputStream(true);
			if (size() > 0) {
				isTitle = false;
			} else {
				isAppend = false;
			}
		} else {
			isAppend = false;
			os = getBufferedOutputStream(false);
		}

		try {
			byte []colSeparator = COL_SEPARATOR;
			if (isCsv) colSeparator = new byte[]{(byte)','};

			if (s instanceof String) {
				String str = (String)s;
				if (str.length() > 0) {
					colSeparator = str.getBytes(charset);
				}
			} else if (s != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("import" + mm.getMessage("function.paramTypeError"));
			}

			LineExporter exporter = new LineExporter(os, charset, colSeparator, LINE_SEPARATOR, isAppend);
			exporter.setQuote(isQuote);
			if (isQuoteEscape) {
				exporter.setEscapeChar('"');
			}
			
			export_x(exporter, cursor, exps, names, isTitle, ctx);
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				os.close();
			} catch (IOException e) {
				throw new RQException(e.getMessage(), e);
			}
		}
	}
	
	/**
	 * 按组导出游标，生成可按组分段的二进制文件
	 * @param cursor 游标
	 * @param exps 导出字段值表达式
	 * @param names 导出字段名
	 * @param gexp 分组表达式，只比较相邻
	 * @param opt
	 * @param ctx
	 */
	public void export_g(ICursor cursor, Expression []exps,
			 String []names, Expression gexp, String opt, Context ctx) {
		BFileWriter writer = new BFileWriter(this, opt);
		writer.export(cursor, exps, names, gexp, ctx);
	}
	
	/**
	 * 从输入流的起始位置读到结束位置，不会保存读入的内容，用于测试硬盘速度
	 * @param in 输入流
	 * @param start 起始位置
	 * @param end 结束位置
	 * @return 读的字节数
	 * @throws IOException
	 */
	private static Object read0(InputStream in, long start, long end) throws IOException {
		byte []buf = new byte[Env.FILE_BUFSIZE];
		if (start != 0) {
			long total = 0;
			long cur = 0;
			while ((total<start) && ((cur = in.skip(start-total)) > 0)) {
				total += cur;
			}

			if (total < start) return null;
		}

		long size = 0;
		int cur;
		if (end > 0) {
			while ((cur = in.read(buf)) != -1) {
				size += cur;
				if (size + start >= end) break;
			}
		} else {
			while ((cur = in.read(buf)) != -1) {
				size += cur;
			}
		}

		return new Long(size);
	}

	/**
	 * 读取输入流中的所有字节
	 * @param in InputStream
	 * @param start long 起始位置，从0开始计数，包括
	 * @param end long 结束位置，从0开始计数，包括，-1表示结尾
	 * @throws IOException
	 * @return byte[]
	 */
	private static byte[] read(InputStream in, long start, long end) throws IOException {
		if (start != 0) {
			long total = 0;
			long cur = 0;
			while ((total<start) && ((cur = in.skip(start-total)) > 0)) {
				total += cur;
			}

			if (total < start) return null;
		}

		if (end > 0) {
			int total = 0;
			int count = (int)(end - start + 1);
			ByteArrayOutputStream out = new ByteArrayOutputStream(count);
			byte[] buf = new byte[4096];

			while(total < count) {
				int rest = count - total;
				int cur = in.read(buf, 0, rest < 4096 ? rest : 4096);
				if (cur == -1) break;

				out.write(buf, 0, cur);
				total += cur;
			}

			return out.toByteArray();
		} else {
			ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
			int count = 0;
			byte[] buf = new byte[4096];
			while( (count=in.read(buf)) != -1 ) {
				out.write(buf, 0, count);
			}

			return out.toByteArray();
		}
	}

	/**
	 * 把文件内容转成字符串返回
	 * @param start long 起始位置，从0开始计数，包括
	 * @param end long 结束位置，从0开始计数，包括，-1表示结尾
	 * @param opt String n：返回成串序列，b：读成byte数组，v：读成值
	 * @throws IOException
	 * @return Object
	 */
	public Object read(long start, long end, String opt) throws IOException {
		if (end > 0 && end < start) return null;

		boolean isMultiLine = false, isBinary = false, isValue = false, isTest = false;
		if (opt != null) {
			if (opt.indexOf('n') != -1) isMultiLine = true;
			if (opt.indexOf('b') != -1) isBinary = true;
			if (opt.indexOf('v') != -1) isValue = true;
			if (opt.indexOf('0') != -1) isTest = true;
		}

		InputStream in = getInputStream();
		try {
			if (isTest) {
				return read0(in, start, end);
			} else if (isBinary) {
				return read(in, start, end);
			} else if (isMultiLine) {
				InputStreamReader isr = new InputStreamReader(in, charset);
				BufferedReader br = new BufferedReader(isr);
				Sequence retSeries = new Sequence();
				for (; ;) {
					String str = br.readLine();
					if (str == null) break;
					if (isValue) {
						retSeries.add(Variant.parse(str, false));
					} else {
						retSeries.add(str);
					}
				}

				retSeries.trimToSize();
				return retSeries;
			} else {
				byte []bts = read(in, start, end);
				if (bts == null) {
					return null;
				}

				// 去掉bom头
				String str;
				if (start == 0 && bts.length > 3 && bts[0] == (byte)0xEF && bts[1] == (byte)0xBB && bts[2] == (byte)0xBF) {
					charset = "UTF-8";
					str = new String(bts, 3, bts.length - 3, charset);
				} else {
					str = new String(bts, charset);
				}
				
				if (isValue) {
					return Variant.parse(str, false);
				} else {
					return str;
				}
			}
		} finally {
			in.close();
		}
	}

	/**
	 * 把对象写入到文件中
	 * @param obj Objcet
	 * @param opt String a：追加写，b：写成byte数组
	 * @throws IOException
	 */
	public void write(Object obj, String opt) throws IOException {
		boolean isAppend = false, isBinary = false;
		byte[] LINE_SEPARATOR = FileObject.LINE_SEPARATOR;
		
		if (opt != null) {
			if (opt.indexOf('a') != -1) isAppend = true;
			if (opt.indexOf('b') != -1) isBinary = true;
			if (opt.indexOf('w') != -1) {
				LINE_SEPARATOR = FileObject.DM_LINE_SEPARATOR;
			}
		}

		OutputStream os = getBufferedOutputStream(isAppend);

		try {
			if (isBinary) {
				if (obj instanceof byte[]) {
					os.write((byte[])obj);
				} else {
					String str = Variant.toString(obj);
					if (str != null) os.write(str.getBytes(charset));
				}
			} else {
				if (isAppend && size() > 0) {
					os.write(LINE_SEPARATOR);
				}

				if (obj instanceof Sequence) {
					Sequence series = (Sequence)obj;
					int len = series.length();
					if (len > 0) {
						Object mem = series.getMem(1);
						String str = Variant.toString(mem);
						if (str != null) os.write(str.getBytes(charset));
					}

					for (int i = 2; i <= len; ++i) {
						os.write(LINE_SEPARATOR);
						Object mem = series.getMem(i);
						String str = Variant.toString(mem);
						if (str != null) os.write(str.getBytes(charset));
					}
				} else {
					String str = Variant.toString(obj);
					if (str != null) os.write(str.getBytes(charset));
				}
			}
		} finally {
			os.close();
		}
	}

	/**
	 * 取输入流
	 * @return InputStream
	 */
	public InputStream getInputStream() {
		return getFile().getInputStream();
	}

	/**
	 * 取按块读入的输入流
	 * @return
	 */
	public BlockInputStream getBlockInputStream() {
		return getBlockInputStream(Env.FILE_BUFSIZE);
	}

	/**
	 * 取按块读入的输入流
	 * @param bufSize 块大小
	 * @return BlockInputStream
	 */
	public BlockInputStream getBlockInputStream(int bufSize) {
		InputStream is = getInputStream();
		return new BlockInputStream(is, bufSize);
	}

	/**
	 * 取输出流
	 * @param isAppend 是否追加写
	 * @return OutputStream
	 */
	public OutputStream getOutputStream(boolean isAppend) {
		OutputStream os = getFile().getOutputStream(isAppend);
		
		if (os instanceof FileOutputStream) {
			FileOutputStream fos = (FileOutputStream)os;
			FileLock lock = null;
			if (opt == null || opt.indexOf('a') == -1) {
				try {
					lock = fos.getChannel().tryLock();
				} catch (Exception e) {
				}
				
				if (lock == null) {
					throw new RQException("另一个程序已锁定文件的一部分，进程无法访问");
				}
			} else {
				FileChannel channel = fos.getChannel();
				while (true) {
					try {
						channel.lock();
						break;
					} catch (OverlappingFileLockException e) {
						try {
							Thread.sleep(LOCK_SLEEP_TIME);
						} catch (InterruptedException ie) {
						}
					} catch (Exception e) {
						throw new RQException(e.getMessage(), e);
					}
				}
			}
		}

		return os;
	}

	/**
	 * 取带缓存的输出流
	 * @param isAppend 是否追加写
	 * @return OutputStream
	 */
	public OutputStream getBufferedOutputStream(boolean isAppend) {
		OutputStream os = getOutputStream(isAppend);
		return new BufferedOutputStream(os, Env.FILE_BUFSIZE);
	}
	
	/**
	 * 取可更改输出位置的输出流
	 * @param isAppend 是否追加写
	 * @return RandomOutputStream
	 */
	public RandomOutputStream getRandomOutputStream(boolean isAppend) {
		RandomOutputStream os = getFile().getRandomOutputStream(isAppend);
		
		boolean lock = false;
		try {
			if (opt == null || opt.indexOf('a') == -1) {
				lock = os.tryLock();
			} else {
				lock = os.lock();
			}
		} catch (Exception e) {
		}
		
		if (!lock) {
			try {
				os.close();
			} catch (IOException e) {
			}
			
			throw new RQException("另一个程序已锁定文件的一部分，进程无法访问");
		}

		return os;
	}

	/**
	 * 取本地文件
	 * @return LocalFile
	 */
	public LocalFile getLocalFile() {
		if (partition == null) {
			return new LocalFile(fileName, opt, ctx);
		} else {
			return new LocalFile(fileName, opt, partition);
		}
	}
	
	/**
	 * 取文件
	 * @return IFile
	 */
	public IFile getFile() {
		if (file != null) {
			return file;
		}

		//if (ip == null || (ip.equals(Env.getLocalHost()) && port == Env.getLocalPort())) {
		if (ip == null) {
			if (partition == null) {
				file = new LocalFile(fileName, opt, ctx);
			} else {
				file = new LocalFile(fileName, opt, partition);
			}
		} else {
			String pathFile = fileName;
			RemoteFile rf = new RemoteFile(ip, port, pathFile, partition);
			rf.setOpt(opt);
			if( isRemoteFileWritable ){
				// 增加是否可写属性
				rf.setWritable();
			}
			
			file = rf;
		}
		
		if (opt != null && opt.indexOf('i') != -1) {
			file = new MemoryFile(file);
		}
		
		return file;
	}

	/**
	 * 返回是否是远程文件
	 * @return true：是远程文件，false：不是远程文件
	 */
	public boolean isRemoteFile() {
		return ip != null;// && !(ip.equals(Env.getLocalHost()) && port == Env.getLocalPort());
	}
	
	/**
	 * 返回文件是否存在
	 * @return boolean true：存在，false：不存在
	 */
	public boolean isExists() {
		return getFile().exists();
	}

	/**
	 * 返回文件的大小
	 * @return
	 */
	public long size() {
		return getFile().size();
	}

	/**
	 * 返回文件最后修改时间，如果是URL文件或jar包里的文件则返回-1
	 * @return Date
	 */
	public Timestamp lastModified() {
		long date = getFile().lastModified();
		if (date > 0) {
			return new Timestamp(date);
		} else {
			return null;
		}
	}

	/**
	 * 删除文件
	 * @return true：成功，false：失败
	 */
	public boolean delete() {
		return getFile().delete();
	}
	
	/**
	 * 删除目录及其子目录
	 * @return true：成功，false：失败
	 */
	public boolean deleteDir() {
		return getFile().deleteDir();
	}

	/**
	 *
	 * @param dest String
	 * @param opt String y：目标文件已存在时强行复制缺省将失败，c：复制，
	 * 					 p：目标文件是相对目录是相对于主目录，默认是相对于源文件的父目录
	 * @return boolean
	 */
	public boolean move(String dest, String opt) {
		return getFile().move(dest, opt);
	}

	/**
	 * 取属性文件的属性
	 * @param key String 键值
	 * @param opt String v：做parse
	 * @throws IOException
	 * @return Object
	 */
	public Object getProperty(String key, String opt) throws IOException {
		boolean isValue = opt != null && opt.indexOf('v') != -1;
		
		// httpfile(...).property("header")
		if (file instanceof HttpFile) {
			String str = ((HttpFile)file).getResponseHeader(key);
			if (isValue) {
				return Variant.parse(str);
			} else {
				return str;
			}
		}
		
		InputStream in = getInputStream();
		try {
			Properties properties = new Properties();
			properties.load(new InputStreamReader(in, charset));
			String str = properties.getProperty(key);
			if (isValue) {
				return Variant.parse(str);
			} else {
				return str;
			}
		} finally {
			in.close();
		}
	}

	/**
	 * 把属性文件读成以{"name", "value"}为结构的序表
	 * @param opt 选项
	 * @return 序表
	 * @throws IOException
	 */
	public Table getProperties(String opt) throws IOException {
		InputStream in = null;
		try{
			in = getInputStream();
			Properties properties = new Properties();
			properties.load(new InputStreamReader(in, charset));
			return getProperties(properties,opt);
		} finally {
			if(in!=null)
				in.close();
		}
	}
	
	/**
	 * 把属读成以{"name", "value"}为结构的序表
	 * @param properties Properties
	 * @param opt 选项
	 * @return 序表
	 * @throws IOException
	 */
	public static Table getProperties(Properties properties, String opt) {
		boolean isValue = opt != null && opt.indexOf('v') != -1;
		boolean isQuote = opt != null && opt.indexOf('q') != -1;
		Table table = new Table(new String[] {"name", "value"});
		Iterator<Map.Entry<Object, Object>> itr = properties.entrySet().iterator();
		
		if (isValue || isQuote) {
			while (itr.hasNext()) {
				Map.Entry<Object, Object> entry = itr.next();
				BaseRecord r = table.newLast();
				r.setNormalFieldValue(0, entry.getKey());
				Object v = entry.getValue();
				if (v instanceof String) {
					if(isValue){
						v = Variant.parse((String)v);
					}else{
						v = Escape.addEscAndQuote((String)v);
					}
				}

				r.setNormalFieldValue(1, v);
			}
		} else {
			while (itr.hasNext()) {
				Map.Entry<Object, Object> entry = itr.next();
				BaseRecord r = table.newLast();
				r.setNormalFieldValue(0, entry.getKey());
				r.setNormalFieldValue(1, entry.getValue());
			}
		}

		return table;
	}

	/**
	 * 在文件对象所在的目录创建一个临时文件
	 * @param prefix 前缀
	 * @return 临时文件路径名
	 */
	public String createTempFile(String prefix) {
		return getFile().createTempFile(prefix);
	}

	/**
	 * 在文件对象所在的目录创建一个临时文件
	 * @return 临时文件路径名
	 */
	public String createTempFile() {
		return getFile().createTempFile(TEMPFILE_PREFIX);
	}
	
	/**
	 * 在文件对象所在的目录创建一个临时文件
	 * @return FileObject 临时文件对象
	 */
	public static FileObject createTempFileObject() {
		String path = Env.getTempPath();
		if (path != null && path.length() > 0) {
			FileObject fo = new FileObject(path);
			return new FileObject(fo.createTempFile(TEMPFILE_PREFIX));
		} else {
			try {
				File tmpFile = File.createTempFile(TEMPFILE_PREFIX, "");
				return new FileObject(tmpFile.getAbsolutePath());
			} catch (IOException e) {
				throw new RQException(e.getMessage(), e);
			}
		}
	}

	/**
	 * 把dfx文件读成网格对象
	 * @return PgmCellSet
	 */
	public PgmCellSet readPgmCellSet() {
		InputStream is = getInputStream();
		try {
			is = new BufferedInputStream(is);
			if (fileName.toLowerCase().endsWith("." + AppConsts.FILE_SPL)) {
				return AppUtil.readSPL(is);
			} else {
				return CellSetUtil.readPgmCellSet(is);
			}
		} catch (Exception e) {
			if (e instanceof RQException) {
				throw (RQException)e;
			} else {
				throw new RQException(e);
			}
		} finally {
			try {
				is.close();
			} catch (IOException e) {
			}
		}
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeByte(1); // 版本号

		out.writeObject(fileName);
		out.writeObject(charset);
		out.writeObject(opt);
		out.writeObject(partition);
		out.writeObject(ip);
		out.writeInt(port);
		out.writeBoolean(isSimpleSQL);
	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		in.readByte(); // 版本号

		fileName = (String) in.readObject();
		charset = (String) in.readObject();
		opt = (String) in.readObject();
		partition = (Integer) in.readObject();
		ip = (String)in.readObject();
		port = in.readInt();
		isSimpleSQL = in.readBoolean();
	}
	
	/**
	 * 设置文件大小
	 * @param size
	 */
	public void setFileSize(long size) {
		IFile file = getFile();
		if (file instanceof LocalFile) {
			((LocalFile)file).setFileSize(size);
		}
	}
	
	/**
	 * 取文件类型
	 * @return 返回FILETYPE_TEXT、FILETYPE_BINARY、FILETYPE_SIMPLETABLE、FILETYPE_GROUPTABLE
	 */
	public int getFileType() {
		InputStream is = null;
		try {
			// 组表(rqgt)、简表(rqst)、二进制集文件(rqtbx)
			is = getFile().getInputStream();
			if (is.read() == 'r' && is.read() == 'q') {
				int b = is.read();
				if (b == 'd') {
					if (is.read() == 'w' && is.read() == 'g' && is.read() == 't') {
						b = is.read();
						if (b == 'c') {
							return FILETYPE_GROUPTABLE;
						} else if (b == 'r') {
							return FILETYPE_GROUPTABLE_ROW;
						}
					}
				} else if (b == 't') {
					if (is.read() == 'b' && is.read() == 'x') {
						return FILETYPE_BINARY;
					}
				}
			}
		} catch (Exception e) {
		} finally {
			try {
				if (is != null) is.close();
			} catch(IOException e) {
			}
		}

		return FILETYPE_TEXT;
	}
	
	/**
	 * 创建基于文件的简单SQL查询
	 * @return FileObject
	 */
	public static FileObject createSimpleQuery() {
		FileObject fo = new FileObject();
		fo.setIsSimpleSQL(true);
		return fo;
	}
	
	/**
	 * 取随机访问文件对象，如果不支持则返回null
	 * @return RandomAccessFile
	 */
	public RandomAccessFile getRandomAccessFile() {
		return getFile().getRandomAccessFile();
	}
}
