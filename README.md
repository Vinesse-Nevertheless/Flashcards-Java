# Java Flashcard Management System (Advanced CLI)

A sophisticated command-line educational tool that manages complex data relationships and provides a robust interface for active recall study. This project serves as a showcase for intermediate Java architecture, specifically focusing on I/O redirection, custom logging, and data persistence.

## 🛠 Technical Architecture & Strengths

Unlike standard student projects, this implementation prioritizes **decoupling** and **extensibility**. Key architectural choices include:

### 1. Non-Invasive Session Logging
Implemented a custom `DualPrintStreamLogger` by extending the Java `PrintStream` class. This allows the application to "hijack" `System.out` to record the entire console session to a file without modifying any of the underlying business logic. This demonstrates an understanding of the Java Class Hierarchy and I/O streams.

### 2. Dependency Injection
To avoid the "Static Trap," this project utilizes **Constructor-based Dependency Injection**. By passing the Logger and UserInput instances as dependencies, the code is significantly more modular and prepared for Unit Testing—a critical requirement for production-grade software.

### 3. Command-Line Interface (CLI) Robustness
The application features a professional-grade argument parser that supports `-import` and `-export` flags. It is built with defensive programming logic to handle:
* **Orphan Flags:** Gracefully handles flags missing associated filenames.
* **Convention Priority:** Implements "Last-One-Wins" logic for duplicate flags.
* **Lifecycle Persistence:** Automatically handles data loading at startup and synchronization at shutdown.

### 4. Advanced Collection Management
The system uses a variety of Java Collections to handle specific data needs:
* **LinkedHashMap:** Maintains the user's defined card order.
* **Deque (ArrayDeque):** Implements logic to track and report ties for the "Hardest Card" statistics accurately.
* **ConcurrentHashMap:** Utilized during file-walking for thread-safe data merging.

## 🚀 Key Features
* **Statistics Tracking:** Monitors errors per card to identify learning gaps.
* **Flexible Persistence:** Import and export card decks with associated statistics in a custom delimited format.
* **Comprehensive Logging:** Export a full transcript of the session for later review.
* **Smart Feedback:** Provides contextual corrections (e.g., notifying the user if their "wrong" answer is actually the correct definition for a different card in the deck).

## 💻 Tech Stack
* **Language:** Java 17+
* **I/O:** NIO.2 (`java.nio.file`), Custom Stream redirection.
* **Design Concepts:** Simplified Command Pattern, Dependency Injection, SRP (Single Responsibility Principle).

## 🔧 Installation & Usage
Compile all classes:
```bash
javac flashcards/*.java
```

Run the application:
```bash
java flashcards.Main
```

Run with automated file sync:
```bash
java flashcards.Main -import my_deck.txt -export backup.txt
```
