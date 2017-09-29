package com.example.gridgal;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.graphics.Matrix;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.provider.VoicemailContract;
import android.util.Log;
import android.support.v7.app.AppCompatActivity;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import static android.R.attr.angle;
import static android.R.attr.bitmap;
import static android.R.attr.data;
import static android.R.attr.orientation;
import static android.R.attr.readPermission;
import static android.R.attr.start;
import static android.R.attr.width;
import static java.lang.System.out;
import static java.security.AccessController.getContext;

/**
 * Created by witwiki on 9/26/2017.
 */

public class Utils {

    private static final String TAG = "Utils";
    private static final LruCache<String, Bitmap> bitmapLruCache = new LruCache<>(32);

    /**
     *  A private constructor is created so no one should ever create a {@link Utils} object.
     *  This class is only meant to hold static variables and methods, which can be accessed
     *  directly from the class name {@link Utils}(and an object instance of {@link Utils }
     *  is not needed).
     */
    private Utils() {
    }



    /**
     *  ********************************************************************************
     *  ********************************************************************************
     *
     *          LOADING LARGE BITMAPS EFFICIENTLY
     *
     *  ********************************************************************************
     *  ********************************************************************************
     */


    /**
     *  Creates and returns a Bitmap of the image at the given filepath,
     *  scaled down to fit the area the Bitmap will be displayed in
     *
     *  @param imagePath        file path of the image to be scaled
     *  @param rWidth           required width of Bitmap to be displayed
     *  @param rHeight          required height of Bitmap to be displayed
     *  @return                 a Bitmap scaled to cover the image view area
     */
    public static Bitmap decodeIncomingImage(String imagePath, int rWidth, int rHeight){

        //  Start Logging
        Log.v(TAG, "Starting the Image Scaling Process for " + imagePath);
        long startTime = android.os.SystemClock.uptimeMillis();

        //  Get the bounds of the image to be Bitmapped
        //  First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options opts = getImageBounds(imagePath);

        //  Calculate inSampleSize
        opts.inSampleSize = calculateInSampleSize(opts.outWidth,
                opts.outHeight, rWidth, rHeight);

        //  Decode bitmap with inSampleSize set
        opts.inJustDecodeBounds = false;
        Bitmap sampledBmp = BitmapFactory.decodeFile(imagePath, opts);

        //  Logging
        long sampledTime = android.os.SystemClock.uptimeMillis();
        Log.v(TAG, "Created a Sampled Bitmap for " + imagePath + ", in " +
                (sampledTime - startTime) + "ms");

        //  Checking Orientation of the image
        Bitmap orientBmp = setOrientation(sampledBmp, imagePath);
        long setOrientationTime = android.os.SystemClock.uptimeMillis();
        Log.v(TAG, "Set Orientation (if needed) for " + imagePath + ", in " +
                (setOrientationTime - sampledTime) + "ms");

        //  Creates a centered bitmap of the desired size.
        Bitmap finalBitmap = ThumbnailUtils.extractThumbnail(orientBmp, rWidth, rHeight);

        //  Finishing up logging activity for this method
        long endTime = android.os.SystemClock.uptimeMillis();
        Log.v(TAG, "Concluded Resizing Bitmap for " + imagePath + ", in " +
                (endTime - setOrientationTime) + "ms");
        Log.v(TAG, "Concluded Creating Bitmap for " + imagePath + ", in " +
                (endTime - startTime) + "ms");

        return finalBitmap;
    }


    /**
     *  Sets a new rotated Bitmap in accordance to the orientation specified in a source file
     *
     *  @param sampledBmp       the Bitmap to rotate
     *  @param imagePath        the file containing the Bitmap's source
     *
     *  @return                 a new Bitmap with the correct rotation
     */
    private static Bitmap setOrientation(Bitmap sampledBmp, String imagePath) {

        try {
            //  Get orientation information of the image file
            ExifInterface exif = new ExifInterface(imagePath);
            int currentOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            float rotateBmp;
            switch (currentOrientation) {
                case ExifInterface.ORIENTATION_NORMAL:
                    rotateBmp = 0;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotateBmp = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotateBmp = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotateBmp = 270;
                    break;
                default:
                    rotateBmp = 0;
                    break;
            }
            return rotateImage(sampledBmp, rotateBmp);
        } catch(IOException e) {
            Log.w(TAG, "Unable to open '" + imagePath + "' to read its current orientation");
            return sampledBmp;
        }
    }

    /**
     *  Returns a rotated version of a Bitmap
     *
     *  @param sampledBmp           the image to rotate
     *  @param rotateBmp            the amount to rotate the image by, in degrees
     *
     *  @return                     a new Bitmap with the newly set rotation
     */
    private static Bitmap rotateImage(Bitmap sampledBmp, float rotateBmp) {
        Matrix matrix = new Matrix();
        matrix.postRotate(rotateBmp);
        Bitmap rotatedImage = Bitmap.createBitmap(sampledBmp,
                0, 0, sampledBmp.getWidth(),
                sampledBmp.getHeight(), matrix, true);

        return rotatedImage;
    }


    /**
     *  Calculates the sample size of the image to scale it down to the given size on
     *  the {@link android.widget.GridView}
     *
     *  @param rawWidth         raw width of the image a.k.a outWidth
     *  @param rawHeight        raw height of the image a.k.a outHeight
     *  @param rWidth           required width of Bitmap to be displayed
     *  @param rHeight          required height of Bitmap to be displayed
     *
     *  @return                 Calculated Sample Size value based on target width and height
     */
    private static int calculateInSampleSize(int rawWidth, int rawHeight, int rWidth, int rHeight) {
        int inSampleSize = 1;

        if (rawHeight > rHeight || rawWidth > rWidth) {

            final int halfHeight = rawHeight / 2;
            final int halfWidth = rawWidth / 2;

            //  Calculates the largest inSampleSize value that is a power of 2 and
            // keeps both height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > rHeight
                    && (halfWidth / inSampleSize) > rWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }



    /**
     *  This method/getter retrieves the bounds of the Image to be decoded into a Bitmap
     *
     *  @param imagePath        The image's file path
     *  @return                 Returns a BitmapFactory.Options object where
     *                          outWidth and outHeight are the width and height
     *                          of the image
     */
    private static BitmapFactory.Options getImageBounds(String imagePath) {

        BitmapFactory.Options opts = new BitmapFactory.Options();
        //  Setting the inJustDecodeBounds property to true while decoding avoids
        //  memory allocation, returning null for the bitmap object
        opts.inJustDecodeBounds = true;
        //  Decoding method to create a Bitmap object
        BitmapFactory.decodeFile(imagePath, opts);

        return opts;
    }


    /**
     *  ********************************************************************************
     *  ********************************************************************************
     *
     *          MEDIASTORE REQUEST
     *
     *  ********************************************************************************
     *  ********************************************************************************
     */

    //  MediaStore Images Query
    public static String[] getImagePaths(Context context)
    {
        context = MainActivity.getContext();

        final String[] columns = {
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.ORIENTATION
        };
//        String selection = MediaStore.Images.Media.BUCKET_DISPLAY_NAME + " = ?";
//        String[] selectionArgs = new String[] {
//                "Camera"
//        };
        //  Order By most recent images
        final String orderBy = MediaStore.Images.Media.DATE_ADDED + " DESC";

        //  Stores all the images from the gallery in Cursor
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                columns,
                null,
                null,
                orderBy);

        //  Total number of images
        int count = cursor.getCount();

        //  Create an array to store path to all the images
        String[] arrPath = new String[count];
        String[] paths = new String[count];

        for (int i = 0; i < count; i++) {
            cursor.moveToPosition(i);
            int dataColumnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);

            //Store the path of the image
            arrPath[i]= cursor.getString(dataColumnIndex);
            paths[i]= cursor.getString(dataColumnIndex);
        }
        cursor.close();
        return paths;
    }


    /**
     *  ********************************************************************************
     *  ********************************************************************************
     *
     *          PROCESSING BITMAPS OFF THE UI THREAD
     *
     *  ********************************************************************************
     *  ********************************************************************************
     */

    /**
     *  This BitmapWorkerTask extends {@link AsyncTask}. It does Bitmap processing
     *  off the UI thread. This performs the task of loading a smaller version of the
     *  image into {@link ImageView}
     */                                                             // START BitmapWorkerTask()
    public static class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap>{
        private final WeakReference<ImageView> imageViewReference;
        private int aWidth, aHeight;
        private File aCachedImageFile;

        //  Constructor
        public BitmapWorkerTask(ImageView imageView, int nWidth, int nHeight, File cachedImageFile) {
            //  The WeakReference to the ImageView ensures that the AsyncTask does not prevent the
            //  {@link ImageView} and anything it references from being garbage collected.
            imageViewReference = new WeakReference<>(imageView);
            Log.v(TAG, "Creating BitmapWorkerTask Object");
            aWidth = nWidth;
            aHeight = nHeight;
            aCachedImageFile = cachedImageFile;
        }   //  END OF BitmapWorkerTask() constructor

        //  Decoding the Image in the Background Thread
        @Override
        protected Bitmap doInBackground(String... params) { // START doInBackground()
            try {
                String imageFilePath = params[0];
                Bitmap imageFile;

                if (aCachedImageFile != null && aCachedImageFile.exists()){
                    Log.v(TAG, "Loading a Cached Bitmap File " + aCachedImageFile.getPath());
                    long startTime = android.os.SystemClock.uptimeMillis();
                    imageFile = BitmapFactory.decodeFile(aCachedImageFile.getAbsolutePath());
                    long endTime = android.os.SystemClock.uptimeMillis();
                    Log.v(TAG, "Finished Loading Cached Bitmap File " + aCachedImageFile.getPath()
                            + ", in " + (endTime - startTime) + "ms");
                } else {
                    imageFile = Utils.decodeIncomingImage(imageFilePath, aWidth, aHeight);

                    //  Save this Thumbnail to disk, so no sampling is run on it again.
                    FileOutputStream outFile = null;
                    try {
                        outFile = new FileOutputStream(aCachedImageFile);
                        imageFile.compress(Bitmap.CompressFormat.JPEG, 97, outFile);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if (outFile != null) {
                                outFile.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                addToCache(new TrimmedBitmapData(imageFilePath, aWidth, aHeight), imageFile);
                return imageFile;
            }catch(Exception e) {
                Log.e(TAG, e.toString());
                return null;
            }
        }   //  END OF doInBackground() method

        //  Check if {@link ImageView} still exists once process is complete to insert Bitmap
        @Override
        protected void onPostExecute(Bitmap bitmap) {       // START onPostExecute()
            if (isCancelled()) {
                Log.d(TAG, "BitmapWorkerTask was Canceled");
                bitmap.recycle();
                bitmap = null;
            }

            if (imageViewReference != null && bitmap != null) {
                final ImageView imageView = imageViewReference.get();
                final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
                if (this == bitmapWorkerTask && imageView != null) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }   //  END OF onPostExecute()
    }   //  END OF BitmapWorkTask (Async Task)


    /**
     *  This methods adds an Image Thumbnail to the Cache
     *
     *  @param data         data about the Thumbnail such as name of the image file;
     *                      dimensions of the thumbnail
     *  @param bitmap       the resulting bitmap that is added to the cache
     */
    public static void addToCache(TrimmedBitmapData data, Bitmap bitmap) {
        if(bitmapLruCache.get(data.toString()) == null) {
            Log.v(TAG, "Adding bitmap to cache");
            bitmapLruCache.put(data.toString(), bitmap);
        }
    }

    /**
     *  Custom class that contains parameters used to generate a
     *  smaller Bitmap from a larger image.
     */
    public static class TrimmedBitmapData {
        public String imageFilePath;
        public int sWidth;
        public int sHeight;

        public TrimmedBitmapData(String filepath, int width, int height) {
            this.imageFilePath = filepath;
            this.sWidth = width;
            this.sHeight = height;
        }

        @Override
        public String toString() {
            return imageFilePath + " " + sWidth + " " + sHeight;
        }
    }

    /**
     *  ********************************************************************************
     *  ********************************************************************************
     *
     *          HANDLING CONCURRENCY
     *
     *  ********************************************************************************
     *  ********************************************************************************
     */


    /**
     *  This {@link Class} refers to Handling Concurrency in the app.
     *  It creates a dedicated {@link Drawable} subclass that is used to store a reference
     *  back to the {@link BitmapWorkerTask}. In this instance, a {@link BitmapDrawable}
     *  is used so a placeholder image can be displayed in the {@link ImageView} while
     *  the task is being completed.
     *
     *  Reference:
     *  https://stuff.mit.edu/afs/sipb/project/android/docs/training/displaying-bitmaps/process-bitmap.html
     *
     */
    static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

        public AsyncDrawable(Resources res, Bitmap bitmap, BitmapWorkerTask bitmapWorkerTask) {
            super(res, bitmap);
            bitmapWorkerTaskReference = new WeakReference<>(bitmapWorkerTask);
        }

        public BitmapWorkerTask getBitmapWorkerTask() {
            return bitmapWorkerTaskReference.get();
        }
    }

    /**
     *  This helper method is used to retrieve the tasks associated with a
     *  particular {@link ImageView}
     *
     *  @param imageView            The {@link ImageView} to retrieve
     *
     *  @return                     The BitmapWorkerTask associated with
     *                              {@link ImageView} else null
     */
    private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }


    /**
     *  This is the primary execution method that asynchronously
     *  loads a thumbnail of the given image into the given view
     *
     *  @param imageView            the ImageView to load the bitmap into
     *  @param imageFilePath        filepath of the (full-size) image to load
     *  @param width                desired width of the thumbnail
     *  @param height               desired height of the thumbnail
     *  @param context              a context, used to retrieve the path where we're
     *                              caching thumbnails on disk.
     */
    public static void loadBitmap(ImageView imageView, String imageFilePath, int width,
                                  int height, Context context) {
        TrimmedBitmapData data = new TrimmedBitmapData(imageFilePath, width, height);
        Log.v(TAG, "Loading Bitmap " + data.toString());

        Bitmap bitmap = bitmapLruCache.get(data.toString());
        if(bitmap != null) {
            Log.v(TAG, "The Bitmap already exists");
            imageView.setImageBitmap(bitmap);
        } else {
            Log.v(TAG, "Need to find or create a bitmap");
            /// Find (or create) the file containing the cached version of
            /// this image at the right size --> new File(File object, String object)
            File cachedImageFile = new File(context.getExternalCacheDir(),
                    data.toString());
            cachedImageFile.getParentFile().mkdir();

            Utils.BitmapWorkerTask task = new Utils.BitmapWorkerTask(imageView,
                    width, height, cachedImageFile);

            Bitmap placeholderBitmap = BitmapFactory.decodeResource(context.getResources(),
                    R.drawable.loading_thumbnail_2);

            final AsyncDrawable asyncDrawable = new AsyncDrawable(context.getResources(),
                    placeholderBitmap, task);
            imageView.setImageDrawable(asyncDrawable);
            task.execute(imageFilePath);
        }
    }

}
