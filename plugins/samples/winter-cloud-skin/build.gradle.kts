plugins {
    base
}

tasks.register<Zip>("packageBpSkin") {
    group = "distribution"
    description = "Package the winter cloud UI skin as a .bpskin file."
    archiveBaseName.set("winter-cloud")
    archiveExtension.set("bpskin")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))

    from(layout.projectDirectory.file("skin-manifest.json"))
    from(layout.projectDirectory.dir("assets")) {
        into("assets")
    }
}
