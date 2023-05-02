class Main {
	public static void main(String[] a){
		System.out.println(new A().run());
	}
}

class A extends B {
	public int run() {
		C x;
		return x.f();
	}
}

class B {
    public int run() {
        return 4;
    }
}

class C extends C {}

class D extends E {}

class E extends D {}
