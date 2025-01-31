package org.monarchinitiative.ristretto.core;

import com.google.common.graph.ValueGraph;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.graph.OntologyGraph;
import org.monarchinitiative.phenol.io.MinimalOntologyLoader;
import org.monarchinitiative.phenol.io.utils.CurieUtil;
import org.monarchinitiative.phenol.io.utils.CurieUtilBuilder;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.ristretto.core.graph.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;


public class HpoComparisonTests {

    private static final Logger logger = LoggerFactory.getLogger(HpoComparisonTests.class);

    public static final Node abnormalCirculatingHormoneConcentration = Node.of("http://purl.obolibrary.org/obo/HP_0003117", "Abnormal circulating hormone concentration");

    @Test
    void ristrettoTest() {
        Instant start = Instant.now();
        Ontology<Node, Node> hpo = TestOntologyGraphs.ristrettoHpo();
        Instant end = Instant.now();
        logger.info("Loaded ristretto hpo graph with {} nodes, {} edges in {}ms",  hpo.nodeCount(), hpo.edgeCount(), Duration.between(start, end).toMillis());

        Node all = Node.of("http://purl.obolibrary.org/obo/HP_0000001", "All");
//        assertThat(graph.predecessors(Node.of("http://purl.obolibrary.org/obo/HP_0000001", "All")), equalTo(Set.of()));

//        var lcaFinder = new NaiveLcaFinder<>(ONTOLOGY);
        Node modeOfInheritance = Node.of("http://purl.obolibrary.org/obo/HP_0000005", "Mode of inheritance");
        Node phenotypicAbnormality = Node.of("http://purl.obolibrary.org/obo/HP_0000118", "Phenotypic abnormality");
        assertThat(hpo.lca(phenotypicAbnormality, modeOfInheritance), equalTo(all));

        Node neoplasmOfTheParathyroidGland = Node.of("http://purl.obolibrary.org/obo/HP_0100733", "Neoplasm of the parathyroid gland");
        Node delayedThelarche = Node.of("http://purl.obolibrary.org/obo/HP_0025515", "Delayed thelarche");
        Node abnormalityOfTheEndocrineSystem = Node.of("http://purl.obolibrary.org/obo/HP_0000818", "Abnormality of the endocrine system");
        assertThat(hpo.lca(delayedThelarche, neoplasmOfTheParathyroidGland), equalTo(abnormalityOfTheEndocrineSystem));

        Set<Node> predecessors = hpo.predecessors(abnormalCirculatingHormoneConcentration);
        System.out.println("Ristretto predecessors (" + predecessors.size() + ")");
        predecessors.forEach(x -> System.out.println(x.id() + ", " + x.label()));

        Set<Node> successors = hpo.successors(abnormalCirculatingHormoneConcentration);
        System.out.println("Ristretto successors (" + successors.size() + ")");
        successors.forEach(x -> System.out.println(x.id() + ", " + x.label()));
    }

    @Test
    void testIc() {
        Ontology<Node, Node> hpo = TestOntologyGraphs.ristrettoHpo();

//        Node node = Node.of("http://purl.obolibrary.org/obo/HP_0032223", "Blood group");
        Node node = Node.of("http://purl.obolibrary.org/obo/HP_0001249", "Intellectual disability");
//        Node node = Node.of("http://purl.obolibrary.org/obo/HP_0000118", "Phenotypic abnormality");
//        Node node = Node.of("http://purl.obolibrary.org/obo/HP_0045027", "Abnormality of the thoracic cavity");
        Set<Node> predecessors = hpo.predecessors(node);
        System.out.println("Ristretto predecessors (" + predecessors.size() + ")");
        predecessors.forEach(x -> System.out.println(x.id() + ", " + x.label() + " " + icSanchez(hpo, x)));

        System.out.println(node.id() + ", " + node.label() + " " + icSanchez(hpo, node));

        Set<Node> successors = hpo.descendants(node);
        System.out.println("Ristretto successors (" + successors.size() + ")");
        successors.forEach(x -> System.out.println(x.id() + ", " + x.label() + " " + icSanchez(hpo, x) + (hpo.isLeafNode(x) ? " *" : "")));
    }

    // IC calculation based on intrinsic ontology properties from https://www.sciencedirect.com/science/article/pii/S0950705110001619
    private double icSanchez(Ontology<Node, Node> ontology, Node node) {
        // IC(a) = -log((leaves(a) / subsumers(a)) +1 / max_leaves + 1)
        int maxLeaves = ontology.leafNodes().size();
        int subsumerCount = ontology.ancestors(node).size();
        long leafCount = leafCount(ontology, node);
        System.out.println(node.id() + ", " + node.label() + " maxLeaves=" + maxLeaves + " #subsumers=" + subsumerCount + " #leaves=" + leafCount + (ontology.isLeafNode(node) ? " *" : ""));
        return -Math.log((double) (Math.max(leafCount, 1) / (double) Math.max(subsumerCount, 1) + 1) / (maxLeaves + 1d));
    }

    private long leafCount(Ontology<Node, Node> ontology, Node node) {
        var leafCount = 0;
        for (Node node1 : ontology.descendants(node)) {
            if (ontology.isLeafNode(node1)) {
                leafCount++;
            }
        }
        return leafCount;
    }

    @Test
    void guavaTest() {
        Instant start = Instant.now();
        ValueGraph<Node, Node> graph = TestOntologyGraphs.guavaValueGraphHpo();
        Instant end = Instant.now();
        logger.info("Loaded guava hpo graph with {} nodes, {} edges in {}ms", graph.nodes().size(), graph.edges().size(), Duration.between(start, end).toMillis());

        Node abnormalCirculatingHormoneConcentration = Node.of("http://purl.obolibrary.org/obo/HP_0003117", "Abnormal circulating hormone concentration");

        Set<Node> predecessors = graph.predecessors(abnormalCirculatingHormoneConcentration);
        System.out.println("Guava predecessors (" + predecessors.size() + ")");
        predecessors.forEach(x -> System.out.println(x.id() + ", " + x.label()));

        Set<Node> successors = graph.successors(abnormalCirculatingHormoneConcentration);
        System.out.println("Guava successors (" + successors.size() + ")");
        successors.forEach(x -> System.out.println(x.id() + ", " + x.label()));

    }

    @Test
    void phenolTest() {
        Instant start = Instant.now();
        CurieUtil curieUtil = CurieUtilBuilder.defaultCurieUtil();
        MinimalOntology ontology = MinimalOntologyLoader.loadOntology(TestOntologyGraphs.HPO_GRAPH_DOCUMENT, curieUtil);
        OntologyGraph<TermId> hpo = ontology.graph();
        logger.info("Loaded phenol graph of {} terms in {}ms", hpo.size(), Duration.between(start, Instant.now()).toMillis());
        // ancestors
        TermId abnormalCirculatingHormoneConcentrationId = TermId.of("HP:0003117");
        System.out.println("Phenol predecessors");
        hpo.getParents(abnormalCirculatingHormoneConcentrationId)
                .forEach(x -> System.out.println(curieUtil.getIri(x).get() + ", " + ontology.termForTermId(x).get().getName()));
        // successors
        System.out.println("Phenol successors");
        hpo.getChildren(abnormalCirculatingHormoneConcentrationId)
                .forEach(x -> System.out.println(curieUtil.getIri(x).get() + ", " + ontology.termForTermId(x).get().getName()));
    }

    @Test
    void comparePredecessorSuccessor() {
        CurieUtil curieUtil = CurieUtilBuilder.defaultCurieUtil();
        MinimalOntology phenolOntology = MinimalOntologyLoader.loadOntology(TestOntologyGraphs.HPO_GRAPH_DOCUMENT, curieUtil);
        OntologyGraph<TermId> phenolHpo = phenolOntology.graph();
        TermId abnormalCirculatingHormoneConcentrationId = curieUtil.getCurie(abnormalCirculatingHormoneConcentration.id()).get();

        Set<Node> phenolPredecessors = phenolHpo.getParentsStream(abnormalCirculatingHormoneConcentrationId)
                .map(termIdToNode(curieUtil, phenolOntology))
                .collect(Collectors.toUnmodifiableSet());
        Set<Node> phenolSuccessors = phenolHpo.getChildrenStream(abnormalCirculatingHormoneConcentrationId)
                .map(termIdToNode(curieUtil, phenolOntology))
                .collect(Collectors.toUnmodifiableSet());

        Ontology<Node, Node> ristrettoHpo = TestOntologyGraphs.ristrettoHpo();
        Set<Node> ristrettoPredecessors = ristrettoHpo.predecessors(abnormalCirculatingHormoneConcentration);
        Set<Node> ristrettoSuccessors = ristrettoHpo.successors(abnormalCirculatingHormoneConcentration);

        assertThat(ristrettoPredecessors, equalTo(phenolPredecessors));
        assertThat(ristrettoSuccessors, equalTo(phenolSuccessors));

        ValueGraph<Node, Node> guavaHpo = TestOntologyGraphs.guavaValueGraphHpo();
        Set<Node> guavaPredecessors = guavaHpo.predecessors(abnormalCirculatingHormoneConcentration);
        Set<Node> guavaSuccessors = guavaHpo.successors(abnormalCirculatingHormoneConcentration);

        assertThat(ristrettoPredecessors, equalTo(guavaPredecessors));
        assertThat(ristrettoSuccessors, equalTo(guavaSuccessors));
    }

    @Test
    void compareAncestorsDescendents() {
        CurieUtil curieUtil = CurieUtilBuilder.defaultCurieUtil();
        MinimalOntology phenolOntology = MinimalOntologyLoader.loadOntology(TestOntologyGraphs.HPO_GRAPH_DOCUMENT, curieUtil);
        OntologyGraph<TermId> phenolHpo = phenolOntology.graph();
        TermId abnormalCirculatingHormoneConcentrationId = curieUtil.getCurie(abnormalCirculatingHormoneConcentration.id()).get();

        Set<Node> phenolAncestors = phenolHpo.getAncestorsStream(abnormalCirculatingHormoneConcentrationId)
                .map(termIdToNode(curieUtil, phenolOntology))
                .collect(Collectors.toUnmodifiableSet());
        Set<Node> phenolDescendents = phenolHpo.getDescendantsStream(abnormalCirculatingHormoneConcentrationId)
                .map(termIdToNode(curieUtil, phenolOntology))
                .collect(Collectors.toUnmodifiableSet());

        Ontology<Node, Node> ristrettoHpo = TestOntologyGraphs.ristrettoHpo();

        Set<Node> ristrettoAncestors = ristrettoHpo.ancestors(abnormalCirculatingHormoneConcentration);
        Set<Node> ristrettoDescendents = ristrettoHpo.descendants(abnormalCirculatingHormoneConcentration);

        assertThat(ristrettoAncestors, equalTo(phenolAncestors));
        assertThat(ristrettoDescendents, equalTo(phenolDescendents));
    }

    private static Function<TermId, Node> termIdToNode(CurieUtil curieUtil, MinimalOntology minimalOntology) {
        return termId -> Node.of(curieUtil.getIri(termId).orElseThrow(), minimalOntology.termForTermId(termId).orElseThrow().getName());
    }
}
