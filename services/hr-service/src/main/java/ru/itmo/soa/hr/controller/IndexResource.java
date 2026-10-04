package ru.itmo.soa.hr.controller;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.math.BigDecimal;
import ru.itmo.soa.hr.dto.IndexationResult;
import ru.itmo.soa.hr.dto.OrganizationIndexationResult;
import ru.itmo.soa.hr.service.IndexationService;

@RequestScoped
@Path("/index")
@Produces(MediaType.APPLICATION_JSON)
public class IndexResource {

    private static final String ID_ISSUE = "Значение должно быть целым числом >= 1";
    private static final String COEFF_ISSUE = "Коэффициент должен быть числом строго больше 0";

    @Inject
    IndexationService service;

    @POST
    @Path("/{worker-id}/{coeff}")
    public IndexationResult indexWorker(
            @PathParam("worker-id") @Pattern(regexp = "\\d{1,10}", message = ID_ISSUE)
            @Min(value = 1, message = ID_ISSUE) @Max(value = Integer.MAX_VALUE, message = ID_ISSUE) String workerId,
            @PathParam("coeff") @DecimalMin(value = "0", inclusive = false, message = COEFF_ISSUE) String coeff) {
        return service.indexWorker(Integer.parseInt(workerId), new BigDecimal(coeff));
    }

    @POST
    @Path("/all/{org-id}/{coeff}")
    public OrganizationIndexationResult indexOrganization(
            @PathParam("org-id") @NotBlank(message = "Значение не может быть пустым") String orgId,
            @PathParam("coeff") @DecimalMin(value = "0", inclusive = false, message = COEFF_ISSUE) String coeff) {
        return service.indexOrganization(orgId, new BigDecimal(coeff));
    }
}
