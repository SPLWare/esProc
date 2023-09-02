package com.scudata.expression.mfn.dw;

import java.io.IOException;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.FileObject;
import com.scudata.dw.PhyTable;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.PhyTableFunction;
import com.scudata.resources.EngineMessage;

/**
 * 为组表创建索引
 * T.index(I:h,w;C,…;F,…;x)
 * @author RunQian
 *
 */
public class Index extends PhyTableFunction {
	public Object calculate(Context ctx) {
		String option = this.option;
		IParam param = this.param;
		
		if (param == null) {
			//delete all
			try {
				return table.deleteIndex(null);
			} catch (IOException e) {
				throw new RQException(e.getMessage(), e);
			}
	    }
		
		String[] fields = null;
		String[] valueFields = null;
		Object I = null;
		Expression w = null;
		
		if (param.isLeaf()) {
			try {
				I = param.getLeafExpression().calculate(ctx);
				if  (option != null) {
					if (option.indexOf('0') != -1 
						|| option.indexOf('2') != -1
						|| option.indexOf('3') != -1) {
						//load index & unload index
						table.createIndex((String) I, null, null, option, null, null);
						return table;
					}
				}
				
				//delete I
				return table.deleteIndex((String) I);
			} catch (IOException e) {
				throw new RQException(e.getMessage(), e);
			}
		} else if (param.getType() == IParam.Comma) {
			IParam sub0 = param.getSub(0);//I
			if (sub0 == null) {
				//delete all
				try {
					return table.deleteIndex(null);
				} catch (IOException e) {
					throw new RQException(e.getMessage(), e);
				}
			} else {
				//delete I
				try {
					I = (String) sub0.getLeafExpression().getIdentifierName();
					return table.deleteIndex((String) I);
				} catch (IOException e) {
					throw new RQException(e.getMessage(), e);
				}
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
				I = parseParam(sub0, ctx);
			} else if (sub0.getType() == IParam.Colon) {
				I = parseParam(sub0.getSub(0), ctx);
				hParam = sub0.getSub(1);
			} else if (sub0.getType() == IParam.Comma) {
				if (sub0.getSubSize() != 2) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("index" + mm.getMessage("function.paramTypeError"));
				}
				w = sub0.getSub(1).getLeafExpression();
				sub0 = sub0.getSub(0);
				if (sub0.getType() == IParam.Colon) {
					I = parseParam(sub0.getSub(0), ctx);
					hParam = sub0.getSub(1);
				} else {
					I = parseParam(sub0, ctx);
				}
			}

			if (fieldParam == null) {
				try {
					return table.deleteIndex((String) I);
				} catch (IOException e) {
					throw new RQException(e.getMessage(), e);
				}
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
			
			boolean isFile = false;
			FileObject file = null;
			if (I instanceof FileObject) {
				isFile = true;
				file = (FileObject) I;
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
				if (isFile)
					((PhyTable)table).createIndex(file, fields, valueFields, option, w, ctx);
				else
					table.createIndex((String) I, fields, valueFields, option, w, ctx);
				return table;
			}
			
			if (hParam != null) {
				//hashIndex
				Integer h = (Integer) hParam.getLeafExpression().calculate(ctx);
				if (isFile)
					((PhyTable)table).createIndex(file, fields, h, option, w, ctx);
				else
					table.createIndex((String) I, fields, h, option, w, ctx);
				return table;
			}
			
			if (isFile)
				((PhyTable)table).createIndex(file, fields, null, option, w, ctx);
			else
				table.createIndex((String) I, fields, null, option, w, ctx);
		}
		
		return table;
	}

	private Object parseParam(IParam param, Context ctx) {
		try {
			return param.getLeafExpression().calculate(ctx);
		} catch (Exception e) {
			return param.getLeafExpression().getIdentifierName();
		}
	}
	
	public boolean isLeftTypeMatch(Object obj) {
		return obj instanceof PhyTable;
	}
}
