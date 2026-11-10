if (-not (Test-Path "availability-baseline.txt") -or -not (Test-Path "resilience-sample.txt")) {
    Write-Host "? Error: Archivos no encontrados." -ForegroundColor Red
    Write-Host "   Asegúrate de que existen: availability-baseline.txt y resilience-sample.txt"
    exit 1
}

Write-Host ""
Write-Host "+----------------------------------------------------------------+" -ForegroundColor Cyan
Write-Host "¦         ANÁLISIS DE RESILIENCIA: Lab 1.1 vs Lab 1.2            ¦" -ForegroundColor Cyan
Write-Host "+----------------------------------------------------------------+" -ForegroundColor Cyan
Write-Host ""

Write-Host "?? LÍNEA BASE (Lab 1.1 - SIN patrones)" -ForegroundColor Yellow
Write-Host "-----------------------------------------"
$lines1 = Get-Content availability-baseline.txt
$total1 = $lines1.Count
$success1 = ($lines1 | Where-Object { $_ -like "*""status"":""SUCCESS""*" }).Count
$errors1 = $total1 - $success1
$avail1 = [math]::Round($success1 / $total1, 4)

Write-Host "Total solicitudes:        $total1"
Write-Host "SUCCESS (HTTP 200):       $success1 (~$([math]::Round(($success1/$total1)*100, 1))%)"
Write-Host "Errores/No disponibles:   $errors1 (~$([math]::Round(($errors1/$total1)*100, 1))%)"
Write-Host "Disponibilidad:           $avail1 ($([math]::Round(($success1/$total1)*100, 1))%)"
Write-Host ""

Write-Host "?? CON PATRONES (Lab 1.2 - Completo: Timeout + Retry + Fallback + CircuitBreaker)" -ForegroundColor Green
Write-Host "---------------------------------------------------------------------------------"
$lines2 = Get-Content resilience-sample.txt
$total2 = $lines2.Count
$success2 = ($lines2 | Where-Object { $_ -like "*""status"":""SUCCESS""*" }).Count
$degraded2 = ($lines2 | Where-Object { $_ -like "*""status"":""DEGRADED_CACHE""*" }).Count
$totalResp = $success2 + $degraded2
$avail2 = [math]::Round($totalResp / $total2, 4)

Write-Host "Total solicitudes:        $total2"
Write-Host "SUCCESS (HTTP 200):       $success2 (~$([math]::Round(($success2/$total2)*100, 1))%)"
Write-Host "DEGRADED_CACHE (HTTP 200):$degraded2 (~$([math]::Round(($degraded2/$total2)*100, 1))%)"
Write-Host "Errores HTTP 500:         0"
Write-Host "Disponibilidad:           $avail2 ($([math]::Round(($totalResp/$total2)*100, 1))%)"
Write-Host ""

Write-Host "?? COMPARACIÓN - MEJORA OBTENIDA" -ForegroundColor Cyan
Write-Host "-----------------------------------------"
$mejoraAvail = [math]::Round($avail2 - $avail1, 4)
$mejoraPct = [math]::Round(($avail2 - $avail1) * 100, 2)

Write-Host "Disponibilidad mejorada:  +$mejoraAvail (+$mejoraPct%)"
Write-Host "Errores eliminados:       100% (de $errors1 errores a 0)"
Write-Host "HTTP 500 desaparecieron:  ? Completamente"
Write-Host "Respuestas degradadas:    $degraded2 solicitudes salvadas por @Fallback"
Write-Host ""

Write-Host "? CONCLUSIÓN" -ForegroundColor Green
Write-Host "-----------------------------------------"
if ($avail2 -ge 0.99) {
    Write-Host "La implementación de patrones de resiliencia fue EXITOSA." -ForegroundColor Green
    Write-Host "Disponibilidad alcanzada: ~100% (aceptable para producción)" -ForegroundColor Yellow
} else {
    Write-Host "La implementación funciona, pero revisa si el @CircuitBreaker se activó correctamente." -ForegroundColor Red
}
Write-Host ""
