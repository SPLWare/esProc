package com.raqsoft.ide.vdb.control;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

public class TransferableObject implements Transferable {
	private Object object;

	public static final DataFlavor objectFlavor = new DataFlavor(TransferableObject.class, "object");

	static DataFlavor[] flavors = { objectFlavor };

	public TransferableObject(Object object) {
		this.object = object;
	}

	public DataFlavor[] getTransferDataFlavors() {
		return flavors;
	}

	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return flavor.equals(objectFlavor);
	}

	public synchronized Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
		if (flavor.equals(objectFlavor)) {
			return object;
		} else {
			return null;
		}
	}
}
