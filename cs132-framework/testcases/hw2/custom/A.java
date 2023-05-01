class Main {
	public static void main(String[] a){
		System.out.println(new A().run());
	}
}

class A extends B {
	public int run() {
		int x;
		x = 1;
		return x;
	}
}

class B {
    public int run() {
        return 4;
    }
}

class C extends D {}

class D extends E {}

class E extends D {}
