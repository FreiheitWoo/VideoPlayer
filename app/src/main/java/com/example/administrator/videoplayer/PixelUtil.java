package com.example.administrator.videoplayer;


import android.content.Context;
import android.content.res.Resources;

//像素转换
public class PixelUtil {
    public static void initContext(Context context) { mContext = context; }
    private static Context mContext;

    public static int dp2px(float value){
        final float scale = mContext.getResources().getDisplayMetrics().densityDpi;
        return (int) (value * (scale / 160) + 0.5f);
    }

    public static int dp2px(float value, Context context){
        final float scale = context.getResources().getDisplayMetrics().densityDpi;
        return (int) (value * (scale / 160) + 0.5f);
    }

    public static int px2dp(float value){
        final float scale = mContext.getResources().getDisplayMetrics().densityDpi;
        return (int) (value * 160 / scale + 0.5f);
    }

    public static int px2dp(float value, Context context){
        final float scale = context.getResources().getDisplayMetrics().densityDpi;
        return (int) (value * 160 / scale + 0.5f);
    }

    public static int sp2px(float value) {
        Resources r;
        if (mContext == null)
            r = Resources.getSystem();
        else
            r = mContext.getResources();
        float spvalue = value * r.getDisplayMetrics().scaledDensity;
        return (int) (spvalue + 0.5f);
    }

    public static int sp2px(float value, Context context) {
        Resources r;
        if (context == null)
            r = Resources.getSystem();
        else
            r = context.getResources();
        float spvalue = value * r.getDisplayMetrics().scaledDensity;
        return (int) (spvalue + 0.5f);
    }

    public  static int px2sp(float value){
        final float scale = mContext.getResources().getDisplayMetrics().densityDpi;
        return (int) (value / scale + 0.5f);
    }

    public static int px2sp(float value, Context context){
        final float scale = context.getResources().getDisplayMetrics().densityDpi;
        return (int) (value / scale + 0.5f);
    }

}
