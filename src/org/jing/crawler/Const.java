package org.jing.crawler;

import org.jing.core.util.GenericUtil;

import java.util.List;

/**
 * Description: <br>
 *
 * @author: bks <br>
 * @createDate: 2021-02-26 <br>
 */
public class Const {
    public static final String CONFIG_PATH = "config";

    public static final String URL = "http://www.Tool168.cn/dm/history.php?";

    public static final List<String> EVENT_DAYS = GenericUtil.initList(new String[]{
        "06-18", "11-11", "12-12"
    });
}
