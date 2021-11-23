package com.scudata.expression.mfn.dw;

import java.util.ArrayList;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.cursor.MemoryCursor;
import com.scudata.dm.cursor.MultipathCursors;
import com.scudata.dw.ColumnTableMetaData;
import com.scudata.dw.ITableMetaData;
import com.scudata.dw.JoinCursor;
import com.scudata.dw.JoinCursor2;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.expression.ParamInfo2;
import com.scudata.expression.TableMetaDataFunction;
import com.scudata.expression.operator.And;
import com.scudata.resources.EngineMessage;

/**
 * 根据A/cs的键/维值取出T的其它字段返回，A/cs对T的键有序
 * T.derive(A/cs,x:C,…;w)
 * @author RunQian
 *
 */
public class Derive extends TableMetaDataFunction {
	public Object calculate(Context ctx) {
		IParam param = this.param;

		ICursor cursor = null;
		param = this.param;
		Expression filter = null;
		String []fkNames = null;
		Sequence []codes = null;
		
		char type = param.getType();
		if (type == IParam.Semicolon) {
			if (param.getSubSize() > 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("derive" + mm.getMessage("function.invalidParam"));
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
					throw new RQException("derive" + mm.getMessage("function.invalidParam"));						
				}
				
				IParam sub0 = expParam.getSub(0);
				IParam sub1 = expParam.getSub(1);
				if (sub0 == null || sub1 == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("derive" + mm.getMessage("function.invalidParam"));
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
					throw new RQException("derive" + mm.getMessage("function.paramTypeError"));
				}
			} else {
				ArrayList<Expression> expList = new ArrayList<Expression>();
				ArrayList<String> fieldList = new ArrayList<String>();
				ArrayList<Sequence> codeList = new ArrayList<Sequence>();
				
				for (int p = 0, psize = expParam.getSubSize(); p < psize; ++p) {
					IParam sub = expParam.getSub(p);
					if (sub == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("derive" + mm.getMessage("function.invalidParam"));						
					} else if (sub.isLeaf()) {
						expList.add(sub.getLeafExpression());
					} else {
						if (sub.getSubSize() != 2) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("derive" + mm.getMessage("function.invalidParam"));						
						}
						
						IParam sub0 = sub.getSub(0);
						IParam sub1 = sub.getSub(1);
						if (sub0 == null || sub1 == null) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("derive" + mm.getMessage("function.invalidParam"));
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
							throw new RQException("derive" + mm.getMessage("function.paramTypeError"));
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
			throw new RQException("derive" + mm.getMessage("function.invalidParam"));
		} else if (param.getSubSize() < 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("derive" + mm.getMessage("function.invalidParam"));
		}
		
		Object obj = param.getSub(0).getLeafExpression().calculate(ctx);
		if (obj instanceof Sequence) {
			cursor = new MemoryCursor((Sequence) obj);
		} else if (obj instanceof ICursor) {
			cursor = (ICursor) obj;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("derive" + mm.getMessage("function.invalidParam"));
		}
	
		IParam newParam = param.create(1, param.getSubSize());
		ParamInfo2 pi = ParamInfo2.parse(newParam, "derive", false, false);
		Expression []exps = pi.getExpressions1();
		String []names = pi.getExpressionStrs2();
		
		if (JoinCursor.isColTable(table)) {
			return derive((ColumnTableMetaData)table, cursor, obj, filter, exps,	names, fkNames, codes, option, ctx);
		} else {
			return derive(table, cursor, obj, filter, exps,	names, fkNames, codes, ctx);
		}
	
	}
	
	public static Object derive(ColumnTableMetaData table, ICursor cursor, Object obj, Expression filter, Expression []exps,
			String[] names, String []fkNames, Sequence []codes, String option, Context ctx) {
		if (cursor instanceof MultipathCursors) {
			MultipathCursors mcursor = (MultipathCursors) cursor;
			ICursor cursors[] = mcursor.getCursors();
			int pathCount = cursors.length;
			for (int i = 0; i < pathCount; ++i) {
				Expression w = null;
				if (filter != null) {
					w = filter.newExpression(ctx); // 分段并行读取时需要复制表达式，同一个表达式不支持并行运算
				}
				ICursor cs = new JoinCursor(table, exps, names, cursors[i], 0x10, option, w, fkNames, codes, ctx);
				cursors[i] = cs;
			}
			return new MultipathCursors(cursors, ctx);
		}
		ICursor cs = new JoinCursor(table, exps, names, cursor, 0, option, filter, fkNames, codes, ctx);
		if (obj instanceof Sequence) {
			return cs.fetch();
		} else {
			return cs;
		}
	}
	
	public static Object derive(ITableMetaData table, ICursor cursor, Object obj, Expression filter, Expression []exps,
			String[] names, String []fkNames, Sequence []codes, Context ctx) {
		if (cursor instanceof MultipathCursors) {
			return JoinCursor2.makeMultiJoinCursor(table, exps, names, (MultipathCursors)cursor, filter, fkNames, codes, 2, ctx);
		}
		ICursor cs = new JoinCursor2(table, exps, names, cursor, filter, fkNames, codes, 0, ctx);
		if (obj instanceof Sequence) {
			return cs.fetch();
		} else {
			return cs;
		}
	}
}
