package com.raqsoft.expression.mfn.sequence;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.SequenceFunction;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.util.Variant;

/**
 * median
 * 		A.median(k:n)
 * 		A.median(k:n, x)
 * 取中位数函数
 * 先把数据源从小到大排序，然后把记录分成多段，取指定段的第一个记录值
 * 参数必须为两个或三个，由':'，','隔开。
 * 第二个参数为排序后的分段数
 * 第一个参数为取第几个分段的第一个记录
 * 第三个参数为取值表达式，源序列根据该表达式计算出新的序列，来执行分段取数。
 * 		A.median(k:n,x)相当于A.(x).median(k:n)
 * 例：	3：5 表示把排序后的记录分为5段，取第三段第一个记录的值。
 * 		若记录不平均，则后面的分段记录多
 * 
 * 若参数为空，则为取中位数。取值方式采用均分取值的方式。
 * 			把计算，并排序后的序列，均分为n段，返回第k段分界线上的数据
 * 
 * 仅支持sequence数据源
 * 
 * @author 于志华
 *
 */
public class Median extends SequenceFunction  {
	public Object calculate(Context ctx) {		
		if (param == null) {
			return srcSequence.median(0, 0);
		} 
		
		int value1=-1, value2=-1;
		char type = param.getType();
		if (type == IParam.Colon) {		//两个参数 A.median(k:n)
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("median" + mm.getMessage("function.invalidParam"));
			}

			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);
			
			if (null != sub0 && !sub0.isLeaf()) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("median" + mm.getMessage("function.invalidParam"));
			}
			
			if (null != sub1 && !sub1.isLeaf()) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("median" + mm.getMessage("function.invalidParam"));
			}

			try {
				if (null != sub0) {
					value1 = (int)Variant.longValue(sub0.getLeafExpression().calculate(ctx));
					// 参数一不能小于1
					if (value1 < 1) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("median" + mm.getMessage("function.invalidParam"));
					}
				}
				if (null != sub1) {
					value2 = (int)Variant.longValue(sub1.getLeafExpression().calculate(ctx));
					// 参数二必须大于1，且大于参数一。
					if (value2 < 1 || value1 > value2 ){
						MessageManager mm = EngineMessage.get();
						throw new RQException("median" + mm.getMessage("function.invalidParam"));
					}
				}
				
				// 若参数二为缺省值，参数一必须也为缺省值。
				if (-1 == value2 && -1 != value1) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("median" + mm.getMessage("function.invalidParam"));
				}
				
				if (-1 == value1)
					value1 = 0;
				if (-1 == value2)
					value2 = 0;
			} catch (Exception e) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("median" + mm.getMessage("function.invalidParam"));
			}
			
			return srcSequence.median(value1, value2);
		} else if (type == IParam.Comma ) {		// 三个参数 A.median(k:n, x)
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("median" + mm.getMessage("function.invalidParam"));
			}

			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);
			
			// 若有分隔符，x不能为空
			if ((null != sub1 && !sub1.isLeaf()) || null == sub1) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("median" + mm.getMessage("function.invalidParam"));
			}
			// 有分隔符， 就必须有分隔符:
			if (null != sub0 && sub0.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("median" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub00 = null;
			IParam sub01 = null;
			if (sub0 != null) {
				sub00 = sub0.getSub(0);
				sub01 = sub0.getSub(1);
			}
			
			if (null != sub00 && !sub00.isLeaf()) {	// k必须是叶参数
				MessageManager mm = EngineMessage.get();
				throw new RQException("median" + mm.getMessage("function.invalidParam"));
			}
			if (null != sub01 && !sub01.isLeaf()) {	// n必须是叶参数
				MessageManager mm = EngineMessage.get();
				throw new RQException("median" + mm.getMessage("function.invalidParam"));
			}

			Sequence seq = null;	// 序列经过表达式x计算后的序列
			try {
				// 对序列计算表达式x
				seq = srcSequence.calc(sub1.getLeafExpression(), ctx);
				
				if (null != sub00) {
					value1 = (int)Variant.longValue(sub00.getLeafExpression().calculate(ctx));
					// 参数一不能小于1
					if (value1 < 1) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("median" + mm.getMessage("function.invalidParam"));
					}
				}
				if (null != sub01) {
					value2 = (int)Variant.longValue(sub01.getLeafExpression().calculate(ctx));
					// 参数二必须大于1，且大于参数一。
					if (value2 < 1 || value1 > value2 ){
						MessageManager mm = EngineMessage.get();
						throw new RQException("median" + mm.getMessage("function.invalidParam"));
					}
				}
				
				// 若参数二为缺省值，参数一必须也为缺省值。
				if (-1 == value2 && -1 != value1) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("median" + mm.getMessage("function.invalidParam"));
				}
				
				if (-1 == value1)
					value1 = 0;
				if (-1 == value2)
					value2 = 0;
			} catch (Exception e) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("median" + mm.getMessage("function.invalidParam"));
			}
			
			// 取得序列值
			return seq.median(value1, value2);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("median" + mm.getMessage("function.invalidParam"));
		}
	}
}
