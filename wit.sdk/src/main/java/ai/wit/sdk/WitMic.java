package ai.wit.sdk;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.lang.Thread;


/**
 * Created by aric on 9/9/14.
 */

public class WitMic {
    static final public int SAMPLE_RATE = 16000;
    static final private int CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    static final private int FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private boolean _isRecording = false;
    private AudioRecord aRecorder = null;
    private PipedInputStream in;
    private PipedOutputStream out;
    IWitCoordinator _witCoordinator;
    protected boolean _detectSpeechStop;

    Handler _handler = new Handler() {
        public void handleMessage(Message msg) {
            _witCoordinator.stopListening();
        }
    };


    static {
        System.loadLibrary("witvad");
    }

    public native int VadInit();
    public native int VadStillTalking(short[] arr, int length);
    public native void VadClean();

    public WitMic(IWitCoordinator witCoordinator, boolean detectSpeechStop) throws IOException {
        in = new PipedInputStream();
        out = new PipedOutputStream();
        in.connect(out);
        _witCoordinator = witCoordinator;
        _detectSpeechStop = detectSpeechStop;
    }

    public void startRecording()
    {
        if (!_isRecording) {
            aRecorder = getRecorder();
            if (aRecorder.getState() == AudioRecord.STATE_INITIALIZED) {
                _isRecording = true;
                aRecorder.startRecording();
                SamplesReaderThread s = new SamplesReaderThread(this, out, getMinBufferSize());
                s.start();
            } else {
                Log.d("WitMic", "AudioRecord not initialized, calling stop for cleaning!");
                stopRecording();
            }
        }
    }

    public void stopRecording()
    {
        if (_isRecording) {
            aRecorder.stop();
            aRecorder.release();
            _isRecording = false;
        }
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
                CHANNEL, FORMAT) * 10;

        return bufferSize;
    }

    public PipedInputStream getInputStream()
    {
        return in;
    }


     class SamplesReaderThread extends Thread {
        private PipedOutputStream iOut;
        private int iBufferSize;
        private WitMic _witMic;

        public SamplesReaderThread(WitMic witMic, PipedOutputStream out, int bufferSize) {
            iOut = out;
            iBufferSize = bufferSize;
            _witMic = witMic;
        }

        @Override
        public void run()
        {
            int nb;
            int readBufferSize = iBufferSize;
            byte[] bytes = new byte[readBufferSize];
            short buffer[] = new short[readBufferSize];
            int vadResult;
            int skippingSamples = 0;


            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            VadInit();
            try {
                while ((nb = aRecorder.read(buffer, 0, readBufferSize)) > 0) {

                    if (skippingSamples < SAMPLE_RATE) {
                        skippingSamples += nb;
                    }
                    if (skippingSamples >= SAMPLE_RATE && _detectSpeechStop == true) {
                        vadResult = VadStillTalking(buffer, nb);
                        if (vadResult == 0) {
                            //Stop the microphone via a Handler so the stopListeing function
                            // of the IWitCoordinator interface is called on the Wit.startListening
                            //calling thread
                            _handler.sendEmptyMessage(0);
                        }
                    }
                    short2byte(buffer, nb, bytes);
                    iOut.write(bytes, 0, nb * 2);
                }
                iOut.close();
            } catch (IOException e) {
                Log.d("SamplesReaderThread", "IOException: " + e.getMessage());
            }
            _witMic.VadClean();
        }

        protected void short2byte(short[] shorts, int nb, byte[] bytes)
        {
            for (int i = 0; i < nb; i++) {
                bytes[i * 2] = (byte)(shorts[i] & 0xff);
                bytes[i * 2 + 1] = (byte)((shorts[i] >> 8) & 0xff);
            }
        }

    }
}
