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

    public static long normalize(int value) {
        if(value >= 0) return value;
        return (~0L >>> 32) & value;
    }

    public static long toAddress(Unsafe unsafe, Object obj) {
        Object[] array = new Object[] {obj};
        long baseOffset = unsafe.arrayBaseOffset(Object[].class);
        return normalize(unsafe.getInt(array, baseOffset));
    }

    public static Object fromAddress(Unsafe unsafe, long address) {
        Object[] array = new Object[] {null};
        long baseOffset = unsafe.arrayBaseOffset(Object[].class);
        unsafe.putLong(array, baseOffset, address);
        return array[0];
    }
}
