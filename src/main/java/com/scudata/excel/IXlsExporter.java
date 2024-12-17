package com.scudata.excel;

import java.io.IOException;

public interface IXlsExporter {
	public void writeLine(int row, Object[] items) throws IOException;
}
