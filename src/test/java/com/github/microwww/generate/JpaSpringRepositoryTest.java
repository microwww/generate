package com.github.microwww.generate;

import com.github.javaparser.ast.CompilationUnit;
import com.github.microwww.generate.util.FileHelper;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JpaSpringRepositoryTest {

    private static final Logger logger = LoggerFactory.getLogger(JpaEntityTest.class);

    @Test
    public void scanJavaEntity2repository() throws IOException {
        File src = JpaEntityTest.test;
        String pkg = "cn.lcs.generate.repository";
        List<CompilationUnit> units = JpaSpringRepository.readJavaEntity2repository(src, pkg);
        List<File> files = FileHelper.writeJavaFile(src, units);
        assertFalse(files.isEmpty());

        logger.info(files.get(0).getCanonicalPath());

        String list = Files.readAllLines(files.get(0).toPath()).stream().collect(Collectors.joining("\n"));
        assertTrue(list.contains("findAll"));
    }
}