package com.scudata.dw;

import java.io.IOException;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.array.LongArray;
import com.scudata.dm.ObjectReader;
import com.scudata.dm.ObjectWriter;
import com.scudata.dm.Sequence;
import com.scudata.resources.EngineMessage;

// 字段元数据
public class ColumnMetaData {
	protected ComTable groupTable;
	private String colName; // 列名，以#开头表示维，名字中把#去掉
	private boolean isDim; // 是否维字段的一部分，即排序字段
	private boolean isKey; // 是否是主键的一部分
	
	// 已废弃
	private int serialBytesLen = 0; // 如果大于0则为排号键
	
	private BlockLink dataBlockLink; // 列块区块链
	private BlockLink segmentBlockLink; // 分段信息区块链，依次记录每个列块的物理位置，如果是维字段再记着最小值和最大值

	private transient BlockLinkWriter colWriter;
	private transient BlockLinkWriter segmentWriter;
	private transient ObjectWriter objectWriter;
	
	private Sequence dict;//字典版本4增加
	private Object dictArray;//字典的数组格式
	private boolean hasMaxMinValues;//版本4增加
	private int dataType = DataBlockType.EMPTY;//列数据类型 版本5增加
	
	public ColumnMetaData() {	
	}
	
	public ColumnMetaData(ColPhyTable table) {
		groupTable = table.groupTable;
		dataBlockLink = new BlockLink(groupTable);
		segmentBlockLink = new BlockLink(groupTable);
		dict = new Sequence();
	}
	
	public ColumnMetaData(ColPhyTable table, ColumnMetaData src) {
		this(table);
		colName = src.colName;
		isDim = src.isDim;
		isKey = src.isKey;
		hasMaxMinValues = src.hasMaxMinValues;
		serialBytesLen = src.serialBytesLen;
	}
	
	public ColumnMetaData(ColumnMetaData src) {
		groupTable = src.groupTable;
		dataBlockLink = src.dataBlockLink;
		segmentBlockLink = src.segmentBlockLink;
		colName = src.colName;
		isDim = src.isDim;
		isKey = src.isKey;
		serialBytesLen = src.serialBytesLen;
	}
	
	/**
	 * 产生列对象
	 * @param table 所属的表
	 * @param name 列名
	 * @param isDim 是否维字段的一部分，即排序字段
	 * @param isKey 是否是主键的一部分
	 */
	public ColumnMetaData(ColPhyTable table, String name, boolean isDim, boolean isKey) {
		this(table);
		
		this.colName = name;
		this.isDim = isDim;
		this.isKey = isKey;
		hasMaxMinValues = true;
	}
	
	public ColumnMetaData(ColPhyTable table, String name, int serialBytesLen) throws IOException {
		this(table);
		if (name.startsWith("#")) {
			colName = name.substring(1);
			isDim = true;
		} else {
			colName = name;
			isDim = false;
		}
		
		this.serialBytesLen = serialBytesLen;
		hasMaxMinValues = true;
	}
	
	public boolean isSerialBytes() {
		return serialBytesLen > 0;
	}
	
	public int getSerialBytesLen() {
		return serialBytesLen;
	}
	
	public String getColName() {
		return colName;
	}
	
	public void setColName(String colName) {
		this.colName = colName;
	}

	/**
	 * 返回字段是否是维的一部分（即排序字段）
	 * @return
	 */
	public boolean isDim() {
		return isDim;
	}
	
	/**
	 * 返回字段是否是主键的一部分
	 * @return
	 */
	public boolean isKey() {
		return isKey;
	}
	
	void applyDataFirstBlock() throws IOException {
		if (dataBlockLink.isEmpty()) {
			dataBlockLink.setFirstBlockPos(groupTable.applyNewBlock());
		}
	}
	
	void applySegmentFirstBlock() throws IOException {
		if (dataBlockLink.isEmpty()) {
			segmentBlockLink.setFirstBlockPos(groupTable.applyNewBlock());
		}
	}
	
	public boolean isColumn(String name) {
		return colName.equals(name);
	}
	
	public void readExternal(BufferReader reader, byte version) throws IOException {
		colName = reader.readUTF();
		isDim = reader.readBoolean();
		serialBytesLen = reader.readInt();
		dataBlockLink.readExternal(reader);
		segmentBlockLink.readExternal(reader);
		
		if (version > 0) {
			isKey = reader.readBoolean();
		} else {
			isKey = isDim;
		}
		
		hasMaxMinValues = isDim;
		if (version > 3) {
			dict =  (Sequence) reader.readObject();
			if (dict == null) {
				dict = new Sequence();
			}
			hasMaxMinValues = true;
		}
		if (version > 4) {
			reader.readInt();//reserve
			dataType = reader.readInt();
			initDictArray();
		} else {
			dataType = DataBlockType.EMPTY;
		}
	}
	
	public void writeExternal(BufferWriter writer) throws IOException {
		writer.writeUTF(colName);
		writer.writeBoolean(isDim);
		writer.writeInt(serialBytesLen);
		dataBlockLink.writeExternal(writer);
		segmentBlockLink.writeExternal(writer);
		
		writer.writeBoolean(isKey); // 版本1增加
		
		// 版本4增加
		Sequence dict = this.dict;
		if (dict != null && dict.length() == 0) {
			dict = null;
		}
		writer.flush();
		writer.writeObject(dict);
		writer.flush();
		
		// 版本5增加
		writer.writeInt(0);
		writer.writeInt(dataType);
	}
	
	public void prepareWrite() throws IOException {
		colWriter = new BlockLinkWriter(dataBlockLink, true);
		segmentWriter = new BlockLinkWriter(segmentBlockLink, true);
		objectWriter = new ObjectWriter(segmentWriter, groupTable.getBlockSize() - ComTable.POS_SIZE);
	}
	
	public void finishWrite() throws IOException {
		colWriter.finishWrite();
		colWriter = null;
		
		objectWriter.flush();
		segmentWriter.finishWrite();
		segmentWriter = null;
		objectWriter = null;
	}
	
	// 追加一个列块，同时需要修改分段信息区块链
	public void appendColBlock(byte []bytes) throws IOException {
		long pos = colWriter.writeDataBlock(bytes);
		objectWriter.writeLong40(pos);
	}
	
	// 追加一个维列块，同时需要修改分段信息区块链
	public void appendColBlock(byte []bytes, Object minValue, Object maxValue, Object startValue) throws IOException {
		long pos = colWriter.writeDataBlock(bytes);
		objectWriter.writeLong40(pos);
		objectWriter.writeObject(minValue);
		objectWriter.writeObject(maxValue);
		objectWriter.writeObject(startValue);
	}
	
	public void copyColBlock(BlockLinkReader colReader, ObjectReader segmentReader) throws IOException {
		long pos = colWriter.copyDataBlock(colReader);
		
		segmentReader.readLong40();
		objectWriter.writeLong40(pos);
		if (hasMaxMinValues()) {
			objectWriter.writeObject(segmentReader.readObject());
			objectWriter.writeObject(segmentReader.readObject());
			objectWriter.writeObject(segmentReader.readObject());
		}
	}
	
	public BlockLinkReader getColReader(boolean isLoadFirstBlock) {
		BlockLinkReader reader = new BlockLinkReader(dataBlockLink, serialBytesLen);
		reader.setDecompressBufferSize(4096);
		
		if (isLoadFirstBlock) {
			try {
				reader.loadFirstBlock();
			} catch (IOException e) {
				throw new RQException(e.getMessage(), e);
			}
		}
		
		reader.setDict(dict);
		return reader;
	}
	
	public ObjectReader getSegmentReader() {
		BlockLinkReader segmentReader = new BlockLinkReader(segmentBlockLink);
		try {
			segmentReader.loadFirstBlock();
			return new ObjectReader(segmentReader, groupTable.getBlockSize() - ComTable.POS_SIZE);
		} catch (IOException e) {
			segmentReader.close();
			throw new RQException(e.getMessage(), e);
		}
	}
	
	/**
	 * 取列块数据输出
	 * @return
	 */
	public BufferWriter getColDataBufferWriter() {
		return new BufferWriter(groupTable.getStructManager());
	}
	
	/**
	 * 把当前column里的blockLink信息填充到info数组里
	 * @param info
	 */
	public void getBlockLinkInfo(LongArray info) {
		info.add(segmentBlockLink.firstBlockPos);
		info.add(segmentBlockLink.lastBlockPos);
		info.add(segmentBlockLink.freeIndex);
		info.add(segmentBlockLink.blockCount);
		info.add(dataBlockLink.firstBlockPos);
		info.add(dataBlockLink.lastBlockPos);
		info.add(dataBlockLink.freeIndex);
		info.add(dataBlockLink.blockCount);
	}
	
	public BlockLink getSegmentBlockLink() {
		return segmentBlockLink;
	}
	
	public BlockLink getDataBlockLink() {
		return dataBlockLink;
	}

	public Sequence getDict() {
		return dict;
	}

	public void setDict(Sequence dict) {
		this.dict = dict;
	}

	public boolean hasMaxMinValues() {
		return hasMaxMinValues;
	}
	
	public int getDataType() {
		return dataType;
	}

	public void setDataType(int dataType) {
		this.dataType = dataType;
	}

	public String getDataLen() {
		return DataBlockType.getTypeLen(dataType);
	}
	
	/**
	 * 根据新的类型做调整
	 * @param dataType
	 * @param checkDataPure 检查类型是否纯
	 */
	public void adjustDataType(int newType, boolean checkDataPure) {
		if (checkDataPure) {
			if (!checkDataPure(newType)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("pdm.arrayTypeError", 
						DataBlockType.getTypeName(dataType), DataBlockType.getTypeName(newType)));
			}
		}
		
		int curType = dataType;
		if (curType == newType || newType == DataBlockType.NULL) {
			return;
		}
		
		if (curType == DataBlockType.STRING && newType == DataBlockType.STRING_ASSIC) {
			return;
		}
		if (newType == DataBlockType.STRING && curType == DataBlockType.STRING_ASSIC) {
			return;
		}
		
		switch (curType) {
		case DataBlockType.EMPTY:
			dataType = newType;
			break;
		case DataBlockType.OBJECT:
			break;
		case DataBlockType.RECORD:
		case DataBlockType.DATE:
		case DataBlockType.DECIMAL:
		case DataBlockType.STRING:
			dataType = DataBlockType.OBJECT;//降级为对象类型
			break;
		case DataBlockType.SEQUENCE:
			if (newType != DataBlockType.TABLE)
				dataType = DataBlockType.OBJECT;
			break;
		case DataBlockType.TABLE:
			if (newType == DataBlockType.SEQUENCE)
				dataType = DataBlockType.SEQUENCE;//降级为SEQUENCE
			else
				dataType = DataBlockType.OBJECT;
			break;
		case DataBlockType.INT:
		case DataBlockType.INT8:
		case DataBlockType.INT16:
		case DataBlockType.INT32:
			if ((newType & 0xF0) == DataBlockType.INT) {
				dataType = DataBlockType.INT;
			} else {
				dataType = DataBlockType.OBJECT;
			}
			break;
		case DataBlockType.LONG:
		case DataBlockType.LONG8:
		case DataBlockType.LONG16:
		case DataBlockType.LONG32:
		case DataBlockType.LONG64:
			if ((newType & 0xF0) == DataBlockType.LONG) {
				dataType = DataBlockType.LONG;
			} else {
				dataType = DataBlockType.OBJECT;
			}
			break;
		case DataBlockType.DOUBLE:
			if (newType == DataBlockType.DOUBLE64) {
				dataType = DataBlockType.DOUBLE;
			} else {
				dataType = DataBlockType.OBJECT;
			}
			break;
		case DataBlockType.DOUBLE64:
			if (newType == DataBlockType.DOUBLE) {
				dataType = DataBlockType.DOUBLE;
			} else {
				dataType = DataBlockType.OBJECT;
			}
			break;
		default:
			dataType = DataBlockType.OBJECT;
		}
	}

	/**
	 * 检查更新的数据类型是否纯
	 * @param newType
	 * @return true: 数据纯, false:数据不纯
	 */
	private boolean checkDataPure(int newType) {
		int curType = dataType;
		if (curType == newType || newType == DataBlockType.NULL) {
			return true;
		}
		
		switch (curType) {
		case DataBlockType.EMPTY:
			break;
		case DataBlockType.OBJECT:
			break;
		case DataBlockType.RECORD:
		case DataBlockType.DATE:
		case DataBlockType.DECIMAL:
		case DataBlockType.STRING:
			return false;
		case DataBlockType.SEQUENCE:
			if (newType != DataBlockType.TABLE)
				return false;
		case DataBlockType.TABLE:
			if (newType == DataBlockType.SEQUENCE)
				return false;
			break;
		case DataBlockType.INT:
		case DataBlockType.INT8:
		case DataBlockType.INT16:
		case DataBlockType.INT32:
			if ((newType & 0xF0) == DataBlockType.INT) {
				break;
			} else {
				return false;
			}
		case DataBlockType.LONG:
		case DataBlockType.LONG8:
		case DataBlockType.LONG16:
		case DataBlockType.LONG32:
		case DataBlockType.LONG64:
			if ((newType & 0xF0) == DataBlockType.LONG) {
				break;
			} else {
				return false;
			}
		case DataBlockType.DOUBLE:
			if (newType == DataBlockType.DOUBLE64) {
				break;
			} else {
				return false;
			}
		case DataBlockType.DOUBLE64:
			if (newType == DataBlockType.DOUBLE) {
				break;
			} else {
				return false;
			}
		default:
			return false;
		}
		return true;
	}
	
	public Object getDictArray() {
		return dictArray;
	}
	
	public void initDictArray() {
		if (dict == null || dict.length() == 0) return;
		dictArray = DataBlockType.dictToArray(dict, this.dataType);
	}
}