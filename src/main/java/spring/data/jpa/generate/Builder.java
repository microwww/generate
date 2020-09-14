package spring.data.jpa.generate;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.conf.JDBCConfigurationImpl;
import org.apache.openjpa.jdbc.meta.ReverseMappingTool;
import org.apache.openjpa.lib.conf.Configurations;
import org.apache.openjpa.lib.util.Options;
import org.apache.openjpa.persistence.EntityManagerFactoryImpl;
import org.apache.openjpa.persistence.PersistenceProviderImpl;
import org.apache.openjpa.persistence.PersistenceUnitInfoImpl;

import javax.sql.DataSource;
import java.io.File;
import java.util.Collections;
import java.util.List;

public class Builder {

    private JDBCConfiguration conf;
    private File srcDirectory;

    public Builder(DataSource dataSource) {
        PersistenceProviderImpl provider = new PersistenceProviderImpl();
        PersistenceUnitInfoImpl unit = new PersistenceUnitInfoImpl();
        unit.setJtaDataSource(dataSource);
        unit.setPersistenceUnitName("default");
        EntityManagerFactoryImpl factory = (EntityManagerFactoryImpl) provider.createContainerEntityManagerFactory(unit, Collections.EMPTY_MAP);
        conf = (JDBCConfiguration) factory.getConfiguration();
        //db2entity(config, pkg);
    }

    public Builder(String driver, String url, String username, String password) {
        conf = new JDBCConfigurationImpl();
        conf.setConnectionDriverName(driver);
        conf.setConnectionURL(url);
        conf.setConnectionUserName(username);
        conf.setConnectionPassword(password);
        conf.setSpecification("jpa");
        //db2entity(conf, pkg);
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
        return JpaEntity.scanJavaEntity(srcDirectory);
    }
}
