package com.scudata.cellset.datamodel;

import java.lang.ref.SoftReference;
import java.util.List;

import com.scudata.cellset.INormalCell;
import com.scudata.dm.Context;
import com.scudata.dm.DBObject;
import com.scudata.dm.Env;
import com.scudata.dm.FileObject;
import com.scudata.dm.KeyWord;
import com.scudata.dm.ParamList;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.util.Variant;

/**
 * 程序网单元格对象
 * @author WangXiaoJun
 *
 */
public class PgmNormalCell extends NormalCell {
	private static final long serialVersionUID = 0x02010014;

	transient protected SoftReference<Expression> expRef; // WeakReference表达式软引用，为了缓存表达式
	transient private int sign = TYPE_BLANK_CELL; // 单元格类型
	transient private Command command; // 如果此单元格为语句则对应相应的语句，否则为空
	transient private boolean containMacro = false;;

	private PgmNormalCell valueCell; // 计算结果需要设置到的单元格，用于设置iff语句的计算结果
	
	// 存盘时使用
	public PgmNormalCell() {
	}

	public PgmNormalCell(CellSet cs, int r, int c) {
		super(cs, r, c);
	}

	public int getType() {
		return sign;
	}

	public void reset() {
		if ((sign & TYPE_CONST_CELL) != 0) {
			// 常数值可能被修改了
			if (expStr.startsWith(KeyWord.CONSTSTRINGPREFIX)) { // 字符串常数'
				value = expStr.substring(1);
			} else {
				value = Variant.parse(expStr, false);
			}
		} else {
			value = null;
			expRef = null;
			command = null;
		}
	}

	/**
	 * 设置单元格表达式
	 * @param exp String
	 */
	public void setExpString(String exp) {
		if (exp != null && exp.equals(this.expStr)) {
			return;
		}

		this.expStr = exp;
		value = null;
		expRef = null;
		command = null;

		if (exp != null && exp.length() > 0) {
			if (exp.startsWith("==")) { // 计算块
				sign = TYPE_CALCULABLE_BLOCK;
				containMacro = Expression.containMacro(exp);
			} else if (exp.startsWith("=")) { // 计算格
				sign = TYPE_CALCULABLE_CELL;
				containMacro = Expression.containMacro(exp);
			} else if (exp.startsWith(">>")) { // 执行块
				sign = TYPE_EXECUTABLE_BLOCK;
				containMacro = Expression.containMacro(exp);
			} else if (exp.startsWith(">")) { // 执行格
				sign = TYPE_EXECUTABLE_CELL;
				containMacro = Expression.containMacro(exp);
			} else if (exp.startsWith("//")) { // 注释块
				sign = TYPE_NOTE_BLOCK;
				//value = exp.substring(2);
			} else if (exp.startsWith("/")) { // 注释格
				sign = TYPE_NOTE_CELL;
				//value = exp.substring(1);
			} else if (Command.isCommand(exp)) { // 语句格
				sign = TYPE_COMMAND_CELL;
				containMacro = Expression.containMacro(exp);
			} else {
				value = parseConstValue(exp);
				sign = TYPE_CONST_CELL;
			}
		} else {
			sign = TYPE_BLANK_CELL;
		}
	}

	public static Object parseConstValue(String str) {
		if (str.startsWith(KeyWord.CONSTSTRINGPREFIX)) { // 字符串常数'
			return str.substring(1);
		} else {
			return Variant.parse(str, false);
		}
	}
	
	// 预处理$(c)
	public String getMacroReplaceString() {
		String exp = expStr;
		if (exp == null) return null;
		int len = exp.length();
		if (len == 0) return null;

		int c1 = exp.charAt(0);
		if (c1 == '=') {
			char c2 = len > 1 ? exp.charAt(1) : 0;
			if (c2 == '=') { // ==
				return getCellId() + exp.substring(1);
			} else {
				return getCellId() + exp;
			}
		} else if (c1 == '>') {
			if (len > 1 && exp.charAt(1) == '>') {
				return exp.substring(2);
			} else {
				return expStr.substring(1);
			}
		} else if (c1 == '/') {
			return null;
		} else {
			return exp;
		}
	}

	public Object getValue(boolean doCalc) {
		return getValue();
	}

	/**
	 * 计算此单元格
	 */
	public void calculate(){
		Context ctx = cs.getContext();
		if ((sign & TYPE_CALCULABLE_CELL) != 0) { // =
			Expression exp;
			if (expRef == null || (exp = expRef.get()) == null) {
				cs.setParseCurrent(row, col);
				String str = expStr.substring(1);
				if (str.startsWith(".")) {
					str = getPrevCell() + str;
				}
				
				str += getSubExpString();
				exp = new Expression(cs, ctx, str);
				
				if (!containMacro()) {
					expRef = new SoftReference<Expression>(exp);
				}
			}

			//value = null; // 先释放之前的值，用于循环体内释放内存
			value = exp.calculate(ctx);
			if (valueCell != null) {
				valueCell.setValue(value);
			}
		} else if ((sign & TYPE_CALCULABLE_BLOCK) != 0) { // ==
			Expression exp;
			if (expRef == null || (exp = expRef.get()) == null) {
				cs.setParseCurrent(row, col);
				exp = new Expression(cs, ctx, expStr.substring(2) + getSubExpString());
				
				if (!containMacro()) {
					expRef = new SoftReference<Expression>(exp);
				}
			}

			//value = null; // 先释放之前的值，用于循环体内释放内存
			value = exp.calculate(ctx);
			if (valueCell != null) {
				valueCell.setValue(value);
			}
		} else if ((sign & TYPE_EXECUTABLE_CELL) != 0) { // >
			Expression exp;
			if (expRef == null || (exp = expRef.get()) == null) {
				cs.setParseCurrent(row, col);
				if (expStr.charAt(0) == '>') {
					exp = new Expression(cs, ctx, expStr.substring(1) + getSubExpString());
				} else {
					exp = new Expression(cs, ctx, expStr + getSubExpString());
				}

				if (!containMacro()) {
					expRef = new SoftReference<Expression>(exp);
				}
			}

			exp.calculate(ctx);
		} else if ((sign & TYPE_EXECUTABLE_BLOCK) != 0) { // >>
			Expression exp;
			if (expRef == null || (exp = expRef.get()) == null) {
				cs.setParseCurrent(row, col);
				exp = new Expression(cs, ctx, expStr.substring(2) + getSubExpString());
				
				if (!containMacro()) {
					expRef = new SoftReference<Expression>(exp);
				}
			}

			exp.calculate(ctx);
		}
	}

	private String getPrevCell() {
		PgmCellSet pcs = (PgmCellSet)cs;
		for (int c = col - 1; c > 0; --c) {
			PgmNormalCell cell = pcs.getPgmNormalCell(row, c);
			if (cell.isCalculableCell() || cell.isCalculableBlock()) {
				return cell.getCellId();
			}
		}
		
		int colCount = cs.getColCount();
		for (int r = row - 1; r > 0; --r) {
			PgmNormalCell cell = pcs.getPgmNormalCell(r, col);
			Command command = cell.getCommand();
			if (command != null && command.getType() == Command.FORR) {
				return cell.getCellId();
			}
			
			for (int c = colCount; c > 0; --c) {
				cell = pcs.getPgmNormalCell(r, c);
				if (cell.isCalculableCell() || cell.isCalculableBlock()) {
					// 如果前面的列全是空白格则跳过此行
					if (c <= col) {
						return cell.getCellId();
					}
					
					for (c = col; c > 0; --c) {
						PgmNormalCell prevCell = pcs.getPgmNormalCell(r, c);
						if (!prevCell.isBlankCell()) {
							return cell.getCellId();
						}
					}
					
					break;
				}
			}
		}
		
		return "";
	}
	
	private String getSubExpString() {
		char lastChar = expStr.charAt(expStr.length() - 1);
		if (lastChar != ',' && lastChar != ';' && lastChar != '(') return "";

		StringBuffer sb = new StringBuffer(256);
		PgmCellSet pcs = (PgmCellSet)cs;
		int endRow = pcs.getCodeBlockEndRow(row, col);
		int colCount = cs.getColCount();
		int startCol = col + 1;

		Next:
		for (int r = row; r <= endRow; ++r) {
			for (int c = startCol; c <= colCount; ++c) {
				PgmNormalCell cell = pcs.getPgmNormalCell(r, c);
				if (cell == null || cell.isNoteCell()) {
					// 跳过注释格
				} else if (cell.isNoteBlock()) {
					// 跳过注释块
					r = pcs.getCodeBlockEndRow(r, c);
					break;
				} else {
					String subStr = cell.getExpString();
					if (subStr != null && subStr.length() > 0) {
						sb.append(subStr);
						lastChar = subStr.charAt(subStr.length() - 1);
						if (lastChar != ',' && lastChar != ';' && lastChar != '(') {
							break Next;
						}
					}
				}
			}
		}

		return sb.toString();
	}

	public Expression getExpression() {
		Expression exp = null;
		if (expRef != null && (exp = expRef.get()) != null) return exp;

		Context ctx = cs.getContext();
		if ((sign & TYPE_CALCULABLE_CELL) != 0) { // =
			cs.setParseCurrent(row, col);
			String str = expStr.substring(1);
			if (str.startsWith(".")) {
				str = getPrevCell() + str;
			}
			
			str += getSubExpString();
			exp = new Expression(cs, ctx, str);
			if (!containMacro()) {
				expRef = new SoftReference<Expression>(exp);
			}
		} else if ((sign & TYPE_CALCULABLE_BLOCK) != 0) { // ==
			cs.setParseCurrent(row, col);
			exp = new Expression(cs, ctx, expStr.substring(2) + getSubExpString());
			if (!containMacro()) {
				expRef = new SoftReference<Expression>(exp);
			}
		} else if ((sign & TYPE_EXECUTABLE_CELL) != 0) { // >
			cs.setParseCurrent(row, col);
			if (expStr.charAt(0) == '>') {
				exp = new Expression(cs, ctx, expStr.substring(1) + getSubExpString());
			} else {
				exp = new Expression(cs, ctx, expStr + getSubExpString());
			}
			
			if (!containMacro()) {
				expRef = new SoftReference<Expression>(exp);
			}
		} else if ((sign & TYPE_EXECUTABLE_BLOCK) != 0) { // >>
			cs.setParseCurrent(row, col);
			exp = new Expression(cs, ctx, expStr.substring(2) + getSubExpString());
			if (!containMacro()) {
				expRef = new SoftReference<Expression>(exp);
			}
		}

		return exp;
	}

	/**
	 * 返回单元格所对应的语句对象
	 * @return Command
	 */
	public Command getCommand() {
		if (command == null && (sign & TYPE_COMMAND_CELL) != 0) {
			if (containMacro()) {
				Command command = Command.parse(expStr);
				initCommand(command);
				return command;
			} else {
				command = Command.parse(expStr);
				initCommand(command);
			}
		}

		return command;
	}

	private void initCommand(Command command) {
		if (command.getType() == Command.IFF) {
			PgmCellSet pcs = (PgmCellSet)cs;
			int totalCol = pcs.getColCount();
			PgmNormalCell ifValueCell = null;
			PgmNormalCell elseValueCell = null;
			
			// 在本行中寻找else分支
			for (int c = col + 1; c <= totalCol; ++c) {
				PgmNormalCell cell = pcs.getPgmNormalCell(row, c);
				command = cell.getCommand();
				if (command != null && command.getType() == Command.ELSE) {
					for (++c; c <= totalCol; ++c) {
						cell = pcs.getPgmNormalCell(row, c);
						if (cell.isCalculableCell() || cell.isCalculableBlock()) {
							elseValueCell = cell;
						}
					}
					
					if (ifValueCell != null) {
						ifValueCell.setValueCell(this);
					}
					
					if (elseValueCell != null) {
						elseValueCell.setValueCell(this);
					}
					
					return;
				} else if (cell.isCalculableCell() || cell.isCalculableBlock()) {
					ifValueCell = cell;
				}
			}
			
			int endBlock = pcs.getCodeBlockEndRow(row, col);
			for (int r = row + 1; r <= endBlock; ++r) {
				for (int c = col + 1; c <= totalCol; ++c) {
					PgmNormalCell cell = pcs.getPgmNormalCell(r, c);
					if (cell.isCalculableCell() || cell.isCalculableBlock()) {
						ifValueCell = cell;
					}
				}
			}
			
			int rowCount = pcs.getRowCount();
			int nextRow = endBlock + 1;
			int elseRow = -1;
			
			if (nextRow <= rowCount) {
				PgmNormalCell cell = pcs.getPgmNormalCell(nextRow, col);
				command = cell.getCommand();
				if (command != null && command.getType() == Command.ELSE) {
					elseRow = nextRow;
				}
				
				for (int c = 1; c < col; ++c) {
					cell = pcs.getPgmNormalCell(nextRow, c);
					if (!cell.isBlankCell()) {
						elseRow = -1;
						break;
					}
				}
			}
			
			if (elseRow != -1) {
				endBlock = pcs.getCodeBlockEndRow(elseRow, col);
				for (int r = elseRow; r <= endBlock; ++r) {
					for (int c = col + 1; c <= totalCol; ++c) {
						PgmNormalCell cell = pcs.getPgmNormalCell(r, c);
						if (cell.isCalculableCell() || cell.isCalculableBlock()) {
							elseValueCell = cell;
						}
					}
				}
			}
			
			if (ifValueCell != null) {
				ifValueCell.setValueCell(this);
			}
			
			if (elseValueCell != null) {
				elseValueCell.setValueCell(this);
			}
		}
	}
	
	/**
	 * 返回是否语句格
	 * @return boolean
	 */
	public boolean isCommandCell() {
		return (sign & TYPE_COMMAND_CELL) != 0;
	}

	/**
	 * 返回是否结果格
	 * @return boolean
	 */
	public boolean isResultCell() {
		return isCommandCell() && Command.isResultCommand(expStr);
	}

	// 返回单元格是否需要计算
	public boolean needCalculate() {
		int tmp = TYPE_CALCULABLE_CELL | TYPE_CALCULABLE_BLOCK |
			TYPE_EXECUTABLE_CELL | TYPE_EXECUTABLE_BLOCK | TYPE_COMMAND_CELL;
		return (sign & tmp) != 0;
	}

	/**
	 * 返回单元格是否是空白格，即没有表达式
	 * @return boolean
	 */
	public boolean isBlankCell() {
		return (sign & TYPE_BLANK_CELL) != 0;
	}

	// 表达式里是否包含宏替换s
	private boolean containMacro() {
		return containMacro;
	}
	
	// 返回单元格是否是常数格
	public boolean isConstCell() {
		return (sign & TYPE_CONST_CELL) != 0;
	}

	/**
	 * 返回单元格是否是注释格
	 * @return boolean
	 */
	public boolean isNoteCell() {
		return (sign & TYPE_NOTE_CELL) != 0;
	}

	/**
	 * 返回单元格是否是注释块
	 * @return boolean
	 */
	public boolean isNoteBlock() {
		return (sign & TYPE_NOTE_BLOCK) != 0;
	}

	// 返回是否是计算块
	public boolean isCalculableBlock() {
		return (sign & TYPE_CALCULABLE_BLOCK) != 0;
	}

	// 返回是否是计算格
	public boolean isCalculableCell() {
		return (sign & TYPE_CALCULABLE_CELL) != 0;
	}
	
	// 返回是否是执行格
	public boolean isExecutableCell() {
		return (sign & TYPE_EXECUTABLE_CELL) != 0;
	}

	// 返回是否是执行块
	public boolean isExecutableBlock() {
		return (sign & TYPE_EXECUTABLE_BLOCK) != 0;
	}

	public Object deepClone(){
		PgmNormalCell cell = new PgmNormalCell(cs, row, col);
		cell.setExpString(expStr);
		cell.tip = tip;
		cell.value = value;
		return cell;
	}

	public boolean needRegulateString() {
		return (sign & TYPE_CONST_CELL) == 0 && (sign & TYPE_BLANK_CELL) == 0 && 
				((sign & TYPE_NOTE_CELL) == 0 && (sign & TYPE_NOTE_BLOCK) == 0 || Env.isAdjustNoteCell());
	}

	public byte calcExpValueType(Context ctx) {
		Object val = getValue();
		if (val == null) {
			Expression exp = getExpression();
			return exp == null ? Expression.TYPE_OTHER : exp.getExpValueType(ctx);
		} else {
			if (val instanceof DBObject) {
				return Expression.TYPE_DB;
			} else if (val instanceof FileObject) {
				return Expression.TYPE_FILE;
			} else {
				return Expression.TYPE_OTHER;
			}
		}
	}

	public void getUsedParamsAndCells(ParamList usedParams, List<INormalCell> usedCells) {
		Context ctx = cs.getContext();
		Expression exp;
		
		if ((sign & TYPE_CALCULABLE_CELL) != 0) { // =
			cs.setParseCurrent(row, col);
			String str = expStr.substring(1);
			if (str.startsWith(".")) {
				str = getPrevCell() + str;
			}
			
			str += getSubExpString();
			exp = new Expression(cs, ctx, str);
		} else if ((sign & TYPE_CALCULABLE_BLOCK) != 0) { // ==
			cs.setParseCurrent(row, col);
			exp = new Expression(cs, ctx, expStr.substring(2) + getSubExpString());
		} else if ((sign & TYPE_EXECUTABLE_CELL) != 0) { // >
			cs.setParseCurrent(row, col);
			if (expStr.charAt(0) == '>') {
				exp = new Expression(cs, ctx, expStr.substring(1) + getSubExpString());
			} else {
				exp = new Expression(cs, ctx, expStr + getSubExpString());
			}
		} else if ((sign & TYPE_EXECUTABLE_BLOCK) != 0) { // >>
			cs.setParseCurrent(row, col);
			exp = new Expression(cs, ctx, expStr.substring(2) + getSubExpString());
		} else {
			Command cmd = getCommand();
			if (cmd != null) {
				IParam param = cmd.getParam(cs, ctx);
				if (param != null) {
					param.getUsedParams(ctx, usedParams);
					param.getUsedCells(usedCells);
				}
			}
			
			return;
		}
		
		exp.getUsedParams(ctx, usedParams);
		exp.getUsedCells(usedCells);
	}
	
	/**
	 * 取当前单元格引用到的单元格
	 * @param resultList
	 */
	public void getUsedCells(List<INormalCell> resultList) {
		Context ctx = cs.getContext();
		Expression exp;
		
		if ((sign & TYPE_CALCULABLE_CELL) != 0) { // =
			cs.setParseCurrent(row, col);
			String str = expStr.substring(1);
			if (str.startsWith(".")) {
				str = getPrevCell() + str;
			}
			
			str += getSubExpString();
			exp = new Expression(cs, ctx, str);
		} else if ((sign & TYPE_CALCULABLE_BLOCK) != 0) { // ==
			cs.setParseCurrent(row, col);
			exp = new Expression(cs, ctx, expStr.substring(2) + getSubExpString());
		} else if ((sign & TYPE_EXECUTABLE_CELL) != 0) { // >
			cs.setParseCurrent(row, col);
			if (expStr.charAt(0) == '>') {
				exp = new Expression(cs, ctx, expStr.substring(1) + getSubExpString());
			} else {
				exp = new Expression(cs, ctx, expStr + getSubExpString());
			}
		} else if ((sign & TYPE_EXECUTABLE_BLOCK) != 0) { // >>
			cs.setParseCurrent(row, col);
			exp = new Expression(cs, ctx, expStr.substring(2) + getSubExpString());
		} else {
			Command cmd = getCommand();
			if (cmd != null) {
				cmd.getUsedCells(cs, ctx, resultList);
			}
			
			return;
		}
		
		exp.getUsedCells(resultList);
	}
	
	/**
	 * 清除单元格值和计算表达式
	 */
	public void clear() {
		value = null;
		expRef = null;
		command = null;
	}

	public void setValueCell(PgmNormalCell valueCell) {
		this.valueCell = valueCell;
	}
}
