package com.github.microwww.generate;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CreateControllerTest {

    @Test
    public void testToURI() {
        String u = CreateController.toURI("AbcdEfghiFEE");
        assertEquals("abcd-efghi-f-e-e", u);
    }

    @Test
    public void testCreate() {
        CompilationUnit unit = CreateController.createClass("a.b.c",
                StaticJavaParser.parse("public class GoConfig {@Id private int id;}").getTypes().get(0).asClassOrInterfaceDeclaration(),
                StaticJavaParser.parse("public class GoConfigService {}").getTypes().get(0).asClassOrInterfaceDeclaration());
        System.out.println(unit.toString());
    }
}