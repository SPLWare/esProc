package com.scudata.dm.query;

import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ConjxCursor;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.cursor.MemoryCursor;
import com.scudata.dm.query.Select.Exp;
import com.scudata.expression.Expression;
import com.scudata.util.CursorUtil;

// 集合运算，连、并、交、差
class SetOperation extends Relation {
    public enum Type {
        UNION, UNIONALL, INTERSECT, MINUS
    }

    private Type type;
    
    public SetOperation(Type type) {
    	this.type = type;
    }
    
    public DataStruct getDataStruct() {
    	return left.getDataStruct();
    }

	public Object getData(Exp where) {
		Object data1 = left.getData(where);
		Object data2 = right.getData(where);
		
		if (data1 == null) {
			if (type == Type.UNIONALL || type == Type.UNION) {
				return data2;
			} else {
				return null;
			}
		} else if (data2 == null) {
			if (type == Type.INTERSECT) {
				return null;
			} else {
				return data1;
			}
		} else if (type == Type.UNIONALL) {
			ICursor []cursors;
			if (data1 instanceof Sequence) {
				Sequence seq1 = (Sequence)data1;
				if (data2 instanceof Sequence) {
					return seq1.append((Sequence)data2);
				} else {
					MemoryCursor cs1 = new MemoryCursor(seq1);
					cursors = new ICursor[] {cs1, (ICursor)data2};
				}
			} else {
				ICursor cs1 = (ICursor)data1;
				if (data2 instanceof Sequence) {
					MemoryCursor cs2 = new MemoryCursor((Sequence)data2);
					cursors = new ICursor[] {cs1, cs2};
				} else {
					cursors = new ICursor[] {cs1, (ICursor)data2};
				}
			}
			
			return new ConjxCursor(cursors);
		}

		Sequence seq1;
		if (data1 instanceof Sequence) {
			seq1 = (Sequence)data1;
		} else {
			seq1 = ((ICursor)data1).fetch();
			if (seq1 == null) {
				if (type == Type.UNION) {
					return data2;
				} else {
					return null;
				}
			}
		}
		
		Sequence seq2;
		if (data2 instanceof Sequence) {
			seq2 = (Sequence)data2;
		} else {
			seq2 = ((ICursor)data2).fetch();
			if (seq2 == null) {
				if (type == Type.INTERSECT) {
					return null;
				} else {
					return seq1;
				}
			}
		}
		
		Context ctx = new Context();
		Expression exp1 = new Expression(ctx, "~.array()");
		Expression exp2 = new Expression(ctx, "~.array()");
		
		if (type == Type.UNION) {
			return CursorUtil.union(seq1, seq2, new Expression[] {exp1, exp2}, ctx);
		} else if (type == Type.INTERSECT) {
			return CursorUtil.isect(seq1, seq2, new Expression[] {exp1, exp2}, ctx);
		} else { //  if (type == Type.MINUS)
			return CursorUtil.diff(seq1, seq2, new Expression[] {exp1, exp2}, ctx);
		}
	}
	
	
	public String toSPL() {
		String leftSPL = left.toSPL();
		String rightSPL = right.toSPL();
		if (type == Type.UNIONALL) {
			return "[" + leftSPL + "," + rightSPL + "].conj()";
		} else {
			String opt = "o";
			if (type == Type.UNION) {
				opt += "u";
			} else if (type == Type.INTERSECT) {
				opt += "i";
			} else {
				opt += "d";
			}
			
			return "[" + leftSPL + "," + rightSPL + "].merge@" + opt + "(~.array())";
		}
	}
}
