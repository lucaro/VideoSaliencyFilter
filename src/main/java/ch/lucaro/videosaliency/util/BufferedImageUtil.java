package ch.lucaro.videosaliency.util;

import org.vitrivr.cineast.core.color.ReadableRGBContainer;

import java.awt.*;
import java.awt.image.BufferedImage;

public class BufferedImageUtil {

    public static BufferedImage adjustSatturation(BufferedImage bimg, float satturation){

        int[] colors = bimg.getRGB(0, 0, bimg.getWidth(), bimg.getHeight(), null, 0, bimg.getWidth());
        float[] hsb = new float[3];

        for (int i = 0; i < colors.length; i++) {
            int color = colors[i];
            Color.RGBtoHSB(ReadableRGBContainer.getRed(color), ReadableRGBContainer.getGreen(color), ReadableRGBContainer.getBlue(color), hsb);
            colors[i] = Color.HSBtoRGB(hsb[0], hsb[1] * satturation, hsb[2]);
        }

        BufferedImage ret = new BufferedImage(bimg.getWidth(), bimg.getHeight(), BufferedImage.TYPE_INT_RGB);

        ret.setRGB(0, 0, bimg.getWidth(), bimg.getHeight(), colors, 0, bimg.getWidth());

        return ret;

    }

}
