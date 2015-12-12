import org.junit.Assert;
import org.junit.Test;
import sun.misc.Unsafe;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.HashSet;

/**
 * Created by Matija Vižintin
 * Date: 11. 12. 2015
 * Time: 15:47
 */
public class JavaUnsafeTest {

    @Test
    public void memory() throws NoSuchFieldException {
        Unsafe unsafe = JavaUnsafe.getUnsafe();

        System.out.printf("Address size: %d\n", unsafe.addressSize());
        System.out.printf("Page size: %d\n\n", unsafe.pageSize());

        // print before
        SomeObject instance = new SomeObject();
        System.out.printf("Value 'someField1' before memory corruption: %s\n", instance.someField1);
        System.out.printf("Value 'someField2' before memory corruption: %s\n", instance.someField2);
        System.out.printf("Value 'someField3' before memory corruption: %s\n", instance.someField3);
        System.out.printf("Value 'someField4' before memory corruption: %s\n\n", instance.someField4);

        // memory corruption
        Field f = instance.getClass().getDeclaredField("someField1");
        unsafe.putObject(instance, unsafe.objectFieldOffset(f), new String[10000]);

        // print after
        System.out.printf("Value \'someField1\' after memory corruption: %s\n", instance.someField1);
        System.out.printf("Value \'someField2\' after memory corruption: %s\n", instance.someField2);
        System.out.printf("Value \'someField3\' after memory corruption: %s\n", instance.someField3);
        System.out.printf("Value \'someField4\' after memory corruption: %s\n", instance.someField4);
    }

    @Test
    public void sizeOf() {
        long size = sizeOf(new SomeObject());
        System.out.printf("Size of \'SomeObject\' is %d\n", size);
    }

    private long sizeOf(Object o) {
        Unsafe unsafe = JavaUnsafe.getUnsafe();

        HashSet<Field> fields = new HashSet<>();
        Class c = o.getClass();
        while (c != Object.class) {
            for (Field f : c.getDeclaredFields()) {
                if ((f.getModifiers() & Modifier.STATIC) == 0) {
                    fields.add(f);
                }
            }
            c = c.getSuperclass();
        }

        // get offset
        long maxSize = 0;
        for (Field f : fields) {
            long offset = unsafe.objectFieldOffset(f);
            if (offset > maxSize) {
                maxSize = offset;
            }
        }

        return ((maxSize / 8) + 1) * 8;   // padding
    }

    //@Test     NOTE: this crashes the JVM at least the 1.8.0_65 on osx
    public void sizeOf2() {
        Unsafe unsafe = JavaUnsafe.getUnsafe();

        SomeObject someObject = new SomeObject();
        long size = unsafe.getAddress(normalize(unsafe.getInt(someObject, 12L)));
        System.out.printf("Size of \'SomeObject\' is %d\n", size);
    }

    private long normalize(int value) {
        if(value >= 0) return value;
        return (~0L >>> 32) & value;
    }

    @Test
    public void objectManipulation() throws InstantiationException, NoSuchFieldException {
        Unsafe unsafe = JavaUnsafe.getUnsafe();

        JavaUnsafe instance = (JavaUnsafe)unsafe.allocateInstance(JavaUnsafe.class);

        // get fields
        Field field1 = instance.getClass().getDeclaredField("someValue");
        Field field2 = instance.getClass().getDeclaredField("someField1");
        Field field3 = instance.getClass().getDeclaredField("someField2");
        long offset1 = unsafe.objectFieldOffset(field1);
        long offset2 = unsafe.objectFieldOffset(field2);
        long offset3 = unsafe.objectFieldOffset(field3);
        System.out.printf("\'someValue\' memory offset: %d\n", offset1);
        System.out.printf("\'someField1\' memory offset: %d\n", offset2);
        System.out.printf("\'someField2\' memory offset: %d\n", offset3);
    }

    @Test
    public void classManipulation()
            throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        Unsafe unsafe = JavaUnsafe.getUnsafe();

        File f = new File("/Users/matijav/Projects/roadrunner/trunk/common/classes/com/solveralynx/gema/common/Period.class");
        byte[] content = null;
        try (FileInputStream input = new FileInputStream(f)) {
            content = new byte[(int)f.length()];
            input.read(content);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // create class with unsafe
        Class<?> c = unsafe.defineClass("com/solveralynx/gema/common/Period", content, 0, content.length, null, null);

        // create instance and set value on it
        Object period = c.newInstance();
        Method dateSetter = c.getMethod("setFromDate", Date.class);
        dateSetter.invoke(period, new Date());

        // print value
        Date date = (Date)c.getMethod("getFromDate").invoke(period);
        System.out.printf("DateFrom on period: %s\n", date);
    }

    //@Test     NOTE: this crashes the JVM at least the 1.8.0_65 on osx
    public void shallowCopy() {
        Unsafe unsafe = JavaUnsafe.getUnsafe();

        SomeObject someObject = new SomeObject();

        long size = sizeOf(someObject);
        long start = toAddress(unsafe, someObject);

        long address = unsafe.allocateMemory(size);
        unsafe.copyMemory(start, address, size);

        SomeObject copy = (SomeObject)fromAddress(unsafe, address);
        System.out.printf("Shallow copy of SomeObject: %s\n", copy);
    }

    private long toAddress(Unsafe unsafe, Object obj) {
        Object[] array = new Object[] {obj};
        long baseOffset = unsafe.arrayBaseOffset(Object[].class);
        return normalize(unsafe.getInt(array, baseOffset));
    }

    private Object fromAddress(Unsafe unsafe, long address) {
        Object[] array = new Object[] {null};
        long baseOffset = unsafe.arrayBaseOffset(Object[].class);
        unsafe.putLong(array, baseOffset, address);
        return array[0];
    }

    @Test
    public void hidePassword() throws NoSuchFieldException, IllegalAccessException {
        String password = new String("securepass");
        String hidden = new String(password.replaceAll(".", "*"));
        System.out.printf("Original password: %s\n", password); // securepass
        System.out.printf("Hidden password: %s\n", hidden); // *********
        Assert.assertEquals("securepass", password);
        Assert.assertEquals("**********", hidden);

        Unsafe unsafe = JavaUnsafe.getUnsafe();
        unsafe.copyMemory(hidden, 0L, null, toAddress(unsafe, password), sizeOf(password));

        System.out.printf("Original password after memory manipulation: %s\n", password);
        System.out.printf("Hidden password after memory manipulation: %s\n",hidden);

        Field stringValue = String.class.getDeclaredField("value");
        stringValue.setAccessible(true);
        char[] mem = (char[]) stringValue.get(password);
        for (int i=0; i < mem.length; i++) {
            mem[i] = '*';
        }
    }
}
