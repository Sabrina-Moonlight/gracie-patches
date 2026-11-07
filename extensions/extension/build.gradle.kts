extension {
    name = "extensions/extension.mpe"
}

android {
    namespace = "app.template.extension"
}

dependencies {
    // Present in the host app; only needed to compile the interceptor.
    compileOnly("com.squareup.okhttp3:okhttp:4.12.0")
}
