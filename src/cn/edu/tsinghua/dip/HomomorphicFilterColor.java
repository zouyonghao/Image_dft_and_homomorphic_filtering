package cn.edu.tsinghua.dip;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class HomomorphicFilterColor {

    public static void main(String[] args) throws IOException {
        String imageName = "Ex_img/hom2";
        String imageFileExtension = ".png";
        BufferedImage srcimg = ImageIO.read(new File(imageName + imageFileExtension));
        BufferedImage destimg = DFT(srcimg);
        ImageIO.write(destimg, "png", new File(imageName + "_for_hfilter_color" + imageFileExtension));
        System.out.println("finished!");
    }

    public static BufferedImage DFT(BufferedImage img) throws IOException {

        int w = img.getWidth(null);
        int h = img.getHeight(null);
        int m = get2PowerEdge(w); // 获得2的整数次幂
        int n = get2PowerEdge(h);

        Complex[][] next = new Complex[m][n];

        BufferedImage destimg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        int[][] redLevel = new int[m][n];
        int[][] greenLevel = new int[m][n];
        int[][] blueLevel = new int[m][n];
        int[][] alpha = new int[m][n];

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (i < w && j < h) {
                    int pixel = img.getRGB(i, j);
                    int red = (pixel & 0x00ff0000) >> 16;
                    int green = (pixel & 0x0000ff00) >> 8;
                    int blue = (pixel & 0x000000ff);

                    alpha[i][j] = (pixel >> 24) & 0xFF;
                    redLevel[i][j] = red;
                    greenLevel[i][j] = green;
                    blueLevel[i][j] = blue;
                } else {
                    redLevel[i][j] = 0;
                    greenLevel[i][j] = 0;
                    blueLevel[i][j] = 0;
                }
            }
        }

        redLevel = homomorphicFilter(m, n, next, redLevel);
        greenLevel = homomorphicFilter(m, n, next, greenLevel);
        blueLevel = homomorphicFilter(m, n, next, blueLevel);

        int newAlpha = (-1) << 24;
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                int pixel = newAlpha | (redLevel[i][j] & 0xFF) << 16 | (greenLevel[i][j] & 0xFF) << 8 | (blueLevel[i][j] & 0xFF);
                destimg.setRGB(i, j, pixel);
            }
        }
//----------------------------------------------------------------------
        return destimg;
    }

    private static int[][] homomorphicFilter(int m, int n, Complex[][] next, int[][] level) {
        double[][] last = new double[m][n];

        // 1. 取 ln
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                last[i][j] = Math.log(level[i][j] + 1);
                if ((last[i][j] % 2) != 0) {
                    last[i][j] *= -1;
                }
            }
        }

        // 2. dft
        //---------------FFT-----------------
        Complex[] temp1 = new Complex[n];
        for (int x = 0; x < m; x++) {
            for (int y = 0; y < n; y++) {
                Complex c = new Complex(last[x][y], 0);
                temp1[y] = c;
            }
            next[x] = fft(temp1);
        }

        // 再把所有的列（已经被行的一维傅里叶变换所替代）都做一维傅里叶变换
        Complex[] temp2 = new Complex[m];
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < m; x++) {
                Complex c = next[x][y];
                temp2[x] = c;
            }
            temp2 = fft(temp2);
            for (int i = 0; i < m; i++) {
                next[i][y] = temp2[i];
            }
        }

        // 3. h filter
        // homomorphic filter
        // hom1 0.2 1.2 80*80
        // hom2
        double rL = 0.9;
        double rH = 1.5;
        double c = 1;
        int D0_2 = 80 * 80;
        double[][] hFilter = new double[m][n];
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < m; x++) {
                hFilter[x][y] = (rH - rL) * (1 - Math.exp(-c * D_2(x, y, m, n) / D0_2)) + rL;
//                System.out.println(hFilter[x][y]);
            }
        }

        // point-wise multiply
        Complex[][] g = new Complex[m][n];
        for (int x = 0; x < m; x++) {
            for (int y = 0; y < n; y++) {
                g[x][y] = next[x][y].times(hFilter[x][y]);
//				System.out.println("g: "+g[x][y].getR()+"  "+g[x][y].getI());
            }
        }

        // 4. DFT_-1
        for (int x = 0; x < m; x++) {
            for (int y = 0; y < n; y++) {
                Complex cc = new Complex(g[x][y].getR(), g[x][y].getI());
                temp1[y] = cc;
            }
            g[x] = ifft(temp1);
        }

        for (int y = 0; y < n; y++) {
            for (int x = 0; x < m; x++) {
                Complex cc = g[x][y];
                temp2[x] = cc;
            }
            temp2 = ifft(temp2);
            for (int i = 0; i < m; i++) {
                g[i][y] = temp2[i];
            }
        }

        // 5. exp
        int[][] result = new int[m][n];
        for (int x = 0; x < m; x++) {
            for (int y = 0; y < n; y++) {
                double tmp = g[x][y].getR();
//                System.out.println(tmp);
                if ((x + y) % 2 != 0) {
                    tmp *= -1;
                }
                last[x][y] = Math.exp(tmp - 1);
//                System.out.println(last[x][y]);
                result[x][y] = (int) last[x][y];
//				System.out.println(last[x][y]);
            }
        }
        return result;
    }

    private static double D_2(int x, int y, int m, int n) {
        return (x - m / 2d) * (x - m / 2d) + (y - n / 2d) * (y - n / 2d);
    }

    // 根据图像的长获得2的整数次幂
    public static int get2PowerEdge(int e) {
        if (e == 1)
            return 1;
        int cur = 1;
        while (true) {
            if (e > cur && e <= 2 * cur)
                return 2 * cur;
            else
                cur *= 2;
        }
    }

    // 快速一维傅里叶变换
    public static Complex[] fft(Complex[] x) { //传入的全都是206
        int N = x.length;
        // base case
        if (N == 1) {
            return x;
        }

        // radix 2 Cooley-Turkey FFT
        if (N % 2 != 0) {
            throw new RuntimeException("N is not a power of 2");
        }

        // fft of even terms
        Complex[] even = new Complex[N / 2];
        for (int k = 0; k < N / 2; k++) {
            even[k] = x[2 * k];
        }
        Complex[] q = fft(even);

        // fft of odd terms
        Complex[] odd = new Complex[N / 2];
        for (int k = 0; k < N / 2; k++) {
            odd[k] = x[2 * k + 1]; //DEBUG  之前这里忘记+1  差点搞死我
        }
        Complex[] r = fft(odd);

        // combine
        Complex[] y = new Complex[N];
        for (int k = 0; k < N / 2; k++) {
            double kth = -2 * k * Math.PI / N;
            Complex wk = new Complex(Math.cos(kth), Math.sin(kth)); // all small number not 0
            y[k] = q[k].plus(wk.times(r[k]));
            y[k + N / 2] = q[k].minus(wk.times(r[k]));
        }

        return y;
    }

    // 快速一维傅里叶逆变换
    public static Complex[] ifft(Complex[] x) {
        int N = x.length;
        Complex[] y = new Complex[N];

        // take conjugate
        for (int i = 0; i < N; i++) {
            y[i] = x[i].conjugate();
        }

        // compute forward fft
        y = fft(y);

        // take conguate again
        for (int i = 0; i < N; i++) {
            y[i] = y[i].conjugate();
        }

        // divide by N
        for (int i = 0; i < N; i++) {
            y[i] = y[i].times(1.0 / N);
        }

        return y;
    }
}
