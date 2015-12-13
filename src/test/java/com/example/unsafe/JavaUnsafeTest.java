package com.example.unsafe;

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
import java.util.Date;

import static com.example.unsafe.JavaUnsafeHelper.*;

/**
 * Created by Matija Vižintin
 * Date: 11. 12. 2015
 * Time: 15:47
 *
 * IMPORTANT NOTE: some tests crash the JVM, at least the 1.8.0_65 on osx
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
        long[] size = approxSizeOf(new SomeObject());
        System.out.printf("Size of \'SomeObject\' is (no-padding: %d, memory-padding: %d)\n", size[0], size[1]);
    }

    @Test
    public void sizeOf2() {
        Unsafe unsafe = JavaUnsafe.getUnsafe();

        SomeObject someObject = new SomeObject();
        long size = unsafe.getAddress(normalize(unsafe.getInt(someObject, 12L)));
        System.out.printf("Size of \'SomeObject\' is %d\n", size);
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

    @Test
    public void shallowCopy() {
        Unsafe unsafe = JavaUnsafe.getUnsafe();

        SomeObject someObject = new SomeObject();

        long size = approxSizeOf(someObject)[1];
        long start = toAddress(unsafe, someObject);

        long address = unsafe.allocateMemory(size);
        unsafe.copyMemory(start, address, size);

        SomeObject copy = (SomeObject)fromAddress(unsafe, address);
        System.out.printf("Shallow copy of SomeObject: %s\n", copy);
    }

    @Test
    public void hidePassword() throws NoSuchFieldException, IllegalAccessException {
        Unsafe unsafe = JavaUnsafe.getUnsafe();

        String password = new String("securepass");
        String hidden = new String(password.replaceAll(".", "*"));
        System.out.printf("Original password: %s\n", password); // securepass
        System.out.printf("Hidden password: %s\n", hidden); // *********
        Assert.assertEquals("securepass", password);
        Assert.assertEquals("**********", hidden);

        unsafe.copyMemory(hidden, 0L, null, toAddress(unsafe, password), approxSizeOf(password)[1]);

        System.out.printf("Original password after memory manipulation: %s\n", password);
        System.out.printf("Hidden password after memory manipulation: %s\n",hidden);

        Field stringValue = String.class.getDeclaredField("value");
        stringValue.setAccessible(true);
        char[] mem = (char[]) stringValue.get(password);
        for (int i=0; i < mem.length; i++) {
            mem[i] = '*';
        }
    }

    @Test
    public void multipleInheritance() {
        Unsafe unsafe = JavaUnsafe.getUnsafe();

        long intClassAddress = normalize(unsafe.getInt(new Integer(0), 4L));
        long strClassAddress = normalize(unsafe.getInt("", 4L));
        unsafe.putAddress(intClassAddress + 36, strClassAddress);

        String s = (String) (Object) (new Integer(666));
    }
}
