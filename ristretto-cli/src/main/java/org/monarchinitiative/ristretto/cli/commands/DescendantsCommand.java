package org.monarchinitiative.ristretto.cli.commands;

import com.google.common.graph.Graphs;
import org.monarchinitiative.ristretto.cli.RistrettoCommand;
import org.monarchinitiative.ristretto.cli.OntologyLoader;
import org.monarchinitiative.ristretto.core.curie.CurieUtil;
import org.monarchinitiative.ristretto.core.graph.Node;
import org.monarchinitiative.ristretto.core.Ontology;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@CommandLine.Command(name = "descendants", description = "Display the descendants (all children) of a node")
public class DescendantsCommand implements Runnable {

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
        Set<Node> predicateNodes = PredicatesConverter.toNodes(predicates, ontology, curieUtil);
        List<Node> inputNodes = termIds.stream()
                .map(termId -> curieUtil.getIri(termId).orElse(""))
                .map(iri -> Optional.ofNullable(nodes.get(iri)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

//        System.out.println("Filtering for: " + predicateNodes);
        for (String termId : termIds) {
            String iri = curieUtil.getIri(termId).orElse("");
            if (nodes.containsKey(iri)) {
                // TODO: enable filtering by predicate (value)
                List<Node> descendants = ontology.descendants(nodes.get(iri)).stream().sorted(Comparator.comparing(Node::label)).toList();
                descendants.forEach(n -> System.out.printf("%s ! %s%n", curieUtil.getCurie(n.id()).orElse(""), n.label()));
            } else {
                System.err.println("No such term: " + termId);
            }
        }
    }
}
