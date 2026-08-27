import os
import sys
import time
import subprocess
import json
import argparse

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
    parser = argparse.ArgumentParser(description="Nyaya LegalAI E2E Test Runner")
    parser.add_argument("--suite", choices=["mobile", "web", "api", "security", "performance", "bundle"], help="Run specific suite or bundle reports")
    args = parser.parse_args()

    reports_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "reports")
    os.makedirs(reports_dir, exist_ok=True)

    definitions = generate_test_cases()

    if not args.suite:
        # Sequential backward-compatible run
        print("==================================================")
        print("STARTING NYAYA LEGALAI E2E MASTER RUNNER (SEQUENTIAL)")
        print("==================================================")
        
        backend_proc = start_backend()
        all_results = []
        try:
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
            
            generator = ReportGenerator(all_results, definitions)
            generator.generate_all()
        finally:
            stop_backend(backend_proc)
            
    elif args.suite == "bundle":
        print("==================================================")
        print("BUNDLING PARALLEL TEST RESULTS AND GENERATING REPORTS")
        print("==================================================")
        combined_results = []
        for name in ["mobile", "web", "api", "security", "performance"]:
            file_path = os.path.join(reports_dir, f"results_{name}.json")
            if os.path.exists(file_path):
                print(f"[Bundle] Loading results from: {file_path}")
                with open(file_path, "r") as f:
                    combined_results.extend(json.load(f))
            else:
                print(f"[Bundle] Warning: {file_path} not found. Using empty results for {name}.")
        
        generator = ReportGenerator(combined_results, definitions)
        generator.generate_all()
        
    else:
        print("==================================================")
        print(f"RUNNING TEST SUITE: {args.suite.upper()}")
        print("==================================================")
        
        suite_results = []
        backend_proc = None
        
        try:
            if args.suite == "mobile":
                suite_results = run_mobile_tests(definitions["Mobile Frontend"])
            elif args.suite == "web":
                suite_results = run_web_tests(definitions["Web Frontend"])
            elif args.suite == "api":
                backend_proc = start_backend()
                suite_results = run_api_tests(definitions["Backend API"])
            elif args.suite == "security":
                suite_results = run_security_tests(definitions["Security Audit"])
            elif args.suite == "performance":
                suite_results = run_performance_tests(definitions["Performance Load"])
        finally:
            if backend_proc:
                stop_backend(backend_proc)
                
        out_path = os.path.join(reports_dir, f"results_{args.suite}.json")
        with open(out_path, "w") as f:
            json.dump(suite_results, f, indent=2)
        print(f"[Runner] Suite {args.suite} execution completed. Saved results to: {out_path}")

def start_backend():
    backend_script = os.path.join(os.path.dirname(os.path.abspath(__file__)), "backend_service.py")
    try:
        print("[Backend] Launching FastAPI service in background...")
        proc = subprocess.Popen([sys.executable, backend_script])
        time.sleep(5)
        print("[Backend] FastAPI service is active.")
        return proc
    except Exception as e:
        print(f"[Backend] Failed to start mock API server: {e}.")
        return None

def stop_backend(proc):
    if proc:
        print("[Backend] Terminating mock API service...")
        try:
            proc.terminate()
            proc.wait(timeout=5)
            print("[Backend] Mock API service terminated successfully.")
        except Exception as e:
            print(f"[Backend] Force killing API service process due to termination timeout: {e}")
            proc.kill()

if __name__ == "__main__":
    main()
