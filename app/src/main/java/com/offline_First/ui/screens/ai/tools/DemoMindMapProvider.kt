package com.offline_First.ui.screens.ai.tools

data class MindMapSubNode(
    val title: String,
    val description: String,
    val keyPoints: List<String>
)

data class MindMapBranch(
    val title: String,
    val iconName: String,
    val colorHex: Long,
    val subNodes: List<MindMapSubNode>
)

data class MindMapData(
    val centralTopic: String,
    val subject: String,
    val summary: String,
    val branches: List<MindMapBranch>
)

object DemoMindMapProvider {
    val sampleTopics = listOf(
        "Photosynthesis Process",
        "Newton's Laws & Dynamics",
        "Python Full-Stack Architecture",
        "Electric Current & Circuits",
        "Machine Learning Fundamentals"
    )

    fun getMindMapForTopic(topicQuery: String): MindMapData {
        val query = topicQuery.trim().lowercase()
        return when {
            query.contains("photo") || query.contains("bio") || query.contains("plant") -> photosynthesisMap
            query.contains("python") || query.contains("full") || query.contains("web") || query.contains("code") -> pythonStackMap
            query.contains("ml") || query.contains("machine") || query.contains("ai") || query.contains("learn") -> mlFundamentalsMap
            query.contains("electric") || query.contains("circuit") || query.contains("current") -> electricityMap
            else -> newtonDynamicsMap
        }
    }

    private val photosynthesisMap = MindMapData(
        centralTopic = "Photosynthesis",
        subject = "Biology • Plant Physiology",
        summary = "Biochemical conversion of solar light energy into chemical energy stored in glucose molecules.",
        branches = listOf(
            MindMapBranch(
                title = "Light Reactions (Thylakoids)",
                iconName = "wb_sunny",
                colorHex = 0xFFFFA000, // Amber/Gold
                subNodes = listOf(
                    MindMapSubNode(
                        title = "Photolysis of Water",
                        description = "Splitting of H₂O molecules by light energy.",
                        keyPoints = listOf("2H₂O → 4H⁺ + 4e⁻ + O₂", "Releases oxygen as a byproduct")
                    ),
                    MindMapSubNode(
                        title = "ATP & NADPH Generation",
                        description = "High-energy electron transport chain.",
                        keyPoints = listOf("Photo-phosphorylation", "Energizes Calvin Cycle")
                    )
                )
            ),
            MindMapBranch(
                title = "Dark Reactions / Calvin Cycle (Stroma)",
                iconName = "nights_stay",
                colorHex = 0xFF2A8F93, // EduNova Teal
                subNodes = listOf(
                    MindMapSubNode(
                        title = "Carbon Fixation (RuBisCO)",
                        description = "Enzymatic incorporation of CO₂ into 3-PGA.",
                        keyPoints = listOf("RuBisCO enzyme catalyst", "Independent of direct light")
                    ),
                    MindMapSubNode(
                        title = "Glucose Synthesis",
                        description = "Reduction into G3P and carbohydrate building.",
                        keyPoints = listOf("Produces C₆H₁₂O₆ (Glucose)", "Regenerates RuBP")
                    )
                )
            ),
            MindMapBranch(
                title = "Essential Requirements",
                iconName = "eco",
                colorHex = 0xFF43A047, // Green
                subNodes = listOf(
                    MindMapSubNode(
                        title = "Chlorophyll Pigments",
                        description = "Light-harvesting complexes located in thylakoids.",
                        keyPoints = listOf("Absorbs blue & red wavelengths", "Reflects green light")
                    ),
                    MindMapSubNode(
                        title = "CO₂ & Stomata Regulation",
                        description = "Atmospheric gas exchange controlled by guard cells.",
                        keyPoints = listOf("Turgor pressure controls pore opening", "Conserves water during drought")
                    )
                )
            )
        )
    )

    private val newtonDynamicsMap = MindMapData(
        centralTopic = "Newton's Laws & Dynamics",
        subject = "Physics • Classical Mechanics",
        summary = "Foundational principles governing the relationship between the forces acting on a body and its motion.",
        branches = listOf(
            MindMapBranch(
                title = "1st Law: Inertia",
                iconName = "shield",
                colorHex = 0xFF3F51B5, // Indigo
                subNodes = listOf(
                    MindMapSubNode(
                        title = "Law of Inertia",
                        description = "Bodies resist changes to their velocity.",
                        keyPoints = listOf("ΣF = 0 ⟹ Δv = 0", "Inertia proportional to mass")
                    ),
                    MindMapSubNode(
                        title = "Inertial Frames",
                        description = "Reference frames where Newton's laws hold without pseudo-forces.",
                        keyPoints = listOf("Non-accelerating observers", "Galilean relativity")
                    )
                )
            ),
            MindMapBranch(
                title = "2nd Law: Force & Momentum",
                iconName = "speed",
                colorHex = 0xFFE53935, // Red
                subNodes = listOf(
                    MindMapSubNode(
                        title = "F = m · a (Constant Mass)",
                        description = "Force equals mass multiplied by acceleration.",
                        keyPoints = listOf("F_net = dp/dt", "Unit: Newton (1 N = 1 kg·m/s²)")
                    ),
                    MindMapSubNode(
                        title = "Impulse (J = F · Δt)",
                        description = "Change in linear momentum caused by a force over time.",
                        keyPoints = listOf("Area under F-t curve", "Seatbelts increase Δt to reduce F")
                    )
                )
            ),
            MindMapBranch(
                title = "3rd Law: Action & Reaction",
                iconName = "sync_alt",
                colorHex = 0xFF00897B, // Teal
                subNodes = listOf(
                    MindMapSubNode(
                        title = "Equal & Opposite Forces",
                        description = "Forces always occur in matched interaction pairs.",
                        keyPoints = listOf("F_AB = -F_BA", "Act on DIFFERENT bodies")
                    ),
                    MindMapSubNode(
                        title = "Conservation of Momentum",
                        description = "Total momentum of an isolated system remains constant.",
                        keyPoints = listOf("Rocket propulsion", "Collisions (Elastic vs Inelastic)")
                    )
                )
            )
        )
    )

    private val pythonStackMap = MindMapData(
        centralTopic = "Python Full-Stack Architecture",
        subject = "Software Engineering",
        summary = "Modern layered application development with Python backends and reactive client frontends.",
        branches = listOf(
            MindMapBranch(
                title = "API & Server (FastAPI)",
                iconName = "dns",
                colorHex = 0xFF009688,
                subNodes = listOf(
                    MindMapSubNode(
                        title = "Async Endpoints (ASGI)",
                        description = "Asynchronous request handling with Uvicorn.",
                        keyPoints = listOf("Non-blocking coroutines", "High concurrency throughput")
                    ),
                    MindMapSubNode(
                        title = "Pydantic Schemas",
                        description = "Type-safe data validation and OpenAPI schema docs.",
                        keyPoints = listOf("Runtime serialization", "Automatic Swagger docs generation")
                    )
                )
            ),
            MindMapBranch(
                title = "Data Layer (SQLAlchemy / PostgreSQL)",
                iconName = "storage",
                colorHex = 0xFF3F51B5,
                subNodes = listOf(
                    MindMapSubNode(
                        title = "Asyncpg & Connection Pooling",
                        description = "Direct binary protocol connection to PostgreSQL.",
                        keyPoints = listOf("Prepared statement caching", "Async session management")
                    ),
                    MindMapSubNode(
                        title = "Alembic Migrations",
                        description = "Database schema version control.",
                        keyPoints = listOf("Declarative schema migrations", "Zero data loss upgrades")
                    )
                )
            ),
            MindMapBranch(
                title = "Client Presentation (Jetpack Compose)",
                iconName = "phone_android",
                colorHex = 0xFF7CB342,
                subNodes = listOf(
                    MindMapSubNode(
                        title = "Declarative Reactive UI",
                        description = "UI re-composes automatically when StateFlow changes.",
                        keyPoints = listOf("Single source of truth", "Compose state hoisting")
                    ),
                    MindMapSubNode(
                        title = "Offline-First Sync",
                        description = "Local SQLite cache + NDK on-device fallback.",
                        keyPoints = listOf("Instant offline accessibility", "Background sync queue")
                    )
                )
            )
        )
    )

    private val mlFundamentalsMap = MindMapData(
        centralTopic = "Machine Learning Foundations",
        subject = "Artificial Intelligence",
        summary = "Hierarchical paradigms of training algorithms from statistical patterns.",
        branches = listOf(
            MindMapBranch(
                title = "Supervised Learning",
                iconName = "table_chart",
                colorHex = 0xFF1E88E5,
                subNodes = listOf(
                    MindMapSubNode(
                        title = "Classification",
                        description = "Predicting discrete category labels.",
                        keyPoints = listOf("Logistic Regression, SVM, Random Forest", "Metrics: Accuracy, F1-Score, AUC")
                    ),
                    MindMapSubNode(
                        title = "Regression",
                        description = "Predicting continuous numeric outputs.",
                        keyPoints = listOf("Linear Regression, Gradient Boosting", "Metrics: MSE, RMSE, R²")
                    )
                )
            ),
            MindMapBranch(
                title = "Unsupervised Learning",
                iconName = "bubble_chart",
                colorHex = 0xFF8E24AA,
                subNodes = listOf(
                    MindMapSubNode(
                        title = "Clustering",
                        description = "Grouping unlabeled data points by inherent similarity.",
                        keyPoints = listOf("K-Means, DBSCAN, Hierarchical", "Centroid and density based")
                    ),
                    MindMapSubNode(
                        title = "Dimensionality Reduction",
                        description = "Compressing features while preserving variance.",
                        keyPoints = listOf("PCA (Principal Component Analysis)", "t-SNE & UMAP for visualization")
                    )
                )
            ),
            MindMapBranch(
                title = "Deep Learning & Neural Networks",
                iconName = "psychology",
                colorHex = 0xFFE91E63,
                subNodes = listOf(
                    MindMapSubNode(
                        title = "Backpropagation & Optimizers",
                        description = "Gradient descent calculation via chain rule.",
                        keyPoints = listOf("Loss function minimization", "Adam, RMSprop, SGD")
                    ),
                    MindMapSubNode(
                        title = "Transformers & LLMs",
                        description = "Self-attention architecture processing sequences in parallel.",
                        keyPoints = listOf("Attention is All You Need", "Foundation models (Gemini, Qwen, Llama)")
                    )
                )
            )
        )
    )

    private val electricityMap = MindMapData(
        centralTopic = "Electric Current & Circuits",
        subject = "Physics • CBSE Class 10",
        summary = "Charge flow, potential difference, circuit analysis, and heating effects.",
        branches = listOf(
            MindMapBranch(
                title = "Ohm's Law & Resistance",
                iconName = "bolt",
                colorHex = 0xFFFF8F00,
                subNodes = listOf(
                    MindMapSubNode(
                        title = "Ohm's Principle (V = IR)",
                        description = "Linear proportionality between potential and current.",
                        keyPoints = listOf("Resistance R = ρ·(L/A)", "Ohmic vs Non-Ohmic conductors")
                    ),
                    MindMapSubNode(
                        title = "Resistor Combinations",
                        description = "Series vs Parallel network configurations.",
                        keyPoints = listOf("Series: R_eq = R₁ + R₂", "Parallel: 1/R_eq = 1/R₁ + 1/R₂")
                    )
                )
            ),
            MindMapBranch(
                title = "Electric Power & Heating",
                iconName = "whatshot",
                colorHex = 0xFFD81B60,
                subNodes = listOf(
                    MindMapSubNode(
                        title = "Joule's Law of Heating",
                        description = "Thermal dissipation from charge collisions.",
                        keyPoints = listOf("H = I²·R·t", "Applications: Fuse, Electric Heater")
                    ),
                    MindMapSubNode(
                        title = "Electric Energy & Power",
                        description = "Rate of electrical work done.",
                        keyPoints = listOf("P = V·I = I²·R = V²/R", "Commercial unit: 1 kWh = 3.6 × 10⁶ J")
                    )
                )
            )
        )
    )
}
