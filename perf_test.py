import requests
import time
import concurrent.futures
import statistics

import os

URL = "http://localhost:8080/api/auth/login"
# Payload invalide mais suffisant pour tester la réaction du serveur (401 ou 400)
email = os.getenv("PERF_TEST_EMAIL", "perf.test@test.com")
password = os.getenv("PERF_TEST_PASSWORD", "wrongpassword")
PAYLOAD = {"email": email, "password": password}
NUM_REQUESTS = 50
CONCURRENT_WORKERS = 10

def make_request(i):
    start = time.time()
    try:
        resp = requests.post(URL, json=PAYLOAD)
        status = resp.status_code
    except Exception as e:
        status = "ERR"
    end = time.time()
    return end - start, status

print(f"Lancement du test de charge sur {URL}")
print(f"{NUM_REQUESTS} requêtes ({CONCURRENT_WORKERS} concurrentes)...")

latencies = []
statuses = {}

start_global = time.time()
with concurrent.futures.ThreadPoolExecutor(max_workers=CONCURRENT_WORKERS) as executor:
    futures = [executor.submit(make_request, i) for i in range(NUM_REQUESTS)]
    for future in concurrent.futures.as_completed(futures):
        lat, status = future.result()
        latencies.append(lat * 1000) # ms
        statuses[status] = statuses.get(status, 0) + 1
end_global = time.time()

total_time = end_global - start_global
rps = NUM_REQUESTS / total_time

print("\n--- RÉSULTATS ---")
print(f"Temps total : {total_time:.2f}s")
print(f"RPS (Req/sec): {rps:.2f}")
print(f"Moyenne : {statistics.mean(latencies):.2f} ms")
print(f"Médiane : {statistics.median(latencies):.2f} ms")
print(f"Max     : {max(latencies):.2f} ms")
print(f"Status  : {statuses}")
