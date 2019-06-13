package com.frank.aidemo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_GET = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 2;

    private ImageView imageView;
    private TextView resultsTextView;
    private Classifier classifier;
    private String currentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = findViewById(R.id.classification);
        resultsTextView = findViewById(R.id.results);
        try {
            classifier = new Classifier(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (classifier != null) {
            ViewGroup.LayoutParams layoutParams = imageView.getLayoutParams();
            layoutParams.width = classifier.getImageSizeX();
            layoutParams.height = classifier.getImageSizeY();
            imageView.setLayoutParams(layoutParams);
        }
        findViewById(R.id.chooseImg).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(intent, REQUEST_IMAGE_GET);
                }
            }
        });
        findViewById(R.id.takePicture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    if (photoFile != null) {
                        Uri photoURI = FileProvider.getUriForFile(MainActivity.this,
                                "com.frank.aidemo.fileprovider",
                                photoFile);
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                    }
                }
            }
        });
    }

    private void processImage() {
        if (classifier != null) {
            Bitmap bitmap = Bitmap.createBitmap(
                    classifier.getImageSizeX(), classifier.getImageSizeY(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            imageView.draw(canvas);
            showResults(classifier.recognizeImage(bitmap));
        }
    }

    private void showResults(List<Classifier.Recognition> results) {
        if (results != null && results.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (Classifier.Recognition obj : results) {
                sb.append(obj.getTitle())
                        .append("  ")
                        .append(String.format(Locale.US, "(%.1f%%)", obj.getConfidence() * 100.0f))
                        .append("\n");
            }
            resultsTextView.setText(sb);
        } else {
            resultsTextView.setText(null);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_GET && resultCode == RESULT_OK) {
            Uri fullPhotoUri = data.getData();
            ParcelFileDescriptor descriptor;
            try {
                descriptor = getContentResolver().openFileDescriptor(fullPhotoUri, "r");
                FileDescriptor fd = descriptor.getFileDescriptor();
                Bitmap bitmap = BitmapFactory.decodeFileDescriptor(fd);
                imageView.setImageBitmap(bitmap);
                processImage();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            int targetW = imageView.getWidth();
            int targetH = imageView.getHeight();
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(currentPhotoPath, bmOptions);
            int photoW = bmOptions.outWidth;
            int photoH = bmOptions.outHeight;
            int scaleFactor = Math.min(photoW / targetW, photoH / targetH);
            bmOptions.inJustDecodeBounds = false;
            bmOptions.inSampleSize = scaleFactor;
            Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath, bmOptions);
            imageView.setImageBitmap(bitmap);
            processImage();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (classifier != null) {
            classifier.close();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }
}
