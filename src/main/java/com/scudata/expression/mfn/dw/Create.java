package com.scudata.expression.mfn.dw;

import java.io.File;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Env;
import com.scudata.dm.FileGroup;
import com.scudata.dm.FileObject;
import com.scudata.dw.ComTable;
import com.scudata.dw.PhyTable;
import com.scudata.expression.IParam;
import com.scudata.expression.PhyTableFunction;
import com.scudata.resources.EngineMessage;

/**
 * 创建组表文件,用T的数据结构创建新的组表文件f
 * T.create(f;x)
 * @author RunQian
 *
 */
public class Create extends PhyTableFunction{

	public Object calculate(Context ctx) {
		Object fo = null;			
		String distribute = null;
		
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("create" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			fo = param.getLeafExpression().calculate(ctx);
		} else {
			if (param.getType() != IParam.Semicolon) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("create" + mm.getMessage("function.invalidParam"));
			}
			
			int size = param.getSubSize();
			if (size != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("create" + mm.getMessage("function.invalidParam"));
			}

			IParam sub0 = param.getSub(0);
			if (sub0 != null) {
				fo = sub0.getLeafExpression().calculate(ctx);
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("create" + mm.getMessage("function.paramTypeError"));
			}
			
			IParam expParam = param.getSub(1);
			if (expParam != null) {
				distribute = expParam.getLeafExpression().toString();
			}
		}
		
		if (fo instanceof FileObject) {
			File file = ((FileObject) fo).getLocalFile().file();
			((PhyTable)table).getGroupTable().reset(file, "n", ctx, distribute);

			PhyTable table = ComTable.openBaseTable(file, ctx);
			Integer partition = ((FileObject) fo).getPartition();
			if (partition != null && partition.intValue() >= 0) {
				table.getGroupTable().setPartition(partition);
			}
			
			return table; 
		} else if (fo instanceof FileGroup) {
			if (distribute == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("create" + mm.getMessage("function.paramTypeError"));
			}
			FileGroup fg = (FileGroup) fo;
			int partitions[] = fg.getPartitions();
			int pcount = fg.getPartitions().length;
			String fileName = fg.getFileName();
			for (int i = 0; i < pcount; ++i) {
				File newFile = Env.getPartitionFile(partitions[i], fileName);
				((PhyTable)table).getGroupTable().reset(newFile, "n", ctx, distribute);
			}
			return fg.open(null, ctx);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("create" + mm.getMessage("function.paramTypeError"));
		}
	}

}
