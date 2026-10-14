package com.krishna.briefcase.billing;

import com.krishna.briefcase.billing.dto.FeeDistributionResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cases/{caseId}/fee-distributions")
public class FeeDistributionController {

    private final FeeDistributionService service;

    public FeeDistributionController(FeeDistributionService service) {
        this.service = service;
    }

    @GetMapping
    public List<FeeDistributionResponse> list(@PathVariable Long caseId) {
        return service.findByCase(caseId);
    }
}
