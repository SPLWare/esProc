package com.scudata.lib.math;

import com.scudata.common.Logger;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

/**
 * 复共轭对组
 * 将共轭复数对重新按需求排序,要求传入的数据全部为可配对的共轭复数或实数
 * 规则：递增实部（实部为0也包含在内），带有负虚数的排在前面；
 *    若实部一样，则虚部按数字大小降序，且符合负虚数在前的规则；所有复数排序完再排实数，也是递增
 */
public class ComPair extends SequenceFunction {
    public Object calculate (Context ctx) {
        if(this.param == null){
            double[][] allConjData = ComBase.toDbl2(this.srcSequence);
            Boolean judge = ComBase.judgePair(allConjData);
            if(judge == Boolean.TRUE){
                double[][] resultDouble = ComBase.comPair(allConjData);
                Sequence result = ComBase.toSeq(resultDouble);
                return result;
            }else{
                MessageManager mm = EngineMessage.get();
                throw new RQException(mm.getMessage("The data in compair() should be paired conjugate complex number."));
            }
        }
        else if(param.isLeaf()){
            Object o = param.getLeafExpression().calculate(ctx);
            if(o instanceof Sequence){
                double[][] allConjData = ComBase.toDbl2((Sequence) o);
                Boolean judge = ComBase.judgePair(allConjData);
                if(judge == Boolean.TRUE){
                    double[][] resultDouble = ComBase.comPair(allConjData);
                    Sequence result = ComBase.toSeq(resultDouble);
                    return result;
                }
            }else{
                MessageManager mm = EngineMessage.get();
                throw new RQException(mm.getMessage("The data in compair() should be paired conjugate complex number."));
            }
        }
        MessageManager mm = EngineMessage.get();
        throw new RQException("compair" + mm.getMessage("function.invalidParam"));
    }
}
