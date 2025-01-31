package org.monarchinitiative.ristretto.cli.commands;

import org.monarchinitiative.ristretto.core.Ontology;
import org.monarchinitiative.ristretto.core.curie.CurieUtil;
import org.monarchinitiative.ristretto.core.graph.Node;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PredicatesConverter {

    private PredicatesConverter() {
    }

    public static Set<Node> toNodes(List<String> predicates, Ontology<Node, Node> ontology, CurieUtil curieUtil) {
        Set<String> predicateIds = predicates.stream()
                .flatMap(PredicatesConverter::shorthandToPredicateCurie)
                // handle cases where 'is_a', 'partOf', 'type', 'subPropertyOf', 'inverseOf', don't have an IRI
                .map(c -> curieUtil.getIri(c).orElse(c))
                .collect(Collectors.toSet());
        Set<Node> predicateNodes = ontology.nodes().stream().filter(node -> predicateIds.contains(node.id())).collect(Collectors.toSet());
        // 'is_a', 'partOf', 'type', 'subPropertyOf', 'inverseOf', don't always have an IRI, so we need to remove all those with an IRI
        predicateIds.removeAll(predicateNodes.stream().map(Node::id).collect(Collectors.toSet()));
        // and add the missing self-referential nodes
        for (String pred : predicateIds) {
            predicateNodes.add(Node.of(pred, pred));
        }
        return Set.copyOf(predicateNodes);
    }

    private static final String IS_A = "is_a";
    private static final String PART_OF = "BFO:0000050";
    private static final String HAS_PART = "BFO:0000051";
    private static final String OCCURS_IN = "BFO:0000066";
    private static final String DEVELOPS_FROM = "RO:0002202";
    private static final String ENABLES = "RO:0002327";
    private static final String ENABLED_BY = "RO:0002333";
    private static final String HAS_DIRECT_INPUT = "RO:0002400";
    private static final String HAS_INPUT = "RO:0002233";
    private static final String HAS_OUTPUT = "RO:0002234";
    private static final String NEGATIVELY_REGULATES = "RO:0002212";
    private static final String POSITIVELY_REGULATES = "RO:0002213";
    private static final String REGULATES = "RO:0002211";
    private static final String RDF_TYPE = "rdf:type";
    private static final String EQUIVALENT_CLASS = "owl:equivalentClass";
    private static final String DISJOINT_WITH = "owl:disjointWith";
    private static final String RDFS_DOMAIN = "rdfs:domain";
    private static final String RDFS_RANGE = "rdfs:range";

    private static Stream<String> shorthandToPredicateCurie(String shorthand) {
        return switch (shorthand) {
            case "i" -> Stream.of(IS_A);
            case "p" -> Stream.of(PART_OF);
            case "h" -> Stream.of(HAS_PART);
            case "o" -> Stream.of(OCCURS_IN);
            case "d" -> Stream.of(DEVELOPS_FROM);
            case "en" -> Stream.of(ENABLES, ENABLED_BY);
            case "io" -> Stream.of(HAS_INPUT, HAS_OUTPUT, HAS_DIRECT_INPUT);
            case "r" -> Stream.of(REGULATES, NEGATIVELY_REGULATES, POSITIVELY_REGULATES);
            case "t" -> Stream.of(RDF_TYPE);
            case "e" -> Stream.of(EQUIVALENT_CLASS);
            case "owl" -> Stream.of(IS_A, RDF_TYPE, EQUIVALENT_CLASS, DISJOINT_WITH, RDFS_DOMAIN, RDFS_RANGE);
            default -> Stream.of(shorthand);
        };
    }
}
