-keep class com.jinhuan.forhappy.core.network.rust.RustNetBridge { *; }
-keepclassmembers class com.jinhuan.forhappy.core.network.rust.RustNetBridge { *; }
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
