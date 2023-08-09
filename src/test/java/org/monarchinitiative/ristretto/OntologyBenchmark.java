package org.monarchinitiative.ristretto;

import com.google.common.graph.ValueGraph;
import org.monarchinitiative.phenol.graph.OntologyGraph;
import org.monarchinitiative.phenol.io.MinimalOntologyLoader;
import org.monarchinitiative.phenol.io.utils.CurieUtilBuilder;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.stream.Collectors;

public class OntologyBenchmark {

    //REMEMBER: The numbers below are just data. To gain reusable insights, you need to follow up on
    //why the numbers are the way they are. Use profilers (see -prof, -lprof), design factorial
    //experiments, perform baseline and negative tests that provide experimental control, make sure
    //the benchmarking environment is safe on JVM/OS/HW level, ask for reviews from the domain experts.
    //Do not assume the numbers tell you what you want them to tell.

    // JDK 17, nodes pure array implementation
    //Benchmark                                Mode  Cnt     Score     Error  Units
    //OntologyBenchmark.guavaInDegree         thrpt   10  3051.572 ±  62.625  ops/s
    //OntologyBenchmark.guavaOutDegree        thrpt   10  3004.166 ± 258.015  ops/s
    //OntologyBenchmark.guavaPredecessors     thrpt   10  2512.935 ± 296.395  ops/s
    //OntologyBenchmark.guavaSuccessors       thrpt   10  2375.491 ± 235.731  ops/s
    //OntologyBenchmark.risrettoInDegree      thrpt   10   433.533 ±  34.496  ops/s
    //OntologyBenchmark.risrettoOutDegree     thrpt   10   427.844 ±  32.925  ops/s
    //OntologyBenchmark.risrettoPredecessors  thrpt   10   426.872 ±  29.141  ops/s
    //OntologyBenchmark.risrettoSuccessors    thrpt   10   399.552 ±  33.996  ops/s

    // JDK 17, nodesIndex LinkedHashMap implementation
    //Benchmark                                Mode  Cnt     Score      Error  Units
    //OntologyBenchmark.risrettoInDegree      thrpt    5  3977.469 ±  212.850  ops/s
    //OntologyBenchmark.risrettoOutDegree     thrpt    5  4130.700 ± 1582.084  ops/s
    //OntologyBenchmark.risrettoPredecessors  thrpt    5  3151.148 ±  821.977  ops/s
    //OntologyBenchmark.risrettoSuccessors    thrpt    5  3113.716 ±  278.484  ops/s

//    # JMH version: 1.32
//    # VM version: JDK 17.0.8, OpenJDK 64-Bit Server VM, 17.0.8+7
//    # Blackhole mode: full + dont-inline hint
//    Benchmark                                Mode  Cnt     Score      Error  Units
//    OntologyBenchmark.guavaInDegree         thrpt    5  2421.888 ±   31.296  ops/s
//    OntologyBenchmark.guavaOutDegree        thrpt    5  2583.093 ±  733.086  ops/s
//    OntologyBenchmark.guavaPredecessors     thrpt    5  2299.017 ±  454.965  ops/s
//    OntologyBenchmark.guavaSuccessors       thrpt    5  2434.137 ±  329.812  ops/s
//    OntologyBenchmark.phenolInDegree        thrpt    5   190.147 ±    4.698  ops/s
//    OntologyBenchmark.phenolOutDegree       thrpt    5   190.108 ±   23.466  ops/s
//    OntologyBenchmark.phenolPredecessors    thrpt    5   315.934 ±   57.484  ops/s
//    OntologyBenchmark.phenolSuccessors      thrpt    5   290.997 ±   29.546  ops/s
//    OntologyBenchmark.risrettoInDegree      thrpt    5  4561.634 ±  264.965  ops/s
//    OntologyBenchmark.risrettoOutDegree     thrpt    5  4743.377 ±  693.705  ops/s
//    OntologyBenchmark.risrettoPredecessors  thrpt    5  3393.475 ± 1015.671  ops/s
//    OntologyBenchmark.risrettoSuccessors    thrpt    5  3432.519 ±   78.624  ops/s

    //Benchmark                               Mode  Cnt   Score    Error  Units
    //OntologyBenchmark.phenolAncestors      thrpt    5  17.923 ±  0.555  ops/s
    //OntologyBenchmark.phenolDescendents    thrpt    5  22.421 ±  8.016  ops/s
    //OntologyBenchmark.risrettoAncestors    thrpt    5  108.372 ± 1.895  ops/s
    //OntologyBenchmark.risrettoDescendents  thrpt    5   71.267 ± 1.724  ops/s

    private static final Ontology<Node, Node> ristrettoHpo = TestOntologyGraphs.ristrettoHpo();
    private static final ValueGraph<Node, Node> guavaHpo = TestOntologyGraphs.guavaValueGraphHpo();
    private static final OntologyGraph<TermId> phenolHpo = setupPhenolHpo();


    private static OntologyGraph<TermId> setupPhenolHpo() {
        MinimalOntology ontology = MinimalOntologyLoader.loadOntology(TestOntologyGraphs.HPO_GRAPH_DOCUMENT, CurieUtilBuilder.defaultCurieUtil());
        return ontology.graph();
    }

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .forks(1)
                .build();
        new Runner(opt).run();
    }

    @Benchmark
    public void phenolSuccessors(Blackhole blackhole) {
        OntologyGraph<TermId> hpo = phenolHpo;
        hpo.forEach(node -> blackhole.consume(hpo.getChildren(node, false)));
    }

    @Benchmark
    public void phenolPredecessors(Blackhole blackhole) {
        OntologyGraph<TermId> hpo = phenolHpo;
        hpo.forEach(node -> blackhole.consume(hpo.getParents(node, false)));
    }

    @Benchmark
    public void phenolInDegree(Blackhole blackhole) {
        OntologyGraph<TermId> hpo = phenolHpo;
        hpo.forEach(node -> blackhole.consume(hpo.getParentsStream(node, false).count()));
    }

    @Benchmark
    public void phenolOutDegree(Blackhole blackhole) {
        OntologyGraph<TermId> hpo = phenolHpo;
        hpo.forEach(node -> blackhole.consume(hpo.getChildrenStream(node, false).count()));
    }

    @Benchmark
    public void phenolAncestors(Blackhole blackhole) {
        OntologyGraph<TermId> hpo = phenolHpo;
        hpo.forEach(node -> blackhole.consume(hpo.getAncestorsStream(node, false).collect(Collectors.toUnmodifiableSet())));
    }

    @Benchmark
    public void phenolDescendents(Blackhole blackhole) {
        OntologyGraph<TermId> hpo = phenolHpo;
        hpo.forEach(node -> blackhole.consume(hpo.getDescendantsStream(node, false).collect(Collectors.toUnmodifiableSet())));
    }

    @Benchmark
    public void risrettoSuccessors(Blackhole blackhole) {
        Ontology<Node, Node> hpo = ristrettoHpo;
        for (Node node : hpo.nodes()) {
            blackhole.consume(hpo.successors(node));
        }
    }

    @Benchmark
    public void risrettoPredecessors(Blackhole blackhole) {
        Ontology<Node, Node> hpo = ristrettoHpo;
        for (Node node : hpo.nodes()) {
            blackhole.consume(hpo.predecessors(node));
        }
    }

    @Benchmark
    public void risrettoInDegree(Blackhole blackhole) {
        Ontology<Node, Node> hpo = ristrettoHpo;
        for (Node node : hpo.nodes()) {
            blackhole.consume(hpo.inDegree(node));
        }
    }

    @Benchmark
    public void risrettoOutDegree(Blackhole blackhole) {
        Ontology<Node, Node> hpo = ristrettoHpo;
        for (Node node : hpo.nodes()) {
            blackhole.consume(hpo.outDegree(node));
        }
    }

    @Benchmark
    public void risrettoAncestors(Blackhole blackhole) {
        Ontology<Node, Node> hpo = ristrettoHpo;
        for (Node node : hpo.nodes()) {
            blackhole.consume(hpo.ancestors(node));
        }
    }

    @Benchmark
    public void risrettoDescendents(Blackhole blackhole) {
        Ontology<Node, Node> hpo = ristrettoHpo;
        for (Node node : hpo.nodes()) {
            blackhole.consume(hpo.descendents(node));
        }
    }

    @Benchmark
    public void guavaSuccessors(Blackhole blackhole) {
        ValueGraph<Node, Node> hpo = guavaHpo;
        for (Node node : hpo.nodes()) {
            blackhole.consume(hpo.successors(node));
        }
    }

    @Benchmark
    public void guavaPredecessors(Blackhole blackhole) {
        ValueGraph<Node, Node> hpo = guavaHpo;
        for (Node node : hpo.nodes()) {
            blackhole.consume(hpo.predecessors(node));
        }
    }

    @Benchmark
    public void guavaInDegree(Blackhole blackhole) {
        ValueGraph<Node, Node> hpo = guavaHpo;
        for (Node node : hpo.nodes()) {
            blackhole.consume(hpo.inDegree(node));
        }
    }

    @Benchmark
    public void guavaOutDegree(Blackhole blackhole) {
        ValueGraph<Node, Node> hpo = guavaHpo;
        for (Node node : hpo.nodes()) {
            blackhole.consume(hpo.outDegree(node));
        }
    }
}
