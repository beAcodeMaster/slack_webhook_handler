import java.net.URI; // URL 같은 개념인데, 웹 요청을 보낼 때 필요한 주소를 담음
import java.net.http.HttpClient; // HTTP 요청을 보낼 클라이언트 (우리가 직접 웹 요청을 보낼 때 필요)
import java.net.http.HttpRequest; // HTTP 요청을 만들기 위한 객체
import java.net.http.HttpResponse; // HTTP 응답을 받을 때 사용하는 객체
import java.util.Random;

public class Webhook {
    public static void main(String[] args) {

        String prompt = choosePrompt();
        String response = useLLM(prompt);
        String example = prompt.split("예문")[0];
        String imgUrl = makeImgUrl(example);
        System.out.println("Generated Response: " + response);
        System.out.println("Generated image: " + imgUrl);

        sendSlackMessage(response, imgUrl);
    }

    //  Together AI API 호출하여 영어 단어 가져오기
    public static String useLLM(String prompt) {
        // 여기에 Slack 웹훅 URL을 넣으면, 우리가 메시지를 Slack으로 보낼 수 있음!
        String apiUrl = System.getenv("API_URL");
        String apiKey = System.getenv("API_KEY");
        String apiModel = "llama-3.3-70b-versatile";
// API 정보가 없으면 오류 출력
        if (apiUrl == null || apiKey == null || apiUrl.isEmpty() || apiKey.isEmpty()) {
            throw new IllegalArgumentException("API_URL 또는 API_KEY가 설정되지 않았습니다.");
        }

        // JSON 요청 본문 생성
        String payload = """
                {
                  "messages": [
                    {
                      "role": "user",
                      "content": "%s"
                    }
                  ],
                  "model": "%s",
                  "temperature": 1
                }
                """.formatted(prompt, apiModel);

        // HTTP 클라이언트 생성
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl)) // 요청 URL 설정
                .header("Content-Type", "application/json") // JSON 요청 설정
                .header("Authorization", "Bearer " + apiKey) // 인증 추가
                .POST(HttpRequest.BodyPublishers.ofString(payload)) // POST 요청과 본문 추가
                .build();
        String result = null;
        try { // try
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
            System.out.println("response.statusCode() = " + response.statusCode());
            String responseBody = response.body();
            String parts = responseBody.split("\"content\":")[1].split("}")[0];
            parts = parts.replace("\\n", "\n");
            parts = parts.replaceAll("\"", "");
            result = parts.replace("\\", "\n");
        } catch (Exception e) { // catch exception e
            throw new RuntimeException(e);
        }
        return result; // 메서드(함수)가 모두 처리되고 나서 이 값을 결과값으로 가져서 이걸 대입하거나 사용할 수 있다
    }


    public static void sendSlackMessage(String text, String img) {
        // 여기에 Slack 웹훅 URL을 넣으면, 우리가 메시지를 Slack으로 보낼 수 있음!
        String slackUrl = System.getenv("SLACK_WEBHOOK_URL");

        // 우리가 Slack에 보낼 메시지 내용을 JSON 형식으로 작성!
        // `text` 속성 안에 있는 문자열이 실제로 Slack에 표시될 내용이야.
        // \n을 넣으면 줄바꿈이 가능해!
//        String payload = "{\"text\": \"채널에 있는 한 줄의 텍스트입니다.\\n또 다른 한 줄의 텍스트입니다.\"}";
        String payload = """
                {"attachments":[{
                "text":"%s",
                "image_url":"%s"
                }]}
                """.formatted(text, img);

        // HTTP 요청을 보낼 클라이언트를 생성! (웹사이트에 요청을 보내는 브라우저 같은 역할)
        HttpClient client = HttpClient.newHttpClient();

        // 우리가 보낼 HTTP 요청을 생성하는 부분
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(slackUrl)) // 우리가 요청을 보낼 대상 (Slack 웹훅 URL)
                .header("Content-Type", "application/json") // 우리가 JSON 형식으로 데이터를 보낼 거라고 알려줌
                .POST(HttpRequest.BodyPublishers.ofString(payload)) //
                .build(); // 이제 요청을 완성!

        // 이제 요청을 실제로 보내고, 응답을 받을 차례!
        try {
            HttpResponse<String> response = client.send(
                    request, // 우리가 만든 요청을 보냄
                    HttpResponse.BodyHandlers.ofString() // 응답을 문자열 형태로 받음
            );

            // 응답 코드 출력 (200이면 성공! 400이나 500대 숫자가 나오면 뭔가 잘못된 거임)
            System.out.println("response.statusCode() = " + response.statusCode());

            // 응답 본문 출력 (Slack이 "ok"라고 보내주면 성공적으로 전송된 거!)
            System.out.println("response.body() = " + response.body());

        } catch (Exception e) {
            // 만약 요청을 보내는 도중에 에러가 나면 여기서 캐치됨
            throw new RuntimeException(e); // 에러를 콘솔에 출력하고 프로그램을 멈춤
        }
    }


    public static String choosePrompt() {
        String[] prompts = {"코딩", "AI", "취업", "직무", "회사"};
        String chosenPrompt = prompts[new Random().nextInt(prompts.length)];
        String baseString = "%s과 관련된 영단어를 '%s 영어: 영어 단어-한글뜻, 영어 예문' 형태로 하나만 출력";
        return String.format(baseString, chosenPrompt, chosenPrompt);
    }

    public static String makeImgUrl(String prompt) {
        String apiUrl = System.getenv("API_URL2");
        String apiKey = System.getenv("API_KEY2");
        String apiModel = "black-forest-labs/FLUX.1-schnell-Free";
        String promptNew = String.format("%s에 있는 영단어를 표현할 수 있는 아이콘응 한국웹툰과 비슷한 느낌으로 출력해줘.", prompt);
        String payload = """
                {
                    "model": "%s",
                    "prompt": "%s",
                    "width": 1024,
                    "height": 768,
                    "steps": 1,
                    "n": 1
                    }
                """.formatted(apiModel, promptNew);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey) // API Key 추가
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Response Code: " + response.statusCode());

            if (response.statusCode() == 200) {
                String answer = response.body().split("\"url\": \"")[1].split("\"")[0];
                System.out.println("Extracted URL: " + answer);
                return answer;
            } else {
                System.out.println("Error: " + response.body());
                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);

        }


    }

}
