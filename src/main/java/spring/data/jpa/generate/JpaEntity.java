package spring.data.jpa.generate;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import spring.data.jpa.generate.util.FileHelper;
import spring.data.jpa.generate.util.ParserHelper;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JpaEntity extends Clazz {

    public JpaEntity(CompilationUnit unit, ClassOrInterfaceDeclaration clazz) {
        super(unit, clazz);
    }

    public static List<JpaEntity> scanJavaEntity(File src) {
        return FileHelper.scanJavaFile(src, f -> {
            try {
                CompilationUnit parse = StaticJavaParser.parse(f);
                return ParserHelper.findTypeByAnnotation(parse, "Entity").map(t -> {
                    return new JpaEntity(parse, t.asClassOrInterfaceDeclaration());
                });
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public boolean entityIdGeneratedValue() {
        ClassOrInterfaceDeclaration clazz = this.getClazz();
        for (FieldDeclaration field : clazz.getFields()) {
            Optional<AnnotationExpr> opt = field.getAnnotationByClass(Id.class).map(e -> {
                if (field.getAnnotationByClass(GeneratedValue.class).isPresent()) {
                } else {
                    NormalAnnotationExpr expr = field.addAndGetAnnotation(GeneratedValue.class);
                    expr.addPair("strategy", "GenerationType.IDENTITY");
                }
                return e;
            });
        }
        Optional<FieldDeclaration> any = clazz.getFields().stream().filter(f -> f.getAnnotationByClass(Id.class).isPresent()).findAny();
        any.ifPresent(field -> {
            if (field.getAnnotationByClass(GeneratedValue.class).isPresent()) {
            } else {
                NormalAnnotationExpr expr = field.addAndGetAnnotation(GeneratedValue.class);
                expr.addPair("strategy", "GenerationType.IDENTITY");
            }
        });
        return any.isPresent();
    }

    public Optional<SpringRepository> createRepository(String pkg) {
        return SpringRepository.createRepository(pkg, this);
    }

    public FormDomain createForm(String pkg) {
        return FormDomain.createEntityForm(this, pkg);
    }

    public ViewDomain createView(String pkg) {
        Clazz cs = ViewDomain.createBaseClass(pkg);
        return ViewDomain.createEntityDTO(this, cs.toClassOrInterfaceType(), pkg);
    }

    public List<FieldDeclaration> entitySetToList() {
        ClassOrInterfaceDeclaration clazz = this.getClazz();
        List<FieldDeclaration> res = new ArrayList<>();
        for (FieldDeclaration field : clazz.getFields()) {
            Optional<AnnotationExpr> exist = field.getAnnotationByClass(OneToMany.class);
            exist.ifPresent(e -> {
                NormalAnnotationExpr ex = (NormalAnnotationExpr) e;
                ex.getPairs().stream().filter(f -> f.getNameAsString().equals("targetEntity")).findAny().ifPresent(val -> {
                    val.getValue().toClassExpr().ifPresent(expr -> {
                        ClassOrInterfaceType type = StaticJavaParser.parseClassOrInterfaceType(List.class.getSimpleName());
                        ClassOrInterfaceType ci = (ClassOrInterfaceType) expr.getType();
                        type.setTypeArguments(ci.removeScope());
                        field.setVariable(0, new VariableDeclarator(type, field.getVariable(0).getName()));

                        clazz.getMethodsByName("get" + ParserHelper.firstUpper(field.getVariable(0).getNameAsString())).forEach(m -> {
                            if (m.getParameters().isEmpty()) {
                                m.setType(type);
                            }
                        });
                        clazz.getMethodsByName("set" + ParserHelper.firstUpper(field.getVariable(0).getNameAsString())).forEach(m -> {
                            if (m.getParameters().size() == 1) {
                                m.setParameter(0, new Parameter(type, m.getParameter(0).getName()));
                            }
                        });
                        res.add(field);
                    });
                });
            });
        }
        return res;
    }

}
