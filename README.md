# WhatsNearMe 🗺️📍

> **Note:** This application was developed as a university project for the "Mobile Application Development" course during the Summer of 2024.

**WhatsNearMe** is a location-based Android application designed for tourists and explorers. It helps users discover points of interest (POIs) around them using an interactive Google Map. With customizable search radiuses and category filters, users can easily find nearby restaurants, museums, and more. Beyond discovery, the app allows users to share locations offline via Bluetooth and keep a personalized "Travel Diary" of visited places, complete with photos taken directly from within the app.

---

## ✨ Features

* **Interactive Map & POI Discovery:** View your current location and discover nearby points of interest mapped in real-time.
* **Customizable Filters:** Filter searches by specific categories (e.g., museums, restaurants) and adjust the discovery radius.
* **Rich Place Details:** Tap on any map marker to view a detailed card containing place information and quick actions.
* **One-Tap Navigation:** Easily launch Google Maps (or your preferred navigation app) to get directions to a selected POI.
* **Peer-to-Peer Bluetooth Sharing:** Share places with nearby friends and devices entirely offline using Bluetooth PAN connections.
* **Personal Travel Diary:** Mark interesting locations as "visited". These are saved persistently in a local database for future memory.
* **Integrated Camera:** Capture photos of your visited locations using the built-in CameraX integration, automatically attaching them to your Travel Diary entries.

---

## 🛠️ Tech Stack

* **Language:** Java
* **Minimum SDK:** 33 (Android 13)
* **Target SDK:** 34 (Android 14)
* **Core APIs & SDKs:**
    * **Google Maps SDK for Android:** For map rendering and user location tracking.
    * **Google Places API:** Queried via REST/HTTP and parsed with **Gson** for nearby location discovery.
    * **Android CameraX:** High-level API used for reliable, in-app photo capture (`camera-core`, `camera2`, `lifecycle`, `view`).
    * **Android Bluetooth APIs:** Used for device discovery, pairing, and peer-to-peer data transmission.
    * **SQLite (`SQLiteOpenHelper`):** Native Android relational database management for the offline Travel Diary.
* **UI/UX:** AndroidX Fragments, Material Design Components, ConstraintLayout.

---

## 🏗️ Architecture Overview

The project relies on a standard **MVC (Model-View-Controller)** approach, which is typical for native Android Java applications.

* **Model (`entities/`, `utils/DatabaseHelper.java`):** Represents the data layer. `MyPlace` acts as the core entity. Data is stored locally via standard SQLite queries and schema management.
* **View (`ui/`, XML Layouts):** Material Design XML layouts and dialogs bound to Activity and Fragment lifecycles.
* **Controller (`Activities`, `Fragments`, `AsyncTask`):** Activities (like `MainActivity`, `CameraActivity`) and Fragments (`MapsFragment`, `DiarioFragment`) act as controllers, orchestrating data flow between the UI, the SQLite database, and asynchronous network tasks (e.g., `FetchPlaces`).

---

## 🚀 Getting Started

Follow these instructions to build and run the project on your local machine.

### Prerequisites
* **Android Studio** (Koala or newer recommended).
* A physical Android device running **Android 13 (API 33)** or higher is highly recommended to test Camera and Bluetooth functionalities (emulators have limited support for these hardware features).
* A **Google Cloud Console** account with the **Maps SDK for Android** and **Places API (New)** enabled.

### 1. Clone the Repository
```bash
git clone https://github.com/andrefilii/WhatsNearMe.git
cd WhatsNearMe
```

### 2. Configure API Keys
This project uses the `secrets-gradle-plugin` to keep API keys secure. You need to provide your own Google Maps API Key to compile and run the app.

1. Open the project root directory.
2. Create or locate the `secrets.properties` file.
3. Add your Google Maps API key to the file:
```properties
MAPS_API_KEY=your_actual_google_api_key_here
```
*(Note: Do not commit your API key to version control).*

### 3. Build and Run
1. Open the project in **Android Studio**.
2. Allow Gradle to sync and download all necessary dependencies (as defined in `libs.versions.toml` and `build.gradle.kts`).
3. Select your target device or emulator.
4. Click **Run** (Shift + F10) to compile and install the app.

---

## 📄 License
This project is open-source and available under the terms of the MIT License. See the [LICENSE](LICENSE) file for more details.
