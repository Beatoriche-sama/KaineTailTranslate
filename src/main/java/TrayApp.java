import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class TrayApp {
    private Translate translateInstance;
    private TextRecognition ocrInstance;
    private AreaScreenshot areaScreenshot;
    private FrameTransparent frameTransparent;
    private PropertiesReader propertiesReader;
    private HashMap<CheckboxMenuItem, Translate.Language> languageMap;
    private HashMap<CheckboxMenuItem, Translate.Service> serviceMap;

    public static final Color alphaTransparent = new Color(0, 0, 0, 1);
    public static final Color trueTransparent = new Color(0, 0, 0, 0);

    public static void main(String[] args) throws AWTException {
        new TrayApp();
    }

    public TrayApp() throws AWTException {
        if (!SystemTray.isSupported()) {
            return;
        }
        languageMap = new HashMap<>();
        serviceMap = new HashMap<>();

        ocrInstance = new TextRecognition();
        translateInstance = new Translate();
        areaScreenshot = new AreaScreenshot();
        propertiesReader = new PropertiesReader();

        propertiesReader.readAndInit(translateInstance, ocrInstance);

        PopupMenu trayMenu = new PopupMenu();

        MenuItem selectItem = new MenuItem("Select area");
        MenuItem manualInput = new MenuItem("Manual input");
        MenuItem keyInput = new MenuItem("Key input");
        Menu serviceSubMenu = new Menu("Translate service");
        Menu languageItem = new Menu("Target language");
        Menu ocrSettings = new Menu("OCR settings");
        MenuItem exitItem = new MenuItem("Exit");

        frameTransparent = new FrameTransparent();
        SelectAreaHandler selectAreaHandler = new SelectAreaHandler(frameTransparent);

        trayMenu.add(selectItem);
        trayMenu.add(manualInput);
        trayMenu.add(keyInput);
        trayMenu.add(serviceSubMenu);
        trayMenu.add(languageItem);
        trayMenu.add(ocrSettings);
        trayMenu.add(exitItem);

        manualInput.addActionListener(e -> new TranslateWindow(frameTransparent)
                .addInputFunction(translateInstance));

        CheckboxMenuItem improveOCR = new CheckboxMenuItem("Improve OCR");
        ocrSettings.add(improveOCR);
        improveOCR.setState(ocrInstance.isImproveNeeded);

        improveOCR.addItemListener(eee -> {
            boolean isImprove = ocrInstance.isImproveNeeded;
            ocrInstance.setImproveNeeded(!isImprove);
            improveOCR.setState(!isImprove);
        });

        CheckboxMenuItem googleSubMenuFree = new CheckboxMenuItem("Google (free)");
        CheckboxMenuItem yandexSubMenuFree = new CheckboxMenuItem("Yandex (free)");
        CheckboxMenuItem deeplHtmlSubMenuFree = new CheckboxMenuItem("DeepL html parse (free)");
        CheckboxMenuItem deeplJsonSubMenu = new CheckboxMenuItem("DeepL json (free)");
        //CheckboxMenuItem deeplSubMenu = new CheckboxMenuItem("DeepL (need key)");

        serviceMap.put(googleSubMenuFree, Translate.Service.GOOGLE);
        serviceMap.put(yandexSubMenuFree, Translate.Service.YANDEX);
        serviceMap.put(deeplHtmlSubMenuFree, Translate.Service.DEEPL_SOUP_FREE);
        serviceMap.put(deeplJsonSubMenu, Translate.Service.DEEPL_JSON_FREE);
        //serviceMap.put(deeplSubMenu, Translate.Service.DEEPL_KEY);

        serviceMap.keySet().forEach(serviceSubMenu::add);

        AtomicReference<CheckboxMenuItem> lastServiceItem =
                new AtomicReference<>(setCheckboxState(serviceMap, translateInstance.getService()));

        ItemListener serviceListener = e -> {
            CheckboxMenuItem menuItem = (CheckboxMenuItem) e.getSource();
            translateInstance.setService(serviceMap.get(menuItem));
            lastServiceItem.get().setState(false);
            lastServiceItem.set(menuItem);
        };

        serviceMap.keySet().forEach(serviceItem ->
                serviceItem.addItemListener(serviceListener));

        keyInput.addActionListener(e -> {
            String key = JOptionPane.showInputDialog(frameTransparent,
                    "Enter your key",
                    "Key input (￣ω￣)",
                    JOptionPane.INFORMATION_MESSAGE);
            JOptionPane.showMessageDialog(frameTransparent,
                    "Your key is: " + key);
            translateInstance.setKey(key);
        });

        CheckboxMenuItem englishItem = new CheckboxMenuItem("English");
        CheckboxMenuItem russianItem = new CheckboxMenuItem("Russian");
        CheckboxMenuItem japaneseItem = new CheckboxMenuItem("Japanese");

        languageMap.put(englishItem, Translate.Language.EN);
        languageMap.put(russianItem, Translate.Language.RU);
        languageMap.put(japaneseItem, Translate.Language.JA);

        AtomicReference<CheckboxMenuItem> lastLangItem =
                new AtomicReference<>(setCheckboxState(languageMap, translateInstance.getLanguage()));

        ItemListener languageListener = e -> {
            CheckboxMenuItem item = (CheckboxMenuItem) e.getSource();
            translateInstance.setLanguage(languageMap.get(item));
            lastLangItem.get().setState(false);
            lastLangItem.set(item);
        };

        languageMap.keySet().forEach(langItem -> {
            langItem.addItemListener(languageListener);
            languageItem.add(langItem);
        });

        frameTransparent.addMouseListener(selectAreaHandler);
        frameTransparent.addMouseMotionListener(selectAreaHandler);
        frameTransparent.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);
                Point pointSelected = selectAreaHandler.getPointSelected();
                Dimension dimensionSelected = selectAreaHandler.getDimensionSelected();
                if (dimensionSelected == null) return;

                TranslateWindow translatedPanel = new TranslateWindow(frameTransparent);

                ExecutorService executorService = Executors.newSingleThreadExecutor();
                Runnable runnable = () -> {
                    BufferedImage bufferedImage;
                    bufferedImage = areaScreenshot.screenshot(pointSelected, dimensionSelected);
                    String ocrResult = translatedPanel.showOcrResult(bufferedImage, ocrInstance);
                    translatedPanel.showTranslateResult(translateInstance, ocrResult);
                };
                executorService.submit(runnable);
                executorService.shutdown();
            }
        });

        selectItem.addActionListener(e -> frameTransparent.setBackground(alphaTransparent));
        exitItem.addActionListener(e -> {
            try {
                propertiesReader.saveConfigs(translateInstance, ocrInstance);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            System.exit(0);
        });

        URL imageURL = TrayApp.class.getResource("trayIcon.jpg");

        Image icon = Toolkit.getDefaultToolkit().getImage(imageURL);

        TrayIcon trayIcon = new TrayIcon(icon, "Kaine's Tail", trayMenu);
        trayIcon.setImageAutoSize(true);

        SystemTray tray = SystemTray.getSystemTray();
        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            e.printStackTrace();
        }

        trayIcon.displayMessage("Kaine's Tail", "Kaine's Tail is ready to translate",
                TrayIcon.MessageType.INFO);
    }

    private CheckboxMenuItem setCheckboxState(HashMap<CheckboxMenuItem, ? extends Enum> map,
                                              Enum currentEnum) {
        CheckboxMenuItem item = map.entrySet().stream()
                .filter(entrySet -> entrySet.getValue() == currentEnum)
                .map(Map.Entry::getKey)
                .findFirst().get();
        item.setState(true);
        return item;
    }
}
