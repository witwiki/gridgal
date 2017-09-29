package com.example.gridgal;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by witwiki on 9/26/2017.
 */

public class ImageAdapter extends BaseAdapter {

    private static Context mContext;

    //  Initialise the String array of file paths of the MediaStore DATA request
//    private static int photoUrls[] = {
//            R.drawable.ic_airport_shuttle_black_24dp,
//            R.drawable.ic_airline_seat_flat_black_24dp,
//            R.drawable.ic_android_black_24dp,
//            R.drawable.ic_laptop_mac_black_24dp};

    private static String[] photoUrls = Utils.getImagePaths(mContext);

    public ImageAdapter(Context context) {
        this.mContext = context;
    }

    @Override
    public int getCount() {
        return photoUrls.length;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            final LayoutInflater layoutInflater = LayoutInflater.from(mContext);
            convertView = layoutInflater.inflate(R.layout.image_layout, null);
        }

        final ImageView imageView = convertView.findViewById(R.id.image_view);

        //  Load Bitmaps to GridView
        Utils.loadBitmap(imageView, photoUrls[position], 100, 100, mContext);

//        imageView.setOnClickListener(new View.OnClickListener(){
//            boolean isImageFitToScreen;
//            @Override
//            public void onClick(View view) {
//                isImageFitToScreen=true;
//                imageView.setLayoutParams(new
//                        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
//                        LinearLayout.LayoutParams.MATCH_PARENT));
//                imageView.setScaleType(ImageView.ScaleType.FIT_XY);
//            }
//        });

        return convertView;
    }

}

