class Main {
	public static void main(String [] args) 
	{
		System.out.println(new C().run());
	}
}
class A extends B 
{
	public int run()
	{ 
		return 2; 
	}
}
class B 
{
	public int run() 
	{ 
		return 1; 
	}
}
class C 
{
	public int run()
	{
		A a;
		B b;
		a = new A();
		b = new A();
		System.out.println(a.run());
		System.out.println(b.run());
		return 3;
	}
}
