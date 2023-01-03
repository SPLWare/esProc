package com.scudata.expression.fn.string;

import java.io.UnsupportedEncodingException;
import javax.xml.bind.DatatypeConverter;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * base64(x:cs,cs')
 * 用cs字符集取出x中的BLOB转成base64串，或将base64串x转成cs’字符集的串，字符集省略时缺省按BLOB处理
 * @author runqian
 *
 */
public class Base64 extends Function {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("base64" + mm.getMessage("function.invalidParam"));
		}
	}
	
	public Object calculate(Context ctx) {
		if (param.isLeaf()) {
			Object val = param.getLeafExpression().calculate(ctx);
			if (val instanceof String) {
				return DatatypeConverter.parseBase64Binary((String)val);
			} else if (val instanceof byte[]) {
				return DatatypeConverter.printBase64Binary((byte[])val);
			} else if (val == null) {
				return null;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("base64" + mm.getMessage("function.paramTypeError"));
			}
		} else {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("base64" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);
			if (sub0 == null || sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("base64" + mm.getMessage("function.invalidParam"));
			}
			
			Object obj = sub0.getLeafExpression().calculate(ctx);
			if (!(obj instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("base64" + mm.getMessage("function.paramTypeError"));
			}
			
			String value = (String)obj;
			obj = sub1.getLeafExpression().calculate(ctx);
			if (!(obj instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("base64" + mm.getMessage("function.paramTypeError"));
			}
			
			String cs = (String)obj;
			try {
				if (param.getType() == IParam.Colon) {
					byte []bytes = value.getBytes(cs);
					return DatatypeConverter.printBase64Binary(bytes);
				} else {
					byte []bytes = DatatypeConverter.parseBase64Binary(value);
					return new String(bytes, cs);
				}
			} catch (UnsupportedEncodingException e) {
				throw new RQException(e);
			}
		}
	}
}
