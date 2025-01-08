package com.owlvation.project.genedu.Dashboard.HomeHelper;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.google.firebase.FirebaseApp;

public class BaseApp extends Application {
    private int activeActivities = 0;
    private boolean isTrackingStarted = false;

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle bundle) {
            }

            @Override
            public void onActivityStarted(Activity activity) {
                activeActivities++;
                if (activeActivities == 1) {
                    if (isTrackingStarted) {
                        StudySessionManager.getInstance(BaseApp.this).resumeSession();
                    } else {
                        StudySessionManager.getInstance(BaseApp.this).startSession();
                        isTrackingStarted = true;
                    }
                }
            }

            @Override
            public void onActivityResumed(Activity activity) {
            }

            @Override
            public void onActivityPaused(Activity activity) {
            }

            @Override
            public void onActivityStopped(Activity activity) {
                activeActivities--;
                if (activeActivities == 0) {
                    StudySessionManager.getInstance(BaseApp.this).pauseSession();
                }
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                if (activity.isFinishing() && activeActivities == 0) {
                    StudySessionManager.getInstance(BaseApp.this).endSession();
                    isTrackingStarted = false;
                }
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
            }
        });
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        StudySessionManager.getInstance(this).cleanup();
    }
}