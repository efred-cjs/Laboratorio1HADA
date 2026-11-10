package com.ecom.catalog;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Random;
import java.util.logging.Logger;

@Path("/v1/products")
public class CatalogResource {

    private static final Logger LOG = Logger.getLogger(CatalogResource.class.getName());
    private final Random random = new Random();

    /**
     * ThreadLocal para rastrear el número de intento de cada solicitud HTTP.
     */
    private static final ThreadLocal<Integer> ATTEMPT_COUNT = ThreadLocal.withInitial(() -> 0);

    /**
     * ThreadLocal para determinar si la solicitud HTTP actual debe fallar en los primeros 2 intentos.
     */
    private static final ThreadLocal<Boolean> SHOULD_FAIL_FIRST_ATTEMPTS = ThreadLocal.withInitial(() -> false);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Timeout(800)
    @Retry(maxRetries = 2, delay = 150)
    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.7, delay = 5000)
    @Fallback(fallbackMethod = "getCatalogFallback")
    public Response getProducts() throws InterruptedException {
        try {
            // Incrementar contador de intentos para esta solicitud HTTP
            ATTEMPT_COUNT.set(ATTEMPT_COUNT.get() + 1);
            int currentAttempt = ATTEMPT_COUNT.get();

            // Separador visual: marca el inicio de una nueva solicitud HTTP
            if (currentAttempt == 1) {
                LOG.info("════════════════════════════════════════════════════════════");
            }

            LOG.info("[Intento " + currentAttempt + "/3] Consultando catálogo de productos...");

            // En el primer intento, decidir si esta solicitud HTTP fallará en todos los intentos
            if (currentAttempt == 1) {
                // 60% de probabilidad de que los 3 intentos fallen
                SHOULD_FAIL_FIRST_ATTEMPTS.set(random.nextInt(10) < 6);
            }

            boolean shouldFail = SHOULD_FAIL_FIRST_ATTEMPTS.get();

            // Los 3 intentos pueden fallar (si fue marcado)
            if (shouldFail && currentAttempt <= 3) {
                if (currentAttempt == 1) {
                    LOG.severe("[Intento " + currentAttempt + "/3] Falla de conexión a la base de datos primaria.");
                    throw new RuntimeException("Database connection timeout");
                } else if (currentAttempt == 2) {
                    LOG.warning("[Intento " + currentAttempt + "/3] Simulando latencia alta en consulta de catálogo...");
                    Thread.sleep(3000);  // Capturado por @Timeout(800)
                } else if (currentAttempt == 3) {
                    LOG.severe("[Intento " + currentAttempt + "/3] Falla de conexión después de reintentos.");
                    throw new RuntimeException("Database connection timeout after retries");
                }
            }

            // Respuesta exitosa
            LOG.info("[Intento " + currentAttempt + "/3] Catálogo retornado exitosamente desde la base de datos primaria.");
            ATTEMPT_COUNT.set(0);  // Resetear inmediatamente
            SHOULD_FAIL_FIRST_ATTEMPTS.set(false);
            return Response.ok("{\"status\":\"SUCCESS\",\"data\":[\"Product A\",\"Product B\",\"Product C\"]}").build();
            
        } finally {
            // Se resetea en fallback si falla
        }
    }

    /**
     * Método de fallback invocado cuando se agotan los reintentos, hay timeout o se abre el CircuitBreaker.
     */
    public Response getCatalogFallback() {
        int failedAttempts = ATTEMPT_COUNT.get();
        LOG.info("Fallback activado: Se agotaron todos los " + failedAttempts + " intentos. Sirviendo datos desde caché.");
        ATTEMPT_COUNT.set(0);  // Resetear contador
        SHOULD_FAIL_FIRST_ATTEMPTS.set(false);  // Resetear flag
        return Response.ok("{\"status\":\"DEGRADED_CACHE\",\"data\":[\"Cached Product A\",\"Cached Product B\"]}").build();
    }
}