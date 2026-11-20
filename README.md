# 🌟 EduNova: Offline-First AI Educational Platform & Adaptive Tutor

[![Kotlin Version](https://img.shields.io/badge/Kotlin-2.0+-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4.svg?logo=android&logoColor=white)](https://developer.android.com/jetpack/compose)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.115+-009688.svg?logo=fastapi&logoColor=white)](https://fastapi.tiangolo.com/)
[![Android API](https://img.shields.io/badge/Android%20API-24%20to%2037-3DDC84.svg?logo=android&logoColor=white)](https://developer.android.com/)
[![Inference Engine](https://img.shields.io/badge/llama.cpp-Edge%20Native%20C++-blue.svg)](https://github.com/ggerganov/llama.cpp)
[![Supabase pgvector](https://img.shields.io/badge/Database-Supabase%20pgvector-3ECF8E.svg?logo=supabase&logoColor=white)](https://supabase.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

> **"Learn anything, anywhere, even with zero internet."**  
> EduNova is an enterprise-grade mobile educational platform and AI tutor for Android, backed by an asynchronous Python cloud service (FastAPI). When connected, it harnesses Google Gemini cloud intelligence with verified CBSE curriculum retrieval; when completely disconnected, it runs **100% private, on-device AI models (`llama.cpp` + Qwen 2.5)** directly on phone hardware.

---

## 🚀 Quick Setup Guide

> [!TIP]
> **New to EduNova or non-technical?** Choose **Option A (Quick Start)** below! It connects the mobile app directly to the live cloud backend, so you don't have to install Python, Docker, or databases. If you are a developer looking to modify the cloud backend, follow **Option B**.

```mermaid
flowchart LR
    A["Choose Your Path"] --> B["Option A: Quick Start (3 Mins)<br/>Connect to live cloud backend"]
    A --> C["Option B: Full Developer Setup<br/>Run local FastAPI + Android Studio"]
    B --> D["Install & Launch APK<br/>Ready to learn!"]
    C --> E["Start Python Backend (:8000)<br/>+ Compile Android App"]
    E --> D
```

---

### 📋 Prerequisites Checklist

Before you begin, ensure you have the following installed on your computer:

| Requirement | Minimum Version | Recommended For | Download Link |
|---|---|---|---|
| **Android Studio** | Ladybug / Jellyfish (2024.1+) | App Compilation & Testing | [Download Android Studio](https://developer.android.com/studio) |
| **Java JDK** | JDK 17 or JDK 21 | Android Gradle Builds | Included with Android Studio (JBR) |
| **Android NDK** | 27.2.12479018 | Native C++ (`llama.cpp`) builds | Available via Android Studio SDK Manager |
| **Python** (for Backend) | Python 3.11+ | Cloud API & Model Scripts | [Download Python](https://www.python.org/downloads/) |
| **Git** | Latest | Cloning repository | [Download Git](https://git-scm.com/) |
| **Device / Emulator** | Android 7.0+ (API 24+) | Running EduNova App | Physical device or Pixel 8 Emulator |

---

### ⚡ Option A: Quick Start (3-Minute Setup - Recommended)

The EduNova backend is already deployed and live in the cloud on Render! You can build and run the Android app immediately without hosting your own server.

#### Step 1: Clone the Repository
Open your terminal (PowerShell on Windows, or Bash on macOS/Linux):
```bash
git clone https://github.com/your-username/edunova-offline-first.git
cd edunova-offline-first
```

#### Step 2: Open in Android Studio
1. Launch **Android Studio**.
2. Click **Open** and select the `Offline First` project directory.
3. Wait for Gradle to download dependencies and sync (2–3 minutes on first launch).

#### Step 3: Verify the Live Backend URL
Open [`gradle.properties`](file:///e:/Offline%20First/gradle.properties) in the root folder. Notice that the production cloud URL is already set by default:
```properties
backendBaseUrl=https://edunova-backend-9waj.onrender.com
```

#### Step 4: Run the App
1. Connect your Android smartphone via USB (with **USB Debugging** enabled in Developer Options) **OR** start an Android Virtual Device (AVD Emulator) with at least 4GB RAM.
2. Click the green **Run ▶** button in Android Studio (or press `Shift + F10`).
3. The app will install and open automatically! Create an account or sign in to begin exploring.

---

### 🛠️ Option B: Full Local Developer Setup (Backend + App)

Follow these steps if you want to run the FastAPI backend server, database, and Android app entirely on your local development machine.

#### Step 1: Set Up & Run the Python Cloud Backend

1. **Navigate to the backend directory and create a virtual environment**:
   ```powershell
   cd backend
   python -m venv .venv
   
   # Activate the virtual environment:
   .\.venv\Scripts\activate      # Windows (PowerShell)
   # source .venv/bin/activate    # macOS / Linux
   ```

2. **Install Python dependencies**:
   ```bash
   pip install --upgrade pip
   pip install -r requirements.txt
   ```

3. **Configure the Environment Variables**:
   Create a `.env` file inside the `backend/` folder (or copy from `.env.example`):
   ```ini
   ENVIRONMENT=development
   DATABASE_URL=postgresql+asyncpg://postgres:your_password@localhost:5432/edunova
   JWT_SECRET_KEY=your-32-byte-secret-hex-key-here
   OPENROUTER_API_KEY=sk-or-v1-your-openrouter-key-here
   OPENROUTER_MODEL=google/gemini-3.8-flash
   CORS_ORIGINS=["http://10.0.2.2:8000","http://localhost:8000","http://127.0.0.1:8000"]
   ```

4. **Run Database Migrations & Seed Baseline Curriculum**:
   ```bash
   alembic upgrade head
   python scripts/seed_data.py
   ```

5. **Start the FastAPI Development Server**:
   ```bash
   uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
   ```
   - 🌐 **Interactive Swagger API Docs**: [http://localhost:8000/docs](http://localhost:8000/docs)
   - 🩺 **Health Check**: [http://localhost:8000/api/v1/health](http://localhost:8000/api/v1/health)

---

#### Step 2: Configure & Compile the Android Client

1. **Direct the Android App to your Local Machine**:
   Open [`gradle.properties`](file:///e:/Offline%20First/gradle.properties) in the project root:
   ```properties
   # For Android Emulator pointing to host computer:
   backendBaseUrl=http://10.0.2.2:8000

   # For Physical Device connected via USB (with 'adb reverse tcp:8000 tcp:8000'):
   # backendBaseUrl=http://127.0.0.1:8000

   # For Physical Device on the same Wi-Fi (replace with your computer's local IP):
   # backendBaseUrl=http://192.168.1.50:8000
   ```

2. **Compile and Run Unit Tests (Optional Verification)**:
   ```powershell
   # Point to Android Studio's bundled JDK
   $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
   .\gradlew.bat testDebugUnitTest
   ```

3. **Build and Install Debug APK**:
   ```powershell
   .\gradlew.bat installDebug
   ```
   *The generated standalone APK is also available at:*  
   [`app/build/outputs/apk/debug/app-debug.apk`](file:///e:/Offline%20First/app/build/outputs/apk/debug/app-debug.apk)

---

#### Step 3: Setting Up Offline AI Models (On-Device LLM)

EduNova uses quantized **Qwen 2.5 GGUF** models so you can chat with the AI tutor without an internet connection.

```mermaid
flowchart TD
    A["Choose Offline Model Setup"] --> B["Method 1: In-App 1-Click Download<br/>(Easiest)"]
    A --> C["Method 2: Python Helper Script<br/>(Fastest for Devs)"]
    
    B --> D["Open App > AI Workspace > Tap 'Download Offline Model'<br/>Downloads directly from Hugging Face"]
    C --> E["Run: python backend/scripts/download_qwen_gguf.py<br/>Pushes model directly to device storage"]
```

- **Method 1 (Built-In 1-Click In-App Download)**:
  1. Open the EduNova app on your phone.
  2. Switch the AI Workspace mode toggle from **Online** to **Offline**.
  3. The app will detect if model weights are missing and show a **"Download AI Tutor Model"** button.
  4. Tap Download — the app streams the verified Qwen GGUF model directly from Hugging Face with progress indicators, background pause/resume, and SHA-256 verification.

- **Method 2 (CLI Pre-Download via Python)**:
  Run the automated script to download the model directly to your machine:
  ```bash
  python backend/scripts/download_qwen_gguf.py
  ```
  Then push it to your Android device's app storage using ADB:
  ```bash
  adb push backend/models/Qwen2.5-1.5B-Instruct-Q4_K_M.gguf /sdcard/Android/data/com.offline_First/files/models/
  ```

---

### 🔧 Troubleshooting & Common Questions

<details>
<summary><b>❓ The Android Emulator cannot connect to my local backend server (Network Error / Timeout)</b></summary>

- Inside the Android Emulator, `localhost` refers to the *emulator itself*, not your computer.
- Always use `http://10.0.2.2:8000` as the `backendBaseUrl` in `gradle.properties` when using an emulator.
- If using a physical phone over USB, run this in PowerShell: `adb reverse tcp:8000 tcp:8000` and set `backendBaseUrl=http://127.0.0.1:8000`.
</details>

<details>
<summary><b>❓ CMake or NDK compilation error in Android Studio</b></summary>

- EduNova requires Android NDK version `27.2.12479018`.
- In Android Studio, go to **Settings (Preferences) > Appearance & Behavior > System Settings > Android SDK > SDK Tools**.
- Check **Show Package Details**, scroll down to **NDK (Side by side)**, check version `27.2.12479018`, and click **Apply**.
</details>

<details>
<summary><b>❓ Port 8000 is already in use by another application</b></summary>

- You can launch Uvicorn on another port, for example port 8080:
  ```bash
  uvicorn app.main:app --host 0.0.0.0 --port 8080 --reload
  ```
- Remember to update `backendBaseUrl` in `gradle.properties` to match port `8080`.
</details>

<details>
<summary><b>❓ How much storage and RAM does on-device AI need?</b></summary>

- **Qwen 2.5 1.5B Instruct (Q4_K_M)**: Requires ~980 MB of disk storage and runs smoothly on phones with **4 GB to 6 GB RAM**.
- **Qwen 2.5 3B Instruct (Q4_K_M)**: Requires ~2.0 GB of disk storage and is recommended for devices with **6 GB to 8 GB+ RAM**.
- The app automatically unloads model weights after **3 minutes** of inactivity to ensure your battery and memory remain untouched when not actively learning!
</details>

---

## 🌟 What is EduNova? (Non-Technical Explanation)

### 💡 The Big Idea: The Pocket Professor That Never Disconnects

Imagine living in an area where the internet goes out every evening, or riding on a train with zero signal, or having a limited cellular data plan. In traditional educational apps, your learning stops completely — quizzes refuse to load, chatbots throw "No Connection" errors, and textbooks remain locked on a remote server.

**EduNova changes that forever.**

Think of EduNova like having a **brilliant, patient private tutor who lives directly inside your phone's memory chip**. 

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                             HOW EDUNOVA WORKS                               │
├──────────────────────────────────────┬──────────────────────────────────────┤
│           🌐 ONLINE MODE             │           📴 OFFLINE MODE            │
│       (When Wi-Fi Is Available)      │         (No Internet Needed)         │
├──────────────────────────────────────┼──────────────────────────────────────┤
│ 🚀 Supercharged Cloud AI (Gemini)    │ 📱 100% On-Device AI (Qwen + C++)    │
│ 📚 Live Syllabus Sync from CBSE      │ 💾 Local Encrypted SQLite Database   │
│ 🔄 Cloud Backup for Messages & Stats │ ⚡ Zero Data Usage & Zero Latency    │
│ 🔎 Live Web & Citation Search        │ 🔒 100% Private — Data Never Leaves  │
└──────────────────────────────────────┴──────────────────────────────────────┘
```

When you have internet, EduNova synchronizes your progress to the cloud and gives you access to ultra-fast Google Gemini AI with verified CBSE school curriculum citations. When your internet cuts off, **nothing breaks**. The app seamlessly switches to its built-in C++ offline engine (`llama.cpp`), allowing you to continue asking questions, practicing exam problems, generating quizzes, and tracking your roadmap without missing a single beat.

---

### 🆚 Traditional Learning Apps vs. EduNova

| Feature | Typical EdTech Apps | EduNova AI Platform |
|---|---|---|
| **Works without Internet** | ❌ No. Shows "Check Connection" spinner | ✅ **100% Functional** on-device offline engine |
| **AI Question Answering** | ⚠️ Online-only API calls | ✅ **Dual-Engine** (Cloud Gemini + Edge Qwen 2.5) |
| **Curriculum Accuracy** | ⚠️ Generic LLM prone to hallucinations | ✅ **Strict Grounded RAG** on official CBSE syllabi |
| **Learning Roadmaps** | 📄 Static, boring list of links | ✅ **Adaptive DAG Graph** that customizes to your pace |
| **Privacy & Security** | ☁️ All chats sent to third-party servers | ✅ **Local Encrypted Storage** (AES-256 GCM) |
| **Interactive Study Tools** | 🔒 Paywalled or separate disjointed apps | ✅ **7 Built-In Tools** (Flashcards, Quizzes, PYQs, Maps) |

---

## 📱 User Experience & Core Features Tour

EduNova is packed with intuitive tools designed for students, competitive exam aspirants (JEE/NEET/GATE), and software engineers.

```mermaid
flowchart TD
    Home["🏠 EduNova Home Hub"]
    Home --> Tab1["📚 Verified Courses<br/>CBSE Classes 9-12 & NCERT"]
    Home --> Tab2["🤖 Dual AI Tutor<br/>Real-time Streaming Chat"]
    Home --> Tab3["🗺️ Career & Skill Roadmaps<br/>Dynamic Personalized Graphs"]
    Home --> Tab4["🛠️ Interactive Study Tools<br/>7 Hands-On Learning Utilities"]
    Home --> Tab5["📊 Progress & Memory<br/>Streak Tracking & Analytics"]
```

---

### 1. 🤖 Dual-Mode AI Workspace
The AI Workspace is the crown jewel of EduNova. It feels like chatting with a world-class teacher:
- **Instant Token Streaming**: Text types out smoothly in real-time.
- **Formula & Math Rendering**: Clean LaTeX math formatting for complex physics and chemistry equations.
- **Copy, Edit & Branch**: Don't like how a question was phrased? Edit your message to branch off into a new line of thought.
- **Offline/Online Switcher**: A simple top toggle lets you switch between cloud speed and edge privacy with one tap.

---

### 2. 🛠️ The 7 Interactive Study Tools Suite

EduNova comes equipped with 7 dedicated micro-apps accessible right from your home screen:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          7 INTERACTIVE STUDY TOOLS                          │
├───────────────────────────┬─────────────────────────────────────────────────┤
│ 🃏 Flashcards Tool        │ Interactive flip-cards with spaced repetition   │
│ 📝 Quiz Arena             │ Timed multiple-choice quizzes with explanations │
│ 📜 PYQ Exam Simulator     │ Real Previous Years' CBSE board exam questions  │
│ 🧠 Concept Mind Maps      │ Visual tree structures breaking down hard topics│
│ 📅 Smart Study Planner    │ Exam countdowns, daily targets, and revisions   │
│ 📊 Topic Analytics        │ Visual heatmaps identifying strong & weak areas │
│ 🔍 Visual Problem Solver  │ Image analysis for textbook diagrams and notes  │
└───────────────────────────┴─────────────────────────────────────────────────┘
```

1. **🃏 Interactive Flashcard Tool**: Flip through digital revision cards, rate your recall difficulty, and let spaced repetition ensure you never forget key formulas.
2. **📝 Interactive Quiz Arena**: Test your knowledge with chapter-wise multiple choice questions, instant grading, and detailed explanations for every option.
3. **📜 Interactive PYQ Exam Tool**: Practice official Previous Years' Questions (Class 9–12) with scoring rubrics and step-by-step marking schemes.
4. **🧠 Interactive Mind Map Tool**: Breaks down massive chapters (e.g., *Electromagnetism* or *Photosynthesis*) into clean, digestible visual branches.
5. **📅 Interactive Study Planner Tool**: Calculates how many days are left until your board or entrance exams and schedules daily bite-sized tasks.
6. **📊 Interactive Topic Reports & Analytics**: Shows radar charts and mastery percentages so you know exactly which chapters need extra revision.
7. **🔍 Interactive Image Problem Solver**: Snap a photo or upload an image of a diagram, circuit, or textbook problem for instant pedagogical breakdowns.

---

### 3. 🗺️ Dynamic Career & Skill Roadmaps

Whether you are preparing for high school board exams or training to become a software engineer, EduNova provides interactive, step-by-step roadmaps:
- **Pre-Built Engineering Tracks**:
  - 🌐 **Modern Web Development**: HTML/CSS, JavaScript, TypeScript, React, Next.js, Backend APIs.
  - 🐍 **Python Specialist**: Core Python, OOP, Data Structures, FastAPI, Asyncio.
  - ☕ **Enterprise Java Developer**: Java 21, Spring Boot, Microservices, JVM internals.
  - 🤖 **Machine Learning & AI**: Math & Statistics, Scikit-Learn, PyTorch, LLMs, Vector Databases.
  - 🗄️ **SQL & Database Architect**: Relational design, Indexing, Query Optimization, NoSQL.
- **Personalized Assessment Engine**: Take a quick diagnostic quiz, and EduNova automatically constructs a custom **Directed Acyclic Graph (DAG)** tailored to your current knowledge level and available study hours per week.

---

### 4. 📚 Official CBSE & NCERT Syllabus Coverage

EduNova contains verified official curriculum materials across four key school standards:
- **Class 9**: Science, Mathematics, Social Science, English
- **Class 10**: Science, Mathematics, Social Science, English (Board Exam Focus)
- **Class 11**: Physics, Chemistry, Mathematics, Biology, Computer Science, Economics
- **Class 12**: Physics, Chemistry, Mathematics, Biology, Computer Science, Economics

Every single chapter is cataloged with verified textbook page references, official CBSE source manifests, and pre-computed semantic embeddings so the AI tutor answers strictly within syllabus bounds.

---

## 🏗️ System Architecture & Engineering

EduNova adheres to clean modular architecture, splitting responsibilities cleanly between the Android frontend, the native C++ inference bridge, and the asynchronous Python backend.

### High-Level Architectural Flow

```mermaid
flowchart TB
    subgraph AndroidClient ["Android Mobile Application (Kotlin + Jetpack Compose)"]
        UI["UI Presentation Layer\n- AI Workspace Screen\n- EduNova Home Hub\n- Roadmap & Assessment Screens\n- Interactive Tools Suite"]
        VM["ViewModel & State Layer\n- AIViewModel\n- RoadmapViewModel\n- AuthViewModel\n- SessionManager (StateFlow)"]
        REPO["Repository Orchestration\n- ModeAwareAIRepository\n- LocalAIRepository\n- OnlineAIRepository\n- Course & Roadmap Repositories"]
        
        subgraph LocalEdgeEngine ["Local Edge Engine (100% Offline)"]
            CACHE_DB[("Local SQLite Database\nedunova_chat_cache.db\nsyllabus_offline.db")]
            TOKEN_STORE["EncryptedTokenStorage\n(MasterKey AES-256 GCM)"]
            MODEL_MGR["OfflineModelManager\n(Watchdog, KV Cache lifecycle)"]
            JNI["JNI Bridge (llama_jni.cpp)"]
            LLAMA_CPP["llama.cpp Native Libraries\n(libllama, libggml-cpu)"]
            GGUF_FILE[("Local GGUF Models\nQwen2.5-1.5B / 3B")]
            E5_LOCAL["Local ONNX Embeddings\n(384-dim E5 Small)"]
        end

        subgraph RemoteNetworking ["Secure Networking Layer"]
            OKHTTP_PUB["Public Auth Client\n(Register, Login, Session Restore)"]
            OKHTTP_AUTH["Authenticated Client\n(Bearer JWT + Auto-Refresh Mutex)"]
            CHAT_CLIENT["Streaming Chat Client\n(SSE Protocol / Fallbacks)"]
        end
    end

    subgraph CloudBackend ["Cloud Backend (FastAPI + Async Python 3.11)"]
        ROUTERS["API Routers (/api/v1)\n- /auth (JWT rotation & replay defense)\n- /chat & /stream (SSE Streaming)\n- /courses & /learning\n- /roadmaps & /profile\n- /memory (Long-term learner facts)"]
        MIDDLEWARE["Security & Defense Middleware\n- SlowAPI Rate Limiter\n- CorrelationIdMiddleware (X-Correlation-ID)\n- Global Error Handlers"]
        SERVICES["Core Business Services\n- AuthService & Token Rotation\n- ChatService\n- SupabaseRetrievalService (RRF)\n- OpenRouterService (Google Gemini)\n- MemoryService (Rule-based Extraction)"]
        SQLA["SQLAlchemy 2.0 Async ORM"]
        DB[("PostgreSQL Database (Supabase)\nUsers, Progress, Roadmaps, Messages\nSyllabus pgvector 384-dim (IVFFlat + GIN)")]
    end

    subgraph ExternalCloud ["External Cloud Services"]
        OPENROUTER["OpenRouter API\n(google/gemini-3.8-flash)"]
        HF["Hugging Face Hub\n(PatilKrish/Qwen_Models2)"]
    end

    %% Flow connections
    UI --> VM
    VM --> REPO
    REPO --> MODEL_MGR
    REPO --> CACHE_DB
    REPO --> OKHTTP_AUTH
    REPO --> OKHTTP_PUB
    REPO --> CHAT_CLIENT

    MODEL_MGR --> JNI
    JNI --> LLAMA_CPP
    LLAMA_CPP --> GGUF_FILE
    MODEL_MGR -.->|1-Click Download| HF

    OKHTTP_PUB -->|HTTPS / REST| ROUTERS
    OKHTTP_AUTH -->|HTTPS + Bearer JWT| ROUTERS
    CHAT_CLIENT -->|SSE Token Stream| ROUTERS

    ROUTERS --> MIDDLEWARE
    MIDDLEWARE --> SERVICES
    SERVICES --> SQLA
    SQLA --> DB
    SERVICES -->|Prompt + Grounded Evidence| OPENROUTER
```

---

### 🔄 Edge vs. Cloud Request Routing Lifecycle

When a student types a question in the AI Workspace, EduNova's `ModeAwareAIRepository` dynamically routes the prompt based on active connectivity, user choice, and on-device model availability:

```mermaid
sequenceDiagram
    autonumber
    actor Learner as Student / Learner
    participant UI as AIWorkspaceScreen
    participant VM as AIViewModel
    participant Router as ModeAwareAIRepository
    participant LocalRepo as LocalAIRepository
    participant ModelMgr as OfflineModelManager
    participant JNI as llama_jni.cpp
    participant OnlineRepo as OnlineAIRepository
    participant FastAPI as Cloud FastAPI
    participant Supabase as Supabase pgvector
    participant Gemini as Google Gemini 3.8

    Learner->>UI: Types question & hits Send
    UI->>VM: sendMessage(prompt)
    VM->>Router: streamMessage(prompt, conversationId, mode)

    alt Mode is OFFLINE (Edge Inference)
        Router->>LocalRepo: streamMessage(...)
        LocalRepo->>ModelMgr: generateStreaming(prompt, history)
        ModelMgr->>ModelMgr: Check instance ID & reset 3-min watchdog
        ModelMgr->>JNI: nativeGenerateStream(handle, prompt, callback)
        loop Token-by-Token Generation
            JNI-->>ModelMgr: onToken(tokenPiece)
            ModelMgr-->>LocalRepo: emit(tokenPiece)
            LocalRepo-->>VM: emit(tokenPiece)
            VM-->>UI: Real-time UI text update
        end
        LocalRepo->>LocalRepo: Commit prompt & answer to local SQLite
    else Mode is ONLINE (Cloud Inference)
        Router->>OnlineRepo: streamMessage(...)
        OnlineRepo->>FastAPI: POST /api/v1/chat/.../stream (Bearer JWT)
        FastAPI->>Supabase: Hybrid Search (E5 384-dim dense + tsvector sparse)
        Supabase-->>FastAPI: Top Chunks fused via RRF (k=60)
        FastAPI->>Gemini: Stream prompt + Grounded Syllabus Context
        loop SSE Event Stream
            Gemini-->>FastAPI: Token chunk
            FastAPI-->>OnlineRepo: SSE 'token' / 'citations' event
            OnlineRepo-->>VM: emit(tokenPiece)
            VM-->>UI: Real-time UI text update
        end
        OnlineRepo->>OnlineRepo: Cache message in local SQLite database
    end
```

---

### 🧠 Hybrid RAG Retrieval Engine

In Online Mode, questions are matched against official curriculum chunks using a state-of-the-art **Hybrid Retrieval** architecture:

1. **Dense Vector Search**: The user query is converted into a 384-dimensional dense vector using the `intfloat/multilingual-e5-small` model. Vector cosine similarity finds conceptually related text even if exact keywords differ.
2. **Sparse Lexical Search**: PostgreSQL full-text search (`tsvector` with `tsquery`) locates exact technical keywords, chapter titles, and formula names.
3. **Reciprocal Rank Fusion (RRF)**: Results from both retrievers are merged mathematically:
   $$\text{RRF}(d) = \sum_{m \in \{\text{dense}, \text{sparse}\}} \frac{1}{k + r_m(d)} \quad (k = 60)$$
4. **Calibrated Confidence Thresholds**:
   - `SUFFICIENT (>= 0.48)`: The system prompt includes verified citations and directs the LLM to ground its response in the retrieved syllabus text.
   - `UNCERTAIN (0.35 - 0.47)`: The LLM answers with standard general pedagogical knowledge while gently advising the student that the topic is outside their specific CBSE chapter.
   - `INSUFFICIENT (< 0.35)`: Prevents hallucination by politely informing the student that the question cannot be answered from the official course materials.

---

### 🔐 Enterprise Security & Hardware-Backed Encryption

- **Dual-Token JWT Architecture**: Short-lived Access Tokens (15 minutes) paired with long-lived Refresh Tokens (30 days). Refresh tokens are stored as SHA-256 hashes linked to a unique `session_family_id`.
- **Replay Attack Defense**: If a revoked refresh token is presented, the backend detects token theft and immediately revokes all sessions in that family.
- **Android Keystore Encryption**: On Android, tokens are never stored in plaintext. They are encrypted using `androidx.security.crypto.MasterKey` with hardware-backed AES-256 GCM encryption.
- **Synchronized Mutex Re-Authentication**: When an OkHttp request encounters a `401 Unauthorized`, an atomic mutex refreshes the token once and transparently retries all concurrent in-flight requests.

---

## 📂 Repository & Directory Structure

EduNova is organized with dedicated documentation in every major directory:

| Directory | Purpose | Dedicated Documentation |
|---|---|---|
| [`adapters/`](file:///e:/Offline%20First/adapters/README.md) | AI agent rule configuration adapters (`CLAUDE.md`, `GEMINI.md`, `GPT_OSS.md`) | [adapters/README.md](file:///e:/Offline%20First/adapters/README.md) |
| [`app/`](file:///e:/Offline%20First/app/README.md) | Android client application (Kotlin, Compose, C++ NDK, SQLite, llama.cpp) | [app/README.md](file:///e:/Offline%20First/app/README.md) |
| ├── [`app/src/main/cpp/`](file:///e:/Offline%20First/app/src/main/cpp/README.md) | Native C++ JNI bridge for `llama.cpp` inference and KV cache control | [app/src/main/cpp/README.md](file:///e:/Offline%20First/app/src/main/cpp/README.md) |
| ├── [`app/src/main/jniLibs/`](file:///e:/Offline%20First/app/src/main/jniLibs/README.md) | Prebuilt 64-bit native libraries (`arm64-v8a`, `x86_64`) | [app/src/main/jniLibs/README.md](file:///e:/Offline%20First/app/src/main/jniLibs/README.md) |
| ├── [`app/src/main/java/.../`](file:///e:/Offline%20First/app/src/main/java/com/offline_First/README.md) | Kotlin source root, `MainActivity.kt`, and `AppContainer.kt` | [java/.../README.md](file:///e:/Offline%20First/app/src/main/java/com/offline_First/README.md) |
| │   ├── [`data/`](file:///e:/Offline%20First/app/src/main/java/com/offline_First/data/README.md) | Data layer: SQLite, EncryptedSharedPreferences, OkHttp clients, repositories | [data/README.md](file:///e:/Offline%20First/app/src/main/java/com/offline_First/data/README.md) |
| │   ├── [`domain/`](file:///e:/Offline%20First/app/src/main/java/com/offline_First/domain/README.md) | Domain models, business entities, and reactive UI state wrappers | [domain/README.md](file:///e:/Offline%20First/app/src/main/java/com/offline_First/domain/README.md) |
| │   └── [`ui/`](file:///e:/Offline%20First/app/src/main/java/com/offline_First/ui/README.md) | Presentation layer: Jetpack Compose screens, ViewModels, and design system | [ui/README.md](file:///e:/Offline%20First/app/src/main/java/com/offline_First/ui/README.md) |
| └── [`app/src/test/`](file:///e:/Offline%20First/app/src/test/README.md) | Android unit and integration test suite (JUnit 4, MockWebServer, Coroutines) | [app/src/test/README.md](file:///e:/Offline%20First/app/src/test/README.md) |
| [`backend/`](file:///e:/Offline%20First/backend/README.md) | Python cloud backend application (FastAPI, SQLAlchemy 2.0 Async, Supabase) | [backend/README.md](file:///e:/Offline%20First/backend/README.md) |
| ├── [`backend/app/`](file:///e:/Offline%20First/backend/app/README.md) | FastAPI core application: routes, core security, middleware, ORM models, services | [backend/app/README.md](file:///e:/Offline%20First/backend/app/README.md) |
| ├── [`backend/docs/`](file:///e:/Offline%20First/backend/docs/README.md) | Backend research notes, threshold analyses, and architectural reports | [backend/docs/README.md](file:///e:/Offline%20First/backend/docs/README.md) |
| ├── [`backend/evals/`](file:///e:/Offline%20First/backend/evals/README.md) | RAG evaluation framework, golden benchmark cases, and scoring metrics | [backend/evals/README.md](file:///e:/Offline%20First/backend/evals/README.md) |
| ├── [`backend/migrations/`](file:///e:/Offline%20First/backend/migrations/README.md) | Alembic database schema migration history and scripts | [backend/migrations/README.md](file:///e:/Offline%20First/backend/migrations/README.md) |
| ├── [`backend/models/`](file:///e:/Offline%20First/backend/models/README.md) | Quantized ONNX embeddings (`e5-small`) and pre-downloaded GGUF weights | [backend/models/README.md](file:///e:/Offline%20First/backend/models/README.md) |
| ├── [`backend/scripts/`](file:///e:/Offline%20First/backend/scripts/README.md) | Operational scripts (syllabus ingestion, data seeding, inference parity) | [backend/scripts/README.md](file:///e:/Offline%20First/backend/scripts/README.md) |
| ├── [`backend/syllabus_sources/`](file:///e:/Offline%20First/backend/syllabus_sources/README.md) | Official CBSE & NCERT curriculum documents, raw PDFs, and source manifests | [syllabus_sources/README.md](file:///e:/Offline%20First/backend/syllabus_sources/README.md) |
| └── [`backend/tests/`](file:///e:/Offline%20First/backend/tests/README.md) | Backend pytest test suite (unit, integration, property-based tests) | [backend/tests/README.md](file:///e:/Offline%20First/backend/tests/README.md) |
| [`docs/`](file:///e:/Offline%20First/docs/README.md) | Global system audits, request flow traces, and corpus inventories | [docs/README.md](file:///e:/Offline%20First/docs/README.md) |
| [`gradle/`](file:///e:/Offline%20First/gradle/README.md) | Gradle wrapper binaries, JVM daemon configs, and version catalog (`libs.versions.toml`) | [gradle/README.md](file:///e:/Offline%20First/gradle/README.md) |
| [`scripts/`](file:///e:/Offline%20First/scripts/README.md) | Project automation: prebuilt compilation, repository search, and validation | [scripts/README.md](file:///e:/Offline%20First/scripts/README.md) |
| [`.gsd/`](file:///e:/Offline%20First/.gsd/README.md) | Get Shit Done (GSD) roadmap, specification, phase execution plans, and state tracking | [.gsd/README.md](file:///e:/Offline%20First/.gsd/README.md) |

---

## 🧪 Testing & Quality Verification

EduNova maintains comprehensive, automated test suites across both the Android application and the backend service:

### 1. Backend Pytest Suite
```powershell
cd backend
.\.venv\Scripts\python.exe -m pytest tests/ -v
```
Test modules include:
- `test_auth.py`: Registration, login, token refresh, and session revocation.
- `test_auth_properties.py`: Property-based fuzz testing of authentication tokens using Hypothesis.
- `test_chat.py` & `test_chat_stream.py`: Real-time SSE streaming and message persistence invariants.
- `test_rrf_scoring.py`: Mathematical validation of Reciprocal Rank Fusion retrieval logic.
- `test_syllabus_sync.py`: Verification of curriculum synchronization payloads.

---

### 2. RAG Retrieval Benchmark Evaluation
```powershell
cd backend
.\.venv\Scripts\python.exe evals/rag/run_eval.py --suite retrieval --output-dir evals/rag/results/
```
Measures **Recall@K**, **MRR@K**, **nDCG@10**, and cross-curriculum leakage across the `golden_set.jsonl` benchmark suite.

---

### 3. Android Client Unit Tests
```powershell
# In the project root:
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat testDebugUnitTest
```
Tests ViewModel state transitions, MockWebServer 401 token refresh mutex, SQLite cache integrity, and offline model watchdog lifecycles.

---

### 4. Full Pre-Commit System Validation
Before submitting pull requests or publishing updates, run the automated health check:
```powershell
.\scripts\validate-all.ps1
```

---

## 🤝 Contributing & Development Workflows

We welcome contributions from educators, students, and engineers!
- Please read [PROJECT_RULES.md](file:///e:/Offline%20First/PROJECT_RULES.md) for architectural boundaries, coding standards, and error-handling conventions.
- Check [GSD-STYLE.md](file:///e:/Offline%20First/GSD-STYLE.md) for commit standards and task orchestration conventions.
- Review [PROJECT_ARCHITECTURE_AND_SUMMARY.md](file:///e:/Offline%20First/PROJECT_ARCHITECTURE_AND_SUMMARY.md) for deep technical details and roadmap progress.

---

## 📄 License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for complete details.

---

<p align="center">
  <b>Built with ❤️ for learners everywhere — online or offline.</b>
</p>
