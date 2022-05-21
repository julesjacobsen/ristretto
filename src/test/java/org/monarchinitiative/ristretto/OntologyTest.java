package org.monarchinitiative.ristretto;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import com.google.common.graph.ValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import org.geneontology.obographs.core.io.OgJsonReader;
import org.geneontology.obographs.core.model.Graph;
import org.geneontology.obographs.core.model.GraphDocument;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Jules Jacobsen <j.jacobsen@qmul.ac.uk>
 */
class OntologyTest {

    private static final Graph hp;
    static {
        GraphDocument graphDocument = null;
        try {
            graphDocument = OgJsonReader.readFile("/home/hhx640/Documents/hp.json");
        } catch (IOException e) {
            e.printStackTrace();
        }
        hp = graphDocument.getGraphs().get(0);
    }

    //    @Disabled
    @Test
    void testOboGraphJsonInput() throws Exception {
        Instant start = Instant.now();
        Map<String, Node> hpNodes = hp.getNodes().stream().filter(node -> node.getId().contains("HP_"))
                .map(node -> Node.of(node.getId(), node.getLabel()))
                .collect(Collectors.toMap(Node::getId, Function.identity()));
        Node isA = Node.of("RO:0000111", "is_a");

        Ontology.Builder<Node, Node> ontologyBuilder = Ontology.<Node, Node>builder();
        hp.getEdges().stream().filter(edge -> edge.getSub().contains("HP_"))
                .forEach(edge -> {
                    Node subject = hpNodes.get(edge.getSub());
                    Node object = hpNodes.get(edge.getObj());
                    if (subject != null && object != null) {
                        ontologyBuilder.putEdgeValue(subject, isA, object);
                    }
                });
        Ontology<Node, Node> hpo = ontologyBuilder.build();
        Instant end = Instant.now();
        System.out.println("Loaded hpo with " + hpo.nodeCount() + " nodes in " + Duration.between(start, end).toMillis() + "ms");

        Instant queriesStart = Instant.now();

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

        Node abnormalCirculatingHormoneConcentration = Node.of("http://purl.obolibrary.org/obo/HP_0003117", "Abnormal circulating hormone concentration");

        System.out.println("CSR predecessors" + hpo.predecessors(abnormalCirculatingHormoneConcentration));
        System.out.println("CSR successors" + hpo.successors(abnormalCirculatingHormoneConcentration));

        System.out.println("CSR ancestors of: " + abnormalCirculatingHormoneConcentration);
        assertThat(ancestors(abnormalCirculatingHormoneConcentration, hpo), equalTo(Set.of(all, phenotypicAbnormality, abnormalityOfTheEndocrineSystem, abnormalCirculatingHormoneConcentration)));
        ancestors(abnormalCirculatingHormoneConcentration, hpo).stream().forEach(node -> System.out.println(node + " -> "));
        System.out.println("CSR descendants of: " + abnormalCirculatingHormoneConcentration);
        descendents(abnormalCirculatingHormoneConcentration, hpo).stream().forEach(node -> System.out.println(node + " -> "));

        System.out.println("Queries took " + Duration.between(queriesStart, Instant.now()).toMillis() + "ms");
    }

    @Test
    void testDirectedAcyclicGraph() {
        Node beer = Node.of("BO:00001", "Beer");
        Node ipa = Node.of("BO:00002", "IPA");
        Node westCoastIpa = Node.of("BO:00003", "West Coast IPA");
        Node gooseIsland = Node.of("BO:00004", "Goose Island");
        Node titanIpa = Node.of("BO:00005", "Titan IPA");
        Node newEnglandIpa = Node.of("BO:00006", "New England IPA");
        Node cloudwaterIpa = Node.of("BO:00007", "Cloudwater IPA");
        // edge value
        Node isA = Node.of("RO:0000111", "is_a");

        Ontology<Node, Node> graph = Ontology.<Node, Node>builder()
                .putEdgeValue(ipa, isA, beer)
                .putEdgeValue(westCoastIpa, isA, ipa)
                .putEdgeValue(newEnglandIpa, isA, ipa)
                .putEdgeValue(cloudwaterIpa, isA, newEnglandIpa)
                .putEdgeValue(gooseIsland, isA, westCoastIpa)
                .putEdgeValue(titanIpa, isA, westCoastIpa)
                .build();

        System.out.println(graph);

        assertThat(graph.predecessors(beer), equalTo(Set.of()));
        assertThat(graph.predecessors(ipa), equalTo(Set.of(beer)));
        assertThat(graph.predecessors(cloudwaterIpa), equalTo(Set.of(newEnglandIpa)));
        assertThat(graph.predecessors(westCoastIpa), equalTo(Set.of(ipa)));
        assertThat(graph.predecessors(newEnglandIpa), equalTo(Set.of(ipa)));
        assertThat(graph.predecessors(gooseIsland), equalTo(Set.of(westCoastIpa)));
        assertThat(graph.predecessors(titanIpa), equalTo(Set.of(westCoastIpa)));

        assertThat(graph.inDegree(beer), equalTo(0));
        assertThat(graph.inDegree(ipa), equalTo(1));
        assertThat(graph.inDegree(cloudwaterIpa), equalTo(1));
        assertThat(graph.inDegree(westCoastIpa), equalTo(1));
        assertThat(graph.inDegree(gooseIsland), equalTo(1));
        assertThat(graph.inDegree(titanIpa), equalTo(1));

        assertThat(graph.successors(beer), equalTo(Set.of(ipa)));
        assertThat(graph.successors(ipa), equalTo(Set.of(newEnglandIpa, westCoastIpa)));
        assertThat(graph.successors(cloudwaterIpa), equalTo(Set.of()));
        assertThat(graph.successors(westCoastIpa), equalTo(Set.of(titanIpa, gooseIsland)));
        assertThat(graph.successors(newEnglandIpa), equalTo(Set.of(cloudwaterIpa)));
        assertThat(graph.successors(gooseIsland), equalTo(Set.of()));
        assertThat(graph.successors(titanIpa), equalTo(Set.of()));

        assertThat(graph.outDegree(beer), equalTo(1));
        assertThat(graph.outDegree(ipa), equalTo(2));
        assertThat(graph.outDegree(cloudwaterIpa), equalTo(0));
        assertThat(graph.outDegree(westCoastIpa), equalTo(2));
        assertThat(graph.outDegree(newEnglandIpa), equalTo(1));
        assertThat(graph.outDegree(gooseIsland), equalTo(0));
        assertThat(graph.outDegree(titanIpa), equalTo(0));

        assertThat(graph.isLeafNode(beer), is(false));
        assertThat(graph.isLeafNode(ipa), is(false));
        assertThat(graph.isLeafNode(cloudwaterIpa), is(true));
        assertThat(graph.isLeafNode(westCoastIpa), is(false));
        assertThat(graph.isLeafNode(gooseIsland), is(true));
        assertThat(graph.isLeafNode(titanIpa), is(true));

        assertThat(graph.isRootNode(beer), is(true));
        assertThat(graph.isRootNode(ipa), is(false));
        assertThat(graph.isRootNode(cloudwaterIpa), is(false));
        assertThat(graph.isRootNode(westCoastIpa), is(false));
        assertThat(graph.isRootNode(gooseIsland), is(false));
        assertThat(graph.isRootNode(titanIpa), is(false));

        assertThat(graph.hasEdgeConnecting(ipa, beer), is(true));
        assertThat(graph.hasEdgeConnecting(beer, ipa), is(false));
        assertThat(graph.hasEdgeConnecting(titanIpa, beer), is(false));
    }

    @Test
    void testDirectedCircularGraph() {
        Ontology<Integer, Integer> graph = Ontology.<Integer, Integer>builder()
                .putEdgeValue(1, 0, 2)
                .putEdgeValue(1, 1, 3)
                .putEdgeValue(1, 2, 4)
                .putEdgeValue(2, 3, 4)
                .putEdgeValue(3, 4, 1)
                .putEdgeValue(3, 5, 4)
                .putEdgeValue(3, 4, 6)
                .putEdgeValue(4, 6, 2)
                .putEdgeValue(4, 7, 3)
                .putEdgeValue(4, 8, 4)
                .putEdgeValue(4, 9, 5)
                .build();

        System.out.println("Children of 1: " +  graph.successors(1));

        assertThat(graph.successors(1), equalTo(Set.of(2, 3, 4)));
        assertThat(graph.successors(2), equalTo(Set.of(4)));
        assertThat(graph.successors(3), equalTo(Set.of(1, 4, 6)));
        assertThat(graph.successors(4), equalTo(Set.of(2, 3, 4, 5)));
        assertThat(graph.successors(5), equalTo(Set.of()));
        assertThat(graph.successors(6), equalTo(Set.of()));

        assertThat(graph.isLeafNode(1), is(false));
        assertThat(graph.isLeafNode(2), is(false));
        assertThat(graph.isLeafNode(3), is(false));
        assertThat(graph.isLeafNode(4), is(false));
        assertThat(graph.isLeafNode(5), is(true));
        assertThat(graph.isLeafNode(6), is(true));

        assertThat(graph.inDegree(1), equalTo(1));
        assertThat(graph.outDegree(1), equalTo(3));

        assertThat(graph.inDegree(2), equalTo(2));
        assertThat(graph.outDegree(2), equalTo(1));

        assertThat(graph.inDegree(3), equalTo(2));
        assertThat(graph.outDegree(3), equalTo(3));

        assertThat(graph.inDegree(4), equalTo(4));
        assertThat(graph.outDegree(4), equalTo(4));

        assertThat(graph.inDegree(5), equalTo(1));
        assertThat(graph.outDegree(5), equalTo(0));

        assertThat(graph.inDegree(6), equalTo(1));
        assertThat(graph.outDegree(6), equalTo(0));

        assertThat(graph.getValueOfEdge(1, 2), equalTo(0));
        assertThat(graph.getValueOfEdge(1, 3), equalTo(1));
        assertThat(graph.getValueOfEdge(1, 4), equalTo(2));
        assertThat(graph.getValueOfEdge(2, 4), equalTo(3));
        assertThat(graph.getValueOfEdge(2, 4), equalTo(3));
        assertThat(graph.getValueOfEdge(3, 1), equalTo(4));
        assertThat(graph.getValueOfEdge(3, 4), equalTo(5));
        assertThat(graph.getValueOfEdge(3, 6), equalTo(4));
        assertThat(graph.getValueOfEdge(4, 2), equalTo(6));
        assertThat(graph.getValueOfEdge(4, 3), equalTo(7));
        assertThat(graph.getValueOfEdge(4, 4), equalTo(8));
        assertThat(graph.getValueOfEdge(4, 5), equalTo(9));

        assertThat(pathExists(1,5, graph), is(true));
        assertThat(pathExists(4,6, graph), is(true));

        dfs(1, graph);
        System.out.println();
        dfs(4, graph);
        System.out.println();
        bfs(4, graph);
        System.out.println();
        bfs(3, graph);
    }

    @Test
    void testSimpleTreeOrderedInput() {
        Ontology<Integer, Integer> graph = Ontology.<Integer, Integer>builder()
                .putEdgeValue(1, 0, 2)
                .putEdgeValue(1, 0, 3)
                .putEdgeValue(2, 0, 4)
                .putEdgeValue(2, 0, 5)
                .putEdgeValue(5, 0, 6)
                .build();

        System.out.println("Children of 1: " +  graph.successors(1));

        assertThat(graph.successors(1), equalTo(Set.of(2, 3)));
        assertThat(graph.successors(2), equalTo(Set.of(4, 5)));
        assertThat(graph.successors(3), equalTo(Set.of()));
        assertThat(graph.successors(4), equalTo(Set.of()));
        assertThat(graph.successors(5), equalTo(Set.of(6)));
        assertThat(graph.successors(6), equalTo(Set.of()));

        assertThat(graph.predecessors(1), equalTo(Set.of()));
        assertThat(graph.predecessors(2), equalTo(Set.of(1)));
        assertThat(graph.predecessors(3), equalTo(Set.of(1)));
        assertThat(graph.predecessors(4), equalTo(Set.of(2)));
        assertThat(graph.predecessors(5), equalTo(Set.of(2)));
        assertThat(graph.predecessors(6), equalTo(Set.of(5)));

        assertThat(graph.isRootNode(1), is(true));
        assertThat(graph.getRootNodes(), equalTo(Set.of(1)));

        assertThat(graph.isLeafNode(1), is(false));
        assertThat(graph.isLeafNode(2), is(false));
        assertThat(graph.isLeafNode(3), is(true));
        assertThat(graph.isLeafNode(4), is(true));
        assertThat(graph.isLeafNode(5), is(false));
        assertThat(graph.isLeafNode(6), is(true));
    }

    @Test
    void testSimpleTreeUnorderedInput() {
        Ontology<Integer, Integer> graph = Ontology.<Integer, Integer>builder()
                .putEdgeValue(2, 0, 4)
                .putEdgeValue(1, 0, 2)
                .putEdgeValue(5, 0, 6)
                .putEdgeValue(2, 0, 5)
                .putEdgeValue(1, 0, 3)
                .build();

        System.out.println("Children of 1: " +  graph.successors(1));

        assertThat(graph.predecessors(1), equalTo(Set.of(2, 3)));
        assertThat(graph.predecessors(2), equalTo(Set.of(4, 5)));
        assertThat(graph.predecessors(3), equalTo(Set.of()));
        assertThat(graph.predecessors(4), equalTo(Set.of()));
        assertThat(graph.predecessors(5), equalTo(Set.of(6)));
        assertThat(graph.predecessors(6), equalTo(Set.of()));

        assertThat(graph.isLeafNode(1), is(true));
        assertThat(graph.getLeafNodes(), equalTo(Set.of(1)));

        assertThat(graph.getRootNodes(), equalTo(Set.of(3, 4, 6)));

        assertThat(graph.isRootNode(1), is(false));
        assertThat(graph.isRootNode(2), is(false));
        assertThat(graph.isRootNode(3), is(true));
        assertThat(graph.isRootNode(4), is(true));
        assertThat(graph.isRootNode(5), is(false));
        assertThat(graph.isRootNode(6), is(true));
    }

    @Test
    void testValueOfNonExistentEdge() {
        Ontology<Integer, Integer> graph = Ontology.<Integer, Integer>builder().build();
        assertThat(graph.getValueOfEdge(1, 2), nullValue());
    }

    @Test
    void traverseUpTreeAndGetEdgeValue() {
        Instant start = Instant.now();
        Map<String, Node> hpNodes = hp.getNodes().stream().filter(node -> node.getId().contains("HP_"))
                .map(node -> Node.of(node.getId(), node.getLabel()))
                .collect(Collectors.toMap(Node::getId, Function.identity()));
        Node isA = Node.of("RO:0000111", "is_a");

        ImmutableValueGraph.Builder<Node, Node> graphBuilder = ValueGraphBuilder
                .directed()
                .<Node, Node>immutable();
                // Ontology needs to become a CsrDirectedGraph and then a new Ontology needs to have reversed bfs/dfs/root/leaf operations
//        Ontology<Node, Node> graph = Ontology.<Node, Node>builder()

        hp.getEdges().stream().filter(edge -> edge.getSub().contains("HP_"))
                .forEach(edge -> {
                    Node subject = hpNodes.get(edge.getSub());
                    Node object = hpNodes.get(edge.getObj());
                    if (subject != null && object != null) {
                        graphBuilder.putEdgeValue(object, subject, isA);
                    }
                });
        ValueGraph<Node, Node> graph = graphBuilder.build();
        Instant end = Instant.now();
        System.out.println("Loaded hpo with " + graph.nodes().size() + " nodes in " + Duration.between(start, end).toMillis() + "ms");
        Node abnormalCirculatingHormoneConcentration = Node.of("http://purl.obolibrary.org/obo/HP_0003117", "Abnormal circulating hormone concentration");

        System.out.println("Guava sucessors" + graph.successors(abnormalCirculatingHormoneConcentration));
        System.out.println("Guava predecessors" + graph.predecessors(abnormalCirculatingHormoneConcentration));

    }

    @Test
    void depthFirstTraversal() {
        Node beer = Node.of("BO:00001", "Beer");
        Node ipa = Node.of("BO:00002", "IPA");
        Node westCoastIpa = Node.of("BO:00003", "West Coast IPA");
        Node gooseIsland = Node.of("BO:00004", "Goose Island");
        Node titanIpa = Node.of("BO:00005", "Titan IPA");
        Node newEnglandIpa = Node.of("BO:00006", "New England IPA");
        Node cloudwaterIpa = Node.of("BO:00007", "Cloudwater IPA");


        Node isA = Node.of("RO:0000111", "is_a");

//        ImmutableValueGraph<Node, Node> ontology = ValueGraphBuilder
//                .directed()
//                .<Node, Node>immutable()
        Ontology<Node, Node> ontology = Ontology.<Node, Node>builder()
                .putEdgeValue(ipa, isA, beer)
                .putEdgeValue(westCoastIpa, isA, ipa)
                .putEdgeValue(newEnglandIpa, isA, ipa)
                .putEdgeValue(cloudwaterIpa, isA, newEnglandIpa)
                .putEdgeValue(gooseIsland, isA, westCoastIpa)
                .putEdgeValue(titanIpa, isA, westCoastIpa)
                .build();

        System.out.println("Depth First Search");
        dfs(beer, ontology);

        System.out.printf("%nBreadth First Search%n");
        bfs(beer, ontology);

        assertThat(ontology.isRootNode(beer), is(true));
        assertThat(ontology.getRootNodes(), equalTo(Set.of(beer)));

        assertThat(ontology.getLeafNodes(), equalTo(Set.of(cloudwaterIpa, gooseIsland, titanIpa)));

        assertThat(descendents(newEnglandIpa, ontology), equalTo(ImmutableSet.of(newEnglandIpa, cloudwaterIpa)));
        assertThat(descendents(westCoastIpa, ontology), equalTo(ImmutableSet.of(westCoastIpa, titanIpa, gooseIsland)));

        assertThat(ancestors(titanIpa, ontology), equalTo(ImmutableSet.of(titanIpa, westCoastIpa, ipa, beer)));
        assertThat(ancestors(cloudwaterIpa, ontology), equalTo(ImmutableSet.of(cloudwaterIpa, newEnglandIpa, ipa, beer)));

        assertThat(pathExists(titanIpa, gooseIsland, ontology), is(false));
        assertThat(pathExists(ipa, gooseIsland, ontology), is(true));
        assertThat(pathExists(westCoastIpa, gooseIsland, ontology), is(true));
        assertThat(pathExists(gooseIsland, westCoastIpa, ontology), is(false));
        assertThat(pathExists(newEnglandIpa, gooseIsland, ontology), is(false));
        assertThat(pathExists(beer, gooseIsland, ontology), is(true));

        NaiveLcaFinder<Node, Node> lcaFinder = new NaiveLcaFinder<>(ontology);
        assertThat(ontology.getLCA(titanIpa, gooseIsland), equalTo(westCoastIpa));
        assertThat(ontology.getLCA(cloudwaterIpa, gooseIsland), equalTo(ipa));
        assertThat(ontology.getLCA(cloudwaterIpa, westCoastIpa), equalTo(ipa));
        assertThat(ontology.getLCA(cloudwaterIpa, ipa), equalTo(ipa));
    }

    <N, V> void dfs(N start, Ontology<N, V> graph) {
        Set<N> seen = new HashSet<>(graph.nodeCount());
        Deque<N> deque = new ArrayDeque<>(graph.nodeCount());
        deque.push(start);
        N current;
        while(!deque.isEmpty()) {
            current = deque.pop();
            System.out.printf("%s->", current);
            if (!seen.contains(current)){
                seen.add(current);
                for (N node : graph.successors(current)) {
                    deque.push(node);
                }
            }
        }
    }

    <N, V> void bfs(N start, Ontology<N, V> graph) {
        Set<N> seen = new HashSet<>(graph.nodeCount());
        Deque<N> deque = new ArrayDeque<>(graph.nodeCount());
        deque.push(start);
        N current;
        while(!deque.isEmpty()) {
            current = deque.pop();
            System.out.printf("%s->", current);
            if (!seen.contains(current)) {
                seen.add(current);
                deque.addAll(graph.successors(current));
            }
        }
    }

    <N, V> Set<N> descendents(N node, Ontology<N, V> graph) {
        Deque<N> deque = new ArrayDeque<>(graph.nodeCount());
        deque.push(node);
        Set<N> descendents = new LinkedHashSet<>();
        N current;
        while(!deque.isEmpty()) {
            current = deque.pop();
            if (!descendents.contains(current)) {
                descendents.add(current);
                for (N decendent : graph.successors(current)) {
                    deque.push(decendent);
                }
            }
        }
        return descendents;
    }

    <N, V> Set<N> ancestors(N node, Ontology<N, V> graph) {
        Deque<N> deque = new ArrayDeque<>(graph.nodeCount());
        deque.push(node);
        Set<N> ancestors = new LinkedHashSet<>();
        N current;
        while(!deque.isEmpty()) {
            current = deque.pop();
            if (!ancestors.contains(current)) {
                ancestors.add(current);
                for (N predecessor : graph.predecessors(current)) {
                    deque.push(predecessor);
                }
            }
        }
        return ancestors;
    }

    <N, V> boolean pathExists(N start, N end, Ontology<N, V> graph) {
        if (start.equals(end)) {
            return false;
        }
        // BFS
        Set<N> seen = new HashSet<>(graph.nodeCount());
        Deque<N> deque = new ArrayDeque<>(graph.nodeCount());
        deque.push(start);
        N current;
        while(!deque.isEmpty()) {
            current = deque.pop();
            if (current.equals(end)) {
                return true;
            }
            if (!seen.contains(current)) {
                seen.add(current);
                deque.addAll(graph.successors(current));
            }
        }

        return false;
    }

    <N, V> N getLca(N a, N b, Ontology<N, V> graph) {
        // need to implement predecessors(N node)
//        The algorithm for a DAG:
//
//        1. Start at each of nodes you wish to find the lca for (a and b)
//        2. Create sets aSet containing a, and bSet containing b
//        3. If either set intersects with the union of the other sets previous values (i.e. the set of notes visited) then
//        that intersection is LCA. if there are multiple intersections then the earliest one added is the LCA.
//        4. Repeat from step 3, with aSet now the parents of everything in aSet, and bSet the parents of everything in bSet
//        5. If there are no more parents to descend to then there is no LCA

        return null;
    }
}