package com.reactnativeizettlesample.izettle;

import android.content.Context;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class IZettlePackage implements ReactPackage {
    private Context mContext;
    private IZettleModule mModuleInstance;


    public IZettlePackage(Context activityContext) {
        mContext = activityContext;

    }

    @Override
    public List<NativeModule> createNativeModules(ReactApplicationContext reactContext) {
        List<NativeModule> modules = new ArrayList<>();

        mModuleInstance = new IZettleModule(reactContext, mContext);
        modules.add(mModuleInstance);
        return modules;
    }

    // Deprecated RN 0.47
    public List<Class<? extends JavaScriptModule>> createJSModules() {
        return Collections.emptyList();
    }

    @Override
    public List<ViewManager> createViewManagers(ReactApplicationContext reactContext) {
        return Arrays.<ViewManager>asList();
    }

}
