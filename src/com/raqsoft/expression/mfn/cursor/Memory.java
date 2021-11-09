package com.raqsoft.expression.mfn.cursor;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.Table;
import com.raqsoft.dw.MemoryTable;
import com.raqsoft.dw.TableMetaData;
import com.raqsoft.expression.CursorFunction;
import com.raqsoft.expression.IParam;
import com.raqsoft.parallel.ClusterCursor;
import com.raqsoft.parallel.ClusterMemoryTable;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.util.CursorUtil;

/**
 * 把游标转成内表
 * cs.memory(K,…)
 * @author RunQian
 *
 */
public class Memory extends CursorFunction {
	public Object calculate(Context ctx) {
		if (cursor instanceof ClusterCursor) {
			return memory((ClusterCursor)cursor, param, ctx);
		}

		Sequence seq;
		String []keys = null;
		String distribute = null;
		Integer partition = null;;
		
		TableMetaData tmd = CursorUtil.getTableMetaData(cursor);
		if (tmd != null) {
			distribute = tmd.getDistribute();
			partition = tmd.getGroupTable().getPartition();
		}
		
		if (param != null) {
			if (param.isLeaf()) {
				keys = new String[]{param.getLeafExpression().getIdentifierName()};
			} else {
				int size = param.getSubSize();
				keys = new String[size];
				for (int i = 0; i < size; ++i) {
					IParam sub = param.getSub(i);
					if (sub == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("memory" + mm.getMessage("function.invalidParam"));
					}
					
					keys[i] = sub.getLeafExpression().getIdentifierName();
				}
			}
		}
		
		if (option != null && option.indexOf('z') != -1) {
			MemoryTable table = new MemoryTable(cursor);
			if (keys != null) {
				table.setPrimary(keys);
			}
			
			if (partition != null) {
				table.setDistribute(distribute);
				table.setPart(partition);
			}
			
			return table;
		}
		
		seq = cursor.fetch();
		Table table;
		
		if (seq instanceof Table) {
			table = (Table)seq;
		} else if (seq == null) {
			return null;
		} else {
			table = seq.derive("o");
		}
		
		MemoryTable result = new MemoryTable(table);
		if (keys != null) {
			result.setPrimary(keys);
		}
		
		if (partition != null) {
			result.setDistribute(distribute);
			result.setPart(partition);
		}
		
		return result;
	}

	private static ClusterMemoryTable memory(ClusterCursor cursor, IParam param, Context ctx) {
		String []fields = null;
		if (param == null) {
		} else if (param.isLeaf()) {
			fields = new String[]{param.getLeafExpression().getIdentifierName()};
		} else {
			int size = param.getSubSize();
			fields = new String[size];
			for (int i = 0; i < size; ++i) {
				IParam sub = param.getSub(i);
				if (sub == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("memory" + mm.getMessage("function.invalidParam"));
				}
				
				fields[i] = sub.getLeafExpression().getIdentifierName();
			}
		}
		
		return cursor.memory(fields, ctx);
	}
}
