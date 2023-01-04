package com.scudata.expression.mfn.op;

import java.util.ArrayList;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dw.pseudo.IPseudo;
import com.scudata.dw.pseudo.PseudoDerive;
import com.scudata.dw.pseudo.PseudoTable;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.expression.OperableFunction;
import com.scudata.expression.ParamInfo2;
import com.scudata.expression.operator.And;
import com.scudata.resources.EngineMessage;

/**
 * 对游标或管道附加添加字段生成新序表运算
 * op.derive(xi:Fi,…) op.derive@x(xi:Fi,…;n) op是游标或管道
 * @author RunQian
 *
 */
public class AttachDerive extends OperableFunction {
	public Object calculate(Context ctx) {
		if (AttachNew.isPseudoOper(operable, param, ctx)) {
			return pseudoCalculate(ctx);
		}
		
		int level = 0;
		if (param != null && param.getType() == IParam.Semicolon) {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("derive" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub = param.getSub(1);
			if (sub == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("derive" + mm.getMessage("function.invalidParam"));
			}
			
			Object val = sub.getLeafExpression().calculate(ctx);
			if (!(val instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("derive" + mm.getMessage("function.paramTypeError"));
			}
			
			level = ((Number)val).intValue();
			if (level < 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("derive" + mm.getMessage("function.invalidParam"));
			}
			
			param = param.getSub(0);
		} else if (option != null && option.indexOf('x') != -1) {
			level = 2;
		}

		//if (param == null && level < 2) {
		//	MessageManager mm = EngineMessage.get();
		//	throw new RQException("derive" + mm.getMessage("function.missingParam"));
		//}
		
		Expression []exps = null;
		String []names = null;
		if (param != null) {
			ParamInfo2 pi = ParamInfo2.parse(param, "derive", false, false);
			exps = pi.getExpressions1();
			names = pi.getExpressionStrs2();
		}
				
		//Derive derive = new Derive(this, exps, names, option, level);
		//if (cs != null) {
		//	derive.setCurrentCell(cs.getCurrent());
		//}
		
		//return operable.addOperation(derive, ctx);
		return operable.derive(this, exps, names, option, level, ctx);
	}
	
	private Object pseudoCalculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("derive" + mm.getMessage("function.missingParam"));
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
				throw new RQException("derive" + mm.getMessage("function.invalidParam"));
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
				return new PseudoDerive(((PseudoTable) pseudo).getPd(), obj, option);
//				else if (srcObj instanceof ClusterTableMetaData) {
//					return ((ClusterTableMetaData)srcObj).createClusterPseudo(null, null, (ClusterPseudo) obj, option, ClusterTableMetaData.TYPE_NEW);
//				}
			}
		}
		
		if (type != IParam.Comma) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("derive" + mm.getMessage("function.invalidParam"));
		} else if (param.getSubSize() < 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("derive" + mm.getMessage("function.invalidParam"));
		}
		
		Object[] objs = AttachNew.parse1stParam(param, ctx);
		Object obj = objs[0];
		String[] csNames = (String[]) objs[1];
		
		IParam newParam = param.create(1, param.getSubSize());
		ParamInfo2 pi = ParamInfo2.parse(newParam, "derive", false, false);
		Expression []exps = pi.getExpressions1();
		String []names = pi.getExpressionStrs2();
		
		return new PseudoDerive(((PseudoTable) pseudo).getPd(), obj, csNames, exps, names, 
				filter, fkNames, codes, opts, option);
	}
}
