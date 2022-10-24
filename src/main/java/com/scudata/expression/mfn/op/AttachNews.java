package com.scudata.expression.mfn.op;

import java.util.ArrayList;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dm.op.News;
import com.scudata.dw.pseudo.IPseudo;
import com.scudata.dw.pseudo.PseudoNews;
import com.scudata.dw.pseudo.PseudoTable;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.expression.OperableFunction;
import com.scudata.expression.ParamInfo2;
import com.scudata.expression.operator.And;
import com.scudata.resources.EngineMessage;

/**
 * 对游标或管道附加创建合并新序表运算
 * op.news(X;xi:Fi,…) op是游标或管道
 * @author RunQian
 *
 */
public class AttachNews extends OperableFunction {
	public Object calculate(Context ctx) {
		if (AttachNew.isPseudoOper(operable, param, ctx)) {
			return pseudoCalculate(ctx);
		}
		
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("news" + mm.getMessage("function.missingParam"));
		} else if (param.getType() != IParam.Semicolon || param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("news" + mm.getMessage("function.invalidParam"));
		}

		IParam sub0 = param.getSub(0);
		if (sub0 == null || !sub0.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("news" + mm.getMessage("function.invalidParam"));
		}
		
		IParam sub1 = param.getSub(1);
		if (sub1 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("news" + mm.getMessage("function.invalidParam"));
		}
		
		Expression gexp = sub0.getLeafExpression();
		ParamInfo2 pi = ParamInfo2.parse(sub1, "news", false, false);
		Expression []exps = pi.getExpressions1();
		String []names = pi.getExpressionStrs2();
		
		News news = new News(this, gexp, exps, names, option);
		if (cs != null) {
			news.setCurrentCell(cs.getCurrent());
		}
		
		return operable.addOperation(news, ctx);
	}
	
	private Object pseudoCalculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("news" + mm.getMessage("function.missingParam"));
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
				throw new RQException("news" + mm.getMessage("function.invalidParam"));
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
				return new PseudoNews(((PseudoTable) pseudo).getPd(), obj, option);
//				else if (srcObj instanceof ClusterTableMetaData) {
//					return ((ClusterTableMetaData)srcObj).createClusterPseudo(null, null, (ClusterPseudo) obj, option, ClusterTableMetaData.TYPE_NEW);
//				}
			}
		}
		
		if (type != IParam.Comma) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("news" + mm.getMessage("function.invalidParam"));
		} else if (param.getSubSize() < 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("news" + mm.getMessage("function.invalidParam"));
		}
		
		Object[] objs = AttachNew.parse1stParam(param, ctx);
		Object obj = objs[0];
		String[] csNames = (String[]) objs[1];
		
		IParam newParam = param.create(1, param.getSubSize());
		ParamInfo2 pi = ParamInfo2.parse(newParam, "news", false, false);
		Expression []exps = pi.getExpressions1();
		String []names = pi.getExpressionStrs2();
		
		return new PseudoNews(((PseudoTable) pseudo).getPd(), obj, csNames, exps, names, 
				filter, fkNames, codes, opts, option);
	}
}
