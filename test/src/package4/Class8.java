package package4;

import package4.jar.Class8_1;

public class Class8
{
    public static void main(String[] args)
    {
        // Reference a class from a jar file
        Class<?> c = Class8_1.class;
        System.out.println(c.getName());
    }
}
