package com.scudata.dm.op;

import java.io.IOException;
import java.util.ArrayList;

import com.scudata.common.Logger;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.BFileWriter;
import com.scudata.dm.ComputeStack;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Env;
import com.scudata.dm.FileObject;
import com.scudata.dm.ListBase1;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.BFileCursor;
import com.scudata.dm.cursor.ConjxCursor;
import com.scudata.dm.cursor.GroupmCursor;
import com.scudata.dm.cursor.GroupxCursor;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.cursor.MemoryCursor;
import com.scudata.dm.cursor.MergesCursor;
import com.scudata.expression.Expression;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;
import com.scudata.util.HashUtil;

/**
 * 用于对推送来的数据执行按多字段进行外存分组汇总运算
 * @author RunQian
 *
 */
public class GroupxResult implements IResult {
	private Expression[] exps; // 分组表达式
	private String []names; // 分组字段名
	private Expression[] calcExps; // 汇总表达式
	private String []calcNames; // 汇总字段名
	private Context ctx; // 计算上下文
	private String opt; // 选项

	private Node[] gathers = null; // 聚合函数数组
	private DataStruct ds; // 结果集数据结构
	private HashUtil hashUtil; // 哈希表工具，用于计算哈希值
	
	// 是否采用排序归并法进行二次汇总
	private boolean isSort = true;

	// 采用排序归并法进行二次汇总
	private ListBase1 []groups; // 分组哈希表
	private Table outTable; // 临时分组结果序表
	private int []sortFields; // 排序字段（即分组字段）序号
	private ArrayList<ICursor> cursorList = new ArrayList<ICursor>(); // 分组临时文件对应的游标
	
	// 利用哈希把组拆分到多个文件
	private RecordTree []recordsArray; // 分组哈希表
	private int totalRecordCount; // 内存中的分组结果记录总数
	private final int fileCount = 29; // 临时文件数
	private FileObject []tmpFiles; // 临时文件对象
	private BFileWriter []writers; // 用于写集文件的对象
	
	/**
	 * 构建外存分组结果对象
	 * @param exps 分组表达式数组
	 * @param names	分组字段名数组
	 * @param calcExps 汇总表达式	数组
	 * @param calcNames	汇总字段名数组
	 * @param opt 选项
	 * @param ctx 计算上下文
	 * @param capacity	内存中保存的最大分组结果数
	 */	
	public GroupxResult(Expression[] exps, String[] names, Expression[] calcExps, 
			String[] calcNames, String opt, Context ctx, int capacity) {
		this.exps = exps;
		this.names = names;
		this.calcExps = calcExps;
		this.calcNames = calcNames;
		this.opt = opt;
		this.ctx = ctx;
		this.hashUtil = new HashUtil(capacity);

		capacity = hashUtil.getCapacity();
		int keyCount = exps.length;
		int valCount = this.calcExps == null ? 0 : this.calcExps.length;
		
		if (names == null) names = new String[keyCount];
		for (int i = 0; i < keyCount; ++i) {
			if (names[i] == null || names[i].length() == 0) {
				names[i] = exps[i].getFieldName();
			}
		}

		if (this.calcNames == null) this.calcNames = new String[valCount];
		for (int i = 0; i < valCount; ++i) {
			if (this.calcNames[i] == null || this.calcNames[i].length() == 0) {
				this.calcNames[i] = this.calcExps[i].getFieldName();
			}
		}

		String[] colNames = new String[keyCount + valCount];
		System.arraycopy(names, 0, colNames, 0, keyCount);
		if (this.calcNames != null) {
			System.arraycopy(this.calcNames, 0, colNames, keyCount, valCount);
		}

		ds = new DataStruct(colNames);
		ds.setPrimary(names);
		this.gathers = Sequence.prepareGatherMethods(this.calcExps, ctx);
		
		if (opt == null || opt.indexOf('u') == -1) {
			groups = new ListBase1[capacity];
			outTable = new Table(ds, capacity);
			outTable.setPrimary(names);
	
			sortFields = new int[keyCount];
			for (int i = 0; i < keyCount; ++i) {
				sortFields[i] = i;
			}
		} else {
			isSort = false;
			recordsArray = new RecordTree[capacity];
		}
	}
	
	// 结果集需要有序时的分组方法
	private void sortGroup(Sequence table, Context ctx) {
		ListBase1 []groups = this.groups;
		Node []gathers = this.gathers;
		Expression[] exps = this.exps;
		Expression[] calcExps = this.calcExps;
		HashUtil hashUtil = this.hashUtil;
		Table outTable = this.outTable;
		
		int keyCount = exps.length;
		int valCount = calcExps == null ? 0 : calcExps.length;

		final int INIT_GROUPSIZE = HashUtil.getInitGroupSize();
		Object []keys = new Object[keyCount];
		int capacity = groups.length;

		ComputeStack stack = ctx.getComputeStack();
		Sequence.Current current = table.new Current();
		stack.push(current);

		try {
			for (int i = 1, len = table.length(); i <= len; ++i) {
				current.setCurrent(i);
				for (int k = 0; k < keyCount; ++k) {
					keys[k] = exps[k].calculate(ctx);
				}

				Record r;
				int hash = hashUtil.hashCode(keys);
				if (groups[hash] == null) {
					if (outTable.length() == capacity) {
						outTable.finishGather1(gathers);
						outTable.sortFields(sortFields);
						FileObject fo = FileObject.createTempFileObject();
						MessageManager mm = EngineMessage.get();
						Logger.info(mm.getMessage("engine.createTmpFile") + fo.getFileName());

						fo.exportSeries(outTable, "b", null);
						BFileCursor bfc = new BFileCursor(fo, null, "x", ctx);
						cursorList.add(bfc);

						outTable.clear();
						for (int g = 0, glen = groups.length; g < glen; ++g) {
							groups[g] = null;
						}
					}

					groups[hash] = new ListBase1(INIT_GROUPSIZE);
					r = outTable.newLast(keys);
					groups[hash].add(r);
					
					for (int v = 0, f = keyCount; v < valCount; ++v, ++f) {
						Object val = gathers[v].gather(ctx);
						r.setNormalFieldValue(f, val);
					}
				} else {
					int index = HashUtil.bsearch_r(groups[hash], keys);
					if (index < 1) {
						if (outTable.length() == capacity) {
							outTable.finishGather1(gathers);
							outTable.sortFields(sortFields);
							FileObject fo = FileObject.createTempFileObject();
							MessageManager mm = EngineMessage.get();
							Logger.info(mm.getMessage("engine.createTmpFile") + fo.getFileName());

							fo.exportSeries(outTable, "b", null);
							BFileCursor bfc = new BFileCursor(fo, null, "x", ctx);
							cursorList.add(bfc);
	
							outTable.clear();
							for (int g = 0, glen = groups.length; g < glen; ++g) {
								groups[g] = null;
							}
	
							groups[hash] = new ListBase1(INIT_GROUPSIZE);
							r = outTable.newLast(keys);
							groups[hash].add(r);
						} else {
							r = outTable.newLast(keys);
							groups[hash].add( -index, r);
						}
						
						for (int v = 0, f = keyCount; v < valCount; ++v, ++f) {
							Object val = gathers[v].gather(ctx);
							r.setNormalFieldValue(f, val);
						}
					} else {
						r = (Record)groups[hash].get(index);
						for (int v = 0, f = keyCount; v < valCount; ++v, ++f) {
							Object val = gathers[v].gather(r.getNormalFieldValue(f), ctx);
							r.setNormalFieldValue(f, val);
						}
					}
				}
			}
		} catch(RuntimeException e) {
			delete();
			throw e;
		} finally {
			stack.pop();
		}
	}
	
	private void writeTempFile(RecordTree []recordsArray) throws IOException {
		final int fileCount = this.fileCount;
		FileObject []tmpFiles = this.tmpFiles;
		BFileWriter []writers = this.writers;
		int len = recordsArray.length;
		
		if (tmpFiles == null) {
			tmpFiles = new FileObject[fileCount];
			writers = new BFileWriter[fileCount];
			this.tmpFiles = tmpFiles;
			this.writers = writers;
			MessageManager mm = EngineMessage.get();
			
			for (int i = 0; i < fileCount; ++i) {
				tmpFiles[i] = FileObject.createTempFileObject();
				writers[i] = new BFileWriter(tmpFiles[i], null);
				writers[i].prepareWrite(ds, false);
				
				Logger.info(mm.getMessage("engine.createTmpFile") + tmpFiles[i].getFileName());
				BFileCursor cursor = new BFileCursor(tmpFiles[i], null, "x", ctx);
				cursorList.add(cursor);
			}
		}
		
		Sequence []seqs = new Sequence[fileCount];
		int initSize = totalRecordCount / fileCount + 1024;
		for (int i = 0; i < fileCount; ++i) {
			seqs[i] = new Sequence(initSize);
		}
		
		for (int i = 0; i < len; ++i) {
			if (recordsArray[i] != null) {
				recordsArray[i].recursiveTraverse(seqs[i % fileCount]);
				recordsArray[i] = null;
			}
		}
		
		for (int i = 0; i < fileCount; ++i) {
			seqs[i].finishGather1(this.gathers);
			writers[i].write(seqs[i]);

		}
	}
	
	// 结果集不需要排序时的分组方法
	private void hashGroup(Sequence table, Context ctx) {
		DataStruct ds = this.ds;
		RecordTree []recordsArray = this.recordsArray;
		Node []gathers = this.gathers;
		Expression[] exps = this.exps;
		Expression[] calcExps = this.calcExps;
		HashUtil hashUtil = this.hashUtil;
		int totalRecordCount = this.totalRecordCount;
		
		int keyCount = exps.length;
		int valCount = calcExps == null ? 0 : calcExps.length;

		Object []keys = new Object[keyCount];
		int capacity = recordsArray.length;

		ComputeStack stack = ctx.getComputeStack();
		Sequence.Current current = table.new Current();
		stack.push(current);

		try {
			for (int i = 1, len = table.length(); i <= len; ++i) {
				current.setCurrent(i);
				for (int k = 0; k < keyCount; ++k) {
					keys[k] = exps[k].calculate(ctx);
				}

				Record r;
				int hash = hashUtil.hashCode(keys);
				if (recordsArray[hash] == null) {
					if (totalRecordCount == capacity) {
						writeTempFile(recordsArray);
						totalRecordCount = 0;
					}

					totalRecordCount++;
					r = new Record(ds, keys);
					recordsArray[hash] = new RecordTree(r);
					
					for (int v = 0, f = keyCount; v < valCount; ++v, ++f) {
						Object val = gathers[v].gather(ctx);
						r.setNormalFieldValue(f, val);
					}
				} else {
					RecordTree.Node node = recordsArray[hash].get(keys);
					r = node.r;
					
					// 数据没有找到相应分组值的记录
					if (r == null) {
						r = new Record(ds, keys);
						node.r = r;
						
						for (int v = 0, f = keyCount; v < valCount; ++v, ++f) {
							Object val = gathers[v].gather(ctx);
							r.setNormalFieldValue(f, val);
						}
						
						if (totalRecordCount < capacity) {
							totalRecordCount++;
						} else {
							writeTempFile(recordsArray);
							totalRecordCount = 0;
						}
					} else {
						for (int v = 0, f = keyCount; v < valCount; ++v, ++f) {
							Object val = gathers[v].gather(r.getNormalFieldValue(f), ctx);
							r.setNormalFieldValue(f, val);
						}
					}
				}
			}
			
			this.totalRecordCount = totalRecordCount;
		} catch(RuntimeException re) {
			delete();
			throw re;
		} catch(Exception e) {
			delete();
			throw new RQException(e.getMessage(), e);
		} finally {
			stack.pop();
		}
	}
	
	/**
	 * 对传入的数据做分组，累积到现有分组结果
	 * @param table 数据
	 * @param ctx 计算上下文
	 */
	public void push(Sequence table, Context ctx) {
		if (isSort) {
			sortGroup(table, ctx);
		} else {
			hashGroup(table, ctx);
		}
	}
	
	/**
	 * 删除临时文件，清空内存数据
	 */	
	private void delete() {
		this.hashUtil = null;
		
		if (isSort) {
			this.groups = null;
			this.outTable = null;
			for (ICursor cursor : cursorList) {
				cursor.close();
			}
		} else {
			this.recordsArray = null;
			if (writers != null) {
				for (BFileWriter writer : writers) {
					writer.close();
				}
				
				for (FileObject file : tmpFiles) {
					file.delete();
				}
			}
		}
	}

	// 取结果集需要有序时的分组方法生成的结果集
	private ICursor sortGroupResult() {
		ListBase1 []groups = this.groups;
		if (groups == null) return null;
		
		ArrayList<ICursor> cursorList = this.cursorList;
		int size = cursorList.size();
		if (size > 0) {
			int bufSize = Env.getMergeFileBufSize(size);
			for (int i = 0; i < size; ++i) {
				BFileCursor bfc = (BFileCursor)cursorList.get(i);
				bfc.setFileBufferSize(bufSize);
			}
		}

		if (outTable.length() > 0) {
			if (size == 0) {
				outTable.finishGather(gathers);
			} else {
				outTable.finishGather1(gathers);
			}
			
			outTable.sortFields(sortFields);
			cursorList.add(new MemoryCursor(outTable));
			size++;
		}

		this.hashUtil = null;
		this.groups = null;
		this.outTable = null;

		if (size == 0) {
			return null;
		} else if (size == 1) {
			return (ICursor)cursorList.get(0);
		} else {
			int keyCount = exps.length;
			ICursor []cursors = new ICursor[size];
			cursorList.toArray(cursors);
			Expression []keyExps = new Expression[keyCount];
			for (int i = 0, q = 1; i < keyCount; ++i, ++q) {
				keyExps[i] = new Expression(ctx, "#" + q);
			}

			MergesCursor mc = new MergesCursor(cursors, keyExps, ctx);
			int valCount = calcExps == null ? 0 : calcExps.length;
			Expression []valExps = new Expression[valCount];
			for (int i = 0, q = keyCount + 1; i < valCount; ++i, ++q) {
				valExps[i] = gathers[i].getRegatherExpression(q);
			}

			return new GroupmCursor(mc, keyExps, names, valExps, calcNames, ctx);
		}
	}
	
	// 取结果集不需要排序时的分组方法生成的结果集
	private ICursor hashGroupResult() {
		RecordTree []recordsArray = this.recordsArray;
		if (recordsArray == null) return null;
		
		// 判断是否用到了外存
		FileObject []tmpFiles = this.tmpFiles;
		if (tmpFiles == null) {
			Sequence seq = new Sequence(totalRecordCount);
			for (RecordTree tree : recordsArray) {
				if (tree != null) {
					tree.recursiveTraverse(seq);
				}
			}
			seq.finishGather(gathers);
			this.recordsArray = null;
			this.tmpFiles = null;
			this.writers = null;
			return new MemoryCursor(seq);
		}
		
		try {
			// 把内存中的记录写到相应的文件
			writeTempFile(recordsArray);
		} catch(Exception e) {
			delete();
			throw new RQException(e.getMessage(), e);
		}

		// 关闭写
		BFileWriter []writers = this.writers;
		for (BFileWriter writer : writers) {
			writer.close();
		}
		
		int fileCount = this.fileCount;
		ICursor []cursors = new ICursor[fileCount];
		
		// 取二次聚合需要用的表达式
		int keyCount = exps.length;
		Expression []keyExps = new Expression[keyCount];
		for (int i = 0, q = 1; i < keyCount; ++i, ++q) {
			keyExps[i] = new Expression(ctx, "#" + q);
		}

		int valCount = calcExps == null ? 0 : calcExps.length;
		Expression []valExps = new Expression[valCount];
		for (int i = 0, q = keyCount + 1; i < valCount; ++i, ++q) {
			valExps[i] = gathers[i].getRegatherExpression(q);
		}
		
		int capacity = hashUtil.getPrevCapacity();
		for (int i = 0; i < fileCount; ++i) {
			ICursor cursor = cursorList.get(i);
			cursors[i] = new GroupxCursor(cursor, keyExps, names, valExps, calcNames, opt, ctx, capacity);
		}

		this.recordsArray = null;
		this.tmpFiles = null;
		this.writers = null;
		
		return new ConjxCursor(cursors);
	}
	
	/**
	 * 返回结果游标
	 * @return ICursor
	 */
	public ICursor getResultCursor() {
		if (isSort) {
			return sortGroupResult();
		} else {
			return hashGroupResult();
		}
	}
	
	/**
	 * 返回计算结果
	 * @return Object
	 */
	public Object result() {
		return getResultCursor();
	}
	
	/**
	 * 不支持此方法
	 */
	public Object combineResult(Object []results) {
		throw new RuntimeException();
	}
}