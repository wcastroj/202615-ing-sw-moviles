# Reglas de ProGuard/R8 para optimización y minificación en Release

# Volley
-keep class com.android.volley.Request { *; }
-keep class com.android.volley.Response { *; }
-keep class com.android.volley.VolleyError { *; }
-keep class com.android.volley.toolbox.** { *; }

# Glide
-keep public class * extends com.bumptech.glide.module.AppGlideModule

# Modelos para parseo JSON
-keepclassmembers class com.example.vinyls_application.models.** { *; }

# DataBinding y ViewBinding
-keepclassmembers class * {
    @androidx.databinding.BindingAdapter <methods>;
}
-keep class com.example.vinyls_application.databinding.** { *; }
-keep class com.example.vinyls_application.ui.widgets.** { *; }
