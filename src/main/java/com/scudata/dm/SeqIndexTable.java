package com.scudata.dm;

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
	
	public SeqIndexTable(Sequence code) {
		this.code = code;
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
			Record r = (Record)code.getMem(i);
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
	}

	public Object find(Object key) {
		if (key instanceof Number) {
			int seq = ((Number)key).intValue();
			if (seq > 0 && seq <= code.length()) {
				return code.getMem(seq);
			}
		}
		
		return null;
	}

	public Object find(Object []keys) {
		if (keys.length == 1) {
			return find(keys[0]);
		}
		
		return null;
	}
}
