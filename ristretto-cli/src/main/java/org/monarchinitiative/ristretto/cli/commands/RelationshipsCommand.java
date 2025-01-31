package org.monarchinitiative.ristretto.cli.commands;

import org.monarchinitiative.ristretto.cli.OntologyLoader;
import org.monarchinitiative.ristretto.cli.RistrettoCommand;
import org.monarchinitiative.ristretto.core.Ontology;
import org.monarchinitiative.ristretto.core.curie.CurieUtil;
import org.monarchinitiative.ristretto.core.graph.Node;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@CommandLine.Command(name = "relationships", description = "Show all relationships for a term or terms")
public class RelationshipsCommand implements Runnable {

    public enum Direction {UP, DOWN, BOTH}

    @CommandLine.ParentCommand
    private RistrettoCommand parent;

    @CommandLine.Parameters(index = "0..*", description = "The term identifier(s)")
    private List<String> termIds = List.of();

    @CommandLine.Option(names = {"-d", "--direction"}, description = "The direction in which to report relationships to the input term. Valid values: ${COMPLETION-CANDIDATES}", defaultValue = "down", converter = DirectionConverter.class)
    private Direction direction;

    @CommandLine.Option(names = {"-p", "--predicates"}, split = ",")
    private List<String> predicates = List.of();

    @Override
    public void run() {
        Path inputFile = parent.getInputFile();
        Ontology<Node, Node> ontology = OntologyLoader.loadOntology(inputFile);
        Map<String, Node> nodes = ontology.nodes().stream().collect(Collectors.toMap(Node::id, Function.identity()));
        CurieUtil curieUtil = parent.curieUtil();
        Set<Node> predicateFilterNodes = PredicatesConverter.toNodes(predicates, ontology, curieUtil);
        Predicate<Node> retainEdgesOfType = retainEdgesOfType(predicateFilterNodes);

        System.out.println("subject\tsubject_label\tpredicate\tpredicate_label\tobject\tobject_label");
        for (String termId : termIds) {
            String iri = curieUtil.getIri(termId).orElse("");
            if (nodes.containsKey(iri)) {
                final Node query = nodes.get(iri);
                switch (direction) {
                    case UP -> lookUp(query, ontology, curieUtil, retainEdgesOfType);
                    case DOWN -> lookDown(query, ontology, curieUtil, retainEdgesOfType);
                    case BOTH -> {
                        lookUp(query, ontology, curieUtil, retainEdgesOfType);
                        lookDown(query, ontology, curieUtil, retainEdgesOfType);
                    }
                }
            } else {
                System.err.println("No such term: " + termId);
            }
        }
    }

    private void lookUp(Node query, Ontology<Node, Node> ontology, CurieUtil curieUtil, Predicate<Node> retainEdgesOfType) {
        ontology.predecessors(query)
                .stream()
                .sorted(Comparator.comparing(Node::label))
                .forEach(n -> {
                    Optional<Node> relationship = ontology.edgeValue(n, query).filter(retainEdgesOfType);
                    relationship.ifPresent(pred -> System.out.printf("%s\t%s\t%s%n", format(query, curieUtil), format(pred, curieUtil), format(n, curieUtil)));
                });
    }

    private void lookDown(Node query, Ontology<Node, Node> ontology, CurieUtil curieUtil, Predicate<Node> retainEdgesOfType) {
        ontology.successors(query)
                .stream()
                .sorted(Comparator.comparing(Node::label))
                .forEach(n -> {
                    Optional<Node> relationship = ontology.edgeValue(query, n).filter(retainEdgesOfType);
                    relationship.ifPresent(pred -> System.out.printf("%s\t%s\t%s%n", format(n, curieUtil), format(pred, curieUtil), format(query, curieUtil)));
                });
    }

    private static Predicate<Node> retainEdgesOfType(Set<Node> predicateNodes) {
        return v -> predicateNodes.isEmpty() || predicateNodes.contains(v);
    }

    private String format(Node node, CurieUtil curieUtil) {
        return curieUtil.getCurie(node.id()).orElse("None") + "\t" + node.label();
    }

    public static class DirectionConverter implements CommandLine.ITypeConverter<Direction> {
        @Override
        public Direction convert(String s) throws Exception {
            return switch (s.toLowerCase()) {
                case "up" -> Direction.UP;
                case "both" -> Direction.BOTH;
                default -> Direction.DOWN;
            };
        }
    }
}
