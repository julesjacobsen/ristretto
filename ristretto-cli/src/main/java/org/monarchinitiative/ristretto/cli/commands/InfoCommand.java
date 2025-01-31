package org.monarchinitiative.ristretto.cli.commands;

import org.monarchinitiative.ristretto.cli.RistrettoCommand;
import org.monarchinitiative.ristretto.cli.OntologyLoader;
import org.monarchinitiative.ristretto.core.curie.CurieUtil;
import org.monarchinitiative.ristretto.core.graph.Node;
import org.monarchinitiative.ristretto.core.Ontology;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@CommandLine.Command(name = "info", description = "Display information about a specific term")
public class InfoCommand implements Runnable {

    @CommandLine.ParentCommand
    private RistrettoCommand parent;

    @CommandLine.Parameters(index = "0..*", description = "The term identifier(s)")
    private List<String> termIds = List.of();

    @Override
    public void run() {
        Path inputFile = parent.getInputFile();
        Ontology<Node, Node> ontology = OntologyLoader.loadOntology(inputFile);

        CurieUtil curieUtil = parent.curieUtil();
        for (String termId : termIds) {
            String iri = curieUtil.getIri(termId).orElse("");
            Optional<Node> optionalNode = ontology.nodes().stream().filter(n -> n.id().equals(iri)).findFirst();
            if (optionalNode.isPresent()) {
                Node node = optionalNode.get();
                System.out.printf("%s ! %s%n", curieUtil.getCurie(node.id()).orElse(""), node.label());
            } else {
                System.err.printf("Term ID not found: %s%n", termId);
            }
        }
    }

}
