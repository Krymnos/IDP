package de.idp.pipeline.util;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.logging.Logger;

public class SystemHelper {
    private static final Logger logger = Logger.getLogger(SystemHelper.class.getName());

    public final static String DEFAULT_PROPERTIES_FILE = "resources/provenance.properties";

    private static Map<String,String> getModifiableEnvironment() throws Exception{
        Class pe = Class.forName("java.lang.ProcessEnvironment");
        Method getenv = pe.getDeclaredMethod("getenv");
        getenv.setAccessible(true);
        Object unmodifiableEnvironment = getenv.invoke(null);
        Class map = Class.forName("java.util.Collections$UnmodifiableMap");
        Field m = map.getDeclaredField("m");
        m.setAccessible(true);
        return (Map) m.get(unmodifiableEnvironment);
    }

    public static void setPropertiesFile() throws Exception {
        setPropertiesFile(DEFAULT_PROPERTIES_FILE);
    }

    public static void setPropertiesFile(String propertiesfile) throws Exception {
        File file = new File(propertiesfile);
        String propertiesfilePath = file.getAbsolutePath();
        logger.info("Use properties file on: " + propertiesfilePath);
        getModifiableEnvironment().put("provenance_properties", propertiesfilePath);
    }
}
