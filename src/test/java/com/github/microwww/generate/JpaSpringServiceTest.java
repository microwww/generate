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

public class JpaSpringServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(JpaEntityTest.class);

    @Test
    public void testReadRepositoryCreateService() throws IOException {
        File file = JpaEntityTest.file;
        List<CompilationUnit> service = JpaSpringService.readRepositoryCreateService(file, "cn.lcs.generate.service");
        List<File> files = FileHelper.writeJavaFile(file, service);

        assertFalse(files.isEmpty());
        logger.info("Create file : {}", files.get(0).getCanonicalPath());

        String list = Files.readAllLines(files.get(0).toPath()).stream().collect(Collectors.joining("\n"));
        assertTrue(list.contains("findAll"));
    }
}