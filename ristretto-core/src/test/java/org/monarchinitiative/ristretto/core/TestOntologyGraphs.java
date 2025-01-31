package org.monarchinitiative.ristretto.core;

import com.google.common.graph.ImmutableValueGraph;
import com.google.common.graph.ValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import org.geneontology.obographs.core.io.OgJsonReader;
import org.geneontology.obographs.core.model.Graph;
import org.geneontology.obographs.core.model.GraphDocument;
import org.monarchinitiative.ristretto.core.graph.Node;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TestOntologyGraphs {

    public static final GraphDocument HPO_GRAPH_DOCUMENT;
    public static final Graph HPO;

    public static final Node modeOfInheritance = Node.of("http://purl.obolibrary.org/obo/HP_0000005", "Mode of inheritance");
    public static final Node phenotypicAbnormality = Node.of("http://purl.obolibrary.org/obo/HP_0000118", "Phenotypic abnormality");

    public static final Node neoplasmOfTheParathyroidGland = Node.of("http://purl.obolibrary.org/obo/HP_0100733", "Neoplasm of the parathyroid gland");
    public static final Node delayedThelarche = Node.of("http://purl.obolibrary.org/obo/HP_0025515", "Delayed thelarche");
    public static final Node abnormalityOfTheEndocrineSystem = Node.of("http://purl.obolibrary.org/obo/HP_0000818", "Abnormality of the endocrine system");
    public static final Node abnormalCirculatingHormoneConcentration = Node.of("http://purl.obolibrary.org/obo/HP_0003117", "Abnormal circulating hormone concentration");


    static {
        GraphDocument graphDocument = null;
        try {
            graphDocument = OgJsonReader.readFile("src/test/resources/hp.json");
        } catch (IOException e) {
            e.printStackTrace();
        }
        HPO_GRAPH_DOCUMENT = graphDocument;
        HPO = graphDocument.getGraphs().get(0);
    }

    private static volatile Ontology<Node, Node> ristrettoHpo = null;
    private static volatile ValueGraph<Node, Node> guavaValueGraphHpo = null;

    public static Ontology<Node, Node> ristrettoHpo() {
        if (ristrettoHpo == null) {
            ristrettoHpo = loadRistrettoHpo();
        }
        return ristrettoHpo;
    }

    private static Ontology<Node, Node> loadRistrettoHpo() {
        Map<String, Node> hpNodes = HPO.getNodes().stream().filter(node -> node.getId().contains("HP_"))
                .map(node -> Node.of(node.getId(), node.getLabel()))
                .collect(Collectors.toMap(Node::id, Function.identity()));
        Node isA = Node.of("RO:0000111", "is_a");

        Ontology.Builder<Node, Node> ontologyBuilder = Ontology.<Node, Node>builder();
        ontologyBuilder.id(HPO.getId());
        HPO.getEdges().stream().filter(edge -> edge.getSub().contains("HP_"))
                .forEach(edge -> {
                    Node subject = hpNodes.get(edge.getSub());
                    Node object = hpNodes.get(edge.getObj());
                    if (!edge.getPred().startsWith("http://") && !edge.getPred().equals("is_a")) {
//                        System.out.println(edge);
                    }
                    Node predicate;
                    if (edge.getPred().equals("is_a")) {
                        predicate = isA;
                    } else {
                        predicate = hpNodes.get(edge.getPred());
                    }
//                    System.out.println(subject + " - " + predicate + " -> " + object);
                    if (subject != null && object != null) {
                        ontologyBuilder.addAxiom(subject, Objects.requireNonNullElse(predicate, isA), object);
                    }
                });
        return ontologyBuilder.build();
    }

    public static ValueGraph<Node, Node> guavaValueGraphHpo() {
        if (guavaValueGraphHpo == null) {
            guavaValueGraphHpo = loadGuavaValueGraphHpo();
        }
        return guavaValueGraphHpo;
    }

    private static ValueGraph<Node, Node> loadGuavaValueGraphHpo() {
        Map<String, Node> hpNodes = HPO.getNodes().stream().filter(node -> node.getId().contains("HP_"))
                .map(node -> Node.of(node.getId(), node.getLabel()))
                .collect(Collectors.toMap(Node::id, Function.identity()));
        Node isA = Node.of("RO:0000111", "is_a");

        ImmutableValueGraph.Builder<Node, Node> graphBuilder = ValueGraphBuilder
                .directed()
                .immutable();
        // Ontology needs to become a CsrDirectedGraph and then a new Ontology needs to have reversed bfs/dfs/root/leaf operations

        HPO.getEdges().stream().filter(edge -> edge.getSub().contains("HP_"))
                .forEach(edge -> {
                    Node subject = hpNodes.get(edge.getSub());
                    Node object = hpNodes.get(edge.getObj());
                    if (subject != null && object != null) {
                        graphBuilder.putEdgeValue(object, subject, isA);
                    }
                });
        return graphBuilder.build();
    }
}
