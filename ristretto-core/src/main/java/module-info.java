module org.monarchinitiative.ristretto.core {
    exports org.monarchinitiative.ristretto.core;
    exports org.monarchinitiative.ristretto.core.curie;
    exports org.monarchinitiative.ristretto.core.graph;
    requires com.google.common;
    requires jsr305;
    requires org.checkerframework.checker.qual;
    requires org.yaml.snakeyaml;
    requires org.slf4j;
}