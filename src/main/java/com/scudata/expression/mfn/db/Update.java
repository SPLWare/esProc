package com.scudata.expression.mfn.db;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;
import com.scudata.expression.DBFunction;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * 执行数据库更新语句
 * db.update(A:A',tbl,F:x,…;P,…)
 * @author RunQian
 *
 */
public class Update extends DBFunction {
	public Object calculate(Context ctx) {
		String []pks = null;
		IParam param = this.param;
		if (param != null && param.getType() == IParam.Semicolon) {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("update" + mm.getMessage("function.invalidParam"));
			}

			IParam p = param.getSub(1);
			if (p == null) {
			} else if (p.isLeaf()) {
				String name = p.getLeafExpression().getIdentifierName();
				pks = new String[] {name};
			} else {
				int size = p.getSubSize();
				pks = new String[size];
				for (int i = 0; i < size; ++i) {
					IParam sub = p.getSub(i);
					if (sub == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("update" + mm.getMessage("function.invalidParam"));
					}

					pks[i] = sub.getLeafExpression().getIdentifierName();
				}
			}

			param = param.getSub(0);
		}

		if (param == null || param.getType() != IParam.Comma) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("update" + mm.getMessage("function.invalidParam"));
		}

		Sequence seq1 = null;
		ICursor cursor = null;
		Sequence seq2 = null;
		
		IParam srcParam = param.getSub(0);
		if (srcParam == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("update" + mm.getMessage("function.invalidParam"));
		} else if (srcParam.isLeaf()) {
			Object srcObj = srcParam.getLeafExpression().calculate(ctx);
			if (srcObj instanceof Sequence) {
				seq1 = (Sequence)srcObj;
			} else if (srcObj instanceof ICursor) {
				cursor = (ICursor)srcObj;
			} else if (srcObj != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("update" + mm.getMessage("function.paramTypeError"));
			}
		} else {
			if (srcParam.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("update" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub0 = srcParam.getSub(0);
			IParam sub1 = srcParam.getSub(1);
			if (sub0 == null || sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("update" + mm.getMessage("function.invalidParam"));
			}
			
			Object srcObj = sub0.getLeafExpression().calculate(ctx);
			if (!(srcObj instanceof Sequence) && srcObj != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("update" + mm.getMessage("function.paramTypeError"));
			}
			
			seq1 = (Sequence)srcObj;
			srcObj = sub1.getLeafExpression().calculate(ctx);
			if (!(srcObj instanceof Sequence) && srcObj != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("update" + mm.getMessage("function.paramTypeError"));
			}
			
			seq2 = (Sequence)srcObj;
		}

		Sequence sequence = null;
		if (seq1 != null) {
			sequence = seq1;
		} else if (seq2 != null) {
			sequence = seq2;
		} else if (cursor != null) {
			sequence = cursor.peek(1);
		}
		
		if (sequence == null) {
			return new Integer(0);
		}

		IParam sub1 = param.getSub(1);
		if (sub1 == null || !sub1.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("update" + mm.getMessage("function.invalidParam"));
		}

		String tableName = sub1.getLeafExpression().getIdentifierName();
		String []names;
		Expression []valExps;
		int fcount;

		int size = param.getSubSize();
		if (size > 2) {
			fcount = size - 2;
			names = new String[fcount];
			valExps = new Expression[fcount];
			for (int i = 2, seq = 0; i < size; ++i, ++seq) {
				IParam sub = param.getSub(i);
				if (sub == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("update" + mm.getMessage("function.invalidParam"));
				} else if (sub.isLeaf()) {
					names[seq] = sub.getLeafExpression().getIdentifierName();
					valExps[seq] = new Expression(ctx, names[seq]);
				} else {
					if (sub.getSubSize() != 2) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("update" + mm.getMessage("function.invalidParam"));
					}

					IParam nameParam = sub.getSub(0);
					IParam valParam = sub.getSub(1);
					if (nameParam == null || valParam == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("update" + mm.getMessage("function.invalidParam"));
					}

					names[seq] = nameParam.getLeafExpression().getIdentifierName();
					valExps[seq] = valParam.getLeafExpression();
				}
			}
		} else {
			DataStruct ds = sequence.dataStruct();
			if (ds == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("update: " + mm.getMessage("engine.needPurePmt"));
			}

			names = ds.getFieldNames();
			fcount = names.length;
			valExps = new Expression[fcount];
			for (int f = 0; f < fcount; ++f) {
				valExps[f] = new Expression(ctx, "#" + (f + 1));
			}
		}

		String []opts = null;
		if (pks != null) {
			opts = new String[fcount];

			Next:
			for (int p = 0; p < pks.length; ++p) {
				String pkName = pks[p];
				for (int f = 0; f < fcount; ++f) {
					if (pkName.equalsIgnoreCase(names[f])) {
						opts[f] = "p";
						continue Next;
					}
				}

				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("function.paramError", "update", mm.getMessage("mfn.dbUpdateKey")));
			}

			if (option != null && option.indexOf('1') != -1) {
				if (opts[0] == null) {
					opts[0] = "a";
				} else {
					opts[0] += "a";
				}
			}
		} else if (option != null && option.indexOf('1') != -1) {
			opts = new String[fcount];
			opts[0] = "a";
		}

		if (cursor != null) {
			return db.update(cursor, tableName, names, opts, valExps, option, ctx);
		} else if (seq2 != null) {
			return db.update(seq1, seq2, tableName, names, opts, valExps, option, ctx);
		} else {
			return db.update(seq1, tableName, names, opts, valExps, option, ctx);
		}
	}
}
