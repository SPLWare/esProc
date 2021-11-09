package com.raqsoft.cellset.datamodel;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.raqsoft.cellset.ICellSet;
import com.raqsoft.cellset.INormalCell;
import com.raqsoft.common.ByteArrayInputRecord;
import com.raqsoft.common.ByteArrayOutputRecord;
import com.raqsoft.common.ByteMap;
import com.raqsoft.common.CellLocation;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.resources.EngineMessage;

abstract public class NormalCell implements INormalCell {
	private static final long serialVersionUID = 0x02010014;

	// 单元格类型
	public final static int TYPE_CALCULABLE_CELL  = 0x00000001; // 计算格 =
	public final static int TYPE_CALCULABLE_BLOCK = 0x00000002; // 计算块 ==
	public final static int TYPE_EXECUTABLE_CELL  = 0x00000004; // 执行格 >
	public final static int TYPE_EXECUTABLE_BLOCK = 0x00000008; // 执行块 >>
	public final static int TYPE_COMMAND_CELL     = 0x00000010; // 语句格
	public final static int TYPE_CONST_CELL       = 0x00000020; // 常数格
	public final static int TYPE_NOTE_CELL        = 0x00000040; // 注释格 /
	public final static int TYPE_NOTE_BLOCK       = 0x00000080; // 注释块 //
	public final static int TYPE_BLANK_CELL       = 0x00000100; // 空白格

	protected CellSet cs;
	protected int row;
	protected int col;
	protected String expStr; // 表达式字符串
	protected String tip;

	protected Object value;

	/**
	 * 序列化时使用
	 */
	public NormalCell() {
	}

	/**
	 * 构建单元格
	 * @param cs CellSet 单元格所属的网格
	 * @param r int 行号
	 * @param c int 列号
	 */
	public NormalCell(CellSet cs, int r, int c) {
		row = r;
		col = c;
		this.cs = cs;
	}

	/**
	 * 取得当前单元格的行号
	 * @return int
	 */
	public int getRow() {
		return row;
	}

	/**
	 * 取得当前单元格的列号
	 * @return int
	 */
	public int getCol() {
		return col;
	}

	/**
	 * 设置当前的行号
	 * @param r int
	 */
	public void setRow(int r) {
		row = r;
	}

	/**
	 * 设置当前的列号
	 * @param c int
	 */
	public void setCol(int c) {
		col = c;
	}

	/**
	 * 设置单元格所属的网格
	 * @param cs ICellSet
	 */
	public void setCellSet(ICellSet cs) {
		if (this.cs != null && this.cs.isExecuteOnly()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("cellset.executeOnly"));
		}

		this.cs = (CellSet)cs;
	}

	/**
	 * 返回单元格所属的网格
	 * @return ICellSet
	 */
	public ICellSet getCellSet() {
		return cs;
	}

	/**
	 * 返回单元格标识
	 * @return String
	 */
	public String getCellId() {
		return CellLocation.getCellId(row, col);
	}

	/**
	 * 设置单元格表达式
	 * @param exp String
	 */
	public void setExpString(String exp) {
		if (cs != null && cs.isExecuteOnly()) {
			return;
		}

		this.expStr = exp;
	}

	/**
	 * @return String 返回单元格表达式
	 */
	public String getExpString() {
		if (cs != null && cs.isExecuteOnly()) {
			return null;
		}

		return expStr;
	}

	/**
	 * 设置单元格值
	 * @param value Object
	 */
	public void setValue(Object value) {
		this.value = value;
	}

	/**
	 * 返回单元格值，没有计算则返回空
	 * @return Object
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * 返回单元格值，没有计算则计算它
	 * @param doCalc boolean
	 * @return Object
	 */
	abstract public Object getValue(boolean doCalc);

	/**
	 * 写内容到流
	 * @param out ObjectOutput 输出流
	 * @throws IOException
	 */
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeByte(2);

		out.writeObject(cs);
		out.writeInt(row);
		out.writeInt(col);
		out.writeObject(expStr);
		out.writeObject(tip);
		out.writeObject(value);
	}

	/**
	 * 从流中读内容
	 * @param in ObjectInput 输入流
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		in.readByte(); // 版本

		cs = (CellSet) in.readObject();
		row = in.readInt();
		col = in.readInt();
		setExpString((String)in.readObject());
		tip = (String)in.readObject();
		value = in.readObject();
	}

	/**
	 * 写内容到流
	 * @throws IOException
	 * @return 输出流
	 */
	public byte[] serialize() throws IOException{
		ByteArrayOutputRecord out = new ByteArrayOutputRecord();

		// cellset
		out.writeInt(row);
		out.writeInt(col);
		out.writeString(expStr);
		out.writeString(tip);
		return out.toByteArray();
	}

	/**
	 * 从流中读内容
	 * @param buf byte[]
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public void fillRecord(byte[] buf) throws IOException, ClassNotFoundException {
		ByteArrayInputRecord in = new ByteArrayInputRecord(buf);

		// cellset由CellSet设进来
		row = in.readInt();
		col = in.readInt();
		setExpString(in.readString());
		tip = in.readString();
	}

	/**
	 * 计算此单元格
	 */
	abstract public void calculate();

	/**
	 * 重设单元格状态为初始状态
	 */
	abstract public void reset();

	/**
	 * 返回单元格类型
	 * @return int
	 */
	abstract public int getType();

	/**
	 * 返回是否是计算块
	 * @return boolean
	 */
	abstract public boolean isCalculableBlock();

	/**
	 * 返回是否是计算格
	 * @return boolean
	 */
	abstract public boolean isCalculableCell();

	/**
	 * 返回单元格是否需要做表达式变迁
	 * @return boolean
	 */
	abstract protected boolean needRegulateString();

	/**
	 * undo时恢复产生的错误引用
	 */
	public void undoErrorRef() {
		cs.setCell(row, col, this);
	}

	protected ByteMap getExpMap(boolean isClone) {
		return null;
	}

	protected void setExpMap(ByteMap map) {
	}

	/**
	 * 设置单元格提示
	 * @param tip String
	 */
	public void setTip(String tip) {
		if (cs != null && cs.isExecuteOnly()) {
			return;
		}

		this.tip = tip;
	}

	/**
	 * 返回单元格提示
	 * @return String
	 */
	public String getTip() {
		return tip;
	}
}
