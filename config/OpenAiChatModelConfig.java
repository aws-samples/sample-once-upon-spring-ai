///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 25+

//DEPS org.springframework.ai:spring-ai-openai:2.0.0-M2

import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;

public class OpenAiChatModelConfig {

    public static OpenAiChatModel createChatModel() {
        var openAiApi = OpenAiApi.builder()
            .apiKey(System.getEnv("OPENAI_API_KEY"))
            .build();

        var options = OpenAiChatOptions.builder()
            .model("gpt-4o-mini")
            .temperature(0.7)
            .maxTokens(200)
            .build();

        return OpenAiChatModel.builder()
            .openAiApi(openAiApi)
            .defaultOptions(options)
            .build();
    }
}