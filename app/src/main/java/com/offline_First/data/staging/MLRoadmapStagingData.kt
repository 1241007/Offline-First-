package com.offline_First.data.staging

/**
 * Complete Staging Data for EduNova Machine Learning Roadmap.
 * Strictly aligned with Roadmap.sh Machine Learning structure (Screenshots 3 & 4)
 * and unified with EduNova's Java styling.
 */
object MLRoadmapStagingData {

    // --- FREQUENTLY ASKED QUESTIONS ---
    val ML_FAQS = listOf(
        RoadmapFaqItem(
            id = "ml_faq_1",
            question = "What is a Machine Learning Engineer?",
            answer = "A Machine Learning Engineer researches, builds, and designs self-running artificial intelligence systems to automate predictive models. They bridge the gap between theoretical data science and production software engineering, turning models into scalable services."
        ),
        RoadmapFaqItem(
            id = "ml_faq_2",
            question = "What mathematics do I need to learn first?",
            answer = "The essential math pillars are Linear Algebra (matrix ops, vectors, eigenvalues), Calculus (derivatives, gradients, chain rule for backpropagation), and Probability & Statistics (distributions, Bayes theorem, hypothesis testing)."
        ),
        RoadmapFaqItem(
            id = "ml_faq_3",
            question = "Which programming language should I learn?",
            answer = "Python is the undisputed industry standard for Machine Learning and AI due to its rich ecosystem of optimized libraries: NumPy, Pandas, Scikit-learn, PyTorch, and TensorFlow."
        ),
        RoadmapFaqItem(
            id = "ml_faq_4",
            question = "What is the difference between Supervised and Unsupervised learning?",
            answer = "Supervised learning trains algorithms on labeled datasets where ground truth outputs are known (classification, regression). Unsupervised learning discovers hidden patterns or groupings in unlabeled datasets (clustering, dimensionality reduction)."
        ),
        RoadmapFaqItem(
            id = "ml_faq_5",
            question = "When should I transition from classical ML to Deep Learning?",
            answer = "Master classical tabular algorithms (Linear/Logistic Regression, Random Forests, Gradient Boosted Trees) first. Transition to Deep Learning (CNNs, RNNs, Transformers) when working with unstructured perceptual data like images, audio, video, or natural language."
        )
    )

    // --- PRACTICE PROJECTS ---
    val ML_PROJECTS = listOf(
        RoadmapProjectItem(
            id = "ml_proj_1",
            title = "Customer Churn Prediction",
            difficulty = "Beginner",
            estimatedHours = 12,
            description = "Build a binary classification pipeline using Scikit-learn to identify customers at risk of canceling subscriptions.",
            relevantSkills = listOf("Pandas", "Scikit-learn", "Logistic Regression", "ROC-AUC", "Feature Scaling"),
            deliverables = listOf(
                "Exploratory data analysis Jupyter notebook",
                "Clean preprocessing pipeline handling missing data",
                "Trained classification models (Logistic Regression, Random Forest)",
                "Evaluation report with Precision, Recall, and Confusion Matrix"
            ),
            instructions = "1. Load telecom customer churn dataset. 2. Encode categorical features. 3. Split 80/20 train/test. 4. Evaluate with F1-score."
        ),
        RoadmapProjectItem(
            id = "ml_proj_2",
            title = "House Price Regression Model",
            difficulty = "Intermediate",
            estimatedHours = 16,
            description = "Develop a regularized polynomial regression model predicting real estate valuation with feature engineering and cross-validation.",
            relevantSkills = listOf("Ridge / Lasso", "Cross-Validation", "Feature Engineering", "Matplotlib", "Seaborn"),
            deliverables = listOf(
                "Feature correlation heatmaps",
                "Outlier detection and log transformation of target price",
                "Hyperparameter grid search for regularizer alpha",
                "Residual distribution diagnostic plots"
            ),
            instructions = "1. Clean features and impute missing numerical data. 2. Tune Ridge and ElasticNet models. 3. Benchmark RMSE scores."
        ),
        RoadmapProjectItem(
            id = "ml_proj_3",
            title = "Image Classification with CNN",
            difficulty = "Advanced",
            estimatedHours = 24,
            description = "Train a deep Convolutional Neural Network with PyTorch to classify multi-class images with data augmentation.",
            relevantSkills = listOf("PyTorch / TensorFlow", "CNNs", "Transfer Learning", "Data Augmentation", "GPU Training"),
            deliverables = listOf(
                "Custom PyTorch Dataset and DataLoader with transforms",
                "CNN architecture with Conv2D, BatchNorm, MaxPooling, and Dropout",
                "Training loop with loss/accuracy curves per epoch",
                "Saved model weights and inference prediction script"
            ),
            instructions = "1. Set up PyTorch Conv2D layers. 2. Apply CrossEntropyLoss and Adam optimizer. 3. Fine-tune pre-trained ResNet."
        ),
        RoadmapProjectItem(
            id = "ml_proj_4",
            title = "Sentiment Analysis with Transformers",
            difficulty = "Advanced",
            estimatedHours = 30,
            description = "Fine-tune a Hugging Face Transformer model for sentiment classification on customer product reviews.",
            relevantSkills = listOf("Hugging Face", "Transformers", "BERT / DistilBERT", "Tokenization", "Model Deployment"),
            deliverables = listOf(
                "Tokenized text pipeline with subword tokenizers",
                "Fine-tuned DistilBERT model with early stopping",
                "ONNX runtime export for fast inference",
                "FastAPI REST endpoint serving real-time predictions"
            ),
            instructions = "1. Tokenize dataset with Hugging Face AutoTokenizer. 2. Fine-tune with Trainer API. 3. Deploy REST API."
        )
    )

    // --- VISUAL NODES FOR MACHINE LEARNING ROADMAP ---
    // Root & Introduction
    val NODE_ROOT = VisualRoadmapNode(
        id = "ml_root",
        title = "Machine Learning",
        section = "Start",
        isMajor = true,
        badge = "Start",
        explanation = "The starting milestone for your machine learning engineering journey.",
        objectives = listOf("Understand core ML concepts", "Navigate mathematical & programming foundations"),
        codeSnippet = "# Python ML Setup\nimport numpy as np\nimport pandas as pd\nimport sklearn"
    )

    val NODE_INTRO = VisualRoadmapNode(
        id = "ml_intro",
        title = "Introduction",
        section = "Overview",
        isMajor = true,
        badge = "Milestone 1",
        explanation = "Introduction to the role of machine learning in software and modern technology.",
        objectives = listOf("Understand what an ML engineer does", "Distinguish ML from classical AI and data science"),
        codeSnippet = "# Machine Learning Overview\n# Experience E improves performance P on task T"
    )

    val NODE_WHAT_IS_ML_ENG = VisualRoadmapNode(
        id = "ml_what_is_ml_eng",
        title = "What is an ML Engineer?",
        section = "Overview",
        explanation = "An ML engineer designs and deploys machine learning models into production systems.",
        objectives = listOf("Model lifecycle", "MLOps integration", "Scalable serving"),
        codeSnippet = "# Bridging theoretical models with production architecture"
    )

    val NODE_ML_VS_AI = VisualRoadmapNode(
        id = "ml_vs_ai",
        title = "ML Engineer vs AI Engineer",
        section = "Overview",
        explanation = "AI Engineers often focus on LLM orchestration, while ML Engineers focus on custom training and architectures.",
        objectives = listOf("Role distinctions", "Tooling differences"),
        codeSnippet = "# ML Engineer: Custom models & algorithms\n# AI Engineer: Foundation model applications"
    )

    val NODE_SKILLS_RESP = VisualRoadmapNode(
        id = "ml_skills_resp",
        title = "Skills and Responsibilities",
        section = "Overview",
        explanation = "Core competencies include Python, statistics, algorithms, distributed training, and cloud deployment.",
        objectives = listOf("Statistical analysis", "Software architecture", "Model monitoring"),
        codeSnippet = "# Key skills: Python, PyTorch, Math, Docker, Kubernetes"
    )

    // Mathematical Foundations
    val NODE_MATH_FOUNDATIONS = VisualRoadmapNode(
        id = "ml_math_foundations",
        title = "Mathematical Foundations",
        section = "Mathematics",
        isMajor = true,
        badge = "Milestone 2",
        explanation = "The mathematical foundation required to understand how optimization algorithms learn.",
        objectives = listOf("Linear algebra", "Multivariate calculus", "Probability & statistics"),
        codeSnippet = "# Core Math: Matrix Operations & Derivatives"
    )

    val NODE_LINEAR_ALGEBRA = VisualRoadmapNode(
        id = "ml_linear_algebra",
        title = "Linear Algebra",
        section = "Mathematics",
        isMajor = true,
        explanation = "Vectors, matrices, tensors, and linear transformations form the bedrock of neural network operations.",
        objectives = listOf("Matrix multiplication", "Eigendecomposition", "SVD decomposition"),
        codeSnippet = "import numpy as np\nA = np.array([[1, 2], [3, 4]])\neigenvalues, eigenvectors = np.linalg.eig(A)"
    )

    val NODE_MATRIX_OPS = VisualRoadmapNode(id = "ml_matrix_ops", title = "Matrix & Matrix Operations", section = "Mathematics", explanation = "Matrix dot products, transpositions, and inversions.", objectives = listOf("Dot product", "Transpose", "Determinant"), codeSnippet = "C = np.dot(A, B)")
    val NODE_SCALARS_VECTORS = VisualRoadmapNode(id = "ml_scalars_vectors", title = "Scalars, Vectors, Tensors", section = "Mathematics", explanation = "0D, 1D, and n-dimensional arrays of numerical values.", objectives = listOf("Tensor rank", "Dimensionality", "Broadcasting"), codeSnippet = "t = torch.randn(3, 3, 3)")
    val NODE_SVD = VisualRoadmapNode(id = "ml_svd", title = "Singular Value Decomposition", section = "Mathematics", explanation = "Factorization of a matrix into orthonormal matrices and singular values.", objectives = listOf("Dimensionality reduction", "Latent semantic analysis"), codeSnippet = "U, S, Vt = np.linalg.svd(A)")
    val NODE_DETERMINANTS = VisualRoadmapNode(id = "ml_determinants", title = "Determinants, Inverse of Matrix", section = "Mathematics", explanation = "Scaling factor of transformations and invertible operations.", objectives = listOf("Inverses", "Matrix singularity"), codeSnippet = "inv_A = np.linalg.inv(A)")
    val NODE_EIGENVALUES = VisualRoadmapNode(id = "ml_eigenvalues", title = "Eigenvalues, Diagonalization", section = "Mathematics", explanation = "Directions where linear transformation stretches without rotating.", objectives = listOf("PCA foundation", "Spectral decomposition"), codeSnippet = "vals, vecs = np.linalg.eig(A)")

    val NODE_CALCULUS = VisualRoadmapNode(
        id = "ml_calculus",
        title = "Calculus",
        section = "Mathematics",
        isMajor = true,
        explanation = "Derivatives and gradients drive cost function minimization via gradient descent.",
        objectives = listOf("Partial derivatives", "Gradient vector", "Chain rule of calculus"),
        codeSnippet = "# Gradient Descent update rule\n# w = w - learning_rate * dL_dw"
    )

    val NODE_DERIVATIVES = VisualRoadmapNode(id = "ml_derivatives", title = "Derivatives, Partial Derivatives", section = "Mathematics", explanation = "Rate of change of a function with respect to individual variables.", objectives = listOf("Slopes", "Multivariable functions"), codeSnippet = "df_dx = 2 * x")
    val NODE_CHAIN_RULE = VisualRoadmapNode(id = "ml_chain_rule", title = "Chain rule of derivation", section = "Mathematics", explanation = "Fundamental rule for computing gradients in layered neural networks.", objectives = listOf("Composite functions", "Backpropagation"), codeSnippet = "dz_dx = (dz_dy) * (dy_dx)")
    val NODE_GRADIENT_HESSIAN = VisualRoadmapNode(id = "ml_gradient_hessian", title = "Gradient, Jacobian, Hessian", section = "Mathematics", explanation = "First and second-order derivatives in multivariable vector calculus.", objectives = listOf("Gradient vectors", "Curvature & Hessians"), codeSnippet = "# Jacobian: First-order partials\n# Hessian: Second-order partials")

    val NODE_DISCRETE_MATH = VisualRoadmapNode(id = "ml_discrete_math", title = "Discrete Mathematics", section = "Mathematics", isMajor = true, explanation = "Graph theory, sets, and combinatorics for algorithm design.", objectives = listOf("Graph structures", "Combinatorics"), codeSnippet = "# Set logic and tree graphs")

    val NODE_STATISTICS = VisualRoadmapNode(
        id = "ml_statistics",
        title = "Statistics",
        section = "Mathematics",
        isMajor = true,
        explanation = "Descriptive and inferential statistics for dataset understanding and hypothesis testing.",
        objectives = listOf("Mean, median, variance", "Hypothesis testing", "P-values and confidence intervals"),
        codeSnippet = "import scipy.stats as stats\nt_stat, p_val = stats.ttest_ind(group_a, group_b)"
    )

    val NODE_BASIC_STATS = VisualRoadmapNode(id = "ml_basic_stats", title = "Basic concepts", section = "Mathematics", explanation = "Measures of central tendency and dispersion.", objectives = listOf("Mean, Variance, Std Dev"), codeSnippet = "mean = np.mean(data)")
    val NODE_DESCRIPTIVE_STATS = VisualRoadmapNode(id = "ml_descriptive_stats", title = "Descriptive Statistics", section = "Mathematics", explanation = "Summarizing data distributions via percentiles, skewness, and kurtosis.", objectives = listOf("Quantiles", "Interquartile range"), codeSnippet = "df.describe()")
    val NODE_GRAPHS_CHARTS = VisualRoadmapNode(id = "ml_graphs_charts", title = "Graphs & Charts", section = "Mathematics", explanation = "Histograms, box plots, scatter plots, and QQ plots.", objectives = listOf("Visualizing variance", "Correlation inspection"), codeSnippet = "sns.histplot(data['feature'])")
    val NODE_INFERENTIAL_STATS = VisualRoadmapNode(id = "ml_inferential_stats", title = "Inferential Statistics", section = "Mathematics", explanation = "Drawing conclusions about populations from sample data.", objectives = listOf("Confidence intervals", "A/B testing"), codeSnippet = "stats.norm.interval(0.95)")

    val NODE_PROBABILITY = VisualRoadmapNode(
        id = "ml_probability",
        title = "Probability",
        section = "Mathematics",
        isMajor = true,
        explanation = "Quantifying uncertainty, conditional likelihood, and stochastic models.",
        objectives = listOf("Bayes theorem", "Random variables", "Probability density functions"),
        codeSnippet = "# Bayes Rule: P(A|B) = P(B|A) * P(A) / P(B)"
    )

    val NODE_BASICS_PROB = VisualRoadmapNode(id = "ml_basics_prob", title = "Basics of Probability", section = "Mathematics", explanation = "Sample spaces, events, mutually exclusive events, and independence.", objectives = listOf("Independence", "Joint probability"), codeSnippet = "P_A_and_B = P_A * P_B")
    val NODE_BAYES_THEOREM = VisualRoadmapNode(id = "ml_bayes_theorem", title = "Bayes Theorem", section = "Mathematics", explanation = "Updating prior probabilities given new observational evidence.", objectives = listOf("Prior, Likelihood, Posterior"), codeSnippet = "P_posterior = (likelihood * prior) / evidence")
    val NODE_RANDOM_VARS = VisualRoadmapNode(id = "ml_random_vars", title = "Random Variables, PDFs", section = "Mathematics", explanation = "Discrete and continuous random variables, CDFs and PDFs.", objectives = listOf("Probability mass functions", "Density functions"), codeSnippet = "pdf = stats.norm.pdf(x, loc=0, scale=1)")
    val NODE_DISTRIBUTIONS = VisualRoadmapNode(id = "ml_distributions", title = "Types of Distribution", section = "Mathematics", explanation = "Gaussian (Normal), Binomial, Poisson, and Bernoulli distributions.", objectives = listOf("Normal distribution", "Central Limit Theorem"), codeSnippet = "samples = np.random.normal(0, 1, 1000)")

    // Programming Fundamentals
    val NODE_PROGRAMMING_FUND = VisualRoadmapNode(
        id = "ml_prog_fund",
        title = "Programming Fundamentals",
        section = "Programming",
        isMajor = true,
        badge = "Milestone 3",
        explanation = "Mastering programming constructs in Python for numerical data processing.",
        objectives = listOf("Python syntax & OOP", "Vectorized computing with NumPy", "DataFrames with Pandas"),
        codeSnippet = "# Python Data Processing\nimport numpy as np\nimport pandas as pd"
    )

    val NODE_PYTHON = VisualRoadmapNode(
        id = "ml_python",
        title = "Python",
        section = "Programming",
        isMajor = true,
        explanation = "Python is the primary language of AI/ML development.",
        objectives = listOf("Syntax, structures, functions", "Vectorized libraries"),
        codeSnippet = "def train_step(x, y):\n    return model(x).backward()"
    )

    val NODE_PY_SYNTAX = VisualRoadmapNode(id = "ml_py_syntax", title = "Basic Syntax", section = "Programming", explanation = "Whitespace, variables, dynamic typing, operators.", objectives = listOf("Indentation", "Type casting"), codeSnippet = "x = [1, 2, 3]")
    val NODE_PY_VARS = VisualRoadmapNode(id = "ml_py_vars", title = "Variables and Data Types", section = "Programming", explanation = "Integers, floats, booleans, strings.", objectives = listOf("Primitive types", "Casting"), codeSnippet = "lr = 0.001")
    val NODE_PY_DATA_STRUCTS = VisualRoadmapNode(id = "ml_py_data_structs", title = "Data Structures", section = "Programming", explanation = "Lists, tuples, sets, and dictionaries.", objectives = listOf("Dict lookups", "List comprehensions"), codeSnippet = "lookup = {'cat': 0, 'dog': 1}")
    val NODE_PY_LOOPS = VisualRoadmapNode(id = "ml_py_loops", title = "Loops", section = "Programming", explanation = "For loops, while loops, and iterators.", objectives = listOf("Epoch loops", "Enumerate"), codeSnippet = "for epoch in range(10): pass")
    val NODE_PY_CONDITIONALS = VisualRoadmapNode(id = "ml_py_conditionals", title = "Conditionals", section = "Programming", explanation = "If, elif, else flow control.", objectives = listOf("Early stopping check"), codeSnippet = "if loss < min_loss: save()")
    val NODE_PY_EXCEPTIONS = VisualRoadmapNode(id = "ml_py_exceptions", title = "Exceptions", section = "Programming", explanation = "Defensive error handling in ETL scripts.", objectives = listOf("Try/except blocks"), codeSnippet = "try: load_data()\nexcept FileNotFoundError: pass")
    val NODE_PY_FUNCTIONS = VisualRoadmapNode(id = "ml_py_functions", title = "Functions, Builtin Functions", section = "Programming", explanation = "Modular functions and built-in iterables.", objectives = listOf("Def statements", "Lambda functions"), codeSnippet = "relu = lambda x: max(0, x)")

    val NODE_OOP = VisualRoadmapNode(
        id = "ml_oop",
        title = "Object Oriented Programming",
        section = "Programming",
        isMajor = true,
        explanation = "Classes and inheritance for creating reusable estimators and neural network modules.",
        objectives = listOf("Custom PyTorch Modules", "Scikit-learn BaseEstimator"),
        codeSnippet = "class Net(nn.Module):\n    def __init__(self):\n        super().__init__()\n        self.fc = nn.Linear(10, 2)"
    )

    val NODE_ESSENTIAL_LIBS = VisualRoadmapNode(
        id = "ml_essential_libs",
        title = "Essential Libraries",
        section = "Programming",
        isMajor = true,
        explanation = "NumPy for tensors, Pandas for dataframes, Matplotlib and Seaborn for plotting.",
        objectives = listOf("NumPy vectorization", "Pandas queries", "Plotting curves"),
        codeSnippet = "import numpy as np, pandas as pd, matplotlib.pyplot as plt, seaborn as sns"
    )

    val NODE_NUMPY = VisualRoadmapNode(id = "ml_numpy", title = "Numpy", section = "Programming", explanation = "N-dimensional array library with C-speed vectorization.", objectives = listOf("Array slicing", "Broadcasting"), codeSnippet = "arr = np.zeros((3, 3))")
    val NODE_PANDAS = VisualRoadmapNode(id = "ml_pandas", title = "Pandas", section = "Programming", explanation = "Data manipulation with Series and DataFrames.", objectives = listOf("Groupby", "Merge", "Missing values"), codeSnippet = "df = pd.read_csv('data.csv')")
    val NODE_MATPLOTLIB = VisualRoadmapNode(id = "ml_matplotlib", title = "Matplotlib", section = "Programming", explanation = "Standard 2D plotting library.", objectives = listOf("Figure and axes", "Line plots"), codeSnippet = "plt.plot(epochs, losses)")
    val NODE_SEABORN = VisualRoadmapNode(id = "ml_seaborn", title = "Seaborn", section = "Programming", explanation = "Statistical data visualization built on Matplotlib.", objectives = listOf("Heatmaps", "Pair plots"), codeSnippet = "sns.heatmap(df.corr())")

    // Data Preparation & Cleaning
    val NODE_DATA_COLLECTION = VisualRoadmapNode(
        id = "ml_data_collection",
        title = "Data Collection",
        section = "Data Preparation",
        isMajor = true,
        badge = "Milestone 4",
        explanation = "Gathering raw training data from relational databases, APIs, web scraping, and IoT streams.",
        objectives = listOf("SQL data querying", "REST API ingestion", "Stream ingestion"),
        codeSnippet = "import requests\nres = requests.get('https://api.edunova.com/sensors').json()"
    )

    val NODE_DATA_SOURCES = VisualRoadmapNode(
        id = "ml_data_sources",
        title = "Data Sources",
        section = "Data Preparation",
        isMajor = true,
        explanation = "Databases (SQL, NoSQL), APIs, internet scraping, and mobile device logs.",
        objectives = listOf("Relational extraction", "Semi-structured JSON"),
        codeSnippet = "# Databases, APIs, Web Scraping, IoT sensors"
    )

    val NODE_DATA_FORMATS = VisualRoadmapNode(
        id = "ml_data_formats",
        title = "Data Formats",
        section = "Data Preparation",
        isMajor = true,
        explanation = "Optimized storage formats: CSV, Excel, JSON, Parquet, and Feather.",
        objectives = listOf("Columnar Parquet storage", "Fast binary deserialization"),
        codeSnippet = "df.to_parquet('data.parquet', compression='snappy')"
    )

    val NODE_PREPROCESSING = VisualRoadmapNode(
        id = "ml_preprocessing",
        title = "Preprocessing Techniques",
        section = "Data Preparation",
        isMajor = true,
        explanation = "Cleaning noisy data, imputing nulls, scaling features, and one-hot encoding.",
        objectives = listOf("StandardScaler / MinMaxScaler", "OneHotEncoder", "Imputation"),
        codeSnippet = "from sklearn.preprocessing import StandardScaler\nscaler = StandardScaler()\nX_scaled = scaler.fit_transform(X)"
    )

    val NODE_DATA_CLEANING = VisualRoadmapNode(id = "ml_data_cleaning", title = "Data Cleaning", section = "Data Preparation", explanation = "Handling duplicate records, missing values, and corrupted data.", objectives = listOf("Imputation", "Outlier removal"), codeSnippet = "df.dropna(subset=['target'])")
    val NODE_FEATURE_ENG = VisualRoadmapNode(id = "ml_feature_eng", title = "Feature Engineering", section = "Data Preparation", explanation = "Creating domain-specific signals from existing raw columns.", objectives = listOf("Interaction terms", "Date part extraction"), codeSnippet = "df['is_weekend'] = df['day'] >= 5")
    val NODE_SCALING_NORM = VisualRoadmapNode(id = "ml_scaling_norm", title = "Feature Scaling & Normalization", section = "Data Preparation", explanation = "Ensuring features have comparable scales for gradient descent.", objectives = listOf("Z-score scaling", "Min-max normalization"), codeSnippet = "X_norm = (X - X.min()) / (X.max() - X.min())")
    val NODE_DIM_REDUCTION = VisualRoadmapNode(id = "ml_dim_reduction", title = "Dimensionality Reduction", section = "Data Preparation", explanation = "Compressing high-dimensional feature spaces while preserving variance.", objectives = listOf("PCA", "t-SNE"), codeSnippet = "from sklearn.decomposition import PCA\npca = PCA(n_components=2)")
    val NODE_FEATURE_SELECTION = VisualRoadmapNode(id = "ml_feature_selection", title = "Feature Selection", section = "Data Preparation", explanation = "Selecting the most predictive subset of features to avoid overfitting.", objectives = listOf("VarianceThreshold", "Recursive Feature Elimination"), codeSnippet = "from sklearn.feature_selection import RFE")

    // Machine Learning Concepts
    val NODE_WHAT_IS_ML = VisualRoadmapNode(id = "ml_what_is_ml", title = "What is Machine Learning?", section = "Machine Learning Core", isMajor = true, explanation = "Algorithms that learn patterns from data without being explicitly programmed.", objectives = listOf("Learning paradigm", "Generalization"), codeSnippet = "# Training on past data to predict unseen events")
    val NODE_TYPES_OF_ML = VisualRoadmapNode(id = "ml_types_of_ml", title = "Types of Machine Learning", section = "Machine Learning Core", isMajor = true, badge = "Milestone 5", explanation = "Supervised, Unsupervised, Semi-supervised, Self-supervised, and Reinforcement Learning.", objectives = listOf("Supervised vs Unsupervised", "Reinforcement learning"), codeSnippet = "# Categories: Supervised, Unsupervised, Reinforcement")

    val NODE_SCIKIT_LEARN = VisualRoadmapNode(
        id = "ml_scikit_learn",
        title = "Scikit-learn",
        section = "Machine Learning Core",
        isMajor = true,
        explanation = "The gold standard Python machine learning library providing unified fit() and predict() interfaces.",
        objectives = listOf("Estimator API", "Pipelines", "GridSearchCV"),
        codeSnippet = "from sklearn.linear_model import LogisticRegression\nclf = LogisticRegression().fit(X_train, y_train)\npreds = clf.predict(X_test)"
    )

    // Supervised Learning
    val NODE_SUPERVISED = VisualRoadmapNode(id = "ml_supervised", title = "Supervised Learning", section = "Supervised Learning", isMajor = true, explanation = "Predicting target labels from labeled training examples.", objectives = listOf("Classification", "Regression"), codeSnippet = "# Predict continuous y (Regression) or discrete y (Classification)")
    val NODE_CLASSIFICATION = VisualRoadmapNode(id = "ml_classification", title = "Classification", section = "Supervised Learning", isMajor = true, explanation = "Assigning input samples into discrete categories.", objectives = listOf("Binary & Multi-class", "Decision boundary"), codeSnippet = "from sklearn.ensemble import RandomForestClassifier\nmodel = RandomForestClassifier()")
    val NODE_REGRESSION = VisualRoadmapNode(id = "ml_regression", title = "Regression", section = "Supervised Learning", isMajor = true, explanation = "Predicting continuous numerical target values.", objectives = listOf("Ordinary Least Squares", "Regularization"), codeSnippet = "from sklearn.linear_model import LinearRegression\nreg = LinearRegression().fit(X, y)")

    // Classification Algorithms
    val NODE_KNN = VisualRoadmapNode(id = "ml_knn", title = "K-Nearest Neighbors (KNN)", section = "Supervised Learning", explanation = "Instance-based learning using distance metrics.", objectives = listOf("Euclidean distance", "Choice of K"), codeSnippet = "from sklearn.neighbors import KNeighborsClassifier")
    val NODE_LOGISTIC_REG = VisualRoadmapNode(id = "ml_logistic_reg", title = "Logistic Regression", section = "Supervised Learning", explanation = "Sigmoid probability curve for binary classification.", objectives = listOf("Log-odds", "Cross-entropy loss"), codeSnippet = "y_prob = 1 / (1 + np.exp(-z))")
    val NODE_SVM = VisualRoadmapNode(id = "ml_svm", title = "Support Vector Machines", section = "Supervised Learning", explanation = "Maximizing margin between classes using kernel tricks.", objectives = listOf("Hyperplanes", "RBF kernel"), codeSnippet = "from sklearn.svm import SVC\nsvc = SVC(kernel='rbf')")
    val NODE_DECISION_TREES = VisualRoadmapNode(id = "ml_decision_trees", title = "Decision Trees, Random Forest", section = "Supervised Learning", explanation = "Tree-based recursive splits and bagging ensembles.", objectives = listOf("Gini impurity", "Ensemble bagging"), codeSnippet = "from sklearn.ensemble import RandomForestClassifier")
    val NODE_GRADIENT_BOOSTING = VisualRoadmapNode(id = "ml_gradient_boosting", title = "Gradient Boosting Machines", section = "Supervised Learning", explanation = "Sequential boosting of weak learners: XGBoost, LightGBM, CatBoost.", objectives = listOf("Residual learning", "Gradient boosting"), codeSnippet = "import xgboost as xgb\nmodel = xgb.XGBClassifier()")

    // Regression Algorithms
    val NODE_LIN_REG = VisualRoadmapNode(id = "ml_lin_reg", title = "Linear Regression", section = "Supervised Learning", explanation = "Modeling linear relationships minimizing sum of squared errors.", objectives = listOf("Cost function", "R-squared"), codeSnippet = "from sklearn.linear_model import LinearRegression")
    val NODE_POLY_REG = VisualRoadmapNode(id = "ml_poly_reg", title = "Polynomial Regression", section = "Supervised Learning", explanation = "Fitting non-linear curves using polynomial degree features.", objectives = listOf("Degree expansion", "Overfitting risk"), codeSnippet = "from sklearn.preprocessing import PolynomialFeatures")
    val NODE_RIDGE_LASSO = VisualRoadmapNode(id = "ml_ridge_lasso", title = "Lasso, Ridge", section = "Supervised Learning", explanation = "L1 and L2 penalty regularization on weights.", objectives = listOf("L1 sparsity", "L2 weight decay"), codeSnippet = "from sklearn.linear_model import Ridge, Lasso")
    val NODE_ELASTICNET = VisualRoadmapNode(id = "ml_elasticnet", title = "ElasticNet Regularization", section = "Supervised Learning", explanation = "Combined convex mixture of L1 and L2 penalties.", objectives = listOf("l1_ratio hyperparameter"), codeSnippet = "from sklearn.linear_model import ElasticNet")

    // Unsupervised Learning
    val NODE_UNSUPERVISED = VisualRoadmapNode(id = "ml_unsupervised", title = "Unsupervised Learning", section = "Unsupervised Learning", isMajor = true, explanation = "Uncovering hidden structures without ground truth labels.", objectives = listOf("Clustering", "Latent representation"), codeSnippet = "# Clustering & Dimensionality Reduction")
    val NODE_CLUSTERING = VisualRoadmapNode(id = "ml_clustering", title = "Clustering", section = "Unsupervised Learning", isMajor = true, explanation = "Grouping similar data points together.", objectives = listOf("K-Means", "DBSCAN", "Hierarchical clustering"), codeSnippet = "from sklearn.cluster import KMeans\nkmeans = KMeans(n_clusters=3).fit(X)")

    // Reinforcement Learning
    val NODE_REINFORCEMENT = VisualRoadmapNode(id = "ml_reinforcement", title = "Reinforcement Learning", section = "Reinforcement Learning", isMajor = true, badge = "Milestone 6", explanation = "Agents learning optimal policies via rewards and penalties in environments.", objectives = listOf("Markov Decision Processes", "Q-Learning", "Policy gradients"), codeSnippet = "# Agent learns action a in state s to maximize expected return R")

    // Model Evaluation
    val NODE_MODEL_EVALUATION = VisualRoadmapNode(id = "ml_model_evaluation", title = "Model Evaluation", section = "Evaluation", isMajor = true, badge = "Milestone 7", explanation = "Techniques to reliably measure generalization and prevent data leakage.", objectives = listOf("Train/Val/Test split", "K-Fold cross-validation", "Evaluation metrics"), codeSnippet = "from sklearn.model_selection import cross_val_score")
    val NODE_METRICS = VisualRoadmapNode(id = "ml_metrics", title = "Metrics to Evaluate", section = "Evaluation", isMajor = true, explanation = "Accuracy, Precision, Recall, F1-Score, ROC-AUC, Log Loss, Confusion Matrix.", objectives = listOf("Tradeoff between precision and recall", "ROC curves"), codeSnippet = "from sklearn.metrics import classification_report\nprint(classification_report(y_true, y_pred))")
    val NODE_VALIDATION_TECH = VisualRoadmapNode(id = "ml_validation_tech", title = "Validation Techniques", section = "Evaluation", isMajor = true, explanation = "K-Fold Cross Validation, Stratified K-Fold, and Leave-One-Out (LOOCV).", objectives = listOf("Preventing data leakage", "Cross-validation"), codeSnippet = "from sklearn.model_selection import KFold\nkf = KFold(n_splits=5)")

    // Deep Learning
    val NODE_DEEP_LEARNING = VisualRoadmapNode(id = "ml_deep_learning", title = "Deep Learning", section = "Deep Learning", isMajor = true, badge = "Milestone 8", explanation = "Hierarchical feature learning using multi-layered artificial neural networks.", objectives = listOf("Backpropagation", "Deep architectures", "GPU acceleration"), codeSnippet = "import torch\nimport torch.nn as nn")
    val NODE_NN_BASICS = VisualRoadmapNode(id = "ml_nn_basics", title = "Neural Network (NN) Basics", section = "Deep Learning", isMajor = true, explanation = "Perceptrons, forward propagation, loss functions, backprop, and activation functions.", objectives = listOf("Activation functions (ReLU, Sigmoid)", "Gradient descent"), codeSnippet = "# Forward: z = Wx + b, a = relu(z)\n# Backward: dL/dW via chain rule")
    val NODE_DL_LIBRARIES = VisualRoadmapNode(id = "ml_dl_libraries", title = "Deep Learning Libraries", section = "Deep Learning", isMajor = true, explanation = "PyTorch, TensorFlow, and Keras frameworks.", objectives = listOf("Autograd engine", "GPU CUDA execution"), codeSnippet = "import torch\ndevice = 'cuda' if torch.cuda.is_available() else 'cpu'")
    val NODE_DL_ARCHITECTURES = VisualRoadmapNode(id = "ml_dl_architectures", title = "Deep Learning Architectures", section = "Deep Learning", isMajor = true, explanation = "CNNs for spatial data, RNNs for temporal sequences, Transformers for attention.", objectives = listOf("CNNs, RNNs, Transformers"), codeSnippet = "# Modern Deep Learning Archs")

    val NODE_CNN = VisualRoadmapNode(id = "ml_cnn", title = "Convolutional Neural Network", section = "Deep Learning", isMajor = true, explanation = "Convolution kernels, pooling layers, and strides for computer vision.", objectives = listOf("Conv2D filters", "Max pooling", "Feature maps"), codeSnippet = "nn.Conv2d(in_channels=3, out_channels=16, kernel_size=3)")
    val NODE_CNN_APPS = VisualRoadmapNode(id = "ml_cnn_apps", title = "Applications of CNNs", section = "Deep Learning", isMajor = true, explanation = "Image recognition, object detection, segmentation, and recommendation.", objectives = listOf("Image classification", "ResNet architectures"), codeSnippet = "from torchvision.models import resnet18\nmodel = resnet18(pretrained=True)")
    val NODE_RNN = VisualRoadmapNode(id = "ml_rnn", title = "Recurrent Neural Networks", section = "Deep Learning", isMajor = true, explanation = "Sequential processing with hidden state feedback: Vanilla RNN, GRU, LSTM.", objectives = listOf("Vanishing gradient problem", "LSTM memory gates"), codeSnippet = "nn.LSTM(input_size=10, hidden_size=20, num_layers=2)")
    val NODE_ATTENTION = VisualRoadmapNode(id = "ml_attention", title = "Attention Mechanisms", section = "Deep Learning", isMajor = true, explanation = "Self-attention and multi-head attention allowing parallel sequence modeling.", objectives = listOf("Query, Key, Value vectors", "Scaled dot-product attention"), codeSnippet = "Attention(Q, K, V) = softmax(Q @ K.T / sqrt(d_k)) @ V")
    val NODE_AUTOENCODERS = VisualRoadmapNode(id = "ml_autoencoders", title = "Autoencoders", section = "Deep Learning", isMajor = true, explanation = "Encoder-decoder networks for unsupervised representation compression and anomaly detection.", objectives = listOf("Bottleneck latent space", "Reconstruction loss"), codeSnippet = "# Encodes x -> z -> reconstructs x_hat")
    val NODE_GANS = VisualRoadmapNode(id = "ml_gans", title = "Generative Adversarial Networks", section = "Deep Learning", isMajor = true, explanation = "Generator and Discriminator minimax game for synthetic data generation.", objectives = listOf("Adversarial training", "Mode collapse"), codeSnippet = "# Minimax game: min_G max_D V(D, G)")

    // Advanced Concepts
    val NODE_ADVANCED_ML = VisualRoadmapNode(id = "ml_advanced", title = "Advanced Concepts in ML", section = "Advanced ML", isMajor = true, badge = "Milestone 9", explanation = "Explainable AI, LLMs, and Natural Language Processing.", objectives = listOf("XAI interpretability", "Transformer NLP"), codeSnippet = "# Explainable AI & Modern NLP")
    val NODE_EXPLAINABLE_AI = VisualRoadmapNode(id = "ml_explainable_ai", title = "Explainable AI", section = "Advanced ML", isMajor = true, explanation = "SHAP and LIME values to explain feature contributions in black-box models.", objectives = listOf("SHAP values", "Model interpretability"), codeSnippet = "import shap\nexplainer = shap.TreeExplainer(model)")
    val NODE_NLP = VisualRoadmapNode(id = "ml_nlp", title = "Natural Language Processing", section = "Advanced ML", isMajor = true, explanation = "Tokenization, lemmatization, word embeddings, and attention-based language models.", objectives = listOf("Word2Vec, FastText", "BERT & LLM Tokenization"), codeSnippet = "from transformers import AutoTokenizer\ntokenizer = AutoTokenizer.from_pretrained('bert-base-uncased')")

    // Subtopics for NLP
    val NODE_TOKENIZATION = VisualRoadmapNode(id = "ml_tokenization", title = "Tokenization", section = "Advanced ML", explanation = "Splitting text into words, characters, or subword tokens (Byte-Pair Encoding).", objectives = listOf("BPE", "WordPiece"), codeSnippet = "tokens = tokenizer.encode('EduNova AI')")
    val NODE_LEMMATIZATION = VisualRoadmapNode(id = "ml_lemmatization", title = "Lemmatization", section = "Advanced ML", explanation = "Reducing inflected words to canonical dictionary forms.", objectives = listOf("Lemmatization vs Stemming"), codeSnippet = "lemma = wordnet_lemmatizer.lemmatize('running')")
    val NODE_EMBEDDINGS = VisualRoadmapNode(id = "ml_embeddings", title = "Embeddings", section = "Advanced ML", explanation = "Dense vector representations mapping semantic meanings into geometric vector spaces.", objectives = listOf("Cosine similarity", "Vector databases"), codeSnippet = "cos_sim = np.dot(u, v) / (norm(u) * norm(v))")
    val NODE_ATTENTION_MODELS = VisualRoadmapNode(id = "ml_attention_models", title = "Attention Models", section = "Advanced ML", explanation = "Transformers replacing recurrence with full self-attention.", objectives = listOf("Transformer encoders", "GPT decoders"), codeSnippet = "from transformers import AutoModelForSequenceClassification")
    val NODE_STEMMING = VisualRoadmapNode(id = "ml_stemming", title = "Stemming", section = "Advanced ML", explanation = "Heuristic chopping of word endings (Porter Stemmer).", objectives = listOf("Porter stemmer", "Rule-based stripping"), codeSnippet = "stem = porter.stem('computation')")

    // All Visual Nodes List for Machine Learning
    val ALL_VISUAL_NODES: List<VisualRoadmapNode> = listOf(
        NODE_ROOT, NODE_INTRO, NODE_WHAT_IS_ML_ENG, NODE_ML_VS_AI, NODE_SKILLS_RESP,
        NODE_MATH_FOUNDATIONS, NODE_LINEAR_ALGEBRA, NODE_MATRIX_OPS, NODE_SCALARS_VECTORS, NODE_SVD, NODE_DETERMINANTS, NODE_EIGENVALUES,
        NODE_CALCULUS, NODE_DERIVATIVES, NODE_CHAIN_RULE, NODE_GRADIENT_HESSIAN,
        NODE_DISCRETE_MATH, NODE_STATISTICS, NODE_BASIC_STATS, NODE_DESCRIPTIVE_STATS, NODE_GRAPHS_CHARTS, NODE_INFERENTIAL_STATS,
        NODE_PROBABILITY, NODE_BASICS_PROB, NODE_BAYES_THEOREM, NODE_RANDOM_VARS, NODE_DISTRIBUTIONS,
        NODE_PROGRAMMING_FUND, NODE_PYTHON, NODE_PY_SYNTAX, NODE_PY_VARS, NODE_PY_DATA_STRUCTS, NODE_PY_LOOPS, NODE_PY_CONDITIONALS, NODE_PY_EXCEPTIONS, NODE_PY_FUNCTIONS,
        NODE_OOP, NODE_ESSENTIAL_LIBS, NODE_NUMPY, NODE_PANDAS, NODE_MATPLOTLIB, NODE_SEABORN,
        NODE_DATA_COLLECTION, NODE_DATA_SOURCES, NODE_DATA_FORMATS, NODE_PREPROCESSING, NODE_DATA_CLEANING, NODE_FEATURE_ENG, NODE_SCALING_NORM, NODE_DIM_REDUCTION, NODE_FEATURE_SELECTION,
        NODE_WHAT_IS_ML, NODE_TYPES_OF_ML, NODE_SCIKIT_LEARN,
        NODE_SUPERVISED, NODE_CLASSIFICATION, NODE_REGRESSION,
        NODE_KNN, NODE_LOGISTIC_REG, NODE_SVM, NODE_DECISION_TREES, NODE_GRADIENT_BOOSTING,
        NODE_LIN_REG, NODE_POLY_REG, NODE_RIDGE_LASSO, NODE_ELASTICNET,
        NODE_UNSUPERVISED, NODE_CLUSTERING, NODE_REINFORCEMENT,
        NODE_MODEL_EVALUATION, NODE_METRICS, NODE_VALIDATION_TECH,
        NODE_DEEP_LEARNING, NODE_NN_BASICS, NODE_DL_LIBRARIES, NODE_DL_ARCHITECTURES,
        NODE_CNN, NODE_CNN_APPS, NODE_RNN, NODE_ATTENTION, NODE_AUTOENCODERS, NODE_GANS,
        NODE_ADVANCED_ML, NODE_EXPLAINABLE_AI, NODE_NLP,
        NODE_TOKENIZATION, NODE_LEMMATIZATION, NODE_EMBEDDINGS, NODE_ATTENTION_MODELS, NODE_STEMMING
    )
}
