package com.raqsoft.expression.mfn.op;

import java.util.ArrayList;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.cursor.ICursor;
import com.raqsoft.dm.op.New;
import com.raqsoft.dw.pseudo.IPseudo;
import com.raqsoft.dw.pseudo.PseudoNew;
import com.raqsoft.dw.pseudo.PseudoTable;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.Node;
import com.raqsoft.expression.OperableFunction;
import com.raqsoft.expression.ParamInfo2;
import com.raqsoft.expression.operator.And;
import com.raqsoft.resources.EngineMessage;

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
		Object obj = param.getSub(0).getLeafExpression().calculate(ctx);		
		IParam newParam = param.create(1, param.getSubSize());
		ParamInfo2 pi = ParamInfo2.parse(newParam, "new", false, false);
		Expression []exps = pi.getExpressions1();
		String []names = pi.getExpressionStrs2();
		
		return new PseudoNew(((PseudoTable) pseudo).getPd(), obj, exps, names, 
				filter, fkNames, codes, option);
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
}
