package com.raqsoft.expression.mfn.cluster;

import java.util.ArrayList;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.ClusterTableMetaDataFunction;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.Node;
import com.raqsoft.expression.ParamInfo2;
import com.raqsoft.expression.operator.And;
import com.raqsoft.parallel.ClusterCursor;
import com.raqsoft.resources.EngineMessage;

/**
 * 创建集群组表游标
 * T.cursor(x:C,…;w;k:n)
 * @author RunQian
 *
 */
public class CreateCursor extends ClusterTableMetaDataFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			return table.cursor(null, null, null, null, null, null, 0, option, ctx);
		}
		
		IParam fieldParam = null; // 选出字段参数
		Expression filter = null; // 过滤条件
		
		// 做关联的字段和关联的维表
		String []fkNames = null;
		Expression []codeExps = null;
		String []opts = null;
		
		// 产生同步分段多路游标
		ClusterCursor mcs = null;
		int segCount = 0;
		
		if (param.getType() == IParam.Semicolon) {
			int size = param.getSubSize();
			if (size > 3) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("cursor" + mm.getMessage("function.invalidParam"));
			}
			
			fieldParam = param.getSub(0);
			IParam expParam = param.getSub(1);
			if (expParam == null) {
			} else if (expParam.isLeaf()) {
				filter = expParam.getLeafExpression();
			} else {
				ArrayList<Expression> expList = new ArrayList<Expression>();
				ArrayList<String> fieldList = new ArrayList<String>();
				ArrayList<Expression> codeExpList = new ArrayList<Expression>();
				ArrayList<String> optList = new ArrayList<String>();
				
				if (expParam.getType() == IParam.Colon) {
					parseFilterParam(expParam, expList, fieldList, codeExpList, optList, ctx);
				} else {
					for (int p = 0, psize = expParam.getSubSize(); p < psize; ++p) {
						IParam sub = expParam.getSub(p);
						parseFilterParam(sub, expList, fieldList, codeExpList, optList, ctx);
					}
				}
				
				int fieldCount = fieldList.size();
				if (fieldCount > 0) {
					fkNames = new String[fieldCount];
					codeExps = new Expression[fieldCount];
					fieldList.toArray(fkNames);
					codeExpList.toArray(codeExps);
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
			
			if (size > 2) {
				IParam mcsParam = param.getSub(2);
				if (mcsParam == null || ! mcsParam.isLeaf()) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("cursor" + mm.getMessage("function.invalidParam"));
				}
				
				Object obj = mcsParam.getLeafExpression().calculate(ctx);
				if (obj instanceof ClusterCursor) {
					mcs = (ClusterCursor)obj;
				} else {
					if (option == null || option.indexOf('m') == -1) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("cursor" + mm.getMessage("function.invalidParam"));
					}
					
					if (!(obj instanceof Number)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("cursor" + mm.getMessage("function.paramTypeError"));
					}

					segCount = ((Number)obj).intValue();
				}
			}
		} else {
			fieldParam = param;
		}
		
		Expression []exps = null;
		String []names = null;
		if (fieldParam != null) {
			ParamInfo2 pi = ParamInfo2.parse(fieldParam, "cursor", false, false);
			exps = pi.getExpressions1();
			names = pi.getExpressionStrs2();
		}
		
		if (mcs == null) {
			return table.cursor(exps, names, filter, fkNames, codeExps, opts, segCount, option, ctx);
		} else {
			return table.cursor(mcs, exps, names, filter, fkNames, codeExps, opts, option, ctx);
		}
	}
	
	private static void parseFilterParam(IParam param, ArrayList<Expression> expList, 
			ArrayList<String> fieldList, ArrayList<Expression> codeExpList, ArrayList<String> optList, Context ctx) {
		if (param == null) {
		} else if (param.isLeaf()) {
			expList.add(param.getLeafExpression());
		} else if (param.getType() == IParam.Colon) {
			int subSize = param.getSubSize();
			if (subSize > 3) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("cursor" + mm.getMessage("function.invalidParam"));						
			}
			
			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);
			if (sub0 == null || sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("cursor" + mm.getMessage("function.invalidParam"));
			}
			
			String fkName = sub0.getLeafExpression().getIdentifierName();
			fieldList.add(fkName);
			codeExpList.add(sub1.getLeafExpression());
			
			if (subSize > 2) {
				IParam sub2 = param.getSub(2);
				if (sub2 != null) {
					String opt = sub2.getLeafExpression().toString();
					optList.add(opt);
				} else {
					optList.add(null);
				}
			} else {
				optList.add(null);
			}
		}
	}
}
