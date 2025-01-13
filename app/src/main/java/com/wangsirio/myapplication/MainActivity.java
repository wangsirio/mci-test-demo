package com.wangsirio.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONObject;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import android.os.AsyncTask;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {
    private String selectedGender = "男";
    private String selectedEducation = "未上学";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        // 初始化性别Spinner
        setupGenderSpinner();
        
        // 初始化学历Spinner
        setupEducationSpinner();
        
        // 初始化年龄输入框
        setupAgeEditText();
        
        // 设置下一步按钮点击事件
        setupNextButton();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    
    private void setupGenderSpinner() {
        Spinner spinnerGender = findViewById(R.id.spinner_gender);
        ArrayAdapter<CharSequence> adapterGender = ArrayAdapter.createFromResource(this,
                R.array.gender_array, android.R.layout.simple_spinner_item);
        adapterGender.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGender.setAdapter(adapterGender);
        
        spinnerGender.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedGender = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }
    
    private void setupEducationSpinner() {
        Spinner spinnerEducation = findViewById(R.id.spinner_education);
        ArrayAdapter<CharSequence> adapterEducation = ArrayAdapter.createFromResource(this,
                R.array.education_array, android.R.layout.simple_spinner_item);
        adapterEducation.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEducation.setAdapter(adapterEducation);
        
        spinnerEducation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedEducation = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }
    
    private void setupAgeEditText() {
        EditText editTextAge = findViewById(R.id.editText_age);
        editTextAge.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().isEmpty()) {
                    int age = Integer.parseInt(s.toString());
                    if (age < 0 || age > 150) {
                        editTextAge.setError("请输入有效年龄（0-150岁）");
                    }
                }
            }
        });
    }
    
    private void setupNextButton() {
        Button buttonNext = findViewById(R.id.button_next);
        buttonNext.setOnClickListener(v -> {
            EditText editTextAge = findViewById(R.id.editText_age);
            String ageStr = editTextAge.getText().toString();
            
            if (ageStr.isEmpty()) {
                editTextAge.setError("请输入年龄");
                return;
            }
            
            int age = Integer.parseInt(ageStr);
            if (age < 0 || age > 150) {
                editTextAge.setError("请输入有效年龄（0-150岁）");
                return;
            }

            // 创建用户数据对象
            UserData userData = new UserData(selectedGender, selectedEducation, age);
            
            // 发送数据到服务器
            new SendDataTask().execute(userData);
        });
    }

    private class SendDataTask extends AsyncTask<UserData, Void, String> {
        @Override
        protected String doInBackground(UserData... userData) {
            HttpURLConnection conn = null;
            try {
                String serverUrl = getString(R.string.server_url);
                Log.d("NetworkRequest", "Connecting to: " + serverUrl);
                
                URL url = new URL(serverUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");
                // 增加超时时间
                conn.setConnectTimeout(1500);  // 15秒
                conn.setReadTimeout(1500);     // 15秒
                conn.setDoOutput(true);
                conn.setDoInput(true);
                // 禁用缓存
                conn.setUseCaches(false);

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("gender", userData[0].getGender());
                jsonObject.put("education", userData[0].getEducation());
                jsonObject.put("age", userData[0].getAge());
                
                String jsonString = jsonObject.toString();
                Log.d("NetworkRequest", "Sending data: " + jsonString);

                // 连接前打印日志
                Log.d("NetworkRequest", "Connecting...");
                conn.connect();
                Log.d("NetworkRequest", "Connected");

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonString.getBytes("UTF-8");
                    os.write(input, 0, input.length);
                    os.flush();
                    Log.d("NetworkRequest", "Data written to output stream");
                }

                int responseCode = conn.getResponseCode();
                Log.d("NetworkRequest", "Response code: " + responseCode);
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // 读取响应
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                        StringBuilder response = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                        Log.d("NetworkRequest", "Response: " + response.toString());
                    }
                    return "success";
                } else {
                    // 读取错误流
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getErrorStream(), "UTF-8"))) {
                        StringBuilder error = new StringBuilder();
                        String errorLine;
                        while ((errorLine = br.readLine()) != null) {
                            error.append(errorLine.trim());
                        }
                        Log.e("NetworkRequest", "Error response: " + error.toString());
                        return "服务器返回错误代码: " + responseCode + ", " + error.toString();
                    }
                }

            } catch (java.net.ConnectException e) {
                Log.e("NetworkRequest", "Connection error: " + e.getMessage(), e);
                return "连接服务器失败，请检查服务器地址是否正确: " + e.getMessage();
            } catch (java.net.SocketTimeoutException e) {
                Log.e("NetworkRequest", "Timeout error: " + e.getMessage(), e);
                return "连接服务器超时: " + e.getMessage();
            } catch (Exception e) {
                Log.e("NetworkRequest", "Error: " + e.getMessage(), e);
                e.printStackTrace();
                return "错误: " + e.getMessage();
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if ("success".equals(result)) {
                // 数据发送成功，跳转到新的Activity
                Intent intent = new Intent(MainActivity.this, Cube_Analysis_Activity.class);
                startActivity(intent);
            } else {
                Toast.makeText(MainActivity.this, result, Toast.LENGTH_LONG).show();
            }
        }
    }
}