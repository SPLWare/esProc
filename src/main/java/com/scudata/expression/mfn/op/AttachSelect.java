package com.scudata.expression.mfn.op;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.FileObject;
import com.scudata.dm.op.Channel;
import com.scudata.dm.op.FilePipe;
import com.scudata.dm.op.IPipe;
import com.scudata.dm.op.Select;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.OperableFunction;
import com.scudata.resources.EngineMessage;

/**
 * 对游标或管道附加过滤运算
 * op.select(x) op.select(x;f) op.select(x;ch) op是游标或管道，f是文件，ch是管道，不满足条件的写到文件或管道
 * @author RunQian
 *
 */
public class AttachSelect extends OperableFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			return operable;
		} else if (param.isLeaf()) {
			Expression fltExp = param.getLeafExpression();
			Select select = new Select(this, fltExp, option);
			select.setCurrentCell(cs.getCurrent());
			return operable.addOperation(select, ctx);
		} else {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("select" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);
			if (sub0 == null || sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("select" + mm.getMessage("function.invalidParam"));
			}
			
			IPipe pipe;
			Object obj = sub1.getLeafExpression().calculate(ctx);
			if (obj instanceof Channel) {
				pipe = (Channel)obj;
			} else if (obj instanceof String) {
				FileObject fo = new FileObject((String)obj);
				pipe = new FilePipe(fo);
			} else if (obj instanceof FileObject) {
				pipe = new FilePipe((FileObject)obj);
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("select" + mm.getMessage("function.paramTypeError"));
			}
			
			Expression fltExp = sub0.getLeafExpression();
			Select select = new Select(this, fltExp, option, pipe);
			select.setCurrentCell(cs.getCurrent());
			return operable.addOperation(select, ctx);
		}
	}
}
