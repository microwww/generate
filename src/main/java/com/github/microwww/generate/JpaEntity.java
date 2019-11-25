package com.github.microwww.generate;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.microwww.generate.util.FileHelper;
import com.github.microwww.generate.util.ParserHelper;
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
import javax.persistence.OneToMany;
import javax.sql.DataSource;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class JpaEntity {

    private JDBCConfiguration conf;

    public JpaEntity(DataSource dataSource) {
        PersistenceProviderImpl provider = new PersistenceProviderImpl();
        PersistenceUnitInfoImpl unit = new PersistenceUnitInfoImpl();
        unit.setJtaDataSource(dataSource);
        unit.setPersistenceUnitName("default");
        EntityManagerFactoryImpl factory = (EntityManagerFactoryImpl) provider.createContainerEntityManagerFactory(unit, Collections.EMPTY_MAP);
        conf = (JDBCConfiguration) factory.getConfiguration();
        //db2entity(config, pkg);
    }

    public JpaEntity(String driver, String url, String username, String password) {
        conf = new JDBCConfigurationImpl();
        conf.setConnectionDriverName(driver);
        conf.setConnectionURL(url);
        conf.setConnectionUserName(username);
        conf.setConnectionPassword(password);
        conf.setSpecification("jpa");
        //db2entity(conf, pkg);
    }

    public JpaEntity createEntity(String srcDirectory, String pkg) {
        return this.createEntity(new File(srcDirectory), pkg);
    }

    public JpaEntity createEntity(File srcDirectory, String pkg) {
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
        return this;
    }

    public static List<File> writeEntityIdGeneratedValue(File srcDirectory) {
        return FileHelper.writeJavaFile(srcDirectory, entityIdGeneratedValue(srcDirectory));
    }

    public static List<CompilationUnit> entityIdGeneratedValue(File srcDirectory) {
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

    public static List<File> writerEntitySetToList(File srcDirectory) {
        return FileHelper.writeJavaFile(srcDirectory, entitySetToList(srcDirectory));
    }

    public static List<CompilationUnit> entitySetToList(File srcDirectory) {
        return FileHelper.scanJavaFile(srcDirectory, file -> {
            try {
                CompilationUnit parse = StaticJavaParser.parse(file);
                String name = file.getName().split("\\.")[0];
                AtomicBoolean exist = new AtomicBoolean();
                parse.getClassByName(name)
                        .ifPresent(clazz -> {
                            clazz.getAnnotationByName("Entity")
                                    .ifPresent(o -> {
                                        for (FieldDeclaration field : clazz.getFields()) {
                                            field.getAnnotationByClass(OneToMany.class).ifPresent(e -> {
                                                NormalAnnotationExpr ex = (NormalAnnotationExpr) e;
                                                ex.getPairs().stream().filter(f -> f.getNameAsString().equals("targetEntity")).findAny().ifPresent(val -> {
                                                    val.getValue().toClassExpr().ifPresent(expr -> {
                                                        ClassOrInterfaceType type = StaticJavaParser.parseClassOrInterfaceType(List.class.getSimpleName());
                                                        ClassOrInterfaceType ci = (ClassOrInterfaceType) expr.getType();
                                                        type.setTypeArguments(ci.removeScope());
                                                        field.setVariable(0, new VariableDeclarator(type, field.getVariable(0).getName()));
                                                        exist.getAndSet(true);

                                                        clazz.getMethodsByName("get" + ParserHelper.firstUpper(field.getVariable(0).getNameAsString())).forEach(m -> {
                                                            if (m.getParameters().isEmpty()) {
                                                                m.setType(type);
                                                            }
                                                        });
                                                        clazz.getMethodsByName("set" + ParserHelper.firstUpper(field.getVariable(0).getNameAsString())).forEach(m -> {
                                                            if (m.getParameters().size() == 1) {
                                                                m.setParameter(0, new Parameter(type, m.getParameter(0).getName()));
                                                            }
                                                        });
                                                    });
                                                });
                                            });
                                        }
                                    });
                        });
                if (!exist.get()) {
                    parse = null;
                }
                return Optional.ofNullable(parse);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
