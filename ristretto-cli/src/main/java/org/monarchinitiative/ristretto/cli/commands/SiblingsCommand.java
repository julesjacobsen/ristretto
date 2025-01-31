package org.monarchinitiative.ristretto.cli.commands;

import org.monarchinitiative.ristretto.cli.RistrettoCommand;
import org.monarchinitiative.ristretto.cli.OntologyLoader;
import org.monarchinitiative.ristretto.core.Ontology;
import org.monarchinitiative.ristretto.core.curie.CurieUtil;
import org.monarchinitiative.ristretto.core.graph.Node;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@CommandLine.Command(name = "siblings", description = "Display the siblings of a node")
public class SiblingsCommand implements Runnable {

    @CommandLine.ParentCommand
    private RistrettoCommand parent;

    @CommandLine.Parameters(index = "0..*", description = "The term identifier(s)")
    private List<String> termIds = List.of();

    @CommandLine.Option(names = {"-p", "--predicates"}, split = ",")
    private List<String> predicates = List.of();

    @Override
    public void run() {
        Path inputFile = parent.getInputFile();
        Ontology<Node, Node> ontology = OntologyLoader.loadOntology(inputFile);
        Map<String, Node> nodes = ontology.nodes().stream().collect(Collectors.toMap(Node::id, Function.identity()));
        CurieUtil curieUtil = parent.curieUtil();
        for (String termId : termIds) {
            String iri = curieUtil.getIri(termId).orElse("");
            if (nodes.containsKey(iri)) {
                Node query = nodes.get(iri);
                Set<Node> parents = ontology.predecessors(query);
                List<Node> siblings = parents
                        .stream()
                        .flatMap(node -> ontology.successors(node).stream())
                        .filter(node -> !node.equals(query))
                        .sorted(Comparator.comparing(Node::label))
                        .toList();
                siblings.forEach(n -> System.out.printf("%s ! %s%n", curieUtil.getCurie(n.id()).orElse(""), n.label()));
            } else {
                System.err.println("No such term: " + termId);
            }
        }
    }
}
