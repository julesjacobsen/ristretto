package org.monarchinitiative.ristretto.cli;

import org.geneontology.obographs.core.io.OgJsonReader;
import org.geneontology.obographs.core.model.Graph;
import org.geneontology.obographs.core.model.GraphDocument;
import org.monarchinitiative.ristretto.core.graph.Node;
import org.monarchinitiative.ristretto.core.Ontology;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OntologyLoader {

    private OntologyLoader() {
    }

    public static GraphDocument loadGraphDocument(Path inputFile) {
        try {
            return OgJsonReader.readFile(inputFile.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Ontology<Node, Node> loadOntology(Path inputFile) {
        GraphDocument graphDocument = loadGraphDocument(inputFile);
        Graph graph = graphDocument.getGraphs().get(0);

        Map<String, Node> nodes = graph.getNodes().stream()
//                .filter(node -> node.getId().contains("HP_"))
                .map(node -> Node.of(node.getId(), node.getLabel()))
                .collect(Collectors.toMap(Node::id, Function.identity()));

        Ontology.Builder<Node, Node> ontologyBuilder = Ontology.<Node, Node>builder();
        ontologyBuilder.id(graph.getId());
        graph.getEdges().forEach(edge -> {
            Node subject = nodes.get(edge.getSub());
            Node object = nodes.get(edge.getObj());
            String pred = edge.getPred();
            // automatically create non-node 'primitive' predicate node such as 'is_a', 'part_of', 'type',
            // 'subPropertyOf', 'inverseOf'. These will not have an expanded IRI from the CurieUtil
            Node predicate = nodes.computeIfAbsent(pred, p -> Node.of(pred, pred));
            if (object != null && subject != null && pred != null) {
                ontologyBuilder.addAxiom(subject, predicate, object);
            }
        });
        return ontologyBuilder.build();
    }

}
