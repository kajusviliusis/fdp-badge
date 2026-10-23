-- Full-text search over title and author. The indexed expression must match the one used by the
-- fts_match / fts_rank functions registered in FullTextSearchFunctionContributor.
CREATE INDEX idx_books_fts ON books USING GIN (to_tsvector('english', title || ' ' || author));
