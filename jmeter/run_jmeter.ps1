# Script to execute JMeter load tests (Green, Yellow, Red scenarios)
# and generate the interactive HTML dashboard report for vg-ms-medical-history

$JMETER_JAR = "C:\tools\apache-jmeter-5.6.3\bin\ApacheJMeter.jar"
$JMX_FILE   = "jmeter\prs_medical_history_load_test.jmx"
$RESULTS_JTL = "jmeter\results.jtl"
$REPORT_DIR = "jmeter\report"

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " STARTING JMETER LOAD TESTS - PRS SIGRC (MEDICAL HISTORY)" -ForegroundColor Cyan
Write-Host " Scenarios: Green (Normal) | Yellow (Peak) | Red (Stress)" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

if (Test-Path $REPORT_DIR) {
    Remove-Item -Recurse -Force $REPORT_DIR
}
if (Test-Path $RESULTS_JTL) {
    Remove-Item -Force $RESULTS_JTL
}

& java -jar $JMETER_JAR -n -t $JMX_FILE -l $RESULTS_JTL -e -o $REPORT_DIR -f

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n[SUCCESS] Load tests completed and report generated at: $REPORT_DIR" -ForegroundColor Green
    Write-Host "Open jmeter\report\index.html in your browser to view the Dashboard." -ForegroundColor Yellow
} else {
    Write-Host "`n[ERROR] An issue occurred during JMeter execution." -ForegroundColor Red
}
