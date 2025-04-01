package com.scudata.expression.fn;

import com.scudata.array.ConstArray;
import com.scudata.array.IArray;
import com.scudata.array.ObjectArray;
import com.scudata.common.CellLocation;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * 用行、列号（从1开始计数）计算Excel格名返回
 * cellname(r, c)
 * @author RunQian
 *
 */
public class CellName extends Function {
	private Expression rowExp;
	private Expression colExp;
	
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("cellname" + mm.getMessage("function.missingParam"));
		} else if (param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("cellname" + mm.getMessage("function.invalidParam"));
		}
		
		IParam rowParam = param.getSub(0);
		IParam colParam = param.getSub(1);
		if (rowParam == null || colParam == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("cellname" + mm.getMessage("function.invalidParam"));
		}
		
		rowExp = rowParam.getLeafExpression();
		colExp = colParam.getLeafExpression();
	}

	public Object calculate(Context ctx) {
		Object row = rowExp.calculate(ctx);
		if (!(row instanceof Number)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("cellname" + mm.getMessage("function.paramTypeError"));
		}
		
		Object col = colExp.calculate(ctx);
		if (!(col instanceof Number)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("cellname" + mm.getMessage("function.paramTypeError"));
		}
		
		return CellLocation.getCellId(((Number)row).intValue(), ((Number)col).intValue());
	}

	/**
	 * 计算出所有行的结果
	 * @param ctx 计算上行文
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx) {
		IArray rowArray = rowExp.calculateAll(ctx);
		IArray colArray = colExp.calculateAll(ctx);
		
		if (!rowArray.isNumberArray() || !colArray.isNumberArray()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("cellname" + mm.getMessage("function.paramTypeError"));
		}
		
		int len = rowArray.size();		
		if (rowArray instanceof ConstArray && colArray instanceof ConstArray) {
			if (rowArray.isNull(1) || colArray.isNull(1)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("cellname" + mm.getMessage("function.paramTypeError"));
			}
			
			int row = ((ConstArray)rowArray).getInt(1);
			int col = ((ConstArray)colArray).getInt(1);
			String id = CellLocation.getCellId(row, col);
			return new ConstArray(id, len);
		} else {
			ObjectArray result = new ObjectArray(len);
			result.setTemporary(true);
			
			for (int i = 1; i < len; ++i) {
				if (rowArray.isNull(i) || colArray.isNull(i)) {
					result.push(null);
				} else {
					int row = rowArray.getInt(i);
					int col = colArray.getInt(i);
					String id = CellLocation.getCellId(row, col);
					result.push(id);
				}
			}
			
			return result;
		}
	}
}
