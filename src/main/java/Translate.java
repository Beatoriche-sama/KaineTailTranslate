import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Translate {
    private String key;
    private Service service;
    private Language language;

    public enum Service {
        DEEPL_SOUP_FREE("DeepLSoupFree"),
        DEEPL_JSON_FREE("DeepLJsonFree"),
        DEEPL_KEY("DeeplKey"),
        GOOGLE("Google"),
        YANDEX("Yandex");

        private final String serviceCode;

        Service(String serviceCode) {
            this.serviceCode = serviceCode;
        }

        public String getServiceCode() {
            return serviceCode;
        }
    }

    public enum Language {
        EN("EN"),
        JA("JA"),
        RU("RU");

        private final String code;

        Language(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    public ArrayList<String> translate(String textToTranslate) throws IOException,
            JSONException, URISyntaxException, ExecutionException, InterruptedException {
        ArrayList<String> translatedArrayList;
        if (service == Service.DEEPL_JSON_FREE) {
            translatedArrayList = parseJSONResponse(runScript(textToTranslate));
        } else {
            String response = null;
            try {
                response = runScript(textToTranslate);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
            translatedArrayList = new ArrayList<>();
            translatedArrayList.add(response);
        }
        return translatedArrayList;
    }

    private String runScript(String sourceText) throws IOException, ExecutionException, InterruptedException {
        InputStream script = this.getClass().getResourceAsStream("TranslatorAPI.py");
        Process pythonProcess = new ProcessBuilder("python", "-",
                sourceText, language.code, service.serviceCode)
                .start();
        return getResult(pythonProcess, script);
    }

    private String getResult(Process process, InputStream inputStream) throws IOException, InterruptedException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        StringBuilder finalResult = new StringBuilder();
        Runnable readInputStream = () -> {
            synchronized (this) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), "windows-1251"))) {
                    for (String line; (line = reader.readLine()) != null; ) {
                        finalResult.append(line).append("\n");
                    }

                } catch (IOException exc) {
                    exc.printStackTrace();
                }
                this.notify();
            }
        };

        OutputStream stdin = process.getOutputStream();
        byte[] buffer = new byte[1024];
        for (int read = 0; read >= 0; read = Objects.requireNonNull(inputStream).read(buffer)) {
            stdin.write(buffer, 0, read);
        }

        executor.submit(readInputStream);
        executor.shutdown();
        stdin.close();
        synchronized (this) {
            this.wait();
        }
        return finalResult.toString();
    }

    private ArrayList<String> parseJSONResponse(String jsonString) throws JSONException {
        JSONObject obj = new JSONObject(jsonString);
        JSONArray translation = obj.getJSONObject("result").getJSONArray("translations");
        JSONArray beams = translation.getJSONObject(0).getJSONArray("beams");
        ArrayList<String> translatedStrings = new ArrayList<>();

        for (int i = 0; i < beams.length(); i++) {
            JSONObject postProcess = beams.getJSONObject(i);
            translatedStrings.add(postProcess.getString("postprocessed_sentence"));
        }
        return translatedStrings;
    }

    public String translateDeeplKey(String text) throws IOException {
        String jsonInputString = "data = {\n" +
                "                'target_lang' : '" + language.code + "',  \n" +
                "                            'auth_key' : '" + key + "',\n" +
                "                'text': '" + text + "'\n" +
                "                }";

        final Content postResult = Request.Post("https://api.deepl.com/v2/translate")
                .connectTimeout(10000)
                .bodyString(jsonInputString, ContentType.APPLICATION_JSON)
                .execute().returnContent();
        return postResult.asString();
    }

    public Language getLanguage() {
        return language;
    }

    public Service getService() {
        return service;
    }

    public String getKey() {
        return key;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
