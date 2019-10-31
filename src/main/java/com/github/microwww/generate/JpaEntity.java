package com.github.microwww.generate;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.microwww.generate.util.FileHelper;
import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.conf.JDBCConfigurationImpl;
import org.apache.openjpa.jdbc.meta.ReverseMappingTool;
import org.apache.openjpa.lib.conf.Configurations;
import org.apache.openjpa.lib.util.Options;
import org.apache.openjpa.persistence.EntityManagerFactoryImpl;
import org.apache.openjpa.persistence.PersistenceProviderImpl;
import org.apache.openjpa.persistence.PersistenceUnitInfoImpl;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.sql.DataSource;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class JpaEntity {

    public File srcDirectory;
    public String pkg;

    public JpaEntity(String src, String pkg) {
        this(new File(src), pkg);
    }

    public JpaEntity(File src, String pkg) {
        this.srcDirectory = src;
        this.pkg = pkg;
    }

    public void db2entity(DataSource dataSource) {
        PersistenceProviderImpl provider = new PersistenceProviderImpl();
        PersistenceUnitInfoImpl unit = new PersistenceUnitInfoImpl();
        unit.setJtaDataSource(dataSource);
        unit.setPersistenceUnitName("default");
        EntityManagerFactoryImpl factory = (EntityManagerFactoryImpl) provider.createContainerEntityManagerFactory(unit, Collections.EMPTY_MAP);
        JDBCConfiguration config = (JDBCConfiguration) factory.getConfiguration();
        db2entity(config, pkg);
    }

    public void db2entity(String driver, String url, String username, String password) {
        JDBCConfiguration conf = new JDBCConfigurationImpl();
        conf.setConnectionDriverName(driver);
        conf.setConnectionURL(url);
        conf.setConnectionUserName(username);
        conf.setConnectionPassword(password);
        conf.setSpecification("jpa");
        db2entity(conf, pkg);
    }

    public void db2entity(JDBCConfiguration conf, String pkg) {
        String f = srcDirectory.getAbsolutePath();
        Options opts = new Options();
        String[] arguments = opts.setFromCmdLine(new String[]{
                "-pkg", pkg,
                "-d", f,
                "-annotations", "true",
                "-nullableAsObject", "true",
                "-md", "none"});
        Configurations.runAgainstAllAnchors(opts, (opts1) -> {
            try {
                return ReverseMappingTool.run(conf, arguments, opts1);
            } finally {
                conf.close();
            }
        });
    }

    public List<File> writeEntityIdGeneratedValue() {
        return FileHelper.writeJavaFile(srcDirectory, entityIdGeneratedValue());
    }

    public List<CompilationUnit> entityIdGeneratedValue() {
        return FileHelper.scanJavaFile(srcDirectory, file -> {
            try {
                CompilationUnit parse = StaticJavaParser.parse(file);
                String name = file.getName().split("\\.")[0];
                Optional<Optional<Optional<CompilationUnit>>> ooo = parse.getClassByName(name).map(clazz -> clazz.getAnnotationByName("Entity").map(o -> {
                    for (FieldDeclaration field : clazz.getFields()) {
                        // NOT need import
                        Optional<CompilationUnit> oo = field.getAnnotationByClass(Id.class).map(e -> {
                            if (field.getAnnotationByClass(GeneratedValue.class).isPresent()) {
                            } else {
                                NormalAnnotationExpr expr = field.addAndGetAnnotation(GeneratedValue.class);
                                expr.addPair("strategy", "GenerationType.IDENTITY");
                            }
                            return parse;
                        });
                        if (oo.isPresent()) {
                            return oo;
                        }
                    }
                    return Optional.empty();
                }));
                return ooo.orElse(Optional.empty()).orElse(Optional.empty());
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
