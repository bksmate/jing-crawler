package org.jing.crawler.ec;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.jing.core.lang.Carrier;
import org.jing.core.lang.JingException;
import org.jing.core.logger.JingLogger;
import org.jing.core.logger.JingLoggerConfiguration;
import org.jing.core.logger.JingLoggerLevel;
import org.jing.core.util.CarrierUtil;
import org.jing.core.util.DateUtil;
import org.jing.core.util.FileUtil;
import org.jing.core.util.GenericUtil;
import org.jing.ext.office.OfficeUtil;

import java.awt.*;
import java.io.File;
import java.lang.Exception;
import java.math.BigInteger;
import java.net.URI;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Description: <br>
 *
 * @author: bks <br>
 * @createDate: 2021-03-01 <br>
 */
@SuppressWarnings("Duplicates") public class Main {
    private static final JingLogger LOGGER = JingLogger.getLogger(Main.class);

    private Carrier goodsC;

    private Main() throws Exception {
        ArrayList<GoodsDto> goodsList = readGoodsList();

        operateGoodsList(goodsList);

        File excelFile = preOperate4ExcelFile();

        writeExcel(goodsList, excelFile);
    }

    private ArrayList<GoodsDto> readGoodsList() throws JingException {
        String configPath = Const.CONFIG_PATH + "/goods.xml";
        LOGGER.imp("Read config: {}", configPath);
        goodsC = CarrierUtil.string2Carrier(FileUtil.readFile(configPath));
        int count = goodsC.getCount("goods");
        LOGGER.imp("Find {} goods", count);
        ArrayList<GoodsDto> goodsList = new ArrayList<>();
        Carrier perC;
        for (int i$ = 0; i$ < count; i$++) {
            perC = goodsC.getCarrier("goods", i$);
            goodsList.add(GoodsDto.fromC(perC));
        }
        return goodsList;
    }

    private void operateGoodsList(ArrayList<GoodsDto> goodsList) throws JingException {
        for (GoodsDto goods : goodsList) {
            new Tool168(goods);
        }
    }

    private File preOperate4ExcelFile() throws JingException {
        File parentFile = new File(goodsC.getString("output", "output"));
        if (parentFile.exists() || parentFile.mkdirs()) {
            File excelFile = new File(parentFile.getAbsolutePath() + File.separator + DateUtil.getCurrentDateString("yyyyMMdd") + ".xlsx");
            if (excelFile.exists() && !excelFile.delete()) {
                throw new JingException("failed to delete old file: {}", excelFile.getAbsolutePath());
            }
            return excelFile;
        }
        else {
            throw new JingException("Failed to mkdirs");
        }
    }

    private void writeExcel(ArrayList<GoodsDto> goodsList, File excelFile) throws Exception {
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
        count = goodsList.size();
        GoodsDto goods;
        Cell titleCell;
        for (int i$ = 0; i$ < count; ++i$) {
            goods = goodsList.get(i$);
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
            childStyle.add2Cell(row$1.createCell(0)).getBase().setCellValue(recordMaxWidth(maxWidthArr, goods.getName(), 0));
            childStyle.add2Cell(row$2.createCell(0));
            childStyle.add2Cell(row$3.createCell(0));
            titleCell = row$1.getCell(0);
            childStyle.add2Cell(row$1.createCell(1)).getBase().setCellValue("日期");
            childStyle.add2Cell(row$2.createCell(1)).getBase().setCellValue("价格");
            childStyle.add2Cell(row$3.createCell(1)).getBase().setCellValue("活动");
            // 最新
            childStyle.add2Cell(row$1.createCell(2)).getBase().setCellValue(goods.getCurrP().getRecordDate());
            childStyle.add2Cell(row$2.createCell(2)).getBase().setCellValue(String.format("%.2f", goods.getCurrP().getAmount()));
            childStyle.add2Cell(row$3.createCell(2)).getBase().setCellValue(recordMaxWidth(maxWidthArr, goods.getCurrP().getEvent(), 2));
            // 最低
            childStyle.add2Cell(row$1.createCell(3)).getBase().setCellValue(goods.getMinP().getRecordDate());
            childStyle.add2Cell(row$2.createCell(3)).getBase().setCellValue(String.format("%.2f", goods.getMinP().getAmount()));
            childStyle.add2Cell(row$3.createCell(3)).getBase().setCellValue(recordMaxWidth(maxWidthArr, goods.getMinP().getEvent(), 3));
            // 最高
            childStyle.add2Cell(row$1.createCell(4)).getBase().setCellValue(goods.getMaxP().getRecordDate());
            childStyle.add2Cell(row$2.createCell(4)).getBase().setCellValue(String.format("%.2f", goods.getMaxP().getAmount()));
            childStyle.add2Cell(row$3.createCell(4)).getBase().setCellValue(recordMaxWidth(maxWidthArr, goods.getMaxP().getEvent(), 4));
            // 电商节
            colIndex = 4;
            for (String eventDay: Const.EVENT_DAYS) {
                colIndex ++;
                priceDto = goods.getEventMap().get(eventDay);
                childStyle.add2Cell(row$1.createCell(colIndex)).getBase().setCellValue(null != priceDto ? priceDto.getRecordDate() : "");
                childStyle.add2Cell(row$2.createCell(colIndex)).getBase().setCellValue(null != priceDto ? String.format("%.2f", priceDto.getAmount()) : "");
                childStyle.add2Cell(row$3.createCell(colIndex)).getBase().setCellValue(recordMaxWidth(maxWidthArr, null != priceDto ? priceDto.getEvent() : "", colIndex));
            }
            // 关注价格
            colIndex ++;
            OfficeUtil.mergeCell(sheet, rowIndex$1, rowIndex$3, colIndex, colIndex);
            childStyle.add2Cell(row$1.createCell(colIndex)).getBase().setCellValue(recordMaxWidth(maxWidthArr, String.format("%.2f", goods.getFocus()), 0));
            childStyle.add2Cell(row$2.createCell(colIndex));
            childStyle.add2Cell(row$3.createCell(colIndex));
            // 链接
            colIndex ++;
            OfficeUtil.mergeCell(sheet, rowIndex$1, rowIndex$3, colIndex, colIndex);
            childStyle.add2Cell(row$1.createCell(colIndex)).getBase().setCellValue(recordMaxWidth(maxWidthArr, goods.getUrl(), 0));
            childStyle.add2Cell(row$2.createCell(colIndex));
            childStyle.add2Cell(row$3.createCell(colIndex));
            if (null != goods.getCurrP() && (goods.getCurrP().getAmount() <= goods.getMinP().getAmount() || goods.getCurrP().getAmount() <= goods.getFocus())) {
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
        LOGGER.fatal("please enter the password:");
        Scanner scanner = new Scanner(System.in);
        if (scanner.hasNext()) {
            String password = scanner.next();
            byte[] secretBytes;
            try {
                secretBytes = MessageDigest.getInstance("md5").digest(password.getBytes());
            } catch (Exception e) {
                throw new JingException(e, "md5 no found");
            }
            String md5code = new BigInteger(1, secretBytes).toString(16);
            String md5After =
                String.valueOf(md5code.charAt(0)) + md5code.charAt(31) + md5code.charAt(1) + md5code.charAt(30)
                    + md5code.charAt(2) + md5code.charAt(29);
            if ("8204ec".equalsIgnoreCase(md5After)) {
                new Main();
            }
            else {
                throw new JingException("invalid password");
            }
        }
    }
}
