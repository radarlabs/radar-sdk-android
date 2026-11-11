# Rules applied to apps that depend on the Radar SDK.

# Huawei HMS location and Firebase Messaging are compileOnly dependencies. Apps that do not
# include them must still build with R8. The SDK uses these classes only if the app includes them:
# - HMS location: used only with the HUAWEI location services provider. If the app sets that
#   provider without HMS location, the SDK logs an error and location calls fail. It does not crash.
# - Firebase Messaging: used only by RadarFirebaseMessagingService, which the app declares.
# When you add a compileOnly dependency that SDK code references directly, add a rule here.
-dontwarn com.huawei.hmf.tasks.**
-dontwarn com.huawei.hms.location.**
-dontwarn com.google.firebase.messaging.**
