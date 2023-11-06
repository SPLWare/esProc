package com.scudata.dw;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.BFileWriter;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Env;
import com.scudata.dm.FileObject;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.BFileCursor;
import com.scudata.dm.cursor.ConjxCursor;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.cursor.MergeCursor;
import com.scudata.dm.cursor.MergeCursor2;
import com.scudata.dm.cursor.MultipathCursors;
import com.scudata.dm.cursor.UpdateMergeCursor;
import com.scudata.dm.op.Operation;
import com.scudata.expression.Expression;
import com.scudata.resources.EngineMessage;

/**
 * 文件组
 * @author runqian
 *
 */
public class PhyTableGroup implements IPhyTable {
	private IPhyTable []tables;
	private String fileName;
	private int []partitions;
	private String opt;
	private Context ctx;
	private Expression distribute;
	
	private Sequence memoryTable; // 内存分表
	
	public PhyTableGroup(String fileName, IPhyTable []tables, int []partitions, String opt, Context ctx) {
		this.fileName = fileName;
		this.tables = tables;
		this.partitions = partitions;
		this.opt = opt;
		this.ctx = ctx;
		distribute = new Expression(ctx, tables[0].getDistribute());
	}
	
	public PhyTableGroup(String fileName, IPhyTable []tables, int []partitions, String opt, Expression distribute, Context ctx) {
		this.fileName = fileName;
		this.tables = tables;
		this.partitions = partitions;
		this.opt = opt;
		this.ctx = ctx;
		this.distribute = distribute;
	}
	
	public void close() {
		for (IPhyTable table : tables) {
			table.close();
		}
	}
	
	/**
	 * 返回组表是否是按列存储的
	 * @return true：是，false：不是
	 */
	public boolean isColumnStored() {
		return tables[0] instanceof ColPhyTable;
	}
	
	public IPhyTable createAnnexTable(String []colNames, int []serialBytesLen, String tableName) throws IOException {
		int count = tables.length;
		IPhyTable []annexTables = new IPhyTable[count];
		for (int i = 0; i < count; ++i) {
			annexTables[i] = tables[i].createAnnexTable(colNames, serialBytesLen, tableName);
		}
		
		return new PhyTableGroup(fileName, annexTables, partitions, opt, distribute, ctx);
	}
	
	public IPhyTable getAnnexTable(String tableName) {
		int count = tables.length;
		IPhyTable []annexTables = new IPhyTable[count];
		for (int i = 0; i < count; ++i) {
			annexTables[i] = tables[i].getAnnexTable(tableName);
		}
		
		return new PhyTableGroup(fileName, annexTables, partitions, opt, distribute, ctx);
	}
	
	public void append(ICursor cursor) throws IOException {
		append(cursor, null);
	}
	
	/**
	 * 带x选项时的数据追加，游标里的数据对应多个分区
	 * @param cursor
	 * @param opt
	 * @throws IOException
	 */
	private void append_x(ICursor cursor, String opt) throws IOException {
		Expression distribute = this.distribute;
		if (distribute == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("dw.lessDistribute"));
		}
		
		Sequence data = cursor.fetch(ICursor.INITSIZE);
		if (data == null || data.length() == 0) {
			return;
		}
		
		Context ctx = this.ctx;
		DataStruct ds = data.dataStruct();
		if (ds == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.needPurePmt"));
		}
		
		ArrayList<BFileWriter>tmpFiles = new ArrayList<BFileWriter>();
		tmpFiles.add(null); // 0位置保留，分区1放在List的索引1处
		int partCount = 0;
		
		try {
			while (data != null && data.length() > 0) {
				Sequence group = data.group(distribute, null, ctx);
				int gcount = group.length();
				for (int g = 1; g <= gcount; ++g) {
					Sequence curGroup = (Sequence)group.getMem(g);
					Object obj = curGroup.calc(1, distribute, ctx);
					if (!(obj instanceof Number)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(mm.getMessage("dw.distributeError"));
					}
					
					int p = ((Number)obj).intValue();
					if (p < 1) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(mm.getMessage("dw.distributeError"));
					}
					
					if (p > partCount) {
						for (; partCount < p; ++partCount) {
							tmpFiles.add(null);
						}
					}
					
					BFileWriter writer = tmpFiles.get(p);
					if (writer == null) {
						writer = new BFileWriter(FileObject.createTempFileObject(), null);
						writer.prepareWrite(ds, false);
						tmpFiles.set(p, writer);
					}
					
					writer.write(curGroup);
				}
				
				data = cursor.fetch(ICursor.INITSIZE);
			}
			
			for (int p = 1; p <= partCount; ++p) {
				BFileWriter writer = tmpFiles.get(p);
				if (writer == null) {
					continue;
				}
				
				writer.close();
				BFileCursor bcs = new BFileCursor(writer.getFile(), null, null, ctx);
				getPartition(p).append(bcs, opt);
			}
		} finally {
			// 删除临时文件
			for (int i = 1; i <= partCount; ++i) {
				BFileWriter writer = tmpFiles.get(i);
				if (writer != null) {
					writer.getFile().delete();
				}
			}
		}
	}
	
	public void append(ICursor cursor, String opt) throws IOException {
		if (opt != null) {
			if (opt.indexOf('y') != -1) {
				memoryTable = cursor.fetch();
				return;
			} else if (opt.indexOf('x') != -1) {
				append_x(cursor, opt);
				return;
			}
		}
		
		Expression distribute = this.distribute;
		if (distribute == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("dw.lessDistribute"));
		}
		
		// 每个单路游标的数据属于同一个分区
		if (cursor instanceof MultipathCursors) {
			ICursor []cursors = ((MultipathCursors)cursor).getCursors();
			for (ICursor cs : cursors) {
				Sequence data = cs.peek(ICursor.FETCHCOUNT);
				if (data == null || data.length() == 0) {
					continue;
				}
				
				Object obj = data.calc(1, distribute, ctx);
				if (!(obj instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("dw.distributeError"));
				}
				
				int p = ((Number)obj).intValue();
				if (p < 1) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("dw.distributeError"));
				}
				
				getPartition(p).append(cs, opt);
			}
		} else {
			Sequence data = cursor.peek(ICursor.FETCHCOUNT);
			if (data == null || data.length() == 0) {
				return;
			}
			
			Object obj = data.calc(1, distribute, ctx);
			if (!(obj instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("dw.distributeError"));
			}
			
			int p = ((Number)obj).intValue();
			if (p < 1) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("dw.distributeError"));
			}
			
			getPartition(p).append(cursor, opt);
		}
	}
	
	private IPhyTable getPartition(int p) {
		int pcount = partitions.length;
		for (int i = 0; i < pcount; ++i) {
			if (partitions[i] == p) {
				return tables[i];
			}
		}
		
		// 如果还没有创建此分区的组表则创建
		File file = Env.getPartitionFile(p, fileName);
		PhyTable tmd = (PhyTable)tables[0];
		ComTable gt = tmd.getGroupTable().dupStruct(file);
		PhyTable result = gt.getBaseTable();
		String tableName = tmd.getTableName();
		if (tableName != null && tableName.length() != 0) {
			result = result.getAnnexTable(tableName);
		}
		
		IPhyTable []tmpTables = new IPhyTable[pcount + 1];
		int []tmpPartitions = new int[pcount + 1];
		System.arraycopy(tables, 0, tmpTables, 0, pcount);
		System.arraycopy(partitions, 0, tmpPartitions, 0, pcount);
		tmpTables[pcount] = result;
		tmpPartitions[pcount] = p;
		
		this.tables = tmpTables;
		this.partitions = tmpPartitions;
		return result;
	}
	
	public Sequence update(Sequence data, String opt) {
		throw new RQException("'update' function is unimplemented in file group!");
	}
	
	public Sequence delete(Sequence data, String opt) {
		throw new RQException("'delete' function is unimplemented in file group!");
	}
	
	public ICursor cursor(Expression []exps, String []fields, Expression filter, 
			String []fkNames, Sequence []codes, String []opts, MultipathCursors mcs, String opt, Context ctx) {
		ICursor []cursors = mcs.getCursors();
		int pathCount = cursors.length;
		int tableCount = tables.length;
		ArrayList<ICursor> []lists = new ArrayList[pathCount];
		
		if (opt != null && opt.indexOf('w') != -1) {
			MultipathCursors mcs2 = (MultipathCursors)tables[0].cursor(exps, fields, 
					filter, fkNames, codes, opts, mcs, opt, ctx);
			
			int deleteField = getDeleteFieldIndex(exps, fields);
			cursors = mcs2.getCursors();
			DataStruct ds = cursors[0].getDataStruct();
			if (ds == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("ds.lessKey"));
			}
			
			int []pkIndex = ds.getPKIndex();
			if (pkIndex == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("ds.lessKey"));
			}
			
			for (int p = 0; p < cursors.length; ++p) {
				lists[p] = new ArrayList<ICursor>(tableCount);
				lists[p].add(cursors[p]);
			}
			
			for (int i = 1; i < tableCount; ++i) {
				mcs2 = (MultipathCursors)tables[i].cursor(exps, fields, 
						filter, fkNames, codes, opts, mcs, opt, ctx);
				cursors = mcs2.getCursors();
				for (int p = 0; p < cursors.length; ++p) {
					lists[p].add(cursors[p]);
				}
			}
			
			if (memoryTable != null) {
				String []sortedFields = getAllSortedColNames();
				mcs2 = memoryTable.cursor(mcs, sortedFields, exps, fields, 
						filter, fkNames, codes, opts, opt, ctx);
				cursors = mcs2.getCursors();
				for (int p = 0; p < cursors.length; ++p) {
					lists[p].add(cursors[p]);
				}
			}
			
			ICursor []resultCursors = new ICursor[pathCount];
			for (int i = 0; i < pathCount; ++i) {
				int size = lists[i].size();
				if (size > 1) {
					cursors = new ICursor[size];
					lists[i].toArray(cursors);
					resultCursors[i] = new UpdateMergeCursor(cursors, pkIndex, deleteField, ctx);
				} else if (size == 1) {
					resultCursors[i] = lists[i].get(0);
				}
			}
			
			return new MultipathCursors(resultCursors, ctx);
		} else {
			MultipathCursors mcs2 = (MultipathCursors)tables[0].cursor(exps, fields, 
					filter, fkNames, codes, opts, mcs, opt, ctx);
			cursors = mcs2.getCursors();
			String []sortFields = cursors[0].getSortFields();
			DataStruct ds = cursors[0].getDataStruct();
			if (ds == null || sortFields == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("grouptable.dataNeedSorted"));
			}
			
			int fcount = sortFields.length;
			int []findex = new int[fcount];
			for (int i = 0; i < fcount; ++i) {
				findex[i] = ds.getFieldIndex(sortFields[i]);
			}
			
			for (int p = 0; p < cursors.length; ++p) {
				lists[p] = new ArrayList<ICursor>(tableCount);
				lists[p].add(cursors[p]);
			}
			
			for (int i = 1; i < tableCount; ++i) {
				mcs2 = (MultipathCursors)tables[i].cursor(exps, fields, 
						filter, fkNames, codes, opts, mcs, opt, ctx);
				cursors = mcs2.getCursors();
				for (int p = 0; p < cursors.length; ++p) {
					lists[p].add(cursors[p]);
				}
			}
			
			if (memoryTable != null) {
				String []sortedFields = getAllSortedColNames();
				mcs2 = memoryTable.cursor(mcs, sortedFields, exps, fields, filter, fkNames, codes, opts, opt, ctx);
				cursors = mcs2.getCursors();
				for (int p = 0; p < cursors.length; ++p) {
					lists[p].add(cursors[p]);
				}
			}
			
			ICursor []resultCursors = new ICursor[pathCount];
			for (int i = 0; i < pathCount; ++i) {
				int size = lists[i].size();
				if (size > 1) {
					cursors = new ICursor[size];
					lists[i].toArray(cursors);
					resultCursors[i] = new MergeCursor(cursors, findex, null, ctx);
				} else if (size == 1) {
					resultCursors[i] = lists[i].get(0);
				}
			}
			
			return new MultipathCursors(resultCursors, ctx);
		}
	}
	
	public void rename(String[] srcFields, String[] newFields, Context ctx) throws IOException {
		throw new RQException("'rename' function is unimplemented in file group!");
	}
	
	public ICursor cursor() {
		return cursor(null, null, null, null, null, null, null, ctx);
	}
	
	public ICursor cursor(String []fields) {
		return cursor(null, fields, null, null, null, null, null, ctx);
	}
	
	public ICursor cursor(String []fields, Expression filter, Context ctx) {
		return cursor(null, fields, filter, null, null, null, null, ctx);
	}
	
	public ICursor cursor(Expression []exps, String []fields, Expression filter, 
			String []fkNames, Sequence []codes, String[] opts, String opt, Context ctx) {
		int count = tables.length;
		ICursor []cursors;
		if (memoryTable == null) {
			cursors = new ICursor[count];
		} else {
			cursors = new ICursor[count + 1];
			exps = Operation.dupExpressions(exps, ctx);
			filter = Operation.dupExpression(filter, ctx);
			cursors[count] = memoryTable.cursor(exps, fields, filter, fkNames, codes, opts, ctx);
		}
		
		for (int i = 0; i < count; ++i) {
			exps = Operation.dupExpressions(exps, ctx);
			filter = Operation.dupExpression(filter, ctx);
			cursors[i] = tables[i].cursor(exps, fields, filter, fkNames, codes, opts, opt, ctx);
		}
		
		boolean isUpdate = false, isMerge = true;
		if (opt != null) {
			if (opt.indexOf('w') != -1) {
				isUpdate = true;
			} else if (opt.indexOf('o') != -1) {
				isMerge = false;
			}
		}
		
		if (isUpdate) {
			DataStruct ds = cursors[0].getDataStruct();
			if (ds == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("ds.lessKey"));
			}
			
			int []pkIndex = ds.getPKIndex();
			if (pkIndex == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("ds.lessKey"));
			}
			
			int deleteField = getDeleteFieldIndex(exps, fields);
			return new UpdateMergeCursor(cursors, pkIndex, deleteField, ctx);
		} else if (isMerge) {
			String []sortFields = cursors[0].getSortFields();
			DataStruct ds = cursors[0].getDataStruct();
			if (ds == null || sortFields == null) {
				return new ConjxCursor(cursors);
			}
			
			int fcount = sortFields.length;
			int []findex = new int[fcount];
			for (int i = 0; i < fcount; ++i) {
				findex[i] = ds.getFieldIndex(sortFields[i]);
			}
			
			return new MergeCursor(cursors, findex, null, ctx);
		} else {
			return new ConjxCursor(cursors);
		}
	}
	
	public ICursor cursor(Expression []exps, String []fields, Expression filter, 
			String []fkNames, Sequence []codes, String[] opts, int pathCount, String opt, Context ctx) {
		if (pathCount < 2) {
			return cursor(exps, fields, filter, fkNames, codes, opts, opt, ctx);
		}
		
		int tableCount = tables.length;
		ArrayList<ICursor> []lists = new ArrayList[pathCount];
		ICursor cs = tables[0].cursor(exps, fields, filter, fkNames, codes, opts, pathCount, opt, ctx);
		
		boolean isUpdate = false, isMerge = true;
		if (opt != null) {
			if (opt.indexOf('w') != -1) {
				isUpdate = true;
			} else if (opt.indexOf('o') != -1) {
				isMerge = false;
			}
		}
		
		if (isUpdate) {
			int deleteField = getDeleteFieldIndex(exps, fields);
			if (cs instanceof MultipathCursors) {
				MultipathCursors mcs = (MultipathCursors)cs;
				ICursor []cursors = mcs.getCursors();
				pathCount = cursors.length;
				DataStruct ds = cursors[0].getDataStruct();
				if (ds == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("ds.lessKey"));
				}
				
				int []pkIndex = ds.getPKIndex();
				if (pkIndex == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("ds.lessKey"));
				}
				
				for (int p = 0; p < pathCount; ++p) {
					lists[p] = new ArrayList<ICursor>(tableCount);
					lists[p].add(cursors[p]);
				}
				
				for (int i = 1; i < tableCount; ++i) {
					MultipathCursors mcs2 = (MultipathCursors)tables[i].cursor(exps, fields, 
							filter, fkNames, codes, opts, mcs, opt, ctx);
					cursors = mcs2.getCursors();
					for (int p = 0; p < cursors.length; ++p) {
						lists[p].add(cursors[p]);
					}
				}
				
				if (memoryTable != null) {
					String []sortedFields = getAllSortedColNames();
					MultipathCursors mcs2 = memoryTable.cursor(mcs, sortedFields, exps, fields, 
							filter, fkNames, codes, opts, opt, ctx);
					cursors = mcs2.getCursors();
					for (int p = 0; p < cursors.length; ++p) {
						lists[p].add(cursors[p]);
					}
				}
				
				ICursor []resultCursors = new ICursor[pathCount];
				for (int i = 0; i < pathCount; ++i) {
					int size = lists[i].size();
					if (size > 1) {
						cursors = new ICursor[size];
						lists[i].toArray(cursors);
						resultCursors[i] = new UpdateMergeCursor(cursors, pkIndex, deleteField, ctx);
					} else if (size == 1) {
						resultCursors[i] = lists[i].get(0);
					}
				}
				
				return new MultipathCursors(resultCursors, ctx);
			} else {
				DataStruct ds = cs.getDataStruct();
				if (ds == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("ds.lessKey"));
				}
				
				int []pkIndex = ds.getPKIndex();
				if (pkIndex == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("ds.lessKey"));
				}
				
				ICursor []cursors;
				if (memoryTable == null) {
					cursors = new ICursor[tableCount];
				} else {
					cursors = new ICursor[tableCount + 1];
					exps = Operation.dupExpressions(exps, ctx);
					filter = Operation.dupExpression(filter, ctx);
					cursors[tableCount] = memoryTable.cursor(exps, fields, filter, fkNames, codes, opts, ctx);
				}
				
				cursors[0] = cs;
				for (int i = 1; i < tableCount; ++i) {
					exps = Operation.dupExpressions(exps, ctx);
					filter = Operation.dupExpression(filter, ctx);
					cursors[i] = tables[i].cursor(exps, fields, filter, fkNames, codes, opts, opt, ctx);
				}
								
				return new UpdateMergeCursor(cursors, pkIndex, deleteField, ctx);
			}
		} else if (isMerge) {
			if (cs instanceof MultipathCursors) {
				MultipathCursors mcs = (MultipathCursors)cs;
				ICursor []cursors = mcs.getCursors();
				pathCount = cursors.length;
				String []sortFields = cursors[0].getSortFields();
				DataStruct ds = cursors[0].getDataStruct();
				
				if (ds != null && sortFields != null) {
					int fcount = sortFields.length;
					int []findex = new int[fcount];
					for (int i = 0; i < fcount; ++i) {
						findex[i] = ds.getFieldIndex(sortFields[i]);
					}
					
					for (int p = 0; p < pathCount; ++p) {
						lists[p] = new ArrayList<ICursor>(tableCount);
						lists[p].add(cursors[p]);
					}
					
					for (int i = 1; i < tableCount; ++i) {
						MultipathCursors mcs2 = (MultipathCursors)tables[i].cursor(exps, fields, 
								filter, fkNames, codes, opts, mcs, opt, ctx);
						cursors = mcs2.getCursors();
						for (int p = 0; p < cursors.length; ++p) {
							lists[p].add(cursors[p]);
						}
					}
					
					if (memoryTable != null) {
						String []sortedFields = getAllSortedColNames();
						MultipathCursors mcs2 = memoryTable.cursor(mcs, sortedFields, exps, fields, 
								filter, fkNames, codes, opts, opt, ctx);
						cursors = mcs2.getCursors();
						for (int p = 0; p < cursors.length; ++p) {
							lists[p].add(cursors[p]);
						}
					}
					
					ICursor []resultCursors = new ICursor[pathCount];
					for (int i = 0; i < pathCount; ++i) {
						int size = lists[i].size();
						if (size > 1) {
							cursors = new ICursor[size];
							lists[i].toArray(cursors);
							resultCursors[i] = new MergeCursor(cursors, findex, null, ctx);
						} else if (size == 1) {
							resultCursors[i] = lists[i].get(0);
						}
					}
					
					return new MultipathCursors(resultCursors, ctx);
				}
			} else {
				ICursor []cursors;
				if (memoryTable == null) {
					cursors = new ICursor[tableCount];
				} else {
					cursors = new ICursor[tableCount + 1];
					exps = Operation.dupExpressions(exps, ctx);
					filter = Operation.dupExpression(filter, ctx);
					cursors[tableCount] = memoryTable.cursor(exps, fields, filter, fkNames, codes, opts, ctx);
				}
				
				cursors[0] = cs;
				for (int i = 1; i < tableCount; ++i) {
					exps = Operation.dupExpressions(exps, ctx);
					filter = Operation.dupExpression(filter, ctx);
					cursors[i] = tables[i].cursor(exps, fields, filter, fkNames, codes, opts, opt, ctx);
				}
				
				String []sortFields = cursors[0].getSortFields();
				DataStruct ds = cursors[0].getDataStruct();
				if (ds == null || sortFields == null) {
					return new ConjxCursor(cursors);
				}
				
				int fcount = sortFields.length;
				int []findex = new int[fcount];
				for (int i = 0; i < fcount; ++i) {
					findex[i] = ds.getFieldIndex(sortFields[i]);
				}
				
				return new MergeCursor(cursors, findex, null, ctx);
			}
		}
		
		// 把每个文件分成pathCount路，然后所有的i路合成一个游标，最后再组成多路游标
		for (int i = 0; i < pathCount; ++i) {
			lists[i] = new ArrayList<ICursor>(tableCount);
		}
		
		if (cs instanceof MultipathCursors) {
			MultipathCursors mcs = (MultipathCursors)cs;
			ICursor []cursors = mcs.getCursors();
			for (int c = 0; c < cursors.length; ++c) {
				lists[c].add(cursors[c]);
			}
		} else {
			lists[0].add(cs);
		}
		
		for (int i = 1; i < tableCount; ++i) {
			cs = tables[i].cursor(exps, fields, filter, fkNames, codes, opts, pathCount, opt, ctx);
			if (cs instanceof MultipathCursors) {
				MultipathCursors mcs = (MultipathCursors)cs;
				ICursor []cursors = mcs.getCursors();
				for (int c = 0; c < cursors.length; ++c) {
					lists[c].add(cursors[c]);
				}
			} else {
				lists[0].add(cs);
			}
		}
		
		if (memoryTable != null) {
			int len = memoryTable.length();
			int blockSize = len / pathCount;
			int start = 1;
			
			for (int i = 1; i <= pathCount; ++i) {
				int end;
				if (i == pathCount) {
					end = len + 1;
				} else {
					end = blockSize * i + 1;
				}

				exps = Operation.dupExpressions(exps, ctx);
				filter = Operation.dupExpression(filter, ctx);
				ICursor cursor = memoryTable.cursor(start, end, exps, fields, filter, fkNames, codes, opts, ctx);
				lists[i].add(cursor);
				start = end;
			}
		}
		
		ArrayList<ICursor> list = new ArrayList<ICursor>(pathCount);
		for (int i = 0; i < pathCount; ++i) {
			int size = lists[i].size();
			if (size > 1) {
				ICursor []cursors = new ICursor[size];
				lists[i].toArray(cursors);
				list.add(new ConjxCursor(cursors));
			} else if (size == 1) {
				list.add(lists[i].get(0));
			}
		}
		
		int size = list.size();
		if (size == 0) {
			return null;
		} else if (size == 1) {
			return list.get(0);
		} else {
			ICursor []cursors = new ICursor[size];
			list.toArray(cursors);
			return new MultipathCursors(cursors, ctx);
		}
	}
	
	public ICursor cursor(Expression []exps, String []fields, Expression filter, 
			String []fkNames, Sequence []codes, String[] opts, int segSeq, int segCount, String opt, Context ctx) {
		if (segCount < 2) {
			return cursor(exps, fields, filter, fkNames, codes, opts, opt, ctx);
		}
		
		int count = tables.length;
		ArrayList<ICursor> list = new ArrayList<ICursor>(count);
		for (int i = 0; i < count; ++i) {
			exps = Operation.dupExpressions(exps, ctx);
			filter = Operation.dupExpression(filter, ctx);
			ICursor cursor = tables[i].cursor(exps, fields, filter, fkNames, codes, opts, segSeq, segCount, opt, ctx);
			if (cursor != null) {
				list.add(cursor);
			}
		}
		
		if (memoryTable != null) {
			int len = memoryTable.length();
			int blockSize = len / segCount;
			int start;
			int end;
			
			if (segSeq == segCount) {
				start = blockSize * (segSeq - 1) + 1;
				end = len + 1;
			} else {
				start = blockSize * (segSeq - 1) + 1;
				end = blockSize * segSeq + 1;
			}
			
			exps = Operation.dupExpressions(exps, ctx);
			filter = Operation.dupExpression(filter, ctx);
			ICursor cursor = memoryTable.cursor(start, end, exps, fields, filter, fkNames, codes, opts, ctx);
			list.add(cursor);
		}
		
		ICursor []cursors = new ICursor[list.size()];
		list.toArray(cursors);
		
		if (opt == null || opt.indexOf('o') == -1) {
			String []sortFields = cursors[0].getSortFields();
			DataStruct ds = cursors[0].getDataStruct();
			if (ds == null || sortFields == null) {
				return new ConjxCursor(cursors);
			}
			
			int fcount = sortFields.length;
			int []findex = new int[fcount];
			for (int i = 0; i < fcount; ++i) {
				findex[i] = ds.getFieldIndex(sortFields[i]);
			}
			
			return new MergeCursor(cursors, findex, null, ctx);
		} else {
			return new ConjxCursor(cursors);
		}
	}
	
	public ICursor cursor(Expression []exps, String []fields, Expression filter, 
			String []fkNames, Sequence []codes, String[] opts, int pathSeq, int pathCount, int pathCount2, String opt, Context ctx) {
		throw new RQException("'mcursor' function is unimplemented in file group!");
	}
	
	public ICursor cursor(Expression []exps, String []fields, Expression filter, 
			String []fkNames, Sequence []codes, String[] opts, ICursor cs, int seg, Object [][]endValues, Context ctx) {
		throw new RQException("'mcursor' function is unimplemented in file group!");
	}
	
	public Table finds(Sequence values) throws IOException {
		Table result = null;
		for (IPhyTable table : tables) {
			Table cur = table.finds(values);
			if (cur != null) {
				if (result == null) {
					result = cur;
				} else {
					result.append(cur, null);
				}
			}
		}
		
		if (opt == null || opt.indexOf('o' ) == -1) {
			String []sortFields = getAllSortedColNames();
			result.sortFields(sortFields);
		}
		
		return result;
	}
	
	public Table finds(Sequence values, String []selFields) throws IOException {
		Table result = null;
		for (IPhyTable table : tables) {
			Table cur = table.finds(values, selFields);
			if (cur != null) {
				if (result == null) {
					result = cur;
				} else {
					result.append(cur, null);
				}
			}
		}
		return result;
	}
	
	public ICursor icursor(String []fields, Expression filter, Object iname, String opt, Context ctx) {
		boolean sort = true;
		int[] sortFields = null;
		int count = tables.length;
		ArrayList<ICursor> list = new ArrayList<ICursor>(count);
		for (int i = 0; i < count; ++i) {
			ICursor cursor = tables[i].icursor(fields, filter, iname, opt, ctx);
			if (cursor != null) {
				list.add(cursor);
				if (sort) {
					if (cursor instanceof IndexCursor) {
						sortFields = ((IndexCursor)cursor).getSortFieldsIndex();
						if (sortFields == null) {
							sort = false;
						}
					}  else if (cursor instanceof IndexFCursor) {
						sortFields = ((IndexFCursor)cursor).getSortFieldsIndex();
						if (sortFields == null) {
							sort = false;
						}
					} else if (cursor instanceof Cursor) {
						//有可能返回Cursor，此时无序
						sort = false;
					} else if (cursor instanceof ConjxCursor) {
						sort = false;
					} else if (cursor instanceof MergeCursor2) {
						sortFields = ((MergeCursor2)cursor).getFields();
						if (sortFields == null) {
							sort = false;
						}
					}
				}
			}
		}

		ICursor []cursors = new ICursor[list.size()];
		list.toArray(cursors);

		if (sort) {
			return new MergeCursor(cursors, sortFields, null, ctx);
		} else {
			return new ConjxCursor(cursors);
		}
	}

	/**
	 * 取主键字段名（含主表）
	 * @return 主键字段名数组
	 */
	public String[] getAllKeyColNames() {
		return tables[0].getAllKeyColNames();
	}
	
	/**
	 * 返回排序字段名（含主表）
	 * @return 排序字段名数组
	 */
	public String[] getAllSortedColNames() {
		return tables[0].getAllSortedColNames();
	}
	
	/**
	 * 返回所有列。（含主表key列）
	 * @return
	 */
	public String[] getAllColNames() {
		return tables[0].getAllColNames();
	}
	
	public boolean deleteIndex(String indexName) throws IOException {
		boolean result = true;
		for (IPhyTable table : tables) {
			if (!table.deleteIndex(indexName)) {
				result = false;
			}
		}
		
		return result;
	}
	
	public void createIndex(String I, String []fields, Object obj, String opt, Expression w, Context ctx) {
		for (IPhyTable table : tables) {
			table.createIndex(I, fields, obj, opt, w, ctx);
		}
	}
	
	// 取分布表达式串
	public String getDistribute() {
		return tables[0].getDistribute();
	}

	public void addColumn(String colName, Expression exp, Context ctx) {
		for (IPhyTable table : tables) {
			table.addColumn(colName, exp, ctx);
		}
	}

	public void deleteColumn(String colName) {
		for (IPhyTable table : tables) {
			table.deleteColumn(colName);
		}
	}
	
	/**
	 * 按排序字段归并分表的数据返回成游标
	 * @param ctx 计算上下文
	 * @return 游标
	 */
	public ICursor merge(Context ctx) {
		String []sortFields = tables[0].getAllSortedColNames();
		if (sortFields == null || sortFields.length == 0) {
			return cursor();
		}
		
		int count = tables.length;
		ICursor []cursors = new ICursor[count];
		for (int i = 0; i < count; ++i) {
			cursors[i] = tables[i].cursor();
		}
		
		// 排序字段在前面
		int fcount = sortFields.length;
		int []fields = new int[fcount];
		for (int i = 0; i < fcount; ++i) {
			fields[i] = i;
		}
		
		return new MergeCursor(cursors, fields, null, ctx);
	}
	

	public IPhyTable[] getTables() {
		return tables;
	}
	
	/**
	 * 使用预分组进行分组计算
	 * @param exps 分组表达式
	 * @param names 分组表达式新名字
	 * @param newExps 汇总表达式
	 * @param newNames 汇总表达式新名字
	 * @param srcTable 要分组的组表
	 * @param w 过滤条件
	 * @param hasM 是否并行
	 * @param n 并行数
	 * @param option
	 * @param ctx
	 * @return
	 */
	public Sequence cgroups(String []expNames, String []names, String []newExpNames, String []newNames,
			Expression w, boolean hasM, int n, String option, FileObject[] files, Context ctx) {
			int count = tables.length;
			
			//把所有表的结果放到一起
			Sequence result = Cuboid.cgroups(expNames, names, newExpNames, newNames, 
					(PhyTable) tables[0], w, hasM, n, option, files, ctx);
			for (int i = 1; i < count; ++i) {
				Sequence seq = Cuboid.cgroups(expNames, names, newExpNames, newNames, 
						(PhyTable) tables[i], w, hasM, n, option, files, ctx);
				result.addAll(seq);
			}
			
			//组织分组表达式
			count = names.length;
			Expression exps[] = new Expression[count];
			for (int i = 0; i < count; i++) {
				exps[i] = new Expression(names[i]);
			}
			
			//组织再聚合表达式
			Expression newExps[] = new Expression[count = newNames.length];//用来存储要聚合的原字段
			for (int i = 0, len = count; i < len; i++) {
				String str = newExpNames[i];
				//对count再聚合，要变为累加
				if (str.indexOf("count(") != -1) {
					str = str.replaceFirst("count", "sum");
				}
				
				//再聚合时要替换一下字段名
				String sub = str.substring(str.indexOf('(') + 1, str.indexOf(')'));
				str = str.replaceAll(sub, "'" + newNames[i] + "'");
				newExps[i] = new Expression(str);
			}
			return result.groups(exps, names, newExps, newNames, option, ctx);
	}

	public int getDeleteFieldIndex(Expression[] exps, String[] fields) {
		return tables[0].getDeleteFieldIndex(exps, fields);
	}

	/**
	 * 取内存分表
	 * @return Sequence
	 */
	public Sequence getMemoryTable() {
		return memoryTable;
	}

	/**
	 * 给复组表添加内存分表，用于历史数据和实时数据混合计算
	 * @param memoryTable 内心序表
	 */
	public void setMemoryTable(Sequence memoryTable) {
		this.memoryTable = memoryTable;
	}

	public void append(PhyTable table) throws IOException {
		throw new RuntimeException();
	}
}