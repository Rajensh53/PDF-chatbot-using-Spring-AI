package com.example.PDF.chatbot.using.Spring.AI.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final VectorStore vectorStore;
    private final ChatModel chatClient;

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");

    public String answerQuery(String userQuery, List<Message> conversationHistory) {
        log.info("User query received: {}", userQuery);

        log.info("Searching for similar documents in vector store...");
        List<Document> similarDocuments = vectorStore.similaritySearch(userQuery);
        log.info("Found {} relevant documents.", similarDocuments.size());

        String documentContent = similarDocuments.stream()
                .map(Document::getContent)
                .collect(Collectors.joining("\n"));

        // Strip HTML tags from document content before sending to LLM
        documentContent = stripHtmlTags(documentContent);

        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(
                """
                        You are an AI assistant that answers questions strictly based on the provided CONTEXT from a PDF document.
                        
                        Instructions:
                        
                        If the answer is explicitly stated in the CONTEXT, provide it clearly and concisely.
                        
                        If the answer is not explicitly stated but can be logically inferred from the CONTEXT, provide the inferred answer and briefly explain your reasoning.
                        
                        If the answer cannot be found or reasonably inferred, respond exactly with:
                        
                        I cannot answer that question based on the provided document.
                        
                        Use clear, well-structured, and concise formatting in your responses (bullet points, numbered lists, or short paragraphs as appropriate).
                        
                        CONTEXT:
                        {context}
                """
        );

        SystemMessage systemMessage = (SystemMessage) systemPromptTemplate.createMessage(java.util.Map.of("context", documentContent));

        UserMessage userMessage = new UserMessage(userQuery);

        List<Message> messages = new ArrayList<>(conversationHistory);
        messages.add(systemMessage);
        messages.add(userMessage);

        Prompt prompt = new Prompt(messages);

        log.info("Generating response from LLM...");
        var response = chatClient.call(prompt);
        log.info("LLM response metadata: {}", response.getMetadata());
        String responseContent = response.getResult().getOutput().getContent();
        log.info("Response generated.");

        // Strip HTML tags from the LLM response before returning
        return stripHtmlTags(responseContent);
    }

    private String stripHtmlTags(String htmlString) {
        if (htmlString == null || htmlString.isEmpty()) {
            return htmlString;
        }
        Matcher matcher = HTML_TAG_PATTERN.matcher(htmlString);
        return matcher.replaceAll("");
    }
}

