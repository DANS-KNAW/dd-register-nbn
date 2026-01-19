/*
 * Copyright (C) 2026 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.registernbn.core;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.registernbn.client.GmhClient;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

@AllArgsConstructor
@Slf4j
public class NbnRegistrationTask implements Runnable {
    private final Path registrationTokenFile;
    private final GmhClient gmhClient;
    private final Path outboxProcessed;
    private final Path outboxFailed;

    @Override
    public void run() {
        try {
            var registrationToken = RegistrationToken.load(registrationTokenFile);
            log.debug("Loaded registration token: {}", registrationToken);
            gmhClient.registerNbn(registrationToken);
            log.info("Successfully registered NBN {}", registrationToken.getNbn());
            Files.move(registrationTokenFile, outboxProcessed.resolve(registrationTokenFile.getFileName()));
        }
        catch (Exception e) {
            log.error("Failed to register NBN", e);
            try {
                var outFile = outboxFailed.resolve(registrationTokenFile.getFileName());
                Files.move(registrationTokenFile, outFile);
                writeErrorFile(outFile, e);
                log.info("Moved registration token file to outbox failed");
            }
            catch (IOException ioException) {
                log.error("Failed to move registration token file to outbox failed", ioException);
            }
        }
    }

    private void writeErrorFile(Path outFile, Exception e) {
        try (var writer = Files.newBufferedWriter(outFile.resolveSibling(outFile.getFileName() + "-error.log"))) {
            writer.write(e.getMessage());
            writer.newLine();
            e.printStackTrace(new PrintWriter(writer));
        }
        catch (IOException ioException) {
            log.error("Failed to write error log for registration token file", ioException);
        }
    }

}
