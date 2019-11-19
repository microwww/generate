package com.github.microwww.generate;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.microwww.generate.util.ParserHelper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CreateController {

    public static String toURI(String name) {
        return ParserHelper.firstLower(name).replaceAll("([A-Z])", "-$1").toLowerCase();
    }

    public static CompilationUnit createClass(String pkg, ClassOrInterfaceDeclaration entity, ClassOrInterfaceDeclaration service) {
        CompilationUnit unit = new CompilationUnit(pkg);
        ClassOrInterfaceDeclaration clazz = unit.addClass(entity.getNameAsString() + "Controller", Modifier.Keyword.PUBLIC);
        clazz.addAndGetAnnotation(RestController.class);
        clazz.addAndGetAnnotation(RequestMapping.class).addPair("value", new StringLiteralExpr("/" + toURI(entity.getNameAsString())));

        FieldDeclaration field = clazz.addField(service.getNameAsString(), ParserHelper.firstLower(service.getNameAsString())).addAnnotation("Autowired");
        field.tryAddImportToParentCompilationUnit(Autowired.class);
        createListMethod(entity, clazz, field);
        createDetailMethod(entity, clazz, field);
        createSaveMethod(entity, clazz, field);

        unit.addImport(ParserHelper.findImportClass(entity));
        unit.addImport(ParserHelper.findImportClass(service));

        return unit;
    }

    /**
     *     @PostMapping("/save")
     *     public void saveGoTeam(GoTeam pay) {
     *         Optional<GoTeam> db = goTeamService.findById(pay.getId());
     *         GoTeam entity = db.orElse(pay);
     *         BeanUtils.copyProperties(pay, entity, "id");
     *         goTeamService.save(entity);
     *     }
     */
    private static void createSaveMethod(ClassOrInterfaceDeclaration entity, ClassOrInterfaceDeclaration clazz, FieldDeclaration field) {
        String param = "entity";
        MethodDeclaration method = clazz.addMethod("save" + entity.getNameAsString(), Modifier.Keyword.PUBLIC);
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
     *     @GetMapping("/detail")
     *     public GoShop detailGoShop(int id) {
     *         return goShopService.findById(id).orElseGet(null);
     *     }
     * </pre>
     *
     * @param entity
     * @param clazz
     * @param field
     */
    private static void createDetailMethod(ClassOrInterfaceDeclaration entity, ClassOrInterfaceDeclaration clazz, FieldDeclaration field) {
        MethodDeclaration method = clazz.addMethod("detail" + entity.getNameAsString(), Modifier.Keyword.PUBLIC);
        method.setType(entity.getNameAsString() + "Value.Simple").addAndGetAnnotation(GetMapping.class).addPair("value", new StringLiteralExpr("/detail"));

        //method.tryAddImportToParentCompilationUnit();

        Optional<FieldDeclaration> id = ParserHelper.findFieldByAnnotation(entity, "Id");
        method.addAndGetParameter(id.get().getElementType(), "id");

        //return goTeamService.findById(id).orElseGet(null);
        method.getBody().get().addStatement(new ReturnStmt(
                new MethodCallExpr(new MethodCallExpr(new MethodCallExpr(
                        new FieldAccessExpr(new ThisExpr(), field.getVariables().get(0).getNameAsString()),
                        "findById", new NodeList<>(new NameExpr("id"))),
                        "map", new NodeList<>(new MethodReferenceExpr(
                        new NameExpr(entity.getNameAsString() + "Value.Simple"), new NodeList<>(), "new"))),
                        "orElseGet", new NodeList<>(new NullLiteralExpr()))
        ));
    }

    /**
     * <pre>
     * @GetMapping("/list")
     * public List &lt; GoTeamValue.Simple> listGoTeam(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
     *     return goTeamService.findAll(page, size).stream()
     *         .map(GoTeamValue.Simple::new)
     *         .collect(Collectors.toList());
     * }
     * </pre>
     *
     * @param entity
     * @param clazz
     * @param field
     */
    private static void createListMethod(ClassOrInterfaceDeclaration entity, ClassOrInterfaceDeclaration clazz, FieldDeclaration field) {
        MethodDeclaration method = clazz.addMethod("list" + entity.getNameAsString(), Modifier.Keyword.PUBLIC);
        //method.tryAddImportToParentCompilationUnit(List.class);
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


}
