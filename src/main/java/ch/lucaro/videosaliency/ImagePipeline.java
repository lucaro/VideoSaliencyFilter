package ch.lucaro.videosaliency;

import boofcv.abst.distort.FDistort;
import boofcv.alg.filter.blur.GBlurImageOps;
import boofcv.alg.interpolate.InterpolationType;
import boofcv.alg.misc.GPixelMath;
import boofcv.alg.misc.PixelMath;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;
import ch.lucaro.videosaliency.util.BufferedImageUtil;
import ch.lucaro.videosaliency.util.FloatArrayImageUtil;

import java.awt.image.BufferedImage;

public class ImagePipeline {

    private float blurRadius = 3f;
    private int dilateRadius = 4;
    private float saturation = 0.1f;

    private Planar<GrayF32> output = null, salient = null, background = null, blurred = null;
    private GrayF32 smallMask = null, mask = null, invertedMask = null;

    public ImagePipeline(int maskDilateRadius, float backgroundBlurRadiusPercent, float backgroundSaturation){
        setMaskDilateRadius(maskDilateRadius);
        setBackgroundBlurRadiusPercent(backgroundBlurRadiusPercent);
        setBackgroundSaturation(backgroundSaturation);
    }

    public void setMaskDilateRadius(int maskDilateRadius){
        this.dilateRadius = Math.max(0, Math.min(20, maskDilateRadius));
    }

    public void setBackgroundBlurRadiusPercent(float backgroundBlurRadiusPercent){
        this.blurRadius = Math.max(0, Math.min(10f, backgroundBlurRadiusPercent)); //max 10% blur radius
    }

    public void setBackgroundSaturation(float backgroundSaturation){
        this.saturation = Math.max(0f, Math.min(1f, backgroundSaturation));
    }

    public synchronized BufferedImage process(BufferedImage bimg, float[][] fimg){

        /* prepare mask */

        FloatArrayImageUtil.forEachInplace(fimg, x -> {
            double d = Math.pow(x, 0.8);
            if (d < 0.5){
                return 0;
            }
            return d;
        });

        fimg = FloatArrayImageUtil.dilate(fimg, dilateRadius);

        smallMask = GBlurImageOps.gaussian(FloatArrayImageUtil.toImageGray(fimg, smallMask), null,-1, 5,null);


        /* prepare frame */

        bimg = ConvertBufferedImage.stripAlphaChannel(bimg);
        Planar<GrayF32> inputF = ConvertBufferedImage.convertFrom(bimg, true, ImageType.pl(3, GrayF32.class));


        /*  prepare background */

        BufferedImage desaturated = BufferedImageUtil.adjustSatturation(bimg, this.saturation);

        Planar<GrayF32> inputDesaturated = ConvertBufferedImage.convertFrom(desaturated, true, ImageType.pl(3, GrayF32.class));

        if (blurred == null){
            blurred = inputF.createSameShape();
        }

        GBlurImageOps.gaussian(inputDesaturated, blurred,-1,(int)(Math.max(inputF.width, inputF.height) / 100f * blurRadius),null);


        /* mask foreground */

        if (salient == null){
            salient = inputF.createSameShape();
        }

        if(mask == null){
            mask = new GrayF32(inputF.width, inputF.height);
        }

        FDistort scaler = new FDistort(smallMask, mask);
        scaler.interp(InterpolationType.BICUBIC).scale();
        scaler.apply();

        GPixelMath.multiply(inputF, mask, salient);


        /* mask background */

        if (background == null){
            background = inputF.createSameShape();
        }

        if (invertedMask == null) {
            invertedMask = mask.createSameShape();
        }

        PixelMath.minus(1f, mask, invertedMask);
        GPixelMath.multiply(blurred, invertedMask, background);


        /* combine */

        if (output == null){
            output = inputF.createSameShape();
        }

        GPixelMath.add(background, salient, output);

        return ConvertBufferedImage.convertTo(output, null, true);

    }

}
