package cn.edu.tsinghua.dip;

class Complex {
    private final double r;
    private final double i;

    Complex(double r, double i) {
        this.r = r;
        this.i = i;
    }

    double abs() {
        return Math.hypot(r, i);
    }

    Complex plus(Complex c) {
        return new Complex(this.r + c.r, this.i + c.i);
    }

    Complex minus(Complex c) {
        return new Complex(this.r - c.r, this.i - c.i);
    }

    Complex times(Complex c) {
        return new Complex(this.r * c.r - this.i * c.i,
                this.r * c.i + this.i * c.r);
    }

    Complex times(double d) {
        return new Complex(this.r * d, this.i * d);
    }

    Complex conjugate() {
        return new Complex(r, -i);
    }

    double getR() {
        return r;
    }

    double getI() {
        return i;
    }

    Complex exp() {
        return new Complex(Math.exp(r) * Math.cos(i),
                Math.exp(r) * Math.sin(i));
    }
}