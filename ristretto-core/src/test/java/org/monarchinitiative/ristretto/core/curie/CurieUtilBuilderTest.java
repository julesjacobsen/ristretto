package org.monarchinitiative.ristretto.core.curie;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Jules Jacobsen <j.jacobsen@qmul.ac.uk>
 */
class CurieUtilBuilderTest {

    @Test
    void defaultCurieMap() {
        assertFalse(CurieUtilBuilder.defaultCurieMap().isEmpty());
    }

    @Test
    void defaultCurieUtil() {
        CurieUtil instance = CurieUtilBuilder.defaultCurieUtil();
        assertEquals(Optional.of("HP:0000001"), instance.getCurie("http://purl.obolibrary.org/obo/HP_0000001"));
    }

    @Test
    void defaultCurieUtilWithDefaultsAndUserDefinedResource() {
        CurieUtil defaultCurieUtil = CurieUtilBuilder.defaultCurieUtil();
        assertFalse(defaultCurieUtil.hasPrefix("WIBBLE"));

        CurieUtil instance = CurieUtilBuilder.withDefaultsAnd(Map.of("WIBBLE", "http://purl.obolibrary.org/obo/WIBBLE_"));
        assertTrue(instance.hasPrefix("WIBBLE"));
        assertEquals(Optional.of("WIBBLE:0000001"), instance.getCurie("http://purl.obolibrary.org/obo/WIBBLE_0000001"));
        assertEquals(Optional.of("http://purl.obolibrary.org/obo/WIBBLE_"), instance.getExpansion("WIBBLE"));
        assertEquals(Optional.of("http://purl.obolibrary.org/obo/WIBBLE_0000001"), instance.getIri("WIBBLE:0000001"), "IRI failed");
    }

    @Test
    void curieUtilWithUserSpecifiedMappingsOnly() {
        Map<String, String> userDefinedCurieMap = Map.of(
                "WIBBLE", "http://purl.obolibrary.org/obo/WIBBLE_",
                "FLUFFY", "http://purl.obolibrary.org/obo/FLUFFY_"
        );

        CurieUtil instance = CurieUtilBuilder.just(userDefinedCurieMap);

        assertEquals(Optional.of("WIBBLE:0000001"), instance.getCurie("http://purl.obolibrary.org/obo/WIBBLE_0000001"));
        assertEquals(Optional.of("FLUFFY:0000001"), instance.getCurie("http://purl.obolibrary.org/obo/FLUFFY_0000001"));
    }

    @Test
    void incorrectIri() {
        CurieUtil instance = CurieUtilBuilder.withDefaultsAnd(Map.of("WIBBLE", "http://purl.obolibrary.org/obo/WIBBLE_"));

        Optional<String> curie = instance.getCurie("http://purl.obolibrary.org/ob/WIBBLE_0000001"); // note missing `o` in `/obo/`

        assertTrue(curie.isEmpty());
    }
}
