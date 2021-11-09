package com.raqsoft.dw;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.DataStruct;
import com.raqsoft.dm.Env;
import com.raqsoft.dm.FileObject;
import com.raqsoft.dm.ListBase1;
import com.raqsoft.dm.LongArray;
import com.raqsoft.dm.ObjectReader;
import com.raqsoft.dm.ObjectWriter;
import com.raqsoft.dm.RandomObjectWriter;
import com.raqsoft.dm.RandomOutputStream;
import com.raqsoft.dm.Record;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.Table;
import com.raqsoft.dm.cursor.BFileCursor;
import com.raqsoft.dm.cursor.ICursor;
import com.raqsoft.dm.op.Select;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.Node;
import com.raqsoft.expression.fn.string.Like;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.util.EnvUtil;

/**
 * 全文索引
 * @author runqian
 *
 */
public class TableFulltextIndex extends TableMetaDataIndex {
	private static final int LIMIT = 1000000;//高频词限制数，超过这个值就抛弃
	
	public TableFulltextIndex(TableMetaData table, FileObject indexFile) {
		super(table, indexFile);
	}
	
	public TableFulltextIndex(TableMetaData table, String indexName) {
		super(table, indexName);
	}
	
	protected void writeHeader(ObjectWriter writer) throws IOException {
		writer.write('r');
		writer.write('q');
		writer.write('d');
		writer.write('w');
		writer.write('i');
		writer.write('d');
		writer.write('w');//模糊查询索引

		writer.write(new byte[32]);
		writer.writeLong64(recordCount);
		writer.writeLong64(index1EndPos);
		writer.writeLong64(index1RecordCount);
		writer.writeLong64(rootItPos);// 指向root1 info 开始位置
		writer.writeLong64(rootItPos2);// 指向root2 info 开始位置
		writer.writeLong64(indexPos);// 1st index 开始位置
		writer.writeLong64(indexPos2);// second index 开始位置
		
		writer.writeStrings(ifields);
		if (filter != null) {
			writer.write(1);
			writer.writeUTF(filter.toString());
		} else {
			writer.write(0);
		}
	}
	
	protected void readHeader(ObjectReader reader) throws IOException {
		if (reader.read() != 'r' || reader.read() != 'q' || 
				reader.read() != 'd' || reader.read() != 'w' ||
				reader.read() != 'i' || reader.read() != 'd' || reader.read() != 'w') {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("license.fileFormatError"));
		}

		reader.readFully(new byte[32]);
		recordCount = reader.readLong64();
		index1EndPos = reader.readLong64();
		index1RecordCount = reader.readLong64();
		rootItPos = reader.readLong64();
		rootItPos2 = reader.readLong64();
		indexPos = reader.readLong64();
		indexPos2 = reader.readLong64();
		reader.readStrings();//ifields
		
		if (reader.read() != 0) {
			filter = new Expression(reader.readUTF());
		} else {
			filter = null;
		}
	}
	
	/**
	 * 检查字符key出现的次数
	 * @param strCounters
	 * @param key
	 * @return
	 */
	private boolean checkStringCount(HashMap<String, Long> strCounters, String key) {
		
		if (strCounters.containsKey(key)) {
			Long  cnt = strCounters.get(key) + 1;
			if (cnt >= LIMIT) {
				return true;
			}
			strCounters.put(key, cnt);
		} else {
			strCounters.put(key, (long) 1);
		}
		return false;
	}
	
	private boolean checkAlpha(char word) {
		if (word >= '0' && word <= 'z') {
			return true;
		}
		return false;
	}
	
	protected ArrayList <ICursor> sort(String []fields, Context ctx, Expression filter) {
		ICursor srcCursor;
		boolean isColTable = srcTable instanceof ColumnTableMetaData;
		if (isColTable) {
			srcCursor = new CTableCursor(srcTable, fields, ctx, filter);
		} else {
			srcCursor = new RTableCursor(srcTable, fields, ctx, filter);
		}
		try {
			int icount = fields.length;
			DataStruct ds = srcTable.getDataStruct();
			ifields = new String[icount];
			boolean isPrimaryTable = srcTable.parent == null;
			String []keyNames = srcTable.groupTable.baseTable.getSortedColNames();
			ArrayList<String> list = new ArrayList<String>();
			if (keyNames != null) {
				for (String name : keyNames) {
					list.add(name);
				}
			}
			
			//check field
			for (int i = 0; i < icount; ++i) {
				int id = ds.getFieldIndex(fields[i]);
				if (id == -1) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(fields[i] + mm.getMessage("ds.fieldNotExist"));
				}
				
				if(!isPrimaryTable) {
					if (list.contains(fields[i])) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(fields[i] + mm.getMessage("ds.fieldNotExist"));
					}
				}
				
				ifields[i] = fields[i];
			}

			Runtime rt = Runtime.getRuntime();
			int baseCount = 100000;//每次取出来的条数
			boolean flag = false;//是否调整过临时文件大小
			HashMap<String, Long> strCounters = new HashMap<String, Long>();//记录每个字符出现的次数
			int fieldsCount;
			if (isColTable) {
				fieldsCount = ((CTableCursor)srcCursor).ds.getFieldCount();
			} else {
				fieldsCount = ((RTableCursor)srcCursor).ds.getFieldCount();
			}
			
			ArrayList <ICursor>cursorList = new ArrayList<ICursor>();
			Sequence table;
			Table subTable;
			int []sortFields = new int[icount + 1];
			for (int i = 0; i < icount + 1; ++i) {
				sortFields[i] = i;
			}

			if (index1EndPos > 0) {
				if (srcTable instanceof ColumnTableMetaData) {
					((CTableCursor)srcCursor).seek(index1EndPos);
				} else {
					((RTableCursor)srcCursor).seek(index1EndPos);
				}
			}
			
			while (true) {
				if (isColTable) {
					table = (Sequence) ((CTableCursor)srcCursor).get(baseCount);
				} else {
					table = (Sequence) ((RTableCursor)srcCursor).get(baseCount);
				}
				if (table == null) break;
				if (table.length() <= 0) break;
				recordCount += table.length();

				ds = table.dataStruct();
				subTable = new Table(ds);
				ListBase1 mems = table.getMems();
				int length = table.length();
				for (int i = 1; i <= length; i++) {
					Record r = (Record) mems.get(i);
					Object []objs = r.getFieldValues();
					String ifield = (String) objs[0];
					
					list.clear();//用于判断重复的字符，例如"宝宝巴士"，重复的"宝"字不能被重复索引
					int strLen = ifield.length();
					for (int j = 0; j < strLen; j++) {
						char ch1 = ifield.charAt(j);
						if (ch1 == ' ') {
							continue;//空格
						}
						
						if (checkAlpha(ch1)) {
							//英文要连续取3个、4个字母
							if (j + 2 < strLen) {
								char ch2 = ifield.charAt(j + 1);
								char ch3 = ifield.charAt(j + 2);
								if (checkAlpha(ch2) && checkAlpha(ch3)) {
									Object []vals = new Object[fieldsCount];
									for (int f = 1; f < fieldsCount; f++) {
										vals[f] = objs[f];
									}
									String str3 = new String("" + ch1 + ch2 + ch3);
									if (!list.contains(str3) && !checkStringCount(strCounters, str3)) {
										vals[0] = str3;
										subTable.newLast(vals);
										list.add(str3);
									}
									
									if (j + 3 < strLen) {
										char ch4 = ifield.charAt(j + 3);
										if (checkAlpha(ch4)) {
											String str4 =  new String(str3 + ch4);
											if (!list.contains(str4)) {
												vals = new Object[fieldsCount];
												for (int f = 1; f < fieldsCount; f++) {
													vals[f] = objs[f];
												}
												vals[0] = str4;
												subTable.newLast(vals);
												list.add(str4);
											}
										}
									}
								}
							}
						} else if (ch1 > 255) {
							String str = new String("" + ch1);
							if (list.contains(str)) {
								continue;//已经存在
							}				
							//处理计数器
							if (checkStringCount(strCounters, str)) {
								continue;
							}
							
							Object []vals = new Object[fieldsCount];
							for (int f = 1; f < fieldsCount; f++) {
								vals[f] = objs[f];
							}
							vals[0] = str;
							subTable.newLast(vals);
							list.add(str);
						}
						
					}
				}

				if (subTable != null && subTable.length() != 0) {
					subTable.sortFields(sortFields);
					FileObject tmp = FileObject.createTempFileObject();
					tmp.exportSeries(subTable, "b", null);
					BFileCursor bfc = new BFileCursor(tmp, null, "x", ctx);
					cursorList.add(bfc);
					subTable = null;
					if (!flag && tmp.size() < TEMP_FILE_SIZE) {
						baseCount = (int) (baseCount * (TEMP_FILE_SIZE / tmp.size()));
						flag = true;
					}
				}
				
				table = null;;
				EnvUtil.runGC(rt);
			}

			int size = cursorList.size();
			if (size > 1) {
				int bufSize = Env.getMergeFileBufSize(size);
				for (int i = 0; i < size; ++i) {
					BFileCursor bfc = (BFileCursor)cursorList.get(i);
					bfc.setFileBufferSize(bufSize);
				}
			}
			
			return cursorList;
		} finally {
			if (srcCursor != null) 
				srcCursor.close();
		}
	}
	
	protected ArrayList <ICursor> sortCol(String []fields, Context ctx, Expression filter) {
		return sort(fields, ctx, filter);
	}
	
	protected ArrayList <ICursor> sortRow(String []fields, Context ctx, Expression filter) {
		return sort(fields, ctx, filter);
	}
	
	/**
	 * 创建索引
	 */
	protected void createIndexTable(ICursor cursor, FileObject indexFile, boolean isAppends) {
		RandomOutputStream os = indexFile.getRandomOutputStream(true);
		RandomObjectWriter writer = new RandomObjectWriter(os);

		long perCount = MAX_LEAF_BLOCK_COUNT;

		ArrayList<Record> maxValues = new ArrayList<Record>();
		Record []rootMaxValues;// root索引块的最大值
		long []positions; // internal索引块在文件中的起始位置
		long []rootPositions; // root索引块在文件中的起始位置
		int blockCount = 0; // 索引块数
		long itPos;
		
		Context ctx = new Context();
		int icount = ifields.length;
		int posCount = this.positionCount;
		
		Expression []ifs = new Expression[icount];
		for (int i = 0; i < icount; ++i) {
			ifs[i] = new Expression("#" + (i + 1));
		}
		
		try {
			if (isAppends) {
				indexFile.setFileSize(indexPos2);
				writer.position(indexPos2);
			} else {
				writer.position(0);
				writeHeader(writer);
			}
			Sequence table;
			Record r = null;

			table = cursor.fetchGroup(ifs, LIMIT, ctx);
			if (table == null || table.length() == 0) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("index" + mm.getMessage("function.invalidParam"));
			}
			
			int p = 1;
			ListBase1 mems = table.getMems();
			int length = table.length();
			while (table != null && length != 0) {
				writer.writeInt(BLOCK_START);
				int count = 0;
				while (count < perCount) {
					int len = getGroupNum(mems, p, icount);
					count += len;
					
					r = (Record)mems.get(p);
					writer.writeInt(len);
					for (int f = 0; f < icount; ++f) {
						writer.writeObject(r.getNormalFieldValue(f));
					}
					
					for (int i = 0; i < len; ++i) {
						r = (Record)mems.get(i + p);
						writer.writeObject(r.getNormalFieldValue(icount));
						//行存时，要把地址也都写下来
						for (int j = 1; j <= posCount; ++j) {
							writer.writeObject(r.getNormalFieldValue(icount + j));
						}
					}
					p += len;
					if (p > length) {
						table = cursor.fetchGroup(ifs, LIMIT, ctx);
						if (table == null || table.length() == 0) break;
						p = 1;
						mems = table.getMems();
						length = table.length();
					}
					
				}
				blockCount++;
				maxValues.add(r);
			}

			writer.writeInt(BLOCK_END);
		
		
			
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				writer.close();
			} catch (IOException ie){};
		}

		positions = new long[blockCount];
		InputStream is = indexFile.getInputStream();
		ObjectReader reader = new ObjectReader(is, BUFFER_SIZE);
		
		try {
			readHeader(reader);
			if (isAppends) {
				reader.seek(indexPos2);
			} else {
				indexPos = reader.position();;
			}
			reader.readInt();
			for (int i = 0; i < blockCount; ++i) {
				positions[i] = reader.position();
				while (true) {
					int count = reader.readInt();
					if (count < 1) break;
					
					for (int f = 0; f < icount; ++f) {
						reader.readObject();
					}
					
					for (int j = 0; j < count; ++j) {
						reader.readLong();
						//行存时，要把地址也都跳过
						for (int c = 0; c < posCount; ++c) {
							reader.readLong();
						}
					}
				}
			}
			
			itPos = reader.position();//interBlock开始位置
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				reader.close();
			} catch (IOException ie){};
		}

		os = indexFile.getRandomOutputStream(true);
		writer = new RandomObjectWriter(os);
		
		try {
			writer.position(itPos);
			for (int i = 0; i < blockCount; ++i) {
				if (i % MAX_INTER_BLOCK_COUNT == 0) {
					if (blockCount - i >= MAX_INTER_BLOCK_COUNT) {
						writer.writeInt(MAX_INTER_BLOCK_COUNT);
					} else {
						writer.writeInt(blockCount - i);
					}
				}
				for (int f = 0; f < icount; ++f) {
					writer.writeObject(maxValues.get(i).getNormalFieldValue(f));
				}

				writer.writeLong(positions[i]);
			}
			 
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				writer.close();
			} catch (IOException ie){};
		}
		
		//获得rootMaxValues 
		int rootBlockCount = (blockCount/MAX_INTER_BLOCK_COUNT);
		rootBlockCount += (blockCount % MAX_INTER_BLOCK_COUNT==0) ? 0 : 1;
		rootMaxValues = new Record[rootBlockCount];
		for (int i = 0; i < rootBlockCount - 1; i++) {
			rootMaxValues[i] = maxValues.get((i+1)*MAX_INTER_BLOCK_COUNT-1);
		}
		rootMaxValues[rootBlockCount - 1] = maxValues.get(blockCount - 1);
		
		//获得 rootPositions
		rootPositions = new long[rootBlockCount];
		is = indexFile.getInputStream();
		reader = new ObjectReader(is, BUFFER_SIZE);
		try {
			reader.seek(itPos);//定位到internalBlock start
			for (int i = 0; i < blockCount; ++i) {
				if (i % MAX_INTER_BLOCK_COUNT == 0){
					rootPositions[i/MAX_INTER_BLOCK_COUNT] = reader.position();
					reader.readInt();
				}

				for (int f = 0; f < icount; ++f) {
					reader.readObject();
				}
				reader.readLong();

			}
			
			itPos = reader.position();
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				reader.close();
			} catch (IOException ie){};
		}
		
		if (isAppends) {
			rootItPos2 = itPos;
			recordCount = srcTable.getTotalRecordCount();
		} else {
			rootItPos = itPos;
			rootItPos2 = 0;
			recordCount = srcTable.getTotalRecordCount();
			index1RecordCount = recordCount;//1区里的记录条数，过滤后的
			index1EndPos = srcTable.getTotalRecordCount();//建立1区时源表里的条数，不是被条件过滤后的，也不包含补区
		}

		//写rootMaxValues rootPositions
		os = indexFile.getRandomOutputStream(true);
		writer = new RandomObjectWriter(os);
		try {
			writer.position(itPos);
			writer.writeInt(rootBlockCount);
			for (int i = 0; i < rootBlockCount; ++i) {
				for (int f = 0; f < icount; ++f) {
					writer.writeObject(rootMaxValues[i].getNormalFieldValue(f));
				}

				writer.writeLong(rootPositions[i]);
			}
			
			writer.writeLong64(blockCount);//internal块的总个数
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				writer.close();
			} catch (IOException ie){};
		}
		
		if (!isAppends) {
			indexPos2 = indexFile.size();
		}
		//write fileHead
		os = indexFile.getRandomOutputStream(true);
		writer = new RandomObjectWriter(os);
		try {
			writer.position(0);
			updateHeader(writer);
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			try {
				writer.close();
			} catch (IOException ie){};
		}
	}
	
	/**
	 * 查询
	 * @param exp 过滤表达式
	 * @param fields 取出字段
	 * @param opt 
	 * @param ctx
	 */
	public ICursor select(Expression exp, String []fields, String opt, Context ctx) {
		readBlockInfo(indexFile);

		int icount = ifields.length;
		if (icount == 0) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Expression.unknownExpression") + exp.toString());
		}

		Node home = exp.getHome();
		//处理like(F,"*xxx*")表达式
		while (home instanceof Like) {
			if (((Like) home).getParam().getSubSize() != 2) {
				break;
			}
			IParam sub1 = ((Like) home).getParam().getSub(0);
			IParam sub2 = ((Like) home).getParam().getSub(1);
			String f = (String) sub1.getLeafExpression().getIdentifierName();
			if (!f.equals(ifields[0])) {
				break;
			}
			
			//必须是like("*关键字*")格式的。否则按照普通的处理
			String fmtExp = (String) sub2.getLeafExpression().calculate(ctx);
			int idx = fmtExp.indexOf("*");
			if (idx != 0) {
				return srcTable.cursor(fields, exp, ctx);
			}
			
			fmtExp = fmtExp.substring(1);
			idx = fmtExp.indexOf("*");
			if (idx != fmtExp.length() - 1) {
				return srcTable.cursor(fields, exp, ctx);
			}
			
			fmtExp = fmtExp.substring(0, fmtExp.length() - 1);
			idx = fmtExp.indexOf("*");
			if (idx >= 0) {
				return srcTable.cursor(fields, exp, ctx);
			}
			
			String regex = "[a-zA-Z0-9]+";
			if (fmtExp.matches(regex) && fmtExp.length() < 3) {
				return srcTable.cursor(fields, exp, ctx);
			}
			LongArray tempPos = select(exp, opt, ctx);
			
			ArrayList<ModifyRecord> mrl = TableMetaData.getModifyRecord(srcTable, exp, ctx);
			if (tempPos != null && tempPos.size() > 0) {
				ICursor cs = new IndexCursor(srcTable, fields, ifields, tempPos.toArray(), opt, ctx);
				if (cs instanceof IndexCursor) {
					((IndexCursor) cs).setModifyRecordList(mrl);
					if (maxRecordLen != 0) {
						((IndexCursor) cs).setRowBufferSize(maxRecordLen);
					}
				}
				
				Select select = new Select(exp, null);
				cs.addOperation(select, ctx);
				return cs;
			} else {
				if (mrl == null) {
					return null;
				} else {
					return new IndexCursor(srcTable, fields, ifields, null, opt, ctx);
				}
			}
		}
		return srcTable.cursor(fields, exp, ctx);
	}
	
	public LongArray select(Expression exp, String opt, Context ctx) {
		String f = ifields[0];
		IParam sub2 = ((Like) exp.getHome()).getParam().getSub(1);
		String fmtExp = (String) sub2.getLeafExpression().calculate(ctx);
		fmtExp = fmtExp.substring(1, fmtExp.length() - 1);
		
		boolean isRow = srcTable instanceof RowTableMetaData;
		long recCountOfSegment[] = null;
		if (!isRow) {
			recCountOfSegment = ((ColumnTableMetaData)srcTable).getSegmentInfo();
		}
		
		//对每个关键字符进行过滤，求交集
		String regex = "[a-zA-Z0-9]+";
		String search = "";
		LongArray tempPos = null;
		int strLen = fmtExp.length();
		int j;
		int p = 0;//表示处理到的位置
		for (j = 0; j < strLen; ) {
			String str = fmtExp.substring(j, j + 1);
			p = j + 1;
			if (str.matches(regex)) {
				//英文
				//尝试连续取4个字母
				if (j + 3 < strLen) {
					String str4 = fmtExp.substring(j, j + 4);
					if (str4.matches(regex)) {
						str = str4;
						p = j + 4;
					}
				} else if (j + 2 < strLen) {//尝试连续取3个字母
					String str3 = fmtExp.substring(j, j + 3);
					if (str3.matches(regex)) {
						str = str3;
						p = j + 3;
					}
				}
			}
			j++;
			if (search.indexOf(str) >= 0) {
				continue;//重复的不再查询
			}
			search = fmtExp.substring(0, p);
			Expression tempExp = new Expression(f + "==\"" + str + "\"");
			LongArray srcPos =  super.select(tempExp, opt, null);
			if (srcPos == null || srcPos.size() == 0) {
				tempPos = null;
				break;
			}
			
			boolean sort = true;
			if (!hasSecIndex()) {
				sort = false;
			}
			
			//排序，归并求交集
			if (isRow) {
				tempPos = TableMetaData.longArrayUnite(tempPos, srcPos.toArray(), getPositionCount(), sort);
				if (tempPos.size() <= ITableIndex.MIN_ICURSOR_REC_COUNT) {
					break;
				}
			} else {
				long [] arr = srcPos.toArray();
				if (sort) {
					Arrays.sort(arr);
				}
				tempPos = TableMetaData.longArrayUnite(tempPos, arr);
				if (TableMetaData.getBlockCount(tempPos, recCountOfSegment) <= ITableIndex.MIN_ICURSOR_BLOCK_COUNT) {
					break;
				}
			}
		}
		if (tempPos == null) {
			tempPos = new LongArray();
		}
		return tempPos;
	}
	
	public void dup(TableMetaData table) {
		String dir = table.getGroupTable().getFile().getAbsolutePath() + "_";
		FileObject indexFile = new FileObject(dir + table.getTableName() + "_" + name);
		RandomOutputStream os = indexFile.getRandomOutputStream(true);
		RandomObjectWriter writer = new RandomObjectWriter(os);
		try {
			writeHeader(writer);
			writer.close();
		} catch (IOException e) {
			throw new RQException(e);
		}
	}
}