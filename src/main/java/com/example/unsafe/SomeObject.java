package com.example.unsafe;

/**
 * Created by Matija Vižintin
 * Date: 11. 12. 2015
 * Time: 14:47
 */
public class SomeObject {
    public byte someField1 = 1;
    public String someField2;
    public float someField3 = 1.99f;
    public int[] someField4 = new int[100];

    public SomeObject() {
        this.someField1 = 1;
        this.someField2 = "someField2";
    }

    public static void main(String[] args) {
        SomeObject so = new SomeObject();

        long[] size = JavaUnsafeHelper.approxSizeOf(so);
        System.out.println("size = " + size[0]);
        System.out.println("size = " + size[1]);
    }
}
