package com.github.microwww.generate;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.microwww.generate.util.FileHelper;
import com.github.microwww.generate.util.ParserHelper;

import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CreateDtoDomain {

    public static CompilationUnit createBaseClass(String pkg) {
        CompilationUnit unit = new CompilationUnit(pkg);
        ClassOrInterfaceDeclaration clazz = unit.addClass("AbstractDomainValue", Modifier.Keyword.PUBLIC, Modifier.Keyword.ABSTRACT);
        TypeParameter classParam = new TypeParameter("T");
        clazz.addTypeParameter(classParam);

        FieldDeclaration field = clazz.addField(classParam, "domain", Modifier.Keyword.PROTECTED, Modifier.Keyword.FINAL);

        ConstructorDeclaration cstr = clazz.addConstructor(Modifier.Keyword.PUBLIC);
        Parameter domain = cstr.addAndGetParameter(classParam, "domain");
        BlockStmt body = cstr.getBody();
        body.addStatement(new AssignExpr(new FieldAccessExpr(new ThisExpr(), field.getVariable(0).getNameAsString()), new NameExpr(domain.getName()), AssignExpr.Operator.ASSIGN));

        MethodDeclaration method = clazz.addMethod("origin", Modifier.Keyword.PUBLIC);
        method.setType(classParam);
        method.getBody().get().addStatement(new ReturnStmt(new FieldAccessExpr(new ThisExpr(), field.getVariable(0).getNameAsString())));

        return unit;
    }

    public static List<CompilationUnit> createEntityDTO(File src, String pkg) {
        String simpleClassName = "Simple";
        String infoClassName = "Info";
        return FileHelper.scanJavaFile(src, file -> {
            try {
                CompilationUnit parse = StaticJavaParser.parse(file);
                final String name = file.getName().split("\\.")[0];
                Optional<Optional<CompilationUnit>> entitys = parse.getClassByName(name)
                        .map(clazz ->
                                clazz.getAnnotationByName("Entity").map(o -> {
                                    String cname = name + "Value";
                                    CompilationUnit unit = new CompilationUnit(pkg);

                                    // IMPORT
                                    ParserHelper.getRootNode(clazz).ifPresent(u -> {
                                        u.getPackageDeclaration().ifPresent(p -> {
                                            unit.addImport(new ImportDeclaration(p.getNameAsString(), false, true));
                                        });
                                    });
                                    unit.addImport("java.util.*");

                                    // CLASS
                                    ClassOrInterfaceDeclaration out = unit.addClass(cname, Modifier.Keyword.PUBLIC, Modifier.Keyword.ABSTRACT);

                                    // INNER CLASS
                                    ClassOrInterfaceDeclaration simple = parse.addClass(simpleClassName, Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC);
                                    ClassOrInterfaceType ext = simple.addExtendedType("AbstractDomainValue").getExtendedTypes(0);
                                    ext.setTypeArguments(new TypeParameter(name));
                                    out.addMember(simple);
                                    {// Constructor
                                        ConstructorDeclaration ctr = simple.addConstructor(Modifier.Keyword.PUBLIC);
                                        Parameter domain = ctr.addAndGetParameter(clazz.getNameAsString(), "domain");
                                        ctr.getBody().addStatement(new MethodCallExpr("super", new NameExpr(domain.getName())));
                                    }

                                    // INFO inner class
                                    ClassOrInterfaceDeclaration info = parse.addClass(infoClassName, Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC);
                                    info.addExtendedType(simple.getNameAsString());
                                    out.addMember(info);
                                    {// Constructor
                                        ConstructorDeclaration ctr = info.addConstructor(Modifier.Keyword.PUBLIC);
                                        Parameter domain = ctr.addAndGetParameter(clazz.getNameAsString(), "domain");
                                        ctr.getBody().addStatement(new MethodCallExpr("super", new NameExpr(domain.getName())));
                                    }

                                    // INNER CLASS delegate
                                    List<MethodDeclaration> methods = clazz.getMethods();
                                    for (MethodDeclaration mth : methods) {
                                        if (mth.getParameters().isEmpty()) {
                                            String mn = mth.getNameAsString();
                                            Pattern compile = Pattern.compile("get([A-Z].*)");
                                            Matcher mch = compile.matcher(mn);
                                            if (mch.matches() && mth.getParameters().isEmpty()) {
                                                String field = ParserHelper.firstLower(mch.group(1));
                                                clazz.getFieldByName(field).map(fd -> {
                                                    String[] ann = new String[]{OneToMany.class.getSimpleName(), ManyToMany.class.getSimpleName(), ManyToOne.class.getSimpleName()};
                                                    return fd.getAnnotationByName(OneToMany.class.getSimpleName()).map(f -> {
                                                        return Optional.of(delegateOne2many(info, new FieldAccessExpr(new SuperExpr(), "domain"), mth));
                                                    }).orElseGet(() -> {
                                                        return fd.getAnnotationByName(ManyToMany.class.getSimpleName()).map(f -> {
                                                            return Optional.of(delegateOne2many(info, new FieldAccessExpr(new SuperExpr(), "domain"), mth));
                                                        }).orElseGet(() -> {
                                                            return fd.getAnnotationByName(ManyToOne.class.getSimpleName()).map(f -> {
                                                                return Optional.of(delegateMany2one(info, new FieldAccessExpr(new SuperExpr(), "domain"), mth));
                                                            }).orElseGet(() -> {
                                                                return fd.getAnnotationByName(OneToOne.class.getSimpleName()).map(f -> {
                                                                    return Optional.of(delegateMany2one(info, new FieldAccessExpr(new SuperExpr(), "domain"), mth));
                                                                }).orElseGet(() -> {
                                                                    return Optional.empty();
                                                                });
                                                            });
                                                        });
                                                    });
                                                }).orElse(Optional.empty()).orElseGet(() -> {
                                                    return ParserHelper.delegate(simple, new FieldAccessExpr(new SuperExpr(), "domain"), mth);
                                                });
                                            }
                                        }
                                    }
                                    return unit;
                                }));
                return entitys.orElse(Optional.empty());
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static MethodDeclaration delegateMany2one(ClassOrInterfaceDeclaration toClazz, FieldAccessExpr fieldAccessExpr, MethodDeclaration method) {
        MethodDeclaration md = ParserHelper.delegate(toClazz, fieldAccessExpr, method);
        if (md.getType() instanceof ClassOrInterfaceType) {
            ClassOrInterfaceType change = (ClassOrInterfaceType) md.getType();
            // public GoTeamValue.Simple getGoTeam() {
            String name = change.getNameAsString();
            change.setName(name + "Value.Simple");

            // return new GoTeamValue.Simple(super.domain.getGoTeam());
            BlockStmt stmt = md.getBody().get().setStatements(new NodeList<>());
            stmt.addStatement(new ReturnStmt(new ObjectCreationExpr(
                    null, change, new NodeList<>(new MethodCallExpr(new FieldAccessExpr(
                            new SuperExpr(), "domain"), method.getName())))));

        }

        return md;
    }

    public static MethodDeclaration delegateOne2many(ClassOrInterfaceDeclaration toClazz, FieldAccessExpr fieldAccessExpr, MethodDeclaration method) {
        MethodDeclaration md = ParserHelper.delegate(toClazz, fieldAccessExpr, method);
        if (md.getType() instanceof ClassOrInterfaceType) {
            ClassOrInterfaceType type = (ClassOrInterfaceType) md.getType();
            type.getTypeArguments().ifPresent(f -> {
                if (f.size() == 1) {
                    ClassOrInterfaceType origin = type.clone();
                    ClassOrInterfaceType change = f.get(0).asClassOrInterfaceType();
                    String name = change.getNameAsString();
                    change.setName(name + "Value.Simple");

                    BlockStmt stmt = md.getBody().get().setStatements(new NodeList<>());
                    // List<GoPlayer> list = super.domain.getGoPlayers();
                    stmt.addStatement(new AssignExpr(
                            new VariableDeclarationExpr(origin, "list"),
                            new MethodCallExpr(new FieldAccessExpr(new SuperExpr(), fieldAccessExpr.getNameAsString()), method.getName()),
                            AssignExpr.Operator.ASSIGN));
                    // return list.stream().map(GoPlayerValue.Simple::new).collect(Collectors.toList());
                    Class impt = Collectors.class;
                    ParserHelper.getRootNode(toClazz).ifPresent( ff ->{
                        ff.addImport(impt);
                    });
                    stmt.addStatement(new ReturnStmt(
                            new MethodCallExpr(new MethodCallExpr(new MethodCallExpr(new NameExpr("list"),
                                    "stream"),
                                    "map", new NodeList<>(new MethodReferenceExpr(new NameExpr(change.getNameAsString()), new NodeList<>(), "new"))),
                                    "collect", new NodeList<>(new MethodCallExpr(
                                    new NameExpr(impt.getSimpleName()),
                                    "toList")))
                    ));
                }
            });
        }

        return md;
    }
}
