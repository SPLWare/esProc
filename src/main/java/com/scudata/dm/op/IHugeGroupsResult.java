package com.scudata.dm.op;

import com.scudata.dm.Table;
import com.scudata.dm.cursor.ICursor;

/**
 * 大结果集分组计算对象
 * @author WangXiaoJun
 *
 */
public abstract class IHugeGroupsResult {
	public abstract Table groups(ICursor []cursors);
}
