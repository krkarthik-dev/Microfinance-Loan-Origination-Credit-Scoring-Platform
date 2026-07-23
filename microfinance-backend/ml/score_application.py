import sys
import time
import json
import random

def score_application(app_id, amount):
    # Simulate ML model processing time
    time.sleep(2)

    # Generate a dummy score between 300 and 900
    score = random.randint(300, 900)

    # Calculate a mock Probability of Default (PoD)
    # Higher score -> Lower PoD
    base_pod = 0.20
    score_factor = (score - 300) / 600
    pod = max(0.01, base_pod - (score_factor * 0.18))
    
    # Determine risk tier based on score
    if score >= 750:
        risk_tier = "LOW"
    elif score >= 600:
        risk_tier = "MEDIUM"
    else:
        risk_tier = "HIGH"

    result = {
        "applicationId": app_id,
        "creditScore": score,
        "probabilityOfDefault": round(pod, 4),
        "riskTier": risk_tier,
        "modelVersion": "v1.2-alpha"
    }

    # Print the result as a JSON string to stdout
    print(json.dumps(result))

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print(json.dumps({"error": "Missing arguments. Usage: score_application.py <app_id> <amount>"}))
        sys.exit(1)
    
    app_id = sys.argv[1]
    amount = sys.argv[2]
    
    try:
        score_application(app_id, amount)
    except Exception as e:
        print(json.dumps({"error": str(e)}))
        sys.exit(1)
