package spring.data.jpa.generate;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import spring.data.jpa.generate.util.ParserHelper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SpringController extends Clazz {

    private SpringService springService;
    private FieldDeclaration service;
    private ViewDomain view;

    public SpringController(CompilationUnit unit, ClassOrInterfaceDeclaration clazz) {
        super(unit, clazz);
    }

    public static SpringController createClass(SpringService service, String pkg, ViewDomain view) {
        Assert.isTrue(view.getJpaEntity().getClazz().getNameAsString().equals(service.getEntity().getNameAsString()), "Entity must equal !");
        CompilationUnit unit = new CompilationUnit(pkg);
        ClassOrInterfaceDeclaration entity = service.getSpringRepository().getJpaEntity().getClazz();
        ClassOrInterfaceDeclaration clazz = unit.addClass(entity.getNameAsString() + "Controller", Modifier.Keyword.PUBLIC);
        clazz.addAndGetAnnotation(RestController.class);
        clazz.addAndGetAnnotation(RequestMapping.class).addPair("value", new StringLiteralExpr("/" + toURI(entity.getNameAsString())));

        unit.addImport(ParserHelper.findImportClass(service.getClazz()));

        String fieldName = service.getClazz().getNameAsString();
        FieldDeclaration field = clazz.addField(fieldName, ParserHelper.firstLower(fieldName)).addAnnotation("Autowired");
        field.tryAddImportToParentCompilationUnit(Autowired.class);

        unit.addImport(ParserHelper.findImportClass(entity));
        unit.addImport(ParserHelper.findImportClass(service.getClazz()));
        unit.getImports().sort(Comparator.comparing(NodeWithName::getNameAsString));

        SpringController con = new SpringController(unit, clazz);
        con.service = field;
        con.springService = service;
        con.view = view;

        return con;
    }

    private void createSaveMethod() {
        ClassOrInterfaceDeclaration clazz = this.getClazz();
        ClassOrInterfaceDeclaration entity = this.springService.getEntity();
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
                        service.getVariable(0).getNameAsString()),
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
                service.getVariable(0).getNameAsString()),
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
     */
    public void addDetailMethod() {
        ClassOrInterfaceDeclaration clazz = this.getClazz();
        ClassOrInterfaceDeclaration entity = this.springService.getEntity();
        // String returnClass = entity.getNameAsString() + "Value.Info";
        String returnClass = view.getClazz().getNameAsString() + ".Info";
        MethodDeclaration method = clazz.addMethod("detail" + entity.getNameAsString(), Modifier.Keyword.PUBLIC);
        method.setType(returnClass).addAndGetAnnotation(GetMapping.class).addPair("value", new StringLiteralExpr("/detail"));

        Optional<FieldDeclaration> id = ParserHelper.findFieldByAnnotation(entity, "Id");
        method.addAndGetParameter(id.get().getElementType(), "id");

        //return goTeamService.findById(id).orElseGet(null);
        method.getBody().get().addStatement(new ReturnStmt(
                new MethodCallExpr(new MethodCallExpr(new MethodCallExpr(
                        new FieldAccessExpr(new ThisExpr(), service.getVariables().get(0).getNameAsString()),
                        "findById", new NodeList<>(new NameExpr("id"))),
                        "map", new NodeList<>(new MethodReferenceExpr(
                        new NameExpr(returnClass), new NodeList<>(), "new"))),
                        "orElseGet", new NodeList<>(new NullLiteralExpr()))
        ));
    }

    /**
     * <pre>
     * &#64;GetMapping("/list")
     * public List &lt; GoTeamValue.Simple&gt; listGoTeam(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
     *     return goTeamService.findAll(page, size).stream()
     *         .map(GoTeamValue.Simple::new)
     *         .collect(Collectors.toList());
     * }
     * </pre>
     *
     * @return list Method
     */
    public MethodDeclaration addListMethod() {
        ClassOrInterfaceDeclaration clazz = this.getClazz();
        ClassOrInterfaceDeclaration entity = this.springService.getEntity();
        MethodDeclaration method = clazz.addMethod("list" + entity.getNameAsString(), Modifier.Keyword.PUBLIC);
        method.addAndGetAnnotation(GetMapping.class).addPair("value", new StringLiteralExpr("/list"));

        String returnClass = view.getClazz().getNameAsString() + ".Simple";
        method.setType(new ClassOrInterfaceType().setName("List").setTypeArguments(new ClassOrInterfaceType().setName(returnClass)));
        method.addAndGetParameter(PrimitiveType.intType(), "page").addAndGetAnnotation(RequestParam.class).addPair("defaultValue", new StringLiteralExpr("0"));
        method.addAndGetParameter(PrimitiveType.intType(), "size").addAndGetAnnotation(RequestParam.class).addPair("defaultValue", new StringLiteralExpr("10"));

        method.getBody().get().addStatement(new ReturnStmt(
                new MethodCallExpr(new MethodCallExpr(new MethodCallExpr(new MethodCallExpr(
                        new FieldAccessExpr(new ThisExpr(), this.service.getVariables().get(0).getNameAsString()),
                        "findAll", new NodeList<>(new NameExpr("page"), new NameExpr("size"))),
                        "stream"),
                        "map", new NodeList<>(new MethodReferenceExpr(new NameExpr(entity.getNameAsString() + "Value.Simple"), new NodeList<>(), "new"))),
                        "collect", new NodeList<>(new MethodCallExpr(new NameExpr("Collectors"), "toList")))
        ));

        method.tryAddImportToParentCompilationUnit(List.class);
        method.tryAddImportToParentCompilationUnit(Collectors.class);
        return method;
    }

    public static String toURI(String name) {
        return ParserHelper.firstLower(name).replaceAll("([A-Z])", "-$1").toLowerCase();
    }

}
