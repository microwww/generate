package com.github.microwww.generate;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import com.github.microwww.generate.util.FileHelper;
import com.github.microwww.generate.util.ParserHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

import static com.github.javaparser.ast.Modifier.Keyword.ABSTRACT;
import static com.github.javaparser.ast.Modifier.Keyword.PUBLIC;

public class JpaSpringRepository {

    private static final Logger logger = LoggerFactory.getLogger(JpaSpringRepository.class);

    public static List<CompilationUnit> readJavaEntity2repository(File src, final String repositoryPackage) {
        return FileHelper.scanJavaFile(src, (f) -> {
            try {
                return createRepository(f, repositoryPackage, findById, findAll);
            } catch (FileNotFoundException e) {
                logger.warn("Create error ! File : {}", f.getAbsolutePath(), e);
                throw new RuntimeException(e);
            }
        });
    }

    public static Optional<CompilationUnit> createRepository(File entityJavaFile, String repositoryPackage) throws FileNotFoundException {
        return createRepository(entityJavaFile, repositoryPackage, findById, findAll);
    }

    public static Optional<CompilationUnit> createRepository(File entityJavaFile, String repositoryPackage, BiConsumer<ClassOrInterfaceDeclaration, ClassOrInterfaceType>... success) throws FileNotFoundException {
        CompilationUnit unit = new CompilationUnit(repositoryPackage);
        CompilationUnit parse = StaticJavaParser.parse(entityJavaFile);

        ParserHelper.findTypeByAnnotation(parse, "Entity").ifPresent(type -> {
            String sname = type.getNameAsString();
            ParserHelper.findFieldByAnnotation(type, "Id").ifPresent(field -> {
                ClassOrInterfaceDeclaration clazz = unit.addInterface(sname + "Repository", PUBLIC);

                NodeList<ClassOrInterfaceType> types = new NodeList<>();
                ClassOrInterfaceType entity = new ClassOrInterfaceType();
                entity.setName(sname);

                String pk = parse.getPackageDeclaration().map(o -> o.getNameAsString() + "." + sname).orElse(sname);
                unit.addImport(pk);
                ClassOrInterfaceType ext = new ClassOrInterfaceType();
                ext.setName("Repository");
                unit.addImport("org.springframework.data.repository.Repository");

                Type idType = ParserHelper.findFieldByAnnotation(type, "Id").map(o -> o.getVariable(0)).get().getType();
                if (idType instanceof PrimitiveType) {
                    ext.setTypeArguments(entity, ((PrimitiveType) idType).toBoxedType());
                } else {
                    ext.setTypeArguments(entity, idType);
                }
                types.add(ext);

                clazz.setExtendedTypes(types);
                // public interface Entity?Repository extends Repository<Entity?, Entity?.Id?>

                if (success != null) {
                    for (BiConsumer suc : success) {
                        suc.accept(clazz, entity);
                    }
                }

                unit.getImports().sort(Comparator.comparing(NodeWithName::getNameAsString));
            });
        });
        if (unit.getTypes().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(unit);
    }

    public static final BiConsumer<ClassOrInterfaceDeclaration, ClassOrInterfaceType> findById = (clazz, entity) -> {
        MethodDeclaration find = clazz.addMethod("findById", PUBLIC, ABSTRACT);
        find.addAndGetParameter("int", "id");
        find.setType(entity);
        find.setBody(null);
    };

    public static final BiConsumer<ClassOrInterfaceDeclaration, ClassOrInterfaceType> findAll = (clazz, entity) -> {
        MethodDeclaration find = clazz.addMethod("findAll", PUBLIC, ABSTRACT);
        find.addAndGetParameter("Pageable", "page");
        CompilationUnit unit = ParserHelper.getRootNode(clazz).get();
        unit.addImport("org.springframework.data.domain.Pageable");
        ClassOrInterfaceType tr = new ClassOrInterfaceType();
        //clazz.addImport ("?.?.?.Entity");
        tr.setName("Page").setTypeArguments(entity);
        find.setType(tr);
        unit.addImport("org.springframework.data.domain.Page");
        find.setBody(null);
    };

}
