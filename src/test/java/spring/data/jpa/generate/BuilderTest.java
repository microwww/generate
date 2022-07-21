package spring.data.jpa.generate;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class BuilderTest {

    @Ignore
    @Test
    public void createEntity() throws IOException {
        File f = new File(".\\target\\generate");
        System.out.println("-------- TARGET ------- " + f.getCanonicalPath());
        String prefix = "cn.test";
        Map<CreateI18nException.Type, CompilationUnit> map = CreateI18nException.writeException(f, prefix + ".exception");
        new Builder("com.mysql.cj.jdbc.Driver", "jdbc:mysql://localhost:3306/test", "root", "123456")
                .createEntity(f, prefix + ".domain").stream().forEach(entity -> {
            entity.entityIdGeneratedValue();
            entity.entitySetToList();
            entity.write(f);

            entity.createRepository(prefix + ".repository").ifPresent(repository -> {
                repository.addFindAll();
                repository.addFindById();
                repository.write(f);

                SpringService service = repository.createService(prefix + ".service");
                service.addFindAll();
                service.addFindById();
                CompilationUnit dec = map.get(CreateI18nException.Type.ExistException);
                String clz = dec.getTypes().get(0).getFullyQualifiedName().get();
                service.addGetOrElseThrow(StaticJavaParser.parseClassOrInterfaceType(clz + ".NotExist"));
                service.addSave();
                service.write(f);

                Clazz base = ViewDomain.createBaseClass(prefix + ".vo");
                base.write(f);
                ViewDomain view = entity.createView(prefix + ".vo", base.toClassOrInterfaceType());
                view.write(f);

                SpringController controller = service.createController(prefix + ".controller", view);
                controller.addDetailMethod();
                controller.addListMethod();
                controller.write(f);
            });
            entity.createForm(prefix + ".form").write(f);
        });
    }
}
