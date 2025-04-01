package com.scudata.lib.math;

import com.scudata.common.Logger;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.fn.algebra.Matrix;
import com.scudata.resources.EngineMessage;
import org.ejml.simple.SimpleMatrix;

import java.util.ArrayList;
import java.util.Arrays;

public class LineProg extends Function {
    public Object calculate(Context ctx) {
        //鍒ゆ柇鍙傛暟鏄惁涓虹┖
        if (param == null) {
            MessageManager mm = EngineMessage.get();
            throw new RQException("LineProg" + mm.getMessage("function.missingParam"));
        }

        Object o1 = null;
        Object o2 = null;
        Object o3 = null;

        if (param.isLeaf()) {
            MessageManager mm = EngineMessage.get();
            throw new RQException("LineProg" + mm.getMessage("function.invalidParam"));
        } else {
            if (param.getSubSize() != 3) {
                MessageManager mm = EngineMessage.get();
                throw new RQException("LineProg" + mm.getMessage("function.invalidParam"));
            }
            IParam sub1 = param.getSub(0);
            IParam sub2 = param.getSub(1);
            IParam sub3 = param.getSub(2);


            if (sub1 == null || sub2 == null || sub3 == null) {
                MessageManager mm = EngineMessage.get();
                throw new RQException("LineProg" + mm.getMessage("function.invalidParam"));
            }

            o1 = sub1.getLeafExpression().calculate(ctx); //A
            o2 = sub2.getLeafExpression().calculate(ctx); //B
            o3 = sub3.getLeafExpression().calculate(ctx); //C
        }

        //涓変釜鍙傛暟蹇呴』鍚屾椂婊¤冻鏉′欢鎵嶅彲璁＄畻锛屽惁鍒欒繑鍥炲弬鏁伴敊璇�
        if (o1 instanceof Sequence && o2 instanceof Sequence && o3 instanceof Sequence) {
            Matrix A = new Matrix((Sequence) o1);
            Matrix B = new Matrix((Sequence) o2);
            Matrix C = new Matrix((Sequence) o3);

            Sequence solveResult = solve(A, B, C);
            Sequence result = new Sequence(2);
            result.add(solveResult.get(1));
            result.add(solveResult.get(2));
            return result;
        }else {
            Logger.warn("The data in LineProg() needs to be matrix");
            MessageManager mm = EngineMessage.get();
            throw new RQException("LineProg" + mm.getMessage("function.paramTypeError"));
        }

    }

    /// 鐢眃ouble[]杞崲涓哄簭鍒楄繑鍥�
    protected static Sequence toSequence(double[] doubles) {
        int len = doubles.length;
        Sequence seq = new Sequence(len);
        for (int i = 0; i < len; i++) {
            seq.add(doubles[i]);
        }
        return seq;
    }


    /// 鐢眃ouble[]杞崲涓哄簭鍒楄繑鍥�
    protected static Sequence toSequence(SimpleMatrix smatrix) {
        int rows = smatrix.numRows();
        int cols = smatrix.numCols();
        Sequence seq = new Sequence(rows);
        for (int r = 0; r < rows; r++) {
            Sequence sub = new Sequence(cols);
            for (int c = 0; c < cols; c++) {
                sub.add(smatrix.get(r, c));
            }
            seq.add(sub);
        }
        return seq;
    }

    //浠呭湪鏈畻娉曚腑闇�瑕佺敤鍒扮殑鏂规硶鎺ュ彛鍏堥兘鏀规垚浜唒rivate鏉冮檺
    //鍦ㄤ互鍚庡彲鑳戒細鐢ㄥ埌鐨勬柟娉曟帴鍙ｏ紝杩斿洖閫氱敤绫诲瀷鐨勪緷鐒朵娇鐢ㄤ簡public
    //鍦ㄤ互鍚庡彲鑳戒細鐢ㄥ埌鐨勬柟娉曟帴鍙ｄ腑锛岃繑鍥炵殑SimpleMatrix绫诲瀷鐨勪娇鐢ㄤ簡protected

    //杞寲涓烘澗寮涘瀷
    private SimpleMatrix getLooseMatrix(SimpleMatrix matrix){

        int row0 = matrix.numRows();
        int col0 = matrix.numCols();
        double[][] looseMatrix = new double[row0][row0+col0];

        //looseMtrix鏉惧紱褰㈠娍鐨勬暟缁勫墠鍗婇儴鍒嗘槸matrix锛屽悗鍗婇儴鍒嗘槸瀵硅绾垮厓绱�
        for(int i = 0;i<row0;i++) {
            for (int j = 0; j < col0; j++) {
                looseMatrix[i][j] = matrix.get(i, j);
            }
        }

        for (int i = 0;i<row0;i++){
            looseMatrix[i][col0+i] = 1.0;
        }

        return new SimpleMatrix(looseMatrix);
    }

    //鏉惧紱褰㈠娍鐨勭郴鏁扮煩闃礎銆佺害鏉熺煩闃礏銆佺洰鏍囧嚱鏁扮煩闃礐缁勫悎涓轰竴涓煩闃碉紝B鏄竴缁�
    private SimpleMatrix joinMatrix(SimpleMatrix A, SimpleMatrix B, SimpleMatrix C){
        int row1 = A.numRows();
        int col1 = A.numCols();
        double[][] s = new double[row1 + 1][col1 + 1];
        for (int i = 1,iLen= row1+1; i < iLen; i++) {
            for (int j = 1,jLen = col1+1; j < jLen; j++) {
                s[i][j] = A.get(i - 1, j - 1); //鍙充笅瑙掓澗寮涚郴鏁扮煩闃礎
                s[i][0] = B.get(i - 1, 0);//宸︿笅瑙掓槸绾︽潫鏉′欢鐭╅樀B锛孊鍙湁涓�鍒楋紝鍦ㄨ繖閲岃緭鍏ユ槸涓�琛�
            }
        }
        //C鍙湁涓�琛岋紝鍦ㄨ繖閲岃緭鍏ヤ篃鏄竴琛岋紝绱㈠紩闂娉ㄦ剰
        for (int m= 1,mLen = C.numCols()+1;m<mLen;m++){
            s[0][m] = C.get(0,m-1);    //鍙充笂瑙掔洰鏍囧嚱鏁扮煩闃礐
        }
        return new SimpleMatrix(s);
    }


    //鏃嬭浆鐭╅樀锛屾浛鎹㈡浛鍏�/鍑哄彉閲忕殑瑙掕壊浣嶇疆
    private void pivotMatrix(SimpleMatrix matrix,int povitParamRow,int pivotParamCol){//杩欓噷matrix鏄疉BC缁勫悎鐭╅樀
        //鍗曠嫭澶勭悊鏇垮嚭鍙橀噺鍙橀噺鎵�鍦ㄨ锛岄渶瑕侀櫎浠ユ浛鍑哄彉閲忕殑绯绘暟matrix[povitParamRow][pivotParamCol]
        // 鏇垮嚭鍙橀噺閫夋嫨锛氬湪绾︽潫闆嗗悎涓紝閫夋嫨瀵瑰綋鍓嶆浛鍏ュ彉閲忕害鏉熸渶绱х殑绗竴涓熀鏈彉閲�
        double RCValue = matrix.get(povitParamRow,pivotParamCol);
        for(int i=0,iLen = matrix.numCols();i<iLen;i++){
            matrix.set(povitParamRow,i,(matrix.get(povitParamRow,i))/RCValue);
        }

        //寰幆闄や簡鏇垮嚭鍙橀噺鎵�鍦ㄨ涔嬪鐨勬墍鏈夎
        for(int i=0,iLen = matrix.numRows();i<iLen;i++){

            if (i != povitParamRow){
                double[] ijValue = new double[matrix.numCols()];
                for(int col=0,colLen = matrix.numCols();col<colLen;col++){
                    ijValue[col] = matrix.get(povitParamRow,col) * matrix.get(i,pivotParamCol);
                }
                for (int col=0;col<matrix.numCols();col++){
                    matrix.set(i,col,matrix.get(i,col)-ijValue[col]);
                }
            }
        }
    }

    //鏍规嵁鏃嬭浆鍚庣殑鐭╅樀锛屼粠鍩烘湰鍙橀噺鏁扮粍涓緱鍒颁竴缁勫熀瑙�
    private double[] getBaseSolution(SimpleMatrix matrix, int[] baseIds){

        double[] X = new double[matrix.numCols()];//瑙ｇ┖闂达紝matrix鐨勫垪缁村害
        for(int i =0,iLen = baseIds.length;i<iLen;i++){
            X[baseIds[i]]=matrix.get(i+1,0);
        }
        return X;
    }


    //鏋勯�犺緟鍔╃嚎鎬ц鍒�
    private SimpleMatrix bldAuxFunc(SimpleMatrix matrix, int[] baseIds){
        //杈呭姪鐭╅樀鐨勬渶鍚庝竴鍒楀瓨鏀綳0鐨勭郴鏁帮紝鍒濆鍖栦负-1
        SimpleMatrix AMatrix = matrix.copy();
        int rows = AMatrix.numRows();
        int cols = AMatrix.numCols();
        double[][] lMatrixDouble = new double[rows][cols+1];
        for(int row = 0;row<rows;row++){
            for(int col = 0;col<cols;col++){
                lMatrixDouble[row][col] = AMatrix.get(row,col);
            }
            lMatrixDouble[row][cols] = -1;
        }
        SimpleMatrix lMatrix = new SimpleMatrix(lMatrixDouble);
        //杈呭姪绾挎�у嚱鏁扮殑鐩爣鍑芥暟涓簔 = X0

        for(int i =0;i<cols;i++) {
            lMatrix.set(0,i,0.0); //绗竴琛岀殑绗竴鍒楀埌鍊掓暟绗簩鍒楄祴鍊�0
        }
        lMatrix.set(0,lMatrix.numCols()-1,1); //绗竴琛岀殑鏈�鍚庝竴鍒楄祴鍊�1

        //浠ヤ笅鏄垵濮嬪寲pivotMatrix鐨勭浜屼釜鍙傛暟鐨勶紝鍘熷厛鐨刱
        //閫夋嫨涓�涓狟鏈�灏忕殑閭ｄ竴琛岀殑鍩烘湰鍙橀噺浣滀负鏇垮嚭鍙橀噺
        //鎶婄害鏉熺煩闃垫嬁鍑烘潵
        double[] bArray = new double[rows-1];
        for(int row=0;row<rows-1;row++){
            bArray[row] = lMatrix.get(row+1,0);
        }
        int minIndex = posMin(bArray);
        int povitParamRow = minIndex+1;

        //浠ヤ笅鏄垵濮嬪寲pivotMatrix鐨勭涓変釜鍙傛暟鐨勶紝鍘熷厛鐨刯
        //閫夋嫨X0浣滀负鏇垮叆鍙橀噺
//        int j = lMatrix.numCols()-1;
        int pivotParamCol = lMatrix.numCols()-1;

        //绗竴娆℃棆杞煩闃碉紝浣垮緱鎵�鏈塀涓烘鏁�
        pivotMatrix(lMatrix,povitParamRow,pivotParamCol);

        //缁存姢鍩烘湰鍙橀噺绱㈠紩鏁扮粍
        // baseIds[k-1] = j;
        baseIds[povitParamRow-1] = pivotParamCol;

        //鐢ㄥ崟绾舰绠楁硶姹傝В璇ヨ緟鍔╃嚎鎬ц鍒�
        lMatrix = simplex(lMatrix,baseIds);

        //濡傛灉姹傝В鍚庣殑杈呭姪绾挎�ц鍒掍腑X0浠嶆槸鍩烘湰鍙橀噺锛岄渶瑕佸啀娆℃棆杞秷鍘籜0
        for (int i= 0,iLen = baseIds.length;i<iLen;i++){
            if (lMatrix.numCols()-1 == baseIds[i]){
                //鎵惧埌鐭╅樀绗竴琛岋紙鐩爣鍑芥暟锛夌郴鏁颁笉涓�0鐨勫彉閲忎綔涓烘浛鍏ュ彉閲�
                double[] auxTargetCoefVector = new double[lMatrix.numCols()-1];
                for (int m=0,mLen = auxTargetCoefVector.length;m<mLen;m++){
                    auxTargetCoefVector[m] = lMatrix.get(0,m+1);
                }

                int auxNegIndex = 0;
                for(int m=0,mLen = auxTargetCoefVector.length;m<mLen;m++){
                    if (auxTargetCoefVector[m] != 0){
                        auxNegIndex = m;
                        break;
                    }
                }

                //鍦ㄨ繖閲屾洿鏂皃ivotmatrix鐨勭涓変釜鍙傛暟
                pivotParamCol = auxNegIndex+1;

                //鎵惧埌X0浣滀负鍩烘湰鍙橀噺鎵�鍦ㄧ殑閭ｄ竴琛岋紝灏哫0浣滀负鏇垮嚭鍙橀噺
                for(int ind = 0,indLen = baseIds.length;ind<indLen;ind++){
                    if(baseIds[ind] == lMatrix.numCols()-1){
                        povitParamRow=ind+1;
                        break;
                    }
                }
//                pivotMatrix(lMatrix,k,j);
                pivotMatrix(lMatrix,povitParamRow,pivotParamCol); //鏃嬭浆鐭╅樀娑堝幓鍩烘湰鍙橀噺X0
                baseIds[povitParamRow-1] = pivotParamCol; //缁存姢鍩烘湰鍙橀噺绱㈠紩鏁扮粍
                break;
            }
        }

        return lMatrix; //杩斿洖杈呭姪鐭╅樀
    }


    //浠庤緟鍔╁嚱鏁颁腑鎭㈠鍘熼棶棰樼殑鐩爣鍑芥暟
    private SimpleMatrix resotrFromLaux(SimpleMatrix lMatrix, double[] z, int[] baseIds){ //z鏄緟鍔╁嚱鏁扮殑鐩爣鍑芥暟绯绘暟

        //寰楀埌鐩爣鍑芥暟绯绘暟涓嶄负0鐨勭储寮曟暟缁勶紝鍗冲熀鏈彉閲忕储寮曟暟缁�
        ArrayList<Integer> zIdsArrayList = new ArrayList<Integer>();

        for(int i = 0,iLen = z.length;i<iLen;i++){ //浠庡墠寰�鍚庯紝绗竴涓笉绛変簬0鐨勭储寮曟嬁鍑烘潵鍗冲彲
            if(z[i] != 0){
                zIdsArrayList.add(i-1);
            }
        }
        int[] zIds = new int[zIdsArrayList.size()];
        for(int id=0,idLen = zIds.length;id< idLen;id++){
            zIds[id] = zIdsArrayList.get(id);
        }

        //restoreMatrix鏄痩Matrix鍘绘帀鏈�鍚庝竴鍒楋紝鏋勫缓浜嗗惊鐜斁杩涘幓
        SimpleMatrix restoreMatrix = new SimpleMatrix(lMatrix.numRows(),lMatrix.numCols()-1);
        for (int i=0,iLen = lMatrix.numRows();i<iLen;i++) {
            for (int j = 0,jLen = lMatrix.numCols() - 1; j < jLen; j++) {
                restoreMatrix.set(i,j,lMatrix.get(i,j));
            }
        }

        //鍒濆鍖栫煩闃电殑绗竴琛屼负鍘熼棶棰樼殑鐩爣鍑芥暟鍚戦噺
        restoreMatrix.setRow(0,0,z);//simpleBase閲岄潰璇ユ柟娉曟槸灏嗚鍐呯殑杩炵画鍏冪礌鍒嗛厤缁欐彁渚涚殑鏁扮粍锛宑olumn - 鏁扮粍瑕佸啓鍏ョ殑鍒椼�俿tartRow - 鏁扮粍鍐欏叆鐨勫垵濮嬪垪銆倂alues - 瑕佸啓鍏ョ煩闃佃鐨勫�笺��

        //濡傛灉鍘熼棶棰樼殑鍩烘湰鍙橀噺瀛樺湪鏂板熀鏈彉閲忔暟缁勪腑锛岃鏄庨渶瑕佹浛鎹㈡秷鍘�
        for(int i=0,iLen =baseIds.length;i<iLen;i++){ //baseIds寰幆涓�閬�
            for(int j=0,jLen = zIds.length;j< jLen;j++){  //zIds寰幆涓�閬�
                if(baseIds[i] == zIds[j]){  //閬嶅巻涓�閬嶆湁瀵瑰簲鐩哥瓑鐨勫氨闇�瑕佹秷鍘�
                    double[] restoreMatrixTemp = new double[restoreMatrix.numCols()];//restore_matrix[0]杩欎竴琛岀殑鍏冪礌鍏堟殏瀛樺湪杩�
                    for(int k=0,kLen =restoreMatrix.numCols();k<kLen;k++){
                        restoreMatrixTemp[k] = restoreMatrix.get(0,baseIds[i]+1)*restoreMatrix.get(i+1,k);
                    }
                    for(int k=0,kLen = restoreMatrix.numCols();k<kLen;k++){
                        restoreMatrix.set(0,k,restoreMatrix.get(0,k)-restoreMatrixTemp[k]);
                    }
                    break;
                }
            }
        }
        return restoreMatrix;
    }




    //鍗曠函褰㈢畻娉曟眰瑙ｇ嚎鎬ц鍒�
    private SimpleMatrix simplex(SimpleMatrix matrix,int[] baseIds){
        SimpleMatrix CMatrix = matrix.copy();

        double[] targetCoefVector = new double[CMatrix.numCols()-1];
        for (int i=0,iLen =targetCoefVector.length;i<iLen;i++){
            targetCoefVector[i] = CMatrix.get(0,i+1);
        }

        //濡傛灉鐩爣绯绘暟鍚戦噺閲屾湁璐熸暟锛屽垯鏃嬭浆鐭╅樀
        while (getMin(targetCoefVector)<0){
            //鍦ㄧ洰鏍囧嚱鏁板悜閲忛噷锛岄�夊彇绯绘暟涓鸿礋鏁扮殑绗竴涓彉閲忕储寮曪紝浣滀负鏇垮叆鍙橀噺
            int negIndex = 0;
            int j = 0; //j鍦ㄥ惊鐜噷锛屾墍浠ュ湪杩欏畾涔�
            for(int i=0,iLen = targetCoefVector.length;i<iLen;i++){
                if (targetCoefVector[i]<0){
                    negIndex = i;
                    j = negIndex+1;
                    break; //鎵惧埌绗竴涓皬浜庨浂鐨勫厓绱犵储寮曞氨涓柇
                }
            }

            //鍦ㄧ害鏉熼泦鍚堥噷锛岄�夊彇瀵规浛鍏ュ彉閲忕害鏉熸渶绱х殑绾︽潫琛岋紝閭ｄ竴琛岀殑鍩烘湰鍙橀噺浣滀负鏇垮嚭鍙橀噺
            ArrayList<Double> bondSetIndexVectorArr = new ArrayList<Double>(); //涓轰簡娣诲姞鍏冪礌浣跨敤鍒楄〃
            for(int i =1,iLen = CMatrix.numRows();i <iLen;i++){
                if (CMatrix.get(i,j)>0){
                    bondSetIndexVectorArr.add(CMatrix.get(i,0)/CMatrix.get(i,j));
                }else{
                    bondSetIndexVectorArr.add(Double.MAX_VALUE);
                }
            }
            //鍏冪礌鏍规嵁鏉′欢娣诲姞鍚庨渶瑕佹壘鍒版渶灏忓�肩储寮�
            double[] bondSetIndexVector = new double[bondSetIndexVectorArr.size()];//ArrayList杞洖double[]
            for(int i= 0,iLen = bondSetIndexVectorArr.size();i<iLen;i++){
                bondSetIndexVector[i] = bondSetIndexVectorArr.get(i);
            }

            int k = posMin(bondSetIndexVector)+1;


            //璇存槑鍘熼棶棰樻棤鐣�
            if(CMatrix.get(k,j) <= 0){
                Logger.warn("鍘熼棶棰樻棤鐣�");
                return null;
            }

            pivotMatrix(CMatrix,k,j); //鏃嬭浆鏇挎崲鏇垮叆鍙橀噺鍜屾浛鍑哄彉閲�
            baseIds[k-1] = j-1;  //缁存姢褰撳墠鍩烘湰鍙橀噺绱㈠紩鏁扮粍
            for (int i=0,iLen = targetCoefVector.length;i<iLen;i++){
                targetCoefVector[i] = CMatrix.get(0,i+1);
            }
        }

        return CMatrix;
    }


    //鍗曠函褰㈢畻娉曟眰瑙ｆ楠ゅ叆鍙�
    //鏋勯�犺緟鍔╃嚎鎬ц鍒掑嚱鏁癰ldAuxFunc閲岄潰杩斿洖lMatrix鍜宐aseIds鏄袱绉嶇被鍨�
    public Sequence solve(Matrix A0,Matrix B0,Matrix C0){

        SimpleMatrix A = new SimpleMatrix(A0.getArray());
        SimpleMatrix B = new SimpleMatrix(B0.getArray());
        SimpleMatrix C = new SimpleMatrix(C0.getArray());

        SimpleMatrix looseMatrix = getLooseMatrix(A); //杞寲寰楀埌鏉惧紱鐭╅樀
//        if(equal != null){
//            DMatrixRMaj DLooseMatrix = looseMatrix.getMatrix(); //looseMatrix鐨勭被鍨嬭浆涓篋MatrixRMaj
//            DMatrixRMaj DEqual = new DMatrixRMaj(equal); //绛夊紡鐨勭郴鏁扮煩闃�
//            DMatrixRMaj rr = new DMatrixRMaj(looseMatrix.numRows()+1,looseMatrix.numCols());
//            looseMatrix  = SimpleMatrix.wrap(CommonOps_DDRM.concatRowsMulti(DEqual,DLooseMatrix,rr));
//        }
        SimpleMatrix matrix = joinMatrix(looseMatrix,B,C); //寰楀埌ABC缁勫悎鐭╅樀

        //鍒濆鍖栧熀鏈彉閲忕殑绱㈠紩鏁扮粍锛宐aseIds鏄湪杩欓噷鍒濆鍖栫殑
        int[] baseIds = new int[B.numRows()];
        for (int i = C.numCols(),iLen = B.numRows()+C.numCols();i<iLen;i++){
            baseIds[i-C.numCols()] = i;
        }

        //绾︽潫绯绘暟鐭╅樀鏈夎礋鏁扮害鏉燂紝璇佹槑娌℃湁鍙瑙ｏ紝闇�瑕佽緟鍔╃嚎鎬у嚱鏁�
        double[] bondCoefMatrix = new double[matrix.numRows()]; //鍏堟妸绾︽潫绯绘暟鐭╅樀鎻愬彇鍑烘潵浣滀负array 锛圔鐭╅樀锛�
        for(int i= 0,iLen =matrix.numRows();i<iLen;i++){
            bondCoefMatrix[i] = matrix.get(i,0);
        }
        if(getMin(bondCoefMatrix)<0){
            Logger.info("鏋勯�犳眰瑙ｈ緟鍔╃嚎鎬ц鍒掑嚱鏁�..."); //鎻愮ず鐢ㄦ埛杩欓噷鐨勬搷浣�
            //鏋勯�犺緟鍔╃嚎鎬ц鍒掑嚱鏁板苟鏃嬭浆姹傝В涔�
            //杩欓噷灏辩敤鍒版瀯閫犺緟鍔╃嚎鎬ц鍒掑嚱鏁癰ldAuxFunc鐨勮繑鍥�
            SimpleMatrix lMatrix = bldAuxFunc(matrix,baseIds);
            //lMatrix鏄湪鏋勯�犺緟鍔╃嚎鎬ц鍒掔殑鎺ュ彛閲屽畾涔夌殑锛屼篃鏈夎繑鍥炲��
            //鎭㈠鍘熼棶棰樼殑鐩爣鍑芥暟
            double[] matrix0 = new double[matrix.numCols()];
            for(int i=0,iLen = matrix0.length;i<iLen;i++){
                matrix0[i] = matrix.get(0,i);
            }
            if(lMatrix != null){
                matrix =resotrFromLaux(lMatrix,matrix0,baseIds);
            }else{
                Logger.warn("杈呭姪绾挎�у嚱鏁扮殑鍘熼棶棰樻病鏈夊彲琛岃В");
                return null;//鐩存帴缁撴潫
            }
        }
        //鍗曠函褰㈢畻娉曟眰瑙ｆ嫢鏈夊熀鏈彲琛岃В鐨勭嚎鎬ц鍒�
        SimpleMatrix retMatrix = simplex(matrix,baseIds);

        //寰楀埌褰撳墠鏈�浼樺熀鏈彲琛岃В
        double[] X = getBaseSolution(retMatrix,baseIds);

//        System.out.println(matrix);//杩愯浠ｇ爜鏃跺弬鑰冿紝涓嶈緭鍑�

        Sequence result = new Sequence();

        if(retMatrix != null){
//            result.add(toSequence(matrix));
            result.add(toSequence(retMatrix));
            result.add(toSequence(X));
        }else{
            Logger.warn("鍘熺嚎鎬ц鍒掗棶棰樻棤鐣�");
//            System.out.println("鍘熺嚎鎬ц鍒掗棶棰樻棤鐣�");
//            result.add(null);
            result.add(null);
            result.add(null);
        }
        return result;
    }



    //寤惰酱鐨勬渶灏忓�肩储寮曪紝python鐨刟rgmin鍑芥暟,娌℃湁+1
    public int posMin(double[] array) {
        int minIndex = 0;
        double minValue = Double.MAX_VALUE;
        for (int j = 0; j < array.length; j++) {
            double current_value = array[j];
            if (current_value < minValue) {
                minValue = current_value;
                minIndex = j;
            }
        }
        return minIndex; //娌℃湁+1锛屽悇澶勮嚜宸辨坊鍔�
    }

    //涓�缁存暟缁勬渶灏忓��
    public double getMin(double[] array){
        double minV = Double.MAX_VALUE;
        for(int i=0,iLen = array.length;i<iLen;i++){
            double currentV = array[i];
            if(currentV < minV){
                minV = currentV;
            }
        }
        return minV;
    }

    public static void main(String[] args){
        //浠ヤ笅鏄父瑙勬儏鍐电殑渚嬪瓙
//        double[] equal =null;
        //绯绘暟鐭╅樀A
        double a[][] = {{2,1},{1,1}, {0,1}};
        Matrix A = new Matrix(a);

        //绾︽潫鐭╅樀B
        double b[][]= {{10,0},{8,0},{7,0}}; //B鍙湁涓�鍒楋紝浣嗘槸涓嶅ソ鏀捐繘鍘荤煩闃甸噷锛屽己琛屽姞浜嗕竴鍒�0
        Matrix B = new Matrix(b);

        //鐩爣鍑芥暟鐭╅樀C
        double c[][] = {{-4,-3},{0,0}};//C鍙湁涓�琛岋紝浣嗘槸涓嶅ソ鏀捐繘鍘荤煩闃甸噷锛屽己琛屽姞浜嗕竴琛�0
        Matrix C = new Matrix(c);

        LineProg model = new LineProg();

        Sequence result = model.solve(A,B,C);
        SimpleMatrix solveRetMatrix = (SimpleMatrix) result.get(1);
        double[] X = (double[]) result.get(2);

        System.out.println("鏈杩唬鏈�浼樿В涓猴細"+ Arrays.toString(X));
        System.out.println("璇ョ嚎鎬ц鍒掔殑鏈�浼樺�兼槸锛�" + (-solveRetMatrix.get(0,0)));



        //浠ヤ笅鏄壒娈婃儏鍐甸渶瑕佹瀯閫犺緟鍔╁嚱鏁扮殑渚嬪瓙
//        double[] equal =null;
        //绯绘暟鐭╅樀A
//        double a[][] = {{1,1},{1,1}};
//        Matrix A = new Matrix(a);
//
//        //绾︽潫鐭╅樀B
//        double b[][]= {{2,0},{-1,0}}; //B鍙湁涓�鍒楋紝浣嗘槸涓嶅ソ鏀捐繘鍘荤煩闃甸噷锛屽己琛屽姞浜嗕竴鍒�0
//        Matrix B = new Matrix(b);
//
//        //鐩爣鍑芥暟鐭╅樀C
//        double c[][] = {{1,2},{0,0}};//C鍙湁涓�琛岋紝浣嗘槸涓嶅ソ鏀捐繘鍘荤煩闃甸噷锛屽己琛屽姞浜嗕竴琛�0
//        Matrix C = new Matrix(c);
//
//        LineProg model = new LineProg();
//
//        Sequence result = model.solve(A,B,C);
//        SimpleMatrix solveRetMatrix = (SimpleMatrix) result.get(1);
//        double[] X = (double[]) result.get(2);
//
//        System.out.println("鏈杩唬鏈�浼樿В涓猴細"+ Arrays.toString(X));
//        System.out.println("璇ョ嚎鎬ц鍒掔殑鏈�浼樺�兼槸锛�" + (-solveRetMatrix.get(0,0)));


    }

}
