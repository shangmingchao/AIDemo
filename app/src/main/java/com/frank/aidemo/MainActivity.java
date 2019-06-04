package com.frank.aidemo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.FileDescriptor;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_GET = 1;

    private ImageView imageView;
    private TextView resultsTextView;
    private Classifier classifier;

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
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (classifier != null) {
            classifier.close();
        }
    }
}
