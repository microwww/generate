package com.github.microwww.generate.util;

import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.javaparser.ast.Modifier.Keyword.PUBLIC;

public class ParserHelper {

    public static Optional<FieldDeclaration> findFieldByAnnotation(TypeDeclaration<?> ty, String name) {
        for (FieldDeclaration f : ty.getFields()) {
            for (AnnotationExpr an : f.getAnnotations()) {
                if (an.getNameAsString().equals(name)) {
                    return Optional.of(f);
                }
            }
        }
        return Optional.empty();
    }

    public static Optional<TypeDeclaration<?>> findTypeByAnnotation(CompilationUnit parse, String annotation) {
        for (TypeDeclaration<?> ty : parse.getTypes()) {
            String clazz = ty.getAnnotationByName(annotation).map(NodeWithName::getNameAsString).orElse(null);
            if (clazz != null) {
                return Optional.of(ty);
            }
        }
        return Optional.empty();
    }

    public static Optional<Map.Entry<ClassOrInterfaceDeclaration, ClassOrInterfaceType>> findTypeBySuperclass(CompilationUnit parse, String classOrInterface) {
        for (TypeDeclaration<?> ty : parse.getTypes()) {
            if (ty instanceof ClassOrInterfaceDeclaration) {
                ClassOrInterfaceDeclaration type = (ClassOrInterfaceDeclaration) ty;
                for (ClassOrInterfaceType clazz : type.getExtendedTypes()) {
                    if (clazz.getNameAsString().equals(classOrInterface)) {
                        return Optional.of(new AbstractMap.SimpleEntry<>(type, clazz));
                    }
                }
                for (ClassOrInterfaceType clazz : type.getImplementedTypes()) {
                    if (clazz.getNameAsString().equals(classOrInterface)) {
                        return Optional.of(new AbstractMap.SimpleEntry<>(type, clazz));
                    }
                }
            }
        }
        return Optional.empty();
    }

    public static List<ImportDeclaration> findImportClass(CompilationUnit parse, Type... type) {
        return findImportClass(parse, Arrays.stream(type));
    }

    public static List<ImportDeclaration> findImportClass(CompilationUnit parse, Collection<Parameter> parameters) {
        return findImportClass(parse, parameters.stream().map(Parameter::getType));
    }

    private static List<ImportDeclaration> findImportClass(CompilationUnit parse, Stream<Type> type) {
        List<ImportDeclaration> list = new CopyOnWriteArrayList<>();
        type.forEach(ty -> {
            List<ImportDeclaration> os = findImportClass(parse, ty);
            list.addAll(os);
        });
        return list;
    }

    public static List<ImportDeclaration> findImportClass(CompilationUnit parse, Type type) {
        List<ImportDeclaration> list = new CopyOnWriteArrayList<>();
        if (type instanceof ClassOrInterfaceType) {
            ClassOrInterfaceType ci = (ClassOrInterfaceType) type;
            findImportClass(parse, ci.getNameAsString()).ifPresent(o -> {
                list.add(o);
            });
            ci.getTypeArguments().ifPresent(oo -> {
                oo.stream().forEach(e -> {
                    List<ImportDeclaration> os = findImportClass(parse, e); // 递归
                    list.addAll(os);
                });
            });
        }
        return list;
    }

    public static void delegate(FieldDeclaration field, MethodDeclaration method) {
        ClassOrInterfaceDeclaration toClazz = (ClassOrInterfaceDeclaration) field.getParentNode().get();
        MethodDeclaration md = toClazz.addMethod(method.getNameAsString(), PUBLIC);
        md.setParameters(method.getParameters());
        md.setType(method.getType());
        CompilationUnit from = getRootNode(method).get();
        CompilationUnit target = getRootNode(field).get();

        ParserHelper.findImportClass(from, md.getType()).stream().forEach(target::addImport);
        ParserHelper.findImportClass(from, method.getParameters()).forEach(target::addImport);

        BlockStmt stmt = md.getBody().get();
        FieldAccessExpr ex = new FieldAccessExpr(new ThisExpr(), field.getVariable(0).getNameAsString());
        List<NameExpr> collect = method.getParameters().stream().map(m -> new NameExpr(m.getNameAsString())).collect(Collectors.toList());
        NodeList<Expression> list = new NodeList<>();
        list.addAll(collect);
        stmt.addStatement(new ReturnStmt(new MethodCallExpr(ex, method.getNameAsString(), list)));
    }

    public static Optional<CompilationUnit> getRootNode(Node node) {
        if (node instanceof CompilationUnit) {
            return Optional.of((CompilationUnit) node);
        } else {
            Optional<Node> p = node.getParentNode();
            if (p.isPresent()) {
                return getRootNode(p.get());
            }
        }
        return Optional.empty();
    }

    public static Optional<ImportDeclaration> findImportClass(CompilationUnit parse, String classOrInterface) {
        for (ImportDeclaration impt : parse.getImports()) {
            if (impt.getNameAsString().endsWith("." + classOrInterface)) {
                return Optional.of(impt);
            }
        }
        return Optional.empty();
    }

    public static String firstLower(String fieldName) {
        return fieldName.substring(0, 1).toLowerCase() + fieldName.substring(1);
    }

    public static String firstUpper(String fieldName) {
        return fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
    }
}
