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

    public static final File file = FileSystems.getDefault().getPath(System.getProperty("user.dir"), "target", "demo").toFile();
    //public static final File file =  "D:\\cygwin64\\home\\changshu.li\\generate\\demo\\src\\main\\java";

    @Test
    @Ignore("Skip, this is a demo !")
    public void createEntity() {
        JpaEntity entities = new JpaEntity("com.mysql.cj.jdbc.Driver", "jdbc:mysql://192.168.1.31/go-one", "root", "123456");//(file, "cn.lcs.generate.domain");
        entities.createEntity(file, "cn.lcs.generate.domain");
    }

    @Test
    public void testWriteEntityIdGeneratedValue() throws IOException {
        List<File> file = JpaEntity.writeEntityIdGeneratedValue(JpaEntityTest.file);
        Assert.assertFalse(file.isEmpty());
        // try again
        file = JpaEntity.writeEntityIdGeneratedValue(JpaEntityTest.file);
        List<String> list = Files.readAllLines(file.get(0).toPath());
        String str = list.stream().collect(Collectors.joining("\n"));
        Assert.assertTrue(str.contains("GeneratedValue"));
    }

    @Test
    public void testEntitySetToList() {
        List<CompilationUnit> file = JpaEntity.entitySetToList(JpaEntityTest.file);
        Assert.assertFalse(file.isEmpty());
        // writer
        JpaEntity.writerEntitySetToList(JpaEntityTest.file);
    }


}