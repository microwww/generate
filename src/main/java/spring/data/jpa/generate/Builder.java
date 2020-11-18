package spring.data.jpa.generate;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.meta.ReverseMappingTool;
import org.apache.openjpa.lib.conf.Configurations;
import org.apache.openjpa.lib.util.Options;
import org.apache.openjpa.persistence.EntityManagerFactoryImpl;
import org.apache.openjpa.persistence.PersistenceProviderImpl;
import org.apache.openjpa.persistence.PersistenceUnitInfoImpl;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.persistence.Column;
import javax.persistence.Table;
import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class Builder {

    private JDBCConfiguration conf;
    private DataSource dataSource;
    private File srcDirectory;

    public Builder(DataSource dataSource) {
        PersistenceProviderImpl provider = new PersistenceProviderImpl();
        PersistenceUnitInfoImpl unit = new PersistenceUnitInfoImpl();
        unit.setJtaDataSource(dataSource);
        unit.setPersistenceUnitName("default");
        EntityManagerFactoryImpl factory = (EntityManagerFactoryImpl) provider.createContainerEntityManagerFactory(unit, Collections.EMPTY_MAP);
        conf = (JDBCConfiguration) factory.getConfiguration();
        this.dataSource = dataSource;
    }

    public Builder(String driver, String url, String username, String password) {
        this(Builder.dataSource(driver, url, username, password));
    }

    private static DataSource dataSource(String driver, String url, String username, String password) {
        DriverManagerDataSource ds = new DriverManagerDataSource(url, username, password);
        ds.setDriverClassName(driver);
        return ds;
    }

    public File getSrcDirectory() {
        return srcDirectory;
    }

    public List<JpaEntity> createEntity(File srcDirectory, String pkg) {
        this.srcDirectory = srcDirectory;
        String f = srcDirectory.getAbsolutePath();
        Options opts = new Options();
        String[] arguments = opts.setFromCmdLine(new String[]{
                "-pkg", pkg,
                "-d", f,
                "-annotations", "true",
                "-nullableAsObject", "true",
                "-md", "none"});
        Configurations.runAgainstAllAnchors(opts, (opts1) -> {
            try {
                return ReverseMappingTool.run(conf, arguments, opts1);
            } finally {
                conf.close();
            }
        });
        try {
            Connection connection = dataSource.getConnection();
            JpaEntity.scanJavaEntity(srcDirectory).forEach(entity -> {
                Optional<AnnotationExpr> tab = entity.getClazz().getAnnotationByClass(Table.class);
                if (!tab.isPresent()) {
                    return;
                }
                if (tab.get().isNormalAnnotationExpr()) {
                    NormalAnnotationExpr an = tab.get().asNormalAnnotationExpr();
                    for (MemberValuePair p : an.getPairs()) {
                        if (!"name".equals(p.getNameAsString())) {
                            continue;
                        }
                        String name = p.getValue().toStringLiteralExpr().get().asString();
                        try {
                            NodeList<BodyDeclaration<?>> list = sort(connection, entity, name);
                            entity.getClazz().setMembers(list);
                            Path path = entity.getUnit().getStorage().get().getPath();
                            Files.write(path, entity.getUnit().toString().getBytes(), StandardOpenOption.WRITE);
                        } catch (SQLException | IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }

            });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return JpaEntity.scanJavaEntity(srcDirectory);
    }

    private NodeList<BodyDeclaration<?>> sort(Connection connection, JpaEntity entity, String name) throws SQLException {
        // ResultSet rset = connection.getMetaData().getTables(connection.getCatalog(), connection.getSchema(), name, new String[]{"TABLE"});
        ResultSet rset = connection.getMetaData().getColumns(connection.getCatalog(), connection.getSchema(), name, null);
        List<String> col = new ArrayList<>();
        while (rset.next()) {
            col.add(rset.getString("COLUMN_NAME"));
        }
        NodeList<BodyDeclaration<?>> members = entity.getClazz().getMembers();
        NodeList<BodyDeclaration<?>> list = new NodeList<>();
        Map<String, BodyDeclaration<?>> membersMap = new TreeMap<>();
        Iterator<BodyDeclaration<?>> iterator = members.iterator();
        while (iterator.hasNext()) {
            BodyDeclaration<?> e = iterator.next();
            if (e.isFieldDeclaration()) {
                String cName = e.asFieldDeclaration().getAnnotationByClass(Column.class).flatMap(ee -> {
                    return ee.asNormalAnnotationExpr().getPairs()
                            .stream().filter(eee -> "name".equals(eee.getName())).findAny()
                            .map(eee -> eee.getValue().asLiteralStringValueExpr().getValue());
                }).orElse(e.asFieldDeclaration().getVariables().get(0).getNameAsString());
                membersMap.put(cName, e);
                iterator.remove();
            }
        }
        for (String s : col) {
            BodyDeclaration<?> remove = membersMap.remove(s);
            if (remove != null) {
                list.add(remove);
            }
        }
        list.addAll(membersMap.values());
        for (BodyDeclaration<?> member : members) {
            list.add(member);
        }
        return list;
    }
}
