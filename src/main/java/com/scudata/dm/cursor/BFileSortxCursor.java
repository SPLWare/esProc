package com.scudata.dm.cursor;

import java.io.IOException;
import java.util.Arrays;

import com.scudata.common.RQException;
import com.scudata.dm.BFileReader;
import com.scudata.dm.DataStruct;
import com.scudata.dm.FileObject;
import com.scudata.dm.ObjectReader;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;

/**
 * 读取集文件(把每条记录的读为字节数组)
 * 用于集文件排序
 * @author LW
 *
 */
public class BFileSortxCursor extends ICursor {
	private static final String BYTES_FIELD_NAME = "BFILE_BYTES_FIELD";
	
	private BFileReader reader;
	private BFileReader reader2;
	private int []fields;
	private int bytesIndex;
	private DataStruct fileDataStruct;
	private ObjectReader in;
	
	/**
	 * @param reader
	 * @param fields
	 */
	public BFileSortxCursor(FileObject file, String []fields) {
		reader = new BFileReader(file, fields, null);
		BFileReader reader2 = new BFileReader(file, fields, null);
		
		try {
			reader.open();
			this.fields = reader.getReadIndex();
			
			int len = fields.length;
			bytesIndex = len;
			
			String[] dsFields = Arrays.copyOf(fields, len + 1);
			dsFields[len] = BYTES_FIELD_NAME;
			dataStruct = new DataStruct(dsFields);
			fileDataStruct = reader.getFileDataStruct();
			
			reader2.open();
			in = reader2.getImporter();
			
		} catch (IOException e) {
			try {
				reader.close();
				reader2.close();
				in.close();
			} catch (IOException e1) {
			}
		}
	}
	
	public DataStruct getFileDataStruct() {
		return fileDataStruct;
	}

	protected Sequence get(int n) {
		if (reader == null || n < 1) {
			return null;
		}
		
		int count = 0;
		Sequence result = new Sequence(n);
		
		BFileReader reader = this.reader;
		ObjectReader in = this.in;
		int []fields = this.fields;
		DataStruct ds = this.dataStruct;
		int bytesIndex = this.bytesIndex;
		
		long lastPos = reader.position();
		
		try {
			while (count < n) {
				Record rec = new Record(ds);
				Object[] values = rec.getFieldValues();
				if (!reader.readRecord(fields, values)) {
					break;
				}
				long pos = reader.position();
				int length = (int) (pos - lastPos);
				
				byte[] bytes = new byte[length];
				
				in.read(bytes);
				values[bytesIndex] = bytes;
				result.add(rec);
				lastPos = pos;
				count++;
			}
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		}
		if (result.length() == 0)
			return null;
		else
			return result;
	}

	protected long skipOver(long n) {
		throw new RuntimeException();
	}
	
	public void close() {
		super.close();
		try {
			if (in != null) {
				in.close();
				in = null;
			}
			if (reader != null) {
				reader.close();
				reader = null;
			}
			if (reader2 != null) {
				reader2.close();
				reader2 = null;
			}
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		}
	}
}
