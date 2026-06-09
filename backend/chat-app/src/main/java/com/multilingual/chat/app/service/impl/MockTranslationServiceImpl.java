package com.multilingual.chat.app.service.impl;

import org.springframework.stereotype.Service;
import com.multilingual.chat.app.service.TranslationService;

@Service
public class MockTranslationServiceImpl implements TranslationService {

    @Override
    public boolean isTranslationRequired(String sourceLanguage, String targetLanguage) {
        if (sourceLanguage == null || targetLanguage == null) {
            return false;
        }

        return !sourceLanguage.trim().equalsIgnoreCase(targetLanguage.trim());
    }

    @Override
    public String translate(String text, String sourceLanguage, String targetLanguage) {
        return "[Translated from " + sourceLanguage + " to " + targetLanguage + "]: " + text;
    }

    @Override
    public String translateToLanguage(String text, String targetLanguage) {
        return "[Translated to " + targetLanguage + "]: " + text;
    }
}
