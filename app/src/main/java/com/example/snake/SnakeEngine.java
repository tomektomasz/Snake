package com.example.snake;

import android.view.SurfaceView;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.SoundPool;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
//import android.view.SurfaceView;
import java.io.IOException;
import java.util.Random;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

class SnakeEngine extends SurfaceView implements Runnable {

    private Thread thread = null;       //Nasz wątek gry dla głównej petli
    private Context context;            //Aby bylo odniesienie do Activity

    private SoundPool soundPool;        //3 linijki dla efektow dzwiekowych
    private int eat_bob = -1;
    private int snake_crash = -1;

    public enum Heading {UP, RIGHT, DOWN, LEFT};    //Do sledzenia ruchu glowy
    private Heading heading=Heading.RIGHT;          //Aby zaczac od pozycji w prawo
    private int screenX;            //Aby zachowac wymiary ekranu
    private int screenY;
    private int snakeLength;        //Jak dlugi jest waz
    private int bobX;               //Gdzie ukrywa sie Bob
    private int bobY;
    private int blockSize;          //Wielkosc jednego kawalka weza
    private final int NUM_BLOCK_WIDE = 40 ; //Wielkosc obszru grywalnego segmentu
    private int numBlocksHigh ;
    private long nextFrameTime ;        //kontrolowanie pauzy miedzy uaktualnieniami
    private final long FPS=5;          //Uaktualnianie gry 5 razy na sek.
    private final long MILLIS_PER_SECOND=1000;  //do przeliczenia ms na s , Ramke bedzie rysowana znacznie czesciej
    private int score;          //Ile pkt. ma gracz
    private int[] snakeXs;      //lokalizacja kazdego segmentu weza
    private int[] snakeYs;
                                //Rzeczy potrzebne do rysowania:
    private volatile boolean isPlaying;         //Czy gra jest aktualnie odtwarzana
    private Canvas canvas;          //Plotno do malowania
    private SurfaceHolder surfaceHolder;        //Wymagane do uzycia canvas
    private Paint paint;            //Troche pedzla dla plutna canvas

    public SnakeEngine(Context context, Point size){            //Konstruktor
        super(context);
        this.context=context;
        screenX=size.x;
        screenY=size.y;
        blockSize=screenX/NUM_BLOCK_WIDE;           //Obliczenie ile blokow miesci sie na ekranie (?)
        numBlocksHigh=screenY/blockSize;            //Ile blokow tej samej wielkosci bedzie pasowac do wysokosci (?)
/*
        soundPool=new SoundPool(10,AudioManager.STREAM_MUSIC,0);    //Wlaczenie dzwieku
        try{
            //Stworzenie obiektu 2 wymaganych klas
            //Uzycie m_Context poniewaz to jest reference do Activity
            AssetManager assetManager=context.getAssets();
            AssetFileDescriptor descriptor;

            //Przygotowanie dwoch dzwiekow w pamieci
            descriptor=assetManager.openFd("dzwiek_w_pliku.ogg");
            eat_bob=soundPool.load(descriptor,0);

            descriptor=assetManager.openFd("drugi_dzwiek_w_pliku.ogg");
            snake_crash=soundPool.load(descriptor,0);
        }   catch (IOException e){
            //Error
        }
*/
        //Obiekty rysunkowe
        surfaceHolder=getHolder();
        paint = new Paint();
        snakeXs=new int[200];
        snakeYs=new int[200];

        //Rozpoczecie gry
        newGame();
    }

    @Override
    public void run() {
        while (isPlaying){
            if(updateRequired()){   //metoda zdefiniowana pozniej
                update();           //metoda zdefiniowana pozniej
                draw();             //metoda zdefiniowana pozniej
            }
        }
    }
    public void pause(){
        isPlaying=false;
        try{
            thread.join();
        } catch (InterruptedException e){
            // Error
        }
    }
    public void resume(){
        isPlaying=true;
        thread=new Thread(this);
        thread.start();
    }
    public void newGame(){
        snakeLength=1;          //zaczynamy od pojedynczego czlonu weza
        snakeXs[0]=NUM_BLOCK_WIDE/2;
        snakeYs[0]=numBlocksHigh/2;
        spawnBob();         //ustawianie Boba do zjedzenia
        score=0;             //zresetowanie pkt.
        nextFrameTime=System.currentTimeMillis();       //astawienie nextFrameTime aby uruchomić aktualizacje
    }
    public void spawnBob(){             //ustawienie Boba w losowej pozycji
        Random random = new Random();
        bobX=random.nextInt(NUM_BLOCK_WIDE-1)+1;
        bobY=random.nextInt(numBlocksHigh-1)+1;
    }
    private void eatBob(){          //Mam Go! -zjedzenie Boba, metoda wprowadzona w update()
        snakeLength++;              //zwiekszenie rozmiaru weza
        spawnBob();                 //odnowienie Boba i ustawienie w nowym losowym miejscu
        score=score+1;
//        soundPool.play(eat_bob,1,1,0,0,1);  //dzwiek
    }
    private void moveSnake(){               //przesuniecie dlugiego weza - tak dlugo jak trzymamy glowe pozostale bloki tez beda ustawione prawidlowo, metoda uzyta w update()
        for(int i=snakeLength;i>0;i--){     //petle zaczynamy od tylu i zmiezamy do glowy, ale sama glowe nalezy wykluczyc
            snakeXs[i]=snakeXs[i-1];
            snakeYs[i]=snakeYs[i-1];
        }
        switch (heading){               //kierunki w ktorych przesuwamy glowe
            case UP:
                snakeYs[0]--;
                break;
            case RIGHT:
                snakeXs[0]++;
                break;
            case DOWN:
                snakeYs[0]++;
                break;
            case LEFT:
                snakeXs[0]--;
                break;
        }
    }
    private boolean detectDeath(){          //zwraca false gdy waz zyje -spawdza czy niema kolizji z krawedzia ekranu, i z cialem weza, metoda uzyta w update()
        boolean dead = false;
        if(snakeXs[0]==-1) dead=true;
        if(snakeXs[0]==NUM_BLOCK_WIDE) dead=true;
        if(snakeYs[0]==-1) dead=true;
        if (snakeYs[0]==numBlocksHigh) dead=true;

        for(int i=snakeLength-1;i>0;i--){
            if((i>4)&&(snakeXs[0]==snakeXs[i])&&(snakeYs[0]==snakeYs[i])){
                dead=true;
            }
        }
        return dead;
    }
    public void update(){
        if(snakeXs[0]==bobX && snakeYs[0]==bobY) eatBob();      //gdy glowa weza wejdzie na Boba
        moveSnake();
        if (detectDeath()){
//            soundPool.play(snake_crash,1,1,0,0,1);        //dzwiek
            newGame();
        }
    }
    public void draw(){             //rysowanie weza i Boba
        if (surfaceHolder.getSurface().isValid()){
            canvas=surfaceHolder.lockCanvas();                  //zablokowanie canvas -wymagane do uzycia canvas
            canvas.drawColor(Color.argb(255,26,128,182));        //wypelnienie ekranu kolorem blue
            paint.setColor(Color.argb(255,255,255,255));        //ustawienie koloru weza i tekstu
            paint.setTextSize(90);          //ustawienie wielkosci tekstu
            canvas.drawText("Score: "+score, 10, 70, paint);        //tekst z pkt i wspolrzedne tekstu
            for (int i=0;i<snakeLength;i++){            //rysowanie samego weza
                canvas.drawRect(snakeXs[i]*blockSize,
                        snakeYs[i]*blockSize,
                        (snakeXs[i]*blockSize)+blockSize,
                        (snakeYs[i]*blockSize)+blockSize,
                         paint);
            }
            paint.setColor(Color.argb(255,255,0,0));            //ustawienie koloru do rysowania Boba
            canvas.drawRect(bobX*blockSize,             //rysowanie bloku Boba
                    bobY*blockSize,
                    (bobX*blockSize)+blockSize,
                    (bobY*blockSize)+blockSize,
                    paint);
            surfaceHolder.unlockCanvasAndPost(canvas);          //odblokowanie canvas i pokazanie grafiki
        }
    }
    public boolean updateRequired(){
        if(nextFrameTime<=System.currentTimeMillis()){
            nextFrameTime=System.currentTimeMillis()+MILLIS_PER_SECOND/FPS;
            return  true;               //gdy zostanie zwrocone true to fynkcja update() i draw() zostana wykonane
        }
        return false;
    }
    @Override
    public boolean onTouchEvent(MotionEvent motionEvent){

        switch (motionEvent.getAction()&MotionEvent.ACTION_MASK){
            case MotionEvent.ACTION_UP:
                if (motionEvent.getX() >= screenX /2){
                    switch (heading){
                        case UP:
                            heading=Heading.RIGHT;
                            break;
                        case RIGHT:
                            heading=Heading.DOWN;
                            break;
                        case DOWN:
                            heading=Heading.LEFT;
                            break;
                        case LEFT:
                            heading=Heading.UP;
                            break;
                    }
                }
                else {
                    switch (heading){
                        case UP:
                            heading=Heading.LEFT;
                            break;
                        case LEFT:
                            heading=Heading.DOWN;
                            break;
                        case DOWN:
                            heading=Heading.RIGHT;
                            break;
                        case RIGHT:
                            heading=Heading.UP;
                            break;
                    }
                }

        }
        return true;
    }

}
