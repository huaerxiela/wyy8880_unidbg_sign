package com.netease.cloudmusic;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.memory.Memory;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;

import java.io.File;

public class Sign extends AbstractJni {

    private final AndroidEmulator emulator;
    private final VM vm;
    private final Module module;
    private final Memory memory;
    private final DvmClass neteaseMusicUtils;

    private Sign() {
        emulator = AndroidEmulatorBuilder
                .for32Bit()
                .setProcessName("com.netease.cloudmusic")
                .addBackendFactory(new Unicorn2Factory(true))
                .build();
        emulator.getSyscallHandler().setVerbose(true);
        memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        memory.setCallInitFunction(true);
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/wangyi/wangyi8880.apk"));
        vm.setJni(this);
        vm.setVerbose(true);
        DalvikModule dm = vm.loadLibrary("poison", true);
        module = dm.getModule();
        module.callFunction(emulator, 0x94d8 + 1, vm.getJavaVM(), 0);
        neteaseMusicUtils = vm.resolveClass("com/netease/cloudmusic/utils/NeteaseMusicUtils");
    }

    public String SerialData(String p1, String p2) {
        String methodSign = "serialdata(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;";
        StringObject obj = neteaseMusicUtils.callStaticJniMethodObject(emulator, methodSign, p1, p2);
        return obj.getValue();
    }

    public static String sendPost(String url, Header[] headers, String param) {
        HttpPost httpPost = new HttpPost(url);
        CloseableHttpClient client = HttpClients.createDefault();
        httpPost.setHeaders(headers);
        StringEntity entity = new StringEntity(param, "UTF-8");
        httpPost.setEntity(entity);
        HttpHost proxy = new HttpHost("127.0.0.1", 8089, "http");
        RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(2000).setConnectTimeout(2000).setProxy(proxy).build();
        httpPost.setConfig(requestConfig);
        try {
            HttpResponse response = client.execute(httpPost);
            if (response.getStatusLine().getStatusCode() == 200) {
                return EntityUtils.toString(response.getEntity(), "UTF-8");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String searchComplex(String keyword) {
        Sign sign = new Sign();
        String host = "http://interface.music.163.com";
        String path = "/eapi/search/complex/page";
        String data = String.format("{\"cursor\":\"{\\\"page\\\":0,\\\"ignoreBlocks\\\":[],\\\"traceId\\\":\\\"\\\"}\",\"needCorrect\":\"true\",\"bizQueryInfo\":\"\",\"channel\":\"hotquery\",\"scene\":\"normal\",\"keyword\":\"%s\",\"header\":\"{}\",\"e_r\":false}", keyword);
        System.out.println(data);
        String path2 = path.replace("/eapi", "/api");
        String s = sign.SerialData(path2, data);
        String param = String.format("params=%s", s);
        System.out.println(param);
        String url = host + path;
        System.out.println(url);
        Header[] headers = {
                new BasicHeader("Accept-Encoding", "gzip"),
                new BasicHeader("Content-Type", "application/x-www-form-urlencoded"),
                new BasicHeader("User-Agent", "NeteaseMusic/7.3.27.1603459753(7003027);Dalvik/2.1.0 (Linux; U; Android 6.0.1; Nexus 5X Build/MTC20K)"),
                new BasicHeader("Cookie", "MUSIC_U=3d1bf9b5def4a5a00d3bf4a309086dfbd7aa1036ac5f9e9dd036dbca10eaefb6dbcb429fac0fda344321f173ae6905c731b299d667364ed3; os=android; osver=8.0.0; appver=5.1.28; deviceId=98cdc76f3b7b7ecbfb890bda7dcda8e6"),
        };
        return sendPost(url, headers, param);
    }

    public static void main(String[] args) {
        System.out.println(searchComplex("china"));
    }
}