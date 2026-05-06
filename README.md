# ThesisConnect

ThesisConnect is a thesis management and collaboration platform built for students who need a simple way to find teammates, form thesis groups, manage shared work, and stay in touch throughout the research process.

The idea behind the project was to bring the most common parts of thesis work into one place instead of splitting everything across messaging apps, shared drives, and manual coordination. A student can create a profile, discover other students, join or manage a thesis group, share thesis documents, comment on files, and communicate directly with teammates.



## Project background

The project proposal was structured around background, scope, rationale, objectives, approach, requirements, and conclusion. That same flow can still be seen in the completed application.

The background of the project came from a common problem students face during thesis work. Communication, file sharing, member coordination, and progress tracking often happen across too many different places. That makes even simple work harder to organize. ThesisConnect was built to reduce that confusion by putting the main thesis-related activities inside one system.

In terms of scope, the project stays focused on the student side of thesis management. It is mainly concerned with how students present themselves, find collaborators, manage group activity, exchange messages, and handle thesis documents in a more organized way.

The main objective was to develop a web-based system where students could:

- build academic profiles
- discover potential thesis partners
- create and manage thesis groups
- exchange messages and updates
- upload, review, and manage thesis documents

The project approach was practical rather than overly broad. Instead of trying to build a full institutional management platform, the focus was kept on the parts students would actually use during thesis collaboration: group formation, communication, notifications, and document sharing.

## What the project does

The system supports the full flow from student discovery to ongoing thesis collaboration.

- User registration and login with JWT-based authentication
- Student profile management, including research interests, skills, academic details, bio, and profile picture
- Student discovery and filtering to find possible thesis partners
- Thesis group creation and management
- Join requests and invitations for group membership
- Admin assignment and group moderation
- Thesis document upload and download
- Document version tracking
- Comments and feedback on shared thesis documents
- Notifications for important actions
- Direct messaging between users
- Group messaging inside thesis groups
- Message pinning for both direct and group conversations

## Main features

The implemented requirements eventually grew into a more complete collaboration system, so the final version covers both management and communication needs inside the same platform.

### 1. User accounts and profiles

Each user can create an account, log in securely, and maintain a profile that includes academic and research-related information. The profile section is meant to help students present themselves clearly before joining a group.

### 2. Student discovery

Students can browse and search for other students using profile information such as name, email, department, university, research interest, and whether someone is currently looking for a group.

### 3. Thesis groups

Users can create thesis groups, send join requests, invite other students, approve or reject requests, and manage group membership. Group admins can also assign other admins when needed.

### 4. Document collaboration

Group members can upload thesis-related files such as drafts, reports, and research material. Uploaded documents support:

- visibility control
- download access
- version history
- updated file uploads
- comments and feedback

### 5. Notifications

The application keeps users informed when something important happens, such as invitations, approvals, rejections, or other group-related actions.

### 6. Messaging

The project also includes communication features so users do not need to leave the platform just to coordinate. There is support for:

- direct one-to-one messaging
- group chat inside thesis groups
- pinning important messages

## Tech stack

- Java 21
- Spring Boot 4
- Spring Security
- JWT authentication
- Spring JDBC
- MySQL
- HTML, CSS, and JSX-based frontend pages served from Spring Boot static resources
- Maven

## Project structure

The project is mainly organized into the following parts:

- `src/main/java/com/example/ThesisConnect/config`
  Security, JWT, and web configuration
- `src/main/java/com/example/ThesisConnect/domain`
  Core domain models such as users, thesis groups, documents, comments, and messages
- `src/main/java/com/example/ThesisConnect/dto`
  Request and response objects used by the API
- `src/main/java/com/example/ThesisConnect/repository`
  JDBC-based data access layer
- `src/main/java/com/example/ThesisConnect/service`
  Business logic for authentication, profiles, groups, documents, and messaging
- `src/main/java/com/example/ThesisConnect/web`
  Controllers for API endpoints and page routing
- `src/main/resources/static`
  Frontend pages and assets
- `src/main/resources/schema.sql`
  Database schema used by the application
- `uploads/`
  Uploaded profile pictures and thesis documents

## Database

The application is configured to use MySQL for local development.

Current MySQL configuration:

- database name: `thesisconnect`
- default URL: `jdbc:mysql://localhost:3306/thesisconnect`
- username: `root`

The schema is created from `src/main/resources/schema.sql`.

Main tables include:

- `users`
- `user_research_interests`
- `user_skills`
- `thesis_groups`
- `group_members`
- `join_requests`
- `notifications`
- `documents`
- `document_versions`
- `comments`
- `direct_messages`
- `group_messages`
- `direct_message_pins`
- `group_message_pins`

## Running the project

### Prerequisites

Before running the project, make sure you have:

- Java 21 installed
- Maven installed, or use the included Maven wrapper
- MySQL running locally
- a MySQL database named `thesisconnect`

### Configuration

Check the following file if you need to change the database connection:

- `src/main/resources/application-mysql.properties`

If needed, update:

- MySQL username
- MySQL password
- MySQL port

### Run with Maven wrapper

On Windows:

```bash
mvnw.cmd spring-boot:run
```

Or with Maven:

```bash
mvn spring-boot:run
```

After the application starts, open it in your browser through the local Spring Boot server.



## Notes

- Uploaded profile pictures are stored in `uploads/profile-pictures`
- Uploaded thesis documents are stored in `uploads/thesis-documents`
- File upload limits are configured in `application.properties`
- JWT settings are also configured in `application.properties`

## Why this project matters

Most thesis work is collaborative, but the tools students use are often scattered. ThesisConnect was built to make that process more organized. Instead of only focusing on one part, like messaging or file upload, the project tries to support the full student workflow from finding collaborators to managing actual thesis materials.

## Final remark

In the end, this project grew from a proposal idea into a complete working thesis management website. It reflects both the technical side of building a full-stack application and the practical side of solving a real student coordination problem in a way that is actually useful for day-to-day thesis work.
