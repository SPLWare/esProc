package com.scudata.expression.fn;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.FileObject;
import com.scudata.dm.HttpFile;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.expression.ParamInfo2;
import com.scudata.resources.EngineMessage;

/**
 * 将url的返回结果封装成文件流
 * httpfile(url:cs, post:cs:method, content; property:value,....)
 * httpfile(url:cs, post:cs:method)
 * httpfile(url,post,contenttype)
 * @author runqian
 *
 */
public class CreateHttpFile extends Function {
	public byte calcExpValueType(Context ctx) {
		return Expression.TYPE_FILE;
	}

	public Node optimize(Context ctx) {
		if (param != null) param.optimize(ctx);
		return this;
	}

	public Object calculate(Context ctx) {
		IParam param = this.param;
		IParam headerParam = null;
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("httpfile" + mm.getMessage("function.missingParam"));
		} else if (param.getType() == IParam.Semicolon) {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("httpfile" + mm.getMessage("function.invalidParam"));
			}
			
			headerParam = param.getSub(1);
			param = param.getSub(0);
			if (param == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("httpfile" + mm.getMessage("function.invalidParam"));
			}
		}
		
		IParam urlParam;
		IParam postParam;
		String type = null;
		
		if (param.getType() == IParam.Comma) {
			int size = param.getSubSize();
			if (size > 3) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("httpfile" + mm.getMessage("function.invalidParam"));
			}

			urlParam = param.getSub(0);
			if (urlParam == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("httpfile" + mm.getMessage("function.invalidParam"));
			}
			
			postParam = param.getSub(1);
			if (size > 2) {
				IParam typeParam = param.getSub(2);
				if (typeParam != null) {
					Object obj = typeParam.getLeafExpression().calculate(ctx);
					if (obj instanceof String) {
						type = (String)obj;
					} else if (obj != null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("httpfile" + mm.getMessage("function.paramTypeError"));
					}
				}
			}
		} else if (param.getType() == IParam.Semicolon) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("httpfile" + mm.getMessage("function.invalidParam"));
		} else {
			urlParam = param;
			postParam = null;
		}

		String pathName;
		String cs = null;
		if (urlParam.isLeaf()) {
			Object pathObj = urlParam.getLeafExpression().calculate(ctx);
			if (!(pathObj instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("httpfile" + mm.getMessage("function.paramTypeError"));
			}

			pathName = (String)pathObj;
		} else {
			if (urlParam.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("httpfile" + mm.getMessage("function.invalidParam"));
			}

			IParam sub0 = urlParam.getSub(0);
			if (sub0 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("httpfile" + mm.getMessage("function.invalidParam"));
			}

			Object pathObj = sub0.getLeafExpression().calculate(ctx);
			if (!(pathObj instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("httpfile" + mm.getMessage("function.paramTypeError"));
			}

			pathName = (String)pathObj;
			IParam sub1 = urlParam.getSub(1);
			if (sub1 != null) {
				Object csObj = sub1.getLeafExpression().calculate(ctx);
				if (!(csObj instanceof String)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("httpfile" + mm.getMessage("function.paramTypeError"));
				}

				cs = (String)csObj;
			}
		}

		final String prefix = "http://";
		/*int len = prefix.length();
		if(pathName.length() < len || 
				!pathName.substring(0, len).toLowerCase().equals(prefix)) {
			pathName = prefix + pathName;
		}*/
		if( !pathName.startsWith( "http://" ) && !pathName.startsWith( "https://" ) ) {
			pathName = prefix + pathName;
		}
		
		HttpFile file = new HttpFile(pathName);
		if (type != null) {
			file.setRequestContentType(type);
		}
		
		if (postParam != null) {
			String post = null;
			String pcs = null;
			String method = null;
			if (postParam.isLeaf()) {
				Object obj = postParam.getLeafExpression().calculate(ctx);
				if (!(obj instanceof String)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("httpfile" + mm.getMessage("function.paramTypeError"));
				}

				post = (String)obj;
			} else {
				if (postParam.getSubSize() > 3) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("httpfile" + mm.getMessage("function.invalidParam"));
				}

				IParam sub0 = postParam.getSub(0);
				if (sub0 != null) {
					Object obj = sub0.getLeafExpression().calculate(ctx);
					if (!(obj instanceof String)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("httpfile" + mm.getMessage("function.paramTypeError"));
					}
	
					post = (String)obj;
				}

				IParam sub1 = postParam.getSub(1);
				if (sub1 != null) {
					Object csObj = sub1.getLeafExpression().calculate(ctx);
					if (!(csObj instanceof String)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("httpfile" + mm.getMessage("function.paramTypeError"));
					}

					pcs = (String)csObj;
				}
				
				if( postParam.getSubSize() == 3 ) {
					IParam sub2 = postParam.getSub(2);
					if (sub2 != null) {
						Object mObj = sub2.getLeafExpression().calculate(ctx);
						if (!(mObj instanceof String)) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("httpfile" + mm.getMessage("function.paramTypeError"));
						}
						method = (String)mObj;
					}
				}
			}
			
			file.setPostParams(post, pcs, method);
		}
		
		if (headerParam != null) {
			ParamInfo2 pi2 = ParamInfo2.parse(headerParam, "httpfile", true, true);
			Expression []exps1 = pi2.getExpressions1();
			Expression []exps2 = pi2.getExpressions2();
			for (int i = 0, count = exps1.length; i < count; ++i) {
				Object obj1 = exps1[i].calculate(ctx);
				if (!(obj1 instanceof String)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("httpfile" + mm.getMessage("function.paramTypeError"));
				}
				
				Object obj2 = exps2[i].calculate(ctx);
				if (!(obj2 instanceof String)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("httpfile" + mm.getMessage("function.paramTypeError"));
				}
				
				file.addRequestHeader((String)obj1, (String)obj2);
			}
		}
		
		return new FileObject(file, pathName, cs, option);
	}
}
