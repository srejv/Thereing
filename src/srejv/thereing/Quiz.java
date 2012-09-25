package srejv.thereing;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.puredata.android.io.AudioParameters;
import org.puredata.android.service.PdService;
import org.puredata.android.utils.PdUiDispatcher;
import org.puredata.core.PdBase;
import org.puredata.core.utils.IoUtils;

import android.os.Bundle;
import android.os.IBinder;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class Quiz extends Activity implements OnClickListener {

	private static final String TAG = "GuitarTuner";
	private PdUiDispatcher dispatcher;
	
	private PdService pdService = null;
	
	private Button buttonA, buttonAs, buttonB, 
		buttonC, buttonCs, buttonD, 
		buttonDs, buttonE, buttonF, 
		buttonFs, buttonG, buttonGs,
		buttonPlay;
	private TextView tvStatus;
	
	private int currentNote;
	private int currentOctave;
	
	private int startNote = 60;
	private int endNote = 71;
	
	private int correct;
	private int missed;
	
	private Random random;
	
	private final ServiceConnection pdConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName name, IBinder service) {
			pdService = ((PdService.PdBinder)service).getService();
			try {
				initPd();
				loadPatch();
			} catch(IOException e) {
				Log.e(TAG, e.toString());
				finish();
			}
		}
		
		public void onServiceDisconnected(ComponentName name) {
			// This method will NEVER be called MUAHAH
		}
	};
	
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	unbindService(pdConnection);
    }
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initGui();
        initSystemServices();
        bindService(new Intent(this, PdService.class), pdConnection, BIND_AUTO_CREATE);
        initGame();
    }
    
    private void initGame() {
    	random = new Random();
    	currentOctave = 5; // Starts at -5 for midi but it's easier math using this
    	correct = 0;
    	missed = 0;
    	nextNote();
    }
    
    private void initGui() {
    	setContentView(R.layout.activity_quiz);
    	
    	buttonC = (Button) findViewById(R.id.ButtonC);
    	buttonC.setOnClickListener(this);
    	
    	buttonCs = (Button) findViewById(R.id.ButtonCsharp);
    	buttonCs.setOnClickListener(this);
    	
    	buttonD = (Button) findViewById(R.id.ButtonD);
    	buttonD.setOnClickListener(this);
    	
    	buttonDs = (Button) findViewById(R.id.ButtonDsharp);
    	buttonDs.setOnClickListener(this);
    	
    	buttonE = (Button) findViewById(R.id.ButtonE);
    	buttonE.setOnClickListener(this);
    	
    	buttonF = (Button) findViewById(R.id.ButtonF);
    	buttonF.setOnClickListener(this);
    	
    	buttonFs = (Button) findViewById(R.id.ButtonFsharp);
    	buttonFs.setOnClickListener(this);
    	
    	buttonG = (Button) findViewById(R.id.ButtonG);
    	buttonG.setOnClickListener(this);
    	
    	buttonGs = (Button) findViewById(R.id.ButtonGsharp);
    	buttonGs.setOnClickListener(this);
    	
    	buttonA = (Button) findViewById(R.id.buttonA);
    	buttonA.setOnClickListener(this);
    	
    	buttonAs = (Button) findViewById(R.id.ButtonAsharp);
    	buttonAs.setOnClickListener(this);
    	
    	buttonB = (Button) findViewById(R.id.ButtonB);
    	buttonB.setOnClickListener(this);
    	
    	buttonPlay = (Button) findViewById(R.id.buttonPlay);
    	buttonPlay.setOnClickListener(this);
    	
    	tvStatus = (TextView) findViewById(R.id.textViewStatus);
    }
    
	private void loadPatch() throws IOException {
		// Finds the patch in a zip and loads it.
    	File dir = getFilesDir();
    	IoUtils.extractZipResource(
    			getResources().openRawResource(R.raw.midinoteplayer), dir, true);
    	File patchFile = new File(dir, "midinoteplayer.pd");
    	PdBase.openPatch(patchFile.getAbsolutePath());
		
	}

	private void initPd() throws IOException {
    	// Configure the audio glue
    	int sampleRate = AudioParameters.suggestSampleRate();
    	pdService.initAudio(sampleRate, 0, 2, 10.0f);
    	start();
    	
    	// Create and install the dispatcher
    	dispatcher = new PdUiDispatcher();
    	PdBase.setReceiver(dispatcher);
	}
	
	private void start() {
    	if (!pdService.isRunning()) {
    	Intent intent = new Intent(Quiz.this, Quiz.class);
    		pdService.startAudio(intent, R.drawable.icon,
    			"Quiz", "Return to Quiz.");
    	}
    }
	
	private void initSystemServices() {
		TelephonyManager telephonyManager =
				(TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		telephonyManager.listen(new PhoneStateListener() {
			@Override
			public void onCallStateChanged(int state, String incomingNumber) {
				if (pdService == null) return;
				if (state == TelephonyManager.CALL_STATE_IDLE) {
					start(); } else {
						pdService.stopAudio(); }
			}
		}, PhoneStateListener.LISTEN_CALL_STATE);
	}
	
	private void triggerNote(int n) {
		PdBase.sendFloat("midinote", n);
		PdBase.sendBang("trigger");
	}
	
	private void checkNote(int note) {
		if(currentNote == startNote + note) {
			tvStatus.setText("Rätt! Det var " + noteToString(note) + "!");
			correct++;
		} else {
			tvStatus.setText("Fel! Det var " + noteToString(currentNote%12) + "!");
			missed++;
		}
		
		nextNote();
	}
	
	private void nextNote() {
		currentNote = startNote + random.nextInt(12);
	}
	
	private String noteToString(int note) {
		switch(note) {
		case 0:
			return "C";
		case 1:
			return "C#";
		case 2:
			return "D";
		case 3:
			return "D#";
		case 4:
			return "E";
		case 5:
			return "F";
		case 6:
			return "F#";
		case 7:
			return "G";
		case 8:
			return "G#";
		case 9:
			return "A";
		case 10:
			return "A#";
		case 11:
			return "B";
		default:
				return "";
		}
	}
	
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch(v.getId()) {
		case R.id.buttonPlay:
			triggerNote(currentNote);
			break;
		case R.id.ButtonC:
			checkNote(0);
			break;
		case R.id.ButtonCsharp:
			checkNote(1);
			break;
		case R.id.ButtonD:
			checkNote(2);
			break;
		case R.id.ButtonDsharp:
			checkNote(3);
			break;
		case R.id.ButtonE:
			checkNote(4);
			break;
		case R.id.ButtonF:
			checkNote(5);
			break;
		case R.id.ButtonFsharp:
			checkNote(6);
			break;
		case R.id.ButtonG:
			checkNote(7);
			break;
		case R.id.ButtonGsharp:
			checkNote(8);
			break;
		case R.id.buttonA:
			checkNote(9);
			break;
		case R.id.ButtonAsharp:
			checkNote(10);
			break;
		case R.id.ButtonB:
			checkNote(11);
			break;
		default:
			break;
		}
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_quiz, menu);
        return true;
    }
}
