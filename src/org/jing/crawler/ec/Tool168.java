package org.jing.crawler.ec;

import org.jing.core.lang.ExceptionHandler;
import org.jing.core.lang.JingException;
import org.jing.core.logger.JingLogger;
import org.jing.core.util.StringUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        // 解析js
        analyzeJavaScript(jsContent, goods);
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
        Pattern pattern = Pattern.compile("\"code\":\"(.*?)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            String codeId = matcher.group(1);
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
        else {
            throw new JingException("Invalid code");
        }
    }

    private void analyzeJavaScript(String jsContent, GoodsDto goods) throws JingException {
        Pattern p$1 = Pattern.compile("\\$\\(\"#titleId\"\\)\\.html\\(\"(.*?)\"\\);");
        Matcher m$1 = p$1.matcher(jsContent);

        ExceptionHandler.publishIfMatch(!m$1.find(), "Invalid php response: cannot find fullName");
        String fullName = m$1.group(1);

        Pattern p$2 = Pattern.compile("chart\\('(.*)', 'https:.*?', 'pc'\\)");
        Matcher m$2 = p$2.matcher(jsContent);

        ExceptionHandler.publishIfMatch(!m$2.find(), "Invalid php response: cannot find dataList");
        String dataList = m$2.group(1);

        Pattern p$3 = Pattern.compile("\\[Date.UTC\\((\\d+?).(\\d+?).(\\d+?)\\),(.*?),\"(.*?)\"]");
        Matcher m$3 = p$3.matcher(dataList);

        PriceDto currP = null, maxP = null, minP = null;
        HashMap<String, PriceDto> eventMap = new HashMap<>();
        while (m$3.find()) {
            currP = new PriceDto().setRecordDate(
                String.format("%s-%s-%s", m$3.group(1), String.format("%02d", StringUtil.parseInteger(m$3.group(2)) + 1), String.format("%02d", StringUtil.parseInteger(m$3.group(3)))))
                .setAmount(StringUtil.parseFloat(m$3.group(4))).setEvent(m$3.group(5));
            if (minP == null || minP.getAmount() >= currP.getAmount()) {
                minP = currP;
            }
            if (maxP == null || maxP.getAmount() <= currP.getAmount()) {
                maxP = currP;
            }
            for (String keyDay : Const.EVENT_DAYS) {
                if (currP.getRecordDate().endsWith(keyDay)) {
                    eventMap.put(keyDay, currP);
                }
            }
        }

        if (null == currP) {
            return;
        }

        goods.setFullName(fullName);
        goods.setCurrP(currP);
        goods.setMaxP(maxP);
        goods.setMinP(minP);
        goods.setEventMap(eventMap);
    }
}
