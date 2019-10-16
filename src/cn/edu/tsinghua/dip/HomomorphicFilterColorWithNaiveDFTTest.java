package cn.edu.tsinghua.dip;

import static org.junit.Assert.*;

public class HomomorphicFilterColorWithNaiveDFTTest {

    @org.junit.Test
    public void DFT() {
        double[][] test = new double[2][2];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                test[i][j] = i * j + 1;
                System.out.print(test[i][j] + "\t");
            }
            System.out.println("\n");
        }
        Complex[][] result = new Complex[2][2];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                result[i][j] = HomomorphicFilterColorWithNaiveDFT.DFT(test, i, j);
            }
        }

        Complex[][] iDFTResult = new Complex[2][2];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                iDFTResult[i][j] = HomomorphicFilterColorWithNaiveDFT.IDFT(result, i, j);
                System.out.print(iDFTResult[i][j].getR() + "\t");
            }
            System.out.println("\n");
        }

    }

    @org.junit.Test
    public void IDFT() {
    }
}