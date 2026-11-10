plugins {
    `java-library`
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

dependencies {
    // Keep aligned with AGP 9.3.2; this jar is consumed by Lint, never the APK.
    testImplementation("com.android.tools.lint:lint-api:32.3.2")
    compileOnly("com.android.tools.lint:lint-api:32.3.2")
    testImplementation("com.android.tools.lint:lint-tests:32.3.2")
    testImplementation("junit:junit:4.13.2")
}

tasks.jar {
    manifest.attributes["Lint-Registry-v2"] = "com.android.purebilibili.lint.CiIssueRegistry"
}

tasks.test {
    maxHeapSize = "2g"
}
