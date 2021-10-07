// Copyright Yahoo. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.language.simple;

import com.yahoo.language.Language;
import com.yahoo.language.LinguisticsCase;
import com.yahoo.language.process.*;
import com.yahoo.language.simple.kstem.KStemmer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * <p>A tokenizer which splits on whitespace, normalizes and transforms using the given implementations
 * and stems using the kstem algorithm.</p>
 *
 * <p>This is not multithread safe.</p>
 *
 * @author Mathias Mølster Lidal
 * @author bratseth
 */
public class SimpleTokenizer implements Tokenizer {

    private static final Logger log = Logger.getLogger(SimpleTokenizer.class.getName());
    private final static int SPACE_CODE = 32;

    private final Normalizer normalizer;
    private final Transformer transformer;
    private final KStemmer stemmer = new KStemmer();
    private final SpecialTokenRegistry specialTokenRegistry;

    public SimpleTokenizer() {
        this(new SimpleNormalizer(), new SimpleTransformer());
    }

    public SimpleTokenizer(Normalizer normalizer) {
        this(normalizer, new SimpleTransformer());
    }

    public SimpleTokenizer(Normalizer normalizer, Transformer transformer) {
        this(normalizer, transformer, new SpecialTokenRegistry(List.of()));
    }

    public SimpleTokenizer(Normalizer normalizer, Transformer transformer, SpecialTokenRegistry specialTokenRegistry) {
        this.normalizer = normalizer;
        this.transformer = transformer;
        this.specialTokenRegistry = specialTokenRegistry;
    }

    @Override
    public Iterable<Token> tokenize(String input, Language language, StemMode stemMode, boolean removeAccents) {
        if (input.isEmpty()) return Collections.emptyList();

        List<Token> tokens = new ArrayList<>();
        int nextCode = input.codePointAt(0);
        TokenType prevType = SimpleTokenType.valueOf(nextCode);
        for (int prev = 0, next = Character.charCount(nextCode); next <= input.length(); ) {
            nextCode = next < input.length() ? input.codePointAt(next) : SPACE_CODE;
            TokenType nextType = SimpleTokenType.valueOf(nextCode);
            if (!prevType.isIndexable() || !nextType.isIndexable()) {
                String original = input.substring(prev, next);
                String token = processToken(original, language, stemMode, removeAccents);
                tokens.add(new SimpleToken(original).setOffset(prev)
                                                    .setType(prevType)
                                                    .setTokenString(token));
                prev = next;
                prevType = nextType;
            }
            next += Character.charCount(nextCode);
        }
        return tokens;
    }

    private String processToken(String token, Language language, StemMode stemMode, boolean removeAccents) {
        String original = token;
        log.log(Level.FINEST, () -> "processToken '" + original + "'");
        token = normalizer.normalize(token);
        token = LinguisticsCase.toLowerCase(token);
        if (removeAccents)
            token = transformer.accentDrop(token, language);
        if (stemMode != StemMode.NONE) {
            String oldToken = token;
            token = stemmer.stem(token);
            String newToken = token;
            log.log(Level.FINEST, () -> "stem '" + oldToken+"' to '" + newToken+"'");
        }
        String result = token;
        log.log(Level.FINEST, () -> "processed token is: " + result);
        return result;
    }

}
