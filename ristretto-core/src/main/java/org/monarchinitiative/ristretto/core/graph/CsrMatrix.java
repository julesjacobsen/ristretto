package org.monarchinitiative.ristretto.core.graph;

import java.util.*;

import static java.util.Objects.requireNonNull;

/**
 * @author Jules Jacobsen <j.jacobsen@qmul.ac.uk>
 */
public class CsrMatrix<N, V> {

    // TODO: rename using rowIndex and columnIndex?
    //  As in https://en.wikipedia.org/wiki/Sparse_matrix#Compressed_sparse_row_.28CSR.2C_CRS_or_Yale_format.29
    private final int[] offsets;
    private final N[] edges;
    private final V[] values;

    private CsrMatrix(int[] offsets, N[] edges, V[] values) {
        this.offsets = offsets;
        this.edges = edges;
        this.values = values;
    }

    public static class Builder<N, V> {

        // adjacency lists for every forward / reverse edge
        private final Map<N, Set<ValueEdge<N, V>>> graph = new TreeMap<>();

        public CsrMatrix.Builder<N, V> putEdgeValue(N nodeA, N nodeB, V value) {
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

        public CsrMatrix<N, V> build() {
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

            return new CsrMatrix<>(offsets, successors, values);
        }
    }

}
