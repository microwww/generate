package com.github.microwww.generate;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.ast.stmt.ReturnStmt;
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
import java.util.function.BiFunction;

import static com.github.javaparser.ast.Modifier.Keyword.DEFAULT;
import static com.github.javaparser.ast.Modifier.Keyword.PUBLIC;

public class JpaSpringRepository {

    private static final Logger logger = LoggerFactory.getLogger(JpaSpringRepository.class);

    public static List<CompilationUnit> readJavaEntity2repository(File src, final String repositoryPackage) {
        return FileHelper.scanJavaFile(src, (f) -> {
            try {
                return createRepository(f, repositoryPackage);
            } catch (FileNotFoundException e) {
                logger.warn("Create error ! File : {}", f.getAbsolutePath(), e);
                throw new RuntimeException(e);
            }
        });
    }

    public static Optional<CompilationUnit> createRepository(File entityJavaFile, String repositoryPackage) throws FileNotFoundException {
        return createRepository(entityJavaFile, repositoryPackage, findById);
    }

    public static Optional<CompilationUnit> createRepository(File entityJavaFile, String repositoryPackage, BiFunction<ClassOrInterfaceDeclaration, ClassOrInterfaceType, MethodDeclaration>... success) throws FileNotFoundException {
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
                ext.setName("JpaRepository");
                unit.addImport("org.springframework.data.jpa.repository.JpaRepository");

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
                    for (BiFunction<ClassOrInterfaceDeclaration, ClassOrInterfaceType, MethodDeclaration> suc : success) {
                        suc.apply(clazz, entity);
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

    /**
     * default Optional<GoConfig> findById(int integer) {
     * ____return this.findById(Integer.valueOf(integer));
     * }
     */
    public static final BiFunction<ClassOrInterfaceDeclaration, ClassOrInterfaceType, MethodDeclaration> findById = (clazz, entity) -> {
        MethodDeclaration find = clazz.addMethod("findById", PUBLIC);
        try {
            Type type = clazz.getExtendedTypes().get(0).getTypeArguments().get().get(1);
            if (type instanceof ClassOrInterfaceType) {
                ClassOrInterfaceType typ = ((ClassOrInterfaceType) type);
                if (typ.isBoxedType()) {
                    Parameter id = find.addAndGetParameter(typ.toUnboxedType(), "id");
                    find.addModifier(DEFAULT);
                    find.getBody().get();
                    ParserHelper.getRootNode(clazz).ifPresent(c -> {
                        c.addImport(Optional.class.getName());
                    });
                    ClassOrInterfaceType opt = new ClassOrInterfaceType().setName(Optional.class.getSimpleName());
                    opt.setTypeArguments(entity);
                    find.setType(opt);

                    Expression param = new MethodCallExpr(typ.getNameAsExpression(), "valueOf", new NodeList<>(id.getNameAsExpression()));
                    find.getBody().get().addStatement(new ReturnStmt(
                            new MethodCallExpr(
                                    new ThisExpr(),
                                    find.getName(),
                                    new NodeList<>(param))));
                } else {// IGNORE
                    return new MethodDeclaration();
                }
            }
        } catch (Exception e) {
            logger.warn("Not find @ID type ! Using Object .", e);
        }
        return find;
    };

    public static final BiFunction<ClassOrInterfaceDeclaration, ClassOrInterfaceType, MethodDeclaration> findAll = (clazz, entity) -> {
        MethodDeclaration find = clazz.addMethod("findAll", PUBLIC);
        find.addAndGetParameter("Pageable", "page");
        CompilationUnit unit = ParserHelper.getRootNode(clazz).get();
        unit.addImport("org.springframework.data.domain.Pageable");
        ClassOrInterfaceType tr = new ClassOrInterfaceType();
        //clazz.addImport ("?.?.?.Entity");
        tr.setName("Page").setTypeArguments(entity);
        find.setType(tr);
        unit.addImport("org.springframework.data.domain.Page");
        return find;
    };

}
