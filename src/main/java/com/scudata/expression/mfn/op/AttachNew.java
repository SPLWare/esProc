package com.scudata.expression.mfn.op;

import java.util.ArrayList;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.op.New;
import com.scudata.dw.pseudo.IPseudo;
import com.scudata.dw.pseudo.PseudoNew;
import com.scudata.dw.pseudo.PseudoTable;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.expression.OperableFunction;
import com.scudata.expression.ParamInfo2;
import com.scudata.expression.operator.And;
import com.scudata.resources.EngineMessage;

/**
 * 对游标或管道附加创建新序表运算;对虚表.new()处理
 * op.new(xi:Fi,…) op是游标或管道
 * @author RunQian
 *
 */
public class AttachNew extends OperableFunction {
	public Object calculate(Context ctx) {
		if (isPseudoOper(operable, param, ctx)) {
			return pseudoCalculate(ctx);
		}
		
		ParamInfo2 pi = ParamInfo2.parse(param, "new", false, false);
		Expression []exps = pi.getExpressions1();
		String []names = pi.getExpressionStrs2();

		New op = new New(this, exps, names, option);
		if (cs != null) {
			op.setCurrentCell(cs.getCurrent());
		}
		
		return operable.addOperation(op, ctx);
	}
	
	private Object pseudoCalculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("new" + mm.getMessage("function.missingParam"));
		}
		IPseudo pseudo = (IPseudo) operable;
		char type = param.getType();		

		if (pseudo == null) return pseudo;
		IParam param = this.param;
		Expression filter = null;
		String []fkNames = null;
		Sequence []codes = null;
		String[] opts = null;
		
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
					com.scudata.expression.mfn.dw.News.parseFilterParam(expParam, expList, fieldList, codeList, optList, "news", ctx);
				} else {
					for (int p = 0, psize = expParam.getSubSize(); p < psize; ++p) {
						IParam sub = expParam.getSub(p);
						com.scudata.expression.mfn.dw.News.parseFilterParam(sub, expList, fieldList, codeList, optList, "news", ctx);
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
		
		if (type == IParam.Normal) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (obj instanceof IPseudo) {
				return new PseudoNew(((PseudoTable) pseudo).getPd(), obj, option);
//				else if (srcObj instanceof ClusterTableMetaData) {
//					return ((ClusterTableMetaData)srcObj).createClusterPseudo(null, null, (ClusterPseudo) obj, option, ClusterTableMetaData.TYPE_NEW);
//				}
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
		String[] csNames = (String[]) objs[1];
		
		IParam newParam = param.create(1, param.getSubSize());
		ParamInfo2 pi = ParamInfo2.parse(newParam, "new", false, false);
		Expression []exps = pi.getExpressions1();
		String []names = pi.getExpressionStrs2();
		
		return new PseudoNew(((PseudoTable) pseudo).getPd(), obj, csNames, exps, names, 
				filter, fkNames, codes, opts, option);
	}
	
	public static boolean isPseudoOper(Object srcObj, IParam param, Context ctx) {
		if (srcObj == null || param == null) return false;
		if (srcObj instanceof PseudoTable) {
			//如果srcObj是原始虚表且参数是虚表
			try {
				if (param.getType() == IParam.Normal) {
					Object obj = param.getLeafExpression().calculate(ctx);
					if (obj instanceof IPseudo || obj instanceof ICursor || obj instanceof Sequence) {
						return true;
					}
				} else {
					ArrayList<Expression> list = new ArrayList<Expression>();
					param.getAllLeafExpression(list);
					Object obj = list.get(0).calculate(ctx);
					if (obj instanceof IPseudo || obj instanceof ICursor || obj instanceof Sequence) {
						return true;
					}
				}
			} catch (RQException e) {
				//obj不是虚表对象则忽略
			}
		}
		return false;
	}
	
	/**
	 * 提取new、news的第一个参数 (A/cs:K)
	 * @param param
	 * @param ctx
	 * @return Object[] {A/cs对象, K数组}
	 */
	public static Object[] parse1stParam(IParam param, Context ctx) {
		Object[] objs = new Object[3];
		Object obj = null;
		String[] csNames = null;
		IParam csParam = param.getSub(0);
		if (csParam.isLeaf()) {
			obj = csParam.getLeafExpression().calculate(ctx);
		} else {
			obj = csParam.getSub(0).getLeafExpression().calculate(ctx);
			IParam newParam = csParam.create(1, csParam.getSubSize());
			ParamInfo2 pi = ParamInfo2.parse(newParam, "new", false, false);
			csNames = pi.getExpressionStrs1();
		}
		
		objs[0] = obj;
		objs[1] = csNames;
		return objs;
	}
}
