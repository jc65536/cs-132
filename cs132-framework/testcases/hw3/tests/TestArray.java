class Main {
	public static void main(String[] a){
        int[] x;
        int[] y;
        int z;
        A b;
        b = new A();
        x = new int[4];
        
        x[3] = 2;
        y = x;
        z = 0;
        System.out.println(x.length);
        System.out.println(y[3]);
        System.out.println(y[2]);
        // System.out.println(x[(0-1)]);
        System.out.println(x[(b.return_negative())]);
        // y = new int[(0-1)];
	}
}
class A
{
    int a;
    public int return_negative()
    {
        return 0-5;
    }
}