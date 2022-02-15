package com.scudata.dw.pseudo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.scudata.dm.Context;
import com.scudata.dm.FileObject;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dw.GroupTable;
import com.scudata.dw.ITableMetaData;

//用于定义虚表的属性
public class PseudoDefination {
	public static final String PD_FILE = "file";
	public static final String PD_ZONE = "zone";
	public static final String PD_DATE = "date";
	public static final String PD_USER = "user";
	public static final String PD_COLUMN = "column";
	
	private Object file;//文件名或多个文件名的序列
	private Sequence zone;//组表分区号列表
	private String date;//分列字段
	private String user;//帐户字段
	private List<PseudoColumn> columns;//部分特殊字段定义
	private List<ITableMetaData> tables;//存所有文件的table对象
	
	private String[] sortedFields;//排序字段
	
	public PseudoDefination() {
		
	}
	
	public PseudoDefination(Record pd, Context ctx) {
		file = getFieldValue(pd, PD_FILE);
		zone = (Sequence) getFieldValue(pd, PD_ZONE);
		date = (String) getFieldValue(pd, PD_DATE);
		user = (String) getFieldValue(pd, PD_USER);
		Sequence seq = (Sequence) getFieldValue(pd, PD_COLUMN);
		if (seq != null) {
			columns = new ArrayList<PseudoColumn>();
			int size = seq.length();
			for (int i = 1; i <= size; i++) {
				Record rec = (Record) seq.get(i);
				columns.add(new PseudoColumn(rec));
			}
		}
		parseFileToTable(ctx);
		sortedFields = getAllSortedColNames();
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

	protected static Object getFieldValue(Record pd, String name) {
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

	private void parseFileToTable(String fn, Context ctx) {
		FileObject fo = new FileObject(fn, null, null, ctx);
		File f = fo.getLocalFile().file();
		tables.add(GroupTable.openBaseTable(f, ctx));
	}
	
	private void parseFileToTable(String fn, int partitions[], Context ctx) {
		if (partitions == null) {
			parseFileToTable(fn, ctx);
		} else {
			for (int partition : partitions) {
				FileObject fo = new FileObject(fn, null, null, ctx);
				fo.setPartition(partition);
				File f = fo.getLocalFile().file();
				tables.add(GroupTable.openBaseTable(f, ctx));
			}
		}
	}
	
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
		return tables.get(0).getAllColNames();
	}

	public String[] getAllSortedColNames() {
		return tables.get(0).getAllSortedColNames();
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
}
