package com.example.q.analogfoodie;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.share.Sharer;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.widget.ShareDialog;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import hu.don.easylut.EasyLUT;
import hu.don.easylut.filter.Filter;
import hu.don.easylut.filter.LutFilterFromResource;
import hu.don.easylut.lutimage.CoordinateToColor;
import hu.don.easylut.lutimage.LutAlignment;


public class MainActivity extends AppCompatActivity implements FilterViewAdapter.OnFilterSelected {

    private static final String TAG = MainActivity.class.getSimpleName();

    private ImageView foreground;
    private ImageView background;
    private ProgressBar pbBusy;
    private Bitmap srcBitmap;
    private RecyclerView rvFilters;
    private Bitmap  originalBitmap,filterBitmap;
    private Bitmap backoriginalBitmap,backFilteredBitmap;
    public static List<FilterSelection> effectItems = new LinkedList<>();
    private FilterSelection lastFilterSelection;
    private FilterSelection backLastFilterSelection;
    public static Bitmap userImageBitmap;
    public static Resources resources;
    private boolean fullRes = false;
    private String dir;
    private int curMaskFilter;
    private int curBackFilter;
    private Button saveBtn;
    private Button backBtn;
    private Button facebookBtn;
    private Button sendBtn;
    private String image_name="";
    private RequestQueue queue;
    ShareDialog shareDialog;
    private CallbackManager callbackManager;
    Rect outRect = new Rect();
    int[] location = new int[2];
    private FrameLayout frameLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        //facebook sdk
        FacebookSdk.sdkInitialize(this.getApplicationContext());
        FacebookSdk.setApplicationId(getResources().getString(R.string.facebook_app_id));
        callbackManager=CallbackManager.Factory.create();
        shareDialog = new ShareDialog(this);
        queue = Volley.newRequestQueue(this);
        setContentView(R.layout.activity_main);

        resources = getResources();

        frameLayout = findViewById(R.id.frameLayout);

        foreground = findViewById(R.id.iv_image2);
        background = findViewById(R.id.background);
        pbBusy = findViewById(R.id.pb_busy);
        saveBtn = (Button)findViewById(R.id.saveBtn);
        backBtn = (Button)findViewById(R.id.backBtn);
        facebookBtn = (Button)findViewById(R.id.facebookBtn);
        rvFilters = (RecyclerView)findViewById(R.id.rv_filters);

        //start filter from recommended filter
        curMaskFilter = 1;

        if(getIntent().getStringExtra("mode").equals("camera")) {
            File myFile = new File(getIntent().getStringExtra("filePath"));
            if(myFile.exists()){
                Bitmap myBitmap = BitmapFactory.decodeFile(myFile.getAbsolutePath());
               userImageBitmap = myBitmap;

                //이 사진을 디코딩해서 db에 넣어줘야대
                ByteArrayOutputStream bao = new ByteArrayOutputStream();
                myBitmap.compress(Bitmap.CompressFormat.JPEG, 90, bao);
                byte[] ba = bao.toByteArray();
                final String ba1 = Base64.encodeToString(ba, Base64.DEFAULT);



                //ba1을 fileString으로 volley 이용해서 post하기
                String url = "http://143.248.140.251:9780/api/photo/add";
                final StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                        new Response.Listener<String>()
                        {
                            @Override
                            public void onResponse(String response) {
                                // response
                                Log.d("Response", response);
                            }
                        },
                        new Response.ErrorListener()
                        {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                // error
                            }
                        }
                ) {
                    @Override
                    protected Map<String, String> getParams()
                    {
                        Map<String, String>  params = new HashMap<String, String>();
                        params.put("fileString", ba1);

                        return params;
                    }
                };
                queue.add(postRequest);
            }

        } else {
            Uri myURI = getIntent().getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), myURI);
                userImageBitmap = bitmap;
                ByteArrayOutputStream bao = new ByteArrayOutputStream();
                userImageBitmap.compress(Bitmap.CompressFormat.JPEG, 90, bao);
                byte[] ba = bao.toByteArray();
                final String ba1 = Base64.encodeToString(ba, Base64.DEFAULT);


                //ba1을 fileString으로 volley 이용해서 post하기
                String url = "http://143.248.140.251:9780/api/photo/add";
                final StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                        new Response.Listener<String>()
                        {
                            @Override
                            public void onResponse(String response) {
                                // response
                                Log.d("Response", response);
                            }
                        },
                        new Response.ErrorListener()
                        {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                // error
                            }
                        }
                ) {
                    @Override
                    protected Map<String, String> getParams()
                    {
                        Map<String, String>  params = new HashMap<String, String>();
                        params.put("fileString", ba1);

                        Log.e("ba1", ba1);

                        return params;
                    }
                };
                queue.add(postRequest);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //save bitmap
                frameLayout.setDrawingCacheEnabled(true);
                Bitmap image = frameLayout.getDrawingCache(true).copy(Bitmap.Config.ARGB_8888, false);
                frameLayout.destroyDrawingCache();
                saveImageToExternalStorage(image);

            }
        });

        backBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                finish();
            }
        });

        facebookBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Target target = new Target() {
                        @Override
                        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                            SharePhoto sharePhoto = new SharePhoto.Builder()
                                    .setBitmap(bitmap)
                                    .build();
                            if (ShareDialog.canShow(SharePhotoContent.class)) {
                                SharePhotoContent content = new SharePhotoContent.Builder()
                                        .addPhoto(sharePhoto)
                                        .build();
                                shareDialog.show(content);
                            }
                        }

                        @Override
                        public void onBitmapFailed(Drawable errorDrawable) {
                        }

                        @Override
                        public void onPrepareLoad(Drawable placeHolderDrawable) {
                        }
                    };

                    shareDialog.registerCallback(callbackManager, new FacebookCallback<Sharer.Result>() {
                        @Override
                        public void onSuccess(Sharer.Result result) {
                            Toast.makeText(getApplicationContext(), "Shared successfully.", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onCancel() {
                            Toast.makeText(getApplicationContext(), "Share cancelled.", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(FacebookException error) {
                            Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

                    frameLayout.setDrawingCacheEnabled(true);
                    Bitmap image = frameLayout.getDrawingCache(true).copy(Bitmap.Config.ARGB_8888, false);
                    frameLayout.destroyDrawingCache();

                    Uri bitmapuri = getImageUri(getApplicationContext(), image);
//
                    //We will fetch photo from link and convert to bitmap
                    Picasso.with(getBaseContext())
                            .load(bitmapuri)
                            .into(target);
                }

        });
        printKeyHash();
       /////////////////////////////////////////////////////////////////////////////////////////////////
       ////set filter
       ////////////////////////////////////////////////////////////////////////////////////////////////
        LutFilterFromResource.Builder squareBgr =
                EasyLUT.fromResourceId().withColorAxes(CoordinateToColor.Type.RGB_TO_ZYX).withResources(resources);
        LutFilterFromResource.Builder squareRgb =
                EasyLUT.fromResourceId().withColorAxes(CoordinateToColor.Type.RGB_TO_XYZ).withResources(resources);
        LutFilterFromResource.Builder squareBrg =
                EasyLUT.fromResourceId().withColorAxes(CoordinateToColor.Type.RGB_TO_YZX).withResources(resources);
        LutFilterFromResource.Builder haldRgb =
                EasyLUT.fromResourceId().withColorAxes(CoordinateToColor.Type.RGB_TO_XYZ).withResources(resources)
                        .withAlignmentMode(LutAlignment.Mode.HALD);


        addFilter("none", EasyLUT.createNonFilter());
        addFilter("sony1",squareRgb.withLutBitmapId(R.drawable.foreground_sony1).createFilter());
        addFilter("sony2",squareRgb.withLutBitmapId(R.drawable.foreground_sony2).createFilter());
        addFilter("red",squareRgb.withLutBitmapId(R.drawable.foreground_red).createFilter());
        addFilter("arri",squareRgb.withLutBitmapId(R.drawable.foreground_arri).createFilter());

        background.post(new Runnable() {
            @Override
            public void run() {
                setImage(userImageBitmap,background);
            }
        });

        //set image
        foreground.post(new Runnable() {
            @Override
            public void run() {
                setImage(userImageBitmap,foreground);
            }
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this,LinearLayoutManager.HORIZONTAL,false);
        rvFilters.setLayoutManager(layoutManager);
        rvFilters.setAdapter(new FilterViewAdapter(effectItems,this,this));
    }

     ////////////////////////////////////////////////////////////////////////////////////////////////
    //additional function
    /////////////////////////////////////////////////////////////////////////////////////////////////

    private void saveImageToExternalStorage(Bitmap finalBitmap) {
        String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
        File myDir = new File(root + "/saved_images_1");
        myDir.mkdirs();
        Random generator = new Random();
        int n = 10000;
        n = generator.nextInt(n);
        String fname = "Image-" + n + ".jpg";
        File file = new File(myDir, fname);
        if (file.exists())
            file.delete();
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }


        // Tell the media scanner about the new file so that it is
        // immediately available to the user.
        MediaScannerConnection.scanFile(this, new String[]{file.toString()}, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("ExternalStorage Scanned",  path + ":");
                        Log.i("ExternalStorage-> uri",  uri.toString());
                    }
                });
    }

     public Uri getImageUri(Context inContext, Bitmap inImage) {
         ByteArrayOutputStream bytes = new ByteArrayOutputStream();
         inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
         String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
         return Uri.parse(path);
     }

    private void printKeyHash() {
        try{
            PackageInfo info = getPackageManager().getPackageInfo("com.example.q.analogfoodie",
                    PackageManager.GET_SIGNATURES);
            for(Signature signature : info.signatures){
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KeyHash",Base64.encodeToString(md.digest(),Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }


    private void addFilter(String name, Filter filter) {
        effectItems.add(new FilterSelection(name.toUpperCase(Locale.ENGLISH), filter));
    }

    private void setBusy(boolean busy, boolean removeImage) {
        if (busy) {
            pbBusy.animate().alpha(1f).start();
            pbBusy.setVisibility(View.VISIBLE);
            foreground.animate().alpha(removeImage ? 0f : 0.5f).start();
//            tvName.animate().alpha(0f).start();
        } else {
            foreground.animate().alpha(1f).start();
//            tvName.animate().alpha(1f).start();
            pbBusy.animate().alpha(0f).start();
        }
    }

    private void setImage(Bitmap bitmap , final ImageView imageView) {
        new AsyncTask<Bitmap, Bitmap, Bitmap[]>() {

            long start;

            @Override
            protected void onPreExecute() {
                Log.d("***check","preexecute for setimage");
                setBusy(true, true);
                start = System.nanoTime();
            }

            @Override
            protected Bitmap[] doInBackground(Bitmap... bitmaps) {
                Log.d("***check","background job for setimage");

                if(imageView == foreground){
                    Bitmap bitmap1 = bitmaps[0];
                    if (bitmap1 == null) {
                        Log.d("***check","null bitmap");
                    }
                    Bitmap mask = BitmapFactory.decodeResource(getResources(),R.drawable.mask);
                    int intWidth = bitmap1.getWidth();
                    int intHeight = bitmap1.getHeight();

                    Bitmap resultMaskBitmap = Bitmap.createBitmap(intWidth,intHeight,Bitmap.Config.ARGB_8888);
                    Bitmap getMaskBitmap = Bitmap.createScaledBitmap(mask,intWidth,intHeight,true);
                    Canvas mCanvas = new Canvas(resultMaskBitmap);
                    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
                    mCanvas.drawBitmap(bitmap1, 0, 0, null);
                    mCanvas.drawBitmap(getMaskBitmap, 0, 0, paint);
                    paint.setXfermode(null);
                    bitmap1 =  resultMaskBitmap;
                    Bitmap bitmap2 = resultMaskBitmap;
                    publishProgress(bitmap1);
                    if (bitmap1 != null && !fullRes) {
                        int measuredHeight = foreground.getMeasuredHeight();
                        int measuredWidth = foreground.getMeasuredWidth();
                        if (measuredWidth != 0 && measuredHeight != 0 && (bitmap1.getHeight() >= measuredHeight || bitmap1.getWidth() >= measuredWidth)) {
                            float originalRatio = (float) bitmap1.getWidth() / (float) bitmap1.getHeight();
                            float measuredRatio = (float) measuredWidth / (float) measuredHeight;
                            if (originalRatio > measuredRatio) {
                                measuredWidth = (int) (measuredHeight * originalRatio);
                            } else {
                                measuredHeight = (int) (measuredWidth / originalRatio);
                            }
                            DisplayMetrics metrics = new DisplayMetrics();
                            getWindowManager().getDefaultDisplay().getMetrics(metrics);
                            bitmap2 = Bitmap.createScaledBitmap(bitmap1, measuredWidth, measuredHeight, true);
                        }
                    }
                    return new Bitmap[]{bitmap1, bitmap2};
                }
                else{
                    Bitmap bitmap1 = bitmaps[0];
                    if (bitmap1 == null) {
                        Log.d("***check","null bitmap");
                    }
                    Bitmap bitmap2 = bitmap1;
                    publishProgress(bitmap1);
                    if (bitmap1 != null && !fullRes) {
                        int measuredHeight = background.getMeasuredHeight();
                        int measuredWidth = background.getMeasuredWidth();
                        if (measuredWidth != 0 && measuredHeight != 0 && (bitmap1.getHeight() >= measuredHeight || bitmap1.getWidth() >= measuredWidth)) {
                            float originalRatio = (float) bitmap1.getWidth() / (float) bitmap1.getHeight();
                            float measuredRatio = (float) measuredWidth / (float) measuredHeight;
                            if (originalRatio > measuredRatio) {
                                measuredWidth = (int) (measuredHeight * originalRatio);
                            } else {
                                measuredHeight = (int) (measuredWidth / originalRatio);
                            }
                            DisplayMetrics metrics = new DisplayMetrics();
                            getWindowManager().getDefaultDisplay().getMetrics(metrics);
                            bitmap2 = Bitmap.createScaledBitmap(bitmap1, measuredWidth, measuredHeight, true);
                        }
                    }
                    return new Bitmap[]{bitmap1, bitmap2};
                }
            }

            @Override
            protected void onProgressUpdate(Bitmap... bitmaps) {
                Log.d("***check","progressupdate for setimage");
                if(imageView == foreground)
                    foreground.setImageBitmap(bitmaps[0]);
                else
                    background.setImageBitmap(bitmaps[0]);
            }

            @Override
            protected void onPostExecute(Bitmap[] bitmap) {
                Log.d("***check","postexecute for setimage");
                if(imageView == foreground){
                    originalBitmap = bitmap[0];
                    filterBitmap = bitmap[1];
                    foreground.setImageBitmap(filterBitmap);
                    setBusy(false, true);
                    foreground.setOnTouchListener(new OnSwipeTouchListener(MainActivity.this){

                        public void onSwipeRight(){
                            dir = "right";
                            curMaskFilter = (curMaskFilter+1)%effectItems.size();
                            lastFilterSelection = effectItems.get(curMaskFilter);
                            onFilterClicked(lastFilterSelection,foreground);
                        }
                        public void onSwipeLeft(){
                            dir = "left";
                            curMaskFilter = (curMaskFilter-1+effectItems.size())%effectItems.size();
                            lastFilterSelection = effectItems.get(curMaskFilter);
                            onFilterClicked(lastFilterSelection,foreground);
                        }
                    });
//                onFilterClicked(lastFilterSelection);
                    if (filterBitmap == null) {
                        Log.d(TAG, String.format("loading bitmap failed in %.2fms", (System.nanoTime() - start) / 1e6f));
                    } else {
                        Log.d(TAG, String.format("loaded %dx%d bitmap in %.2fms", filterBitmap.getWidth(), filterBitmap.getHeight(), (System.nanoTime() - start) / 1e6f));
                    }
                }
               else{
                    backoriginalBitmap = bitmap[0];
                    backFilteredBitmap = bitmap[1];
                    background.setImageBitmap(backFilteredBitmap);
                    setBusy(false,true);
                    onFilterClicked(backLastFilterSelection,background);
                    if (backFilteredBitmap == null) {
                        Log.d(TAG, String.format("loading bitmap failed in %.2fms", (System.nanoTime() - start) / 1e6f));
                    } else {
                        Log.d(TAG, String.format("loaded %dx%d bitmap in %.2fms", backFilteredBitmap.getWidth(), backFilteredBitmap.getHeight(), (System.nanoTime() - start) / 1e6f));
                    }
                }
            }
        }.execute(bitmap);
    }

    @Override
    public void onFilterClicked(FilterSelection filterSelection,final ImageView imageView) {
        final FilterSelection filter = filterSelection;
        final FilterSelection Backfilter= filterSelection;
        new AsyncTask<Void, Void, Bitmap>() {
            long start;
            @Override
            protected void onPreExecute() {
                Log.d("***check","preexecute for filterclicked");
                setBusy(true, false);
                start = System.nanoTime();
            }

            @Override
            protected Bitmap doInBackground(Void... voids) {
                if(imageView == foreground){
                    lastFilterSelection = filter;
                    Log.d("***check","background job for filterclicked");

//                Bitmap bitmap2 = resultMaskBitmap;
//                publishProgress(bitmap1);
                    if (lastFilterSelection == null || filterBitmap == null) {
                        return filterBitmap;
                    }
                    Log.i("***TEST", filterBitmap.toString());
                    Log.i("***TEST", lastFilterSelection.filter.toString());
                    try {
                        return lastFilterSelection.filter.apply(filterBitmap);
                    } catch (Exception e){
                        e.printStackTrace();
                        return null;
                    }
                }
                else{
                    backLastFilterSelection = Backfilter;
                    Log.d("***check","background is changed");
                    if(backLastFilterSelection == null || originalBitmap == null){
                        return backFilteredBitmap;
                    }
                    try{
                        return backLastFilterSelection.filter.apply(backFilteredBitmap);
                    }catch(Exception e){
                        e.printStackTrace();
                        return null;
                    }
                }
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {

                if(imageView == foreground) {
                    Bitmap mask = BitmapFactory.decodeResource(getResources(),R.drawable.mask);
                    int intWidth = bitmap.getWidth();
                    int intHeight = bitmap.getHeight();
                    Bitmap resultMaskBitmap = Bitmap.createBitmap(intWidth,intHeight,Bitmap.Config.ARGB_8888);
                    Bitmap getMaskBitmap = Bitmap.createScaledBitmap(mask,intWidth,intHeight,true);
                    Canvas mCanvas = new Canvas(resultMaskBitmap);
                    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
                    mCanvas.drawBitmap(bitmap, 0, 0, null);
                    mCanvas.drawBitmap(getMaskBitmap, 0, 0, paint);
                    paint.setXfermode(null);
                    bitmap=  resultMaskBitmap;
                   foreground.setImageBitmap(bitmap);
                }
                else background.setImageBitmap(bitmap);
                setBusy(false, false);
                if (bitmap == null) {
                    Log.d(TAG, String.format("processing bitmap failed in %.2fms", (System.nanoTime() - start) / 1e6f));
                } else {
                    Log.d(TAG, String.format("processed %dx%d bitmap in %.2fms", bitmap.getWidth(), bitmap.getHeight(), (System.nanoTime() - start) / 1e6f));
                }
            }
        }.execute();
    }

    public void Merge(View view,Bitmap backBitmap,Bitmap maskedBitmap) {
        Bitmap mergedImages = createSingleImageFromMultipleImages(backBitmap,maskedBitmap);
    }

    private Bitmap createSingleImageFromMultipleImages(Bitmap firstImage, Bitmap secondImage){
        Bitmap result = Bitmap.createBitmap(firstImage.getWidth(), firstImage.getHeight(), firstImage.getConfig());
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(firstImage, 0f, 0f, null);
        canvas.drawBitmap(secondImage, 10, 10, null);
        return result;
    }
}
