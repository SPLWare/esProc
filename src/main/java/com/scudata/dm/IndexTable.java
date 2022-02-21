package com.scudata.dm;

import com.scudata.expression.Expression;

/**
 * 内存索引表基类
 * @author WangXiaoJun
 *
 */
abstract public class IndexTable {
	/**
	 * 按排号键建立成多层树状索引
	 * @param code
	 * @param field
	 * @return
	 */
	public static IndexTable instance_s(Sequence code, int field) {
		SerialBytesIndexTable it = new SerialBytesIndexTable();
		it.create(code, field);
		return it;
	}
	
	/**
	 * 为序列创建索引
	 * @param code 序列
	 * @return
	 */
	public static IndexTable instance(Sequence code) {
		return instance(code, code.length());
	}
	
	/**
	 * 为序列创建索引
	 * @param code 序列
	 * @param capacity 索引表容量
	 * @return
	 */
	public static IndexTable instance(Sequence code, int capacity) {
		Object obj = null;
		if (code.length() > 0) {
			obj = code.getMem(1);
		}
		
		if (obj instanceof Record) {
			Record r = (Record)obj;
			if (r.dataStruct().getTimeKeyCount() == 1) {
				int []pkIndex = r.dataStruct().getPKIndex();
				return new TimeIndexTable(code, pkIndex, capacity);
			}
		}
		
		HashIndexTable it = new HashIndexTable(capacity);
		it.create(code);
		return it;
	}
	
	/**
	 * 为排列按指定表达式创建索引
	 * @param code 排列
	 * @param exp 字段表达式
	 * @param ctx
	 * @return
	 */
	public static IndexTable instance(Sequence code, Expression exp, Context ctx) {
		return instance(code, exp, ctx, code.length());
	}
	
	/**
	 * 为排列按指定表达式创建索引
	 * @param code 排列
	 * @param exp 字段表达式
	 * @param ctx
	 * @param capacity 哈希表容量
	 * @return
	 */
	public static IndexTable instance(Sequence code, Expression exp, Context ctx, int capacity) {
		if (exp == null) {
			return instance(code, capacity);
		} else {
			HashIndexTable it = new HashIndexTable(capacity);
			it.create(code, exp, ctx);
			return it;
		}
	}

	/**
	 * 为排列按指定表达式创建多字段索引
	 * @param code 排列
	 * @param exps 字段表达式数组
	 * @param ctx
	 * @return
	 */
	public static IndexTable instance(Sequence code, Expression []exps, Context ctx) {
		return instance(code, exps, ctx, code.length());
	}
	
	public static IndexTable instance(Sequence code, Expression []exps, Context ctx, int capacity) {
		if (exps == null) {
			return instance(code, capacity);
		} else if (exps.length == 1) {
			return instance(code, exps[0], ctx, capacity);
		} else {
			Object obj = null;
			if (code.length() > 0) {
				obj = code.getMem(1);
			}
			
			if (obj instanceof Record) {
				DataStruct ds = ((Record)obj).dataStruct();
				if (ds.getTimeKeyCount() == 1) {
					int []pkIndex = ds.getPKIndex();
					if (ds.isSameFields(exps, pkIndex)) {
						return new TimeIndexTable(code, pkIndex, capacity);
					}
				}
			}
			
			HashArrayIndexTable it = new HashArrayIndexTable(capacity);
			it.create(code, exps, ctx);
			return it;
		}
	}

	public static IndexTable instance(Sequence code, int []fields, int capacity, String opt) {
		if (fields.length == 1) {
			HashIndexTable it = new HashIndexTable(capacity, opt);
			it.create(code, fields[0]);
			return it;
		} else {
			if (code.length() > 0) {
				Record r = (Record)code.getMem(1);
				if (r.dataStruct().getTimeKeyCount() == 1) {
					return new TimeIndexTable(code, fields, capacity);
				}
			}
			
			HashArrayIndexTable it = new HashArrayIndexTable(capacity, opt);
			it.create(code, fields);
			return it;
		}
	}
	
	/**
	 * 根据键查找对应的值，此方法用于主键为一个字段的哈希表
	 * @param key 键
	 * @return
	 */
	abstract public Object find(Object key);

	/**
	 * 根据键查找对应的值，此方法用于主键为一个字段的哈希表
	 * @param keys 键值数组
	 * @return
	 */
	abstract public Object find(Object []keys);
}
