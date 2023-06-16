package com.example.monkeytype;

import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


public class Main extends Application {
    AtomicInteger pos = new AtomicInteger(0);
    boolean isLanguageChosen = false;
    boolean isTimeChosen = false;
    boolean isStarted = false;
    boolean isStopped = false;
    boolean isPaused = false;
    int correct = 0;
    int incorrect = 0;
    int extra = 0;
    int missed = 0;
    int mistakes = 0;
    int lettersPerSecond = 0;
    int textLength = 0;
    int letters = 0;
    int secondsForWord = 0;
    final List<String> wordsList = new ArrayList<>();
    List<String> allGeneratedWordsList = new ArrayList<>();
    List<Integer> secondsPerWord = new ArrayList<>();
    ComboBox<String> comboBox = new ComboBox<>();
    ComboBox<String> comboBoxTime = new ComboBox<>();
    NumberAxis xAxis = new NumberAxis();
    NumberAxis yAxis = new NumberAxis();
    XYChart.Series<Number, Number>
            seriesRaw = new XYChart.Series<>();
    XYChart.Series<Number, Number>
            seriesWPM = new XYChart.Series<>();
    AreaChart<Number, Number> areaChart = new AreaChart<>(xAxis, yAxis);
    Label chooseLanguage = new Label("Choose language:");
    Label chooseTime = new Label("Choose time:");
    Label timeShow = new Label("Time left:");
    Label statShow = new Label("Stats");
    Label shortcutShow = new Label("Tab + Enter: restart test.    Ctrl + Shift + P: pause.    Esc: end test.");
    IntegerProperty correctProperty = new SimpleIntegerProperty(0);
    IntegerProperty missedProperty = new SimpleIntegerProperty(0);
    IntegerProperty incorrectProperty = new SimpleIntegerProperty(0);
    IntegerProperty extraProperty = new SimpleIntegerProperty(0);

    @Override
    public void start(Stage primaryStage) throws Exception {

        AtomicInteger time = new AtomicInteger();

        xAxis.setLabel("Seconds");
        yAxis.setLabel("Words per minute");

        seriesRaw.setName("Raw");

        seriesWPM.setName("WPM");

        IntegerProperty valueProperty = new SimpleIntegerProperty(0);
        timeShow.textProperty().bind(valueProperty.asString("Time left: %d"));

        statShow.textProperty().bind(new StringBinding() {
            @Override
            protected String computeValue() {
                {
                    super.bind(correctProperty, missedProperty, incorrectProperty, extraProperty);
                }
                return correctProperty.get() + "/" + incorrectProperty.get() + "/" + extraProperty.get() + "/" + missedProperty.get();
            }
        });

        List<String> languageNames = getTxtFileNames();
        for (String language : languageNames){
            comboBox.getItems().add(language);
        }

        String[] timeCollection = new String[]{"15", "20", "45", "60", "90", "120", "300"};
        for (String timeIn : timeCollection){
            comboBoxTime.getItems().add(timeIn);
        }

        TextFlow textFlow = new TextFlow();
        comboBox.getSelectionModel().clearSelection();

        comboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            textFlow.getChildren().clear();
            readWordsFromFile("dictionary/" + newValue + ".txt", wordsList);

            String stringOfWords = thirtyWords(wordsList);
            textLength = stringOfWords.length();
            Text text = new Text(stringOfWords);
            text.setFill(Color.GREY);
            textFlow.getChildren().add(text);

            pos.set(0);
            isLanguageChosen = true;
        });

        comboBoxTime.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> {
            try {
                time.set(Integer.parseInt(newValue));
                valueProperty.set(Integer.parseInt(newValue));
            }
            catch (NumberFormatException ex){
                System.out.println(ex.getMessage());
            }
            isTimeChosen = true;
        }));

        areaChart.setTitle("Your WPM");
        areaChart.setLegendSide(Side.RIGHT);
        areaChart.getData().add(seriesRaw);
        areaChart.getData().add(seriesWPM);

        areaChart.setVisible(false);
        statShow.setVisible(false);

        GridPane root = new GridPane();
        root.setPadding(new Insets(10));
        root.setVgap(20);

        root.add(chooseLanguage, 0 ,0);
        root.add(chooseTime, 1, 0);
        root.add(comboBox, 0, 1);
        root.add(comboBoxTime, 1, 1);
        root.add(timeShow, 0, 2);
        root.add(textFlow, 0, 3);
        root.add(areaChart, 0 , 4);
        root.add(statShow, 0, 5);
        root.add(shortcutShow, 0, 6);

        root.getColumnConstraints().add(new ColumnConstraints(400));

        GridPane.setHalignment(comboBox, HPos.CENTER);
        GridPane.setHalignment(textFlow, HPos.CENTER);
        GridPane.setColumnSpan(textFlow, 2);
        GridPane.setHalignment(chooseLanguage, HPos.CENTER);
        GridPane.setHalignment(chooseTime, HPos.CENTER);

        Scene scene = new Scene(root);

        scene.setOnKeyPressed(event -> {
            if (event.isControlDown() && event.isShiftDown() && event.getCode() == KeyCode.P){
                if (isStarted) {
                    isPaused = !isPaused;
                    System.out.println("Paused");
                    event.consume();
                }
            }
            else if (event.getCode() == KeyCode.ESCAPE){
                isPaused = !isPaused;
                time.set(0);
                event.consume();
            }
        });

        scene.setOnKeyTyped(event -> {
            if (isLanguageChosen && isTimeChosen) {
                if (!isStarted){
                    seriesRaw.getData().clear();
                    seriesWPM.getData().clear();

                    Service<Void> service = new Service<Void>() {
                        @Override
                        protected Task<Void> createTask() {
                            return new Task<Void>() {
                                @Override
                                protected Void call() throws Exception {
                                    int currentTime = 0;
                                    while (time.get() > 0) {
                                        while (isPaused){
                                            try {
                                                Thread.sleep(100);
                                            } catch (InterruptedException e) {
                                                throw new RuntimeException(e);
                                            }
                                        }

                                        Platform.runLater(() -> {
                                            time.set(time.get() - 1);
                                            valueProperty.set(valueProperty.intValue() - 1);
                                        });

                                        try {
                                            Thread.sleep(1000);
                                        } catch (InterruptedException e) {
                                            throw new RuntimeException(e);
                                        }

                                        currentTime++;

                                        int finalCurrentTime = currentTime;
                                        int rawWpm = calculateRawWpm();
                                        int currentWpm = calculateCurrentWpm(rawWpm);

                                        mistakes = 0;

                                        Platform.runLater(() -> {
                                            System.out.println("Time: " + finalCurrentTime);
                                            System.out.println("RAW: " + rawWpm);
                                            System.out.println("WPM: " + currentWpm);

                                            seriesRaw.getData().add(new XYChart.Data<Number, Number>(finalCurrentTime, rawWpm));
                                            seriesWPM.getData().add(new XYChart.Data<Number, Number>(finalCurrentTime, currentWpm));
                                        });
                                    }

                                    stopTest();

                                    return null;
                                }
                            };
                        }
                    };

                    service.start();
                    //thread.setDaemon(true);
                    //thread.start();

                    comboBox.setDisable(true);
                    comboBoxTime.setDisable(true);
                    areaChart.setVisible(false);
                    statShow.setVisible(false);

                    isStarted = true;
                    isPaused = false;
                }
                if (!Objects.equals(event.getCharacter(), "\b") && event.getCode() != KeyCode.ESCAPE && !isPaused) {
                    char typedCharacter = event.getCharacter().charAt(0);
                    updateTextFlow(typedCharacter, textFlow);
                    pos.getAndIncrement();
                }
            }
        });



        primaryStage.setMinWidth(400);
        primaryStage.setMinHeight(400);
        primaryStage.setResizable(false);
        primaryStage.setTitle("Monkey type");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private List<String> getTxtFileNames() {

        List<String> txtFileNames = new ArrayList<>();
        String folderPath = "dictionary";

        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(folderPath), "*.txt")) {
            for (Path path : directoryStream) {
                txtFileNames.add(path.getFileName().toString().substring(0, path.getFileName().toString().length() - 4));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return txtFileNames;
    }

    private void readWordsFromFile(String filePath, List<String> words) {
        words.clear();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                words.add(line);
            }
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

    private String thirtyWords(List<String> words){
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < 30; i++){
            Random random = new Random();
            try {
                String word = words.get(random.nextInt(words.size() + 1));
                allGeneratedWordsList.add(word);
                res.append(word).append(" ");
            }
            catch (IndexOutOfBoundsException ex){
                System.out.println(ex.getMessage());
            }
        }

        return res.toString();
    }

    private void updateTextFlow(char typedCharacter, TextFlow textFlow){
        String string = new String();
        string += typedCharacter;
        Text newText = new Text(string);

        int textNum = textFlow.getChildren().size();

        if (textNum > 0) {
            List<Text> textList = new ArrayList<>();
            for (int i = 0; i < textFlow.getChildren().size(); i++) {
                textList.add((Text) textFlow.getChildren().get(i));
            }
            textFlow.getChildren().clear();

                for (int i = 0; i < textList.size(); i++) {
                    if (i == pos.get()) {
                        if (newText.getText().charAt(0) == textList.get(i).getText().charAt(0)) {
                            newText.setFill(Color.GREEN);

                            TranslateTransition translateTransition = new TranslateTransition(Duration.millis(100), newText);
                            translateTransition.setByY(-3);
                            translateTransition.setInterpolator(Interpolator.LINEAR);
                            translateTransition.setCycleCount(2);
                            translateTransition.setAutoReverse(true);

                            String updatedString = textList.get(i).getText().substring(1);

                            Text updatedText = new Text(updatedString);
                            updatedText.setFill(Color.GREY);

                            textList.set(i, updatedText);
                            textFlow.getChildren().add(newText);

                            translateTransition.play();

                            correct++;
                            lettersPerSecond++;
                            letters++;
                            secondsForWord++;
                            if (Objects.equals(newText.getText(), " ")){
                                secondsPerWord.add(secondsForWord);
                                secondsForWord = 0;
                            }
                        } else if (Objects.equals(newText.getText(), " ")) {
                            String omittedChar = textList.get(i).getText().substring(0, 1);

                            String remainingString = textList.get(i).getText().substring(1);

                            Text omittedText = new Text(omittedChar);
                            Text remainingText = new Text(remainingString);
                            remainingText.setFill(Color.GREY);

                            TranslateTransition translateTransition = new TranslateTransition(Duration.millis(100), omittedText);
                            translateTransition.setByY(-3);
                            translateTransition.setInterpolator(Interpolator.LINEAR);
                            translateTransition.setCycleCount(2);
                            translateTransition.setAutoReverse(true);

                            textFlow.getChildren().add(omittedText);
                            translateTransition.play();

                            textList.set(i, remainingText);

                            missed++;
                            letters++;
                            mistakes++;
                        } else if (Character.isWhitespace(textList.get(i).getText().charAt(0))) {
                            newText.setFill(Color.ORANGE);

                            TranslateTransition translateTransition = new TranslateTransition(Duration.millis(100), newText);
                            translateTransition.setByY(-3);
                            translateTransition.setInterpolator(Interpolator.LINEAR);
                            translateTransition.setCycleCount(2);
                            translateTransition.setAutoReverse(true);

                            textFlow.getChildren().add(newText);

                            translateTransition.play();

                            extra++;
                            lettersPerSecond++;
                            mistakes++;
                        } else {
                            newText.setFill(Color.RED);

                            TranslateTransition translateTransition = new TranslateTransition(Duration.millis(100), newText);
                            translateTransition.setByY(-3);
                            translateTransition.setInterpolator(Interpolator.LINEAR);
                            translateTransition.setCycleCount(2);
                            translateTransition.setAutoReverse(true);

                            textFlow.getChildren().add(newText);

                            translateTransition.play();

                            incorrect++;
                            lettersPerSecond++;
                            mistakes++;
                        }
                    }
                    textFlow.getChildren().add(textList.get(i));
                }
                if (textLength == letters){
                    textFlow.getChildren().clear();

                    String stringOfWords = thirtyWords(wordsList);
                    textLength = stringOfWords.length();
                    Text text = new Text(stringOfWords);
                    text.setFill(Color.GREY);

                    textFlow.getChildren().add(text);

                    pos.set(-1);
                    letters = 0;
                }
            }
    }

    private void stopTest(){
        Platform.runLater(() -> {
            isStopped = true;
            isStarted = false;
            isPaused = false;

            comboBox.setDisable(false);
            comboBoxTime.setDisable(false);

            correctProperty.set(correct);
            incorrectProperty.set(incorrect);
            extraProperty.set(extra);
            missedProperty.set(missed);

            System.out.println("Correct: " + correct);
            System.out.println("Incorrect: " + incorrect);
            System.out.println("Extra: " + extra);
            System.out.println("Missed: " + missed);

            areaChart.setVisible(true);
            statShow.setVisible(true);

            comboBox.getSelectionModel().clearSelection();
            comboBoxTime.getSelectionModel().clearSelection();

            isTimeChosen = false;
            isLanguageChosen = false;

            generateSaveFile();

            allGeneratedWordsList.clear();
            secondsPerWord.clear();

            secondsForWord = 0;
            correct = 0;
            incorrect = 0;
            missed = 0;
            extra = 0;
            mistakes = 0;
        });
    }


    private int calculateRawWpm(){
        int res = lettersPerSecond * 12;
        lettersPerSecond = 0;
        return res;
    }

    private int calculateCurrentWpm(int rawWPM){
        int res = rawWPM - mistakes * 12;

        if (res < 0) {
            res = 0;
        }

        return res;
    }

    private void generateSaveFile(){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        Date currentDate = new Date();
        String fileName = dateFormat.format(currentDate);

        try(FileWriter writer = new FileWriter(fileName + ".txt")) {
            for (int i = 0; i < secondsPerWord.size(); i++) {
                String line = allGeneratedWordsList.get(i) + " -> " + 60/secondsPerWord.get(i) + "wpm\n";

                writer.write(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

