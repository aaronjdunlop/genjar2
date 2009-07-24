package package2;

import package1.Class2;
import package1.Class1;

public class Class5 {

    public static void main(String[] args) {
        Class5 class5 = new Class5();
        class5.doNothing(Class1.class, Class2.class);
    }

    private void doNothing(Object theClass, Object anotherClass) {
    }
}