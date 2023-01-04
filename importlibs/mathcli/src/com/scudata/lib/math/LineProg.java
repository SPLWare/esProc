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
        //判断参数是否为空
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

        //三个参数必须同时满足条件才可计算，否则返回参数错误
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

    /// 由double[]转换为序列返回
    protected static Sequence toSequence(double[] doubles) {
        int len = doubles.length;
        Sequence seq = new Sequence(len);
        for (int i = 0; i < len; i++) {
            seq.add(doubles[i]);
        }
        return seq;
    }


    /// 由double[]转换为序列返回
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

    //仅在本算法中需要用到的方法接口先都改成了private权限
    //在以后可能会用到的方法接口，返回通用类型的依然使用了public
    //在以后可能会用到的方法接口中，返回的SimpleMatrix类型的使用了protected

    //转化为松弛型
    private SimpleMatrix getLooseMatrix(SimpleMatrix matrix){

        int row0 = matrix.numRows();
        int col0 = matrix.numCols();
        double[][] looseMatrix = new double[row0][row0+col0];

        //looseMtrix松弛形势的数组前半部分是matrix，后半部分是对角线元素
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

    //松弛形势的系数矩阵A、约束矩阵B、目标函数矩阵C组合为一个矩阵，B是一维
    private SimpleMatrix joinMatrix(SimpleMatrix A, SimpleMatrix B, SimpleMatrix C){
        int row1 = A.numRows();
        int col1 = A.numCols();
        double[][] s = new double[row1 + 1][col1 + 1];
        for (int i = 1,iLen= row1+1; i < iLen; i++) {
            for (int j = 1,jLen = col1+1; j < jLen; j++) {
                s[i][j] = A.get(i - 1, j - 1); //右下角松弛系数矩阵A
                s[i][0] = B.get(i - 1, 0);//左下角是约束条件矩阵B，B只有一列，在这里输入是一行
            }
        }
        //C只有一行，在这里输入也是一行，索引问题注意
        for (int m= 1,mLen = C.numCols()+1;m<mLen;m++){
            s[0][m] = C.get(0,m-1);    //右上角目标函数矩阵C
        }
        return new SimpleMatrix(s);
    }


    //旋转矩阵，替换替入/出变量的角色位置
    private void pivotMatrix(SimpleMatrix matrix,int povitParamRow,int pivotParamCol){//这里matrix是ABC组合矩阵
        //单独处理替出变量变量所在行，需要除以替出变量的系数matrix[povitParamRow][pivotParamCol]
        // 替出变量选择：在约束集合中，选择对当前替入变量约束最紧的第一个基本变量
        double RCValue = matrix.get(povitParamRow,pivotParamCol);
        for(int i=0,iLen = matrix.numCols();i<iLen;i++){
            matrix.set(povitParamRow,i,(matrix.get(povitParamRow,i))/RCValue);
        }

        //循环除了替出变量所在行之外的所有行
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

    //根据旋转后的矩阵，从基本变量数组中得到一组基解
    private double[] getBaseSolution(SimpleMatrix matrix, int[] baseIds){

        double[] X = new double[matrix.numCols()];//解空间，matrix的列维度
        for(int i =0,iLen = baseIds.length;i<iLen;i++){
            X[baseIds[i]]=matrix.get(i+1,0);
        }
        return X;
    }


    //构造辅助线性规划
    private SimpleMatrix bldAuxFunc(SimpleMatrix matrix, int[] baseIds){
        //辅助矩阵的最后一列存放X0的系数，初始化为-1
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
        //辅助线性函数的目标函数为z = X0

        for(int i =0;i<cols;i++) {
            lMatrix.set(0,i,0.0); //第一行的第一列到倒数第二列赋值0
        }
        lMatrix.set(0,lMatrix.numCols()-1,1); //第一行的最后一列赋值1

        //以下是初始化pivotMatrix的第二个参数的，原先的k
        //选择一个B最小的那一行的基本变量作为替出变量
        //把约束矩阵拿出来
        double[] bArray = new double[rows-1];
        for(int row=0;row<rows-1;row++){
            bArray[row] = lMatrix.get(row+1,0);
        }
        int minIndex = posMin(bArray);
        int povitParamRow = minIndex+1;

        //以下是初始化pivotMatrix的第三个参数的，原先的j
        //选择X0作为替入变量
//        int j = lMatrix.numCols()-1;
        int pivotParamCol = lMatrix.numCols()-1;

        //第一次旋转矩阵，使得所有B为正数
        pivotMatrix(lMatrix,povitParamRow,pivotParamCol);

        //维护基本变量索引数组
        // baseIds[k-1] = j;
        baseIds[povitParamRow-1] = pivotParamCol;

        //用单纯形算法求解该辅助线性规划
        lMatrix = simplex(lMatrix,baseIds);

        //如果求解后的辅助线性规划中X0仍是基本变量，需要再次旋转消去X0
        for (int i= 0,iLen = baseIds.length;i<iLen;i++){
            if (lMatrix.numCols()-1 == baseIds[i]){
                //找到矩阵第一行（目标函数）系数不为0的变量作为替入变量
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

                //在这里更新pivotmatrix的第三个参数
                pivotParamCol = auxNegIndex+1;

                //找到X0作为基本变量所在的那一行，将X0作为替出变量
                for(int ind = 0,indLen = baseIds.length;ind<indLen;ind++){
                    if(baseIds[ind] == lMatrix.numCols()-1){
                        povitParamRow=ind+1;
                        break;
                    }
                }
//                pivotMatrix(lMatrix,k,j);
                pivotMatrix(lMatrix,povitParamRow,pivotParamCol); //旋转矩阵消去基本变量X0
                baseIds[povitParamRow-1] = pivotParamCol; //维护基本变量索引数组
                break;
            }
        }

        return lMatrix; //返回辅助矩阵
    }


    //从辅助函数中恢复原问题的目标函数
    private SimpleMatrix resotrFromLaux(SimpleMatrix lMatrix, double[] z, int[] baseIds){ //z是辅助函数的目标函数系数

        //得到目标函数系数不为0的索引数组，即基本变量索引数组
        ArrayList<Integer> zIdsArrayList = new ArrayList<Integer>();

        for(int i = 0,iLen = z.length;i<iLen;i++){ //从前往后，第一个不等于0的索引拿出来即可
            if(z[i] != 0){
                zIdsArrayList.add(i-1);
            }
        }
        int[] zIds = new int[zIdsArrayList.size()];
        for(int id=0,idLen = zIds.length;id< idLen;id++){
            zIds[id] = zIdsArrayList.get(id);
        }

        //restoreMatrix是lMatrix去掉最后一列，构建了循环放进去
        SimpleMatrix restoreMatrix = new SimpleMatrix(lMatrix.numRows(),lMatrix.numCols()-1);
        for (int i=0,iLen = lMatrix.numRows();i<iLen;i++) {
            for (int j = 0,jLen = lMatrix.numCols() - 1; j < jLen; j++) {
                restoreMatrix.set(i,j,lMatrix.get(i,j));
            }
        }

        //初始化矩阵的第一行为原问题的目标函数向量
        restoreMatrix.setRow(0,0,z);//simpleBase里面该方法是将行内的连续元素分配给提供的数组，column - 数组要写入的列。startRow - 数组写入的初始列。values - 要写入矩阵行的值。

        //如果原问题的基本变量存在新基本变量数组中，说明需要替换消去
        for(int i=0,iLen =baseIds.length;i<iLen;i++){ //baseIds循环一遍
            for(int j=0,jLen = zIds.length;j< jLen;j++){  //zIds循环一遍
                if(baseIds[i] == zIds[j]){  //遍历一遍有对应相等的就需要消去
                    double[] restoreMatrixTemp = new double[restoreMatrix.numCols()];//restore_matrix[0]这一行的元素先暂存在这
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




    //单纯形算法求解线性规划
    private SimpleMatrix simplex(SimpleMatrix matrix,int[] baseIds){
        SimpleMatrix CMatrix = matrix.copy();

        double[] targetCoefVector = new double[CMatrix.numCols()-1];
        for (int i=0,iLen =targetCoefVector.length;i<iLen;i++){
            targetCoefVector[i] = CMatrix.get(0,i+1);
        }

        //如果目标系数向量里有负数，则旋转矩阵
        while (getMin(targetCoefVector)<0){
            //在目标函数向量里，选取系数为负数的第一个变量索引，作为替入变量
            int negIndex = 0;
            int j = 0; //j在循环里，所以在这定义
            for(int i=0,iLen = targetCoefVector.length;i<iLen;i++){
                if (targetCoefVector[i]<0){
                    negIndex = i;
                    j = negIndex+1;
                    break; //找到第一个小于零的元素索引就中断
                }
            }

            //在约束集合里，选取对替入变量约束最紧的约束行，那一行的基本变量作为替出变量
            ArrayList<Double> bondSetIndexVectorArr = new ArrayList<Double>(); //为了添加元素使用列表
            for(int i =1,iLen = CMatrix.numRows();i <iLen;i++){
                if (CMatrix.get(i,j)>0){
                    bondSetIndexVectorArr.add(CMatrix.get(i,0)/CMatrix.get(i,j));
                }else{
                    bondSetIndexVectorArr.add(Double.MAX_VALUE);
                }
            }
            //元素根据条件添加后需要找到最小值索引
            double[] bondSetIndexVector = new double[bondSetIndexVectorArr.size()];//ArrayList转回double[]
            for(int i= 0,iLen = bondSetIndexVectorArr.size();i<iLen;i++){
                bondSetIndexVector[i] = bondSetIndexVectorArr.get(i);
            }

            int k = posMin(bondSetIndexVector)+1;


            //说明原问题无界
            if(CMatrix.get(k,j) <= 0){
                Logger.warn("原问题无界");
                return null;
            }

            pivotMatrix(CMatrix,k,j); //旋转替换替入变量和替出变量
            baseIds[k-1] = j-1;  //维护当前基本变量索引数组
            for (int i=0,iLen = targetCoefVector.length;i<iLen;i++){
                targetCoefVector[i] = CMatrix.get(0,i+1);
            }
        }

        return CMatrix;
    }


    //单纯形算法求解步骤入口
    //构造辅助线性规划函数bldAuxFunc里面返回lMatrix和baseIds是两种类型
    public Sequence solve(Matrix A0,Matrix B0,Matrix C0){

        SimpleMatrix A = new SimpleMatrix(A0.getArray());
        SimpleMatrix B = new SimpleMatrix(B0.getArray());
        SimpleMatrix C = new SimpleMatrix(C0.getArray());

        SimpleMatrix looseMatrix = getLooseMatrix(A); //转化得到松弛矩阵
//        if(equal != null){
//            DMatrixRMaj DLooseMatrix = looseMatrix.getMatrix(); //looseMatrix的类型转为DMatrixRMaj
//            DMatrixRMaj DEqual = new DMatrixRMaj(equal); //等式的系数矩阵
//            DMatrixRMaj rr = new DMatrixRMaj(looseMatrix.numRows()+1,looseMatrix.numCols());
//            looseMatrix  = SimpleMatrix.wrap(CommonOps_DDRM.concatRowsMulti(DEqual,DLooseMatrix,rr));
//        }
        SimpleMatrix matrix = joinMatrix(looseMatrix,B,C); //得到ABC组合矩阵

        //初始化基本变量的索引数组，baseIds是在这里初始化的
        int[] baseIds = new int[B.numRows()];
        for (int i = C.numCols(),iLen = B.numRows()+C.numCols();i<iLen;i++){
            baseIds[i-C.numCols()] = i;
        }

        //约束系数矩阵有负数约束，证明没有可行解，需要辅助线性函数
        double[] bondCoefMatrix = new double[matrix.numRows()]; //先把约束系数矩阵提取出来作为array （B矩阵）
        for(int i= 0,iLen =matrix.numRows();i<iLen;i++){
            bondCoefMatrix[i] = matrix.get(i,0);
        }
        if(getMin(bondCoefMatrix)<0){
            Logger.info("构造求解辅助线性规划函数..."); //提示用户这里的操作
            //构造辅助线性规划函数并旋转求解之
            //这里就用到构造辅助线性规划函数bldAuxFunc的返回
            SimpleMatrix lMatrix = bldAuxFunc(matrix,baseIds);
            //lMatrix是在构造辅助线性规划的接口里定义的，也有返回值
            //恢复原问题的目标函数
            double[] matrix0 = new double[matrix.numCols()];
            for(int i=0,iLen = matrix0.length;i<iLen;i++){
                matrix0[i] = matrix.get(0,i);
            }
            if(lMatrix != null){
                matrix =resotrFromLaux(lMatrix,matrix0,baseIds);
            }else{
                Logger.warn("辅助线性函数的原问题没有可行解");
                return null;//直接结束
            }
        }
        //单纯形算法求解拥有基本可行解的线性规划
        SimpleMatrix retMatrix = simplex(matrix,baseIds);

        //得到当前最优基本可行解
        double[] X = getBaseSolution(retMatrix,baseIds);

//        System.out.println(matrix);//运行代码时参考，不输出

        Sequence result = new Sequence();

        if(retMatrix != null){
//            result.add(toSequence(matrix));
            result.add(toSequence(retMatrix));
            result.add(toSequence(X));
        }else{
            Logger.warn("原线性规划问题无界");
//            System.out.println("原线性规划问题无界");
//            result.add(null);
            result.add(null);
            result.add(null);
        }
        return result;
    }



    //延轴的最小值索引，python的argmin函数,没有+1
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
        return minIndex; //没有+1，各处自己添加
    }

    //一维数组最小值
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
        //以下是常规情况的例子
//        double[] equal =null;
        //系数矩阵A
        double a[][] = {{2,1},{1,1}, {0,1}};
        Matrix A = new Matrix(a);

        //约束矩阵B
        double b[][]= {{10,0},{8,0},{7,0}}; //B只有一列，但是不好放进去矩阵里，强行加了一列0
        Matrix B = new Matrix(b);

        //目标函数矩阵C
        double c[][] = {{-4,-3},{0,0}};//C只有一行，但是不好放进去矩阵里，强行加了一行0
        Matrix C = new Matrix(c);

        LineProg model = new LineProg();

        Sequence result = model.solve(A,B,C);
        SimpleMatrix solveRetMatrix = (SimpleMatrix) result.get(1);
        double[] X = (double[]) result.get(2);

        System.out.println("本次迭代最优解为："+ Arrays.toString(X));
        System.out.println("该线性规划的最优值是：" + (-solveRetMatrix.get(0,0)));



        //以下是特殊情况需要构造辅助函数的例子
//        double[] equal =null;
        //系数矩阵A
//        double a[][] = {{1,1},{1,1}};
//        Matrix A = new Matrix(a);
//
//        //约束矩阵B
//        double b[][]= {{2,0},{-1,0}}; //B只有一列，但是不好放进去矩阵里，强行加了一列0
//        Matrix B = new Matrix(b);
//
//        //目标函数矩阵C
//        double c[][] = {{1,2},{0,0}};//C只有一行，但是不好放进去矩阵里，强行加了一行0
//        Matrix C = new Matrix(c);
//
//        LineProg model = new LineProg();
//
//        Sequence result = model.solve(A,B,C);
//        SimpleMatrix solveRetMatrix = (SimpleMatrix) result.get(1);
//        double[] X = (double[]) result.get(2);
//
//        System.out.println("本次迭代最优解为："+ Arrays.toString(X));
//        System.out.println("该线性规划的最优值是：" + (-solveRetMatrix.get(0,0)));


    }

}
