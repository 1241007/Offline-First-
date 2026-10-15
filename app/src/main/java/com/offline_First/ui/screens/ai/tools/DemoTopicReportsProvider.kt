package com.offline_First.ui.screens.ai.tools

data class KeyConceptSummary(
    val title: String,
    val definition: String,
    val formulaOrFact: String
)

data class TopicBreakdownSection(
    val sectionTitle: String,
    val weightage: String,
    val summary: String,
    val keyConcepts: List<KeyConceptSummary>,
    val commonMistakes: List<String>
)

data class TopicReport(
    val topicTitle: String,
    val subject: String,
    val examImportance: String, // e.g. "High Yield • 8-10 Marks in CBSE"
    val difficultyLevel: String, // "Medium", "Hard"
    val estimatedStudyTime: String, // "2.5 Hours"
    val executiveSummary: String,
    val prerequisites: List<String>,
    val sections: List<TopicBreakdownSection>,
    val frequentExamQuestions: List<String>,
    val aiStudyStrategy: List<String>
)

object DemoTopicReportsProvider {
    val sampleTopics = listOf(
        "Newton's Laws of Motion",
        "Photosynthesis & Respiration",
        "Python Asynchronous Programming",
        "Electric Potential & Circuits",
        "Chemical Kinetics & Equilibrium"
    )

    fun getReportForTopic(topicQuery: String): TopicReport {
        val query = topicQuery.trim().lowercase()
        return when {
            query.contains("photo") || query.contains("bio") || query.contains("plant") || query.contains("respir") -> photosynthesisReport
            query.contains("python") || query.contains("async") || query.contains("code") || query.contains("coroutine") -> pythonAsyncReport
            query.contains("electric") || query.contains("circuit") || query.contains("potential") || query.contains("ohm") -> electricityReport
            query.contains("chem") || query.contains("kinet") || query.contains("equil") -> chemistryReport
            else -> newtonsLawsReport
        }
    }

    private val newtonsLawsReport = TopicReport(
        topicTitle = "Newton's Laws of Motion & Dynamics",
        subject = "Physics • Mechanics",
        examImportance = "High Yield (8-12 Marks in Board & Competitive Exams)",
        difficultyLevel = "Moderate to Challenging",
        estimatedStudyTime = "3.5 Hours",
        executiveSummary = "Sir Isaac Newton's three laws formulate the foundation of classical mechanics, describing how external forces govern the acceleration and equilibrium of macroscopic bodies.",
        prerequisites = listOf(
            "Scalars vs Vectors (Resolution of vectors)",
            "Kinematics equations (v = u + at, s = ut + ½at²)",
            "Basic understanding of Free Body Diagrams (FBD)"
        ),
        sections = listOf(
            TopicBreakdownSection(
                sectionTitle = "1. First Law (Law of Inertia)",
                weightage = "2-3 Marks",
                summary = "An object maintains its state of rest or uniform motion in a straight line unless compelled to change that state by an external net force.",
                keyConcepts = listOf(
                    KeyConceptSummary(
                        title = "Inertia & Mass",
                        definition = "Inherent property of matter resisting changes in its state of motion.",
                        formulaOrFact = "Quantitative measure of inertia = Mass (kg)"
                    ),
                    KeyConceptSummary(
                        title = "Equilibrium Condition",
                        definition = "When the vector sum of all forces on a body is zero, acceleration is zero.",
                        formulaOrFact = "ΣF_x = 0, ΣF_y = 0 ⟹ a = 0"
                    )
                ),
                commonMistakes = listOf(
                    "Confusing inertia with force (inertia is a property of mass, not a force).",
                    "Forgetting that uniform velocity also satisfies the first law without net force."
                )
            ),
            TopicBreakdownSection(
                sectionTitle = "2. Second Law (Force & Rate of Momentum)",
                weightage = "4-5 Marks (Derivation + Numericals)",
                summary = "The rate of change of linear momentum is directly proportional to applied force and takes place in the direction of the force.",
                keyConcepts = listOf(
                    KeyConceptSummary(
                        title = "Fundamental Equation",
                        definition = "F = dp/dt. When mass is constant: F = m · a.",
                        formulaOrFact = "1 Newton = 1 kg·m/s²"
                    ),
                    KeyConceptSummary(
                        title = "Impulse-Momentum Theorem",
                        definition = "Integral of force over time equals total change in linear momentum.",
                        formulaOrFact = "J = ∫ F dt = Δp = m(v - u)"
                    )
                ),
                commonMistakes = listOf(
                    "Applying F = ma when mass varies (e.g. rocket burning fuel requires F = dp/dt).",
                    "Neglecting friction when resolving forces along inclined planes."
                )
            ),
            TopicBreakdownSection(
                sectionTitle = "3. Third Law & Conservation of Momentum",
                weightage = "3-4 Marks",
                summary = "To every action, there is always an equal and opposite reaction acting on two mutually interacting bodies.",
                keyConcepts = listOf(
                    KeyConceptSummary(
                        title = "Action-Reaction Pairs",
                        definition = "Forces always occur in simultaneous matched pairs on DIFFERENT bodies.",
                        formulaOrFact = "F_AB = -F_BA"
                    ),
                    KeyConceptSummary(
                        title = "Conservation of Linear Momentum",
                        definition = "In the absence of external forces, total momentum remains strictly conserved.",
                        formulaOrFact = "m₁u₁ + m₂u₂ = m₁v₁ + m₂v₂"
                    )
                ),
                commonMistakes = listOf(
                    "Assuming action and reaction cancel out (they act on separate bodies, so they cannot cancel).",
                    "Ignoring vector directions when calculating recoil velocity."
                )
            )
        ),
        frequentExamQuestions = listOf(
            "State Newton's Second Law and derive F = ma from first principles (3 Marks).",
            "Why does a cricket fielder pull his hands backward while catching a fast-moving ball? (2 Marks).",
            "A rocket of initial mass 5000 kg ejects gas at 1000 m/s relative to the rocket. Calculate thrust when fuel burns at 20 kg/s (3 Marks)."
        ),
        aiStudyStrategy = listOf(
            "Master drawing clean Free-Body Diagrams (FBD) for pulley and inclined plane systems.",
            "Memorize the vector form F_net = ma along each orthogonal axis separately.",
            "Practice 5 numericals on conservation of momentum before attempting exam derivations."
        )
    )

    private val photosynthesisReport = TopicReport(
        topicTitle = "Photosynthesis & Cellular Bioenergetics",
        subject = "Biology • Plant Physiology",
        examImportance = "High Yield (6-8 Marks in Board Exams)",
        difficultyLevel = "Moderate",
        estimatedStudyTime = "2.5 Hours",
        executiveSummary = "Comprehensive biochemical mechanism whereby autotrophs convert solar radiant energy into stable chemical bonds of glucose, releasing oxygen gas as a byproduct.",
        prerequisites = listOf(
            "Chloroplast structure (Stroma, Thylakoids, Grana)",
            "Basic redox reactions (Oxidation = loss of electrons, Reduction = gain)",
            "Structure of ATP and NADPH"
        ),
        sections = listOf(
            TopicBreakdownSection(
                sectionTitle = "1. Light-Dependent Reactions",
                weightage = "3-4 Marks",
                summary = "Occur in the thylakoid membranes; converts light photons into chemical energy currency.",
                keyConcepts = listOf(
                    KeyConceptSummary(
                        title = "Photolysis of Water",
                        definition = "Light-driven splitting of water at Photosystem II (PSII).",
                        formulaOrFact = "2H₂O ⟹ 4H⁺ + 4e⁻ + O₂"
                    ),
                    KeyConceptSummary(
                        title = "Photophosphorylation",
                        definition = "Chemiosmotic synthesis of ATP driven by proton gradient across thylakoid lumen.",
                        formulaOrFact = "Produces ATP + NADPH"
                    )
                ),
                commonMistakes = listOf(
                    "Stating that oxygen comes from CO₂ instead of water (it comes from photolysis of H₂O).",
                    "Confusing cyclic with non-cyclic photophosphorylation electron paths."
                )
            ),
            TopicBreakdownSection(
                sectionTitle = "2. Calvin Cycle (Light-Independent)",
                weightage = "3-4 Marks",
                summary = "Occurs in the stroma; enzymatic assimilation of CO₂ into hexose carbohydrates.",
                keyConcepts = listOf(
                    KeyConceptSummary(
                        title = "Carbon Fixation (RuBisCO)",
                        definition = "Enzyme RuBisCO catalyzes attachment of CO₂ to 5-carbon RuBP.",
                        formulaOrFact = "Yields 2 molecules of 3-PGA"
                    ),
                    KeyConceptSummary(
                        title = "Reduction & Regeneration",
                        definition = "ATP and NADPH reduce 3-PGA to G3P; remaining G3P regenerates RuBP.",
                        formulaOrFact = "6 turns of cycle = 1 Glucose (C₆H₁₂O₆)"
                    )
                ),
                commonMistakes = listOf(
                    "Calling it 'Dark Reactions' and assuming it only happens at night (it relies on products of light reactions).",
                    "Miscounting ATP and NADPH requirement per glucose molecule (18 ATP + 12 NADPH)."
                )
            )
        ),
        frequentExamQuestions = listOf(
            "Draw a well-labeled diagram of a chloroplast and outline sites of light and dark reactions (3 Marks).",
            "Trace the path of an electron from water to NADP⁺ in non-cyclic photophosphorylation (Z-scheme) (5 Marks).",
            "Why is RuBisCO called the most abundant enzyme on Earth? State its dual affinity (2 Marks)."
        ),
        aiStudyStrategy = listOf(
            "Memorize the Z-scheme diagram with electron carriers (PQ, Cytochrome b6f, PC).",
            "Use the EduNova Flashcard Deck to test the exact stoichiometry of the Calvin Cycle.",
            "Compare C3 and C4 pathways regarding photorespiration efficiency."
        )
    )

    private val pythonAsyncReport = TopicReport(
        topicTitle = "Python Asynchronous Programming (asyncio)",
        subject = "Computer Science • Software Architecture",
        examImportance = "Essential Professional / High Yield for Backend Architecture",
        difficultyLevel = "Advanced",
        estimatedStudyTime = "3.0 Hours",
        executiveSummary = "Concurrently managing I/O-bound tasks in a single-threaded event loop through non-blocking coroutines, avoiding threading overhead and race conditions.",
        prerequisites = listOf(
            "Python generators & `yield` syntax",
            "Synchronous I/O vs non-blocking system calls",
            "GIL (Global Interpreter Lock) constraints"
        ),
        sections = listOf(
            TopicBreakdownSection(
                sectionTitle = "1. Event Loop & Coroutines",
                weightage = "Core Paradigm",
                summary = "The central orchestrator continuously dispatching ready tasks and pausing on awaiting handles.",
                keyConcepts = listOf(
                    KeyConceptSummary(
                        title = "`async` / `await` Syntax",
                        definition = "Declares native coroutines that yield control back to the event loop.",
                        formulaOrFact = "`async def` returns a coroutine object; `await` yields execution"
                    ),
                    KeyConceptSummary(
                        title = "Non-blocking Sockets",
                        definition = "OS epoll/kqueue selectors monitor I/O descriptors without blocking main thread.",
                        formulaOrFact = "Ideal for FastAPI, websockets, and database network calls"
                    )
                ),
                commonMistakes = listOf(
                    "Calling synchronous blocking functions (e.g. `time.sleep()` or `requests.get()`) inside an async coroutine.",
                    "Forgetting to `await` a coroutine, causing a RuntimeWarning."
                )
            ),
            TopicBreakdownSection(
                sectionTitle = "2. Concurrent Task Orchestration",
                weightage = "Implementation",
                summary = "Scheduling multiple asynchronous coroutines in parallel.",
                keyConcepts = listOf(
                    KeyConceptSummary(
                        title = "`asyncio.gather(*aws)`",
                        definition = "Runs awaitable objects in the aws sequence concurrently and collects ordered results.",
                        formulaOrFact = "return_exceptions=True prevents single task failure from aborting others"
                    ),
                    KeyConceptSummary(
                        title = "`asyncio.Task` Wrapping",
                        definition = "Schedules execution on the active loop immediately.",
                        formulaOrFact = "`asyncio.create_task(coro)` schedules background execution"
                    )
                ),
                commonMistakes = listOf(
                    "Expecting CPU-bound operations to speed up (asyncio is for I/O; use multiprocessing for CPU tasks).",
                    "Not managing unhandled task exceptions causing silent failures."
                )
            )
        ),
        frequentExamQuestions = listOf(
            "Explain the difference between Multithreading, Multiprocessing, and Asyncio in Python.",
            "Write a snippet demonstrating `asyncio.gather` for fetching 3 endpoints concurrently.",
            "What happens if a coroutine calls `time.sleep(5)` instead of `asyncio.sleep(5)`?"
        ),
        aiStudyStrategy = listOf(
            "Inspect the EduNova backend codebase (`main.py` and `database.py`) to see real async SQLAlchemy sessions in action.",
            "Practice converting a synchronous requests loop into an `httpx.AsyncClient` pipeline."
        )
    )

    private val electricityReport = TopicReport(
        topicTitle = "Electric Potential, Current & Circuits",
        subject = "Physics • Electromagnetism",
        examImportance = "High Yield (8-10 Marks in Board Exams)",
        difficultyLevel = "Moderate",
        estimatedStudyTime = "3.0 Hours",
        executiveSummary = "Theoretical formulation of electrostatic potential difference, resistive power dissipation, and Kirchhoff's circuit principles.",
        prerequisites = listOf(
            "Electric Charge (Q = ne)",
            "Basic algebra & proportional relationships",
            "SI Units (Volt, Ampere, Ohm, Watt)"
        ),
        sections = listOf(
            TopicBreakdownSection(
                sectionTitle = "1. Potential Difference & Ohm's Law",
                weightage = "4 Marks",
                summary = "Work done per unit charge moving between two points and its linear relationship to current.",
                keyConcepts = listOf(
                    KeyConceptSummary(
                        title = "Electric Potential (V)",
                        definition = "V = W / Q. Work done in moving unit positive charge from infinity.",
                        formulaOrFact = "1 Volt = 1 Joule / 1 Coulomb"
                    ),
                    KeyConceptSummary(
                        title = "Ohm's Law & Resistivity",
                        definition = "V = I·R. Resistance R = ρ·(L/A) depends on material, length, and cross-section.",
                        formulaOrFact = "Unit of Resistivity (ρ) = Ω·m"
                    )
                ),
                commonMistakes = listOf(
                    "Confusing Resistance with Resistivity (Resistivity is an intrinsic property, independent of length or area).",
                    "Incorrect parallel resistance formula derivation."
                )
            ),
            TopicBreakdownSection(
                sectionTitle = "2. Heating Effect of Current & Power",
                weightage = "3-4 Marks",
                summary = "Electrical energy converted to thermal energy due to inelastic electron collisions in conductors.",
                keyConcepts = listOf(
                    KeyConceptSummary(
                        title = "Joule's Law of Heating",
                        definition = "Heat produced is directly proportional to square of current, resistance, and time.",
                        formulaOrFact = "H = I² · R · t"
                    ),
                    KeyConceptSummary(
                        title = "Electric Power",
                        definition = "Rate at which electrical energy is consumed in a circuit.",
                        formulaOrFact = "P = V·I = I²·R = V²/R"
                    )
                ),
                commonMistakes = listOf(
                    "Applying P = I²R to parallel circuits where voltage is constant (use P = V²/R instead).",
                    "Forgetting commercial unit conversion: 1 kWh = 3.6 × 10⁶ Joules."
                )
            )
        ),
        frequentExamQuestions = listOf(
            "Deduce the expression for equivalent resistance of three resistors connected in parallel (3 Marks).",
            "Why is tungsten used almost exclusively for filament of electric lamps? (2 Marks).",
            "An electric heater of resistance 8 Ω draws 15 A from the service mains for 2 hours. Calculate the rate at which heat is developed (3 Marks)."
        ),
        aiStudyStrategy = listOf(
            "Draw circuit schematics and label junction currents before writing equations.",
            "Solve the top 3 numericals on commercial units of electricity (kilowatt-hour bills)."
        )
    )

    private val chemistryReport = TopicReport(
        topicTitle = "Chemical Kinetics & Equilibrium",
        subject = "Chemistry • Physical Chemistry",
        examImportance = "High Yield (7-9 Marks in Board Exams)",
        difficultyLevel = "Challenging",
        estimatedStudyTime = "3.5 Hours",
        executiveSummary = "Quantitative study of rates of chemical transformations, reaction order mechanisms, activation energy barriers, and dynamic equilibrium states.",
        prerequisites = listOf(
            "Stoichiometric balancing",
            "Molar concentration (Molarity M = mol/L)",
            "Exothermic vs Endothermic enthalpy profiles"
        ),
        sections = listOf(
            TopicBreakdownSection(
                sectionTitle = "1. Reaction Rate & Rate Laws",
                weightage = "4 Marks",
                summary = "Rate of change of reactant or product concentrations over time.",
                keyConcepts = listOf(
                    KeyConceptSummary(
                        title = "Rate Law & Order",
                        definition = "Rate = k[A]^x[B]^y where (x + y) is the overall reaction order.",
                        formulaOrFact = "Order is determined EXPERIMENTALLY, not from stoichiometry"
                    ),
                    KeyConceptSummary(
                        title = "Arrhenius Equation",
                        definition = "Exponential dependence of rate constant on temperature and activation energy.",
                        formulaOrFact = "k = A · e^(-Ea / RT) ⟹ ln(k₂/k₁) = (Ea/R)·(1/T₁ - 1/T₂)"
                    )
                ),
                commonMistakes = listOf(
                    "Confusing molecularity with order of reaction (molecularity is theoretical and cannot be zero or fractional).",
                    "Units of rate constant 'k' change based on overall reaction order."
                )
            ),
            TopicBreakdownSection(
                sectionTitle = "2. Dynamic Equilibrium & Le Chatelier",
                weightage = "3-4 Marks",
                summary = "State where forward and reverse reaction rates are exactly equal.",
                keyConcepts = listOf(
                    KeyConceptSummary(
                        title = "Equilibrium Constant (Kc)",
                        definition = "Ratio of product concentrations to reactant concentrations at equilibrium.",
                        formulaOrFact = "Kc = [C]^c[D]^d / ([A]^a[B]^b)"
                    ),
                    KeyConceptSummary(
                        title = "Le Chatelier's Principle",
                        definition = "System shifts position of equilibrium to relieve any applied stress.",
                        formulaOrFact = "Increasing pressure shifts toward fewer gas moles"
                    )
                ),
                commonMistakes = listOf(
                    "Thinking catalysts change the equilibrium constant (catalysts only speed up reaching equilibrium).",
                    "Including pure solids or pure liquids in the equilibrium expression."
                )
            )
        ),
        frequentExamQuestions = listOf(
            "Distinguish between order and molecularity of a chemical reaction (3 Marks).",
            "Derive the integrated rate equation for a first-order reaction and show half-life is independent of initial concentration (5 Marks).",
            "State Le Chatelier's principle and apply it to Haber's synthesis of ammonia: N₂ + 3H₂ ⇌ 2NH₃ (ΔH < 0) (3 Marks)."
        ),
        aiStudyStrategy = listOf(
            "Memorize units of k: (mol/L)^(1-n) · s⁻¹ where n is reaction order.",
            "Practice the half-life derivation t_1/2 = 0.693 / k for first-order reactions."
        )
    )
}
