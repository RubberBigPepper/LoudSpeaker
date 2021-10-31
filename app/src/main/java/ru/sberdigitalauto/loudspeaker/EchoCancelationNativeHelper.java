package ru.sberdigitalauto.loudspeaker;

public class EchoCancelationNativeHelper {

    private final int ECHO_TIME_MS = 300;

    private long aecNativeHandler = 0;//это хандлер AEC из нативной части

    private native long createAEC(int bufferSize, int filterSize);
    private native void closeAEC(long aecHandler);
    private native void cancelEcho(long aecHandler, byte[] micInput, byte[] echoInput, byte[] echoOut);

    private byte[] echoOut = null;

    public boolean prepareAEC(AudioConfig audioConfig) {
        int bufferSize = audioConfig.getRecordingBufferSize() / 2;//у нас 16 битные сэмплы
        int filterSize = audioConfig.getSampleRate() * ECHO_TIME_MS / 1000; //размер фильра в сэмплах
        echoOut = new byte[bufferSize];
        aecNativeHandler = createAEC(bufferSize, filterSize);
        return aecNativeHandler != 0;
    }


    public void releaseAEC(){
        closeAEC(aecNativeHandler);
    }

    public byte[] makeAEC(byte[] micInput, byte[] echoInput){
        cancelEcho(aecNativeHandler, micInput, echoInput, echoOut);
        return echoOut;
    }
}
