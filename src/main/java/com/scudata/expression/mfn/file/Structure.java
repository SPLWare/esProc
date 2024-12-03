package com.scudata.expression.mfn.file;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.FileObject;
import com.scudata.dm.ObjectReader;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dw.Cuboid;
import com.scudata.dw.DataBlockType;
import com.scudata.dw.BlockLink;
import com.scudata.dw.BlockLinkReader;
import com.scudata.dw.ColPhyTable;
import com.scudata.dw.ColumnMetaData;
import com.scudata.dw.ComTable;
import com.scudata.dw.ITableIndex;
import com.scudata.dw.RowPhyTable;
import com.scudata.dw.PhyTable;
import com.scudata.dw.PhyTableIndex;
import com.scudata.expression.FileFunction;
import com.scudata.parallel.ClusterFile;
import com.scudata.parallel.ClusterPhyTable;
import com.scudata.resources.EngineMessage;

/**
 * 获得组表文件的结构
 * f.structure()
 * @author LiWei
 *
 */
public class Structure extends FileFunction {
	private static final String FIELD_NAMES[] = { "field", "key", "del", "row", "zip", "seg", "zonex", "attach", "block" };
	private static final String ATTACH_FIELD_NAMES[] = { "name", "field", "key", "row", "zip", "seg", "zonex", "attach" };
	private static final String COL_FIELD_FIELD_NAMES[] = {"name", "dim", "type", "type-len", "dict"};
	private static final String COL_FIELD_FIELD_NAMES_EXT[] = {"name", "dim", "type", "type-len", "dict", "block-nums", "block-ratio"};
	private static final String ROW_FIELD_FIELD_NAMES[] = {"name", "dim"};
	private static final String CUBOID_FIELD_NAMES[] = { "name", "keys", "aggr" };
	private static final String CUBOID_FIELD_NAMES2[] = { "keys", "aggr" };
	private static final String CUBOID_AGGR_FIELD_NAMES[] = { "name", "exp" };
	private static final String BLOCK_INFO_FIELD_NAMES[] = {"min", "max", "count", "pos"};
	
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
			boolean hasI = false;
			boolean hasC = false;
			if (option != null) {
				if (option.indexOf('i') != -1)
					hasI = true;
				if (option.indexOf('c') != -1)
					hasC = true;
			}
			if (hasI) {
				Sequence seq = new Sequence();
				seq.add(PhyTableIndex.getIndexStruct(file));
				return seq;
			}
			if (hasC) {
				Sequence seq = new Sequence();
				seq.add(getTableCuboidStruct(file));
				return seq;
			}
			
			PhyTable table = ComTable.openBaseTable(file, ctx);
			
			Integer partition = file.getPartition();
			if (partition != null && partition.intValue() > 0) {
				table.getGroupTable().setPartition(partition);
			}
			
			if (table instanceof ColPhyTable && option !=null && option.indexOf("b") != -1) {
				return getTableBlockInfo((ColPhyTable) table);
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
		
		Record rec;
		if (table.isBaseTable()) {
			rec = new Record(new DataStruct(FIELD_NAMES));
		} else {
			rec = new Record(new DataStruct(ATTACH_FIELD_NAMES));
			rec.setNormalFieldValue(idx++, table.getTableName());
		}
		
		String[] colNames = table.getAllColNames();
		rec.setNormalFieldValue(idx++, getTableColumnStruct(table, option));
		rec.setNormalFieldValue(idx++, table.hasPrimaryKey());
		if (table.isBaseTable()) {
			rec.setNormalFieldValue(idx++, table.getGroupTable().hasDeleteKey());
		}
		rec.setNormalFieldValue(idx++, table instanceof RowPhyTable);
		rec.setNormalFieldValue(idx++, table.getGroupTable().isCompress());
		
		String seg = table.getSegmentCol();
		rec.setNormalFieldValue(idx++, seg != null && colNames[0] != null && seg.equals(colNames[0]));
		rec.setNormalFieldValue(idx++, table.getGroupTable().getDistribute());
		
		ArrayList<PhyTable> tables = table.getTableList();
		if (tables != null && tables.size() > 0) {
			Sequence seq = new Sequence();
			for (PhyTable tbl : tables) {
				seq.add(getTableStruct(tbl, option));
			}
			rec.setNormalFieldValue(idx, seq);
		}
		
		if (table.isBaseTable()) {
			idx++;
			rec.setNormalFieldValue(idx, table.getGroupTable().getBlockSize());
		}
		return rec;
	}
	
	/**
	 * 获得table的列的结构
	 * @param table
	 * @param option 
	 * @return
	 */
	protected static Sequence getTableColumnStruct(PhyTable table, String option) {
		Sequence seq = new Sequence();
		if (table instanceof ColPhyTable) {
			ColumnMetaData[] columns = ((ColPhyTable) table).getColumns();
			boolean isExt = option != null && option.indexOf("e") != -1;
			DataStruct ds;
			double blockCount = 0.0;
			
			if (isExt) {
				ds = new DataStruct(COL_FIELD_FIELD_NAMES_EXT);
				for (ColumnMetaData column: columns ) {
					blockCount += getTableRowInfo(column);
				}
				
			} else {
				ds = new DataStruct(COL_FIELD_FIELD_NAMES);
			}
			
			for (ColumnMetaData column: columns ) {
				Record rec = new Record(ds);
				rec.setNormalFieldValue(0, column.getColName());
				rec.setNormalFieldValue(1, column.isKey());
				rec.setNormalFieldValue(2, DataBlockType.getTypeName(column.getDataType()));
				rec.setNormalFieldValue(3, DataBlockType.getTypeLen(column.getDataType()));
				Sequence dict = column.getDict();
				if (dict != null && dict.length() == 0) {
					dict = null;
				}
				rec.setNormalFieldValue(4, dict);
				
				if (isExt) {
					int num = getTableRowInfo(column);
					rec.setNormalFieldValue(5, num);
					rec.setNormalFieldValue(6, String.format("%.2f", num / blockCount * 100));
				}
				seq.add(rec);
			}
		} else {
			RowPhyTable rowTable = ((RowPhyTable) table);
			String[] columns = rowTable.getColNames();
			for (int c = 0, len = columns.length; c < len; c++) {
				String column = columns[c];
				Record rec = new Record(new DataStruct(ROW_FIELD_FIELD_NAMES));
				rec.setNormalFieldValue(0, column);
				rec.setNormalFieldValue(1, rowTable.isDim(column));
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
	
	private static Object getTableCuboidStruct(FileObject fo) {
		File file = fo.getLocalFile().file();
		Cuboid srcCuboid = null;
		Record rec = null;
		try {
			srcCuboid = new Cuboid(file, null);
			rec = new Record(new DataStruct(CUBOID_FIELD_NAMES2));
			rec.setNormalFieldValue(0, new Sequence(srcCuboid.getExps()));//分组表达式
			
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
			rec.setNormalFieldValue(1, aggr);
			srcCuboid.close();
		} catch (Exception e) {
			if (srcCuboid != null) srcCuboid.close();
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("license.fileFormatError"));
		}
		return rec;
	}
	
	//统计组表的分段信息
	private Sequence getTableBlockInfo(ColPhyTable table) {
		String[] keys = table.getAllKeyColNames();
		if (keys == null) return null;
		ColumnMetaData[] cols = table.getColumns(keys);
		int curBlock = 0;
		int endBlock = table.getDataBlockCount();
		int fcount = keys.length;
		ObjectReader[] segmentReaders = new ObjectReader[fcount];
		for (int i = 0; i < fcount; i++) {
			segmentReaders[i] = cols[i].getSegmentReader();
		}
		
		BlockLinkReader rowCountReader = table.getSegmentReader();
		
		Sequence result = new Sequence();
		String dsFields[] = BLOCK_INFO_FIELD_NAMES.clone();
		for (int i = 0; i < fcount; i++) {
			dsFields[0] += "-" + cols[i].getColName();
		}
		
		
		try {
			while (curBlock < endBlock) {
				curBlock++;
				int recordCount = rowCountReader.readInt32();
				Sequence minSeq = new Sequence(), maxSeq = new Sequence(), posSeq = new Sequence();
				for (int i = 0; i < fcount; i++) {
					long pos = segmentReaders[i].readLong40();
					Object minValue = null;
					Object maxValue = null;
					if (cols[i].hasMaxMinValues()) {
						minValue = segmentReaders[i].readObject();
						maxValue = segmentReaders[i].readObject();
						segmentReaders[i].skipObject();
					}
					minSeq.add(minValue);
					maxSeq.add(maxValue);
					posSeq.add(pos);
				}
				Record rec = new Record(new DataStruct(dsFields));
				if (fcount == 1) {
					rec.setNormalFieldValue(0, minSeq.get(1));
					rec.setNormalFieldValue(1, maxSeq.get(1));
					rec.setNormalFieldValue(2, recordCount);
					rec.setNormalFieldValue(3, posSeq.get(1));
				} else {
					rec.setNormalFieldValue(0, minSeq);
					rec.setNormalFieldValue(1, maxSeq);
					rec.setNormalFieldValue(2, recordCount);
					rec.setNormalFieldValue(3, posSeq);
				}
				result.add(rec);
			}
			
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				for (int i = 0; i < fcount; i++) {
					segmentReaders[i].close();
				}
				rowCountReader.close();
			} catch (IOException e) {
			}
		}
		return result;
	}
	
	//统计组表各列的信息
	private static int getTableRowInfo(ColumnMetaData column) {
		BlockLink blockLink = column.getDataBlockLink();
		return blockLink.getBlockCount();
	}
}
