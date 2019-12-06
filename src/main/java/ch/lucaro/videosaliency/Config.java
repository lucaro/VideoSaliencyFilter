package ch.lucaro.videosaliency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Config {

    private float blurRadius = 3f;
    private int dilateRadius = 4;
    private float saturation = 0.1f;
    private int audioBufferSize = 1024;
    private float minFrequency = 100;
    private float maxFrequency = 2000;
    private float noiseAmplitude = 0.005f;
    private float gamma = 0.8f;
    private float cutOffThreshold = 0.5f;

    private static final Logger LOGGER = LogManager.getLogger();
    private static Config instance = null;

    public static Config getConfig() {

        if(instance != null){
            return instance;
        }

        File inputFile = new File("config.json");
        if (!inputFile.exists()){
            LOGGER.info("No config file found, using default values");
            instance = new Config();
            return instance;
        }
        LOGGER.info("Found config file {}", inputFile.getName());
        try {
            instance = (new ObjectMapper()).readValue(inputFile, Config.class);
        } catch (IOException e) {
            LOGGER.error("Error parsing config in '{}': {}", inputFile.getName(), e.getMessage());
            instance = new Config();
        }
        return instance;
    }

    @JsonProperty
    public Float getBlurRadius() {
        return blurRadius;
    }

    private void setBlurRadius(float blurRadius) {
        this.blurRadius = blurRadius;
    }

    @JsonProperty
    public Integer getDilateRadius() {
        return dilateRadius;
    }

    private void setDilateRadius(int dilateRadius) {
        this.dilateRadius = dilateRadius;
    }

    @JsonProperty
    public Float getSaturation() {
        return saturation;
    }

    private void setSaturation(float saturation) {
        this.saturation = saturation;
    }

    @JsonProperty
    public Integer getAudioBufferSize() {
        return audioBufferSize;
    }

    private void setAudioBufferSize(int audioBufferSize) {
        this.audioBufferSize = audioBufferSize;
    }

    @JsonProperty
    public Float getMinFrequency() {
        return minFrequency;
    }

    private void setMinFrequency(float minFrequency) {
        this.minFrequency = minFrequency;
    }

    @JsonProperty
    public Float getMaxFrequency() {
        return maxFrequency;
    }

    private void setMaxFrequency(float maxFrequency) {
        this.maxFrequency = maxFrequency;
    }

    @JsonProperty
    public Float getNoiseAmplitude() {
        return this.noiseAmplitude;
    }

    private void setNoiseAmplitude(float noiseAmplitude){
        this.noiseAmplitude = noiseAmplitude;
    }

    @JsonProperty
    public Float getGamma() { return gamma; }

    private void setGamma(float gamma) { this.gamma = gamma; }

    @JsonProperty
    public Float getCutOffThreshold() { return cutOffThreshold; }

    public void setCutOffThreshold(float cutOffThreshold) { this.cutOffThreshold = cutOffThreshold; }
}
