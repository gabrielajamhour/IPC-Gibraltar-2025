# Interactive Navigation Tool — Nautical Charts of the Strait of Gibraltar

## Overview

A Java desktop application designed to support the study and practice of nautical-chart navigation in the Strait of Gibraltar.

The project translates real exam-style navigation tasks into interactive tools, with an emphasis on usability, problem-based learning, and practical interaction with the chart.

## Key Features

- **Drawing tools**: points, lines and arcs
- **Measurement tools**: distance measurement, ruler and protractor
- **Map interaction**: zoom and navigation
- **Problem-based learning**: navigation scenarios inspired by exam exercises
- **User management**: authentication and profiles
- **Session tracking**: usage history and completed sessions

## Technical Stack

- **Java 21**
- **JavaFX**
- **SQLite**
- **FXML / Scene Builder**
- **NetBeans**

## Architecture

The application separates the main UI and application responsibilities into several areas:

```text
src/
├── controllers/   # Application and screen controllers
├── views/         # FXML user interfaces
├── util/          # Navigation, drawing, measurement and session utilities
├── resources/     # Images and other application resources
└── styles/        # CSS stylesheets
```

The project uses JavaFX controllers to connect the FXML views with application behavior, while utility classes encapsulate reusable interaction and navigation logic.

## Running the Project

1. Clone the repository.
2. Open the project in NetBeans.
3. Configure a Java 21 JDK and the JavaFX libraries.
4. Build and run the application.

The project uses SQLite for local persistence. The local database file is intentionally **not tracked by Git**.

## Context

Developed as a team project for *Interfaces Persona-Computador* at Universitat Politècnica de València (UPV).

## Authors

- Gabriela Rego Jamhour
- Rafael Alonso Pellizzari

