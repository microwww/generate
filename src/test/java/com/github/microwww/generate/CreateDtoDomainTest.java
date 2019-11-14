package com.github.microwww.generate;

import com.github.javaparser.ast.CompilationUnit;
import org.junit.Test;

import static org.junit.Assert.*;

public class CreateDtoDomainTest {

    @Test
    public void testCreateBaseClass(){
        CompilationUnit unit = CreateDtoDomain.createBaseClass("a.b.c");
        System.out.println(unit.toString());
    }
}