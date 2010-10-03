-libraryjars <java.home>/lib/rt.jar;<java.home>/../lib/tools.jar
-keep public class MCPatcher { public static void main(java.lang.String[]); }
-keep class org.apache.commons.logging.impl.LogFactoryImpl
-keep class org.apache.commons.logging.Log
-keep class org.apache.commons.logging.impl.Jdk14Logger {
        <init>(java.lang.String);
}
-dontnote
-dontwarn
-optimizationpasses 3
-allowaccessmodification
-mergeinterfacesaggressively