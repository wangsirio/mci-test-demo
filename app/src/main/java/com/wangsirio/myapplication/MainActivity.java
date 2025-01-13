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
            
            // 启动Cube_Analysis_Activity并传递userData
            Intent intent = new Intent(MainActivity.this, Cube_Analysis_Activity.class);
            intent.putExtra("userData", userData);
            startActivity(intent);
        });
    }
}