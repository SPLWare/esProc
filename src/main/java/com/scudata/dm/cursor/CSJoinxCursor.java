package com.scudata.dm.cursor;

import java.util.ArrayList;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.*;
import com.scudata.dm.op.Derive;
import com.scudata.dm.op.DiffJoin;
import com.scudata.dm.op.FilterJoin;
import com.scudata.dm.op.Join;
import com.scudata.dm.op.New;
import com.scudata.dm.op.Operation;
import com.scudata.expression.Expression;
import com.scudata.resources.EngineMessage;
import com.scudata.util.CursorUtil;
import com.scudata.util.Variant;

/**
 * 游标joinx类，（不是归并）
 * 游标与一个可分段集文件f或实表T做join运算，f/T可以有多组
 * 把游标按照f/T的分段信息写出到若干临时文件
 * @author 
 *
 */
public class CSJoinxCursor extends ICursor {
	public static final String SEQ_FIELDNAME = "rq_csjoinx_seq_";
	private boolean hasU;//true:不保持原序
	private boolean hasO;//true:不保持原序
	private boolean isEnd;
	private boolean hasSeq = false;//是否追加过序号字段
	private boolean needOrgName = false;//是否用fname命名
	private int orgFieldsCount;//fname原记录的字段数
	private int seqIndex;//序号位置
	private ICursor srcCursor;//源游标
	private SyncReader []fileOrTable;//维表
	private Expression [][]fields;//事实表字段
	private Expression [][]keys;//维表字段
	private Expression [][]newExps;//新的表达式
	private String [][]newNames;//新的表达式名字
	private String option;
	private String fname;
	private int n;//缓冲区条数
	
	//用于计算
	private transient ICursor cursor;//维表游标
	private transient int fileIndex = 0;
	private transient int fileSize;
	private transient ICursor fileCursor;//临时文件游标
	private transient ICursor []fileCursors;//临时文件游标数组
	private transient Sequence []table;//当前数据
	private transient ICursor sortCursor;
	
	//用于最后一次计算
	private SyncReader lastReader;
	private transient Expression [][]lastFields;//事实表字段
	private transient Expression [][]lastKeys;//维表字段
	private transient Expression [][]lastNewExps;//新的表达式
	private transient String [][]lastNewNames;//新的表达式名字

	/**
	 * 
	 * @param cursor 		事实表源游标
	 * @param fileOrTable	维表
	 * @param fields		事实表字段
	 * @param keys			维表字段
	 * @param exps			新的表达式
	 * @param names			新的表达式的字段名
	 * @param ctx
	 * @param option
	 * @param n				维表每段的缓冲区大小
	 */
	public CSJoinxCursor(ICursor cursor, SyncReader []fileOrTable, Expression [][]fields, Expression [][]keys, 
			Expression [][]exps, String [][]names, String fname, Context ctx, String option, int n) {
		srcCursor = cursor;
		this.fileOrTable = fileOrTable;
		this.fields = fields;
		this.keys = keys;
		this.newExps = exps;
		this.newNames = names;
		this.ctx = ctx;
		this.option = option;
		this.n = n;
		this.fname = fname;
		
		//如果newNames里有null，则用newExps替代
		for (int i = 0, len = newExps.length; i < len; i++) {
			String[] arr = newNames[i];
			for (int j = 0, len2 = arr.length; j < len2; j++) {
				if (arr[j] == null) {
					arr[j] = newExps[i][j].getFieldName();
				}
			}
		}
		
		if (option != null) {
			if (option.indexOf('u') != -1) hasU = true;
			if (option.indexOf('o') != -1) hasO = true;
		}
		
		table = new Sequence[1];
	}

	/**
	 * 并行计算时需要改变上下文
	 * 继承类如果用到了表达式还需要用新上下文重新解析表达式
	 */
	public void resetContext(Context ctx) {
		if (this.ctx != ctx) {
			newExps = Operation.dupExpressions(newExps, ctx);
			super.resetContext(ctx);
		}
	}

	private void init() {
		ICursor cs = srcCursor;
		int fcount = fileOrTable.length;
		
		if (hasO) {
			if (fcount > 1 || !hasU) {
				//如果多轮或者没有@u
				option = option.replace("o", "");
				needOrgName = true;
			}
		}
		
		//backup oplist
		ArrayList<Operation> opListBk = opList;
		opList = null;
		
		for (int i = 0; i < fcount; i++) {
			Sequence values = fileOrTable[i].getValues();//每段的首条值

			boolean needSeq = hasU ? false : i == 0;
			if (needSeq && (!hasSeq)) {
				hasSeq = true;
				Derive newOp = new Derive( new Expression[]{new Expression("0L")}, new String[]{CSJoinxCursor.SEQ_FIELDNAME}, null);
				cs.addOperation(newOp, ctx);
				Sequence seq = cs.peek(1);
				if (seq == null || seq.length() == 0) {
					isEnd = true;
					return;
				}
				seqIndex = seq.dataStruct().getFieldCount() - 1;//保存序号的位置
			}
			
			if (needOrgName && i == 0) {
				//如果需要改fname
				if (hasSeq) {
					orgFieldsCount = seqIndex;
				} else {
					Sequence seq = cs.peek(1);
					if (seq == null || seq.length() == 0) {
						isEnd = true;
						return;
					}
					orgFieldsCount = seq.dataStruct().getFieldCount();
				}
			}
			
			boolean hasData = cursorToFiles(cs, fields[i], values, needSeq);
			if (!hasData) {
				isEnd = true;
				return;
			}
			
			//进行join
			FileObject tempFile = null;
			BFileCursor fCursor = null;
			try {
				if (i == fcount - 1) {
					fileIndex = 0;
					fileSize = fileCursors.length;
					lastReader = fileOrTable[i];
					lastFields = new Expression[][] { this.fields[i] };
					lastKeys = new Expression[][] { this.keys[i] };
					lastNewExps = new Expression[][] { this.newExps[i] };
					lastNewNames = new String[][] { this.newNames[i] };
					fileCursor = null;
					if (!hasU) {
						processSeqField(lastNewNames[0]);
						sortCursor = sortBySeq();
						if (sortCursor == null) {
							isEnd = true;
							return;
						} else {
							isEnd = false;
						}
					} else {
						if (needOrgName) {
							sortCursor = toFileCursor();
						}
					}
					opList = opListBk;
					return;//最后一个不在这里做join，这样可以少写一次
				}
				
				tempFile = FileObject.createTempFileObject();
				cs = new BFileCursor(tempFile, null, "x", ctx);
				tempFile.setFileSize(0);
				Sequence[] seqs = new Sequence[1];
				Expression[][] fields = new Expression[][] { this.fields[i] };
				Expression[][] keys = new Expression[][] { this.keys[i] };
				Expression[][] newExps = new Expression[][] { this.newExps[i] };
				String[][] newNames = new String[][] { this.newNames[i] };
				
				boolean hasNewExps = false;
				if (newExps[0] != null && newExps[0].length > 0) {
					hasNewExps = true;
				}
				boolean isIsect = false, isDiff = false;
				if (!hasNewExps && option != null) {
					if (option.indexOf('i') != -1) {
						isIsect = true;
					} else if (option.indexOf('d') != -1) {
						isDiff = true;
					}
				}
				
				for (int j = 0, filesCount = fileCursors.length; j < filesCount; j++) {
					ICursor c = fileCursors[j];
					if (c == null) {
						fileOrTable[i].getData(j);
						continue;
					}
					seqs[0] = fileOrTable[i].getData(j);
					fCursor = (BFileCursor) c;
					
					Operation op;
					if (isIsect) {
						op = new FilterJoin(null, fields, seqs, keys, option);
					} else if (isDiff) {
						op = new DiffJoin(null, fields, seqs, keys, option);
					} else {
						op = new Join(null, fields, seqs, keys, newExps, newNames, option);
					}
					fCursor.addOperation(op, ctx);

					Sequence table = fCursor.fetch(FETCHCOUNT);
					while (table != null && table.length() != 0) {
						tempFile.exportSeries(table, "ab", null);
						table = fCursor.fetch(FETCHCOUNT);
					}
				} 
			} catch (Exception e) {
				if (fCursor != null) {
					fCursor.close();
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
			
			if (tempFile.size() == 0) {
				tempFile.delete();
				isEnd = true;
				return;
			}
		}
	}
	
	/**
	 * 读取一段临时文件的数据出来（带join操作）
	 * @return
	 */
	private boolean loadFile() {
		boolean hasNewExps = false;
		if (lastNewExps[0] != null && lastNewExps[0].length > 0) {
			hasNewExps = true;
		}
		boolean isIsect = false, isDiff = false;
		if (!hasNewExps && option != null) {
			if (option.indexOf('i') != -1) {
				isIsect = true;
			} else if (option.indexOf('d') != -1) {
				isDiff = true;
			}
		}
		
		while (fileIndex < fileSize) {
			if (fileCursors[fileIndex] == null) {
				lastReader.getData(fileIndex);
				fileIndex++;
				continue;
			}
			table[0] = null;
			table[0] = lastReader.getData(fileIndex);
			fileCursor = fileCursors[fileIndex];
			
			Operation op;
			if (isIsect) {
				op = new FilterJoin(null, lastFields, table, lastKeys, option);
			} else if (isDiff) {
				op = new DiffJoin(null, lastFields, table, lastKeys, option);
			} else {
				op = new Join(fname, lastFields, table, lastKeys, lastNewExps, lastNewNames, option);
			}
			fileCursor.addOperation(op, ctx);
			fileIndex++;
			return true;
		}
		isEnd = true;
		return false;
		
	}
	
	protected Sequence get(int n) {
		if (fileCursors == null && sortCursor == null) {
			init();
		}
		if (isEnd || n < 1) return null;
		
		if (sortCursor != null) {
			if (needOrgName) {
				Sequence data = sortCursor.fetch(n);
				if (data == null || data.length() == 0) {
					return null;
				}
				DataStruct ds = data.dataStruct();
				DataStruct newDs;
				DataStruct subDs;
				String []field = ds.getFieldNames();
				String []newField = new String[field.length + 1 - orgFieldsCount];
				String []subField = new String[orgFieldsCount];
				
				newField[0] = fname;
				System.arraycopy(field, orgFieldsCount, newField, 1, newField.length - 1);
				System.arraycopy(field, 0, subField, 0, orgFieldsCount);
				newDs = new DataStruct(newField);
				subDs = new DataStruct(subField);
				
				int newFieldLen = newField.length;
				int subFieldLen = orgFieldsCount;
				int len = data.length();
				Sequence result = new Sequence(len);
				for (int i = 1; i <= len; ++i) {
					Record rec = (Record) data.get(i);
					Record newRec = new Record(newDs);
					Record subRec = new Record(subDs);
					for (int j = 0; j < subFieldLen; j++) {
						subRec.setNormalFieldValue(j, rec.getNormalFieldValue(j));
					}
					for (int j = 0; j < newFieldLen - 1; j++) {
						newRec.setNormalFieldValue(j + 1, rec.getNormalFieldValue(j + subFieldLen));
					}
					newRec.setNormalFieldValue(0, subRec);
					result.add(newRec);
				}
				return result;
			} else {
				return sortCursor.fetch(n);
			}
		}
		

		Sequence newTable = null;
		if (fileCursor == null) {
			if (!loadFile()) {
				return null;
			}
		}

		Sequence data = null;
		int count = 0;
		try {
			while (count < n) {
				data = fileCursor.fetch(n - count);
				
				if (data == null || data.length() == 0) {
					fileCursor.close();
					if (!loadFile()) {
						return newTable;
					}
				} else {
					if (newTable == null) {
						newTable = data;
					} else {
						newTable.addAll(data);
					}
					count += data.length();
				}

			} 
		} catch (Exception e) {
			close();
			if (e instanceof RQException) {
				throw (RQException)e;
			} else {
				throw new RQException(e.getMessage(), e);
			}
		}
		if (newTable.length() > 0) {
			return newTable;
		} else {
			return null;
		}
	}

	protected long skipOver(long n) {
		if (sortCursor != null) {
			return sortCursor.skip(n);
		}
		if (isEnd || n < 1) return 0;

		if (fileCursor == null) {
			if (!loadFile()) {
				return 0;
			}
		}

		long count = 0;
		while(count < n) {
			long c = fileCursor.skip(n - count);
			if (c == 0) {
				if (!loadFile()) {
					return count;
				}
			} else {
				count += c;
			}
			
		}
		
		return count;
	}

	public synchronized void close() {
		super.close();
		if (fileCursor != null) {
			fileCursor.close();
			fileCursor = null;
		}
		
		fileCursors = null;
		
		if (sortCursor != null) {
			sortCursor.close();
			sortCursor = null;
		}
		
		if (cursor != null) {
			cursor.close();
		}
		
		srcCursor.close();
		isEnd = true;
	}
	
	/**
	 * 重置游标
	 * @return 返回是否成功，true：游标可以从头重新取数，false：不可以从头重新取数
	 */
	public boolean reset() {
		super.close();
		if (fileCursor != null) {
			fileCursor.close();
		}
		
		if (cursor != null) {
			cursor.close();
		}
		
		if (sortCursor != null) {
			sortCursor.close();
			sortCursor = null;
		}
		
		srcCursor.reset();
		isEnd = false;
		init();
		return true;
	}
	
	/**
	 * 把游标的数据写出到临时文件
	 * @param cursor 游标
	 * @param fields 写出字段
	 * @param values 分段信息
	 * @param needSeq 是否保持序号信息（为了实现原序）
	 * @return
	 */
	private boolean cursorToFiles(ICursor cursor, Expression []fields, Sequence values, boolean needSeq) {
		final int fetchCount = n;
		Sequence table = cursor.fetch(fetchCount);
		if (table == null || table.length() == 0) {
			return false;
		}
		DataStruct ds = table.dataStruct();
		setDataStruct(ds);
		int seqIndex = this.seqIndex;

		//获取字段index
		int fcount = fields.length;
		int []findex = new int[fcount];
		for (int f = 0; f < fcount; ++f) {
			findex[f] = ds.getFieldIndex(fields[f].toString());
			if (findex[f] == -1) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(fields[f] + mm.getMessage("ds.fieldNotExist"));
			}
		}
		
		Object []curVals = new Object[fcount];
		int segCount = values.length();
		Sequence []outSeqs = new Sequence[segCount + 1];
		FileObject []files = new FileObject[segCount + 1];
		fileCursors = new BFileCursor[segCount + 1];
		for (int i = 0; i <= segCount; i++) {
			files[i] = FileObject.createTempFileObject();
			files[i].setFileSize(0);
			fileCursors[i] = new BFileCursor(files[i], null, "x", ctx);
			outSeqs[i] = new Sequence();
		}
		
		long seq = 0;
		try {
			while (table != null && table.length() > 0) {
				//遍历取出来的记录，送到每个临时outSeq里
				int len = table.length();
				for (int i = 1; i <= len; ++i) {
					BaseRecord record = (BaseRecord) table.getMem(i);
					for (int f = 0; f < fcount; ++f) {
						curVals[f] = record.getNormalFieldValue(findex[f]);
					}
					
					if (needSeq) {
						record.setNormalFieldValue(seqIndex, ++seq);
					}
					
					int low = 1, high = segCount, middle = 0;
					if (segCount != 0) {
						while (low <= high) {
							middle = (low + high) / 2;
							Object[] vals = (Object[]) values.getMem(middle);
							int cmp = Variant.compareArrays(vals, curVals);
							if(cmp > 0) {
								high = middle - 1;
							} else if(cmp < 0) {
								low = middle + 1;
							} else {
								break;
							}
						}
						Object[] vals = (Object[]) values.getMem(middle);
						if (Variant.compareArrays(vals, curVals) > 0) middle--;
					} else {
						middle = 0;
					}
					outSeqs[middle].add(record);
					record = null;
					
					
				}
				
				//把outSeqs里的记录写到文件
				for (int i = 0; i <= segCount; i++) {
					files[i].exportSeries(outSeqs[i], "ab", null);
					outSeqs[i].clear();
				}
				table = null;
				table = cursor.fetch(fetchCount);

			}
		} catch (Exception e) {
			if (e instanceof RQException) {
				throw (RQException)e;
			} else {
				throw new RQException(e.getMessage(), e);
			}
		}
		
		for (int i = 0; i <= segCount; i++) {
			FileObject f = files[i];
			if (f.size() == 0) {
				fileCursors[i].close();
				files[i] = null;
				fileCursors[i] = null;
			}
		}
		return true;
	}

	private ICursor toFileCursor() {
		ICursor cursor = this;
		Sequence table = cursor.fetch(ICursor.FETCHCOUNT);
		if (table == null || table.length() == 0) {
			return null;
		}

		FileObject file = FileObject.createTempFileObject();
		try {
			while (table != null && table.length() > 0) {
				file.exportSeries(table, "ab", null);
				table = cursor.fetch(ICursor.FETCHCOUNT);
			}
		} catch (Exception e) {
			if (e instanceof RQException) {
				throw (RQException)e;
			} else {
				throw new RQException(e.getMessage(), e);
			}
		}
		return new BFileCursor(file, null, "x", ctx);
	}

	private void processSeqField(String []newNames) {
		String []oldNames = dataStruct.getFieldNames();
		int oldColCount = oldNames.length;
		
		// 合并新字段
		int newColCount = oldColCount + newNames.length;
		String []totalNames = new String[newColCount];
		System.arraycopy(oldNames, 0, totalNames, 0, oldColCount);
		System.arraycopy(newNames, 0, totalNames, oldColCount, newNames.length);
		setDataStruct(dataStruct.create(totalNames));

	}
	
	private ICursor sortBySeq() {
		int seqIndex = this.seqIndex;
		DataStruct ds = getDataStruct();
		
		//做排序
		Expression []exps = new Expression[]{new Expression(CSJoinxCursor.SEQ_FIELDNAME)};
		ICursor cs = CursorUtil.sortx(this, exps, ctx, this.n, null);
		
		//去掉序号
		String []oldFields = ds.getFieldNames();
		int oldLen = oldFields.length;
		int newLen = oldLen - 1;
		String []newFields = new String[newLen];
		Expression []expressions = new Expression[newLen];
		int c = 0;
		for (int i = 0; i < oldLen; i++) {
			if (i == seqIndex) {
				continue;
			} else {
				expressions[c] = new Expression(ctx, "#" + (i + 1));
				newFields[c] = oldFields[i];
			}
			c++;
		}
		New newOp = new New(expressions, newFields, null);
		if (cs != null) cs.addOperation(newOp, ctx);
		return cs;
	}
	
	protected void finalize() throws Throwable {
		close();
	}
}
