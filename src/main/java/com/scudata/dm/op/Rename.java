package com.scudata.dm.op;

import com.scudata.dm.BaseRecord;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;

/**
 * 游标或管道附加的对字段进行重命名运算处理类
 * op.rename(F:F',…) op是游标或管道
 * @author RunQian
 *
 */
public class Rename extends Operation {
	private String []srcFields; // 源字段名数组
	private String []newFields; // 新字段名数组
	private DataStruct prevDs; // 上一条处理的记录的数据结构

	
	public Rename(Function function, String []srcFields, String []newFields) {
		super(function);
		this.srcFields = srcFields;
		this.newFields = newFields;
	}
	
	public Rename(String []srcFields, String []newFields) {
		this(null, srcFields, newFields);
	}
	
	/**
	 * 复制运算用于多线程计算，因为表达式不能多线程计算
	 * @param ctx 计算上下文
	 * @return Operation
	 */
	public Operation duplicate(Context ctx) {
		return new Rename(function, srcFields, newFields);
	}

	/**
	 * 处理游标或管道当前推送的数据
	 * @param seq 数据
	 * @param ctx 计算上下文
	 * @return
	 */
	public Sequence process(Sequence seq, Context ctx) {
		if (seq != null && seq.length() > 0) {
			Object obj = seq.getMem(1);
			if (obj instanceof BaseRecord) {
				DataStruct ds = ((BaseRecord)obj).dataStruct();
				if (prevDs != ds) {
					ds.rename(srcFields, newFields);
					prevDs = ds;
				}
			}
		}
		
		return seq;
	}
}
