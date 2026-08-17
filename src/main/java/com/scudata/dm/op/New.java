package com.scudata.dm.op;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Sequence;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;

/**
 * 游标或管道的附加的产生序表计算处理类
 * cs.new(...)
 * @author RunQian
 *
 */
public class New extends Operation  {
	private Expression []newExps; // 字段表达式数组
	private String []names; // 字段名数组
	private String opt; // 选项
	private DataStruct newDs; // 结构集数据结构
	
	public New(Expression []newExps, String []names, String opt) {
		this(null, newExps, names, opt);
	}
	
	public New(Function function, Expression []newExps, String []names, String opt) {
		super(function);
		int colCount = newExps.length;
		if (names == null) names = new String[colCount];
		
		this.newExps = newExps;
		this.names = names;
		this.opt = opt;
	}
	
	/**
	 * 复制运算用于多线程计算，因为表达式不能多线程计算
	 * @param ctx 计算上下文
	 * @return Operation
	 */
	public Operation duplicate(Context ctx) {
		Expression []dupExps = dupExpressions(newExps, ctx);
		return new New(function, dupExps, names, opt);
	}

	/**
	 * 处理游标或管道当前推送的数据
	 * @param seq 数据
	 * @param ctx 计算上下文
	 * @return
	 */
	public Sequence process(Sequence seq, Context ctx) {
		if (newDs == null) {
			seq.getNewFieldNames(newExps, names, "new");
			newDs = new DataStruct(names);
			
			// 检查是否可以继承主键
			DataStruct ds = seq.dataStruct();
			if (ds != null && ds.getPrimary() != null) {
				String []keyNames = ds.getPrimary();
				int keyCount = keyNames.length;
				String []newKeyNames = new String[keyCount];
				int fcount = newExps.length;
				
				Next:
				for (int i = 0; i < keyCount; ++i) {
					String keyName = keyNames[i];
					for (int f = 0; f < fcount; ++f) {
						String fname = newExps[f].getFieldName(ds);
						if (keyName.equals(fname)) {
							newKeyNames[i] = names[f];
							continue Next;
						}
					}
					
					newKeyNames = null;
					break;
				}
				
				if (newKeyNames != null) {
					newDs.setPrimary(newKeyNames);
				}
			}
		}
		
		return seq.newTable(newDs, newExps, opt, ctx);
	}

	/**
	 * 返回运算结果的数据结构，如果不是返回序表则返回空
	 * @param srcDs 运算前的数据结构
	 * @return
	 */
	public DataStruct getResultDataStruct(DataStruct srcDs) {
		if (newDs != null) {
			return newDs;
		} else if (srcDs == null) {
			return new DataStruct(names);
		} else {
			int colCount = newExps.length;
			for (int i = 0; i < colCount; ++i) {
				if (names[i] == null || names[i].length() == 0) {
					if (newExps[i] == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("new" + mm.getMessage("function.invalidParam"));
					}

					names[i] = newExps[i].getFieldName(srcDs);
				}
			}
			
			return new DataStruct(names);
		}
	}
}
