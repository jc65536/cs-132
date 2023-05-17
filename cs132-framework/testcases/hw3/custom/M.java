class M {
    public static void main(String[] args) {
        A m;
        m = new A();
        m = new B();
        m = new C();
        m = new D();
        m = new E();
        m = new F();
    }
}

class A {
    public int a1() {
        return 0;
    }
}

class B extends A {
    public int b1() {
        return 0;
    }

    public int b2() {
        return 0;
    }
}

class C extends B {
    public int a1() {
        return 1;
    }

    public int b1() {
        return 1;
    }
}

class D extends B {
    public int b2() {
        return 1;
    }
}

class E extends A {
    public int a1() {
        return 1;
    }

    public int b1() {
        return 0;
    }
}

class F extends E {
    public int f1() {
        return 0;
    }
}
