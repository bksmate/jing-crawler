package org.jing.crawler.ec;

import org.jing.core.lang.BaseDto;
import org.jing.core.lang.Carrier;
import org.jing.core.lang.JingException;
import org.jing.core.util.StringUtil;

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
