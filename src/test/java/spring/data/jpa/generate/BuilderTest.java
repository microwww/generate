package spring.data.jpa.generate;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class BuilderTest {

    @Test
    public void createEntity() throws IOException {
        File f = new File("C:\\Users\\changshu.li\\Desktop\\demo");
        Map<CreateI18nException.Type, CompilationUnit> map = CreateI18nException.writeException(f, "test.exception");
        new Builder("com.mysql.cj.jdbc.Driver", "jdbc:mysql://localhost:3306/test", "root", "123456")
                .createEntity(f, "test.domain").stream().forEach(entity -> {
            entity.entityIdGeneratedValue();
            entity.entitySetToList();
            entity.write(f);

            entity.createRepository("test.repository").ifPresent(repository -> {
                repository.addFindAll();
                repository.addFindById();
                repository.write(f);

                SpringService service = repository.createService("test.service");
                service.addFindAll();
                service.addFindById();
                CompilationUnit dec = map.get(CreateI18nException.Type.ExistException);
                String clz = dec.getTypes().get(0).getFullyQualifiedName().get();
                service.addGetOrElseThrow(StaticJavaParser.parseClassOrInterfaceType(clz + ".NotExist"));
                service.addSave();
                service.write(f);

                Clazz base = ViewDomain.createBaseClass("test.vo");
                base.write(f);
                ViewDomain view = entity.createView("test.vo", base.toClassOrInterfaceType());
                view.write(f);

                SpringController controller = service.createController("test.controller", view);
                controller.addDetailMethod();
                controller.addListMethod();
                controller.write(f);
            });
            entity.createForm("test.form").write(f);
        });
    }
}
