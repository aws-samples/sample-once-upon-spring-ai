///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 25+

import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.List;

/// Wraps DefaultToolCallingManager to escape literal control characters in LLM-generated
/// tool call arguments. Bedrock models sometimes emit unescaped newlines/tabs inside JSON
/// string values, which causes Jackson to reject them when BedrockProxyChatModel re-parses
/// tool arguments during the conversation loop in createRequest().
class SanitizingToolCallingManager implements ToolCallingManager {

    private final DefaultToolCallingManager delegate = DefaultToolCallingManager.builder().build();

    @Override
    public List<ToolDefinition> resolveToolDefinitions(ToolCallingChatOptions chatOptions) {
        return delegate.resolveToolDefinitions(chatOptions);
    }

    @Override
    public ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse chatResponse) {
        return delegate.executeToolCalls(prompt, sanitizeToolCallArgs(chatResponse));
    }

    private ChatResponse sanitizeToolCallArgs(ChatResponse response) {
        var generations = response.getResults().stream().map(gen -> {
            var output = gen.getOutput();
            if (!output.hasToolCalls()) return gen;

            var sanitizedCalls = output.getToolCalls().stream()
                    .map(tc -> new AssistantMessage.ToolCall(
                            tc.id(), tc.type(), tc.name(),
                            escapeControlChars(tc.arguments())))
                    .toList();

            var sanitizedMessage = AssistantMessage.builder()
                    .content(output.getText())
                    .properties(output.getMetadata())
                    .toolCalls(sanitizedCalls)
                    .media(output.getMedia())
                    .build();
            return new Generation(sanitizedMessage, gen.getMetadata());
        }).toList();

        return new ChatResponse(generations, response.getMetadata());
    }

    /// Escapes literal control characters inside JSON string values.
    static String escapeControlChars(String json) {
        var sb = new StringBuilder(json.length());
        boolean inString = false;
        boolean escaped = false;
        for (int i = 0; i < json.length(); i++) {
            var ch = json.charAt(i);
            if (escaped) {
                sb.append(ch);
                escaped = false;
                continue;
            }
            if (ch == '\\' && inString) {
                sb.append(ch);
                escaped = true;
                continue;
            }
            if (ch == '"') {
                inString = !inString;
                sb.append(ch);
                continue;
            }
            if (inString && ch < 0x20) {
                switch (ch) {
                    case '\n' -> sb.append("\\n");
                    case '\r' -> sb.append("\\r");
                    case '\t' -> sb.append("\\t");
                    default -> sb.append("\\u%04x".formatted((int) ch));
                }
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }
}