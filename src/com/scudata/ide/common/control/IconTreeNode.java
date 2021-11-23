package com.scudata.ide.common.control;

import javax.swing.Icon;
import javax.swing.tree.DefaultMutableTreeNode;

import com.scudata.ide.common.GC;
import com.scudata.ide.common.GM;

/**
 * 显示图标的树结点的编辑类
 *
 */
public class IconTreeNode extends DefaultMutableTreeNode {

	private static final long serialVersionUID = 1L;

	/** 文件夹 */
	public static final byte TYPE_FOLDER = 0;
	/** 表 */
	public static final byte TYPE_TABLE = 1;
	/** 字段 */
	public static final byte TYPE_COLUMN = 2;

	/**
	 * 结点类型。TYPE_FOLDER,TYPE_KEYVAL,TYPE_TABLE,TYPE_COLUMN
	 */
	private byte type = TYPE_FOLDER;

	/**
	 * 结点名称
	 */
	private String name;

	/**
	 * 构造函数
	 * 
	 * @param name
	 *            结点名称
	 */
	public IconTreeNode(String name) {
		this(name, TYPE_FOLDER);
	}

	/**
	 * 构造函数
	 * 
	 * @param name
	 *            结点名称
	 * @param type
	 *            结点类型。TYPE_FOLDER,TYPE_KEYVAL,TYPE_TABLE,TYPE_COLUMN
	 */
	public IconTreeNode(String name, byte type) {
		this.type = type;
		this.name = name;
	}

	/**
	 * 取结点名称
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * 设置结点名称
	 * 
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 取结点类型。TYPE_FOLDER,TYPE_KEYVAL,TYPE_TABLE,TYPE_COLUMN
	 * 
	 * @return
	 */
	public byte getType() {
		return type;
	}

	/**
	 * 设置结点类型。TYPE_FOLDER,TYPE_KEYVAL,TYPE_TABLE,TYPE_COLUMN
	 * 
	 * @param type
	 */
	public void setType(byte type) {
		this.type = type;
	}

	/**
	 * 转换为字符串
	 */
	public String toString() {
		return name;
	}

	/**
	 * 克隆
	 * 
	 * @return
	 */
	public IconTreeNode deepClone() {
		return new IconTreeNode(name, type);
	}

	/**
	 * 获取显示的图标
	 * 
	 * @return
	 */
	public Icon getDispIcon() {
		String url = GC.IMAGES_PATH;
		if (this.isRoot()) {
			url += "tree0.gif";
		} else if (type == TYPE_FOLDER) {
			url += "treefolder.gif";
		} else if (type == TYPE_TABLE) {
			url += "treetable.gif";
		} else if (type == TYPE_COLUMN) {
			url += "treecolumn.gif";
		}
		return GM.getImageIcon(url);
	}
}
