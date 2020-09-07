package com.github.microwww.generate;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import com.github.microwww.generate.util.FileHelper;
import com.github.microwww.generate.util.ParserHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import javax.persistence.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class CreateDomainForm {
    public static final Logger logger = LoggerFactory.getLogger(CreateDomainForm.class);

    public static void createIdClass(File target, String pkg) {
        CompilationUnit unit = new CompilationUnit(pkg);
        TypeDeclaration<ClassOrInterfaceDeclaration> dec = new ClassOrInterfaceDeclaration(new NodeList<>(Modifier.createModifierList(Modifier.Keyword.PUBLIC)), false, "ID");
        unit.addType(dec);
        FileHelper.writeJavaFile(target, Collections.singletonList(unit));
    }

    public static List<CompilationUnit> refExtendsID(File src) {
        return FileHelper.scanJavaFile(src, file -> {
            try {
                CompilationUnit parse = StaticJavaParser.parse(file);
                AtomicBoolean exist = new AtomicBoolean();
                for (TypeDeclaration<?> type : parse.getTypes()) {
                    if (type.isClassOrInterfaceDeclaration()) {
                        type.getMembers().stream().filter(e -> e.isClassOrInterfaceDeclaration()).filter(ee -> {
                            return "Ref".equals(ee.asClassOrInterfaceDeclaration().getNameAsString());
                        }).findAny().ifPresent(e -> {
                            e.asClassOrInterfaceDeclaration().setExtendedTypes(new NodeList<>(StaticJavaParser.parseClassOrInterfaceType("ID")));
                            exist.set(true);
                        });
                    }
                }
                if (exist.get()) {
                    return Optional.of(parse);
                }
                return Optional.empty();
            } catch (FileNotFoundException e) {
            }
            return Optional.empty();
        });
    }

    public static void createJavaFile(File src, File target, String pkg) throws IOException {
        Assert.isTrue(src.isDirectory(), "Must is directory : " + src.getAbsolutePath());
        List<CompilationUnit> form = createEntityForm(src, pkg);
        for (CompilationUnit e : form) {
            String pack = e.getPackageDeclaration().map(PackageDeclaration::getNameAsString).orElse(pkg);
            if (e.getTypes().isEmpty()) {
                continue;
            }
            File file = new File(new File(target, pack.replace('.', File.separatorChar)), e.getType(0).getNameAsString() + ".java");
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            try (
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))
            ) {
                writer.write(e.toString());
            }
        }
    }

    public static List<CompilationUnit> createEntityForm(File src, String pkg) {
        return FileHelper.scanJavaFile(src, file -> {
            try {
                CompilationUnit parse = StaticJavaParser.parse(file);
                //PackageDeclaration pd = parse.getPackageDeclaration().get();
                ClassOrInterfaceDeclaration clz = parse.getPrimaryType().get().asClassOrInterfaceDeclaration();

                Optional<AnnotationExpr> find = clz.getAnnotations().stream().filter(e -> {
                    return Entity.class.getSimpleName().equalsIgnoreCase(e.getNameAsString());
                }).findAny();
                if (!find.isPresent()) {
                    return Optional.empty();
                }

                CompilationUnit unit = new CompilationUnit(pkg);
                ClassOrInterfaceDeclaration out = unit.addClass(clz.getNameAsString() + "Form", Modifier.Keyword.PUBLIC);
                ClassOrInterfaceDeclaration ref = new ClassOrInterfaceDeclaration(Modifier.createModifierList(Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC), false, "Ref");
                out.addMember(ref); // INNER CLASS
                ClassOrInterfaceDeclaration nc = new ClassOrInterfaceDeclaration(Modifier.createModifierList(Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC), false, "Form");
                ClassOrInterfaceType etn = StaticJavaParser.parseClassOrInterfaceType(ref.getNameAsString());
                nc.setExtendedTypes(new NodeList<>(etn));
                out.addMember(nc); // INNER CLASS

                List<FieldDeclaration> fields = clz.getFields();
                List<FieldDeclaration> list = new ArrayList<>();
                List<FieldDeclaration> ids = new ArrayList<>();
                for (FieldDeclaration f : fields) {
                    for (AnnotationExpr an : f.getAnnotations()) {
                        String ann = an.getNameAsString();
                        if (Id.class.getSimpleName().equals(ann)) {
                            ids.add(f);
                            break;
                        } else if (ManyToOne.class.getSimpleName().equals(ann)) {
                            ClassOrInterfaceType type = f.getElementType().asClassOrInterfaceType();
                            // Optional<ImportDeclaration> imp = ParserHelper.findImportClassFromClassPath(file, parse, type);
                            ClassOrInterfaceType id = StaticJavaParser.parseClassOrInterfaceType(type.getNameAsString() + "Form.Ref");
                            FieldDeclaration ff = new FieldDeclaration(Modifier.createModifierList(Modifier.Keyword.PRIVATE), id, f.getVariable(0).getNameAsString());
                            list.add(ff);
                            break;
                        } else if (Basic.class.getSimpleName().equals(ann)) {
                            list.add(f);
                            break;
                        }
                    }
                }

                copy(parse, ids, unit, ref);
                copy(parse, list, unit, nc);

                return Optional.of(unit);
            } catch (Exception e) {
                try {
                    logger.warn("Parse java error : ", file.getCanonicalPath(), e);
                } catch (IOException ex) {
                }
            }
            return Optional.empty();
        });
    }

    public static void copy(CompilationUnit src, List<FieldDeclaration> fs, CompilationUnit target, ClassOrInterfaceDeclaration nc) {
        fs.stream().map(f -> { //
            Type type = f.getCommonType();
            if (type.isPrimitiveType()) {
                type = ((PrimitiveType) type).toBoxedType();
            }
            return nc.addField(type, f.getVariable(0).getNameAsString(), Modifier.Keyword.PRIVATE);
        }).collect(Collectors.toList()).forEach(ff -> {
            ff.createGetter();
            ff.createSetter();
            List<Optional<ImportDeclaration>> clz = ParserHelper.findImportClass(src, ff.getCommonType());
            clz.stream().filter(Optional::isPresent).map(Optional::get).forEach(e -> target.addImport(e));
        });
    }
}
