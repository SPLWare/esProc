package com.raqsoft.dm.cursor;

import java.io.IOException;

import com.raqsoft.common.RQException;
import com.raqsoft.dm.BFileReader;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.DataStruct;
import com.raqsoft.dm.Env;
import com.raqsoft.dm.FileObject;
import com.raqsoft.dm.Sequence;

/**
 * 集文件游标
 * @author
 *
 */
public class BFileCursor extends ICursor {
	private FileObject fileObject; // 文件对象
	private String []fields; // 选出字段
	
	// 用于多线程运算
	private int segSeq; // 分段号
	private int segCount; // 总段数
	
	private String opt; // 选项
	private int fileBufSize = Env.FILE_BUFSIZE; // 读物理文件时的缓冲区大小
	private BFileReader reader; // 集文件读取器
	private boolean isDeleteFile; // 游标关闭后删除源文件，用于计算过程中产生的临时集文件

	// 对分段做换算，换算成物理文件的起始位置和结束位置，会做掐头去尾处理
	private long startPos = -1;
	private long endPos;
	
	/**
	 * 创建集文件游标
	 * @param fileObject 文件对象
	 * @param fields 选出字段
	 * @param opt 选项
	 * @param ctx 计算上下文
	 */
	public BFileCursor(FileObject fileObject, String []fields, String opt, Context ctx) {
		this(fileObject, fields, 1, 1, opt, ctx);
	}

	/**
	 * 创建集文件游标
	 * @param fileObject 文件对象
	 * @param fields 选出字段
	 * @param segSeq 当前游标要读的段，从1开始计数
	 * @param segCount 分段数
	 * @param opt 选项
	 * @param ctx 计算上下文
	 */
	public BFileCursor(FileObject fileObject, String []fields, 
			int segSeq, int segCount, String opt, Context ctx) {
		this.fileObject = fileObject;
		this.fields = fields;
		this.segSeq = segSeq;
		this.segCount = segCount;
		this.opt = opt;
		
		this.ctx = ctx;
		reader = new BFileReader(fileObject, fields, segSeq, segCount, opt);
		if (opt != null) {
			if (opt.indexOf('x') != -1) {
				if (ctx != null) ctx.addResource(this);
				isDeleteFile = true;
			}
		}
	}

	/**
	 * 设置读文件的起始位置和结束位置
	 * @param startPos 起始位置，会做掐头去尾处理
	 * @param endPos 结束位置
	 */
	public void setPosRange(long startPos, long endPos) {
		this.startPos = startPos;
		this.endPos = endPos;
	}
	
	/**
	 * 设置读文件缓冲区大小
	 * @param size
	 */
	public void setFileBufferSize(int size) {
		this.fileBufSize = size;
	}

	/**
	 * 读取指定条数的数据返回
	 * @param n 数量
	 * @return Sequence
	 */
	protected Sequence get(int n) {
		if (n < 1 || reader == null) {
			return null;
		}
		
		try {
			if (!reader.isOpen()) {
				reader.open(fileBufSize);
				DataStruct ds = reader.getResultSetDataStruct();
				setDataStruct(ds);
				
				if (!isDeleteFile && ctx != null) {
					ctx.addResource(this);
				}
				
				if (startPos > 0) {
					reader.seek(startPos);
					reader.setEndPos(endPos);
				}
			}
			
			Sequence seq = reader.read(n);
			return seq;
		} catch (Exception e) {
			close();
			throw new RQException(e.getMessage(), e);
		}
	}

	/**
	 * 跳过指定条数的数据
	 * @param n 数量
	 * @return long 实际跳过的条数
	 */
	protected long skipOver(long n) {
		if (n < 1 || reader == null) {
			return 0;
		}
		
		try {
			if (!reader.isOpen()) {
				reader.open(fileBufSize);
				DataStruct ds = reader.getResultSetDataStruct();
				setDataStruct(ds);
				
				if (startPos > 0) {
					reader.seek(startPos);
					reader.setEndPos(endPos);
				}
			}
			
			long count = reader.skip(n);
			return count;
		} catch (Exception e) {
			close();
			throw new RQException(e.getMessage(), e);
		}
	}

	/**
	 * 关闭游标
	 */
	public synchronized void close() {
		super.close();
		
		if (fileObject != null) {
			if (reader != null) {
				if (ctx != null) ctx.removeResource(this);
				try {
					reader.close();
				} catch (IOException e) {
				}
				
				reader = null;
			}

			if (isDeleteFile) {
				fileObject.delete();
				fileObject = null;
			}
		}
	}

	protected void finalize() throws Throwable {
		close();
	}
	
	/**
	 * 重置游标
	 * @return 返回是否成功，true：游标可以从头重新取数，false：不可以从头重新取数
	 */
	public boolean reset() {
		close();
		
		if (fileObject != null) {
			reader = new BFileReader(fileObject, fields, segSeq, segCount, opt);
			return true;
		} else {
			return false;
		}
	}
}
