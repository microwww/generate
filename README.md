# generate
Auto generate java code, make with javaparser !

Auto create java code for spring-data-jpa.

## help to create
java Entity, repository(spring-data-jpa), Service, and Controller 

## DEMO
You can see the [demo in test](https://github.com/microwww/generate/tree/master/src/test/java/com/github/microwww/generate)

## Simple Builder

1. 添加 maven 依赖
    ```
    <dependency>
        <groupId>com.github.microwww</groupId>
        <artifactId>generate</artifactId>
        <version>0.1.0</version>
        <scope>test</scope>
    <dependency>
    ```
2. next run ::
    ```
    File file = new File(System.getProperty("user.dir"), "test");
    GenerateBuilder.config("com.mysql.cj.jdbc.Driver", "jdbc:mysql://localhost:3306/demo", "root", "123456")
            .writeEntity(file, "com.example.demo.domain")
            .writeEntityIdGeneratedValue()
            .writerEntitySetToList()
        .and().dto("com.example.demo.dto")
            .writeAbstractBaseClassFile()
            .writeDTOFile()
        .and().repository("com.example.demo.repository")
            .writeRepositoryFile()
        .and().service("com.example.demo.service")
            .writeServiceFile()
        .and().controller("com.example.demo.controller")
            .writeControllerFile();
    ```
good luck !
