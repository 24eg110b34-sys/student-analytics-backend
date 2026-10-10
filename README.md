# Student Performance Analytics and Prediction Dashboard: Backend (Spring Boot + MySQL)

## Requirements
- JDK 17 or 21 (Java 26 is not supported by Spring Boot 3.2)
- Maven 3.8+
- MySQL 8 running locally

## Run locally
1. Start MySQL. The database `student_analytics` is created automatically.
2. Check the MySQL login in `src/main/resources/application.properties`
   (currently `root` / `ram1122`), or set `DB_USERNAME` and `DB_PASSWORD`.
   Do not push a real password to a public GitHub repo.
3. In this folder run:
   ```
   mvn clean spring-boot:run
   ```
   Keep the project outside OneDrive. Syncing can corrupt Maven builds.
4. Open http://localhost:8080/api/health  ->  `{"status":"ok"}`

On the first start the app creates the tables and loads demo data:
24 students, 6 semesters of subjects, and 4 semesters of marks, attendance and study hours.

## Demo logins
| Role | Email | Password |
|---|---|---|
| Faculty | `faculty@demo.com` | `faculty123` |
| Student | `aarav@demo.com` (also `diya@`, `rohan@`, ... `@demo.com`) | `student123` |

New students added by faculty get the password `student123` unless faculty sets another one.

## How the numbers are calculated
- Subject total = 20% assignment + 30% internal + 50% exam.
- Grade points (10-point scale): 90+ = 10, 80+ = 9, 70+ = 8, 60+ = 7, 50+ = 6, 40+ = 5, below 40 = 0 (fail).
- SGPA = credit-weighted grade points of one semester. CGPA = credit-weighted over all semesters so far.
- Attendance % = classes attended / classes held.

## Prediction module
`PredictionService` trains a multiple linear regression on every semester in the database:

`SGPA = b0 + b1 x study hours + b2 x attendance + b3 x assignment average + b4 x previous CGPA`

The coefficients are solved with the normal equations in plain Java (no ML library). The model is retrained on each
request, so it improves as faculty add data. With fewer than 12 training rows it uses a fixed heuristic instead.
Risk levels: HIGH (predicted SGPA below 5.5 or attendance below 65%), MEDIUM (below 7.0 or attendance below 75%), otherwise LOW.

## API
| Method | Path | Who | Description |
|---|---|---|---|
| POST | `/api/auth/login` | all | `{email, password}` -> `{token, user}` |
| GET | `/api/me/dashboard` | student | own dashboard, charts data and prediction |
| POST | `/api/predict` | any user | what-if prediction `{studyHours, attendance, assignmentScore, previousCgpa}` |
| GET | `/api/subjects` | any user | all subjects |
| GET | `/api/faculty/overview` | faculty | class-wide statistics |
| GET / POST | `/api/faculty/students` | faculty | list (`?q=` search) / add student |
| DELETE | `/api/faculty/students/{id}` | faculty | remove a student |
| GET | `/api/faculty/students/{id}/dashboard` | faculty | one student's dashboard |
| PUT | `/api/faculty/students/{id}/marks` | faculty | `{subjectId, assignment, internal, exam}` |
| PUT | `/api/faculty/students/{id}/attendance` | faculty | `{subjectId, held, attended}` |
| PUT | `/api/faculty/students/{id}/study-hours` | faculty | `{semester, hoursPerWeek}` |
| POST | `/api/faculty/subjects` | faculty | `{code, name, credits, semester}` |

Send `Authorization: Bearer <token>` on every request except login and health.

## Settings (environment variables)
`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` (32+ characters), `CORS_ORIGINS` (frontend URLs, comma separated),
`SEED_DEMO_DATA` (`false` to start empty), `PORT`.

## Deploy on Render
Render has no managed MySQL, so create a MySQL database elsewhere (Aiven, Railway, TiDB Cloud and similar).
1. Push this folder to GitHub and create a **Web Service** on Render with **Runtime: Docker** (uses the `Dockerfile`).
2. Set `DB_URL` (for example `jdbc:mysql://HOST:PORT/DBNAME?useSSL=true`), `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`
   and `CORS_ORIGINS` (your Vercel URL).
3. Set the health check path to `/api/health`.
