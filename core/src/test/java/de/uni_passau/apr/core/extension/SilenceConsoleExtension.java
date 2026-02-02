package de.uni_passau.apr.core.extension;

import org.junit.jupiter.api.extension.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class SilenceConsoleExtension implements BeforeAllCallback, AfterAllCallback {

    private PrintStream originalOut;
    private PrintStream originalErr;

    @Override
    public void beforeAll(ExtensionContext context) {
        originalOut = System.out;
        originalErr = System.err;

        System.setOut(new PrintStream(new ByteArrayOutputStream()));
        System.setErr(new PrintStream(new ByteArrayOutputStream()));
    }

    @Override
    public void afterAll(ExtensionContext context) {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }
}
