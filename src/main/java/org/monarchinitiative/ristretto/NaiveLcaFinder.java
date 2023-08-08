package org.monarchinitiative.ristretto;

//import org.jgrapht.alg.lca.BinaryLiftingLCAFinder;
//import org.jgrapht.alg.lca.EulerTourRMQLCAFinder;
//import org.jgrapht.alg.lca.HeavyPathLCAFinder;
//import org.jgrapht.alg.lca.TarjanLCAFinder;

import java.lang.reflect.Array;
import java.util.*;

/**
 * Find the Lowest Common Ancestor of a directed graph.
 *
 * <p>
 * Find the LCA, defined as <i>Let $G = (V, E)$ be a DAG, and let $x, y \in V$ . Let $G x,y$ be the
 * subgraph of $G$ induced by the set of all common ancestors of $x$ and $y$. Define SLCA (x, y) to
 * be the set of out-degree 0 nodes (leafs) in $G x,y$. The lowest common ancestors of $x$ and $y$
 * are the elements of SLCA (x, y). This naive algorithm simply starts at $a$ and $b$, recursing
 * upwards to the root(s) of the DAG. Wherever the recursion paths cross we have found our LCA.</i>
 * from <i> Michael A. Bender, Martín Farach-Colton, Giridhar Pemmasani, Steven Skiena, Pavel
 * Sumazin, Lowest common ancestors in trees and directed acyclic graphs, Journal of Algorithms,
 * Volume 57, Issue 2, 2005, Pages 75-94, ISSN 0196-6774,
 * https://doi.org/10.1016/j.jalgor.2005.08.001. </i>
 *
 * <p>
 * The algorithm:
 *
 * <pre>
 * 1. Start at each of nodes you wish to find the lca for (a and b)
 * 2. Create sets aSet containing a, and bSet containing b
 * 3. If either set intersects with the union of the other sets previous values (i.e. the set of notes visited) then
 *    that intersection is LCA. if there are multiple intersections then the earliest one added is the LCA.
 * 4. Repeat from step 3, with aSet now the parents of everything in aSet, and bSet the parents of everything in bSet
 * 5. If there are no more parents to descend to then there is no LCA
 * </pre>
 * <p>
 * The rationale for this working is that in each iteration of the loop we are considering all the
 * ancestors of a that have a path of length n back to a, where n is the depth of the recursion. The
 * same is true of b.
 *
 * <p>
 * We start by checking if a == b.<br>
 * if not we look to see if there is any intersection between parents(a) and (parents(b) union b)
 * (and the same with a and b swapped)<br>
 * if not we look to see if there is any intersection between parents(parents(a)) and
 * (parents(parents(b)) union parents(b) union b) (and the same with a and b swapped)<br>
 * continues
 *
 * <p>
 * This means at the end of recursion n, we know if there is an LCA that has a path of &lt;=n to a
 * and b. Of course we may have to wait longer if the path to a is of length n, but the path to
 * b&gt;n. at the first loop we have a path of 0 length from the nodes we are considering as LCA to
 * their respective children which we wish to find the LCA for.
 *
 * <p>
 * Preprocessing Time complexity: $O(1)$<br>
 * Preprocessing Space complexity: $O(1)$<br>
 * Query Time complexity: $O(|V|)$<br>
 * Query Space complexity: $O(|V|)$<br>
 * </p>
 *
 * <p>
 * For trees or forests please use either {@link BinaryLiftingLCAFinder},
 * {@link HeavyPathLCAFinder}, {@link EulerTourRMQLCAFinder} or {@link TarjanLCAFinder}.
 * </p>
 *
 * @param <V> the graph vertex type
 * @param <N> the graph node/edge type
 * @author Leo Crawford
 * @author Alexandru Valeanu
 */
public class NaiveLcaFinder<N, V> {

    private final Ontology<N, V> graph;

    /**
     * Create a new instance of the naive LCA finder.
     *
     * @param graph the input graph
     */
    public NaiveLcaFinder(Ontology<N, V> graph) {
        this.graph = Objects.requireNonNull(graph, "Graph cannot be null");
    }

    /**
     * {@inheritDoc}
     */
    public N getLCA(N a, N b) {
        if (!graph.containsNode(a))
            throw new IllegalArgumentException("invalid vertex: " + a);

        if (!graph.containsNode(b))
            throw new IllegalArgumentException("invalid vertex: " + b);

        if (a.equals(b)) {
            return a;
        }
        return findLca(Set.of(a), Set.of(b), new LinkedHashSet<>(), new LinkedHashSet<>());
    }

    /**
     * {@inheritDoc}
     */
    public Set<N> getLCASet(N a, N b) {
        @SuppressWarnings("unchecked")
        Set<N>[] visitedSets = (Set<N>[]) Array.newInstance(Set.class, 2);
        // set of nodes visited from a
        visitedSets[0] = new LinkedHashSet<>();
        // set of nodes visited from b
        visitedSets[1] = new LinkedHashSet<>();

        doubleBfs(a, b, visitedSets);
        // all common ancestors of both a and b
        Set<N> intersection;

        // optimization trick: save the intersection using the smaller set
        if (visitedSets[0].size() < visitedSets[1].size()) {
            visitedSets[0].retainAll(visitedSets[1]);
            intersection = visitedSets[0];
        } else {
            visitedSets[1].retainAll(visitedSets[0]);
            intersection = visitedSets[1];
        }

        /*
         * Find the set of all non-leaves by iterating through the set of common ancestors. When we
         * encounter a node which is still part of the SLCA(a, b) we remove its parent(s).
         */
        Set<N> nonLeaves = new LinkedHashSet<>();
        for (N node : intersection) {
            for (N source : graph.predecessors(node)) {
                if (intersection.contains(source))
                    nonLeaves.add(source);
            }
        }

        // perform the actual removal of non-leaves
        intersection.removeAll(nonLeaves);
        return intersection;
    }

    /**
     * Perform a simultaneous bottom-up bfs from a and b, saving all visited nodes. Once a a node
     * has been visited from both a and b, it is no longer expanded in our search (we know that its
     * ancestors won't be part of the SLCA(x, y) set).
     */
    private void doubleBfs(N a, N b, Set<N>[] visitedSets) {
        @SuppressWarnings("unchecked")
        Queue<N>[] queues = (Queue<N>[]) Array.newInstance(Queue.class, 2);
        queues[0] = new ArrayDeque<>();
        queues[1] = new ArrayDeque<>();

        queues[0].add(a);
        queues[1].add(b);

        visitedSets[0].add(a);
        visitedSets[1].add(b);

        for (int ind = 0; !queues[0].isEmpty() || !queues[1].isEmpty(); ind ^= 1) {
            if (!queues[ind].isEmpty()) {
                N node = queues[ind].poll();

                if (!visitedSets[0].contains(node) || !visitedSets[1].contains(node)) {
                    for (N source : graph.predecessors(node)) {
                        if (!visitedSets[ind].contains(source)) {
                            queues[ind].add(source);
                            visitedSets[ind].add(source);
                        }
                    }
                }
            }
        }
    }

    /**
     * Recurse through the descendants of aSet and bSet looking for the LCA of a and b, which are
     * members of sets aSeenSet and bSeenSet respectively, along with all elements on the paths from
     * every member of aSet and bSet
     */
    private N findLca(Set<N> aSet, Set<N> bSet, LinkedHashSet<N> aSeenSet, LinkedHashSet<N> bSeenSet) {
        while (true) {
            // if there is no LCA...
            if ((aSet.isEmpty()) && (bSet.isEmpty())) {
                return null;
            }

            // does aSet intersect with bSeenSet
            if (!Collections.disjoint(aSet, bSeenSet)) {
                return overlappingMember(aSet, bSeenSet);
            }

            // does bSet intersect with aSeenSet
            if (!Collections.disjoint(bSet, aSeenSet)) {
                return overlappingMember(bSet, aSeenSet);
            }
            if (!Collections.disjoint(aSet, bSet)) {
                return overlappingMember(aSet, bSet);
            }

            aSeenSet.addAll(aSet);
            bSeenSet.addAll(bSet);

            aSet = allParents(aSet);

            // no point doing the same again (and it can stop us getting stuck in
            // an infinite loop)
            aSet.removeAll(aSeenSet);

            bSet = allParents(bSet);
            bSet.removeAll(bSeenSet);
        }
    }

    /**
     * Find the immediate parents of every item in the given set, and return a set containing all
     * those parents
     *
     * @param vertexSet the set of vertex to find parents of
     * @return a set of every parent of every vertex passed in
     */
    private Set<N> allParents(Set<N> vertexSet) {
        HashSet<N> result = new HashSet<>();
        for (N node : vertexSet) {
            result.addAll(graph.predecessors(node));
        }
        return result;
    }

    /**
     * Return a single vertex that is both in $x$ and $y$. If there is more than one then select the
     * first element from the iterator returned from $y$, after all the elements of $x$ have been
     * removed. this allows an orderedSet to be passed in to give predictable results.
     *
     * @param x set containing vertex
     * @param y set containing vertex, which may be ordered to give predictable results
     * @return the first element of $y$ that is also in $x$, or null if no such element
     */
    private N overlappingMember(Set<N> x, Set<N> y) {
        y.retainAll(x);
        return y.iterator().next();
    }

}
