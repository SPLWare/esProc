package com.scudata.expression.mfn.file;

import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dw.PhyTable;
import com.scudata.dw.PhyTableGroup;
import com.scudata.expression.FileGroupFunction;

/**
 * 获得组表文件的结构
 * f.structure()
 * @author LiWei
 *
 */
public class FileGroupStructure extends FileGroupFunction {
	public Object calculate(Context ctx) {
		PhyTableGroup tmg = fg.open(option, ctx);
		PhyTable table = (PhyTable) tmg.getTables()[0];
		Sequence seq = new Sequence();
		seq.add(Structure.getTableStruct(table, option));
		table.close();
		return seq;
	}
}
