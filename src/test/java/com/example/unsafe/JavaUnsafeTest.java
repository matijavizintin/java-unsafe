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
 *
 * ANOTHER IMPORTANT NOTE: don't forget to use -XX:-UseCompressedOops otherwise jvm will compress pointers to 4bytes
 *
 * unsafe docs: http://www.docjar.com/html/api/sun/misc/Unsafe.java.html
 */
public class JavaUnsafeTest {

    @Test
    public void memory() throws NoSuchFieldException {
        @SuppressWarnings("unchecked")
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

    @Test       // TODO: it doesn't look very much accurate :)
    public void sizeOf2() {
        Unsafe unsafe = JavaUnsafe.getUnsafe();

        SomeObject someObject = new SomeObject();
        long size = unsafe.getAddress(unsafe.getLong(someObject, 8L) + 12);
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
        System.out.printf("Class name: %s\n", c.getName());

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

        // create original object
        SomeObject someObject = new SomeObject();
        System.out.printf("Original object of SomeObject: %s\n", someObject);

        // get objects size and location
        long size = approxSizeOf(someObject)[1];
        long sourceAddress = calculateMemoryAddress(unsafe, someObject);

        // allocate new memory and copy to new location
        long copyAddress = unsafe.allocateMemory(size);
        unsafe.copyMemory(sourceAddress, copyAddress, size);

        // read copy from new location
        SomeObject copy = (SomeObject)readFromMemoryAddress(unsafe, copyAddress);
        System.out.printf("Shallow copy of SomeObject: %s\n", copy);

        Assert.assertEquals(someObject, copy);
    }

    @Test
    public void readFromMemory() {
        Unsafe unsafe = JavaUnsafe.getUnsafe();
        Object mine = "Hi there".toCharArray();
        long address = calculateMemoryAddress(unsafe, mine);
        System.out.println("Address: " + address);

        // print from memory
        printBytesFromMemory(unsafe, address, 40);
    }

    @Test
    public void hidePassword() throws NoSuchFieldException, IllegalAccessException {
        Unsafe unsafe = JavaUnsafe.getUnsafe();

        // create password field and a hidden copy
        String password = new String("securepass");
        String hidden = new String(password.replaceAll(".", "*"));
        System.out.printf("Original password: %s\n", password); // securepass
        System.out.printf("Hidden password: %s\n", hidden); // *********
        Assert.assertEquals("securepass", password);
        Assert.assertEquals("**********", hidden);

        // corrupt memory
        long passwordMemoryAddress = calculateMemoryAddress(unsafe, password);
        unsafe.copyMemory(hidden, 0L, null, passwordMemoryAddress, approxSizeOf(hidden)[1]);

        // print results
        System.out.printf("Original password after memory manipulation: %s\n", password);
        System.out.printf("Hidden password after memory manipulation: %s\n",hidden);
        Assert.assertEquals(password, hidden);

        // backing array corruption
        Field stringValue = String.class.getDeclaredField("value");
        stringValue.setAccessible(true);
        char[] mem = (char[]) stringValue.get(password);
        for (int i=0; i < mem.length; i++) {
            mem[i] = '*';
        }
    }

    @Test       // TODO not working, check 36 - probably is bound to 32b jvm
    public void multipleInheritance() {
        Unsafe unsafe = JavaUnsafe.getUnsafe();

        long intClassAddress = unsafe.getLong(new Integer(0), 8L);
        long strClassAddress = unsafe.getLong("", 8L);
        unsafe.putAddress(intClassAddress + 36, strClassAddress);

        String s = (String) (Object) (new Integer(1));
    }
}
