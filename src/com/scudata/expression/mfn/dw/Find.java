package com.scudata.expression.mfn.dw;

import java.io.IOException;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.IParam;
import com.scudata.expression.TableMetaDataFunction;
import com.scudata.resources.EngineMessage;

/**
 * 用主键值找到组表的记录
 * T.find(k;x:C,..)
 * @author RunQian
 *
 */
public class Find extends TableMetaDataFunction {
	public Object calculate(Context ctx) {
		IParam param = this.param;
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("find" + mm.getMessage("function.missingParam"));
		}

		String []selFields = null;
		if (param.getType() == IParam.Semicolon) {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("find" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub1 = param.getSub(1);
			param = param.getSub(0);
			if (param == null || sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("find" + mm.getMessage("function.missingParam"));
			}
			
			if (sub1.isLeaf()) {
				selFields = new String[] {sub1.getLeafExpression().getIdentifierName()};
			} else {
				if (sub1.getType() != IParam.Comma) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("find" + mm.getMessage("function.invalidParam"));
				}
				int size = sub1.getSubSize();
				selFields = new String[size];
				for (int i = 0; i < size; ++i) {
					IParam p = sub1.getSub(i);
					if (p == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("find" + mm.getMessage("function.invalidParam"));
					}
					
					selFields[i] = p.getLeafExpression().getIdentifierName();
				}
			}
		}
		
		Object key;
		if (param.isLeaf()) {
			key = param.getLeafExpression().calculate(ctx);
		} else {
			int count = param.getSubSize();
			Sequence seq = new Sequence(count);
			key = seq;
			for (int i = 0; i < count; ++i) {
				IParam sub = param.getSub(i);
				if (sub == null || !sub.isLeaf()) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("find" + mm.getMessage("function.invalidParam"));
				}
				
				seq.add(sub.getLeafExpression().calculate(ctx));
			}
		}

		Sequence keys;
		if (option == null || option.indexOf('k') == -1) {
			keys = new Sequence(1);
			keys.add(key);
		} else {
			if (!(key instanceof Sequence)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("find" + mm.getMessage("function.paramTypeError"));
			}
			
			keys = (Sequence)key;
		}
		
		try {
			return table.finds(keys, selFields);
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		}
	}
}
