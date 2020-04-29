package com.github.microwww.generate;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.microwww.generate.util.FileHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

public class DeleteMethod {

    public static List<CompilationUnit> deleteMethod(File srcDirectory, final BiPredicate<ClassOrInterfaceDeclaration, MethodDeclaration> func) {
        return FileHelper.scanJavaFile(srcDirectory, file -> {
            try {
                CompilationUnit parse = StaticJavaParser.parse(file);
                NodeList<TypeDeclaration<?>> types = parse.getTypes();
                for (TypeDeclaration type : types) {
                    if (type instanceof ClassOrInterfaceDeclaration) {
                        ClassOrInterfaceDeclaration clazz = type.asClassOrInterfaceDeclaration();
                        List<MethodDeclaration> method = clazz.getMethods().stream().filter((e) -> {//
                            return func.test(clazz, e);
                        }).collect(Collectors.toList());
                        method.forEach(e -> {
                            clazz.remove(e);
                        });
                    }
                }
                return Optional.ofNullable(parse);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
