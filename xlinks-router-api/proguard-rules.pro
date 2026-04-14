# Keep annotation metadata for Spring/MyBatis reflection.
-keepattributes *Annotation*,EnclosingMethod,InnerClasses,Signature,Record,MethodParameters

# Keep source file/line info for server-side troubleshooting.
-keepattributes SourceFile,LineNumberTable

# Keep main entry.
-keep class site.xlinks.ai.router.ApiApplication { *; }

# Keep Spring beans that are typically discovered/reflected by annotation.
-keep @org.springframework.context.annotation.Configuration class * { *; }
-keep @org.springframework.stereotype.Component class * { *; }
-keep @org.springframework.stereotype.Service class * { *; }
-keep @org.springframework.stereotype.Repository class * { *; }
-keep @org.springframework.web.bind.annotation.RestController class * { *; }
-keep @org.springframework.web.bind.annotation.ControllerAdvice class * { *; }
-keep @org.springframework.boot.context.properties.ConfigurationProperties class * { *; }

# Keep MyBatis mapper interfaces and entity-like data carriers.
-keep @org.apache.ibatis.annotations.Mapper interface * { *; }
-keep class site.xlinks.ai.router.entity.** { *; }
-keep class site.xlinks.ai.router.dto.** { *; }
-keep class site.xlinks.ai.router.vo.** { *; }
-keep class site.xlinks.ai.router.context.** { *; }

# Keep enum methods used by framework/json conversion.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Ignore warning noise from optional/runtime-only paths in server packaging.
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.slf4j.**
-dontwarn org.springframework.**

# Enable shrinking and optimization by default.
-allowaccessmodification
-repackageclasses
