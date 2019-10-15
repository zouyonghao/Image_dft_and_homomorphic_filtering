package cn.edu.tsinghua.dip;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        String imageName = "Ex_img/ImageProcessingDemo";
        String imageFileExtension = ".png";
        BufferedImage srcimg = ImageIO.read(new File(imageName + imageFileExtension));
        BufferedImage destimg = DFT(srcimg);
        ImageIO.write(destimg, "png", new File(imageName + "_for_compare" + imageFileExtension));
        System.out.println("finished!");
    }

    public static BufferedImage DFT(BufferedImage img) throws IOException {

        int w = img.getWidth(null);
        int h = img.getHeight(null);
        int m = get2PowerEdge(w); // 获得2的整数次幂
//		System.out.println(m);
        int n = get2PowerEdge(h);
//		System.out.println(n);
        int[][] last = new int[m][n];
        Complex[][] next = new Complex[m][n];
        int pixel, alpha = -1, newred, newgreen, newblue, newrgb;

        BufferedImage destimg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
//----------------------------------------------------------------------
        //  first: Image Padding and move it to center 填充图像至2的整数次幂并乘以（-1）^(x+y)
        //  use 2-D array last to store
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (i < w && j < h) {
                    pixel = img.getRGB(i, j);
                    if ((i + j) % 2 == 0) {
                        newred = pixel & 0x00ff0000 >> 16;
                    } else {
                        newred = -(pixel & 0x00ff0000 >> 16);
                    }
                    last[i][j] = newred;
                } else {
                    last[i][j] = 0;
                }
            }
        }

//----------------------------------------------------------------------
        // second: Fourier Transform 离散傅里叶变换
        // u-width v-height x-width y-height

        //------------Normal-DFT-------------
//			for (int u = 0; u < m; u++){
//				for (int v = 0; v <n; v++) {
//					next[u][v] = DFT(last, u, v);
//					System.out.println("U: "+u+"---v: "+v);
//				}
//			}
//			if (true) { // 生成DFT图片,记得修改图片大小
//			destimg = showFourierImage(next);
//			return destimg;
//		}

        //---------------FFT-----------------
        // 先把所有的行都做一维傅里叶变换，再放回去
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

		if (true) { // 生成DFT图片,记得修改图片大小
			destimg = showFourierImage(next);
			return destimg;
		}

//----------------------------------------------------------------------
        // third: Generate the frequency filter and filter the image in frequency domain 生成频率域滤波器并滤波

        // 构造原始滤波函数
        Complex[][] filter = new Complex[m][n];
        //这个是11X11均值滤波
//		for (int x = 0; x < m; x++) {
//			for (int y = 0; y < n; y++) {
//				if (x < 11 && y < 11) {
//					if ((x+y)%2==0)
//						filter[x][y] = new Complex(1/121d, 0); // double 后面赋值数字记得加d！！！！！！！
//					else
//						filter[x][y] = new Complex(-1/121d, 0);
//				}
//				else {
//					filter[x][y] = new Complex(0, 0);
//				}
//			}
//		}

        //下面这个是拉普拉斯滤波
        filter[0][0] = new Complex(0, 0);
        filter[0][1] = new Complex(-1, 0);
        filter[0][2] = new Complex(0, 0);
        filter[1][0] = new Complex(-1, 0);
        filter[1][1] = new Complex(4, 0);
        filter[1][2] = new Complex(-1, 0);
        filter[2][0] = new Complex(0, 0);
        filter[2][1] = new Complex(-1, 0);
        filter[2][2] = new Complex(0, 0);
        for (int x = 0; x < m; x++) {
            for (int y = 0; y < n; y++) {
                if (x < 3 && y < 3) {/*上面已经写好了*/} else {
                    filter[x][y] = new Complex(0, 0);
                }
            }
        }

        // 傅里叶变换 转换为频率域
        for (int x = 0; x < m; x++) {
            for (int y = 0; y < n; y++) {
                Complex c = new Complex(filter[x][y].getR(), filter[x][y].getI());
                temp1[y] = c;
            }
            filter[x] = fft(temp1);
        }

        for (int y = 0; y < n; y++) {
            for (int x = 0; x < m; x++) {
                Complex c = new Complex(filter[x][y].getR(), filter[x][y].getI());
//				Complex c = filter[x][y];
                temp2[x] = c;
            }
            temp2 = fft(temp2);
            for (int i = 0; i < m; i++) {
                filter[i][y] = temp2[i];
            }
        }

//		if (true) {
//			destimg = showFourierImage(filter);
//			return destimg;
//		}

        // point-wise multiply
        Complex[][] g = new Complex[m][n];
        for (int x = 0; x < m; x++) {
            for (int y = 0; y < n; y++) {
                g[x][y] = filter[x][y].times(next[x][y]);
//				System.out.println("g: "+g[x][y].getR()+"  "+g[x][y].getI());
            }
        }
//----------------------------------------------------------------------
        // fourth: use IDFT to get the image 傅里叶逆变换
        for (int x = 0; x < m; x++) {
            for (int y = 0; y < n; y++) {
                Complex c = new Complex(g[x][y].getR(), g[x][y].getI());
                temp1[y] = c;
            }
            g[x] = ifft(temp1);
        }

//		for (int x = 0; x < m; x++) {
//			for (int y = 0; y < n; y++) {
//				System.out.println("gifft-g: "+g[x][y].getR()+"  "+g[x][y].getI());
//			}
//		}

        for (int y = 0; y < n; y++) {
            for (int x = 0; x < m; x++) {
                Complex c = g[x][y];
                temp2[x] = c;
            }
            temp2 = ifft(temp2);
            for (int i = 0; i < m; i++) {
                g[i][y] = temp2[i];
            }
        }

        for (int x = 0; x < m; x++) {
            for (int y = 0; y < n; y++) {
//				System.out.println("ifft-g: "+g[x][y].getR()+"  "+g[x][y].getI());
            }
        }
//----------------------------------------------------------------------
        // fifth：取实部
        for (int x = 0; x < m; x++) {
            for (int y = 0; y < n; y++) {
                last[x][y] = (int) g[x][y].getR();
//				System.out.println(last[x][y]);
            }
        }
//----------------------------------------------------------------------
        // sixth: move the image back and cut the image 乘以(-1)^(x+y)再剪裁图像
//		int srcpixel, srcred;
        int newalpha = (-1) << 24;
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                //	srcpixel = img.getRGB(i, j);
                //	srcred = srcpixel&0x00ff0000>>16;
                newred = last[i][j];
                if ((i + j) % 2 != 0)
                    newred = -newred;
                //	newred = srcred-newred;
                newblue = newred; // 先写这个 ，如果先改变newred的值，newblue也会变成改过后的newred！
                newgreen = newred << 8; // 这个也一样，反正不能放到newred改变自己之前！
                newred = newred << 16;
                newrgb = newalpha | newred | newgreen | newblue;
                destimg.setRGB(i, j, newrgb);
//				System.out.println("R: "+newred+"---G: "+newgreen+"---B: "+newblue);
            }
        }
//----------------------------------------------------------------------
        return destimg;
    }

//---------------------other functions--------------------------

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

    // 返回傅里叶频谱图
    public static BufferedImage showFourierImage(Complex[][] f) {
        int w = f.length;
        int h = f[0].length;
        double max = 0;
        double min = 0;
        BufferedImage destimg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        //-------------------First get abs(取模)--------------------------
        double[][] abs = new double[w][h];
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                abs[i][j] = f[i][j].abs();
//				System.out.println(f[i][j].getR()+"  "+f[i][j].getI());
            }
        }
        //-------------------Second get log(取log + 1)-------------------
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                abs[i][j] = Math.log(abs[i][j] + 1);
            }
        }
        //-------------------Third quantization(量化)---------------------
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
        //-------------------Fourth setImage----------------------------
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

    // normal 2-D DFT
    public static Complex DFT(int[][] f, int u, int v) {
        int M = f.length;
        int N = f[0].length;
        Complex c = new Complex(0, 0);
        for (int x = 0; x < M; x++) {
            for (int y = 0; y < N; y++) {
                Complex temp = new Complex(0, -2 * Math.PI * (u * x / (float) M + v * y / (float) N));
                c = c.plus(temp.exp().times(f[x][y]));
            }
        }
        return c;
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

    // 快速一维卷积
    public Complex[] convolve(Complex[] x, Complex[] y) {

        if (x.length != y.length) {
            throw new RuntimeException("Dimension don't agree");
        }

        int N = x.length;

        // compute fft of each sequence;
        Complex[] a = fft(x);
        Complex[] b = fft(y);

        // point-wise multiply
        Complex[] c = new Complex[N];
        for (int i = 0; i < N; i++) {
            c[i] = a[i].times(b[i]);
        }

        // compute inverse FFT
//		return ifft(c);
        return c;
    }
}