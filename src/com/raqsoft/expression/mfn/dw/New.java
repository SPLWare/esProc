package com.raqsoft.expression.mfn.dw;

import java.util.ArrayList;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.cursor.ICursor;
import com.raqsoft.dm.cursor.MemoryCursor;
import com.raqsoft.dm.cursor.MultipathCursors;
import com.raqsoft.dw.ColumnTableMetaData;
import com.raqsoft.dw.ITableMetaData;
import com.raqsoft.dw.JoinCursor;
import com.raqsoft.dw.JoinCursor2;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.Node;
import com.raqsoft.expression.ParamInfo2;
import com.raqsoft.expression.TableMetaDataFunction;
import com.raqsoft.expression.operator.And;
import com.raqsoft.resources.EngineMessage;

/**
 * 根据A/cs的键/维值取出T的其它字段返回，A/cs对T的键有序
 * T.new(A/cs,x:C,…;w)
 * @author RunQian
 *
 */
public class New extends TableMetaDataFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("new" + mm.getMessage("function.missingParam"));
		}
		if (table == null) {
			return table;
		}
		
		char type = param.getType();
		ICursor cursor = null;
		IParam param = this.param;
		Expression filter = null;
		String []fkNames = null;
		Sequence []codes = null;
		
		if (type == IParam.Semicolon) {
			if (param.getSubSize() > 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("new" + mm.getMessage("function.invalidParam"));
			}
			
			IParam expParam = param.getSub(1);
			param = param.getSub(0);
			type = param.getType();
			
			if (expParam == null) {
			} else if (expParam.isLeaf()) {
				filter = expParam.getLeafExpression();
			} else if (expParam.getType() == IParam.Colon) {
				if (expParam.getSubSize() != 2) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("new" + mm.getMessage("function.invalidParam"));						
				}
				
				IParam sub0 = expParam.getSub(0);
				IParam sub1 = expParam.getSub(1);
				if (sub0 == null || sub1 == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("new" + mm.getMessage("function.invalidParam"));
				}
				
				String fkName = sub0.getLeafExpression().getIdentifierName();
				fkNames = new String[]{fkName};
				Object val = sub1.getLeafExpression().calculate(ctx);
				if (val instanceof Sequence) {
					codes = new Sequence[]{(Sequence)val};
				} else if (val == null) {
					return null;
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("new" + mm.getMessage("function.paramTypeError"));
				}
			} else {
				ArrayList<Expression> expList = new ArrayList<Expression>();
				ArrayList<String> fieldList = new ArrayList<String>();
				ArrayList<Sequence> codeList = new ArrayList<Sequence>();
				
				for (int p = 0, psize = expParam.getSubSize(); p < psize; ++p) {
					IParam sub = expParam.getSub(p);
					if (sub == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("new" + mm.getMessage("function.invalidParam"));						
					} else if (sub.isLeaf()) {
						expList.add(sub.getLeafExpression());
					} else {
						if (sub.getSubSize() != 2) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("new" + mm.getMessage("function.invalidParam"));						
						}
						
						IParam sub0 = sub.getSub(0);
						IParam sub1 = sub.getSub(1);
						if (sub0 == null || sub1 == null) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("new" + mm.getMessage("function.invalidParam"));
						}
						
						String fkName = sub0.getLeafExpression().getIdentifierName();
						Object val = sub1.getLeafExpression().calculate(ctx);
						if (val instanceof Sequence) {
							fieldList.add(fkName);
							codeList.add((Sequence)val);
						} else if (val == null) {
							return null;
						} else {
							MessageManager mm = EngineMessage.get();
							throw new RQException("new" + mm.getMessage("function.paramTypeError"));
						}
					}
				}
				
				int fieldCount = fieldList.size();
				if (fieldCount > 0) {
					fkNames = new String[fieldCount];
					codes = new Sequence[fieldCount];
					fieldList.toArray(fkNames);
					codeList.toArray(codes);
				}
				
				int expCount = expList.size();
				if (expCount == 1) {
					filter = expList.get(0);
				} else if (expCount > 1) {
					Expression exp = expList.get(0);
					Node home = exp.getHome();
					for (int i = 1; i < expCount; ++i) {
						exp = expList.get(i);
						And and = new And();
						and.setLeft(home);
						and.setRight(exp.getHome());
						home = and;
					}
					
					filter = new Expression(home);
				}
			}
		}

		if (type != IParam.Comma) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("new" + mm.getMessage("function.invalidParam"));
		} else if (param.getSubSize() < 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("new" + mm.getMessage("function.invalidParam"));
		}
		Object obj = param.getSub(0).getLeafExpression().calculate(ctx);
		if (obj instanceof Sequence) {
			cursor = new MemoryCursor((Sequence) obj);
		} else if (obj instanceof ICursor) {
			cursor = (ICursor) obj;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("new" + mm.getMessage("function.invalidParam"));
		}
	
		
		IParam newParam = param.create(1, param.getSubSize());
		ParamInfo2 pi = ParamInfo2.parse(newParam, "new", false, false);
		Expression []exps = pi.getExpressions1();
		String []names = pi.getExpressionStrs2();
		
		if (JoinCursor.isColTable(table)) {
			return _new((ColumnTableMetaData)table, cursor, obj, filter, exps,	names, fkNames, codes, option, ctx);
		} else {
			return _new(table, cursor, obj, filter, exps,	names, fkNames, codes, ctx);
		}
	
	}
	
	public static  Object _new(ColumnTableMetaData table, ICursor cursor, Object obj, Expression filter, Expression []exps,
			String[] names, String []fkNames, Sequence []codes, String option, Context ctx) {
		int type = 0x01;
		if (cursor instanceof MultipathCursors) {
			type += 0x10;
			MultipathCursors mcursor = (MultipathCursors) cursor;
			ICursor cursors[] = mcursor.getCursors();
			int pathCount = cursors.length;
			for (int i = 0; i < pathCount; ++i) {
				Expression w = null;
				if (filter != null) {
					w = filter.newExpression(ctx); // 分段并行读取时需要复制表达式，同一个表达式不支持并行运算
				}
				ICursor cs = new JoinCursor(table, exps, names, cursors[i], type, option, w, fkNames, codes, ctx);
				cursors[i] = cs;
			}
			return new MultipathCursors(cursors, ctx);
		}
		ICursor cs = new JoinCursor(table, exps, names, cursor, type, option, filter, fkNames, codes, ctx);
		if (obj instanceof Sequence) {
			return cs.fetch();
		} else {
			return cs;
		}
	}
	
	public static Object _new(ITableMetaData table, ICursor cursor, Object obj, Expression filter, Expression []exps,
			String[] names, String []fkNames, Sequence []codes, Context ctx) {
		if (cursor instanceof MultipathCursors) {
			return JoinCursor2.makeMultiJoinCursor(table, exps, names, (MultipathCursors)cursor, filter, fkNames, codes, 2, ctx);
		}
		ICursor cs = new JoinCursor2(table, exps, names, cursor, filter, fkNames, codes, 1, ctx);
		if (obj instanceof Sequence) {
			return cs.fetch();
		} else {
			return cs;
		}
	}
}
