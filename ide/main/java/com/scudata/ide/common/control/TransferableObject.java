package com.scudata.ide.common.control;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

/**
 * IDE复制时传递的对象
 */
public class TransferableObject implements Transferable {
	/**
	 * 复制的对象
	 */
	private Object object;
	/**
	 * DataFlavor对象
	 */
	public static final DataFlavor objectFlavor = new DataFlavor(
			TransferableObject.class, "object");

	/**
	 * DataFlavor数组
	 */
	static DataFlavor[] flavors = { objectFlavor };

	/**
	 * 构造函数
	 * 
	 * @param object
	 *            复制的对象
	 */
	public TransferableObject(Object object) {
		this.object = object;
	}

	/**
	 * 取传递的DataFlavor数组
	 */
	public DataFlavor[] getTransferDataFlavors() {
		return flavors;
	}

	/**
	 * DataFlavor是否被支持
	 */
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return flavor.equals(objectFlavor);
	}

	/**
	 * 从DataFlavor取传递的对象
	 */
	public synchronized Object getTransferData(DataFlavor flavor)
			throws UnsupportedFlavorException {
		if (flavor.equals(objectFlavor)) {
			return object;
		} else {
			return null;
		}
	}
}
