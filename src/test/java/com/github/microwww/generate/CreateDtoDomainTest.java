package com.github.microwww.generate;

import com.github.javaparser.ast.CompilationUnit;
import com.github.microwww.generate.util.FileHelper;
import org.junit.Test;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class CreateDtoDomainTest {

    @Test
    public void testCreateBaseClass() {
        CompilationUnit unit = CreateDtoDomain.createBaseClass("cn.lcs.generate.controller.dto");
        FileHelper.writeJavaFile(JpaEntityTest.file, Collections.singletonList(unit));
    }

    @Test
    public void testCreateEntityDTO() {
        File file = JpaEntityTest.file;
        List<CompilationUnit> dto = CreateDtoDomain.createEntityDTO(file, "cn.lcs.generate.controller.dto");
        System.out.println(dto.get(0).toString());
        FileHelper.writeJavaFile(file, dto);
    }
}