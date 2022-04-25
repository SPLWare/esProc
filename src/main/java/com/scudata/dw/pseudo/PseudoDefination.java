package com.scudata.dw.pseudo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.BFileReader;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.FileGroup;
import com.scudata.dm.FileObject;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dw.GroupTable;
import com.scudata.dw.ITableMetaData;
import com.scudata.expression.Expression;
import com.scudata.resources.EngineMessage;

//用于定义虚表的属性
public class PseudoDefination {
	public static final String PD_FILE = "file";
	public static final String PD_ZONE = "zone";
	public static final String PD_DATE = "date";
	public static final String PD_USER = "user";
	public static final String PD_COLUMN = "column";
	public static final String PD_VAR = "var";
	
	private Object file;//文件名或多个文件名的序列
	private Sequence zone;//组表分区号列表
	private String date;//分列字段
	private String user;//帐户字段
	private String var;//序表/内表/集群内表变量名
	private List<PseudoColumn> columns;//部分特殊字段定义
	private List<ITableMetaData> tables;//存所有文件的table对象
	private Sequence memoryTable;//内存虚表的序表对象
	private FileObject fileObject;//集文件对象
	private DataStruct ds;//集文件结构
	private boolean isBFile = false;
	private String[] sortedFields;//排序字段
	
	public PseudoDefination() {
		
	}
	
	public PseudoDefination(Record pd, Context ctx) {
		file = getFieldValue(pd, PD_FILE);
		zone = (Sequence) getFieldValue(pd, PD_ZONE);
		date = (String) getFieldValue(pd, PD_DATE);
		user = (String) getFieldValue(pd, PD_USER);
		var = (String) getFieldValue(pd, PD_VAR);
		Sequence seq = (Sequence) getFieldValue(pd, PD_COLUMN);
		if (seq != null) {
			columns = new ArrayList<PseudoColumn>();
			int size = seq.length();
			for (int i = 1; i <= size; i++) {
				Record rec = (Record) seq.get(i);
				columns.add(new PseudoColumn(rec));
			}
		}
		if (file == null && var == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("file.fileNotExist", "NULL"));
		}
		
		if (file != null) {
			if (!checkBFile(ctx)) {
				parseFileToTable(ctx);
			}
			sortedFields = getAllSortedColNames();
		}
		
		if (var != null) {
			memoryTable = (Sequence) new Expression(var).calculate(ctx);
		}
	}
	
	public Object getFile() {
		return file;
	}

	public void setFile(Object file) {
		this.file = file;
	}

	public Sequence getZone() {
		return zone;
	}

	public void setZone(Sequence zone) {
		this.zone = zone;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}
	
	public String getVar() {
		return var;
	}

	public void setVar(String var) {
		this.var = var;
	}
	
	public List<PseudoColumn> getColumns() {
		return columns;
	}
	
	public void setColumns(List<PseudoColumn> columns) {
		this.columns = columns;
	}


	public List<ITableMetaData> getTables() {
		return tables;
	}

	public void setTables(List<ITableMetaData> tables) {
		this.tables = tables;
	}

	public Sequence getMemoryTable() {
		return memoryTable;
	}

	public void setMemoryTable(Sequence memoryTable) {
		this.memoryTable = memoryTable;
	}

	public static Object getFieldValue(Record pd, String name) {
		int index = pd.getFieldIndex(name);
		if (index != -1) {
			return pd.getFieldValue(index);
		} else {
			return null;
		}
	}
	
	/**
	 * 根据字段名查找伪列
	 * @param pname 字段名
	 * @return
	 */
	public PseudoColumn findColumnByName(String name) {
		if (name == null || columns == null || columns.size() == 0) {
			return null;
		} else {
			for (PseudoColumn col : columns) {
				if (col.getName() != null && name.equals(col.getName())) {
					return col;
				}
			}
		}
		return null;
	}
	
	/**
	 * 根据伪字段名查找伪列
	 * @param pname 伪字段名，也可能是二值bits里面的字段名
	 * @return
	 */
	public PseudoColumn findColumnByPseudoName(String pname) {
		if (pname == null || columns == null || columns.size() == 0) {
			return null;
		} else {
			for (PseudoColumn col : columns) {
				if (col.getPseudo() != null && pname.equals(col.getPseudo())) {
					return col;
				}
				if (col.getBits() != null && col.getBits().firstIndexOf(pname) != 0) {
					return col;
				}
			}
		}
		return null;
	}

	/**
	 * 得到文件fn的组表对象
	 * @param fn
	 * @param partitions
	 * @param ctx
	 */
	private void parseFileToTable(String fn, int partitions[], Context ctx) {
		if (partitions == null) {
			FileObject fo = new FileObject(fn, null, null, ctx);
			File f = fo.getLocalFile().file();
			tables.add(GroupTable.openBaseTable(f, ctx));
		} else {
			tables.add(new FileGroup(fn, partitions).open(null, ctx));
		}
	}
	
	/**
	 * 得到文件或文件组的组表对象
	 * @param ctx
	 */
	private void parseFileToTable(Context ctx) {
		Object file = this.file;
		int partitions[] = null;

		Sequence zone = getZone();
		if (zone != null) {
			partitions = zone.toIntArray();
		}
		
		tables = new ArrayList<ITableMetaData>();
		if (file instanceof String) {
			parseFileToTable((String) file, partitions, ctx);
		} else {
			Sequence seq = (Sequence) file;
			int size = seq.length();
			for (int i = 1; i <= size; i++) {
				parseFileToTable((String) seq.get(i), partitions, ctx);
			}
		}
	}

	public String[] getAllColNames() {
		if (isBFile) {
			return ds.getFieldNames();
		} else if (var == null) {
			return tables.get(0).getAllColNames();
		} else {
			return memoryTable.dataStruct().getFieldNames();
		}
	}

	public String[] getAllSortedColNames() {
		if (isBFile) {
			return ds.getPrimary();
		} else if (var == null) {
			return tables.get(0).getAllSortedColNames();
		} else {
			return memoryTable.dataStruct().getPrimary();
		}
	}
	
	public void addPseudoColumn(PseudoColumn column) {
		if (columns == null) {
			columns = new ArrayList<PseudoColumn>();
		}
		columns.add(column);
	}
	
	/**
	 * 判断fields是否是虚表的有序字段
	 * @param fields
	 * @return
	 */
	public boolean isSortedFields(String[] fields) {
		if (sortedFields == null || fields == null) {
			return false;
		}
		
		int len = fields.length;
		if (len > sortedFields.length || len == 0) {
			return false;
		}
		
		for (int i = 0; i < len; i++) {
			if (fields[i] == null) {
				return false;
			}
			if (!fields[i].equals(sortedFields[i])) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * 检查是否是集文件
	 * @return
	 */
	private boolean checkBFile(Context ctx) {
		FileObject fo = new FileObject((String)file, null, null, ctx);;
		BFileReader reader = new BFileReader(fo);
		try {
			reader.open();
			ds = reader.getFileDataStruct();
			reader.close();
			
			setFileObject(fo);
			isBFile = true;
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	public boolean isBFile() {
		return isBFile;
	}

	public FileObject getFileObject() {
		return fileObject;
	}

	public void setFileObject(FileObject fileObject) {
		this.fileObject = fileObject;
	}
}
