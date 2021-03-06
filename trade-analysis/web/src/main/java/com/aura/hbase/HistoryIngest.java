package com.aura.hbase;

import com.aura.model.ShopInfo;
import com.aura.service.ShopInfoService;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.stereotype.Controller;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;

@Controller("historyIngest")
public class HistoryIngest extends Ingest {

    @Resource
    private ShopInfoService service;

    private static final String user_pay = "trade-analysis/web/data/user_pay.txt";
    public static final String QUALIFIER_NAME_SHOPID = "shopid";
    public static final String QUALIFIER_NAME_PAYTIME = "paytime";
    public static final String QUALIFIER_NAME_CITYNAME = "city";
    public static final String QUALIFIER_NAME_PERPAY = "perpay";
    public static final String QUALIFIER_NAME_CATE2 = "cate2";

    //insert data to hbase
    @Override
    public void process() {
        FileSystem fs = null;
        BufferedReader in = null;
        Configuration conf = new Configuration();
        Path myPath = new Path(user_pay);
        try {
            fs = myPath.getFileSystem(conf);

            FSDataInputStream hdfsInStream = fs.open(new Path(user_pay));
            in = new BufferedReader(new InputStreamReader(hdfsInStream));
            String line = null;
            while ((line = in.readLine()) != null) {
                System.out.println(line);
                String[] parts = line.split(",", -1);
                String rowkey = userIdCompletion(parts[0]) + removeLine(parts[2].substring(0,10));
                Put put = new Put(Bytes.toBytes(rowkey));
                //get shop_info from MySQL
                System.out.println(parts[1]);
                ShopInfo info = service.getShopInfoById(Integer.valueOf(parts[1].trim()));
                //byte[] family, byte[] qualifier, byte[] value
                put.addColumn(Bytes.toBytes(column_family_cf1), Bytes.toBytes(QUALIFIER_NAME_SHOPID), Bytes.toBytes(parts[1].trim()));
                put.addColumn(Bytes.toBytes(column_family_cf1), Bytes.toBytes(QUALIFIER_NAME_CITYNAME), Bytes.toBytes(info.getCityName()));
                put.addColumn(Bytes.toBytes(column_family_cf1), Bytes.toBytes(QUALIFIER_NAME_CATE2), Bytes.toBytes(info.getCate2Name()));
                put.addColumn(Bytes.toBytes(column_family_cf1), Bytes.toBytes(QUALIFIER_NAME_PERPAY), Bytes.toBytes(info.getPerPay()));

                table.put(put);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //userId左侧补零，归一化为八位
    public static String userIdCompletion(String userId) {
        DecimalFormat df = new DecimalFormat("00000000");
        String userIDCom = df.format(Long.valueOf(userId));
        return userIDCom;
    }

    //时间戳保留年月日，去掉中划线
    public static String removeLine(String timestamp) {
        return timestamp.replace("-","");
    }
}
