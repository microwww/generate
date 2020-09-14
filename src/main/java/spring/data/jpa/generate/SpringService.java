package spring.data.jpa.generate;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import spring.data.jpa.generate.util.ParserHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;

import static com.github.javaparser.ast.Modifier.Keyword.PRIVATE;
import static com.github.javaparser.ast.Modifier.Keyword.PUBLIC;

public class SpringService extends Clazz {
    private static final Logger logger = LoggerFactory.getLogger(SpringService.class);

    private SpringRepository springRepository;
    private FieldDeclaration repository;

    public SpringService(CompilationUnit unit, ClassOrInterfaceDeclaration clazz) {
        super(unit, clazz);
    }

    public SpringController createController(String pkg, ViewDomain view) {
        return SpringController.createClass(this, pkg, view);
    }

    public static SpringService createServiceByRepository(SpringRepository repository, String servicePackage) {
        CompilationUnit unit = new CompilationUnit(servicePackage);
        CompilationUnit parse = repository.getUnit();

        // ParserHelper.findTypeBySuperclass(parse, "JpaRepository").ifPresent(se -> {
        ClassOrInterfaceType entity = repository.getJpaEntity().toClassOrInterfaceType();

        String name = repository.getClazz().getNameAsString();
        parse.getPackageDeclaration().ifPresent(e -> {
            unit.addImport(e.getNameAsString() + "." + name);
        });

        String clz = ParserHelper.findImportClass(parse, entity.getNameAsString()).map(ImportDeclaration::getNameAsString)//
                .orElse(parse.getPackageDeclaration().map(o -> o.getNameAsString() + "." + entity.getNameAsString()).orElse(""));

        //ClassOrInterfaceType id = (ClassOrInterfaceType) args.get(1); // ignore

        ClassOrInterfaceDeclaration clazz = unit.addClass(entity.getName() + "Service", PUBLIC);
        clazz.addAnnotation("Service");
        unit.addImport("org.springframework.stereotype.Service");
        clazz.addAnnotation("Transactional");
        unit.addImport("org.springframework.transaction.annotation.Transactional");

        FieldDeclaration field = clazz.addField(name, ParserHelper.firstLower(name), PRIVATE);
        field.addAndGetAnnotation("Autowired");
        unit.addImport(clz);
        unit.addImport("org.springframework.beans.factory.annotation.Autowired");
        field.createGetter();
        field.createSetter();

        unit.getImports().sort(Comparator.comparing(NodeWithName::getNameAsString));

        SpringService service = new SpringService(unit, clazz);
        service.setSpringRepository(repository);
        service.setRepository(field);
        return service;
    }

    public void addFindById() {
        List<MethodDeclaration> method = springRepository.getClazz().getMethodsByName("findById");//.get(0);
        if (!method.isEmpty()) {
            ParserHelper.delegate(repository, method.get(0));
        }
    }

    //    public Page<BscDicCodeType> findAll(int page, int size) {
    //        return this.bscDicCodeTypeRepository.findAll(PageRequest.of(page, size));
    //    }
    public void addFindAll() {
        MethodDeclaration findAll = springRepository.getClazz().getMethodsByName("findAll").get(0);
        MethodDeclaration mth = ParserHelper.delegate(repository, findAll);
        mth.setBody(new BlockStmt());
        Parameter page = new Parameter(PrimitiveType.intType(), "page");
        Parameter size = new Parameter(PrimitiveType.intType(), "size");
        mth.setParameters(new NodeList<>(page, size));

        Expression param = new MethodCallExpr(
                new TypeExpr(new ClassOrInterfaceType().setName("PageRequest")),
                "of",
                new NodeList<>(page.getNameAsExpression(), size.getNameAsExpression()));
        this.getUnit().addImport("org.springframework.data.domain.PageRequest");

        mth.getBody().get().addStatement(
                new ReturnStmt(
                        new MethodCallExpr(
                                new FieldAccessExpr(
                                        new ThisExpr(), this.repository.getVariables().get(0).getNameAsString()),
                                mth.getName(),
                                new NodeList<>(param))));
    }

    public void addSave() {
        ClassOrInterfaceType entity = springRepository.getJpaEntity().toClassOrInterfaceType();
        ClassOrInterfaceDeclaration clazz = springRepository.getClazz();
        MethodDeclaration save = clazz.addMethod("save", PUBLIC);
        save.setType(entity);
        Parameter parameter = save.addAndGetParameter(entity, "entity");
        BlockStmt stmt = save.getBody().get();
        stmt.addStatement(
                new ReturnStmt(
                        new MethodCallExpr(
                                new FieldAccessExpr(
                                        new ThisExpr(), repository.getVariables().get(0).getNameAsString()),
                                save.getName(),
                                new NodeList<>(new NameExpr(parameter.getName())))));
    }

    public void addGetOrElseThrow(ClassOrInterfaceType exception) {
        List<MethodDeclaration> method = springRepository.getClazz().getMethodsByName("findById");
        if (!method.isEmpty()) {
            ClassOrInterfaceDeclaration clazz = (ClassOrInterfaceDeclaration) repository.getParentNode().get();
            MethodDeclaration def = method.get(0);
            MethodDeclaration save = clazz.addMethod("getOrElseThrow", PUBLIC);
            Type type = def.getType().asClassOrInterfaceType().getTypeArguments().get().get(0);
            save.setType(type);
            Parameter parameter = save.addAndGetParameter(def.getParameter(0));
            BlockStmt stmt = save.getBody().get();
            // return new ExistException.NotExist(Album.class);
            BlockStmt lambda = new BlockStmt().addStatement(new ReturnStmt(
                    new ObjectCreationExpr(null, exception, new NodeList<>(new ClassExpr(type)))
            ));
            /**
             *     public Album getById(int id) {
             *         return this.albumRepository.findById(id).orElseThrow(() -> {
             *             return new ExistException.NotExist(Album.class);
             *         });
             *     }
             */
            stmt.addStatement(
                    new ReturnStmt(
                            new MethodCallExpr(
                                    new MethodCallExpr(
                                            new FieldAccessExpr(
                                                    new ThisExpr(), repository.getVariables().get(0).getNameAsString()
                                            ),
                                            def.getName(),
                                            new NodeList<>(new NameExpr(parameter.getName()))
                                    )
                                    , "orElseThrow"
                                    , new NodeList<>(new LambdaExpr(new NodeList<>(), lambda))
                            )
                    )
            );
        }
    }

    public SpringRepository getSpringRepository() {
        return springRepository;
    }

    public void setSpringRepository(SpringRepository springRepository) {
        this.springRepository = springRepository;
    }

    public FieldDeclaration getRepository() {
        return repository;
    }

    public ClassOrInterfaceDeclaration getEntity() {
        return this.getSpringRepository().getJpaEntity().getClazz();
    }

    public void setRepository(FieldDeclaration repository) {
        this.repository = repository;
    }
}
