package com.scudata.dm.op;

import com.scudata.dm.Context;
import com.scudata.dm.FileObject;
import com.scudata.dm.Sequence;

/**
 * 用于保持管道当前数据作为结果集
 * @author RunQian
 *
 */
public class FetchResult implements IResult {
	private Sequence result;
	private FilePipe filePipe;
	
	public FetchResult() {
		result = new Sequence();
	}
	
	public FetchResult(FileObject fo) {
		filePipe = new FilePipe(fo);
	}
	
	/**
	 * 处理游标或管道当前推送的数据
	 * @param seq 数据
	 * @param ctx 计算上下文
	 * @return
	 */
	public void push(Sequence table, Context ctx) {
		if (result != null) {
			result.addAll(table);
		} else {
			filePipe.push(table, ctx);
		}
	}
	
	/**
	 * 数据推送结束时调用
	 * @param ctx 计算上下文
	 */
	public void finish(Context ctx) {
		// 多路管道时对应同一个FilePipe，会多次调用
		//if (filePipe != null) {
		//	filePipe.close();
		//}
	}
	
	 /**
	  * 数据推送结束，取最终的计算结果
	  * @return 结果
	  */
	public Object result() {
		if (filePipe != null) {
			filePipe.close();
		}
		
		Sequence seq = result;
		if (seq == null || seq.length() == 0) {
			return null;
		}
		
		result = null;
		return seq;
	}
	
	/**
	 * 集群管道最后需要把各节点机的返回结果再合并一下
	 * @param results 每个节点机的计算结果
	 * @return 合并后的结果
	 */
	public Object combineResult(Object []results) {
		int count = results.length;
		Sequence result = new Sequence();
		for (int i = 0; i < count; ++i) {
			if (results[i] instanceof Sequence) {
				result.addAll((Sequence)results[i]);
			} else if (results[i] != null) {
				result.add(results[i]);
			}
		}

		return result;
	}
}