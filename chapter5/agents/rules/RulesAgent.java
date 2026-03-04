///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 25+
//SOURCES RulesTools.java
//REPOS mavencentral,spring-milestones=https://repo.spring.io/milestone
//DEPS org.springframework.boot:spring-boot-starter-web:4.0.2
//DEPS org.springframework.ai:spring-ai-bedrock-converse:2.0.0-M2
//DEPS org.springframework.ai:spring-ai-client-chat:2.0.0-M2
//DEPS org.springframework.ai:spring-ai-vector-store:2.0.0-M2
//DEPS org.springframework.ai:spring-ai-bedrock:2.0.0-M2
//DEPS org.springaicommunity:spring-ai-a2a-server-autoconfigure:0.2.0
//DEPS software.amazon.awssdk:bedrockruntime:2.41.34
//DEPS software.amazon.awssdk:auth:2.41.34
//DEPS org.slf4j:slf4j-api:2.0.17
//DEPS org.springframework:spring-core:7.0.3

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/// Rules Agent — D&D 5e rules lookup via A2A protocol with vector knowledge base.
/// Prerequisites: Run utils/CreateKnowledgeBase.java first to generate the knowledge base.
/// Run with: jbang RulesAgent.java
@SpringBootApplication
public class RulesAgent {

    private static final String SYSTEM_PROMPT = """
        You are a D&D rules expert. When asked about rules, use the queryDndRules tool once
        to find the relevant rule, then provide a clear, concise answer with the page reference.
        Keep responses brief and focused on the specific rule requested.
        """;

    public static void main(final String[] args) {
        // TODO 1: Configure Spring Boot properties for this A2A agent server.
        //   Use System.setProperty() to set four properties before SpringApplication.run():
        //     - Server port (this agent runs on port 8000)
        //     - Servlet context path (A2A convention uses "/a2a")
        //     - Application name
        //     - Enable the A2A server auto-configuration

        SpringApplication.run(RulesAgent.class, args);
    }

    // TODO 2: Create an AgentCard bean that describes this agent to the A2A network.
    //   The AgentCard is published at /.well-known/agent-card.json so other agents can discover you.
    //   Use AgentCard.Builder to set: name, description, url, version, capabilities, I/O modes, skills, and protocol version.
    //   Inject the port and context path via @Value to construct the URL dynamically.
    //   Define at least one AgentSkill for rules lookup with an id, name, description, tags, and examples.

    // TODO 3: Create an AgentExecutor bean that wires the ChatClient with the RulesTools.
    //   Wire a ChatClient with the SYSTEM_PROMPT and RulesTools, then wrap it in a DefaultAgentExecutor.
    //   The executor's lambda should extract the user message text and invoke the ChatClient.
}

/// Vector store configuration — loads the knowledge base from utils/
@Configuration
class VectorStoreConfig {

    private static final Logger log = LoggerFactory.getLogger("VectorStoreConfig");
    private static final String VECTOR_STORE_FILE = "./../../utils/dm_knowledge_base.json";

    @Bean
    BedrockTitanEmbeddingModel embeddingModel() {
        var titanApi = new TitanEmbeddingBedrockApi(
                "amazon.titan-embed-text-v2:0",
                "us-west-2",
                Duration.ofSeconds(60));
        return new BedrockTitanEmbeddingModel(titanApi, ObservationRegistry.NOOP);
    }

    @Bean
    VectorStore vectorStore(BedrockTitanEmbeddingModel embeddingModel) {
        var store = SimpleVectorStore.builder(embeddingModel).build();

        var file = new File(VECTOR_STORE_FILE);
        if (file.exists()) {
            store.load(file);
            log.info("Loaded knowledge base from: {} ({} bytes)", VECTOR_STORE_FILE, file.length());
        } else {
            log.warn("Knowledge base not found: {}. Run CreateKnowledgeBase.java first!", VECTOR_STORE_FILE);
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
                .credentialsProvider(DefaultCredentialsProvider.builder().build())
                .build();

        var options = BedrockChatOptions.builder()
                .model("global.anthropic.claude-haiku-4-5-20251001-v1:0")
                .build();

        return BedrockProxyChatModel.builder()
                .bedrockRuntimeClient(bedrockClient)
                .defaultOptions(options)
                .build();
    }
}
