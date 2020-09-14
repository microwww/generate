package spring.data.jpa.generate;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import com.github.microwww.generate.JpaSpringRepository;
import spring.data.jpa.generate.util.ParserHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Id;
import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.javaparser.ast.Modifier;

public class SpringRepository extends Clazz {
    private static final Logger logger = LoggerFactory.getLogger(JpaSpringRepository.class);

    private JpaEntity jpaEntity;
    private Type primitiveKey;

    public SpringRepository(CompilationUnit unit, ClassOrInterfaceDeclaration clazz) {
        super(unit, clazz);
    }

    public static List<SpringRepository> readJavaEntity2repository(File src, String repositoryPackage) {
        return JpaEntity.scanJavaEntity(src).stream().map(e -> {
            return createRepository(repositoryPackage, e);
        }).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
    }

    public SpringService createService(String pkg) {
        return SpringService.createServiceByRepository(this, pkg);
    }

    /**
     * <pre>
     * default Optional&lt;GoConfig&gt; findById(int integer) {
     *     return this.findById(Integer.valueOf(integer));
     * }
     * </pre>
     *
     * @return
     */
    public MethodDeclaration addFindById() {
        ClassOrInterfaceDeclaration clazz = getClazz();
        MethodDeclaration find = clazz.addMethod("findById", Modifier.Keyword.PUBLIC);
        try {
            Type type = clazz.getExtendedTypes().get(0).getTypeArguments().get().get(1);
            if (type instanceof ClassOrInterfaceType) {
                ClassOrInterfaceType typ = ((ClassOrInterfaceType) type);
                if (typ.isBoxedType()) {
                    Parameter id = find.addAndGetParameter(typ.toUnboxedType(), "id");
                    find.addModifier(Modifier.Keyword.DEFAULT);
                    find.getBody().get();
                    ParserHelper.getRootNode(clazz).ifPresent(c -> {
                        c.addImport(Optional.class.getName());
                    });

                    ClassOrInterfaceType opt = new ClassOrInterfaceType().setName(Optional.class.getSimpleName());
                    opt.setTypeArguments(jpaEntity.toClassOrInterfaceType());
                    find.setType(opt);

                    Expression param = new MethodCallExpr(typ.getNameAsExpression(), "valueOf", new NodeList<>(id.getNameAsExpression()));
                    find.getBody().get().addStatement(new ReturnStmt(
                            new MethodCallExpr(
                                    new ThisExpr(),
                                    find.getName(),
                                    new NodeList<>(param))));
                }
            }
        } catch (Exception e) {
            logger.warn("Not find @ID type ! Using Object .", e);
        }
        return find;
    }

    public MethodDeclaration addFindAll() {
        ClassOrInterfaceDeclaration clazz = getClazz();
        MethodDeclaration find = clazz.addMethod("findAll", Modifier.Keyword.PUBLIC);
        find.addAndGetParameter("Pageable", "page");
        CompilationUnit unit = ParserHelper.getRootNode(clazz).get();
        unit.addImport("org.springframework.data.domain.Pageable");
        ClassOrInterfaceType tr = new ClassOrInterfaceType();
        //clazz.addImport ("?.?.?.Entity");
        tr.setName("Page").setTypeArguments(jpaEntity.toClassOrInterfaceType());
        find.setType(tr);
        unit.addImport("org.springframework.data.domain.Page");
        return find;
    }

    public static Optional<SpringRepository> createRepository(String repositoryPackage, JpaEntity jpaEntity) {
        CompilationUnit unit = new CompilationUnit(repositoryPackage);
        ClassOrInterfaceDeclaration type = jpaEntity.getClazz();
        String sname = type.getNameAsString();
        Optional<FieldDeclaration> opt = ParserHelper.findFieldByAnnotation(type, "Id");
        Optional<SpringRepository> rep = opt.map(field -> {
            ClassOrInterfaceDeclaration clazz = unit.addInterface(sname + "Repository", Modifier.Keyword.PUBLIC);

            NodeList<ClassOrInterfaceType> types = new NodeList<>();
            ClassOrInterfaceType entity = new ClassOrInterfaceType();
            entity.setName(sname);

            CompilationUnit parse = jpaEntity.getUnit();
            String pk = parse.getPackageDeclaration().map(o -> o.getNameAsString() + "." + sname).orElse(sname);
            unit.addImport(pk);
            ClassOrInterfaceType ext = new ClassOrInterfaceType();
            ext.setName("JpaRepository");
            unit.addImport("org.springframework.data.jpa.repository.JpaRepository");

            Type idType = ParserHelper.findFieldByAnnotation(type, Id.class.getSimpleName()).map(o -> o.getVariable(0)).get().getType();
            if (idType instanceof PrimitiveType) {
                ext.setTypeArguments(entity, ((PrimitiveType) idType).toBoxedType());
            } else {
                ext.setTypeArguments(entity, idType);
            }
            types.add(ext);

            clazz.setExtendedTypes(types);
            // public interface Entity?Repository extends Repository<Entity?, Entity?.Id?>
            unit.getImports().sort(Comparator.comparing(NodeWithName::getNameAsString));

            SpringRepository spr = new SpringRepository(unit, clazz);
            spr.setJpaEntity(jpaEntity);
            spr.setPrimitiveKey(idType);
            return spr;
        });
        return rep;
    }

    public JpaEntity getJpaEntity() {
        return jpaEntity;
    }

    public void setJpaEntity(JpaEntity jpaEntity) {
        this.jpaEntity = jpaEntity;
    }

    public Type getPrimitiveKey() {
        return primitiveKey;
    }

    public void setPrimitiveKey(Type primitiveKey) {
        this.primitiveKey = primitiveKey;
    }
}
