package com.scudata.cellset.datamodel;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import com.scudata.cellset.ICellSet;
import com.scudata.cellset.IColCell;
import com.scudata.cellset.INormalCell;
import com.scudata.cellset.IRowCell;
import com.scudata.common.ByteArrayInputRecord;
import com.scudata.common.ByteArrayOutputRecord;
import com.scudata.common.ByteMap;
import com.scudata.common.CellLocation;
import com.scudata.common.DBSession;
import com.scudata.common.Logger;
import com.scudata.common.MD5;
import com.scudata.common.Matrix;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.DBObject;
import com.scudata.dm.FileObject;
import com.scudata.dm.IQueryable;
import com.scudata.dm.JobSpace;
import com.scudata.dm.KeyWord;
import com.scudata.dm.Machines;
import com.scudata.dm.ParallelCaller;
import com.scudata.dm.Param;
import com.scudata.dm.ParamList;
import com.scudata.dm.RetryException;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.cursor.MultipathCursors;
import com.scudata.dm.op.Channel;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.ParamInfo2;
import com.scudata.expression.ParamParser;
import com.scudata.resources.EngineMessage;
import com.scudata.thread.CursorLooper;
import com.scudata.thread.Job;
import com.scudata.thread.ThreadPool;
import com.scudata.util.Variant;

// 程序网
public class PgmCellSet extends CellSet {
	private static final long serialVersionUID = 0x02010010;
	public static final int PRIVILEGE_FULL = 0; // 完全控制，可以做任何事情
	public static final int PRIVILEGE_EXEC = 1; // 只可以执行

	private static final int SIGN_AUTOCALC = 0x00000010; // 自动计算

	private int sign = 0;
	private ByteMap customPropMap; // 自定义属性

	// private String []psws = new String[2]; // 2级密码，高级别在前
	private String pswHash; // 密码hash值
	private int nullPswPrivilege = PRIVILEGE_EXEC; // 无密码登陆时的权限
	transient private int curPrivilege = PRIVILEGE_FULL; // 当前的权限

	transient protected CellLocation curLct; // 当前运算的单元格的位置
	transient private Object curDb; // DBObject或this，this表示文件简单dql

	transient private LinkedList<CmdCode> stack = new LinkedList<CmdCode>(); // 计算堆栈
	transient private CellLocation parseLct; // 当前正在解析表达式的单元格

	transient private Sequence resultValue; // result语句返回值，后面的会覆盖前面的
	transient private int resultCurrent;
	transient private CellLocation resultLct; // result语句所在单元格

	transient private boolean interrupt; // 是否调用过interrupt
	transient private boolean isInterrupted; // 计算时使用，暂停后会重置
	transient private boolean hasReturn = false;

	transient private String name; // 网名，在DfxManager中使用

	// func fn(arg,…)
	transient private HashMap<String, FuncInfo> fnMap; // [函数名, 函数信息]映射
	transient private ForkCmdCode forkCmdCode; // 当前执行的fork代码块

	private String isvHash; // 有功能点14（KIT）时，写出dfx时需要写出授权文件中isv的MD5值

	private static class CmdCode {
		protected byte type; // Command类型
		protected int row; // Command的行号
		protected int col; // Command的列号
		protected int blockEndRow; // Command的代码块结束行行号

		public CmdCode(byte type, int r, int c, int endRow) {
			this.type = type;
			this.row = r;
			this.col = c;
			this.blockEndRow = endRow;
		}
	}

	private static abstract class ForCmdCode extends CmdCode {
		protected int seq = 0; // for循环序号

		public ForCmdCode(int r, int c, int endRow) {
			super(Command.FOR, r, c, endRow);
		}

		abstract public boolean hasNextValue();

		abstract public Object nextValue();

		public Object endValue() {
			return null;
		}

		public int getSeq() {
			return seq;
		}

		public void setSeq(int n) {
			this.seq = n;
		}
	}
	
	private static class ForkCmdCode extends CmdCode {
		protected int seq = 0; // fork线程序号

		public ForkCmdCode(int r, int c, int endRow, int seq) {
			super(Command.FORK, r, c, endRow);
			this.seq = seq;
		}
	}
	
	private static class EndlessForCmdCode extends ForCmdCode {
		public EndlessForCmdCode(int r, int c, int endRow) {
			super(r, c, endRow);
		}

		public boolean hasNextValue() {
			return true;
		}

		public Object nextValue() {
			return new Integer(++seq);
		}
	}

	private static class SequenceForCmdCode extends ForCmdCode {
		private Sequence sequence;

		public SequenceForCmdCode(int r, int c, int endRow, Sequence sequence) {
			super(r, c, endRow);
			this.sequence = sequence;
		}

		public boolean hasNextValue() {
			return seq < sequence.length();
		}

		public Object nextValue() {
			return sequence.get(++seq);
		}
	}

	private static class BoolForCmdCode extends ForCmdCode {
		private Expression exp;
		private Context ctx;

		public BoolForCmdCode(int r, int c, int endRow, Expression exp,
				Context ctx) {
			super(r, c, endRow);
			this.exp = exp;
			this.ctx = ctx;
		}

		public boolean hasNextValue() {
			Object value = exp.calculate(ctx);
			if (!(value instanceof Boolean)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.forVarTypeError"));
			}

			return ((Boolean) value).booleanValue();
		}

		public Object nextValue() {
			++seq;
			return Boolean.TRUE;
		}

		public Object endValue() {
			return Boolean.FALSE;
		}
	}

	private static class IntForCmdCode extends ForCmdCode {
		private int start;
		private int end;
		private int step;

		public IntForCmdCode(int r, int c, int endRow, int start, int end,
				int step) {
			super(r, c, endRow);
			this.start = start;
			this.end = end;
			this.step = step;
		}

		public boolean hasNextValue() {
			if (step >= 0) {
				return start <= end;
			} else {
				return start >= end;
			}
		}

		public Object nextValue() {
			Object val = new Integer(start);

			++seq;
			start += step;
			return val;
		}

		public Object endValue() {
			return new Integer(start);
		}
	}

	private static class CursorForCmdCode extends ForCmdCode {
		private ICursor cursor;
		private int count;
		private Expression gexp;
		private Context ctx;
		private Sequence table;

		// private boolean bi = false; // 是否用group@i

		public CursorForCmdCode(int r, int c, int endRow, ICursor cursor,
				int count, Expression gexp, Context ctx) {
			super(r, c, endRow);
			this.cursor = cursor;
			this.count = count;
			this.gexp = gexp;
			this.ctx = ctx;
		}

		public boolean hasNextValue() {
			if (gexp == null) {
				table = cursor.fetch(count);
			} else {
				table = cursor.fetchGroup(gexp, ctx);
			}

			return table != null && table.length() > 0;
		}

		public Object nextValue() {
			++seq;
			return table;
		}

		public void close() {
			cursor.close();
		}
	}

	private static class ForkJob extends Job {
		PgmCellSet pcs;
		int row;
		int col;
		int endRow;

		public ForkJob(PgmCellSet pcs, int row, int col, int endRow) {
			this.pcs = pcs;
			this.row = row;
			this.col = col;
			this.endRow = endRow;
		}

		public void run() {
			pcs.executeFork(row, col, endRow);
		}

		public Object getResult() {
			return pcs.getForkResult();
		}
	}

	private class SubForkJob extends Job {
		private IParam param;
		private int row;
		private int col;
		private int endRow;
		private Context ctx;

		public SubForkJob(IParam param, int row, int col, int endRow,
				Context ctx) {
			this.param = param;
			this.row = row;
			this.col = col;
			this.endRow = endRow;
			this.ctx = ctx;
		}

		public void run() {
			runForkCmd(param, row, col, endRow, ctx);
		}
	}

	/**
	 * 网格中定义的函数信息
	 * @author RunQian
	 *
	 */
	public class FuncInfo {
		private PgmNormalCell cell; // 函数所在单元格
		private String[] argNames; // 参数名

		public FuncInfo(PgmNormalCell cell, String[] argNames) {
			this.cell = cell;
			this.argNames = argNames;
		}

		/**
		 * 取函数所在的单元格
		 * @return
		 */
		public PgmNormalCell getCell() {
			return cell;
		}

		/**
		 * 取函数的参数名
		 * @return
		 */
		public String[] getArgNames() {
			return argNames;
		}
	}

	public PgmCellSet() {
	}

	/**
	 * 构造一个指定行数和列数的表格
	 * @param row int 行数
	 * @param col int 列数
	 */
	public PgmCellSet(int row, int col) {
		super(row, col);
	}

	public NormalCell newCell(int r, int c) {
		return new PgmNormalCell(this, r, c);
	}

	public RowCell newRowCell(int r) {
		return new RowCell(r);
	}

	public ColCell newColCell(int c) {
		return new ColCell(c);
	}

	public PgmNormalCell getPgmNormalCell(int row, int col) {
		return (PgmNormalCell) cellMatrix.get(row, col);
	}

	public INormalCell getCurrent() {
		return curLct == null ? null : getNormalCell(curLct.getRow(),
				curLct.getCol());
	}

	public void setCurrent(INormalCell cell) {
		if (cell == null) {
			curLct = null;
		} else {
			if (curLct == null) {
				curLct = new CellLocation(cell.getRow(), cell.getCol());
			} else {
				curLct.set(cell.getRow(), cell.getCol());
			}
		}
	}

	// 创建一个新的网格，新网引用源网的单元格，拥有自己的计算环境
	public PgmCellSet newCalc() {
		Matrix m1 = cellMatrix;
		int colSize = cellMatrix.getColSize();
		int rowSize = cellMatrix.getRowSize();

		PgmCellSet pcs = new PgmCellSet();
		Matrix m2 = new Matrix(rowSize, colSize);
		pcs.cellMatrix = m2;

		for (int r = 0; r < rowSize; ++r) {
			for (int c = 0; c < colSize; ++c) {
				m2.set(r, c, m1.get(r, c));
			}
		}

		pcs.sign = sign;
		pcs.pswHash = pswHash;
		pcs.nullPswPrivilege = nullPswPrivilege;
		Context ctx = getContext();
		pcs.setContext(ctx.newComputeContext());
		pcs.name = name;
		return pcs;
	}

	// 生成新的网格供cursor(c,…)使用
	public PgmCellSet newCursorDFX(INormalCell cell, Object[] args) {
		int rowCount = getRowCount();
		int colCount = getColCount();
		PgmCellSet newPcs = new PgmCellSet(rowCount, colCount);

		int row = cell.getRow();
		int col = cell.getCol();
		int endRow = getCodeBlockEndRow(row, col);

		// 代码块外的格子只引用格值，不设置表达式
		for (int r = 1; r < row; ++r) {
			for (int c = 1; c <= colCount; ++c) {
				Object val = getPgmNormalCell(r, c).getValue();
				newPcs.getPgmNormalCell(r, c).setValue(val);
			}
		}

		for (int r = endRow + 1; r <= rowCount; ++r) {
			for (int c = 1; c <= colCount; ++c) {
				Object val = getPgmNormalCell(r, c).getValue();
				newPcs.getPgmNormalCell(r, c).setValue(val);
			}
		}

		for (int r = row; r <= endRow; ++r) {
			for (int c = 1; c < col; ++c) {
				Object val = getPgmNormalCell(r, c).getValue();
				newPcs.getPgmNormalCell(r, c).setValue(val);
			}

			for (int c = col; c <= colCount; ++c) {
				INormalCell tmp = getCell(r, c);
				INormalCell cellClone = (INormalCell) tmp.deepClone();
				cellClone.setCellSet(newPcs);
				newPcs.setCell(r, c, cellClone);
			}
		}

		// 把参数值设到func单元格上及后面的格子
		if (args != null) {
			int paramRow = row;
			int paramCol = col;
			for (int i = 0, pcount = args.length; i < pcount; ++i) {
				newPcs.getPgmNormalCell(paramRow, paramCol).setValue(args[i]);
				if (paramCol < colCount) {
					paramCol++;
				} else {
					break;
					// if (paramRow == getRowCount() && i < paramCount - 1) {
					// MessageManager mm = EngineMessage.get();
					// throw new RQException("call" +
					// mm.getMessage("function.paramCountNotMatch"));
					// }
					// paramRow++;
					// paramCol = 1;
				}
			}
		}

		newPcs.setContext(getContext());
		newPcs.setCurrent(cell);
		newPcs.setNext(row, col + 1, false);
		newPcs.name = name;
		return newPcs;
	}

	/**
	 * 深度克隆
	 * @return 克隆出的对象
	 */
	public Object deepClone() {
		PgmCellSet pcs = new PgmCellSet();

		int colSize = cellMatrix.getColSize();
		int rowSize = cellMatrix.getRowSize();
		pcs.cellMatrix = new Matrix(rowSize, colSize);
		;

		for (int col = 1; col < colSize; col++) {
			for (int row = 1; row < rowSize; row++) {
				INormalCell cell = getCell(row, col);
				INormalCell cellClone = (INormalCell) cell.deepClone();
				cellClone.setCellSet(pcs);
				pcs.cellMatrix.set(row, col, cellClone);
			}
		}

		// 行首格和列首格
		for (int col = 1; col < colSize; col++)
			pcs.cellMatrix.set(0, col, getColCell(col).deepClone());
		for (int row = 1; row < rowSize; row++)
			pcs.cellMatrix.set(row, 0, getRowCell(row).deepClone());

		ParamList param = getParamList();
		if (param != null) {
			pcs.setParamList((ParamList) param.deepClone());
		}

		pcs.sign = sign;
		if (customPropMap != null) {
			pcs.customPropMap = (ByteMap) customPropMap.deepClone();
		}

		pcs.pswHash = pswHash;
		pcs.nullPswPrivilege = nullPswPrivilege;
		pcs.name = name;
		return pcs;
	}

	/**
	 * 写内容到流
	 * @param out ObjectOutput 输出流
	 * @throws IOException
	 */
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeByte(2);
		out.writeInt(sign); // 不与报表4集成
		out.writeObject(customPropMap);
		out.writeObject(pswHash);
		out.writeInt(nullPswPrivilege);
		
		out.writeObject(name); // 版本2写出
	}

	/**
	 * 从流中读内容
	 * @param in ObjectInput 输入流
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		super.readExternal(in);
		int v = in.readByte();
		sign = in.readInt();
		customPropMap = (ByteMap) in.readObject();
		pswHash = (String) in.readObject();
		nullPswPrivilege = in.readInt();
		
		if (v > 1) {
			name = (String)in.readObject();
		}
	}

	public byte[] serialize() throws IOException {
		ByteArrayOutputRecord out = new ByteArrayOutputRecord();

		// 序列化单元格矩阵
		int rowCount = getRowCount();
		int colCount = getColCount();
		out.writeInt(rowCount);
		out.writeInt(colCount);
		for (int row = 1; row <= rowCount; ++row) {
			IRowCell rc = getRowCell(row);
			out.writeRecord(rc);
		}
		for (int col = 1; col <= colCount; ++col) {
			IColCell cc = getColCell(col);
			out.writeRecord(cc);
		}
		for (int row = 1; row <= rowCount; ++row) {
			for (int col = 1; col <= colCount; ++col) {
				INormalCell nc = getCell(row, col);
				out.writeRecord(nc);
			}
		}

		out.writeRecord(paramList);
		out.writeInt(sign); // 不与报表4集成
		out.writeRecord(customPropMap);

		out.writeStrings(null); // 为了兼容psws
		out.writeInt(0); // 兼容之前的画布

		out.writeString(pswHash);
		out.writeInt(nullPswPrivilege);

		isvHash = null;
		out.writeString(isvHash);
		return out.toByteArray();
	}

	/**
	 * 从流中读内容
	 * @param buf byte[]
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public void fillRecord(byte[] buf) throws IOException,
			ClassNotFoundException {
		ByteArrayInputRecord in = new ByteArrayInputRecord(buf);

		// 生成单元格矩阵
		int rowCount = in.readInt();
		int colCount = in.readInt();
		cellMatrix = new Matrix(rowCount + 1, colCount + 1);
		for (int row = 1; row <= rowCount; ++row) {
			RowCell rc = (RowCell) in.readRecord(newRowCell(row));
			cellMatrix.set(row, 0, rc);
		}
		for (int col = 1; col <= colCount; ++col) {
			ColCell cc = (ColCell) in.readRecord(newColCell(col));
			cellMatrix.set(0, col, cc);
		}
		for (int row = 1; row <= rowCount; ++row) {
			for (int col = 1; col <= colCount; ++col) {
				NormalCell nc = (NormalCell) in.readRecord(newCell(row, col));
				cellMatrix.set(row, col, nc);
			}
		}

		paramList = (ParamList) in.readRecord(new ParamList());
		sign = in.readInt();
		customPropMap = (ByteMap) in.readRecord(new ByteMap());

		if (in.available() > 0) {
			in.readStrings();
			if (in.available() > 0) {
				in.readInt(); // 兼容之前的画布
				if (in.available() > 0) {
					pswHash = in.readString();
					nullPswPrivilege = in.readInt();
					if (in.available() > 0) {
						isvHash = in.readString();
					}
				}
			}
		}
	}

	// 跳过else代码块
	private void skipCodeBlock() {
		int curRow = curLct.getRow();
		int curCol = curLct.getCol();
		int endBlock = getCodeBlockEndRow(curRow, curCol);

		// 下一个要执行的单元格设到代码块后的单元格
		setNext(endBlock + 1, 1, true);
	}

	// 把下一个要执行的格设到if所对应的else语句的下一格
	// 当前格为if语句格
	private void toElseCmd() {
		int curRow = curLct.getRow();
		int curCol = curLct.getCol();
		int totalCol = getColCount();

		int level = 0;
		Command command;

		// 在本行中寻找else分支
		for (int c = curCol + 1; c <= totalCol; ++c) {
			PgmNormalCell cell = getPgmNormalCell(curRow, c);
			if ((command = cell.getCommand()) != null) {
				byte type = command.getType();
				if (type == Command.ELSE) {
					if (level == 0) { // 找到对应的else分支
						setNext(curRow, c + 1, false);
						return;
					} else {
						level--;
					}
				} else if (type == Command.ELSEIF) {
					if (level == 0) { // 找到对应的elseif分支
						setNext(curRow, c, false);
						runIfCmd(cell, command);
						return;
					}
				} else if (type == Command.IF) {
					level++;
				}
			}
		}

		// 跳过代码块
		int endBlock = getCodeBlockEndRow(curRow, curCol);
		int nextRow = endBlock + 1;
		if (nextRow <= getRowCount()) {
			for (int c = 1; c <= totalCol; ++c) {
				PgmNormalCell cell = getPgmNormalCell(nextRow, c);
				if (!cell.isBlankCell()) {
					if (c != curCol) { // 没有else分支
						setNext(nextRow, c, true);
					} else {
						command = cell.getCommand();
						if (command == null) {
							setNext(nextRow, c, true);
						} else {
							byte type = command.getType();
							if (type == Command.ELSE) { // 找到对应的else分支
								setNext(nextRow, c + 1, false);
							} else if (type == Command.ELSEIF) { // 找到对应的elseif分支
								setNext(nextRow, c, false);
								runIfCmd(cell, command);
							} else {
								setNext(nextRow, c, true);
							}
						}
					}
					return;
				}
			}
		} else {
			setNext(nextRow, 1, true); // 可能在循环或函数里
		}
	}

	private int getIfBlockEndRow(int prow, int pcol) {
		int level = 0;
		int totalCol = getColCount();
		Command command;

		// 在本行中寻找else分支
		for (int c = pcol + 1; c <= totalCol; ++c) {
			PgmNormalCell cell = getPgmNormalCell(prow, c);
			if ((command = cell.getCommand()) != null) {
				byte type = command.getType();
				if (type == Command.ELSE) {
					if (level == 0) { // 找到对应的else分支
						return prow;
					} else {
						level--;
					}
				} else if (type == Command.ELSEIF) {
					if (level == 0) { // 找到对应的elseif分支
						return prow;
					}
				} else if (type == Command.IF) {
					level++;
				}
			}
		}

		// 跳过代码块
		int endBlock = getCodeBlockEndRow(prow, pcol);
		int totalRow = getRowCount();
		if (endBlock < totalRow) {
			int nextRow = endBlock + 1;
			for (int c = 1; c <= totalCol; ++c) {
				PgmNormalCell cell = getPgmNormalCell(nextRow, c);
				if (cell.isBlankCell())
					continue;

				command = cell.getCommand();
				if (command == null) {
					return endBlock;
				} else {
					byte type = command.getType();
					if (type == Command.ELSE) { // 找到对应的else分支
						return getCodeBlockEndRow(nextRow, c);
					} else if (type == Command.ELSEIF) { // 找到对应的elseif分支
						return getIfBlockEndRow(nextRow, c);
					} else {
						return endBlock;
					}
				}
			}
			throw new RuntimeException();
		} else {
			return endBlock;
		}
	}

	// 执行if程序格
	private void runIfCmd(NormalCell cell, Command command) {
		Context ctx = getContext();
		Expression exp = command.getExpression(this, ctx);
		if (exp == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("if" + mm.getMessage("function.invalidParam"));
		}

		Object value = exp.calculate(ctx);
		cell.setValue(value);
		if (Variant.isTrue(value)) {
			setNext(curLct.getRow(), curLct.getCol() + 1, false);
		} else {
			toElseCmd();
		}
	}

	/**
	 * 返回for单元格当前的循环序号 #A1
	 * @param r int for单元格的行号
	 * @param c int for单元格的列号
	 * @return int
	 */
	public int getForCellRepeatSeq(int r, int c) {
		for (int i = 0; i < stack.size(); ++i) {
			CmdCode cmd = stack.get(i);
			if (cmd.row == r && cmd.col == c) {
				if (cmd.type == Command.FOR) {
					return ((ForCmdCode) cmd).getSeq();
				} else {
					break;
				}
			}
		}

		if (forkCmdCode != null && forkCmdCode.row == r && forkCmdCode.col == c) {
			return forkCmdCode.seq;
		}
		
		MessageManager mm = EngineMessage.get();
		throw new RQException("#" + CellLocation.getCellId(r, c)
				+ mm.getMessage("engine.needInFor"));
	}

	// 执行for程序格
	private void runForCmd(NormalCell cell, Command command) {
		int row = curLct.getRow();
		int col = curLct.getCol();
		if (stack.size() > 0) {
			CmdCode cmd = stack.getFirst();
			if (cmd != null && cmd.row == row && cmd.col == col) {
				// 执行下一次循环
				ForCmdCode forCmd = (ForCmdCode) cmd;
				if (forCmd.hasNextValue()) {
					cell.setValue(forCmd.nextValue());
					setNext(row, col + 1, false); // 执行下一单元格
				} else {
					// 跳出循环
					cell.setValue(forCmd.endValue());
					stack.removeFirst();
					setNext(cmd.blockEndRow + 1, 1, true);
				}

				return;
			}
		}

		// 首次执行循环，计算循环变量
		ForCmdCode cmdCode;
		Context ctx = getContext();
		int endRow = getCodeBlockEndRow(row, col);
		Expression exp = command.getExpression(this, ctx);
		if (exp == null) {
			cmdCode = new EndlessForCmdCode(row, col, endRow);
		} else {
			Object value = exp.calculate(ctx);
			if (value instanceof Number) {
				IParam param = command.getParam(this, ctx);
				if (param.isLeaf()) {
					cmdCode = new IntForCmdCode(row, col, endRow, 1,
							((Number) value).intValue(), 1);
				} else {
					int size = param.getSubSize();
					if (size > 3) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("for"
								+ mm.getMessage("function.invalidParam"));
					}

					IParam sub = param.getSub(1);
					if (sub == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("for"
								+ mm.getMessage("function.invalidParam"));
					}

					Object obj = sub.getLeafExpression().calculate(ctx);
					if (!(obj instanceof Number)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(
								mm.getMessage("engine.forVarTypeError"));
					}

					int start = ((Number) value).intValue();
					int end = ((Number) obj).intValue();
					int step;
					if (size > 2) {
						sub = param.getSub(2);
						if (sub == null) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("for"
									+ mm.getMessage("function.invalidParam"));
						}

						obj = sub.getLeafExpression().calculate(ctx);
						if (!(obj instanceof Number)) {
							MessageManager mm = EngineMessage.get();
							throw new RQException(
									mm.getMessage("engine.forVarTypeError"));
						}

						step = ((Number) obj).intValue();
					} else {
						if (start <= end) {
							step = 1;
						} else {
							step = -1;
						}
					}

					cmdCode = new IntForCmdCode(row, col, endRow, start, end,
							step);
				}
			} else if (value instanceof Sequence) {
				cmdCode = new SequenceForCmdCode(row, col, endRow,
						(Sequence) value);
			} else if (value instanceof Boolean) {
				cell.setValue(value);
				if (((Boolean) value).booleanValue()) {
					cmdCode = new BoolForCmdCode(row, col, endRow, exp, ctx);
					cmdCode.setSeq(1);
					stack.addFirst(cmdCode);
					setNext(row, col + 1, false);
				} else {
					setNext(endRow + 1, 1, true); // 跳出循环
				}

				return;
			} else if (value instanceof ICursor) {
				IParam param = command.getParam(this, ctx);
				int count = 1;
				Expression gexp = null;

				if (param.getType() == IParam.Semicolon) {
					if (param.getSubSize() != 2) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("for"
								+ mm.getMessage("function.invalidParam"));
					}

					IParam sub = param.getSub(1);
					if (sub == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("for"
								+ mm.getMessage("function.invalidParam"));
					} else if (sub.isLeaf()) {
						gexp = sub.getLeafExpression();
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("for"
								+ mm.getMessage("function.invalidParam"));
					}
				} else if (!param.isLeaf()) {
					if (param.getSubSize() != 2) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("for"
								+ mm.getMessage("function.invalidParam"));
					}

					IParam sub = param.getSub(1);
					if (sub != null) {
						Object countObj = sub.getLeafExpression()
								.calculate(ctx);
						if (!(countObj instanceof Number)) {
							MessageManager mm = EngineMessage.get();
							throw new RQException(
									mm.getMessage("engine.forVarTypeError"));
						}

						count = ((Number) countObj).intValue();
						if (count < 1) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("for"
									+ mm.getMessage("function.invalidParam"));
						}
					}
				}

				cmdCode = new CursorForCmdCode(row, col, endRow,
						(ICursor) value, count, gexp, ctx);
			} else if (value == null) {
				cmdCode = null;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.forVarTypeError"));
			}
		}

		if (cmdCode != null && cmdCode.hasNextValue()) {
			cell.setValue(cmdCode.nextValue());
			stack.addFirst(cmdCode);
			setNext(row, col + 1, false);
		} else {
			setNext(endRow + 1, 1, true); // 跳出循环
		}
	}

	private void runContinueCmd(Command command) {
		CellLocation forLct = command.getCellLocation(getContext());
		int index = -1;

		for (int i = 0, size = stack.size(); i < size; ++i) {
			CmdCode cmd = stack.get(i);
			if (cmd.type == Command.FOR) {
				if (forLct == null
						|| (forLct.getRow() == cmd.row && forLct.getCol() == cmd.col)) {
					index = i;
					break;
				}
			}
		}

		if (index == -1) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("next" + mm.getMessage("engine.needInFor"));
		}

		for (int i = 0; i < index; ++i) {
			// 跳出内部for循环
			CmdCode cmd = stack.removeFirst();
			if (cmd.type == Command.FOR) {
				endForCommand((ForCmdCode) cmd);
			}
		}

		CmdCode cmd = stack.getFirst();
		setNext(cmd.row, cmd.col, false);
	}

	private void runBreakCmd(Command command) {
		CellLocation forLct = command.getCellLocation(getContext());
		int index = -1;

		for (int i = 0, size = stack.size(); i < size; ++i) {
			CmdCode cmd = stack.get(i);
			if (cmd.type == Command.FOR) {
				if (forLct == null
						|| (forLct.getRow() == cmd.row && forLct.getCol() == cmd.col)) {
					index = i;
					break;
				}
			}
		}

		if (index == -1) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("break" + mm.getMessage("engine.needInFor"));
		}

		for (int i = 0; i < index; ++i) {
			// 跳出内部for循环
			CmdCode cmd = stack.removeFirst();
			if (cmd.type == Command.FOR) {
				endForCommand((ForCmdCode) cmd);
			}
		}

		CmdCode cmd = stack.removeFirst();
		endForCommand((ForCmdCode) cmd);
		setNext(cmd.blockEndRow + 1, 1, true);
	}

	private void runGotoCmd(Command command) {
		CellLocation lct = command.getCellLocation(getContext());
		if (lct == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(command.getLocation()
					+ mm.getMessage("cellset.cellNotExist"));
		}

		int r = lct.getRow();
		int c = lct.getCol();
		int index = -1;

		for (int i = 0, size = stack.size(); i < size; ++i) {
			CmdCode cmd = stack.get(i);
			if (r > cmd.blockEndRow || r < cmd.row) {
				index = i;
			} else if (c <= cmd.col) {
				if (r == cmd.row) {
					index = i;
				} else {
					// 不能跳到循环内的前面的空白格
					MessageManager mm = EngineMessage.get();
					throw new RQException(
							mm.getMessage("cellset.invalidGotoCell"));
				}
			} else {
				break;
			}
		}

		for (int i = 0; i <= index; ++i) {
			// 跳出内部for循环
			CmdCode cmd = stack.removeFirst();
			if (cmd.type == Command.FOR) {
				endForCommand((ForCmdCode) cmd);
			}
		}

		setNext(r, c, false);
	}

	// 重设序列的循环序号
	private void endForCommand(ForCmdCode cmd) {
		if (cmd instanceof CursorForCmdCode) {
			((CursorForCmdCode) cmd).close();
		}
	}

	// 创建用于执行fork命令的网格
	private PgmCellSet newForkPgmCellSet(int row, int col, int endRow,
			Context ctx, boolean isLocal) {
		int rowCount = getRowCount();
		int colCount = getColCount();
		PgmCellSet pcs = new PgmCellSet(rowCount, colCount);

		if (isLocal) {
			for (int r = 1; r <= rowCount; ++r) {
				for (int c = 1; c <= colCount; ++c) {
					PgmNormalCell cell = getPgmNormalCell(r, c);
					PgmNormalCell newCell = pcs.getPgmNormalCell(r, c);
					newCell.setExpString(cell.getExpString());
					newCell.setValue(cell.getValue());
				}
			}

			pcs.setContext(ctx.newComputeContext());
		} else {
			// 多机调用时，如果引用了变量和fork代码块外的格子则传送这些变量和格子值
			ParamList usedParams = new ParamList();
			ArrayList<INormalCell> usedCells = new ArrayList<INormalCell>();

			for (int r = row; r <= endRow; ++r) {
				for (int c = col + 1; c <= colCount; ++c) {
					PgmNormalCell cell = getPgmNormalCell(r, c);
					cell.getUsedParamsAndCells(usedParams, usedCells);
					PgmNormalCell newCell = pcs.getPgmNormalCell(r, c);
					newCell.setExpString(cell.getExpString());
				}
			}

			pcs.setParamList(usedParams);
			for (INormalCell cell : usedCells) {
				int r = cell.getRow();
				int c = cell.getCol();
				if (r < row || r > endRow || c < col) {
					pcs.getPgmNormalCell(r, c).setValue(cell.getValue());
				}
			}
		}

		pcs.name = name;
		return pcs;
	}

	private void executeFork(int row, int col, int endRow) {
		curLct = new CellLocation(row, col);
		setNext(row, col + 1, false);
		CellLocation lct = curLct;
		if (lct == null || lct.getRow() > endRow)
			return;

		do {
			lct = runNext2();
		} while (lct != null && lct.getRow() <= endRow && resultValue == null);

		if (resultValue == null) {
			// 未碰到result和end缺省返回代码中最后一个计算格值
			int colCount = getColCount();
			for (int r = endRow; r >= row; --r) {
				for (int c = colCount; c > col; --c) {
					PgmNormalCell cell = getPgmNormalCell(r, c);
					if (cell.isCalculableCell() || cell.isCalculableBlock()) {
						Object val = cell.getValue();
						resultValue = new Sequence(1);
						resultValue.add(val);
						return;
					}
				}
			}
		}
	}

	private Object getForkResult() {
		if (resultValue != null) {
			if (resultValue.length() == 0) {
				return null;
			} else if (resultValue.length() == 1) {
				return resultValue.get(1);
			} else {
				return resultValue;
			}
		} else {
			return null;
		}
	}

	// 判断fork后是否紧跟着fork，连续的fork并行执行
	private boolean isNextCommandBlock(int prevEndRow, int col, byte cmdType) {
		int totalRowCount = getRowCount();
		if (prevEndRow == totalRowCount) {
			return false;
		}

		int nextRow = prevEndRow + 1;
		PgmNormalCell cell = getPgmNormalCell(nextRow, col);
		Command nextCommand = cell.getCommand();
		if (nextCommand == null || nextCommand.getType() != cmdType) {
			return false;
		}

		for (int c = 1; c < col; ++c) {
			cell = getPgmNormalCell(nextRow, c);
			if (!cell.isBlankCell()) {
				return false;
			}
		}

		return true;
	}

	private void runForkCmd(Command command, Context ctx) {
		int row = curLct.getRow();
		int col = curLct.getCol();
		int endRow = getCodeBlockEndRow(row, col);
		IParam param = command.getParam(this, ctx);

		// 只有单个fork
		if (!isNextCommandBlock(endRow, col, Command.FORK)) {
			runForkCmd(param, row, col, endRow, ctx);
		} else {
			// 多个连续的fork并行执行
			ArrayList<SubForkJob> list = new ArrayList<SubForkJob>();
			while (true) {
				SubForkJob job = new SubForkJob(param, row, col, endRow, ctx);
				list.add(job);

				if (isNextCommandBlock(endRow, col, Command.FORK)) {
					row = endRow + 1;
					endRow = getCodeBlockEndRow(row, col);
					PgmNormalCell cell = getPgmNormalCell(row, col);
					command = cell.getCommand();
					param = command.getParam(this, ctx);
				} else {
					break;
				}
			}

			ThreadPool pool = ThreadPool.newInstance(list.size());
			try {
				for (SubForkJob job : list) {
					pool.submit(job);
				}

				for (SubForkJob job : list) {
					job.join();
				}
			} finally {
				pool.shutdown();
			}
		}

		setNext(endRow + 1, 1, true);
	}

	// fork ….;h,s
	private void runForkxCmd(IParam param, int row, int col, int endRow,
			Context ctx) {
		if (param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("fork"
					+ mm.getMessage("function.invalidParam"));
		}

		IParam leftParam = param.getSub(0);
		IParam rightParam = param.getSub(1);
		Object hostObj;
		if (rightParam == null || !rightParam.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("fork"
					+ mm.getMessage("function.invalidParam"));
		} else {
			hostObj = rightParam.getLeafExpression().calculate(ctx);
		}

		Machines mc = new Machines();
		if (!mc.set(hostObj)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("callx"
					+ mm.getMessage("function.invalidParam"));
		}

		String[] hosts = mc.getHosts();
		int[] ports = mc.getPorts();

		int mcount = -1; // 并行数
		Object[] args = null; // 参数
		if (leftParam == null) {
		} else if (leftParam.isLeaf()) {
			Object val = leftParam.getLeafExpression().calculate(ctx);
			if (val instanceof Sequence) {
				int len = ((Sequence) val).length();
				if (len == 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("fork"
							+ mm.getMessage("function.invalidParam"));
				}

				mcount = len;
			}

			args = new Object[] { val };
		} else {
			int pcount = leftParam.getSubSize();
			args = new Object[pcount];
			for (int p = 0; p < pcount; ++p) {
				IParam sub = leftParam.getSub(p);
				if (sub != null) {
					args[p] = sub.getLeafExpression().calculate(ctx);
					if (args[p] instanceof Sequence) {
						int len = ((Sequence) args[p]).length();
						if (len == 0) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("fork"
									+ mm.getMessage("function.invalidParam"));
						}

						if (mcount == -1) {
							mcount = len;
						} else if (mcount != len) {
							MessageManager mm = EngineMessage.get();
							throw new RQException(
									"fork"
											+ mm.getMessage("function.paramCountNotMatch"));
						}
					}
				}
			}
		}

		// 复制fork代码块成新网格传给节点机
		PgmCellSet pcs = newForkPgmCellSet(row, col, endRow, ctx, false);
		ParallelCaller caller = new ParallelCaller(pcs, hosts, ports);
		caller.setContext(ctx);
		// if (mcount > 0 && mcount < hosts.length) {
		// caller.setOptions("a");
		// }

		// 查找有没有对应的reduce
		int nextRow = endRow + 1;
		if (nextRow <= getRowCount()) {
			Command command = getPgmNormalCell(nextRow, col).getCommand();
			if (command != null && command.getType() == Command.REDUCE) {
				int reduceEndRow = getCodeBlockEndRow(nextRow, col);
				PgmCellSet reduce = newForkPgmCellSet(nextRow, col,
						reduceEndRow, ctx, false);
				caller.setReduce(reduce, new CellLocation(row, col),
						new CellLocation(nextRow, col));
			}
		}

		if (args != null) {
			// 通过网格变量传递参数，然后设置fork所在格的表达式为=变量
			final String pname = "tmp_fork_param";
			ParamList pl = pcs.getParamList();
			if (pl == null) {
				pl = new ParamList();
				pcs.setParamList(pl);
			}

			pl.add(0, new Param(pname, Param.VAR, null));
			pcs.getPgmNormalCell(row, col).setExpString("=" + pname);

			if (mcount == -1) {
				mcount = 1;
			}

			int pcount = args.length;
			for (int i = 1; i <= mcount; ++i) {
				ArrayList<Object> list = new ArrayList<Object>(1);
				if (pcount == 1) {
					if (args[0] instanceof Sequence) {
						Sequence sequence = (Sequence) args[0];
						list.add(sequence.get(i));
					} else {
						list.add(args[0]);
					}
				} else {
					Sequence seq = new Sequence(pcount);
					list.add(seq);

					for (int p = 0; p < pcount; ++p) {
						if (args[p] instanceof Sequence) {
							Sequence sequence = (Sequence) args[p];
							seq.add(sequence.get(i));
						} else {
							seq.add(args[p]);
						}
					}
				}

				caller.addCall(list);
			}
		}

		JobSpace js = ctx.getJobSpace();
		if (js != null)
			caller.setJobSpaceId(js.getID());

		Object result = caller.execute();
		getPgmNormalCell(row, col).setValue(result);
	}

	// 执行fork程序格
	private void runForkCmd(IParam param, int row, int col, int endRow,
			Context ctx) {
		Object[] args;
		if (param == null) {
			args = new Object[] { null };
			// MessageManager mm = EngineMessage.get();
			// throw new RQException("fork" +
			// mm.getMessage("function.missingParam"));
		} else if (param.getType() == IParam.Semicolon) {
			runForkxCmd(param, row, col, endRow, ctx);
			return;
		} else if (param.isLeaf()) {
			Object val = param.getLeafExpression().calculate(ctx);
			if (val instanceof MultipathCursors) {
				ICursor[] cursors = ((MultipathCursors) val)
						.getParallelCursors();
				val = new Sequence(cursors);
			}

			args = new Object[] { val };
		} else {
			int pcount = param.getSubSize();
			args = new Object[pcount];
			for (int i = 0; i < pcount; ++i) {
				IParam sub = param.getSub(i);
				if (sub == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("fork"
							+ mm.getMessage("function.invalidParam"));
				}

				args[i] = sub.getLeafExpression().calculate(ctx);
			}
		}

		int pcount = args.length; // 参数个数
		int mcount = -1; // 并行数
		for (int i = 0; i < pcount; ++i) {
			if (args[i] instanceof Sequence) {
				int len = ((Sequence) args[i]).length();
				if (len == 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("fork"
							+ mm.getMessage("function.invalidParam"));
				}

				if (mcount == -1) {
					mcount = len;
				} else if (mcount != len) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("fork"
							+ mm.getMessage("function.paramCountNotMatch"));
				}
			}
		}

		if (mcount == -1) {
			mcount = 1;
		}

		ForkJob[] jobs = new ForkJob[mcount];
		Sequence result = new Sequence(mcount);
		ThreadPool pool = ThreadPool.newInstance(mcount);

		try {
			for (int i = 0; i < mcount; ++i) {
				// 如果是游标重新设置一下上下文，否则游标里附加的运算用同一个上下文会受影响
				PgmCellSet pcs = newForkPgmCellSet(row, col, endRow, ctx, true);
				pcs.forkCmdCode = new ForkCmdCode(row, col, endRow, i + 1);
				
				Context newCtx = pcs.getContext();

				Object val;
				if (pcount == 1) {
					if (args[0] instanceof Sequence) {
						val = ((Sequence) args[0]).get(i + 1);
					} else {
						val = args[0];
					}

					if (val instanceof ICursor) {
						((ICursor) val).setContext(newCtx);
					}
				} else {
					Sequence seq = new Sequence(pcount);
					val = seq;
					for (int p = 0; p < pcount; ++p) {
						if (args[p] instanceof Sequence) {
							Object mem = ((Sequence) args[p]).get(i + 1);
							seq.add(mem);
							if (mem instanceof ICursor) {
								((ICursor) mem).setContext(newCtx);
							}
						} else {
							seq.add(args[p]);
							if (args[p] instanceof ICursor) {
								((ICursor) args[p]).setContext(newCtx);
							}
						}
					}
				}

				pcs.getPgmNormalCell(row, col).setValue(val);
				jobs[i] = new ForkJob(pcs, row, col, endRow);
				pool.submit(jobs[i]);
			}

			for (int i = 0; i < mcount; ++i) {
				jobs[i].join();
				result.add(jobs[i].getResult());
			}
		} finally {
			pool.shutdown();
		}

		getPgmNormalCell(row, col).setValue(result);
	}

	private void runChannelCmd(Command command, Context ctx) {
		IParam param = command.getParam(this, ctx);
		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("channel"
					+ mm.getMessage("function.invalidParam"));
		}

		Object obj = param.getLeafExpression().calculate(ctx);
		if (!(obj instanceof ICursor)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("channel"
					+ mm.getMessage("function.paramTypeError"));
		}

		ICursor cs = (ICursor) obj;
		CellLocation curLct = this.curLct;
		int row = curLct.getRow();
		int col = curLct.getCol();
		int endRow = getCodeBlockEndRow(row, col);

		// 对每个channel所在的格子创建管道，并设为单元格值
		ArrayList<PgmNormalCell> cellList = new ArrayList<PgmNormalCell>();
		PgmNormalCell cell = getPgmNormalCell(row, col);

		// 定义完管道的运算再给游标附件push运算，因为游标可能调用fetch@0缓存了一部分数据
		// 如果还没定义完管道的运算，则游标缓存的数据则不会执行管道后来附加的运算
		Channel channel = cs.newChannel(ctx, false);
		cell.setValue(channel);
		cellList.add(cell);
		setNext(row, col + 1, false);

		do {
			curLct = runNext2();
		} while (curLct != null && curLct.getRow() <= endRow);

		channel.addPushToCursor(cs);

		// 找连续不带参数的channel代码块
		while (isNextCommandBlock(endRow, col, Command.CHANNEL)) {
			row = endRow + 1;
			cell = getPgmNormalCell(row, col);
			if (cell.getCommand().getParam(this, ctx) != null) {
				break;
			}

			endRow = getCodeBlockEndRow(row, col);
			channel = cs.newChannel(ctx, false);
			cell.setValue(channel);
			cellList.add(cell);

			setNext(row, col + 1, false);
			do {
				curLct = runNext2();
			} while (curLct != null && curLct.getRow() <= endRow);

			channel.addPushToCursor(cs);
		}

		// 遍历游标数据
		if (cs instanceof MultipathCursors) {
			MultipathCursors mcs = (MultipathCursors) cs;
			ICursor[] cursors = mcs.getCursors();
			int csCount = cursors.length;
			ThreadPool pool = ThreadPool.newInstance(csCount);

			try {
				CursorLooper[] loopers = new CursorLooper[csCount];
				for (int i = 0; i < csCount; ++i) {
					loopers[i] = new CursorLooper(cursors[i]);
					pool.submit(loopers[i]);
				}

				for (CursorLooper looper : loopers) {
					looper.join();
				}
			} finally {
				pool.shutdown();
			}
		} else {
			while (true) {
				Sequence src = cs.fuzzyFetch(ICursor.FETCHCOUNT);
				if (src == null || src.length() == 0)
					break;
			}
		}

		// 取出管道的计算结果设给单元格
		for (PgmNormalCell chCell : cellList) {
			channel = (Channel) chCell.getValue();
			chCell.setValue(channel.result());
		}

		setNext(endRow + 1, 1, true);
	}

	private void runReturnCmd(Command command) {
		hasReturn = true;
		Context ctx = getContext();
		Expression[] exps = command.getExpressions(this, ctx);
		int count = exps.length;

		resultValue = new Sequence(count);
		for (int i = 0; i < count; ++i) {
			if (exps[i] == null) {
				resultValue.add(null);
			} else {
				Object obj = exps[i].calculate(ctx);
				resultValue.add(obj);
			}
		}

		int r = curLct.getRow();
		int c = curLct.getCol();
		getCell(r, c).setValue(resultValue);
		resultLct = new CellLocation(r, c);
		
		// 程序网游标需要多个return，所以不结束程序执行，调用的地方自己判断是否结束
		setNext(r, c + 1, false);
		//runFinished();
	}

	private void runSqlCmd(NormalCell cell, SqlCommand command) {
		Context ctx = getContext();
		Object dbObj;
		Expression dbExp = command.getDbExpression(this, ctx);

		if (dbExp != null) {
			if (command.isLogicSql()) {
				dbObj = FileObject.createSimpleQuery();
				curDb = dbObj;
			} else {
				Object obj = dbExp.calculate(ctx);
				if (!(obj instanceof DBObject) && !(obj instanceof IQueryable)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(command.getDb()
							+ mm.getMessage("engine.dbsfNotExist"));
				}

				dbObj = obj;
				curDb = dbObj;
			}
		} else {
			if (curDb == null) {
				DBSession dbs = ctx.getDBSession();
				if (dbs == null) {
					dbObj = FileObject.createSimpleQuery();
					curDb = dbObj;
				} else {
					dbObj = new DBObject(dbs);
					curDb = dbObj;
				}
			} else {
				dbObj = curDb;
			}
		}

		// 跳过代码块
		// int endRow = getCodeBlockEndRow(curLct.getRow(), curLct.getCol());
		String sql = command.getSql();
		if (sql == null) {
			setNext(curLct.getRow(), curLct.getCol() + 1, false);
			return;
		}

		IParam param = command.getParam(this, ctx);
		Object[] paramVals = null;
		byte[] types = null;

		if (param != null) {
			ParamInfo2 pi = ParamInfo2.parse(param, "SQL command", true, false);
			paramVals = pi.getValues1(ctx);
			Object[] typeObjs = pi.getValues2(ctx);
			int count = typeObjs.length;

			types = new byte[count];
			for (int i = 0; i < count; ++i) {
				if (typeObjs[i] == null)
					continue;
				if (!(typeObjs[i] instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("SQL command"
							+ mm.getMessage("function.paramTypeError"));
				}

				types[i] = ((Number) typeObjs[i]).byteValue();
			}
		}

		Object val;
		if (dbObj instanceof DBObject) {
			String opt = command.getOption();
			if (command.isQuery()) {
				if (opt == null || opt.indexOf('1') == -1) {
					val = ((DBObject) dbObj).query(sql, paramVals, types, opt,
							ctx);
				} else {
					val = ((DBObject) dbObj).query1(sql, paramVals, types, opt);
				}
			} else {
				val = ((DBObject) dbObj).execute(sql, paramVals, types, opt);
			}
		} else {
			val = ((IQueryable)dbObj).query(sql, paramVals, this, ctx);
		}

		cell.setValue(val);
		// 跳过代码块
		// setNext(endRow + 1, 1, true);
		setNext(curLct.getRow(), curLct.getCol() + 1, false);
	}

	private void runTryCmd(NormalCell cell, Command command) {
		int row = cell.getRow();
		int col = cell.getCol();
		int endRow = getCodeBlockEndRow(row, col);
		CmdCode cmdCode = new CmdCode(Command.TRY, row, col, endRow);
		stack.addFirst(cmdCode);
		setNext(row, col + 1, false);
	}

	private void clearArea(IParam startParam, IParam endParam, Context ctx) {
		if (startParam == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("clear" + mm.getMessage("function.invalidParam"));
		}

		INormalCell startCell = startParam.getLeafExpression().calculateCell(ctx);
		if (startCell == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("clear"
					+ mm.getMessage("function.invalidParam"));
		}

		ICellSet cs = startCell.getCellSet();
		int left = startCell.getCol();
		int top = startCell.getRow();
		int right;
		int bottom;

		// 仅写A1:表示清除以A1为主格的代码块格值
		if (endParam == null) {
			right = getColCount();
			bottom = getCodeBlockEndRow(top, left);
		} else {
			INormalCell endCell = endParam.getLeafExpression().calculateCell(ctx);
			if (endCell == null || endCell.getCellSet() != cs) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("clear" + mm.getMessage("function.invalidParam"));
			}

			right = endCell.getCol();
			bottom = endCell.getRow();
		}

		if (top <= bottom) {
			if (left <= right) { // 左上 - 右下
				for (int r = top; r <= bottom; ++r) {
					for (int c = left; c <= right; ++c) {
						cs.getCell(r, c).clear();
					}
				}
			} else { // 右上 - 左下
				for (int r = top; r <= bottom; ++r) {
					for (int c = left; c >= right; --c) {
						cs.getCell(r, c).clear();
					}
				}
			}
		} else {
			if (left <= right) { // 左下 - 右上
				for (int r = top; r >= bottom; --r) {
					for (int c = left; c <= right; ++c) {
						cs.getCell(r, c).clear();
					}
				}
			} else { // 右下 - 左上
				for (int r = top; r >= bottom; --r) {
					for (int c = left; c >= right; --c) {
						cs.getCell(r, c).clear();
					}
				}
			}
		}
	}

	private void runClearCmd(Command command) {
		Context ctx = getContext();
		IParam param = command.getParam(this, ctx);
		if (param == null) {
		} else if (param.isLeaf()) {
			INormalCell cell = param.getLeafExpression().calculateCell(ctx);
			if (cell == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("[]"
						+ mm.getMessage("function.invalidParam"));
			}

			cell.clear();
		} else if (param.getType() == IParam.Comma) { // ,
			int size = param.getSubSize();
			for (int i = 0; i < size; ++i) {
				IParam sub = param.getSub(i);
				if (sub == null) {
				} else if (sub.isLeaf()) {
					INormalCell cell = sub.getLeafExpression().calculateCell(
							ctx);
					if (cell == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("[]"
								+ mm.getMessage("function.invalidParam"));
					}

					cell.clear();
				} else { // :
					clearArea(sub.getSub(0), sub.getSub(1), ctx);
				}
			}
		} else if (param.getType() == IParam.Colon) { // :
			clearArea(param.getSub(0), param.getSub(1), ctx);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("clear"
					+ mm.getMessage("function.invalidParam"));
		}

		setNext(curLct.getRow(), curLct.getCol() + 1, false);
	}

	private void runEndCmd(Command command) {
		Context ctx = getContext();
		IParam param = command.getParam(this, ctx);

		if (param == null) {
			runFinished();
			// throw new RetryException("error");
		} else if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			runFinished();
			throw new RetryException("error " + Variant.toString(obj));
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("error"
					+ mm.getMessage("function.invalidParam"));
		}
	}

	// 设置下一个要执行的单元格
	public void setNext(int row, int col, boolean isCheckStack) {
		int colCount = getColCount();
		if (col > colCount) {
			row++;
			col = 1;
			isCheckStack = true; // 换行检查堆栈
		}

		if (isCheckStack) {
			while (stack.size() > 0) {
				// 下一个要执行的单元格是否在代码块范围内
				CmdCode cmd = stack.getFirst();
				if (row > cmd.blockEndRow) {
					if (cmd.type == Command.FOR) {
						// 继续下一次循环
						curLct.set(cmd.row, cmd.col);
						return;
					} else {
						// 跳出try块，继续检查堆栈
						stack.removeFirst();
					}
				} else {
					break;
				}
			}
		}

		if (row > getRowCount()) {
			runFinished();
		} else {
			// 跳过空单元格、注释格、常数格
			PgmNormalCell cell = getPgmNormalCell(row, col);
			if (cell.isBlankCell() || cell.isNoteCell() || cell.isConstCell()) {
				setNext(row, col + 1, false);
			} else if (cell.isNoteBlock()) { // 跳过注释块
				setNext(getCodeBlockEndRow(row, col) + 1, 1, true);
			} else {
				curLct.set(row, col);
			}
		}
	}

	/**
	 * 返回单元格代码块的结束行行号
	 * @param prow int
	 * @param pcol int
	 * @return int
	 */
	public int getCodeBlockEndRow(int prow, int pcol) {
		int totalRow = getRowCount();
		for (int row = prow + 1; row <= totalRow; ++row) {
			for (int c = 1; c <= pcol; ++c) {
				PgmNormalCell cell = getPgmNormalCell(row, c);
				if (!cell.isBlankCell()) {
					return row - 1;
				}
			}
		}

		return totalRow;
	}

	private CellLocation runNext2() {
		Context ctx = getContext();
		if (curLct == null) {
			// 开始执行时把下一个要执行的格设到第一个单元格上
			curLct = new CellLocation();
			setNext(1, 1, false);
			hasReturn = false;

			return curLct;
		}

		try {
			// 执行当前的单元格，并找出下一个要执行的格
			PgmNormalCell cell = getPgmNormalCell(curLct.getRow(),
					curLct.getCol());
			Command command = cell.getCommand();

			if (command == null) {
				cell.calculate();
				if (cell.isCalculableBlock() || cell.isExecutableBlock()) {
					int endRow = getCodeBlockEndRow(curLct.getRow(),
							curLct.getCol());
					setNext(endRow + 1, 1, true);
				} else {
					setNext(curLct.getRow(), curLct.getCol() + 1, false);
				}
			} else {
				byte type = command.getType();
				switch (type) {
				case Command.IF:
					runIfCmd(cell, command);
					break;
				case Command.ELSE:
				case Command.ELSEIF:
					skipCodeBlock();
					break;
				case Command.FOR:
					runForCmd(cell, command);
					break;
				case Command.CONTINUE:
					runContinueCmd(command);
					break;
				case Command.BREAK:
					runBreakCmd(command);
					break;
				case Command.FUNC:
				case Command.REDUCE:
					skipCodeBlock();
					break;
				case Command.RETURN:
				case Command.RESULT:
					// MessageManager mm = EngineMessage.get();
					// throw new
					// RQException(mm.getMessage("engine.unknownRet"));
					runReturnCmd(command);
					break;
				case Command.SQL:
					runSqlCmd(cell, (SqlCommand) command);
					break;
				case Command.CLEAR:
					runClearCmd(command);
					break;
				case Command.END:
					runEndCmd(command);
					break;
				case Command.FORK:
					runForkCmd(command, ctx);
					break;
				case Command.GOTO:
					runGotoCmd(command);
					break;
				case Command.CHANNEL:
					runChannelCmd(command, ctx);
					break;
				case Command.TRY:
					runTryCmd(cell, command);
					break;
				default:
					throw new RuntimeException();
				}
			}
		} catch (RetryException re) {
			throw re;
		} catch (RQException re) {
			String cellId = curLct.toString();
			if (name != null) {
				cellId = "[" + name + "]." + cellId;
			}
			
			String msg = re.getMessage();
			if (goCatch(cellId + ' ' + msg)) {
				MessageManager mm = EngineMessage.get();
				msg = mm.getMessage("error.cell", cellId) + msg;
				Logger.error(msg, re);
			} else {
				MessageManager mm = EngineMessage.get();
				msg = mm.getMessage("error.cell", cellId) + msg;
				re.setMessage(msg);
				throw re;
			}
		} catch (Throwable e) {
			String cellId = curLct.toString();
			if (name != null) {
				cellId = "[" + name + "]." + cellId;
			}
			
			String msg = e.getMessage();
			if (goCatch(msg)) {
				MessageManager mm = EngineMessage.get();
				msg = mm.getMessage("error.cell", cellId) + msg;
				Logger.error(msg, e);
			} else {
				MessageManager mm = EngineMessage.get();
				msg = mm.getMessage("error.cell", cellId) + msg;
				throw new RQException(msg, e);
			}
		}

		return curLct;
	}

	private boolean goCatch(String error) {
		while (stack.size() > 0) {
			// 下一个要执行的单元格是否在代码块范围内
			CmdCode cmd = stack.getFirst();
			if (cmd.type == Command.TRY) {
				stack.removeFirst();
				setNext(cmd.blockEndRow + 1, 1, true);
				getPgmNormalCell(cmd.row, cmd.col).setValue(error);
				return true;
			} else {
				stack.removeFirst();
			}
		}

		return false;
	}

	/**
	 * 执行下一个单元格，如果返回空则执行完毕
	 * @return CellLocation
	 */
	public CellLocation runNext() {
		// try {
		// enterTask();
		return runNext2();
		// } finally {
		// leaveTask();
		// }
	}

	/**
	 * 从当前格继续执行，直到运算结束
	 */
	public void run() {
		while (true) {
			if (isInterrupted) {
				isInterrupted = false;
				break;
			}
			
			if (runNext2() == null) {
				break;
			} else if (hasReturn()) {
				// 碰到return则停止执行
				runFinished();
				break;
			}
		}
	}

	/**
	 * 依次计算代码块中除主格外的单元格，返回最后一个计算格的结果
	 * @param int row
	 * @param int col
	 * @return Object
	 */
	public Object executeSubCell(int row, int col) {
		int rowCount = getRowCount();
		int colCount = getColCount();
		if (row < 1 || row > rowCount || col < 1 || col > colCount) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(CellLocation.getCellId(row, col)
					+ mm.getMessage("cellset.cellNotExist"));
		}

		if (col == colCount)
			return null;

		// 保存现场
		CellLocation oldLct = curLct;
		LinkedList<CmdCode> oldStack = stack;
		Object retVal = null;

		try {
			curLct = new CellLocation();
			stack = new LinkedList<CmdCode>();

			int endRow = getCodeBlockEndRow(row, col);
			setNext(row, col + 1, false); // 不执行主格,从子格开始执行
			for (; curLct != null;) {
				int curRow = curLct.getRow();
				if (curRow > endRow)
					break;
				int curCol = curLct.getCol();

				runNext2();

				PgmNormalCell cell = getPgmNormalCell(curRow, curCol);
				if (cell.isCalculableCell() || cell.isCalculableBlock()) {
					retVal = cell.getValue();
				}
			}
		} finally {
			// 恢复现场
			curLct = oldLct;
			stack = oldStack;
		}

		return retVal;
	}

	/**
	 * 执行某个单元格，如果是循环块则执行代码块
	 * @param row int 单元格行号
	 * @param col int 单元格列号
	 */
	public void runCell(int row, int col) {
		// if (curLct == null) checkLicense();

		PgmNormalCell cell = getPgmNormalCell(row, col);
		if (!cell.needCalculate())
			return;

		// 保存现场
		CellLocation oldLct = curLct;
		LinkedList<CmdCode> oldStack = stack;

		try {
			// enterTask();

			curLct = new CellLocation(row, col);
			stack = new LinkedList<CmdCode>();
			Command cmd = cell.getCommand();
			if (cmd == null) {
				cell.calculate();
			} else {
				CellLocation lct;
				int endRow;
				byte type = cmd.getType();

				switch (type) {
				case Command.ELSE:
				case Command.ELSEIF:
				case Command.CONTINUE:
				case Command.BREAK:
				case Command.FUNC:
				case Command.REDUCE:
					break;
				case Command.RETURN:
				case Command.RESULT:
					runReturnCmd(cmd);
					break;
				case Command.IF:
					endRow = getIfBlockEndRow(row, col);
					do {
						lct = runNext2();
					} while (lct != null && lct.getRow() <= endRow);
					break;
				case Command.FOR:
					endRow = getCodeBlockEndRow(row, col);
					do {
						lct = runNext2();
					} while (lct != null && lct.getRow() <= endRow);
					break;
				case Command.SQL:
					runSqlCmd(cell, (SqlCommand) cmd);
					break;
				case Command.CLEAR:
					runClearCmd(cmd);
					break;
				case Command.END:
					runEndCmd(cmd);
					break;
				case Command.FORK:
					runForkCmd(cmd, getContext());
					break;
				case Command.CHANNEL:
					runChannelCmd(cmd, getContext());
					break;
				case Command.TRY:
					// runTryCmd(cell, cmd);
					break;
				default:
					throw new RuntimeException();
				}
			}
		} finally {
			// leaveTask();

			// 恢复现场
			curLct = oldLct;
			stack = oldStack;
		}
	}

	/**
	 * 执行指定格子的子函数，可递归调用
	 * @param row int 子函数所在的行
	 * @param col int 子函数所在的列
	 * @param args Object[] 参数数组
	 * @param opt String i：不递归调用，不用复制网格
	 * @return Object 子函数返回值
	 */
	public Object executeFunc(int row, int col, Object[] args, String opt) {
		PgmNormalCell cell = getPgmNormalCell(row, col);
		Command cmd = cell.getCommand();
		if (cmd == null || cmd.getType() != Command.FUNC) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("engine.callNeedSub"));
		}

		String expStr = cmd.getExpression();
		if (expStr != null && expStr.length() > 0) {
			int nameEnd = KeyWord.scanId(expStr, 0);
			String fnName = expStr.substring(0, nameEnd);
			return executeFunc(fnName, args, opt);
		}

		int endRow = getCodeBlockEndRow(row, col);
		if (opt != null && opt.indexOf('i') != -1) {
			CellLocation oldLct = curLct;
			Object result = executeFunc(row, col, endRow, args);
			curLct = oldLct;
			return result;
		}

		// 共享函数体外的格子
		PgmCellSet pcs = newCalc();
		int colCount = getColCount();
		for (int r = row; r <= endRow; ++r) {
			for (int c = col; c <= colCount; ++c) {
				INormalCell tmp = getCell(r, c);
				INormalCell cellClone = (INormalCell) tmp.deepClone();
				cellClone.setCellSet(pcs);
				pcs.cellMatrix.set(r, c, cellClone);
			}
		}

		return pcs.executeFunc(row, col, endRow, args);
	}

	/**
	 * 根据函数名取函数信息
	 * @param fnName 函数名
	 * @return
	 */
	public FuncInfo getFuncInfo(String fnName) {
		if (fnMap == null) {
			// 遍历网格中定义的函数，生成函数名映射表
			fnMap = new HashMap<String, FuncInfo>();
			int rowCount = getRowCount();
			int colCount = getColCount();
			Context ctx = getContext();

			for (int r = 1; r <= rowCount; ++r) {
				for (int c = 1; c <= colCount; ++c) {
					PgmNormalCell cell = getPgmNormalCell(r, c);
					Command command = cell.getCommand();
					if (command == null || command.getType() != Command.FUNC) {
						continue;
					}

					String expStr = command.getExpression();
					if (expStr == null || expStr.length() == 0) {
						continue;
					}

					int len = expStr.length();
					int nameEnd = KeyWord.scanId(expStr, 0);
					if (nameEnd == len) {
						FuncInfo funcInfo = new FuncInfo(cell, null);
						fnMap.put(expStr, funcInfo);
					} else {
						String name = expStr.substring(0, nameEnd);
						for (; nameEnd < len
								&& Character.isWhitespace(expStr
										.charAt(nameEnd)); ++nameEnd) {
						}

						if (nameEnd == len) {
							FuncInfo funcInfo = new FuncInfo(cell, null);
							fnMap.put(name, funcInfo);
						} else if (expStr.charAt(nameEnd) == '('
								&& expStr.charAt(len - 1) == ')') {
							String[] argNames = null;
							IParam param = ParamParser.parse(
									expStr.substring(nameEnd + 1, len - 1),
									this, ctx, false);
							if (param != null) {
								argNames = param.toStringArray("func", false);
							}

							FuncInfo funcInfo = new FuncInfo(cell, argNames);
							fnMap.put(name, funcInfo);
						} else {
							MessageManager mm = EngineMessage.get();
							throw new RQException("func"
									+ mm.getMessage("function.invalidParam"));
						}
					}
				}
			}
		}

		FuncInfo funcInfo = fnMap.get(fnName);
		if (funcInfo != null) {
			return funcInfo;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(fnName
					+ mm.getMessage("Expression.unknownFunction"));
		}
	}

	/**
	 * 执行指定名字的子函数，可递归调用
	 * @param fnName 函数名
	 * @param args Object[] 参数数组
	 * @param opt String i：不递归调用，不用复制网格
	 * @return Object 子函数返回值
	 */
	public Object executeFunc(String fnName, Object[] args, String opt) {
		FuncInfo funcInfo = getFuncInfo(fnName);
		PgmNormalCell cell = funcInfo.getCell();
		int row = cell.getRow();
		int col = cell.getCol();
		int colCount = getColCount();
		int endRow = getCodeBlockEndRow(row, col);

		// 共享函数体外的格子
		PgmCellSet pcs = newCalc();
		String[] argNames = funcInfo.getArgNames();
		if (argNames != null) {
			// 把参数设到上下文中
			int argCount = argNames.length;
			if (args == null || args.length != argCount) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(fnName
						+ mm.getMessage("function.paramCountNotMatch"));
			}

			Context ctx = pcs.getContext();
			for (int i = 0; i < argCount; ++i) {
				ctx.setParamValue(argNames[i], args[i]);
			}
		}

		for (int r = row; r <= endRow; ++r) {
			for (int c = col; c <= colCount; ++c) {
				INormalCell tmp = getCell(r, c);
				INormalCell cellClone = (INormalCell) tmp.deepClone();
				cellClone.setCellSet(pcs);
				pcs.cellMatrix.set(r, c, cellClone);
			}
		}

		// 定义了名字和参数的函数不再将参数填入单元格
		return pcs.executeFunc(row, col, endRow, null); // args
	}

	private Object executeFunc(int row, int col, int endRow, Object[] args) {
		int colCount = getColCount();

		// 把参数值设到func单元格上及后面的格子
		if (args != null) {
			int paramRow = row;
			int paramCol = col;
			for (int i = 0, pcount = args.length; i < pcount; ++i) {
				getPgmNormalCell(paramRow, paramCol).setValue(args[i]);
				if (paramCol < colCount) {
					paramCol++;
				} else {
					break;
					// if (paramRow == getRowCount() && i < paramCount - 1) {
					// MessageManager mm = EngineMessage.get();
					// throw new RQException("call" +
					// mm.getMessage("function.paramCountNotMatch"));
					// }
					// paramRow++;
					// paramCol = 1;
				}
			}
		}

		curLct = new CellLocation(row, col);
		setNext(row, col + 1, false); // 从子格开始执行

		for (; curLct != null;) {
			int curRow = curLct.getRow();
			if (curRow > endRow) { // 超出了代码块，没有return格
				break;
			}

			int curCol = curLct.getCol();
			PgmNormalCell cell = getPgmNormalCell(curRow, curCol);
			Command cmd = cell.getCommand();
			if (cmd == null) {
				runNext2();
			} else if (cmd.getType() == Command.RETURN) {
				Context ctx = getContext();
				Expression exp = cmd.getExpression(this, ctx);
				if (exp != null) {
					return exp.calculate(ctx);
				} else {
					return null;
				}
			} else {
				runNext2();
			}
		}

		// 未碰到return缺省返回代码块中最后一个计算格值
		for (int r = endRow; r >= row; --r) {
			for (int c = colCount; c > col; --c) {
				PgmNormalCell cell = getPgmNormalCell(r, c);
				if (cell.isCalculableCell() || cell.isCalculableBlock()) {
					return cell.getValue();
				}
			}
		}

		return null;
	}

	/**
	 * 执行子函数，不可递归调用
	 * @param row int 子函数所在的行
	 * @param col int 子函数所在的列
	 * @param args Object[] 参数数组
	 * @return Object 子函数返回值
	 */
	/*
	 * public Object executeFunc_nr(int row, int col, Object []args) { if (row <
	 * 1 || row > getRowCount() || col < 1 || col > getColCount()) {
	 * MessageManager mm = EngineMessage.get(); throw new
	 * RQException(mm.getMessage("engine.callNeedSub")); }
	 * 
	 * PgmNormalCell cell = getPgmNormalCell(row, col); Command cmd =
	 * cell.getCommand(); if (cmd == null || cmd.getType() != Command.FUNC) {
	 * MessageManager mm = EngineMessage.get(); throw new
	 * RQException(mm.getMessage("engine.callNeedSub")); }
	 * 
	 * // 把参数值设到func单元格上及后面的格子 if (args != null) { int paramRow = row; int
	 * paramCol = col; for (int i = 0, pcount = args.length; i < pcount; ++i) {
	 * getPgmNormalCell(paramRow, paramCol).setValue(args[i]); if (paramCol <
	 * getColCount()) { paramCol++; } else { break; //if (paramRow ==
	 * getRowCount() && i < paramCount - 1) { // MessageManager mm =
	 * EngineMessage.get(); // throw new RQException("call" +
	 * mm.getMessage("function.paramCountNotMatch")); //} //paramRow++;
	 * //paramCol = 1; } } }
	 * 
	 * 
	 * // 保存现场 CellLocation oldLct = curLct; LinkedList <CmdCode> oldStack =
	 * stack;
	 * 
	 * try { curLct = new CellLocation(row, col); stack = new LinkedList
	 * <CmdCode>(); setNext(row, col + 1, false); // 从子格开始执行 int endRow =
	 * getCodeBlockEndRow(row, col);
	 * 
	 * for (; curLct != null;) { int curRow = curLct.getRow(); if (curRow >
	 * endRow) { // 超出了代码块，没有return格 return null; }
	 * 
	 * int curCol = curLct.getCol(); cell = getPgmNormalCell(curRow, curCol);
	 * cmd = cell.getCommand(); if (cmd == null) { runNext2(); } else if
	 * (cmd.getType() == Command.RETURN) { Context ctx = getContext();
	 * Expression exp = cmd.getExpression(this, ctx); if (exp != null) { return
	 * exp.calculate(ctx); } else { return null; } } else { runNext2(); } } }
	 * finally { // 恢复现场 curLct = oldLct; stack = oldStack; }
	 * 
	 * return null; }
	 */

	/**
	 * 结束运行，释放资源，格值仍保留
	 */
	public void runFinished() {
		super.runFinished();
		curLct = null;
		stack.clear();
		curDb = null;
	}

	public void reset() {
		super.reset();
		resultValue = null;
		resultCurrent = 0;
		resultLct = null;
		interrupt = false;
		isInterrupted = false;
		hasReturn = false;
		fnMap = null;
	}

	// ---------------------------calc end------------------------------

	protected void setParseCurrent(int row, int col) {
		if (parseLct == null) {
			parseLct = new CellLocation(row, col);
		} else {
			parseLct.set(row, col);
		}
	}

	// $() $(a) $(a:b)
	public String getMacroReplaceString(String strCell) {
		Context ctx = getContext();
		strCell = Expression.replaceMacros(strCell, this, ctx);
		strCell = strCell.trim();

		int sr, sc, er, ec;
		int colonIndex;
		if (strCell != null && (colonIndex = strCell.indexOf(':')) != -1) {
			String startStr = strCell.substring(0, colonIndex);
			INormalCell startCell = getCell(startStr);
			if (startCell == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(startStr
						+ mm.getMessage("cellset.cellNotExist"));
			}

			String endStr = strCell.substring(colonIndex + 1);
			INormalCell endCell = getCell(endStr);
			if (endCell == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(endStr
						+ mm.getMessage("cellset.cellNotExist"));
			}

			sr = startCell.getRow();
			sc = startCell.getCol();
			er = endCell.getRow();
			ec = endCell.getCol();

			if (sr > er || sc > ec) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("\":\""
						+ mm.getMessage("operator.cellLocation"));
			}
		} else {
			INormalCell cell = getCell(strCell);
			if (cell == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(strCell
						+ mm.getMessage("cellset.cellNotExist"));
			}

			er = sr = cell.getRow();
			ec = sc = cell.getCol();
		}

		StringBuffer buffer = new StringBuffer(100);
		for (int r = sr; r <= er; ++r) {
			for (int c = sc; c <= ec; ++c) {
				PgmNormalCell cell = getPgmNormalCell(r, c);
				if (cell.isBlankCell() || cell.isNoteCell())
					continue;
				if (cell.isNoteBlock()) {
					// 跳过注释块
					r = getCodeBlockEndRow(r, c);
					break;
				}

				setParseCurrent(r, c);
				String cellStr = cell.getMacroReplaceString();
				buffer.append(Expression.replaceMacros(cellStr, this, ctx));
				buffer.append(',');
			}
		}

		int len = buffer.length();
		return len > 0 ? buffer.substring(0, len - 1) : "";
	}

	public String getPrevCellSet(String str, int pos) {
		return null;
	}

	// call是否被中断
	public boolean isCallInterrupted() {
		return curLct != null && resultValue == null;
	}

	/**
	 * 计算网格，返回result单元格的值
	 * @return Object
	 */
	public Object execute() {
		// 运算第一个result语句
		resultValue = null;
		while (runNext2() != null && resultValue == null) {
			if (isInterrupted) {
				isInterrupted = false;
				break;
			}
		}

		if (resultValue != null) {
			if (resultValue.length() == 0) {
				return null;
			} else if (resultValue.length() == 1) {
				return resultValue.get(1);
			} else {
				return resultValue;
			}
		} else {
			// 未碰到result和end缺省返回代码中最后一个计算格值
			return getLastCalculableCellValue();
		}
	}

	// 取最后一个计算格的格值
	public Object getLastCalculableCellValue() {
		int colCount = getColCount();
		for (int r = getRowCount(); r > 0; --r) {
			for (int c = colCount; c > 0; --c) {
				PgmNormalCell cell = getPgmNormalCell(r, c);
				if (cell.isCalculableCell() || cell.isCalculableBlock()) {
					Object val = cell.getValue();
					resultValue = new Sequence(1);
					resultValue.add(val);
					return val;
				}
			}
		}

		return null;
	}

	/**
	 * 开始计算网格返回结果
	 */
	public void calculateResult() {
		execute();
	}

	/**
	 * 是否还有结果返回
	 * @return boolean
	 */
	public boolean hasNextResult() {
		if (resultValue != null && resultValue.length() > 0) {
			return true;
		} else if (curLct == null) {
			return false; // 计算完毕
		} else {
			resultValue = null;
			resultCurrent = 0;
			while (runNext2() != null && resultValue == null) {
				if (isInterrupted) {
					isInterrupted = false;
					return false;
				}
			}

			return hasNextResult();
		}
	}

	// 是否执行到了return语句
	public boolean hasReturn() {
		return hasReturn;
	}

	/**
	 * 取下一个结果
	 * @return Object
	 */
	public Object nextResult() {
		if (!hasNextResult())
			return null;

		if (resultCurrent < 1)
			resultCurrent = 1; // 首次取

		Object obj = resultValue.get(resultCurrent);
		if (resultCurrent < resultValue.length()) {
			resultValue.set(resultCurrent, null);
			resultCurrent++;
		} else {
			resultValue = null;
		}

		return obj;
	}

	/**
	 * 取下一个结果的位置
	 * @return CellLocation
	 */
	public CellLocation nextResultLocation() {
		if (!hasNextResult())
			return null;
		return resultLct;
	}

	/**
	 * 中断执行，以单元格为单位
	 */
	public void interrupt() {
		interrupt = true;
		isInterrupted = true;
	}

	public boolean getInterrupt() {
		return interrupt;
	}

	/**
	 * 设置是否自动计算
	 * @param b boolean
	 */
	public void setAutoCalc(boolean b) {
		if (b) {
			sign |= SIGN_AUTOCALC;
		} else {
			sign &= ~SIGN_AUTOCALC;
		}
	}

	/**
	 * 返回是否自动计算
	 * @return boolean
	 */
	public boolean isAutoCalc() {
		return (sign & SIGN_AUTOCALC) == SIGN_AUTOCALC;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	/**
	 * 取自定义属性映射
	 * @return ByteMap
	 */
	public ByteMap getCustomPropMap() {
		return customPropMap;
	}

	/**
	 * 设置自定义属性映射
	 * @param map ByteMap
	 */
	public void setCustomPropMap(ByteMap map) {
		if (!isExecuteOnly()) {
			this.customPropMap = map;
		}
	}

	/**
	 * 设置密码
	 * @param psw String
	 */
	public void setPassword(String psw) {
		if (psw == null || psw.length() == 0) {
			this.pswHash = null;
		} else {
			MD5 md5 = new MD5();
			this.pswHash = md5.getMD5ofStr(psw);
		}
	}

	public String getPasswordHash() {
		return pswHash;
	}

	/**
	 * 设置无密码登陆时的权限
	 * @param p int：PRIVILEGE_FULL、PRIVILEGE_EXEC
	 */
	// public void setNullPasswordPrivilege(int p) {
	// this.nullPswPrivilege = p;
	// }

	/**
	 * 取无密码登陆时的权限
	 * @return int：PRIVILEGE_FULL、PRIVILEGE_EXEC
	 */
	public int getNullPasswordPrivilege() {
		return this.nullPswPrivilege;
	}

	/**
	 * 设置当前的密码
	 * @param psw String
	 */
	public void setCurrentPassword(String psw) {
		this.curPrivilege = getPrivilege(pswHash, psw, nullPswPrivilege);
	}

	public static int getPrivilege(String pswHash, String psw,
			int nullPswPrivilege) {
		if (pswHash == null) {
			return PRIVILEGE_FULL;
		} else if (psw == null || psw.length() == 0) {
			return nullPswPrivilege;
		} else {
			MD5 md5 = new MD5();
			psw = md5.getMD5ofStr(psw);
			if (psw.equals(pswHash)) {
				return PRIVILEGE_FULL;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("cellset.pswError"));
			}
		}
	}

	/**
	 * 返回当前网格的权限
	 * @return int
	 */
	public int getCurrentPrivilege() {
		return curPrivilege;
	}

	public boolean isExecuteOnly() {
		return curPrivilege == PRIVILEGE_EXEC;
	}
}
