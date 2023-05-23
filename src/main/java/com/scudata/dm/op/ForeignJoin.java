package com.scudata.dm.op;

import com.scudata.array.BoolArray;
import com.scudata.array.IArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.ComputeStack;
import com.scudata.dm.Context;
import com.scudata.dm.Current;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Param;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 对游标或管道附加bind运算
 * @author WangXiaoJun
 *
 */
public class ForeignJoin extends Operation {
	private Expression []dimExps; // 关联字段表达式数组
	private String []aliasNames; // 别名
	private Expression [][]newExps; // 取出的代码表的字段表达式数组
	private String [][]newNames; // 取出的代码表的字段名数组
	private String opt; // 选项
	
	private DataStruct oldDs; // 源表数据结构
	private DataStruct newDs; // 结果集数据结构
	private int [][]tgtIndexs; // newExps字段在结果集的位置
	private boolean isIsect; // 交连接，默认为左连接
	
	public ForeignJoin(Expression[] dimExps, String []aliasNames, Expression[][] newExps, String[][] newNames, String opt) {
		this(null, dimExps, aliasNames, newExps, newNames, opt);
	}

	public ForeignJoin(Function function, Expression[] dimExps, String []aliasNames, 
			Expression[][] newExps, String[][] newNames, String opt) {
		super(function);
		this.dimExps = dimExps;
		this.aliasNames = aliasNames;
		this.newExps = newExps;
		this.opt = opt;
		
		int count = newExps.length;
		if (newNames == null) {
			newNames = new String[count][];
		}
		
		this.newNames = newNames;
		isIsect = opt != null && opt.indexOf('i') != -1;
		
		for (int i = 0; i < count; ++i) {
			Expression []curExps = newExps[i];
			if (curExps == null) {
				continue;
			}
			
			int curLen = curExps.length;
			if (newNames[i] == null) {
				newNames[i] = new String[curLen];
			}
			
			String []curNames = newNames[i];
			for (int j = 0; j < curLen; ++j) {
				if (curNames[j] == null || curNames[j].length() == 0) {
					curNames[j] = curExps[j].getFieldName();
				}
			}
		}
	}
	
	/**
	 * 取操作是否会减少元素数，比如过滤函数会减少记录
	 * 此函数用于游标的精确取数，如果附加的操作不会使记录数减少则只需按传入的数量取数即可
	 * @return true：会，false：不会
	 */
	public boolean isDecrease() {
		return isIsect;
	}
	
	/**
	 * 复制运算用于多线程计算，因为表达式不能多线程计算
	 * @param ctx 计算上下文
	 * @return Operation
	 */
	public Operation duplicate(Context ctx) {
		Expression []dimExps1 = dupExpressions(dimExps, ctx);
		Expression [][]newExps1 = dupExpressions(newExps, ctx);
		return new ForeignJoin(function, dimExps1, aliasNames, newExps1, newNames, opt);
	}

	private void init(Sequence data, Context ctx) {
		if (newDs != null) {
			return;
		}
		
		oldDs = data.dataStruct();
		if (oldDs == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.needPurePmt"));
		}
		
		Sequence seq = new Sequence();
		String []oldKey = oldDs.getPrimary();
		seq.addAll(oldDs.getFieldNames());
		int dcount = newNames.length;
		tgtIndexs = new int[dcount][];
		
		for (int i = 0; i < dcount; ++i) {
			String []curNames = newNames[i];
			if (curNames == null) {
				continue;
			}
			
			int curLen = curNames.length;
			int []tmp = new int[curLen];
			tgtIndexs[i] = tmp;
			
			for (int f = 0; f < curLen; ++f) {
				// 如果新加的字段在源表中已存在则改写现有字段
				int index = oldDs.getFieldIndex(curNames[f]);
				if (index == -1) {
					tmp[f] = seq.length();
					seq.add(curNames[f]);
				} else {
					tmp[f] = index;
				}
			}
		}

		String []names = new String[seq.length()];
		seq.toArray(names);
		newDs = new DataStruct(names);
		if (oldKey != null) {
			newDs.setPrimary(oldKey);
		}
	}
	
	/**
	 * 处理游标或管道当前推送的数据
	 * @param seq 数据
	 * @param ctx 计算上下文
	 * @return
	 */
	public Sequence process(Sequence seq, Context ctx) {
		init(seq, ctx);
		
		if (isIsect) {
			return join_i(seq, ctx);
		} else {
			return join(seq, ctx);
		}
	}
	
	private Sequence join(Sequence data, Context ctx) {
		int len = data.length();
		Table result = new Table(newDs, len);
		ComputeStack stack = ctx.getComputeStack();
		
		// 允许后面C引用前面的F，可以一次性join多层
		for (int i = 1; i <= len; ++i) {
			BaseRecord old = (BaseRecord)data.getMem(i);
			result.newLast(old.getFieldValues());
		}
		
		Current current = new Current(result);
		stack.push(current);

		try {
			for (int fk = 0, fkCount = dimExps.length; fk < fkCount; ++fk) {
				Expression dimExp = dimExps[fk];
				int []tgtIndexs = this.tgtIndexs[fk];
				
				if (tgtIndexs == null) {
					for (int i = 1; i <= len; ++i) {
						current.setCurrent(i);
						dimExp.calculate(ctx);
					}
				} else {
					Sequence dimData = new Sequence(len);
					IArray mems = dimData.getMems();
					
					for (int i = 1; i <= len; ++i) {
						current.setCurrent(i);
						mems.push(dimExp.calculate(ctx));
					}
					
					Param param = null;
					Object oldValue = null;
					if (aliasNames != null && aliasNames[fk] != null) {
						param = ctx.getParam(aliasNames[fk]);
						if (param == null) {
							param = new Param(aliasNames[fk], Param.VAR, dimData);
							ctx.addParam(param);
						} else {
							oldValue = param.getValue();
							param.setValue(dimData);
						}
					}
					
					Expression []curNewExps = newExps[fk];
					int newCount = curNewExps.length;
					
					try {
						Current dimCurrent = new Current(dimData);
						stack.push(dimCurrent);
						
						if (param == null) {
							for (int i = 1; i <= len; ++i) {
								current.setCurrent(i);
								dimCurrent.setCurrent(i);
								
								BaseRecord r = (BaseRecord)result.getMem(i);
								for (int f = 0; f < newCount; ++f) {
									r.setNormalFieldValue(tgtIndexs[f], curNewExps[f].calculate(ctx));
								}
							}
						} else {
							for (int i = 1; i <= len; ++i) {
								current.setCurrent(i);
								dimCurrent.setCurrent(i);
								param.setValue(mems.get(i));
								
								BaseRecord r = (BaseRecord)result.getMem(i);
								for (int f = 0; f < newCount; ++f) {
									r.setNormalFieldValue(tgtIndexs[f], curNewExps[f].calculate(ctx));
								}
							}
						}
					} finally {
						stack.pop();
						if (param != null) {
							param.setValue(oldValue);
						}
					}
				}
			}
		} finally {
			stack.pop();
		}

		return result;
	}
	
	private Table join_i(Sequence data, Context ctx) {
		int len = data.length();
		Table result = new Table(newDs, len);
		boolean []signs = new boolean[len + 1];
		for (int i = 1; i <= len; ++i) {
			signs[i] = true;
		}
		
		// 允许后面C引用前面的F，可以一次性join多层
		for (int i = 1; i <= len; ++i) {
			BaseRecord old = (BaseRecord)data.getMem(i);
			result.newLast(old.getFieldValues());
		}
		
		ComputeStack stack = ctx.getComputeStack();
		Current current = new Current(result);
		stack.push(current);

		try {
			for (int fk = 0, fkCount = dimExps.length; fk < fkCount; ++fk) {
				Expression dimExp = dimExps[fk];
				int []tgtIndexs = this.tgtIndexs[fk];
				
				if (tgtIndexs == null) {
					for (int i = 1; i <= len; ++i) {
						if (signs[i]) {
							current.setCurrent(i);
							Object dr = dimExp.calculate(ctx);
							if (Variant.isFalse(dr)) {
								signs[i] = false;
							}
						}
					}
				} else {
					Sequence dimData = new Sequence(len);
					IArray mems = dimData.getMems();
					
					for (int i = 1; i <= len; ++i) {
						if (signs[i]) {
							current.setCurrent(i);
							Object dr = dimExp.calculate(ctx);
							mems.push(dr);
							
							if (Variant.isFalse(dr)) {
								signs[i] = false;
							}
						} else {
							mems.pushNull();
						}
					}
					
					Param param = null;
					Object oldValue = null;
					if (aliasNames != null && aliasNames[fk] != null) {
						param = ctx.getParam(aliasNames[fk]);
						if (param == null) {
							param = new Param(aliasNames[fk], Param.VAR, dimData);
							ctx.addParam(param);
						} else {
							oldValue = param.getValue();
							param.setValue(dimData);
						}
					}
					
					Expression []curNewExps = newExps[fk];
					int newCount = curNewExps.length;
					
					try {
						Current dimCurrent = new Current(dimData);
						stack.push(dimCurrent);
						
						if (param == null) {
							for (int i = 1; i <= len; ++i) {
								if (signs[i]) {
									current.setCurrent(i);
									dimCurrent.setCurrent(i);
									
									BaseRecord r = (BaseRecord)result.getMem(i);
									for (int f = 0; f < newCount; ++f) {
										r.setNormalFieldValue(tgtIndexs[f], curNewExps[f].calculate(ctx));
									}
								}
							}
						} else {
							for (int i = 1; i <= len; ++i) {
								if (signs[i]) {
									current.setCurrent(i);
									dimCurrent.setCurrent(i);
									param.setValue(mems.get(i));
									
									BaseRecord r = (BaseRecord)result.getMem(i);
									for (int f = 0; f < newCount; ++f) {
										r.setNormalFieldValue(tgtIndexs[f], curNewExps[f].calculate(ctx));
									}
								}
							}
						}
					} finally {
						stack.pop();
						if (param != null) {
							param.setValue(oldValue);
						}
					}
				}
			}
		} finally {
			stack.pop();
		}

		IArray mems = result.getMems();
		BoolArray signArray = new BoolArray(signs, len);
		mems = mems.select(signArray);
		result.setMems(mems);
		return result;
	}
}
