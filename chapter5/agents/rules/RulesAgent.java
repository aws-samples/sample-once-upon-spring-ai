///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 25+
//SOURCES RulesTools.java
//FILES dm_knowledge_base.json=./../../utils/dm_knowledge_base.json
//REPOS mavencentral,spring-milestones=https://repo.spring.io/milestone
//DEPS io.netty:netty-bom:4.2.9.Final@pom
//DEPS org.springframework.boot:spring-boot-starter-web:4.0.2
//DEPS com.fasterxml.jackson.core:jackson-annotations:2.21
//DEPS org.springframework.ai:spring-ai-bedrock-converse:2.0.0
//DEPS org.springframework.ai:spring-ai-client-chat:2.0.0
//DEPS org.springframework.ai:spring-ai-vector-store:2.0.0
//DEPS org.springframework.ai:spring-ai-bedrock:2.0.0
//DEPS org.springaicommunity:spring-ai-a2a-server-autoconfigure:0.3.0
//DEPS software.amazon.awssdk:bedrockruntime:2.41.34
//DEPS software.amazon.awssdk:auth:2.41.34
//DEPS org.slf4j:slf4j-api:2.0.17
//DEPS org.springframework:spring-core:7.0.3
//RUNTIME_OPTIONS -Daws.region=us-west-2 --enable-native-access=ALL-UNNAMED

package com.amazonaws;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentSkill;
import io.micrometer.observation.ObservationRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.a2a.server.executor.DefaultAgentExecutor;
import org.springframework.ai.bedrock.converse.BedrockChatOptions;
import org.springframework.ai.bedrock.converse.BedrockProxyChatModel;
import org.springframework.ai.bedrock.titan.BedrockTitanEmbeddingModel;
import org.springframework.ai.bedrock.titan.api.TitanEmbeddingBedrockApi;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;

import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/// Rules Agent — TTRPG 5e rules lookup via A2A protocol with vector knowledge base.
/// Prerequisites: Run utils/CreateKnowledgeBase.java first to generate the knowledge base.
/// Run with: jbang RulesAgent.java
@SpringBootApplication
public class RulesAgent {

    private static final String SYSTEM_PROMPT = """
        You are a TTRPG 5e rules expert. When asked about rules, ALWAYS use the queryDndRules tool
        to find the relevant rule from the official source — never answer from memory alone.
        Provide a clear, concise answer that includes:
        1. The rule mechanic (what dice to roll, what modifiers apply, DCs, etc.)
        2. The page reference from the source material
        3. Any relevant conditions, advantages, or disadvantages
        Keep responses focused, actionable, and ready for the Game Master to use in gameplay.
        """;

    public static void main(final String[] args) {
        // Step 1: Configure Spring Boot properties for this A2A agent server.
        System.setProperty("server.port", "8000");
        System.setProperty("server.servlet.context-path", "/a2a");
        System.setProperty("spring.application.name", "rules-agent");
        System.setProperty("spring.ai.a2a.server.enabled", "true");
        SpringApplication.run(RulesAgent.class, args);
    }

    // Step 2: Publish an AgentCard so the orchestrator can discover this agent over A2A.
    @Bean
    AgentCard agentCard(@Value("${server.port:8000}") int port,
                        @Value("${server.servlet.context-path:/a2a}") String contextPath) {
        return new AgentCard.Builder()
                .name("Rules Agent")
                .description("""
                    Specialized TTRPG 5e rules lookup agent that provides fast, authoritative rule clarifications
                    from the Basic Rules. Queries a vector knowledge base containing indexed TTRPG content and returns
                    brief, page-referenced rule explanations.""")
                .url("http://localhost:" + port + contextPath + "/")
                .version("1.0.0")
                .capabilities(new AgentCapabilities.Builder().streaming(false).build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of(
                    new AgentSkill.Builder()
                        .id("rules_lookup")
                        .name("TTRPG Rules Lookup")
                        .description("""
                            Look up any TTRPG 5e rule from the official Basic Rules knowledge base. Covers combat, \
                            spellcasting, ability checks, saving throws, conditions, movement, equipment, and more. \
                            Returns the exact rule with page references.""")
                        .tags(List.of("rules", "dnd", "mechanics", "combat", "spells", "conditions"))
                        .examples(List.of(
                            "How do attack rolls work?",
                            "What is armor class and how is it calculated?",
                            "Rules for perception checks",
                            "How does spellcasting work for sorcerers?",
                            "What happens when a character is grappled?",
                            "Rules for saving throws"))
                        .build()))
                .protocolVersion("0.3.0")
                .build();
    }

    // Step 3: Wire a ChatClient + RulesTools into a DefaultAgentExecutor — that's how A2A invokes us.
    @Bean
    AgentExecutor agentExecutor(BedrockProxyChatModel chatModel, RulesTools rulesTools) {
        var chatClient = ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(rulesTools)
                .build();

        return new DefaultAgentExecutor(chatClient, (chat, requestContext) -> {
            String userMessage = DefaultAgentExecutor.extractTextFromMessage(requestContext.getMessage());
            return chat.prompt().user(userMessage).call().content();
        });
    }
}

/// Vector store configuration — loads the knowledge base from classpath
@Configuration
class VectorStoreConfig {

    private static final Logger log = LoggerFactory.getLogger("VectorStoreConfig");
    private static final String VECTOR_STORE_RESOURCE = "dm_knowledge_base.json";

    @Bean
    BedrockTitanEmbeddingModel embeddingModel() {
        var titanApi = new TitanEmbeddingBedrockApi(
                "amazon.titan-embed-text-v2:0",
                "us-west-2",
                Duration.ofSeconds(60));
        return new BedrockTitanEmbeddingModel(titanApi, ObservationRegistry.NOOP);
    }

    @Bean
    VectorStore vectorStore(BedrockTitanEmbeddingModel embeddingModel) throws IOException {
        var store = SimpleVectorStore.builder(embeddingModel).build();

        var resource = new ClassPathResource(VECTOR_STORE_RESOURCE);
        if (resource.exists()) {
            store.load(resource);
            log.info("Loaded knowledge base from classpath: {} ({} bytes)",
                    VECTOR_STORE_RESOURCE, resource.contentLength());
        } else {
            log.warn("Knowledge base not found on classpath: {}. Run CreateKnowledgeBase.java first!",
                    VECTOR_STORE_RESOURCE);
        }

        return store;
    }
}

/// Bedrock ChatModel configuration
@Configuration
class RulesAgentConfig {

    @Bean
    BedrockProxyChatModel chatModel() {
        var bedrockClient = BedrockRuntimeClient.builder()
                .region(Region.US_WEST_2)
                .credentialsProvider(AnonymousCredentialsProvider.create())
                .overrideConfiguration(c -> c.putHeader("Authorization",
                        "Bearer " + System.getenv("AWS_BEARER_TOKEN_BEDROCK")))
                .build();

        var options = BedrockChatOptions.builder()
                .model("us.anthropic.claude-haiku-4-5-20251001-v1:0")
                .build();

        return BedrockProxyChatModel.builder()
                .bedrockRuntimeClient(bedrockClient)
                .options(options)
                .build();
    }
}
