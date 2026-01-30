package com.scudata.dm.query;

import java.util.ArrayList;
import java.util.List;

import com.scudata.cellset.ICellSet;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.query.Select.Exp;
import com.scudata.expression.Expression;

class Join extends Relation {
	// CROSS, INNER, LEFT, RIGHT, FULL, IMPLICIT(from A, B where...)
	//public enum JoinType {
	//	INNER, LEFT, RIGHT, FULL, CROSS, IMPLICIT
	//}
	//private JoinType type;
	
	private String option = null; // 连接选项，1：左连接，f：全连接，null：内连接
	private Exp on; // 关联表达式，可空
	
	public Join(Select select) {
		this.select = select;
	}
	
	public Join(Select select, String option) {
		this.select = select;
		this.option = option;
	}

	public Exp getOn() {
		return on;
	}

	public void setOn(Exp on) {
		this.on = on;
	}
	
	public DataStruct getDataStruct() {
		throw new RuntimeException();
	}
	
	public void getAllJoinTables(ArrayList<QueryBody> tableList) {
		left.getAllJoinTables(tableList);
		tableList.add(right);
	}
	
	public void setJoinFieldName(String fieldName) {
		left.setJoinFieldName(fieldName);
		right.setJoinFieldName(fieldName);
	}
	
	private String[] createJoinFieldNames(ArrayList<QueryBody> tableList) {
		int count = tableList.size();
		String []names = new String[count];
		String name = "join" + hashCode();
		
		for (int i = 0; i < count; ++i) {
			names[i] = name + "_" + (i + 1);
		}
		
		return names;
	}
	
	private Object xjoin(ArrayList<QueryBody> tableList) {
		String []names = createJoinFieldNames(tableList);
		int count = tableList.size();
		Sequence []datas = new Sequence[count];
		
		for (int i = 0; i < count; ++i) {
			Object data = tableList.get(i).getData();
			if (data instanceof Sequence) {
				datas[i] = (Sequence)data;
			} else if (data instanceof ICursor) {
				datas[i] = ((ICursor)data).fetch();
			}
		}
		
		for (int i = 0; i < count; ++i) {
			tableList.get(i).setJoinFieldName(names[i]);
		}
		
		Context ctx = select.getContext();
		return Sequence.xjoin(datas, null, null, names, null, ctx);
	}
	
	private Object xjoin(ArrayList<QueryBody> tableList, Exp where) {
		List<And> andList = where.splitAnd();
		int tableCount = tableList.size();
		
		QueryBody leftTable = tableList.get(0);
		QueryBody rightTable = tableList.get(1);
		Object leftData = leftTable.getData();
		Object rightData = rightTable.getData();
		
		String leftFilter = null;
		String rightFilter = null;
		boolean isJoin = true;
		ArrayList<Expression> leftExpList = new ArrayList<Expression>();
		ArrayList<Expression> rightExpList = new ArrayList<Expression>();
		String xjoinFilter = null;
		
		for (int i = andList.size() - 1; i >= 0; --i) {
			And and = andList.get(i);
			if (and.isSingleTable(rightTable)) {
				andList.remove(i);
				String spl = and.getExp().toSPL();
				if (rightFilter == null) {
					rightFilter = spl;
				} else {
					rightFilter = spl + "&&" + rightFilter;
				}
			} else if (and.isSingleTable(leftTable)) {
				andList.remove(i);
				String spl = and.getExp().toSPL();
				if (leftFilter == null) {
					leftFilter = spl;
				} else {
					leftFilter = spl + "&&" + leftFilter;
				}
			} else if (and.isTable(leftTable, rightTable)) {
				andList.remove(i);
				Exp exp = and.getExp();
				String spl = exp.toSPL();
				if (xjoinFilter == null) {
					xjoinFilter = spl;
				} else {
					xjoinFilter = spl + "&&" + xjoinFilter;
				}
				
				if (isJoin) {
					boolean b = exp.splitJionExp(leftTable, rightTable, leftExpList, rightExpList);
					if (!b) {
						isJoin = false;
					}
				}
			}
		}
		
		Context ctx = select.getContext();
		ICellSet cellSet = select.getCellSet();
		Sequence leftSeq = null;
		Sequence rightSeq = null;
		
		if (leftData instanceof Sequence) {
			leftSeq = (Sequence)leftData;
			if (leftFilter != null) {
				Expression exp = new Expression(cellSet, ctx, leftFilter);
				leftSeq = (Sequence)leftSeq.select(exp, null, ctx);
			}
		} else if (leftData instanceof ICursor) {
			ICursor cs = (ICursor)leftData;
			if (leftFilter != null) {
				Expression exp = new Expression(cellSet, ctx, leftFilter);
				cs.select(null, exp, null, ctx);
			}
			
			leftSeq = cs.fetch();
		}
		
		if (rightData instanceof Sequence) {
			rightSeq = (Sequence)rightData;
			if (rightFilter != null) {
				Expression exp = new Expression(cellSet, ctx, rightFilter);
				rightSeq = (Sequence)rightSeq.select(exp, null, ctx);
			}
		} else if (rightData instanceof ICursor) {
			ICursor cs = (ICursor)rightData;
			if (rightFilter != null) {
				Expression exp = new Expression(cellSet, ctx, rightFilter);
				cs.select(null, exp, null, ctx);
			}
			
			rightSeq = cs.fetch();
		}
		
		String []allNames = createJoinFieldNames(tableList);
		Sequence []sequences = new Sequence[] {leftSeq, rightSeq};
		String []names = new String[] {allNames[0], allNames[1]};
		Sequence result;
		
		if (isJoin) {
			int fcount = leftExpList.size();
			Expression []exps1 = new Expression[fcount];
			Expression []exps2 = new Expression[fcount];
			leftExpList.toArray(exps1);
			rightExpList.toArray(exps2);
			Expression [][]totalExps = new Expression[][] {exps1, exps2};
			result = Sequence.join(sequences, totalExps, names, null, ctx);
		} else {
			Expression []exps = new Expression[2];
			if (xjoinFilter != null) {
				exps[1] = new Expression(cellSet, ctx, xjoinFilter);
			}
			
			result = Sequence.xjoin(sequences, exps, null, names, null, ctx);
		}
		
		leftTable.setJoinFieldName(allNames[0]);
		rightTable.setJoinFieldName(allNames[1]);
		
		for (int t = 2; t < tableCount; ++t) {
			rightFilter = null;
			isJoin = true;
			leftExpList.clear();
			rightExpList.clear();
			xjoinFilter = null;
			rightTable = tableList.get(t);
			
			for (int i = andList.size() - 1; i >= 0; --i) {
				And and = andList.get(i);
				if (and.isSingleTable(rightTable)) {
					andList.remove(i);
					String spl = and.getExp().toSPL();
					if (rightFilter == null) {
						rightFilter = spl;
					} else {
						rightFilter = spl + "&&" + rightFilter;
					}
				} else if (and.isTable(tableList, t)) {
					andList.remove(i);
					Exp exp = and.getExp();
					String spl = exp.toSPL();
					if (xjoinFilter == null) {
						xjoinFilter = spl;
					} else {
						xjoinFilter = spl + "&&" + xjoinFilter;
					}
					
					if (isJoin) {
						boolean b = exp.splitJionExp(tableList, t, leftExpList, rightExpList);
						if (!b) {
							isJoin = false;
						}
					}
				}
			}
			
			rightData = rightTable.getData();
			if (rightData instanceof Sequence) {
				rightSeq = (Sequence)rightData;
				if (rightFilter != null) {
					Expression exp = new Expression(cellSet, ctx, rightFilter);
					rightSeq = (Sequence)rightSeq.select(exp, null, ctx);
				}
			} else if (rightData instanceof ICursor) {
				ICursor cs = (ICursor)rightData;
				if (rightFilter != null) {
					Expression exp = new Expression(cellSet, ctx, rightFilter);
					cs.select(null, exp, null, ctx);
				}
				
				rightSeq = cs.fetch();
			}
			
			sequences[0] = result;
			sequences[1] = rightSeq;
			names[0] = "_1";
			names[1] = allNames[t];
			
			if (isJoin) {
				int fcount = leftExpList.size();
				Expression []exps1 = new Expression[fcount];
				Expression []exps2 = new Expression[fcount];
				leftExpList.toArray(exps1);
				rightExpList.toArray(exps2);
				Expression [][]totalExps = new Expression[][] {exps1, exps2};
				result = Sequence.join(sequences, totalExps, names, null, ctx);
			} else {
				Expression []exps = new Expression[2];
				if (xjoinFilter != null) {
					exps[1] = new Expression(cellSet, ctx, xjoinFilter);
				}
				
				result = Sequence.xjoin(sequences, exps, null, names, null, ctx);
			}
			
			rightTable.setJoinFieldName(allNames[t]);
			int curTableCount = t + 1;
			Expression []newExps = new Expression[curTableCount];
			String []newNames = new String[curTableCount];
			System.arraycopy(allNames, 0, newNames, 0, curTableCount);
			
			for (int i = 0; i < t; ++i) {
				newExps[i] = new Expression(cellSet, ctx, "#1.#" + (i + 1));
			}
			
			newExps[t] = new Expression(cellSet, ctx, "#2");
			result = result.newTable(newNames, newExps, ctx);
		}
		
		return result;
	}
	
	public Object getData(Exp where) {
		ArrayList<QueryBody> tableList = new ArrayList<QueryBody>();
		getAllJoinTables(tableList);
		if (on == null) {
			if (where == null) {
				return xjoin(tableList);
			} else {
				return xjoin(tableList, where);
			}
		}

		int lastTable = tableList.size() - 1;
		List<And> andList = on.splitAnd();
		String leftFilter = null;
		String rightFilter = null;
		boolean isJoin = true;
		ArrayList<Expression> leftExpList = new ArrayList<Expression>();
		ArrayList<Expression> rightExpList = new ArrayList<Expression>();
		String xjoinFilter = null;
		
		for (int i = andList.size() - 1; i >= 0; --i) {
			And and = andList.get(i);
			if (and.isSingleTable(right)) {
				andList.remove(i);
				String spl = and.getExp().toSPL();
				if (rightFilter == null) {
					rightFilter = spl;
				} else {
					rightFilter = spl + "&&" + rightFilter;
				}
			} else if (and.containTable(right)) {
				andList.remove(i);
				Exp exp = and.getExp();
				String spl = exp.toSPL();
				if (xjoinFilter == null) {
					xjoinFilter = spl;
				} else {
					xjoinFilter = spl + "&&" + xjoinFilter;
				}
				
				if (isJoin) {
					boolean b = exp.splitJionExp(tableList, lastTable, leftExpList, rightExpList);
					if (!b) {
						isJoin = false;
					}
				}
			} else {
				andList.remove(i);
				String spl = and.getExp().toSPL();
				if (leftFilter == null) {
					leftFilter = spl;
				} else {
					leftFilter = spl + "&&" + leftFilter;
				}
			}
		}
		
		if ((option == null || option.equals("1")) && where != null) {
			andList = where.splitAnd();
			ArrayList<Exp> resultList = new ArrayList<Exp>();
			boolean sign = true;
			
			for (And and : andList) {
				if (and.isSingleTable(right)) {
					if (option != null && option.equals("1")) {
						resultList.add(and.getExp());
					} else {
						sign = false;
						String spl = and.getExp().toSPL();
						if (rightFilter == null) {
							rightFilter = spl;
						} else {
							rightFilter = spl + "&&" + rightFilter;
						}
					}
				} else if (and.containTable(right)) {
					resultList.add(and.getExp());
				} else {
					sign = false;
					String spl = and.getExp().toSPL();
					if (leftFilter == null) {
						leftFilter = spl;
					} else {
						leftFilter = spl + "&&" + leftFilter;
					}
				}
			}
			
			if (!sign) {
				where = select.toAndExp(resultList);
			}
		}

		Object leftData = left.select(leftFilter);
		Object rightData = right.select(rightFilter);
		
		Context ctx = select.getContext();
		ICellSet cellSet = select.getCellSet();
		Sequence leftSeq = null;
		Sequence rightSeq = null;
		
		if (leftData instanceof Sequence) {
			leftSeq = (Sequence)leftData;
		} else if (leftData instanceof ICursor) {
			ICursor cs = (ICursor)leftData;
			leftSeq = cs.fetch();
		}
		
		if (rightData instanceof Sequence) {
			rightSeq = (Sequence)rightData;
		} else if (rightData instanceof ICursor) {
			ICursor cs = (ICursor)rightData;
			rightSeq = cs.fetch();
		}
		
		Sequence result;
		String name = "join" + hashCode();
		Sequence []sequences = new Sequence[] {leftSeq, rightSeq};
		String []names = new String[] {name + "_1", name + "_2"};
		int fcount = leftExpList.size();
		
		if (isJoin && fcount > 0) {
			Expression []exps1 = new Expression[fcount];
			Expression []exps2 = new Expression[fcount];
			leftExpList.toArray(exps1);
			rightExpList.toArray(exps2);
			Expression [][]totalExps = new Expression[][] {exps1, exps2};
			result = Sequence.join(sequences, totalExps, names, option, ctx);
		} else {
			Expression []exps = new Expression[2];
			if (xjoinFilter != null) {
				exps[1] = new Expression(cellSet, ctx, xjoinFilter);
			}
			
			result = Sequence.xjoin(sequences, exps, null, names, option, ctx);
		}
		
		right.setJoinFieldName(names[1]);
		if (lastTable == 1) {
			left.setJoinFieldName(names[0]);
		} else {
			Expression []newExps = new Expression[lastTable + 1];
			String []newNames = new String[lastTable + 1];
			String []leftNames = leftSeq.dataStruct().getFieldNames();
			System.arraycopy(leftNames, 0, newNames, 0, lastTable);
			newNames[lastTable] = names[1];
			
			for (int i = 0; i < lastTable; ++i) {
				newExps[i] = new Expression(cellSet, ctx, "#1.#" + (i + 1));
			}
			
			newExps[lastTable] = new Expression(cellSet, ctx, "#2");
			result = result.newTable(newNames, newExps, ctx);
		}
		
		if (where == null) {
			return result;
		} else {
			String spl = where.toSPL();
			Expression exp = new Expression(cellSet, ctx, spl);
			return result.select(exp, null, ctx);
		}
	}
}
