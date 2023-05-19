class Main {
	public static void main(String[] a){
        boolean x;
        boolean y;
        int b;
        x = true;
        y = false;
        b = 0;
        while(x&&y)
            b = b+1;
        while(x)
            x = false;
        System.out.println(b);
	}
}
