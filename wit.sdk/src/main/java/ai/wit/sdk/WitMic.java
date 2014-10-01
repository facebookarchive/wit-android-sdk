package ai.wit.sdk;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.gson.JsonElement;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.lang.Thread;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;

import ai.wit.sdk.IWitListener;

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
            aRecorder.stop();
            _witCoordinator.stopListening();
        }
    };


    static {
        System.loadLibrary("witvad");
    }

    public native int VadInit();
    public native int VadStillTalking(short[] arr);
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
        _isRecording = true;
        aRecorder = getRecorder();
        SamplesReaderThread s = new SamplesReaderThread(this, out, getMinBufferSize());
        s.start();

    }

    public void stopRecording()
    {
        aRecorder.stop();
        _isRecording = false;
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
            byte buffer[] = new byte[readBufferSize];
            short[] bufferShort;
            int vadResult;
            ByteBuffer bBuffer;
            int i = 0;

            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            aRecorder.startRecording();
            VadInit();
            try {
                while ((nb = aRecorder.read(buffer, 0, readBufferSize)) > 0) {
                    i++;
                    bBuffer = ByteBuffer.wrap(buffer, 0, nb);
                    bufferShort = getShortsFromBytes(buffer, nb);
                    if (_detectSpeechStop == true) {
                        vadResult = VadStillTalking(bufferShort);
                        if (vadResult == 0) {
                            //Stop the microphone via a Handler so the stopListeing function
                            // of the IWitCoordinator interface is called on the Wit.startListening
                            //calling thread
                            _handler.sendEmptyMessage(0);
                        }
                    }
                    iOut.write(buffer, 0, nb);
                }
                iOut.close();
            } catch (IOException e) {
                Log.d("SamplesReaderThread", "IOException: " + e.getMessage());
            }
            _witMic.VadClean();
        }

        protected short[] getShortsFromBytes(byte[] bytes, int length)
        {
            short[] shorts = new short[length / 2];
            ByteBuffer bb;
            short shortVal;
            int shorti = 0;
            int i = 0;

            while (i < length) {
                bb = ByteBuffer.allocate(2);
                bb.order(ByteOrder.LITTLE_ENDIAN);
                bb.put(bytes[i]);
                bb.put(bytes[i + 1]);
                shortVal = bb.getShort(0);
                shorts[shorti] = shortVal;
                shorti++;
                i += 2;
            }

            return shorts;
        }
    }
}
