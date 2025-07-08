# 🌤️ WeatherAPP – Interactive Weather App with Map

[![Platform](https://img.shields.io/badge/platform-Android-green)](https://developer.android.com)
[![Language](https://img.shields.io/badge/language-Kotlin-blue)](https://kotlinlang.org/)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-purple)](https://developer.android.com/jetpack/compose)
[![API](https://img.shields.io/badge/API-Open--Meteo-lightgrey)](https://open-meteo.com/)
[![Map](https://img.shields.io/badge/map-OpenStreetMap-orange)](https://www.openstreetmap.org/)

**WeatherAPP** is an interactive weather application that allows users to select any point on the map to retrieve accurate weather data using the [Open-Meteo API](https://open-meteo.com/).  
Built with **Jetpack Compose** and **OpenStreetMap**, the app offers a smooth, elegant interface designed for simplicity and responsiveness.

---

## 🚀 Features

### 📍 User location & recentering

- Automatically centers the map on the user's current location.
- Dedicated recenter button with a **visual indicator** showing whether tracking is active or not.

---

### 🗺️ Advanced map interaction

- Full gesture support:  
  - **Scroll**,  
  - **Pinch to zoom**,  
  - **Two-finger rotation**,  
  - **Long press** to select a point.
- Adds a **custom marker** on long-press location.
- **Haptic feedback** (vibration) on long press.

---

### 🌐 Real-time weather with Open-Meteo

- Requests accurate weather data based on the **latitude and longitude** of the selected point.

---

### ⚠️ Position confirmation

- A confirmation **alert dialog** appears after long-press to avoid accidental selections.

---

### 🧭 Detailed weather screen

- Main card for current weather:
  - Weather **icon** and **description** (sunny, cloudy, rainy, etc.)
  - **Min / Current / Max** temperature
  - **Humidity** and **wind speed**
  - Timestamp of the closest available forecast
- Horizontal scrollable forecast:
  - Hourly weather cards for each day of the current week.

### 🌗 Light & Dark theme support

- Fully supports **system theme** (light/dark) for a consistent and pleasant experience at any time of day.

---

## 🎨 Design & Graphics

- The **app icon** and all **weather icons** used in the interface were **custom-designed by me** with attention to consistency, clarity, and visual appeal.

---

## 🛠️ Tech Stack

- **Kotlin**
- **Jetpack Compose**
- **OSMDroid**
- **Open-Meteo API**
- **Material Design 3**
- ViewModel + State Management
- Navigation Compose

---

## ▶️ Getting Started

1. **Clone the repository**:
   ```bash
   git clone https://github.com/Bobotico/WeatherApp.git
   cd WeatherAPP
Open the project in Android Studio (Giraffe or newer).

Build and run the app on an emulator or physical Android device.

## 📸 Screenshots
![1](https://github.com/user-attachments/assets/3f714d2e-211d-4dbc-9336-8ad03005862f)
![2](https://github.com/user-attachments/assets/df329bac-b24d-497d-84a5-d733ca79f17b)
![3](https://github.com/user-attachments/assets/1018a8bd-298e-4bc6-9f6b-0f06869370d4)
![4](https://github.com/user-attachments/assets/29e1d38d-9981-457e-859b-87eca023128b)
![5](https://github.com/user-attachments/assets/28a94dae-6332-470e-875a-134d3b798d92)
![6](https://github.com/user-attachments/assets/18d74e9c-4a74-4538-970c-fd1165d58dc2)
![7](https://github.com/user-attachments/assets/2601f726-0392-4449-8dcd-63a6ac8db8f7)
![8](https://github.com/user-attachments/assets/5c0feb4c-eeeb-45d3-ab0a-acb26dcd597e)
![9](https://github.com/user-attachments/assets/71e48270-2e1d-4d37-be88-2aed6e36e73b)
![10](https://github.com/user-attachments/assets/6114676e-9e5f-46bf-91c8-c49bfc0e7f9c)
![11](https://github.com/user-attachments/assets/2f4adee2-0b63-46a6-becd-be1500979c73)

## 🤝 Contributions
Feel free to open issues or submit pull requests!
Suggestions, improvements, and contributions are always welcome.

## 📜 License
This project is licensed under the MIT License.
See the LICENSE file for details.

## 📬 Contact
Made with ❤️ by Bobotico
