package com.github.microwww.generate;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;

import static com.github.javaparser.ast.Modifier.Keyword.*;

public class JavaParserDemo {

    @Test
    public void testCreateClass() {
        CompilationUnit compilationUnit = new CompilationUnit("com.github.microwww.code");
        ClassOrInterfaceDeclaration myClass = compilationUnit
                .addClass("MyClass")
                .setPublic(true);
        myClass.addField(int.class, "A_CONSTANT", PUBLIC, STATIC);
        myClass.addField(String.class, "name", PRIVATE);
        String code = myClass.toString();
        Assert.assertTrue(code.contains("public class MyClass"));
        Assert.assertTrue(compilationUnit.toString().startsWith("package"));
    }

    @Test
    public void testParseClass() {
        CompilationUnit compilationUnit = StaticJavaParser.parse("class A { }");
        Optional<ClassOrInterfaceDeclaration> classA = compilationUnit.getClassByName("A");
        classA.orElseThrow(RuntimeException::new);
    }

}
