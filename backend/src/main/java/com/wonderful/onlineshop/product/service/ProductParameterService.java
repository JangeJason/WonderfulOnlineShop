package com.wonderful.onlineshop.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wonderful.onlineshop.common.BusinessException;
import com.wonderful.onlineshop.product.dto.AdminMaterialRulesDto;
import com.wonderful.onlineshop.product.dto.MaterialConfigResponse;
import com.wonderful.onlineshop.product.entity.ParameterOption;
import com.wonderful.onlineshop.product.entity.ProductMaterialParamOptionRule;
import com.wonderful.onlineshop.product.entity.ProductMaterialParamRule;
import com.wonderful.onlineshop.product.entity.ProductParameter;
import com.wonderful.onlineshop.product.mapper.ParameterOptionMapper;
import com.wonderful.onlineshop.product.mapper.ProductMaterialParamOptionRuleMapper;
import com.wonderful.onlineshop.product.mapper.ProductMaterialParamRuleMapper;
import com.wonderful.onlineshop.product.mapper.ProductParameterMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProductParameterService {

    private final ProductParameterMapper paramMapper;
    private final ParameterOptionMapper optionMapper;
    private final ProductMaterialParamRuleMapper materialParamRuleMapper;
    private final ProductMaterialParamOptionRuleMapper materialParamOptionRuleMapper;

    public ProductParameterService(
            ProductParameterMapper paramMapper,
            ParameterOptionMapper optionMapper,
            ProductMaterialParamRuleMapper materialParamRuleMapper,
            ProductMaterialParamOptionRuleMapper materialParamOptionRuleMapper) {
        this.paramMapper = paramMapper;
        this.optionMapper = optionMapper;
        this.materialParamRuleMapper = materialParamRuleMapper;
        this.materialParamOptionRuleMapper = materialParamOptionRuleMapper;
    }

    public List<ProductParameter> getParametersByProductId(Long productId) {
        return paramMapper.selectList(new LambdaQueryWrapper<ProductParameter>()
                .eq(ProductParameter::getProductId, productId)
                .orderByAsc(ProductParameter::getSortOrder));
    }

    public List<ParameterOption> getOptionsByParamId(Long parameterId) {
        return optionMapper.selectList(new LambdaQueryWrapper<ParameterOption>()
                .eq(ParameterOption::getParameterId, parameterId)
                .orderByAsc(ParameterOption::getSortOrder));
    }

    public ProductParameter createParameter(ProductParameter parameter) {
        if (parameter.getIsRequired() == null) {
            parameter.setIsRequired(true);
        }
        if (parameter.getIsMultiple() == null) {
            parameter.setIsMultiple(false);
        }
        if (parameter.getIsDynamicByMaterial() == null) {
            parameter.setIsDynamicByMaterial(false);
        }
        paramMapper.insert(parameter);
        return parameter;
    }

    public ProductParameter updateParameter(Long id, ProductParameter patch) {
        ProductParameter existing = paramMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("Parameter not found");
        }
        if (patch.getParamName() != null)
            existing.setParamName(patch.getParamName());
        if (patch.getParamType() != null)
            existing.setParamType(patch.getParamType());
        if (patch.getIsRequired() != null)
            existing.setIsRequired(patch.getIsRequired());
        if (patch.getIsMultiple() != null)
            existing.setIsMultiple(patch.getIsMultiple());
        if (patch.getIsDynamicByMaterial() != null)
            existing.setIsDynamicByMaterial(patch.getIsDynamicByMaterial());
        // allow clearing unit
        existing.setUnit(patch.getUnit());
        if (patch.getPricingRule() != null)
            existing.setPricingRule(patch.getPricingRule());
        if (patch.getValidationRules() != null)
            existing.setValidationRules(patch.getValidationRules());
        if (patch.getSortOrder() != null)
            existing.setSortOrder(patch.getSortOrder());

        paramMapper.updateById(existing);
        return existing;
    }

    public void deleteParameter(Long id) {
        ProductParameter existing = paramMapper.selectById(id);
        if (existing == null) {
            return;
        }
        paramMapper.deleteById(id);
        // Also delete options associated with this parameter
        optionMapper.delete(new LambdaQueryWrapper<ParameterOption>().eq(ParameterOption::getParameterId, id));
        materialParamRuleMapper.delete(new LambdaQueryWrapper<ProductMaterialParamRule>()
                .eq(ProductMaterialParamRule::getProductId, existing.getProductId())
                .eq(ProductMaterialParamRule::getParamId, id));
        materialParamOptionRuleMapper.delete(new LambdaQueryWrapper<ProductMaterialParamOptionRule>()
                .eq(ProductMaterialParamOptionRule::getProductId, existing.getProductId())
                .eq(ProductMaterialParamOptionRule::getParamId, id));
    }

    public ParameterOption createOption(ParameterOption option) {
        optionMapper.insert(option);
        return option;
    }

    public ParameterOption updateOption(Long id, ParameterOption patch) {
        ParameterOption existing = optionMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("Option not found");
        }
        if (patch.getOptionName() != null)
            existing.setOptionName(patch.getOptionName());
        if (patch.getPriceAdjustment() != null)
            existing.setPriceAdjustment(patch.getPriceAdjustment());
        if (patch.getImageUrl() != null)
            existing.setImageUrl(patch.getImageUrl());
        if (patch.getSortOrder() != null)
            existing.setSortOrder(patch.getSortOrder());

        optionMapper.updateById(existing);
        return existing;
    }

    public void deleteOption(Long id) {
        ParameterOption option = optionMapper.selectById(id);
        optionMapper.deleteById(id);
        if (option != null) {
            materialParamOptionRuleMapper.delete(new LambdaQueryWrapper<ProductMaterialParamOptionRule>()
                    .eq(ProductMaterialParamOptionRule::getOptionId, id));
        }
    }

    public Optional<ProductParameter> findMaterialParameter(Long productId) {
        return getParametersByProductId(productId).stream()
                .filter(p -> p.getParamType() != null && "SELECT".equalsIgnoreCase(p.getParamType()))
                .filter(p -> p.getParamName() != null && "材质".equals(p.getParamName().trim()))
                .findFirst();
    }

    public MaterialConfigResponse getMaterialConfig(Long productId, Long materialOptionId) {
        Optional<ProductParameter> materialParamOpt = findMaterialParameter(productId);
        Long materialParameterId = materialParamOpt.map(ProductParameter::getId).orElse(null);
        if (materialOptionId == null) {
            return new MaterialConfigResponse(materialParameterId, null, Collections.emptyList(), Collections.emptyMap());
        }

        List<ProductMaterialParamRule> paramRules = materialParamRuleMapper.selectList(
                new LambdaQueryWrapper<ProductMaterialParamRule>()
                        .eq(ProductMaterialParamRule::getProductId, productId)
                        .eq(ProductMaterialParamRule::getMaterialOptionId, materialOptionId)
                        .eq(ProductMaterialParamRule::getEnabled, true));
        List<Long> enabledDynamicParamIds = paramRules.stream()
                .map(ProductMaterialParamRule::getParamId)
                .distinct()
                .toList();

        List<ProductMaterialParamOptionRule> optionRules = materialParamOptionRuleMapper.selectList(
                new LambdaQueryWrapper<ProductMaterialParamOptionRule>()
                        .eq(ProductMaterialParamOptionRule::getProductId, productId)
                        .eq(ProductMaterialParamOptionRule::getMaterialOptionId, materialOptionId)
                        .eq(ProductMaterialParamOptionRule::getEnabled, true));
        Map<Long, List<Long>> enabledOptionIds = optionRules.stream()
                .collect(Collectors.groupingBy(
                        ProductMaterialParamOptionRule::getParamId,
                        Collectors.mapping(ProductMaterialParamOptionRule::getOptionId, Collectors.toList())));

        enabledOptionIds.replaceAll((k, v) -> v.stream().distinct().toList());
        return new MaterialConfigResponse(materialParameterId, materialOptionId, enabledDynamicParamIds, enabledOptionIds);
    }

    public AdminMaterialRulesDto.MaterialRulesResponse getAdminMaterialRules(Long productId, Long materialOptionId) {
        Long resolvedMaterialOptionId = materialOptionId;
        if (resolvedMaterialOptionId == null) {
            Optional<ProductParameter> materialParam = findMaterialParameter(productId);
            if (materialParam.isPresent()) {
                List<ParameterOption> materialOptions = getOptionsByParamId(materialParam.get().getId());
                if (!materialOptions.isEmpty()) {
                    resolvedMaterialOptionId = materialOptions.get(0).getId();
                }
            }
        }

        MaterialConfigResponse config = getMaterialConfig(productId, resolvedMaterialOptionId);
        List<ProductParameter> dynamicParams = getParametersByProductId(productId).stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsDynamicByMaterial()))
                .toList();
        List<AdminMaterialRulesDto.ParamRuleItem> rules = new ArrayList<>();
        Set<Long> enabledParamIds = new LinkedHashSet<>(config.enabledDynamicParamIds());
        for (ProductParameter p : dynamicParams) {
            Long paramId = p.getId();
            boolean enabled = enabledParamIds.contains(paramId);
            List<Long> optionIds = config.enabledOptionIds().getOrDefault(paramId, Collections.emptyList());
            rules.add(new AdminMaterialRulesDto.ParamRuleItem(paramId, enabled, optionIds));
        }
        return new AdminMaterialRulesDto.MaterialRulesResponse(config.materialParameterId(), resolvedMaterialOptionId, rules);
    }

    @Transactional
    public AdminMaterialRulesDto.MaterialRulesResponse saveAdminMaterialRules(Long productId, AdminMaterialRulesDto.MaterialRulesPayload payload) {
        if (payload == null || payload.materialOptionId() == null) {
            throw new BusinessException("materialOptionId 不能为空");
        }
        Long materialOptionId = payload.materialOptionId();
        List<ProductParameter> productParams = getParametersByProductId(productId);
        Map<Long, ProductParameter> paramMap = productParams.stream()
                .collect(Collectors.toMap(ProductParameter::getId, p -> p));
        List<ProductParameter> dynamicParams = productParams.stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsDynamicByMaterial()))
                .toList();
        Set<Long> dynamicParamIds = dynamicParams.stream().map(ProductParameter::getId).collect(Collectors.toSet());

        materialParamRuleMapper.delete(new LambdaQueryWrapper<ProductMaterialParamRule>()
                .eq(ProductMaterialParamRule::getProductId, productId)
                .eq(ProductMaterialParamRule::getMaterialOptionId, materialOptionId));
        materialParamOptionRuleMapper.delete(new LambdaQueryWrapper<ProductMaterialParamOptionRule>()
                .eq(ProductMaterialParamOptionRule::getProductId, productId)
                .eq(ProductMaterialParamOptionRule::getMaterialOptionId, materialOptionId));

        if (payload.rules() != null) {
            for (AdminMaterialRulesDto.ParamRuleItem rule : payload.rules()) {
                if (rule == null || rule.paramId() == null || !dynamicParamIds.contains(rule.paramId())) {
                    continue;
                }
                if (!Boolean.TRUE.equals(rule.enabled())) {
                    continue;
                }
                ProductMaterialParamRule row = new ProductMaterialParamRule();
                row.setProductId(productId);
                row.setMaterialOptionId(materialOptionId);
                row.setParamId(rule.paramId());
                row.setEnabled(true);
                materialParamRuleMapper.insert(row);

                ProductParameter param = paramMap.get(rule.paramId());
                if (param != null && "SELECT".equalsIgnoreCase(param.getParamType()) && rule.optionIds() != null) {
                    List<Long> distinctOptionIds = rule.optionIds().stream()
                            .filter(Objects::nonNull)
                            .distinct()
                            .toList();
                    if (!distinctOptionIds.isEmpty()) {
                        for (Long optionId : distinctOptionIds) {
                            ProductMaterialParamOptionRule optionRule = new ProductMaterialParamOptionRule();
                            optionRule.setProductId(productId);
                            optionRule.setMaterialOptionId(materialOptionId);
                            optionRule.setParamId(rule.paramId());
                            optionRule.setOptionId(optionId);
                            optionRule.setEnabled(true);
                            materialParamOptionRuleMapper.insert(optionRule);
                        }
                    }
                }
            }
        }
        return getAdminMaterialRules(productId, materialOptionId);
    }

    public MaterialConstraintView getConstraintView(Long productId, Long materialOptionId) {
        List<ProductParameter> allParams = getParametersByProductId(productId);
        MaterialConfigResponse config = getMaterialConfig(productId, materialOptionId);
        Set<Long> enabledDynamicParamIds = new LinkedHashSet<>(config.enabledDynamicParamIds());
        Map<Long, Set<Long>> enabledOptionIdSet = new HashMap<>();
        config.enabledOptionIds().forEach((k, v) -> enabledOptionIdSet.put(k, new LinkedHashSet<>(v)));

        Set<Long> visibleParamIds = new LinkedHashSet<>();
        Long materialParamId = config.materialParameterId();
        for (ProductParameter p : allParams) {
            if (materialParamId != null && Objects.equals(materialParamId, p.getId())) {
                visibleParamIds.add(p.getId());
                continue;
            }
            boolean dynamic = Boolean.TRUE.equals(p.getIsDynamicByMaterial());
            if (!dynamic || enabledDynamicParamIds.contains(p.getId())) {
                visibleParamIds.add(p.getId());
            }
        }
        return new MaterialConstraintView(config.materialParameterId(), materialOptionId, visibleParamIds, enabledOptionIdSet);
    }

    public record MaterialConstraintView(
            Long materialParameterId,
            Long materialOptionId,
            Set<Long> visibleParamIds,
            Map<Long, Set<Long>> enabledOptionIdSet
    ) {
    }
}
