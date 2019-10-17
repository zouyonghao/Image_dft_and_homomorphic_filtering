package cn.edu.tsinghua.dip;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class HomomorphicFilter {

    public static void main(String[] args) throws IOException {
        String imageName = "Ex_img/hom2";
        String imageFileExtension = ".png";
        BufferedImage srcimg = ImageIO.read(new File(imageName + imageFileExtension));
        int d0 = 100;
//        for (double rL = 0; rL < 1; rL += 0.1) {
//            for (double rH = 0; rH < 1; rH += 0.1) {
//                BufferedImage destimg = DFT(srcimg, rL, rH, d0);
//                ImageIO.write(destimg, "png", new File(imageName + "_for_hfilter"
//                        + "_" + rL + "_" + rH + "_" + d0 + imageFileExtension));
//            }
//        }
        double rL = 0.2;
        double rH = 0.5;
        BufferedImage destimg = DFT(srcimg, rL, rH, d0);
        ImageIO.write(destimg, "png", new File(imageName + "_for_hfilter"
                + "_" + rL + "_" + rH + "_" + d0 + imageFileExtension));
        System.out.println("finished!");
    }

    public static BufferedImage DFT(BufferedImage img, double rL, double rH, int d0) throws IOException {

        int w = img.getWidth(null);
        int h = img.getHeight(null);
        // 获得2的整数次幂
        int m = get2PowerEdge(w);
        int n = get2PowerEdge(h);
        double[][] last = new double[m][n];
        Complex[][] next = new Complex[m][n];
        int pixel, newred, newgreen, newblue, newrgb;

        BufferedImage destimg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        int[][] alpha = new int[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (i < w && j < h) {
                    pixel = img.getRGB(i, j);
                    int red = (pixel & 0x00ff0000) >> 16;
                    int green = (pixel & 0x0000ff00) >> 8;
                    int blue = (pixel & 0x000000ff);
                    int gray = (red + green + blue) / 3;
//                    int gray = red;
                    alpha[i][j] = (pixel & 0xff000000) >> 24;
                    last[i][j] = gray;
                } else {
                    last[i][j] = 0;
                }
                // 1. 取 ln
                last[i][j] = Math.log(last[i][j] + 1);
                if ((i + j) % 2 != 0) {
                    last[i][j] *= -1;
                }
            }
        }

        // 2. dft
        // 先算行
        for (int x = 0; x < m; x++) {
            Complex[] temp = new Complex[n];
            for (int y = 0; y < n; y++) {
                Complex c = new Complex(last[x][y], 0);
                temp[y] = c;
            }
            next[x] = fft(temp);
        }

        // 再算列
        for (int y = 0; y < n; y++) {
            Complex[] temp = new Complex[m];
            for (int x = 0; x < m; x++) {
                Complex c = next[x][y];
                temp[x] = c;
            }
            temp = fft(temp);
            for (int i = 0; i < m; i++) {
                next[i][y] = temp[i];
            }
        }

        // 3. h filter
        // homomorphic filter
        // hom1
        // hom2
//        double rL = 0.1;
//        double rH = 0.8;
        double c = 1;
        double D0_2 = d0 * d0;
        double[][] hFilter = new double[m][n];
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < m; x++) {
                hFilter[x][y] = (rH - rL) * (1d - Math.exp(-c * D_2(x, y, m, n) / D0_2)) + rL;
            }
        }

        Complex[][] g = new Complex[m][n];
        for (int x = 0; x < m; x++) {
            for (int y = 0; y < n; y++) {
                g[x][y] = next[x][y].times(hFilter[x][y]);
            }
        }

        // 4. DFT_-1
        for (int x = 0; x < m; x++) {
            Complex[] temp = new Complex[m];
            for (int y = 0; y < n; y++) {
                Complex cc = new Complex(g[x][y].getR(), g[x][y].getI());
                temp[y] = cc;
            }
            g[x] = ifft(temp);
        }

        for (int y = 0; y < n; y++) {
            Complex[] temp = new Complex[m];
            for (int x = 0; x < m; x++) {
                Complex cc = g[x][y];
                temp[x] = cc;
            }
            temp = ifft(temp);
            for (int i = 0; i < m; i++) {
                g[i][y] = temp[i];
            }
        }

        // 5. exp
        for (int x = 0; x < m; x++) {
            for (int y = 0; y < n; y++) {
                double temp = g[x][y].getR();
                if ((x + y) % 2 != 0) {
                    temp *= -1;
                }
                last[x][y] = Math.exp(temp) - 1;
            }
        }

        // 6. clip
        double max = last[0][0];
        double min = last[0][0];
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                if (last[i][j] > max)
                    max = last[i][j];
                if (last[i][j] < min)
                    min = last[i][j];
            }
        }
        int level = 255;
        double range = max - min;
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                last[i][j] = (last[i][j] - min) / range * level;
            }
        }

        int newalpha = (-1) << 24;
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                newred = (int) last[i][j];
                newblue = newred;
                newgreen = newred << 8;
                newred = newred << 16;
                newrgb = newalpha | newred | newgreen | newblue;
                destimg.setRGB(i, j, newrgb);
            }
        }
        return destimg;
    }

    private static double D_2(int x, int y, int m, int n) {
        return (x - m / 2) * (x - m / 2) + (y - n / 2) * (y - n / 2);
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
    public static Complex[] fft(Complex[] x) {
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
