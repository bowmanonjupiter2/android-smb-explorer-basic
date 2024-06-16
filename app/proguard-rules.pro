# Retain line numbers for stack traces
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# Keep all public classes, methods, and fields
-keep public class * {
    public *;
}

# Keep all class members with certain annotations
-keepclassmembers class ** {
    @androidx.annotation.Keep *;
}

# Do not strip annotations
-keepattributes *Annotation*

# Keep names of native methods
-keepclasseswithmembers class * {
    native <methods>;
}

# Retain main entry points
-keepclasseswithmembers class * {
    public static void main(java.lang.String[]);
}

# Please add these rules to your existing keep rules in order to suppress warnings.
# This is generated automatically by the Android Gradle plugin.
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn javax.annotation.concurrent.GuardedBy
-dontwarn javax.naming.NamingEnumeration
-dontwarn javax.naming.NamingException
-dontwarn javax.naming.directory.Attribute
-dontwarn javax.naming.directory.Attributes
-dontwarn javax.naming.directory.DirContext
-dontwarn javax.naming.directory.InitialDirContext
-dontwarn javax.naming.directory.SearchControls
-dontwarn javax.naming.directory.SearchResult
-dontwarn javax.security.auth.callback.NameCallback
-dontwarn javax.security.auth.kerberos.KerberosKey
-dontwarn javax.security.auth.kerberos.KerberosPrincipal
-dontwarn javax.security.auth.login.Configuration
-dontwarn javax.security.auth.login.LoginContext
-dontwarn javax.servlet.Filter
-dontwarn javax.servlet.FilterChain
-dontwarn javax.servlet.FilterConfig
-dontwarn javax.servlet.ServletConfig
-dontwarn javax.servlet.ServletException
-dontwarn javax.servlet.ServletOutputStream
-dontwarn javax.servlet.ServletRequest
-dontwarn javax.servlet.ServletResponse
-dontwarn javax.servlet.http.HttpServlet
-dontwarn javax.servlet.http.HttpServletRequest
-dontwarn javax.servlet.http.HttpServletRequestWrapper
-dontwarn javax.servlet.http.HttpServletResponse
-dontwarn javax.servlet.http.HttpSession
-dontwarn org.ietf.jgss.GSSContext
-dontwarn org.ietf.jgss.GSSCredential
-dontwarn org.ietf.jgss.GSSException
-dontwarn org.ietf.jgss.GSSManager
-dontwarn org.ietf.jgss.GSSName
-dontwarn org.ietf.jgss.MessageProp
-dontwarn org.ietf.jgss.Oid
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn org.slf4j.impl.StaticMDCBinder
-dontwarn org.slf4j.impl.StaticMarkerBinder
# Add any additional rules specific to your project
