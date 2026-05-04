# Semantic Module Example

This module receives preprocessed tokens and applies controlled semantic normalization.

## Input

student worry academic pressure emotional support university support

## Synonym normalization

worry -> anxiety

## Polysemy resolution

academic + pressure -> academic_pressure  
emotional + support -> emotional_support  
university + support -> institutional_support

## Output

student anxiety academic_pressure emotional_support institutional_support

## Output contract for indexing

The semantic module returns a `List<String>` containing canonical semantic terms.

The indexing module can consume this list directly to build the global vocabulary and the FrecT matrix.

Example Java output:

```java
List<String> semanticTerms = List.of(
    "student",
    "anxiety",
    "academic_pressure",
    "emotional_support",
    "institutional_support"
);

