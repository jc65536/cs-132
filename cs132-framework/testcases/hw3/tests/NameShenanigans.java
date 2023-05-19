class Main {
	public static void main(String[] a){
       AA x;
       x = new AA();
       System.out.println(x.A());
	}
}

class A extends AA 
{
    public int AA() 
    { 
        return 1; 
    }
}
class AA 
{
    public int A() 
    { 
        return 0; 
    }
}