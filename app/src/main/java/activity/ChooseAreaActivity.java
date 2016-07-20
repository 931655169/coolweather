package activity;



import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.zjm.coolweather.R;

import java.util.ArrayList;
import java.util.List;

import model.City;
import db.CoolWeatherDB;
import model.County;
import model.Province;
import util.HttpCallbackListener;
import util.HttpUtil;
import util.Utility;

/**
 * Created by zjm on 2016/7/17.
 */
public class ChooseAreaActivity extends Activity {
    public static final int LEVEL_PROVINCE=0;
    public static final int LEVEL_CITY=1;
    public static final int LEVEL_COUNTY=2;

    private ProgressDialog progressDialog;
    private TextView titleText;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private CoolWeatherDB coolWeatherDB;
    private List<String> dataList=new ArrayList<String>();
    /**
     * 省列表
     */
    private List<Province> provinceList;
    /**
     * 市列表
     */
    private List<City> cityList;
    /**
     * 县列表
     */
    private List<County> countyList;
    /**
     * 选中的省份
     */
    private Province selectedProvince;
    /**
     * 选中的城市
     */
    private City selectedCity;
    /**
     * 当前选中的级别
     */
    private int currentLevel;

    /**
     *是否从WeatherActivtiy中跳转过来
    **/
    private boolean isFromWeatherActivtiy;

    @Override
    protected  void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);//继承父类

        isFromWeatherActivtiy=getIntent().getBooleanExtra("form_weather_activtiy",false);
        SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(this);
        //已经选择好城市且不是从WeatherActivtity跳转过来的，才会直接转到WeatherActivtity
        if (prefs.getBoolean("city_selected",false) && (!isFromWeatherActivtiy)){
            Intent intent=new Intent(this,WeatherActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.choose_area);
        listView=(ListView) findViewById(R.id.list_view);
        titleText=(TextView) findViewById(R.id.title_text);
        adapter=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,dataList);
        listView.setAdapter(adapter);
        coolWeatherDB=CoolWeatherDB.getInstance(this);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> args0, View view, int index, long arg3) {
                if (currentLevel==LEVEL_PROVINCE){//如果设置等级为省
                    selectedProvince=provinceList.get(index);//存入数据
                    queryCities();
                }else if(currentLevel==LEVEL_CITY){//如果设置等级为城市
                    selectedCity=cityList.get(index);
                    queryCounties();
                }else if (currentLevel==LEVEL_COUNTY){//如果设置为县
                    String countyCode=countyList.get(index).getCountyCode();
                    Intent intent=new Intent(ChooseAreaActivity.this,WeatherActivity.class);
                    intent.putExtra("county_code",countyCode);
                    startActivity(intent);
                    finish();
                }
            }
        });
        queryProvinces();//加载省级数据
    }
    /**
     * 查询全国所有的省，优先从数据库查询，如果没有查询到再去服务器查询
     */
    private void queryProvinces(){
        provinceList=coolWeatherDB.loadProvinces();
        if (provinceList.size()>0){//如果省列表有数据的话
            dataList.clear();
            for(Province province:provinceList){//将省列表的数据循环存储
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText("中国");
            currentLevel=LEVEL_PROVINCE;
        }else{
            queryFromServer(null,"province");
        }
    }
    /**
     * 查询选中省内所有的市，优先从数据库查询，如果没有查询到再去服务器上查询
     */
    private void queryCities(){
        cityList=coolWeatherDB.loadCities(selectedProvince.getId());
        if (cityList.size()>0){//若有循环列表
            dataList.clear();
            for (City city:cityList){//循环存储完城市的名字
            dataList.add(city.getCityName());
        }
            adapter.notifyDataSetChanged();//动态刷新listView
            listView.setSelection(0);
            titleText.setText(selectedProvince.getProvinceName());
            currentLevel=LEVEL_CITY;
    }else{
            queryFromServer(selectedProvince.getProvinceCode(),"city");
        }
}
/**
 * 查询选中市内所有的县，优先从数据库查询，如果没有查询到再去服务器查询。
 */
private void queryCounties(){
countyList=coolWeatherDB.loadCounties(selectedCity.getId());
    if (countyList.size()>0){
        dataList.clear();
        for (County county:countyList){
            dataList.add(county.getCountyName());
        }
        adapter.notifyDataSetChanged();
        listView.setSelection(0);
        titleText.setText(selectedCity.getCityName());
        currentLevel=LEVEL_COUNTY;
    }else {
        queryFromServer(selectedCity.getCityCode(),"county");
    }
}
/**
 * 根据传入的代码和类型从服务器上查询省市县的数据
 */
private void queryFromServer(final String code,final String type){
    String address;
    if (!TextUtils.isEmpty(code)){
        address="http://www.weather.com.cn/data/list3/city"+code+".xml";
    }else{
        address="http://www.weather.com.cn/data/list3/city.xml";
    }
    showProgressDialog();
    HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
        @Override
        public void onFinish(String response) {
            boolean result=false;
            if ("province".equals(type)){
                result= Utility.handleProvincesResponse(coolWeatherDB,response);
            }else if("city".equals(type)){
                result=Utility.handleCitiesResponse(coolWeatherDB,response,selectedProvince.getId());
            }else if("county".equals(type)){
                result=Utility.handleCountiesResponse(coolWeatherDB,response,selectedCity.getId());
            }
            if (result){
                //通过runOnUiThread()方法回到主线程处理逻辑
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        if ("province".equals(type)){
                            queryProvinces();
                        }else if ("city".equals(type)){
                            queryCities();
                        }else if ("county".equals(type)){
                            queryCounties();
                        }
                    }
                });
            }
        }

        @Override
        public void onError(Exception e) {
            //通过runOnUiThread方法回到主线程逻辑
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    closeProgressDialog();
                    Toast.makeText(ChooseAreaActivity.this,"加载失败", Toast.LENGTH_SHORT).show();
                }
            });
        }
    });
}
    /**
     * 显示进度对话框
     */
    private void showProgressDialog() {
        if (progressDialog==null){
            progressDialog=new ProgressDialog(this);
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);//按对话框以外的地方不起作用，按返回键才起作用
        }
        progressDialog.show();
    }
    /**
     * 关闭进度对话框
     */
    private void closeProgressDialog(){
        if (progressDialog != null){
            progressDialog.dismiss();
        }
    }
    /**
     * 捕获Back按键，根据当前的级别来判断，此时应该返回市列表，省列表，还是直接退出
     */
    @Override
    public void onBackPressed(){
        if (currentLevel==LEVEL_COUNTY){
            queryCities();
        }else if (currentLevel==LEVEL_CITY){
            queryProvinces();
        }else {
            if (isFromWeatherActivtiy){
                Intent intent=new Intent(this,WeatherActivity.class);
                startActivity(intent);
            }
            finish();
        }
    }
}
