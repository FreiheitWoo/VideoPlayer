package com.example.administrator.videoplayer;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

public class MainActivity extends AppCompatActivity {

    private VideoView videoView;
    private LinearLayout controllerLayout;
    private ImageView play_controller_img, screen_img, volume_img;
    private TextView time_current_tv,time_total_tv;
    private SeekBar play_seek, volume_seek;
    public static final int UPDATE_UI = 1;
    private int screen_width, screen_height;
    private RelativeLayout videoLayout;
    private AudioManager mAudioManager;
    private boolean isFullScreen = false;
    private boolean isAdjust = false;
    private int threshold = 10;
    private float mBrightness;
    private boolean isSeekBar  = true;
    private ImageView operation_bg, operation_percent;
    private FrameLayout progress_layout;
    private Button changeUri;
    private EditText rtspUrl;
    private RadioButton radioStream,radioFile;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAudioManager = (AudioManager)getSystemService(AUDIO_SERVICE);
        initUI();
        setPlayerEvent();

        //String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Movies/testCar.mp4";
        //本地视频播放
        //videoView.setVideoPath(path);

        changeUri.setOnClickListener(new Button.OnClickListener(){
            public void onClick(View v) {
                if (radioStream.isChecked()) {
                    PlayRtspStream(rtspUrl.getEditableText().toString());
                }
                else if (radioFile.isChecked()){
                    PlayLocalFile(rtspUrl.getEditableText().toString());
                }
            }
        });
        //网络视频播放

        UIHandler.sendEmptyMessage(UPDATE_UI);

        //MediaController控制视频播放
        //MediaController controller = new MediaController(this);

        //设置VideoView与MediaController建立关联
        //videoView.setMediaController(controller);
        //controller.setMediaPlayer(videoView);
    }

    //play rtsp stream
    private void PlayRtspStream(String rtspUrl){
        videoView.setVideoURI(Uri.parse(rtspUrl));
        videoView.start();
    }

    //play local file
    private void PlayLocalFile(String filePath){
        videoView.setVideoPath(Environment.getExternalStorageDirectory() + "/" + filePath);
        videoView.start();
    }

    private void updateTextViewWithTimeFormat(TextView textView, int millisecond){
        int second = millisecond / 1000;
        int hh = second / 3600;
        int mm = (second % 3600) / 60;
        int ss = second % 60;
        String str = null;
        if(hh != 0){
            str = String.format("%02d:%02d:%02d", hh, mm, ss);
        }
        else {
            str = String.format("%02d:%02d", mm, ss);
        }

        textView.setText(str);
    }

    private Handler UIHandler = new Handler() {
        @Override
        public void handleMessage(Message msg){
            super.handleMessage(msg);
            if(msg.what == UPDATE_UI){
                //获取当前播放时间
                int currentPosition = videoView.getCurrentPosition();
                //获取视频总时间
                int totalduration = videoView.getDuration();

                //格式化视频时间
                updateTextViewWithTimeFormat(time_current_tv, currentPosition);
                updateTextViewWithTimeFormat(time_total_tv, totalduration);
                play_seek.setMax(totalduration);
                play_seek.setProgress(currentPosition);
                UIHandler.sendEmptyMessageDelayed(UPDATE_UI, 500);
            }

        }
    };

    @Override
    protected void onPause(){
        super.onPause();
        UIHandler.removeMessages(UPDATE_UI);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void setPlayerEvent(){
        play_controller_img.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if(videoView.isPlaying()){
                    play_controller_img.setImageResource(R.drawable.play_btn_style);
                    //暂停播放
                    videoView.pause();
                    UIHandler.removeMessages(UPDATE_UI);
                }

                else
                {
                    play_controller_img.setImageResource(R.drawable.pause_btn_style);
                    //继续播放
                    videoView.start();
                    UIHandler.sendEmptyMessage(UPDATE_UI);
                }
            }
        });
        play_seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                updateTextViewWithTimeFormat(time_current_tv, progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                UIHandler.removeMessages(UPDATE_UI);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //令视频播放进度遵循SeekBar停止拖动这一刻的进度
                int progress = seekBar.getProgress();
                videoView.seekTo(progress);
                UIHandler.sendEmptyMessage(UPDATE_UI);
            }
        });

        volume_seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        //横竖屏切换
        screen_img.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if(isFullScreen){
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }
                else
                {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                }
            }
        });


        /**
         * 控制VideoViewd的手势事件,左边控制亮度，右边音量增减
         */
        videoView.setOnTouchListener(new View.OnTouchListener() {
            private long firstClick;
            private long lastClick;
            private int count = 0;
            private float lastX = 0,lastY = 0;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        // 如果第二次点击 距离第一次点击时间过长 那么将第二次点击看为第一次点击
                        if (firstClick != 0 && System.currentTimeMillis() - firstClick > 300) {
                            count = 0;
                        }
                        count++;
                        if (count == 1) {
                            firstClick = System.currentTimeMillis();
                            if (!isSeekBar) {
                                play_seek.setVisibility(View.GONE);
                                play_controller_img.setVisibility(View.GONE);
                                volume_img.setVisibility(View.GONE);
                                volume_seek.setVisibility(View.GONE);
                                screen_img.setVisibility(View.GONE);
                                rtspUrl.setVisibility(View.GONE);
                                radioFile.setVisibility(View.GONE);
                                radioStream.setVisibility(View.GONE);
                                changeUri.setVisibility(View.GONE);
                                isSeekBar = true;
                            } else {
                                play_seek.setVisibility(View.VISIBLE);
                                play_controller_img.setVisibility(View.VISIBLE);
                                if (isFullScreen) {
                                    volume_img.setVisibility(View.VISIBLE);
                                    volume_seek.setVisibility(View.VISIBLE);
                                }
                                rtspUrl.setVisibility(View.VISIBLE);
                                radioFile.setVisibility(View.VISIBLE);
                                radioStream.setVisibility(View.VISIBLE);
                                changeUri.setVisibility(View.VISIBLE);
                                screen_img.setVisibility(View.VISIBLE);
                                isSeekBar = false;
                            }
                        } else if (count == 2) {
                            lastClick = System.currentTimeMillis();
                            // 两次点击小于300ms 也就是连续点击
                            if (lastClick - firstClick < 300) {// 判断是否是执行了双击事件
                                if (videoView.isPlaying()) {
                                    play_controller_img.setImageResource(R.drawable.play_btn_style);
                                    videoView.pause();
                                } else {
                                    play_controller_img.setImageResource(R.drawable.pause_btn_style);
                                    videoView.start();
                                }
                            }
                        }  //手指落下屏幕
                        lastX = event.getX();
                        lastY = event.getY();
                        break;
                    }
                    case MotionEvent.ACTION_MOVE: {//手指在屏幕上移动

                        float x = event.getX();
                        float y = event.getY();
                        float deltaY = y - lastY;

                        if (x < screen_width / 2) {
                            //调节亮度
                            if (deltaY > 0) {
                                //降低亮度
                                changeBrightness(-deltaY);
                            } else{
                                //提高亮度
                                changeBrightness(-deltaY);
                            }

                        } else {
                            if (deltaY > 0) {
                                //减小声音
                                changeVolume(-deltaY);
                            } else {
                                //增大声音
                                changeVolume(-deltaY);
                            }
                        }

                        lastX = x;
                        lastY = y;

                }

                    break;
                    case MotionEvent.ACTION_UP: {
                        progress_layout.setVisibility(View.GONE);//手指离开
                        break;
                    }
                }
                return true;
            }
        });

    }

    //更改音量
    private void changeVolume(float deltaY){
        int max = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int current = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int index = (int)(deltaY / screen_height * max * 10);
        int volume = Math.max(current+index, 0);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,volume,0);
        if (progress_layout.getVisibility() == View.GONE){
            progress_layout.setVisibility(View.VISIBLE);
        }
        operation_bg.setImageResource(R.mipmap.video_volume_bg);
        ViewGroup.LayoutParams layoutParams = operation_percent.getLayoutParams();
        layoutParams.width = (int) (PixelUtil.dp2px(94) * (float)volume / max);
        operation_percent.setLayoutParams(layoutParams);
        volume_seek.setProgress(volume);
    }
    //更改亮度
    private void changeBrightness(float deltaY){
        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        mBrightness = attributes.screenBrightness;
        float index = deltaY / screen_height * 5;
        mBrightness += index;
        if(mBrightness > 1.0f){
            mBrightness = 1.0f;
        }
        if(mBrightness < 0.0f){
            mBrightness = 0.01f;
        }
        attributes.screenBrightness = mBrightness;
        if (progress_layout.getVisibility() == View.GONE){
            progress_layout.setVisibility(View.VISIBLE);
        }
        operation_bg.setImageResource(R.mipmap.video_brightness_bg);
        ViewGroup.LayoutParams layoutParams = operation_percent.getLayoutParams();
        layoutParams.width = (int) (PixelUtil.dp2px(94) * mBrightness);
        operation_percent.setLayoutParams(layoutParams);
        getWindow().setAttributes(attributes);
    }

    //初始化UI
    private void initUI(){
        PixelUtil.initContext(this);
        videoView = findViewById(R.id.videoView);
        controllerLayout = findViewById(R.id.controllerbar_layout);
        play_controller_img = findViewById(R.id.pause_img);
        screen_img = findViewById(R.id.screen_img);
        volume_img = findViewById(R.id.volume_img);
        time_current_tv = findViewById(R.id.time_current_tv);
        time_total_tv = findViewById(R.id.time_total_tv);
        play_seek = findViewById(R.id.play_seek);
        volume_seek = findViewById(R.id.volume_seek);
        videoLayout = findViewById(R.id.videoLayout);
        screen_width = getResources().getDisplayMetrics().widthPixels;
        screen_height = getResources().getDisplayMetrics().heightPixels;

        int streamMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int streamVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        volume_seek.setMax(streamMaxVolume);
        volume_seek.setProgress(streamVolume);
        operation_bg = findViewById(R.id.operation_bg);
        operation_percent = findViewById(R.id.operation_percent);
        progress_layout = findViewById(R.id.progress_layout);
        changeUri = findViewById(R.id.start_play);
        rtspUrl = findViewById(R.id.edit_url);
        changeUri = findViewById(R.id.start_play);
        radioStream = findViewById(R.id.radioButtonStream);
        radioFile = findViewById(R.id.radioButtonFile);

    }

    private void setVideoViewScale(int width, int height){
        ViewGroup.LayoutParams layoutParams = videoView.getLayoutParams();
        layoutParams.width = width;
        layoutParams.height = height;
        videoView.setLayoutParams(layoutParams);

        ViewGroup.LayoutParams layoutParams1 = videoLayout.getLayoutParams();
        layoutParams1.width = width;
        layoutParams1.height = height;
        videoLayout.setLayoutParams(layoutParams1);

    }




    //监听屏幕方向改变
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        //当屏幕方向为横屏时
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            setVideoViewScale(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            volume_img.setVisibility(View.VISIBLE);
            volume_seek.setVisibility(View.VISIBLE);
            isFullScreen = true;

            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        //当屏幕方向为竖屏时
        else{
            setVideoViewScale(ViewGroup.LayoutParams.MATCH_PARENT, PixelUtil.dp2px(290));
            volume_img.setVisibility(View.GONE);
            volume_seek.setVisibility(View.GONE);
            isFullScreen = false;
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        }
    }
}
