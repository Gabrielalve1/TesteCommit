package com.gabriel.bydassistant;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {
    private static final int REQ_AUDIO = 1001;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private SpeechRecognizer recognizer;
    private Intent recognizerIntent;
    private TextToSpeech tts;
    private TextView status;
    private TextView transcript;
    private EditText keyInput;
    private SharedPreferences prefs;
    private boolean enabled = false;
    private boolean speaking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("byd_assistant", MODE_PRIVATE);
        buildUi();
        tts = new TextToSpeech(this, this);

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
        } else {
            setupRecognizer();
        }
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(42, 30, 42, 30);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Color.rgb(16,16,16));

        TextView title = new TextView(this);
        title.setText("BYD Assistente");
        title.setTextSize(30);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        status = new TextView(this);
        status.setText("Preparando...");
        status.setTextSize(20);
        status.setTextColor(Color.rgb(0,200,83));
        status.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(-1, -2);
        statusLp.setMargins(0,18,0,18);
        root.addView(status, statusLp);

        transcript = new TextView(this);
        transcript.setText("Quando estiver verde, é só falar. Não precisa tocar no microfone.");
        transcript.setTextSize(18);
        transcript.setTextColor(Color.LTGRAY);
        transcript.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams trLp = new LinearLayout.LayoutParams(-1, 0, 1f);
        root.addView(transcript, trLp);

        keyInput = new EditText(this);
        keyInput.setHint("Cole sua NVIDIA API Key aqui uma vez");
        keyInput.setSingleLine(true);
        keyInput.setTextColor(Color.WHITE);
        keyInput.setHintTextColor(Color.GRAY);
        keyInput.setBackgroundColor(Color.rgb(38,38,38));
        String saved = prefs.getString("nvidia_key", "");
        if (!saved.isEmpty()) keyInput.setText(saved);
        root.addView(keyInput, new LinearLayout.LayoutParams(-1, -2));

        Button start = new Button(this);
        start.setText("ATIVAR ASSISTENTE");
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(-1, -2);
        btnLp.setMargins(0,16,0,0);
        root.addView(start, btnLp);
        start.setOnClickListener(v -> {
            String key = keyInput.getText().toString().trim();
            if (key.isEmpty()) {
                status.setText("Cole a chave NVIDIA primeiro");
                return;
            }
            prefs.edit().putString("nvidia_key", key).apply();
            enabled = true;
            keyInput.setVisibility(View.GONE);
            start.setVisibility(View.GONE);
            speak("Pronto. Estou ouvindo. Pode falar comigo.");
        });

        setContentView(root);
    }

    private void setupRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status.setText("Reconhecimento de voz não disponível neste Android");
            return;
        }
        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "pt-BR");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) { if (enabled) status.setText("Ouvindo..."); }
            @Override public void onBeginningOfSpeech() { status.setText("Pode falar..."); }
            @Override public void onRmsChanged(float rmsdB) { }
            @Override public void onBufferReceived(byte[] buffer) { }
            @Override public void onEndOfSpeech() { status.setText("Pensando..."); }
            @Override public void onError(int error) {
                if (enabled && !speaking) handler.postDelayed(MainActivity.this::startListening, 700);
            }
            @Override public void onResults(Bundle results) {
                ArrayList<String> texts = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (texts == null || texts.isEmpty()) {
                    if (enabled) startListening();
                    return;
                }
                String userText = texts.get(0);
                transcript.setText("Você: " + userText);
                askNvidia(userText);
            }
            @Override public void onPartialResults(Bundle partialResults) {
                ArrayList<String> texts = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (texts != null && !texts.isEmpty()) transcript.setText("Você: " + texts.get(0));
            }
            @Override public void onEvent(int eventType, Bundle params) { }
        });
    }

    private void startListening() {
        if (!enabled || speaking || recognizer == null) return;
        try {
            recognizer.cancel();
            recognizer.startListening(recognizerIntent);
        } catch (Exception e) {
            status.setText("Erro no microfone: " + e.getMessage());
            handler.postDelayed(this::startListening, 1200);
        }
    }

    private void askNvidia(String userText) {
        status.setText("Pensando...");
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("https://integrate.api.nvidia.com/v1/chat/completions");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(20000);
                conn.setReadTimeout(60000);
                conn.setDoOutput(true);
                conn.setRequestProperty("Authorization", "Bearer " + prefs.getString("nvidia_key", ""));
                conn.setRequestProperty("Content-Type", "application/json");

                JSONObject body = new JSONObject();
                body.put("model", "meta/llama-3.1-8b-instruct");
                body.put("temperature", 0.6);
                body.put("max_tokens", 220);
                body.put("stream", false);

                JSONArray messages = new JSONArray();
                JSONObject sys = new JSONObject();
                sys.put("role", "system");
                sys.put("content", "Você é uma assistente de voz automotiva. Responda sempre em português brasileiro, de forma natural, curta e conversacional. Não use markdown. Não diga que é um modelo de linguagem. Se não souber algo, diga de forma simples.");
                messages.put(sys);
                JSONObject usr = new JSONObject();
                usr.put("role", "user");
                usr.put("content", userText);
                messages.put(usr);
                body.put("messages", messages);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                }

                int code = conn.getResponseCode();
                InputStream in = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);

                if (code < 200 || code >= 300) throw new Exception("HTTP " + code + ": " + sb);
                JSONObject json = new JSONObject(sb.toString());
                String answer = json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content").trim();
                runOnUiThread(() -> {
                    transcript.setText("Assistente: " + answer);
                    speak(answer);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    status.setText("Falha na IA");
                    transcript.setText(e.getMessage());
                    handler.postDelayed(this::startListening, 1500);
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private void speak(String text) {
        if (tts == null) return;
        speaking = true;
        if (recognizer != null) recognizer.cancel();
        status.setText("Falando...");
        Bundle params = new Bundle();
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, "assistant_reply");
    }

    @Override
    public void onInit(int statusCode) {
        if (statusCode == TextToSpeech.SUCCESS) {
            tts.setLanguage(new Locale("pt", "BR"));
            tts.setSpeechRate(1.02f);
            tts.setPitch(1.0f);
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override public void onStart(String utteranceId) { speaking = true; }
                @Override public void onDone(String utteranceId) {
                    speaking = false;
                    handler.postDelayed(MainActivity.this::startListening, 250);
                }
                @Override public void onError(String utteranceId) {
                    speaking = false;
                    handler.postDelayed(MainActivity.this::startListening, 500);
                }
            });
            status.setText("Pronto para ativar");
        } else {
            status.setText("TTS do Android não iniciou");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_AUDIO && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setupRecognizer();
        } else if (requestCode == REQ_AUDIO) {
            status.setText("Permissão de microfone necessária");
        }
    }

    @Override
    protected void onDestroy() {
        enabled = false;
        if (recognizer != null) recognizer.destroy();
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }
}
