package spring.data.jpa.generate;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import spring.data.jpa.generate.util.FileHelper;

import java.io.File;
import java.util.Collections;

public abstract class Clazz {
    private final CompilationUnit unit;
    private final ClassOrInterfaceDeclaration clazz;

    public Clazz(CompilationUnit unit, ClassOrInterfaceDeclaration clazz) {
        this.unit = unit;
        this.clazz = clazz;
    }

    public CompilationUnit getUnit() {
        return unit;
    }

    public ClassOrInterfaceDeclaration getClazz() {
        return clazz;
    }

    public ClassOrInterfaceType toClassOrInterfaceType() {
        String name = this.getClazz().getNameAsString();
        return StaticJavaParser.parseClassOrInterfaceType(name);
    }

    public File write(File src) {
        return FileHelper.writeJavaFile(src, Collections.singletonList(this.unit)).get(0);
    }
}
