# Exam Management System

## Project Overview

The Exam Management System is a desktop application developed using Java Swing. It allows administrators to manage students, examinations, questions, and results. Students can log in, attend timed examinations, and view their results.

The application uses local text files for data storage without requiring an external database.

## Features

### Admin Module
- Admin login
- Dashboard with examination statistics
- Add, edit, delete, and search students
- Create and manage examinations
- Add, edit, and delete questions
- View student examination results
- View performance statistics
- Change admin password
- Reset application data

### Student Module
- Student login
- View available examinations
- Attend multiple-choice examinations
- Countdown timer
- Automatic submission when time expires
- Automatic answer evaluation
- View examination results
- Change student password

## Technologies Used

- Java
- Java Swing
- Java AWT
- Java Collections
- Java File Handling
- Java NIO
- LocalDateTime

## Requirements

- JDK 8 or above
- VS Code or IntelliJ IDEA
- Windows, Linux, or macOS

## Installation and Execution

**1. Clone the repository**

```bash
git clone https://github.com/YOUR-USERNAME/Exam-Management-System.git
```

**2. Open the project folder**

```bash
cd Exam-Management-System
```

**3. Compile the program**

```bash
javac ExamManagementGUI.java
```

**4. Run the application**

```bash
java ExamManagementGUI
```

## Default Login Credentials

**Admin**

- Username: `admin`
- Password: `admin123`

**Students**

| Student ID | Password |
|---|---|
| S101 | pass101 |
| S102 | pass102 |
| S103 | pass103 |

## Project Structure

```text
Exam-Management-System/
│
├── ExamManagementGUI.java
├── README.md
│
└── data/
    ├── admin.txt
    ├── students.txt
    ├── exams.txt
    ├── questions.txt
    └── results.txt
```

The `data` folder and text files are automatically created when the application runs for the first time.

## Data Storage

The application uses text files to store information.

- `admin.txt` – Admin login details
- `students.txt` – Student information
- `exams.txt` – Examination details
- `questions.txt` – Questions and answers
- `results.txt` – Examination results

All changes are saved automatically and remain available after restarting the application.

## Future Enhancements

- MySQL database integration
- Password encryption
- PDF result generation
- Excel report generation
- Advanced performance analysis
- Web-based examination access

## Project Information

- Project Name: Exam Management System
- Language: Java
- GUI: Java Swing
- Application Type: Desktop Application
- Database: File-Based Storage

- Author
- Suntharavelu S
