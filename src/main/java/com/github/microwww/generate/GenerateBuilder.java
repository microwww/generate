package com.github.microwww.generate;

import com.github.javaparser.ast.CompilationUnit;
import com.github.microwww.generate.util.FileHelper;

import javax.sql.DataSource;
import java.io.File;
import java.util.Collections;
import java.util.List;

public class GenerateBuilder {

    private JpaEntity jpaEntity;
    private File src;

    private GenerateBuilder(JpaEntity entities) {
        this.jpaEntity = entities;
    }

    public static GenerateBuilder config(String driver, String url, String username, String password) {
        return new GenerateBuilder(new JpaEntity(driver, url, username, password));
    }

    public static GenerateBuilder config(DataSource dataSource) {
        return new GenerateBuilder(new JpaEntity(dataSource));
    }

    public GenerateEntity writeEntity(File src, String pckage) {
        this.src = src;
        jpaEntity.createEntity(src, pckage);
        return new GenerateEntity(src);
    }

    public JpaEntity getEntities() {
        return jpaEntity;
    }

    public GenerateRepository repository(String pkg) {
        return new GenerateRepository(pkg);
    }

    public GenerateService service(String pkg) {
        return new GenerateService(pkg);
    }

    public GenerateController controller(String pkg) {
        return new GenerateController(pkg);
    }

    public GenerateDTO dto(String pkg) {
        return new GenerateDTO(pkg);
    }

    public class GenerateDTO extends Add {
        private String pkg;

        public GenerateDTO(String pkg) {
            this.pkg = pkg;
        }

        public GenerateDTO writeDTOFile() {
            List<CompilationUnit> dto = CreateDtoDomain.createEntityDTO(src, pkg);
            FileHelper.writeJavaFile(src, dto);
            return this;
        }

        public GenerateDTO writeAbstractBaseClassFile() {
            CompilationUnit dto = CreateDtoDomain.createBaseClass(pkg);
            FileHelper.writeJavaFile(src, Collections.singletonList(dto));
            return this;
        }
    }

    public class GenerateController extends Add {
        private String pkg;

        public GenerateController(String pkg) {
            this.pkg = pkg;
        }

        public GenerateController writeControllerFile() {
            CreateController.writeClassesFile(src, pkg);
            return this;
        }
    }


    /**
     *
     */
    public class GenerateService extends Add {
        private String pkg;

        public GenerateService(String pkg) {
            this.pkg = pkg;
        }

        public GenerateService writeServiceFile() {
            List<CompilationUnit> units = JpaSpringService.readRepositoryCreateService(src, pkg);
            FileHelper.writeJavaFile(src, units);
            return this;
        }
    }

    /**
     *
     */
    public class Add {
        public GenerateBuilder and() {
            return GenerateBuilder.this;
        }
    }

    /**
     *
     */
    public class GenerateRepository extends Add {
        private String pkg;

        public GenerateRepository(String pkg) {
            this.pkg = pkg;
        }

        public GenerateRepository writeRepositoryFile() {
            List<CompilationUnit> units = JpaSpringRepository.readJavaEntity2repository(src, pkg);
            FileHelper.writeJavaFile(src, units);
            return this;
        }
    }

    /**
     *
     */
    public class GenerateEntity extends Add {

        private final File src;

        public GenerateEntity(File src) {
            this.src = src;
        }

        public GenerateEntity writeEntityIdGeneratedValue() {
            JpaEntity.writeEntityIdGeneratedValue(this.src);
            return this;
        }

        public GenerateEntity writerEntitySetToList() {
            JpaEntity.writerEntitySetToList(this.src);
            return this;
        }

        public JpaEntity getEntities() {
            return jpaEntity;
        }
    }

}
