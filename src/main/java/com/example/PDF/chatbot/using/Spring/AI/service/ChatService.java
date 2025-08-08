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
        log.info("User query received: {}", userQuery);

        log.info("Searching for similar documents in vector store...");
        List<Document> similarDocuments = vectorStore.similaritySearch(userQuery);
        log.info("Found {} relevant documents.", similarDocuments.size());

        String documentContent = similarDocuments.stream()
                .map(Document::getContent)
                .collect(Collectors.joining("\n"));

        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(
                """
                You are a helpful assistant that answers questions based on the provided CONTEXT from a PDF document.
                If the answer is not explicitly available in the CONTEXT, but can be reasonably inferred, you may do so.
                If the answer cannot be found or reasonably inferred from the CONTEXT, please state: \"I cannot answer that question based on the provided document.\"
                Keep your answers concise and to the point.

                CONTEXT:
                {context}
                """
        );

        var systemMessage = systemPromptTemplate.createMessage(java.util.Map.of("context", documentContent));

        var userMessage = new org.springframework.ai.chat.messages.UserMessage(userQuery);

        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

        log.info("Generating response from LLM...");
        String responseContent = chatClient.call(prompt).getResult().getOutput().getContent();
        log.info("Response generated.");
        return responseContent;
    }
}

