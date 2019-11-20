package com.github.microwww.generate;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.microwww.generate.util.FileHelper;
import com.github.microwww.generate.util.ParserHelper;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class CreateControllerTest {

    private static final Logger logger = LoggerFactory.getLogger(CreateControllerTest.class);

    @Test
    public void testToURI() {
        String u = CreateController.toURI("AbcdEfghiFEE");
        assertEquals("abcd-efghi-f-e-e", u);
    }

    @Test
    public void testCreateController() {
        List<CompilationUnit> units = FileHelper.scanJavaFile(JpaEntityTest.file, f -> {
            try {
                CompilationUnit parse = StaticJavaParser.parse(f);
                return ParserHelper.findTypeByAnnotation(parse, "Entity").map(e -> {
                    CreateController create = new CreateController(JpaEntityTest.file, (ClassOrInterfaceDeclaration) e);
                    CompilationUnit unit = create.createClass("cn.lcs.generate.controller");
                    return unit;
                });
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
        assertFalse(units.isEmpty());
        logger.info("writer java file : {}", units.get(0).toString());

        FileHelper.writeJavaFile(JpaEntityTest.file, units);
    }
}