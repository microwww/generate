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

## version 0.1.1

This version modify all the api, all demo list down !
```
@Test
public void createEntity() throws IOException {
    File f = new File("C:\\Users\\changshu.li\\Desktop\\demo");
    // 创建异常类, 如果不需要可以不生成
    Map<CreateI18nException.Type, CompilationUnit> map = CreateI18nException.writeException(f, "test.exception");
    new Builder("com.mysql.cj.jdbc.Driver", "jdbc:mysql://localhost:3306/test", "root", "123456")
            // 创建实体, 如果不需要创建可以使用 JpaEntity.scanJavaEntity() 直接扫描
            .createEntity(f, "test.domain").stream().forEach(entity -> {
        // 遍历实体

        // 根据实体 生成 repository
        entity.createRepository("test.repository").ifPresent(repository -> {
            repository.addFindAll();
            repository.addFindById();
            // 生成 repository java文件, 如果已经存在可以不创建 Java 文件
            repository.write(f);

            // 根据repository 生成 service
            SpringService service = repository.createService("test.service");
            service.addFindAll();
            service.addFindById();
            // 生成 getOrElseThrow 方法. 如果不生成 则不需要生成 异常类
            CompilationUnit dec = map.get(CreateI18nException.Type.ExistException);
            String clz = dec.getTypes().get(0).getFullyQualifiedName().get();
            service.addGetOrElseThrow(StaticJavaParser.parseClassOrInterfaceType(clz));
            service.addSave();
            // 如果已经存在可以不创建 Java 文件
            service.write(f);

            // 生成 vo 类, 如果不生成 则不需要生成 异常类
            ViewDomain view = entity.createView("test.vo");
            // 如果已经存在可以不创建 Java 文件
            view.write(f);

            // 根据 service/view 生成 controller
            SpringController controller = service.createController("test.controller", view);
            controller.addDetailMethod();
            controller.addListMethod();
            // 如果已经存在, 或不需要可以不创建 Java 文件
            controller.write(f);
        });
        // 生成简单的管理后台用的 form, 使用 BeanUtils.copyProperties() 可以修改所有的表单属性
        entity.createForm("test.form").write(f);
    });
}
```
