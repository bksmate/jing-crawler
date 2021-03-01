package org.jing.crawler.ec;

import org.jing.core.lang.Carrier;
import org.jing.core.lang.ExceptionHandler;
import org.jing.core.lang.JingException;
import org.jing.core.logger.JingLogger;
import org.jing.core.util.CarrierUtil;
import org.jing.core.util.FileUtil;
import org.jing.crawler.Const;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Description: 168比价网 <br>
 *
 * @author: bks <br>
 * @createDate: 2021-03-01 <br>
 */
@SuppressWarnings("Duplicates") public class Tool168 {
    private static final JingLogger LOGGER = JingLogger.getLogger(Tool168.class);

    private static final String WEB_PREFIX = "http://www.tool168.cn/?m=history&a=view&btnSearch=搜索&k=";

    private static final String CHECK_CODE_URL = "http://www.tool168.cn/dm/ptinfo.php";

    private static final String HISTORY_URL_PREFIX = "http://www.tool168.cn/dm/history.php?code=";

    public Tool168(GoodsDto goods) throws JingException {
        String httpUrl = goods.getUrl();

        // 构造请求url
        String requestUrl = buildRequestUrl(httpUrl);

        // 请求地址
        String checkCodeId = getCheckCodeUrl(requestUrl);

        // 加载请求头
        Map<String, String> ptInfoMap = loadRequestHeader("ptinfo");
        Map<String, String> historyMap = loadRequestHeader("history");

        // 请求json
        String json = getJsonByCheckCodeId(checkCodeId, ptInfoMap, httpUrl);

        // 请求历史数据
        String jsContent = request4History(json, historyMap);
        System.out.println(jsContent);
    }

    private String buildRequestUrl(String httpUrl) throws JingException {
        try {
            String encodeUrl = URLEncoder.encode(httpUrl, "gbk");
            encodeUrl = URLEncoder.encode(encodeUrl, "gbk");
            String requestUrl = WEB_PREFIX + encodeUrl;
            LOGGER.debug("request url: {}", requestUrl);
            return requestUrl;
        }
        catch (Exception e) {
            throw new JingException(e);
        }
    }

    private String getCheckCodeUrl(String requestUrl) throws JingException {
        try {
            Document document = Jsoup.connect(requestUrl).get();
            Element element = document.getElementById("checkCodeId");
            String checkCodeId = element.attr("value");
            LOGGER.debug("check code id: {}", checkCodeId);
            return checkCodeId;
        }
        catch (Exception e) {
            throw new JingException(e);
        }
    }

    private Map<String, String> loadRequestHeader(String name) throws JingException {
        Map<String, String> headerMap = new HashMap<>();
        File headerMapFile = new File(Const.CONFIG_PATH + "/tool168/" + name + ".txt");
        if (headerMapFile.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(headerMapFile))) {
                String row;
                while ((row = br.readLine()) != null) {
                    row = row.trim();
                    if (row.startsWith(":")) {
                        row = row.substring(1);
                        headerMap.put(":" + row.substring(0, row.indexOf(":")), row.substring(row.indexOf(":") + 1).trim());
                    }
                    else {
                        headerMap.put(row.substring(0, row.indexOf(":")), row.substring(row.indexOf(":") + 1).trim());
                    }
                }
            }
            catch (Exception e) {
                throw new JingException(e);
            }
        }
        return headerMap;
    }

    private String getJsonByCheckCodeId(String checkCodeId, Map<String, String> headerMap, String httpUrl) throws JingException {
        try {
            Document document = Jsoup.connect(CHECK_CODE_URL).headers(headerMap)
                .data("checkCode", checkCodeId)
                .data("con", httpUrl)
                .post();
            String json = document.getElementsByTag("body").first().text();
            LOGGER.debug("code id: {}", json);
            return json;
        }
        catch (Exception e) {
            throw new JingException(e);
        }
    }

    private String request4History(String json, Map<String, String> headerMap) throws JingException {
        Carrier jsonC = CarrierUtil.jsonContent2Carrier(json);
        String codeId = jsonC.getString("code", "");

        ExceptionHandler.checkNull(codeId, "empty code");

        String url = HISTORY_URL_PREFIX + codeId;
        try {
            Document document = Jsoup.connect(url).headers(headerMap).get();
            return document.getElementsByTag("body").text();
        }
        catch (Exception e) {
            throw new JingException(e);
        }
    }

    public static void main(String[] args) throws JingException {
        String configPath = Const.CONFIG_PATH + "/goods.xml";
        LOGGER.imp("Read config: {}", configPath);
        Carrier goodsC = CarrierUtil.string2Carrier(FileUtil.readFile(configPath));
        int count = goodsC.getCount("goods");
        LOGGER.imp("Find {} goods", count);
        ArrayList<GoodsDto> goodsList = new ArrayList<>();
        Carrier perC;
        for (int i$ = 0; i$ < count; i$++) {
            perC = goodsC.getCarrier("goods", i$);
            goodsList.add(GoodsDto.fromC(perC));
        }
        for (GoodsDto goods : goodsList) {
            new Tool168(goods);
        }
    }
}
