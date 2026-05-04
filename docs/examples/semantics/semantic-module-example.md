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
