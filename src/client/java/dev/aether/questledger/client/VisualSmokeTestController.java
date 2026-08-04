package dev.aether.questledger.client;

import dev.aether.questledger.QuestLedger;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Temporary CI-only visual smoke-test controller. It is completely inert unless
 * QUEST_LEDGER_VISUAL_SMOKE_TEST=1 is present in the process environment.
 */
public final class VisualSmokeTestController {
    private static final boolean ENABLED = "1".equals(
            System.getenv("QUEST_LEDGER_VISUAL_SMOKE_TEST")
    );

    private static int ticks;

    private VisualSmokeTestController() {
    }

    public static void tick(Minecraft client) {
        if (!ENABLED) {
            return;
        }

        ticks++;
        if (ticks == 80) {
            client.gui.setScreen(new QuestLedgerScreen(null));
        } else if (ticks == 120) {
            captureFramebuffer("quest-ledger-builder.png");
            marker("quest-ledger-builder-ready");
        } else if (ticks == 240) {
            client.gui.setScreen(new QuestTypesScreen(null));
        } else if (ticks == 280) {
            captureFramebuffer("quest-ledger-types.png");
            marker("quest-ledger-types-ready");
        } else if (ticks == 400) {
            client.stop();
        }
    }

    private static void captureFramebuffer(String fileName) {
        try {
            GL11.glFinish();

            IntBuffer viewport = BufferUtils.createIntBuffer(4);
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
            int width = viewport.get(2);
            int height = viewport.get(3);
            if (width <= 0 || height <= 0) {
                throw new IOException("Invalid OpenGL viewport: " + width + "x" + height);
            }

            ByteBuffer pixels = BufferUtils.createByteBuffer(width * height * 3);
            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
            GL11.glReadBuffer(GL11.GL_FRONT);
            GL11.glReadPixels(
                    0,
                    0,
                    width,
                    height,
                    GL11.GL_RGB,
                    GL11.GL_UNSIGNED_BYTE,
                    pixels
            );

            BufferedImage image = new BufferedImage(
                    width,
                    height,
                    BufferedImage.TYPE_INT_RGB
            );
            for (int y = 0; y < height; y++) {
                int sourceY = height - 1 - y;
                for (int x = 0; x < width; x++) {
                    int offset = (sourceY * width + x) * 3;
                    int red = pixels.get(offset) & 0xFF;
                    int green = pixels.get(offset + 1) & 0xFF;
                    int blue = pixels.get(offset + 2) & 0xFF;
                    image.setRGB(x, y, (red << 16) | (green << 8) | blue);
                }
            }

            Path output = Path.of(fileName);
            ImageIO.write(image, "png", output.toFile());
            QuestLedger.LOGGER.info(
                    "Captured visual smoke-test framebuffer {} ({}x{})",
                    output.toAbsolutePath(),
                    width,
                    height
            );
        } catch (IOException | RuntimeException exception) {
            QuestLedger.LOGGER.error("Could not capture visual smoke-test framebuffer", exception);
        }
    }

    private static void marker(String fileName) {
        try {
            Files.writeString(Path.of(fileName), "ready\n");
        } catch (IOException exception) {
            QuestLedger.LOGGER.error("Could not write visual smoke-test marker", exception);
        }
    }
}
