package com.scudata.dm;

import com.scudata.array.BoolArray;
import com.scudata.array.IArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.resources.EngineMessage;

/**
 * 用于序号键的索引，外键值是记录在维表中的序号
 * @author RunQian
 *
 */
public class SeqIndexTable extends IndexTable {
	private Sequence code; // 维表
	private int len;
	
	public SeqIndexTable(Sequence code) {
		this.code = code;
		this.len = code.length();
	}
	
	/**
	 * 构建排号索引
	 * @param code 维表
	 * @param field 序号字段的索引
	 */
	public SeqIndexTable(Sequence code, int field) {
		int len = code.length();
		Sequence result = new Sequence(len);
		
		for (int i = 1; i <= len; ++i) {
			BaseRecord r = (BaseRecord)code.getMem(i);
			Object v = r.getNormalFieldValue(field);
			if (!(v instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.needIntExp"));
			}
			
			int seq = ((Number)v).intValue();
			if (seq <= result.length() && result.getMem(seq) != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(seq + mm.getMessage("engine.dupKeys"));
			}
			
			result.set(seq, r);
		}
		
		this.code = result;
		this.len = result.length();
	}

	public Object find(Object key) {
		if (key instanceof Number) {
			int seq = ((Number)key).intValue();
			if (seq > 0 && seq <= len) {
				return code.getMem(seq);
			}
		}
		
		return null;
	}

	public Object find(Object []keys) {
		return find(keys[0]);
	}
	
	public int findPos(Object key) {
		if (key instanceof Number) {
			int seq = ((Number)key).intValue();
			if (seq > 0 && seq <= len && code.isTrue(seq)) {
				return seq;
			}
		}
		
		return 0;
	}

	public int findPos(Object []keys) {
		return findPos(keys[0]);
	}
	
	public int[] findAllPos(IArray key) {
		int len = key.size();
		int codeLen = this.len;
		int[] pos = new int[len + 1];
		Sequence code = this.code;
		
		if (code instanceof Table) {
			for (int i = 1; i <= len; i++) {
				if (!key.isNull(i)) {
					int seq = key.getInt(i);
					if (seq > 0 && seq <= codeLen) {
						pos[i] = seq;
					}
				}
			}
		} else {
			IArray mems = code.getMems();
			for (int i = 1; i <= len; i++) {
				if (!key.isNull(i)) {
					int seq = key.getInt(i);
					if (seq > 0 && seq <= codeLen && mems.isTrue(seq)) {
						pos[i] = seq;
					}
				}
			}
		}
		
		return pos;
	}

	public int[] findAllPos(IArray[] keys) {
		return findAllPos(keys[0]);
	}

	public int[] findAllPos(IArray key, BoolArray signArray) {
		int len = key.size();
		int codeLen = this.len;
		int[] pos = new int[len + 1];
		Sequence code = this.code;
		
		if (code instanceof Table) {
			for (int i = 1; i <= len; i++) {
				if (signArray.isTrue(i) && !key.isNull(i)) {
					int seq = key.getInt(i);
					if (seq > 0 && seq <= codeLen) {
						pos[i] = seq;
					}
				}
			}
		} else {
			IArray mems = code.getMems();
			for (int i = 1; i <= len; i++) {
				if (signArray.isTrue(i) && !key.isNull(i)) {
					int seq = key.getInt(i);
					if (seq > 0 && seq <= codeLen && mems.isTrue(seq)) {
						pos[i] = seq;
					}
				}
			}
		}
		
		return pos;
	}

	public int[] findAllPos(IArray[] keys, BoolArray signArray) {
		return findAllPos(keys[0], signArray);
	}
}
