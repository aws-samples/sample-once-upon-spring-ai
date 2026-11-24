package com.amazonaws;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.a2aproject.sdk.A2A;
import org.a2aproject.sdk.client.Client;
import org.a2aproject.sdk.client.TaskEvent;
import org.a2aproject.sdk.client.config.ClientConfig;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransport;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransportConfig;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.TextPart;

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
        for (var url : agentUrls) {
            try {
                log.info("Resolving agent card from: {}", url);
                var card = A2A.getAgentCard(url);
                cards.put(card.name(), card);
                log.info("Discovered agent: {} at {}", card.name(), url);
            } catch (Exception e) {
                log.error("Failed to connect to agent at {}: {}", url, e.getMessage());
            }
        }
    }

    @Tool(description = """
        Sends a task to a remote agent. Use this to delegate work to specialized agents
        such as the Rules Agent (TTRPG mechanics) or the Character Agent (character management).
        """)
    String sendMessage(
            @ToolParam(description = "The name of the agent to send the task to") String agentName,
            @ToolParam(description = "The comprehensive task description and context to send to the agent") String task) {

        log.info("Sending message to agent '{}': {}", agentName, task);

        var agentCard = cards.get(agentName);
        if (agentCard == null) {
            return "Agent '%s' not found. Available agents: %s"
                    .formatted(agentName, String.join(", ", cards.keySet()));
        }

        try {
            var message = Message.builder()
                    .role(Message.Role.ROLE_USER)
                    .parts(List.of(new TextPart(task)))
                    .build();

            var responseFuture = new CompletableFuture<String>();

            var client = Client.builder(agentCard)
                    .clientConfig(new ClientConfig.Builder().setAcceptedOutputModes(List.of("text")).build())
                    .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
                    .addConsumers(List.of((event, _) -> {
                        if (event instanceof TaskEvent taskEvent) {
                            var completedTask = taskEvent.getTask();
                            log.info("Received task response: status={}", completedTask.status().state());
                            var sb = new StringBuilder();
                            if (completedTask.artifacts() != null) {
                                for (var artifact : completedTask.artifacts()) {
                                    if (artifact.parts() == null) continue;
                                    for (var part : artifact.parts()) {
                                        if (part instanceof TextPart textPart) sb.append(textPart.text());
                                    }
                                }
                            }
                            responseFuture.complete(sb.toString());
                        }
                    }))
                    .build();

            client.sendMessage(message);

            var result = responseFuture.get(60, TimeUnit.SECONDS);
            log.info("Agent '{}' response: {}", agentName, result);
            return "[Raw data from %s — rewrite in your own Game Master voice]: %s".formatted(agentName, result);
        } catch (Exception e) {
            log.error("Error sending message to agent '{}': {}", agentName, e.getMessage());
            return "Error communicating with agent '%s': %s".formatted(agentName, e.getMessage());
        }
    }

    /// Builds rich agent descriptions from A2A agent cards, including skills
    /// discovered via A2A.getAgentCard(). This gives the LLM full visibility into
    /// what each remote agent can do.
    String getAgentDescriptions() {
        return cards.values().stream()
                .map(card -> {
                    var sb = new StringBuilder();
                    sb.append("Agent: %s\n".formatted(card.name()));
                    sb.append("  Description: %s\n".formatted(
                            card.description() != null ? card.description() : "No description"));
                    if (card.skills() != null && !card.skills().isEmpty()) {
                        sb.append("  Skills:\n");
                        for (var skill : card.skills()) {
                            sb.append("    - %s: %s\n".formatted(skill.name(), skill.description()));
                            if (skill.examples() != null && !skill.examples().isEmpty()) {
                                sb.append("      Examples: %s\n".formatted(
                                        String.join(", ", skill.examples())));
                            }
                        }
                    }
                    return sb.toString();
                })
                .collect(Collectors.joining("\n"));
    }

    List<String> getAgentNames() {
        return List.copyOf(cards.keySet());
    }
}
