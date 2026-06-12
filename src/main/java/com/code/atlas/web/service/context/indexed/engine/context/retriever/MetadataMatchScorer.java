package com.code.atlas.web.service.context.indexed.engine.context.retriever;

import com.code.atlas.web.service.context.indexed.dto.FileSummaryOfflineResponse;
import com.code.atlas.web.service.context.indexed.dto.Intent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class MetadataMatchScorer {

    static final int SYMBOL_MATCH = 100;
    static final int DATA_STRUCTURE_MATCH = 90;
    static final int CONCEPT_MATCH = 80;
    static final int CAPABILITY_MATCH = 70;
    static final int ARCHITECTURAL_ROLE_MATCH = 60;
    static final int CHANGE_IMPACT_AREA_MATCH = 50;
    static final int DEPENDENCY_MATCH = 40;
    static final int BUSINESS_DOMAIN_MATCH = 30;
    static final int KEYWORD_MATCH = 20;
    static final int SUMMARY_MATCH = 20;
    static final int SEARCH_TEXT_MATCH = 10;

    private MetadataMatchScorer() {
    }

    public static ScoreResult score(Intent intent, FileSummaryOfflineResponse.Metadata metadata) {
        if (metadata == null) {
            return new ScoreResult(0, List.of(), List.of());
        }
        List<String> reasons = new ArrayList<>();
        Set<String> creditedReasons = new LinkedHashSet<>();
        List<String> matchedSymbols = new ArrayList<>();
        int totalScore = 0;

        totalScore += scoreSymbols(intent.symbols(), safeList(metadata.symbols()), matchedSymbols, reasons, creditedReasons);
        totalScore += scoreSearchTermsAgainstList(
                searchTerms(intent),
                safeList(metadata.dataStructures()),
                DATA_STRUCTURE_MATCH,
                "dataStructure",
                reasons,
                creditedReasons
        );
        totalScore += scoreTermsAgainstList(
                intent.concepts(),
                safeList(metadata.concepts()),
                CONCEPT_MATCH,
                "concept",
                reasons,
                creditedReasons
        );
        totalScore += scoreTermsAgainstList(
                intent.capabilities(),
                safeList(metadata.capabilities()),
                CAPABILITY_MATCH,
                "capability",
                reasons,
                creditedReasons
        );
        totalScore += scoreArchitecturalRoles(intent.architecturalRoles(), metadata.architecturalRole(), reasons, creditedReasons);
        totalScore += scoreTermsAgainstList(
                intent.changeImpactAreas(),
                safeList(metadata.changeImpactAreas()),
                CHANGE_IMPACT_AREA_MATCH,
                "changeImpactArea",
                reasons,
                creditedReasons
        );
        totalScore += scoreSearchTermsAgainstList(
                searchTerms(intent),
                safeList(metadata.dependencies()),
                DEPENDENCY_MATCH,
                "dependency",
                reasons,
                creditedReasons
        );
        totalScore += scoreSearchTermsAgainstList(
                searchTerms(intent),
                safeList(metadata.businessDomains()),
                BUSINESS_DOMAIN_MATCH,
                "businessDomain",
                reasons,
                creditedReasons
        );
        totalScore += scoreSearchTermsAgainstList(
                searchTerms(intent),
                safeList(metadata.keywords()),
                KEYWORD_MATCH,
                "keyword",
                reasons,
                creditedReasons
        );
        totalScore += scoreSearchTermsAgainstText(
                searchTerms(intent),
                metadata.summary(),
                SUMMARY_MATCH,
                "summary",
                reasons,
                creditedReasons
        );
        totalScore += scoreSearchTermsAgainstText(
                searchTerms(intent),
                metadata.searchText(),
                SEARCH_TEXT_MATCH,
                "searchText",
                reasons,
                creditedReasons
        );

        return new ScoreResult(totalScore, List.copyOf(reasons), List.copyOf(matchedSymbols));
    }

    private static List<String> searchTerms(Intent intent) {
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        terms.addAll(intent.symbols());
        terms.addAll(intent.concepts());
        terms.addAll(intent.capabilities());
        return List.copyOf(terms);
    }

    private static int scoreSymbols(
            List<String> intentSymbols,
            List<String> metadataSymbols,
            List<String> matchedSymbols,
            List<String> reasons,
            Set<String> creditedReasons
    ) {
        int score = 0;
        for (String intentSymbol : intentSymbols) {
            for (String metadataSymbol : metadataSymbols) {
                if (TermNormalizer.equalsNormalized(intentSymbol, metadataSymbol)) {
                    score += SYMBOL_MATCH;
                    addReason("symbol", intentSymbol, reasons, creditedReasons);
                    matchedSymbols.add(metadataSymbol.trim());
                    break;
                }
            }
        }
        return score;
    }

    private static int scoreTermsAgainstList(
            List<String> intentTerms,
            List<String> metadataValues,
            int weight,
            String reasonPrefix,
            List<String> reasons,
            Set<String> creditedReasons
    ) {
        int score = 0;
        for (String intentTerm : intentTerms) {
            for (String metadataValue : metadataValues) {
                if (TermNormalizer.equalsNormalized(intentTerm, metadataValue)) {
                    score += weight;
                    addReason(reasonPrefix, intentTerm, reasons, creditedReasons);
                    break;
                }
            }
        }
        return score;
    }

    private static int scoreSearchTermsAgainstList(
            List<String> searchTerms,
            List<String> metadataValues,
            int weight,
            String reasonPrefix,
            List<String> reasons,
            Set<String> creditedReasons
    ) {
        return scoreTermsAgainstList(searchTerms, metadataValues, weight, reasonPrefix, reasons, creditedReasons);
    }

    private static int scoreArchitecturalRoles(
            List<String> intentRoles,
            String metadataRole,
            List<String> reasons,
            Set<String> creditedReasons
    ) {
        if (metadataRole == null || metadataRole.isBlank()) {
            return 0;
        }
        int score = 0;
        for (String intentRole : intentRoles) {
            if (TermNormalizer.equalsNormalized(intentRole, metadataRole)) {
                score += ARCHITECTURAL_ROLE_MATCH;
                addReason("architecturalRole", metadataRole.trim(), reasons, creditedReasons);
                break;
            }
        }
        return score;
    }

    private static int scoreSearchTermsAgainstText(
            List<String> searchTerms,
            String text,
            int weight,
            String reasonPrefix,
            List<String> reasons,
            Set<String> creditedReasons
    ) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        int score = 0;
        for (String searchTerm : searchTerms) {
            if (TermNormalizer.containsNormalized(text, searchTerm)) {
                score += weight;
                addReason(reasonPrefix, searchTerm, reasons, creditedReasons);
            }
        }
        return score;
    }

    private static void addReason(String prefix, String value, List<String> reasons, Set<String> creditedReasons) {
        String trimmedValue = value == null ? "" : value.trim();
        if (trimmedValue.isEmpty()) {
            return;
        }
        String reasonKey = prefix + ":" + TermNormalizer.normalize(trimmedValue);
        if (creditedReasons.add(reasonKey)) {
            reasons.add(prefix + ": " + trimmedValue);
        }
    }

    private static List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }

    public record ScoreResult(int score, List<String> reasons, List<String> matchedSymbols) {
    }

}
