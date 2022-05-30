package com.scudata.dw;

import java.io.IOException;

import com.scudata.common.RQException;
import com.scudata.dm.LongArray;
import com.scudata.dm.ObjectReader;
import com.scudata.dm.ObjectWriter;

// 字段元数据
public class ColumnMetaData {
	protected GroupTable groupTable;
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
	
	public ColumnMetaData() {	
	}
	
	public ColumnMetaData(ColumnTableMetaData table) {
		groupTable = table.groupTable;
		dataBlockLink = new BlockLink(groupTable);
		segmentBlockLink = new BlockLink(groupTable);
	}
	
	public ColumnMetaData(ColumnTableMetaData table, ColumnMetaData src) {
		this(table);
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
	public ColumnMetaData(ColumnTableMetaData table, String name, boolean isDim, boolean isKey) {
		this(table);
		
		this.colName = name;
		this.isDim = isDim;
		this.isKey = isKey;
	}
	
	public ColumnMetaData(ColumnTableMetaData table, String name, int serialBytesLen) throws IOException {
		this(table);
		if (name.startsWith("#")) {
			colName = name.substring(1);
			isDim = true;
		} else {
			colName = name;
			isDim = false;
		}
		
		this.serialBytesLen = serialBytesLen;
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
	}
	
	public void writeExternal(BufferWriter writer) throws IOException {
		writer.writeUTF(colName);
		writer.writeBoolean(isDim);
		writer.writeInt(serialBytesLen);
		dataBlockLink.writeExternal(writer);
		segmentBlockLink.writeExternal(writer);
		
		writer.writeBoolean(isKey); // 版本1增加
	}
	
	public void prepareWrite() throws IOException {
		colWriter = new BlockLinkWriter(dataBlockLink, true);
		segmentWriter = new BlockLinkWriter(segmentBlockLink, true);
		objectWriter = new ObjectWriter(segmentWriter, groupTable.getBlockSize() - GroupTable.POS_SIZE);
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
		if (isDim()) {
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
		
		return reader;
	}
	
	public ObjectReader getSegmentReader() {
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
}