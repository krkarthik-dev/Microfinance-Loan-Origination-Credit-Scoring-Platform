import sys
import json
import numpy as np
from sklearn.ensemble import RandomForestClassifier

def generate_mock_model():
    """
    Creates a dummy Random Forest model fitted on synthetic data.
    In a real-world scenario, you would load a pre-trained .pkl file.
    """
    np.random.seed(42)
    # Synthetic dataset: [Income, LoanAmount, Age, ExistingDebt]
    X_train = np.array([
        [50000, 10000, 30, 2000],
        [80000, 5000, 45, 1000],
        [30000, 20000, 25, 5000],
        [120000, 50000, 50, 10000],
        [20000, 5000, 22, 3000]
    ])
    # 0 = Default, 1 = Paid
    y_train = np.array([1, 1, 0, 1, 0])
    
    model = RandomForestClassifier(n_estimators=10, random_state=42)
    model.fit(X_train, y_train)
    return model

def main():
    try:
        # Read JSON input from standard input (passed by Java ProcessBuilder)
        input_data = sys.stdin.read()
        if not input_data:
            raise ValueError("No input data provided")

        data = json.loads(input_data)
        
        # Extract features (using dummy values if missing for this simulation)
        # We assume data has at least appliedAmount, maybe others.
        loan_amount = float(data.get('appliedAmount', 10000))
        # Dummy values for features not provided by the current Java backend yet
        income = float(data.get('income', 60000))
        age = int(data.get('age', 35))
        existing_debt = float(data.get('existingDebt', 2000))

        X_input = np.array([[income, loan_amount, age, existing_debt]])

        # Generate model and predict
        model = generate_mock_model()
        
        # predict_proba returns [[prob_default, prob_paid]]
        probabilities = model.predict_proba(X_input)[0]
        prob_default = probabilities[0]
        
        # Calculate a normalized credit score (300 to 900)
        # Lower probability of default = higher score
        base_score = 300
        max_score = 900
        score_range = max_score - base_score
        
        credit_score = int(max_score - (prob_default * score_range))

        # Prepare output JSON
        output = {
            "creditScore": credit_score,
            "probabilityOfDefault": round(prob_default, 4)
        }
        
        # Print to stdout (Java captures this)
        print(json.dumps(output))

    except Exception as e:
        # Print error to stderr
        print(json.dumps({"error": str(e)}), file=sys.stderr)
        sys.exit(1)

if __name__ == "__main__":
    main()
