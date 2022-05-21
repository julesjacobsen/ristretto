package org.monarchinitiative.ristretto;

import com.google.common.collect.AbstractIterator;

import java.util.*;

import static java.util.Objects.requireNonNull;

/**
 * Directed Value graph implemented using Compressed Sparse Row.
 *
 * @author Jules Jacobsen <j.jacobsen@qmul.ac.uk>
 */
public class CsrDirectedGraph<N, V> {

    // perhaps this is best kept at the Graph level and the CsrMatrix (this class) should just require this position.
    private final Map<N, Integer> nodesIndex;

    private final int[] offsets;
    private final N[] edges;
    private final V[] values;

    private CsrDirectedGraph(Map<N, Integer> nodesIndex, int[] offsets, N[] edges, V[] values) {
        this.nodesIndex = nodesIndex;
        this.offsets = offsets;
        this.edges = edges;
        this.values = values;
    }

    public int nodeCount() {
        return nodesIndex.size();
    }

    public Set<N> nodes() {
        return nodesIndex.keySet();
    }

    public int edgeCount() {
        return edges.length;
    }

    public Set<N> successors(N node) {
        // need to maintain order otherwise valueOfEdge
        return new AbstractSet<>() {
            private final N[] successors = successorsAsArray(node);

            @Override
            public Iterator<N> iterator() {
                return new ArrayIterator<>(successors);
            }

            @Override
            public int size() {
                return successors.length;
            }
        };
    }

    private N[] successorsAsArray(N node) {
        int nodeIndex = nodesIndex.get(node);
        return Arrays.copyOfRange(edges, offsets[nodeIndex], offsets[nodeIndex + 1]);
    }

    public int outDegree(N node) {
        int nodeIndex = nodesIndex.get(node);
        return offsets[nodeIndex + 1] - offsets[nodeIndex];
    }

    public boolean hasEdgeConnecting(N nodeU, N nodeV) {
        return successors(requireNonNull(nodeU)).contains(requireNonNull(nodeV));
    }

    public V getValueOfEdge(N nodeU, N nodeV) {
        requireNonNull(nodeU);
        requireNonNull(nodeV);

        if (!nodesIndex.containsKey(nodeU) || !nodesIndex.containsKey(nodeV)) {
            return null;
        }

        int nodeIndex = nodesIndex.get(nodeU);
        N[] successors = successorsAsArray(nodeU);
        // not efficient for large numbers of successors - use binary search if these can be ordered when graph is created
        for (int i = 0; i < successors.length; i++) {
            N node = successors[i];
            if (nodeV.equals(node)) {
                return values[offsets[nodeIndex] + i];
            }
        }
        return null;
    }

    //TODO: consider copying the Guava Graph API
    public static <N, V> CsrDirectedGraph.Builder<N, V> builder() {
        return new CsrDirectedGraph.Builder<>();
    }

    // https://db.in.tum.de/teaching/ws1718/seminarHauptspeicherdbs/paper/valiyev.pdf
    public static class Builder<N, V> {

        // adjacency lists for every forward / reverse edge
        private Map<N, Set<ValueEdge<N, V>>> graph = new TreeMap<>();

        public CsrDirectedGraph.Builder<N, V> putEdgeValue(N nodeA, N nodeB, V value) {
            requireNonNull(nodeA);
            requireNonNull(nodeB);
            requireNonNull(value);

            // this is forwards-only e.g. A -> B  for a directed graph.
            Set<ValueEdge<N, V>> edges1;
            if (graph.containsKey(nodeA)) {
                edges1 = graph.get(nodeA);
            } else {
                edges1 = new LinkedHashSet<>();
                graph.put(nodeA, edges1);
            }
            edges1.add(ValueEdge.of(nodeA, value, nodeB));

            // ensure nodeB is also represented as this might be a leaf node and only occur here
            graph.computeIfAbsent(nodeB, node -> new LinkedHashSet<>());

            return this;
        }

        public CsrDirectedGraph<N, V> build() {
            // use ImmutableMap the getNodes() can return the view on the keySet
            Map<N, Integer> nodesIndex = new LinkedHashMap<>(graph.size());

            int index = 0;
            for (N n : graph.keySet()) {
                nodesIndex.put(n, index++);
            }

            int edgeCount = 0;
            for (Set<ValueEdge<N, V>> edge : graph.values()) {
                edgeCount += edge.size();
            }

            int[] offsets = new int[graph.size() + 1];
            N[] successors = (N[]) new Object[edgeCount];
            V[] values = (V[]) new Object[edgeCount];

            int currentEdgeIndex = 0;
            for (Map.Entry<N, Set<ValueEdge<N, V>>> entry : graph.entrySet()) {
                Integer nodeIndex = nodesIndex.get(entry.getKey());
                for (ValueEdge<N, V> valueEdge : entry.getValue()) {
                    successors[currentEdgeIndex] = valueEdge.getNodeB();
                    values[currentEdgeIndex] = valueEdge.getValue();
                    currentEdgeIndex++;
                }
                offsets[nodeIndex + 1] = currentEdgeIndex;
            }

            return new CsrDirectedGraph<>(nodesIndex, offsets, successors, values);
        }
    }


    private static final class ArrayIterator<E> implements Iterator<E> {

        private final E[] array;
        private final int size;
        private int cursor;

        ArrayIterator(E[] array) {
            this.array = array;
            this.size = array.length;
            this.cursor = 0;
        }

        public boolean hasNext() {
            return cursor != size;
        }

        public E next() {
            try {
                int i = cursor;
                E next = array[i];
                cursor = i + 1;
                return next;
            } catch (IndexOutOfBoundsException e) {
                throw new NoSuchElementException();
            }
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        public void add(E e) {
            throw new UnsupportedOperationException();
        }
    }
}
