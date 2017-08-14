package com.example.administrator.assetsapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.example.administrator.assetsapp.Bean.LabelBean;
import com.example.administrator.assetsapp.Bean.ShowListView;
import com.example.administrator.assetsapp.Bean.TempBean;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.google.zxing.activity.CaptureActivity;
import com.google.zxing.activity.PermissionUtils;

import org.xutils.common.Callback;
import org.xutils.ex.HttpException;
import org.xutils.http.RequestParams;
import org.xutils.x;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.view.View.*;

/**
 * 主页面
 */

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnClickListener {
    @BindView(R.id.llSensor)
    LinearLayout llSensor;
    @BindView(R.id.llScan)
    LinearLayout llScan;
    @BindView(R.id.llLabel)
    LinearLayout llLabel;
    @BindView(R.id.llAdout)
    LinearLayout llAdout;

    @BindView(R.id.list_mian)
    ListView listMain;



    public static final int REQUEST_CODE = 0;
    //记录第一次点击的时间
    private long clickTime = 0;
    public static final String TAG = "Main2";
    public static final String API_BASE_URL = "http://112.74.212.95/php/select_latest_temp.php";
    public String resp = "";
    //将数值存放在list
    List<HashMap<String, Object>> list = new ArrayList<>();

    private Timer mainTimer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        mainTimer = new Timer();
        setTimerTask();
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        getView();
        initView();

    }

    private void initView() {
        llSensor.setOnClickListener(this);
        llScan.setOnClickListener(this);
        llLabel.setOnClickListener(this);
        llAdout.setOnClickListener(this);
    }

    //网络请求
    public void getView() {
        RequestParams requestParams = new RequestParams("http://112.74.212.95/php/select_asset.php");
        x.http().get(requestParams, new Callback.CacheCallback<String>() {
            @Override
            public void onSuccess(String result) {
                if (result.length() > 5) {
                    list.clear();
                    list = parseJSONWithGSON(result);
                    Log.d(TAG, "list is " + list);
                    ShowListView showListView = new ShowListView(MainActivity.this, list, R.layout.item_list_content);
                    listMain.setAdapter(showListView);
                    showListView.notifyDataSetChanged();
                }
            }

            @Override
            public void onError(Throwable ex, boolean isOnCallback) {
                Toast.makeText(x.app(), ex.getMessage(), Toast.LENGTH_LONG).show();
                if (ex instanceof HttpException) { // 网络错误
                    HttpException httpEx = (HttpException) ex;
                    int responseCode = httpEx.getCode();
                    String responseMsg = httpEx.getMessage();
                    String errorResult = httpEx.getResult();
                    // ...
                } else { // 其他错误
                    // ...
                }
            }

            @Override
            public void onCancelled(CancelledException cex) {
                Toast.makeText(x.app(), "cancelled", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFinished() {

            }

            @Override
            public boolean onCache(String result) {
                return false;
            }
        });

    }

    public List<HashMap<String, Object>> parseJSONWithGSON(String response) {
        JsonObject jsonObject = new JsonParser().parse(response).getAsJsonObject();
        JsonArray jsonArray = jsonObject.getAsJsonArray("data");
        Gson gson = new Gson();
        ArrayList<LabelBean> labelBeans = new ArrayList<>();
        for (JsonElement res : jsonArray) {
            LabelBean labelBean = gson.fromJson(res, new TypeToken<LabelBean>() {
            }.getType());
            labelBeans.add(labelBean);
        }

        int i = 0;
        for (LabelBean res : labelBeans) {
            Log.d(TAG, "设备名  " + res.getCardName());
            Log.d(TAG, "地点    " + res.getPlaceName());
            Log.d(TAG, "出入库  " + res.getInout());
            Log.d(TAG, "时间    " + res.getMASKSYNCV2());
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put("CardName", res.getCardName());
            map.put("PlaceName", res.getPlaceName());
            map.put("Inout", res.getInout());
            map.put("MASKSYNCV2", res.getMASKSYNCV2());
            list.add(map);
            i++;
            if(i>3){
                break;
            }
        }
        return list;
    }

    //数据采集显示
    private void setData() {
        Log.d(TAG, "启动");
        final String title = "最新温度数据表(° )";
        final String title2 = "最新湿度数据表(% )";
        final String[] xLabel1 = new String[7];
        final String[] xLabel2 = new String[7];
        final String[] data1 = new String[7];
        final String[] data2 = new String[7];
        final String[] qi = new String[7];
        final String[] huo = new String[7];
        final String[] time = new String[7];
        final String[] name = new String[7];
        RequestParams params = new RequestParams("http://112.74.212.95/php/select_temp.php");

        Callback.Cancelable cancelable
                = x.http().get(params, new Callback.CacheCallback<String>() {
            @Override
            public void onSuccess(String resp) {
                if (resp.length() > 10) {
                    Log.d(TAG, resp);
                    int j = 6;
                    Gson gson = new Gson();
                    List<TempBean> ressul = gson.fromJson(resp, new TypeToken<List<TempBean>>() {
                    }.getType());
                    for (TempBean res : ressul) {
                        Log.d(TAG, "Temp is " + res.getTemp());
                        Log.d(TAG, "Humidity is " + res.getHumidity());
                        String str = res.getHappenTime().toString().substring(11);
                        time[j] = res.getHappenTime().toString().substring(0, 10);
                        Log.d(TAG, "Time is " + time[j]);
                        Log.d(TAG, "str is " + str);
                        xLabel1[j] = str;
                        xLabel2[j] = str;
                        data1[j] = res.getTemp();
                        data2[j] = res.getHumidity();
                        qi[j] = res.getQi();
                        huo[j] = res.getHuo();
                        name[j] = res.getplaceName().toString();
                        j--;
                    }

                    if (Double.parseDouble(data2[6].toString()) >= 90.0) {
                        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                        dialog.setTitle("提示");
                        dialog.setMessage(name[6]+"湿度过高，请马上前往地点查看！");
                        dialog.setPositiveButton("确定", new
                                DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {

                                    }
                                });
                        dialog.show();
                    }
                    if (qi[6].toString().contains("异常")) {
                        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
//                        dialog.setIcon(R.drawable.check_bg)
                        dialog.setTitle("提示");
                        dialog.setMessage(name[6]+"气体异常，请马上前往地点查看！");
                        dialog.setPositiveButton("确定", new
                                DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {

                                    }
                                });

                        dialog.show();
                    }

                    if (huo[6].toString().contains("异常")) {
                        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                        dialog.setTitle("提示");
                        dialog.setMessage(name[6]+"火焰异常，请马上前往地点查看！");
                        dialog.setPositiveButton("确定", new
                                DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {

                                    }
                                });
                        dialog.show();
                    }
                } else

                {
                    Log.d(TAG, "超时");
                }
            }

            @Override
            public void onError(Throwable ex, boolean isOnCallback) {
                Toast.makeText(x.app(), ex.getMessage(), Toast.LENGTH_LONG).show();
                if (ex instanceof HttpException) { // 网络错误
                    HttpException httpEx = (HttpException) ex;
                    int responseCode = httpEx.getCode();
                    String responseMsg = httpEx.getMessage();
                    String errorResult = httpEx.getResult();
                    // ...
                } else { // 其他错误
                    // ...
                }
            }

            @Override
            public void onCancelled(CancelledException cex) {
                Toast.makeText(x.app(), "取消", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFinished() {

            }

            @Override
            public boolean onCache(String result) {
                return false;
            }
        });
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    //跳转页面
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.llSensor:
                Intent inSensor = new Intent(MainActivity.this, SensorActivity.class);
                startActivity(inSensor);

                break;
            case R.id.llScan:
                showCamera(view);
                Intent inScan = new Intent(MainActivity.this, CaptureActivity.class);
                startActivity(inScan);

                break;
            case R.id.llLabel:
                Intent inLabel = new Intent(MainActivity.this, LabelActivity.class);
                startActivity(inLabel);

                break;
            case R.id.llAdout:
                Intent inAdout = new Intent(MainActivity.this, AdoutActivity.class);
                startActivity(inAdout);
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) { //RESULT_OK = -1
            Bundle bundle = data.getExtras();
            String scanResult = bundle.getString("result");
            Toast.makeText(MainActivity.this, scanResult, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Called when the 'show camera' button is clicked.
     * Callback is defined in resource layout definition.
     */
    public void showCamera(View view) {
        Log.i(TAG, "Show camera button pressed. Checking permission.");
        PermissionUtils.requestPermission(this, PermissionUtils.CODE_CAMERA, mPermissionGrant);
    }

    private PermissionUtils.PermissionGrant mPermissionGrant = new PermissionUtils.PermissionGrant() {
        @Override
        public void onPermissionGranted(int requestCode) {
            switch (requestCode) {
                case PermissionUtils.CODE_CAMERA:
                    Log.d(TAG, "相机打开成功");
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        PermissionUtils.requestPermissionsResult(this, requestCode, permissions, grantResults, mPermissionGrant);
    }


    //点击退出
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            exit();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    //判断是否退出
    private void exit() {
        if ((System.currentTimeMillis() - clickTime) > 2000) {
            Toast.makeText(getApplicationContext(), "提示：再按一次后退键退出程序",
                    Toast.LENGTH_SHORT).show();
            clickTime = System.currentTimeMillis();
        } else {
            Log.e(TAG, "exit application");
            this.finish();
        }
    }

    private void setTimerTask() {
        mainTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                getView();
                //setData();
            }
        }, 200, 500);//表示200毫秒之后，每隔500毫秒执行一次
    }
}
