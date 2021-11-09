package com.raqsoft.dm;

import com.raqsoft.common.*;
import com.raqsoft.resources.EngineMessage;

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
		
		Record r = (Record)code.getMem(1);
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
			r = (Record)code.getMem(i);
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
}
