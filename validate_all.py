"""
Final Integration Validator - Plans 23 & 24
Efficient validation of all components
"""
import json, os, time, subprocess

def validate_all():
    results = {"plan23": {}, "plan24": {}, "overall": "PASS"}
    
    # Plan 23 Validation
    print("🧪 Plan 23 Validation...")
    files_23 = [
        "reverse_engineer/observer.py",
        "reverse_engineer/auth_analyzer.py", 
        "reverse_engineer/endpoint_discovery.py",
        "reverse_engineer/code_generator.py",
        "reverse_engineer/validator.py",
        "reverse_engineer/main.py"
    ]
    missing_23 = [f for f in files_23 if not os.path.exists(f)]
    results["plan23"] = {
        "files": len(files_23) - len(missing_23),
        "total": len(files_23),
        "status": "PASS" if not missing_23 else "FAIL"
    }
    print(f"   ✓ Files: {results['plan23']['files']}/{results['plan23']['total']}")
    
    # Plan 24 Validation  
    print("\n🧪 Plan 24 Validation...")
    files_24 = [
        "src/main/java/com/supremeai/mcp/MCPServerController.java",
        "src/main/java/com/supremeai/skill/SkillEngine.java",
        "src/main/java/com/supremeai/learning/SelfLearningRouter.java",
        "src/main/java/com/supremeai/swarm/SwarmCoordinator.java"
    ]
    missing_24 = [f for f in files_24 if not os.path.exists(f)]
    results["plan24"] = {
        "files": len(files_24) - len(missing_24),
        "total": len(files_24),
        "status": "PASS" if not missing_24 else "FAIL"
    }
    print(f"   ✓ Files: {results['plan24']['files']}/{results['plan24']['total']}")
    
    # Java Compilation
    print("\n🧪 Java Compilation...")
    result = subprocess.run(
        ["./gradlew", "compileJava"],
        capture_output=True, text=True, timeout=60
    )
    compile_ok = "BUILD SUCCESSFUL" in result.stdout
    results["java_compile"] = "PASS" if compile_ok else "FAIL"
    print(f"   {'✓' if compile_ok else '✗'} Java compilation")
    
    # Test Pipeline
    print("\n🧪 Pipeline Test...")
    import sys
    sys.path.insert(0, "/home/nazifarabbu/OneDrive/supremeai/reverse_engineer")
    from main import ReverseEngineer
    engine = ReverseEngineer("https://example.com")
    pipeline_result = engine.run_full_pipeline()
    pipeline_ok = pipeline_result.get('validation', {}).get('passed', False)
    results["pipeline"] = "PASS" if pipeline_ok else "FAIL"
    print(f"   {'✓' if pipeline_ok else '✗'} Pipeline test")
    
    # Overall
    all_pass = all([
        results["plan23"]["status"] == "PASS",
        results["plan24"]["status"] == "PASS", 
        results["java_compile"] == "PASS",
        results["pipeline"] == "PASS"
    ])
    results["overall"] = "PASS" if all_pass else "FAIL"
    
    # Save
    with open("validation_report.json", "w") as f:
        json.dump(results, f, indent=2)
    
    print(f"\n{'✅' if all_pass else '❌'} OVERALL: {results['overall']}")
    return all_pass

if __name__ == "__main__":
    validate_all()
