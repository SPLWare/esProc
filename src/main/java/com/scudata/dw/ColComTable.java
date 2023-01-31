package com.scudata.dw;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import com.scudata.array.LongArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Env;
import com.scudata.resources.EngineMessage;

/**
 * 列存组表类
 * @author runqian
 *
 */
public class ColComTable extends ComTable {
	/**
	 * 打开已经存在的组表
	 * @param file
	 * @param raf
	 * @param ctx
	 * @throws IOException
	 */
	public ColComTable(File file, RandomAccessFile raf, Context ctx) throws IOException {
		this.file = file;
		this.raf = raf;
		this.ctx = ctx;
		if (ctx != null) 
			ctx.addResource(this);
		readHeader();
	}

	/**
	 * 打开已经存在的组表
	 * @param file
	 * @param ctx
	 * @throws IOException
	 */
	public ColComTable(File file, Context ctx) throws IOException {
		this.file = file;
		this.raf = new RandomAccessFile(file, "rw");
		this.ctx = ctx;
		if (ctx != null) 
			ctx.addResource(this);
		readHeader();
	}
	
	/**
	 * 打开已经存在的组表,不检查出错日志，仅内部使用
	 * @param file
	 * @throws IOException
	 */
	public ColComTable(File file) throws IOException {
		this.file = file;
		this.raf = new RandomAccessFile(file, "rw");
		readHeader();
	}
			
	/**
	 * 创建组表
	 * @param file 表文件
	 * @param colNames 列名称
	 * @param distribute 分布表达式
	 * @param opt u：不压缩数据，p：按第一字段分段
	 * @param ctx 上下文
	 * @throws IOException
	 */
	public ColComTable(File file, String []colNames, String distribute, String opt, Context ctx) 
			throws IOException {
		file.delete();
		File parent = file.getParentFile();
		if (parent != null) {
			// 创建目录，否则如果目录不存在RandomAccessFile会抛异常
			parent.mkdirs();
		}
		
		this.file = file;
		this.raf = new RandomAccessFile(file, "rw");
		this.ctx = ctx;
		if (ctx != null) {
			ctx.addResource(this);
		}
		
		// 是否压缩
		if (opt != null && opt.indexOf('u') != -1) {
			setCompress(false);
		}
		
		setBlockSize(Env.getBlockSize());
		enlargeSize = blockSize * 16;
		headerBlockLink = new BlockLink(this);
		headerBlockLink.setFirstBlockPos(applyNewBlock());

		baseTable = new ColPhyTable(this, colNames);
		structManager = new StructManager();
		
		// 按第一字段分段
		if (opt != null && opt.indexOf('p') != -1) {
			baseTable.segmentCol = baseTable.getColName(0);
		}
		
		this.distribute = distribute;
		this.reserve[0] = 4;
		save();
	}
	
	/**
	 * 复制src的结构创建一个新组表文件
	 * @param file 新表的文件
	 * @param src 原组表
	 * @throws IOException
	 */
	public ColComTable(File file, ColComTable src) throws IOException {
		this.file = file;
		this.raf = new RandomAccessFile(file, "rw");
		this.ctx = src.ctx;
		if (ctx != null) {
			ctx.addResource(this);
		}
		
		System.arraycopy(src.reserve, 0, reserve, 0, reserve.length);
		blockSize = src.blockSize;
		enlargeSize = src.enlargeSize;
		
		headerBlockLink = new BlockLink(this);
		headerBlockLink.setFirstBlockPos(applyNewBlock());
		writePswHash = src.writePswHash;
		readPswHash = src.readPswHash;
		distribute = src.distribute;
		structManager = src.structManager;
		
		try {
			baseTable = new ColPhyTable(this, null, (ColPhyTable) src.baseTable);
		} catch (Exception e) {
			if (raf != null) {
				raf.close();
			}
		}
		save();
	}
	
	/**
	 * 重新打开组表，文件被其它对象修改
	 * @throws IOException
	 */
	protected void reopen() throws IOException {
		// 读写文件头时做同步以支持同时读写
		raf = new RandomAccessFile(file, "rw");
		Object syncObj = getSyncObject();
		synchronized(syncObj) {
			restoreTransaction();
			raf.seek(0);
			byte []bytes = new byte[32];
			raf.read(bytes);
			if (bytes[0] != 'r' || bytes[1] != 'q' || bytes[2] != 'd' || bytes[3] != 'w' || bytes[4] != 'g' || bytes[5] != 't') {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("license.fileFormatError"));
			}
			if (bytes[6] != 'c' && bytes[6] != 'C') {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("license.fileFormatError"));
			}
			
			BufferReader reader = new BufferReader(structManager, bytes, 7, 25);
			setBlockSize(reader.readInt32());
			headerBlockLink = new BlockLink(this);
			headerBlockLink.readExternal(reader);
			
			BlockLinkReader headerReader = new BlockLinkReader(headerBlockLink);
			bytes = headerReader.readBlocks();
			headerReader.close();
			reader = new BufferReader(structManager, bytes);
			reader.read(); // r
			reader.read(); // q
			reader.read(); // d
			reader.read(); // w
			reader.read(); // g
			reader.read(); // t
			reader.read(); // c
			
			blockSize = reader.readInt32();
			headerBlockLink.readExternal(reader);
			
			reader.read(reserve); // 保留位
			freePos = reader.readLong40();
			fileSize = reader.readLong40();
			
			if (reserve[0] > 0) {
				writePswHash = reader.readString();
				readPswHash = reader.readString();
				checkPassword(null);
				
				if (reserve[0] > 1) {
					distribute = reader.readString();
				}
			}
			
			int dsCount = reader.readInt();
			if (dsCount > 0) {
				ArrayList<DataStruct> dsList = new ArrayList<DataStruct>(dsCount);
				for (int i = 0; i < dsCount; ++i) {
					String []fieldNames = reader.readStrings();
					DataStruct ds = new DataStruct(fieldNames);
					dsList.add(ds);
				}
				
				structManager = new StructManager(dsList);
			} else {
				structManager = new StructManager();
			}
			
			//baseTable = new ColumnTableMetaData(this, null);
			baseTable.readExternal(reader);
		}
	}
	
	/**
	 * 读取文件头
	 * 修改读写时需要同步修改reopen函数
	 */
	protected void readHeader() throws IOException {
		// 读写文件头时做同步以支持同时读写
		Object syncObj = getSyncObject();
		synchronized(syncObj) {
			//restoreTransaction();
			raf.seek(0);
			byte []bytes = new byte[32];
			raf.read(bytes);
			if (bytes[0] != 'r' || bytes[1] != 'q' || bytes[2] != 'd' || bytes[3] != 'w' || bytes[4] != 'g' || bytes[5] != 't') {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("license.fileFormatError"));
			}
			if (bytes[6] != 'c' && bytes[6] != 'C') {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("license.fileFormatError"));
			}
			
			BufferReader reader = new BufferReader(structManager, bytes, 7, 25);
			setBlockSize(reader.readInt32());
			headerBlockLink = new BlockLink(this);
			headerBlockLink.readExternal(reader);
			
			BlockLinkReader headerReader = new BlockLinkReader(headerBlockLink);
			bytes = headerReader.readBlocks();
			headerReader.close();
			reader = new BufferReader(structManager, bytes);
			reader.read(); // r
			reader.read(); // q
			reader.read(); // d
			reader.read(); // w
			reader.read(); // g
			reader.read(); // t
			reader.read(); // c
			
			blockSize = reader.readInt32();
			headerBlockLink.readExternal(reader);
			
			reader.read(reserve); // 保留位
			freePos = reader.readLong40();
			fileSize = reader.readLong40();
			
			if (reserve[0] > 0) {
				writePswHash = reader.readString();
				readPswHash = reader.readString();
				checkPassword(null);
				
				if (reserve[0] > 1) {
					distribute = reader.readString();
				}
			}
			
			int dsCount = reader.readInt();
			if (dsCount > 0) {
				ArrayList<DataStruct> dsList = new ArrayList<DataStruct>(dsCount);
				for (int i = 0; i < dsCount; ++i) {
					String []fieldNames = reader.readStrings();
					DataStruct ds = new DataStruct(fieldNames);
					dsList.add(ds);
				}
				
				structManager = new StructManager(dsList);
			} else {
				structManager = new StructManager();
			}
			
			baseTable = new ColPhyTable(this);
			baseTable.readExternal(reader);
		}
	}
	
	/**
	 * 写文件头
	 */
	protected void writeHeader() throws IOException {
		//不能写低版本的文件
		if (reserve[0] < 4) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("dw.oldVersion2"));
		}
		
		// 读写文件头时做同步以支持同时读写
		Object syncObj = getSyncObject();
		synchronized(syncObj) {
			beginTransaction(null);
			BufferWriter writer = new BufferWriter(structManager);
			writer.write('r');
			writer.write('q');
			writer.write('d');
			writer.write('w');
			writer.write('g');
			writer.write('t');
			writer.write('C');
			
			writer.writeInt32(blockSize);
			headerBlockLink.writeExternal(writer);
			
			reserve[0] = 4; // 1增加密码，2增加分布函数，3增加预分组，4列式存储（数据类型，字典等）
			writer.write(reserve); // 保留位
			
			writer.writeLong40(freePos);
			writer.writeLong40(fileSize);
			
			// 下面两个成员版本1增加的
			writer.writeString(writePswHash);
			writer.writeString(readPswHash);
			
			writer.writeString(distribute); // 版本2增加
			
			ArrayList<DataStruct> dsList = structManager.getStructList();
			if (dsList != null) {
				writer.writeInt(dsList.size());
				for (DataStruct ds : dsList) {
					String []fieldNames = ds.getFieldNames();
					writer.writeStrings(fieldNames);
				}
			} else {
				writer.writeInt(0);
			}
			
			baseTable.writeExternal(writer);
			
			BlockLinkWriter headerWriter = new BlockLinkWriter(headerBlockLink, false);
			headerWriter.rewriteBlocks(writer.finish());
			headerWriter.close();
			//headerWriter.finishWrite();
			
			// 重写headerBlockLink
			writer.write('r');
			writer.write('q');
			writer.write('d');
			writer.write('w');
			writer.write('g');
			writer.write('t');
			writer.write('C');
			
			writer.writeInt32(blockSize);
			headerBlockLink.writeExternal(writer);
			raf.seek(0);
			raf.write(writer.finish());
			raf.getChannel().force(true);
			commitTransaction(0);
		}
	}
	
	/**
	 * 获得区块链信息,不包含组表header和补区
	 */
	public long[] getBlockLinkInfo() {
		LongArray info = new LongArray(1024);
		ColPhyTable baseTable = (ColPhyTable) this.baseTable;
		//segment block link
		BlockLink segmentBlockLink = baseTable.segmentBlockLink;
		info.add(segmentBlockLink.firstBlockPos);
		info.add(segmentBlockLink.lastBlockPos);
		info.add(segmentBlockLink.freeIndex);
		info.add(segmentBlockLink.blockCount);
		
		//columns block link
		ColumnMetaData []columns = baseTable.getColumns();
		for (ColumnMetaData col : columns) {
			col.getBlockLinkInfo(info);
		}
		
		for (PhyTable table : baseTable.tableList) {
			//segment block link
			segmentBlockLink = table.segmentBlockLink;
			info.add(segmentBlockLink.firstBlockPos);
			info.add(segmentBlockLink.lastBlockPos);
			info.add(segmentBlockLink.freeIndex);
			info.add(segmentBlockLink.blockCount);
			//导列 block link
			((ColPhyTable) table).getGuideColumn().getBlockLinkInfo(info);
			//columns block link
			columns = ((ColPhyTable) table).getColumns();
			for (ColumnMetaData col : columns) {
				col.getBlockLinkInfo(info);
			}
		}
		return info.getDatas();
	}
	
	public boolean isPureFormat() {
		return reserve[0] > 3;
	}
}