# Logistic Project

A portfolio project designed to solve a real operational problem: planning and simulating last-mile delivery with business constraints such as weight, volume, capacity, and route feasibility.

This repository is intentionally focused on software engineering quality, domain modeling, and optimization-oriented thinking.


## Run Via CLI

Prerequisites:

- Java 21+
- Maven 3.9+

Commands:

```bash
mvn clean compile
mvn exec:java
```

The CLI will ask for vehicle data, origin coordinates, and package data. It then:

- validates weight and volume constraints,
- rejects invalid loads with clear reasons,
- and prints dispatch order using express-first priority and Euclidean distance.