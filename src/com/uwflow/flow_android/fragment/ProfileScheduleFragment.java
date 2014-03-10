package com.uwflow.flow_android.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import com.uwflow.flow_android.MainFlowActivity;
import com.uwflow.flow_android.R;
import com.uwflow.flow_android.constant.Constants;
import com.uwflow.flow_android.db_object.ScheduleCourses;
import com.uwflow.flow_android.db_object.ScheduleImage;
import com.uwflow.flow_android.network.FlowDatabaseLoader;
import com.uwflow.flow_android.network.FlowImageLoader;
import com.uwflow.flow_android.network.FlowImageLoaderCallback;

public class ProfileScheduleFragment extends Fragment implements View.OnClickListener {
    private String mScheduleImageURL;
    private RadioGroup mRadioGroup;
    private ImageView mImageSchedule;
    private Button mBtnExportCal;
    private Button mBtnShare;
    private LinearLayout mScheduleListLayout;
    private LinearLayout mScheduleWeekLayout;
    private View rootView;
    private ProfileScheduleReceiver profileScheduleReceiver;
    protected FlowImageLoaderCallback scheduleImageCallback;
    protected FlowImageLoader flowImageLoader;
    protected FlowDatabaseLoader flowDatabaseLoader;
    protected ScheduleImage scheduleImage;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.profile_schedule_layout, container, false);
        flowImageLoader = new FlowImageLoader(getActivity().getApplicationContext());
        flowDatabaseLoader = new FlowDatabaseLoader(getActivity().getApplicationContext(),
                ((MainFlowActivity) getActivity()).getHelper());
        scheduleImage = new ScheduleImage();
        mRadioGroup = (RadioGroup) rootView.findViewById(R.id.radio_group_view);
        mImageSchedule = (ImageView) rootView.findViewById(R.id.image_schedule);
        mImageSchedule.setOnClickListener(this);
        mImageSchedule.setScaleType(ImageView.ScaleType.FIT_XY);
        mBtnExportCal = (Button) rootView.findViewById(R.id.btn_export_calendar);
        mBtnShare = (Button) rootView.findViewById(R.id.btn_share);
        mScheduleListLayout = (LinearLayout) rootView.findViewById(R.id.list_layout);
        mScheduleWeekLayout = (LinearLayout) rootView.findViewById(R.id.week_layout);
        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.radio_list_view:
                        // List layout selected
                        mScheduleListLayout.setVisibility(View.VISIBLE);
                        mScheduleWeekLayout.setVisibility(View.GONE);
                        break;
                    case R.id.radio_week_view:
                        // Week layout selected
                        mScheduleListLayout.setVisibility(View.GONE);
                        mScheduleWeekLayout.setVisibility(View.VISIBLE);
                }
            }
        });

        mBtnShare.setEnabled(false);
        mBtnShare.setOnClickListener(this);
        mBtnExportCal.setOnClickListener(this);
        // call this before setting up the receiver
        populateData();
        profileScheduleReceiver = new ProfileScheduleReceiver();
        LocalBroadcastManager.getInstance(this.getActivity().getApplicationContext()).registerReceiver(profileScheduleReceiver,
                new IntentFilter(Constants.BroadcastActionId.UPDATE_PROFILE_USER_SCHEDULE));
        return rootView;

    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_export_calendar:
                // TODO: handle calendar export
                break;
            case R.id.btn_share:
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, mScheduleImageURL);
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Check out my schedule!");
                startActivity(Intent.createChooser(shareIntent, "Share schedule"));
                break;
            case R.id.image_schedule:
                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                Fragment fullScreenImageFragment = new FullScreenImageFragment();

                //TODO:Replace this with original bitmap once the scheduleBitmap is being loaded properly from URL
                Bitmap catImage = BitmapFactory.decodeResource(getActivity().getApplicationContext().getResources(),
                        R.drawable.kitty);
                Bundle bundle = new Bundle();
                if (scheduleImage != null)
                    bundle.putParcelable("ScheduleImage", scheduleImage.getImage());
                fullScreenImageFragment.setArguments(bundle);

                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.replace(R.id.content_frame, fullScreenImageFragment);
                transaction.addToBackStack(null);
                transaction.commit();
                break;
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        // Unregister since the activity is not visible
        LocalBroadcastManager.getInstance(this.getActivity().getApplicationContext()).unregisterReceiver(profileScheduleReceiver);
        super.onDestroyView();
    }


    protected void populateData() {
        final ProfileFragment profileFragment = ProfileFragment.convertFragment(getParentFragment());
        if (profileFragment == null)
            return;
        ScheduleCourses scheduleCourses = profileFragment.getUserSchedule();
        scheduleImage = flowDatabaseLoader.queryUserScheduleImage(profileFragment.getProfileID());
        if (scheduleImage != null) {
            mImageSchedule.setImageBitmap(scheduleImage.getImage());
            mBtnShare.setEnabled(true);
        } else {
            if (scheduleCourses != null && scheduleCourses.getScreenshotUrl() != null) {
                // assume the URL is valid and an image will be returned
                // TODO: change this conditional to 'if the image is successfully fetched'
                mScheduleImageURL = scheduleCourses.getScreenshotUrl();
                scheduleImageCallback = new FlowImageLoaderCallback() {
                    @Override
                    public void onImageLoaded(Bitmap bitmap) {
                        //add to database
                        scheduleImage = new ScheduleImage();
                        scheduleImage.setImage(bitmap);
                        scheduleImage.setId(profileFragment.getProfileID());
                        flowDatabaseLoader.updateOrCreateUserScheduleImage(scheduleImage);
                    }
                };
                flowImageLoader.loadImage(mScheduleImageURL, mImageSchedule, scheduleImageCallback);
                mBtnShare.setEnabled(true);
            }
        }
    }

    protected class ProfileScheduleReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            populateData();
        }
    }
}


