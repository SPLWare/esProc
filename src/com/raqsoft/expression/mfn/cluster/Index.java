package com.raqsoft.expression.mfn.cluster;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.ClusterTableMetaDataFunction;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.IParam;
import com.raqsoft.resources.EngineMessage;

/**
 * 为集群组表创建索引
 * T.index(I:h,w;C,…;F,…;x)
 * @author RunQian
 *
 */
public class Index extends ClusterTableMetaDataFunction {
	public Object calculate(Context ctx) {
		String option = this.option;
		IParam param = this.param;
		
		if (param == null) {
			return table.deleteIndex(null);
	    }
		String[] fields = null;
		String[] valueFields = null;
		String I = null;
		Expression w = null;
		
		if (param.isLeaf()) {
			I = (String) param.getLeafExpression().getIdentifierName();
			return table.deleteIndex(I);
		} else if (param.getType() == IParam.Comma) {
			IParam sub0 = param.getSub(0);//I
			if (sub0 == null) {
				return table.deleteIndex(null);
			} else {
				I = (String) sub0.getLeafExpression().getIdentifierName();
				return table.deleteIndex(I);
			}
		} else if (param.getType() == IParam.Semicolon) {
			int size = param.getSubSize();
			if ((size < 2) || (size > 3)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("index" + mm.getMessage("function.paramTypeError"));
			}
			IParam sub0 = param.getSub(0);
			IParam fieldParam = param.getSub(1);
			IParam valueFieldParam = null;
			if (size == 3) {
				valueFieldParam = param.getSub(2);
			}
			IParam hParam = null;
			if (sub0.isLeaf()) {
				I = (String) sub0.getLeafExpression().getIdentifierName();
			} else if (sub0.getType() == IParam.Colon) {
				I = (String) sub0.getSub(0).getLeafExpression().getIdentifierName();
				hParam = sub0.getSub(1);
			} else if (sub0.getType() == IParam.Comma) {
				if (sub0.getSubSize() != 2) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("index" + mm.getMessage("function.paramTypeError"));
				}
				w = sub0.getSub(1).getLeafExpression();
				sub0 = sub0.getSub(0);
				if (sub0.getType() == IParam.Colon) {
					I = (String) sub0.getSub(0).getLeafExpression().getIdentifierName();
					hParam = sub0.getSub(1);
				} else {
					I = (String) sub0.getLeafExpression().getIdentifierName();
				}
			}

			if (fieldParam == null) {
				return table.deleteIndex(I);
			} else if (fieldParam.isLeaf()) {
				fields = new String[]{fieldParam.getLeafExpression().getIdentifierName()};
			} else {
				int fcount = fieldParam.getSubSize();
				fields = new String[fcount];
				for (int i = 0; i < fcount; ++i) {
					IParam sub = fieldParam.getSub(i);
					if (sub == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("index" + mm.getMessage("function.invalidParam"));
					}
					
					fields[i] = sub.getLeafExpression().getIdentifierName();
				}
			}
			
			if (valueFieldParam != null) {
				if (valueFieldParam.isLeaf()) {
					valueFields = new String[]{valueFieldParam.getLeafExpression().getIdentifierName()};
				} else {
					int fcount = valueFieldParam.getSubSize();
					valueFields = new String[fcount];
					for (int i = 0; i < fcount; ++i) {
						IParam sub = valueFieldParam.getSub(i);
						if (sub == null) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("index" + mm.getMessage("function.invalidParam"));
						}
						
						valueFields[i] = sub.getLeafExpression().getIdentifierName();
					}
				}
				
				//key-value-Index
				table.createIndex(I, fields, valueFields, option, w);
				return table;
			}
			
			if (hParam != null) {
				//hashIndex
				Integer h = (Integer) hParam.getLeafExpression().calculate(ctx);
				table.createIndex(I, fields, h, option, w);
				return table;
			}

			table.createIndex(I, fields, null, option, w);
		}
		
		return table;
	}

}
