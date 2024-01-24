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
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    TextView caption;
    ImageView imageView;
    Button picture;
    Button analyze;
    int imageSize = 224;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        caption = findViewById(R.id.result);
        imageView = findViewById(R.id.imageView);
        picture = findViewById(R.id.button);
        analyze = findViewById(R.id.buttonAnalyze);

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



    /*public void captioning(Bitmap image) throws IOException {
        try {
        Model1 model = Model1.newInstance(getApplicationContext());

        //creates inputs for reference
        TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 7, 7, 576}, DataType.FLOAT32);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
        byteBuffer.order(ByteOrder.nativeOrder());

        //?
        TensorBuffer inputFeature1 = TensorBuffer.createFixedSize(new int[]{1, 1}, DataType.INT64);
        inputFeature1.loadBuffer(byteBuffer);

        int[] intValues = new int[224 * 224];
        image.getPixels(intValues, 0, image.getWidth(),0,0,image.getWidth(), image.getHeight());

        //iterate over pixels
        int pixel = 0;
        for(int i = 0; i < imageSize; i++){
            for(int j = 0; j < imageSize; j++){
                int val = intValues[pixel++]; // RGB
                byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 255.f));
                byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 255.f));
                byteBuffer.putFloat((val & 0xFF) * (1.f / 255.f));
            }
        }
        inputFeature0.loadBuffer(byteBuffer);

        //qui si blocca l'app
        Model1.Outputs outputs = model.process(inputFeature0, inputFeature1);
        TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

        model.close();

        } catch (IOException e) {
        // TODO Handle the exception
           }

    }*/

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
                           /* try {
                                caption.setText(captioning(finalImage));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }*/

                        });
                    }
                }
            });
}