package com.raqsoft.expression.mfn.dw;

import java.io.File;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Env;
import com.raqsoft.dm.FileGroup;
import com.raqsoft.dm.FileObject;
import com.raqsoft.dw.GroupTable;
import com.raqsoft.dw.TableMetaData;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.TableMetaDataFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 创建组表文件,用T的数据结构创建新的组表文件f
 * T.create(f;x)
 * @author RunQian
 *
 */
public class Create extends TableMetaDataFunction{

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
			((TableMetaData)table).getGroupTable().reset(file, "n", ctx, distribute);

			TableMetaData table = GroupTable.openBaseTable(file, ctx);
			Integer partition = ((FileObject) fo).getPartition();
			if (partition != null && partition.intValue() > 0) {
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
				((TableMetaData)table).getGroupTable().reset(newFile, "n", ctx, distribute);
			}
			return fg.open(null, ctx);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("create" + mm.getMessage("function.paramTypeError"));
		}
	}

}
