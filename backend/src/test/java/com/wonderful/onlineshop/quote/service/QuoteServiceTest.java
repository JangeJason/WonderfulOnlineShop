package com.wonderful.onlineshop.quote.service;

import com.wonderful.onlineshop.product.entity.ParameterOption;
import com.wonderful.onlineshop.product.entity.Product;
import com.wonderful.onlineshop.product.entity.ProductParameter;
import com.wonderful.onlineshop.product.service.ProductParameterService;
import com.wonderful.onlineshop.product.service.ProductService;
import com.wonderful.onlineshop.quote.dto.QuoteRequest;
import com.wonderful.onlineshop.quote.dto.QuoteRequestItem;
import com.wonderful.onlineshop.quote.dto.QuoteResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuoteServiceTest {

    @Mock
    private ProductService productService;

    @Mock
    private ProductParameterService parameterService;

    private QuoteService quoteService;

    @BeforeEach
    void setUp() {
        quoteService = new QuoteService(productService, parameterService);
    }

    @Test
    void quote_shouldCalculateFormulaOptionAndSetupFee() {
        Product product = product(1L, "10.00", "#L * #W * 0.1", "30.00", 100);

        ProductParameter p1 = inputParam(11L, 1L, "L");
        ProductParameter p2 = inputParam(12L, 1L, "W");
        ProductParameter p3 = selectParam(13L, 1L, "材质");

        ParameterOption opt = option(101L, 13L, "铜版纸", "5.00");

        when(productService.getActiveRequired(1L)).thenReturn(product);
        when(parameterService.getParametersByProductId(1L)).thenReturn(List.of(p1, p2, p3));
        when(parameterService.getOptionsByParamId(13L)).thenReturn(List.of(opt));

        QuoteRequest request = new QuoteRequest(
                1L,
                50,
                List.of(
                        new QuoteRequestItem(11L, "L", new BigDecimal("20"), null),
                        new QuoteRequestItem(12L, "W", new BigDecimal("10"), null),
                        new QuoteRequestItem(13L, "材质", null, List.of(101L))
                )
        );

        QuoteResponse response = quoteService.quote(request);

        assertAmount("35.00", response.unitPrice());
        assertAmount("1780.00", response.totalPrice());
        assertAmount("10.00", response.breakdown().get("基础价"));
        Map.Entry<String, BigDecimal> formulaEntry = response.breakdown().entrySet().stream()
                .filter(e -> e.getKey().startsWith("计价公式"))
                .findFirst()
                .orElseThrow();
        assertAmount("20.00", formulaEntry.getValue());
        assertAmount("5.00", response.breakdown().get("材质（铜版纸）"));
        assertAmount("30.00", response.breakdown().get("setupFee"));
    }

    @Test
    void quote_shouldIgnoreInvalidFormulaAndSkipSetupFeeWhenThresholdMet() {
        Product product = product(2L, "100.00", "#QTY *", "20.00", 10);
        ProductParameter qty = inputParam(21L, 2L, "QTY");

        when(productService.getActiveRequired(2L)).thenReturn(product);
        when(parameterService.getParametersByProductId(2L)).thenReturn(List.of(qty));

        QuoteRequest request = new QuoteRequest(
                2L,
                10,
                List.of(new QuoteRequestItem(21L, "QTY", new BigDecimal("5"), null))
        );

        QuoteResponse response = quoteService.quote(request);

        assertAmount("100.00", response.unitPrice());
        assertAmount("1000.00", response.totalPrice());
        assertAmount("0", response.breakdown().get("setupFee"));
        assertTrue(response.breakdown().keySet().stream().noneMatch(k -> k.startsWith("计价公式")));
    }

    @Test
    void quote_shouldSumMultiSelectOptionsAndRoundHalfUp() {
        Product product = product(3L, "10.005", null, "0.00", 0);
        ProductParameter craft = selectParam(31L, 3L, "工艺");

        ParameterOption o1 = option(301L, 31L, "覆膜", "1.20");
        ParameterOption o2 = option(302L, 31L, "烫金", "-0.40");

        when(productService.getActiveRequired(3L)).thenReturn(product);
        when(parameterService.getParametersByProductId(3L)).thenReturn(List.of(craft));
        when(parameterService.getOptionsByParamId(31L)).thenReturn(List.of(o1, o2));

        QuoteRequest request = new QuoteRequest(
                3L,
                3,
                List.of(new QuoteRequestItem(31L, "工艺", null, List.of(301L, 302L)))
        );

        QuoteResponse response = quoteService.quote(request);

        // 10.005 + (1.20 - 0.40) = 10.805 -> 10.81
        assertAmount("10.81", response.unitPrice());
        assertAmount("32.43", response.totalPrice());
        assertNotNull(response.breakdown().get("工艺（覆膜，烫金）"));
        assertAmount("0.80", response.breakdown().get("工艺（覆膜，烫金）"));
    }

    private static Product product(Long id, String basePrice, String formula, String setupFee, Integer freeSetupQuantity) {
        Product p = new Product();
        p.setId(id);
        p.setBasePrice(new BigDecimal(basePrice));
        p.setPricingFormula(formula);
        p.setSetupFee(new BigDecimal(setupFee));
        p.setFreeSetupQuantity(freeSetupQuantity);
        p.setStatus(1);
        return p;
    }

    private static ProductParameter inputParam(Long id, Long productId, String name) {
        ProductParameter p = new ProductParameter();
        p.setId(id);
        p.setProductId(productId);
        p.setParamName(name);
        p.setParamType("INPUT");
        p.setRequired(true);
        return p;
    }

    private static ProductParameter selectParam(Long id, Long productId, String name) {
        ProductParameter p = new ProductParameter();
        p.setId(id);
        p.setProductId(productId);
        p.setParamName(name);
        p.setParamType("SELECT");
        p.setRequired(false);
        return p;
    }

    private static ParameterOption option(Long id, Long parameterId, String name, String adjust) {
        ParameterOption o = new ParameterOption();
        o.setId(id);
        o.setParameterId(parameterId);
        o.setOptionName(name);
        o.setPriceAdjustment(new BigDecimal(adjust));
        return o;
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
