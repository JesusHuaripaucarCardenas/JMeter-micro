# Pruebas de Carga con Apache JMeter - `vg-ms-medical-history`

El plan de pruebas de carga (`doc/jmeter/test_prs_medical_history_load.jmx`, también disponible en `jmeter/prs_medical_history_load_test.jmx` y `load-tests/medical-histories.jmx`) ejecuta de manera secuencial (`serialize_threadgroups = true`) los **3 escenarios de carga** (**Verde, Amarillo y Rojo**) en una sola corrida para generar un Dashboard HTML unificado.

## Escenarios Incluidos en el Plan (`.jmx`)

| Escenario | Thread Group | Usuarios | Ramp-up | Loops | Muestras Totales | Endpoints Evaluados |
|---|---|---:|---:|---:|---:|---|
| **Verde (Normal)** | `Green Scenario - Normal Load (20 Users)` | 20 | 5 s | 5 | 200 | `[Green] GET /actuator/health`<br>`[Green] GET /api/v1/medical-histories?status=ACTIVE` |
| **Amarillo (Pico)** | `Yellow Scenario - High / Peak Load (100 Users)` | 100 | 10 s | 5 | 1,000 | `[Yellow] GET /api/v1/medical-histories/patient/PAT-001`<br>`[Yellow] GET /api/v1/medical-histories/doctor/DOC-001` |
| **Rojo (Estrés)** | `Red Scenario - Extreme / Stress Load (400 Users)` | 400 | 5 s | 5 | 4,000 | `[Red] GET /api/v1/medical-histories`<br>`[Red] GET /api/v1/patients/PAT-001/medical-record/encounters` |

---

## Ejecución en GitHub Codespaces (Linux)

1. **Terminal 1 (Levantar el microservicio en puerto `9091`):**
   ```bash
   mvn spring-boot:run
   ```
2. **Terminal 2 (Descargar JMeter 5.6.3 oficial y ejecutar el plan completo):**
   ```bash
   curl -L https://archive.apache.org/dist/jmeter/binaries/apache-jmeter-5.6.3.tgz -o jmeter.tgz && tar -xzf jmeter.tgz
   rm -rf test-results.jtl load-test-report load-test-report.zip
   ./apache-jmeter-5.6.3/bin/jmeter -n -t doc/jmeter/test_prs_medical_history_load.jmx -l test-results.jtl -e -o load-test-report/
   zip -r load-test-report.zip load-test-report/
   ```
