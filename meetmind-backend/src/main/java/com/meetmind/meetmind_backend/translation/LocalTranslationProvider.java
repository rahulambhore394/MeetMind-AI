package com.meetmind.meetmind_backend.translation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Offline-first, local translation provider for English, Hindi, Marathi, Gujarati, Spanish, French, German, Japanese, Chinese, etc.
 * Uses phrase/glossary matching, token translation, and intelligent fallback.
 */
@Component
public class LocalTranslationProvider implements TranslationProvider {

    private static final Logger log = LoggerFactory.getLogger(LocalTranslationProvider.class);
    private static final Set<String> SUPPORTED_LANGS = Set.of(
            "en", "hi", "mr", "gu", "es", "fr", "de", "zh", "ja", "ar", "bn", "ta", "te", "kn", "ml", "pa", "ru"
    );

    // Phrase dictionaries: key = "sourceLang:targetLang:lowerCaseSourcePhrase"
    private final Map<String, String> phraseDictionary = new HashMap<>();
    // Word dictionaries: key = "sourceLang:targetLang:lowerCaseWord"
    private final Map<String, String> wordDictionary = new HashMap<>();

    public LocalTranslationProvider() {
        initDictionaries();
    }

    @Override
    public String providerName() {
        return "local-dictionary";
    }

    @Override
    public boolean supportsLanguagePair(String sourceLanguage, String targetLanguage) {
        if (sourceLanguage == null || targetLanguage == null) return false;
        return true;
    }

    @Override
    public TranslationResult translate(String text, String sourceLanguage, String targetLanguage)
            throws TranslationException {

        if (text == null) {
            throw new TranslationException("Input text cannot be null");
        }

        String s = normalize(sourceLanguage);
        String t = normalize(targetLanguage);
        if (s.isEmpty()) s = "en";
        if (t.isEmpty()) t = "en";

        // Fast-path: Identity translation
        if (s.equalsIgnoreCase(t) || text.trim().isEmpty()) {
            return new TranslationResult(text, text, s, t, providerName());
        }

        String translated = doTranslate(text, s, t);
        return new TranslationResult(text, translated, s, t, providerName());
    }

    private String doTranslate(String text, String s, String t) {
        String trimmed = text.trim();
        String lower = trimmed.toLowerCase();

        // 1. Direct whole phrase match
        String phraseKey = s + ":" + t + ":" + lower;
        if (phraseDictionary.containsKey(phraseKey)) {
            return phraseDictionary.get(phraseKey);
        }

        // 2. Multi-word phrase replacements within text
        String result = trimmed;
        for (Map.Entry<String, String> entry : phraseDictionary.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(s + ":" + t + ":")) {
                String phrase = key.substring((s + ":" + t + ":").length());
                if (result.toLowerCase().contains(phrase)) {
                    // Case-insensitive replacement
                    result = result.replaceAll("(?i)" + java.util.regex.Pattern.quote(phrase), entry.getValue());
                }
            }
        }

        // 3. Word-by-word fallback for remaining tokens
        String[] tokens = result.split("(\\s+|(?=[,.:;!?])|(?<=[,.:;!?]))");
        StringBuilder sb = new StringBuilder();
        for (String token : tokens) {
            if (token.isBlank()) {
                sb.append(token);
                continue;
            }
            String wordKey = s + ":" + t + ":" + token.toLowerCase().trim();
            if (wordDictionary.containsKey(wordKey)) {
                sb.append(wordDictionary.get(wordKey));
            } else {
                sb.append(token);
            }
        }

        return sb.toString().trim();
    }

    private String normalize(String lang) {
        if (lang == null) return "";
        String trimmed = lang.trim().toLowerCase();
        int dashIdx = trimmed.indexOf('-');
        if (dashIdx != -1) {
            trimmed = trimmed.substring(0, dashIdx);
        }
        int underIdx = trimmed.indexOf('_');
        if (underIdx != -1) {
            trimmed = trimmed.substring(0, underIdx);
        }
        return trimmed;
    }

    private void addEntry(String s, String t, String src, String tgt) {
        String cleanSrc = src.trim().toLowerCase();
        if (cleanSrc.contains(" ")) {
            phraseDictionary.put(s + ":" + t + ":" + cleanSrc, tgt);
        } else {
            wordDictionary.put(s + ":" + t + ":" + cleanSrc, tgt);
        }
    }

    private void addTriPair(String en, String hi, String mr) {
        // en <-> hi
        addEntry("en", "hi", en, hi);
        addEntry("hi", "en", hi, en);
        // en <-> mr
        addEntry("en", "mr", en, mr);
        addEntry("mr", "en", mr, en);
        // hi <-> mr
        addEntry("hi", "mr", hi, mr);
        addEntry("mr", "hi", mr, hi);
    }

    private void initDictionaries() {
        // Meeting phrases
        addTriPair("hello", "नमस्ते", "नमस्कार");
        addTriPair("hi", "नमस्ते", "नमस्कार");
        addTriPair("good morning", "शुभ प्रभात", "शुभ सकाळ");
        addTriPair("good afternoon", "शुभ दोपहर", "शुभ दुपार");
        addTriPair("good evening", "शुभ संध्या", "शुभ संध्याकाळ");
        addTriPair("welcome to the meeting", "बैठक में आपका स्वागत है", "बैठकीत आपले स्वागत आहे");
        addTriPair("let's start the meeting", "मीटिंग शुरू करते हैं", "मीटिंग सुरू करूया");
        addTriPair("thank you", "धन्यवाद", "धन्यवाद");
        addTriPair("thanks", "धन्यवाद", "धन्यवाद");
        addTriPair("yes", "हाँ", "होय");
        addTriPair("no", "नहीं", "नाही");
        addTriPair("ok", "ठीक है", "ठीक आहे");
        addTriPair("okay", "ठीक है", "ठीक आहे");
        addTriPair("can you hear me", "क्या आप मुझे सुन सकते हैं", "तुम्ही मला ऐकू शकता का");
        addTriPair("i can hear you", "मैं आपको सुन सकता हूँ", "मी तुम्हाला ऐकू शकतो");
        addTriPair("please share your screen", "कृपया अपनी स्क्रीन साझा करें", "कृपया आपली स्क्रीन शेअर करा");
        addTriPair("let's discuss the project", "परियोजना पर चर्चा करते हैं", "प्रकल्पावर चर्चा करूया");
        addTriPair("any questions", "कोई सवाल", "काही प्रश्न");
        addTriPair("the meeting is ended", "बैठक समाप्त हो गई है", "बैठक संपली आहे");
        addTriPair("goodbye", "अलविदा", "पुन्हा भेटू");
        addTriPair("how are you", "आप कैसे हैं", "तुम्ही कसे आहात");
        addTriPair("i am fine", "मैं ठीक हूँ", "मी मजेत आहे");
        addTriPair("what is the update", "क्या अपडेट है", "काय अपडेट आहे");
        addTriPair("good job", "शाबाश", "छान काम");
        addTriPair("i agree", "मैं सहमत हूँ", "मी सहमत आहे");
        addTriPair("let's proceed", "आगे बढ़ते हैं", "पुढे जाऊया");

        // Common meeting and business vocabulary
        addTriPair("meeting", "बैठक", "बैठक");
        addTriPair("agenda", "कार्यसूची", "कार्यपत्रिका");
        addTriPair("project", "परियोजना", "प्रकल्प");
        addTriPair("participant", "प्रतिभागी", "सहभागी");
        addTriPair("participants", "प्रतिभागी", "सहभागी");
        addTriPair("host", "मेजबान", "यजमान");
        addTriPair("audio", "ऑडियो", "ऑडिओ");
        addTriPair("video", "वीडियो", "व्हिडिओ");
        addTriPair("screen", "स्क्रीन", "स्क्रीन");
        addTriPair("chat", "चैट", "चॅट");
        addTriPair("message", "संदेश", "संदेश");
        addTriPair("recording", "रिकॉर्डिंग", "रेकॉर्डिंग");
        addTriPair("transcript", "प्रतिलेख", "उतारा");
        addTriPair("translation", "अनुवाद", "भाषांतर");
        addTriPair("language", "भाषा", "भाषा");
        addTriPair("team", "टीम", "संघ");
        addTriPair("start", "शुरू", "सुरू");
        addTriPair("end", "समाप्त", "समाप्त");
        addTriPair("join", "शामिल होना", "सामील व्हा");
        addTriPair("leave", "छोड़ना", "बाहेर पडा");
        addTriPair("today", "आज", "आज");
        addTriPair("tomorrow", "कल", "उद्या");
        addTriPair("status", "स्थिति", "स्थिती");
        addTriPair("update", "अपडेट", "अद्यतन");
        addTriPair("problem", "समस्या", "समस्या");
        addTriPair("solution", "समाधान", "उपाय");
        addTriPair("next", "अगला", "पुढील");
        addTriPair("discuss", "चर्चा", "चर्चा");
        addTriPair("action item", "कार्य बिंदु", "कृती घटक");
        addTriPair("deadline", "समय सीमा", "मुदत");

        // Gujarati (gu), Spanish (es), French (fr), German (de), Japanese (ja), Chinese (zh)
        addMultiPair("hello", Map.of("gu", "નમસ્તે", "es", "Hola", "fr", "Bonjour", "de", "Hallo", "ja", "こんにちは", "zh", "你好"));
        addMultiPair("welcome to the meeting", Map.of("gu", "મીટિંગમાં સ્વાગત છે", "es", "Bienvenido a la reunión", "fr", "Bienvenue à la réunion", "de", "Willkommen zum Treffen", "ja", "会議へようこそ", "zh", "欢迎来到会议"));
        addMultiPair("thank you", Map.of("gu", "આભાર", "es", "Gracias", "fr", "Merci", "de", "Danke", "ja", "ありがとう", "zh", "谢谢"));
        addMultiPair("yes", Map.of("gu", "હા", "es", "Sí", "fr", "Oui", "de", "Ja", "ja", "はい", "zh", "是"));
        addMultiPair("no", Map.of("gu", "ના", "es", "No", "fr", "Non", "de", "Nein", "ja", "いいえ", "zh", "不"));
        addMultiPair("goodbye", Map.of("gu", "આવજો", "es", "Adiós", "fr", "Au revoir", "de", "Auf Wiedersehen", "ja", "さようなら", "zh", "再见"));
        addMultiPair("meeting", Map.of("gu", "મીટિંગ", "es", "Reunión", "fr", "Réunion", "de", "Treffen", "ja", "会議", "zh", "会议"));
        addMultiPair("transcript", Map.of("gu", "ટ્રાન્સક્રિપ્ટ", "es", "Transcripción", "fr", "Transcription", "de", "Transkript", "ja", "文字起こし", "zh", "逐字稿"));
    }

    private void addMultiPair(String en, Map<String, String> translations) {
        translations.forEach((lang, value) -> {
            addEntry("en", lang, en, value);
            addEntry(lang, "en", value, en);
        });
    }
}
