plugins {
    id("gg.essential.multi-version.root")
}

preprocess {
    val fabric12100 = createNode("1.21-fabric", 12100, "official")
    val fabric11902 = createNode("1.19.2-fabric", 11902, "official")

    fabric12100.link(fabric11902)
}
