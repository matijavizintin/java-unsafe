package com.example.unsafe;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.logging.Logger;

/**
 * Created by Matija Vižintin
 * Date: 13. 12. 2015
 * Time: 10:15
 */
public class JavaUnsafeHelper {
    private static final Logger LOGGER = Logger.getLogger(JavaUnsafeHelper.class.getSimpleName());

    public static long[] approxSizeOf(Object o) {
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
            LOGGER.info("Field " + f.getName() + " has offset " + offset);

            if (offset > maxSize) {
                maxSize = offset;
            }
        }

        return new long[]{maxSize, ((maxSize / 8) + 1) * 8};   // padding
    }

    public static Object readFromMemoryAddress(Unsafe unsafe, long address) {
        Object[] array = new Object[]{null};
        long baseOffset = unsafe.arrayBaseOffset(Object[].class);
        unsafe.putLong(array, baseOffset, address);
        return array[0];
    }

    public static long calculateMemoryAddress(Unsafe unsafe, Object o) {
        Object[] array = new Object[]{o};

        long baseOffset = unsafe.arrayBaseOffset(Object[].class);
        int addressSize = unsafe.addressSize();
        long objectAddress;
        switch (addressSize) {
            case 4:
                objectAddress = unsafe.getInt(array, baseOffset);
                break;
            case 8:
                objectAddress = unsafe.getLong(array, baseOffset);
                break;
            default:
                throw new Error("unsupported address size: " + addressSize);
        }

        return objectAddress;
    }

    public static void printBytesFromMemory(Unsafe unsafe, long objectAddress, int limit) {
        for (long i = 24; i < limit; i = i + 2) {
            int cur = unsafe.getByte(objectAddress + i);
            System.out.print((char)cur);
        }
        System.out.println();
    }
}
