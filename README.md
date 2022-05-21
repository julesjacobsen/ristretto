Compressed Sparse Row (CSR) graph implementation for Ontologies

Ontology being a Directed Acyclic Graph (DAG) where the edges have properties.

Consider extending [Guava's graph](https://github.com/google/guava/wiki/GraphsExplained) [AbstractValueGraph](https://guava.dev/releases/snapshot/api/docs/com/google/common/graph/AbstractValueGraph.html) as this has a nice API.
They can also be [adapted to JGraphT](https://jgrapht.org/guide/UserOverview#guava-graph-adapter).