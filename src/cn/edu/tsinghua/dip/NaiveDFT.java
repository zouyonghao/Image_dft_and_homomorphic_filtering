package cn.edu.tsinghua.dip;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NaiveDFT {

    private static final ExecutorService THREAD_POOL_EXECUTOR = Executors.newFixedThreadPool(12);

    public static void main(String[] args) throws IOException {
        String imageName = args[0];
        String imageFileExtension = ".png";
        BufferedImage srcimg = ImageIO.read(new File(imageName + imageFileExtension));
        BufferedImage destimg = DFT(srcimg);
        ImageIO.write(destimg, "png", new File(imageName + "_processed" + imageFileExtension));
        System.out.println("finished!");
        THREAD_POOL_EXECUTOR.shutdown();
    }

    public static BufferedImage DFT(BufferedImage img) throws IOException {
        int m = img.getWidth(null);
        int n = img.getHeight(null);
        int[][] last = new int[m][n];
        Complex[][] next = new Complex[m][n];
        int pixel, gray;

        BufferedImage destImg;
        //  乘以（-1）^(x+y)
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                pixel = img.getRGB(i, j);
                int red = pixel & 0x00ff0000 >> 16;
                int green = (pixel & 0x0000ff00) >> 8;
                int blue = (pixel & 0x000000ff);
                gray = (red + green + blue) / 3;
                if ((i + j) % 2 != 0) {
                    gray = -gray;
                }
                last[i][j] = gray;
            }
        }

        CountDownLatch latch = new CountDownLatch(m * n);
        for (int u = 0; u < m; u++) {
            for (int v = 0; v < n; v++) {
                int finalU = u;
                int finalV = v;
                THREAD_POOL_EXECUTOR.submit(() -> {
                    next[finalU][finalV] = DFT(last, finalU, finalV);
                    latch.countDown();
                });
                // System.out.println("U: " + u + "---v: " + v);
            }
        }

        try {
            while (latch.getCount() > 0) {
                System.out.println(latch.getCount());
                Thread.sleep(100);
            }
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        destImg = showFourierImage(next);
        return destImg;

    }

    // 返回傅里叶频谱图
    private static BufferedImage showFourierImage(Complex[][] f) {
        int w = f.length;
        int h = f[0].length;
        double max = 0;
        double min = 0;
        BufferedImage destimg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        // 取模
        double[][] abs = new double[w][h];
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                abs[i][j] = f[i][j].abs();
//				System.out.println(f[i][j].getR()+"  "+f[i][j].getI());
            }
        }
        // 取log + 1
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                abs[i][j] = Math.log(abs[i][j] + 1);
            }
        }
        // 量化
        max = abs[0][0];
        min = abs[0][0];
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                if (abs[i][j] > max)
                    max = abs[i][j];
                if (abs[i][j] < min)
                    min = abs[i][j];
            }
        }
        int level = 255;
        double interval = (max - min) / level;
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                for (int k = 0; k <= level; k++) {
                    if (abs[i][j] >= k * interval && abs[i][j] < (k + 1) * interval) {
                        abs[i][j] = (k * interval / (max - min)) * level;
                        break;
                    }
                }
            }
        }

        int newalpha = (-1) << 24;
        int newred;
        int newblue;
        int newgreen;
        int newrgb;
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                newred = (int) abs[i][j] << 16;
                newgreen = (int) abs[i][j] << 8;
                newblue = (int) abs[i][j];
                newrgb = newalpha | newred | newgreen | newblue;
                destimg.setRGB(i, j, newrgb);
            }
        }
        return destimg;
    }

    private static Complex DFT(int[][] f, int u, int v) {
        int M = f.length;
        int N = f[0].length;
        Complex c = new Complex(0, 0);
        double u_M = (double) u / (double) M;
        double v_N = (double) v / (double) N;
        for (int x = 0; x < M; x++) {
            for (int y = 0; y < N; y++) {
                Complex temp = new Complex(0, -2 * Math.PI * (x * u_M + y * v_N));
                c = c.plus(temp.exp().times(f[x][y]));
            }
        }
        return c;
    }

    public static Complex IDFT(int[][] f, int u, int v) {
        int M = f.length;
        int N = f[0].length;
        double u_M = (double) u / (double) M;
        double v_N = (double) v / (double) N;
        int MN = M*N;
        double MN_1 = 1.0/MN;
        Complex c = new Complex(0, 0);
        for (int x = 0; x < M; x++) {
            for (int y = 0; y < N; y++) {
                Complex temp = new Complex(0, 2 * Math.PI * (x * u_M + y * v_N));
                c = c.plus(temp.exp().times(f[x][y])).times(MN_1);
            }
        }
        return c;
    }
}
