package org.jing.crawler.ec;

import org.jing.core.lang.BaseDto;
import org.jing.core.lang.Carrier;
import org.jing.core.lang.JingException;
import org.jing.core.util.StringUtil;

import java.util.HashMap;

/**
 * Description: <br>
 *
 * @author: bks <br>
 * @createDate: 2021-03-01 <br>
 */
public class GoodsDto extends BaseDto {
    private String name;

    private String url;

    private float focus;

    private String fullName;

    private PriceDto currP, maxP, minP;

    private HashMap<String, PriceDto> eventMap;

    public PriceDto getCurrP() {
        return currP;
    }

    public void setCurrP(PriceDto currP) {
        this.currP = currP;
    }

    public PriceDto getMaxP() {
        return maxP;
    }

    public void setMaxP(PriceDto maxP) {
        this.maxP = maxP;
    }

    public PriceDto getMinP() {
        return minP;
    }

    public void setMinP(PriceDto minP) {
        this.minP = minP;
    }

    public HashMap<String, PriceDto> getEventMap() {
        return eventMap;
    }

    public void setEventMap(HashMap<String, PriceDto> eventMap) {
        this.eventMap = eventMap;
    }

    public String getName() {
        return name;
    }

    public GoodsDto setName(String name) {
        this.name = name;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public GoodsDto setUrl(String url) {
        this.url = url;
        return this;
    }

    public float getFocus() {
        return focus;
    }

    public GoodsDto setFocus(float focus) {
        this.focus = focus;
        return this;
    }

    public String getFullName() {
        return fullName;
    }

    public GoodsDto setFullName(String fullName) {
        this.fullName = fullName;
        return this;
    }

    public static GoodsDto fromC(Carrier configC) throws JingException {
        GoodsDto goodsDto = new GoodsDto();

        goodsDto.name = configC.getString("name");
        goodsDto.url = configC.getString("url");
        goodsDto.focus = StringUtil.parseFloat(configC.getString("focus"));

        return goodsDto;
    }
}
