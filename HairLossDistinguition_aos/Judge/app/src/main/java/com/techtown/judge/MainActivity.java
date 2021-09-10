package com.techtown.judge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private ImageView imageView;
    private TextView textView;
    private TextView textView2;
    private File file;
    Interpreter tflite = null;
    String mCurrentPhotoPath;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        File sdcard = Environment.getExternalStorageDirectory();
        file = new File(sdcard,"capture.jpg");

        textView = findViewById(R.id.textView2);
        textView2 = findViewById(R.id.textView3);
        imageView = findViewById(R.id.imageView);

        Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                capture();
            }
        });

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                Log.d("Debug", "권한 설정 완료"); }
            else {
                Log.d("Debug", "권한 설정 요청");
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }

    }

    private void capture() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = null;
        try {
            File tempDir = getCacheDir();

            String timeStamp = new SimpleDateFormat("yyyyMMdd").format(new Date());
            String imageFileName = "Capture_" + timeStamp + "_";

            File tempImage = File.createTempFile(
                    imageFileName,
                    ".jpg",
                    tempDir
            );
            mCurrentPhotoPath = tempImage.getAbsolutePath();
            photoFile = tempImage;
        } catch (IOException e){
            Log.d("debug", "파일생성에러");
        }
        if (photoFile != null){
            Uri photoURI = FileProvider.getUriForFile(this, getPackageName() + ".fileProvider", photoFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            startActivityForResult(intent, 101);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data){
        super.onActivityResult(resultCode,resultCode,data);
        if(requestCode == 101) {
            File file2 = new File(mCurrentPhotoPath);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 8;
            Bitmap bitmap = null;
            bitmap = BitmapFactory.decodeFile(file2.getAbsolutePath(), options);
            imageView.setImageBitmap(bitmap);
            float[][][][] input = new float[1][224][224][3];
            float[][] output = new float[1][1];
            output[0][0] = 4444.444F;
            try {
                Bitmap tfbit = MediaStore.Images.Media.getBitmap(getContentResolver(), Uri.fromFile(file2));
                Bitmap re_tfbit = tfbit.createScaledBitmap(tfbit,224,224,true);
                for (int x = 0; x < 224; x++) {
                    for (int y = 0; y < 224; y++) {
                        int pixel = re_tfbit.getPixel(x, y);
                        input[0][x][y][0] = Color.red(pixel) / 255.0f;
                        input[0][x][y][1] = Color.green(pixel) / 255.0f;
                        input[0][x][y][2] = Color.blue(pixel) / 255.0f;
                    }
                }
            } catch (IOException e) {
                Log.d("debug","오류3");
                Toast.makeText(getApplicationContext(), "비트맵 오류", Toast.LENGTH_SHORT).show();
            }
            try {
                Interpreter lite = getTfliteInterpreter("hair_model.tflite");
                lite.run(input, output);
            } catch (Exception e) {
                Log.d("debug","오류4");
                Toast.makeText(getApplicationContext(), "불러오기 오류", Toast.LENGTH_SHORT).show();
            }
            int value = Math.round(output[0][0]);
            if(value == 1){
                textView.setText("합격");
            }
            else{
                textView.setText("탈모..!");
            }
            textView2.setText(Float.toString(output[0][0]));
        }
    }
    private Interpreter getTfliteInterpreter(String modelPath) {
        try {
            return new Interpreter(loadModelFile(MainActivity.this, modelPath));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    private MappedByteBuffer loadModelFile(Activity activity, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d("Debug", "onRequestPermissionsResult");
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED ) {
            Log.d("Debug", "Permission: " + permissions[0] + "was " + grantResults[0]);
        }
    }
}