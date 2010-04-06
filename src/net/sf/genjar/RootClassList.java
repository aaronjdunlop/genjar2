package net.sf.genjar;

public class RootClassList
{
    private String classnames;

    public void setNames(String classnames)
    {
        this.classnames = classnames;
    }

    public String[] getClassNames()
    {
        return classnames != null && classnames.length() > 0 ? classnames.split(",") : new String[0];
    }
}
