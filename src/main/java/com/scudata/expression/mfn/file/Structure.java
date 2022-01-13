package com.scudata.expression.mfn.file;

import java.io.File;
import java.util.ArrayList;

import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.FileObject;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dw.Cuboid;
import com.scudata.dw.GroupTable;
import com.scudata.dw.ITableIndex;
import com.scudata.dw.RowTableMetaData;
import com.scudata.dw.TableMetaData;
import com.scudata.expression.FileFunction;
import com.scudata.parallel.ClusterFile;
import com.scudata.parallel.ClusterTableMetaData;

/**
 * 获得组表文件的结构
 * f.structure()
 * @author LiWei
 *
 */
public class Structure extends FileFunction {
	private static final String FIELD_NAMES[] = { "field", "keys", "row", "zip", "seg", "zonex", "index", "cuboid", "attach" };
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
			ClusterTableMetaData table = cf.openGroupTable(ctx);
			Sequence seq = new Sequence();
			seq.add(getTableStruct(table, option));
			table.close();
			return seq;
		} else {
			// 本地文件
			File f = file.getLocalFile().file();
			TableMetaData table = GroupTable.openBaseTable(f, ctx);
			
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
	
	private static Record getTableStruct(ClusterTableMetaData table, String option) {
		return table.getStructure();
	}
	
	/**
	 * 获得table的结构，保存到out里
	 * @param table
	 */
	public static Record getTableStruct(TableMetaData table, String option) {
		boolean hasI = false;
		boolean hasC = false;
		if (option != null) {
			if (option.indexOf('i') != -1)
				hasI = true;
			if (option.indexOf('c') != -1)
				hasC = true;
		}
		
		Record rec = new Record(new DataStruct(FIELD_NAMES));
		String[] colNames = table.getAllColNames();
		rec.setNormalFieldValue(0, new Sequence(colNames));
		rec.setNormalFieldValue(1, new Sequence(table.getAllKeyColNames()));
		rec.setNormalFieldValue(2, table instanceof RowTableMetaData);
		rec.setNormalFieldValue(3, table.getGroupTable().isCompress());
		
		String seg = table.getSegmentCol();
		rec.setNormalFieldValue(4, seg != null && colNames[0] != null && seg.equals(colNames[0]));
		rec.setNormalFieldValue(5, table.getGroupTable().getDistribute());
		if (hasI) {
			rec.setNormalFieldValue(6, getTableIndexStruct(table));
		}
		if (hasC) {
			rec.setNormalFieldValue(7, getTableCuboidStruct(table));
		}
		
		ArrayList<TableMetaData> tables = table.getTableList();
		if (tables != null && tables.size() > 0) {
			Sequence seq = new Sequence();
			for (TableMetaData tbl : tables) {
				seq.add(getTableStruct(tbl, option));
			}
		}
		
		return rec;
	}
	
	/**
	 * 获得table的索引的结构
	 * @param table
	 * @returnhy
	 */
	private static Sequence getTableIndexStruct(TableMetaData table) {
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
	private static Sequence getTableCuboidStruct(TableMetaData table) {
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
					r.setNormalFieldValue(0, newExps[i]);
					aggr.add(r);
				}
				rec.setNormalFieldValue(2, aggr);
				
				srcCuboid.close();
			} catch (Exception e) {
				if (srcCuboid != null) srcCuboid.close();
			}
		}
		return seq;
	}
}
