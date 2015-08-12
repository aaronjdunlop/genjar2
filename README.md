The `<genjar>` task provides an easy method to package a standalone jar file including all dependencies and referenced classes. A basic example specifies a root class (often the class which defines a main() entry point) and a classpath which will be searched for referenced classes. `<genjar>` traces through references and packages a jar file which contains all classes which will be required at runtime.

```
<?xml version="1.0" encoding="UTF-8"?>
<project default="a">

    <taskdef classpath="lib/genjar.jar" resource="genjar.properties" />

    <target name="a">
        <genjar jarfile="a.jar">
            <class name="A" />
            <classpath>
                <pathelement location="build" />
            </classpath>
        </genjar>
    </target>
</project>
```

`<genjar>` is particularly useful in conjunction with the [cltool4j](http://github.com/aaronjdunlop/cltool4j) project, to simplify developing and building standalone Java tools (see the `genjar-targets.xml` file available in that project's Downloads).

This project modifies and extends the now-defunct [SourceForge genjar project](http://genjar.sourceforge.net/) to use Ant's jar syntax and to package source jars as well.

All attributes and contained elements from  [Ant's jar task](http://ant.apache.org/manual/Tasks/jar.html) are available, and the following contained elements are added:

 * `class` : Specifies the primary root class. This class is assumed to contain the `main()` entry point, and will be specified as the primary entry point for use with `java -jar` using the `Main-Class` attribute in `META-INF/MANIFEST.MF`.

```
<class name="Foo" />
```

 * `classes`: Specifies other root classes. These classes and their dependencies will also be included in the generated jar.
```
<class name="Foo" /> <!-- The primary entry point is 'Foo' -->
<classes names="B,C,D,org.genjar.util.E"> <!-- these classes and all their dependencies will be added as well -->
```

 * `classpath`, `classpathref` - Specifies the classpath which will be searched for dependencies
 * `runtimeclasspath`, `runtimeclasspathref` - Specifies other directories or jar files which should included at runtime via the jar file `MANIFEST.MF`, but not included in the generated jar. 
```
<runtimeclasspath>
  <fileset dir="lib includes="*.jar" />
</runtimeclasspath>
```

