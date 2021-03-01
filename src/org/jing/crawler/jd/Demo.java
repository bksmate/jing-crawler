package org.jing.crawler.jd;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.jing.core.lang.Carrier;
import org.jing.core.lang.ExceptionHandler;
import org.jing.core.lang.JingException;
import org.jing.core.logger.JingLogger;
import org.jing.core.util.CarrierUtil;
import org.jing.core.util.FileUtil;
import org.jing.core.util.GenericUtil;
import org.jing.core.util.StringUtil;
import org.jing.crawler.Const;
import org.jing.crawler.GoodsConfigDto;
import org.jing.crawler.PriceDto;
import org.jing.ext.office.OfficeUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.awt.*;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Description: <br>
 *
 * @author: bks <br>
 * @createDate: 2021-02-26 <br>
 */
public class Demo {
    private static final JingLogger LOGGER = JingLogger.getLogger(Demo.class);

    private ArrayList<GoodsConfigDto> configList;

    private Demo() throws Exception {
        // 读取配置
        configList = readConfig();

        // 逐个请求
        ArrayList<PriceDto> infoList = new ArrayList<>();
        Carrier hikariC = new Carrier("hikari");
        Carrier retC;
        for (GoodsConfigDto config : configList) {
            retC = request(config, infoList);
            if (null != retC) {
                hikariC.addValueByKey("goods", retC);
            }
            else {
                LOGGER.imp("Failed to query {}", config.getName());
            }
        }

        // 输出到文件
        LOGGER.imp("Output...");
        writeExcel();
        /*String outputPath = goodsC.getString("output", "output.xml");
        FileUtil.writeFile(outputPath, allC.asXML());

        URI uri = new URI("file:///" + outputPath.replaceAll("\\\\", "/"));
        Desktop.getDesktop().browse(uri);*/

    }

    private ArrayList<GoodsConfigDto> readConfig() throws JingException {
        String configPath = Const.CONFIG_PATH + "/goods.xml";
        LOGGER.imp("Read config: {}", configPath);
        Carrier goodsC = CarrierUtil.string2Carrier(FileUtil.readFile(configPath));
        int count = goodsC.getCount("goods");
        LOGGER.imp("Find {} goods", count);
        ArrayList<GoodsConfigDto> retList = new ArrayList<>();
        Carrier perC;
        for (int i$ = 0; i$ < count; i$++) {
            perC = goodsC.getCarrier("goods", i$);
            retList.add(GoodsConfigDto.fromC(perC));
        }

        return retList;
    }

    private Carrier request(GoodsConfigDto config, ArrayList<PriceDto> infoList) throws JingException {
        try {
            LOGGER.imp("Request for {}", config.getName());
            Document document;
            if ("post".equalsIgnoreCase(config.getMethod())) {
                document = Jsoup.connect(config.getUrl()).headers(config.getHeaderMap()).post();
            }
            else {
                document = Jsoup.connect(config.getUrl()).headers(config.getHeaderMap()).get();
            }
            PriceDto minP = null, maxP = null, currP = null;
            HashMap<String, PriceDto> eventMap = new HashMap<>();
            String url;
            Carrier retC;
            String fullName;
            String dataList;

            Elements elements = document.getElementsByTag("body");
            Element element = elements.get(0);
            String jsContent = element.text();
            Pattern p$1 = Pattern.compile("\\$\\(\"#titleId\"\\)\\.html\\(\"(.*?)\"\\);");
            Matcher m$1 = p$1.matcher(jsContent);

            ExceptionHandler.publishIfMatch(!m$1.find(), "Invalid php response: cannot find fullName");
            fullName = m$1.group(1);

            Pattern p$2 = Pattern.compile("chart\\('(.*)', 'https:.*?', 'pc'\\)");
            Matcher m$2 = p$2.matcher(jsContent);

            ExceptionHandler.publishIfMatch(!m$2.find(), "Invalid php response: cannot find dataList");
            dataList = m$2.group(1);

            Pattern p$3 = Pattern.compile("\\[Date.UTC\\((\\d+?).(\\d+?).(\\d+?)\\),(.*?),\"(.*?)\"]");
            Matcher m$3 = p$3.matcher(dataList);
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
                return null;
            }

            retC = new Carrier();
            retC.setValueByKey("name", config.getName());
            retC.setValueByKey("fullName", fullName);
            retC.setValueByKey("min", minP.toC());
            retC.setValueByKey("max", maxP.toC());
            retC.setValueByKey("now", currP.toC());
            retC.setValueByKey("lower", config.getLower());
            for (Map.Entry<String, PriceDto> p$ : eventMap.entrySet()) {
                retC.addValueByKey("event", p$.getValue().toC());
            }
            // 添加醒目标志
            currP.setFlag(config.getLower() >= currP.getAmount() || currP.getAmount() == minP.getAmount());
            infoList.add(currP);

            config.setCurrP(currP);
            config.setMinP(minP);
            config.setMaxP(maxP);
            config.setEventMap(eventMap);

            return retC;
        }
        catch (Exception e) {
            throw new JingException(e);
        }
    }

    private void writeExcel() throws Exception {
        File excelFile = new File("logs/temp.xlsx");
        Workbook excel = OfficeUtil.createExcel(excelFile);
        Sheet sheet = excel.createSheet("sheet1");
        short stringFormat = OfficeUtil.createDataFormat(excel, "@");
        OfficeUtil.CellStyle headerStyle = OfficeUtil.createCellStyle(excel)
            .setFontNameAndSize(OfficeUtil.FontName.YaHei, 10).setBold(true)
            .setAlignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER).setCellFormat(stringFormat)
            .setAllBorder(BorderStyle.MEDIUM);
        OfficeUtil.CellStyle childStyle = OfficeUtil.createCellStyle(excel)
            .setFontNameAndSize(OfficeUtil.FontName.YaHei, 10)
            .setAlignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER).setCellFormat(stringFormat)
            .setLeftBorder(BorderStyle.THIN).setRightBorder(BorderStyle.THIN).setBottomBorder(BorderStyle.THIN)
            .setWrap(true);
        int rowIndex = -1;
        List<String> columnList = GenericUtil.initList(new String[] { "商品名", "最新", "最低", "最高" });
        int count = Const.EVENT_DAYS.size();
        for (int i$ = 0; i$ < count; i$ ++) {
            columnList.add(Const.EVENT_DAYS.get(i$));
        }
        columnList.add("关注价格");
        columnList.add("链接");

        int colCount = columnList.size();
        int[] maxWidthArr = new int[colCount];
        maxWidthArr[0] = 50;
        sheet.setColumnWidth(0, maxWidthArr[0]);
        maxWidthArr[1] = 5;
        sheet.setColumnWidth(1, maxWidthArr[1]);
        for (int i$ = 2; i$ < colCount - 1; i$++) {
            maxWidthArr[i$] = 30;
            sheet.setColumnWidth(i$, maxWidthArr[i$]);
        }
        rowIndex++;
        int colIndex;
        Row row = sheet.createRow(rowIndex);
        row.setHeight((short) 700);

        for (int i$ = 0; i$ < colCount; i$ ++) {
            if (i$ == 0) {
                OfficeUtil.mergeCell(sheet, 0, 0, 0, 1);
                headerStyle.add2Cell(row.createCell(i$)).getBase().setCellValue(columnList.get(i$));
            }
            headerStyle.add2Cell(row.createCell(i$ + 1)).getBase().setCellValue(columnList.get(i$));
        }

        int rowIndex$1, rowIndex$2, rowIndex$3;
        Row row$1, row$2, row$3;
        PriceDto priceDto;
        count = configList.size();
        GoodsConfigDto config;
        Cell titleCell;
        for (int i$ = 0; i$ < count; ++i$) {
            config = configList.get(i$);
            rowIndex$1 = i$ * 3 + 1;
            rowIndex$2 = i$ * 3 + 2;
            rowIndex$3 = i$ * 3 + 3;
            OfficeUtil.mergeCell(sheet, rowIndex$1, rowIndex$3, 0, 0);
            row$1 = sheet.createRow(rowIndex$1);
            row$1.setHeight((short) 300);
            row$2 = sheet.createRow(rowIndex$2);
            row$2.setHeight((short) 300);
            row$3 = sheet.createRow(rowIndex$3);
            row$3.setHeight((short) 1000);
            childStyle.add2Cell(row$1.createCell(0)).getBase().setCellValue(recordMaxWidth(maxWidthArr, config.getName(), 0));
            childStyle.add2Cell(row$2.createCell(0));
            childStyle.add2Cell(row$3.createCell(0));
            titleCell = row$1.getCell(0);
            childStyle.add2Cell(row$1.createCell(1)).getBase().setCellValue("日期");
            childStyle.add2Cell(row$2.createCell(1)).getBase().setCellValue("价格");
            childStyle.add2Cell(row$3.createCell(1)).getBase().setCellValue("活动");
            // 最新
            childStyle.add2Cell(row$1.createCell(2)).getBase().setCellValue(config.getCurrP().getRecordDate());
            childStyle.add2Cell(row$2.createCell(2)).getBase().setCellValue(String.format("%.2f", config.getCurrP().getAmount()));
            childStyle.add2Cell(row$3.createCell(2)).getBase().setCellValue(recordMaxWidth(maxWidthArr, config.getCurrP().getEvent(), 2));
            // 最低
            childStyle.add2Cell(row$1.createCell(3)).getBase().setCellValue(config.getMinP().getRecordDate());
            childStyle.add2Cell(row$2.createCell(3)).getBase().setCellValue(String.format("%.2f", config.getMinP().getAmount()));
            childStyle.add2Cell(row$3.createCell(3)).getBase().setCellValue(recordMaxWidth(maxWidthArr, config.getMinP().getEvent(), 3));
            // 最高
            childStyle.add2Cell(row$1.createCell(4)).getBase().setCellValue(config.getMaxP().getRecordDate());
            childStyle.add2Cell(row$2.createCell(4)).getBase().setCellValue(String.format("%.2f", config.getMaxP().getAmount()));
            childStyle.add2Cell(row$3.createCell(4)).getBase().setCellValue(recordMaxWidth(maxWidthArr, config.getMaxP().getEvent(), 4));
            // 电商节
            colIndex = 4;
            for (String eventDay: Const.EVENT_DAYS) {
                colIndex ++;
                priceDto = config.getEventMap().get(eventDay);
                childStyle.add2Cell(row$1.createCell(colIndex)).getBase().setCellValue(null != priceDto ? priceDto.getRecordDate() : "");
                childStyle.add2Cell(row$2.createCell(colIndex)).getBase().setCellValue(null != priceDto ? String.format("%.2f", priceDto.getAmount()) : "");
                childStyle.add2Cell(row$3.createCell(colIndex)).getBase().setCellValue(recordMaxWidth(maxWidthArr, null != priceDto ? priceDto.getEvent() : "", colIndex));
            }
            // 关注价格
            colIndex ++;
            OfficeUtil.mergeCell(sheet, rowIndex$1, rowIndex$3, colIndex, colIndex);
            childStyle.add2Cell(row$1.createCell(colIndex)).getBase().setCellValue(recordMaxWidth(maxWidthArr, String.format("%.2f", config.getLower()), 0));
            childStyle.add2Cell(row$2.createCell(colIndex));
            childStyle.add2Cell(row$3.createCell(colIndex));
            // 链接
            colIndex ++;
            OfficeUtil.mergeCell(sheet, rowIndex$1, rowIndex$3, colIndex, colIndex);
            childStyle.add2Cell(row$1.createCell(colIndex)).getBase().setCellValue(recordMaxWidth(maxWidthArr, config.getMerchantUrl(), 0));
            childStyle.add2Cell(row$2.createCell(colIndex));
            childStyle.add2Cell(row$3.createCell(colIndex));
            if (null != config.getCurrP() && (config.getCurrP().getAmount() <= config.getMinP().getAmount() || config.getCurrP().getAmount() <= config.getLower())) {
                titleCell.setCellValue("【醒目】" + titleCell.getStringCellValue());
            }
        }


        for (int i$ = 0; i$ < colCount; ++i$) {
            if (maxWidthArr[i$] != 0) {
                sheet.setColumnWidth(i$, Math.min(12000, maxWidthArr[i$] * 256));
            }
        }

        OfficeUtil.writeExcel(excel, excelFile);
        URI uri = new URI("file:///" + excelFile.getAbsolutePath().replaceAll("\\\\", "/"));
        Desktop.getDesktop().browse(uri);
    }

    private String recordMaxWidth(int[] maxWidthArr, String content, int index) throws JingException {
        try {
            int length = content.getBytes("GBK").length;
            if (maxWidthArr[index] == 0 || maxWidthArr[index] < length) {
                maxWidthArr[index] = length;
            }
            return content;
        }
        catch (Exception e) {
            throw new JingException(e);
        }
    }

    public static void main(String[] args) throws Exception {
        new Demo();
    }
}
