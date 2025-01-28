# QTeam Solutions

## Overview

QTeam Solutions is a Java-based project

## Features

- List contents of an S3 bucket
- Retrieve metadata of S3 objects
- Download S3 objects as files

## Prerequisites

- Java 21 or higher
- Maven

## Setup

1. Clone the repository:
    ```sh
    git clone https://github.com/CosminZns/qteam.solutions.git
    cd qteam.solutions
    ```

2. Build the project using Maven:
    ```sh
    mvn clean install
    ```

## Running the Application

To run the application, execute the `Main` class:
```sh
mvn exec:java -Dexec.mainClass="be.everesst.socialriskdeclaration.Main"
