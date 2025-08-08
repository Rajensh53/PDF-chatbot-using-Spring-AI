package com.example.PDF.chatbot.using.Spring.AI.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.stream.Collectors;
import org.springframework.ai.chat.model.ChatModel;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final VectorStore vectorStore;
    private final ChatModel chatClient;

    public String answerQuery(String userQuery) {
        log.info("Processing user query: {}", userQuery);

        List<Document> similarDocuments = vectorStore.similaritySearch(userQuery);
        log.info("Found {} relevant documents", similarDocuments.size());

        String documentContent = similarDocuments.stream()
                .map(Document::getContent)
                .collect(Collectors.joining("\n"));

        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(
                """
                You are a helpful assistant who is an expert in answering questions based on the provided context.
                The context is from a PDF document.
                Be concise and do not include any preamble.

                CONTEXT:
                {context}
                """
        );

        var systemMessage = systemPromptTemplate.createMessage(java.util.Map.of("context", documentContent));

        var userMessage = new org.springframework.ai.chat.messages.UserMessage(userQuery);

        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

        return chatClient.call(prompt).getResult().getOutput().getContent();
    }
}
 