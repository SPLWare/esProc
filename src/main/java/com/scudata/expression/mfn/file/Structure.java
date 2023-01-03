package com.scudata.expression.mfn.file;

import java.io.File;
import java.util.ArrayList;

import com.scudata.dm.BaseRecord;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.FileObject;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dw.Cuboid;
import com.scudata.dw.DataBlockType;
import com.scudata.dw.ColPhyTable;
import com.scudata.dw.ColumnMetaData;
import com.scudata.dw.ComTable;
import com.scudata.dw.ITableIndex;
import com.scudata.dw.RowPhyTable;
import com.scudata.dw.PhyTable;
import com.scudata.expression.FileFunction;
import com.scudata.parallel.ClusterFile;
import com.scudata.parallel.ClusterPhyTable;

/**
 * 获得组表文件的结构
 * f.structure()
 * @author LiWei
 *
 */
public class Structure extends FileFunction {
	private static final String FIELD_NAMES[] = { "field", "key", "row", "zip", "seg", "zonex", "index", "cuboid", "attach" };
	private static final String ATTACH_FIELD_NAMES[] = { "name", "field", "key", "row", "zip", "seg", "zonex", "index", "cuboid", "attach" };
	private static final String COL_FIELD_FIELD_NAMES[] = {"name", "dim", "type", "type-len", "dict"};
	private static final String ROW_FIELD_FIELD_NAMES[] = {"name", "dim"};
	private static final String CUBOID_FIELD_NAMES[] = { "name", "keys", "aggr" };
	private static final String CUBOID_AGGR_FIELD_NAMES[] = { "name", "exp" };
	
	public Object calculate(Context ctx) {
		if (file.isRemoteFile()) {
			// 远程文件
			String host = file.getIP();
			int port = file.getPort();
			String fileName = file.getFileName();
			Integer partition = file.getPartition();
			int p = partition == null ? -1 : partition.intValue();
			ClusterFile cf = new ClusterFile(host, port, fileName, p, ctx);
			ClusterPhyTable table = cf.openGroupTable(ctx);
			Sequence seq = new Sequence();
			seq.add(getTableStruct(table, option));
			table.close();
			return seq;
		} else {
			// 本地文件
			File f = file.getLocalFile().file();
			PhyTable table = ComTable.openBaseTable(f, ctx);
			
			Integer partition = file.getPartition();
			if (partition != null && partition.intValue() > 0) {
				table.getGroupTable().setPartition(partition);
			}
			Sequence seq = new Sequence();
			seq.add(getTableStruct(table, option));
			table.close();
			return seq;
		}
	}
	
	protected static BaseRecord getTableStruct(ClusterPhyTable table, String option) {
		return table.getStructure();
	}
	
	/**
	 * 获得table的结构，保存到out里
	 * @param table
	 */
	public static Record getTableStruct(PhyTable table, String option) {
		int idx = 0;
		boolean hasI = false;
		boolean hasC = false;
		if (option != null) {
			if (option.indexOf('i') != -1)
				hasI = true;
			if (option.indexOf('c') != -1)
				hasC = true;
		}
		
		Record rec;
		if (table.isBaseTable()) {
			rec = new Record(new DataStruct(FIELD_NAMES));
		} else {
			rec = new Record(new DataStruct(ATTACH_FIELD_NAMES));
			rec.setNormalFieldValue(idx++, table.getTableName());
		}
		
		String[] colNames = table.getAllColNames();
		rec.setNormalFieldValue(idx++, getTableColumnStruct(table));
		rec.setNormalFieldValue(idx++, table.hasPrimaryKey());
		rec.setNormalFieldValue(idx++, table instanceof RowPhyTable);
		rec.setNormalFieldValue(idx++, table.getGroupTable().isCompress());
		
		String seg = table.getSegmentCol();
		rec.setNormalFieldValue(idx++, seg != null && colNames[0] != null && seg.equals(colNames[0]));
		rec.setNormalFieldValue(idx++, table.getGroupTable().getDistribute());
		if (hasI) {
			rec.setNormalFieldValue(idx, getTableIndexStruct(table));
		}
		idx++;
		
		if (hasC) {
			rec.setNormalFieldValue(idx, getTableCuboidStruct(table));
		}
		idx++;
		
		ArrayList<PhyTable> tables = table.getTableList();
		if (tables != null && tables.size() > 0) {
			Sequence seq = new Sequence();
			for (PhyTable tbl : tables) {
				seq.add(getTableStruct(tbl, option));
			}
			rec.setNormalFieldValue(idx, seq);
		}
		
		return rec;
	}
	
	/**
	 * 获得table的列的结构
	 * @param table
	 * @return
	 */
	protected static Sequence getTableColumnStruct(PhyTable table) {
		Sequence seq = new Sequence();
		if (table instanceof ColPhyTable) {
			ColumnMetaData[] columns = ((ColPhyTable) table).getColumns();
			for (ColumnMetaData column: columns ) {
				Record rec = new Record(new DataStruct(COL_FIELD_FIELD_NAMES));
				rec.setNormalFieldValue(0, column.getColName());
				rec.setNormalFieldValue(1, column.isDim());
				rec.setNormalFieldValue(2, DataBlockType.getTypeName(column.getDataType()));
				rec.setNormalFieldValue(3, DataBlockType.getTypeLen(column.getDataType()));
				Sequence dict = column.getDict();
				if (dict != null && dict.length() == 0) {
					dict = null;
				}
				rec.setNormalFieldValue(4, dict);
				seq.add(rec);
			}
		} else {
			RowPhyTable rowTable = ((RowPhyTable) table);
			String[] columns = rowTable.getColNames();
			boolean[] isDim = rowTable.getDimIndex();
			for (int c = 0, len = columns.length; c < len; c++) {
				String column = columns[c];
				Record rec = new Record(new DataStruct(ROW_FIELD_FIELD_NAMES));
				rec.setNormalFieldValue(0, column);
				rec.setNormalFieldValue(1, isDim[c]);
				seq.add(rec);
			}
		}
		
		return seq;
	}
	
	/**
	 * 获得table的索引的结构
	 * @param table
	 * @return
	 */
	protected static Sequence getTableIndexStruct(PhyTable table) {
		String inames[] = table.getIndexNames();
		if (inames == null) {
			return null;
		}
		Sequence seq = new Sequence();
		String dir = table.getGroupTable().getFile().getAbsolutePath() + "_";
		for (String iname: inames) {
			FileObject indexFile = new FileObject(dir + table.getTableName() + "_" + iname);
			if (indexFile.isExists()) {
				ITableIndex index = table.getTableMetaDataIndex(indexFile, iname, true);
				seq.add(index.getIndexStruct());
			}
		}
		return seq;
	}
	
	/**
	 * 获得table的预分组的结构
	 * @param table
	 * @return
	 */
	protected static Sequence getTableCuboidStruct(PhyTable table) {
		String cuboids[] = table.getCuboids();
		if (cuboids == null) {
			return null;
		}

		Sequence seq = new Sequence();
		String dir = table.getGroupTable().getFile().getAbsolutePath() + "_";
		for (String cuboid: cuboids) {
			FileObject fo = new FileObject(dir + table.getTableName() + Cuboid.CUBE_PREFIX + cuboid);
			File file = fo.getLocalFile().file();
			Cuboid srcCuboid = null;
			
			try {
				srcCuboid = new Cuboid(file, null);
				Record rec = new Record(new DataStruct(CUBOID_FIELD_NAMES));
				rec.setNormalFieldValue(0, cuboid);
				rec.setNormalFieldValue(1, new Sequence(srcCuboid.getExps()));//分组表达式
				
				/**
				 * 组织汇总表达式
				 */
				Sequence aggr = new Sequence();
				String[] newExps = srcCuboid.getNewExps();//汇总表达式
				String[] names = srcCuboid.getBaseTable().getAllColNames();//这里的后半部分是汇总表达式的name
				int len = newExps.length;
				int start = names.length - len;
				for (int i = 0; i < len; i++) {
					Record r = new Record(new DataStruct(CUBOID_AGGR_FIELD_NAMES));
					r.setNormalFieldValue(0, names[start + i]);
					r.setNormalFieldValue(1, newExps[i]);
					aggr.add(r);
				}
				rec.setNormalFieldValue(2, aggr);
				seq.add(rec);
				srcCuboid.close();
			} catch (Exception e) {
				if (srcCuboid != null) srcCuboid.close();
			}
		}
		return seq;
	}
}
