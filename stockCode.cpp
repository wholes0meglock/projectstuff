#include <iostream>
#include <string>
#include <vector>
#include <map>
#include <unordered_map>
#include <iomanip>
#include <cstdlib>
#include <ctime>
#include <raylib.h>
#include <sstream>
#define _CRT_SECURE_NO_WARNINGS
#define _CRT_NONSTDC_NO_WARNINGS
using namespace std;


class IUser {
public:
    virtual void displayPortfolio() const = 0;
    virtual ~IUser() {}
};

class User : public IUser {
protected:
    string username;
    double balance;
    unordered_map<string, int> portfolio;

public:
    User(const string& name, double bal = 10000.0) : username(name), balance(bal) {}

    string getUsername() const {
        return username;
    }

    double getBalance() const {
        return balance;
    }

    void deposit(double amt) { balance += amt; }

    bool withdraw(double amt) {
        if (amt > balance) return false;
        balance -= amt;
        return true;
    }

    void addStock(const string& stock, int qty) {
        portfolio[stock] += qty;
    }

    bool removeStock(const string& stock, int qty) {
        if (portfolio[stock] < qty) return false;
        portfolio[stock] -= qty;
        if (portfolio[stock] == 0) portfolio.erase(stock);
        return true;
    }

    void displayPortfolio() const override {
        cout << "\nPortfolio of " << username << ":\n";
        if (portfolio.empty()) {
            cout << "  No stocks owned.\n";
            return;
        }
        for (const auto& p : portfolio) {
            cout << "  " << p.first << ": " << p.second << " shares\n";
        }
    }

    const unordered_map<string, int>& getPortfolio() const {
        return portfolio;
    }

    void viewBalance() const {
        cout << "\nCurrent Balance: $" << fixed << setprecision(2) << balance << endl;
    }

    void viewStockQuantity(const string& stock) const {
        auto it = portfolio.find(stock);
        if (it != portfolio.end()) {
            cout << "You own " << it->second << " shares of " << stock << ".\n";
        }
        else {
            cout << "You do not own any shares of " << stock << ".\n";
        }
    }
};


class UserManager {
private:
    unordered_map<string, User*> users;
    User* currentUser = nullptr;

public:
    ~UserManager() {
        for (auto& pair : users) delete pair.second;
    }

    bool login(const string& username) {
        if (users.find(username) == users.end()) {
            users[username] = new User(username);
        }
        currentUser = users[username];
        return true;
    }

    void logout() {
        currentUser = nullptr;
    }

    User* getCurrentUser() {
        return currentUser;
    }
};


class IStockMarket {
public:
    virtual void updatePrices() = 0;
    virtual void displayPrices() const = 0;
    virtual ~IStockMarket() {}
};

class StockMarket : public IStockMarket {
protected:
    unordered_map<string, double> stockPrices;

public:
    StockMarket() {
        stockPrices = {
            {"AAPL", 150.0}, {"GOOG", 2800.0}, {"TSLA", 700.0},
			{"MSFT", 300.0}, {"AMZN", 3400.0} , {"FB", 350.0},
			{"NFLX", 600.0}, {"NVDA", 200.0}, {"AMD", 100.0},
            {"INTC", 50.0}, {"DIS", 180.0}, {"V", 250.0} , {"JIIT" , 50} , {"Jaypee", 0.01}
        };
        srand(static_cast<unsigned int>(time(nullptr)));
    }

    virtual void updatePrices() override {
        for (auto& p : stockPrices) {
            double change = (rand() % 1000 - 500) / 100.0;
            p.second += change;
            if (p.second < 1) p.second = 1;
        }
    }

    virtual void displayPrices() const override {
        cout << "\nLive Stock Prices:\n";
        for (const auto& p : stockPrices) {
            cout << "  " << setw(5) << p.first << " : $" << fixed << setprecision(2) << p.second << "\n";
        }
    }

    double getPrice(const string& stock) const {
        auto it = stockPrices.find(stock);
        return it != stockPrices.end() ? it->second : -1;
    }

    unordered_map<string, double> getAllPrices() const {
        return stockPrices;
    }

    bool buyStock(User* user, const string& stock, int quantity) {
        double price = getPrice(stock);
        if (price == -1) return false;

        double total = price * quantity;
        if (!user->withdraw(total)) return false;

        user->addStock(stock, quantity);
        return true;
    }

    bool sellStock(User* user, const string& stock, int quantity) {
        double price = getPrice(stock);
        if (price == -1) return false;

        if (!user->removeStock(stock, quantity)) return false;

        user->deposit(price * quantity);
        return true;
    }
};


class StockPredictor : public StockMarket {
private:
    unordered_map<string, vector<double>> priceHistoryMap;
    const int SMA_WINDOW = 5;

public:
    void recordPrice(const string& stock, double price) {
        priceHistoryMap[stock].push_back(price);
        if (priceHistoryMap[stock].size() > 50) {
            priceHistoryMap[stock].erase(priceHistoryMap[stock].begin());
        }
    }

    double predictSMA(const string& stock) {
        if (priceHistoryMap[stock].size() < SMA_WINDOW) return -1;

        double sum = 0;
        for (size_t i = priceHistoryMap[stock].size() - SMA_WINDOW; i < priceHistoryMap[stock].size(); ++i) {
            sum += priceHistoryMap[stock][i];
        }
        return sum / SMA_WINDOW;
    }
};


class PortfolioAnalyzer {
public:
    void analyze(const unordered_map<string, int>& portfolio, const unordered_map<string, double>& prices) {
        cout << "\n=== Portfolio Analysis ===\n";
        double totalValue = 0;

        if (portfolio.empty()) {
            cout << "No stocks to analyze.\n";
            return;
        }

        for (const auto& [stock, quantity] : portfolio) {
            auto it = prices.find(stock);
            if (it != prices.end()) {
                double value = it->second * quantity;
                cout << stock << ": " << quantity << " shares x $" << fixed << setprecision(2)
                    << it->second << " = $" << value << endl;
                totalValue += value;
            }
        }

        cout << "Total Portfolio Value: $" << fixed << setprecision(2) << totalValue << endl;
    }
};


struct Transaction {
    string type;
    string stock;
    int quantity;
    double price;
    string timestamp;
};


class TransactionLogger {
private:
    map<string, vector<Transaction>> userLogs;

    string getCurrentTimestamp() {
        time_t now = time(nullptr);
        char buf[80];
        tm timeInfo;
        localtime_s(&timeInfo, &now);
        strftime(buf, sizeof(buf), "%Y-%m-%d %X", &timeInfo);
        return string(buf);
    }

public:
    void logTransaction(const string& username, const string& type, const string& stock, int quantity, double price) {
        Transaction tx = { type, stock, quantity, price, getCurrentTimestamp() };
        userLogs[username].push_back(tx);
    }

    void displayUserHistory(const string& username) const {
        auto it = userLogs.find(username);
        if (it == userLogs.end() || it->second.empty()) {
            cout << "\nNo transactions for " << username << ".\n";
            return;
        }

        cout << "\nTransaction History for " << username << ":\n";
        for (const auto& tx : it->second) {
            cout << "  [" << tx.timestamp << "] " << tx.type
                << " " << tx.quantity << " x " << tx.stock
                << " @ $" << fixed << setprecision(2) << tx.price << "\n";
        }
    }
};


void DrawCenteredText(const char* text, int posY, int fontSize, Color color) {
    int textWidth = MeasureText(text, fontSize);
    DrawText(text, (GetScreenWidth() - textWidth) / 2, posY, fontSize, color);
}

void DrawButton(const char* text, Rectangle bounds, Color bgColor, Color textColor) {
    DrawRectangleRec(bounds, bgColor);
    DrawText(text, bounds.x + (bounds.width - MeasureText(text, 20)) / 2, bounds.y + 10, 20, textColor);
}

bool Button(const char* text, Rectangle bounds) {
    bool clicked = false;
    Color bgColor = LIGHTGRAY;

    if (CheckCollisionPointRec(GetMousePosition(), bounds)) {
        bgColor = GRAY;
        if (IsMouseButtonPressed(MOUSE_LEFT_BUTTON)) {
            clicked = true;
        }
    }

    DrawButton(text, bounds, bgColor, BLACK);
    return clicked;
}

void DrawStockPrices(const unordered_map<string, double>& prices, int startY) {
    int y = startY;
    for (const auto& p : prices) {
        stringstream ss;
        ss << p.first << ": $" << fixed << setprecision(2) << p.second;
        DrawText(ss.str().c_str(), 50, y, 20, BLACK);
        y += 30;
    }
}

void DrawPortfolio(const User* user, const unordered_map<string, double>& prices, int startY) {
    if (!user) return;

    stringstream balanceText;
    balanceText << "Balance: $" << fixed << setprecision(2) << user->getBalance();
    DrawText(balanceText.str().c_str(), 50, startY, 20, BLACK);

    const auto& portfolio = user->getPortfolio();
    if (portfolio.empty()) {
        DrawText("No stocks owned", 50, startY + 40, 20, GRAY);
        return;
    }

    int y = startY + 40;
    for (const auto& p : portfolio) {
        stringstream ss;
        double price = prices.count(p.first) ? prices.at(p.first) : 0.0;
        double value = price * p.second;
        ss << p.first << ": " << p.second << " shares ($" << fixed << setprecision(2) << value << ")";
        DrawText(ss.str().c_str(), 50, y, 20, BLACK);
        y += 30;
    }
}

bool TextInputBox(Rectangle bounds, char* text, int textSize, bool& active) {
    bool clicked = CheckCollisionPointRec(GetMousePosition(), bounds) && IsMouseButtonPressed(MOUSE_LEFT_BUTTON);

    if (clicked) {
        active = true;
    }
    else if (IsMouseButtonPressed(MOUSE_LEFT_BUTTON)) {
        active = false;
    }

    if (active) {
        int key = GetCharPressed();
        while (key > 0) {
            if ((key >= 32 && key <= 125) && (strlen(text) < textSize - 1)) {
                if (strlen(text) < textSize - 1) {
                    text[strlen(text)] = static_cast<char>(key);
                    text[strlen(text) + 1] = '\0';
                }
            }
            key = GetCharPressed();
        }

        if (IsKeyPressed(KEY_BACKSPACE) && strlen(text) > 0) {
            text[strlen(text) - 1] = '\0';
        }
    }

    DrawRectangleRec(bounds, active ? LIGHTGRAY : WHITE);
    DrawRectangleLinesEx(bounds, 1, DARKGRAY);
    DrawText(text, bounds.x + 5, bounds.y + 5, 20, BLACK);

    return active;
}

template<typename T>
T clamp(T value, T min, T max) {
    if (value < min) return min;
    if (value > max) return max;
    return value;
}

int main() {
    const int screenWidth = 800;
    const int screenHeight = 600;

    InitWindow(screenWidth, screenHeight, "Stock Simulator");
    SetTargetFPS(15);

    UserManager userManager;
    StockPredictor market;
    PortfolioAnalyzer analyzer;
    TransactionLogger logger;

    char usernameInput[256] = { 0 };
    bool loggedIn = false;
    int currentScreen = 0;
    bool usernameInputActive = false;

    static Vector2 stockScroll = { 0, 0 };
    static string selectedStock = "";

    while (!WindowShouldClose()) {
        market.updatePrices();
        for (const auto& p : market.getAllPrices()) {
            market.recordPrice(p.first, p.second);
        }

        if (!loggedIn) {
            if (IsKeyPressed(KEY_ENTER) && strlen(usernameInput) > 0) {
                userManager.login(usernameInput);
                loggedIn = true;
                currentScreen = 1;
                usernameInputActive = false;
            }

            Rectangle inputBox = { screenWidth / 2 - 150, 200, 300, 40 };
            TextInputBox(inputBox, usernameInput, 256, usernameInputActive);
        }

        BeginDrawing();
        ClearBackground(RAYWHITE);

        if (!loggedIn) {
            DrawCenteredText("STOCK SIMULATOR ", 50, 40, DARKGRAY);
            DrawCenteredText("Enter your username:", 150, 30, DARKGRAY);

            Rectangle inputBox = { screenWidth / 2 - 150, 200, 300, 40 };
            TextInputBox(inputBox, usernameInput, 256, usernameInputActive);

            Rectangle loginButton = { screenWidth / 2 - 100, 260, 200, 40 };
            if (Button("Login", loginButton) && strlen(usernameInput) > 0) {
                userManager.login(usernameInput);
                loggedIn = true;
                currentScreen = 1;
            }
        }
        else {
            User* user = userManager.getCurrentUser();
            auto prices = market.getAllPrices();

            string welcomeText = "Welcome, " + user->getUsername() + "!";
            DrawText(welcomeText.c_str(), 20, 20, 20, DARKGRAY);

            string balanceText = "Balance: $" + to_string(user->getBalance());
            balanceText = balanceText.substr(0, balanceText.find(".") + 3);
            DrawText(balanceText.c_str(), 20, 50, 20, DARKGRAY);

            Rectangle viewPricesBtn = { 50, 100, 200, 40 };
            Rectangle buyStockBtn = { 50, 160, 200, 40 };
            Rectangle sellStockBtn = { 50, 220, 200, 40 };
            Rectangle portfolioBtn = { 50, 280, 200, 40 };
            Rectangle predictBtn = { 50, 340, 200, 40 };
            Rectangle logoutBtn = { 50, 400, 200, 40 };

            if (Button("View Prices", viewPricesBtn)) currentScreen = 1;
            if (Button("Buy Stock", buyStockBtn)) currentScreen = 2;
            if (Button("Sell Stock", sellStockBtn)) currentScreen = 3;
            if (Button("View Portfolio", portfolioBtn)) currentScreen = 4;
            if (Button("AI Predictions", predictBtn)) currentScreen = 5;
            if (Button("Logout", logoutBtn)) {
                userManager.logout();
                loggedIn = false;
                currentScreen = 0;
                memset(usernameInput, 0, sizeof(usernameInput));
            }

            Rectangle stockListArea = { 300, 100, 250, 450 };
            Rectangle actionArea = { 560, 100, 190, 450 };

            DrawRectangleRec(stockListArea, Color{ 230, 230, 230, 255 });
            DrawRectangleLinesEx(stockListArea, 2, DARKGRAY);

            DrawRectangleRec(actionArea, LIGHTGRAY);
            DrawRectangleLinesEx(actionArea, 2, DARKGRAY);

            DrawText("Current Prices:", 310, 120, 20, BLACK);

            Rectangle stockScrollArea = { stockListArea.x + 5, stockListArea.y + 40,
                                        stockListArea.width - 10, stockListArea.height - 50 };
            BeginScissorMode(stockScrollArea.x, stockScrollArea.y, stockScrollArea.width, stockScrollArea.height);

            int yPos = static_cast<int>(stockScrollArea.y + stockScroll.y);
            for (const auto& p : prices) {
                Color textColor = (selectedStock == p.first) ? BLUE : BLACK;

                DrawText(p.first.c_str(), stockScrollArea.x + 10, yPos, 20, textColor);

                stringstream priceSS;
                priceSS << "$" << fixed << setprecision(2) << p.second;
                string priceText = priceSS.str();
                int priceWidth = MeasureText(priceText.c_str(), 20);
                DrawText(priceText.c_str(), stockScrollArea.x + stockScrollArea.width - priceWidth - 10, yPos, 20, textColor);

                if (CheckCollisionPointRec(GetMousePosition(),
                    { stockScrollArea.x, (float)yPos, stockScrollArea.width, 25 }) &&
                    IsMouseButtonPressed(MOUSE_LEFT_BUTTON)) {
                    selectedStock = p.first;
                }

                yPos += 30;
            }
            EndScissorMode();

            if (CheckCollisionPointRec(GetMousePosition(), stockScrollArea)) {
                stockScroll.y += GetMouseWheelMove() * 20;
                stockScroll.y = clamp<float>(stockScroll.y,
                    -(static_cast<float>(yPos) - (stockScrollArea.y + stockScrollArea.height)),
                    0.0f);
            }

            switch (currentScreen) {
            case 1: {
                DrawText("Select an action", 570, 120, 20, BLACK);
                DrawText("Prices are shown", 570, 160, 20, BLACK);
                DrawText("on the left", 570, 190, 20, BLACK);
                break;
            }
            case 2: {
                static char stockInput[10] = { 0 };
                static char qtyInput[10] = { 0 };
                static bool stockInputActive = false;
                static bool qtyInputActive = false;

                if (!selectedStock.empty() && strlen(stockInput) == 0) {
                    strncpy_s(stockInput, sizeof(stockInput), selectedStock.c_str(), _TRUNCATE);
                }

                DrawText("Buy Stock", 570, 120, 20, BLACK);
                DrawText("Symbol:", 570, 160, 20, BLACK);
                DrawText("Qty:", 570, 220, 20, BLACK);

                Rectangle stockBox = { 570, 190, 150, 30 };
                Rectangle qtyBox = { 570, 250, 150, 30 };
                Rectangle buyBtn = { 570, 300, 150, 40 };

                TextInputBox(stockBox, stockInput, 10, stockInputActive);
                TextInputBox(qtyBox, qtyInput, 10, qtyInputActive);

                if (Button("Buy", buyBtn)) {
                    int quantity = atoi(qtyInput);
                    if (quantity > 0 && market.buyStock(user, stockInput, quantity)) {
                        logger.logTransaction(user->getUsername(), "BUY", stockInput,
                            quantity, market.getPrice(stockInput));
                        memset(stockInput, 0, sizeof(stockInput));
                        memset(qtyInput, 0, sizeof(qtyInput));
                    }
                }
                break;
            }
            case 3: {
                static char stockInput[10] = { 0 };
                static char qtyInput[10] = { 0 };
                static bool stockInputActive = false;
                static bool qtyInputActive = false;

                if (!selectedStock.empty() && strlen(stockInput) == 0) {
                    strncpy_s(stockInput, sizeof(stockInput), selectedStock.c_str(), _TRUNCATE);
                }

                DrawText("Sell Stock", 570, 120, 20, BLACK);
                DrawText("Symbol:", 570, 160, 20, BLACK);
                DrawText("Qty:", 570, 220, 20, BLACK);

                Rectangle stockBox = { 570, 190, 150, 30 };
                Rectangle qtyBox = { 570, 250, 150, 30 };
                Rectangle sellBtn = { 570, 300, 150, 40 };

                TextInputBox(stockBox, stockInput, 10, stockInputActive);
                TextInputBox(qtyBox, qtyInput, 10, qtyInputActive);

                if (Button("Sell", sellBtn)) {
                    int quantity = atoi(qtyInput);
                    if (quantity > 0 && market.sellStock(user, stockInput, quantity)) {
                        logger.logTransaction(user->getUsername(), "SELL", stockInput,
                            quantity, market.getPrice(stockInput));
                        memset(stockInput, 0, sizeof(stockInput));
                        memset(qtyInput, 0, sizeof(qtyInput));
                    }
                }
                break;
            }
            case 4: {
                DrawText("Your Portfolio", 570, 120, 20, BLACK);

                const auto& portfolio = user->getPortfolio();
                if (portfolio.empty()) {
                    DrawText("No stocks owned", 570, 160, 20, GRAY);
                    break;
                }

                int yPos = 160;
                for (const auto& p : portfolio) {
                    stringstream ss;
                    double price = prices.count(p.first) ? prices.at(p.first) : 0.0;
                    double value = price * p.second;
                    ss << p.first << ": " << p.second << " shares\n$" << fixed << setprecision(2) << value;
                    DrawText(ss.str().c_str(), 570, yPos, 20, BLACK);
                    yPos += 50;
                }
                break;
            }
            case 5: {
                static char stockInput[10] = { 0 };
                static double smaPrediction = 0;
                static bool stockInputActive = false;

                if (!selectedStock.empty() && strlen(stockInput) == 0) {
                    strncpy_s(stockInput, sizeof(stockInput), selectedStock.c_str(), _TRUNCATE);
                }

                DrawText("Predictions", 570, 120, 20, BLACK);
                DrawText("Symbol:", 570, 160, 20, BLACK);

                Rectangle stockBox = { 570, 190, 150, 30 };
                Rectangle predictBtn = { 570, 230, 150, 30 };

                TextInputBox(stockBox, stockInput, 10, stockInputActive);

                if (Button("Predict", predictBtn)) {
                    smaPrediction = market.predictSMA(stockInput);
                }

                if (smaPrediction != 0) {
                    stringstream smaText;
                    smaText << "SMA Prediction: $" << fixed << setprecision(2) << smaPrediction;
                    DrawText(smaText.str().c_str(), 570, 280, 20, BLACK);
                }
                break;
            }
            }
        }

        EndDrawing();
    }

    CloseWindow();
    return 0;
}