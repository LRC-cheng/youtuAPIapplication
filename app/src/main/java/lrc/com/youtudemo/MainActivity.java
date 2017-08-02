package lrc.com.youtudemo;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.youtu.Youtu;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import static com.common.Config.APP_ID;
import static com.common.Config.SECRET_ID;
import static com.common.Config.SECRET_KEY;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    String TAG = "main";

    //动态获取权限
    private static final int IMAGE = 1;
    private static final int REQUEST_EXTERNAL_STORAGE = 2;

    //测试响应时间
    long starTime;
    long endTime;
    long time;

    Bitmap bm;

    ImageView imgAddPhoto;
    ImageView imgShow;
    TextView result;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

    }

    //初始化布局资源
    void init(){
        imgAddPhoto = (ImageView) findViewById(R.id.img_add);
        result = (TextView) findViewById(R.id.result);
        imgShow = (ImageView) findViewById(R.id.img_show);
        imgAddPhoto.setOnClickListener(this);

    }
    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.img_add:
                //启动相册
                Intent intent1 = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent1, IMAGE);
                break;
            default:
                break;
        }
    }


    //发送图片到优图服务器
    void checkPhoto(final Bitmap bitmapA){
        Toast.makeText(this, "send", Toast.LENGTH_SHORT).show();
        new Thread() {
            public void run() {
                Youtu faceYoutu = new Youtu(APP_ID, SECRET_ID, SECRET_KEY, Youtu.API_YOUTU_END_POINT);
                JSONObject respose;
                try {
                    starTime=System.currentTimeMillis();
                    respose = faceYoutu.ImageTag(bitmapA);
                    Message msg = new Message();
                    msg.obj = respose;
                    handler.sendMessage(msg);
                } catch (KeyManagementException e) {
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }.start();

    }

    //获取结果
    private Handler handler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            endTime=System.currentTimeMillis();
            time = endTime-starTime;
            Toast.makeText(MainActivity.this,"get"+msg.obj.toString()+",耗时:"+time, Toast.LENGTH_SHORT).show();
            result.setText(msg.obj.toString());
            selectTag();
            Log.d(TAG,msg.obj.toString());
            Log.d(TAG,"time:"+time);
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //storagepermissions(this);
        //获取图片路径
        if (requestCode == IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            String[] filePathColumns = {MediaStore.Images.Media.DATA};
            Cursor c = getContentResolver().query(selectedImage, filePathColumns, null, null, null);
            if (c != null) {
                c.moveToFirst();
            }
            int columnIndex = c.getColumnIndex(filePathColumns[0]);
            String imagePath = c.getString(columnIndex);

            bm = ImagePathToBitmap(imagePath);
            if(bm!=null) {
                imgShow.setVisibility(View.VISIBLE);
                imgShow.setImageBitmap(bm);
                checkPhoto(bm);
            }else{
                Toast.makeText(this, "加载失败", Toast.LENGTH_SHORT).show();
            }
            c.close();
        }
    }

    //通过路径转换为Bitmap
    private Bitmap ImagePathToBitmap(String imgPath) {
        //Toast.makeText(this, "imgPath:"+imgPath, Toast.LENGTH_SHORT).show();
        Bitmap bitmap=null;
        if (imgPath != null) {
            bitmap = BitmapFactory.decodeFile(imgPath);
        } else {
            Toast.makeText(this, "false to get photo", Toast.LENGTH_SHORT).show();
        }
        return bitmap;
    }


    //筛选标签
    public void selectTag() {
        String tagName;
        int sorce;
        String text = " ";
        String rs = result.getText().toString();
        JsonParser parser = new JsonParser();
        JsonObject object = (JsonObject) parser.parse(rs);
        JsonArray array = object.get("tags").getAsJsonArray();
        for (int i = 0; i < array.size(); i++) {
            JsonObject subObject = array.get(i).getAsJsonObject();
            tagName = subObject.get("tag_name").getAsString();
            sorce = subObject.get("tag_confidence").getAsInt();
            if(sorce>30){
                text=text+tagName+" ";
            }
        }
        result.setText(text);
    }


//    //动态获取权限（如果加载不了图片可能需要用到下面的代码）
//    private static String[] PERMISSIONS_STORAGE = {
//            Manifest.permission.READ_EXTERNAL_STORAGE
//    };
//    public static void storagepermissions(Activity activity) {
//        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
//        if(permission != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
//        }
//    }
}
