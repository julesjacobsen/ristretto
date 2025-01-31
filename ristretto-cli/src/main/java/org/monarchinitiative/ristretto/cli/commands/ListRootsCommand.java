package org.monarchinitiative.ristretto.cli.commands;

import org.monarchinitiative.ristretto.cli.RistrettoCommand;
import org.monarchinitiative.ristretto.cli.OntologyLoader;
import org.monarchinitiative.ristretto.core.curie.CurieUtil;
import org.monarchinitiative.ristretto.core.graph.Node;
import org.monarchinitiative.ristretto.core.Ontology;
import picocli.CommandLine;

import java.nio.file.Path;


@CommandLine.Command(name = "roots", description = "Display the root nodes of an ontology")
public class ListRootsCommand implements Runnable {

    @CommandLine.ParentCommand
    private RistrettoCommand parent;

    @Override
    public void run() {
        Path inputFile = parent.getInputFile();
        Ontology<Node, Node> ontology = OntologyLoader.loadOntology(inputFile);
        CurieUtil curieUtil = parent.curieUtil();

        ontology.rootNodes().forEach(node -> System.out.printf("%s ! %s%n", curieUtil.getCurie(node.id()).orElse(""), node.label()));
    }
}
