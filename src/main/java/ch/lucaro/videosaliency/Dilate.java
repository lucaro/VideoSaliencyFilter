package ch.lucaro.videosaliency;

public class Dilate {

    public static float[][] dilate(float[][] input, int radius){

        float[][] output = new float[input.length][input[0].length];

        for (int x = 0; x < output.length; ++x){
            for (int y = 0; y < output[x].length; ++y){
                float max = 0;

                for (int xx = Math.max(0, x - radius); xx < Math.min(output.length, x + radius); ++xx){
                    for (int yy = Math.max(0, y - radius); yy < Math.min(output[x].length, y + radius); ++y){
                        max = Math.max(max, input[xx][yy]);
                    }
                }

                output[x][y] = max;

            }
        }

        return output;

    }

}
