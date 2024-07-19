plugins {
    id("gg.essential.multi-version.root")
}

preprocess {
    val fabric12100 = createNode("1.21-fabric", 12100, "official")
    val fabric11605 = createNode("1.16.5-fabric", 11605, "official")

    fabric12100.link(fabric11605)
}
