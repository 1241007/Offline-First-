package com.offline_First.ui.screens.ai.tools

data class QuizQuestion(
    val id: Int,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class QuizTopic(
    val topicTitle: String,
    val subject: String,
    val difficulty: String,
    val questions: List<QuizQuestion>
)

object DemoQuizProvider {
    val sampleTopics = listOf(
        "Newton's Laws of Motion",
        "Photosynthesis & Respiration",
        "Python Data Structures",
        "Electric Current & Circuits",
        "Chemical Bonding & Reactions"
    )

    fun getQuizForTopic(topicQuery: String): QuizTopic {
        val query = topicQuery.trim().lowercase()
        return when {
            query.contains("photo") || query.contains("bio") || query.contains("plant") || query.contains("respir") -> biologyQuiz
            query.contains("python") || query.contains("data") || query.contains("code") || query.contains("struct") -> pythonQuiz
            query.contains("electric") || query.contains("circuit") || query.contains("current") || query.contains("ohm") -> electricityQuiz
            query.contains("chem") || query.contains("bond") || query.contains("atom") || query.contains("reaction") -> chemistryQuiz
            else -> physicsQuiz // Default to Newton's laws / Physics
        }
    }

    private val physicsQuiz = QuizTopic(
        topicTitle = "Newton's Laws of Motion",
        subject = "Physics • Class 10/11",
        difficulty = "Intermediate",
        questions = listOf(
            QuizQuestion(
                id = 1,
                question = "A passenger in a moving bus falls forward when the bus suddenly stops. This is explained by:",
                options = listOf(
                    "Newton's First Law (Inertia)",
                    "Newton's Second Law (F = ma)",
                    "Newton's Third Law (Action-Reaction)",
                    "Law of Conservation of Momentum"
                ),
                correctIndex = 0,
                explanation = "Due to the inertia of motion, the passenger's upper body tends to keep moving forward when the bus stops suddenly."
            ),
            QuizQuestion(
                id = 2,
                question = "If the net force acting on an object is doubled while its mass remains constant, its acceleration will:",
                options = listOf(
                    "Remain unchanged",
                    "Be halved",
                    "Double",
                    "Quadruple"
                ),
                correctIndex = 2,
                explanation = "From Newton's Second Law, a = F/m. Since acceleration is directly proportional to net force, doubling F doubles a."
            ),
            QuizQuestion(
                id = 3,
                question = "Rocket propulsion functions primarily on which fundamental principle?",
                options = listOf(
                    "Gravitational attraction",
                    "Conservation of energy only",
                    "Newton's Third Law & Conservation of Momentum",
                    "Bernoulli's Principle"
                ),
                correctIndex = 2,
                explanation = "The high-velocity expulsion of exhaust gas downward creates an equal and opposite reaction force pushing the rocket upward."
            ),
            QuizQuestion(
                id = 4,
                question = "What is the SI unit of momentum (p = mv)?",
                options = listOf(
                    "kg·m/s²",
                    "kg·m/s",
                    "N·m",
                    "Joule·s"
                ),
                correctIndex = 1,
                explanation = "Momentum is mass (kg) multiplied by velocity (m/s), giving kg·m/s (or Newton-seconds)."
            )
        )
    )

    private val pythonQuiz = QuizTopic(
        topicTitle = "Python Data Structures",
        subject = "Computer Science",
        difficulty = "Foundation",
        questions = listOf(
            QuizQuestion(
                id = 1,
                question = "Which of the following built-in data types in Python is IMMUTABLE?",
                options = listOf(
                    "List",
                    "Dictionary",
                    "Tuple",
                    "Set"
                ),
                correctIndex = 2,
                explanation = "Tuples cannot be modified after creation, making them immutable and hashable."
            ),
            QuizQuestion(
                id = 2,
                question = "What is the average time complexity to look up a key in a Python dictionary?",
                options = listOf(
                    "O(1)",
                    "O(log n)",
                    "O(n)",
                    "O(n²)"
                ),
                correctIndex = 0,
                explanation = "Python dictionaries are implemented using hash tables, giving them average O(1) constant lookup time."
            ),
            QuizQuestion(
                id = 3,
                question = "What will `[x**2 for x in range(4)]` evaluate to?",
                options = listOf(
                    "[1, 4, 9, 16]",
                    "[0, 1, 4, 9]",
                    "[0, 2, 4, 6]",
                    "[0, 1, 2, 3]"
                ),
                correctIndex = 1,
                explanation = "range(4) produces 0, 1, 2, 3. Squaring each yields [0, 1, 4, 9]."
            ),
            QuizQuestion(
                id = 4,
                question = "Which data structure follows the First-In-First-Out (FIFO) principle?",
                options = listOf(
                    "Stack",
                    "Queue",
                    "Priority Heap",
                    "Graph"
                ),
                correctIndex = 1,
                explanation = "A Queue processes elements in the order they arrived (FIFO), unlike a Stack (LIFO)."
            )
        )
    )

    private val biologyQuiz = QuizTopic(
        topicTitle = "Photosynthesis & Plant Biology",
        subject = "Biology • Class 10",
        difficulty = "Board Exam Ready",
        questions = listOf(
            QuizQuestion(
                id = 1,
                question = "Which organelle is the site of photosynthesis in eukaryotic plant cells?",
                options = listOf(
                    "Mitochondria",
                    "Chloroplast",
                    "Ribosome",
                    "Endoplasmic Reticulum"
                ),
                correctIndex = 1,
                explanation = "Chloroplasts contain chlorophyll pigments and thylakoid membranes where light reactions take place."
            ),
            QuizQuestion(
                id = 2,
                question = "What is the byproduct gas released during the light-dependent reactions of photosynthesis?",
                options = listOf(
                    "Carbon Dioxide (CO₂)",
                    "Nitrogen (N₂)",
                    "Oxygen (O₂)",
                    "Methane (CH₄)"
                ),
                correctIndex = 2,
                explanation = "Water molecules are split (photolysis) during the light reactions, releasing oxygen gas."
            ),
            QuizQuestion(
                id = 3,
                question = "The primary light-absorbing green pigment in plants is:",
                options = listOf(
                    "Carotenoid",
                    "Anthocyanin",
                    "Chlorophyll a",
                    "Xanthophyll"
                ),
                correctIndex = 2,
                explanation = "Chlorophyll a is the essential photosynthetic pigment that traps photons of solar radiation."
            )
        )
    )

    private val electricityQuiz = QuizTopic(
        topicTitle = "Electric Current & Circuits",
        subject = "Physics • CBSE Class 10",
        difficulty = "Intermediate",
        questions = listOf(
            QuizQuestion(
                id = 1,
                question = "According to Ohm's Law (V = IR), if resistance is doubled at constant voltage, the current will:",
                options = listOf(
                    "Double",
                    "Halve",
                    "Remain unchanged",
                    "Become zero"
                ),
                correctIndex = 1,
                explanation = "Current I = V/R. Since I is inversely proportional to R, doubling R reduces current to half."
            ),
            QuizQuestion(
                id = 2,
                question = "Two resistors of 6 Ω and 3 Ω are connected in PARALLEL. What is their equivalent resistance?",
                options = listOf(
                    "9 Ω",
                    "2 Ω",
                    "4.5 Ω",
                    "18 Ω"
                ),
                correctIndex = 1,
                explanation = "1/Req = 1/6 + 1/3 = 3/6 = 1/2. Therefore Req = 2 Ω."
            ),
            QuizQuestion(
                id = 3,
                question = "An ammeter is always connected in _______ with the circuit element whose current is measured.",
                options = listOf(
                    "Series",
                    "Parallel",
                    "Either series or parallel",
                    "Bridge configuration"
                ),
                correctIndex = 0,
                explanation = "An ammeter has very low internal resistance and must be connected in series to measure total current flow."
            )
        )
    )

    private val chemistryQuiz = QuizTopic(
        topicTitle = "Chemical Bonding & Reactions",
        subject = "Chemistry • Class 10/11",
        difficulty = "Intermediate",
        questions = listOf(
            QuizQuestion(
                id = 1,
                question = "An ionic bond is formed between two atoms by:",
                options = listOf(
                    "Equal sharing of electrons",
                    "Complete transfer of one or more electrons",
                    "Sharing of protons",
                    "Magnetic attraction of nuclei"
                ),
                correctIndex = 1,
                explanation = "Ionic bonds form when one atom transfers electrons to another, creating electrostatic attraction between opposite ions."
            ),
            QuizQuestion(
                id = 2,
                question = "What type of chemical reaction is: 2H₂ + O₂ → 2H₂O ?",
                options = listOf(
                    "Decomposition reaction",
                    "Displacement reaction",
                    "Combination reaction",
                    "Double displacement reaction"
                ),
                correctIndex = 2,
                explanation = "Two reactants combine to produce a single product, which defines a combination (synthesis) reaction."
            ),
            QuizQuestion(
                id = 3,
                question = "The pH value of pure neutral water at 25°C is:",
                options = listOf(
                    "0",
                    "7",
                    "14",
                    "1"
                ),
                correctIndex = 1,
                explanation = "Neutral water has equal concentrations of H⁺ and OH⁻ ions, corresponding to pH 7."
            )
        )
    )
}
