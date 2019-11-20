package com.github.microwww.generate;

import com.github.javaparser.ast.CompilationUnit;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

public class JpaEntityTest {

    public static final File file;

    static { // maven standard directory structure
        File root = new File(FileHelper.class.getResource("/").getFile());//  !! / ==> /target/test-classes/
        file = FileSystems.getDefault().getPath(root.getAbsolutePath(), "..", "..",
                "src", "main", "java").toFile();
    }

    @Test
    @Ignore("Skip, this is a demo !")
    public void createEntity() {
        JpaEntity entities = new JpaEntity(file, "cn.lcs.generate.domain");
        entities.db2entity("com.mysql.cj.jdbc.Driver", "jdbc:mysql://192.168.1.31/go-one", "root", "123456");
    }

    @Test
    public void testWriteEntityIdGeneratedValue() throws IOException {
        JpaEntity entities = new JpaEntity(file, "cn.lcs.generate.domain");
        List<File> file = entities.writeEntityIdGeneratedValue();
        Assert.assertFalse(file.isEmpty());
        // try again
        file = entities.writeEntityIdGeneratedValue();
        List<String> list = Files.readAllLines(file.get(0).toPath());
        String str = list.stream().collect(Collectors.joining("\n"));
        Assert.assertTrue(str.contains("GeneratedValue"));
    }

    @Test
    public void testEntitySetToList() {
        JpaEntity entities = new JpaEntity(file, "cn.lcs.generate.domain");
        List<CompilationUnit> file = entities.entitySetToList();
        Assert.assertFalse(file.isEmpty());
        // writer
        entities.writerEntitySetToList();
    }


}