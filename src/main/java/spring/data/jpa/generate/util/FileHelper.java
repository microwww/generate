package spring.data.jpa.generate.util;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spring.data.jpa.generate.util.ParserHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FileHelper {
    private static final Logger logger = LoggerFactory.getLogger(ParserHelper.class);

    public static <T> List<T> scanJavaFile(File src, Function<File, Optional<T>> consumer) {
        List<T> res = new ArrayList<>();
        File[] list = new File[]{src};
        if (src.isDirectory()) {
            list = src.listFiles();
        }
        for (File f : list) {
            if (f.isDirectory()) {
                List<T> add = scanJavaFile(f, consumer); // 递归
                res.addAll(add);
            } else if (f.getName().toLowerCase().endsWith(".java")) {
                consumer.apply(f).ifPresent(repository -> {
                    res.add(repository);
                });
            } else {
                logger.debug("skip file {}", f);
            }
        }
        return res;
    }

    public static List<File> writeJavaFile(File src, List<CompilationUnit> classes) {
        return classes.stream().map(a -> {
            TypeDeclaration<?> type = a.getType(0);
            String pkg = a.getPackageDeclaration().map(PackageDeclaration::getNameAsString).orElse("");
            File res = FileSystems.getDefault().getPath(src.getAbsolutePath(), pkg.replace('.', File.separatorChar),
                    type.getNameAsString() + ".java").toFile();
            return new AbstractMap.SimpleEntry<>(res, a);
        }).filter(se -> {
            File f = se.getKey();
            if (f.exists()) {
                if (f.isDirectory()) {
                    return false;
                }
            } else {
                try {
                    f.getParentFile().mkdirs();
                    f.createNewFile();
                } catch (IOException ex) {
                    logger.warn("Not can create file {}", f, ex);
                    return false;
                }
            }
            return f.canWrite();
        }).map(se -> {
            try (OutputStream out = new FileOutputStream(se.getKey())) {
                out.write(se.getValue().toString().getBytes("UTF8"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return se.getKey();
        }).collect(Collectors.toList());
    }
}
