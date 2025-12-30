package me.mdbell.awtea.util;

import java.io.File;
import java.io.IOException;

import me.mdbell.awtea.util.ast.ProgramNode;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;

public class ShaderTest {

    private static final Logger log = LoggerFactory.getLogger(ShaderTest.class);

    public static void main(String[] args) throws IOException {
        File f = new File("test.asal");
        log.info("Loading {}", f.getAbsolutePath());

        ShaderTokenizer tokenizer = new ShaderTokenizer(f);
        ShaderParser parser = new ShaderParser(tokenizer);

        ProgramNode node = parser.parse();

        log.info("ProgramNode: {}", node);
    }
}
