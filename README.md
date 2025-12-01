# Sampah Jujur

A waste collection marketplace Android application that connects households who want to sell recyclable waste with collectors who purchase recyclable materials.

## Team Members

- **Melvin Waluyo** (22/492978/TK/53972)
- **Alexander Johan Pramudito** (22/492990/TK/53976)
- **Muhammad Grandiv Lava Putra** (22/493242/TK/54023)

## Overview

Sampah Jujur is a mobile marketplace platform built with modern Android development practices. The app facilitates the circular economy by making it easy for households to sell their recyclable waste to registered collectors, promoting environmental sustainability through proper waste management.

### Key Features

- **Dual User Roles**: Separate interfaces for households and waste collectors
- **Pickup Request System**: Households can request waste pickups with detailed waste categorization
- **Real-time Notifications**: Push notifications for request updates and status changes
- **Chat System**: In-app messaging between households and collectors
- **Location Services**: Map integration for pickup location selection and routing
- **Image Upload**: Cloudinary integration for waste item photos
- **Request Management**: Track request status from creation to completion
- **User Profiles**: Manage personal information and view transaction history

## Technology Stack

### Core Technologies

- **Kotlin** - Primary programming language
- **Jetpack Compose** - Modern declarative UI framework
- **Material Design 3** - UI design system
- **MVVM Architecture** - Clean separation of concerns

### Firebase Services

- **Firebase Authentication** - User authentication and management
- **Cloud Firestore** - Real-time NoSQL database
- **Firebase Cloud Messaging** - Push notifications
- **Firebase Analytics** - Usage analytics

### Key Libraries

- **Hilt** - Dependency injection
- **Jetpack Navigation** - Navigation component for Compose
- **Coil** - Image loading
- **Google Maps SDK** - Location and mapping features
- **Cloudinary** - Cloud-based image storage and management
- **Room** - Local database for offline support
- **Retrofit** - HTTP client for API calls
- **Coroutines & Flow** - Asynchronous programming

## Prerequisites

- **Android Studio**: Ladybug (2024.2.1) or later
- **JDK**: 17 or higher
- **Android SDK**: API 24 (Android 7.0) minimum, API 35 target
- **Google Services**: Firebase project setup required

## Project Setup

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/sampah-jujur.git
cd sampah-jujur
```

### 2. Firebase Configuration

1. Create a Firebase project at [Firebase Console](https://console.firebase.google.com)
2. Register your Android app with package name: `com.melodi.sampahjujur`
3. Download `google-services.json` and place it in the `app/` directory
4. Enable the following Firebase services:
   - Authentication (Email/Password)
   - Cloud Firestore
   - Cloud Messaging

### 3. Local Configuration

Create a `local.properties` file in the project root with the following configuration:

```properties
# Android SDK location
sdk.dir=/path/to/Android/sdk

# Cloudinary Configuration
cloudinary.cloud.name=your_cloud_name
cloudinary.api.key=your_api_key
cloudinary.api.secret=your_api_secret
cloudinary.upload.folder=sampah-jujur

# Notification Server
notification.server.url=http://your-server-url:3000
```

### 4. Build the Project

#### Using Command Line (Windows)

```bash
# Clean build
gradlew.bat clean

# Build debug APK
gradlew.bat assembleDebug

# Install on connected device
gradlew.bat installDebug
```

#### Using Command Line (Linux/Mac)

```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug
```

#### Using Android Studio

1. Open the project in Android Studio
2. Let Gradle sync complete
3. Click **Run** or press `Shift + F10`

## Project Structure

```
app/src/main/java/com/melodi/sampahjujur/
├── di/                          # Dependency Injection
│   ├── AppModule.kt            # App-level dependencies
│   ├── FirebaseModule.kt       # Firebase instances
│   └── DatabaseModule.kt       # Room database
│
├── model/                       # Data Models
│   ├── User.kt                 # User model
│   ├── PickupRequest.kt        # Pickup request model
│   ├── WasteItem.kt            # Waste item model
│   ├── ChatMessage.kt          # Chat message model
│   └── ...
│
├── repository/                  # Data Layer
│   ├── AuthRepository.kt       # Authentication operations
│   ├── RequestRepository.kt    # Request management
│   ├── UserRepository.kt       # User data operations
│   ├── ChatRepository.kt       # Chat operations
│   └── ...
│
├── viewmodel/                   # Business Logic
│   ├── AuthViewModel.kt        # Authentication state
│   ├── HouseholdViewModel.kt   # Household operations
│   ├── CollectorViewModel.kt   # Collector operations
│   ├── ChatViewModel.kt        # Chat functionality
│   └── ...
│
├── ui/
│   ├── screens/                # Screen Composables
│   │   ├── auth/              # Login, Register
│   │   ├── household/         # Household user screens
│   │   ├── collector/         # Collector user screens
│   │   └── shared/            # Shared screens (Settings, Profile)
│   │
│   ├── components/             # Reusable UI Components
│   │   ├── WasteItemCard.kt
│   │   ├── RequestCard.kt
│   │   └── ...
│   │
│   └── theme/                  # Material3 Theme
│       ├── Color.kt
│       ├── Type.kt
│       └── Theme.kt
│
├── navigation/                  # Navigation
│   └── NavGraph.kt             # Navigation routes & graph
│
├── service/                     # Background Services
│   ├── FirebaseMessagingService.kt
│   └── ...
│
├── utils/                       # Utilities
│   ├── CloudinaryHelper.kt
│   ├── LocationHelper.kt
│   └── ...
│
├── data/                        # Local Database
│   ├── dao/                    # Room DAOs
│   ├── entity/                 # Room entities
│   └── AppDatabase.kt          # Database instance
│
├── MainActivity.kt              # Main Activity
└── SampahJujurApplication.kt   # Application Class
```

## Architecture

The app follows **MVVM (Model-View-ViewModel)** with **Clean Architecture** principles:

```
┌─────────────────────────────────────┐
│      UI Layer (Compose)             │
│  - Screens (Composables)            │
│  - UI Components                     │
└──────────────┬──────────────────────┘
               │ observes StateFlow
┌──────────────▼──────────────────────┐
│      ViewModel Layer                │
│  - Business Logic                    │
│  - State Management                  │
└──────────────┬──────────────────────┘
               │ uses
┌──────────────▼──────────────────────┐
│      Repository Layer               │
│  - Data Abstraction                  │
│  - Single Source of Truth            │
└──────────────┬──────────────────────┘
               │ accesses
┌──────────────▼──────────────────────┐
│      Data Sources                   │
│  - Firebase (Remote)                 │
│  - Room Database (Local)             │
└─────────────────────────────────────┘
```

### Key Principles

1. **Separation of Concerns**: Each layer has a single, well-defined responsibility
2. **Dependency Injection**: Hilt manages dependencies throughout the app
3. **Unidirectional Data Flow**: UI observes state from ViewModels, sends events back
4. **Reactive Programming**: StateFlow and Coroutines for async operations
5. **Offline-First**: Room database provides offline capability

## User Flows

### Household User Flow

1. **Registration** → Select "Household" role
2. **Create Request** → Add waste items with photos and quantities
3. **Select Location** → Choose pickup location on map
4. **Submit Request** → Request goes to "Pending" status
5. **Collector Accepts** → Receive notification, status becomes "Accepted"
6. **Chat** → Communicate with collector if needed
7. **Pickup Completed** → Collector marks as complete
8. **Rate & Review** → Provide feedback

### Collector User Flow

1. **Registration** → Select "Collector" role
2. **Browse Requests** → View all pending pickup requests
3. **View Details** → See waste items, location, and estimated value
4. **Accept Request** → Commit to pickup
5. **Navigate** → Use built-in routing to pickup location
6. **Chat** → Communicate with household
7. **Complete Pickup** → Mark request as completed
8. **Get Rated** → Receive rating from household

## Testing

### Run Unit Tests

```bash
# Run all unit tests
gradlew.bat test

# Run unit tests with coverage
gradlew.bat testDebugUnitTest

# Run specific test class
gradlew.bat test --tests "com.melodi.sampahjujur.YourTestClass"
```

### Run Instrumented Tests

```bash
# Requires connected device or emulator
gradlew.bat connectedAndroidTest
```

### Lint Checking

```bash
gradlew.bat lint
```

## Build Variants

### Debug Build

```bash
gradlew.bat assembleDebug
```

- Includes debugging symbols
- Connects to development Firebase project
- Allows debugging via USB

### Release Build

```bash
gradlew.bat assembleRelease
```

- Optimized and minified
- Requires signing configuration
- Ready for Play Store deployment

## Firebase Security Rules

Ensure proper Firestore security rules are configured in Firebase Console:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // User documents
    match /users/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth.uid == userId;
    }

    // Pickup requests
    match /pickupRequests/{requestId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null;
      allow update: if request.auth != null &&
        (resource.data.householdId == request.auth.uid ||
         resource.data.collectorId == request.auth.uid);
    }

    // Chat messages
    match /chats/{chatId}/messages/{messageId} {
      allow read, write: if request.auth != null;
    }
  }
}
```

## Common Issues & Troubleshooting

### Build Failures

**Issue**: `google-services.json not found`
- **Solution**: Download from Firebase Console and place in `app/` directory

**Issue**: `Cloudinary credentials not found`
- **Solution**: Add Cloudinary configuration to `local.properties`

### Runtime Issues

**Issue**: Firebase Authentication fails
- **Solution**: Check Firebase project configuration and enable Email/Password auth

**Issue**: Maps not showing
- **Solution**: Ensure Google Maps API key is configured in Firebase

**Issue**: Images not uploading
- **Solution**: Verify Cloudinary credentials in `local.properties`

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is developed as part of an academic assignment.

## Contact

For questions or issues, please contact the development team:

- Melvin Waluyo - [GitHub Profile]
- Alexander Johan Pramudito - [GitHub Profile]
- Muhammad Grandiv Lava Putra - [GitHub Profile]

## Acknowledgments

- Jetpack Compose documentation and community
- Firebase documentation
- Material Design 3 guidelines
- Android development community
