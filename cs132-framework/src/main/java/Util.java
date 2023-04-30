public class Util {
    static <T> T error(String s) {
        System.out.println("Type error");
        System.exit(1);
        return null;
    }

    static boolean expect(boolean b, String msg) {
        if (!b)
            Util.error(msg);
        
        return b;
    }
}
