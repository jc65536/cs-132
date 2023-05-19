class Main {
	public static void main(String[] a){
		System.out.println(new A().run());
	}
}

class A {
    int a;

	public int run() {
		int b;
        a = 1;
		b = this.helper(15);
		return b;
	}

	public int helper(int param) {
		int x;
		x = param;
		param = param + 1;
		System.out.println(x+a);
		return x;
	}
}
