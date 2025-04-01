package com.scudata.lib.math;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

/**
 *相位角angle
 *复数数组z的每个元素返回区间[-pi，pi]中的相位角；theta 中的角度表示为 z = abs(z).*exp(i*theta)。
 *简单理解：设复数为A+Bi，那么相位就是arctan(B/A)。
 *如果 z = x + iy 的元素是非负实数，则 angle 返回 0。如果 z 的元素是负实数，则 angle 返回 π。
 */

public class ComAngle extends SequenceFunction {
    public Object calculate(Context ctx) {
        if (this.param == null) {
            ComBase[] cdata = ComBase.toCom(this.srcSequence);
            int len = cdata.length;
            double[] resultDouble = new double[len];
            for (int i = 0; i < len; i++) {
                ComBase co = cdata[i];
                resultDouble[i] = co.comAngle();
            }
            return ComBase.toSeq(resultDouble);
        } else if (param.isLeaf()) {
            Object o = param.getLeafExpression().calculate(ctx);
            if (o instanceof Sequence) {
                ComBase[] cdata = ComBase.createCom(this.srcSequence, (Sequence) o);
                int len = cdata.length;
                double[] resultDouble = new double[len];
                for (int i = 0; i < len; i++) {
                    ComBase co = cdata[i];
                    resultDouble[i] = co.comAngle();
                }
                return ComBase.toSeq(resultDouble);
            } else {
                MessageManager mm = EngineMessage.get();
                throw new RQException("comangle" + mm.getMessage("function.paramTypeError"));
            }
        }
        MessageManager mm = EngineMessage.get();
        throw new RQException("comangle" + mm.getMessage("function.invalidParam"));
    }
}
