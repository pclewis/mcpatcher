package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.MCPatcherUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;

public class FontUtils {
    private static final int ROWS = 16;
    private static final int COLS = 16;

    private static final boolean showLines = false;

    public static float[] computeCharWidths(String filename, BufferedImage image, int[] rgb, int[] charWidth) {
        MCPatcherUtils.log("computeCharWidths(%s)", filename);
        float[] charWidthf = new float[charWidth.length];
        int width = image.getWidth();
        int height = image.getHeight();
        int colWidth = width / COLS;
        int rowHeight = height / ROWS;
        for (int ch = 0; ch < charWidth.length; ch++) {
            int row = ch / COLS;
            int col = ch % COLS;
            outer:
            for (int colIdx = colWidth - 1; colIdx >= 0; colIdx--) {
                int x = col * colWidth + colIdx;
                for (int rowIdx = 0; rowIdx < rowHeight; rowIdx++) {
                    int y = row * rowHeight + rowIdx;
                    int pixel = rgb[x + y * width];
                    if (isOpaque(pixel)) {
                        if (printThis(ch)) {
                            MCPatcherUtils.log("'%c' pixel (%d, %d) = %08x", (char) ch, x, y, pixel);
                        }
                        charWidthf[ch] = (128.0f * (float) colIdx + 256.0f) / (float) width;
                        if (showLines) {
                            for (int i = 0; i < rowHeight; i++) {
                                y = row * rowHeight + i;
                                for (int j = 0; j < Math.max(colWidth / 16, 1); j++) {
                                    image.setRGB(x + j, y, (i == rowIdx ? 0xff0000ff : 0xffff0000));
                                    image.setRGB(col * colWidth + j, y, 0xff00ff00);
                                }
                            }
                        }
                        break outer;
                    }
                }
            }
            if (ch == 32) {
                charWidthf[ch] = 4.0f;
            }
            if (charWidthf[ch] <= 0.0f) {
                charWidthf[ch] = 2.0f;
            }
        }
        try {
            getCharWidthOverrides(filename, charWidthf);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        for (int ch = 0; ch < charWidth.length; ch++) {
            charWidth[ch] = Math.round(charWidthf[ch]);
            if (printThis(ch)) {
                MCPatcherUtils.log("charWidth['%c'] = %f", (char) ch, charWidthf[ch]);
            }
        }
        return charWidthf;
    }

    private static boolean isOpaque(int pixel) {
        return (pixel & 0xff) > 0;
    }

    private static boolean printThis(int ch) {
        return (ch >= 'A' && ch <= 'F') || (ch >= 'a' && ch <= 'f');
    }

    private static void getCharWidthOverrides(String font, float[] charWidthf) {
        String textFile = font.replace(".png", "_widths.txt");
        Class<?> utils;
        try {
            utils = Class.forName(MCPatcherUtils.TEXTURE_UTILS_CLASS);
        } catch (ClassNotFoundException e) {
            return;
        }
        InputStream is;
        try {
            Method getResource = utils.getDeclaredMethod("getResourceAsStream", String.class);
            Object o = getResource.invoke(null, textFile);
            if (!(o instanceof InputStream)) {
                return;
            }
            is = (InputStream) o;
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        MCPatcherUtils.log("reading character widths from %s", textFile);
        try {
            Properties props = new Properties();
            props.load(is);
            for (Map.Entry entry : props.entrySet()) {
                String key = entry.getKey().toString().trim();
                String value = entry.getValue().toString().trim();
                if (!value.equals("")) {
                    try {
                        int ch = Integer.parseInt(key);
                        float width = Float.parseFloat(value);
                        if (ch >= 0 && ch < charWidthf.length) {
                            MCPatcherUtils.log("    setting charWidthf[%d] to %f", ch, width);
                            charWidthf[ch] = width;
                        }
                    } catch (NumberFormatException e) {
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            MCPatcherUtils.close(is);
        }
    }
}
