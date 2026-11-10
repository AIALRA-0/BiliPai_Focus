plugins {
    base
}

tasks.register<Zip>("packageBpSkin") {
    group = "distribution"
    description = "Package the Blue Snow Maid UI skin as a .bpskin file."
    archiveBaseName.set("blue-snow-maid")
    archiveExtension.set("bpskin")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))

    from(layout.projectDirectory.file("skin-manifest.json"))
    from(layout.projectDirectory.dir("assets")) {
        into("assets")
    }
}
