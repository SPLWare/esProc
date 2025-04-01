package com.scudata.dm.cursor;

import com.scudata.dm.BaseRecord;
import com.scudata.dm.Sequence;

/**
 * 把游标数据按照组表更新表的规则去重
 * @author WangXiaoJun
 *
 */
public class UpdateIdCursor extends ICursor {
	private ICursor cs; // 更新数据游标
	private int []keys; // 主键字段
	private int deleteField; // 删除标识字段，如果没有删除标识字段则为-1
	
	private Sequence data; // 游标缓存的数据
	private int cur; // 游标当前记录在缓存数据中的索引
	
	/**
	 * 构建更新去重游标
	 * @param cs 更新数据游标
	 * @param keys 主键字段
	 * @param deleteField 删除标识字段，如果没有删除标识字段则为-1
	 */
	public UpdateIdCursor(ICursor cs, int []keys, int deleteField) {
		this.cs = cs;
		this.keys = keys;
		this.deleteField = deleteField;
	}
	
	protected Sequence get(int n) {
		if (cs == null) {
			return null;
		}
		
		if (data == null) {
			data = cs.fetch(n);
			if (data == null || data.length() == 0) {
				cs = null;
				return null;
			}
			
			cur = 1;
		}
		
		int []keys = this.keys;
		int deleteField = this.deleteField;
		Sequence seq = data;
		int len = seq.length();
		BaseRecord prev = (BaseRecord)seq.getMem(cur);
		Sequence result = new Sequence(n);
		
		if (deleteField == -1) {
			for (int i = cur + 1; i <= len; ++i) {
				BaseRecord r = (BaseRecord)seq.getMem(i);
				if (!prev.isEquals(r, keys)) {
					result.add(prev);
				}
				
				prev = r;
			}
			
			data = cs.fetch(n);
			if (data == null || data.length() == 0) {
				cs = null;
				result.add(prev);
			} else {
				cur = 1;
				BaseRecord r = (BaseRecord)data.getMem(1);
				if (!prev.isEquals(r, keys)) {
					result.add(prev);
				}
			}
		} else {
			// 有删除标志的更新
			for (int i = cur + 1; i <= len; ++i) {
				BaseRecord r = (BaseRecord)seq.getMem(i);
				if (!prev.isEquals(r, keys)) {
					result.add(prev);
				} else {
					// 返回false则清除当前记录
					if (!UpdateMergeCursor.merge(prev, r, deleteField)) {
						i++;
						if (i <= len) {
							r = (BaseRecord)seq.getMem(i);
						} else {
							data = null;
							return result;
						}
					}
				}
				
				prev = r;
			}
			
			data = cs.fetch(n);
			if (data == null || data.length() == 0) {
				cs = null;
				result.add(prev);
			} else {
				cur = 1;
				BaseRecord r = (BaseRecord)data.getMem(1);
				if (!prev.isEquals(r, keys)) {
					result.add(prev);
				} else {
					// 返回false则清除当前记录
					if (!UpdateMergeCursor.merge(prev, r, deleteField)) {
						cur++;
						if (cur > data.length()) {
							cs = null;
						}
					}
				}
			}
		}
		
		return result;
	}

	protected long skipOver(long n) {
		Sequence data = get((int)n);
		if (data != null) {
			return data.length();
		} else {
			return 0;
		}
	}

	/**
	 * 关闭游标
	 */
	public synchronized void close() {
		super.close();
		cs = null;
	}
}
