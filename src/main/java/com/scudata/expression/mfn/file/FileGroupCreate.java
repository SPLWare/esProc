package com.scudata.expression.mfn.file;

import java.io.IOException;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.FileGroupFunction;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * 创建复组表
 * f.create(C,…;x)
 * @author RunQian
 *
 */
public class FileGroupCreate extends FileGroupFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("create" + mm.getMessage("function.missingParam"));
		}

		IParam colParam = param;
		String distribute = null;
		Integer blockSize = null;
		
		if (param.getType() == IParam.Semicolon) {
			int size = param.getSubSize();
			if (size != 2 && size != 3) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("create" + mm.getMessage("function.invalidParam"));
			}
			
			colParam = param.getSub(0);
			if (colParam == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("create" + mm.getMessage("function.invalidParam"));
			}
			
			IParam expParam = param.getSub(1);
			if (expParam != null) {
				distribute = expParam.getLeafExpression().toString();
			}
			
			if (size == 3) {
				IParam blockSizeParam = param.getSub(2);
				if (blockSizeParam != null) {
					String b = blockSizeParam.getLeafExpression().calculate(ctx).toString();
					try {
						blockSize = Integer.parseInt(b);
					} catch (NumberFormatException e) {
					}
				}
			}
		}
		String []cols;
		if (colParam.isLeaf()) {
			cols = new String[]{colParam.getLeafExpression().getIdentifierName()};
		} else {
			int size = colParam.getSubSize();
			cols = new String[size];
			for (int i = 0; i < size; ++i) {
				IParam sub = colParam.getSub(i);
				if (sub == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("create" + mm.getMessage("function.invalidParam"));
				}
				
				cols[i] = sub.getLeafExpression().getIdentifierName();
			}
		}

		try {
			return fg.create(cols, distribute, option, blockSize, ctx);
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		}
	}
}
