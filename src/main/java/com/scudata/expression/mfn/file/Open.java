package com.scudata.expression.mfn.file;

import java.io.File;

import com.scudata.dm.Context;
import com.scudata.dw.ComTable;
import com.scudata.dw.PhyTable;
import com.scudata.expression.FileFunction;
import com.scudata.parallel.ClusterFile;

/**
 * 打开组表
 * f.open()
 * @author RunQian
 *
 */
public class Open extends FileFunction {
	public Object calculate(Context ctx) {
		if (file.isRemoteFile()) {
			// 远程文件
			String host = file.getIP();
			int port = file.getPort();
			String fileName = file.getFileName();
			Integer partition = file.getPartition();
			int p = partition == null ? -1 : partition.intValue();
			ClusterFile cf = new ClusterFile(host, port, fileName, p, ctx);
			return cf.openGroupTable(ctx);
		} else {
			// 本地文件
			File f = file.getLocalFile().file();
			PhyTable table = ComTable.openBaseTable(f, ctx);
			
			Integer partition = file.getPartition();
			if (partition != null && partition.intValue() > 0) {
				table.getGroupTable().setPartition(partition);
			}
			
			return table;
		}
	}
}
