package com.github.microwww.generate;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.TypeParameter;

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
}
