package com.aizuda.snail.ai.features.rag.strategy.parser;

import com.aizuda.snail.ai.common.execption.SnailAiException;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Component
public class PlainTextParser implements DocumentParser {

    @Override
    public boolean supports(String fileType) {
        return "txt".equalsIgnoreCase(fileType) || "text".equalsIgnoreCase(fileType);
    }

    @Override
    public String parse(InputStream inputStream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            throw new SnailAiException("Failed to parse text file", e);
        }
    }
}
