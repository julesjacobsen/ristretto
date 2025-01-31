package org.monarchinitiative.ristretto.core;

import org.geneontology.obographs.core.io.OgJsonReader;
import org.geneontology.obographs.core.model.GraphDocument;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.ristretto.core.graph.NaiveLcaFinder;
import org.monarchinitiative.ristretto.core.graph.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Jules Jacobsen <j.jacobsen@qmul.ac.uk>
 */
class OntologyTest {

    private static final Logger logger = LoggerFactory.getLogger(OntologyTest.class);

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
        Node hasSubClass = Node.of("RO:0000111", "hasSubClass");

        Ontology<Node, Node> graph = Ontology.<Node, Node>builder()
                .addAxiom(ipa, isA, beer)
//                .addAxiom(beer, hasSubClass, ipa)
                .addAxiom(westCoastIpa, isA, ipa)
                .addAxiom(newEnglandIpa, isA, ipa)
                .addAxiom(cloudwaterIpa, isA, newEnglandIpa)
                .addAxiom(gooseIsland, isA, westCoastIpa)
                .addAxiom(titanIpa, isA, westCoastIpa)
                .build();

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

        assertThat(graph.lca(beer, ipa), equalTo(beer));
        assertThat(graph.lca(ipa, ipa), equalTo(ipa));
        assertThat(graph.lca(ipa, titanIpa), equalTo(ipa));
        assertThat(graph.lca(titanIpa, ipa), equalTo(ipa));
        assertThat(graph.lca(newEnglandIpa, westCoastIpa), equalTo(ipa));
        assertThat(graph.lca(gooseIsland, titanIpa), equalTo(westCoastIpa));
        assertThat(graph.lca(cloudwaterIpa, titanIpa), equalTo(ipa));
        assertThat(graph.lca(cloudwaterIpa, newEnglandIpa), equalTo(newEnglandIpa));

        assertThat(graph.ancestors(titanIpa).contains(westCoastIpa), is(true));
        assertThat(graph.isAncestorOf(titanIpa, westCoastIpa), is(true));

        assertThat(graph.ancestors(titanIpa).contains(newEnglandIpa), is(false));
        assertThat(graph.isAncestorOf(titanIpa, newEnglandIpa), is(false));

        assertThat(graph.ancestors(cloudwaterIpa).contains(newEnglandIpa), is(true));
        assertThat(graph.isAncestorOf(cloudwaterIpa, newEnglandIpa), is(true));

        assertThat(graph.ancestors(cloudwaterIpa).contains(westCoastIpa), is(false));
        assertThat(graph.isAncestorOf(cloudwaterIpa, westCoastIpa), is(false));

        assertThat(graph.isAncestorOf(titanIpa, ipa), is(true));
        assertThat(graph.isAncestorOf(cloudwaterIpa, ipa), is(true));
    }

    @Test
    void testDirectedCircularGraph() {
        Ontology<Integer, Integer> graph = Ontology.<Integer, Integer>builder()
                .addEdge(1, 0, 2)
                .addEdge(1, 1, 3)
                .addEdge(1, 2, 4)
                .addEdge(2, 3, 4)
                .addEdge(3, 4, 1)
                .addEdge(3, 5, 4)
                .addEdge(4, 6, 2)
                .addEdge(4, 7, 3)
                .addEdge(4, 8, 4)
                .addEdge(4, 9, 5)
                .build();

        System.out.println("Children of 1: " + graph.successors(1));

        assertThat(graph.successors(1), equalTo(Set.of(2, 3, 4)));
        assertThat(graph.successors(2), equalTo(Set.of(4)));
        assertThat(graph.successors(3), equalTo(Set.of(1, 4)));
        assertThat(graph.successors(4), equalTo(Set.of(2, 3, 4, 5)));
        assertThat(graph.successors(5), equalTo(Set.of()));

        assertThat(graph.isLeafNode(1), is(false));
        assertThat(graph.isLeafNode(2), is(false));
        assertThat(graph.isLeafNode(3), is(false));
        assertThat(graph.isLeafNode(4), is(false));
        assertThat(graph.isLeafNode(5), is(true));

        assertThat(graph.inDegree(1), equalTo(1));
        assertThat(graph.outDegree(1), equalTo(3));

        assertThat(graph.inDegree(2), equalTo(2));
        assertThat(graph.outDegree(2), equalTo(1));

        assertThat(graph.inDegree(3), equalTo(2));
        assertThat(graph.outDegree(3), equalTo(2));

        assertThat(graph.inDegree(4), equalTo(4));
        assertThat(graph.outDegree(4), equalTo(4));

        assertThat(graph.inDegree(5), equalTo(1));
        assertThat(graph.outDegree(5), equalTo(0));

        assertThat(graph.valueOfEdge(1, 2), equalTo(0));
        assertThat(graph.valueOfEdge(1, 3), equalTo(1));
        assertThat(graph.valueOfEdge(1, 4), equalTo(2));
        assertThat(graph.valueOfEdge(2, 4), equalTo(3));
        assertThat(graph.valueOfEdge(2, 4), equalTo(3));
        assertThat(graph.valueOfEdge(3, 1), equalTo(4));
        assertThat(graph.valueOfEdge(3, 4), equalTo(5));
        assertThat(graph.valueOfEdge(4, 2), equalTo(6));
        assertThat(graph.valueOfEdge(4, 3), equalTo(7));
        assertThat(graph.valueOfEdge(4, 4), equalTo(8));
        assertThat(graph.valueOfEdge(4, 5), equalTo(9));

        assertThat(pathExists(1, 5, graph), is(true));
        assertThat(pathExists(5, 1, graph), is(false));
        assertThat(pathExists(2, 3, graph), is(true));
        assertThat(pathExists(3, 2, graph), is(true));

        assertThat(pathExists(1, 1, graph), is(true));
//        assertThat(pathExists(5,5, graph), is(false));

        assertThat(graph.lca(2, 3), equalTo(1));


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
        Ontology<Integer, String> graph = Ontology.<Integer, String>builder()
                .addEdge(1, "hasChild", 2)
                .addEdge(1, "hasChild", 3)
                .addEdge(2, "hasChild", 4)
                .addEdge(2, "hasChild", 5)
                .addEdge(5, "hasChild", 6)
                .build();
//        System.out.println(graph.edges());

        System.out.println("Children of 1: " + graph.successors(1));

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
        assertThat(graph.rootNodes(), equalTo(Set.of(1)));

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
                .addEdge(2, 0, 4)
                .addEdge(1, 0, 2)
                .addEdge(5, 0, 6)
                .addEdge(2, 0, 5)
                .addEdge(1, 0, 3)
                .build();

        System.out.println("Children of 1: " + graph.successors(1));

        assertThat(graph.successors(1), equalTo(Set.of(2, 3)));
        assertThat(graph.successors(2), equalTo(Set.of(4, 5)));
        assertThat(graph.successors(3), equalTo(Set.of()));
        assertThat(graph.successors(4), equalTo(Set.of()));
        assertThat(graph.successors(5), equalTo(Set.of(6)));
        assertThat(graph.successors(6), equalTo(Set.of()));

        assertThat(graph.isRootNode(1), is(true));
        assertThat(graph.rootNodes(), equalTo(Set.of(1)));

        assertThat(graph.leafNodes(), equalTo(Set.of(3, 4, 6)));

        assertThat(graph.isLeafNode(1), is(false));
        assertThat(graph.isLeafNode(2), is(false));
        assertThat(graph.isLeafNode(3), is(true));
        assertThat(graph.isLeafNode(4), is(true));
        assertThat(graph.isLeafNode(5), is(false));
        assertThat(graph.isLeafNode(6), is(true));
    }

    @Test
    void testValueOfNonExistentEdge() {
        Ontology<Integer, Integer> graph = Ontology.<Integer, Integer>builder().build();
        assertThat(graph.valueOfEdge(1, 2), nullValue());
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
                .addAxiom(ipa, isA, beer)
                .addAxiom(westCoastIpa, isA, ipa)
                .addAxiom(newEnglandIpa, isA, ipa)
                .addAxiom(cloudwaterIpa, isA, newEnglandIpa)
                .addAxiom(gooseIsland, isA, westCoastIpa)
                .addAxiom(titanIpa, isA, westCoastIpa)
                .build();

        System.out.println("Depth First Search");
        dfs(beer, ontology);

        System.out.printf("%nBreadth First Search%n");
        bfs(beer, ontology);

        assertThat(ontology.isRootNode(beer), is(true));
        assertThat(ontology.rootNodes(), equalTo(Set.of(beer)));

        assertThat(ontology.leafNodes(), equalTo(Set.of(cloudwaterIpa, gooseIsland, titanIpa)));

        assertThat(descendants(newEnglandIpa, ontology), equalTo(Set.of(newEnglandIpa, cloudwaterIpa)));
        assertThat(descendants(westCoastIpa, ontology), equalTo(Set.of(westCoastIpa, titanIpa, gooseIsland)));

        assertThat(ancestors(titanIpa, ontology), equalTo(Set.of(titanIpa, westCoastIpa, ipa, beer)));
        assertThat(ancestors(cloudwaterIpa, ontology), equalTo(Set.of(cloudwaterIpa, newEnglandIpa, ipa, beer)));

        assertThat(pathExists(titanIpa, gooseIsland, ontology), is(false));
        assertThat(pathExists(ipa, gooseIsland, ontology), is(true));
        assertThat(pathExists(westCoastIpa, gooseIsland, ontology), is(true));
        assertThat(pathExists(gooseIsland, westCoastIpa, ontology), is(false));
        assertThat(pathExists(newEnglandIpa, gooseIsland, ontology), is(false));
        assertThat(pathExists(beer, gooseIsland, ontology), is(true));

        NaiveLcaFinder<Node, Node> lcaFinder = new NaiveLcaFinder<>(ontology);
        assertThat(ontology.lca(titanIpa, gooseIsland), equalTo(westCoastIpa));
        assertThat(ontology.lca(cloudwaterIpa, gooseIsland), equalTo(ipa));
        assertThat(ontology.lca(cloudwaterIpa, westCoastIpa), equalTo(ipa));
        assertThat(ontology.lca(cloudwaterIpa, ipa), equalTo(ipa));
    }

    @Test
    void testMaxoLoad() {
        GraphDocument graphDocument = null;
        try {
            graphDocument = OgJsonReader.readFile(Path.of("src/test/resources/maxo.json").toFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
        var MAXO = graphDocument.getGraphs().get(0);

        Map<String, Node> hpNodes = MAXO.getNodes().stream()
//                .filter(node -> node.getId().contains("MAXO_"))
                .map(node -> Node.of(node.getId(), node.getLabel()))
                .collect(Collectors.toMap(Node::id, Function.identity()));
        Node isA = Node.of("RO:0000111", "is_a");

        Ontology.Builder<Node, Node> ontologyBuilder = Ontology.<Node, Node>builder();
        ontologyBuilder.id(MAXO.getId());
        MAXO.getEdges().stream()
//                .filter(edge -> edge.getSub().contains("MAXO_"))
                .forEach(edge -> {
                    Node subject = hpNodes.get(edge.getSub());
                    Node object = hpNodes.get(edge.getObj());
                    if (!edge.getPred().startsWith("http://") && !edge.getPred().equals("is_a")) {
//                        System.out.println(edge);
                    }
                    Node predicate;
                    if (edge.getPred().equals("is_a")) {
                        predicate = isA;
                    } else {
                        predicate = hpNodes.get(edge.getPred());
                    }
//                    System.out.println(subject + " - " + predicate + " -> " + object);
                    if (subject != null && object != null) {
                        ontologyBuilder.addAxiom(subject, Objects.requireNonNullElse(predicate, isA), object);
                    }
                });
        var ontology = ontologyBuilder.build();
        var ancestors = ontology.ancestors(Node.of("http://purl.obolibrary.org/obo/MAXO_0000486", "distraction osteogenesis"));
        ancestors.forEach(System.out::println);
    }
    
    <N, V> void dfs(N start, Ontology<N, V> graph) {
        Set<N> seen = new HashSet<>(graph.nodeCount());
        Deque<N> deque = new ArrayDeque<>(graph.nodeCount());
        deque.push(start);
        N current;
        while (!deque.isEmpty()) {
            current = deque.pop();
            System.out.printf("%s->", current);
            if (!seen.contains(current)) {
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
        while (!deque.isEmpty()) {
            current = deque.pop();
            System.out.printf("%s->", current);
            if (!seen.contains(current)) {
                seen.add(current);
                deque.addAll(graph.successors(current));
            }
        }
    }

    <N, V> Set<N> descendants(N node, Ontology<N, V> graph) {
        Deque<N> deque = new ArrayDeque<>();
        deque.push(node);
        Set<N> descendants = new LinkedHashSet<>();
        N current;
        while (!deque.isEmpty()) {
            current = deque.pop();
            if (!descendants.contains(current)) {
                descendants.add(current);
                for (N descendant : graph.successors(current)) {
                    deque.push(descendant);
                }
            }
        }
        return descendants;
    }

    <N, V> Set<N> ancestors(N node, Ontology<N, V> graph) {
        Deque<N> deque = new ArrayDeque<>();
        deque.push(node);
        Set<N> ancestors = new LinkedHashSet<>();
        N current;
        while (!deque.isEmpty()) {
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
//        if (start.equals(end)) {
//            return false;
//        }
        // BFS
        Set<N> seen = new HashSet<>(graph.nodeCount());
        Deque<N> deque = new ArrayDeque<>(graph.nodeCount());
        deque.push(start);
        N current;
        while (!deque.isEmpty()) {
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