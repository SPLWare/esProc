package com.scudata.thread;

import java.io.IOException;
import java.util.TreeMap;

import com.scudata.common.Logger;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.BFileWriter;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.FileObject;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.op.IGroupsResult;
import com.scudata.expression.Expression;
import com.scudata.resources.EngineMessage;
import com.scudata.util.CursorUtil;

/**
 * 执行外存分组运算的任务
 * @author RunQian
 *
 */
public class GroupxJob extends Job {
	private ICursor cursor; // 数据游标
	private Expression gexp; // @g选项时的分段表达式
	private Expression[] exps; // 分组字段表达式数组
	private String[] names; // 分组字段名数组
	private Expression[] calcExps; // 汇总字段表达式数组
	private String[] calcNames; // 汇总字段名数组
	
	private Context ctx; // 计算上下文
	private int fetchCount; // 每次取数的数量
	private int capacity; // 内存能够存放的分组结果的数量，用于groupx@n
	
	private TreeMap<Object, BFileWriter> fileMap; // 大分组值和临时集文件映射
	
	// @g选项使用
	public GroupxJob(ICursor cursor, Expression gexp, Expression[] exps, String[] names,
			Expression[] calcExps, String[] calcNames, Context ctx, 
			int fetchCount, TreeMap<Object, BFileWriter> fileMap) {
		this.cursor = cursor;
		this.gexp = gexp;
		this.exps = exps;
		this.names = names;
		this.calcExps = calcExps;
		this.calcNames = calcNames;
		this.ctx = ctx;
		this.fileMap = fileMap;
		
		if (fetchCount > ICursor.FETCHCOUNT) {
			this.fetchCount = fetchCount;
		} else {
			this.fetchCount = ICursor.FETCHCOUNT;
		}
	}

	// @n选项使用
	public GroupxJob(ICursor cursor, Expression[] exps, String[] names,
			Expression[] calcExps, String[] calcNames, Context ctx, 
			int capacity, int fetchCount, TreeMap<Object, BFileWriter> fileMap) {
		this.cursor = cursor;
		this.exps = exps;
		this.names = names;
		this.calcExps = calcExps;
		this.calcNames = calcNames;
		this.ctx = ctx;
		this.fileMap = fileMap;
		this.capacity = capacity;
		if (fetchCount > ICursor.FETCHCOUNT) {
			this.fetchCount = fetchCount;
		} else {
			this.fetchCount = ICursor.FETCHCOUNT;
		}
	}
	
	private void groupx_g() {
		MessageManager mm = EngineMessage.get();
		String msg = mm.getMessage("engine.createTmpFile");
		ICursor cursor = this.cursor;
		Expression gexp = this.gexp;
		Expression[] exps = this.exps;
		String[] names = this.names;
		Expression[] calcExps = this.calcExps;
		String[] calcNames = this.calcNames;
		Context ctx = this.ctx;
		int fetchCount = this.fetchCount;
		TreeMap<Object, BFileWriter> fileMap = this.fileMap;
		DataStruct ds = cursor.getDataStruct();
		
		try {
			// 遍历游标数据
			while (true) {
				Sequence seq = cursor.fetch(fetchCount);
				if (seq == null || seq.length() == 0) {
					break;
				}

				// 按大分组表达式对数据进行分组
				Sequence groups = seq.group(gexp, null, ctx);
				int gcount = groups.length();
				for (int i = 1; i <= gcount; ++i) {
					// 对每个大分组进行首次汇总
					Sequence group = (Sequence)groups.getMem(i);
					Object gval = group.calc(1, gexp, ctx);
					IGroupsResult gresult = IGroupsResult.instance(exps, names, calcExps, calcNames, ds, null, ctx);
					gresult.push(group, ctx);
					group = gresult.getTempResult();
					
					// 对文件映射做同步取得相应的临时文件
					BFileWriter writer = null;
					synchronized(fileMap) {
						writer = fileMap.get(gval);
						if (writer == null) {
							FileObject fo = FileObject.createTempFileObject();
							Logger.info(msg + fo.getFileName());
							writer = new BFileWriter(fo, null);
							writer.prepareWrite(gresult.getResultDataStruct(), false);
							fileMap.put(gval, writer);
						}
					}
					
					// 锁住临时文件并把首次分组结果写到临时文件中
					synchronized(writer) {
						writer.write(group);
					}
				}
			}
		} catch (IOException e) {
			throw new RQException(e);
		}
	}
	
	private void groupx_n() {
		MessageManager mm = EngineMessage.get();
		String msg = mm.getMessage("engine.createTmpFile");
		ICursor cursor = this.cursor;
		Expression[] exps = this.exps;
		String[] names = this.names;
		Expression[] calcExps = this.calcExps;
		String[] calcNames = this.calcNames;
		Context ctx = this.ctx;
		int capacity = this.capacity;
		int fetchCount = this.fetchCount;
		TreeMap<Object, BFileWriter> fileMap = this.fileMap;
		DataStruct ds = cursor.getDataStruct();
		
		try {
			// 遍历游标数据
			while (true) {
				Sequence seq = cursor.fetch(fetchCount);
				if (seq == null || seq.length() == 0) {
					break;
				}

				// 进行首次汇总汇总
				IGroupsResult gresult = IGroupsResult.instance(exps, names, calcExps, calcNames, ds, null, ctx);
				gresult.push(seq, ctx);
				seq = gresult.getTempResult();
				
				Sequence groups = CursorUtil.group_n(seq, capacity);
				int gcount = groups.length();
				for (int i = 1; i <= gcount; ++i) {
					Sequence group = (Sequence)groups.getMem(i);
					if (group.length() == 0) {
						continue;
					}

					BaseRecord r = (BaseRecord)group.getMem(1);
					int index = ((Number)r.getNormalFieldValue(0)).intValue() / capacity + 1;
					Integer gval = new Integer(index);
					
					// 对文件映射做同步取得相应的临时文件
					BFileWriter writer = null;
					synchronized(fileMap) {
						writer = fileMap.get(gval);
						if (writer == null) {
							FileObject fo = FileObject.createTempFileObject();
							Logger.info(msg + fo.getFileName());
							writer = new BFileWriter(fo, null);
							writer.prepareWrite(gresult.getResultDataStruct(), false);
							fileMap.put(gval, writer);
						}
					}
					
					// 锁住临时文件并把首次分组结果写到临时文件中
					synchronized(writer) {
						writer.write(group);
					}
				}
			}
		} catch (IOException e) {
			throw new RQException(e);
		}
	}
	
	public void run() {
		if (gexp == null) {
			groupx_n();
		} else {
			groupx_g();
		}
	}
}
