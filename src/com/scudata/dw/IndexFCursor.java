package com.scudata.dw;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

import com.scudata.common.RQException;
import com.scudata.dm.DataStruct;
import com.scudata.dm.FileObject;
import com.scudata.dm.ListBase1;
import com.scudata.dm.ObjectReader;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.ICursor;

/**
 * 遍历KV索引的游标
 * 用于取全部数据的情况
 * @author runqian
 *
 */
public class IndexFCursor extends ICursor {
	private static final int BUFFER_SIZE = 1024;
	private long recordCount;
	private String []ifields;
	private String []vfields;
	private long startPos;
	private boolean isClosed;
	private Sequence cache;
	
	private FileObject indexFile;
	private ObjectReader reader;
	private boolean isFirstSkip = true;
	
	public IndexFCursor(TableKeyValueIndex index, long startPos) {
		this.startPos = startPos;
		
		ifields = index.ifields;
		vfields = index.vfields;
		indexFile = index.indexFile;
		recordCount = index.srcTable.getActualRecordCount();
		
		int icount = ifields.length;
		int vcount = vfields.length;
		String []fieldNames = Arrays.copyOf(ifields, icount + vcount);
		System.arraycopy(vfields, 0, fieldNames, icount, vcount);

		dataStruct = new DataStruct(fieldNames);
		init();
	}
	
	void init() {
		InputStream is = indexFile.getInputStream();
		reader = new ObjectReader(is, BUFFER_SIZE);
		try {
			reader.seek(startPos);
		} catch (IOException e) {
			try {
				reader.close();
			} catch (IOException e1) {
			}
			throw new RQException(e.getMessage(), e);
		}
	}
	
	private void getData(Sequence data, int n) {
		int icount = ifields.length;
		int vcount = vfields.length;
		
		ArrayList<Object> keyList = new ArrayList<Object>(64);
		DataStruct ds = dataStruct;
		Record r = new Record(ds);
		ListBase1 mems = data.getMems();
		
		try {
			int count;
			int size = 0;
			reader.readLong40();
			
			while ((count = reader.readInt()) > 0) {
				for (int i = 0; i < icount; ++i) {
					r.setNormalFieldValue(i, reader.readObject());
				}

				for (int i = 0; i < count; ++i) {
					for (int j = 0; j < icount; ++j) {
						keyList.add(r.getNormalFieldValue(j));
					}
					size++;
					reader.readInt();
				}
			}
			
			//到了value块了,如果有命中的,取value值
			reader.readInt();
			
			int c = 0;
			if (size > n) {
				int i;
				for (i = 0; i < n; ++i) {
					r = new Record(ds);
					for (int j = 0; j < icount; ++j) {
						r.setNormalFieldValue(j, keyList.get(c++));//key
					}
					for (int j = 0; j < vcount; ++j) {
						r.setNormalFieldValue(icount + j, reader.readObject());//values
					}
					reader.skipObject();//伪号
					mems.add(r);
				}
				Table tmp = new Table(ds, ICursor.FETCHCOUNT);
				this.cache = tmp;
				mems = tmp.getMems();
				for (; i < size; ++i) {
					r = new Record(ds);
					for (int j = 0; j < icount; ++j) {
						r.setNormalFieldValue(j, keyList.get(c++));//key
					}
					for (int j = 0; j < vcount; ++j) {
						r.setNormalFieldValue(icount + j, reader.readObject());//values
					}
					reader.skipObject();//伪号
					mems.add(r);
				}
			} else {
				for (int i = 0; i < size; ++i) {
					r = new Record(ds);
					for (int j = 0; j < icount; ++j) {
						r.setNormalFieldValue(j, keyList.get(c++));//key
					}
					for (int j = 0; j < vcount; ++j) {
						r.setNormalFieldValue(icount + j, reader.readObject());//values
					}
					reader.skipObject();//伪号
					mems.add(r);
				}
			}
			
			keyList.clear();
			c = reader.readInt();
			if (c == TableKeyValueIndex.BLOCK_END) {
				reader.close();
				reader = null;
			}
		} catch (IOException e) {
			try {
				reader.close();
			} catch (IOException e1) {
			}
			throw new RQException(e.getMessage(), e);
		}
	}
	
	protected Sequence get(int n) {
		isFirstSkip = false;
		if (isClosed || n < 1) {
			return null;
		}

		Sequence cache = this.cache;
		if (cache != null) {
			int len = cache.length();
			if (len > n) {
				this.cache = cache.split(n + 1);
				return cache;
			} else if (len == n) {
				this.cache = null;
				return cache;
			}
		} else {
			cache = new Table(dataStruct, ICursor.FETCHCOUNT);
		}

		this.cache = null;
		while (cache.length() < n) {
			if (reader == null) {
				break;
			}
			getData(cache, n - cache.length());
		}
		
		if (cache.length() > 0) {
			return cache;
		} else {
			return null;
		}
	}

	protected long skipOver(long n) {
		if (isFirstSkip && n == MAXSKIPSIZE) {
			return recordCount;
		}
		
		Sequence data;
		long rest = n;
		long count = 0;
		while (rest != 0) {
			if (rest > FETCHCOUNT) {
				data = get(FETCHCOUNT);
			} else {
				data = get((int)rest);
			}
			if (data == null) {
				break;
			} else {
				count += data.length();
			}
			rest -= data.length();
		}

		isFirstSkip = false;
		return count;
	}
	
	
	public void close() {
		super.close();
		isClosed = true;
		cache = null;
		
		try {
			if (reader != null) {
				reader.close();
			}
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		} finally {
			reader = null;
		}
	}
	
	public boolean reset() {
		close();
		
		isClosed = false;
		init();
		isFirstSkip = true;
		return true;
	}
	
	/**
	 * 根据索引字段和取出字段，返回有序字段
	 * @return
	 */
	public int[] getSortFieldsIndex() {
		if (ifields == null)
			return null;
		
		String[] ifields = this.ifields;
		int len = ifields.length;
		int indexs[] = new int[len];
		for (int i = 0; i < len; i++) {
			indexs[i] = dataStruct.getFieldIndex(ifields[i]);
		}
		
		return indexs;
	}
}