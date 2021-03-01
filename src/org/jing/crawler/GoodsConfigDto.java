package org.jing.crawler;

import org.jing.core.lang.BaseDto;
import org.jing.core.lang.Carrier;
import org.jing.core.lang.JingException;
import org.jing.core.util.StringUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Description: <br>
 *
 * @author: bks <br>
 * @createDate: 2021-02-26 <br>
 */
public class GoodsConfigDto extends BaseDto {
    private String name;

    private String id;

    private String code;

    private String url;

    private String method;

    private String merchantUrl;

    private float lower;

    private PriceDto currP, maxP, minP;

    private HashMap<String, PriceDto> eventMap;

    public String getMerchantUrl() {
        return merchantUrl;
    }

    public GoodsConfigDto setMerchantUrl(String merchantUrl) {
        this.merchantUrl = merchantUrl;
        return this;
    }

    public HashMap<String, PriceDto> getEventMap() {
        return eventMap;
    }

    public GoodsConfigDto setEventMap(HashMap<String, PriceDto> eventMap) {
        this.eventMap = eventMap;
        return this;
    }

    public PriceDto getCurrP() {
        return currP;
    }

    public GoodsConfigDto setCurrP(PriceDto currP) {
        this.currP = currP;
        return this;
    }

    public PriceDto getMaxP() {
        return maxP;
    }

    public GoodsConfigDto setMaxP(PriceDto maxP) {
        this.maxP = maxP;
        return this;
    }

    public PriceDto getMinP() {
        return minP;
    }

    public GoodsConfigDto setMinP(PriceDto minP) {
        this.minP = minP;
        return this;
    }

    private Map<String, String> headerMap = new HashMap<>();

    public float getLower() {
        return lower;
    }

    public GoodsConfigDto setLower(float lower) {
        this.lower = lower;
        return this;
    }

    public String getName() {
        return name;
    }

    public GoodsConfigDto setName(String name) {
        this.name = name;
        return this;
    }

    public String getId() {
        return id;
    }

    public GoodsConfigDto setId(String id) {
        this.id = id;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public GoodsConfigDto setUrl(String url) {
        this.url = url;
        return this;
    }

    public String getMethod() {
        return method;
    }

    public GoodsConfigDto setMethod(String method) {
        this.method = method;
        return this;
    }

    public String getCode() {
        return code;
    }

    public GoodsConfigDto setCode(String code) {
        this.code = code;
        return this;
    }

    public Map<String, String> getHeaderMap() {
        return headerMap;
    }

    public GoodsConfigDto setHeaderMap(Map<String, String> headerMap) {
        this.headerMap = headerMap;
        return this;
    }

    public static GoodsConfigDto fromC(Carrier c) throws JingException {
        GoodsConfigDto config = new GoodsConfigDto();
        config.setName(c.getString("name", ""));
        config.setId(c.getString("id", ""));
        config.setCode(c.getString("code", ""));
        config.setMethod(c.getString("method", ""));
        config.setLower(StringUtil.parseFloat(c.getString("lower", "")));
        config.setMerchantUrl(c.getString("merchantUrl", ""));

        String id = config.getId();
        File headerMapFile = new File(String.format(Const.CONFIG_PATH + "/headers/%s.txt", id));
        if (!headerMapFile.exists()) {
            headerMapFile = new File(Const.CONFIG_PATH + "/requestHeader.txt");
        }
        if (headerMapFile.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(headerMapFile))) {
                String row;
                while ((row = br.readLine()) != null) {
                    row = row.trim();
                    if (row.startsWith(":")) {
                        row = row.substring(1);
                        config.headerMap
                            .put(":" + row.substring(0, row.indexOf(":")), row.substring(row.indexOf(":") + 1).trim());
                    }
                    else {
                        config.headerMap.put(row.substring(0, row.indexOf(":")), row.substring(row.indexOf(":") + 1).trim());
                    }
                }
            }
            catch (Exception e) {
                throw new JingException(e);
            }
        }

        config.setUrl(Const.URL + "code=" + config.getCode());

        return config;
    }
}
