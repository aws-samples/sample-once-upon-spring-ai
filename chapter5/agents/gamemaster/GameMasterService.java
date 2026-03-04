package com.amazonaws;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import io.a2a.A2A;
import io.a2a.client.Client;
import io.a2a.client.ClientEvent;
import io.a2a.client.TaskEvent;
import io.a2a.client.config.ClientConfig;
import io.a2a.client.transport.jsonrpc.JSONRPCTransport;
import io.a2a.client.transport.jsonrpc.JSONRPCTransportConfig;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Artifact;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.Task;
import io.a2a.spec.TextPart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/// Manages connections to remote A2A agents (Rules Agent, Character Agent).
/// Resolves agent cards at startup and provides a sendMessage tool for the ChatClient.
@Service
class GameMasterService {

    private static final Logger log = LoggerFactory.getLogger(GameMasterService.class);

    private final Map<String, AgentCard> cards = new HashMap<>();

    GameMasterService(@Value("${remote.agents.urls}") List<String> agentUrls) {
        for (String url : agentUrls) {
            try {
                log.info("Resolving agent card from: {}", url);

                String path = new URI(url).getPath();
                AgentCard card = A2A.getAgentCard(url, path + ".well-known/agent-card.json", null);

                this.cards.put(card.name(), card);
                log.info("Discovered agent: {} at {}", card.name(), url);
            } catch (Exception e) {
                log.error("Failed to connect to agent at {}: {}", url, e.getMessage());
            }
        }
    }

    @Tool(description = """
        Sends a task to a remote agent. Use this to delegate work to specialized agents
        such as the Rules Agent (D&D mechanics) or the Character Agent (character management).
        """)
    String sendMessage(
            @ToolParam(description = "The name of the agent to send the task to") String agentName,
            @ToolParam(description = "The comprehensive task description and context to send to the agent") String task) {

        log.info("Sending message to agent '{}': {}", agentName, task);

        AgentCard agentCard = this.cards.get(agentName);
        if (agentCard == null) {
            String availableAgents = String.join(", ", this.cards.keySet());
            return "Agent '%s' not found. Available agents: %s".formatted(agentName, availableAgents);
        }

        try {
            var message = new Message.Builder()
                    .role(Message.Role.USER)
                    .parts(List.of(new TextPart(task, null)))
                    .build();

            CompletableFuture<String> responseFuture = new CompletableFuture<>();
            AtomicReference<String> responseText = new AtomicReference<>("");

            BiConsumer<ClientEvent, AgentCard> consumer = (event, card) -> {
                if (event instanceof TaskEvent taskEvent) {
                    Task completedTask = taskEvent.getTask();
                    log.info("Received task response: status={}", completedTask.getStatus().state());

                    if (completedTask.getArtifacts() != null) {
                        var sb = new StringBuilder();
                        for (Artifact artifact : completedTask.getArtifacts()) {
                            if (artifact.parts() != null) {
                                for (Part<?> part : artifact.parts()) {
                                    if (part instanceof TextPart textPart) {
                                        sb.append(textPart.getText());
                                    }
                                }
                            }
                        }
                        responseText.set(sb.toString());
                    }
                    responseFuture.complete(responseText.get());
                }
            };

            var clientConfig = new ClientConfig.Builder()
                    .setAcceptedOutputModes(List.of("text"))
                    .build();

            var client = Client.builder(agentCard)
                    .clientConfig(clientConfig)
                    .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
                    .addConsumers(List.of(consumer))
                    .build();

            client.sendMessage(message);

            String result = responseFuture.get(60, TimeUnit.SECONDS);
            log.info("Agent '{}' response: {}", agentName, result);
            return "[Raw data from %s — rewrite in your own Game Master voice]: %s".formatted(agentName, result);
        } catch (Exception e) {
            log.error("Error sending message to agent '{}': {}", agentName, e.getMessage());
            return "Error communicating with agent '%s': %s".formatted(agentName, e.getMessage());
        }
    }

    String getAgentDescriptions() {
        return this.cards.values().stream()
                .map(card -> """
                    {"name": "%s", "description": "%s"}""".formatted(
                        card.name(),
                        card.description() != null ? card.description() : "No description"))
                .collect(Collectors.joining("\n"));
    }

    List<String> getAgentNames() {
        return List.copyOf(this.cards.keySet());
    }
}
