package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.FileObject;
import com.scudata.expression.IParam;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;
import com.scudata.util.BFileUtil;

/**
 * 对集文件序列进行外存排序
 * f.sortx(Fi,…;f’)
 * @author LW
 *
 */
public class Sortx extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("sortx" + mm.getMessage("function.missingParam"));
		}

		IParam sortParam = null;
		FileObject out = null;
		
		if (param.getType() == IParam.Semicolon) {
			if (param.getSubSize() > 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("sortx" + mm.getMessage("function.invalidParam"));
			}

			sortParam = param.getSub(0);
			if (sortParam == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("sortx" + mm.getMessage("function.invalidParam"));
			}

			IParam sub = param.getSub(1);
			if (sub == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("sortx" + mm.getMessage("function.invalidParam"));
			} else if (sub.isLeaf()) {
				Object obj = sub.getLeafExpression().calculate(ctx);
				if (obj instanceof FileObject) {
					out = (FileObject) obj;
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("sortx" + mm.getMessage("function.paramTypeError"));
				}
			
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("sortx" + mm.getMessage("function.invalidParam"));
			}
		} else {
			sortParam = param;
		}

		String []fields;
		if (sortParam.isLeaf()) { // 只有一个参数
			fields = new String[]{ sortParam.getLeafExpression().toString() };
		} else if (sortParam.getType() == IParam.Comma) { // ,
			int size = sortParam.getSubSize();
			fields = new String[size];
			for (int i = 0; i < size; ++i) {
				IParam sub = sortParam.getSub(i);
				if (sub == null || !sub.isLeaf()) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("sortx" + mm.getMessage("function.invalidParam"));
				}
				fields[i] = sub.getLeafExpression().toString();
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("sortx" + mm.getMessage("function.invalidParam"));
		}

		BFileUtil.sortx(srcSequence, out, fields, ctx, option);
		return Boolean.TRUE;
	}
}
