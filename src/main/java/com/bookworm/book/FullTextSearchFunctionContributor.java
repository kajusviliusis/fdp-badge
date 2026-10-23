package com.bookworm.book;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.StandardBasicTypes;

/**
 * Registers the PostgreSQL full-text search expressions used by {@link BookSpecifications}, so that they can be
 * called from the JPA Criteria API as {@code fts_match(title, author, query)} and
 * {@code fts_rank(title, author, query)}.
 *
 * <p>The {@code to_tsvector(...)} expression is identical to the one in the {@code idx_books_fts} GIN index
 * (see migration {@code V2}); changing one requires changing the other. The query text is parsed with
 * {@code websearch_to_tsquery}, which accepts arbitrary user input without raising syntax errors.
 *
 * <p>Registered through {@code META-INF/services}; only PostgreSQL can execute these functions.
 */
public class FullTextSearchFunctionContributor implements FunctionContributor {

    static final String MATCH = "fts_match";
    static final String RANK = "fts_rank";

    private static final String DOCUMENT = "to_tsvector('english', ?1 || ' ' || ?2)";
    private static final String QUERY = "websearch_to_tsquery('english', ?3)";

    @Override
    public void contributeFunctions(FunctionContributions contributions) {
        BasicTypeRegistry types = contributions.getTypeConfiguration().getBasicTypeRegistry();
        contributions.getFunctionRegistry()
                .patternDescriptorBuilder(MATCH, "(" + DOCUMENT + " @@ " + QUERY + ")")
                .setExactArgumentCount(3)
                .setInvariantType(types.resolve(StandardBasicTypes.BOOLEAN))
                .register();
        contributions.getFunctionRegistry()
                .patternDescriptorBuilder(RANK, "ts_rank(" + DOCUMENT + ", " + QUERY + ")")
                .setExactArgumentCount(3)
                .setInvariantType(types.resolve(StandardBasicTypes.DOUBLE))
                .register();
    }
}
