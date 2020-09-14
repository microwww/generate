package spring.data.jpa.generate;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import spring.data.jpa.generate.util.FileHelper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CreateI18nException {

    public enum Type {
        I18nException, ServiceException, ExistException;

        public CompilationUnit copyClass(String pkg) throws IOException {
            URL url = CreateI18nException.class.getResource("/template/" + this.name() + ".java");
            InputStream content = (InputStream) url.getContent();
            CompilationUnit u = StaticJavaParser.parse(new InputStreamReader(content, StandardCharsets.UTF_8));
            return u.setPackageDeclaration(pkg);
        }

        public File write(File src, String pkg) throws IOException {
            CompilationUnit unit = this.copyClass(pkg);
            return FileHelper.writeJavaFile(src, Collections.singletonList(unit)).get(0);
        }
    }

    public static Map<Type, CompilationUnit> writeException(File src, String pkg) throws IOException {
        Map<Type, CompilationUnit> map = new HashMap<>();
        for (Type v : Type.values()) {
            CompilationUnit unit = v.copyClass(pkg);
            FileHelper.writeJavaFile(src, Collections.singletonList(unit)).get(0);
            map.put(v, unit);
        }
        return map;
    }
}
