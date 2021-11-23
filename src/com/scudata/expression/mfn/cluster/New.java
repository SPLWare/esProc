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
import com.scudata.expression.operator.And;
import com.scudata.parallel.ClusterCursor;
import com.scudata.parallel.ClusterMemoryTable;
import com.scudata.resources.EngineMessage;

/**
 * 根据A/cs的键/维值取出集群组表的其它字段返回成集群游标，A/cs对T的键有序
 * T.new(A/cs,x:C,…;w)
 * @author RunQian
 *
 */
public class New extends ClusterTableMetaDataFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("new" + mm.getMessage("function.missingParam"));
		}
		if (table == null) {
			return table;
		}
		
		char type = param.getType();
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
		if (!(obj instanceof ClusterMemoryTable) && 
			!(obj instanceof ClusterCursor)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("new" + mm.getMessage("function.invalidParam"));
		}

		IParam newParam = param.create(1, param.getSubSize());
		ParamInfo2 pi = ParamInfo2.parse(newParam, "new", false, false);
		Expression []exps = pi.getExpressions1();
		String []names = pi.getExpressionStrs2();

		return table.news(exps, names, obj, 1, option, filter, fkNames, codes);
	}
}
