load(":config.bzl", "JAVA8_ARCHITECTURES", "JAVA_ARCHITECTURES", "JAVA_BASE_PACKAGES", "JAVA_DISTROS")
load(":java.bzl", "java_base_image", "java_base_image_index", "java_image", "java_image_index")

package(default_visibility = ["//visibility:public"])

# publishable java base images
[
    java_base_image(
        distro,
        arch,
        JAVA_BASE_PACKAGES[distro],
    )
    for distro in JAVA_DISTROS
    for arch in JAVA_ARCHITECTURES[distro]
]

[
    java_base_image_index(
        distro,
        JAVA_ARCHITECTURES[distro],
    )
    for distro in JAVA_DISTROS
]

# publishable java temurin images from adoptium deb repository
ADOPTIUM_DEB_PER_DISTRO = [
    ("17", "debian13"),
    ("21", "debian13"),
    ("25", "debian13"),
]

[
    java_image(distro, java_version, arch)
    for java_version, distro in ADOPTIUM_DEB_PER_DISTRO
    for arch in JAVA_ARCHITECTURES[distro]
]

# Java 8: limited architecture support (no riscv64), uses dedicated repo
[
    java_image(distro, "8", arch, repo_suffix = "adoptium_java8")
    for distro in JAVA_DISTROS
    for arch in JAVA8_ARCHITECTURES[distro]
]

# all image indexes
[
    java_image_index(
        distro,
        java_version,
        JAVA_ARCHITECTURES[distro],
    )
    for java_version, distro in ADOPTIUM_DEB_PER_DISTRO
]

[
    java_image_index(
        distro,
        "8",
        JAVA8_ARCHITECTURES[distro],
    )
    for distro in JAVA_DISTROS
]
