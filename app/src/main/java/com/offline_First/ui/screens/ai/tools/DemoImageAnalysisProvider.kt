package com.offline_First.ui.screens.ai.tools

data class ImageAnalysisResult(
    val title: String,
    val subject: String,
    val problemStatement: String,
    val extractedDiagramOrFormula: String,
    val keyConcepts: List<String>,
    val stepByStepSolution: List<String>,
    val finalAnswer: String,
    val proTip: String
)

object DemoImageAnalysisProvider {
    val sampleProblems = listOf(
        "Physics: Pulley & Incline Mechanics",
        "Mathematics: Definite Integration by Parts",
        "Chemistry: Organic Reaction Mechanism",
        "Circuit: Resistor Bridge & Kirchhoff Law"
    )

    fun analyzeProblem(problemQuery: String): ImageAnalysisResult {
        val query = problemQuery.trim().lowercase()
        return when {
            query.contains("math") || query.contains("integral") || query.contains("calc") -> mathIntegralResult
            query.contains("chem") || query.contains("organic") || query.contains("reaction") -> chemistryReactionResult
            query.contains("circuit") || query.contains("resistor") || query.contains("kirchhoff") -> circuitResult
            else -> physicsPulleyResult
        }
    }

    private val physicsPulleyResult = ImageAnalysisResult(
        title = "Incline Plane with Pulley & Suspended Mass",
        subject = "Physics • Classical Mechanics",
        problemStatement = "A block of mass m₁ = 4 kg rests on a frictionless plane inclined at θ = 30°. It is connected by a massless cord passing over a frictionless pulley to a hanging mass m₂ = 3 kg. Find the acceleration of the system.",
        extractedDiagramOrFormula = "System: Inclined plane at 30° ➔ Tension T ➔ Suspended m₂ = 3 kg",
        keyConcepts = listOf("Newton's 2nd Law (F = ma)", "Tension along cord", "Component of gravity m₁g sin(θ)"),
        stepByStepSolution = listOf(
            "Step 1: Identify forces along the incline for block m₁: The component of gravity pulling it down the slope is m₁g sin(30°) = 4 × 9.8 × 0.5 = 19.6 N.",
            "Step 2: Identify downward force on suspended mass m₂: Gravity acting directly downward = m₂g = 3 × 9.8 = 29.4 N.",
            "Step 3: Determine direction of motion: Since m₂g (29.4 N) > m₁g sin(30°) (19.6 N), mass m₂ moves downward and m₁ moves up the incline.",
            "Step 4: Formulate the equations of motion:\n  (1) m₂g - T = m₂ · a\n  (2) T - m₁g sin(30°) = m₁ · a",
            "Step 5: Add equations (1) and (2) to eliminate Tension T:\n  m₂g - m₁g sin(30°) = (m₁ + m₂) · a\n  29.4 - 19.6 = (4 + 3) · a\n  9.8 = 7 · a ⟹ a = 9.8 / 7 = 1.4 m/s²."
        ),
        finalAnswer = "Acceleration a = 1.40 m/s² (m₁ accelerates up the plane, m₂ accelerates downward). Tension T = 25.2 N.",
        proTip = "Always define a single consistent coordinate direction for the entire continuous cord before writing ΣF = ma."
    )

    private val mathIntegralResult = ImageAnalysisResult(
        title = "Definite Integral of x · e^(2x) dx",
        subject = "Mathematics • Integral Calculus",
        problemStatement = "Evaluate the definite integral: ∫ from 0 to 1 of x · e^(2x) dx using integration by parts.",
        extractedDiagramOrFormula = "∫₀¹ x · e^(2x) dx",
        keyConcepts = listOf("Integration by Parts: ∫ u dv = u·v - ∫ v du", "ILATE rule: Algebraic (u = x), Exponential (dv = e^(2x) dx)"),
        stepByStepSolution = listOf(
            "Step 1: Choose u and dv using the ILATE priority rule:\n  Let u = x ⟹ du = dx\n  Let dv = e^(2x) dx ⟹ v = ½ e^(2x)",
            "Step 2: Apply the integration by parts formula:\n  ∫ x · e^(2x) dx = u·v - ∫ v du = ½ x e^(2x) - ∫ ½ e^(2x) dx",
            "Step 3: Integrate the remaining term:\n  ∫ ½ e^(2x) dx = ¼ e^(2x)",
            "Step 4: Combine the indefinite result:\n  [½ x e^(2x) - ¼ e^(2x)] from 0 to 1 = [¼ (2x - 1) e^(2x)]₀¹",
            "Step 5: Evaluate at bounds (upper limit 1, lower limit 0):\n  At x = 1: ¼ (2(1) - 1) e² = ¼ e²\n  At x = 0: ¼ (2(0) - 1) e⁰ = -¼\n  Result = ¼ e² - (-¼) = ¼ (e² + 1)."
        ),
        finalAnswer = "Value = ¼ (e² + 1) ≈ 2.097",
        proTip = "Factor out common exponential terms before evaluating bounds to minimize arithmetic sign errors."
    )

    private val chemistryReactionResult = ImageAnalysisResult(
        title = "Nucleophilic Substitution Mechanism (SN2)",
        subject = "Chemistry • Organic Chemistry",
        problemStatement = "Predict the reaction mechanism and stereochemical outcome when (S)-2-bromobutane is treated with sodium hydroxide (NaOH) in acetone.",
        extractedDiagramOrFormula = "CH₃-CH(Br)-CH₂-CH₃ + OH⁻ ➔ Inversion of Configuration",
        keyConcepts = listOf("SN2 Bimolecular Substitution", "Backside nucleophilic attack", "Walden Inversion"),
        stepByStepSolution = listOf(
            "Step 1: Analyze substrate and nucleophile: 2-bromobutane is a secondary alkyl halide. Hydroxide (OH⁻) is a strong nucleophile, and acetone is a polar aprotic solvent. These conditions strongly favor the SN2 pathway.",
            "Step 2: Concerted Transition State: The nucleophile (OH⁻) attacks the electrophilic carbon from the backside opposite to the leaving group (Br⁻).",
            "Step 3: Stereochemical consequence: Backside attack results in complete inversion of stereochemical configuration at the chiral carbon (Walden Inversion).",
            "Step 4: Final product identification: (S)-2-bromobutane is converted into (R)-butan-2-ol."
        ),
        finalAnswer = "Product: (R)-butan-2-ol via concerted SN2 mechanism with 100% optical inversion.",
        proTip = "Polar aprotic solvents (like acetone or DMSO) do not solvate anions strongly, maximizing the nucleophilicity of OH⁻ for SN2."
    )

    private val circuitResult = ImageAnalysisResult(
        title = "Bridge Resistor Network & Kirchhoff Laws",
        subject = "Physics • Current Electricity",
        problemStatement = "In the bridge circuit shown, four resistors R₁ = 2 Ω, R₂ = 4 Ω, R₃ = 3 Ω, R₄ = 6 Ω are connected to a 12 V battery with a central galvanometer bridge of 5 Ω. Find the bridge current.",
        extractedDiagramOrFormula = "R₁/R₂ = 2/4 = 0.5, R₃/R₄ = 3/6 = 0.5 ➔ Balanced Wheatstone Bridge",
        keyConcepts = listOf("Wheatstone Bridge Condition (R₁/R₂ = R₃/R₄)", "Equipotential nodes", "Kirchhoff's Current Law"),
        stepByStepSolution = listOf(
            "Step 1: Test Wheatstone bridge balance condition: R₁/R₂ = 2/4 = 0.5; R₃/R₄ = 3/6 = 0.5.",
            "Step 2: Balance verification: Since R₁/R₂ = R₃/R₄, the potential at node B equals the potential at node D (V_B = V_D).",
            "Step 3: Bridge current calculation: Since there is zero potential difference across the galvanometer (V_B - V_D = 0), no current flows through the central 5 Ω resistor (I_g = 0 A).",
            "Step 4: Simplify circuit: Remove the central branch. The circuit reduces to two parallel branches:\n  Branch 1: R₁ + R₂ = 2 + 4 = 6 Ω\n  Branch 2: R₃ + R₄ = 3 + 6 = 9 Ω.",
            "Step 5: Equivalent resistance: 1/R_eq = 1/6 + 1/9 = 5/18 ⟹ R_eq = 18/5 = 3.6 Ω. Total current I = V / R_eq = 12 / 3.6 = 3.33 A."
        ),
        finalAnswer = "Bridge Galvanometer Current I_g = 0 A (Balanced Bridge). Total Battery Current I = 3.33 A.",
        proTip = "Always check the cross-ratio condition R₁/R₂ = R₃/R₄ first before writing lengthy Kirchhoff loop mesh equations."
    )
}
