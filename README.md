# Gerenciador Financeiro 💰

A modern Android financial management application built with Jetpack Compose and Clean Architecture principles.

## 📋 Overview

Gerenciador Financeiro is a comprehensive personal finance management app that helps users track their income, expenses, and financial goals. Built entirely with modern Android development tools and following best practices.

## ✨ Features

- **Transaction Management**: Track income and expenses with categories and tags
- **Financial Goals**: Set and monitor savings goals with progress tracking
- **Bank Account Integration**: Manage multiple bank accounts and track balances
- **Budget Planning**: Create monthly budgets and monitor spending
- **Analytics & Reports**: Visualize spending patterns with charts and summaries
- **Recurring Transactions**: Automate regular income and expenses
- **Multi-Category Support**: Organize transactions with customizable categories
- **Dark Mode**: Full dark theme support

## 🏗️ Architecture

This project follows **Clean Architecture** principles with clear separation of concerns:

```
app/
├── data/              # Data layer
│   ├── local/         # Room database, DAOs, entities
│   └── repository/    # Repository implementations
├── domain/            # Business logic layer
│   ├── model/         # Domain models
│   ├── repository/    # Repository interfaces
│   └── usecase/       # Business use cases
├── ui/                # Presentation layer
│   ├── components/    # Reusable UI components
│   ├── navigation/    # Navigation graph
│   ├── screens/       # Feature screens
│   ├── theme/         # Material Design theme
│   └── viewmodel/     # ViewModels
├── di/                # Dependency injection
└── util/              # Utilities and helpers
```

### Architecture Layers

- **Data Layer**: Manages data sources (Room database) and implements repositories
- **Domain Layer**: Contains business logic, use cases, and repository contracts
- **Presentation Layer**: Jetpack Compose UI with ViewModels following MVVM pattern

## 🛠️ Tech Stack

### Core
- **Kotlin** - Primary programming language
- **Jetpack Compose** - Modern declarative UI framework
- **Coroutines & Flow** - Asynchronous programming
- **Material Design 3** - UI components and theming

### Architecture Components
- **ViewModel** - UI state management
- **Room Database** - Local data persistence
- **Navigation Compose** - In-app navigation
- **Hilt** - Dependency injection

### Tools
- **KSP** - Kotlin Symbol Processing
- **Firebase Analytics** - Usage tracking
- **JUnit & Espresso** - Testing frameworks

## 📱 Requirements

- Android SDK 26 (Android 8.0) or higher
- Android Studio Hedgehog or newer
- Kotlin 2.0.21+
- Gradle 8.13.2+

## 🚀 Getting Started

### Clone the Repository

```bash
git clone https://github.com/yourusername/GerenciadorFinanceiro.git
cd GerenciadorFinanceiro
```

### Setup Firebase (Optional)

If you want to use Firebase Analytics:

1. Create a Firebase project at [Firebase Console](https://console.firebase.google.com/)
2. Download `google-services.json`
3. Place it in the `app/` directory

### Build and Run

```bash
# Make the script executable
chmod +x run-app.sh

# Run the app
./run-app.sh
```

Or use Android Studio:
1. Open the project in Android Studio
2. Sync Gradle dependencies
3. Run on emulator or physical device

## 📦 Project Structure

### Key Modules

- **Transaction Module**: Income/expense tracking with categories
- **Goal Module**: Financial goal setting and progress tracking
- **Account Module**: Bank account management
- **Budget Module**: Monthly budget planning
- **Analytics Module**: Reports and visualizations

### Database Schema

The app uses Room for local storage with the following main entities:
- `TransactionEntity` - Financial transactions
- `GoalEntity` - Savings goals
- `CategoryEntity` - Transaction categories
- `BankAccountEntity` - Bank accounts
- `BudgetEntity` - Budget plans
- `RecurringTransactionEntity` - Automated transactions

## 🧪 Testing

```bash
# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest
```

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Code Style

- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable and function names
- Write clean, self-documenting code
- Add comments for complex logic
- Ensure all tests pass before submitting PR

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgments

- Built with [Jetpack Compose](https://developer.android.com/jetpack/compose)
- Icons from [Material Icons](https://fonts.google.com/icons)
- Inspired by modern financial management principles

## 📧 Contact

For questions or suggestions, please open an issue on GitHub.

---

**Note**: This is an open-source project developed for educational and personal use. Feel free to fork, modify, and use it for your own needs.

