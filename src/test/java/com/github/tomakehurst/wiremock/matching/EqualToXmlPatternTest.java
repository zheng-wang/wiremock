/*
 * Copyright (C) 2011 Thomas Akehurst
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.tomakehurst.wiremock.matching;

import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.common.LocalNotifier;
import com.github.tomakehurst.wiremock.common.Notifier;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Locale;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToXml;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class EqualToXmlPatternTest {

    private Mockery context;

    @Rule
    public WireMockRule wm = new WireMockRule(options().dynamicPort());

    @Before
    public void init() {
        context = new Mockery();
        LocalNotifier.set(new ConsoleNotifier(true));

        // We assert English XML parser error messages in this test. So we set our default locale to English to make
        // this test succeed even for users with non-English default locales.
        Locale.setDefault(Locale.ENGLISH);
    }

    @After
    public void cleanup() {
        LocalNotifier.set(null);
    }

    @Test
    public void returnsNoMatchAnd1DistanceWhenActualIsNull() {
        EqualToXmlPattern pattern = new EqualToXmlPattern(
            "<things>\n" +
            "    <thing characteristic=\"tepid\"/>\n" +
            "    <thing characteristic=\"tedious\"/>\n" +
            "</things>"
        );

        MatchResult matchResult = pattern.match(null);

        assertFalse(matchResult.isExactMatch());
        assertThat(matchResult.getDistance(), is(1.0));
    }

    @Test
    public void returnsNoMatchAnd1DistanceWhenActualIsEmpty() {
        EqualToXmlPattern pattern = new EqualToXmlPattern(
            "<things>\n" +
                "    <thing characteristic=\"tepid\"/>\n" +
                "    <thing characteristic=\"tedious\"/>\n" +
                "</things>"
        );

        MatchResult matchResult = pattern.match("");

        assertFalse(matchResult.isExactMatch());
        assertThat(matchResult.getDistance(), is(1.0));
    }

    @Test
    public void returnsNoMatchAnd1DistanceWhenActualIsNotXml() {
        EqualToXmlPattern pattern = new EqualToXmlPattern(
            "<things>\n" +
                "    <thing characteristic=\"tepid\"/>\n" +
                "    <thing characteristic=\"tedious\"/>\n" +
                "</things>"
        );

        MatchResult matchResult = pattern.match("{ \"markup\": \"wrong\" }");

        assertFalse(matchResult.isExactMatch());
        assertThat(matchResult.getDistance(), is(1.0));
    }

    @Test
    public void returnsExactMatchWhenDocumentsAreIdentical() {
        EqualToXmlPattern pattern = new EqualToXmlPattern(
            "<things>\n" +
            "    <thing characteristic=\"tepid\"/>\n" +
            "    <thing characteristic=\"tedious\"/>\n" +
            "</things>"
        );

        assertTrue(pattern.match(
            "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
            "<things>\n" +
            "    <thing characteristic=\"tepid\"/>\n" +
            "    <thing characteristic=\"tedious\"/>\n" +
            "</things>"
        ).isExactMatch());
    }

    @Test
    public void returnsExactMatchWhenDocumentsAreIdenticalOtherThanWhitespace() {
        EqualToXmlPattern pattern = new EqualToXmlPattern(
            "<things>\n" +
            "    <thing characteristic=\"tepid\"/>\n" +
            "    <thing characteristic=\"tedious\"/>\n" +
            "</things>"
        );

        assertTrue(pattern.match(
            "<things><thing characteristic=\"tepid\"/><thing characteristic=\"tedious\"/></things>"
        ).isExactMatch());
    }

    @Test
    public void returnsNoMatchWhenDocumentsAreTotallyDifferent() {
        EqualToXmlPattern pattern = new EqualToXmlPattern(
            "<things>\n" +
            "    <thing characteristic=\"tepid\"/>\n" +
            "    <thing characteristic=\"tedious\"/>\n" +
            "</things>"
        );

        MatchResult matchResult = pattern.match("<no-things-at-all />");

        assertFalse(matchResult.isExactMatch());
        assertThat(matchResult.getDistance(), is(0.5)); //Not high enough really, some more tweaking needed
    }

    @Test
    public void returnsLowDistanceWhenActualDocumentHasMissingElement() {
        EqualToXmlPattern pattern = new EqualToXmlPattern(
            "<things>\n" +
            "    <thing characteristic=\"tepid\"/>\n" +
            "    <thing characteristic=\"tedious\"/>\n" +
            "</things>"
        );

        MatchResult matchResult = pattern.match(
            "<things>\n" +
            "    <thing characteristic=\"tepid\"/>\n" +
            "</things>"
        );

        assertThat(matchResult.getDistance(), closeTo(0.14, 2));
    }

    @Test
    public void returnsExactMatchOnNamespacedXml() {
        EqualToXmlPattern pattern = new EqualToXmlPattern(
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
            "    <soap:Body>\n" +
            "        <stuff xmlns=\"https://example.com/mynamespace\">\n" +
            "            <things />\n" +
            "        </stuff>\n" +
            "    </soap:Body>\n" +
            "</soap:Envelope>\n"
        );

        MatchResult match = pattern.match(
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                "    <soap:Body>\n" +
                "        <stuff xmlns=\"https://example.com/mynamespace\">\n" +
                "            <things />\n" +
                "        </stuff>\n" +
                "    </soap:Body>\n" +
                "</soap:Envelope>\n"
        );

        assertThat(match.getDistance(), is(0.0));
        assertTrue(match.isExactMatch());
    }

    @Test
    public void returnsExactMatchOnNamespacedXmlWhenNamespacePrefixesDiffer() {
        EqualToXmlPattern pattern = new EqualToXmlPattern(
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<shampoo:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:shampoo=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                "    <shampoo:Body>\n" +
                "        <stuff xmlns=\"https://example.com/mynamespace\">\n" +
                "            <things />\n" +
                "        </stuff>\n" +
                "    </shampoo:Body>\n" +
                "</shampoo:Envelope>\n"
        );

        MatchResult match = pattern.match(
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                "    <soap:Body>\n" +
                "        <stuff xmlns=\"https://example.com/mynamespace\">\n" +
                "            <things />\n" +
                "        </stuff>\n" +
                "    </soap:Body>\n" +
                "</soap:Envelope>\n"
        );

        assertThat(match.getDistance(), is(0.0));
        assertTrue(match.isExactMatch());
    }

    @Test
    public void doesNotReturnExactMatchWhenNamespaceUriDiffers() {
        EqualToXmlPattern pattern = new EqualToXmlPattern(
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                "    <soap:Body>\n" +
                "        <stuff xmlns=\"https://example.com/mynamespace\">\n" +
                "            <things />\n" +
                "        </stuff>\n" +
                "    </soap:Body>\n" +
                "</soap:Envelope>\n"
        );

        assertFalse(pattern.match(
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                "    <soap:Body>\n" +
                "        <stuff xmlns=\"https://example.com/the-wrong-namespace\">\n" +
                "            <things />\n" +
                "        </stuff>\n" +
                "    </soap:Body>\n" +
                "</soap:Envelope>\n"
        ).isExactMatch());
    }

    @Test
    public void returnsExactMatchWhenAttributesAreInDifferentOrder() {
        EqualToXmlPattern pattern = new EqualToXmlPattern("<my-attribs one=\"1\" two=\"2\" three=\"3\"/>");
        assertTrue(pattern.match("<my-attribs two=\"2\" one=\"1\" three=\"3\"/>").isExactMatch());
    }

    @Test
    public void returnsExactMatchWhenElementsAreInDifferentOrder() {
        EqualToXmlPattern pattern = new EqualToXmlPattern(
            "<my-elements>\n" +
            "    <one />\n" +
            "    <two />\n" +
            "    <three />\n" +
            "</my-elements>"
        );

        assertTrue(pattern.match(
            "<my-elements>\n" +
            "    <two />\n" +
            "    <three />\n" +
            "    <one />\n" +
            "</my-elements>"
        ).isExactMatch());
    }

    @Test
    public void returnsNoMatchWhenTagNamesDifferAndContentIsSame() throws Exception {
        final EqualToXmlPattern pattern = new EqualToXmlPattern("<one>Hello</one>");
        final MatchResult matchResult = pattern.match("<two>Hello</two>");

        assertThat(matchResult.isExactMatch(), equalTo(false));
        assertThat(matchResult.getDistance(), not(equalTo(0.0)));
    }

    @Test
    public void logsASensibleErrorMessageWhenActualXmlIsBadlyFormed() {
        expectInfoNotification("Failed to process XML. Content is not allowed in prolog.");
        equalToXml("<well-formed />").match("badly-formed >").isExactMatch();
    }

    @Test
    public void doesNotFetchDtdBecauseItCouldResultInAFailedMatch() throws Exception {
        String xmlWithDtdThatCannotBeFetched = "<!DOCTYPE my_request SYSTEM \"https://thishostname.doesnotexist.com/one.dtd\"><do_request/>";
        EqualToXmlPattern pattern = new EqualToXmlPattern(xmlWithDtdThatCannotBeFetched);
        assertTrue(pattern.match(xmlWithDtdThatCannotBeFetched).isExactMatch());
    }

    private void expectInfoNotification(final String message) {
        final Notifier notifier = context.mock(Notifier.class);
        context.checking(new Expectations() {{
            one(notifier).info(with(containsString(message)));
        }});
        LocalNotifier.set(notifier);
    }

    @Test
    public void createEqualToXmlPatternWithPlaceholderFromWireMockClass() {
        Boolean enablePlaceholders = Boolean.TRUE;
        String placeholderOpeningDelimiterRegex = "theOpeningDelimiterRegex";
        String placeholderClosingDelimiterRegex = "theClosingDelimiterRegex";
        EqualToXmlPattern equalToXmlPattern = equalToXml("<a/>", enablePlaceholders, placeholderOpeningDelimiterRegex, placeholderClosingDelimiterRegex);
        assertEquals(enablePlaceholders, equalToXmlPattern.isEnablePlaceholders());
        assertEquals(placeholderOpeningDelimiterRegex, equalToXmlPattern.getPlaceholderOpeningDelimiterRegex());
        assertEquals(placeholderClosingDelimiterRegex, equalToXmlPattern.getPlaceholderClosingDelimiterRegex());
    }

    @Test
    public void createEqualToXmlPatternWithPlaceholderFromWireMockClass_DefaultDelimiters() {
        Boolean enablePlaceholders = Boolean.TRUE;
        EqualToXmlPattern equalToXmlPattern = equalToXml("<a/>", enablePlaceholders);
        assertEquals(enablePlaceholders, equalToXmlPattern.isEnablePlaceholders());
        assertNull(equalToXmlPattern.getPlaceholderOpeningDelimiterRegex());
        assertNull(equalToXmlPattern.getPlaceholderClosingDelimiterRegex());
    }

    @Test
    public void returnsMatchWhenTextNodeIsIgnored() {
        String expectedXml = "<a>#{xmlunit.ignore}</a>";
        String actualXml = "<a>123</a>";
        EqualToXmlPattern pattern = new EqualToXmlPattern(expectedXml, true, "#\\{", "}");
        MatchResult matchResult = pattern.match(actualXml);

        assertTrue(matchResult.isExactMatch());
        assertEquals(matchResult.getDistance(), 0.0, 0);
    }

    @Test
    public void returnsMatchWhenTextNodeIsIgnored_DefaultDelimiters() {
        String expectedXml = "<a>${xmlunit.ignore}</a>";
        String actualXml = "<a>123</a>";
        EqualToXmlPattern pattern = new EqualToXmlPattern(expectedXml, true, null, null);
        MatchResult matchResult = pattern.match(actualXml);

        assertTrue(matchResult.isExactMatch());
        assertEquals(matchResult.getDistance(), 0.0, 0);
    }
}
