package com.scudata.expression.mfn.dw;

import java.util.ArrayList;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.cursor.MemoryCursor;
import com.scudata.dm.cursor.MultipathCursors;
import com.scudata.dm.op.Operation;
import com.scudata.dw.ColPhyTable;
import com.scudata.dw.IPhyTable;
import com.scudata.dw.JoinCursor;
import com.scudata.dw.JoinCursor2;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.expression.ParamInfo2;
import com.scudata.expression.PhyTableFunction;
import com.scudata.expression.operator.And;
import com.scudata.resources.EngineMessage;

/**
 * 根据A/cs的键/维值取出T的其它字段返回，A/cs对T的键有序
 * T.new(A/cs,x:C,…;w)
 * @author RunQian
 *
 */
public class New extends PhyTableFunction {
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
		String []opts = null;
		
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
			} else {
				ArrayList<Expression> expList = new ArrayList<Expression>();
				ArrayList<String> fieldList = new ArrayList<String>();
				ArrayList<Sequence> codeList = new ArrayList<Sequence>();
				ArrayList<String> optList = new ArrayList<String>();
				
				if (expParam.getType() == IParam.Colon) {
					News.parseFilterParam(expParam, expList, fieldList, codeList, optList, "new", ctx);
				} else {
					for (int p = 0, psize = expParam.getSubSize(); p < psize; ++p) {
						IParam sub = expParam.getSub(p);
						News.parseFilterParam(sub, expList, fieldList, codeList, optList, "new", ctx);
					}
				}
				
				int fieldCount = fieldList.size();
				if (fieldCount > 0) {
					fkNames = new String[fieldCount];
					codes = new Sequence[fieldCount];
					opts = new String[fieldCount];
					fieldList.toArray(fkNames);
					codeList.toArray(codes);
					optList.toArray(opts);
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
		
		Object[] objs = parse1stParam(param, ctx);
		Object obj = objs[0];
		cursor = (ICursor) objs[1];
		String[] csNames = (String[]) objs[2];
		
		IParam newParam = param.create(1, param.getSubSize());
		ParamInfo2 pi = ParamInfo2.parse(newParam, "new", false, false);
		Expression []exps = pi.getExpressions1();
		String []names = pi.getExpressionStrs2();
		
		if (JoinCursor.isColTable(table)) {
			return _new((ColPhyTable)table, cursor, obj, csNames, filter, exps,	names, fkNames, codes, opts, option, ctx);
		} else {
			return _new(table, cursor, obj, filter, exps, names, fkNames, codes, opts, ctx);
		}
	}

	/**
	 * 提取new、news的第一个参数 (A/cs:K)
	 * @param param
	 * @param ctx
	 * @return Object[] {转换后的cursor, A/cs对象, K数组}
	 */
	public static Object[] parse1stParam(IParam param, Context ctx) {
		Object[] objs = new Object[3];
		ICursor cursor;
		Object obj = null;
		String[] csNames = null;
		IParam csParam = param.getSub(0);
		if (csParam.isLeaf()) {
			obj = csParam.getLeafExpression().calculate(ctx);
			if (obj instanceof Sequence) {
				cursor = new MemoryCursor((Sequence) obj);
			} else if (obj instanceof ICursor) {
				cursor = (ICursor) obj;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("new" + mm.getMessage("function.invalidParam"));
			}
		} else {
			obj = csParam.getSub(0).getLeafExpression().calculate(ctx);
			if (obj instanceof Sequence) {
				cursor = new MemoryCursor((Sequence) obj);
			} else if (obj instanceof ICursor) {
				cursor = (ICursor) obj;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("new" + mm.getMessage("function.invalidParam"));
			}
			int size = csParam.getSubSize();
			if (size > 1) {
				size--;
				csNames = new String[size];
				for (int i = 0; i < size; i++) {
					csNames[i] = csParam.getSub(i + 1).getLeafExpression().getIdentifierName();
				}
			}
		}
		
		objs[0] = obj;
		objs[1] = cursor;
		objs[2] = csNames;
		return objs;
	}
	
	public static  Object _new(ColPhyTable table, ICursor cursor, Object obj, 
			String[] csNames, Expression filter, Expression []exps, String[] names, 
			String []fkNames, Sequence []codes, String[] opts, String option, Context ctx) {
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
				Expression[] exps_ = Operation.dupExpressions(exps, ctx);
				ICursor cs = cursors[i].attachNews(table, csNames, w, exps_, names, fkNames, codes, opts, option, type, ctx);
				cursors[i] = cs;
			}
			return new MultipathCursors(cursors, ctx);
		}
		ICursor cs = cursor.attachNews(table, csNames, filter, exps, names, fkNames, codes, opts, option, type, ctx);
		if (obj instanceof Sequence) {
			return cs.fetch();
		} else {
			return cs;
		}
	}
	
	public static Object _new(IPhyTable table, ICursor cursor, Object obj, Expression filter, Expression []exps,
			String[] names, String []fkNames, Sequence []codes, String[] opts, Context ctx) {
		if (cursor instanceof MultipathCursors) {
			return JoinCursor2.makeMultiJoinCursor(table, exps, names, (MultipathCursors)cursor, filter, fkNames, codes, opts, 1, ctx);
		}
		ICursor cs = new JoinCursor2(table, exps, names, cursor, filter, fkNames, codes, opts, 1, ctx);
		if (obj instanceof Sequence) {
			return cs.fetch();
		} else {
			return cs;
		}
	}
}
