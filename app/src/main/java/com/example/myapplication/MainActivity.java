package com.example.myapplication;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    private static final int IMAGE_SIZE = 224 ;
    TextView caption;
    ImageView imageView;
    Button picture;
    Button analyze;
    int imageSize = 224;

    //name of the model stored in Asset
    private static  final String MODEL_PATH = "model.tflite";
    //Instantiate the Interpreter
    private Interpreter tflite;
    private ByteBuffer inputImageBuffer;
    private static  final String WORD_MAP = "WORD_MAP.txt";
    private final int VOCAB_SIZE = 112216;
    private Map<Integer, String> indexToWordMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        caption = findViewById(R.id.result);
        imageView = findViewById(R.id.imageView);
        picture = findViewById(R.id.button);
        analyze = findViewById(R.id.buttonAnalyze);

        try {
            tflite = new Interpreter(loadModelFile());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Carica la mappatura tra indici e parole dal file di testo
        indexToWordMap = loadIndexToWordMap(WORD_MAP);

        // Inizializza il buffer di input per l'immagine
        int imageSize = 224; // Sostituisci con le dimensioni corrette del tuo modello
        inputImageBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
        inputImageBuffer.order(ByteOrder.nativeOrder());

        picture.setOnClickListener(view -> {
            // Launch camera if we have permission
            if (checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                someActivityResultLauncher.launch(cameraIntent);
            } else {
                //Request camera permission if we don't have it.
                requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
            }
        });
    }

    // Carica il file del modello TensorFlow Lite dal tuo asset
    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = getAssets().openFd(MODEL_PATH);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    // Carica la mappatura tra indici e parole da un file di testo
    private Map<Integer, String> loadIndexToWordMap(String fileName) {
        Map<Integer, String> map = new HashMap<>();
        try {
            InputStream is = getAssets().open(fileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            String line;
            int index = 0;
            while ((line = reader.readLine()) != null) {
                map.put(index, line.trim());
                index++;
            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }

    // Esegui inferenza sull'immagine
    private String runInference(Bitmap bitmap) {
        // Preprocessa l'immagine e carica i dati nel buffer di input
        preprocessImage(bitmap);

        // Effettua l'inferenza
        float[][] outputScores = new float[1][VOCAB_SIZE]; // Sostituisci con la dimensione corretta del tuo vocabolario
        tflite.run(inputImageBuffer, outputScores);

        // Post-processa i risultati e genera la caption
        String caption = postprocessResults(outputScores);

        return caption;
    }

    // Preprocessa l'immagine e carica i dati nel buffer di input
    private void preprocessImage(Bitmap bitmap) {
        // Sostituisci con la tua logica di preelaborazione dell'immagine
        // Assicurati di adattare questa logica alle esigenze specifiche del tuo modello
        bitmap = Bitmap.createScaledBitmap(bitmap, IMAGE_SIZE, IMAGE_SIZE, true);

        int[] intValues = new int[IMAGE_SIZE * IMAGE_SIZE];
        float[] floatValues = new float[IMAGE_SIZE * IMAGE_SIZE * 3];

        bitmap.getPixels(intValues, 0, IMAGE_SIZE, 0, 0, IMAGE_SIZE, IMAGE_SIZE);

        for (int i = 0; i < intValues.length; ++i) {
            final int val = intValues[i];
            floatValues[i * 3] = ((float)((val >> 16) & 0xFF))/255;//R
            floatValues[i * 3 + 1] = ((float)((val >> 8) & 0xFF))/255;//G
            floatValues[i * 3 + 2] = ((float)((val & 0xFF)))/255;//B
        }
    }

    // Post-processa i risultati e genera la caption
    private String postprocessResults(float[][] outputScores) {
        // Sostituisci con la tua logica di post-elaborazione
        // Esempio: restituisci la parola corrispondente al punteggio massimo
        int maxIndex = 0;
        for (int i = 1; i < VOCAB_SIZE; i++) {
            if (outputScores[0][i] > outputScores[0][maxIndex]) {
                maxIndex = i;
            }
        }
        return "Caption generata: " + getIndexToWord(maxIndex); // Sostituisci con la tua mappatura
    }

    // Mappa l'indice al termine corrispondente nel tuo vocabolario
    private String getIndexToWord(int index) {
        if (indexToWordMap.containsKey(index)) {
            return indexToWordMap.get(index);
        } else {
            return "Parola sconosciuta";
        }
    }

    // Chiamato quando hai un'immagine da processare
    private void processImage(Bitmap imageBitmap) {
        String caption = runInference(imageBitmap);
        updateUIWithCaption(caption);
    }
    // Aggiorna l'interfaccia utente con la caption generata
    private void updateUIWithCaption(String caption) {
        TextView captionTextView = findViewById(R.id.result);
        captionTextView.setText(caption);
    }

    private final ActivityResultLauncher<Intent> someActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // There are no request codes
                        Intent data = result.getData();
                        Bitmap image = (Bitmap) data.getExtras().get("data");
                        int dimension = Math.min(image.getWidth(), image.getHeight());
                        image = ThumbnailUtils.extractThumbnail(image, dimension, dimension);
                        imageView.setImageBitmap(image);
                        image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
                        Bitmap finalImage = image;
                        analyze.setOnClickListener(v -> {
                            processImage(finalImage);
                        });
                    }
                }
            });
}