package com.scudata.dm.op;

import java.io.IOException;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.BFileWriter;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.FileObject;
import com.scudata.dm.IResource;
import com.scudata.dm.Sequence;
import com.scudata.resources.EngineMessage;

/**
 * 文件管道，用于把接收到的数据保存到可分段集文件中
 * @author RunQian
 *
 */
public class FilePipe implements IPipe, IResource {
	private FileObject fo;
	private BFileWriter writer;
	
	public FilePipe(FileObject fo) {
		fo.delete();
		this.fo = fo;
	}
	
	/**
	 * 往管道推送数据
	 * @param seq 数据
	 * @param ctx 计算上下文
	 */
	public synchronized void push(Sequence seq, Context ctx) {
		if (seq == null || seq.length() == 0) {
			return;
		}
		
		if (writer == null) {
			if (ctx != null) {
				ctx.addResource(this);
			}
			
			writer = new BFileWriter(fo, "az");
			DataStruct ds = seq.dataStruct();
			if (ds == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.needPurePmt"));
			}
			
			try {
				writer.prepareWrite(ds, false);
			} catch (IOException e) {
				throw new RQException(e);
			}
		}
		
		try {
			writer.writeBlock(seq);
		} catch (IOException e) {
			throw new RQException(e);
		}
	}
	
	/**
	 * 数据推送结束
	 * @param ctx
	 */
	public synchronized void finish(Context ctx) {
		close();
	}
	
	/**
	 * 关闭管道
	 * @param ctx
	 */
	public void close() {
		if (writer != null) {
			try {
				writer.writeHeader(false);
			} catch (IOException e) {
				throw new RQException(e);
			}
			
			writer.close();
			writer = null;
		}
	}
}