package com.example.esp8266apiintegration;

import android.content.Context;
import android.graphics.Color;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.Button;

public class UIUtils {
    public static Button createStyledButton(Context context, String text, View.OnClickListener onClick) {
        Button button = new Button(context);
        button.setText(text);
        button.setOnClickListener(onClick);
        button.setBackgroundResource(R.drawable.styled_button);
        button.setTextColor(Color.parseColor("#00FF00"));
        button.setPadding(16, 8, 16, 8); // Consistent padding
        return button;
    }
}
