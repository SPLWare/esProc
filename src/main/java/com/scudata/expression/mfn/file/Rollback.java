package com.scudata.expression.mfn.file;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import com.scudata.common.MD5;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.FileObject;
import com.scudata.dw.BufferReader;
import com.scudata.dw.ComTable;
import com.scudata.dw.PhyTable;
import com.scudata.expression.FileFunction;
import com.scudata.resources.EngineMessage;

/**
 * 如果更新组表文件失败，调用此函数恢复数据
 * @author RunQian
 *
 */
public class Rollback extends FileFunction {
	public Object calculate(Context ctx) {
		String psw = null;
		if (param != null) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (!(obj instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("rollback" + mm.getMessage("function.paramTypeError"));
			}
			
			psw = (String)obj;
		}
		
		File f = file.getLocalFile().file();
		return groupTableRollBack(f, psw, ctx);
	}
	
	public static boolean groupTableRollBack(File file, String psw, Context ctx) {
		File sf = ComTable.getSupplementFile(file);
		if (sf.exists()) {
			groupTableRollBack(sf, psw, ctx);
		}
		
		String dir = file.getAbsolutePath() + "_TransactionLog";
		FileObject logFile = new FileObject(dir);
		boolean result = false;
		if (logFile.isExists()) {
			result = true;
			RandomAccessFile raf = null;
			try {
				raf = new RandomAccessFile(logFile.getLocalFile().file(), "rw");
				int len  = (int) raf.length();
				if (len <= 48) {
					raf.close();
					logFile.delete();
					return Boolean.TRUE;
				}
				byte []bytes = new byte[len - 16];
				byte []mac1 = new byte[16];
				
				raf.seek(0);
				raf.readFully(bytes);			
				raf.readFully(mac1);
				raf.close();
				
				//检查备份日志文件的完整性
				byte []mac2 = MD5.get(bytes);
				for (int i = 0; i < 16; ++i) {
					if (mac1[i] != mac2[i]) {
						logFile.delete();
						return Boolean.TRUE;
					}
				}
				
				if (bytes[0] != 'r' || bytes[1] != 'q' || bytes[2] != 'd' || bytes[3] != 'w' || bytes[4] != 'g' || bytes[5] != 't') {
					logFile.delete();
					return Boolean.TRUE;
				}
				
				BufferReader reader = new BufferReader(null, bytes);
				reader.read(); // r
				reader.read(); // q
				reader.read(); // d
				reader.read(); // w
				reader.read(); // g
				reader.read(); // t
				reader.read(); // 
				
				reader.readInt32();
				
				reader.readLong40();
				reader.readLong40();
				reader.readInt32();
				reader.readInt32();
				
				byte []reserve = new byte[32];
				reader.read(reserve); // 保留位
				long freePos = reader.readLong40();
				long fileSize = reader.readLong40();
				if (reserve[0] > 0) {
					String writePswHash = reader.readString();
					reader.readString();
					if (writePswHash != null) {
						if (psw == null) {
							MessageManager mm = EngineMessage.get();
							throw new RQException(mm.getMessage("cellset.pswError"));
						}
						MD5 md5 = new MD5();
						boolean canWrite = md5.getMD5ofStr(psw).equals(writePswHash);
						if (!canWrite) {
							MessageManager mm = EngineMessage.get();
							throw new RQException(mm.getMessage("cellset.pswError"));
						}
					}
				}
				
				raf = new RandomAccessFile(file, "rw");
				raf.seek(0);
				raf.write(bytes);
				raf.getChannel().force(false);
				
				raf.setLength(freePos);//clear
				raf.setLength(fileSize);//恢复文件大小
				raf.close();
				
				logFile.delete();
			} catch (IOException e) {
				try {
					raf.close();
				} catch (IOException e1) {
					throw new RQException(e1.getMessage(), e1);
				}
				throw new RQException(e.getMessage(), e);
			}
		}
		
		//处理索引的恢复
		dir = file.getAbsolutePath() + "_I_TransactionLog";
		logFile = new FileObject(dir);
		if (logFile.isExists()) {
			RandomAccessFile raf = null;
			ComTable gtable = null;
			try {
				raf = new RandomAccessFile(logFile.getLocalFile().file(), "rw");
				String tableName = raf.readUTF();
				
				gtable = ComTable.createGroupTable(file);
				gtable.checkPassword(psw);
				PhyTable btable = gtable.getBaseTable();
				PhyTable table = btable;
				if (!tableName.equals(btable.getTableName())) {
					table = btable.getAnnexTable(tableName);
				}
				if (table != null)
					table.resetIndex(ctx);
				raf.close();
				logFile.delete();
			} catch (IOException e) {
				try {
					raf.close();
					gtable.close();
				} catch (IOException e1) {
					throw new RQException(e1.getMessage(), e1);
				}
				throw new RQException(e.getMessage(), e);
			}
		}
		return result;
	}
}
