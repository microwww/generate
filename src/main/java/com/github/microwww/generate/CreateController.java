package com.github.microwww.generate;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.microwww.generate.util.FileHelper;
import com.github.microwww.generate.util.ParserHelper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CreateController {

    private final File src;
    private final ClassOrInterfaceDeclaration entity;
    private ClassOrInterfaceDeclaration _service;

    public CreateController(File src, ClassOrInterfaceDeclaration entity) {
        this.src = src;
        this.entity = entity;
    }

    public synchronized ClassOrInterfaceDeclaration getService() {
        if (_service == null) {
            _service = searchService().orElseGet(() -> {
                TypeDeclaration clazz = StaticJavaParser.parse("public class " + entity.getNameAsString() + "Service {}").getType(0);
                return (ClassOrInterfaceDeclaration) clazz;
            });
        }
        return _service;
    }

    public static List<File> writeClassesFile(File src, String pkg) {
        List<CompilationUnit> classes = createClasses(src, pkg);
        return FileHelper.writeJavaFile(src, classes);
    }

    public static List<CompilationUnit> createClasses(File src, String pkg) {
        List<CompilationUnit> units = FileHelper.scanJavaFile(src, f -> {
            try {
                CompilationUnit parse = StaticJavaParser.parse(f);
                return ParserHelper.findTypeByAnnotation(parse, "Entity").map(e -> {
                    CreateController create = new CreateController(src, (ClassOrInterfaceDeclaration) e);
                    CompilationUnit unit = create.createClass(pkg);
                    return unit;
                });
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
        return units;
    }

    public CompilationUnit createClass(String pkg) {
        CompilationUnit unit = new CompilationUnit(pkg);
        ClassOrInterfaceDeclaration clazz = unit.addClass(entity.getNameAsString() + "Controller", Modifier.Keyword.PUBLIC);
        clazz.addAndGetAnnotation(RestController.class);
        clazz.addAndGetAnnotation(RequestMapping.class).addPair("value", new StringLiteralExpr("/" + toURI(entity.getNameAsString())));

        ClassOrInterfaceDeclaration service = this.getService();
        unit.addImport(ParserHelper.findImportClass(service));

        FieldDeclaration field = clazz.addField(service.getNameAsString(), ParserHelper.firstLower(service.getNameAsString())).addAnnotation("Autowired");
        field.tryAddImportToParentCompilationUnit(Autowired.class);
        createListMethod(clazz, field);
        createDetailMethod(clazz, field);
        createSaveMethod(clazz, field);

        unit.addImport(ParserHelper.findImportClass(entity));
        unit.addImport(ParserHelper.findImportClass(service));
        unit.getImports().sort(Comparator.comparing(NodeWithName::getNameAsString));

        return unit;
    }

    /**
     * <pre>
     *     public void saveGoTeam(GoTeam pay) {
     *         Optional&lt;GoTeam&gt; db = goTeamService.findById(pay.getId());
     *         GoTeam entity = db.orElse(pay);
     *         BeanUtils.copyProperties(pay, entity, "id");
     *         goTeamService.save(entity);
     *     }
     *     </pre>
     */
    private void createSaveMethod(ClassOrInterfaceDeclaration clazz, FieldDeclaration field) {
        String param = "entity";
        MethodDeclaration method = clazz.addMethod("save" + entity.getNameAsString(), Modifier.Keyword.PUBLIC);
        method.addAndGetAnnotation(PostMapping.class).addPair("value", new StringLiteralExpr("/save"));
        method.addParameter(entity.getNameAsString(), param);
        Optional<FieldDeclaration> id = ParserHelper.findFieldByAnnotation(entity, "Id");

        method.getBody().get().addStatement(new AssignExpr(
                new VariableDeclarationExpr(
                        new ClassOrInterfaceType().setName(entity.getNameAsString()),
                        "db"),
                new MethodCallExpr(new MethodCallExpr(new FieldAccessExpr(new ThisExpr(),
                        field.getVariable(0).getNameAsString()),
                        "findById", new NodeList<>(new MethodCallExpr(
                        new NameExpr(param),
                        "get" + ParserHelper.firstUpper(id.get().getVariable(0).getNameAsString()))
                )), "orElse", new NodeList<>(new NameExpr(param))),
                AssignExpr.Operator.ASSIGN));

        method.tryAddImportToParentCompilationUnit(BeanUtils.class);

        method.getBody().get().addStatement(new MethodCallExpr(new NameExpr("BeanUtils"),
                "copyProperties", new NodeList<>(
                new NameExpr(param),
                new NameExpr("db"),
                new StringLiteralExpr(id.get().getVariable(0).getNameAsString()))));

        method.getBody().get().addStatement(new MethodCallExpr(new FieldAccessExpr(new ThisExpr(),
                field.getVariable(0).getNameAsString()),
                "save",
                new NodeList<>(new NameExpr("db"))));
    }

    /**
     * <pre>
     *     &#64;GetMapping("/detail")
     *     public GoShop detailGoShop(int id) {
     *         return goShopService.findById(id).orElseGet(null);
     *     }
     * </pre>
     *
     * @param clazz
     * @param field
     */
    private void createDetailMethod(ClassOrInterfaceDeclaration clazz, FieldDeclaration field) {
        String returnClass = entity.getNameAsString() + "Value.Info";
        MethodDeclaration method = clazz.addMethod("detail" + entity.getNameAsString(), Modifier.Keyword.PUBLIC);
        method.setType(returnClass).addAndGetAnnotation(GetMapping.class).addPair("value", new StringLiteralExpr("/detail"));

        searchDTO("Info").ifPresent(c -> {
            ParserHelper.getRootNode(clazz).ifPresent(o -> {
                o.addImport(ParserHelper.findImportClass(c));
            });
        });

        Optional<FieldDeclaration> id = ParserHelper.findFieldByAnnotation(entity, "Id");
        method.addAndGetParameter(id.get().getElementType(), "id");

        //return goTeamService.findById(id).orElseGet(null);
        method.getBody().get().addStatement(new ReturnStmt(
                new MethodCallExpr(new MethodCallExpr(new MethodCallExpr(
                        new FieldAccessExpr(new ThisExpr(), field.getVariables().get(0).getNameAsString()),
                        "findById", new NodeList<>(new NameExpr("id"))),
                        "map", new NodeList<>(new MethodReferenceExpr(
                        new NameExpr(returnClass), new NodeList<>(), "new"))),
                        "orElseGet", new NodeList<>(new NullLiteralExpr()))
        ));
    }

    /**
     * <pre>
     * &#64;GetMapping("/list")
     * public List &lt; GoTeamValue.Simple> listGoTeam(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
     *     return goTeamService.findAll(page, size).stream()
     *         .map(GoTeamValue.Simple::new)
     *         .collect(Collectors.toList());
     * }
     * </pre>
     *
     * @param clazz
     * @param field
     */
    private void createListMethod(ClassOrInterfaceDeclaration clazz, FieldDeclaration field) {
        MethodDeclaration method = clazz.addMethod("list" + entity.getNameAsString(), Modifier.Keyword.PUBLIC);
        method.addAndGetAnnotation(GetMapping.class).addPair("value", new StringLiteralExpr("/list"));
        method.setType(new ClassOrInterfaceType().setName("List").setTypeArguments(new ClassOrInterfaceType().setName(entity.getNameAsString() + "Value.Simple")));
        method.addAndGetParameter(PrimitiveType.intType(), "page").addAndGetAnnotation(RequestParam.class).addPair("defaultValue", new StringLiteralExpr("0"));
        method.addAndGetParameter(PrimitiveType.intType(), "size").addAndGetAnnotation(RequestParam.class).addPair("defaultValue", new StringLiteralExpr("10"));

        method.getBody().get().addStatement(new ReturnStmt(
                new MethodCallExpr(new MethodCallExpr(new MethodCallExpr(new MethodCallExpr(
                        new FieldAccessExpr(new ThisExpr(), field.getVariables().get(0).getNameAsString()),
                        "findAll", new NodeList<>(new NameExpr("page"), new NameExpr("size"))),
                        "stream"),
                        "map", new NodeList<>(new MethodReferenceExpr(new NameExpr(entity.getNameAsString() + "Value.Simple"), new NodeList<>(), "new"))),
                        "collect", new NodeList<>(new MethodCallExpr(new NameExpr("Collectors"), "toList")))
        ));

        method.tryAddImportToParentCompilationUnit(List.class);
        method.tryAddImportToParentCompilationUnit(Collectors.class);
    }

    public Optional<ClassOrInterfaceDeclaration> searchDTO(String innerClass) {
        String className = entity.getNameAsString() + "Value";
        Optional<CompilationUnit> clazz = this.searchClass(className);
        //clazz.get().get
        Optional<ClassOrInterfaceDeclaration> simple = clazz.map(m -> {
            for (TypeDeclaration<?> type : m.getTypes()) {
                for (BodyDeclaration<?> yp : type.getMembers()) {
                    if (yp.isClassOrInterfaceDeclaration()) {
                        ClassOrInterfaceDeclaration dec = (ClassOrInterfaceDeclaration) yp;
                        if (dec.getNameAsString().equals(innerClass)) {
                            return Optional.of(dec);
                        }
                    }
                }
            }
            return Optional.ofNullable((ClassOrInterfaceDeclaration) null);
        }).orElse(Optional.empty());
        if (simple.isPresent()) {
            return clazz.map(o -> o.getClassByName(className)).orElse(Optional.empty());
        }
        return Optional.empty();
    }

    public Optional<ClassOrInterfaceDeclaration> searchService() {
        String className = entity.getNameAsString() + "Service";
        return this.searchClass(className).map(m -> {
            return m.getClassByName(className);
        }).orElse(Optional.empty());
    }

    public Optional<CompilationUnit> searchClass(String className) {
        //String className = entity.getNameAsString() + "Service";
        String fileName = className + ".java";
        List<File> files = FileHelper.scanJavaFile(src, (f) -> {
            if (f.getName().equalsIgnoreCase(fileName)) {
                return Optional.of(f);
            }
            return Optional.empty();
        });
        if (!files.isEmpty()) {
            try {
                return Optional.of(StaticJavaParser.parse(files.get(0)));
            } catch (FileNotFoundException e) {
            }
        }
        return Optional.empty();
    }

    public static String toURI(String name) {
        return ParserHelper.firstLower(name).replaceAll("([A-Z])", "-$1").toLowerCase();
    }

}
