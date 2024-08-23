package com.scudata.expression.fn.gather;

import java.io.IOException;

import com.scudata.common.RQException;
import com.scudata.dm.FileObject;
import com.scudata.dm.ObjectReader;

interface IValues {
	Object getTop();
	Object pop();
}

class Values implements IValues {
	private Object []values;
	private int count;
	private int index = 0;
	
	public Values(Object []values) {
		this.values = values;
		this.count = values.length;
	}
	
	public Object getTop() {
		if (index < count) {
			return values[index];
		} else {
			return null;
		}
	}
	
	public Object pop() {
		if (index < count) {
			return values[index++];
		} else {
			return null;
		}
	}
}

class FileValues implements IValues {
	private FileObject fo;
	private ObjectReader reader;
	private int count; // 剩余值数量
	private Object value; // 当前读到的值
	
	public FileValues(FileObject fo) {
		try {
			this.fo = fo;
			reader = new ObjectReader(fo.getInputStream());
			count = reader.readInt();
			value = reader.readObject();
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		}
	}
	
	public Object getTop() {
		return value;
	}
	
	public Object pop() {
		if (count > 1) {
			Object obj = value;
			count--;
			
			try {
				value = reader.readObject();
			} catch (IOException e) {
				throw new RQException(e.getMessage(), e);
			}
			
			return obj;
		} else if (count == 1) {
			Object obj = value;
			count = 0;
			value = null;
			delete();
			return obj;
		} else {
			return null;
		}
	}
	
	public void delete() {
		try {
			reader.close();
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		}
		
		fo.delete();
	}
}
