package com.scudata.expression.mfn.string;

import java.nio.charset.Charset;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.common.Types;
import com.scudata.dm.Context;
import com.scudata.dm.FileObject;
import com.scudata.dm.MemoryFile;
import com.scudata.dm.cursor.FileCursor;
import com.scudata.dm.cursor.ICursor;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.ParamInfo3;
import com.scudata.expression.StringFunction;
import com.scudata.resources.EngineMessage;

/**
 * 把二维表字符串导成序表
 * S.import(F:type:fmt,…; s)
 * @author RunQian
 *
 */
public class Import extends StringFunction {
	public Object calculate(Context ctx) {
		final String charset = Charset.defaultCharset().name();
		byte []bytes = srcStr.getBytes();
		MemoryFile mf = new MemoryFile(new byte[][]{bytes});
		FileObject fo = new FileObject(mf, "", charset, null);
		ICursor cursor = createStringCursor(fo, param, option, ctx);
		return cursor.fetch();
	}

	private static ICursor createStringCursor(FileObject fo, IParam param, String option, Context ctx) {
		IParam fieldParam = param;
		IParam sParam = null;
		
		if (param != null && param.getType() == IParam.Semicolon) {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("import" + mm.getMessage("function.invalidParam"));
			}

			fieldParam = param.getSub(0);
			sParam = param.getSub(1);
		}

		String []fields = null;
		byte []types = null;
		String []fmts = null;
		int segSeq = 1;
		int segCount = 1;
		String s = null;

		if (fieldParam != null) {
			ParamInfo3 pi = ParamInfo3.parse(fieldParam, "cursor", true, false, false);
			fields = pi.getExpressionStrs1();
			String []typeNames = pi.getExpressionStrs2();
			
			int fcount = fields.length;
			Expression []exps = pi.getExpressions3();

			for (int i = 0; i < fcount; ++i) {
				String type = typeNames[i];
				if (type == null) continue;
				if (types == null) types = new byte[fcount];

				if (type.equals("string")) {
					types[i] = Types.DT_STRING;
				} else if (type.equals("int")) {
					types[i] = Types.DT_INT;
				} else if (type.equals("float")) {
					types[i] = Types.DT_DOUBLE;
				} else if (type.equals("long")) {
					types[i] = Types.DT_LONG;
				} else if (type.equals("decimal")) {
					types[i] = Types.DT_DECIMAL;
				} else if (type.equals("date")) {
					types[i] = Types.DT_DATE;
				} else if (type.equals("datetime")) {
					types[i] = Types.DT_DATETIME;
				} else if (type.equals("time")) {
					types[i] = Types.DT_TIME;
				} else if (type.equals("bool")) {
					types[i] = Types.DT_BOOLEAN;
				} else {
					try {
						int len = Integer.parseInt(type);
						if (len < 1) {
							MessageManager mm = EngineMessage.get();
							throw new RQException(type + mm.getMessage("engine.unknownType"));
						}
						
						types[i] = Types.DT_SERIALBYTES;
						if (fmts == null) {
							fmts = new String[fcount];
						}
						
						fmts[i] = type;
					} catch (NumberFormatException e) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(type + mm.getMessage("engine.unknownType"));
					}
				}
				
				if (exps[i] != null) {
					Object obj = exps[i].calculate(ctx);
					if (!(obj instanceof String)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("import" + mm.getMessage("function.paramTypeError"));
					}
					
					if (fmts == null) {
						fmts = new String[fcount];
					}
					
					fmts[i] = (String)obj;
				}
			}
		}
				
		if (sParam != null) {
			Object obj = sParam.getLeafExpression().calculate(ctx);
			if (obj != null && !(obj instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("import" + mm.getMessage("function.paramTypeError"));
			}

			s = (String)obj;
		}
		
		FileCursor cursor = new FileCursor(fo, segSeq, segCount, fields, types, s, option, ctx);
		cursor.setFormats(fmts);
		return cursor;
	}
}
