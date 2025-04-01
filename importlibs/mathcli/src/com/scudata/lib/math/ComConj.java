package com.scudata.lib.math;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

/**
 * 复共轭
 * 实部相等，虚部互为相反数
 */
public class ComConj extends SequenceFunction {
    public Object calculate(Context ctx) {
        if (this.param == null) {

            ComBase[] cdata = ComBase.toCom(this.srcSequence);
            ComBase[] comResult = ComBase.comConj(cdata);
            Sequence result = ComBase.toSeq(comResult);
            return result;
        } else if (param.isLeaf()) {
            Object o = param.getLeafExpression().calculate(ctx);
            if (o instanceof Sequence) {
                ComBase[] cdata = ComBase.createCom(this.srcSequence, (Sequence) o);
                ComBase[] comResult = ComBase.comConj(cdata);
                Sequence result = ComBase.toSeq(comResult);
                return result;
            } else {
                MessageManager mm = EngineMessage.get();
                throw new RQException("comconj" + mm.getMessage("function.paramTypeError"));
            }
        }
        MessageManager mm = EngineMessage.get();
        throw new RQException("comconj" + mm.getMessage("function.invalidParam"));

    }
}
