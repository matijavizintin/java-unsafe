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
        new SomeObject();
    }
}
