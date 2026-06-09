package com.multilingual.chat.app.service;

public interface TranslationService {

    String translate(String text, String sourceLanguage, String targetLanguage);

    String translateToLanguage(String text, String targetLanguage);

    boolean isTranslationRequired(String sourceLanguage, String targetLanguage);
}
