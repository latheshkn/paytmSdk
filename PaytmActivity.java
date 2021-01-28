package com.designurway.idlidosa.paytmallinonesdk;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


import com.designurway.idlidosa.R;
import com.designurway.idlidosa.activity.DashboardActivity;
import com.designurway.idlidosa.activity.PaymentSucessfulActivity;
import com.designurway.idlidosa.model.CustomerAddress;
import com.designurway.idlidosa.model.ErrorMessageModel;
import com.designurway.idlidosa.model.GetNotificationResponse;
import com.designurway.idlidosa.model.OrderStatusModel;
import com.designurway.idlidosa.retrofit.BaseClient;
import com.designurway.idlidosa.retrofit.RetrofitApi;
import com.designurway.idlidosa.utils.AndroidUtils;
import com.designurway.idlidosa.utils.PreferenceManager;
import com.paytm.pgsdk.PaytmOrder;
import com.paytm.pgsdk.PaytmPaymentTransactionCallback;
import com.paytm.pgsdk.TransactionManager;

import org.parceler.Parcels;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PaytmActivity extends AppCompatActivity {
    private String TAG = "MainActivity";

    private ProgressBar progressBar;
    private EditText txnAmount;
    private String midString = "uHykBm20360288053432", txnAmountString = "", orderIdString = "", txnTokenString = "";
    private Button btnPayNow;
    private Integer ActivityRequestCode = 2;
    TextView text_sample;
    TextView toolbar_title_tv;
    TextView total_amount;
    TextView text_subtotal;
    TextView txt_name;
    TextView txt_mobile;
    TextView text_address;
    ImageView add_symbol_imgv;
    Button btn_payment;
    String jsonString,amount,address;
    PreferenceManager preferenceManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paytm);
//        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        btn_payment = (Button) findViewById(R.id.btn_payment);
//        txnAmount = (EditText) findViewById(R.id.txnAmountId);

        toolbar_title_tv = (TextView) findViewById(R.id.toolbar_title_tv);
        total_amount = (TextView) findViewById(R.id.total_amount);
        text_subtotal = (TextView) findViewById(R.id.text_subtotal);
        txt_name = (TextView) findViewById(R.id.txt_name);
        txt_mobile = (TextView) findViewById(R.id.txt_mobile);
        text_address = (TextView) findViewById(R.id.text_address);
        add_symbol_imgv = (ImageView) findViewById(R.id.add_symbol_imgv);
        toolbar_title_tv.setText("Payment");
        add_symbol_imgv.setVisibility(View.GONE);
        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("ddMMyyyy");
        String date = df.format(c.getTime());
        Random rand = new Random();
        int min = 1000, max = 9999;
// nextInt as provided by Random is exclusive of the top value so you need to add 1
        int randomNum = rand.nextInt((max - min) + 1) + min;
        orderIdString = date + String.valueOf(randomNum);

        preferenceManager=new PreferenceManager();
        btn_payment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


//                ConfromOrder();
                txnAmountString = total_amount.getText().toString();

                String errors = "";
                if (orderIdString.equalsIgnoreCase("")) {
                    errors = "Enter valid Order ID here\n";
                    Toast.makeText(PaytmActivity.this, errors, Toast.LENGTH_SHORT).show();

                } else if (txnAmountString.equalsIgnoreCase("")) {
                    errors = "Enter valid Amount here\n";
                    Toast.makeText(PaytmActivity.this, errors, Toast.LENGTH_SHORT).show();

                } else {

                    getToken();
                }

            }
        });


        Parcelable parcelable = getIntent().getParcelableExtra("DATA_KEY");
        CustomerAddress customerAddress = Parcels.unwrap(parcelable);
        Log.d("Parceble", customerAddress.toString());
//        Bundle bundle=getIntent().getExtras();
        Log.d("Parceble",customerAddress.getMobile());
        txt_name.setText(customerAddress.getName());
        text_address.setText(customerAddress.getCityAddress());
        amount=customerAddress.getAmount();
        address=customerAddress.getCityAddress();
        txt_mobile.setText(customerAddress.getMobile());
//        total_amount.setText(customerAddress.getCityAddress());
        total_amount.setText(amount);
        text_subtotal.setText(amount);
//        if (bundle!=null){
//            String amount=bundle.getString("amount");
//            Toast.makeText(this, amount, Toast.LENGTH_SHORT).show();
//            total_amount.setText(amount);
//            text_subtotal.setText(amount);
//        }

    }

    private void getToken() {
        Log.e(TAG, " get token start");
//        progressBar.setVisibility(View.VISIBLE);
        ServiceWrapper serviceWrapper = new ServiceWrapper(null);
        Call<Token_Res> call = serviceWrapper.getTokenCall("12345", midString, orderIdString, txnAmountString);
        call.enqueue(new Callback<Token_Res>() {
            @Override
            public void onResponse(Call<Token_Res> call, Response<Token_Res> response) {
                Log.e(TAG, "respo " + response.isSuccessful());
//                progressBar.setVisibility(View.GONE);
                try {

                    if (response.isSuccessful() && response.body() != null) {
                        if (response.body().getBody().getTxnToken() != "") {
                            Log.e(TAG, " transaction token : " + response.body().getBody().getTxnToken());
                            startPaytmPayment(response.body().getBody().getTxnToken());
                        } else {
                            Log.e(TAG, " Token status false");
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, " error in Token Res " + e.toString());
                }
            }

            @Override
            public void onFailure(Call<Token_Res> call, Throwable t) {
//                progressBar.setVisibility(View.GONE);
                Log.e(TAG, " response error " + t.toString());
            }
        });

    }


    public void startPaytmPayment(String token) {

        txnTokenString = token;
        // for test mode use it
        // String host = "https://securegw-stage.paytm.in/";
        // for production mode use it
        String host = "https://securegw.paytm.in/";
        String orderDetails = "MID: " + midString + ", OrderId: " + orderIdString + ", TxnToken: " + txnTokenString
                + ", Amount: " + txnAmountString;
        //Log.e(TAG, "order details "+ orderDetails);

        String callBackUrl = host + "theia/paytmCallback?ORDER_ID=" + orderIdString;
        Log.e(TAG, " callback URL " + callBackUrl);
        PaytmOrder paytmOrder = new PaytmOrder(orderIdString, midString, txnTokenString, txnAmountString, callBackUrl);
        TransactionManager transactionManager = new TransactionManager(paytmOrder, new PaytmPaymentTransactionCallback() {
            @Override
            public void onTransactionResponse(Bundle bundle) {
                Log.e(TAG, "Response (onTransactionResponse) : " + bundle.toString());

                Toast.makeText(PaytmActivity.this,"Tansaction " + bundle.toString(), Toast.LENGTH_SHORT).show();

//                Log.d("bundleresponse","bundle"+bundle.getString("RESPMSG"));
            }

            @Override
            public void networkNotAvailable() {
                Log.e(TAG, "network not available ");
            }

            @Override
            public void onErrorProceed(String s) {
                Log.e(TAG, " onErrorProcess " + s.toString());
            }

            @Override
            public void clientAuthenticationFailed(String s) {
                Log.e(TAG, "Clientauth " + s);
            }

            @Override
            public void someUIErrorOccurred(String s) {
                Log.e(TAG, " UI error " + s);
            }

            @Override
            public void onErrorLoadingWebPage(int i, String s, String s1) {
                Log.e(TAG, " error loading web " + s + "--" + s1);
            }

            @Override
            public void onBackPressedCancelTransaction() {
                Log.e(TAG, "backPress ");
                Intent intent=new Intent(PaytmActivity.this, DashboardActivity.class);
                startActivity(intent);
              finish();
            }

            @Override
            public void onTransactionCancel(String s, Bundle bundle) {
                Log.e(TAG, " transaction cancel " + s);

                Intent intent=new Intent(PaytmActivity.this, DashboardActivity.class);
                startActivity(intent);
                finish();
            }


        });

        transactionManager.setShowPaymentUrl(host + "theia/api/v1/showPaymentPage");
        transactionManager.startTransaction(this, ActivityRequestCode);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        Log.e("RESPCODE", " result code " + resultCode);
        // -1 means successful  // 0 means failed
        // one error is - nativeSdkForMerchantMessage : networkError
        super.onActivityResult(requestCode, resultCode, data);

//        Toast.makeText(this, "payment success" + data.getStringExtra("response").toString(), Toast.LENGTH_SHORT).show();



        if (requestCode == ActivityRequestCode && data != null) {

            Bundle bundle = data.getExtras();
//            String status = bundle.get("RESPCODE").toString();

//            Toast.makeText(this, "RESPCODE "+ status, Toast.LENGTH_SHORT).show();
//            Log.e("RESPCODE", "RESPCODE "+status);
//            Intent intent=new Intent(PaytmActivity.this, DashboardActivity.class);



            if (bundle != null) {

//                Toast.makeText(this, bundle.toString(), Toast.LENGTH_SHORT).show();

                Log.e(TAG, "Bundle = "+ bundle.toString());

                for (String key : bundle.keySet()) {
//                    Toast.makeText(this, "Inside For loop", Toast.LENGTH_SHORT).show();

                    Log.e(TAG, "Payment Done" + " : " + (bundle.get(key)!= null ? bundle.get(key) : "NULL"));

//

                    jsonString = bundle.getString(key);

                    Log.d(TAG,"JsonString : "+jsonString);

                    Bundle bund = new Bundle();
                    bund.putString("dataa",jsonString);

                    if(jsonString.isEmpty() ){
//                        Toast.makeText(this, "JsonString is Empty", Toast.LENGTH_SHORT).show();
                    }else if(jsonString.equals("onBackPressedCancelTransaction")){
                        Toast.makeText(this, "Payment Cancelled", Toast.LENGTH_SHORT).show();
                        Intent intent=new Intent(PaytmActivity.this, DashboardActivity.class);
                        startActivity(intent);
                        finish();
                    }
                    else {
                        ConfromOrder();
                        postComboWonDetails();
                        Intent intent=new Intent(PaytmActivity.this, PaymentSucessfulActivity.class);
                        intent.putExtras(bund);
                        startActivity(intent);
                        finish();
                    }
                }

            }
        } else {
            Log.e(TAG, " payment failed");
        }
    }

    public void ConfromOrder() {
        Log.d("confirmorder", "method");
        String order_id = AndroidUtils.randomName(5);
        RetrofitApi api = BaseClient.getClient().create(RetrofitApi.class);

        Call<OrderStatusModel>
                call = api.postOrderDetails(PreferenceManager.getCustomerId(), order_id, amount, address);
        Toast.makeText(this, PreferenceManager.getCustomerId(), Toast.LENGTH_SHORT).show();
        call.enqueue(new Callback<OrderStatusModel>() {

            @Override
            public void onResponse(Call<OrderStatusModel> call, Response<OrderStatusModel> response) {
                Log.d("confirmorder", "success");
                if (response.isSuccessful()) {
                    Log.d("confirmorder", "success");
                    OrderStatusModel orderStatusModel = response.body();

                    getNotification(order_id);

                    Toast.makeText(PaytmActivity.this, orderStatusModel.getMessage(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(PaytmActivity.this, response.message(), Toast.LENGTH_SHORT).show();

                }
            }

            @Override
            public void onFailure(Call<OrderStatusModel> call, Throwable t) {
                Log.d(TAG, "onFailure" + t.getMessage());


            }
        });
    }


    public void getNotification(String order_id) {
        Log.d("confirmorder", "Nmethod");
        RetrofitApi api = BaseClient.getClient().create(RetrofitApi.class);
        Call<GetNotificationResponse> call = api.getNotification(order_id,  "new order");
        call.enqueue(new Callback<GetNotificationResponse>() {
            @Override
            public void onResponse(Call<GetNotificationResponse> call, Response<GetNotificationResponse> response) {
                if (response.isSuccessful()) {
                    Log.d("confirmorder", "Nresponsemethod");
                    Toast.makeText(PaytmActivity.this, "success", Toast.LENGTH_SHORT).show();
                } else {

                }
            }

            @Override
            public void onFailure(Call<GetNotificationResponse> call, Throwable t) {
                Toast.makeText(PaytmActivity.this, "Onfail", Toast.LENGTH_SHORT).show();

            }
        });
    }

    private void postComboWonDetails() {
        Log.d(TAG,"postCombo");
        String totalAmount=amount;
        RetrofitApi retrofitApi=BaseClient.getClient().create(RetrofitApi.class);
        Call<ErrorMessageModel> call=retrofitApi.updateComboWonDetails(AndroidUtils.randomName(5),
                PreferenceManager.getCustomerId(),
                totalAmount);
        call.enqueue(new Callback<ErrorMessageModel>() {
            @Override
            public void onResponse(Call<ErrorMessageModel> call, Response<ErrorMessageModel> response) {
                if (response.isSuccessful()){
//                    goToNext(new DashBoardFragment());
                }else{
//                    Toast.makeText(getContext(), response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ErrorMessageModel> call, Throwable t){
                Log.d(TAG,"onFailure"+t.getMessage());

            }
        });

    }
}