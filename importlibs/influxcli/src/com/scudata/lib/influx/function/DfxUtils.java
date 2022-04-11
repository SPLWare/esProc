package com.scudata.lib.influx.function;

import java.io.InputStream;

import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.common.Logger;
import com.scudata.dm.Context;
import com.scudata.dm.Env;
import com.scudata.dm.FileObject;
import com.scudata.dm.JobSpace;
import com.scudata.dm.JobSpaceManager;
import com.scudata.util.CellSetUtil;
import com.scudata.util.Variant;

public class DfxUtils {

	
	public static String execDfxScript(String script, Context ctx) throws Exception {
		return execDfxScript(script, ctx, true);
	}
	public static String execDfxScript(String script, Context ctx, boolean closeJobSpace) throws Exception {
		String jsId = "jsId" + System.currentTimeMillis();
		JobSpace space = JobSpaceManager.getSpace(jsId);
		try{
			PgmCellSet pcs = CellSetUtil.toPgmCellSet(script);
			pcs.setContext(ctx);
			ctx.setJobSpace(space);
			Object o = pcs.execute();
			ctx.setParamValue("_returnValue_", o);
		}catch(Exception e){
			Logger.warn(e);
		}finally{
			if (closeJobSpace) JobSpaceManager.closeSpace(jsId);
		}
		return jsId;
	}
	
	public static String execDfxFile(String file, Context ctx) throws Exception {
		return execDfxFile(file, ctx, true);
	}
	public static String execDfxFile(String file, Context ctx, boolean closeJobSpace) throws Exception {
		String jsId = "jsId" + System.currentTimeMillis();
		JobSpace space = JobSpaceManager.getSpace(jsId);
		try{
			FileObject fo = new FileObject(file, Env.getDefaultCharsetName(), "s", ctx);
			PgmCellSet pcs = fo.readPgmCellSet();
			pcs.setContext(ctx);
			ctx.setJobSpace(space);
			Object o = pcs.execute();
			ctx.setParamValue("_returnValue_", o);
		}catch(Exception e){
			Logger.warn(e);
			throw e;
		}finally{
			if (closeJobSpace) JobSpaceManager.closeSpace(jsId);
		}
		return jsId;
	}

	
	public static String execDfxFile(InputStream file, Context ctx) throws Exception {
		return execDfxFile(file, ctx, true);
	}
	public static String execDfxFile(InputStream file, Context ctx, boolean closeJobSpace) throws Exception {
		String jsId = "jsId" + System.currentTimeMillis();
		JobSpace space = JobSpaceManager.getSpace(jsId);
		try{
			PgmCellSet pcs = CellSetUtil.readPgmCellSet(file);
			
			pcs.setContext(ctx);
			ctx.setJobSpace(space);
			Object o = pcs.execute();
			ctx.setParamValue("_returnValue_", o);
		}catch(Exception e){
			Logger.warn(e);
			throw e;
		}finally{
			if (closeJobSpace) JobSpaceManager.closeSpace(jsId);
		}
		return jsId;
	}

	public static String toString(Object o) {
		if (o == null) return null;
		if (o instanceof java.sql.Time 
				|| o instanceof java.sql.Timestamp 
				|| o instanceof java.sql.Date 
				|| o instanceof String 
				|| o instanceof java.util.Date) {
			return "\""+Variant.toString(o)+"\"";
		} else return Variant.toString(o);
	}
}
