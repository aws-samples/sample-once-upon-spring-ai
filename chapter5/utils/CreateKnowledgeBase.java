///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 25+
//REPOS mavencentral,spring-milestones=https://repo.spring.io/milestone
//DEPS org.springframework.ai:spring-ai-pdf-document-reader:2.0.0-M4
//DEPS org.springframework.ai:spring-ai-vector-store:2.0.0-M4
//DEPS org.springframework.ai:spring-ai-bedrock:2.0.0-M4
//DEPS software.amazon.awssdk:bedrockruntime:2.41.34
//DEPS software.amazon.awssdk:auth:2.41.34
//DEPS org.slf4j:slf4j-api:2.0.17
//DEPS org.slf4j:slf4j-simple:2.0.17

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.bedrock.titan.BedrockTitanEmbeddingModel;
import org.springframework.ai.bedrock.titan.api.TitanEmbeddingBedrockApi;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.core.io.FileSystemResource;

import io.micrometer.observation.ObservationRegistry;

import java.io.File;
import java.time.Duration;
import java.util.List;

/// Creates a D&D rules knowledge base from the Basic Rules PDF.
/// Uses Spring AI's PagePdfDocumentReader and SimpleVectorStore with Bedrock Titan Embeddings.
///
/// Prerequisites:
///   1. Download "DnD_BasicRules_2018.pdf" and place it in this directory
///   2. AWS credentials configured with Bedrock access in us-west-2
///
/// Usage: jbang CreateKnowledgeBase.java
///
/// Output: dnd_knowledge_base.json (SimpleVectorStore file)

private static final Logger log = LoggerFactory.getLogger("CreateKnowledgeBase");
private static final String PDF_FILE = "DnD_BasicRules_2018.pdf";
private static final String VECTOR_STORE_FILE = "dm_knowledge_base.json";

void main() {
    // Step 1: Validate PDF exists
    var pdfFile = new File(PDF_FILE);
    if (!pdfFile.exists()) {
        log.error("PDF file not found: {}", pdfFile.getAbsolutePath());
        log.error("Download the D&D Basic Rules PDF and place it in this directory.");
        return;
    }
    log.info("Found PDF: {} ({} bytes)", PDF_FILE, pdfFile.length());

    // Step 2: Create Bedrock Titan Embedding Model
    log.info("Initializing Bedrock Titan Embedding Model...");
    var titanApi = new TitanEmbeddingBedrockApi(
            "amazon.titan-embed-text-v2:0",
            "us-west-2",
            Duration.ofSeconds(60));
    var embeddingModel = new BedrockTitanEmbeddingModel(titanApi, ObservationRegistry.NOOP);

    // Step 3: Read PDF — one Document per page, with page metadata
    log.info("Reading PDF...");
    var pdfReader = new PagePdfDocumentReader(new FileSystemResource(pdfFile));
    List<Document> pages = pdfReader.get();
    log.info("Extracted {} pages from PDF", pages.size());

    // Step 4: Split into smaller chunks for better retrieval
    log.info("Splitting into chunks...");
    var splitter = new TokenTextSplitter();
    List<Document> chunks = splitter.apply(pages);

    // Filter out very short chunks (< 50 chars)
    chunks = chunks.stream()
            .filter(doc -> doc.getText().length() >= 50)
            .toList();
    log.info("Created {} chunks after filtering", chunks.size());

    // Step 5: Create SimpleVectorStore and add documents (embeddings computed automatically)
    log.info("Creating vector store and computing embeddings (this may take a while)...");
    var vectorStore = SimpleVectorStore.builder(embeddingModel).build();

    // Add in batches of 100 for efficient processing
    int batchSize = 100;
    for (int i = 0; i < chunks.size(); i += batchSize) {
        int end = Math.min(i + batchSize, chunks.size());
        var batch = chunks.subList(i, end);
        vectorStore.add(batch);
        log.info("  Added batch {}/{} ({} documents)",
                (i / batchSize) + 1,
                (int) Math.ceil((double) chunks.size() / batchSize),
                batch.size());
    }

    // Step 6: Save to file
    vectorStore.save(new File(VECTOR_STORE_FILE));
    log.info("Knowledge base saved to: {}", VECTOR_STORE_FILE);
    log.info("Done! The RulesAgent will load this file at startup.");
}
