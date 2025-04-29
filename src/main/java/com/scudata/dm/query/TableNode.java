package com.scudata.dm.query;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import com.scudata.cellset.ICellSet;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Env;
import com.scudata.dm.FileObject;
import com.scudata.dm.Sequence;
import com.scudata.dm.UserUtils;
import com.scudata.dm.cursor.BFileCursor;
import com.scudata.dm.cursor.ConjxCursor;
import com.scudata.dm.cursor.FileCursor;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.cursor.MemoryCursor;
import com.scudata.dm.query.Select.Exp;
import com.scudata.dm.query.utils.FileUtil;
import com.scudata.dw.ComTable;
import com.scudata.dw.PhyTable;
import com.scudata.excel.ExcelTool;
import com.scudata.excel.XlsxSImporter;
import com.scudata.expression.Expression;
import com.scudata.expression.mfn.dw.CreateCursor;
import com.scudata.resources.EngineMessage;
import com.scudata.resources.ParseMessage;
import com.scudata.util.JSONUtil;

public class TableNode extends QueryBody {
	final public static int TYPE_BIN = 0;
	final public static int TYPE_TXT = 1;
	final public static int TYPE_CSV = 2;
	final public static int TYPE_XLS = 3;
	final public static int TYPE_XLSX = 4;
	final public static int TYPE_JSON = 5;
	final public static int TYPE_CTX = 6;
	//final public static int TYPE_CS = 10;
	//final public static int TYPE_SEQUENCE = 11;
	
	private String name;
	private ICursor cursor;
	private Sequence sequence;
	private ArrayList<FileObject> fileList;
	private int type;
	private DataStruct struct;

	public TableNode(Select select, Sequence sequence, String name, String aliasName) {
		this.select = select;
		this.name = name;
		this.aliasName = aliasName;
		setData(sequence);
	}
	
	public TableNode(Select select, ICursor cs, String name, String aliasName) {
		this.select = select;
		this.name = name;
		this.aliasName = aliasName;
		setData(cs);
	}
	
	public TableNode(Select select, String fileName, String aliasName) {
		this.select = select;
		this.name = fileName;
		this.aliasName = aliasName;

		WithItem with = select.getWithItem(fileName);
		if (with != null) {
			Sequence data = with.getData();
			setData(data);
			return;
		}
		
		Context ctx = select.getContext();
		String password = null;
		int index = fileName.lastIndexOf(':'); // 密码分隔符
		
		if(index != -1) {
			password = fileName.substring(index + 1).trim();
			if(password.startsWith("'") && password.endsWith("'")) {
				password = password.substring(1, password.length() - 1);
			}
			
			fileName = fileName.substring(0, index).trim();
		}
		
		int dotIndex = fileName.lastIndexOf('.');
		if(dotIndex == -1) {
			Expression exp = new Expression(select.getCellSet(), ctx, fileName);
			Object obj = exp.calculate(ctx);
			if(obj instanceof ICursor) {
				setData((ICursor)obj);
			} else if(obj instanceof Sequence) {
				setData((Sequence)obj);
			} else {
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":scanFrom, 不支持的表变量类型");
			}
			
			return;
		}
		
		String fileType = fileName.substring(dotIndex);
		File appHome = ctx.getAppHome();
		String mainPath = Env.getMainPath();
		File []files = null;
		
		if (appHome != null) {
			String strFile = appHome.getAbsolutePath();
			if (mainPath != null && mainPath.length() > 0) {
				strFile += "/" + mainPath;
			}
			
			strFile += "/" + fileName;
			files = FileUtil.getFiles(strFile);
			if (files != null) {
				setData(files, fileType);
				return;
			}
			
			if (Env.getPaths() != null) {
				for (String path : Env.getPaths()) {
					strFile = appHome.getAbsolutePath() + "/" + path + "/" + fileName;
					files = FileUtil.getFiles(strFile);
					if (files != null) {
						setData(files, fileType);
						return;
					}
				}
			}
		} else {
			files = FileUtil.getFiles(fileName);
			if (files != null) {
				setData(files, fileType);
				return;
			}
			
			if (mainPath != null && mainPath.length() > 0) {
				String strFile = mainPath + "/" + fileName;
				files = FileUtil.getFiles(strFile);
				if (files != null) {
					setData(files, fileType);
					return;
				}
			}
			
			if (Env.getPaths() != null) {
				for (String path : Env.getPaths()) {
					String strFile = path + "/" + fileName;
					files = FileUtil.getFiles(strFile);
					if (files != null) {
						setData(files, fileType);
						return;
					}
				}
			}
		}
		
		MessageManager mm = EngineMessage.get();
		throw new RQException(mm.getMessage("file.fileNotExist", fileName));
	}
	
	public QueryBody getQueryBody(String tableName) {
		if (aliasName != null) {
			if (Select.isEquals(aliasName, tableName)) {
				return this;
			} else {
				return null;
			}
		} else {
			if (Select.isEquals(name, tableName)) {
				return this;
			} else {
				return null;
			}
		}
	}
	
	public QueryBody getQueryBody(String tableName, String fieldName) {
		if (tableName != null) {
			if (aliasName != null) {
				if (Select.isEquals(aliasName, tableName)) {
					return this;
				} else {
					return null;
				}
			} else {
				if (Select.isEquals(name, tableName)) {
					return this;
				} else {
					return null;
				}
			}
		} else {
			if (getFieldIndex(fieldName) != -1) {
				return this;
			} else {
				return null;
			}
		}
	}
	
	private int getFieldIndex(String fieldName) {
		DataStruct ds = getDataStruct();
		if (ds == null) {
			return -1;
		}
		
		return Select.getFieldIndex(ds, fieldName);
	}
	
	private void setData(Sequence sequence) {
		this.sequence = sequence;
		struct = sequence.dataStruct();
	}
	
	private void setData(File []files, String fileType) {
		fileList = new ArrayList<FileObject>();
		Context ctx = select.getContext();
		for (File file : files) {
			fileList.add(new FileObject(file.getAbsolutePath(), null, "s", ctx));
		}
		
		fileType = fileType.toLowerCase();
		if(fileType.equals(".btx")) {
			type = TableNode.TYPE_BIN;
		} else if(fileType.equals(".txt")) {
			type = TableNode.TYPE_TXT;
		} else if(fileType.equals(".csv")) {
			type = TableNode.TYPE_CSV;
		} else if(fileType.equals(".xls")) {
			type = TableNode.TYPE_XLS;
		} else if(fileType.equals(".xlsx")) {
			type = TableNode.TYPE_XLSX;
		} else if(fileType.equals(".json")) {
			type = TableNode.TYPE_JSON;
		} else if(fileType.equals(".ctx")) {
			type = TableNode.TYPE_CTX;
		} else {
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + ":scanFrom, 异常的表名(注意表名不能为关键字或以数字开头):" + name);
		}
	}
	
	private void setData(ICursor cs) {
		cursor = cs;
		struct = cs.getDataStruct();
		
		if(struct == null) {
			Sequence data = cs.peek(1);
			if(data != null) {
				struct = data.dataStruct();
			}
		}
	}
	
	public ArrayList<FileObject> getFiles() {
		return fileList;
	}
	
	public ICursor getDataCursor() {
		if (cursor != null) {
			return cursor;
		} else if (sequence != null) {
			cursor = new MemoryCursor(sequence);
			return cursor;
		}

		int fileCount = fileList.size();
		Context ctx = select.getContext();
		ICursor cursor;
		
		if (type == TableNode.TYPE_BIN) {
			if (fileCount == 1) {
				FileObject fo = fileList.get(0);
				cursor = new BFileCursor(fo, null, null, ctx);
			} else {
				ICursor []cursors = new ICursor[fileCount];
				for (int i = 0; i < fileCount; ++i) {
					FileObject fo = fileList.get(i);
					cursors[i] = new BFileCursor(fo, null, null, ctx);
				}
				
				cursor = new ConjxCursor(cursors);
			}
		} else if (type == TableNode.TYPE_TXT) {
			if (fileCount == 1) {
				FileObject fo = fileList.get(0);
				cursor = new FileCursor(fo, 1, 1, null, "t", ctx);
			} else {
				ICursor []cursors = new ICursor[fileCount];
				for (int i = 0; i < fileCount; ++i) {
					FileObject fo = fileList.get(i);
					cursors[i] = new FileCursor(fo, 1, 1, null, "t", ctx);
				}
				
				cursor = new ConjxCursor(cursors);
			}
		} else if (type == TableNode.TYPE_CSV) {
			if (fileCount == 1) {
				FileObject fo = fileList.get(0);
				cursor = new FileCursor(fo, 1, 1, null, "tc", ctx);
			} else {
				ICursor []cursors = new ICursor[fileCount];
				for (int i = 0; i < fileCount; ++i) {
					FileObject fo = fileList.get(i);
					cursors[i] = new FileCursor(fo, 1, 1, null, "tc", ctx);
				}
				
				cursor = new ConjxCursor(cursors);
			}
		} else if (type == TableNode.TYPE_XLS) {
			InputStream in = null;
			BufferedInputStream bis = null;
			Sequence data = null;
			
			try {
				for (int i = 0; i < fileCount; ++i) {
					in = fileList.get(i).getInputStream();
					bis = new BufferedInputStream(in, Env.FILE_BUFSIZE);
					ExcelTool importer = new ExcelTool(in, false, null);
					Sequence seq = (Sequence)importer.fileXlsImport("t");
					if (data == null) {
						data = seq;
					} else {
						data.append(seq);
					}
				}
			} catch (IOException e) {
				throw new RQException(e.getMessage(), e);
			} finally {
				try {
					if (in != null) {
						in.close();
					}
				} catch (IOException e) {
					throw new RQException(e.getMessage(), e);
				}
				
				try {
					if (bis != null) {
						bis.close();
					}
				} catch (IOException e) {
					throw new RQException(e.getMessage(), e);
				}
			}
			
			cursor = new MemoryCursor(data);
		} else if (type == TableNode.TYPE_XLSX) {
			if (fileCount == 1) {
				FileObject fo = fileList.get(0);
				XlsxSImporter importer = new XlsxSImporter(fo, null, 0, 0, new Integer(1), "t");
				cursor = UserUtils.newCursor(importer, "t");
			} else {
				ICursor []cursors = new ICursor[fileCount];
				for (int i = 0; i < fileCount; ++i) {
					FileObject fo = fileList.get(i);
					XlsxSImporter importer = new XlsxSImporter(fo, null, 0, 0, new Integer(1), "t");
					cursors[i] = UserUtils.newCursor(importer, "t");
				}
				
				cursor = new ConjxCursor(cursors);
			}
		} else if (type == TableNode.TYPE_JSON) {
			Sequence data = null;
			for (int i = 0; i < fileCount; ++i) {
				FileObject fo = fileList.get(i);
				char []chars;
				
				try {
					String str = (String)fo.read(0, -1, null);
					chars = str.toCharArray();
				} catch (IOException e) {
					throw new RQException(e.getMessage(), e);
				}
				
				Object result = JSONUtil.parseJSON(chars, 0, chars.length - 1);
				if (result instanceof Sequence) {
					if (data == null) {
						data = (Sequence)result;
					} else {
						data.append((Sequence)result);
					}
				} else {
					if (data == null) {
						data = new Sequence();
					}

					data.add(result);
				}
			}
			
			cursor = new MemoryCursor(data);
		} else { // ctx
			if (fileCount == 1) {
				FileObject fo = fileList.get(0);
				PhyTable phyTable = ComTable.openBaseTable(fo, ctx);
				cursor = CreateCursor.createCursor(phyTable, null, "x", ctx);
			} else {
				ICursor []cursors = new ICursor[fileCount];
				for (int i = 0; i < fileCount; ++i) {
					FileObject fo = fileList.get(i);
					PhyTable phyTable = ComTable.openBaseTable(fo, ctx);
					cursors[i] = CreateCursor.createCursor(phyTable, null, "x", ctx);
				}
				
				cursor = new ConjxCursor(cursors);
			}
		}
		
		setData(cursor);
		return cursor;
	}

	//这个是求原始表的数据结构而不是最终游标的
	public DataStruct getDataStruct() {
		if (struct == null) {
			getDataCursor();
		}
		
		return struct;
	}

	public Object getData(Exp where) {
		ICursor cs = getDataCursor();
		if (where != null && cs != null) {
			String expStr = where.toSPL();
			Context ctx = select.getContext();
			ICellSet cellSet = select.getCellSet();
			Expression exp = new Expression(cellSet, ctx, expStr);
			cs.select(null, exp, null, ctx);
		}
		
		return cs;
	}
}
