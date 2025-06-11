# Currency Converter Application

This application allows users to select a base currency from a list of EU currencies, enter an amount, select a target currency, and see the converted amount. The conversion rates are fetched live from the European Central Bank (ECB). The application also calculates and displays a transaction fee based on the EUR equivalent of the amount being converted, with a tiered percentage system. A message prompts the user about how to reach a cheaper transaction fee tier.

## Features

- Fetches daily exchange rates from the European Central Bank (ECB).
- Converts amounts between various currencies.
- Base currencies limited to a selection of EU currencies.
- Target currencies populated from ECB data.
- Calculates transaction fees based on a tiered system:
    - 0 - 999.99 EUR equivalent: 5%
    - 1000 - 1999.99 EUR equivalent: 4%
    - 2000 - 2999.99 EUR equivalent: 3.2%
    - 3000 - 3999.99 EUR equivalent: 2.56%
    - 4000 - 4999.99 EUR equivalent: 2.048%
    - 5000+ EUR equivalent: 1.6384%
- Displays a message guiding users to the next fee threshold.
- Simple Swing-based graphical user interface.

## Prerequisites

- Java Development Kit (JDK) 11 or higher installed and configured.
- Internet connection (for fetching exchange rates from ECB).

## How to Compile and Run from Local PC

These instructions assume your project is located at `C:\Users\roy_b\Documents\GitHub\convcurrency`.

1.  **Open a Command Prompt or Terminal.**

2.  **Navigate to the project directory:**
    ```bash
    cd C:\Users\roy_b\Documents\GitHub\convcurrency
    ```

3.  **Create an output directory for compiled classes (if it doesn't already exist):**
    ```bash
    mkdir out
    ```
    *(You can ignore this command if the 'out' directory already exists.)*

4.  **Compile the Java code:**
    Navigate to the root of the project (`C:\Users\roy_b\Documents\GitHub\convcurrency`) and run:
    ```bash
    javac -d out -cp src src/CurrencyConverter.java
    ```
    This command tells the Java compiler (`javac`) to place the compiled `.class` files into the `out` directory (`-d out`), to look for source files in the `src` directory (`-cp src`), and to compile `CurrencyConverter.java`.

5.  **Run the application:**
    Ensure you are still in the project root directory (`C:\Users\roy_b\Documents\GitHub\convcurrency`) and run:
    ```bash
    java -cp out CurrencyConverter
    ```
    This command tells Java (`java`) to look for compiled classes in the `out` directory (`-cp out`) and to run the main method in the `CurrencyConverter` class.

A window for the Currency Converter application should appear.

## Notes
- The application requires an active internet connection on first launch (and whenever rates need to be updated, currently on every launch) to fetch the latest exchange rates from the ECB. If it cannot connect, an error message will be displayed.
- The list of base currencies is predefined. The list of target currencies is dynamically populated from the ECB data.
- The paths in the commands above are specific to the user's request. Adjust them if your project is in a different location.
