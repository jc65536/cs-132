class M {
    public static void main(String[] args) {
        A a;
        a = new B();
        System.out.println(a.a1());
        a = new C();
        System.out.println(a.a1());
    }
}

class A {
    public int a1() {
        return 0;
    }

    public int a2() {
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

class E extends A {
    public int a2() {
        return 1;
    }

    public int b1() {
        return 0;
    }
}
