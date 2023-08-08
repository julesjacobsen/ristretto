package org.monarchinitiative.ristretto;


import java.util.*;

import static java.util.Objects.requireNonNull;

/**
 * Directed Value graph implemented using Compressed Sparse Row. The maximum size of this is the Integer max value i.e.
 * 2^31 or 2,147,483,647 edges.
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
        this.nodesIndex = Collections.unmodifiableMap(nodesIndex);
        this.offsets = offsets;
        this.edges = edges;
        this.values = values;
    }

    public int nodeCount() {
        return nodesIndex.size();
    }

    public boolean containsNode(N node) {
        return nodesIndex.containsKey(node);
    }

    public Set<N> nodes() {
        return nodesIndex.keySet();
    }

    public int edgeCount() {
        return edges.length;
    }

    public Set<N> successors(N node) {
        // need to maintain order otherwise valueOfEdge
        int nodeIndex = getNodeIndex(node);
        if (nodeIndex < 0) {
            throw new NoSuchElementException("Node not found: " + node);
        }

        return new AbstractSet<>() {
            private final int start = offsets[nodeIndex];
            private final int end = offsets[nodeIndex + 1];

            @Override
            public Iterator<N> iterator() {
                return new ArrayIterator<>(edges, start, end);
            }

            @Override
            public int size() {
                return end - start;
            }
        };
    }

    public int outDegree(N node) {
        int nodeIndex = getNodeIndex(node);
        return offsets[nodeIndex + 1] - offsets[nodeIndex];
    }

    private int getNodeIndex(N node) {
        return nodesIndex.getOrDefault(node, -1);
    }

    public boolean hasEdgeConnecting(N nodeU, N nodeV) {
        return successors(requireNonNull(nodeU)).contains(requireNonNull(nodeV));
    }

    public V getValueOfEdge(N nodeU, N nodeV) {
        requireNonNull(nodeU);
        requireNonNull(nodeV);

        if (!containsNode(nodeU) || !containsNode(nodeV)) {
            return null;
        }

        int nodeIndex = getNodeIndex(nodeU);
        int start = offsets[nodeIndex];
        int end = offsets[nodeIndex + 1];
        // not efficient for large numbers of successors - use binary search if these can be ordered when graph is created
        for (int i = 0; i < end - start; i++) {
            N node = edges[start + i];
            if (nodeV.equals(node)) {
                return values[start + i];
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

        public CsrDirectedGraph.Builder<N, V> putEdgeValue(N source, N target, V value) {
            requireNonNull(source);
            requireNonNull(target);
            requireNonNull(value);

            // this is forwards-only e.g. A -> B  for a directed graph.
            Set<ValueEdge<N, V>> sourceEdges;
            if (graph.containsKey(source)) {
                sourceEdges = graph.get(source);
            } else {
                sourceEdges = new LinkedHashSet<>();
                graph.put(source, sourceEdges);
            }
            sourceEdges.add(ValueEdge.of(source, value, target));

            // ensure target is also represented as this might be a leaf node and only occur here
            graph.computeIfAbsent(target, node -> new LinkedHashSet<>());

            return this;
        }

        public CsrDirectedGraph<N, V> build() {
            // use ImmutableMap the getNodes() can return the view on the keySet
            Map<N, Integer> nodesIndex = new LinkedHashMap<>(graph.size());

            int index = 0;
            for (N n : graph.keySet()) {
                nodesIndex.put(n, index++);
            }

            int[] offsets = new int[graph.size() + 1];

            int edgeCount = 0;
            for (Set<ValueEdge<N, V>> edge : graph.values()) {
                edgeCount += edge.size();
            }
            @SuppressWarnings("unchecked")
            N[] successors = (N[]) new Object[edgeCount];
            @SuppressWarnings("unchecked")
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
            // clean-up?
            graph = null;
            return new CsrDirectedGraph<>(nodesIndex, offsets, successors, values);
        }
    }


    private static final class ArrayIterator<E> implements Iterator<E> {

        private final E[] array;
        private final int end;
        private int cursor;

        ArrayIterator(E[] array, int start, int end) {
            this.array = array;
            this.end = end;
            this.cursor = start;
        }

        public boolean hasNext() {
            return cursor < end;
        }

        public E next() {
            try {
                return array[cursor++];
            } catch (IndexOutOfBoundsException e) {
                throw new NoSuchElementException();
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }
}
