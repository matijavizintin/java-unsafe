import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * Created by Matija Vižintin
 * Date: 11. 12. 2015
 * Time: 14:33
 */
public class JavaUnsafe {
    private String someValue = null;
    private Long someField1 = null;
    private Double someField2 = null;

    private JavaUnsafe() {
        someValue = "someValue";
        someField1 = 1L;
        someField2 = 1.;
    }

    public static Unsafe getUnsafe() {
        // retrieve unsafe
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            return (Unsafe)f.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return null;
        }
    }
}
