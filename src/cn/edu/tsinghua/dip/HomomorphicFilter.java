package cn.edu.tsinghua.dip;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class HomomorphicFilter {

    public static void main(String[] args) throws IOException {
        String imageName = "Ex_img/ImageProcessingDemo";
        String imageFileExtension = ".png";
        BufferedImage srcimg = ImageIO.read(new File(imageName + imageFileExtension));
        BufferedImage destimg = DFT(srcimg);
        ImageIO.write(destimg, "png", new File(imageName + "_for_hfilter" + imageFileExtension));
        System.out.println("finished!");
    }

    public static BufferedImage DFT(BufferedImage img) throws IOException {

        int w = img.getWidth(null);
        int h = img.getHeight(null);
        int m = get2PowerEdge(w); // 获得2的整数次幂
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
                    if ((i + j) % 2 != 0) {
                        gray = -gray;
                    }
                    last[i][j] = gray;
                } else {
                    last[i][j] = 0;
                }
                // 1. 取 ln
                last[i][j] = Math.log(last[i][j] + 1);
            }
        }

        // 2. dft
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
        double rL = 0.2;
        double rH = 2;
        double c = 1;
        int D0_2 = 40 * 40;
        double[][] hFilter = new double[m][n];
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < m; x++) {
                hFilter[x][y] = (rH - rL) * (1 - Math.exp(-c * D_2(x, y, m, n) / D0_2)) + rL;
            }
        }

        // point-wise multiply
        Complex[][] g = new Complex[m][n];
        for (int x = 0; x < m; x++) {
            for (int y = 0; y < n; y++) {
                g[x][y] = next[x][y].times(hFilter[x][y]);
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
        for (int x = 0; x < m; x++) {
            for (int y = 0; y < n; y++) {
                last[x][y] = Math.exp(g[x][y].getR() - 1);
            }
        }

        int newalpha = (-1) << 24;
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                if ((i + j) % 2 != 0) {
                    last[i][j] = -last[i][j];
                }
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
//		if (N == 256) {
//			for (int i = 0; i < N; i++)
//			System.out.println(i+"---"+x[i].getR()+"  "+x[i].getI());
//		}
//		System.out.println(N);

        // base case
        if (N == 1) {
//			System.out.println(x[0].getR()+"  "+x[0].getI()); // !!!!ERROR
//			return new Complex[] {x[0]};
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
//			System.out.println("wk: "+N+"---"+wk.getR()+"  "+wk.getI());
//			System.out.println("q[k]: "+N+"---"+q[k].getR()+"  "+q[k].getI());
//			System.out.println("r[k]: "+N+"---"+r[k].getR()+"  "+r[k].getI());
//			System.out.println("wk.times(r[k]): "+N+"---"+wk.times(r[k]).getR()+"  "+wk.times(r[k]).getI());
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
