package com.muradaslanov.artbookjava;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.muradaslanov.artbookjava.databinding.ActivityDetailsBinding;

import java.io.ByteArrayOutputStream;

public class DetailsActivity extends AppCompatActivity {

    private ActivityDetailsBinding binding;
    ActivityResultLauncher<Intent> activityResultLauncher;
    ActivityResultLauncher<String> permissionLauncher;
    Bitmap selectedImg;
    SQLiteDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetailsBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        registerLauncher();

        database = this.openOrCreateDatabase("Arts",MODE_PRIVATE,null);

        Intent intent = getIntent();
        String info = intent.getStringExtra("info");

        if(info.equals("new")){
//            new art
            binding.nameText.setText("");
            binding.artistText.setText("");
            binding.yearText.setText("");
            binding.button.setVisibility(View.VISIBLE);

            binding.imageView.setImageResource(R.drawable.select);
        }else{
//            existing art
            int artId = intent.getIntExtra("artid",0);
            binding.button.setVisibility(View.INVISIBLE);
            binding.yearText.setFocusable(false);
            binding.artistText.setFocusable(false);
            binding.nameText.setFocusable(false);

            try{

                Cursor cursor = database.rawQuery("SELECT * FROM arts WHERE id = ?",new String[]{String.valueOf(artId)});

                int artNameIx = cursor.getColumnIndex("artname");
                int painterNameIx = cursor.getColumnIndex("artist");
                int yearIx = cursor.getColumnIndex("year");
                int imgIx = cursor.getColumnIndex("image");

                while (cursor.moveToNext()){
                    binding.nameText.setText(cursor.getString(artNameIx));
                    binding.artistText.setText(cursor.getString(painterNameIx));
                    binding.yearText.setText(cursor.getString(yearIx));

                    byte[] bytes = cursor.getBlob(imgIx);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                    binding.imageView.setImageBitmap(bitmap);
                }

                cursor.close();

            }catch (Exception e){
                e.printStackTrace();
            }
        }

    }

    public void save(View view){

        String name = String.valueOf(binding.nameText.getText());
        String artist = String.valueOf(binding.artistText.getText());
        int year = Integer.parseInt(String.valueOf(binding.yearText.getText()));

        Bitmap smallImg = compressImage(selectedImg,300);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        smallImg.compress(Bitmap.CompressFormat.PNG,50,outputStream);
        byte[] byteArray = outputStream.toByteArray();

        try{
            database.execSQL("CREATE TABLE IF NOT EXISTS arts(id INTEGER PRIMARY KEY, artname VARCHAR, artist VARCHAR, year INTEGER, image BLOB)");

           String sqlString = "INSERT INTO arts (artname, artist, year, image) VALUES(?, ?, ?, ?)";
           SQLiteStatement sqLiteStatement = database.compileStatement(sqlString);
           sqLiteStatement.bindString(1,name);
           sqLiteStatement.bindString(2,artist);
           sqLiteStatement.bindString(3,String.valueOf(year));
           sqLiteStatement.bindBlob(4,byteArray);

           sqLiteStatement.execute();

        }catch (Exception e){
            e.printStackTrace();
        }

        Intent intent = new Intent(DetailsActivity.this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);


    }

    public Bitmap compressImage(Bitmap selectedImg,int maximumSize){
        int width = selectedImg.getWidth();
        int height = selectedImg.getHeight();

        float bitmapRatio = (float) width / (float) height;

        if(bitmapRatio > 1){
//            landscape
            width = maximumSize;
            height = (int) (width/bitmapRatio);
        }else{
//            portrait
            height = maximumSize;
            width = (int) (height * bitmapRatio);
        }
return selectedImg.createScaledBitmap(selectedImg,width,height,true);
    }

    public void selectImage(View view){

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)){

    Snackbar.make(view,"Permission needed for gallery",Snackbar.LENGTH_INDEFINITE).setAction("Give permission", new View.OnClickListener() {
        @Override
        public void onClick(View view) {
//            request permission
            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);

        }
    }).show();

}else{
    //            request permission
    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
}

        }else{
//            gallery
            Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            activityResultLauncher.launch(intentToGallery);

        }

    }

    private void registerLauncher(){

        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
if(result.getResultCode() == RESULT_OK){
    Intent resultIntent = result.getData();
    if(resultIntent != null){
        Uri imageData = resultIntent.getData();
//        binding.imageView.setImageURI(imageData);

        try{
if(Build.VERSION.SDK_INT >= 28) {
    ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), imageData);
    selectedImg = ImageDecoder.decodeBitmap(source);
    binding.imageView.setImageBitmap(selectedImg);
}/*else{
    selectedImg = MediaStore.Images.Media.getBitmap(DetailsActivity.this,getContentResolver(),imageData);
    binding.imageView.setImageBitmap(selectedImg);
}*/

        }catch (Exception e){
           e.printStackTrace();
        }

    }
}
            }
        });

        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {
if(result){
//    permission granted
    Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
    activityResultLauncher.launch(intentToGallery);
}else{
//    permission denied
    Toast.makeText(DetailsActivity.this, "Permission required", Toast.LENGTH_SHORT).show();
}
            }
        });

    }

}