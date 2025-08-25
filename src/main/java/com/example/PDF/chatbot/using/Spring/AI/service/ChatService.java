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

    public reactor.core.publisher.Flux<String> answerQueryStream(String userQuery, List<Message> conversationHistory) {
        log.info("User query received for streaming: {}", userQuery);

        log.info("Searching for similar documents in vector store...");
        org.springframework.ai.vectorstore.SearchRequest searchRequest = org.springframework.ai.vectorstore.SearchRequest.query(userQuery).withTopK(3);
        List<Document> similarDocuments = vectorStore.similaritySearch(searchRequest);
        log.info("Found {} relevant documents.", similarDocuments.size());

        String documentContent = similarDocuments.stream()
                .map(Document::getContent)
                .collect(Collectors.joining("\n"));

        // Strip HTML tags from document content before sending to LLM
        documentContent = stripHtmlTags(documentContent);

        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(
                """
                            You are a specialized AI assistant. Your ONLY function is to answer questions based on the CONTEXT provided below. You are forbidden from using any external knowledge.

                            Rules to follow:

                            1.  Carefully analyze the user's question and the CONTEXT.
                            2.  If the user's question is a greeting (e.g., "Hi", "Hello"), respond politely and invite them to ask about the document. Do not answer any other questions in the same response.
                            3.  If the question is about the content of the document, follow these steps:
                                a. Find the answer directly within the CONTEXT.
                                b. If the answer is explicitly stated, provide it clearly and concisely.
                                c. If the answer is not explicit but can be logically inferred, provide the inferred answer and state that it is an inference based on the text.
                                d. If the answer cannot be found or inferred from the CONTEXT, you MUST respond with the exact phrase: "I cannot answer that question based on the provided document." Do not offer any other information.
                            4.  If the user's question is NOT related to the CONTEXT, you MUST respond with the exact phrase: "Your question does not appear to relate to the provided document. Please ask something related." Do not attempt to answer it.
                            5.  Formatting Rules for answers:
                                - Use numbered steps (1, 2, 3, …) for workflows or processes.
                                - Use bullet points (•) for lists or factual details.
                                - Keep answers short and clear.

                            IMPORTANT: Before answering, ask yourself: "Is the answer to this question contained within the CONTEXT below?" If the answer is no, you must follow rule 3d or 4.

                            CONTEXT from the PDF:
                            {context}
                """
        );

        SystemMessage systemMessage = (SystemMessage) systemPromptTemplate.createMessage(java.util.Map.of("context", documentContent));

        UserMessage userMessage = new UserMessage(userQuery);

        List<Message> messages = new ArrayList<>(conversationHistory);
        messages.add(systemMessage);
        messages.add(userMessage);

        Prompt prompt = new Prompt(messages);

        log.info("Generating streaming response from LLM...");

        return chatClient.stream(prompt)
                .map(response -> response.getResult().getOutput().getContent())
                .map(this::stripHtmlTags)
                .collectList()
                .map(list -> String.join("", list))
                .doOnSuccess(s -> log.info("Streaming response completed."))
                .doOnError(e -> log.error("Error during streaming response", e))
                .flux();
    }

    private String stripHtmlTags(String htmlString) {
        if (htmlString == null || htmlString.isEmpty()) {
            return htmlString;
        }
        Matcher matcher = HTML_TAG_PATTERN.matcher(htmlString);
        return matcher.replaceAll("");
    }
}