package com.scudata.dm.cursor;

import java.util.ArrayList;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.*;
import com.scudata.dm.op.Operation;
import com.scudata.dw.ColPhyTable;
import com.scudata.dw.Cursor;
import com.scudata.expression.Expression;
import com.scudata.parallel.ClusterCursor;
import com.scudata.resources.EngineMessage;
import com.scudata.util.CursorUtil;

/**
 * 游标joinx类，归并joinx
 * 游标与一个可分段集文件或实表T做归并join运算。
 * @author LiWei
 * 
 */
public class CSJoinxCursor3 extends ICursor {
	private ICursor srcCursor;//源游标
	private Expression []keys;//维表字段
	private Expression []exps;//新的表达式
	
	private ICursor mergeCursor;//归并游标
	private DataStruct ds = null;
	private int len1;//原记录字段数
	private int len2;//新表达式字段数
	
	private boolean isEnd;
	private int n;//缓冲区条数
	private String[] expNames;
	private String option;
	
	/**
	 * 构造器
	 * @param cursor	源游标
	 * @param fields	事实join表字段
	 * @param fileTable	维表对象
	 * @param keys		维表join字段
	 * @param exps		维表新表达式
	 * @param expNames	维表新表达式名称
	 * @param fname
	 * @param ctx
	 * @param n
	 * @param option
	 */
	public CSJoinxCursor3(ICursor cursor, Expression []fields, Object fileTable, 
			Expression[] keys, Expression[] exps, String[] expNames, String fname, Context ctx, int n, String option) {
		srcCursor = cursor;
		this.keys = keys;
		this.exps = exps;
		this.ctx = ctx;
		this.n = n;
		this.option = option;
		this.expNames = expNames;
		if (this.n < ICursor.FETCHCOUNT) {
			this.n = ICursor.FETCHCOUNT;
		}
		
		//如果newNames里有null，则用newExps替代
		if (exps != null && expNames != null) {
			for (int i = 0, len = expNames.length; i < len; i++) {
				if (expNames[i] == null && exps[i] != null) {
					expNames[i] = exps[i].getFieldName();
				}
			}
		}

		//归并两个游标
		ICursor cursor2 = toCursor(fileTable);
		ICursor cursors[] = {cursor, cursor2};
		String names[] = {null, null};
		if (keys == null) {
			//没有关联字段时取主键
			String[] pkeys = cursor2.getDataStruct().getPrimary();
			int size = fields.length;
			if (pkeys == null || pkeys.length < size) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("joinx" + mm.getMessage("ds.lessKey"));
			}
			keys = new Expression[size];
			for (int i = 0; i < size; i++) {
				keys[i] = new Expression(pkeys[i]);
			}
		}
		Expression joinKeys[][] = {fields, keys};
		mergeCursor = joinx(cursors, names, joinKeys, option, ctx);
	}

	/**
	 * 游标对关联字段有序，做有序归并连接
	 * @param cursors 游标数组
	 * @param names 结果集字段名数组
	 * @param exps 关联字段表达式数组
	 * @param opt 选项
	 * @param ctx Context 计算上下文
	 * @return ICursor 结果集游标
	 */
	private static ICursor joinx(ICursor []cursors, String []names, Expression [][]exps, String opt, Context ctx) {
		boolean isPJoin = false, isIsect = false, isDiff = false;
		if (opt != null) {
			if (opt.indexOf('p') != -1) {
				isPJoin = true;
			} else if (opt.indexOf('i') != -1) {
				isIsect = true;
			} else if (opt.indexOf('d') != -1) {
				isDiff = true;
			}
		}
		
		int count = cursors.length;
		boolean isCluster = true; // 是否有集群游标
		boolean isMultipath = false; // 是否是多路游标连接
		int pathCount = 1;
		
		for (int i = 0; i < count; ++i) {
			if (cursors[i] instanceof IMultipath) {
				if (i == 0) {
					isMultipath = true;
					pathCount = ((IMultipath)cursors[i]).getPathCount();
				} else if (pathCount != ((IMultipath)cursors[i]).getPathCount()) {
					isMultipath = false;
				}
			} else {
				isMultipath = false;
			}
			
			if (!(cursors[i] instanceof ClusterCursor)) {
				isCluster = false;
			}
		}
		
		if (isCluster) {
			ClusterCursor []tmp = new ClusterCursor[count];
			System.arraycopy(cursors, 0, tmp, 0, count);
			return ClusterCursor.joinx(tmp, exps, names, opt, ctx);
		} else if (isMultipath && pathCount > 1) {
			// 多路游标会做同步分段，只要每个表的相应路做连接即可
			ICursor []result = new ICursor[pathCount];
			ICursor [][]multiCursors = new ICursor[count][];
			for (int i = 0; i < count; ++i) {
				IMultipath multipath = (IMultipath)cursors[i];
				multiCursors[i] = multipath.getParallelCursors();
			}
			
			for (int i = 0; i < pathCount; ++i) {
				if (isPJoin) {
					ICursor []curs = new ICursor[count];
					for (int c = 0; c < count; ++c) {
						curs[c] = multiCursors[c][i];
					}

					result[i] = new PJoinCursor(curs, names);
				} else if (isIsect || isDiff) {
					ICursor []curs = new ICursor[count];
					for (int c = 0; c < count; ++c) {
						curs[c] = multiCursors[c][i];
					}
					
					Context tmpCtx = ctx.newComputeContext();
					Expression [][]tmpExps = Operation.dupExpressions(exps, tmpCtx);
					result[i] = new MergeFilterCursor(curs, tmpExps, opt, tmpCtx);
				} else {
					if (count == 2 && exps[0].length == 1) {
						Context tmpCtx = ctx.newComputeContext();
						Expression exp1 = Operation.dupExpression(exps[0][0], tmpCtx);
						Expression exp2 = Operation.dupExpression(exps[1][0], tmpCtx);
						result[i] = new JoinxCursor3(multiCursors[0][i], exp1, multiCursors[1][i], exp2, names, opt, tmpCtx);
					} else {
						ICursor []curs = new ICursor[count];
						for (int c = 0; c < count; ++c) {
							curs[c] = multiCursors[c][i];
						}
						
						Context tmpCtx = ctx.newComputeContext();
						Expression [][]tmpExps = Operation.dupExpressions(exps, tmpCtx);
						result[i] = new JoinxCursor(curs, tmpExps, names, opt, tmpCtx);
					}
				}
			}
			
			// 每一路的关联结果再组成多路游标
			return new MultipathCursors(result, ctx);
		} else if (isPJoin) {
			return new PJoinCursor(cursors, names);
		} else if (isIsect || isDiff) {
			return new MergeFilterCursor(cursors, exps, opt, ctx);
		} else {
			if (count == 2 && exps[0].length == 1) {
				// 对关联字段个数为1的两表连接做优化
				return new JoinxCursor2(cursors[0], exps[0][0], cursors[1], exps[1][0], names, opt, ctx);
			} else {
				return new JoinxCursor(cursors, exps, names, opt, ctx);
			}
		}
	}
	
	void init() {
		//组织数据结构
		if (option !=null && (option.indexOf('i') != -1 || option.indexOf('d') != -1)) {
			Sequence temp = mergeCursor.peek(1);
			if (temp != null) {
				BaseRecord r = (BaseRecord) temp.getMem(1);
				ds = r.dataStruct();
				len1 = 0;
			}
		} else {
			Sequence temp = mergeCursor.peek(1);
			if (temp != null) {
				BaseRecord r = (BaseRecord) temp.getMem(1);
				BaseRecord r1 = (BaseRecord) r.getNormalFieldValue(0);
				len1 = r1.getFieldCount();
				len2 = exps == null ? 0 : exps.length;
				String[] names = new String[len1 + len2];
				System.arraycopy(r1.getFieldNames(), 0, names, 0, len1);
				System.arraycopy(expNames, 0, names, len1, len2);
				ds = new DataStruct(names);
			}
		}
	}
	
	/**
	 * 从join字段和新表达式中提取需要的字段
	 * @param dataExps
	 * @param newExps
	 * @param ctx
	 * @return
	 */
	private static String[] makeFields(Expression []dataExps, Expression []newExps ,Context ctx) {
		int len = dataExps.length;
		ArrayList<String> keys = new ArrayList<String>(len);
		for (int j = 0; j < len; j++) {
			keys.add(dataExps[j].toString());
		}
		for (Expression exp : newExps) {
			exp.getUsedFields(ctx, keys);
		}
		String[] arr = new String[keys.size()];
		keys.toArray(arr);
		return arr;
	}
	
	/**
	 * 把维表对象转换成游标
	 * @param obj
	 * @return
	 */
	private ICursor toCursor(Object obj) {
		if (obj instanceof ColPhyTable) {
			String fields[] = makeFields(keys, exps, ctx);
			return (Cursor) ((ColPhyTable) obj).cursor(null, fields, null, null, null, null, null, ctx);
		} else if (obj instanceof FileObject) {
			return new BFileCursor((FileObject) obj, null, null, null);
		} else if (obj instanceof ICursor) {
			return (ICursor) obj;
		} else {
			return null;
		}
	}
	
	// 并行计算时需要改变上下文
	// 继承类如果用到了表达式还需要用新上下文重新解析表达式
	public void resetContext(Context ctx) {
		if (this.ctx != ctx) {
			exps = Operation.dupExpressions(exps, ctx);
			super.resetContext(ctx);
		}
	}

	protected Sequence get(int n) {
		if (isEnd || n < 1) return null;
		
		if (ds == null) {
			init();
		}
		
		Sequence temp = mergeCursor.fetch(n);
		if (temp == null || temp.length() == 0) {
			return null;
		}
		
		if (len1 == 0) {
			return temp;
		}
		
		Context ctx = this.ctx;
		Expression []exps = this.exps;
		int len1 = this.len1;
		int len2 = this.len2;
		int len = temp.length();
		Table result = new Table(ds);
		for (int i = 1; i <= len; i++) {
			BaseRecord r = (BaseRecord) temp.getMem(i);
			BaseRecord r1 = (BaseRecord) r.getNormalFieldValue(0);
			Object r2 = r.getNormalFieldValue(1);
			
			BaseRecord record = result.newLast(r1.getFieldValues());
			for (int f = 0; f < len2; f++) {
				if (r2 != null) {
					if (r2 instanceof BaseRecord) {
						record.setNormalFieldValue(f + len1, ((BaseRecord)r2).calc(exps[f], ctx));	
					} else if (r2 instanceof Sequence) {
						record.setNormalFieldValue(f + len1, ((Sequence)r2).calc(exps[f], ctx));
					}
					
				}
			}
		}
		
		return result;
	}

	protected long skipOver(long n) {
		if (isEnd || n < 1) return 0;
		long total = 0;
		while (n > 0) {
			Sequence seq;
			if (n > FETCHCOUNT) {
				seq = get(FETCHCOUNT);
			} else {
				seq = get((int)n);
			}
			
			if (seq == null || seq.length() == 0) {
				break;
			}
			
			total += seq.length();
			n -= seq.length();
		}
		
		return total;
	}

	public synchronized void close() {
		super.close();
		srcCursor.close();
		isEnd = true;
	}
	
	/**
	 * 重置游标
	 * @return 返回是否成功，true：游标可以从头重新取数，false：不可以从头重新取数
	 */
	public boolean reset() {
		super.close();
		srcCursor.reset();
		isEnd = false;
		return true;
	}
	
	/**
	 * 归并join（多套数据）
	 * @param cursor	源游标
	 * @param fields	事实join表字段
	 * @param fileTable	维表对象
	 * @param keys		维表join字段
	 * @param exps		维表新表达式
	 * @param expNames	维表新表达式名称
	 * @param fname
	 * @param ctx
	 * @param n
	 * @param option
	 * @return
	 */
	public static ICursor MergeJoinx(ICursor cursor, Expression[][] fields, Object[] fileTable, Expression[][] keys,
			Expression[][] exps, String[][] expNames, String fname, Context ctx, int n, String option) {
		if (fileTable == null) {
			return null;
		}
		
		if (option.indexOf('i') == -1) {
			option += '1';
		} else {
			option = option.replaceAll("i", "");
		}
		
		if (cursor instanceof MultipathCursors) {
			return MultipathMergeJoinx((MultipathCursors)cursor, fields, fileTable, keys,
					exps, expNames, fname, ctx, n, option);
		}

		ICursor temp = null;
		FileObject tempFile = null;
		int fileCount =  fileTable.length;
		try {
			/**
			 * 对多套数据进行join，每次的结果写出到文件，（不处理最后一套join）
			 */
			for (int i = 0; i < fileCount - 1; i++) {
				temp = new CSJoinxCursor3(cursor, fields[i], fileTable[i], keys[i], exps[i], 
						expNames[i], fname, ctx, n, option);
				
				tempFile = FileObject.createTempFileObject();
				cursor = new BFileCursor(tempFile, null, "x", ctx);
				tempFile.setFileSize(0);
				
				Sequence table = temp.fetch(FETCHCOUNT);
				while (table != null && table.length() != 0) {
					tempFile.exportSeries(table, "ab", null);
					table = temp.fetch(FETCHCOUNT);
				}
				temp = null;
			}
		} catch (Exception e) {
			if (temp != null) {
				temp.close();
			}
			if (tempFile != null && tempFile.isExists()) {
				tempFile.delete();
			}
			if (e instanceof RQException) {
				throw (RQException)e;
			} else {
				throw new RQException(e.getMessage(), e);
			}
		}
		
		int i = fileCount - 1;
		return new CSJoinxCursor3(cursor, fields[i], fileTable[i], keys[i], exps[i], 
				expNames[i], fname, ctx, n, option);
	}
	
	/**
	 * 把维表对象转换成游标
	 * @param obj
	 * @return
	 */
	private static MultipathCursors toMultipathCursors(Object obj, MultipathCursors mcs,  String fields[], Context ctx) {
		if (obj instanceof ColPhyTable) {
			return (MultipathCursors) ((ColPhyTable) obj).cursor(null, fields, null, null, null, null, mcs, "k", ctx);
		} if (obj instanceof MultipathCursors) {
			return (MultipathCursors) obj;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("joinx" + mm.getMessage("dw.needMCursor"));
		}
	}
	
	public static ICursor MultipathMergeJoinx(MultipathCursors cursor, Expression[][] fields, Object[] fileTable, Expression[][] keys,
			Expression[][] exps, String[][] expNames, String fname, Context ctx, int n, String option) {
		ICursor[] cursors = cursor.getParallelCursors();
		int pathCount = cursor.getPathCount();
		ICursor results[] = new ICursor[pathCount];
		
		String[] names = makeFields(keys[0], exps[0], ctx);
		ICursor[] fileTableCursors = toMultipathCursors(fileTable[0], cursor, names, ctx).getParallelCursors();
		
		if (fileTableCursors == null) {
			for (int i = 0; i < pathCount; ++i) {
				Expression[][] fields_ = Operation.dupExpressions(fields, ctx);
				Expression[][] keys_ = Operation.dupExpressions(keys, ctx);
				Expression[][] exps_ = Operation.dupExpressions(exps, ctx);
				
				results[i] = MergeJoinx(cursors[i], fields_, fileTable,
						keys_, exps_, expNames, fname, ctx, n, option);
			}
		} else {
			if (fileTable.length != 1) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("joinx" + mm.getMessage("function.invalidParam"));
			}
			for (int i = 0; i < pathCount; ++i) {
				Expression[][] fields_ = Operation.dupExpressions(fields, ctx);
				Expression[][] keys_ = Operation.dupExpressions(keys, ctx);
				Expression[][] exps_ = Operation.dupExpressions(exps, ctx);
				
				results[i] = MergeJoinx(cursors[i], fields_, new Object[] {fileTableCursors[i]},
						keys_, exps_, expNames, fname, ctx, n, option);
			}
		}
		
		return new MultipathCursors(results, ctx);
	}
}
