package com.scudata.expression.fn;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;
import com.scudata.util.HttpUpload;

// httpupload(url:cs,ai:vi,...;fi:fni,...)
public class Http_Upload extends Function {
	private static HttpUpload createHttpUploader(IParam param, Context ctx) {
		if (param.isLeaf()) {
			Object url = param.getLeafExpression().calculate(ctx);
			if (!(url instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("httpupload" + mm.getMessage("function.paramTypeError"));
			}
			
			return new HttpUpload((String)url);
		} else if (param.getSubSize() == 2) {
			IParam urlParam = param.getSub(0);
			IParam csParam = param.getSub(1);
			if (urlParam == null || csParam == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("httpupload" + mm.getMessage("function.invalidParam"));
			}
			
			Object url = urlParam.getLeafExpression().calculate(ctx);
			if (!(url instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("httpupload" + mm.getMessage("function.paramTypeError"));
			}
			
			Object cs = csParam.getLeafExpression().calculate(ctx);
			if (!(cs instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("httpupload" + mm.getMessage("function.paramTypeError"));
			}
			
			HttpUpload upload = new HttpUpload((String)url);
			upload.setResultEncoding((String)cs);
			return upload;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("httpupload" + mm.getMessage("function.invalidParam"));
		}
	}
	
	private static void addFile(HttpUpload uploader, IParam param, Context ctx) {
		IParam nameParam = param.getSub(0);
		IParam fileParam = param.getSub(1);
		if (nameParam == null || fileParam == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("httpupload" + mm.getMessage("function.invalidParam"));
		}
		
		Object name = nameParam.getLeafExpression().calculate(ctx);
		if (!(name instanceof String)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("httpupload" + mm.getMessage("function.paramTypeError"));
		}
		
		Object file = fileParam.getLeafExpression().calculate(ctx);
		if (file instanceof String) {
			uploader.addFile((String)name, (String)file);
		} /*else if (file instanceof FileObject) {
			FileObject fo = (FileObject)file;
			try {
				bytes = (byte[])fo.read(0, -1, "b");
			} catch (IOException e) {
				throw new RQException(e);
			}
		} */else if (file instanceof byte[]) {
			uploader.addFile((String)name, (byte[])file);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("httpupload" + mm.getMessage("function.paramTypeError"));
		}
	}
	
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("httpupload" + mm.getMessage("function.missingParam"));
		} else if (param.getType() != IParam.Semicolon || param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("httpupload" + mm.getMessage("function.invalidParam"));
		}
		
		IParam leftParam = param.getSub(0);
		HttpUpload uploader;
		if (leftParam == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("httpupload" + mm.getMessage("function.invalidParam"));
		} else if (leftParam.getType() == IParam.Comma) {
			IParam sub = leftParam.getSub(0);
			if (sub == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("httpupload" + mm.getMessage("function.invalidParam"));
			}
			
			uploader = createHttpUploader(sub, ctx);
			for (int i = 1, size = leftParam.getSubSize(); i < size; ++i) {
				sub = leftParam.getSub(i);
				if (sub == null || sub.getSubSize() != 2) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("httpupload" + mm.getMessage("function.invalidParam"));
				}
				
				IParam argParam = sub.getSub(0);
				IParam valParam = sub.getSub(1);
				if (argParam == null || valParam == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("httpupload" + mm.getMessage("function.invalidParam"));
				}
				
				Object arg = argParam.getLeafExpression().calculate(ctx);
				if (!(arg instanceof String)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("httpupload" + mm.getMessage("function.paramTypeError"));
				}
				
				Object val = valParam.getLeafExpression().calculate(ctx);
				if (!(val instanceof String)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("httpupload" + mm.getMessage("function.paramTypeError"));
				}
				
				uploader.addParam((String)arg, (String)val);
			}
		} else {
			uploader = createHttpUploader(leftParam, ctx);
		}
		
		IParam rightParam = param.getSub(1);
		if (rightParam == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("httpupload" + mm.getMessage("function.invalidParam"));
		} else if (rightParam.getType() == IParam.Comma) {
			for (int i = 0, size = rightParam.getSubSize(); i < size; ++i) {
				IParam sub = rightParam.getSub(i);
				if (sub == null || sub.getSubSize() != 2) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("httpupload" + mm.getMessage("function.invalidParam"));
				}
				
				addFile(uploader, sub, ctx);
			}
		} else {
			if (rightParam.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("httpupload" + mm.getMessage("function.invalidParam"));
			}
			
			addFile(uploader, rightParam, ctx);
		}
		
		return uploader.upload();
	}
}
