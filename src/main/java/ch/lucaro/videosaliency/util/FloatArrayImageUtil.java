package ch.lucaro.videosaliency.util;

import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import org.vitrivr.cineast.core.color.ReadableRGBContainer;

import java.awt.image.BufferedImage;
import java.util.function.DoubleUnaryOperator;

public class FloatArrayImageUtil {

    public static float[][] dilate(float[][] input, int radius){

        float[][] output = new float[input.length][input[0].length];

        for (int x = 0; x < output.length; ++x){
            for (int y = 0; y < output[x].length; ++y){
                float max = 0;

                for (int xx = Math.max(0, x - radius); xx < Math.min(output.length, x + radius); ++xx){
                    for (int yy = Math.max(0, y - radius); yy < Math.min(output[x].length, y + radius); ++yy){
                        max = Math.max(max, input[xx][yy]);
                    }
                }
                output[x][y] = max;
            }
        }

        return output;

    }

    public static int floatToIntColor(float f){
        int i = (int) (f * 255);
        return i | (i << 8) | (i << 16) | (255 << 24);
    }

    public static BufferedImage toBufferedImage(float[][] fimg){

        final int width = fimg[0].length, height = fimg.length;

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int x = 0; x < width; ++x){
            for (int y = 0; y < height; ++y){
                img.setRGB(x, y, floatToIntColor(fimg[y][x]));
            }
        }

        return img;
    }

    public static float[][] fromBufferedImage(BufferedImage bimg){

        float[][] fimg = new float[bimg.getWidth()][bimg.getHeight()];

        final int width = fimg[0].length, height = fimg.length;

        for (int x = 0; x < width; ++x){
            for (int y = 0; y < height; ++y){
                fimg[y][x] = ReadableRGBContainer.getRed(bimg.getRGB(x, y)) / 255f;
            }
        }

        return fimg;

    }

    public static void forEachInplace(float[][] fimg, DoubleUnaryOperator operator){

        for (int x = 0; x < fimg.length; ++x) {
            for (int y = 0; y < fimg[x].length; ++y) {
                fimg[x][y] = (float) operator.applyAsDouble(fimg[x][y]);
            }
        }

    }

    public static GrayF32 toImageGray(float[][] fimg, GrayF32 img){
        final int width = fimg[0].length, height = fimg.length;

        GrayF32 ret;

        if (img != null && img.width == width && img.height == height){
            ret = img;
        } else {
            ret = new GrayF32(width, height);
        }



        for (int x = 0; x < width; ++x){
            for (int y = 0; y < height; ++y){
                ret.unsafe_set(x, y, fimg[y][x]);
            }
        }

        return ret;
    }

}
