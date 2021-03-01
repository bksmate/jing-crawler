package org.jing.crawler.ec;

import org.jing.core.lang.BaseDto;
import org.jing.core.lang.Carrier;
import org.jing.core.lang.JingException;

/**
 * Description: <br>
 *
 * @author: bks <br>
 * @createDate: 2021-02-26 <br>
 */
public class PriceDto extends BaseDto {
    private String recordDate;

    private float amount;

    private String event;

    private boolean flag;

    public boolean isFlag() {
        return flag;
    }

    public PriceDto setFlag(boolean flag) {
        this.flag = flag;
        return this;
    }

    public String getRecordDate() {
        return recordDate;
    }

    public PriceDto setRecordDate(String recordDate) {
        this.recordDate = recordDate;
        return this;
    }

    public float getAmount() {
        return amount;
    }

    public PriceDto setAmount(float amount) {
        this.amount = amount;
        return this;
    }

    public String getEvent() {
        return event;
    }

    public PriceDto setEvent(String event) {
        this.event = event;
        return this;
    }

    public Carrier toC() throws JingException {
        return new Carrier().setValueByKey("date", recordDate).setValueByKey("amount", amount).setValueByKey("event", event);
    }
}
