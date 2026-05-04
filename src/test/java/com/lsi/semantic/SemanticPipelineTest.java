package com.lsi.semantic;

import junit.framework.TestCase;

import java.util.List;

public class SemanticPipelineTest extends TestCase {

    public void testSynonymCanonicalization() {
        SynonymExpander expander = new SynonymExpander();

        List<String> result = expander.expand(List.of(
                "worry",
                "nervousness",
                "tension",
                "workout"
        ));

        assertEquals(List.of(
                "anxiety",
                "anxiety",
                "stress",
                "physical_activity"
        ), result);
    }

    public void testPolysemyResolutionWithContextBeforeAmbiguousTerm() {
        PolysemyResolver resolver = new PolysemyResolver();

        List<String> result = resolver.resolve(List.of(
                "academic",
                "pressure",
                "emotional",
                "support",
                "university",
                "support"
        ));

        assertEquals(List.of(
                "academic_pressure",
                "emotional_support",
                "institutional_support"
        ), result);
    }

    public void testUnknownTermsArePreserved() {
        SemanticPipeline pipeline = new SemanticPipeline();

        List<String> result = pipeline.process(List.of(
                "student",
                "motivation",
                "focus"
        ));

        assertEquals(List.of(
                "student",
                "motivation",
                "focus"
        ), result);
    }

    public void testFullSemanticPipelineWithSynonymsAndPolysemy() {
        SemanticPipeline pipeline = new SemanticPipeline();

        List<String> result = pipeline.process(List.of(
                "student",
                "worry",
                "academic",
                "pressure",
                "emotional",
                "support",
                "university",
                "support"
        ));

        assertEquals(List.of(
                "student",
                "anxiety",
                "academic_pressure",
                "emotional_support",
                "institutional_support"
        ), result);
    }
}
