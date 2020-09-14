package spring.data.jpa.generate;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import spring.data.jpa.generate.util.FileHelper;
import spring.data.jpa.generate.util.ParserHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class FormDomain extends Clazz {
    public static final Logger logger = LoggerFactory.getLogger(FormDomain.class);

    private JpaEntity jpaEntity;

    public FormDomain(CompilationUnit unit, ClassOrInterfaceDeclaration clazz) {
        super(unit, clazz);
    }

    public class ID {
        public void createIdClass(File src, String pkg) {
            CompilationUnit unit = new CompilationUnit(pkg);
            TypeDeclaration<ClassOrInterfaceDeclaration> dec = new ClassOrInterfaceDeclaration(new NodeList<>(Modifier.createModifierList(Modifier.Keyword.PUBLIC)), false, "ID");
            unit.addType(dec);
            FileHelper.writeJavaFile(src, Collections.singletonList(unit));
        }

        public void refExtendsID() {
            CompilationUnit parse = FormDomain.this.getUnit();
            for (TypeDeclaration<?> type : parse.getTypes()) {
                if (type.isClassOrInterfaceDeclaration()) {
                    type.getMembers().stream().filter(e -> e.isClassOrInterfaceDeclaration()).filter(ee -> {
                        return "Ref".equals(ee.asClassOrInterfaceDeclaration().getNameAsString());
                    }).findAny().ifPresent(e -> {
                        e.asClassOrInterfaceDeclaration().setExtendedTypes(new NodeList<>(StaticJavaParser.parseClassOrInterfaceType("ID")));
                    });
                }
            }
        }
    }

    public static FormDomain createEntityForm(JpaEntity jpaEntity, String pkg) {
        ClassOrInterfaceDeclaration clz = jpaEntity.getClazz();
        CompilationUnit unit = new CompilationUnit(pkg);
        ClassOrInterfaceDeclaration out = unit.addClass(clz.getNameAsString() + "Form", Modifier.Keyword.PUBLIC);

        ClassOrInterfaceDeclaration ref = new ClassOrInterfaceDeclaration(
                Modifier.createModifierList(Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC),
                false,
                "Ref");
        out.addMember(ref); // INNER CLASS

        ClassOrInterfaceDeclaration nc = new ClassOrInterfaceDeclaration(
                Modifier.createModifierList(Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC),
                false,
                "Form");
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

        CompilationUnit parse = jpaEntity.getUnit();
        copy(parse, ids, unit, ref);
        copy(parse, list, unit, nc);

        FormDomain form = new FormDomain(unit, out);
        form.setJpaEntity(jpaEntity);
        return form;
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

    public JpaEntity getJpaEntity() {
        return jpaEntity;
    }

    public void setJpaEntity(JpaEntity jpaEntity) {
        this.jpaEntity = jpaEntity;
    }
}
