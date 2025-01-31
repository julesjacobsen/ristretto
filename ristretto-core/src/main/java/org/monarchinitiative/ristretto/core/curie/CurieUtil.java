package org.monarchinitiative.ristretto.core.curie;

import java.util.Map;
import java.util.Optional;

public interface CurieUtil {

  /**
   * Create {@link CurieUtil} backed by provided <code>prefixToIri</code> map.
   *
   * @param prefixToIri map of CURIE prefixes (e.g. <code>HP</code>) to IRI prefixes (e.g. <code>http://purl.obolibrary.org/obo/HP_</code>).
   * @return an instance backed by the provided {@link Map}.
   */
  static CurieUtil of(Map<String, String> prefixToIri) {
    return new CurieUtilDefault(prefixToIri);
  }

  Map<String, String> prefixToIri();

  boolean hasPrefix(String curiePrefix);

  /**
   * Get CURIE for given <code>iri</code>.
   *
   * @param iri international resource identifier (e.g. <code>http://purl.obolibrary.org/obo/HP_1234567</code>).
   * @return the corresponding CURIE (e.g. <code>HP:1234567</code>) or an empty optional if the IRI prefix is not recognized.
   */
  Optional<String> getCurie(String iri);

  /**
   * @param curiePrefix CURIE prefix, e.g. <code>HP</code> for <code>HP:1234567</code>.
   * @return the corresponding expansion (e.g. <code>http://purl.obolibrary.org/obo/HP_</code>).
   */
  Optional<String> getExpansion(String curiePrefix);

  /**
   * Expand CURIE to IRI.
   *
   * @param curie CURIE to expand (e.g. <code>HP:1234567</code>).
   * @return IRI value (e.g. <code>http://purl.obolibrary.org/obo/HP_1234567</code>).
   */
  default Optional<String> getIri(String curie) {
    int sepPos = curie.indexOf(':');
    if (sepPos == -1) {
      return Optional.empty();
    }
    String prefix = curie.substring(0, sepPos);
    String id = curie.substring(sepPos + 1);
    return getExpansion(prefix).map(iri -> iri + id);
  }

}
