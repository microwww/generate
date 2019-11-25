package com.github.microwww.generate;

import org.junit.Test;

import java.io.File;

public class GenerateBuilderTest {

    @Test
    public void testBuilder() {
        File file = new File(System.getProperty("user.dir"), "test");
        GenerateBuilder.config("com.mysql.cj.jdbc.Driver", "jdbc:mysql://192.168.1.31/go-one", "root", "123456")
                .writeEntity(file, "cn.xy.goone.domain")
                .writeEntityIdGeneratedValue()
                .writerEntitySetToList()
                .and().dto("cn.xy.goone.dto").writeAbstractBaseClassFile().writeDTOFile()
                .and().repository("cn.xy.goone.repository").writeRepositoryFile()
                .and().service("cn.xy.goone.service").writeServiceFile()
                .and().controller("cn.xy.goone.controller").writeControllerFile();
    }

}