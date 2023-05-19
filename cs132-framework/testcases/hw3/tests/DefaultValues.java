class Main {
	public static void main(String[] a){
		int x;
        boolean y;
        A b;
        A c;
        b = new A();
        System.out.println(x);
        System.out.println(y);
        System.out.println(b.run());
        System.out.println(c);
        System.out.println(c.run());
	}
}

class A 
{
    int x;
    boolean y;
    A a;

    public int run()
    {
        System.out.println(y);
        System.out.println(a);
        return x;
    }
}