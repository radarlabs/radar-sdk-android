# Rules applied to apps that depend on the Radar SDK.

# Huawei HMS location and Firebase Messaging are compileOnly dependencies. Apps that don't
# include them never load the classes that reference them, so R8 shouldn't fail on them.
-dontwarn com.huawei.hmf.tasks.**
-dontwarn com.huawei.hms.location.**
-dontwarn com.google.firebase.FirebaseApp
-dontwarn com.google.firebase.messaging.**
