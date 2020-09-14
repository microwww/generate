package spring.data.jpa.generate;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.TypeParameter;
import spring.data.jpa.generate.util.ParserHelper;

import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ViewDomain extends Clazz {

    private JpaEntity jpaEntity;

    public ViewDomain(CompilationUnit unit, ClassOrInterfaceDeclaration clazz) {
        super(unit, clazz);
    }

    public static Clazz createBaseClass(String pkg) {
        return createBaseClass(pkg, "AbstractDomainValue");
    }

    public static Clazz createBaseClass(String pkg, String name) {
        CompilationUnit unit = new CompilationUnit(pkg);
        ClassOrInterfaceDeclaration clazz = unit.addClass(name, Modifier.Keyword.PUBLIC, Modifier.Keyword.ABSTRACT);
        TypeParameter classParam = new TypeParameter("T");
        clazz.addTypeParameter(classParam);

        FieldDeclaration field = clazz.addField(classParam, "domain", Modifier.Keyword.PROTECTED, Modifier.Keyword.FINAL);

        ConstructorDeclaration cstr = clazz.addConstructor(Modifier.Keyword.PUBLIC);
        Parameter domain = cstr.addAndGetParameter(classParam, "domain");
        BlockStmt body = cstr.getBody();
        body.addStatement(new AssignExpr(new FieldAccessExpr(new ThisExpr(), field.getVariable(0).getNameAsString()), new NameExpr(domain.getName()), AssignExpr.Operator.ASSIGN));

        createGetOriginMethod(clazz, classParam, field);
        createConversionMethod(clazz);

        Clazz cz = new Clazz(unit, clazz) {
        };
        return cz;
    }

    private static void createGetOriginMethod(ClassOrInterfaceDeclaration clazz, TypeParameter classParam, FieldDeclaration field) {
        MethodDeclaration method = clazz.addMethod("origin", Modifier.Keyword.PUBLIC);
        method.setType(classParam);
        method.getBody().get().addStatement(new ReturnStmt(new FieldAccessExpr(new ThisExpr(), field.getVariable(0).getNameAsString())));
    }

    /**
     * <pre>
     *     public static &lt; T, V extends AbstractDomainValue &lt; T &gt; &gt; List &lt; V &gt;  conversion(Class &lt; V &gt;  clazz, Collection &lt; T &gt;  list) {
     *         return page.stream().map((T m) - &gt;  {
     *             try {
     *                 return clazz.getConstructor(new Class[] { m.getClass() }).newInstance(m);
     *             } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
     *                 throw new UnsupportedOperationException("Need one Constructor with one  &lt; T &gt;  param", e);
     *             }
     *         });
     *     }
     * </pre>
     **/
    private static void createConversionMethod(ClassOrInterfaceDeclaration clazz) {
        // public static <T, V extends AbstractDomainValue<T &gt;  &gt;  List<V &gt;  conversion(Class<V &gt;  clazz, Collection<T &gt;  list) {
        MethodDeclaration method = clazz.addMethod("conversion", Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC);
        method.setType(new ClassOrInterfaceType().setName("List").setTypeArguments(new ClassOrInterfaceType().setName("V")));
        ClassOrInterfaceType ex = StaticJavaParser.parseClassOrInterfaceType("AbstractDomainValue");
        ex.setTypeArguments(new TypeParameter("T"));
        method.setTypeParameters(new NodeList<>(
                new TypeParameter("T"),
                new TypeParameter("V",
                        new NodeList(ex))));
        String parma1 = "clazz";
        String parma2 = "list";
        method.addParameter(
                new ClassOrInterfaceType()
                        .setName("Class")
                        .setTypeArguments(new TypeParameter("V")),
                parma1);
        method.addParameter(new ClassOrInterfaceType()
                        .setName("Collection")
                        .setTypeArguments(new TypeParameter("T")),
                parma2);

        method.tryAddImportToParentCompilationUnit(Collection.class);
        method.tryAddImportToParentCompilationUnit(List.class);
        method.tryAddImportToParentCompilationUnit(InstantiationException.class);
        method.tryAddImportToParentCompilationUnit(InvocationTargetException.class);
        method.tryAddImportToParentCompilationUnit(Collectors.class);

        String lambParam = "m";
        //page.stream().map(m - &gt;  {
        method.getBody().get().addStatement(new ReturnStmt( // return
                new MethodCallExpr(new MethodCallExpr(new MethodCallExpr(new NameExpr(parma2), "stream"), // page.stream(..)
                        "map", new NodeList<>( // map( ... )
                        new LambdaExpr(
                                new NodeList(new Parameter(new ClassOrInterfaceType().setName("T"), lambParam)),
                                new BlockStmt().addStatement(
                                        new TryStmt(
                                                new BlockStmt().addStatement(new ReturnStmt(//return clazz.getConstructor(new Class[]{m.getClass()}).newInstance(m);
                                                        new MethodCallExpr(
                                                                new MethodCallExpr(new NameExpr(parma1), "getConstructor", //clazz.getConstructor(....)
                                                                        new NodeList<>(new ArrayCreationExpr(new ClassOrInterfaceType().setName("Class"), // new Class
                                                                                new NodeList<>(new ArrayCreationLevel()), // []
                                                                                new ArrayInitializerExpr(new NodeList<>( // { ...
                                                                                        new MethodCallExpr(new NameExpr(lambParam), "getClass") // m.getClass()
                                                                                )) // }
                                                                        ))
                                                                ), // new Class[]{m.getClass()}
                                                                "newInstance", new NodeList<>(new NameExpr(lambParam))) // .newInstance(m)
                                                )),
                                                new NodeList<>(new CatchClause(new Parameter(new ClassOrInterfaceType().setName("NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException"),
                                                        "e"),
                                                        new BlockStmt().addStatement(new ThrowStmt(new ObjectCreationExpr()
                                                                .setType("UnsupportedOperationException").setArguments(new NodeList<>(
                                                                        new StringLiteralExpr("Need one Constructor with one <T> param"), new NameExpr("e"))))))),
                                                null
                                        )
                                )
                        ))
                ), "collect", new NodeList<>(new MethodCallExpr(new NameExpr("Collectors"), "toList")))
        ));
    }

    public static ViewDomain createEntityDTO(JpaEntity jpaEntity, ClassOrInterfaceType extend, String pkg) {
        String simpleClassName = "Simple";
        String infoClassName = "Info";
        String moreClassName = "More";

        ClassOrInterfaceDeclaration entity = jpaEntity.getClazz();
        CompilationUnit parse = jpaEntity.getUnit();
        CompilationUnit unit = new CompilationUnit(pkg);

        // IMPORT
        ParserHelper.getRootNode(entity).ifPresent(u -> {
            u.getPackageDeclaration().ifPresent(p -> {
                unit.addImport(new ImportDeclaration(p.getNameAsString(), false, true));
            });
        });
        unit.addImport("java.util.*");

        // CLASS
        ClassOrInterfaceDeclaration out = unit.addClass(entity.getNameAsString() + "Value", Modifier.Keyword.PUBLIC, Modifier.Keyword.ABSTRACT);

        // INNER CLASS
        ClassOrInterfaceDeclaration simple = parse.addClass(simpleClassName, Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC);
        ClassOrInterfaceType ext = simple.addExtendedType(extend).getExtendedTypes(0);
        ext.setTypeArguments(new TypeParameter(entity.getNameAsString()));
        out.addMember(simple);
        {// Constructor
            ConstructorDeclaration ctr = simple.addConstructor(Modifier.Keyword.PUBLIC);
            Parameter domain = ctr.addAndGetParameter(entity.getNameAsString(), "domain");
            ctr.getBody().addStatement(new MethodCallExpr("super", new NameExpr(domain.getName())));
        }

        // INFO inner class
        ClassOrInterfaceDeclaration info = parse.addClass(infoClassName, Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC);
        info.addExtendedType(simple.getNameAsString());
        out.addMember(info);
        {// Constructor
            ConstructorDeclaration ctr = info.addConstructor(Modifier.Keyword.PUBLIC);
            Parameter domain = ctr.addAndGetParameter(entity.getNameAsString(), "domain");
            ctr.getBody().addStatement(new MethodCallExpr("super", new NameExpr(domain.getName())));
        }

        // MORE inner class
        ClassOrInterfaceDeclaration more = parse.addClass(moreClassName, Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC);
        more.addExtendedType(info.getNameAsString());
        out.addMember(more);
        {// Constructor
            ConstructorDeclaration ctr = more.addConstructor(Modifier.Keyword.PUBLIC);
            Parameter domain = ctr.addAndGetParameter(entity.getNameAsString(), "domain");
            ctr.getBody().addStatement(new MethodCallExpr("super", new NameExpr(domain.getName())));
        }

        // INNER CLASS delegate
        List<MethodDeclaration> methods = entity.getMethods();
        for (MethodDeclaration mth : methods) {
            if (mth.getParameters().isEmpty()) {
                String mn = mth.getNameAsString();
                Pattern compile = Pattern.compile("get([A-Z].*)");
                Matcher mch = compile.matcher(mn);
                if (mch.matches() && mth.getParameters().isEmpty()) {
                    String field = ParserHelper.firstLower(mch.group(1));
                    entity.getFieldByName(field).map(fd -> {
                        String[] ann = new String[]{OneToMany.class.getSimpleName(), ManyToMany.class.getSimpleName(), ManyToOne.class.getSimpleName()};
                        return fd.getAnnotationByName(OneToMany.class.getSimpleName()).map(f -> {
                            return Optional.of(delegateOne2many(more, new FieldAccessExpr(new SuperExpr(), "domain"), mth));
                        }).orElseGet(() -> {
                            return fd.getAnnotationByName(ManyToMany.class.getSimpleName()).map(f -> {
                                return Optional.of(delegateOne2many(more, new FieldAccessExpr(new SuperExpr(), "domain"), mth));
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

        ViewDomain vdo = new ViewDomain(unit, out);
        vdo.jpaEntity = jpaEntity;
        return vdo;
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
                    ParserHelper.getRootNode(toClazz).ifPresent(ff -> {
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

    public JpaEntity getJpaEntity() {
        return jpaEntity;
    }

    public void setJpaEntity(JpaEntity jpaEntity) {
        this.jpaEntity = jpaEntity;
    }
}
