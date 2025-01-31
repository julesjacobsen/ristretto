package org.monarchinitiative.ristretto.core.curie;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Default {@link CurieUtil} implementation backed by two immutable maps.
 */
class CurieUtilDefault implements CurieUtil {

  private final Trie trie;
  private final Map<String, String> prefixToIri;
  private final Map<String, String> iriToPrefix;

  CurieUtilDefault(Map<String, String> prefixToIri) {
    this.prefixToIri = Map.copyOf(Objects.requireNonNull(prefixToIri));
    this.iriToPrefix = prefixToIri.entrySet().stream()
      .collect(Collectors.toUnmodifiableMap(Map.Entry::getValue, Map.Entry::getKey));
    this.trie = new Trie(prefixToIri.values());
  }

  public Map<String, String> prefixToIri() {
    return prefixToIri;
  }

  public Map<String, String> iriToPrefix() {
    return iriToPrefix;
  }

  @Override
  public boolean hasPrefix(String curiePrefix) {
    return prefixToIri.containsKey(curiePrefix);
  }

  @Override
  public Optional<String> getCurie(String iri) {
    String prefix = trie.search(iri);
    if (prefix.isBlank())
      return Optional.empty();

    String curiePrefix = iriToPrefix.get(prefix);
    return Optional.of(curiePrefix + ":" + iri.substring(prefix.length()));
  }

  @Override
  public Optional<String> getExpansion(String curiePrefix) {
    return Optional.ofNullable(prefixToIri.get(curiePrefix));
  }

}
