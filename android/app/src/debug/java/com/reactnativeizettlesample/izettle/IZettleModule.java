package com.reactnativeizettlesample;
import com.reactnativeizettlesample.R;
import com.reactnativeizettlesample.BuildConfig;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.core.content.res.ResourcesCompat;

import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

// IZETTLE
import com.izettle.payments.android.core.StateObserver;
import com.izettle.payments.android.payment.TransactionReference;
import com.izettle.payments.android.payment.refunds.CardPaymentPayload;
import com.izettle.payments.android.payment.refunds.RefundsManager;
import com.izettle.payments.android.payment.refunds.RetrieveCardPaymentFailureReason;
import com.izettle.payments.android.sdk.IZettleSDK;
import com.izettle.payments.android.sdk.User.AuthState;
import com.izettle.payments.android.sdk.User.AuthState.LoggedIn;
import com.izettle.payments.android.ui.payment.CardPaymentActivity;
import com.izettle.payments.android.ui.payment.CardPaymentResult;
import com.izettle.payments.android.ui.payment.FailureReason;
import com.izettle.payments.android.ui.readers.CardReadersActivity;
import com.izettle.payments.android.ui.refunds.RefundResult;
import com.izettle.payments.android.ui.refunds.RefundsActivity;
import com.izettle.payments.android.ui.SdkLifecycle;
import static com.izettle.payments.android.architecturecomponents.HelpersKt.toLiveData;

// Facebook
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Arguments; 

public class IZettleModule extends ReactContextBaseJavaModule implements ActivityEventListener {

    private Context mActivityContext;
    private ReactApplicationContext reactContext;
    private static AuthObserver observer;
    private static Boolean isLogged = false;
    private static AuthState loggedUser;

    private static final String TAG = "iZettleTag";
    private static final String AUTH_STATUS_CHANGE_EVENT = "iZettleUserAuthStatusChanged";

    private static int REQUEST_CODE_PAYMENT = 1001;
    private static int REQUEST_CODE_REFUND = 1002;

    private Callback paymentCallback;
    private Callback refundCallback;

    private String lastPaymentTraceId = "";

    public IZettleModule(ReactApplicationContext RCTContext, Context activityContext) {
        super(RCTContext);
        RCTContext.addActivityEventListener(this);
        mActivityContext = activityContext;
        reactContext = RCTContext;
        reactContext.addActivityEventListener(this);
    }

    @Override
    public String getName() {
        return "IZettleModule";
    }

    // React methods 
    @ReactMethod
    public void charge(Integer amount, Callback callback) {
        try {
            paymentCallback = callback;
            TransactionReference reference = getTransactionReference("PAYMENT_EXTRA_INFO");
            Intent intent = new CardPaymentActivity.IntentBuilder(mActivityContext)
            .amount(amount)
            .reference(reference)
            .enableTipping(false)
            .build();
                  
        getCurrentActivity().startActivityForResult(intent, REQUEST_CODE_PAYMENT);
          
        } catch (Exception e) {
          Log.e(TAG, "charge: " + e.getMessage());
          WritableMap error = Arguments.createMap();
          error.putString("reason", "Unexpected");
          error.putString("message", e.getMessage());
          paymentCallback.invoke(false, null, error);  
        }
      }

    @ReactMethod
    public void checkAuthStatus(Callback callback) {
        try {
          callback.invoke(IZettleModule.isLogged);
        } catch (Exception e) {
          Log.e(TAG, "checkAuthStatus: " + e.getMessage());
        }
    }      
    
    @ReactMethod
    public void getUserData(Callback callback) {
          try {
            if (IZettleModule.loggedUser instanceof LoggedIn)  {
              callback.invoke(buildUserDataMap());
              return;
            }
          } catch (Exception e) {
            Log.e(TAG, "getUserData: " + e.getMessage());
          }
          callback.invoke();
      }

    @ReactMethod
    public void initModule() {
        try {
          Log.i(TAG, "iZettle SDK is being initialized");
          String clientId = mActivityContext.getString(R.string.client_id);
          String scheme = mActivityContext.getString(R.string.redirect_url_scheme);
          String host = mActivityContext.getString(R.string.redirect_url_host);
          String redirectUrl = scheme + host;
          IZettleSDK.Instance.init(mActivityContext, clientId, redirectUrl);
          ProcessLifecycleOwner.get().getLifecycle().addObserver(new SdkLifecycle(IZettleSDK.Instance));

          observer = new AuthObserver();
          IZettleSDK.Instance.getUser().getState().addObserver(observer);
        } catch (Exception e) {
          Log.e(TAG, e.getMessage());
          Log.e(TAG, "initModule: " + e.getMessage());
        }
    }   

    @ReactMethod
    public void login() {
        try {
          IZettleSDK.Instance.getUser().login(getCurrentActivity(), ResourcesCompat.getColor(mActivityContext.getResources(), R.color.splascreen_background, null));
        } catch (Exception e) {
          Log.e(TAG, "login: " + e.getMessage());
        }
      }
      
    @ReactMethod
    public void logout() {
      try {
        IZettleSDK.Instance.getUser().logout();
      } catch (Exception e) {
        Log.e(TAG, "logout: " + e.getMessage());
      }
  }

    @ReactMethod
    public void refund(String internalTraceId, Callback callback) {
      try {
        refundCallback = callback;
        IZettleSDK.Instance.getRefundsManager().retrieveCardPayment(internalTraceId, new RefundCallback());
      } catch (Exception e) {
        Log.e(TAG, "refund: " + e.getMessage());          
      }
    }

    @ReactMethod
    public void settings() {
        try {
          getCurrentActivity().startActivity(CardReadersActivity.newIntent(getCurrentActivity()));
        } catch (Exception e) {
          Log.e(TAG, "settings: " + e.getMessage());
        }
    }

    // Utils
    private TransactionReference getTransactionReference(String extraType) {
      String internalTraceId = UUID.randomUUID().toString();
      lastPaymentTraceId = internalTraceId;
      return new TransactionReference.Builder(internalTraceId)
      .put(extraType, "Started from home screen")
      .build();
    }

    private WritableMap buildUserDataMap() throws Exception{
      WritableMap user = Arguments.createMap();
      WritableMap image = Arguments.createMap();
      if (IZettleModule.loggedUser instanceof LoggedIn)  {        
        user.putString("publicName", ((LoggedIn)IZettleModule.loggedUser).getInfo().getPublicName());
        image.putString("small", ((LoggedIn)IZettleModule.loggedUser).getInfo().getImageUrl().getSmall());
        image.putString("medium", ((LoggedIn)IZettleModule.loggedUser).getInfo().getImageUrl().getMedium());
        image.putString("large", ((LoggedIn)IZettleModule.loggedUser).getInfo().getImageUrl().getLarge());
        user.putMap("imageUrl", image);
        user.putString("userId", ((LoggedIn)IZettleModule.loggedUser).getInfo().getUserId());
        user.putString("organizationId", ((LoggedIn)IZettleModule.loggedUser).getInfo().getOrganizationId());
        user.putString("timeZone", ((LoggedIn)IZettleModule.loggedUser).getInfo().getTimeZone().getTimeZone().getID());
        user.putString("country", ((LoggedIn)IZettleModule.loggedUser).getInfo().getCountry().toString());
        user.putString("currency", ((LoggedIn)IZettleModule.loggedUser).getInfo().getCurrency().toString());
        return user;
      }
      throw new Exception("User is not logged");
    }

    private void sendEvent(
      String eventName,
      @Nullable WritableMap params
    ) {
      if(!reactContext.hasActiveCatalystInstance()) {
        return;
    }
      reactContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
      .emit(eventName, params);
    }

    private WritableMap buildPaymentResultData(CardPaymentResult result) {
      WritableMap resultData = Arguments.createMap();
      long gratuityAmount = 0L;
      try {
          gratuityAmount = ((CardPaymentResult.Completed)result).getPayload().getAmount();
          Double d = (double)gratuityAmount;
      }catch (Exception ignored){}
      
      resultData.putDouble("amount", ((CardPaymentResult.Completed)result).getPayload().getAmount());
      resultData.putDouble("gratuityAmount", (double)gratuityAmount);
      resultData.putString("cardType", ((CardPaymentResult.Completed)result).getPayload().getCardType());
      resultData.putString("cardPaymentEntryMode", ((CardPaymentResult.Completed)result).getPayload().getCardPaymentEntryMode());
      resultData.putString("tsi", ((CardPaymentResult.Completed)result).getPayload().getTsi());
      resultData.putString("tvr", ((CardPaymentResult.Completed)result).getPayload().getTvr());
      resultData.putString("applicationIdentifier", ((CardPaymentResult.Completed)result).getPayload().getApplicationIdentifier());
      resultData.putString("cardIssuingBank", ((CardPaymentResult.Completed)result).getPayload().getCardIssuingBank());
      resultData.putString("maskedPan", ((CardPaymentResult.Completed)result).getPayload().getMaskedPan());
      resultData.putString("applicationName", ((CardPaymentResult.Completed)result).getPayload().getApplicationName());
      resultData.putString("authorizationCode", ((CardPaymentResult.Completed)result).getPayload().getAuthorizationCode());
      resultData.putDouble("installmentAmount", ((CardPaymentResult.Completed)result).getPayload().getInstallmentAmount());
      resultData.putDouble("nrOfInstallments", ((CardPaymentResult.Completed)result).getPayload().getNrOfInstallments());
      resultData.putString("mxFiid", ((CardPaymentResult.Completed)result).getPayload().getMxFiid());
      resultData.putString("mxCardType", ((CardPaymentResult.Completed)result).getPayload().getMxCardType());
      resultData.putString("internalTraceId", lastPaymentTraceId);
      // TransactionReference reference = ((CardPaymentResult.Completed)result).getPayload().getReference();
      return resultData;

    }

    private WritableMap buildRefundResultData(RefundResult result) {
      WritableMap resultData = Arguments.createMap();
      
      resultData.putDouble("originalAmount", ((RefundResult.Completed) result).getPayload().getOriginalAmount());
      resultData.putDouble("refundedAmount", ((RefundResult.Completed) result).getPayload().getRefundedAmount());
      resultData.putString("cardType", ((RefundResult.Completed) result).getPayload().getCardType());
      resultData.putString("cardPaymentUUID", ((RefundResult.Completed) result).getPayload().getCardPaymentUUID());
      resultData.putString("maskedPan", ((RefundResult.Completed) result).getPayload().getMaskedPan());
      
      return resultData;
    }

    private void handlePaymentResult(Intent data) {
      WritableMap payment = null;
      Boolean status = false;
      WritableMap error = Arguments.createMap();
      String errorReason =  "";
      String errorMessage = "";

      try {
        if (data == null || paymentCallback == null) {
          throw new Exception("Unexpected");
        }

        CardPaymentResult result = data.getParcelableExtra(CardPaymentActivity.RESULT_EXTRA_PAYLOAD);     

        if (result instanceof CardPaymentResult.Completed) {
          payment = buildPaymentResultData(result);
          status = true;
        } else if (result instanceof CardPaymentResult.Canceled) {
          errorReason = "Canceled";
        } else if (result instanceof CardPaymentResult.Failed) {
          Log.i(TAG, ((CardPaymentResult.Failed)result).getReason().toString());
          
          if(((CardPaymentResult.Failed)result).getReason() instanceof FailureReason.NetworkError) {
            errorReason = "NetworkError";
          }
          else if(((CardPaymentResult.Failed)result).getReason() instanceof FailureReason.NotAuthorized) {
            errorReason = "NotAuthorized";
          }        
          else if(((CardPaymentResult.Failed)result).getReason() instanceof FailureReason.AboveMaximum) {
            errorReason = "AboveMaximum";
          }
          else if(((CardPaymentResult.Failed)result).getReason() instanceof FailureReason.BellowMinimum) {
            errorReason = "BellowMinimum";
          }
          else { //if(((CardPaymentResult.Failed)result).getReason() instanceof FailureReason.TechnicalError) {
            errorReason = "TechnicalError";
          }
        }
      } catch (Exception e) {
        errorReason = "Unexpected";
        errorMessage = e.getMessage();
      }
      error.putString("reason", errorReason);
      error.putString("message", errorMessage);
      paymentCallback.invoke(status, payment, error);
      paymentCallback = null;
    }

    private void handleRefundResult(Intent data) {
      WritableMap refund = null;
      Boolean status = false;
      WritableMap error = Arguments.createMap();
      String errorReason =  "";
      String errorMessage = "";

      try {
        if (data == null || refundCallback == null) {
          throw new Exception("Unexpected");
        }

        RefundResult result = data.getParcelableExtra(RefundsActivity.RESULT_EXTRA_PAYLOAD);

        if (result instanceof RefundResult.Completed) {
          refund = buildRefundResultData(result);
          status = true;
        } else if (result instanceof RefundResult.Canceled) {
          errorReason = "Canceled";
          errorMessage = "Estorno foi cancelado pelo vendedor";
        } else if (result instanceof RefundResult.Failed) {
          String reason = ((RefundResult.Failed)result).getReason().getDescription();
          errorReason = reason;
          errorMessage = "Erro ao realizar estorno";
        }
      } catch (Exception e) {
        errorReason = "Unexpected";
        errorMessage = e.getMessage();
      }
      error.putString("reason", errorReason);
      error.putString("message", errorMessage);
      refundCallback.invoke(status, error, refund);
      refundCallback = null;
    }
    
    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PAYMENT) {
          handlePaymentResult(data);
        }
        if (requestCode == REQUEST_CODE_REFUND) {
          handleRefundResult(data);
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
    }

    // Refund callback
    private class RefundCallback implements RefundsManager.Callback<CardPaymentPayload, RetrieveCardPaymentFailureReason> {
      @Override
      public void onFailure(RetrieveCardPaymentFailureReason reason) {
        WritableMap error = Arguments.createMap();
        error.putString("type", "Failed");
        error.putString("message", reason.getDescription());
        refundCallback.invoke(false, error);
      }

      @Override
      public void onSuccess(CardPaymentPayload payload) {
          TransactionReference reference = new TransactionReference.Builder(UUID.randomUUID().toString())
          .put("REFUND_EXTRA_INFO", "Started from home screen")
          .build();

          Intent intent = new RefundsActivity.IntentBuilder(getCurrentActivity())
                  .cardPayment(payload)
                  .reference(reference)
                  .build();

          getCurrentActivity().startActivityForResult(intent, REQUEST_CODE_REFUND);
      }
  }

    // Auth status observer  
    private class AuthObserver implements StateObserver<AuthState> {
      @Override
      public void onNext(AuthState authState) {
        WritableMap params = Arguments.createMap();
        try {
          IZettleModule.loggedUser = authState;

            if (authState instanceof  LoggedIn) {
              IZettleModule.isLogged = true;
              params.putBoolean("status", true);
              params.putMap("user", buildUserDataMap());
              sendEvent(AUTH_STATUS_CHANGE_EVENT, params);
            } else {
              IZettleModule.isLogged = false;
              params.putBoolean("status", false);
              sendEvent(AUTH_STATUS_CHANGE_EVENT, params);
              return;
            }
        } catch (Exception e) {
          params.putBoolean("status", false);
          params.putString("error", e.getMessage());
          sendEvent(AUTH_STATUS_CHANGE_EVENT, params);
        }
      }
    }
}
