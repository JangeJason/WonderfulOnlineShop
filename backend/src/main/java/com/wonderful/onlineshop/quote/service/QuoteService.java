package com.wonderful.onlineshop.quote.service;

import com.wonderful.onlineshop.product.entity.ParameterOption;
import com.wonderful.onlineshop.product.entity.Product;
import com.wonderful.onlineshop.product.entity.ProductParameter;
import com.wonderful.onlineshop.product.service.ProductParameterService;
import com.wonderful.onlineshop.product.service.ProductService;
import com.wonderful.onlineshop.quote.dto.QuoteRequest;
import com.wonderful.onlineshop.quote.dto.QuoteRequestItem;
import com.wonderful.onlineshop.quote.dto.QuoteResponse;
import com.wonderful.onlineshop.common.BusinessException;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class QuoteService {

    private final ProductService productService;
    private final ProductParameterService parameterService;
    private final ExpressionParser parser = new SpelExpressionParser();

    public QuoteService(ProductService productService, ProductParameterService parameterService) {
        this.productService = productService;
        this.parameterService = parameterService;
    }

    public QuoteResponse quote(QuoteRequest req) {
        Product product = productService.getActiveRequired(req.productId());
        BigDecimal basePrice = product.getBasePrice();

        // Load all parameters for this product
        List<ProductParameter> parameters = parameterService.getParametersByProductId(product.getId());

        // Create a map of user inputs for easy lookup
        Map<Long, QuoteRequestItem> inputMap = req.items().stream()
                .collect(Collectors.toMap(QuoteRequestItem::parameterId, item -> item));

        Long materialOptionId = resolveMaterialOptionId(parameters, inputMap);
        ProductParameterService.MaterialConstraintView constraintView =
                parameterService.getConstraintView(product.getId(), materialOptionId);
        validateInputs(parameters, inputMap, constraintView);

        // Prepare context for SpEL (for formulas)
        StandardEvaluationContext context = new StandardEvaluationContext();
        // Populate context with variable names (e.g., "L" -> 200)
        parameters.forEach(param -> {
            QuoteRequestItem input = inputMap.get(param.getId());
            if (input != null && input.valueNumber() != null) {
                // Use paramName as variable name (ensure it's safe) or add a code field later
                // For now, assume paramName is the variable name like "L", "W"
                context.setVariable(param.getParamName(), input.valueNumber());
            }
        });

        BigDecimal formulaPart = BigDecimal.ZERO;
        BigDecimal optionAdjust = BigDecimal.ZERO;

        // Evaluate product-level pricing formula (references all parameters)
        String formula = product.getPricingFormula();
        String displayFormula = "计价公式";
        if (formula != null && !formula.isBlank()) {
            displayFormula = "计价公式（" + formula.replace("*", " x ") + "）";
            try {
                BigDecimal result = parser.parseExpression(formula).getValue(context, BigDecimal.class);
                if (result != null) {
                    formulaPart = result;
                }
            } catch (Exception e) {
                System.err.println("Error evaluating pricing formula: " + e.getMessage());
            }
        }

        // Collect individual selected option adjustments
        Map<String, BigDecimal> optionBreakdowns = new HashMap<>();

        for (ProductParameter param : parameters) {
            QuoteRequestItem input = inputMap.get(param.getId());

            if ("SELECT".equalsIgnoreCase(param.getParamType())) {
                if (input != null && input.selectedOptionIds() != null) {
                    List<ParameterOption> options = parameterService.getOptionsByParamId(param.getId());

                    BigDecimal paramOptionTotal = options.stream()
                            .filter(opt -> input.selectedOptionIds().contains(opt.getId()))
                            .map(ParameterOption::getPriceAdjustment)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    if (paramOptionTotal.compareTo(BigDecimal.ZERO) != 0) {
                        optionAdjust = optionAdjust.add(paramOptionTotal);

                        // Extract exact option names for the breakdown string
                        List<String> selectedOptionNames = options.stream()
                                .filter(opt -> input.selectedOptionIds().contains(opt.getId()))
                                .map(ParameterOption::getOptionName)
                                .toList();
                        String optionNamesStr = String.join("，", selectedOptionNames);

                        optionBreakdowns.put(param.getParamName() + "（" + optionNamesStr + "）", paramOptionTotal);
                    }
                }
            }
        }

        BigDecimal unitPrice = basePrice.add(optionAdjust).add(formulaPart)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalPrice = unitPrice.multiply(new BigDecimal(req.quantity()))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal appliedSetupFee = BigDecimal.ZERO;

        // Calculate setup fee logic
        Integer freeThreshold = product.getFreeSetupQuantity();
        BigDecimal setupFee = product.getSetupFee();

        if (setupFee != null && setupFee.compareTo(BigDecimal.ZERO) > 0) {
            // Apply setup fee only if threshold is defined and quantity is less than the
            // threshold
            if (freeThreshold != null && freeThreshold > 0 && req.quantity() < freeThreshold) {
                appliedSetupFee = setupFee;
                totalPrice = totalPrice.add(appliedSetupFee).setScale(2, RoundingMode.HALF_UP);
            }
        }

        Map<String, BigDecimal> breakdown = new LinkedHashMap<>(); // Use LinkedHashMap to preserve order
        breakdown.put("基础价", basePrice);
        if (formulaPart.compareTo(BigDecimal.ZERO) != 0) {
            breakdown.put(displayFormula, formulaPart);
        }
        breakdown.putAll(optionBreakdowns);
        breakdown.put("setupFee", appliedSetupFee);

        return new QuoteResponse(req.productId(), req.quantity(), unitPrice, totalPrice, breakdown);
    }

    private Long resolveMaterialOptionId(List<ProductParameter> parameters, Map<Long, QuoteRequestItem> inputMap) {
        ProductParameter materialParam = parameters.stream()
                .filter(p -> "SELECT".equalsIgnoreCase(p.getParamType()))
                .filter(p -> p.getParamName() != null && "材质".equals(p.getParamName().trim()))
                .findFirst()
                .orElse(null);
        if (materialParam == null) {
            return null;
        }
        QuoteRequestItem materialInput = inputMap.get(materialParam.getId());
        if (materialInput == null || materialInput.selectedOptionIds() == null || materialInput.selectedOptionIds().isEmpty()) {
            throw new BusinessException("请先选择材质");
        }
        return materialInput.selectedOptionIds().get(0);
    }

    private void validateInputs(
            List<ProductParameter> parameters,
            Map<Long, QuoteRequestItem> inputMap,
            ProductParameterService.MaterialConstraintView constraintView) {
        Set<Long> visibleParamIds = constraintView.visibleParamIds();

        for (QuoteRequestItem item : inputMap.values()) {
            if (!visibleParamIds.contains(item.parameterId())) {
                throw new BusinessException("参数不适用于当前材质，请重新选择");
            }
        }

        for (ProductParameter param : parameters) {
            if (!visibleParamIds.contains(param.getId())) {
                continue;
            }
            QuoteRequestItem input = inputMap.get(param.getId());
            boolean required = !Boolean.FALSE.equals(param.getIsRequired());
            if (required) {
                if ("INPUT".equalsIgnoreCase(param.getParamType())) {
                    if (input == null || input.valueNumber() == null) {
                        throw new BusinessException("必填参数未填写: " + param.getParamName());
                    }
                } else {
                    if (input == null || input.selectedOptionIds() == null || input.selectedOptionIds().isEmpty()) {
                        throw new BusinessException("必填参数未选择: " + param.getParamName());
                    }
                }
            }

            if (!"SELECT".equalsIgnoreCase(param.getParamType()) || input == null || input.selectedOptionIds() == null) {
                continue;
            }

            Set<Long> allowedOptionIds = constraintView.enabledOptionIdSet().get(param.getId());
            if (allowedOptionIds != null && !allowedOptionIds.isEmpty()) {
                for (Long selected : input.selectedOptionIds()) {
                    if (!allowedOptionIds.contains(selected)) {
                        throw new BusinessException("参数选项不适用于当前材质: " + param.getParamName());
                    }
                }
            }
        }
    }
}
