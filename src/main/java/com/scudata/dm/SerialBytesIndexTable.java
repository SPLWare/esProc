package com.scudata.dm;

import com.scudata.array.BoolArray;
import com.scudata.array.IArray;
import com.scudata.common.*;
import com.scudata.resources.EngineMessage;

class SerialBytesIndexTable extends IndexTable {
	private static final int SERIALBYTE_LEN = 256;
	private Object []datas;
	private int level;
	
	public SerialBytesIndexTable() {
		datas = new Object[SERIALBYTE_LEN];
	}

	public void create(Sequence code, int field) {
		int len = code.length();
		if (len == 0) {
			return;
		}
		
		BaseRecord r = (BaseRecord)code.getMem(1);
		Object fval = r.getNormalFieldValue(field);
		if (!(fval instanceof SerialBytes)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.optError"));
		}
		
		SerialBytes sb = (SerialBytes)fval;
		int level = sb.length();
		this.level = level;
		Object []root = this.datas;
		
		for (int i = 1; i <= len; ++i) {
			r = (BaseRecord)code.getMem(i);
			fval = r.getNormalFieldValue(field);
			if (!(fval instanceof SerialBytes)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.optError"));
			}
			
			sb = (SerialBytes)fval;
			if (sb.length() != level) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.keyValCountNotMatch"));
			}
			
			Object []prev = root;
			for (int c = 1; c < level; ++c) {
				int n = (int)sb.getByte(c);
				if (prev[n] == null) {
					prev[n] = new Object[SERIALBYTE_LEN];
				}

				prev = (Object[])prev[n];
			}
			
			prev[(int)sb.getByte(level)] = r;
		}
	}

	public Object find(Object key) {
		if (!(key instanceof SerialBytes)) {
			return null;
		}
		
		SerialBytes sb = (SerialBytes)key;
		int len = sb.length();
		if (len != level) {
			return null;
		}
		
		Object []prev = datas;
		for (int c = 1; c < len; ++c) {
			int n = (int)sb.getByte(c);
			if (prev[n] == null) {
				return null;
			}

			prev = (Object[])prev[n];
		}
		
		return prev[(int)sb.getByte(level)];
	}

	public Object find(Object []keys) {
		if (keys.length == 1) {
			return find(keys[0]);
		} else {
			return null; // key not found
		}
	}
	
	public int findPos(Object key) {
		if (!(key instanceof SerialBytes)) {
			return -1;
		}
		
		SerialBytes sb = (SerialBytes)key;
		int len = sb.length();
		if (len != level) {
			return -1;
		}
		
		Object []prev = datas;
		for (int c = 1; c < len; ++c) {
			int n = (int)sb.getByte(c);
			if (prev[n] == null) {
				return -1;
			}

			prev = (Object[])prev[n];
		}
		
		return (int)sb.getByte(level);
	}
	
	public int findPos(Object []keys) {
		if (keys.length == 1) {
			return findPos(keys[0]);
		} else {
			return 0; // key not found
		}
	}
	
	public int[] findAllPos(IArray key) {
		if (key == null) {
			return null;
		}
		int len = key.size();
		int[] pos = new int[len + 1];
		for (int i = 1; i <= len; i++) {
			Object obj = key.get(i);
			pos[i] = findPos(obj);
		}
		return pos;
	}

	public int[] findAllPos(IArray[] keys) {
		if (keys == null) {
			return null;
		}
		
		int keyCount = keys.length;
		int len = keys[0].size();
		int[] pos = new int[len + 1];
		
		if (keyCount != 1) {
			return pos;
		}
		return findAllPos(keys[0]);
	}

	public int[] findAllPos(IArray key, BoolArray signArray) {
		if (key == null) {
			return null;
		}
		int len = key.size();
		int[] pos = new int[len + 1];
		for (int i = 1; i <= len; i++) {
			if (signArray.isFalse(i)) {
				continue;
			}
			Object obj = key.get(i);
			pos[i] = findPos(obj);
		}
		return pos;
	}

	public int[] findAllPos(IArray[] keys, BoolArray signArray) {
		if (keys == null) {
			return null;
		}
		
		int keyCount = keys.length;
		int len = keys[0].size();
		int[] pos = new int[len + 1];
		
		if (keyCount != 1) {
			return pos;
		}
		return findAllPos(keys[0], signArray);
	}
}
