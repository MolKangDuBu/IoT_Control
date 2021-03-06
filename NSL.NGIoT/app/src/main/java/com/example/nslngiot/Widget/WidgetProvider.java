package com.example.nslngiot.Widget;

import android.app.Activity;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.example.nslngiot.LoginMemberActivity;
import com.example.nslngiot.Network_Utill.VolleyQueueSingleTon;
import com.example.nslngiot.R;
import com.example.nslngiot.Security_Utill.AES;
import com.example.nslngiot.Security_Utill.KEYSTORE;
import com.example.nslngiot.Security_Utill.RSA;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class WidgetProvider extends AppWidgetProvider {

    private final static String reflash_Flag = "com.example.nslngiot.imgbtn_widget_refresh";
    private SharedPreferences Preferences; // 앱 XML 저장 및 읽기 전용
    private String calenderTitle; // 연구실 일정 정보
    private boolean lab_Person; // 재실 여부
    private boolean lab_Coffe; // 커피 잔여량
    private boolean lab_A4; // a4 잔여량
    private long mNow;
    private Date mDate;
    private SimpleDateFormat mFormat;

    /*브로드캐스트를 수신할때, Override된 콜백 메소드가 호출되기 직전에 호출됨*/
    @Override
    public void onReceive(final Context context, Intent intent) {
        String action = intent.getAction(); // 새로고침 시, 액션 이벤트 리시브

        // 날짜 셋팅
        mNow = System.currentTimeMillis();
        mDate = new Date(mNow);
        mFormat = new SimpleDateFormat("YYYY년 MM월 dd일");

        // 클릭 이벤트 셋팅
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_member_status);
        Intent setup_intent = new Intent(context, WidgetProvider.class);
        setup_intent.setAction(reflash_Flag); // 새로고침 등록
        PendingIntent pendingIntent_reflash = PendingIntent.getBroadcast(context, 0, setup_intent, 0);
        views.setOnClickPendingIntent(R.id.imgbtn_widget_refresh, pendingIntent_reflash);


        // 새로 고침 이벤트 체크 진행
        if (action != null && action.equals(reflash_Flag.trim())) {

            // 일정 정보 누를 시, Activity 실행
            Intent intentCalendar = new Intent(context, LoginMemberActivity.class);
            // 로그인 멤버 액티비티로 보내는 암호 키생성 신호
            intentCalendar.putExtra("signal", "keystore");
            intentCalendar.setData(Uri.parse(intentCalendar.toUri(Intent.URI_INTENT_SCHEME)));
            PendingIntent mainActivityPendingIntent = PendingIntent.getActivity(context, 0, intentCalendar, 0);
            views.setOnClickPendingIntent(R.id.tv_widget_calendar, mainActivityPendingIntent);

            member_calendar_Request(context); // 연구실 일정정보 호출
        } // 지정 주기마다 위젯 업데이트 시
        else if (action != null && action.equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE)) {

            // 일정 정보 누를 시, Activity 실행
            Intent intentCalendar = new Intent(context, LoginMemberActivity.class);
            // 로그인 멤버 액티비티로 보내는 암호 키생성 신호
            intentCalendar.putExtra("signal", "keystore");
            intentCalendar.setData(Uri.parse(intentCalendar.toUri(Intent.URI_INTENT_SCHEME)));
            PendingIntent mainActivityPendingIntent = PendingIntent.getActivity(context, 0, intentCalendar, 0);
            views.setOnClickPendingIntent(R.id.tv_widget_calendar, mainActivityPendingIntent);

            member_calendar_Request(context); // 연구실 일정정보 호출

            // 네트워크 진행 완료 시, 받아온 데이터가 지워지는 문제 발생, XML에 저장.
            // 사용 시, 다시 XML 데이터 불러오는 안전한 형태로 진행
            Preferences = context.getSharedPreferences("LAB_WIDGET", Activity.MODE_PRIVATE);
            calenderTitle = Preferences.getString("CALENDER", "등록된 일정이 없습니다.");
            lab_Person = Preferences.getBoolean("PERSON", false);
            lab_Coffe = Preferences.getBoolean("COFFE", false);
            lab_A4 = Preferences.getBoolean("A4", false);

            views.setTextViewText(R.id.tv_widget_calendar, " 연구실 대표 일정: " + calenderTitle + "\n 상세 정보는 일정을 눌러 확인하세요.");
            if (lab_Person)
                views.setImageViewResource(R.id.img_person, R.drawable.people_exist);
            else
                views.setImageViewResource(R.id.img_person, R.drawable.people_nonexist);
            if (lab_Coffe)
                views.setImageViewResource(R.id.img_coffee, R.drawable.coffee_exist);
            else
                views.setImageViewResource(R.id.img_coffee, R.drawable.coffee_nonexist);
            if (lab_A4)
                views.setImageViewResource(R.id.img_a4, R.drawable.a4_exist);
            else
                views.setImageViewResource(R.id.img_a4, R.drawable.a4_nonexist);
        } else
            views.setTextViewText(R.id.tv_widget_calendar, " 새로 고침을 눌러주세요.");


        AppWidgetManager.getInstance(context).updateAppWidget(new ComponentName(context, WidgetProvider.class), views); // 위젯 업데이트
        super.onReceive(context, intent);
    }

    @Override
    public void onEnabled(Context context) {
        /*
         * 위젯이 처음 생성될때 호출됨
         * 동일한 위젯이 생성되도 최초 생성때만 호출됨
         */
        KEYSTORE keystore = new KEYSTORE();
        keystore.keyStore_init(context); // 최초 1회 KeyStore에 저장할 RSA키 생성
        AES.aesKeyGen();
        AES.secretKEY = KEYSTORE.keyStore_Encryption(AES.secretKEY);
        // 생성된 개인키/대칭키 keystore의 비대칭암호로 암호화하여 static 메모리 적재

        // 기존 연구실 '일정 정보'&'상태 정보' XML 기본 값 저장
        Preferences = context.getSharedPreferences("LAB_WIDGET", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = Preferences.edit();
        editor.putBoolean("PERSON", false);
        editor.putBoolean("COFFE", false);
        editor.putBoolean("A4", false);
        editor.putString("CALENDER", "기본셋팅");
        editor.apply();
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        /*
         * 위젯을 갱신할때 호출됨
         * 주의 : Configure Activity를 정의했을때는 위젯 등록시 처음 한번은 호출이 되지 않습니다
         */
    }

    @Override
    public void onDisabled(Context context) {
        /*
         * 위젯의 마지막 인스턴스가 제거될때 호출됨
         * onEnabled()에서 정의한 리소스 정리할때
         */
        // 기존 연구실 '일정 정보'&'상태 정보' XML 삭제
        Preferences = context.getSharedPreferences("LAB_WIDGET", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = Preferences.edit();
        editor.clear();
        editor.apply();
    }

    private void status_Request(final Context context) {
        final StringBuffer url = new StringBuffer("http://210.125.212.191:8888/IoT/IoTStatusCheck.jsp");

        StringRequest stringRequest = new StringRequest(
                Request.Method.POST, String.valueOf(url),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        // 암호화된 대칭키를 키스토어의 개인키로 복호화
                        char[] decryptAESkey = KEYSTORE.keyStore_Decryption(AES.secretKEY);

                        // 복호화된 대칭키를 이용하여 암호화된 데이터를 복호화 하여 진행
                        response = AES.aesDecryption(response.toCharArray(), decryptAESkey);

                        java.util.Arrays.fill(decryptAESkey,(char)0x20);

                        String[] resPonse_split = response.split("-");
                        switch (resPonse_split[0].trim()) { // 0번지는 재실여부
                            case "open":
                                lab_Person = true;
                                break;
                            case "close":
                                lab_Person = false;
                                break;
                            default:
                                lab_Person = false;
                                break;
                        }

                        switch (resPonse_split[1].trim()) { // 1번지는 커피 여부
                            case "coffeeenough":
                                lab_Coffe = true;
                                break;
                            case "coffeelack":
                                lab_Coffe = false;
                                break;
                            default:
                                lab_Coffe = false;
                                break;
                        }

                        switch (resPonse_split[2].trim()) { // 2번지는 A4 여부
                            case "A4enough":
                                lab_A4 = true;
                                break;
                            case "A4lack":
                                lab_A4 = false;
                                break;
                            default:
                                lab_A4 = false;
                                break;
                        }

                        // 위젯에 등록할 '상태' 정보 XML에 저장
                        Preferences = context.getSharedPreferences("LAB_WIDGET", Activity.MODE_PRIVATE);
                        SharedPreferences.Editor editor = Preferences.edit();
                        editor.putBoolean("PERSON", lab_Person);
                        editor.putBoolean("COFFE", lab_Coffe);
                        editor.putBoolean("A4", lab_A4);
                        editor.apply();

                        ChangeWidget(context);//변경된 아이콘 업데이트
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                // 암호화된 대칭키를 키스토어의 개인키로 복호화
                char[] decryptAESkey = KEYSTORE.keyStore_Decryption(AES.secretKEY);

                params.put("key", RSA.rsaEncryption(decryptAESkey, RSA.serverPublicKey.toCharArray()));
                params.put("check", AES.aesEncryption("security".toCharArray(), decryptAESkey));

                decryptAESkey = null;
                return params;
            }
        };

        // 캐시 데이터 가져오지 않음 왜냐면 기존 데이터 가져올 수 있기때문
        // 항상 새로운 데이터를 위해 false
        stringRequest.setShouldCache(false);
        VolleyQueueSingleTon.getInstance(context).addToRequestQueue(stringRequest);
    }

    // 연구실 일정 정보 조회
    private void member_calendar_Request(final Context context) {
        final StringBuffer url = new StringBuffer("http://210.125.212.191:8888/IoT/Schedule.jsp");

        StringRequest stringRequest = new StringRequest(
                Request.Method.POST, String.valueOf(url),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            // 암호화된 대칭키를 키스토어의 개인키로 복호화
                            char[] decryptAESkey = KEYSTORE.keyStore_Decryption(AES.secretKEY);

                            // 복호화된 대칭키를 이용하여 암호화된 데이터를 복호화 하여 진행
                            response = AES.aesDecryption(response.toCharArray(), decryptAESkey);

                            java.util.Arrays.fill(decryptAESkey,(char)0x20);

                            if ("scheduleNotExist".equals(response.trim())) {
                                // 등록된 일정이 없을 시
                                Preferences = context.getSharedPreferences("LAB_WIDGET", Activity.MODE_PRIVATE);
                                SharedPreferences.Editor editor = Preferences.edit();
                                editor.putString("CALENDER", "등록된 일정이 없습니다.");
                                editor.apply();
                            } else if ("error".equals(response.trim())) { // 시스템 오류
                                Toast.makeText(context, "시스템 오류입니다.", Toast.LENGTH_SHORT).show();
                            } else {
                                JSONArray jarray = new JSONArray(response);
                                JSONObject row = jarray.getJSONObject(0);

                                // 위젯에 등록할 '일정' 정보 XML에 저장
                                Preferences = context.getSharedPreferences("LAB_WIDGET", Activity.MODE_PRIVATE);
                                SharedPreferences.Editor editor = Preferences.edit();
                                editor.putString("CALENDER", row.getString("save_title"));
                                editor.apply();
                            }
                        } catch (JSONException e) {
                            System.err.println("WidgetProvider Response JSONException error");
                        }
                        status_Request(context);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();

                // 암호화된 대칭키를 키스토어의 개인키로 복호화
                char[] decryptAESkey = KEYSTORE.keyStore_Decryption(AES.secretKEY);

                params.put("securitykey", RSA.rsaEncryption(decryptAESkey, RSA.serverPublicKey.toCharArray()));
                params.put("type", AES.aesEncryption("scheduleList".toCharArray(), decryptAESkey));
                params.put("date", AES.aesEncryption(getTime().toCharArray(), decryptAESkey));

                java.util.Arrays.fill(decryptAESkey,(char)0x20);
                return params;
            }
        };

        // 캐시 데이터 가져오지 않음 왜냐면 기존 데이터 가져올 수 있기때문
        // 항상 새로운 데이터를 위해 false
        stringRequest.setShouldCache(false);
        VolleyQueueSingleTon.getInstance(context).addToRequestQueue(stringRequest);
    }

    private String getTime() {
        mNow = System.currentTimeMillis();
        mDate = new Date(mNow);
        return mFormat.format(mDate);
    }

    private void ChangeWidget(Context context) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_member_status);

        Preferences = context.getSharedPreferences("LAB_WIDGET", Activity.MODE_PRIVATE);
        calenderTitle = Preferences.getString("CALENDER", "등록된 일정이 없습니다.");
        lab_Person = Preferences.getBoolean("PERSON", false);
        lab_Coffe = Preferences.getBoolean("COFFE", false);
        lab_A4 = Preferences.getBoolean("A4", false);

        views.setTextViewText(R.id.tv_widget_calendar, " 연구실 대표 일정: " + calenderTitle + "\n 상세 정보는 일정을 눌러 확인하세요.");
        if (lab_Person)
            views.setImageViewResource(R.id.img_person, R.drawable.people_exist);
        else
            views.setImageViewResource(R.id.img_person, R.drawable.people_nonexist);
        if (lab_Coffe)
            views.setImageViewResource(R.id.img_coffee, R.drawable.coffee_exist);
        else
            views.setImageViewResource(R.id.img_coffee, R.drawable.coffee_nonexist);
        if (lab_A4)
            views.setImageViewResource(R.id.img_a4, R.drawable.a4_exist);
        else
            views.setImageViewResource(R.id.img_a4, R.drawable.a4_nonexist);

        Toast.makeText(context, "연구실 정보 조회 성공", Toast.LENGTH_SHORT).show();
        AppWidgetManager.getInstance(context).updateAppWidget(new ComponentName(context, WidgetProvider.class), views); // 위젯 업데이트

    }
}