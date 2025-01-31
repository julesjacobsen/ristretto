package org.monarchinitiative.ristretto.core.graph;

import java.util.Objects;

/**
 * @author Jules Jacobsen <j.jacobsen@qmul.ac.uk>
 */
public class ValueEdge<N, V> {

    private final N nodeA;
    private final V value;
    private final N nodeB;

    private ValueEdge(N nodeA, V value, N nodeB) {
        this.nodeA = nodeA;
        this.value = value;
        this.nodeB = nodeB;
    }

    public static <N, V> ValueEdge <N, V> of(N nodeA, V value, N nodeB) {
        return new ValueEdge<>(nodeA, value, nodeB);
    }

    public N getNodeA() {
        return nodeA;
    }

    public V getValue() {
        return value;
    }

    public N getNodeB() {
        return nodeB;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ValueEdge)) return false;
        ValueEdge valueEdge = (ValueEdge) o;
        return Objects.equals(nodeA, valueEdge.nodeA) &&
                Objects.equals(value, valueEdge.value) &&
                Objects.equals(nodeB, valueEdge.nodeB);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeA, value, nodeB);
    }

    @Override
    public String toString() {
        return "Edge{" +
                "nodeA=" + nodeA +
                ", nodeB=" + nodeB +
                ", value=" + value +
                '}';
    }

}
