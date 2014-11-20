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
import java.util.Arrays;

import org.jtransforms.fft.FloatFFT_1D;

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
    protected Wit.vadConfig _vad;
    protected Wit.VadTuning _vadTuning;
    protected int _vadTimeout;
    protected boolean _currentStoppedByVad = false;
    public boolean lastStoppedByVad = false;

    Handler _stopHandler = new Handler() {
        public void handleMessage(Message msg) {
            _witCoordinator.stopListening();
        }
    };

    Handler _streamingHandler = new Handler() {
        public void handleMessage(Message msg) {
            _witCoordinator.voiceActivityStarted();
        }
    };


    static {
        System.loadLibrary("witvad");
    }

    public native int VadInit(int vadTuning, int vadTimeout);
    public native int VadStillTalking(short[] samples, float[] fft_mags);
    public native int GetVadSamplesPerFrame();
    public native void VadClean();

    public WitMic(IWitCoordinator witCoordinator, Wit.vadConfig vad, Wit.VadTuning vadTuning, int vadTimeout) throws IOException {
        int inputStreamSize = getPipedInputStreamSize();
        in = new PipedInputStream(inputStreamSize);
        out = new PipedOutputStream();
        in.connect(out);
        _witCoordinator = witCoordinator;
        _vad = vad;
        _vadTuning = vadTuning;
        _vadTimeout = vadTimeout;
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
            lastStoppedByVad = _currentStoppedByVad;
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

    /**
     * AudioRecord will override its buffer as soon as it needs to write new data received from the
     * microphone. So writing the data to the output stream should never block/wait.
     * For that reason, we compute the size needed for the output stream to hold 20 seconds of data.
     */
    protected int getPipedInputStreamSize()
    {
        int nbChannel = 1;
        int nbBits = 16;
        int nbSeconds = 20;
        int bufferSize;

        bufferSize = SAMPLE_RATE * (nbBits / 2) * nbChannel * nbSeconds;

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
        private boolean _streamingStarted = false;

        public SamplesReaderThread(WitMic witMic, PipedOutputStream out, int bufferSize) {
            iOut = out;
            iBufferSize = bufferSize;
            _witMic = witMic;
            if (_vad != Wit.vadConfig.full) {
                _streamingStarted = true;
            }
        }

        @Override
        public void run()
        {
            int nb;
            int readBufferSize = iBufferSize;
            byte[] bytes = new byte[readBufferSize];

            int maxPastBuffers = 5;
            byte[][] pastByteBuffers = new byte[maxPastBuffers][];
            byte[] sizedBuffer;
            short buffer[] = new short[readBufferSize];
            int vadResult;
            int skippingSamples = 0;

            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            VadInit(_vadTuning.ordinal(), _vadTimeout);

            FloatFFT_1D fft = new FloatFFT_1D(GetVadSamplesPerFrame());
            float[] fft_mags = new float[GetVadSamplesPerFrame()/2];
            float[] fft_modules = new float[GetVadSamplesPerFrame()];
            short[] samples;

            _currentStoppedByVad = false;
            try {
                while ((nb = aRecorder.read(buffer, 0, readBufferSize)) > 0) {

                    if (skippingSamples < SAMPLE_RATE) {
                        skippingSamples += nb;
                    }
                    if (skippingSamples >= SAMPLE_RATE && _vad != Wit.vadConfig.disabled) {
                        int samplesAnalyzed = 0;
                        while(samplesAnalyzed + GetVadSamplesPerFrame() < nb){
                            samples = Arrays.copyOfRange(buffer, samplesAnalyzed, samplesAnalyzed +GetVadSamplesPerFrame());
                            for(int i=0; i<GetVadSamplesPerFrame(); i++){
                                fft_modules[i] = (float)samples[i];
                            }
                            fft.realForward(fft_modules); //results are stored in place

                            //transform to magnitudes
                            fft_mags[0]=fft_modules[0];
                            //the 0th (DC) component is different and has no imaginary part
                            for(int i=1; i<GetVadSamplesPerFrame()/2; i++){
                                fft_mags[i]=(float)Math.sqrt(Math.pow(fft_modules[2*i],2)+Math.pow(fft_modules[2*i+1],2));
                            }

                            vadResult = VadStillTalking(buffer, fft_mags);

                            if (_vad == Wit.vadConfig.full && vadResult == 1) {
                                _streamingHandler.sendEmptyMessage(0);
                                _streamingStarted = true;
                                int nbStreamed = streamPastBuffers(pastByteBuffers);
                                Log.d(getClass().getName(), "Just caugth "+ nbStreamed + " buffers");
                            }

                            if (vadResult == 0) {
                                //Stop the microphone via a Handler so the stopListeing function
                                // of the IWitCoordinator interface is called on the Wit.startListening
                                //calling thread
                                _currentStoppedByVad = true;
                                _stopHandler.sendEmptyMessage(0);
                            }
                            samplesAnalyzed+=GetVadSamplesPerFrame();
                        }
                    }
                    short2byte(buffer, nb, bytes);
                    if (_streamingStarted) {
                        iOut.write(bytes, 0, nb * 2);
                    } else {
                        sizedBuffer = Arrays.copyOf(bytes, nb * 2);
                        Log.d("WIT", "Sized buffer length is: " + sizedBuffer.length);
                        pushPastBuffer(pastByteBuffers, sizedBuffer);
                        Log.d(getClass().getName(), "streaming did not start yet");
                    }
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

         protected int streamPastBuffers(byte[][] pastBuffers) throws IOException {
             int length = pastBuffers.length;
             int sentCounter = 0;

             while ((length--) > 0) {
                 if (pastBuffers[length] != null) {
                     iOut.write(pastBuffers[length]);
                     sentCounter++;
                 }
             }

             return sentCounter;
         }

         protected void pushPastBuffer(byte[][] buffers, byte[] buffer) {
            int length = buffers.length;

             while ((--length) > 0) {

                 buffers[length] = buffers[length - 1];
             }
             buffers[0] = buffer;
         }
    }
}
