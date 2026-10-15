package com.offline_First.ui.screens.ai.tools

data class PyqQuestion(
    val id: Int,
    val year: String, // e.g. "CBSE 2024 (Set 1)"
    val marks: Int,
    val questionText: String,
    val markingSchemeSteps: List<String>,
    val finalAnswer: String
)

data class PyqChapter(
    val chapterTitle: String,
    val subject: String,
    val totalWeightage: String,
    val questions: List<PyqQuestion>
)

object DemoPyqProvider {
    val sampleChapters = listOf(
        "Physics: Electricity & Circuits",
        "Physics: Light (Reflection & Refraction)",
        "Chemistry: Chemical Reactions & Equations",
        "Biology: Life Processes"
    )

    fun getPyqsForChapter(query: String): PyqChapter {
        val q = query.trim().lowercase()
        return when {
            q.contains("light") || q.contains("optics") || q.contains("mirror") || q.contains("lens") -> lightPyqs
            q.contains("chem") || q.contains("reaction") || q.contains("equation") -> chemistryPyqs
            q.contains("bio") || q.contains("life") || q.contains("process") -> biologyPyqs
            else -> electricityPyqs
        }
    }

    private val electricityPyqs = PyqChapter(
        chapterTitle = "Electricity & Circuits",
        subject = "Physics • CBSE Class 10",
        totalWeightage = "8-10 Marks in Board Exam",
        questions = listOf(
            PyqQuestion(
                id = 1,
                year = "CBSE 2024 (Delhi Set 1)",
                marks = 3,
                questionText = "State Ohm's law. Draw a circuit diagram to verify this law indicating the positive and negative terminals of the ammeter and voltmeter.",
                markingSchemeSteps = listOf(
                    "Ohm's Law statement: Potential difference across ends of a metallic conductor is directly proportional to current flowing through it, provided temperature remains constant (V ∝ I) [1 Mark].",
                    "Circuit Diagram: Series connection of Battery, Plug Key, Ammeter, Resistor/Rheostat and parallel connection of Voltmeter across resistor [1 Mark].",
                    "Polarity: Correct (+) and (-) indications on both ammeter and voltmeter meters [1 Mark]."
                ),
                finalAnswer = "V = IR; Ammeter in series, Voltmeter in parallel with proper terminal polarities."
            ),
            PyqQuestion(
                id = 2,
                year = "CBSE 2023 (All India)",
                marks = 5,
                questionText = "(a) Derive the expression for equivalent resistance of three resistors R₁, R₂, R₃ connected in parallel.\n(b) An electric lamp of 100 Ω, a toaster of 50 Ω, and a water filter of 500 Ω are connected in parallel to a 220 V source. Find the resistance of an electric iron connected to the same source that takes as much current as all three appliances, and what is the current through it?",
                markingSchemeSteps = listOf(
                    "Derivation: In parallel, V is identical across all resistors. Total current I = I₁ + I₂ + I₃ [1 Mark].",
                    "Substitute Ohm's Law: V/R_p = V/R₁ + V/R₂ + V/R₃ ⟹ 1/R_p = 1/R₁ + 1/R₂ + 1/R₃ [2 Marks].",
                    "Equivalent Resistance calculation: 1/R = 1/100 + 1/50 + 1/500 = (5 + 10 + 1)/500 = 16/500 = 4/125 ⟹ R = 125/4 = 31.25 Ω [1 Mark].",
                    "Current calculation: I = V / R = 220 / 31.25 = 7.04 A [1 Mark]."
                ),
                finalAnswer = "Resistance of iron = 31.25 Ω; Total Current = 7.04 A."
            ),
            PyqQuestion(
                id = 3,
                year = "CBSE 2022 (Term II)",
                marks = 2,
                questionText = "Why are coils of electric toasters and electric irons made of an alloy rather than a pure metal?",
                markingSchemeSteps = listOf(
                    "Point 1: Alloys have higher resistivity than their constituent pure metals, producing higher heating effect (H = I²Rt) [1 Mark].",
                    "Point 2: Alloys do not oxidize (burn) readily even at high temperatures [1 Mark]."
                ),
                finalAnswer = "Higher resistivity and resistance to high-temperature oxidation."
            )
        )
    )

    private val lightPyqs = PyqChapter(
        chapterTitle = "Light (Reflection & Refraction)",
        subject = "Physics • CBSE Class 10",
        totalWeightage = "7-9 Marks in Board Exam",
        questions = listOf(
            PyqQuestion(
                id = 1,
                year = "CBSE 2024 (Set 2)",
                marks = 3,
                questionText = "A 4 cm tall object is placed perpendicular to the principal axis of a convex lens of focal length 20 cm. If the distance of the object from the lens is 15 cm, find the position, nature, and size of the image formed.",
                markingSchemeSteps = listOf(
                    "Given: h = +4 cm, f = +20 cm, u = -15 cm. Lens formula: 1/f = 1/v - 1/u ⟹ 1/v = 1/20 - 1/15 = (3 - 4)/60 = -1/60 ⟹ v = -60 cm [1.5 Marks].",
                    "Magnification: m = v/u = (-60)/(-15) = +4 [0.5 Mark].",
                    "Height of image: h' = m · h = 4 × 4 = +16 cm [0.5 Mark].",
                    "Nature: Virtual, erect, and magnified (on the same side as the object) [0.5 Mark]."
                ),
                finalAnswer = "Position: 60 cm in front of lens; Height: 16 cm; Nature: Virtual & Erect."
            )
        )
    )

    private val chemistryPyqs = PyqChapter(
        chapterTitle = "Chemical Reactions & Equations",
        subject = "Chemistry • CBSE Class 10",
        totalWeightage = "5-6 Marks in Board Exam",
        questions = listOf(
            PyqQuestion(
                id = 1,
                year = "CBSE 2023",
                marks = 3,
                questionText = "Identify the substance oxidized, substance reduced, oxidizing agent, and reducing agent in: MnO₂ + 4HCl → MnCl₂ + 2H₂O + Cl₂.",
                markingSchemeSteps = listOf(
                    "Substance oxidized: HCl (loss of hydrogen / oxidation of Cl⁻ to Cl₂) [0.75 Mark].",
                    "Substance reduced: MnO₂ (loss of oxygen / Mn⁴⁺ to Mn²⁺) [0.75 Mark].",
                    "Oxidizing agent: MnO₂ [0.75 Mark].",
                    "Reducing agent: HCl [0.75 Mark]."
                ),
                finalAnswer = "Oxidized: HCl; Reduced: MnO₂; Oxidizing agent: MnO₂; Reducing agent: HCl."
            )
        )
    )

    private val biologyPyqs = PyqChapter(
        chapterTitle = "Life Processes",
        subject = "Biology • CBSE Class 10",
        totalWeightage = "8-9 Marks in Board Exam",
        questions = listOf(
            PyqQuestion(
                id = 1,
                year = "CBSE 2024",
                marks = 5,
                questionText = "Describe the mechanism of double circulation in human beings with the help of a schematic diagram. Why is it necessary?",
                markingSchemeSteps = listOf(
                    "Explanation: Blood travels through the heart twice during each complete cardiac cycle (Pulmonary circulation + Systemic circulation) [2 Marks].",
                    "Flow path: Deoxygenated blood (Body → Right Atrium → Right Ventricle → Lungs) and Oxygenated blood (Lungs → Left Atrium → Left Ventricle → Body) [2 Marks].",
                    "Necessity: Prevents mixing of oxygenated and deoxygenated blood, ensuring highly efficient oxygen delivery to maintain constant warm-blooded body temperature [1 Mark]."
                ),
                finalAnswer = "Double circulation ensures complete separation of oxygenated and deoxygenated blood for high energy efficiency."
            )
        )
    )
}
