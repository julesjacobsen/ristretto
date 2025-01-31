package org.monarchinitiative.ristretto.cli.commands;

import org.monarchinitiative.ristretto.cli.RistrettoCommand;
import org.monarchinitiative.ristretto.cli.OntologyLoader;
import org.monarchinitiative.ristretto.core.curie.CurieUtil;
import org.monarchinitiative.ristretto.core.graph.Node;
import org.monarchinitiative.ristretto.core.Ontology;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@CommandLine.Command(name = "ancestors", description = "Display the ancestors (all parents) of a node(s)")
public class AncestorsCommand implements Runnable {

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
                List<Node> ancestors = ontology.ancestors(nodes.get(iri)).stream().sorted(Comparator.comparing(Node::label)).toList();
                ancestors.forEach(n -> System.out.printf("%s ! %s%n", curieUtil.getCurie(n.id()).orElse(""), n.label()));
            } else {
                System.err.println("No such term: " + termId);
            }
        }
    }
}
