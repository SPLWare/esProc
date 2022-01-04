package com.scudata.expression.mfn.cursor;

import java.util.ArrayList;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.FileObject;
import com.scudata.dm.SyncReader;
import com.scudata.dm.cursor.CSJoinxCursor;
import com.scudata.dm.cursor.CSJoinxCursor2;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.cursor.MultipathCursors;
import com.scudata.dm.op.Operation;
import com.scudata.dw.ColumnTableMetaData;
import com.scudata.dw.Cursor;
import com.scudata.expression.CursorFunction;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * 把游标与排列或者外存表做连接，复制一些字段生成新游标返回
 * cs.joinx(C:…,f:K:…,x:F,…;…;…;n)
 * @author RunQian
 *
 */
public class Joinx extends CursorFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("joinx" + mm.getMessage("function.missingParam"));
		}

		Expression [][]exps;
		Object []codes;
		Expression [][]dataExps;
		Expression [][]newExps;
		String [][]newNames;
		boolean isOrg = option != null && option.indexOf('o') != -1;
		String fname = null;
		int capacity = ICursor.INITSIZE;
		
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("joinx" + mm.getMessage("function.invalidParam"));
		}
		
		if (param.getType() == IParam.Comma) {
			if (isOrg) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("joinx" + mm.getMessage("function.invalidParam"));
			}
			exps = new Expression[1][];
			codes = new Object[1];
			dataExps = new Expression[1][];
			newExps = new Expression[1][];
			newNames = new String[1][];
			parseJoinParam(param, 0, exps, codes, dataExps, newExps, newNames, ctx);
			
		} else if (param.getType() == IParam.Semicolon) {
			int count = param.getSubSize();
			IParam sub = param.getSub(count - 1);
			if (sub == null) {
				count--;
			} else if (sub.isLeaf()) {
				Object obj = sub.getLeafExpression().calculate(ctx);
				if (obj instanceof Number) {
					int n = ((Number)obj).intValue();
					if (n > capacity) capacity = n;
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("joinx" + mm.getMessage("function.paramTypeError"));
				}
				count--;
			}
			
			if (isOrg) {
				if (count < 2) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("joinx" + mm.getMessage("function.invalidParam"));
				}
				sub = param.getSub(0);
				if (sub != null) {
					if (!sub.isLeaf()) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("joinx" + mm.getMessage("function.invalidParam"));
					}
					
					fname = sub.getLeafExpression().getIdentifierName();
				}
				
				int len = count - 1;
				exps = new Expression[len][];
				codes = new Object[len];
				dataExps = new Expression[len][];
				newExps = new Expression[len][];
				newNames = new String[len][];
	
				for (int i = 0; i < len; ++i) {
					sub = param.getSub(i + 1);
					if (sub == null || sub.getType() != IParam.Comma) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("joinx" + mm.getMessage("function.invalidParam"));
					}
	
					parseJoinParam(sub, i, exps, codes, dataExps, newExps, newNames, ctx);
				}
			} else {
				if (count < 1) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("joinx" + mm.getMessage("function.invalidParam"));
				}
				exps = new Expression[count][];
				codes = new Object[count];
				dataExps = new Expression[count][];
				newExps = new Expression[count][];
				newNames = new String[count][];
	
				for (int i = 0; i < count; ++i) {
					sub = param.getSub(i);
					if (sub == null || sub.getType() != IParam.Comma) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("joinx" + mm.getMessage("function.invalidParam"));
					}
	
					parseJoinParam(sub, i, exps, codes, dataExps, newExps, newNames, ctx);
				}
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("joinx" + mm.getMessage("function.invalidParam"));
		}

		if (option != null && option.indexOf('q') != -1) {
			return new CSJoinxCursor2(cursor, exps, codes, dataExps, newExps, newNames, fname, ctx, capacity, option);
		} else {
			return getJoinxCursor(cursor, codes, exps, dataExps, newExps, newNames, fname, ctx, option, capacity);
		}
	}

	private static String[] makeFields(Expression []dataExps, Expression []newExps ,Context ctx) {
		int len = dataExps.length;
		ArrayList<String> keys = new ArrayList<String>(len);
		for (int j = 0; j < len; j++) {
			keys.add(dataExps[j].toString());
		}
		for (Expression exp : newExps) {
			exp.getUsedFields(ctx, keys);
		}
		String[] arr = new String[keys.size()];
		keys.toArray(arr);
		return arr;
	}
	
	public static ICursor getJoinxCursor(Object srcObj, Object []codes, Expression [][]exps, Expression [][]dataExps, 
			Expression [][]newExps, String [][]newNames, String fname, Context ctx, String option, int capacity) {
		int count = codes.length;
		SyncReader[] readers = new SyncReader[count];
		for (int i = 0; i < count; i++) {
			String[] fields = makeFields(dataExps[i], newExps[i], ctx);//得到维表字段
			
			if (codes[i] instanceof ColumnTableMetaData) {
				readers[i] = new SyncReader((ColumnTableMetaData)codes[i], fields, capacity);
			} else if (codes[i] instanceof FileObject) {
				readers[i] = new SyncReader((FileObject)codes[i], dataExps[i], capacity);
			} else if (codes[i] instanceof Cursor) {
				readers[i] = new SyncReader((Cursor)codes[i], dataExps[i], capacity);
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("joinx" + mm.getMessage("function.invalidParam"));
			}
		}
		
		if (srcObj instanceof MultipathCursors) {
			MultipathCursors mcs = (MultipathCursors)srcObj;
			int pathCount = mcs.getPathCount();
			ICursor results[] = new ICursor[pathCount];
			for (int i = 0; i < count; i++) {
				readers[i].setParallCount(pathCount);
			}
			ICursor cursors[] = mcs.getCursors();
			for (int i = 0; i < pathCount; ++i) {
				Context tmpCtx = ctx.newComputeContext();
				Expression [][]tmpExps = Operation.dupExpressions(exps, tmpCtx);
				Expression [][]tmpNewExps = Operation.dupExpressions(newExps, tmpCtx);
				results[i] = new CSJoinxCursor(cursors[i], readers, tmpExps, dataExps, tmpNewExps, newNames, fname, tmpCtx, option, capacity / pathCount);
			}
			return new MultipathCursors(results, ctx);
		}
		return new CSJoinxCursor(((ICursor) srcObj), readers, exps, dataExps, newExps, newNames, fname, ctx, option, capacity);
	}
}
