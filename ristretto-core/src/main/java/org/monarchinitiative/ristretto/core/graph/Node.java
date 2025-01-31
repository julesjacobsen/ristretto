package org.monarchinitiative.ristretto.core.graph;

import java.util.Objects;

/**
 * @author Jules Jacobsen <j.jacobsen@qmul.ac.uk>
 */
public class Node implements Comparable<Node> {
    private final String id;
    private final String label;

    private volatile int hash;

    private Node(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public static Node of(String id, String label) {
        return new Node(id, label);
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Node)) return false;
        Node node = (Node) o;
        return Objects.equals(id, node.id) &&
                Objects.equals(label, node.label);
    }

    @Override
    public int hashCode() {
        // caching this approximately doubles/triples the throughput
        if (hash == 0) {
            hash = Objects.hash(id, label);
        }
        return hash;
    }

    @Override
    public String toString() {
        return "Node{" +
                "id='" + id + '\'' +
                ", label='" + label + '\'' +
                '}';
    }

    @Override
    public int compareTo(Node o) {
        return id.compareTo(o.id);
    }
}
