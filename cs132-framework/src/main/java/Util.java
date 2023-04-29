import javax.lang.model.type.NullType;

public class Util {
    static <T> T error(String s) {
        System.out.println(s);
        System.exit(1);
        return null;
    }
}
