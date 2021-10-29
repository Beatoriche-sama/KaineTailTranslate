import net.miginfocom.swing.MigLayout;
import net.sourceforge.tess4j.TesseractException;
import org.json.JSONException;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class TranslateWindow extends JDialog {
    private final JPanel panelText;
    private final JTextArea translatedTextArea, originalTextArea;
    private final JLabel statusInfoLabel;
    private JButton previousResultButton, nextResultButton, translateButton;

    private boolean isOriginalTextVisible;
    private ArrayList<String> translateResult;

    public TranslateWindow(Container container) {
        setTitle("Translate window (´｡• ω •｡`)");
        try {
            setIconImage(ImageIO.read(Objects.requireNonNull(TranslateWindow.class
                    .getResource("trayIcon.jpg"))));
        } catch (IOException e) {
            e.printStackTrace();
        }
        setLocationRelativeTo(container);
        setAlwaysOnTop(true);

        JScrollPane scrollPane = new JScrollPane();
        setContentPane(scrollPane);

        isOriginalTextVisible = true;
        BufferedImage image = null;
        try {
            image = ImageIO.read(Objects.requireNonNull(TranslateWindow.
                    class.getResource("background.jpg")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        BufferedImage finalImage = image;
        panelText = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(finalImage, 0, 0, getWidth(), getHeight(), this);
            }
        };

        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setViewportView(panelText);
        Font font = new Font("TimesRoman", Font.BOLD, 17);

        statusInfoLabel = new JLabel("Current status");
        statusInfoLabel.setForeground(Color.white);
        translatedTextArea = createTextArea(font);
        originalTextArea = createTextArea(font);
        JLabel originalLabel = new JLabel("Original:");
        originalLabel.setForeground(Color.white);
        JLabel translateLabel = new JLabel("Translate:");
        translateLabel.setForeground(Color.white);
        JButton recognizedTextButton = new JButton("Show/hide original");

        panelText.setLayout(new MigLayout());
        panelText.add(recognizedTextButton);
        createNextPrevButtons(panelText, translatedTextArea);
        panelText.add(statusInfoLabel, "wrap");
        panelText.add(translateLabel);
        panelText.add(originalLabel, "wrap");

        panelText.add(translatedTextArea);
        panelText.add(originalTextArea, "hidemode 3, wrap");

        recognizedTextButton.addActionListener(ee2 -> {
            isOriginalTextVisible = !isOriginalTextVisible;
            if (isOriginalTextVisible) {
                recognizedTextButton.setText("Hide original");
            } else {
                recognizedTextButton.setText("Show original");
            }
            originalTextArea.setVisible(isOriginalTextVisible);
            originalLabel.setVisible(isOriginalTextVisible);
            translateButton.setVisible(isOriginalTextVisible);
            setSize(getPreferredSize());
        });

        setSize(getPreferredSize());
        setVisible(true);
    }

    public void addInputFunction(Translate translateInstance) {
        originalTextArea.setEditable(true);
        translateButton = new JButton("Translate, please ╰(*´︶`*)╯");
        translateButton.addActionListener(e ->
                showTranslateResult(translateInstance, originalTextArea.getText()));
        panelText.add(translateButton);
    }

    public String showOcrResult(BufferedImage bufferedImage, TextRecognition ocrInstance) {
        String ocrResult = "";
        String labelResultString;
        statusInfoLabel.setText("Recognizing text...");
        try {
            ocrResult = ocrInstance.recognizeText("eng+jpn+rus", bufferedImage);
            labelResultString = "Text is recognized.";
        } catch (TesseractException | IOException e) {
            labelResultString = "Text recognition is failed.";
        }
        statusInfoLabel.setText(labelResultString);
        originalTextArea.setText(ocrResult);
        return ocrResult;
    }

    public void showTranslateResult(Translate translateInstance, String sourceText) {
        statusInfoLabel.setText("Translating text...");
        String labelResultString;

        try {
            translateResult = translateInstance.translate(sourceText);
            labelResultString = "Text is translated.";
        } catch (IOException | URISyntaxException |
                ExecutionException | InterruptedException e) {
            labelResultString = "Translation is failed.";
        } catch (JSONException exception){
            labelResultString = "Translation is failed. " +
                    "DeepL json limit is exceeded, please switch to another service.";
        }

        statusInfoLabel.setText(labelResultString);
        translatedTextArea.setText(translateResult.get(0));

        boolean isEnabled = translateResult.size() > 1;
        previousResultButton.setEnabled(isEnabled);
        nextResultButton.setEnabled(isEnabled);
    }

    private void createNextPrevButtons(Container container,
                                       JTextArea textArea) {
        nextResultButton = new JButton(">>");
        nextResultButton.setEnabled(false);
        previousResultButton = new JButton("<<");
        previousResultButton.setEnabled(false);

        nextResultButton.addActionListener(ee -> {
            String newText;
            String oldText = textArea.getText();
            int index = translateResult.indexOf(oldText);
            if (index == translateResult.size() - 1) {
                newText = translateResult.get(0);
            } else {
                newText = translateResult.get(index + 1);
            }
            textArea.setText(newText);
        });

        previousResultButton.addActionListener(ee1 -> {
            String newText;
            String oldText = textArea.getText();
            int index = translateResult.indexOf(oldText);
            if (index == 0) {
                newText = translateResult.get(translateResult.size() - 1);
            } else {
                newText = translateResult.get(index - 1);
            }
            textArea.setText(newText);
        });

        container.add(previousResultButton, "flowx, split 3");
        container.add(nextResultButton);
    }

    private JTextArea createTextArea(Font font) {
        JTextArea textArea = new JTextArea();
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);
        textArea.setBackground(Color.WHITE);
        textArea.setEditable(false);
        textArea.setFont(font);
        textArea.setBorder(BorderFactory.createLineBorder(Color.black));
        return textArea;
    }
}
