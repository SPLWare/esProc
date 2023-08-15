package com.scudata.dw;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import com.scudata.array.IArray;
import com.scudata.array.LongArray;
import com.scudata.common.IntArrayList;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.ObjectReader;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.SerialBytes;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.ConjxCursor;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.cursor.MemoryCursor;
import com.scudata.dm.cursor.MergesCursor;
import com.scudata.dm.cursor.MultipathCursors;
import com.scudata.expression.Constant;
import com.scudata.expression.Expression;
import com.scudata.expression.Node;
import com.scudata.expression.UnknownSymbol;
import com.scudata.expression.mfn.serial.Sbs;
import com.scudata.expression.operator.Add;
import com.scudata.expression.operator.DotOperator;
import com.scudata.expression.operator.Equals;
import com.scudata.expression.operator.Greater;
import com.scudata.expression.operator.NotEquals;
import com.scudata.expression.operator.NotGreater;
import com.scudata.expression.operator.NotSmaller;
import com.scudata.expression.operator.Smaller;
import com.scudata.resources.EngineMessage;
import com.scudata.thread.ThreadPool;
import com.scudata.util.Variant;

/**
 * 列存基表类
 * @author runqian
 *
 */
public class ColPhyTable extends PhyTable {
	private transient ColumnMetaData []columns;
	private transient ColumnMetaData []allColumns; //含主表
	private transient ColumnMetaData []sortedColumns; // 排序字段
	private transient ColumnMetaData []allSortedColumns; // 排序字段含主表
	
	private transient String []allKeyColNames; // 主键字段名数组（含主表）
	
	private transient ColumnMetaData guideColumn;//导列
	protected int sortedColStartIndex;//主表的排序字段个数
	
	private static final String GUIDE_COLNAME = "_guidecol";

	/**
	 * 用于序列化
	 * @param groupTable
	 */
	public ColPhyTable(ComTable groupTable) {
		this.groupTable = groupTable;
		this.segmentBlockLink = new BlockLink(groupTable);
		this.modifyBlockLink1 = new BlockLink(groupTable);
		this.modifyBlockLink2 = new BlockLink(groupTable);
	}
	
	/**
	 * 用于序列化
	 * @param groupTable
	 * @param parent
	 */
	public ColPhyTable(ComTable groupTable, ColPhyTable parent) {
		this.groupTable = groupTable;
		this.parent = parent;
		this.segmentBlockLink = new BlockLink(groupTable);
		this.modifyBlockLink1 = new BlockLink(groupTable);
		this.modifyBlockLink2 = new BlockLink(groupTable);
	}
	
	/**
	 * 用于新创建一个基表
	 * @param groupTable
	 * @param colNames
	 * @param serialBytesLen
	 * @throws IOException
	 */
	public ColPhyTable(ComTable groupTable, String []colNames) throws IOException {
		this.groupTable = groupTable;
		this.tableName = "";
		this.segmentBlockLink = new BlockLink(groupTable);
		this.modifyBlockLink1 = new BlockLink(groupTable);
		this.modifyBlockLink2 = new BlockLink(groupTable);
		
		int count = colNames.length;
		columns = new ColumnMetaData[count];
		int keyStart = -1; // 主键的起始字段
		
		// 主键起始字段前面的字段认为是排序字段
		for (int i = 0; i < count; ++i) {
			if (colNames[i].startsWith(KEY_PREFIX)) {
				keyStart = i;
				break;
			}
		}
		
		for (int i = 0; i < count; ++i) {
			if (colNames[i].startsWith(KEY_PREFIX)) {
				String colName = colNames[i].substring(KEY_PREFIX.length());
				columns[i] = new ColumnMetaData(this, colName, true, true);
			} else if (i < keyStart) {
				columns[i] = new ColumnMetaData(this, colNames[i], true, false);
			} else {
				columns[i] = new ColumnMetaData(this, colNames[i], false, false);
			}
		}
		
		init();
		
		if (sortedColumns == null) {
			hasPrimaryKey = false;
			isSorted = false;
		}
		
		tableList = new ArrayList<PhyTable>();
		this.reserve[0] = 4;
	}

	/**
	 * 用于新创建一个基表
	 * @param groupTable
	 * @param colNames
	 * @param serialBytesLen
	 * @throws IOException
	 */
	public ColPhyTable(ComTable groupTable, String []colNames, int []serialBytesLen) throws IOException {
		this.groupTable = groupTable;
		this.tableName = "";
		this.colNames = colNames;
		this.segmentBlockLink = new BlockLink(groupTable);
		this.modifyBlockLink1 = new BlockLink(groupTable);
		this.modifyBlockLink2 = new BlockLink(groupTable);
		
		int count = colNames.length;
		columns = new ColumnMetaData[count];
		for (int i = 0; i < count; ++i) {
			columns[i] = new ColumnMetaData(this, colNames[i], serialBytesLen[i]);
		}
		
		init();
		
		if (sortedColumns == null) {
			hasPrimaryKey = false;
			isSorted = false;
		}
		tableList = new ArrayList<PhyTable>();
	}

	/**
	 * 附表的创建
	 * @param groupTable 要创建附表的组表
	 * @param colNames 列名称
	 * @param serialBytesLen 排号长度
	 * @param tableName 附表名
	 * @param parent 主表对象
	 * @throws IOException
	 */
	public ColPhyTable(ComTable groupTable, String []colNames, int []serialBytesLen,
			String tableName, ColPhyTable parent) throws IOException {
		this.groupTable = groupTable;
		this.parent = parent;
		this.tableName = tableName;
		this.colNames = colNames;
		this.segmentBlockLink = new BlockLink(groupTable);
		this.modifyBlockLink1 = new BlockLink(groupTable);
		this.modifyBlockLink2 = new BlockLink(groupTable);
		
		int count = colNames.length;
		columns = new ColumnMetaData[count];
		for (int i = 0; i < count; ++i) {
			if (colNames[i].startsWith(KEY_PREFIX)) {
				String colName = colNames[i].substring(KEY_PREFIX.length());
				columns[i] = new ColumnMetaData(this, colName, true, true);
			} else {
				columns[i] = new ColumnMetaData(this, colNames[i], false, false);
			}
		}
		
		init();
		
		if (getAllSortedColumns() == null) {
			hasPrimaryKey = false;
			isSorted = false;
		}
		
		if (parent != null) {
			//目前限制只依附一层
			if (parent.parent != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("ds.dsNotMatch"));
			}
			
			PhyTable primaryTable = parent;
			String []primarySortedColNames = primaryTable.getSortedColNames();
			String []primaryColNames = primaryTable.getColNames();
			ArrayList<String> collist = new ArrayList<String>();
			for (String name : primaryColNames) {
				collist.add(name);
			}
			
			//字段不能与主表字段重复
			for (int i = 0, len = colNames.length; i < len; i++) {
				if (collist.contains(colNames[i])) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(colNames[i] + mm.getMessage("dw.fieldSameToPrimaryTable"));
				}
			}
			
			for (String name : primarySortedColNames) {
				collist.remove(name);
			}

			sortedColStartIndex = primarySortedColNames.length;
			guideColumn = new ColumnMetaData(this, tableName + GUIDE_COLNAME, false, false);
		}
		
		tableList = new ArrayList<PhyTable>();
	}
	
	/**
	 * 根据src创建一个同构的基表
	 * @param groupTable 要创建基表的组表
	 * @param parent 主表对象
	 * @param src 提供结构的源基表
	 * @throws IOException
	 */
	public ColPhyTable(ComTable groupTable, ColPhyTable parent, ColPhyTable src) throws IOException {
		this.groupTable = groupTable;
		this.parent = parent;
		
		System.arraycopy(src.reserve, 0, reserve, 0, reserve.length);
		segmentCol = src.segmentCol;
		segmentSerialLen = src.segmentSerialLen;
		tableName = src.tableName;
		colNames = src.colNames;
		this.segmentBlockLink = new BlockLink(groupTable);
		this.modifyBlockLink1 = new BlockLink(groupTable);
		this.modifyBlockLink2 = new BlockLink(groupTable);
		
		int count = colNames.length;
		columns = new ColumnMetaData[count];
		ColumnMetaData []srcCols = src.getColumns();
		for (int i = 0; i < count; ++i) {
			columns[i] = new ColumnMetaData(this, srcCols[i]);
		}
		
		init();
		
		if (sortedColumns == null && allSortedColumns == null) {
			hasPrimaryKey = false;
			isSorted = false;
		}
		
		if (parent != null) {
			guideColumn = new ColumnMetaData(this, tableName + GUIDE_COLNAME, false, false);
		}

		dupIndexAdnCuboid(src);
		
		tableList = new ArrayList<PhyTable>();
		for (PhyTable srcSub : src.tableList) {
			tableList.add(new ColPhyTable(groupTable, this, (ColPhyTable)srcSub));
		}
	}

	/**
	 * 初始化，读取维、列名等基本信息
	 */
	protected void init() {
		ColumnMetaData []columns = this.columns;
		int dimCount = 0;
		int keyCount = 0;
		int j = 0;
		
		// 创建新数组，否则文件组时可能影响到其它分区组表的创建
		colNames = new String[columns.length];
		for (ColumnMetaData col : columns) {
			if (col.isDim()) {
				dimCount++;
			}
			
			if (col.isKey()) {
				keyCount++;
			}
			
			colNames[j++] = col.getColName();
		}
		
		if (keyCount > 0) {
			sortedColumns = new ColumnMetaData[dimCount];
			allKeyColNames = new String[keyCount];
			
			int i = 0, k = 0;
			for (ColumnMetaData col : columns) {
				if (col.isDim()) {
					sortedColumns[i++] = col;
				}
				
				if (col.isKey()) {
					allKeyColNames[k++] = col.getColName();
				}
			}
		}
		
		if (parent != null) {
			// 合并父表的主键
			String []parentKeys = parent.getAllKeyColNames();
			if (keyCount > 0) {
				int parentKeyCount = parentKeys.length;
				String []tmp = new String[parentKeyCount+ keyCount ];
				System.arraycopy(parentKeys, 0, tmp, 0, parentKeyCount);
				System.arraycopy(allKeyColNames, 0, tmp, parentKeyCount, keyCount);
				allKeyColNames = tmp;
			} else {
				allKeyColNames = parentKeys;
			}
			
			String []primarySortedColNames = parent.getSortedColNames();
			sortedColStartIndex = primarySortedColNames.length;
			
			allSortedColumns = new ColumnMetaData[sortedColStartIndex + dimCount];
			ColumnMetaData []baseSortedCols = ((ColPhyTable) parent).getSortedColumns();
			int i = 0;
			if (baseSortedCols != null) {
				for (ColumnMetaData col : baseSortedCols) {
					allSortedColumns[i++] = col;
				}
			}
			if (sortedColumns != null) {
				for (ColumnMetaData col : sortedColumns) {
					allSortedColumns[i++] = col;
				}
			}
			ColumnMetaData []parentColumns = ((ColPhyTable) parent).getSortedColumns();
			allColumns = new ColumnMetaData[parentColumns.length + columns.length];
			allColNames = new String[parentColumns.length + columns.length];
			i = 0;
			for (ColumnMetaData col : parentColumns) {
				allColNames[i] = col.getColName();
				allColumns[i++] = col;
			}
			for (ColumnMetaData col : columns) {
				allColNames[i] = col.getColName();
				allColumns[i++] = col;
			}
			ds = new DataStruct(allColNames);
		} else {
			ds = new DataStruct(colNames);
		}
	}
	
	public ColumnMetaData[] getColumns() {
		return columns;
	}
	
	/**
	 * 返回所有列。（含主表key列）
	 * @return
	 */
	public ColumnMetaData[] getAllColumns() {
		if (parent == null) return columns;
		return allColumns;
	}
	
	/**
	 * 返回所有列。（含主表所有字段)
	 * @return
	 */
	ColumnMetaData[] getTotalColumns() {
		if (parent == null) return columns;
		int baseColCount = ((ColPhyTable)parent).columns.length;
		int len = baseColCount + columns.length;
		ColumnMetaData[] cols = new ColumnMetaData[len];
		System.arraycopy(((ColPhyTable)parent).columns, 0, cols, 0, baseColCount);
		System.arraycopy(columns, 0, cols, baseColCount, columns.length);		
		return cols;
	}
	
	/**
	 * 返回所有列名称。（含主表所有字段)
	 * @return
	 */
	public String[] getTotalColNames() {
		if (parent == null) return colNames;
		int baseColCount = parent.colNames.length;
		int len = baseColCount + colNames.length;
		String[] names = new String[len];
		System.arraycopy(parent.colNames, 0, names, 0, baseColCount);
		System.arraycopy(colNames, 0, names, baseColCount, colNames.length);		
		return names;
	}
	
	/**
	 * 返回有序列
	 * @return
	 */
	public ColumnMetaData[] getSortedColumns() {
		return sortedColumns;
	}
	
	/**
	 * 返回有序列（含主表)
	 * @return
	 */
	public ColumnMetaData[] getAllSortedColumns() {
		if (parent == null) return sortedColumns;
		return allSortedColumns;
	}
	
	/**
	 * 返回有序列名
	 * @return
	 */
	public String[] getSortedColNames() {
		if (sortedColumns == null) return null;
		int len = sortedColumns.length;
		String []names = new String[len];
		for (int i = 0; i < len; ++i) {
			names[i] = sortedColumns[i].getColName();
		}
		return names;
	}

	/**
	 * 返回有序列名（含主表)
	 * @return
	 */
	public String[] getAllSortedColNames() {
		if (parent == null) return getSortedColNames();
		if (allSortedColumns == null) return null;
		int len = allSortedColumns.length;
		String []names = new String[len];
		for (int i = 0; i < len; ++i) {
			names[i] = allSortedColumns[i].getColName();
		}
		return names;
	}
	
	/**
	 * 取主键字段名（含主表）
	 * @return 主键字段名数组
	 */
	public String[] getAllKeyColNames() {
		return allKeyColNames;
	}
	
	/**
	 * 根据字段名返回指定列
	 * @param fields 字段名
	 * @return
	 */
	public ColumnMetaData[] getColumns(String []fields) {
		if (fields == null) {
			return columns;
		}
		
		ColumnMetaData []columns = this.columns;
		int srcCount = columns.length;
		int count = fields.length;
		ColumnMetaData []result = new ColumnMetaData[count];
		
		Next:
		for (int i = 0; i < count; ++i) {
			String field = fields[i];
			for (int s = 0; s < srcCount; ++s) {
				if (columns[s].isColumn(field)) {
					result[i] = columns[s];
					continue Next;
				}
			}
			
			MessageManager mm = EngineMessage.get();
			throw new RQException(field + mm.getMessage("ds.fieldNotExist"));
		}
		
		return result;
	}
	
	/**
	 * 返回每列排号的长度（如果是排号列的话）
	 */
	public int[] getSerialBytesLen() {
		int len = columns.length;
		int []serialBytesLen = new int[len];
		for (int i = 0; i < len; i++) {
			serialBytesLen[i] = columns[i].getSerialBytesLen();
		}
		return serialBytesLen;
	}
	
	/**
	 * 提取exp里涉及的列
	 * @param exps
	 * @return
	 */
	public ColumnMetaData[] getColumns(Expression []exps) {
		if (exps == null) {
			return columns;
		}
		
		ColumnMetaData []columns = this.columns;
		int srcCount = columns.length;
		int count = exps.length;
		ColumnMetaData []result = new ColumnMetaData[count];

		Next:
		for (int i = 0; i < count; ++i) {
			if (exps[i].getHome() instanceof UnknownSymbol) {
				String col = exps[i].getIdentifierName();
				for (int s = 0; s < srcCount; ++s) {
					if (columns[s].isColumn(col)) {
						result[i] = columns[s];
						continue Next;
					}
				}

				MessageManager mm = EngineMessage.get();
				throw new RQException(col + mm.getMessage("ds.fieldNotExist"));
			}
		}
		return result;
	}
	
	/**
	 * 提取exp里需要计算的列(k.sbs() k1+k2)
	 * @param exps
	 * @return
	 */
	public ArrayList<ColumnMetaData> getExpColumns(Expression []exps) {
		if (exps == null) {
			return null;
		}
		
		ArrayList<ColumnMetaData> result = new ArrayList<ColumnMetaData>();
		ColumnMetaData []columns = this.columns;
		int srcCount = columns.length;
		int count = exps.length;

		Next:
		for (int i = 0; i < count; ++i) {
			if (exps[i].getHome() instanceof DotOperator) {
				String col = null;
				Object left = exps[i].getHome().getLeft();
				Object right = exps[i].getHome().getRight();
				if (left instanceof UnknownSymbol && right instanceof Sbs) {
					col = ((UnknownSymbol)left).getName();
				} else {
					continue;
				}
				for (int s = 0; s < srcCount; ++s) {
					if (columns[s].isColumn(col)) {
						if (! result.contains(columns[s])) {
							result.add(columns[s]);
						}
						continue Next;
					}
				}

				MessageManager mm = EngineMessage.get();
				throw new RQException(col + mm.getMessage("ds.fieldNotExist"));
			} else if (exps[i].getHome() instanceof Add) {
				String col1 = null;
				String col2 = null;
				Object obj1 = exps[i].getHome().getLeft();
				Object obj2 = exps[i].getHome().getRight();
				if ((obj1 instanceof UnknownSymbol) 
						&& (obj2 instanceof UnknownSymbol)) {
					col1 = ((UnknownSymbol)obj1).getName();
					col2 = ((UnknownSymbol)obj2).getName();
				}
				boolean b1 = false,b2 = false;
				for (int s = 0; s < srcCount; ++s) {
					if (columns[s].isColumn(col1)) {
						if (! result.contains(columns[s])) {
							result.add(columns[s]);
						}
						b1 = true;
					}
					if (columns[s].isColumn(col2)) {
						if (! result.contains(columns[s])) {
							result.add(columns[s]);
						}
						b2 = true;
					}
				}
				if (b1 && b2) continue;
				MessageManager mm = EngineMessage.get();
				throw new RQException(col1 + " or " + col2 + mm.getMessage("ds.fieldNotExist"));
			}
		}
		if (result.size() == 0) {
			return null;
		}
		return result;
	}

	/**
	 * 根据字段名获得列
	 * @param field
	 * @return
	 */
	public ColumnMetaData getColumn(String field) {
		ColumnMetaData []columns = this.columns;
		for (ColumnMetaData col : columns) {
			if (col.isColumn(field)) {
				return col;
			}
		}
		
		return null;
	}

	/**
	 * 返回附表的导列
	 * @return
	 */
	public ColumnMetaData getGuideColumn() {
		return guideColumn;
	}
	
	/**
	 * 申请第一块空间
	 */
	protected void applyFirstBlock() throws IOException {
		if (segmentBlockLink.isEmpty()) {
			segmentBlockLink.setFirstBlockPos(groupTable.applyNewBlock());
			ColumnMetaData []columns = this.columns;
			for (ColumnMetaData col : columns) {
				col.applySegmentFirstBlock();
			}
			
			for (ColumnMetaData col : columns) {
				col.applyDataFirstBlock();
			}
			
			if (parent != null) {
				guideColumn.applySegmentFirstBlock();
				guideColumn.applyDataFirstBlock();
			}
		}
	}
	
	/**
	 * 准备写。在追加、删除、修改数据前调用。
	 * 调用后会对关键信息进行备份，防止写一般出意外时表数据损坏
	 */
	protected void prepareAppend() throws IOException {
		applyFirstBlock();
		
		segmentWriter = new BlockLinkWriter(segmentBlockLink, true);
		for (ColumnMetaData col : columns) {
			col.prepareWrite();
		}
		if (parent != null) {
			guideColumn.prepareWrite();
		}
	}
	
	/**
	 * 结束写
	 */
	protected void finishAppend() throws IOException {
		segmentWriter.finishWrite();
		segmentWriter = null;
		for (ColumnMetaData col : columns) {
			col.finishWrite();
		}
		
		if (parent != null) {
			guideColumn.finishWrite();
		}
		
		groupTable.save();
		updateIndex();
	}
	
	/**
	 * 读取表头数据
	 */
	public void readExternal(BufferReader reader) throws IOException {
		reader.read(reserve);
		tableName = reader.readUTF();
		colNames = reader.readStrings();
		dataBlockCount = reader.readInt32();
		totalRecordCount = reader.readLong40();
		segmentBlockLink.readExternal(reader);
		curModifyBlock = reader.readByte();
		modifyBlockLink1.readExternal(reader);
		modifyBlockLink2.readExternal(reader);
		
		int count = reader.readInt();
		columns = new ColumnMetaData[count];
		for (int i = 0; i < count; ++i) {
			columns[i] = new ColumnMetaData(this);
			columns[i].readExternal(reader, reserve[0]);
		}
		
		count = reader.readInt();
		if (count > 0) {
			maxValues = new Object[count];
			for (int i = 0; i < count; ++i) {
				maxValues[i] = reader.readObject();
			}
		}
	
		hasPrimaryKey = reader.readBoolean();
		isSorted = reader.readBoolean();
		boolean isPrimaryTable  = reader.readBoolean();
		if (!isPrimaryTable) {
			guideColumn = new ColumnMetaData(this);
			guideColumn.readExternal(reader, reserve[0]);
		}
		
		indexNames = reader.readStrings();
		if (indexNames == null) {
			indexFields = null;
			indexValueFields = null;
		} else {
			int indexCount = indexNames.length;
			indexFields = new String[indexCount][];
			for (int i = 0; i < indexCount; i++) {
				indexFields[i] = reader.readStrings();
			}
			indexValueFields = new String[indexCount][];
			for (int i = 0; i < indexCount; i++) {
				indexValueFields[i] = reader.readStrings();
			}
		}

		if (groupTable.reserve[0] > 2) {
			cuboids = reader.readStrings();//版本3增加
		}
		segmentCol = (String)reader.readObject();
		segmentSerialLen = reader.readInt();
		init();
		
		count = reader.readInt();
		tableList = new ArrayList<PhyTable>(count);
		for (int i = 0; i < count; ++i) {
			PhyTable table = new ColPhyTable(groupTable, this);
			table.readExternal(reader);
			tableList.add(table);
		}
	}
	
	/**
	 * 写出表头数据
	 */
	public void writeExternal(BufferWriter writer) throws IOException {
		reserve[0] = 5;
		writer.write(reserve);
		writer.writeUTF(tableName);
		writer.writeStrings(colNames);
		writer.writeInt32(dataBlockCount);
		writer.writeLong40(totalRecordCount);
		segmentBlockLink.writeExternal(writer);
		writer.writeByte(curModifyBlock);
		modifyBlockLink1.writeExternal(writer);
		modifyBlockLink2.writeExternal(writer);
		
		ColumnMetaData []columns = this.columns;
		int count = columns.length;
		writer.writeInt(count);
		for (int i = 0; i < count; ++i) {
			columns[i].writeExternal(writer);
		}
		
		if (maxValues == null) {
			writer.writeInt(0);
		} else {
			writer.writeInt(maxValues.length);
			for (Object val : maxValues) {
				writer.writeObject(val);
			}
			
			writer.flush();
		}
		
		writer.writeBoolean(hasPrimaryKey);
		writer.writeBoolean(isSorted);
		writer.writeBoolean(parent == null);
		if (parent != null) {
			guideColumn.writeExternal(writer);
		}
		
		writer.writeStrings(indexNames);
		if (indexNames != null) {
			for (int i = 0, indexCount = indexNames.length; i < indexCount; i++) {
				writer.writeStrings(indexFields[i]);
			}
			for (int i = 0, indexCount = indexNames.length; i < indexCount; i++) {
				writer.writeStrings(indexValueFields[i]);
			}
		}

		writer.writeStrings(cuboids);//版本3增加
		
		writer.writeObject(segmentCol);
		writer.flush();
		writer.writeInt(segmentSerialLen);
		
		ArrayList<PhyTable> tableList = this.tableList;
		count = tableList.size();
		writer.writeInt(count);
		for (int i = 0; i < count; ++i) {
			tableList.get(i).writeExternal(writer);
		}
	}
	
	/**
	 * 追加附表的一块数据
	 * @param data
	 * @param start 开始的列
	 * @param recList 导列数据
	 * @throws IOException
	 */
	private void appendAttachedDataBlock(Sequence data, boolean []isMyCol, LongArray recList) throws IOException {
		ColumnMetaData []columns = this.allColumns;
		int count = columns.length;

		Object []minValues = new Object[count];;//一块的最小维值
		Object []maxValues = new Object[count];;//一块的最大维值
		Object []startValues = new Object[count];
		int[] dataTypeInfo = new int[count];
				
		BufferWriter bufferWriter = guideColumn.getColDataBufferWriter();
		BufferWriter bufferWriters[] = new BufferWriter[count];
		
		DataBlockWriterJob[] jobs = new DataBlockWriterJob[count];
		ThreadPool pool = ThreadPool.newInstance(count);
		
		int end = data.length();
		try {
			//写导列
			bufferWriter.write(DataBlockType.LONG);
			for (int i = 1; i <= end; ++i) {
				bufferWriter.writeLong(recList.getLong(i));
			}
			bufferWriter.writeBoolean(false);
			
			//写数据列任务
			for (int i = 0; i < count; i++) {
				if (!isMyCol[i])
					continue;
				bufferWriters[i] = columns[i].getColDataBufferWriter();
				Sequence dict = columns[i].getDict();
				jobs[i] = new DataBlockWriterJob(bufferWriters[i], data, dict, i, 1, end, 
						maxValues, minValues, startValues, dataTypeInfo);
				pool.submit(jobs[i]);
			}
			
			for (int i = 0; i < count; ++i) {
				if (!isMyCol[i]) continue;
				jobs[i].join();
			}
		} finally {
			pool.shutdown();
		}
		
		//统计列数据类型
		boolean doCheck = groupTable.isCheckDataPure();
		for (int j = 0; j < count; j++) {
			if (!isMyCol[j]) continue;
			columns[j].adjustDataType(dataTypeInfo[j], doCheck);
			columns[j].initDictArray();
		}

		if (recList.size() == 0) {
			//如果是空块，则各列写一个null
			bufferWriter.writeObject(null);
			for (int j = 0; j < count; j++) {
				if (!isMyCol[j]) continue;
				bufferWriters[j].writeObject(null);
			}
		}
		
		guideColumn.appendColBlock(bufferWriter.finish());

		//提交每个列块buffer
		for (int j = 0; j < count; j++) {
			if (!isMyCol[j]) continue;
			columns[j].appendColBlock(bufferWriters[j].finish(), minValues[j], maxValues[j], startValues[j]);
		}
		
		//更新分段信息buffer
		appendSegmentBlock(end);
	}
	
	/**
	 * 追加附表的一块数据(旧格式)
	 * @param data
	 * @param start 开始的列
	 * @param recList 导列数据
	 * @throws IOException
	 */
	public void appendAttachedDataBlockV3(Sequence data, boolean []isMyCol, LongArray recList) throws IOException {
		BaseRecord r;
		ColumnMetaData []columns = this.allColumns;
		int count = columns.length;
		int []serialBytesLen = new int[count];
		Object []minValues = null;//一块的最小维值
		Object []maxValues = null;//一块的最大维值
		Object []startValues = null;
		
		if (sortedColumns != null) {
			minValues = new Object[count];
			maxValues = new Object[count];
			startValues = new Object[count];
		}
		
		BufferWriter bufferWriter = guideColumn.getColDataBufferWriter();
		BufferWriter bufferWriters[] = new BufferWriter[count];
		for (int i = 0; i < count; i++) {
			if (!isMyCol[i]) continue;
			serialBytesLen[i] = columns[i].getSerialBytesLen();
			bufferWriters[i] = columns[i].getColDataBufferWriter();
		}
		
		int end = data.length();
		for (int i = 1; i <= end; ++i) {
			//写导列
			bufferWriter.writeObject(recList.get(i - 1));
			
			r = (BaseRecord) data.get(i);
			Object[] vals = r.getFieldValues();
			//把一条写到各列的buffer
			for (int j = 0; j < count; j++) {
				if (!isMyCol[j]) continue;
				Object obj = vals[j];
				if (serialBytesLen[j] > 0) {
					if (obj instanceof SerialBytes) {
						bufferWriters[j].writeObject(obj);
					} else {
						Long val;
						if (obj instanceof Integer) {
							val = (Integer)obj & 0xFFFFFFFFL;
						} else {
							val = (Long) obj;
						}
						bufferWriters[j].writeObject(new SerialBytes(val, serialBytesLen[j]));
					}
				} else {
					bufferWriters[j].writeObject(obj);
				}
			}
			for (int j = 0; j < count; j++) {
				if (!isMyCol[j]) continue;
				Object obj = vals[j];
				if (columns[j].isDim()) {
					if (Variant.compare(obj, maxValues[j], true) > 0)
						maxValues[j] = obj;
					if (i == 1)
					{
						minValues[j] = obj;//第一个要赋值，因为null表示最小
						startValues[j] = obj;
					}
					if (Variant.compare(obj, minValues[j], true) < 0)
						minValues[j] = obj;
				}
			}
		}

		if (recList.size() == 0) {
			//如果是空块，则各列写一个null
			bufferWriter.writeObject(null);
			for (int j = 0; j < count; j++) {
				if (!isMyCol[j]) continue;
				bufferWriters[j].writeObject(null);
			}
		}
		
		guideColumn.appendColBlock(bufferWriter.finish());
		
		if (sortedColumns == null) {
			//提交每个列块buffer
			for (int j = 0; j < count; j++) {
				if (!isMyCol[j]) continue;
				columns[j].appendColBlock(bufferWriters[j].finish());
			}
			//更新分段信息buffer
			appendSegmentBlock(end);
			return;
		}

		//提交每个列块buffer
		for (int j = 0; j < count; j++) {
			if (!isMyCol[j]) continue;
			if (!columns[j].isDim()) {
				//追加列块
				columns[j].appendColBlock(bufferWriters[j].finish());
			} else {
				//追加维块
				columns[j].appendColBlock(bufferWriters[j].finish(), minValues[j], maxValues[j], startValues[j]);
			}
		}
		
		//更新分段信息buffer
		appendSegmentBlock(end);
	}
	
	/**
	 * 把data序列的指定范围的数据写出(新格式)
	 * @param data 数据序列
	 * @param start 开始位置
	 * @param end 结束位置
	 * @throws IOException
	 */
	private void appendDataBlock(Sequence data, int start, int end) throws IOException {
		ColumnMetaData []columns = this.columns;
		int count = columns.length;
		Object []minValues = new Object[count];//一块的最小维值
		Object []maxValues = new Object[count];//一块的最大维值
		Object []startValues = new Object[count];
		int[] dataTypeInfo = new int[count];

		BufferWriter bufferWriters[] = new BufferWriter[count];
		DataBlockWriterJob[] jobs = new DataBlockWriterJob[count];
		ThreadPool pool = ThreadPool.newInstance(count);
		
		try {
			for (int i = 0; i < count; i++) {
				bufferWriters[i] = columns[i].getColDataBufferWriter();
				Sequence dict = columns[i].getDict();
				jobs[i] = new DataBlockWriterJob(bufferWriters[i], data, dict, i, start, end, 
						maxValues, minValues, startValues, dataTypeInfo);
				pool.submit(jobs[i]);
			}
			
			for (int i = 0; i < count; ++i) {
				jobs[i].join();
			}
		} finally {
			pool.shutdown();
		}
		
		//统计列数据类型
		boolean doCheck = groupTable.isCheckDataPure();
		for (int j = 0; j < count; j++) {
			columns[j].adjustDataType(dataTypeInfo[j], doCheck);
			columns[j].initDictArray();
		}
		
		//提交每个列块buffer
		for (int j = 0; j < count; j++) {
			columns[j].appendColBlock(bufferWriters[j].finish(), minValues[j], maxValues[j], startValues[j]);
		}
		
		//更新分段信息buffer
		appendSegmentBlock(end - start + 1);
	}
	
	/**
	 * 把data序列的指定范围的数据写出(旧格式)
	 * @param data 数据序列
	 * @param start 开始位置
	 * @param end 结束位置
	 * @throws IOException
	 */
	public void appendDataBlockV3(Sequence data, int start, int end) throws IOException {
		BaseRecord r;
		ColumnMetaData []columns = this.columns;
		int count = columns.length;
		int []serialBytesLen = new int[count];
		Object []minValues = null;//一块的最小维值
		Object []maxValues = null;//一块的最大维值
		Object []startValues = null;
		
		if (sortedColumns != null) {
			minValues = new Object[count];
			maxValues = new Object[count];
			startValues = new Object[count];
		}
		
		BufferWriter bufferWriters[] = new BufferWriter[count];
		for (int i = 0; i < count; i++) {
			serialBytesLen[i] = columns[i].getSerialBytesLen();
			bufferWriters[i] = columns[i].getColDataBufferWriter();
		}
		
		IArray mems = data.getMems();
		for (int i = start; i <= end; ++i) {
			r = (BaseRecord) mems.get(i);
			mems.set(i, null);
			
			Object[] vals = r.getFieldValues();
			//把一条写到各列的buffer
			for (int j = 0; j < count; j++) {
				Object obj = vals[j];
				if (serialBytesLen[j] > 0) {
					if (obj instanceof SerialBytes) {
						if (((SerialBytes)obj).length() > serialBytesLen[j]) {
							MessageManager mm = EngineMessage.get();
							throw new RQException(mm.getMessage("engine.indexOutofBound"));
						}
						bufferWriters[j].writeObject(obj);
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException(mm.getMessage("dw.needSerialBytes"));
					}
				} else {
					bufferWriters[j].writeObject(obj);
				}
				if (columns[j].isDim()) {
					if (Variant.compare(obj, maxValues[j], true) > 0)
						maxValues[j] = obj;
					if (i == start) {
						minValues[j] = obj;//第一个要赋值，因为null表示最小
						startValues[j] = obj;
					}
					if (Variant.compare(obj, minValues[j], true) < 0)
						minValues[j] = obj;
				}
			}
		}
		
		if (sortedColumns == null) {
			//提交每个列块buffer
			for (int j = 0; j < count; j++) {
				columns[j].appendColBlock(bufferWriters[j].finish());
			}
			//更新分段信息buffer
			appendSegmentBlock(end - start + 1);
			return;
		}

		//提交每个列块buffer
		for (int j = 0; j < count; j++) {
			if (!columns[j].isDim()) {
				//追加列块
				columns[j].appendColBlock(bufferWriters[j].finish());
			} else {
				//追加维块
				columns[j].appendColBlock(bufferWriters[j].finish(), minValues[j], maxValues[j], startValues[j]);
			}
		}
		
		//更新分段信息buffer
		appendSegmentBlock(end - start + 1);
	}
	
	/**
	 * 把游标的数据写出
	 * @param cursor
	 * @throws IOException
	 */
	private void appendNormal(ICursor cursor) throws IOException {
		Sequence data = cursor.fetch(MIN_BLOCK_RECORD_COUNT);
		while (data != null && data.length() > 0) {
			appendDataBlock(data, 1, data.length());
			data = cursor.fetch(MIN_BLOCK_RECORD_COUNT);
		}
	}
	
	/**
	 * 把游标的数据写出 （附表）
	 * @param cursor
	 * @throws IOException
	 */
	private void appendAttached(ICursor cursor) throws IOException {
		PhyTable primaryTable = parent;
		int pBlockCount = primaryTable.getDataBlockCount();//主表的已有总块数
		int curBlockCount = dataBlockCount;//要追加的开始块号
		int pkeyEndIndex = sortedColStartIndex;
		
		String []primaryTableKeys = primaryTable.getSortedColNames();
		ArrayList<String> primaryTableKeyList = new ArrayList<String>();
		for (String name : primaryTableKeys) {
			primaryTableKeyList.add(name);
		}
		String []colNames = getAllColNames();
		int fcount = colNames.length;
		boolean []isMyCol = new boolean[fcount];
		for (int i = 0; i < fcount; i++) {
			if (primaryTableKeyList.contains(colNames[i])) {
				isMyCol[i] = false;
			} else {
				isMyCol[i] = true;
			}
		}
		
		Cursor cs;
		if (primaryTable.totalRecordCount == 0) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("dw.baseTableNull"));
		}
		cs = (Cursor) primaryTable.cursor(primaryTableKeys);
		cs.setSegment(curBlockCount, curBlockCount + 1);
		Sequence pkeyData = cs.fetch(ICursor.MAXSIZE);
		int pkeyIndex = 1;
		int pkeyDataLen = pkeyData.length();
		ComTableRecord curPkey = (ComTableRecord) pkeyData.get(1);
		Object []curPkeyVals = curPkey.getFieldValues();
		
		int sortedColCount = allSortedColumns.length;
		Object []tableMaxValues = this.maxValues;
		Object []lastValues = new Object[sortedColCount];//上一条维的值
		
		LongArray guideCol = new LongArray(MIN_BLOCK_RECORD_COUNT);
		Sequence seq = new Sequence(MIN_BLOCK_RECORD_COUNT);
		Sequence data = cursor.fetch(ICursor.FETCHCOUNT);
		BaseRecord r;
		Object []vals = new Object[sortedColCount];
		int []findex = getSortedColIndex();
		
		while (data != null && data.length() > 0) {
			int len = data.length();
			for (int i = 1; i <= len; ++i) {
				r = (BaseRecord) data.get(i);
				for (int f = 0; f < sortedColCount; ++f) {
					vals[f] = r.getNormalFieldValue(findex[f]);
				}
				
				//找本条和主键对应的记录
				while (true) {
					int cmp = Variant.compareArrays(curPkeyVals, vals, pkeyEndIndex);
					if (cmp == 0) {
						break;
					} else if (cmp < 0) {
						pkeyIndex++;
						if (pkeyIndex > pkeyDataLen) {
							//注意：这时有可能seq里没有记录，这就要追加一个空块
							//处理完一段了就提交一块
							appendAttachedDataBlock(seq, isMyCol, guideCol);
							seq.clear();
							guideCol = new LongArray(MIN_BLOCK_RECORD_COUNT);
							
							//取下一段主键数据
							curBlockCount++;
							if (curBlockCount >= pBlockCount) {
								//主表取到最后了，附表里不应该还有数据，抛异常
								MessageManager mm = EngineMessage.get();
								throw new RQException(mm.getMessage("dw.appendNotMatch") + r.toString(null));
							}
							cs = (Cursor) primaryTable.cursor(primaryTableKeys);
							cs.setSegment(curBlockCount, curBlockCount + 1);
							pkeyData = cs.fetch(ICursor.MAXSIZE);
							pkeyIndex = 1;
							pkeyDataLen = pkeyData.length();
						}
						curPkey = (ComTableRecord) pkeyData.get(pkeyIndex);
						curPkeyVals = curPkey.getFieldValues();
					} else if (cmp > 0) {
						//没找到对应的主表记录，抛异常
						MessageManager mm = EngineMessage.get();
						throw new RQException(mm.getMessage("dw.appendNotMatch") + r.toString(null));
					}
				}
				
				//到了这里就确定要追加一条了
				guideCol.add(curPkey.getRecordSeq());//添加导列
				
				//处理主键和排序
				if (isSorted) {
					if (tableMaxValues != null) {
						int cmp = Variant.compareArrays(vals, tableMaxValues, sortedColCount);
						if (cmp < 0) {
							//附表追加的数据必须有序
							MessageManager mm = EngineMessage.get();
							throw new RQException(mm.getMessage("dw.appendAttachedTable"));
						} else if (cmp == 0){
							if (hasPrimaryKey) hasPrimaryKey = false;
						} else {
							System.arraycopy(vals, 0, tableMaxValues, 0, sortedColCount);
						}
					} else {
						tableMaxValues = maxValues = new Object[sortedColCount];
						System.arraycopy(vals, 0, tableMaxValues, 0, sortedColCount);
					}
				}
				
				seq.add(r);//把这条暂存				
				System.arraycopy(vals, 0, lastValues, 0, sortedColCount);//存上一条维值				
			}
			data = cursor.fetch(ICursor.FETCHCOUNT);
		}
		
		//处理最后一个列块 (即附表取到最后了，主表里还有的话，就不管了)
		if (seq.length() > 0) {
			appendAttachedDataBlock(seq, isMyCol, guideCol);
		}
		
	}
	
	/**
	 * 把游标的数据写出。写出时需要进行分段。
	 * @param cursor
	 * @throws IOException
	 */
	private void appendSegment(ICursor cursor) throws IOException {
		int recCount = 0;
		int sortedColCount = sortedColumns.length;
		Object []tableMaxValues = this.maxValues;

		String segmentCol = getSegmentCol();
		int segmentSerialLen = getSegmentSerialLen();
		int segmentIndex = 0;

		for (int i = 0; i < sortedColCount; i++) {
			if (segmentCol.equals(sortedColumns[i].getColName())) {
				segmentIndex = i;
				break;
			}
		}
		int cmpLen = segmentIndex + 1;
		int serialBytesLen = sortedColumns[segmentIndex].getSerialBytesLen();
		if (segmentSerialLen == 0 || segmentSerialLen > serialBytesLen) {
			segmentSerialLen = serialBytesLen;
		}
		Object []lastValues = new Object[cmpLen];//上一条维的值
		Object []curValues = new Object[cmpLen];//当前条维的值
		
		Sequence seq = new Sequence(MIN_BLOCK_RECORD_COUNT);
		Sequence data = cursor.fetch(ICursor.FETCHCOUNT);
		BaseRecord r;
		Object []vals = new Object[sortedColCount];
		int []findex = getSortedColIndex();
		
		while (data != null && data.length() > 0) {
			int len = data.length();
			for (int i = 1; i <= len; ++i) {
				r = (BaseRecord) data.get(i);
				for (int f = 0; f < sortedColCount; ++f) {
					vals[f] = r.getNormalFieldValue(findex[f]);
				}

				//这里判断是否够一个列块了
				if (recCount >= MIN_BLOCK_RECORD_COUNT){
					System.arraycopy(vals, 0, curValues, 0, cmpLen);
					if (0 != Variant.compareArrays(lastValues, curValues, cmpLen)) {
						appendDataBlock(seq, 1, seq.length());
						seq.clear();
						recCount = 0;
					}
				}
				
				//处理主键和排序
				if (isSorted) {
					if (tableMaxValues != null) {
						int cmp = Variant.compareArrays(vals, tableMaxValues, sortedColCount);
						if (cmp < 0) {
							hasPrimaryKey = false;
							isSorted = false;
							maxValues = null;
						} else if (cmp == 0){
							if (hasPrimaryKey) hasPrimaryKey = false;
						} else {
							System.arraycopy(vals, 0, tableMaxValues, 0, sortedColCount);
						}
					} else {
						tableMaxValues = maxValues = new Object[sortedColCount];
						System.arraycopy(vals, 0, tableMaxValues, 0, sortedColCount);
					}
					if (tableList.size() > 0 && !hasPrimaryKey) {
						//存在附表时，主表追加的数据必须有序唯一
						MessageManager mm = EngineMessage.get();
						throw new RQException(mm.getMessage("dw.appendPrimaryTable"));
					}
				}
				
				seq.add(r);//把这条暂存
				System.arraycopy(vals, 0, lastValues, 0, cmpLen);
				recCount++;
			}
			data = cursor.fetch(ICursor.FETCHCOUNT);
		}
		
		//处理最后一个列块
		if (seq.length() > 0) {
			appendDataBlock(seq, 1, seq.length());
		}
		
	}
	
	/**
	 * 把游标的数据写出。写出时需要判断数据是否对维有序。
	 * @param cursor
	 * @throws IOException
	 */
	private void appendSorted(ICursor cursor) throws IOException {
		int recCount = 0;
		int sortedColCount = sortedColumns.length;
		Object []tableMaxValues = this.maxValues;
		Object []lastValues = new Object[sortedColCount];//上一条维的值
		
		Sequence seq = new Sequence(MIN_BLOCK_RECORD_COUNT);
		Sequence data = cursor.fetch(ICursor.FETCHCOUNT);
		BaseRecord r;
		Object []vals = new Object[sortedColCount];
		int []findex = getSortedColIndex();

		while (data != null && data.length() > 0) {
			int len = data.length();
			for (int i = 1; i <= len; ++i) {
				r = (BaseRecord) data.get(i);
				for (int f = 0; f < sortedColCount; ++f) {
					vals[f] = r.getNormalFieldValue(findex[f]);
				}

				//这里判断是否够一个列块了
				if (recCount >= MAX_BLOCK_RECORD_COUNT) {
					//这时提交一半
					appendDataBlock(seq, 1, MAX_BLOCK_RECORD_COUNT/2);
					seq = (Sequence) seq.get(MAX_BLOCK_RECORD_COUNT/2 + 1, seq.length() + 1);
					recCount = seq.length(); 
				} else if (recCount >= MIN_BLOCK_RECORD_COUNT){
					boolean doAppend = true;
					int segLen;
					if (sortedColCount > 1) {
						segLen = sortedColCount / 2;
					} else {
						segLen = 1;
					}
					for (int c = 0; c < segLen; c++) {
						int cmp = Variant.compare(lastValues[c], vals[c], true);
						if (cmp == 0) {
							doAppend = false;
							break;
						}
					}
					if (doAppend) {
						appendDataBlock(seq, 1, seq.length());
						seq.clear();
						recCount = 0;
					}
				}
				
				//处理主键和排序
				if (isSorted) {
					if (tableMaxValues != null) {
						int cmp = Variant.compareArrays(vals, tableMaxValues, sortedColCount);
						if (cmp < 0) {
							hasPrimaryKey = false;
							isSorted = false;
							maxValues = null;
						} else if (cmp == 0){
							if (hasPrimaryKey) {
								hasPrimaryKey = false;
							}
						} else {
							System.arraycopy(vals, 0, tableMaxValues, 0, sortedColCount);
						}
						if (tableList.size() > 0 && !hasPrimaryKey) {
							//存在附表时，主表追加的数据必须有序唯一
							MessageManager mm = EngineMessage.get();
							throw new RQException(mm.getMessage("dw.appendPrimaryTable"));
						}
					} else {
						tableMaxValues = maxValues = new Object[sortedColCount];
						System.arraycopy(vals, 0, tableMaxValues, 0, sortedColCount);
					}
				}
				
				seq.add(r);//把这条暂存				
				System.arraycopy(vals, 0, lastValues, 0, sortedColCount);//存上一条维值				
				recCount++;
			}
			data = cursor.fetch(ICursor.FETCHCOUNT);
		}
		
		//处理最后一个列块
		if (seq.length() > 0) {
			appendDataBlock(seq, 1, seq.length());
		}
		
	}
	
	private void mergeAppend(ICursor cursor, String opt) throws IOException {
		// 不支持带附表的组表归并追加
		if (!isSingleTable()) {
			throw new RQException("'append@m' is unimplemented in annex table!");
		}
		
		// 检查数据结构是否兼容
		Sequence data = cursor.peek(ICursor.FETCHCOUNT);		
		if (data == null || data.length() <= 0) {
			return;
		}
		
		//判断结构匹配
		DataStruct ds = data.dataStruct();
		if (ds == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.needPurePmt"));
		}
		
		ColumnMetaData []columns = this.columns;
		int colCount = columns.length;
		if (colCount != ds.getFieldCount()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.dsNotMatch"));
		}
		
		for (int i = 0; i < colCount; i++) {
			if (!ds.getFieldName(i).equals(columns[i].getColName())) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.dsNotMatch"));
			}
		}
		
		// 归并读的数据先保存到临时文件
		ColComTable groupTable = (ColComTable)getGroupTable();
		File srcFile = groupTable.getFile();
		File tmpFile = File.createTempFile("tmpdata", "", srcFile.getParentFile());
		ColComTable tmpGroupTable = null;
		
		try {
			Context ctx = new Context();
			tmpGroupTable = new ColComTable(tmpFile, groupTable);
			
			PhyTable baseTable = tmpGroupTable.getBaseTable();
			
			int dcount = sortedColumns.length;
			Expression []mergeExps = new Expression[dcount];
			for (int i = 0; i < dcount; ++i) {
				mergeExps[i] = new Expression(sortedColumns[i].getColName());
			}
			
			// 做归并
			Cursor srcCursor = new Cursor(this);
			ICursor []cursors = new ICursor[]{srcCursor, cursor};
			MergesCursor mergeCursor = new MergesCursor(cursors, mergeExps, ctx);
			String[] indexNames = baseTable.indexNames;
			String[] cuboids = baseTable.cuboids;
			baseTable.deleteIndex(null);//临时文件不需要在append时处理index和cuboid
			baseTable.deleteCuboid(null);
			baseTable.append(mergeCursor);
			baseTable.appendCache();
			baseTable.indexNames = indexNames;
			baseTable.cuboids = cuboids;
			tmpGroupTable.save();
			baseTable.close();
			
			// 关闭并删除组表文件，把临时文件重命名为组表文件名
			groupTable.raf.close();
			groupTable.file.delete();
			tmpFile.renameTo(groupTable.file);
			
			// 重新打开组表
			groupTable.reopen();
			groupTable.baseTable.resetIndex(ctx);
			groupTable.baseTable.resetCuboid(ctx);
		} finally {
			if (tmpGroupTable != null) {
				tmpGroupTable.raf.close();
			}
			
			tmpFile.delete();
		}
	}
	
	/**
	 * 以归并方式追加(暂不支持有附表的情况)
	 */
	public void append(ICursor cursor, String opt) throws IOException {
		if (isSorted && opt != null) {
			if (opt.indexOf('y') != -1) {
				Sequence data = cursor.fetch();
				ColPhyTable ctmd = (ColPhyTable)getSupplementTable(false);
				if (ctmd == null) {
					append_y(data);
				} else {
					ctmd.append_y(data);
				}
			} else if (opt.indexOf('a') != -1) {
				ColPhyTable ctmd = (ColPhyTable)getSupplementTable(true);
				ctmd.mergeAppend(cursor, opt);
			} else if (opt.indexOf('m') != -1) {
				mergeAppend(cursor, opt);
			} else {
				append(cursor);
				if (opt.indexOf('i') != -1) {
					appendCache();
				}
			}
		} else if (opt != null) {
			if (opt.indexOf('y') != -1) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("ds.lessKey"));
			}
			
			append(cursor);
			if (opt.indexOf('i') != -1) {
				appendCache();
			}
		} else {
			append(cursor);
		}
	}

	/**
	 * 追加游标的数据到组表
	 * @param cursor
	 */
	public void append(ICursor cursor) throws IOException {
		getGroupTable().checkWritable();
		
		// 如果没有维字段则取GroupTable.MIN_BLOCK_RECORD_COUNT条记录
		// 否则假设有3个维字段d1、d2、d3，根据维字段的值取出至少MIN_BLOCK_RECORD_COUNT条记录
		// 如果[d1,d2,d3]是主键则不要把[d1,d2]值相同的给拆到两个块里，反之不要把[d1,d2,d3]值相同的拆到两个块里
		// 如果相同的超过了MAX_BLOCK_RECORD_COUNT，则以MAX_BLOCK_RECORD_COUNT / 2条为一块
		// 把每一列的数据写到BufferWriter然后调用finish得到字节数组，再调用compress压缩数据，最后写进ColumnMetaData
		// 有维字段时要更新maxValues、hasPrimaryKey两个成员，如果hasPrimaryKey为false则不再更新
		if (cursor == null) {
			return;
		}

		Sequence data = cursor.peek(MIN_BLOCK_RECORD_COUNT);		
		if (data == null || data.length() <= 0) {
			return;
		}
		
		//判断结构匹配
		DataStruct ds = data.dataStruct();
		if (ds == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.needPurePmt"));
		}
		
		ColumnMetaData []allColumns;
		if (parent == null) {
			allColumns = columns;
		} else {
			allColumns = this.allColumns;
		}
		int count = allColumns.length;
		for (int i = 0; i < count; i++) {
			if (!ds.getFieldName(i).equals(allColumns[i].getColName())) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.dsNotMatch"));
			}
		}
		
		//如果游标数据不足1块
		if (data.length() < MIN_BLOCK_RECORD_COUNT) {
			if (appendCache == null) {
				appendCache = data;
			} else {
				appendCache.addAll(data);
			}
			data = null;
			cursor.close();
			if (appendCache.length() >= MIN_BLOCK_RECORD_COUNT) {
				appendCache();
			}
			return;
		}
		
		//如果有缓存数据
		if (appendCache != null) {
			ICursor []cursorArray = new ICursor[2];
			cursorArray[0] = new MemoryCursor(appendCache);
			cursorArray[1] = cursor;
			cursor = new ConjxCursor(cursorArray);
			appendCache = null;
		}
		
		// 准备写数据
		prepareAppend();
		
		if (parent != null) {
			parent.appendCache();
			appendAttached(cursor);
		} else if (sortedColumns == null) {
			appendNormal(cursor);
		} else if (getSegmentCol() == null) {
			appendSorted(cursor);
		} else {
			appendSegment(cursor);
		}
		
		// 结束写数据，保存到文件
		finishAppend();
	}
	
	protected void appendSegmentBlock(int recordCount) throws IOException {
		dataBlockCount++;//数据块
		totalRecordCount += recordCount;//总记录数
		segmentWriter.writeInt32(recordCount);//
	}
	
	/**
	 * 取字段过滤优先级
	 * @param col
	 * @return
	 */
	public int getColumnFilterPriority(ColumnMetaData col) {
		if (sortedColumns != null) {
			int len = sortedColumns.length;
			for (int i = 0; i < len; ++i) {
				if (sortedColumns[i] == col) {
					return i;
				}
			}
			
			return len;
		} else {
			return 0;
		}
	}
	
	/**
	 * 返回这个基表的游标
	 */
	public ICursor cursor() {
		ComTable groupTable = getGroupTable();
		groupTable.checkReadable();
		
		ICursor cs;
		if (parent != null) {
			cs = JoinTableCursor.createAnnexCursor(this);
		} else {
			cs = new Cursor(this);
		}
		
		PhyTable tmd = getSupplementTable(false);
		if (tmd == null) {
			return cs;
		} else {
			ICursor cs2 = tmd.cursor();
			return merge(cs, cs2);
		}
	}

	/**
	 * 返回这个基表的游标
	 * @param exps 取出字段表达式（当exps为null时按照fields取出）
	 * @param fields 取出字段的新名称
	 * @param filter 过滤表达式
	 * @param fkNames 指定FK过滤的字段名称
	 * @param codes 指定FK过滤的数据序列
	 * @param opts 关联字段进行关联的选项
	 * @param opt 选项
	 * @param ctx 上下文
	 */
	public ICursor cursor(Expression []exps, String []fields, Expression filter, 
			String []fkNames, Sequence []codes, String []opts, String opt, Context ctx) {
		ComTable groupTable = getGroupTable();
		groupTable.checkReadable();
		
		ICursor cs = JoinTableCursor.createAnnexCursor(this, exps, fields, filter, fkNames, codes, ctx);
		if (cs == null) {
			cs = new Cursor(this, exps, fields, filter, fkNames, codes, opts, ctx);
		}
		
		PhyTable tmd = getSupplementTable(false);
		if (tmd == null) {
			return cs;
		} else {
			ICursor cs2 = tmd.cursor(exps, fields, filter, fkNames, codes, opts, opt, ctx);
			return merge(cs, cs2);
		}
	}

	public IFilter getFirstDimFilter(Expression exp, Context ctx) {
		Object obj = Cursor.parseFilter(this, exp, ctx);
		if (obj instanceof IFilter) {
			ColumnMetaData firstDim = getSortedColumns()[0];
			IFilter filter = (IFilter)obj;
			if (!filter.isMultiFieldOr() && filter.getColumn() == firstDim) {
				return filter;
			} else {
				return null;
			}
		} else if (obj instanceof ArrayList) {
			ColumnMetaData firstDim = getSortedColumns()[0];
			@SuppressWarnings("unchecked")
			ArrayList<Object> list = (ArrayList<Object>)obj;
			for (Object f : list) {
				if (f instanceof IFilter) {
					IFilter filter = (IFilter)f;
					if (!filter.isMultiFieldOr() && filter.getColumn() == firstDim) {
						return filter;
					}
				}
			}
		}

		return null;
	}
	
	/**
	 * 把表达式按列进行拆分
	 * @param exp 表达式
	 * @param ctx 计算上下文
	 * @return
	 */
	public IFilter[] getSortedFieldFilters(Expression exp, Context ctx) {
		Object obj = Cursor.parseFilter(this, exp, ctx);
		if (obj instanceof IFilter) {
			IFilter filter = (IFilter)obj;
			if (filter.isMultiFieldOr()) {
				return null;
			}
			
			ColumnMetaData column = filter.getColumn();
			if (column == null || !column.hasMaxMinValues()) {
				return null;
			} else {
				return new IFilter[] {filter};
			}
		} else if (obj instanceof ArrayList) {
			ArrayList<Object> list = (ArrayList<Object>)obj;
			ArrayList<IFilter> filterList = new ArrayList<IFilter>();
			
			for (Object f : list) {
				if (f instanceof IFilter) {
					IFilter filter = (IFilter)f;
					if (!filter.isMultiFieldOr()) {
						ColumnMetaData column = filter.getColumn();
						if (column != null && column.hasMaxMinValues()) {
							filterList.add(filter);
						}
					}
				}
			}
			
			if (filterList.size() > 0) {
				IFilter []filters = new IFilter[filterList.size()];
				filterList.toArray(filters);
				Arrays.sort(filters);
				return filters;
			} else {
				return null;
			}
		} else {
			return null;
		}
	}
	
	/**
	 * 返回这个基表的多路游标
	 * @param exps 取出字段表达式，（当exps为null时按照fields取出）
	 * @param fields 取出字段的新名称
	 * @param filter 过滤表达式
	 * @param fkNames 指定FK过滤的字段名称
	 * @param codes 指定FK过滤的数据序列
	 * @param pathCount 路数
	 * @param opt 选项
	 * @param ctx 上下文
	 */
	public ICursor cursor(Expression []exps, String []fields, Expression filter, String []fkNames, 
			Sequence []codes, String []opts, int pathCount, String opt, Context ctx) {
		if (pathCount < 2) {
			return cursor(exps, fields, filter, fkNames, codes, opts, opt, ctx);
		}
		
		PhyTable tmd = getSupplementTable(false);
		int blockCount = getDataBlockCount();
		if (blockCount == 0) {
			if (tmd == null) {
				return new MemoryCursor(null);
			} else {
				return tmd.cursor(exps, fields, filter, fkNames, codes, opts, pathCount, opt, ctx);
			}
		}
		
		// 如果是主表并且有维字段，则找出过滤表达式中关于第一个维的条件
		// 用第一个维的条件过滤出满足条件的块再拆分成多路游标
		//IFilter dimFilter = null;
		//if (filter != null && parent == null && getSortedColumns() != null) {
		//	dimFilter = getFirstDimFilter(filter, ctx);
		//}
		
		// 先按过滤字段找出满足条件的数据块，再进行分段
		IFilter []filters = null;
		if (filter != null && parent == null && (opt == null || opt.indexOf('w') == -1)) {
			filters = getSortedFieldFilters(filter, ctx);
		}
		
		ICursor []cursors;
		if (filters == null) {
			int avg = blockCount / pathCount;
			if (avg < 1) {
				avg = 1;
				pathCount = blockCount;
			}
			
			// 前面的块每段多一块
			int mod = blockCount % pathCount;
			cursors = new ICursor[pathCount];
			int start = 0;
			for (int i = 0; i < pathCount; ++i) {
				int end = start + avg;
				if (i < mod) {
					end++;
				}
				
				if (filter != null) {
					// 分段并行读取时需要复制表达式，同一个表达式不支持并行运算
					filter = filter.newExpression(ctx);
				}
				
				ICursor cursor = JoinTableCursor.createAnnexCursor(this, exps, fields, filter, fkNames, codes, ctx);
				if (cursor == null) {
					cursor = new Cursor(this, exps, fields, filter, fkNames, codes, opts, ctx);
				}
				
				if (cursor instanceof Cursor) {
					((Cursor) cursor).setSegment(start, end);
				} else {
					((JoinTableCursor) cursor).setSegment(start, end);
				}

				cursors[i] = cursor;
				start = end;
			}
		} else {
			IntArrayList list = new IntArrayList();
			int filterCount = filters.length;
			ObjectReader []readers = new ObjectReader[filterCount];
			
			for (int f = 0; f < filterCount; ++f) {
				ColumnMetaData column = filters[f].getColumn();
				readers[f] = column.getSegmentReader();
			}
			
			try {
				for (int i = 0; i < blockCount; ++i) {
					boolean match = true;
					for (int f = 0; f < filterCount; ++f) {
						readers[f].readLong40();
						Object minValue = readers[f].readObject();
						Object maxValue = readers[f].readObject();
						readers[f].skipObject();
						
						if (match && !filters[f].match(minValue, maxValue)) {
							match = false;
						}
					}
					
					if (match) {
						list.addInt(i);
					}
				}
			} catch (IOException e) {
				throw new RQException(e.getMessage(), e);
			}
			
			blockCount = list.size();
			if (blockCount == 0) {
				return new MemoryCursor(null);
			}
			
			int avg = blockCount / pathCount;
			if (avg < 1) {
				// 每段不足一块
				cursors = new ICursor[blockCount];
				for (int i = 0; i < blockCount; ++i) {
					// 分段并行读取时需要复制表达式，同一个表达式不支持并行运算
					filter = filter.newExpression(ctx);
					Cursor cursor = new Cursor(this, exps, fields, filter, fkNames, codes, opts, ctx);
					int b = list.getInt(i);
					cursor.setSegment(b, b + 1);
					cursors[i] = cursor;
				}
			} else {
				// 前面的块每段多一块
				int mod = blockCount % pathCount;
				cursors = new ICursor[pathCount];
				int start = 0;
				for (int i = 0; i < pathCount; ++i) {
					int end = start + avg;
					if (i < mod) {
						end++;
					}
					
					// 分段并行读取时需要复制表达式，同一个表达式不支持并行运算
					filter = filter.newExpression(ctx);
					Cursor cursor = new Cursor(this, exps, fields, filter, fkNames, codes, opts, ctx);
					cursor.setSegment(list.getInt(start), list.getInt(end - 1) + 1);
					cursors[i] = cursor;
					start = end;
				}
			}
		}
		
		MultipathCursors mcs = new MultipathCursors(cursors, ctx);
		if (tmd == null) {
			return mcs;
		}
		
		String []sortFields = ((IDWCursor)cursors[0]).getSortFields();
		if (sortFields != null) {
			ICursor cs2 = tmd.cursor(exps, fields, filter, fkNames, codes, opts, mcs, null, ctx);
			return merge(mcs, (MultipathCursors)cs2, sortFields);
		} else {
			ICursor cs2 = tmd.cursor(exps, fields, filter, fkNames, codes, opts, pathCount, opt, ctx);
			return conj(mcs, cs2);
		}
	}

	/**
	 * 返回分段游标，把基表分为segCount段，返回第segSeq段的数据
	 * @param exps 取出字段表达式，（当exps为null时按照fields取出）
	 * @param fields 取出字段的新名称
	 * @param filter 过滤表达式
	 * @param fkNames 指定FK过滤的字段名称
	 * @param codes 指定FK过滤的数据序列
	 * @param segSeq 第几段
	 * @param segCount  分段数
	 * @param opt 选项
	 * @param ctx 上下文
	 */
	public ICursor cursor(Expression []exps, String []fields, Expression filter, String []fkNames, 
			Sequence []codes, String []opts, int segSeq, int segCount, String opt, Context ctx) {
		getGroupTable().checkReadable();
		
		if (filter != null) {
			// 分段并行读取时需要复制表达式，同一个表达式不支持并行运算
			filter = filter.newExpression(ctx);
		}
		
		IDWCursor cursor = (IDWCursor)JoinTableCursor.createAnnexCursor(this, exps, fields, filter, fkNames, codes, ctx);
		if (cursor == null) {
			cursor = new Cursor(this, exps, fields, filter, fkNames, codes, opts, ctx);
		}
		
		if (segCount < 2) {
			return cursor;
		}
		
		int startBlock = 0;
		int endBlock = -1;
		int avg = dataBlockCount / segCount;
		
		if (avg < 1) {
			// 每段不足一块
			if (segSeq <= dataBlockCount) {
				startBlock = segSeq - 1;
				endBlock = segSeq;
			}
		} else {
			if (segSeq > 1) {
				endBlock = segSeq * avg;
				startBlock = endBlock - avg;
				
				// 剩余的块后面的每段多一块
				int mod = dataBlockCount % segCount;
				int n = mod - (segCount - segSeq);
				if (n > 0) {
					endBlock += n;
					startBlock += n - 1;
				}
			} else {
				endBlock = avg;
			}
		}

		cursor.setSegment(startBlock, endBlock);
		return cursor;
	}
	
	public static Sequence fetchToValue(IDWCursor cursor, String []names, Object []vals) {
		// 只取第一块的记录，如果第一块没有满足条件的就返回
		Sequence seq = cursor.getStartBlockData(ICursor.FETCHCOUNT);
		if (seq == null || seq.length() == 0) {
			return null;
		}
		
		int fcount = names.length;
		int []findex = new int[fcount];
		DataStruct ds = ((BaseRecord)seq.getMem(1)).dataStruct();
		for (int f = 0; f < fcount; ++f) {
			findex[f] = ds.getFieldIndex(names[f]);
			if (findex[f] == -1) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(names[f] + mm.getMessage("ds.fieldNotExist"));
			}
		}
		
		Sequence result = null;
		Object []curVals = new Object[fcount];
		
		while (true) {
			int len = seq.length();
			for (int i = 1; i <= len; ++i) {
				BaseRecord r = (BaseRecord)seq.getMem(i);
				for (int f = 0; f < fcount; ++f) {
					curVals[f] = r.getNormalFieldValue(findex[f]);
				}
				
				if (Variant.compareArrays(curVals, vals) >= 0) {
					if (i == 1) {
						cursor.setCache(seq);
						return result;
					} else if (result == null) {
						cursor.setCache(seq.split(i));
						result = seq;
					} else {
						cursor.setCache(seq.split(i));
						result.addAll(seq);
					}
					return result;
				}
			}
			
			if (result == null) {
				result = seq;
			} else {
				result.addAll(seq);
			}
			
			seq = cursor.getStartBlockData(ICursor.FETCHCOUNT);
			if (seq == null || seq.length() == 0) {
				return result;
			}
		}
	}
	
	/**
	 * 根据多路游标mcs，返回一个同步分段的多路游标
	 * @param exps 取出字段表达式，（当exps为null时按照fields取出）
	 * @param fields 取出字段的新名称
	 * @param filter 过滤表达式
	 * @param fkNames 指定FK过滤的字段名称
	 * @param codes 指定FK过滤的数据序列
	 * @param mcs 多路游标
	 * @param opt 选项
	 * @param ctx 上下文
	 */
	public ICursor cursor(Expression []exps, String []fields, Expression filter, String []fkNames, 
			Sequence []codes,  String []opts, MultipathCursors mcs, String opt, Context ctx) {
		getGroupTable().checkReadable();
		
		ICursor []srcCursors = mcs.getParallelCursors();
		int segCount = srcCursors.length;
		if (segCount == 1) {
			ICursor cs = cursor(exps, fields, filter, fkNames, codes, opts, opt, ctx);
			ICursor []cursors = new ICursor[] {cs};
			return new MultipathCursors(cursors, ctx);
		}
		
		Object [][]minValues = new Object [segCount][];
		int fcount = -1;
		
		for (int i = 1; i < segCount; ++i) {
			minValues[i] = srcCursors[i].getSegmentStartValues(opt);
			if (minValues[i] != null) {
				if (fcount == -1) {
					fcount = minValues[i].length;
				} else if (fcount != minValues[i].length) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("dw.segFieldNotMatch"));
				}
			}
		}
		
		if (fcount == -1) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("dw.segFieldNotMatch"));
		}

		String []dimFields;
		ColumnMetaData[] sortedCols;
		if (opt != null && opt.indexOf('k') != -1) {
			// 有k选项时以首键做为同步分段字段
			String []keys = getAllKeyColNames();
			if (keys == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("dw.segFieldNotMatch"));
			}
			
			fcount = 1;
			dimFields = new String[] {keys[0]};
			sortedCols = new ColumnMetaData[] {getColumn(keys[0])};
		} else {
			sortedCols = getAllSortedColumns();
			if (sortedCols.length < fcount) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("dw.segFieldNotMatch"));
			}
			
			dimFields = new String[fcount];
			for (int f = 0; f < fcount; ++f) {
				dimFields[f] = sortedCols[f].getColName();
			}
		}
		
		int blockCount = getDataBlockCount();
		ICursor []cursors = new ICursor[segCount];
		int startBlock = 0;
		int currentBlock = 0; // 当前读的最小值的块
		
		// 需要掐头的游标的对应的前面的游标的序号
		int []appendSegs = new int[segCount];
		for (int s = 0; s < segCount; ++s) {
			appendSegs[s] = -1;
		}
		
		try {
			ObjectReader []readers = new ObjectReader[fcount];
			Object []blockMinVals = new Object[fcount];
			for (int f = 0; f < fcount; ++f) {
				readers[f] = sortedCols[f].getSegmentReader();
				readers[f].readLong40();
				readers[f].skipObject();
				readers[f].skipObject();
				blockMinVals[f] = readers[f].readObject(); //startValue
			}
			
			Next:
			for (int s = 0; s < segCount; ++s) {
				if (filter != null) {
					// 分段并行读取时需要复制表达式，同一个表达式不支持并行运算
					filter = filter.newExpression(ctx);
				}
				
				int nextSeg = s + 1;
				Object []nextMinValue = null;
				while (nextSeg < segCount) {
					nextMinValue = minValues[nextSeg];
					if (nextMinValue != null) {
						break;
					} else {
						nextSeg++;
					}
				}
				
				if (nextMinValue == null) {
					cursors[s] = cursor(exps, fields, filter, fkNames, codes, opts, opt, ctx);
					((IDWCursor)cursors[s]).setSegment(startBlock, blockCount);
					startBlock = blockCount;
					continue;
				}
				
				while (currentBlock < blockCount) {
					int cmp = Variant.compareArrays(blockMinVals, nextMinValue);
					if (cmp > 0) {
						cursors[s] = cursor(exps, fields, filter, fkNames, codes, opts, opt, ctx);
						
						if (currentBlock > 0) {
							((IDWCursor)cursors[s]).setSegment(startBlock, currentBlock - 1);
							startBlock = currentBlock - 1;
							appendSegs[nextSeg] = s;
						} else {
							((IDWCursor)cursors[s]).setSegment(0, 0);
						}
						
						continue Next;
					} else if (cmp == 0) {
						cursors[s] = cursor(exps, fields, filter, fkNames, codes, opts, opt, ctx);
						((IDWCursor)cursors[s]).setSegment(startBlock, currentBlock);
						startBlock = currentBlock;
						continue Next;
					} else {
						currentBlock++;
						if (currentBlock < blockCount) {
							for (int f = 0; f < fcount; ++f) {
								readers[f].readLong40();
								readers[f].skipObject();
								readers[f].skipObject();
								blockMinVals[f] = readers[f].readObject(); //startValue
							}
						}
					}
				}
				
				cursors[s] = cursor(exps, fields, filter, fkNames, codes, opts, opt, ctx);
				((IDWCursor)cursors[s]).setSegment(startBlock, blockCount);
				startBlock = blockCount;
			}
			
			for (int i = segCount - 1; i > 0; --i) {
				if (appendSegs[i] != -1) {
					Sequence seq = fetchToValue((IDWCursor)cursors[i], dimFields, minValues[i]);
					((IDWCursor)cursors[appendSegs[i]]).setAppendData(seq);
				}
			}
		} catch (IOException e) {
			throw new RQException(e);
		}

		MultipathCursors result = new MultipathCursors(cursors, ctx);
		PhyTable tmd = getSupplementTable(false);
		if (tmd == null) {
			return result;
		}
		
		String []sortFields = ((IDWCursor)cursors[0]).getSortFields();
		if (sortFields != null) {
			ICursor cs2 = tmd.cursor(exps, fields, filter, fkNames, codes, opts, result, null, ctx);
			return merge(result, (MultipathCursors)cs2, sortFields);
		} else {
			ICursor cs2 = tmd.cursor(exps, fields, filter, fkNames, codes, opts, mcs, null, ctx);
			return conj(result, cs2);
		}
	}

	// 有补文件时的数据更新
	private Sequence update(PhyTable stmd, Sequence data, String opt) throws IOException {
		boolean isUpdate = true, isInsert = true, isSave = true;
		Sequence result = null;
		if (opt != null) {
			if (opt.indexOf('i') != -1) isUpdate = false;
			if (opt.indexOf('u') != -1) {
				if (!isUpdate) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(opt + mm.getMessage("engine.optConflict"));
				}
				
				isInsert = false;
			}
			
			if (opt.indexOf('n') != -1) result = new Sequence();
			if (opt.indexOf('m') != -1) isSave = false;
		}
		
		DataStruct ds = data.dataStruct();
		if (ds == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.needPurePmt"));
		}
		
		if (!ds.isCompatible(this.ds)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.dsNotMatch"));
		}
		
		// 对更新数据进行排序
		data.sortFields(getAllSortedColNames());
		appendCache();
		
		ColumnMetaData[] columns = getAllSortedColumns();
		int keyCount = columns.length;
		int []keyIndex = new int[keyCount];
		for (int k = 0; k < keyCount; ++k) {
			keyIndex[k] = ds.getFieldIndex(columns[k].getColName());
			if (keyIndex[k] < 0) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(columns[k].getColName() + mm.getMessage("ds.fieldNotExist"));
			}
		}
		
		boolean isPrimaryTable = parent == null;
		int len = data.length();
		long []seqs = new long[len + 1];
		int []block = new int[len + 1];//是否在一个段的底部insert(子表)
		long []recNum = null;
		int []temp = new int[1];
		
		if (isPrimaryTable) {
			RecordSeqSearcher searcher = new RecordSeqSearcher(this);
			if (keyCount == 1) {
				int k = keyIndex[0];
				for (int i = 1; i <= len; ++i) {
					BaseRecord r = (BaseRecord)data.getMem(i);
					seqs[i] = searcher.findNext(r.getFieldValue(k));
				}
			} else {
				Object []keyValues = new Object[keyCount];
				for (int i = 1; i <= len; ++i) {
					BaseRecord r = (BaseRecord)data.getMem(i);
					for (int k = 0; k < keyCount; ++k) {
						keyValues[k] = r.getFieldValue(keyIndex[k]);
					}
					
					seqs[i] = searcher.findNext(keyValues);
				}
			}
		} else {
			recNum  = new long[len + 1];//子表对应到主表的伪号，0表示在主表补区
			ColPhyTable baseTable = (ColPhyTable) this.groupTable.baseTable;
			RecordSeqSearcher baseSearcher = new RecordSeqSearcher(baseTable);
			RecordSeqSearcher2 searcher = new RecordSeqSearcher2(this);
			if (keyCount == 1) {
				int k = keyIndex[0];
				for (int i = 1; i <= len; ++i) {
					BaseRecord r = (BaseRecord)data.getMem(i);
					seqs[i] = searcher.findNext(r.getFieldValue(k), temp);
					block[i] = temp[0];
					if (seqs[i] < 0) {
						//如果是插入，要判断一下是否在主表的列区
						long seq = baseSearcher.findNext(r.getFieldValue(k));
						if (seq > 0) {
							recNum[i] = seq;
						} else {
							if (baseSearcher.isEnd()) {
								//子表插入的数据必须在主表
								MessageManager mm = EngineMessage.get();
								throw new RQException(r.toString(null) + mm.getMessage("grouptable.invalidData"));	
							}
							recNum[i] = 0;//如果在主表补区，下面会处理
						}
					} else {
						recNum[i] = searcher.getRecNum();
					}
				}
			} else {
				Object []keyValues = new Object[keyCount];
				int baseKeyCount = sortedColStartIndex;
				Object []baseKeyValues = new Object[baseKeyCount];
				
				for (int i = 1; i <= len; ++i) {
					BaseRecord r = (BaseRecord)data.getMem(i);
					for (int k = 0; k < keyCount; ++k) {
						keyValues[k] = r.getFieldValue(keyIndex[k]);
						if (k < baseKeyCount) {
							baseKeyValues[k] = keyValues[k]; 
						}
					}
					
					seqs[i] = searcher.findNext(keyValues, temp);
					block[i] = temp[0];
					if (seqs[i] < 0 || block[i] > 0) {
						//如果是插入，要判断一下是否在主表的列区
						long seq = baseSearcher.findNext(baseKeyValues);
						if (seq > 0) {
							recNum[i] = seq;
						} else {
							if (baseSearcher.isEnd()) {
								//子表插入的数据必须在主表
								MessageManager mm = EngineMessage.get();
								throw new RQException(r.toString(null) + mm.getMessage("grouptable.invalidData"));	
							}
							recNum[i] = 0;//如果在主表补区，下面会处理
						}
					} else {
						recNum[i] = searcher.getRecNum();
					}
				}
			}
		}
		
		// 需要在最后插入的调用append追加
		Sequence append = new Sequence();
		ArrayList<ModifyRecord> modifyRecords = getModifyRecords();
		boolean needUpdateSubTable = false;
		
		if (modifyRecords == null) {
			modifyRecords = new ArrayList<ModifyRecord>(len);
			this.modifyRecords = modifyRecords;
			for (int i = 1; i <= len; ++i) {
				BaseRecord sr = (BaseRecord)data.getMem(i);
				if (seqs[i] > 0) {
					if (isUpdate) {
						ModifyRecord r = new ModifyRecord(seqs[i], ModifyRecord.STATE_UPDATE, sr.toRecord());
						modifyRecords.add(r);
						if (result != null) {
							result.add(sr);
						}
					}
				} else {
					append.add(sr);
				}
			}
		} else {
			int srcLen = modifyRecords.size();
			ArrayList<ModifyRecord> tmp = new ArrayList<ModifyRecord>(len + srcLen);
			int s = 0;
			int t = 1;
			
			while (s < srcLen && t <= len) {
				ModifyRecord mr = modifyRecords.get(s);
				long seq1 = mr.getRecordSeq();
				long seq2 = seqs[t];
				if (seq2 > 0) {
					if (seq1 < seq2) {
						s++;
						tmp.add(mr);
					} else if (seq1 == seq2) {
						if (mr.getState() == ModifyRecord.STATE_INSERT) {
							s++;
							tmp.add(mr);
						} else {
							if ((mr.getState() == ModifyRecord.STATE_UPDATE && isUpdate) || 
									(mr.getState() == ModifyRecord.STATE_DELETE && isInsert)) {
								// 状态都用update
								BaseRecord sr = (BaseRecord)data.getMem(t);
								mr.setRecord(sr.toRecord(), ModifyRecord.STATE_UPDATE);
								if (result != null) {
									result.add(sr);
								}
							}

							s++;
							t++;
							tmp.add(mr);
						}
					} else {
						if (isUpdate) {
							BaseRecord sr = (BaseRecord)data.getMem(t);
							mr = new ModifyRecord(seq2, ModifyRecord.STATE_UPDATE, sr.toRecord());
							tmp.add(mr);
							
							if (result != null) {
								result.add(sr);
							}
						}
						
						t++;
					}
				} else {
					seq2 = -seq2;
					if (seq1 < seq2) {
						s++;
						tmp.add(mr);
					} else if (seq1 == seq2) {
						if (mr.getState() == ModifyRecord.STATE_INSERT) {
							int cmp = mr.getRecord().compare((BaseRecord)data.getMem(t), keyIndex);
							if (cmp < 0) {
								s++;
								tmp.add(mr);
							} else if (cmp == 0) {
								if (isUpdate) {
									BaseRecord sr = (BaseRecord)data.getMem(t);
									mr.setRecord(sr.toRecord());
									if (result != null) {
										result.add(sr);
									}
								}
								
								tmp.add(mr);
								s++;
								t++;
							} else {
								append.add(data.getMem(t));
								t++;
							}
						} else {
							append.add(data.getMem(t));
							t++;
						}
					} else {
						append.add(data.getMem(t));
						t++;
					}
				}
			}
			
			for (; s < srcLen; ++s) {
				tmp.add(modifyRecords.get(s));
			}
			
			for (; t <= len; ++t) {
				BaseRecord sr = (BaseRecord)data.getMem(t);
				if (seqs[t] > 0) {
					if (isUpdate) {
						ModifyRecord r = new ModifyRecord(seqs[t], ModifyRecord.STATE_UPDATE, sr.toRecord());
						tmp.add(r);
						if (result != null) {
							result.add(sr);
						}
					}
				} else {
					append.add(sr);
				}
			}
			
			this.modifyRecords = tmp;
			if (srcLen != tmp.size()) {
				needUpdateSubTable = true;
			}
		}
		
		if (!isPrimaryTable) {
			//子表补区最后要根据主表补区修改
			update(parent.getModifyRecords());
			
			for (ModifyRecord r : modifyRecords) {
				if (r.getState() == ModifyRecord.STATE_INSERT) {
					if (r.getParentRecordSeq() == 0) {
						this.modifyRecords = null;
						this.modifyRecords = getModifyRecords();
						//子表插入的数据必须在主表
						MessageManager mm = EngineMessage.get();
						throw new RQException(r.getRecord().toString(null) + mm.getMessage("grouptable.invalidData"));
					}
				}
			}
			
		}
		
		if (isSave) {
			saveModifyRecords();
		}
		
		if (isPrimaryTable && needUpdateSubTable) {
			//主表有insert，就必须更新所有子表补区
			ArrayList<PhyTable> tableList = getTableList();
			for (int i = 0, size = tableList.size(); i < size; ++i) {
				ColPhyTable t = ((ColPhyTable)tableList.get(i));
				boolean needSave = t.update(modifyRecords);
				if (needSave) {
					t.saveModifyRecords();
				}
			}
		}
		
		if (append.length() > 0) {
			Sequence seq = stmd.update(append, opt);
			if (result != null) {
				result.addAll(seq);
			}
		}
		
		if (isSave) {
			groupTable.save();
		}
		
		return result;
	}
	
	/**
	 * 更新基表
	 */
	public Sequence update(Sequence data, String opt) throws IOException {
		if (data != null) {
			data = new Sequence(data);
		}
		
		if (!hasPrimaryKey) {
			//没有维时进行append
			boolean hasY = opt != null && opt.indexOf('y') != -1;
			if (hasY) {
				append_y(data);
			} else {
				append(new MemoryCursor(data));
			}
			return data;
		}
		
		ComTable groupTable = getGroupTable();
		groupTable.checkWritable();
		PhyTable tmd = getSupplementTable(false);
		if (tmd != null) {
			return update(tmd, data, opt);
		}
		
		boolean isInsert = true,isUpdate = true;
		Sequence result = null;
		if (opt != null) {
			if (opt.indexOf('y') != -1) {
				return update_y(data, opt);
			}
			
			if (opt.indexOf('i') != -1) isUpdate = false;
			if (opt.indexOf('u') != -1) {
				if (!isUpdate) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(opt + mm.getMessage("engine.optConflict"));
				}
				
				isInsert = false;
			}
			
			if (opt.indexOf('n') != -1) result = new Sequence();
		}
		
		long totalRecordCount = this.totalRecordCount;
		if (totalRecordCount == 0) {
			if (isInsert) {
				ICursor cursor = new MemoryCursor(data);
				append(cursor);
				appendCache();
				if (result != null) {
					result.addAll(data);
				}
			}
			
			return result;
		}
		
		DataStruct ds = data.dataStruct();
		if (ds == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.needPurePmt"));
		}
		
		if (!ds.isCompatible(this.ds)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.dsNotMatch"));
		}
		
		// 对更新数据进行排序
		data.sortFields(getAllSortedColNames());
		appendCache();
		
		ColumnMetaData[] columns = getAllSortedColumns();
		int keyCount = columns.length;
		int []keyIndex = new int[keyCount];
		for (int k = 0; k < keyCount; ++k) {
			keyIndex[k] = ds.getFieldIndex(columns[k].getColName());
			if (keyIndex[k] < 0) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(columns[k].getColName() + mm.getMessage("ds.fieldNotExist"));
			}
		}
		
		boolean isPrimaryTable = parent == null;
		int len = data.length();
		long []seqs = new long[len + 1];
		int []block = new int[len + 1];//是否在一个段的底部insert(子表)
		long []recNum = null;
		int []temp = new int[1];
		
		if (isPrimaryTable) {
			RecordSeqSearcher searcher = new RecordSeqSearcher(this);
			
			if (keyCount == 1) {
				int k = keyIndex[0];
				for (int i = 1; i <= len; ++i) {
					BaseRecord r = (BaseRecord)data.getMem(i);
					seqs[i] = searcher.findNext(r.getFieldValue(k));
				}
			} else {
				Object []keyValues = new Object[keyCount];
				for (int i = 1; i <= len; ++i) {
					BaseRecord r = (BaseRecord)data.getMem(i);
					for (int k = 0; k < keyCount; ++k) {
						keyValues[k] = r.getFieldValue(keyIndex[k]);
					}
					
					seqs[i] = searcher.findNext(keyValues);
				}
			}
		} else {
			recNum  = new long[len + 1];//子表对应到主表的伪号，0表示在主表补区
			ColPhyTable baseTable = (ColPhyTable) this.groupTable.baseTable;
			RecordSeqSearcher baseSearcher = new RecordSeqSearcher(baseTable);
			RecordSeqSearcher2 searcher = new RecordSeqSearcher2(this);
			if (keyCount == 1) {
				int k = keyIndex[0];
				for (int i = 1; i <= len; ++i) {
					BaseRecord r = (BaseRecord)data.getMem(i);
					seqs[i] = searcher.findNext(r.getFieldValue(k), temp);
					block[i] = temp[0];
					if (seqs[i] < 0) {
						//如果是插入，要判断一下是否在主表的列区
						long seq = baseSearcher.findNext(r.getFieldValue(k));
						if (seq > 0) {
							recNum[i] = seq;
						} else {
							if (baseSearcher.isEnd()) {
								//子表插入的数据必须在主表
								MessageManager mm = EngineMessage.get();
								throw new RQException(r.toString(null) + mm.getMessage("grouptable.invalidData"));	
							}
							recNum[i] = 0;//如果在主表补区，下面会处理
						}
					} else {
						recNum[i] = searcher.getRecNum();
					}
				}
			} else {
				Object []keyValues = new Object[keyCount];
				int baseKeyCount = sortedColStartIndex;
				Object []baseKeyValues = new Object[baseKeyCount];
				
				for (int i = 1; i <= len; ++i) {
					BaseRecord r = (BaseRecord)data.getMem(i);
					for (int k = 0; k < keyCount; ++k) {
						keyValues[k] = r.getFieldValue(keyIndex[k]);
						if (k < baseKeyCount) {
							baseKeyValues[k] = keyValues[k]; 
						}
					}
					
					seqs[i] = searcher.findNext(keyValues, temp);
					block[i] = temp[0];
					if (seqs[i] < 0 || block[i] > 0) {
						//如果是插入，要判断一下是否在主表的列区
						long seq = baseSearcher.findNext(baseKeyValues);
						if (seq > 0) {
							recNum[i] = seq;
						} else {
							if (baseSearcher.isEnd()) {
								//子表插入的数据必须在主表
								MessageManager mm = EngineMessage.get();
								throw new RQException(r.toString(null) + mm.getMessage("grouptable.invalidData"));	
							}
							recNum[i] = 0;//如果在主表补区，下面会处理
						}
					} else {
						recNum[i] = searcher.getRecNum();
					}
				}
			}
		}
		
		// 需要在最后插入的调用append追加
		Sequence append = new Sequence();
		ArrayList<ModifyRecord> modifyRecords = getModifyRecords();
		boolean needUpdateSubTable = false;
		
		if (modifyRecords == null) {
			modifyRecords = new ArrayList<ModifyRecord>(len);
			this.modifyRecords = modifyRecords;
			for (int i = 1; i <= len; ++i) {
				BaseRecord sr = (BaseRecord)data.getMem(i);
				if (seqs[i] > 0) {
					if (isUpdate) {
						ModifyRecord r = new ModifyRecord(seqs[i], ModifyRecord.STATE_UPDATE, sr.toRecord());
						modifyRecords.add(r);
						if (result != null) {
							result.add(sr);
						}
					}
				} else if (isInsert) {
					long seq = -seqs[i];
					if (seq <= totalRecordCount || block[i] > 0) {
						ModifyRecord r = new ModifyRecord(seq, ModifyRecord.STATE_INSERT, sr.toRecord());
						r.setBlock(block[i]);
						//如果是子表insert 要处理parentRecordSeq，因为子表insert的可能指向主表列区
						//这里先设置为指向列区伪号，最后会根据主表补区修改
						if (!isPrimaryTable) {
							r.setParentRecordSeq(recNum[i]);
						}
						modifyRecords.add(r);
					} else {
						append.add(sr);
					}
					
					if (result != null) {
						result.add(sr);
					}
				}
			}
		} else {
			int srcLen = modifyRecords.size();
			ArrayList<ModifyRecord> tmp = new ArrayList<ModifyRecord>(len + srcLen);
			int s = 0;
			int t = 1;
			
			while (s < srcLen && t <= len) {
				ModifyRecord mr = modifyRecords.get(s);
				long seq1 = mr.getRecordSeq();
				long seq2 = seqs[t];
				if (seq2 > 0) {
					if (seq1 < seq2) {
						s++;
						tmp.add(mr);
					} else if (seq1 == seq2) {
						if (mr.getState() == ModifyRecord.STATE_INSERT) {
							s++;
							tmp.add(mr);
						} else {
							if ((mr.getState() == ModifyRecord.STATE_UPDATE && isUpdate) || 
									(mr.getState() == ModifyRecord.STATE_DELETE && isInsert)) {
								// 状态都用update
								BaseRecord sr = (BaseRecord)data.getMem(t);
								mr.setRecord(sr.toRecord(), ModifyRecord.STATE_UPDATE);
								if (result != null) {
									result.add(sr);
								}
							}

							s++;
							t++;
							tmp.add(mr);
						}
					} else {
						if (isUpdate) {
							BaseRecord sr = (BaseRecord)data.getMem(t);
							mr = new ModifyRecord(seq2, ModifyRecord.STATE_UPDATE, sr.toRecord());
							tmp.add(mr);
							
							if (result != null) {
								result.add(sr);
							}
						}
						
						t++;
					}
				} else {
					seq2 = -seq2;
					if (seq1 < seq2) {
						s++;
						tmp.add(mr);
					} else if (seq1 == seq2) {
						if (mr.getState() == ModifyRecord.STATE_INSERT) {
							int cmp = mr.getRecord().compare((BaseRecord)data.getMem(t), keyIndex);
							if (cmp < 0) {
								s++;
								tmp.add(mr);
							} else if (cmp == 0) {
								if (isUpdate) {
									BaseRecord sr = (BaseRecord)data.getMem(t);
									mr.setRecord(sr.toRecord());
									if (result != null) {
										result.add(sr);
									}
								}
								
								tmp.add(mr);
								s++;
								t++;
							} else {
								if (isInsert) {
									BaseRecord sr = (BaseRecord)data.getMem(t);
									mr = new ModifyRecord(seq2, ModifyRecord.STATE_INSERT, sr.toRecord());
									mr.setBlock(block[t]);
									//如果是子表insert 要处理parentRecordSeq，因为子表insert的可能指向主表列区
									//这里先设置为指向列区伪号，最后会根据主表补区修改
									if (!isPrimaryTable) {
										mr.setParentRecordSeq(recNum[t]);
									}
									modifyRecords.add(mr);
									tmp.add(mr);
									if (result != null) {
										result.add(sr);
									}
								}
								
								t++;
							}
						} else {
							if (isInsert) {
								BaseRecord sr = (BaseRecord)data.getMem(t);
								mr = new ModifyRecord(seq2, ModifyRecord.STATE_INSERT, sr.toRecord());
								mr.setBlock(block[t]);
								//如果是子表insert 要处理parentRecordSeq，因为子表insert的可能指向主表列区
								//这里先设置为指向列区伪号，最后会根据主表补区修改
								if (!isPrimaryTable) {
									mr.setParentRecordSeq(recNum[t]);
								}
								modifyRecords.add(mr);
								tmp.add(mr);
								if (result != null) {
									result.add(sr);
								}
							}
							
							t++;
						}
					} else {
						if (isInsert) {
							BaseRecord sr = (BaseRecord)data.getMem(t);
							mr = new ModifyRecord(seq2, ModifyRecord.STATE_INSERT, sr.toRecord());
							mr.setBlock(block[t]);
							//如果是子表insert 要处理parentRecordSeq，因为子表insert的可能指向主表列区
							//这里先设置为指向列区伪号，最后会根据主表补区修改
							if (!isPrimaryTable) {
								mr.setParentRecordSeq(recNum[t]);
							}
							modifyRecords.add(mr);
							tmp.add(mr);
							if (result != null) {
								result.add(sr);
							}
						}
						
						t++;
					}
				}
			}
			
			for (; s < srcLen; ++s) {
				tmp.add(modifyRecords.get(s));
			}
			
			for (; t <= len; ++t) {
				BaseRecord sr = (BaseRecord)data.getMem(t);
				if (seqs[t] > 0) {
					if (isUpdate) {
						ModifyRecord r = new ModifyRecord(seqs[t], ModifyRecord.STATE_UPDATE, sr.toRecord());
						tmp.add(r);
						if (result != null) {
							result.add(sr);
						}
					}
				} else if (isInsert) {
					long seq = -seqs[t];
					if (seq <= totalRecordCount) {
						ModifyRecord r = new ModifyRecord(seq, ModifyRecord.STATE_INSERT, sr.toRecord());
						r.setBlock(block[t]);
						//如果是子表insert 要处理parentRecordSeq，因为子表insert的可能指向主表列区
						//这里先设置为指向列区伪号，最后会根据主表补区修改
						if (!isPrimaryTable) {
							r.setParentRecordSeq(recNum[t]);
						}
						modifyRecords.add(r);
						tmp.add(r);
					} else {
						append.add(sr);
					}
					
					if (result != null) {
						result.add(sr);
					}
				}
			}
			
			this.modifyRecords = tmp;
			if (srcLen != tmp.size()) {
				needUpdateSubTable = true;
			}
		}
		
		if (!isPrimaryTable) {
			//子表补区最后要根据主表补区修改
			update(parent.getModifyRecords());
			
			for (ModifyRecord r : modifyRecords) {
				if (r.getState() == ModifyRecord.STATE_INSERT) {
					if (r.getParentRecordSeq() == 0) {
						this.modifyRecords = null;
						this.modifyRecords = getModifyRecords();
						//子表插入的数据必须在主表
						MessageManager mm = EngineMessage.get();
						throw new RQException(r.getRecord().toString(null) + mm.getMessage("grouptable.invalidData"));
					}
				}
			}
			
		}
		
		saveModifyRecords();
		
		if (isPrimaryTable && needUpdateSubTable) {
			//主表有insert，就必须更新所有子表补区
			ArrayList<PhyTable> tableList = getTableList();
			for (int i = 0, size = tableList.size(); i < size; ++i) {
				ColPhyTable t = ((ColPhyTable)tableList.get(i));
				boolean needSave = t.update(modifyRecords);
				if (needSave) {
					t.saveModifyRecords();
				}
			}
		}
		
		if (append.length() > 0) {
			ICursor cursor = new MemoryCursor(append);
			append(cursor);
			appendCache();
		} else {
			groupTable.save();
		}
		
		return result;
	}
	
	// 融合到内存中的补区，不写入外存
	private void append_y(Sequence data) throws IOException {
		if (data == null || data.length() == 0) {
			return;
		}
		
		DataStruct ds = data.dataStruct();
		if (ds == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.needPurePmt"));
		}
		
		if (!ds.isCompatible(this.ds)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.dsNotMatch"));
		}
		
		int len = data.length();
		ArrayList<ModifyRecord> modifyRecords = getModifyRecords();
		if (modifyRecords == null) {
			modifyRecords = new ArrayList<ModifyRecord>(len);
			this.modifyRecords = modifyRecords;
		}
				
		BaseRecord r1 = (BaseRecord)data.get(1);
		String []pks = getAllSortedColNames();
		int keyCount = pks == null ? 0 : pks.length;
		int []keyIndex = new int[keyCount];
		for (int i = 0; i < keyCount; ++i) {
			keyIndex[i] = ds.getFieldIndex(pks[i]);
		}
		
		if (keyCount > 0 && maxValues != null && r1.compare(keyIndex, maxValues) < 0) {
			// 需要归并
			long []seqs = new long[len + 1];
			RecordSeqSearcher searcher = new RecordSeqSearcher(this);
			
			if (keyCount == 1) {
				int k = keyIndex[0];
				for (int i = 1; i <= len; ++i) {
					BaseRecord r = (BaseRecord)data.getMem(i);
					seqs[i] = searcher.findNext(r.getFieldValue(k));
				}
			} else {
				Object []keyValues = new Object[keyCount];
				for (int i = 1; i <= len; ++i) {
					BaseRecord r = (BaseRecord)data.getMem(i);
					for (int k = 0; k < keyCount; ++k) {
						keyValues[k] = r.getFieldValue(keyIndex[k]);
					}
					
					seqs[i] = searcher.findNext(keyValues);
				}
			}
			
			for (int i = 1; i <= len; ++i) {
				BaseRecord sr = (BaseRecord)data.getMem(i);
				if (seqs[i] > 0) {
					ModifyRecord r = new ModifyRecord(seqs[i], ModifyRecord.STATE_INSERT, sr.toRecord());
					modifyRecords.add(r);
				} else {
					ModifyRecord r = new ModifyRecord(-seqs[i], ModifyRecord.STATE_INSERT, sr.toRecord());
					modifyRecords.add(r);
				}
			}
		} else {
			long seq = totalRecordCount + 1;
			for (int i = 1; i <= len; ++i) {
				BaseRecord sr = (BaseRecord)data.getMem(i);
				ModifyRecord r = new ModifyRecord(seq, ModifyRecord.STATE_INSERT, sr.toRecord());
				modifyRecords.add(r);
			}
		}
	}

	// 融合到内存中的补区，不写入外存
	private Sequence update_y(Sequence data, String opt) throws IOException {
		boolean isInsert = true,isUpdate = true;
		Sequence result = null;
		if (opt != null) {
			if (opt.indexOf('i') != -1) isUpdate = false;
			if (opt.indexOf('u') != -1) {
				if (!isUpdate) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(opt + mm.getMessage("engine.optConflict"));
				}
				
				isInsert = false;
			}
			
			if (opt.indexOf('n') != -1) result = new Sequence();
		}
		
		long totalRecordCount = this.totalRecordCount;
		DataStruct ds = data.dataStruct();
		if (ds == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.needPurePmt"));
		}
		
		if (!ds.isCompatible(this.ds)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.dsNotMatch"));
		}
		
		// 对更新数据进行排序
		data.sortFields(getAllSortedColNames());
		appendCache();
		
		ColumnMetaData[] columns = getAllSortedColumns();
		int keyCount = columns.length;
		int []keyIndex = new int[keyCount];
		for (int k = 0; k < keyCount; ++k) {
			keyIndex[k] = ds.getFieldIndex(columns[k].getColName());
			if (keyIndex[k] < 0) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(columns[k].getColName() + mm.getMessage("ds.fieldNotExist"));
			}
		}
		
		boolean isPrimaryTable = parent == null;
		int len = data.length();
		long []seqs = new long[len + 1];
		int []block = new int[len + 1];//是否在一个段的底部insert(子表)
		long []recNum = null;
		int []temp = new int[1];
		
		if (isPrimaryTable) {
			RecordSeqSearcher searcher = new RecordSeqSearcher(this);
			
			if (keyCount == 1) {
				int k = keyIndex[0];
				for (int i = 1; i <= len; ++i) {
					BaseRecord r = (BaseRecord)data.getMem(i);
					seqs[i] = searcher.findNext(r.getFieldValue(k));
				}
			} else {
				Object []keyValues = new Object[keyCount];
				for (int i = 1; i <= len; ++i) {
					BaseRecord r = (BaseRecord)data.getMem(i);
					for (int k = 0; k < keyCount; ++k) {
						keyValues[k] = r.getFieldValue(keyIndex[k]);
					}
					
					seqs[i] = searcher.findNext(keyValues);
				}
			}
		} else {
			recNum  = new long[len + 1];//子表对应到主表的伪号，0表示在主表补区
			ColPhyTable baseTable = (ColPhyTable) this.groupTable.baseTable;
			RecordSeqSearcher baseSearcher = new RecordSeqSearcher(baseTable);
			RecordSeqSearcher2 searcher = new RecordSeqSearcher2(this);
			if (keyCount == 1) {
				int k = keyIndex[0];
				for (int i = 1; i <= len; ++i) {
					BaseRecord r = (BaseRecord)data.getMem(i);
					seqs[i] = searcher.findNext(r.getFieldValue(k), temp);
					block[i] = temp[0];
					if (seqs[i] < 0) {
						//如果是插入，要判断一下是否在主表的列区
						long seq = baseSearcher.findNext(r.getFieldValue(k));
						if (seq > 0) {
							recNum[i] = seq;
						} else {
							if (baseSearcher.isEnd()) {
								//子表插入的数据必须在主表
								MessageManager mm = EngineMessage.get();
								throw new RQException(r.toString(null) + mm.getMessage("grouptable.invalidData"));	
							}
							recNum[i] = 0;//如果在主表补区，下面会处理
						}
					} else {
						recNum[i] = searcher.getRecNum();
					}
				}
			} else {
				Object []keyValues = new Object[keyCount];
				int baseKeyCount = sortedColStartIndex;
				Object []baseKeyValues = new Object[baseKeyCount];
				
				for (int i = 1; i <= len; ++i) {
					BaseRecord r = (BaseRecord)data.getMem(i);
					for (int k = 0; k < keyCount; ++k) {
						keyValues[k] = r.getFieldValue(keyIndex[k]);
						if (k < baseKeyCount) {
							baseKeyValues[k] = keyValues[k]; 
						}
					}
					
					seqs[i] = searcher.findNext(keyValues, temp);
					block[i] = temp[0];
					if (seqs[i] < 0 || block[i] > 0) {
						//如果是插入，要判断一下是否在主表的列区
						long seq = baseSearcher.findNext(baseKeyValues);
						if (seq > 0) {
							recNum[i] = seq;
						} else {
							if (baseSearcher.isEnd()) {
								//子表插入的数据必须在主表
								MessageManager mm = EngineMessage.get();
								throw new RQException(r.toString(null) + mm.getMessage("grouptable.invalidData"));	
							}
							recNum[i] = 0;//如果在主表补区，下面会处理
						}
					} else {
						recNum[i] = searcher.getRecNum();
					}
				}
			}
		}
		
		ArrayList<ModifyRecord> modifyRecords = getModifyRecords();
		boolean needUpdateSubTable = false;
		
		if (modifyRecords == null) {
			modifyRecords = new ArrayList<ModifyRecord>(len);
			this.modifyRecords = modifyRecords;
			for (int i = 1; i <= len; ++i) {
				BaseRecord sr = (BaseRecord)data.getMem(i);
				if (seqs[i] > 0) {
					if (isUpdate) {
						ModifyRecord r = new ModifyRecord(seqs[i], ModifyRecord.STATE_UPDATE, sr.toRecord());
						modifyRecords.add(r);
						if (result != null) {
							result.add(sr);
						}
					}
				} else if (isInsert) {
					long seq = -seqs[i];
					if (seq <= totalRecordCount || block[i] > 0) {
						ModifyRecord r = new ModifyRecord(seq, ModifyRecord.STATE_INSERT, sr.toRecord());
						r.setBlock(block[i]);
						//如果是子表insert 要处理parentRecordSeq，因为子表insert的可能指向主表列区
						//这里先设置为指向列区伪号，最后会根据主表补区修改
						if (!isPrimaryTable) {
							r.setParentRecordSeq(recNum[i]);
						}
						modifyRecords.add(r);
					} else {
						ModifyRecord r = new ModifyRecord(seq, ModifyRecord.STATE_INSERT, sr.toRecord());
						r.setBlock(block[i]);
						//如果是子表insert 要处理parentRecordSeq，因为子表insert的可能指向主表列区
						//这里先设置为指向列区伪号，最后会根据主表补区修改
						if (!isPrimaryTable) {
							r.setParentRecordSeq(recNum[i]);
						}
						
						modifyRecords.add(r);
					}
					
					if (result != null) {
						result.add(sr);
					}
				}
			}
		} else {
			int srcLen = modifyRecords.size();
			ArrayList<ModifyRecord> tmp = new ArrayList<ModifyRecord>(len + srcLen);
			int s = 0;
			int t = 1;
			
			while (s < srcLen && t <= len) {
				ModifyRecord mr = modifyRecords.get(s);
				long seq1 = mr.getRecordSeq();
				long seq2 = seqs[t];
				if (seq2 > 0) {
					if (seq1 < seq2) {
						s++;
						tmp.add(mr);
					} else if (seq1 == seq2) {
						if (mr.getState() == ModifyRecord.STATE_INSERT) {
							s++;
							tmp.add(mr);
						} else {
							if ((mr.getState() == ModifyRecord.STATE_UPDATE && isUpdate) || 
									(mr.getState() == ModifyRecord.STATE_DELETE && isInsert)) {
								// 状态都用update
								BaseRecord sr = (BaseRecord)data.getMem(t);
								mr.setRecord(sr.toRecord(), ModifyRecord.STATE_UPDATE);
								if (result != null) {
									result.add(sr);
								}
							}

							s++;
							t++;
							tmp.add(mr);
						}
					} else {
						if (isUpdate) {
							BaseRecord sr = (BaseRecord)data.getMem(t);
							mr = new ModifyRecord(seq2, ModifyRecord.STATE_UPDATE, sr.toRecord());
							tmp.add(mr);
							
							if (result != null) {
								result.add(sr);
							}
						}
						
						t++;
					}
				} else {
					seq2 = -seq2;
					if (seq1 < seq2) {
						s++;
						tmp.add(mr);
					} else if (seq1 == seq2) {
						if (mr.getState() == ModifyRecord.STATE_INSERT) {
							int cmp = mr.getRecord().compare((BaseRecord)data.getMem(t), keyIndex);
							if (cmp < 0) {
								s++;
								tmp.add(mr);
							} else if (cmp == 0) {
								if (isUpdate) {
									BaseRecord sr = (BaseRecord)data.getMem(t);
									mr.setRecord(sr.toRecord());
									if (result != null) {
										result.add(sr);
									}
								}
								
								tmp.add(mr);
								s++;
								t++;
							} else {
								if (isInsert) {
									BaseRecord sr = (BaseRecord)data.getMem(t);
									mr = new ModifyRecord(seq2, ModifyRecord.STATE_INSERT, sr.toRecord());
									mr.setBlock(block[t]);
									//如果是子表insert 要处理parentRecordSeq，因为子表insert的可能指向主表列区
									//这里先设置为指向列区伪号，最后会根据主表补区修改
									if (!isPrimaryTable) {
										mr.setParentRecordSeq(recNum[t]);
									}
									modifyRecords.add(mr);
									tmp.add(mr);
									if (result != null) {
										result.add(sr);
									}
								}
								
								t++;
							}
						} else {
							if (isInsert) {
								BaseRecord sr = (BaseRecord)data.getMem(t);
								mr = new ModifyRecord(seq2, ModifyRecord.STATE_INSERT, sr.toRecord());
								mr.setBlock(block[t]);
								//如果是子表insert 要处理parentRecordSeq，因为子表insert的可能指向主表列区
								//这里先设置为指向列区伪号，最后会根据主表补区修改
								if (!isPrimaryTable) {
									mr.setParentRecordSeq(recNum[t]);
								}
								modifyRecords.add(mr);
								tmp.add(mr);
								if (result != null) {
									result.add(sr);
								}
							}
							
							t++;
						}
					} else {
						if (isInsert) {
							BaseRecord sr = (BaseRecord)data.getMem(t);
							mr = new ModifyRecord(seq2, ModifyRecord.STATE_INSERT, sr.toRecord());
							mr.setBlock(block[t]);
							//如果是子表insert 要处理parentRecordSeq，因为子表insert的可能指向主表列区
							//这里先设置为指向列区伪号，最后会根据主表补区修改
							if (!isPrimaryTable) {
								mr.setParentRecordSeq(recNum[t]);
							}
							modifyRecords.add(mr);
							tmp.add(mr);
							if (result != null) {
								result.add(sr);
							}
						}
						
						t++;
					}
				}
			}
			
			for (; s < srcLen; ++s) {
				tmp.add(modifyRecords.get(s));
			}
			
			for (; t <= len; ++t) {
				BaseRecord sr = (BaseRecord)data.getMem(t);
				if (seqs[t] > 0) {
					if (isUpdate) {
						ModifyRecord r = new ModifyRecord(seqs[t], ModifyRecord.STATE_UPDATE, sr.toRecord());
						tmp.add(r);
						if (result != null) {
							result.add(sr);
						}
					}
				} else if (isInsert) {
					long seq = -seqs[t];
					if (seq <= totalRecordCount) {
						ModifyRecord r = new ModifyRecord(seq, ModifyRecord.STATE_INSERT, sr.toRecord());
						r.setBlock(block[t]);
						//如果是子表insert 要处理parentRecordSeq，因为子表insert的可能指向主表列区
						//这里先设置为指向列区伪号，最后会根据主表补区修改
						if (!isPrimaryTable) {
							r.setParentRecordSeq(recNum[t]);
						}
						modifyRecords.add(r);
						tmp.add(r);
					} else {
						ModifyRecord r = new ModifyRecord(seq, ModifyRecord.STATE_INSERT, sr.toRecord());
						r.setBlock(block[t]);
						//如果是子表insert 要处理parentRecordSeq，因为子表insert的可能指向主表列区
						//这里先设置为指向列区伪号，最后会根据主表补区修改
						if (!isPrimaryTable) {
							r.setParentRecordSeq(recNum[t]);
						}
						
						modifyRecords.add(r);
						tmp.add(r);
					}
					
					if (result != null) {
						result.add(sr);
					}
				}
			}
			
			this.modifyRecords = tmp;
			if (srcLen != tmp.size()) {
				needUpdateSubTable = true;
			}
		}
		
		if (!isPrimaryTable) {
			//子表补区最后要根据主表补区修改
			update(parent.getModifyRecords());
			
			for (ModifyRecord r : modifyRecords) {
				if (r.getState() == ModifyRecord.STATE_INSERT) {
					if (r.getParentRecordSeq() == 0) {
						this.modifyRecords = null;
						this.modifyRecords = getModifyRecords();
						//子表插入的数据必须在主表
						MessageManager mm = EngineMessage.get();
						throw new RQException(r.getRecord().toString(null) + mm.getMessage("grouptable.invalidData"));
					}
				}
			}
			
		}
		
		//saveModifyRecords();
		
		if (isPrimaryTable && needUpdateSubTable) {
			//主表有insert，就必须更新所有子表补区
			ArrayList<PhyTable> tableList = getTableList();
			for (int i = 0, size = tableList.size(); i < size; ++i) {
				ColPhyTable t = ((ColPhyTable)tableList.get(i));
				boolean needSave = t.update(modifyRecords);
				if (needSave) {
					t.saveModifyRecords();
				}
			}
		}
		
		//if (append.length() > 0) {
		//	ICursor cursor = new MemoryCursor(append);
		//	append(cursor);
		//} else {
		//	groupTable.save();
		//}
				
		return result;
	}
	
	/**
	 * 重写一些列的数据
	 * 注意：输入的数据要保证原序。这里不处理分段，所有的分段都按照原来的。
	 * @param cursor 要写入的数据
	 * @param opt	选项
	 * @throws IOException
	 */
	public void update(ICursor cursor, String opt) throws IOException {
		/**
		 * 根据cursor数据的结构，获得要更新的列
		 */
		Sequence temp = cursor.peek(1);
		String[] fields = ((BaseRecord)temp.getMem(1)).getFieldNames();
		ColumnMetaData[] columns = getColumns(fields);
		
		/**
		 * 获得目前每个分段的记录条数
		 */
		BlockLinkReader rowCountReader = getSegmentReader();
		int blockCount = getDataBlockCount();
		long recordCountArray[] = new long[blockCount];
		try {
			for (int i = 0; i < blockCount; ++i) {
				recordCountArray[i]  = rowCountReader.readInt32();
			}
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				rowCountReader.close();
			} catch (Exception e){};
		}
		
		/**
		 * 把要更新的列清空
		 */
		for (ColumnMetaData col : columns) {
			BlockLink blockLink = col.getSegmentBlockLink();//分段信息块
			blockLink.setFirstBlockPos(blockLink.firstBlockPos);
			blockLink.freeIndex = 0;
			
			blockLink = col.getDataBlockLink();//数据块
			blockLink.setFirstBlockPos(blockLink.firstBlockPos);
			blockLink.freeIndex = 0;
			
			col.getDict().clear();
			col.initDictArray();
		}
		
		/**
		 * 写前变量的初始化
		 */
		for (ColumnMetaData col : columns) {
			col.prepareWrite();
		}
		int columnCount = columns.length;
		BufferWriter bufferWriters[] = new BufferWriter[columnCount];
		Object []minValues = new Object[columnCount];
		Object []maxValues = new Object[columnCount];
		Object []startValues = new Object[columnCount];
		int[] dataTypeInfo = new int[columnCount];
		
		/**
		 * 循环写入新数据
		 */
		for (long count : recordCountArray) {
			Sequence data = cursor.fetch((int) count);
			int end = data.length();
			for (int i = 0; i < columnCount; i++) {
				//写新数据到每个列块
				bufferWriters[i] = columns[i].getColDataBufferWriter();
				Sequence dict = columns[i].getDict();
				DataBlockWriterJob.writeDataBlock(bufferWriters[i], data, dict, i, 1, end, 
						maxValues, minValues, startValues, dataTypeInfo);
				
				//统计列数据类型
				boolean doCheck = groupTable.isCheckDataPure();
				columns[i].adjustDataType(dataTypeInfo[i], doCheck);
				columns[i].initDictArray();
				
				//提交每个列块buffer
				columns[i].appendColBlock(bufferWriters[i].finish(), minValues[i], maxValues[i], startValues[i]);
			}
			
		}
		
		/**
		 * 提交列信息
		 */
		for (ColumnMetaData col : columns) {
			col.finishWrite();
		}
		groupTable.save();
		updateIndex();
	}
	
	/** 按照data里数据的维值，删除指定的记录
	 * @param data 
	 * @param opt
	 */
	public Sequence delete(Sequence data, String opt) throws IOException {
		boolean deleteByBaseKey = false;//只用于内部删除子表的列区，此时不会有@n
		if (opt != null && opt.indexOf('s') != -1) {
			deleteByBaseKey = true;
		}
		
		if (!hasPrimaryKey && !deleteByBaseKey) {
			//没有维时不处理
			return null;
		}
		
		if (data != null) {
			data = new Sequence((Sequence)data);
		}
		
		ComTable groupTable = getGroupTable();
		groupTable.checkWritable();
		
		Sequence result1 = null;
		PhyTable tmd = getSupplementTable(false);
		if (tmd != null) {
			// 有补文件时先删除补文件中的数据，补文件中不存在的再在源文件中删除
			result1 = tmd.delete(data, "n");
			data = (Sequence) data.diff(result1, false);
		}
		
		appendCache();
		boolean nopt = false, isSave = true;
		if (opt != null) {
			if (opt.indexOf('n') != -1) {
				nopt = true;
			}
			
			if (opt.indexOf('y') != -1) {
				isSave = false;
			}
		}
		
		long totalRecordCount = this.totalRecordCount;
		if (totalRecordCount == 0 || data == null || data.length() == 0) {
			return nopt ? result1 : null;
		}
		
		Sequence result = null;
		if (nopt) {
			result = new Sequence();
		}
		
		DataStruct ds = data.dataStruct();
		if (ds == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.needPurePmt"));
		}
				
		ColumnMetaData[] columns = getAllSortedColumns();
		int keyCount = columns.length;
		if (deleteByBaseKey) {
			keyCount = sortedColStartIndex;
		}
		
		int []keyIndex = new int[keyCount];
		for (int k = 0; k < keyCount; ++k) {
			keyIndex[k] = ds.getFieldIndex(columns[k].getColName());
			if (keyIndex[k] < 0) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(columns[k].getColName() + mm.getMessage("ds.fieldNotExist"));
			}
		}
		
		boolean isPrimaryTable = parent == null;
		if (deleteByBaseKey) {
			data.sortFields(parent.getSortedColNames());
		} else {
			data.sortFields(getAllSortedColNames());
		}
		
		int len = data.length();
		long []seqs = new long[len + 1];
		int temp[] = new int[1];
		LongArray seqList = null;
		Sequence seqListData = null;
		if (deleteByBaseKey) {
			seqList = new LongArray(len * 10);
			seqListData = new Sequence(len);
		}
		
		if (isPrimaryTable) {
			RecordSeqSearcher searcher = new RecordSeqSearcher(this);
			
			if (keyCount == 1) {
				int k = keyIndex[0];
				for (int i = 1; i <= len; ++i) {
					BaseRecord r = (BaseRecord)data.getMem(i);
					seqs[i] = searcher.findNext(r.getFieldValue(k));
				}
			} else {
				Object []keyValues = new Object[keyCount];
				for (int i = 1; i <= len; ++i) {
					BaseRecord r = (BaseRecord)data.getMem(i);
					for (int k = 0; k < keyCount; ++k) {
						keyValues[k] = r.getFieldValue(keyIndex[k]);
					}
					
					seqs[i] = searcher.findNext(keyValues);
				}
			}
		} else {
			RecordSeqSearcher2 searcher = new RecordSeqSearcher2(this);
			Object []keyValues = new Object[keyCount];
			int baseKeyCount = sortedColStartIndex;
			Object []baseKeyValues = new Object[baseKeyCount];
			
			for (int i = 1; i <= len;) {
				BaseRecord r = (BaseRecord)data.getMem(i);
				for (int k = 0; k < keyCount; ++k) {
					keyValues[k] = r.getFieldValue(keyIndex[k]);
					if (k < baseKeyCount) {
						baseKeyValues[k] = keyValues[k]; 
					}
				}
				
				if (deleteByBaseKey) {
					long s = searcher.findNext(keyValues, keyCount);
					if (s <= 0) {
						i++;//当找不到时才++
					} else {
						seqList.add(s);
						seqListData.add(r);
					}

				} else {
					seqs[i] = searcher.findNext(keyValues, temp);
					i++;
				}
				
			}
		}
		
		if (deleteByBaseKey) {
			len = seqList.size();
			if (0 == len) {
				return result;
			}
			
			seqs = seqList.getDatas();
			data = seqListData;
		}
		
		ArrayList<ModifyRecord> modifyRecords = getModifyRecords();
		boolean modified = true;
		
		if (modifyRecords == null) {
			modifyRecords = new ArrayList<ModifyRecord>(len);
			for (int i = 1; i <= len; ++i) {
				if (seqs[i] > 0) {
					ModifyRecord r = new ModifyRecord(seqs[i]);
					modifyRecords.add(r);
					
					if (result != null) {
						result.add(data.getMem(i));
					}
				}
			}
			
			if (modifyRecords.size() > 0) {
				this.modifyRecords = modifyRecords;
			} else {
				modified = false;
			}
		} else {
			int srcLen = modifyRecords.size();
			ArrayList<ModifyRecord> tmp = new ArrayList<ModifyRecord>(len + srcLen);
			int s = 0;
			int t = 1;
			
			while (s < srcLen && t <= len) {
				ModifyRecord mr = modifyRecords.get(s);
				long seq1 = mr.getRecordSeq();
				long seq2 = seqs[t];
				if (seq2 > 0) {
					if (seq1 < seq2) {
						s++;
					} else if (seq1 == seq2) {
						if (mr.getState() == ModifyRecord.STATE_INSERT) {
							int cmp = mr.getRecord().compare((BaseRecord)data.getMem(t), keyIndex);
							if (cmp < 0) {
								tmp.add(mr);
							} else if (cmp == 0) {
								if (result != null) {
									result.add(data.getMem(t));
								}
							} else {
								if (result != null) {
									result.add(data.getMem(t));
								}

								ModifyRecord r = new ModifyRecord(seqs[t]);
								tmp.add(r);
								tmp.add(mr);
								t++;
							}
							s++;
							continue;
						} else {
							if (result != null && mr.getState() == ModifyRecord.STATE_UPDATE) {
								result.add(data.getMem(t));
							}
							
							mr.setDelete();
							s++;
							t++;
						}
					} else {
						mr = new ModifyRecord(seq2);
						if (result != null) {
							result.add(data.getMem(t));
						}
	
						t++;
					}
					
					tmp.add(mr);
				} else {
					seq2 = -seq2;
					if (seq1 < seq2) {
						s++;
						tmp.add(mr);
					} else if (seq1 == seq2) {
						if (mr.getState() == ModifyRecord.STATE_INSERT) {
							int cmp = mr.getRecord().compare((BaseRecord)data.getMem(t), keyIndex);
							if (cmp < 0) {
								s++;
								tmp.add(mr);
							} else if (cmp == 0) {
								if (result != null) {
									result.add(data.getMem(t));
								}
	
								s++;
								t++;
							} else {
								t++;
							}
						} else {
							s++;
							t++;
							tmp.add(mr);
						}
					} else {
						t++;
					}
				}
			}
			
			for (; s < srcLen; ++s) {
				tmp.add(modifyRecords.get(s));
			}
			
			for (; t <= len; ++t) {
				if (seqs[t] > 0) {
					if (result != null) {
						result.add(data.getMem(t));
					}

					ModifyRecord r = new ModifyRecord(seqs[t]);
					tmp.add(r);
				}
			}
			
			this.modifyRecords = tmp;
		}
		
		if (modified) {
			if (isPrimaryTable) {
				//主表有delete，就必须同步delete子表
				ArrayList<PhyTable> tableList = getTableList();
				int size = tableList.size();
				for (int i = 0; i < size; ++i) {
					ColPhyTable t = ((ColPhyTable)tableList.get(i));
					t.delete(data, "s");//删除子表列区
					t.delete(data);//删除子表补区
				}
				
				//主表有删除，补区的位置会变化，还要同步子表补区
				for (int i = 0; i < size; ++i) {
					ColPhyTable t = ((ColPhyTable)tableList.get(i));
					t.update(this.modifyRecords);
					t.saveModifyRecords();
				}
			}
			
			if (!deleteByBaseKey && isSave) {
				saveModifyRecords();
			}
		}
		
		if (!deleteByBaseKey  && isSave) {
			groupTable.save();
		}
		
		if (nopt) {
			result.addAll(result1);
		}
		
		return result;
	}
	
	//根据主表的补区，同步更新自己的补区
	private boolean update(ArrayList<ModifyRecord> baseModifyRecords) throws IOException {
		getGroupTable().checkWritable();
		
		if (baseModifyRecords == null) return false;
		ArrayList<ModifyRecord> modifyRecords = getModifyRecords();
		if (modifyRecords == null) {
			return false;
		}
		//int fieldsLen = columns.length;
		int len = sortedColStartIndex;
		int []index = new int[len];
		int []findex = getSortedColIndex();
		for (int i = 0; i < len; ++i) {
			index[i] = findex[i];
		}
		
		boolean find = false;
		int parentRecordSeq = 0;
		for (ModifyRecord mr : baseModifyRecords) {
			parentRecordSeq++;
			BaseRecord mrec = mr.getRecord();
			
			if (mr.getState() != ModifyRecord.STATE_DELETE) {
				for (ModifyRecord r : modifyRecords) {
					if (r.getState() == ModifyRecord.STATE_DELETE) {
						continue;
					}
					
					BaseRecord rec = r.getRecord();
					int cmp = rec.compare(mrec, index);
					if (cmp == 0) {
						if (mr.getState() == ModifyRecord.STATE_INSERT) {
							r.setParentRecordSeq(-parentRecordSeq);
						} else {
							r.setParentRecordSeq(mr.getRecordSeq());
						}
						find = true;
					}
				}
			}
		}
		return find;
	}
	
	//根据data删除子表的补区
	private boolean delete(Sequence data) throws IOException {
		ArrayList<ModifyRecord> tmp = new ArrayList<ModifyRecord>();
		ArrayList<ModifyRecord> srcModifyRecords = new ArrayList<ModifyRecord>();
		ArrayList<ModifyRecord> modifyRecords = getModifyRecords();
		if (modifyRecords == null) {
			return false;
		}
		tmp.addAll(modifyRecords);
		
		int len = sortedColStartIndex;
		int []index = new int[len];
		int []findex = getSortedColIndex();
		for (int i = 0; i < len; ++i) {
			index[i] = findex[i];
		}
		
		len = data.length();
		boolean delete = false;
		for (int i = 1; i <= len; i++) {
			BaseRecord mrec = (BaseRecord) data.get(i);
			srcModifyRecords.clear();
			srcModifyRecords.addAll(tmp);
			tmp.clear();
			for (ModifyRecord r : srcModifyRecords) {
				int state = r.getState();
				if (state == ModifyRecord.STATE_UPDATE) {
					BaseRecord rec = r.getRecord();
					int cmp = rec.compare(mrec, index);
					if (cmp == 0) {
						r.setDelete();
						r.setRecord(null);
						delete = true;
					}
					tmp.add(r);
				} else if (state == ModifyRecord.STATE_INSERT) {
					BaseRecord rec = r.getRecord();
					int cmp = rec.compare(mrec, index);
					if (cmp == 0) {
						delete = true;
					} else {
						tmp.add(r);
					}
				} else {
					tmp.add(r);
				}
			}
		}
		
		if (delete) {
			this.modifyRecords = tmp;
		}
		return delete;
	}
	
	private Object[][] toKeyValuesArray(Sequence values) {
		int valueLen = values.length();
		if (valueLen == 0) {
			return null;
		}
		
		ColumnMetaData[] cols = getSortedColumns();		
		Object [][]dimValues = new Object[valueLen][];
		Object obj = values.getMem(1);
		int dimCount;
		if (obj instanceof Sequence) {
			Sequence seq = (Sequence)obj;
			dimCount = seq.length();
			if (dimCount > cols.length) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("find" + mm.getMessage("function.invalidParam"));
			}
			
			dimValues = new Object[valueLen][];
			for (int i = 1; i <= valueLen; ++i) {
				seq = (Sequence)values.getMem(i);
				dimValues[i - 1] = seq.toArray();
			}
		} else if (obj instanceof BaseRecord) {
			BaseRecord r = (BaseRecord)obj;
			int []keyIndex = r.dataStruct().getPKIndex();
			if (keyIndex == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("ds.lessKey"));
			}

			dimCount = keyIndex.length;
			if (dimCount > cols.length) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("find" + mm.getMessage("function.invalidParam"));
			}
			
			dimValues = new Object[valueLen][];
			for (int i = 1; i <= valueLen; ++i) {
				r = (BaseRecord)values.getMem(i);
				Object []cur = new Object[dimCount];
				for (int f = 0; f < dimCount; ++f) {
					cur[f] = r.getNormalFieldValue(keyIndex[f]);
				}
				
				dimValues[i - 1] = cur;
			}
		} else {
			dimCount = 1;
			dimValues = new Object[valueLen][];
			for (int i = 1; i <= valueLen; ++i) {
				dimValues[i - 1] = new Object[]{values.getMem(i)};
			}
		}
		
		return dimValues;
	}
	
	/**
	 * 检查是否是时间键find
	 * @param values
	 * @return
	 */
	private boolean checkFindsByTimekey(Sequence values) {
		if (getGroupTable().hasTimeKey()) {
			String []keys = getAllSortedColNames();
			int keyCount = keys.length;
			if (keyCount <= 1) {
				return false;
			}
			
			Object obj = values.getMem(1);
			int valueLen = values.length();
			if (valueLen == 0) {
				return false;
			}
			if (obj instanceof Sequence) {
				Sequence seq = (Sequence)obj;
				int dimCount = seq.length();
				if (dimCount == keyCount ) {
					return true;
				}
			}
		}
		return false;
	}
	/**
	 * 根据主键查找记录
	 * @param values
	 */
	public Table finds(Sequence values) throws IOException {
		return finds(values, null);
	}
	
	/**
	 * 根据主键查找记录，selFields为选出列
	 * @param values
	 * @param selFields
	 */
	public Table finds(Sequence values, String []selFields) throws IOException {
		getGroupTable().checkReadable();
		
		if (!hasPrimaryKey()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("dw.lessKey"));
		}
		
		if (checkFindsByTimekey(values)) {
			String []keys = getAllSortedColNames();
			Expression exp = new Expression("null.contain(" + keys[0] + ")");
			Sequence keyValues = new Sequence();
			int valueLen = values.length();
			keyValues = new Sequence();
			for (int i = 1; i <= valueLen; ++i) {
				Sequence seq = (Sequence)values.getMem(i);
				keyValues.add(seq.getMem(1));
			}
			
			Context ctx = new Context();
			exp.getHome().setLeft(new Constant(keyValues));
			Sequence temp = cursor(selFields, exp, ctx).fetch();
			if (temp == null) return null;
			
			Sequence result = new Sequence(valueLen);
			for (int i = 1; i <= valueLen; ++i) {
				Sequence seq = (Sequence)values.getMem(i);
				result.add(temp.findByKey(seq, false));
			}
			Table table = new Table(result.dataStruct());
			table.addAll(result);
			return table;
		}
		
		if (parent != null || getModifyRecords() != null) {
			String []keys = getAllSortedColNames();
			int keyCount = keys.length;
			Expression exp;
			Sequence keyValues = values;
			
			if (keyCount == 1) {
				exp = new Expression("null.contain(" + keys[0] + ")"); 
				Object obj = values.getMem(1);
				int valueLen = values.length();
				if (valueLen == 0) {
					return null;
				}
				if (obj instanceof Sequence) {
					Sequence seq = (Sequence)obj;
					int dimCount = seq.length();
					if (dimCount > keyCount) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("find" + mm.getMessage("function.invalidParam"));
					}
					
					keyValues = new Sequence();
					for (int i = 1; i <= valueLen; ++i) {
						seq = (Sequence)values.getMem(i);
						keyValues.add(seq.getMem(1));
					}
				}
			} else {
				String str = "null.contain([";
				for (int i = 0; i < keyCount; i++) {
					str += keys[i];
					if (i != keyCount - 1) {
						str += ",";
					}
				}
				str += "])";
				exp = new Expression(str); 
			}

			Context ctx = new Context();
			exp.getHome().setLeft(new Constant(keyValues));
			Sequence result = cursor(selFields, exp, ctx).fetch();
			if (result == null) return null;
			Table table = new Table(result.dataStruct());
			table.addAll(result);
			return table;
		}
		
		if (selFields == null) {
			selFields = getColNames();
		}
		
		Object [][]dimValues = toKeyValuesArray(values);
		if (dimValues == null) {
			return null;
		}
		
		int valueLen = dimValues.length;
		int dimCount = dimValues[0].length;
		
		// 把维字段和选出列合并
		ColumnMetaData[] cols = getSortedColumns();
		ArrayList<ColumnMetaData> list = new ArrayList<ColumnMetaData>();
		for (int i = 0; i < dimCount; ++i) {
			list.add(cols[i]);
		}
		
		for (String field : selFields) {
			ColumnMetaData col = getColumn(field);
			if (col == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(field + mm.getMessage("ds.fieldNotExist"));
			}
			
			if (!list.contains(col)) {
				list.add(col);
			}
		}
		
		int colCount = list.size();
		int []findex = new int [colCount]; // 每个字段在结果集数据结构中的字段号
		ColumnMetaData []columns = new ColumnMetaData[colCount];
		list.toArray(columns);
		DataStruct ds = new DataStruct(selFields);
		
		BlockLinkReader rowCountReader = getSegmentReader();
		BlockLinkReader []colReaders = new BlockLinkReader[colCount];
		ObjectReader []segmentReaders = new ObjectReader[colCount];
		
		for (int i = 0; i < colCount; ++i) {
			ColumnMetaData col = list.get(i);
			colReaders[i] = col.getColReader(false);
			segmentReaders[i] = col.getSegmentReader();
			findex[i] = ds.getFieldIndex(col.getColName());
		}
				
		int valueIndex = 0;
		Table table = new Table(ds, valueLen);
		int blockCount = getDataBlockCount();
		long []prevPos = new long[colCount];
		long []curPos = new long[colCount];

		Object []curStartValues = new Object[dimCount];
		int prevCount = rowCountReader.readInt32();
		Object []tmpDimValues = new Object[dimCount];
		
		for (int f = 0; f < dimCount; ++f) {
			prevPos[f] = segmentReaders[f].readLong40();
			segmentReaders[f].skipObject();
			segmentReaders[f].skipObject();
			segmentReaders[f].skipObject();
		}
		
		for (int f = dimCount; f < colCount; ++f) {
			prevPos[f] = segmentReaders[f].readLong40();
			if (columns[f].hasMaxMinValues()) {
				segmentReaders[f].skipObject();
				segmentReaders[f].skipObject();
				segmentReaders[f].skipObject();
			}
		}
		
		IntArrayList indexList = new IntArrayList();		
		for (int b = 1; b < blockCount; ++b) {
			int curCount = rowCountReader.readInt32();
			for (int f = 0; f < dimCount; ++f) {
				curPos[f] = segmentReaders[f].readLong40();
				segmentReaders[f].skipObject();
				segmentReaders[f].skipObject();
				curStartValues[f] = segmentReaders[f].readObject();
			}
			
			for (int f = dimCount; f < colCount; ++f) {
				curPos[f] = segmentReaders[f].readLong40();
				if (columns[f].hasMaxMinValues()) {
					segmentReaders[f].skipObject();
					segmentReaders[f].skipObject();
					segmentReaders[f].skipObject();
				}
			}
			
			Object []curDimValues = dimValues[valueIndex];
			int cmp = Variant.compareArrays(curStartValues, curDimValues);
			if (cmp > 0) {
				BufferReader []readers = new BufferReader[colCount];
				for (int f = 0; f < dimCount; ++f) {
					readers[f] = colReaders[f].readBlockData(prevPos[f], prevCount);
				}
				
				for (int i = 0; i < prevCount; ++i) {
					for (int f = 0; f < dimCount; ++f) {
						tmpDimValues[f] = readers[f].readObject();
					}
					
					cmp = Variant.compareArrays(tmpDimValues, curDimValues);
					if (cmp == 0) {
						BaseRecord r = table.newLast();
						for (int f = 0; f < dimCount; ++f) {
							if (findex[f] != -1) {
								r.setNormalFieldValue(findex[f], curDimValues[f]);
							}
						}
						
						indexList.addInt(i);
						valueIndex++;
						if (valueIndex == valueLen) {
							break;
						} else {
							curDimValues = dimValues[valueIndex];
							if (Variant.compareArrays(curStartValues, curDimValues) <= 0) {
								break;
							}
						}
					} else if (cmp > 0) {
						for (++valueIndex; valueIndex < valueLen; ++valueIndex) {
							curDimValues = dimValues[valueIndex];
							cmp = Variant.compareArrays(tmpDimValues, curDimValues);
							if (cmp == 0) {
								BaseRecord r = table.newLast();
								for (int f = 0; f < dimCount; ++f) {
									if (findex[f] != -1) {
										r.setNormalFieldValue(findex[f], curDimValues[f]);
									}
								}
								
								indexList.addInt(i);
								break;
							} else if (cmp < 0) {
								break;
							}
						}
						
						if (valueIndex == valueLen) {
							break;
						}
					}
				}
				
				int count = indexList.size();
				if (count > 0) {
					for (int f = dimCount; f < colCount; ++f) {
						readers[f] = colReaders[f].readBlockData(prevPos[f], prevCount);
					}
					
					int prev = 0;
					for (int i = 0, m = table.length() - count + 1; i < count; ++i, ++m) {
						BaseRecord r = (BaseRecord)table.getMem(m);
						int index = indexList.getInt(i);
						for (int f = dimCount; f < colCount; ++f) {
							for (int j = prev; j < index; ++j) {
								readers[f].skipObject();
							}
							
							r.setNormalFieldValue(findex[f], readers[f].readObject());
						}
						
						prev = index + 1;
					}
					
					indexList.clear();
				}
				
				if (valueIndex == valueLen) {
					break;
				}
			}
			
			prevCount = curCount;
			long []tmpPos = prevPos;
			prevPos = curPos;
			curPos = tmpPos;
		}
		
		if (valueIndex < valueLen) {
			BufferReader []readers = new BufferReader[colCount];
			for (int f = 0; f < dimCount; ++f) {
				readers[f] = colReaders[f].readBlockData(prevPos[f], prevCount);
			}
			
			Object []curDimValues = dimValues[valueIndex];
			for (int i = 0; i < prevCount; ++i) {
				for (int f = 0; f < dimCount; ++f) {
					tmpDimValues[f] = readers[f].readObject();
				}
				
				int cmp = Variant.compareArrays(tmpDimValues, curDimValues);
				if (cmp == 0) {
					BaseRecord r = table.newLast();
					for (int f = 0; f < dimCount; ++f) {
						if (findex[f] != -1) {
							r.setNormalFieldValue(findex[f], curDimValues[f]);
						}
					}
					
					indexList.addInt(i);
					valueIndex++;
					if (valueIndex == valueLen) {
						break;
					} else {
						curDimValues = dimValues[valueIndex];
					}
				} else if (cmp > 0) {
					for (++valueIndex; valueIndex < valueLen; ++valueIndex) {
						curDimValues = dimValues[valueIndex];
						cmp = Variant.compareArrays(tmpDimValues, curDimValues);
						if (cmp == 0) {
							BaseRecord r = table.newLast();
							for (int f = 0; f < dimCount; ++f) {
								if (findex[f] != -1) {
									r.setNormalFieldValue(findex[f], curDimValues[f]);
								}
							}
							
							indexList.addInt(i);
							break;
						} else if (cmp < 0) {
							break;
						}
					}
					
					if (valueIndex == valueLen) {
						break;
					}
				}
			}
			
			int count = indexList.size();
			if (count > 0) {
				for (int f = dimCount; f < colCount; ++f) {
					readers[f] = colReaders[f].readBlockData(prevPos[f], prevCount);
				}
				
				int prev = 0;
				for (int i = 0, m = table.length() - count + 1; i < count; ++i, ++m) {
					BaseRecord r = (BaseRecord)table.getMem(m);
					int index = indexList.getInt(i);
					for (int f = dimCount; f < colCount; ++f) {
						for (int j = prev; j < index; ++j) {
							readers[f].skipObject();
						}
						
						r.setNormalFieldValue(findex[f], readers[f].readObject());
					}
					
					prev = index + 1;
				}
			}
		}
		
		return table;
	}

	/**
	 * 根据n返回分段点值和每段条数
	 * @param keys 分段字段
	 * @param list 返回的每段条数
	 * @param values 返回分段点值
	 * @param n 期望的每段条数
	 * @throws IOException 
	 */
	public void getSegmentInfo(String []keys, ArrayList<Integer> list, Sequence values, int n) throws IOException {
		ColumnMetaData []columns = getColumns(keys);
		int colCount = keys.length;
		BlockLinkReader rowCountReader = getSegmentReader();
		ObjectReader []segmentReaders = new ObjectReader[colCount];
		
		for (int i = 0; i < colCount; ++i) {
			segmentReaders[i] = columns[i].getSegmentReader();
		}
		
		int blockCount = getDataBlockCount();
		int  sum = rowCountReader.readInt32();
		for (int f = 0; f < colCount; ++f) {
			segmentReaders[f].readLong40();
			segmentReaders[f].skipObject();
			segmentReaders[f].skipObject();
			segmentReaders[f].skipObject();
		}
		
		for (int i = 1; i < blockCount; ++i) {
			int cnt = rowCountReader.readInt32();
			if (sum + cnt > n) {
				list.add(sum);
				sum = cnt;
				Object []vals = new Object[colCount];
				for (int f = 0; f < colCount; ++f) {
					segmentReaders[f].readLong40();
					segmentReaders[f].skipObject();
					segmentReaders[f].skipObject();
					vals[f] = segmentReaders[f].readObject();
				}
				values.add(vals);
			} else {
				sum += cnt;
				for (int f = 0; f < colCount; ++f) {
					segmentReaders[f].readLong40();
					segmentReaders[f].skipObject();
					segmentReaders[f].skipObject();
					segmentReaders[f].skipObject();
				}
			}
		}
		list.add(sum);//最后一个分段条数，有可能只有这一个
	}
	
	/**
	 * 根据n返回分段点和段数
	 * @param keys 分段字段
	 * @param list 返回的每段条数
	 * @param values 返回分段点值
	 * @param n 期望的每段条数
	 * @throws IOException 
	 */
	public void getSegmentInfo2(String []keys, ArrayList<Integer> list, Sequence values, int n) throws IOException {
		ColumnMetaData []columns = getColumns(keys);
		int colCount = keys.length;
		BlockLinkReader rowCountReader = getSegmentReader();
		ObjectReader []segmentReaders = new ObjectReader[colCount];
		
		for (int i = 0; i < colCount; ++i) {
			segmentReaders[i] = columns[i].getSegmentReader();
		}
		
		int blockCount = getDataBlockCount();
		list.add(0);
		int  sum = rowCountReader.readInt32();
		for (int f = 0; f < colCount; ++f) {
			segmentReaders[f].readLong40();
			segmentReaders[f].skipObject();
			segmentReaders[f].skipObject();
			segmentReaders[f].skipObject();
		}
		
		for (int i = 1; i < blockCount; ++i) {
			int cnt = rowCountReader.readInt32();
			if (sum + cnt > n) {
				if (i + 1 != blockCount) {
					list.add(i);
					list.add(i);
					sum = cnt;
					Object []vals = new Object[colCount];
					for (int f = 0; f < colCount; ++f) {
						segmentReaders[f].readLong40();
						segmentReaders[f].skipObject();
						segmentReaders[f].skipObject();
						vals[f] = segmentReaders[f].readObject();
					}
					values.add(vals);
				}
				

			} else {
				sum += cnt;
				for (int f = 0; f < colCount; ++f) {
					segmentReaders[f].readLong40();
					segmentReaders[f].skipObject();
					segmentReaders[f].skipObject();
					segmentReaders[f].skipObject();
				}
			}
		}
		list.add(blockCount);//最后段数
	}
	
	/**
	 * 返回列的最大最小值
	 * @param key
	 * @return
	 * @throws IOException
	 */
	public Object[] getMaxMinValue(String key) throws IOException {
		if (this.totalRecordCount == 0) {
			return null;
		}
		ColumnMetaData column = getColumn(key);
		if (column == null) {
			return null;
		}
		if (!column.hasMaxMinValues()) {
			Expression max = new Expression("max(" + key +")");
			Expression min = new Expression("min(" + key +")");
			Expression[] exps = new Expression[] {max, min};
			Sequence seq = cursor(new String[] {key}).groups(null, null, exps, null, null, new Context());
			return ((BaseRecord)seq.get(1)).getFieldValues();
		}
		
		ObjectReader segmentReader = column.getSegmentReader();
		
		int blockCount = getDataBlockCount();
		Object max, min, obj;
		segmentReader.readLong40();
		min = segmentReader.readObject();
		max = segmentReader.readObject();
		segmentReader.skipObject();
		
		for (int i = 1; i < blockCount; ++i) {
			segmentReader.readLong40();
			obj = segmentReader.readObject();
			if (Variant.compare(obj, min) < 0) {
				min = obj;
			}
			obj = segmentReader.readObject();
			if (Variant.compare(obj, max) > 0) {
				max = obj;
			}
			segmentReader.skipObject();
		}
		return new Object[] {max, min};
	}
	
	/**
	 * 返回每个分段的记录数
	 */
	public long[] getSegmentInfo() {
		BlockLinkReader rowCountReader = getSegmentReader();
		int blockCount = getDataBlockCount();
		long recCountOfSegment[] = new long[blockCount];
		long sum = 0;
		try {
			for (int i = 0; i < blockCount; ++i) {
				sum += rowCountReader.readInt32();
				recCountOfSegment[i] = sum;
			}
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				rowCountReader.close();
			} catch (Exception e){};
		}
		return recCountOfSegment;
	}
	
	/**
	 * 合并两个组表文件
	 * @param table 另一个组表
	 * @throws IOException
	 */
	public void append(ColPhyTable table) throws IOException {
		getGroupTable().checkWritable();
		table.getGroupTable().checkReadable();
		
		if (!table.getDataStruct().isCompatible(getDataStruct())) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.dsNotMatch"));
		}
		
		if (getModifyRecords() != null || table.getModifyRecords() != null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("grouptable.invalidData"));
		}
		
		// 取出追加的表的首条记录的维字段值用来判断合并后的表是否有序和有主键
		Object []startValues = null;
		if (isSorted) {
			ColumnMetaData []columns = table.getSortedColumns();
			int count = columns.length;
			startValues = new Object[count];
			for (int i = 0; i < count; ++i) {
				ObjectReader segmentReader = columns[i].getSegmentReader();
				segmentReader.readLong40();
				segmentReader.skipObject();
				segmentReader.skipObject();
				startValues[i] = segmentReader.readObject();
			}
		}
		
		if (hasPrimaryKey) {
			if (!table.hasPrimaryKey || Variant.compareArrays(maxValues, startValues) >= 0) {
				hasPrimaryKey = false;
			}
		}
		
		if (!hasPrimaryKey && isSorted) {
			if (!table.isSorted || Variant.compareArrays(maxValues, startValues) > 0) {
				isSorted = false;
			}
		}
		
		// 准备写数据
		prepareAppend();
		
		ColumnMetaData []columns = this.columns;
		ColumnMetaData []columns2 = table.columns;
		int colCount = columns.length;
		
		BlockLinkReader rowCountReader = table.getSegmentReader();
		BlockLinkReader []colReaders = new BlockLinkReader[colCount];
		ObjectReader []segmentReaders = new ObjectReader[colCount];
		for (int i = 0; i < colCount; ++i) {
			colReaders[i] = columns2[i].getColReader(true);
			segmentReaders[i] = columns2[i].getSegmentReader();
		}
		
		int blockCount = table.getDataBlockCount();
		for (int i = 0; i < blockCount; ++i) {
			for (int j = 0; j < colCount; j++) {
				columns[j].copyColBlock(colReaders[j], segmentReaders[j]);
			}
			
			//更新分段信息buffer
			appendSegmentBlock(rowCountReader.readInt32());
		}
		
		// 结束写数据，保存到文件
		finishAppend();
		
		rowCountReader.close();
		for (int i = 0; i < colCount; ++i) {
			colReaders[i].close();
			segmentReaders[i].close();
		}
	}
	
	/**
	 * 获得补区最开始的记录所在的块号
	 * @return 块号
	 */
	public int getFirstBlockFromModifyRecord() {
		long minSeq = Long.MAX_VALUE;
		ArrayList<ModifyRecord> recs = getModifyRecords();
		if (recs != null) {
			for (ModifyRecord rec : recs) {
				long seq = rec.getRecordSeq();
				if (minSeq > seq) {
					minSeq = seq;
				}
			}
		}
		if (minSeq == Long.MAX_VALUE) {
			return -1;
		}
		long []recCountOfSegment = getSegmentInfo();
		int len = recCountOfSegment.length;
		for (int i = 0; i < len; i++) {
			if (minSeq <= recCountOfSegment[i]) {
				return i;
			}
		}
		return -1;
	}
	
	/**
	 * 重置各个区块链
	 * @param block 截止块号
	 * @return 最靠近文件末尾的截断位置
	 */
	public long resetByBlock(int block) {
		BlockLinkReader rowCountReader = getSegmentReader();
		long sum = 0;
		int blockSize = getGroupTable().blockSize;
		try {
			//把分段信息读出来
			for (int i = 0; i < block; ++i) {
				int cnt = rowCountReader.readInt32();
				sum += cnt;
			}

			//修改分段信息
			segmentBlockLink.lastBlockPos = rowCountReader.position();
			segmentBlockLink.freeIndex = rowCountReader.getCaret();
			
			//重置属性
			totalRecordCount = sum;
			dataBlockCount = block;
			//清空补区
			modifyRecords.clear();
			saveModifyRecords();
			
			maxValues = null;
			//重置每个列块
			long resetPos = 0;//保存最靠近文件末尾的截断位置
			for (ColumnMetaData col : columns) {
				ObjectReader reader;
				BlockLinkReader segmentReader = new BlockLinkReader(col.getSegmentBlockLink());
				try {
					segmentReader.loadFirstBlock();
					reader = new ObjectReader(segmentReader, blockSize - ComTable.POS_SIZE);
				} catch (IOException e) {
					segmentReader.close();
					throw new RQException(e.getMessage(), e);
				}
				
				//读取列块的分段信息
				for (int i = 0; i < block; ++i) {
					reader.readLong40();
					if (col.hasMaxMinValues()) {
						reader.readObject();
						reader.readObject();
						reader.readObject();
					}
				}
				
				//重写列块的分段信息
				BlockLink blockLink = col.getSegmentBlockLink();
				blockLink.freeIndex = (int) (reader.position() % blockSize);
				blockLink.lastBlockPos = segmentReader.position();
				
				long tempPos = reader.readLong40();//保存截止后开始的地址
				if (resetPos < tempPos) {
					resetPos = tempPos;
				}
				reader.close();
				segmentReader.close();

				blockLink = col.getDataBlockLink();
				blockLink.freeIndex = (int) (tempPos % blockSize);
				blockLink.lastBlockPos = tempPos - (tempPos % blockSize);
			}

			if (parent != null) {
				BlockLinkReader segmentReader = new BlockLinkReader(guideColumn.getSegmentBlockLink());
				ObjectReader reader;
				try {
					segmentReader.loadFirstBlock();
					reader = new ObjectReader(segmentReader, blockSize - ComTable.POS_SIZE);
				} catch (IOException e) {
					segmentReader.close();
					throw new RQException(e.getMessage(), e);
				}
				//读取导列分段信息
				for (int i = 0; i < block; ++i) {
					reader.readLong40();
				}
				//重写列块的分段信息
				BlockLink blockLink = guideColumn.getSegmentBlockLink();
				blockLink.freeIndex = (int) (reader.position() % blockSize);
				blockLink.lastBlockPos = segmentReader.position();
				
				long tempPos = reader.readLong40();//保存截止后开始的地址
				if (resetPos < tempPos) {
					resetPos = tempPos;
				}
				reader.close();
				segmentReader.close();

				blockLink = guideColumn.getDataBlockLink();
				blockLink.freeIndex = (int) (tempPos % blockSize);
				blockLink.lastBlockPos = tempPos - (tempPos % blockSize);
			}
			
			return (resetPos - (resetPos % blockSize) + blockSize);
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				rowCountReader.close();
			} catch (Exception e){};
		}
	}
	
	/**
	 * 检查field是否是维字段
	 * 如果是，则根据表达式计算范围
	 * 否则返回NULL
	 * @param field
	 * @param node 表达式的home节点
	 * @param ctx
	 */
	long[] checkDim(String field, Node node, Context ctx) {
		ColumnMetaData col = getColumn(field);
		if (col == null || !col.isDim()) {
			return null;
		}
		
		int operator = 0;
		if (node instanceof Equals) {
			operator = IFilter.EQUAL;
		} else if (node instanceof Greater) {
			operator = IFilter.GREATER;
		} else if (node instanceof NotSmaller) {
			operator = IFilter.GREATER_EQUAL;
		} else if (node instanceof Smaller) {
			operator = IFilter.LESS;
		} else if (node instanceof NotGreater) {
			operator = IFilter.LESS_EQUAL;
		} else if (node instanceof NotEquals) {
			operator = IFilter.NOT_EQUAL;
		} else {
			return null;
		}
		
		Object value;
		if (node.getRight() instanceof UnknownSymbol) {
			value = node.getLeft().calculate(ctx);
		} else {
			value = node.getRight().calculate(ctx);
		}

		long seq = 0;
		int blockCount = dataBlockCount;
		ColumnFilter filter = new ColumnFilter(col, 0, operator, value);
		LongArray intervals = new LongArray(blockCount);
		BlockLinkReader rowCountReader = getSegmentReader();
		ObjectReader segmentReader = col.getSegmentReader();
		try {
			Object maxValue, minValue;
			for (int i = 0; i < blockCount; ++i) {
				int recordCount = rowCountReader.readInt32();
				segmentReader.readLong40();
				minValue = segmentReader.readObject();
				maxValue = segmentReader.readObject();
				segmentReader.skipObject();
				if (filter.match(minValue, maxValue) && recordCount != 1) {
					intervals.add(seq + 1);
					intervals.add(seq + recordCount);
				}
				seq += recordCount;
			} 
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		}
		if (intervals.size() == 0) {
			return null;
		}
		return intervals.getDatas();
	}
	
	/**
	 * 写入缓存的数据
	 */
	public void appendCache() throws IOException {
		if (appendCache == null) return;
		
		ICursor cursor = new MemoryCursor(appendCache);
		// 准备写数据
		prepareAppend();
		
		if (parent != null) {
			parent.appendCache();
			appendAttached(cursor);
		} else if (sortedColumns == null) {
			appendNormal(cursor);
		} else if (getSegmentCol() == null) {
			appendSorted(cursor);
		} else {
			appendSegment(cursor);
		}

		appendCache = null;
		// 结束写数据，保存到文件
		finishAppend();
	}
	
	/**
	 * 返回这个基表的多路游标 (用于集群的节点机)
	 * @param exps 取出字段表达式（当exps为null时按照fields取出）
	 * @param fields 取出字段的新名称
	 * @param filter 过滤表达式
	 * @param fkNames 指定FK过滤的字段名称
	 * @param codes 指定FK过滤的数据序列
	 * @param pathSeq 第几段
	 * @param pathCount 节点机数
	 * @param pathCount2 节点机上指定的块数
	 * @param opt 选项
	 * @param ctx 上下文
	 * @return
	 */
	public ICursor cursor(Expression []exps, String[] fields, Expression filter, String[] fkNames, 
			Sequence[] codes, String[] opts, int pathSeq, int pathCount, int pathCount2, String opt, Context ctx) {
		if (dataBlockCount < pathCount || pathCount2 < 2) {
			//如果块数少于节点机数或者节点机上指定的块数少于2
			return cursor(exps, fields, filter, fkNames, codes, opts, pathSeq, pathCount, opt, ctx);
		}
				
		ICursor []cursors = new ICursor[pathCount2];
		
		//得到划分给当前节点机的块数
		int avg = dataBlockCount / pathCount;
		int count = avg;
		if (pathSeq == pathCount) {
			count += dataBlockCount % pathCount;
		}
		
		int offset = (pathSeq - 1) * avg;
		if (count < pathCount2) {
			//如果要处理的块数小于并行数
			int i = 0;
			for (; i < count; i++) {
				if (filter != null) {
					// 分段并行读取时需要复制表达式，同一个表达式不支持并行运算
					filter = filter.newExpression(ctx);
				}

				cursors[i] = cursor(exps, fields, filter, fkNames, codes, opts, opt, ctx);
				((IDWCursor) cursors[i]).setSegment(offset + i, offset + i + 1);
			}
			
			for (; i < pathCount2; i++) {
				if (filter != null) {
					// 分段并行读取时需要复制表达式，同一个表达式不支持并行运算
					filter = filter.newExpression(ctx);
				}

				cursors[i] = cursor(exps, fields, filter, fkNames, codes, opts, opt, ctx);
				((IDWCursor) cursors[i]).setSegment(0, -1);
			}
		} else {
			//得到划分给当前节点机的每一路的块数
			int avg2 = count / pathCount2;
			for (int i = 0; i < pathCount2; i++) {
				if (filter != null) {
					// 分段并行读取时需要复制表达式，同一个表达式不支持并行运算
					filter = filter.newExpression(ctx);
				}
				
				cursors[i] = cursor(exps, fields, filter, fkNames, codes, opts, opt, ctx);
				if (i != pathCount2 - 1) {
					((IDWCursor) cursors[i]).setSegment(offset + avg2 * i, offset + avg2 * (i + 1));
				} else {
					((IDWCursor) cursors[i]).setSegment(offset + avg2 * i, offset + count);
				}
			}
		}

		return new MultipathCursors(cursors, ctx);
	}

	/**
	 * 根据给定的游标，返回同步分段的多路游标
	 * @param exps 取出字段表达式（当exps为null时按照fields取出）
	 * @param fields 取出字段的新名称
	 * @param filter 过滤表达式
	 * @param fkNames 指定FK过滤的字段名称
	 * @param codes 指定FK过滤的数据序列
	 * @param opts 关联字段进行关联的选项
	 * @param cursor 参考游标
	 * @param seg 节点机号
	 * @param endValues 尾部要追加的记录
	 * @param opt 选项
	 * @param ctx
	 * @return
	 */
	public ICursor cursor(Expression []exps, String[] fields, Expression filter, String[] fkNames, 
			Sequence[] codes, String[] opts, ICursor cursor, int seg, Object [][]endValues, String opt, Context ctx) {
		getGroupTable().checkReadable();
		
		ArrayList<ICursor> csList = new ArrayList<ICursor>();
		ICursor []srcs;
		if (cursor instanceof MultipathCursors) {
			srcs = ((MultipathCursors) cursor).getParallelCursors();
		} else {
			srcs = new ICursor[]{cursor};
		}
		
		for (ICursor cs : srcs) {
			if (!(cs instanceof Cursor) && !(cs instanceof JoinTableCursor)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("cursor" + mm.getMessage("dw.needMCursor"));
			}
		}
		
		PhyTable table = ((IDWCursor) srcs[0]).getTableMetaData();
		String []names = table.getAllSortedColNames();
		if (names == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("cursor" + mm.getMessage("dw.needMCursor"));
		}
		String segCol = table.getSegmentCol();
		if (segCol != null) {
			for (int i = names.length - 1; i >= 0; --i) {
				if (names[i].equals(segCol)) {
					String []tmp = new String[i + 1];
					System.arraycopy(names, 0, tmp, 0, i + 1);
					names = tmp;
					break;
				}
			}
		}
		
		int fcount = names.length;
		if (seg != 0) {
			//如果不是第一个节点机
			BaseRecord rec = new Record(cursor.getDataStruct());
			Sequence tempSeq = new Sequence();
			tempSeq.add(rec);
			csList.add(new MemoryCursor(tempSeq));
		}
		
		int firstNullIndex = -1;
		for (int i = 0, len = srcs.length; i < len; i++) {
			if (srcs[i].peek(1) == null && firstNullIndex < 0) {
				firstNullIndex = i;
			}
			csList.add(srcs[i]);
		}
		
		if (seg + 1 <= endValues.length) {
			//如果不是最后一个节点机
			Object []objs = endValues[seg];
			BaseRecord rec = new Record(cursor.getDataStruct());
			for (int f = 0; f < fcount; f++) {
				rec.set(names[f], objs[f]);
			}
			Sequence tempSeq = new Sequence();
			tempSeq.add(rec);
			if (firstNullIndex < 0) {
				csList.add(new MemoryCursor(tempSeq));
			} else {
				csList.add(firstNullIndex, new MemoryCursor(tempSeq));
			}
		}
		
		srcs = new ICursor[csList.size()];
		csList.toArray(srcs);
		
		int segCount = srcs.length;

		ColumnMetaData[] sortedCols = getAllSortedColumns();
		if (sortedCols.length < fcount) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("dw.segFieldNotMatch"));
		}
		
		String []dimFields = new String[fcount];
		for (int f = 0; f < fcount; ++f) {
			dimFields[f] = sortedCols[f].getColName();
		}
		
		Object [][]minValues = new Object [segCount][];
		int dataSegCount = segCount; // 有数据的多路游标路数
		
		for (int i = 0; i < segCount; ++i) {
			Sequence seq = srcs[i].peek(1);
			if (seq == null) {
				dataSegCount = i;
				for (++i; i < segCount; ++i) {
					if (srcs[i].peek(1) != null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("cursor" + mm.getMessage("dw.needMCursor"));
					}
				}
				
				break;
			}

			BaseRecord r = (BaseRecord)seq.get(1);
			Object []vals = new Object[fcount];
			minValues[i] = vals;
			for (int f = 0; f < fcount; ++f) {
				vals[f] = r.getFieldValue(names[f]);
			}
		}
		
		ObjectReader []readers = new ObjectReader[fcount];
		for (int f = 0; f < fcount; ++f) {
			readers[f] = sortedCols[f].getSegmentReader();
		}
		
		int blockCount = getDataBlockCount();
		int s = 1;
		Object []blockMinVals = new Object[fcount];
		ICursor []cursors = new ICursor[segCount];
		boolean []isEquals = new boolean[segCount];
		
		//用空游标补全
		for (int i = dataSegCount; i < segCount; ++i) {
			Cursor cs = new Cursor(this, fields, filter, fkNames, codes, opts, ctx);
			cs.setSegment(0, -1);
			cursors[i] = cs;
		}
		try {
			// 不是同步分段的，需要掐头去尾
			int startBlock = 0;
			for (int b = 0; b < blockCount && s < dataSegCount; ++b) {
				for (int f = 0; f < fcount; ++f) {
					readers[f].readLong40();
					readers[f].skipObject();
					readers[f].skipObject();
					blockMinVals[f] = readers[f].readObject(); //startValue
				}
				
				if (filter != null) {
					// 分段并行读取时需要复制表达式，同一个表达式不支持并行运算
					filter = filter.newExpression(ctx);
				}
				
				int cmp = Variant.compareArrays(blockMinVals, minValues[s]);
				if (cmp > 0) {
					ICursor cs = cursor(exps, fields, filter, fkNames, codes, opts, opt, ctx);
					cursors[s - 1] = cs;
					if (b > 0) {
						((IDWCursor) cs).setSegment(startBlock, b - 1);
						startBlock = b - 1;
					} else {
						((IDWCursor) cs).setSegment(startBlock, 0);
						startBlock = 0;
					}
					
					isEquals[s] = false;
					s++;
				} else if (cmp == 0) {
					ICursor cs = cursor(exps, fields, filter, fkNames, codes, opts, opt, ctx);
					cursors[s - 1] = cs;
					((IDWCursor) cs).setSegment(startBlock, b);
					startBlock = b;
					
					isEquals[s] = true;
					s++;
				}
			}
			
			if (s == dataSegCount) {
				if (filter != null) {
					// 分段并行读取时需要复制表达式，同一个表达式不支持并行运算
					filter = filter.newExpression(ctx);
				}
				
				ICursor cs = cursor(exps, fields, filter, fkNames, codes, opts, opt, ctx);
				cursors[s - 1] = cs;
				((IDWCursor) cs).setSegment(startBlock, blockCount);
				
				for (int i = 1; i < s; ++i) {
					if (!isEquals[i]) {
						Sequence seq = fetchToValue((IDWCursor)cursors[i], dimFields, minValues[i]);
						((IDWCursor) cursors[i - 1]).setAppendData(seq);
					}
				}
			} else {
				// 最后一段的起始值小于等于参照的多路游标的中间段的起始值
				if (filter != null) {
					// 分段并行读取时需要复制表达式，同一个表达式不支持并行运算
					filter = filter.newExpression(ctx);
				}
				
				ICursor cs = cursor(exps, fields, filter, fkNames, codes, opts, opt, ctx);
				cursors[s - 1] = cs;
				((IDWCursor) cs).setSegment(startBlock, blockCount - 1);
				
				for (int i = 1; i < s; ++i) {
					if (!isEquals[i]) {
						Sequence seq = fetchToValue((IDWCursor)cursors[i], dimFields, minValues[i]);
						((IDWCursor) cursors[i - 1]).setAppendData(seq);
					}
				}
				
				// 定义最后一段游标，其它空段从最后一段取出相应的值
				if (filter != null) {
					// 分段并行读取时需要复制表达式，同一个表达式不支持并行运算
					filter = filter.newExpression(ctx);
				}
				
				cursors[dataSegCount - 1] = new Cursor(this, exps, fields, filter, fkNames, codes, opts, ctx);
				((IDWCursor) cursors[dataSegCount - 1]).setSegment(blockCount - 1, blockCount);
				
				Sequence seq = fetchToValue((IDWCursor)cursors[dataSegCount - 1], dimFields, minValues[s]);
				((IDWCursor) cursors[s - 1]).setAppendData(seq);
				
				for (; s < dataSegCount - 1; ++s) {
					if (filter != null) {
						// 分段并行读取时需要复制表达式，同一个表达式不支持并行运算
						filter = filter.newExpression(ctx);
					}
					
					cursors[s] = new Cursor(this, exps, fields, filter, fkNames, codes, opts, ctx);
					((IDWCursor) cursors[s]).setSegment(blockCount - 1, blockCount - 1);
					//cursors[s] = new MemoryCursor(null);
					
					seq = fetchToValue((IDWCursor)cursors[dataSegCount - 1], dimFields, minValues[s + 1]);
					((IDWCursor) cursors[s]).setAppendData(seq);
				}
			}
		} catch (IOException e) {
			throw new RQException(e);
		}
		
		csList.clear();
		int len = cursors.length - 1;
		if (seg != 1) {
			for (int i = 1; i < len; i++) {
				csList.add(cursors[i]);
			}
		} else {
			for (int i = 0; i < len; i++) {
				csList.add(cursors[i]);
			}
		}
		if (seg + 1 > endValues.length) {
			csList.add(cursors[len]);
		}
		
		cursors = new ICursor[csList.size()];
		csList.toArray(cursors);
		return new MultipathCursors(cursors, ctx);
	}
	
	/**
	 * 添加一列
	 * @param colName 列名
	 * @param exp 列值表达式
	 * @param ctx 
	 */
	public void addColumn(String colName, Expression exp, Context ctx) {
		//如果有补区，则先reset
		if (getModifyRecords() != null) {
			groupTable.reset(null, null, ctx, null);
		}
		
		//检查列是否已经存在
		ColumnMetaData existCol = getColumn(colName);
		if (null != existCol) {
			if (existCol.isDim() || existCol.isKey() || this.getModifyRecords() != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("alter" + mm.getMessage("dw.columnNotEditable"));
			}
		}
		
		PhyTable tmd = getSupplementTable(false);
		if (tmd != null) {
			// 有补文件时先处理补文件
			tmd.addColumn(colName, exp, ctx);
		}
		
		//新建立一个列
		ColumnMetaData col = new ColumnMetaData(this, colName, false, false);
		ICursor cursor = cursor();
		BlockLinkReader rowCountReader = getSegmentReader();
		
		try {
			//初始化列块
			col.applySegmentFirstBlock();
			col.applyDataFirstBlock();
			col.prepareWrite();
			int curBlock = 0, endBlock = getDataBlockCount();
			
			//按照每段的记录数把其它列取出来，并按照exp计算
			while (curBlock < endBlock) {
				curBlock++;
				
				//取数，计算
				int recordCount = rowCountReader.readInt32();
				Sequence data = (Sequence) cursor.fetch(recordCount);
				data = data.newTable(new String[] {""}, new Expression[]{exp}, ctx);
				
				//把计算得到数据，写入新列
				Object []minValues = new Object[1];//一块的最小维值
				Object []maxValues = new Object[1];//一块的最大维值
				Object []startValues = new Object[1];
				int[] dataTypeInfo = new int[1];
				
				BufferWriter bufferWriter = col.getColDataBufferWriter();
				Sequence dict = col.getDict();
				int len = data.length();
				DataBlockWriterJob.writeDataBlock(bufferWriter, data, dict, 0, 1, len, maxValues, minValues, startValues, dataTypeInfo);
				
				//统计列数据类型
				boolean doCheck = groupTable.isCheckDataPure();
				col.adjustDataType(dataTypeInfo[0], doCheck);
				
				//提交列块buffer
				col.appendColBlock(bufferWriter.finish(), minValues[0], maxValues[0], startValues[0]);
			}
			
			col.finishWrite();
			cursor.close();
			
			if (existCol != null) {
				//替换
				int len = columns.length;
				for (int i = 0; i < len; i++) {
					ColumnMetaData column = columns[i];
					if (column.getColName().equals(colName)) {
						columns[i] = col;
						break;
					}
				}
				groupTable.save();
			} else {
				//追加
				ColumnMetaData []newColumns = java.util.Arrays.copyOf(columns, columns.length + 1);
				String []newColNames = java.util.Arrays.copyOf(colNames, colNames.length + 1);
				newColumns[columns.length] = col;
				newColNames[columns.length] = colName;
				columns = newColumns;
				colNames = newColNames;
				groupTable.save();
			}
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		}
	}
	
	public void deleteColumn(String colName) {
		//如果有补区，则先reset
		if (getModifyRecords() != null) {
			groupTable.reset(null, null, new Context(), null);
		}
				
		PhyTable tmd = getSupplementTable(false);
		if (tmd != null) {
			// 有补文件时先删除补文件中的
			tmd.deleteColumn(colName);
		}
		
		ColumnMetaData col = getColumn(colName);
		
		//检查列是否存在
		if (col == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("alter" + mm.getMessage("dw.columnNotExist"));
		}
		
		//检查是否在删除维列或排序列
		if (col.isDim() || col.isKey()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("alter" + mm.getMessage("dw.columnNotEditable"));
		}
		
		ColumnMetaData []newColumns = new ColumnMetaData[columns.length - 1];
		String []newColNames = new String[colNames.length - 1];
		int i = 0;
		for (ColumnMetaData cmd : columns) {
			if (cmd != col) {//这里用的是引用对比
				newColumns[i] = cmd;
				newColNames[i++] = cmd.getColName();
			}
		}
		
		columns = newColumns;
		colNames = newColNames;
		
		try {
			groupTable.save();
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		}
	}
}