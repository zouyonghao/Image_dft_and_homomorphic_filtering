package cn.edu.tsinghua.dip;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomomorphicFilterColorWithNaiveDFT {

    private static final ExecutorService THREAD_POOL_EXECUTOR = Executors.newFixedThreadPool(12);

    public static void main(String[] args) throws IOException {
        String imageName = "Ex_img/hom1";
        String imageFileExtension = ".png";
        BufferedImage srcimg = ImageIO.read(new File(imageName + imageFileExtension));
        BufferedImage destimg = DFT(srcimg);
        ImageIO.write(destimg, "png", new File(imageName + "_for_hfilter_naive" + imageFileExtension));
        System.out.println("finished!");
        THREAD_POOL_EXECUTOR.shutdown();
    }

    public static BufferedImage DFT(BufferedImage img) {

        int m = img.getWidth(null);
        int n = img.getHeight(null);

        BufferedImage destimg = new BufferedImage(m, n, BufferedImage.TYPE_INT_ARGB);

        int[][] redLevel = new int[m][n];
        int[][] greenLevel = new int[m][n];
        int[][] blueLevel = new int[m][n];
        int[][] alpha = new int[m][n];

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                int pixel = img.getRGB(i, j);
                int red = (pixel & 0x00ff0000) >> 16;
                int green = (pixel & 0x0000ff00) >> 8; // 250 = 0xfa
                int blue = (pixel & 0x000000ff); // 255 = 0xff

                alpha[i][j] = (pixel >> 24) & 0xFF;
                redLevel[i][j] = red;
                greenLevel[i][j] = green;
                blueLevel[i][j] = blue;
            }
        }

        redLevel = homomorphicFilter(m, n, redLevel);
        greenLevel = homomorphicFilter(m, n, greenLevel);
        blueLevel = homomorphicFilter(m, n, blueLevel);

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                int pixel = (alpha[i][j] & 0xFF) << 24 | (redLevel[i][j] & 0xFF) << 16 | (greenLevel[i][j] & 0xFF) << 8 | (blueLevel[i][j] & 0xFF);
                destimg.setRGB(i, j, pixel);
            }
        }
        return destimg;
    }

    private static int[][] homomorphicFilter(int m, int n, int[][] level) {
        double[][] last = new double[m][n];

        Complex[][] next = new Complex[m][n];

        // 1. 取 ln
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                last[i][j] = Math.log(level[i][j] + 1);
            }
        }

        // 2. dft
        CountDownLatch latch = new CountDownLatch(m * n);
        for (int u = 0; u < m; u++) {
            for (int v = 0; v < n; v++) {
                int finalU = u;
                int finalV = v;
                THREAD_POOL_EXECUTOR.submit(() -> {
                    next[finalU][finalV] = DFT(last, finalU, finalV);
                    latch.countDown();
                });
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

        // 3. h filter
        // homomorphic filter
        // hom1 0.2 1.2 80*80
        // hom2
        double rL = 0.5;
        double rH = 1.5;
        double c = 1;
        int D0_2 = 80 * 80;
        double[][] hFilter = new double[m][n];
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < m; x++) {
                hFilter[x][y] = (rH - rL) * (1 - Math.exp(-c * D_2(x, y, m, n) / D0_2)) + rL;
            }
        }

        Complex[][] g = new Complex[m][n];
        for (int x = 0; x < m; x++) {
            for (int y = 0; y < n; y++) {
                g[x][y] = next[x][y].times(hFilter[x][y]);
            }
        }

        // 4. DFT_-1
        final CountDownLatch latch2 = new CountDownLatch(m * n);
        final Complex[][] iDFTResult = new Complex[m][n];
        for (int u = 0; u < m; u++) {
            for (int v = 0; v < n; v++) {
                int finalU = u;
                int finalV = v;
                THREAD_POOL_EXECUTOR.submit(() -> {
                    iDFTResult[finalU][finalV] = IDFT(g, finalU, finalV);
                    latch2.countDown();
                });
            }
        }

        try {
            while (latch2.getCount() > 0) {
                System.out.println(latch2.getCount());
                Thread.sleep(100);
            }
            latch2.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 5. exp
        int[][] result = new int[m][n];
        for (int x = 0; x < m; x++) {
            for (int y = 0; y < n; y++) {
                last[x][y] = Math.exp(iDFTResult[x][y].getR() - 1);
                result[x][y] = (int) last[x][y];
            }
        }
        return result;
    }

    private static double D_2(int x, int y, int m, int n) {
        return (x - m / 2d) * (x - m / 2d) + (y - n / 2d) * (y - n / 2d);
    }


    public static Complex DFT(double[][] f, int u, int v) {
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

    public static Complex IDFT(Complex[][] f, int u, int v) {
        int M = f.length;
        int N = f[0].length;
        double u_M = (double) u / (double) M;
        double v_N = (double) v / (double) N;
        int MN = M * N;
        double MN_1 = 1.0 / MN;
        Complex c = new Complex(0, 0);
        for (int x = 0; x < M; x++) {
            for (int y = 0; y < N; y++) {
                Complex temp = new Complex(0, 2 * Math.PI * (x * u_M + y * v_N));
                c = c.plus(temp.exp().times(f[x][y]));
            }
        }
        return c.times(MN_1);
    }
}
