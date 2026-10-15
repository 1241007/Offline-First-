package com.offline_First.ui.screens.ai.tools

data class FlashcardItem(
    val id: Int,
    val question: String,
    val answer: String,
    val hint: String,
    val category: String
)

data class FlashcardDeck(
    val deckTitle: String,
    val subject: String,
    val totalCards: Int,
    val cards: List<FlashcardItem>
)

object DemoFlashcardProvider {
    val sampleTopics = listOf(
        "Physics Core Laws",
        "Cell Biology & Genetics",
        "Python Key Concepts",
        "Calculus & Trigonometry",
        "Organic Chemistry Basics"
    )

    fun getDeckForTopic(topicQuery: String): FlashcardDeck {
        val query = topicQuery.trim().lowercase()
        return when {
            query.contains("bio") || query.contains("cell") || query.contains("gene") -> biologyDeck
            query.contains("python") || query.contains("code") || query.contains("prog") -> pythonDeck
            query.contains("math") || query.contains("calc") || query.contains("trig") -> mathDeck
            query.contains("chem") || query.contains("organic") || query.contains("acid") -> chemistryDeck
            else -> physicsDeck
        }
    }

    private val physicsDeck = FlashcardDeck(
        deckTitle = "Physics Core Laws & Units",
        subject = "Physics • Foundation",
        totalCards = 5,
        cards = listOf(
            FlashcardItem(
                id = 1,
                question = "What is Newton's First Law of Motion?",
                answer = "An object remains in a state of rest or uniform motion in a straight line unless acted upon by an external unbalanced force (Law of Inertia).",
                hint = "Think about inertia.",
                category = "Mechanics"
            ),
            FlashcardItem(
                id = 2,
                question = "State Ohm's Law and its mathematical formula.",
                answer = "The electric current through a conductor between two points is directly proportional to the voltage across the two points: V = I × R.",
                hint = "Relates Voltage, Current, and Resistance.",
                category = "Electricity"
            ),
            FlashcardItem(
                id = 3,
                question = "What is the difference between Speed and Velocity?",
                answer = "Speed is a scalar quantity (magnitude only, e.g., 60 km/h). Velocity is a vector quantity (magnitude + direction, e.g., 60 km/h North).",
                hint = "Scalar vs Vector.",
                category = "Kinematics"
            ),
            FlashcardItem(
                id = 4,
                question = "What is the Law of Conservation of Energy?",
                answer = "Energy cannot be created or destroyed; it can only be transformed from one form into another. Total energy in an isolated system remains constant.",
                hint = "Transformation without loss.",
                category = "Thermodynamics"
            ),
            FlashcardItem(
                id = 5,
                question = "What is Snell's Law of Refraction?",
                answer = "n₁ sin(θ₁) = n₂ sin(θ₂). The ratio of the sine of the angle of incidence to the sine of the angle of refraction is a constant equal to the refractive index.",
                hint = "Bending of light across media.",
                category = "Optics"
            )
        )
    )

    private val pythonDeck = FlashcardDeck(
        deckTitle = "Python Core Essentials",
        subject = "Computer Science",
        totalCards = 5,
        cards = listOf(
            FlashcardItem(
                id = 1,
                question = "What is the difference between a List and a Tuple in Python?",
                answer = "Lists are mutable (can be changed, defined with []) whereas Tuples are immutable (read-only after creation, defined with ()).",
                hint = "Mutability.",
                category = "Data Types"
            ),
            FlashcardItem(
                id = 2,
                question = "What does the `GIL` (Global Interpreter Lock) do?",
                answer = "A mutex that protects access to Python objects, preventing multiple native threads from executing Python bytecodes at once in CPython.",
                hint = "Threading constraint in CPython.",
                category = "Runtime"
            ),
            FlashcardItem(
                id = 3,
                question = "How does Python handle memory management?",
                answer = "Through automatic reference counting combined with a cyclic garbage collector to detect and collect reference cycles.",
                hint = "Ref counting + GC.",
                category = "Architecture"
            ),
            FlashcardItem(
                id = 4,
                question = "What is a Generator in Python and why is it useful?",
                answer = "A function that yields values one at a time using `yield`. It computes values lazily on-the-fly, saving memory compared to loading a full list.",
                hint = "Lazy evaluation using `yield`.",
                category = "Advanced"
            ),
            FlashcardItem(
                id = 5,
                question = "What are *args and **kwargs?",
                answer = "`*args` allows passing a variable number of positional arguments as a tuple. `**kwargs` allows passing a variable number of keyword arguments as a dictionary.",
                hint = "Variable function arguments.",
                category = "Functions"
            )
        )
    )

    private val biologyDeck = FlashcardDeck(
        deckTitle = "Cell Biology & Genetics",
        subject = "Biology • Class 10/11",
        totalCards = 4,
        cards = listOf(
            FlashcardItem(
                id = 1,
                question = "Why is Mitochondria called the powerhouse of the cell?",
                answer = "It synthesizes adenosine triphosphate (ATP), the primary energy currency required for cellular metabolic activities, via aerobic respiration.",
                hint = "ATP generation.",
                category = "Cytology"
            ),
            FlashcardItem(
                id = 2,
                question = "What are the four nucleotide nitrogenous bases in DNA?",
                answer = "Adenine (A), Thymine (T), Guanine (G), and Cytosine (C). A pairs with T (2 hydrogen bonds) and G pairs with C (3 hydrogen bonds).",
                hint = "A-T and G-C pairs.",
                category = "Genetics"
            ),
            FlashcardItem(
                id = 3,
                question = "What is the primary function of Ribosomes?",
                answer = "Protein synthesis (translation): reading mRNA transcripts and assembling corresponding amino acid chains.",
                hint = "Protein factory.",
                category = "Cytology"
            ),
            FlashcardItem(
                id = 4,
                question = "Define Osmosis.",
                answer = "The movement of water molecules from a region of higher water potential (lower solute concentration) to lower water potential through a selectively permeable membrane.",
                hint = "Water diffusion across semipermeable membrane.",
                category = "Cell Transport"
            )
        )
    )

    private val mathDeck = FlashcardDeck(
        deckTitle = "Calculus & Trigonometry Identities",
        subject = "Mathematics",
        totalCards = 4,
        cards = listOf(
            FlashcardItem(
                id = 1,
                question = "What is the fundamental Pythagorean trigonometric identity?",
                answer = "sin²(θ) + cos²(θ) = 1. Also 1 + tan²(θ) = sec²(θ) and 1 + cot²(θ) = csc²(θ).",
                hint = "Square of sine plus cosine.",
                category = "Trigonometry"
            ),
            FlashcardItem(
                id = 2,
                question = "What is the Product Rule in differentiation?",
                answer = "d/dx [u(x) · v(x)] = u'(x) · v(x) + u(x) · v'(x).",
                hint = "Derivative of first times second + ...",
                category = "Calculus"
            ),
            FlashcardItem(
                id = 3,
                question = "What is the derivative of ln(x) and e^x?",
                answer = "d/dx [ln(x)] = 1/x (for x > 0), and d/dx [e^x] = e^x.",
                hint = "Logarithmic and exponential derivatives.",
                category = "Calculus"
            ),
            FlashcardItem(
                id = 4,
                question = "What does the definite integral ∫[a to b] f(x) dx represent geometrically?",
                answer = "The net signed area between the curve y = f(x) and the x-axis from x = a to x = b.",
                hint = "Area under the curve.",
                category = "Integration"
            )
        )
    )

    private val chemistryDeck = FlashcardDeck(
        deckTitle = "Organic & General Chemistry",
        subject = "Chemistry",
        totalCards = 4,
        cards = listOf(
            FlashcardItem(
                id = 1,
                question = "What is Le Chatelier's Principle?",
                answer = "If a dynamic equilibrium is disturbed by changing the conditions (concentration, temperature, or pressure), the position of equilibrium shifts to counteract the change.",
                hint = "Equilibrium self-adjustment.",
                category = "Physical Chemistry"
            ),
            FlashcardItem(
                id = 2,
                question = "What is the general formula of Alkanes, Alkenes, and Alkynes?",
                answer = "Alkanes (single bond): CnH2n+2\nAlkenes (double bond): CnH2n\nAlkynes (triple bond): CnH2n-2",
                hint = "Saturated vs unsaturated hydrocarbons.",
                category = "Organic Chemistry"
            ),
            FlashcardItem(
                id = 3,
                question = "What is an Acid and a Base according to the Brønsted-Lowry definition?",
                answer = "An acid is a proton (H⁺) donor, and a base is a proton (H⁺) acceptor.",
                hint = "Proton donor vs acceptor.",
                category = "Inorganic"
            ),
            FlashcardItem(
                id = 4,
                question = "What is Avogadro's Number and what does it represent?",
                answer = "6.022 × 10²³ particles/mol. It is the number of constituent atoms, molecules, or ions in exactly one mole of a substance.",
                hint = "Particles in one mole.",
                category = "Stoichiometry"
            )
        )
    )
}
