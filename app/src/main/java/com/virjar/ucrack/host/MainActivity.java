package com.virjar.ucrack.host;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.common.collect.Lists;
import com.virjar.ucrack.R;
import com.virjar.ucrack.plugin.ToolConstant;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import java.util.List;

public class MainActivity extends AppCompatActivity {


    private AppAdapter adapter;
    private List<Appinfo> appinfos = Lists.newArrayList();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ListView listView = (ListView) findViewById(R.id.applist);
        adapter = new AppAdapter(appinfos);
        listView.setAdapter(adapter);
        new Thread() {
            @Override
            public void run() {
                super.run();
                getInstallAppList();
            }
        }.start();
    }


    private void getInstallAppList() {

        final List<Appinfo> allAppInfo = Lists.newLinkedList();
        try {
            List<PackageInfo> packageInfos = getPackageManager().getInstalledPackages(0);
            for (PackageInfo packageInfo : packageInfos) {
                if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0 && (!StringUtils.equals(packageInfo.packageName, "com.smartdone.dexdump"))) {
                    Appinfo info = new Appinfo();
                    info.setIcon(zoomDrawable(packageInfo.applicationInfo.loadIcon(getPackageManager()), DensityUtil.dip2px(this, 128), DensityUtil.dip2px(this, 128)));
                    info.setAppName(packageInfo.applicationInfo.loadLabel(getPackageManager()).toString());
                    info.setAppPackage(packageInfo.packageName);
                    allAppInfo.add(info);
                }
            }
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    appinfos.addAll(allAppInfo);
                    adapter.notifyDataSetChanged();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    class AppAdapter extends BaseAdapter {

        private List<Appinfo> appinfos;


        public AppAdapter(List<Appinfo> appinfos) {
            this.appinfos = appinfos;
        }

        @Override
        public int getCount() {
            return appinfos.size();
        }

        @Override
        public Object getItem(int i) {
            return appinfos.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            View v = LayoutInflater.from(MainActivity.this).inflate(R.layout.item, null);
            final int posi = i;
            final TextView appname = (TextView) v.findViewById(R.id.tv_appname);
            appname.setText(appinfos.get(i).getAppName());
            TextView appPackage = (TextView) v.findViewById(R.id.tv_apppackage);
            appPackage.setText(appinfos.get(i).getAppPackage());
            ImageView icon = (ImageView) v.findViewById(R.id.iv_icon);
            icon.setImageDrawable(appinfos.get(i).getIcon());

            //给view添加点击事件
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, DetailConfigActivity.class);
                    JSONObject data = new JSONObject();
                    String appName = appinfos.get(posi).getAppName();
                    String appPackage = appinfos.get(posi).getAppPackage();
                    intent.putExtra(ToolConstant.appName, appName);
                    intent.putExtra(ToolConstant.appPackage, appPackage);
                    startActivity(intent);
                }
            });
            return v;
        }
    }

    private Drawable zoomDrawable(Drawable drawable, int w, int h) {
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        Bitmap oldbmp = drawableToBitmap(drawable);
        Matrix matrix = new Matrix();
        float scaleWidth = ((float) w / width);
        float scaleHeight = ((float) h / height);
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap newbmp = Bitmap.createBitmap(oldbmp, 0, 0, width, height,
                matrix, true);
        return new BitmapDrawable(null, newbmp);
    }

    private Bitmap drawableToBitmap(Drawable drawable) {
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        Bitmap.Config config = drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                : Bitmap.Config.RGB_565;
        Bitmap bitmap = Bitmap.createBitmap(width, height, config);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);
        return bitmap;
    }
}
