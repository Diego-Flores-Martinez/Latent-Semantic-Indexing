package com.lsi.semantic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Resolves controlled polysemy using simple context rules.
 *
 * Example:
 * academic + pressure -> academic_pressure
 * emotional + support -> emotional_support
 * university + support -> institutional_support
 */
public class PolysemyResolver {

    private static final String DEFAULT_RESOURCE = "/polysemy_rules.csv";
    private static final int CONTEXT_WINDOW = 2;

    private final List<PolysemyRule> rules;

    public PolysemyResolver() {
        this(loadDefaultRules());
    }

    public PolysemyResolver(List<PolysemyRule> rules) {
        Objects.requireNonNull(rules, "rules cannot be null");

        this.rules = new ArrayList<>(rules);
        this.rules.sort(PolysemyResolver::compareRules);
    }

    public List<String> resolve(List<String> tokens) {
        Objects.requireNonNull(tokens, "tokens cannot be null");

        List<String> normalizedTokens = normalizeTokens(tokens);
        List<String> result = new ArrayList<>();

        int i = 0;

        while (i < normalizedTokens.size()) {
            String current = normalizedTokens.get(i);

            if (i + 1 < normalizedTokens.size()) {
                String next = normalizedTokens.get(i + 1);

                PolysemyRule contextBefore = bestRuleForContextAndAmbiguous(current, next);

                if (contextBefore != null) {
                    result.add(contextBefore.assignedSense());
                    i += 2;
                    continue;
                }

                PolysemyRule contextAfter = bestRuleForContextAndAmbiguous(next, current);

                if (contextAfter != null) {
                    result.add(contextAfter.assignedSense());
                    i += 2;
                    continue;
                }
            }

            PolysemyRule localContextRule = bestRuleNearToken(current, normalizedTokens, i);

            if (localContextRule != null) {
                result.add(localContextRule.assignedSense());
            } else {
                result.add(current);
            }

            i++;
        }

        return result;
    }

    public List<PolysemyRule> rules() {
        return List.copyOf(rules);
    }

    private PolysemyRule bestRuleForContextAndAmbiguous(String contextToken, String ambiguousTerm) {
        return rules.stream()
                .filter(rule -> rule.ambiguousTerm().equals(ambiguousTerm))
                .filter(rule -> rule.contextToken().equals(contextToken))
                .min(PolysemyResolver::compareRules)
                .orElse(null);
    }

    private PolysemyRule bestRuleNearToken(String token, List<String> tokens, int index) {
        List<PolysemyRule> candidates = new ArrayList<>();

        for (PolysemyRule rule : rules) {
            if (!rule.ambiguousTerm().equals(token)) {
                continue;
            }

            if (contextAppearsNear(tokens, index, rule.contextToken())) {
                candidates.add(rule);
            }
        }

        return candidates.stream()
                .min(PolysemyResolver::compareRules)
                .orElse(null);
    }

    private boolean contextAppearsNear(List<String> tokens, int index, String contextToken) {
        int start = Math.max(0, index - CONTEXT_WINDOW);
        int end = Math.min(tokens.size() - 1, index + CONTEXT_WINDOW);

        for (int i = start; i <= end; i++) {
            if (i == index) {
                continue;
            }

            if (tokens.get(i).equals(contextToken)) {
                return true;
            }
        }

        return false;
    }

    private static List<String> normalizeTokens(List<String> tokens) {
        List<String> normalized = new ArrayList<>();

        for (String token : tokens) {
            String value = normalize(token);

            if (!value.isBlank()) {
                normalized.add(value);
            }
        }

        return normalized;
    }

    private static List<PolysemyRule> loadDefaultRules() {
        List<PolysemyRule> loadedRules = new ArrayList<>();

        try (InputStream input = PolysemyResolver.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (input == null) {
                return loadedRules;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(input, StandardCharsets.UTF_8))) {

                String line;

                while ((line = reader.readLine()) != null) {
                    line = line.trim();

                    if (line.isBlank() || line.startsWith("#")) {
                        continue;
                    }

                    if (line.equalsIgnoreCase("ambiguous_term,context_token,assigned_sense,priority")) {
                        continue;
                    }

                    String[] parts = line.split(",", -1);

                    if (parts.length < 4) {
                        continue;
                    }

                    String ambiguousTerm = normalize(parts[0]);
                    String contextToken = normalize(parts[1]);
                    String assignedSense = normalize(parts[2]);
                    int priority = parsePriority(parts[3]);

                    if (!ambiguousTerm.isBlank()
                            && !contextToken.isBlank()
                            && !assignedSense.isBlank()) {
                        loadedRules.add(new PolysemyRule(
                                ambiguousTerm,
                                contextToken,
                                assignedSense,
                                priority
                        ));
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not load polysemy_rules.csv", e);
        }

        return loadedRules;
    }

    private static int parsePriority(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static int compareRules(PolysemyRule a, PolysemyRule b) {
        int byPriority = Integer.compare(b.priority(), a.priority());

        if (byPriority != 0) {
            return byPriority;
        }

        int bySense = a.assignedSense().compareTo(b.assignedSense());

        if (bySense != 0) {
            return bySense;
        }

        return a.contextToken().compareTo(b.contextToken());
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "_");
    }

    public record PolysemyRule(
            String ambiguousTerm,
            String contextToken,
            String assignedSense,
            int priority
    ) {
        public Map<String, String> asMap() {
            Map<String, String> map = new LinkedHashMap<>();
            map.put("ambiguousTerm", ambiguousTerm);
            map.put("contextToken", contextToken);
            map.put("assignedSense", assignedSense);
            map.put("priority", String.valueOf(priority));
            return map;
        }
    }
}
