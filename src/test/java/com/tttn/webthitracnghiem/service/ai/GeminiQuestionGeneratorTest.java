package com.tttn.webthitracnghiem.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.containsString;

class GeminiQuestionGeneratorTest {

    @Test
    void isUnavailableWhenApiKeyIsMissing() {
        GeminiQuestionGenerator generator = generator(new RestTemplate(), "", "gemini-test");

        assertThat(generator.isAvailable()).isFalse();
    }

    @Test
    void parsesJsonQuestionsFromGeminiResponse() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo(containsString("/models/gemini-test:generateContent")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "test-key"))
                .andRespond(withSuccess("""
                        {
                          "candidates": [
                            {
                              "content": {
                                "parts": [
                                  {"text": "```json\\n[{\\\"title\\\":\\\"Câu hỏi mẫu\\\",\\\"optionA\\\":\\\"A\\\",\\\"optionB\\\":\\\"B\\\",\\\"optionC\\\":\\\"C\\\",\\\"optionD\\\":\\\"D\\\",\\\"answer\\\":2,\\\"level\\\":1}]\\n```"}
                                ]
                              }
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        List<GeneratedQuestion> questions = generator(restTemplate, "test-key", "gemini-test")
                .generate(request());

        assertThat(questions).hasSize(1);
        assertThat(questions.get(0).title()).isEqualTo("Câu hỏi mẫu");
        assertThat(questions.get(0).answer()).isEqualTo(2);
        assertThat(questions.get(0).level()).isEqualTo(1);
        server.verify();
    }

    @Test
    void parsesQuestionWithQuestionKeyAndLetterAnswer() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo(containsString("/models/gemini-test:generateContent")))
                .andRespond(withSuccess("""
                        {
                          "candidates": [
                            {
                              "content": {
                                "parts": [
                                  {"text": "[{\\\"question\\\":\\\"Khái niệm lực là gì?\\\",\\\"optionA\\\":\\\"Là đại lượng vecto\\\",\\\"optionB\\\":\\\"Là vô hướng\\\",\\\"optionC\\\":\\\"C\\\",\\\"optionD\\\":\\\"D\\\",\\\"answer\\\":\\\"A\\\",\\\"level\\\":\\\"vận dụng\\\"}]"}
                                ]
                              }
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        List<GeneratedQuestion> questions = generator(restTemplate, "test-key", "gemini-test")
                .generate(request());

        assertThat(questions).hasSize(1);
        assertThat(questions.get(0).title()).isEqualTo("Khái niệm lực là gì?");
        assertThat(questions.get(0).optionA()).isEqualTo("Là đại lượng vecto");
        assertThat(questions.get(0).answer()).isEqualTo(1);
        assertThat(questions.get(0).level()).isEqualTo(3);
        server.verify();
    }

    @Test
    void parsesQuestionsWrappedInObject() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo(containsString("/models/gemini-test:generateContent")))
                .andRespond(withSuccess("""
                        {
                          "candidates": [
                            {
                              "content": {
                                "parts": [
                                  {"text": "{\\\"questions\\\": [{\\\"title\\\":\\\"Câu 1\\\",\\\"optionA\\\":\\\"A\\\",\\\"optionB\\\":\\\"B\\\",\\\"optionC\\\":\\\"C\\\",\\\"optionD\\\":\\\"D\\\",\\\"answer\\\":4,\\\"level\\\":2}]}"}
                                ]
                              }
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        List<GeneratedQuestion> questions = generator(restTemplate, "test-key", "gemini-test")
                .generate(request());

        assertThat(questions).hasSize(1);
        assertThat(questions.get(0).title()).isEqualTo("Câu 1");
        assertThat(questions.get(0).answer()).isEqualTo(4);
        assertThat(questions.get(0).level()).isEqualTo(2);
        server.verify();
    }

    @Test
    void rejectsNonJsonModelOutput() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo(containsString("/models/gemini-test:generateContent")))
                .andRespond(withSuccess("""
                        {"candidates":[{"content":{"parts":[{"text":"not json"}]}}]}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> generator(restTemplate, "test-key", "gemini-test").generate(request()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("định dạng");
    }

    private static GeminiQuestionGenerator generator(RestTemplate restTemplate, String apiKey, String model) {
        return new GeminiQuestionGenerator(
                new ObjectMapper(), restTemplate, apiKey, model,
                "https://generativelanguage.googleapis.com/v1beta");
    }

    private static QuestionGenerationRequest request() {
        return new QuestionGenerationRequest(
                "Toán", 6, "Bài mẫu", "Hiểu khái niệm", "Nội dung bài học", 1, 1, 0, 0);
    }
}
