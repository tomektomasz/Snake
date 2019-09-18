package com.example.snake;

import android.app.Activity;
import android.graphics.Point;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Display;

public class MainActivity extends Activity {        //SnakeMainActivity
    SnakeEngine snakeEngine;    //W konstruktorze musi być (this,size) , a w klasie metody pause() i resume()

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Display display = getWindowManager().getDefaultDisplay();   //uzyskuje wymiary ekranu w pikselach

        // Initialize the result into a Point object
        Point size = new Point();
        display.getSize(size);

        snakeEngine = new SnakeEngine(this, size);  //tworzenie obiektu-instancji naszej klasy, trzeba bedzie uwzglednic w konstruktorze

        // Ustawienie snakeEngine w widoku Activity
        setContentView(snakeEngine);    // byc moze powinno byc: setContentView(R.layout.activity_main);

    }

    @Override
    protected void onResume() {
        super.onResume();
        snakeEngine.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        snakeEngine.pause();
    }
}
