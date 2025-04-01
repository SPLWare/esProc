package com.scudata.cellset.datamodel;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.scudata.cellset.IRowCell;
import com.scudata.common.ByteArrayInputRecord;
import com.scudata.common.ByteArrayOutputRecord;

public class RowCell implements IRowCell {
	private static final long serialVersionUID = 0x02010015;
	private final static byte version = (byte) 1;

	// 可视属性取值
	public final static byte VISIBLE_ALWAYS = 0; // 总是可见
	public final static byte VISIBLE_ALWAYSNOT = 1; // 总是不可见
	public final static byte VISIBLE_FIRSTPAGE = 2; // 首页可见
	public final static byte VISIBLE_FIRSTPAGENOT = 3; // 首页不可见

	private int row;
	private float height = 25.0f;
	private int level;

	private byte visible; // 可视属性
	private boolean isBreakPage; // 行后是否分页

	// 存盘时使用
	public RowCell() {
	}

	public RowCell(int row) {
		this.row = row;
	}

	/**
	 * 返回行号
	 * @return int
	 */
	public int getRow() {
		return row;
	}

	/**
	 * 设置行号
	 * @param row int
	 */
	public void setRow(int row) {
		this.row = row;
	}

	/**
	 * 设置行高
	 * @param h float
	 */
	public void setHeight(float h) {
		height = h;
	}

	/**
	 * 返回行高
	 * @return float
	 */
	public float getHeight() {
		return height;
	}

	/**
	 * 返回层号
	 * @return int
	 */
	public int getLevel() {
		return level;
	}

	/**
	 * 设置层号
	 * @param level int
	 */
	public void setLevel(int level) {
		this.level = level;
	}

	/**
	 * 返回行是否可见
	 * @return byte 取值 VISIBLE_ALWAYS、VISIBLE_ALWAYSNOT、VISIBLE_FIRSTPAGE、VISIBLE_FIRSTPAGENOT
	 */
	public byte getVisible(){
		return visible;
	}

	/**
	 * 设置行是否可见
	 * @param b byte 取值 VISIBLE_ALWAYS、VISIBLE_ALWAYSNOT、VISIBLE_FIRSTPAGE、VISIBLE_FIRSTPAGENOT
	 */
	public void setVisible(byte b){
		visible = b;
	}

	/**
	 * @return 返回行后是否分页
	 */
	public boolean isBreakPage(){
		return isBreakPage;
	}

	/**
	 * 设置行后是否分页
	 * @param b 为true则行后分页，否则不分页
	 */
	public void setBreakPage(boolean b){
		isBreakPage = b;
	}

	/**
	 * 深度克隆
	 * @return 克隆出的对象
	 */
	public Object deepClone(){
		RowCell cell = new RowCell(row);
		cell.height = height;
		cell.level = level;

		cell.visible = visible;
		cell.isBreakPage = isBreakPage;
		return cell;
	}

	/**
	 * 写内容到流
	 * @param out ObjectOutput 输出流
	 * @throws IOException
	 */
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeByte(version);

		out.writeInt(row);
		out.writeFloat(height);
		out.writeInt(level);

		out.writeByte(visible);
		out.writeBoolean(isBreakPage);
	}

	/**
	 * 从流中读内容
	 * @param in ObjectInput 输入流
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		in.readByte(); // version

		row = in.readInt();
		height = in.readFloat();
		level = in.readInt();

		visible = in.readByte();
		isBreakPage = in.readBoolean();
	}

	/**
	 * 写内容到流
	 * @throws IOException
	 * @return 输出流
	 */
	public byte[] serialize() throws IOException{
		ByteArrayOutputRecord out = new ByteArrayOutputRecord();
		out.writeInt(row);
		out.writeFloat(height);
		out.writeInt(level);

		out.writeByte(visible);
		out.writeBoolean(isBreakPage);
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
		row = in.readInt();
		height = in.readFloat();
		level = in.readInt();

		visible = in.readByte();
		isBreakPage = in.readBoolean();
	}
}
