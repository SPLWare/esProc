package com.scudata.expression.mfn.dw;

import java.io.File;
import java.io.IOException;

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
		Integer blockSize = null;
		
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
				if (sub0.isLeaf()) {
					fo = sub0.getLeafExpression().calculate(ctx);
				} else {
					IParam fileParam = sub0.getSub(0);
					if (fileParam == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("create" + mm.getMessage("function.paramTypeError"));
					}
					fo = fileParam.getLeafExpression().calculate(ctx);
					
					IParam blockSizeParam = sub0.getSub(1);
					if (blockSizeParam != null) {
						String b = blockSizeParam.getLeafExpression().calculate(ctx).toString();
						try {
							blockSize = Integer.parseInt(b);
						} catch (NumberFormatException e) {
						}
					}
				}
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
			FileObject file = (FileObject) fo;

			String opt = option;
			if ((opt == null || opt.indexOf('y') == -1) && file.isExists()) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("file.fileAlreadyExist", file.getFileName()));
			} else if (opt != null && opt.indexOf('y') != -1 && file.isExists()) {
				try {
					ComTable table = ComTable.open(file, ctx);
					table.delete();
				} catch (IOException e) {
					throw new RQException(e.getMessage(), e);
				}
			}
			
			opt = opt == null ? "n" : opt + "n";
			((PhyTable)table).getGroupTable().reset(file.getLocalFile().file(), opt, ctx, distribute, blockSize, null);

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
			String opt = option;
			if ((opt == null || opt.indexOf('y') == -1) && fg.isExist()) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("file.fileAlreadyExist", fg.getFileName()));
			} else if (opt != null && opt.indexOf('y') != -1 && fg.isExist()) {
				fg.delete(ctx);
			}
			
			opt = opt == null ? "n" : opt + "n";
			int partitions[] = fg.getPartitions();
			int pcount = fg.getPartitions().length;
			String fileName = fg.getFileName();
			for (int i = 0; i < pcount; ++i) {
				File newFile = Env.getPartitionFile(partitions[i], fileName);
				((PhyTable)table).getGroupTable().reset(newFile, opt, ctx, distribute, blockSize, null);
			}
			return fg.open(null, ctx);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("create" + mm.getMessage("function.paramTypeError"));
		}
	}

}
