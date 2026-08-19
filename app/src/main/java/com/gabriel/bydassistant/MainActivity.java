package com.gabriel.bydassistant;

import android.Manifest;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;
import org.vosk.android.StorageService;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener, RecognitionListener {
    private static final int REQ_AUDIO = 1001;
    private static final String NVIDIA_URL = "https://integrate.api.nvidia.com/v1/chat/completions";
    private static final String NVIDIA_MODEL = "nvidia/nemotron-3-ultra-550b-a55b";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final JSONArray history = new JSONArray();

    private Model voskModel;
    private SpeechService speechService;
    private TextToSpeech tts;
    private TextView status;
    private TextView transcript;
    private EditText keyInput;
    private Button startButton;
    private SharedPreferences prefs;

    private boolean enabled = false;
    private boolean busy = false;
    private boolean modelReady = false;
    private boolean ttsReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("byd_assistant", MODE_PRIVATE);
        buildUi();
        tts = new TextToSpeech(this, this);

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
        } else {
            initOfflineSpeechModel();
        }
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(50, 34, 50, 34);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Color.rgb(10, 12, 16));

        TextView title = new TextView(this);
        title.setText("LUNA • BYD Assistente");
        title.setTextSize(31);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        status = new TextView(this);
        status.setText("Preparando reconhecimento de voz em português...");
        status.setTextSize(21);
        status.setTextColor(Color.rgb(70, 220, 120));
        status.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(-1, -2);
        statusLp.setMargins(0, 22, 0, 22);
        root.addView(status, statusLp);

        transcript = new TextView(this);
        transcript.setText("Esta versão lê o microfone diretamente e não depende do reconhecimento de voz do Google.");
        transcript.setTextSize(19);
        transcript.setTextColor(Color.LTGRAY);
        transcript.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams trLp = new LinearLayout.LayoutParams(-1, 0, 1f);
        root.addView(transcript, trLp);

        keyInput = new EditText(this);
        keyInput.setHint("Chave NVIDIA nvapi-...");
        keyInput.setSingleLine(true);
        keyInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        keyInput.setTextColor(Color.WHITE);
        keyInput.setHintTextColor(Color.GRAY);
        keyInput.setBackgroundColor(Color.rgb(34, 38, 45));
        String saved = prefs.getString("nvidia_key", "");
        if (!saved.isEmpty()) keyInput.setText(saved);
        root.addView(keyInput, new LinearLayout.LayoutParams(-1, -2));

        startButton = new Button(this);
        startButton.setText("ATIVAR LUNA");
        startButton.setEnabled(false);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(-1, -2);
        btnLp.setMargins(0, 18, 0, 0);
        root.addView(startButton, btnLp);

        startButton.setOnClickListener(v -> activateAssistant());
        setContentView(root);
    }

    private void initOfflineSpeechModel() {
        status.setText("Carregando português offline...");
        StorageService.unpack(this, "model-pt", "model-pt-runtime",
                model -> {
                    voskModel = model;
                    modelReady = true;
                    status.setText("Português pronto • toque em ATIVAR LUNA");
                    startButton.setEnabled(true);
                },
                exception -> {
                    status.setText("Erro ao carregar português");
                    transcript.setText("Modelo de voz: " + exception.getMessage());
                });
    }

    private void activateAssistant() {
        String key = keyInput.getText().toString().trim();
        if (key.isEmpty()) {
            status.setText("Cole sua chave NVIDIA primeiro");
            return;
        }
        if (!modelReady) {
            status.setText("Ainda carregando o português...");
            return;
        }
        prefs.edit().putString("nvidia_key", key).apply();
        enabled = true;
        keyInput.setVisibility(View.GONE);
        startButton.setVisibility(View.GONE);
        transcript.setText("Assistente ativada. Fale normalmente quando aparecer OUVINDO.");

        if (ttsReady) {
            speak("Pronto. Eu sou a Luna. Pode falar comigo.");
        } else {
            startDirectListening();
        }
    }

    private void startDirectListening() {
        if (!enabled || busy || voskModel == null) return;
        try {
            if (speechService == null) {
                Recognizer recognizer = new Recognizer(voskModel, 16000.0f);
                speechService = new SpeechService(recognizer, 16000.0f);
                speechService.startListening(this);
            } else {
                speechService.setPause(false);
            }
            status.setText("OUVINDO... fale comigo");
        } catch (Exception e) {
            status.setText("Erro ao abrir microfone");
            transcript.setText(e.getMessage());
        }
    }

    private String jsonText(String hypothesis, String field) {
        try {
            return new JSONObject(hypothesis).optString(field, "").trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    @Override
    public void onPartialResult(String hypothesis) {
        if (!enabled || busy) return;
        String text = jsonText(hypothesis, "partial");
        if (!text.isEmpty()) {
            transcript.setText("Você: " + text);
            status.setText("TE OUVINDO...");
        }
    }

    @Override
    public void onResult(String hypothesis) {
        if (!enabled || busy) return;
        String text = jsonText(hypothesis, "text");
        if (text.isEmpty()) return;

        busy = true;
        if (speechService != null) speechService.setPause(true);
        transcript.setText("Você: " + text);
        status.setText("PENSANDO...");
        askNvidia(text);
    }

    @Override
    public void onFinalResult(String hypothesis) {
        // O serviço contínuo usa onResult para cada frase. Este callback ocorre ao encerrar.
    }

    @Override
    public void onError(Exception e) {
        if (!enabled) return;
        status.setText("Falha no áudio • tentando novamente");
        transcript.setText(e.getMessage() == null ? "Erro de áudio" : e.getMessage());
        shutdownSpeechService();
        busy = false;
        handler.postDelayed(this::startDirectListening, 900);
    }

    @Override
    public void onTimeout() {
        if (enabled && !busy) startDirectListening();
    }

    private synchronized void appendHistory(String role, String content) {
        try {
            JSONObject m = new JSONObject();
            m.put("role", role);
            m.put("content", content);
            history.put(m);
            while (history.length() > 10) history.remove(0);
        } catch (Exception ignored) { }
    }

    private void askNvidia(String userText) {
        appendHistory("user", userText);
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(NVIDIA_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(20000);
                conn.setReadTimeout(60000);
                conn.setDoOutput(true);
                conn.setRequestProperty("Authorization", "Bearer " + prefs.getString("nvidia_key", ""));
                conn.setRequestProperty("Content-Type", "application/json");

                JSONObject body = new JSONObject();
                body.put("model", NVIDIA_MODEL);
                body.put("temperature", 0.55);
                body.put("max_tokens", 260);
                body.put("stream", false);

                JSONArray messages = new JSONArray();
                JSONObject system = new JSONObject();
                system.put("role", "system");
                system.put("content", "Você é Luna, uma assistente de voz dentro de um carro BYD no Brasil. Converse sempre em português brasileiro natural. Responda como numa conversa falada: frases curtas, úteis e humanas, sem markdown, sem listas longas e sem dizer que é um modelo de linguagem. Mantenha contexto das mensagens anteriores. Se o motorista pedir algo que você ainda não consegue executar no carro, explique em uma frase e continue ajudando.");
                messages.put(system);
                synchronized (this) {
                    for (int i = 0; i < history.length(); i++) messages.put(history.getJSONObject(i));
                }
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
                String answer = json.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                        .trim();

                appendHistory("assistant", answer);
                runOnUiThread(() -> {
                    transcript.setText("Luna: " + answer);
                    speak(answer);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    busy = false;
                    status.setText("Falha na IA • voltando a ouvir");
                    transcript.setText(e.getMessage());
                    handler.postDelayed(this::startDirectListening, 1200);
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private void speak(String text) {
        if (!ttsReady || tts == null) {
            busy = false;
            startDirectListening();
            return;
        }
        if (speechService != null) speechService.setPause(true);
        status.setText("LUNA FALANDO...");
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, new Bundle(), "luna_reply");
    }

    @Override
    public void onInit(int statusCode) {
        if (statusCode == TextToSpeech.SUCCESS) {
            int language = tts.setLanguage(new Locale("pt", "BR"));
            ttsReady = language != TextToSpeech.LANG_MISSING_DATA && language != TextToSpeech.LANG_NOT_SUPPORTED;
            tts.setSpeechRate(1.03f);
            tts.setPitch(1.0f);
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override public void onStart(String utteranceId) { }
                @Override public void onDone(String utteranceId) {
                    handler.post(() -> {
                        busy = false;
                        startDirectListening();
                    });
                }
                @Override public void onError(String utteranceId) {
                    handler.post(() -> {
                        busy = false;
                        startDirectListening();
                    });
                }
            });
        } else {
            ttsReady = false;
        }
    }

    private void shutdownSpeechService() {
        if (speechService != null) {
            try { speechService.stop(); } catch (Exception ignored) { }
            try { speechService.shutdown(); } catch (Exception ignored) { }
            speechService = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_AUDIO && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initOfflineSpeechModel();
        } else if (requestCode == REQ_AUDIO) {
            status.setText("Permissão de microfone necessária");
        }
    }

    @Override
    protected void onDestroy() {
        enabled = false;
        shutdownSpeechService();
        if (voskModel != null) {
            try { voskModel.close(); } catch (Exception ignored) { }
        }
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
