package com.scudata.expression.mfn.dw;

import java.util.ArrayList;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Env;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ConjxCursor;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.cursor.MergeCursor2;
import com.scudata.dm.cursor.MultipathCursors;
import com.scudata.dw.IDWCursor;
import com.scudata.dw.IPhyTable;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.expression.ParamInfo2;
import com.scudata.expression.PhyTableFunction;
import com.scudata.expression.operator.And;
import com.scudata.resources.EngineMessage;

/**
 * 创建组表游标
 * T.cursor(x:C,…;w;k:n)
 * @author RunQian
 *
 */
public class CreateCursor extends PhyTableFunction {
	public Object calculate(Context ctx) {
		ICursor cs = createCursor(table, param, option, ctx);
		if (option != null && option.indexOf('x') != -1) {
			setOptionX(cs, option);
		}
		return cs;
	}
	
	private static void parseFilterParam(IParam param, ArrayList<Expression> expList, 
			ArrayList<String> fieldList, ArrayList<Sequence> codeList, ArrayList<String> optList, Context ctx) {
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
			Object val = sub1.getLeafExpression().calculate(ctx);
			if (val instanceof Sequence) {
				codeList.add((Sequence)val);
			} else if (val == null) {
				codeList.add(new Sequence());
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("join" + mm.getMessage("function.paramTypeError"));
			}
			
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
	
	public static void setOptionX(ICursor cs, String opt) {
		if (cs instanceof IDWCursor) {
			((IDWCursor) cs).setOption(opt);
		} else if (cs instanceof MultipathCursors) {
			MultipathCursors mcs = (MultipathCursors) cs;
			ICursor[] cursors = mcs.getCursors();
			for (ICursor cursor : cursors) {
				setOptionX(cursor, opt);
			}
		} else if (cs instanceof MergeCursor2) {
			MergeCursor2 mcs = (MergeCursor2) cs;
			setOptionX(mcs.getCursor1(), opt);
			setOptionX(mcs.getCursor2(), opt);
		} else if (cs instanceof ConjxCursor) {
			ConjxCursor mcs = (ConjxCursor) cs;
			ICursor[] cursors = mcs.getCursors();
			for (ICursor cursor : cursors) {
				setOptionX(cursor, opt);
			}
		}
	}
	
	public static ICursor createCursor(IPhyTable table, IParam param, String opt, Context ctx) {
		// 是否产生多路游标
		boolean isMultiThread = opt != null && opt.indexOf('m') != -1;
		
		if (param == null && !isMultiThread) {
			return table.cursor();
		}
		
		IParam fieldParam = null; // 选出字段参数
		Expression filter = null; // 过滤条件
		
		// 做关联的字段和关联的维表
		String []fkNames = null;
		Sequence []codes = null;
		String []opts = null;
		
		MultipathCursors mcs = null; // 同步分段游标
		int segSeq = 0;
		int segCount = 0;
		if (isMultiThread) {
			segCount = Env.getCursorParallelNum();
		}
		
		if (param != null && param.getType() == IParam.Semicolon) {
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
				ArrayList<Sequence> codeList = new ArrayList<Sequence>();
				ArrayList<String> optList = new ArrayList<String>();
				
				if (expParam.getType() == IParam.Colon) {
					parseFilterParam(expParam, expList, fieldList, codeList, optList, ctx);
				} else {
					for (int p = 0, psize = expParam.getSubSize(); p < psize; ++p) {
						IParam sub = expParam.getSub(p);
						parseFilterParam(sub, expList, fieldList, codeList, optList, ctx);
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
			
			if (size > 2) {
				IParam segParam = param.getSub(2);
				if (segParam == null) {
				} else if (segParam.isLeaf()) {
					Object obj = segParam.getLeafExpression().calculate(ctx);
					if (obj instanceof MultipathCursors) {
						mcs = (MultipathCursors)obj;
					} else if (obj instanceof ICursor) {
						// cursor@m如果结果集为空或者满足条件的块小于2可能不会返回多路游标
						isMultiThread = false;
					} else {
						if (!isMultiThread) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("cursor" + mm.getMessage("function.invalidParam"));
						}
						
						if (!(obj instanceof Number)) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("cursor" + mm.getMessage("function.paramTypeError"));
						}

						segCount = ((Number)obj).intValue();
					}
				} else {
					if (segParam.getSubSize() != 2) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("cursor" + mm.getMessage("function.invalidParam"));
					}
					
					IParam sub0 = segParam.getSub(0);
					IParam sub1 = segParam.getSub(1);
					if (sub0 == null || sub1 == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("cursor" + mm.getMessage("function.invalidParam"));
					}
					
					Object obj = sub0.getLeafExpression().calculate(ctx);
					if (!(obj instanceof Number)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("cursor" + mm.getMessage("function.paramTypeError"));
					}

					segSeq = ((Number)obj).intValue();
					obj = sub1.getLeafExpression().calculate(ctx);
					if (!(obj instanceof Number)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("cursor" + mm.getMessage("function.paramTypeError"));
					}

					segCount = ((Number)obj).intValue();
					if (segSeq < 1 || segSeq > segCount) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("cursor" + mm.getMessage("function.invalidParam"));
					}
					
					isMultiThread = false;
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
		
		if (mcs != null) {
			return table.cursor(exps, names, filter, fkNames, codes, opts, mcs, opt, ctx);
		} else if (isMultiThread && segCount > 1) {
			return table.cursor(exps, names, filter, fkNames, codes, opts, segCount, opt, ctx);
		} else {
			if (segSeq < 1) {
				return table.cursor(exps, names, filter, fkNames, codes, opts, opt, ctx);
			} else {
				return table.cursor(exps, names, filter, fkNames, codes, opts, segSeq, segCount, opt, ctx);
			}
		}
	}
	
	public boolean isLeftTypeMatch(Object obj) {
		if (obj instanceof IPhyTable) {
			if (option != null && option.indexOf('v') != -1)
				return false;
			return true;
		}
		
		return false;
	}
}
