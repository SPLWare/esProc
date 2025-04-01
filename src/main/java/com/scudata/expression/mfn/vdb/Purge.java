package com.scudata.expression.mfn.vdb;

import java.io.File;
import java.io.IOException;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.VSFunction;
import com.scudata.resources.EngineMessage;
import com.scudata.vdb.Library;

/**
 * 整理数据库数据，使访问速度更快
 * v.purge()
 * @author RunQian
 *
 */
public class Purge extends VSFunction {
	public Object calculate(Context ctx) {
		if (param != null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("purge" + mm.getMessage("function.invalidParam"));
		}
		
		Library library = vs.getVDB().getLibrary();
		File file = new File(library.getPathName());
		
		try {
			File tmpFile = File.createTempFile("tmpdata", "", file.getParentFile());
			boolean result = library.reset(tmpFile.getAbsolutePath());
			library.stop();
			
			if (result) {
				if (file.delete()) {
					tmpFile.renameTo(file);
				} else {
					tmpFile.delete();
					MessageManager mm = EngineMessage.get();
					throw new RQException("purge" + mm.getMessage("file.deleteFailed"));
				}
			} else {
				tmpFile.delete();
			}
			
			return result;
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		}
	}
}
