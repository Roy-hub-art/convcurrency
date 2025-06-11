import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI; // Added for URI.create()
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class CurrencyConverter {

    private JFrame frame;
    private JComboBox<String> baseCurrencyComboBox;
    private JTextField amountTextField;
    private JComboBox<String> targetCurrencyComboBox;
    private JLabel resultLabel;
    private JLabel feeLabel;
    private JLabel thresholdLabel;

    private Map<String, Double> exchangeRates = new HashMap<>();
    private final String[] EU_CURRENCIES = {"AMD", "AZN", "BAM", "BGN", "BYN", "CHF", "CZK", "DKK", "EUR", "GBP", "GEL", "HUF", "ISK", "KZT", "MDL", "MKD", "NOK", "PLN", "RON", "RUB", "RSD", "SEK", "TRY", "UAH"}; // Updated list of European currencies, excludes ALL
    private static final String ECB_RATES_URL = "https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new CurrencyConverter().createAndShowGUI();
            } catch (Exception e) {
                e.printStackTrace();
                // Display a user-friendly error dialog if GUI creation fails
                JOptionPane.showMessageDialog(null, "Failed to start application: " + e.getMessage(), "Application Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void createAndShowGUI() {
        frame = new JFrame("Currency Converter");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Base Currency
        gbc.gridx = 0;
        gbc.gridy = 0;
        frame.add(new JLabel("From Currency:"), gbc);
        baseCurrencyComboBox = new JComboBox<>(EU_CURRENCIES);
        gbc.gridx = 1;
        frame.add(baseCurrencyComboBox, gbc);

        // Amount
        gbc.gridx = 0;
        gbc.gridy = 1;
        frame.add(new JLabel("Amount:"), gbc);
        amountTextField = new JTextField(10);
        gbc.gridx = 1;
        frame.add(amountTextField, gbc);

        // Target Currency
        gbc.gridx = 0;
        gbc.gridy = 2;
        frame.add(new JLabel("To Currency:"), gbc);
        targetCurrencyComboBox = new JComboBox<>(); // Will be populated from API
        gbc.gridx = 1;
        frame.add(targetCurrencyComboBox, gbc);

        // Convert Button
        JButton convertButton = new JButton("Convert");
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        frame.add(convertButton, gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL; // Reset
        gbc.gridwidth = 1; // Reset

        // Result Label
        gbc.gridx = 0;
        gbc.gridy = 4;
        frame.add(new JLabel("Converted Amount:"), gbc);
        resultLabel = new JLabel("-");
        gbc.gridx = 1;
        frame.add(resultLabel, gbc);

        // Fee Label
        gbc.gridx = 0;
        gbc.gridy = 5;
        frame.add(new JLabel("Transaction Fee:"), gbc);
        feeLabel = new JLabel("-");
        gbc.gridx = 1;
        frame.add(feeLabel, gbc);

        // Threshold Label
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        thresholdLabel = new JLabel("Fee threshold info will appear here.");
        thresholdLabel.setForeground(Color.BLUE);
        frame.add(thresholdLabel, gbc);
        gbc.gridwidth = 1; // Reset

        // Action Listener for Convert Button (to be implemented)
        convertButton.addActionListener(e -> performConversion());

        // Load exchange rates and populate target currency combo box
        loadInitialData();

        frame.pack();
        frame.setLocationRelativeTo(null); // Center the window
        frame.setVisible(true);
    }

    private void loadInitialData() {
        // Fetch exchange rates in a background thread to avoid freezing the GUI
        new Thread(() -> {
            fetchExchangeRates();
        }).start();
        System.out.println("loadInitialData: Exchange rate fetching initiated.");
    }

    private void performConversion() {
        // 1. Get and validate user input
        String amountText = amountTextField.getText().trim();
        if (amountText.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please enter an amount.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountText);
            if (amount <= 0) {
                JOptionPane.showMessageDialog(frame, "Amount must be a positive number.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(frame, "Invalid amount format. Please enter a valid number.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String baseCurrency = (String) baseCurrencyComboBox.getSelectedItem();
        String targetCurrency = (String) targetCurrencyComboBox.getSelectedItem();

        if (baseCurrency == null || targetCurrency == null) {
            JOptionPane.showMessageDialog(frame, "Please select both base and target currencies.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 2. Handle same currency "conversion" (no actual conversion or fee)
        if (baseCurrency.equals(targetCurrency)) {
            resultLabel.setText(String.format("%.2f %s", amount, targetCurrency));
            feeLabel.setText("N/A (same currency)"); // Or 0.0 fee
            thresholdLabel.setText("No conversion needed.");
            return;
        }

        // 3. Perform the currency conversion
        double convertedValue = convertCurrency(amount, baseCurrency, targetCurrency);

        // If conversion was successful (not -1 which indicates an error)
        if (convertedValue != -1) { // Check for error indicator from convertCurrency
            // Format to 2 decimal places for display, could be more for some currencies
            resultLabel.setText(String.format("%.2f %s", convertedValue, targetCurrency));
        } else {
            resultLabel.setText("-"); // Error occurred
        }

        // 4. Determine the EUR equivalent of the original amount for fee calculation
        double amountInEuroForFee;
        // If the base currency is already EUR, no conversion needed for fee base.
        if (baseCurrency.equals("EUR")) {
            amountInEuroForFee = amount;
        } else {
            double baseRate = exchangeRates.getOrDefault(baseCurrency, 0.0);
            if (baseRate > 0) {
                // Otherwise, convert the original amount to EUR to find the fee base.
                amountInEuroForFee = amount / baseRate;
            } else {
                // This case should ideally be caught by convertCurrency, but as a fallback:
                feeLabel.setText("Error calculating fee base.");
                thresholdLabel.setText("");
                System.out.println("performConversion: Could not determine EUR equivalent for fee calculation due to missing base rate.");
                return; // Can't proceed with fee if EUR equivalent is unknown
            }
        }

        // 5. Calculate the transaction fee
        // Call to actual fee calculation will be here
        // Calculate Fee (Step 5)
        double fee = calculateFee(amountInEuroForFee);
        if (amountInEuroForFee > 0) { // Avoid division by zero for percentage display
            feeLabel.setText(String.format("%.2f EUR (%.3f%%)", fee, (fee / amountInEuroForFee) * 100));
        } else {
            feeLabel.setText(String.format("%.2f EUR", fee)); // Show only fee amount if base is zero
        }

        // 6. Generate the threshold message
        // Call to threshold message logic will be here
        // Threshold Message (Step 6)
        String thresholdMsg = getThresholdMessage(amountInEuroForFee);
        thresholdLabel.setText(thresholdMsg);

        System.out.println("performConversion: Fully processed.");
    }

    // Method to fetch and parse ECB data (Step 2)
    private void fetchExchangeRates() {
        System.out.println("Fetching exchange rates from ECB...");
        try {
            URL url = URI.create(ECB_RATES_URL).toURL(); // Use URI.create().toURL() to avoid deprecation
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000); // 5 seconds
            connection.setReadTimeout(5000);    // 5 seconds

            try (InputStream inputStream = connection.getInputStream()) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(inputStream);
                doc.getDocumentElement().normalize();

                // The ECB XML has a structure like: <Cube time="YYYY-MM-DD"><Cube currency="USD" rate="1.23"/><Cube currency="JPY" rate="130.5"/>...</Cube></Cube>
                NodeList cubeNodes = doc.getElementsByTagName("Cube");
                // Temporary map to build the new rates before replacing the main one
                Map<String, Double> rates = new HashMap<>();
                rates.put("EUR", 1.0); // Add EUR itself for easier calculations later

                for (int i = 0; i < cubeNodes.getLength(); i++) {
                    Element cubeElement = (Element) cubeNodes.item(i);
                    if (cubeElement.hasAttribute("currency") && cubeElement.hasAttribute("rate")) {
                        String currency = cubeElement.getAttribute("currency");
                        double rate = Double.parseDouble(cubeElement.getAttribute("rate"));
                        rates.put(currency, rate);
                    }
                }

                if (rates.size() > 1) { // More than just EUR
                    exchangeRates = rates; // Atomically update
                    // Update the UI with the new currencies on the Event Dispatch Thread
                    System.out.println("Successfully fetched and parsed " + (exchangeRates.size() -1) + " exchange rates.");
                    // Update UI components on the Event Dispatch Thread
                    SwingUtilities.invokeLater(() -> {
                        targetCurrencyComboBox.removeAllItems(); // Clear previous/dummy items

                        // Populate with the filtered list of European currencies
                        // that are also present in the fetched ECB rates.
                        // EU_CURRENCIES array is already updated to exclude "ALL".
                        for (String currencyCode : EU_CURRENCIES) {
                            if (exchangeRates.containsKey(currencyCode)) { // Ensure ECB provides this currency
                                targetCurrencyComboBox.addItem(currencyCode);
                            } else if (currencyCode.equals("EUR")) {
                                // EUR should always be addable even if not explicitly in ECB 'Cube currency=' list,
                                // as it's the base. exchangeRates map has it added manually.
                                targetCurrencyComboBox.addItem("EUR");
                            }
                        }

                        // Optionally, select a default target currency, e.g., USD if available AND in EU_CURRENCIES
                        // Or better, select EUR or the first item if available.
                        if (targetCurrencyComboBox.getItemCount() > 0) {
                             boolean hasUSD = false; // USD is not in EU_CURRENCIES, this check is moot for USD.
                             // Check if "USD" is in the EU_CURRENCIES array (it's not, per recent updates)
                             // for(int i=0; i<EU_CURRENCIES.length; i++) {
                             //    if(EU_CURRENCIES[i].equals("USD")) {
                             //        hasUSD = true;
                             //        break;
                             //    }
                             // }

                             // Since USD is not in the European list, let's pick EUR if available, or the first one.
                             boolean hasEUR = false;
                             for(int i=0; i<targetCurrencyComboBox.getItemCount(); i++) {
                                 if(targetCurrencyComboBox.getItemAt(i).equals("EUR")) {
                                     hasEUR = true;
                                     break;
                                 }
                             }
                            if (hasEUR) {
                                targetCurrencyComboBox.setSelectedItem("EUR");
                            } else {
                                targetCurrencyComboBox.setSelectedIndex(0); // Select the first available currency
                            }
                        }
                    });
                } else {
                    System.err.println("No exchange rates found in the XML data.");
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame, "Could not find any exchange rates in the data from ECB.", "API Data Error", JOptionPane.WARNING_MESSAGE));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Store the error or show it to the user appropriately
            // For now, print to console and show a dialog
            System.err.println("Error fetching or parsing exchange rates: " + e.getMessage());
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame, "Error fetching exchange rates from ECB: " + e.getMessage() + "\nPlease check your internet connection or try again later.", "API Error", JOptionPane.ERROR_MESSAGE));
        }
    }

    // Method for currency conversion (Step 4)
    // Converts the given amount from fromCurrency to toCurrency using EUR as an intermediary.
    private double convertCurrency(double amount, String fromCurrency, String toCurrency) {
        if (exchangeRates == null || exchangeRates.isEmpty()) {
            System.err.println("Exchange rates not loaded.");
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame, "Exchange rates are not available. Please try again later.", "Conversion Error", JOptionPane.ERROR_MESSAGE));
            return -1; // Indicate error
        }

        // EUR is the base for all rates in exchangeRates map
        // exchangeRates.get("USD") means 1 EUR = X USD

        double rateFrom = exchangeRates.getOrDefault(fromCurrency, -1.0);
        double rateTo = exchangeRates.getOrDefault(toCurrency, -1.0);

        if (rateFrom <= 0 || rateTo <= 0) {
            System.err.println("Invalid currency code or rate for: " + fromCurrency + " or " + toCurrency);
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame, "Invalid currency selected or rates unavailable for the selected pair.", "Conversion Error", JOptionPane.ERROR_MESSAGE));
            return -1; // Indicate error
        }

        // Step 1: Convert the initial amount to EUR if it is not already EUR.
        double amountInEur;
        if (fromCurrency.equals("EUR")) {
            amountInEur = amount;
        } else {
            amountInEur = amount / rateFrom; // Convert input amount to EUR
        }

        // Step 2: Convert the EUR amount to the target currency.
        double convertedAmount;
        if (toCurrency.equals("EUR")) {
            convertedAmount = amountInEur;
        } else {
            convertedAmount = amountInEur * rateTo; // Convert EUR amount to target currency
        }

        System.out.println("Converted " + amount + " " + fromCurrency + " to " + convertedAmount + " " + toCurrency);
        return convertedAmount;
    }

    // Method for fee calculation (Step 5)
    // Calculates the transaction fee based on the EUR equivalent of the amount.
    private double calculateFee(double amountInEuro) {
        if (amountInEuro < 0) return 0; // Should not happen with prior checks

        // Determine the fee percentage based on the transaction amount in EUR.
        double feePercentage;
        if (amountInEuro < 1000) { // 0 - 999.99
            feePercentage = 0.05; // 5%
        } else if (amountInEuro < 2000) { // 1000 - 1999.99
            feePercentage = 0.04; // 4%
        } else if (amountInEuro < 3000) { // 2000 - 2999.99
            feePercentage = 0.032; // 3.2%
        } else if (amountInEuro < 4000) { // 3000 - 3999.99
            feePercentage = 0.0256; // 2.56%
        } else if (amountInEuro < 5000) { // 4000 - 4999.99
            feePercentage = 0.02048; // 2.048%
        } else { // 5000 and above
            feePercentage = 0.016384; // 1.6384%
        }

        // Calculate the actual fee amount.
        double feeAmount = amountInEuro * feePercentage;
        System.out.println("Calculated fee for " + amountInEuro + " EUR is " + feeAmount + " EUR (" + (feePercentage*100) + "%)");
        return feeAmount;
    }

    // Method for threshold message (Step 6)
    // Generates a message advising the user on how to reach a better fee tier.
    private String getThresholdMessage(double amountInEuro) {
        if (amountInEuro < 0) return ""; // Should not happen

        double currentFeePercentage;
        double nextTierStart = -1;
        double nextTierFeePercentage = -1;

        // Determine current fee and next tier
        if (amountInEuro < 1000) { // Current: 5%
            currentFeePercentage = 0.05;
            nextTierStart = 1000;
            nextTierFeePercentage = 0.04; // 4%
        } else if (amountInEuro < 2000) { // Current: 4%
            currentFeePercentage = 0.04;
            nextTierStart = 2000;
            nextTierFeePercentage = 0.032; // 3.2%
        } else if (amountInEuro < 3000) { // Current: 3.2%
            currentFeePercentage = 0.032;
            nextTierStart = 3000;
            nextTierFeePercentage = 0.0256; // 2.56%
        } else if (amountInEuro < 4000) { // Current: 2.56%
            currentFeePercentage = 0.0256;
            nextTierStart = 4000;
            nextTierFeePercentage = 0.02048; // 2.048%
        } else if (amountInEuro < 5000) { // Current: 2.048%
            currentFeePercentage = 0.02048;
            nextTierStart = 5000;
            nextTierFeePercentage = 0.016384; // 1.6384%
        } else { // Current: 1.6384% (lowest tier)
            // User is in the highest tier, no better tier available.
            currentFeePercentage = 0.016384;
            return String.format("Current fee: %.3f%%. You are already in the best available fee tier.", currentFeePercentage * 100);
        }

        // Calculate how much more EUR is needed to reach the start of the next tier.
        double amountNeededForNextTier = nextTierStart - amountInEuro;

        return String.format("Current fee: %.3f%%. To reach %.2f EUR for %.3f%% fee, add %.2f EUR.",
                currentFeePercentage * 100,
                nextTierStart,
                nextTierFeePercentage * 100,
                amountNeededForNextTier);
    }
}
