package com.scudata.expression.fn;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * 取出序列/序表中出现次数最多的成员。
 * @author runqian
 *
 */
public class Mode extends Function {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("mode" + mm.getMessage("function.missingParam"));
		}
	}

	public Object calculate(Context ctx) {
		IParam param = this.param;
		if (param.isLeaf()) {
			return param.getLeafExpression().calculate(ctx);
		} else {
			int size = param.getSubSize();
			HashMap<Object, Integer> map = new HashMap<Object, Integer>(size);
			for (int i = 0; i < size; ++i) {
				IParam sub = param.getSub(i);
				if (sub != null) {
					Object obj = sub.getLeafExpression().calculate(ctx);
					if (obj != null) {
						Integer n = map.get(obj);
						if (n == null) {
							map.put(obj, 1);
						} else {
							map.put(obj, n + 1);
						}
					}
				}
			}

			Object result = null;
			int count = 0;
			Set<Map.Entry<Object, Integer>> entrySet = map.entrySet();
			Iterator<Map.Entry<Object, Integer>> itr = entrySet.iterator();
			while (itr.hasNext()) {
				Map.Entry<Object, Integer> entry = itr.next();
				if (entry.getValue() > count) {
					result = entry.getKey();
					count = entry.getValue();
				}
			}
			
			return result;
		}
	}
}
