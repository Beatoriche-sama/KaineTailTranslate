import java.io.*;
import java.util.Arrays;
import java.util.Properties;

public class PropertiesReader {
    private final Properties prop;
    private final String propFileName;

    public PropertiesReader() {
        prop = new Properties();
        propFileName = "config.properties";
    }

    private String getPropertyValue(String property) throws IOException {
        InputStream inputStream = new FileInputStream(propFileName);
        prop.load(inputStream);
        String result = prop.getProperty(property);
        inputStream.close();
        return result;
    }

    public void readAndInit(Translate translateInstance, TextRecognition ocrInstance) {
        Translate.Language[] languages = new Translate.Language[]
                {Translate.Language.EN, Translate.Language.JA, Translate.Language.RU};
        Translate.Service[] services = new Translate.Service[]
                {Translate.Service.GOOGLE, Translate.Service.YANDEX, Translate.Service.DEEPL_KEY,
                        Translate.Service.DEEPL_JSON_FREE, Translate.Service.DEEPL_SOUP_FREE};

        boolean isOcrImprove;
        Translate.Language lang;
        Translate.Service service;

        try {
            String key = getPropertyValue("key");
            translateInstance.setKey(key);

            isOcrImprove = Boolean.parseBoolean(getPropertyValue("isOcrImprove"));
            ocrInstance.setImproveNeeded(isOcrImprove);

            String langCode = getPropertyValue("lang");
            lang = Arrays.stream(languages)
                    .filter(l -> l.getCode().equals(langCode))
                    .findAny().orElse(Translate.Language.RU);
            translateInstance.setLanguage(lang);

            String serviceCode = getPropertyValue("service");
            service = Arrays.stream(services)
                    .filter(s -> s.getServiceCode().equals(serviceCode))
                    .findAny().orElse(Translate.Service.GOOGLE);
            translateInstance.setService(service);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveConfigs(Translate translateInstance, TextRecognition ocrInstance) throws IOException {
        InputStream in = new FileInputStream(propFileName);
        prop.load(in);
        in.close();

        OutputStream out = new FileOutputStream(propFileName);
        prop.setProperty("key", translateInstance.getKey());
        prop.setProperty("lang",
                String.valueOf(translateInstance.getLanguage()));
        prop.setProperty("service",
                String.valueOf(translateInstance.getService()));
        prop.setProperty("isOcrImprove",
                String.valueOf(ocrInstance.isImproveNeeded));
        prop.store(out, null);
        out.close();
    }
}
