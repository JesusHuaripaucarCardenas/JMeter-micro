# Pruebas de Carga con Apache JMeter - `vg-ms-medical-history`

El plan de pruebas de carga (`load-tests/medical-histories.jmx`, también disponible en `jmeter/prs_medical_history_load_test.jmx` y `doc/jmeter/test_prs_medical_history_load.jmx`) ejecuta de manera secuencial (`serialize_threadgroups = true`) los **3 escenarios de carga** (**Verde, Amarillo y Rojo**) en una sola corrida para generar un Dashboard HTML unificado.

## Escenarios Incluidos en el Plan (`.jmx`)

| Escenario | Thread Group | Usuarios | Ramp-up | Loops | Muestras Totales | Endpoints Evaluados |
|---|---|---:|---:|---:|---:|---|
| **Verde (Normal)** | `Green Scenario - Normal Load (20 Users)` | 20 | 5 s | 5 | 200 | `[Green] GET /actuator/health`<br>`[Green] GET /api/v1/medical-histories?status=ACTIVE` |
| **Amarillo (Pico)** | `Yellow Scenario - High / Peak Load (100 Users)` | 100 | 10 s | 5 | 1,000 | `[Yellow] GET /api/v1/medical-histories/patient/PAT-001`<br>`[Yellow] GET /api/v1/medical-histories/doctor/DOC-001` |
| **Rojo (Estrés)** | `Red Scenario - Extreme / Stress Load (400 Users)` | 400 | 5 s | 5 | 4,000 | `[Red] GET /api/v1/medical-histories`<br>`[Red] GET /api/v1/patients/PAT-001/medical-record/encounters` |

---

## 1. Ejecución Local en Windows (PowerShell)

1. Levantar el microservicio en una terminal (corre en el puerto `9091`):
   ```powershell
   mvn spring-boot:run
   ```
2. En otra terminal PowerShell, ejecutar el script automatizado:
   ```powershell
   .\jmeter\run_jmeter.ps1
   ```
   O directamente con JMeter 5.6.3:
   ```powershell
   C:\tools\apache-jmeter-5.6.3\bin\jmeter.bat -n -t load-tests/medical-histories.jmx -l test-results.jtl -e -o load-test-report/ -f
   ```
3. Abrir `jmeter\report\index.html` (o `load-test-report\index.html`) en el navegador.

---

## 2. Ejecución en GitHub Codespaces (Linux)

1. **Terminal 1 (Levantar el microservicio):**
   ```bash
   mvn spring-boot:run
   ```
2. **Terminal 2 (Descargar JMeter 5.6.3 oficial y ejecutar el plan completo):**
   ```bash
   curl -L https://archive.apache.org/dist/jmeter/binaries/apache-jmeter-5.6.3.tgz -o jmeter.tgz && tar -xzf jmeter.tgz
   rm -rf test-results.jtl load-test-report load-test-report.zip
   ./apache-jmeter-5.6.3/bin/jmeter -n -t load-tests/medical-histories.jmx -l test-results.jtl -e -o load-test-report/
   zip -r load-test-report.zip load-test-report/
   ```
3. Descargar `load-test-report.zip`, descomprimir la carpeta completa y abrir `index.html`.
