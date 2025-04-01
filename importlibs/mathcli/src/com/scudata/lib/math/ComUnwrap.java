package com.scudata.lib.math;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * 平移相位角
 * Q = unwrap(P) 展开向量 P 中的弧度相位角。每当连续相位角之间的跳跃大于或等于 π 弧度时，
 * unwrap 就会通过增加 ±2π 的整数倍来平移相位角，直到跳跃小于 π。
 * 如果 P 是矩阵，unwrap 将按列运算。
 * 如果 P 是多维数组，unwrap 将对大小大于 1 的第一个维度进行运算。
 */
public class ComUnwrap extends Function {

    public Object calculate (Context ctx) {
        if (param == null) {
            MessageManager mm = EngineMessage.get();
            throw new RQException("comunwrap" + mm.getMessage("function.missingParam"));
        } else if (param.isLeaf()) {
            Object o = param.getLeafExpression().calculate(ctx);
            if (o instanceof Sequence) {
                double[] angleResult = ComBase.toDbl((Sequence) o);
                Sequence result = ComBase.comUnwrap(angleResult);
                return result;
            }
        }  else{
            if (param.getSubSize() == 1) {
                IParam sub1 = param.getSub(0);
                if (sub1 == null) {
                    MessageManager mm = EngineMessage.get();
                    throw new RQException("comunwrap" + mm.getMessage("function.invalidParam"));
                }
                Object o1 = sub1.getLeafExpression().calculate(ctx);
                if (o1 instanceof Sequence) {
                    double[] angleResult = ComBase.toDbl((Sequence) o1);
                    Sequence result = ComBase.comUnwrap(angleResult);
                    return result;
                } else {
                    MessageManager mm = EngineMessage.get();
                    throw new RQException("comunwrap" + mm.getMessage("function.paramTypeError"));
                }
            }
            else if (param.getSubSize() == 3) {
                IParam sub1 = param.getSub(0);
                IParam sub2 = param.getSub(1);
                IParam sub3 = param.getSub(2);
                if (sub1 == null || sub2 == null || sub3 == null) {
                    MessageManager mm = EngineMessage.get();
                    throw new RQException("comunwrap" + mm.getMessage("function.invalidParam"));
                }
                Object o1 = sub1.getLeafExpression().calculate(ctx);
                Object o2 = sub2.getLeafExpression().calculate(ctx);
                Object o3 = sub3.getLeafExpression().calculate(ctx);
                if (o1 instanceof Sequence && o2 instanceof Number && o3 instanceof Number) {
                    double[][] angleResultArr = ComBase.toDbl2((Sequence) o1);
                    double tol = ((Number) o2).doubleValue();
                    int dim = ((Number) o3).intValue();
                    Sequence result = ComBase.comUnwrap(angleResultArr, tol, dim);
                    return result;
                } else {
                    MessageManager mm = EngineMessage.get();
                    throw new RQException("comunwrap" + mm.getMessage("function.paramTypeError"));
                }
            }
        }
        MessageManager mm = EngineMessage.get();
        throw new RQException("comunwrap" + mm.getMessage("function.invalidParam"));
    }

}
