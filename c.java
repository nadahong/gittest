package net.g1project.ecp.order.vo.business;

import java.math.BigDecimal;

import junit.framework.TestCase;
import net.g1project.bluewave.core.exception.G1IllegalArgumentException;
import net.g1project.bluewave.core.util.G1AmountUtils.AmountDecimalType;
import net.g1project.bluewave.core.util.G1AmountUtils.TaxType;

public class OrderItemBizVOTest extends TestCase {

    double taxRate = 0.08;

    public void testTax() {
        OrderItemBizVO item = new OrderItemBizVO();
        item.setTaxRate(0.0);

        // assertEquals(item.getTaxRate(), 0.0);

        // System.out.println(item.getTaxRate());

        Double priceIncludingTax = getPriceIncludingTax(6.2, taxRate, TaxType.TAX_EXCLUSION, AmountDecimalType.DECIMAL_TRUE);
        Double priceExcludingTax = getPriceExcludingTax(6.2, taxRate, TaxType.TAX_EXCLUSION, AmountDecimalType.DECIMAL_TRUE);
        // System.out.println("##### 1");
        // System.out.println(priceIncludingTax);
        // System.out.println(priceExcludingTax);
        // System.out.println(BigDecimal.valueOf(priceIncludingTax).subtract(BigDecimal.valueOf(priceExcludingTax)));

        Double priceIncludingTax2 = getPriceIncludingTax(6.2, taxRate, TaxType.TAX_INCLUSION, AmountDecimalType.DECIMAL_TRUE);
        Double priceExcludingTax2 = getPriceExcludingTax(6.2, taxRate, TaxType.TAX_INCLUSION, AmountDecimalType.DECIMAL_TRUE);
        // System.out.println("##### 2");
        // System.out.println(priceIncludingTax2);
        // System.out.println(priceExcludingTax2);
        // System.out.println(BigDecimal.valueOf(priceIncludingTax2).subtract(BigDecimal.valueOf(priceExcludingTax2)));

        Double priceIncludingTax3 = getPriceIncludingTax(6.2, taxRate, TaxType.TAX_FREE, AmountDecimalType.DECIMAL_TRUE);
        Double priceExcludingTax3 = getPriceExcludingTax(6.2, taxRate, TaxType.TAX_FREE, AmountDecimalType.DECIMAL_TRUE);
        // System.out.println("##### 3");
        // System.out.println(priceIncludingTax3);
        // System.out.println(priceExcludingTax3);
        // System.out.println(BigDecimal.valueOf(priceIncludingTax3).subtract(BigDecimal.valueOf(priceExcludingTax3)));

        // System.out.println(new OrderBizVO());
        // System.out.println(new OrderItemBizVO());
    }

    /**
     * <p>
     * 세금포함가격
     * </p>
     * 
     * <pre>
     * 세금포함금액을 구한다.
     * price including tax
     * 税込み価格
     * 
     * <pre>
     *
     * @param price - 가격
     * @param taxRate - 세율 8% -> 0.08 입력 
     * @param taxType - 입력가격의 세금타입
     * @param decimalType - 금액에 소숫점 사용유무, 센트를 사용하는 나라는 DECIMAL_TRUE 설정.
     * @return 세금포함가격
     */
    public static Double getPriceIncludingTax(
            final Double price, final Double taxRate, final TaxType taxType, final AmountDecimalType decimalType
            ) {

        Double result = 0.0;
        BigDecimal standardDecimal = BigDecimal.ONE;
        int scale = 0;
        if (AmountDecimalType.DECIMAL_TRUE.equals(decimalType)) {
            standardDecimal = new BigDecimal("100");
            scale = 2;
        }
        try {
            if (TaxType.TAX_EXCLUSION.equals(taxType)) {
                BigDecimal tmpPrice = BigDecimal.valueOf(price).multiply(standardDecimal);
                BigDecimal tmpTaxRate = BigDecimal.ONE.add(BigDecimal.valueOf(taxRate));
                tmpPrice = tmpPrice.multiply(tmpTaxRate).divide(standardDecimal, scale, BigDecimal.ROUND_DOWN);
                result = tmpPrice.doubleValue();
            } else {
                result = price;
            }
        } catch (Exception e) {
            throw new G1IllegalArgumentException(e.getMessage());
        }

        return result;
    }

    /**
     * <p>
     * 세금제외가격
     * </p>
     * 
     * <pre>
     * 세금을 포함하지 않은 금액을 구한다.
     * price excluding tax
     * 税抜き価格
     * 
     * <pre>
     *
     * @param price - 가격
     * @param taxRate - 세율 8% -> 0.08 입력 
     * @param taxType - 입력가격의 세금타입
     * @param decimalType - 금액에 소숫점 사용유무, 센트를 사용하는 나라는 DECIMAL_TRUE 설정.
     * @return 세금제외가격
     */
    public static Double getPriceExcludingTax(
            final Double price, final Double taxRate, final TaxType taxType, final AmountDecimalType decimalType
            ) {

        Double result = 0.0;
        BigDecimal standardDecimal = BigDecimal.ONE;
        int scale = 0;
        if (AmountDecimalType.DECIMAL_TRUE.equals(decimalType)) {
            standardDecimal = new BigDecimal("100");
            scale = 2;
        }
        try {
            if (TaxType.TAX_INCLUSION.equals(taxType)) {
                BigDecimal tmpPrice = BigDecimal.valueOf(price).multiply(standardDecimal);
                BigDecimal tmpTaxRate = BigDecimal.ONE.add(BigDecimal.valueOf(taxRate));
                tmpPrice = tmpPrice.divide(standardDecimal).divide(tmpTaxRate, scale, BigDecimal.ROUND_UP);
                result = tmpPrice.doubleValue();
            } else {
                result = price;
            }
        } catch (Exception e) {
            throw new G1IllegalArgumentException(e.getMessage(), e);
        }

        return result;
    }
}




