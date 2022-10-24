package com.scudata.expression.mfn.cluster;

import java.util.ArrayList;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.ClusterTableMetaDataFunction;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.expression.ParamInfo2;
import com.scudata.expression.mfn.op.AttachNew;
import com.scudata.expression.operator.And;
import com.scudata.parallel.ClusterCursor;
import com.scudata.parallel.ClusterMemoryTable;
import com.scudata.resources.EngineMessage;

/**
 * 根据A/cs的键/维值取出T的其它字段返回成集群游标，A/cs对T的键有序
 * T.news(A/cs,x:C,…;w) T与A/cs是多对一关系，A/cs的记录会关联T的多条记录
 * @author RunQian
 *
 */
public class News extends ClusterTableMetaDataFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("news" + mm.getMessage("function.missingParam"));
		}
		if (table == null) {
			return table;
		}
		
		char type = param.getType();
		IParam param = this.param;
		Expression filter = null;
		String []fkNames = null;
		Sequence []codes = null;
		String []opts = null;
		
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
		
		if (type == IParam.Comma) {
			if (param.getSubSize() < 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("news" + mm.getMessage("function.invalidParam"));
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("news" + mm.getMessage("function.invalidParam"));
		}
		
		Object[] objs = AttachNew.parse1stParam(param, ctx);
		Object obj = objs[0];
		String[] csNames = (String[]) objs[1];
		
		if (!(obj instanceof ClusterMemoryTable) && 
			!(obj instanceof ClusterCursor)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("news" + mm.getMessage("function.invalidParam"));
		}
	
		
		IParam newParam = param.create(1, param.getSubSize());
		ParamInfo2 pi = ParamInfo2.parse(newParam, "news", false, false);
		Expression []exps = pi.getExpressions1();
		String []names = pi.getExpressionStrs2();
		return table.news(exps, names, obj, csNames, 2, option, filter, fkNames, codes, opts);
	}
}
