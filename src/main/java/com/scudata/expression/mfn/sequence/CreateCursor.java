package com.scudata.expression.mfn.sequence;

import java.util.ArrayList;

import com.scudata.array.ObjectArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Env;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.cursor.IMultipath;
import com.scudata.dm.cursor.MemoryCursor;
import com.scudata.dm.cursor.MultipathCursors;
import com.scudata.dm.op.DiffJoin;
import com.scudata.dm.op.New;
import com.scudata.dm.op.Select;
import com.scudata.dm.op.Switch;
import com.scudata.dw.MemoryTable;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.expression.ParamInfo2;
import com.scudata.expression.SequenceFunction;
import com.scudata.expression.operator.And;
import com.scudata.resources.EngineMessage;
import com.scudata.util.CursorUtil;

/**
 * 把序列转成游标或多路游标
 * A.cursor(k:n) A.cursor@m(n) A.cursor@m(mcs,K:K‘,...)
 * @author RunQian
 *
 */
public class CreateCursor extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (srcSequence instanceof MemoryTable) {
			return createCursor((MemoryTable)srcSequence, param, option, ctx);
		} else if (srcSequence.ifn() instanceof ICursor) {
			int len = srcSequence.length();
			ObjectArray array = new ObjectArray(len);
			
			for (int i = 1; i <= len; ++i) {
				Object obj = srcSequence.getMem(i);
				if (obj instanceof IMultipath) {
					ICursor []cursors = ((IMultipath)obj).getParallelCursors();
					array.addAll(cursors);
				} else if (obj instanceof ICursor) {
					array.add(obj);
				} else if (obj != null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mcursor" + mm.getMessage("function.paramTypeError"));
				}
			}
			
			ICursor[] cursors = new ICursor[array.size()];
			array.toArray(cursors);
			return new MultipathCursors(cursors, ctx);
		} else {
			if (option == null || option.indexOf('m') == -1) {
				return createCursor(srcSequence, param, option, ctx);
			} else {
				return createMultipathCursor(srcSequence, param, option, ctx);
			}
		}
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
		
	// 由内表创建游标，参数同由组表创建游标相同
	private static ICursor createCursor(MemoryTable table, IParam param, String opt, Context ctx) {
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
		
		IMultipath mcs = null; // 同步分段游标
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

			int colCount = names.length;
			for (int i = 0; i < colCount; ++i) {
				if (names[i] == null || names[i].length() == 0) {
					if (exps[i] == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("cursor" + mm.getMessage("function.invalidParam"));
					}

					names[i] = exps[i].getIdentifierName();
				} else {
					if (exps[i] == null) {
						exps[i] = Expression.NULL;
					}
				}
			}
		}

		ICursor cs;
		if (mcs != null) {
			cs = createSyncCursor(table, mcs, null, null, opt, ctx);
		} else {
			cs = table.cursor(segSeq, segCount, ctx);
		}
		
		Switch switchOp = null;
		if (fkNames != null) {
			int fkCount = fkNames.length;
			ArrayList<Expression[]> diffExpList = new ArrayList<Expression[]>();
			ArrayList<Sequence> diffCodeList = new ArrayList<Sequence>();
			
			ArrayList<String> switchFkList = new ArrayList<String>();
			ArrayList<Sequence> switchCodeList = new ArrayList<Sequence>();
			ArrayList<Expression> switchExpList = new ArrayList<Expression>();
			
			for (int i = 0; i < fkCount; ++i) {
				if (opts[i] == null) {
					switchFkList.add(fkNames[i]);
					switchCodeList.add(codes[i]);
					switchExpList.add(null);
				} else if (opts[i].equals("null")) {
					Expression exp = new Expression(ctx, fkNames[i]);
					diffExpList.add(new Expression[] {exp});
					diffCodeList.add(codes[i]);
				} else if (opts[i].equals("#")) {
					switchFkList.add(fkNames[i]);
					switchCodeList.add(codes[i]);
					Expression exp = new Expression("#");
					switchExpList.add(exp);
				}
			}
			
			int diffCount = diffExpList.size();
			if (diffCount > 0) {
				Expression [][]diffExps = new Expression[diffCount][];
				Sequence []diffCodes = new Sequence[diffCount];
				diffExpList.toArray(diffExps);
				diffCodeList.toArray(diffCodes);
				
				DiffJoin diff = new DiffJoin(diffExps, diffCodes, new Expression[diffCount][]);
				cs.addOperation(diff, ctx);
			}
			
			int switchCount = switchFkList.size();
			if (switchCount > 0) {
				String []switchFkNames = new String[switchCount];
				Sequence []switchCodes = new Sequence[switchCount];
				Expression []switchExps = new Expression[switchCount];
				switchFkList.toArray(switchFkNames);
				switchCodeList.toArray(switchCodes);
				switchExpList.toArray(switchExps);
				switchOp = new Switch(switchFkNames, switchCodes, switchExps, "i");
			}
		}
		
		if (filter != null) {
			Select select = new Select(filter, null);
			cs.addOperation(select, ctx);
		}
		
		if (names != null) {
			New newOp = new New(exps, names, null);
			cs.addOperation(newOp, ctx);
		} else {
			com.scudata.dm.op.Derive deriveOp = new com.scudata.dm.op.Derive(null, null, null);
			cs.addOperation(deriveOp, ctx);
		}
		
		if (switchOp != null) {
			cs.addOperation(switchOp, ctx);
		}
		return cs;
	}
	
	private static ICursor createSyncCursor(Sequence seq, IMultipath mcs, String []keys1, String []keys2, String opt, Context ctx) {
		int len = seq.length();
		ICursor []cursors = mcs.getParallelCursors();
		int pathCount = cursors.length;
		
		if (opt != null && opt.indexOf('p') != -1) {
			keys1 = keys2 = new String[] {"#1"};
		} else if (keys1 == null) {
			DataStruct ds = seq.dataStruct();
			if (ds == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("ds.lessKey"));
			}
			
			keys1 = ds.getPrimary();
			if (keys1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("ds.lessKey"));
			}
			
			Sequence data = cursors[0].peek(1);
			if (data == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("ds.lessKey"));
			}
			
			ds = data.dataStruct();
			if (ds == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("ds.lessKey"));
			}
			
			keys2 = ds.getPrimary();
			if (keys2 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("ds.lessKey"));
			}
			
			int fcount1 = keys1.length;
			int fcount2 = keys2.length;
			if (fcount1 > fcount2) {
				String []tmp = new String[fcount2];
				System.arraycopy(keys1, 0, tmp, 0, fcount2);
				keys1 = tmp;
			} else if (fcount1 < fcount2) {
				String []tmp = new String[fcount1];
				System.arraycopy(keys2, 0, tmp, 0, fcount1);
				keys2 = tmp;
			}
		}
		
		int fcount = keys1.length;
		Object [][]minValues = new Object [pathCount][];
		
		for (int i = 0; i < pathCount; ++i) {
			Sequence data = cursors[i].peek(1);
			if (data == null) {
				throw new RQException("Less data.");
			}

			BaseRecord r = (BaseRecord)data.get(1);
			Object []vals = new Object[fcount];
			minValues[i] = vals;
			for (int f = 0; f < fcount; ++f) {
				vals[f] = r.getFieldValue(keys2[f]);
			}
		}
		
		ICursor []resultCursors = new ICursor[pathCount];
		Expression []exps = new Expression[fcount];
		for (int f = 0; f < fcount; ++f) {
			exps[f] = new Expression(ctx, keys1[f]);
		}
		
		int start = 1;
		for (int i = 1; i < pathCount; ++i) {
			int index = (Integer)seq.pselect(exps, minValues[i], start, "s", ctx);
			if (index < 0) {
				index = -index;
			}
			
			resultCursors[i - 1] = seq.cursor(start, index);
			start = index;
		}
		
		resultCursors[pathCount - 1] = seq.cursor(start, len + 1);
		return new MultipathCursors(resultCursors, ctx);
	}
	
	private static ICursor createMultipathCursor(Sequence seq, IParam param, String opt, Context ctx) {
		int pathCount;
		if (param == null) {
			pathCount = Env.getCursorParallelNum();
		} else if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (obj instanceof Number) {
				pathCount = ((Number)obj).intValue();
			} else if (obj instanceof IMultipath) {
				return createSyncCursor(seq, (IMultipath)obj, null, null, opt, ctx);
			} else if (obj instanceof ICursor) {
				pathCount = 0;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("cursor" + mm.getMessage("function.paramTypeError"));
			}
		} else if (param.getType() == IParam.Comma) {
			int paramCount = param.getSubSize();
			IParam sub = param.getSub(0);
			if (sub == null || !sub.isLeaf()) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("cursor" + mm.getMessage("function.invalidParam"));					
			}
			
			Object obj = sub.getLeafExpression().calculate(ctx);
			if (obj instanceof IMultipath) {
				String []keys1 = new String[paramCount - 1];
				String []keys2 = new String[paramCount - 1];
				for (int i = 1; i < paramCount; ++i) {
					sub = param.getSub(i);
					if (sub == null || sub.getSubSize() != 2) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("cursor" + mm.getMessage("function.invalidParam"));					
					}
					
					IParam sub0 = sub.getSub(0);
					IParam sub1 = sub.getSub(1);
					if (sub0 == null || sub1 == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("cursor" + mm.getMessage("function.invalidParam"));					
					}
					
					keys1[i - 1] = sub0.getLeafExpression().getIdentifierName();
					keys2[i - 1] = sub1.getLeafExpression().getIdentifierName();
				}
				
				return createSyncCursor(seq, (IMultipath)obj, keys1, keys2, opt, ctx);
			} else if (obj instanceof ICursor) {
				return new MemoryCursor(seq);
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("cursor" + mm.getMessage("function.paramTypeError"));
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("cursor" + mm.getMessage("function.invalidParam"));
		}
		
		return CursorUtil.cursor(seq, pathCount, opt, ctx);
	}
	
	// 由内存序列创建游标
	private static ICursor createCursor(Sequence seq, IParam param, String opt, Context ctx) {
		if (param == null) {
			return seq.cursor();
		} else if (param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("cursor" + mm.getMessage("function.invalidParam"));
		}
		
		IParam sub0 = param.getSub(0);
		IParam sub1 = param.getSub(1);
		if (sub0 == null || sub1 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("cursor" + mm.getMessage("function.invalidParam"));
		}
		
		Object val0= sub0.getLeafExpression().calculate(ctx);
		if (!(val0 instanceof Number)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("cursor" + mm.getMessage("function.paramTypeError"));
		}
		
		Object val1= sub1.getLeafExpression().calculate(ctx);
		if (!(val1 instanceof Number)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("cursor" + mm.getMessage("function.paramTypeError"));
		}
		
		int segSeq = ((Number)val0).intValue();
		int segCount = ((Number)val1).intValue();
		if (segSeq < 1 || segSeq > segCount) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("cursor" + mm.getMessage("function.invalidParam"));
		}
		
		return CursorUtil.cursor(seq, segSeq, segCount, opt, ctx);
	}
}
