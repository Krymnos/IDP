package de.idp.pipeline.util;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

public class SystemHelper {
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

    public static void setUpEnvironment() throws Exception {
        File file = new File("resources/provenance.properties");
        String propertiesfilePath = file.getAbsolutePath();
        getModifiableEnvironment().put("provenance_properties", propertiesfilePath);
    }
}
