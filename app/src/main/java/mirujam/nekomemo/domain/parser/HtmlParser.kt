package mirujam.nekomemo.domain.parser

import mirujam.nekomemo.domain.model.ExtractedQuestionBank

/**
 * Contract for parsing HTML content into structured question banks.
 */
interface HtmlParser {
    /**
     * Parse HTML string into an [ExtractedQuestionBank].
     *
     * @param html Raw HTML content from a webpage.
     * @return Parsed question bank with extracted questions.
     */
    fun parse(html: String): ExtractedQuestionBank

    /**
     * Decode HTML-encoded string that has been escaped by JavaScript.
     * Handles JSON-wrapped strings and provides a fallback for plain escapes.
     *
     * @param raw The encoded raw string, may be null or blank.
     * @return Decoded plain text string.
     */
    fun decodeHtmlFromJs(raw: String?): String
}
