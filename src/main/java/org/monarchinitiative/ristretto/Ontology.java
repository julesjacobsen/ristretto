package org.monarchinitiative.ristretto;

import com.google.common.graph.*;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;

import static java.util.Objects.requireNonNull;

/**
 * @author Jules Jacobsen <j.jacobsen@qmul.ac.uk>
 */
public class Ontology<N, V> extends AbstractValueGraph<N, V> {

    private final String id;
    private final CsrDirectedGraph<N, V> forwardGraph;
    private final CsrDirectedGraph<N, V> reverseGraph;

    private final NaiveLcaFinder<N, V> lcaFinder;

    private Ontology(String id, CsrDirectedGraph<N, V> forwardGraph, CsrDirectedGraph<N, V> reverseGraph) {
        this.id = id;
        this.forwardGraph = forwardGraph;
        this.reverseGraph = reverseGraph;

        lcaFinder = new NaiveLcaFinder<>(this);
    }

    public String id() {
        return id;
    }

    public N getLCA(N a, N b) {
        return lcaFinder.getLCA(a, b);
    }

    public int nodeCount() {
        return forwardGraph.nodeCount();
    }

    @Override
    public long edgeCount() {
        return forwardGraph.edgeCount();
    }

    public boolean containsNode(N node) {
        return forwardGraph.containsNode(node);
    }

    @Override
    public Set<N> successors(N node) {
        // looking down the tree
        return forwardGraph.successors(node);
    }

    @Override
    public Set<N> predecessors(N node) {
        // looking up the tree
        return reverseGraph.successors(node);
    }

    public Set<N> getLeafNodes() {
        Set<N> set = new LinkedHashSet<>();
        for (N n : forwardGraph.nodes()) {
            if (isLeafNode(n)) {
                set.add(n);
            }
        }
        return set;
    }

    public boolean isLeafNode(N node) {
        return outDegree(node) == 0;
    }

    @Override
    public int outDegree(N node) {
        return forwardGraph.outDegree(node);
    }

    public Set<N> getRootNodes() {
        Set<N> set = new LinkedHashSet<>();
        for (N n : reverseGraph.nodes()) {
            if (isRootNode(n)) {
                set.add(n);
            }
        }
        return set;
    }

    public boolean isRootNode(N node) {
        return inDegree(node) == 0;
    }

    @Override
    public int inDegree(N node) {
        return reverseGraph.outDegree(node);
    }

    @Override
    public boolean hasEdgeConnecting(N nodeU, N nodeV) {
        return predecessors(requireNonNull(nodeU)).contains(requireNonNull(nodeV));
    }

    public V getValueOfEdge(N nodeU, N nodeV) {
        requireNonNull(nodeU);
        requireNonNull(nodeV);
        return forwardGraph.getValueOfEdge(nodeU, nodeV);
    }

    @Override
    public Set<N> nodes() {
        return forwardGraph.nodes();
    }

    @Override
    public boolean isDirected() {
        return true;
    }

    @Override
    public boolean allowsSelfLoops() {
        return true;
    }

    @Override
    public ElementOrder<N> nodeOrder() {
        return null;
    }

    @Override
    public Set<N> adjacentNodes(N node) {
        return forwardGraph.successors(node);
    }


    @Override
    public Optional<V> edgeValue(EndpointPair<N> endpoints) {
        return Optional.ofNullable(edgeValueOrDefault(endpoints.nodeU(), endpoints.nodeV(), null));
    }

    @Nullable
    @Override
    public V edgeValueOrDefault(N nodeU, N nodeV, @Nullable V defaultValue) {
        V value = forwardGraph.getValueOfEdge(requireNonNull(nodeU), requireNonNull(nodeV));
        return value == null ?  defaultValue : value;
    }

    @Nullable
    @Override
    public V edgeValueOrDefault(EndpointPair<N> endpoints, @Nullable V defaultValue) {
        return edgeValueOrDefault(endpoints.nodeU(), endpoints.nodeV(), defaultValue);
    }


    @Override
    public String toString() {
        return "Ontology{" +
                "forwardGraph=" + forwardGraph +
                ", reverseGraph=" + reverseGraph +
                '}';
    }

    public static <N extends Comparable<N>, V extends Comparable<V>> Builder<N, V> builder() {
        return new Builder<>();
    }

    // https://db.in.tum.de/teaching/ws1718/seminarHauptspeicherdbs/paper/valiyev.pdf
    public static class Builder<N extends Comparable<N>, V extends Comparable<V>> {

        private String id = "";
        private final CsrDirectedGraph.Builder<N, V> forwardGraph = CsrDirectedGraph.builder();
        private final CsrDirectedGraph.Builder<N, V> reverseGraph = CsrDirectedGraph.builder();

        public Builder<N, V> id(String id) {
            this.id = id;
            return this;
        }

        public Builder<N, V> addAxiom(N subject, V predicate, N object) {
            requireNonNull(subject);
            requireNonNull(object);
            requireNonNull(predicate);

            // this is forwards-only e.g. A -> B  for a directed graph.
            // in an ontology the direction is usually pointing 'up' the tree to the root, where the root is the common
            // ancestor of all terms. With assertions being made in the form:
            //     Subject Predicate Object
            //     IPA isA Beer  (isA == subClassOf)
            //     B - subClassOf -> A ()
            //     A - hasSubClass -> B  ()
            forwardGraph.putEdgeValue(object, subject, predicate);
            reverseGraph.putEdgeValue(subject, object, predicate);

            return this;
        }

        public Builder<N, V> addEdge(N source, V value, N target) {
            requireNonNull(source);
            requireNonNull(target);
            requireNonNull(value);

            // this is forwards-only e.g. A -> B  for a directed graph.
            // in an ontology the direction is usually pointing 'up' the tree to the root, where the root is the common
            // ancestor of all terms. With assertions being made in the form:
            //     Subject Predicate Object
            //     IPA isA Beer
            forwardGraph.putEdgeValue(source, target, value);
            reverseGraph.putEdgeValue(target, source, value);

            return this;
        }

        public Ontology<N, V> build() {
            return new Ontology<>(id, forwardGraph.build(), reverseGraph.build());
        }
    }

}
