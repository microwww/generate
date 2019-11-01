package com.github.microwww.generate;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.ast.stmt.BlockStmt;
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
import java.util.function.BiConsumer;

import static com.github.javaparser.ast.Modifier.Keyword.PRIVATE;
import static com.github.javaparser.ast.Modifier.Keyword.PUBLIC;

public class JpaSpringService {
    private static final Logger logger = LoggerFactory.getLogger(JpaSpringService.class);

    public static List<CompilationUnit> readRepositoryCreateService(File src, final String servicePackage) {
        return FileHelper.scanJavaFile(src, (f) -> {
            try {
                return createServiceByRepository(f, servicePackage, findById, findAll, save);
            } catch (FileNotFoundException e) {
                logger.warn("Create error ! File : {}", f.getAbsolutePath(), e);
                throw new RuntimeException(e);
            }
        });
    }

    public static Optional<CompilationUnit> createServiceByRepository(File repositoryJavaFile, String servicePackage, BiConsumer<FieldDeclaration, ClassOrInterfaceDeclaration>... consumer) throws FileNotFoundException {
        CompilationUnit unit = new CompilationUnit(servicePackage);
        CompilationUnit parse = StaticJavaParser.parse(repositoryJavaFile);

        ParserHelper.findTypeBySuperclass(parse, "JpaRepository").ifPresent(se -> {
            String name = se.getKey().getNameAsString();
            NodeList<Type> args = se.getValue().getTypeArguments().get();
            ClassOrInterfaceType entity = (ClassOrInterfaceType) args.get(0);

            parse.getPackageDeclaration().ifPresent(e -> {
                unit.addImport(e.getNameAsString() + "." + name);
            });

            String clz = ParserHelper.findImportClass(parse, entity.getNameAsString()).map(ImportDeclaration::getNameAsString)//
                    .orElse(parse.getPackageDeclaration().map(o -> o.getNameAsString() + "." + entity).orElse(""));

            //ClassOrInterfaceType id = (ClassOrInterfaceType) args.get(1); // ignore

            ClassOrInterfaceDeclaration clazz = unit.addClass(name.replace("Repository", "") + "Service", PUBLIC);
            clazz.addAnnotation("Service");
            unit.addImport("org.springframework.stereotype.Service");
            clazz.addAnnotation("Transactional");
            unit.addImport("org.springframework.transaction.annotation.Transactional");

            FieldDeclaration field = clazz.addField(name, ParserHelper.firstLower(name), PRIVATE);
            field.addAndGetAnnotation("Autowired");
            unit.addImport(clz);
            unit.addImport("org.springframework.beans.factory.annotation.Autowired");
            field.createGetter();
            field.createSetter();

            if (consumer != null) {
                for (BiConsumer<FieldDeclaration, ClassOrInterfaceDeclaration> c : consumer) {
                    c.accept(field, se.getKey());
                }
            }

            unit.getImports().sort(Comparator.comparing(NodeWithName::getNameAsString));
        });
        if (unit.getTypes().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(unit);
    }

    public static final BiConsumer<FieldDeclaration, ClassOrInterfaceDeclaration> findById = (field, repository) -> {
        List<MethodDeclaration> method = repository.getMethodsByName("findById");//.get(0);
        if (!method.isEmpty()) {
            ParserHelper.delegate(field, method.get(0));
        }
    };

    //    public Page<BscDicCodeType> findAll(int page, int size) {
    //        return this.bscDicCodeTypeRepository.findAll(PageRequest.of(page, size));
    //    }
    public static final BiConsumer<FieldDeclaration, ClassOrInterfaceDeclaration> findAll = (field, repository) -> {
        ClassOrInterfaceDeclaration cfc = (ClassOrInterfaceDeclaration) field.getParentNode().get();
        ClassOrInterfaceType type = (ClassOrInterfaceType) repository.getExtendedTypes(0).getTypeArguments().get().get(0);
        MethodDeclaration mth = JpaSpringRepository.findAll.apply(cfc, type);
        mth.setBody(new BlockStmt());
        Parameter page = new Parameter(PrimitiveType.intType(), "page");
        Parameter size = new Parameter(PrimitiveType.intType(), "size");
        mth.setParameters(new NodeList<>(page, size));

        Expression param = new MethodCallExpr(
                new TypeExpr(new ClassOrInterfaceType().setName("PageRequest")),
                "of",
                new NodeList<>(page.getNameAsExpression(), size.getNameAsExpression()));
        ParserHelper.getRootNode(field).get().addImport("org.springframework.data.domain.PageRequest");

        mth.getBody().get().addStatement(
                new ReturnStmt(
                        new MethodCallExpr(
                                new FieldAccessExpr(
                                        new ThisExpr(), field.getVariables().get(0).getNameAsString()),
                                mth.getName(),
                                new NodeList<>(param))));
    };

    public static final BiConsumer<FieldDeclaration, ClassOrInterfaceDeclaration> save = (field, repository) -> {
        ClassOrInterfaceType type = repository.getExtendedTypes().get(0);
        ClassOrInterfaceType entity = (ClassOrInterfaceType) type.getTypeArguments().get().get(0);
        ClassOrInterfaceDeclaration clazz = (ClassOrInterfaceDeclaration) field.getParentNode().get();
        MethodDeclaration save = clazz.addMethod("save", PUBLIC);
        save.setType(entity);
        Parameter parameter = save.addAndGetParameter(entity, "entity");
        BlockStmt stmt = save.getBody().get();
        stmt.addStatement(
                new ReturnStmt(
                        new MethodCallExpr(
                                new FieldAccessExpr(
                                        new ThisExpr(), field.getVariables().get(0).getNameAsString()),
                                save.getName(),
                                new NodeList<>(new NameExpr(parameter.getName())))));
    };
}
