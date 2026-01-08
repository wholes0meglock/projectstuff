package com.CurrencyConverter;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.Node;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;




public class Main extends Application {
    private Label chartStatusLabel;
    private Map<String, Map<String, Double>> rateCache = new HashMap<>();
    private long lastFetchTime = 0;
    private static final long CACHE_DURATION = 300000;
    private static final String BASE_URL = "https://api.exchangerate-api.com/v4/latest/";
    private static final String FIXER_API_URL = "https://api.fixer.io/latest";
    private static final String EXCHANGERATE_API = "https://api.exchangerate-api.com/v4/latest/";

    private ComboBox<String> fromCurrencyComboBox;
    private ComboBox<String> toCurrencyComboBox;
    private TextField amountTextField;
    private Label resultLabel;
    private LineChart<Number, Number> lineChart;
    private Map<String, Double> exchangeRates;
    private List<String> popularCurrencies;
    private ObjectMapper objectMapper = new ObjectMapper();
    private ListView<String> selectedCurrenciesList;
    private ObservableList<String> selectedCurrencies;
    private ComboBox<String> addCurrencyComboBox;
    private Button compareCurrenciesButton;

    private boolean darkMode = true;

    private void toggleTheme(Scene scene) {
        if (darkMode) {
            scene.getStylesheets().clear();
            scene.getStylesheets().add(getClass().getResource("/light-theme.css").toExternalForm());
            darkMode = false;
        } else {
            scene.getStylesheets().clear();
            scene.getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());
            darkMode = true;
        }
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("ExChango");


        initializePopularCurrencies();


        fromCurrencyComboBox = new ComboBox<>(FXCollections.observableArrayList(popularCurrencies));
        fromCurrencyComboBox.setValue("USD");
        fromCurrencyComboBox.setOnAction(e -> fetchExchangeRates(fromCurrencyComboBox.getValue()));

        toCurrencyComboBox = new ComboBox<>(FXCollections.observableArrayList(popularCurrencies));
        toCurrencyComboBox.setValue("EUR");

        amountTextField = new TextField("1.0");
        amountTextField.setPromptText("Enter amount");

        Button convertButton = new Button("Convert");
        convertButton.setOnAction(e -> convertCurrency());

        resultLabel = new Label("Ready for conversion");
        resultLabel.setStyle("-fx-background-color: #2E3440; " +
                "-fx-text-fill: #88C0D0; " +
                "-fx-font-size: 16px; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 15px; " +
                "-fx-border-color: #4C566A; " +
                "-fx-border-width: 2px; " +
                "-fx-border-radius: 10px; " +
                "-fx-background-radius: 10px; " +
                "-fx-alignment: center;");
        resultLabel.setMaxWidth(Double.MAX_VALUE);

        Button swapButton = new Button("Swap Currencies");
        swapButton.setOnAction(e -> swapCurrencies());

        Button showGraphButton = new Button("Exchange Rate History (Last 7 Days)");
        showGraphButton.setOnAction(e -> showExchangeRateHistory());



        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Days");
        yAxis.setLabel("Exchange Rate");

        lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Exchange Rate History");
        lineChart.setLegendVisible(false);
        lineChart.setVisible(false);


        GridPane gridPane = new GridPane();
        gridPane.setPadding(new Insets(10));
        gridPane.setHgap(10);
        gridPane.setVgap(10);

        gridPane.add(new Label("From:"), 0, 0);
        gridPane.add(fromCurrencyComboBox, 1, 0);
        gridPane.add(new Label("To:"), 0, 1);
        gridPane.add(toCurrencyComboBox, 1, 1);
        gridPane.add(new Label("Amount:"), 0, 2);
        gridPane.add(amountTextField, 1, 2);
        gridPane.add(convertButton, 0, 3);
        gridPane.add(swapButton, 1, 3);
        gridPane.add(showGraphButton, 0, 4, 2, 1);
        gridPane.add(resultLabel, 0, 5, 2, 1);
        VBox multiCurrencySelector = createMultiCurrencySelector();
        fetchExchangeRates(fromCurrencyComboBox.getValue());
        VBox mainLayout = new VBox(10);
        mainLayout.setPadding(new Insets(10));
        mainLayout.getChildren().addAll(gridPane, multiCurrencySelector, lineChart);
        Scene scene = new Scene(mainLayout, 1000, 900);
        scene.getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());
        Button toggleThemeButton = new Button("Toggle Theme");
        toggleThemeButton.setOnAction(e -> toggleTheme(scene));
        gridPane.add(toggleThemeButton, 1, 6);
        primaryStage.setScene(scene);
        primaryStage.show();

    }
    private VBox createMultiCurrencySelector() {
        VBox container = new VBox(10);
        container.setPadding(new Insets(15));
        container.setStyle("-fx-background-color: #3B4252; -fx-border-color: #4C566A; -fx-border-radius: 10px;");

        Label title = new Label(" Multi-Currency Comparison");
        title.setStyle("-fx-text-fill: #88C0D0; -fx-font-size: 16px; -fx-font-weight: bold;");

        Label subtitle = new Label("Select up to 5 currencies to compare (vs USD)");
        subtitle.setStyle("-fx-text-fill: #D8DEE9; -fx-font-size: 12px;");

        selectedCurrencies = FXCollections.observableArrayList();
        selectedCurrenciesList = new ListView<>(selectedCurrencies);
        selectedCurrenciesList.setPrefHeight(120);
        selectedCurrenciesList.setStyle("-fx-control-inner-background: #2E3440; -fx-text-fill: white;");

        HBox selectorBox = new HBox(10);
        addCurrencyComboBox = new ComboBox<>(FXCollections.observableArrayList(popularCurrencies));
        addCurrencyComboBox.setPromptText("Select currency");
        addCurrencyComboBox.setStyle("-fx-background-color: #2E3440; -fx-text-fill: white;");

        Button addButton = new Button("Add");
        addButton.setStyle("-fx-background-color: #5E81AC; -fx-text-fill: white;");
        addButton.setOnAction(e -> addCurrencyForComparison());

        Button removeButton = new Button("Remove");
        removeButton.setStyle("-fx-background-color: #BF616A; -fx-text-fill: white;");
        removeButton.setOnAction(e -> removeSelectedCurrency());

        Button clearButton = new Button("Clear All");
        clearButton.setStyle("-fx-background-color: #D08770; -fx-text-fill: white;");
        clearButton.setOnAction(e -> selectedCurrencies.clear());

        selectorBox.getChildren().addAll(addCurrencyComboBox, addButton, removeButton, clearButton);

        compareCurrenciesButton = new Button(" Generate Comparison Chart");
        compareCurrenciesButton.setStyle("-fx-background-color: #A3BE8C; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        compareCurrenciesButton.setOnAction(e -> showMultiCurrencyComparison());
        compareCurrenciesButton.setMaxWidth(Double.MAX_VALUE);

        container.getChildren().addAll(title, subtitle, selectedCurrenciesList, selectorBox, compareCurrenciesButton);
        return container;
    }
    private void addCurrencyForComparison() {
        String currency = addCurrencyComboBox.getValue();
        if (currency != null && !selectedCurrencies.contains(currency)) {
            if (selectedCurrencies.size() < 5) {
                selectedCurrencies.add(currency);
            } else {
                resultLabel.setText("Maximum 5 currencies allowed");
            }
        }
    }

    private void removeSelectedCurrency() {
        String selected = selectedCurrenciesList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            selectedCurrencies.remove(selected);
        }
    }

    private void showMultiCurrencyComparison() {
        if (selectedCurrencies.isEmpty()) {
            resultLabel.setText("Please select at least one currency to compare");
            return;
        }

        resultLabel.setText("Generating multi-currency comparison...");

        new Thread(() -> {
            try {
                Map<String, List<Double>> currencyRates = new HashMap<>();
                List<String> dates = new ArrayList<>();

                Calendar calendar = Calendar.getInstance();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

                for (String currency : selectedCurrencies) {
                    List<Double> rates = new ArrayList<>();
                    Random random = new Random();

                    calendar = Calendar.getInstance();

                    for (int i = 6; i >= 0; i--) {
                        calendar.add(Calendar.DAY_OF_YEAR, -1);
                        if (i == 6) {
                            dates.add(dateFormat.format(calendar.getTime()).substring(5));
                        }


                        double baseRate = getRealisticRate(currency);
                        double simulatedRate = baseRate * (0.97 + 0.06 * random.nextDouble());
                        rates.add(simulatedRate);
                    }
                    currencyRates.put(currency, rates);
                }

                javafx.application.Platform.runLater(() -> {
                    updateMultiCurrencyChart(dates, currencyRates);
                    resultLabel.setText("Multi-currency comparison chart generated!");
                });

            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    resultLabel.setText("Error generating comparison chart");
                });
            }
        }).start();
    }
    private void styleChartForTheme() {

        lineChart.setStyle("-fx-text-fill: #ECEFF4; " +
                "-fx-font-size: 14px; " +
                "-fx-font-family: 'System';");


        NumberAxis xAxis = (NumberAxis) lineChart.getXAxis();
        NumberAxis yAxis = (NumberAxis) lineChart.getYAxis();

        xAxis.setStyle("-fx-text-fill: #88C0D0; " +
                "-fx-tick-label-fill: #88C0D0; " +
                "-fx-axis-tick-label-fill: #88C0D0; " +
                "-fx-axis-label-fill: #88C0D0;");

        yAxis.setStyle("-fx-text-fill: #88C0D0; " +
                "-fx-tick-label-fill: #88C0D0; " +
                "-fx-axis-tick-label-fill: #88C0D0; " +
                "-fx-axis-label-fill: #88C0D0;");


        lineChart.lookup(".chart-title").setStyle("-fx-text-fill: #88C0D0; -fx-font-size: 16px; -fx-font-weight: bold;");


        lineChart.lookup(".chart-legend").setStyle("-fx-text-fill: #ECEFF4; -fx-background-color: transparent;");
    }
    private double getRealisticRate(String currency) {

        if (exchangeRates != null && exchangeRates.containsKey(currency)) {
            return exchangeRates.get(currency);
        } else {

            fetchExchangeRates("USD");
            if (exchangeRates != null && exchangeRates.containsKey(currency)) {
                return exchangeRates.get(currency);
            }
        }


        return getFallbackRate(currency);
    }
    private double getFallbackRate(String currency) {

        switch (currency) {
            case "EUR": return 0.92;
            case "GBP": return 0.79;
            case "JPY": return 149.0;
            case "CAD": return 1.36;
            case "AUD": return 1.52;
            case "CHF": return 0.88;
            case "CNY": return 7.18;
            case "INR": return 83.0;
            case "SGD": return 1.34;
            case "NZD": return 1.62;
            case "MXN": return 17.2;
            case "BRL": return 4.95;
            case "KRW": return 1310.0;
            case "RUB": return 92.5;
            case "TRY": return 28.7;
            case "ZAR": return 18.6;
            case "SEK": return 10.4;
            case "NOK": return 10.6;
            case "DKK": return 6.88;
            default: return 1.0;
        }
    }
    private void updateMultiCurrencyChart(List<String> dates, Map<String, List<Double>> currencyRates) {
        lineChart.getData().clear();
        lineChart.setTitle("Multi-Currency Exchange Rate Comparison (vs USD)");
        lineChart.setLegendVisible(true);
        lineChart.setVisible(true);


        String[] lineColors = {"#88C0D0", "#A3BE8C", "#EBCB8B", "#BF616A", "#B48EAD"};

        int colorIndex = 0;
        for (String currency : currencyRates.keySet()) {
            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName(currency);

            List<Double> rates = currencyRates.get(currency);
            for (int i = 0; i < rates.size(); i++) {
                series.getData().add(new XYChart.Data<>(i + 1, rates.get(i)));
            }

            lineChart.getData().add(series);


            if (colorIndex < lineColors.length) {
                String color = lineColors[colorIndex];
                series.getNode().setStyle("-fx-stroke: " + color + "; -fx-stroke-width: 2px;");
            }
            colorIndex++;
        }


        NumberAxis xAxis = (NumberAxis) lineChart.getXAxis();
        xAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(xAxis) {
            @Override
            public String toString(Number object) {
                int index = object.intValue() - 1;
                if (index >= 0 && index < dates.size()) {
                    return dates.get(index);
                }
                return "";
            }
        });
        styleChartForTheme();
    }
    private void initializePopularCurrencies() {
        popularCurrencies = Arrays.asList(
                "USD", "EUR", "GBP", "JPY", "CAD", "AUD", "CHF", "CNY", "INR", "MXN",
                "BRL", "RUB", "KRW", "SGD", "NZD", "TRY", "ZAR", "SEK", "NOK", "DKK"
        );
    }
    private void updateUIWithCachedRates() {
        javafx.application.Platform.runLater(() -> {
            resultLabel.setText("Using cached exchange rates");

        });
    }

    private void fetchExchangeRates(String baseCurrency) {

        long currentTime = System.currentTimeMillis();
        if (rateCache.containsKey(baseCurrency) &&
                (currentTime - lastFetchTime) < CACHE_DURATION) {

            System.out.println("Using cached rates for: " + baseCurrency);
            exchangeRates = rateCache.get(baseCurrency);
            updateUIWithCachedRates();
            return;
        }


        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + baseCurrency);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    throw new RuntimeException("HTTP error code: " + responseCode);
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JsonNode jsonResponse = objectMapper.readTree(response.toString());
                JsonNode rates = jsonResponse.get("rates");

                exchangeRates = new HashMap<>();
                for (String currency : popularCurrencies) {
                    if (rates.has(currency)) {
                        exchangeRates.put(currency, rates.get(currency).asDouble());
                    }
                }


                rateCache.put(baseCurrency, new HashMap<>(exchangeRates));
                lastFetchTime = System.currentTimeMillis();

                System.out.println("Fresh rates fetched and cached for: " + baseCurrency);

                javafx.application.Platform.runLater(() -> {
                    resultLabel.setText("Exchange rates loaded successfully!");
                });

            } catch (Exception e) {
                e.printStackTrace();

                if (rateCache.containsKey(baseCurrency)) {
                    System.out.println("API failed, using cached rates as fallback");
                    exchangeRates = rateCache.get(baseCurrency);
                    javafx.application.Platform.runLater(() -> {
                        resultLabel.setText("Using cached rates (API unavailable)");
                    });
                } else {
                    javafx.application.Platform.runLater(() -> {
                        resultLabel.setText("Error fetching rates: " + e.getMessage());
                    });
                }
            }
        }).start();
    }
    private String getCacheStatus() {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastFetch = currentTime - lastFetchTime;
        long minutes = (CACHE_DURATION - timeSinceLastFetch) / 60000;

        if (minutes > 0) {
            return "Cache valid for " + minutes + " more minutes";
        } else {
            return "Cache expired, will fetch fresh data";
        }
    }
    private void makeChartInteractive(LineChart<Number, Number> chart) {

        new Thread(() -> {
            try {
                Thread.sleep(300);
                javafx.application.Platform.runLater(() -> {
                    addSimpleTooltips(chart);
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
    private void addSimpleTooltips(LineChart<Number, Number> chart) {
        for (XYChart.Series<Number, Number> series : chart.getData()) {
            for (XYChart.Data<Number, Number> data : series.getData()) {
                Node node = data.getNode();
                if (node != null) {

                    Tooltip tooltip = new Tooltip(String.format("Rate: %.4f", data.getYValue()));
                    Tooltip.install(node, tooltip);


                    node.setOnMouseEntered(e -> node.setStyle("-fx-background-color: #ff4444;"));
                    node.setOnMouseExited(e -> node.setStyle(""));
                }
            }
        }
    }
    private void addHoverAreas(LineChart<Number, Number> chart) {
        for (XYChart.Series<Number, Number> series : chart.getData()) {
            for (XYChart.Data<Number, Number> data : series.getData()) {
                Node node = data.getNode();
                if (node != null) {
                    javafx.scene.shape.Rectangle hoverArea = new javafx.scene.shape.Rectangle(20, 20);
                    hoverArea.setFill(javafx.scene.paint.Color.TRANSPARENT);
                    hoverArea.setStroke(javafx.scene.paint.Color.TRANSPARENT);


                    hoverArea.setTranslateX(node.getTranslateX());
                    hoverArea.setTranslateY(node.getTranslateY());


                    Tooltip tooltip = new Tooltip();
                    tooltip.setText(String.format("Day: %.0f\nRate: %.4f",
                            data.getXValue().doubleValue(),
                            data.getYValue().doubleValue()));

                    Tooltip.install(hoverArea, tooltip);


                    hoverArea.setOnMouseEntered(e -> {
                        node.setScaleX(2.0);
                        node.setScaleY(2.0);
                        if (chartStatusLabel != null) {
                            chartStatusLabel.setText(String.format("Rate: %.4f on Day %.0f",
                                    data.getYValue().doubleValue(), data.getXValue().doubleValue()));
                        }
                    });

                    hoverArea.setOnMouseExited(e -> {
                        node.setScaleX(1.0);
                        node.setScaleY(1.0);
                        if (chartStatusLabel != null) {
                            chartStatusLabel.setText("Hover over data points to see values");
                        }
                    });

                    ((javafx.scene.Parent) node.getParent()).getChildrenUnmodifiable().add(hoverArea);
                }
            }
        }
    }

    private void setupDataPointTooltip(XYChart.Data<Number, Number> data) {
        Tooltip tooltip = new Tooltip();
        tooltip.setText(String.format("Day: %.0f\nRate: %.4f",
                data.getXValue().doubleValue(),
                data.getYValue().doubleValue()));

        Tooltip.install(data.getNode(), tooltip);


        data.getNode().setOnMouseEntered(e -> {
            data.getNode().setScaleX(1.5);
            data.getNode().setScaleY(1.5);
            if (chartStatusLabel != null) {
                chartStatusLabel.setText(String.format("Rate: %.4f on Day %.0f",
                        data.getYValue().doubleValue(), data.getXValue().doubleValue()));
            }
        });

        data.getNode().setOnMouseExited(e -> {
            data.getNode().setScaleX(1.0);
            data.getNode().setScaleY(1.0);
            if (chartStatusLabel != null) {
                chartStatusLabel.setText("Hover over data points to see values");
            }
        });
    }
    private void setupCrosshairTracking(LineChart<Number, Number> chart) {
        chart.setOnMouseMoved(event -> {
            if (event.getY() > chart.getBoundsInLocal().getMinY() &&
                    event.getY() < chart.getBoundsInLocal().getMaxY()) {

                NumberAxis yAxis = (NumberAxis) chart.getYAxis();
                double yValue = yAxis.getValueForDisplay(event.getY()).doubleValue();
                if (chartStatusLabel != null) {
                    chartStatusLabel.setText(String.format("Current level: %.4f", yValue));
                }
            }
        });

        chart.setOnMouseExited(e -> {
            if (chartStatusLabel != null) {
                chartStatusLabel.setText("Hover over data points to see values");
            }
        });
    }
private void convertCurrency() {
    try {
        System.out.println("Cache status: " + getCacheStatus());

        String fromCurrency = fromCurrencyComboBox.getValue();
        String toCurrency = toCurrencyComboBox.getValue();
        double amount = Double.parseDouble(amountTextField.getText());

        if (exchangeRates == null) {
            resultLabel.setText("Loading rates...");
            fetchExchangeRates(fromCurrency);
            return;
        }

        if (!exchangeRates.containsKey(toCurrency)) {
            resultLabel.setText("Rate for " + toCurrency + " not available");
            return;
        }

        double rate = exchangeRates.get(toCurrency);
        double result = amount * rate;
        resultLabel.setText(String.format("%.2f %s = %.2f %s", amount, fromCurrency, result, toCurrency));

    } catch (NumberFormatException e) {
        resultLabel.setText("Please enter a valid number");
    }
}


    private void swapCurrencies() {
        String from = fromCurrencyComboBox.getValue();
        String to = toCurrencyComboBox.getValue();

        fromCurrencyComboBox.setValue(to);
        toCurrencyComboBox.setValue(from);

        fetchExchangeRates(fromCurrencyComboBox.getValue());
    }

    private void showExchangeRateHistory() {
        String fromCurrency = fromCurrencyComboBox.getValue();
        String toCurrency = toCurrencyComboBox.getValue();


        resultLabel.setText("Fetching historical data...");

        new Thread(() -> {
            try {
                List<String> dates = new ArrayList<>();
                List<Double> rates = new ArrayList<>();

                Calendar calendar = Calendar.getInstance();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");


                if (exchangeRates != null && exchangeRates.containsKey(toCurrency)) {
                    double currentRate = exchangeRates.get(toCurrency);


                    Random random = new Random();
                    for (int i = 6; i >= 0; i--) {
                        calendar.add(Calendar.DAY_OF_YEAR, -1);
                        String date = dateFormat.format(calendar.getTime());


                        double simulatedRate = currentRate * (0.95 + 0.1 * random.nextDouble());

                        dates.add(date.substring(5));
                        rates.add(simulatedRate);
                    }


                    javafx.application.Platform.runLater(() -> {
                        updateChart(dates, rates, fromCurrency, toCurrency);
                        resultLabel.setText("Historical data loaded (simulated)");
                    });
                } else {
                    throw new RuntimeException("Current exchange rates not available");
                }

            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    resultLabel.setText("Error: " + e.getMessage() + " - Using simulated data");

                    showSimulatedHistoricalData();
                });
            }
        }).start();
    }

    private void showSimulatedHistoricalData() {
        String fromCurrency = fromCurrencyComboBox.getValue();
        String toCurrency = toCurrencyComboBox.getValue();

        List<String> dates = new ArrayList<>();
        List<Double> rates = new ArrayList<>();

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        Random random = new Random();
        double baseRate = 0.85;

        for (int i = 6; i >= 0; i--) {
            calendar.add(Calendar.DAY_OF_YEAR, -1);
            String date = dateFormat.format(calendar.getTime());

            double simulatedRate = baseRate * (0.9 + 0.2 * random.nextDouble());

            dates.add(date.substring(5));
            rates.add(simulatedRate);
        }

        updateChart(dates, rates, fromCurrency, toCurrency);
    }

    private void updateChart(List<String> dates, List<Double> rates, String fromCurrency, String toCurrency) {
        lineChart.getData().clear();

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName(fromCurrency + " to " + toCurrency);

        for (int i = 0; i < rates.size(); i++) {
            series.getData().add(new XYChart.Data<>(i + 1, rates.get(i)));
        }

        lineChart.getData().add(series);
        lineChart.setVisible(true);


        NumberAxis xAxis = (NumberAxis) lineChart.getXAxis();
        xAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(xAxis) {
            @Override
            public String toString(Number object) {
                int index = object.intValue() - 1;
                if (index >= 0 && index < dates.size()) {
                    return dates.get(index);
                }
                return "";
            }
        });
        makeChartInteractive(lineChart);
        styleChartForTheme();
    }

    public static void main(String[] args) {
        launch(args);
    }
}