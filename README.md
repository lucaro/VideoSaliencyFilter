# VideoSaliencyFilter
_A tool for generating saliency-filtered video queries_

## About
The video saliency filter is a tool to approximate human audio-visual memory by removing non-salient visual and auditory
information from a video file. It uses a deep-neural method for predicting the visual most salient regions of a video
by utilizing the model published via https://github.com/remega/OMCNN_2CLSTM. For the audio part, a simple bandpass filter
in conjunction with some brownian noise is used. **This project is currently in an alpha stage.** If you find bugs or have
suggestions, please open an issue.

## Installation and use
This project is implemented in Java and uses TensorFlow. It is highly recommended to be run in a CUDA-enabled environment.
To build and run the tool, just use the following commands:
```
git clone https://github.com/lucaro/VideoSaliencyFilter
cd VideoSaliencyFilter
./gradlew jar
java -jar build/libs/vsf.jar <input video> <output video>
```