package org.monarchinitiative.ristretto;

import com.google.common.graph.ImmutableValueGraph;
import com.google.common.graph.ValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import org.geneontology.obographs.core.io.OgJsonReader;
import org.geneontology.obographs.core.model.Graph;
import org.geneontology.obographs.core.model.GraphDocument;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.graph.builder.GraphBuilder;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OntologyBenchmark {

    private static final Graph HP;
    private static final Ontology<Node, Node> ristrettoHpo;
    private static final ValueGraph<Node, Node> guavaHpo;
//    private static final org.jgrapht.Graph<Object, Node> jgraphtHpo;

    static {
        GraphDocument graphDocument = null;
        try {
            graphDocument = OgJsonReader.readFile("/home/hhx640/Documents/hp.json");
        } catch (IOException e) {
            e.printStackTrace();
        }
        HP = graphDocument.getGraphs().get(0);

        // Setup
        ristrettoHpo = setupOntologyHpo();
        guavaHpo = setupGuavaHpo();
//        jgraphtHpo = setupJgraphtHpo();
    }

    private static ValueGraph<Node, Node> setupGuavaHpo() {
        ImmutableValueGraph.Builder<Node, Node> graphBuilder = ValueGraphBuilder
                .directed()
                .immutable();

        Map<String, Node> hpNodes = HP.getNodes().stream().filter(node -> node.getId().contains("HP_"))
                .map(node -> Node.of(node.getId(), node.getLabel()))
                .collect(Collectors.toMap(Node::getId, Function.identity()));
        Node isA = Node.of("RO:0000111", "is_a");
        HP.getEdges().stream().filter(edge -> edge.getSub().contains("HP_"))
                .forEach(edge -> {
                    Node subject = hpNodes.get(edge.getSub());
                    Node object = hpNodes.get(edge.getObj());
                    if (subject != null && object != null) {
                        graphBuilder.putEdgeValue(object, subject, isA);
                    }
                });
        return graphBuilder.build();
    }

//    private static org.jgrapht.Graph<Object, Node> setupJgraphtHpo() {
//        // TODO: this is broken
//        var graphBuilder= DirectedAcyclicGraph.createBuilder(Node.class);
//        Map<String, Node> hpNodes = HP.getNodes().stream().filter(node -> node.getId().contains("HP_"))
//                .map(node -> Node.of(node.getId(), node.getLabel()))
//                .collect(Collectors.toMap(Node::getId, Function.identity()));
//        Node isA = Node.of("RO:0000111", "is_a");
//        HP.getEdges().stream().filter(edge -> edge.getSub().contains("HP_"))
//                .forEach(edge -> {
//                    Node subject = hpNodes.get(edge.getSub());
//                    Node object = hpNodes.get(edge.getObj());
//                    if (subject != null && object != null) {
//                        graphBuilder.addEdge(subject, object, isA);
//                    }
//                });
//        return graphBuilder.build();
//    }

    private static Ontology<Node, Node> setupOntologyHpo() {
        Ontology.Builder<Node, Node> ontologyBuilder = Ontology.builder();

        Map<String, Node> hpNodes = HP.getNodes().stream().filter(node -> node.getId().contains("HP_"))
                .map(node -> Node.of(node.getId(), node.getLabel()))
                .collect(Collectors.toMap(Node::getId, Function.identity()));
        Node isA = Node.of("RO:0000111", "is_a");

        HP.getEdges().stream().filter(edge -> edge.getSub().contains("HP_"))
                .forEach(edge -> {
                    Node subject = hpNodes.get(edge.getSub());
                    Node object = hpNodes.get(edge.getObj());
                    if (subject != null && object != null) {
                        ontologyBuilder.putEdgeValue(subject, isA, object);
                    }
                });
        return ontologyBuilder.build();
    }

    public static final Node modeOfInheritance = Node.of("http://purl.obolibrary.org/obo/HP_0000005", "Mode of inheritance");
    public static final Node phenotypicAbnormality = Node.of("http://purl.obolibrary.org/obo/HP_0000118", "Phenotypic abnormality");

    public static final Node neoplasmOfTheParathyroidGland = Node.of("http://purl.obolibrary.org/obo/HP_0100733", "Neoplasm of the parathyroid gland");
    public static final Node delayedThelarche = Node.of("http://purl.obolibrary.org/obo/HP_0025515", "Delayed thelarche");
    public static final Node abnormalityOfTheEndocrineSystem = Node.of("http://purl.obolibrary.org/obo/HP_0000818", "Abnormality of the endocrine system");

    public static final Node abnormalCirculatingHormoneConcentration = Node.of("http://purl.obolibrary.org/obo/HP_0003117", "Abnormal circulating hormone concentration");

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .forks(2)
                .build();
        new Runner(opt).run();
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
