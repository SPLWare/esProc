package com.scudata.dw;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.ListBase1;
import com.scudata.dm.LongArray;
import com.scudata.dm.ObjectReader;
import com.scudata.dm.ObjectWriter;
import com.scudata.dm.Param;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.ConjxCursor;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.cursor.MemoryCursor;
import com.scudata.dm.cursor.MergesCursor;
import com.scudata.dm.cursor.MultipathCursors;
import com.scudata.expression.Expression;
import com.scudata.expression.Node;
import com.scudata.expression.UnknownSymbol;
import com.scudata.expression.operator.Equals;
import com.scudata.expression.operator.Greater;
import com.scudata.expression.operator.NotEquals;
import com.scudata.expression.operator.NotGreater;
import com.scudata.expression.operator.NotSmaller;
import com.scudata.expression.operator.Smaller;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 行存基表类
 * @author runqian
 *
 */
public class RowTableMetaData extends TableMetaData {
	protected transient String []sortedColNames; // 排序字段(维字段)
	private transient String []allSortedColNames; // 排序字段(维字段) 含主表
	protected transient String []allKeyColNames; // key字段
	protected BlockLink dataBlockLink; // 行块区块链

	protected transient BlockLinkWriter colWriter;
	protected transient ObjectWriter objectWriter;
	protected int sortedColStartIndex;////主表的维字段个数
	
	protected boolean[] isDim;//是否是维字段
	protected boolean[] isKey;//是否是key字段
	protected int []serialBytesLen;//每个列的排号长度，0表示不是排号类型
	
	/**
	 * 用于序列化
	 * @param groupTable
	 */
	public RowTableMetaData(GroupTable groupTable) {
		this.groupTable = groupTable;
		dataBlockLink = new BlockLink(groupTable);
		segmentBlockLink = new BlockLink(groupTable);
		this.modifyBlockLink1 = new BlockLink(groupTable);
		this.modifyBlockLink2 = new BlockLink(groupTable);
	}
	
	/**
	 * 用于序列化
	 * @param groupTable
	 * @param parent
	 */
	public RowTableMetaData(GroupTable groupTable, RowTableMetaData parent) {
		this.groupTable = groupTable;
		this.parent = parent;
		dataBlockLink = new BlockLink(groupTable);
		segmentBlockLink = new BlockLink(groupTable);
		this.modifyBlockLink1 = new BlockLink(groupTable);
		this.modifyBlockLink2 = new BlockLink(groupTable);
	}
	
	/**
	 * 新建基表
	 * @param groupTable
	 * @param colNames
	 * @throws IOException
	 */
	public RowTableMetaData(GroupTable groupTable, String []colNames) throws IOException {
		// 创建新数组，否则文件组时可能影响到其它分区组表的创建
		String []tmp = new String[colNames.length];
		System.arraycopy(colNames, 0, tmp, 0, colNames.length);
		colNames = tmp;

		this.groupTable = groupTable;
		this.tableName = "";
		dataBlockLink = new BlockLink(groupTable);
		this.segmentBlockLink = new BlockLink(groupTable);
		this.modifyBlockLink1 = new BlockLink(groupTable);
		this.modifyBlockLink2 = new BlockLink(groupTable);
		
		int len = colNames.length;
		isDim = new boolean[len];
		isKey = new boolean[len];
		serialBytesLen = new int[len];
		
		int keyStart = -1; // 主键的起始字段
		
		// 主键起始字段前面的字段认为是排序字段
		for (int i = 0; i < len; ++i) {
			if (colNames[i].startsWith(KEY_PREFIX)) {
				keyStart = i;
				break;
			}
		}
		
		for (int i = 0; i < len; ++i) {
			if (colNames[i].startsWith(KEY_PREFIX)) {
				colNames[i] = colNames[i].substring(KEY_PREFIX.length());
				isDim[i] = true;
				isKey[i] = true;
			} else if (i < keyStart) {
				isDim[i] = true;
			}
		}

		this.colNames = colNames;
		init();
		
		if (sortedColNames == null) {
			hasPrimaryKey = false;
			isSorted = false;
		}
		tableList = new ArrayList<TableMetaData>();
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
	public RowTableMetaData(GroupTable groupTable, String []colNames, int []serialBytesLen,
			String tableName, RowTableMetaData parent) throws IOException {
		// 创建新数组，否则文件组时可能影响到其它分区组表的创建
		String []tmp = new String[colNames.length];
		System.arraycopy(colNames, 0, tmp, 0, colNames.length);
		colNames = tmp;

		this.groupTable = groupTable;
		this.tableName = tableName;
		this.parent = parent;
		dataBlockLink = new BlockLink(groupTable);
		this.segmentBlockLink = new BlockLink(groupTable);
		this.modifyBlockLink1 = new BlockLink(groupTable);
		this.modifyBlockLink2 = new BlockLink(groupTable);
		
		this.serialBytesLen = serialBytesLen;
		int len = colNames.length;
		isDim = new boolean[len];
		isKey = new boolean[len];
		for (int i = 0; i < len; ++i) {
			String name = colNames[i];
			if (name.startsWith("#")) {
				colNames[i] = name.substring(1);
				isDim[i] = isKey[i] = true;
			} else {
				isDim[i] = isKey[i] = false;
			}
		}
		this.colNames = colNames;
		init();
		
		if (getAllSortedColNames() == null) {
			hasPrimaryKey = false;
			isSorted = false;
		}
		
		if (parent != null) {
			//目前限制只依附一层
			if (parent.parent != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("ds.dsNotMatch"));
			}
			
			TableMetaData primaryTable = parent;
			String []primarySortedColNames = primaryTable.getSortedColNames();
			String []primaryColNames = primaryTable.getColNames();
			ArrayList<String> collist = new ArrayList<String>();
			for (String name : primaryColNames) {
				collist.add(name);
			}
			
			//字段不能与主表字段重复
			for (int i = 0; i < len; i++) {
				if (collist.contains(colNames[i])) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(colNames[i] + mm.getMessage("dw.fieldSameToPrimaryTable"));
				}
			}
			
			for (String name : primarySortedColNames) {
				collist.remove(name);
			}

			sortedColStartIndex = primarySortedColNames.length;
		}
		
		tableList = new ArrayList<TableMetaData>();
	}
	
	/**
	 * 根据src创建一个同构的基表
	 * @param groupTable 要创建基表的组表
	 * @param parent 主表对象
	 * @param src 提供结构的源基表
	 * @throws IOException
	 */
	public RowTableMetaData(GroupTable groupTable, RowTableMetaData parent, RowTableMetaData src) throws IOException {
		this.groupTable = groupTable;
		this.parent = parent;
		
		System.arraycopy(src.reserve, 0, reserve, 0, reserve.length);
		segmentCol = src.segmentCol;
		segmentSerialLen = src.segmentSerialLen;
		tableName = src.tableName;
		colNames = src.colNames;

		isDim = src.isDim;
		isKey = src.isKey;
		serialBytesLen = src.serialBytesLen;
		
		dataBlockLink = new BlockLink(groupTable);
		this.segmentBlockLink = new BlockLink(groupTable);
		this.modifyBlockLink1 = new BlockLink(groupTable);
		this.modifyBlockLink2 = new BlockLink(groupTable);
		
		init();
		
		if (getAllSortedColNames() == null) {
			hasPrimaryKey = false;
			isSorted = false;
		}
		
		tableList = new ArrayList<TableMetaData>();
		for (TableMetaData srcSub : src.tableList) {
			tableList.add(new RowTableMetaData(groupTable, this, (RowTableMetaData)srcSub));
		}
	}

	/**
	 * 初始化，读取维、列名等基本信息
	 */
	protected void init() {
		String[] col = colNames;
		int dimCount = getDimCount();
		int keyCount = getKeyCount();
		int count = col.length;

		if (dimCount > 0) {
			sortedColNames = new String[dimCount];
			int j = 0;
			for (int i = 0; i < count; ++i) {
				if (isDim(col[i])) {
					sortedColNames[j++] = col[i];
				}
			}
		}
		
		if (keyCount > 0) {
			allKeyColNames = new String[keyCount];
			int j = 0;
			for (int i = 0; i < count; ++i) {
				if (isKey(col[i])) {
					allKeyColNames[j++] = col[i];
				}
			}
		}
		
		if (parent != null) {
			TableMetaData primaryTable = parent;
			String []primarySortedColNames = primaryTable.getAllSortedColNames();
			sortedColStartIndex = primarySortedColNames.length;

			allColNames = new String[sortedColStartIndex + colNames.length];
			int i = 0;
			for (String colName : primarySortedColNames) {
				allColNames[i++] = colName;
			}
			for (String colName : colNames) {
				allColNames[i++] = colName;
			}
			
			allSortedColNames = new String[sortedColStartIndex + dimCount];
			i = 0;
			if (primarySortedColNames != null) {
				for (String colName : primarySortedColNames) {
					allSortedColNames[i++] = colName;
				}
			}
			if (sortedColNames != null) {
				for (String colName : sortedColNames) {
					allSortedColNames[i++] = colName;
				}
			}
			ds = new DataStruct(getAllColNames());
		} else {
			ds = new DataStruct(colNames);
		}
	}
	
	/**
	 * 返回所有维列名称
	 * @return
	 */
	public String[] getSortedColNames() {
		return sortedColNames;
	}

	/**
	 * 返回所有key列名称
	 * @return
	 */
	public String[] getAllKeyColNames() {
		return allKeyColNames;
	}
	
	/**
	 * 申请第一块空间
	 */
	protected void applyFirstBlock() throws IOException {
		if (segmentBlockLink.isEmpty()) {
			segmentBlockLink.setFirstBlockPos(groupTable.applyNewBlock());
			
			if (dataBlockLink.isEmpty()) {
				dataBlockLink.setFirstBlockPos(groupTable.applyNewBlock());
			}
		}
	}
	
	/**
	 * 准备写。在追加、删除、修改数据前调用。
	 * 调用后会对关键信息进行备份，防止写一般出意外时表数据损坏
	 */
	protected void prepareAppend() throws IOException {
		applyFirstBlock();
		
		colWriter = new BlockLinkWriter(dataBlockLink, true);
		segmentWriter = new BlockLinkWriter(segmentBlockLink, true);
		objectWriter = new ObjectWriter(segmentWriter, groupTable.getBlockSize() - GroupTable.POS_SIZE);
	}
	
	/**
	 * 结束写
	 */
	protected void finishAppend() throws IOException {
		colWriter.finishWrite();
		colWriter = null;
		
		objectWriter.flush();
		segmentWriter.finishWrite();
		segmentWriter = null;
		objectWriter = null;
		
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
		dataBlockLink.readExternal(reader);
		curModifyBlock = reader.readByte();
		modifyBlockLink1.readExternal(reader);
		modifyBlockLink2.readExternal(reader);
		
		int count = reader.readInt();
		colNames = new String[count];
		for (int i = 0; i < count; ++i) {
			colNames[i] = reader.readUTF();
		}
		serialBytesLen = new int[count];
		for (int i = 0; i < count; ++i) {
			serialBytesLen[i] = reader.readInt();
		}
		isDim = new boolean[count];
		for (int i = 0; i < count; ++i) {
			isDim[i] = reader.readBoolean();
		}
		
		if (reserve[0] > 0) {
			isKey = new boolean[count];
			for (int i = 0; i < count; ++i) {
				isKey[i] = reader.readBoolean();
			}
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
		tableList = new ArrayList<TableMetaData>(count);
		for (int i = 0; i < count; ++i) {
			TableMetaData table = new RowTableMetaData(groupTable, this);
			table.readExternal(reader);
			tableList.add(table);
		}
	}
	
	/**
	 * 写出表头数据
	 */
	public void writeExternal(BufferWriter writer) throws IOException {
		reserve[0] = 1;
		writer.write(reserve);
		writer.writeUTF(tableName);
		writer.writeStrings(colNames);
		writer.writeInt32(dataBlockCount);
		writer.writeLong40(totalRecordCount);
		segmentBlockLink.writeExternal(writer);
		dataBlockLink.writeExternal(writer);
		writer.writeByte(curModifyBlock);
		modifyBlockLink1.writeExternal(writer);
		modifyBlockLink2.writeExternal(writer);
		
		String []colNames = this.colNames;
		int count = colNames.length;
		writer.writeInt(count);
		for (int i = 0; i < count; ++i) {
			writer.writeUTF(colNames[i]);
		}
		for (int i = 0; i < count; ++i) {
			writer.writeInt(serialBytesLen[i]);
		}
		for (int i = 0; i < count; ++i) {
			writer.writeBoolean(isDim[i]);
		}
		
		//版本1增加
		for (int i = 0; i < count; ++i) {
			writer.writeBoolean(isKey[i]);
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
		
		ArrayList<TableMetaData> tableList = this.tableList;
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
		Record r;
		int count = allColNames.length;
		boolean isDim[] = getDimIndex();
		Object []minValues = null;//一块的最小维值
		Object []maxValues = null;//一块的最大维值
		
		if (sortedColNames != null) {
			minValues = new Object[count];
			maxValues = new Object[count];
		}
		
		RowBufferWriter bufferWriter= new RowBufferWriter(groupTable.getStructManager());
		long recNum = totalRecordCount;
		
		int end = data.length();
		for (int i = 1; i <= end; ++i) {
			//写伪号
			bufferWriter.writeObject(++recNum);
			//写导列
			bufferWriter.writeObject(recList.get(i - 1));
			
			r = (Record) data.get(i);
			Object[] vals = r.getFieldValues();
			//写一条到buffer
			for (int j = 0; j < count; j++) {
				if (!isMyCol[j]) continue;
				Object obj = vals[j];
				bufferWriter.writeObject(obj);
			}
			for (int j = 0; j < count; j++) {
				if (!isMyCol[j]) continue;
				Object obj = vals[j];
				if (isDim[j]) {
					if (Variant.compare(obj, maxValues[j], true) > 0)
						maxValues[j] = obj;
					if (i == 1)
						minValues[j] = obj;//第一个要赋值，因为null表示最小
					if (Variant.compare(obj, minValues[j], true) < 0)
						minValues[j] = obj;
				}
			}
		}

		if (recList.size() == 0) {
			//如果是空块，则写一个null
			bufferWriter.writeObject(null);
		}
		
		if (sortedColNames == null) {
			//提交buffer到行块
			long pos = colWriter.writeDataBuffer(bufferWriter.finish());
			//更新分段信息
			appendSegmentBlock(end);
			objectWriter.writeLong40(pos);
		} else {
			//提交buffer到行块
			long pos = colWriter.writeDataBuffer(bufferWriter.finish());
			//更新分段信息
			appendSegmentBlock(end);
			objectWriter.writeLong40(pos);
			for (int i = 0; i < count; ++i) {
				if (!isMyCol[i]) continue;
				if (isDim[i]) {
					objectWriter.writeObject(minValues[i]);
					objectWriter.writeObject(maxValues[i]);
				}
			}
		}
	}
	
	/**
	 * 把data序列的指定范围的数据写出
	 * @param data 数据序列
	 * @param start 开始位置
	 * @param end 结束位置
	 * @throws IOException
	 */
	protected void appendDataBlock(Sequence data, int start, int end) throws IOException {
		Record r;
		int count = colNames.length;
		boolean isDim[] = getDimIndex();
		Object []minValues = null;//一块的最小维值
		Object []maxValues = null;//一块的最大维值

		if (sortedColNames != null) {
			minValues = new Object[count];
			maxValues = new Object[count];
		}

		RowBufferWriter bufferWriter= new RowBufferWriter(groupTable.getStructManager());
		long recNum = totalRecordCount;
		
		for (int i = start; i <= end; ++i) {
			r = (Record) data.get(i);
			Object[] vals = r.getFieldValues();
			//把一条的所有列写到buffer
			bufferWriter.writeObject(++recNum);//行存要先写一个伪号
			for (int j = 0; j < count; j++) {
				Object obj = vals[j];
				bufferWriter.writeObject(obj);
				if (isDim[j]) {
					if (Variant.compare(obj, maxValues[j], true) > 0)
						maxValues[j] = obj;
					if (i == start)
						minValues[j] = obj;//第一个要赋值，因为null表示最小
					if (Variant.compare(obj, minValues[j], true) < 0)
						minValues[j] = obj;
				}
			}
		}
		
		//写数据时不压缩
		if (sortedColNames == null) {
			//提交buffer到行块
			long pos = colWriter.writeDataBuffer(bufferWriter.finish());
			//更新分段信息
			appendSegmentBlock(end - start + 1);
			objectWriter.writeLong40(pos);
		} else {
			//提交buffer到行块
			long pos = colWriter.writeDataBuffer(bufferWriter.finish());
			//更新分段信息
			appendSegmentBlock(end - start + 1);
			objectWriter.writeLong40(pos);
			for (int i = 0; i < count; ++i) {
				if (isDim[i]) {
					objectWriter.writeObject(minValues[i]);
					objectWriter.writeObject(maxValues[i]);
				}
			}
		}
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
		TableMetaData primaryTable = parent;
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
		
		RowCursor cs;
		if (primaryTable.totalRecordCount == 0) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("dw.baseTableNull"));
		}
		cs = (RowCursor) primaryTable.cursor(primaryTableKeys);
		cs.setSegment(curBlockCount, curBlockCount + 1);
		Sequence pkeyData = cs.fetch(ICursor.MAXSIZE);
		int pkeyIndex = 1;

		int pkeyDataLen = pkeyData.length();
		GroupTableRecord curPkey = (GroupTableRecord) pkeyData.get(1);
		Object []curPkeyVals = curPkey.getFieldValues();

		String []allSortedColNames = getAllSortedColNames();
		int sortedColCount = allSortedColNames.length;
		Object []tableMaxValues = this.maxValues;
		Object []lastValues = new Object[sortedColCount];//上一条维的值
		
		LongArray guideCol = new LongArray(MIN_BLOCK_RECORD_COUNT);
		Sequence seq = new Sequence(MIN_BLOCK_RECORD_COUNT);
		Sequence data = cursor.fetch(ICursor.FETCHCOUNT);
		Record r;
		Object []vals = new Object[sortedColCount];
		int []findex = new int[sortedColCount];
		DataStruct ds = data.dataStruct();
		for (int f = 0; f < sortedColCount; ++f) {
			findex[f] = ds.getFieldIndex(allSortedColNames[f]);
		}
		
		while (data != null && data.length() > 0) {
			int len = data.length();
			for (int i = 1; i <= len; ++i) {
				r = (Record) data.get(i);
				for (int f = 0; f < sortedColCount; ++f) {
					Object obj = r.getNormalFieldValue(findex[f]);
					vals[f] = obj;
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
							cs = (RowCursor) primaryTable.cursor(primaryTableKeys);
							cs.setSegment(curBlockCount, curBlockCount + 1);
							pkeyData = cs.fetch(ICursor.MAXSIZE);
							pkeyIndex = 1;
							pkeyDataLen = pkeyData.length();
						}
						curPkey = (GroupTableRecord) pkeyData.get(pkeyIndex);
						curPkeyVals = curPkey.getFieldValues();
					} else if (cmp > 0) {
						//不应该出现，抛异常
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
		int sortedColCount = sortedColNames.length;
		Object []tableMaxValues = this.maxValues;

		String []sortedColNames = this.sortedColNames;
		String segmentCol = groupTable.baseTable.getSegmentCol();
		int segmentSerialLen = groupTable.baseTable.getSegmentSerialLen();
		int segmentIndex = 0;

		for (int i = 0; i < sortedColCount; i++) {
			if (segmentCol.equals(sortedColNames[i])) {
				segmentIndex = i;
				break;
			}
		}
		int cmpLen = segmentIndex + 1;
		int serialBytesLen = getSerialBytesLen(segmentIndex);
		if (segmentSerialLen == 0 || segmentSerialLen > serialBytesLen) {
			segmentSerialLen = serialBytesLen;
		}
		Object []lastValues = new Object[cmpLen];//上一条维的值
		Object []curValues = new Object[cmpLen];//当前条维的值
		
		Sequence seq = new Sequence(MIN_BLOCK_RECORD_COUNT);
		Sequence data = cursor.fetch(ICursor.FETCHCOUNT);
		Record r;
		Object []vals = new Object[sortedColCount];
		int []findex = getSortedColIndex();
		
		while (data != null && data.length() > 0) {
			int len = data.length();
			for (int i = 1; i <= len; ++i) {
				r = (Record) data.get(i);
				for (int f = 0; f < sortedColCount; ++f) {
					vals[f] = r.getNormalFieldValue(findex[f]);
				}

				//这里判断是否够一个列块了
				if (recCount >= MIN_BLOCK_RECORD_COUNT){
					if (serialBytesLen < 1) {
						System.arraycopy(vals, 0, curValues, 0, cmpLen);
					} else {
						System.arraycopy(vals, 0, curValues, 0, segmentIndex);
						Long val;
						Object  obj = vals[segmentIndex];
						if (obj instanceof Integer) {
							val = (Integer)obj & 0xFFFFFFFFL;
						} else {
							val = (Long) obj;
						}
						curValues[segmentIndex] = val >>> (serialBytesLen - segmentSerialLen) * 8;
					}
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
				
				//把这条暂存
				seq.add(r);
				
				if (serialBytesLen < 1) {
					System.arraycopy(vals, 0, lastValues, 0, cmpLen);
				} else {
					System.arraycopy(vals, 0, lastValues, 0, segmentIndex);//存上一条维值
					Long val;
					Object  obj = vals[segmentIndex];
					if (obj instanceof Integer) {
						val = (Integer)obj & 0xFFFFFFFFL;
					} else {
						val = (Long) obj;
					}
					lastValues[segmentIndex] = val >>> (serialBytesLen - segmentSerialLen) * 8;
				}
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
		int sortedColCount = sortedColNames.length;
		Object []tableMaxValues = this.maxValues;
		Object []lastValues = new Object[sortedColCount];//上一条维的值
		
		Sequence seq = new Sequence(MIN_BLOCK_RECORD_COUNT);
		Sequence data = cursor.fetch(ICursor.FETCHCOUNT);
		Record r;
		Object []vals = new Object[sortedColCount];
		int []findex = getSortedColIndex();

		while (data != null && data.length() > 0) {
			int len = data.length();
			for (int i = 1; i <= len; ++i) {
				r = (Record) data.get(i);
				for (int f = 0; f < sortedColCount; ++f) {
					vals[f] = r.getNormalFieldValue(findex[f]);
				}

				//这里判断是否够一个列块了
				if (recCount >= MAX_BLOCK_RECORD_COUNT) {
					//这时提交一半
					appendDataBlock(seq, 1, MAX_BLOCK_RECORD_COUNT/2);
					seq = seq.get(MAX_BLOCK_RECORD_COUNT/2 + 1, seq.length() + 1);
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
							if (hasPrimaryKey) hasPrimaryKey = false;
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
	
	/**
	 * 追加数据
	 */
	public void append(ICursor cursor) throws IOException {
		// 如果没有维字段则取GroupTable.MIN_BLOCK_RECORD_COUNT条记录
		// 否则假设有3个维字段d1、d2、d3，根据维字段的值取出至少MIN_BLOCK_RECORD_COUNT条记录
		// 如果[d1,d2,d3]是主键则不要把[d1,d2]值相同的给拆到两个块里，反之不要把[d1,d2,d3]值相同的拆到两个块里
		// 如果相同的超过了MAX_BLOCK_RECORD_COUNT，则以MAX_BLOCK_RECORD_COUNT / 2条为一块
		// 把每一列的数据写到BufferWriter然后调用finish得到字节数组，再调用compress压缩数据，最后写进ColumnMetaData
		// 有维字段时要更新maxValues、hasPrimaryKey两个成员，如果hasPrimaryKey为false则不再更新
		if (cursor == null) {
			return;
		}

		getGroupTable().checkWritable();
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
		
		String []colNames = getAllColNames();
		int count = colNames.length;
		for (int i = 0; i < count; i++) {
			if (!ds.getFieldName(i).equals(colNames[i])) {
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
		} else if (sortedColNames == null) {
			appendNormal(cursor);
		} else if (groupTable.baseTable.getSegmentCol() == null) {
			appendSorted(cursor);
		} else {
			appendSegment(cursor);
		}
		
		// 结束写数据，保存到文件
		finishAppend();
	}

	protected void appendSegmentBlock(int recordCount) throws IOException {
		dataBlockCount++;
		totalRecordCount += recordCount;
		objectWriter.writeInt32(recordCount);
	}
	
	/**
	 * 取字段过滤优先级
	 * @param name
	 * @return
	 */
	int getColumnFilterPriority(String name) {
		if (sortedColNames != null) {
			int len = sortedColNames.length;
			for (int i = 0; i < len; ++i) {
				if (sortedColNames[i] == name) {
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
		getGroupTable().checkReadable();
		ICursor cs = new RowCursor(this);
		TableMetaData tmd = getSupplementTable(false);
		if (tmd == null) {
			return cs;
		} else {
			ICursor cs2 = tmd.cursor();
			return merge(cs, cs2);
		}
	}
	
	public ICursor cursor(String []fields) {
		getGroupTable().checkReadable();
		ICursor cs = new RowCursor(this, fields);
		TableMetaData tmd = getSupplementTable(false);
		if (tmd == null) {
			return cs;
		} else {
			ICursor cs2 = tmd.cursor(fields);
			return merge(cs, cs2);
		}
	}
	
	public ICursor cursor(String []fields, Expression exp, Context ctx) {
		getGroupTable().checkReadable();
		ICursor cs = new RowCursor(this, fields, exp, ctx);
		TableMetaData tmd = getSupplementTable(false);
		if (tmd == null) {
			return cs;
		} else {
			ICursor cs2 = tmd.cursor(fields, exp, ctx);
			return merge(cs, cs2);
		}
	}

	public ICursor cursor(Expression[] exps, String[] fields, Expression filter, Context ctx) {
		getGroupTable().checkReadable();
		ICursor cs = new RowCursor(this, null, filter, exps, fields, ctx);
		TableMetaData tmd = getSupplementTable(false);
		if (tmd == null) {
			return cs;
		} else {
			ICursor cs2 = tmd.cursor(exps, fields, filter, null, null, null, ctx);
			return merge(cs, cs2);
		}
	}

	public ICursor cursor(String []fields, Expression filter, Context ctx, int segSeq, int segCount) {
		getGroupTable().checkReadable();
		return cursor(null, fields, filter, ctx, segSeq, segCount);
	}
	
	public ICursor cursor(Expression []exps, String []fields, Expression filter, Context ctx, int segSeq, int segCount) {
		getGroupTable().checkReadable();
		if (filter != null) {
			filter = filter.newExpression(ctx); // 分段并行读取时需要复制表达式，同一个表达式不支持并行运算
		}
		
		String []fetchFields;
		if (exps != null) {
			int colCount = exps.length;
			fetchFields = new String[colCount];
			for (int i = 0; i < colCount; ++i) {
				if (exps[i] == null) {
					exps[i] = Expression.NULL;
				}
				
				if (exps[i].getHome() instanceof UnknownSymbol) {
					fetchFields[i] = exps[i].getIdentifierName();
				}
			}
		} else {
			fetchFields = fields;
			fields = null;
		}
		
		RowCursor cursor = new RowCursor(this, fetchFields, filter, exps, fields, ctx);
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
	public ICursor cursor(String []fields, Expression exp, MultipathCursors mcs, Context ctx) {
		getGroupTable().checkReadable();
		//行存不支持多路
		MessageManager mm = EngineMessage.get();
		throw new RQException("cursor" + mm.getMessage("dw.needMCursor"));
	}
	
	public ICursor cursor(String []fields, Expression filter, String []fkNames, Sequence []codes, String []opts, Context ctx) {
		if (fkNames == null) {
			return cursor(fields, filter, ctx);
		} else {
			throw new RQException("K:T param is unimplemented in row group table!");
		}
	}

	public ICursor cursor(String []fields, Expression filter, String []fkNames, Sequence []codes, 
			String []opts, Context ctx, int pathCount) {
		if (fkNames == null) {
			return cursor(fields, filter, ctx, pathCount);
		} else {
			throw new RQException("K:T param is unimplemented in row group table!");
		}
	}
	
	public ICursor cursor(String []fields, Expression filter, String []fkNames, Sequence []codes, 
			String []opts, Context ctx, int pathSeq, int pathCount) {
		if (fkNames == null) {
			return cursor(fields, filter, ctx, pathSeq, pathCount);
		} else {
			throw new RQException("K:T param is unimplemented in row group table!");
		}
	}
	
	public ICursor cursor(Expression []exps, String []fields, Expression filter, String []fkNames, Sequence []codes,
			String []opts, Context ctx) {
		if (fkNames == null) {
			return cursor(exps, fields, filter, ctx);
		} else {
			throw new RQException("K:T param is unimplemented in row group table!");
		}
	}
	
	public ICursor cursor(Expression []exps, String []fields, Expression filter, String []fkNames, Sequence []codes,
			String []opts, Context ctx, int pathCount) {
		if (fkNames == null) {
			return cursor(exps, fields, filter, ctx, pathCount);
		} else {
			throw new RQException("K:T param is unimplemented in row group table!");
		}
	}
	
	public ICursor cursor(Expression []exps, String []fields, Expression filter, String []fkNames, Sequence []codes,
			String []opts, Context ctx, int pathSeq, int pathCount) {
		if (fkNames == null) {
			return cursor(exps, fields, filter, ctx, pathSeq, pathCount);
		} else {
			throw new RQException("K:T param is unimplemented in row group table!");
		}
	}
	
	public ICursor cursor(String []fields, Expression exp, String []fkNames, Sequence []codes, 
			String []opts, MultipathCursors mcs, String opt, Context ctx) {
		if (fkNames == null) {
			return cursor(fields, exp, mcs, ctx);
		} else {
			throw new RQException("K:T param is unimplemented in row group table!");
		}
	}

	/**
	 * 有补文件时的数据更新
	 * @param stmd
	 * @param data
	 * @param opt
	 * @return
	 * @throws IOException
	 */
	private Sequence update(TableMetaData stmd, Sequence data, String opt) throws IOException {
		boolean isUpdate = true, isInsert = true;
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
		
		String[] columns = getAllSortedColNames();
		int keyCount = columns.length;
		int []keyIndex = new int[keyCount];
		for (int k = 0; k < keyCount; ++k) {
			keyIndex[k] = ds.getFieldIndex(columns[k]);
			if (keyIndex[k] < 0) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(columns[k] + mm.getMessage("ds.fieldNotExist"));
			}
		}
		
		boolean isPrimaryTable = parent == null;
		int len = data.length();
		long []seqs = new long[len + 1];
		int []block = new int[len + 1];//是否在一个段的底部insert(子表)
		long []recNum = null;
		int []temp = new int[1];
		
		if (isPrimaryTable) {
			RowRecordSeqSearcher searcher = new RowRecordSeqSearcher(this);
			if (keyCount == 1) {
				int k = keyIndex[0];
				for (int i = 1; i <= len; ++i) {
					Record r = (Record)data.getMem(i);
					seqs[i] = searcher.findNext(r.getFieldValue(k));
				}
			} else {
				Object []keyValues = new Object[keyCount];
				for (int i = 1; i <= len; ++i) {
					Record r = (Record)data.getMem(i);
					for (int k = 0; k < keyCount; ++k) {
						keyValues[k] = r.getFieldValue(keyIndex[k]);
					}
					
					seqs[i] = searcher.findNext(keyValues);
				}
			}
		} else {
			recNum  = new long[len + 1];//子表对应到主表的伪号，0表示在主表补区
			RowTableMetaData baseTable = (RowTableMetaData) this.groupTable.baseTable;
			RowRecordSeqSearcher baseSearcher = new RowRecordSeqSearcher(baseTable);
			RowRecordSeqSearcher2 searcher = new RowRecordSeqSearcher2(this);
			if (keyCount == 1) {
				int k = keyIndex[0];
				for (int i = 1; i <= len; ++i) {
					Record r = (Record)data.getMem(i);
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
					Record r = (Record)data.getMem(i);
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
				Record sr = (Record)data.getMem(i);
				if (seqs[i] > 0) {
					if (isUpdate) {
						ModifyRecord r = new ModifyRecord(seqs[i], ModifyRecord.STATE_UPDATE, sr);
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
								Record sr = (Record)data.getMem(t);
								mr.setRecord(sr, ModifyRecord.STATE_UPDATE);
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
							Record sr = (Record)data.getMem(t);
							mr = new ModifyRecord(seq2, ModifyRecord.STATE_UPDATE, sr);
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
							int cmp = mr.getRecord().compare((Record)data.getMem(t), keyIndex);
							if (cmp < 0) {
								s++;
								tmp.add(mr);
							} else if (cmp == 0) {
								if (isUpdate) {
									Record sr = (Record)data.getMem(t);
									mr.setRecord(sr);
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
				Record sr = (Record)data.getMem(t);
				if (seqs[t] > 0) {
					if (isUpdate) {
						ModifyRecord r = new ModifyRecord(seqs[t], ModifyRecord.STATE_UPDATE, sr);
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
		
		saveModifyRecords();
		
		if (isPrimaryTable && needUpdateSubTable) {
			//主表有insert，就必须更新所有子表补区
			ArrayList<TableMetaData> tableList = getTableList();
			for (int i = 0, size = tableList.size(); i < size; ++i) {
				RowTableMetaData t = ((RowTableMetaData)tableList.get(i));
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
		
		groupTable.save();
		return result;
	}
	
	/**
	 * 更新
	 */
	public Sequence update(Sequence data, String opt) throws IOException {
		if (!hasPrimaryKey) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("dw.lessKey"));
		}
		
		GroupTable groupTable = getGroupTable();
		groupTable.checkWritable();
		TableMetaData tmd = getSupplementTable(false);
		if (tmd != null) {
			return update(tmd, data, opt);
		}
		
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
		
		String[] columns = getAllSortedColNames();
		int keyCount = columns.length;
		int []keyIndex = new int[keyCount];
		for (int k = 0; k < keyCount; ++k) {
			keyIndex[k] = ds.getFieldIndex(columns[k]);
			if (keyIndex[k] < 0) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(columns[k] + mm.getMessage("ds.fieldNotExist"));
			}
		}
		
		boolean isPrimaryTable = parent == null;
		int len = data.length();
		long []seqs = new long[len + 1];
		int []block = new int[len + 1];//是否在一个段的底部insert(子表)
		long []recNum = null;
		int []temp = new int[1];
		
		if (isPrimaryTable) {
			RowRecordSeqSearcher searcher = new RowRecordSeqSearcher(this);
			
			if (keyCount == 1) {
				int k = keyIndex[0];
				for (int i = 1; i <= len; ++i) {
					Record r = (Record)data.getMem(i);
					seqs[i] = searcher.findNext(r.getFieldValue(k));
				}
			} else {
				Object []keyValues = new Object[keyCount];
				for (int i = 1; i <= len; ++i) {
					Record r = (Record)data.getMem(i);
					for (int k = 0; k < keyCount; ++k) {
						keyValues[k] = r.getFieldValue(keyIndex[k]);
					}
					
					seqs[i] = searcher.findNext(keyValues);
				}
			}
		} else {
			recNum  = new long[len + 1];//子表对应到主表的伪号，0表示在主表补区
			RowTableMetaData baseTable = (RowTableMetaData) this.groupTable.baseTable;
			RowRecordSeqSearcher baseSearcher = new RowRecordSeqSearcher(baseTable);
			RowRecordSeqSearcher2 searcher = new RowRecordSeqSearcher2(this);
			if (keyCount == 1) {
				int k = keyIndex[0];
				for (int i = 1; i <= len; ++i) {
					Record r = (Record)data.getMem(i);
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
					Record r = (Record)data.getMem(i);
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
				Record sr = (Record)data.getMem(i);
				if (seqs[i] > 0) {
					if (isUpdate) {
						ModifyRecord r = new ModifyRecord(seqs[i], ModifyRecord.STATE_UPDATE, sr);
						r.setParentRecordSeq(recNum[i]);
						modifyRecords.add(r);
						if (result != null) {
							result.add(sr);
						}
					}
				} else if (isInsert) {
					long seq = -seqs[i];
					if (seq <= totalRecordCount || block[i] > 0) {
						ModifyRecord r = new ModifyRecord(seq, ModifyRecord.STATE_INSERT, sr);
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
								Record sr = (Record)data.getMem(t);
								mr.setRecord(sr, ModifyRecord.STATE_UPDATE);
								mr.setParentRecordSeq(recNum[t]);
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
							Record sr = (Record)data.getMem(t);
							mr = new ModifyRecord(seq2, ModifyRecord.STATE_UPDATE, sr);
							mr.setParentRecordSeq(recNum[t]);
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
							int cmp = mr.getRecord().compare((Record)data.getMem(t), keyIndex);
							if (cmp < 0) {
								s++;
								tmp.add(mr);
							} else if (cmp == 0) {
								if (isUpdate) {
									Record sr = (Record)data.getMem(t);
									mr.setRecord(sr);
									if (result != null) {
										result.add(sr);
									}
								}
								
								tmp.add(mr);
								s++;
								t++;
							} else {
								if (isInsert) {
									Record sr = (Record)data.getMem(t);
									mr = new ModifyRecord(seq2, ModifyRecord.STATE_INSERT, sr);
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
								Record sr = (Record)data.getMem(t);
								mr = new ModifyRecord(seq2, ModifyRecord.STATE_INSERT, sr);
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
							Record sr = (Record)data.getMem(t);
							mr = new ModifyRecord(seq2, ModifyRecord.STATE_INSERT, sr);
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
				Record sr = (Record)data.getMem(t);
				if (seqs[t] > 0) {
					if (isUpdate) {
						ModifyRecord r = new ModifyRecord(seqs[t], ModifyRecord.STATE_UPDATE, sr);
						tmp.add(r);
						if (result != null) {
							result.add(sr);
						}
					}
				} else if (isInsert) {
					long seq = -seqs[t];
					if (seq <= totalRecordCount) {
						ModifyRecord r = new ModifyRecord(seq, ModifyRecord.STATE_INSERT, sr);
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
			ArrayList<TableMetaData> tableList = getTableList();
			for (int i = 0, size = tableList.size(); i < size; ++i) {
				RowTableMetaData t = ((RowTableMetaData)tableList.get(i));
				boolean needSave = t.update(modifyRecords);
				if (needSave) {
					t.saveModifyRecords();
				}
			}
		}
		
		if (append.length() > 0) {
			ICursor cursor = new MemoryCursor(append);
			append(cursor);
		} else {
			groupTable.save();
		}
		
		return result;
	}
	
	public Sequence delete(Sequence data, String opt) throws IOException {
		if (!hasPrimaryKey) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("dw.lessKey"));
		}
		
		GroupTable groupTable = getGroupTable();
		groupTable.checkWritable();
		
		Sequence result1 = null;
		TableMetaData tmd = getSupplementTable(false);
		if (tmd != null) {
			// 有补文件时先删除补文件中的数据，补文件中不存在的再在源文件中删除
			result1 = tmd.delete(data, "n");
			data = data.diff(result1, false);
		}
		
		appendCache();
		boolean nopt = opt != null && opt.indexOf('n') != -1;
		long totalRecordCount = this.totalRecordCount;
		if (totalRecordCount == 0 || data == null || data.length() == 0) {
			return nopt ? result1 : null;
		}
		
		Sequence result = null;
		if (nopt) {
			result = new Sequence();
		}
		
		boolean deleteByBaseKey = false;//只用于内部删除子表的列区，此时不会有@n
		if (opt != null && opt.indexOf('s') != -1) {
			deleteByBaseKey = true;
		}
		
		DataStruct ds = data.dataStruct();
		if (ds == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.needPurePmt"));
		}
				
		String[] columns = getAllSortedColNames();
		int keyCount = columns.length;
		if (deleteByBaseKey) {
			keyCount = sortedColStartIndex;
		}
		int []keyIndex = new int[keyCount];
		for (int k = 0; k < keyCount; ++k) {
			keyIndex[k] = ds.getFieldIndex(columns[k]);
			if (keyIndex[k] < 0) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(columns[k] + mm.getMessage("ds.fieldNotExist"));
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
			seqList.add(0);
			seqListData = new Sequence(len);
		}
		
		if (isPrimaryTable) {
			RowRecordSeqSearcher searcher = new RowRecordSeqSearcher(this);
			
			if (keyCount == 1) {
				int k = keyIndex[0];
				for (int i = 1; i <= len; ++i) {
					Record r = (Record)data.getMem(i);
					seqs[i] = searcher.findNext(r.getFieldValue(k));
				}
			} else {
				Object []keyValues = new Object[keyCount];
				for (int i = 1; i <= len; ++i) {
					Record r = (Record)data.getMem(i);
					for (int k = 0; k < keyCount; ++k) {
						keyValues[k] = r.getFieldValue(keyIndex[k]);
					}
					
					seqs[i] = searcher.findNext(keyValues);
				}
			}
		} else {
			RowRecordSeqSearcher2 searcher = new RowRecordSeqSearcher2(this);
			
			Object []keyValues = new Object[keyCount];
			int baseKeyCount = sortedColStartIndex;
			Object []baseKeyValues = new Object[baseKeyCount];
			
			for (int i = 1; i <= len;) {
				Record r = (Record)data.getMem(i);
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
			len = seqList.size() - 1;
			if (0 == len) {
				return result;
			}
			seqs = seqList.toArray();
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
							int cmp = mr.getRecord().compare((Record)data.getMem(t), keyIndex);
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
							int cmp = mr.getRecord().compare((Record)data.getMem(t), keyIndex);
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
				ArrayList<TableMetaData> tableList = getTableList();
				int size = tableList.size();
				for (int i = 0; i < size; ++i) {
					RowTableMetaData t = ((RowTableMetaData)tableList.get(i));
					t.delete(data, "s");//删除子表列区
					t.delete(data);//删除子表补区
				}
				
				//主表有删除，补区的位置会变化，还要同步子表补区
				for (int i = 0; i < size; ++i) {
					RowTableMetaData t = ((RowTableMetaData)tableList.get(i));
					t.update(this.modifyRecords);
					t.saveModifyRecords();
				}
			}
			
			if (!deleteByBaseKey) {
				saveModifyRecords();
			}
		}
		
		if (!deleteByBaseKey) {
			groupTable.save();
		}
		
		if (nopt) {
			result.addAll(result1);
		}
		
		return result;
	}
	
	/**
	 * 根据基表的补区，同步更新自己的补区
	 * @param baseModifyRecords 基表的补区记录
	 * @return
	 * @throws IOException
	 */
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
			Record mrec = mr.getRecord();
			
			if (mr.getState() != ModifyRecord.STATE_DELETE) {
				for (ModifyRecord r : modifyRecords) {
					if (r.getState() == ModifyRecord.STATE_DELETE) {
						continue;
					}
					
					Record rec = r.getRecord();
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
	
	/**
	 * 根据data删除子表的补区
	 * @param data
	 * @return
	 * @throws IOException
	 */
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
			Record mrec = (Record) data.get(i);
			srcModifyRecords.clear();
			srcModifyRecords.addAll(tmp);
			tmp.clear();
			for (ModifyRecord r : srcModifyRecords) {
				int state = r.getState();
				if (state == ModifyRecord.STATE_UPDATE) {
					Record rec = r.getRecord();
					int cmp = rec.compare(mrec, index);
					if (cmp == 0) {
						r.setDelete();
						r.setRecord(null);
						delete = true;
					}
					tmp.add(r);
				} else if (state == ModifyRecord.STATE_INSERT) {
					Record rec = r.getRecord();
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
	
	/**
	 * 返回reader（可选是否读第一块）
	 * @param isLoadFirstBlock
	 * @return
	 */
	public BlockLinkReader getRowReader(boolean isLoadFirstBlock) {
		BlockLinkReader reader = new BlockLinkReader(dataBlockLink);
		reader.setDecompressBufferSize(4096);
		
		if (isLoadFirstBlock) {
			try {
				reader.loadFirstBlock();
			} catch (IOException e) {
				throw new RQException(e.getMessage(), e);
			}
		}
		
		return reader;
	}

	public ObjectReader getSegmentObjectReader() {
		BlockLinkReader segmentReader = new BlockLinkReader(segmentBlockLink);
		try {
			segmentReader.loadFirstBlock();
			return new ObjectReader(segmentReader, groupTable.getBlockSize() - GroupTable.POS_SIZE);
		} catch (IOException e) {
			segmentReader.close();
			throw new RQException(e.getMessage(), e);
		}
	}
	
	/**
	 * 获得维字段的位置
	 * @return
	 */
	public boolean[] getDimIndex() {
		String[] col = getAllColNames();
		boolean []isDim = new boolean[col.length];

		for (int i = 0, len = col.length; i < len; i++) {
			if (isDim(col[i])) {
				isDim[i] = true;
			} else {
				isDim[i] = false;
			}
		}
		return isDim;
	}

	/**
	 * 判断是否是维
	 * @param colName
	 * @return
	 */
	protected boolean isDim(String colName) {
		boolean isDim[] = this.isDim;
		String colNames[] = this.colNames;
		int len = colNames.length;
		
		for (int i = 0; i < len; ++i) {
			if (colName.equals(colNames[i])) {
				return isDim[i];
			}
		}
		return false;
	}
	
	/**
	 * 判断是否是key
	 * @param colName
	 * @return
	 */
	protected boolean isKey(String colName) {
		boolean isKey[] = this.isDim;
		String colNames[] = this.colNames;
		int len = colNames.length;
		
		for (int i = 0; i < len; ++i) {
			if (colName.equals(colNames[i])) {
				return isKey[i];
			}
		}
		return false;
	}
	
	/**
	 * 得到维字段个数
	 * @return
	 */
	protected int getDimCount() {
		boolean isDim[] = this.isDim; 
		int len = isDim.length;
		int count = 0;
		
		for (int i = 0; i < len; ++i) {
			if (isDim[i]) {
				count++;
			}
		}
		return count;
	}
	
	/**
	 * 得到维字段个数
	 * @return
	 */
	protected int getKeyCount() {
		boolean isKey[] = this.isKey; 
		int len = isKey.length;
		int count = 0;
		
		for (int i = 0; i < len; ++i) {
			if (isKey[i]) {
				count++;
			}
		}
		return count;
	}
	
	protected int getSerialBytesLen(int index) {
		return serialBytesLen[index];
	}
	
	public int[] getSerialBytesLen() {
		return serialBytesLen;
	}
	
	public String[] getAllSortedColNames() {
		if (parent == null) return getSortedColNames();
		return allSortedColNames;
	}
	
	/**
	 * 返回所有字段名称，包含主表、子表所有字段
	 * @return
	 */
	public String[] getTotalColNames() {
		if (parent == null) return colNames;
		int len = getTotalColCount();
		int baseColCount = parent.colNames.length;
		String[] names = new String[len];
		System.arraycopy(parent.colNames, 0, names, 0, baseColCount);
		System.arraycopy(colNames, 0, names, baseColCount, colNames.length);		
		return names;
	}
	
	/**
	 * 总字段数
	 * @return
	 */
	public int getTotalColCount() {
		if (parent == null) return colNames.length;
		return parent.colNames.length + colNames.length;
	}
	
	/**
	 * 所有字段的排号长度（包含主表、子表所有字段）
	 * @return
	 */
	public int[] getTotalSerialBytesLen() {
		if (parent == null) return serialBytesLen;
		int len = getTotalColCount();
		int []baseSerialBytesLen = parent.getSerialBytesLen();
		int baseColCount = baseSerialBytesLen.length;
		int[] serialBytesLen = new int[len];
		System.arraycopy(baseSerialBytesLen, 0, serialBytesLen, 0, baseColCount);
		System.arraycopy(this.serialBytesLen, 0, serialBytesLen, baseColCount, this.serialBytesLen.length);		
		return serialBytesLen;
	}
	
	/**
	 * 行存不支持多路
	 * @param fields
	 * @param filter
	 * @param ctx
	 * @param pathCount
	 * @return
	 */
	public ICursor cursor(String[] fields, Expression filter, Context ctx, int pathCount) {
		MessageManager mm = EngineMessage.get();
		throw new RQException("cursor" + mm.getMessage("dw.needMCursor"));
	}
	
	public ICursor cursor(Expression[] exps, String[] fields, Expression filter, Context ctx, int pathCount) {
		return cursor(exps, fields, filter, null, null, null, pathCount, ctx);
	}
	
	public Table finds(Sequence values) throws IOException {
		return finds(values, null);
	}

	public Table finds(Sequence values, String[] selFields) throws IOException {
		getGroupTable().checkReadable();
		
		if (!hasPrimaryKey()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("dw.lessKey"));
		}
		
		String []keys = getAllSortedColNames();
		String key0 = keys[0];
		Object obj = values.get(1);
		if (obj instanceof Sequence) {
			ListBase1 mem = values.getMems();
			Sequence newSeq = new Sequence(mem.size());
			ListBase1 newMem = newSeq.getMems();
			for (int i = 1; i < mem.size(); i++) {
				Sequence seq = (Sequence) mem.get(i);
				Object obj1 = seq.get(1);
				newMem.set(i, obj1);
			}
			values = newSeq;
		}
		Context ctx = new Context();
		ctx.addParam(new Param("values", Param.VAR, values));
		Expression exp = new Expression("values.contain(" + key0 + ")"); 
		Sequence result = cursor(selFields, exp, ctx).fetch();
		Table table = new Table(result.dataStruct());
		table.addAll(result);
		return table;
	}

	public int getFirstBlockFromModifyRecord() {
		// TODO Auto-generated method stub
		return 0;
	}

	public long resetByBlock(int block) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	long[] checkDim(String field, Node node, Context ctx) {
		if (!isDim(field) || (parent != null)) {
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
		
		int blockCount = dataBlockCount;
		ColumnFilter filter = new ColumnFilter(field, 0, operator, value);
		LongArray intervals = new LongArray();
		ObjectReader segmentReader = getSegmentObjectReader();
		int index = -1;
		String []colNames = this.colNames;
		for (int i = 0; i < colNames.length; ++i) {
			if (field.equals(colNames[i])) {
				index = i;
				break;
			}
		}
		
		try {
			boolean flag = false;
			Object maxValue = null, minValue = null;
			int keyCount = getAllSortedColNamesLength();
			
			for (int i = 0; i < blockCount; ++i) {
				int recordCount = segmentReader.readInt32();
				long pos = segmentReader.readLong40();
				if (flag) {
					intervals.add(pos - 1);
				}
				flag = false;
				for (int k = 0; k < keyCount; ++k) {
					if (k == index) {
						minValue = segmentReader.readObject();
						maxValue = segmentReader.readObject();
					} else {
						segmentReader.skipObject();
						segmentReader.skipObject();
					}
				}
				if (filter.match(minValue, maxValue) && recordCount != 1) {
					intervals.add(pos);
					flag = true;
				}
			}
			if (flag) {
				intervals.add(getGroupTable().fileSize);
			}
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		}
		if (intervals.size() == 0) {
			return null;
		}
		return intervals.toArray();
	}

	// @m 以归并方式追加，暂不支持有附表的情况
	public void append(ICursor cursor, String opt) throws IOException {
		if (isSorted && opt != null) {
			if (opt.indexOf('a') != -1) {
				RowTableMetaData ctmd = (RowTableMetaData)getSupplementTable(true);
				ctmd.mergeAppend(cursor, opt);
			} else if (opt.indexOf('m') != -1) {
				mergeAppend(cursor, opt);
			} else {
				append(cursor);
				if (opt.indexOf('i') != -1) {
					appendCache();
				}
			}
		} else {
			append(cursor);
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
		
		String []columns = this.colNames;
		int colCount = columns.length;
		if (colCount != ds.getFieldCount()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.dsNotMatch"));
		}
		
		for (int i = 0; i < colCount; i++) {
			if (!ds.getFieldName(i).equals(columns[i])) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.dsNotMatch"));
			}
		}
				
		// 归并读的数据先保存到临时文件
		RowGroupTable groupTable = (RowGroupTable)getGroupTable();
		File srcFile = groupTable.getFile();
		File tmpFile = File.createTempFile("tmpdata", "", srcFile.getParentFile());
		try {
			Context ctx = new Context();
			String colNames[] = this.colNames.clone();
			for (int i = 0; i < colNames.length; i++) {
				if (isDim[i]) {
					colNames[i] = "#" + colNames[i];
				}
			}
			RowGroupTable tmpGroupTable = new RowGroupTable(tmpFile, colNames, groupTable.getDistribute(), null, ctx);
			tmpGroupTable.writePswHash = groupTable.writePswHash;
			tmpGroupTable.readPswHash = groupTable.readPswHash;
			
			TableMetaData baseTable = tmpGroupTable.getBaseTable();
			if (segmentCol != null) {
				baseTable.setSegmentCol(segmentCol, segmentSerialLen);
			}
			
			int dcount = sortedColNames.length;
			Expression []mergeExps = new Expression[dcount];
			for (int i = 0; i < dcount; ++i) {
				mergeExps[i] = new Expression(sortedColNames[i]);
			}
			
			// 做归并
			RowCursor srcCursor = new RowCursor(this);
			ICursor []cursors = new ICursor[]{srcCursor, cursor};
			MergesCursor mergeCursor = new MergesCursor(cursors, mergeExps, ctx);
			baseTable.append(mergeCursor);
			baseTable.close();
			
			// 关闭并删除组表文件，把临时文件重命名为组表文件名
			groupTable.raf.close();
			groupTable.file.delete();
			tmpFile.renameTo(groupTable.file);
			
			// 重新打开组表
			groupTable.reopen();
		} finally {
			tmpFile.delete();
		}	
	}
	
	//写入缓存的数据
	public void appendCache() throws IOException {
		if (appendCache == null) return;
		
		ICursor cursor = new MemoryCursor(appendCache);
		// 准备写数据
		prepareAppend();
		
		if (parent != null) {
			parent.appendCache();
			appendAttached(cursor);
		} else if (sortedColNames == null) {
			appendNormal(cursor);
		} else if (groupTable.baseTable.getSegmentCol() == null) {
			appendSorted(cursor);
		} else {
			appendSegment(cursor);
		}
		
		// 结束写数据，保存到文件
		finishAppend();
		appendCache = null;
	}

	public ICursor cursor(String[] fields, Expression filter, String[] fkNames, Sequence[] codes, 
			String []opts, Context ctx, int pathSeq, int pathCount, int pathCount2) {
		getGroupTable().checkReadable();
		//行存不支持多路
		MessageManager mm = EngineMessage.get();
		throw new RQException("cursor" + mm.getMessage("dw.needMCursor"));
	}

	public ICursor cursor(String[] fields, Expression exp, String[] fkNames, Sequence[] codes, 
			String []opts, ICursor cs, int seg, Object [][]endValues, Context ctx) {
		getGroupTable().checkReadable();
		//行存不支持多路
		MessageManager mm = EngineMessage.get();
		throw new RQException("cursor" + mm.getMessage("dw.needMCursor"));
	}

	public ICursor cursor(Expression[] exps, String[] fields, Expression filter, String[] fkNames, 
			Sequence[] codes,String []opts, int pathCount, Context ctx) {
		if (fkNames != null || codes != null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("cursor" + mm.getMessage("dw.needMCursor"));
		}
		
		if (pathCount < 2) {
			return cursor(exps, fields, filter, fkNames, codes, opts, ctx);
		}
		
		TableMetaData tmd = getSupplementTable(false);
		int blockCount = getDataBlockCount();
		if (blockCount == 0) {
			if (tmd == null) {
				return new MemoryCursor(null);
			} else {
				return tmd.cursor(exps, fields, filter, fkNames, codes,  opts, pathCount, ctx);
			}
		}
		
		ICursor []cursors;

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
			
			RowCursor cursor = new RowCursor(this, null, filter, exps, fields, ctx);
			
			cursor.setSegment(start, end);

			cursors[i] = cursor;
			start = end;
		}
		
		MultipathCursors mcs = new MultipathCursors(cursors, ctx);
		if (tmd == null) {
			return mcs;
		}
		
		String []sortFields = ((IDWCursor)cursors[0]).getSortFields();
		if (sortFields != null) {
			ICursor cs2 = tmd.cursor(exps, fields, filter, fkNames, codes,  opts, mcs, null, ctx);
			return merge(mcs, (MultipathCursors)cs2, sortFields);
		} else {
			ICursor cs2 = tmd.cursor(exps, fields, filter, fkNames, codes,  opts, pathCount, ctx);
			return conj(mcs, cs2);
		}
	
	}

	public ICursor cursor(Expression[] exps, String[] fields, Expression filter, String[] fkNames, 
			Sequence[] codes,String []opts, int segSeq, int segCount, Context ctx) {

		getGroupTable().checkReadable();
		
		if (filter != null) {
			// 分段并行读取时需要复制表达式，同一个表达式不支持并行运算
			filter = filter.newExpression(ctx);
		}
		
		RowCursor cursor = new RowCursor(this, null, filter, exps, fields, ctx);
		
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

	public ICursor cursor(Expression[] exps, String[] fields, Expression filter, String[] fkNames, 
			Sequence[] codes,String []opts, MultipathCursors mcs, String opt, Context ctx) {
		//行存不支持多路
		MessageManager mm = EngineMessage.get();
		throw new RQException("cursor" + mm.getMessage("dw.needMCursor"));
	}

	public ICursor cursor(Expression[] exps, String[] fields, Expression filter, String[] fkNames, 
			Sequence[] codes,String []opts, int pathSeq, int pathCount, int pathCount2, Context ctx) {
		//行存不支持多路
		MessageManager mm = EngineMessage.get();
		throw new RQException("cursor" + mm.getMessage("dw.needMCursor"));
	}

	public ICursor cursor(Expression[] exps, String[] fields, Expression filter, String[] fkNames, 
			Sequence[] codes,String []opts, ICursor cs, int seg, Object[][] endValues, Context ctx) {
		//行存不支持多路
		MessageManager mm = EngineMessage.get();
		throw new RQException("cursor" + mm.getMessage("dw.needMCursor"));
	}

	public void addColumn(String colName, Expression exp, Context ctx) {
		//行存不支持修改列
		MessageManager mm = EngineMessage.get();
		throw new RQException("cursor" + mm.getMessage("dw.needMCursor"));
	}

	public void deleteColumn(String colName) {
		// TODO Auto-generated method stub
		
	}
}