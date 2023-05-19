class Main {
	public static void main(String[] a){
		System.out.println(new E().test2());
	}
}

class A {
	int y;
	public int run() {
		int x;
        x = 1;
		return y;
	}
}
class B extends A {
	boolean y;
	public int run() {
		int x;
        x = 2;
		return x;
	}
}  

class D {
	public A test()
	{
		A b;
		b = new B();
		System.out.println(b.run());
		return b;
	}
}

class E
{
	public int test2()
	{
		A b;
		D d;
		b = new A();
		System.out.println(b.run());
		d = new D();
		b = d.test();
		System.out.println(b.run());
		return 1;
	}
}