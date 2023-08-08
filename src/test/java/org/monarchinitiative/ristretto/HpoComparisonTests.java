package org.monarchinitiative.ristretto;

import com.google.common.graph.ValueGraph;
import org.geneontology.obographs.core.model.Graph;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.graph.OntologyGraph;
import org.monarchinitiative.phenol.io.MinimalOntologyLoader;
import org.monarchinitiative.phenol.io.utils.CurieUtil;
import org.monarchinitiative.phenol.io.utils.CurieUtilBuilder;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
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
        assertThat(hpo.getLCA(phenotypicAbnormality, modeOfInheritance), equalTo(all));

        Node neoplasmOfTheParathyroidGland = Node.of("http://purl.obolibrary.org/obo/HP_0100733", "Neoplasm of the parathyroid gland");
        Node delayedThelarche = Node.of("http://purl.obolibrary.org/obo/HP_0025515", "Delayed thelarche");
        Node abnormalityOfTheEndocrineSystem = Node.of("http://purl.obolibrary.org/obo/HP_0000818", "Abnormality of the endocrine system");
        assertThat(hpo.getLCA(delayedThelarche, neoplasmOfTheParathyroidGland), equalTo(abnormalityOfTheEndocrineSystem));

        Set<Node> predecessors = hpo.predecessors(abnormalCirculatingHormoneConcentration);
        System.out.println("Ristretto predecessors (" + predecessors.size() + ")");
        predecessors.forEach(x -> System.out.println(x.getId() + ", " + x.getLabel()));

        Set<Node> successors = hpo.successors(abnormalCirculatingHormoneConcentration);
        System.out.println("Ristretto successors (" + successors.size() + ")");
        successors.forEach(x -> System.out.println(x.getId() + ", " + x.getLabel()));
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
        predecessors.forEach(x -> System.out.println(x.getId() + ", " + x.getLabel()));

        Set<Node> successors = graph.successors(abnormalCirculatingHormoneConcentration);
        System.out.println("Guava successors (" + successors.size() + ")");
        successors.forEach(x -> System.out.println(x.getId() + ", " + x.getLabel()));

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
        hpo.getParents(abnormalCirculatingHormoneConcentrationId, false)
                .forEach(x -> System.out.println(curieUtil.getIri(x).get() + ", " + ontology.termForTermId(x).get().getName()));
        // successors
        System.out.println("Phenol successors");
        hpo.getChildren(abnormalCirculatingHormoneConcentrationId, false)
                .forEach(x -> System.out.println(curieUtil.getIri(x).get() + ", " + ontology.termForTermId(x).get().getName()));
    }

    @Test
    void compareResults() {
        CurieUtil curieUtil = CurieUtilBuilder.defaultCurieUtil();
        MinimalOntology phenolOntology = MinimalOntologyLoader.loadOntology(TestOntologyGraphs.HPO_GRAPH_DOCUMENT, curieUtil);
        OntologyGraph<TermId> phenolHpo = phenolOntology.graph();
        TermId abnormalCirculatingHormoneConcentrationId = curieUtil.getCurie(abnormalCirculatingHormoneConcentration.getId()).get();

        Set<Node> phenolPredecessors = phenolHpo.getParentsStream(abnormalCirculatingHormoneConcentrationId, false)
                .map(termId -> Node.of(curieUtil.getIri(termId).orElseThrow(), phenolOntology.termForTermId(termId).orElseThrow().getName()))
                .collect(Collectors.toUnmodifiableSet());
        Set<Node> phenolSuccessors = phenolHpo.getChildrenStream(abnormalCirculatingHormoneConcentrationId, false)
                .map(termId -> Node.of(curieUtil.getIri(termId).orElseThrow(), phenolOntology.termForTermId(termId).orElseThrow().getName()))
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

}
