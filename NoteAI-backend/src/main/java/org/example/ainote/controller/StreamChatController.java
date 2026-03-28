package org.example.ainote.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class StreamChatController {

    private final ChatClient chatClient;

    public StreamChatController(ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).build();
    }

    @GetMapping(path = "/string", produces = MediaType.TEXT_PLAIN_VALUE)
    public String chat(@RequestParam String prompt,
                       @RequestParam(required = false, defaultValue = "en") String lang) {
        String formattedPrompt = String.format("""
                You are a helpful assistant that writes **short summaries** of user notes.
                Here's the note:
                %s
                Summarize it in **the same language** as the original note.
                Do **not** translate or paraphrase the content into another language.
                If the language cannot be reliably detected, default to %s.
                Keep the summary concise — 1–2 sentences, **no introductions or conclusions**.
                Only output the summary.
                """, prompt, lang);

        return chatClient.prompt()
                .user(formattedPrompt)
                .call()
                .content();
    }
}