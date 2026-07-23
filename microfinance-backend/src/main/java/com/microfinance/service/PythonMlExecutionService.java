package com.microfinance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microfinance.dto.MlScoringRequest;
import com.microfinance.dto.MlScoringResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service responsible for executing the Python ML Script as a local process.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PythonMlExecutionService {

    private final ObjectMapper objectMapper;

    @Value("${app.ml.python-command:py}")
    private String pythonCommand;

    @Value("${app.ml.script-path:../microfinance-ml/credit_scoring.py}")
    private String scriptPath;

    /**
     * Executes the local Python ML script and returns the parsed response.
     * 
     * @param request the applicant data
     * @return the ML scoring response
     */
    public MlScoringResponse executeScoringModel(MlScoringRequest request) {
        log.info("Executing Python ML script at {} for application {}", scriptPath, request.getApplicationId());
        
        try {
            // 1. Serialize request to JSON
            String jsonRequest = objectMapper.writeValueAsString(request);

            // 2. Build process command
            ProcessBuilder processBuilder = new ProcessBuilder(pythonCommand, scriptPath);
            processBuilder.redirectErrorStream(false); // keep stderr separate to detect script errors

            // 3. Start process
            Process process = processBuilder.start();

            // 4. Write JSON to Python's standard input
            try (OutputStream os = process.getOutputStream()) {
                os.write(jsonRequest.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            // 5. Wait for process to complete (with a reasonable timeout, e.g., 30 seconds)
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                log.error("Python ML script timed out after 30 seconds.");
                return new MlScoringResponse(null, null, "ML Script execution timed out.");
            }

            // 6. Capture stdout and stderr
            String stdout;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                stdout = reader.lines().collect(Collectors.joining("\n"));
            }

            String stderr;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                stderr = reader.lines().collect(Collectors.joining("\n"));
            }

            // 7. Handle process exit code
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("Python script failed with exit code {}. Stderr: {}", exitCode, stderr);
                return new MlScoringResponse(null, null, "ML Script failed: " + stderr);
            }

            // 8. Deserialize stdout back to response DTO
            if (stdout == null || stdout.trim().isEmpty()) {
                log.error("Python script returned empty output.");
                return new MlScoringResponse(null, null, "Empty output from ML script.");
            }
            
            log.debug("Python script raw output: {}", stdout);
            return objectMapper.readValue(stdout, MlScoringResponse.class);

        } catch (Exception e) {
            log.error("Exception occurred while executing Python ML script.", e);
            return new MlScoringResponse(null, null, "Exception: " + e.getMessage());
        }
    }
}
