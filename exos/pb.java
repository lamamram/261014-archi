package com.insurance.pricing;

import com.insurance.claims.ClaimsHistoryService;
import com.insurance.promotions.ActivePromotionService;

/**
 * Service de tarification
 */
public class PremiumCalculator {
    private ClaimsHistoryService claimsService;
    private ActivePromotionService promotionService;
    
    public PremiumCalculator() {
        this.claimsService = new ClaimsHistoryService();
        this.promotionService = new ActivePromotionService();
    }
    
    /**
     * Calcul de la prime
     */
    public BigDecimal calculatePremium(Policy policy) {
        BigDecimal basePremium = calculateBasePremium(policy);
        
        List<Claim> claims = claimsService.getClaimsByPolicyHolder(
            policy.getHolderId()
        );
        BigDecimal claimsPenalty = claimsService.calculateMalusPercentage(claims);
        

        Promotion activePromo = promotionService.findActivePromotion(
            policy.getProductType(),
            policy.getCustomerSegment()
        );
        BigDecimal discount = promotionService.calculateDiscount(
            activePromo,
            basePremium
        );
        
        return basePremium
            .multiply(BigDecimal.ONE.add(claimsPenalty))
            .subtract(discount);
    }
    
    private BigDecimal calculateBasePremium(Policy policy) {
        return policy.getCoverage()
            .multiply(policy.getRiskFactor())
            .multiply(new BigDecimal("0.05"));
    }
}


package com.insurance.claims;

public class ClaimsHistoryService {
    
    /**
     * Règles de malus
     * - Nouvelles lois
     * - Nouveaux types de sinistres
     * - Règles spécifiques par région
     */
    public BigDecimal calculateMalusPercentage(List<Claim> claims) {
        if (claims.isEmpty()) return BigDecimal.ZERO;
        
        long bodilyClaims = claims.stream()
            .filter(c -> c.getType() == ClaimType.BODILY_INJURY)
            .count();
        
        BigDecimal totalAmount = claims.stream()
            .map(Claim::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (totalAmount.compareTo(new BigDecimal("50000")) > 0) {
            return new BigDecimal("0.35"); // 35% de malus
        }
        
        return new BigDecimal("0.10").multiply(
            new BigDecimal(claims.size())
        );
    }
    
    public List<Claim> getClaimsByPolicyHolder(String holderId) {
        return claimRepository.findByHolderId(holderId);
    }
}


package com.insurance.promotions;

public class ActivePromotionService {
    
    /**
     * Promotions
     * - Campagnes saisonnières
     * - Offres flash
     * - Promotions partenaires
     */
    public Promotion findActivePromotion(String productType, String segment) {
        LocalDate today = LocalDate.now();
        
        if (today.getMonth() == Month.DECEMBER) {
            return new Promotion("NOEL2025", new BigDecimal("0.15"));
        }
        
        if ("YOUNG_DRIVER".equals(segment) && "AUTO".equals(productType)) {
            return new Promotion("YOUNG15", new BigDecimal("0.10"));
        }
        
        if ("BANK_PARTNER".equals(segment)) {
            return new Promotion("BANK20", new BigDecimal("0.20"));
        }
        
        return Promotion.NO_PROMOTION;
    }
    
    public BigDecimal calculateDiscount(Promotion promo, BigDecimal basePremium) {
        // Calcul de réduction avec règles spécifiques par promo
        return basePremium.multiply(promo.getDiscountRate());
    }
}