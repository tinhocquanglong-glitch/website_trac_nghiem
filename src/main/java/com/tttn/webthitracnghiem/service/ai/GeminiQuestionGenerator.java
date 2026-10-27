package com.tttn.webthitracnghiem.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Gemini adapter for the lesson-plan question generation contract. */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class GeminiQuestionGenerator implements QuestionGenerator {
    private static final Logger logger = LoggerFactory.getLogger(GeminiQuestionGenerator.class);
    private static final String DEFAULT_MODEL = "gemini-3.5-flash-lite";
    private static final String DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com/v1beta";

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String model;
    private final String baseUrl;

    @Autowired
    public GeminiQuestionGenerator(
            ObjectMapper objectMapper,
            @Value("${GEMINI_API_KEY:}") String apiKey,
            @Value("${GEMINI_MODEL:" + DEFAULT_MODEL + "}") String model,
            @Value("${GEMINI_API_URL:" + DEFAULT_BASE_URL + "}") String baseUrl,
            @Value("${GEMINI_TIMEOUT_MS:30000}") int timeoutMs) {
        this(objectMapper, createRestTemplate(timeoutMs), apiKey, model, baseUrl);
    }

    GeminiQuestionGenerator(ObjectMapper objectMapper, RestTemplate restTemplate,
                            String apiKey, String model, String baseUrl) {
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null ? "" : model.trim();
        this.baseUrl = normalizeBaseUrl(baseUrl);
    }

    @Override
    public boolean isAvailable() {
        return !apiKey.isBlank() && !model.isBlank() && !baseUrl.isBlank();
    }

    @Override
    public List<GeneratedQuestion> generate(QuestionGenerationRequest request) {
        return generateWithPrompt(buildPrompt(request));
    }

    @Override
    public List<GeneratedQuestion> generateFromUploadedLessonPlan(
            String subjectName, Integer grade, String content, int count, int easy, int medium, int hard) {
        String prompt = buildUploadedLessonPlanPrompt(subjectName, grade, content, count, easy, medium, hard);
        return generateWithPrompt(prompt);
    }

    private List<GeneratedQuestion> generateWithPrompt(String prompt) {
        if (!isAvailable()) {
            throw new IllegalStateException("Gemini chưa được cấu hình API key.");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("contents", List.of(Map.of(
                "parts", List.of(Map.of("text", prompt)))));
        payload.put("generationConfig", Map.of(
                "temperature", 0.7,
                "maxOutputTokens", 4096,
                "responseMimeType", "application/json"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey);

        List<String> modelsToTry = new ArrayList<>();
        if (!model.isBlank()) {
            modelsToTry.add(model);
        }
        for (String candidate : List.of("gemini-3.5-flash-lite", "gemini-3.1-flash-lite", "gemini-3.6-flash", "gemini-3.8-flash", "gemini-3.5-flash")) {
            if (!modelsToTry.contains(candidate)) {
                modelsToTry.add(candidate);
            }
        }

        Exception lastException = null;
        for (String targetModel : modelsToTry) {
            URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .path("/models/{model}:generateContent")
                    .buildAndExpand(targetModel)
                    .toUri();
            ResponseEntity<String> response;
            try {
                logger.info("Calling Gemini API with model '{}'...", targetModel);
                response = restTemplate.postForEntity(
                        uri, new HttpEntity<>(payload, headers), String.class);
            } catch (HttpStatusCodeException ex) {
                lastException = ex;
                logger.warn("Gemini model '{}' returned HTTP {}: {}", targetModel, ex.getRawStatusCode(), ex.getResponseBodyAsString());
                continue;
            } catch (RestClientException ex) {
                lastException = ex;
                logger.warn("Gemini model '{}' request failed: {}", targetModel, ex.getMessage());
                continue;
            }

            try {
                List<GeneratedQuestion> result = parseResponse(response.getBody());
                if (!result.isEmpty()) {
                    logger.info("Successfully generated {} questions using model '{}'", result.size(), targetModel);
                    return result;
                }
            } catch (JsonProcessingException ex) {
                logger.warn("Failed to parse response from Gemini model '{}': {}", targetModel, ex.getMessage());
                throw invalidResponse();
            }
        }

        logger.error("All Gemini models failed. Last exception: {}", lastException != null ? lastException.getMessage() : "unknown");
        throw new IllegalStateException(
                "Gemini không thể sinh câu hỏi lúc này (" + (lastException != null ? lastException.getMessage() : "không có phản hồi")
                        + "). Vui lòng kiểm tra cấu hình hoặc thử lại.", lastException);
    }

    private List<GeneratedQuestion> parseResponse(String responseBody) throws JsonProcessingException {
        if (responseBody == null || responseBody.isBlank()) {
            throw invalidResponse();
        }
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            throw invalidResponse();
        }

        StringBuilder modelText = new StringBuilder();
        for (JsonNode part : candidates.get(0).path("content").path("parts")) {
            if (part.path("thought").asBoolean(false)) {
                continue;
            }
            JsonNode text = part.get("text");
            if (text != null && text.isTextual()) {
                modelText.append(text.asText());
            }
        }
        if (modelText.isEmpty()) {
            for (JsonNode part : candidates.get(0).path("content").path("parts")) {
                JsonNode text = part.get("text");
                if (text != null && text.isTextual()) {
                    modelText.append(text.asText());
                }
            }
        }
        if (modelText.isEmpty()) {
            throw invalidResponse();
        }

        String rawJson = extractJson(modelText.toString());
        JsonNode questionsNode = objectMapper.readTree(rawJson);
        if (questionsNode.isObject()) {
            JsonNode arrayNode = null;
            if (questionsNode.has("questions") && questionsNode.get("questions").isArray()) {
                arrayNode = questionsNode.get("questions");
            } else if (questionsNode.has("data") && questionsNode.get("data").isArray()) {
                arrayNode = questionsNode.get("data");
            } else if (questionsNode.has("items") && questionsNode.get("items").isArray()) {
                arrayNode = questionsNode.get("items");
            } else {
                java.util.Iterator<JsonNode> elements = questionsNode.elements();
                while (elements.hasNext()) {
                    JsonNode el = elements.next();
                    if (el.isArray()) {
                        arrayNode = el;
                        break;
                    }
                }
            }
            if (arrayNode != null) {
                questionsNode = arrayNode;
            }
        }
        if (!questionsNode.isArray()) {
            throw invalidResponse();
        }

        List<GeneratedQuestion> questions = new ArrayList<>();
        for (JsonNode question : questionsNode) {
            if (!question.isObject()) {
                continue;
            }
            String title = text(question, "title", "question", "content", "cauHoi", "cau_hoi", "prompt");
            String optA = text(question, "optionA", "option_a", "A", "a", "ansA");
            String optB = text(question, "optionB", "option_b", "B", "b", "ansB");
            String optC = text(question, "optionC", "option_c", "C", "c", "ansC");
            String optD = text(question, "optionD", "option_d", "D", "d", "ansD");
            if (optD.isBlank() && !optA.isBlank() && !optB.isBlank() && !optC.isBlank()) {
                optD = "Cả 3 phương án trên đều sai";
            }
            int ans = parseAnswer(question, optA, optB, optC, optD);
            int lvl = parseLevel(question);
            questions.add(new GeneratedQuestion(title, optA, optB, optC, optD, ans, lvl));
        }
        return questions;
    }

    private static String buildPrompt(QuestionGenerationRequest request) {
        return """
                Bạn là Chuyên gia Khảo thí và Đo lường Giáo dục hàng đầu tại Việt Nam, am hiểu sâu sắc Chương trình GDPT mới.
                Nhiệm vụ của bạn: Hãy nghiên cứu kỹ lý thuyết, bản chất và mục tiêu kiến thức của bài học dưới đây, sau đó SÁNG TẠO ra đúng %d câu hỏi trắc nghiệm 4 lựa chọn (A, B, C, D) chất lượng cao.

                QUY TẮC SƯ PHẠM VÀ TƯ DUY SÁNG TẠO (BẮT BUỘC TUÂN THỦ):
                1. ĐỌC HIỂU SÂU BẢN CHẤT LÝ THUYẾT (KHÔNG CHÉP 1-1):
                   - KHÔNG ĐƯỢC sao chép nguyên văn câu chữ trong giáo trình / tài liệu tham khảo.
                   - TUYỆT ĐỐI KHÔNG làm câu hỏi đục lỗ từ ngữ cơ học, không hỏi vẹt kiểu: "Theo tài liệu...", "Câu nào sau đây được nhắc đến trong bài...".
                   - Phải thấu hiểu bản chất định nghĩa, định lý, công thức, quy luật, mối quan hệ nhân - quả, ý nghĩa thực tế để tự sáng tạo câu hỏi bằng văn phong mới mẻ, tự nhiên.

                2. SÁNG TẠO TÌNH HUỐNG THỰC TẾ & BÀI TOÁN TƯ DUY:
                   - Thiết kế các câu hỏi gắn liền với ngữ cảnh thực tế đời sống, hiện tượng khoa học, bài toán ứng dụng hoặc tình huống mới lạ.
                   - Học sinh phải hiểu sâu kiến thức mới giải quyết được, không thể chỉ nhìn mặt chữ trong tài liệu để chọn đáp án.

                3. PHÂN BỐ VÀ MỨC ĐỘ NHẬN THỨC:
                   - Tổng số câu hỏi: %d câu.
                   - Mức 1 (Nhận biết - %d câu): Kiểm tra sự nắm vững bản chất khái niệm/quy luật thông qua cách diễn đạt mới, mô tả bản chất chứ không học vẹt.
                   - Mức 2 (Thông hiểu - %d câu): Yêu cầu giải thích nguyên nhân, so sánh, phân biệt các trường hợp dễ nhầm lẫn, suy luận logic từ kiến thức đã học.
                   - Mức 3 (Vận dụng / Vận dụng cao - %d câu): Đưa ra tình huống thực tế, bài toán thực tiễn, số liệu hoặc bối cảnh mới đòi hỏi người học vận dụng kiến thức bài học để giải quyết vấn đề.

                4. PHƯƠNG ÁN TRẢ LỜI & ĐÁP ÁN:
                   - Mỗi câu có đúng 4 phương án: optionA, optionB, optionC, optionD.
                   - 3 phương án gây nhiễu (distractors) phải logic, khoa học, đánh trúng các ngộ nhận và lỗi sai tư duy phổ biến của học sinh, có độ dài và văn phong tương đương phương án đúng.
                   - answer: là số nguyên 1, 2, 3 hoặc 4 (tương ứng optionA = 1, optionB = 2, optionC = 3, optionD = 4).
                   - Đảo ngẫu nhiên vị trí đáp án đúng giữa các câu hỏi.

                5. ĐỊNH DẠNG ĐẦU RA (JSON BẮT BUỘC):
                   - Chỉ trả về duy nhất một mảng JSON (JSON array), không có markdown text giải thích ngoài.
                   - Cấu trúc từng phần tử:
                     {
                       "title": "Nội dung câu hỏi sáng tạo, rõ ràng, mang tính tư duy sư phạm",
                       "optionA": "Nội dung phương án A",
                       "optionB": "Nội dung phương án B",
                       "optionC": "Nội dung phương án C",
                       "optionD": "Nội dung phương án D",
                       "answer": 1,
                       "level": 1
                     }

                THÔNG TIN BÀI HỌC CẦN NGHIÊN CỨU:
                - Môn: %s
                - Khối lớp: %s
                - Tên bài học: %s
                - Mục tiêu cần đạt: %s
                <lesson-content>
                %s
                </lesson-content>
                """.formatted(
                request.count(),
                request.count(), request.easy(), request.medium(), request.hard(),
                safe(request.subjectName()),
                request.grade() == null ? "Không xác định" : request.grade().toString(),
                safe(request.lessonName()), safe(request.objectives()), safe(request.content()));
    }

    private static String buildUploadedLessonPlanPrompt(
            String subjectName, Integer grade, String rawPlanContent, int count, int easy, int medium, int hard) {
        String safeContent = rawPlanContent == null ? "" : rawPlanContent.trim();
        if (safeContent.length() > 35000) {
            safeContent = safeContent.substring(0, 35000) + "\n...[Tài liệu tiếp tục nhưng đã cắt bớt để vừa giới hạn]";
        }

        return """
                Bạn là Chuyên gia Khảo thí và Đo lường Giáo dục hàng đầu tại Việt Nam, am hiểu sâu sắc Chương trình Giáo dục Phổ thông mới.
                Người dùng (giáo viên) đã tải lên một tài liệu KẾ HOẠCH BÀI DẠY (KHBD / Giáo án) thuộc Môn học: %s (Khối lớp: %s).

                NHIỆM VỤ CỦA BẠN:
                Đọc hiểu toàn bộ tài liệu giáo án được cung cấp dưới đây, sau đó SÁNG TẠO ra đúng %d câu hỏi trắc nghiệm 4 lựa chọn (A, B, C, D) chất lượng cao để kiểm tra mức độ tiếp thu KIẾN THỨC BÀI HỌC của học sinh.

                ****************************************************************************************
                QUY TẮC PHÂN LOẠI VÀ CHỌN LỌC NỘI DUNG SƯ PHẠM (BẮT BUỘC TUÂN THỦ 100%%):
                Một bản KHBD luôn có 2 nhóm nội dung. Bạn phải phân biệt và xử lý như sau:

                NHÓM 1: CÁC NỘI DUNG NGHIỆP VỤ SƯ PHẠM CỦA GIÁO VIÊN (TUYỆT ĐỐI KHÔNG ĐƯỢC LẤY ĐỂ ĐẶT CÂU HỎI):
                - Tuyệt đối KHÔNG hỏi về Mục tiêu bài học (Ví dụ: "Mục tiêu của bài này là gì?", "Học sinh cần phát triển năng lực gì?", "Học sinh phải hiểu được...", "Yêu cầu cần đạt là gì?").
                - Tuyệt đối KHÔNG hỏi về Phẩm chất và Năng lực chung/đặc thù (yêu nước, nhân ái, chăm chỉ, trung thực, tự chủ, giao tiếp...).
                - Tuyệt đối KHÔNG hỏi về Tiến trình bài dạy và các bước tổ chức hoạt động của giáo viên và học sinh (Ví dụ: "Hoạt động 1 có tên là gì?", "Giáo viên yêu cầu học sinh làm gì?", "Học sinh chuyển giao nhiệm vụ như thế nào?", "Các bước thực hiện hoạt động khởi động...").
                - Tuyệt đối KHÔNG hỏi về Thiết bị dạy học, đồ dùng dạy học, phiếu học tập, bảng kiểm, tiêu chí đánh giá (Ví dụ: "Phiếu học tập số 1 yêu cầu gì?", "Giáo viên sử dụng đồ dùng nào?").
                => NHỮNG CÂU HỎI VỀ NHÓM 1 LÀ HOÀN TOÀN SAI VỀ MẶT KHẢO THÍ HỌC SINH! BẠN TUYỆT ĐỐI KHÔNG ĐƯỢC TẠO RA BẤT KỲ CÂU NÀO DẠNG NÀY.

                NHÓM 2: KIẾN THỨC CỐT LÕI CỦA BÀI HỌC DÀNH CHO HỌC SINH (CHỈ ĐƯỢC PHÉP LẤY NHÓM NÀY ĐỂ ĐẶT CÂU HỎI):
                - Chỉ trích xuất phần KIẾN THỨC MÔN HỌC THỰC TẾ xuất hiện trong bài giảng (thường nằm ở phần nội dung bài học, kết luận kiến thức, phần kiến thức mới, bảng dữ liệu kiến thức):
                  + Định nghĩa, khái niệm khoa học, thuật ngữ chuyên môn.
                  + Định lý, tính chất, quy tắc, công thức toán học/khoa học.
                  + Cú pháp câu lệnh, cấu trúc giải thuật, dữ liệu (nếu là môn Tin học).
                  + Hiện tượng tự nhiên, phản ứng hóa học, cơ chế sinh học, định luật vật lý.
                  + Sự kiện, nguyên nhân - kết quả, nhân vật lịch sử, địa lý kinh tế - xã hội.
                  + Ý nghĩa tác phẩm, hình tượng, nghệ thuật, từ vựng, ngữ pháp...
                - Từ kiến thức cốt lõi đó, hãy đặt câu hỏi trắc nghiệm kiểm tra độ hiểu bài, khả năng phân tích, so sánh và vận dụng kiến thức của học sinh.
                ****************************************************************************************

                YÊU CẦU ĐỘ KHÓ:
                - Tổng số câu: %d câu.
                - Mức 1 (Nhận biết - %d câu): Kiểm tra sự ghi nhớ và nhận diện khái niệm, định nghĩa, công thức cốt lõi của bài.
                - Mức 2 (Thông hiểu - %d câu): Yêu cầu giải thích nguyên nhân, so sánh, phân biệt các trường hợp, suy luận logic từ kiến thức đã học.
                - Mức 3 (Vận dụng - %d câu): Đưa ra tình huống thực tế, bài toán thực tiễn, số liệu hoặc ngữ cảnh mới đòi hỏi học sinh áp dụng kiến thức để giải quyết.

                YÊU CẦU PHƯƠNG ÁN TRẢ LỜI & ĐÁP ÁN:
                - Mỗi câu hỏi gồm 4 phương án: optionA, optionB, optionC, optionD.
                - 3 phương án gây nhiễu phải hợp lý, khoa học, đánh trúng vào các ngộ nhận sai lầm thường gặp của học sinh, có độ dài và văn phong tương đương phương án đúng.
                - answer: số nguyên 1, 2, 3 hoặc 4 (tương ứng A=1, B=2, C=3, D=4).
                - Phân bổ ngẫu nhiên đáp án đúng giữa các phương án.

                ĐỊNH DẠNG ĐẦU RA (JSON BẮT BUỘC):
                Chỉ trả về DUY NHẤT một JSON array thuần túy, không có giải thích hay markdown bên ngoài:
                [
                  {
                    "title": "Nội dung câu hỏi trắc nghiệm kiểm tra kiến thức học sinh",
                    "optionA": "Nội dung phương án A",
                    "optionB": "Nội dung phương án B",
                    "optionC": "Nội dung phương án C",
                    "optionD": "Nội dung phương án D",
                    "answer": 1,
                    "level": 1
                  }
                ]

                NỘI DUNG TÀI LIỆU KẾ HOẠCH BÀI DẠY CỦA GIÁO VIÊN:
                <tai-lieu-khbd>
                %s
                </tai-lieu-khbd>
                """.formatted(
                safe(subjectName),
                grade == null || grade <= 0 ? "Chung" : "Lớp " + grade,
                count,
                count, easy, medium, hard,
                safeContent);
    }

    private static String extractJson(String text) {
        String trimmed = text.trim();
        if (trimmed.contains("```")) {
            int firstFence = trimmed.indexOf("```");
            int endOfFirstLine = trimmed.indexOf('\n', firstFence);
            int lastFence = trimmed.lastIndexOf("```");
            if (endOfFirstLine >= 0 && lastFence > endOfFirstLine) {
                trimmed = trimmed.substring(endOfFirstLine + 1, lastFence).trim();
            }
        }
        int firstBracket = trimmed.indexOf('[');
        int lastBracket = trimmed.lastIndexOf(']');
        if (firstBracket >= 0 && lastBracket > firstBracket) {
            return trimmed.substring(firstBracket, lastBracket + 1).trim();
        }
        int firstBrace = trimmed.indexOf('{');
        int lastBrace = trimmed.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1).trim();
        }
        return trimmed;
    }

    private static String text(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && !value.isNull() && !value.asText("").isBlank()) {
                return value.asText("").trim();
            }
        }
        return "";
    }

    private static int parseAnswer(JsonNode question, String optA, String optB, String optC, String optD) {
        String[] answerFields = {"answer", "ans", "correct", "correctAnswer", "correct_answer", "dapAn", "dap_an"};
        for (String field : answerFields) {
            JsonNode value = question.get(field);
            if (value != null && !value.isNull()) {
                if (value.isNumber()) {
                    int num = value.asInt(0);
                    if (num >= 1 && num <= 4) return num;
                }
                String s = value.asText("").trim();
                String upper = s.toUpperCase(Locale.ROOT);
                if (upper.equals("A") || upper.equals("1") || upper.equals("OPTIONA") || upper.equals("OPTION_A")) return 1;
                if (upper.equals("B") || upper.equals("2") || upper.equals("OPTIONB") || upper.equals("OPTION_B")) return 2;
                if (upper.equals("C") || upper.equals("3") || upper.equals("OPTIONC") || upper.equals("OPTION_C")) return 3;
                if (upper.equals("D") || upper.equals("4") || upper.equals("OPTIOND") || upper.equals("OPTION_D")) return 4;

                if (!optA.isBlank() && (s.equalsIgnoreCase(optA) || s.startsWith(optA))) return 1;
                if (!optB.isBlank() && (s.equalsIgnoreCase(optB) || s.startsWith(optB))) return 2;
                if (!optC.isBlank() && (s.equalsIgnoreCase(optC) || s.startsWith(optC))) return 3;
                if (!optD.isBlank() && (s.equalsIgnoreCase(optD) || s.startsWith(optD))) return 4;
            }
        }
        return 1;
    }

    private static int parseLevel(JsonNode question) {
        String[] levelFields = {"level", "difficulty", "mucDo", "muc_do", "grade_level"};
        for (String field : levelFields) {
            JsonNode value = question.get(field);
            if (value != null && !value.isNull()) {
                if (value.isNumber()) {
                    int num = value.asInt(1);
                    if (num >= 1 && num <= 3) return num;
                }
                String lower = value.asText("").trim().toLowerCase(Locale.ROOT);
                if (lower.contains("nhận biết") || lower.contains("nhan biet") || lower.contains("easy") || lower.equals("1")) return 1;
                if (lower.contains("thông hiểu") || lower.contains("thong hieu") || lower.contains("medium") || lower.equals("2")) return 2;
                if (lower.contains("vận dụng") || lower.contains("van dung") || lower.contains("hard") || lower.equals("3")) return 3;
            }
        }
        return 1;
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "Không có" : value;
    }

    private static IllegalStateException invalidResponse() {
        return new IllegalStateException("Gemini trả về dữ liệu không đúng định dạng câu hỏi.");
    }

    private static RestTemplate createRestTemplate(int timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int timeout = Math.max(timeoutMs, 1000);
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        return new RestTemplate(factory);
    }

    private static String normalizeBaseUrl(String value) {
        if (value == null) return "";
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
