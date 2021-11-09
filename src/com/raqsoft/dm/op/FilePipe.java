package com.raqsoft.dm.op;

import java.io.IOException;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.BFileWriter;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.DataStruct;
import com.raqsoft.dm.FileObject;
import com.raqsoft.dm.IResource;
import com.raqsoft.dm.Sequence;
import com.raqsoft.resources.EngineMessage;

/**
 * 文件管道，用于把接收到的数据保存到文件中
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
		if (writer == null) {
			if (ctx != null) {
				ctx.addResource(this);
			}
			
			writer = new BFileWriter(fo, "a");
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
			writer.write(seq);
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
			writer.close();
			writer = null;
		}
	}
}