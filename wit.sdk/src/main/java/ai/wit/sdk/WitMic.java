package ai.wit.sdk;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.google.gson.JsonElement;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.lang.Thread;
import java.nio.ByteOrder;
import java.util.HashMap;

import ai.wit.sdk.IWitListener;

/**
 * Created by aric on 9/9/14.
 */

public class WitMic {
    static final public int SAMPLE_RATE = 44100;
    static final private int CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    static final private int FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private boolean _isRecording = false;
    private AudioRecord aRecorder = null;
    private PipedInputStream in;
    private PipedOutputStream out;

    public WitMic() throws IOException {
        in = new PipedInputStream();
        out = new PipedOutputStream();
        in.connect(out);
    }

    public void startRecording()
    {
        _isRecording = true;
        aRecorder = getRecorder();
        aRecorder.startRecording();
        SamplesReaderThread s = new SamplesReaderThread(aRecorder, out, getMinBufferSize());
        s.start();
    }

    public void stopRecording()
    {
        aRecorder.stop();
        _isRecording = false;
        aRecorder = null;
    }


    public boolean toggle()
    {
        boolean started;

        if (!_isRecording) {
            startRecording();
            started = true;
        } else {
            stopRecording();
            started = false;
        }

        return started;
    }

    public boolean isRecording() {
        return _isRecording;
    }


    public AudioRecord getRecorder()
    {
        int bufferSize;
        AudioRecord audioRecord;

        bufferSize = getMinBufferSize();
        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                CHANNEL, FORMAT, bufferSize);

        return audioRecord;
    }

    protected int getMinBufferSize()
    {
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                CHANNEL, FORMAT);

        return bufferSize;
    }

    public PipedInputStream getInputStream()
    {
        return in;
    }


    private class SamplesReaderThread extends Thread {
        private AudioRecord iRecorder;
        private PipedOutputStream iOut;
        private int iBufferSize;
        public SamplesReaderThread(AudioRecord recorder, PipedOutputStream out, int bufferSize) {
            iOut = out;
            iRecorder = recorder;
            iBufferSize = bufferSize;
        }

        @Override
        public void run()
        {
            int nb;
            byte buffer[] = new byte[iBufferSize];
            try {
                while ((nb = iRecorder.read(buffer, 0, iBufferSize)) > -1) {
                    iOut.write(buffer, 0, nb);
                    if (!_isRecording) {
                        iRecorder.stop();
                        out.close();
                    }
                }
                iOut.close();
            } catch (IOException e) {
                Log.d("SamplesReaderThread", "IOException: " + e.getMessage());
            }
        }

    }
}
