package com.amazonaws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

/// Rules lookup tool — queries the vector store for D&D 5e rules.
@Service
class RulesTools {

    private static final Logger log = LoggerFactory.getLogger(RulesTools.class);

    private final VectorStore vectorStore;

    RulesTools(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Tool(description = "Fast D&D 5e rule lookup. Returns brief rule with page reference.")
    String queryDndRules(@ToolParam(description = "The rules question or topic to look up") String query) {
        log.info("Tool called: queryDndRules('{}')", query);

        var results = vectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(3).build());

        if (results.isEmpty()) return "No rules found for that query.";

        var sb = new StringBuilder();
        for (var doc : results) {
            var page = doc.getMetadata().getOrDefault("page", "?");
            sb.append("[Page %s] %s\n\n".formatted(page, doc.getText()));
        }
        return sb.toString().strip();
    }
}
