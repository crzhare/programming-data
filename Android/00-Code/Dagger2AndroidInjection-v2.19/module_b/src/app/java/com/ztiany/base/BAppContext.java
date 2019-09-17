package com.ztiany.base;

import android.app.Activity;
import android.view.View;

import com.ztiany.base.di.HasViewInjector;
import com.ztiany.base.di.ViewComponentBuilder;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;

/**
 * @author Ztiany
 *         Email: ztiany3@gmail.com
 *         Date : 2017-09-19 15:33
 */
public class BAppContext extends AppContext implements HasViewInjector {

    @Inject
    DispatchingAndroidInjector<Activity> dispatchingActivityInjector;

    @Inject
    Map<Class<? extends View>, Provider<ViewComponentBuilder>> viewBuilders;

    @Override
    public void onCreate() {
        super.onCreate();
        DaggerAppComponent.create().inject(this);
    }

    @Override
    public AndroidInjector<Activity> activityInjector() {
        return dispatchingActivityInjector;
    }

    @Override
    public Map<Class<? extends View>, Provider<ViewComponentBuilder>> viewInjectors() {
        return viewBuilders;
    }

}
