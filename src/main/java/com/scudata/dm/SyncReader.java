package com.scudata.dm;

import java.io.IOException;
import java.util.ArrayList;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.cursor.BFileCursor;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dw.ColPhyTable;
import com.scudata.dw.Cursor;
import com.scudata.dw.MemoryTable;
import com.scudata.expression.Expression;
import com.scudata.resources.EngineMessage;

//用于多个线程同步从组表、集文件、游标取数
public class SyncReader {

	private Object srcObj;
	private ArrayList<Integer> countList = new ArrayList<Integer>();//每段的条数
	private Sequence values = new Sequence();//每段的首条值
	
	private int fetched[];
	private int parallCount = 1;
	private int SYNC_THREAD_NUM = 8;//同时取出的维表块数（时刻保证有这么多个块可用）
	private Sequence datas[];
	private String[] fields;//需要的维表字段
	private Thread []threads;
	
	public ArrayList<Integer> getCountList() {
		return countList;
	}

	public Sequence getValues() {
		return values;
	}

	private void init() {
		int size = countList.size();
		if (srcObj instanceof ColPhyTable) {
			size = countList.size() / 2;
		}
		datas = new Sequence[size];
		fetched = new int[size];
		threads = new Thread[size];
		for (int i = 0; i < size; ++i) {
			threads[i] = newLoadDataThread(srcObj, i, countList, datas, fields);
		}

		if (SYNC_THREAD_NUM > size) {
			SYNC_THREAD_NUM = size;
		}
	}
	
	public SyncReader(ColPhyTable table, String[] fields, int n) {
		try {
			String[] keys = table.getAllSortedColNames();
			if (keys == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("ds.lessKey"));
			}
			table.getSegmentInfo2(keys, countList, values, n);
		} catch (IOException e) {
			throw new RQException(e);
		}
		
		srcObj = table;
		this.fields = fields;
		init();
	}
	
	public SyncReader(FileObject file, Expression []exps, int n) {
		//转换为String[]
		int len = exps.length;
		String []keys = new String[len];
		for (int j = 0; j < len; j++) {
			keys[j] = exps[j].toString();
		}
		BFileReader reader = new BFileReader(file, keys, null);

		try {
			reader.getSegmentInfo(countList, values, n);
		} catch (IOException e) {
			throw new RQException(e);
		}
		srcObj = file;//
		init();
	}
	
	public SyncReader(Cursor cursor, Expression []exps, int n) {
		//转换为String[]
		int len = exps.length;
		String []keys = new String[len];
		for (int j = 0; j < len; j++) {
			keys[j] = exps[j].toString();
		}
		
		ColPhyTable tempTable;
		try {
			tempTable = (ColPhyTable) ((Cursor)cursor).getTableMetaData();
			tempTable.getSegmentInfo2(keys, countList, values, n);
		} catch (IOException e) {
			throw new RQException(e);
		}
		srcObj = tempTable;
		init();
	}
	
	public void loadData(int index) {
		if (srcObj instanceof ColPhyTable) {
			int len = fields.length;
			Expression[] fieldExps = new Expression[len];
			for (int i = 0; i < len; i++) {
				fieldExps[i] = new Expression(fields[i]);
			}
			Cursor cursor = (Cursor) ((ColPhyTable) srcObj).cursor(fieldExps, null, null, null, null, null, null);
			cursor.setSegment(false);
			cursor.reset();
			cursor.setSegment(countList.get(index * 2), countList.get(index * 2 + 1));
			datas[index] = new MemoryTable(cursor);
		} else {
			ICursor srcCursor = new BFileCursor((FileObject) srcObj, null, null, null);
			//TODO 设置分段pos
			datas[index] = srcCursor.fetch(countList.get(index));
		}
	}
	
	public static void loadData(Object srcObj, int index, ArrayList<Integer> countList, Sequence []datas, String[] fields) {
		if (index >= datas.length) {
			return;
		}
		if (srcObj instanceof ColPhyTable) {
			Cursor cursor = (Cursor) ((ColPhyTable) srcObj).cursor(fields);
			cursor.setSegment(false);
			cursor.reset();
			cursor.setSegment(countList.get(index * 2), countList.get(index * 2 + 1));
			datas[index] = new MemoryTable(cursor);
		} else {
			ICursor srcCursor = new BFileCursor((FileObject) srcObj, null, null, null);
			//TODO 设置分段pos
			datas[index] = new MemoryTable(srcCursor, countList.get(index));
		}
	}
	
	public synchronized Sequence getData(int index) {
		fetched[index] ++;
		if (datas[index] == null) {
			//loadData(index);
			if (parallCount == 1 || srcObj instanceof FileObject) {
				loadData(index);
			} else {

				try {
					//run到这里就说明join的速度比这里取数要快
					//一次启动多个
					int num = SYNC_THREAD_NUM;
					Thread[] threads = this.threads;
					for (int i = 0; i < num; i++) {
						if (index + i >= threads.length) {
							break;
						}
						if (threads[index + i].getState() == Thread.State.NEW) {
							threads[index + i].start(); //启动
						}
					}
					for (int i = 0; i < num; i++) {
						if (index + i >= threads.length) {
							break;
						}
						threads[index + i].join();
					}
				} catch (InterruptedException e) {
					throw new RQException(e);
				}
				
			}
		}
		
		if (fetched[index] == parallCount) {
			//如果所有的线程(joinx线程)都取过了
			Sequence data = datas[index];
			datas[index] = null;
			
			if (!(srcObj instanceof FileObject)) {
				//启动下一个未启动的线程
				int next = index + SYNC_THREAD_NUM;
				if (next < threads.length) {
					Thread t = threads[next];
					if (t.getState() == Thread.State.NEW) {
						t.start();
					}
				}
			}
			return data;
		}
		return datas[index];
	}
	
	public void setParallCount(int parallCount) {
		if (SYNC_THREAD_NUM < parallCount) {
			parallCount = SYNC_THREAD_NUM;
		}
		this.parallCount = parallCount;
		
		if (parallCount == 1) return;
		for (int i = 0; i < SYNC_THREAD_NUM; ++i) {
			threads[i].start(); // 启动线程分段取数
		}
	}
	
	private static Thread newLoadDataThread(final Object srcObj, final int index, 
			final ArrayList<Integer> countList, final Sequence []datas, final String[] fields) {
		return new Thread() {
			public void run() {
				loadData(srcObj, index, countList, datas, fields);
			}
		};
	}
}
