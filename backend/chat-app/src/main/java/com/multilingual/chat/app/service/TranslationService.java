package com.multilingual.chat.app.repository;

public interface TranslationService {

    String translate(String text, String sourceLanguage, String targetLanguage);

    boolean isTranslationRequired(String sourceLanguage, String targetLanguage);
}
