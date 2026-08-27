import os
import sys
import time
import subprocess
import signal

# Ensure we can import from PythonTesting
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from test_definitions import generate_test_cases
from mobile_suite import run_mobile_tests
from web_suite import run_web_tests
from api_suite import run_api_tests
from security_suite import run_security_tests
from performance_suite import run_performance_tests
from report_generator import ReportGenerator

def main():
    print("==================================================")
    print("STARTING NYAYA LEGALAI E2E MASTER RUNNER")
    print("==================================================")
    
    # 1. Start FastAPI Mock Service in the background
    backend_script = os.path.join(os.path.dirname(os.path.abspath(__file__)), "backend_service.py")
    backend_proc = None
    try:
        print("[Backend] Launching FastAPI service in background...")
        backend_proc = subprocess.Popen(
            [sys.executable, backend_script]
        )
        # Give uvicorn some time to bind port 8000
        time.sleep(5)
        print("[Backend] FastAPI service is active.")
    except Exception as e:
        print(f"[Backend] Failed to start mock API server: {e}. Continuing in simulation mode.")

    all_results = []
    
    try:
        # 2. Load Definitions
        definitions = generate_test_cases()
        
        # 3. Run each suite sequentially
        mob_res = run_mobile_tests(definitions["Mobile Frontend"])
        all_results.extend(mob_res)
        
        web_res = run_web_tests(definitions["Web Frontend"])
        all_results.extend(web_res)
        
        api_res = run_api_tests(definitions["Backend API"])
        all_results.extend(api_res)
        
        sec_res = run_security_tests(definitions["Security Audit"])
        all_results.extend(sec_res)
        
        perf_res = run_performance_tests(definitions["Performance Load"])
        all_results.extend(perf_res)
        
        # 4. Generate Reports (Excel, HTML, Markdown summaries)
        generator = ReportGenerator(all_results, definitions)
        generator.generate_all()
        
    finally:
        # 5. Clean terminate FastAPI Mock Service
        if backend_proc:
            print("[Backend] Terminating mock API service...")
            try:
                backend_proc.terminate()
                backend_proc.wait(timeout=5)
                print("[Backend] Mock API service terminated successfully.")
            except Exception as e:
                print(f"[Backend] Force killing API service process due to termination timeout: {e}")
                backend_proc.kill()
                
    print("\n==================================================")
    print("NYAYA LEGALAI E2E MASTER RUNNER COMPLETED")
    print("==================================================")

if __name__ == "__main__":
    main()
