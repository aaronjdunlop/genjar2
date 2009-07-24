package package3;

public class Class6
{
    // Two inner classes, each with an anonymous inner class of its own (the two anonymous classes should be auto-named '$1', and we want to be sure we avoid conflicts
    public static class Inner
    {
        public static Runnable runnable1 = new Runnable()
        {
            @Override
            public void run()
            {}
        };
    }

    public static class Inner2
    {
        static Runnable runnable = new Runnable()
        {
            @Override
            public void run()
            {}
        };
    }
}