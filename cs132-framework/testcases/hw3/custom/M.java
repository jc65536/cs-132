class M {
    public static void main(String[] args) {
        A m;
        m = new B();
        System.out.println(m.b());
    }
}

class A {
    public int a() {
        return 1;
    }
}

class B extends A {
    public int b() {
        return 0;
    }
}

class C extends B {
    A n;
    int k;

    public int b() {
        System.out.println(n.a());
        return 2 + 1;
    }
}
