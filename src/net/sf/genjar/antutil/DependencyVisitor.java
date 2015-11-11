/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package net.sf.genjar.antutil;

import java.util.HashMap;
import java.util.Set;
import java.util.StringTokenizer;

import net.sf.genjar.GenJar;

import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.DescendingVisitor;
import org.apache.bcel.classfile.EmptyVisitor;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

/**
 * Copy of Ant's DependencyVisitor in genjar namespace so we won't have to have Ant's BCEL jar in classpath
 */
public class DependencyVisitor extends EmptyVisitor {

    /** The collected dependencies */
    private final HashMap<String, String> dependencies = new HashMap<String, String>();

    /** Reference chain, used to backtrack the source of a class reference. See {@link GenJar} */
    private final HashMap<String, String> referenceMap;

    public DependencyVisitor(final HashMap<String, String> referenceMap) {
        this.referenceMap = referenceMap;
    }

    /**
     * The current class's constant pool - used to determine class names from class references.
     */
    private ConstantPool constantPool;

    /**
     * A reference to the {@link DescendingVisitor} that is currently executing, so we can backtrack the reference
     * chain.
     */
    private DescendingVisitor descendingVisitor;

    /**
     * Returns the set of classes required by the visited classes
     * 
     * @return The set of classes required by the visited classes
     */
    public Set<String> getDependencies() {
        return dependencies.keySet();
    }

    /** Clear the current set of collected dependencies. */
    public void clearDependencies() {
        dependencies.clear();
    }

    /**
     * Visit the constant pool of a class
     * 
     * @param constantPool the constant pool of the class being visited.
     */
    @Override
    @SuppressWarnings("hiding")
    public void visitConstantPool(final ConstantPool constantPool) {
        this.constantPool = constantPool;
    }

    /**
     * Visit a class reference
     * 
     * @param constantClass the constantClass entry for the class reference
     */
    @Override
    public void visitConstantClass(final ConstantClass constantClass) {
        final String classname = constantClass.getConstantValue(constantPool).toString();
        addSlashClass(classname);
    }

    /**
     * Visit a name and type ref
     * 
     * Look for class references in this
     * 
     * @param obj the name and type reference being visited.
     */
    @Override
    public void visitConstantNameAndType(final ConstantNameAndType obj) {
        final String name = obj.getName(constantPool);
        if (obj.getSignature(constantPool).equals("Ljava/lang/Class;") && name.startsWith("class$")) {
            String classname = name.substring("class$".length()).replace('$', '.');
            // does the class have a package structure
            final int index = classname.lastIndexOf(".");
            if (index > 0) {
                char start;
                // check if the package structure is more than 1 level deep
                final int index2 = classname.lastIndexOf(".", index - 1);
                if (index2 != -1) {
                    // class name has more than 1 package level 'com.company.Class'
                    start = classname.charAt(index2 + 1);
                } else {
                    // class name has only 1 package level 'package.Class'
                    start = classname.charAt(0);
                }
                // Check to see if it's an inner class 'com.company.Class$Inner'
                // CheckStyle:MagicNumber OFF
                if ((start > 0x40) && (start < 0x5B)) {
                    // first letter of the previous segment of the class name 'Class'
                    // is upper case ascii. so according to the spec it's an inner class
                    classname = classname.substring(0, index) + "$" + classname.substring(index + 1);
                    addClass(classname);
                } else {
                    // Add the class in dotted notation 'com.company.Class'
                    addClass(classname);
                }
                // CheckStyle:MagicNumber ON
            } else {
                // Add a class with no package 'Class'
                addClass(classname);
            }
        }
    }

    /**
     * Visit a field of the class.
     * 
     * @param field the field being visited
     */
    @Override
    public void visitField(final Field field) {
        addClasses(field.getSignature());
    }

    /**
     * Visit a Java class
     * 
     * @param javaClass the class being visited.
     */
    @Override
    public void visitJavaClass(final JavaClass javaClass) {
        addClass(javaClass.getClassName());
    }

    /**
     * Visit a method of the current class
     * 
     * @param method the method being visited.
     */
    @Override
    public void visitMethod(final Method method) {
        final String signature = method.getSignature();
        final int pos = signature.indexOf(")");
        addClasses(signature.substring(1, pos));
        addClasses(signature.substring(pos + 1));
    }

    /**
     * Add a classname to the list of dependency classes
     * 
     * @param classname the class to be added to the list of dependencies.
     */
    void addClass(final String classname) {
        int level = 0;
        Object predecessor = descendingVisitor.predecessor();
        try {
            for (; predecessor != null && !(predecessor instanceof JavaClass); level++) {
                predecessor = descendingVisitor.predecessor(level);
            }
        } catch (final Exception e) {
            predecessor = null;
        }

        if (predecessor != null) {
            final String referencingClassname = ((JavaClass) predecessor).getClassName();

            if (!referenceMap.containsKey(classname) && !classname.equals(referencingClassname)) {
                referenceMap.put(classname, referencingClassname);
            }
        }

        dependencies.put(classname, classname);
    }

    /**
     * Add all the classes from a descriptor string.
     * 
     * @param string the descriptor string, being descriptors separated by ';' characters.
     */
    private void addClasses(final String string) {
        final StringTokenizer tokens = new StringTokenizer(string, ";");
        while (tokens.hasMoreTokens()) {
            final String descriptor = tokens.nextToken();
            final int pos = descriptor.indexOf('L');
            if (pos != -1) {
                addSlashClass(descriptor.substring(pos + 1));
            }
        }
    }

    /**
     * Adds a class name in slash format (for example org/apache/tools/ant/Main).
     * 
     * @param classname the class name in slash format
     */
    private void addSlashClass(final String classname) {
        addClass(classname.replace('/', '.'));
    }

    public void setDescendingVistor(final DescendingVisitor descendingVisitor) {
        this.descendingVisitor = descendingVisitor;
    }
}